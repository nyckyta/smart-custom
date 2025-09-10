package edu.ukma.smart.virtual;

import static edu.ukma.smart.virtual.errors.OperationError.ErrorCode.FAILED_TO_RELEASE_CONNECTION_TIMEOUT;

import edu.ukma.smart.virtual.errors.Err;
import edu.ukma.smart.virtual.errors.FatalError;
import edu.ukma.smart.virtual.errors.OperationError;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectionPool implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(ConnectionPool.class);
    // queue to simulate round robin connection usage
    private final int poolLimit;
    private final Supplier<Connection> connectionSupplier;
    private final Set<Connection> availableConnections;
    private final Set<Connection> acquiredConnections;
    private final ExecutorService timeoutExecutor = Executors.newSingleThreadExecutor(r -> {
        var th = new Thread(r);
        th.setDaemon(true);
        th.setName("Virtual-tables-connection-pool-thread-" + th.hashCode());
        return th;
    });

    private final ExecutorService syncExecutor = Executors.newSingleThreadExecutor(r -> {
        var th = new Thread(r);
        th.setDaemon(true);
        th.setName("Virtual-tables-connection-executor-thread-" + th.hashCode());
        return th;
    });
    private final int networkTimeoutMs;

    public ConnectionPool(
        Supplier<Connection> connectionSupplier,
        int poolLimit,
        int networkTimeoutMs
    ) {
        this.poolLimit = poolLimit;
        this.connectionSupplier = connectionSupplier;
        this.availableConnections = new HashSet<>();
        this.acquiredConnections = new HashSet<>();
        this.networkTimeoutMs = networkTimeoutMs;

    }

    public Optional<Connection> acquireConnection(boolean readOnly, int timeoutMillis) throws ExecutionException,
        InterruptedException {
        Future<Optional<Connection>> acquire = syncExecutor.submit(() -> {
            var optionalConn = availableConnections.stream().findAny();
            if (optionalConn.isEmpty()) {
                // pool has reached limit, all connections are acquired
                if (acquiredConnections.size() >= poolLimit) {
                    log.debug("Can't acquire connection, all connections are busy");
                    return Optional.empty();
                }

                // create new connection and return immediately
                var conn = createConnection();
                conn.setReadOnly(readOnly);
                acquiredConnections.add(conn);
                log.info("Created new connection {}", conn);
                return Optional.of(conn);
            }


            var conn = optionalConn.get();
            availableConnections.remove(conn);
            // remove closed and replace it by a new
            if (conn.isClosed()) {
                log.debug("Connection has been closed, removing and recreating");
                conn = createConnection();
                conn.setReadOnly(readOnly);
                acquiredConnections.add(conn);
                log.info("Acquired connection {}", conn);
                return Optional.of(conn);
            }

            // return available connection
            conn.setReadOnly(readOnly);
            acquiredConnections.add(conn);
            log.info("Pool connection has been acquired {}", conn);
            return Optional.of(conn);
        });

        try {
            return acquire.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            log.warn("Failed to acquire connection due to timeout", e);
            return Optional.empty();
        }

    }

    public Optional<Err> releaseConnection(Connection connection, int timeoutMillis)
        throws ExecutionException, InterruptedException {
        Future<?> task = syncExecutor.submit(() -> {
            log.info("Releasing connection {}", connection);
            var hasRemoved = acquiredConnections.remove(connection);
            if (!hasRemoved) {
                if (availableConnections.contains(connection)) {
                    log.error("Available connection has been released, can it be second time release? conn {}", connection);
                    throw new FatalError("Not acquired connection has bee released, can it be double release?");
                }

                log.warn(
                    "Unknown connection has been released {}; Currently acquired connections( {} ); Currently available connections{};",
                    connection,
                    acquiredConnections.stream().map(Object::toString).collect(Collectors.joining(",")),
                    availableConnections.stream().map(Object::toString).collect(Collectors.joining(","))
                );
                throw new FatalError("Unknown connection has been released");
            }
            availableConnections.add(connection);
        });

        try {
            task.get(timeoutMillis, TimeUnit.MILLISECONDS);
            return Optional.empty();
        } catch (TimeoutException e) {
            return Optional.of(OperationError.of(FAILED_TO_RELEASE_CONNECTION_TIMEOUT));
        }
    }


    @Override
    public void close() {
        syncExecutor.execute(() -> {
            syncExecutor.shutdown();
            syncExecutor.close();
            timeoutExecutor.shutdown();
            timeoutExecutor.close();
            for (var conn : acquiredConnections) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    log.warn("Failed to close acquired connection {}", conn);
                }
            }
            for (var conn : availableConnections) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    log.warn("Failed to close available connection {}", conn);
                }
            }
            availableConnections.clear();
            acquiredConnections.clear();
        });
    }

    private Connection createConnection() throws SQLException {
        var conn = connectionSupplier.get();
        conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
        conn.setAutoCommit(false);
        conn.setNetworkTimeout(timeoutExecutor, networkTimeoutMs);
        return conn;
    }
}

package edu.ukma.smart.virtual;

import edu.ukma.smart.virtual.ddl.create.Property;
import edu.ukma.smart.virtual.errors.Return;
import edu.ukma.smart.virtual.metadata.Table;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public interface MetadataProcessor {

    Return<List<Table>> processTables(ResultSet rs) throws SQLException;

    Return<List<Property>> processProperties(ResultSet rs) throws SQLException;
}

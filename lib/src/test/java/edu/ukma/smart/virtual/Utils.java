package edu.ukma.smart.virtual;

import java.nio.charset.StandardCharsets;
import java.util.Random;

public class Utils {

    public static String generateRandomString(int length) {
        Random r = new Random();
        var bytes = new byte[length];

        for (int i = 0; i < length; i += 1) {
            // generate random lower case ascii letter
            bytes[i] = (byte) r.nextInt(97, 123);
        }

        return new String(bytes, StandardCharsets.UTF_8);
    }
}

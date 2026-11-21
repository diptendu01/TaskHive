package com.taskhive.backend.util;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

/**
 * Generates RFC 9562 UUIDv7 (time-ordered) values.
 */
public final class Uuid7 {
    private static final SecureRandom RANDOM = new SecureRandom();

    private Uuid7() {
    }

    public static UUID now() {
        long unixMillis = Instant.now().toEpochMilli();
        byte[] rand = new byte[10];
        RANDOM.nextBytes(rand);

        long randA = ((rand[0] & 0x0FL) << 8) | (rand[1] & 0xFFL);
        long msb = ((unixMillis & 0xFFFFFFFFFFFFL) << 16)
                | (0x7L << 12)
                | randA;

        long randB = 0L;
        for (int i = 2; i < 10; i++) {
            randB = (randB << 8) | (rand[i] & 0xFFL);
        }
        long lsb = (randB & 0x3FFFFFFFFFFFFFFFL) | 0x8000000000000000L;

        return new UUID(msb, lsb);
    }
}
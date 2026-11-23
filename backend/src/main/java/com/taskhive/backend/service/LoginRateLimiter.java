package com.taskhive.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redis-backed login rate limiter (10 failed attempts per 15-minute window per IP + email),
 * with an automatic in-memory fallback when Redis is unavailable (e.g. local dev/tests).
 */
@Service
public class LoginRateLimiter {

    public static final int MAX_ATTEMPTS = 10;
    public static final Duration WINDOW = Duration.ofMinutes(15);
    private static final String KEY_PREFIX = "taskhive:ratelimit:login:";
    private static final Logger log = LoggerFactory.getLogger(LoginRateLimiter.class);

    private final StringRedisTemplate redisTemplate;
    private final ConcurrentHashMap<String, LocalBucket> localBuckets = new ConcurrentHashMap<>();

    private record LocalBucket(int attempts, Instant expiresAt) {
    }

    public LoginRateLimiter(@Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void checkAllowed(String clientIp, String email) {
        String key = buildKey(clientIp, email);
        long attempts = getAttempts(key);
        if (attempts >= MAX_ATTEMPTS) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too many failed login attempts. Please try again in 15 minutes."
            );
        }
    }

    public void recordFailure(String clientIp, String email) {
        String key = buildKey(clientIp, email);
        if (redisTemplate != null) {
            try {
                Long count = redisTemplate.opsForValue().increment(key);
                if (count != null && count == 1L) {
                    redisTemplate.expire(key, WINDOW);
                }
                return;
            } catch (Exception ex) {
                log.debug("Redis rate-limit write unavailable, using local fallback: {}", ex.getMessage());
            }
        }
        Instant now = Instant.now();
        localBuckets.compute(key, (k, existing) -> {
            if (existing == null || now.isAfter(existing.expiresAt())) {
                return new LocalBucket(1, now.plus(WINDOW));
            }
            return new LocalBucket(existing.attempts() + 1, existing.expiresAt());
        });
    }

    public void clear(String clientIp, String email) {
        String key = buildKey(clientIp, email);
        if (redisTemplate != null) {
            try {
                redisTemplate.delete(key);
            } catch (Exception ex) {
                log.debug("Redis rate-limit clear unavailable: {}", ex.getMessage());
            }
        }
        localBuckets.remove(key);
    }

    private long getAttempts(String key) {
        if (redisTemplate != null) {
            try {
                String value = redisTemplate.opsForValue().get(key);
                return value != null ? Long.parseLong(value) : 0L;
            } catch (Exception ex) {
                log.debug("Redis rate-limit read unavailable, using local fallback: {}", ex.getMessage());
            }
        }
        LocalBucket bucket = localBuckets.get(key);
        if (bucket == null || Instant.now().isAfter(bucket.expiresAt())) {
            localBuckets.remove(key);
            return 0L;
        }
        return bucket.attempts();
    }

    private String buildKey(String clientIp, String email) {
        String safeIp = clientIp == null || clientIp.isBlank() ? "unknown" : clientIp.trim();
        String safeEmail = email == null ? "" : email.trim().toLowerCase();
        return KEY_PREFIX + safeIp + ":" + safeEmail;
    }
}

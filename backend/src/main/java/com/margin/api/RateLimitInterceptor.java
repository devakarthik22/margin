package com.margin.api;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IP-based token-bucket rate limiter for the ad-hoc review endpoints.
 * 10 requests per minute per client IP. The in-memory bucket map is unbounded —
 * acceptable for a demo; production should evict stale entries with a TTL cache.
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final int REQUESTS_PER_MINUTE = 10;
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws IOException {
        // Tomcat re-invokes interceptors on async dispatches (SSE completion callbacks).
        // Skip them — they're part of the same original request, not new ones.
        if (request.getDispatcherType() == DispatcherType.ASYNC) {
            return true;
        }
        Bucket bucket = buckets.computeIfAbsent(resolveIp(request), ip -> newBucket());
        if (bucket.tryConsume(1)) {
            return true;
        }
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", "60");
        response.getWriter().write("{\"error\":\"Rate limit exceeded. Please try again in a minute.\"}");
        return false;
    }

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(REQUESTS_PER_MINUTE)
                .refillGreedy(REQUESTS_PER_MINUTE, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return (forwarded != null && !forwarded.isBlank())
                ? forwarded.split(",")[0].trim()
                : request.getRemoteAddr();
    }
}

package com.example.client.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;
import java.util.function.Supplier;

@Service
@Slf4j
public class HybridCacheService {

    private final CacheManager cacheManager;
    private final ExecutorService executor;
    private static final long TIMEOUT_MS = 2000; // 2 seconds

    public HybridCacheService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
        this.executor = Executors.newFixedThreadPool(10);
    }

    /**
     * Get from cache with timeout. Falls back to null if cache operation exceeds timeout.
     */
    public <T> T getWithTimeout(String cacheName, Object key, Class<T> type) {
        try {
            Future<T> future = executor.submit(() -> {
                Cache cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    Cache.ValueWrapper wrapper = cache.get(key);
                    if (wrapper != null) {
                        return type.cast(wrapper.get());
                    }
                }
                return null;
            });

            return future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            log.warn("Cache get operation timed out for cache: {}, key: {}", cacheName, key);
            return null;
        } catch (Exception e) {
            log.error("Error getting from cache: {}, key: {}", cacheName, key, e);
            return null;
        }
    }

    /**
     * Put to cache asynchronously for eventual consistency.
     * Returns immediately without waiting for cache operation to complete.
     */
    public void putAsync(String cacheName, Object key, Object value) {
        executor.submit(() -> {
            try {
                Cache cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.put(key, value);
                    log.debug("Async cache put successful for cache: {}, key: {}", cacheName, key);
                }
            } catch (Exception e) {
                log.error("Error putting to cache asynchronously: {}, key: {}", cacheName, key, e);
            }
        });
    }

    /**
     * Evict from cache asynchronously.
     * Returns immediately without waiting for cache operation to complete.
     */
    public void evictAsync(String cacheName, Object key) {
        executor.submit(() -> {
            try {
                Cache cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.evict(key);
                    log.debug("Async cache evict successful for cache: {}, key: {}", cacheName, key);
                }
            } catch (Exception e) {
                log.error("Error evicting from cache asynchronously: {}, key: {}", cacheName, key, e);
            }
        });
    }

    /**
     * Get from cache or execute supplier if miss, with timeout protection.
     */
    public <T> T getOrCompute(String cacheName, Object key, Supplier<T> supplier, Class<T> type) {
        T cached = getWithTimeout(cacheName, key, type);
        if (cached != null) {
            return cached;
        }

        T value = supplier.get();
        if (value != null) {
            putAsync(cacheName, key, value);
        }
        return value;
    }

    /**
     * Clear entire cache asynchronously.
     */
    public void clearAsync(String cacheName) {
        executor.submit(() -> {
            try {
                Cache cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                    log.info("Cache cleared: {}", cacheName);
                }
            } catch (Exception e) {
                log.error("Error clearing cache: {}", cacheName, e);
            }
        });
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }
}
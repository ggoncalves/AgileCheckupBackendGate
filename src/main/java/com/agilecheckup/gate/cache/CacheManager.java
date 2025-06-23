package com.agilecheckup.gate.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Simple in-memory cache manager for API Gateway responses.
 * 
 * IMPORTANT: This is a temporary implementation residing in the Gate module.
 * As the caching needs grow, this should be refactored into a separate 
 * AgileCheckupCache module.
 * 
 * Future refactoring guidelines:
 * 1. Create new module: AgileCheckupCache
 * 2. Define cache interfaces in Perpetua (e.g., CacheProvider, CacheConfig)
 * 3. Implement adapters for different cache backends:
 *    - InMemoryCacheAdapter (current Caffeine implementation)
 *    - RedisCacheAdapter (for distributed caching)
 *    - DynamoDBCacheAdapter (for AWS-native solution)
 * 4. Use dependency injection to provide cache implementation
 * 5. Configure cache backend via application.properties
 * 
 * Best practices for future cache module:
 * - Use cache-aside pattern for read operations
 * - Implement cache warming for critical data
 * - Add metrics and monitoring (cache hit/miss rates)
 * - Support multi-tenancy with tenant-specific cache keys
 * - Implement cache invalidation strategies
 * - Consider using AWS ElastiCache for production
 * 
 * Example future structure:
 * AgileCheckupCache/
 * ├── cache-api/          (interfaces and DTOs)
 * ├── cache-core/         (core implementation)
 * ├── cache-caffeine/     (Caffeine adapter)
 * ├── cache-redis/        (Redis adapter)
 * └── cache-dynamodb/     (DynamoDB adapter)
 * 
 * @author Claude (claude-opus-4-20250514)
 */
@Singleton
public class CacheManager {
    
    private final Cache<String, Object> cache;
    
    /**
     * Default cache configuration:
     * - Maximum 1000 entries (suitable for Lambda memory constraints)
     * - 5 minutes TTL (balances freshness vs performance)
     * - LRU eviction policy
     */
    @Inject
    public CacheManager() {
        this.cache = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats() // Enable statistics for monitoring
                .build();
    }
    
    /**
     * Retrieves a value from cache.
     * 
     * @param key The cache key (should include tenant ID for multi-tenancy)
     * @param type The expected type of the cached value
     * @return Optional containing the cached value if present and of correct type
     */
    public <T> Optional<T> get(String key, Class<T> type) {
        if (StringUtils.isBlank(key) || type == null) {
            return Optional.empty();
        }
        
        Object value = cache.getIfPresent(key);
        if (value != null && type.isInstance(value)) {
            return Optional.of(type.cast(value));
        }
        return Optional.empty();
    }
    
    /**
     * Stores a value in cache.
     * 
     * @param key The cache key (should include tenant ID for multi-tenancy)
     * @param value The value to cache
     */
    public void put(String key, Object value) {
        if (StringUtils.isNotBlank(key) && value != null) {
            cache.put(key, value);
        }
    }
    
    /**
     * Removes a value from cache.
     * 
     * @param key The cache key to invalidate
     */
    public void evict(String key) {
        if (StringUtils.isNotBlank(key)) {
            cache.invalidate(key);
        }
    }
    
    /**
     * Clears all cached entries.
     * Use with caution in production.
     */
    public void clear() {
        cache.invalidateAll();
    }
    
    /**
     * Gets cache statistics for monitoring.
     * 
     * @return Cache statistics including hit rate, miss count, etc.
     */
    public CacheStats getStats() {
        var stats = cache.stats();
        return CacheStats.builder()
                .hitCount(stats.hitCount())
                .missCount(stats.missCount())
                .hitRate(stats.hitRate())
                .evictionCount(stats.evictionCount())
                .size(cache.estimatedSize())
                .build();
    }
    
    /**
     * Cache statistics DTO.
     */
    @Getter
    @RequiredArgsConstructor
    @lombok.Builder
    public static class CacheStats {
        private final long hitCount;
        private final long missCount;
        private final double hitRate;
        private final long evictionCount;
        private final long size;
    }
}
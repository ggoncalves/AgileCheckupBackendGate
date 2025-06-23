package com.agilecheckup.gate.component;

import com.agilecheckup.gate.cache.CacheManager;
import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

/**
 * Dagger module for Gate-specific caching components.
 * 
 * This module provides cache-related dependencies for the API Gateway.
 * When refactoring to a separate cache module, this would be moved to
 * the cache module and imported by both Gate and Perpetua modules.
 * 
 * @author Claude (claude-opus-4-20250514)
 */
@Module
public class GateCacheModule {
    
    @Provides
    @Singleton
    public CacheManager provideCacheManager() {
        return new CacheManager();
    }
}
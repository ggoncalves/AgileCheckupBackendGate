package com.agilecheckup.gate.component;

import javax.inject.Singleton;

import com.agilecheckup.dagger.component.ServiceComponent;
import com.agilecheckup.gate.cache.CacheManager;

import dagger.Component;

/**
 * Dagger component for API Gateway that extends ServiceComponent
 * and adds Gate-specific dependencies like caching.
 * 
 * @author Claude (claude-opus-4-20250514)
 */
@Singleton
@Component(modules = {GateCacheModule.class}, dependencies = {ServiceComponent.class})
public interface GateComponent extends ServiceComponent {

  /**
   * Provides access to the cache manager.
   * 
   * @return The CacheManager instance
   */
  CacheManager cacheManager();
}
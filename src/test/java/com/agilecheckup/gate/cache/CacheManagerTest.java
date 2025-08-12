package com.agilecheckup.gate.cache;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for CacheManager.
 * 
 * @author Claude (claude-opus-4-20250514)
 */
@ExtendWith(MockitoExtension.class)
class CacheManagerTest {

  private CacheManager cacheManager;

  @BeforeEach
  void setUp() {
    cacheManager = new CacheManager();
  }

  @Test
  void get_shouldReturnEmpty_whenKeyIsNull() {
    Optional<String> result = cacheManager.get(null, String.class);

    assertThat(result).isEmpty();
  }

  @Test
  void get_shouldReturnEmpty_whenKeyIsBlank() {
    Optional<String> result = cacheManager.get("", String.class);

    assertThat(result).isEmpty();
  }

  @Test
  void get_shouldReturnEmpty_whenTypeIsNull() {
    cacheManager.put("key", "value");

    Optional<Object> result = cacheManager.get("key", null);

    assertThat(result).isEmpty();
  }

  @Test
  void get_shouldReturnEmpty_whenKeyNotFound() {
    Optional<String> result = cacheManager.get("nonexistent", String.class);

    assertThat(result).isEmpty();
  }

  @Test
  void get_shouldReturnValue_whenKeyExists() {
    String key = "testKey";
    String value = "testValue";
    cacheManager.put(key, value);

    Optional<String> result = cacheManager.get(key, String.class);

    assertThat(result).isPresent();
    assertThat(result.get()).isEqualTo(value);
  }

  @Test
  void get_shouldReturnEmpty_whenTypeMismatch() {
    String key = "testKey";
    cacheManager.put(key, "stringValue");

    Optional<Integer> result = cacheManager.get(key, Integer.class);

    assertThat(result).isEmpty();
  }

  @Test
  void put_shouldNotStore_whenKeyIsNull() {
    cacheManager.put(null, "value");

    Optional<String> result = cacheManager.get(null, String.class);

    assertThat(result).isEmpty();
  }

  @Test
  void put_shouldNotStore_whenKeyIsBlank() {
    cacheManager.put("", "value");

    Optional<String> result = cacheManager.get("", String.class);

    assertThat(result).isEmpty();
  }

  @Test
  void put_shouldNotStore_whenValueIsNull() {
    String key = "testKey";
    cacheManager.put(key, null);

    Optional<Object> result = cacheManager.get(key, Object.class);

    assertThat(result).isEmpty();
  }

  @Test
  void put_shouldStoreValue_whenKeyAndValueAreValid() {
    String key = "testKey";
    TestObject value = new TestObject("test", 123);

    cacheManager.put(key, value);
    Optional<TestObject> result = cacheManager.get(key, TestObject.class);

    assertThat(result).isPresent();
    assertThat(result.get().getName()).isEqualTo("test");
    assertThat(result.get().getValue()).isEqualTo(123);
  }

  @Test
  void evict_shouldRemoveValue_whenKeyExists() {
    String key = "testKey";
    cacheManager.put(key, "value");

    cacheManager.evict(key);
    Optional<String> result = cacheManager.get(key, String.class);

    assertThat(result).isEmpty();
  }

  @Test
  void evict_shouldNotThrow_whenKeyIsNull() {
    cacheManager.evict(null);
    // Should complete without exception
  }

  @Test
  void evict_shouldNotThrow_whenKeyIsBlank() {
    cacheManager.evict("");
    // Should complete without exception
  }

  @Test
  void clear_shouldRemoveAllValues() {
    cacheManager.put("key1", "value1");
    cacheManager.put("key2", "value2");
    cacheManager.put("key3", "value3");

    cacheManager.clear();

    assertThat(cacheManager.get("key1", String.class)).isEmpty();
    assertThat(cacheManager.get("key2", String.class)).isEmpty();
    assertThat(cacheManager.get("key3", String.class)).isEmpty();
  }

  @Test
  void getStats_shouldReturnInitialStats() {
    CacheManager.CacheStats stats = cacheManager.getStats();

    assertThat(stats.getHitCount()).isEqualTo(0);
    assertThat(stats.getMissCount()).isEqualTo(0);
    // Hit rate can be 0.0, 1.0, or NaN when no operations have been performed
    assertThat(stats.getHitRate()).satisfiesAnyOf(
        hitRate -> assertThat(hitRate).isEqualTo(0.0), hitRate -> assertThat(hitRate).isEqualTo(1.0), hitRate -> assertThat(hitRate).isNaN()
    );
    assertThat(stats.getEvictionCount()).isEqualTo(0);
    assertThat(stats.getSize()).isEqualTo(0);
  }

  @Test
  void getStats_shouldUpdateAfterOperations() {
    cacheManager.put("key1", "value1");
    cacheManager.get("key1", String.class); // Hit
    cacheManager.get("key2", String.class); // Miss

    CacheManager.CacheStats stats = cacheManager.getStats();

    assertThat(stats.getHitCount()).isEqualTo(1);
    assertThat(stats.getMissCount()).isEqualTo(1);
    assertThat(stats.getHitRate()).isEqualTo(0.5);
    assertThat(stats.getSize()).isEqualTo(1);
  }

  // Test helper class
  @lombok.AllArgsConstructor
  @lombok.Getter
  private static class TestObject {
    private final String name;
    private final int value;
  }
}
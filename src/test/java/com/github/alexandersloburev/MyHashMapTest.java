package com.github.alexandersloburev;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;


class MyHashMapTest {

  private static final Duration HANG_GUARD = Duration.ofMillis(500);

  private MyHashMap<String, Integer> map;

  @BeforeEach
  void setUp() {
    map = new MyHashMap<>();
  }

  @Nested
  @DisplayName("Constructor")
  class ConstructorTest {

    @Test
    @DisplayName("A new map is empty with size 0")
    void when_newMap_then_isEmptyAndSizeZero() {
      assertTrue(map.isEmpty());
      assertEquals(0, map.size());
    }

    @Test
    @DisplayName("A map with custom capacity is usable")
    void when_customCapacity_then_mapIsUsable() {
      MyHashMap<String, Integer> custom = new MyHashMap<>(64);
      custom.put("a", 1);
      assertEquals(1, custom.get("a"));
      assertEquals(1, custom.size());
    }

    @Test
    @DisplayName("A map with a non-power-of-two capacity still works")
    void when_nonPowerOfTwoCapacity_then_mapStillWorks() {
      MyHashMap<String, Integer> odd = new MyHashMap<>(10);
      odd.put("a", 1);
      odd.put("b", 2);
      assertEquals(1, odd.get("a"));
      assertEquals(2, odd.get("b"));
      assertEquals(2, odd.size());
    }

    @Test
    @DisplayName("Zero capacity is rejected with IllegalArgumentException")
    void when_zeroCapacity_then_throwsIllegalArgumentException() {
      assertThrows(IllegalArgumentException.class, () -> new MyHashMap<>(0));
    }

    @Test
    @DisplayName("Negative capacity is rejected with IllegalArgumentException")
    void when_negativeCapacity_then_throwsIllegalArgumentException() {
      assertThrows(IllegalArgumentException.class, () -> new MyHashMap<>(-1));
    }
  }

  @Nested
  @DisplayName("put")
  class PutTest {

    @Test
    @DisplayName("put() returns null when the key is new")
    void when_putNewKey_then_returnsNull() {
      assertNull(map.put("k", 1));
    }

    @Test
    @DisplayName("size grows after inserting new keys")
    void when_putNewKeys_then_sizeIncrements() {
      map.put("a", 1);
      map.put("b", 2);
      assertEquals(2, map.size());
    }

    @Test
    @DisplayName("put() returns the previous value when the key already exists")
    void when_putExistingKey_then_returnsPreviousValue() {
      map.put("k", 1);
      assertEquals(1, map.put("k", 2));
    }

    @Test
    @DisplayName("size does not change when overwriting an existing key")
    void when_putExistingKey_then_sizeDoesNotChange() {
      map.put("k", 1);
      map.put("k", 2);
      map.put("k", 3);
      assertEquals(1, map.size());
    }

    @Test
    @DisplayName("get() returns the latest value after an overwrite")
    void when_putExistingKey_then_getReturnsLatestValue() {
      map.put("k", 1);
      map.put("k", 2);
      assertEquals(2, map.get("k"));
    }

    @Test
    @DisplayName("A null key is stored and retrievable")
    void when_putNullKey_then_keyStoredAndRetrievable() {
      map.put(null, 42);
      assertEquals(42, map.get(null));
      assertEquals(1, map.size());
    }

    @Test
    @DisplayName("A null key is overwritten, not duplicated")
    void when_putNullKeyTwice_then_overwrittenNotDuplicated() {
      map.put(null, 1);
      assertEquals(1, map.put(null, 2));
      assertEquals(2, map.get(null));
      assertEquals(1, map.size());
    }

    @Test
    @DisplayName("A null value is stored and returned by get()")
    void when_putNullValue_then_storedAndReturnedByGet() {
      map.put("k", null);
      assertNull(map.get("k"));
      assertTrue(map.containsKey("k"));
      assertEquals(1, map.size());
    }

    @Test
    @DisplayName("Overwriting a null value returns null and keeps size stable")
    void when_putOverwritesNullValue_then_returnsNullAndSizeStable() {
      map.put("k", null);
      assertNull(map.put("k", 1));
      assertEquals(1, map.size());
      assertEquals(1, map.get("k"));
    }

    @Test
    @DisplayName(
        "Equal keys passed as different instances are treated as the same key")
    void
    when_putEqualKeysAsDifferentInstances_then_treatedAsSameKey() {
      map.put(new String("key"), 1);
      map.put(new String("key"), 2);
      assertEquals(1, map.size());
      assertEquals(2, map.get("key"));
    }

    @Test
    @DisplayName("Colliding string keys are all retrievable")
    void when_putCollidingStringKeys_then_allRetrievable() {
      // "Aa", "BB", "C#" share hashCode 2112
      map.put("Aa", 1);
      map.put("BB", 2);
      map.put("C#", 3);
      assertEquals(3, map.size());
      assertEquals(1, map.get("Aa"));
      assertEquals(2, map.get("BB"));
      assertEquals(3, map.get("C#"));
    }

    @Test
    @DisplayName(
        "Keys with the same hashCode but not equal are stored separately")
    void
    when_putSameHashButNotEqualKeys_then_storedSeparately() {
      map.put("Aa", 1);
      map.put("BB", 2);
      assertEquals(2, map.size());
    }
  }

  @Nested
  @DisplayName("get")
  class GetTest {

    @Test
    @DisplayName("get() returns the value for an existing key")
    void when_getExistingKey_then_returnsValue() {
      map.put("k", 5);
      assertEquals(5, map.get("k"));
    }

    @Test
    @DisplayName("get() returns null for an absent key")
    void when_getAbsentKey_then_returnsNull() {
      map.put("a", 1);
      assertNull(map.get("b"));
    }

    @Test
    @DisplayName("get() returns null on an empty map")
    void when_getFromEmptyMap_then_returnsNull() {
      assertNull(map.get("anything"));
    }

    @Test
    @DisplayName("get() finds a value using an equal key instance")
    void when_getWithEqualKeyInstance_then_findsValue() {
      map.put("key", 7);
      assertEquals(7, map.get(new String("key")));
    }

    @Test
    @DisplayName("get() returns null for an absent null key")
    void when_getNullKey_when_absent_then_returnsNull() {
      assertNull(map.get(null));
    }

    @Test
    @DisplayName("get() finds a value at the tail of a deep chain")
    void when_getFromDeepChain_then_findsTailValue() {
      MyHashMap<CollisionKey, Integer> m = chain(5);
      assertEquals(4, m.get(CollisionKey.of(4)));
    }
  }

  @Nested
  @DisplayName("remove")
  class RemoveTest {

    @Test
    @DisplayName(
        "remove() returns the value and empties the map for a single element")
    void
    when_removeOnlyElement_then_returnsValueAndMapEmpty() {
      map.put("k", 5);
      assertEquals(5, map.remove("k"));
      assertEquals(0, map.size());
      assertTrue(map.isEmpty());
      assertNull(map.get("k"));
    }

    @Test
    @DisplayName("remove() returns null and keeps size for an absent key")
    void when_removeAbsentKey_then_returnsNullAndSizeUnchanged() {
      map.put("k", 1);
      assertNull(map.remove("missing"));
      assertEquals(1, map.size());
    }

    @Test
    @DisplayName("remove() returns null on an empty map")
    void when_removeFromEmptyMap_then_returnsNull() {
      assertNull(map.remove("k"));
      assertEquals(0, map.size());
    }

    @Test
    @DisplayName("remove() of a chain head keeps the rest of the chain intact")
    void when_removeHeadOfChain_then_returnsValueAndRestIntact() {
      MyHashMap<CollisionKey, Integer> m = chain(3);
      Integer removed = assertTimeoutPreemptively(
          HANG_GUARD, () -> m.remove(CollisionKey.of(0)));
      assertEquals(0, removed);
      assertEquals(2, m.size());
      assertNull(m.get(CollisionKey.of(0)));
      assertEquals(1, m.get(CollisionKey.of(1)));
      assertEquals(2, m.get(CollisionKey.of(2)));
    }

    @Test
    @DisplayName(
        "remove() of a chain middle keeps the rest of the chain intact")
    void
    when_removeMiddleOfChain_then_returnsValueAndRestIntact() {
      MyHashMap<CollisionKey, Integer> m = chain(3);
      Integer removed = assertTimeoutPreemptively(
          HANG_GUARD, () -> m.remove(CollisionKey.of(1)));
      assertEquals(1, removed);
      assertEquals(2, m.size());
      assertEquals(0, m.get(CollisionKey.of(0)));
      assertEquals(2, m.get(CollisionKey.of(2)));
      assertNull(m.get(CollisionKey.of(1)));
    }

    @Test
    @DisplayName("remove() of a chain tail keeps the rest of the chain intact")
    void when_removeTailOfChain_then_returnsValueAndRestIntact() {
      MyHashMap<CollisionKey, Integer> m = chain(3);
      Integer removed = assertTimeoutPreemptively(
          HANG_GUARD, () -> m.remove(CollisionKey.of(2)));
      assertEquals(2, removed);
      assertEquals(2, m.size());
      assertEquals(0, m.get(CollisionKey.of(0)));
      assertEquals(1, m.get(CollisionKey.of(1)));
    }

    @Test
    @DisplayName("remove() of an absent key in an occupied bucket returns null")
    void when_removeAbsentKeyFromOccupiedBucket_then_returnsNull() {
      MyHashMap<CollisionKey, Integer> m = chain(2);
      Integer removed = assertTimeoutPreemptively(
          HANG_GUARD, () -> m.remove(CollisionKey.of(99)));
      assertNull(removed);
      assertEquals(2, m.size());
    }

    @Test
    @DisplayName("Removing a chain from tail to head removes all elements")
    void when_removeChainFromTailToHead_then_allRemoved() {
      MyHashMap<CollisionKey, Integer> m = chain(3);
      assertEquals(2, assertTimeoutPreemptively(
                          HANG_GUARD, () -> m.remove(CollisionKey.of(2))));
      assertEquals(1, assertTimeoutPreemptively(
                          HANG_GUARD, () -> m.remove(CollisionKey.of(1))));
      assertEquals(0, assertTimeoutPreemptively(
                          HANG_GUARD, () -> m.remove(CollisionKey.of(0))));
      assertTrue(m.isEmpty());
    }

    @Test
    @DisplayName("remove() returns the value for a present null key")
    void when_removeNullKey_when_present_then_returnsValue() {
      map.put(null, 9);
      assertEquals(
          9, assertTimeoutPreemptively(HANG_GUARD, () -> map.remove(null)));
      assertEquals(0, map.size());
      assertNull(map.get(null));
    }

    @Test
    @DisplayName("remove() returns null for an absent null key")
    void when_removeNullKey_when_absent_then_returnsNull() {
      assertNull(map.remove(null));
      assertEquals(0, map.size());
    }

    @Test
    @DisplayName("Putting the same key after remove() stores it again")
    void when_putSameKeyAfterRemove_then_keyStoredAgain() {
      map.put("k", 1);
      map.remove("k");
      assertNull(map.put("k", 2));
      assertEquals(2, map.get("k"));
      assertEquals(1, map.size());
    }
  }

  @Nested
  @DisplayName("containsKey")
  class ContainsKeyTest {

    @Test
    @DisplayName("containsKey() returns true for a present key")
    void when_containsKeyPresent_then_true() {
      map.put("k", 1);
      assertTrue(map.containsKey("k"));
    }

    @Test
    @DisplayName("containsKey() returns false for an absent key")
    void when_containsKeyAbsent_then_false() {
      map.put("k", 1);
      assertFalse(map.containsKey("other"));
    }

    @Test
    @DisplayName("containsKey() returns false on an empty map")
    void when_containsKeyOnEmptyMap_then_false() {
      assertFalse(map.containsKey("k"));
    }

    @Test
    @DisplayName("containsKey() returns true for an equal key instance")
    void when_containsKeyWithEqualInstance_then_true() {
      map.put("key", 1);
      assertTrue(map.containsKey(new String("key")));
    }

    @Test
    @DisplayName("containsKey() returns true for a present null key")
    void when_containsKeyNullKey_when_present_then_true() {
      map.put(null, 1);
      assertTrue(map.containsKey(null));
    }

    @Test
    @DisplayName("containsKey() returns false for an absent null key")
    void when_containsKeyNullKey_when_absent_then_false() {
      assertFalse(map.containsKey(null));
    }

    @Test
    @DisplayName("containsKey() is true for chain members and false for others")
    void when_containsKeyInDeepChain_then_trueForMembersFalseForOthers() {
      MyHashMap<CollisionKey, Integer> m = chain(5);
      assertTrue(m.containsKey(CollisionKey.of(4)));
      assertFalse(m.containsKey(CollisionKey.of(99)));
    }

    @Test
    @DisplayName("containsKey() returns false after removal")
    void when_containsKeyAfterRemove_then_false() {
      map.put("k", 1);
      map.remove("k");
      assertFalse(map.containsKey("k"));
    }

    @Test
    @DisplayName("containsKey() returns true for a key mapped to null")
    void when_containsKeyWithNullValue_then_true() {
      map.put("k", null);
      assertTrue(map.containsKey("k"));
    }
  }

  @Nested
  @DisplayName("size / isEmpty")
  class SizeAndIsEmptyTest {

    @Test
    @DisplayName("A new map has size 0")
    void when_newMap_then_sizeZero() {
      assertEquals(0, map.size());
    }

    @Test
    @DisplayName("size is correct after puts, an overwrite and a removal")
    void when_putOverwriteAndRemove_then_sizeCorrect() {
      map.put("a", 1);
      map.put("b", 2);
      map.put("a", 3);
      map.remove("b");
      assertEquals(1, map.size());
    }

    @Test
    @DisplayName("isEmpty() is true after removing all entries")
    void when_removeAllEntries_then_isEmptyTrue() {
      map.put("a", 1);
      map.put("b", 2);
      map.remove("a");
      map.remove("b");
      assertTrue(map.isEmpty());
    }
  }

  @Nested
  @DisplayName("Volume tests")
  class VolumeTest {

    @Test
    @DisplayName("A thousand entries are all retrievable")
    void when_putThousandEntries_then_allRetrievable() {
      for (int i = 0; i < 1_000; ++i) {
        assertNull(map.put("key" + i, i));
      }
      assertEquals(1_000, map.size());
      for (int i = 0; i < 1_000; ++i) {
        assertEquals(i, map.get("key" + i));
      }
    }

    @Test
    @DisplayName("After removing half of the entries the rest remain intact")
    void when_removeHalfEntries_then_remainingIntact() {
      for (int i = 0; i < 200; ++i) {
        map.put("k" + i, i);
      }
      assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
        for (int i = 0; i < 200; i += 2) {
          assertEquals(i, map.remove("k" + i));
        }
      });
      assertEquals(100, map.size());
      for (int i = 0; i < 200; ++i) {
        if (i % 2 == 0) {
          assertNull(map.get("k" + i));
        } else {
          assertEquals(i, map.get("k" + i));
        }
      }
    }
  }

  @Nested
  @DisplayName("Differential test against java.util.HashMap (oracle)")
  class OracleTest {

    @Test
    @DisplayName("Random operations match java.util.HashMap behaviour")
    void when_randomOperations_then_behaviorMatchesReference() {
      MyHashMap<Integer, String> my = new MyHashMap<>();
      Map<Integer, String> ref = new HashMap<>();
      Random rnd = new Random(42);

      for (int i = 0; i < 300; ++i) {
        int key = rnd.nextInt(64);
        String value = "v" + rnd.nextInt(1_000);
        assertEquals(ref.put(key, value), my.put(key, value));
        assertEquals(ref.size(), my.size());
      }

      for (int key = 0; key < 64; ++key) {
        assertEquals(ref.get(key), my.get(key));
        assertEquals(ref.containsKey(key), my.containsKey(key));
      }

      assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
        for (int i = 0; i < 40; ++i) {
          int key = rnd.nextInt(64);
          assertEquals(ref.remove(key), my.remove(key));
          assertEquals(ref.size(), my.size());
        }
      });

      for (int key = 0; key < 64; key++) {
        // final verification
        assertEquals(ref.get(key), my.get(key));
      }
      assertEquals(ref.size(), my.size());
    }
  }

  @Nested
  @DisplayName("resize")
  class ResizeTest {

    @Test
    @DisplayName("All entries survive growth beyond the load factor")
    void when_sizeExceedsThreshold_then_allEntriesSurviveResize() {
      MyHashMap<Integer, Integer> m = new MyHashMap<>();
      for (int i = 0; i < 1_000; ++i) {
        assertNull(m.put(i, i));
      }
      assertEquals(1_000, m.size());
      for (int i = 0; i < 1_000; ++i) {
        assertEquals(i, m.get(i));
      }
    }

    @Test
    @DisplayName(
        "A bucket is correctly split into low and high halves on resize")
    void
    when_resizeSplitsBucket_then_bothHalvesRetrievable() {
      // hash 0 and hash 16 share bucket 0 at capacity 16 (mask 0b1111);
      // on resize to 32 the new bit 0b10000 splits them into buckets 0 and 16.
      // threshold(16) = 12, so a resize happens mid-fill with a 13-node chain.
      MyHashMap<CollisionKey, Integer> m = new MyHashMap<>();
      for (int i = 0; i < 20; ++i) {
        m.put(new CollisionKey(i, (i % 2 == 0) ? 0 : 16), i);
      }
      assertEquals(20, m.size());
      for (int i = 0; i < 20; ++i) {
        assertEquals(i, m.get(new CollisionKey(i, (i % 2 == 0) ? 0 : 16)));
      }
    }

    @Test
    @DisplayName("Overwrites after a resize are still found")
    void when_putAfterResize_then_entriesRetrievable() {
      MyHashMap<String, Integer> m = new MyHashMap<>();
      for (int i = 0; i < 100; ++i) {
        m.put("k" + i, i);
      }
      for (int i = 0; i < 100; ++i) {
        m.put("k" + i, i + 100);
      }
      assertEquals(100, m.size());
      for (int i = 0; i < 100; ++i) {
        assertEquals(i + 100, m.get("k" + i));
      }
    }
  }

  @Nested
  @DisplayName("clear")
  class ClearTest {

    @Test
    @DisplayName("clear() on an empty map has no effect and throws nothing")
    void when_clearEmptyMap_then_sizeZeroAndNoException() {
      map.clear();
      assertTrue(map.isEmpty());
      assertEquals(0, map.size());
      assertNull(map.get("k"));
    }

    @Test
    @DisplayName("clear() resets size to 0 and makes the map empty")
    void when_clearNonEmptyMap_then_sizeZeroAndIsEmpty() {
      map.put("a", 1);
      map.put("b", 2);
      map.put("c", 3);
      map.clear();
      assertEquals(0, map.size());
      assertTrue(map.isEmpty());
    }

    @Test
    @DisplayName("clear() removes all previously stored entries")
    void when_clearNonEmptyMap_then_allEntriesGone() {
      // covers a null key, a null value and a collision chain ("Aa"/"BB", hash
      // 2112)
      map.put("a", 1);
      map.put(null, 2);
      map.put("c", null);
      map.put("Aa", 4);
      map.put("BB", 5);
      map.clear();
      assertNull(map.get("a"));
      assertNull(map.get(null));
      assertNull(map.get("c"));
      assertNull(map.get("Aa"));
      assertNull(map.get("BB"));
      assertFalse(map.containsKey("a"));
      assertFalse(map.containsKey(null));
      assertFalse(map.containsKey("BB"));
    }

    @Test
    @DisplayName("remove() returns null for any key after clear()")
    void when_removeAfterClear_then_returnsNull() {
      map.put("k", 1);
      map.clear();
      assertNull(map.remove("k"));
      assertEquals(0, map.size());
    }

    @Test
    @DisplayName(
        "put() of a former key after clear() is treated as a brand-new key")
    void
    when_putSameKeyAfterClear_then_treatedAsNewKey() {
      map.put("k", 1);
      map.clear();
      assertNull(map.put("k", 2));
      assertEquals(1, map.size());
      assertEquals(2, map.get("k"));
    }

    @Test
    @DisplayName(
        "The map is fully usable after clear(): put/get/overwrite/remove")
    void
    when_operationsAfterClear_then_mapWorks() {
      map.put("a", 1);
      map.put("b", 2);
      map.clear();
      assertNull(map.put("x", 10));
      assertEquals(10, map.get("x"));
      assertEquals(10, map.put("x", 11)); // overwrite still works
      assertEquals(1, map.size());
      assertEquals(11, map.remove("x")); // remove still works
      assertTrue(map.isEmpty());
    }

    @Test
    @DisplayName("clear() after growth (resizes) keeps the map consistent "
                 + "and refillable")
    void
    when_clearAfterResize_then_mapConsistentAndRefillable() {
      // Grow well beyond the threshold to trigger several resizes,
      // then verify that clear() resets the resize state coherently:
      // refilling must not lose entries (threshold vs table.length consistency)
      for (int i = 0; i < 500; ++i) {
        map.put("k" + i, i);
      }
      assertEquals(500, map.size());

      map.clear();
      assertTrue(map.isEmpty());

      for (int i = 0; i < 300; ++i) {
        assertNull(map.put("j" + i, i));
      }
      assertEquals(300, map.size());
      for (int i = 0; i < 300; ++i) {
        assertEquals(i, map.get("j" + i));
      }
    }

    @Test
    @DisplayName("clear() is idempotent")
    void when_clearTwice_then_noExceptionAndStillEmpty() {
      map.put("a", 1);
      map.clear();
      map.clear();
      assertEquals(0, map.size());
      assertTrue(map.isEmpty());
    }

    @Test
    @DisplayName("A long collision chain is cleared entirely")
    void when_clearMapWithChains_then_allChainKeysGone() {
      MyHashMap<CollisionKey, Integer> m = chain(10);
      assertEquals(10, m.size());
      m.clear();
      assertEquals(0, m.size());
      for (int i = 0; i < 10; ++i) {
        assertNull(m.get(CollisionKey.of(i)));
        assertFalse(m.containsKey(CollisionKey.of(i)));
      }
    }
  }

  /**
   * A key with a controllable hashCode: all instances sharing the same hash
   * are guaranteed to land in the same bucket. equals() compares by id, which
   * lets us build chains of distinct keys inside a single bucket.
   */
  private static final class CollisionKey {
    final int id;
    final int hash;

    private CollisionKey(int id, int hash) {
      this.id = id;
      this.hash = hash;
    }

    static CollisionKey of(int id) { return new CollisionKey(id, 0); }

    @Override
    public int hashCode() {
      return hash;
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof CollisionKey && ((CollisionKey)o).id == id;
    }
  }

  /** Builds a map with n keys in one bucket */
  private MyHashMap<CollisionKey, Integer> chain(int n) {
    MyHashMap<CollisionKey, Integer> my = new MyHashMap<>();
    for (int i = 0; i < n; ++i) {
      my.put(CollisionKey.of(i), i);
    }
    return my;
  }
}
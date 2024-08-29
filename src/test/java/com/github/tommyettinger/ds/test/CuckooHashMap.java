/*
 * Copyright (c) 2024 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.github.tommyettinger.ds.test;

import com.github.tommyettinger.digital.BitConversion;
import com.github.tommyettinger.random.AceRandom;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Cuckoo hash table based implementation of the <tt>Map</tt> interface. This
 * implementation provides all the optional map operations, and permits
 * <code>null</code> values and the <code>null</code> key. This class makes no
 * guarantees as to the order of the map; in particular, it does not guarantee
 * that the order will remain constant over time.
 * <p>
 * This implementation provides constant-time performance for most basic operations
 * (including but not limited to <tt>get</tt> and <tt>put</tt>). Specifically,
 * the implementation guarantees O(1) time performance on <tt>get</tt> calls and
 * amortized O(1) on <tt>put</tt>.
 * <p>
 * Iterating over the collection requires a time proportional to the capacity
 * of the map. The default capacity of an empty map is 16. The map will resize
 * its internal capacity whenever it grows past the load factor specified for the
 * current instance. The default load factor for this map is <code>0.45</code>.
 * Beware that this implementation can only guarantee non-amortized O(1) on
 * <tt>get</tt> iff the load factor is relatively low (generally below 0.60).
 * For more details, it's interesting to read <a href="http://www.it-c.dk/people/pagh/papers/cuckoo-jour.pdf">the
 * original Cuckoo Hash Map paper</a>.
 * <p>
 * If many mappings are to be stored in a <tt>CuckooHashMap</tt> instance, creating
 * it with a sufficiently large capacity will allow the mappings to be stored more
 * efficiently than letting it perform automatic rehashing as needed to grow the table.
 * <p>
 * Note that this implementation is not synchronized and not thread safe. If you need
 * thread safety, you'll need to implement your own locking around the map or wrap
 * the instance around a call to {@link Collections#synchronizedMap(Map)}.
 *
 * @param <K>  the type of keys maintained by this map
 * @param <V>  the type of mapped values
 */
@SuppressWarnings("WeakerAccess")
public class CuckooHashMap<K, V> extends AbstractMap<K, V> implements Map<K, V> {
  // TODO implement Cloneable, Serializable and fail fast iterators.

  private static final int THRESHOLD_LOOP = 8;
  private static final int DEFAULT_START_SIZE = 16;
  private static final float DEFAULT_LOAD_FACTOR = 0.45f;

  private int defaultStartSize = DEFAULT_START_SIZE;
  private float loadFactor = DEFAULT_LOAD_FACTOR;

  private final HashFunctionFactory hashFunctionFactory;
  private HashFunction hashFunction1;
  private HashFunction hashFunction2;

  private int size = 0;

  /**
   * Immutable container of entries in the map.
   */
  private static class MapEntry<V1> {
    final Object key;
    final V1 value;

    MapEntry(final Object key, final V1 value) {
      this.key = key;
      this.value = value;
    }
  }

  /**
   * Used as an internal key in the internal map in place of `null` keys supplied
   * by the user.
   *
   * We're only interested in this object's hashcode. The `equals` method
   * is used for convenience over implementing the same checks in the actual
   * hashmap implementation and makes for an elegant implementation.
   */
  private static final Object KEY_NULL = new Object() {
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override
    public boolean equals(Object obj) {
      return obj == this || obj == null;
    }
  };

  public interface HashFunction {
    int hash(Object obj);
  }

  public interface HashFunctionFactory {
    HashFunction generate(int buckets);
  }

  private static class DefaultHashFunctionFactory implements HashFunctionFactory {
    private static final AceRandom RANDOM = new AceRandom();

    /**
	 * From Mikkel Thorup in "String Hashing for Linear Probing."
	 * <a href="http://www.diku.dk/summer-school-2014/course-material/mikkel-thorup/hash.pdf_copy">...</a>
	 */
    private static class DefaultHashFunction implements HashFunction {
      final int a;
      final int b;
      final int hashBits;

      DefaultHashFunction(int a, int b, int buckets) {
        this.a = a == 0 ? 12345 : a;
        this.b = b == 0 ? 56789 : b;

        // Find the position of the most-significant bit; this will determine the number of bits
        // we need to set in the hash function.
        // This only works when buckets is a power of two, but that's every time here.
        hashBits = -BitConversion.countTrailingZeros(buckets);
      }

      @Override
      public int hash(final Object obj) {
        // The Cuckoo hashing this uses fails egregiously if two hashes collide over all bits.
        // Using identity hash codes would give us a much better chance of avoiding that.
//        final int h = System.identityHashCode(obj);

        final int h = obj.hashCode();

        // Shift the product down so that only `hashBits` bits remain in the output.
        return BitConversion.imul(h, a) + BitConversion.imul((h << 16 | h >>> 16), b) >>> hashBits;
      }
    }

    @Override
    public HashFunction generate(int buckets) {
      return new DefaultHashFunction(RANDOM.nextInt(), RANDOM.nextInt(), buckets);
    }
  }

  private MapEntry<V>[] T1;
  private MapEntry<V>[] T2;

  /**
   * Constructs an empty <tt>CuckooHashMap</tt> with the default initial capacity (16).
   */
  public CuckooHashMap() {
    this(DEFAULT_START_SIZE, DEFAULT_LOAD_FACTOR, new DefaultHashFunctionFactory());
  }

  /**
   * Constructs an empty <tt>CuckooHashMap</tt> with the specified initial capacity.
   * The given capacity will be rounded to the nearest power of two.
   *
   * @param initialCapacity  the initial capacity.
   */
  public CuckooHashMap(int initialCapacity) {
    this(initialCapacity, DEFAULT_LOAD_FACTOR, new DefaultHashFunctionFactory());
  }

  /**
   * Constructs an empty <tt>CuckooHashMap</tt> with the specified load factor.
   *
   * The load factor will cause the Cuckoo hash map to double in size when the number
   * of items it contains has filled up more than <tt>loadFactor</tt>% of the available
   * space.
   *
   * @param loadFactor  the load factor.
   */
  public CuckooHashMap(float loadFactor) {
    this(DEFAULT_START_SIZE, loadFactor, new DefaultHashFunctionFactory());
  }

  @SuppressWarnings("unchecked")
  public CuckooHashMap(int initialCapacity, float loadFactor, HashFunctionFactory hashFunctionFactory) {
    if (initialCapacity <= 0) {
      throw new IllegalArgumentException("initial capacity must be strictly positive");
    }
    if (loadFactor <= 0.f || loadFactor > 1.f) {
      throw new IllegalArgumentException("load factor must be a value in the (0.0f, 1.0f] range.");
    }

    size = 0;
    defaultStartSize = Math.max(2, 0x80000000 >>> BitConversion.countLeadingZeros(initialCapacity));

    // Capacity is meant to be the total capacity of the two internal tables.
    T1 = new MapEntry[defaultStartSize / 2];
    T2 = new MapEntry[defaultStartSize / 2];

    this.loadFactor = loadFactor;
    this.hashFunctionFactory = hashFunctionFactory;

    regenHashFunctions(defaultStartSize / 2);
  }

  @Override
  public boolean containsKey(Object key) {
    return get(key) != null;
  }

  @Override
  public V get(Object key) {
    return get(key, null);
  }

  public V getOrDefault(Object key, V defaultValue) {
    return get(key, defaultValue);
  }

  private V get(Object key, V defaultValue) {
    Object actualKey = key != null ? key : KEY_NULL;

    MapEntry<V> v1 = T1[hashFunction1.hash(actualKey)];
    if (v1 != null && v1.key.equals(actualKey)) {
      return v1.value;
    }

    MapEntry<V> v2 = T2[hashFunction2.hash(actualKey)];
    if (v2 != null && v2.key.equals(actualKey)) {
      return v2.value;
    }

    return defaultValue;
  }

  @SuppressWarnings("unchecked")
  @Override
  public V put(K key, V value) {
    Object actualKey = (key != null ? key : KEY_NULL);

    final V old = get(actualKey);
    if (old == null) {
      // If we need to grow after adding this item, it's probably best to grow before we add it.
      final float currentLoad = (size() + 1) / (T1.length + T2.length);
      if (currentLoad >= loadFactor) {
        grow();
      }
    }

    MapEntry<V> v;

    while ((v = putSafe(actualKey, value)) != null) {
      actualKey = v.key;
      value = v.value;
      if (!rehash()) {
        grow();
      }
    }

    if (old == null) {
      // Do not increase the size if we're replacing the item.
      size++;
    }

    return old;
  }

  /**
   * @return the key we failed to move because of collisions or <tt>null</tt> if
   * successful.
   */
  private MapEntry<V> putSafe(Object key, V value) {
    MapEntry<V> newV, t1, t2;
    int loop = 0;

    while (loop++ < THRESHOLD_LOOP) {
      newV = new MapEntry<>(key, value);
      t1 = T1[hashFunction1.hash(key)];
      t2 = T2[hashFunction2.hash(key)];

      // Check if we must just update the value first.
      if (t1 != null && t1.key.equals(key)) {
        T1[hashFunction1.hash(key)] = newV;
        return null;
      }
      if (t2 != null && t2.key.equals(key)) {
        T2[hashFunction2.hash(key)] = newV;
        return null;
      }

      // We're intentionally biased towards adding items in T1 since that leads to
      // slightly faster successful lookups.
      if (t1 == null) {
        T1[hashFunction1.hash(key)] = newV;
        return null;
      } else if (t2 == null) {
        T2[hashFunction2.hash(key)] = newV;
        return null;
      } else {
        // Both tables have an item in the required position, we need to move things around.
        // Prefer always moving from T1 for simplicity.
        key = t1.key;
        value= t1.value;
        T1[hashFunction1.hash(key)] = newV;
      }
    }

    return new MapEntry<>(key, value);
  }

  @Override
  public V remove(Object key) {
    // TODO halve the size of the hashmap when we delete enough keys.
    Object actualKey = (key != null ? key : KEY_NULL);

    MapEntry<V> v1 = T1[hashFunction1.hash(actualKey)];
    MapEntry<V> v2 = T2[hashFunction2.hash(actualKey)];
    V oldValue;

    if (v1 != null && v1.key.equals(actualKey)) {
      oldValue = T1[hashFunction1.hash(actualKey)].value;
      T1[hashFunction1.hash(actualKey)] = null;
      size--;
      return oldValue;
    }

    if (v2 != null && v2.key.equals(actualKey)) {
      oldValue = T2[hashFunction2.hash(actualKey)].value;
      T2[hashFunction2.hash(actualKey)] = null;
      size--;
      return oldValue;
    }

    return null;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void clear() {
    size = 0;
    T1 = new MapEntry[defaultStartSize / 2];
    T2 = new MapEntry[defaultStartSize / 2];
    regenHashFunctions(defaultStartSize / 2);
  }

  private void regenHashFunctions(final int size) {
    hashFunction1 = hashFunctionFactory.generate(size);
    hashFunction2 = hashFunctionFactory.generate(size);
  }

  /**
   * Double the size of the map until we can successfully manage to re-add all the items
   * we currently contain.
   */
  private void grow() {
    int newSize = T1.length;
    do {
      newSize <<= 1;
    } while (!grow(newSize));
  }

  @SuppressWarnings("unchecked")
  private boolean grow(final int newSize) {
    // Save old state as we may need to restore it if the grow fails.
    MapEntry<V>[] oldT1 = T1;
    MapEntry<V>[] oldT2 = T2;
    HashFunction oldH1 = hashFunction1;
    HashFunction oldH2 = hashFunction2;

    // Already point T1 and T2 to the new tables since putSafe operates on them.
    T1 = new MapEntry[newSize];
    T2 = new MapEntry[newSize];

    regenHashFunctions(newSize);

    for (int i = 0; i < oldT1.length; i++) {
      if (oldT1[i] != null) {
        if (putSafe(oldT1[i].key, oldT1[i].value) != null) {
          T1 = oldT1;
          T2 = oldT2;
          hashFunction1 = oldH1;
          hashFunction2 = oldH2;
          return false;
        }
      }
      if (oldT2[i] != null) {
        if (putSafe(oldT2[i].key, oldT2[i].value) != null) {
          T1 = oldT1;
          T2 = oldT2;
          hashFunction1 = oldH1;
          hashFunction2 = oldH2;
          return false;
        }
      }
    }

    return true;
  }

  @SuppressWarnings("unchecked")
  private boolean rehash() {
    // Save old state as we may need to restore it if the grow fails.
    MapEntry<V>[] oldT1 = T1;
    MapEntry<V>[] oldT2 = T2;
    HashFunction oldH1 = hashFunction1;
    HashFunction oldH2 = hashFunction2;

    boolean success;

    for (int threshold = 0; threshold < THRESHOLD_LOOP; threshold++) {
      success = true;
      hashFunction1 = hashFunctionFactory.generate(T1.length);
      hashFunction2 = hashFunctionFactory.generate(T1.length);

      // Already point T1 and T2 to the new tables since putSafe operates on them.
      T1 = new MapEntry[oldT1.length];
      T2 = new MapEntry[oldT2.length];

      for (int i = 0; i < oldT1.length; i++) {
        if (oldT1[i] != null) {
          if (putSafe(oldT1[i].key, oldT1[i].value) != null) {
            // Restore state, we need to change hash function.
            T1 = oldT1;
            T2 = oldT2;
            hashFunction1 = oldH1;
            hashFunction2 = oldH2;
            success = false;
            break;
          }
        }
        if (oldT2[i] != null) {
          if (putSafe(oldT2[i].key, oldT2[i].value) != null) {
            // Restore state, we need to change hash function.
            T1 = oldT1;
            T2 = oldT2;
            hashFunction1 = oldH1;
            hashFunction2 = oldH2;
            success = false;
            break;
          }
        }
      }

      if (success) {
        return true;
      }
    }

    return false;
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public boolean isEmpty() {
    return size == 0;
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m) {
    for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
      put(entry.getKey(), entry.getValue());
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public Set<K> keySet() {
    Set<K> set = new HashSet<>(size);
    for (int i = 0; i < T1.length; i++) {
      if (T1[i] != null) {
        if (KEY_NULL.equals(T1[i].key)) {
          set.add(null);
        } else {
          set.add((K) T1[i].key);
        }
      }
      if (T2[i] != null) {
        if (KEY_NULL.equals(T2[i].key)) {
          set.add(null);
        } else {
          set.add((K) T2[i].key);
        }
      }
    }
    return set;
  }

  @Override
  public Collection<V> values() {
    List<V> values = new ArrayList<>(size);

    // Since we must not return the values in a specific order, it's more efficient to
    // iterate over each array individually so we can exploit cache locality rather than
    // reuse the index over T1 and T2.
    for (int i = 0; i < T1.length; i++) {
      if (T1[i] != null) {
        values.add(T1[i].value);
      }
    }
    for (int i = 0; i < T2.length; i++) {
      if (T2[i] != null) {
        values.add(T2[i].value);
      }
    }
    return values;
  }

  @Override
  public Set<Entry<K, V>> entrySet() {
    Set<Entry<K, V>> entrySet = new HashSet<>(size);
    for (K key : keySet()) {
      entrySet.add(new SimpleEntry<>(key, get(key)));
    }

    return entrySet;
  }

  @Override
  public boolean containsValue(Object value) {
    for (int i = 0; i < T1.length; i++) {
      if (T1[i] != null && T1[i].value.equals(value)) {
        return true;
      }
    }
    for (int i = 0; i < T2.length; i++) {
      if (T2[i] != null && T2[i].value.equals(value)) {
        return true;
      }
    }
    return false;
  }

  private static int roundPowerOfTwo(int n) {
    n--;

    n |= n >>> 1;
    n |= n >>> 2;
    n |= n >>> 4;
    n |= n >>> 8;
    n |= n >>> 16;

    return (n < 0) ? 1 : n + 1;
  }
}
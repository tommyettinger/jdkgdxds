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
import com.github.tommyettinger.ds.Utilities;

import java.util.*;

/**
 * Cuckoo hash table based implementation of the <tt>Map</tt> interface. This
 * implementation provides all the optional map operations, and permits
 * <code>null</code> values. This class makes no
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
 * If many mappings are to be stored in a <tt>CustomCuckooMap</tt> instance, creating
 * it with a sufficiently large capacity will allow the mappings to be stored more
 * efficiently than letting it perform automatic rehashing as needed to grow the table.
 * <p>
 * Note that this implementation is not synchronized and not thread safe. If you need
 * thread safety, you'll need to implement your own locking around the map or wrap
 * the instance around a call to {@link Collections#synchronizedMap(Map)}.
 * <p>
 * This is derived from <a href="https://github.com/ivgiuliani/cuckoohash">this Github repo</a>
 * by Ivan Giuliani.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 */
public class CustomCuckooMap<K, V> extends AbstractMap<K, V> implements Map<K, V> {

	protected static final int THRESHOLD_LOOP = 8;
	protected static final int DEFAULT_START_SIZE = 16;
	protected static final float DEFAULT_LOAD_FACTOR = 0.45f;

	private float loadFactor;

	protected int shift;
	protected long hashFunction1;
	protected long hashFunction2;

	protected int size = 0;

	private Map.Entry<K, V>[] T1;
	private Map.Entry<K, V>[] T2;

	/**
	 * Constructs an empty <tt>CustomCuckooMap</tt> with the default initial capacity (16).
	 */
	public CustomCuckooMap () {
		this(DEFAULT_START_SIZE, DEFAULT_LOAD_FACTOR);
	}

	/**
	 * Constructs an empty <tt>CustomCuckooMap</tt> with the specified initial capacity.
	 * The given capacity will be rounded to the nearest power of two.
	 *
	 * @param initialCapacity the initial capacity.
	 */
	public CustomCuckooMap (int initialCapacity) {
		this(initialCapacity, DEFAULT_LOAD_FACTOR);
	}

	/**
	 * Constructs an empty <tt>CustomCuckooMap</tt> with the specified load factor.
	 * <p>
	 * The load factor will cause the Cuckoo hash map to double in size when the number
	 * of items it contains has filled up more than <tt>loadFactor</tt>% of the available
	 * space.
	 *
	 * @param loadFactor the load factor.
	 */
	public CustomCuckooMap (float loadFactor) {
		this(DEFAULT_START_SIZE, loadFactor);
	}

	@SuppressWarnings("unchecked")
	public CustomCuckooMap (int initialCapacity, float loadFactor) {
		if (initialCapacity <= 0) {
			throw new IllegalArgumentException("initial capacity must be strictly positive");
		}
		if (loadFactor <= 0.f || loadFactor > 1.f) {
			throw new IllegalArgumentException("load factor must be a value in the (0.0f, 1.0f] range.");
		}

		size = 0;
		// half the table size used by linear-probing tables, due to ~ instead of -
		int defaultStartSize = 1 << ~BitConversion.countLeadingZeros(Math.max(2, initialCapacity) - 1);
		shift = BitConversion.countLeadingZeros(defaultStartSize - 1L);
		// Capacity is meant to be the total capacity of the two internal tables.
		T1 = new Map.Entry[defaultStartSize];
		T2 = new Map.Entry[defaultStartSize];

		this.loadFactor = loadFactor;

		regenHashFunctions(defaultStartSize);
	}

	@Override
	public boolean containsKey (Object key) {
		return get(key) != null;
	}

	@Override
	public V get (Object key) {
		return get(key, null);
	}

	@Override
	public V getOrDefault (Object key, V defaultValue) {
		return get(key, defaultValue);
	}

	private V get (Object key, V defaultValue) {

		int hc = key.hashCode();
		Map.Entry<K, V> v1 = T1[(int)(hashFunction1 * hc >>> shift)];
		if (v1 != null && v1.getKey().equals(key)) {
			return v1.getValue();
		}

		Map.Entry<K, V> v2 = T2[(int)(hashFunction2 * hc >>> shift)];
		if (v2 != null && v2.getKey().equals(key)) {
			return v2.getValue();
		}

		return defaultValue;
	}

	@Override
	public V put (K key, V value) {

		final V old = get(key);
		if (old == null) {
			// If we need to grow after adding this item, it's probably best to grow before we add it.
			if (size() + 1 >= loadFactor * (T1.length << 1)) {
				grow();
			}
		}
		K k;
		while ((k = putSafe(key, value)) != null) {
			key = k;
			value = get(k, null);
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
	private K putSafe (K key, V value) {
		Map.Entry<K, V> newV;
		int loop = 0;

		while (loop++ < THRESHOLD_LOOP) {
			newV = new SimpleEntry<>(key, value);
			int hc = key.hashCode();
			int hr1 = (int)(hashFunction1 * hc >>> shift);
			int hr2 = (int)(hashFunction2 * hc >>> shift);
			Map.Entry<K, V> t1 = T1[hr1];
			Map.Entry<K, V> t2 = T2[hr2];

			// Check if we must just update the value first.
			if (t1 != null && t1.getKey().equals(key)) {
				T1[hr1] = newV;
				return null;
			}
			if (t2 != null && t2.getKey().equals(key)) {
				T2[hr2] = newV;
				return null;
			}

			// We're intentionally biased towards adding items in T1 since that leads to
			// slightly faster successful lookups.
			if (t1 == null) {
				T1[hr1] = newV;
				return null;
			} else if (t2 == null) {
				T2[hr2] = newV;
				return null;
			} else {
				// Both tables have an item in the required position, we need to move things around.
				// Prefer always moving from T1 for simplicity.
				key = t1.getKey();
				value = t1.getValue();
				T1[(int)(hashFunction1 * key.hashCode() >>> shift)] = newV;
			}
		}

		return key;
	}

	@Override
	public V remove (Object key) {
		if (key == null)
			return null;
		int hc = key.hashCode();
		int hr1 = (int)(hashFunction1 * hc >>> shift);
		int hr2 = (int)(hashFunction2 * hc >>> shift);
		Map.Entry<K, V> v1 = T1[hr1];
		Map.Entry<K, V> v2 = T2[hr2];
		V oldValue;

		if (v1 != null && v1.getKey().equals(key)) {
			oldValue = T1[hr1].getValue();
			T1[hr1] = null;
			size--;
			return oldValue;
		}

		if (v2 != null && v2.getKey().equals(key)) {
			oldValue = T2[hr2].getValue();
			T2[hr2] = null;
			size--;
			return oldValue;
		}

		return null;
	}

	@Override
	public void clear () {
		size = 0;
		Arrays.fill(T1, null);
		Arrays.fill(T2, null);
		regenHashFunctions(T1.length);
	}

	private void regenHashFunctions (final int size) {
		int idx1 = (int)(-(hashFunction2 ^ ((size + hashFunction2) * hashFunction2 | 5L)) >>> 56);
		int idx2 = (int)(-(hashFunction1 ^ ((size + hashFunction1) * hashFunction1 | 5L)) >>> 56) | 256;
		hashFunction1 = Utilities.GOOD_MULTIPLIERS[idx1];
		hashFunction2 = Utilities.GOOD_MULTIPLIERS[idx2];
		shift = BitConversion.countLeadingZeros(size - 1L);
	}

	/**
	 * Double the size of the map until we can successfully manage to re-add all the items
	 * we currently contain.
	 */
	private void grow () {
		int newSize = T1.length;
		do {
			newSize <<= 1;
		} while (!grow(newSize));
	}

	@SuppressWarnings("unchecked")
	private boolean grow (final int newSize) {
		// Save old state as we may need to restore it if the grow fails.
		Map.Entry<K, V>[] oldT1 = T1;
		Map.Entry<K, V>[] oldT2 = T2;
		long oldH1 = hashFunction1;
		long oldH2 = hashFunction2;

		// Already point T1 and T2 to the new tables since putSafe operates on them.
		T1 = new Map.Entry[newSize];
		T2 = new Map.Entry[newSize];

		regenHashFunctions(newSize);

		for (int i = 0; i < oldT1.length; i++) {
			if (oldT1[i] != null) {
				if (putSafe(oldT1[i].getKey(), oldT1[i].getValue()) != null) {
					T1 = oldT1;
					T2 = oldT2;
					hashFunction1 = oldH1;
					hashFunction2 = oldH2;
					return false;
				}
			}
			if (oldT2[i] != null) {
				if (putSafe(oldT2[i].getKey(), oldT2[i].getValue()) != null) {
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
	private boolean rehash () {
		// Save old state as we may need to restore it if the grow fails.
		Map.Entry<K, V>[] oldT1 = T1;
		Map.Entry<K, V>[] oldT2 = T2;
		long oldH1 = hashFunction1;
		long oldH2 = hashFunction2;

		boolean success;

		for (int threshold = 0; threshold < THRESHOLD_LOOP; threshold++) {
			success = true;
			regenHashFunctions(T1.length);

			// Already point T1 and T2 to the new tables since putSafe operates on them.
			T1 = new Map.Entry[oldT1.length];
			T2 = new Map.Entry[oldT2.length];

			for (int i = 0; i < oldT1.length; i++) {
				if (oldT1[i] != null) {
					if (putSafe(oldT1[i].getKey(), oldT1[i].getValue()) != null) {
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
					if (putSafe(oldT2[i].getKey(), oldT2[i].getValue()) != null) {
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
	public int size () {
		return size;
	}

	@Override
	public boolean isEmpty () {
		return size == 0;
	}

	@Override
	public void putAll (Map<? extends K, ? extends V> m) {
		for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
			put(entry.getKey(), entry.getValue());
		}
	}

	@Override
	public Set<K> keySet () {
		Set<K> set = new HashSet<>(size);
		for (int i = 0; i < T1.length; i++) {
			if (T1[i] != null) {
				set.add(T1[i].getKey());
			}
		}
		for (int i = 0; i < T2.length; i++) {
			if (T2[i] != null) {
				set.add(T2[i].getKey());
			}
		}
		return set;
	}

	@Override
	public Collection<V> values () {
		List<V> values = new ArrayList<>(size);

		// Since we must not return the values in a specific order, it's more efficient to
		// iterate over each array individually so that we can exploit cache locality rather than
		// reuse the index over T1 and T2.
		for (int i = 0; i < T1.length; i++) {
			if (T1[i] != null) {
				values.add(T1[i].getValue());
			}
		}
		for (int i = 0; i < T2.length; i++) {
			if (T2[i] != null) {
				values.add(T2[i].getValue());
			}
		}
		return values;
	}

	@Override
	public Set<Entry<K, V>> entrySet () {
		Set<Entry<K, V>> set = new HashSet<>(size);
		for (int i = 0; i < T1.length; i++) {
			if (T1[i] != null) {
				set.add(new SimpleImmutableEntry<>(T1[i]));
			}
		}
		for (int i = 0; i < T2.length; i++) {
			if (T2[i] != null) {
				set.add(new SimpleImmutableEntry<>(T2[i]));
			}
		}
		return set;
	}

	@Override
	public boolean containsValue (Object value) {
		for (int i = 0; i < T1.length; i++) {
			if (T1[i] != null && T1[i].getValue().equals(value)) {
				return true;
			}
		}
		for (int i = 0; i < T2.length; i++) {
			if (T2[i] != null && T2[i].getValue().equals(value)) {
				return true;
			}
		}
		return false;
	}

}
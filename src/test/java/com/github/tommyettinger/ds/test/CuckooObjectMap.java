/*
 * Copyright (c) 2008-2025 See AUTHORS file.
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
 */

package com.github.tommyettinger.ds.test;

import com.github.tommyettinger.ds.Utilities;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An unordered map. This implementation is a cuckoo hash map using 3 hashes (if table size is less than 2^16) or 4 hashes (if
 * table size is greater than or equal to 2^16), random walking, and a small stash for problematic keys Null keys are not allowed.
 * Null values are allowed. No allocation is done except when growing the table size. <br>
 * <br>
 * This map performs very fast get, containsKey, and remove (typically O(1), worst case O(log(n))). Put may be a bit slower,
 * depending on hash collisions. Load factors greater than 0.91 greatly increase the chances the map will have to rehash to the
 * next higher POT size.
 *
 * @author Nathan Sweet
 */
public class CuckooObjectMap<K, V> {
	// primes for hash functions 2, 3, and 4
	private static final int PRIME2 = 0xf48c5;// 0xbe1f14b1;
	private static final int PRIME3 = 0x8aee1;// 0xb4b82e39;
	private static final int PRIME4 = 0xcb91d;// 0xced1c241;

	static int random = 1;

	public int size;

	K[] keyTable;
	V[] valueTable;
	int capacity, stashSize;

	private float loadFactor;
	private int hashShift, mask, threshold;
	private int stashCapacity;
	private int pushIterations;
	private boolean isBigTable;

	/**
	 * Creates a new map with an initial capacity of 32 and a load factor of {@link Utilities#getDefaultLoadFactor()}. This map will hold 25 items before growing the
	 * backing table.
	 */
	public CuckooObjectMap() {
		this(32, Utilities.getDefaultLoadFactor());
	}

	/**
	 * Creates a new map with a load factor of {@link Utilities#getDefaultLoadFactor()}. This map will hold initialCapacity * {@link Utilities#getDefaultLoadFactor()} items before growing the backing
	 * table.
	 */
	public CuckooObjectMap(int initialCapacity) {
		this(initialCapacity, Utilities.getDefaultLoadFactor());
	}

	/**
	 * Creates a new map with the specified initial capacity and load factor. This map will hold initialCapacity * loadFactor
	 * items before growing the backing table.
	 */
	public CuckooObjectMap(int initialCapacity, float loadFactor) {
		if (initialCapacity < 0) throw new IllegalArgumentException("initialCapacity must be >= 0: " + initialCapacity);
		if (initialCapacity > 1 << 30)
			throw new IllegalArgumentException("initialCapacity is too large: " + initialCapacity);
		capacity = nextPowerOfTwo(initialCapacity);

		if (loadFactor <= 0) throw new IllegalArgumentException("loadFactor must be > 0: " + loadFactor);
		this.loadFactor = loadFactor;

		// big table is when capacity >= 2^16
		isBigTable = (capacity >>> 16) != 0;

		threshold = (int) (capacity * loadFactor);
		mask = capacity - 1;
		hashShift = 31 - Integer.numberOfTrailingZeros(capacity);
		stashCapacity = Math.max(3, (int) Math.ceil(Math.log(capacity)) * 2);
		pushIterations = Math.max(Math.min(capacity, 8), (int) Math.sqrt(capacity) / 8);

		keyTable = (K[]) new Object[capacity + stashCapacity];
		valueTable = (V[]) new Object[keyTable.length];
	}

	/**
	 * Creates a new map identical to the specified map.
	 */
	public CuckooObjectMap(CuckooObjectMap<? extends K, ? extends V> map) {
		this(map.capacity, map.loadFactor);
		stashSize = map.stashSize;
		System.arraycopy(map.keyTable, 0, keyTable, 0, map.keyTable.length);
		System.arraycopy(map.valueTable, 0, valueTable, 0, map.valueTable.length);
		size = map.size;
	}

	/**
	 * Returns the old value associated with the specified key, or null.
	 */
	public V put(K key, V value) {
		if (key == null) throw new IllegalArgumentException("key cannot be null.");
		return put_internal(key, value);
	}

	private V put_internal(K key, V value) {
		// avoid getfield opcode
		K[] keyTable = this.keyTable;
		int mask = this.mask;
		boolean isBigTable = this.isBigTable;

		// Check for existing keys.
		int hashCode = key.hashCode();
		int index1 = hashCode & mask;
		K key1 = keyTable[index1];
		if (key.equals(key1)) {
			V oldValue = valueTable[index1];
			valueTable[index1] = value;
			return oldValue;
		}

		int index2 = hash2(hashCode);
		K key2 = keyTable[index2];
		if (key.equals(key2)) {
			V oldValue = valueTable[index2];
			valueTable[index2] = value;
			return oldValue;
		}

		int index3 = hash3(hashCode);
		K key3 = keyTable[index3];
		if (key.equals(key3)) {
			V oldValue = valueTable[index3];
			valueTable[index3] = value;
			return oldValue;
		}

		int index4 = -1;
		K key4 = null;
		if (isBigTable) {
			index4 = hash4(hashCode);
			key4 = keyTable[index4];
			if (key.equals(key4)) {
				V oldValue = valueTable[index4];
				valueTable[index4] = value;
				return oldValue;
			}
		}

		// Update key in the stash.
		for (int i = capacity, n = i + stashSize; i < n; i++) {
			if (key.equals(keyTable[i])) {
				V oldValue = valueTable[i];
				valueTable[i] = value;
				return oldValue;
			}
		}

		// Check for empty buckets.
		if (key1 == null) {
			keyTable[index1] = key;
			valueTable[index1] = value;
			if (size++ >= threshold) resize(capacity << 1);
			return null;
		}

		if (key2 == null) {
			keyTable[index2] = key;
			valueTable[index2] = value;
			if (size++ >= threshold) resize(capacity << 1);
			return null;
		}

		if (key3 == null) {
			keyTable[index3] = key;
			valueTable[index3] = value;
			if (size++ >= threshold) resize(capacity << 1);
			return null;
		}

		if (isBigTable && key4 == null) {
			keyTable[index4] = key;
			valueTable[index4] = value;
			if (size++ >= threshold) resize(capacity << 1);
			return null;
		}

		push(key, value, index1, key1, index2, key2, index3, key3, index4, key4);
		return null;
	}

	public void putAll(CuckooObjectMap<K, V> map) {
		ensureCapacity(map.size);
		for (Entry<K, V> entry : map.entries())
			put(entry.key, entry.value);
	}

	/**
	 * Skips checks for existing keys.
	 */
	private void putResize(K key, V value) {
		// Check for empty buckets.
		int hashCode = key.hashCode();
		int index1 = hashCode & mask;
		K key1 = keyTable[index1];
		if (key1 == null) {
			keyTable[index1] = key;
			valueTable[index1] = value;
			if (size++ >= threshold) resize(capacity << 1);
			return;
		}

		int index2 = hash2(hashCode);
		K key2 = keyTable[index2];
		if (key2 == null) {
			keyTable[index2] = key;
			valueTable[index2] = value;
			if (size++ >= threshold) resize(capacity << 1);
			return;
		}

		int index3 = hash3(hashCode);
		K key3 = keyTable[index3];
		if (key3 == null) {
			keyTable[index3] = key;
			valueTable[index3] = value;
			if (size++ >= threshold) resize(capacity << 1);
			return;
		}

		int index4 = -1;
		K key4 = null;
		if (isBigTable) {
			index4 = hash4(hashCode);
			key4 = keyTable[index4];
			if (key4 == null) {
				keyTable[index4] = key;
				valueTable[index4] = value;
				if (size++ >= threshold) resize(capacity << 1);
				return;
			}
		}

		push(key, value, index1, key1, index2, key2, index3, key3, index4, key4);
	}

	private void push(K insertKey, V insertValue, int index1, K key1, int index2, K key2, int index3, K key3, int index4,
					  K key4) {
		// avoid getfield opcode
		K[] keyTable = this.keyTable;
		V[] valueTable = this.valueTable;
		int mask = this.mask;
		boolean isBigTable = this.isBigTable;

		// Push keys until an empty bucket is found.
		K evictedKey;
		V evictedValue;
		int i = 0, pushIterations = this.pushIterations;
		int n = isBigTable ? 4 : 3;
		do {
			random = random * 0x4F1BB ^ 0x7F4A7C15;
			// Replace the key and value for one of the hashes.
			switch ((random >>> 16) % n) {
				case 0:
					evictedKey = key1;
					evictedValue = valueTable[index1];
					keyTable[index1] = insertKey;
					valueTable[index1] = insertValue;
					break;
				case 1:
					evictedKey = key2;
					evictedValue = valueTable[index2];
					keyTable[index2] = insertKey;
					valueTable[index2] = insertValue;
					break;
				case 2:
					evictedKey = key3;
					evictedValue = valueTable[index3];
					keyTable[index3] = insertKey;
					valueTable[index3] = insertValue;
					break;
				default:
					evictedKey = key4;
					evictedValue = valueTable[index4];
					keyTable[index4] = insertKey;
					valueTable[index4] = insertValue;
					break;
			}

			// If the evicted key hashes to an empty bucket, put it there and stop.
			int hashCode = evictedKey.hashCode();
			index1 = hashCode & mask;
			key1 = keyTable[index1];
			if (key1 == null) {
				keyTable[index1] = evictedKey;
				valueTable[index1] = evictedValue;
				if (size++ >= threshold) resize(capacity << 1);
				return;
			}

			index2 = hash2(hashCode);
			key2 = keyTable[index2];
			if (key2 == null) {
				keyTable[index2] = evictedKey;
				valueTable[index2] = evictedValue;
				if (size++ >= threshold) resize(capacity << 1);
				return;
			}

			index3 = hash3(hashCode);
			key3 = keyTable[index3];
			if (key3 == null) {
				keyTable[index3] = evictedKey;
				valueTable[index3] = evictedValue;
				if (size++ >= threshold) resize(capacity << 1);
				return;
			}

			if (isBigTable) {
				index4 = hash4(hashCode);
				key4 = keyTable[index4];
				if (key4 == null) {
					keyTable[index4] = evictedKey;
					valueTable[index4] = evictedValue;
					if (size++ >= threshold) resize(capacity << 1);
					return;
				}
			}

			if (++i == pushIterations) break;

			insertKey = evictedKey;
			insertValue = evictedValue;
		} while (true);

		putStash(evictedKey, evictedValue);
	}

	private void putStash(K key, V value) {
		if (stashSize == stashCapacity) {
			// Too many pushes occurred and the stash is full, increase the table size.
			resize(capacity << 1);
			put_internal(key, value);
			return;
		}
		// Store key in the stash.
		int index = capacity + stashSize;
		keyTable[index] = key;
		valueTable[index] = value;
		stashSize++;
		size++;
	}

	public V get(K key) {
		int hashCode = key.hashCode();
		int index = hashCode & mask;
		if (!key.equals(keyTable[index])) {
			index = hash2(hashCode);
			if (!key.equals(keyTable[index])) {
				index = hash3(hashCode);
				if (!key.equals(keyTable[index])) {
					if (isBigTable) {
						index = hash4(hashCode);
						if (!key.equals(keyTable[index])) return getStash(key);
					} else {
						return getStash(key);
					}
				}
			}
		}
		return valueTable[index];
	}

	private V getStash(K key) {
		K[] keyTable = this.keyTable;
		for (int i = capacity, n = i + stashSize; i < n; i++)
			if (key.equals(keyTable[i])) return valueTable[i];
		return null;
	}

	/**
	 * Returns the value for the specified key, or the default value if the key is not in the map.
	 */
	public V get(K key, V defaultValue) {
		int hashCode = key.hashCode();
		int index = hashCode & mask;
		if (!key.equals(keyTable[index])) {
			index = hash2(hashCode);
			if (!key.equals(keyTable[index])) {
				index = hash3(hashCode);
				if (!key.equals(keyTable[index])) {
					if (isBigTable) {
						index = hash4(hashCode);
						if (!key.equals(keyTable[index])) return getStash(key, defaultValue);
					} else {
						return getStash(key, defaultValue);
					}
				}
			}
		}
		return valueTable[index];
	}

	private V getStash(K key, V defaultValue) {
		K[] keyTable = this.keyTable;
		for (int i = capacity, n = i + stashSize; i < n; i++)
			if (key.equals(keyTable[i])) return valueTable[i];
		return defaultValue;
	}

	public V remove(K key) {
		int hashCode = key.hashCode();
		int index = hashCode & mask;
		if (key.equals(keyTable[index])) {
			keyTable[index] = null;
			V oldValue = valueTable[index];
			valueTable[index] = null;
			size--;
			return oldValue;
		}

		index = hash2(hashCode);
		if (key.equals(keyTable[index])) {
			keyTable[index] = null;
			V oldValue = valueTable[index];
			valueTable[index] = null;
			size--;
			return oldValue;
		}

		index = hash3(hashCode);
		if (key.equals(keyTable[index])) {
			keyTable[index] = null;
			V oldValue = valueTable[index];
			valueTable[index] = null;
			size--;
			return oldValue;
		}

		if (isBigTable) {
			index = hash4(hashCode);
			if (key.equals(keyTable[index])) {
				keyTable[index] = null;
				V oldValue = valueTable[index];
				valueTable[index] = null;
				size--;
				return oldValue;
			}
		}

		return removeStash(key);
	}

	V removeStash(K key) {
		K[] keyTable = this.keyTable;
		for (int i = capacity, n = i + stashSize; i < n; i++) {
			if (key.equals(keyTable[i])) {
				V oldValue = valueTable[i];
				removeStashIndex(i);
				size--;
				return oldValue;
			}
		}
		return null;
	}

	void removeStashIndex(int index) {
		// If the removed location was not last, move the last tuple to the removed location.
		stashSize--;
		int lastIndex = capacity + stashSize;
		if (index < lastIndex) {
			keyTable[index] = keyTable[lastIndex];
			valueTable[index] = valueTable[lastIndex];
			valueTable[lastIndex] = null;
		} else
			valueTable[index] = null;
	}

	/**
	 * Reduces the size of the backing arrays to be the specified capacity or less. If the capacity is already less, nothing is
	 * done. If the map contains more items than the specified capacity, the next highest power of two capacity is used instead.
	 */
	public void shrink(int maximumCapacity) {
		if (maximumCapacity < 0) throw new IllegalArgumentException("maximumCapacity must be >= 0: " + maximumCapacity);
		if (size > maximumCapacity) maximumCapacity = size;
		if (capacity <= maximumCapacity) return;
		maximumCapacity = nextPowerOfTwo(maximumCapacity);
		resize(maximumCapacity);
	}

	/**
	 * Clears the map and reduces the size of the backing arrays to be the specified capacity if they are larger.
	 */
	public void clear(int maximumCapacity) {
		if (capacity <= maximumCapacity) {
			clear();
			return;
		}
		size = 0;
		resize(maximumCapacity);
	}

	public void clear() {
		K[] keyTable = this.keyTable;
		V[] valueTable = this.valueTable;
		for (int i = capacity + stashSize; i-- > 0; ) {
			keyTable[i] = null;
			valueTable[i] = null;
		}
		size = 0;
		stashSize = 0;
	}

	/**
	 * Returns true if the specified value is in the map. Note this traverses the entire map and compares every value, which may
	 * be an expensive operation.
	 *
	 * @param identity If true, uses == to compare the specified value with values in the map. If false, uses
	 *                 {@link #equals(Object)}.
	 */
	public boolean containsValue(Object value, boolean identity) {
		V[] valueTable = this.valueTable;
		if (value == null) {
			K[] keyTable = this.keyTable;
			for (int i = capacity + stashSize; i-- > 0; )
				if (keyTable[i] != null && valueTable[i] == null) return true;
		} else if (identity) {
			for (int i = capacity + stashSize; i-- > 0; )
				if (valueTable[i] == value) return true;
		} else {
			for (int i = capacity + stashSize; i-- > 0; )
				if (value.equals(valueTable[i])) return true;
		}
		return false;
	}

	public boolean containsKey(K key) {
		int hashCode = key.hashCode();
		int index = hashCode & mask;
		if (!key.equals(keyTable[index])) {
			index = hash2(hashCode);
			if (!key.equals(keyTable[index])) {
				index = hash3(hashCode);
				if (!key.equals(keyTable[index])) {
					if (isBigTable) {
						index = hash4(hashCode);
						if (!key.equals(keyTable[index])) return containsKeyStash(key);
					} else {
						return containsKeyStash(key);
					}
				}
			}
		}
		return true;
	}

	private boolean containsKeyStash(K key) {
		K[] keyTable = this.keyTable;
		for (int i = capacity, n = i + stashSize; i < n; i++)
			if (key.equals(keyTable[i])) return true;
		return false;
	}

	/**
	 * Returns the key for the specified value, or null if it is not in the map. Note this traverses the entire map and compares
	 * every value, which may be an expensive operation.
	 *
	 * @param identity If true, uses == to compare the specified value with values in the map. If false, uses
	 *                 {@link #equals(Object)}.
	 */
	public K findKey(Object value, boolean identity) {
		V[] valueTable = this.valueTable;
		if (value == null) {
			K[] keyTable = this.keyTable;
			for (int i = capacity + stashSize; i-- > 0; )
				if (keyTable[i] != null && valueTable[i] == null) return keyTable[i];
		} else if (identity) {
			for (int i = capacity + stashSize; i-- > 0; )
				if (valueTable[i] == value) return keyTable[i];
		} else {
			for (int i = capacity + stashSize; i-- > 0; )
				if (value.equals(valueTable[i])) return keyTable[i];
		}
		return null;
	}

	/**
	 * Increases the size of the backing array to acommodate the specified number of additional items. Useful before adding many
	 * items to avoid multiple backing array resizes.
	 */
	public void ensureCapacity(int additionalCapacity) {
		int sizeNeeded = size + additionalCapacity;
		if (sizeNeeded >= threshold) resize(nextPowerOfTwo((int) (sizeNeeded / loadFactor)));
	}

	private void resize(int newSize) {
		int oldEndIndex = capacity + stashSize;

		capacity = newSize;
		threshold = (int) (newSize * loadFactor);
		mask = newSize - 1;
		hashShift = 31 - Integer.numberOfTrailingZeros(newSize);
		stashCapacity = Math.max(3, (int) Math.ceil(Math.log(newSize)) * 2);
		pushIterations = Math.max(Math.min(newSize, 8), (int) Math.sqrt(newSize) / 8);

		// big table is when capacity >= 2^16
		isBigTable = (capacity >>> 16) != 0;

		K[] oldKeyTable = keyTable;
		V[] oldValueTable = valueTable;

		keyTable = (K[]) new Object[newSize + stashCapacity];
		valueTable = (V[]) new Object[newSize + stashCapacity];

		int oldSize = size;
		size = 0;
		stashSize = 0;
		if (oldSize > 0) {
			for (int i = 0; i < oldEndIndex; i++) {
				K key = oldKeyTable[i];
				if (key != null) putResize(key, oldValueTable[i]);
			}
		}
	}

	private int hash2(int h) {
		h *= PRIME2;
		return (h ^ h >>> hashShift) & mask;
	}

	private int hash3(int h) {
		h *= PRIME3;
		return (h ^ h >>> hashShift) & mask;
	}

	private int hash4(int h) {
		h *= PRIME4;
		return (h ^ h >>> hashShift) & mask;
	}

	public String toString() {
		if (size == 0) return "{}";
		StringBuilder buffer = new StringBuilder(32);
		buffer.append('{');
		K[] keyTable = this.keyTable;
		V[] valueTable = this.valueTable;
		int i = keyTable.length;
		while (i-- > 0) {
			K key = keyTable[i];
			if (key == null) continue;
			buffer.append(key);
			buffer.append('=');
			buffer.append(valueTable[i]);
			break;
		}
		while (i-- > 0) {
			K key = keyTable[i];
			if (key == null) continue;
			buffer.append(", ");
			buffer.append(key);
			buffer.append('=');
			buffer.append(valueTable[i]);
		}
		buffer.append('}');
		return buffer.toString();
	}

	/**
	 * Returns an iterator for the entries in the map. Remove is supported.
	 */
	public Entries<K, V> entries() {
		return new Entries(this);
	}

	/**
	 * Returns an iterator for the values in the map. Remove is supported.
	 */
	public Values<V> values() {
		return new Values(this);
	}

	/**
	 * Returns an iterator for the keys in the map. Remove is supported.
	 */
	public Keys<K> keys() {
		return new Keys(this);
	}

	static public class Entry<K, V> {
		public K key;
		public V value;

		public String toString() {
			return key + "=" + value;
		}
	}

	static private class MapIterator<K, V> {
		public boolean hasNext;

		final CuckooObjectMap<K, V> map;
		int nextIndex, currentIndex;

		public MapIterator(CuckooObjectMap<K, V> map) {
			this.map = map;
			reset();
		}

		public void reset() {
			currentIndex = -1;
			nextIndex = -1;
			advance();
		}

		void advance() {
			hasNext = false;
			K[] keyTable = map.keyTable;
			for (int n = map.capacity + map.stashSize; ++nextIndex < n; ) {
				if (keyTable[nextIndex] != null) {
					hasNext = true;
					break;
				}
			}
		}

		public void remove() {
			if (currentIndex < 0) throw new IllegalStateException("next must be called before remove.");
			if (currentIndex >= map.capacity) {
				map.removeStashIndex(currentIndex);
				nextIndex = currentIndex - 1;
				advance();
			} else {
				map.keyTable[currentIndex] = null;
				map.valueTable[currentIndex] = null;
			}
			currentIndex = -1;
			map.size--;
		}
	}

	static public class Entries<K, V> extends MapIterator<K, V> implements Iterable<Entry<K, V>>, Iterator<Entry<K, V>> {
		Entry<K, V> entry = new Entry<K, V>();

		public Entries(CuckooObjectMap<K, V> map) {
			super(map);
		}

		/**
		 * Note the same entry instance is returned each time this method is called.
		 */
		public Entry<K, V> next() {
			if (!hasNext) throw new NoSuchElementException();
			K[] keyTable = map.keyTable;
			entry.key = keyTable[nextIndex];
			entry.value = map.valueTable[nextIndex];
			currentIndex = nextIndex;
			advance();
			return entry;
		}

		public boolean hasNext() {
			return hasNext;
		}

		public Iterator<Entry<K, V>> iterator() {
			return this;
		}
	}

	static public class Values<V> extends MapIterator<Object, V> implements Iterable<V>, Iterator<V> {
		public Values(CuckooObjectMap<?, V> map) {
			super((CuckooObjectMap<Object, V>) map);
		}

		public boolean hasNext() {
			return hasNext;
		}

		public V next() {
			if (!hasNext) throw new NoSuchElementException();
			V value = map.valueTable[nextIndex];
			currentIndex = nextIndex;
			advance();
			return value;
		}

		public Iterator<V> iterator() {
			return this;
		}

		/**
		 * Returns a new array containing the remaining values.
		 */
		public ArrayList<V> toArray() {
			ArrayList array = new ArrayList(map.size);
			while (hasNext)
				array.add(next());
			return array;
		}

		/**
		 * Adds the remaining values to the specified array.
		 */
		public void toArray(ArrayList<V> array) {
			while (hasNext)
				array.add(next());
		}
	}

	static public class Keys<K> extends MapIterator<K, Object> implements Iterable<K>, Iterator<K> {
		public Keys(CuckooObjectMap<K, ?> map) {
			super((CuckooObjectMap<K, Object>) map);
		}

		public boolean hasNext() {
			return hasNext;
		}

		public K next() {
			if (!hasNext) throw new NoSuchElementException();
			K key = map.keyTable[nextIndex];
			currentIndex = nextIndex;
			advance();
			return key;
		}

		public Iterator<K> iterator() {
			return this;
		}

		/**
		 * Returns a new array containing the remaining keys.
		 */
		public ArrayList<K> toArray() {
			ArrayList array = new ArrayList(map.size);
			while (hasNext)
				array.add(next());
			return array;
		}
	}

	public static int nextPowerOfTwo(int value) {
		return 1 << -Integer.numberOfLeadingZeros(Math.max(2, value) - 1);
	}
}

/*
 * Copyright (c) 2022-2025 See AUTHORS file.
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

package com.github.tommyettinger.ds.jdk;


import java.util.*;

/**
 * A {@link HashMap} that also stores keys in an {@link ArrayList} using the insertion order.
 * <p>
 * Iteration over the {@link #entrySet()}, {@link #keySet()}, and {@link #values()} is ordered and faster than an unordered map. Keys
 * can also be accessed and the order changed using {@link #order()}. There is some additional overhead for put and remove.
 * <p>
 * Unordered sets and maps are not designed to provide especially fast iteration. Iteration is faster with {@code Ordered} types like
 * OrderedHashMap.
 *
 * @author Tommy Ettinger
 */
public class OrderedHashMap<K, V> extends HashMap<K, V> {

	protected final ArrayList<K> keys;

	transient Set<K> keySet;
	transient Collection<V> values;

	/**
	 * Creates a new map with an initial capacity of 16 and a load factor of 0.75f .
	 */
	public OrderedHashMap() {
		this(16);
	}

	/**
	 * Creates a new map with the given starting capacity and a load factor of 0.75f .
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 */
	public OrderedHashMap(int initialCapacity) {
		this(initialCapacity, 0.75f);
	}

	/**
	 * Creates a new map with the specified initial capacity and load factor. This map will hold initialCapacity items before
	 * growing the backing table.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 * @param loadFactor      what fraction of the capacity can be filled before this has to resize; 0 &lt; loadFactor &lt;= 1
	 */
	public OrderedHashMap(int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
		keys = new ArrayList<>(initialCapacity);
	}

	/**
	 * Creates a new map identical to the specified map.
	 *
	 * @param map      the map to copy
	 */
	public OrderedHashMap(Map<? extends K, ? extends V> map) {
		this(map.size());
		putAll(map);
	}

	/**
	 * Creates a new set by copying {@code count} items from the given OrderedHashMap, starting at {@code offset} in that Map,
	 * into this.
	 *
	 * @param other    another OrderedHashMap of the same type
	 * @param offset   the first index in other's ordering to draw an item from
	 * @param count    how many items to copy from other
	 */
	public OrderedHashMap(OrderedHashMap<? extends K, ? extends V> other, int offset, int count) {
		this(count);
		putAll(0, other, offset, count);
	}

	/**
	 * Given two side-by-side arrays, one of keys, one of values, this constructs a map and inserts each pair of key and value into it.
	 * If keys and values have different lengths, this only uses the length of the smaller array.
	 *
	 * @param keys     an array of keys
	 * @param values   an array of values
	 */
	public OrderedHashMap(K[] keys, V[] values) {
		this(Math.min(keys.length, values.length));
		putAll(keys, values);
	}

	/**
	 * Given two side-by-side collections, one of keys, one of values, this constructs a map and inserts each pair of key and value into it.
	 * If keys and values have different lengths, this only uses the length of the smaller collection.
	 *
	 * @param keys     a Collection of keys
	 * @param values   a Collection of values
	 */
	public OrderedHashMap(Collection<? extends K> keys, Collection<? extends V> values) {
		this(Math.min(keys.size(), values.size()));
		putAll(keys, values);
	}

	/**
	 * Creates a new map identical to the specified map.
	 *
	 * @param map the map to copy
	 */
	public OrderedHashMap(OrderedHashMap<? extends K, ? extends V> map) {
		super(map);
		keys = new ArrayList<>(map.keys);
	}

	@Override
	public V put(K key, V value) {
		int oldSize = size();
		V old = super.put(key, value);
		if(size() != oldSize)
			keys.add(key);
		return old;
	}

	/**
	 * Puts the given key and value into this map at the given index in its order.
	 * If the key is already present at a different index, it is moved to the given index and its
	 * value is set to the given value.
	 *
	 * @param key   a K key; must not be null
	 * @param value a V value; permitted to be null
	 * @param index the index in the order to place the given key and value; must be non-negative and less than {@link #size()}
	 * @return the previous value associated with key, if there was one, or null otherwise
	 */
	public V put(K key, V value, int index) {
		int oldSize = size();
		V old = super.put(key, value);
		if(size() != oldSize)
			keys.add(index, key);
		else {
			int oldIndex = keys.indexOf(key);
			if(oldIndex != index)
				keys.add(index, keys.remove(oldIndex));
		}
		return old;
	}

	public V putOrDefault(K key, V value, V defaultValue) {
		int oldSize = size();
		V old = super.put(key, value);
		if(size() != oldSize) {
			keys.add(key);
			return defaultValue;
		}
		return old;
	}

	/**
	 * Puts every key-value pair in the given map into this, with the values from the given map
	 * overwriting the previous values if two keys are identical. This will put keys in the order of the given map.
	 *
	 * @param map a map with compatible key and value types; will not be modified
	 */
	public void putAll(OrderedHashMap<? extends K, ? extends V> map) {
		for (int i = 0, kl = map.size(); i < kl; i++) {
			put(map.keyAt(i), map.getAt(i));
		}
	}

	/**
	 * Adds up to {@code count} entries, starting from {@code offset}, in the map {@code other} to this set,
	 * inserting at the end of the iteration order.
	 *
	 * @param other  a non-null ordered map with the same type and compatible generic types
	 * @param offset the first index in {@code other} to use
	 * @param count  how many indices in {@code other} to use
	 */
	public void putAll(OrderedHashMap<? extends K, ? extends V> other, int offset, int count) {
		int end = Math.min(offset + count, other.size());
		for (int i = offset; i < end; i++) {
			put(other.keyAt(i), other.getAt(i));
		}
	}

	/**
	 * Adds up to {@code count} entries, starting from {@code offset}, in the map {@code other} to this set,
	 * inserting starting at {@code insertionIndex} in the iteration order.
	 *
	 * @param insertionIndex where to insert into the iteration order
	 * @param other          a non-null ordered map with the same type and compatible generic types
	 * @param offset         the first index in {@code other} to use
	 * @param count          how many indices in {@code other} to use
	 */
	public void putAll(int insertionIndex, OrderedHashMap<? extends K, ? extends V> other, int offset, int count) {
		int end = Math.min(offset + count, other.size());
		for (int i = offset; i < end; i++) {
			put(other.keyAt(i), other.getAt(i), insertionIndex++);
		}
	}

	/**
	 * Given two side-by-side collections, one of keys, one of values, this inserts each pair of key and value into this map with put().
	 *
	 * @param keys   a Collection of keys
	 * @param values a Collection of values
	 */
	public void putAll(Collection<? extends K> keys, Collection<? extends V> values) {
		int length = Math.min(keys.size(), values.size());
		K key;
		Iterator<? extends K> ki = keys.iterator();
		Iterator<? extends V> vi = values.iterator();
		while (ki.hasNext() && vi.hasNext()) {
			key = ki.next();
			if (key != null) {
				put(key, vi.next());
			}
		}
	}

	/**
	 * Given two side-by-side arrays, one of keys, one of values, this inserts each pair of key and value into this map with put().
	 *
	 * @param keys   an array of keys
	 * @param values an array of values
	 */
	public void putAll(K[] keys, V[] values) {
		putAll(keys, 0, values, 0, Math.min(keys.length, values.length));
	}

	/**
	 * Given two side-by-side arrays, one of keys, one of values, this inserts each pair of key and value into this map with put().
	 *
	 * @param keys   an array of keys
	 * @param values an array of values
	 * @param length how many items from keys and values to insert, at-most
	 */
	public void putAll(K[] keys, V[] values, int length) {
		putAll(keys, 0, values, 0, length);
	}

	/**
	 * Given two side-by-side arrays, one of keys, one of values, this inserts each pair of key and value into this map with put().
	 *
	 * @param keys        an array of keys
	 * @param keyOffset   the first index in keys to insert
	 * @param values      an array of values
	 * @param valueOffset the first index in values to insert
	 * @param length      how many items from keys and values to insert, at-most
	 */
	public void putAll(K[] keys, int keyOffset, V[] values, int valueOffset, int length) {
		length = Math.min(length, Math.min(keys.length - keyOffset, values.length - valueOffset));
		K key;
		for (int k = keyOffset, v = valueOffset, i = 0, n = length; i < n; i++, k++, v++) {
			key = keys[k];
			put(key, values[v]);
		}
	}

	@Override
	public V remove(Object key) {
		// If key is not present, using an O(1) containsKey() lets us avoid an O(n) remove step on keys.
		if (!super.containsKey(key)) {
			return null;
		}
		keys.remove(key);
		return super.remove(key);
	}

	/**
	 * Removes the entry at the given index in the order, returning the value of that entry.
	 *
	 * @param index the index of the entry to remove; must be at least 0 and less than {@link #size()}
	 * @return the value of the removed entry
	 */
	public V removeAt(int index) {
		return super.remove(keys.remove(index));
	}

	/**
	 * Changes the key {@code before} to {@code after} without changing its position in the order or its value. Returns true if
	 * {@code after} has been added to the OrderedHashMap and {@code before} has been removed; returns false if {@code after} is
	 * already present or {@code before} is not present. If you are iterating over an OrderedHashMap and have an index, you should
	 * prefer {@link #alterAt(int, Object)}, which doesn't need to search for an index like this does and so can be faster.
	 *
	 * @param before a key that must be present for this to succeed
	 * @param after  a key that must not be in this map for this to succeed
	 * @return true if {@code before} was removed and {@code after} was added, false otherwise
	 */
	public boolean alter(K before, K after) {
		if (before == null || after == null) return false;
		if (containsKey(after)) {
			return false;
		}
		int index = keys.indexOf(before);
		if (index == -1) {
			return false;
		}
		super.put(after, super.remove(before));
		keys.set(index, after);
		return true;
	}

	/**
	 * Changes the key at the given {@code index} in the order to {@code after}, without changing the ordering of other entries or
	 * any values. If {@code after} is already present, this returns false; it will also return false if {@code index} is invalid
	 * for the size of this map. Otherwise, it returns true. Unlike {@link #alter(Object, Object)}, this operates in constant time.
	 *
	 * @param index the index in the order of the key to change; must be non-negative and less than {@link #size(}
	 * @param after the key that will replace the contents at {@code index}; this key must not be present for this to succeed
	 * @return true if {@code after} successfully replaced the key at {@code index}, false otherwise
	 */
	public boolean alterAt(int index, K after) {
		if (after == null || index < 0 || index >= size() || containsKey(after)) return false;
		super.put(after, super.remove(keys.get(index)));
		keys.set(index, after);
		return true;
	}

	/**
	 * Changes the value at a specified {@code index} in the iteration order to {@code v}, without changing keys at all.
	 * If {@code index} isn't currently a valid index in the iteration order, this returns null. Otherwise, it returns the
	 * value that was previously held at {@code index}, which may also be null.
	 *
	 * @param v     the new V value to assign
	 * @param index the index in the iteration order to set {@code v} at
	 * @return the previous value held at {@code index} in the iteration order, which may be null if the value was null or if {@code index} was invalid
	 */
	public V setAt(int index, V v) {
		if (index < 0 || index >= size()) {
			return null;
		}
		return put(keys.get(index), v);
	}

	/**
	 * Gets the V value at the given {@code index} in the insertion order. The index should be between 0
	 * (inclusive) and {@link #size()} (exclusive).
	 *
	 * @param index an index in the insertion order, between 0 (inclusive) and {@link #size()} (exclusive)
	 * @return the value at the given index
	 */
	public V getAt(int index) {
		return get(keys.get(index));
	}

	/**
	 * Gets the K key at the given {@code index} in the insertion order. The index should be between 0
	 * (inclusive) and {@link #size()} (exclusive).
	 *
	 * @param index an index in the insertion order, between 0 (inclusive) and {@link #size()} (exclusive)
	 * @return the key at the given index
	 */
	public K keyAt(int index) {
		return keys.get(index);
	}

	@Override
	public void clear() {
		keys.clear();
		super.clear();
	}

	/**
	 * Gets the ArrayList of keys in the order this class will iterate through them.
	 * Returns a direct reference to the same ArrayList this uses, so changes to the returned list will
	 * also change the iteration order here.
	 *
	 * @return the ArrayList of keys, in iteration order (usually insertion-order), that this uses
	 */
	public ArrayList<K> order() {
		return keys;
	}

	/**
	 * Sorts this OrderedHashMap in-place by the keys' natural ordering; {@code K} must implement {@link Comparable}.
	 */
	public void sort() {
		keys.sort(null);
	}

	/**
	 * Sorts this OrderedHashMap in-place by the given Comparator used on the keys. If {@code comp} is null, then this
	 * will sort by the natural ordering of the keys, which requires {@code K} to {@link Comparable}.
	 *
	 * @param comp a Comparator that can compare two {@code K} keys, or null to use the keys' natural ordering
	 */
	public void sort(Comparator<? super K> comp) {
		keys.sort(comp);
	}

	/**
	 * Sorts this OrderedHashMap in-place by the given Comparator used on the values. {@code comp} must
	 * be able to compare {@code V} values. If any null values are present in this OrderedHashMap, then comp
	 * must be able to sort or otherwise handle null values.
	 *
	 * @param comp a non-null Comparator that can compare {@code V} values
	 */
	public void sortByValue(Comparator<V> comp) {
		if (comp != null)
			keys.sort((a, b) -> comp.compare(get(a), get(b)));
	}

	/**
	 * Removes the items between the specified start index, inclusive, and end index, exclusive.
	 * Note that this takes different arguments than some other range-related methods; this needs
	 * a start index and an end index, rather than a count of items. This matches the behavior in
	 * the JDK collections.
	 *
	 * @param start the first index to remove, inclusive
	 * @param end   the last index (after what should be removed), exclusive
	 */
	public void removeRange(int start, int end) {
		start = Math.max(0, start);
		end = Math.min(keys.size(), end);
		for (int i = start; i < end; i++) {
			super.remove(keys.get(i));
		}
		keys.subList(start, end).clear();
	}

	/**
	 * Reduces the size of the map to the specified size. If the map is already smaller than the specified
	 * size, no action is taken.
	 *
	 * @param newSize the target size to try to reach by removing items, if smaller than the current size
	 */
	public void truncate(int newSize) {
		if (size() > newSize) {
			removeRange(newSize, size());
		}
	}

	/**
	 * Returns a {@link Set} view of the keys contained in this map.
	 * The set is backed by the map, so changes to the map are
	 * reflected in the set, and vice versa.  If the map is modified
	 * while an iteration over the set is in progress (except through
	 * the iterator's own {@code remove} operation), the results of
	 * the iteration are undefined.  The set supports element removal,
	 * which removes the corresponding mapping from the map, via the
	 * {@code Iterator.remove}, {@code Set.remove},
	 * {@code removeAll}, {@code retainAll}, and {@code clear}
	 * operations.  It does not support the {@code add} or {@code addAll}
	 * operations.
	 *
	 * @return a set view of the keys contained in this map
	 */
	@Override
	public Set<K> keySet() {
		if(keySet == null)
			keySet = new OrderedMapKeys();
		return keySet;
	}

	/**
	 * Returns a Collection for the values in the map. Remove is supported by the Collection's iterator.
	 *
	 * @return a {@link Collection} of V values
	 */
	@Override
	public Collection<V> values() {
		if(values == null)
			values = new OrderedMapValues();
		return values;
	}
//
//	/**
//	 * Returns a Set of Map.Entry, containing the entries in the map. Remove is supported by the Set's iterator.
//	 *
//	 * @return a {@link Set} of {@link Map.Entry} key-value pairs
//	 */
//	@Override
//	public Set<Map.Entry<K, V>> entrySet() {
//		return new OrderedMapEntries<>();
//	}
//
//	public static class OrderedMapEntries<K, V> extends Entries<K, V> {
//		protected ArrayList<K> keys;
//
//		public OrderedMapEntries(OrderedHashMap<K, V> map) {
//			super(map);
//			keys = map.keys;
//		}
//
//		@Override
//		public Map<K, V> appendInto(Map<K, V> map) {
//			MapIterator<K, V, Map.Entry<K, V>> iter = iterator();
//			while (iter.hasNext) {
//				K k = keys.get(iter.nextIndex);
//				map.put(k, iter.map.get(k));
//				iter.findNextIndex();
//			}
//			return map;
//		}
//
//		@Override
//		public MapIterator<K, V, Map.Entry<K, V>> iterator() {
//			return new MapIterator<K, V, Map.Entry<K, V>>(map) {
//				@Override
//				public MapIterator<K, V, Map.Entry<K, V>> iterator() {
//					return this;
//				}
//
//				@Override
//				public void reset() {
//					currentIndex = -1;
//					nextIndex = 0;
//					hasNext = map.size > 0;
//				}
//
//				@Override
//				public boolean hasNext() {
//					return hasNext;
//				}
//
//				@Override
//				public Entry<K, V> next() {
//					if (!hasNext) {
//						throw new NoSuchElementException();
//					}
//					currentIndex = nextIndex;
//					K k = keys.get(nextIndex);
//					Entry<K, V> entry = new Entry<>(k, map.get(k));
//					nextIndex++;
//					hasNext = nextIndex < map.size;
//					return entry;
//				}
//
//				@Override
//				public void remove() {
//					if (currentIndex < 0) {
//						throw new IllegalStateException("next must be called before remove.");
//					}
//					map.remove(keys.get(currentIndex));
//					nextIndex--;
//					currentIndex = -1;
//				}
//			};
//		}
//	}

	final class OrderedMapKeys extends AbstractSet<K> {
		@Override
		public Iterator<K> iterator() {
			return keys.iterator();
		}

		@Override
		public int size() {
			return OrderedHashMap.this.size();
		}

		@Override
		public boolean contains(Object o) {
			return containsKey(o);
		}

		@Override
		public boolean remove(Object o) {
			int oldSize = size();
			OrderedHashMap.this.remove(o);
			return size() != oldSize;
		}

		@Override
		public void clear() {
			OrderedHashMap.this.clear();
		}

		@Override
		public Object[] toArray() {
			return keys.toArray();
		}

		@Override
		public <T> T[] toArray(T[] a) {
			return keys.toArray(a);
		}
	}

	final class ValueIterator implements Iterator<V> {
		Iterator<K> keyIterator;
		K currentKey;
		public ValueIterator(){
			keyIterator = keys.iterator();
		}

		@Override
		public boolean hasNext() {
			return keyIterator.hasNext();
		}

		@Override
		public V next() {
			return get(currentKey = keyIterator.next());
		}

		@Override
		public void remove() {
			OrderedHashMap.super.remove(currentKey);
			keyIterator.remove();
		}
	}

	final class OrderedMapValues extends AbstractCollection<V> {

		@Override
		public Iterator<V> iterator() {
			return new ValueIterator();
		}

		@Override
		public int size() {
			return OrderedHashMap.this.size();
		}

		@Override
		public boolean contains(Object o) {
			return containsValue(o);
		}

		@Override
		public void clear() {
			OrderedHashMap.this.clear();
		}
	}

	/**
	 * Constructs an empty map given the types as generic type arguments.
	 * This is usually less useful than just using the constructor, but can be handy
	 * in some code-generation scenarios when you don't know how many arguments you will have.
	 *
	 * @param <K> the type of keys
	 * @param <V> the type of values
	 * @return a new map containing nothing
	 */
	public static <K, V> OrderedHashMap<K, V> with() {
		return new OrderedHashMap<>(0);
	}

	/**
	 * Constructs a single-entry map given one key and one value.
	 * This is mostly useful as an optimization for {@link #with(Object, Object, Object...)}
	 * when there's no "rest" of the keys or values.
	 *
	 * @param key0   the first and only key
	 * @param value0 the first and only value
	 * @param <K>    the type of key0
	 * @param <V>    the type of value0
	 * @return a new map containing just the entry mapping key0 to value0
	 */
	public static <K, V> OrderedHashMap<K, V> with(K key0, V value0) {
		OrderedHashMap<K, V> map = new OrderedHashMap<>(1);
		map.put(key0, value0);
		return map;
	}

	/**
	 * Constructs a single-entry map given two key-value pairs.
	 * This is mostly useful as an optimization for {@link #with(Object, Object, Object...)}
	 * when there's no "rest" of the keys or values.
	 *
	 * @param key0   a K key
	 * @param value0 a V value
	 * @param key1   a K key
	 * @param value1 a V value
	 * @param <K>    the type of key0
	 * @param <V>    the type of value0
	 * @return a new map containing entries mapping each key to the following value
	 */
	public static <K, V> OrderedHashMap<K, V> with(K key0, V value0, K key1, V value1) {
		OrderedHashMap<K, V> map = new OrderedHashMap<>(2);
		map.put(key0, value0);
		map.put(key1, value1);
		return map;
	}

	/**
	 * Constructs a single-entry map given three key-value pairs.
	 * This is mostly useful as an optimization for {@link #with(Object, Object, Object...)}
	 * when there's no "rest" of the keys or values.
	 *
	 * @param key0   a K key
	 * @param value0 a V value
	 * @param key1   a K key
	 * @param value1 a V value
	 * @param key2   a K key
	 * @param value2 a V value
	 * @param <K>    the type of key0
	 * @param <V>    the type of value0
	 * @return a new map containing entries mapping each key to the following value
	 */
	public static <K, V> OrderedHashMap<K, V> with(K key0, V value0, K key1, V value1, K key2, V value2) {
		OrderedHashMap<K, V> map = new OrderedHashMap<>(3);
		map.put(key0, value0);
		map.put(key1, value1);
		map.put(key2, value2);
		return map;
	}

	/**
	 * Constructs a single-entry map given four key-value pairs.
	 * This is mostly useful as an optimization for {@link #with(Object, Object, Object...)}
	 * when there's no "rest" of the keys or values.
	 *
	 * @param key0   a K key
	 * @param value0 a V value
	 * @param key1   a K key
	 * @param value1 a V value
	 * @param key2   a K key
	 * @param value2 a V value
	 * @param key3   a K key
	 * @param value3 a V value
	 * @param <K>    the type of key0
	 * @param <V>    the type of value0
	 * @return a new map containing entries mapping each key to the following value
	 */
	public static <K, V> OrderedHashMap<K, V> with(K key0, V value0, K key1, V value1, K key2, V value2, K key3, V value3) {
		OrderedHashMap<K, V> map = new OrderedHashMap<>(4);
		map.put(key0, value0);
		map.put(key1, value1);
		map.put(key2, value2);
		map.put(key3, value3);
		return map;
	}

	/**
	 * Constructs a map given alternating keys and values.
	 * This can be useful in some code-generation scenarios, or when you want to make a
	 * map conveniently by-hand and have it populated at the start. You can also use
	 * {@link #OrderedHashMap(Object[], Object[])}, which takes all keys and then all values.
	 * This needs all keys to have the same type and all values to have the same type, because
	 * it gets those types from the first key parameter and first value parameter. Any keys that don't
	 * have K as their type or values that don't have V as their type have that entry skipped.
	 *
	 * @param key0   the first key; will be used to determine the type of all keys
	 * @param value0 the first value; will be used to determine the type of all values
	 * @param rest   a varargs or non-null array of alternating K, V, K, V... elements
	 * @param <K>    the type of keys, inferred from key0
	 * @param <V>    the type of values, inferred from value0
	 * @return a new map containing the given keys and values
	 */
	public static <K, V> OrderedHashMap<K, V> with(K key0, V value0, Object... rest) {
		OrderedHashMap<K, V> map = new OrderedHashMap<>(1 + (rest.length >>> 1));
		map.put(key0, value0);
		map.putPairs(rest);
		return map;
	}

	/**
	 * Attempts to put alternating key-value pairs into this map, drawing a key, then a value from {@code pairs}, then
	 * another key, another value, and so on until another pair cannot be drawn. Any keys that don't
	 * have K as their type or values that don't have V as their type have that entry skipped.
	 * <br>
	 * If any item in {@code pairs} cannot be cast to the appropriate K or V type for its position in the arguments,
	 * that pair is ignored and neither that key nor value is put into the map. If any key is null, that pair is
	 * ignored, as well. If {@code pairs} is an Object array that is null, the entire call to putPairs() is ignored.
	 * If the length of {@code pairs} is odd, the last item (which will be unpaired) is ignored.
	 *
	 * @param pairs an array or varargs of alternating K, V, K, V... elements
	 */
	@SuppressWarnings("unchecked")
	public void putPairs(Object... pairs) {
		if (pairs != null) {
			for (int i = 1; i < pairs.length; i += 2) {
				try {
					if (pairs[i - 1] != null)
						put((K) pairs[i - 1], (V) pairs[i]);
				} catch (ClassCastException ignored) {
				}
			}
		}
	}
}

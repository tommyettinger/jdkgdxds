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

package com.github.tommyettinger.ds;

import com.github.tommyettinger.digital.Base;
import com.github.tommyettinger.ds.support.sort.IntComparator;
import com.github.tommyettinger.ds.support.sort.IntComparators;
import com.github.tommyettinger.ds.support.sort.LongComparator;
import com.github.tommyettinger.ds.support.sort.LongComparators;

import com.github.tommyettinger.ds.support.util.IntAppender;
import com.github.tommyettinger.ds.support.util.LongAppender;
import com.github.tommyettinger.ds.support.util.LongIterator;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import static com.github.tommyettinger.ds.Utilities.tableSize;

/**
 * A {@link LongIntMap} that also stores keys in a {@link LongList} using the insertion order. No
 * allocation is done except when growing the table size.
 * <p>
 * Iteration over the {@link #entrySet()}, {@link #keySet()}, and {@link #values()} is ordered and faster than an unordered map. Keys
 * can also be accessed and the order changed using {@link #order()}. There is some additional overhead for put and remove.
 * <p>
 * This class performs fast contains (typically O(1), worst case O(n) but that is rare in practice). Remove is somewhat slower due
 * to {@link #order()}. Add may be slightly slower, depending on hash collisions. Hashcodes are rehashed to reduce
 * collisions and the need to resize. Load factors greater than 0.91 greatly increase the chances to resize to the next higher POT
 * size.
 * <p>
 * Unordered sets and maps are not designed to provide especially fast iteration. Iteration is faster with {@link Ordered} types like
 * ObjectOrderedSet and LongIntOrderedMap.
 * <p>
 * You can customize most behavior of this map by extending it. {@link #place(long)} can be overridden to change how hashCodes
 * are calculated (which can be useful for types like {@link StringBuilder} that don't implement hashCode()), and
 * {@link #locateKey(long)} can be overridden to change how equality is calculated.
 * <p>
 * This implementation uses linear probing with the backward shift algorithm for removal.
 * It tries different hashes from a simple family, with the hash changing on resize.
 * Linear probing continues to work even when all hashCodes collide, just more slowly.
 *
 * @author Nathan Sweet
 * @author Tommy Ettinger
 */
public class LongIntOrderedMap extends LongIntMap implements Ordered.OfLong {

	protected final LongList keys;

	/**
	 * Creates a new map with an initial capacity of {@link Utilities#getDefaultTableCapacity()} and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 *
	 * @param ordering determines what implementation {@link #order()} will use
	 */
	public LongIntOrderedMap(OrderType ordering) {
		this(Utilities.getDefaultTableCapacity(), ordering);
	}

	/**
	 * Creates a new map with the given starting capacity and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 * @param ordering        determines what implementation {@link #order()} will use
	 */
	public LongIntOrderedMap(int initialCapacity, OrderType ordering) {
		this(initialCapacity, Utilities.getDefaultLoadFactor(), ordering);
	}

	/**
	 * Creates a new map with the specified initial capacity and load factor. This map will hold initialCapacity items before
	 * growing the backing table.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 * @param loadFactor      what fraction of the capacity can be filled before this has to resize; 0 &lt; loadFactor &lt;= 1
	 * @param ordering        determines what implementation {@link #order()} will use
	 */
	public LongIntOrderedMap(int initialCapacity, float loadFactor, OrderType ordering) {
		super(initialCapacity, loadFactor);
		switch (ordering) {
			case DEQUE:
				keys = new LongDeque(initialCapacity);
				break;
			case BAG:
				keys = new LongBag(initialCapacity);
				break;
			default:
				keys = new LongList(initialCapacity);
		}
	}

	/**
	 * Creates a new map identical to the specified map.
	 *
	 * @param map the map to copy
	 */
	public LongIntOrderedMap(LongIntOrderedMap map) {
		super(map);
		if (map.keys instanceof LongDeque) keys = new LongDeque((LongDeque) map.keys);
		else if (map.keys instanceof LongBag) keys = new LongBag(map.keys);
		else keys = new LongList(map.keys);
	}

	/**
	 * Creates a new map identical to the specified map.
	 *
	 * @param map      the map to copy
	 * @param ordering determines what implementation {@link #order()} will use
	 */
	public LongIntOrderedMap(LongIntMap map, OrderType ordering) {
		this(map.size(), map.loadFactor, ordering);
		hashMultiplier = map.hashMultiplier;
		LongIterator it = map.keySet().iterator();
		while (it.hasNext()) {
			long k = it.nextLong();
			put(k, map.get(k));
		}
	}

	/**
	 * Given two side-by-side arrays, one of keys, one of values, this constructs a map and inserts each pair of key and value into it.
	 * If keys and values have different lengths, this only uses the length of the smaller array.
	 *
	 * @param keys     an array of keys
	 * @param values   an array of values
	 * @param ordering determines what implementation {@link #order()} will use
	 */
	public LongIntOrderedMap(long[] keys, int[] values, OrderType ordering) {
		this(Math.min(keys.length, values.length), ordering);
		putAll(keys, values);
	}

	/**
	 * Given two side-by-side collections, one of keys, one of values, this constructs a map and inserts each pair of key and value into it.
	 * If keys and values have different lengths, this only uses the length of the smaller collection.
	 *
	 * @param keys     a PrimitiveCollection of keys
	 * @param values   a PrimitiveCollection of values
	 * @param ordering determines what implementation {@link #order()} will use
	 */
	public LongIntOrderedMap(PrimitiveCollection.OfLong keys, PrimitiveCollection.OfInt values, OrderType ordering) {
		this(Math.min(keys.size(), values.size()), ordering);
		putAll(keys, values);
	}

	/**
	 * Creates a new set by copying {@code count} items from the given LongIntOrderedMap, starting at {@code offset} in that Map,
	 * into this.
	 *
	 * @param other    another LongIntOrderedMap of the same type
	 * @param offset   the first index in other's ordering to draw an item from
	 * @param count    how many items to copy from other
	 * @param ordering determines what implementation {@link #order()} will use
	 */
	public LongIntOrderedMap(LongIntOrderedMap other, int offset, int count, OrderType ordering) {
		this(count, other.loadFactor, ordering);
		hashMultiplier = other.hashMultiplier;
		putAll(0, other, offset, count);
	}

	/**
	 * Creates a new map with an initial capacity of {@link Utilities#getDefaultTableCapacity()} and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 */
	public LongIntOrderedMap() {
		this(OrderType.LIST);
	}

	/**
	 * Creates a new map with the given starting capacity and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 */
	public LongIntOrderedMap(int initialCapacity) {
		this(initialCapacity, Utilities.getDefaultLoadFactor(), OrderType.LIST);
	}

	/**
	 * Creates a new map with the specified initial capacity and load factor. This map will hold initialCapacity items before
	 * growing the backing table.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 * @param loadFactor      what fraction of the capacity can be filled before this has to resize; 0 &lt; loadFactor &lt;= 1
	 */
	public LongIntOrderedMap(int initialCapacity, float loadFactor) {
		this(initialCapacity, loadFactor, OrderType.LIST);
	}

	/**
	 * Creates a new map identical to the specified map.
	 *
	 * @param map the map to copy
	 */
	public LongIntOrderedMap(LongIntMap map) {
		this(map, OrderType.LIST);
	}

	/**
	 * Given two side-by-side arrays, one of keys, one of values, this constructs a map and inserts each pair of key and value into it.
	 * If keys and values have different lengths, this only uses the length of the smaller array.
	 *
	 * @param keys   an array of keys
	 * @param values an array of values
	 */
	public LongIntOrderedMap(long[] keys, int[] values) {
		this(keys, values, OrderType.LIST);
	}

	/**
	 * Given two side-by-side collections, one of keys, one of values, this constructs a map and inserts each pair of key and value into it.
	 * If keys and values have different lengths, this only uses the length of the smaller collection.
	 *
	 * @param keys   a PrimitiveCollection of keys
	 * @param values a PrimitiveCollection of values
	 */
	public LongIntOrderedMap(PrimitiveCollection.OfLong keys, PrimitiveCollection.OfInt values) {
		this(keys, values, OrderType.LIST);
	}

	/**
	 * Creates a new set by copying {@code count} items from the given LongIntOrderedMap, starting at {@code offset} in that Map,
	 * into this.
	 *
	 * @param other  another LongIntOrderedMap of the same type
	 * @param offset the first index in other's ordering to draw an item from
	 * @param count  how many items to copy from other
	 */
	public LongIntOrderedMap(LongIntOrderedMap other, int offset, int count) {
		this(other, offset, count, other.keys instanceof LongBag ? OrderType.BAG
			: other.keys instanceof LongDeque ? OrderType.DEQUE
			: OrderType.LIST);
	}

	@Override
	public int put(long key, int value) {
		if (key == 0) {
			int oldValue = defaultValue;
			if (hasZeroValue) {
				oldValue = zeroValue;
			} else {
				keys.add(0);
				size++;
			}
			hasZeroValue = true;
			zeroValue = value;
			return oldValue;
		}
		int i = locateKey(key);
		if (i >= 0) { // Existing key was found.
			int oldValue = valueTable[i];
			valueTable[i] = value;
			return oldValue;
		}
		i = ~i; // Empty space was found.
		keyTable[i] = key;
		valueTable[i] = value;
		keys.add(key);
		if (++size >= threshold) {
			resize(keyTable.length << 1);
		}
		return defaultValue;
	}

	/**
	 * Puts the given key and value into this map at the given index in its order.
	 * If the key is already present at a different index, it is moved to the given index and its
	 * value is set to the given value.
	 *
	 * @param key   a long key
	 * @param value an int value
	 * @param index the index in the order to place the given key and value; must be non-negative and less than {@link #size()}
	 * @return the previous value associated with key, if there was one, or {@link #defaultValue} otherwise
	 */
	public int put(long key, int value, int index) {
		if (key == 0) {
			int oldValue = defaultValue;
			if (hasZeroValue) {
				oldValue = zeroValue;
				int oldIndex = keys.indexOf(key);
				if (oldIndex != index) {
					keys.insert(index, keys.removeAt(oldIndex));
				}
			} else {
				keys.insert(index, 0);
				size++;
			}
			hasZeroValue = true;
			zeroValue = value;
			return oldValue;
		}
		int i = locateKey(key);
		if (i >= 0) { // Existing key was found.
			int oldValue = valueTable[i];
			valueTable[i] = value;
			int oldIndex = keys.indexOf(key);
			if (oldIndex != index) {
				keys.insert(index, keys.removeAt(oldIndex));
			}
			return oldValue;
		}
		i = ~i; // Empty space was found.
		keyTable[i] = key;
		valueTable[i] = value;
		keys.insert(index, key);
		if (++size >= threshold) {
			resize(keyTable.length << 1);
		}
		return defaultValue;
	}

	@Override
	public int putOrDefault(long key, int value, int defaultValue) {
		if (key == 0) {
			int oldValue = defaultValue;
			if (hasZeroValue) {
				oldValue = zeroValue;
			} else {
				size++;
				keys.add(key);
			}
			hasZeroValue = true;
			zeroValue = value;
			return oldValue;
		}
		int i = locateKey(key);
		if (i >= 0) { // Existing key was found.
			int oldValue = valueTable[i];
			valueTable[i] = value;
			return oldValue;
		}
		i = ~i; // Empty space was found.
		keyTable[i] = key;
		valueTable[i] = value;
		keys.add(key);
		if (++size >= threshold) {
			resize(keyTable.length << 1);
		}
		return defaultValue;
	}

	/**
	 * Puts every key-value pair in the given map into this, with the values from the given map
	 * overwriting the previous values if two keys are identical. This will put keys in the order of the given map.
	 *
	 * @param map a map with compatible key and value types; will not be modified
	 */
	public void putAll(LongIntOrderedMap map) {
		ensureCapacity(map.size);
		LongList ks = map.keys;
		int kl = ks.size();
		long k;
		for (int i = 0; i < kl; i++) {
			k = ks.get(i);
			put(k, map.get(k));
		}
	}

	/**
	 * Adds up to {@code count} entries, starting from {@code offset}, in the map {@code other} to this set,
	 * inserting at the end of the iteration order.
	 *
	 * @param other  a non-null ordered map with the same type
	 * @param offset the first index in {@code other} to use
	 * @param count  how many indices in {@code other} to use
	 */
	public void putAll(LongIntOrderedMap other, int offset, int count) {
		putAll(size, other, offset, count);
	}

	/**
	 * Adds up to {@code count} entries, starting from {@code offset}, in the map {@code other} to this set,
	 * inserting starting at {@code insertionIndex} in the iteration order.
	 *
	 * @param insertionIndex where to insert into the iteration order
	 * @param other          a non-null ordered map with the same type
	 * @param offset         the first index in {@code other} to use
	 * @param count          how many indices in {@code other} to use
	 */
	public void putAll(int insertionIndex, LongIntOrderedMap other, int offset, int count) {
		int end = Math.min(offset + count, other.size());
		ensureCapacity(end - offset);
		for (int i = offset; i < end; i++) {
			put(other.keyAt(i), other.getAt(i), insertionIndex++);
		}
	}

	@Override
	public int remove(long key) {
		// If key is not present, using an O(1) containsKey() lets us avoid an O(n) remove step on keys.
		if (!super.containsKey(key)) {
			return defaultValue;
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
	public int removeAt(int index) {
		return super.remove(keys.removeAt(index));
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
	@Override
	public void removeRange(int start, int end) {
		start = Math.max(0, start);
		end = Math.min(keys.size(), end);
		for (int i = start; i < end; i++) {
			super.remove(keys.get(i));
		}
		keys.removeRange(start, end);
	}

	/**
	 * Reduces the size of the map to the specified size. If the map is already smaller than the specified
	 * size, no action is taken.
	 *
	 * @param newSize the target size to try to reach by removing items, if smaller than the current size
	 */
	@Override
	public void truncate(int newSize) {
		if (size > newSize) {
			removeRange(newSize, size);
		}
	}

	/**
	 * Increases the size of the backing array to accommodate the specified number of additional items / loadFactor. Useful before
	 * adding many items to avoid multiple backing array resizes.
	 *
	 * @param additionalCapacity how many additional items this should be able to hold without resizing (probably)
	 */
	@Override
	public void ensureCapacity(int additionalCapacity) {
		int tableSize = tableSize(size + additionalCapacity, loadFactor);
		if (keyTable.length < tableSize) {
			resize(tableSize);
		}
		keys.ensureCapacity(additionalCapacity);
	}

	@Override
	public int getAndIncrement(long key, int defaultValue, int increment) {
		if (key == 0) {
			if (hasZeroValue) {
				int old = zeroValue;
				zeroValue += increment;
				return old;
			}
			hasZeroValue = true;
			zeroValue = defaultValue + increment;
			keys.add(key);
			size++;
			return defaultValue;
		}
		int i = locateKey(key);
		if (i >= 0) { // Existing key was found.
			int oldValue = valueTable[i];
			valueTable[i] += increment;
			return oldValue;
		}
		i = ~i; // Empty space was found.
		keyTable[i] = key;
		valueTable[i] = defaultValue + increment;
		keys.add(key);
		if (++size >= threshold) {
			resize(keyTable.length << 1);
		}
		return defaultValue;

	}

	/**
	 * Changes the key {@code before} to {@code after} without changing its position in the order or its value. Returns true if
	 * {@code after} has been added to the LongIntOrderedMap and {@code before} has been removed; returns false if {@code after} is
	 * already present or {@code before} is not present. If you are iterating over an LongIntOrderedMap and have an index, you should
	 * prefer {@link #alterAt(int, long)}, which doesn't need to search for an index like this does and so can be faster.
	 *
	 * @param before a key that must be present for this to succeed
	 * @param after  a key that must not be in this map for this to succeed
	 * @return true if {@code before} was removed and {@code after} was added, false otherwise
	 */
	public boolean alter(long before, long after) {
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
	 * for the size of this map. Otherwise, it returns true. Unlike {@link #alter(long, long)}, this operates in constant time.
	 *
	 * @param index the index in the order of the key to change; must be non-negative and less than {@link #size}
	 * @param after the key that will replace the contents at {@code index}; this key must not be present for this to succeed
	 * @return true if {@code after} successfully replaced the key at {@code index}, false otherwise
	 */
	public boolean alterAt(int index, long after) {
		if (index < 0 || index >= size || containsKey(after)) {
			return false;
		}
		super.put(after, super.remove(keys.get(index)));
		keys.set(index, after);
		return true;
	}

	/**
	 * Changes the value at a specified {@code index} in the iteration order to {@code v}, without changing keys at all.
	 * If {@code index} isn't currently a valid index in the iteration order, this returns {@link #defaultValue}.
	 * Otherwise, it returns the value that was previously held at {@code index}, which may be equal to {@link #defaultValue}.
	 *
	 * @param v     the new int value to assign
	 * @param index the index in the iteration order to set {@code v} at
	 * @return the previous value held at {@code index} in the iteration order, which may be null if the value was null or if {@code index} was invalid
	 */
	public int setAt(int index, int v) {
		if (index < 0 || index >= size) {
			return defaultValue;
		}
		final int pos = locateKey(keys.get(index));
		final int oldValue = valueTable[pos];
		valueTable[pos] = v;
		return oldValue;
	}

	/**
	 * Gets the int value at the given {@code index} in the insertion order. The index should be between 0
	 * (inclusive) and {@link #size()} (exclusive).
	 *
	 * @param index an index in the insertion order, between 0 (inclusive) and {@link #size()} (exclusive)
	 * @return the value at the given index
	 */
	public int getAt(int index) {
		return get(keys.get(index));
	}

	/**
	 * Gets the long key at the given {@code index} in the insertion order. The index should be between 0
	 * (inclusive) and {@link #size()} (exclusive).
	 *
	 * @param index an index in the insertion order, between 0 (inclusive) and {@link #size()} (exclusive)
	 * @return the key at the given index
	 */
	public long keyAt(int index) {
		return keys.get(index);
	}

	@Override
	public void clear(int maximumCapacity) {
		keys.clear();
		super.clear(maximumCapacity);
	}

	@Override
	public void clear() {
		keys.clear();
		super.clear();
	}

	/**
	 * Gets the LongList of keys in the order this class will iterate through them.
	 * Returns a direct reference to the same LongList this uses, so changes to the returned list will
	 * also change the iteration order here.
	 *
	 * @return the LongList of keys, in iteration order (usually insertion-order), that this uses
	 */
	@Override
	public LongList order() {
		return keys;
	}

	/**
	 * Sorts this LongIntOrderedMap in-place by the keys' natural ordering.
	 */
	public void sort() {
		keys.sort();
	}

	/**
	 * Sorts this LongIntOrderedMap in-place by the given LongComparator used on the keys. If {@code comp} is null, then this
	 * will sort by the natural ordering of the keys.
	 *
	 * @param comp a LongComparator, such as one from {@link LongComparators}, or null to use the keys' natural ordering
	 */
	public void sort(@Nullable LongComparator comp) {
		keys.sort(comp);
	}

	/**
	 * Sorts this LongIntOrderedMap in-place by the given {@link IntComparator} used on the values. {@code comp}
	 * must not be null.  You can use {@link IntComparators#NATURAL_COMPARATOR}
	 * to do what {@link #sort()} does (just sorting values in this case instead of keys).
	 *
	 * @param comp a non-null IntComparator, such as one from {@link IntComparators}
	 */
	public void sortByValue(IntComparator comp) {
		keys.sort((a, b) -> comp.compare(get(a), get(b)));
	}

	/**
	 * Returns a {@link PrimitiveCollection.OfLong} view of the keys contained in this map.
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
	 * <p>Note that the same Collection instance is returned each time this
	 * method is called. Use the {@link OrderedMapKeys#OrderedMapKeys(LongIntOrderedMap)}
	 * constructor for nested or multithreaded iteration.
	 *
	 * @return a set view of the keys contained in this map
	 */
	@Override
	public Keys keySet() {
		if (keys1 == null || keys2 == null) {
			keys1 = new OrderedMapKeys(this);
			keys2 = new OrderedMapKeys(this);
		}
		if (!keys1.iter.valid) {
			keys1.iter.reset();
			keys1.iter.valid = true;
			keys2.iter.valid = false;
			return keys1;
		}
		keys2.iter.reset();
		keys2.iter.valid = true;
		keys1.iter.valid = false;
		return keys2;
	}

	/**
	 * Returns a {@link PrimitiveCollection.OfInt} for the values in the map. Remove is supported by the Collection's iterator.
	 * <p>Note that the same Collection instance is returned each time this method is called. Use the
	 * {@link OrderedMapValues#OrderedMapValues(LongIntOrderedMap)} constructor for nested or multithreaded iteration.
	 *
	 * @return a {@link PrimitiveCollection.OfInt} backed by this map
	 */
	@Override
	public Values values() {
		if (values1 == null || values2 == null) {
			values1 = new OrderedMapValues(this);
			values2 = new OrderedMapValues(this);
		}
		if (!values1.iter.valid) {
			values1.iter.reset();
			values1.iter.valid = true;
			values2.iter.valid = false;
			return values1;
		}
		values2.iter.reset();
		values2.iter.valid = true;
		values1.iter.valid = false;
		return values2;
	}

	/**
	 * Returns a {@link PrimitiveCollection.OfInt} of {@link Entry}, containing the entries in the map.
	 * Remove is supported by the Set's iterator.
	 *
	 * <p>Note that the same iterator instance is returned each time this method is called.
	 * Use the {@link OrderedMapEntries#OrderedMapEntries(LongIntOrderedMap)} constructor for nested or multithreaded iteration.
	 *
	 * @return a {@link PrimitiveCollection.OfInt} of {@link Entry} key-value pairs
	 */
	@Override
	public Entries entrySet() {
		if (entries1 == null || entries2 == null) {
			entries1 = new OrderedMapEntries(this);
			entries2 = new OrderedMapEntries(this);
		}
		if (!entries1.iter.valid) {
			entries1.iter.reset();
			entries1.iter.valid = true;
			entries2.iter.valid = false;
			return entries1;
		}
		entries2.iter.reset();
		entries2.iter.valid = true;
		entries1.iter.valid = false;
		return entries2;
	}

	/**
	 * Reuses the iterator of the reused {@link Entries}
	 * produced by {@link #entrySet()}; does not permit nested iteration. Iterate over
	 * {@link OrderedMapEntries#OrderedMapEntries(LongIntOrderedMap)} if you need nested or
	 * multithreaded iteration. You can remove an Entry from this LongIntOrderedMap
	 * using this Iterator.
	 *
	 * @return an {@link Iterator} over key-value pairs as {@link Map.Entry} values
	 */
	@Override
	public @NonNull EntryIterator iterator() {
		return entrySet().iterator();
	}

	/**
	 * Appends to a StringBuilder from the contents of this LongIntOrderedMap, but uses the given {@link LongAppender} and
	 * {@link IntAppender} to convert each key and each value to a customizable representation and append them
	 * to a StringBuilder. These functions are often method references to methods in Base, such as
	 * {@link Base#appendUnsigned(CharSequence, long)} and {@link Base#appendUnsigned(CharSequence, int)}. To use
	 * the default String representation, you can use {@code Appender::append} as an appender. To write values
	 * so that they can be read back as Java source code, use {@code Base::appendReadable} for each appender.
	 * <br>
	 * Using {@code Base::appendReadable}, if you separate keys
	 * from values with {@code ", "} and also separate entries with {@code ", "}, that allows the output to be
	 * copied into source code that calls {@link #with(Number, Number, Number...)} (if {@code braces} is false).
	 *
	 * @param sb                a StringBuilder that this can append to
	 * @param entrySeparator    how to separate entries, such as {@code ", "}
	 * @param keyValueSeparator how to separate each key from its value, such as {@code "="} or {@code ":"}
	 * @param braces            true to wrap the output in curly braces, or false to omit them
	 * @param keyAppender       a function that takes a StringBuilder and a long, and returns the modified StringBuilder
	 * @param valueAppender     a function that takes a StringBuilder and a int, and returns the modified StringBuilder
	 * @return {@code sb}, with the appended keys and values of this map
	 */
	@Override
	public StringBuilder appendTo(StringBuilder sb, String entrySeparator, String keyValueSeparator, boolean braces,
								  LongAppender keyAppender, IntAppender valueAppender) {
		if (size == 0) {
			return braces ? sb.append("{}") : sb;
		}
		if (braces) {
			sb.append('{');
		}
		LongList keys = this.keys;
		for (int i = 0, n = keys.size(); i < n; i++) {
			long key = keys.get(i);
			if (i > 0)
				sb.append(entrySeparator);
			keyAppender.apply(sb, key).append(keyValueSeparator);
			valueAppender.apply(sb, get(key));
		}
		if (braces) {
			sb.append('}');
		}
		return sb;
	}

	public static class OrderedMapEntries extends Entries {
		protected LongList keys;

		public OrderedMapEntries(LongIntOrderedMap map) {
			super(map);
			keys = map.keys;
			iter = new EntryIterator(map) {

				@Override
				public void reset() {
					currentIndex = -1;
					nextIndex = 0;
					hasNext = map.size > 0;
				}

				@Override
				public Entry next() {
					if (!hasNext) {
						throw new NoSuchElementException();
					}
					if (!valid) {
						throw new RuntimeException("#iterator() cannot be used nested.");
					}
					currentIndex = nextIndex;
					entry.key = keys.get(nextIndex);
					entry.value = map.get(entry.key);
					nextIndex++;
					hasNext = nextIndex < map.size;
					return entry;
				}

				@Override
				public void remove() {
					if (currentIndex < 0) {
						throw new IllegalStateException("next must be called before remove.");
					}
					map.remove(entry.key);
					nextIndex--;
					currentIndex = -1;
				}
			};
		}

	}

	public static class OrderedMapKeys extends Keys {
		private final LongList keys;

		public OrderedMapKeys(LongIntOrderedMap map) {
			super(map);
			keys = map.keys;
			iter = new KeyIterator(map) {

				@Override
				public void reset() {
					currentIndex = -1;
					nextIndex = 0;
					hasNext = map.size > 0;
				}

				@Override
				public long nextLong() {
					if (!hasNext) {
						throw new NoSuchElementException();
					}
					if (!valid) {
						throw new RuntimeException("#iterator() cannot be used nested.");
					}
					long key = keys.get(nextIndex);
					currentIndex = nextIndex;
					nextIndex++;
					hasNext = nextIndex < map.size;
					return key;
				}

				@Override
				public void remove() {
					if (currentIndex < 0) {
						throw new IllegalStateException("next must be called before remove.");
					}
					map.remove(keys.get(currentIndex));
					nextIndex = currentIndex;
					currentIndex = -1;
				}
			};
		}

	}

	public static class OrderedMapValues extends Values {
		private final LongList keys;

		public OrderedMapValues(LongIntOrderedMap map) {
			super(map);
			keys = map.keys;
			iter = new ValueIterator(map) {

				@Override
				public void reset() {
					currentIndex = -1;
					nextIndex = 0;
					hasNext = map.size > 0;
				}

				@Override
				public int nextInt() {
					if (!hasNext) {
						throw new NoSuchElementException();
					}
					if (!valid) {
						throw new RuntimeException("#iterator() cannot be used nested.");
					}
					int value = map.get(keys.get(nextIndex));
					currentIndex = nextIndex;
					nextIndex++;
					hasNext = nextIndex < map.size;
					return value;
				}

				@Override
				public void remove() {
					if (currentIndex < 0) {
						throw new IllegalStateException("next must be called before remove.");
					}
					map.remove(keys.get(currentIndex));
					nextIndex = currentIndex;
					currentIndex = -1;
				}
			};
		}

	}


	/**
	 * Constructs an empty map.
	 * This is usually less useful than just using the constructor, but can be handy
	 * in some code-generation scenarios when you don't know how many arguments you will have.
	 *
	 * @return a new map containing nothing
	 */
	public static LongIntOrderedMap with() {
		return new LongIntOrderedMap(0);
	}

	/**
	 * Constructs a single-entry map given one key and one value.
	 * This is mostly useful as an optimization for {@link #with(Number, Number, Number...)}
	 * when there's no "rest" of the keys or values. Like the more-argument with(), this will
	 * convert its Number keys and values to primitive long and int, regardless of which
	 * Number type was used.
	 *
	 * @param key0   the first and only key; will be converted to primitive long
	 * @param value0 the first and only value; will be converted to primitive int
	 * @return a new map containing just the entry mapping key0 to value0
	 */
	public static LongIntOrderedMap with(Number key0, Number value0) {
		LongIntOrderedMap map = new LongIntOrderedMap(1);
		map.put(key0.longValue(), value0.intValue());
		return map;
	}

	/**
	 * Constructs a map given alternating keys and values.
	 * This is mostly useful as an optimization for {@link #with(Number, Number, Number...)}
	 * when there's no "rest" of the keys or values. Like the more-argument with(), this will
	 * convert its Number keys and values to primitive long and int, regardless of which
	 * Number type was used.
	 *
	 * @param key0   a Number key; will be converted to primitive long
	 * @param value0 a Number for a value; will be converted to primitive int
	 * @param key1   a Number key; will be converted to primitive long
	 * @param value1 a Number for a value; will be converted to primitive int
	 * @return a new map containing the given key-value pairs
	 */
	public static LongIntOrderedMap with(Number key0, Number value0, Number key1, Number value1) {
		LongIntOrderedMap map = new LongIntOrderedMap(2);
		map.put(key0.longValue(), value0.intValue());
		map.put(key1.longValue(), value1.intValue());
		return map;
	}

	/**
	 * Constructs a map given alternating keys and values.
	 * This is mostly useful as an optimization for {@link #with(Number, Number, Number...)}
	 * when there's no "rest" of the keys or values. Like the more-argument with(), this will
	 * convert its Number keys and values to primitive long and int, regardless of which
	 * Number type was used.
	 *
	 * @param key0   a Number key; will be converted to primitive long
	 * @param value0 a Number for a value; will be converted to primitive int
	 * @param key1   a Number key; will be converted to primitive long
	 * @param value1 a Number for a value; will be converted to primitive int
	 * @param key2   a Number key; will be converted to primitive long
	 * @param value2 a Number for a value; will be converted to primitive int
	 * @return a new map containing the given key-value pairs
	 */
	public static LongIntOrderedMap with(Number key0, Number value0, Number key1, Number value1, Number key2, Number value2) {
		LongIntOrderedMap map = new LongIntOrderedMap(3);
		map.put(key0.longValue(), value0.intValue());
		map.put(key1.longValue(), value1.intValue());
		map.put(key2.longValue(), value2.intValue());
		return map;
	}

	/**
	 * Constructs a map given alternating keys and values.
	 * This is mostly useful as an optimization for {@link #with(Number, Number, Number...)}
	 * when there's no "rest" of the keys or values. Like the more-argument with(), this will
	 * convert its Number keys and values to primitive long and int, regardless of which
	 * Number type was used.
	 *
	 * @param key0   a Number key; will be converted to primitive long
	 * @param value0 a Number for a value; will be converted to primitive int
	 * @param key1   a Number key; will be converted to primitive long
	 * @param value1 a Number for a value; will be converted to primitive int
	 * @param key2   a Number key; will be converted to primitive long
	 * @param value2 a Number for a value; will be converted to primitive int
	 * @param key3   a Number key; will be converted to primitive long
	 * @param value3 a Number for a value; will be converted to primitive int
	 * @return a new map containing the given key-value pairs
	 */
	public static LongIntOrderedMap with(Number key0, Number value0, Number key1, Number value1, Number key2, Number value2, Number key3, Number value3) {
		LongIntOrderedMap map = new LongIntOrderedMap(4);
		map.put(key0.longValue(), value0.intValue());
		map.put(key1.longValue(), value1.intValue());
		map.put(key2.longValue(), value2.intValue());
		map.put(key3.longValue(), value3.intValue());
		return map;
	}

	/**
	 * Constructs a map given alternating keys and values.
	 * This can be useful in some code-generation scenarios, or when you want to make a
	 * map conveniently by-hand and have it populated at the start. You can also use
	 * {@link #LongIntOrderedMap(long[], int[])}, which takes all keys and then all values.
	 * This needs all keys to be some kind of (boxed) Number, and converts them to primitive
	 * {@code long}s. It also needs all values to be a (boxed) Number, and converts them to
	 * primitive {@code int}s. Any keys or values that aren't {@code Number}s have that
	 * entry skipped.
	 *
	 * @param key0   the first key; will be converted to a primitive long
	 * @param value0 the first value; will be converted to a primitive int
	 * @param rest   an array or varargs of Number elements
	 * @return a new map containing the given key-value pairs
	 */
	public static LongIntOrderedMap with(Number key0, Number value0, Number... rest) {
		LongIntOrderedMap map = new LongIntOrderedMap(1 + (rest.length >>> 1));
		map.put(key0.longValue(), value0.intValue());
		map.putPairs(rest);
		return map;
	}

	/**
	 * Constructs an empty map.
	 * This is usually less useful than just using the constructor, but can be handy
	 * in some code-generation scenarios when you don't know how many arguments you will have.
	 *
	 * @return a new map containing nothing
	 */
	public static LongIntOrderedMap withPrimitive() {
		return new LongIntOrderedMap(0);
	}

	/**
	 * Constructs a single-entry map given one key and one value.
	 * This is mostly useful as an optimization for {@link #with(Number, Number, Number...)}
	 * when there's no "rest" of the keys or values. Unlike the vararg with(), this doesn't
	 * box its arguments into Number items.
	 *
	 * @param key0   the first and only key
	 * @param value0 the first and only value
	 * @return a new map containing just the entry mapping key0 to value0
	 */
	public static LongIntOrderedMap withPrimitive(long key0, int value0) {
		LongIntOrderedMap map = new LongIntOrderedMap(1);
		map.put(key0, value0);
		return map;
	}

	/**
	 * Constructs a map given alternating keys and values.
	 * This is mostly useful as an optimization for {@link #with(Number, Number, Number...)}
	 * when there's no "rest" of the keys or values. Unlike the vararg with(), this doesn't
	 * box its arguments into Number items.
	 *
	 * @param key0   a long key
	 * @param value0 an int value
	 * @param key1   a long key
	 * @param value1 an int value
	 * @return a new map containing the given key-value pairs
	 */
	public static LongIntOrderedMap withPrimitive(long key0, int value0, long key1, int value1) {
		LongIntOrderedMap map = new LongIntOrderedMap(2);
		map.put(key0, value0);
		map.put(key1, value1);
		return map;
	}

	/**
	 * Constructs a map given alternating keys and values.
	 * This is mostly useful as an optimization for {@link #with(Number, Number, Number...)}
	 * when there's no "rest" of the keys or values. Unlike the vararg with(), this doesn't
	 * box its arguments into Number items.
	 *
	 * @param key0   a long key
	 * @param value0 an int value
	 * @param key1   a long key
	 * @param value1 an int value
	 * @param key2   a long key
	 * @param value2 an int value
	 * @return a new map containing the given key-value pairs
	 */
	public static LongIntOrderedMap withPrimitive(long key0, int value0, long key1, int value1, long key2, int value2) {
		LongIntOrderedMap map = new LongIntOrderedMap(3);
		map.put(key0, value0);
		map.put(key1, value1);
		map.put(key2, value2);
		return map;
	}

	/**
	 * Constructs a map given alternating keys and values.
	 * This is mostly useful as an optimization for {@link #with(Number, Number, Number...)}
	 * when there's no "rest" of the keys or values. Unlike the vararg with(), this doesn't
	 * box its arguments into Number items.
	 *
	 * @param key0   a long key
	 * @param value0 an int value
	 * @param key1   a long key
	 * @param value1 an int value
	 * @param key2   a long key
	 * @param value2 an int value
	 * @param key3   a long key
	 * @param value3 an int value
	 * @return a new map containing the given key-value pairs
	 */
	public static LongIntOrderedMap withPrimitive(long key0, int value0, long key1, int value1, long key2, int value2, long key3, int value3) {
		LongIntOrderedMap map = new LongIntOrderedMap(4);
		map.put(key0, value0);
		map.put(key1, value1);
		map.put(key2, value2);
		map.put(key3, value3);
		return map;
	}

	/**
	 * Creates a new map by parsing all of {@code str},
	 * with entries separated by {@code entrySeparator}, such as {@code ", "} and
	 * the keys separated from values by {@code keyValueSeparator}, such as {@code "="}.
	 *
	 * @param str               a String containing parseable text
	 * @param entrySeparator    the String separating every key-value pair
	 * @param keyValueSeparator the String separating every key from its corresponding value
	 */
	public static LongIntOrderedMap parse(String str,
												  String entrySeparator,
												  String keyValueSeparator) {
		return parse(str, entrySeparator, keyValueSeparator, false);
	}

	/**
	 * Creates a new map by parsing all of {@code str} (or if {@code brackets} is true, all but the first and last
	 * chars), with entries separated by {@code entrySeparator},
	 * such as {@code ", "} and the keys separated from values by {@code keyValueSeparator}, such as {@code "="}.
	 *
	 * @param str               a String containing parseable text
	 * @param entrySeparator    the String separating every key-value pair
	 * @param keyValueSeparator the String separating every key from its corresponding value
	 * @param brackets          if true, the first and last chars in {@code str} will be ignored
	 */
	public static LongIntOrderedMap parse(String str,
												  String entrySeparator,
												  String keyValueSeparator,
												  boolean brackets) {
		LongIntOrderedMap m = new LongIntOrderedMap();
		if(brackets)
			m.putLegible(str, entrySeparator, keyValueSeparator, 1, str.length() - 1);
		else
			m.putLegible(str, entrySeparator, keyValueSeparator, 0, -1);
		return m;
	}

	/**
	 * Creates a new map by parsing the given subrange of {@code str},
	 * with entries separated by {@code entrySeparator}, such as {@code ", "} and the keys separated from values
	 * by {@code keyValueSeparator}, such as {@code "="}.
	 *
	 * @param str               a String containing parseable text
	 * @param entrySeparator    the String separating every key-value pair
	 * @param keyValueSeparator the String separating every key from its corresponding value
	 * @param offset            the first position to read parseable text from in {@code str}
	 * @param length            how many chars to read; -1 is treated as maximum length
	 */
	public static LongIntOrderedMap parse(String str,
												  String entrySeparator,
												  String keyValueSeparator,
												  int offset,
												  int length) {
		LongIntOrderedMap m = new LongIntOrderedMap();
		m.putLegible(str, entrySeparator, keyValueSeparator, offset, length);
		return m;
	}
}

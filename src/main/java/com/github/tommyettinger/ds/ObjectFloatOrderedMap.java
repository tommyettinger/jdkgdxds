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
import com.github.tommyettinger.ds.support.sort.FloatComparator;
import com.github.tommyettinger.ds.support.sort.FloatComparators;

import com.github.tommyettinger.ds.support.util.Appender;
import com.github.tommyettinger.ds.support.util.FloatAppender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import static com.github.tommyettinger.ds.Utilities.tableSize;

/**
 * An {@link ObjectFloatMap} that also stores keys in an {@link ObjectList} using the insertion order. Null keys are not allowed. No
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
 * ObjectOrderedSet and ObjectFloatOrderedMap.
 * <p>
 * You can customize most behavior of this map by extending it. {@link #place(Object)} can be overridden to change how hashCodes
 * are calculated (which can be useful for types like {@link StringBuilder} that don't implement hashCode()), and
 * {@link #equate(Object, Object)} can be overridden to change how equality is calculated.
 * <p>
 * This implementation uses linear probing with the backward shift algorithm for removal.
 * It tries different hashes from a simple family, with the hash changing on resize.
 * Linear probing continues to work even when all hashCodes collide; it just works more slowly in that case.
 *
 * @author Nathan Sweet
 * @author Tommy Ettinger
 */
public class ObjectFloatOrderedMap<K> extends ObjectFloatMap<K> implements Ordered<K> {

	protected final ObjectList<K> keys;

	/**
	 * Creates a new map with an initial capacity of {@link Utilities#getDefaultTableCapacity()} and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 * @param ordering determines what implementation {@link #order()} will use
	 */
	public ObjectFloatOrderedMap (OrderType ordering) {
		this(Utilities.getDefaultTableCapacity(), ordering);
	}

	/**
	 * Creates a new map with the given starting capacity and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 * @param ordering determines what implementation {@link #order()} will use
	 */
	public ObjectFloatOrderedMap (int initialCapacity, OrderType ordering) {
		this(initialCapacity, Utilities.getDefaultLoadFactor(), ordering);
	}

	/**
	 * Creates a new map with the specified initial capacity and load factor. This map will hold initialCapacity items before
	 * growing the backing table.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 * @param loadFactor      what fraction of the capacity can be filled before this has to resize; 0 &lt; loadFactor &lt;= 1
	 * @param ordering determines what implementation {@link #order()} will use
	 */
	public ObjectFloatOrderedMap (int initialCapacity, float loadFactor, OrderType ordering) {
		super(initialCapacity, loadFactor);
        if (ordering == OrderType.BAG) {
            keys = new ObjectBag<>(initialCapacity);
        } else {
            keys = new ObjectList<>(initialCapacity);
        }
	}

	/**
	 * Creates a new map identical to the specified map.
	 *
	 * @param map the map to copy
	 */
	public ObjectFloatOrderedMap (ObjectFloatOrderedMap<? extends K> map) {
		super(map);
		if(map.keys instanceof ObjectBag) keys = new ObjectBag<>(map.keys);
		else keys = new ObjectList<>(map.keys);
	}

	/**
	 * Creates a new map identical to the specified map.
	 *
	 * @param map the map to copy
	 * @param ordering determines what implementation {@link #order()} will use
	 */
	public ObjectFloatOrderedMap (ObjectFloatMap<? extends K> map, OrderType ordering) {
		this(map.size(), map.loadFactor, ordering);
		hashMultiplier = map.hashMultiplier;
		for(K k : map.keySet()) {
			put(k, map.get(k));
		}
	}

	/**
	 * Creates a new set by copying {@code count} items from the given ObjectFloatOrderedMap, starting at {@code offset} in that Map,
	 * into this.
	 *
	 * @param other  another ObjectFloatOrderedMap of the same type
	 * @param offset the first index in other's ordering to draw an item from
	 * @param count  how many items to copy from other
	 */
	public ObjectFloatOrderedMap (ObjectFloatOrderedMap<? extends K> other, int offset, int count) {
		this(other, offset, count,
				other.keys instanceof ObjectBag ? OrderType.BAG
						: OrderType.LIST);
	}

	/**
	 * Creates a new map identical to the specified map.
	 *
	 * @param map the map to copy
	 */
	public ObjectFloatOrderedMap (ObjectFloatMap<? extends K> map) {
		this(map, OrderType.LIST);

	}

	/**
	 * Creates a new set by copying {@code count} items from the given ObjectFloatOrderedMap, starting at {@code offset} in that Map,
	 * into this.
	 *
	 * @param other  another ObjectFloatOrderedMap of the same type
	 * @param offset the first index in other's ordering to draw an item from
	 * @param count  how many items to copy from other
	 * @param ordering determines what implementation {@link #order()} will use
	 */
	public ObjectFloatOrderedMap (ObjectFloatOrderedMap<? extends K> other, int offset, int count, OrderType ordering) {
		this(count, other.loadFactor, ordering);
		hashMultiplier = other.hashMultiplier;
		putAll(0, other, offset, count);
	}

	/**
	 * Given two side-by-side arrays, one of keys, one of values, this constructs a map and inserts each pair of key and value into it.
	 * If keys and values have different lengths, this only uses the length of the smaller array.
	 *
	 * @param keys   an array of keys
	 * @param values an array of values
	 * @param ordering determines what implementation {@link #order()} will use
	 */
	public ObjectFloatOrderedMap (K[] keys, float[] values, OrderType ordering) {
		this(Math.min(keys.length, values.length), ordering);
		putAll(keys, values);
	}

	/**
	 * Given two side-by-side collections, one of keys, one of values, this constructs a map and inserts each pair of key and value into it.
	 * If keys and values have different lengths, this only uses the length of the smaller collection.
	 *
	 * @param keys   a Collection of keys
	 * @param values a PrimitiveCollection of values
	 * @param ordering determines what implementation {@link #order()} will use
	 */
	public ObjectFloatOrderedMap (Collection<? extends K> keys, PrimitiveCollection.OfFloat values, OrderType ordering) {
		this(Math.min(keys.size(), values.size()), ordering);
		putAll(keys, values);
	}

	/**
	 * Creates a new map with an initial capacity of {@link Utilities#getDefaultTableCapacity()} and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 */
	public ObjectFloatOrderedMap () {
		this(OrderType.LIST);
	}

	/**
	 * Creates a new map with the given starting capacity and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 */
	public ObjectFloatOrderedMap (int initialCapacity) {
		this(initialCapacity, Utilities.getDefaultLoadFactor(), OrderType.LIST);
	}

	/**
	 * Creates a new map with the specified initial capacity and load factor. This map will hold initialCapacity items before
	 * growing the backing table.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 * @param loadFactor      what fraction of the capacity can be filled before this has to resize; 0 &lt; loadFactor &lt;= 1
	 */
	public ObjectFloatOrderedMap (int initialCapacity, float loadFactor) {
		this(initialCapacity, loadFactor, OrderType.LIST);
	}

	/**
	 * Given two side-by-side arrays, one of keys, one of values, this constructs a map and inserts each pair of key and value into it.
	 * If keys and values have different lengths, this only uses the length of the smaller array.
	 *
	 * @param keys   an array of keys
	 * @param values an array of values
	 */
	public ObjectFloatOrderedMap (K[] keys, float[] values) {
		this(keys, values, OrderType.LIST);
	}

	/**
	 * Given two side-by-side collections, one of keys, one of values, this constructs a map and inserts each pair of key and value into it.
	 * If keys and values have different lengths, this only uses the length of the smaller collection.
	 *
	 * @param keys   a Collection of keys
	 * @param values a PrimitiveCollection of values
	 */
	public ObjectFloatOrderedMap (Collection<? extends K> keys, PrimitiveCollection.OfFloat values) {
		this(keys, values, OrderType.LIST);
	}

	@Override
	public float put (K key, float value) {
		if(key == null) return defaultValue;
		int i = locateKey(key);
		if (i >= 0) { // Existing key was found.
			float oldValue = valueTable[i];
			valueTable[i] = value;
			return oldValue;
		}
		i = ~i; // Empty space was found.
		keyTable[i] = key;
		valueTable[i] = value;
		keys.add(key);
		if (++size >= threshold) {resize(keyTable.length << 1);}
		return defaultValue;
	}

	/**
	 * Puts the given key and value into this map at the given index in its order.
	 * If the key is already present at a different index, it is moved to the given index and its
	 * value is set to the given value.
	 *
	 * @param key   a K key; must not be null
	 * @param value a float value
	 * @param index the index in the order to place the given key and value; must be non-negative and less than {@link #size()}
	 * @return the previous value associated with key, if there was one, or {@link #defaultValue} otherwise
	 */
	public float put (K key, float value, int index) {
		if(key == null) return defaultValue;
		int i = locateKey(key);
		if (i >= 0) { // Existing key was found.
			float oldValue = valueTable[i];
			valueTable[i] = value;
			int oldIndex = keys.indexOf(key);
			if (oldIndex != index) {keys.insert(index, keys.removeAt(oldIndex));}
			return oldValue;
		}
		i = ~i; // Empty space was found.
		keyTable[i] = key;
		valueTable[i] = value;
		keys.insert(index, key);
		if (++size >= threshold) {resize(keyTable.length << 1);}
		return defaultValue;
	}

	@Override
	public float putOrDefault (K key, float value, float defaultValue) {
		if(key == null) return defaultValue;
		int i = locateKey(key);
		if (i >= 0) { // Existing key was found.
			float oldValue = valueTable[i];
			valueTable[i] = value;
			return oldValue;
		}
		i = ~i; // Empty space was found.
		keyTable[i] = key;
		valueTable[i] = value;
		keys.add(key);
		if (++size >= threshold) {resize(keyTable.length << 1);}
		return defaultValue;
	}

	/**
	 * Puts every key-value pair in the given map into this, with the values from the given map
	 * overwriting the previous values if two keys are identical. This will put keys in the order of the given map.
	 *
	 * @param map a map with compatible key and value types; will not be modified
	 */
	public void putAll (ObjectFloatOrderedMap<? extends K> map) {
		ensureCapacity(map.size);
		for (int i = 0, kl = map.size; i < kl; i++) {
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
	public void putAll (ObjectFloatOrderedMap<? extends K> other, int offset, int count) {
		putAll(size, other, offset, count);
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
	public void putAll (int insertionIndex, ObjectFloatOrderedMap<? extends K> other, int offset, int count) {
		int end = Math.min(offset + count, other.size());
		ensureCapacity(end - offset);
		for (int i = offset; i < end; i++) {
			put(other.keyAt(i), other.getAt(i), insertionIndex++);
		}
	}

	@Override
	public float remove (Object key) {
		if(key == null) return defaultValue;
		// If key is not present, using an O(1) containsKey() lets us avoid an O(n) remove step on keys.
		if (!super.containsKey(key)) {return defaultValue;}
		keys.remove(key);
		return super.remove(key);
	}

	/**
	 * Removes the entry at the given index in the order, returning the value of that entry.
	 *
	 * @param index the index of the entry to remove; must be at least 0 and less than {@link #size()}
	 * @return the value of the removed entry
	 */
	public float removeAt (int index) {
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
	public void removeRange (int start, int end) {
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
	public void truncate (int newSize) {
		if (size > newSize) {removeRange(newSize, size);}
	}

	/**
	 * Increases the size of the backing array to accommodate the specified number of additional items / loadFactor. Useful before
	 * adding many items to avoid multiple backing array resizes.
	 *
	 * @param additionalCapacity how many additional items this should be able to hold without resizing (probably)
	 */
	@Override
	public void ensureCapacity (int additionalCapacity) {
		int tableSize = tableSize(size + additionalCapacity, loadFactor);
		if (keyTable.length < tableSize) {resize(tableSize);}
		keys.ensureCapacity(additionalCapacity);

	}

	@Override
	public float getAndIncrement (K key, float defaultValue, float increment) {
		if(key == null) return defaultValue;
		int i = locateKey(key);
		if (i >= 0) { // Existing key was found.
			float oldValue = valueTable[i];
			valueTable[i] += increment;
			return oldValue;
		}
		i = ~i; // Empty space was found.
		keyTable[i] = key;
		valueTable[i] = defaultValue + increment;
		keys.add(key);
		if (++size >= threshold) {resize(keyTable.length << 1);}
		return defaultValue;
	}

	/**
	 * Changes the key {@code before} to {@code after} without changing its position in the order or its value. Returns true if
	 * {@code after} has been added to the ObjectFloatOrderedMap and {@code before} has been removed; returns false if {@code after} is
	 * already present or {@code before} is not present. If you are iterating over an ObjectFloatOrderedMap and have an index, you should
	 * prefer {@link #alterAt(int, Object)}, which doesn't need to search for an index like this does and so can be faster.
	 *
	 * @param before a key that must be present for this to succeed
	 * @param after  a key that must not be in this map for this to succeed
	 * @return true if {@code before} was removed and {@code after} was added, false otherwise
	 */
	public boolean alter (K before, K after) {
		if (before == null || after == null || containsKey(after)) {return false;}
		int index = keys.indexOf(before);
		if (index == -1) {return false;}
		super.put(after, super.remove(before));
		keys.set(index, after);
		return true;
	}

	/**
	 * Changes the key at the given {@code index} in the order to {@code after}, without changing the ordering of other entries or
	 * any values. If {@code after} is already present, this returns false; it will also return false if {@code index} is invalid
	 * for the size of this map. Otherwise, it returns true. Unlike {@link #alter(Object, Object)}, this operates in constant time.
	 *
	 * @param index the index in the order of the key to change; must be non-negative and less than {@link #size}
	 * @param after the key that will replace the contents at {@code index}; this key must not be present for this to succeed
	 * @return true if {@code after} successfully replaced the key at {@code index}, false otherwise
	 */
	public boolean alterAt (int index, K after) {
		if (after == null || index < 0 || index >= size || containsKey(after)) {return false;}
		super.put(after, super.remove(keys.get(index)));
		keys.set(index, after);
		return true;
	}

	/**
	 * Changes the value at a specified {@code index} in the iteration order to {@code v}, without changing keys at all.
	 * If {@code index} isn't currently a valid index in the iteration order, this returns null. Otherwise, it returns the
	 * value that was previously held at {@code index}, which may also be null.
	 *
	 * @param v     the new float value to assign
	 * @param index the index in the iteration order to set {@code v} at
	 * @return the previous value held at {@code index} in the iteration order, which may be null if the value was null or if {@code index} was invalid
	 */
	public float setAt (int index, float v) {
		if (index < 0 || index >= size) {return defaultValue;}
		final int pos = locateKey(keys.get(index));
		final float oldValue = valueTable[pos];
		valueTable[pos] = v;
		return oldValue;
	}

	/**
	 * Gets the float value at the given {@code index} in the insertion order. The index should be between 0
	 * (inclusive) and {@link #size()} (exclusive).
	 *
	 * @param index an index in the insertion order, between 0 (inclusive) and {@link #size()} (exclusive)
	 * @return the value at the given index
	 */
	public float getAt (int index) {
		return get(keys.get(index));
	}

	/**
	 * Gets the K key at the given {@code index} in the insertion order. The index should be between 0
	 * (inclusive) and {@link #size()} (exclusive).
	 *
	 * @param index an index in the insertion order, between 0 (inclusive) and {@link #size()} (exclusive)
	 * @return the key at the given index
	 */
	public K keyAt (int index) {
		return keys.get(index);
	}

	@Override
	public void clear (int maximumCapacity) {
		keys.clear();
		super.clear(maximumCapacity);
	}

	@Override
	public void clear () {
		keys.clear();
		super.clear();
	}

	/**
	 * Gets the ObjectList of keys in the order this class will iterate through them.
	 * Returns a direct reference to the same ObjectList this uses, so changes to the returned list will
	 * also change the iteration order here.
	 *
	 * @return the ObjectList of keys, in iteration order (usually insertion-order), that this uses
	 */
	@Override
	public ObjectList<K> order () {
		return keys;
	}

	/**
	 * Sorts this ObjectFloatOrderedMap in-place by the keys' natural ordering; {@code K} must implement {@link Comparable}.
	 */
	public void sort () {
		keys.sort(null);
	}

	/**
	 * Sorts this ObjectFloatOrderedMap in-place by the given Comparator used on the keys. If {@code comp} is null, then this
	 * will sort by the natural ordering of the keys, which requires {@code K} to {@link Comparable}.
	 *
	 * @param comp a Comparator that can compare two {@code K} keys, or null to use the keys' natural ordering
	 */
	public void sort (@Nullable Comparator<? super K> comp) {
		keys.sort(comp);
	}

	/**
	 * Sorts this ObjectFloatOrderedMap in-place by the given FloatComparator used on the values. {@code comp} must not be null,
	 * and must be able to compare {@code float} values. You can use {@link FloatComparators#NATURAL_COMPARATOR} to do
	 * what {@link #sort()} does (just sorting values in this case instead of keys); there is also a reversed comparator
	 * available, {@link FloatComparators#OPPOSITE_COMPARATOR}.
	 *
	 * @param comp a non-null {@link FloatComparator}
	 */
	public void sortByValue (FloatComparator comp) {
		keys.sort((a, b) -> comp.compare(get(a), get(b)));
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
	 * <p>Note that the same Collection instance is returned each time this
	 * method is called. Use the {@link OrderedMapKeys#OrderedMapKeys(ObjectFloatOrderedMap)}
	 * constructor for nested or multithreaded iteration.
	 *
	 * @return a set view of the keys contained in this map
	 */
	@Override
	public Keys<K> keySet () {
		if (keys1 == null || keys2 == null) {
			keys1 = new OrderedMapKeys<>(this);
			keys2 = new OrderedMapKeys<>(this);
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
	 * Returns a Collection for the values in the map. Remove is supported by the Collection's iterator.
	 * <p>Note that the same Collection instance is returned each time this method is called. Use the
	 * {@link OrderedMapValues#OrderedMapValues(ObjectFloatOrderedMap)} constructor for nested or multithreaded iteration.
	 *
	 * @return a {@link PrimitiveCollection.OfFloat} of the float values
	 */
	@Override
	public Values<K> values () {
		if (values1 == null || values2 == null) {
			values1 = new OrderedMapValues<>(this);
			values2 = new OrderedMapValues<>(this);
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
	 * Returns a Set of Map.Entry, containing the entries in the map. Remove is supported by the Set's iterator.
	 *
	 * <p>Note that the same iterator instance is returned each time this method is called.
	 * Use the {@link OrderedMapEntries#OrderedMapEntries(ObjectFloatOrderedMap)} constructor for nested or multithreaded iteration.
	 *
	 * @return a {@link Set} of {@link Map.Entry} key-value pairs
	 */
	@Override
	public Entries<K> entrySet () {
		if (entries1 == null || entries2 == null) {
			entries1 = new OrderedMapEntries<>(this);
			entries2 = new OrderedMapEntries<>(this);
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
	 * {@link OrderedMapEntries#OrderedMapEntries(ObjectFloatOrderedMap)} if you need nested or
	 * multithreaded iteration. You can remove an Entry from this ObjectFloatOrderedMap
	 * using this Iterator.
	 *
	 * @return an {@link Iterator} over key-value pairs as {@link Map.Entry} values
	 */
	@Override
	public @NonNull EntryIterator<K> iterator () {
		return entrySet().iterator();
	}

	/**
	 * Appends to a StringBuilder from the contents of this ObjectFloatOrderedMap, but uses the given {@link Appender} and
	 * {@link FloatAppender} to convert each key and each value to a customizable representation and append them
	 * to a StringBuilder. These functions are often method references to methods in Base, such as
	 * {@link Base#appendFriendly(StringBuilder, float)}. To use
	 * the default String representation, you can use {@code StringBuilder::append} as an appender. To write numeric values
	 * so that they can be read back as Java source code, use {@code Base::appendReadable} for each appender.
	 *
	 * @param sb                a StringBuilder that this can append to
	 * @param entrySeparator    how to separate entries, such as {@code ", "}
	 * @param keyValueSeparator how to separate each key from its value, such as {@code "="} or {@code ":"}
	 * @param braces            true to wrap the output in curly braces, or false to omit them
	 * @param keyAppender       a function that takes a StringBuilder and a K, and returns the modified StringBuilder
	 * @param valueAppender     a function that takes a StringBuilder and a float, and returns the modified StringBuilder
	 * @return {@code sb}, with the appended keys and values of this map
	 */
	@Override
	public StringBuilder appendTo (StringBuilder sb, String entrySeparator, String keyValueSeparator, boolean braces, Appender<K> keyAppender, FloatAppender valueAppender) {
		if (size == 0) {return braces ? sb.append("{}") : sb;}
		if (braces) {sb.append('{');}
		ObjectList<K> keys = this.keys;
		for (int i = 0, n = keys.size(); i < n; i++) {
			K key = keys.get(i);
			if (i > 0) {sb.append(entrySeparator);}
			if(key == this)
				sb.append("(this)");
			else
				keyAppender.apply(sb, key);
			sb.append(keyValueSeparator);
			valueAppender.apply(sb, get(key));
		}
		if (braces) {sb.append('}');}
		return sb;
	}

	public static class OrderedMapEntries<K> extends Entries<K> {
		protected ObjectList<K> keys;

		public OrderedMapEntries (ObjectFloatOrderedMap<K> map) {
			super(map);
			keys = map.keys;
			iter = new EntryIterator<K>(map) {

				@Override
				public void reset () {
					currentIndex = -1;
					nextIndex = 0;
					hasNext = map.size > 0;
				}

				@Override
				public Entry<K> next () {
					if (!hasNext) {throw new NoSuchElementException();}
					if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
					currentIndex = nextIndex;
					entry.key = keys.get(nextIndex);
					entry.value = map.get(entry.key);
					nextIndex++;
					hasNext = nextIndex < map.size;
					return entry;
				}

				@Override
				public void remove () {
					if (currentIndex < 0) {throw new IllegalStateException("next must be called before remove.");}
					if (entry.key != null) {
						map.remove(entry.key);
					}
					nextIndex--;
					currentIndex = -1;
				}
			};
		}

	}

	public static class OrderedMapKeys<K> extends Keys<K> {
		private final ObjectList<K> keys;

		public OrderedMapKeys (ObjectFloatOrderedMap<K> map) {
			super(map);
			keys = map.keys;
			iter = new KeyIterator<K>(map) {

				@Override
				public void reset () {
					currentIndex = -1;
					nextIndex = 0;
					hasNext = map.size > 0;
				}

				@Override
				public K next () {
					if (!hasNext) {throw new NoSuchElementException();}
					if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
					K key = keys.get(nextIndex);
					currentIndex = nextIndex;
					nextIndex++;
					hasNext = nextIndex < map.size;
					return key;
				}

				@Override
				public void remove () {
					if (currentIndex < 0) {throw new IllegalStateException("next must be called before remove.");}
					map.remove(keys.get(currentIndex));
					nextIndex = currentIndex;
					currentIndex = -1;
				}
			};
		}

	}

	public static class OrderedMapValues<K> extends Values<K> {
		private final ObjectList<K> keys;

		public OrderedMapValues (ObjectFloatOrderedMap<K> map) {
			super(map);
			keys = map.keys;
			iter = new ValueIterator<K>(map) {

				@Override
				public void reset () {
					currentIndex = -1;
					nextIndex = 0;
					hasNext = map.size > 0;
				}

				@Override
				public float nextFloat () {
					if (!hasNext) {throw new NoSuchElementException();}
					if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
					float value = map.get(keys.get(nextIndex));
					currentIndex = nextIndex;
					nextIndex++;
					hasNext = nextIndex < map.size;
					return value;
				}

				@Override
				public void remove () {
					if (currentIndex < 0) {throw new IllegalStateException("next must be called before remove.");}
					map.remove(keys.get(currentIndex));
					nextIndex = currentIndex;
					currentIndex = -1;
				}
			};
		}

	}

	/**
	 * Constructs an empty map given the key type as a generic type argument.
	 * This is usually less useful than just using the constructor, but can be handy
	 * in some code-generation scenarios when you don't know how many arguments you will have.
	 *
	 * @param <K>    the type of keys
	 * @return a new map containing nothing
	 */
	public static <K> ObjectFloatOrderedMap<K> with () {
		return new ObjectFloatOrderedMap<>(0);
	}

	/**
	 * Constructs a single-entry map given one key and one value.
	 * This is mostly useful as an optimization for {@link #with(Object, Number, Object...)}
	 * when there's no "rest" of the keys or values. Like the more-argument with(), this will
	 * convert its Number value to a primitive float, regardless of which Number type was used.
	 *
	 * @param key0   the first and only key
	 * @param value0 the first and only value; will be converted to primitive float
	 * @param <K>    the type of key0
	 * @return a new map containing just the entry mapping key0 to value0
	 */
	public static <K> ObjectFloatOrderedMap<K> with (K key0, Number value0) {
		ObjectFloatOrderedMap<K> map = new ObjectFloatOrderedMap<>(1);
		map.put(key0, value0.floatValue());
		return map;
	}

	/**
	 * Constructs a map given alternating keys and values.
	 * This is mostly useful as an optimization for {@link #with(Object, Number, Object...)}
	 * when there's no "rest" of the keys or values. Like the more-argument with(), this will
	 * convert its Number values to primitive floats, regardless of which Number type was used.
	 *
	 * @param key0   a K key
	 * @param value0 a Number for a value; will be converted to primitive float
	 * @param key1   a K key
	 * @param value1 a Number for a value; will be converted to primitive float
	 * @param <K>    the type of keys
	 * @return a new map containing the given key-value pairs
	 */
	public static <K> ObjectFloatOrderedMap<K> with (K key0, Number value0, K key1, Number value1) {
		ObjectFloatOrderedMap<K> map = new ObjectFloatOrderedMap<>(2);
		map.put(key0, value0.floatValue());
		map.put(key1, value1.floatValue());
		return map;
	}

	/**
	 * Constructs a map given alternating keys and values.
	 * This is mostly useful as an optimization for {@link #with(Object, Number, Object...)}
	 * when there's no "rest" of the keys or values. Like the more-argument with(), this will
	 * convert its Number values to primitive floats, regardless of which Number type was used.
	 *
	 * @param key0   a K key
	 * @param value0 a Number for a value; will be converted to primitive float
	 * @param key1   a K key
	 * @param value1 a Number for a value; will be converted to primitive float
	 * @param key2   a K key
	 * @param value2 a Number for a value; will be converted to primitive float
	 * @param <K>    the type of keys
	 * @return a new map containing the given key-value pairs
	 */
	public static <K> ObjectFloatOrderedMap<K> with (K key0, Number value0, K key1, Number value1, K key2, Number value2) {
		ObjectFloatOrderedMap<K> map = new ObjectFloatOrderedMap<>(3);
		map.put(key0, value0.floatValue());
		map.put(key1, value1.floatValue());
		map.put(key2, value2.floatValue());
		return map;
	}

	/**
	 * Constructs a map given alternating keys and values.
	 * This is mostly useful as an optimization for {@link #with(Object, Number, Object...)}
	 * when there's no "rest" of the keys or values. Like the more-argument with(), this will
	 * convert its Number values to primitive floats, regardless of which Number type was used.
	 *
	 * @param key0   a K key
	 * @param value0 a Number for a value; will be converted to primitive float
	 * @param key1   a K key
	 * @param value1 a Number for a value; will be converted to primitive float
	 * @param key2   a K key
	 * @param value2 a Number for a value; will be converted to primitive float
	 * @param key3   a K key
	 * @param value3 a Number for a value; will be converted to primitive float
	 * @param <K>    the type of keys
	 * @return a new map containing the given key-value pairs
	 */
	public static <K> ObjectFloatOrderedMap<K> with (K key0, Number value0, K key1, Number value1, K key2, Number value2, K key3, Number value3) {
		ObjectFloatOrderedMap<K> map = new ObjectFloatOrderedMap<>(4);
		map.put(key0, value0.floatValue());
		map.put(key1, value1.floatValue());
		map.put(key2, value2.floatValue());
		map.put(key3, value3.floatValue());
		return map;
	}

	/**
	 * Constructs a map given alternating keys and values.
	 * This can be useful in some code-generation scenarios, or when you want to make a
	 * map conveniently by-hand and have it populated at the start. You can also use
	 * {@link #ObjectFloatOrderedMap(Object[], float[])}, which takes all keys and then all values.
	 * This needs all keys to have the same type, because it gets a generic type from the
	 * first key parameter. All values must be some type of boxed Number, such as {@link Integer}
	 * or {@link Double}, and will be converted to primitive {@code float}s. Any keys that don't
	 * have K as their type or values that aren't {@code Number}s have that entry skipped.
	 *
	 * @param key0   the first key; will be used to determine the type of all keys
	 * @param value0 the first value; will be converted to primitive float
	 * @param rest   a varargs or non-null array of alternating K, Number, K, Number... elements
	 * @param <K>    the type of keys, inferred from key0
	 * @return a new map containing the given keys and values
	 */
	public static <K> ObjectFloatOrderedMap<K> with (K key0, Number value0, Object... rest) {
		ObjectFloatOrderedMap<K> map = new ObjectFloatOrderedMap<>(1 + (rest.length >>> 1));
		map.put(key0, value0.floatValue());
		map.putPairs(rest);
		return map;
	}

	/**
	 * Constructs an empty map given the key type as a generic type argument.
	 * This is usually less useful than just using the constructor, but can be handy
	 * in some code-generation scenarios when you don't know how many arguments you will have.
	 *
	 * @param <K>    the type of keys
	 * @return a new map containing nothing
	 */
	public static <K> ObjectFloatOrderedMap<K> withPrimitive () {
		return new ObjectFloatOrderedMap<>(0);
	}

	/**
	 * Constructs a single-entry map given one key and one value.
	 * This is mostly useful as an optimization for {@link #with(Object, Number, Object...)}
	 * when there's no "rest" of the keys or values. Unlike with(), this takes unboxed float as
	 * its value type, and will not box it.
	 *
	 * @param key0   a K for a key
	 * @param value0 a float for a value
	 * @param <K>    the type of key0
	 * @return a new map containing just the entry mapping key0 to value0
	 */
	public static <K> ObjectFloatOrderedMap<K> withPrimitive (K key0, float value0) {
		ObjectFloatOrderedMap<K> map = new ObjectFloatOrderedMap<>(1);
		map.put(key0, value0);
		return map;
	}

	/**
	 * Constructs a map given alternating keys and values.
	 * This is mostly useful as an optimization for {@link #with(Object, Number, Object...)}
	 * when there's no "rest" of the keys or values. Unlike with(), this takes unboxed float as
	 * its value type, and will not box it.
	 *
	 * @param key0   a K key
	 * @param value0 a float for a value
	 * @param key1   a K key
	 * @param value1 a float for a value
	 * @param <K>    the type of keys
	 * @return a new map containing the given key-value pairs
	 */
	public static <K> ObjectFloatOrderedMap<K> withPrimitive (K key0, float value0, K key1, float value1) {
		ObjectFloatOrderedMap<K> map = new ObjectFloatOrderedMap<>(2);
		map.put(key0, value0);
		map.put(key1, value1);
		return map;
	}

	/**
	 * Constructs a map given alternating keys and values.
	 * This is mostly useful as an optimization for {@link #with(Object, Number, Object...)}
	 * when there's no "rest" of the keys or values.  Unlike with(), this takes unboxed float as
	 * its value type, and will not box it.
	 *
	 * @param key0   a K key
	 * @param value0 a float for a value
	 * @param key1   a K key
	 * @param value1 a float for a value
	 * @param key2   a K key
	 * @param value2 a float for a value
	 * @param <K>    the type of keys
	 * @return a new map containing the given key-value pairs
	 */
	public static <K> ObjectFloatOrderedMap<K> withPrimitive (K key0, float value0, K key1, float value1, K key2, float value2) {
		ObjectFloatOrderedMap<K> map = new ObjectFloatOrderedMap<>(3);
		map.put(key0, value0);
		map.put(key1, value1);
		map.put(key2, value2);
		return map;
	}

	/**
	 * Constructs a map given alternating keys and values.
	 * This is mostly useful as an optimization for {@link #with(Object, Number, Object...)}
	 * when there's no "rest" of the keys or values.  Unlike with(), this takes unboxed float as
	 * its value type, and will not box it.
	 *
	 * @param key0   a K key
	 * @param value0 a float for a value
	 * @param key1   a K key
	 * @param value1 a float for a value
	 * @param key2   a K key
	 * @param value2 a float for a value
	 * @param key3   a K key
	 * @param value3 a float for a value
	 * @param <K>    the type of keys
	 * @return a new map containing the given key-value pairs
	 */
	public static <K> ObjectFloatOrderedMap<K> withPrimitive (K key0, float value0, K key1, float value1, K key2, float value2, K key3, float value3) {
		ObjectFloatOrderedMap<K> map = new ObjectFloatOrderedMap<>(4);
		map.put(key0, value0);
		map.put(key1, value1);
		map.put(key2, value2);
		map.put(key3, value3);
		return map;
	}
}

/*
 * Copyright (c) 2022 See AUTHORS file.
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

package com.github.tommyettinger.ds;

import javax.annotation.Nullable;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

import static com.github.tommyettinger.ds.Utilities.neverIdentical;
import static com.github.tommyettinger.ds.Utilities.tableSize;

/**
 * An unordered map where the keys and values are objects. Null keys are not allowed. No allocation is done except when growing
 * the table size.
 * <p>
 * This class performs fast contains and remove (typically O(1), worst case O(n) but that is rare in practice). Add may be
 * slightly slower, depending on hash collisions. Hashcodes are rehashed to reduce collisions and the need to resize. Load factors
 * greater than 0.91 greatly increase the chances to resize to the next higher POT size.
 * <p>
 * Unordered sets and maps are not designed to provide especially fast iteration. Iteration is faster with {@link Ordered} types like
 * ObjectOrderedSet and ObjectObjectOrderedMap.
 * <p>
 * You can customize most behavior of this map by extending it. {@link #place(Object)} can be overridden to change how hashCodes
 * are calculated (which can be useful for types like {@link StringBuilder} that don't implement hashCode()), and
 * {@link #equate(Object, Object)} can be overridden to change how equality is calculated.
 * <p>
 * This implementation uses linear probing with the backward shift algorithm for removal. Hashcodes are not rehashed by default, but
 * user code can subclass this and change the {@link #place(Object)} method if rehashing or an alternate hash is optimal. Linear
 * probing continues to work even when all hashCodes collide; it just works more slowly in that case.
 *
 * @author Nathan Sweet
 * @author Tommy Ettinger
 */
public class ObjectObjectMap<K, V> implements Map<K, V>, Iterable<Map.Entry<K, V>> {

	protected int size;

	protected K[] keyTable;
	protected V[] valueTable;

	/**
	 * Between 0f (exclusive) and 1f (inclusive, if you're careful), this determines how full the backing tables
	 * can get before this increases their size. Larger values use less memory but make the data structure slower.
	 */
	protected float loadFactor;

	/**
	 * Precalculated value of {@code (int)(keyTable.length * loadFactor)}, used to determine when to resize.
	 */
	protected int threshold;

	/**
	 * Used by {@link #place(Object)} typically, this should always equal {@code Long.numberOfLeadingZeros(mask)}.
	 * For a table that could hold 2 items (with 1 bit indices), this would be {@code 64 - 1 == 63}. For a table that
	 * could hold 256 items (with 8 bit indices), this would be {@code 64 - 8 == 56}.
	 */
	protected int shift;

	/**
	 * Used by {@link #place(Object)} to mix hashCode() results. Changes on every call to {@link #resize(int)} by default.
	 * This only needs to be serialized if the full key and value tables are serialized, or if the iteration order should be
	 * the same before and after serialization. Iteration order is better handled by using {@link ObjectObjectOrderedMap}.
	 */
	protected long hashMultiplier = 0x9E3779B97F4A7C15L;

	/**
	 * A bitmask used to confine hashcodes to the size of the table. Must be all 1 bits in its low positions, ie a power of two
	 * minus 1.
	 */
	protected int mask;
	@Nullable protected transient Entries<K, V> entries1;
	@Nullable protected transient Entries<K, V> entries2;
	@Nullable protected transient Values<K, V> values1;
	@Nullable protected transient Values<K, V> values2;
	@Nullable protected transient Keys<K, V> keys1;
	@Nullable protected transient Keys<K, V> keys2;

	/**
	 * Creates a new map with an initial capacity of 51 and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 */
	public ObjectObjectMap () {
		this(51, Utilities.getDefaultLoadFactor());
	}

	/**
	 * Creates a new map with the given starting capacity and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 */
	public ObjectObjectMap (int initialCapacity) {
		this(initialCapacity, Utilities.getDefaultLoadFactor());
	}

	/**
	 * Creates a new map with the specified initial capacity and load factor. This map will hold initialCapacity items before
	 * growing the backing table.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 * @param loadFactor      what fraction of the capacity can be filled before this has to resize; 0 &lt; loadFactor &lt;= 1
	 */
	public ObjectObjectMap (int initialCapacity, float loadFactor) {
		if (loadFactor <= 0f || loadFactor > 1f) {throw new IllegalArgumentException("loadFactor must be > 0 and <= 1: " + loadFactor);}
		this.loadFactor = loadFactor;

		int tableSize = tableSize(initialCapacity, loadFactor);
		threshold = (int)(tableSize * loadFactor);
		mask = tableSize - 1;
		shift = Long.numberOfLeadingZeros(mask);

		keyTable = (K[])new Object[tableSize];
		valueTable = (V[])new Object[tableSize];
	}

	/**
	 * Creates a new map identical to the specified map.
	 *
	 * @param map an ObjectObjectMap to copy
	 */
	public ObjectObjectMap (ObjectObjectMap<? extends K, ? extends V> map) {
		this.loadFactor = map.loadFactor;
		this.threshold = map.threshold;
		this.mask = map.mask;
		this.shift = map.shift;
		keyTable = Arrays.copyOf(map.keyTable, map.keyTable.length);
		valueTable = Arrays.copyOf(map.valueTable, map.valueTable.length);
		size = map.size;
	}

	/**
	 * Creates a new map identical to the specified map.
	 *
	 * @param map a Map to copy; ObjectObjectMap or its subclasses will be faster
	 */
	public ObjectObjectMap (Map<? extends K, ? extends V> map) {
		this(map.size());
		for (K k : map.keySet()) {
			put(k, map.get(k));
		}
	}

	/**
	 * Given two side-by-side arrays, one of keys, one of values, this constructs a map and inserts each pair of key and value into it.
	 * If keys and values have different lengths, this only uses the length of the smaller array.
	 *
	 * @param keys   an array of keys
	 * @param values an array of values
	 */
	public ObjectObjectMap (K[] keys, V[] values) {
		this(Math.min(keys.length, values.length));
		putAll(keys, values);
	}

	/**
	 * Given two side-by-side collections, one of keys, one of values, this constructs a map and inserts each pair of key and value into it.
	 * If keys and values have different lengths, this only uses the length of the smaller collection.
	 *
	 * @param keys   a Collection of keys
	 * @param values a Collection of values
	 */
	public ObjectObjectMap (Collection<? extends K> keys, Collection<? extends V> values) {
		this(Math.min(keys.size(), values.size()));
		putAll(keys, values);
	}

	/**
	 * Given two side-by-side collections, one of keys, one of values, this inserts each pair of key and value into this map with put().
	 *
	 * @param keys   a Collection of keys
	 * @param values a Collection of values
	 */
	public void putAll (Collection<? extends K> keys, Collection<? extends V> values) {
		int length = Math.min(keys.size(), values.size());
		ensureCapacity(length);
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
	 * Returns an index &gt;= 0 and &lt;= {@link #mask} for the specified {@code item}, mixed.
	 * <p>
	 * The default behavior uses a basic hash mixing family; it simply gets the
	 * {@link Object#hashCode()} of {@code item}, multiplies it by the current
	 * {@link #hashMultiplier}, and makes an unsigned right shift by {@link #shift} before
	 * casting to int and returning. Because the hashMultiplier changes every time the backing
	 * table resizes, if a problematic sequence of keys piles up with many collisions, that won't
	 * continue to cause problems when the next resize changes the hashMultiplier again. This
	 * doesn't have much way of preventing trouble from hashCode() implementations that always
	 * or very frequently return 0, but nothing really can handle that well.
	 * <br>
	 * This can be overridden to hash {@code item} differently, though all implementors must
	 * ensure this returns results in the range of 0 to {@link #mask}, inclusive. If nothing
	 * else is changed, then unsigned-right-shifting an int or long by {@link #shift} will also
	 * restrict results to the correct range. You should usually override this method
	 * if you also override {@link #equate(Object, Object)}, because two equal values should have
	 * the same hash. If you are confident that the hashCode() implementation used by item will
	 * have reasonable quality, you can override this with a simpler implementation, such as
	 * {@code return item.hashCode() & mask;}. This simpler version is not used by default, even
	 * though it can be slightly faster, because the default hashing family provides much
	 * better resilience against high collision rates when they occur accidentally. If collision
	 * rates are high on the low bits of many hashes, then the simpler version tends to be
	 * significantly slower than the hashing family. Neither version provides stronger defenses
	 * against maliciously-chosen items, but linear probing naturally won't fail entirely even in
	 * that case. It is possible that a user could write an implementation of place() that is more
	 * robust against malicious inputs; one such approach is optionally employed by .NET Core and
	 * newer versions for the hashes of strings. That approach is similar to the current one here.
	 *
	 * @param item a non-null Object; its hashCode() method should be used by most implementations
	 * @return an index between 0 and {@link #mask} (both inclusive)
	 */
	protected int place (Object item) {
		return (int)(item.hashCode() * hashMultiplier >>> shift);
		// This can be used if you know hashCode() has few collisions normally, and won't be maliciously manipulated.
//		return item.hashCode() & mask;
	}

	/**
	 * Compares the objects left and right, which are usually keys, for equality, returning true if they are considered
	 * equal. This is used by the rest of this class to determine whether two keys are considered equal. Normally, this
	 * returns {@code left.equals(right)}, but subclasses can override it to use reference equality, fuzzy equality, deep
	 * array equality, or any other custom definition of equality. Usually, {@link #place(Object)} is also overridden if
	 * this method is.
	 *
	 * @param left  must be non-null; typically a key being compared, but not necessarily
	 * @param right may be null; typically a key being compared, but can often be null for an empty key slot, or some other type
	 * @return true if left and right are considered equal for the purposes of this class
	 */
	protected boolean equate (Object left, @Nullable Object right) {
		return left.equals(right);
	}

	/**
	 * Returns the index of the key if already present, else {@code ~index} for the next empty index. This calls
	 * {@link #equate(Object, Object)} to determine if two keys are equivalent.
	 *
	 * @param key a non-null K key
	 * @return a negative index if the key was not found, or the non-negative index of the existing key if found
	 */
	protected int locateKey (Object key) {
		K[] keyTable = this.keyTable;
		for (int i = place(key); ; i = i + 1 & mask) {
			K other = keyTable[i];
			if (equate(key, other))
				return i; // Same key was found.
			if (other == null)
				return ~i; // Always negative; means empty space is available at i.
		}
	}

	/**
	 * Returns the old value associated with the specified key, or null.
	 */
	@Override
	@Nullable
	public V put (K key, @Nullable V value) {
		int i = locateKey(key);
		if (i >= 0) { // Existing key was found.
			V oldValue = valueTable[i];
			valueTable[i] = value;
			return oldValue;
		}
		i = ~i; // Empty space was found.
		keyTable[i] = key;
		valueTable[i] = value;
		if (++size >= threshold) {resize(keyTable.length << 1);}
		return null;
	}

	@Nullable
	public V putOrDefault (K key, @Nullable V value, @Nullable V defaultValue) {
		int i = locateKey(key);
		if (i >= 0) { // Existing key was found.
			V oldValue = valueTable[i];
			valueTable[i] = value;
			return oldValue;
		}
		i = ~i; // Empty space was found.
		keyTable[i] = key;
		valueTable[i] = value;
		if (++size >= threshold) {resize(keyTable.length << 1);}
		return defaultValue;
	}

	/**
	 * Puts every key-value pair in the given map into this, with the values from the given map
	 * overwriting the previous values if two keys are identical.
	 *
	 * @param map a map with compatible key and value types; will not be modified
	 */
	public void putAll (ObjectObjectMap<? extends K, ? extends V> map) {
		ensureCapacity(map.size);
		K[] keyTable = map.keyTable;
		V[] valueTable = map.valueTable;
		K key;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			key = keyTable[i];
			if (key != null) {put(key, valueTable[i]);}
		}
	}

	/**
	 * Given two side-by-side arrays, one of keys, one of values, this inserts each pair of key and value into this map with put().
	 *
	 * @param keys   an array of keys
	 * @param values an array of values
	 */
	public void putAll (K[] keys, V[] values) {
		putAll(keys, 0, values, 0, Math.min(keys.length, values.length));
	}

	/**
	 * Given two side-by-side arrays, one of keys, one of values, this inserts each pair of key and value into this map with put().
	 *
	 * @param keys   an array of keys
	 * @param values an array of values
	 * @param length how many items from keys and values to insert, at-most
	 */
	public void putAll (K[] keys, V[] values, int length) {
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
	public void putAll (K[] keys, int keyOffset, V[] values, int valueOffset, int length) {
		length = Math.min(length, Math.min(keys.length - keyOffset, values.length - valueOffset));
		ensureCapacity(length);
		K key;
		for (int k = keyOffset, v = valueOffset, i = 0, n = length; i < n; i++, k++, v++) {
			key = keys[k];
			if (key != null) {put(key, values[v]);}
		}
	}

	/**
	 * Skips checks for existing keys, doesn't increment size.
	 */
	private void putResize (K key, @Nullable V value) {
		K[] keyTable = this.keyTable;
		for (int i = place(key); ; i = i + 1 & mask) {
			if (keyTable[i] == null) {
				keyTable[i] = key;
				valueTable[i] = value;
				return;
			}
		}
	}

	/**
	 * Returns the value for the specified key, or null if the key is not in the map.
	 * Note that null is also a valid value that can be assigned to a legitimate key, so
	 * checking that the result of this method is null does not guarantee that the
	 * {@code key} is not present.
	 *
	 * @param key a non-null Object that should almost always be a {@code K} (or an instance of a subclass of {@code K})
	 */
	@Override
	@Nullable
	public V get (Object key) {
		K[] keyTable = this.keyTable;
		for (int i = place(key); ; i = i + 1 & mask) {
			K other = keyTable[i];
			if (equate(key, other))
				return valueTable[i];
			if (other == null)
				return null;
		}
	}

	/**
	 * Returns the value for the specified key, or the given default value if the key is not in the map.
	 */
	@Override
	@Nullable
	public V getOrDefault (Object key, @Nullable V defaultValue) {
		K[] keyTable = this.keyTable;
		for (int i = place(key); ; i = i + 1 & mask) {
			K other = keyTable[i];
			if (equate(key, other))
				return valueTable[i];
			if (other == null)
				return defaultValue;
		}
	}

	@Override
	@Nullable
	public V remove (Object key) {
		int i = locateKey(key);
		if (i < 0) {return null;}
		K[] keyTable = this.keyTable;
		V[] valueTable = this.valueTable;
		K rem;
		V oldValue = valueTable[i];
		int mask = this.mask, next = i + 1 & mask;
		while ((rem = keyTable[next]) != null) {
			int placement = place(rem);
			if ((next - placement & mask) > (i - placement & mask)) {
				keyTable[i] = rem;
				valueTable[i] = valueTable[next];
				i = next;
			}
			next = next + 1 & mask;
		}
		keyTable[i] = null;
		valueTable[i] = null;
		size--;
		return oldValue;
	}

	/**
	 * Copies all of the mappings from the specified map to this map
	 * (optional operation).  The effect of this call is equivalent to that
	 * of calling {@link #put(Object, Object) put(k, v)} on this map once
	 * for each mapping from key {@code k} to value {@code v} in the
	 * specified map.  The behavior of this operation is undefined if the
	 * specified map is modified while the operation is in progress.
	 * <br>
	 * Note that {@link #putAll(ObjectObjectMap)} is more specific and can be
	 * more efficient by using the internal details of this class.
	 *
	 * @param m mappings to be stored in this map
	 * @throws UnsupportedOperationException if the {@code putAll} operation
	 *                                       is not supported by this map
	 * @throws ClassCastException            if the class of a key or value in the
	 *                                       specified map prevents it from being stored in this map
	 * @throws NullPointerException          if the specified map is null, or if
	 *                                       this map does not permit null keys or values, and the
	 *                                       specified map contains null keys or values
	 * @throws IllegalArgumentException      if some property of a key or value in
	 *                                       the specified map prevents it from being stored in this map
	 */
	@Override
	public void putAll (Map<? extends K, ? extends V> m) {
		ensureCapacity(m.size());
		for (Map.Entry<? extends K, ? extends V> kv : m.entrySet()) {put(kv.getKey(), kv.getValue());}
	}

	/**
	 * Returns true if the map has one or more items.
	 */
	public boolean notEmpty () {
		return size > 0;
	}

	/**
	 * Returns the number of key-value mappings in this map.  If the
	 * map contains more than {@code Integer.MAX_VALUE} elements, returns
	 * {@code Integer.MAX_VALUE}.
	 *
	 * @return the number of key-value mappings in this map
	 */
	@Override
	public int size () {
		return size;
	}

	/**
	 * Returns true if the map is empty.
	 */
	@Override
	public boolean isEmpty () {
		return size == 0;
	}

	/**
	 * Reduces the size of the backing arrays to be the specified capacity / loadFactor, or less. If the capacity is already less,
	 * nothing is done. If the map contains more items than the specified capacity, the next highest power of two capacity is used
	 * instead.
	 */
	public void shrink (int maximumCapacity) {
		if (maximumCapacity < 0) {throw new IllegalArgumentException("maximumCapacity must be >= 0: " + maximumCapacity);}
		int tableSize = tableSize(Math.max(maximumCapacity, size), loadFactor);
		if (keyTable.length > tableSize) {resize(tableSize);}
	}

	/**
	 * Clears the map and reduces the size of the backing arrays to be the specified capacity / loadFactor, if they are larger.
	 */
	public void clear (int maximumCapacity) {
		int tableSize = tableSize(maximumCapacity, loadFactor);
		if (keyTable.length <= tableSize) {
			clear();
			return;
		}
		size = 0;
		resize(tableSize);
	}

	@Override
	public void clear () {
		if (size == 0) {return;}
		size = 0;
		Arrays.fill(keyTable, null);
		Arrays.fill(valueTable, null);
	}

	/**
	 * Returns true if the specified value is in the map. Note this traverses the entire map and compares every value, which may
	 * be an expensive operation.
	 *
	 * @param identity If true, uses == to compare the specified value with values in the map. If false, uses
	 *                 {@link #equals(Object)}.
	 */
	public boolean containsValue (@Nullable Object value, boolean identity) {
		V[] valueTable = this.valueTable;
		if (value == null) {
			K[] keyTable = this.keyTable;
			for (int i = valueTable.length - 1; i >= 0; i--) {if (keyTable[i] != null && valueTable[i] == null) {return true;}}
		} else if (identity) {
			for (int i = valueTable.length - 1; i >= 0; i--) {if (valueTable[i] == value) {return true;}}
		} else {
			for (int i = valueTable.length - 1; i >= 0; i--) {if (value.equals(valueTable[i])) {return true;}}
		}
		return false;
	}

	@Override
	public boolean containsKey (Object key) {
		K[] keyTable = this.keyTable;
		for (int i = place(key); ; i = i + 1 & mask) {
			K other = keyTable[i];
			if (equate(key, other))
				return true;
			if (other == null)
				return false;
		}
	}

	/**
	 * Returns {@code true} if this map maps one or more keys to the
	 * specified value.  More formally, returns {@code true} if and only if
	 * this map contains at least one mapping to a value {@code v} such that
	 * {@code (value==null ? v==null : value.equals(v))}.  This operation
	 * will probably require time linear in the map size for most
	 * implementations of the {@code Map} interface.
	 *
	 * @param value value whose presence in this map is to be tested
	 * @return {@code true} if this map maps one or more keys to the
	 * specified value
	 * @throws ClassCastException   if the value is of an inappropriate type for
	 *                              this map
	 *                              (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
	 * @throws NullPointerException if the specified value is null and this
	 *                              map does not permit null values
	 *                              (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
	 */
	@Override
	public boolean containsValue (Object value) {
		return containsValue(value, false);
	}

	/**
	 * Returns the key for the specified value, or null if it is not in the map. Note this traverses the entire map and compares
	 * every value, which may be an expensive operation.
	 *
	 * @param identity If true, uses == to compare the specified value with values in the map. If false, uses
	 *                 {@link #equals(Object)}.
	 */
	@Nullable
	public K findKey (@Nullable Object value, boolean identity) {
		V[] valueTable = this.valueTable;
		if (value == null) {
			K[] keyTable = this.keyTable;
			for (int i = valueTable.length - 1; i >= 0; i--) {if (keyTable[i] != null && valueTable[i] == null) {return keyTable[i];}}
		} else if (identity) {
			for (int i = valueTable.length - 1; i >= 0; i--) {if (valueTable[i] == value) {return keyTable[i];}}
		} else {
			for (int i = valueTable.length - 1; i >= 0; i--) {if (value.equals(valueTable[i])) {return keyTable[i];}}
		}
		return null;
	}

	/**
	 * Increases the size of the backing array to accommodate the specified number of additional items / loadFactor. Useful before
	 * adding many items to avoid multiple backing array resizes.
	 *
	 * @param additionalCapacity how many additional items this should be able to hold without resizing (probably)
	 */
	public void ensureCapacity (int additionalCapacity) {
		int tableSize = tableSize(size + additionalCapacity, loadFactor);
		if (keyTable.length < tableSize) {resize(tableSize);}
	}

	protected void resize (int newSize) {
		int oldCapacity = keyTable.length;
		threshold = (int)(newSize * loadFactor);
		mask = newSize - 1;
		shift = Long.numberOfLeadingZeros(mask);

		// multiplier from Steele and Vigna, Computationally Easy, Spectrally Good Multipliers for Congruential
		// Pseudorandom Number Generators
		hashMultiplier *= 0xF1357AEA2E62A9C5L;
		// ensures hashMultiplier is never too small, and is always odd
		hashMultiplier |= 0x0000010000000001L;

		K[] oldKeyTable = keyTable;
		V[] oldValueTable = valueTable;

		keyTable = (K[])new Object[newSize];
		valueTable = (V[])new Object[newSize];

		if (size > 0) {
			for (int i = 0; i < oldCapacity; i++) {
				K key = oldKeyTable[i];
				if (key != null) {putResize(key, oldValueTable[i]);}
			}
		}
	}

	public float getLoadFactor () {
		return loadFactor;
	}

	public void setLoadFactor (float loadFactor) {
		if (loadFactor <= 0f || loadFactor > 1f) {throw new IllegalArgumentException("loadFactor must be > 0 and <= 1: " + loadFactor);}
		this.loadFactor = loadFactor;
		int tableSize = tableSize(size, loadFactor);
		if (tableSize - 1 != mask) {
			resize(tableSize);
		}
	}

	@Override
	public int hashCode () {
		int h = size;
		K[] keyTable = this.keyTable;
		V[] valueTable = this.valueTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			K key = keyTable[i];
			if (key != null) {
				h ^= key.hashCode();
				V value = valueTable[i];
				if (value != null) {h ^= value.hashCode();}
			}
		}
		return h;
	}

	@Override
	public boolean equals (Object obj) {
		if (obj == this) {return true;}
		if (!(obj instanceof ObjectObjectMap)) {return false;}
		ObjectObjectMap other = (ObjectObjectMap)obj;
		if (other.size != size) {return false;}
		K[] keyTable = this.keyTable;
		V[] valueTable = this.valueTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			K key = keyTable[i];
			if (key != null) {
				V value = valueTable[i];
				if (value == null) {
					if (other.getOrDefault(key, neverIdentical) != null) {return false;}
				} else {
					if (!value.equals(other.get(key))) {return false;}
				}
			}
		}
		return true;
	}

	/**
	 * Uses == for comparison of each value.
	 */
	public boolean equalsIdentity (@Nullable Object obj) {
		if (obj == this) {return true;}
		if (!(obj instanceof ObjectObjectMap)) {return false;}
		ObjectObjectMap other = (ObjectObjectMap)obj;
		if (other.size != size) {return false;}
		K[] keyTable = this.keyTable;
		V[] valueTable = this.valueTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			K key = keyTable[i];
			if (key != null && valueTable[i] != other.getOrDefault(key, neverIdentical)) {return false;}
		}
		return true;
	}

	public String toString (String separator) {
		return toString(separator, false);
	}

	@Override
	public String toString () {
		return toString(", ", true);
	}

	protected String toString (String separator, boolean braces) {
		if (size == 0) {return braces ? "{}" : "";}
		StringBuilder buffer = new StringBuilder(32);
		if (braces) {buffer.append('{');}
		K[] keyTable = this.keyTable;
		V[] valueTable = this.valueTable;
		int i = keyTable.length;
		while (i-- > 0) {
			K key = keyTable[i];
			if (key == null) {continue;}
			buffer.append(key == this ? "(this)" : key);
			buffer.append('=');
			V value = valueTable[i];
			buffer.append(value == this ? "(this)" : value);
			break;
		}
		while (i-- > 0) {
			K key = keyTable[i];
			if (key == null) {continue;}
			buffer.append(separator);
			buffer.append(key == this ? "(this)" : key);
			buffer.append('=');
			V value = valueTable[i];
			buffer.append(value == this ? "(this)" : value);
		}
		if (braces) {buffer.append('}');}
		return buffer.toString();
	}

	/**
	 * Reduces the size of the map to the specified size. If the map is already smaller than the specified
	 * size, no action is taken. This indiscriminately removes items from the backing array until the
	 * requested newSize is reached, or until the full backing array has had its elements removed.
	 *
	 * @param newSize the target size to try to reach by removing items, if smaller than the current size
	 */
	public void truncate (int newSize) {
		K[] keyTable = this.keyTable;
		V[] valTable = this.valueTable;
		for (int i = 0; i < keyTable.length && size > newSize; i++) {
			if (keyTable[i] != null) {
				keyTable[i] = null;
				valTable[i] = null;
				--size;
			}
		}
	}

	/**
	 * Reuses the iterator of the reused {@link Entries} produced by {@link #entrySet()};
	 * does not permit nested iteration. Iterate over {@link Entries#Entries(ObjectObjectMap)} if you
	 * need nested or multithreaded iteration. You can remove an Entry from this ObjectObjectMap
	 * using this Iterator.
	 *
	 * @return an {@link Iterator} over {@link Map.Entry} key-value pairs; remove is supported.
	 */
	@Override
	public Iterator<Map.Entry<K, V>> iterator () {
		return entrySet().iterator();
	}

	/**
	 * Returns a {@link Set} view of the keys contained in this map.
	 * The set is backed by the map, so changes to the map are
	 * reflected in the set, and vice-versa.  If the map is modified
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
	 * method is called. Use the {@link Keys} constructor for nested or
	 * multithreaded iteration.
	 *
	 * @return a set view of the keys contained in this map
	 */
	@Override
	public Keys<K, V> keySet () {
		if (keys1 == null || keys2 == null) {
			keys1 = new Keys<>(this);
			keys2 = new Keys<>(this);
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
	 * Returns a Collection of the values in the map. Remove is supported. Note that the same Collection instance is returned each
	 * time this method is called. Use the {@link Values} constructor for nested or multithreaded iteration.
	 *
	 * @return a {@link Collection} of V values
	 */
	@Override
	public Values<K, V> values () {
		if (values1 == null || values2 == null) {
			values1 = new Values<>(this);
			values2 = new Values<>(this);
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
	 * Note that the same iterator instance is returned each time this method is called.
	 * Use the {@link Entries} constructor for nested or multithreaded iteration.
	 *
	 * @return a {@link Set} of {@link Map.Entry} key-value pairs
	 */
	@Override
	public Entries<K, V> entrySet () {
		if (entries1 == null || entries2 == null) {
			entries1 = new Entries<>(this);
			entries2 = new Entries<>(this);
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

	public static class Entry<K, V> implements Map.Entry<K, V> {
		@Nullable public K key;
		@Nullable public V value;

		@Override
		@Nullable
		public String toString () {
			return key + "=" + value;
		}

		/**
		 * Returns the key corresponding to this entry.
		 *
		 * @return the key corresponding to this entry
		 * @throws IllegalStateException implementations may, but are not
		 *                               required to, throw this exception if the entry has been
		 *                               removed from the backing map.
		 */
		@Override
		public K getKey () {
			Objects.requireNonNull(key);
			return key;
		}

		/**
		 * Returns the value corresponding to this entry.  If the mapping
		 * has been removed from the backing map (by the iterator's
		 * {@code remove} operation), the results of this call are undefined.
		 *
		 * @return the value corresponding to this entry
		 * @throws IllegalStateException implementations may, but are not
		 *                               required to, throw this exception if the entry has been
		 *                               removed from the backing map.
		 */
		@Override
		@Nullable
		public V getValue () {
			return value;
		}

		/**
		 * Replaces the value corresponding to this entry with the specified
		 * value (optional operation).  (Writes through to the map.)  The
		 * behavior of this call is undefined if the mapping has already been
		 * removed from the map (by the iterator's {@code remove} operation).
		 *
		 * @param value new value to be stored in this entry
		 * @return old value corresponding to the entry
		 * @throws UnsupportedOperationException if the {@code put} operation
		 *                                       is not supported by the backing map
		 * @throws ClassCastException            if the class of the specified value
		 *                                       prevents it from being stored in the backing map
		 * @throws NullPointerException          if the backing map does not permit
		 *                                       null values, and the specified value is null
		 * @throws IllegalArgumentException      if some property of this value
		 *                                       prevents it from being stored in the backing map
		 * @throws IllegalStateException         implementations may, but are not
		 *                                       required to, throw this exception if the entry has been
		 *                                       removed from the backing map.
		 */
		@Override
		@Nullable
		public V setValue (V value) {
			V old = this.value;
			this.value = value;
			return old;
		}

		@Override
		public boolean equals (@Nullable Object o) {
			if (this == o) {return true;}
			if (o == null || getClass() != o.getClass()) {return false;}

			Entry<?, ?> entry = (Entry<?, ?>)o;

			if (!Objects.equals(key, entry.key)) {return false;}
			return Objects.equals(value, entry.value);
		}

		@Override
		public int hashCode () {
			int result = key != null ? key.hashCode() : 0;
			result = 31 * result + (value != null ? value.hashCode() : 0);
			return result;
		}
	}

	static protected abstract class MapIterator<K, V, I> implements Iterable<I>, Iterator<I> {
		public boolean hasNext;

		protected final ObjectObjectMap<K, V> map;
		protected int nextIndex, currentIndex;
		protected boolean valid = true;

		public MapIterator (ObjectObjectMap<K, V> map) {
			this.map = map;
			reset();
		}

		public void reset () {
			currentIndex = -1;
			nextIndex = -1;
			findNextIndex();
		}

		void findNextIndex () {
			K[] keyTable = map.keyTable;
			for (int n = keyTable.length; ++nextIndex < n; ) {
				if (keyTable[nextIndex] != null) {
					hasNext = true;
					return;
				}
			}
			hasNext = false;
		}

		@Override
		public void remove () {
			int i = currentIndex;
			if (i < 0) {throw new IllegalStateException("next must be called before remove.");}
			K[] keyTable = map.keyTable;
			V[] valueTable = map.valueTable;
			int mask = map.mask, next = i + 1 & mask;
			K key;
			while ((key = keyTable[next]) != null) {
				int placement = map.place(key);
				if ((next - placement & mask) > (i - placement & mask)) {
					keyTable[i] = key;
					valueTable[i] = valueTable[next];
					i = next;
				}
				next = next + 1 & mask;
			}
			keyTable[i] = null;
			valueTable[i] = null;
			map.size--;
			if (i != currentIndex) {--nextIndex;}
			currentIndex = -1;
		}
	}

	public static class Entries<K, V> extends AbstractSet<Map.Entry<K, V>> {
		protected Entry<K, V> entry = new Entry<>();
		protected MapIterator<K, V, Map.Entry<K, V>> iter;

		public Entries (ObjectObjectMap<K, V> map) {
			iter = new MapIterator<K, V, Map.Entry<K, V>>(map) {
				@Override
				public Iterator<Map.Entry<K, V>> iterator () {
					return this;
				}

				/** Note the same entry instance is returned each time this method is called. */
				@Override
				public Map.Entry<K, V> next () {
					if (!hasNext) {throw new NoSuchElementException();}
					if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
					K[] keyTable = map.keyTable;
					entry.key = keyTable[nextIndex];
					entry.value = map.valueTable[nextIndex];
					currentIndex = nextIndex;
					findNextIndex();
					return entry;
				}

				@Override
				public boolean hasNext () {
					if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
					return hasNext;
				}
			};
		}

		@Override
		public boolean contains (Object o) {
			return iter.map.containsKey(o);
		}

		/**
		 * Returns an iterator over the elements contained in this collection.
		 *
		 * @return an iterator over the elements contained in this collection
		 */
		@Override
		public Iterator<Map.Entry<K, V>> iterator () {
			return iter;
		}

		@Override
		public int size () {
			return iter.map.size;
		}
	}

	public static class Values<K, V> extends AbstractCollection<V> {
		protected MapIterator<K, V, V> iter;

		public Values (ObjectObjectMap<K, V> map) {
			iter = new MapIterator<K, V, V>(map) {
				@Override
				public Iterator<V> iterator () {
					return this;
				}

				@Override
				public boolean hasNext () {
					if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
					return hasNext;
				}

				@Override
				public V next () {
					if (!hasNext) {throw new NoSuchElementException();}
					if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
					V value = map.valueTable[nextIndex];
					currentIndex = nextIndex;
					findNextIndex();
					return value;
				}
			};

		}

		/**
		 * Returns an iterator over the elements contained in this collection.
		 *
		 * @return an iterator over the elements contained in this collection
		 */
		@Override
		public Iterator<V> iterator () {
			return iter;
		}

		@Override
		public int size () {
			return iter.map.size;
		}

	}

	public static class Keys<K, V> extends AbstractSet<K> {
		protected MapIterator<K, V, K> iter;

		public Keys (ObjectObjectMap<K, V> map) {
			iter = new MapIterator<K, V, K>(map) {
				@Override
				public Iterator<K> iterator () {
					return this;
				}

				@Override
				public boolean hasNext () {
					if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
					return hasNext;
				}

				@Override
				public K next () {
					if (!hasNext) {throw new NoSuchElementException();}
					if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
					K key = map.keyTable[nextIndex];
					currentIndex = nextIndex;
					findNextIndex();
					return key;
				}
			};
		}

		@Override
		public boolean contains (Object o) {
			return iter.map.containsKey(o);
		}

		/**
		 * Returns an iterator over the elements contained in this collection.
		 *
		 * @return an iterator over the elements contained in this collection
		 */
		@Override
		public Iterator<K> iterator () {
			return iter;
		}

		@Override
		public int size () {
			return iter.map.size;
		}

		@Override
		public int hashCode () {
			int h = 0;
			iter.reset();
			while (iter.hasNext()) {
				K obj = iter.next();
				if (obj != null)
					h += obj.hashCode();
			}
			return h;
		}
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
	public static <K, V> ObjectObjectMap<K, V> with (K key0, V value0) {
		ObjectObjectMap<K, V> map = new ObjectObjectMap<>(1);
		map.put(key0, value0);
		return map;
	}

	/**
	 * Constructs a map given alternating keys and values.
	 * This can be useful in some code-generation scenarios, or when you want to make a
	 * map conveniently by-hand and have it populated at the start. You can also use
	 * {@link #ObjectObjectMap(Object[], Object[])}, which takes all keys and then all values.
	 * This needs all keys to have the same type and all values to have the same type, because
	 * it gets those types from the first key parameter and first value parameter. Any keys that don't
	 * have K as their type or values that don't have V as their type have that entry skipped.
	 *
	 * @param key0   the first key; will be used to determine the type of all keys
	 * @param value0 the first value; will be used to determine the type of all values
	 * @param rest   an array or varargs of alternating K, V, K, V... elements
	 * @param <K>    the type of keys, inferred from key0
	 * @param <V>    the type of values, inferred from value0
	 * @return a new map containing the given keys and values
	 */
	@SuppressWarnings("unchecked")
	public static <K, V> ObjectObjectMap<K, V> with (K key0, V value0, Object... rest) {
		ObjectObjectMap<K, V> map = new ObjectObjectMap<>(1 + (rest.length >>> 1));
		map.put(key0, value0);
		for (int i = 1; i < rest.length; i += 2) {
			try {
				map.put((K)rest[i - 1], (V)rest[i]);
			} catch (ClassCastException ignored) {
			}
		}
		return map;
	}
}

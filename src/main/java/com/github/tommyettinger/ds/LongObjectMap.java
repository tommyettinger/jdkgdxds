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
import com.github.tommyettinger.digital.BitConversion;
import com.github.tommyettinger.ds.support.util.*;
import com.github.tommyettinger.function.LongObjBiConsumer;
import com.github.tommyettinger.function.LongObjToObjBiFunction;

import com.github.tommyettinger.function.LongToObjFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

import com.github.tommyettinger.function.ObjObjToObjBiFunction;

import static com.github.tommyettinger.ds.Utilities.neverIdentical;
import static com.github.tommyettinger.ds.Utilities.tableSize;

/**
 * An unordered map where the keys are unboxed longs and the values are objects. Null keys are not allowed. No allocation is
 * done except when growing the table size.
 * <p>
 * This class performs fast contains and remove (typically O(1), worst case O(n) but that is rare in practice). Add may be
 * slightly slower, depending on hash collisions. Hashcodes are rehashed to reduce collisions and the need to resize. Load factors
 * greater than 0.91 greatly increase the chances to resize to the next higher POT size.
 * <p>
 * Unordered sets and maps are not designed to provide especially fast iteration. Iteration is faster with {@link Ordered} types like
 * ObjectOrderedSet and ObjectObjectOrderedMap.
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
public class LongObjectMap<V> implements Iterable<LongObjectMap.Entry<V>> {

	protected int size;

	protected long[] keyTable;
	protected @Nullable V[] valueTable;
	protected boolean hasZeroValue;
	@Nullable
	protected V zeroValue;

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
	 * Used by {@link #place(long)} to bit shift the upper bits of an {@code int} into a usable range (&gt;= 0 and &lt;=
	 * {@link #mask}). The shift can be negative, which is convenient to match the number of bits in mask: if mask is a 7-bit
	 * number, a shift of -7 shifts the upper 7 bits into the lowest 7 positions. This class sets the shift &gt; 32 and &lt; 64,
	 * which when used with an int will still move the upper bits of an int to the lower bits due to Java's implicit modulus on
	 * shifts.
	 * <p>
	 * {@link #mask} can also be used to mask the low bits of a number, which may be faster for some hashcodes, if
	 * {@link #place(long)} is overridden.
	 */
	protected int shift;

	/**
	 * A bitmask used to confine hashcodes to the size of the tables. Must be all 1 bits in its low positions, ie a power of two
	 * minus 1. If {@link #place(long)} is overridden, this can be used instead of {@link #shift} to isolate usable bits of a
	 * hash.
	 */
	protected int mask;

	/**
	 * Used by {@link #place(long)} to mix hashCode() results. Changes on every call to {@link #resize(int)} by default.
	 * This should always change when {@link #shift} changes, meaning, when the backing table resizes.
	 * This only needs to be serialized if the full key and value tables are serialized, or if the iteration order should be
	 * the same before and after serialization. Iteration order is better handled by using {@link LongObjectOrderedMap}.
	 */
	protected int hashMultiplier;

	@Nullable
	protected transient Entries<V> entries1;
	@Nullable
	protected transient Entries<V> entries2;
	@Nullable
	protected transient Values<V> values1;
	@Nullable
	protected transient Values<V> values2;
	@Nullable
	protected transient Keys<V> keys1;
	@Nullable
	protected transient Keys<V> keys2;

	/**
	 * Returned by {@link #get(long)} when no value exists for the given key, as well as some other methods to indicate that
	 * no value in the Map could be returned.
	 */
	@Nullable
	public V defaultValue = null;

	/**
	 * Creates a new map with an initial capacity of {@link Utilities#getDefaultTableCapacity()} and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 */
	public LongObjectMap() {
		this(Utilities.getDefaultTableCapacity(), Utilities.getDefaultLoadFactor());
	}

	/**
	 * Creates a new map with the given starting capacity and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 */
	public LongObjectMap(int initialCapacity) {
		this(initialCapacity, Utilities.getDefaultLoadFactor());
	}

	/**
	 * Creates a new map with the specified initial capacity and load factor. This map will hold initialCapacity items before
	 * growing the backing table.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 * @param loadFactor      what fraction of the capacity can be filled before this has to resize; 0 &lt; loadFactor &lt;= 1
	 */
	public LongObjectMap(int initialCapacity, float loadFactor) {
		if (loadFactor <= 0f || loadFactor > 1f) {
			throw new IllegalArgumentException("loadFactor must be > 0 and <= 1: " + loadFactor);
		}
		this.loadFactor = loadFactor;

		int tableSize = tableSize(initialCapacity, loadFactor);
		threshold = (int) (tableSize * loadFactor);
		mask = tableSize - 1;
		shift = BitConversion.countLeadingZeros(mask) + 32;
		hashMultiplier = Utilities.GOOD_MULTIPLIERS[64 - shift];

		keyTable = new long[tableSize];
		valueTable = (V[]) new Object[tableSize];
	}

	/**
	 * Creates a new map identical to the specified map.
	 * This performs a shallow copy, so any references to values (as well as the default value) are shared with the old map.
	 *
	 * @param map the map to copy
	 */
	public LongObjectMap(LongObjectMap<? extends V> map) {
		this((int) (map.keyTable.length * map.loadFactor), map.loadFactor);
		System.arraycopy(map.keyTable, 0, keyTable, 0, map.keyTable.length);
		System.arraycopy(map.valueTable, 0, valueTable, 0, map.valueTable.length);
		size = map.size;
		defaultValue = map.defaultValue;
		hashMultiplier = map.hashMultiplier;
		zeroValue = map.zeroValue;
		hasZeroValue = map.hasZeroValue;
	}

	/**
	 * Given two side-by-side arrays, one of keys, one of values, this constructs a map and inserts each pair of key and value into it.
	 * If keys and values have different lengths, this only uses the length of the smaller array.
	 *
	 * @param keys   an array of keys
	 * @param values an array of values
	 */
	public LongObjectMap(long[] keys, V[] values) {
		this(Math.min(keys.length, values.length));
		putAll(keys, values);
	}

	/**
	 * Returns an index &gt;= 0 and &lt;= {@link #mask} for the specified {@code item}.
	 *
	 * @param item any long; it is usually mixed or masked here
	 * @return an index between 0 and {@link #mask} (both inclusive)
	 */
	protected int place(long item) {
		return (int) (hashMultiplier * (item ^ item << 32) >>> shift);
	}

	/**
	 * Returns the index of the key if already present, else {@code ~index} for the next empty index.
	 * While this can be overridden to compare for equality differently than {@code ==} between ints, that
	 * isn't recommended because this has to treat zero keys differently, and it finds those with {@code ==}.
	 * If you want to treat equality between longs differently for some reason, you would also need to override
	 * {@link #containsKey(long)} and {@link #get(long)}, at the very least.
	 */
	protected int locateKey(long key) {
		long[] keyTable = this.keyTable;
		for (int i = place(key); ; i = i + 1 & mask) {
			long other = keyTable[i];
			if (other == 0) {
				return ~i; // Empty space is available.
			}
			if (other == key) {
				return i; // Same key was found.
			}
		}
	}

	/**
	 * Returns the old value associated with the specified key, or this map's {@link #defaultValue} if there was no prior value.
	 */
	@Nullable
	public V put(long key, @Nullable V value) {
		if (key == 0) {
			V oldValue = defaultValue;
			if (hasZeroValue) {
				oldValue = zeroValue;
			} else {
				size++;
			}
			hasZeroValue = true;
			zeroValue = value;
			return oldValue;
		}
		int i = locateKey(key);
		if (i >= 0) { // Existing key was found.
			V oldValue = valueTable[i];
			valueTable[i] = value;
			return oldValue;
		}
		i = ~i; // Empty space was found.
		keyTable[i] = key;
		valueTable[i] = value;
		if (++size >= threshold) {
			resize(keyTable.length << 1);
		}
		return defaultValue;
	}

	/**
	 * Returns the old value associated with the specified key, or the given {@code defaultValue} if there was no prior value.
	 */
	@Nullable
	public V putOrDefault(long key, @Nullable V value, @Nullable V defaultValue) {
		if (key == 0) {
			V oldValue = defaultValue;
			if (hasZeroValue) {
				oldValue = zeroValue;
			} else {
				size++;
			}
			hasZeroValue = true;
			zeroValue = value;
			return oldValue;
		}
		int i = locateKey(key);
		if (i >= 0) { // Existing key was found.
			V oldValue = valueTable[i];
			valueTable[i] = value;
			return oldValue;
		}
		i = ~i; // Empty space was found.
		keyTable[i] = key;
		valueTable[i] = value;
		if (++size >= threshold) {
			resize(keyTable.length << 1);
		}
		return defaultValue;
	}

	/**
	 * Puts every key-value pair in the given map into this, with the values from the given map
	 * overwriting the previous values if two keys are identical.
	 *
	 * @param map a map with compatible key and value types; will not be modified
	 */
	public void putAll(LongObjectMap<? extends V> map) {
		ensureCapacity(map.size);
		if (map.hasZeroValue) {
			if (!hasZeroValue) {
				size++;
			}
			hasZeroValue = true;
			zeroValue = map.zeroValue;
		}
		long[] keyTable = map.keyTable;
		V[] valueTable = map.valueTable;
		long key;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			key = keyTable[i];
			if (key != 0) {
				put(key, valueTable[i]);
			}
		}
	}

	/**
	 * Given two side-by-side arrays, one of keys, one of values, this inserts each pair of key and value into this map with put().
	 *
	 * @param keys   an array of keys
	 * @param values an array of values
	 */
	public void putAll(long[] keys, V[] values) {
		putAll(keys, 0, values, 0, Math.min(keys.length, values.length));
	}

	/**
	 * Given two side-by-side arrays, one of keys, one of values, this inserts each pair of key and value into this map with put().
	 *
	 * @param keys   an array of keys
	 * @param values an array of values
	 * @param length how many items from keys and values to insert, at-most
	 */
	public void putAll(long[] keys, V[] values, int length) {
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
	public void putAll(long[] keys, int keyOffset, V[] values, int valueOffset, int length) {
		length = Math.min(length, Math.min(keys.length - keyOffset, values.length - valueOffset));
		ensureCapacity(length);
		for (int k = keyOffset, v = valueOffset, i = 0, n = length; i < n; i++, k++, v++) {
			put(keys[k], values[v]);
		}
	}

	/**
	 * Given two side-by-side collections, one of keys, one of values, this constructs a map and inserts each pair of key and value into it.
	 * If keys and values have different lengths, this only uses the length of the smaller collection.
	 *
	 * @param keys   a PrimitiveCollection of keys
	 * @param values a PrimitiveCollection of values
	 */
	public LongObjectMap(PrimitiveCollection.OfLong keys, Collection<? extends V> values) {
		this(Math.min(keys.size(), values.size()));
		putAll(keys, values);
	}

	/**
	 * Given two side-by-side collections, one of keys, one of values, this inserts each pair of key and value into this map with put().
	 *
	 * @param keys   a PrimitiveCollection of keys
	 * @param values a PrimitiveCollection of values
	 */
	public void putAll(PrimitiveCollection.OfLong keys, Collection<? extends V> values) {
		int length = Math.min(keys.size(), values.size());
		ensureCapacity(length);
		LongIterator ki = keys.iterator();
		Iterator<? extends V> vi = values.iterator();
		while (ki.hasNext() && vi.hasNext()) {
			put(ki.nextLong(), vi.next());
		}
	}

	/**
	 * Skips checks for existing keys, doesn't increment size.
	 */
	protected void putResize(long key, @Nullable V value) {
		long[] keyTable = this.keyTable;
		for (int i = place(key); ; i = i + 1 & mask) {
			if (keyTable[i] == 0) {
				keyTable[i] = key;
				valueTable[i] = value;
				return;
			}
		}
	}

	/**
	 * Returns the value for the specified key, or {@link #defaultValue} if the key is not in the map.
	 *
	 * @param key any {@code long}
	 */
	@Nullable
	public V get(long key) {
		if (key == 0) {
			return hasZeroValue ? zeroValue : defaultValue;
		}
		long[] keyTable = this.keyTable;
		for (int i = place(key); ; i = i + 1 & mask) {
			long other = keyTable[i];
			if (other == 0)
				return defaultValue;
			if (other == key)
				return valueTable[i];
		}
	}

	/**
	 * Returns the value for the specified key, or the default value if the key is not in the map.
	 */
	@Nullable
	public V getOrDefault(long key, @Nullable V defaultValue) {
		if (key == 0) {
			return hasZeroValue ? zeroValue : defaultValue;
		}
		long[] keyTable = this.keyTable;
		for (int i = place(key); ; i = i + 1 & mask) {
			long other = keyTable[i];
			if (other == 0)
				return defaultValue;
			if (other == key)
				return valueTable[i];
		}
	}

	@Nullable
	public V remove(long key) {
		if (key == 0) {
			if (hasZeroValue) {
				hasZeroValue = false;
				--size;
				@Nullable V oldValue = zeroValue;
				zeroValue = null;
				return oldValue;
			}
			return defaultValue;
		}
		int pos = locateKey(key);
		if (pos < 0) return defaultValue;
		long[] keyTable = this.keyTable;
		@Nullable V[] valueTable = this.valueTable;
		@Nullable V oldValue = valueTable[pos];

		int mask = this.mask, last, slot;
		size--;
		for (; ; ) {
			pos = ((last = pos) + 1) & mask;
			for (; ; ) {
				if ((key = keyTable[pos]) == 0) {
					keyTable[last] = 0;
					valueTable[last] = null;
					return oldValue;
				}
				slot = place(key);
				if (last <= pos ? last >= slot || slot > pos : last >= slot && slot > pos) break;
				pos = (pos + 1) & mask;
			}
			keyTable[last] = key;
			valueTable[last] = valueTable[pos];
		}
	}

	/**
	 * Returns true if the map has one or more items.
	 */
	public boolean notEmpty() {
		return size != 0;
	}

	/**
	 * Returns the number of key-value mappings in this map.  If the
	 * map contains more than {@code Integer.MAX_VALUE} elements, returns
	 * {@code Integer.MAX_VALUE}.
	 *
	 * @return the number of key-value mappings in this map
	 */
	public int size() {
		return size;
	}

	/**
	 * Returns true if the map is empty.
	 */
	public boolean isEmpty() {
		return size == 0;
	}

	/**
	 * Gets the default value, a {@code V} which is returned by {@link #get(long)} if the key is not found.
	 * If not changed, the default value is null.
	 *
	 * @return the current default value
	 */
	@Nullable
	public V getDefaultValue() {
		return defaultValue;
	}

	/**
	 * Sets the default value, a {@code V} which is returned by {@link #get(long)} if the key is not found.
	 * If not changed, the default value is null. Note that {@link #getOrDefault(long, Object)} is also available,
	 * which allows specifying a "not-found" value per-call.
	 *
	 * @param defaultValue may be any V object or null; should usually be one that doesn't occur as a typical value
	 */
	public void setDefaultValue(@Nullable V defaultValue) {
		this.defaultValue = defaultValue;
	}

	/**
	 * Reduces the size of the backing arrays to be the specified capacity / loadFactor, or less. If the capacity is already less,
	 * nothing is done. If the map contains more items than the specified capacity, the next highest power of two capacity is used
	 * instead.
	 */
	public void shrink(int maximumCapacity) {
		if (maximumCapacity < 0) {
			throw new IllegalArgumentException("maximumCapacity must be >= 0: " + maximumCapacity);
		}
		int tableSize = tableSize(Math.max(maximumCapacity, size), loadFactor);
		if (keyTable.length > tableSize) {
			resize(tableSize);
		}
	}

	/**
	 * Clears the map and reduces the size of the backing arrays to be the specified capacity / loadFactor, if they are larger.
	 */
	public void clear(int maximumCapacity) {
		int tableSize = tableSize(maximumCapacity, loadFactor);
		if (keyTable.length <= tableSize) {
			clear();
			return;
		}
		hasZeroValue = false;
		zeroValue = null;
		size = 0;
		resize(tableSize);
	}

	public void clear() {
		if (size == 0) {
			return;
		}
		hasZeroValue = false;
		zeroValue = null;
		size = 0;
		Arrays.fill(keyTable, 0);
		Utilities.clear(valueTable);
	}

	/**
	 * Returns true if the specified value is in the map. Note this traverses the entire map and compares every value, which may
	 * be an expensive operation.
	 */
	public boolean containsValue(@Nullable Object value) {
		if (hasZeroValue) {
			return Objects.equals(zeroValue, value);
		}
		V[] valueTable = this.valueTable;
		long[] keyTable = this.keyTable;
		for (int i = valueTable.length - 1; i >= 0; i--) {
			if (keyTable[i] != 0 && Objects.equals(valueTable[i], value)) {
				return true;
			}
		}
		return false;
	}

	public boolean containsKey(long key) {
		if (key == 0) {
			return hasZeroValue;
		}
		long[] keyTable = this.keyTable;
		for (int i = place(key); ; i = i + 1 & mask) {
			long other = keyTable[i];
			if (other == 0)
				return false;
			if (other == key)
				return true;
		}
	}

	/**
	 * Returns a key that maps to the specified value, or {@code defaultKey} if value is not in the map.
	 * Note, this traverses the entire map and compares
	 * every value, which may be an expensive operation.
	 *
	 * @param value      the value to search for
	 * @param defaultKey the key to return when value cannot be found
	 * @return a key that maps to value, if present, or defaultKey if value cannot be found
	 */
	public long findKey(@Nullable V value, long defaultKey) {
		if (hasZeroValue && Objects.equals(zeroValue, value)) {
			return 0;
		}
		V[] valueTable = this.valueTable;
		long[] keyTable = this.keyTable;
		for (int i = valueTable.length - 1; i >= 0; i--) {
			if (keyTable[i] != 0 && Objects.equals(valueTable[i], value)) {
				return keyTable[i];
			}
		}

		return defaultKey;
	}

	/**
	 * Increases the size of the backing array to accommodate the specified number of additional items / loadFactor. Useful before
	 * adding many items to avoid multiple backing array resizes.
	 */
	public void ensureCapacity(int additionalCapacity) {
		int tableSize = tableSize(size + additionalCapacity, loadFactor);
		if (keyTable.length < tableSize) {
			resize(tableSize);
		}
	}

	protected void resize(int newSize) {
		int oldCapacity = keyTable.length;
		threshold = (int) (newSize * loadFactor);
		mask = newSize - 1;
		shift = BitConversion.countLeadingZeros(mask) + 32;
		hashMultiplier = Utilities.GOOD_MULTIPLIERS[64 - shift];

		long[] oldKeyTable = keyTable;
		V[] oldValueTable = valueTable;

		keyTable = new long[newSize];
		valueTable = (V[]) new Object[newSize];

		if (size > 0) {
			for (int i = 0; i < oldCapacity; i++) {
				long key = oldKeyTable[i];
				if (key != 0) {
					putResize(key, oldValueTable[i]);
				}
			}
		}
	}

	/**
	 * Gets the current hashMultiplier, used in {@link #place(long)} to mix hash codes.
	 * If {@link #setHashMultiplier(int)} is never called, the hashMultiplier will always be drawn from
	 * {@link Utilities#GOOD_MULTIPLIERS}, with the index equal to {@code 64 - shift}.
	 *
	 * @return the current hashMultiplier
	 */
	public int getHashMultiplier() {
		return hashMultiplier;
	}

	/**
	 * Sets the hashMultiplier to the given int, which will be made odd if even and always negative (by OR-ing with
	 * 0x80000001). This can be any negative, odd int, but should almost always be drawn from
	 * {@link Utilities#GOOD_MULTIPLIERS} or something like it.
	 *
	 * @param hashMultiplier any int; will be made odd if even.
	 */
	public void setHashMultiplier(int hashMultiplier) {
		this.hashMultiplier = hashMultiplier | 0x80000001;
	}

	/**
	 * Gets the length of the internal array used to store all keys, as well as empty space awaiting more items to be
	 * entered. This length is equal to the length of the array used to store all values, and empty space for values,
	 * here. This is also called the capacity.
	 *
	 * @return the length of the internal array that holds all keys
	 */
	public int getTableSize() {
		return keyTable.length;
	}

	public float getLoadFactor() {
		return loadFactor;
	}

	public void setLoadFactor(float loadFactor) {
		if (loadFactor <= 0f || loadFactor > 1f) {
			throw new IllegalArgumentException("loadFactor must be > 0 and <= 1: " + loadFactor);
		}
		this.loadFactor = loadFactor;
		int tableSize = tableSize(size, loadFactor);
		if (tableSize - 1 != mask) {
			resize(tableSize);
		}
	}

	@Override
	public int hashCode() {
		long h = hasZeroValue && zeroValue != null ? zeroValue.hashCode() * 0x9E3779B97F4A7C15L + size : size;
		long[] keyTable = this.keyTable;
		V[] valueTable = this.valueTable;
		V v;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			long key = keyTable[i];
			if (key != 0) {
				h += key ^ key >>> 32;
				v = valueTable[i];
				if (v != null)
					h += v.hashCode() * 0x9E3779B97F4A7C15L;
			}
		}
		return (int) (h ^ h >>> 32);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof LongObjectMap)) {
			return false;
		}
		LongObjectMap other = (LongObjectMap) obj;
		if (other.size != size) {
			return false;
		}
		if (other.hasZeroValue != hasZeroValue || !Objects.equals(other.zeroValue, zeroValue)) {
			return false;
		}
		long[] keyTable = this.keyTable;
		V[] valueTable = this.valueTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			long key = keyTable[i];
			if (key != 0) {
				V value = valueTable[i];
				if (value == null) {
					if (other.getOrDefault(key, neverIdentical) != null) {
						return false;
					}
				} else {
					if (!value.equals(other.get(key))) {
						return false;
					}
				}
			}
		}
		return true;
	}

	@Override
	public String toString() {
		return toString(", ", true);
	}

	/**
	 * Delegates to {@link #toString(String, boolean)} with the given entrySeparator and without braces.
	 * This is different from {@link #toString()}, which includes braces by default.
	 *
	 * @param entrySeparator how to separate entries, such as {@code ", "}
	 * @return a new String representing this map
	 */
	public String toString(String entrySeparator) {
		return toString(entrySeparator, false);
	}

	public String toString(String entrySeparator, boolean braces) {
		return appendTo(new StringBuilder(32), entrySeparator, braces).toString();
	}

	/**
	 * Makes a String from the contents of this LongObjectMap, but uses the given {@link LongAppender} and
	 * {@link Appender} to convert each key and each value to a customizable representation and append them
	 * to a temporary StringBuilder. These functions are often method references to methods in Base, such as
	 * {@link Base#appendReadable(CharSequence, long)} and {@link Base#appendUnsigned(CharSequence, long)}. To use
	 * the default String representation, you can use {@link LongAppender#DEFAULT} as a keyAppender or
	 * {@code Appender::append} as a valueAppender.
	 *
	 * @param entrySeparator    how to separate entries, such as {@code ", "}
	 * @param keyValueSeparator how to separate each key from its value, such as {@code "="} or {@code ":"}
	 * @param braces            true to wrap the output in curly braces, or false to omit them
	 * @param keyAppender       a function that takes a StringBuilder and a long, and returns the modified StringBuilder
	 * @param valueAppender     a function that takes a StringBuilder and a V, and returns the modified StringBuilder
	 * @return a new String representing this map
	 */
	public String toString(String entrySeparator, String keyValueSeparator, boolean braces,
						   LongAppender keyAppender, Appender<V> valueAppender) {
		return appendTo(new StringBuilder(), entrySeparator, keyValueSeparator, braces, keyAppender, valueAppender).toString();
	}

	public StringBuilder appendTo(StringBuilder sb, String entrySeparator, boolean braces) {
		return appendTo(sb, entrySeparator, "=", braces, LongAppender.DEFAULT, Appender::append);
	}

	/**
	 * Appends to a StringBuilder from the contents of this LongFloatMap, but uses the given {@link LongAppender} and
	 * {@link Appender} to convert each key and each value to a customizable representation and append them
	 * to a StringBuilder. These functions are often method references to methods in Base, such as
	 * {@link Base#appendReadable(CharSequence, long)} and {@link Base#appendUnsigned(CharSequence, long)}. To use
	 * the default String representation, you can use {@code Appender::append} as an appender. To write values
	 * so that they can be read back as Java source code, use {@code Base::appendReadable} for the keyAppender.
	 *
	 * @param sb                a StringBuilder that this can append to
	 * @param entrySeparator    how to separate entries, such as {@code ", "}
	 * @param keyValueSeparator how to separate each key from its value, such as {@code "="} or {@code ":"}
	 * @param braces            true to wrap the output in curly braces, or false to omit them
	 * @param keyAppender       a function that takes a StringBuilder and a long, and returns the modified StringBuilder
	 * @param valueAppender     a function that takes a StringBuilder and a V, and returns the modified StringBuilder
	 * @return {@code sb}, with the appended keys and values of this map
	 */
	public StringBuilder appendTo(StringBuilder sb, String entrySeparator, String keyValueSeparator, boolean braces,
								  LongAppender keyAppender, Appender<V> valueAppender) {
		if (size == 0) {
			return braces ? sb.append("{}") : sb;
		}
		if (braces) {
			sb.append('{');
		}
		if (hasZeroValue) {
			keyAppender.apply(sb, 0).append(keyValueSeparator);
			valueAppender.apply(sb, zeroValue);
			if (zeroValue == this)
				sb.append("(this)");
			else
				valueAppender.apply(sb, zeroValue);

			if (size > 1) {
				sb.append(entrySeparator);
			}
		}
		long[] keyTable = this.keyTable;
		V[] valueTable = this.valueTable;
		int i = keyTable.length;
		while (i-- > 0) {
			long key = keyTable[i];
			if (key == 0) {
				continue;
			}
			keyAppender.apply(sb, key).append(keyValueSeparator);
			V value = valueTable[i];
			if (value == this)
				sb.append("(this)");
			else
				valueAppender.apply(sb, value);
			break;
		}
		while (i-- > 0) {
			long key = keyTable[i];
			if (key == 0) {
				continue;
			}
			sb.append(entrySeparator);
			keyAppender.apply(sb, key).append(keyValueSeparator);
			V value = valueTable[i];
			if (value == this)
				sb.append("(this)");
			else
				valueAppender.apply(sb, value);

		}
		if (braces) {
			sb.append('}');
		}
		return sb;
	}

	/**
	 * Performs the given action for each entry in this map until all entries
	 * have been processed or the action throws an exception.  Unless
	 * otherwise specified by the implementing class, actions are performed in
	 * the order of entry set iteration (if an iteration order is specified.)
	 * Exceptions thrown by the action are relayed to the caller.
	 *
	 * @param action The action to be performed for each entry
	 */
	public void forEach(LongObjBiConsumer<? super V> action) {
		for (Entry<V> entry : entrySet()) {
			action.accept(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * Replaces each entry's value with the result of invoking the given
	 * function on that entry until all entries have been processed or the
	 * function throws an exception.  Exceptions thrown by the function are
	 * relayed to the caller.
	 *
	 * @param function the function to apply to each entry
	 */
	public void replaceAll(LongObjToObjBiFunction<? super V, ? extends V> function) {
		for (Entry<V> entry : entrySet()) {
			entry.setValue(function.apply(entry.getKey(), entry.getValue()));
		}
	}

	/**
	 * Reduces the size of the map to the specified size. If the map is already smaller than the specified
	 * size, no action is taken. This indiscriminately removes items from the backing array until the
	 * requested newSize is reached, or until the full backing array has had its elements removed.
	 * <br>
	 * This tries to remove from the end of the iteration order, but because the iteration order is not
	 * guaranteed by an unordered map, this can remove essentially any item(s) from the map if it is larger
	 * than newSize.
	 *
	 * @param newSize the target size to try to reach by removing items, if smaller than the current size
	 */
	public void truncate(int newSize) {
		long[] keyTable = this.keyTable;
		V[] valTable = this.valueTable;
		newSize = Math.max(0, newSize);
		for (int i = keyTable.length - 1; i >= 0 && size > newSize; i--) {
			if (keyTable[i] != 0) {
				keyTable[i] = 0;
				valTable[i] = null;
				--size;
			}
		}
		if (hasZeroValue && size > newSize) {
			hasZeroValue = false;
			zeroValue = null;
			--size;
		}
	}

	/**
	 * Reuses the iterator of the reused {@link Entries} produced by {@link #entrySet()};
	 * does not permit nested iteration. Iterate over {@link Entries#Entries(LongObjectMap)} if you
	 * need nested or multithreaded iteration. You can remove an Entry from this LongObjectMap
	 * using this Iterator.
	 *
	 * @return an {@link Iterator} over {@link Entry} key-value pairs; remove is supported.
	 */
	@Override
	public @NotNull EntryIterator<V> iterator() {
		return entrySet().iterator();
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
	 * method is called. Use the {@link Keys} constructor for nested or
	 * multithreaded iteration.
	 *
	 * @return a set view of the keys contained in this map
	 */
	public Keys<V> keySet() {
		if (keys1 == null || keys2 == null) {
			keys1 = new Keys<V>(this);
			keys2 = new Keys<V>(this);
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
	 * @return a {@link Collection} containing V values
	 */
	public Values<V> values() {
		if (values1 == null || values2 == null) {
			values1 = new Values<V>(this);
			values2 = new Values<V>(this);
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
	 * Returns a Set of Entry, containing the entries in the map. Remove is supported by the Set's iterator.
	 * Note that the same iterator instance is returned each time this method is called.
	 * Use the {@link Entries} constructor for nested or multithreaded iteration.
	 *
	 * @return a {@link Set} of {@link Entry} key-value pairs
	 */
	public Entries<V> entrySet() {
		if (entries1 == null || entries2 == null) {
			entries1 = new Entries<V>(this);
			entries2 = new Entries<V>(this);
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

	public static class Entry<V> {
		public long key;
		@Nullable
		public V value;

		public Entry() {
		}

		public Entry(long key, @Nullable V value) {
			this.key = key;
			this.value = value;
		}

		public Entry(Entry<V> entry) {
			this.key = entry.key;
			this.value = entry.value;
		}

		@Override
		public String toString() {
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
		public long getKey() {
			return key;
		}

		/**
		 * Returns the value corresponding to this entry.  If the mapping
		 * has been removed from the backing map (by the iterator's
		 * {@code remove} operation), the results of this call are undefined.
		 *
		 * @return the value corresponding to this entry
		 */
		@Nullable
		public V getValue() {
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
		@Nullable
		public V setValue(@Nullable V value) {
			V old = this.value;
			this.value = value;
			return old;
		}

		@Override
		public boolean equals(@Nullable Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}

			Entry entry = (Entry) o;

			if (key != entry.key) {
				return false;
			}
			return Objects.equals(value, entry.value);
		}

		@Override
		public int hashCode() {
			return (int) (key ^ key >>> 32) ^ (value == null ? 0 : value.hashCode());
		}
	}

	public static abstract class MapIterator<V> {
		static protected final int INDEX_ILLEGAL = -2, INDEX_ZERO = -1;

		public boolean hasNext;

		protected final LongObjectMap<V> map;
		protected int nextIndex, currentIndex;
		protected boolean valid = true;

		public MapIterator(LongObjectMap<V> map) {
			this.map = map;
			reset();
		}

		public void reset() {
			currentIndex = INDEX_ILLEGAL;
			nextIndex = INDEX_ZERO;
			if (map.hasZeroValue) {
				hasNext = true;
			} else {
				findNextIndex();
			}
		}

		void findNextIndex() {
			long[] keyTable = map.keyTable;
			for (int n = keyTable.length; ++nextIndex < n; ) {
				if (keyTable[nextIndex] != 0) {
					hasNext = true;
					return;
				}
			}
			hasNext = false;
		}

		/**
		 * Returns {@code true} if the iteration has more elements.
		 * (In other words, returns {@code true} if next() would
		 * return an element rather than throwing an exception.)
		 *
		 * @return {@code true} if the iteration has more elements
		 */
		public boolean hasNext() {
			return hasNext;
		}

		public void remove() {
			int i = currentIndex;
			if (i == INDEX_ZERO && map.hasZeroValue) {
				map.hasZeroValue = false;
				map.zeroValue = null;
			} else if (i < 0) {
				throw new IllegalStateException("next must be called before remove.");
			} else {
				long[] keyTable = map.keyTable;
				V[] valueTable = map.valueTable;
				int mask = map.mask, next = i + 1 & mask;
				long key;
				while ((key = keyTable[next]) != 0) {
					int placement = map.place(key);
					if ((next - placement & mask) > (i - placement & mask)) {
						keyTable[i] = key;
						valueTable[i] = valueTable[next];
						i = next;
					}
					next = next + 1 & mask;
				}
				keyTable[i] = 0;
				valueTable[i] = null;
				if (i != currentIndex)
					--nextIndex;
			}
			currentIndex = INDEX_ILLEGAL;
			map.size--;
		}
	}

	public static class KeyIterator<V> extends MapIterator<V> implements LongIterator {
		public KeyIterator(LongObjectMap<V> map) {
			super(map);
		}

		@Override
		public long nextLong() {
			if (!hasNext) {
				throw new NoSuchElementException();
			}
			if (!valid) {
				throw new RuntimeException("#iterator() cannot be used nested.");
			}
			long key = nextIndex == INDEX_ZERO ? 0 : map.keyTable[nextIndex];
			currentIndex = nextIndex;
			findNextIndex();
			return key;
		}

		/**
		 * Returns a new LongList containing the remaining keys.
		 */
		public LongList toList() {
			LongList list = new LongList(map.size);
			while (hasNext) {
				list.add(nextLong());
			}
			return list;
		}

		@Override
		public boolean hasNext() {
			if (!valid) {
				throw new RuntimeException("#iterator() cannot be used nested.");
			}
			return hasNext;
		}
	}

	public static class ValueIterator<V> extends MapIterator<V> implements Iterator<V> {
		public ValueIterator(LongObjectMap<V> map) {
			super(map);
		}

		/**
		 * Returns the next {@code V} element in the iteration.
		 *
		 * @return the next {@code V} element in the iteration
		 * @throws NoSuchElementException if the iteration has no more elements
		 */
		@Override
		@Nullable
		public V next() {
			if (!hasNext) {
				throw new NoSuchElementException();
			}
			if (!valid) {
				throw new RuntimeException("#iterator() cannot be used nested.");
			}
			V value = nextIndex == INDEX_ZERO ? map.zeroValue : map.valueTable[nextIndex];
			currentIndex = nextIndex;
			findNextIndex();
			return value;
		}

		@Override
		public boolean hasNext() {
			if (!valid) {
				throw new RuntimeException("#iterator() cannot be used nested.");
			}
			return hasNext;
		}
	}

	public static class EntryIterator<V> extends MapIterator<V> implements Iterable<Entry<V>>, Iterator<Entry<V>> {
		protected Entry<V> entry = new Entry<>();

		public EntryIterator(LongObjectMap<V> map) {
			super(map);
		}

		@Override
		public @NotNull EntryIterator<V> iterator() {
			return this;
		}

		/**
		 * Note the same entry instance is returned each time this method is called.
		 */
		@Override
		public Entry<V> next() {
			if (!hasNext) {
				throw new NoSuchElementException();
			}
			if (!valid) {
				throw new RuntimeException("#iterator() cannot be used nested.");
			}
			long[] keyTable = map.keyTable;
			if (nextIndex == INDEX_ZERO) {
				entry.key = 0;
				entry.value = map.zeroValue;
			} else {
				entry.key = keyTable[nextIndex];
				entry.value = map.valueTable[nextIndex];
			}
			currentIndex = nextIndex;
			findNextIndex();
			return entry;
		}

		@Override
		public boolean hasNext() {
			if (!valid) {
				throw new RuntimeException("#iterator() cannot be used nested.");
			}
			return hasNext;
		}
	}

	public static class Entries<V> extends AbstractSet<Entry<V>> implements EnhancedCollection<Entry<V>> {
		protected EntryIterator<V> iter;

		public Entries(LongObjectMap<V> map) {
			iter = new EntryIterator<>(map);
		}

		/**
		 * Returns an iterator over the elements contained in this collection.
		 *
		 * @return an iterator over the elements contained in this collection
		 */
		@Override
		public @NotNull EntryIterator<V> iterator() {
			return iter;
		}

		@Override
		public int size() {
			return iter.map.size;
		}

		@Override
		public int hashCode() {
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			iter.reset();
			int hc = super.hashCode();
			iter.currentIndex = currentIdx;
			iter.nextIndex = nextIdx;
			iter.hasNext = hn;
			return hc;
		}

		@Override
		public String toString() {
			return toString(", ", true);
		}

		/**
		 * The iterator is reused by this data structure, and you can reset it
		 * back to the start of the iteration order using this.
		 */
		public void resetIterator() {
			iter.reset();
		}

		/**
		 * Returns a new {@link ObjectList} containing the remaining items.
		 * Does not change the position of this iterator.
		 */
		public ObjectList<Entry<V>> toList() {
			ObjectList<Entry<V>> list = new ObjectList<>(iter.map.size);
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			while (iter.hasNext) {
				list.add(new Entry<>(iter.next()));
			}
			iter.currentIndex = currentIdx;
			iter.nextIndex = nextIdx;
			iter.hasNext = hn;
			return list;
		}

		/**
		 * Append the remaining items that this can iterate through into the given Collection.
		 * Does not change the position of this iterator.
		 *
		 * @param coll any modifiable Collection; may have items appended into it
		 * @return the given collection
		 */
		public Collection<Entry<V>> appendInto(Collection<Entry<V>> coll) {
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			while (iter.hasNext) {
				coll.add(new Entry<>(iter.next()));
			}
			iter.currentIndex = currentIdx;
			iter.nextIndex = nextIdx;
			iter.hasNext = hn;
			return coll;
		}

		/**
		 * Append the remaining items that this can iterate through into the given Map.
		 * Does not change the position of this iterator. Note that a Map is not a Collection.
		 *
		 * @param coll any modifiable Map; may have items appended into it
		 * @return the given map
		 */
		public LongObjectMap<V> appendInto(LongObjectMap<V> coll) {
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			while (iter.hasNext) {
				iter.next();
				coll.put(iter.entry.key, iter.entry.value);
			}
			iter.currentIndex = currentIdx;
			iter.nextIndex = nextIdx;
			iter.hasNext = hn;
			return coll;
		}
	}

	public static class Values<V> extends AbstractCollection<V> implements EnhancedCollection<V> {
		protected ValueIterator<V> iter;

		@Override
		public boolean add(@Nullable V item) {
			throw new UnsupportedOperationException("LongObjectMap.Values is read-only");
		}

		@Override
		public boolean remove(@Nullable Object item) {
			throw new UnsupportedOperationException("LongObjectMap.Values is read-only");
		}

		@Override
		public boolean contains(@Nullable Object item) {
			return iter.map.containsValue(item);
		}

		@Override
		public void clear() {
			throw new UnsupportedOperationException("LongObjectMap.Values is read-only");
		}

		/**
		 * Returns an iterator over the elements contained in this collection.
		 *
		 * @return an iterator over the elements contained in this collection
		 */
		@Override
		public @NotNull ValueIterator<V> iterator() {
			return iter;
		}

		@Override
		public int size() {
			return iter.map.size;
		}

		@Override
		public String toString() {
			return toString(", ", true);
		}

		public Values(LongObjectMap<V> map) {
			iter = new ValueIterator<>(map);
		}

		/**
		 * The iterator is reused by this data structure, and you can reset it
		 * back to the start of the iteration order using this.
		 */
		public void resetIterator() {
			iter.reset();
		}

		/**
		 * Returns a new {@link ObjectList} containing the remaining items.
		 * Does not change the position of this iterator.
		 */
		public ObjectList<V> toList() {
			ObjectList<V> list = new ObjectList<>(iter.map.size);
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			while (iter.hasNext) {
				list.add(iter.next());
			}
			iter.currentIndex = currentIdx;
			iter.nextIndex = nextIdx;
			iter.hasNext = hn;
			return list;
		}

		/**
		 * Append the remaining items that this can iterate through into the given Collection.
		 * Does not change the position of this iterator.
		 *
		 * @param coll any modifiable Collection; may have items appended into it
		 * @return the given collection
		 */
		public Collection<V> appendInto(Collection<V> coll) {
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			while (iter.hasNext) {
				coll.add(iter.next());
			}
			iter.currentIndex = currentIdx;
			iter.nextIndex = nextIdx;
			iter.hasNext = hn;
			return coll;
		}
	}

	public static class Keys<V> implements PrimitiveSet.SetOfLong {
		protected KeyIterator<V> iter;

		public Keys(LongObjectMap<V> map) {
			iter = new KeyIterator<>(map);
		}

		@Override
		public boolean add(long item) {
			throw new UnsupportedOperationException("LongObjectMap.Keys is read-only");
		}

		@Override
		public boolean remove(long item) {
			throw new UnsupportedOperationException("LongObjectMap.Keys is read-only");
		}

		@Override
		public boolean contains(long item) {
			return iter.map.containsKey(item);
		}

		@Override
		public LongIterator iterator() {
			return iter;
		}

		@Override
		public void clear() {
			throw new UnsupportedOperationException("LongObjectMap.Keys is read-only");
		}

		@Override
		public int size() {
			return iter.map.size;
		}

		@Override
		public int hashCode() {
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			iter.reset();
			long hc = 1;
			while (iter.hasNext) {
				hc += iter.nextLong();
			}
			iter.currentIndex = currentIdx;
			iter.nextIndex = nextIdx;
			iter.hasNext = hn;
			return (int) (hc ^ hc >>> 32);
		}

		/**
		 * The iterator is reused by this data structure, and you can reset it
		 * back to the start of the iteration order using this.
		 */
		public void resetIterator() {
			iter.reset();
		}

		/**
		 * Returns a new {@link ObjectList} containing the remaining items.
		 * Does not change the position of this iterator.
		 */
		public LongList toList() {
			LongList list = new LongList(iter.map.size);
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			while (iter.hasNext) {
				list.add(iter.nextLong());
			}
			iter.currentIndex = currentIdx;
			iter.nextIndex = nextIdx;
			iter.hasNext = hn;
			return list;
		}

		/**
		 * Append the remaining items that this can iterate through into the given Collection.
		 * Does not change the position of this iterator.
		 *
		 * @param coll any modifiable Collection; may have items appended into it
		 * @return the given collection
		 */
		public PrimitiveCollection.OfLong appendInto(PrimitiveCollection.OfLong coll) {
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			while (iter.hasNext) {
				coll.add(iter.nextLong());
			}
			iter.currentIndex = currentIdx;
			iter.nextIndex = nextIdx;
			iter.hasNext = hn;
			return coll;
		}

		@SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
		@Override
		public boolean equals(Object other) {
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			boolean eq = SetOfLong.super.equalContents(other);
			iter.currentIndex = currentIdx;
			iter.nextIndex = nextIdx;
			iter.hasNext = hn;
			return eq;
		}

		@Override
		public String toString() {
			return toString(", ", true);
		}
	}

	@Nullable
	public V putIfAbsent(long key, V value) {
		if (key == 0) {
			if (hasZeroValue) {
				return zeroValue;
			}
			return put(key, value);
		}
		int i = locateKey(key);
		if (i >= 0) {
			return valueTable[i];
		}
		return put(key, value);
	}

	public boolean replace(long key, V oldValue, V newValue) {
		V curValue = get(key);
		if (!Objects.equals(curValue, oldValue) || !containsKey(key)) {
			return false;
		}
		put(key, newValue);
		return true;
	}

	@Nullable
	public V replace(long key, V value) {
		if (key == 0) {
			if (hasZeroValue) {
				V oldValue = zeroValue;
				zeroValue = value;
				return oldValue;
			}
			return defaultValue;
		}
		int i = locateKey(key);
		if (i >= 0) {
			V oldValue = valueTable[i];
			valueTable[i] = value;
			return oldValue;
		}
		return defaultValue;
	}

	@Nullable
	public V computeIfAbsent(long key, LongToObjFunction<? extends V> mappingFunction) {
		int i = locateKey(key);
		if (i < 0) {
			V newValue = mappingFunction.apply(key);
			put(key, newValue);
			return newValue;
		} else
			return valueTable[i];
	}

	public boolean remove(long key, Object value) {
		int i = locateKey(key);
		if (i >= 0 && Objects.equals(valueTable[i], value)) {
			remove(key);
			return true;
		}
		return false;
	}

	@Nullable
	public V merge(long key, V value, ObjObjToObjBiFunction<? super V, ? super V, ? extends V> remappingFunction) {
		int i = locateKey(key);
		V next = (i < 0) ? value : remappingFunction.apply(valueTable[i], value);
		if (next == null)
			remove(key);
		else
			put(key, next);
		return next;
	}

	/**
	 * Just like Map's merge() default method, but this doesn't use Java 8 APIs (so it should work on RoboVM), and this
	 * won't remove entries if the remappingFunction returns null (in that case, it will call {@code put(key, null)}).
	 * This also uses a functional interface from Funderby instead of the JDK, for RoboVM support.
	 *
	 * @param key               key with which the resulting value is to be associated
	 * @param value             the value to be merged with the existing value
	 *                          associated with the key or, if no existing value
	 *                          is associated with the key, to be associated with the key
	 * @param remappingFunction given a V from this and the V {@code value}, this should return what V to use
	 * @return the value now associated with key
	 */
	@Nullable
	public V combine(long key, V value, ObjObjToObjBiFunction<? super V, ? super V, ? extends V> remappingFunction) {
		int i = locateKey(key);
		V next = (i < 0) ? value : remappingFunction.apply(valueTable[i], value);
		put(key, next);
		return next;
	}

	/**
	 * Simply calls {@link #combine(long, Object, ObjObjToObjBiFunction)} on this map using every
	 * key-value pair in {@code other}. If {@code other} isn't empty, calling this will probably modify
	 * this map, though this depends on the {@code remappingFunction}.
	 *
	 * @param other             a non-null Map (or subclass) with compatible key and value types
	 * @param remappingFunction given a V value from this and a value from other, this should return what V to use
	 */
	public void combine(LongObjectMap<? extends V> other, ObjObjToObjBiFunction<? super V, ? super V, ? extends V> remappingFunction) {
		for (LongObjectMap.Entry<? extends V> e : other.entrySet()) {
			combine(e.getKey(), e.getValue(), remappingFunction);
		}
	}

	/**
	 * Constructs an empty map given the key type as a generic type argument.
	 * This is usually less useful than just using the constructor, but can be handy
	 * in some code-generation scenarios when you don't know how many arguments you will have.
	 *
	 * @param <V> the type of values
	 * @return a new map containing nothing
	 */
	public static <V> LongObjectMap<V> with() {
		return new LongObjectMap<>(0);
	}

	/**
	 * Constructs a single-entry map given one key and one value.
	 * This is mostly useful as an optimization for {@link #with(Number, Object, Object...)}
	 * when there's no "rest" of the keys or values. Like the more-argument with(), this will
	 * convert its V value to a primitive float, regardless of which Number type was used.
	 *
	 * @param key0   the first and only key; will be converted to primitive long
	 * @param value0 the first and only value
	 * @param <V>    the type of value0
	 * @return a new map containing just the entry mapping key0 to value0
	 */
	public static <V> LongObjectMap<V> with(Number key0, V value0) {
		LongObjectMap<V> map = new LongObjectMap<>(1);
		map.put(key0.longValue(), value0);
		return map;
	}

	/**
	 * Constructs a map given alternating keys and values.
	 * This is mostly useful as an optimization for {@link #with(Number, Object, Object...)}
	 * when there's no "rest" of the keys or values. Like the more-argument with(), this will
	 * convert its V values to primitive floats, regardless of which Number type was used.
	 *
	 * @param key0   a Number key; will be converted to primitive long
	 * @param value0 a V value
	 * @param key1   a Number key; will be converted to primitive long
	 * @param value1 a V value
	 * @param <V>    the type of values
	 * @return a new map containing the given key-value pairs
	 */
	public static <V> LongObjectMap<V> with(Number key0, V value0, Number key1, V value1) {
		LongObjectMap<V> map = new LongObjectMap<>(2);
		map.put(key0.longValue(), value0);
		map.put(key1.longValue(), value1);
		return map;
	}

	/**
	 * Constructs a map given alternating keys and values.
	 * This is mostly useful as an optimization for {@link #with(Number, Object, Object...)}
	 * when there's no "rest" of the keys or values. Like the more-argument with(), this will
	 * convert its V values to primitive floats, regardless of which Number type was used.
	 *
	 * @param key0   a Number key; will be converted to primitive long
	 * @param value0 a V value
	 * @param key1   a Number key; will be converted to primitive long
	 * @param value1 a V value
	 * @param key2   a Number key; will be converted to primitive long
	 * @param value2 a V value
	 * @param <V>    the type of values
	 * @return a new map containing the given key-value pairs
	 */
	public static <V> LongObjectMap<V> with(Number key0, V value0, Number key1, V value1, Number key2, V value2) {
		LongObjectMap<V> map = new LongObjectMap<>(3);
		map.put(key0.longValue(), value0);
		map.put(key1.longValue(), value1);
		map.put(key2.longValue(), value2);
		return map;
	}

	/**
	 * Constructs a map given alternating keys and values.
	 * This is mostly useful as an optimization for {@link #with(Number, Object, Object...)}
	 * when there's no "rest" of the keys or values. Like the more-argument with(), this will
	 * convert its V values to primitive floats, regardless of which Number type was used.
	 *
	 * @param key0   a Number key; will be converted to primitive long
	 * @param value0 a V value
	 * @param key1   a Number key; will be converted to primitive long
	 * @param value1 a V value
	 * @param key2   a Number key; will be converted to primitive long
	 * @param value2 a V value
	 * @param key3   a Number key; will be converted to primitive long
	 * @param value3 a V value
	 * @param <V>    the type of values
	 * @return a new map containing the given key-value pairs
	 */
	public static <V> LongObjectMap<V> with(Number key0, V value0, Number key1, V value1, Number key2, V value2, Number key3, V value3) {
		LongObjectMap<V> map = new LongObjectMap<>(4);
		map.put(key0.longValue(), value0);
		map.put(key1.longValue(), value1);
		map.put(key2.longValue(), value2);
		map.put(key3.longValue(), value3);
		return map;
	}

	/**
	 * Constructs a map given alternating keys and values.
	 * This can be useful in some code-generation scenarios, or when you want to make a
	 * map conveniently by-hand and have it populated at the start. You can also use
	 * {@link #LongObjectMap(long[], Object[])}, which takes all keys and then all values.
	 * This needs all keys to have the same type, because it gets a generic type from the
	 * first key parameter. All keys must be some type of boxed Number, such as {@link Integer}
	 * or {@link Double}, and will be converted to primitive {@code long}s. Any values that don't
	 * have V as their type or keys that aren't {@code Number}s have that entry skipped.
	 *
	 * @param key0   the first key; will be converted to primitive long
	 * @param value0 the first value; will be used to determine the type of all values
	 * @param rest   a varargs or non-null array of alternating Number, V, Number, V... elements
	 * @param <V>    the type of values, inferred from value0
	 * @return a new map containing the given keys and values
	 */
	public static <V> LongObjectMap<V> with(Number key0, V value0, Object... rest) {
		LongObjectMap<V> map = new LongObjectMap<>(1 + (rest.length >>> 1));
		map.put(key0.longValue(), value0);
		map.putPairs(rest);
		return map;
	}

	/**
	 * Attempts to put alternating key-value pairs into this map, drawing a key, then a value from {@code pairs}, then
	 * another key, another value, and so on until another pair cannot be drawn.  All keys must be some type of boxed
	 * Number, such as {@link Integer} or {@link Double}, and will be converted to primitive {@code long}s. Any keys
	 * that aren't {@code Number}s or values that don't have V as their type have that entry skipped.
	 * <br>
	 * If any item in {@code pairs} cannot be cast to the appropriate Number or V type for its position in the
	 * arguments, that pair is ignored and neither that key nor value is put into the map. If any key is null, that pair
	 * is ignored, as well. If {@code pairs} is an Object array that is null, the entire call to putPairs() is ignored.
	 * If the length of {@code pairs} is odd, the last item (which will be unpaired) is ignored.
	 *
	 * @param pairs an array or varargs of alternating Number, V, Number, V... elements
	 */
	@SuppressWarnings("unchecked")
	public void putPairs(Object... pairs) {
		if (pairs != null) {
			for (int i = 1; i < pairs.length; i += 2) {
				try {
					if (pairs[i - 1] != null)
						put(((Number) pairs[i - 1]).longValue(), (V) pairs[i]);
				} catch (ClassCastException ignored) {
				}
			}
		}
	}

	/**
	 * Adds items to this map drawn from the result of {@link #toString(String)} or
	 * {@link #appendTo(StringBuilder, String, boolean)}. Every key-value pair should be separated by
	 * {@code ", "}, and every key should be followed by {@code "="} before the value (which
	 * {@link #toString()} does).
	 * Each item can vary significantly in length, and should use
	 * {@link Base#BASE10} digits, which should be human-readable. Any brackets inside the given range
	 * of characters will ruin the parsing, so increase offset by 1 and
	 * reduce length by 2 if the original String had brackets added to it.
	 *
	 * @param str         a String containing BASE10 chars
	 * @param valueParser a PartialParser that returns a {@code V} value from a section of {@code str}
	 */
	public void putLegible(String str, PartialParser<V> valueParser) {
		putLegible(str, ", ", "=", valueParser, 0, -1);
	}

	/**
	 * Adds items to this map drawn from the result of {@link #toString(String)} or
	 * {@link #appendTo(StringBuilder, String, boolean)}. Every key-value pair should be separated by
	 * {@code entrySeparator}, and every key should be followed by "=" before the value (which
	 * {@link #toString(String)} does).
	 * Each item can vary significantly in length, and should use
	 * {@link Base#BASE10} digits, which should be human-readable. Any brackets inside the given range
	 * of characters will ruin the parsing, so increase offset by 1 and
	 * reduce length by 2 if the original String had brackets added to it.
	 *
	 * @param str            a String containing BASE10 chars
	 * @param entrySeparator the String separating every key-value pair
	 * @param valueParser    a PartialParser that returns a {@code V} value from a section of {@code str}
	 */
	public void putLegible(String str, String entrySeparator, PartialParser<V> valueParser) {
		putLegible(str, entrySeparator, "=", valueParser, 0, -1);
	}

	/**
	 * Adds items to this map drawn from the result of {@link #toString(String)} or
	 * {@link #appendTo(StringBuilder, String, String, boolean, LongAppender, Appender)}. Each key can vary
	 * significantly in length, and should use {@link Base#BASE10} digits, which should be human-readable. Any brackets
	 * inside the given range of characters will ruin the parsing, so increase offset by 1 and
	 * reduce length by 2 if the original String had brackets added to it.
	 *
	 * @param str               a String containing BASE10 chars
	 * @param entrySeparator    the String separating every key-value pair
	 * @param keyValueSeparator the String separating every key from its corresponding value
	 * @param valueParser       a PartialParser that returns a {@code V} value from a section of {@code str}
	 */
	public void putLegible(String str, String entrySeparator, String keyValueSeparator, PartialParser<V> valueParser) {
		putLegible(str, entrySeparator, keyValueSeparator, valueParser, 0, -1);
	}

	/**
	 * Puts key-value pairs into this map drawn from the result of {@link #toString(String)} or
	 * {@link #appendTo(StringBuilder, String, String, boolean, LongAppender, Appender)}. Each key can vary
	 * significantly in length, and should use {@link Base#BASE10} digits, which should be human-readable. Any brackets
	 * inside the given range of characters will ruin the parsing, so increase offset by 1 and
	 * reduce length by 2 if the original String had brackets added to it.
	 *
	 * @param str               a String containing BASE10 chars
	 * @param entrySeparator    the String separating every key-value pair
	 * @param keyValueSeparator the String separating every key from its corresponding value
	 * @param valueParser       a PartialParser that returns a {@code V} value from a section of {@code str}
	 * @param offset            the first position to read BASE10 chars from in {@code str}
	 * @param length            how many chars to read; -1 is treated as maximum length
	 */
	public void putLegible(String str, String entrySeparator, String keyValueSeparator, PartialParser<V> valueParser, int offset, int length) {
		int sl, el, kvl;
		if (str == null || entrySeparator == null || keyValueSeparator == null || valueParser == null
			|| (sl = str.length()) < 1 || (el = entrySeparator.length()) < 1 || (kvl = keyValueSeparator.length()) < 1
			|| offset < 0 || offset > sl - 1) return;
		final int lim = length < 0 ? sl : Math.min(offset + length, sl);
		int end = str.indexOf(keyValueSeparator, offset + 1);
		long k = 0;
		boolean incomplete = false;
		while (end != -1 && end + kvl < lim) {
			k = Base.BASE10.readLong(str, offset, end);
			offset = end + kvl;
			end = str.indexOf(entrySeparator, offset + 1);
			if (end != -1 && end + el < lim) {
				put(k, valueParser.parse(str, offset, end));
				offset = end + el;
				end = str.indexOf(keyValueSeparator, offset + 1);
			} else {
				incomplete = true;
			}
		}
		if (incomplete && offset < lim) {
			put(k, valueParser.parse(str, offset, lim));
		}
	}

	/**
	 * Constructs an empty map given the key type as a generic type argument.
	 * This is usually less useful than just using the constructor, but can be handy
	 * in some code-generation scenarios when you don't know how many arguments you will have.
	 *
	 * @param <V> the type of values
	 * @return a new map containing nothing
	 */
	public static <V> LongObjectMap<V> withPrimitive() {
		return new LongObjectMap<>(0);
	}

	/**
	 * Constructs a single-entry map given one key and one value.
	 * This is mostly useful as an optimization for {@link #with(Number, Object, Object...)}
	 * when there's no "rest" of the keys or values. Unlike with(), this takes unboxed long as
	 * its key type, and will not box it.
	 *
	 * @param key0   a long key
	 * @param value0 a V value
	 * @param <V>    the type of value0
	 * @return a new map containing just the entry mapping key0 to value0
	 */
	public static <V> LongObjectMap<V> withPrimitive(long key0, V value0) {
		LongObjectMap<V> map = new LongObjectMap<>(1);
		map.put(key0, value0);
		return map;
	}

	/**
	 * Constructs a map given alternating keys and values.
	 * This is mostly useful as an optimization for {@link #with(Number, Object, Object...)}
	 * when there's no "rest" of the keys or values. Unlike with(), this takes unboxed long as
	 * its key type, and will not box it.
	 *
	 * @param key0   a long key
	 * @param value0 a V value
	 * @param key1   a long key
	 * @param value1 a V value
	 * @param <V>    the type of values
	 * @return a new map containing the given key-value pairs
	 */
	public static <V> LongObjectMap<V> withPrimitive(long key0, V value0, long key1, V value1) {
		LongObjectMap<V> map = new LongObjectMap<>(2);
		map.put(key0, value0);
		map.put(key1, value1);
		return map;
	}

	/**
	 * Constructs a map given alternating keys and values.
	 * This is mostly useful as an optimization for {@link #with(Number, Object, Object...)}
	 * when there's no "rest" of the keys or values. Unlike with(), this takes unboxed long as
	 * its key type, and will not box it.
	 *
	 * @param key0   a long key
	 * @param value0 a V value
	 * @param key1   a long key
	 * @param value1 a V value
	 * @param key2   a long key
	 * @param value2 a V value
	 * @param <V>    the type of values
	 * @return a new map containing the given key-value pairs
	 */
	public static <V> LongObjectMap<V> withPrimitive(long key0, V value0, long key1, V value1, long key2, V value2) {
		LongObjectMap<V> map = new LongObjectMap<>(3);
		map.put(key0, value0);
		map.put(key1, value1);
		map.put(key2, value2);
		return map;
	}

	/**
	 * Constructs a map given alternating keys and values.
	 * This is mostly useful as an optimization for {@link #with(Number, Object, Object...)}
	 * when there's no "rest" of the keys or values. Unlike with(), this takes unboxed long as
	 * its key type, and will not box it.
	 *
	 * @param key0   a long key
	 * @param value0 a V value
	 * @param key1   a long key
	 * @param value1 a V value
	 * @param key2   a long key
	 * @param value2 a V value
	 * @param key3   a long key
	 * @param value3 a V value
	 * @param <V>    the type of values
	 * @return a new map containing the given key-value pairs
	 */
	public static <V> LongObjectMap<V> withPrimitive(long key0, V value0, long key1, V value1, long key2, V value2, long key3, V value3) {
		LongObjectMap<V> map = new LongObjectMap<>(4);
		map.put(key0, value0);
		map.put(key1, value1);
		map.put(key2, value2);
		map.put(key3, value3);
		return map;
	}

	/**
	 * Creates a new map by parsing all of {@code str} with the given PartialParser for values,
	 * with entries separated by {@code entrySeparator}, such as {@code ", "} and
	 * the keys separated from values by {@code keyValueSeparator}, such as {@code "="}.
	 * <br>
	 * Various {@link PartialParser} instances are defined as constants, such as
	 * {@link PartialParser#DEFAULT_STRING}, and others can be created by static methods in PartialParser, such as
	 * {@link PartialParser#objectListParser(PartialParser, String, boolean)}.
	 *
	 * @param str               a String containing parseable text
	 * @param entrySeparator    the String separating every key-value pair
	 * @param keyValueSeparator the String separating every key from its corresponding value
	 * @param valueParser       a PartialParser that returns a {@code V} value from a section of {@code str}
	 */
	public static <V> LongObjectMap<V> parse(String str,
													String entrySeparator,
													String keyValueSeparator,
													PartialParser<V> valueParser) {
		return parse(str, entrySeparator, keyValueSeparator, valueParser, false);
	}
	/**
	 * Creates a new map by parsing all of {@code str} (or if {@code brackets} is true, all but the first and last
	 * chars) with the given PartialParser for values, with entries separated by {@code entrySeparator},
	 * such as {@code ", "} and the keys separated from values by {@code keyValueSeparator}, such as {@code "="}.
	 * <br>
	 * Various {@link PartialParser} instances are defined as constants, such as
	 * {@link PartialParser#DEFAULT_STRING}, and others can be created by static methods in PartialParser, such as
	 * {@link PartialParser#objectListParser(PartialParser, String, boolean)}.
	 *
	 * @param str               a String containing parseable text
	 * @param entrySeparator    the String separating every key-value pair
	 * @param keyValueSeparator the String separating every key from its corresponding value
	 * @param valueParser       a PartialParser that returns a {@code V} value from a section of {@code str}
	 * @param brackets          if true, the first and last chars in {@code str} will be ignored
	 */
	public static <V> LongObjectMap<V> parse(String str,
													String entrySeparator,
													String keyValueSeparator,
													PartialParser<V> valueParser,
													boolean brackets) {
		LongObjectMap<V> m = new LongObjectMap<>();
		if(brackets)
			m.putLegible(str, entrySeparator, keyValueSeparator, valueParser, 1, str.length() - 1);
		else
			m.putLegible(str, entrySeparator, keyValueSeparator, valueParser, 0, -1);
		return m;
	}

	/**
	 * Creates a new map by parsing the given subrange of {@code str} with the given PartialParser for values,
	 * with entries separated by {@code entrySeparator}, such as {@code ", "} and the keys separated from values
	 * by {@code keyValueSeparator}, such as {@code "="}.
	 * <br>
	 * Various {@link PartialParser} instances are defined as constants, such as
	 * {@link PartialParser#DEFAULT_STRING}, and others can be created by static methods in PartialParser, such as
	 * {@link PartialParser#objectListParser(PartialParser, String, boolean)}.
	 *
	 * @param str               a String containing parseable text
	 * @param entrySeparator    the String separating every key-value pair
	 * @param keyValueSeparator the String separating every key from its corresponding value
	 * @param valueParser       a PartialParser that returns a {@code V} value from a section of {@code str}
	 * @param offset            the first position to read parseable text from in {@code str}
	 * @param length            how many chars to read; -1 is treated as maximum length
	 */
	public static <V> LongObjectMap<V> parse(String str,
													String entrySeparator,
													String keyValueSeparator,
													PartialParser<V> valueParser,
													int offset,
													int length) {
		LongObjectMap<V> m = new LongObjectMap<>();
		m.putLegible(str, entrySeparator, keyValueSeparator, valueParser, offset, length);
		return m;
	}
}

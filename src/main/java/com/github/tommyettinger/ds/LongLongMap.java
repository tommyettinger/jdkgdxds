/*
 * Copyright (c) 2022-2023 See AUTHORS file.
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

import com.github.tommyettinger.digital.BitConversion;
import com.github.tommyettinger.ds.support.util.LongIterator;
import com.github.tommyettinger.function.LongLongBiConsumer;

import org.checkerframework.checker.nullness.qual.Nullable;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import com.github.tommyettinger.function.LongLongToLongBiFunction;
import com.github.tommyettinger.function.LongToLongFunction;

import static com.github.tommyettinger.ds.Utilities.tableSize;

/**
 * An unordered map where the keys are unboxed longs and the values are also unboxed longs. Null keys are not allowed. No allocation is
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
public class LongLongMap implements Iterable<LongLongMap.Entry> {

	protected int size;

	protected long[] keyTable;
	protected long[] valueTable;
	protected boolean hasZeroValue;
	protected long zeroValue;
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
	 * Used by {@link #place(long)} to bit shift the upper bits of a {@code long} into a usable range (&gt;= 0 and &lt;=
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
	 * Used by {@link #place(long)} to mix hashCode() results. Changes on every call to {@link #resize(int)} by default.
	 * This only needs to be serialized if the full key and value tables are serialized, or if the iteration order should be
	 * the same before and after serialization. Iteration order is better handled by using {@link LongLongOrderedMap}.
	 */
	protected long hashMultiplier = 0xD1B54A32D192ED03L;

	/**
	 * A bitmask used to confine hashcodes to the size of the tables. Must be all 1 bits in its low positions, ie a power of two
	 * minus 1. If {@link #place(long)} is overridden, this can be used instead of {@link #shift} to isolate usable bits of a
	 * hash.
	 */
	protected int mask;

	@Nullable protected transient Entries entries1;
	@Nullable protected transient Entries entries2;
	@Nullable protected transient Values values1;
	@Nullable protected transient Values values2;
	@Nullable protected transient Keys keys1;
	@Nullable protected transient Keys keys2;

	public long defaultValue = 0;

	/**
	 * Creates a new map with an initial capacity of 51 and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 */
	public LongLongMap () {
		this(51, Utilities.getDefaultLoadFactor());
	}

	/**
	 * Creates a new map with the given starting capacity and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 */
	public LongLongMap (int initialCapacity) {
		this(initialCapacity, Utilities.getDefaultLoadFactor());
	}

	/**
	 * Creates a new map with the specified initial capacity and load factor. This map will hold initialCapacity items before
	 * growing the backing table.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 * @param loadFactor      what fraction of the capacity can be filled before this has to resize; 0 &lt; loadFactor &lt;= 1
	 */
	public LongLongMap (int initialCapacity, float loadFactor) {
		if (loadFactor <= 0f || loadFactor > 1f) {throw new IllegalArgumentException("loadFactor must be > 0 and <= 1: " + loadFactor);}
		this.loadFactor = loadFactor;

		int tableSize = tableSize(initialCapacity, loadFactor);
		threshold = (int)(tableSize * loadFactor);
		mask = tableSize - 1;
		shift = BitConversion.countLeadingZeros((long)mask);

		keyTable = new long[tableSize];
		valueTable = new long[tableSize];
	}

	/**
	 * Creates a new map identical to the specified map.
	 *
	 * @param map the map to copy
	 */
	public LongLongMap (LongLongMap map) {
		this((int)(map.keyTable.length * map.loadFactor), map.loadFactor);
		System.arraycopy(map.keyTable, 0, keyTable, 0, map.keyTable.length);
		System.arraycopy(map.valueTable, 0, valueTable, 0, map.valueTable.length);
		size = map.size;
		defaultValue = map.defaultValue;
		zeroValue = map.zeroValue;
		hasZeroValue = map.hasZeroValue;
		hashMultiplier = map.hashMultiplier;
	}

	/**
	 * Given two side-by-side arrays, one of keys, one of values, this constructs a map and inserts each pair of key and value into it.
	 * If keys and values have different lengths, this only uses the length of the smaller array.
	 *
	 * @param keys   an array of keys
	 * @param values an array of values
	 */
	public LongLongMap (long[] keys, long[] values) {
		this(Math.min(keys.length, values.length));
		putAll(keys, values);
	}

	/**
	 * Given two side-by-side collections, one of keys, one of values, this constructs a map and inserts each pair of key and value into it.
	 * If keys and values have different lengths, this only uses the length of the smaller collection.
	 *
	 * @param keys   a PrimitiveCollection of keys
	 * @param values a PrimitiveCollection of values
	 */
	public LongLongMap (PrimitiveCollection.OfLong keys, PrimitiveCollection.OfLong values) {
		this(Math.min(keys.size(), values.size()));
		putAll(keys, values);
	}

	/**
	 * Given two side-by-side collections, one of keys, one of values, this inserts each pair of key and value into this map with put().
	 *
	 * @param keys   a PrimitiveCollection of keys
	 * @param values a PrimitiveCollection of values
	 */
	public void putAll (PrimitiveCollection.OfLong keys, PrimitiveCollection.OfLong values) {
		int length = Math.min(keys.size(), values.size());
		ensureCapacity(length);
		LongIterator ki = keys.iterator();
		LongIterator vi = values.iterator();
		while (ki.hasNext() && vi.hasNext()) {
			put(ki.nextLong(), vi.nextLong());
		}
	}

	/**
	 * Returns an index &gt;= 0 and &lt;= {@link #mask} for the specified {@code item}.
	 * Defaults to using {@link #hashMultiplier}, which changes every time the data structure resizes.
	 *
	 * @param item any long; it is usually mixed or masked here
	 * @return an index between 0 and {@link #mask} (both inclusive)
	 */
	protected int place (long item) {
		return (int)((item ^ item >>> 32) * hashMultiplier >>> shift);
	}

	/**
	 * Returns the index of the key if already present, else {@code ~index} for the next empty index.
	 * While this can be overridden to compare for equality differently than {@code ==} between ints, that
	 * isn't recommended because this has to treat zero keys differently, and it finds those with {@code ==}.
	 * If you want to treat equality between longs differently for some reason, you would also need to override
	 * {@link #containsKey(long)} and {@link #get(long)}, at the very least.
	 */
	protected int locateKey (long key) {
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
	public long put (long key, long value) {
		if (key == 0) {
			long oldValue = defaultValue;
			if (hasZeroValue) {oldValue = zeroValue;} else {size++;}
			hasZeroValue = true;
			zeroValue = value;
			return oldValue;
		}
		int i = locateKey(key);
		if (i >= 0) { // Existing key was found.
			long oldValue = valueTable[i];
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
	 * Returns the old value associated with the specified key, or the given {@code defaultValue} if there was no prior value.
	 */
	public long putOrDefault (long key, long value, long defaultValue) {
		if (key == 0) {
			long oldValue = defaultValue;
			if (hasZeroValue) {oldValue = zeroValue;} else {size++;}
			hasZeroValue = true;
			zeroValue = value;
			return oldValue;
		}
		int i = locateKey(key);
		if (i >= 0) { // Existing key was found.
			long oldValue = valueTable[i];
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
	public void putAll (LongLongMap map) {
		ensureCapacity(map.size);
		if (map.hasZeroValue) {
			if (!hasZeroValue) {size++;}
			hasZeroValue = true;
			zeroValue = map.zeroValue;
		}
		long[] keyTable = map.keyTable;
		long[] valueTable = map.valueTable;
		long key;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			key = keyTable[i];
			if (key != 0) {put(key, valueTable[i]);}
		}
	}

	/**
	 * Given two side-by-side arrays, one of keys, one of values, this inserts each pair of key and value into this map with put().
	 *
	 * @param keys   an array of keys
	 * @param values an array of values
	 */
	public void putAll (long[] keys, long[] values) {
		putAll(keys, 0, values, 0, Math.min(keys.length, values.length));
	}

	/**
	 * Given two side-by-side arrays, one of keys, one of values, this inserts each pair of key and value into this map with put().
	 *
	 * @param keys   an array of keys
	 * @param values an array of values
	 * @param length how many items from keys and values to insert, at-most
	 */
	public void putAll (long[] keys, long[] values, int length) {
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
	public void putAll (long[] keys, int keyOffset, long[] values, int valueOffset, int length) {
		length = Math.min(length, Math.min(keys.length - keyOffset, values.length - valueOffset));
		ensureCapacity(length);
		for (int k = keyOffset, v = valueOffset, i = 0, n = length; i < n; i++, k++, v++) {
			put(keys[k], values[v]);
		}
	}

	/**
	 * Skips checks for existing keys, doesn't increment size.
	 */
	protected void putResize (long key, long value) {
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
	public long get (long key) {
		if (key == 0) {return hasZeroValue ? zeroValue : defaultValue;}
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
	public long getOrDefault (long key, long defaultValue) {
		if (key == 0) {return hasZeroValue ? zeroValue : defaultValue;}
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
	 * Returns the key's current value and increments the stored value. If the key is not in the map, defaultValue + increment is
	 * put into the map and defaultValue is returned.
	 */
	public long getAndIncrement (long key, long defaultValue, long increment) {
		if (key == 0) {
			if (hasZeroValue) {
				long old = zeroValue;
				zeroValue += increment;
				return old;
			}
			hasZeroValue = true;
			zeroValue = defaultValue + increment;
			size++;
			return defaultValue;
		}
		int i = locateKey(key);
		if (i >= 0) { // Existing key was found.
			long oldValue = valueTable[i];
			valueTable[i] += increment;
			return oldValue;
		}
		i = ~i; // Empty space was found.
		keyTable[i] = key;
		valueTable[i] = defaultValue + increment;
		if (++size >= threshold) {resize(keyTable.length << 1);}
		return defaultValue;
	}

	public long remove (long key) {
		if (key == 0) {
			if (hasZeroValue) {
				hasZeroValue = false;
				--size;
				return zeroValue;
			}
			return defaultValue;
		}
		int i = locateKey(key);
		if (i < 0) {return defaultValue;}
		long[] keyTable = this.keyTable;
		long rem;
		long[] valueTable = this.valueTable;
		long oldValue = valueTable[i];
		int mask = this.mask, next = i + 1 & mask;
		while ((rem = keyTable[next]) != 0) {
			int placement = place(rem);
			if ((next - placement & mask) > (i - placement & mask)) {
				keyTable[i] = rem;
				valueTable[i] = valueTable[next];
				i = next;
			}
			next = next + 1 & mask;
		}
		keyTable[i] = 0;

		size--;
		return oldValue;
	}

	/**
	 * Returns true if the map has one or more items.
	 */
	public boolean notEmpty () {
		return size != 0;
	}

	/**
	 * Returns the number of key-value mappings in this map.  If the
	 * map contains more than {@code Integer.MAX_VALUE} elements, returns
	 * {@code Integer.MAX_VALUE}.
	 *
	 * @return the number of key-value mappings in this map
	 */
	public int size () {
		return size;
	}

	/**
	 * Returns true if the map is empty.
	 */
	public boolean isEmpty () {
		return size == 0;
	}

	/**
	 * Gets the default value, a {@code long} which is returned by {@link #get(long)} if the key is not found.
	 * If not changed, the default value is 0.
	 *
	 * @return the current default value
	 */
	public long getDefaultValue () {
		return defaultValue;
	}

	/**
	 * Sets the default value, a {@code long} which is returned by {@link #get(long)} if the key is not found.
	 * If not changed, the default value is 0. Note that {@link #getOrDefault(long, long)} is also available,
	 * which allows specifying a "not-found" value per-call.
	 *
	 * @param defaultValue may be any long; should usually be one that doesn't occur as a typical value
	 */
	public void setDefaultValue (long defaultValue) {
		this.defaultValue = defaultValue;
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
		hasZeroValue = false;
		size = 0;
		resize(tableSize);
	}

	public void clear () {
		if (size == 0) {return;}
		hasZeroValue = false;
		size = 0;
		Arrays.fill(keyTable, 0);
	}

	/**
	 * Returns true if the specified value is in the map. Note this traverses the entire map and compares every value, which may
	 * be an expensive operation.
	 */
	public boolean containsValue (long value) {
		if (hasZeroValue && zeroValue == value) {return true;}
		long[] valueTable = this.valueTable;
		long[] keyTable = this.keyTable;
		for (int i = valueTable.length - 1; i >= 0; i--) {
			if (keyTable[i] != 0 && valueTable[i] == value) {return true;}
		}
		return false;
	}

	public boolean containsKey (long key) {
		if (key == 0) {return hasZeroValue;}
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
	 * Returns the key for the specified value, or null if it is not in the map. Note this traverses the entire map and compares
	 * every value, which may be an expensive operation.
	 */
	public long findKey (long value, long defaultKey) {
		if (hasZeroValue && zeroValue == value) {return 0;}
		long[] valueTable = this.valueTable;
		long[] keyTable = this.keyTable;
		for (int i = valueTable.length - 1; i >= 0; i--) {
			if (keyTable[i] != 0 && valueTable[i] == value) {return keyTable[i];}
		}

		return defaultKey;
	}

	/**
	 * Increases the size of the backing array to accommodate the specified number of additional items / loadFactor. Useful before
	 * adding many items to avoid multiple backing array resizes.
	 */
	public void ensureCapacity (int additionalCapacity) {
		int tableSize = tableSize(size + additionalCapacity, loadFactor);
		if (keyTable.length < tableSize) {resize(tableSize);}
	}

	protected void resize (int newSize) {
		int oldCapacity = keyTable.length;
		threshold = (int)(newSize * loadFactor);
		mask = newSize - 1;
		shift = BitConversion.countLeadingZeros((long)mask);

		hashMultiplier = Utilities.GOOD_MULTIPLIERS[(int)(hashMultiplier >>> 48 + shift) & 511];
		long[] oldKeyTable = keyTable;
		long[] oldValueTable = valueTable;

		keyTable = new long[newSize];
		valueTable = new long[newSize];

		if (size > 0) {
			for (int i = 0; i < oldCapacity; i++) {
				long key = oldKeyTable[i];
				if (key != 0) {putResize(key, oldValueTable[i]);}
			}
		}
	}

	/**
	 * Gets the current hash multiplier as used by {@link #place(long)}; for specific advanced usage only.
	 * The hash multiplier changes whenever {@link #resize(int)} is called, though its value before the resize
	 * affects its value after.
	 * @return the current hash multiplier, which should always be a large odd long
	 */
	public long getHashMultiplier () {
		return hashMultiplier;
	}

	/**
	 * Sets the current hash multiplier, then immediately calls {@link #resize(int)} without changing the target size; this
	 * is for specific advanced usage only. Calling resize() will change the multiplier before it gets used, and the current
	 * {@link #size()} of the data structure also changes the value. The hash multiplier is used by {@link #place(long)}.
	 * The hash multiplier must be an odd long, and should usually be "rather large." Here, that means the absolute value of
	 * the multiplier should be at least a quadrillion or so (a million billions, or roughly {@code 0x4000000000000L}). The
	 * only validation this does is to ensure the multiplier is odd; everything else is up to the caller. The hash multiplier
	 * changes whenever {@link #resize(int)} is called, though its value before the resize affects its value after. Because
	 * of how resize() randomizes the multiplier, even inputs such as {@code 1L} and {@code -1L} actually work well.
	 * <br>
	 * This is accessible at all mainly so serialization code that has a need to access the hash multiplier can do so, but
	 * also to provide an "emergency escape route" in case of hash flooding. Using one of the "known good" longs in
	 * {@link Utilities#GOOD_MULTIPLIERS} should usually be fine if you don't know what multiplier will work well.
	 * Be advised that because this has to call resize(), it isn't especially fast, and it slows
	 * down the more items are in the data structure. If you in a situation where you are worried about hash flooding, you
	 * also shouldn't permit adversaries to cause this method to be called frequently.
	 * @param hashMultiplier any odd long; will not be used as-is
	 */
	public void setHashMultiplier (long hashMultiplier) {
		this.hashMultiplier = hashMultiplier | 1L;
		resize(keyTable.length);
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
		long h = hasZeroValue ? zeroValue + size : size;
		long[] keyTable = this.keyTable;
		long[] valueTable = this.valueTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			long key = keyTable[i];
			if (key != 0) {
				h += key ^ key >>> 32;
				key = valueTable[i];
				h += key ^ key >>> 32;
			}
		}
		return (int)(h ^ h >>> 32);
	}

	@Override
	public boolean equals (Object obj) {
		if (obj == this) {return true;}
		if (!(obj instanceof LongLongMap)) {return false;}
		LongLongMap other = (LongLongMap)obj;
		if (other.size != size) {return false;}
		if (other.hasZeroValue != hasZeroValue || other.zeroValue != zeroValue) {return false;}
		long[] keyTable = this.keyTable;
		long[] valueTable = this.valueTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			long key = keyTable[i];
			if (key != 0) {
				long value = valueTable[i];
				if (value != other.get(key)) {return false;}
			}
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
		if (hasZeroValue) {
			buffer.append("0=").append(zeroValue);
			if (size > 1) {buffer.append(separator);}
		}
		long[] keyTable = this.keyTable;
		long[] valueTable = this.valueTable;
		int i = keyTable.length;
		while (i-- > 0) {
			long key = keyTable[i];
			if (key == 0) {continue;}
			buffer.append(key);
			buffer.append('=');
			long value = valueTable[i];
			buffer.append(value);
			break;
		}
		while (i-- > 0) {
			long key = keyTable[i];
			if (key == 0) {continue;}
			buffer.append(separator);
			buffer.append(key);
			buffer.append('=');
			long value = valueTable[i];
			buffer.append(value);
		}
		if (braces) {buffer.append('}');}
		return buffer.toString();
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
	public void forEach (LongLongBiConsumer action) {
		for (Entry entry : entrySet()) {
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
	public void replaceAll (LongLongToLongBiFunction function) {
		for (Entry entry : entrySet()) {
			entry.setValue(function.applyAsLong(entry.getKey(), entry.getValue()));
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
	public void truncate (int newSize) {
		long[] keyTable = this.keyTable;
		for (int i = keyTable.length - 1; i >= 0 && size > newSize; i--) {
			if (keyTable[i] != 0) {
				keyTable[i] = 0;
				--size;
			}
		}
		if (hasZeroValue && size > newSize) {
			hasZeroValue = false;
			--size;
		}
	}

	/**
	 * Reuses the iterator of the reused {@link Entries} produced by {@link #entrySet()};
	 * does not permit nested iteration. Iterate over {@link Entries#Entries(LongLongMap)} if you
	 * need nested or multithreaded iteration. You can remove an Entry from this LongLongMap
	 * using this Iterator.
	 *
	 * @return an {@link Iterator} over {@link Entry} key-value pairs; remove is supported.
	 */
	@Override
	public Iterator<Entry> iterator () {
		return entrySet().iterator();
	}

	/**
	 * Returns a {@link Set} view of the keys contained in this map.
	 * The set is backed by the map, so changes to the map are
	 * reflected in the set, and vice-versa.  If the map is modified
	 * while an iteration over the set is in progress (except through
	 * the iterator's own {@code remove} operation), the results of
	 * the iteration are undefined. The set supports element removal,
	 * which removes the corresponding mapping from the map, via the
	 * {@link LongIterator#remove()} operation.  It does
	 * not support the {@code add}, {@code addAll}, {@code remove},
	 * {@code removeAll}, or {@code clear} operations.
	 *
	 * <p>Note that the same Collection instance is returned each time this
	 * method is called. Use the {@link Keys} constructor for nested or
	 * multithreaded iteration.
	 *
	 * @return a set view of the keys contained in this map
	 */
	public Keys keySet () {
		if (keys1 == null || keys2 == null) {
			keys1 = new Keys(this);
			keys2 = new Keys(this);
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
	 * @return a {@link PrimitiveCollection.OfLong} containing {@code long} values
	 */
	public Values values () {
		if (values1 == null || values2 == null) {
			values1 = new Values(this);
			values2 = new Values(this);
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
	public Entries entrySet () {
		if (entries1 == null || entries2 == null) {
			entries1 = new Entries(this);
			entries2 = new Entries(this);
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

	public static class Entry {
		public long key;
		public long value;

		public Entry () {
		}

		public Entry (long key, long value) {
			this.key = key;
			this.value = value;
		}

		public Entry (Entry entry) {
			this.key = entry.key;
			this.value = entry.value;
		}

		@Override
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
		public long getKey () {
			return key;
		}

		/**
		 * Returns the value corresponding to this entry.  If the mapping
		 * has been removed from the backing map (by the iterator's
		 * {@code remove} operation), the results of this call are undefined.
		 *
		 * @return the value corresponding to this entry
		 */
		public long getValue () {
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
		public long setValue (long value) {
			long old = this.value;
			this.value = value;
			return old;
		}

		@Override
		public boolean equals (@Nullable Object o) {
			if (this == o) {return true;}
			if (o == null || getClass() != o.getClass()) {return false;}

			Entry entry = (Entry)o;

			if (key != entry.key) {return false;}
			return value == entry.value;
		}

		@Override
		public int hashCode () {
			return (int)((key ^ key >>> 32) * 0x9E3779B97F4A7C15L + (value ^ value << 32) >>> 32);
		}
	}

	public static abstract class MapIterator {
		static protected final int INDEX_ILLEGAL = -2, INDEX_ZERO = -1;

		public boolean hasNext;

		protected final LongLongMap map;
		protected int nextIndex, currentIndex;
		protected boolean valid = true;

		public MapIterator (LongLongMap map) {
			this.map = map;
			reset();
		}

		public void reset () {
			currentIndex = INDEX_ILLEGAL;
			nextIndex = INDEX_ZERO;
			if (map.hasZeroValue) {hasNext = true;} else {findNextIndex();}
		}

		void findNextIndex () {
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
		public boolean hasNext () {
			return hasNext;
		}

		public void remove () {
			int i = currentIndex;
			if (i == INDEX_ZERO && map.hasZeroValue) {
				map.hasZeroValue = false;
			} else if (i < 0) {
				throw new IllegalStateException("next must be called before remove.");
			} else {
				long[] keyTable = map.keyTable;
				long[] valueTable = map.valueTable;
				int mask = map.mask;
				int next = i + 1 & mask;
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
				if (i != currentIndex) {--nextIndex;}
			}
			currentIndex = INDEX_ILLEGAL;
			map.size--;
		}

	}

	public static class KeyIterator extends MapIterator implements LongIterator {
		public KeyIterator (LongLongMap map) {
			super(map);
		}

		@Override
		public long nextLong () {
			if (!hasNext) {throw new NoSuchElementException();}
			if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
			long key = nextIndex == INDEX_ZERO ? 0 : map.keyTable[nextIndex];
			currentIndex = nextIndex;
			findNextIndex();
			return key;
		}

		/**
		 * Returns a new LongList containing the remaining keys.
		 */
		public LongList toList () {
			LongList list = new LongList(map.size);
			while (hasNext) {list.add(nextLong());}
			return list;
		}

		@Override
		public boolean hasNext () {
			if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
			return hasNext;
		}
	}

	public static class ValueIterator extends MapIterator implements LongIterator {
		public ValueIterator (LongLongMap map) {
			super(map);
		}

		/**
		 * Returns the next {@code long} element in the iteration.
		 *
		 * @return the next {@code long} element in the iteration
		 * @throws NoSuchElementException if the iteration has no more elements
		 */
		@Override
		public long nextLong () {
			if (!hasNext) {throw new NoSuchElementException();}
			if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
			long value = nextIndex == INDEX_ZERO ? map.zeroValue : map.valueTable[nextIndex];
			currentIndex = nextIndex;
			findNextIndex();
			return value;
		}

		@Override
		public boolean hasNext () {
			if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
			return hasNext;
		}
	}

	public static class EntryIterator extends MapIterator implements Iterable<Entry>, Iterator<Entry> {
		protected Entry entry = new Entry();

		public EntryIterator (LongLongMap map) {
			super(map);
		}

		@Override
		public Iterator<Entry> iterator () {
			return this;
		}

		/**
		 * Note the same entry instance is returned each time this method is called.
		 */
		@Override
		public Entry next () {
			if (!hasNext) {throw new NoSuchElementException();}
			if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
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
		public boolean hasNext () {
			if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
			return hasNext;
		}
	}

	public static class Entries extends AbstractSet<Entry> {
		protected EntryIterator iter;

		public Entries (LongLongMap map) {
			iter = new EntryIterator(map);
		}

		/**
		 * Returns an iterator over the elements contained in this collection.
		 *
		 * @return an iterator over the elements contained in this collection
		 */
		@Override
		public Iterator<Entry> iterator () {
			return iter;
		}

		@Override
		public int size () {
			return iter.map.size;
		}

		@Override
		public int hashCode () {
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			iter.reset();
			int hc = super.hashCode();
			iter.currentIndex = currentIdx;
			iter.nextIndex = nextIdx;
			iter.hasNext = hn;
			return hc;
		}

		/**
		 * The iterator is reused by this data structure, and you can reset it
		 * back to the start of the iteration order using this.
		 */
		public void resetIterator () {
			iter.reset();
		}

		/**
		 * Returns a new {@link ObjectList} containing the remaining items.
		 * Does not change the position of this iterator.
		 */
		public ObjectList<Entry> toList () {
			ObjectList<Entry> list = new ObjectList<>(iter.map.size);
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			while (iter.hasNext) {list.add(new Entry(iter.next()));}
			iter.currentIndex = currentIdx;
			iter.nextIndex = nextIdx;
			iter.hasNext = hn;
			return list;
		}

		/**
		 * Append the remaining items that this can iterate through into the given Collection.
		 * Does not change the position of this iterator.
		 * @param coll any modifiable Collection; may have items appended into it
		 * @return the given collection
		 */
		public Collection<Entry> appendInto(Collection<Entry> coll) {
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			while (iter.hasNext) {coll.add(new Entry(iter.next()));}
			iter.currentIndex = currentIdx;
			iter.nextIndex = nextIdx;
			iter.hasNext = hn;
			return coll;
		}

		/**
		 * Append the remaining items that this can iterate through into the given Map.
		 * Does not change the position of this iterator. Note that a Map is not a Collection.
		 * @param coll any modifiable Map; may have items appended into it
		 * @return the given map
		 */
		public LongLongMap appendInto(LongLongMap coll) {
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

	public static class Values implements PrimitiveCollection.OfLong {
		protected ValueIterator iter;

		@Override
		public boolean add (long item) {
			throw new UnsupportedOperationException("LongLongMap.Values is read-only");
		}

		@Override
		public boolean remove (long item) {
			throw new UnsupportedOperationException("LongLongMap.Values is read-only");
		}

		@Override
		public boolean contains (long item) {
			return iter.map.containsValue(item);
		}

		@Override
		public void clear () {
			throw new UnsupportedOperationException("LongLongMap.Values is read-only");
		}

		/**
		 * Returns an iterator over the elements contained in this collection.
		 *
		 * @return an iterator over the elements contained in this collection
		 */
		@Override
		public LongIterator iterator () {
			return iter;
		}

		@Override
		public int size () {
			return iter.map.size;
		}

		public Values (LongLongMap map) {
			iter = new ValueIterator(map);
		}

		@Override
		public int hashCode () {
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			iter.reset();
			int hc = super.hashCode();
			iter.currentIndex = currentIdx;
			iter.nextIndex = nextIdx;
			iter.hasNext = hn;
			return hc;
		}

		/**
		 * The iterator is reused by this data structure, and you can reset it
		 * back to the start of the iteration order using this.
		 */
		public void resetIterator () {
			iter.reset();
		}

		/**
		 * Returns a new {@link ObjectList} containing the remaining items.
		 * Does not change the position of this iterator.
		 */
		public LongList toList () {
			LongList list = new LongList(iter.map.size);
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			while (iter.hasNext) {list.add(iter.nextLong());}
			iter.currentIndex = currentIdx;
			iter.nextIndex = nextIdx;
			iter.hasNext = hn;
			return list;
		}

		/**
		 * Append the remaining items that this can iterate through into the given Collection.
		 * Does not change the position of this iterator.
		 * @param coll any modifiable Collection; may have items appended into it
		 * @return the given collection
		 */
		public PrimitiveCollection.OfLong appendInto(PrimitiveCollection.OfLong coll) {
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			while (iter.hasNext) {coll.add(iter.nextLong());}
			iter.currentIndex = currentIdx;
			iter.nextIndex = nextIdx;
			iter.hasNext = hn;
			return coll;
		}

		@SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
		@Override
		public boolean equals (Object other) {
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			boolean eq = PrimitiveCollection.OfLong.super.areEqual(other);
			iter.currentIndex = currentIdx;
			iter.nextIndex = nextIdx;
			iter.hasNext = hn;
			return eq;
		}
	}

	public static class Keys implements PrimitiveCollection.OfLong {
		protected KeyIterator iter;

		public Keys (LongLongMap map) {
			iter = new KeyIterator(map);
		}

		@Override
		public boolean add (long item) {
			throw new UnsupportedOperationException("LongLongMap.Keys is read-only");
		}

		@Override
		public boolean remove (long item) {
			throw new UnsupportedOperationException("LongLongMap.Keys is read-only");
		}

		@Override
		public boolean contains (long item) {
			return iter.map.containsKey(item);
		}

		@Override
		public LongIterator iterator () {
			return iter;
		}

		@Override
		public void clear () {
			throw new UnsupportedOperationException("LongLongMap.Keys is read-only");
		}

		@Override
		public int size () {
			return iter.map.size;
		}

		@Override
		public int hashCode () {
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			iter.reset();
			int hc = super.hashCode();
			iter.currentIndex = currentIdx;
			iter.nextIndex = nextIdx;
			iter.hasNext = hn;
			return hc;
		}

		/**
		 * The iterator is reused by this data structure, and you can reset it
		 * back to the start of the iteration order using this.
		 */
		public void resetIterator () {
			iter.reset();
		}

		/**
		 * Returns a new {@link ObjectList} containing the remaining items.
		 * Does not change the position of this iterator.
		 */
		public LongList toList () {
			LongList list = new LongList(iter.map.size);
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			while (iter.hasNext) {list.add(iter.nextLong());}
			iter.currentIndex = currentIdx;
			iter.nextIndex = nextIdx;
			iter.hasNext = hn;
			return list;
		}

		/**
		 * Append the remaining items that this can iterate through into the given Collection.
		 * Does not change the position of this iterator.
		 * @param coll any modifiable Collection; may have items appended into it
		 * @return the given collection
		 */
		public PrimitiveCollection.OfLong appendInto(PrimitiveCollection.OfLong coll) {
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			while (iter.hasNext) {coll.add(iter.nextLong());}
			iter.currentIndex = currentIdx;
			iter.nextIndex = nextIdx;
			iter.hasNext = hn;
			return coll;
		}

		@SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
		@Override
		public boolean equals (Object other) {
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			boolean eq = PrimitiveCollection.OfLong.super.areEqual(other);
			iter.currentIndex = currentIdx;
			iter.nextIndex = nextIdx;
			iter.hasNext = hn;
			return eq;
		}
	}

	public long putIfAbsent (long key, long value) {
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

	public boolean replace (long key, long oldValue, long newValue) {
		long curValue = get(key);
		if (curValue != oldValue || !containsKey(key)) {
			return false;
		}
		put(key, newValue);
		return true;
	}

	public long replace (long key, long value) {
		if (key == 0) {
			if (hasZeroValue) {
				long oldValue = zeroValue;
				zeroValue = value;
				return oldValue;
			}
			return defaultValue;
		}
		int i = locateKey(key);
		if (i >= 0) {
			long oldValue = valueTable[i];
			valueTable[i] = value;
			return oldValue;
		}
		return defaultValue;
	}

	public long computeIfAbsent (long key, LongToLongFunction mappingFunction) {
		int i = locateKey(key);
		if (i < 0) {
			long newValue = mappingFunction.applyAsLong(key);
			put(key, newValue);
			return newValue;
		} else
			return valueTable[i];
	}

	public boolean remove (long key, long value) {
		int i = locateKey(key);
		if (i >= 0 && valueTable[i] == value) {
			remove(key);
			return true;
		}
		return false;
	}

	/**
	 * Just like Map's merge() default method, but this doesn't use Java 8 APIs (so it should work on RoboVM),
	 * this uses primitive values, and this won't remove entries if the remappingFunction returns null (because
	 * that isn't possible with primitive types).
	 * This uses a functional interface from Funderby.
	 * @param key key with which the resulting value is to be associated
	 * @param value the value to be merged with the existing value
	 *        associated with the key or, if no existing value
	 *        is associated with the key, to be associated with the key
	 * @param remappingFunction given a long from this and the long {@code value}, this should return what long to use
	 * @return the value now associated with key
	 */
	public long combine (long key, long value, LongLongToLongBiFunction remappingFunction) {
		int i = locateKey(key);
		long next = (i < 0) ? value : remappingFunction.applyAsLong(valueTable[i], value);
		put(key, next);
		return next;
	}

	/**
	 * Simply calls {@link #combine(long, long, LongLongToLongBiFunction)} on this map using every
	 * key-value pair in {@code other}. If {@code other} isn't empty, calling this will probably modify
	 * this map, though this depends on the {@code remappingFunction}.
	 * @param other a non-null LongLongMap (or subclass) with a compatible key type
	 * @param remappingFunction given a long value from this and a value from other, this should return what long to use
	 */
	public void combine (LongLongMap other, LongLongToLongBiFunction remappingFunction) {
		for (LongLongMap.Entry e : other.entrySet()) {
			combine(e.key, e.value, remappingFunction);
		}
	}

	/**
	 * Constructs an empty map.
	 * This is usually less useful than just using the constructor, but can be handy
	 * in some code-generation scenarios when you don't know how many arguments you will have.
	 *
	 * @return a new map containing nothing
	 */
	public static LongLongMap with () {
		return new LongLongMap(0);
	}

	/**
	 * Constructs a single-entry map given one key and one value.
	 * This is mostly useful as an optimization for {@link #with(Number, Number, Number...)}
	 * when there's no "rest" of the keys or values. Like the more-argument with(), this will
	 * convert its Number key to a primitive long, regardless of which Number type was used.
	 *
	 * @param key0   the first and only key; will be converted to a primitive long
	 * @param value0 the first and only value; will be converted to a primitive long
	 * @return a new map containing just the entry mapping key0 to value0
	 */
	public static LongLongMap with (Number key0, Number value0) {
		LongLongMap map = new LongLongMap(1);
		map.put(key0.longValue(), value0.longValue());
		return map;
	}

	/**
	 * Constructs a map given alternating keys and values.
	 * This can be useful in some code-generation scenarios, or when you want to make a
	 * map conveniently by-hand and have it populated at the start. You can also use
	 * {@link #LongLongMap(long[], long[])}, which takes all keys and then all values.
	 * This needs all keys to be some kind of (boxed) Number, and converts them to primitive
	 * {@code long}s. It also needs all values to be a (boxed) Number, and converts them to
	 * primitive {@code long}s. Any keys or values that aren't {@code Number}s have that
	 * entry skipped.
	 *
	 * @param key0   the first key; will be converted to a primitive long
	 * @param value0 the first value; will be converted to a primitive long
	 * @param rest   an array or varargs of Number elements
	 * @return a new map containing the given keys and values
	 */
	public static LongLongMap with (Number key0, Number value0, Number... rest) {
		LongLongMap map = new LongLongMap(1 + (rest.length >>> 1));
		map.put(key0.longValue(), value0.longValue());
		for (int i = 1; i < rest.length; i += 2) {
			map.put(rest[i - 1].longValue(), rest[i].longValue());
		}
		return map;
	}

	/**
	 * Constructs an empty map.
	 * This is usually less useful than just using the constructor, but can be handy
	 * in some code-generation scenarios when you don't know how many arguments you will have.
	 *
	 * @return a new map containing nothing
	 */
	public static LongLongMap withPrimitive () {
		return new LongLongMap(0);
	}

	/**
	 * Constructs a single-entry map given one key and one value.
	 * This is mostly useful as an optimization for {@link #withPrimitive(long, long, long...)}
	 * when there's no "rest" of the keys or values. This variation requires both the key
	 * and the value to be primitive {@code long}s.
	 * <br>
	 * This method has to be named differently from {@link #with(Number, Number)} to
	 * disambiguate the two, which would otherwise both be callable with all primitives
	 * (due to auto-boxing).
	 *
	 * @param key0   the first and only key; must not be boxed
	 * @param value0 the first and only value; must not be boxed
	 * @return a new map containing just the entry mapping key0 to value0
	 */
	public static LongLongMap withPrimitive (long key0, long value0) {
		LongLongMap map = new LongLongMap(1);
		map.put(key0, value0);
		return map;
	}

	/**
	 * Constructs a map given alternating keys and values.
	 * This can be useful in some code-generation scenarios, or when you want to make a
	 * map conveniently by-hand and have it populated at the start. You can also use
	 * {@link #LongLongMap(long[], long[])}, which takes all keys and then all values.
	 * This needs all keys and all values to be primitive {@code long}s; if any are boxed,
	 * then you should call {@link #with(Number, Number, Number...)}.
	 * <br>
	 * This method has to be named differently from {@link #with(Number, Number, Number...)} to
	 * disambiguate the two, which would otherwise both be callable with all primitives
	 * (due to auto-boxing).
	 *
	 * @param key0   the first key; must not be boxed
	 * @param value0 the first value; must not be boxed
	 * @param rest   an array or varargs of primitive long elements
	 * @return a new map containing the given keys and values
	 */
	public static LongLongMap withPrimitive (long key0, long value0, long... rest) {
		LongLongMap map = new LongLongMap(1 + (rest.length >>> 1));
		map.put(key0, value0);
		for (int i = 1; i < rest.length; i += 2) {
			map.put(rest[i - 1], rest[i]);
		}
		return map;
	}
}

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

import com.github.tommyettinger.function.IntObjBiConsumer;
import com.github.tommyettinger.function.IntObjToObjBiFunction;

import org.checkerframework.checker.nullness.qual.Nullable;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.PrimitiveIterator;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.IntFunction;

import static com.github.tommyettinger.ds.Utilities.neverIdentical;
import static com.github.tommyettinger.ds.Utilities.tableSize;

/**
 * An unordered map where the keys are unboxed ints and the values are objects. Null keys are not allowed. No allocation is
 * done except when growing the table size.
 * <p>
 * This class performs fast contains and remove (typically O(1), worst case O(n) but that is rare in practice). Add may be
 * slightly slower, depending on hash collisions. Hashcodes are rehashed to reduce collisions and the need to resize. Load factors
 * greater than 0.91 greatly increase the chances to resize to the next higher POT size.
 * <p>
 * Unordered sets and maps are not designed to provide especially fast iteration. Iteration is faster with {@link Ordered} types like
 * ObjectOrderedSet and ObjectObjectOrderedMap.
 * <p>
 * You can customize most behavior of this map by extending it. {@link #place(int)} can be overridden to change how hashCodes
 * are calculated (which can be useful for types like {@link StringBuilder} that don't implement hashCode()), and
 * {@link #locateKey(int)} can be overridden to change how equality is calculated.
 * <p>
 * This implementation uses linear probing with the backward shift algorithm for removal.
 * It tries different hashes from a simple family, with the hash changing on resize.
 * Linear probing continues to work even when all hashCodes collide, just more slowly.
 *
 * @author Nathan Sweet
 * @author Tommy Ettinger
 */
public class IntObjectMap<V> implements Iterable<IntObjectMap.Entry<V>> {

	protected int size;

	protected int[] keyTable;
	protected V[] valueTable;
	protected boolean hasZeroValue;
	@Nullable protected V zeroValue;

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
	 * Used by {@link #place(int)} to bit shift the upper bits of a {@code long} into a usable range (&gt;= 0 and &lt;=
	 * {@link #mask}). The shift can be negative, which is convenient to match the number of bits in mask: if mask is a 7-bit
	 * number, a shift of -7 shifts the upper 7 bits into the lowest 7 positions. This class sets the shift &gt; 32 and &lt; 64,
	 * which when used with an int will still move the upper bits of an int to the lower bits due to Java's implicit modulus on
	 * shifts.
	 * <p>
	 * {@link #mask} can also be used to mask the low bits of a number, which may be faster for some hashcodes, if
	 * {@link #place(int)} is overridden.
	 */
	protected int shift;

	/**
	 * Used by {@link #place(int)} to mix hashCode() results. Changes on every call to {@link #resize(int)} by default.
	 * This only needs to be serialized if the full key and value tables are serialized, or if the iteration order should be
	 * the same before and after serialization. Iteration order is better handled by using {@link IntObjectOrderedMap}.
	 */
	protected long hashMultiplier = 0xD1B54A32D192ED03L;

	/**
	 * A bitmask used to confine hashcodes to the size of the table. Must be all 1 bits in its low positions, ie a power of two
	 * minus 1. If {@link #place(int)} is overridden, this can be used instead of {@link #shift} to isolate usable bits of a
	 * hash.
	 */
	protected int mask;

	@Nullable protected transient Entries<V> entries1;
	@Nullable protected transient Entries<V> entries2;
	@Nullable protected transient Values<V> values1;
	@Nullable protected transient Values<V> values2;
	@Nullable protected transient Keys<V> keys1;
	@Nullable protected transient Keys<V> keys2;

	@Nullable public V defaultValue = null;

	/**
	 * Creates a new map with an initial capacity of 51 and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 */
	public IntObjectMap () {
		this(51, Utilities.getDefaultLoadFactor());
	}

	/**
	 * Creates a new map with the given starting capacity and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 */
	public IntObjectMap (int initialCapacity) {
		this(initialCapacity, Utilities.getDefaultLoadFactor());
	}

	/**
	 * Creates a new map with the specified initial capacity and load factor. This map will hold initialCapacity items before
	 * growing the backing table.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 * @param loadFactor      what fraction of the capacity can be filled before this has to resize; 0 &lt; loadFactor &lt;= 1
	 */
	public IntObjectMap (int initialCapacity, float loadFactor) {
		if (loadFactor <= 0f || loadFactor > 1f) {throw new IllegalArgumentException("loadFactor must be > 0 and <= 1: " + loadFactor);}
		this.loadFactor = loadFactor;

		int tableSize = tableSize(initialCapacity, loadFactor);
		threshold = (int)(tableSize * loadFactor);
		mask = tableSize - 1;
		shift = Long.numberOfLeadingZeros(mask);

		keyTable = new int[tableSize];
		valueTable = (V[])new Object[tableSize];
	}

	/**
	 * Creates a new map identical to the specified map.
	 * This performs a shallow copy, so any references to values (as well as the default value) are shared with the old map.
	 *
	 * @param map the map to copy
	 */
	public IntObjectMap (IntObjectMap<? extends V> map) {
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
	public IntObjectMap (int[] keys, V[] values) {
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
	public IntObjectMap (PrimitiveCollection.OfInt keys, Collection<? extends V> values) {
		this(Math.min(keys.size(), values.size()));
		putAll(keys, values);
	}

	/**
	 * Given two side-by-side collections, one of keys, one of values, this inserts each pair of key and value into this map with put().
	 *
	 * @param keys   a PrimitiveCollection of keys
	 * @param values a PrimitiveCollection of values
	 */
	public void putAll (PrimitiveCollection.OfInt keys, Collection<? extends V> values) {
		int length = Math.min(keys.size(), values.size());
		ensureCapacity(length);
		PrimitiveIterator.OfInt ki = keys.iterator();
		Iterator<? extends V> vi = values.iterator();
		while (ki.hasNext() && vi.hasNext()) {
			put(ki.next(), vi.next());
		}
	}

	/**
	 * Returns an index &gt;= 0 and &lt;= {@link #mask} for the specified {@code item}.
	 * Defaults to using {@link #hashMultiplier}, which changes every time the data structure resizes.
	 *
	 * @param item any int; it is usually mixed or masked here
	 * @return an index between 0 and {@link #mask} (both inclusive)
	 */
	protected int place (int item) {
		return (int)(item * hashMultiplier >>> shift);
	}

	/**
	 * Returns the index of the key if already present, else {@code ~index} for the next empty index.
	 * While this can be overridden to compare for equality differently than {@code ==} between ints, that
	 * isn't recommended because this has to treat zero keys differently, and it finds those with {@code ==}.
	 */
	protected int locateKey (int key) {
		int[] keyTable = this.keyTable;
		for (int i = place(key); ; i = i + 1 & mask) {
			int other = keyTable[i];
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
	public V put (int key, @Nullable V value) {
		if (key == 0) {
			V oldValue = defaultValue;
			if (hasZeroValue) {oldValue = zeroValue;} else {size++;}
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
		if (++size >= threshold) {resize(keyTable.length << 1);}
		return defaultValue;
	}

	/**
	 * Returns the old value associated with the specified key, or the given {@code defaultValue} if there was no prior value.
	 */
	@Nullable
	public V putOrDefault (int key, @Nullable V value, @Nullable V defaultValue) {
		if (key == 0) {
			V oldValue = defaultValue;
			if (hasZeroValue) {oldValue = zeroValue;} else {size++;}
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
		if (++size >= threshold) {resize(keyTable.length << 1);}
		return defaultValue;
	}

	/**
	 * Puts every key-value pair in the given map into this, with the values from the given map
	 * overwriting the previous values if two keys are identical.
	 *
	 * @param map a map with compatible key and value types; will not be modified
	 */
	public void putAll (IntObjectMap<? extends V> map) {
		ensureCapacity(map.size);
		if (map.hasZeroValue) {
			if (!hasZeroValue) {size++;}
			hasZeroValue = true;
			zeroValue = map.zeroValue;
		}
		int[] keyTable = map.keyTable;
		V[] valueTable = map.valueTable;
		int key;
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
	public void putAll (int[] keys, V[] values) {
		putAll(keys, 0, values, 0, Math.min(keys.length, values.length));
	}

	/**
	 * Given two side-by-side arrays, one of keys, one of values, this inserts each pair of key and value into this map with put().
	 *
	 * @param keys   an array of keys
	 * @param values an array of values
	 * @param length how many items from keys and values to insert, at-most
	 */
	public void putAll (int[] keys, V[] values, int length) {
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
	public void putAll (int[] keys, int keyOffset, V[] values, int valueOffset, int length) {
		length = Math.min(length, Math.min(keys.length - keyOffset, values.length - valueOffset));
		ensureCapacity(length);
		for (int k = keyOffset, v = valueOffset, i = 0, n = length; i < n; i++, k++, v++) {
			put(keys[k], values[v]);
		}
	}

	/**
	 * Skips checks for existing keys, doesn't increment size.
	 */
	protected void putResize (int key, @Nullable V value) {
		int[] keyTable = this.keyTable;
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
	 * @param key any {@code int}
	 */
	@Nullable
	public V get (int key) {
		if (key == 0) {return hasZeroValue ? zeroValue : defaultValue;}
		int[] keyTable = this.keyTable;
		for (int i = place(key); ; i = i + 1 & mask) {
			int other = keyTable[i];
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
	public V getOrDefault (int key, @Nullable V defaultValue) {
		if (key == 0) {return hasZeroValue ? zeroValue : defaultValue;}
		int[] keyTable = this.keyTable;
		for (int i = place(key); ; i = i + 1 & mask) {
			int other = keyTable[i];
			if (other == 0)
				return defaultValue;
			if (other == key)
				return valueTable[i];
		}
	}

	public @Nullable V remove (int key) {
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
		int[] keyTable = this.keyTable;
		int rem;
		V[] valueTable = this.valueTable;
		V oldValue = valueTable[i];
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
		valueTable[i] = null;

		size--;
		return oldValue;
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
	 * Gets the default value, a {@code V} which is returned by {@link #get(int)} if the key is not found.
	 * If not changed, the default value is null.
	 *
	 * @return the current default value
	 */
	@Nullable
	public V getDefaultValue () {
		return defaultValue;
	}

	/**
	 * Sets the default value, a {@code V} which is returned by {@link #get(int)} if the key is not found.
	 * If not changed, the default value is null. Note that {@link #getOrDefault(int, Object)} is also available,
	 * which allows specifying a "not-found" value per-call.
	 *
	 * @param defaultValue may be any V object or null; should usually be one that doesn't occur as a typical value
	 */
	public void setDefaultValue (@Nullable V defaultValue) {
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
		size = 0;
		resize(tableSize);
	}

	public void clear () {
		if (size == 0) {return;}
		size = 0;
		Arrays.fill(keyTable, 0);
	}

	/**
	 * Returns true if the specified value is in the map. Note this traverses the entire map and compares every value, which may
	 * be an expensive operation.
	 */
	public boolean containsValue (@Nullable Object value) {
		if (hasZeroValue) {
			return Objects.equals(zeroValue, value);
		}
		V[] valueTable = this.valueTable;
		int[] keyTable = this.keyTable;
		for (int i = valueTable.length - 1; i >= 0; i--) {
			if (keyTable[i] != 0 && Objects.equals(valueTable[i], value)) {return true;}
		}
		return false;
	}

	public boolean containsKey (int key) {
		if (key == 0) {return hasZeroValue;}
		int[] keyTable = this.keyTable;
		for (int i = place(key); ; i = i + 1 & mask) {
			int other = keyTable[i];
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
	public int findKey (@Nullable V value, int defaultKey) {
		if (hasZeroValue && Objects.equals(zeroValue, value)) {return 0;}
		V[] valueTable = this.valueTable;
		int[] keyTable = this.keyTable;
		for (int i = valueTable.length - 1; i >= 0; i--) {
			if (keyTable[i] != 0 && Objects.equals(valueTable[i], value)) {return keyTable[i];}
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
		shift = Long.numberOfLeadingZeros(mask);

		// we modify the hash multiplier by multiplying it by a number that Vigna and Steele considered optimal
		// for a 64-bit MCG random number generator, XORed with 2 times size to randomize the low bits more.
		hashMultiplier *= size + size ^ 0xF1357AEA2E62A9C5L;

		int[] oldKeyTable = keyTable;
		V[] oldValueTable = valueTable;

		keyTable = new int[newSize];
		valueTable = (V[])new Object[newSize];

		if (size > 0) {
			for (int i = 0; i < oldCapacity; i++) {
				int key = oldKeyTable[i];
				if (key != 0) {putResize(key, oldValueTable[i]);}
			}
		}
	}

	/**
	 * Gets the current hash multiplier as used by {@link #place(int)}; for specific advanced usage only.
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
	 * {@link #size()} of the data structure also changes the value. The hash multiplier is used by {@link #place(int)}.
	 * The hash multiplier must be an odd long, and should usually be "rather large." Here, that means the absolute value of
	 * the multiplier should be at least a quadrillion or so (a million billions, or roughly {@code 0x4000000000000L}). The
	 * only validation this does is to ensure the multiplier is odd; everything else is up to the caller. The hash multiplier
	 * changes whenever {@link #resize(int)} is called, though its value before the resize affects its value after. Because
	 * of how resize() randomizes the multiplier, even inputs such as {@code 1L} and {@code -1L} actually work well.
	 * <br>
	 * This is accessible at all mainly so serialization code that has a need to access the hash multiplier can do so, but
	 * also to provide an "emergency escape route" in case of hash flooding. Using one of the "golden longs" in
	 * {@link com.github.tommyettinger.digital.MathTools#GOLDEN_LONGS} should usually be fine if you don't know what
	 * multiplier will work well. Be advised that because this has to call resize(), it isn't especially fast, and it slows
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
		int h = hasZeroValue && zeroValue != null ? zeroValue.hashCode() ^ size : size;
		int[] keyTable = this.keyTable;
		V[] valueTable = this.valueTable;
		V v;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			int key = keyTable[i];
			if (key != 0) {
				h ^= key;
				v = valueTable[i];
				if (v != null)
					h ^= v.hashCode();
			}
		}
		return h;
	}

	@Override
	public boolean equals (Object obj) {
		if (obj == this) {return true;}
		if (!(obj instanceof IntObjectMap)) {return false;}
		IntObjectMap other = (IntObjectMap)obj;
		if (other.size != size) {return false;}
		if (other.hasZeroValue != hasZeroValue || !Objects.equals(other.zeroValue, zeroValue)) {return false;}
		int[] keyTable = this.keyTable;
		V[] valueTable = this.valueTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			int key = keyTable[i];
			if (key != 0) {
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
		int[] keyTable = this.keyTable;
		V[] valueTable = this.valueTable;
		int i = keyTable.length;
		while (i-- > 0) {
			int key = keyTable[i];
			if (key == 0) {continue;}
			buffer.append(key);
			buffer.append('=');
			V value = valueTable[i];
			buffer.append(value);
			break;
		}
		while (i-- > 0) {
			int key = keyTable[i];
			if (key == 0) {continue;}
			buffer.append(separator);
			buffer.append(key);
			buffer.append('=');
			V value = valueTable[i];
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
	public void forEach (IntObjBiConsumer<? super V> action) {
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
	public void replaceAll (IntObjToObjBiFunction<? super V, ? extends V> function) {
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
	public void truncate (int newSize) {
		int[] keyTable = this.keyTable;
		V[] valTable = this.valueTable;
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
	 * does not permit nested iteration. Iterate over {@link Entries#Entries(IntObjectMap)} if you
	 * need nested or multithreaded iteration. You can remove an Entry from this IntObjectMap
	 * using this Iterator.
	 *
	 * @return an {@link Iterator} over {@link Entry} key-value pairs; remove is supported.
	 */
	@Override
	public Iterator<Entry<V>> iterator () {
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
	public Keys<V> keySet () {
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
	public Values<V> values () {
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
	public Entries<V> entrySet () {
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
		public int key;
		@Nullable public V value;

		public Entry () {
		}

		public Entry (int key, @Nullable V value) {
			this.key = key;
			this.value = value;
		}

		public Entry (Entry<V> entry) {
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
		public int getKey () {
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
		@Nullable
		public V setValue (@Nullable V value) {
			V old = this.value;
			this.value = value;
			return old;
		}

		@Override
		public boolean equals (@Nullable Object o) {
			if (this == o) {return true;}
			if (o == null || getClass() != o.getClass()) {return false;}

			Entry entry = (Entry)o;

			if (key != entry.key) {return false;}
			return Objects.equals(value, entry.value);
		}

		@Override
		public int hashCode () {
			return value == null ? key : key ^ value.hashCode();
		}
	}

	static protected abstract class MapIterator<V> {
		static protected final int INDEX_ILLEGAL = -2, INDEX_ZERO = -1;

		public boolean hasNext;

		protected final IntObjectMap<V> map;
		protected int nextIndex, currentIndex;
		protected boolean valid = true;

		public MapIterator (IntObjectMap<V> map) {
			this.map = map;
			reset();
		}

		public void reset () {
			currentIndex = INDEX_ILLEGAL;
			nextIndex = INDEX_ZERO;
			if (map.hasZeroValue) {hasNext = true;} else {findNextIndex();}
		}

		void findNextIndex () {
			int[] keyTable = map.keyTable;
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
				map.zeroValue = null;
			} else if (i < 0) {
				throw new IllegalStateException("next must be called before remove.");
			} else {
				int[] keyTable = map.keyTable;
				V[] valueTable = map.valueTable;
				int mask = map.mask;
				int next = i + 1 & mask;
				int key;
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
				if (i != currentIndex) {--nextIndex;}
			}
			currentIndex = INDEX_ILLEGAL;
			map.size--;
		}

	}

	public static class KeyIterator<V> extends MapIterator<V> implements PrimitiveIterator.OfInt {

		public KeyIterator (IntObjectMap<V> map) {
			super(map);
		}

		@Override
		public int nextInt () {
			if (!hasNext) {throw new NoSuchElementException();}
			if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
			int key = nextIndex == INDEX_ZERO ? 0 : map.keyTable[nextIndex];
			currentIndex = nextIndex;
			findNextIndex();
			return key;
		}

		/**
		 * Returns a new IntList containing the remaining keys.
		 */
		public IntList toList () {
			IntList list = new IntList(true, map.size);
			while (hasNext) {list.add(nextInt());}
			return list;
		}

		@Override
		public boolean hasNext () {
			if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
			return hasNext;
		}
	}

	public static class ValueIterator<V> extends MapIterator<V> implements Iterator<V> {
		public ValueIterator (IntObjectMap<V> map) {
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
		public V next () {
			if (!hasNext) {throw new NoSuchElementException();}
			if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
			V value = nextIndex == INDEX_ZERO ? map.zeroValue : map.valueTable[nextIndex];
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

	public static class EntryIterator<V> extends MapIterator<V> implements Iterable<Entry<V>>, Iterator<Entry<V>> {
		protected Entry<V> entry = new Entry<>();

		public EntryIterator (IntObjectMap<V> map) {
			super(map);
		}

		@Override
		public Iterator<Entry<V>> iterator () {
			return this;
		}

		/**
		 * Note the same entry instance is returned each time this method is called.
		 */
		@Override
		public Entry<V> next () {
			if (!hasNext) {throw new NoSuchElementException();}
			if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
			int[] keyTable = map.keyTable;
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

	public static class Entries<V> extends AbstractSet<Entry<V>> {
		protected EntryIterator<V> iter;

		public Entries (IntObjectMap<V> map) {
			iter = new EntryIterator<>(map);
		}

		/**
		 * Returns an iterator over the elements contained in this collection.
		 *
		 * @return an iterator over the elements contained in this collection
		 */
		@Override
		public Iterator<Entry<V>> iterator () {
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
		public ObjectList<Entry<V>> toList () {
			ObjectList<Entry<V>> list = new ObjectList<>(iter.map.size);
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			while (iter.hasNext) {list.add(new Entry<>(iter.next()));}
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
		public Collection<Entry<V>> appendInto(Collection<Entry<V>> coll) {
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			while (iter.hasNext) {coll.add(new Entry<>(iter.next()));}
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
		public IntObjectMap<V> appendInto(IntObjectMap<V> coll) {
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

	public static class Values<V> extends AbstractCollection<V> {
		protected ValueIterator<V> iter;

		@Override
		public boolean add (@Nullable V item) {
			throw new UnsupportedOperationException("IntObjectMap.Values is read-only");
		}

		@Override
		public boolean remove (@Nullable Object item) {
			throw new UnsupportedOperationException("IntObjectMap.Values is read-only");
		}

		@Override
		public boolean contains (@Nullable Object item) {
			return iter.map.containsValue(item);
		}

		@Override
		public void clear () {
			throw new UnsupportedOperationException("IntObjectMap.Values is read-only");
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

		public Values (IntObjectMap<V> map) {
			iter = new ValueIterator<>(map);
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
		public ObjectList<V> toList () {
			ObjectList<V> list = new ObjectList<>(iter.map.size);
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			while (iter.hasNext) {list.add(iter.next());}
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
		public Collection<V> appendInto(Collection<V> coll) {
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			while (iter.hasNext) {coll.add(iter.next());}
			iter.currentIndex = currentIdx;
			iter.nextIndex = nextIdx;
			iter.hasNext = hn;
			return coll;
		}

	}

	public static class Keys<V> implements PrimitiveCollection.OfInt {
		protected KeyIterator<V> iter;

		public Keys (IntObjectMap<V> map) {
			iter = new KeyIterator<>(map);
		}

		@Override
		public boolean add (int item) {
			throw new UnsupportedOperationException("IntObjectMap.Keys is read-only");
		}

		@Override
		public boolean remove (int item) {
			throw new UnsupportedOperationException("IntObjectMap.Keys is read-only");
		}

		@Override
		public boolean contains (int item) {
			return iter.map.containsKey(item);
		}

		@Override
		public PrimitiveIterator.OfInt iterator () {
			return iter;
		}

		@Override
		public void clear () {
			throw new UnsupportedOperationException("IntObjectMap.Keys is read-only");
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
		public IntList toList () {
			IntList list = new IntList(iter.map.size);
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			while (iter.hasNext) {list.add(iter.nextInt());}
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
		public PrimitiveCollection.OfInt appendInto(PrimitiveCollection.OfInt coll) {
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			while (iter.hasNext) {coll.add(iter.nextInt());}
			iter.currentIndex = currentIdx;
			iter.nextIndex = nextIdx;
			iter.hasNext = hn;
			return coll;
		}

		@SuppressWarnings("DataFlowIssue") // PrimitiveCollection.OfInt.equals() changes the iterator
		@Override
		public boolean equals (Object other) {
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			boolean eq = OfInt.super.equals(other);
			iter.currentIndex = currentIdx;
			iter.nextIndex = nextIdx;
			iter.hasNext = hn;
			return eq;
		}
	}

	@Nullable
	public V putIfAbsent (int key, V value) {
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

	public boolean replace (int key, V oldValue, V newValue) {
		V curValue = get(key);
		if (!Objects.equals(curValue, oldValue) || !containsKey(key)) {
			return false;
		}
		put(key, newValue);
		return true;
	}

	@Nullable
	public V replace (int key, V value) {
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

	public V computeIfAbsent (int key, IntFunction<? extends V> mappingFunction) {
		int i = locateKey(key);
		if (i < 0) {
			V newValue = mappingFunction.apply(key);
			put(key, newValue);
			return newValue;
		} else
			return valueTable[i];
	}

	public boolean remove (int key, Object value) {
		int i = locateKey(key);
		if (i >= 0 && Objects.equals(valueTable[i], value)) {
			remove(key);
			return true;
		}
		return false;
	}

	@Nullable
	public V merge (int key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
		int i = locateKey(key);
		V next = (i < 0) ? value : remappingFunction.apply(valueTable[i], value);
		if (next == null)
			remove(key);
		else
			put(key, next);
		return next;
	}

	/**
	 * Constructs a single-entry map given one key and one value.
	 * This is mostly useful as an optimization for {@link #with(Number, Object, Object...)}
	 * when there's no "rest" of the keys or values. Like the more-argument with(), this will
	 * convert its Number key to a primitive int, regardless of which Number type was used.
	 *
	 * @param key0   the first and only key; will be converted to a primitive int
	 * @param value0 the first and only value
	 * @param <V>    the type of value0
	 * @return a new map containing just the entry mapping key0 to value0
	 */
	public static <V> IntObjectMap<V> with (Number key0, V value0) {
		IntObjectMap<V> map = new IntObjectMap<>(1);
		map.put(key0.intValue(), value0);
		return map;
	}

	/**
	 * Constructs a map given alternating keys and values.
	 * This can be useful in some code-generation scenarios, or when you want to make a
	 * map conveniently by-hand and have it populated at the start. You can also use
	 * {@link #IntObjectMap(int[], Object[])}, which takes all keys and then all values.
	 * This needs all keys to be some kind of (boxed) Number, and converts them to primitive
	 * {@code int}s. It also needs all values to have the same type, because it gets those
	 * types from the first key parameter and first value parameter. Any keys that aren't
	 * {@code Number}s or values that don't have V as their type have that entry skipped.
	 *
	 * @param key0   the first key; will be converted to a primitive int
	 * @param value0 the first value; will be used to determine the type of all values
	 * @param rest   an array or varargs of alternating Number, V, Number, V... elements
	 * @param <V>    the type of values, inferred from value0
	 * @return a new map containing the given keys and values
	 */
	@SuppressWarnings("unchecked")
	public static <V> IntObjectMap<V> with (Number key0, V value0, Object... rest) {
		IntObjectMap<V> map = new IntObjectMap<>(1 + (rest.length >>> 1));
		map.put(key0.intValue(), value0);
		for (int i = 1; i < rest.length; i += 2) {
			try {
				map.put(((Number)rest[i - 1]).intValue(), (V)rest[i]);
			} catch (ClassCastException ignored) {
			}
		}
		return map;
	}
}

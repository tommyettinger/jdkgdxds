/*******************************************************************************
 * Copyright 2020 See AUTHORS file.
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
 ******************************************************************************/

package com.github.tommyettinger.ds;

import com.github.tommyettinger.ds.support.BitConversion;
import com.github.tommyettinger.ds.support.function.FloatFloatToFloatBiFunction;
import com.github.tommyettinger.ds.support.function.ObjFloatConsumer;
import com.github.tommyettinger.ds.support.function.ObjFloatToFloatBiFunction;
import com.github.tommyettinger.ds.support.function.ToFloatFunction;
import com.github.tommyettinger.ds.support.util.FloatIterator;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import static com.github.tommyettinger.ds.Utilities.tableSize;

/**
 * An unordered map where the keys are objects and the values are floats. Null keys are not allowed. No allocation is done except
 * when growing the table size.
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
 * {@link #locateKey(Object)} can be overridden to change how equality is calculated.
 * <p>
 * This implementation uses linear probing with the backward shift algorithm for removal. Hashcodes are rehashed using Fibonacci
 * hashing, instead of the more common power-of-two mask, to better distribute poor hashCodes (see <a href=
 * "https://probablydance.com/2018/06/16/fibonacci-hashing-the-optimization-that-the-world-forgot-or-a-better-alternative-to-integer-modulo/">Malte
 * Skarupke's blog post</a>). Linear probing continues to work even when all hashCodes collide, just more slowly.
 *
 * @author Nathan Sweet
 * @author Tommy Ettinger
 */
public class ObjectFloatMap<K> implements Iterable<ObjectFloatMap.Entry<K>>, Serializable {
	private static final long serialVersionUID = 0L;

	protected int size;

	protected K[] keyTable;
	protected float[] valueTable;

	protected float loadFactor;
	protected int threshold;

	protected int shift;

	/**
	 * A bitmask used to confine hashcodes to the size of the table. Must be all 1 bits in its low positions, ie a power of two
	 * minus 1.
	 */
	protected int mask;
	@Nullable protected transient Entries<K> entries1;
	@Nullable protected transient Entries<K> entries2;
	@Nullable protected transient Values<K> values1;
	@Nullable protected transient Values<K> values2;
	@Nullable protected transient Keys<K> keys1;
	@Nullable protected transient Keys<K> keys2;

	public float defaultValue = 0;

	/**
	 * Creates a new map with an initial capacity of 51 and a load factor of 0.8.
	 */
	public ObjectFloatMap () {
		this(51, 0.8f);
	}

	/**
	 * Creates a new map with the given starting capacity and a load factor of 0.8.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 */
	public ObjectFloatMap (int initialCapacity) {
		this(initialCapacity, 0.8f);
	}

	/**
	 * Creates a new map with the specified initial capacity and load factor. This map will hold initialCapacity items before
	 * growing the backing table.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 * @param loadFactor what fraction of the capacity can be filled before this has to resize; 0 &lt; loadFactor &lt;= 1
	 */
	public ObjectFloatMap (int initialCapacity, float loadFactor) {
		if (loadFactor <= 0f || loadFactor > 1f) { throw new IllegalArgumentException("loadFactor must be > 0 and <= 1: " + loadFactor); }
		this.loadFactor = loadFactor;

		int tableSize = tableSize(initialCapacity, loadFactor);
		threshold = (int)(tableSize * loadFactor);
		mask = tableSize - 1;
		shift = Long.numberOfLeadingZeros(mask);

		keyTable = (K[])new Object[tableSize];
		valueTable = new float[tableSize];
	}

	/**
	 * Creates a new map identical to the specified map.
	 * @param map the map to copy
	 */
	public ObjectFloatMap (ObjectFloatMap<? extends K> map) {
		this((int)(map.keyTable.length * map.loadFactor), map.loadFactor);
		System.arraycopy(map.keyTable, 0, keyTable, 0, map.keyTable.length);
		System.arraycopy(map.valueTable, 0, valueTable, 0, map.valueTable.length);
		size = map.size;
		defaultValue = map.defaultValue;
	}

	/**
	 * Given two side-by-side arrays, one of keys, one of values, this constructs a map and inserts each pair of key and value into it.
	 * If keys and values have different lengths, this only uses the length of the smaller array.
	 *
	 * @param keys   an array of keys
	 * @param values an array of values
	 */
	public ObjectFloatMap (K[] keys, float[] values) {
		this(Math.min(keys.length, values.length));
		putAll(keys, values);
	}

	/**
	 * Given two side-by-side collections, one of keys, one of values, this constructs a map and inserts each pair of key and value into it.
	 * If keys and values have different lengths, this only uses the length of the smaller collection.
	 *
	 * @param keys   a Collection of keys
	 * @param values a PrimitiveCollection of values
	 */
	public ObjectFloatMap (Collection<? extends K> keys, PrimitiveCollection.OfFloat values) {
		this(Math.min(keys.size(), values.size()));
		putAll(keys, values);
	}

	/**
	 * Given two side-by-side collections, one of keys, one of values, this inserts each pair of key and value into this map with put().
	 *
	 * @param keys   a Collection of keys
	 * @param values a PrimitiveCollection of values
	 */
	public void putAll (Collection<? extends K> keys, PrimitiveCollection.OfFloat values) {
		int length = Math.min(keys.size(), values.size());
		ensureCapacity(length);
		K key;
		Iterator<? extends K> ki = keys.iterator();
		FloatIterator vi = values.iterator();
		while (ki.hasNext() && vi.hasNext()) {
			key = ki.next();
			if (key != null) {
				put(key, vi.next());
			}
		}
	}

	/**
	 * Returns an index &gt;= 0 and &lt;= {@link #mask} for the specified {@code item}.
	 * <p>
	 * The default behavior uses Fibonacci hashing; it simply gets the {@link Object#hashCode()}
	 * of {@code item}, multiplies it by a specific long constant related to the golden ratio,
	 * and makes an unsigned right shift by {@link #shift} before casting to int and returning.
	 * This can be overridden to hash {@code item} differently, though all implementors must
	 * ensure this returns results in the range of 0 to {@link #mask}, inclusive. If nothing
	 * else is changed, then unsigned-right-shifting an int or long by {@link #shift} will also
	 * restrict results to the correct range.
	 *
	 * @param item a non-null Object; its hashCode() method should be used by most implementations.
	 */
	protected int place (Object item) {
		return (int)(item.hashCode() * 0x9E3779B97F4A7C15L >>> shift);
	}

	/**
	 * Returns the index of the key if already present, else {@code ~index} for the next empty index. This can be overridden
	 * to compare for equality differently than {@link Object#equals(Object)}.
	 * <p>
	 * If source is not easily available and you want to override this, the reference source is:
	 * <pre>
	 * protected int locateKey (Object key) {
	 * 		K[] keyTable = this.keyTable;
	 * 		for (int i = place(key); ; i = i + 1 &amp; mask) {
	 * 			K other = keyTable[i];
	 * 			if (other == null)
	 * 				return ~i; // Always negative; means empty space is available at i.
	 * 			if (other.equals(key)) // If you want to change how equality is determined, do it here.
	 * 				return i; // Same key was found.
	 *      }
	 * }
	 * </pre>
	 *
	 * @param key a non-null Object that should probably be a K
	 */
	protected int locateKey (Object key) {
		K[] keyTable = this.keyTable;
		for (int i = place(key); ; i = i + 1 & mask) {
			K other = keyTable[i];
			if (other == null) {
				return ~i; // Always negative; means empty space is available at i.
			}
			if (other.equals(key)) // If you want to change how equality is determined, do it here.
			{
				return i; // Same key was found.
			}
		}
	}

	/**
	 * Returns the old value associated with the specified key, or this map's {@link #defaultValue} if there was no prior value.
	 */
	public float put (K key, float value) {
		int i = locateKey(key);
		if (i >= 0) { // Existing key was found.
			float oldValue = valueTable[i];
			valueTable[i] = value;
			return oldValue;
		}
		i = ~i; // Empty space was found.
		keyTable[i] = key;
		valueTable[i] = value;
		if (++size >= threshold) { resize(keyTable.length << 1); }
		return defaultValue;
	}

	/**
	 * Returns the old value associated with the specified key, or the given {@code defaultValue} if there was no prior value.
	 */
	public float putOrDefault (K key, float value, float defaultValue) {
		int i = locateKey(key);
		if (i >= 0) { // Existing key was found.
			float oldValue = valueTable[i];
			valueTable[i] = value;
			return oldValue;
		}
		i = ~i; // Empty space was found.
		keyTable[i] = key;
		valueTable[i] = value;
		if (++size >= threshold) { resize(keyTable.length << 1); }
		return defaultValue;
	}

	public void putAll (ObjectFloatMap<? extends K> map) {
		ensureCapacity(map.size);
		K[] keyTable = map.keyTable;
		float[] valueTable = map.valueTable;
		K key;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			key = keyTable[i];
			if (key != null) { put(key, valueTable[i]); }
		}
	}

	/**
	 * Given two side-by-side arrays, one of keys, one of values, this inserts each pair of key and value into this map with put().
	 *
	 * @param keys   an array of keys
	 * @param values an array of values
	 */
	public void putAll (K[] keys, float[] values) {
		putAll(keys, 0, values, 0, Math.min(keys.length, values.length));
	}

	/**
	 * Given two side-by-side arrays, one of keys, one of values, this inserts each pair of key and value into this map with put().
	 *
	 * @param keys   an array of keys
	 * @param values an array of values
	 * @param length how many items from keys and values to insert, at-most
	 */
	public void putAll (K[] keys, float[] values, int length) {
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
	public void putAll (K[] keys, int keyOffset, float[] values, int valueOffset, int length) {
		length = Math.min(length, Math.min(keys.length - keyOffset, values.length - valueOffset));
		ensureCapacity(length);
		K key;
		for (int k = keyOffset, v = valueOffset, i = 0, n = length; i < n; i++, k++, v++) {
			key = keys[k];
			if (key != null) { put(key, values[v]); }
		}
	}

	/**
	 * Skips checks for existing keys, doesn't increment size.
	 */
	private void putResize (K key, float value) {
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
	 * Returns the value for the specified key, or {@link #defaultValue} if the key is not in the map.
	 *
	 * @param key a non-null Object that should almost always be a {@code K} (or an instance of a subclass of {@code K})
	 */
	public float get (Object key) {
		int i = locateKey(key);
		return i < 0 ? defaultValue : valueTable[i];
	}

	/**
	 * Returns the value for the specified key, or the default value if the key is not in the map.
	 */
	public float getOrDefault (Object key, float defaultValue) {
		int i = locateKey(key);
		return i < 0 ? defaultValue : valueTable[i];
	}

	/**
	 * Returns the key's current value and increments the stored value. If the key is not in the map, defaultValue + increment is
	 * put into the map and defaultValue is returned.
	 */
	public float getAndIncrement (K key, float defaultValue, float increment) {
		int i = locateKey(key);
		if (i >= 0) { // Existing key was found.
			float oldValue = valueTable[i];
			valueTable[i] += increment;
			return oldValue;
		}
		i = ~i; // Empty space was found.
		keyTable[i] = key;
		valueTable[i] = defaultValue + increment;
		if (++size >= threshold) { resize(keyTable.length << 1); }
		return defaultValue;
	}

	public float remove (Object key) {
		int i = locateKey(key);
		if (i < 0) { return defaultValue; }
		K[] keyTable = this.keyTable;
		K rem;
		float[] valueTable = this.valueTable;
		float oldValue = valueTable[i];
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
	 * Gets the default value, a {@code float} which is returned by {@link #get(Object)} if the key is not found.
	 * If not changed, the default value is 0.
	 *
	 * @return the current default value
	 */
	public float getDefaultValue () {
		return defaultValue;
	}

	/**
	 * Sets the default value, a {@code float} which is returned by {@link #get(Object)} if the key is not found.
	 * If not changed, the default value is 0. Note that {@link #getOrDefault(Object, float)} is also available,
	 * which allows specifying a "not-found" value per-call.
	 *
	 * @param defaultValue may be any float; should usually be one that doesn't occur as a typical value
	 */
	public void setDefaultValue (float defaultValue) {
		this.defaultValue = defaultValue;
	}

	/**
	 * Reduces the size of the backing arrays to be the specified capacity / loadFactor, or less. If the capacity is already less,
	 * nothing is done. If the map contains more items than the specified capacity, the next highest power of two capacity is used
	 * instead.
	 */
	public void shrink (int maximumCapacity) {
		if (maximumCapacity < 0) { throw new IllegalArgumentException("maximumCapacity must be >= 0: " + maximumCapacity); }
		int tableSize = tableSize(maximumCapacity, loadFactor);
		if (keyTable.length > tableSize) { resize(tableSize); }
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
		if (size == 0) { return; }
		size = 0;
		Arrays.fill(keyTable, null);
	}

	public boolean containsKey (Object key) {
		return locateKey(key) >= 0;
	}

	/**
	 * Returns true if the specified value is in the map. Note this traverses the entire map and compares every value, which may
	 * be an expensive operation.
	 *
	 * @param value the float value to check for; will be compared with {@link Utilities#isEqual(float, float)}
	 * @return true if this map contains the given value, false otherwise
	 */
	public boolean containsValue (float value) {
		float[] valueTable = this.valueTable;
		K[] keyTable = this.keyTable;
		for (int i = valueTable.length - 1; i >= 0; i--) {
			if (keyTable[i] != null && Utilities.isEqual(valueTable[i], value)) { return true; }
		}
		return false;
	}

	/**
	 * Returns true if the specified value is in the map. Note this traverses the entire map and compares every value, which may
	 * be an expensive operation.
	 *
	 * @param value     the float value to check for; will be compared with {@link Utilities#isEqual(float, float, float)}
	 * @param tolerance how much the given value is permitted to differ from a value in this while being considered equal
	 * @return true if this map contains the given value, false otherwise
	 */
	public boolean containsValue (float value, float tolerance) {
		float[] valueTable = this.valueTable;
		K[] keyTable = this.keyTable;
		for (int i = valueTable.length - 1; i >= 0; i--) {
			if (keyTable[i] != null && Utilities.isEqual(valueTable[i], value, tolerance)) { return true; }
		}
		return false;
	}

	/**
	 * Returns the key for the specified value, or null if it is not in the map. Note this traverses the entire map and compares
	 * every value, which may be an expensive operation. Uses {@link Utilities#isEqual(float, float)} to compare values.
	 *
	 * @param value the value to look for
	 * @return the key associated with the given value, if it was found, or null otherwise
	 */
	@Nullable
	public K findKey (float value) {
		float[] valueTable = this.valueTable;
		K[] keyTable = this.keyTable;
		for (int i = valueTable.length - 1; i >= 0; i--) {
			if (keyTable[i] != null && Utilities.isEqual(valueTable[i], value)) { return keyTable[i]; }
		}
		return null;
	}

	/**
	 * Returns the key for the specified value, or null if it is not in the map. Note this traverses the entire map and compares
	 * every value, which may be an expensive operation. Uses {@link Utilities#isEqual(float, float, float)} to compare values.
	 *
	 * @param value     the value to look for
	 * @param tolerance how much the given value is permitted to differ from a value in this while being considered equal
	 * @return the key associated with the given value, if it was found, or null otherwise
	 */
	@Nullable
	public K findKey (float value, float tolerance) {
		float[] valueTable = this.valueTable;
		K[] keyTable = this.keyTable;
		for (int i = valueTable.length - 1; i >= 0; i--) {
			if (keyTable[i] != null && Utilities.isEqual(valueTable[i], value, tolerance)) { return keyTable[i]; }
		}
		return null;
	}

	/**
	 * Increases the size of the backing array to accommodate the specified number of additional items / loadFactor. Useful before
	 * adding many items to avoid multiple backing array resizes.
	 */
	public void ensureCapacity (int additionalCapacity) {
		int tableSize = tableSize(size + additionalCapacity, loadFactor);
		if (keyTable.length < tableSize) { resize(tableSize); }
	}

	protected void resize (int newSize) {
		int oldCapacity = keyTable.length;
		threshold = (int)(newSize * loadFactor);
		mask = newSize - 1;
		shift = Long.numberOfLeadingZeros(mask);

		K[] oldKeyTable = keyTable;
		float[] oldValueTable = valueTable;

		keyTable = (K[])new Object[newSize];
		valueTable = new float[newSize];

		if (size > 0) {
			for (int i = 0; i < oldCapacity; i++) {
				K key = oldKeyTable[i];
				if (key != null) { putResize(key, oldValueTable[i]); }
			}
		}
	}

	public float getLoadFactor () {
		return loadFactor;
	}

	public void setLoadFactor (float loadFactor) {
		if (loadFactor <= 0f || loadFactor > 1f) { throw new IllegalArgumentException("loadFactor must be > 0 and <= 1: " + loadFactor); }
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
		float[] valueTable = this.valueTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			K key = keyTable[i];
			if (key != null) {
				h ^= key.hashCode();
				h ^= BitConversion.floatToRawIntBits(valueTable[i]);
			}
		}
		return h;
	}

	@Override
	public boolean equals (Object obj) {
		if (obj == this) { return true; }
		if (!(obj instanceof ObjectFloatMap)) { return false; }
		ObjectFloatMap other = (ObjectFloatMap)obj;
		if (other.size != size) { return false; }
		K[] keyTable = this.keyTable;
		float[] valueTable = this.valueTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			K key = keyTable[i];
			if (key != null) {
				float value = valueTable[i];
				if (value != other.get(key)) { return false; }
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
		if (size == 0) { return braces ? "{}" : ""; }
		StringBuilder buffer = new StringBuilder(32);
		if (braces) { buffer.append('{'); }
		K[] keyTable = this.keyTable;
		float[] valueTable = this.valueTable;
		int i = keyTable.length;
		while (i-- > 0) {
			K key = keyTable[i];
			if (key == null) { continue; }
			buffer.append(key == this ? "(this)" : key);
			buffer.append('=');
			float value = valueTable[i];
			buffer.append(value);
			break;
		}
		while (i-- > 0) {
			K key = keyTable[i];
			if (key == null) { continue; }
			buffer.append(separator);
			buffer.append(key == this ? "(this)" : key);
			buffer.append('=');
			float value = valueTable[i];
			buffer.append(value);
		}
		if (braces) { buffer.append('}'); }
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
	public void forEach(ObjFloatConsumer<? super K> action) {
		for(Entry<K> entry : entrySet()) {
			action.accept(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * Replaces each entry's value with the result of invoking the given
	 * function on that entry until all entries have been processed or the
	 * function throws an exception.  Exceptions thrown by the function are
	 * relayed to the caller.
	 * @param function the function to apply to each entry
	 */
	public void replaceAll(ObjFloatToFloatBiFunction<? super K> function){
		for (Entry<K> entry : entrySet()) {
			entry.setValue(function.applyAsFloat(entry.getKey(), entry.getValue()));
		}
	}

	/**
	 * Reuses the iterator of the reused {@link Entries} produced by {@link #entrySet()};
	 * does not permit nested iteration. Iterate over {@link Entries#Entries(ObjectFloatMap)} if you
	 * need nested or multithreaded iteration. You can remove an Entry from this ObjectFloatMap
	 * using this Iterator.
	 *
	 * @return an {@link Iterator} over {@link Entry} key-value pairs; remove is supported.
	 */
	@Override
	public Iterator<Entry<K>> iterator () {
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
	public Keys<K> keySet () {
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
	 * @return a {@link Collection} of float values
	 */
	public Values<K> values () {
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
	 * Returns a Set of Entry, containing the entries in the map. Remove is supported by the Set's iterator.
	 * Note that the same iterator instance is returned each time this method is called.
	 * Use the {@link Entries} constructor for nested or multithreaded iteration.
	 *
	 * @return a {@link Set} of {@link Entry} key-value pairs
	 */
	public Entries<K> entrySet () {
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

	public static class Entry<K> {
		@Nullable public K key;
		public float value;

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
		public K getKey () {
			assert key != null;
			return key;
		}

		/**
		 * Returns the value corresponding to this entry.  If the mapping
		 * has been removed from the backing map (by the iterator's
		 * {@code remove} operation), the results of this call are undefined.
		 *
		 * @return the value corresponding to this entry
		 */
		public float getValue () {
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
		public float setValue (float value) {
			float old = this.value;
			this.value = value;
			return old;
		}

		@Override
		public boolean equals (@Nullable Object o) {
			if (this == o) { return true; }
			if (o == null || getClass() != o.getClass() || key == null) { return false; }

			Entry<?> entry = (Entry<?>)o;

			if (!key.equals(entry.key)) { return false; }
			return value == entry.value;
		}

		@Override
		public int hashCode () {
			assert key != null;
			return key.hashCode() * 31 + BitConversion.floatToRawIntBits(value);
		}
	}

	static protected abstract class MapIterator<K> {
		public boolean hasNext;

		protected final ObjectFloatMap<K> map;
		protected int nextIndex, currentIndex;
		protected boolean valid = true;

		public MapIterator (ObjectFloatMap<K> map) {
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

		public void remove () {
			int i = currentIndex;
			if (i < 0) { throw new IllegalStateException("next must be called before remove."); }
			K[] keyTable = map.keyTable;
			float[] valueTable = map.valueTable;
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

			map.size--;
			if (i != currentIndex) { --nextIndex; }
			currentIndex = -1;
		}
	}

	public static class KeyIterator<K> extends MapIterator<K> implements Iterable<K>, Iterator<K> {

		public KeyIterator (ObjectFloatMap<K> map) {
			super(map);
		}

		@Override
		public Iterator<K> iterator () {
			return this;
		}

		@Override
		public boolean hasNext () {
			if (!valid) { throw new RuntimeException("#iterator() cannot be used nested."); }
			return hasNext;
		}

		@Override
		public K next () {
			if (!hasNext) { throw new NoSuchElementException(); }
			if (!valid) { throw new RuntimeException("#iterator() cannot be used nested."); }
			K key = map.keyTable[nextIndex];
			currentIndex = nextIndex;
			findNextIndex();
			return key;
		}
	}

	public static class ValueIterator<K> extends MapIterator<K> implements FloatIterator {
		public ValueIterator (ObjectFloatMap<K> map) {
			super(map);
		}

		/**
		 * Returns the next {@code float} element in the iteration.
		 *
		 * @return the next {@code float} element in the iteration
		 * @throws NoSuchElementException if the iteration has no more elements
		 */
		@Override
		public float nextFloat () {
			if (!hasNext) { throw new NoSuchElementException(); }
			if (!valid) { throw new RuntimeException("#iterator() cannot be used nested."); }
			float value = map.valueTable[nextIndex];
			currentIndex = nextIndex;
			findNextIndex();
			return value;
		}

		@Override
		public boolean hasNext () {
			if (!valid) { throw new RuntimeException("#iterator() cannot be used nested."); }
			return hasNext;
		}
	}

	public static class EntryIterator<K> extends MapIterator<K> implements Iterable<Entry<K>>, Iterator<Entry<K>> {
		protected Entry<K> entry = new Entry<>();

		public EntryIterator (ObjectFloatMap<K> map) {
			super(map);
		}

		@Override
		public Iterator<Entry<K>> iterator () {
			return this;
		}

		/**
		 * Note the same entry instance is returned each time this method is called.
		 */
		@Override
		public Entry<K> next () {
			if (!hasNext) { throw new NoSuchElementException(); }
			if (!valid) { throw new RuntimeException("#iterator() cannot be used nested."); }
			K[] keyTable = map.keyTable;
			entry.key = keyTable[nextIndex];
			entry.value = map.valueTable[nextIndex];
			currentIndex = nextIndex;
			findNextIndex();
			return entry;
		}

		@Override
		public boolean hasNext () {
			if (!valid) { throw new RuntimeException("#iterator() cannot be used nested."); }
			return hasNext;
		}
	}

	public static class Entries<K> extends AbstractSet<Entry<K>> {
		protected EntryIterator<K> iter;

		public Entries (ObjectFloatMap<K> map) {
			iter = new EntryIterator<>(map);
		}

		/**
		 * Returns an iterator over the elements contained in this collection.
		 *
		 * @return an iterator over the elements contained in this collection
		 */
		@Override
		public Iterator<Entry<K>> iterator () {
			return iter;
		}

		@Override
		public int size () {
			return iter.map.size;
		}
	}

	public static class Values<K> implements PrimitiveCollection.OfFloat {
		protected ValueIterator<K> iter;

		@Override
		public boolean add (float item) {
			throw new UnsupportedOperationException("ObjectFloatMap.Values is read-only");
		}

		@Override
		public boolean remove (float item) {
			throw new UnsupportedOperationException("ObjectFloatMap.Values is read-only");
		}

		@Override
		public boolean contains (float item) {
			return iter.map.containsValue(item);
		}

		@Override
		public void clear () {
			throw new UnsupportedOperationException("ObjectFloatMap.Values is read-only");
		}

		@Override
		public FloatIterator iterator () {
			return iter;
		}

		@Override
		public int size () {
			return iter.map.size;
		}

		public Values (ObjectFloatMap<K> map) {
			iter = new ValueIterator<>(map);
		}

	}

	public static class Keys<K> extends AbstractSet<K> {
		protected KeyIterator<K> iter;

		public Keys (ObjectFloatMap<K> map) {
			iter = new KeyIterator<>(map);
		}

		@Override
		public boolean contains (Object o) {
			return iter.map.containsKey(o);
		}

		@Override
		public Iterator<K> iterator () {
			return iter;
		}

		@Override
		public int size () {
			return iter.map.size;
		}
	}

	public float putIfAbsent (K key, float value) {
		int i = locateKey(key);
		if (i >= 0) {
			return valueTable[i];
		}
		return put(key, value);
	}

	public boolean replace (K key, float oldValue, float newValue) {
		float curValue = get(key);
		if (curValue != oldValue || !containsKey(key)) {
			return false;
		}
		put(key, newValue);
		return true;
	}

	public float replace (K key, float value) {
		int i = locateKey(key);
		if (i >= 0) {
			float oldValue = valueTable[i];
			valueTable[i] = value;
			return oldValue;
		}
		return defaultValue;
	}

	public float computeIfAbsent(K key, ToFloatFunction<? super K> mappingFunction) {
		int i = locateKey(key);
		if (i < 0) {
			float newValue = mappingFunction.applyAsFloat(key);
			put(key, newValue);
			return newValue;
		}
		else return valueTable[i];
	}

	public boolean remove(Object key, float value) {
		int i = locateKey(key);
		if (i >= 0 && valueTable[i] == value) {
			remove(key);
			return true;
		}
		return false;
	}

	public float merge(K key, float value, FloatFloatToFloatBiFunction remappingFunction) {
		int i = locateKey(key);
		float next = (i < 0) ? value : remappingFunction.applyAsFloat(valueTable[i], value);
		put(key, next);
		return next;
	}

	/**
	 * Constructs a single-entry map given one key and one value.
	 * This is mostly useful as an optimization for {@link #with(Object, Number, Object...)}
	 * when there's no "rest" of the keys or values. Like the more-argument with(), this will
	 * convert its Number value to a primitive float, regardless of which Number type was used.
	 * @param key0 the first and only key
	 * @param value0 the first and only value; will be converted to primitive float
	 * @param <K> the type of key0
	 * @return a new map containing just the entry mapping key0 to value0
	 */
	public static <K> ObjectFloatMap<K> with(K key0, Number value0) {
		ObjectFloatMap<K> map = new ObjectFloatMap<>(1);
		map.put(key0, value0.floatValue());
		return map;
	}

	/**
	 * Constructs a map given alternating keys and values.
	 * This can be useful in some code-generation scenarios, or when you want to make a
	 * map conveniently by-hand and have it populated at the start. You can also use
	 * {@link #ObjectFloatMap(Object[], float[])}, which takes all keys and then all values.
	 * This needs all keys to have the same type, because it gets a generic type from the
	 * first key parameter. All values must be some type of boxed Number, such as {@link Integer}
	 * or {@link Double}, and will be converted to primitive {@code float}s. Any keys that don't
	 * have K as their type or values that aren't {@code Number}s have that entry skipped.
	 * @param key0 the first key; will be used to determine the type of all keys
	 * @param value0 the first value; will be converted to primitive float
	 * @param rest an array or varargs of alternating K, Number, K, Number... elements
	 * @param <K> the type of keys, inferred from key0
	 * @return a new map containing the given keys and values
	 */
	@SuppressWarnings("unchecked")
	public static <K> ObjectFloatMap<K> with(K key0, Number value0, Object... rest){
		ObjectFloatMap<K> map = new ObjectFloatMap<>(1 + (rest.length >>> 1));
		map.put(key0, value0.floatValue());
		for (int i = 1; i < rest.length; i += 2) {
			try {
				map.put((K)rest[i - 1], ((Number)rest[i]).floatValue());
			}catch (ClassCastException ignored){
			}
		}
		return map;
	}
}

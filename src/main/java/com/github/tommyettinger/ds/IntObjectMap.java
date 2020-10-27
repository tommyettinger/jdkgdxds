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

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.PrimitiveIterator;
import java.util.Set;

import static com.github.tommyettinger.ds.Utilities.tableSize;

/**
 * An unordered map where the keys are unboxed ints and the values are objects. Null keys are not allowed. No allocation is
 * done except when growing the table size.
 * <p>
 * This class performs fast contains and remove (typically O(1), worst case O(n) but that is rare in practice). Add may be
 * slightly slower, depending on hash collisions. Hashcodes are rehashed to reduce collisions and the need to resize. Load factors
 * greater than 0.91 greatly increase the chances to resize to the next higher POT size.
 * <p>
 * Unordered sets and maps are not designed to provide especially fast iteration. Iteration is faster with OrderedSet and
 * OrderedMap.
 * <p>
 * You can customize most behavior of this map by extending it. {@link #place(int)} can be overridden to change how hashCodes
 * are calculated (which can be useful for types like {@link StringBuilder} that don't implement hashCode()), and
 * {@link #locateKey(int)} can be overridden to change how equality is calculated.
 * <p>
 * This implementation uses linear probing with the backward shift algorithm for removal. Hashcodes are rehashed using Fibonacci
 * hashing, instead of the more common power-of-two mask, to better distribute poor hashCodes (see <a href=
 * "https://probablydance.com/2018/06/16/fibonacci-hashing-the-optimization-that-the-world-forgot-or-a-better-alternative-to-integer-modulo/">Malte
 * Skarupke's blog post</a>). Linear probing continues to work even when all hashCodes collide, just more slowly.
 *
 * @author Nathan Sweet
 * @author Tommy Ettinger
 */
public class IntObjectMap<V> implements Iterable<IntObjectMap.Entry<V>>, Serializable {
	private static final long serialVersionUID = 0L;

	protected int size;

	protected int[] keyTable;
	protected V[] valueTable;
	protected boolean hasZeroValue;
	@Nullable protected V zeroValue;
	protected final float loadFactor;
	protected int threshold;

	protected int shift;

	/**
	 * A bitmask used to confine hashcodes to the size of the table. Must be all 1 bits in its low positions, ie a power of two
	 * minus 1.
	 */
	protected int mask;
	@Nullable protected Entries<V> entries1;
	@Nullable protected Entries<V> entries2;
	@Nullable protected Values<V> values1;
	@Nullable protected Values<V> values2;
	@Nullable protected Keys<V> keys1;
	@Nullable protected Keys<V> keys2;

	@Nullable public V defaultValue = null;

	/**
	 * Creates a new map with an initial capacity of 51 and a load factor of 0.8.
	 */
	public IntObjectMap () {
		this(51, 0.8f);
	}

	/**
	 * Creates a new map with a load factor of 0.8.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 */
	public IntObjectMap (int initialCapacity) {
		this(initialCapacity, 0.8f);
	}

	/**
	 * Creates a new map with the specified initial capacity and load factor. This map will hold initialCapacity items before
	 * growing the backing table.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 */
	public IntObjectMap (int initialCapacity, float loadFactor) {
		if (loadFactor <= 0f || loadFactor > 1f) { throw new IllegalArgumentException("loadFactor must be > 0 and <= 1: " + loadFactor); }
		this.loadFactor = loadFactor;

		int tableSize = tableSize(initialCapacity, loadFactor);
		threshold = (int)(tableSize * loadFactor);
		mask = tableSize - 1;
		shift = Long.numberOfLeadingZeros(mask);

		keyTable = new int[tableSize];
		valueTable = (V[])(new Object[tableSize]);
	}

	/**
	 * Creates a new map identical to the specified map.
	 */
	public IntObjectMap (IntObjectMap<? extends V> map) {
		this((int)(map.keyTable.length * map.loadFactor), map.loadFactor);
		System.arraycopy(map.keyTable, 0, keyTable, 0, map.keyTable.length);
		System.arraycopy(map.valueTable, 0, valueTable, 0, map.valueTable.length);
		size = map.size;
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
	protected int place (int item) {
		return (int)(item * 0x9E3779B97F4A7C15L >>> shift);
	}

	/**
	 * Returns the index of the key if already present, else {@code -1 - index} for the next empty index. This can be overridden
	 * to compare for equality differently than {@code ==}.
	 */
	private int locateKey (int key) {
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
	@Nullable public V put (int key, @Nullable V value) {
		if (key == 0) {
			V oldValue = defaultValue;
			if (hasZeroValue) { oldValue = zeroValue; } else { size++; }
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
		if (++size >= threshold) { resize(keyTable.length << 1); }
		return defaultValue;
	}

	/**
	 * Returns the old value associated with the specified key, or the given {@code defaultValue} if there was no prior value.
	 */
	@Nullable public V putOrDefault (int key, @Nullable V value, @Nullable V defaultValue) {
		if (key == 0) {
			V oldValue = defaultValue;
			if (hasZeroValue) { oldValue = zeroValue; } else { size++; }
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
		if (++size >= threshold) { resize(keyTable.length << 1); }
		return defaultValue;
	}

	public void putAll (IntObjectMap<? extends V> map) {
		ensureCapacity(map.size);
		if (map.hasZeroValue) {
			if (!hasZeroValue) { size++; }
			hasZeroValue = true;
			zeroValue = map.zeroValue;
		}
		int[] keyTable = map.keyTable;
		V[] valueTable = map.valueTable;
		int key;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			key = keyTable[i];
			if (key != 0) { put(key, valueTable[i]); }
		}
	}

	/**
	 * Skips checks for existing keys, doesn't increment size.
	 */
	private void putResize (int key, @Nullable V value) {
		int[] keyTable = this.keyTable;
		for (int i = place(key); ; i = (i + 1) & mask) {
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
	@Nullable public V get (int key) {
		if (key == 0) { return (hasZeroValue) ? zeroValue : defaultValue; }
		int i = locateKey(key);
		return i < 0 ? defaultValue : valueTable[i];
	}

	/**
	 * Returns the value for the specified key, or the default value if the key is not in the map.
	 */
	@Nullable public V getOrDefault (int key, @Nullable V defaultValue) {
		if (key == 0) { return (hasZeroValue) ? zeroValue : defaultValue; }
		int i = locateKey(key);
		return i < 0 ? defaultValue : valueTable[i];
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
		if (i < 0) { return defaultValue; }
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
	@Nullable public V getDefaultValue () {
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
			if (keyTable[i] != 0 && Objects.equals(valueTable[i], value)) { return true; }
		}
		return false;
	}

	public boolean containsKey (int key) {
		if (key == 0) { return hasZeroValue; }
		return locateKey(key) >= 0;
	}

	/**
	 * Returns the key for the specified value, or null if it is not in the map. Note this traverses the entire map and compares
	 * every value, which may be an expensive operation.
	 */
	public int findKey (@Nullable V value, int defaultKey) {
		if (hasZeroValue && Objects.equals(zeroValue, value)) { return 0; }
		V[] valueTable = this.valueTable;
		int[] keyTable = this.keyTable;
		for (int i = valueTable.length - 1; i >= 0; i--) {
			if (keyTable[i] != 0 && Objects.equals(valueTable[i], value)) { return keyTable[i]; }
		}

		return defaultKey;
	}

	/**
	 * Increases the size of the backing array to accommodate the specified number of additional items / loadFactor. Useful before
	 * adding many items to avoid multiple backing array resizes.
	 */
	public void ensureCapacity (int additionalCapacity) {
		int tableSize = tableSize(size + additionalCapacity, loadFactor);
		if (keyTable.length < tableSize) { resize(tableSize); }
	}

	final void resize (int newSize) {
		int oldCapacity = keyTable.length;
		threshold = (int)(newSize * loadFactor);
		mask = newSize - 1;
		shift = Long.numberOfLeadingZeros(mask);

		int[] oldKeyTable = keyTable;
		V[] oldValueTable = valueTable;

		keyTable = new int[newSize];
		valueTable = (V[]) new Object[newSize];

		if (size > 0) {
			for (int i = 0; i < oldCapacity; i++) {
				int key = oldKeyTable[i];
				if (key != 0) { putResize(key, oldValueTable[i]); }
			}
		}
	}

	public int hashCode () {
		int h = (hasZeroValue && zeroValue != null ? zeroValue.hashCode() ^ size : size);
		int[] keyTable = this.keyTable;
		V[] valueTable = this.valueTable;
		V v;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			int key = keyTable[i];
			if (key != 0) {
				h ^= key;
				v = valueTable[i];
				if(v != null) 
					h ^= v.hashCode();
			}
		}
		return h;
	}

	public boolean equals (Object obj) {
		if (obj == this) { return true; }
		if (!(obj instanceof IntObjectMap)) { return false; }
		IntObjectMap other = (IntObjectMap)obj;
		if (other.size != size) { return false; }
		if (other.hasZeroValue != hasZeroValue || other.zeroValue != zeroValue) { return false; }
		int[] keyTable = this.keyTable;
		V[] valueTable = this.valueTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			int key = keyTable[i];
			if (key != 0) {
				V value = valueTable[i];
				if (!Objects.equals(value, other.get(key))) { return false; }
			}
		}
		return true;
	}

	public String toString (String separator) {
		return toString(separator, false);
	}

	public String toString () {
		return toString(", ", true);
	}

	protected String toString (String separator, boolean braces) {
		if (size == 0) { return braces ? "{}" : ""; }
		StringBuilder buffer = new StringBuilder(32);
		if (braces) { buffer.append('{'); }
		if (hasZeroValue) {
			buffer.append("0=").append(zeroValue);
			if (size > 1) { buffer.append(separator); }
		}
		int[] keyTable = this.keyTable;
		V[] valueTable = this.valueTable;
		int i = keyTable.length;
		while (i-- > 0) {
			int key = keyTable[i];
			if (key == 0) { continue; }
			buffer.append(key);
			buffer.append('=');
			V value = valueTable[i];
			buffer.append(value);
			break;
		}
		while (i-- > 0) {
			int key = keyTable[i];
			if (key == 0) { continue; }
			buffer.append(separator);
			buffer.append(key);
			buffer.append('=');
			V value = valueTable[i];
			buffer.append(value);
		}
		if (braces) { buffer.append('}'); }
		return buffer.toString();
	}

	/**
	 * Reuses the iterator of the reused {@link Entries} produced by {@link #entrySet()};
	 * does not permit nested iteration. Iterate over {@link Entries#Entries(IntObjectMap)} if you
	 * need nested or multithreaded iteration. You can remove an Entry from this IntObjectMap
	 * using this Iterator.
	 *
	 * @return an {@link Iterator} over {@link Entry} key-value pairs; remove is supported.
	 */
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
	 * @return a {@link java.util.Collection} containing V values
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
		@Nullable public V getValue () {
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
		@Nullable public V setValue (@Nullable V value) {
			V old = this.value;
			this.value = value;
			return old;
		}

		@Override public boolean equals (@Nullable Object o) {
			if (this == o) { return true; }
			if (o == null || getClass() != o.getClass()) { return false; }

			Entry entry = (Entry)o;

			if (key != (entry.key)) { return false; }
			return Objects.equals(value, entry.value);
		}

		@Override public int hashCode () {
			return (value == null ? key : key ^ value.hashCode());
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
			if (map.hasZeroValue) { hasNext = true; } else { findNextIndex(); }
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
			} else if (i < 0) {
				throw new IllegalStateException("next must be called before remove.");
			} else {
				int[] keyTable = map.keyTable;
				int mask = map.mask;
				int next = i + 1 & mask;
				int key;
				while ((key = keyTable[next]) != 0) {
					int placement = map.place(key);
					if ((next - placement & mask) > (i - placement & mask)) {
						keyTable[i] = key;
						i = next;
					}
					next = next + 1 & mask;
				}
				keyTable[i] = 0;
				if (i != currentIndex) { --nextIndex; }
			}
			currentIndex = INDEX_ILLEGAL;
			map.size--;
		}

	}

	public static class KeyIterator<V> extends MapIterator<V> implements PrimitiveIterator.OfInt {
		static private final int INDEX_ILLEGAL = -2, INDEX_ZERO = -1;

		public boolean hasNext;

		int nextIndex, currentIndex;
		boolean valid = true;

		public KeyIterator (IntObjectMap<V> map) {
			super(map);
		}

		@Override public int nextInt () {
			if (!hasNext) { throw new NoSuchElementException(); }
			if (!valid) { throw new RuntimeException("#iterator() cannot be used nested."); }
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
			while (hasNext) { list.add(next()); }
			return list;
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
			if (!hasNext) { throw new NoSuchElementException(); }
			if (!valid) { throw new RuntimeException("#iterator() cannot be used nested."); }
			V value = nextIndex == INDEX_ZERO ? map.zeroValue : map.valueTable[nextIndex];
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

	public static class EntryIterator<V> extends MapIterator<V> implements Iterable<Entry<V>>, Iterator<Entry<V>> {
		protected Entry<V> entry = new Entry<>();

		public EntryIterator (IntObjectMap<V> map) {
			super(map);
		}

		public Iterator<Entry<V>> iterator () {
			return this;
		}

		/**
		 * Note the same entry instance is returned each time this method is called.
		 */
		@Override 
		public Entry<V> next () {
			if (!hasNext) { throw new NoSuchElementException(); }
			if (!valid) { throw new RuntimeException("#iterator() cannot be used nested."); }
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
			if (!valid) { throw new RuntimeException("#iterator() cannot be used nested."); }
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
		@Override public Iterator<Entry<V>> iterator () {
			return iter;
		}

		@Override public int size () {
			return iter.map.size;
		}
	}

	public static class Values<V> extends AbstractCollection<V> {
		protected ValueIterator<V> iter;

		@Override public boolean add (@Nullable V item) {
			throw new UnsupportedOperationException("IntObjectMap.Values is read-only");
		}

		@Override public boolean remove (@Nullable Object item) {
			throw new UnsupportedOperationException("IntObjectMap.Values is read-only");
		}

		@Override public boolean contains (@Nullable Object item) {
			return iter.map.containsValue(item);
		}

		@Override public void clear () {
			throw new UnsupportedOperationException("IntObjectMap.Values is read-only");
		}

		/**
		 * Returns an iterator over the elements contained in this collection.
		 *
		 * @return an iterator over the elements contained in this collection
		 */
		public ValueIterator<V> iterator () {
			return iter;
		}

		public int size () {
			return iter.map.size;
		}

		public Values (IntObjectMap<V> map) {
			iter = new ValueIterator<>(map);
		}

	}

	public static class Keys<V> implements PrimitiveCollection.OfInt {
		protected KeyIterator<V> iter;

		public Keys (IntObjectMap<V> map) {
			iter = new KeyIterator<>(map);
		}

		@Override public boolean add (int item) {
			throw new UnsupportedOperationException("IntObjectMap.Keys is read-only");
		}

		@Override public boolean remove (int item) {
			throw new UnsupportedOperationException("IntObjectMap.Keys is read-only");
		}

		@Override public boolean contains (int item) {
			return iter.map.containsKey(item);
		}

		@Override public PrimitiveIterator.OfInt iterator () {
			return iter;
		}

		@Override public void clear () {
			throw new UnsupportedOperationException("IntObjectMap.Keys is read-only");
		}

		@Override public int size () {
			return iter.map.size;
		}
	}
}

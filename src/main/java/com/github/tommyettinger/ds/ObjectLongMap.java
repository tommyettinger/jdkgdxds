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
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.Set;

import static com.github.tommyettinger.ds.Utilities.tableSize;

/**
 * An unordered map where the keys and values are objects. Null keys are not allowed. No allocation is done except when growing
 * the table size.
 * <p>
 * This class performs fast contains and remove (typically O(1), worst case O(n) but that is rare in practice). Add may be
 * slightly slower, depending on hash collisions. Hashcodes are rehashed to reduce collisions and the need to resize. Load factors
 * greater than 0.91 greatly increase the chances to resize to the next higher POT size.
 * <p>
 * Unordered sets and maps are not designed to provide especially fast iteration. Iteration is faster with OrderedSet and
 * OrderedMap.
 * <p>
 * This implementation uses linear probing with the backward shift algorithm for removal.
 * Linear probing continues to work even when all hashCodes collide, just more slowly.
 *
 * @author Nathan Sweet
 * @author Tommy Ettinger
 */
public class ObjectLongMap<K> implements Iterable<ObjectLongMap.Entry<K>>, Serializable {
	private static final long serialVersionUID = 0L;

	public int size;

	protected K[] keyTable;
	protected long[] valueTable;

	protected float loadFactor;
	protected int threshold;

	protected int shift;

	/**
	 * A bitmask used to confine hashcodes to the size of the table. Must be all 1 bits in its low positions, ie a power of two
	 * minus 1.
	 */
	protected int mask;
	protected Entries<K> entries1, entries2;
	protected Values values1, values2;
	protected Keys<K> keys1, keys2;
	
	public long defaultValue = 0L;

	/**
	 * Creates a new map with an initial capacity of 51 and a load factor of 0.8.
	 */
	public ObjectLongMap () {
		this(51, 0.8f);
	}

	/**
	 * Creates a new map with a load factor of 0.8.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 */
	public ObjectLongMap (int initialCapacity) {
		this(initialCapacity, 0.8f);
	}

	/**
	 * Creates a new map with the specified initial capacity and load factor. This map will hold initialCapacity items before
	 * growing the backing table.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 */
	public ObjectLongMap (int initialCapacity, float loadFactor) {
		if (loadFactor <= 0f || loadFactor > 1f)
			throw new IllegalArgumentException("loadFactor must be > 0 and <= 1: " + loadFactor);
		this.loadFactor = loadFactor;

		int tableSize = tableSize(initialCapacity, loadFactor);
		threshold = (int)(tableSize * loadFactor);
		mask = tableSize - 1;
		shift = Integer.numberOfLeadingZeros(mask);

		keyTable = (K[])new Object[tableSize];
		valueTable = new long[tableSize];
	}

	/**
	 * Creates a new map identical to the specified map.
	 */
	public ObjectLongMap (ObjectLongMap<? extends K> map) {
		this((int)(map.keyTable.length * map.loadFactor), map.loadFactor);
		System.arraycopy(map.keyTable, 0, keyTable, 0, map.keyTable.length);
		System.arraycopy(map.valueTable, 0, valueTable, 0, map.valueTable.length);
		size = map.size;
	}

	/**
	 * Returns an index &gt;= 0 and &lt;= {@link #mask} for the specified {@code item}.
	 *
	 * @param item a non-null Object; its hashCode() method should be used by most implementations.
	 */
	protected int place (Object item) {
		final int h = item.hashCode() * 0x9E377;
		return (h ^ h >>> shift) & mask;
	}

	/**
	 * Returns the index of the key if already present, else {@code ~index} for the next empty index. This can be overridden
	 * to compare for equality differently than {@link Object#equals(Object)}.
	 *
	 * @param key a non-null Object that should probably be a K
	 */
	protected int locateKey (Object key) {
		K[] keyTable = this.keyTable;
		for (int i = place(key); ; i = i + 1 & mask) {
			K other = keyTable[i];
			if (other == null)
				return ~i; // Empty space is available.
			if (other.equals(key))
				return i; // Same key was found.
		}
	}

	/**
	 * Returns the old value associated with the specified key, or null.
	 */
	public long put (K key, long value) {
		int i = locateKey(key);
		if (i >= 0) { // Existing key was found.
			long oldValue = valueTable[i];
			valueTable[i] = value;
			return oldValue;
		}
		i = ~i; // Empty space was found.
		keyTable[i] = key;
		valueTable[i] = value;
		if (++size >= threshold)
			resize(keyTable.length << 1);
		return defaultValue;
	}

	public void putAll (ObjectLongMap<? extends K> map) {
		ensureCapacity(map.size);
		K[] keyTable = map.keyTable;
		long[] valueTable = map.valueTable;
		K key;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			key = keyTable[i];
			if (key != null)
				put(key, valueTable[i]);
		}
	}

	/**
	 * Skips checks for existing keys, doesn't increment size.
	 */
	private void putResize (K key, long value) {
		K[] keyTable = this.keyTable;
		for (int i = place(key); ; i = (i + 1) & mask) {
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
	public long get (Object key) {
		int i = locateKey(key);
		return i < 0 ? defaultValue : valueTable[i];
	}

	/**
	 * Returns the value for the specified key, or the default value if the key is not in the map.
	 */
	public long getOrDefault (Object key, long defaultValue) {
		int i = locateKey(key);
		return i < 0 ? defaultValue : valueTable[i];
	}

	public long remove (Object key) {
		int i = locateKey(key);
		if (i < 0)
			return defaultValue;
		K[] keyTable = this.keyTable;
		long[] valueTable = this.valueTable;
		long oldValue = valueTable[i];
		int mask = this.mask, next = i + 1 & mask;
		while ((key = keyTable[next]) != null) {
			int placement = place(key);
			if ((next - placement & mask) > (i - placement & mask)) {
				keyTable[i] = (K)key;
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
	 * map contains more than <tt>Integer.MAX_VALUE</tt> elements, returns
	 * <tt>Integer.MAX_VALUE</tt>.
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
	 * Gets the default value, a {@code long} which is returned by {@link #get(Object)} if the key is not found.
	 * If not changed, the default value is 0.
	 * @return the current default value
	 */
	public long getDefaultValue () {
		return defaultValue;
	}

	/**
	 * Sets the default value, a {@code long} which is returned by {@link #get(Object)} if the key is not found.
	 * If not changed, the default value is 0. Note that {@link #getOrDefault(Object, long)} is also available,
	 * which allows specifying a "not-found" value per-call.
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
		if (maximumCapacity < 0)
			throw new IllegalArgumentException("maximumCapacity must be >= 0: " + maximumCapacity);
		int tableSize = tableSize(maximumCapacity, loadFactor);
		if (keyTable.length > tableSize)
			resize(tableSize);
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
		if (size == 0)
			return;
		size = 0;
		Arrays.fill(keyTable, null);
	}

	/**
	 * Returns true if the specified value is in the map. Note this traverses the entire map and compares every value, which may
	 * be an expensive operation.
	 *
	 */
	public boolean containsValue (long value) {
		long[] valueTable = this.valueTable;
		K[] keyTable = this.keyTable;
		for (int i = valueTable.length - 1; i >= 0; i--) {
			if (keyTable[i] != null && valueTable[i] == value)
				return true;
		}
		return false;
	}

	public boolean containsKey (Object key) {
		return locateKey(key) >= 0;
	}
	
	/**
	 * Returns the key for the specified value, or null if it is not in the map. Note this traverses the entire map and compares
	 * every value, which may be an expensive operation.
	 * 
	 */
	public @Nullable K findKey (long value) {
		long[] valueTable = this.valueTable;
		K[] keyTable = this.keyTable;
		for (int i = valueTable.length - 1; i >= 0; i--) {
			if (keyTable[i] != null && valueTable[i] == value)
				return keyTable[i];
		}

		return null;
	}

	/**
	 * Increases the size of the backing array to accommodate the specified number of additional items / loadFactor. Useful before
	 * adding many items to avoid multiple backing array resizes.
	 */
	public void ensureCapacity (int additionalCapacity) {
		int tableSize = tableSize(size + additionalCapacity, loadFactor);
		if (keyTable.length < tableSize)
			resize(tableSize);
	}

	final void resize (int newSize) {
		int oldCapacity = keyTable.length;
		threshold = (int)(newSize * loadFactor);
		mask = newSize - 1;
		shift = Integer.numberOfLeadingZeros(mask);

		K[] oldKeyTable = keyTable;
		long[] oldValueTable = valueTable;

		keyTable = (K[])new Object[newSize];
		valueTable = new long[newSize];

		if (size > 0) {
			for (int i = 0; i < oldCapacity; i++) {
				K key = oldKeyTable[i];
				if (key != null)
					putResize(key, oldValueTable[i]);
			}
		}
	}

	public int hashCode () {
		long h = size;
		K[] keyTable = this.keyTable;
		long[] valueTable = this.valueTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			K key = keyTable[i];
			if (key != null) {
				h += key.hashCode() * 0x9E3779B97F4A7C15L;
				h += valueTable[i];
			}
		}
		return (int)(h ^ h >>> 32);
	}

	public boolean equals (Object obj) {
		if (obj == this)
			return true;
		if (!(obj instanceof ObjectLongMap))
			return false;
		ObjectLongMap other = (ObjectLongMap)obj;
		if (other.size != size)
			return false;
		K[] keyTable = this.keyTable;
		long[] valueTable = this.valueTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			K key = keyTable[i];
			if (key != null) {
				long value = valueTable[i];					
				if (value != other.get(key))
						return false;
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
		if (size == 0)
			return braces ? "{}" : "";
		StringBuilder buffer = new StringBuilder(32);
		if (braces)
			buffer.append('{');
		K[] keyTable = this.keyTable;
		long[] valueTable = this.valueTable;
		int i = keyTable.length;
		while (i-- > 0) {
			K key = keyTable[i];
			if (key == null)
				continue;
			buffer.append(key == this ? "(this)" : key);
			buffer.append('=');
			long value = valueTable[i];
			buffer.append(value);
			break;
		}
		while (i-- > 0) {
			K key = keyTable[i];
			if (key == null)
				continue;
			buffer.append(separator);
			buffer.append(key == this ? "(this)" : key);
			buffer.append('=');
			long value = valueTable[i];
			buffer.append(value);
		}
		if (braces)
			buffer.append('}');
		return buffer.toString();
	}

	/**
	 * Reuses the iterator of the reused {@link Entries} produced by {@link #entrySet()};
	 * does not permit nested iteration. Iterate over {@link Entries#Entries(ObjectLongMap)} if you
	 * need nested or multithreaded iteration. You can remove an Entry from this ObjectMap
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
	 * the iterator's own <tt>remove</tt> operation), the results of
	 * the iteration are undefined.  The set supports element removal,
	 * which removes the corresponding mapping from the map, via the
	 * <tt>Iterator.remove</tt>, <tt>Set.remove</tt>,
	 * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt>
	 * operations.  It does not support the <tt>add</tt> or <tt>addAll</tt>
	 * operations.
	 * 
	 * <p>Note that the same Collection instance is returned each time this
	 * method is called. Use the {@link Keys} constructor for nested or
	 * multithreaded iteration.
	 *
	 * @return a set view of the keys contained in this map
	 */
	public Keys<K> keySet () { 
		if (keys1 == null) {
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
	 * @return a {@link Collection} of long values
	 */
	public Values<K> values () {
		if (values1 == null) {
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
		if (entries1 == null) {
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

	static public class Entry<K> {
		public K key;
		public long value;

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
			return key;
		}

		/**
		 * Returns the value corresponding to this entry.  If the mapping
		 * has been removed from the backing map (by the iterator's
		 * <tt>remove</tt> operation), the results of this call are undefined.
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
		 * removed from the map (by the iterator's <tt>remove</tt> operation).
		 *
		 * @param value new value to be stored in this entry
		 * @return old value corresponding to the entry
		 * @throws UnsupportedOperationException if the <tt>put</tt> operation
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
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;

			Entry<?> entry = (Entry<?>)o;

			if (!key.equals(entry.key))
				return false;
			return value == entry.value;
		}

		@Override
		public int hashCode () {
			long result = key.hashCode() * 31L + value;
			return (int)(result ^ result >>> 32);
		}
	}

	static protected abstract class MapIterator<K> {
		public boolean hasNext;

		protected final ObjectLongMap<K> map;
		protected int nextIndex, currentIndex;
		protected boolean valid = true;

		public MapIterator (ObjectLongMap<K> map) {
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
			if (i < 0)
				throw new IllegalStateException("next must be called before remove.");
			K[] keyTable = map.keyTable;
			long[] valueTable = map.valueTable;
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
			if (i != currentIndex)
				--nextIndex;
			currentIndex = -1;
		}
	}
	
	static public class KeyIterator<K> extends MapIterator<K> implements Iterable<K>, Iterator<K> {

		public KeyIterator(ObjectLongMap<K> map) {
			super(map);
		}
		@Override
		public Iterator<K> iterator () {
			return this;
		}

		@Override
		public boolean hasNext () {
			if (!valid)
				throw new RuntimeException("#iterator() cannot be used nested.");
			return hasNext;
		}

		@Override
		public K next () {
			if (!hasNext)
				throw new NoSuchElementException();
			if (!valid)
				throw new RuntimeException("#iterator() cannot be used nested.");
			K key = map.keyTable[nextIndex];
			currentIndex = nextIndex;
			findNextIndex();
			return key;
		}
	}
	
	static public class ValueIterator<K> extends MapIterator<K> implements PrimitiveIterator.OfLong {
		public ValueIterator(ObjectLongMap<K> map) {
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
			if (!hasNext)
				throw new NoSuchElementException();
			if (!valid)
				throw new RuntimeException("#iterator() cannot be used nested.");
			long value = map.valueTable[nextIndex];
			currentIndex = nextIndex;
			findNextIndex();
			return value;
		}

		@Override
		public boolean hasNext () {
			if (!valid)
				throw new RuntimeException("#iterator() cannot be used nested.");
			return hasNext;
		}
	}
	
	static public class EntryIterator<K> extends MapIterator<K> implements Iterable<Entry<K>>, Iterator<Entry<K>> {
		protected Entry<K> entry = new Entry<K>();

		public EntryIterator(ObjectLongMap<K> map) {
			super(map);
		}
		public Iterator<Entry<K>> iterator () {
			return this;
		}

		/** Note the same entry instance is returned each time this method is called. */
		@Override
		public Entry<K> next () {
			if (!hasNext)
				throw new NoSuchElementException();
			if (!valid)
				throw new RuntimeException("#iterator() cannot be used nested.");
			K[] keyTable = map.keyTable;
			entry.key = keyTable[nextIndex];
			entry.value = map.valueTable[nextIndex];
			currentIndex = nextIndex;
			findNextIndex();
			return entry;
		}

		@Override
		public boolean hasNext () {
			if (!valid)
				throw new RuntimeException("#iterator() cannot be used nested.");
			return hasNext;
		}
	}

	static public class Entries<K> extends AbstractSet<Entry<K>> {
		protected EntryIterator<K> iter;

		protected Entries () {
		}

		public Entries (ObjectLongMap<K> map) {
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

	static public class Values<K> {
		protected ValueIterator iter;

		/**
		 * Returns an iterator over the elements contained in this collection.
		 *
		 * @return an iterator over the elements contained in this collection
		 */
		public PrimitiveIterator.OfLong iterator () {
			return iter;
		}

		public int size () {
			return iter.map.size;
		}

		protected Values () {
		}

		public Values (ObjectLongMap<K> map) {
			iter = new ValueIterator(map);
		}

	}

	static public class Keys<K> extends AbstractSet<K> {
		protected KeyIterator<K> iter;

		protected Keys () {
		}

		public Keys (ObjectLongMap<K> map) {
			iter = new KeyIterator<>(map);
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
	}
}

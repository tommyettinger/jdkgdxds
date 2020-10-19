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
 * Unordered sets and maps are not designed to provide especially fast iteration. Iteration is faster with OrderedSet and
 * OrderedMap.
 * <p>
 * This implementation uses linear probing with the backward shift algorithm for removal. Hashcodes are rehashed using Fibonacci
 * hashing, instead of the more common power-of-two mask, to better distribute poor hashCodes (see <a href=
 * "https://probablydance.com/2018/06/16/fibonacci-hashing-the-optimization-that-the-world-forgot-or-a-better-alternative-to-integer-modulo/">Malte
 * Skarupke's blog post</a>). Linear probing continues to work even when all hashCodes collide, just more slowly.
 *
 * @author Nathan Sweet
 * @author Tommy Ettinger
 */
public class ObjectMap<K, V> implements Map<K, V>, Iterable<Map.Entry<K, V>>, Serializable {
	private static final long serialVersionUID = 0L;

	public int size;

	protected K[] keyTable;
	protected V[] valueTable;

	protected float loadFactor;
	protected int threshold;

	protected int shift;

	/**
	 * A bitmask used to confine hashcodes to the size of the table. Must be all 1 bits in its low positions, ie a power of two
	 * minus 1.
	 */
	protected int mask;
	protected @Nullable Entries<K, V> entries1;
	protected @Nullable Entries<K, V> entries2;
	protected @Nullable Values<V> values1;
	protected @Nullable Values<V> values2;
	protected @Nullable Keys<K> keys1;
	protected @Nullable Keys<K> keys2;

	/**
	 * Creates a new map with an initial capacity of 51 and a load factor of 0.8.
	 */
	public ObjectMap () {
		this(51, 0.8f);
	}

	/**
	 * Creates a new map with a load factor of 0.8.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 */
	public ObjectMap (int initialCapacity) {
		this(initialCapacity, 0.8f);
	}

	/**
	 * Creates a new map with the specified initial capacity and load factor. This map will hold initialCapacity items before
	 * growing the backing table.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 */
	public ObjectMap (int initialCapacity, float loadFactor) {
		if (loadFactor <= 0f || loadFactor > 1f)
			throw new IllegalArgumentException("loadFactor must be > 0 and <= 1: " + loadFactor);
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
	 */
	public ObjectMap (ObjectMap<? extends K, ? extends V> map) { 
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
	 */
	public ObjectMap (Map<? extends K, ? extends V> map) {
		this(map.size());
		for (K k : map.keySet()) {
			put(k, map.get(k));
		}
	}

	/**
	 * Returns an index &gt;= 0 and &lt;= {@link #mask} for the specified {@code item}.
	 *
	 * @param item a non-null Object; its hashCode() method should be used by most implementations.
	 */
	protected int place (Object item) {
		return (int)(item.hashCode() * 0x9E3779B97F4A7C15L >>> shift);
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
	@Override
	public @Nullable V put (K key, @Nullable V value) {
		int i = locateKey(key);
		if (i >= 0) { // Existing key was found.
			V oldValue = valueTable[i];
			valueTable[i] = value;
			return oldValue;
		}
		i = ~i; // Empty space was found.
		keyTable[i] = key;
		valueTable[i] = value;
		if (++size >= threshold)
			resize(keyTable.length << 1);
		return null;
	}

	public void putAll (ObjectMap<? extends K, ? extends V> map) {
		ensureCapacity(map.size);
		K[] keyTable = map.keyTable;
		V[] valueTable = map.valueTable;
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
	private void putResize (K key, @Nullable V value) {
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
	 * Returns the value for the specified key, or null if the key is not in the map.
	 * Note that null is also a valid value that can be assigned to a legitimate key, so
	 * checking that the result of this method is null does not guarantee that the
	 * {@code key} is not present.
	 * 
	 * @param key a non-null Object that should almost always be a {@code K} (or an instance of a subclass of {@code K})
	 */
	@Override
	public @Nullable V get (Object key) {
		int i = locateKey(key);
		return i < 0 ? null : valueTable[i];
	}

	/**
	 * Returns the value for the specified key, or the given default value if the key is not in the map.
	 */
	@Override
	public @Nullable V getOrDefault (Object key, @Nullable V defaultValue) {
		int i = locateKey(key);
		return i < 0 ? defaultValue : valueTable[i];
	}

	@Override
	public @Nullable V remove (Object key) {
		int i = locateKey(key);
		if (i < 0)
			return null;
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
		for (Map.Entry<? extends K, ? extends V> kv : m.entrySet())
			put(kv.getKey(), kv.getValue());
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

	@Override
	public void clear () {
		if (size == 0)
			return;
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
			for (int i = valueTable.length - 1; i >= 0; i--)
				if (keyTable[i] != null && valueTable[i] == null)
					return true;
		} else if (identity) {
			for (int i = valueTable.length - 1; i >= 0; i--)
				if (valueTable[i] == value)
					return true;
		} else {
			for (int i = valueTable.length - 1; i >= 0; i--)
				if (value.equals(valueTable[i]))
					return true;
		}
		return false;
	}

	@Override
	public boolean containsKey (Object key) {
		return locateKey(key) >= 0;
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
	public @Nullable K findKey (@Nullable Object value, boolean identity) {
		V[] valueTable = this.valueTable;
		if (value == null) {
			K[] keyTable = this.keyTable;
			for (int i = valueTable.length - 1; i >= 0; i--)
				if (keyTable[i] != null && valueTable[i] == null)
					return keyTable[i];
		} else if (identity) {
			for (int i = valueTable.length - 1; i >= 0; i--)
				if (valueTable[i] == value)
					return keyTable[i];
		} else {
			for (int i = valueTable.length - 1; i >= 0; i--)
				if (value.equals(valueTable[i]))
					return keyTable[i];
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
		if (keyTable.length < tableSize)
			resize(tableSize);
	}

	protected void resize (int newSize) {
		int oldCapacity = keyTable.length;
		threshold = (int)(newSize * loadFactor);
		mask = newSize - 1;
		shift = Long.numberOfLeadingZeros(mask);

		K[] oldKeyTable = keyTable;
		V[] oldValueTable = valueTable;

		keyTable = (K[])new Object[newSize];
		valueTable = (V[])new Object[newSize];

		if (size > 0) {
			for (int i = 0; i < oldCapacity; i++) {
				K key = oldKeyTable[i];
				if (key != null)
					putResize(key, oldValueTable[i]);
			}
		}
	}

	public int hashCode () {
		int h = size;
		K[] keyTable = this.keyTable;
		V[] valueTable = this.valueTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			K key = keyTable[i];
			if (key != null) {
				h += key.hashCode();
				V value = valueTable[i];
				if (value != null)
					h += value.hashCode();
			}
		}
		return h;
	}

	public boolean equals (Object obj) {
		if (obj == this)
			return true;
		if (!(obj instanceof ObjectMap))
			return false;
		ObjectMap other = (ObjectMap)obj;
		if (other.size != size)
			return false;
		K[] keyTable = this.keyTable;
		V[] valueTable = this.valueTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			K key = keyTable[i];
			if (key != null) {
				V value = valueTable[i];
				if (value == null) {
					if (other.getOrDefault(key, neverIdentical) != null)
						return false;
				} else {
					if (!value.equals(other.get(key)))
						return false;
				}
			}
		}
		return true;
	}

	/**
	 * Uses == for comparison of each value.
	 */
	public boolean equalsIdentity (@Nullable Object obj) {
		if (obj == this)
			return true;
		if (!(obj instanceof ObjectMap))
			return false;
		ObjectMap other = (ObjectMap)obj;
		if (other.size != size)
			return false;
		K[] keyTable = this.keyTable;
		V[] valueTable = this.valueTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			K key = keyTable[i];
			if (key != null && valueTable[i] != other.getOrDefault(key, neverIdentical))
				return false;
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
		V[] valueTable = this.valueTable;
		int i = keyTable.length;
		while (i-- > 0) {
			K key = keyTable[i];
			if (key == null)
				continue;
			buffer.append(key == this ? "(this)" : key);
			buffer.append('=');
			V value = valueTable[i];
			buffer.append(value == this ? "(this)" : value);
			break;
		}
		while (i-- > 0) {
			K key = keyTable[i];
			if (key == null)
				continue;
			buffer.append(separator);
			buffer.append(key == this ? "(this)" : key);
			buffer.append('=');
			V value = valueTable[i];
			buffer.append(value == this ? "(this)" : value);
		}
		if (braces)
			buffer.append('}');
		return buffer.toString();
	}

	/**
	 * Reuses the iterator of the reused {@link Entries} produced by {@link #entrySet()};
	 * does not permit nested iteration. Iterate over {@link Entries#Entries(ObjectMap)} if you
	 * need nested or multithreaded iteration. You can remove an Entry from this ObjectMap
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
	 * @return a {@link Collection} of V values
	 */
	@Override
	public Values<V> values () {
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

	static public class Entry<K, V> implements Map.Entry<K, V> {
		public @Nullable K key;
		public @Nullable V value;

		public @Nullable String toString () {
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
		public @Nullable V getValue () {
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
		public @Nullable V setValue (V value) {
			V old = this.value;
			this.value = value;
			return old;
		}

		@Override
		public boolean equals (@Nullable Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;

			Entry<?, ?> entry = (Entry<?, ?>)o;

			if (key != null ? !key.equals(entry.key) : entry.key != null)
				return false;
			return value != null ? value.equals(entry.value) : entry.value == null;
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

		protected final ObjectMap<K, V> map;
		protected int nextIndex, currentIndex;
		protected boolean valid = true;

		public MapIterator (ObjectMap<K, V> map) {
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
			if (i < 0)
				throw new IllegalStateException("next must be called before remove.");
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
			if (i != currentIndex)
				--nextIndex;
			currentIndex = -1;
		}
	}

	static public class Entries<K, V> extends AbstractSet<Map.Entry<K, V>> {
		protected Entry<K, V> entry = new Entry<>();
		protected MapIterator<K, V, Map.Entry<K, V>> iter;
		
		public Entries (ObjectMap<K, V> map) {
			initialize(map);
		}
		
		protected void initialize(ObjectMap<K, V> map){
			iter = new MapIterator<K, V, Map.Entry<K, V>>(map) {
				@Override
				public Iterator<Map.Entry<K, V>> iterator () {
					return this;
				}

				/** Note the same entry instance is returned each time this method is called. */
				@Override
				public Map.Entry<K, V> next () {
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
			};
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

	static public class Values<V> extends AbstractCollection<V> {
		protected MapIterator<Object, V, V> iter;

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

		public Values (ObjectMap<?, V> map) {
			initialize(map);

		}
		protected void initialize(ObjectMap<?, V> map){
			iter = new MapIterator<Object, V, V>((ObjectMap<Object, V>)map) {
				@Override
				public Iterator<V> iterator () {
					return this;
				}

				@Override
				public boolean hasNext () {
					if (!valid)
						throw new RuntimeException("#iterator() cannot be used nested.");
					return hasNext;
				}

				@Override
				public V next () {
					if (!hasNext)
						throw new NoSuchElementException();
					if (!valid)
						throw new RuntimeException("#iterator() cannot be used nested.");
					V value = map.valueTable[nextIndex];
					currentIndex = nextIndex;
					findNextIndex();
					return value;
				}
			};
		}

	}

	static public class Keys<K> extends AbstractSet<K> {
		protected MapIterator<K, Object, K> iter;

		public Keys (ObjectMap<K, ?> map) {
			initialize(map);
		}
		protected void initialize(ObjectMap<K, ?> map){
			iter = new MapIterator<K, Object, K>((ObjectMap<K, Object>)map) {
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
			};
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

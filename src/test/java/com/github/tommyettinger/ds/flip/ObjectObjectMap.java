/*
 * Copyright (c) 2024 See AUTHORS file.
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

package com.github.tommyettinger.ds.flip;


import com.github.tommyettinger.digital.BitConversion;
import com.github.tommyettinger.ds.Utilities;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

/**
 * A {@link Map} that starts using cuckoo hashing and can flip its algorithm
 * internally to use linear probing, if warranted.
 * This implementation provides all the optional map operations, and permits
 * {@code null} values, but not {@code null} keys. This class makes no
 * guarantees as to the order of the map; in particular, it does not guarantee
 * that the order will remain constant over time. Cuckoo hashing can be extremely
 * fast (in most cases) or unusably slow (in the worst case), so being able to
 * flip the implementation to the more-reliable linear probing is a major benefit.
 * <br>
 * Both for cuckoo hashing and linear probing, this uses a family of hash functions
 * that are all similar to Fibonacci hashing. Cuckoo hashing uses two different
 * multipliers, one used to address "table 1" and the other used to address
 * "table 2" -- the names are misnomers because they really are one array, with
 * table 1 using odd indices and table 2 using even indices. Linear probing uses
 * a multiplier from the same set as the "table 1" multipliers, but can address
 * both even and odd indices in the array.
 * <br>
 * Cuckoo hashing can have severe problems if given multiple different items with
 * the same hashCode, for all bits. There are probably other cases that give it
 * trouble in the same way. This map switches to linear probing to resolve
 * collisions if it would be required to flip in a purely-cuckoo-hashed map.
 * This flip can only happen once per map, and makes it use linear probing for the
 * rest of its lifetime. Linear probing has much better worst-case performance on
 * {@link #put(Object, Object)}, and flipping should only occur in that worst case.
 * The map won't return to cuckoo hashing because if the worst-case can occur at
 * all, this should be using linear probing, and if it flipped once, it could really
 * need linear probing when the same problematic combination of keys is inserted.
 * <br>
 * Iterating over the collection requires a time proportional to the capacity
 * of the map. The default capacity of an empty map is 16. The map will resize
 * its internal capacity whenever it grows past the load factor specified for the
 * current instance. The default load factor for this map is {@code 0.45}.
 * Beware that this implementation can only guarantee non-amortized O(1) on
 * {@link #get(Object)} iff the load factor is relatively low (generally below 0.60).
 * For more details, it's helpful to read
 * <a href="http://www.it-c.dk/people/pagh/papers/cuckoo-jour.pdf">the original Cuckoo Hash Map paper</a>.
 * <br>
 * Note that this implementation is not synchronized and not thread safe. If you need
 * thread safety, you'll need to implement your own locking around the map or wrap
 * the instance around a call to {@link Collections#synchronizedMap(Map)}.
 * <br>
 * This is derived from <a href="https://github.com/ivgiuliani/cuckoohash">this Github repo</a>
 * by Ivan Giuliani, at least for the cuckoo hashing part. If you're looking through that repo,
 * it may help to know that where it calls {@code rehash()}, this code calls {@link #flip()}.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 */
public class ObjectObjectMap<K, V> implements Map<K, V> {

	protected static final int DEFAULT_START_SIZE = 16;
	protected static final float DEFAULT_LOAD_FACTOR = 0.45f;

	/**
	 * While cuckoo hashing, this is 4 or greater, and is used to determine how many times keys can be relocated before
	 * this decides to call {@link #flip()}. After this flips to using linear probing, this is always 0, and whether this
	 * is 0 or not determines the algorithm this class uses.
	 */
	protected int flipThreshold;

	/**
	 * Simply the size of the {@link #keyTable} times the {@link #loadFactor}, floored. Stored to avoid recalculating;
	 * this only changes upon {@link #resize(int)}.
	 */
	protected int loadThreshold;

	/**
	 * Between 0f (exclusive) and 1f (inclusive, if you're careful), this determines how full the backing tables
	 * can get before this increases their size. Larger values use less memory but make the data structure slower.
	 * Beware that this should be 0.6 or less while this uses cuckoo hashing; without that guarantee, many
	 * operations will slow down significantly. While this uses linear probing, this automatically becomes a
	 * higher value (closer to 1).
	 */
	protected float loadFactor;
	/**
	 * A bitmask used to confine hash codes to the size of the table. Must be all 1-bits in its low positions, i.e. a
	 * power of two minus 1.
	 */
	protected int mask;

	/**
	 * A shift amount used to reduce a 64-bit value to a specific smaller range (less than 32 bits). The shift is always
	 * greater than 32 and less than 64.
	 */
	protected int shift;

	/**
	 * Used both by cuckoo hashing and linear probing; drawn from {@link Utilities#GOOD_MULTIPLIERS}.
	 */
	protected long hashMultiplier1;

	/**
	 * Used by cuckoo hashing; drawn from {@link Utilities#GOOD_MULTIPLIERS}.
	 */
	protected long hashMultiplier2;

	/**
	 * How many keys are in this map.
	 */
	protected int size;

	protected K[] keyTable;

	protected V[] valueTable;

	@Nullable
	protected V defaultValue = null;

	/**
	 * Only used by {@link #putSafe(Object, Object)} to transfer a value along with the key it returns.
	 * Can typically be ignored.
	 */
	protected transient V displacedValue;

	/**
	 * Holds a cached entrySet().
	 */
	@Nullable
	protected transient Set<Map.Entry<K,V>> entrySet;

	@Nullable
	protected transient Set<K> keySet;

	@Nullable
	protected transient Collection<V> values;

	/**
	 * Constructs an empty {@code ObjectObjectMap} with the default initial capacity (16)
	 * and the default load factor of {@code 0.45}.
	 */
	public ObjectObjectMap () {
		this(DEFAULT_START_SIZE, DEFAULT_LOAD_FACTOR);
	}

	/**
	 * Constructs an empty {@code ObjectObjectMap} with the specified initial capacity
	 * and the default load factor of {@code 0.45}.
	 * The given capacity will be rounded to the nearest power of two.
	 *
	 * @param initialCapacity the initial capacity
	 */
	public ObjectObjectMap (int initialCapacity) {
		this(initialCapacity, DEFAULT_LOAD_FACTOR);
	}

	/**
	 * Constructs an empty {@code ObjectObjectMap} with the specified load factor and an initial
	 * capacity of 16.
	 * <p>
	 * The load factor will cause the map to double in size when the number
	 * of items it contains has filled up more than {@code loadFactor} of the available
	 * space.
	 *
	 * @param loadFactor the load factor
	 */
	public ObjectObjectMap (float loadFactor) {
		this(DEFAULT_START_SIZE, loadFactor);
	}

	/**
	 * Constructs an empty {@code ObjectObjectMap} with the specified load factor and initial
	 * capacity.
	 * <p>
	 * The load factor will cause the map to double in size when the number
	 * of items it contains has filled up more than {@code loadFactor} of the available
	 * space.
	 *
	 * @param initialCapacity the initial capacity
	 * @param loadFactor the load factor
	 */
	@SuppressWarnings("unchecked")
	public ObjectObjectMap (int initialCapacity, float loadFactor) {
		if (initialCapacity <= 0) {
			throw new IllegalArgumentException("initial capacity must be strictly positive");
		}
		if (loadFactor <= 0.f || loadFactor > 1.f) {
			throw new IllegalArgumentException("load factor must be a value in the (0.0f, 1.0f] range.");
		}

		size = 0;
		int tableSize = Math.max(2, 1 << -BitConversion.countLeadingZeros(initialCapacity));
		mask = tableSize - 1;
		shift = BitConversion.countLeadingZeros(tableSize - 1L);
		flipThreshold = BitConversion.countTrailingZeros(tableSize) + 4;

		keyTable = (K[])new Object[tableSize];
		valueTable = (V[])new Object[tableSize];
		this.loadFactor = loadFactor;
		loadThreshold = (int)(loadFactor * tableSize);

		regenHashMultipliers(tableSize);
	}

	public ObjectObjectMap (ObjectObjectMap<? extends K, ? extends V> other) {
		size = other.size;
		mask = other.mask;
		shift = other.shift;
		flipThreshold = other.flipThreshold;
		loadFactor = other.loadFactor;
		loadThreshold = other.loadThreshold;
		defaultValue = other.defaultValue;
		hashMultiplier1 = other.hashMultiplier1;
		hashMultiplier2 = other.hashMultiplier2;
		keyTable = Arrays.copyOf(other.keyTable, other.keyTable.length);
		valueTable = Arrays.copyOf(other.valueTable, other.valueTable.length);
	}

	@Override
	public boolean containsKey (Object key) {
		if(key == null) return false;
		if(flipThreshold == 0){
			K[] keyTable = this.keyTable;
			for (int i = (int)(key.hashCode() * hashMultiplier1 >>> shift); ; i = i + 1 & mask) {
				K other = keyTable[i];
				if (key.equals(other))
					return true;
				if (other == null)
					return false;
			}
		}
		final int hc = key.hashCode();

		return key == keyTable[(int)(hashMultiplier1 * hc >>> shift) | 1] ||
			key == keyTable[(int)(hashMultiplier2 * hc >>> shift) & -2];
	}

	@Override
	public V get (Object key) {
		return getOrDefault(key, defaultValue);
	}

	public V getOrDefault (Object key, @Nullable V defaultValue) {
		if(key == null) return defaultValue;

		if(flipThreshold == 0){
			K[] keyTable = this.keyTable;
			for (int i = (int)(key.hashCode() * hashMultiplier1 >>> shift); ; i = i + 1 & mask) {
				K other = keyTable[i];
				if (key.equals(other))
					return valueTable[i];
				if (other == null)
					return defaultValue;
			}
		}
		int hc = key.hashCode();
		int hr1 = (int)(hashMultiplier1 * hc >>> shift) | 1;
		if (key == keyTable[hr1]) {
			return valueTable[hr1];
		}

		int hr2 = (int)(hashMultiplier2 * hc >>> shift) & -2;
		if (key == keyTable[hr2]) {
			return valueTable[hr2];
		}

		return defaultValue;
	}

	@Override
	public V put (K key, V value) {
		if(key == null) throw new NullPointerException("ObjectObjectMap does not permit null keys.");

		if(flipThreshold == 0) {
			int i = locateKey(key);
			if (i >= 0) { // Existing key was found.
				V oldValue = valueTable[i];
				valueTable[i] = value;
				return oldValue;
			}
			i = ~i; // Empty space was found.
			keyTable[i] = key;
			valueTable[i] = value;
			if (++size >= loadThreshold) {resize(keyTable.length << 1);}
			return defaultValue;
		}

		boolean absent = true;
		V old = defaultValue;
		int hc = key.hashCode();
		int hr1 = (int)(hashMultiplier1 * hc >>> shift) | 1;
		if (key == keyTable[hr1]) {
			old = valueTable[hr1];
			absent = false;
		} else {
			int hr2 = (int)(hashMultiplier2 * hc >>> shift) & -2;
			if (key == keyTable[hr2]) {
				old = valueTable[hr2];
				absent = false;
			}
		}

		if (absent) {
			// If we need to resize after adding this item, it's probably best to resize before we add it.
			if (size() + 1 >= loadThreshold) {
				resize();
			}
		}
		if ((key = putSafe(key, value)) != null) {
			value = displacedValue;
			displacedValue = null;
			flip();
			int i = locateKey(key);
			if (i >= 0) { // Existing key was found.
				valueTable[i] = value;
				return old;
			}
			i = ~i; // Empty space was found.
			keyTable[i] = key;
			valueTable[i] = value;
			if (++size >= loadThreshold) {resize(keyTable.length << 1);}
			return old;
		}

		if (absent) {
			// Only increase the size if no item was already there.
			size++;
		}

		return old;
	}

	/**
	 * Attempts to place the given key and value, but is permitted to fail. If this fails, it returns a displaced key
	 * and assigns its displaced value to {@link #displacedValue}.
	 * @return the key we failed to move because of collisions or <tt>null</tt> if
	 * successful.
	 */
	protected K putSafe (K key, V value) {
		int loop = 0;
		while (loop++ < flipThreshold) {
			int hc = key.hashCode();
			int hr1 = (int)(hashMultiplier1 * hc >>> shift) | 1;
			K k1 = keyTable[hr1];
			if (k1 == null || key == k1) {
				valueTable[hr1] = value;
				return null;
			}
			int hr2 = (int)(hashMultiplier2 * hc >>> shift) & -2;
			K k2 = keyTable[hr2];
			if (k2 == null || key == k2) {
				valueTable[hr2] = value;
				return null;
			}

			// Both tables have an item in the required position that doesn't have the same key, we need to move things around.
			// Prefer always moving from the odd entries for simplicity.
			V temp = valueTable[hr1];
			keyTable[hr1] = key;
			key = k1;
			valueTable[hr1] = value;
			value = temp;
		}
		displacedValue = value;
		return key;
	}

	@Override
	public V remove (Object key) {
		if (key == null)
			return null;

		if(flipThreshold == 0) {
			int i = locateKey(key);
			if (i < 0) {return defaultValue;}
			K[] keyTable = this.keyTable;
			V[] valueTable = this.valueTable;
			K rem;
			V oldValue = valueTable[i];
			int mask = this.mask, next = i + 1 & mask;
			while ((rem = keyTable[next]) != null) {
				int placement = (int)(rem.hashCode() * hashMultiplier1 >>> shift);
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

		int hc = key.hashCode();
		int hr1 = (int)(hashMultiplier1 * hc >>> shift) | 1;
		V oldValue = null;

		if (key == keyTable[hr1]) {
			oldValue = valueTable[hr1];
			keyTable[hr1] = null;
			valueTable[hr1] = null;
			size--;
		} else {
			int hr2 = (int)(hashMultiplier2 * hc >>> shift) & -2;
			if (key == keyTable[hr2]) {
				oldValue = valueTable[hr2];
				keyTable[hr2] = null;
				valueTable[hr2] = null;
				size--;
			}
		}

		return oldValue;
	}

	@Override
	public void clear () {
		size = 0;
		Arrays.fill(keyTable, null);
		Arrays.fill(valueTable, null);
	}

	/**
	 * Gets the default value, a {@code V} which is returned by {@link #get(Object)} if the key is not found.
	 * If not changed, the default value is null.
	 *
	 * @return the current default value
	 */
	@Nullable
	public V getDefaultValue () {
		return defaultValue;
	}

	/**
	 * Sets the default value, a {@code V} which is returned by {@link #get(Object)} if the key is not found.
	 * If not changed, the default value is null. Note that {@link #getOrDefault(Object, Object)} is also available,
	 * which allows specifying a "not-found" value per-call.
	 *
	 * @param defaultValue may be any V object or null; should usually be one that doesn't occur as a typical value
	 */
	public void setDefaultValue (@Nullable V defaultValue) {
		this.defaultValue = defaultValue;
	}

	/**
	 * Pseudo-randomly selects new values for hash multipliers 1 and 2, using the existing hash multipliers and a
	 * {@code modifier} to determine which multipliers to choose. This draws multipliers exactly from
	 * {@link Utilities#GOOD_MULTIPLIERS}, with {@code hashMultiplier1} receiving a value from the first 256 items in
	 * that table, and {@code hashMultiplier2} receiving a value from the last 256 items.
	 * @param modifier usually refers to a new size of table when they are being resized (but doesn't have to)
	 */
	protected void regenHashMultipliers(int modifier) {
		//This is close to a kind of Xor-Square-Or pattern, or XQO, that (if modifier weren't added) would be a passable
		//random number generator. The result is used to select one of 256 possible long values for hashMultiplier1, and
		//a different result selects from a different 256 possible long values for hashMultiplier2.
		int idx1 = (int)(-(hashMultiplier2 ^ ((modifier + hashMultiplier2) * hashMultiplier2 | 5L)) >>> 56);
		int idx2 = (int)(-(hashMultiplier1 ^ ((modifier + hashMultiplier1) * hashMultiplier1 | 7L)) >>> 56) | 256;
		hashMultiplier1 = Utilities.GOOD_MULTIPLIERS[idx1];
		hashMultiplier2 = Utilities.GOOD_MULTIPLIERS[idx2];
	}

	/**
	 * Double the size of the map until we can successfully manage to re-add all the items
	 * we currently contain.
	 */
	protected void resize() {
		if(size == 0) return;
		int newSize = keyTable.length;
		do {
			newSize <<= 1;
		} while (!resize(newSize));
	}

	@SuppressWarnings("unchecked")
	protected boolean resize(final int newSize) {
		if(size == 0) return true;
		if(flipThreshold == 0) {
			int oldCapacity = keyTable.length;
			loadThreshold = (int)(newSize * loadFactor);
			mask = newSize - 1;
			shift = BitConversion.countLeadingZeros(newSize - 1L);

			hashMultiplier1 = Utilities.GOOD_MULTIPLIERS[(int)(hashMultiplier1 >>> 48 + shift) & 511];
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
			return true;
		}

		// Save old state as we may need to restore it if the resize() operation fails.
		K[] oldK = keyTable;
		V[] oldV = valueTable;
		long oldH1 = hashMultiplier1;
		long oldH2 = hashMultiplier2;
		mask = newSize - 1;
		shift = BitConversion.countLeadingZeros(newSize - 1L);
		flipThreshold = BitConversion.countTrailingZeros(newSize) + 4;
		loadThreshold = (int)(loadFactor * newSize);

		// Already point keyTable and valueTable to the new tables since putSafe operates on them.
		keyTable = (K[])new Object[newSize];
		valueTable = (V[])new Object[newSize];

		regenHashMultipliers(newSize);

		for (int i = 0; i < oldK.length; i++) {
			if (oldK[i] != null) {
				if (putSafe(oldK[i], oldV[i]) != null) {
					keyTable = oldK;
					valueTable = oldV;
					hashMultiplier1 = oldH1;
					hashMultiplier2 = oldH2;
					mask = keyTable.length - 1;
					shift = BitConversion.countLeadingZeros(newSize - 1L);
					flipThreshold = BitConversion.countTrailingZeros(keyTable.length) + 4;
					loadThreshold = (int)(loadFactor * keyTable.length);
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * "Flips the switch" from using cuckoo hashing to using linear probing.  This changes {@link #flipThreshold} to 0,
	 * always, which is what indicates we have switched to linear probing. This must only be called once, typically when
	 * cuckoo hashing has failed to place a key, and it cannot be reversed. While this degrades the performance of the
	 * map somewhat in the best-case and expected-case, it drastically improves performance in the worst-case, which
	 * should be the only time this has to be called.
	 */
	@SuppressWarnings("unchecked")
	protected void flip() {
		// Save old state as we may need to restore it if the resize() operation fails.
		K[] oldK = keyTable;
		V[] oldV = valueTable;

		keyTable = (K[]) new Object[oldK.length];
		valueTable = (V[]) new Object[oldV.length];

		loadFactor = (float) Math.sqrt(loadFactor);
		flipThreshold = 0;
		loadThreshold = (int)(loadFactor * keyTable.length);
		size = 0;

		for (int i = 0; i < oldK.length; i++) {
			if (oldK[i] != null) {
				put(oldK[i], oldV[i]);
			}
		}
	}

	@Override
	public int size () {
		return size;
	}

	@Override
	public boolean isEmpty () {
		return size == 0;
	}

	@Override
	public void putAll (Map<? extends K, ? extends V> m) {
		for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
			put(entry.getKey(), entry.getValue());
		}
	}

	@Override
	public boolean containsValue (Object value) {
		for (int i = 0; i < keyTable.length; i++) {
			if (keyTable[i] != null && Objects.equals(valueTable[i], value)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns the index of the key if already present, else {@code ~index} for the next empty index.
	 *
	 * @param key a non-null K key
	 * @return a negative index if the key was not found, or the non-negative index of the existing key if found
	 */
	protected int locateKey (Object key) {
		K[] keyTable = this.keyTable;
		for (int i = (int)(key.hashCode() * hashMultiplier1 >>> shift); ; i = i + 1 & mask) {
			K other = keyTable[i];
			if (key.equals(other))
				return i; // Same key was found.
			if (other == null)
				return ~i; // Always negative; means empty space is available at position `i`.
		}
	}

	/**
	 * Puts key and value but skips checks for existing keys, and doesn't increment size. Meant for use during
	 * {@link #resize(int)}, hence the name, when using linear probing.
	 */
	protected void putResize (K key, @Nullable V value) {
		K[] keyTable = this.keyTable;
		for (int i = (int)(key.hashCode() * hashMultiplier1 >>> shift); ; i = i + 1 & mask) {
			if (keyTable[i] == null) {
				keyTable[i] = key;
				valueTable[i] = value;
				return;
			}
		}
	}

	@Override
	public @NonNull Set<Map.Entry<K, V>> entrySet () {
		Set<Map.Entry<K,V>> entries;
		return (entries = entrySet) == null ? (entrySet = new EntrySet<>(this)) : entries;
	}

	/**
	 * Just a {@link Map.Entry} with a mutable key and a mutable value, so it can be reused.
	 * @param <K> Should match the {@code K} of a Map that contains this Entry
	 * @param <V> Should match the {@code V} of a Map that contains this Entry
	 */
	public static class Entry<K, V> implements Map.Entry<K, V> {
		@Nullable public K key;
		@Nullable public V value;

		public Entry () {
		}

		public Entry (@Nullable K key, @Nullable V value) {
			this.key = key;
			this.value = value;
		}

		public Entry (Map.Entry<? extends K, ? extends V> entry) {
			key = entry.getKey();
			value = entry.getValue();
		}

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

	/**
	 * A Set of {@link Map.Entry} that is closely tied to a map, and represents the K,V entries in that map.
	 * You normally create an EntrySet via {@link #entrySet()}, but you can also call the constructor yourself if you
	 * want to iterate over the same map at the same time at different rates.
	 * @param <K> Should match the {@code K} of the related map
	 * @param <V> Should match the {@code V} of the related map
	 */
	public static class EntrySet<K, V> extends AbstractSet<Map.Entry<K, V>> {

		protected final ObjectObjectMap<K, V> map;
		public EntrySet(ObjectObjectMap<K, V> map) {
			this.map = map;
		}
		/**
		 * Returns an iterator over the elements contained in this collection.
		 *
		 * @return an iterator over the elements contained in this collection
		 */
		@Override
		@NonNull
		public Iterator<Map.Entry<K, V>> iterator() {
			return new EntryIterator<>(map);
		}

		@Override
		public int size() {
			return map.size;
		}
	}

	public static class EntryIterator<K, V> implements Iterable<Map.Entry<K, V>>, Iterator<Map.Entry<K, V>> {
		public boolean hasNext;

		protected final ObjectObjectMap<K, V> map;
		protected final Entry<K, V> entry;
		protected int nextIndex, currentIndex;
		public boolean valid = true;

		public EntryIterator(ObjectObjectMap<K, V> map) {
			this.map = map;
			entry = new Entry<>();
			reset();
		}

		public void reset () {
			currentIndex = -1;
			nextIndex = -1;
			findNextIndex();
		}

		protected void findNextIndex () {
			K[] keyTable = map.keyTable;
			for (int n = keyTable.length; ++nextIndex < n; ) {
				if (keyTable[nextIndex] != null) {
					hasNext = true;
					return;
				}
			}
			hasNext = false;
		}

		/**
		 * Note: the same entry instance is returned each time this method is called.
		 *
		 * @return a reused Entry that will have its key and value set to the next pair
		 */
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

		@Override
		public void remove () {
			int i = currentIndex;
			if (i < 0) {throw new IllegalStateException("next must be called before remove.");}
			K[] keyTable = map.keyTable;
			V[] valueTable = map.valueTable;

			if(map.flipThreshold == 0) {
				final long hashMultiplier1 = map.hashMultiplier1;
				K rem;
				int mask = map.mask, next = i + 1 & mask, shift = map.shift;
				while ((rem = keyTable[next]) != null) {
					int placement = (int)(rem.hashCode() * hashMultiplier1 >>> shift);
					if ((next - placement & mask) > (i - placement & mask)) {
						keyTable[i] = rem;
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
				return;
			}
			K key = keyTable[i];
			final long hashMultiplier1 = map.hashMultiplier1, hashMultiplier2 = map.hashMultiplier2;
			int hc = key.hashCode(), shift = map.shift;
			int hr1 = (int)(hashMultiplier1 * hc >>> shift) | 1;

			if (key == keyTable[hr1]) {
				keyTable[hr1] = null;
				valueTable[hr1] = null;
				map.size--;
				if (i != currentIndex) {--nextIndex;}
				currentIndex = -1;

			} else {
				int hr2 = (int)(hashMultiplier2 * hc >>> shift) & -2;
				if (key == keyTable[hr2]) {
					keyTable[hr2] = null;
					valueTable[hr2] = null;
					map.size--;
					if (i != currentIndex) {--nextIndex;}
					currentIndex = -1;
				}
			}
		}

		/**
		 * Returns an iterator over elements of type {@code T}.
		 *
		 * @return an Iterator.
		 */
		@Override
		public @NonNull Iterator<Map.Entry<K, V>> iterator() {
			return this;
		}
	}

	public static class KeySet<K> extends AbstractSet<K> {
		ObjectObjectMap<K, ?> map;
		public KeySet(ObjectObjectMap<K, ?> map) {
			this.map = map;
		}

		public @NonNull Iterator<K> iterator() {
			return new Iterator<K>() {
				private final Iterator<? extends Map.Entry<K, ?>> i = map.entrySet().iterator();

				public boolean hasNext() {
					return i.hasNext();
				}

				public K next() {
					return i.next().getKey();
				}

				public void remove() {
					i.remove();
				}
			};
		}

		public int size() {
			return map.size();
		}

		public boolean isEmpty() {
			return map.isEmpty();
		}

		public void clear() {
			map.clear();
		}

		public boolean contains(Object k) {
			return map.containsKey(k);
		}


	}

    public @NonNull Set<K> keySet() {
        Set<K> ks = keySet;
        if (ks == null) {
            ks = new KeySet<>(this);
			keySet = ks;
        }
        return ks;
    }


	public static class ValueCollection<V> extends AbstractCollection<V> {
		ObjectObjectMap<?, V> map;
		public ValueCollection(ObjectObjectMap<?, V> map) {
			this.map = map;
		}

		public @NonNull Iterator<V> iterator() {
			return new Iterator<V>() {
				private final Iterator<? extends Map.Entry<?, V>> i = map.entrySet().iterator();

				public boolean hasNext() {
					return i.hasNext();
				}

				public V next() {
					return i.next().getValue();
				}

				public void remove() {
					i.remove();
				}
			};
		}

		public int size() {
			return map.size();
		}

		public boolean isEmpty() {
			return map.isEmpty();
		}

		public void clear() {
			map.clear();
		}

		public boolean contains(Object v) {
			return map.containsValue(v);
		}


	}

	public @NonNull Collection<V> values() {
        Collection<V> vals = values;
        if (vals == null) {
            vals = new ValueCollection<>(this);
            values = vals;
        }
        return vals;
    }

}
/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
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

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.*;

/** An {@link ObjectMap} that also stores keys in an {@link java.util.ArrayList} using the insertion order. Null keys are not allowed. No
 * allocation is done except when growing the table size.
 * <p>
 * Iteration over the {@link #entrySet()} ()}, {@link #keySet()} ()}, and {@link #values()} is ordered and faster than an unordered map. Keys
 * can also be accessed and the order changed using {@link #orderedKeys()}. There is some additional overhead for put and remove.
 * <p>
 * This class performs fast contains (typically O(1), worst case O(n) but that is rare in practice). Remove is somewhat slower due
 * to {@link #orderedKeys()}. Add may be slightly slower, depending on hash collisions. Hashcodes are rehashed to reduce
 * collisions and the need to resize. Load factors greater than 0.91 greatly increase the chances to resize to the next higher POT
 * size.
 * <p>
 * Unordered sets and maps are not designed to provide especially fast iteration. Iteration is faster with OrderedSet and
 * OrderedMap.
 * <p>
 * This implementation uses linear probing with the backward shift algorithm for removal. Hashcodes are rehashed using Fibonacci
 * hashing, instead of the more common power-of-two mask, to better distribute poor hashCodes (see <a href=
 * "https://probablydance.com/2018/06/16/fibonacci-hashing-the-optimization-that-the-world-forgot-or-a-better-alternative-to-integer-modulo/">Malte
 * Skarupke's blog post</a>). Linear probing continues to work even when all hashCodes collide, just more slowly.
 * @author Nathan Sweet
 * @author Tommy Ettinger */
public class OrderedMap<K, V> extends ObjectMap<K, V> implements Serializable {
	private static final long serialVersionUID = 0L;

	final ArrayList<K> keys;

	public OrderedMap () {
		keys = new ArrayList<>();
	}

	public OrderedMap (int initialCapacity) {
		super(initialCapacity);
		keys = new ArrayList<>(initialCapacity);
	}

	public OrderedMap (int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
		keys = new ArrayList<>(initialCapacity);
	}

	public OrderedMap (OrderedMap<? extends K, ? extends V> map) {
		super(map);
		keys = new ArrayList<>(map.keys);
	}
	/** Creates a new map identical to the specified map. */
	public OrderedMap (Map<? extends K, ? extends V> map) {
		this(map.size());
		for(K k : map.keySet()){
			put(k, map.get(k));
		}
	}

	public V put (K key, V value) {
		int i = locateKey(key);
		if (i >= 0) { // Existing key was found.
			V oldValue = valueTable[i];
			valueTable[i] = value;
			return oldValue;
		}
		i = -(i + 1); // Empty space was found.
		keyTable[i] = key;
		valueTable[i] = value;
		keys.add(key);
		if (++size >= threshold) resize(keyTable.length << 1);
		return null;
	}

	public <T extends K> void putAll (OrderedMap<T, ? extends V> map) {
		ensureCapacity(map.size);
		ArrayList<T> ks = map.keys;
		int kl = ks.size();
		T k;
		for (int i = 0; i < kl; i++) {
			k = ks.get(i);
			put(k, map.get(k));
		}
	}

	public V remove (Object key) {
		if(!keys.remove(key)) return null;
		return super.remove(key);
	}

	public V removeIndex (int index) {
		return super.remove(keys.remove(index));
	}

	/** Changes the key {@code before} to {@code after} without changing its position in the order or its value. Returns true if
	 * {@code after} has been added to the OrderedMap and {@code before} has been removed; returns false if {@code after} is
	 * already present or {@code before} is not present. If you are iterating over an OrderedMap and have an index, you should
	 * prefer {@link #alterIndex(int, Object)}, which doesn't need to search for an index like this does and so can be faster.
	 * @param before a key that must be present for this to succeed
	 * @param after a key that must not be in this map for this to succeed
	 * @return true if {@code before} was removed and {@code after} was added, false otherwise */
	public boolean alter (K before, K after) {
		if (containsKey(after)) return false;
		int index = keys.indexOf(before);
		if (index == -1) return false;
		super.put(after, super.remove(before));
		keys.set(index, after);
		return true;
	}

	/** Changes the key at the given {@code index} in the order to {@code after}, without changing the ordering of other entries or
	 * any values. If {@code after} is already present, this returns false; it will also return false if {@code index} is invalid
	 * for the size of this map. Otherwise, it returns true. Unlike {@link #alter(Object, Object)}, this operates in constant time.
	 * @param index the index in the order of the key to change; must be non-negative and less than {@link #size}
	 * @param after the key that will replace the contents at {@code index}; this key must not be present for this to succeed
	 * @return true if {@code after} successfully replaced the key at {@code index}, false otherwise */
	public boolean alterIndex (int index, K after) {
		if (index < 0 || index >= size || containsKey(after)) return false;
		super.put(after, super.remove(keys.get(index)));
		keys.set(index, after);
		return true;
	}

	public void clear (int maximumCapacity) {
		keys.clear();
		super.clear(maximumCapacity);
	}

	public void clear () {
		keys.clear();
		super.clear();
	}

	public ArrayList<K> orderedKeys () {
		return keys;
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
	 * @return a set view of the keys contained in this map
	 */
	@Override
	public @NotNull OrderedMapKeys<K> keySet() {
		return new OrderedMapKeys<>(this);
	}

	/**
	 * Returns a Collection for the values in the map. Remove is supported by the Collection's iterator.
	 * <p>
	 * Permits nested or multithreaded iteration, but allocates a new {@link Values} instance per-call.
	 *
	 * @return a {@link Collection} of V values
	 */
	@Override
	public @NotNull OrderedMapValues<V> values() {
		return new OrderedMapValues<>(this);
	}

	/**
	 * Returns a Set of Map.Entry, containing the entries in the map. Remove is supported by the Set's iterator.
	 * <p>
	 * Permits nested or multithreaded iteration, but allocates a new {@link Entries} instance per-call.
	 *
	 * @return a {@link Set} of {@link Map.Entry} key-value pairs
	 */
	@Override
	public @NotNull OrderedMapEntries<K, V> entrySet() {
		return new OrderedMapEntries<>(this);
	}

	public @NotNull Iterator<Map.Entry<K, V>> iterator () {
		return entrySet().iterator();
	}

	protected String toString (String separator, boolean braces) {
		if (size == 0) return braces ? "{}" : "";
		java.lang.StringBuilder buffer = new java.lang.StringBuilder(32);
		if (braces) buffer.append('{');
		ArrayList<K> keys = this.keys;
		for (int i = 0, n = keys.size(); i < n; i++) {
			K key = keys.get(i);
			if (i > 0) buffer.append(separator);
			buffer.append(key == this ? "(this)" : key);
			buffer.append('=');
			V value = get(key);
			buffer.append(value == this ? "(this)" : value);
		}
		if (braces) buffer.append('}');
		return buffer.toString();
	}

	static public class OrderedMapEntries<K, V> extends Entries<K, V> {
		protected ArrayList<K> keys;
		public OrderedMapEntries (OrderedMap<K, V> map) {
			keys = map.keys;
			iter = new MapIterator<K, V, Map.Entry<K, V>>(map) {
				@NotNull
				@Override
				public Iterator<Map.Entry<K, V>> iterator() {
					return this;
				}
				public void reset () {
					currentIndex = -1;
					nextIndex = 0;
					hasNext = map.size > 0;
				}

				public boolean hasNext () {
					if (!valid) throw new JdkgdxdsRuntimeException("#iterator() cannot be used nested.");
					return hasNext;
				}

				public Entry<K, V> next () {
					if (!hasNext) throw new NoSuchElementException();
					if (!valid) throw new JdkgdxdsRuntimeException("#iterator() cannot be used nested.");
					currentIndex = nextIndex;
					entry.key = keys.get(nextIndex);
					entry.value = map.get(entry.key);
					nextIndex++;
					hasNext = nextIndex < map.size;
					return entry;
				}

				public void remove () {
					if (currentIndex < 0) throw new IllegalStateException("next must be called before remove.");
					map.remove(entry.key);
					nextIndex--;
					currentIndex = -1;
				}
			};
		}
	}

	static public class OrderedMapKeys<K> extends Keys<K> {
		protected ArrayList<K> keys;

		public OrderedMapKeys (OrderedMap<K, ?> map) {
			keys = map.keys;
			iter = new MapIterator<K, Object, K>((OrderedMap) map) {
				@Override
				public @NotNull Iterator<K> iterator() {
					return this;
				}

				public boolean hasNext () {
					if (!valid) throw new JdkgdxdsRuntimeException("#iterator() cannot be used nested.");
					return hasNext;
				}

				public void reset () {
					currentIndex = -1;
					nextIndex = 0;
					hasNext = map.size > 0;
				}

				public K next () {
					if (!hasNext) throw new NoSuchElementException();
					if (!valid) throw new JdkgdxdsRuntimeException("#iterator() cannot be used nested.");
					K key = keys.get(nextIndex);
					currentIndex = nextIndex;
					nextIndex++;
					hasNext = nextIndex < map.size;
					return key;
				}

				public void remove () {
					if (currentIndex < 0) throw new IllegalStateException("next must be called before remove.");
					((OrderedMap)map).removeIndex(currentIndex);
					nextIndex = currentIndex;
					currentIndex = -1;
				}

			};

		}

		@NotNull
		@Override
		public Object[] toArray() {
			return super.toArray();
		}
		
		@NotNull
		@Override
		public <T> T[] toArray(@NotNull T[] a) {
			return super.toArray(a);
		}
	}

	static public class OrderedMapValues<V> extends Values<V> {
		private ArrayList<?> keys;

		public OrderedMapValues (OrderedMap<?, V> map) {
			keys = map.keys;
			iter = new MapIterator<Object, V, V>((ObjectMap<Object, V>) map) {
				@Override
				public @NotNull Iterator<V> iterator() {
					return this;
				}

				@Override
				public boolean hasNext() {
					if (!valid) throw new JdkgdxdsRuntimeException("#iterator() cannot be used nested.");
					return hasNext;
				}

				public void reset () {
					currentIndex = -1;
					nextIndex = 0;
					hasNext = map.size > 0;
				}

				public V next () {
					if (!hasNext) throw new NoSuchElementException();
					if (!valid) throw new JdkgdxdsRuntimeException("#iterator() cannot be used nested.");
					V value = map.get(keys.get(nextIndex));
					currentIndex = nextIndex;
					nextIndex++;
					hasNext = nextIndex < map.size;
					return value;
				}

				public void remove () {
					if (currentIndex < 0) throw new IllegalStateException("next must be called before remove.");
					((OrderedMap)map).removeIndex(currentIndex);
					nextIndex = currentIndex;
					currentIndex = -1;
				}
			};
		}
	}
}

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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.NoSuchElementException;

/** A {@link ObjectSet} that also stores keys in an {@link ArrayList} using the insertion order. Null keys are not allowed. No
 * allocation is done except when growing the table size.
 * <p>
 * {@link #iterator() Iteration} is ordered and faster than an unordered set. Keys can also be accessed and the order changed
 * using {@link #orderedItems()}. There is some additional overhead for put and remove.
 * <p>
 * This class performs fast contains (typically O(1), worst case O(n) but that is rare in practice). Remove is somewhat slower due
 * to {@link #orderedItems()}. Add may be slightly slower, depending on hash collisions. Hashcodes are rehashed to reduce
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
public class OrderedSet<T> extends ObjectSet<T> implements Serializable {
	private static final long serialVersionUID = 0L;
	final ArrayList<T> items;

	public OrderedSet () {
		items = new ArrayList();
	}

	public OrderedSet (int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
		items = new ArrayList(initialCapacity);
	}

	public OrderedSet (int initialCapacity) {
		super(initialCapacity);
		items = new ArrayList(initialCapacity);
	}

	public OrderedSet (OrderedSet<? extends T> set) {
		super(set);
		items = new ArrayList(set.items);
	}
	
	/** Creates a new set that contains all distinct elements in {@code coll}. */
	public OrderedSet (Collection<? extends T> coll) {
		this(coll.size());
		addAll(coll);
	}
	
	public boolean add (T key) {
		if (!super.add(key)) return false;
		items.add(key);
		return true;
	}

	/** Sets the key at the specfied index. Returns true if the key was not already in the set. If this set already contains the
	 * key, the existing key's index is changed if needed and false is returned. */
	public boolean add (T key, int index) {
		if (!super.add(key)) {
			int oldIndex = items.indexOf(key);
			if (oldIndex != index) items.add(index, items.remove(oldIndex));
			return false;
		}
		items.add(index, key);
		return true;
	}

	public void addAll (OrderedSet<T> set) {
		ensureCapacity(set.size);
		ArrayList<T> si = set.items;
		for (int i = 0, n = si.size(); i < n; i++)
			add(si.get(i));
	}

	public boolean remove (Object key) {
		if (!super.remove(key)) return false;
		items.remove(key);
		return true;
	}

	public T removeIndex (int index) {
		T key = items.remove(index);
		super.remove(key);
		return key;
	}

	/** Changes the item {@code before} to {@code after} without changing its position in the order. Returns true if {@code after}
	 * has been added to the OrderedSet and {@code before} has been removed; returns false if {@code after} is already present or
	 * {@code before} is not present. If you are iterating over an OrderedSet and have an index, you should prefer
	 * {@link #alterIndex(int, Object)}, which doesn't need to search for an index like this does and so can be faster.
	 * @param before an item that must be present for this to succeed
	 * @param after an item that must not be in this set for this to succeed
	 * @return true if {@code before} was removed and {@code after} was added, false otherwise */
	public boolean alter (T before, T after) {
		if (contains(after)) return false;
		if (!super.remove(before)) return false;
		super.add(after);
		items.set(items.indexOf(before), after);
		return true;
	}

	/** Changes the item at the given {@code index} in the order to {@code after}, without changing the ordering of other items. If
	 * {@code after} is already present, this returns false; it will also return false if {@code index} is invalid for the size of
	 * this set. Otherwise, it returns true. Unlike {@link #alter(Object, Object)}, this operates in constant time.
	 * @param index the index in the order of the item to change; must be non-negative and less than {@link #size}
	 * @param after the item that will replace the contents at {@code index}; this item must not be present for this to succeed
	 * @return true if {@code after} successfully replaced the contents at {@code index}, false otherwise */
	public boolean alterIndex (int index, T after) {
		if (index < 0 || index >= size || contains(after)) return false;
		super.remove(items.get(index));
		super.add(after);
		items.set(index, after);
		return true;
	}

	public void clear (int maximumCapacity) {
		items.clear();
		super.clear(maximumCapacity);
	}

	public void clear () {
		items.clear();
		super.clear();
	}

	public ArrayList<T> orderedItems () {
		return items;
	}

	public OrderedSetIterator<T> iterator () {
		return new OrderedSetIterator(this);
	}

	public String toString (String separator) {
		if (size == 0) return "{}";
		ArrayList<T> items = this.items;
		java.lang.StringBuilder buffer = new java.lang.StringBuilder(32);
		buffer.append('{');
		buffer.append(items.get(0));
		for (int i = 1; i < size; i++) {
			buffer.append(separator);
			buffer.append(items.get(i));
		}
		buffer.append('}');
		return buffer.toString();
	}

	public String toString () {
		return toString(", ");
	}

	static public class OrderedSetIterator<K> extends ObjectSetIterator<K> {
		private ArrayList<K> items;

		public OrderedSetIterator (OrderedSet<K> set) {
			super(set);
			items = set.items;
		}

		public void reset () {
			nextIndex = 0;
			hasNext = set.size > 0;
		}

		public K next () {
			if (!hasNext) throw new NoSuchElementException();
			if (!valid) throw new JdkgdxdsRuntimeException("#iterator() cannot be used nested.");
			K key = items.get(nextIndex);
			nextIndex++;
			hasNext = nextIndex < set.size;
			return key;
		}

		public void remove () {
			if (nextIndex < 0) throw new IllegalStateException("next must be called before remove.");
			nextIndex--;
			((OrderedSet)set).removeIndex(nextIndex);
		}
	}

	static public <T> OrderedSet<T> with (T... array) {
		OrderedSet<T> set = new OrderedSet<T>();
		set.addAll(array);
		return set;
	}
}

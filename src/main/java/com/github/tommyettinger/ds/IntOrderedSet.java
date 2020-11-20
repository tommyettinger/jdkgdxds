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
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;

import static com.github.tommyettinger.ds.Utilities.tableSize;

/**
 * A {@link IntSet} that also stores keys in a {@link IntList} using the insertion order. No
 * allocation is done except when growing the table size.
 * <p>
 * {@link #iterator() Iteration} is ordered and faster than an unordered set. Keys can also be accessed and the order changed
 * using {@link #order()}. There is some additional overhead for put and remove.
 * <p>
 * This class performs fast contains (typically O(1), worst case O(n) but that is rare in practice). Remove is somewhat slower due
 * to {@link #order()}. Add may be slightly slower, depending on hash collisions. Hashcodes are rehashed to reduce
 * collisions and the need to resize. Load factors greater than 0.91 greatly increase the chances to resize to the next higher POT
 * size.
 * <p>
 * Unordered sets and maps are not designed to provide especially fast iteration. Iteration is faster with {@link Ordered} types like
 * ObjectOrderedSet and ObjectObjectOrderedMap.
 * <p>
 * You can customize most behavior of this set by extending it. {@link #place(int)} can be overridden to change how hashCodes
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
public class IntOrderedSet extends IntSet implements Ordered.OfInt, Serializable {
	private static final long serialVersionUID = 0L;
	protected final IntList items;

	public IntOrderedSet () {
		items = new IntList();
	}

	public IntOrderedSet (int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
		items = new IntList(initialCapacity);
	}

	public IntOrderedSet (int initialCapacity) {
		super(initialCapacity);
		items = new IntList(initialCapacity);
	}

	public IntOrderedSet (IntOrderedSet set) {
		super(set);
		items = new IntList(set.items);
	}

	/**
	 * Creates a new set that contains all distinct elements in {@code coll}.
	 */
	public IntOrderedSet (OfInt coll) {
		this(coll.size());
		addAll(coll);
	}

	@Override
	public boolean add (int key) {
		return super.add(key) && items.add(key);
	}

	/**
	 * Sets the key at the specfied index. Returns true if the key was not already in the set. If this set already contains the
	 * key, the existing key's index is changed if needed and false is returned.
	 */
	public boolean add (int key, int index) {
		if (!super.add(key)) {
			int oldIndex = items.indexOf(key);
			if (oldIndex != index) { items.add(index, items.removeAtIndex(oldIndex)); }
			return false;
		}
		items.add(index, key);
		return true;
	}

	public void addAll (Ordered.OfInt other) {
		IntList oo = other.order();
		ensureCapacity(oo.size());
		for (int i = 0, n = oo.size(); i < n; i++) { add(oo.get(i)); }
	}

	@Override
	public boolean remove (int key) {
		return super.remove(key) && items.remove(key);
	}

	public int removeAtIndex (int index) {
		int key = items.removeAtIndex(index);
		super.remove(key);
		return key;
	}

	/**
	 * Increases the size of the backing array to accommodate the specified number of additional items / loadFactor. Useful before
	 * adding many items to avoid multiple backing array resizes.
	 *
	 * @param additionalCapacity how many additional items this should be able to hold without resizing (probably)
	 */
	@Override
	public void ensureCapacity (int additionalCapacity) {
		int tableSize = tableSize(size + additionalCapacity, loadFactor);
		if (keyTable.length < tableSize) { resize(tableSize); }
		items.ensureCapacity(size + additionalCapacity);
	}

	/**
	 * Changes the item {@code before} to {@code after} without changing its position in the order. Returns true if {@code after}
	 * has been added to the ObjectOrderedSet and {@code before} has been removed; returns false if {@code after} is already present or
	 * {@code before} is not present. If you are iterating over an ObjectOrderedSet and have an index, you should prefer
	 * {@link #alterIndex(int, int)}, which doesn't need to search for an index like this does and so can be faster.
	 *
	 * @param before an item that must be present for this to succeed
	 * @param after  an item that must not be in this set for this to succeed
	 * @return true if {@code before} was removed and {@code after} was added, false otherwise
	 */
	public boolean alter (int before, int after) {
		if (contains(after)) { return false; }
		if (!super.remove(before)) { return false; }
		super.add(after);
		items.set(items.indexOf(before), after);
		return true;
	}

	/**
	 * Changes the item at the given {@code index} in the order to {@code after}, without changing the ordering of other items. If
	 * {@code after} is already present, this returns false; it will also return false if {@code index} is invalid for the size of
	 * this set. Otherwise, it returns true. Unlike {@link #alter(int, int)}, this operates in constant time.
	 *
	 * @param index the index in the order of the item to change; must be non-negative and less than {@link #size}
	 * @param after the item that will replace the contents at {@code index}; this item must not be present for this to succeed
	 * @return true if {@code after} successfully replaced the contents at {@code index}, false otherwise
	 */
	public boolean alterIndex (int index, int after) {
		if (index < 0 || index >= size || contains(after)) { return false; }
		super.remove(items.get(index));
		super.add(after);
		items.set(index, after);
		return true;
	}

	/**
	 * Gets the int item at the given {@code index} in the insertion order. The index should be between 0
	 * (inclusive) and {@link #size()} (exclusive).
	 *
	 * @param index an index in the insertion order, between 0 (inclusive) and {@link #size()} (exclusive)
	 * @return the item at the given index
	 */
	public int getAtIndex (int index) {
		return items.get(index);
	}

	@Override
	public void clear (int maximumCapacity) {
		items.clear();
		super.clear(maximumCapacity);
	}

	@Override
	public void clear () {
		items.clear();
		super.clear();
	}

	/**
	 * Gets the ObjectList of items in the order this class will iterate through them.
	 * Returns a direct reference to the same ObjectList this uses, so changes to the returned list will
	 * also change the iteration order here.
	 *
	 * @return the ObjectList of items, in iteration order (usually insertion-order), that this uses
	 */
	@Override
	public IntList order () {
		return items;
	}

	/**
	 * Sorts this ObjectOrderedSet in-place by the keys' natural ordering; {@code T} must implement {@link Comparable}.
	 */
	public void sort () {
		items.sort();
	}

	/**
	 * Iterates through items in the same order as {@link #order()}.
	 * Reuses one of two iterators, and does not permit nested iteration;
	 * use {@link OrderedSetIterator#OrderedSetIterator(IntOrderedSet)} to nest iterators.
	 *
	 * @return an {@link Iterator} over the T items in this, in order
	 */
	@Override
	public PrimitiveIterator.OfInt iterator () {
		if (iterator1 == null || iterator2 == null) {
			iterator1 = new OrderedSetIterator(this);
			iterator2 = new OrderedSetIterator(this);
		}
		if (!iterator1.valid) {
			iterator1.reset();
			iterator1.valid = true;
			iterator2.valid = false;
			return iterator1;
		}
		iterator2.reset();
		iterator2.valid = true;
		iterator1.valid = false;
		return iterator2;
	}

	public String toString (String separator) {
		if (size == 0) { return "{}"; }
		IntList items = this.items;
		StringBuilder buffer = new StringBuilder(32);
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

	public static class OrderedSetIterator extends IntSetIterator {
		private final IntList items;

		public OrderedSetIterator (IntOrderedSet set) {
			super(set);
			items = set.items;
		}

		@Override
		public void reset () {
			nextIndex = 0;
			hasNext = set.size > 0;
		}

		@Override
		public int nextInt () {
			if (!hasNext) { throw new NoSuchElementException(); }
			if (!valid) { throw new RuntimeException("#iterator() cannot be used nested."); }
			int key = items.get(nextIndex);
			nextIndex++;
			hasNext = nextIndex < set.size;
			return key;
		}

		@Override
		public void remove () {
			if (nextIndex < 0) { throw new IllegalStateException("next must be called before remove."); }
			nextIndex--;
			set.remove(items.get(nextIndex));
		}
	}

	public static IntOrderedSet with (int... array) {
		IntOrderedSet set = new IntOrderedSet();
		set.addAll(array);
		return set;
	}
}

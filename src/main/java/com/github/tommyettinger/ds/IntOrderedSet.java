/*
 * Copyright (c) 2022-2025 See AUTHORS file.
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
 */

package com.github.tommyettinger.ds;

import com.github.tommyettinger.ds.support.util.IntIterator;

import java.util.Iterator;
import java.util.NoSuchElementException;

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
 * are calculated (which can be useful for types like {@link StringBuilder} that don't implement hashCode()).
 * <p>
 * This implementation uses linear probing with the backward shift algorithm for removal.
 * It tries different hashes from a simple family, with the hash changing on resize.
 * Linear probing continues to work even when all hashCodes collide, just more slowly.
 *
 * @author Nathan Sweet
 * @author Tommy Ettinger
 */
public class IntOrderedSet extends IntSet implements Ordered.OfInt {

	protected final IntList items;

	public IntOrderedSet() {
		this(Utilities.getDefaultTableCapacity());
	}

	/**
	 * Creates an IntOrderedSet with the option to use an IntDeque or IntBag for keeping order.
	 *
	 * @param ordering determines what implementation {@link #order()} will use
	 */
	public IntOrderedSet(OrderType ordering) {
		this(Utilities.getDefaultTableCapacity(), Utilities.getDefaultLoadFactor(), ordering);
	}

	public IntOrderedSet(int initialCapacity, float loadFactor) {
		this(initialCapacity, loadFactor, OrderType.LIST);
	}

	public IntOrderedSet(int initialCapacity, float loadFactor, OrderType ordering) {
		super(initialCapacity, loadFactor);
		switch (ordering) {
			case DEQUE:
				items = new IntDeque(initialCapacity);
				break;
			case BAG:
				items = new IntBag(initialCapacity);
				break;
			default:
				items = new IntList(initialCapacity);
		}
	}

	public IntOrderedSet(int initialCapacity, OrderType ordering) {
		this(initialCapacity, Utilities.getDefaultLoadFactor(), ordering);
	}

	public IntOrderedSet(int initialCapacity) {
		this(initialCapacity, Utilities.getDefaultLoadFactor(), OrderType.LIST);
	}

	/**
	 * Creates a new instance containing the items in the specified iterator.
	 *
	 * @param coll an iterator that will have its remaining contents added to this
	 */
	public IntOrderedSet(IntIterator coll, OrderType ordering) {
		this(ordering);
		addAll(coll);
	}

	/**
	 * Creates a new instance containing the items in the specified iterator.
	 *
	 * @param coll an iterator that will have its remaining contents added to this
	 */
	public IntOrderedSet(IntIterator coll) {
		this();
		addAll(coll);
	}

	public IntOrderedSet(IntOrderedSet set, OrderType ordering) {
		super(set);
		switch (ordering) {
			case DEQUE:
				items = new IntDeque(set.items.iterator());
				break;
			case BAG:
				items = new IntBag(set.items);
				break;
			default:
				items = new IntList(set.items);
		}
	}

	public IntOrderedSet(IntOrderedSet set) {
		super(set);
		if (set.items instanceof IntDeque) items = new IntDeque((IntDeque) set.items);
		else if (set.items instanceof IntBag) items = new IntBag(set.items);
		else items = new IntList(set.items);
	}

	/**
	 * Creates a new set that contains all distinct elements in {@code set}.
	 *
	 * @param set an IntSet without an order
	 */
	public IntOrderedSet(IntSet set) {
		this(set, OrderType.LIST);
	}

	/**
	 * Creates a new set that contains all distinct elements in {@code set}.
	 *
	 * @param set      an IntSet without an order
	 * @param ordering determines what implementation {@link #order()} will use
	 */
	public IntOrderedSet(IntSet set, OrderType ordering) {
		this(set.size(), set.loadFactor, ordering);
		addAll(set);
	}

	/**
	 * Creates a new set that contains all distinct elements in {@code coll}.
	 */
	public IntOrderedSet(OfInt coll) {
		this(coll, OrderType.LIST);
	}

	/**
	 * Creates a new set that contains all distinct elements in {@code coll}.
	 *
	 * @param coll     any {@link PrimitiveCollection.OfInt}
	 * @param ordering determines what implementation {@link #order()} will use
	 */
	public IntOrderedSet(OfInt coll, OrderType ordering) {
		this(coll.size(), Utilities.getDefaultLoadFactor(), ordering);
		addAll(coll);
	}

	/**
	 * Creates a new set by copying {@code count} items from the given Ordered, starting at {@code offset} in that Ordered,
	 * into this.
	 *
	 * @param other  another Ordered.OfInt
	 * @param offset the first index in other's ordering to draw an item from
	 * @param count  how many items to copy from other
	 */
	public IntOrderedSet(Ordered.OfInt other, int offset, int count) {
		this(other, offset, count, OrderType.LIST);
	}

	/**
	 * Creates a new set by copying {@code count} items from the given Ordered, starting at {@code offset} in that Ordered,
	 * into this.
	 *
	 * @param other    another Ordered.OfInt
	 * @param offset   the first index in other's ordering to draw an item from
	 * @param count    how many items to copy from other
	 * @param ordering determines what implementation {@link #order()} will use
	 */
	public IntOrderedSet(Ordered.OfInt other, int offset, int count, OrderType ordering) {
		this(count, Utilities.getDefaultLoadFactor(), ordering);
		addAll(0, other, offset, count);
	}

	/**
	 * Creates a new set using {@code length} items from the given {@code array}, starting at {@code} offset (inclusive).
	 *
	 * @param array  an array to draw items from
	 * @param offset the first index in array to draw an item from
	 * @param length how many items to take from array; bounds-checking is the responsibility of the using code
	 */
	public IntOrderedSet(int[] array, int offset, int length) {
		this(array, offset, length, OrderType.LIST);
	}

	/**
	 * Creates a new set using {@code length} items from the given {@code array}, starting at {@code} offset (inclusive).
	 *
	 * @param array    an array to draw items from
	 * @param offset   the first index in array to draw an item from
	 * @param length   how many items to take from array; bounds-checking is the responsibility of the using code
	 * @param ordering determines what implementation {@link #order()} will use
	 */
	public IntOrderedSet(int[] array, int offset, int length, OrderType ordering) {
		this(length, Utilities.getDefaultLoadFactor(), ordering);
		addAll(array, offset, length);
	}

	/**
	 * Creates a new set that contains all distinct elements in {@code items}.
	 *
	 * @param items an array that will be used in full, except for duplicate items
	 */
	public IntOrderedSet(int[] items) {
		this(items, 0, items.length, OrderType.LIST);
	}

	/**
	 * Creates a new set that contains all distinct elements in {@code items}.
	 *
	 * @param items    an array that will be used in full, except for duplicate items
	 * @param ordering determines what implementation {@link #order()} will use
	 */
	public IntOrderedSet(int[] items, OrderType ordering) {
		this(items, 0, items.length, ordering);
	}

	@Override
	public boolean add(int key) {
		return super.add(key) && items.add(key);
	}

	/**
	 * Sets the key at the specified index. Returns true if the key was not already in the set. If this set already contains the
	 * key, the existing key's index is changed if needed and false is returned. Note, the order of the parameters matches the
	 * order in {@link ObjectList} and the rest of the JDK, not OrderedSet in libGDX.
	 *
	 * @param index where in the iteration order to add the given key, or to move it if already present
	 * @param key   what int item to try to add, if not already present
	 * @return true if the key was added for the first time, or false if the key was already present (even if moved)
	 */
	public boolean add(int index, int key) {
		if (!super.add(key)) {
			int oldIndex = items.indexOf(key);
			if (oldIndex != index) {
				items.insert(index, items.removeAt(oldIndex));
			}
			return false;
		}
		items.insert(index, key);
		return true;
	}

	/**
	 * Adds up to {@code count} items, starting from {@code offset}, in the Ordered {@code other} to this set,
	 * inserting at the end of the iteration order.
	 *
	 * @param other  a non-null {@link Ordered.OfInt} of {@code T}
	 * @param offset the first index in {@code other} to use
	 * @param count  how many indices in {@code other} to use
	 * @return true if this is modified by this call, as {@link #addAll(IntSet)} does
	 */
	public boolean addAll(Ordered.OfInt other, int offset, int count) {
		return addAll(size, other, offset, count);
	}

	/**
	 * Adds up to {@code count} items, starting from {@code offset}, in the Ordered {@code other} to this set,
	 * inserting starting at {@code insertionIndex} in the iteration order.
	 *
	 * @param insertionIndex where to insert into the iteration order
	 * @param other          a non-null {@link Ordered.OfInt}
	 * @param offset         the first index in {@code other} to use
	 * @param count          how many indices in {@code other} to use
	 * @return true if this is modified by this call, as {@link #addAll(IntSet)} does
	 */
	public boolean addAll(int insertionIndex, Ordered.OfInt other, int offset, int count) {
		boolean changed = false;
		int end = Math.min(offset + count, other.size());
		ensureCapacity(end - offset);
		for (int i = offset; i < end; i++) {
			add(insertionIndex++, other.order().get(i));
			changed = true;
		}
		return changed;
	}

	@Override
	public boolean remove(int key) {
		return super.remove(key) && items.remove(key);
	}

	/**
	 * Removes and returns the item at the given index in this set's order.
	 *
	 * @param index the index of the item to remove
	 * @return the removed item
	 */
	public int removeAt(int index) {
		int key = items.removeAt(index);
		super.remove(key);
		return key;
	}

	/**
	 * Removes the items between the specified start index, inclusive, and end index, exclusive.
	 * Note that this takes different arguments than some other range-related methods; this needs
	 * a start index and an end index, rather than a count of items. This matches the behavior in
	 * the JDK collections.
	 *
	 * @param start the first index to remove, inclusive
	 * @param end   the last index (after what should be removed), exclusive
	 */
	@Override
	public void removeRange(int start, int end) {
		start = Math.max(0, start);
		end = Math.min(items.size(), end);
		for (int i = start; i < end; i++) {
			super.remove(items.get(i));
		}
		items.removeRange(start, end);
	}

	@Override
	public int first() {
		if (size == 0)
			throw new IllegalStateException("Cannot get the first() item of an empty IntOrderedSet.");
		return items.items[0];
	}

	/**
	 * Increases the size of the backing array to accommodate the specified number of additional items / loadFactor. Useful before
	 * adding many items to avoid multiple backing array resizes.
	 *
	 * @param additionalCapacity how many additional items this should be able to hold without resizing (probably)
	 */
	@Override
	public void ensureCapacity(int additionalCapacity) {
		int tableSize = tableSize(size + additionalCapacity, loadFactor);
		if (keyTable.length < tableSize) {
			resize(tableSize);
		}
		items.ensureCapacity(additionalCapacity);
	}

	/**
	 * Changes the item {@code before} to {@code after} without changing its position in the order. Returns true if {@code after}
	 * has been added to the ObjectOrderedSet and {@code before} has been removed; returns false if {@code after} is already present or
	 * {@code before} is not present. If you are iterating over an ObjectOrderedSet and have an index, you should prefer
	 * {@link #alterAt(int, int)}, which doesn't need to search for an index like this does and so can be faster.
	 *
	 * @param before an item that must be present for this to succeed
	 * @param after  an item that must not be in this set for this to succeed
	 * @return true if {@code before} was removed and {@code after} was added, false otherwise
	 */
	public boolean alter(int before, int after) {
		if (contains(after)) {
			return false;
		}
		if (!super.remove(before)) {
			return false;
		}
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
	public boolean alterAt(int index, int after) {
		if (index < 0 || index >= size || contains(after)) {
			return false;
		}
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
	public int getAt(int index) {
		return items.get(index);
	}

	@Override
	public void clear(int maximumCapacity) {
		items.clear();
		super.clear(maximumCapacity);
	}

	@Override
	public void clear() {
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
	public IntList order() {
		return items;
	}

	/**
	 * Sorts this ObjectOrderedSet in-place by the keys' natural ordering; {@code T} must implement {@link Comparable}.
	 */
	public void sort() {
		items.sort();
	}

	/**
	 * Reduces the size of the set to the specified size. If the set is already smaller than the specified
	 * size, no action is taken.
	 *
	 * @param newSize the target size to try to reach by removing items, if smaller than the current size
	 */
	@Override
	public void truncate(int newSize) {
		if (size > newSize) {
			removeRange(newSize, size);
		}
	}

	/**
	 * Iterates through items in the same order as {@link #order()}.
	 *
	 * @return an {@link Iterator} over the T items in this, in order
	 */
	@Override
	public IntSetIterator iterator() {
		return new IntOrderedSetIterator(this);
	}

	@Override
	public int hashCode() {
		int h = size;
		// Iterating over the order rather than the key table avoids wasting time on empty entries.
		// The order may be a LongDeque internally, so we cannot just iterate over the internal array.
		IntList order = items;
		for (int i = 0, n = order.size(); i < n; i++) {
			h += order.get(i);
		}
		// Using any bitwise operation can help by keeping results in int range on GWT.
		// This also can improve the low-order bits on problematic item types like Vector2.
		return h ^ h >>> 16;
	}

	public static class IntOrderedSetIterator extends IntSetIterator {
		private final IntList items;

		public IntOrderedSetIterator(IntOrderedSet set) {
			super(set);
			items = set.items;
		}

		@Override
		public void reset() {
			nextIndex = 0;
			hasNext = set.size > 0;
		}

		@Override
		public int nextInt() {
			if (!hasNext) {
				throw new NoSuchElementException();
			}
			int key = items.get(nextIndex);
			nextIndex++;
			hasNext = nextIndex < set.size;
			return key;
		}

		@Override
		public void remove() {
			if (nextIndex < 0) {
				throw new IllegalStateException("next must be called before remove.");
			}
			nextIndex--;
			set.remove(items.get(nextIndex));
		}
	}

	/**
	 * Constructs an empty set.
	 * This is usually less useful than just using the constructor, but can be handy
	 * in some code-generation scenarios when you don't know how many arguments you will have.
	 *
	 * @return a new set containing nothing
	 */
	public static IntOrderedSet with() {
		return new IntOrderedSet(0);
	}

	/**
	 * Creates a new IntOrderedSet that holds only the given item, but can be resized.
	 *
	 * @param item an int item
	 * @return a new IntOrderedSet that holds the given item
	 */
	public static IntOrderedSet with(int item) {
		IntOrderedSet set = new IntOrderedSet(1);
		set.add(item);
		return set;
	}

	/**
	 * Creates a new IntOrderedSet that holds only the given items, but can be resized.
	 *
	 * @param item0 an int item
	 * @param item1 an int item
	 * @return a new IntOrderedSet that holds the given items
	 */
	public static IntOrderedSet with(int item0, int item1) {
		IntOrderedSet set = new IntOrderedSet(2);
		set.add(item0);
		set.add(item1);
		return set;
	}

	/**
	 * Creates a new IntOrderedSet that holds only the given items, but can be resized.
	 *
	 * @param item0 an int item
	 * @param item1 an int item
	 * @param item2 an int item
	 * @return a new IntOrderedSet that holds the given items
	 */
	public static IntOrderedSet with(int item0, int item1, int item2) {
		IntOrderedSet set = new IntOrderedSet(3);
		set.add(item0);
		set.add(item1);
		set.add(item2);
		return set;
	}

	/**
	 * Creates a new IntOrderedSet that holds only the given items, but can be resized.
	 *
	 * @param item0 an int item
	 * @param item1 an int item
	 * @param item2 an int item
	 * @param item3 an int item
	 * @return a new IntOrderedSet that holds the given items
	 */
	public static IntOrderedSet with(int item0, int item1, int item2, int item3) {
		IntOrderedSet set = new IntOrderedSet(4);
		set.add(item0);
		set.add(item1);
		set.add(item2);
		set.add(item3);
		return set;
	}

	/**
	 * Creates a new IntOrderedSet that holds only the given items, but can be resized.
	 *
	 * @param item0 an int item
	 * @param item1 an int item
	 * @param item2 an int item
	 * @param item3 an int item
	 * @param item4 an int item
	 * @return a new IntOrderedSet that holds the given items
	 */
	public static IntOrderedSet with(int item0, int item1, int item2, int item3, int item4) {
		IntOrderedSet set = new IntOrderedSet(5);
		set.add(item0);
		set.add(item1);
		set.add(item2);
		set.add(item3);
		set.add(item4);
		return set;
	}

	/**
	 * Creates a new IntOrderedSet that holds only the given items, but can be resized.
	 *
	 * @param item0 an int item
	 * @param item1 an int item
	 * @param item2 an int item
	 * @param item3 an int item
	 * @param item4 an int item
	 * @param item5 an int item
	 * @return a new IntOrderedSet that holds the given items
	 */
	public static IntOrderedSet with(int item0, int item1, int item2, int item3, int item4, int item5) {
		IntOrderedSet set = new IntOrderedSet(6);
		set.add(item0);
		set.add(item1);
		set.add(item2);
		set.add(item3);
		set.add(item4);
		set.add(item5);
		return set;
	}

	/**
	 * Creates a new IntOrderedSet that holds only the given items, but can be resized.
	 *
	 * @param item0 an int item
	 * @param item1 an int item
	 * @param item2 an int item
	 * @param item3 an int item
	 * @param item4 an int item
	 * @param item5 an int item
	 * @param item6 an int item
	 * @return a new IntOrderedSet that holds the given items
	 */
	public static IntOrderedSet with(int item0, int item1, int item2, int item3, int item4, int item5, int item6) {
		IntOrderedSet set = new IntOrderedSet(7);
		set.add(item0);
		set.add(item1);
		set.add(item2);
		set.add(item3);
		set.add(item4);
		set.add(item5);
		set.add(item6);
		return set;
	}

	/**
	 * Creates a new IntOrderedSet that holds only the given items, but can be resized.
	 *
	 * @param item0 an int item
	 * @param item1 an int item
	 * @param item2 an int item
	 * @param item3 an int item
	 * @param item4 an int item
	 * @param item5 an int item
	 * @param item6 an int item
	 * @return a new IntOrderedSet that holds the given items
	 */
	public static IntOrderedSet with(int item0, int item1, int item2, int item3, int item4, int item5, int item6, int item7) {
		IntOrderedSet set = new IntOrderedSet(8);
		set.add(item0);
		set.add(item1);
		set.add(item2);
		set.add(item3);
		set.add(item4);
		set.add(item5);
		set.add(item6);
		set.add(item7);
		return set;
	}

	/**
	 * Creates a new IntOrderedSet that holds only the given items, but can be resized.
	 * This overload will only be used when an array is supplied and the type of the
	 * items requested is the component type of the array, or if varargs are used and
	 * there are 9 or more arguments.
	 *
	 * @param varargs an int varargs or int array; remember that varargs allocate
	 * @return a new IntOrderedSet that holds the given items
	 */
	public static IntOrderedSet with(int... varargs) {
		return new IntOrderedSet(varargs);
	}

	/**
	 * Calls {@link #parse(String, String, boolean)} with brackets set to false.
	 *
	 * @param str       a String that will be parsed in full
	 * @param delimiter the delimiter between items in str
	 * @return a new collection parsed from str
	 */
	public static IntOrderedSet parse(String str, String delimiter) {
		return parse(str, delimiter, false);
	}

	/**
	 * Creates a new collection and fills it by calling {@link #addLegible(String, String, int, int)} on either all of
	 * {@code str} (if {@code brackets} is false) or {@code str} without its first and last chars (if {@code brackets}
	 * is true). Each item is expected to be separated by {@code delimiter}.
	 *
	 * @param str       a String that will be parsed in full (depending on brackets)
	 * @param delimiter the delimiter between items in str
	 * @param brackets  if true, the first and last chars in str will be ignored
	 * @return a new collection parsed from str
	 */
	public static IntOrderedSet parse(String str, String delimiter, boolean brackets) {
		IntOrderedSet c = new IntOrderedSet();
		if (brackets)
			c.addLegible(str, delimiter, 1, str.length() - 1);
		else
			c.addLegible(str, delimiter);
		return c;
	}

	/**
	 * Creates a new collection and fills it by calling {@link #addLegible(String, String, int, int)} with the given
	 * four parameters as-is.
	 *
	 * @param str       a String that will have the given section parsed
	 * @param delimiter the delimiter between items in str
	 * @param offset    the first position to parse in str, inclusive
	 * @param length    how many chars to parse, starting from offset
	 * @return a new collection parsed from str
	 */
	public static IntOrderedSet parse(String str, String delimiter, int offset, int length) {
		IntOrderedSet c = new IntOrderedSet();
		c.addLegible(str, delimiter, offset, length);
		return c;
	}
}

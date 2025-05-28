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

import com.github.tommyettinger.ds.support.util.LongIterator;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Iterator;
import java.util.NoSuchElementException;

import static com.github.tommyettinger.ds.Utilities.tableSize;

/**
 * A {@link LongSet} that also stores keys in a {@link LongList} using the insertion order. No
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
 * You can customize most behavior of this set by extending it. {@link #place(long)} can be overridden to change how hashCodes
 * are calculated (which can be useful for types like {@link StringBuilder} that don't implement hashCode()).
 * <p>
 * This implementation uses linear probing with the backward shift algorithm for removal.
 * It tries different hashes from a simple family, with the hash changing on resize.
 * Linear probing continues to work even when all hashCodes collide, just more slowly.
 *
 * @author Nathan Sweet
 * @author Tommy Ettinger
 */
public class LongOrderedSet extends LongSet implements Ordered.OfLong {

	protected final LongList items;


	public LongOrderedSet () {
		this(Utilities.getDefaultTableCapacity());
	}

	/**
	 * Creates an IntOrderedSet with the option to use an IntDeque for keeping order.
	 * @param useDequeOrder if true, {@link #order()} will internally be an {@link IntDeque}; otherwise, it will be an {@link IntList}
	 */
	public LongOrderedSet (boolean useDequeOrder) {
		this(Utilities.getDefaultTableCapacity(), Utilities.getDefaultLoadFactor(), useDequeOrder);
	}

	public LongOrderedSet (int initialCapacity, float loadFactor) {
		this(initialCapacity, loadFactor, false);
	}

	public LongOrderedSet (int initialCapacity, float loadFactor, boolean useDequeOrder) {
		super(initialCapacity, loadFactor);
		if(useDequeOrder) items = new LongDeque(initialCapacity);
		else items = new LongList(initialCapacity);
	}

	public LongOrderedSet (int initialCapacity) {
		this(initialCapacity, Utilities.getDefaultLoadFactor(), false);
	}

	/**
	 * Creates a new instance containing the items in the specified iterator.
	 *
	 * @param coll an iterator that will have its remaining contents added to this
	 */
	public LongOrderedSet (LongIterator coll) {
		this();
		addAll(coll);
	}

	public LongOrderedSet (LongOrderedSet set) {
		super(set);
		if(set.items instanceof LongDeque) items = new LongDeque((LongDeque) set.items);
		else items = new LongList(set.items);
	}

	/**
	 * Creates a new set that contains all distinct elements in {@code set}.
	 * @param set a LongSet without an order
	 */
	public LongOrderedSet (LongSet set) {
		this(set, false);
	}

	/**
	 * Creates a new set that contains all distinct elements in {@code set}.
	 * @param set a LongSet without an order
	 * @param useDequeOrder if true, {@link #order()} will internally be an {@link IntDeque}; otherwise, it will be an {@link IntList}
	 */
	public LongOrderedSet (LongSet set, boolean useDequeOrder) {
		this(set.size(), set.loadFactor, useDequeOrder);
		hashMultiplier = set.hashMultiplier;
		addAll(set);
	}

	/**
	 * Creates a new set that contains all distinct elements in {@code coll}.
	 */
	public LongOrderedSet (OfLong coll) {
		this(coll, false);
	}

	/**
	 * Creates a new set that contains all distinct elements in {@code coll}.
	 * @param coll any {@link PrimitiveCollection.OfInt}
	 * @param useDequeOrder if true, {@link #order()} will internally be an {@link IntDeque}; otherwise, it will be an {@link IntList}
	 */
	public LongOrderedSet (OfLong coll, boolean useDequeOrder) {
		this(coll.size(), Utilities.getDefaultLoadFactor(), useDequeOrder);
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
	public LongOrderedSet (Ordered.OfLong other, int offset, int count) {
		this(other, offset, count, false);
	}

	/**
	 * Creates a new set by copying {@code count} items from the given Ordered, starting at {@code offset} in that Ordered,
	 * into this.
	 *
	 * @param other  another Ordered.OfInt
	 * @param offset the first index in other's ordering to draw an item from
	 * @param count  how many items to copy from other
	 * @param useDequeOrder if true, {@link #order()} will internally be an {@link IntDeque}; otherwise, it will be an {@link IntList}
	 */
	public LongOrderedSet (Ordered.OfLong other, int offset, int count, boolean useDequeOrder) {
		this(count, Utilities.getDefaultLoadFactor(), useDequeOrder);
		addAll(0, other, offset, count);
	}

	/**
	 * Creates a new set using {@code length} items from the given {@code array}, starting at {@code} offset (inclusive).
	 *
	 * @param array  an array to draw items from
	 * @param offset the first index in array to draw an item from
	 * @param length how many items to take from array; bounds-checking is the responsibility of the using code
	 */
	public LongOrderedSet (long[] array, int offset, int length) {
		this(array, offset, length, false);
	}

	/**
	 * Creates a new set using {@code length} items from the given {@code array}, starting at {@code} offset (inclusive).
	 *
	 * @param array  an array to draw items from
	 * @param offset the first index in array to draw an item from
	 * @param length how many items to take from array; bounds-checking is the responsibility of the using code
	 * @param useDequeOrder if true, {@link #order()} will internally be an {@link IntDeque}; otherwise, it will be an {@link IntList}
	 */
	public LongOrderedSet (long[] array, int offset, int length, boolean useDequeOrder) {
		this(length, Utilities.getDefaultLoadFactor(), useDequeOrder);
		addAll(array, offset, length);
	}

	/**
	 * Creates a new set that contains all distinct elements in {@code items}.
	 *
	 * @param items an array that will be used in full, except for duplicate items
	 */
	public LongOrderedSet (long[] items) {
		this(items, 0, items.length, false);
	}

	/**
	 * Creates a new set that contains all distinct elements in {@code items}.
	 *
	 * @param items an array that will be used in full, except for duplicate items
	 * @param useDequeOrder if true, {@link #order()} will internally be an {@link IntDeque}; otherwise, it will be an {@link IntList}
	 */
	public LongOrderedSet (long[] items, boolean useDequeOrder) {
		this(items, 0, items.length, useDequeOrder);
	}

	@Override
	public boolean add (long key) {
		return super.add(key) && items.add(key);
	}

	/**
	 * Sets the key at the specified index. Returns true if the key was not already in the set. If this set already contains the
	 * key, the existing key's index is changed if needed and false is returned. Note, the order of the parameters matches the
	 * order in {@link ObjectList} and the rest of the JDK, not OrderedSet in libGDX.
	 *
	 * @param index where in the iteration order to add the given key, or to move it if already present
	 * @param key   what long item to try to add, if not already present
	 * @return true if the key was added for the first time, or false if the key was already present (even if moved)
	 */
	public boolean add (int index, long key) {
		if (!super.add(key)) {
			int oldIndex = items.indexOf(key);
			if (oldIndex != index) {items.insert(index, items.removeAt(oldIndex));}
			return false;
		}
		items.insert(index, key);
		return true;
	}

	/**
	 * Adds up to {@code count} items, starting from {@code offset}, in the Ordered {@code other} to this set,
	 * inserting at the end of the iteration order.
	 *
	 * @param other  a non-null {@link Ordered.OfLong} of {@code T}
	 * @param offset the first index in {@code other} to use
	 * @param count  how many indices in {@code other} to use
	 * @return true if this is modified by this call, as {@link #addAll(LongSet)} does
	 */
	public boolean addAll (Ordered.OfLong other, int offset, int count) {
		return addAll(size, other, offset, count);
	}

	/**
	 * Adds up to {@code count} items, starting from {@code offset}, in the Ordered {@code other} to this set,
	 * inserting starting at {@code insertionIndex} in the iteration order.
	 *
	 * @param insertionIndex where to insert into the iteration order
	 * @param other          a non-null {@link Ordered.OfLong}
	 * @param offset         the first index in {@code other} to use
	 * @param count          how many indices in {@code other} to use
	 * @return true if this is modified by this call, as {@link #addAll(LongSet)} does
	 */
	public boolean addAll (int insertionIndex, Ordered.OfLong other, int offset, int count) {
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
	public boolean remove (long key) {
		return super.remove(key) && items.remove(key);
	}

	/**
	 * Removes and returns the item at the given index in this set's order.
	 *
	 * @param index the index of the item to remove
	 * @return the removed item
	 */
	public long removeAt (int index) {
		long key = items.removeAt(index);
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
	public void removeRange (int start, int end) {
		start = Math.max(0, start);
		end = Math.min(items.size(), end);
		for (int i = start; i < end; i++) {
			super.remove(items.get(i));
		}
		items.removeRange(start, end);
	}

	@Override
	public long first () {
		if (size == 0)
			throw new IllegalStateException("Cannot get the first() item of an empty LongOrderedSet.");
		return items.items[0];
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
		if (keyTable.length < tableSize) {resize(tableSize);}
		items.ensureCapacity(additionalCapacity);
	}

	/**
	 * Changes the item {@code before} to {@code after} without changing its position in the order. Returns true if {@code after}
	 * has been added to the ObjectOrderedSet and {@code before} has been removed; returns false if {@code after} is already present or
	 * {@code before} is not present. If you are iterating over an ObjectOrderedSet and have an index, you should prefer
	 * {@link #alterAt(int, long)}, which doesn't need to search for an index like this does and so can be faster.
	 *
	 * @param before an item that must be present for this to succeed
	 * @param after  an item that must not be in this set for this to succeed
	 * @return true if {@code before} was removed and {@code after} was added, false otherwise
	 */
	public boolean alter (long before, long after) {
		if (contains(after)) {return false;}
		if (!super.remove(before)) {return false;}
		super.add(after);
		items.set(items.indexOf(before), after);
		return true;
	}

	/**
	 * Changes the item at the given {@code index} in the order to {@code after}, without changing the ordering of other items. If
	 * {@code after} is already present, this returns false; it will also return false if {@code index} is invalid for the size of
	 * this set. Otherwise, it returns true. Unlike {@link #alter(long, long)}, this operates in constant time.
	 *
	 * @param index the index in the order of the item to change; must be non-negative and less than {@link #size}
	 * @param after the item that will replace the contents at {@code index}; this item must not be present for this to succeed
	 * @return true if {@code after} successfully replaced the contents at {@code index}, false otherwise
	 */
	public boolean alterAt (int index, long after) {
		if (index < 0 || index >= size || contains(after)) {return false;}
		super.remove(items.get(index));
		super.add(after);
		items.set(index, after);
		return true;
	}

	/**
	 * Gets the long item at the given {@code index} in the insertion order. The index should be between 0
	 * (inclusive) and {@link #size()} (exclusive).
	 *
	 * @param index an index in the insertion order, between 0 (inclusive) and {@link #size()} (exclusive)
	 * @return the item at the given index
	 */
	public long getAt (int index) {
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
	public LongList order () {
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
	 * use {@link LongOrderedSetIterator#LongOrderedSetIterator(LongOrderedSet)} to nest iterators.
	 *
	 * @return an {@link Iterator} over the T items in this, in order
	 */
	@Override
	public LongSetIterator iterator () {
		if (iterator1 == null || iterator2 == null) {
			iterator1 = new LongOrderedSetIterator(this);
			iterator2 = new LongOrderedSetIterator(this);
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

	@Override
	public int hashCode() {
		int h = size;
		// Iterating over the order rather than the key table avoids wasting time on empty entries.
		// The order may be a LongDeque internally, so we cannot just iterate over the internal array.
		LongList order = items;
		for (int i = 0, n = order.size(); i < n; i++) {
			long key = order.get(i);
			h += (int)(key ^ key >>> 32);
		}
		// Using any bitwise operation can help by keeping results in int range on GWT.
		// This also can improve the low-order bits on problematic item types like Vector2.
		return h ^ h >>> 16;
	}

	public String toString (String separator) {
		if (size == 0) {return "{}";}
		LongList items = this.items;
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

	/**
	 * Reduces the size of the set to the specified size. If the set is already smaller than the specified
	 * size, no action is taken.
	 *
	 * @param newSize the target size to try to reach by removing items, if smaller than the current size
	 */
	@Override
	public void truncate (int newSize) {
		if (size > newSize) {removeRange(newSize, size);}
	}

	public static class LongOrderedSetIterator extends LongSet.LongSetIterator {
		private final LongList items;

		public LongOrderedSetIterator (LongOrderedSet set) {
			super(set);
			items = set.items;
		}

		@Override
		public void reset () {
			nextIndex = 0;
			hasNext = set.size > 0;
		}

		@Override
		public long nextLong () {
			if (!hasNext) {throw new NoSuchElementException();}
			if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
			long key = items.get(nextIndex);
			nextIndex++;
			hasNext = nextIndex < set.size;
			return key;
		}

		@Override
		public void remove () {
			if (nextIndex < 0) {throw new IllegalStateException("next must be called before remove.");}
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
	public static LongOrderedSet with () {
		return new LongOrderedSet(0);
	}

	/**
	 * Creates a new LongOrderedSet that holds only the given item, but can be resized.
	 * @param item a long item
	 * @return a new LongOrderedSet that holds the given item
	 */
	public static LongOrderedSet with (long item) {
		LongOrderedSet set = new LongOrderedSet(1);
		set.add(item);
		return set;
	}

	/**
	 * Creates a new LongOrderedSet that holds only the given items, but can be resized.
	 * @param item0 a long item
	 * @param item1 a long item
	 * @return a new LongOrderedSet that holds the given items
	 */
	public static LongOrderedSet with (long item0, long item1) {
		LongOrderedSet set = new LongOrderedSet(2);
		set.add(item0);
		set.add(item1);
		return set;
	}

	/**
	 * Creates a new LongOrderedSet that holds only the given items, but can be resized.
	 * @param item0 a long item
	 * @param item1 a long item
	 * @param item2 a long item
	 * @return a new LongOrderedSet that holds the given items
	 */
	public static LongOrderedSet with (long item0, long item1, long item2) {
		LongOrderedSet set = new LongOrderedSet(3);
		set.add(item0);
		set.add(item1);
		set.add(item2);
		return set;
	}

	/**
	 * Creates a new LongOrderedSet that holds only the given items, but can be resized.
	 * @param item0 a long item
	 * @param item1 a long item
	 * @param item2 a long item
	 * @param item3 a long item
	 * @return a new LongOrderedSet that holds the given items
	 */
	public static LongOrderedSet with (long item0, long item1, long item2, long item3) {
		LongOrderedSet set = new LongOrderedSet(4);
		set.add(item0);
		set.add(item1);
		set.add(item2);
		set.add(item3);
		return set;
	}

	/**
	 * Creates a new LongOrderedSet that holds only the given items, but can be resized.
	 * @param item0 a long item
	 * @param item1 a long item
	 * @param item2 a long item
	 * @param item3 a long item
	 * @param item4 a long item
	 * @return a new LongOrderedSet that holds the given items
	 */
	public static LongOrderedSet with (long item0, long item1, long item2, long item3, long item4) {
		LongOrderedSet set = new LongOrderedSet(5);
		set.add(item0);
		set.add(item1);
		set.add(item2);
		set.add(item3);
		set.add(item4);
		return set;
	}

	/**
	 * Creates a new LongOrderedSet that holds only the given items, but can be resized.
	 * @param item0 a long item
	 * @param item1 a long item
	 * @param item2 a long item
	 * @param item3 a long item
	 * @param item4 a long item
	 * @param item5 a long item
	 * @return a new LongOrderedSet that holds the given items
	 */
	public static LongOrderedSet with (long item0, long item1, long item2, long item3, long item4, long item5) {
		LongOrderedSet set = new LongOrderedSet(6);
		set.add(item0);
		set.add(item1);
		set.add(item2);
		set.add(item3);
		set.add(item4);
		set.add(item5);
		return set;
	}

	/**
	 * Creates a new LongOrderedSet that holds only the given items, but can be resized.
	 * @param item0 a long item
	 * @param item1 a long item
	 * @param item2 a long item
	 * @param item3 a long item
	 * @param item4 a long item
	 * @param item5 a long item
	 * @param item6 a long item
	 * @return a new LongOrderedSet that holds the given items
	 */
	public static LongOrderedSet with (long item0, long item1, long item2, long item3, long item4, long item5, long item6) {
		LongOrderedSet set = new LongOrderedSet(7);
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
	 * Creates a new LongOrderedSet that holds only the given items, but can be resized.
	 * @param item0 a long item
	 * @param item1 a long item
	 * @param item2 a long item
	 * @param item3 a long item
	 * @param item4 a long item
	 * @param item5 a long item
	 * @param item6 a long item
	 * @return a new LongOrderedSet that holds the given items
	 */
	public static LongOrderedSet with (long item0, long item1, long item2, long item3, long item4, long item5, long item6, long item7) {
		LongOrderedSet set = new LongOrderedSet(8);
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
	 * Creates a new LongOrderedSet that holds only the given items, but can be resized.
	 * This overload will only be used when an array is supplied and the type of the
	 * items requested is the component type of the array, or if varargs are used and
	 * there are 9 or more arguments.
	 * @param varargs a long varargs or long array; remember that varargs allocate
	 * @return a new LongOrderedSet that holds the given items
	 */
	public static LongOrderedSet with (long... varargs) {
		return new LongOrderedSet(varargs);
	}
}

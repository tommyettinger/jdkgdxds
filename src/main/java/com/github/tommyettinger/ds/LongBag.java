/*
 * Copyright (c) 2023 See AUTHORS file.
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

package com.github.tommyettinger.ds;

import com.github.tommyettinger.ds.support.util.LongIterator;

import java.util.List;

/**
 * An unordered List of long items. This allows efficient iteration via a reused iterator or via index.
 * This class avoids a memory copy when removing elements (the last element is moved to the removed element's position).
 * Items are permitted to change position in the ordering when any item is removed or added.
 * Although this won't keep an order during modifications, you can {@link #sort()} the bag to ensure,
 * if no modifications are made later, that the iteration will happen in sorted order.
 */
public class LongBag extends LongList {
	/**
	 * Returns true if this implementation retains order, which it does not.
	 *
	 * @return false
	 */
	@Override
	public boolean keepsOrder () {
		return false;
	}

	/**
	 * Creates an ordered bag with a capacity of 10.
	 */
	public LongBag () {
		super();
	}

	/**
	 * Creates an ordered bag with the specified capacity.
	 *
	 * @param capacity
	 */
	public LongBag (int capacity) {
		super(capacity);
	}

	/**
	 * Creates a new bag containing the elements in the specific list or bag. The capacity is set to the number of elements,
	 * so any subsequent elements added will cause the backing array to be grown.
	 *
	 * @param list another LongList or LongBag
	 */
	public LongBag (LongList list) {
		super(list);
	}

	/**
	 * Creates a new bag containing the elements in the specified array. The capacity is set to the number of elements,
	 * so any subsequent elements added will cause the backing array to be grown.
	 *
	 * @param array a non-null long array to add to this bag
	 */
	public LongBag (long[] array) {
		super(array);
	}

	/**
	 * Creates a new bag containing the elements in the specified array. The capacity is set to the number of elements, so any
	 * subsequent elements added will cause the backing array to be grown.
	 *
	 * @param array a non-null long array to add to this bag
	 * @param startIndex the first index in {@code array} to use
	 * @param count how many items to use from {@code array}
	 */
	public LongBag (long[] array, int startIndex, int count) {
		super(array, startIndex, count);
	}

	/**
	 * Creates a new bag containing the items in the specified PrimitiveCollection.OfLong.
	 *
	 * @param coll a primitive collection that will have its contents added to this
	 */
	public LongBag (OfLong coll) {
		super(coll);
	}

	/**
	 * Creates a new instance containing the items in the specified iterator.
	 *
	 * @param coll an iterator that will have its remaining contents added to this
	 */
	public LongBag (LongIterator coll) {
		this();
		addAll(coll);
	}

	/**
	 * Copies the given Ordered.OfLong into a new bag.
	 *
	 * @param other another Ordered.OfLong
	 */
	public LongBag (Ordered.OfLong other) {
		super(other);
	}

	/**
	 * Creates a new bag by copying {@code count} items from the given Ordered, starting at {@code offset} in that Ordered,
	 * into this.
	 *
	 * @param other  another Ordered.OfLong
	 * @param offset the first index in other's ordering to draw an item from
	 * @param count  how many items to copy from other
	 */
	public LongBag (Ordered.OfLong other, int offset, int count) {
		super(other, offset, count);
	}

	/**
	 * This always adds {@code element} to the end of this bag's ordering.
	 * @param index ignored
	 * @param element element to be inserted
	 */
	@Override
	public void insert (int index, long element) {
		if (index > size) {throw new IndexOutOfBoundsException("index can't be > size: " + index + " > " + size);}
		long[] items = this.items;
		if (size == items.length) {items = resize(Math.max(8, (int)(size * 1.75f)));}
		items[size] = element;
		++size;
	}

	/**
	 * Removes and returns the item at the specified index.
	 * Note that this is equivalent to {@link List#remove(int)}, but can't have that name because
	 * we also have {@link #remove(long)} that removes a value, rather than an index.
	 *
	 * @param index the index of the item to remove and return
	 * @return the removed item
	 */
	@Override
	public long removeAt (int index) {
		if (index >= size) {throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);}
		long[] items = this.items;
		long value = items[index];
		size--;
		items[index] = items[size];
		return value;
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
		int n = size;
		if (end > n) {throw new IndexOutOfBoundsException("end can't be > size: " + end + " > " + size);}
		if (start > end) {throw new IndexOutOfBoundsException("start can't be > end: " + start + " > " + end);}
		int count = end - start, lastIndex = n - count;
		int i = Math.max(lastIndex, end);
		System.arraycopy(items, i, items, start, n - i);
		size = n - count;
	}

	@Override
	public int hashCode () {
		long[] items = this.items;
		long h = 1;
		for (int i = 0, n = size; i < n; i++) {
			h += items[i];
		}
		return (int)(h ^ h >>> 32);
	}

	/**
	 * Constructs an empty bag.
	 * This is usually less useful than just using the constructor, but can be handy
	 * in some code-generation scenarios when you don't know how many arguments you will have.
	 *
	 * @return a new bag containing nothing
	 */
	public static LongBag with () {
		return new LongBag(0);
	}

	/**
	 * Creates a new LongBag that holds only the given item, but can be resized.
	 * @param item a long item
	 * @return a new LongBag that holds the given item
	 */
	public static LongBag with (long item) {
		LongBag bag = new LongBag(1);
		bag.add(item);
		return bag;
	}

	/**
	 * Creates a new LongBag that holds only the given items, but can be resized.
	 * @param item0 a long item
	 * @param item1 a long item
	 * @return a new LongBag that holds the given items
	 */
	public static LongBag with (long item0, long item1) {
		LongBag bag = new LongBag(2);
		bag.add(item0);
		bag.add(item1);
		return bag;
	}

	/**
	 * Creates a new LongBag that holds only the given items, but can be resized.
	 * @param item0 a long item
	 * @param item1 a long item
	 * @param item2 a long item
	 * @return a new LongBag that holds the given items
	 */
	public static LongBag with (long item0, long item1, long item2) {
		LongBag bag = new LongBag(3);
		bag.add(item0);
		bag.add(item1);
		bag.add(item2);
		return bag;
	}

	/**
	 * Creates a new LongBag that holds only the given items, but can be resized.
	 * @param item0 a long item
	 * @param item1 a long item
	 * @param item2 a long item
	 * @param item3 a long item
	 * @return a new LongBag that holds the given items
	 */
	public static LongBag with (long item0, long item1, long item2, long item3) {
		LongBag bag = new LongBag(4);
		bag.add(item0);
		bag.add(item1);
		bag.add(item2);
		bag.add(item3);
		return bag;
	}

	/**
	 * Creates a new LongBag that holds only the given items, but can be resized.
	 * @param item0 a long item
	 * @param item1 a long item
	 * @param item2 a long item
	 * @param item3 a long item
	 * @param item4 a long item
	 * @return a new LongBag that holds the given items
	 */
	public static LongBag with (long item0, long item1, long item2, long item3, long item4) {
		LongBag bag = new LongBag(5);
		bag.add(item0);
		bag.add(item1);
		bag.add(item2);
		bag.add(item3);
		bag.add(item4);
		return bag;
	}

	/**
	 * Creates a new LongBag that holds only the given items, but can be resized.
	 * @param item0 a long item
	 * @param item1 a long item
	 * @param item2 a long item
	 * @param item3 a long item
	 * @param item4 a long item
	 * @param item5 a long item
	 * @return a new LongBag that holds the given items
	 */
	public static LongBag with (long item0, long item1, long item2, long item3, long item4, long item5) {
		LongBag bag = new LongBag(6);
		bag.add(item0);
		bag.add(item1);
		bag.add(item2);
		bag.add(item3);
		bag.add(item4);
		bag.add(item5);
		return bag;
	}

	/**
	 * Creates a new LongBag that holds only the given items, but can be resized.
	 * @param item0 a long item
	 * @param item1 a long item
	 * @param item2 a long item
	 * @param item3 a long item
	 * @param item4 a long item
	 * @param item5 a long item
	 * @param item6 a long item
	 * @return a new LongBag that holds the given items
	 */
	public static LongBag with (long item0, long item1, long item2, long item3, long item4, long item5, long item6) {
		LongBag bag = new LongBag(7);
		bag.add(item0);
		bag.add(item1);
		bag.add(item2);
		bag.add(item3);
		bag.add(item4);
		bag.add(item5);
		bag.add(item6);
		return bag;
	}

	/**
	 * Creates a new LongBag that holds only the given items, but can be resized.
	 * @param item0 a long item
	 * @param item1 a long item
	 * @param item2 a long item
	 * @param item3 a long item
	 * @param item4 a long item
	 * @param item5 a long item
	 * @param item6 a long item
	 * @return a new LongBag that holds the given items
	 */
	public static LongBag with (long item0, long item1, long item2, long item3, long item4, long item5, long item6, long item7) {
		LongBag bag = new LongBag(8);
		bag.add(item0);
		bag.add(item1);
		bag.add(item2);
		bag.add(item3);
		bag.add(item4);
		bag.add(item5);
		bag.add(item6);
		bag.add(item7);
		return bag;
	}

	/**
	 * Creates a new LongBag that holds only the given items, but can be resized.
	 * This overload will only be used when an array is supplied and the type of the
	 * items requested is the component type of the array, or if varargs are used and
	 * there are 9 or more arguments.
	 * @param varargs a long varargs or long array; remember that varargs allocate
	 * @return a new LongBag that holds the given items
	 */
	public static LongBag with (long... varargs) {
		return new LongBag(varargs);
	}
}

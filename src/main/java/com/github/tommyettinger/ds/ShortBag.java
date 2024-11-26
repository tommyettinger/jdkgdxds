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

import com.github.tommyettinger.ds.support.util.ShortIterator;

import java.util.List;
/**
 * An unordered List of short items. This allows efficient iteration via a reused iterator or via index.
 * This class avoids a memory copy when removing elements (the last element is moved to the removed element's position).
 * Items are permitted to change position in the ordering when any item is removed or added.
 * Although this won't keep an order during modifications, you can {@link #sort()} the bag to ensure,
 * if no modifications are made later, that the iteration will happen in sorted order.
 */
public class ShortBag extends ShortList {
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
	public ShortBag () {
		super();
	}

	/**
	 * Creates an ordered bag with the specified capacity.
	 *
	 * @param capacity
	 */
	public ShortBag (int capacity) {
		super(capacity);
	}

	/**
	 * Creates a new bag containing the elements in the specific list or bag. The capacity is set to the number of elements,
	 * so any subsequent elements added will cause the backing array to be grown.
	 *
	 * @param list another ShortList or ShortBag
	 */
	public ShortBag (ShortList list) {
		super(list);
	}

	/**
	 * Creates a new bag containing the elements in the specified array. The capacity is set to the number of elements,
	 * so any subsequent elements added will cause the backing array to be grown.
	 *
	 * @param array a non-null short array to add to this bag
	 */
	public ShortBag (short[] array) {
		super(array);
	}

	/**
	 * Creates a new bag containing the elements in the specified array. The capacity is set to the number of elements, so any
	 * subsequent elements added will cause the backing array to be grown.
	 *
	 * @param array a non-null short array to add to this bag
	 * @param startIndex the first index in {@code array} to use
	 * @param count how many items to use from {@code array}
	 */
	public ShortBag (short[] array, int startIndex, int count) {
		super(array, startIndex, count);
	}

	/**
	 * Creates a new bag containing the items in the specified PrimitiveCollection.OfShort.
	 *
	 * @param coll a primitive collection that will have its contents added to this
	 */
	public ShortBag (OfShort coll) {
		super(coll);
	}

	/**
	 * Creates a new instance containing the items in the specified iterator.
	 *
	 * @param coll an iterator that will have its remaining contents added to this
	 */
	public ShortBag (ShortIterator coll) {
		this();
		addAll(coll);
	}

	/**
	 * Copies the given Ordered.OfShort into a new bag.
	 *
	 * @param other another Ordered.OfShort
	 */
	public ShortBag (Ordered.OfShort other) {
		super(other);
	}

	/**
	 * Creates a new bag by copying {@code count} items from the given Ordered, starting at {@code offset} in that Ordered,
	 * into this.
	 *
	 * @param other  another Ordered.OfShort
	 * @param offset the first index in other's ordering to draw an item from
	 * @param count  how many items to copy from other
	 */
	public ShortBag (Ordered.OfShort other, int offset, int count) {
		super(other, offset, count);
	}

	/**
	 * This always adds {@code element} to the end of this bag's ordering.
	 * @param index ignored
	 * @param element element to be inserted
	 */
	@Override
	public void insert (int index, short element) {
		if (index > size) {throw new IndexOutOfBoundsException("index can't be > size: " + index + " > " + size);}
		short[] items = this.items;
		if (size == items.length) {items = resize(Math.max(8, (int)(size * 1.75f)));}
		items[size] = element;
		++size;
	}

	/**
	 * Removes and returns the item at the specified index.
	 * Note that this is equivalent to {@link List#remove(int)}, but can't have that name because
	 * we also have {@link #remove(short)} that removes a value, rather than an index.
	 *
	 * @param index the index of the item to remove and return
	 * @return the removed item
	 */
	@Override
	public short removeAt (int index) {
		if (index >= size) {throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);}
		short[] items = this.items;
		short value = items[index];
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
		short[] items = this.items;
		int h = 1;
		for (int i = 0, n = size; i < n; i++) {
			h += items[i];
		}
		return h;
	}

	/**
	 * Constructs an empty bag.
	 * This is usually less useful than just using the constructor, but can be handy
	 * in some code-generation scenarios when you don't know how many arguments you will have.
	 *
	 * @return a new bag containing nothing
	 */
	public static ShortBag with () {
		return new ShortBag(0);
	}

	/**
	 * Creates a new ShortBag that holds only the given item, but can be resized.
	 * @param item a short item
	 * @return a new ShortBag that holds the given item
	 */

	public static ShortBag with (short item) {
		ShortBag bag = new ShortBag(1);
		bag.add(item);
		return bag;
	}

	/**
	 * Creates a new ShortBag that holds only the given items, but can be resized.
	 * @param item0 a short item
	 * @param item1 a short item
	 * @return a new ShortBag that holds the given items
	 */
	public static ShortBag with (short item0, short item1) {
		ShortBag bag = new ShortBag(2);
		bag.add(item0);
		bag.add(item1);
		return bag;
	}

	/**
	 * Creates a new ShortBag that holds only the given items, but can be resized.
	 * @param item0 a short item
	 * @param item1 a short item
	 * @param item2 a short item
	 * @return a new ShortBag that holds the given items
	 */
	public static ShortBag with (short item0, short item1, short item2) {
		ShortBag bag = new ShortBag(3);
		bag.add(item0);
		bag.add(item1);
		bag.add(item2);
		return bag;
	}

	/**
	 * Creates a new ShortBag that holds only the given items, but can be resized.
	 * @param item0 a short item
	 * @param item1 a short item
	 * @param item2 a short item
	 * @param item3 a short item
	 * @return a new ShortBag that holds the given items
	 */
	public static ShortBag with (short item0, short item1, short item2, short item3) {
		ShortBag bag = new ShortBag(4);
		bag.add(item0);
		bag.add(item1);
		bag.add(item2);
		bag.add(item3);
		return bag;
	}

	/**
	 * Creates a new ShortBag that holds only the given items, but can be resized.
	 * @param item0 a short item
	 * @param item1 a short item
	 * @param item2 a short item
	 * @param item3 a short item
	 * @param item4 a short item
	 * @return a new ShortBag that holds the given items
	 */
	public static ShortBag with (short item0, short item1, short item2, short item3, short item4) {
		ShortBag bag = new ShortBag(5);
		bag.add(item0);
		bag.add(item1);
		bag.add(item2);
		bag.add(item3);
		bag.add(item4);
		return bag;
	}

	/**
	 * Creates a new ShortBag that holds only the given items, but can be resized.
	 * @param item0 a short item
	 * @param item1 a short item
	 * @param item2 a short item
	 * @param item3 a short item
	 * @param item4 a short item
	 * @param item5 a short item
	 * @return a new ShortBag that holds the given items
	 */
	public static ShortBag with (short item0, short item1, short item2, short item3, short item4, short item5) {
		ShortBag bag = new ShortBag(6);
		bag.add(item0);
		bag.add(item1);
		bag.add(item2);
		bag.add(item3);
		bag.add(item4);
		bag.add(item5);
		return bag;
	}

	/**
	 * Creates a new ShortBag that holds only the given items, but can be resized.
	 * @param item0 a short item
	 * @param item1 a short item
	 * @param item2 a short item
	 * @param item3 a short item
	 * @param item4 a short item
	 * @param item5 a short item
	 * @param item6 a short item
	 * @return a new ShortBag that holds the given items
	 */
	public static ShortBag with (short item0, short item1, short item2, short item3, short item4, short item5, short item6) {
		ShortBag bag = new ShortBag(7);
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
	 * Creates a new ShortBag that holds only the given items, but can be resized.
	 * @param item0 a short item
	 * @param item1 a short item
	 * @param item2 a short item
	 * @param item3 a short item
	 * @param item4 a short item
	 * @param item5 a short item
	 * @param item6 a short item
	 * @return a new ShortBag that holds the given items
	 */
	public static ShortBag with (short item0, short item1, short item2, short item3, short item4, short item5, short item6, short item7) {
		ShortBag bag = new ShortBag(8);
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
	 * Creates a new ShortBag that holds only the given items, but can be resized.
	 * This overload will only be used when an array is supplied and the type of the
	 * items requested is the component type of the array, or if varargs are used and
	 * there are 9 or more arguments.
	 * @param varargs a short varargs or short array; remember that varargs allocate
	 * @return a new ShortBag that holds the given items
	 */
	public static ShortBag with (short... varargs) {
		return new ShortBag(varargs);
	}
}

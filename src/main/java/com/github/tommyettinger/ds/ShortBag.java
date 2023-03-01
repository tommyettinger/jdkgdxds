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

import java.util.List;
/**
 * An unordered List of short items. This allows efficient iteration via a reused iterator or via index.
 * This class avoids a memory copy when removing elements (the last element is moved to the removed element's position).
 * Items are permitted to change position in the ordering when any item is removed or added.
 * Although this won't keep an order during modifications, you can {@link #sort()} the bag to ensure,
 * if no modifications are made after, that the iteration will happen in sorted order.
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
	 * Creates an ordered array with a capacity of 10.
	 */
	public ShortBag () {
		super();
	}

	/**
	 * Creates an ordered array with the specified capacity.
	 *
	 * @param capacity
	 */
	public ShortBag (int capacity) {
		super(capacity);
	}

	/**
	 * Creates a new list containing the elements in the specific array. The new array will be ordered if the specific array is
	 * ordered. The capacity is set to the number of elements, so any subsequent elements added will cause the backing array to be
	 * grown.
	 *
	 * @param array
	 */
	public ShortBag (ShortList array) {
		super(array);
	}

	/**
	 * Creates a new ordered array containing the elements in the specified array. The capacity is set to the number of elements,
	 * so any subsequent elements added will cause the backing array to be grown.
	 *
	 * @param array
	 */
	public ShortBag (short[] array) {
		super(array);
	}

	/**
	 * Creates a new list containing the elements in the specified array. The capacity is set to the number of elements, so any
	 * subsequent elements added will cause the backing array to be grown.
	 *
	 * @param array
	 * @param startIndex
	 * @param count
	 */
	public ShortBag (short[] array, int startIndex, int count) {
		super(array, startIndex, count);
	}

	/**
	 * Creates a new list containing the items in the specified PrimitiveCollection.OfShort. Only this class currently implements
	 * that interface, but user code can as well.
	 *
	 * @param coll a primitive collection that will have its contents added to this
	 */
	public ShortBag (OfShort coll) {
		super(coll);
	}

	/**
	 * Copies the given Ordered.OfShort into a new ShortList.
	 *
	 * @param other another Ordered.OfShort
	 */
	public ShortBag (Ordered.OfShort other) {
		super(other);
	}

	/**
	 * Creates a new list by copying {@code count} items from the given Ordered, starting at {@code offset} in that Ordered,
	 * into this.
	 *
	 * @param other  another Ordered.OfShort
	 * @param offset the first index in other's ordering to draw an item from
	 * @param count  how many items to copy from other
	 */
	public ShortBag (Ordered.OfShort other, int offset, int count) {
		super(other, offset, count);
	}

	@Override
	public void insert (int index, short value) {
		if (index > size) {throw new IndexOutOfBoundsException("index can't be > size: " + index + " > " + size);}
		short[] items = this.items;
		if (size == items.length) {items = resize(Math.max(8, (int)(size * 1.75f)));}
		items[size] = items[index];
		size++;
		items[index] = value;
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
		if (end >= n) {throw new IndexOutOfBoundsException("end can't be >= size: " + end + " >= " + size);}
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

	public static ShortBag with (short item) {
		ShortBag list = new ShortBag(1);
		list.add(item);
		return list;
	}

	/**
	 * @see #ShortBag(short[])
	 */
	public static ShortBag with (short... array) {
		return new ShortBag(array);
	}

}

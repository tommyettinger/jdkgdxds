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

import com.github.tommyettinger.digital.BitConversion;

import java.util.List;

/**
 * An unordered List of double items. This allows efficient iteration via a reused iterator or via index.
 * This class avoids a memory copy when removing elements (the last element is moved to the removed element's position).
 * Items are permitted to change position in the ordering when any item is removed or added.
 * Although this won't keep an order during modifications, you can {@link #sort()} the bag to ensure,
 * if no modifications are made later, that the iteration will happen in sorted order.
 */
public class DoubleBag extends DoubleList {
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
	public DoubleBag () {
		super();
	}

	/**
	 * Creates an ordered bag with the specified capacity.
	 *
	 * @param capacity
	 */
	public DoubleBag (int capacity) {
		super(capacity);
	}

	/**
	 * Creates a new bag containing the elements in the specific list or bag. The capacity is set to the number of elements,
	 * so any subsequent elements added will cause the backing array to be grown.
	 *
	 * @param list another DoubleList or DoubleBag
	 */
	public DoubleBag (DoubleList list) {
		super(list);
	}

	/**
	 * Creates a new bag containing the elements in the specified array. The capacity is set to the number of elements,
	 * so any subsequent elements added will cause the backing array to be grown.
	 *
	 * @param array a non-null double array to add to this bag
	 */
	public DoubleBag (double[] array) {
		super(array);
	}

	/**
	 * Creates a new bag containing the elements in the specified array. The capacity is set to the number of elements, so any
	 * subsequent elements added will cause the backing array to be grown.
	 *
	 * @param array a non-null double array to add to this bag
	 * @param startIndex the first index in {@code array} to use
	 * @param count how many items to use from {@code array}
	 */
	public DoubleBag (double[] array, int startIndex, int count) {
		super(array, startIndex, count);
	}

	/**
	 * Creates a new bag containing the items in the specified PrimitiveCollection.OfDouble.
	 *
	 * @param coll a primitive collection that will have its contents added to this
	 */
	public DoubleBag (OfDouble coll) {
		super(coll);
	}

	/**
	 * Copies the given Ordered.OfDouble into a new bag.
	 *
	 * @param other another Ordered.OfDouble
	 */
	public DoubleBag (Ordered.OfDouble other) {
		super(other);
	}

	/**
	 * Creates a new bag by copying {@code count} items from the given Ordered, starting at {@code offset} in that Ordered,
	 * into this.
	 *
	 * @param other  another Ordered.OfDouble
	 * @param offset the first index in other's ordering to draw an item from
	 * @param count  how many items to copy from other
	 */
	public DoubleBag (Ordered.OfDouble other, int offset, int count) {
		super(other, offset, count);
	}

	/**
	 * This always adds {@code element} to the end of this bag's ordering.
	 * @param index ignored
	 * @param element element to be inserted
	 */
	@Override
	public void insert (int index, double element) {
		if (index > size) {throw new IndexOutOfBoundsException("index can't be > size: " + index + " > " + size);}
		double[] items = this.items;
		if (size == items.length) {items = resize(Math.max(8, (int)(size * 1.75f)));}
		items[size] = element;
		++size;
	}

	/**
	 * Removes and returns the item at the specified index.
	 * Note that this is equivalent to {@link List#remove(int)}, but can't have that name because
	 * we also have {@link #remove(double)} that removes a value, rather than an index.
	 *
	 * @param index the index of the item to remove and return
	 * @return the removed item
	 */
	@Override
	public double removeAt (int index) {
		if (index >= size) {throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);}
		double[] items = this.items;
		double value = items[index];
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
		double[] items = this.items;
		int h = 1;
		for (int i = 0, n = size; i < n; i++) {
			h += BitConversion.doubleToMixedIntBits(items[i]);
		}
		return h;
	}

	public static DoubleBag with (double item) {
		DoubleBag list = new DoubleBag(1);
		list.add(item);
		return list;
	}

	/**
	 * @see #DoubleBag(double[])
	 */
	public static DoubleBag with (double... array) {
		return new DoubleBag(array);
	}

}

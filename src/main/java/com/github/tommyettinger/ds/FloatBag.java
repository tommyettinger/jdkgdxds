/*
 * Copyright (c) 2023-2025 See AUTHORS file.
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

import com.github.tommyettinger.digital.BitConversion;
import com.github.tommyettinger.ds.support.util.FloatIterator;

import java.util.List;

/**
 * An unordered List of float items. This allows efficient iteration via a reused iterator or via index.
 * This class avoids a memory copy when removing elements (the last element is moved to the removed element's position).
 * Items are permitted to change position in the ordering when any item is removed or added.
 * Although this won't keep an order during modifications, you can {@link #sort()} the bag to ensure,
 * if no modifications are made later, that the iteration will happen in sorted order.
 */
public class FloatBag extends FloatList {
	/**
	 * Returns true if this implementation retains order, which it does not.
	 *
	 * @return false
	 */
	@Override
	public boolean keepsOrder() {
		return false;
	}

	/**
	 * Creates an ordered bag with a capacity of 10.
	 */
	public FloatBag() {
		super();
	}

	/**
	 * Creates an ordered bag with the specified capacity.
	 *
	 * @param capacity
	 */
	public FloatBag(int capacity) {
		super(capacity);
	}

	/**
	 * Creates a new bag containing the elements in the specific list or bag. The capacity is set to the number of elements,
	 * so any subsequent elements added will cause the backing array to be grown.
	 *
	 * @param list another FloatList or FloatBag
	 */
	public FloatBag(FloatList list) {
		super(list);
	}

	/**
	 * Creates a new bag containing the elements in the specified array. The capacity is set to the number of elements,
	 * so any subsequent elements added will cause the backing array to be grown.
	 *
	 * @param array a non-null float array to add to this bag
	 */
	public FloatBag(float[] array) {
		super(array);
	}

	/**
	 * Creates a new bag containing the elements in the specified array. The capacity is set to the number of elements, so any
	 * subsequent elements added will cause the backing array to be grown.
	 *
	 * @param array      a non-null float array to add to this bag
	 * @param startIndex the first index in {@code array} to use
	 * @param count      how many items to use from {@code array}
	 */
	public FloatBag(float[] array, int startIndex, int count) {
		super(array, startIndex, count);
	}

	/**
	 * Creates a new bag containing the items in the specified PrimitiveCollection.OfFloat.
	 *
	 * @param coll a primitive collection that will have its contents added to this
	 */
	public FloatBag(OfFloat coll) {
		super(coll);
	}

	/**
	 * Creates a new instance containing the items in the specified iterator.
	 *
	 * @param coll an iterator that will have its remaining contents added to this
	 */
	public FloatBag(FloatIterator coll) {
		this();
		addAll(coll);
	}

	/**
	 * Copies the given Ordered.OfFloat into a new bag.
	 *
	 * @param other another Ordered.OfFloat
	 */
	public FloatBag(Ordered.OfFloat other) {
		super(other);
	}

	/**
	 * Creates a new bag by copying {@code count} items from the given Ordered, starting at {@code offset} in that Ordered,
	 * into this.
	 *
	 * @param other  another Ordered.OfFloat
	 * @param offset the first index in other's ordering to draw an item from
	 * @param count  how many items to copy from other
	 */
	public FloatBag(Ordered.OfFloat other, int offset, int count) {
		super(other, offset, count);
	}

	/**
	 * This always adds {@code element} to the end of this bag's ordering.
	 *
	 * @param index   ignored
	 * @param element element to be inserted
	 */
	@Override
	public void insert(int index, float element) {
		if (index > size) {
			throw new IndexOutOfBoundsException("index can't be > size: " + index + " > " + size);
		}
		float[] items = this.items;
		if (size == items.length) {
			items = resize(Math.max(8, (int) (size * 1.75f)));
		}
		items[size] = element;
		++size;
	}

	/**
	 * Removes and returns the item at the specified index.
	 * Note that this is equivalent to {@link List#remove(int)}, but can't have that name because
	 * we also have {@link #remove(float)} that removes a value, rather than an index.
	 *
	 * @param index the index of the item to remove and return
	 * @return the removed item
	 */
	@Override
	public float removeAt(int index) {
		if (index >= size) {
			throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);
		}
		float[] items = this.items;
		float value = items[index];
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
	public void removeRange(int start, int end) {
		int n = size;
		if (end > n) {
			throw new IndexOutOfBoundsException("end can't be > size: " + end + " > " + size);
		}
		if (start > end) {
			throw new IndexOutOfBoundsException("start can't be > end: " + start + " > " + end);
		}
		int count = end - start, lastIndex = n - count;
		int i = Math.max(lastIndex, end);
		System.arraycopy(items, i, items, start, n - i);
		size = n - count;
	}

	@Override
	public int hashCode() {
		float[] items = this.items;
		int h = size;
		for (int i = 0, n = size; i < n; i++) {
			h += BitConversion.floatToRawIntBits(items[i]);
		}
		return h ^ h >>> 16;
	}

	/**
	 * Constructs an empty bag.
	 * This is usually less useful than just using the constructor, but can be handy
	 * in some code-generation scenarios when you don't know how many arguments you will have.
	 *
	 * @return a new bag containing nothing
	 */
	public static FloatBag with() {
		return new FloatBag(0);
	}

	/**
	 * Creates a new FloatBag that holds only the given item, but can be resized.
	 *
	 * @param item a float item
	 * @return a new FloatBag that holds the given item
	 */

	public static FloatBag with(float item) {
		FloatBag bag = new FloatBag(1);
		bag.add(item);
		return bag;
	}

	/**
	 * Creates a new FloatBag that holds only the given items, but can be resized.
	 *
	 * @param item0 a float item
	 * @param item1 a float item
	 * @return a new FloatBag that holds the given items
	 */
	public static FloatBag with(float item0, float item1) {
		FloatBag bag = new FloatBag(2);
		bag.add(item0);
		bag.add(item1);
		return bag;
	}

	/**
	 * Creates a new FloatBag that holds only the given items, but can be resized.
	 *
	 * @param item0 a float item
	 * @param item1 a float item
	 * @param item2 a float item
	 * @return a new FloatBag that holds the given items
	 */
	public static FloatBag with(float item0, float item1, float item2) {
		FloatBag bag = new FloatBag(3);
		bag.add(item0);
		bag.add(item1);
		bag.add(item2);
		return bag;
	}

	/**
	 * Creates a new FloatBag that holds only the given items, but can be resized.
	 *
	 * @param item0 a float item
	 * @param item1 a float item
	 * @param item2 a float item
	 * @param item3 a float item
	 * @return a new FloatBag that holds the given items
	 */
	public static FloatBag with(float item0, float item1, float item2, float item3) {
		FloatBag bag = new FloatBag(4);
		bag.add(item0);
		bag.add(item1);
		bag.add(item2);
		bag.add(item3);
		return bag;
	}

	/**
	 * Creates a new FloatBag that holds only the given items, but can be resized.
	 *
	 * @param item0 a float item
	 * @param item1 a float item
	 * @param item2 a float item
	 * @param item3 a float item
	 * @param item4 a float item
	 * @return a new FloatBag that holds the given items
	 */
	public static FloatBag with(float item0, float item1, float item2, float item3, float item4) {
		FloatBag bag = new FloatBag(5);
		bag.add(item0);
		bag.add(item1);
		bag.add(item2);
		bag.add(item3);
		bag.add(item4);
		return bag;
	}

	/**
	 * Creates a new FloatBag that holds only the given items, but can be resized.
	 *
	 * @param item0 a float item
	 * @param item1 a float item
	 * @param item2 a float item
	 * @param item3 a float item
	 * @param item4 a float item
	 * @param item5 a float item
	 * @return a new FloatBag that holds the given items
	 */
	public static FloatBag with(float item0, float item1, float item2, float item3, float item4, float item5) {
		FloatBag bag = new FloatBag(6);
		bag.add(item0);
		bag.add(item1);
		bag.add(item2);
		bag.add(item3);
		bag.add(item4);
		bag.add(item5);
		return bag;
	}

	/**
	 * Creates a new FloatBag that holds only the given items, but can be resized.
	 *
	 * @param item0 a float item
	 * @param item1 a float item
	 * @param item2 a float item
	 * @param item3 a float item
	 * @param item4 a float item
	 * @param item5 a float item
	 * @param item6 a float item
	 * @return a new FloatBag that holds the given items
	 */
	public static FloatBag with(float item0, float item1, float item2, float item3, float item4, float item5, float item6) {
		FloatBag bag = new FloatBag(7);
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
	 * Creates a new FloatBag that holds only the given items, but can be resized.
	 *
	 * @param item0 a float item
	 * @param item1 a float item
	 * @param item2 a float item
	 * @param item3 a float item
	 * @param item4 a float item
	 * @param item5 a float item
	 * @param item6 a float item
	 * @return a new FloatBag that holds the given items
	 */
	public static FloatBag with(float item0, float item1, float item2, float item3, float item4, float item5, float item6, float item7) {
		FloatBag bag = new FloatBag(8);
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
	 * Creates a new FloatBag that holds only the given items, but can be resized.
	 * This overload will only be used when an array is supplied and the type of the
	 * items requested is the component type of the array, or if varargs are used and
	 * there are 9 or more arguments.
	 *
	 * @param varargs a float varargs or float array; remember that varargs allocate
	 * @return a new FloatBag that holds the given items
	 */
	public static FloatBag with(float... varargs) {
		return new FloatBag(varargs);
	}

	/**
	 * Calls {@link #parse(String, String, boolean)} with brackets set to false.
	 * @param str a String that will be parsed in full
	 * @param delimiter the delimiter between items in str
	 * @return a new collection parsed from str
	 */
	public static FloatBag parse(String str, String delimiter) {
		return parse(str, delimiter, false);
	}

	/**
	 * Creates a new collection and fills it by calling {@link #addLegible(String, String, int, int)} on either all of
	 * {@code str} (if {@code brackets} is false) or {@code str} without its first and last chars (if {@code brackets}
	 * is true). Each item is expected to be separated by {@code delimiter}.
	 * @param str a String that will be parsed in full (depending on brackets)
	 * @param delimiter the delimiter between items in str
	 * @param brackets if true, the first and last chars in str will be ignored
	 * @return a new collection parsed from str
	 */
	public static FloatBag parse(String str, String delimiter, boolean brackets) {
		FloatBag c = new FloatBag();
		if(brackets)
			c.addLegible(str, delimiter, 1, str.length() - 1);
		else
			c.addLegible(str, delimiter);
		return c;
	}

	/**
	 * Creates a new collection and fills it by calling {@link #addLegible(String, String, int, int)} with the given
	 * four parameters as-is.
	 * @param str a String that will have the given section parsed
	 * @param delimiter the delimiter between items in str
	 * @param offset the first position to parse in str, inclusive
	 * @param length how many chars to parse, starting from offset
	 * @return a new collection parsed from str
	 */
	public static FloatBag parse(String str, String delimiter, int offset, int length) {
		FloatBag c = new FloatBag();
		c.addLegible(str, delimiter, offset, length);
		return c;
	}
}

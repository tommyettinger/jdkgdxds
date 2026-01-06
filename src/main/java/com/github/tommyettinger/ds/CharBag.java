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

import com.github.tommyettinger.digital.Base;
import com.github.tommyettinger.ds.support.util.CharIterator;

import java.util.Arrays;
import java.util.List;

/**
 * An unordered List of char items. This allows efficient iteration via an iterator or via index.
 * This class avoids a memory copy when removing elements (the last element is moved to the removed element's position).
 * Items are permitted to change position in the ordering when any item is removed or added.
 * Although this won't keep an order during modifications, you can {@link #sort()} the bag to ensure,
 * if no modifications are made later, that the iteration will happen in sorted order.
 */
public class CharBag extends CharList implements CharSequence, Appendable {
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
	 * Creates an unordered bag with a capacity of 10.
	 */
	public CharBag() {
		super();
	}

	/**
	 * Creates an unordered bag with the specified capacity.
	 *
	 * @param capacity
	 */
	public CharBag(int capacity) {
		super(capacity);
	}

	/**
	 * Creates a new bag containing the elements in the specific list or bag. The capacity is set to the number of elements,
	 * so any subsequent elements added will cause the backing array to be grown.
	 *
	 * @param list another CharList or CharBag
	 */
	public CharBag(CharList list) {
		super(list);
	}

	/**
	 * Creates a new bag containing the elements in the specified array. The capacity is set to the number of elements,
	 * so any subsequent elements added will cause the backing array to be grown.
	 *
	 * @param array a non-null char array to add to this bag
	 */
	public CharBag(char[] array) {
		super(array);
	}

	/**
	 * Creates a new bag containing the elements in the specified array. The capacity is set to the number of elements, so any
	 * subsequent elements added will cause the backing array to be grown.
	 *
	 * @param array      a non-null char array to add to this bag
	 * @param startIndex the first index in {@code array} to use
	 * @param count      how many items to use from {@code array}
	 */
	public CharBag(char[] array, int startIndex, int count) {
		super(array, startIndex, count);
	}

	/**
	 * Creates a new bag containing the items in the specified PrimitiveCollection.OfChar.
	 *
	 * @param coll a primitive collection that will have its contents added to this
	 */
	public CharBag(OfChar coll) {
		super(coll);
	}

	/**
	 * Creates a new instance containing the items in the specified iterator.
	 *
	 * @param coll an iterator that will have its remaining contents added to this
	 */
	public CharBag(CharIterator coll) {
		this();
		addAll(coll);
	}

	/**
	 * Copies the given Ordered.OfChar into a new bag.
	 *
	 * @param other another Ordered.OfChar
	 */
	public CharBag(Ordered.OfChar other) {
		super(other);
	}

	/**
	 * Creates a new bag by copying {@code count} items from the given Ordered, starting at {@code offset} in that Ordered,
	 * into this.
	 *
	 * @param other  another Ordered.OfChar
	 * @param offset the first index in other's ordering to draw an item from
	 * @param count  how many items to copy from other
	 */
	public CharBag(Ordered.OfChar other, int offset, int count) {
		super(other, offset, count);
	}

	public CharBag(CharSequence other) {
		this(other.length());
		append(other);
	}

	/**
	 * Creates a new CharBag from part of another CharSequence; start is inclusive and end is exclusive.
	 *
	 * @param other a CharSequence to use part of, such as a String or a CharList
	 * @param start first index in {@code other} to use, inclusive
	 * @param end   last index in {@code other} to use, exclusive
	 */
	public CharBag(CharSequence other, int start, int end) {
		this(end - start);
		append(other, start, end);
	}

	/**
	 * This always adds {@code element} to the end of this bag's ordering.
	 *
	 * @param index   ignored
	 * @param element element to be inserted
	 */
	@Override
	public void insert(int index, char element) {
		if (index > size) {
			throw new IndexOutOfBoundsException("index can't be > size: " + index + " > " + size);
		}
		char[] items = this.items;
		if (size == items.length) {
			items = resize(Math.max(8, (int) (size * 1.75f)));
		}
		items[size] = element;
		++size;
	}

	/**
	 * Removes and returns the item at the specified index.
	 * Note that this is equivalent to {@link List#remove(int)}, but can't have that name because
	 * we also have {@link #remove(char)} that removes a value, rather than an index.
	 *
	 * @param index the index of the item to remove and return
	 * @return the removed item
	 */
	@Override
	public char removeAt(int index) {
		if (index >= size) {
			throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);
		}
		char[] items = this.items;
		char value = items[index];
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

	/**
	 * Gets a new CharBag from the given subrange, clamping start and end so that this will not throw any Exception.
	 *
	 * @param start the start index, inclusive; clamped
	 * @param end   the end index, exclusive; clamped
	 * @return a new CharBag copied from the given subrange bounds
	 */
	@Override
	public CharBag subSequence(int start, int end) {
		return new CharBag(items, Math.max(Math.min(start, size - 1), 0), Math.min(Math.max(end - start, 0), size));
	}

	@Override
	public CharBag append(CharSequence csq) {
		if (csq == null) {
			add('n', 'u', 'l', 'l');
		} else {
			final int len = csq.length();
			ensureCapacity(len);
			for (int i = 0; i < len; i++) {
				add(csq.charAt(i));
			}
		}
		return this;
	}

	@Override
	public CharBag append(CharSequence csq, int start, int end) {
		if (csq == null) {
			add('n', 'u', 'l', 'l');
		} else {
			ensureCapacity(end - start);
			for (int i = start; i < end; i++) {
				add(csq.charAt(i));
			}
		}
		return this;
	}

	@Override
	public CharBag append(char c) {
		add(c);
		return this;
	}

	/**
	 * Appends a literal newline (Unicode character u000A).
	 *
	 * @return this, for chaining.
	 */
	@Override
	public CharBag appendLine() {
		return append('\n');
	}

	/**
	 * Appends a literal newline (Unicode character u000A).
	 * This is identical to {@link #appendLine()} but is faster to type or recommend.
	 *
	 * @return this, for chaining.
	 */
	@Override
	public CharBag line() {
		return append('\n');
	}

	/**
	 * Appends the base-10 signed textual form of the given number, without allocating.
	 * This uses {@link Base#appendSigned(CharSequence, int)}.
	 *
	 * @param number the int to append
	 * @return this, for chaining
	 */
	@Override
	public CharBag append(int number) {
		return Base.BASE10.appendSigned(this, number);
	}

	/**
	 * Appends the base-10 signed textual form of the given number, without allocating.
	 * This uses {@link Base#appendSigned(CharSequence, long)}. This does not append a trailing {@code 'L'}.
	 *
	 * @param number the long to append
	 * @return this, for chaining
	 */
	@Override
	public CharBag append(long number) {
		return Base.BASE10.appendSigned(this, number);
	}

	/**
	 * Appends the base-10 decimal or engineering textual form of the given number, without allocating.
	 * This uses {@link Base#appendGeneral(CharSequence, float)}. This does not append a trailing {@code 'f'}.
	 *
	 * @param number the float to append
	 * @return this, for chaining
	 */
	@Override
	public CharBag append(float number) {
		return Base.BASE10.appendGeneral(this, number);
	}

	/**
	 * Appends the base-10 decimal or engineering textual form of the given number, without allocating.
	 * This uses {@link Base#appendGeneral(CharSequence, double)}.
	 *
	 * @param number the double to append
	 * @return this, for chaining
	 */
	@Override
	public CharBag append(double number) {
		return Base.BASE10.appendGeneral(this, number);
	}

	/**
	 * Appends either the four chars {@code 't', 'r', 'u', 'e'} if {@code value} is true, or the five chars
	 * {@code 'f', 'a', 'l', 's', 'e'} if it is false.
	 *
	 * @param value either true or false
	 * @return this, for chaining
	 */
	@Override
	public CharBag append(boolean value) {
		super.append(value);
		return this;
	}

	@Override
	public int hashCode() {
		char[] items = this.items;
		int h = size;
		for (int i = 0, n = size; i < n; i++) {
			h += items[i];
		}
		return h ^ h >>> 16;
	}

	/**
	 * Adds {@code count} repetitions of {@code padWith} to the start (left) of this list.
	 * <br>
	 * Note that because the order of a bag is unreliable, this could conceivably pad to any point
	 * in the order, but this does actually move all elements to the right and prepend padWith count times.
	 *
	 * @param count   how many repetitions of {@code padWith} to add
	 * @param padWith the item to pad with
	 * @return this, for chaining
	 */
	public CharBag padLeft(int count, char padWith) {
		if (count > 0) {
			ensureCapacity(count);
			System.arraycopy(items, 0, items, count, size);
			Arrays.fill(items, 0, count, padWith);
			size += count;
		}
		return this;
	}

	/**
	 * Adds {@code count} repetitions of {@code padWith} to the end (right) of this list.
	 *
	 * @param count   how many repetitions of {@code padWith} to add
	 * @param padWith the item to pad with
	 * @return this, for chaining
	 */
	public CharBag padRight(int count, char padWith) {
		if (count > 0) {
			ensureCapacity(count);
			Arrays.fill(items, size, size + count, padWith);
			size += count;
		}
		return this;
	}

	/**
	 * Constructs an empty bag.
	 * This is usually less useful than just using the constructor, but can be handy
	 * in some code-generation scenarios when you don't know how many arguments you will have.
	 *
	 * @return a new bag containing nothing
	 */
	public static CharBag with() {
		return new CharBag(0);
	}

	/**
	 * Creates a new CharBag that holds only the given item, but can be resized.
	 *
	 * @param item a char item
	 * @return a new CharBag that holds the given item
	 */

	public static CharBag with(char item) {
		CharBag bag = new CharBag(1);
		bag.add(item);
		return bag;
	}

	/**
	 * Creates a new CharBag that holds only the given items, but can be resized.
	 *
	 * @param item0 a char item
	 * @param item1 a char item
	 * @return a new CharBag that holds the given items
	 */
	public static CharBag with(char item0, char item1) {
		CharBag bag = new CharBag(2);
		bag.add(item0);
		bag.add(item1);
		return bag;
	}

	/**
	 * Creates a new CharBag that holds only the given items, but can be resized.
	 *
	 * @param item0 a char item
	 * @param item1 a char item
	 * @param item2 a char item
	 * @return a new CharBag that holds the given items
	 */
	public static CharBag with(char item0, char item1, char item2) {
		CharBag bag = new CharBag(3);
		bag.add(item0);
		bag.add(item1);
		bag.add(item2);
		return bag;
	}

	/**
	 * Creates a new CharBag that holds only the given items, but can be resized.
	 *
	 * @param item0 a char item
	 * @param item1 a char item
	 * @param item2 a char item
	 * @param item3 a char item
	 * @return a new CharBag that holds the given items
	 */
	public static CharBag with(char item0, char item1, char item2, char item3) {
		CharBag bag = new CharBag(4);
		bag.add(item0);
		bag.add(item1);
		bag.add(item2);
		bag.add(item3);
		return bag;
	}

	/**
	 * Creates a new CharBag that holds only the given items, but can be resized.
	 *
	 * @param item0 a char item
	 * @param item1 a char item
	 * @param item2 a char item
	 * @param item3 a char item
	 * @param item4 a char item
	 * @return a new CharBag that holds the given items
	 */
	public static CharBag with(char item0, char item1, char item2, char item3, char item4) {
		CharBag bag = new CharBag(5);
		bag.add(item0);
		bag.add(item1);
		bag.add(item2);
		bag.add(item3);
		bag.add(item4);
		return bag;
	}

	/**
	 * Creates a new CharBag that holds only the given items, but can be resized.
	 *
	 * @param item0 a char item
	 * @param item1 a char item
	 * @param item2 a char item
	 * @param item3 a char item
	 * @param item4 a char item
	 * @param item5 a char item
	 * @return a new CharBag that holds the given items
	 */
	public static CharBag with(char item0, char item1, char item2, char item3, char item4, char item5) {
		CharBag bag = new CharBag(6);
		bag.add(item0);
		bag.add(item1);
		bag.add(item2);
		bag.add(item3);
		bag.add(item4);
		bag.add(item5);
		return bag;
	}

	/**
	 * Creates a new CharBag that holds only the given items, but can be resized.
	 *
	 * @param item0 a char item
	 * @param item1 a char item
	 * @param item2 a char item
	 * @param item3 a char item
	 * @param item4 a char item
	 * @param item5 a char item
	 * @param item6 a char item
	 * @return a new CharBag that holds the given items
	 */
	public static CharBag with(char item0, char item1, char item2, char item3, char item4, char item5, char item6) {
		CharBag bag = new CharBag(7);
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
	 * Creates a new CharBag that holds only the given items, but can be resized.
	 *
	 * @param item0 a char item
	 * @param item1 a char item
	 * @param item2 a char item
	 * @param item3 a char item
	 * @param item4 a char item
	 * @param item5 a char item
	 * @param item6 a char item
	 * @return a new CharBag that holds the given items
	 */
	public static CharBag with(char item0, char item1, char item2, char item3, char item4, char item5, char item6, char item7) {
		CharBag bag = new CharBag(8);
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
	 * Creates a new CharBag that holds only the given items, but can be resized.
	 * This overload will only be used when an array is supplied and the type of the
	 * items requested is the component type of the array, or if varargs are used and
	 * there are 9 or more arguments.
	 *
	 * @param varargs a char varargs or char array; remember that varargs allocate
	 * @return a new CharBag that holds the given items
	 */
	public static CharBag with(char... varargs) {
		return new CharBag(varargs);
	}

	/**
	 * Calls {@link #parse(String, String, boolean)} with brackets set to false.
	 *
	 * @param str       a String that will be parsed in full
	 * @param delimiter the delimiter between items in str
	 * @return a new collection parsed from str
	 */
	public static CharBag parse(String str, String delimiter) {
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
	public static CharBag parse(String str, String delimiter, boolean brackets) {
		CharBag c = new CharBag();
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
	public static CharBag parse(String str, String delimiter, int offset, int length) {
		CharBag c = new CharBag();
		c.addLegible(str, delimiter, offset, length);
		return c;
	}
}

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

import com.github.tommyettinger.ds.support.sort.LongComparator;
import com.github.tommyettinger.ds.support.sort.LongComparators;
import com.github.tommyettinger.ds.support.util.LongIterator;
import com.github.tommyettinger.function.LongToLongFunction;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Arrays;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Random;

/**
 * A resizable, insertion-ordered long list. Primitive-backed, so it avoids the boxing that occurs with an ArrayList of Long.
 * This tries to imitate most of the {@link java.util.List} interface, though it can't implement it without boxing its items.
 * Has a primitive iterator accessible via {@link #iterator()}.
 *
 * @author Nathan Sweet
 * @author Tommy Ettinger
 * @see LongBag LongBag is an unordered variant on LongList.
 */
public class LongList implements PrimitiveCollection.OfLong, Ordered.OfLong, Arrangeable {
	/**
	 * Returns true if this implementation retains order, which it does.
	 * @return true
	 */
	public boolean keepsOrder () {
		return true;
	}

	public long[] items;
	protected int size;
	@Nullable protected transient LongListIterator iterator1;
	@Nullable protected transient LongListIterator iterator2;

	/**
	 * Creates an ordered list with a capacity of 10.
	 */
	public LongList () {
		this(10);
	}

	/**
	 * Creates an ordered list with the specified capacity.
	 * @param capacity Any elements added beyond this will cause the backing array to be grown.
	 */
	public LongList (int capacity) {
		items = new long[capacity];
	}

	/**
	 * Creates an ordered list with the specified capacity.
	 *
	 * @param ordered ignored; for an unordered list use {@link LongBag}
	 * @param capacity Any elements added beyond this will cause the backing array to be grown.
	 * @deprecated LongList is always ordered; for an unordered list use {@link LongBag}
	 */
	@Deprecated
	public LongList (boolean ordered, int capacity) {
		this(capacity);
	}

	/**
	 * Creates a new list containing the elements in the given list. The new list will be ordered. The capacity is set
	 * to the number of elements, so any subsequent elements added will cause the backing array to be grown.
	 * @param list another LongList (or LongBag) to copy from
	 */
	public LongList (LongList list) {
		size = list.size;
		items = new long[size];
		System.arraycopy(list.items, 0, items, 0, size);
	}

	/**
	 * Creates a new list containing the elements in the specified array. The capacity is set to the number of elements,
	 * so any subsequent elements added will cause the backing array to be grown.
	 * @param array a long array to copy from
	 */
	public LongList (long[] array) {
		this(array, 0, array.length);
	}

	/**
	 * Creates a new list containing the elements in the specified array. The capacity is set to the number of elements, so any
	 * subsequent elements added will cause the backing array to be grown.
	 *
	 * @param array a non-null long array to add to this list
	 * @param startIndex the first index in {@code array} to use
	 * @param count how many items to use from {@code array}
	 */
	public LongList (long[] array, int startIndex, int count) {
		this(count);
		size = count;
		System.arraycopy(array, startIndex, items, 0, count);
	}

	/**
	 * Creates a new list containing the elements in the specified array. The capacity is set to the number of elements, so any
	 * subsequent elements added will cause the backing array to be grown.
	 *
	 * @param ordered ignored; for an unordered list use {@link LongBag}
	 * @param array a non-null long array to add to this list
	 * @param startIndex the first index in {@code array} to use
	 * @param count how many items to use from {@code array}
	 * @deprecated LongList is always ordered; for an unordered list use {@link LongBag}
	 */
	@Deprecated
	public LongList (boolean ordered, long[] array, int startIndex, int count) {
		this(array, startIndex, count);
	}

	/**
	 * Creates a new list containing the items in the specified PrimitiveCollection.OfLong.
	 *
	 * @param coll a primitive collection that will have its contents added to this
	 */
	public LongList (OfLong coll) {
		this(coll.size());
		addAll(coll);
	}

	/**
	 * Creates a new instance containing the items in the specified iterator.
	 *
	 * @param coll an iterator that will have its remaining contents added to this
	 */
	public LongList (LongIterator coll) {
		this();
		addAll(coll);
	}

	/**
	 * Copies the given Ordered.OfLong into a new LongList.
	 *
	 * @param other another Ordered.OfLong that will have its contents copied into this
	 */
	public LongList (Ordered.OfLong other) {
		this(other.order());
	}

	/**
	 * Creates a new list by copying {@code count} items from the given Ordered, starting at {@code offset} in that Ordered,
	 * into this.
	 *
	 * @param other  another Ordered.OfLong
	 * @param offset the first index in other's ordering to draw an item from
	 * @param count  how many items to copy from other
	 */
	public LongList (Ordered.OfLong other, int offset, int count) {
		this(count);
		addAll(0, other, offset, count);
	}

	// Newly-added
	@Override
	public int size () {
		return size;
	}

	// Modified from libGDX
	@Override
	public boolean add (long value) {
		long[] items = this.items;
		if (size == items.length) {items = resize(Math.max(8, (int)(size * 1.75f)));}
		items[size++] = value;
		return true;
	}

	public void add (long value1, long value2) {
		long[] items = this.items;
		if (size + 1 >= items.length) {items = resize(Math.max(8, (int)(size * 1.75f)));}
		items[size] = value1;
		items[size + 1] = value2;
		size += 2;
	}

	public void add (long value1, long value2, long value3) {
		long[] items = this.items;
		if (size + 2 >= items.length) {items = resize(Math.max(8, (int)(size * 1.75f)));}
		items[size] = value1;
		items[size + 1] = value2;
		items[size + 2] = value3;
		size += 3;
	}

	public void add (long value1, long value2, long value3, long value4) {
		long[] items = this.items;
		if (size + 3 >= items.length) {
			items = resize(Math.max(9, (int)(size * 1.75f)));
		}
		items[size] = value1;
		items[size + 1] = value2;
		items[size + 2] = value3;
		items[size + 3] = value4;
		size += 4;
	}

	// Modified from libGDX
	public boolean addAll (LongList list) {
		return addAll(list.items, 0, list.size);
	}

	// Modified from libGDX
	public boolean addAll (LongList list, int offset, int count) {
		if (offset + count > list.size) {throw new IllegalArgumentException("offset + count must be <= list.size: " + offset + " + " + count + " <= " + list.size);}
		return addAll(list.items, offset, count);
	}

	/**
	 * Adds up to {@code count} items, starting from {@code offset}, in the Ordered.OfLong {@code other} to this list,
	 * inserting at the end of the iteration order.
	 *
	 * @param other  a non-null {@link Ordered.OfLong}
	 * @param offset the first index in {@code other} to use
	 * @param count  how many indices in {@code other} to use
	 * @return true if this is modified by this call, as {@link #addAll(LongList)} does
	 */
	public boolean addAll (Ordered.OfLong other, int offset, int count) {
		return addAll(size(), other, offset, count);
	}

	/**
	 * Adds up to {@code count} items, starting from {@code offset}, in the Ordered.OfLong {@code other} to this list,
	 * inserting starting at {@code insertionIndex} in the iteration order.
	 *
	 * @param insertionIndex where to insert into the iteration order
	 * @param other          a non-null {@link Ordered.OfLong}
	 * @param offset         the first index in {@code other} to use
	 * @param count          how many indices in {@code other} to use
	 * @return true if this is modified by this call, as {@link #addAll(LongList)} does
	 */
	public boolean addAll (int insertionIndex, Ordered.OfLong other, int offset, int count) {
		boolean changed = false;
		int end = Math.min(offset + count, other.size());
		ensureCapacity(end - offset);
		for (int i = offset; i < end; i++) {
			insert(insertionIndex++, other.order().get(i));
			changed = true;
		}
		return changed;
	}

	// Modified from libGDX
	public boolean addAll (long... array) {
		return addAll(array, 0, array.length);
	}

	// Modified from libGDX
	public boolean addAll (long[] array, int offset, int length) {
		long[] items = this.items;
		int sizeNeeded = size + length;
		if (sizeNeeded > items.length) {items = resize(Math.max(Math.max(8, sizeNeeded), (int)(size * 1.75f)));}
		System.arraycopy(array, offset, items, size, length);
		size += length;
		return true;
	}

	//Kotlin-friendly operator
	public long get (int index) {
		if (index >= size) {throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);}
		return items[index];
	}

	//Kotlin-friendly operator
	public void set (int index, long value) {
		if (index >= size) {throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);}
		items[index] = value;
	}

	// Modified from libGDX
	public void plus (int index, long value) {
		if (index >= size) {throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);}
		items[index] += value;
	}

	/**
	 * Adds {@code value} to each item in this LongList, stores it in this and returns it.
	 * The presence of this method allows Kotlin code to use the {@code +} operator (though it
	 * shouldn't be used more than once in an expression, because this method modifies this LongList).
	 *
	 * @param value each item in this will be assigned {@code item + value}
	 * @return this for chaining and Kotlin compatibility
	 */
	// Modified from libGDX
	// Kotlin-friendly operator
	public LongList plus (long value) {
		long[] items = this.items;
		for (int i = 0, n = size; i < n; i++) {items[i] += value;}
		return this;
	}

	// Modified from libGDX
	public void times (int index, long value) {
		if (index >= size) {throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);}
		items[index] *= value;
	}

	/**
	 * Multiplies each item in this LongList by {@code value}, stores it in this and returns it.
	 * The presence of this method allows Kotlin code to use the {@code *} operator (though it
	 * shouldn't be used more than once in an expression, because this method modifies this LongList).
	 *
	 * @param value each item in this will be assigned {@code item * value}
	 * @return this for chaining and Kotlin compatibility
	 */
	// Modified from libGDX
	// Kotlin-friendly operator
	public LongList times (long value) {
		long[] items = this.items;
		for (int i = 0, n = size; i < n; i++) {items[i] *= value;}
		return this;
	}

	// Newly-added
	public void minus (int index, long value) {
		if (index >= size) {throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);}
		items[index] -= value;
	}

	/**
	 * Takes each item in this LongList and subtracts {@code value}, stores it in this and returns it.
	 * This is just a minor convenience in Java, but the presence of this method allows Kotlin code to use
	 * the {@code -} operator (though it shouldn't be used more than once in an expression, because
	 * this method modifies this LongList).
	 *
	 * @param value each item in this will be assigned {@code item - value}
	 * @return this for chaining and Kotlin compatibility
	 */
	// Newly-added
	// Kotlin-friendly operator
	public LongList minus (long value) {
		long[] items = this.items;
		for (int i = 0, n = size; i < n; i++) {items[i] -= value;}
		return this;
	}

	// Newly-added
	public void div (int index, long value) {
		if (index >= size) {throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);}
		items[index] /= value;
	}

	/**
	 * Divides each item in this LongList by {@code value}, stores it in this and returns it.
	 * The presence of this method allows Kotlin code to use the {@code /} operator (though it
	 * shouldn't be used more than once in an expression, because this method modifies this LongList).
	 *
	 * @param value each item in this will be assigned {@code item / value}
	 * @return this for chaining and Kotlin compatibility
	 */
	// Newly-added
	// Kotlin-friendly operator
	public LongList div (long value) {
		long[] items = this.items;
		for (int i = 0, n = size; i < n; i++) {items[i] /= value;}
		return this;
	}

	// Newly-added
	public void rem (int index, long value) {
		if (index >= size) {throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);}
		items[index] %= value;
	}

	/**
	 * Gets the remainder of each item in this LongList with {@code value}, stores it in this and returns it.
	 * The presence of this method allows Kotlin code to use the {@code %} operator (though it
	 * shouldn't be used more than once in an expression, because this method modifies this LongList).
	 *
	 * @param value each item in this will be assigned {@code item % value}
	 * @return this for chaining and Kotlin compatibility
	 */
	// Newly-added
	// Kotlin-friendly operator
	public LongList rem (long value) {
		long[] items = this.items;
		for (int i = 0, n = size; i < n; i++) {items[i] %= value;}
		return this;
	}

	public void insert (int index, long value) {
		if (index > size) {throw new IndexOutOfBoundsException("index can't be > size: " + index + " > " + size);}
		long[] items = this.items;
		if (size == items.length) {items = resize(Math.max(8, (int)(size * 1.75f)));}
		System.arraycopy(items, index, items, index + 1, size - index);
		size++;
		items[index] = value;
	}

	/**
	 * Inserts the specified number of items at the specified index. The new items will have values equal to the values at those
	 * indices before the insertion, and the previous values will be pushed to after the duplicated range.
	 * @param index the first index to duplicate
	 * @param count how many items to duplicate
	 */
	public boolean duplicateRange (int index, int count) {
		if (index > size) {throw new IndexOutOfBoundsException("index can't be > size: " + index + " > " + size);}
		int sizeNeeded = size + count;
		if (sizeNeeded > items.length) {items = resize(Math.max(Math.max(8, sizeNeeded), (int)(size * 1.75f)));}
		System.arraycopy(items, index, items, index + count, size - index);
		size = sizeNeeded;
		return count > 0;
	}

	/**
	 * Returns this LongList, since it is its own order. This is only here to satisfy
	 * the {@link Ordered.OfLong} interface.
	 *
	 * @return this LongList
	 */
	@Override
	public LongList order () {
		return this;
	}

	@Override
	public void swap (int first, int second) {
		if (first >= size) {throw new IndexOutOfBoundsException("first can't be >= size: " + first + " >= " + size);}
		if (second >= size) {throw new IndexOutOfBoundsException("second can't be >= size: " + second + " >= " + size);}
		long[] items = this.items;
		long firstValue = items[first];
		items[first] = items[second];
		items[second] = firstValue;
	}

	@Override
	public boolean contains (long value) {
		int i = size - 1;
		long[] items = this.items;
		while (i >= 0) {if (items[i--] == value) {return true;}}
		return false;
	}

	/**
	 * Returns true if this LongList contains, at least once, every item in {@code other}; otherwise returns false.
	 *
	 * @param other an LongList
	 * @return true if this contains every item in {@code other}, otherwise false
	 */
	// Newly-added
	public boolean containsAll (LongList other) {
		long[] others = other.items;
		int otherSize = other.size;
		for (int i = 0; i < otherSize; i++) {
			if (!contains(others[i])) {return false;}
		}
		return true;
	}

	/**
	 * Returns the first index in this list that contains the specified value, or -1 if it is not present.
	 *
	 * @param value a long value to search for
	 * @return the first index of the given value, or -1 if it is not present
	 */
	public int indexOf (long value) {
		long[] items = this.items;
		for (int i = 0, n = size; i < n; i++) {if (items[i] == value) {return i;}}
		return -1;
	}

	/**
	 * Returns the last index in this list that contains the specified value, or -1 if it is not present.
	 *
	 * @param value a long value to search for
	 * @return the last index of the given value, or -1 if it is not present
	 */
	public int lastIndexOf (long value) {
		long[] items = this.items;
		for (int i = size - 1; i >= 0; i--) {if (items[i] == value) {return i;}}
		return -1;
	}

	/**
	 * Removes the first occurrence of {@code value} from this LongList, returning true if anything was removed.
	 * Otherwise, this returns false.
	 *
	 * @param value the value to (attempt to) remove
	 * @return true if a value was removed, false if the LongList is unchanged
	 */
	// Modified from libGDX
	@Override
	public boolean remove (long value) {
		long[] items = this.items;
		for (int i = 0, n = size; i < n; i++) {
			if (items[i] == value) {
				removeAt(i);
				return true;
			}
		}
		return false;
	}

	/**
	 * Removes and returns the item at the specified index.
	 * Note that this is equivalent to {@link java.util.List#remove(int)}, but can't have that name because
	 * we also have {@link #remove(long)} that removes a value, rather than an index.
	 *
	 * @param index the index of the item to remove and return
	 * @return the removed item
	 */
	public long removeAt (int index) {
		if (index >= size) {throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);}
		long[] items = this.items;
		long value = items[index];
		size--;
		System.arraycopy(items, index + 1, items, index, size - index);
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
	public void removeRange (int start, int end) {
		int n = size;
		if (end > n) {throw new IndexOutOfBoundsException("end can't be > size: " + end + " > " + size);}
		if (start > end) {throw new IndexOutOfBoundsException("start can't be > end: " + start + " > " + end);}
		int count = end - start;
		System.arraycopy(items, start + count, items, start, n - (start + count));
		size = n - count;
	}

	/**
	 * Removes from this LongList all occurrences of any elements contained in the specified collection.
	 *
	 * @param c a primitive collection of int items to remove fully, such as another LongList or a LongDeque
	 * @return true if this list was modified.
	 */
	public boolean removeAll (OfLong c) {
		int size = this.size;
		int startSize = size;
		long[] items = this.items;
		LongIterator it = c.iterator();
		for (int i = 0, n = c.size(); i < n; i++) {
			long item = it.nextLong();
			for (int ii = 0; ii < size; ii++) {
				if (item == items[ii]) {
					removeAt(ii--);
					size--;
				}
			}
		}
		return size != startSize;
	}

	/**
	 * Removes from this LongList element-wise occurrences of elements contained in the specified collection.
	 * Note that if a value is present more than once in this LongList, only one of those occurrences
	 * will be removed for each occurrence of that value in {@code c}. If {@code c} has the same
	 * contents as this LongList or has additional items, then removing each of {@code c} will clear this.
	 *
	 * @param c a primitive collection of int items to remove one-by-one, such as another LongList or a LongDeque
	 * @return true if this list was modified.
	 */
	public boolean removeEach (OfLong c) {
		int size = this.size;
		int startSize = size;
		long[] items = this.items;
		LongIterator it = c.iterator();
		for (int i = 0, n = c.size(); i < n; i++) {
			long item = it.nextLong();
			for (int ii = 0; ii < size; ii++) {
				if (item == items[ii]) {
					removeAt(ii);
					size--;
					break;
				}
			}
		}
		return size != startSize;
	}

	/**
	 * Removes all items from this LongList that are not present somewhere in {@code other}, any number of times.
	 *
	 * @param other a PrimitiveCollection.OfLong that contains the items that this should keep, whenever present
	 * @return true if this LongList changed as a result of this call, otherwise false
	 */
	// Newly-added
	public boolean retainAll (OfLong other) {
		final int size = this.size;
		final long[] items = this.items;
		int r = 0, w = 0;
		for (; r < size; r++) {
			if (other.contains(items[r])) {
				items[w++] = items[r];
			}
		}

		return size != (this.size = w);
	}

	/**
	 * Replaces each element of this list with the result of applying the
	 * given operator to that element.
	 *
	 * @param operator a LongToLongFunction (a functional interface defined in funderby)
	 */
	public void replaceAll (LongToLongFunction operator) {
		for (int i = 0, n = size; i < n; i++) {
			items[i] = operator.applyAsLong(items[i]);
		}
	}

	/**
	 * Removes and returns the last item.
	 *
	 * @return the last item, removed from this
	 */
	public long pop () {
		if (size == 0) {throw new IndexOutOfBoundsException("LongList is empty.");}
		return items[--size];
	}

	/**
	 * Returns the last item.
	 *
	 * @return the last item, without modifying this
	 */
	public long peek () {
		if (size == 0) {throw new IndexOutOfBoundsException("LongList is empty.");}
		return items[size - 1];
	}

	/**
	 * Returns the first item.
	 *
	 * @return the first item, without modifying this
	 */
	// Modified from libGDX
	public long first () {
		if (size == 0) {throw new IndexOutOfBoundsException("LongList is empty.");}
		return items[0];
	}

	/**
	 * Returns true if the list has one or more items, or false otherwise.
	 *
	 * @return true if the list has one or more items, or false otherwise
	 */
	public boolean notEmpty () {
		return size != 0;
	}

	/**
	 * Returns true if the list is empty.
	 *
	 * @return true if the list is empty, or false if it has any items
	 */
	@Override
	public boolean isEmpty () {
		return size == 0;
	}

	/**
	 * Effectively removes all items from this LongList.
	 * This is done simply by setting size to 0; because a {@code long} item isn't a reference, it doesn't need to be set to null.
	 */
	@Override
	public void clear () {
		size = 0;
	}

	/**
	 * Reduces the size of the backing array to the size of the actual items. This is useful to release memory when many items
	 * have been removed, or if it is known that more items will not be added.
	 *
	 * @return {@link #items}; this will be a different reference if this resized
	 */
	public long[] shrink () {
		if (items.length != size) {resize(size);}
		return items;
	}

	public void trimToSize() {
		shrink();
	}

	/**
	 * Increases the size of the backing array to accommodate the specified number of additional items. Useful before adding many
	 * items to avoid multiple backing array resizes.
	 *
	 * @return {@link #items}; this will be a different reference if this resized
	 */
	public long[] ensureCapacity (int additionalCapacity) {
		if (additionalCapacity < 0) {throw new IllegalArgumentException("additionalCapacity must be >= 0: " + additionalCapacity);}
		int sizeNeeded = size + additionalCapacity;
		if (sizeNeeded > items.length) {resize(Math.max(Math.max(8, sizeNeeded), (int)(size * 1.75f)));}
		return items;
	}

	/**
	 * Sets the list size, leaving any values beyond the current size undefined.
	 *
	 * @return {@link #items}; this will be a different reference if this resized to a larger capacity
	 */
	public long[] setSize (int newSize) {
		if (newSize < 0) {throw new IllegalArgumentException("newSize must be >= 0: " + newSize);}
		if (newSize > items.length) {resize(Math.max(8, newSize));}
		size = newSize;
		return items;
	}

	protected long[] resize (int newSize) {
		long[] newItems = new long[newSize];
		long[] items = this.items;
		System.arraycopy(items, 0, newItems, 0, Math.min(size, newItems.length));
		this.items = newItems;
		return newItems;
	}

	public void sort () {
		Arrays.sort(items, 0, size);
	}

	/**
	 * Sorts all elements according to the order induced by the specified
	 * comparator using {@link LongComparators#sort(long[], int, int, LongComparator)}.
	 * If {@code c} is null, this instead delegates to {@link #sort()},
	 * which uses {@link Arrays#sort(long[])}, and does not always run in-place.
	 *
	 * <p>This sort is guaranteed to be <i>stable</i>: equal elements will not be reordered as a result
	 * of the sort. The sorting algorithm is an in-place mergesort that is significantly slower than a
	 * standard mergesort, as its running time is <i>O</i>(<var>n</var>&nbsp;(log&nbsp;<var>n</var>)<sup>2</sup>), but it does not allocate additional memory; as a result, it can be
	 * used as a generic sorting algorithm.
	 *
	 * @param c the comparator to determine the order of the LongList
	 */
	public void sort (@Nullable final LongComparator c) {
		if (c == null) {
			sort();
		} else {
			sort(0, size, c);
		}
	}

	/**
	 * Sorts the specified range of elements according to the order induced by the specified
	 * comparator using mergesort, or {@link Arrays#sort(long[], int, int)} if {@code c} is null.
	 * This purely uses {@link LongComparators#sort(long[], int, int, LongComparator)}, and you
	 * can see its docs for more information.
	 *
	 * @param from the index of the first element (inclusive) to be sorted.
	 * @param to   the index of the last element (exclusive) to be sorted.
	 * @param c    the comparator to determine the order of the LongList
	 */
	public void sort (final int from, final int to, final LongComparator c) {
		LongComparators.sort(items, from, to, c);
	}

	@Override
	public void reverse () {
		long[] items = this.items;
		for (int i = 0, lastIndex = size - 1, n = size / 2; i < n; i++) {
			int ii = lastIndex - i;
			long temp = items[i];
			items[i] = items[ii];
			items[ii] = temp;
		}
	}

	// Modified from libGDX
	@Override
	public void shuffle (Random random) {
		long[] items = this.items;
		for (int i = size - 1; i > 0; i--) {
			int ii = random.nextInt(i + 1);
			long temp = items[i];
			items[i] = items[ii];
			items[ii] = temp;
		}
	}

	/**
	 * Reduces the size of the list to the specified size. If the list is already smaller than the specified size, no action is
	 * taken.
	 */
	public void truncate (int newSize) {
		newSize = Math.max(0, newSize);
		if (size > newSize) {size = newSize;}
	}

	/**
	 * Returns a random item from the list, or zero if the list is empty.
	 *
	 * @param random a {@link Random} or a subclass, such as any from juniper
	 * @return a randomly selected item from this, or {@code 0} if this is empty
	 */
	public long random (Random random) {
		if (size == 0) {return 0;}
		return items[random.nextInt(size)];
	}

	/**
	 * Allocates a new long array with {@code size} elements and fills it with the items in this.
	 *
	 * @return a new long array with the same contents as this
	 */
	public long[] toArray () {
		long[] array = new long[size];
		System.arraycopy(items, 0, array, 0, size);
		return array;
	}

	/**
	 * If {@code array.length} at least equal to {@link #size()}, this copies the contents of this
	 * into {@code array} and returns it; otherwise, it allocates a new long array that can fit all
	 * the items in this, and proceeds to copy into that and return that.
	 *
	 * @param array a long array that will be modified if it can fit {@link #size()} items
	 * @return {@code array}, if it had sufficient size, or a new array otherwise, either with a copy of this
	 */
	public long[] toArray (long[] array) {
		if (array.length < size)
			array = new long[size];
		System.arraycopy(items, 0, array, 0, size);
		return array;
	}

	@Override
	public int hashCode () {
		long[] items = this.items;
		long h = size;
		for (int i = 0, n = size; i < n; i++) {
			h = h * 0x9E3779B97F4A7C15L + items[i];
		}
		return (int)(h ^ h >>> 32);
	}

	@Override
	public boolean equals (Object object) {
		if (object == this) {return true;}
		if (!(object instanceof LongList)) {return false;}
		LongList list = (LongList)object;
		int n = size;
		if (n != list.size()) {return false;}
		long[] items1 = this.items, items2 = list.items;
		for (int i = 0; i < n; i++) {if (items1[i] != items2[i]) {return false;}}
		return true;
	}

	@Override
	public String toString () {
		return toString(", ", true);
	}

	/**
	 * Returns a Java 8 primitive iterator over the int items in this LongList. Iterates in order if
	 * {@link #keepsOrder()} returns true, which it does for a LongList but not a LongBag.
	 * <br>
	 * This will reuse one of two iterators in this LongList; this does not allow nested iteration.
	 * Use {@link LongListIterator#LongListIterator(LongList)} to nest iterators.
	 *
	 * @return a {@link LongIterator}; use its nextLong() method instead of next()
	 */
	@Override
	public LongListIterator iterator () {
		if (iterator1 == null || iterator2 == null) {
			iterator1 = new LongListIterator(this);
			iterator2 = new LongListIterator(this);
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

	/**
	 * A {@link LongIterator}, plus {@link ListIterator} methods, over the elements of a LongList.
	 * Use {@link #nextLong()} in preference to {@link #next()} to avoid allocating Long objects.
	 */
	public static class LongListIterator implements LongIterator {
		protected int index, latest = -1;
		protected LongList list;
		protected boolean valid = true;

		public LongListIterator (LongList list) {
			this.list = list;
		}

		public LongListIterator (LongList list, int index) {
			if (index < 0 || index >= list.size())
				throw new IndexOutOfBoundsException("LongListIterator does not satisfy index >= 0 && index < list.size()");
			this.list = list;
			this.index = index;
		}

		/**
		 * Returns the next {@code int} element in the iteration.
		 *
		 * @return the next {@code int} element in the iteration
		 * @throws NoSuchElementException if the iteration has no more elements
		 */
		@Override
		public long nextLong () {
			if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
			if (index >= list.size()) {throw new NoSuchElementException();}
			return list.get(latest = index++);
		}

		/**
		 * Returns {@code true} if the iteration has more elements.
		 * (In other words, returns {@code true} if {@link #nextLong} would
		 * return an element rather than throwing an exception.)
		 *
		 * @return {@code true} if the iteration has more elements
		 */
		@Override
		public boolean hasNext () {
			if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
			return index < list.size();
		}

		/**
		 * Returns {@code true} if this list iterator has more elements when
		 * traversing the list in the reverse direction.  (In other words,
		 * returns {@code true} if {@link #previousLong} would return an element
		 * rather than throwing an exception.)
		 *
		 * @return {@code true} if the list iterator has more elements when
		 * traversing the list in the reverse direction
		 */
		public boolean hasPrevious () {
			if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
			return index > 0 && list.notEmpty();
		}

		/**
		 * Returns the previous element in the list and moves the cursor
		 * position backwards.  This method may be called repeatedly to
		 * iterate through the list backwards, or intermixed with calls to
		 * {@link #nextLong} to go back and forth.  (Note that alternating calls
		 * to {@code next} and {@code previous} will return the same
		 * element repeatedly.)
		 *
		 * @return the previous element in the list
		 * @throws NoSuchElementException if the iteration has no previous
		 *                                element
		 */
		public long previousLong () {
			if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
			if (index <= 0 || list.isEmpty()) {throw new NoSuchElementException();}
			return list.get(latest = --index);
		}

		/**
		 * Returns the index of the element that would be returned by a
		 * subsequent call to {@link #nextLong}. (Returns list size if the list
		 * iterator is at the end of the list.)
		 *
		 * @return the index of the element that would be returned by a
		 * subsequent call to {@code next}, or list size if the list
		 * iterator is at the end of the list
		 */
		public int nextIndex () {
			return index;
		}

		/**
		 * Returns the index of the element that would be returned by a
		 * subsequent call to {@link #previousLong}. (Returns -1 if the list
		 * iterator is at the beginning of the list.)
		 *
		 * @return the index of the element that would be returned by a
		 * subsequent call to {@code previous}, or -1 if the list
		 * iterator is at the beginning of the list
		 */
		public int previousIndex () {
			return index - 1;
		}

		/**
		 * Removes from the list the last element that was returned by {@link
		 * #nextLong} or {@link #previousLong} (optional operation).  This call can
		 * only be made once per call to {@code next} or {@code previous}.
		 * It can be made only if {@link #add} has not been
		 * called after the last call to {@code next} or {@code previous}.
		 *
		 * @throws UnsupportedOperationException if the {@code remove}
		 *                                       operation is not supported by this list iterator
		 * @throws IllegalStateException         if neither {@code next} nor
		 *                                       {@code previous} have been called, or {@code remove} or
		 *                                       {@code add} have been called after the last call to
		 *                                       {@code next} or {@code previous}
		 */
		@Override
		public void remove () {
			if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
			if (latest == -1 || latest >= list.size()) {throw new NoSuchElementException();}
			list.removeAt(latest);
			index = latest;
			latest = -1;
		}

		/**
		 * Replaces the last element returned by {@link #nextLong} or
		 * {@link #previousLong} with the specified element (optional operation).
		 * This call can be made only if neither {@link #remove} nor {@link
		 * #add} have been called after the last call to {@code next} or
		 * {@code previous}.
		 *
		 * @param t the element with which to replace the last element returned by
		 *          {@code next} or {@code previous}
		 * @throws UnsupportedOperationException if the {@code set} operation
		 *                                       is not supported by this list iterator
		 * @throws ClassCastException            if the class of the specified element
		 *                                       prevents it from being added to this list
		 * @throws IllegalArgumentException      if some aspect of the specified
		 *                                       element prevents it from being added to this list
		 * @throws IllegalStateException         if neither {@code next} nor
		 *                                       {@code previous} have been called, or {@code remove} or
		 *                                       {@code add} have been called after the last call to
		 *                                       {@code next} or {@code previous}
		 */
		public void set (long t) {
			if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
			if (latest == -1 || latest >= list.size()) {throw new NoSuchElementException();}
			list.set(latest, t);
		}

		/**
		 * Inserts the specified element into the list (optional operation).
		 * The element is inserted immediately before the element that
		 * would be returned by {@link #nextLong}, if any, and after the element
		 * that would be returned by {@link #previousLong}, if any.  (If the
		 * list contains no elements, the new element becomes the sole element
		 * on the list.)  The new element is inserted before the implicit
		 * cursor: a subsequent call to {@code next} would be unaffected, and a
		 * subsequent call to {@code previous} would return the new element.
		 * (This call increases by one the value that would be returned by a
		 * call to {@code nextIndex} or {@code previousIndex}.)
		 *
		 * @param t the element to insert
		 * @throws UnsupportedOperationException if the {@code add} method is
		 *                                       not supported by this list iterator
		 * @throws ClassCastException            if the class of the specified element
		 *                                       prevents it from being added to this list
		 * @throws IllegalArgumentException      if some aspect of this element
		 *                                       prevents it from being added to this list
		 */
		public void add (long t) {
			if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
			if (index > list.size()) {throw new NoSuchElementException();}
			list.insert(index, t);
			if(list.keepsOrder()) ++index;
			latest = -1;
		}

		public void reset () {
			index = 0;
			latest = -1;
		}

		public void reset (int index) {
			if (index < 0 || index >= list.size())
				throw new IndexOutOfBoundsException("LongListIterator does not satisfy index >= 0 && index < list.size()");
			this.index = index;
			latest = -1;
		}

		/**
		 * Returns an iterator over elements of type {@code long}.
		 *
		 * @return this same LongListIterator.
		 */
		public LongList.LongListIterator iterator () {
			return this;
		}
	}

	/**
	 * Constructs an empty list.
	 * This is usually less useful than just using the constructor, but can be handy
	 * in some code-generation scenarios when you don't know how many arguments you will have.
	 *
	 * @return a new list containing nothing
	 */
	public static LongList with () {
		return new LongList(0);
	}

	/**
	 * Creates a new LongList that holds only the given item, but can be resized.
	 * @param item a long item
	 * @return a new LongList that holds the given item
	 */
	public static LongList with (long item) {
		LongList list = new LongList(1);
		list.add(item);
		return list;
	}

	/**
	 * Creates a new LongList that holds only the given items, but can be resized.
	 * @param item0 a long item
	 * @param item1 a long item
	 * @return a new LongList that holds the given items
	 */
	public static LongList with (long item0, long item1) {
		LongList list = new LongList(2);
		list.add(item0);
		list.add(item1);
		return list;
	}

	/**
	 * Creates a new LongList that holds only the given items, but can be resized.
	 * @param item0 a long item
	 * @param item1 a long item
	 * @param item2 a long item
	 * @return a new LongList that holds the given items
	 */
	public static LongList with (long item0, long item1, long item2) {
		LongList list = new LongList(3);
		list.add(item0);
		list.add(item1);
		list.add(item2);
		return list;
	}

	/**
	 * Creates a new LongList that holds only the given items, but can be resized.
	 * @param item0 a long item
	 * @param item1 a long item
	 * @param item2 a long item
	 * @param item3 a long item
	 * @return a new LongList that holds the given items
	 */
	public static LongList with (long item0, long item1, long item2, long item3) {
		LongList list = new LongList(4);
		list.add(item0);
		list.add(item1);
		list.add(item2);
		list.add(item3);
		return list;
	}

	/**
	 * Creates a new LongList that holds only the given items, but can be resized.
	 * @param item0 a long item
	 * @param item1 a long item
	 * @param item2 a long item
	 * @param item3 a long item
	 * @param item4 a long item
	 * @return a new LongList that holds the given items
	 */
	public static LongList with (long item0, long item1, long item2, long item3, long item4) {
		LongList list = new LongList(5);
		list.add(item0);
		list.add(item1);
		list.add(item2);
		list.add(item3);
		list.add(item4);
		return list;
	}

	/**
	 * Creates a new LongList that holds only the given items, but can be resized.
	 * @param item0 a long item
	 * @param item1 a long item
	 * @param item2 a long item
	 * @param item3 a long item
	 * @param item4 a long item
	 * @param item5 a long item
	 * @return a new LongList that holds the given items
	 */
	public static LongList with (long item0, long item1, long item2, long item3, long item4, long item5) {
		LongList list = new LongList(6);
		list.add(item0);
		list.add(item1);
		list.add(item2);
		list.add(item3);
		list.add(item4);
		list.add(item5);
		return list;
	}

	/**
	 * Creates a new LongList that holds only the given items, but can be resized.
	 * @param item0 a long item
	 * @param item1 a long item
	 * @param item2 a long item
	 * @param item3 a long item
	 * @param item4 a long item
	 * @param item5 a long item
	 * @param item6 a long item
	 * @return a new LongList that holds the given items
	 */
	public static LongList with (long item0, long item1, long item2, long item3, long item4, long item5, long item6) {
		LongList list = new LongList(7);
		list.add(item0);
		list.add(item1);
		list.add(item2);
		list.add(item3);
		list.add(item4);
		list.add(item5);
		list.add(item6);
		return list;
	}

	/**
	 * Creates a new LongList that holds only the given items, but can be resized.
	 * @param item0 a long item
	 * @param item1 a long item
	 * @param item2 a long item
	 * @param item3 a long item
	 * @param item4 a long item
	 * @param item5 a long item
	 * @param item6 a long item
	 * @return a new LongList that holds the given items
	 */
	public static LongList with (long item0, long item1, long item2, long item3, long item4, long item5, long item6, long item7) {
		LongList list = new LongList(8);
		list.add(item0);
		list.add(item1);
		list.add(item2);
		list.add(item3);
		list.add(item4);
		list.add(item5);
		list.add(item6);
		list.add(item7);
		return list;
	}

	/**
	 * Creates a new LongList that holds only the given items, but can be resized.
	 * This overload will only be used when an array is supplied and the type of the
	 * items requested is the component type of the array, or if varargs are used and
	 * there are 9 or more arguments.
	 * @param varargs a long varargs or long array; remember that varargs allocate
	 * @return a new LongList that holds the given items
	 */
	public static LongList with (long... varargs) {
		return new LongList(varargs);
	}
}

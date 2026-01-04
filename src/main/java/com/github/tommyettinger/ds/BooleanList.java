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

import com.github.tommyettinger.ds.support.sort.BooleanComparator;
import com.github.tommyettinger.ds.support.sort.BooleanComparators;
import com.github.tommyettinger.ds.support.util.BooleanIterator;
import com.github.tommyettinger.function.BooleanPredicate;

import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Random;

/**
 * A resizable, insertion-ordered boolean list. Primitive-backed, so it avoids the boxing that occurs with an ArrayList of Boolean.
 * This tries to imitate most of the {@link java.util.List} interface, though it can't implement it without boxing its items.
 * Has a primitive iterator accessible via {@link #iterator()}.
 *
 * @author Nathan Sweet
 * @author Tommy Ettinger
 * @see BooleanBag BooleanBag is an unordered variant on BooleanList.
 */
public class BooleanList implements PrimitiveCollection.OfBoolean, Ordered.OfBoolean, Arrangeable {
	/**
	 * Returns true if this implementation retains order, which it does.
	 *
	 * @return true
	 */
	public boolean keepsOrder() {
		return true;
	}

	public boolean[] items;
	protected int size;
	protected transient BooleanListIterator iterator1;
	protected transient BooleanListIterator iterator2;

	/**
	 * Creates an ordered list with a capacity of 10.
	 */
	public BooleanList() {
		this(10);
	}

	/**
	 * Creates an ordered list with the specified capacity.
	 *
	 * @param capacity Any elements added beyond this will cause the backing array to be grown.
	 */
	public BooleanList(int capacity) {
		items = new boolean[capacity];
	}

	/**
	 * Creates an ordered list with the specified capacity.
	 *
	 * @param ordered  ignored; for an unordered list use {@link BooleanBag}
	 * @param capacity Any elements added beyond this will cause the backing array to be grown.
	 * @deprecated BooleanList is always ordered; for an unordered list use {@link BooleanBag}
	 */
	@Deprecated
	public BooleanList(boolean ordered, int capacity) {
		this(capacity);
	}

	/**
	 * Creates a new list containing the elements in the given list. The new list will be ordered. The capacity is set
	 * to the number of elements, so any subsequent elements added will cause the backing array to be grown.
	 *
	 * @param list another BooleanList (or BooleanBag) to copy from
	 */
	public BooleanList(BooleanList list) {
		size = list.size;
		items = new boolean[size];
		System.arraycopy(list.items, 0, items, 0, size);
	}

	/**
	 * Creates a new list containing the elements in the specified array. The capacity is set to the number of elements,
	 * so any subsequent elements added will cause the backing array to be grown.
	 *
	 * @param array a boolean array to copy from
	 */
	public BooleanList(boolean[] array) {
		this(array, 0, array.length);
	}

	/**
	 * Creates a new list containing the elements in the specified array. The capacity is set to the number of elements, so any
	 * subsequent elements added will cause the backing array to be grown.
	 *
	 * @param array      a non-null boolean array to add to this list
	 * @param startIndex the first index in {@code array} to use
	 * @param count      how many items to use from {@code array}
	 */
	public BooleanList(boolean[] array, int startIndex, int count) {
		this(count);
		size = count;
		System.arraycopy(array, startIndex, items, 0, count);
	}

	/**
	 * Creates a new list containing the elements in the specified array. The capacity is set to the number of elements, so any
	 * subsequent elements added will cause the backing array to be grown.
	 *
	 * @param ordered    ignored; for an unordered list use {@link BooleanBag}
	 * @param array      a non-null boolean array to add to this list
	 * @param startIndex the first index in {@code array} to use
	 * @param count      how many items to use from {@code array}
	 * @deprecated BooleanList is always ordered; for an unordered list use {@link BooleanBag}
	 */
	@Deprecated
	public BooleanList(boolean ordered, boolean[] array, int startIndex, int count) {
		this(array, startIndex, count);
	}

	/**
	 * Creates a new list containing the items in the specified PrimitiveCollection.OfBoolean.
	 *
	 * @param coll a primitive collection that will have its contents added to this
	 */
	public BooleanList(OfBoolean coll) {
		this(coll.size());
		addAll(coll);
	}

	/**
	 * Creates a new instance containing the items in the specified iterator.
	 *
	 * @param coll an iterator that will have its remaining contents added to this
	 */
	public BooleanList(BooleanIterator coll) {
		this();
		addAll(coll);
	}

	/**
	 * Copies the given Ordered.OfBoolean into a new BooleanList.
	 *
	 * @param other another Ordered.OfBoolean that will have its contents copied into this
	 */
	public BooleanList(Ordered.OfBoolean other) {
		this(other.order());
	}

	/**
	 * Creates a new list by copying {@code count} items from the given Ordered, starting at {@code offset} in that Ordered,
	 * into this.
	 *
	 * @param other  another Ordered.OfBoolean
	 * @param offset the first index in other's ordering to draw an item from
	 * @param count  how many items to copy from other
	 */
	public BooleanList(Ordered.OfBoolean other, int offset, int count) {
		this(count);
		addAll(0, other, offset, count);
	}

	// Newly-added
	@Override
	public int size() {
		return size;
	}

	// Modified from libGDX
	@Override
	public boolean add(boolean value) {
		boolean[] items = this.items;
		if (size == items.length) {
			items = resize(Math.max(8, (int) (size * 1.75f)));
		}
		items[size++] = value;
		return true;
	}

	public void add(boolean value1, boolean value2) {
		boolean[] items = this.items;
		if (size + 1 >= items.length) {
			items = resize(Math.max(8, (int) (size * 1.75f)));
		}
		items[size] = value1;
		items[size + 1] = value2;
		size += 2;
	}

	public void add(boolean value1, boolean value2, boolean value3) {
		boolean[] items = this.items;
		if (size + 2 >= items.length) {
			items = resize(Math.max(8, (int) (size * 1.75f)));
		}
		items[size] = value1;
		items[size + 1] = value2;
		items[size + 2] = value3;
		size += 3;
	}

	public void add(boolean value1, boolean value2, boolean value3, boolean value4) {
		boolean[] items = this.items;
		if (size + 3 >= items.length) {
			items = resize(Math.max(9, (int) (size * 1.75f)));
		}
		items[size] = value1;
		items[size + 1] = value2;
		items[size + 2] = value3;
		items[size + 3] = value4;
		size += 4;
	}

	// Modified from libGDX
	public boolean addAll(BooleanList list) {
		return addAll(list.items, 0, list.size);
	}

	// Modified from libGDX
	public boolean addAll(BooleanList list, int offset, int count) {
		if (offset + count > list.size) {
			throw new IllegalArgumentException("offset + count must be <= list.size: " + offset + " + " + count + " <= " + list.size);
		}
		return addAll(list.items, offset, count);
	}

	/**
	 * Adds up to {@code count} items, starting from {@code offset}, in the Ordered.OfBoolean {@code other} to this list,
	 * inserting at the end of the iteration order.
	 *
	 * @param other  a non-null {@link Ordered.OfBoolean}
	 * @param offset the first index in {@code other} to use
	 * @param count  how many indices in {@code other} to use
	 * @return true if this is modified by this call, as {@link #addAll(BooleanList)} does
	 */
	public boolean addAll(Ordered.OfBoolean other, int offset, int count) {
		return addAll(size(), other, offset, count);
	}

	/**
	 * Adds up to {@code count} items, starting from {@code offset}, in the Ordered.OfBoolean {@code other} to this list,
	 * inserting starting at {@code insertionIndex} in the iteration order.
	 *
	 * @param insertionIndex where to insert into the iteration order
	 * @param other          a non-null {@link Ordered.OfBoolean}
	 * @param offset         the first index in {@code other} to use
	 * @param count          how many indices in {@code other} to use
	 * @return true if this is modified by this call, as {@link #addAll(BooleanList)} does
	 */
	public boolean addAll(int insertionIndex, Ordered.OfBoolean other, int offset, int count) {
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
	public boolean addAll(boolean... array) {
		return addAll(array, 0, array.length);
	}

	// Modified from libGDX
	public boolean addAll(boolean[] array, int offset, int length) {
		boolean[] items = this.items;
		int sizeNeeded = size + length;
		if (sizeNeeded > items.length) {
			items = resize(Math.max(Math.max(8, sizeNeeded), (int) (size * 1.75f)));
		}
		System.arraycopy(array, offset, items, size, length);
		size += length;
		return true;
	}

	//Kotlin-friendly operator
	public boolean get(int index) {
		if (index >= size) {
			throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);
		}
		return items[index];
	}

	//Kotlin-friendly operator
	public void set(int index, boolean value) {
		if (index >= size) {
			throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);
		}
		items[index] = value;
	}

	public void and(int index, boolean value) {
		if (index >= size) {
			throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);
		}
		items[index] &= value;
	}

	public void or(int index, boolean value) {
		if (index >= size) {
			throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);
		}
		items[index] |= value;
	}

	public void xor(int index, boolean value) {
		if (index >= size) {
			throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);
		}
		items[index] ^= value;
	}

	public void not(int index) {
		if (index >= size) {
			throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);
		}
		items[index] ^= true;
	}

	public void insert(int index, boolean value) {
		if (index > size) {
			throw new IndexOutOfBoundsException("index can't be > size: " + index + " > " + size);
		}
		boolean[] items = this.items;
		if (size == items.length) {
			items = resize(Math.max(8, (int) (size * 1.75f)));
		}
		System.arraycopy(items, index, items, index + 1, size - index);
		size++;
		items[index] = value;
	}

	/**
	 * Inserts the specified number of items at the specified index. The new items will have values equal to the values at those
	 * indices before the insertion, and the previous values will be pushed to after the duplicated range.
	 *
	 * @param index the first index to duplicate
	 * @param count how many items to duplicate
	 */
	public boolean duplicateRange(int index, int count) {
		if (index > size) {
			throw new IndexOutOfBoundsException("index can't be > size: " + index + " > " + size);
		}
		int sizeNeeded = size + count;
		if (sizeNeeded > items.length) {
			items = resize(Math.max(Math.max(8, sizeNeeded), (int) (size * 1.75f)));
		}
		System.arraycopy(items, index, items, index + count, size - index);
		size = sizeNeeded;
		return count > 0;
	}

	/**
	 * Returns this BooleanList, since it is its own order. This is only here to satisfy
	 * the {@link Ordered.OfBoolean} interface.
	 *
	 * @return this BooleanList
	 */
	@Override
	public BooleanList order() {
		return this;
	}

	@Override
	public void swap(int first, int second) {
		if (first >= size) {
			throw new IndexOutOfBoundsException("first can't be >= size: " + first + " >= " + size);
		}
		if (second >= size) {
			throw new IndexOutOfBoundsException("second can't be >= size: " + second + " >= " + size);
		}
		boolean[] items = this.items;
		boolean firstValue = items[first];
		items[first] = items[second];
		items[second] = firstValue;
	}

	@Override
	public boolean contains(boolean value) {
		int i = size - 1;
		boolean[] items = this.items;
		while (i >= 0) {
			if (items[i--] == value) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns true if this BooleanList contains, at least once, every item in {@code other}; otherwise returns false.
	 *
	 * @param other an BooleanList
	 * @return true if this contains every item in {@code other}, otherwise false
	 */
	// Newly-added
	public boolean containsAll(BooleanList other) {
		boolean[] others = other.items;
		int otherSize = other.size;
		for (int i = 0; i < otherSize; i++) {
			if (!contains(others[i])) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns the first index in this list that contains the specified value, or -1 if it is not present.
	 *
	 * @param value a boolean value to search for
	 * @return the first index of the given value, or -1 if it is not present
	 */
	public int indexOf(boolean value) {
		boolean[] items = this.items;
		for (int i = 0, n = size; i < n; i++) {
			if (items[i] == value) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Returns the last index in this list that contains the specified value, or -1 if it is not present.
	 *
	 * @param value a boolean value to search for
	 * @return the last index of the given value, or -1 if it is not present
	 */
	public int lastIndexOf(boolean value) {
		boolean[] items = this.items;
		for (int i = size - 1; i >= 0; i--) {
			if (items[i] == value) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Removes the first occurrence of {@code value} from this BooleanList, returning true if anything was removed.
	 * Otherwise, this returns false.
	 *
	 * @param value the value to (attempt to) remove
	 * @return true if a value was removed, false if the BooleanList is unchanged
	 */
	// Modified from libGDX
	@Override
	public boolean remove(boolean value) {
		boolean[] items = this.items;
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
	 * we also have {@link #remove(boolean)} that removes a value, rather than an index.
	 *
	 * @param index the index of the item to remove and return
	 * @return the removed item
	 */
	public boolean removeAt(int index) {
		if (index >= size) {
			throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);
		}
		boolean[] items = this.items;
		boolean value = items[index];
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
	public void removeRange(int start, int end) {
		int n = size;
		if (end > n) {
			throw new IndexOutOfBoundsException("end can't be > size: " + end + " > " + size);
		}
		if (start > end) {
			throw new IndexOutOfBoundsException("start can't be > end: " + start + " > " + end);
		}
		int count = end - start;
		System.arraycopy(items, start + count, items, start, n - (start + count));
		size = n - count;
	}

	/**
	 * Removes from this BooleanList all occurrences of any elements contained in the specified collection.
	 *
	 * @param c a primitive collection of int items to remove fully, such as another BooleanList or a BooleanDeque
	 * @return true if this list was modified.
	 */
	public boolean removeAll(OfBoolean c) {
		int size = this.size;
		int startSize = size;
		boolean[] items = this.items;
		BooleanIterator it = c.iterator();
		for (int i = 0, n = c.size(); i < n; i++) {
			boolean item = it.nextBoolean();
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
	 * Removes from this BooleanList element-wise occurrences of elements contained in the specified collection.
	 * Note that if a value is present more than once in this BooleanList, only one of those occurrences
	 * will be removed for each occurrence of that value in {@code c}. If {@code c} has the same
	 * contents as this BooleanList or has additional items, then removing each of {@code c} will clear this.
	 *
	 * @param c a primitive collection of int items to remove one-by-one, such as another BooleanList or a BooleanDeque
	 * @return true if this list was modified.
	 */
	public boolean removeEach(OfBoolean c) {
		int size = this.size;
		int startSize = size;
		boolean[] items = this.items;
		BooleanIterator it = c.iterator();
		for (int i = 0, n = c.size(); i < n; i++) {
			boolean item = it.nextBoolean();
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
	 * Removes all items from this BooleanList that are not present somewhere in {@code other}, any number of times.
	 *
	 * @param other a PrimitiveCollection.OfBoolean that contains the items that this should keep, whenever present
	 * @return true if this BooleanList changed as a result of this call, otherwise false
	 */
	// Newly-added
	public boolean retainAll(OfBoolean other) {
		final int size = this.size;
		final boolean[] items = this.items;
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
	 * @param operator a BooleanPredicate (a functional interface defined in funderby)
	 */
	public void replaceAll(BooleanPredicate operator) {
		for (int i = 0, n = size; i < n; i++) {
			items[i] = operator.test(items[i]);
		}
	}

	/**
	 * Replaces the first occurrence of {@code find} with {@code replace}. Returns true if it performed the replacement,
	 * or false if there was nothing to replace. This also returns false if find and replace are the same.
	 * @param find the item to search for
	 * @param replace the item to replace {@code find} with, if possible
	 * @return true if this changed, or false otherwise
	 */
	public boolean replaceFirst(boolean find, boolean replace) {
		if (find != replace) {
			boolean[] items = this.items;
			for (int i = 0, n = size; i < n; i++) {
				if (items[i] == find) {
					items[i] = replace;
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Replaces every occurrence of {@code find} with {@code replace}. Returns the number of changed items, which is 0
	 * if nothing was found or in the case that find and replace are the same.
	 * @param find the item to search for
	 * @param replace the item to replace {@code find} with, if possible
	 * @return the number of replacements that occurred; 0 if nothing was found or replaced
	 */
	public int replaceAll(boolean find, boolean replace) {
		int replacements = 0;
		if (find != replace) {
			boolean[] items = this.items;
			for (int i = 0, n = size; i < n; i++) {
				if (items[i] == find) {
					items[i] = replace;
					++replacements;
				}
			}
		}
		return replacements;
	}

	/**
	 * Removes and returns the last item.
	 *
	 * @return the last item, removed from this
	 */
	public boolean pop() {
		if (size == 0) {
			throw new IndexOutOfBoundsException("BooleanList is empty.");
		}
		return items[--size];
	}

	/**
	 * Returns the last item.
	 *
	 * @return the last item, without modifying this
	 */
	public boolean peek() {
		if (size == 0) {
			throw new IndexOutOfBoundsException("BooleanList is empty.");
		}
		return items[size - 1];
	}

	/**
	 * Returns the first item.
	 *
	 * @return the first item, without modifying this
	 */
	// Modified from libGDX
	public boolean first() {
		if (size == 0) {
			throw new IndexOutOfBoundsException("BooleanList is empty.");
		}
		return items[0];
	}

	/**
	 * Returns true if the list has one or more items, or false otherwise.
	 *
	 * @return true if the list has one or more items, or false otherwise
	 */
	public boolean notEmpty() {
		return size != 0;
	}

	/**
	 * Returns true if the list is empty.
	 *
	 * @return true if the list is empty, or false if it has any items
	 */
	@Override
	public boolean isEmpty() {
		return size == 0;
	}

	/**
	 * Effectively removes all items from this BooleanList.
	 * This is done simply by setting size to 0; because a {@code boolean} item isn't a reference, it doesn't need to be set to null.
	 */
	@Override
	public void clear() {
		size = 0;
	}

	/**
	 * Reduces the size of the backing array to the size of the actual items. This is useful to release memory when many items
	 * have been removed, or if it is known that more items will not be added.
	 *
	 * @return {@link #items}; this will be a different reference if this resized
	 */
	public boolean[] shrink() {
		if (items.length != size) {
			resize(size);
		}
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
	public boolean[] ensureCapacity(int additionalCapacity) {
		if (additionalCapacity < 0) {
			throw new IllegalArgumentException("additionalCapacity must be >= 0: " + additionalCapacity);
		}
		int sizeNeeded = size + additionalCapacity;
		if (sizeNeeded > items.length) {
			resize(Math.max(Math.max(8, sizeNeeded), (int) (size * 1.75f)));
		}
		return items;
	}

	/**
	 * Sets the list size, leaving any values beyond the current size undefined.
	 *
	 * @return {@link #items}; this will be a different reference if this resized to a larger capacity
	 */
	public boolean[] setSize(int newSize) {
		if (newSize < 0) {
			throw new IllegalArgumentException("newSize must be >= 0: " + newSize);
		}
		if (newSize > items.length) {
			resize(Math.max(8, newSize));
		}
		size = newSize;
		return items;
	}

	protected boolean[] resize(int newSize) {
		boolean[] newItems = new boolean[newSize];
		boolean[] items = this.items;
		System.arraycopy(items, 0, newItems, 0, Math.min(size, newItems.length));
		this.items = newItems;
		return newItems;
	}

	/**
	 * Sorts this entire collection using {@link BooleanComparators#sort(boolean[], int, int, BooleanComparator)}
	 * in ascending order (false, then true).
	 */
	public void sort() {
		sort(BooleanComparators.NATURAL_COMPARATOR);
	}

	/**
	 * Uses {@link BooleanComparators#sort(boolean[], int, int, BooleanComparator)} to sort a (clamped) subrange of
	 * this collection in ascending order (false, then true).
	 *
	 * @param from the index of the first element (inclusive) to be sorted
	 * @param to   the index of the last element (exclusive) to be sorted
	 */
	public void sort(int from, int to) {
		sort(from, to, BooleanComparators.NATURAL_COMPARATOR);
	}

	/**
	 * Sorts all elements according to the order induced by the specified
	 * comparator using {@link BooleanComparators#sort(boolean[], int, int, BooleanComparator)}.
	 * If {@code c} is null, this uses {@link BooleanComparators#NATURAL_COMPARATOR} as its c (which
	 * sorts false before true).
	 * <p>This sort is guaranteed to be <i>stable</i>: equal elements will not be reordered as a result
	 * of the sort. The sorting algorithm is an in-place mergesort that is significantly slower than a
	 * standard mergesort, as its running time is <i>O</i>(<var>n</var>&nbsp;(log&nbsp;<var>n</var>)<sup>2</sup>), but
	 * it does not allocate additional memory; as a result, it can be used as a generic sorting algorithm.
	 *
	 * @param c the comparator to determine the order of the BooleanList
	 */
	public void sort(final BooleanComparator c) {
		sort(0, size, c);
	}

	/**
	 * Sorts the specified (clamped) subrange of elements according to the order induced by the specified
	 * comparator using mergesort, or {@link BooleanComparators#NATURAL_COMPARATOR} if {@code c} is null (which
	 * sorts false before true).
	 * This purely uses {@link BooleanComparators#sort(boolean[], int, int, BooleanComparator)}, and you
	 * can see its docs for more information.
	 *
	 * @param from the index of the first element (inclusive) to be sorted.
	 * @param to   the index of the last element (exclusive) to be sorted.
	 * @param c    the comparator to determine the order of the BooleanList
	 */
	public void sort(int from, int to, BooleanComparator c) {
		from = Math.max(Math.min(from, size - 1), 0);
		to = Math.max(Math.min(to, size), from);
		BooleanComparators.sort(items, from, to, c);
	}

	@Override
	public void reverse() {
		boolean[] items = this.items;
		for (int i = 0, lastIndex = size - 1, n = size / 2; i < n; i++) {
			int ii = lastIndex - i;
			boolean temp = items[i];
			items[i] = items[ii];
			items[ii] = temp;
		}
	}

	// Modified from libGDX
	@Override
	public void shuffle(Random random) {
		boolean[] items = this.items;
		for (int i = size - 1; i > 0; i--) {
			int ii = random.nextInt(i + 1);
			boolean temp = items[i];
			items[i] = items[ii];
			items[ii] = temp;
		}
	}

	/**
	 * Reduces the size of the list to the specified size. If the list is already smaller than the specified size, no action is
	 * taken.
	 */
	public void truncate(int newSize) {
		newSize = Math.max(0, newSize);
		if (size > newSize) {
			size = newSize;
		}
	}

	/**
	 * Returns a random item from the list, or false if the list is empty.
	 *
	 * @param random a {@link Random} or a subclass, such as any from juniper
	 * @return a randomly selected item from this, or {@code false} if this is empty
	 */
	public boolean random(Random random) {
		if (size == 0) {
			return false;
		}
		return items[random.nextInt(size)];
	}

	/**
	 * Allocates a new boolean array with {@code size} elements and fills it with the items in this.
	 *
	 * @return a new boolean array with the same contents as this
	 */
	public boolean[] toArray() {
		boolean[] array = new boolean[size];
		System.arraycopy(items, 0, array, 0, size);
		return array;
	}

	/**
	 * If {@code array.length} at least equal to {@link #size()}, this copies the contents of this
	 * into {@code array} and returns it; otherwise, it allocates a new boolean array that can fit all
	 * the items in this, and proceeds to copy into that and return that.
	 *
	 * @param array a boolean array that will be modified if it can fit {@link #size()} items
	 * @return {@code array}, if it had sufficient size, or a new array otherwise, either with a copy of this
	 */
	public boolean[] toArray(boolean[] array) {
		if (array.length < size)
			array = new boolean[size];
		System.arraycopy(items, 0, array, 0, size);
		return array;
	}

	@Override
	public int hashCode() {
		boolean[] items = this.items;
		int h = size;
		for (int i = 0, n = size; i < n; i++) {
			h = h * 31 + (items[i] ? 421 : 5);
		}
		return h ^ h >>> 16;
	}

	@Override
	public boolean equals(Object object) {
		if (object == this) {
			return true;
		}
		if (!(object instanceof BooleanList)) {
			return false;
		}
		BooleanList list = (BooleanList) object;
		int n = size;
		if (n != list.size()) {
			return false;
		}
		boolean[] items1 = this.items, items2 = list.items;
		for (int i = 0; i < n; i++) {
			if (items1[i] != items2[i]) {
				return false;
			}
		}
		return true;
	}

	@Override
	public String toString() {
		return toString(", ", true);
	}

	/**
	 * Returns a new primitive iterator over the items in this BooleanList. Iterates in order if
	 * {@link #keepsOrder()} returns true, which it does for a BooleanList but not a BooleanBag.
	 *
	 * @return a {@link BooleanIterator}; use its nextBoolean() method instead of next()
	 */
	@Override
	public BooleanListIterator iterator() {
		return new BooleanListIterator(this);
	}

	/**
	 * A {@link BooleanIterator}, plus {@link ListIterator} methods, over the elements of a BooleanList.
	 * Use {@link #nextBoolean()} in preference to {@link #next()} to avoid allocating Boolean objects.
	 */
	public static class BooleanListIterator implements BooleanIterator {
		protected int index, latest = -1;
		protected BooleanList list;

		public BooleanListIterator(BooleanList list) {
			this.list = list;
		}

		public BooleanListIterator(BooleanList list, int index) {
			if (index < 0 || index >= list.size())
				throw new IndexOutOfBoundsException("BooleanListIterator does not satisfy index >= 0 && index < list.size()");
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
		public boolean nextBoolean() {
			if (index >= list.size()) {
				throw new NoSuchElementException();
			}
			return list.get(latest = index++);
		}

		/**
		 * Returns {@code true} if the iteration has more elements.
		 * (In other words, returns {@code true} if {@link #nextBoolean} would
		 * return an element rather than throwing an exception.)
		 *
		 * @return {@code true} if the iteration has more elements
		 */
		@Override
		public boolean hasNext() {
			return index < list.size();
		}

		/**
		 * Returns {@code true} if this list iterator has more elements when
		 * traversing the list in the reverse direction.  (In other words,
		 * returns {@code true} if {@link #previousBoolean} would return an element
		 * rather than throwing an exception.)
		 *
		 * @return {@code true} if the list iterator has more elements when
		 * traversing the list in the reverse direction
		 */
		public boolean hasPrevious() {
			return index > 0 && list.notEmpty();
		}

		/**
		 * Returns the previous element in the list and moves the cursor
		 * position backwards.  This method may be called repeatedly to
		 * iterate through the list backwards, or intermixed with calls to
		 * {@link #nextBoolean} to go back and forth.  (Note that alternating calls
		 * to {@code next} and {@code previous} will return the same
		 * element repeatedly.)
		 *
		 * @return the previous element in the list
		 * @throws NoSuchElementException if the iteration has no previous
		 *                                element
		 */
		public boolean previousBoolean() {
			if (index <= 0 || list.isEmpty()) {
				throw new NoSuchElementException();
			}
			return list.get(latest = --index);
		}

		/**
		 * Returns the index of the element that would be returned by a
		 * subsequent call to {@link #nextBoolean}. (Returns list size if the list
		 * iterator is at the end of the list.)
		 *
		 * @return the index of the element that would be returned by a
		 * subsequent call to {@code next}, or list size if the list
		 * iterator is at the end of the list
		 */
		public int nextIndex() {
			return index;
		}

		/**
		 * Returns the index of the element that would be returned by a
		 * subsequent call to {@link #previousBoolean}. (Returns -1 if the list
		 * iterator is at the beginning of the list.)
		 *
		 * @return the index of the element that would be returned by a
		 * subsequent call to {@code previous}, or -1 if the list
		 * iterator is at the beginning of the list
		 */
		public int previousIndex() {
			return index - 1;
		}

		/**
		 * Removes from the list the last element that was returned by {@link
		 * #nextBoolean} or {@link #previousBoolean} (optional operation).  This call can
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
		public void remove() {
			if (latest == -1 || latest >= list.size()) {
				throw new NoSuchElementException();
			}
			list.removeAt(latest);
			index = latest;
			latest = -1;
		}

		/**
		 * Replaces the last element returned by {@link #nextBoolean} or
		 * {@link #previousBoolean} with the specified element (optional operation).
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
		public void set(boolean t) {
			if (latest == -1 || latest >= list.size()) {
				throw new NoSuchElementException();
			}
			list.set(latest, t);
		}

		/**
		 * Inserts the specified element into the list (optional operation).
		 * The element is inserted immediately before the element that
		 * would be returned by {@link #nextBoolean}, if any, and after the element
		 * that would be returned by {@link #previousBoolean}, if any.  (If the
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
		public void add(boolean t) {
			if (index > list.size()) {
				throw new NoSuchElementException();
			}
			list.insert(index, t);
			if (list.keepsOrder()) ++index;
			latest = -1;
		}

		public void reset() {
			index = 0;
			latest = -1;
		}

		public void reset(int index) {
			if (index < 0 || index >= list.size())
				throw new IndexOutOfBoundsException("BooleanListIterator does not satisfy index >= 0 && index < list.size()");
			this.index = index;
			latest = -1;
		}

		/**
		 * Returns an iterator over elements of type {@code boolean}.
		 *
		 * @return this same BooleanListIterator.
		 */
		public BooleanList.BooleanListIterator iterator() {
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
	public static BooleanList with() {
		return new BooleanList(0);
	}

	/**
	 * Creates a new BooleanList that holds only the given item, but can be resized.
	 *
	 * @param item a boolean item
	 * @return a new BooleanList that holds the given item
	 */

	public static BooleanList with(boolean item) {
		BooleanList list = new BooleanList(1);
		list.add(item);
		return list;
	}

	/**
	 * Creates a new BooleanList that holds only the given items, but can be resized.
	 *
	 * @param item0 a boolean item
	 * @param item1 a boolean item
	 * @return a new BooleanList that holds the given items
	 */
	public static BooleanList with(boolean item0, boolean item1) {
		BooleanList list = new BooleanList(2);
		list.add(item0);
		list.add(item1);
		return list;
	}

	/**
	 * Creates a new BooleanList that holds only the given items, but can be resized.
	 *
	 * @param item0 a boolean item
	 * @param item1 a boolean item
	 * @param item2 a boolean item
	 * @return a new BooleanList that holds the given items
	 */
	public static BooleanList with(boolean item0, boolean item1, boolean item2) {
		BooleanList list = new BooleanList(3);
		list.add(item0);
		list.add(item1);
		list.add(item2);
		return list;
	}

	/**
	 * Creates a new BooleanList that holds only the given items, but can be resized.
	 *
	 * @param item0 a boolean item
	 * @param item1 a boolean item
	 * @param item2 a boolean item
	 * @param item3 a boolean item
	 * @return a new BooleanList that holds the given items
	 */
	public static BooleanList with(boolean item0, boolean item1, boolean item2, boolean item3) {
		BooleanList list = new BooleanList(4);
		list.add(item0);
		list.add(item1);
		list.add(item2);
		list.add(item3);
		return list;
	}

	/**
	 * Creates a new BooleanList that holds only the given items, but can be resized.
	 *
	 * @param item0 a boolean item
	 * @param item1 a boolean item
	 * @param item2 a boolean item
	 * @param item3 a boolean item
	 * @param item4 a boolean item
	 * @return a new BooleanList that holds the given items
	 */
	public static BooleanList with(boolean item0, boolean item1, boolean item2, boolean item3, boolean item4) {
		BooleanList list = new BooleanList(5);
		list.add(item0);
		list.add(item1);
		list.add(item2);
		list.add(item3);
		list.add(item4);
		return list;
	}

	/**
	 * Creates a new BooleanList that holds only the given items, but can be resized.
	 *
	 * @param item0 a boolean item
	 * @param item1 a boolean item
	 * @param item2 a boolean item
	 * @param item3 a boolean item
	 * @param item4 a boolean item
	 * @param item5 a boolean item
	 * @return a new BooleanList that holds the given items
	 */
	public static BooleanList with(boolean item0, boolean item1, boolean item2, boolean item3, boolean item4, boolean item5) {
		BooleanList list = new BooleanList(6);
		list.add(item0);
		list.add(item1);
		list.add(item2);
		list.add(item3);
		list.add(item4);
		list.add(item5);
		return list;
	}

	/**
	 * Creates a new BooleanList that holds only the given items, but can be resized.
	 *
	 * @param item0 a boolean item
	 * @param item1 a boolean item
	 * @param item2 a boolean item
	 * @param item3 a boolean item
	 * @param item4 a boolean item
	 * @param item5 a boolean item
	 * @param item6 a boolean item
	 * @return a new BooleanList that holds the given items
	 */
	public static BooleanList with(boolean item0, boolean item1, boolean item2, boolean item3, boolean item4, boolean item5, boolean item6) {
		BooleanList list = new BooleanList(7);
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
	 * Creates a new BooleanList that holds only the given items, but can be resized.
	 *
	 * @param item0 a boolean item
	 * @param item1 a boolean item
	 * @param item2 a boolean item
	 * @param item3 a boolean item
	 * @param item4 a boolean item
	 * @param item5 a boolean item
	 * @param item6 a boolean item
	 * @return a new BooleanList that holds the given items
	 */
	public static BooleanList with(boolean item0, boolean item1, boolean item2, boolean item3, boolean item4, boolean item5, boolean item6, boolean item7) {
		BooleanList list = new BooleanList(8);
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
	 * Creates a new BooleanList that holds only the given items, but can be resized.
	 * This overload will only be used when an array is supplied and the type of the
	 * items requested is the component type of the array, or if varargs are used and
	 * there are 9 or more arguments.
	 *
	 * @param varargs a boolean varargs or boolean array; remember that varargs allocate
	 * @return a new BooleanList that holds the given items
	 */
	public static BooleanList with(boolean... varargs) {
		return new BooleanList(varargs);
	}

	/**
	 * Calls {@link #parse(String, String, boolean)} with brackets set to false.
	 * @param str a String that will be parsed in full
	 * @param delimiter the delimiter between items in str
	 * @return a new collection parsed from str
	 */
	public static BooleanList parse(String str, String delimiter) {
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
	public static BooleanList parse(String str, String delimiter, boolean brackets) {
		BooleanList c = new BooleanList();
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
	public static BooleanList parse(String str, String delimiter, int offset, int length) {
		BooleanList c = new BooleanList();
		c.addLegible(str, delimiter, offset, length);
		return c;
	}
}

/*
 * Copyright (c) 2022 See AUTHORS file.
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

import com.github.tommyettinger.ds.support.function.BooleanUnaryOperator;
import com.github.tommyettinger.ds.support.sort.BooleanComparator;
import com.github.tommyettinger.ds.support.sort.BooleanComparators;
import com.github.tommyettinger.ds.support.util.BooleanIterator;

import javax.annotation.Nullable;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import com.github.tommyettinger.ds.support.EnhancedRandom;

/**
 * A resizable, ordered or unordered boolean list. Primitive-backed, so it avoids the boxing that occurs with an ArrayList of Boolean.
 * If unordered, this class avoids a memory copy when removing elements (the last element is moved to the removed element's position).
 * This tries to imitate most of the {@link java.util.List} interface, though it can't implement it without boxing its items.
 * Has a Java 8 {@link PrimitiveIterator} accessible via {@link #iterator()}.
 *
 * @author Nathan Sweet
 * @author Tommy Ettinger
 */
public class BooleanList implements PrimitiveCollection.OfBoolean, Ordered.OfBoolean, Arrangeable {

	public boolean[] items;
	protected int size;
	public boolean ordered;
	@Nullable protected transient BooleanListIterator iterator1;
	@Nullable protected transient BooleanListIterator iterator2;

	/**
	 * Creates an ordered array with a capacity of 16.
	 */
	public BooleanList () {
		this(true, 16);
	}

	/**
	 * Creates an ordered array with the specified capacity.
	 */
	public BooleanList (int capacity) {
		this(true, capacity);
	}

	/**
	 * @param ordered  If false, methods that remove elements may change the order of other elements in the array, which avoids a
	 *                 memory copy.
	 * @param capacity Any elements added beyond this will cause the backing array to be grown.
	 */
	public BooleanList (boolean ordered, int capacity) {
		this.ordered = ordered;
		items = new boolean[capacity];
	}

	/**
	 * Creates a new list containing the elements in the specific array. The new array will be ordered if the specific array is
	 * ordered. The capacity is set to the number of elements, so any subsequent elements added will cause the backing array to be
	 * grown.
	 */
	public BooleanList (BooleanList array) {
		this.ordered = array.ordered;
		size = array.size;
		items = new boolean[size];
		System.arraycopy(array.items, 0, items, 0, size);
	}

	/**
	 * Creates a new ordered array containing the elements in the specified array. The capacity is set to the number of elements,
	 * so any subsequent elements added will cause the backing array to be grown.
	 */
	public BooleanList (boolean[] array) {
		this(true, array, 0, array.length);
	}

	/**
	 * Creates a new list containing the elements in the specified array. The capacity is set to the number of elements, so any
	 * subsequent elements added will cause the backing array to be grown.
	 */
	public BooleanList (boolean[] array, int startIndex, int count) {
		this(true, array, startIndex, count);
	}

	/**
	 * Creates a new list containing the elements in the specified array. The capacity is set to the number of elements, so any
	 * subsequent elements added will cause the backing array to be grown.
	 *
	 * @param ordered If false, methods that remove elements may change the order of other elements in the array, which avoids a
	 *                memory copy.
	 */
	public BooleanList (boolean ordered, boolean[] array, int startIndex, int count) {
		this(ordered, count);
		size = count;
		System.arraycopy(array, startIndex, items, 0, count);
	}

	/**
	 * Creates a new list containing the items in the specified PrimitiveCollection.OfBoolean. Only this class currently implements
	 * that interface, but user code can as well.
	 * @param coll a primitive collection that will have its contents added to this
	 */
	public BooleanList (OfBoolean coll) {
		this(coll.size());
		addAll(coll);
	}

	/**
	 * Copies the given Ordered.OfBoolean into a new BooleanList.
	 * @param other another Ordered.OfBoolean
	 */
	public BooleanList (Ordered.OfBoolean other) {
		this(other.order());
	}

	/**
	 * Creates a new list by copying {@code count} items from the given Ordered, starting at {@code offset} in that Ordered,
	 * into this.
	 * @param other another Ordered.OfBoolean
	 * @param offset the first index in other's ordering to draw an item from
	 * @param count how many items to copy from other
	 */
	public BooleanList (Ordered.OfBoolean other, int offset, int count) {
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
	public boolean add (boolean value) {
		boolean[] items = this.items;
		if (size == items.length) { items = resize(Math.max(8, (int)(size * 1.75f))); }
		items[size++] = value;
		return true;
	}

	public void add (boolean value1, boolean value2) {
		boolean[] items = this.items;
		if (size + 1 >= items.length) { items = resize(Math.max(8, (int)(size * 1.75f))); }
		items[size] = value1;
		items[size + 1] = value2;
		size += 2;
	}

	public void add (boolean value1, boolean value2, boolean value3) {
		boolean[] items = this.items;
		if (size + 2 >= items.length) { items = resize(Math.max(8, (int)(size * 1.75f))); }
		items[size] = value1;
		items[size + 1] = value2;
		items[size + 2] = value3;
		size += 3;
	}

	public void add (boolean value1, boolean value2, boolean value3, boolean value4) {
		boolean[] items = this.items;
		if (size + 3 >= items.length) {
			items = resize(Math.max(8, (int)(size * 1.8f))); // 1.75 isn't enough when size=5.
		}
		items[size] = value1;
		items[size + 1] = value2;
		items[size + 2] = value3;
		items[size + 3] = value4;
		size += 4;
	}

	// Modified from libGDX
	public boolean addAll (BooleanList array) {
		return addAll(array.items, 0, array.size);
	}

	// Modified from libGDX
	public boolean addAll (BooleanList array, int offset, int length) {
		if (offset + length > array.size) { throw new IllegalArgumentException("offset + length must be <= size: " + offset + " + " + length + " <= " + array.size); }
		return addAll(array.items, offset, length);
	}

	/**
	 * Adds up to {@code count} items, starting from {@code offset}, in the Ordered.OfBoolean {@code other} to this list,
	 * inserting at the end of the iteration order.
	 *
	 * @param other          a non-null {@link Ordered.OfBoolean}
	 * @param offset         the first index in {@code other} to use
	 * @param count          how many indices in {@code other} to use
	 * @return true if this is modified by this call, as {@link #addAll(Ordered.OfBoolean)} does
	 */
	public boolean addAll (Ordered.OfBoolean other, int offset, int count) {
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
	 * @return true if this is modified by this call, as {@link #addAll(Ordered.OfBoolean)} does
	 */
	public boolean addAll (int insertionIndex, Ordered.OfBoolean other, int offset, int count) {
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
	public boolean addAll (boolean... array) {
		return addAll(array, 0, array.length);
	}

	// Modified from libGDX
	public boolean addAll (boolean[] array, int offset, int length) {
		boolean[] items = this.items;
		int sizeNeeded = size + length;
		if (sizeNeeded > items.length) { items = resize(Math.max(Math.max(8, sizeNeeded), (int)(size * 1.75f))); }
		System.arraycopy(array, offset, items, size, length);
		size += length;
		return true;
	}

	//Kotlin-friendly operator
	public boolean get (int index) {
		if (index >= size) { throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size); }
		return items[index];
	}

	//Kotlin-friendly operator
	public void set (int index, boolean value) {
		if (index >= size) { throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size); }
		items[index] = value;
	}

	public void and (int index, boolean value) {
		if (index >= size) { throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size); }
		items[index] &= value;
	}

	public void or (int index, boolean value) {
		if (index >= size) { throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size); }
		items[index] |= value;
	}

	public void xor (int index, boolean value) {
		if (index >= size) { throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size); }
		items[index] ^= value;
	}

	public void not (int index) {
		if (index >= size) { throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size); }
		items[index] ^= true;
	}

	public void insert (int index, boolean value) {
		if (index > size) { throw new IndexOutOfBoundsException("index can't be > size: " + index + " > " + size); }
		boolean[] items = this.items;
		if (size == items.length) { items = resize(Math.max(8, (int)(size * 1.75f))); }
		if (ordered) { System.arraycopy(items, index, items, index + 1, size - index); } else { items[size] = items[index]; }
		size++;
		items[index] = value;
	}

	/**
	 * Inserts the specified number of items at the specified index. The new items will have values equal to the values at those
	 * indices before the insertion.
	 */
	public void insertRange (int index, int count) {
		if (index > size) { throw new IndexOutOfBoundsException("index can't be > size: " + index + " > " + size); }
		int sizeNeeded = size + count;
		if (sizeNeeded > items.length) { items = resize(Math.max(Math.max(8, sizeNeeded), (int)(size * 1.75f))); }
		System.arraycopy(items, index, items, index + count, size - index);
		size = sizeNeeded;
	}

	/**
	 * Returns this BooleanList, since it is its own order. This is only here to satisfy
	 * the {@link Ordered.OfBoolean} interface.
	 *
	 * @return this BooleanList
	 */
	@Override
	public BooleanList order () {
		return this;
	}

	@Override
	public void swap (int first, int second) {
		if (first >= size) { throw new IndexOutOfBoundsException("first can't be >= size: " + first + " >= " + size); }
		if (second >= size) { throw new IndexOutOfBoundsException("second can't be >= size: " + second + " >= " + size); }
		boolean[] items = this.items;
		boolean firstValue = items[first];
		items[first] = items[second];
		items[second] = firstValue;
	}

	@Override
	public boolean contains (boolean value) {
		int i = size - 1;
		boolean[] items = this.items;
		while (i >= 0) { if (items[i--] == value) { return true; } }
		return false;
	}

	/**
	 * Returns true if this BooleanList contains, at least once, every item in {@code other}; otherwise returns false.
	 *
	 * @param other an BooleanList
	 * @return true if this contains every item in {@code other}, otherwise false
	 */
	// Newly-added
	public boolean containsAll (BooleanList other) {
		boolean[] others = other.items;
		int otherSize = other.size;
		for (int i = 0; i < otherSize; i++) {
			if (!contains(others[i])) { return false; }
		}
		return true;
	}

	/**
	 * Returns the first index in this list that contains the specified value, or -1 if it is not present.
	 * @param value a boolean value to search for
	 * @return the first index of the given value, or -1 if it is not present
	 */
	public int indexOf (boolean value) {
		boolean[] items = this.items;
		for (int i = 0, n = size; i < n; i++) { if (items[i] == value) { return i; } }
		return -1;
	}

	/**
	 * Returns the last index in this list that contains the specified value, or -1 if it is not present.
	 * @param value a boolean value to search for
	 * @return the last index of the given value, or -1 if it is not present
	 */
	public int lastIndexOf (boolean value) {
		boolean[] items = this.items;
		for (int i = size - 1; i >= 0; i--) { if (items[i] == value) { return i; } }
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
	public boolean remove (boolean value) {
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
	public boolean removeAt (int index) {
		if (index >= size) { throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size); }
		boolean[] items = this.items;
		boolean value = items[index];
		size--;
		if (ordered) { System.arraycopy(items, index + 1, items, index, size - index); } else { items[index] = items[size]; }
		return value;
	}

	/**
	 * Removes the items between the specified start index, inclusive, and end index, exclusive.
	 * Note that this takes different arguments than some other range-related methods; this needs
	 * a start index and an end index, rather than a count of items. This matches the behavior in
	 * the JDK collections.
	 * @param start the first index to remove, inclusive
	 * @param end the last index (after what should be removed), exclusive
	 */
	public void removeRange (int start, int end) {
		int n = size;
		if (end >= n) { throw new IndexOutOfBoundsException("end can't be >= size: " + end + " >= " + size); }
		if (start > end) { throw new IndexOutOfBoundsException("start can't be > end: " + start + " > " + end); }
		int count = end - start, lastIndex = n - count;
		if (ordered) { System.arraycopy(items, start + count, items, start, n - (start + count)); } else {
			int i = Math.max(lastIndex, end);
			System.arraycopy(items, i, items, start, n - i);
		}
		size = n - count;
	}

	/**
	 * Removes from this array all of elements contained in the specified array.
	 * Note that if a value is present more than once in this BooleanList, only one of those occurrences
	 * will be removed for each occurrence of that value in {@code array}. If {@code array} has the same
	 * contents as this BooleanList or has additional items, then removing all of {@code array} will clear this.
	 *
	 * @return true if this array was modified.
	 */
	public boolean removeAll (BooleanList array) {
		int size = this.size;
		int startSize = size;
		boolean[] items = this.items;
		for (int i = 0, n = array.size; i < n; i++) {
			boolean item = array.get(i);
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
	 * @param other an BooleanList that contains the items that this should keep, whenever present
	 * @return true if this BooleanList changed as a result of this call, otherwise false
	 */
	// Newly-added
	public boolean retainAll (BooleanList other) {
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
	 * @param operator a BooleanUnaryOperator (an interface defined in the JDK)
	 */
	public void replaceAll(BooleanUnaryOperator operator) {
		for (int i = 0, n = size; i < n; i++) {
			items[i] = operator.applyAsBoolean(items[i]);
		}
	}

	/**
	 * Removes and returns the last item.
	 * @return the last item, removed from this
	 */
	public boolean pop () {
		if (size == 0) { throw new IndexOutOfBoundsException("BooleanList is empty."); }
		return items[--size];
	}

	/**
	 * Returns the last item.
	 * @return the last item, without modifying this
	 */
	public boolean peek () {
		if (size == 0) { throw new IndexOutOfBoundsException("BooleanList is empty."); }
		return items[size - 1];
	}

	/**
	 * Returns the first item.
	 * @return the first item, without modifying this
	 */
	// Modified from libGDX
	public boolean first () {
		if (size == 0) { throw new IndexOutOfBoundsException("BooleanList is empty."); }
		return items[0];
	}

	/**
	 * Returns true if the array has one or more items, or false otherwise.
	 * @return true if the array has one or more items, or false otherwise
	 */
	public boolean notEmpty () {
		return size > 0;
	}

	/**
	 * Returns true if the array is empty.
	 * @return true if the array is empty, or false if it has any items
	 */
	@Override
	public boolean isEmpty () {
		return size == 0;
	}

	/**
	 * Effectively removes all items from this BooleanList.
	 * This is done simply by setting size to 0; because a {@code boolean} item isn't a reference, it doesn't need to be set to null.
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
	public boolean[] shrink () {
		if (items.length != size) { resize(size); }
		return items;
	}

	/**
	 * Increases the size of the backing array to accommodate the specified number of additional items. Useful before adding many
	 * items to avoid multiple backing array resizes.
	 *
	 * @return {@link #items}; this will be a different reference if this resized
	 */
	public boolean[] ensureCapacity (int additionalCapacity) {
		if (additionalCapacity < 0) { throw new IllegalArgumentException("additionalCapacity must be >= 0: " + additionalCapacity); }
		int sizeNeeded = size + additionalCapacity;
		if (sizeNeeded > items.length) { resize(Math.max(Math.max(8, sizeNeeded), (int)(size * 1.75f))); }
		return items;
	}

	/**
	 * Sets the array size, leaving any values beyond the current size undefined.
	 *
	 * @return {@link #items}; this will be a different reference if this resized to a larger capacity
	 */
	public boolean[] setSize (int newSize) {
		if (newSize < 0) { throw new IllegalArgumentException("newSize must be >= 0: " + newSize); }
		if (newSize > items.length) { resize(Math.max(8, newSize)); }
		size = newSize;
		return items;
	}

	protected boolean[] resize (int newSize) {
		boolean[] newItems = new boolean[newSize];
		boolean[] items = this.items;
		System.arraycopy(items, 0, newItems, 0, Math.min(size, newItems.length));
		this.items = newItems;
		return newItems;
	}

	public void sort () {
		sort(BooleanComparators.NATURAL_COMPARATOR);
	}

	@Override
	public void reverse () {
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
	public void shuffle (EnhancedRandom random) {
		boolean[] items = this.items;
		for (int i = size - 1; i >= 0; i--) {
			int ii = random.nextInt(i + 1);
			boolean temp = items[i];
			items[i] = items[ii];
			items[ii] = temp;
		}
	}

	/**
	 * Reduces the size of the array to the specified size. If the array is already smaller than the specified size, no action is
	 * taken.
	 */
	public void truncate (int newSize) {
		if (size > newSize) { size = newSize; }
	}

	/**
	 * Returns a random item from the array, or false if the array is empty.
	 * @param random a {@link EnhancedRandom} such as {@link com.github.tommyettinger.ds.support.LaserRandom} from this library
	 * @return a randomly selected item from this, or {@code 0} if this is empty
	 */
	public boolean random (EnhancedRandom random) {
		if (size == 0) { return false; }
		return items[random.nextInt(size)];
	}

	/**
	 * Allocates a new boolean array with {@code size} elements and fills it with the items in this.
	 * @return a new boolean array with the same contents as this
	 */
	public boolean[] toArray () {
		boolean[] array = new boolean[size];
		System.arraycopy(items, 0, array, 0, size);
		return array;
	}

	/**
	 * If {@code array.length} at least equal to {@link #size()}, this copies the contents of this
	 * into {@code array} and returns it; otherwise, it allocates a new boolean array that can fit all
	 * of the items in this, and proceeds to copy into that and return that.
	 * @param array a boolean array that will be modified if it can fit {@link #size()} items
	 * @return {@code array}, if it had sufficient size, or a new array otherwise, either with a copy of this
	 */
	public boolean[] toArray (boolean[] array) {
		if(array.length < size)
			array = new boolean[size];
		System.arraycopy(items, 0, array, 0, size);
		return array;
	}

	@Override
	public int hashCode () {
		boolean[] items = this.items;
		int h = 1;
		if (ordered) {
			for (int i = 0, n = size; i < n; i++) { h = h * 31 + (items[i] ? 421 : 5); }
		} else {
			for (int i = 0, n = size; i < n; i++) {
				h += items[i] ? 421 : 5;
			}
		}
		return h;
	}

	/**
	 * Returns false if either array is unordered.
	 */
	@Override
	public boolean equals (Object object) {
		if (object == this) { return true; }
		if (!ordered) { return false; }
		if (!(object instanceof BooleanList)) { return false; }
		BooleanList array = (BooleanList)object;
		if (!array.ordered) { return false; }
		int n = size;
		if (n != array.size) { return false; }
		boolean[] items1 = this.items, items2 = array.items;
		for (int i = 0; i < n; i++) { if (items1[i] != items2[i]) { return false; } }
		return true;
	}

	@Override
	public String toString () {
		if (size == 0) { return "[]"; }
		boolean[] items = this.items;
		StringBuilder buffer = new StringBuilder(32);
		buffer.append('[');
		buffer.append(items[0]);
		for (int i = 1; i < size; i++) {
			buffer.append(", ");
			buffer.append(items[i]);
		}
		buffer.append(']');
		return buffer.toString();
	}

	public String toString (String separator) {
		if (size == 0) { return ""; }
		boolean[] items = this.items;
		StringBuilder buffer = new StringBuilder(32);
		buffer.append(items[0]);
		for (int i = 1; i < size; i++) {
			buffer.append(separator);
			buffer.append(items[i]);
		}
		return buffer.toString();
	}

	/**
	 * Returns a Java 8 primitive iterator over the int items in this BooleanList. Iterates in order if {@link #ordered}
	 * is true, otherwise this is not guaranteed to iterate in the same order as items were added.
	 * <br>
	 * This will reuse one of two iterators in this BooleanList; this does not allow nested iteration.
	 * Use {@link BooleanListIterator#BooleanListIterator(BooleanList)} to nest iterators.
	 *
	 * @return a {@link BooleanIterator}; use its nextBoolean() method instead of next()
	 */
	@Override
	public BooleanListIterator iterator () {
		if (iterator1 == null || iterator2 == null) {
			iterator1 = new BooleanListIterator(this);
			iterator2 = new BooleanListIterator(this);
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

	public static class BooleanListIterator implements BooleanIterator {
		protected int index = 0;
		protected BooleanList list;
		protected boolean valid = true;

		public BooleanListIterator (BooleanList list) {
			this.list = list;
		}

		/**
		 * Returns the next {@code int} element in the iteration.
		 *
		 * @return the next {@code int} element in the iteration
		 * @throws NoSuchElementException if the iteration has no more elements
		 */
		@Override
		public boolean nextBoolean () {
			if (!valid) { throw new RuntimeException("#iterator() cannot be used nested."); }
			if (index >= list.size) { throw new NoSuchElementException(); }
			return list.get(index++);
		}

		@Override
		public void remove () {
			if (!valid) { throw new RuntimeException("#iterator() cannot be used nested."); }
			if (index >= list.size) { throw new NoSuchElementException(); }
			list.removeAt(index);
		}

		/**
		 * Returns {@code true} if the iteration has more elements.
		 * (In other words, returns {@code true} if {@link #next} would
		 * return an element rather than throwing an exception.)
		 *
		 * @return {@code true} if the iteration has more elements
		 */
		@Override
		public boolean hasNext () {
			if (!valid) { throw new RuntimeException("#iterator() cannot be used nested."); }
			return index < list.size;
		}

		public void reset () {
			index = 0;
		}
	}

	public static BooleanList with(boolean item) {
		BooleanList list = new BooleanList(1);
		list.add(item);
		return list;
	}

	/**
	 * @see #BooleanList(boolean[])
	 */
	public static BooleanList with (boolean... array) {
		return new BooleanList(array);
	}

/// The remainder of the code is based on FastUtil.

	/**
	 * Transforms two consecutive sorted ranges into a single sorted range. The initial ranges are
	 * {@code [first..middle)} and {@code [middle..last)}, and the resulting range is
	 * {@code [first..last)}. Elements in the first input range will precede equal elements in
	 * the second.
	 */
	private void inPlaceMerge (final int from, int mid, final int to, final BooleanComparator comp) {
		if (from >= mid || mid >= to) { return; }
		if (to - from == 2) {
			if (comp.compare(get(mid), get(from)) < 0) { swap(from, mid); }
			return;
		}

		int firstCut;
		int secondCut;

		if (mid - from > to - mid) {
			firstCut = from + (mid - from) / 2;
			secondCut = lowerBound(mid, to, firstCut, comp);
		} else {
			secondCut = mid + (to - mid) / 2;
			firstCut = upperBound(from, mid, secondCut, comp);
		}

		int first2 = firstCut;
		int middle2 = mid;
		int last2 = secondCut;
		if (middle2 != first2 && middle2 != last2) {
			int first1 = first2;
			int last1 = middle2;
			while (first1 < --last1) { swap(first1++, last1); }
			first1 = middle2;
			last1 = last2;
			while (first1 < --last1) { swap(first1++, last1); }
			first1 = first2;
			last1 = last2;
			while (first1 < --last1) { swap(first1++, last1); }
		}

		mid = firstCut + secondCut - mid;
		inPlaceMerge(from, firstCut, mid, comp);
		inPlaceMerge(mid, secondCut, to, comp);
	}

	/**
	 * Performs a binary search on an already-sorted range: finds the first position where an
	 * element can be inserted without violating the ordering. Sorting is by a user-supplied
	 * comparison function.
	 *
	 * @param from the index of the first element (inclusive) to be included in the binary search.
	 * @param to   the index of the last element (exclusive) to be included in the binary search.
	 * @param pos  the position of the element to be searched for.
	 * @param comp the comparison function.
	 * @return the largest index i such that, for every j in the range {@code [first..i)},
	 * {@code comp.compare(get(j), get(pos))} is {@code true}.
	 */
	private int lowerBound (int from, final int to, final int pos, final BooleanComparator comp) {
		int len = to - from;
		boolean[] items = this.items;
		while (len > 0) {
			int half = len / 2;
			int middle = from + half;
			if (comp.compare(items[middle], items[pos]) < 0) {
				from = middle + 1;
				len -= half + 1;
			} else {
				len = half;
			}
		}
		return from;
	}

	/**
	 * Performs a binary search on an already sorted range: finds the last position where an element
	 * can be inserted without violating the ordering. Sorting is by a user-supplied comparison
	 * function.
	 *
	 * @param from the index of the first element (inclusive) to be included in the binary search.
	 * @param to   the index of the last element (exclusive) to be included in the binary search.
	 * @param pos  the position of the element to be searched for.
	 * @param comp the comparison function.
	 * @return The largest index i such that, for every j in the range {@code [first..i)},
	 * {@code comp.compare(get(pos), get(j))} is {@code false}.
	 */
	private int upperBound (int from, final int to, final int pos, final BooleanComparator comp) {
		int len = to - from;
		boolean[] items = this.items;
		while (len > 0) {
			int half = len / 2;
			int middle = from + half;
			if (comp.compare(items[pos], items[middle]) < 0) {
				len = half;
			} else {
				from = middle + 1;
				len -= half + 1;
			}
		}
		return from;
	}

	/**
	 * Sorts all elements according to the order induced by the specified
	 * comparator using mergesort. If {@code c} is null, this instead uses
	 * {@link BooleanComparators#NATURAL_COMPARATOR} to sort.
	 *
	 * <p>This sort is guaranteed to be <i>stable</i>: equal elements will not be reordered as a result
	 * of the sort. The sorting algorithm is an in-place mergesort that is significantly slower than a
	 * standard mergesort, as its running time is <i>O</i>(<var>n</var>&nbsp;(log&nbsp;<var>n</var>)<sup>2</sup>), but it does not allocate additional memory; as a result, it can be
	 * used as a generic sorting algorithm.
	 *
	 * @param c the comparator to determine the order of the BooleanList
	 */
	public void sort (@Nullable final BooleanComparator c) {
		if (c == null) {
			sort(0, size, BooleanComparators.NATURAL_COMPARATOR);
		} else {
			sort(0, size, c);
		}
	}

	/**
	 * Sorts the specified range of elements according to the order induced by the specified
	 * comparator using mergesort.
	 *
	 * <p>This sort is guaranteed to be <i>stable</i>: equal elements will not be reordered as a result
	 * of the sort. The sorting algorithm is an in-place mergesort that is significantly slower than a
	 * standard mergesort, as its running time is <i>O</i>(<var>n</var>&nbsp;(log&nbsp;<var>n</var>)<sup>2</sup>), but it does not allocate additional memory; as a result, it can be
	 * used as a generic sorting algorithm.
	 *
	 * @param from the index of the first element (inclusive) to be sorted.
	 * @param to   the index of the last element (exclusive) to be sorted.
	 * @param c    the comparator to determine the order of the BooleanList
	 */
	public void sort (final int from, final int to, final BooleanComparator c) {
		if (to <= 0) {
			return;
		}
		if (from < 0 || from >= size || to > size) {
			throw new UnsupportedOperationException("The given from/to range in BooleanList.sort() is invalid.");
		}
		/*
		 * We retain the same method signature as quickSort. Given only a comparator and this list
		 * do not know how to copy and move elements from/to temporary arrays. Hence, in contrast to
		 * the JDK mergesorts this is an "in-place" mergesort, i.e. does not allocate any temporary
		 * arrays. A non-inplace mergesort would perhaps be faster in most cases, but would require
		 * non-intuitive delegate objects...
		 */
		final int length = to - from;

		boolean[] items = this.items;

		// Insertion sort on smallest arrays, less than 16 items
		if (length < 16) {
			for (int i = from; i < to; i++) {
				for (int j = i; j > from && c.compare(items[j - 1], items[j]) > 0; j--) {
					swap(j, j - 1);
				}
			}
			return;
		}

		// Recursively sort halves
		int mid = from + to >>> 1;
		sort(from, mid, c);
		sort(mid, to, c);

		// If list is already sorted, nothing left to do. This is an
		// optimization that results in faster sorts for nearly ordered lists.
		if (c.compare(items[mid - 1], items[mid]) <= 0) { return; }

		// Merge sorted halves
		inPlaceMerge(from, mid, to, c);
	}
}

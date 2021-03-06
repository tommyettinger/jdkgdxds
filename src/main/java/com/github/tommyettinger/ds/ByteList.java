/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
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
 ******************************************************************************/

package com.github.tommyettinger.ds;

import com.github.tommyettinger.ds.support.function.ByteUnaryOperator;
import com.github.tommyettinger.ds.support.sort.ByteComparator;
import com.github.tommyettinger.ds.support.sort.ByteComparator;
import com.github.tommyettinger.ds.support.sort.ByteComparators;
import com.github.tommyettinger.ds.support.util.ByteIterator;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import com.github.tommyettinger.ds.support.EnhancedRandom;

/**
 * A resizable, ordered or unordered byte list. Primitive-backed, so it avoids the boxing that occurs with an ArrayList of Byte.
 * If unordered, this class avoids a memory copy when removing elements (the last element is moved to the removed element's position).
 * This tries to imitate most of the {@link java.util.List} interface, though it can't implement it without boxing its items.
 * Has a Java 8 {@link PrimitiveIterator} accessible via {@link #iterator()}.
 *
 * @author Nathan Sweet
 * @author Tommy Ettinger
 */
public class ByteList implements PrimitiveCollection.OfByte, Ordered.OfByte, Arrangeable {

	public byte[] items;
	protected int size;
	public boolean ordered;
	@Nullable protected transient ByteListIterator iterator1;
	@Nullable protected transient ByteListIterator iterator2;

	/**
	 * Creates an ordered array with a capacity of 16.
	 */
	public ByteList () {
		this(true, 16);
	}

	/**
	 * Creates an ordered array with the specified capacity.
	 */
	public ByteList (int capacity) {
		this(true, capacity);
	}

	/**
	 * @param ordered  If false, methods that remove elements may change the order of other elements in the array, which avoids a
	 *                 memory copy.
	 * @param capacity Any elements added beyond this will cause the backing array to be grown.
	 */
	public ByteList (boolean ordered, int capacity) {
		this.ordered = ordered;
		items = new byte[capacity];
	}

	/**
	 * Creates a new list containing the elements in the specific array. The new array will be ordered if the specific array is
	 * ordered. The capacity is set to the number of elements, so any subsequent elements added will cause the backing array to be
	 * grown.
	 */
	public ByteList (ByteList array) {
		this.ordered = array.ordered;
		size = array.size;
		items = new byte[size];
		System.arraycopy(array.items, 0, items, 0, size);
	}

	/**
	 * Creates a new ordered array containing the elements in the specified array. The capacity is set to the number of elements,
	 * so any subsequent elements added will cause the backing array to be grown.
	 */
	public ByteList (byte[] array) {
		this(true, array, 0, array.length);
	}

	/**
	 * Creates a new list containing the elements in the specified array. The capacity is set to the number of elements, so any
	 * subsequent elements added will cause the backing array to be grown.
	 */
	public ByteList (byte[] array, int startIndex, int count) {
		this(true, array, startIndex, count);
	}

	/**
	 * Creates a new list containing the elements in the specified array. The capacity is set to the number of elements, so any
	 * subsequent elements added will cause the backing array to be grown.
	 *
	 * @param ordered If false, methods that remove elements may change the order of other elements in the array, which avoids a
	 *                memory copy.
	 */
	public ByteList (boolean ordered, byte[] array, int startIndex, int count) {
		this(ordered, count);
		size = count;
		System.arraycopy(array, startIndex, items, 0, count);
	}

	/**
	 * Creates a new list containing the items in the specified PrimitiveCollection.OfByte. Only this class currently implements
	 * that interface, but user code can as well.
	 * @param coll a primitive collection that will have its contents added to this
	 */
	public ByteList (OfByte coll) {
		this(coll.size());
		addAll(coll);
	}

	// Newly-added
	@Override
	public int size () {
		return size;
	}

	// Modified from libGDX
	@Override
	public boolean add (byte value) {
		byte[] items = this.items;
		if (size == items.length) { items = resize(Math.max(8, (int)(size * 1.75f))); }
		items[size++] = value;
		return true;
	}

	public void add (byte value1, byte value2) {
		byte[] items = this.items;
		if (size + 1 >= items.length) { items = resize(Math.max(8, (int)(size * 1.75f))); }
		items[size] = value1;
		items[size + 1] = value2;
		size += 2;
	}

	public void add (byte value1, byte value2, byte value3) {
		byte[] items = this.items;
		if (size + 2 >= items.length) { items = resize(Math.max(8, (int)(size * 1.75f))); }
		items[size] = value1;
		items[size + 1] = value2;
		items[size + 2] = value3;
		size += 3;
	}

	public void add (byte value1, byte value2, byte value3, byte value4) {
		byte[] items = this.items;
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
	public boolean addAll (ByteList array) {
		return addAll(array.items, 0, array.size);
	}

	// Modified from libGDX
	public boolean addAll (ByteList array, int offset, int length) {
		if (offset + length > array.size) { throw new IllegalArgumentException("offset + length must be <= size: " + offset + " + " + length + " <= " + array.size); }
		return addAll(array.items, offset, length);
	}

	// Modified from libGDX
	public boolean addAll (byte... array) {
		return addAll(array, 0, array.length);
	}

	// Modified from libGDX
	public boolean addAll (byte[] array, int offset, int length) {
		byte[] items = this.items;
		int sizeNeeded = size + length;
		if (sizeNeeded > items.length) { items = resize(Math.max(Math.max(8, sizeNeeded), (int)(size * 1.75f))); }
		System.arraycopy(array, offset, items, size, length);
		size += length;
		return true;
	}

	//Kotlin-friendly operator
	public byte get (int index) {
		if (index >= size) { throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size); }
		return items[index];
	}

	//Kotlin-friendly operator
	public void set (int index, byte value) {
		if (index >= size) { throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size); }
		items[index] = value;
	}

	// Modified from libGDX
	public void plus (int index, byte value) {
		if (index >= size) { throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size); }
		items[index] += value;
	}

	/**
	 * Adds {@code value} to each item in this ByteList, stores it in this and returns it.
	 * The presence of this method allows Kotlin code to use the {@code +} operator (though it
	 * shouldn't be used more than once in an expression, because this method modifies this ByteList).
	 *
	 * @param value each item in this will be assigned {@code item + value}
	 * @return this for chaining and Kotlin compatibility
	 */
	// Modified from libGDX
	// Kotlin-friendly operator
	public ByteList plus (byte value) {
		byte[] items = this.items;
		for (int i = 0, n = size; i < n; i++) { items[i] += value; }
		return this;
	}

	// Modified from libGDX
	public void times (int index, byte value) {
		if (index >= size) { throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size); }
		items[index] *= value;
	}

	/**
	 * Multiplies each item in this ByteList by {@code value}, stores it in this and returns it.
	 * The presence of this method allows Kotlin code to use the {@code *} operator (though it
	 * shouldn't be used more than once in an expression, because this method modifies this ByteList).
	 *
	 * @param value each item in this will be assigned {@code item * value}
	 * @return this for chaining and Kotlin compatibility
	 */
	// Modified from libGDX
	// Kotlin-friendly operator
	public ByteList times (byte value) {
		byte[] items = this.items;
		for (int i = 0, n = size; i < n; i++) { items[i] *= value; }
		return this;
	}

	// Newly-added
	public void minus (int index, byte value) {
		if (index >= size) { throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size); }
		items[index] -= value;
	}

	/**
	 * Takes each item in this ByteList and subtracts {@code value}, stores it in this and returns it.
	 * This is just a minor convenience in Java, but the presence of this method allows Kotlin code to use
	 * the {@code -} operator (though it shouldn't be used more than once in an expression, because
	 * this method modifies this ByteList).
	 *
	 * @param value each item in this will be assigned {@code item - value}
	 * @return this for chaining and Kotlin compatibility
	 */
	// Newly-added
	// Kotlin-friendly operator
	public ByteList minus (byte value) {
		byte[] items = this.items;
		for (int i = 0, n = size; i < n; i++) { items[i] -= value; }
		return this;
	}

	// Newly-added
	public void div (int index, byte value) {
		if (index >= size) { throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size); }
		items[index] /= value;
	}

	/**
	 * Divides each item in this ByteList by {@code value}, stores it in this and returns it.
	 * The presence of this method allows Kotlin code to use the {@code /} operator (though it
	 * shouldn't be used more than once in an expression, because this method modifies this ByteList).
	 *
	 * @param value each item in this will be assigned {@code item / value}
	 * @return this for chaining and Kotlin compatibility
	 */
	// Newly-added
	// Kotlin-friendly operator
	public ByteList div (byte value) {
		byte[] items = this.items;
		for (int i = 0, n = size; i < n; i++) { items[i] /= value; }
		return this;
	}

	// Newly-added
	public void rem (int index, byte value) {
		if (index >= size) { throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size); }
		items[index] %= value;
	}

	/**
	 * Gets the remainder of each item in this ByteList with {@code value}, stores it in this and returns it.
	 * The presence of this method allows Kotlin code to use the {@code %} operator (though it
	 * shouldn't be used more than once in an expression, because this method modifies this ByteList).
	 *
	 * @param value each item in this will be assigned {@code item % value}
	 * @return this for chaining and Kotlin compatibility
	 */
	// Newly-added
	// Kotlin-friendly operator
	public ByteList rem (byte value) {
		byte[] items = this.items;
		for (int i = 0, n = size; i < n; i++) { items[i] %= value; }
		return this;
	}

	public void insert (int index, byte value) {
		if (index > size) { throw new IndexOutOfBoundsException("index can't be > size: " + index + " > " + size); }
		byte[] items = this.items;
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
	 * Returns this ByteList, since it is its own order. This is only here to satisfy
	 * the {@link Ordered.OfByte} interface.
	 *
	 * @return this ByteList
	 */
	@Override
	public ByteList order () {
		return this;
	}

	@Override
	public void swap (int first, int second) {
		if (first >= size) { throw new IndexOutOfBoundsException("first can't be >= size: " + first + " >= " + size); }
		if (second >= size) { throw new IndexOutOfBoundsException("second can't be >= size: " + second + " >= " + size); }
		byte[] items = this.items;
		byte firstValue = items[first];
		items[first] = items[second];
		items[second] = firstValue;
	}

	@Override
	public boolean contains (byte value) {
		int i = size - 1;
		byte[] items = this.items;
		while (i >= 0) { if (items[i--] == value) { return true; } }
		return false;
	}

	/**
	 * Returns true if this ByteList contains, at least once, every item in {@code other}; otherwise returns false.
	 *
	 * @param other an ByteList
	 * @return true if this contains every item in {@code other}, otherwise false
	 */
	// Newly-added
	public boolean containsAll (ByteList other) {
		byte[] others = other.items;
		int otherSize = other.size;
		for (int i = 0; i < otherSize; i++) {
			if (!contains(others[i])) { return false; }
		}
		return true;
	}

	/**
	 * Returns the first index in this list that contains the specified value, or -1 if it is not present.
	 * @param value a byte value to search for
	 * @return the first index of the given value, or -1 if it is not present
	 */
	public int indexOf (byte value) {
		byte[] items = this.items;
		for (int i = 0, n = size; i < n; i++) { if (items[i] == value) { return i; } }
		return -1;
	}

	/**
	 * Returns the last index in this list that contains the specified value, or -1 if it is not present.
	 * @param value a byte value to search for
	 * @return the last index of the given value, or -1 if it is not present
	 */
	public int lastIndexOf (byte value) {
		byte[] items = this.items;
		for (int i = size - 1; i >= 0; i--) { if (items[i] == value) { return i; } }
		return -1;
	}

	/**
	 * Removes the first occurrence of {@code value} from this ByteList, returning true if anything was removed.
	 * Otherwise, this returns false.
	 *
	 * @param value the value to (attempt to) remove
	 * @return true if a value was removed, false if the ByteList is unchanged
	 */
	// Modified from libGDX
	@Override
	public boolean remove (byte value) {
		byte[] items = this.items;
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
	 * we also have {@link #remove(byte)} that removes a value, rather than an index.
	 *
	 * @param index the index of the item to remove and return
	 * @return the removed item
	 */
	public byte removeAt (int index) {
		if (index >= size) { throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size); }
		byte[] items = this.items;
		byte value = items[index];
		size--;
		if (ordered) { System.arraycopy(items, index + 1, items, index, size - index); } else { items[index] = items[size]; }
		return value;
	}

	/**
	 * Removes the items between the specified indices, inclusive.
	 */
	public void removeRange (int startIndex, int endIndex) {
		int n = size;
		if (endIndex >= n) { throw new IndexOutOfBoundsException("end can't be >= size: " + endIndex + " >= " + size); }
		if (startIndex > endIndex) { throw new IndexOutOfBoundsException("start can't be > end: " + startIndex + " > " + endIndex); }
		int count = endIndex - startIndex + 1, lastIndex = n - count;
		if (ordered) { System.arraycopy(items, startIndex + count, items, startIndex, n - (startIndex + count)); } else {
			int i = Math.max(lastIndex, endIndex + 1);
			System.arraycopy(items, i, items, startIndex, n - i);
		}
		size = n - count;
	}

	/**
	 * Removes from this array all of elements contained in the specified array.
	 * Note that if a value is present more than once in this ByteList, only one of those occurrences
	 * will be removed for each occurrence of that value in {@code array}. If {@code array} has the same
	 * contents as this ByteList or has additional items, then removing all of {@code array} will clear this.
	 *
	 * @return true if this array was modified.
	 */
	public boolean removeAll (ByteList array) {
		int size = this.size;
		int startSize = size;
		byte[] items = this.items;
		for (int i = 0, n = array.size; i < n; i++) {
			byte item = array.get(i);
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
	 * Removes all items from this ByteList that are not present somewhere in {@code other}, any number of times.
	 *
	 * @param other an ByteList that contains the items that this should keep, whenever present
	 * @return true if this ByteList changed as a result of this call, otherwise false
	 */
	// Newly-added
	public boolean retainAll (ByteList other) {
		final int size = this.size;
		final byte[] items = this.items;
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
	 * @param operator a ByteUnaryOperator (an interface defined in the JDK)
	 */
	public void replaceAll(ByteUnaryOperator operator) {
		for (int i = 0, n = size; i < n; i++) {
			items[i] = operator.applyAsByte(items[i]);
		}
	}

	/**
	 * Removes and returns the last item.
	 * @return the last item, removed from this
	 */
	public byte pop () {
		if (size == 0) { throw new IndexOutOfBoundsException("ByteList is empty."); }
		return items[--size];
	}

	/**
	 * Returns the last item.
	 * @return the last item, without modifying this
	 */
	public byte peek () {
		if (size == 0) { throw new IndexOutOfBoundsException("ByteList is empty."); }
		return items[size - 1];
	}

	/**
	 * Returns the first item.
	 * @return the first item, without modifying this
	 */
	// Modified from libGDX
	public byte first () {
		if (size == 0) { throw new IndexOutOfBoundsException("ByteList is empty."); }
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
	 * Effectively removes all items from this ByteList.
	 * This is done simply by setting size to 0; because a {@code byte} item isn't a reference, it doesn't need to be set to null.
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
	public byte[] shrink () {
		if (items.length != size) { resize(size); }
		return items;
	}

	/**
	 * Increases the size of the backing array to accommodate the specified number of additional items. Useful before adding many
	 * items to avoid multiple backing array resizes.
	 *
	 * @return {@link #items}; this will be a different reference if this resized
	 */
	public byte[] ensureCapacity (int additionalCapacity) {
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
	public byte[] setSize (int newSize) {
		if (newSize < 0) { throw new IllegalArgumentException("newSize must be >= 0: " + newSize); }
		if (newSize > items.length) { resize(Math.max(8, newSize)); }
		size = newSize;
		return items;
	}

	protected byte[] resize (int newSize) {
		byte[] newItems = new byte[newSize];
		byte[] items = this.items;
		System.arraycopy(items, 0, newItems, 0, Math.min(size, newItems.length));
		this.items = newItems;
		return newItems;
	}

	public void sort () {
		Arrays.sort(items, 0, size);
	}

	@Override
	public void reverse () {
		byte[] items = this.items;
		for (int i = 0, lastIndex = size - 1, n = size / 2; i < n; i++) {
			int ii = lastIndex - i;
			byte temp = items[i];
			items[i] = items[ii];
			items[ii] = temp;
		}
	}

	// Modified from libGDX
	@Override
	public void shuffle (EnhancedRandom random) {
		byte[] items = this.items;
		for (int i = size - 1; i >= 0; i--) {
			int ii = random.nextInt(i + 1);
			byte temp = items[i];
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
	 * Returns a random item from the array, or zero if the array is empty.
	 * @param random a {@link EnhancedRandom} such as {@link com.github.tommyettinger.ds.support.LaserRandom} from this library
	 * @return a randomly selected item from this, or {@code 0} if this is empty
	 */
	public byte random (EnhancedRandom random) {
		if (size == 0) { return 0; }
		return items[random.nextInt(size)];
	}

	/**
	 * Allocates a new byte array with {@code size} elements and fills it with the items in this.
	 * @return a new byte array with the same contents as this
	 */
	public byte[] toArray () {
		byte[] array = new byte[size];
		System.arraycopy(items, 0, array, 0, size);
		return array;
	}

	/**
	 * If {@code array.length} at least equal to {@link #size()}, this copies the contents of this
	 * into {@code array} and returns it; otherwise, it allocates a new byte array that can fit all
	 * of the items in this, and proceeds to copy into that and return that.
	 * @param array a byte array that will be modified if it can fit {@link #size()} items
	 * @return {@code array}, if it had sufficient size, or a new array otherwise, either with a copy of this
	 */
	public byte[] toArray (byte[] array) {
		if(array.length < size)
			array = new byte[size];
		System.arraycopy(items, 0, array, 0, size);
		return array;
	}

	@Override
	public int hashCode () {
		byte[] items = this.items;
		int h = 1;
		if (ordered) {
			for (int i = 0, n = size; i < n; i++) { h = h * 31 + items[i]; }
		} else {
			for (int i = 0, n = size; i < n; i++) {
				h += items[i];
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
		if (!(object instanceof ByteList)) { return false; }
		ByteList array = (ByteList)object;
		if (!array.ordered) { return false; }
		int n = size;
		if (n != array.size) { return false; }
		byte[] items1 = this.items, items2 = array.items;
		for (int i = 0; i < n; i++) { if (items1[i] != items2[i]) { return false; } }
		return true;
	}

	@Override
	public String toString () {
		if (size == 0) { return "[]"; }
		byte[] items = this.items;
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
		byte[] items = this.items;
		StringBuilder buffer = new StringBuilder(32);
		buffer.append(items[0]);
		for (int i = 1; i < size; i++) {
			buffer.append(separator);
			buffer.append(items[i]);
		}
		return buffer.toString();
	}

	/**
	 * Returns a Java 8 primitive iterator over the int items in this ByteList. Iterates in order if {@link #ordered}
	 * is true, otherwise this is not guaranteed to iterate in the same order as items were added.
	 * <br>
	 * This will reuse one of two iterators in this ByteList; this does not allow nested iteration.
	 * Use {@link ByteListIterator#ByteListIterator(ByteList)} to nest iterators.
	 *
	 * @return a {@link ByteIterator}; use its nextByte() method instead of next()
	 */
	@Override
	public ByteListIterator iterator () {
		if (iterator1 == null || iterator2 == null) {
			iterator1 = new ByteListIterator(this);
			iterator2 = new ByteListIterator(this);
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

	public static class ByteListIterator implements ByteIterator {
		protected int index = 0;
		protected ByteList list;
		protected boolean valid = true;

		public ByteListIterator (ByteList list) {
			this.list = list;
		}

		/**
		 * Returns the next {@code int} element in the iteration.
		 *
		 * @return the next {@code int} element in the iteration
		 * @throws NoSuchElementException if the iteration has no more elements
		 */
		@Override
		public byte nextByte () {
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

	public static ByteList with(byte item) {
		ByteList list = new ByteList(1);
		list.add(item);
		return list;
	}

	/**
	 * @see #ByteList(byte[])
	 */
	public static ByteList with (byte... array) {
		return new ByteList(array);
	}

	/**
	 * Sorts all elements according to the order induced by the specified
	 * comparator using {@link ByteComparators#sort(byte[], int, int, ByteComparator)}.
	 * If {@code c} is null, this instead delegates to {@link #sort()},
	 * which uses {@link Arrays#sort(byte[])}, and does not always run in-place.
	 *
	 * <p>This sort is guaranteed to be <i>stable</i>: equal elements will not be reordered as a result
	 * of the sort. The sorting algorithm is an in-place mergesort that is significantly slower than a
	 * standard mergesort, as its running time is <i>O</i>(<var>n</var>&nbsp;(log&nbsp;<var>n</var>)<sup>2</sup>), but it does not allocate additional memory; as a result, it can be
	 * used as a generic sorting algorithm.
	 *
	 * @param c the comparator to determine the order of the ByteList
	 */
	public void sort (@Nullable final ByteComparator c) {
		if (c == null) {
			sort();
		} else {
			sort(0, size, c);
		}
	}

	/**
	 * Sorts the specified range of elements according to the order induced by the specified
	 * comparator using mergesort, or {@link Arrays#sort(byte[], int, int)} if {@code c} is null.
	 * This purely uses {@link ByteComparators#sort(byte[], int, int, ByteComparator)}, and you
	 * can see its docs for more information.
	 *
	 * @param from the index of the first element (inclusive) to be sorted.
	 * @param to   the index of the last element (exclusive) to be sorted.
	 * @param c    the comparator to determine the order of the ByteList
	 */
	public void sort (final int from, final int to, final ByteComparator c) {
		ByteComparators.sort(items, from, to, c);
	}
}

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

import com.github.tommyettinger.function.CharToCharFunction;
import com.github.tommyettinger.ds.support.sort.CharComparator;
import com.github.tommyettinger.ds.support.sort.CharComparators;
import com.github.tommyettinger.ds.support.util.CharIterator;

import org.checkerframework.checker.nullness.qual.Nullable;
import java.util.Arrays;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.Random;

/**
 * A resizable, ordered or unordered char list. Primitive-backed, so it avoids the boxing that occurs with an ArrayList of Character.
 * If unordered, this class avoids a memory copy when removing elements (the last element is moved to the removed element's position).
 * This tries to imitate most of the {@link java.util.List} interface, though it can't implement it without boxing its items.
 * Has a Java 8 {@link PrimitiveIterator} accessible via {@link #iterator()}.
 *
 * @author Nathan Sweet
 * @author Tommy Ettinger
 */
public class CharList implements PrimitiveCollection.OfChar, Ordered.OfChar, Arrangeable {

	public char[] items;
	protected int size;
	public boolean ordered;
	@Nullable protected transient CharListIterator iterator1;
	@Nullable protected transient CharListIterator iterator2;

	/**
	 * Creates an ordered array with a capacity of 10.
	 */
	public CharList () {
		this(true, 10);
	}

	/**
	 * Creates an ordered array with the specified capacity.
	 */
	public CharList (int capacity) {
		this(true, capacity);
	}

	/**
	 * @param ordered  If false, methods that remove elements may change the order of other elements in the array, which avoids a
	 *                 memory copy.
	 * @param capacity Any elements added beyond this will cause the backing array to be grown.
	 */
	public CharList (boolean ordered, int capacity) {
		this.ordered = ordered;
		items = new char[capacity];
	}

	/**
	 * Creates a new list containing the elements in the specific array. The new array will be ordered if the specific array is
	 * ordered. The capacity is set to the number of elements, so any subsequent elements added will cause the backing array to be
	 * grown.
	 */
	public CharList (CharList array) {
		this.ordered = array.ordered;
		size = array.size;
		items = new char[size];
		System.arraycopy(array.items, 0, items, 0, size);
	}

	/**
	 * Creates a new ordered array containing the elements in the specified array. The capacity is set to the number of elements,
	 * so any subsequent elements added will cause the backing array to be grown.
	 */
	public CharList (char[] array) {
		this(true, array, 0, array.length);
	}

	/**
	 * Creates a new list containing the elements in the specified array. The capacity is set to the number of elements, so any
	 * subsequent elements added will cause the backing array to be grown.
	 */
	public CharList (char[] array, int startIndex, int count) {
		this(true, array, startIndex, count);
	}

	/**
	 * Creates a new list containing the elements in the specified array. The capacity is set to the number of elements, so any
	 * subsequent elements added will cause the backing array to be grown.
	 *
	 * @param ordered If false, methods that remove elements may change the order of other elements in the array, which avoids a
	 *                memory copy.
	 */
	public CharList (boolean ordered, char[] array, int startIndex, int count) {
		this(ordered, count);
		size = count;
		System.arraycopy(array, startIndex, items, 0, count);
	}

	/**
	 * Creates a new list containing the items in the specified PrimitiveCollection.OfChar. Only this class currently implements
	 * that interface, but user code can as well.
	 *
	 * @param coll a primitive collection that will have its contents added to this
	 */
	public CharList (OfChar coll) {
		this(coll.size());
		addAll(coll);
	}

	/**
	 * Copies the given Ordered.OfChar into a new CharList.
	 *
	 * @param other another Ordered.OfChar
	 */
	public CharList (Ordered.OfChar other) {
		this(other.order());
	}

	/**
	 * Creates a new list by copying {@code count} items from the given Ordered, starting at {@code offset} in that Ordered,
	 * into this.
	 *
	 * @param other  another Ordered.OfChar
	 * @param offset the first index in other's ordering to draw an item from
	 * @param count  how many items to copy from other
	 */
	public CharList (Ordered.OfChar other, int offset, int count) {
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
	public boolean add (char value) {
		char[] items = this.items;
		if (size == items.length) {items = resize(Math.max(8, (int)(size * 1.75f)));}
		items[size++] = value;
		return true;
	}

	public void add (char value1, char value2) {
		char[] items = this.items;
		if (size + 1 >= items.length) {items = resize(Math.max(8, (int)(size * 1.75f)));}
		items[size] = value1;
		items[size + 1] = value2;
		size += 2;
	}

	public void add (char value1, char value2, char value3) {
		char[] items = this.items;
		if (size + 2 >= items.length) {items = resize(Math.max(8, (int)(size * 1.75f)));}
		items[size] = value1;
		items[size + 1] = value2;
		items[size + 2] = value3;
		size += 3;
	}

	public void add (char value1, char value2, char value3, char value4) {
		char[] items = this.items;
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
	public boolean addAll (CharList array) {
		return addAll(array.items, 0, array.size);
	}

	// Modified from libGDX
	public boolean addAll (CharList array, int offset, int length) {
		if (offset + length > array.size) {throw new IllegalArgumentException("offset + length must be <= size: " + offset + " + " + length + " <= " + array.size);}
		return addAll(array.items, offset, length);
	}

	/**
	 * Adds all items in the Ordered.OfChar {@code other} to this list, inserting at the end of the iteration order.
	 *
	 * @param other a non-null {@link Ordered.OfChar}
	 * @return true if this is modified by this call, as {@link #addAll(Ordered.OfChar)} does
	 */
	public boolean addAll (Ordered.OfChar other) {
		return addAll(size(), other, 0, other.size());
	}

	/**
	 * Adds up to {@code count} items, starting from {@code offset}, in the Ordered.OfChar {@code other} to this list,
	 * inserting at the end of the iteration order.
	 *
	 * @param other  a non-null {@link Ordered.OfChar}
	 * @param offset the first index in {@code other} to use
	 * @param count  how many indices in {@code other} to use
	 * @return true if this is modified by this call, as {@link #addAll(Ordered.OfChar)} does
	 */
	public boolean addAll (Ordered.OfChar other, int offset, int count) {
		return addAll(size(), other, offset, count);
	}

	/**
	 * Adds up to {@code count} items, starting from {@code offset}, in the Ordered.OfChar {@code other} to this list,
	 * inserting starting at {@code insertionIndex} in the iteration order.
	 *
	 * @param insertionIndex where to insert into the iteration order
	 * @param other          a non-null {@link Ordered.OfChar}
	 * @param offset         the first index in {@code other} to use
	 * @param count          how many indices in {@code other} to use
	 * @return true if this is modified by this call, as {@link #addAll(Ordered.OfChar)} does
	 */
	public boolean addAll (int insertionIndex, Ordered.OfChar other, int offset, int count) {
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
	public boolean addAll (char... array) {
		return addAll(array, 0, array.length);
	}

	// Modified from libGDX
	public boolean addAll (char[] array, int offset, int length) {
		char[] items = this.items;
		int sizeNeeded = size + length;
		if (sizeNeeded > items.length) {items = resize(Math.max(Math.max(8, sizeNeeded), (int)(size * 1.75f)));}
		System.arraycopy(array, offset, items, size, length);
		size += length;
		return true;
	}

	//Kotlin-friendly operator
	public char get (int index) {
		if (index >= size) {throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);}
		return items[index];
	}

	//Kotlin-friendly operator
	public void set (int index, char value) {
		if (index >= size) {throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);}
		items[index] = value;
	}

	public void insert (int index, char value) {
		if (index > size) {throw new IndexOutOfBoundsException("index can't be > size: " + index + " > " + size);}
		char[] items = this.items;
		if (size == items.length) {items = resize(Math.max(8, (int)(size * 1.75f)));}
		if (ordered) {System.arraycopy(items, index, items, index + 1, size - index);} else {items[size] = items[index];}
		size++;
		items[index] = value;
	}

	/**
	 * Inserts the specified number of items at the specified index. The new items will have values equal to the values at those
	 * indices before the insertion.
	 */
	public void insertRange (int index, int count) {
		if (index > size) {throw new IndexOutOfBoundsException("index can't be > size: " + index + " > " + size);}
		int sizeNeeded = size + count;
		if (sizeNeeded > items.length) {items = resize(Math.max(Math.max(8, sizeNeeded), (int)(size * 1.75f)));}
		System.arraycopy(items, index, items, index + count, size - index);
		size = sizeNeeded;
	}

	/**
	 * Returns this CharList, since it is its own order. This is only here to satisfy
	 * the {@link Ordered.OfChar} interface.
	 *
	 * @return this CharList
	 */
	@Override
	public CharList order () {
		return this;
	}

	@Override
	public void swap (int first, int second) {
		if (first >= size) {throw new IndexOutOfBoundsException("first can't be >= size: " + first + " >= " + size);}
		if (second >= size) {throw new IndexOutOfBoundsException("second can't be >= size: " + second + " >= " + size);}
		char[] items = this.items;
		char firstValue = items[first];
		items[first] = items[second];
		items[second] = firstValue;
	}

	@Override
	public boolean contains (char value) {
		int i = size - 1;
		char[] items = this.items;
		while (i >= 0) {if (items[i--] == value) {return true;}}
		return false;
	}

	/**
	 * Returns true if this CharList contains, at least once, every item in {@code other}; otherwise returns false.
	 *
	 * @param other an CharList
	 * @return true if this contains every item in {@code other}, otherwise false
	 */
	// Newly-added
	public boolean containsAll (CharList other) {
		char[] others = other.items;
		int otherSize = other.size;
		for (int i = 0; i < otherSize; i++) {
			if (!contains(others[i])) {return false;}
		}
		return true;
	}

	/**
	 * Returns the first index in this list that contains the specified value, or -1 if it is not present.
	 *
	 * @param value a char value to search for
	 * @return the first index of the given value, or -1 if it is not present
	 */
	public int indexOf (char value) {
		char[] items = this.items;
		for (int i = 0, n = size; i < n; i++) {if (items[i] == value) {return i;}}
		return -1;
	}

	/**
	 * Returns the last index in this list that contains the specified value, or -1 if it is not present.
	 *
	 * @param value a char value to search for
	 * @return the last index of the given value, or -1 if it is not present
	 */
	public int lastIndexOf (char value) {
		char[] items = this.items;
		for (int i = size - 1; i >= 0; i--) {if (items[i] == value) {return i;}}
		return -1;
	}

	/**
	 * Removes the first occurrence of {@code value} from this CharList, returning true if anything was removed.
	 * Otherwise, this returns false.
	 *
	 * @param value the value to (attempt to) remove
	 * @return true if a value was removed, false if the CharList is unchanged
	 */
	// Modified from libGDX
	@Override
	public boolean remove (char value) {
		char[] items = this.items;
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
	 * we also have {@link #remove(char)} that removes a value, rather than an index.
	 *
	 * @param index the index of the item to remove and return
	 * @return the removed item
	 */
	public char removeAt (int index) {
		if (index >= size) {throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);}
		char[] items = this.items;
		char value = items[index];
		size--;
		if (ordered) {System.arraycopy(items, index + 1, items, index, size - index);} else {items[index] = items[size];}
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
		if (end >= n) {throw new IndexOutOfBoundsException("end can't be >= size: " + end + " >= " + size);}
		if (start > end) {throw new IndexOutOfBoundsException("start can't be > end: " + start + " > " + end);}
		int count = end - start, lastIndex = n - count;
		if (ordered) {System.arraycopy(items, start + count, items, start, n - (start + count));} else {
			int i = Math.max(lastIndex, end);
			System.arraycopy(items, i, items, start, n - i);
		}
		size = n - count;
	}

	/**
	 * Removes from this CharList all occurrences of any elements contained in the specified collection.
	 *
	 * @param c a primitive collection of int items to remove fully, such as another CharList or a CharDeque
	 * @return true if this list was modified.
	 */
	public boolean removeAll (PrimitiveCollection.OfChar c) {
		int size = this.size;
		int startSize = size;
		char[] items = this.items;
		CharIterator it = c.iterator();
		for (int i = 0, n = c.size(); i < n; i++) {
			char item = it.nextChar();
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
	 * Removes from this CharList element-wise occurrences of elements contained in the specified collection.
	 * Note that if a value is present more than once in this CharList, only one of those occurrences
	 * will be removed for each occurrence of that value in {@code c}. If {@code c} has the same
	 * contents as this CharList or has additional items, then removing each of {@code c} will clear this.
	 *
	 * @param c a primitive collection of int items to remove one-by-one, such as another CharList or a CharDeque
	 * @return true if this list was modified.
	 */
	public boolean removeEach (PrimitiveCollection.OfChar c) {
		int size = this.size;
		int startSize = size;
		char[] items = this.items;
		CharIterator it = c.iterator();
		for (int i = 0, n = c.size(); i < n; i++) {
			char item = it.nextChar();
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
	 * Removes all items from this CharList that are not present somewhere in {@code other}, any number of times.
	 *
	 * @param other an CharList that contains the items that this should keep, whenever present
	 * @return true if this CharList changed as a result of this call, otherwise false
	 */
	// Newly-added
	public boolean retainAll (CharList other) {
		final int size = this.size;
		final char[] items = this.items;
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
	 * @param operator a CharToCharFunction (a functional interface defined in funderby)
	 */
	public void replaceAll (CharToCharFunction operator) {
		for (int i = 0, n = size; i < n; i++) {
			items[i] = operator.applyAsChar(items[i]);
		}
	}

	/**
	 * Removes and returns the last item.
	 *
	 * @return the last item, removed from this
	 */
	public char pop () {
		if (size == 0) {throw new IndexOutOfBoundsException("CharList is empty.");}
		return items[--size];
	}

	/**
	 * Returns the last item.
	 *
	 * @return the last item, without modifying this
	 */
	public char peek () {
		if (size == 0) {throw new IndexOutOfBoundsException("CharList is empty.");}
		return items[size - 1];
	}

	/**
	 * Returns the first item.
	 *
	 * @return the first item, without modifying this
	 */
	// Modified from libGDX
	public char first () {
		if (size == 0) {throw new IndexOutOfBoundsException("CharList is empty.");}
		return items[0];
	}

	/**
	 * Returns true if the array has one or more items, or false otherwise.
	 *
	 * @return true if the array has one or more items, or false otherwise
	 */
	public boolean notEmpty () {
		return size > 0;
	}

	/**
	 * Returns true if the array is empty.
	 *
	 * @return true if the array is empty, or false if it has any items
	 */
	@Override
	public boolean isEmpty () {
		return size == 0;
	}

	/**
	 * Effectively removes all items from this CharList.
	 * This is done simply by setting size to 0; because a {@code char} item isn't a reference, it doesn't need to be set to null.
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
	public char[] shrink () {
		if (items.length != size) {resize(size);}
		return items;
	}

	/**
	 * Increases the size of the backing array to accommodate the specified number of additional items. Useful before adding many
	 * items to avoid multiple backing array resizes.
	 *
	 * @return {@link #items}; this will be a different reference if this resized
	 */
	public char[] ensureCapacity (int additionalCapacity) {
		if (additionalCapacity < 0) {throw new IllegalArgumentException("additionalCapacity must be >= 0: " + additionalCapacity);}
		int sizeNeeded = size + additionalCapacity;
		if (sizeNeeded > items.length) {resize(Math.max(Math.max(8, sizeNeeded), (int)(size * 1.75f)));}
		return items;
	}

	/**
	 * Sets the array size, leaving any values beyond the current size undefined.
	 *
	 * @return {@link #items}; this will be a different reference if this resized to a larger capacity
	 */
	public char[] setSize (int newSize) {
		if (newSize < 0) {throw new IllegalArgumentException("newSize must be >= 0: " + newSize);}
		if (newSize > items.length) {resize(Math.max(8, newSize));}
		size = newSize;
		return items;
	}

	protected char[] resize (int newSize) {
		char[] newItems = new char[newSize];
		char[] items = this.items;
		System.arraycopy(items, 0, newItems, 0, Math.min(size, newItems.length));
		this.items = newItems;
		return newItems;
	}

	public void sort () {
		Arrays.sort(items, 0, size);
	}

	@Override
	public void reverse () {
		char[] items = this.items;
		for (int i = 0, lastIndex = size - 1, n = size / 2; i < n; i++) {
			int ii = lastIndex - i;
			char temp = items[i];
			items[i] = items[ii];
			items[ii] = temp;
		}
	}

	// Modified from libGDX
	@Override
	public void shuffle (Random random) {
		char[] items = this.items;
		for (int i = size - 1; i > 0; i--) {
			int ii = random.nextInt(i + 1);
			char temp = items[i];
			items[i] = items[ii];
			items[ii] = temp;
		}
	}

	/**
	 * Reduces the size of the array to the specified size. If the array is already smaller than the specified size, no action is
	 * taken.
	 */
	public void truncate (int newSize) {
		if (size > newSize) {size = newSize;}
	}

	/**
	 * Returns a random item from the array, or zero if the array is empty.
	 *
	 * @param random a {@link Random} or a subclass, such as any from juniper
	 * @return a randomly selected item from this, or {@code 0} if this is empty
	 */
	public char random (Random random) {
		if (size == 0) {return 0;}
		return items[random.nextInt(size)];
	}

	/**
	 * Allocates a new char array with {@code size} elements and fills it with the items in this.
	 *
	 * @return a new char array with the same contents as this
	 */
	public char[] toArray () {
		char[] array = new char[size];
		System.arraycopy(items, 0, array, 0, size);
		return array;
	}

	/**
	 * If {@code array.length} at least equal to {@link #size()}, this copies the contents of this
	 * into {@code array} and returns it; otherwise, it allocates a new char array that can fit all
	 * of the items in this, and proceeds to copy into that and return that.
	 *
	 * @param array a char array that will be modified if it can fit {@link #size()} items
	 * @return {@code array}, if it had sufficient size, or a new array otherwise, either with a copy of this
	 */
	public char[] toArray (char[] array) {
		if (array.length < size)
			array = new char[size];
		System.arraycopy(items, 0, array, 0, size);
		return array;
	}

	@Override
	public int hashCode () {
		char[] items = this.items;
		int h = 1;
		if (ordered) {
			for (int i = 0, n = size; i < n; i++) {h = h * 31 + items[i];}
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
		if (object == this) {return true;}
		if (!ordered) {return false;}
		if (!(object instanceof CharList)) {return false;}
		CharList array = (CharList)object;
		if (!array.ordered) {return false;}
		int n = size;
		if (n != array.size) {return false;}
		char[] items1 = this.items, items2 = array.items;
		for (int i = 0; i < n; i++) {if (items1[i] != items2[i]) {return false;}}
		return true;
	}

	@Override
	public String toString () {
		if (size == 0) {return "[]";}
		char[] items = this.items;
		StringBuilder buffer = new StringBuilder(32);
		buffer.append("['");
		buffer.append(items[0]);
		for (int i = 1; i < size; i++) {
			buffer.append("', '");
			buffer.append(items[i]);
		}
		buffer.append("']");
		return buffer.toString();
	}

	public String toString (String separator) {
		if (size == 0) {return "";}
		char[] items = this.items;
		StringBuilder buffer = new StringBuilder(32);
		buffer.append(items[0]);
		for (int i = 1; i < size; i++) {
			buffer.append(separator);
			buffer.append(items[i]);
		}
		return buffer.toString();
	}

	/**
	 * Simply returns all of the char items in this as one String, with no delimiters.
	 * This is the same as calling {@code String.valueOf(charList.items, 0, charList.size())} .
	 *
	 * @return a String containing only the char items in this CharList
	 */
	public String toDenseString () {
		return String.valueOf(items, 0, size);
	}

	/**
	 * Returns a Java 8 primitive iterator over the int items in this CharList. Iterates in order if {@link #ordered}
	 * is true, otherwise this is not guaranteed to iterate in the same order as items were added.
	 * <br>
	 * This will reuse one of two iterators in this CharList; this does not allow nested iteration.
	 * Use {@link CharListIterator#CharListIterator(CharList)} to nest iterators.
	 *
	 * @return a {@link CharIterator}; use its nextChar() method instead of next()
	 */
	@Override
	public CharListIterator iterator () {
		if (iterator1 == null || iterator2 == null) {
			iterator1 = new CharListIterator(this);
			iterator2 = new CharListIterator(this);
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
	 * A {@link CharIterator}, plus {@link ListIterator} methods, over the elements of a CharList.
	 * Use {@link #nextChar()} in preference to {@link #next()} to avoid allocating Character objects.
	 */
	public static class CharListIterator implements CharIterator {
		protected int index, latest = -1;
		protected CharList list;
		protected boolean valid = true;

		public CharListIterator (CharList list) {
			this.list = list;
		}

		public CharListIterator (CharList list, int index) {
			if (index < 0 || index >= list.size())
				throw new IndexOutOfBoundsException("CharListIterator does not satisfy index >= 0 && index < list.size()");
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
		public char nextChar () {
			if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
			if (index >= list.size()) {throw new NoSuchElementException();}
			return list.get(latest = index++);
		}

		/**
		 * Returns {@code true} if the iteration has more elements.
		 * (In other words, returns {@code true} if {@link #nextChar} would
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
		 * returns {@code true} if {@link #previousChar} would return an element
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
		 * {@link #nextChar} to go back and forth.  (Note that alternating calls
		 * to {@code next} and {@code previous} will return the same
		 * element repeatedly.)
		 *
		 * @return the previous element in the list
		 * @throws NoSuchElementException if the iteration has no previous
		 *                                element
		 */
		public char previousChar () {
			if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
			if (index <= 0 || list.isEmpty()) {throw new NoSuchElementException();}
			return list.get(latest = --index);
		}

		/**
		 * Returns the index of the element that would be returned by a
		 * subsequent call to {@link #nextChar}. (Returns list size if the list
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
		 * subsequent call to {@link #previousChar}. (Returns -1 if the list
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
		 * #nextChar} or {@link #previousChar} (optional operation).  This call can
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
		 * Replaces the last element returned by {@link #nextChar} or
		 * {@link #previousChar} with the specified element (optional operation).
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
		public void set (char t) {
			if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
			if (latest == -1 || latest >= list.size()) {throw new NoSuchElementException();}
			list.set(latest, t);
		}

		/**
		 * Inserts the specified element into the list (optional operation).
		 * The element is inserted immediately before the element that
		 * would be returned by {@link #nextChar}, if any, and after the element
		 * that would be returned by {@link #previousChar}, if any.  (If the
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
		public void add (char t) {
			if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
			if (index > list.size()) {throw new NoSuchElementException();}
			list.insert(index++, t);
			latest = -1;
		}

		public void reset () {
			index = 0;
			latest = -1;
		}

		public void reset (int index) {
			if (index < 0 || index >= list.size())
				throw new IndexOutOfBoundsException("CharListIterator does not satisfy index >= 0 && index < list.size()");
			this.index = index;
			latest = -1;
		}

		/**
		 * Returns an iterator over elements of type {@code char}.
		 *
		 * @return this same CharListIterator.
		 */
		public CharList.CharListIterator iterator () {
			return this;
		}
	}

	public static CharList with (char item) {
		CharList list = new CharList(1);
		list.add(item);
		return list;
	}

	/**
	 * @see #CharList(char[])
	 */
	public static CharList with (char... array) {
		return new CharList(array);
	}

	/**
	 * Sorts all elements according to the order induced by the specified
	 * comparator using {@link CharComparators#sort(char[], int, int, CharComparator)}.
	 * If {@code c} is null, this instead delegates to {@link #sort()},
	 * which uses {@link Arrays#sort(char[])}, and does not always run in-place.
	 *
	 * <p>This sort is guaranteed to be <i>stable</i>: equal elements will not be reordered as a result
	 * of the sort. The sorting algorithm is an in-place mergesort that is significantly slower than a
	 * standard mergesort, as its running time is <i>O</i>(<var>n</var>&nbsp;(log&nbsp;<var>n</var>)<sup>2</sup>), but it does not allocate additional memory; as a result, it can be
	 * used as a generic sorting algorithm.
	 *
	 * @param c the comparator to determine the order of the CharList
	 */
	public void sort (@Nullable final CharComparator c) {
		if (c == null) {
			sort();
		} else {
			sort(0, size, c);
		}
	}

	/**
	 * Sorts the specified range of elements according to the order induced by the specified
	 * comparator using mergesort, or {@link Arrays#sort(char[], int, int)} if {@code c} is null.
	 * This purely uses {@link CharComparators#sort(char[], int, int, CharComparator)}, and you
	 * can see its docs for more information.
	 *
	 * @param from the index of the first element (inclusive) to be sorted.
	 * @param to   the index of the last element (exclusive) to be sorted.
	 * @param c    the comparator to determine the order of the CharList
	 */
	public void sort (final int from, final int to, final CharComparator c) {
		CharComparators.sort(items, from, to, c);
	}

}

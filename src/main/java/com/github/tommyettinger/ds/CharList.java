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

import com.github.tommyettinger.ds.support.sort.CharComparator;
import com.github.tommyettinger.ds.support.sort.CharComparators;
import com.github.tommyettinger.ds.support.util.CharIterator;
import com.github.tommyettinger.function.CharToCharFunction;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Arrays;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Random;

/**
 * A resizable, insertion-ordered char list. Primitive-backed, so it avoids the boxing that occurs with an ArrayList of Character.
 * This tries to imitate most of the {@link java.util.List} interface, though it can't implement it without boxing its items.
 * Has a primitive iterator accessible via {@link #iterator()}.
 *
 * @author Nathan Sweet
 * @author Tommy Ettinger
 * @see CharBag CharBag is an unordered variant on CharList.
 */
public class CharList implements PrimitiveCollection.OfChar, Ordered.OfChar, Arrangeable, CharSequence, Appendable,
	Comparable<CharList> {
	/**
	 * Returns true if this implementation retains order, which it does.
	 *
	 * @return true
	 */
	public boolean keepsOrder() {
		return true;
	}

	public char[] items;
	protected int size;
	@Nullable
	protected transient CharListIterator iterator1;
	@Nullable
	protected transient CharListIterator iterator2;

	/**
	 * Creates an ordered list with a capacity of 10.
	 */
	public CharList() {
		this(10);
	}

	/**
	 * Creates an ordered list with the specified capacity.
	 *
	 * @param capacity Any elements added beyond this will cause the backing array to be grown.
	 */
	public CharList(int capacity) {
		items = new char[capacity];
	}

	/**
	 * Creates an ordered list with the specified capacity.
	 *
	 * @param ordered  ignored; for an unordered list use {@link CharBag}
	 * @param capacity Any elements added beyond this will cause the backing array to be grown.
	 * @deprecated CharList is always ordered; for an unordered list use {@link CharBag}
	 */
	@Deprecated
	public CharList(boolean ordered, int capacity) {
		this(capacity);
	}

	/**
	 * Creates a new list containing the elements in the given list. The new list will be ordered. The capacity is set
	 * to the number of elements, so any subsequent elements added will cause the backing array to be grown.
	 *
	 * @param list another CharList (or CharBag) to copy from
	 */
	public CharList(CharList list) {
		size = list.size;
		items = new char[size];
		System.arraycopy(list.items, 0, items, 0, size);
	}

	/**
	 * Creates a new list containing the elements in the specified array. The capacity is set to the number of elements,
	 * so any subsequent elements added will cause the backing array to be grown.
	 *
	 * @param array a char array to copy from
	 */
	public CharList(char[] array) {
		this(array, 0, array.length);
	}

	/**
	 * Creates a new list containing the elements in the specified array. The capacity is set to the number of elements, so any
	 * subsequent elements added will cause the backing array to be grown.
	 *
	 * @param array      a non-null char array to add to this list
	 * @param startIndex the first index in {@code array} to use
	 * @param count      how many items to use from {@code array}
	 */
	public CharList(char[] array, int startIndex, int count) {
		this(count);
		size = count;
		System.arraycopy(array, startIndex, items, 0, count);
	}

	/**
	 * Creates a new list containing the elements in the specified array. The capacity is set to the number of elements, so any
	 * subsequent elements added will cause the backing array to be grown.
	 *
	 * @param ordered    ignored; for an unordered list use {@link CharBag}
	 * @param array      a non-null char array to add to this list
	 * @param startIndex the first index in {@code array} to use
	 * @param count      how many items to use from {@code array}
	 * @deprecated CharList is always ordered; for an unordered list use {@link CharBag}
	 */
	@Deprecated
	public CharList(boolean ordered, char[] array, int startIndex, int count) {
		this(array, startIndex, count);
	}

	/**
	 * Creates a new list containing the items in the specified PrimitiveCollection.OfChar.
	 *
	 * @param coll a primitive collection that will have its contents added to this
	 */
	public CharList(OfChar coll) {
		this(coll.size());
		addAll(coll);
	}

	/**
	 * Creates a new instance containing the items in the specified iterator.
	 *
	 * @param coll an iterator that will have its remaining contents added to this
	 */
	public CharList(CharIterator coll) {
		this();
		addAll(coll);
	}

	/**
	 * Copies the given Ordered.OfChar into a new CharList.
	 *
	 * @param other another Ordered.OfChar that will have its contents copied into this
	 */
	public CharList(Ordered.OfChar other) {
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
	public CharList(Ordered.OfChar other, int offset, int count) {
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
	public boolean add(char value) {
		char[] items = this.items;
		if (size == items.length) {
			items = resize(Math.max(8, (int) (size * 1.75f)));
		}
		items[size++] = value;
		return true;
	}

	public void add(char value1, char value2) {
		char[] items = this.items;
		if (size + 1 >= items.length) {
			items = resize(Math.max(8, (int) (size * 1.75f)));
		}
		items[size] = value1;
		items[size + 1] = value2;
		size += 2;
	}

	public void add(char value1, char value2, char value3) {
		char[] items = this.items;
		if (size + 2 >= items.length) {
			items = resize(Math.max(8, (int) (size * 1.75f)));
		}
		items[size] = value1;
		items[size + 1] = value2;
		items[size + 2] = value3;
		size += 3;
	}

	public void add(char value1, char value2, char value3, char value4) {
		char[] items = this.items;
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
	public boolean addAll(CharList list) {
		return addAll(list.items, 0, list.size);
	}

	// Modified from libGDX
	public boolean addAll(CharList list, int offset, int count) {
		if (offset + count > list.size) {
			throw new IllegalArgumentException("offset + count must be <= list.size: " + offset + " + " + count + " <= " + list.size);
		}
		return addAll(list.items, offset, count);
	}

	/**
	 * Adds up to {@code count} items, starting from {@code offset}, in the Ordered.OfChar {@code other} to this list,
	 * inserting at the end of the iteration order.
	 *
	 * @param other  a non-null {@link Ordered.OfChar}
	 * @param offset the first index in {@code other} to use
	 * @param count  how many indices in {@code other} to use
	 * @return true if this is modified by this call, as {@link #addAll(CharList)} does
	 */
	public boolean addAll(Ordered.OfChar other, int offset, int count) {
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
	 * @return true if this is modified by this call, as {@link #addAll(CharList)} does
	 */
	public boolean addAll(int insertionIndex, Ordered.OfChar other, int offset, int count) {
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
	public boolean addAll(char... array) {
		return addAll(array, 0, array.length);
	}

	// Modified from libGDX
	public boolean addAll(char[] array, int offset, int length) {
		char[] items = this.items;
		int sizeNeeded = size + length;
		if (sizeNeeded > items.length) {
			items = resize(Math.max(Math.max(8, sizeNeeded), (int) (size * 1.75f)));
		}
		System.arraycopy(array, offset, items, size, length);
		size += length;
		return true;
	}

	//Kotlin-friendly operator
	public char get(int index) {
		if (index >= size) {
			throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);
		}
		return items[index];
	}

	//Kotlin-friendly operator
	public void set(int index, char value) {
		if (index >= size) {
			throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);
		}
		items[index] = value;
	}

	public void insert(int index, char value) {
		if (index > size) {
			throw new IndexOutOfBoundsException("index can't be > size: " + index + " > " + size);
		}
		char[] items = this.items;
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
	 * Returns this CharList, since it is its own order. This is only here to satisfy
	 * the {@link Ordered.OfChar} interface.
	 *
	 * @return this CharList
	 */
	@Override
	public CharList order() {
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
		char[] items = this.items;
		char firstValue = items[first];
		items[first] = items[second];
		items[second] = firstValue;
	}

	@Override
	public boolean contains(char value) {
		int i = size - 1;
		char[] items = this.items;
		while (i >= 0) {
			if (items[i--] == value) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Simply calls {@link #indexOf(CharSequence)} and checks that it doesn't return {@code -1}.
	 * @param csq a CharSequence, such as a String or another CharList
	 * @return true if this CharList contains the chars in {@code csq} consecutively and in order
	 */
	public boolean contains(CharSequence csq) {
		return indexOf(csq) != -1;
	}

	/**
	 * Simply calls {@link #indexOfIgnoreCase(CharSequence)} and checks that it doesn't return {@code -1}.
	 * @param csq a CharSequence, such as a String or another CharList
	 * @return true if this CharList contains the chars in {@code csq} consecutively and in order, ignoring case
	 */
	public boolean containsIgnoreCase(CharSequence csq) {
		return indexOfIgnoreCase(csq) != -1;
	}

	/**
	 * Returns true if this CharList contains, at least once, every item in {@code other}; otherwise returns false.
	 *
	 * @param other an CharList
	 * @return true if this contains every item in {@code other}, otherwise false
	 */
	// Newly-added
	public boolean containsAll(CharList other) {
		char[] others = other.items;
		int otherSize = other.size;
		for (int i = 0; i < otherSize; i++) {
			if (!contains(others[i])) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns the first index in this list that contains {@code search}, or -1 if it is not present.
	 *
	 * @param search a char to search for
	 * @return the first index of the given char, or -1 if it is not present
	 */
	public int indexOf(char search) {
		return indexOf(search, 0);
	}

	/**
	 * Tries to return the first index {@code search} appears at in this list, starting at {@code fromIndex};
	 * if {@code search} is not present, this returns -1.
	 *
	 * @param search the char to search for
	 * @param fromIndex the initial index in this list to start searching (inclusive)
	 * @return the index {@code search} was found at, or -1 if it was not found
	 */
	public int indexOf (char search, int fromIndex) {
		if (fromIndex < 0 || fromIndex >= size) return -1;
		char[] items = this.items;
		for (int i = fromIndex, n = size; i < n; i++)
			if (items[i] == search) return i;
		return -1;
	}

	/**
	 * Returns the first index in this list that contains {@code search}, or -1 if it is not present.
	 * This compares the given char as if both it and
	 * this CharSequence have had every character converted to upper case by {@link Casing#caseUp(char)}.
	 *
	 * @param search a char to search for
	 * @return the first index of the given char, or -1 if it is not present
	 */
	public int indexOfIgnoreCase(char search) {
		return indexOfIgnoreCase(search, 0);
	}

	/**
	 * Tries to return the first index {@code search} appears at in this list, starting at {@code fromIndex};
	 * if {@code search} is not present, this returns -1. This compares the given char as if both it and
	 * this CharSequence have had every character converted to upper case by {@link Casing#caseUp(char)}.
	 *
	 * @param search the char to search for
	 * @param fromIndex the initial index in this list to start searching (inclusive)
	 * @return the index {@code search} was found at, or -1 if it was not found
	 */
	public int indexOfIgnoreCase (char search, int fromIndex) {
		if (fromIndex < 0 || fromIndex >= size) return -1;
		char[] items = this.items;
		final char upperSearch = Casing.caseUp(search);
		for (int i = fromIndex, n = size; i < n; i++)
			if (Casing.caseUp(items[i]) == upperSearch) return i;
		return -1;
	}

	/**
	 * Tries to return the first index {@code search} appears at in this list, starting at index 0;
	 * if {@code search} is not present, this returns -1.
	 *
	 * @param search the CharSequence (such as a String or another CharList) to search for
	 * @return the index {@code search} was found at, or -1 if it was not found
	 */
	public int indexOf (CharSequence search) {
		return indexOf(search, 0);
	}

	/**
	 * Tries to return the first index {@code search} appears at in this list, starting at {@code fromIndex};
	 * if {@code search} is not present, this returns -1.
	 * <br>
	 * Mostly copied from libGDX, like the rest of this class, but from the latest version instead of a
	 * much-older version.
	 *
	 * @param search the CharSequence (such as a String or another CharList) to search for
	 * @param fromIndex the initial index in this list to start searching (inclusive)
	 * @return the index {@code search} was found at, or -1 if it was not found
	 */
	public int indexOf (CharSequence search, int fromIndex) {
		if (search == null) throw new IllegalArgumentException("search cannot be null.");
		if (fromIndex < 0 || fromIndex >= size) return -1;
		int searchLen = search.length();
		if (searchLen == 1) return indexOf(search.charAt(0), fromIndex);
		if (searchLen == 0) return fromIndex;
		if (searchLen > size) return -1;
		char[] items = this.items;
		int searchableSize = size - searchLen + 1;
		for (int i = fromIndex; i < searchableSize; i++) {
			boolean found = true;
			for (int j = 0; j < searchLen && found; j++)
				found = search.charAt(j) == items[i + j];
			if (found) return i;
		}
		return -1;
	}

	/**
	 * Tries to return the first index {@code search} appears at in this list, starting at index 0;
	 * if {@code search} is not present, this returns -1. This compares the given CharSequence as if both it and
	 * this CharSequence have had every character converted to upper case by {@link Casing#caseUp(char)}.
	 *
	 * @param search the CharSequence (such as a String or another CharList) to search for
	 * @return the index {@code search} was found at, or -1 if it was not found
	 */
	public int indexOfIgnoreCase (CharSequence search) {
		return indexOfIgnoreCase(search, 0);
	}

	/**
	 * Tries to return the first index {@code search} appears at in this list, starting at {@code fromIndex};
	 * if {@code search} is not present, this returns -1. This compares the given CharSequence as if both it and
	 * this CharSequence have had every character converted to upper case by {@link Casing#caseUp(char)}.
	 * <br>
	 * Mostly copied from libGDX, like the rest of this class, but from the latest version instead of a
	 * much-older version.
	 *
	 * @param search the CharSequence (such as a String or another CharList) to search for
	 * @param fromIndex the initial index in this list to start searching (inclusive)
	 * @return the index {@code search} was found at, or -1 if it was not found
	 */
	public int indexOfIgnoreCase (CharSequence search, int fromIndex) {
		if (search == null) throw new IllegalArgumentException("search cannot be null.");
		if (fromIndex < 0 || fromIndex >= size) return -1;
		int searchLen = search.length();
		if (searchLen == 1) return indexOfIgnoreCase(search.charAt(0), fromIndex);
		if (searchLen == 0) return fromIndex;
		if (searchLen > size) return -1;
		char[] items = this.items;
		int searchableSize = size - searchLen + 1;
		for (int i = fromIndex; i < searchableSize; i++) {
			boolean found = true;
			for (int j = 0; j < searchLen && found; j++)
				found = Casing.caseUp(search.charAt(j)) == Casing.caseUp(items[i + j]);
			if (found) return i;
		}
		return -1;
	}

	/**
	 * Returns the last index in this list that contains {@code search}, or -1 if it is not present.
	 *
	 * @param search a char to search for
	 * @return the last index of the given value, or -1 if it is not present
	 */
	public int lastIndexOf(char search) {
		return lastIndexOf(search, size - 1);
	}
	/**
	 * Returns the last index in this list that contains {@code search}, starting the search at
	 * {@code fromIndex} (inclusive) and moving toward the start, or -1 if it is not present.
	 *
	 * @param search a char to search for
	 * @param fromIndex the initial index to check (zero-indexed, starts at 0, inclusive)
	 * @return the last index of the given value, or -1 if it is not present
	 */
	public int lastIndexOf(char search, int fromIndex) {
		char[] items = this.items;
		for (int i = fromIndex; i >= 0; i--) {
			if (items[i] == search) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Tries to return the first index {@code search} appears at in this list, starting the search at the end
	 * and working toward the start; if {@code search} is not present, this returns -1.
	 *
	 * @param search the CharSequence (such as a String or another CharList) to search for
	 * @return the index {@code search} was found at, or -1 if it was not found
	 */
	public int lastIndexOf (CharSequence search) {
		return lastIndexOf(search, size - 1);
	}

	/**
	 * Tries to return the first index {@code search} appears at in this list, starting the search at {@code fromIndex}
	 * and working toward the start; if {@code search} is not present, this returns -1.
	 *
	 * @param search the CharSequence (such as a String or another CharList) to search for
	 * @param fromIndex the initial index in this list to start searching (zero-indexed, starts at 0, inclusive)
	 * @return the index {@code search} was found at, or -1 if it was not found
	 */
	public int lastIndexOf (CharSequence search, int fromIndex) {
		if (search == null) throw new IllegalArgumentException("search cannot be null.");
		if (fromIndex < 0 || fromIndex >= size) return -1;
		int searchLen = search.length();
		if (searchLen == 1) return lastIndexOf(search.charAt(0), fromIndex);
		if (searchLen == 0) return fromIndex;
		if (searchLen > fromIndex) return -1;
		char[] items = this.items;
		int searchableSize = fromIndex - searchLen + 1;
		for (int i = searchableSize; i >= 0; i--) {
			boolean found = true;
			for (int j = 0; j < searchLen && found; j++)
				found = search.charAt(j) == items[i + j];
			if (found) return i;
		}
		return -1;
	}

	/**
	 * Returns the last index in this list that contains {@code search}, or -1 if it is not present.
	 * This compares the given char as if both it and this CharSequence have had every character converted
	 * to upper case by {@link Casing#caseUp(char)}.
	 *
	 * @param search a char to search for
	 * @return the last index of the given value, or -1 if it is not present
	 */
	public int lastIndexOfIgnoreCase(char search) {
		return lastIndexOfIgnoreCase(search, size - 1);
	}
	/**
	 * Returns the last index in this list that contains {@code search}, starting the search at
	 * {@code fromIndex} (inclusive) and moving toward the start, or -1 if it is not present.
	 * This compares the given char as if both it and this CharSequence have had every character converted
	 * to upper case by {@link Casing#caseUp(char)}.
	 *
	 * @param search a char to search for
	 * @param fromIndex the initial index to check (zero-indexed, starts at 0, inclusive)
	 * @return the last index of the given value, or -1 if it is not present
	 */
	public int lastIndexOfIgnoreCase(char search, int fromIndex) {
		char[] items = this.items;
		final char upperSearch = Casing.caseUp(search);
		for (int i = fromIndex; i >= 0; i--) {
			if (Casing.caseUp(items[i]) == upperSearch) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Tries to return the first index {@code search} appears at in this list, starting the search at the end
	 * and working toward the start; if {@code search} is not present, this returns -1.
	 * This compares the given CharSequence as if both it and this CharSequence have had every character converted
	 * to upper case by {@link Casing#caseUp(char)}.
	 *
	 * @param search the CharSequence (such as a String or another CharList) to search for
	 * @return the index {@code search} was found at, or -1 if it was not found
	 */
	public int lastIndexOfIgnoreCase (CharSequence search) {
		return lastIndexOfIgnoreCase(search, size - 1);
	}

	/**
	 * Tries to return the first index {@code search} appears at in this list, starting the search at {@code fromIndex}
	 * and working toward the start; if {@code search} is not present, this returns -1.
	 * This compares the given CharSequence as if both it and this CharSequence have had every character converted
	 * to upper case by {@link Casing#caseUp(char)}.
	 *
	 * @param search the CharSequence (such as a String or another CharList) to search for
	 * @param fromIndex the initial index in this list to start searching (zero-indexed, starts at 0, inclusive)
	 * @return the index {@code search} was found at, or -1 if it was not found
	 */
	public int lastIndexOfIgnoreCase (CharSequence search, int fromIndex) {
		if (search == null) throw new IllegalArgumentException("search cannot be null.");
		if (fromIndex < 0 || fromIndex >= size) return -1;
		int searchLen = search.length();
		if (searchLen == 1) return lastIndexOfIgnoreCase(search.charAt(0), fromIndex);
		if (searchLen == 0) return fromIndex;
		if (searchLen > fromIndex) return -1;
		char[] items = this.items;
		int searchableSize = fromIndex - searchLen + 1;
		for (int i = searchableSize; i >= 0; i--) {
			boolean found = true;
			for (int j = 0; j < searchLen && found; j++)
				found = Casing.caseUp(search.charAt(j)) == Casing.caseUp(items[i + j]);
			if (found) return i;
		}
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
	public boolean remove(char value) {
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
	public char removeAt(int index) {
		if (index >= size) {
			throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);
		}
		char[] items = this.items;
		char value = items[index];
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
	 * Removes from this CharList all occurrences of any elements contained in the specified collection.
	 *
	 * @param c a primitive collection of int items to remove fully, such as another CharList or a CharDeque
	 * @return true if this list was modified.
	 */
	public boolean removeAll(OfChar c) {
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
	public boolean removeEach(OfChar c) {
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
	 * @param other a PrimitiveCollection.OfChar that contains the items that this should keep, whenever present
	 * @return true if this CharList changed as a result of this call, otherwise false
	 */
	// Newly-added
	public boolean retainAll(OfChar other) {
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
	public void replaceAll(CharToCharFunction operator) {
		for (int i = 0, n = size; i < n; i++) {
			items[i] = operator.applyAsChar(items[i]);
		}
	}

	/**
	 * Removes and returns the last item.
	 *
	 * @return the last item, removed from this
	 */
	public char pop() {
		if (size == 0) {
			throw new IndexOutOfBoundsException("CharList is empty.");
		}
		return items[--size];
	}

	/**
	 * Returns the last item.
	 *
	 * @return the last item, without modifying this
	 */
	public char peek() {
		if (size == 0) {
			throw new IndexOutOfBoundsException("CharList is empty.");
		}
		return items[size - 1];
	}

	/**
	 * Returns the first item.
	 *
	 * @return the first item, without modifying this
	 */
	// Modified from libGDX
	public char first() {
		if (size == 0) {
			throw new IndexOutOfBoundsException("CharList is empty.");
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

	@Override
	public int length() {
		return size();
	}

	@Override
	public char charAt(int index) {
		return get(index);
	}

	/**
	 * Returns true if the list is empty.
	 *
	 * @return true if the list is empty, or false if it has any items
	 */
	@SuppressWarnings("Since15")
	public boolean isEmpty() {
		return size == 0;
	}

	/**
	 * Creates a new CharList by copying the given subrange of this CharList.
	 *
	 * @param start the start index, inclusive
	 * @param end   the end index, exclusive
	 * @return a new CharList copying the given subrange of this CharList
	 */
	@Override
	public CharList subSequence(int start, int end) {
		return new CharList(items, start, end - start);
	}

	/**
	 * Effectively removes all items from this CharList.
	 * This is done simply by setting size to 0; because a {@code char} item isn't a reference, it doesn't need to be set to null.
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
	public char[] shrink() {
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
	public char[] ensureCapacity(int additionalCapacity) {
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
	public char[] setSize(int newSize) {
		if (newSize < 0) {
			throw new IllegalArgumentException("newSize must be >= 0: " + newSize);
		}
		if (newSize > items.length) {
			resize(Math.max(8, newSize));
		}
		size = newSize;
		return items;
	}

	protected char[] resize(int newSize) {
		char[] newItems = new char[newSize];
		char[] items = this.items;
		System.arraycopy(items, 0, newItems, 0, Math.min(size, newItems.length));
		this.items = newItems;
		return newItems;
	}

	public void sort() {
		Arrays.sort(items, 0, size);
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
	public void sort(@Nullable final CharComparator c) {
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
	public void sort(final int from, final int to, final CharComparator c) {
		CharComparators.sort(items, from, to, c);
	}

	@Override
	public void reverse() {
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
	public void shuffle(Random random) {
		char[] items = this.items;
		for (int i = size - 1; i > 0; i--) {
			int ii = random.nextInt(i + 1);
			char temp = items[i];
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
	 * Returns a random item from the list, or zero if the list is empty.
	 *
	 * @param random a {@link Random} or a subclass, such as any from juniper
	 * @return a randomly selected item from this, or {@code 0} if this is empty
	 */
	public char random(Random random) {
		if (size == 0) {
			return 0;
		}
		return items[random.nextInt(size)];
	}

	/**
	 * Allocates a new char array with {@code size} elements and fills it with the items in this.
	 *
	 * @return a new char array with the same contents as this
	 */
	public char[] toArray() {
		char[] array = new char[size];
		System.arraycopy(items, 0, array, 0, size);
		return array;
	}

	/**
	 * If {@code array.length} at least equal to {@link #size()}, this copies the contents of this
	 * into {@code array} and returns it; otherwise, it allocates a new char array that can fit all
	 * the items in this, and proceeds to copy into that and return that.
	 *
	 * @param array a char array that will be modified if it can fit {@link #size()} items
	 * @return {@code array}, if it had sufficient size, or a new array otherwise, either with a copy of this
	 */
	public char[] toArray(char[] array) {
		if (array.length < size)
			array = new char[size];
		System.arraycopy(items, 0, array, 0, size);
		return array;
	}

	@Override
	public int hashCode() {
		char[] items = this.items;
		int h = size;
		for (int i = 0, n = size; i < n; i++) {
			h = h * 31 + items[i];
		}
		return h ^ h >>> 16;
	}

	@Override
	public boolean equals(Object object) {
		if (object == this) {
			return true;
		}
		if (!(object instanceof CharSequence)) {
			return false;
		}
		CharSequence csq = (CharSequence) object;
		int n = size;
		if (n != csq.length()) {
			return false;
		}
		char[] items1 = this.items;
		for (int i = 0; i < n; i++) {
			if (items1[i] != csq.charAt(i)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Checks if this is equal to another CharSequence, but runs all chars in both the given text and this through
	 * {@link Casing#caseUp(char)} before comparing (making the comparison case-insensitive for almost all scripts in
	 * use today, except some situations for Georgian).
	 *
	 * @param csq any other CharSequence, such as a String, StringBuilder, or CharList
	 * @return true if the chars in this are equivalent to those in {@code csq} if compared as case-insensitive
	 */
	public boolean equalsIgnoreCase(CharSequence csq) {
		if (csq == this) {
			return true;
		}
		int n = size;
		if (n != csq.length()) {
			return false;
		}
		char[] items1 = this.items;
		for (int i = 0; i < n; i++) {
			if (Casing.caseUp(items1[i]) != Casing.caseUp(csq.charAt(i))) {
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
	 * Simply returns all the char items in this as one String, with no delimiters.
	 * This is the same as calling {@code String.valueOf(charList.items, 0, charList.size())} .
	 *
	 * @return a String containing only the char items in this CharList
	 */
	public String toDenseString() {
		return String.valueOf(items, 0, size);
	}

	/**
	 * Returns a Java 8 primitive iterator over the int items in this CharList. Iterates in order if
	 * {@link #keepsOrder()} returns true, which it does for a CharList but not a CharBag.
	 * <br>
	 * This will reuse one of two iterators in this CharList; this does not allow nested iteration.
	 * Use {@link CharListIterator#CharListIterator(CharList)} to nest iterators.
	 *
	 * @return a {@link CharIterator}; use its nextChar() method instead of next()
	 */
	@Override
	public CharListIterator iterator() {
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

	@Override
	public CharList append(@Nullable CharSequence csq) {
		if(csq == null) {
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
	public CharList append(@Nullable CharSequence csq, int start, int end) {
		if(csq == null) {
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
	public CharList append(char c) {
		add(c);
		return this;
	}

	/**
	 * Adds {@code count} repetitions of {@code padWith} to the start (left) of this list.
	 * @param count how many repetitions of {@code padWith} to add
	 * @param padWith the item to pad with
	 * @return this, for chaining
	 */
	public CharList padLeft(int count, char padWith) {
		if(count > 0) {
			ensureCapacity(count);
			System.arraycopy(items, 0, items, count, size);
			Arrays.fill(items, 0, count, padWith);
			size += count;
		}
		return this;
	}

	/**
	 * Adds {@code count} repetitions of {@code padWith} to the end (right) of this list.
	 * @param count how many repetitions of {@code padWith} to add
	 * @param padWith the item to pad with
	 * @return this, for chaining
	 */
	public CharList padRight(int count, char padWith) {
		if(count > 0) {
			ensureCapacity(count);
			Arrays.fill(items, size, size + count, padWith);
			size += count;
		}
		return this;
	}

	@Override
	public int compareTo(CharList o) {
		if (o == null) return Integer.MAX_VALUE;
		final int tLen = size(), oLen = o.length();
		if (tLen == oLen) {
			for (int i = 0; i < oLen; i++) {
				int diff = get(i) - o.charAt(i);
				if (diff != 0) {
					return diff;
				}
			}
			return 0;
		} else {
			return tLen - oLen;
		}
	}

	/**
	 * Compares this CharList with an arbitrary CharSequence type, returning the lexicographical comparison of the two
	 * as if Java 11's {@code CharSequence.compare()} was called on this and {@code other}. This does not need Java 11.
	 * <br>
	 * The name here is different from {@link #compareTo(CharList)} because this does not satisfy an important
	 * constraint of the {@link Comparable} interface (while this has a compareWith method that can take a CharSequence,
	 * an arbitrary CharSequence cannot be compared to this type with compareTo() or compareWith() ).
	 *
	 * @param other any other CharSequence; if null, the comparison will return {@link Integer#MAX_VALUE}
	 * @return a positive number, zero, or a negative number if this is lexicographically greater than, equal to, or
	 * less than other, respectively
	 */
	public int compareWith(@Nullable CharSequence other) {
		if (other == null) return Integer.MAX_VALUE;
		final int tLen = size(), oLen = other.length();
		if (tLen == oLen) {
			for (int i = 0; i < oLen; i++) {
				int diff = get(i) - other.charAt(i);
				if (diff != 0) {
					return diff;
				}
			}
			return 0;
		} else {
			return tLen - oLen;
		}
	}

	/**
	 * Compares this CharList with an arbitrary CharSequence type, returning the lexicographical comparison of the two
	 * as if Java 11's {@code CharSequence.compare()} was called on this if it had been entirely raised to upper case
	 * and {@code other} if it also had been entirely raised to upper case. This does not need Java 11. This uses
	 * {@link Casing#caseUp(char)} to perform its case conversion, which works for all alphabets that have case except
	 * for the Georgian alphabet.
	 * <br>
	 * The name here is different from {@link #compareTo(CharList)} because this does not satisfy an important
	 * constraint of the {@link Comparable} interface (while this has a compareWith method that can take a CharSequence,
	 * an arbitrary CharSequence cannot be compared to this type with compareTo() or compareWith() ).
	 *
	 * @param other any other CharSequence; if null, the comparison will return {@link Integer#MAX_VALUE}
	 * @return a positive number, zero, or a negative number if this is lexicographically greater than, equal to, or
	 * less than other, respectively, using case-insensitive comparison
	 */
	public int compareWithIgnoreCase(@Nullable CharSequence other) {
		if (other == null) return Integer.MAX_VALUE;
		final int tLen = size(), oLen = other.length();
		if (tLen == oLen) {
			for (int i = 0; i < oLen; i++) {
				int diff = Casing.caseUp(get(i)) - Casing.caseUp(other.charAt(i));
				if (diff != 0) {
					return diff;
				}
			}
			return 0;
		} else {
			return tLen - oLen;
		}
	}

	/**
	 * A {@link CharIterator}, plus {@link ListIterator} methods, over the elements of a CharList.
	 * Use {@link #nextChar()} in preference to {@link #next()} to avoid allocating Character objects.
	 */
	public static class CharListIterator implements CharIterator {
		protected int index, latest = -1;
		protected CharList list;
		/**
		 * Used to track if a reusable iterator can be used now.
		 * This is public so subclasses of CharList (in other packages) can still access this
		 * directly even though it belongs to CharListIterator, not CharList.
		 */
		public boolean valid = true;

		public CharListIterator(CharList list) {
			this.list = list;
		}

		public CharListIterator(CharList list, int index) {
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
		public char nextChar() {
			if (!valid) {
				throw new RuntimeException("#iterator() cannot be used nested.");
			}
			if (index >= list.size()) {
				throw new NoSuchElementException();
			}
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
		public boolean hasNext() {
			if (!valid) {
				throw new RuntimeException("#iterator() cannot be used nested.");
			}
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
		public boolean hasPrevious() {
			if (!valid) {
				throw new RuntimeException("#iterator() cannot be used nested.");
			}
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
		public char previousChar() {
			if (!valid) {
				throw new RuntimeException("#iterator() cannot be used nested.");
			}
			if (index <= 0 || list.isEmpty()) {
				throw new NoSuchElementException();
			}
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
		public int nextIndex() {
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
		public int previousIndex() {
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
		public void remove() {
			if (!valid) {
				throw new RuntimeException("#iterator() cannot be used nested.");
			}
			if (latest == -1 || latest >= list.size()) {
				throw new NoSuchElementException();
			}
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
		public void set(char t) {
			if (!valid) {
				throw new RuntimeException("#iterator() cannot be used nested.");
			}
			if (latest == -1 || latest >= list.size()) {
				throw new NoSuchElementException();
			}
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
		public void add(char t) {
			if (!valid) {
				throw new RuntimeException("#iterator() cannot be used nested.");
			}
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
				throw new IndexOutOfBoundsException("CharListIterator does not satisfy index >= 0 && index < list.size()");
			this.index = index;
			latest = -1;
		}

		/**
		 * Returns an iterator over elements of type {@code char}.
		 *
		 * @return this same CharListIterator.
		 */
		public CharList.CharListIterator iterator() {
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
	public static CharList with() {
		return new CharList(0);
	}

	/**
	 * Creates a new CharList that holds only the given item, but can be resized.
	 *
	 * @param item a char item
	 * @return a new CharList that holds the given item
	 */

	public static CharList with(char item) {
		CharList list = new CharList(1);
		list.add(item);
		return list;
	}

	/**
	 * Creates a new CharList that holds only the given items, but can be resized.
	 *
	 * @param item0 a char item
	 * @param item1 a char item
	 * @return a new CharList that holds the given items
	 */
	public static CharList with(char item0, char item1) {
		CharList list = new CharList(2);
		list.add(item0);
		list.add(item1);
		return list;
	}

	/**
	 * Creates a new CharList that holds only the given items, but can be resized.
	 *
	 * @param item0 a char item
	 * @param item1 a char item
	 * @param item2 a char item
	 * @return a new CharList that holds the given items
	 */
	public static CharList with(char item0, char item1, char item2) {
		CharList list = new CharList(3);
		list.add(item0);
		list.add(item1);
		list.add(item2);
		return list;
	}

	/**
	 * Creates a new CharList that holds only the given items, but can be resized.
	 *
	 * @param item0 a char item
	 * @param item1 a char item
	 * @param item2 a char item
	 * @param item3 a char item
	 * @return a new CharList that holds the given items
	 */
	public static CharList with(char item0, char item1, char item2, char item3) {
		CharList list = new CharList(4);
		list.add(item0);
		list.add(item1);
		list.add(item2);
		list.add(item3);
		return list;
	}

	/**
	 * Creates a new CharList that holds only the given items, but can be resized.
	 *
	 * @param item0 a char item
	 * @param item1 a char item
	 * @param item2 a char item
	 * @param item3 a char item
	 * @param item4 a char item
	 * @return a new CharList that holds the given items
	 */
	public static CharList with(char item0, char item1, char item2, char item3, char item4) {
		CharList list = new CharList(5);
		list.add(item0);
		list.add(item1);
		list.add(item2);
		list.add(item3);
		list.add(item4);
		return list;
	}

	/**
	 * Creates a new CharList that holds only the given items, but can be resized.
	 *
	 * @param item0 a char item
	 * @param item1 a char item
	 * @param item2 a char item
	 * @param item3 a char item
	 * @param item4 a char item
	 * @param item5 a char item
	 * @return a new CharList that holds the given items
	 */
	public static CharList with(char item0, char item1, char item2, char item3, char item4, char item5) {
		CharList list = new CharList(6);
		list.add(item0);
		list.add(item1);
		list.add(item2);
		list.add(item3);
		list.add(item4);
		list.add(item5);
		return list;
	}

	/**
	 * Creates a new CharList that holds only the given items, but can be resized.
	 *
	 * @param item0 a char item
	 * @param item1 a char item
	 * @param item2 a char item
	 * @param item3 a char item
	 * @param item4 a char item
	 * @param item5 a char item
	 * @param item6 a char item
	 * @return a new CharList that holds the given items
	 */
	public static CharList with(char item0, char item1, char item2, char item3, char item4, char item5, char item6) {
		CharList list = new CharList(7);
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
	 * Creates a new CharList that holds only the given items, but can be resized.
	 *
	 * @param item0 a char item
	 * @param item1 a char item
	 * @param item2 a char item
	 * @param item3 a char item
	 * @param item4 a char item
	 * @param item5 a char item
	 * @param item6 a char item
	 * @return a new CharList that holds the given items
	 */
	public static CharList with(char item0, char item1, char item2, char item3, char item4, char item5, char item6, char item7) {
		CharList list = new CharList(8);
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
	 * Creates a new CharList that holds only the given items, but can be resized.
	 * This overload will only be used when an array is supplied and the type of the
	 * items requested is the component type of the array, or if varargs are used and
	 * there are 9 or more arguments.
	 *
	 * @param varargs a char varargs or char array; remember that varargs allocate
	 * @return a new CharList that holds the given items
	 */
	public static CharList with(char... varargs) {
		return new CharList(varargs);
	}
}

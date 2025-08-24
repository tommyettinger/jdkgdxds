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

import com.github.tommyettinger.ds.support.sort.ByteComparator;
import com.github.tommyettinger.ds.support.sort.ByteComparators;
import com.github.tommyettinger.ds.support.sort.ShortComparator;
import com.github.tommyettinger.ds.support.sort.ShortComparators;
import com.github.tommyettinger.ds.support.util.ShortIterator;
import com.github.tommyettinger.function.ShortToShortFunction;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;

/**
 * A resizable, insertion-ordered double-ended queue of primitive {@code short} with efficient add and remove at the
 * beginning and end. This extends {@link ShortList} and supports {@link RandomAccess}. Like ShortList, it is a
 * {@link OfShort}, {@link Arrangeable}, and {@link Ordered.OfShort}.
 * Values in the backing array may wrap back to the beginning, making add and remove at the beginning and end O(1)
 * (unless the backing array needs to resize when adding). Deque functionality is provided via {@link #removeLast()} and
 * {@link #addFirst(short)}.
 * <br>
 * Unlike most Deque implementations in the JDK, you can get and set items anywhere in the deque in constant time with
 * {@link #get(int)} and {@link #set(int, short)}. Relative to a {@link ShortList}, {@link #get(int)} has slightly
 * higher overhead, but it still runs in constant time. Unlike ArrayDeque in the JDK, this implements
 * {@link #equals(Object)} and {@link #hashCode()}. This can provide what are effectively
 * {@link ListIterator ListIterators} for iteration from an index or in reverse order.
 * <br>
 * Unlike {@link ArrayDeque} or {@link ArrayList}, most methods that take an index here try to be "forgiving;" that is,
 * they treat negative indices as index 0, and too-large indices as the last index, rather than throwing an Exception,
 * except in some cases where the ShortDeque is empty and an item from it is required. An exception is in
 * {@link #set(int, short)}, which allows prepending by setting a negative index, or appending by setting a too-large
 * index. This isn't a standard JDK behavior, and it doesn't always act how Deque or List is documented.
 * <br>
 * Some new methods are present here, or have been made public when they weren't before. {@link #removeRange(int, int)},
 * for instance, is now public, as is {@link #resize(int)}. New APIs include Deque-like methods that affect the middle
 * of the deque, such as {@link #peekAt(int)} and {@link #pollAt(int)}. There are more bulk methods that work at the
 * head or tail region of the deque, such as {@link #addAllFirst(OfShort)} and {@link #truncateFirst(int)}. There are
 * the methods from {@link Arrangeable}, and many default methods from PrimitiveCollection and Ordered.
 * <br>
 * In general, this is an improvement over {@link ArrayDeque} in every type of functionality, and is mostly equivalent
 * to {@link ShortList} as long as the performance of {@link #get(int)} is adequate. Because it is array-backed, it
 * should usually be much faster than {@link LinkedList}, as well; only periodic resizing and modifications in the
 * middle of the List using an iterator should be typically faster for {@link LinkedList}.
 */
public class ShortDeque extends ShortList implements RandomAccess, Arrangeable, PrimitiveCollection.OfShort, Ordered.OfShort {

	/**
	 * The value returned when nothing can be obtained from this deque and an exception is not meant to be thrown,
	 * such as when calling {@link #peek()} on an empty deque.
	 */
	public short defaultValue = 0;

	/**
	 * Index of first element. Logically smaller than tail. Unless empty, it points to a valid element inside the deque.
	 */
	protected int head = 0;

	/**
	 * Index of last element. Logically bigger than head. Unless empty, it points to a valid element inside the deque.
	 * This may be the same as head, and is if there is one element in the deque (or none), that will be the case.
	 */
	protected int tail = 0;

	@Nullable
	protected transient ShortDequeIterator descendingIterator1;
	@Nullable
	protected transient ShortDequeIterator descendingIterator2;

	/**
	 * Creates a new ShortDeque which can hold 16 values without needing to resize the backing array.
	 */
	public ShortDeque() {
		this(16);
	}

	/**
	 * Creates a new ShortDeque which can hold the specified number of values without needing to resize the backing
	 * array.
	 *
	 * @param initialSize how large the backing array should be, without any padding
	 */
	public ShortDeque(int initialSize) {
		super(Math.max(1, initialSize));
	}

	/**
	 * Creates a new ShortDeque using all the contents of the given Collection.
	 *
	 * @param coll a Collection of short that will be copied into this and used in full
	 * @throws NullPointerException if {@code coll} is {@code null}
	 */
	public ShortDeque(OfShort coll) {
		this(coll.size());
		addAll(coll);
	}

	/**
	 * Creates a new instance containing the items in the specified iterator.
	 *
	 * @param iter an iterator that will have its remaining contents added to this
	 * @throws NullPointerException if {@code iter} is {@code null}
	 */
	public ShortDeque(ShortIterator iter) {
		this(16);
		addAll(iter);
	}

	/**
	 * Copies the given ShortDeque exactly into this one. Individual values will be shallow-copied.
	 *
	 * @param deque another ShortDeque to copy
	 * @throws NullPointerException if {@code deque} is {@code null}
	 */
	public ShortDeque(ShortDeque deque) {
		this(deque.items.length);
		System.arraycopy(deque.items, 0, items, 0, deque.items.length);
		this.size = deque.size;
		this.head = deque.head;
		this.tail = deque.tail;
		this.defaultValue = deque.defaultValue;
	}

	public ShortDeque(Ordered.OfShort other, int offset, int count) {
		this(count);
		addAll(0, other, offset, count);
	}

	/**
	 * Creates a new ShortDeque using all the contents of the given array.
	 *
	 * @param a an array of short that will be copied into this and used in full
	 * @throws NullPointerException if {@code a} is {@code null}
	 */
	public ShortDeque(short[] a) {
		this(a.length);
		System.arraycopy(a, 0, items, 0, a.length);
		size = a.length;
		tail = Math.max(0, size - 1);
	}

	/**
	 * Creates a new ShortDeque using {@code count} items from {@code a}, starting at {@code offset}.
	 * If {@code count} is 0 or less, this will create an empty ShortDeque with capacity 1.
	 *
	 * @param a      an array of short
	 * @param offset where in {@code a} to start using items
	 * @param count  how many items to use from {@code a}
	 * @throws NullPointerException if {@code a} is {@code null}
	 */
	public ShortDeque(short[] a, int offset, int count) {
		this(count);
		System.arraycopy(a, offset, items, 0, count);
		size = count;
		tail = Math.max(0, count - 1);
	}


	public ShortDeque(Ordered.OfShort other) {
		this(other, 0, other.size());
	}

	@Override
	public ShortDeque order() {
		return this;
	}

	/**
	 * Gets the default value, which is the value returned when nothing can be obtained from this deque and an exception
	 * is not meant to be thrown, such as when calling peek() on an empty deque. Unless changed, the default value is
	 * usually {@code null}.
	 *
	 * @return the current default value
	 */
	public short getDefaultValue() {
		return defaultValue;
	}

	/**
	 * Sets the default value, which is the value returned when nothing can be obtained from this deque and an exception
	 * is not meant to be thrown, such as when calling peek() on an empty deque. Unless changed, the default value is
	 * usually {@code null}.
	 *
	 * @param defaultValue any short this can return instead of throwing an Exception, or {@code null}
	 */
	public void setDefaultValue(short defaultValue) {
		this.defaultValue = defaultValue;
	}

	/**
	 * Appends given short to the tail (enqueue to tail). Unless the backing array needs resizing, operates in O(1) time.
	 *
	 * @param value can be null
	 */
	public void addLast(short value) {
		short[] items = this.items;

		if (size == items.length)
			items = resize(items.length << 1);

		if (++tail == items.length) tail = 0;
		if (++size == 1) tail = head;
		items[tail] = value;
	}

	public void addLast(short value1, short value2) {
		short[] items = this.items;

		if (size + 2 > items.length)
			items = resize(size + 2 << 1);

		if (++tail == items.length) tail = 0;
		if (size == 0) tail = head;
		items[tail] = value1;
		if (++tail == items.length) tail = 0;
		items[tail] = value2;
		size += 2;
	}

	public void addLast(short value1, short value2, short value3) {
		short[] items = this.items;

		if (size + 3 > items.length)
			items = resize(size + 3 << 1);

		if (++tail == items.length) tail = 0;
		if (size == 0) tail = head;
		items[tail] = value1;
		if (++tail == items.length) tail = 0;
		items[tail] = value2;
		if (++tail == items.length) tail = 0;
		items[tail] = value3;
		size += 3;
	}

	public void addLast(short value1, short value2, short value3, short value4) {
		short[] items = this.items;

		if (size + 4 > items.length)
			items = resize(size + 4 << 1);

		if (++tail == items.length) tail = 0;
		if (size == 0) tail = head;
		items[tail] = value1;
		if (++tail == items.length) tail = 0;
		items[tail] = value2;
		if (++tail == items.length) tail = 0;
		items[tail] = value3;
		if (++tail == items.length) tail = 0;
		items[tail] = value4;
		size += 4;
	}

	/**
	 * Prepend given value to the head (enqueue to head). Unless backing array needs resizing, operates in O(1) time.
	 *
	 * @param value can be null
	 * @see #addLast(short)
	 */
	public void addFirst(short value) {
		short[] items = this.items;

		if (size == items.length)
			items = resize(items.length << 1);

		int head = this.head - 1;
		if (head == -1) head = items.length - 1;
		items[head] = value;

		this.head = head;
		if (++size == 1) tail = head;
	}

	public void addFirst(short value1, short value2) {
		short[] items = this.items;

		if (size + 2 > items.length)
			items = resize(size + 2 << 1);


		int head = this.head - 1;
		if (head == -1) head = items.length - 1;
		if (size == 0) tail = head;
		items[head] = value2;
		if (--head == -1) head = items.length - 1;
		items[head] = value1;
		size += 2;

		this.head = head;
	}

	public void addFirst(short value1, short value2, short value3) {
		short[] items = this.items;

		if (size + 3 > items.length)
			items = resize(size + 3 << 1);

		int head = this.head - 1;
		if (head == -1) head = items.length - 1;
		if (size == 0) tail = head;
		items[head] = value3;
		if (--head == -1) head = items.length - 1;
		items[head] = value2;
		if (--head == -1) head = items.length - 1;
		items[head] = value1;
		size += 3;

		this.head = head;
	}

	public void addFirst(short value1, short value2, short value3, short value4) {
		short[] items = this.items;

		if (size + 4 > items.length)
			items = resize(size + 4 << 1);

		int head = this.head - 1;
		if (head == -1) head = items.length - 1;
		if (size == 0) tail = head;
		items[head] = value4;
		if (--head == -1) head = items.length - 1;
		items[head] = value3;
		if (--head == -1) head = items.length - 1;
		items[head] = value2;
		if (--head == -1) head = items.length - 1;
		items[head] = value1;
		size += 4;

		this.head = head;
	}

	/**
	 * Trims the capacity of this {@code ShortDeque} instance to be the
	 * deque's current size.  An application can use this operation to minimize
	 * the storage of a {@code ShortDeque} instance.
	 */
	public void trimToSize() {
		if (size < items.length) {
			if (head <= tail) {
				items = Arrays.copyOfRange(items, head, tail + 1);
			} else {
				short[] next = new short[size];
				System.arraycopy(items, head, next, 0, items.length - head);
				System.arraycopy(items, 0, next, items.length - head, tail + 1);
				items = next;
			}
			head = 0;
			tail = items.length - 1;
		}
	}

	@Override
	public short[] shrink() {
		trimToSize();
		return items;
	}

	/**
	 * Increases the size of the backing array to accommodate the specified number of additional items. Useful before adding many
	 * items to avoid multiple backing array resizes.
	 *
	 * @return the backing array this will use after this call
	 */
	public short[] ensureCapacity(int additional) {
		final int needed = size + additional;
		if (items.length < needed) {
			resize(needed);
		}
		return items;
	}

	/**
	 * Resizes the backing array. newSize should be greater than the current size; otherwise, newSize will be set to
	 * size and the resize to the same size will (for most purposes) be wasted effort. If this is not empty, this will
	 * rearrange the items internally to be linear and have the head at index 0, with the tail at {@code size - 1}.
	 * This always allocates a new internal backing array.
	 *
	 * @return the new backing array, as a direct reference
	 */
	public short[] resize(int newSize) {
		if (newSize < size)
			newSize = size;
		final short[] items = this.items;
		final int head = this.head;
		final int tail = this.tail;

		final short[] newArray = new short[Math.max(1, newSize)];

		if (size > 0) {
			if (head <= tail) {
				// Continuous
				System.arraycopy(items, head, newArray, 0, tail - head + 1);
			} else {
				// Wrapped
				final int rest = items.length - head;
				System.arraycopy(items, head, newArray, 0, rest);
				System.arraycopy(items, 0, newArray, rest, tail + 1);
			}
			this.head = 0;
			this.tail = size - 1;
		}
		this.items = newArray;
		return newArray;
	}

	/**
	 * Make sure there is a "gap" of exactly {@code gapSize} values starting at {@code index}. This can
	 * resize the backing array to achieve this goal. If possible, this will keep the same backing array and modify
	 * it in-place. The "gap" is not assigned null, and may contain old/duplicate references; calling code <em>must</em>
	 * overwrite the entire gap with additional values to ensure GC correctness.
	 *
	 * @param index   the 0-based index in the iteration order where the gap will be present
	 * @param gapSize the number of items that will need filling in the gap, and can be filled without issues.
	 * @return the position in the array where the gap will begin, which is unrelated to the index
	 */
	protected int ensureGap(int index, int gapSize) {
		if (gapSize <= 0) return 0;
		if (index < 0) index = 0;
		if (index > size) {
			int oldSize = size;
			ensureCapacity(gapSize);
			return oldSize;
		}
		if (size == 0) {
			this.head = 0;
			this.tail = gapSize - 1;
			if (items.length < gapSize) {
				this.items = new short[gapSize];
			}
			return 0;
		} else if (size == 1) {
			if (items.length < gapSize + size) {
				short item = this.items[head];
				this.items = new short[gapSize + size];
				if (index == 0) {
					this.items[gapSize] = item;
					this.head = 0;
					this.tail = gapSize;
					return 0;
				} else {
					this.items[0] = item;
					this.head = 0;
					this.tail = gapSize;
					return 1;
				}
			} else {
				if (index != 0) {
					if (head != 0) {
						this.items[0] = this.items[head];
					}
					this.head = 0;
					this.tail = gapSize;
					return 1;
				} else {
					if (head != gapSize) {
						this.items[gapSize] = this.items[head];
					}
					this.head = 0;
					this.tail = gapSize;
					return 0;
				}
			}
		}

		final short[] items = this.items;
		final int head = this.head;
		final int tail = this.tail;
		final int newSize = Math.max(size + gapSize, items.length);
		if (newSize == items.length) {
			// keep the same array because there is enough room to form the gap.
			if (head <= tail) {
				if (head != 0) {
					if (index > 0)
						System.arraycopy(items, head, items, 0, index);
					this.head = 0;
				}
				System.arraycopy(items, head + index, items, index + gapSize, size - this.head - index);
				this.tail += gapSize - (head - this.head);
				return index;
			} else {
				if (head + index <= this.items.length) {
					if (head - gapSize >= 0) {
						System.arraycopy(this.items, head, this.items, head - gapSize, index);
						this.head -= gapSize;
						return this.head + index;
					} else {
						System.arraycopy(this.items, head + index, this.items, head + index + gapSize, this.items.length - (head + index + gapSize));
						this.tail += gapSize;
						return this.head + index;
					}
				} else {
					int wrapped = head + index - items.length;
					System.arraycopy(items, wrapped, items, wrapped + gapSize, tail + 1 - wrapped);
					this.tail += gapSize;
					return wrapped;
				}
			}
		} else {
			final short[] newArray = new short[newSize];

			if (head <= tail) {
				// Continuous
				if (index > 0)
					System.arraycopy(items, head, newArray, 0, index);
				this.head = 0;
				System.arraycopy(items, head + index, newArray, index + gapSize, size - head - index);
				this.tail += gapSize;
			} else {
				// Wrapped
				final int headPart = items.length - head;
				if (index < headPart) {
					if (index > 0)
						System.arraycopy(items, head, newArray, 0, index);
					this.head = 0;
					System.arraycopy(items, head + index, newArray, index + gapSize, headPart - index);
					System.arraycopy(items, 0, newArray, index + gapSize + headPart - index, tail + 1);
					this.tail = size + gapSize - 1;
				} else {
					System.arraycopy(items, head, newArray, 0, headPart);
					int wrapped = index - headPart; // same as: head + index - values.length;
					System.arraycopy(items, 0, newArray, headPart, wrapped);
					System.arraycopy(items, wrapped, newArray, headPart + wrapped + gapSize, tail + 1 - wrapped);
					this.tail = size + gapSize - 1;
				}
			}
			this.items = newArray;
			return index;
		}
	}

	@Override
	public boolean addAll(ShortList list) {
		return addAll(size, list, 0, list.size());
	}

	@Override
	public boolean addAll(ShortList list, int offset, int count) {
		return addAll(size, list, offset, count);
	}

	/**
	 * Remove the first item from the deque. (dequeue from head) Always O(1).
	 *
	 * @return removed short
	 * @throws NoSuchElementException when the deque is empty
	 */
	public short removeFirst() {
		if (size == 0) {
			// Underflow
			throw new NoSuchElementException("ShortDeque is empty.");
		}

		final short[] items = this.items;

		final short result = items[head];

		head++;
		if (head == items.length) {
			head = 0;
		}
		if (--size <= 1) tail = head;

		return result;
	}

	/**
	 * Remove the last item from the deque. (dequeue from tail) Always O(1).
	 *
	 * @return removed short
	 * @throws NoSuchElementException when the deque is empty
	 * @see #removeFirst()
	 */
	public short removeLast() {
		if (size == 0) {
			throw new NoSuchElementException("ShortDeque is empty.");
		}

		final short[] items = this.items;
		int tail = this.tail;
		final short result = items[tail];

		if (tail == 0) {
			tail = items.length - 1;
		} else {
			--tail;
		}
		if (--size <= 1) tail = head;

		this.tail = tail;

		return result;
	}

	/**
	 * Inserts the specified element at the front of this deque unless it would
	 * violate capacity restrictions.  When using a capacity-restricted deque,
	 * this method is generally preferable to the {@link #addFirst} method,
	 * which can fail to insert an element only by throwing an exception.
	 *
	 * @param t the element to add
	 * @return {@code true} if the element was added to this deque, else
	 * {@code false}
	 * @throws ClassCastException       if the class of the specified element
	 *                                  prevents it from being added to this deque
	 * @throws NullPointerException     if the specified element is null and this
	 *                                  deque does not permit null elements
	 * @throws IllegalArgumentException if some property of the specified
	 *                                  element prevents it from being added to this deque
	 */
	public boolean offerFirst(short t) {
		addFirst(t);
		return true;
	}

	/**
	 * Inserts the specified element at the end of this deque unless it would
	 * violate capacity restrictions.  When using a capacity-restricted deque,
	 * this method is generally preferable to the {@link #addLast} method,
	 * which can fail to insert an element only by throwing an exception.
	 *
	 * @param t the element to add
	 * @return {@code true} if the element was added to this deque, else
	 * {@code false}
	 * @throws ClassCastException       if the class of the specified element
	 *                                  prevents it from being added to this deque
	 * @throws NullPointerException     if the specified element is null and this
	 *                                  deque does not permit null elements
	 * @throws IllegalArgumentException if some property of the specified
	 *                                  element prevents it from being added to this deque
	 */
	public boolean offerLast(short t) {
		addLast(t);
		return true;
	}

	/**
	 * Retrieves and removes the first element of this deque,
	 * or returns {@link #getDefaultValue() defaultValue} if this deque is empty. The default value is usually
	 * {@code null} unless it has been changed with {@link #setDefaultValue(short)}.
	 *
	 * @return the head of this deque, or {@link #getDefaultValue() defaultValue} if this deque is empty
	 * @see #removeFirst() the alternative removeFirst() throws an Exception if the deque is empty
	 */
	public short pollFirst() {
		if (size == 0) {
			// Underflow
			return defaultValue;
		}

		final short[] items = this.items;

		final short result = items[head];

		head++;
		if (head == items.length) {
			head = 0;
		}
		if (--size <= 1) tail = head;

		return result;
	}

	/**
	 * Retrieves and removes the last element of this deque,
	 * or returns {@link #getDefaultValue() defaultValue} if this deque is empty. The default value is usually
	 * {@code null} unless it has been changed with {@link #setDefaultValue(short)}.
	 *
	 * @return the tail of this deque, or {@link #getDefaultValue() defaultValue} if this deque is empty
	 * @see #removeLast() the alternative removeLast() throws an Exception if the deque is empty
	 */
	public short pollLast() {
		if (size == 0) {
			return defaultValue;
		}

		final short[] items = this.items;
		int tail = this.tail;
		final short result = items[tail];

		if (tail == 0) {
			tail = items.length - 1;
		} else {
			--tail;
		}
		if (--size <= 1) tail = head;

		this.tail = tail;

		return result;
	}

	/**
	 * Retrieves, but does not remove, the first element of this deque.
	 * <p>
	 * This method differs from {@link #peekFirst peekFirst} only in that it
	 * throws an exception if this deque is empty.
	 *
	 * @return the head of this deque
	 * @throws NoSuchElementException if this deque is empty
	 */
	public short getFirst() {
		return first();
	}

	/**
	 * Retrieves, but does not remove, the last element of this deque.
	 * This method differs from {@link #peekLast peekLast} only in that it
	 * throws an exception if this deque is empty.
	 *
	 * @return the tail of this deque
	 * @throws NoSuchElementException if this deque is empty
	 */
	public short getLast() {
		return last();
	}

	/**
	 * Retrieves, but does not remove, the first element of this deque,
	 * or returns {@link #getDefaultValue() defaultValue} if this deque is empty.
	 *
	 * @return the head of this deque, or {@link #getDefaultValue() defaultValue} if this deque is empty
	 */
	public short peekFirst() {
		if (size == 0) {
			// Underflow
			return defaultValue;
		}
		return items[head];
	}

	/**
	 * Retrieves, but does not remove, the last element of this deque,
	 * or returns {@link #getDefaultValue() defaultValue} if this deque is empty.
	 *
	 * @return the tail of this deque, or {@link #getDefaultValue() defaultValue} if this deque is empty
	 */
	public short peekLast() {
		if (size == 0) {
			// Underflow
			return defaultValue;
		}
		return items[tail];
	}

	/**
	 * Removes the first occurrence of the specified element from this deque.
	 * If the deque does not contain the element, it is unchanged.
	 * More formally, removes the first element {@code e} such that
	 * {@code o == e} (if such an element exists).
	 * Returns {@code true} if this deque contained the specified element
	 * (or equivalently, if this deque changed as a result of the call).
	 *
	 * @param o element to be removed from this deque, if present
	 * @return {@code true} if an element was removed as a result of this call
	 */
	public boolean removeFirstOccurrence(short o) {
		return removeValue(o);
	}

	/**
	 * Removes the last occurrence of the specified element from this deque.
	 * If the deque does not contain the element, it is unchanged.
	 * More formally, removes the last element {@code e} such that
	 * {@code o == e} (if such an element exists).
	 * Returns {@code true} if this deque contained the specified element
	 * (or equivalently, if this deque changed as a result of the call).
	 *
	 * @param o element to be removed from this deque, if present
	 * @return {@code true} if an element was removed as a result of this call
	 */
	public boolean removeLastOccurrence(short o) {
		return removeLastValue(o);
	}

	/**
	 * Inserts the specified element into the deque represented by this deque
	 * (in other words, at the tail of this deque) if it is possible to do so
	 * immediately without violating capacity restrictions, returning
	 * {@code true} upon success and throwing an
	 * {@code IllegalStateException} if no space is currently available.
	 * When using a capacity-restricted deque, it is generally preferable to
	 * use {@link #offer(short) offer}.
	 *
	 * <p>This method is equivalent to {@link #addLast}.
	 *
	 * @param t the element to add
	 * @return {@code true} (as specified by {@link Collection#add})
	 */
	public boolean add(short t) {
		addLast(t);
		return true;
	}

	@Override
	public void add(short value1, short value2) {
		addLast(value1, value2);
	}

	@Override
	public void add(short value1, short value2, short value3) {
		addLast(value1, value2, value3);
	}

	@Override
	public void add(short value1, short value2, short value3, short value4) {
		addLast(value1, value2, value3, value4);
	}

	/**
	 * Inserts the specified element into this deque at the specified index.
	 * Unlike {@link #offerFirst(short)} and {@link #offerLast(short)}, this does not run in expected constant time unless
	 * the index is less than or equal to 0 (where it acts like offerFirst()) or greater than or equal to {@link #size()}
	 * (where it acts like offerLast()).
	 *
	 * @param index the index in the deque's insertion order to insert the item
	 * @param item  a short item to insert; may be null
	 */
	public void insert(int index, short item) {
		if (index <= 0)
			addFirst(item);
		else if (index >= size)
			addLast(item);
		else {
			short[] items = this.items;

			if (++size > items.length) {
				resize(items.length << 1);
				items = this.items;
			}

			if (head <= tail) {
				index += head;
				if (index >= items.length) index -= items.length;
				int after = index + 1;
				if (after >= items.length) after = 0;

				System.arraycopy(items, index, items, after, head + size - index - 1);
				items[index] = item;
				tail = head + size - 1;
				if (tail >= items.length) {
					tail = 0;
				}
			} else {
				if (head + index < items.length) {
					// backward shift
					System.arraycopy(items, head, items, head - 1, index);
					items[head - 1 + index] = item;
					head--;
				} else {
					// forward shift
					index = head + index - items.length;
					System.arraycopy(items, index, items, index + 1, tail - index + 1);
					items[index] = item;
					tail++;
				}
			}
		}
	}

	/**
	 * Inserts the specified element into the queue represented by this deque
	 * (in other words, at the tail of this deque) if it is possible to do so
	 * immediately without violating capacity restrictions, returning
	 * {@code true} upon success and {@code false} if no space is currently
	 * available.  When using a capacity-restricted deque, this method is
	 * generally preferable to the {@link #add} method, which can fail to
	 * insert an element only by throwing an exception.
	 *
	 * <p>This method is equivalent to {@link #offerLast}.
	 *
	 * @param t the element to add
	 * @return {@code true} if the element was added to this deque, else {@code false}
	 */
	public boolean offer(short t) {
		addLast(t);
		return true;
	}

	/**
	 * Retrieves and removes the head of the queue represented by this deque
	 * (in other words, the first element of this deque).
	 * This method differs from {@link #poll() poll()} only in that it
	 * throws an exception if this deque is empty.
	 *
	 * <p>This method is equivalent to {@link #removeFirst()}.
	 *
	 * @return the head of the queue represented by this deque
	 * @throws NoSuchElementException if this deque is empty
	 */
	public short remove() {
		return removeFirst();
	}

	/**
	 * Retrieves and removes the head of the queue represented by this deque
	 * (in other words, the first element of this deque), or returns
	 * {@link #getDefaultValue() defaultValue} if this deque is empty.
	 *
	 * <p>This method is equivalent to {@link #pollFirst()}.
	 *
	 * @return the first element of this deque, or {@link #getDefaultValue() defaultValue} if
	 * this deque is empty
	 */
	public short poll() {
		return pollFirst();
	}

	/**
	 * Retrieves, but does not remove, the head of the queue represented by
	 * this deque (in other words, the first element of this deque).
	 * This method differs from {@link #peek peek} only in that it throws an
	 * exception if this deque is empty.
	 *
	 * <p>This method is equivalent to {@link #getFirst()}.
	 *
	 * @return the head of the queue represented by this deque
	 * @throws NoSuchElementException if this deque is empty
	 */
	public short element() {
		return first();
	}

	/**
	 * Retrieves, but does not remove, the head of the queue represented by
	 * this deque (in other words, the first element of this deque), or
	 * returns {@link #getDefaultValue() defaultValue} if this deque is empty.
	 *
	 * <p>This method is equivalent to {@link #peekFirst()}.
	 *
	 * @return the head of the queue represented by this deque, or
	 * {@link #getDefaultValue() defaultValue} if this deque is empty
	 */
	public short peek() {
		return peekFirst();
	}

	/**
	 * Adds all the elements in the specified collection at the end
	 * of this deque, as if by calling {@link #addLast} on each one,
	 * in the order that they are returned by the collection's iterator.
	 *
	 * <p>When using a capacity-restricted deque, it is generally preferable
	 * to call {@link #offer(short) offer} separately on each element.
	 *
	 * <p>An exception encountered while trying to add an element may result
	 * in only some of the elements having been successfully added when
	 * the associated exception is thrown.
	 *
	 * @param c the elements to be inserted into this deque
	 * @return {@code true} if this deque changed as a result of the call
	 */
	public boolean addAll(OfShort c) {
		final int cs = c.size();
		if (cs == 0) return false;
		int oldSize = size;
		ensureCapacity(Math.max(cs, oldSize));
		if (c == this) {
			if (head <= tail) {
				if (tail + 1 < items.length)
					System.arraycopy(items, head, items, tail + 1, Math.min(size, items.length - tail - 1));
				if (items.length - tail - 1 < size)
					System.arraycopy(items, head + items.length - tail - 1, items, 0, size - (items.length - tail - 1));
			} else {
				System.arraycopy(items, head, items, tail + 1, items.length - head);
				System.arraycopy(items, 0, items, tail + 1 + items.length - head, tail + 1);
			}
			tail += oldSize;
			size += oldSize;
		} else {
			addAll(c.iterator());
		}
		return oldSize != size;
	}

	/**
	 * An alias for {@link #addAll(OfShort)}, this adds every item in {@code c} to this in order at the end.
	 *
	 * @param c the elements to be inserted into this deque
	 * @return {@code true} if this deque changed as a result of the call
	 */
	public boolean addAllLast(OfShort c) {
		return addAll(c);
	}

	/**
	 * Adds every item in {@code c} to this in order at the start. The iteration order of {@code c} will be preserved
	 * for the added items.
	 *
	 * @param c the elements to be inserted into this deque
	 * @return {@code true} if this deque changed as a result of the call
	 */
	public boolean addAllFirst(OfShort c) {
		final int cs = c.size();
		if (cs == 0) return false;
		int oldSize = size;
		ensureCapacity(Math.max(cs, oldSize));
		if (c == this) {
			if (head <= tail) {
				if (head >= oldSize)
					System.arraycopy(items, head, items, head - oldSize, oldSize);
				else if (head > 0) {
					System.arraycopy(items, tail + 1 - head, items, 0, head);
					System.arraycopy(items, head, items, items.length - (oldSize - head), oldSize - head);
				} else {
					System.arraycopy(items, head, items, items.length - oldSize, oldSize);
				}
			} else {
				System.arraycopy(items, head, items, head - oldSize, items.length - head);
				System.arraycopy(items, 0, items, items.length - oldSize, tail + 1);
			}
			head -= oldSize;
			if (head < 0) head += items.length;
			size += oldSize;
		} else {
			int i = ensureGap(0, cs);
			ShortIterator it = c.iterator();
			while (it.hasNext()) {
				items[i++] = it.nextShort();
				if (i == items.length) i = 0;
			}
			size += cs;
		}
		return oldSize != size;
	}

	/**
	 * An alias for {@link #addAll(int, OfShort)}; inserts all elements
	 * in the specified collection into this list at the specified position.
	 * Shifts the element currently at that position (if any) and any subsequent
	 * elements to the right (increases their indices). The new elements
	 * will appear in this list in the order that they are returned by the
	 * specified collection's iterator. The behavior of this operation is
	 * undefined if the specified collection is modified while the
	 * operation is in progress. (Note that this will occur if the specified
	 * collection is this list, and it's nonempty.)
	 *
	 * @param index index at which to insert the first element from the
	 *              specified collection
	 * @param c     collection containing elements to be added to this list
	 * @return {@code true} if this list changed as a result of the call
	 */
	public boolean insertAll(int index, OfShort c) {
		return addAll(index, c);
	}

	public boolean addAll(int index, OfShort c) {
		int oldSize = size;
		if (index <= 0)
			addAllFirst(c);
		else if (index >= oldSize)
			addAll(c);
		else {
			final int cs = c.size();
			if (c.isEmpty()) return false;
			int place = ensureGap(index, cs);
			short[] items = this.items;
			if (c == this) {
				System.arraycopy(items, head, items, place, place - head);
				System.arraycopy(items, place + cs, items, place + place - head, tail + 1 - place - cs);
			} else {
				ShortIterator it = c.iterator();
				while (it.hasNext()) {
					items[place++] = it.nextShort();
					if (place >= items.length) place -= items.length;
				}
			}
			size += cs;
		}
		return oldSize != size;
	}

	/**
	 * Exactly like {@link #addAll(OfShort)}, but takes an array instead of a PrimitiveCollection.OfShort.
	 *
	 * @param array the elements to be inserted into this deque
	 * @return {@code true} if this deque changed as a result of the call
	 * @see #addAll(OfShort)
	 */
	public boolean addAll(short[] array) {
		return addAll(array, 0, array.length);
	}

	/**
	 * Like {@link #addAll(short[])}, but only uses at most {@code length} items from {@code array}, starting at {@code offset}.
	 *
	 * @param array  the elements to be inserted into this deque
	 * @param offset the index of the first item in array to add
	 * @param length how many items, at most, to add from array into this
	 * @return {@code true} if this deque changed as a result of the call
	 * @see #addAll(short[])
	 */
	public boolean addAll(short[] array, int offset, int length) {
		final int cs = Math.min(array.length - offset, length);
		if (cs <= 0) return false;
		int place = ensureGap(size, cs);
		System.arraycopy(array, offset, this.items, place, cs);
		size += cs;
		return true;
	}

	/**
	 * An alias for {@link #addAll(short[])}.
	 *
	 * @param array the elements to be inserted into this deque
	 * @return {@code true} if this deque changed as a result of the call
	 * @see #addAll(short[])
	 */
	public boolean addAllLast(short[] array) {
		return addAll(array, 0, array.length);
	}

	/**
	 * An alias for {@link #addAll(short[], int, int)}.
	 *
	 * @param array  the elements to be inserted into this deque
	 * @param offset the index of the first item in array to add
	 * @param length how many items, at most, to add from array into this
	 * @return {@code true} if this deque changed as a result of the call
	 * @see #addAll(short[], int, int)
	 */
	public boolean addAllLast(short[] array, int offset, int length) {
		return addAll(array, offset, length);
	}

	/**
	 * Exactly like {@link #addAllFirst(OfShort)}, but takes an array instead of a PrimitiveCollection.OfShort.
	 *
	 * @param array the elements to be inserted into this deque
	 * @return {@code true} if this deque changed as a result of the call
	 * @see #addAllFirst(OfShort)
	 */
	public boolean addAllFirst(short[] array) {
		return addAllFirst(array, 0, array.length);
	}

	/**
	 * Like {@link #addAllFirst(short[])}, but only uses at most {@code length} items from {@code array}, starting at
	 * {@code offset}. The order of {@code array} will be preserved, starting at the head of the deque.
	 *
	 * @param array  the elements to be inserted into this deque
	 * @param offset the index of the first item in array to add
	 * @param length how many items, at most, to add from array into this
	 * @return {@code true} if this deque changed as a result of the call
	 * @see #addAllFirst(short[])
	 */
	public boolean addAllFirst(short[] array, int offset, int length) {
		final int cs = Math.min(array.length - offset, length);
		if (cs <= 0) return false;
		int place = ensureGap(0, cs);
		System.arraycopy(array, offset, this.items, place, cs);
		size += cs;
		return true;
	}

	/**
	 * Alias for {@link #addAll(int, short[])}.
	 *
	 * @param index the index in this deque's iteration order to place the first item in {@code array}
	 * @param array the elements to be inserted into this deque
	 * @return {@code true} if this deque changed as a result of the call
	 */
	public boolean insertAll(int index, short[] array) {
		return addAll(index, array, 0, array.length);
	}

	/**
	 * Alias for {@link #addAll(int, short[], int, int)}.
	 *
	 * @param index  the index in this deque's iteration order to place the first item in {@code array}
	 * @param array  the elements to be inserted into this deque
	 * @param offset the index of the first item in array to add
	 * @param length how many items, at most, to add from array into this
	 * @return {@code true} if this deque changed as a result of the call
	 */
	public boolean insertAll(int index, short[] array, int offset, int length) {
		return addAll(index, array, offset, length);
	}

	/**
	 * Like {@link #addAll(int, OfShort)}, but takes an array instead of a PrimitiveCollection.OfShort and inserts it
	 * so the first item will be at the given {@code index}.
	 * The order of {@code array} will be preserved, starting at the given index in this deque.
	 *
	 * @param index the index in this deque's iteration order to place the first item in {@code array}
	 * @param array the elements to be inserted into this deque
	 * @return {@code true} if this deque changed as a result of the call
	 * @see #addAll(short[])
	 */
	public boolean addAll(int index, short[] array) {
		return addAll(index, array, 0, array.length);
	}

	/**
	 * Like {@link #addAll(int, OfShort)}, but takes an array instead of a PrimitiveCollection.OfShort, gets items starting at
	 * {@code offset} from that array, using {@code length} items, and inserts them
	 * so the item at the given offset will be at the given {@code index}.
	 * The order of {@code array} will be preserved, starting at the given index in this deque.
	 *
	 * @param index  the index in this deque's iteration order to place the first item in {@code array}
	 * @param array  the elements to be inserted into this deque
	 * @param offset the index of the first item in array to add
	 * @param length how many items, at most, to add from array into this
	 * @return {@code true} if this deque changed as a result of the call
	 * @see #addAll(short[])
	 */
	public boolean addAll(int index, short[] array, int offset, int length) {
		int oldSize = size;
		if (index <= 0)
			addAllFirst(array, offset, length);
		else if (index >= oldSize)
			addAll(array, offset, length);
		else {
			final int cs = Math.min(array.length - offset, length);
			if (cs <= 0) return false;
			int place = ensureGap(index, cs);
			System.arraycopy(array, offset, this.items, place, cs);
			size += cs;
		}
		return oldSize != size;
	}

	/**
	 * Exactly like {@link #addAll(OfShort)}, but takes an Ordered.OfShort instead of a PrimitiveCollection.OfShort.
	 *
	 * @param ord the elements to be inserted into this deque
	 * @return {@code true} if this deque changed as a result of the call
	 * @see #addAll(OfShort)
	 */
	public boolean addAll(Ordered.OfShort ord) {
		return addAll(size, ord, 0, ord.size());
	}

	/**
	 * Like {@link #addAll(short[])}, but only uses at most {@code length} items from {@code ord}, starting at {@code offset}.
	 *
	 * @param ord    the elements to be inserted into this deque
	 * @param offset the index of the first item in ord to add
	 * @param length how many items, at most, to add from ord into this
	 * @return {@code true} if this deque changed as a result of the call
	 * @see #addAll(short[])
	 */
	public boolean addAll(Ordered.OfShort ord, int offset, int length) {
		return addAll(size, ord, offset, length);
	}

	/**
	 * Like {@link #addAll(int, OfShort)}, but takes an ord instead of a PrimitiveCollection.OfShort and inserts it
	 * so the first item will be at the given {@code index}.
	 * The order of {@code ord} will be preserved, starting at the given index in this deque.
	 *
	 * @param index the index in this deque's iteration order to place the first item in {@code ord}
	 * @param ord   the elements to be inserted into this deque
	 * @return {@code true} if this deque changed as a result of the call
	 * @see #addAll(Ordered.OfShort)
	 */
	public boolean addAll(int index, Ordered.OfShort ord) {
		return addAll(index, ord, 0, ord.size());
	}

	/**
	 * Like {@link #addAll(int, OfShort)}, but takes an array instead of a PrimitiveCollection.OfShort, gets items starting at
	 * {@code offset} from that array, using {@code length} items, and inserts them
	 * so the item at the given offset will be at the given {@code index}.
	 * The order of {@code array} will be preserved, starting at the given index in this deque.
	 *
	 * @param index  the index in this deque's iteration order to place the first item in {@code array}
	 * @param ord    the elements to be inserted into this deque
	 * @param offset the index of the first item in array to add
	 * @param length how many items, at most, to add from array into this
	 * @return {@code true} if this deque changed as a result of the call
	 * @see #addAll(Ordered.OfShort)
	 */
	public boolean addAll(int index, Ordered.OfShort ord, int offset, int length) {
		final int cs = Math.min(ord.size() - offset, length);
		if (cs <= 0) return false;
		int place = ensureGap(index, cs);
		ShortList er = ord.order();
		for (int i = offset, n = offset + cs; i < n; i++) {
			items[place++] = er.get(i);
			if (place == items.length) place = 0;
		}
		size += cs;
		return true;
	}

	/**
	 * An alias for {@link #addAll(Ordered.OfShort)}.
	 *
	 * @param ord the elements to be inserted into this deque
	 * @return {@code true} if this deque changed as a result of the call
	 * @see #addAll(Ordered.OfShort)
	 */
	public boolean addAllLast(Ordered.OfShort ord) {
		return addAll(size, ord, 0, ord.size());
	}

	/**
	 * An alias for {@link #addAll(Ordered.OfShort, int, int)}.
	 *
	 * @param ord    the elements to be inserted into this deque
	 * @param offset the index of the first item in ord to add
	 * @param length how many items, at most, to add from ord into this
	 * @return {@code true} if this deque changed as a result of the call
	 * @see #addAll(Ordered.OfShort, int, int)
	 */
	public boolean addAllLast(Ordered.OfShort ord, int offset, int length) {
		return addAll(size, ord, offset, length);
	}

	/**
	 * Exactly like {@link #addAllFirst(OfShort)}, but takes an ord instead of a PrimitiveCollection.OfShort.
	 *
	 * @param ord the elements to be inserted into this deque
	 * @return {@code true} if this deque changed as a result of the call
	 * @see #addAllFirst(OfShort)
	 */
	public boolean addAllFirst(Ordered.OfShort ord) {
		return addAll(0, ord, 0, ord.size());
	}

	/**
	 * Like {@link #addAllFirst(Ordered.OfShort)}, but only uses at most {@code length} items from {@code ord}, starting at
	 * {@code offset}. The order of {@code ord} will be preserved, starting at the head of the deque.
	 *
	 * @param ord    the elements to be inserted into this deque
	 * @param offset the index of the first item in ord to add
	 * @param length how many items, at most, to add from ord into this
	 * @return {@code true} if this deque changed as a result of the call
	 * @see #addAllFirst(Ordered.OfShort)
	 */
	public boolean addAllFirst(Ordered.OfShort ord, int offset, int length) {
		return addAll(0, ord, offset, length);
	}

	/**
	 * Alias for {@link #addAll(int, Ordered.OfShort)}.
	 *
	 * @param index the index in this deque's iteration order to place the first item in {@code ord}
	 * @param ord   the elements to be inserted into this deque
	 * @return {@code true} if this deque changed as a result of the call
	 */
	public boolean insertAll(int index, Ordered.OfShort ord) {
		return addAll(index, ord, 0, ord.size());
	}

	/**
	 * Alias for {@link #addAll(int, Ordered.OfShort, int, int)}.
	 *
	 * @param index  the index in this deque's iteration order to place the first item in {@code ord}
	 * @param ord    the elements to be inserted into this deque
	 * @param offset the index of the first item in ord to add
	 * @param length how many items, at most, to add from ord into this
	 * @return {@code true} if this deque changed as a result of the call
	 */
	public boolean insertAll(int index, Ordered.OfShort ord, int offset, int length) {
		return addAll(index, ord, offset, length);
	}

	/**
	 * Pushes an element onto the stack represented by this deque (in other
	 * words, at the head of this deque).
	 *
	 * <p>This method is equivalent to {@link #addFirst}.
	 *
	 * @param t the element to push
	 */
	public void push(short t) {
		addFirst(t);
	}

	/**
	 * Pops an element from the stack represented by this deque.  In other
	 * words, removes and returns the first element of this deque.
	 *
	 * <p>This method is equivalent to {@link #removeFirst()}.
	 *
	 * @return the element at the front of this deque (which is the top
	 * of the stack represented by this deque)
	 * @throws NoSuchElementException if this deque is empty
	 */
	public short pop() {
		return removeFirst();
	}

	/**
	 * Removes the first occurrence of the specified element from this deque.
	 * If the deque does not contain the element, it is unchanged.
	 * More formally, removes the first element {@code e} such that
	 * {@code o == e} (if such an element exists).
	 * Returns {@code true} if this deque contained the specified element
	 * (or equivalently, if this deque changed as a result of the call).
	 *
	 * <p>This method is equivalent to {@link #removeFirstOccurrence(short)}.
	 *
	 * @param o element to be removed from this deque, if present
	 * @return {@code true} if an element was removed as a result of this call
	 */
	public boolean remove(short o) {
		return removeFirstOccurrence(o);
	}

	/**
	 * Returns {@code true} if this deque contains the specified element.
	 * More formally, returns {@code true} if and only if this deque contains
	 * at least one element {@code e} such that {@code o == e}.
	 *
	 * @param o element whose presence in this deque is to be tested
	 * @return {@code true} if this deque contains the specified element
	 */
	public boolean contains(short o) {
		return indexOf(o, 0) != -1;
	}

	@Override
	public boolean containsAll(ShortList other) {
		return containsAll(other.iterator());
	}

	/**
	 * Returns the number of elements in this deque.
	 *
	 * @return the number of elements in this deque
	 */
	public int size() {
		return size;
	}

	/**
	 * Returns an array containing all the elements in this collection.
	 * If this collection makes any guarantees as to what order its elements
	 * are returned by its iterator, this method must return the elements in
	 * the same order. The returned array's {@linkplain Class#getComponentType
	 * runtime component type} is {@code short}.
	 *
	 * <p>The returned array will be "safe" in that no references to it are
	 * maintained by this collection.  (In other words, this method must
	 * allocate a new array even if this collection is backed by an array).
	 * The caller is thus free to modify the returned array.
	 *
	 * @return an array, whose {@linkplain Class#getComponentType runtime component
	 * type} is {@code short}, containing all the elements in this collection
	 */
	public short @NonNull [] toArray() {
		short[] next = new short[size];
		if (head <= tail) {
			System.arraycopy(items, head, next, 0, tail - head + 1);
		} else {
			System.arraycopy(items, head, next, 0, size - head);
			System.arraycopy(items, 0, next, size - head, tail + 1);
		}
		return next;
	}

	@Override
	public short[] toArray(short[] array) {
		if (array.length < size)
			array = new short[size];
		if (head <= tail) {
			System.arraycopy(items, head, array, 0, tail - head + 1);
		} else {
			System.arraycopy(items, head, array, 0, size - head);
			System.arraycopy(items, 0, array, size - head, tail + 1);
		}
		return array;
	}

	@Override
	public short[] setSize(int newSize) {
		if (newSize < 0) clear();
		else if (newSize > items.length) resize(Math.max(8, newSize));
		else truncate(newSize);
		return items;
	}

	/**
	 * Alias for {@link #truncate(int)}.
	 *
	 * @param newSize the size this deque should have after this call completes, if smaller than the current size
	 */
	public void truncateLast(int newSize) {
		truncate(newSize);
	}

	/**
	 * Reduces the size of the deque to the specified size by bulk-removing items from the tail end.
	 * If the deque is already smaller than the specified size, no action is taken.
	 *
	 * @param newSize the size this deque should have after this call completes, if smaller than the current size
	 */
	public void truncate(int newSize) {
		if (newSize <= 0) {
			clear();
			return;
		}
		int oldSize = size;
		if (oldSize > newSize) {
			if (head <= tail) {
				// only removing from tail, near the end, toward head, near the start
				tail -= oldSize - newSize;
				size = newSize;
			} else if (head + newSize < items.length) {
				// tail is near the start, but we have to remove elements through the start and into the back
				tail = head + newSize;
				size = newSize;
			} else {
				// tail is near the start, but we only have to remove some elements between tail and the start
				tail -= (oldSize - newSize);
				size = newSize;
			}
		}
	}

	/**
	 * Reduces the size of the deque to the specified size by bulk-removing from the head.
	 * If the deque is already smaller than the specified size, no action is taken.
	 *
	 * @param newSize the size this deque should have after this call completes, if smaller than the current size
	 */
	public void truncateFirst(int newSize) {
		if (newSize <= 0) {
			clear();
			return;
		}
		int oldSize = size;
		if (oldSize > newSize) {
			if (head <= tail || head + oldSize - newSize < items.length) {
				// only removing from head to head + newSize, which is contiguous
				head += oldSize - newSize;
				if (head >= items.length) head -= items.length;
				size = newSize;
			} else {
				// tail is near the start, and we are removing from head to the end and then part near start
				head = tail + 1 - newSize;
				size = newSize;
			}
		}
	}

	/**
	 * Removes from this list all the elements whose index is between
	 * {@code fromIndex}, inclusive, and {@code toIndex}, exclusive.
	 * Shifts any succeeding elements to the left (reduces their index).
	 * This call shrinks the list by {@code (toIndex - fromIndex)} elements.
	 * If {@code toIndex==fromIndex}, this operation has no effect.
	 * If {@code fromIndex} is 0 or less, this delegates to {@link #truncateFirst(int)};
	 * if {@code toIndex} is equal to or greater than the
	 * size of this collection, this delegates to {@link #truncate(int)}.
	 * <br>
	 * This is public here, not protected as in most JDK collections, because there are
	 * actually sometimes needs for this in user code.
	 *
	 * @param fromIndex index of first element to be removed (inclusive)
	 * @param toIndex   index after last element to be removed (exclusive)
	 */
	public void removeRange(int fromIndex, int toIndex) {
		if (fromIndex <= 0) {
			truncateFirst(size - toIndex);
			return;
		}
		if (toIndex >= size) {
			truncate(fromIndex);
			return;
		}
		if (fromIndex < toIndex) {
			int removedCount = toIndex - fromIndex;
			if (head <= tail) {
				// tail is near the end, head is near the start
				int tailMinusTo = tail + 1 - (head + toIndex);
				if (tailMinusTo < 0) tailMinusTo += items.length;
				System.arraycopy(items, head + toIndex, items, head + fromIndex, tailMinusTo);
				tail -= removedCount;
				size -= removedCount;
			} else if (head + toIndex < items.length) {
				// head is at the end, and tail wraps around, but we are only removing items between head and end
				int headPlusFrom = head + fromIndex;
				if (headPlusFrom >= items.length) headPlusFrom -= items.length;
				System.arraycopy(items, head, items, headPlusFrom, removedCount);
				head += removedCount;
				size -= removedCount;
			} else if (head + toIndex - items.length - removedCount >= 0) {
				// head is at the end, and tail wraps around, but we are only removing items between start and tail
				System.arraycopy(items, head + toIndex - items.length, items, head + fromIndex - items.length, tail + 1 - (head + toIndex - items.length));
				tail -= removedCount;
				size -= removedCount;
			} else {
				// head is at the end, tail wraps around, and we must remove items that wrap from end to start
				System.arraycopy(items, head, items, items.length - fromIndex, fromIndex);
				System.arraycopy(items, head + toIndex - items.length, items, 0, tail + 1 - (head + toIndex - items.length));
				tail -= (head + toIndex - items.length);
				head = (items.length - fromIndex);
				size -= removedCount;
			}
		}
	}

	/**
	 * Returns the index of the first occurrence of value in the deque, or -1 if no such value exists.
	 *
	 * @param value the short to look for
	 * @return An index of the first occurrence of value in the deque or -1 if no such value exists
	 */
	public int indexOf(short value) {
		return indexOf(value, 0);
	}

	/**
	 * Returns the index of the first occurrence of value in the deque, or -1 if no such value exists.
	 * This returns {@code fromIndex} if {@code value} is present at that point,
	 * so if you chain calls to indexOf(), the subsequent fromIndex should be larger than the last-returned index.
	 *
	 * @param value     the short to look for
	 * @param fromIndex the initial index to check (zero-indexed, starts at the head, inclusive)
	 * @return An index of first occurrence of value at or after fromIndex in the deque, or -1 if no such value exists
	 */
	public int indexOf(short value, int fromIndex) {
		if (size == 0)
			return -1;
		short[] items = this.items;
		final int head = this.head, tail = this.tail;
		int i = head + Math.min(Math.max(fromIndex, 0), size - 1);
		if (i >= items.length)
			i -= items.length;

		if (head <= tail) {
			for (; i <= tail; i++)
				if (items[i] == value)
					return i - head;
		} else {
			for (int n = items.length; i < n; i++)
				if (items[i] == value)
					return i - head;
			for (i = 0; i <= tail; i++)
				if (items[i] == value)
					return i + items.length - head;
		}
		return -1;
	}

	/**
	 * Returns the index of the last occurrence of value in the deque, or -1 if no such value exists.
	 *
	 * @param value the short to look for
	 * @return An index of the last occurrence of value in the deque or -1 if no such value exists
	 */
	public int lastIndexOf(short value) {
		return lastIndexOf(value, size - 1);
	}

	/**
	 * Returns the index of last occurrence of {@code value} in the deque, starting from {@code fromIndex} and going
	 * backwards, or -1 if no such value exists. This returns {@code fromIndex} if {@code value} is present at that
	 * point, so if you chain calls to indexOf(), the subsequent fromIndex should be smaller than the last-returned
	 * index.
	 *
	 * @param value     the short to look for
	 * @param fromIndex the initial index to check (zero-indexed, starts at the head, inclusive)
	 * @return An index of last occurrence of value at or before fromIndex in the deque, or -1 if no such value exists
	 */
	public int lastIndexOf(short value, int fromIndex) {
		if (size == 0)
			return -1;
		short[] items = this.items;
		final int head = this.head, tail = this.tail;
		int i = head + Math.min(Math.max(fromIndex, 0), size - 1);
		if (i >= items.length)
			i -= items.length;
		else if (i < 0)
			i += items.length;


		if (head <= tail) {
			for (; i >= head; i--)
				if (items[i] == value)
					return i - head;
		} else {
			for (; i >= 0; i--)
				if (items[i] == value)
					return i + items.length - head;
			for (i = items.length - 1; i >= head; i--)
				if (items[i] == value)
					return i - head;
		}
		return -1;
	}

	public ShortListIterator listIterator() {
		if (iterator1 == null || iterator2 == null) {
			iterator1 = new ShortDequeIterator(this);
			iterator2 = new ShortDequeIterator(this);
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
	 * Gets an iterator over this deque that starts at the given index.
	 *
	 * @param index the index to start iterating from in this deque
	 * @return a reused iterator starting at the given index
	 */
	public ShortListIterator listIterator(int index) {
		if (iterator1 == null || iterator2 == null) {
			iterator1 = new ShortDequeIterator(this, index, false);
			iterator2 = new ShortDequeIterator(this, index, false);
		}
		if (!iterator1.valid) {
			iterator1.reset(index);
			iterator1.valid = true;
			iterator2.valid = false;
			return iterator1;
		}
		iterator2.reset(index);
		iterator2.valid = true;
		iterator1.valid = false;
		return iterator2;
	}

	/**
	 * Removes the first instance of the specified value in the deque.
	 *
	 * @param value the short to remove
	 * @return true if value was found and removed, false otherwise
	 */
	public boolean removeValue(short value) {
		int index = indexOf(value, 0);
		if (index == -1)
			return false;
		removeAt(index);
		return true;
	}

	/**
	 * Removes the last instance of the specified value in the deque.
	 *
	 * @param value the short to remove
	 * @return true if value was found and removed, false otherwise
	 */
	public boolean removeLastValue(short value) {
		int index = lastIndexOf(value);
		if (index == -1)
			return false;
		removeAt(index);
		return true;
	}

	/**
	 * Removes the element at the specified position in this deque.
	 * Shifts any subsequent elements to the left (subtracts one
	 * from their indices).  Returns the element that was removed from the
	 * deque.
	 *
	 * @param index the index of the element to be removed
	 * @return the element previously at the specified position
	 */
	public short removeAt(int index) {
		if (index <= 0)
			return removeFirst();
		if (index >= size)
			return removeLast();

		short[] items = this.items;
		int head = this.head, tail = this.tail;
		index += head;
		short value;
		if (head <= tail) { // index is between head and tail.
			value = items[index];
			System.arraycopy(items, index + 1, items, index, tail - index);
			this.tail--;
			if (this.tail == -1) this.tail = items.length - 1;
		} else if (index >= items.length) { // index is between 0 and tail.
			index -= items.length;
			value = items[index];
			System.arraycopy(items, index + 1, items, index, tail - index);
			this.tail--;
			if (this.tail == -1) this.tail = items.length - 1;
		} else { // index is between head and values.length.
			value = items[index];
			System.arraycopy(items, head, items, head + 1, index - head);
			this.head++;
			if (this.head == items.length) {
				this.head = 0;
			}
		}
		size--;
		return value;
	}

	/**
	 * Removes the element at the specified position in this deque.
	 * Shifts any subsequent elements to the left (subtracts one
	 * from their indices).  Returns the element that was removed from the
	 * deque, or {@link #getDefaultValue() the default value} if this is empty.
	 * This will not throw an Exception in normal usage, even if index is
	 * negative (which makes this simply return {@link #pollFirst()}) or greater
	 * than or equal to {@link #size()} (which makes this return {@link #pollLast()}).
	 * <br>
	 * This is an alias for {@link #poll(int)} for compatibility with primitive-backed lists and deques;
	 * {@link #poll(int)} can refer to the method that removes an item by value, not by index, in those types.
	 *
	 * @param index the index of the element to be removed
	 * @return the element previously at the specified position
	 */
	public short pollAt(int index) {
		return poll(index);
	}

	/**
	 * Removes the element at the specified position in this deque.
	 * Shifts any subsequent elements to the left (subtracts one
	 * from their indices). Returns the element that was removed from the
	 * deque, or {@link #getDefaultValue() the default value} if this is empty.
	 * This will not throw an Exception in normal usage, even if index is
	 * negative (which makes this simply return {@link #pollFirst()}) or greater
	 * than or equal to {@link #size()} (which makes this return {@link #pollLast()}).
	 *
	 * @param index the index of the element to be removed
	 * @return the element previously at the specified position
	 */
	public short poll(int index) {
		if (index <= 0)
			return pollFirst();
		if (index >= size)
			return pollLast();
		// No need to check for size to be 0 because the above checks will already do that, and one will run.

		short[] items = this.items;
		int head = this.head, tail = this.tail;
		index += head;
		short value;
		if (head <= tail) { // index is between head and tail.
			value = items[index];
			System.arraycopy(items, index + 1, items, index, tail - index);
			this.tail--;
			if (this.tail == -1) this.tail = items.length - 1;
		} else if (index >= items.length) { // index is between 0 and tail.
			index -= items.length;
			value = items[index];
			System.arraycopy(items, index + 1, items, index, tail - index);
			this.tail--;
			if (this.tail == -1) this.tail = items.length - 1;
		} else { // index is between head and values.length.
			value = items[index];
			System.arraycopy(items, head, items, head + 1, index - head);
			this.head++;
			if (this.head == items.length) {
				this.head = 0;
			}
		}
		size--;
		return value;
	}

	@Override
	public boolean removeAll(OfShort c) {
		return removeAll(c.iterator());
	}

	@Override
	public boolean removeEach(OfShort c) {
		return removeEach(c.iterator());
	}

	@Override
	public boolean retainAll(OfShort other) {
		// Gets the deque to be internally the same as a ShortList, if not already.
		if (head != 0) trimToSize();
		// That allows us to use the ShortList retainAll() verbatim.
		return super.retainAll(other);
	}

	/**
	 * Returns true if the deque has one or more items.
	 */
	public boolean notEmpty() {
		return size != 0;
	}

	/**
	 * Returns true if the deque is empty.
	 */
	public boolean isEmpty() {
		return size == 0;
	}

	/**
	 * Returns the first (head) item in the deque (without removing it).
	 *
	 * @throws NoSuchElementException when the deque is empty
	 * @see #peekFirst() peeking won't throw an exception, and will return the ShortDeque's default value if empty
	 * @see #removeFirst()
	 */
	public short first() {
		if (size == 0) {
			// Underflow
			throw new NoSuchElementException("ShortDeque is empty.");
		}
		return items[head];
	}

	/**
	 * Returns the last (tail) item in the deque (without removing it).
	 *
	 * @throws NoSuchElementException when the deque is empty
	 * @see #peekLast() peeking won't throw an exception, and will return the ShortDeque's default value if empty
	 */
	public short last() {
		if (size == 0) {
			// Underflow
			throw new NoSuchElementException("ShortDeque is empty.");
		}
		return items[tail];
	}

	/**
	 * Returns the element at the specified position in this deque.
	 * Like {@link ArrayList} or {@link ShortList}, but unlike {@link LinkedList}, this runs in O(1) time.
	 * It is expected to be slightly slower than {@link ShortList#get(int)}, which also runs in O(1) time.
	 * Unlike get() in ArrayList or ShortList, this considers negative indices to refer to the first item, and
	 * too-large indices to refer to the last item. That means it delegates to {@link #getFirst()} or
	 * {@link #getLast()} in those cases instead of throwing an {@link IndexOutOfBoundsException}, though it may
	 * throw a {@link NoSuchElementException} if the deque is empty and there is no item it can get.
	 *
	 * @param index index of the element to return
	 * @return the element at the specified position in this deque
	 * @throws NoSuchElementException if the deque is empty
	 */
	public short get(int index) {
		if (index <= 0)
			return getFirst();
		if (index >= size - 1)
			return getLast();
		final short[] items = this.items;

		int i = head + index;
		if (i >= items.length)
			i -= items.length;
		return items[i];
	}

	/**
	 * Returns the element at the specified position in this deque.
	 * Like {@link ArrayList} or {@link ShortList}, but unlike {@link LinkedList}, this runs in O(1) time.
	 * It is expected to be slightly slower than {@link ShortList#get(int)}, which also runs in O(1) time.
	 * Unlike get() in ArrayList or ShortList, this considers negative indices to refer to the first item, and
	 * too-large indices to refer to the last item. That means it delegates to {@link #peekFirst()} or
	 * {@link #peekLast()} in those cases instead of throwing an {@link IndexOutOfBoundsException}, and it will
	 * return {@link #getDefaultValue() the default value} if the deque is empty. Unless changed, the default value
	 * is usually {@code null}.
	 *
	 * @param index index of the element to return
	 * @return the element at the specified position in this deque
	 */
	public short peekAt(int index) {
		if (index <= 0)
			return peekFirst();
		if (index >= size - 1)
			return peekLast();
		final short[] items = this.items;

		int i = head + index;
		if (i >= items.length)
			i -= items.length;
		return items[i];
	}

	/**
	 * Replaces the element at the specified position in this list with the
	 * specified element. If this deque is empty or the index is larger than the largest index currently in this
	 * deque, this delegates to {@link #addLast(short)} and returns {@link #getDefaultValue() the default value}.
	 * If the index is negative, this delegates to {@link #addFirst(short)} and returns
	 * {@link #getDefaultValue() the default value}.
	 *
	 * @param index index of the element to replace
	 * @param item  element to be stored at the specified position
	 * @return the element previously at the specified position
	 */
	public short assign(int index, short item) {
		if (size <= 0 || index >= size) {
			addLast(item);
			return defaultValue;
		}
		if (index < 0) {
			addFirst(item);
			return defaultValue;
		}
		final short[] items = this.items;

		int i = head + Math.max(Math.min(index, size - 1), 0);
		if (i >= items.length)
			i -= items.length;
		short old = items[i];
		items[i] = item;
		return old;
	}

	/**
	 * Replaces the element at the specified position in this list with the
	 * specified element. If this deque is empty or the index is larger than the largest index currently in this
	 * deque, this delegates to {@link #addLast(short)} and returns {@link #getDefaultValue() the default value}.
	 * If the index is negative, this delegates to {@link #addFirst(short)} and returns
	 * {@link #getDefaultValue() the default value}.
	 *
	 * @param index index of the element to replace
	 * @param item  element to be stored at the specified position
	 */
	public void set(int index, short item) {
		if (size <= 0 || index >= size) {
			addLast(item);
			return;
		}
		if (index < 0) {
			addFirst(item);
			return;
		}
		final short[] items = this.items;

		int i = head + index;
		if (i >= items.length)
			i -= items.length;
		items[i] = item;
	}

	@Override
	public void plus(int index, short value) {
		final short[] items = this.items;

		int i = head + Math.min(Math.max(index, 0), size - 1);
		if (i >= items.length)
			i -= items.length;
		items[i] += value;
	}

	@Override
	public ShortList plus(short value) {
		final short[] items = this.items;
		if (head <= tail) {
			for (int i = head; i <= tail; i++) {
				items[i] += value;
			}
		} else {
			for (int i = head; i < items.length; i++) {
				items[i] += value;
			}
			for (int i = 0; i <= tail; i++) {
				items[i] += value;
			}
		}
		return this;
	}

	@Override
	public void times(int index, short value) {
		final short[] items = this.items;

		int i = head + Math.min(Math.max(index, 0), size - 1);
		if (i >= items.length)
			i -= items.length;
		items[i] *= value;
	}

	@Override
	public ShortList times(short value) {
		final short[] items = this.items;
		if (head <= tail) {
			for (int i = head; i <= tail; i++) {
				items[i] *= value;
			}
		} else {
			for (int i = head; i < items.length; i++) {
				items[i] *= value;
			}
			for (int i = 0; i <= tail; i++) {
				items[i] *= value;
			}
		}
		return this;
	}

	@Override
	public void minus(int index, short value) {
		final short[] items = this.items;

		int i = head + Math.min(Math.max(index, 0), size - 1);
		if (i >= items.length)
			i -= items.length;
		items[i] -= value;
	}

	@Override
	public ShortList minus(short value) {
		final short[] items = this.items;
		if (head <= tail) {
			for (int i = head; i <= tail; i++) {
				items[i] -= value;
			}
		} else {
			for (int i = head; i < items.length; i++) {
				items[i] -= value;
			}
			for (int i = 0; i <= tail; i++) {
				items[i] -= value;
			}
		}
		return this;
	}

	@Override
	public void div(int index, short value) {
		final short[] items = this.items;

		int i = head + Math.min(Math.max(index, 0), size - 1);
		if (i >= items.length)
			i -= items.length;
		items[i] /= value;
	}

	@Override
	public ShortList div(short value) {
		final short[] items = this.items;
		if (head <= tail) {
			for (int i = head; i <= tail; i++) {
				items[i] /= value;
			}
		} else {
			for (int i = head; i < items.length; i++) {
				items[i] /= value;
			}
			for (int i = 0; i <= tail; i++) {
				items[i] /= value;
			}
		}
		return this;
	}

	@Override
	public void rem(int index, short value) {
		final short[] items = this.items;

		int i = head + Math.min(Math.max(index, 0), size - 1);
		if (i >= items.length)
			i -= items.length;
		items[i] %= value;
	}

	@Override
	public ShortList rem(short value) {
		final short[] items = this.items;
		if (head <= tail) {
			for (int i = head; i <= tail; i++) {
				items[i] %= value;
			}
		} else {
			for (int i = head; i < items.length; i++) {
				items[i] %= value;
			}
			for (int i = 0; i <= tail; i++) {
				items[i] %= value;
			}
		}
		return this;
	}

	@Override
	public void replaceAll(ShortToShortFunction operator) {
		final short[] items = this.items;
		if (head <= tail) {
			for (int i = head; i <= tail; i++) {
				items[i] = operator.applyAsShort(items[i]);
			}
		} else {
			for (int i = head; i < items.length; i++) {
				items[i] = operator.applyAsShort(items[i]);
			}
			for (int i = 0; i <= tail; i++) {
				items[i] = operator.applyAsShort(items[i]);
			}
		}
	}

	/**
	 * Replaces the first occurrence of {@code find} with {@code replace}. Returns true if it performed the replacement,
	 * or false if there was nothing to replace. This also returns false if find and replace are the same.
	 * @param find the item to search for
	 * @param replace the item to replace {@code find} with, if possible
	 * @return true if this changed, or false otherwise
	 */
	@Override
	public boolean replaceFirst(short find, short replace) {
		if (find != replace) {
			short[] items = this.items;
			if (head <= tail) {
				for (int i = head, n = tail; i <= n; i++) {
					if (items[i] == find) {
						items[i] = replace;
						return true;
					}
				}
			} else {
				for (int i = head, n = items.length; i < n; i++) {
					if (items[i] == find) {
						items[i] = replace;
						return true;
					}
				}
				for (int i = 0, n = tail; i <= n; i++) {
					if (items[i] == find) {
						items[i] = replace;
						return true;
					}
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
	@Override
	public int replaceAll(short find, short replace) {
		int replacements = 0;
		if (find != replace) {
			short[] items = this.items;
			if (head <= tail) {
				for (int i = head, n = tail; i <= n; i++) {
					if (items[i] == find) {
						items[i] = replace;
						++replacements;
					}
				}
			} else {
				for (int i = head, n = items.length; i < n; i++) {
					if (items[i] == find) {
						items[i] = replace;
						++replacements;
					}
				}
				for (int i = 0, n = tail; i <= n; i++) {
					if (items[i] == find) {
						items[i] = replace;
						++replacements;
					}
				}
			}
		}
		return replacements;
	}

	/**
	 * Inserts the specified number of items at the specified index. The new items will have values equal to the values at those
	 * indices before the insertion, and the previous values will be pushed to after the duplicated range.
	 *
	 * @param index the first index to duplicate
	 * @param count how many items to duplicate
	 */
	@Override
	public boolean duplicateRange(int index, int count) {
		int place = ensureGap(index + count, count);
		if (place >= head + index + count) {
			System.arraycopy(items, head + index, items, place, count);
		} else {
			System.arraycopy(items, 0, items, count - place, place);
			System.arraycopy(items, head, items, place, count - place);
		}
		size += count;
		return count > 0;
	}

	/**
	 * Removes all values from this deque. This operates in O(1) time.
	 */
	public void clear() {
		if (size == 0)
			return;
		this.head = 0;
		this.tail = 0;
		this.size = 0;
	}

	/**
	 * Returns an iterator for the items in the deque. Remove is supported.
	 * <br>
	 * Reuses one of two iterators for this deque. For nested or multithreaded
	 * iteration, use {@link ShortDequeIterator#ShortDequeIterator(ShortDeque)}.
	 */
	public ShortListIterator iterator() {
		if (iterator1 == null || iterator2 == null) {
			iterator1 = new ShortDequeIterator(this);
			iterator2 = new ShortDequeIterator(this);
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
	 * Returns an iterator over the elements in this deque in reverse
	 * sequential order. The elements will be returned in order from
	 * last (tail) to first (head).
	 * <br>
	 * Reuses one of two descending iterators for this deque. For nested or multithreaded
	 * iteration, use {@link ShortDequeIterator#ShortDequeIterator(ShortDeque, boolean)}.
	 *
	 * @return an iterator over the elements in this deque in reverse sequence
	 */
	public ShortListIterator descendingIterator() {
		if (descendingIterator1 == null || descendingIterator2 == null) {
			descendingIterator1 = new ShortDequeIterator(this, true);
			descendingIterator2 = new ShortDequeIterator(this, true);
		}
		if (!descendingIterator1.valid) {
			descendingIterator1.reset();
			descendingIterator1.valid = true;
			descendingIterator2.valid = false;
			return descendingIterator1;
		}
		descendingIterator2.reset();
		descendingIterator2.valid = true;
		descendingIterator1.valid = false;
		return descendingIterator2;
	}

	/**
	 * Returns an iterator over the elements in this deque in reverse
	 * sequential order. The elements will be returned in order from
	 * {@code index} backwards to first (head).
	 * <br>
	 * Reuses one of two descending iterators for this deque. For nested or multithreaded
	 * iteration, use {@link ShortDequeIterator#ShortDequeIterator(ShortDeque, boolean)}.
	 *
	 * @param index the index to start iterating from in this deque
	 * @return an iterator over the elements in this deque in reverse sequence
	 */
	public ShortListIterator descendingIterator(int index) {
		if (descendingIterator1 == null || descendingIterator2 == null) {
			descendingIterator1 = new ShortDequeIterator(this, index, true);
			descendingIterator2 = new ShortDequeIterator(this, index, true);
		}
		if (!descendingIterator1.valid) {
			descendingIterator1.reset(index);
			descendingIterator1.valid = true;
			descendingIterator2.valid = false;
			return descendingIterator1;
		}
		descendingIterator2.reset(index);
		descendingIterator2.valid = true;
		descendingIterator1.valid = false;
		return descendingIterator2;
	}

	/**
	 * Delegates to {@link #toString(String, boolean)} with a delimiter of {@code ", "} and square brackets enabled.
	 *
	 * @return the square-bracketed String representation of this ShortDeque, with items separated by ", "
	 */
	public String toString() {
		return toString(", ", true);
	}

	public int hashCode() {
		final int size = this.size;
		final short[] items = this.items;
		final int backingLength = items.length;
		int index = this.head;

		int hash = size + 1;
		for (int s = 0; s < size; s++) {
			final short value = items[index];

			hash *= 43; // avoids LEA pessimization
			hash ^= value; // avoids precision loss on GWT

			index++;
			if (index == backingLength)
				index = 0;
		}

		return hash;
	}

	/**
	 * Using {@code ==} between each item in order, compares for equality with
	 * other subtypes of {@link ShortList}.
	 * If {@code o} is not a ShortList
	 * (and is also not somehow reference-equivalent to this collection), this returns false.
	 * This uses the {@link OfShort#iterator()} of both this and {@code o},
	 * so if either is in the
	 * middle of a concurrent iteration that modifies the collection, this may fail.
	 *
	 * @param o object to be compared for equality with this collection
	 * @return true if this is equal to o, or false otherwise
	 */
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!((o instanceof ShortList)))
			return false;

		ShortIterator e1 = iterator();
		ShortIterator e2 = ((ShortList) o).iterator();
		while (e1.hasNext() && e2.hasNext()) {
			short o1 = e1.nextShort();
			short o2 = e2.nextShort();
			if (o1 != o2)
				return false;
		}
		return !(e1.hasNext() || e2.hasNext());
	}

	/**
	 * Switches the ordering of positions {@code first} and {@code second}, without changing any items beyond that.
	 *
	 * @param first  the first position, must not be negative and must be less than {@link #size()}
	 * @param second the second position, must not be negative and must be less than {@link #size()}
	 */
	public void swap(int first, int second) {
		if (first < 0)
			throw new IndexOutOfBoundsException("first index can't be < 0: " + first);
		if (first >= size)
			throw new IndexOutOfBoundsException("first index can't be >= size: " + first + " >= " + size);
		if (second < 0)
			throw new IndexOutOfBoundsException("second index can't be < 0: " + second);
		if (second >= size)
			throw new IndexOutOfBoundsException("second index can't be >= size: " + second + " >= " + size);
		if (first == second) return;
		final short[] items = this.items;

		int f = head + first;
		if (f >= items.length)
			f -= items.length;

		int s = head + second;
		if (s >= items.length)
			s -= items.length;

		short fv = items[f];
		items[f] = items[s];
		items[s] = fv;
	}

	/**
	 * Reverses this ShortDeque in-place.
	 */
	public void reverse() {
		final short[] items = this.items;
		int f, s, len = items.length;
		short fv;
		for (int n = size >> 1, b = 0, t = size - 1; b <= n && b != t; b++, t--) {
			f = head + b;
			if (f >= len)
				f -= len;
			s = head + t;
			if (s >= len)
				s -= len;
			fv = items[f];
			items[f] = items[s];
			items[s] = fv;
		}
	}

	public void shuffle(Random rng) {
		for (int i = size() - 1; i > 0; i--) {
			int r = rng.nextInt(i + 1);
			if (r != i)
				set(i, assign(r, get(i)));
		}
	}

	/**
	 * Sorts this deque in-place using {@link Arrays#sort(short[], int, int)} in ascending order.
	 */
	@Override
	public void sort() {
		sort(0, size);
	}

	/**
	 * Uses {@link Arrays#sort(short[], int, int)} to sort a (clamped) subrange of this deque.
	 *
	 * @param from first index to use, inclusive
	 * @param to   last index to use, exclusive
	 */
	@Override
	public void sort(int from, int to) {
		from = Math.max(Math.min(from, size - 1), 0);
		to = Math.max(Math.min(to, size), from);
		if (head + to <= items.length) {
			Arrays.sort(items, head + from, head + to);
		} else if (head + from >= items.length) {
			Arrays.sort(items, head + from - items.length, head + to - items.length);
		} else {
			trimToSize(); // rearranges items so it is linear starting at 0
			Arrays.sort(items, from, to);
		}
	}

	/**
	 * Sorts this deque in-place using {@link ShortComparators#sort(short[], int, int, ShortComparator)}.
	 * This should operate in O(n log(n)) time or less when the internals of the deque are
	 * continuous (the head is before the tail in the array). If the internals are not
	 * continuous, this takes an additional O(n) step (where n is less than the size of
	 * the deque) to rearrange the internals before sorting. You can pass null as the value
	 * for {@code comparator}, which will make this
	 * use the natural ordering for short.
	 *
	 * @param comparator the Comparator to use for short items; may be null to use the natural
	 *                   order of short items when short implements Comparable of short
	 */
	@Override
	public void sort(@Nullable ShortComparator comparator) {
		if (comparator == null) {
			sort();
		} else if (head <= tail) {
			ShortComparators.sort(items, head, tail + 1, comparator);
		} else {
			System.arraycopy(items, head, items, tail + 1, items.length - head);
			ShortComparators.sort(items, 0, tail + 1 + items.length - head, comparator);
			tail = (size > 0) ? size - 1 : 0;
			head = 0;
		}
	}

	@Override
	public void sort(int from, int to, @Nullable ShortComparator comparator) {
		from = Math.max(Math.min(from, size - 1), 0);
		to = Math.max(Math.min(to, size), from);
		if (comparator == null) {
			sort(from, to);
		} else if (head + to <= items.length) {
			ShortComparators.sort(items, head + from, head + to, comparator);
		} else if (head + from >= items.length) {
			ShortComparators.sort(items, head + from - items.length, head + to - items.length, comparator);
		} else {
			trimToSize(); // rearranges items so it is linear starting at 0
			ShortComparators.sort(items, from, to, comparator);
		}
	}

	/**
	 * Gets a randomly selected item from this ShortDeque, using the given random number generator.
	 * Throws a {@link NoSuchElementException} if empty.
	 *
	 * @param random any Random or subclass of it, such as {@link com.github.tommyettinger.digital.AlternateRandom}.
	 * @return a randomly selected item from this deque
	 */
	public short getRandom(Random random) {
		if (size <= 0) {
			throw new NoSuchElementException("ShortDeque is empty.");
		}
		return get(random.nextInt(size));
	}

	/**
	 * Gets a randomly selected item from this ShortDeque, using the given random number generator.
	 *
	 * @param random any Random or subclass of it, such as {@link com.github.tommyettinger.digital.AlternateRandom}.
	 * @return a randomly selected item from this deque, or the default value if empty
	 */
	public short random(Random random) {
		return peekAt(random.nextInt(size));
	}

	/**
	 * A {@link ShortIterator} over the elements of a ShortDeque.
	 */
	public static class ShortDequeIterator extends ShortListIterator implements ShortIterator {
		protected int index, latest = -1;
		protected boolean valid = true;
		protected final int direction;

		public ShortDequeIterator(ShortDeque deque) {
			this(deque, false);
		}

		public ShortDequeIterator(ShortDeque deque, boolean descendingOrder) {
			super(deque);
			direction = descendingOrder ? -1 : 1;
		}

		public ShortDequeIterator(ShortDeque deque, int index, boolean descendingOrder) {
			super(deque, index);
			direction = descendingOrder ? -1 : 1;
		}

		/**
		 * Returns the next {@code short} element in the iteration.
		 *
		 * @return the next {@code short} element in the iteration
		 * @throws NoSuchElementException if the iteration has no more elements
		 */
		@Override
		public short nextShort() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}
			latest = index;
			index += direction;
			return list.get(latest);
		}

		/**
		 * Returns {@code true} if the iteration has more elements.
		 * (In other words, returns {@code true} if {@link #next} would
		 * return an element rather than throwing an exception.)
		 *
		 * @return {@code true} if the iteration has more elements
		 */
		@Override
		public boolean hasNext() {
			if (!valid) {
				throw new RuntimeException("#iterator() cannot be used nested.");
			}
			return direction == 1 ? index < list.size() : index > 0 && list.notEmpty();
		}

		/**
		 * Returns {@code true} if this list iterator has more elements when
		 * traversing the list in the reverse direction.  (In other words,
		 * returns {@code true} if {@link #previous} would return an element
		 * rather than throwing an exception.)
		 *
		 * @return {@code true} if the list iterator has more elements when
		 * traversing the list in the reverse direction
		 */
		public boolean hasPrevious() {
			if (!valid) {
				throw new RuntimeException("#iterator() cannot be used nested.");
			}
			return direction == -1 ? index < list.size() : index > 0 && list.notEmpty();
		}

		/**
		 * Returns the previous element in the list and moves the cursor
		 * position backwards.  This method may be called repeatedly to
		 * iterate through the list backwards, or intermixed with calls to
		 * {@link #next} to go back and forth.  (Note that alternating calls
		 * to {@code next} and {@code previous} will return the same
		 * element repeatedly.)
		 *
		 * @return the previous element in the list
		 * @throws NoSuchElementException if the iteration has no previous
		 *                                element
		 */
		public short previous() {
			if (!hasPrevious()) {
				throw new NoSuchElementException();
			}
			latest = index -= direction;
			return list.get(latest);

		}

		/**
		 * Returns the index of the element that would be returned by a
		 * subsequent call to {@link #next}. (Returns list size if the list
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
		 * subsequent call to {@link #previous}. (Returns -1 if the list
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
		 * #next} or {@link #previous} (optional operation).  This call can
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
		 * Replaces the last element returned by {@link #next} or
		 * {@link #previous} with the specified element (optional operation).
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
		public void set(short t) {
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
		 * would be returned by {@link #next}, if any, and after the element
		 * that would be returned by {@link #previous}, if any.  (If the
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
		public void add(short t) {
			if (!valid) {
				throw new RuntimeException("#iterator() cannot be used nested.");
			}
			if (index > list.size()) {
				throw new NoSuchElementException();
			}
			list.insert(index, t);
			index += direction;
			latest = -1;
		}

		public void reset() {
			index = list.size() - 1 & direction >> 31;
			latest = -1;
		}

		public void reset(int index) {
			if (index < 0 || index >= list.size())
				throw new IndexOutOfBoundsException("ShortDequeIterator does not satisfy index >= 0 && index < deque.size()");
			this.index = index;
			latest = -1;
		}

		/**
		 * Returns an iterator over elements of type {@code short}.
		 *
		 * @return a ShortIterator; really this same ShortDequeIterator.
		 */
		public ShortDequeIterator iterator() {
			return this;
		}
	}

	/**
	 * Constructs an empty deque.
	 * This is usually less useful than just using the constructor, but can be handy
	 * in some code-generation scenarios when you don't know how many arguments you will have.
	 *
	 * @return a new deque containing nothing
	 */
	public static ShortDeque with() {
		return new ShortDeque(1);
	}

	/**
	 * Creates a new ShortDeque that holds only the given item, but can be resized.
	 *
	 * @param item one short item
	 * @return a new ShortDeque that holds the given item
	 */
	public static ShortDeque with(short item) {
		ShortDeque deque = new ShortDeque(1);
		deque.add(item);
		return deque;
	}

	/**
	 * Creates a new ShortDeque that holds only the given items, but can be resized.
	 *
	 * @param item0 a short item
	 * @param item1 a short item
	 * @return a new ShortDeque that holds the given items
	 */
	public static ShortDeque with(short item0, short item1) {
		ShortDeque deque = new ShortDeque(2);
		deque.add(item0, item1);
		return deque;
	}

	/**
	 * Creates a new ShortDeque that holds only the given items, but can be resized.
	 *
	 * @param item0 a short item
	 * @param item1 a short item
	 * @param item2 a short item
	 * @return a new ShortDeque that holds the given items
	 */
	public static ShortDeque with(short item0, short item1, short item2) {
		ShortDeque deque = new ShortDeque(3);
		deque.add(item0, item1, item2);
		return deque;
	}

	/**
	 * Creates a new ShortDeque that holds only the given items, but can be resized.
	 *
	 * @param item0 a short item
	 * @param item1 a short item
	 * @param item2 a short item
	 * @param item3 a short item
	 * @return a new ShortDeque that holds the given items
	 */
	public static ShortDeque with(short item0, short item1, short item2, short item3) {
		ShortDeque deque = new ShortDeque(4);
		deque.add(item0, item1, item2, item3);
		return deque;
	}

	/**
	 * Creates a new ShortDeque that holds only the given items, but can be resized.
	 *
	 * @param item0 a short item
	 * @param item1 a short item
	 * @param item2 a short item
	 * @param item3 a short item
	 * @param item4 a short item
	 * @return a new ShortDeque that holds the given items
	 */
	public static ShortDeque with(short item0, short item1, short item2, short item3, short item4) {
		ShortDeque deque = new ShortDeque(5);
		deque.add(item0, item1, item2, item3);
		deque.add(item4);
		return deque;
	}

	/**
	 * Creates a new ShortDeque that holds only the given items, but can be resized.
	 *
	 * @param item0 a short item
	 * @param item1 a short item
	 * @param item2 a short item
	 * @param item3 a short item
	 * @param item4 a short item
	 * @param item5 a short item
	 * @return a new ShortDeque that holds the given items
	 */
	public static ShortDeque with(short item0, short item1, short item2, short item3, short item4, short item5) {
		ShortDeque deque = new ShortDeque(6);
		deque.add(item0, item1, item2, item3);
		deque.add(item4, item5);
		return deque;
	}

	/**
	 * Creates a new ShortDeque that holds only the given items, but can be resized.
	 *
	 * @param item0 a short item
	 * @param item1 a short item
	 * @param item2 a short item
	 * @param item3 a short item
	 * @param item4 a short item
	 * @param item5 a short item
	 * @param item6 a short item
	 * @return a new ShortDeque that holds the given items
	 */
	public static ShortDeque with(short item0, short item1, short item2, short item3, short item4, short item5, short item6) {
		ShortDeque deque = new ShortDeque(7);
		deque.add(item0, item1, item2, item3);
		deque.add(item4, item5, item6);
		return deque;
	}

	/**
	 * Creates a new ShortDeque that holds only the given items, but can be resized.
	 *
	 * @param item0 a short item
	 * @param item1 a short item
	 * @param item2 a short item
	 * @param item3 a short item
	 * @param item4 a short item
	 * @param item5 a short item
	 * @param item6 a short item
	 * @param item7 a short item
	 * @return a new ShortDeque that holds the given items
	 */
	public static ShortDeque with(short item0, short item1, short item2, short item3, short item4, short item5, short item6, short item7) {
		ShortDeque deque = new ShortDeque(8);
		deque.add(item0, item1, item2, item3);
		deque.add(item4, item5, item6, item7);
		return deque;
	}

	/**
	 * Creates a new ShortDeque that will hold the items in the given array or varargs.
	 * This overload will only be used when a short array is supplied, or if varargs are used and
	 * there are 9 or more arguments.
	 *
	 * @param varargs either 0 or more short items, or an array of short
	 * @return a new ShortDeque that holds the given short items
	 */
	public static ShortDeque with(short... varargs) {
		return new ShortDeque(varargs);
	}
}

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

import com.github.tommyettinger.ds.support.sort.ShortComparator;
import com.github.tommyettinger.ds.support.sort.ShortComparators;
import com.github.tommyettinger.ds.support.util.ShortIterator;

import org.checkerframework.checker.nullness.qual.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Random;

/**
 * A resizable, insertion-ordered double-ended queue of shorts with efficient add and remove at the beginning and end. Values in the
 * backing array may wrap back to the beginning, making add and remove at the beginning and end O(1) (unless the backing array needs to
 * resize when adding). Deque functionality is provided via {@link #removeLast()} and {@link #addFirst(short)}.
 * <br>
 * Unlike most Deque implementations in the JDK, you can get and set items anywhere in the deque in constant time with {@link #get(int)}
 * and {@link #set(int, short)}.
 */
public class ShortDeque implements PrimitiveCollection.OfShort, Arrangeable {

	protected short defaultValue = -1;

	/**
	 * Contains the values in the queue. Head and tail indices go in a circle around this array, wrapping at the end.
	 */
	protected short[] values;

	/**
	 * Index of first element. Logically smaller than tail. Unless empty, it points to a valid element inside queue.
	 */
	protected int head = 0;

	/**
	 * Index of last element. Logically bigger than head. Usually points to an empty position, but points to the head when full
	 * (size == values.length).
	 */
	protected int tail = 0;

	/**
	 * Number of elements in the queue.
	 */
	public int size = 0;

	protected transient @Nullable ShortDequeIterator iterator1;
	protected transient @Nullable ShortDequeIterator iterator2;

	protected transient @Nullable ShortDequeIterator descendingIterator1;
	protected transient @Nullable ShortDequeIterator descendingIterator2;

	/**
	 * Creates a new ShortDeque which can hold 16 values without needing to resize backing array.
	 */
	public ShortDeque () {
		this(16);
	}

	/**
	 * Creates a new ShortDeque which can hold the specified number of values without needing to resize backing array.
	 */
	public ShortDeque (int initialSize) {
		this.values = new short[initialSize];
	}

	/**
	 * Creates a new ShortDeque using all of the contents of the given PrimitiveCollection.OfShort, such as
	 * a {@link ShortList}.
	 *
	 * @param coll a PrimitiveCollection.OfShort that will be copied into this and used in full
	 */
	public ShortDeque (PrimitiveCollection.OfShort coll) {
		this(coll.size());
		addAll(coll);
	}

	/**
	 * Creates a new instance containing the items in the specified iterator.
	 *
	 * @param coll an iterator that will have its remaining contents added to this
	 */
	public ShortDeque (ShortIterator coll) {
		this();
		addAll(coll);
	}

	/**
	 * Copies the given ShortDeque exactly into this one. Individual values will be shallow-copied.
	 *
	 * @param deque another ShortDeque to copy
	 */
	public ShortDeque (ShortDeque deque) {
		this.values = Arrays.copyOf(deque.values, deque.values.length);
		this.size = deque.size;
		this.head = deque.head;
		this.tail = deque.tail;
		this.defaultValue = deque.defaultValue;
	}

	/**
	 * Creates a new ShortDeque using all of the contents of the given array.
	 *
	 * @param a an array of long that will be copied into this and used in full
	 */
	public ShortDeque (short[] a) {
		tail = a.length;
		this.values = Arrays.copyOf(a, tail);
		size = tail;
	}

	/**
	 * Creates a new ShortDeque using {@code count} items from {@code a}, starting at {@code offset}.
	 *
	 * @param a      an array of long
	 * @param offset where in {@code a} to start using items
	 * @param count  how many items to use from {@code a}
	 */
	public ShortDeque (short[] a, int offset, int count) {
		this.values = Arrays.copyOfRange(a, offset, offset + count);
		tail = count;
		size = count;
	}

	public short getDefaultValue () {
		return defaultValue;
	}

	public void setDefaultValue (short defaultValue) {
		this.defaultValue = defaultValue;
	}

	/**
	 * Append given item to the tail (enqueue to tail). Unless backing array needs resizing, operates in O(1) time.
	 *
	 * @param item a short to add to the tail
	 * @see #addFirst(short)
	 */
	public void addLast (short item) {
		short[] values = this.values;

		if (size == values.length) {
			resize(values.length << 1);
			values = this.values;
		}

		if (tail == values.length) {
			tail = 0;
		}
		values[tail++] = item;
		size++;
	}

	/**
	 * Prepend given item to the head (enqueue to head). Unless backing array needs resizing, operates in O(1) time.
	 *
	 * @param item a short to add to the head
	 * @see #addLast(short)
	 */
	public void addFirst (short item) {
		short[] values = this.values;

		if (size == values.length) {
			resize(values.length << 1);
			values = this.values;
		}

		int head = this.head;
		head--;
		if (head == -1) {
			head = values.length - 1;
		}
		values[head] = item;

		this.head = head;
		this.size++;
	}

	/**
	 * Increases the size of the backing array to accommodate the specified number of additional items. Useful before adding many
	 * items to avoid multiple backing array resizes.
	 */
	public void ensureCapacity (int additional) {
		final int needed = size + additional;
		if (values.length < needed) {
			resize(needed);
		}
	}

	/**
	 * Resize backing array. newSize must be bigger than current size.
	 */
	protected void resize (int newSize) {
		final short[] values = this.values;
		final int head = this.head;
		final int tail = this.tail;

		final short[] newArray = new short[Math.max(1, newSize)];
		if (head < tail) {
			// Continuous
			System.arraycopy(values, head, newArray, 0, tail - head);
		} else if (size > 0) {
			// Wrapped
			final int rest = values.length - head;
			System.arraycopy(values, head, newArray, 0, rest);
			System.arraycopy(values, 0, newArray, rest, tail);
		}
		this.values = newArray;
		this.head = 0;
		this.tail = size;
	}

	/**
	 * Remove the first item from the queue. (dequeue from head) Always O(1).
	 *
	 * @return removed item
	 * @throws NoSuchElementException when queue is empty
	 */
	public short removeFirst () {
		if (size == 0) {
			// Underflow
			throw new NoSuchElementException("ShortDeque is empty.");
		}

		final short[] values = this.values;

		final short result = values[head];
		head++;
		if (head == values.length) {
			head = 0;
		}
		size--;

		return result;
	}

	/**
	 * Remove the last item from the queue. (dequeue from tail) Always O(1).
	 *
	 * @return removed item
	 * @throws NoSuchElementException when queue is empty
	 * @see #removeFirst()
	 */
	public short removeLast () {
		if (size == 0) {
			throw new NoSuchElementException("ShortDeque is empty.");
		}

		final short[] values = this.values;
		int tail = this.tail;
		tail--;
		if (tail == -1) {
			tail = values.length - 1;
		}
		final short result = values[tail];
		this.tail = tail;
		size--;

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
	 * @throws IllegalArgumentException if some property of the specified
	 *                                  element prevents it from being added to this deque
	 */
	public boolean offerFirst (short t) {
		int oldSize = size;
		addFirst(t);
		return oldSize != size;
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
	 * @throws IllegalArgumentException if some property of the specified
	 *                                  element prevents it from being added to this deque
	 */
	public boolean offerLast (short t) {
		int oldSize = size;
		addLast(t);
		return oldSize != size;
	}

	/**
	 * Retrieves and removes the first element of this deque,
	 * or returns {@link #getDefaultValue() defaultValue} if this deque is empty.
	 *
	 * @return the head of this deque, or {@link #getDefaultValue() defaultValue} if this deque is empty
	 */
	public short pollFirst () {
		if(size == 0)
			return defaultValue;
		final short[] values = this.values;

		final short result = values[head];
		head++;
		if (head == values.length) {
			head = 0;
		}
		size--;

		return result;
	}

	/**
	 * Retrieves and removes the last element of this deque,
	 * or returns {@link #getDefaultValue() defaultValue} if this deque is empty.
	 *
	 * @return the tail of this deque, or {@link #getDefaultValue() defaultValue} if this deque is empty
	 */
	public short pollLast () {
		if (size == 0) {
			return defaultValue;
		}

		final short[] values = this.values;
		int tail = this.tail;
		tail--;
		if (tail == -1) {
			tail = values.length - 1;
		}
		final short result = values[tail];
		this.tail = tail;
		size--;

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
	public short getFirst () {
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
	public short getLast () {
		return last();
	}

	/**
	 * Retrieves, but does not remove, the first element of this deque,
	 * or returns {@link #defaultValue} if this deque is empty.
	 *
	 * @return the head of this deque, or {@link #defaultValue} if this deque is empty
	 */
	public short peekFirst () {
		if (size == 0) {
			// Underflow
			return defaultValue;
		}
		return values[head];
	}

	/**
	 * Retrieves, but does not remove, the last element of this deque,
	 * or returns {@link #defaultValue} if this deque is empty.
	 *
	 * @return the tail of this deque, or {@link #defaultValue} if this deque is empty
	 */
	public short peekLast () {
		if (size == 0) {
			// Underflow
			return defaultValue;
		}
		final short[] values = this.values;
		int tail = this.tail;
		tail--;
		if (tail == -1) {
			tail = values.length - 1;
		}
		return values[tail];
	}

	/**
	 * Removes the first occurrence of the specified element from this deque.
	 * If the deque does not contain the element, it is unchanged.
	 * More formally, removes the first element {@code e} such that
	 * {@code o == e)} (if such an element exists).
	 * Returns {@code true} if this deque contained the specified element
	 * (or equivalently, if this deque changed as a result of the call).
	 *
	 * @param o element to be removed from this deque, if present
	 * @return {@code true} if an element was removed as a result of this call
	 */
	public boolean removeFirstOccurrence (short o) {
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
	public boolean removeLastOccurrence (short o) {
		return removeLastValue(o);
	}

	/**
	 * Inserts the specified element into the queue represented by this deque
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
	 * @throws IllegalStateException    if the element cannot be added at this
	 *                                  time due to capacity restrictions
	 * @throws IllegalArgumentException if some property of the specified
	 *                                  element prevents it from being added to this deque
	 */
	@Override
	public boolean add (short t) {
		int oldSize = size;
		addLast(t);
		return oldSize != size;
	}

	/**
	 * Inserts the specified element into this deque at the specified index.
	 * Unlike {@link #offerFirst(short)} and {@link #offerLast(short)}, this does not run in expected constant time unless
	 * the index is less than or equal to 0 (where it acts like offerFirst()) or greater than or equal to {@link #size()}
	 * (where it acts like offerLast()).
	 * @param index the index in the deque's insertion order to insert the item
	 * @param item a short item to insert; may be null
	 * @return true if this deque was modified
	 */
	public boolean add (int index, short item) {
		int oldSize = size;
		if(index <= 0)
			addFirst(item);
		else if(index >= oldSize)
			addLast(item);
		else {
			short[] values = this.values;

			if (size == values.length) {
				resize(values.length << 1);
				values = this.values;
			}

			if(head < tail) {
				index += head;
				if(index >= values.length) index -= values.length;
				System.arraycopy(values, index, values, (index + 1) % values.length, tail - index);
				values[index] = item;
				tail++;
				if (tail > values.length) {
					tail = 1;
				}
			} else {
				if (head + index < values.length) {
					// backward shift
					System.arraycopy(values, head, values, head - 1, index);
					values[head - 1 + index] = item;
					head--;
					// don't need to check for head being negative, because head is always > tail
				}
				else {
					// forward shift
					index -= values.length - 1;
					System.arraycopy(values, head + index, values, head + index + 1, tail - head - index);
					values[head + index] = item;
					tail++;
					// again, don't need to check for tail going around, because the head is in the way and doesn't need to move
				}
			}
			size++;
		}
		return oldSize != size;
	}

	/**
	 * This is an alias for {@link #add(int, short)} to improve compatibility with primitive lists.
	 *
	 * @param index   index at which the specified element is to be inserted
	 * @param element element to be inserted
	 */
	public boolean insert (int index, short element) {
		return add(index, element);
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
	 * @return {@code true} if the element was added to this deque, else
	 * {@code false}
	 * @throws IllegalArgumentException if some property of the specified
	 *                                  element prevents it from being added to this deque
	 */
	public boolean offer (short t) {
		int oldSize = size;
		addLast(t);
		return oldSize != size;
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
	public short remove () {
		return removeFirst();
	}

	/**
	 * Retrieves and removes the head of the queue represented by this deque
	 * (in other words, the first element of this deque), or returns
	 * {@link #defaultValue} if this deque is empty.
	 *
	 * <p>This method is equivalent to {@link #pollFirst()}.
	 *
	 * @return the first element of this deque, or {@link #defaultValue} if
	 * this deque is empty
	 */
	public short poll () {
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
	public short element () {
		return first();
	}

	/**
	 * Retrieves, but does not remove, the head of the queue represented by
	 * this deque (in other words, the first element of this deque), or
	 * returns {@link #defaultValue} if this deque is empty.
	 *
	 * <p>This method is equivalent to {@link #peekFirst()}.
	 *
	 * @return the head of the queue represented by this deque, or
	 * {@link #defaultValue} if this deque is empty
	 */
	public short peek () {
		return peekFirst();
	}

	/**
	 * Pushes an element onto the stack represented by this deque (in other
	 * words, at the head of this deque) if it is possible to do so
	 * immediately without violating capacity restrictions, throwing an
	 * {@code IllegalStateException} if no space is currently available.
	 *
	 * <p>This method is equivalent to {@link #addFirst}.
	 *
	 * @param t the element to push
	 * @throws IllegalStateException    if the element cannot be added at this
	 *                                  time due to capacity restrictions
	 * @throws ClassCastException       if the class of the specified element
	 *                                  prevents it from being added to this deque
	 * @throws NullPointerException     if the specified element is null and this
	 *                                  deque does not permit null elements
	 * @throws IllegalArgumentException if some property of the specified
	 *                                  element prevents it from being added to this deque
	 */
	public void push (short t) {
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
	public short pop () {
		return removeFirst();
	}

	/**
	 * Removes the first occurrence of the specified element from this deque.
	 * If the deque does not contain the element, it is unchanged.
	 * More formally, removes the first element {@code e} such that
	 * {@code Objects.equals(o, e)} (if such an element exists).
	 * Returns {@code true} if this deque contained the specified element
	 * (or equivalently, if this deque changed as a result of the call).
	 *
	 * <p>This method is equivalent to {@link #removeFirstOccurrence(short)}.
	 *
	 * @param o element to be removed from this deque, if present
	 * @return {@code true} if an element was removed as a result of this call
	 * @throws ClassCastException   if the class of the specified element
	 *                              is incompatible with this deque
	 *                              (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>)
	 * @throws NullPointerException if the specified element is null and this
	 *                              deque does not permit null elements
	 *                              (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>)
	 */
	public boolean remove (short o) {
		return removeFirstOccurrence(o);
	}

	/**
	 * Returns {@code true} if this deque contains the specified element.
	 * More formally, returns {@code true} if and only if this deque contains
	 * at least one element {@code e} such that {@code Objects.equals(o, e)}.
	 *
	 * @param o element whose presence in this deque is to be tested
	 * @return {@code true} if this deque contains the specified element
	 * @throws ClassCastException   if the class of the specified element
	 *                              is incompatible with this deque
	 *                              (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>)
	 * @throws NullPointerException if the specified element is null and this
	 *                              deque does not permit null elements
	 *                              (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>)
	 */
	public boolean contains (short o) {
		return indexOf(o) != -1;
	}

	/**
	 * Returns the number of elements in this deque.
	 *
	 * @return the number of elements in this deque
	 */
	@Override
	public int size () {
		return size;
	}

	/**
	 * Returns an array containing all of the elements in this collection.
	 * If this collection makes any guarantees as to what order its elements
	 * are returned by its iterator, this method must return the elements in
	 * the same order. The returned array's {@linkplain Class#getComponentType
	 * runtime component type} is {@code Object}.
	 *
	 * <p>The returned array will be "safe" in that no references to it are
	 * maintained by this collection.  (In other words, this method must
	 * allocate a new array even if this collection is backed by an array).
	 * The caller is thus free to modify the returned array.
	 *
	 * @return an array, whose {@linkplain Class#getComponentType runtime component
	 * type} is {@code Object}, containing all of the elements in this collection
	 */
	@Override
	public short[] toArray () {
		short[] next = new short[size];
		if (head < tail) {
			System.arraycopy(values, head, next, 0, tail - head);
		} else {
			System.arraycopy(values, head, next, 0, size - head);
			System.arraycopy(values, 0, next, size - head, tail);
		}
		return next;
	}

	/**
	 * Reduces the size of the deque to the specified size. If the deque is already smaller than the specified
	 * size, no action is taken.
	 */
	public void truncate (int newSize) {
		newSize = Math.max(0, newSize);
		if (size() > newSize) {
			if(head < tail) {
				// only removing from tail, near the end, toward head, near the start
				tail -= size() - newSize;
				size = newSize;
			} else if(head + newSize < values.length) {
				// tail is near the start, but we have to remove elements through the start and into the back
				tail = head + newSize;
				size = newSize;
			} else {
				// tail is near the start, but we only have to remove some elements between tail and the start
				tail -= size() - newSize;
				size = newSize;
			}
		}
	}

	/**
	 * Returns the index of first occurrence of value in the queue, or -1 if no such value exists.
	 *
	 * @return An index of first occurrence of value in queue or -1 if no such value exists
	 */
	public int indexOf (short value) {
		if (size == 0)
			return -1;
		short[] values = this.values;
		final int head = this.head, tail = this.tail;
		if (head < tail) {
			for (int i = head; i < tail; i++)
				if (values[i] == value)
					return i - head;
		} else {
			for (int i = head, n = values.length; i < n; i++)
				if (values[i] == value)
					return i - head;
			for (int i = 0; i < tail; i++)
				if (values[i] == value)
					return i + values.length - head;
		}
		return -1;
	}

	/**
	 * Returns the index of last occurrence of value in the queue, or -1 if no such value exists.
	 *
	 * @return An index of last occurrence of value in queue or -1 if no such value exists
	 */
	public int lastIndexOf (short value) {
		if (size == 0)
			return -1;
		short[] values = this.values;
		final int head = this.head, tail = this.tail;
		if (head < tail) {
			for (int i = tail - 1; i >= head; i--)
				if (values[i] == value)
					return i - head;
		} else {
			for (int i = tail - 1; i >= 0; i--)
				if (values[i] == value)
					return i + values.length - head;
			for (int i = values.length - 1; i >= head; i--)
				if (values[i] == value)
					return i - head;
		}
		return -1;
	}

	/**
	 * Removes the first instance of the specified value in the queue.
	 *
	 * @return true if value was found and removed, false otherwise
	 */
	public boolean removeValue (short value) {
		int index = indexOf(value);
		if (index == -1)
			return false;
		removeAt(index);
		return true;
	}

	/**
	 * Removes the last instance of the specified value in the queue.
	 *
	 * @return true if value was found and removed, false otherwise
	 */
	public boolean removeLastValue (short value) {
		int index = lastIndexOf(value);
		if (index == -1)
			return false;
		removeAt(index);
		return true;
	}

	/**
	 * Removes and returns the item at the specified index.
	 */
	public short removeAt (int index) {
		if (index < 0)
			throw new IndexOutOfBoundsException("index can't be < 0: " + index);
		if (index >= size)
			throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);

		short[] values = this.values;
		int head = this.head, tail = this.tail;
		index += head;
		short value;
		if (head < tail) { // index is between head and tail.
			value = values[index];
			System.arraycopy(values, index + 1, values, index, tail - index - 1);
			this.tail--;
		} else if (index >= values.length) { // index is between 0 and tail.
			index -= values.length;
			value = values[index];
			System.arraycopy(values, index + 1, values, index, tail - index - 1);
			this.tail--;
		} else { // index is between head and values.length.
			value = values[index];
			System.arraycopy(values, head, values, head + 1, index - head);
			this.head++;
			if (this.head == values.length) {
				this.head = 0;
			}
		}
		size--;
		return value;
	}

	/**
	 * Returns true if the queue has one or more items.
	 */
	public boolean notEmpty () {
		return size != 0;
	}

	/**
	 * Returns true if the queue is empty.
	 */
	public boolean isEmpty () {
		return size == 0;
	}

	/**
	 * Returns the first (head) item in the queue (without removing it).
	 *
	 * @throws NoSuchElementException when queue is empty
	 * @see #addFirst(short)
	 * @see #removeFirst()
	 */
	public short first () {
		if (size == 0) {
			// Underflow
			throw new NoSuchElementException("ShortDeque is empty.");
		}
		return values[head];
	}

	/**
	 * Returns the last (tail) item in the queue (without removing it).
	 *
	 * @throws NoSuchElementException when queue is empty
	 * @see #addLast(short)
	 * @see #removeLast()
	 */
	public short last () {
		if (size == 0) {
			// Underflow
			throw new NoSuchElementException("ShortDeque is empty.");
		}
		final short[] values = this.values;
		int tail = this.tail;
		tail--;
		if (tail == -1)
			tail = values.length - 1;
		return values[tail];
	}

	/**
	 * Retrieves the value in queue without removing it. Indexing is from the front to back, zero based. Therefore get(0) is the
	 * same as {@link #first()}.
	 *
	 * @throws IndexOutOfBoundsException when the index is negative or >= size
	 */
	public short get (int index) {
		if (index < 0)
			throw new IndexOutOfBoundsException("index can't be < 0: " + index);
		if (index >= size)
			throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);
		final short[] values = this.values;

		int i = head + index;
		if (i >= values.length)
			i -= values.length;
		return values[i];
	}

	/**
	 * Sets an existing position in this deque to the given item. Indexing is from the front to back, zero based.
	 *
	 * @param index the index to set
	 * @param item  what value should replace the contents of the specified index
	 * @return the previous contents of the specified index
	 * @throws IndexOutOfBoundsException when the index is negative or >= size
	 */
	public short set (int index, short item) {
		if (index < 0)
			throw new IndexOutOfBoundsException("index can't be < 0: " + index);
		if (index >= size)
			throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);
		final short[] values = this.values;

		int i = head + index;
		if (i >= values.length)
			i -= values.length;
		short old = values[i];
		values[i] = item;
		return old;
	}

	/**
	 * Removes all values from this queue. Values in backing array are set to null to prevent memory leak, so this operates in
	 * O(n).
	 */
	public void clear () {
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
	@Override
	public ShortDequeIterator iterator () {
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
	public ShortDequeIterator descendingIterator () {
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

	@Override
	public String toString () {
		return toString(", ", true);
	}

	public int hashCode () {
		final int size = this.size;
		final short[] values = this.values;
		final int backingLength = values.length;
		int index = this.head;

		int hash = size + 1;
		for (int s = 0; s < size; s++) {
			final short value = values[index];

			hash *= 421;
			hash += value;
			index++;
			if (index == backingLength)
				index = 0;
		}

		return hash;
	}

	public boolean equals (Object o) {
		if (this == o)
			return true;
		if (!(o instanceof ShortDeque))
			return false;

		ShortDeque q = (ShortDeque)o;
		final int size = this.size;

		if (q.size != size)
			return false;

		final short[] myValues = this.values;
		final int myBackingLength = myValues.length;
		final short[] itsValues = q.values;
		final int itsBackingLength = itsValues.length;

		int myIndex = head;
		int itsIndex = q.head;
		for (int s = 0; s < size; s++) {
			short myValue = myValues[myIndex];
			short itsValue = itsValues[itsIndex];

			if (myValue != itsValue)
				return false;
			myIndex++;
			itsIndex++;
			if (myIndex == myBackingLength)
				myIndex = 0;
			if (itsIndex == itsBackingLength)
				itsIndex = 0;
		}
		return true;
	}

	/**
	 * Switches the ordering of positions {@code first} and {@code second}, without changing any items beyond that.
	 *
	 * @param first  the first position, must not be negative and must be less than {@link #size()}
	 * @param second the second position, must not be negative and must be less than {@link #size()}
	 */
	@Override
	public void swap (int first, int second) {
		if (first < 0)
			throw new IndexOutOfBoundsException("first index can't be < 0: " + first);
		if (first >= size)
			throw new IndexOutOfBoundsException("first index can't be >= size: " + first + " >= " + size);
		if (second < 0)
			throw new IndexOutOfBoundsException("second index can't be < 0: " + second);
		if (second >= size)
			throw new IndexOutOfBoundsException("second index can't be >= size: " + second + " >= " + size);
		final short[] values = this.values;

		int f = head + first;
		if (f >= values.length)
			f -= values.length;

		int s = head + second;
		if (s >= values.length)
			s -= values.length;

		short fv = values[f];
		values[f] = values[s];
		values[s] = fv;

	}

	/**
	 * Reverses this ShortDeque in-place.
	 */
	@Override
	public void reverse () {
		final short[] values = this.values;
		int f, s, len = values.length;
		short fv;
		for (int n = size >> 1, b = 0, t = size - 1; b <= n && b != t; b++, t--) {
			f = head + b;
			if (f >= len)
				f -= len;
			s = head + t;
			if (s >= len)
				s -= len;
			fv = values[f];
			values[f] = values[s];
			values[s] = fv;
		}
	}

	/**
	 * Sorts this deque in-place using {@link Arrays#sort(short[], int, int)}.
	 * This should operate in O(n log(n)) time or less when the internals of the deque are
	 * continuous (the head is before the tail in the array). If the internals are not
	 * continuous, this takes an additional O(n) step (where n is less than the size of
	 * the deque) to rearrange the internals before sorting.
	 */
	public void sort () {
		if (head <= tail) {
			Arrays.sort(values, head, tail);
		} else {
			System.arraycopy(values, head, values, tail, values.length - head);
			Arrays.sort(values, 0, size);
			tail = size;
			head = 0;
		}
	}

	/**
	 * Sorts this deque in-place using {@link ShortComparators#sort(short[], int, int, ShortComparator)}.
	 * This should operate in O(n log(n)) time or less when the internals of the deque are
	 * continuous (the head is before the tail in the array). If the internals are not
	 * continuous, this takes an additional O(n) step (where n is less than the size of
	 * the deque) to rearrange the internals before sorting.
	 */
	public void sort (@Nullable ShortComparator c) {
		if (head <= tail) {
			ShortComparators.sort(values, head, tail, c);
		} else {
			System.arraycopy(values, head, values, tail, values.length - head);
			ShortComparators.sort(values, 0, size, c);
			tail = size;
			head = 0;
		}
	}

	public short random (Random random) {
		if (size <= 0) {
			throw new NoSuchElementException("ShortDeque is empty.");
		}
		return get(random.nextInt(size));
	}

	/**
	 * A {@link ShortIterator}, plus similar methods to a {@link ListIterator}, over the elements of an ShortDeque.
	 * Use {@link #nextShort()} in preference to {@link #next()} to avoid allocating Short objects.
	 */
	public static class ShortDequeIterator implements ShortIterator {
		protected int index, latest = -1;
		protected ShortDeque deque;
		protected boolean valid = true;
		private final int direction;

		public ShortDequeIterator (ShortDeque deque) {
			this(deque, false);
		}
		public ShortDequeIterator (ShortDeque deque, boolean descendingOrder) {
			this.deque = deque;
			direction = descendingOrder ? -1 : 1;
		}

		public ShortDequeIterator (ShortDeque deque, int index, boolean descendingOrder) {
			if (index < 0 || index >= deque.size())
				throw new IndexOutOfBoundsException("ShortDequeIterator does not satisfy index >= 0 && index < deque.size()");
			this.deque = deque;
			this.index = index;
			direction = descendingOrder ? -1 : 1;
		}

		/**
		 * Returns the next {@code short} element in the iteration.
		 *
		 * @return the next {@code short} element in the iteration
		 * @throws NoSuchElementException if the iteration has no more elements
		 */
		public short nextShort () {
			if (!hasNext()) {throw new NoSuchElementException();}
			latest = index;
			index += direction;
			return deque.get(latest);
		}

		/**
		 * Returns {@code true} if the iteration has more elements.
		 * (In other words, returns {@code true} if {@link #nextShort} would
		 * return an element rather than throwing an exception.)
		 *
		 * @return {@code true} if the iteration has more elements
		 */
		@Override
		public boolean hasNext () {
			if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
			return direction == 1 ? index < deque.size() : index > 0 && deque.notEmpty();
		}

		/**
		 * Returns {@code true} if this list iterator has more elements when
		 * traversing the list in the reverse direction.  (In other words,
		 * returns {@code true} if {@link #previousShort} would return an element
		 * rather than throwing an exception.)
		 *
		 * @return {@code true} if the list iterator has more elements when
		 * traversing the list in the reverse direction
		 */
		public boolean hasPrevious () {
			if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
			return direction == -1 ? index < deque.size() : index > 0 && deque.notEmpty();
		}

		/**
		 * Returns the previous element in the list and moves the cursor
		 * position backwards.  This method may be called repeatedly to
		 * iterate through the list backwards, or intermixed with calls to
		 * {@link #nextShort} to go back and forth.  (Note that alternating calls
		 * to {@code next} and {@code previous} will return the same
		 * element repeatedly.)
		 *
		 * @return the previous element in the list
		 * @throws NoSuchElementException if the iteration has no previous
		 *                                element
		 */
		public short previousShort () {
			if (!hasPrevious()) {throw new NoSuchElementException();}
			return deque.get(latest = (index -= direction));
		}

		/**
		 * Returns the index of the element that would be returned by a
		 * subsequent call to {@link #nextShort}. (Returns list size if the list
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
		 * subsequent call to {@link #previousShort}. (Returns -1 if the list
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
		 * #nextShort} or {@link #previousShort} (optional operation).  This call can
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
			if (latest == -1 || latest >= deque.size()) {throw new NoSuchElementException();}
			deque.removeAt(latest);
			index = latest;
			latest = -1;
		}

		/**
		 * Replaces the last element returned by {@link #nextShort} or
		 * {@link #previousShort} with the specified element (optional operation).
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
		public void set (short t) {
			if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
			if (latest == -1 || latest >= deque.size()) {throw new NoSuchElementException();}
			deque.set(latest, t);
		}

		/**
		 * Inserts the specified element into the list (optional operation).
		 * The element is inserted immediately before the element that
		 * would be returned by {@link #nextShort}, if any, and after the element
		 * that would be returned by {@link #previousShort}, if any.  (If the
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
		public void add (short t) {
			if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
			if (index > deque.size()) {throw new NoSuchElementException();}
			deque.insert(index, t);
			index += direction;
			latest = -1;
		}

		public void reset () {
			index = deque.size - 1 & direction >> 31;
			latest = -1;
		}

		public void reset (int index) {
			if (index < 0 || index >= deque.size())
				throw new IndexOutOfBoundsException("ShortDequeIterator does not satisfy index >= 0 && index < deque.size()");
			this.index = index;
			latest = -1;
		}

		/**
		 * Returns an iterator over elements of type {@code short}. Allows this to be used like an {@link Iterable}.
		 *
		 * @return this same ShortDequeIterator.
		 */
		public ShortDequeIterator iterator () {
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
	public static ShortDeque with () {
		return new ShortDeque(0);
	}

	/**
	 * Creates a new ShortDeque that holds only the given item, but can be resized.
	 * @param item a short item
	 * @return a new ShortDeque that holds the given item
	 */

	public static ShortDeque with (short item) {
		ShortDeque deque = new ShortDeque(1);
		deque.add(item);
		return deque;
	}

	/**
	 * Creates a new ShortDeque that holds only the given items, but can be resized.
	 * @param item0 a short item
	 * @param item1 a short item
	 * @return a new ShortDeque that holds the given items
	 */
	public static ShortDeque with (short item0, short item1) {
		ShortDeque deque = new ShortDeque(2);
		deque.add(item0);
		deque.add(item1);
		return deque;
	}

	/**
	 * Creates a new ShortDeque that holds only the given items, but can be resized.
	 * @param item0 a short item
	 * @param item1 a short item
	 * @param item2 a short item
	 * @return a new ShortDeque that holds the given items
	 */
	public static ShortDeque with (short item0, short item1, short item2) {
		ShortDeque deque = new ShortDeque(3);
		deque.add(item0);
		deque.add(item1);
		deque.add(item2);
		return deque;
	}

	/**
	 * Creates a new ShortDeque that holds only the given items, but can be resized.
	 * @param item0 a short item
	 * @param item1 a short item
	 * @param item2 a short item
	 * @param item3 a short item
	 * @return a new ShortDeque that holds the given items
	 */
	public static ShortDeque with (short item0, short item1, short item2, short item3) {
		ShortDeque deque = new ShortDeque(4);
		deque.add(item0);
		deque.add(item1);
		deque.add(item2);
		deque.add(item3);
		return deque;
	}

	/**
	 * Creates a new ShortDeque that holds only the given items, but can be resized.
	 * @param item0 a short item
	 * @param item1 a short item
	 * @param item2 a short item
	 * @param item3 a short item
	 * @param item4 a short item
	 * @return a new ShortDeque that holds the given items
	 */
	public static ShortDeque with (short item0, short item1, short item2, short item3, short item4) {
		ShortDeque deque = new ShortDeque(5);
		deque.add(item0);
		deque.add(item1);
		deque.add(item2);
		deque.add(item3);
		deque.add(item4);
		return deque;
	}

	/**
	 * Creates a new ShortDeque that holds only the given items, but can be resized.
	 * @param item0 a short item
	 * @param item1 a short item
	 * @param item2 a short item
	 * @param item3 a short item
	 * @param item4 a short item
	 * @param item5 a short item
	 * @return a new ShortDeque that holds the given items
	 */
	public static ShortDeque with (short item0, short item1, short item2, short item3, short item4, short item5) {
		ShortDeque deque = new ShortDeque(6);
		deque.add(item0);
		deque.add(item1);
		deque.add(item2);
		deque.add(item3);
		deque.add(item4);
		deque.add(item5);
		return deque;
	}

	/**
	 * Creates a new ShortDeque that holds only the given items, but can be resized.
	 * @param item0 a short item
	 * @param item1 a short item
	 * @param item2 a short item
	 * @param item3 a short item
	 * @param item4 a short item
	 * @param item5 a short item
	 * @param item6 a short item
	 * @return a new ShortDeque that holds the given items
	 */
	public static ShortDeque with (short item0, short item1, short item2, short item3, short item4, short item5, short item6) {
		ShortDeque deque = new ShortDeque(7);
		deque.add(item0);
		deque.add(item1);
		deque.add(item2);
		deque.add(item3);
		deque.add(item4);
		deque.add(item5);
		deque.add(item6);
		return deque;
	}

	/**
	 * Creates a new ShortDeque that holds only the given items, but can be resized.
	 * @param item0 a short item
	 * @param item1 a short item
	 * @param item2 a short item
	 * @param item3 a short item
	 * @param item4 a short item
	 * @param item5 a short item
	 * @param item6 a short item
	 * @return a new ShortDeque that holds the given items
	 */
	public static ShortDeque with (short item0, short item1, short item2, short item3, short item4, short item5, short item6, short item7) {
		ShortDeque deque = new ShortDeque(8);
		deque.add(item0);
		deque.add(item1);
		deque.add(item2);
		deque.add(item3);
		deque.add(item4);
		deque.add(item5);
		deque.add(item6);
		deque.add(item7);
		return deque;
	}

	/**
	 * Creates a new ShortDeque that holds only the given items, but can be resized.
	 * This overload will only be used when an array is supplied and the type of the
	 * items requested is the component type of the array, or if varargs are used and
	 * there are 9 or more arguments.
	 * @param varargs a short varargs or short array; remember that varargs allocate
	 * @return a new ShortDeque that holds the given items
	 */
	public static ShortDeque with (short... varargs) {
		return new ShortDeque(varargs);
	}
}
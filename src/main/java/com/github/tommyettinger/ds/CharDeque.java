/*******************************************************************************
 * Copyright 2021 See AUTHORS file.
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

import com.github.tommyettinger.ds.support.EnhancedRandom;
import com.github.tommyettinger.ds.support.sort.CharComparator;
import com.github.tommyettinger.ds.support.sort.CharComparators;
import com.github.tommyettinger.ds.support.util.CharIterator;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.NoSuchElementException;

/**
 * A resizable, insertion-ordered double-ended queue of shorts with efficient add and remove at the beginning and end. Values in the
 * backing array may wrap back to the beginning, making add and remove at the beginning and end O(1) (unless the backing array needs to
 * resize when adding). Deque functionality is provided via {@link #removeLast()} and {@link #addFirst(char)}.
 * <br>
 * Unlike most Deque implementations in the JDK, you can get and set items anywhere in the deque in constant time with {@link #get(int)}
 * and {@link #set(int, char)}.
 */
public class CharDeque implements PrimitiveCollection.OfChar, Arrangeable {

	protected char defaultValue = '\uffff';

	/** Contains the values in the queue. Head and tail indices go in a circle around this array, wrapping at the end. */
	protected char[] values;

	/** Index of first element. Logically smaller than tail. Unless empty, it points to a valid element inside queue. */
	protected int head = 0;

	/** Index of last element. Logically bigger than head. Usually points to an empty position, but points to the head when full
	 * (size == values.length). */
	protected int tail = 0;

	/** Number of elements in the queue. */
	public int size = 0;

	protected transient @Nullable CharDequeIterator iterator1;
	protected transient @Nullable CharDequeIterator iterator2;

	protected transient @Nullable CharDequeIterator descendingIterator1;
	protected transient @Nullable CharDequeIterator descendingIterator2;

	/** Creates a new CharDeque which can hold 16 values without needing to resize backing array. */
	public CharDeque () {
		this(16);
	}

	/** Creates a new CharDeque which can hold the specified number of values without needing to resize backing array. */
	public CharDeque (int initialSize) {
		this.values = new char[initialSize];
	}

	/**
	 * Creates a new CharDeque using all of the contents of the given PrimitiveCollection.OfChar, such as
	 * a {@link CharList}.
	 * @param coll a PrimitiveCollection.OfChar that will be copied into this and used in full
	 */
	public CharDeque (PrimitiveCollection.OfChar coll) {
		this(coll.size());
		addAll(coll);
	}

	/**
	 * Copies the given CharDeque exactly into this one. Individual values will be shallow-copied.
	 * @param deque another CharDeque to copy
	 */
	public CharDeque (CharDeque deque) {
		this.values = Arrays.copyOf(deque.values, deque.values.length);
		this.size = deque.size;
		this.head = deque.head;
		this.tail = deque.tail;
		this.defaultValue = deque.defaultValue;
	}

	/**
	 * Creates a new CharDeque using all of the contents of the given array.
	 * @param a an array of long that will be copied into this and used in full
	 */
	public CharDeque (char[] a) {
		tail = a.length;
		this.values = Arrays.copyOf(a, tail);
		size = tail;
	}

	/**
	 * Creates a new CharDeque using {@code count} items from {@code a}, starting at {@code offset}.
	 * @param a an array of long
	 * @param offset where in {@code a} to start using items
	 * @param count how many items to use from {@code a}
	 */
	public CharDeque (char[] a, int offset, int count) {
		this.values = Arrays.copyOfRange(a, offset, offset + count);
		tail = count;
		size = count;
	}

	public char getDefaultValue () {
		return defaultValue;
	}

	public void setDefaultValue (char defaultValue) {
		this.defaultValue = defaultValue;
	}

	/** Append given item to the tail (enqueue to tail). Unless backing array needs resizing, operates in O(1) time.
	 * @see #addFirst(char) 
	 * @param item a char to add to the tail */
	public void addLast (char item) {
		char[] values = this.values;

		if (size == values.length) {
			resize(values.length << 1);
			values = this.values;
		}

		values[tail++] = item;
		if (tail == values.length) {
			tail = 0;
		}
		size++;
	}

	/** Prepend given item to the head (enqueue to head). Unless backing array needs resizing, operates in O(1) time.
	 * @see #addLast(char)
	 * @param item a char to add to the head */
	public void addFirst (char item) {
		char[] values = this.values;

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

	/** Increases the size of the backing array to accommodate the specified number of additional items. Useful before adding many
	 * items to avoid multiple backing array resizes. */
	public void ensureCapacity (int additional) {
		final int needed = size + additional;
		if (values.length < needed) {
			resize(needed);
		}
	}

	/** Resize backing array. newSize must be bigger than current size. */
	protected void resize (int newSize) {
		final char[] values = this.values;
		final int head = this.head;
		final int tail = this.tail;

		final char[] newArray = new char[newSize];
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

	/** Remove the first item from the queue. (dequeue from head) Always O(1).
	 * @return removed item
	 * @throws NoSuchElementException when queue is empty */
	public char removeFirst () {
		if (size == 0) {
			// Underflow
			throw new NoSuchElementException("CharDeque is empty.");
		}

		final char[] values = this.values;

		final char result = values[head];
		head++;
		if (head == values.length) {
			head = 0;
		}
		size--;

		return result;
	}

	/** Remove the last item from the queue. (dequeue from tail) Always O(1).
	 * @see #removeFirst()
	 * @return removed item
	 * @throws NoSuchElementException when queue is empty */
	public char removeLast () {
		if (size == 0) {
			throw new NoSuchElementException("CharDeque is empty.");
		}

		final char[] values = this.values;
		int tail = this.tail;
		tail--;
		if (tail == -1) {
			tail = values.length - 1;
		}
		final char result = values[tail];
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
	 * @throws ClassCastException       if the class of the specified element
	 *                                  prevents it from being added to this deque
	 * @throws NullPointerException     if the specified element is null and this
	 *                                  deque does not permit null elements
	 * @throws IllegalArgumentException if some property of the specified
	 *                                  element prevents it from being added to this deque
	 */
	public boolean offerFirst (char t) {
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
	 * @throws ClassCastException       if the class of the specified element
	 *                                  prevents it from being added to this deque
	 * @throws NullPointerException     if the specified element is null and this
	 *                                  deque does not permit null elements
	 * @throws IllegalArgumentException if some property of the specified
	 *                                  element prevents it from being added to this deque
	 */
	public boolean offerLast (char t) {
		int oldSize = size;
		addLast(t);
		return oldSize != size;
	}

	/**
	 * Retrieves and removes the first element of this deque,
	 * or returns {@code null} if this deque is empty.
	 *
	 * @return the head of this deque, or {@code null} if this deque is empty
	 */
	public char pollFirst () {
		return removeFirst();
	}

	/**
	 * Retrieves and removes the last element of this deque,
	 * or returns {@code null} if this deque is empty.
	 *
	 * @return the tail of this deque, or {@code null} if this deque is empty
	 */
	public char pollLast () {
		return removeLast();
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
	public char getFirst () {
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
	public char getLast () {
		return last();
	}

	/**
	 * Retrieves, but does not remove, the first element of this deque,
	 * or returns {@link #defaultValue} if this deque is empty.
	 *
	 * @return the head of this deque, or {@link #defaultValue} if this deque is empty
	 */
	public char peekFirst () {
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
	public char peekLast () {
		if (size == 0) {
			// Underflow
			return defaultValue;
		}
		final char[] values = this.values;
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
	 * {@code Objects.equals(o, e)} (if such an element exists).
	 * Returns {@code true} if this deque contained the specified element
	 * (or equivalently, if this deque changed as a result of the call).
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
	public boolean removeFirstOccurrence (char o) {
		return removeValue(o);
	}

	/**
	 * Removes the last occurrence of the specified element from this deque.
	 * If the deque does not contain the element, it is unchanged.
	 * More formally, removes the last element {@code e} such that
	 * {@code Objects.equals(o, e)} (if such an element exists).
	 * Returns {@code true} if this deque contained the specified element
	 * (or equivalently, if this deque changed as a result of the call).
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
	public boolean removeLastOccurrence (char o) {
		return removeLastValue(o);
	}

	/**
	 * Inserts the specified element into the queue represented by this deque
	 * (in other words, at the tail of this deque) if it is possible to do so
	 * immediately without violating capacity restrictions, returning
	 * {@code true} upon success and throwing an
	 * {@code IllegalStateException} if no space is currently available.
	 * When using a capacity-restricted deque, it is generally preferable to
	 * use {@link #offer(char) offer}.
	 *
	 * <p>This method is equivalent to {@link #addLast}.
	 *
	 * @param t the element to add
	 * @return {@code true} (as specified by {@link Collection#add})
	 * @throws IllegalStateException    if the element cannot be added at this
	 *                                  time due to capacity restrictions
	 * @throws ClassCastException       if the class of the specified element
	 *                                  prevents it from being added to this deque
	 * @throws NullPointerException     if the specified element is null and this
	 *                                  deque does not permit null elements
	 * @throws IllegalArgumentException if some property of the specified
	 *                                  element prevents it from being added to this deque
	 */
	@Override
	public boolean add (char t) {
		int oldSize = size;
		addLast(t);
		return oldSize != size;
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
	 * @throws ClassCastException       if the class of the specified element
	 *                                  prevents it from being added to this deque
	 * @throws NullPointerException     if the specified element is null and this
	 *                                  deque does not permit null elements
	 * @throws IllegalArgumentException if some property of the specified
	 *                                  element prevents it from being added to this deque
	 */
	public boolean offer (char t) {
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
	public char remove () {
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
	public char poll () {
		return removeFirst();
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
	public char element () {
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
	public char peek () {
		return peekFirst();
	}

	/**
	 * Adds all of the elements in the specified collection at the end
	 * of this deque, as if by calling {@link #addLast} on each one,
	 * in the order that they are returned by the collection's iterator.
	 *
	 * <p>When using a capacity-restricted deque, it is generally preferable
	 * to call {@link #offer(char) offer} separately on each element.
	 *
	 * <p>An exception encountered while trying to add an element may result
	 * in only some of the elements having been successfully added when
	 * the associated exception is thrown.
	 *
	 * @param c the elements to be inserted into this deque
	 * @return {@code true} if this deque changed as a result of the call
	 * @throws IllegalStateException    if not all the elements can be added at
	 *                                  this time due to insertion restrictions
	 * @throws ClassCastException       if the class of an element of the specified
	 *                                  collection prevents it from being added to this deque
	 * @throws NullPointerException     if the specified collection contains a
	 *                                  null element and this deque does not permit null elements,
	 *                                  or if the specified collection is null
	 * @throws IllegalArgumentException if some property of an element of the
	 *                                  specified collection prevents it from being added to this deque
	 */
	@Override
	public boolean addAll (OfChar c) {
		int oldSize = size;
		CharIterator it = c.iterator();
		while (it.hasNext()) {
			addLast(it.nextChar());
		}
		return oldSize != size;
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
	public void push (char t) {
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
	public char pop () {
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
	 * <p>This method is equivalent to {@link #removeFirstOccurrence(char)}.
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
	public boolean remove (char o) {
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
	public boolean contains (char o) {
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
	public char[] toArray () {
		char[] next = new char[size];
		if(head < tail) {
			System.arraycopy(values, head, next, 0, tail - head);
		}
		else {
			System.arraycopy(values, head, next, 0, size - head);
			System.arraycopy(values, 0, next, size - head, tail);
		}
		return next;
	}

	/** Returns the index of first occurrence of value in the queue, or -1 if no such value exists.
	 * @return An index of first occurrence of value in queue or -1 if no such value exists */
	public int indexOf (char value) {
		if (size == 0)
			return -1;
		char[] values = this.values;
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

	/** Returns the index of last occurrence of value in the queue, or -1 if no such value exists.
	 * @return An index of last occurrence of value in queue or -1 if no such value exists */
	public int lastIndexOf (char value) {
		if (size == 0)
			return -1;
		char[] values = this.values;
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

	/** Removes the first instance of the specified value in the queue.
	 * @return true if value was found and removed, false otherwise */
	public boolean removeValue (char value) {
		int index = indexOf(value);
		if (index == -1) return false;
		removeAt(index);
		return true;
	}

	/** Removes the last instance of the specified value in the queue.
	 * @return true if value was found and removed, false otherwise */
	public boolean removeLastValue (char value) {
		int index = lastIndexOf(value);
		if (index == -1) return false;
		removeAt(index);
		return true;
	}

	/** Removes and returns the item at the specified index. */
	public char removeAt (int index) {
		if (index < 0) throw new IndexOutOfBoundsException("index can't be < 0: " + index);
		if (index >= size) throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);

		char[] values = this.values;
		int head = this.head, tail = this.tail;
		index += head;
		char value;
		if (head < tail) { // index is between head and tail.
			value = values[index];
			System.arraycopy(values, index + 1, values, index, tail - index);
			this.tail--;
		} else if (index >= values.length) { // index is between 0 and tail.
			index -= values.length;
			value = values[index];
			System.arraycopy(values, index + 1, values, index, tail - index);
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

	/** Returns true if the queue has one or more items. */
	public boolean notEmpty () {
		return size > 0;
	}

	/** Returns true if the queue is empty. */
	public boolean isEmpty () {
		return size == 0;
	}

	/** Returns the first (head) item in the queue (without removing it).
	 * @see #addFirst(char)
	 * @see #removeFirst()
	 * @throws NoSuchElementException when queue is empty */
	public char first () {
		if (size == 0) {
			// Underflow
			throw new NoSuchElementException("CharDeque is empty.");
		}
		return values[head];
	}

	/** Returns the last (tail) item in the queue (without removing it).
	 * @see #addLast(char)
	 * @see #removeLast()
	 * @throws NoSuchElementException when queue is empty */
	public char last () {
		if (size == 0) {
			// Underflow
			throw new NoSuchElementException("CharDeque is empty.");
		}
		final char[] values = this.values;
		int tail = this.tail;
		tail--;
		if (tail == -1) tail = values.length - 1;
		return values[tail];
	}

	/** Retrieves the value in queue without removing it. Indexing is from the front to back, zero based. Therefore get(0) is the
	 * same as {@link #first()}.
	 * @throws IndexOutOfBoundsException when the index is negative or >= size */
	public char get (int index) {
		if (index < 0) throw new IndexOutOfBoundsException("index can't be < 0: " + index);
		if (index >= size) throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);
		final char[] values = this.values;

		int i = head + index;
		if (i >= values.length) i -= values.length;
		return values[i];
	}

	/** Sets an existing position in this deque to the given item. Indexing is from the front to back, zero based.
	 * @param index the index to set
	 * @param item what value should replace the contents of the specified index
	 * @return the previous contents of the specified index
	 * @throws IndexOutOfBoundsException when the index is negative or >= size */
	public char set (int index, char item) {
		if (index < 0) throw new IndexOutOfBoundsException("index can't be < 0: " + index);
		if (index >= size) throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);
		final char[] values = this.values;

		int i = head + index;
		if (i >= values.length) i -= values.length;
		char old = values[i];
		values[i] = item;
		return old;
	}

	/** Removes all values from this queue. Values in backing array are set to null to prevent memory leak, so this operates in
	 * O(n). */
	public void clear () {
		if (size == 0) return;
		this.head = 0;
		this.tail = 0;
		this.size = 0;
	}

	/**
	 * Returns an iterator for the items in the deque. Remove is supported.
	 * <br>
	 * Reuses one of two iterators for this deque. For nested or multithreaded
	 * iteration, use {@link CharDequeIterator#CharDequeIterator(CharDeque)}.
	 */
	@Override
	public CharIterator iterator () {
		if (iterator1 == null || iterator2 == null) {
			iterator1 = new CharDequeIterator(this);
			iterator2 = new CharDequeIterator(this);
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
	 * iteration, use {@link CharDequeIterator#CharDequeIterator(CharDeque, boolean)}.
	 *
	 * @return an iterator over the elements in this deque in reverse sequence
	 */
	public CharIterator descendingIterator () {
		if (descendingIterator1 == null || descendingIterator2 == null) {
			descendingIterator1 = new CharDequeIterator(this, true);
			descendingIterator2 = new CharDequeIterator(this, true);
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

	public String toString () {
		if (size == 0) {
			return "[]";
		}
		final char[] values = this.values;
		final int head = this.head;
		final int tail = this.tail;

		StringBuilder sb = new StringBuilder(64);
		sb.append('[');
		sb.append(values[head]);
		for (int i = (head + 1) % values.length; i != tail; i = (i + 1) % values.length) {
			sb.append(", ").append(values[i]);
		}
		sb.append(']');
		return sb.toString();
	}

	public String toString (String separator) {
		if (size == 0) return "";
		final char[] values = this.values;
		final int head = this.head;
		final int tail = this.tail;

		StringBuilder sb = new StringBuilder(64);
		sb.append(values[head]);
		for (int i = (head + 1) % values.length; i != tail; i = (i + 1) % values.length)
			sb.append(separator).append(values[i]);
		return sb.toString();
	}
	/**
	 * Simply returns all of the char items in this as one String, with no delimiters.
	 * @return a String containing only the char items in this CharDeque
	 */
	public String toDenseString() {
		if(head < tail)
			return String.valueOf(values, head, size);
		else
			return String.valueOf(values, head, values.length - head) + String.valueOf(values, 0, tail);
	}

	public int hashCode () {
		final int size = this.size;
		final char[] values = this.values;
		final int backingLength = values.length;
		int index = this.head;

		int hash = size + 1;
		for (int s = 0; s < size; s++) {
			final char value = values[index];

			hash *= 421;
			hash += value;
			index++;
			if (index == backingLength) index = 0;
		}

		return hash;
	}

	public boolean equals (Object o) {
		if (this == o) return true;
		if (!(o instanceof CharDeque)) return false;

		CharDeque q = (CharDeque)o;
		final int size = this.size;

		if (q.size != size) return false;

		final char[] myValues = this.values;
		final int myBackingLength = myValues.length;
		final char[] itsValues = q.values;
		final int itsBackingLength = itsValues.length;

		int myIndex = head;
		int itsIndex = q.head;
		for (int s = 0; s < size; s++) {
			char myValue = myValues[myIndex];
			char itsValue = itsValues[itsIndex];

			if (myValue != itsValue) return false;
			myIndex++;
			itsIndex++;
			if (myIndex == myBackingLength) myIndex = 0;
			if (itsIndex == itsBackingLength) itsIndex = 0;
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
		if (first < 0) throw new IndexOutOfBoundsException("first index can't be < 0: " + first);
		if (first >= size) throw new IndexOutOfBoundsException("first index can't be >= size: " + first + " >= " + size);
		if (second < 0) throw new IndexOutOfBoundsException("second index can't be < 0: " + second);
		if (second >= size) throw new IndexOutOfBoundsException("second index can't be >= size: " + second + " >= " + size);
		final char[] values = this.values;

		int f = head + first;
		if (f >= values.length) f -= values.length;

		int s = head + second;
		if (s >= values.length) s -= values.length;

		char fv = values[f];
		values[f] = values[s];
		values[s] = fv;

	}

	/**
	 * Reverses this CharDeque in-place.
	 */
	@Override
	public void reverse () {
		final char[] values = this.values;
		int f, s, len = values.length;
		char fv;
		for (int n = size >> 1, b = 0, t = size - 1; b <= n && b != t; b++, t--) {
			f = head + b;
			if(f >= len) f -= len;
			s = head + t;
			if(s >= len) s -= len;
			fv = values[f];
			values[f] = values[s];
			values[s] = fv;
		}
	}

	/**
	 * Sorts this deque in-place using {@link Arrays#sort(char[], int, int)}.
	 * This should operate in O(n log(n)) time or less when the internals of the deque are
	 * continuous (the head is before the tail in the array). If the internals are not
	 * continuous, this takes an additional O(n) step (where n is less than the size of
	 * the deque) to rearrange the internals before sorting.
	 */
	public void sort(){
		if(head <= tail) {
			Arrays.sort(values, head, tail);
		} else {
			System.arraycopy(values, head, values, tail, values.length - head);
			Arrays.sort(values, 0, size);
			tail = size;
			head = 0;
		}
	}

	/**
	 * Sorts this deque in-place using {@link CharComparators#sort(char[], int, int, CharComparator)}.
	 * This should operate in O(n log(n)) time or less when the internals of the deque are
	 * continuous (the head is before the tail in the array). If the internals are not
	 * continuous, this takes an additional O(n) step (where n is less than the size of
	 * the deque) to rearrange the internals before sorting.
	 */
	public void sort(@Nullable CharComparator c){
		if(head <= tail) {
			CharComparators.sort(values, head, tail, c);
		} else {
			System.arraycopy(values, head, values, tail, values.length - head);
			CharComparators.sort(values, 0, size, c);
			tail = size;
			head = 0;
		}
	}

	public char random(EnhancedRandom random){
		if(size <= 0) {
			throw new NoSuchElementException("CharDeque is empty.");
		}
		return get(random.nextInt(size));
	}

	public static class CharDequeIterator implements CharIterator {
		private final CharDeque deque;
		private final boolean descending;
		int index;
		boolean valid = true;

		public CharDequeIterator (CharDeque deque) {
			this(deque, false);
		}

		public CharDequeIterator (CharDeque deque, boolean descendingOrder) {
			this.deque = deque;
			if(this.descending = descendingOrder)
				index = this.deque.size - 1;
		}

		public boolean hasNext () {
			if (!valid) {
				throw new RuntimeException("#iterator() cannot be used nested.");
			}
			return descending ? index >= 0 : index < deque.size;
		}

		public char nextChar () {
			if (index >= deque.size || index < 0) throw new NoSuchElementException(String.valueOf(index));
			if (!valid) {
				throw new RuntimeException("#iterator() cannot be used nested.");
			}
			return deque.get(descending ? index-- : index++);
		}

		public void remove () {
			if(descending) index++;
			else index--;
			deque.removeAt(index);
		}

		public void reset () {
			index = descending ? deque.size - 1 : 0;
		}

		public CharIterator iterator () {
			return this;
		}
	}

	public static CharDeque with(char item){
		CharDeque deque = new CharDeque();
		deque.add(item);
		return deque;
	}
	
	public static CharDeque with(char... items){
		return new CharDeque(items);
	}
}
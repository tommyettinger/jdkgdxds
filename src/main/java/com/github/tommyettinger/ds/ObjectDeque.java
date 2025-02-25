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

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;

/**
 * A resizable, insertion-ordered double-ended queue of objects with efficient add and remove at the beginning and end.
 * This also implements the {@link List} interface (except for {@link #subList(int, int)}).
 * Values in the backing array may wrap back to the beginning, making add and remove at the beginning and end O(1)
 * (unless the backing array needs to resize when adding). Deque functionality is provided via {@link #removeLast()} and
 * {@link #addFirst(Object)}.
 * <br>
 * Unlike most Deque implementations in the JDK, you can get and set items anywhere in the deque in constant time with
 * {@link #get(int)} and {@link #set(int, Object)}. Relative to an {@link ObjectList}, {@link #get(int)} has slightly
 * higher overhead, but it still runs in constant time. Unlike ArrayDeque in the JDK, this implements
 * {@link #equals(Object)} and {@link #hashCode()}, as well as {@link #equalsIdentity(Object)}. This can provide
 * full-blown {@link ListIterator ListIterators} for iteration from an index or in reverse order.
 * <br>
 * In general, this is an improvement over {@link ArrayDeque} in every type of functionality, and is mostly equivalent
 * to {@link ObjectList} as long as you don't need {@link #subList(int, int)} and the performance of {@link #get(int)}
 * is adequate. Because it is array-backed, it should usually be much faster than {@link LinkedList}, as well; only
 * periodic resizing and modifications in the middle of the List using an iterator should be typically faster for
 * {@link LinkedList}.
 */
public class ObjectDeque<T> implements Deque<T>, List<T>, Arrangeable, EnhancedCollection<T> {

	/**
	 * The value returned when nothing can be obtained from this deque and an exception is not meant to be thrown,
	 * such as when calling {@link #peek()} on an empty deque.
	 */
	@Nullable
	protected T defaultValue = null;
	/**
	 * Contains the values in the queue. Head and tail indices go in a circle around this array, wrapping at the end.
	 */
	protected @Nullable T[] values;

	/**
	 * Index of first element. Logically smaller than tail. Unless empty, it points to a valid element inside queue.
	 */
	protected int head = 0;

	/**
	 * Index of last element. Logically bigger than head. Usually points to an empty position, but points to the head when full
	 * {@code (size == values.length)}.
	 */
	protected int tail = 0;

	/**
	 * Number of elements in the queue.
	 */
	public int size = 0;

	protected transient @Nullable ObjectDequeIterator<T> iterator1;
	protected transient @Nullable ObjectDequeIterator<T> iterator2;

	protected transient @Nullable ObjectDequeIterator<T> descendingIterator1;
	protected transient @Nullable ObjectDequeIterator<T> descendingIterator2;

	/**
	 * Creates a new ObjectDeque which can hold 16 values without needing to resize backing array.
	 */
	public ObjectDeque () {
		this(16);
	}

	/**
	 * Creates a new ObjectDeque which can hold the specified number of values without needing to resize backing array.
	 */
	public ObjectDeque (int initialSize) {
		// noinspection unchecked
		this.values = (T[])new Object[initialSize];
	}

	/**
	 * Creates a new ObjectDeque using all the contents of the given Collection.
	 *
	 * @param coll a Collection of T that will be copied into this and used in full
	 */
	public ObjectDeque (Collection<? extends T> coll) {
		this(coll.size());
		addAll(coll);
	}

	/**
	 * Creates a new instance containing the items in the specified iterator.
	 *
	 * @param coll an iterator that will have its remaining contents added to this
	 */
	public ObjectDeque (Iterator<? extends T> coll) {
		this();
		addAll(coll);
	}

	/**
	 * Copies the given ObjectDeque exactly into this one. Individual values will be shallow-copied.
	 *
	 * @param deque another ObjectDeque to copy
	 */
	public ObjectDeque (ObjectDeque<? extends T> deque) {
		this.values = Arrays.copyOf(deque.values, deque.values.length);
		this.size = deque.size;
		this.head = deque.head;
		this.tail = deque.tail;
		this.defaultValue = deque.defaultValue;
	}

	/**
	 * Creates a new ObjectDeque using all the contents of the given array.
	 *
	 * @param a an array of T that will be copied into this and used in full
	 */
	public ObjectDeque (T[] a) {
		tail = a.length;
		this.values = Arrays.copyOf(a, tail);
		size = tail;
	}

	/**
	 * Creates a new ObjectDeque using {@code count} items from {@code a}, starting at {@code offset}.
	 *
	 * @param a      an array of T
	 * @param offset where in {@code a} to start using items
	 * @param count  how many items to use from {@code a}
	 */
	public ObjectDeque (T[] a, int offset, int count) {
		this.values = Arrays.copyOfRange(a, offset, offset + count);
		tail = count;
		size = count;
	}

	@Nullable
	public T getDefaultValue () {
		return defaultValue;
	}

	public void setDefaultValue (@Nullable T defaultValue) {
		this.defaultValue = defaultValue;
	}

	/**
	 * Append given object to the tail (enqueue to tail). Unless backing array needs resizing, operates in O(1) time.
	 *
	 * @param object can be null
	 */
	public void addLast (@Nullable T object) {
		@Nullable T[] values = this.values;

		if (size == values.length) {
			resize(values.length << 1);
			values = this.values;
		}

		if (tail == values.length) {
			tail = 0;
		}
		values[tail++] = object;
		size++;
	}

	/**
	 * Prepend given object to the head (enqueue to head). Unless backing array needs resizing, operates in O(1) time.
	 *
	 * @param object can be null
	 * @see #addLast(Object)
	 */
	public void addFirst (@Nullable T object) {
		@Nullable T[] values = this.values;

		if (size == values.length) {
			resize(values.length << 1);
			values = this.values;
		}

		int head = this.head;
		head--;
		if (head == -1) {
			head = values.length - 1;
		}
		values[head] = object;

		this.head = head;
		size++;
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
		final @Nullable T[] values = this.values;
		final int head = this.head;
		final int tail = this.tail;

		final @Nullable T[] newArray = (T[])new Object[Math.max(1, newSize)];
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
	 * @return removed object
	 * @throws NoSuchElementException when queue is empty
	 */
	@Nullable
	public T removeFirst () {
		if (size == 0) {
			// Underflow
			throw new NoSuchElementException("ObjectDeque is empty.");
		}

		final @Nullable T[] values = this.values;

		final T result = values[head];
		values[head] = null;
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
	 * @return removed object
	 * @throws NoSuchElementException when queue is empty
	 * @see #removeFirst()
	 */
	@Nullable
	public T removeLast () {
		if (size == 0) {
			throw new NoSuchElementException("ObjectDeque is empty.");
		}

		final @Nullable T[] values = this.values;
		int tail = this.tail;
		tail--;
		if (tail == -1) {
			tail = values.length - 1;
		}
		final T result = values[tail];
		values[tail] = null;
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
	@Override
	public boolean offerFirst (@Nullable T t) {
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
	@Override
	public boolean offerLast (@Nullable T t) {
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
	@Override
	@Nullable
	public T pollFirst () {
		if (size == 0) {
			// Underflow
			return defaultValue;
		}

		final @Nullable T[] values = this.values;

		final T result = values[head];
		values[head] = null;
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
	@Override
	@Nullable
	public T pollLast () {
		if (size == 0) {
			return defaultValue;
		}

		final @Nullable T[] values = this.values;
		int tail = this.tail;
		tail--;
		if (tail == -1) {
			tail = values.length - 1;
		}
		final T result = values[tail];
		values[tail] = null;
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
	@Override
	@Nullable
	public T getFirst () {
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
	@Override
	@Nullable
	public T getLast () {
		return last();
	}

	/**
	 * Retrieves, but does not remove, the first element of this deque,
	 * or returns {@link #getDefaultValue() defaultValue} if this deque is empty.
	 *
	 * @return the head of this deque, or {@link #getDefaultValue() defaultValue} if this deque is empty
	 */
	@Override
	@Nullable
	public T peekFirst () {
		if (size == 0) {
			// Underflow
			return defaultValue;
		}
		return values[head];
	}

	/**
	 * Retrieves, but does not remove, the last element of this deque,
	 * or returns {@link #getDefaultValue() defaultValue} if this deque is empty.
	 *
	 * @return the tail of this deque, or {@link #getDefaultValue() defaultValue} if this deque is empty
	 */
	@Override
	@Nullable
	public T peekLast () {
		if (size == 0) {
			// Underflow
			return defaultValue;
		}
		final @Nullable T[] values = this.values;
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
	@Override
	public boolean removeFirstOccurrence (@Nullable Object o) {
		return removeValue(o, false);
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
	@Override
	public boolean removeLastOccurrence (@Nullable Object o) {
		return removeLastValue(o, false);
	}

	/**
	 * Inserts the specified element into the queue represented by this deque
	 * (in other words, at the tail of this deque) if it is possible to do so
	 * immediately without violating capacity restrictions, returning
	 * {@code true} upon success and throwing an
	 * {@code IllegalStateException} if no space is currently available.
	 * When using a capacity-restricted deque, it is generally preferable to
	 * use {@link #offer(Object) offer}.
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
	public boolean add (@Nullable T t) {
		int oldSize = size;
		addLast(t);
		return oldSize != size;
	}

	/**
	 * Inserts the specified element into this deque at the specified index.
	 * Unlike {@link #offerFirst(Object)} and {@link #offerLast(Object)}, this does not run in expected constant time unless
	 * the index is less than or equal to 0 (where it acts like offerFirst()) or greater than or equal to {@link #size()}
	 * (where it acts like offerLast()).
	 * @param index the index in the deque's insertion order to insert the item
	 * @param item a T item to insert; may be null
	 * @return true if this deque was modified
	 */
	public void add (int index, @Nullable T item) {
		insert(index, item);
	}

	/**
	 * This is an alias for {@link #add(int, Object)} that returns {@code true} to indicate it does modify
	 * this ObjectDeque.
	 *
	 * @param index index at which the specified element is to be inserted
	 * @param item  element to be inserted
	 * @return true if this was modified, which should always happen
	 */
	public boolean insert (int index, @Nullable T item) {
		int oldSize = size;
		if(index <= 0)
			addFirst(item);
		else if(index >= oldSize)
			addLast(item);
		else {
			@Nullable T[] values = this.values;

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
	@Override
	public boolean offer (@Nullable T t) {
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
	@Override
	@Nullable
	public T remove () {
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
	@Override
	@Nullable
	public T poll () {
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
	@Override
	@Nullable
	public T element () {
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
	@Override
	@Nullable
	public T peek () {
		return peekFirst();
	}

	/**
	 * Adds all the elements in the specified collection at the end
	 * of this deque, as if by calling {@link #addLast} on each one,
	 * in the order that they are returned by the collection's iterator.
	 *
	 * <p>When using a capacity-restricted deque, it is generally preferable
	 * to call {@link #offer(Object) offer} separately on each element.
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
	public boolean addAll (Collection<? extends T> c) {
		int oldSize = size;
		if(c.isEmpty()) return false;
		ensureCapacity(Math.max(c.size(), oldSize));
		for (T t : c) {
			addLast(t);
		}
		return oldSize != size;
	}

	/**
	 * An alias for {@link #addAll(Collection)}, this adds every item in {@code c} to this in order at the end.
	 * @param c the elements to be inserted into this deque
	 * @return {@code true} if this deque changed as a result of the call
	 */
	public boolean addAllLast (Collection<? extends T> c) {
		return addAll(c);
	}

	/**
	 * Adds every item in {@code c} to this in order at the start. The iteration order of {@code c} will be preserved
	 * for the added items.
	 * @param c the elements to be inserted into this deque
	 * @return {@code true} if this deque changed as a result of the call
	 */
	public boolean addAllFirst (Collection<@Nullable ? extends T> c) {
		@Nullable T[] values = this.values;
		final int cs = c.size();
		if(cs == 0) return false;
		if (size + cs > values.length) {
			ensureCapacity(Math.max(cs, size));
			values = this.values;
		}

		int head = this.head;
		head -= cs;
		if (head < 0) {
			head = values.length - cs;
		}
		int index = 0;
		for(T object : c){
			values[head + index++] = object;
		}

		this.head = head;
		size += cs;
		return true;
	}

	/**
	 * An alias for {@link #addAll(int, Collection)}; inserts all elements
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
	 * @param c collection containing elements to be added to this list
	 * @return {@code true} if this list changed as a result of the call
	 */
	public boolean insertAll(int index, Collection<? extends T> c) {
		return addAll(index, c);
	}

	@Override
	public boolean addAll(int index, Collection<? extends T> c) {
		int oldSize = size;
		if(index <= 0)
			addAllFirst(c);
		else if(index >= oldSize)
			addAll(c);
		else {
			@Nullable T[] values = this.values;
			final int cs = c.size();
			if(cs == 0) return false;
			if (size + cs > values.length) {
				ensureCapacity(Math.max(cs, oldSize));
				values = this.values;
			}

			if(head < tail) {
				index += head;
				if(index + cs > values.length) index -= values.length;
				System.arraycopy(values, index, values, (index + cs) % values.length, tail - index);
				for(T item : c) {
					values[index++] = item;
				}
				tail += cs;
				if (tail > values.length) {
					tail = 1;
				}
			} else {
				if (head + index < values.length) {
					// backward shift
					System.arraycopy(values, head, values, head - cs, index);
					head -= cs;
					for(T item : c) {
						values[head + index++] = item;
					}
					// don't need to check for head being negative, because head is always > tail
				}
				else {
					// forward shift
					index -= values.length - cs;
					System.arraycopy(values, head + index, values, head + index + cs, tail - head - index);
					for(T item : c) {
						values[head + index++] = item;
					}
					tail += cs;
					// again, don't need to check for tail going around, because the head is in the way and doesn't need to move
				}
			}
			size += cs;
		}
		return oldSize != size;
	}

	/**
	 * Exactly like {@link #addAll(Collection)}, but takes an array instead of a Collection.
	 * @see #addAll(Collection)
	 * @param array the elements to be inserted into this deque
	 * @return {@code true} if this deque changed as a result of the call
	 */
	public boolean addAll (T[] array) {
		return addAll(array, 0, array.length);
	}
	/**
	 * Like {@link #addAll(Object[])}, but only uses at most {@code length} items from {@code array}, starting at {@code offset}.
	 * @see #addAll(Object[])
	 * @param array the elements to be inserted into this deque
	 * @param offset the index of the first item in array to add
	 * @param length how many items, at most, to add from array into this
	 * @return {@code true} if this deque changed as a result of the call
	 */
	public boolean addAll (T[] array, int offset, int length) {
		int oldSize = size;
		int cs = Math.min(array.length - offset, length);
		if(cs <= 0) return false;
		ensureCapacity(Math.max(oldSize, cs));
		for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
			addLast(array[i]);
		}
		return oldSize != size;
	}

	/**
	 * Exactly like {@link #addAllFirst(Collection)}, but takes an array instead of a Collection.
	 * @see #addAllFirst(Collection)
	 * @param array the elements to be inserted into this deque
	 * @return {@code true} if this deque changed as a result of the call
	 */
	public boolean addAllFirst (T[] array) {
		return addAllFirst(array, 0, array.length);
	}

	/**
	 * Like {@link #addAllFirst(Object[])}, but only uses at most {@code length} items from {@code array}, starting at
	 * {@code offset}. The order of {@code array} will be preserved, starting at the head of the deque.
	 * @see #addAllFirst(Object[])
	 * @param array the elements to be inserted into this deque
	 * @param offset the index of the first item in array to add
	 * @param length how many items, at most, to add from array into this
	 * @return {@code true} if this deque changed as a result of the call
	 */
	public boolean addAllFirst (T[] array, int offset, int length) {
		@Nullable T[] values = this.values;
		final int cs = Math.min(array.length - offset, length);
		if(cs <= 0) return false;
		if (size + cs > values.length) {
			ensureCapacity(Math.max(cs, size));
			values = this.values;
		}

		int head = this.head;
		head -= cs;
		if (head < 0) {
			head = values.length - cs;
		}
		System.arraycopy(array, offset, values, head, cs);

		this.head = head;
		size += cs;
		return true;
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
	@Override
	public void push (@Nullable T t) {
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
	@Override
	@Nullable
	public T pop () {
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
	 * <p>This method is equivalent to {@link #removeFirstOccurrence(Object)}.
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
	@Override
	public boolean remove (@Nullable Object o) {
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
	@Override
	public boolean contains (@Nullable Object o) {
		return indexOf(o, false) != -1;
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
	 * Returns an array containing all the elements in this collection.
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
	 * type} is {@code Object}, containing all the elements in this collection
	 */
	@Override
	public Object @NonNull [] toArray () {
		Object[] next = new Object[size];
		if (head < tail) {
			System.arraycopy(values, head, next, 0, tail - head);
		} else {
			System.arraycopy(values, head, next, 0, size - head);
			System.arraycopy(values, 0, next, size - head, tail);
		}
		return next;
	}

	/**
	 * Returns an array containing all the elements in this collection;
	 * the runtime type of the returned array is that of the specified array.
	 * If the collection fits in the specified array, it is returned therein.
	 * Otherwise, a new array is allocated with the runtime type of the
	 * specified array and the size of this collection.
	 *
	 * <p>If this collection fits in the specified array with room to spare
	 * (i.e., the array has more elements than this collection), the element
	 * in the array immediately following the end of the collection is set to
	 * {@code null}.  (This is useful in determining the length of this
	 * collection <i>only</i> if the caller knows that this collection does
	 * not contain any {@code null} elements.)
	 *
	 * <p>If this collection makes any guarantees as to what order its elements
	 * are returned by its iterator, this method must return the elements in
	 * the same order.
	 *
	 * @param a the array into which the elements of this collection are to be
	 *          stored, if it is big enough; otherwise, a new array of the same
	 *          runtime type is allocated for this purpose.
	 * @return an array containing all the elements in this collection
	 * @throws ArrayStoreException  if the runtime type of any element in this
	 *                              collection is not assignable to the {@linkplain Class#getComponentType
	 *                              runtime component type} of the specified array
	 * @throws NullPointerException if the specified array is null
	 */
	@Override
	public <E> @Nullable  E @NonNull [] toArray (@Nullable E[] a) {
		int oldSize = size;
		if (a.length < oldSize) {
			a = Arrays.copyOf(a, oldSize);
		}
		@Nullable Object[] result = a;
		Iterator<T> it = iterator();
		for (int i = 0; i < oldSize; ++i) {
			result[i] = it.next();
		}
		if (a.length > oldSize) {
			a[oldSize] = null;
		}
		return a;
	}

	/**
	 * Returns {@code true} if this collection contains all the elements
	 * in the specified collection.
	 *
	 * @param c collection to be checked for containment in this collection
	 * @return {@code true} if this collection contains all the elements
	 * in the specified collection
	 * @throws ClassCastException   if the types of one or more elements
	 *                              in the specified collection are incompatible with this
	 *                              collection
	 *                              (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>)
	 * @throws NullPointerException if the specified collection contains one
	 *                              or more null elements and this collection does not permit null
	 *                              elements
	 *                              (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>),
	 *                              or if the specified collection is null.
	 * @see #contains(Object)
	 */
	@Override
	public boolean containsAll (Collection<?> c) {
		for (Object o : c) {
			if (!contains(o))
				return false;
		}
		return true;
	}

	/**
	 * Exactly like {@link #containsAll(Collection)}, but takes an array instead of a Collection.
	 * @see #containsAll(Collection)
	 * @param array array to be checked for containment in this deque
	 * @return {@code true} if this deque contains all the elements
	 * in the specified array
	 */
	public boolean containsAll (Object[] array) {
		for (Object o : array) {
			if (!contains(o))
				return false;
		}
		return true;
	}

	/**
	 * Like {@link #containsAll(Object[])}, but only uses at most {@code length} items from {@code array}, starting at {@code offset}.
	 * @see #containsAll(Object[])
	 * @param array array to be checked for containment in this deque
	 * @param offset the index of the first item in array to check
	 * @param length how many items, at most, to check from array
	 * @return {@code true} if this deque contains all the elements
	 * in the specified range of array
	 */
	public boolean containsAll (Object[] array, int offset, int length) {
		for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
			if(!contains(array[i])) return false;
		}
		return true;
	}

	/**
	 * Returns true if this ObjectDeque contains any of the specified values.
	 *
	 * @param values may contain nulls, but must not be null itself
	 * @return true if this ObjectDeque contains any of the items in {@code values}, false otherwise
	 */
	public boolean containsAny (Iterable<?> values) {
		for (Object v : values) {
			if (contains(v)) {return true;}
		}
		return false;
	}

	/**
	 * Returns true if this ObjectDeque contains any of the specified values.
	 *
	 * @param values may contain nulls, but must not be null itself
	 * @return true if this ObjectDeque contains any of the items in {@code values}, false otherwise
	 */
	public boolean containsAny (Object[] values) {
		for (Object v : values) {
			if (contains(v)) {return true;}
		}
		return false;
	}

	/**
	 * Returns true if this ObjectDeque contains any items from the specified range of values.
	 *
	 * @param values may contain nulls, but must not be null itself
	 * @param offset the index to start checking in values
	 * @param length how many items to check from values
	 * @return true if this ObjectDeque contains any of the items in the given range of {@code values}, false otherwise
	 */
	public boolean containsAny (Object[] values, int offset, int length) {
		for (int i = offset, n = 0; n < length && i < values.length; i++, n++) {
			if (contains(values[i])) {return true;}
		}
		return false;
	}

	/**
	 * Removes all of this collection's elements that are also contained in the
	 * specified collection (optional operation).  After this call returns,
	 * this collection will contain no elements in common with the specified
	 * collection.
	 *
	 * @param  other collection containing elements to be removed from this collection
	 * @return {@code true} if this deque changed as a result of the call
	 * @throws UnsupportedOperationException if the {@code removeAll} method
	 *                                       is not supported by this collection
	 * @throws ClassCastException            if the types of one or more elements
	 *                                       in this collection are incompatible with the specified
	 *                                       collection
	 *                                       (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>)
	 * @throws NullPointerException          if this collection contains one or more
	 *                                       null elements and the specified collection does not support
	 *                                       null elements
	 *                                       (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>),
	 *                                       or if the specified collection is null
	 * @see #remove(Object)
	 * @see #contains(Object)
	 */
	@Override
	public boolean removeAll (Collection<?> other) {
		ObjectDequeIterator<?> me = iterator();
		int originalSize = size();
		for (Object item : other) {
			me.reset();
			while (me.hasNext()) {
				if (Objects.equals(me.next(), item)) {
					me.remove();
				}
			}
		}
		return originalSize != size();
	}

	/**
	 * Exactly like {@link #removeAll(Collection)}, but takes an array instead of a Collection.
	 * @see #removeAll(Collection)
	 * @param other array containing elements to be removed from this collection
	 * @return {@code true} if this deque changed as a result of the call
	 */
	public boolean removeAll (Object[] other) {
		return removeAll(other, 0, other.length);
	}
	/**
	 * Like {@link #removeAll(Object[])}, but only uses at most {@code length} items from {@code array}, starting at {@code offset}.
	 * @see #removeAll(Object[])
	 * @param array the elements to be removed from this deque
	 * @param offset the index of the first item in array to remove
	 * @param length how many items, at most, to get from array and remove from this
	 * @return {@code true} if this deque changed as a result of the call
	 */
	public boolean removeAll (Object[] array, int offset, int length) {
		ObjectDequeIterator<?> me = iterator();
		int originalSize = size();
		for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
			Object item = array[i];
			me.reset();
			while (me.hasNext()) {
				if (Objects.equals(me.next(), item)) {
					me.remove();
				}
			}
		}
		return originalSize != size();
	}

	/**
	 * Removes from this collection element-wise occurrences of elements contained in the specified other collection.
	 * Note that if a value is present more than once in this collection, only one of those occurrences
	 * will be removed for each occurrence of that value in {@code other}. If {@code other} has the same
	 * contents as this collection or has additional items, then removing each of {@code other} will clear this.
	 *
	 * @param other a Collection of items to remove one-by-one, such as an ObjectList or an ObjectSet
	 * @return true if this deque was modified.
	 */
	public boolean removeEach (Iterable<?> other) {
		boolean changed = false;
		for(Object item : other) {
			changed |= remove(item);
		}
		return changed;
	}

	/**
	 * Exactly like {@link #removeEach(Iterable)}, but takes an array instead of a Collection.
	 * @see #removeEach(Iterable)
	 * @param array array containing elements to be removed from this collection
	 * @return {@code true} if this deque changed as a result of the call
	 */
	public boolean removeEach (Object[] array) {
		return removeEach(array, 0, array.length);
	}

	/**
	 * Like {@link #removeEach(Object[])}, but only uses at most {@code length} items from {@code array}, starting at {@code offset}.
	 * @see #removeEach(Object[])
	 * @param array the elements to be removed from this deque
	 * @param offset the index of the first item in array to remove
	 * @param length how many items, at most, to get from array and remove from this
	 * @return {@code true} if this deque changed as a result of the call
	 */
	public boolean removeEach (Object[] array, int offset, int length) {
		boolean changed = false;
		for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
			changed |= remove(array[i]);
		}
		return changed;
	}

	/**
	 * Retains only the elements in this collection that are contained in the
	 * specified collection (optional operation).  In other words, removes from
	 * this collection all of its elements that are not contained in the
	 * specified collection.
	 *
	 * @param c collection containing elements to be retained in this collection
	 * @return {@code true} if this collection changed as a result of the call
	 * @throws UnsupportedOperationException if the {@code retainAll} operation
	 *                                       is not supported by this collection
	 * @throws ClassCastException            if the types of one or more elements
	 *                                       in this collection are incompatible with the specified
	 *                                       collection
	 *                                       (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>)
	 * @throws NullPointerException          if this collection contains one or more
	 *                                       null elements and the specified collection does not permit null
	 *                                       elements
	 *                                       (<a href="{@docRoot}/java.base/java/util/Collection.html#optional-restrictions">optional</a>),
	 *                                       or if the specified collection is null
	 * @see #remove(Object)
	 * @see #contains(Object)
	 */
	@Override
	public boolean retainAll (Collection<?> c) {
		int oldSize = size;
		for (Object o : c) {
			int idx;
			do {
				if ((idx = indexOf(o, false)) != -1)
					remove(idx);
			} while (idx == -1);
		}
		return oldSize != size;
	}

	/**
	 * Exactly like {@link #retainAll(Collection)}, but takes an array instead of a Collection.
	 * @see #retainAll(Collection)
	 * @param array array containing elements to be retained in this collection
	 * @return {@code true} if this deque changed as a result of the call
	 */
	public boolean retainAll (Object[] array) {
		int oldSize = size;
		for (Object o : array) {
			int idx;
			do {
				if ((idx = indexOf(o, false)) != -1)
					remove(idx);
			} while (idx == -1);
		}
		return oldSize != size;
	}

	/**
	 * Like {@link #retainAll(Object[])}, but only uses at most {@code length} items from {@code array}, starting at {@code offset}.
	 * @see #retainAll(Object[])
	 * @param array the elements to be retained in this deque
	 * @param offset the index of the first item in array to retain
	 * @param length how many items, at most, to retain from array in this
	 * @return {@code true} if this deque changed as a result of the call
	 */
	public boolean retainAll (Object[] array, int offset, int length) {
		int oldSize = size;
		for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
			Object o = array[i];
			int idx;
			do {
				if ((idx = indexOf(o, false)) != -1)
					remove(idx);
			} while (idx == -1);
		}
		return oldSize != size;
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
				Arrays.fill(values, head + newSize, tail, null);
				tail -= size() - newSize;
				size = newSize;
			} else if(head + newSize < values.length) {
				// tail is near the start, but we have to remove elements through the start and into the back
				Arrays.fill(values, 0, tail, null);
				tail = head + newSize;
				Arrays.fill(values, tail, values.length, null);
				size = newSize;
			} else {
				// tail is near the start, but we only have to remove some elements between tail and the start
				final int newTail = tail - (size() - newSize);
				Arrays.fill(values, newTail, tail, null);
				tail = newTail;
				size = newSize;
			}
		}
	}

	/**
	 * Returns the index of the first occurrence of value in the queue, or -1 if no such value exists.
	 * Uses .equals() to compare items.
	 *
	 * @return An index of the first occurrence of value in queue or -1 if no such value exists
	 */
	public int indexOf (@Nullable Object value) {
		return indexOf(value, false);
	}

	/**
	 * Returns the index of first occurrence of value in the queue, or -1 if no such value exists.
	 *
	 * @param identity If true, == comparison will be used. If false, .equals() comparison will be used.
	 * @return An index of first occurrence of value in queue or -1 if no such value exists
	 */
	public int indexOf (@Nullable Object value, boolean identity) {
		if (size == 0)
			return -1;
		@Nullable T[] values = this.values;
		final int head = this.head, tail = this.tail;
		if (identity || value == null) {
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
		} else {
			if (head < tail) {
				for (int i = head; i < tail; i++)
					if (value.equals(values[i]))
						return i - head;
			} else {
				for (int i = head, n = values.length; i < n; i++)
					if (value.equals(values[i]))
						return i - head;
				for (int i = 0; i < tail; i++)
					if (value.equals(values[i]))
						return i + values.length - head;
			}
		}
		return -1;
	}

	/**
	 * Returns the index of the last occurrence of value in the queue, or -1 if no such value exists.
	 * Uses .equals() to compare items.
	 *
	 * @return An index of the last occurrence of value in queue or -1 if no such value exists
	 */
	public int lastIndexOf (@Nullable Object value) {
		return lastIndexOf(value, false);
	}

	/**
	 * Returns the index of last occurrence of value in the queue, or -1 if no such value exists.
	 *
	 * @param identity If true, == comparison will be used. If false, .equals() comparison will be used.
	 * @return An index of last occurrence of value in queue or -1 if no such value exists
	 */
	public int lastIndexOf (@Nullable Object value, boolean identity) {
		if (size == 0)
			return -1;
		@Nullable T[] values = this.values;
		final int head = this.head, tail = this.tail;
		if (identity || value == null) {
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
		} else {
			if (head < tail) {
				for (int i = tail - 1; i >= head; i--)
					if (value.equals(values[i]))
						return i - head;
			} else {
				for (int i = tail - 1; i >= 0; i--)
					if (value.equals(values[i]))
						return i + values.length - head;
				for (int i = values.length - 1; i >= head; i--)
					if (value.equals(values[i]))
						return i - head;
			}
		}
		return -1;
	}

	@Override
	public ListIterator<T> listIterator() {
		if (iterator1 == null || iterator2 == null) {
			iterator1 = new ObjectDequeIterator<>(this);
			iterator2 = new ObjectDequeIterator<>(this);
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
	public ListIterator<T> listIterator(int index) {
		if (iterator1 == null || iterator2 == null) {
			iterator1 = new ObjectDequeIterator<>(this, index, false);
			iterator2 = new ObjectDequeIterator<>(this, index, false);
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
	 * Not implemented and not intended to be; throws an {@link UnsupportedOperationException} always.
	 * @param fromIndex ignored
	 * @param toIndex ignored
	 * @return never
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public List<T> subList(int fromIndex, int toIndex) {
		throw new UnsupportedOperationException("subList() is not supported.");
	}

	/**
	 * Removes the first instance of the specified value in the queue.
	 *
	 * @param identity If true, == comparison will be used. If false, .equals() comparison will be used.
	 * @return true if value was found and removed, false otherwise
	 */
	public boolean removeValue (@Nullable Object value, boolean identity) {
		int index = indexOf(value, identity);
		if (index == -1)
			return false;
		remove(index);
		return true;
	}

	/**
	 * Removes the last instance of the specified value in the queue.
	 *
	 * @param identity If true, == comparison will be used. If false, .equals() comparison will be used.
	 * @return true if value was found and removed, false otherwise
	 */
	public boolean removeLastValue (@Nullable Object value, boolean identity) {
		int index = lastIndexOf(value, identity);
		if (index == -1)
			return false;
		remove(index);
		return true;
	}

	/**
	 * Removes and returns the item at the specified index.
	 * This is an alias for {@link #remove(int)} for compatibility with primitive-backed lists and deques.
	 */
	@Nullable
	public T removeAt(int index) {
		return remove(index);
	}

	@Override
	@Nullable
	public T remove(int index) {
		if (index <= 0)
			return removeFirst();
		if (index >= size)
			return removeLast();

		@Nullable T[] values = this.values;
		int head = this.head, tail = this.tail;
		index += head;
		T value;
		if (head < tail) { // index is between head and tail.
			value = values[index];
			System.arraycopy(values, index + 1, values, index, tail - index - 1);
			this.tail--;
			values[this.tail] = null;
		} else if (index >= values.length) { // index is between 0 and tail.
			index -= values.length;
			value = values[index];
			System.arraycopy(values, index + 1, values, index, tail - index - 1);
			this.tail--;
			values[this.tail] = null;
		} else { // index is between head and values.length.
			value = values[index];
			System.arraycopy(values, head, values, head + 1, index - head);
			values[this.head] = null;
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
	@Override
	public boolean isEmpty () {
		return size == 0;
	}

	/**
	 * Returns the first (head) item in the queue (without removing it).
	 *
	 * @throws NoSuchElementException when queue is empty
	 * @see #addFirst(Object)
	 * @see #removeFirst()
	 */
	@Override
	public @Nullable T first () {
		if (size == 0) {
			// Underflow
			throw new NoSuchElementException("ObjectDeque is empty.");
		}
		return values[head];
	}

	/**
	 * Returns the last (tail) item in the queue (without removing it).
	 *
	 * @throws NoSuchElementException when queue is empty
	 * @see #addLast(Object)
	 * @see #removeLast()
	 */
	public @Nullable T last () {
		if (size == 0) {
			// Underflow
			throw new NoSuchElementException("ObjectDeque is empty.");
		}
		final @Nullable T[] values = this.values;
		int tail = this.tail;
		tail--;
		if (tail == -1)
			tail = values.length - 1;
		return values[tail];
	}

	@Override
	public @Nullable T get (int index) {
		if (index <= 0)
			return getFirst();
		if (index >= size - 1)
			return getLast();
		final @Nullable T[] values = this.values;

		int i = head + index;
		if (i >= values.length)
			i -= values.length;
		return values[i];
	}

	@Override
	public @Nullable T set (int index, @Nullable T item) {
		if (index < 0)
			throw new IndexOutOfBoundsException("index can't be < 0: " + index);
		if (index >= size)
			throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);
		final @Nullable T[] values = this.values;

		int i = head + index;
		if (i >= values.length)
			i -= values.length;
		T old = values[i];
		values[i] = item;
		return old;
	}

	/**
	 * Removes all values from this queue. Values in backing array are set to null to prevent memory leaks, so this
	 * operates in O(n) time.
	 */
	@Override
	public void clear () {
		if (size == 0)
			return;
		final @Nullable T[] values = this.values;
		final int head = this.head;
		final int tail = this.tail;

		if (head < tail) {
			// Continuous
			Utilities.clear(values, head, tail - head);
		} else {
			// Wrapped
			Utilities.clear(values, head, values.length - head);
			Utilities.clear(values, 0, tail);
		}
		this.head = 0;
		this.tail = 0;
		this.size = 0;
	}

	/**
	 * Returns an iterator for the items in the deque. Remove is supported.
	 * <br>
	 * Reuses one of two iterators for this deque. For nested or multithreaded
	 * iteration, use {@link ObjectDequeIterator#ObjectDequeIterator(ObjectDeque)}.
	 */
	@Override
	public @NonNull ObjectDequeIterator<T> iterator () {
		if (iterator1 == null || iterator2 == null) {
			iterator1 = new ObjectDequeIterator<>(this);
			iterator2 = new ObjectDequeIterator<>(this);
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
	 * iteration, use {@link ObjectDequeIterator#ObjectDequeIterator(ObjectDeque, boolean)}.
	 *
	 * @return an iterator over the elements in this deque in reverse sequence
	 */
	@Override
	public @NonNull ObjectDequeIterator<T> descendingIterator () {
		if (descendingIterator1 == null || descendingIterator2 == null) {
			descendingIterator1 = new ObjectDequeIterator<>(this, true);
			descendingIterator2 = new ObjectDequeIterator<>(this, true);
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
		final @Nullable T[] values = this.values;
		final int backingLength = values.length;
		int index = this.head;

		int hash = size + 1;
		for (int s = 0; s < size; s++) {
			final T value = values[index];

			hash *= 31;
			if (value != null)
				hash += value.hashCode();

			index++;
			if (index == backingLength)
				index = 0;
		}

		return hash;
	}

	/**
	 * Using {@link Object#equals(Object)} between each item in order, compares for equality specifically with
	 * other ObjectDeque collections. If {@code o} is not an ObjectDeque
	 * (and is also not somehow reference-equivalent to this collection), this returns false.
	 * @param o object to be compared for equality with this collection
	 * @return true if this is equal to o, or false otherwise
	 */
	public boolean equals (Object o) {
		if (this == o)
			return true;
		if (!(o instanceof ObjectDeque))
			return false;

		ObjectDeque<?> q = (ObjectDeque<?>)o;
		final int size = this.size;

		if (q.size != size)
			return false;

		final @Nullable T[] myValues = this.values;
		final int myBackingLength = myValues.length;
		final Object[] itsValues = q.values;
		final int itsBackingLength = itsValues.length;

		int myIndex = head;
		int itsIndex = q.head;
		for (int s = 0; s < size; s++) {
			T myValue = myValues[myIndex];
			Object itsValue = itsValues[itsIndex];

			if (!(Objects.equals(myValue, itsValue)))
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
	 * Using {@code ==} between each item in order, compares for equality specifically with
	 * other ObjectDeque collections. If {@code o} is not an ObjectDeque
	 * (and is also not somehow reference-equivalent to this collection), this returns false.
	 * @param o object to be compared for equality with this collection
	 * @return true if this is equal to o, or false otherwise
	 */
	public boolean equalsIdentity (Object o) {
		if (this == o)
			return true;
		if (!(o instanceof ObjectDeque))
			return false;

		ObjectDeque<?> q = (ObjectDeque<?>)o;
		final int size = this.size;

		if (q.size != size)
			return false;

		final @Nullable T[] myValues = this.values;
		final int myBackingLength = myValues.length;
		final Object[] itsValues = q.values;
		final int itsBackingLength = itsValues.length;

		int myIndex = head;
		int itsIndex = q.head;
		for (int s = 0; s < size; s++) {
			if (myValues[myIndex] != itsValues[itsIndex])
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
		final @Nullable T[] values = this.values;

		int f = head + first;
		if (f >= values.length)
			f -= values.length;

		int s = head + second;
		if (s >= values.length)
			s -= values.length;

		T fv = values[f];
		values[f] = values[s];
		values[s] = fv;

	}

	/**
	 * Reverses this ObjectDeque in-place.
	 */
	@Override
	public void reverse () {
		final @Nullable T[] values = this.values;
		int f, s, len = values.length;
		T fv;
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
	 * Attempts to sort this deque in-place using its natural ordering, which requires T to
	 * implement {@link Comparable} of T.
	 */
	public void sort () {
		sort(null);
	}

	/**
	 * Sorts this deque in-place using {@link Arrays#sort(Object[], int, int, Comparator)}.
	 * This should operate in O(n log(n)) time or less when the internals of the deque are
	 * continuous (the head is before the tail in the array). If the internals are not
	 * continuous, this takes an additional O(n) step (where n is less than the size of
	 * the deque) to rearrange the internals before sorting. You can pass null as the value
	 * for {@code comparator} if T implements {@link Comparable} of T, which will make this
	 * use the natural ordering for T.
	 *
	 * @param comparator the Comparator to use for T items; may be null to use the natural
	 *                   order of T items when T implements Comparable of T
	 */
	public void sort (@Nullable Comparator<? super T> comparator) {
		if (head <= tail) {
			Arrays.sort(values, head, tail, comparator);
		} else {
			System.arraycopy(values, head, values, tail, values.length - head);
			Arrays.sort(values, 0, tail + values.length - head, comparator);
			tail = tail + values.length - head;
			head = 0;
		}
	}

	@Nullable
	public T random (Random random) {
		if (size <= 0) {
			throw new NoSuchElementException("ObjectDeque is empty.");
		}
		return get(random.nextInt(size));
	}

	/**
	 * An {@link Iterator} and {@link ListIterator} over the elements of an ObjectDeque, while also an {@link Iterable}.
	 * @param <T> the generic type for the ObjectDeque this iterates over
	 */
	public static class ObjectDequeIterator<T> implements Iterable<T>, ListIterator<T> {
		protected int index, latest = -1;
		protected ObjectDeque<T> deque;
		protected boolean valid = true;
		private final int direction;

		public ObjectDequeIterator (ObjectDeque<T> deque) {
			this(deque, false);
		}
		public ObjectDequeIterator (ObjectDeque<T> deque, boolean descendingOrder) {
			this.deque = deque;
			direction = descendingOrder ? -1 : 1;
		}

		public ObjectDequeIterator (ObjectDeque<T> deque, int index, boolean descendingOrder) {
			if (index < 0 || index >= deque.size())
				throw new IndexOutOfBoundsException("ObjectDequeIterator does not satisfy index >= 0 && index < deque.size()");
			this.deque = deque;
			this.index = index;
			direction = descendingOrder ? -1 : 1;
		}

		/**
		 * Returns the next {@code T} element in the iteration.
		 *
		 * @return the next {@code T} element in the iteration
		 * @throws NoSuchElementException if the iteration has no more elements
		 */
		@Override
		@Nullable
		public T next () {
			if (!hasNext()) {throw new NoSuchElementException();}
			latest = index;
			index += direction;
			return deque.get(latest);
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
			if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
			return direction == 1 ? index < deque.size() : index > 0 && deque.notEmpty();
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
		@Override
		public boolean hasPrevious () {
			if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
			return direction == -1 ? index < deque.size() : index > 0 && deque.notEmpty();
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
		@Override
		@Nullable
		public T previous () {
			if (!hasPrevious()) {throw new NoSuchElementException();}
			return deque.get(latest = (index -= direction));
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
		@Override
		public int nextIndex () {
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
		@Override
		public int previousIndex () {
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
		public void remove () {
			if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
			if (latest == -1 || latest >= deque.size()) {throw new NoSuchElementException();}
			deque.remove(latest);
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
		@Override
		public void set (@Nullable T t) {
			if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
			if (latest == -1 || latest >= deque.size()) {throw new NoSuchElementException();}
			deque.set(latest, t);
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
		@Override
		public void add (@Nullable T t) {
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
				throw new IndexOutOfBoundsException("ObjectDequeIterator does not satisfy index >= 0 && index < deque.size()");
			this.index = index;
			latest = -1;
		}

		/**
		 * Returns an iterator over elements of type {@code T}.
		 *
		 * @return a ListIterator; really this same ObjectDequeIterator.
		 */
		@Override
		public @NonNull ObjectDequeIterator<T> iterator () {
			return this;
		}
	}

	/**
	 * Constructs an empty deque given the type as a generic type argument.
	 * This is usually less useful than just using the constructor, but can be handy
	 * in some code-generation scenarios when you don't know how many arguments you will have.
	 *
	 * @param <T>    the type of items; must be given explicitly
	 * @return a new deque containing nothing
	 */
	public static <T> ObjectDeque<T> with () {
		return new ObjectDeque<>(0);
	}

	/**
	 * Creates a new ObjectDeque that holds only the given item, but can be resized.
	 * @param item one T item
	 * @return a new ObjectDeque that holds the given item
	 * @param <T> the type of item, typically inferred
	 */
	public static <T> ObjectDeque<T> with (T item) {
		ObjectDeque<T> deque = new ObjectDeque<>(1);
		deque.add(item);
		return deque;
	}

	/**
	 * Creates a new ObjectDeque that holds only the given items, but can be resized.
	 * @param item0 a T item
	 * @param item1 a T item
	 * @return a new ObjectDeque that holds the given items
	 * @param <T> the type of item, typically inferred
	 */
	public static <T> ObjectDeque<T> with (T item0, T item1) {
		ObjectDeque<T> deque = new ObjectDeque<>(2);
		deque.add(item0, item1);
		return deque;
	}

	/**
	 * Creates a new ObjectDeque that holds only the given items, but can be resized.
	 * @param item0 a T item
	 * @param item1 a T item
	 * @param item2 a T item
	 * @return a new ObjectDeque that holds the given items
	 * @param <T> the type of item, typically inferred
	 */
	public static <T> ObjectDeque<T> with (T item0, T item1, T item2) {
		ObjectDeque<T> deque = new ObjectDeque<>(3);
		deque.add(item0, item1, item2);
		return deque;
	}

	/**
	 * Creates a new ObjectDeque that holds only the given items, but can be resized.
	 * @param item0 a T item
	 * @param item1 a T item
	 * @param item2 a T item
	 * @param item3 a T item
	 * @return a new ObjectDeque that holds the given items
	 * @param <T> the type of item, typically inferred
	 */
	public static <T> ObjectDeque<T> with (T item0, T item1, T item2, T item3) {
		ObjectDeque<T> deque = new ObjectDeque<>(4);
		deque.add(item0, item1, item2, item3);
		return deque;
	}

	/**
	 * Creates a new ObjectDeque that holds only the given items, but can be resized.
	 * @param item0 a T item
	 * @param item1 a T item
	 * @param item2 a T item
	 * @param item3 a T item
	 * @param item4 a T item
	 * @return a new ObjectDeque that holds the given items
	 * @param <T> the type of item, typically inferred
	 */
	public static <T> ObjectDeque<T> with (T item0, T item1, T item2, T item3, T item4) {
		ObjectDeque<T> deque = new ObjectDeque<>(5);
		deque.add(item0, item1, item2, item3);
		deque.add(item4);
		return deque;
	}

	/**
	 * Creates a new ObjectDeque that holds only the given items, but can be resized.
	 * @param item0 a T item
	 * @param item1 a T item
	 * @param item2 a T item
	 * @param item3 a T item
	 * @param item4 a T item
	 * @param item5 a T item
	 * @return a new ObjectDeque that holds the given items
	 * @param <T> the type of item, typically inferred
	 */
	public static <T> ObjectDeque<T> with (T item0, T item1, T item2, T item3, T item4, T item5) {
		ObjectDeque<T> deque = new ObjectDeque<>(6);
		deque.add(item0, item1, item2, item3);
		deque.add(item4, item5);
		return deque;
	}

	/**
	 * Creates a new ObjectDeque that holds only the given items, but can be resized.
	 * @param item0 a T item
	 * @param item1 a T item
	 * @param item2 a T item
	 * @param item3 a T item
	 * @param item4 a T item
	 * @param item5 a T item
	 * @param item6 a T item
	 * @return a new ObjectDeque that holds the given items
	 * @param <T> the type of item, typically inferred
	 */
	public static <T> ObjectDeque<T> with (T item0, T item1, T item2, T item3, T item4, T item5, T item6) {
		ObjectDeque<T> deque = new ObjectDeque<>(7);
		deque.add(item0, item1, item2, item3);
		deque.add(item4, item5, item6);
		return deque;
	}

	/**
	 * Creates a new ObjectDeque that holds only the given items, but can be resized.
	 * @param item0 a T item
	 * @param item1 a T item
	 * @param item2 a T item
	 * @param item3 a T item
	 * @param item4 a T item
	 * @param item5 a T item
	 * @param item6 a T item
	 * @return a new ObjectDeque that holds the given items
	 * @param <T> the type of item, typically inferred
	 */
	public static <T> ObjectDeque<T> with (T item0, T item1, T item2, T item3, T item4, T item5, T item6, T item7) {
		ObjectDeque<T> deque = new ObjectDeque<>(8);
		deque.add(item0, item1, item2, item3);
		deque.add(item4, item5, item6, item7);
		return deque;
	}

	/**
	 * Creates a new ObjectDeque that will hold the items in the given array or varargs.
	 * This overload will only be used when an array is supplied and the type of the
	 * items requested is the component type of the array, or if varargs are used and
	 * there are 9 or more arguments.
	 * @param varargs either 0 or more T items, or an array of T
	 * @return a new ObjectDeque that holds the given T items
	 * @param <T> the type of items, typically inferred by all the items being the same type
	 */
	@SafeVarargs
	public static <T> ObjectDeque<T> with (T... varargs) {
		return new ObjectDeque<>(varargs);
	}
}
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

import com.github.tommyettinger.ds.support.sort.ObjectComparators;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;

/**
 * A resizable, insertion-ordered double-ended queue of objects with efficient add and remove at the beginning and end.
 * This implements both the {@link List} and {@link Deque} interfaces, and supports {@link RandomAccess}.
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
 * Unlike {@link ArrayDeque} or {@link ArrayList}, most methods that take an index here try to be "forgiving;" that is,
 * they treat negative indices as index 0, and too-large indices as the last index, rather than throwing an Exception,
 * except in some cases where the ObjectDeque is empty and an item from it is required. An exception is in
 * {@link #set(int, Object)}, which allows prepending by setting a negative index, or appending by setting a too-large
 * index. This isn't a standard JDK behavior, and it doesn't always act how Deque or List is documented.
 * <br>
 * Some new methods are present here, or have been made public when they weren't before. {@link #removeRange(int, int)},
 * for instance, is now public, as is {@link #resize(int)}. New APIs include Deque-like methods that affect the middle
 * of the deque, such as {@link #peekAt(int)} and {@link #pollAt(int)}. There are more bulk methods that work at the
 * head or tail region of the deque, such as {@link #addAllFirst(Collection)} and {@link #truncateFirst(int)}. There are
 * the methods from {@link Arrangeable}, and relevant ones from {@link Ordered} (this isn't Ordered because it doesn't
 * provide its order as an ObjectList, but can do similar things).
 * <br>
 * In general, this is an improvement over {@link ArrayDeque} in every type of functionality, and is mostly equivalent
 * to {@link ObjectList} as long as the performance of {@link #get(int)} is adequate. Because it is array-backed, it
 * should usually be much faster than {@link LinkedList}, as well; only periodic resizing and modifications in the
 * middle of the List using an iterator should be typically faster for {@link LinkedList}.
 */
public class ObjectDeque<@Nullable T> extends AbstractList<T> implements Deque<T>, List<T>, Lisque<T>, RandomAccess, Arrangeable, EnhancedCollection<T>, Arrangeable.ArrangeableList<T> {

	/**
	 * The value returned when nothing can be obtained from this deque and an exception is not meant to be thrown,
	 * such as when calling {@link #peek()} on an empty deque.
	 */
	@Nullable
	public T defaultValue = null;
	/**
	 * Contains the values in the deque. Head and tail indices go in a circle around this array, wrapping at the end.
	 */
	protected @Nullable T[] items;

	/**
	 * Index of first element. Logically smaller than tail. Unless empty, it points to a valid element inside the deque.
	 */
	protected int head = 0;

	/**
	 * Index of last element. Logically bigger than head. Unless empty, it points to a valid element inside the deque.
	 * This may be the same as head, and is if there is one element in the deque (or none), that will be the case.
	 */
	protected int tail = 0;

	/**
	 * Number of elements in the deque.
	 */
	public int size = 0;

	protected transient @Nullable ObjectDequeIterator<T> iterator1;
	protected transient @Nullable ObjectDequeIterator<T> iterator2;
	protected transient @Nullable ObjectDequeIterator<T> descendingIterator1;
	protected transient @Nullable ObjectDequeIterator<T> descendingIterator2;

	/**
	 * Creates a new ObjectDeque which can hold 16 values without needing to resize the backing array.
	 */
	public ObjectDeque() {
		this(16);
	}

	/**
	 * Creates a new ObjectDeque which can hold the specified number of values without needing to resize the backing
	 * array.
	 * @param initialSize how large the backing array should be, without any padding
	 */
	public ObjectDeque(int initialSize) {
		// noinspection unchecked
		this.items = (T[])new Object[Math.max(1, initialSize)];
	}

	/**
	 * Creates a new ObjectDeque using all the contents of the given Collection.
	 *
	 * @param coll a Collection of T that will be copied into this and used in full
	 * @throws NullPointerException if {@code coll} is {@code null}
	 */
	public ObjectDeque(Collection<? extends T> coll) {
		this(coll.size());
		addAll(coll);
	}

	/**
	 * Creates a new instance containing the items in the specified iterator.
	 *
	 * @param iter an iterator that will have its remaining contents added to this
	 * @throws NullPointerException if {@code iter} is {@code null}
	 */
	public ObjectDeque(Iterator<? extends T> iter) {
		this();
		addAll(iter);
	}

	/**
	 * Copies the given ObjectDeque exactly into this one. Individual values will be shallow-copied.
	 *
	 * @param deque another ObjectDeque to copy
	 * @throws NullPointerException if {@code deque} is {@code null}
	 */
	public ObjectDeque(ObjectDeque<? extends T> deque) {
		this.items = Arrays.copyOf(deque.items, deque.items.length);
		this.size = deque.size;
		this.head = deque.head;
		this.tail = deque.tail;
		this.defaultValue = deque.defaultValue;
	}

	/**
	 * Creates a new ObjectDeque using all the contents of the given array.
	 *
	 * @param a an array of T that will be copied into this and used in full
	 * @throws NullPointerException if {@code a} is {@code null}
	 */
	public ObjectDeque(T[] a) {
		this.items = Arrays.copyOf(a, Math.max(1, a.length));
		size = a.length;
		tail = Math.max(0, size - 1);
	}

	/**
	 * Creates a new ObjectDeque using {@code count} items from {@code a}, starting at {@code offset}.
	 * If {@code count} is 0 or less, this will create an empty ObjectDeque with capacity 1.
	 * @param a      an array of T
	 * @param offset where in {@code a} to start using items
	 * @param count  how many items to use from {@code a}
	 * @throws NullPointerException if {@code a} is {@code null}
	 */
	public ObjectDeque(T[] a, int offset, int count) {
		int adjusted = Math.max(1, count);
		this.items = Arrays.copyOfRange(a, offset, offset + adjusted);
		tail = adjusted - 1;
		size = Math.max(0, count);
	}

	/**
	 * Gets the default value, which is the value returned when nothing can be obtained from this deque and an exception
	 * is not meant to be thrown, such as when calling peek() on an empty deque. Unless changed, the default value is
	 * usually {@code null}.
	 * @return the current default value
	 */
	@Nullable
	public T getDefaultValue () {
		return defaultValue;
	}

	/**
	 * Sets the default value, which is the value returned when nothing can be obtained from this deque and an exception
	 * is not meant to be thrown, such as when calling peek() on an empty deque. Unless changed, the default value is
	 * usually {@code null}.
	 * @param defaultValue any T object this can return instead of throwing an Exception, or {@code null}
	 */
	public void setDefaultValue (@Nullable T defaultValue) {
		this.defaultValue = defaultValue;
	}

	/**
	 * Appends {@code value} to the tail (enqueue to tail). Unless backing array needs resizing, operates in O(1) time.
	 *
	 * @param value can be null
	 */
	@Override
	public void addLast (@Nullable T value) {
		@Nullable T[] items = this.items;

		if (size == items.length) {
			resize(items.length << 1);
			items = this.items;
		}

		if (++tail == items.length) tail = 0;
		if(++size == 1) tail = head;
		items[tail] = value;
		modCount++;
	}

	public void addLast(@Nullable T value1, @Nullable T value2) {
		@Nullable T[] items = this.items;

		if (size + 2 > items.length) {
			resize(size + 2 << 1);
			items = this.items;
		}

		if (++tail == items.length) tail = 0;
		if(size == 0) tail = head;
		items[tail] = value1;
		if (++tail == items.length) tail = 0;
		items[tail] = value2;
		size += 2;
		modCount += 2;
	}

	public void addLast(@Nullable T value1, @Nullable T value2, @Nullable T value3) {
		@Nullable T[] items = this.items;

		if (size + 3 > items.length) {
			resize(size + 3 << 1);
			items = this.items;
		}
		if (++tail == items.length) tail = 0;
		if(size == 0) tail = head;
		items[tail] = value1;
		if (++tail == items.length) tail = 0;
		items[tail] = value2;
		if (++tail == items.length) tail = 0;
		items[tail] = value3;
		size += 3;
		modCount += 3;
	}

	public void addLast(@Nullable T value1, @Nullable T value2, @Nullable T value3, @Nullable T value4) {
		@Nullable T[] items = this.items;

		if (size + 4 > items.length) {
			resize(size + 4 << 1);
			items = this.items;
		}
		if (++tail == items.length) tail = 0;
		if(size == 0) tail = head;
		items[tail] = value1;
		if (++tail == items.length) tail = 0;
		items[tail] = value2;
		if (++tail == items.length) tail = 0;
		items[tail] = value3;
		if (++tail == items.length) tail = 0;
		items[tail] = value4;
		size += 4;
		modCount += 4;
	}

	/**
	 * Prepends {@code value} to the head (enqueue to head). Unless backing array needs resizing, operates in O(1) time.
	 *
	 * @param value can be null
	 * @see #addLast(Object)
	 */
	public void addFirst (@Nullable T value) {
		@Nullable T[] items = this.items;

		if (size == items.length) {
			resize(size << 1);
			items = this.items;
		}
		int head = this.head - 1;
		if (head == -1) head = items.length - 1;
		items[head] = value;

		this.head = head;
		if(++size == 1) tail = head;
		modCount++;
	}

	public void addFirst (@Nullable T value1, @Nullable T value2) {
		@Nullable T[] items = this.items;

		if (size + 2 > items.length) {
			resize(size + 2 << 1);
			items = this.items;
		}

		int head = this.head - 1;
		if (head == -1) head = items.length - 1;
		if(size == 0) tail = head;
		items[head] = value2;
		if (--head == -1) head = items.length - 1;
		items[head] = value1;
		size += 2;

		this.head = head;
		modCount += 2;
	}

	public void addFirst (@Nullable T value1, @Nullable T value2, @Nullable T value3) {
		@Nullable T[] items = this.items;

		if (size + 3 > items.length) {
			resize(size + 3 << 1);
			items = this.items;
		}
		int head = this.head - 1;
		if (head == -1) head = items.length - 1;
		if(size == 0) tail = head;
		items[head] = value3;
		if (--head == -1) head = items.length - 1;
		items[head] = value2;
		if (--head == -1) head = items.length - 1;
		items[head] = value1;
		size += 3;

		this.head = head;
		modCount += 3;
	}

	public void addFirst (@Nullable T value1, @Nullable T value2, @Nullable T value3, @Nullable T value4) {
		@Nullable T[] items = this.items;

		if (size + 4 > items.length) {
			resize(size + 4 << 1);
			items = this.items;
		}

		int head = this.head - 1;
		if (head == -1) head = items.length - 1;
		if(size == 0) tail = head;
		items[head] = value4;
		if (--head == -1) head = items.length - 1;
		items[head] = value3;
		if (--head == -1) head = items.length - 1;
		items[head] = value2;
		if (--head == -1) head = items.length - 1;
		items[head] = value1;
		size += 4;

		this.head = head;
		modCount += 4;
	}

	/**
	 * Trims the capacity of this {@code ObjectDeque} instance to be the
	 * deque's current size.  An application can use this operation to minimize
	 * the storage of an {@code ObjectDeque} instance.
	 */
	public void trimToSize() {
		modCount++;
		if (size < items.length) {
			if(head <= tail) {
				items = Arrays.copyOfRange(items, head, tail+1);
			} else {
				@Nullable T[] next = Arrays.copyOf(items, size);
				System.arraycopy(items, head, next, 0, items.length - head);
				System.arraycopy(items, 0, next, items.length - head, tail + 1);
				items = next;
			}
			head = 0;
			tail = items.length - 1;
		}
	}

	/**
	 * Increases the size of the backing array to accommodate the specified number of additional items. Useful before adding many
	 * items to avoid multiple backing array resizes.
	 */
	public void ensureCapacity (int additional) {
		final int needed = size + additional;
		if (items.length < needed) {
			resize(needed);
		}
	}

	/**
	 * Reduces the size of the backing array to the size of the actual items. This is useful to release memory when many items
	 * have been removed, or if it is known that more items will not be added.
	 * This is an alias for {@link #trimToSize()}.
	 */
	public void shrink() {
		trimToSize();
	}

	/**
	 * Resizes the backing array. newSize should be greater than the current size; otherwise, newSize will be set to
	 * size and the resize to the same size will (for most purposes) be wasted effort. If this is not empty, this will
	 * rearrange the items internally to be linear and have the head at index 0, with the tail at {@code size - 1}.
	 * This always allocates a new internal backing array.
	 */
	public void resize (int newSize) {
		if(newSize < size)
			newSize = size;
		final @Nullable T[] items = this.items;
		final int head = this.head;
		final int tail = this.tail;

		@SuppressWarnings("unchecked")
		final @Nullable T[] newArray = (T[])new Object[Math.max(1, newSize)];

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
	}

	/**
	 * Make sure there is a "gap" of exactly {@code gapSize} values starting at {@code index}. This can
	 * resize the backing array to achieve this goal. If possible, this will keep the same backing array and modify
	 * it in-place. The "gap" is not assigned null, and may contain old/duplicate references; calling code <em>must</em>
	 * overwrite the entire gap with additional values to ensure GC correctness.
	 * @implNote This is considered an incomplete modification for the purpose of {@link #modCount}, so it does not
	 * change modCount; the code that fills in the gap should change modCount instead.
	 * @param index the 0-based index in the iteration order where the gap will be present
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
			this.head = this.tail = 0;
			if (items.length < gapSize) {
				//noinspection unchecked
				this.items = (T[]) new Object[gapSize];
			}
			return 0;
		} else if (size == 1) {
			if (items.length < gapSize + size) {
				T item = this.items[head];
				//noinspection unchecked
				this.items = (T[]) new Object[gapSize + size];
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
						this.items[head] = null;
					}
					this.head = 0;
					this.tail = gapSize;
					return 1;
				} else {
					if (head != gapSize) {
						this.items[gapSize] = this.items[head];
						this.items[head] = null;
					}
					this.head = 0;
					this.tail = gapSize;
					return 0;
				}
			}
		}

		final @Nullable T[] items = this.items;
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
				if (head + index <= items.length) {
					if(head - gapSize >= 0) {
						System.arraycopy(items, head, items, head - gapSize, index);
						this.head -= gapSize;
						return this.head + index;
					} else {
						System.arraycopy(items, head + index, items, head + index + gapSize, items.length - (head + index + gapSize));
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
			@SuppressWarnings("unchecked") final @Nullable T[] newArray = (T[]) new Object[newSize];

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

	/**
	 * Inserts the specified number of items at the specified index. The new items will have values equal to the values at those
	 * indices before the insertion, and the previous values will be pushed to after the duplicated range.
	 * @param index the first index to duplicate
	 * @param count how many items to duplicate
	 */
	public boolean duplicateRange(int index, int count) {
		int place = ensureGap(index + count, count);
		if(place >= head + index + count){
			System.arraycopy(items, head + index, items, place, count);
		} else {
			System.arraycopy(items, 0, items, count - place, place);
			System.arraycopy(items, head, items, place, count - place);
		}
		size += count;
		return count > 0;
	}

	/**
	 * Remove the first item from the deque. (dequeue from head) Always O(1).
	 *
	 * @return removed object
	 * @throws NoSuchElementException when the deque is empty
	 */
	@Nullable
	@Override
	public T removeFirst () {
		if (size == 0) {
			// Underflow
			throw new NoSuchElementException("ObjectDeque is empty.");
		}

		final @Nullable T[] items = this.items;

		final T result = items[head];
		items[head] = null;
		head++;
		if (head == items.length) {
			head = 0;
		}
		if(--size == 0) tail = head;
		modCount++;

		return result;
	}

	/**
	 * Remove the last item from the deque. (dequeue from tail) Always O(1).
	 *
	 * @return removed object
	 * @throws NoSuchElementException when the deque is empty
	 * @see #removeFirst()
	 */
	@Nullable
	@Override
	public T removeLast () {
		if (size == 0) {
			throw new NoSuchElementException("ObjectDeque is empty.");
		}

		final @Nullable T[] items = this.items;
		int tail = this.tail;
		final T result = items[tail];
		items[tail] = null;

		if (tail == 0) {
			tail = items.length - 1;
		} else {
			--tail;
		}
		this.tail = tail;

		if(--size == 0) head = tail;
		modCount++;

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
	 * or returns {@link #getDefaultValue() defaultValue} if this deque is empty. The default value is usually
	 * {@code null} unless it has been changed with {@link #setDefaultValue(Object)}.
	 *
	 * @see #removeFirst() the alternative removeFirst() throws an Exception if the deque is empty
	 * @return the head of this deque, or {@link #getDefaultValue() defaultValue} if this deque is empty
	 */
	@Override
	@Nullable
	public T pollFirst () {
		if (size == 0) {
			// Underflow
			return defaultValue;
		}

		final @Nullable T[] items = this.items;

		final T result = items[head];
		items[head] = null;
		head++;
		if (head == items.length) {
			head = 0;
		}
		if(--size == 0) tail = head;
		modCount++;

		return result;
	}

	/**
	 * Retrieves and removes the last element of this deque,
	 * or returns {@link #getDefaultValue() defaultValue} if this deque is empty. The default value is usually
	 * {@code null} unless it has been changed with {@link #setDefaultValue(Object)}.
	 *
	 * @see #removeLast() the alternative removeLast() throws an Exception if the deque is empty
	 * @return the tail of this deque, or {@link #getDefaultValue() defaultValue} if this deque is empty
	 */
	@Override
	@Nullable
	public T pollLast () {
		if (size == 0) {
			return defaultValue;
		}

		final @Nullable T[] items = this.items;
		int tail = this.tail;
		final T result = items[tail];
		items[tail] = null;

		if (tail == 0) {
			tail = items.length - 1;
		} else {
			--tail;
		}
		this.tail = tail;

		if(--size == 0) head = tail;
		modCount++;

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
		return items[head];
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
		return items[tail];
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
	 * Inserts the specified element into the deque represented by this deque
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
		addLast(t);
		return true;
	}

	@Override
	public boolean add(T item0, T item1) {
		addLast(item0, item1);
		return true;
	}

	@Override
	public boolean add(T item0, T item1, T item2) {
		addLast(item0, item1, item2);
		return true;
	}

	@Override
	public boolean add(T item0, T item1, T item2, T item3) {
		addLast(item0, item1, item2, item3);
		return true;
	}

	/**
	 * Inserts the specified element into this deque at the specified index.
	 * Unlike {@link #offerFirst(Object)} and {@link #offerLast(Object)}, this does not run in expected constant time unless
	 * the index is less than or equal to 0 (where it acts like offerFirst()) or greater than or equal to {@link #size()}
	 * (where it acts like offerLast()).
	 * @param index the index in the deque's insertion order to insert the item
	 * @param item a T item to insert; may be null
	 */
	@Override
	public void add (int index, @Nullable T item) {
		if(index <= 0)
			addFirst(item);
		else if(index >= size)
			addLast(item);
		else {
			@Nullable T[] items = this.items;

			if (++size > items.length) {
				resize(items.length << 1);
				items = this.items;
			}

			if(head <= tail) {
				index += head;
				if(index >= items.length) index -= items.length;
				int after = index + 1;
				if(after >= items.length) after = 0;

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
				}
				else {
					// forward shift
					index = head + index - items.length;
					System.arraycopy(items, index, items, index + 1, tail - index + 1);
					items[index] = item;
					tail++;
				}
			}
			modCount++;
		}

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
		add(index, item);
		return true;
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
	public boolean addAll (Collection<@Nullable ? extends T> c) {
		final int cs = c.size();
		if(cs == 0) return false;
		int oldSize = size;
		ensureCapacity(Math.max(cs, oldSize));
		if(c == this) {
			if(head <= tail) {
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
			for (T t : c) {
				addLast(t);
			}
		}
		return oldSize != size;
	}

	/**
	 * An alias for {@link #addAll(Collection)}, this adds every item in {@code c} to this in order at the end.
	 * @param c the elements to be inserted into this deque
	 * @return {@code true} if this deque changed as a result of the call
	 */
	public boolean addAllLast (Collection<@Nullable ? extends T> c) {
		return addAll(c);
	}

	/**
	 * Adds every item in {@code c} to this in order at the start. The iteration order of {@code c} will be preserved
	 * for the added items.
	 * @param c the elements to be inserted into this deque
	 * @return {@code true} if this deque changed as a result of the call
	 */
	public boolean addAllFirst (Collection<@Nullable ? extends T> c) {
		final int cs = c.size();
		if(cs == 0) return false;
		int oldSize = size;
		ensureCapacity(Math.max(cs, oldSize));
		if(c == this) {
			if(head <= tail) {
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
			if(head < 0) head += items.length;
			size += oldSize;
		} else {
			int i = ensureGap(0, cs);
			for(T t : c){
				items[i++] = t;
				if(i == items.length) i = 0;
			}
			size += cs;
		}
		return oldSize != size;
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
	public boolean insertAll(int index, Collection<@Nullable ? extends T> c) {
		return addAll(index, c);
	}

	@Override
	public boolean addAll(int index, Collection<@Nullable ? extends T> c) {
		int oldSize = size;
		if(index <= 0)
			addAllFirst(c);
		else if(index >= oldSize)
			addAll(c);
		else {
			final int cs = c.size();
			if(c.isEmpty()) return false;
			int place = ensureGap(index, cs);
			@Nullable T[] items = this.items;
			if(c == this){
				System.arraycopy(items, head, items, place, place - head);
				System.arraycopy(items, place + cs, items, place + place - head, tail + 1 - place - cs);
			} else {
				for (T item : c) {
					items[place++] = item;
					if (place >= items.length) place -= items.length;
				}
			}
			size += cs;
			modCount += cs;
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
		final int cs = Math.min(array.length - offset, length);
		if(cs <= 0) return false;
		int place = ensureGap(size, cs);
		System.arraycopy(array, offset, this.items, place, cs);
		size += cs;
		modCount += cs;
		return true;
	}


	/**
	 * An alias for {@link #addAll(Object[])}.
	 * @see #addAll(Object[])
	 * @param array the elements to be inserted into this deque
	 * @return {@code true} if this deque changed as a result of the call
	 */
	public boolean addAllLast (T[] array) {
		return addAll(array, 0, array.length);
	}
	/**
	 * An alias for {@link #addAll(Object[], int, int)}.
	 * @see #addAll(Object[], int, int)
	 * @param array the elements to be inserted into this deque
	 * @param offset the index of the first item in array to add
	 * @param length how many items, at most, to add from array into this
	 * @return {@code true} if this deque changed as a result of the call
	 */
	public boolean addAllLast (T[] array, int offset, int length) {
		return addAll(array, offset, length);
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
		final int cs = Math.min(array.length - offset, length);
		if(cs <= 0) return false;
		int place = ensureGap(0, cs);
		System.arraycopy(array, offset, this.items, place, cs);
		size += cs;
		modCount += cs;
		return true;
	}

	/**
	 * Alias for {@link #addAll(int, Object[])}.
	 * @param index the index in this deque's iteration order to place the first item in {@code array}
	 * @param array the elements to be inserted into this deque
	 * @return {@code true} if this deque changed as a result of the call
	 */
	public boolean insertAll(int index, T[] array) {
		return addAll(index, array, 0, array.length);
	}

	/**
	 * Alias for {@link #addAll(int, Object[], int, int)}.
	 * @param index the index in this deque's iteration order to place the first item in {@code array}
	 * @param array the elements to be inserted into this deque
	 * @param offset the index of the first item in array to add
	 * @param length how many items, at most, to add from array into this
	 * @return {@code true} if this deque changed as a result of the call
	 */
	public boolean insertAll(int index, T[] array, int offset, int length) {
		return addAll(index, array, offset, length);
	}
	/**
	 * Like {@link #addAll(int, Collection)}, but takes an array instead of a Collection and inserts it
	 * so the first item will be at the given {@code index}.
	 * The order of {@code array} will be preserved, starting at the given index in this deque.
	 * @see #addAll(Object[])
	 * @param index the index in this deque's iteration order to place the first item in {@code array}
	 * @param array the elements to be inserted into this deque
	 * @return {@code true} if this deque changed as a result of the call
	 */
	public boolean addAll(int index, T[] array) {
		return addAll(index, array, 0, array.length);
	}

	/**
	 * Like {@link #addAll(int, Collection)}, but takes an array instead of a Collection, gets items starting at
	 * {@code offset} from that array, using {@code length} items, and inserts them
	 * so the item at the given offset will be at the given {@code index}.
	 * The order of {@code array} will be preserved, starting at the given index in this deque.
	 * @see #addAll(Object[])
	 * @param index the index in this deque's iteration order to place the first item in {@code array}
	 * @param array the elements to be inserted into this deque
	 * @param offset the index of the first item in array to add
	 * @param length how many items, at most, to add from array into this
	 * @return {@code true} if this deque changed as a result of the call
	 */
	public boolean addAll(int index, T[] array, int offset, int length) {
		int oldSize = size;
		if(index <= 0)
			addAllFirst(array, offset, length);
		else if(index >= oldSize)
			addAll(array, offset, length);
		else {
			final int cs = Math.min(array.length - offset, length);
			if(cs <= 0) return false;
			int place = ensureGap(index, cs);
			System.arraycopy(array, offset, this.items, place, cs);
			size += cs;
			modCount += cs;
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
		if (head <= tail) {
			System.arraycopy(items, head, next, 0, tail - head + 1);
		} else {
			System.arraycopy(items, head, next, 0, size - head);
			System.arraycopy(items, 0, next, size - head, tail + 1);
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
	public <E> @Nullable E @NonNull [] toArray (@Nullable E @NonNull [] a) {
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
	@Override
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
	@Override
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
	@Override
	public boolean containsAnyIterable(Iterable<?> values) {
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
	@Override
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
	@Override
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
	@Override
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
	@Override
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
	@Override
	public boolean removeEachIterable(Iterable<?> other) {
		boolean changed = false;
		for(Object item : other) {
			changed |= remove(item);
		}
		return changed;
	}

	/**
	 * Exactly like {@link #removeEachIterable(Iterable)}, but takes an array instead of a Collection.
	 * @see #removeEachIterable(Iterable)
	 * @param array array containing elements to be removed from this collection
	 * @return {@code true} if this deque changed as a result of the call
	 */
	@Override
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
	@Override
	public boolean removeEach (Object[] array, int offset, int length) {
		boolean changed = false;
		for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
			changed |= remove(array[i]);
		}
		return changed;
	}

	/**
	 * Exactly like {@link #retainAll(Collection)}, but takes an array instead of a Collection.
	 * @see #retainAll(Collection)
	 * @param array array containing elements to be retained in this collection
	 * @return {@code true} if this deque changed as a result of the call
	 */
	public boolean retainAll (Object[] array) {
		Objects.requireNonNull(array);
		boolean modified = false;
		ListIterator<T> it = iterator();
		OUTER:
		while (it.hasNext()) {
			T check = it.next();
			for (int i = 0, n = array.length; i < n; i++) {
				if(Objects.equals(array[i], check))
					continue OUTER;
			}
			it.remove();
			modified = true;
		}
		return modified;
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
		Objects.requireNonNull(array);
		boolean modified = false;
		ListIterator<T> it = iterator();
		OUTER:
		while (it.hasNext()) {
			T check = it.next();
			for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
				if(Objects.equals(array[i], check))
					continue OUTER;
			}
			it.remove();
			modified = true;
		}
		return modified;
	}


	/**
	 * Selects the kth-lowest element from this ObjectDeque according to Comparator ranking. This might partially sort the ObjectDeque,
	 * changing its order. The ObjectDeque must have a size greater than 0, or a {@link RuntimeException} will be thrown.
	 *
	 * @param comparator used for comparison
	 * @param kthLowest  rank of desired object according to comparison; k is based on ordinal numbers, not array indices. For min
	 *                   value use 1, for max value use size of the ObjectDeque; using 0 results in a runtime exception.
	 * @return the value of the kth lowest ranked object.
	 * @see Select
	 */
	public T selectRanked (Comparator<T> comparator, int kthLowest) {
		if (kthLowest < 1) {
			throw new RuntimeException("kthLowest must be greater than 0; 1 = first, 2 = second...");
		}
		return Select.select(this, comparator, kthLowest, size());
	}

	/**
	 * Gets the index of the kth-lowest element from this ObjectDeque according to Comparator ranking. This might partially sort the
	 * ObjectDeque, changing its order. The ObjectDeque must have a size greater than 0, or a {@link RuntimeException} will be thrown.
	 *
	 * @param comparator used for comparison
	 * @param kthLowest  rank of desired object according to comparison; k is based on ordinal numbers, not array indices. For min
	 *                   value use 1, for max value use size of the ObjectDeque; using 0 results in a runtime exception.
	 * @return the index of the kth lowest ranked object.
	 * @see #selectRanked(Comparator, int)
	 */
	public int selectRankedIndex (Comparator<T> comparator, int kthLowest) {
		if (kthLowest < 1) {
			throw new RuntimeException("kthLowest must be greater than 0; 1 = first, 2 = second...");
		}
		return Select.selectIndex(this, comparator, kthLowest, size());
	}

	/**
	 * Alias for {@link #truncate(int)}.
	 * @param newSize the size this deque should have after this call completes, if smaller than the current size
	 */
	public void truncateLast (int newSize) {
		truncate(newSize);
	}

	/**
	 * Reduces the size of the deque to the specified size by bulk-removing items from the tail end.
	 * If the deque is already smaller than the specified size, no action is taken.
	 * @param newSize the size this deque should have after this call completes, if smaller than the current size
	 */
	public void truncate (int newSize) {
		if(newSize <= 0) {
			clear();
			return;
		}
		int oldSize = size;
		if (oldSize > newSize) {
			if(head <= tail) {
				// only removing from tail, near the end, toward head, near the start
				Arrays.fill(items, head + newSize, tail + 1, null);
				tail -= oldSize - newSize;
				size = newSize;
			} else if(head + newSize < items.length) {
				// tail is near the start, but we have to remove elements through the start and into the back
				Arrays.fill(items, 0, tail + 1, null);
				tail = head + newSize;
				Arrays.fill(items, tail, items.length, null);
				size = newSize;
			} else {
				// tail is near the start, but we only have to remove some elements between tail and the start
				final int newTail = tail - (oldSize - newSize);
				Arrays.fill(items, newTail + 1, tail + 1, null);
				tail = newTail;
				size = newSize;
			}
			modCount += oldSize - newSize;
		}
	}

	/**
	 * Reduces the size of the deque to the specified size by bulk-removing from the head.
	 * If the deque is already smaller than the specified size, no action is taken.
	 * @param newSize the size this deque should have after this call completes, if smaller than the current size
	 */
	public void truncateFirst (int newSize) {
		if(newSize <= 0) {
			clear();
			return;
		}
		int oldSize = size;
		if (oldSize > newSize) {
			if(head <= tail || head + oldSize - newSize < items.length) {
				// only removing from head to head + newSize, which is contiguous
				Arrays.fill(items, head, head + oldSize - newSize, null);
				head += oldSize - newSize;
				if(head >= items.length) head -= items.length;
				size = newSize;
			} else {
				// tail is near the start, and we are removing from head to the end and then part near start
				Arrays.fill(items, head, items.length, null);
				head = tail + 1 - newSize;
				Arrays.fill(items, 0, head, null);
				size = newSize;
			}
			modCount += oldSize - newSize;
		}
	}

	/**
	 * Removes from this list all the elements whose index is between
	 * {@code fromIndex}, inclusive, and {@code toIndex}, exclusive.
	 * Shifts any succeeding elements to the left (reduces their index).
	 * This call shortens the list by {@code (toIndex - fromIndex)} elements.
	 * If {@code toIndex==fromIndex}, this operation has no effect.
	 * If {@code fromIndex} is 0 or less, this delegates to {@link #truncateFirst(int)};
	 * if {@code toIndex} is equal to or greater than the
	 * size of this collection, this delegates to {@link #truncate(int)}.
	 * <br>
	 * This is public here, not protected as in most JDK collections, because there are
	 * actually sometimes needs for this in user code.
	 *
	 * @param fromIndex index of first element to be removed (inclusive)
	 * @param toIndex index after last element to be removed (exclusive)
	 */
	@Override
	public void removeRange(int fromIndex, int toIndex) {
		if(fromIndex <= 0){
			truncateFirst(size - toIndex);
			return;
		}
		if(toIndex >= size) {
			truncate(fromIndex);
			return;
		}
		if (fromIndex < toIndex) {
			int removedCount = toIndex - fromIndex;
			if(head <= tail) {
				// tail is near the end, head is near the start
				int tailMinusTo = tail + 1 - (head + toIndex);
				if(tailMinusTo < 0) tailMinusTo += items.length;
				System.arraycopy(items, head + toIndex, items, head + fromIndex, tailMinusTo);
				Arrays.fill(items, tail + 1 - removedCount, tail + 1, null);
				tail -= removedCount;
				size -= removedCount;
			} else if(head + toIndex < items.length) {
				// head is at the end, and tail wraps around, but we are only removing items between head and end
				int headPlusFrom = head + fromIndex;
				if(headPlusFrom >= items.length) headPlusFrom -= items.length;
				System.arraycopy(items, head, items, headPlusFrom, removedCount);
				Arrays.fill(items, head, head + removedCount, null);
				head += removedCount;
				size -= removedCount;
			} else if(head + toIndex - items.length - removedCount >= 0) {
				// head is at the end, and tail wraps around, but we are only removing items between start and tail
				System.arraycopy(items, head + toIndex - items.length, items, head + fromIndex - items.length, tail + 1 - (head + toIndex - items.length));
				Arrays.fill(items, tail + 1 - removedCount, tail + 1, null);
				tail -= removedCount;
				size -= removedCount;
			} else {
				// head is at the end, tail wraps around, and we must remove items that wrap from end to start
				System.arraycopy(items, head, items, items.length - fromIndex, fromIndex);
				System.arraycopy(items, head + toIndex - items.length, items, 0, tail + 1 - (head + toIndex - items.length));
				Arrays.fill(items, head, items.length - fromIndex, null);
				Arrays.fill(items, tail + 1 - (head + toIndex - items.length), tail + 1, null);
				tail -= (head + toIndex - items.length);
				head = (items.length - fromIndex);
				size -= removedCount;
			}
			modCount += removedCount;
		}
	}

	/**
	 * Returns the index of the first occurrence of value in the deque, or -1 if no such value exists.
	 * Uses .equals() to compare items.
	 *
	 * @param value the Object to look for, which may be null
	 * @return An index of the first occurrence of value in the deque or -1 if no such value exists
	 */
	@Override
	public int indexOf (@Nullable Object value) {
		return indexOf(value, false);
	}

	/**
	 * Returns the index of the first occurrence of value in the deque, or -1 if no such value exists.
	 * Uses .equals() to compare items. This returns {@code fromIndex} if {@code value} is present at that point,
	 * so if you chain calls to indexOf(), the subsequent fromIndex should be larger than the last-returned index.
	 *
	 * @param value the Object to look for, which may be null
	 * @param fromIndex the initial index to check (zero-indexed, starts at the head, inclusive)
	 * @return An index of first occurrence of value at or after fromIndex in the deque, or -1 if no such value exists
	 */
	public int indexOf (@Nullable Object value, int fromIndex) {
		return indexOf(value, fromIndex, false);
	}

	/**
	 * Returns the index of first occurrence of value in the deque, or -1 if no such value exists.
	 * When {@code identity} is false, uses .equals() to compare items; when identity is true, uses {@code ==} .
	 *
	 * @param value the Object to look for, which may be null
	 * @param identity If true, == comparison will be used. If false, .equals() comparison will be used.
	 * @return An index of first occurrence of value in the deque or -1 if no such value exists
	 */
	public int indexOf (@Nullable Object value, boolean identity) {
		return indexOf(value, 0, identity);
	}

	/**
	 * Returns the index of first occurrence of {@code value} in the deque, starting from {@code fromIndex},
	 * or -1 if no such value exists. This returns {@code fromIndex} if {@code value} is present at that point,
	 * so if you chain calls to indexOf(), the subsequent fromIndex should be larger than the last-returned index.
	 * When {@code identity} is false, uses .equals() to compare items; when identity is true, uses {@code ==} .
	 *
	 * @param value the Object to look for, which may be null
	 * @param fromIndex the initial index to check (zero-indexed, starts at the head, inclusive)
	 * @param identity If true, == comparison will be used. If false, .equals() comparison will be used.
	 * @return An index of first occurrence of value at or after fromIndex in the deque, or -1 if no such value exists
	 */
	public int indexOf (@Nullable Object value, int fromIndex, boolean identity) {
		if (size == 0)
			return -1;
		@Nullable T[] items = this.items;
		final int head = this.head, tail = this.tail;
		int i = head + Math.min(Math.max(fromIndex, 0), size - 1);
		if (i >= items.length)
			i -= items.length;

		if (identity || value == null) {
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
		} else {
			if (head <= tail) {
				for (; i <= tail; i++)
					if (value.equals(items[i]))
						return i - head;
			} else {
				for (int n = items.length; i < n; i++)
					if (value.equals(items[i]))
						return i - head;
				for (i = 0; i <= tail; i++)
					if (value.equals(items[i]))
						return i + items.length - head;
			}
		}
		return -1;
	}

	/**
	 * Returns the index of the last occurrence of value in the deque, or -1 if no such value exists.
	 * Uses .equals() to compare items.
	 *
	 * @param value the Object to look for, which may be null
	 * @return An index of the last occurrence of value in the deque or -1 if no such value exists
	 */
	@Override
	public int lastIndexOf (@Nullable Object value) {
		return lastIndexOf(value, false);
	}

	/**
	 * Returns the index of last occurrence of {@code value} in the deque, starting from {@code fromIndex} and going
	 * backwards, or -1 if no such value exists. This returns {@code fromIndex} if {@code value} is present at that
	 * point, so if you chain calls to indexOf(), the subsequent fromIndex should be smaller than the last-returned
	 * index. Uses .equals() to compare items.
	 *
	 * @param value the Object to look for, which may be null
	 * @param fromIndex the initial index to check (zero-indexed, starts at the head, inclusive)
	 * @return An index of last occurrence of value at or before fromIndex in the deque, or -1 if no such value exists
	 */
	public int lastIndexOf (@Nullable Object value, int fromIndex) {
		return lastIndexOf(value, fromIndex, false);
	}

	/**
	 * Returns the index of the last occurrence of value in the deque, or -1 if no such value exists.
	 *
	 * @param value the Object to look for, which may be null
	 * @param identity If true, == comparison will be used. If false, .equals() comparison will be used.
	 * @return An index of the last occurrence of value in the deque or -1 if no such value exists
	 */
	public int lastIndexOf (@Nullable Object value, boolean identity) {
		return lastIndexOf(value, size - 1, identity);
	}

	/**
	 * Returns the index of last occurrence of {@code value} in the deque, starting from {@code fromIndex} and going
	 * backwards, or -1 if no such value exists. This returns {@code fromIndex} if {@code value} is present at that
	 * point, so if you chain calls to indexOf(), the subsequent fromIndex should be smaller than the last-returned
	 * index. When {@code identity} is false, uses .equals() to compare items; when identity is true, uses {@code ==} .
	 *
	 * @param value the Object to look for, which may be null
	 * @param fromIndex the initial index to check (zero-indexed, starts at the head, inclusive)
	 * @param identity If true, == comparison will be used. If false, .equals() comparison will be used.
	 * @return An index of last occurrence of value at or before fromIndex in the deque, or -1 if no such value exists
	 */
	public int lastIndexOf (@Nullable Object value, int fromIndex, boolean identity) {
		if (size == 0)
			return -1;
		@Nullable T[] items = this.items;
		final int head = this.head, tail = this.tail;
		int i = head + Math.min(Math.max(fromIndex, 0), size - 1);
		if (i >= items.length)
			i -= items.length;
		else if (i < 0)
			i += items.length;

		if (identity || value == null) {
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
		} else {
			if (head <= tail) {
				for (; i >= head; i--)
					if (value.equals(items[i]))
						return i - head;
			} else {
				for (; i >= 0; i--)
					if (value.equals(items[i]))
						return i + items.length - head;
				for (i = items.length - 1; i >= head; i--)
					if (value.equals(items[i]))
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
	/**
	 * Gets an iterator over this deque that starts at the given index.
	 * @param index the index to start iterating from in this deque
	 * @return a reused iterator starting at the given index
	 */
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

	@Override
	public List<T> subList(int fromIndex, int toIndex) {
		return super.subList(fromIndex, toIndex);
	}

	/**
	 * Removes the first instance of the specified value in the deque.
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
	 * Removes the last instance of the specified value in the deque.
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
	 * Removes the element at the specified position in this deque.
	 * Shifts any subsequent elements to the left (subtracts one
	 * from their indices).  Returns the element that was removed from the
	 * deque.
	 * <br>
	 * This is an alias for {@link #remove(int)} for compatibility with primitive-backed lists and deques;
	 * {@link #remove(int)} can refer to the method that removes an item by value, not by index, in those types.
	 *
	 * @param index the index of the element to be removed
	 * @return the element previously at the specified position
	 */
	@Nullable
	public T removeAt(int index) {
		return remove(index);
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
	@Override
	@Nullable
	public T remove(int index) {
		if (index <= 0)
			return removeFirst();
		if (index >= size)
			return removeLast();

		@Nullable T[] items = this.items;
		int head = this.head, tail = this.tail;
		index += head;
		T value;
		if (head <= tail) { // index is between head and tail.
			value = items[index];
			System.arraycopy(items, index + 1, items, index, tail - index);
			items[this.tail] = null;
			this.tail--;
			if(this.tail == -1) this.tail = items.length - 1;
		} else if (index >= items.length) { // index is between 0 and tail.
			index -= items.length;
			value = items[index];
			System.arraycopy(items, index + 1, items, index, tail - index);
			items[this.tail] = null;
			this.tail--;
			if(this.tail == -1) this.tail = items.length - 1;
		} else { // index is between head and values.length.
			value = items[index];
			System.arraycopy(items, head, items, head + 1, index - head);
			items[this.head] = null;
			this.head++;
			if (this.head == items.length) {
				this.head = 0;
			}
		}
		size--;
		modCount++;
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
	@Nullable
	public T pollAt(int index) {
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
	@Nullable
	public T poll(int index) {
		if (index <= 0)
			return pollFirst();
		if (index >= size)
			return pollLast();
		// No need to check for size to be 0 because the above checks will already do that, and one will run.

		@Nullable T[] items = this.items;
		int head = this.head, tail = this.tail;
		index += head;
		T value;
		if (head <= tail) { // index is between head and tail.
			value = items[index];
			System.arraycopy(items, index + 1, items, index, tail - index);
			items[this.tail] = null;
			this.tail--;
			if(this.tail == -1) this.tail = items.length - 1;
		} else if (index >= items.length) { // index is between 0 and tail.
			index -= items.length;
			value = items[index];
			System.arraycopy(items, index + 1, items, index, tail - index);
			items[this.tail] = null;
			this.tail--;
			if(this.tail == -1) this.tail = items.length - 1;
		} else { // index is between head and values.length.
			value = items[index];
			System.arraycopy(items, head, items, head + 1, index - head);
			items[this.head] = null;
			this.head++;
			if (this.head == items.length) {
				this.head = 0;
			}
		}
		size--;
		modCount++;
		return value;
	}

	/**
	 * Returns true if the deque has one or more items.
	 */
	public boolean notEmpty () {
		return size != 0;
	}

	/**
	 * Returns true if the deque is empty.
	 */
	@Override
	public boolean isEmpty () {
		return size == 0;
	}

	/**
	 * Returns the first (head) item in the deque (without removing it).
	 *
	 * @throws NoSuchElementException when the deque is empty
	 * @see #peekFirst() peeking won't throw an exception, and will return the ObjectDeque's default value if empty
	 * @see #removeFirst()
	 */
	@Override
	public @Nullable T first () {
		if (size == 0) {
			// Underflow
			throw new NoSuchElementException("ObjectDeque is empty.");
		}
		return items[head];
	}

	/**
	 * Returns the last (tail) item in the deque (without removing it).
	 *
	 * @throws NoSuchElementException when the deque is empty
	 * @see #peekLast() peeking won't throw an exception, and will return the ObjectDeque's default value if empty
	 */
	public @Nullable T last () {
		if (size == 0) {
			// Underflow
			throw new NoSuchElementException("ObjectDeque is empty.");
		}
		return items[tail];
	}

	/**
	 * Returns the element at the specified position in this deque.
	 * Like {@link ArrayList} or {@link ObjectList}, but unlike {@link LinkedList}, this runs in O(1) time.
	 * It is expected to be slightly slower than {@link ObjectList#get(int)}, which also runs in O(1) time.
	 * Unlike get() in ArrayList or ObjectList, this considers negative indices to refer to the first item, and
	 * too-large indices to refer to the last item. That means it delegates to {@link #getFirst()} or
	 * {@link #getLast()} in those cases instead of throwing an {@link IndexOutOfBoundsException}, though it may
	 * throw a {@link NoSuchElementException} if the deque is empty and there is no item it can get.
	 *
	 * @param index index of the element to return
	 * @return the element at the specified position in this deque
	 * @throws NoSuchElementException if the deque is empty
	 */
	@Override
	public @Nullable T get (int index) {
		if (index <= 0)
			return getFirst();
		if (index >= size - 1)
			return getLast();
		final @Nullable T[] items = this.items;

		int i = head + index;
		if (i >= items.length)
			i -= items.length;
		return items[i];
	}

	/**
	 * Returns the element at the specified position in this deque.
	 * Like {@link ArrayList} or {@link ObjectList}, but unlike {@link LinkedList}, this runs in O(1) time.
	 * It is expected to be slightly slower than {@link ObjectList#get(int)}, which also runs in O(1) time.
	 * Unlike get() in ArrayList or ObjectList, this considers negative indices to refer to the first item, and
	 * too-large indices to refer to the last item. That means it delegates to {@link #peekFirst()} or
	 * {@link #peekLast()} in those cases instead of throwing an {@link IndexOutOfBoundsException}, and it will
	 * return {@link #getDefaultValue() the default value} if the deque is empty. Unless changed, the default value
	 * is usually {@code null}.
	 *
	 * @param index index of the element to return
	 * @return the element at the specified position in this deque
	 */
	public @Nullable T peekAt (int index) {
		if (index <= 0)
			return peekFirst();
		if (index >= size - 1)
			return peekLast();
		final @Nullable T[] items = this.items;

		int i = head + index;
		if (i >= items.length)
			i -= items.length;
		return items[i];
	}

	/**
	 * Replaces the element at the specified position in this list with the
	 * specified element. If this deque is empty or the index is larger than the largest index currently in this
	 * deque, this delegates to {@link #addLast(Object)} and returns {@link #getDefaultValue() the default value}.
	 * If the index is negative, this delegates to {@link #addFirst(Object)} and returns
	 * {@link #getDefaultValue() the default value}.
	 *
	 * @param index index of the element to replace
	 * @param item element to be stored at the specified position
	 * @return the element previously at the specified position
	 * @throws ClassCastException if the class of the specified element
	 *         prevents it from being put in this list
	 */
	@Override
	public @Nullable T set (int index, @Nullable T item) {
		if (size <= 0 || index >= size) {
			addLast(item);
			return defaultValue;
		}
		if (index < 0) {
			addFirst(item);
			return defaultValue;
		}
		final @Nullable T[] items = this.items;

		int i = head + Math.max(Math.min(index, size - 1), 0);
		if (i >= items.length)
			i -= items.length;
		T old = items[i];
		items[i] = item;
//		modCount++; // apparently this isn't a structural modification?
		return old;
	}

	/**
	 * Removes all values from this deque. Values in backing array are set to null to prevent memory leaks, so this
	 * operates in O(n) time.
	 */
	@Override
	public void clear () {
		if (size == 0)
			return;
		final @Nullable T[] items = this.items;
		final int head = this.head;
		final int tail = this.tail;

		if (head <= tail) {
			// Continuous
			Utilities.clear(items, head, tail - head + 1);
		} else if(tail == 0){
			Utilities.clear(items, head, items.length - head);
		} else {
			// Wrapped
			Utilities.clear(items, head, items.length - head);
			Utilities.clear(items, 0, tail + 1);
		}
		this.head = 0;
		this.tail = 0;
		modCount += size;
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

	/**
	 * Returns an iterator over the elements in this deque in reverse
	 * sequential order. The elements will be returned in order from
	 * {@code index} backwards to first (head).
	 * <br>
	 * Reuses one of two descending iterators for this deque. For nested or multithreaded
	 * iteration, use {@link ObjectDequeIterator#ObjectDequeIterator(ObjectDeque, boolean)}.
	 *
	 * @param index the index to start iterating from in this deque
	 * @return an iterator over the elements in this deque in reverse sequence
	 */
	public @NonNull ObjectDequeIterator<T> descendingIterator (int index) {
		if (descendingIterator1 == null || descendingIterator2 == null) {
			descendingIterator1 = new ObjectDequeIterator<>(this, index, true);
			descendingIterator2 = new ObjectDequeIterator<>(this, index, true);
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
	 * @return the square-bracketed String representation of this ObjectDeque, with items separated by ", "
	 */
	@Override
	public String toString () {
		return toString(", ", true);
	}

	@Override
	public int hashCode () {
		final int size = this.size;
		final @Nullable T[] items = this.items;
		final int backingLength = items.length;
		int index = this.head;

		int hash = size + 1;
		for (int s = 0; s < size; s++) {
			final T value = items[index];

			hash *= 29; // avoids LEA pessimization
			if (value != null)
				hash += value.hashCode();

			index++;
			if (index == backingLength)
				index = 0;
		}

		return hash;
	}

	/**
	 * Using {@link Objects#equals(Object)} between each item in order, compares for equality with
	 * other types implementing {@link List} or {@link Queue}, including other {@link Deque} types.
	 * If {@code o} is not a List or Queue
	 * (and is also not somehow reference-equivalent to this collection), this returns false.
	 * This uses the {@link Iterable#iterator()} of both this and {@code o}, so if either is in the
	 * middle of a concurrent iteration that modifies the Collection, this may fail.
	 * @param o object to be compared for equality with this collection
	 * @return true if this is equal to o, or false otherwise
	 */
	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!((o instanceof List) || (o instanceof Queue)))
			return false;

		Iterator<T> e1 = iterator();
		Iterator<?> e2 = ((Iterable<?>) o).iterator();
		while (e1.hasNext() && e2.hasNext()) {
			T o1 = e1.next();
			Object o2 = e2.next();
			if (!Objects.equals(o1, o2))
				return false;
		}
		return !(e1.hasNext() || e2.hasNext());
	}

	/**
	 * Using {@code ==} between each item in order, compares for equality with
	 * other types implementing {@link List} or {@link Queue}, including other {@link Deque} types.
	 * If {@code o} is not a List or Queue
	 * (and is also not somehow reference-equivalent to this collection), this returns false.
	 * This uses the {@link Iterable#iterator()} of both this and {@code o}, so if either is in the
	 * middle of a concurrent iteration that modifies the Collection, this may fail.
	 * @param o object to be compared for equality with this collection
	 * @return true if this is equal to o, or false otherwise
	 */
	public boolean equalsIdentity (Object o) {
		if (o == this)
			return true;
		if (!((o instanceof List) || (o instanceof Queue)))
			return false;

		Iterator<T> e1 = iterator();
		Iterator<?> e2 = ((Iterable<?>) o).iterator();
		while (e1.hasNext() && e2.hasNext()) {
			T o1 = e1.next();
			Object o2 = e2.next();
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
		if(first == second) return;
		final @Nullable T[] items = this.items;

		int f = head + first;
		if (f >= items.length)
			f -= items.length;

		int s = head + second;
		if (s >= items.length)
			s -= items.length;

		T fv = items[f];
		items[f] = items[s];
		items[s] = fv;

		//modCount += 2; // I don't think this is "structural"
	}

	/**
	 * Reverses this ObjectDeque in-place.
	 */
	@Override
	public void reverse () {
		final @Nullable T[] items = this.items;
		int f, s, len = items.length;
		T fv;
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
//			modCount += 2; // I don't think this is "structural"
		}
	}

	@Override
	public void shuffle (Random rng) {
		// This won't change modCount, because it isn't "structural"
		for (int i = size() - 1; i > 0; i--) {
			int r = rng.nextInt(i + 1);
			if(r != i)
				set(i, set(r, get(i)));
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
	 * Sorts this deque in-place using {@link ObjectComparators#sort(Object[], int, int, Comparator)}.
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
	@Override
	public void sort (@Nullable Comparator<? super T> comparator) {
		if (head <= tail) {
			ObjectComparators.sort(items, head, tail+1, comparator);
		} else {
			System.arraycopy(items, head, items, tail + 1, items.length - head);
			ObjectComparators.sort(items, 0, tail + 1 + items.length - head, comparator);
			tail += items.length - head;
			head = 0;
		}
	}

	/**
	 * Sorts this deque in-place using {@link ObjectComparators#sort(Object[], int, int, Comparator)}.
	 * This only sorts between indices {@code from} (inclusive) and {@code to} (exclusive).
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
	public void sort (int from, int to, @Nullable Comparator<? super T> comparator) {
		if (head <= tail) {
			ObjectComparators.sort(items, head + from, head + to, comparator);
		} else {
			resize(items.length);
			ObjectComparators.sort(items, from, to, comparator);
		}
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
	public void sortJDK (@Nullable Comparator<? super T> comparator) {
		if (head <= tail) {
			Arrays.sort(items, head, tail+1, comparator);
		} else {
			System.arraycopy(items, head, items, tail + 1, items.length - head);
			Arrays.sort(items, 0, tail + 1 + items.length - head, comparator);
			tail += items.length - head;
			head = 0;
		}
//		modCount += size; // I don't think this is "structural"
	}

	/**
	 * Gets a randomly selected item from this ObjectDeque. Throws a {@link NoSuchElementException} if empty.
	 * @param random any Random or subclass of it, such as {@link com.github.tommyettinger.digital.AlternateRandom}.
	 * @return a randomly selected item from this deque, or the default value if empty
	 */
	@Nullable
	public T random (Random random) {
		if (size <= 0) {
			throw new NoSuchElementException("ObjectDeque is empty.");
		}
		return get(random.nextInt(size));
	}

	/**
	 * Like {@link #random(Random)}, but returns {@link #getDefaultValue() the default value} if empty.
	 * @param random any Random or subclass of it, such as {@link com.github.tommyettinger.digital.AlternateRandom}.
	 * @return a randomly selected item from this deque, or the default value if empty
	 */
	@Nullable
	public T peekRandom (Random random) {
		return peekAt(random.nextInt(size));
	}

	/**
	 * An {@link Iterator} and {@link ListIterator} over the elements of an ObjectDeque, while also an {@link Iterable}.
	 * @param <T> the generic type for the ObjectDeque this iterates over
	 */
	public static class ObjectDequeIterator<T> implements Iterable<T>, ListIterator<T> {
		public int index, latest = -1;
		public ObjectDeque<T> deque;
		public boolean valid = true;
		public final int direction;
		public int expectedModCount;

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
			expectedModCount = deque.modCount;
		}

		/**
		 * Checks if this iterator's expected amount of modifications to the deque matches what the deque reports.
		 * This is used to ensure the {@link ObjectDeque#iterator()} and {@link ObjectDeque#listIterator()} are
		 * both fail-fast iterators.
		 * @throws ConcurrentModificationException if the check fails
		 */
		public final void modCheck() {
			if (deque.modCount != expectedModCount)
				throw new ConcurrentModificationException("ObjectDeque's iterator is mismatched with its ObjectDeque.");
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
			modCheck();
			latest = index;
			index += direction;
			final T t = deque.get(latest);
			modCheck();
			return t;
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
			modCheck();
			latest = index -= direction;
			final T t = deque.get(latest);
			modCheck();
			return t;

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
			modCheck();
			deque.remove(latest);
			index = latest;
			latest = -1;
			expectedModCount = deque.modCount;
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
			modCheck();
			deque.set(latest, t);
			expectedModCount = deque.modCount;
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
			modCheck();
			deque.insert(index, t);
			index += direction;
			latest = -1;
			expectedModCount = deque.modCount;

		}

		public void reset () {
			index = deque.size - 1 & direction >> 31;
			latest = -1;
			expectedModCount = deque.modCount;
		}

		public void reset (int index) {
			if (index < 0 || index >= deque.size())
				throw new IndexOutOfBoundsException("ObjectDequeIterator does not satisfy index >= 0 && index < deque.size()");
			this.index = index;
			latest = -1;
			expectedModCount = deque.modCount;
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
	 * @param item7 a T item
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
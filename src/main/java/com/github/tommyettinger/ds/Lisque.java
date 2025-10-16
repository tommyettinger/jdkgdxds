/*
 * Copyright (c) 2025 See AUTHORS file.
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


import java.util.*;

/**
 * A combination List/Deque with some expanded features based on Deque's mix of exceptional and non-exceptional methods.
 *
 * @param <T> the generic type of items
 */
public interface Lisque<T> extends List<T>, Deque<T>, Collection<T> {
	boolean add(T t);

	void add(int index, T element);

	T set(int index, T element);

	boolean contains(Object o);

	boolean containsAll(Collection<?> c);

	boolean removeAll(Collection<?> c);

	boolean retainAll(Collection<?> c);

	void sort(Comparator<? super T> c);

	int indexOf(Object o);

	int lastIndexOf(Object o);

	default boolean insert(int index, T item) {
		add(index, item);
		return true;
	}

	default boolean insertAll(int index, Collection<? extends T> c) {
		return addAll(index, c);
	}

	default boolean addAllLast(Collection<? extends T> c) {
		return addAll(c);
	}

	boolean addAllFirst(Collection<? extends T> c);

	boolean addAll(T[] array);

	boolean addAll(T[] array, int offset, int length);

	boolean addAll(int index, T[] array);

	boolean addAll(int index, T[] array, int offset, int length);

	default boolean insertAll(int index, T[] array) {
		return addAll(index, array);
	}

	default boolean insertAll(int index, T[] array, int offset, int length) {
		return addAll(index, array, offset, length);
	}

	default boolean addAllLast(T[] array) {
		return addAll(array);
	}

	default boolean addAllLast(T[] array, int offset, int length) {
		return addAll(array, offset, length);
	}

	boolean addAllFirst(T[] array);

	boolean addAllFirst(T[] array, int offset, int length);

	boolean retainAll(Object[] array);

	boolean retainAll(Object[] array, int offset, int length);

	void truncate(int newSize);

	default void truncateLast(int newSize) {
		truncate(newSize);
	}

	void truncateFirst(int newSize);

	void removeRange(int fromIndex, int toIndex);

	int indexOf(Object value, int fromIndex);

	int lastIndexOf(Object value, int fromIndex);

	default T removeAt(int index) {
		return remove(index);
	}

	T poll(int index);

	default T pollAt(int index) {
		return poll(index);
	}

	default boolean notEmpty() {
		return !isEmpty();
	}

	T last();

	T peekAt(int index);

	T random(Random random);

	default T peekRandom(Random random) {
		return peekAt(random.nextInt(size()));
	}

	default boolean offerFirst(T t) {
		addFirst(t);
		return true;

	}

	default boolean offerLast(T t) {
		addLast(t);
		return true;
	}

	default T pollFirst() {
		if (isEmpty()) return null;
		return removeFirst();
	}

	default T pollLast() {
		if (isEmpty()) return null;
		return removeLast();
	}

	default T peekFirst() {
		if (isEmpty()) return null;
		return getFirst();
	}

	default T peekLast() {
		if (isEmpty()) return null;
		return getLast();
	}

	default boolean removeFirstOccurrence(Object o) {
		int idx = indexOf(o);
		if (idx == -1) return false;
		removeAt(idx);
		return true;
	}

	default boolean removeLastOccurrence(Object o) {
		int idx = lastIndexOf(o);
		if (idx == -1) return false;
		removeAt(idx);
		return true;
	}

	default boolean offer(T t) {
		addLast(t);
		return true;
	}

	default T remove() {
		return removeFirst();
	}

	default T poll() {
		return pollFirst();
	}

	default T element() {
		return getFirst();
	}

	default T peek() {
		return isEmpty() ? null : getFirst();
	}

	default void push(T t) {
		addFirst(t);
	}

	default T pop() {
		return removeFirst();
	}

	default boolean isEmpty() {
		return size() == 0;
	}

	void addFirst(T t);

	void addLast(T t);

	T getFirst();

	T getLast();

	T removeFirst();

	T removeLast();

	Lisque<T> reversed();

	Iterator<T> iterator();

	ListIterator<T> listIterator();

	ListIterator<T> listIterator(int index);

	Iterator<T> descendingIterator();
}

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

import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * A combination List/Deque with some expanded features based on Deque's mix of exceptional and non-exceptional methods.
 *
 * @param <T> the generic type of items
 */
public interface Lisque<T> extends List<T>, Deque<T>, Collection<T> {
	boolean add(@Nullable T t);

	void add(int index, @Nullable T element);

	@Nullable
	T set(int index, @Nullable T element);

	boolean contains(@Nullable Object o);

	boolean containsAll(Collection<@Nullable ?> c);

	boolean removeAll(Collection<@Nullable ?> c);

	boolean retainAll(Collection<@Nullable ?> c);

	void sort(@Nullable Comparator<? super T> c);

	int indexOf(@Nullable Object o);

	int lastIndexOf(@Nullable Object o);

	default boolean insert(int index, @Nullable T item) {
		add(index, item);
		return true;
	}

	default boolean insertAll(int index, Collection<@Nullable ? extends T> c) {
		return addAll(index, c);
	}

	default boolean addAllLast(Collection<@Nullable ? extends T> c) {
		return addAll(c);
	}

	boolean addAllFirst(Collection<? extends T> c);

	boolean addAll(@Nullable T[] array);

	boolean addAll(@Nullable T[] array, int offset, int length);

	boolean addAll(int index, @Nullable T[] array);

	boolean addAll(int index, @Nullable T[] array, int offset, int length);

	default boolean insertAll(int index, @Nullable T[] array) {
		return addAll(index, array);
	}

	default boolean insertAll(int index, @Nullable T[] array, int offset, int length) {
		return addAll(index, array, offset, length);
	}

	default boolean addAllLast(@Nullable T[] array) {
		return addAll(array);
	}

	default boolean addAllLast(@Nullable T[] array, int offset, int length) {
		return addAll(array, offset, length);
	}

	boolean addAllFirst(@Nullable T[] array);

	boolean addAllFirst(@Nullable T[] array, int offset, int length);

	boolean retainAll(@Nullable Object[] array);

	boolean retainAll(@Nullable Object[] array, int offset, int length);

	void truncate(int newSize);

	default void truncateLast(int newSize) {
		truncate(newSize);
	}

	void truncateFirst(int newSize);

	void removeRange(int fromIndex, int toIndex);

	int indexOf(@Nullable Object value, int fromIndex);

	int lastIndexOf(@Nullable Object value, int fromIndex);

	default @Nullable T removeAt(int index) {
		return remove(index);
	}

	@Nullable
	T poll(int index);

	default @Nullable T pollAt(int index) {
		return poll(index);
	}

	default boolean notEmpty() {
		return !isEmpty();
	}

	@Nullable
	T last();

	@Nullable
	T peekAt(int index);

	@Nullable
	T random(Random random);

	default @Nullable T peekRandom(Random random) {
		return peekAt(random.nextInt(size()));
	}

	default boolean offerFirst(@Nullable T t) {
		addFirst(t);
		return true;

	}

	default boolean offerLast(@Nullable T t) {
		addLast(t);
		return true;
	}

	default @Nullable T pollFirst() {
		if (isEmpty()) return null;
		return removeFirst();
	}

	default @Nullable T pollLast() {
		if (isEmpty()) return null;
		return removeLast();
	}

	default @Nullable T peekFirst() {
		if (isEmpty()) return null;
		return getFirst();
	}

	default @Nullable T peekLast() {
		if (isEmpty()) return null;
		return getLast();
	}

	default boolean removeFirstOccurrence(@Nullable Object o) {
		int idx = indexOf(o);
		if (idx == -1) return false;
		removeAt(idx);
		return true;
	}

	default boolean removeLastOccurrence(@Nullable Object o) {
		int idx = lastIndexOf(o);
		if (idx == -1) return false;
		removeAt(idx);
		return true;
	}

	default boolean offer(@Nullable T t) {
		addLast(t);
		return true;
	}

	default @Nullable T remove() {
		return removeFirst();
	}

	default @Nullable T poll() {
		return pollFirst();
	}

	default @Nullable T element() {
		return getFirst();
	}

	default @Nullable T peek() {
		return isEmpty() ? null : getFirst();
	}

	default void push(@Nullable T t) {
		addFirst(t);
	}

	default @Nullable T pop() {
		return removeFirst();
	}

	default boolean isEmpty() {
		return size() == 0;
	}

	void addFirst(@Nullable T t);

	void addLast(@Nullable T t);

	@Nullable
	T getFirst();

	@Nullable
	T getLast();

	@Nullable
	T removeFirst();

	@Nullable
	T removeLast();

	Lisque<T> reversed();

	Iterator<T> iterator();

	ListIterator<T> listIterator();

	ListIterator<T> listIterator(int index);

	Iterator<T> descendingIterator();
}

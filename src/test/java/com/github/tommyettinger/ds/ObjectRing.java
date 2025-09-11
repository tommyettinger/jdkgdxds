/*
 * Copyright (c) 2023-2025 See AUTHORS file.
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

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Random;

/**
 * A fixed-capacity ring buffer built around {@link ObjectDeque}
 * that only supports adding to the buffer, not removing.
 * Removes items from the start instead of changing its size.
 * This keeps items at their same index even when earlier items
 * have been removed (or "forgotten"), though iteration will
 * start at the first valid item, and won't go through potentially
 * many forgotten items. If you use the typical Deque API of
 * {@link #addLast(Object)}, {@link #getFirst()}, etc. you won't
 * notice a difference. If you use the index-based part of the
 * API, such as {@link #get(int)}, then the first valid index is
 * generally obtainable with {@link #getForgotten()}, and the limit
 * (one after the last valid index) is obtainable with
 * {@link #getLimit()}.
 */
public class ObjectRing<T> extends ObjectDeque<T> {
	protected int forgotten = 0;

	public ObjectRing() {
		super();
	}

	public ObjectRing(int initialSize) {
		super(initialSize);
	}

	public ObjectRing(Collection<? extends T> coll) {
		super(coll);
	}

	public ObjectRing(ObjectDeque<? extends T> deque) {
		super(deque);
	}

	public ObjectRing(T[] a) {
		super(a);
	}

	public ObjectRing(T[] a, int offset, int count) {
		super(a, offset, count);
	}

	public int capacity() {
		return items.length;
	}

	@Override
	public void ensureCapacity(int additional) {
		throw new UnsupportedOperationException("Capacity cannot change size in a Ring buffer.");
	}

	@Override
	public void resize(int newSize) {
		super.removeFirst();
		forgotten++;
	}

	@Override
	public @Nullable T removeFirst() {
		throw new UnsupportedOperationException("Ring buffers are append-only.");
	}

	@Override
	public @Nullable T removeLast() {
		throw new UnsupportedOperationException("Ring buffers are append-only.");
	}

	@Override
	public @Nullable T pollFirst() {
		throw new UnsupportedOperationException("Ring buffers are append-only.");
	}

	@Override
	public @Nullable T pollLast() {
		throw new UnsupportedOperationException("Ring buffers are append-only.");
	}

	@Override
	public boolean removeFirstOccurrence(@Nullable Object o) {
		throw new UnsupportedOperationException("Ring buffers are append-only.");
	}

	@Override
	public boolean removeLastOccurrence(@Nullable Object o) {
		throw new UnsupportedOperationException("Ring buffers are append-only.");
	}

	@Override
	public @Nullable T remove() {
		throw new UnsupportedOperationException("Ring buffers are append-only.");
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException("Ring buffers are append-only.");
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException("Ring buffers are append-only.");
	}

	@Override
	public @Nullable T poll() {
		throw new UnsupportedOperationException("Ring buffers are append-only.");
	}

	@Override
	public @Nullable T pop() {
		throw new UnsupportedOperationException("Ring buffers are append-only.");
	}

	@Override
	public boolean remove(@Nullable Object o) {
		throw new UnsupportedOperationException("Ring buffers are append-only.");
	}

	@Override
	public boolean removeValue(@Nullable Object value, boolean identity) {
		throw new UnsupportedOperationException("Ring buffers are append-only.");
	}

	@Override
	public boolean removeLastValue(@Nullable Object value, boolean identity) {
		throw new UnsupportedOperationException("Ring buffers are append-only.");
	}

	@Override
	public void add(int index, @Nullable T item) {
		if (index >= forgotten)
			super.add(index - forgotten, item);
	}

	@Override
	public boolean insert(int index, @Nullable T element) {
		if (index < forgotten) return false;
		return super.insert(index - forgotten, element);
	}

	@Override
	public @Nullable T removeAt(int index) {
		throw new UnsupportedOperationException("Ring buffers are append-only.");
	}

	@Override
	public @Nullable T remove(int index) {
		throw new UnsupportedOperationException("Ring buffers are append-only.");
	}

	@Override
	public @Nullable T get(int index) {
		if (index < forgotten) return defaultValue;
		return super.get(index - forgotten);
	}

	@Override
	public @Nullable T set(int index, @Nullable T item) {
		if (index < forgotten) return defaultValue;
		return super.set(index - forgotten, item);
	}

	@Override
	public @Nullable T random(Random random) {
		if (size <= 0) {
			throw new NoSuchElementException("ObjectRing is empty.");
		}
		return super.get(random.nextInt(size));
	}

	/**
	 * Gets how many items have been forgotten because they were present
	 * at the head of the ObjectRing when a new item was added.
	 * The int this returns is typically the minimum value for index arguments here.
	 *
	 * @return the number of items that have been forgotten
	 */
	public int getForgotten() {
		return forgotten;
	}

	/**
	 * Gets the upper exclusive limit for indices in the index-based API here.
	 * This is equivalent to {@link #getForgotten()} plus {@link #size()}.
	 *
	 * @return the upper exclusive limit for indices
	 */
	public int getLimit() {
		return forgotten + size;
	}

	public static <T> ObjectRing<T> with(T item) {
		ObjectRing<T> deque = new ObjectRing<>(1);
		deque.add(item);
		return deque;
	}

	@SafeVarargs
	public static <T> ObjectRing<T> with(T... items) {
		return new ObjectRing<>(items);
	}
}

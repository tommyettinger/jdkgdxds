/*
 * Copyright (c) 2023 See AUTHORS file.
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

import java.util.Collection;

/**
 * A fixed-size ring buffer built around {@link ObjectDeque}.
 * Removes items from the start instead of changing its size.
 */
public class ObjectRing<T> extends ObjectDeque<T> {
	public ObjectRing () {
		super();
	}

	public ObjectRing (int initialSize) {
		super(initialSize);
	}

	public ObjectRing (Collection<? extends T> coll) {
		super(coll);
	}

	public ObjectRing (ObjectDeque<? extends T> deque) {
		super(deque);
	}

	public ObjectRing (T[] a) {
		super(a);
	}

	public ObjectRing (T[] a, int offset, int count) {
		super(a, offset, count);
	}

	public int capacity() {
		return values.length;
	}

	@Override
	public void ensureCapacity (int additional) {
		throw new UnsupportedOperationException("Capacity cannot change size in a Ring buffer.");
	}

	@Override
	protected void resize (int newSize) {
		removeFirst();
	}

	public static <T> ObjectRing<T> with (T item) {
		ObjectRing<T> deque = new ObjectRing<>(1);
		deque.add(item);
		return deque;
	}

	@SafeVarargs
	public static <T> ObjectRing<T> with (T... items) {
		return new ObjectRing<>(items);
	}
}

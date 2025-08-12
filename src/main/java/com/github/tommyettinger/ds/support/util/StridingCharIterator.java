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

package com.github.tommyettinger.ds.support.util;

import java.util.NoSuchElementException;

/**
 * Wraps a CharIterator so that it starts at an offset, skipping that many items, then returning items that match a given
 * stride, such as every other item, or every tenth item. If the offset is 0, this will try to return the first item and
 * then any items matching the stride, so if the stride is 2, it will return the first item, skip one item, return the
 * item after that, skip, return, skip, return, etc.
 * This has undefined behavior if any items the CharIterator could return are modified during iteration.
 * <br>
 * You can change the iterator, offset, and stride at once using {@link #set(CharIterator, int, int)}, and can also just
 * change the iterator with {@link #set(CharIterator)}.
 */
public class StridingCharIterator implements CharIterator {
	public CharIterator iterator;
	protected int offset = 0;
	protected int stride = 2;
	protected int index = -1;
	protected boolean end = false;
	protected boolean available = false;
	protected char next;

	public StridingCharIterator() {
	}

	public StridingCharIterator(final CharIterator iterator, int offset, int stride) {
		set(iterator, offset, stride);
	}

	public void set(final CharIterator iterator, int offset, int stride) {
		this.iterator = iterator;
		this.offset = Math.max(0, offset);
		this.stride = Math.max(1, stride);
		index = -1;
		end = available = false;
	}

	public void set(final CharIterator iterator) {
		set(iterator, offset, stride);
	}

	@Override
	public boolean hasNext() {
		if (end) return false;
		if (available) return true;
		while (iterator.hasNext()) {
			final char n = iterator.next();
			if (++index >= offset && (index - offset) % stride == 0) {
				next = n;
				available = true;
				return true;
			}
		}
		end = true;
		return false;
	}

	@Override
	public char nextChar() {
		if (!available && !hasNext()) throw new NoSuchElementException("No elements remaining.");
		final char result = next;
		available = false;
		return result;
	}

	/**
	 * NOTE: this does not change the stride or offset, so the same sequence of values will be returned regardless of if
	 * some elements are removed with this method.
	 */
	@Override
	public void remove() {
		if (available) throw new IllegalStateException("Cannot remove between a call to hasNext() and next().");
		iterator.remove();
	}
}

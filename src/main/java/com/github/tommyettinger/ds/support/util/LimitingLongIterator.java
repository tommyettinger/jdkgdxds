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

/**
 * Wraps an Iterator so that it only returns at most a specific amount of items (defined by calls to
 * {@link #nextLong()}. This can be useful to limit infinite Iterators so they only produce a finite amount of results.
 * This has undefined behavior if any items the Iterator could return are modified during iteration.
 * <br>
 * You can change the Iterator and limit at once using {@link #set(LongIterator, int)}, and can also just
 * change the Iterator with {@link #set(LongIterator)}.
 */
public class LimitingLongIterator implements LongIterator {
	public LongIterator iterator;
	protected int limit = 1;
	protected int remaining = 1;

	public LimitingLongIterator() {
	}

	public LimitingLongIterator(final LongIterator iterator, int limit) {
		set(iterator, limit);
	}

	public void set(final LongIterator iterator, int limit) {
		this.iterator = iterator;
		this.remaining = this.limit = Math.max(0, limit);
	}

	public void set(final LongIterator iterator) {
		set(iterator, limit);
	}

	@Override
	public boolean hasNext() {
		return (iterator.hasNext() && remaining > 0);
	}

	@Override
	public long nextLong() {
		remaining--;
		return iterator.next();
	}

	@Override
	public void remove() {
		iterator.remove();
	}
}

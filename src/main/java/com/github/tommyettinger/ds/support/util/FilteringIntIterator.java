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

import com.github.tommyettinger.function.IntPredicate;

import java.util.NoSuchElementException;

/**
 * Wraps an IntIterator so that it skips any items for which {@link #filter} returns {@code false}. You can use
 * a lambda or other predicate that accepts {@code int} and returns true or false for the filter here.
 * This has undefined behavior if any items the IntIterator could return are modified during iteration.
 * <br>
 * You can change both the IntIterator and filter at once using {@link #set(IntIterator, IntPredicate)} , and can
 * also just change the Iterator with {@link #set(IntIterator)}.
 * <br>
 * Based very closely on {@code Predicate.PredicateIterator} from <a href="https://libgdx.com">libGDX</a>, but changed
 * to handle primitive items.
 */
public class FilteringIntIterator implements IntIterator {
	public IntIterator iterator;
	public IntPredicate filter;
	protected boolean end = false;
	protected boolean available = false;
	protected int next;

	public FilteringIntIterator() {
	}

	public FilteringIntIterator(final IntIterator iterator, final IntPredicate filter) {
		set(iterator, filter);
	}

	public void set(final IntIterator iterator, final IntPredicate predicate) {
		this.iterator = iterator;
		this.filter = predicate;
		end = available = false;
	}

	public void set(final IntIterator iterator) {
		set(iterator, filter);
	}

	@Override
	public boolean hasNext() {
		if (end) return false;
		if (available) return true;
		while (iterator.hasNext()) {
			final int n = iterator.nextInt();
			if (filter.test(n)) {
				next = n;
				available = true;
				return true;
			}
		}
		end = true;
		return false;
	}

	@Override
	public int nextInt() {
		if (!available && !hasNext()) throw new NoSuchElementException("No elements remaining.");
		final int result = next;
		available = false;
		return result;
	}

	@Override
	public void remove() {
		if (available) throw new IllegalStateException("Cannot remove between a call to hasNext() and next().");
		iterator.remove();
	}
}

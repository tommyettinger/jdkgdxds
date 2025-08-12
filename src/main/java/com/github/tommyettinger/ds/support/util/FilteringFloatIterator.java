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

import com.github.tommyettinger.function.FloatPredicate;

import java.util.NoSuchElementException;

/**
 * Wraps a FloatIterator so that it skips any items for which {@link #filter} returns {@code false}. You can use
 * a lambda or other predicate that accepts {@code float} and returns true or false for the filter here.
 * This has undefined behavior if any items the FloatIterator could return are modified during iteration.
 * <br>
 * You can change both the FloatIterator and filter at once using {@link #set(FloatIterator, FloatPredicate)} , and can
 * also just change the Iterator with {@link #set(FloatIterator)}.
 * <br>
 * Based very closely on {@code Predicate.PredicateIterator} from <a href="https://libgdx.com">libGDX</a>, but changed
 * to handle primitive items.
 */
public class FilteringFloatIterator implements FloatIterator {
	public FloatIterator iterator;
	public FloatPredicate filter;
	protected boolean end = false;
	protected boolean available = false;
	protected float next;

	public FilteringFloatIterator() {
	}

	public FilteringFloatIterator(final FloatIterator iterator, final FloatPredicate filter) {
		set(iterator, filter);
	}

	public void set(final FloatIterator iterator, final FloatPredicate predicate) {
		this.iterator = iterator;
		this.filter = predicate;
		end = available = false;
	}

	public void set(final FloatIterator iterator) {
		set(iterator, filter);
	}

	@Override
	public boolean hasNext() {
		if (end) return false;
		if (available) return true;
		while (iterator.hasNext()) {
			final float n = iterator.nextFloat();
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
	public float nextFloat() {
		if (!available && !hasNext()) throw new NoSuchElementException("No elements remaining.");
		final float result = next;
		available = false;
		return result;
	}

	@Override
	public void remove() {
		if (available) throw new IllegalStateException("Cannot remove between a call to hasNext() and next().");
		iterator.remove();
	}
}

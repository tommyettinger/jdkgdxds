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

import com.github.tommyettinger.function.BooleanPredicate;

import java.util.NoSuchElementException;

/**
 * Wraps a BooleanIterator so that it skips any items for which {@link #filter} returns {@code false}. You can use
 * a lambda or other predicate that accepts {@code boolean} and returns true or false for the filter here.
 * This has undefined behavior if any items the BooleanIterator could return are modified during iteration.
 * <br>
 * You can change both the BooleanIterator and filter at once using {@link #set(BooleanIterator, BooleanPredicate)} , and can
 * also just change the Iterator with {@link #set(BooleanIterator)}.
 * <br>
 * Based very closely on {@code Predicate.PredicateIterator} from <a href="https://libgdx.com">libGDX</a>, but changed
 * to handle primitive items.
 */
public class FilteringBooleanIterator implements BooleanIterator {
    public BooleanIterator iterator;
    public BooleanPredicate filter;
    protected boolean end = false;
    protected boolean available = false;
    protected boolean next;

    public FilteringBooleanIterator() {
    }

    public FilteringBooleanIterator(final BooleanIterator iterator, final BooleanPredicate filter) {
        set(iterator, filter);
    }

    public void set(final BooleanIterator iterator, final BooleanPredicate predicate) {
        this.iterator = iterator;
        this.filter = predicate;
        end = available = false;
    }

    public void set(final BooleanIterator iterator) {
        set(iterator, filter);
    }

    @Override
    public boolean hasNext() {
        if (end) return false;
        if (available) return true;
        while (iterator.hasNext()) {
            final boolean n = iterator.nextBoolean();
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
    public boolean nextBoolean() {
        if (!available && !hasNext()) throw new NoSuchElementException("No elements remaining.");
        final boolean result = next;
        available = false;
        return result;
    }

    @Override
    public void remove() {
        if (available) throw new IllegalStateException("Cannot remove between a call to hasNext() and next().");
        iterator.remove();
    }
}

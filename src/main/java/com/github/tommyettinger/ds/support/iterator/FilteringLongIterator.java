package com.github.tommyettinger.ds.support.iterator;

import com.github.tommyettinger.function.LongPredicate;

import java.util.NoSuchElementException;

/**
 * Wraps a LongIterator so that it skips any items for which {@link #filter} returns {@code false}. You can use
 * a lambda or other predicate that accepts {@code long} and returns true or false for the filter here.
 * This has undefined behavior if any items the LongIterator could return are modified during iteration.
 * <br>
 * You can change both the LongIterator and filter at once using {@link #set(LongIterator, LongPredicate)} , and can
 * also just change the Iterator with {@link #set(LongIterator)}.
 * <br>
 * Based very closely on {@code Predicate.PredicateIterator} from <a href="https://libgdx.com">libGDX</a>, but changed
 * to handle primitive items.
 */
public class FilteringLongIterator implements LongIterator {
    public LongIterator iterator;
    public LongPredicate filter;
    protected boolean end = false;
    protected boolean available = false;
    protected long next;

    public FilteringLongIterator() {
    }

    public FilteringLongIterator(final LongIterator iterator, final LongPredicate filter) {
        set(iterator, filter);
    }

    public void set(final LongIterator iterator, final LongPredicate predicate) {
        this.iterator = iterator;
        this.filter = predicate;
        end = available = false;
    }

    public void set(final LongIterator iterator) {
        set(iterator, filter);
    }

    @Override
    public boolean hasNext() {
        if (end) return false;
        if (available) return true;
        while (iterator.hasNext()) {
            final long n = iterator.nextLong();
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
    public long nextLong() {
        if (!available && !hasNext()) throw new NoSuchElementException("No elements remaining.");
        final long result = next;
        available = false;
        return result;
    }

    @Override
    public void remove() {
        if (available) throw new IllegalStateException("Cannot remove between a call to hasNext() and next().");
        iterator.remove();
    }
}

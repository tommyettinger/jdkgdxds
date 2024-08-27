package com.github.tommyettinger.ds.support.iterator;

import com.github.tommyettinger.function.ShortPredicate;

import java.util.NoSuchElementException;

/**
 * Wraps a ShortIterator so that it skips any items for which {@link #filter} returns {@code false}. You can use
 * a lambda or other predicate that accepts {@code short} and returns true or false for the filter here.
 * This has undefined behavior if any items the ShortIterator could return are modified during iteration.
 * <br>
 * You can change both the ShortIterator and filter at once using {@link #set(ShortIterator, ShortPredicate)} , and can
 * also just change the Iterator with {@link #set(ShortIterator)}.
 * <br>
 * Based very closely on {@code Predicate.PredicateIterator} from <a href="https://libgdx.com">libGDX</a>, but changed
 * to handle primitive items.
 */
public class FilteringShortIterator implements ShortIterator {
    public ShortIterator iterator;
    public ShortPredicate filter;
    protected boolean end = false;
    protected boolean available = false;
    protected short next;

    public FilteringShortIterator() {
    }

    public FilteringShortIterator(final ShortIterator iterator, final ShortPredicate filter) {
        set(iterator, filter);
    }

    public void set(final ShortIterator iterator, final ShortPredicate predicate) {
        this.iterator = iterator;
        this.filter = predicate;
        end = available = false;
    }

    public void set(final ShortIterator iterator) {
        set(iterator, filter);
    }

    @Override
    public boolean hasNext() {
        if (end) return false;
        if (available) return true;
        while (iterator.hasNext()) {
            final short n = iterator.nextShort();
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
    public short nextShort() {
        if (!available && !hasNext()) throw new NoSuchElementException("No elements remaining.");
        final short result = next;
        available = false;
        return result;
    }

    @Override
    public void remove() {
        if (available) throw new IllegalStateException("Cannot remove between a call to hasNext() and next().");
        iterator.remove();
    }
}

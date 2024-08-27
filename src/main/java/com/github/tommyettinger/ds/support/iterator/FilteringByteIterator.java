package com.github.tommyettinger.ds.support.iterator;

import com.github.tommyettinger.function.BytePredicate;

import java.util.NoSuchElementException;

/**
 * Wraps a ByteIterator so that it skips any items for which {@link #filter} returns {@code false}. You can use
 * a lambda or other predicate that accepts {@code byte} and returns true or false for the filter here.
 * This has undefined behavior if any items the ByteIterator could return are modified during iteration.
 * <br>
 * You can change both the ByteIterator and filter at once using {@link #set(ByteIterator, BytePredicate)} , and can
 * also just change the Iterator with {@link #set(ByteIterator)}.
 * <br>
 * Based very closely on {@code Predicate.PredicateIterator} from <a href="https://libgdx.com">libGDX</a>, but changed
 * to handle primitive items.
 */
public class FilteringByteIterator implements ByteIterator {
    public ByteIterator iterator;
    public BytePredicate filter;
    protected boolean end = false;
    protected boolean available = false;
    protected byte next;

    public FilteringByteIterator() {
    }

    public FilteringByteIterator(final ByteIterator iterator, final BytePredicate filter) {
        set(iterator, filter);
    }

    public void set(final ByteIterator iterator, final BytePredicate predicate) {
        this.iterator = iterator;
        this.filter = predicate;
        end = available = false;
    }

    public void set(final ByteIterator iterator) {
        set(iterator, filter);
    }

    @Override
    public boolean hasNext() {
        if (end) return false;
        if (available) return true;
        while (iterator.hasNext()) {
            final byte n = iterator.nextByte();
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
    public byte nextByte() {
        if (!available && !hasNext()) throw new NoSuchElementException("No elements remaining.");
        final byte result = next;
        available = false;
        return result;
    }

    @Override
    public void remove() {
        if (available) throw new IllegalStateException("Cannot remove between a call to hasNext() and next().");
        iterator.remove();
    }
}

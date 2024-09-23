package com.github.tommyettinger.ds.support.util;

/**
 * Wraps an Iterator so that it only returns at most a specific amount of items (defined by calls to
 * {@link #nextBoolean()}. This can be useful to limit infinite Iterators so they only produce a finite amount of results.
 * This has undefined behavior if any items the Iterator could return are modified during iteration.
 * <br>
 * You can change the Iterator and limit at once using {@link #set(BooleanIterator, int)}, and can also just
 * change the Iterator with {@link #set(BooleanIterator)}.
 */
public class LimitingBooleanIterator implements BooleanIterator {
    public BooleanIterator iterator;
    protected int limit = 1;
    protected int remaining = 1;

    public LimitingBooleanIterator() {
    }

    public LimitingBooleanIterator(final BooleanIterator iterator, int limit) {
        set(iterator, limit);
    }

    public void set (final BooleanIterator iterator, int limit) {
        this.iterator = iterator;
        this.remaining = this.limit = Math.max(0, limit);
    }

    public void set (final BooleanIterator iterator) {
        set(iterator, limit);
    }

    @Override
    public boolean hasNext () {
        return (iterator.hasNext() && remaining > 0);
    }

    @Override
    public boolean nextBoolean () {
        remaining--;
        return iterator.next();
    }

    @Override
    public void remove () {
        iterator.remove();
    }
}

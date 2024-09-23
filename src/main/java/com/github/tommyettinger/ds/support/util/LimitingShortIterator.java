package com.github.tommyettinger.ds.support.util;

/**
 * Wraps an Iterator so that it only returns at most a specific amount of items (defined by calls to
 * {@link #nextShort()}. This can be useful to limit infinite Iterators so they only produce a finite amount of results.
 * This has undefined behavior if any items the Iterator could return are modified during iteration.
 * <br>
 * You can change the Iterator and limit at once using {@link #set(ShortIterator, int)}, and can also just
 * change the Iterator with {@link #set(ShortIterator)}.
 */
public class LimitingShortIterator implements ShortIterator {
    public ShortIterator iterator;
    protected int limit = 1;
    protected int remaining = 1;

    public LimitingShortIterator() {
    }

    public LimitingShortIterator(final ShortIterator iterator, int limit) {
        set(iterator, limit);
    }

    public void set (final ShortIterator iterator, int limit) {
        this.iterator = iterator;
        this.remaining = this.limit = Math.max(0, limit);
    }

    public void set (final ShortIterator iterator) {
        set(iterator, limit);
    }

    @Override
    public boolean hasNext () {
        return (iterator.hasNext() && remaining > 0);
    }

    @Override
    public short nextShort () {
        remaining--;
        return iterator.next();
    }

    @Override
    public void remove () {
        iterator.remove();
    }
}

package com.github.tommyettinger.ds.support.util;

/**
 * Wraps an Iterator so that it only returns at most a specific amount of items (defined by calls to
 * {@link #nextFloat()}. This can be useful to limit infinite Iterators so they only produce a finite amount of results.
 * This has undefined behavior if any items the Iterator could return are modified during iteration.
 * <br>
 * You can change the Iterator and limit at once using {@link #set(FloatIterator, int)}, and can also just
 * change the Iterator with {@link #set(FloatIterator)}.
 */
public class LimitingFloatIterator implements FloatIterator {
    public FloatIterator iterator;
    protected int limit = 1;
    protected int remaining = 1;

    public LimitingFloatIterator() {
    }

    public LimitingFloatIterator(final FloatIterator iterator, int limit) {
        set(iterator, limit);
    }

    public void set (final FloatIterator iterator, int limit) {
        this.iterator = iterator;
        this.remaining = this.limit = Math.max(0, limit);
    }

    public void set (final FloatIterator iterator) {
        set(iterator, limit);
    }

    @Override
    public boolean hasNext () {
        return (iterator.hasNext() && remaining > 0);
    }

    @Override
    public float nextFloat () {
        remaining--;
        return iterator.next();
    }

    @Override
    public void remove () {
        iterator.remove();
    }
}

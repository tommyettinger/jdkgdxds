package com.github.tommyettinger.ds.support.util;

import java.util.Iterator;

/**
 * Wraps an Iterator so that it only returns at most a specific amount of items (defined by calls to {@link #next()}.
 * This can be useful to limit infinite Iterators so they only produce a finite amount of results.
 * This has undefined behavior if any items the Iterator could return are modified during iteration.
 * <br>
 * You can change the Iterator and limit at once using {@link #set(Iterator, int)}, and can also just
 * change the Iterator with {@link #set(Iterator)}.
 * @param <T> the type of items this can return, and the type the wrapped Iterator returns
 */
public class LimitingIterator<T> implements Iterator<T> {
    public Iterator<T> iterator;
    protected int limit = 1;
    protected int remaining = 1;

    public LimitingIterator() {
    }

    public LimitingIterator(final Iterator<T> iterator, int limit) {
        set(iterator, limit);
    }

    public void set (final Iterator<T> iterator, int limit) {
        this.iterator = iterator;
        this.remaining = this.limit = Math.max(0, limit);
    }

    public void set (final Iterator<T> iterator) {
        set(iterator, limit);
    }

    @Override
    public boolean hasNext () {
        return (iterator.hasNext() && remaining > 0);
    }

    @Override
    public T next () {
        remaining--;
        return iterator.next();
    }

    @Override
    public void remove () {
        iterator.remove();
    }
}
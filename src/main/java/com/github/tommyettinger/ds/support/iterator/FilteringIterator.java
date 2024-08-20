package com.github.tommyettinger.ds.support.iterator;

import com.github.tommyettinger.function.ObjPredicate;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Wraps an Iterator so that it skips any items for which {@link #filter} returns {@code false}. You can use a lambda
 * or other predicate that accepts {@code T} and returns true or false for the filter here.
 * This has undefined behavior if any items the Iterator could return are modified during iteration.
 * <br>
 * You can change both the Iterator and filter at once using {@link #set(Iterator, ObjPredicate)}, and can also just
 * change the Iterator with {@link #set(Iterator)}.
 * <br>
 * Based very closely on {@code Predicate.PredicateIterator} from <a href="https://libgdx.com">libGDX</a>, but changed
 * to handle null items.
 * @param <T> the type of items this can return, and the type the wrapped Iterator returns
 */
public class FilteringIterator<T> implements Iterator<T> {
    public Iterator<T> iterator;
    public ObjPredicate<T> filter;
    protected boolean end = false;
    protected boolean available = false;
    protected T next = null;

    public FilteringIterator() {
    }

    public FilteringIterator (final Iterator<T> iterator, final ObjPredicate<T> filter) {
        set(iterator, filter);
    }

    public void set (final Iterator<T> iterator, final ObjPredicate<T> predicate) {
        this.iterator = iterator;
        this.filter = predicate;
        end = available = false;
        next = null;
    }

    public void set (final Iterator<T> iterator) {
        set(iterator, filter);
    }

    @Override
    public boolean hasNext () {
        if (end) return false;
        if (available) return true;
        while (iterator.hasNext()) {
            final T n = iterator.next();
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
    public T next () {
        if (!available && !hasNext()) throw new NoSuchElementException("No elements remaining.");
        final T result = next;
        next = null;
        available = false;
        return result;
    }

    @Override
    public void remove () {
        if (available) throw new IllegalStateException("Cannot remove between a call to hasNext() and next().");
        iterator.remove();
    }
}

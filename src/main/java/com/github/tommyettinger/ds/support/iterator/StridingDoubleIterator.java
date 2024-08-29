package com.github.tommyettinger.ds.support.iterator;

import java.util.NoSuchElementException;

/**
 * Wraps a DoubleIterator so that it starts at an offset, skipping that many items, then returning items that match a given
 * stride, such as every other item, or every tenth item. If the offset is 0, this will try to return the first item and
 * then any items matching the stride, so if the stride is 2, it will return the first item, skip one item, return the
 * item after that, skip, return, skip, return, etc.
 * This has undefined behavior if any items the DoubleIterator could return are modified during iteration.
 * <br>
 * You can change the iterator, offset, and stride at once using {@link #set(DoubleIterator, int, int)}, and can also just
 * change the iterator with {@link #set(DoubleIterator)}.
 */
public class StridingDoubleIterator implements DoubleIterator {
    public DoubleIterator iterator;
    protected int offset = 0;
    protected int stride = 2;
    protected int index = -1;
    protected boolean end = false;
    protected boolean available = false;
    protected double next;

    public StridingDoubleIterator() {
    }

    public StridingDoubleIterator(final DoubleIterator iterator, int offset, int stride) {
        set(iterator, offset, stride);
    }

    public void set (final DoubleIterator iterator, int offset, int stride) {
        this.iterator = iterator;
        this.offset = Math.max(0, offset);
        this.stride = Math.max(1, stride);
        end = available = false;
    }

    public void set (final DoubleIterator iterator) {
        set(iterator, offset, stride);
    }

    @Override
    public boolean hasNext () {
        if (end) return false;
        if (available) return true;
        while (iterator.hasNext()) {
            final double n = iterator.next();
            if (++index >= offset && (index - offset) % stride == 0) {
                next = n;
                available = true;
                return true;
            }
        }
        end = true;
        return false;
    }

    @Override
    public double nextDouble () {
        if (!available && !hasNext()) throw new NoSuchElementException("No elements remaining.");
        final double result = next;
        available = false;
        return result;
    }

    /**
     * NOTE: this does not change the stride or offset, so the same sequence of values will be returned regardless of if
     * some elements are removed with this method.
     */
    @Override
    public void remove () {
        if (available) throw new IllegalStateException("Cannot remove between a call to hasNext() and next().");
        iterator.remove();
    }
}

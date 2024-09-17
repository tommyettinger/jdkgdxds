package com.github.tommyettinger.ds.support.util;

import com.github.tommyettinger.function.ShortToShortFunction;

import java.util.NoSuchElementException;

/**
 * Wraps an Iterator so that it calls a function on each item it would otherwise return, and returns that function's
 * result.
 * If you need to return a different type than what the iterator returns, you can use an {@link AlteringIterator}.
 * This has undefined behavior if any items the Iterator could return are modified during iteration.
 * <br>
 * You can change both the Iterator and editor at once using {@link #set(ShortIterator, ShortToShortFunction)} ,
 * and can also just change the Iterator with {@link #set(ShortIterator)}.
 */
public class EditingShortIterator implements ShortIterator {
    public ShortIterator iterator;
    public ShortToShortFunction editor;

    public EditingShortIterator() {
    }

    public EditingShortIterator(final ShortIterator iterator, final ShortToShortFunction editor) {
        set(iterator, editor);
    }

    public void set (final ShortIterator iterator, final ShortToShortFunction editor) {
        this.iterator = iterator;
        this.editor = editor;
    }

    public void set (final ShortIterator iterator) {
        set(iterator, editor);
    }

    @Override
    public boolean hasNext () {
        return iterator.hasNext();
    }

    @Override
    public short nextShort () {
        if (!hasNext()) throw new NoSuchElementException("No elements remaining.");
        return editor.applyAsShort(iterator.nextShort());
    }

    @Override
    public void remove () {
        iterator.remove();
    }
}

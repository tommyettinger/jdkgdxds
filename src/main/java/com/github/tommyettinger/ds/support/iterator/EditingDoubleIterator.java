package com.github.tommyettinger.ds.support.iterator;

import com.github.tommyettinger.function.DoubleToDoubleFunction;

import java.util.NoSuchElementException;

/**
 * Wraps an Iterator so that it calls a function on each item it would otherwise return, and returns that function's
 * result.
 * If you need to return a different type than what the iterator returns, you can use an {@link AlteringIterator}.
 * This has undefined behavior if any items the Iterator could return are modified during iteration.
 * <br>
 * You can change both the Iterator and editor at once using {@link #set(DoubleIterator, DoubleToDoubleFunction)} ,
 * and can also just change the Iterator with {@link #set(DoubleIterator)}.
 */
public class EditingDoubleIterator implements DoubleIterator {
    public DoubleIterator iterator;
    public DoubleToDoubleFunction editor;

    public EditingDoubleIterator() {
    }

    public EditingDoubleIterator(final DoubleIterator iterator, final DoubleToDoubleFunction editor) {
        set(iterator, editor);
    }

    public void set (final DoubleIterator iterator, final DoubleToDoubleFunction editor) {
        this.iterator = iterator;
        this.editor = editor;
    }

    public void set (final DoubleIterator iterator) {
        set(iterator, editor);
    }

    @Override
    public boolean hasNext () {
        return iterator.hasNext();
    }

    @Override
    public double nextDouble () {
        if (!hasNext()) throw new NoSuchElementException("No elements remaining.");
        return editor.applyAsDouble(iterator.nextDouble());
    }

    @Override
    public void remove () {
        iterator.remove();
    }
}

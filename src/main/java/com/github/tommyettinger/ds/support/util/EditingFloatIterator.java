package com.github.tommyettinger.ds.support.util;

import com.github.tommyettinger.function.FloatToFloatFunction;

import java.util.NoSuchElementException;

/**
 * Wraps an Iterator so that it calls a function on each item it would otherwise return, and returns that function's
 * result.
 * If you need to return a different type than what the iterator returns, you can use an {@link AlteringIterator}.
 * This has undefined behavior if any items the Iterator could return are modified during iteration.
 * <br>
 * You can change both the Iterator and editor at once using {@link #set(FloatIterator, FloatToFloatFunction)} ,
 * and can also just change the Iterator with {@link #set(FloatIterator)}.
 */
public class EditingFloatIterator implements FloatIterator {
    public FloatIterator iterator;
    public FloatToFloatFunction editor;

    public EditingFloatIterator() {
    }

    public EditingFloatIterator(final FloatIterator iterator, final FloatToFloatFunction editor) {
        set(iterator, editor);
    }

    public void set (final FloatIterator iterator, final FloatToFloatFunction editor) {
        this.iterator = iterator;
        this.editor = editor;
    }

    public void set (final FloatIterator iterator) {
        set(iterator, editor);
    }

    @Override
    public boolean hasNext () {
        return iterator.hasNext();
    }

    @Override
    public float nextFloat () {
        if (!hasNext()) throw new NoSuchElementException("No elements remaining.");
        return editor.applyAsFloat(iterator.nextFloat());
    }

    @Override
    public void remove () {
        iterator.remove();
    }
}

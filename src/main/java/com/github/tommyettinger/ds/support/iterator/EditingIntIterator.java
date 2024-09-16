package com.github.tommyettinger.ds.support.iterator;

import com.github.tommyettinger.function.IntToIntFunction;

import java.util.NoSuchElementException;

/**
 * Wraps an Iterator so that it calls a function on each item it would otherwise return, and returns that function's
 * result.
 * If you need to return a different type than what the iterator returns, you can use an {@link AlteringIterator}.
 * This has undefined behavior if any items the Iterator could return are modified during iteration.
 * <br>
 * You can change both the Iterator and editor at once using {@link #set(IntIterator, IntToIntFunction)} ,
 * and can also just change the Iterator with {@link #set(IntIterator)}.
 */
public class EditingIntIterator implements IntIterator {
    public IntIterator iterator;
    public IntToIntFunction editor;

    public EditingIntIterator() {
    }

    public EditingIntIterator(final IntIterator iterator, final IntToIntFunction editor) {
        set(iterator, editor);
    }

    public void set (final IntIterator iterator, final IntToIntFunction editor) {
        this.iterator = iterator;
        this.editor = editor;
    }

    public void set (final IntIterator iterator) {
        set(iterator, editor);
    }

    @Override
    public boolean hasNext () {
        return iterator.hasNext();
    }

    @Override
    public int nextInt () {
        if (!hasNext()) throw new NoSuchElementException("No elements remaining.");
        return editor.applyAsInt(iterator.nextInt());
    }

    @Override
    public void remove () {
        iterator.remove();
    }
}

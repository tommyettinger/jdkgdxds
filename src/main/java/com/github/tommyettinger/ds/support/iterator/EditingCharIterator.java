package com.github.tommyettinger.ds.support.iterator;

import com.github.tommyettinger.function.CharToCharFunction;

import java.util.NoSuchElementException;

/**
 * Wraps an Iterator so that it calls a function on each item it would otherwise return, and returns that function's
 * result.
 * If you need to return a different type than what the iterator returns, you can use an {@link AlteringIterator}.
 * This has undefined behavior if any items the Iterator could return are modified during iteration.
 * <br>
 * You can change both the Iterator and editor at once using {@link #set(CharIterator, CharToCharFunction)} ,
 * and can also just change the Iterator with {@link #set(CharIterator)}.
 */
public class EditingCharIterator implements CharIterator {
    public CharIterator iterator;
    public CharToCharFunction editor;

    public EditingCharIterator() {
    }

    public EditingCharIterator(final CharIterator iterator, final CharToCharFunction editor) {
        set(iterator, editor);
    }

    public void set (final CharIterator iterator, final CharToCharFunction editor) {
        this.iterator = iterator;
        this.editor = editor;
    }

    public void set (final CharIterator iterator) {
        set(iterator, editor);
    }

    @Override
    public boolean hasNext () {
        return iterator.hasNext();
    }

    @Override
    public char nextChar () {
        if (!hasNext()) throw new NoSuchElementException("No elements remaining.");
        return editor.applyAsChar(iterator.nextChar());
    }

    @Override
    public void remove () {
        iterator.remove();
    }
}

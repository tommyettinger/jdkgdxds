package com.github.tommyettinger.ds.support.iterator;

import com.github.tommyettinger.function.ByteToByteFunction;

import java.util.NoSuchElementException;

/**
 * Wraps an Iterator so that it calls a function on each item it would otherwise return, and returns that function's
 * result.
 * If you need to return a different type than what the iterator returns, you can use an {@link AlteringIterator}.
 * This has undefined behavior if any items the Iterator could return are modified during iteration.
 * <br>
 * You can change both the Iterator and editor at once using {@link #set(ByteIterator, ByteToByteFunction)} ,
 * and can also just change the Iterator with {@link #set(ByteIterator)}.
 */
public class EditingByteIterator implements ByteIterator {
    public ByteIterator iterator;
    public ByteToByteFunction editor;

    public EditingByteIterator() {
    }

    public EditingByteIterator(final ByteIterator iterator, final ByteToByteFunction editor) {
        set(iterator, editor);
    }

    public void set (final ByteIterator iterator, final ByteToByteFunction editor) {
        this.iterator = iterator;
        this.editor = editor;
    }

    public void set (final ByteIterator iterator) {
        set(iterator, editor);
    }

    @Override
    public boolean hasNext () {
        return iterator.hasNext();
    }

    @Override
    public byte nextByte () {
        if (!hasNext()) throw new NoSuchElementException("No elements remaining.");
        return editor.applyAsByte(iterator.nextByte());
    }

    @Override
    public void remove () {
        iterator.remove();
    }
}

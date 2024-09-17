package com.github.tommyettinger.ds.support.util;

import com.github.tommyettinger.function.ObjToSameFunction;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Wraps an Iterator so that it calls a function on each item it would otherwise return, and returns that function's
 * result.
 * If you need to return a different type than what the iterator returns, you can use an {@link AlteringIterator}.
 * This has undefined behavior if any items the Iterator could return are modified during iteration.
 * <br>
 * You can change both the Iterator and editor at once using {@link #set(Iterator, ObjToSameFunction)}, and can also
 * just change the Iterator with {@link #set(Iterator)}.
 *
 * @param <T> the type of items this can return, and the type the wrapped Iterator returns
 */
public class EditingIterator<T> implements Iterator<T> {
    public Iterator<T> iterator;
    public ObjToSameFunction<T> editor;

    public EditingIterator() {
    }

    public EditingIterator(final Iterator<T> iterator, final ObjToSameFunction<T> editor) {
        set(iterator, editor);
    }

    public void set (final Iterator<T> iterator, final ObjToSameFunction<T> editor) {
        this.iterator = iterator;
        this.editor = editor;
    }

    public void set (final Iterator<T> iterator) {
        set(iterator, editor);
    }

    @Override
    public boolean hasNext () {
        return iterator.hasNext();
    }

    @Override
    public T next () {
        if (!hasNext()) throw new NoSuchElementException("No elements remaining.");
        return editor.apply(iterator.next());
    }

    @Override
    public void remove () {
        iterator.remove();
    }
}

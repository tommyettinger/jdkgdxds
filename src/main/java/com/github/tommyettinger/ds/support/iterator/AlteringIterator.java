package com.github.tommyettinger.ds.support.iterator;

import com.github.tommyettinger.function.ObjToObjFunction;
import com.github.tommyettinger.function.ObjToObjFunction;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Wraps an Iterator so that it calls a function on each item it would otherwise return, and returns that function's
 * result. That means this is just like {@link EditingIterator}, except this may return a different type than what the
 * iterator returns.
 * This has undefined behavior if any items the Iterator could return are modified during iteration.
 * <br>
 * You can change both the Iterator and editor at once using {@link #set(Iterator, ObjToObjFunction)}, and can also
 * just change the Iterator with {@link #set(Iterator)}.

 * @param <T> the type of items this can return, and the type the wrapped Iterator returns
 */
public class AlteringIterator<T, R> implements Iterator<R> {
    public Iterator<T> iterator;
    public ObjToObjFunction<T, R> editor;

    public AlteringIterator() {
    }

    public AlteringIterator(final Iterator<T> iterator, final ObjToObjFunction<T, R> editor) {
        set(iterator, editor);
    }

    public void set (final Iterator<T> iterator, final ObjToObjFunction<T, R> editor) {
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
    public R next () {
        if (!hasNext()) throw new NoSuchElementException("No elements remaining.");
        return editor.apply(iterator.next());
    }

    @Override
    public void remove () {
        iterator.remove();
    }
}

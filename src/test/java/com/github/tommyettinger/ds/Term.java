package com.github.tommyettinger.ds;

import java.util.Collection;

/**
 * Mostly internal; describes part of a {@link Junction}.
 * @param <T> the type being joined in the Junction; must be Comparable
 */
public interface Term<T extends Comparable<T>> extends Comparable<Term<T>> {
    boolean match(Collection<? extends T> seq);

    char symbol();

    String name();
}

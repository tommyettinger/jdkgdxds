package com.github.tommyettinger.ds;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.Objects;

public class Junction<T extends Comparable<T>> implements Term<T> {
    public final Term<T> root;

    public Junction() {
        root = new Any<>();
    }
    public Junction(Term<T> root) {
        this.root = canonicalize(root);
    }

    // TODO: NYI
    public Term<T> canonicalize(Term<T> term) {
        return term;
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof Junction)) return false;

        Junction<?> junction = (Junction<?>) o;
        return root.equals(junction.root);
    }

    @Override
    public int hashCode() {
        return root.hashCode();
    }

    @Override
    public boolean match(Collection<? extends T> seq) {
        return root.match(seq);
    }

    @Override
    public char symbol() {
        return '@';
    }

    @Override
    public String name() {
        return "(" + root + ")";
    }

    public static class Leaf<T extends Comparable<T>> implements Term<T>{
        public final @Nullable T item;

        public Leaf() {
            item = null;
        }
        public Leaf(@Nullable T item) {
            this.item = item;
        }

        @Override
        public boolean match(Collection<? extends T> seq) {
            return seq.contains(item);
        }

        @Override
        public char symbol() {
            return '=';
        }

        @Override
        public String name() {
            return "yes";
        }

        @Override
        public String toString() {
            return item == null ? "''" : item.toString();
        }

        @Override
        public final boolean equals(Object o) {
            if (!(o instanceof Junction.Leaf)) return Objects.equals(o, item);

            Leaf<?> leaf = (Leaf<?>) o;
            return Objects.equals(item, leaf.item);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(item);
        }
    }
    public static class Any<T extends Comparable<T>> implements Term<T>{
        public final ObjectList<Term<T>> contents;

        public Any() {
            contents = new ObjectList<>(0);
        }

        @SafeVarargs
        public Any(T... items) {
            contents = new ObjectList<>(items.length);
            for (int i = 0; i < items.length; i++) {
                contents.add(new Leaf<>(items[i]));
            }
            contents.sort();
        }

        public Any(Collection<Term<T>> coll) {
            contents = new ObjectList<>(coll);
            contents.sort();
        }

        @Override
        public boolean match(Collection<? extends T> seq) {
            for (int i = 0; i < contents.size(); i++) {
                if(contents.get(i).match(seq)) return true;
            }
            return false;
        }

        @Override
        public char symbol() {
            return '|';
        }

        @Override
        public String name() {
            return "any";
        }

        @Override
        public String toString() {
            return contents.toString("|", false, StringBuilder::append);
        }

        @Override
        public final boolean equals(Object o) {
            if (!(o instanceof Any)) return false;

            Any<?> any = (Any<?>) o;
            return contents.equals(any.contents);
        }

        @Override
        public int hashCode() {
            return contents.hashCode();
        }
    }
}

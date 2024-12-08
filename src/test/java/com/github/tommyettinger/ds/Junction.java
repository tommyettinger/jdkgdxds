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

    /**
     * Compares this object with the specified object for order.  Returns a
     * negative integer, zero, or a positive integer as this object is less
     * than, equal to, or greater than the specified object.
     *
     * <p>The implementor must ensure {@link Integer#signum
     * signum}{@code (x.compareTo(y)) == -signum(y.compareTo(x))} for
     * all {@code x} and {@code y}.  (This implies that {@code
     * x.compareTo(y)} must throw an exception if and only if {@code
     * y.compareTo(x)} throws an exception.)
     *
     * <p>The implementor must also ensure that the relation is transitive:
     * {@code (x.compareTo(y) > 0 && y.compareTo(z) > 0)} implies
     * {@code x.compareTo(z) > 0}.
     *
     * <p>Finally, the implementor must ensure that {@code
     * x.compareTo(y)==0} implies that {@code signum(x.compareTo(z))
     * == signum(y.compareTo(z))}, for all {@code z}.
     *
     * @param o the object to be compared.
     * @return a negative integer, zero, or a positive integer as this object
     * is less than, equal to, or greater than the specified object.
     * @throws NullPointerException if the specified object is null
     * @throws ClassCastException   if the specified object's type prevents it
     *                              from being compared to this object.
     * @apiNote It is strongly recommended, but <i>not</i> strictly required that
     * {@code (x.compareTo(y)==0) == (x.equals(y))}.  Generally speaking, any
     * class that implements the {@code Comparable} interface and violates
     * this condition should clearly indicate this fact.  The recommended
     * language is "Note: this class has a natural ordering that is
     * inconsistent with equals."
     */
    @Override
    public int compareTo(Term<T> o) {
        return root.compareTo(o);
    }

    public static class Leaf<T extends Comparable<T>> implements Term<T>{
        public final T item;

        public Leaf() {
            item = (T)(new Object());
        }
        public Leaf(T item) {
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
            return item.toString();
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

        @Override
        public int compareTo(Term<T> o) {
            return o instanceof Leaf ? item.compareTo(((Leaf<T>)o).item) : Integer.signum(o.symbol() - symbol());
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

        @Override
        public int compareTo(Term<T> o) {
            if(o instanceof Any) {
                Any<T> a = (Any<T>)o;
                if(contents.size() != a.contents.size())
                    return contents.size() - a.contents.size();
                for (int i = 0; i < contents.size(); i++) {
                    int comp = contents.get(i).compareTo(a.contents.get(i));
                    if(comp != 0) return comp;
                }
            }
            return Integer.signum(o.symbol() - symbol());
        }
    }
}

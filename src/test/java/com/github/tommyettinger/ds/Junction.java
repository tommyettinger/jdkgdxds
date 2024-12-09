package com.github.tommyettinger.ds;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.Objects;

/**
 * Matches potentially more than one {@code T} value in different ways against a supplied {@link Collection} of
 * {@code T}. This is inspired by the Junction type in <a href="https://docs.raku.org/type/Junction">Raku</a>, but
 * isn't totally equivalent. A Junction is the outermost parent of its hierarchy, and contains {@link Term} nodes.
 * Note, the {@link #equals(Object)} method is meant to compare two Junctions to see if they are equivalent, while
 * the {@link #match(Collection)} method is how you actually check if this Junction matches a Collection.
 *
 * @param <T> any Comparable type, such as String or any enum type
 */
public class Junction<T extends Comparable<T>> implements Term<T> {
    public Term<T> root;

    public Junction() {
        root = new Any<>();
    }
    public Junction(Term<T> root) {
        this.root = root.canonicalize();
    }

    private Junction(Class<Void> ignored, T item) {
        this.root = Leaf.of(item);
    }

    @Override
    public void appendChildren(Collection<Term<T>> appending) {
        appending.add(root);
    }

    @Override
    public @Nullable T value() {
        return null;
    }

    // TODO: NYI
    public Term<T> canonicalize() {
        return root.canonicalize();
    }

    public Junction<T> negate() {
        if(root instanceof Not) // not
            root = ((Not)root).term;
        else {
            root = Not.of(root);
        }
        return this;
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof Junction)) return Objects.equals(o, root);

        Junction<?> junction = (Junction<?>) o;
        return root.equals(junction.root);
    }

    @Override
    public int hashCode() {
        return root.hashCode();
    }

    @Override
    public boolean match(Collection<? extends T> coll) {
        return root.match(coll);
    }

    @Override
    public Collection<T> remove(Collection<T> coll) {
        return root.remove(coll);
    }

    @Override
    public char symbol() {
        return '@';
    }

    @Override
    public String name() {
        return "junction";
    }

    @Override
    public String toString() {
        return root.toString();
    }

    @Override
    public int compareTo(Term<T> o) {
        return root.compareTo(o);
    }
    public static <T extends Comparable<T>> Junction<T> of(T item) {
        return new Junction<>(Void.TYPE, item);
    }

    public static class Leaf<T extends Comparable<T>> implements Term<T>{
        public T item;

        public Leaf() {
        }
        public Leaf(T item) {
            this.item = item;
        }

        @Override
        public boolean match(Collection<? extends T> coll) {
            return coll.contains(item);
        }

        @Override
        public Collection<T> remove(Collection<T> coll) {
            coll.remove(item);
            return coll;
        }

        @Override
        public void appendChildren(Collection<Term<T>> appending) {
        }

        @Override
        public @Nullable T value() {
            return item;
        }

        @Override
        public Term<T> canonicalize() {
            return this;
        }

        @Override
        public char symbol() {
            return '=';
        }

        @Override
        public String name() {
            return "just";
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

        public static <T extends Comparable<T>> Leaf<T> of(T item) {
            return new Leaf<>(item);
        }
    }

    public static class Not<T extends Comparable<T>> implements Term<T>{
        public Term<T> term;

        public Not() {
        }
        public Not(T item) {
            this.term = Leaf.of(item);
        }
        private Not(Class<Void> ignored, Term<T> term) {
            this.term = term;
        }

        @Override
        public boolean match(Collection<? extends T> coll) {
            return !term.match(coll);
        }

        @Override
        public Collection<T> remove(Collection<T> coll) {
            ObjectList<T> list = new ObjectList<>(coll);
            term.remove(list);
            coll.removeAll(list);
            return coll;
        }

        @Override
        public void appendChildren(Collection<Term<T>> appending) {
            appending.add(term);
        }

        @Override
        public @Nullable T value() {
            return null;
        }

        @Override
        public Term<T> canonicalize() {
            return term instanceof Not ? ((Not<T>) term).term : this;
        }

        @Override
        public char symbol() {
            return '~';
        }

        @Override
        public String name() {
            return "not";
        }

        @Override
        public String toString() {
            return "!" + term;
        }

        @Override
        public final boolean equals(Object o) {
            if (!(o instanceof Junction.Not)) return !Objects.equals(o, term);

            Not<?> leaf = (Not<?>) o;
            return !Objects.equals(term, leaf.term);
        }

        @Override
        public int hashCode() {
            return ~Objects.hashCode(term);
        }

        @Override
        public int compareTo(Term<T> o) {
            return -(o instanceof Not ? term.compareTo(((Not<T>)o).term) : Integer.signum(o.symbol() - symbol()));
        }

        public static <T extends Comparable<T>> Not<T> of(Term<T> term) {
            return new Not<>(Void.TYPE, term);
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

        /**
         * Use via {@link #of(Term[])} instead of directly.
         * @param ignored {@link Void#TYPE}
         * @param terms an array of Terms that will be put into {@link #contents} and sorted
         */
        private Any(Class<Void> ignored, Term<T>[] terms) {
            contents = new ObjectList<>(terms);
            contents.sort();
        }

        @Override
        public boolean match(Collection<? extends T> coll) {
            for (int i = 0; i < contents.size(); i++) {
                if(contents.get(i).match(coll)) return true;
            }
            return false;
        }

        @Override
        public Collection<T> remove(Collection<T> coll) {
            for (int i = 0; i < contents.size(); i++) {
                contents.get(i).remove(coll);
            }
            return coll;
        }

        @Override
        public void appendChildren(Collection<Term<T>> appending) {
            appending.addAll(contents);
        }

        @Override
        public @Nullable T value() {
            return null;
        }

        @Override
        public Term<T> canonicalize() {
            for (int i = 0, n = contents.size(); i < n; i++) {
                Term<T> child = contents.get(i);
                if(child instanceof Any){
                    contents.removeAt(i--);
                    contents.addAll(((Any<T>) child).contents);
                }
            }
            for (int i = 0, n = contents.size(); i < n; i++) {
                contents.get(i).canonicalize();
            }
            return this;
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
            return contents.appendTo(new StringBuilder(contents.size() + 2).append('(')
                    , "|", false).append(')').toString();
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

        @SafeVarargs
        public static <T extends Comparable<T>> Any<T> of(Term<T>... terms){
            return new Any<>(Void.TYPE, terms);
        }
    }

    public static class All<T extends Comparable<T>> implements Term<T>{
        public final ObjectList<Term<T>> contents;

        public All() {
            contents = new ObjectList<>(0);
        }

        @SafeVarargs
        public All(T... items) {
            contents = new ObjectList<>(items.length);
            for (int i = 0; i < items.length; i++) {
                contents.add(new Leaf<>(items[i]));
            }
            contents.sort();
        }

        public All(Collection<Term<T>> coll) {
            contents = new ObjectList<>(coll);
            contents.sort();
        }

        /**
         * Use via {@link #of(Term[])} instead of directly.
         * @param ignored {@link Void#TYPE}
         * @param terms an array of Terms that will be put into {@link #contents} and sorted
         */
        private All(Class<Void> ignored, Term<T>[] terms) {
            contents = new ObjectList<>(terms);
            contents.sort();
        }

        @Override
        public boolean match(Collection<? extends T> coll) {
            for (int i = 0; i < contents.size(); i++) {
                if(!contents.get(i).match(coll)) return false;
            }
            return true;
        }

        @Override
        public Collection<T> remove(Collection<T> coll) {
            for (int i = 0; i < contents.size(); i++) {
                if(!contents.get(i).match(coll)) return coll;
            }
            for (int i = 0; i < contents.size(); i++) {
                contents.get(i).remove(coll);
            }
            return coll;
        }

        @Override
        public void appendChildren(Collection<Term<T>> appending) {
            appending.addAll(contents);
        }

        @Override
        public @Nullable T value() {
            return null;
        }

        @Override
        public Term<T> canonicalize() {
            for (int i = 0, n = contents.size(); i < n; i++) {
                Term<T> child = contents.get(i);
                if(child instanceof All){
                    contents.removeAt(i--);
                    contents.addAll(((All<T>) child).contents);
                }
            }
            for (int i = 0, n = contents.size(); i < n; i++) {
                contents.get(i).canonicalize();
            }
            return this;
        }

        @Override
        public char symbol() {
            return '&';
        }

        @Override
        public String name() {
            return "all";
        }

        @Override
        public String toString() {
            return contents.appendTo(new StringBuilder(contents.size() + 2).append('(')
                    , "&", false).append(')').toString();
        }

        @Override
        public final boolean equals(Object o) {
            if (!(o instanceof All)) return false;

            All<?> any = (All<?>) o;
            return contents.equals(any.contents);
        }

        @Override
        public int hashCode() {
            return contents.hashCode();
        }

        @Override
        public int compareTo(Term<T> o) {
            if(o instanceof All) {
                All<T> a = (All<T>)o;
                if(contents.size() != a.contents.size())
                    return contents.size() - a.contents.size();
                for (int i = 0; i < contents.size(); i++) {
                    int comp = contents.get(i).compareTo(a.contents.get(i));
                    if(comp != 0) return comp;
                }
            }
            return Integer.signum(o.symbol() - symbol());
        }

        @SafeVarargs
        public static <T extends Comparable<T>> All<T> of(Term<T>... terms){
            return new All<>(Void.TYPE, terms);
        }
    }

    public static class One<T extends Comparable<T>> implements Term<T>{
        public final ObjectList<Term<T>> contents;

        public One() {
            contents = new ObjectList<>(0);
        }

        @SafeVarargs
        public One(T... items) {
            contents = new ObjectList<>(items.length);
            for (int i = 0; i < items.length; i++) {
                contents.add(new Leaf<>(items[i]));
            }
            contents.sort();
        }

        public One(Collection<Term<T>> coll) {
            contents = new ObjectList<>(coll);
            contents.sort();
        }

        /**
         * Use via {@link #of(Term[])} instead of directly.
         * @param ignored {@link Void#TYPE}
         * @param terms an array of Terms that will be put into {@link #contents} and sorted
         */
        private One(Class<Void> ignored, Term<T>[] terms) {
            contents = new ObjectList<>(terms);
            contents.sort();
        }

        @Override
        public boolean match(Collection<? extends T> coll) {
            int count = 0;
            for (int i = 0; i < contents.size() && count <= 1; i++) {
                if(contents.get(i).match(coll)) count++;
            }
            return count == 1;
        }

        @Override
        public Collection<T> remove(Collection<T> coll) {
            for (int i = 0; i < contents.size(); i++) {
                if(contents.get(i).match(coll)) return contents.get(i).remove(coll);
            }
            return coll;
        }

        @Override
        public void appendChildren(Collection<Term<T>> appending) {
            appending.addAll(contents);
        }

        @Override
        public @Nullable T value() {
            return null;
        }

        @Override
        public Term<T> canonicalize() {
            for (int i = 0, n = contents.size(); i < n; i++) {
                contents.get(i).canonicalize();
            }
            return this;
        }

        @Override
        public char symbol() {
            return '^';
        }

        @Override
        public String name() {
            return "one";
        }

        @Override
        public String toString() {
            return contents.appendTo(new StringBuilder(contents.size() + 2).append('(')
                    , "^", false).append(')').toString();

        }

        @Override
        public final boolean equals(Object o) {
            if (!(o instanceof One)) return false;

            One<?> any = (One<?>) o;
            return contents.equals(any.contents);
        }

        @Override
        public int hashCode() {
            return contents.hashCode();
        }

        @Override
        public int compareTo(Term<T> o) {
            if(o instanceof One) {
                One<T> a = (One<T>)o;
                if(contents.size() != a.contents.size())
                    return contents.size() - a.contents.size();
                for (int i = 0; i < contents.size(); i++) {
                    int comp = contents.get(i).compareTo(a.contents.get(i));
                    if(comp != 0) return comp;
                }
            }
            return Integer.signum(o.symbol() - symbol());
        }

        @SafeVarargs
        public static <T extends Comparable<T>> One<T> of(Term<T>... terms){
            return new One<>(Void.TYPE, terms);
        }
    }

}

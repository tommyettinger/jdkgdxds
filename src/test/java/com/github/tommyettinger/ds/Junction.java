package com.github.tommyettinger.ds;

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
    public final Term<T> root;

    public Junction() {
        root = new Any<>();
    }
    public Junction(Term<T> root) {
        this.root = canonicalize(root);
    }

    private Junction(Class<Void> ignored, T item) {
        this.root = Leaf.of(item);
    }

    // TODO: NYI
    public Term<T> canonicalize(Term<T> term) {
        return term;
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
        public boolean match(Collection<? extends T> seq) {
            return !term.match(seq);
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
            return "not(" + term.toString() + ")";
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
        public boolean match(Collection<? extends T> seq) {
            for (int i = 0; i < contents.size(); i++) {
                if(!contents.get(i).match(seq)) return false;
            }
            return true;
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
            return contents.toString("&", false, StringBuilder::append);
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
        public boolean match(Collection<? extends T> seq) {
            int count = 0;
            for (int i = 0; i < contents.size() && count <= 1; i++) {
                if(contents.get(i).match(seq)) count++;
            }
            return count == 1;
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
            return contents.toString("^", false, StringBuilder::append);
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

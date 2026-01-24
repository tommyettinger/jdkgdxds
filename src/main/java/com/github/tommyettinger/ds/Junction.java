/*
 * Copyright (c) 2025 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.tommyettinger.ds;

import com.github.tommyettinger.ds.support.util.Appender;
import com.github.tommyettinger.function.ObjToObjFunction;

import java.io.IOException;
import java.util.Collection;
import java.util.Objects;

/**
 * Matches potentially more than one {@code T} value in different ways against a supplied {@link Collection} of
 * {@code T}. This is inspired by the Junction type in <a href="https://docs.raku.org/type/Junction">Raku</a>, but
 * isn't totally equivalent. A Junction is the outermost parent of its hierarchy, and contains a {@link Term} node.
 * Note, the {@link #equals(Object)} method is meant to compare two Junctions to see if they are equivalent, while
 * the {@link #match(Collection)} method is how you actually check if this Junction matches a Collection.
 * <br>
 * A Junction mostly provides the same API as any other Term type, but does also supply {@link #negate()}, which
 * can be useful when you don't want to use {@link #remove(Collection)} to remove matches, but instead want to
 * filter and keep only terms that match this Junction. Note that negate() modifies this Junction in-place, so you
 * might want to call negate() again after filtering.
 * <br>
 * There are several inner classes here, all {@link Term} types, which are used to actually implement the different
 * types of logic for different types of matching. {@link Leaf} is simplest, and simply wraps a single T instance in
 * a Term so it can be used with other Terms. {@link Not} negates matches on its Term item, so if {@code ==} would
 * make sense without a Not, {@code !=} would be used instead with a Not. {@link Any} has multiple Terms, and will
 * match if any of those Terms match. The contrasting type is {@link All}, which also has multiple Terms, but will
 * match only if all of those Terms match. Lastly, {@link One} is special, and matches only if exactly one of its
 * multiple Terms match. Any, All, and One are usually shown as taking two arguments, but can actually take 1 or more.
 * This is important for One because it still requires exactly one match even if 10 arguments are given.
 * <br>
 * This provides a static convenience method, {@link #parse(String)}, that can parse a Junction of String from a
 * String that may contain symbols for various terms, and/or parentheses. Given an input such as {@code a|b|c},
 * you get a Junction that will match any of "a", "b", or "c". Alternatively, an input such as
 * {@code (beef|turkey|veggie|warm melted cheese)&bun} will match a Collection that contains "beef" as well as
 * "bun", "turkey" as well as "bun", "veggie" as well as "bun", or "warm melted cheese" as well as "bun".
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
	public T value() {
		return null;
	}

	public Term<T> canonicalize() {
		return root.canonicalize();
	}

	public Junction<T> negate() {
		if (root instanceof Not) // not
			root = ((Not<T>) root).term;
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
	public <S extends CharSequence & Appendable> S appendTo(S sb, Appender<T> appender) {
		return root.appendTo(sb, appender);
	}

	@Override
	public int compareTo(Term<T> o) {
		return root.compareTo(o);
	}

	public static <T extends Comparable<T>> Junction<T> of(T item) {
		return new Junction<>(Void.TYPE, item);
	}

	/**
	 * Simply matches a single {@code T} value, with no additional Terms involved.
	 *
	 * @param <T> the Comparable type shared by all Terms in this Junction
	 */
	public static class Leaf<T extends Comparable<T>> implements Term<T> {
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
		public T value() {
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
		public <S extends CharSequence & Appendable> S appendTo(S sb, Appender<T> appender) {
			return appender.apply(sb, item);
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
			return o instanceof Leaf ? item.compareTo(((Leaf<T>) o).item) : (symbol() - o.symbol());
		}

		public static <T extends Comparable<T>> Leaf<T> of(T item) {
			return new Leaf<>(item);
		}
	}

	/**
	 * Takes a Term and treats a case where it matches or doesn't match as the opposite.
	 * This can take up to two Term parameters in its constructor, but it only uses the last one.
	 *
	 * @param <T> the Comparable type shared by all Terms in this Junction
	 */
	public static class Not<T extends Comparable<T>> implements Term<T> {
		public Term<T> term;

		public Not() {
		}

		public Not(T item) {
			this.term = Leaf.of(item);
		}

		public Not(Term<T> ignored, Term<T> right) {
			term = right;
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
		public T value() {
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
			return "~" + term;
		}

		@Override
		public <S extends CharSequence & Appendable> S appendTo(S sb, Appender<T> appender) {
			try {
				sb.append('~');
				term.appendTo(sb, appender);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return sb;
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
			return o instanceof Not ? term.compareTo(((Not<T>) o).term) : (symbol() - o.symbol());
		}

		public static <T extends Comparable<T>> Not<T> of(Term<T> term) {
			return new Not<>(Void.TYPE, term);
		}
	}

	/**
	 * Takes one or more Terms and matches if any of those Terms match.
	 *
	 * @param <T> the Comparable type shared by all Terms in this Junction
	 */
	public static class Any<T extends Comparable<T>> implements Term<T> {
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
		}

		public Any(Term<T> left, Term<T> right) {
			contents = ObjectList.with(left, right);
		}

		public Any(Collection<Term<T>> coll) {
			contents = new ObjectList<>(coll);
		}

		/**
		 * Use via {@link #of(Term[])} instead of directly.
		 *
		 * @param ignored {@link Void#TYPE}
		 * @param terms   an array of Terms that will be put into {@link #contents} and sorted
		 */
		private Any(Class<Void> ignored, Term<T>[] terms) {
			contents = new ObjectList<>(terms);
		}

		@Override
		public boolean match(Collection<? extends T> coll) {
			for (int i = 0; i < contents.size(); i++) {
				if (contents.get(i).match(coll)) return true;
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
		public T value() {
			return null;
		}

		@Override
		public Term<T> canonicalize() {
			for (int i = 0, n = contents.size(); i < n; i++) {
				Term<T> child = contents.get(i);
				if (child instanceof Any) {
					contents.removeAt(i--);
					contents.addAll(((Any<T>) child).contents);
				}
			}
			for (int i = 0, n = contents.size(); i < n; i++) {
				contents.get(i).canonicalize();
			}
			contents.sort();
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
		public <S extends CharSequence & Appendable> S appendTo(S sb, Appender<T> appender) {
			try {
				sb.append('(');
				contents.appendTo(sb, "|", false, Term.termAppender(appender));
				sb.append(')');
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return sb;
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
			if (o instanceof Any) {
				Any<T> a = (Any<T>) o;
				if (contents.size() != a.contents.size())
					return contents.size() - a.contents.size();
				for (int i = 0; i < contents.size(); i++) {
					int comp = contents.get(i).compareTo(a.contents.get(i));
					if (comp != 0) return comp;
				}
			}
			return (symbol() - o.symbol());
		}

		@SafeVarargs
		public static <T extends Comparable<T>> Any<T> of(Term<T>... terms) {
			return new Any<>(Void.TYPE, terms);
		}
	}

	/**
	 * Takes one or more Terms and matches if all of those Terms match.
	 *
	 * @param <T> the Comparable type shared by all Terms in this Junction
	 */
	public static class All<T extends Comparable<T>> implements Term<T> {
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
		}

		public All(Term<T> left, Term<T> right) {
			contents = ObjectList.with(left, right);
		}

		public All(Collection<Term<T>> coll) {
			contents = new ObjectList<>(coll);
		}

		/**
		 * Use via {@link #of(Term[])} instead of directly.
		 *
		 * @param ignored {@link Void#TYPE}
		 * @param terms   an array of Terms that will be put into {@link #contents} and sorted
		 */
		private All(Class<Void> ignored, Term<T>[] terms) {
			contents = new ObjectList<>(terms);
		}

		@Override
		public boolean match(Collection<? extends T> coll) {
			for (int i = 0; i < contents.size(); i++) {
				if (!contents.get(i).match(coll)) return false;
			}
			return true;
		}

		@Override
		public Collection<T> remove(Collection<T> coll) {
			for (int i = 0; i < contents.size(); i++) {
				if (!contents.get(i).match(coll)) return coll;
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
		public T value() {
			return null;
		}

		@Override
		public Term<T> canonicalize() {
			for (int i = 0, n = contents.size(); i < n; i++) {
				Term<T> child = contents.get(i);
				if (child instanceof All) {
					contents.removeAt(i--);
					contents.addAll(((All<T>) child).contents);
				}
			}
			for (int i = 0, n = contents.size(); i < n; i++) {
				contents.get(i).canonicalize();
			}
			contents.sort();
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
		public <S extends CharSequence & Appendable> S appendTo(S sb, Appender<T> appender) {
			try {
				sb.append('(');
				contents.appendTo(sb, "&", false, Term.termAppender(appender));
				sb.append(')');
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return sb;
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
			if (o instanceof All) {
				All<T> a = (All<T>) o;
				if (contents.size() != a.contents.size())
					return contents.size() - a.contents.size();
				for (int i = 0; i < contents.size(); i++) {
					int comp = contents.get(i).compareTo(a.contents.get(i));
					if (comp != 0) return comp;
				}
			}
			return (symbol() - o.symbol());
		}

		@SafeVarargs
		public static <T extends Comparable<T>> All<T> of(Term<T>... terms) {
			return new All<>(Void.TYPE, terms);
		}
	}

	/**
	 * Takes one or more Terms and matches if exactly one of those Terms matches.
	 *
	 * @param <T> the Comparable type shared by all Terms in this Junction
	 */
	public static class One<T extends Comparable<T>> implements Term<T> {
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
		}

		public One(Term<T> left, Term<T> right) {
			contents = ObjectList.with(left, right);
		}

		public One(Collection<Term<T>> coll) {
			contents = new ObjectList<>(coll);
		}

		/**
		 * Use via {@link #of(Term[])} instead of directly.
		 *
		 * @param ignored {@link Void#TYPE}
		 * @param terms   an array of Terms that will be put into {@link #contents} and sorted
		 */
		private One(Class<Void> ignored, Term<T>[] terms) {
			contents = new ObjectList<>(terms);
		}

		@Override
		public boolean match(Collection<? extends T> coll) {
			int count = 0;
			for (int i = 0; i < contents.size() && count <= 1; i++) {
				if (contents.get(i).match(coll)) count++;
			}
			return count == 1;
		}

		@Override
		public Collection<T> remove(Collection<T> coll) {
			for (int i = 0; i < contents.size(); i++) {
				if (contents.get(i).match(coll)) return contents.get(i).remove(coll);
			}
			return coll;
		}

		@Override
		public void appendChildren(Collection<Term<T>> appending) {
			appending.addAll(contents);
		}

		@Override
		public T value() {
			return null;
		}

		@Override
		public Term<T> canonicalize() {
			for (int i = 0, n = contents.size(); i < n; i++) {
				Term<T> child = contents.get(i);
				if (child instanceof One) {
					contents.removeAt(i--);
					contents.addAll(((One<T>) child).contents);
				}
			}
			for (int i = 0, n = contents.size(); i < n; i++) {
				contents.get(i).canonicalize();
			}
			contents.sort();
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
		public <S extends CharSequence & Appendable> S appendTo(S sb, Appender<T> appender) {
			try {
				sb.append('(');
				contents.appendTo(sb, "^", false, Term.termAppender(appender));
				sb.append(')');
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return sb;
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
			if (o instanceof One) {
				One<T> a = (One<T>) o;
				if (contents.size() != a.contents.size())
					return contents.size() - a.contents.size();
				for (int i = 0; i < contents.size(); i++) {
					int comp = contents.get(i).compareTo(a.contents.get(i));
					if (comp != 0) return comp;
				}
			}
			return (symbol() - o.symbol());
		}

		@SafeVarargs
		public static <T extends Comparable<T>> One<T> of(Term<T>... terms) {
			return new One<>(Void.TYPE, terms);
		}
	}

	static final ObjectIntMap<String> OPERATORS = ObjectIntMap.with("~", 27, "^", 9, "&", 6, "|", 3);

	/**
	 * Tokenizes a range of the String {@code text} from {@code start} inclusive to {@code end} exclusive.
	 * Returns an ObjectDeque of Strings; they should be considered "operator-like" if they are one of
	 * {@code ()|&^~} and otherwise are names.
	 *
	 * @param text  the String to tokenize
	 * @param start the first index to read from, inclusive
	 * @param end   the last index to stop reading before, exclusive
	 * @return an ObjectDeque of the tokenized Strings
	 */
	static ObjectDeque<String> lex(String text, int start, int end) {
		ObjectDeque<String> deque = new ObjectDeque<>(end - start >>> 1);
		StringBuilder sb = new StringBuilder(32);
		for (int i = start; i < end; i++) {
			char current = text.charAt(i);
			switch (current) {
				case '~':
					if (sb.length() > 0)
						deque.add(sb.toString());
					deque.add("");
					deque.add(String.valueOf(current));
					sb.setLength(0);
					break;
				case '(':
				case ')':
				case '|':
				case '&':
				case '^':
					if (sb.length() > 0)
						deque.add(sb.toString());
					deque.add(String.valueOf(current));
					sb.setLength(0);
					break;
				default:
					sb.append(current);
			}
		}
		if (sb.length() > 0)
			deque.add(sb.toString());
		return deque;
	}

	static boolean checkPrecedence(int opPrecedence, String other) {
		return OPERATORS.get(other) >= opPrecedence;
	}

	/**
	 * <a href="https://eddmann.com/posts/shunting-yard-implementation-in-java/">Credit to Edd Mann</a>.
	 * Edd's implementation operates on a StringBuilder, whereas we output another ObjectDeque, so the
	 * order needed some work.
	 *
	 * @param tokens typically produced by {@link #lex(String, int, int)}
	 * @return the tokens, rearranged in postfix order and with parentheses removed
	 */
	static ObjectDeque<String> shuntingYard(ObjectDeque<String> tokens) {
		ObjectDeque<String> output = new ObjectDeque<>(tokens.size()), stack = new ObjectDeque<>(16);

		for (String token : tokens) {
			if (OPERATORS.containsKey(token)) {
				int opPrecedence = OPERATORS.get(token);
				while (stack.notEmpty() && checkPrecedence(opPrecedence, stack.peek()))
					output.add(stack.pop());
				stack.push(token);
			} else if (token.equals("(")) {
				stack.push(token);
			} else if (token.equals(")")) {
				while (!"(".equals(stack.peek()))
					output.add(stack.pop());
				stack.pop();
			} else {
				output.add(token);
			}
		}

		while (stack.notEmpty())
			output.add(stack.pop());

		return output;
	}

	/**
	 * Parses the String {@code text} into one Junction of String.
	 *
	 * @param text the String to parse
	 * @return the resulting Junction of String
	 */
	public static Junction<String> parse(String text) {
		return parse(text, 0, text.length());
	}

	/**
	 * Parses a substring of {@code text} into one Junction of String. The {@code start} is inclusive and
	 * the {@code end} is exclusive.
	 *
	 * @param text  the String to parse
	 * @param start the first index to read from, inclusive
	 * @param end   the last index to stop reading before, exclusive
	 * @return the resulting Junction of String
	 */
	public static Junction<String> parse(String text, int start, int end) {
		return parse(String::toString, text, start, end);
	}

	/**
	 * Parses all of {@code text} into one Junction of T, creating T items from String sections using {@code converter}.
	 *
	 * @param converter converts String sections to T values to put in the Junction; an enum's {@code valueOf(String)} can work
	 * @param text      the String to parse
	 * @return the resulting Junction of String
	 */
	public static <T extends Comparable<T>> Junction<T> parse(ObjToObjFunction<String, T> converter, String text) {
		return parse(converter, text, 0, text.length());
	}

	/**
	 * Parses a substring of {@code text} into one Junction of T, creating T items from String sections using
	 * {@code converter}. The {@code start} is inclusive and the {@code end} is exclusive.
	 *
	 * @param converter converts String sections to T values to put in the Junction; an enum's {@code valueOf(String)} can work
	 * @param text      the String to parse
	 * @param start     the first index to read from, inclusive
	 * @param end       the last index to stop reading before, exclusive
	 * @return the resulting Junction of String
	 */
	public static <T extends Comparable<T>> Junction<T> parse(ObjToObjFunction<String, T> converter, String text, int start, int end) {
		ObjectDeque<String> tokens = lex(text, start, end);
		tokens = shuntingYard(tokens);
		ObjectDeque<Term<T>> terms = new ObjectDeque<>(tokens.size());
		for (String tok : tokens) {
			if (OPERATORS.containsKey(tok)) {
				Term<T> right = terms.removeLast(), left = terms.removeLast();
				switch (tok.charAt(0)) {
					case '~':
						terms.addLast(Not.of(right));
						break;
					case '^':
						terms.addLast(new One<>(left, right));
						break;
					case '&':
						terms.addLast(new All<>(left, right));
						break;
					case '|':
						terms.addLast(new Any<>(left, right));
						break;
				}
			} else {
				terms.addLast(Leaf.of(converter.apply(tok)));
			}
		}
		if (terms.isEmpty()) terms.addLast(Any.of());
		return new Junction<>(terms.getLast());
	}
}

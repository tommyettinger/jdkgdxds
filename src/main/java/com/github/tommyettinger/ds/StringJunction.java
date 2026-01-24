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

import java.io.IOException;
import java.util.Collection;
import java.util.Objects;

/**
 * Matches potentially more than one String value in different ways against a supplied {@link Collection} of
 * String. This is inspired by the Junction type in <a href="https://docs.raku.org/type/Junction">Raku</a>, but isn't
 * totally equivalent. A StringJunction is the outermost parent of its hierarchy, and contains a {@link Term} node.
 * Note, the {@link #equals(Object)} method is meant to compare two StringJunctions to see if they are equivalent, while
 * the {@link #match(Collection)} method is how you actually check if this StringJunction matches a Collection.
 * <br>
 * A StringJunction mostly provides the same API as any other Term type, but does also supply {@link #negate()}, which
 * can be useful when you don't want to use {@link #remove(Collection)} to remove matches, but instead want to
 * filter and keep only terms that match this StringJunction. Note that negate() modifies this StringJunction in-place,
 * so you might want to call negate() again after filtering.
 * <br>
 * There are several inner classes here, all {@link Term} types, which are used to actually implement the different
 * types of logic for different types of matching. {@link Leaf} is simplest, and simply wraps a single String instance
 * in a Term so it can be used with other Terms. {@link Not} negates matches on its Term item, so if {@code ==} would
 * make sense without a Not, {@code !=} would be used instead with a Not. {@link Any} has multiple Terms, and will
 * match if any of those Terms match. The contrasting type is {@link All}, which also has multiple Terms, but will
 * match only if all of those Terms match. Lastly, {@link One} is special, and matches only if exactly one of its
 * multiple Terms match. Any, All, and One are usually shown as taking two arguments, but can actually take 1 or more.
 * This is important for One because it still requires exactly one match even if 10 arguments are given.
 * <br>
 * This provides a static convenience method, {@link #parse(String)}, that can parse a StringJunction from a
 * String that may contain symbols for various terms, and/or parentheses. Given an input such as {@code a|b|c},
 * you get a StringJunction that will match any of "a", "b", or "c". Alternatively, an input such as
 * {@code (beef|turkey|veggie|warm melted cheese)&bun} will match a Collection that contains "beef" as well as
 * "bun", "turkey" as well as "bun", "veggie" as well as "bun", or "warm melted cheese" as well as "bun".
 */
public class StringJunction implements Term<String> {
	public Term<String> root;

	public StringJunction() {
		root = new Any();
	}

	public StringJunction(Term<String> root) {
		this.root = root.canonicalize();
	}

	private StringJunction(Class<Void> ignored, String item) {
		this.root = Leaf.of(item);
	}

	@Override
	public void appendChildren(Collection<Term<String>> appending) {
		appending.add(root);
	}

	@Override
	public String value() {
		return null;
	}

	public Term<String> canonicalize() {
		return root.canonicalize();
	}

	public StringJunction negate() {
		if (root instanceof Not)
			root = ((Not) root).term;
		else {
			root = Not.of(root);
		}
		return this;
	}

	@Override
	public final boolean equals(Object o) {
		if (!(o instanceof StringJunction)) return Objects.equals(o, root);

		StringJunction junction = (StringJunction) o;
		return root.equals(junction.root);
	}

	@Override
	public int hashCode() {
		return root.hashCode();
	}

	@Override
	public boolean match(Collection<? extends String> coll) {
		return root.match(coll);
	}

	@Override
	public Collection<String> remove(Collection<String> coll) {
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
	public <S extends CharSequence & Appendable> S appendTo(S sb, Appender<String> appender) {
		return root.appendTo(sb, appender);
	}

	public <S extends CharSequence & Appendable> S appendTo(S sb) {
		return root.appendTo(sb, Appender.STRING_APPENDER);
	}

	@Override
	public int compareTo(Term<String> o) {
		return root.compareTo(o);
	}

	public static StringJunction of(String item) {
		return new StringJunction(Void.TYPE, item);
	}

	/**
	 * Simply matches a single String value, with no additional Terms involved.
	 */
	public static class Leaf implements Term<String> {
		public String item;

		public Leaf() {
		}

		public Leaf(String item) {
			this.item = item;
		}

		@Override
		public boolean match(Collection<? extends String> coll) {
			return coll.contains(item);
		}

		@Override
		public Collection<String> remove(Collection<String> coll) {
			coll.remove(item);
			return coll;
		}

		@Override
		public void appendChildren(Collection<Term<String>> appending) {
		}

		@Override
		public String value() {
			return item;
		}

		@Override
		public Term<String> canonicalize() {
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
			return item;
		}

		@Override
		public <S extends CharSequence & Appendable> S appendTo(S sb, Appender<String> appender) {
			return appender.apply(sb, item);
		}

		public <S extends CharSequence & Appendable> S appendTo(S sb) {
			return Appender.STRING_APPENDER.apply(sb, item);
		}

		@Override
		public final boolean equals(Object o) {
			if (!(o instanceof StringJunction.Leaf)) return Objects.equals(o, item);

			Leaf leaf = (Leaf) o;
			return Objects.equals(item, leaf.item);
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(item);
		}

		@Override
		public int compareTo(Term<String> o) {
			return o instanceof Leaf ? item.compareTo(((Leaf) o).item) : (symbol() - o.symbol());
		}

		public static Leaf of(String item) {
			return new Leaf(item);
		}
	}

	/**
	 * Takes a Term and treats a case where it matches or doesn't match as the opposite.
	 * This can take up to two Term parameters in its constructor, but it only uses the last one.
	 */
	public static class Not implements Term<String> {
		public Term<String> term;

		public Not() {
		}

		public Not(String item) {
			this.term = Leaf.of(item);
		}

		public Not(Term<String> ignored, Term<String> right) {
			term = right;
		}

		private Not(Class<Void> ignored, Term<String> term) {
			this.term = term;
		}

		@Override
		public boolean match(Collection<? extends String> coll) {
			return !term.match(coll);
		}

		@Override
		public Collection<String> remove(Collection<String> coll) {
			ObjectList<String> list = new ObjectList<>(coll);
			term.remove(list);
			coll.removeAll(list);
			return coll;
		}

		@Override
		public void appendChildren(Collection<Term<String>> appending) {
			appending.add(term);
		}

		@Override
		public String value() {
			return null;
		}

		@Override
		public Term<String> canonicalize() {
			return term instanceof Not ? ((Not) term).term : this;
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
		public <S extends CharSequence & Appendable> S appendTo(S sb, Appender<String> appender) {
			try {
				sb.append('~');
				term.appendTo(sb, appender);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return sb;
		}

		public <S extends CharSequence & Appendable> S appendTo(S sb) {
			try {
				sb.append('~');
				term.appendTo(sb, Appender.STRING_APPENDER);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return sb;
		}

		@Override
		public final boolean equals(Object o) {
			if (!(o instanceof StringJunction.Not)) return !Objects.equals(o, term);

			Not leaf = (Not) o;
			return !Objects.equals(term, leaf.term);
		}

		@Override
		public int hashCode() {
			return ~Objects.hashCode(term);
		}

		@Override
		public int compareTo(Term<String> o) {
			return o instanceof Not ? term.compareTo(((Not) o).term) : (symbol() - o.symbol());
		}

		public static Not of(Term<String> term) {
			return new Not(Void.TYPE, term);
		}
	}

	/**
	 * Takes one or more Terms and matches if any of those Terms match.
	 */
	public static class Any implements Term<String> {
		public final ObjectList<Term<String>> contents;

		public Any() {
			contents = new ObjectList<>(0);
		}

		public Any(String... items) {
			contents = new ObjectList<>(items.length);
			for (int i = 0; i < items.length; i++) {
				contents.add(new Leaf(items[i]));
			}
		}

		public Any(Term<String> left, Term<String> right) {
			contents = ObjectList.with(left, right);
		}

		public Any(Collection<Term<String>> coll) {
			contents = new ObjectList<>(coll);
		}

		/**
		 * Use via {@link #of(Term[])} instead of directly.
		 *
		 * @param ignored {@link Void#TYPE}
		 * @param terms   an array of Terms that will be put into {@link #contents} and sorted
		 */
		private Any(Class<Void> ignored, Term<String>[] terms) {
			contents = new ObjectList<>(terms);
		}

		@Override
		public boolean match(Collection<? extends String> coll) {
			for (int i = 0; i < contents.size(); i++) {
				if (contents.get(i).match(coll)) return true;
			}
			return false;
		}

		@Override
		public Collection<String> remove(Collection<String> coll) {
			for (int i = 0; i < contents.size(); i++) {
				contents.get(i).remove(coll);
			}
			return coll;
		}

		@Override
		public void appendChildren(Collection<Term<String>> appending) {
			appending.addAll(contents);
		}

		@Override
		public String value() {
			return null;
		}

		@Override
		public Term<String> canonicalize() {
			for (int i = 0, n = contents.size(); i < n; i++) {
				Term<String> child = contents.get(i);
				if (child instanceof Any) {
					contents.removeAt(i--);
					contents.addAll(((Any) child).contents);
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
		public <S extends CharSequence & Appendable> S appendTo(S sb, Appender<String> appender) {
			try {
				sb.append('(');
				contents.appendTo(sb, "|", false, Term.termAppender(appender));
				sb.append(')');
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return sb;
		}

		public <S extends CharSequence & Appendable> S appendTo(S sb) {
			try {
				sb.append('(');
				contents.appendTo(sb, "|", false, Term.termOfStringAppender);
				sb.append(')');
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return sb;
		}

		@Override
		public final boolean equals(Object o) {
			if (!(o instanceof Any)) return false;

			Any any = (Any) o;
			return contents.equals(any.contents);
		}

		@Override
		public int hashCode() {
			return contents.hashCode();
		}

		@Override
		public int compareTo(Term<String> o) {
			if (o instanceof Any) {
				Any a = (Any) o;
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
		public static Any of(Term<String>... terms) {
			return new Any(Void.TYPE, terms);
		}
	}

	/**
	 * Takes one or more Terms and matches if all of those Terms match.
	 */
	public static class All implements Term<String> {
		public final ObjectList<Term<String>> contents;

		public All() {
			contents = new ObjectList<>(0);
		}

		public All(String... items) {
			contents = new ObjectList<>(items.length);
			for (int i = 0; i < items.length; i++) {
				contents.add(new Leaf(items[i]));
			}
		}

		public All(Term<String> left, Term<String> right) {
			contents = ObjectList.with(left, right);
		}

		public All(Collection<Term<String>> coll) {
			contents = new ObjectList<>(coll);
		}

		/**
		 * Use via {@link #of(Term[])} instead of directly.
		 *
		 * @param ignored {@link Void#TYPE}
		 * @param terms   an array of Terms that will be put into {@link #contents} and sorted
		 */
		private All(Class<Void> ignored, Term<String>[] terms) {
			contents = new ObjectList<>(terms);
		}

		@Override
		public boolean match(Collection<? extends String> coll) {
			for (int i = 0; i < contents.size(); i++) {
				if (!contents.get(i).match(coll)) return false;
			}
			return true;
		}

		@Override
		public Collection<String> remove(Collection<String> coll) {
			for (int i = 0; i < contents.size(); i++) {
				if (!contents.get(i).match(coll)) return coll;
			}
			for (int i = 0; i < contents.size(); i++) {
				contents.get(i).remove(coll);
			}
			return coll;
		}

		@Override
		public void appendChildren(Collection<Term<String>> appending) {
			appending.addAll(contents);
		}

		@Override
		public String value() {
			return null;
		}

		@Override
		public Term<String> canonicalize() {
			for (int i = 0, n = contents.size(); i < n; i++) {
				Term<String> child = contents.get(i);
				if (child instanceof All) {
					contents.removeAt(i--);
					contents.addAll(((All) child).contents);
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
		public <S extends CharSequence & Appendable> S appendTo(S sb, Appender<String> appender) {
			try {
				sb.append('(');
				contents.appendTo(sb, "&", false, Term.termAppender(appender));
				sb.append(')');
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return sb;
		}

		public <S extends CharSequence & Appendable> S appendTo(S sb) {
			try {
				sb.append('(');
				contents.appendTo(sb, "&", false, Term.termOfStringAppender);
				sb.append(')');
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return sb;
		}

		@Override
		public final boolean equals(Object o) {
			if (!(o instanceof All)) return false;

			All any = (All) o;
			return contents.equals(any.contents);
		}

		@Override
		public int hashCode() {
			return contents.hashCode();
		}

		@Override
		public int compareTo(Term<String> o) {
			if (o instanceof All) {
				All a = (All) o;
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
		public static All of(Term<String>... terms) {
			return new All(Void.TYPE, terms);
		}
	}

	/**
	 * Takes one or more Terms and matches if exactly one of those Terms matches.
	 */
	public static class One implements Term<String> {
		public final ObjectList<Term<String>> contents;

		public One() {
			contents = new ObjectList<>(0);
		}

		public One(String... items) {
			contents = new ObjectList<>(items.length);
			for (int i = 0; i < items.length; i++) {
				contents.add(new Leaf(items[i]));
			}
		}

		public One(Term<String> left, Term<String> right) {
			contents = ObjectList.with(left, right);
		}

		public One(Collection<Term<String>> coll) {
			contents = new ObjectList<>(coll);
		}

		/**
		 * Use via {@link #of(Term[])} instead of directly.
		 *
		 * @param ignored {@link Void#TYPE}
		 * @param terms   an array of Terms that will be put into {@link #contents} and sorted
		 */
		private One(Class<Void> ignored, Term<String>[] terms) {
			contents = new ObjectList<>(terms);
		}

		@Override
		public boolean match(Collection<? extends String> coll) {
			int count = 0;
			for (int i = 0; i < contents.size() && count <= 1; i++) {
				if (contents.get(i).match(coll)) count++;
			}
			return count == 1;
		}

		@Override
		public Collection<String> remove(Collection<String> coll) {
			for (int i = 0; i < contents.size(); i++) {
				if (contents.get(i).match(coll)) return contents.get(i).remove(coll);
			}
			return coll;
		}

		@Override
		public void appendChildren(Collection<Term<String>> appending) {
			appending.addAll(contents);
		}

		@Override
		public String value() {
			return null;
		}

		@Override
		public Term<String> canonicalize() {
			for (int i = 0, n = contents.size(); i < n; i++) {
				Term<String> child = contents.get(i);
				if (child instanceof One) {
					contents.removeAt(i--);
					contents.addAll(((One) child).contents);
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
		public <S extends CharSequence & Appendable> S appendTo(S sb, Appender<String> appender) {
			try {
				sb.append('(');
				contents.appendTo(sb, "^", false, Term.termAppender(appender));
				sb.append(')');
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return sb;
		}

		public <S extends CharSequence & Appendable> S appendTo(S sb) {
			try {
				sb.append('(');
				contents.appendTo(sb, "^", false, Term.termOfStringAppender);
				sb.append(')');
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return sb;
		}

		@Override
		public final boolean equals(Object o) {
			if (!(o instanceof One)) return false;

			One any = (One) o;
			return contents.equals(any.contents);
		}

		@Override
		public int hashCode() {
			return contents.hashCode();
		}

		@Override
		public int compareTo(Term<String> o) {
			if (o instanceof One) {
				One a = (One) o;
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
		public static One of(Term<String>... terms) {
			return new One(Void.TYPE, terms);
		}
	}

	/**
	 * Parses the String {@code text} into one StringJunction.
	 *
	 * @param text the String to parse
	 * @return the resulting StringJunction
	 */
	public static StringJunction parse(String text) {
		return parse(text, 0, text.length());
	}

	/**
	 * Parses a substring of {@code text} into one StringJunction. The {@code start} is inclusive and
	 * the {@code end} is exclusive.
	 *
	 * @param text  the String to parse
	 * @param start the first index to read from, inclusive
	 * @param end   the last index to stop reading before, exclusive
	 * @return the resulting StringJunction
	 */
	public static StringJunction parse(String text, int start, int end) {
		ObjectDeque<String> tokens = Junction.lex(text, start, end);
		tokens = Junction.shuntingYard(tokens);
		ObjectDeque<Term<String>> terms = new ObjectDeque<>(tokens.size());
		for (String tok : tokens) {
			if (Junction.OPERATORS.containsKey(tok)) {
				Term<String> right = terms.removeLast(), left = terms.removeLast();
				switch (tok.charAt(0)) {
					case '~':
						terms.addLast(Not.of(right));
						break;
					case '^':
						terms.addLast(new One(left, right));
						break;
					case '&':
						terms.addLast(new All(left, right));
						break;
					case '|':
						terms.addLast(new Any(left, right));
						break;
				}
			} else {
				terms.addLast(Leaf.of(tok));
			}
		}
		if (terms.isEmpty()) terms.addLast(Any.of());
		return new StringJunction(terms.getLast());
	}
}

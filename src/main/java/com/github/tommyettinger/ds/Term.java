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

import java.util.Collection;

/**
 * Mostly internal; describes part of a {@link Junction}.
 *
 * @param <T> the type being joined in the Junction; must be Comparable
 */
public interface Term<T extends Comparable<T>> extends Comparable<Term<T>> {
	/**
	 * A predicate that checks if the given Collection of T satisfies this Term.
	 * Returns true if this Term matches the given Collection, or false otherwise.
	 *
	 * @param coll a Collection of T that will not be modified
	 * @return true if coll matches, or false otherwise
	 */
	boolean match(Collection<? extends T> coll);

	/**
	 * Modifies the given Collection of T by removing any items that match this Term.
	 * You can use {@link Junction#negate()} on an outer Junction to flip this to perform the converse
	 * operation to removing, filtering.
	 *
	 * @param coll a Collection of T that may be modified
	 * @return usually coll, after modifications
	 */
	Collection<T> remove(Collection<T> coll);

	/**
	 * If this Term has sub-Terms, which this calls children, calling appendChildren will take all children
	 * one level descendant from this and place them into {@code appending}, in undefined order.
	 * Typically, after appendChildren() has been called at least once and doesn't need to append more, calling
	 * code will sort {@code appending}.
	 *
	 * @param appending will be modified by appending child Terms
	 */
	void appendChildren(Collection<Term<T>> appending);

	/**
	 * If this term has a T value (not inside another wrapping Term), this returns that value.
	 * Otherwise, this returns null.
	 *
	 * @return a T value not inside another wrapping Term, or null if this Term doesn't have a T value.
	 */
	T value();

	/**
	 * Attempts to convert this Term and its children (recursively) to a single possible format for potentially
	 * many different internal representations. This mostly means things like {@code Not(Not(Leaf("something")))}
	 * can be simplified to {@code Leaf("something")}, and chains of Any of Any of Any of... can be simplified to
	 * one Any with more items. The last case also works for All, but not One.
	 *
	 * @return a unified formatting of the data this held, modifying this Term in place.
	 */
	Term<T> canonicalize();

	/**
	 * Gets a single char constant that represents this Term and determines its comparison order in the
	 * event of a tie. Every Term class should return a different char from this method.
	 *
	 * @return a char that represents this Term and is used to break ties in sorting.
	 */
	char symbol();

	/**
	 * Gets a plain-English name, typically all lower-case and one word, that describes what operation this
	 * Term performs.
	 *
	 * @return a typically lower-case single-word name describing what this Term does
	 */
	String name();

	/**
	 * Slightly different from the normal toString() behavior, this may incorporate {@link #name()} but doesn't
	 * need to, and if it contains multiple parts, they should be separated by {@link #symbol()}.
	 *
	 * @return a String representation of this Term, or sometimes only its contents
	 */
	String toString();

	/**
	 * Appends a representation of this Term to an Appendable CharSequence, using {@code appender} to get textual forms
	 * from T items.
	 * <br>
	 * If this is a Term of String, you can use {@link Appender#STRING_APPENDER} as the second parameter.
	 *
	 * @param sb        an Appendable CharSequence that this can append to
	 * @param appender  a function that takes an Appendable CharSequence and a T, and returns the modified {@code S}
	 * @return {@code sb}, with the appended representation of this Term
	 * @param <S>  any type that is both a CharSequence and an Appendable, such as StringBuilder, StringBuffer, CharBuffer, or CharList
	 */
	<S extends CharSequence & Appendable> S appendTo(S sb, Appender<T> appender);

	/**
	 * Generates an Appender that can append Term of T items to an Appendable CharSequence.
	 *
	 * @param appender an Appender that can append {@code T} items (not Term of T; that's what this method does)
	 * @return a new Appender of Term of T
	 * @param <T> the generic type that each Term carries, which is always Comparable (and is often String)
	 */
	static <T extends Comparable<T>> Appender<Term<T>> termAppender(final Appender<T> appender) {
		return new Appender<Term<T>>() {
			@Override
			public <S extends CharSequence & Appendable> S apply(S sb, Term<T> item) {
				return item.appendTo(sb, appender);
			}
		};
	}

	/**
	 * An existing Appender for Terms of String, a common usage.
	 */
	Appender<Term<String>> termOfStringAppender = new Appender<Term<String>>() {
		@Override
		public <S extends CharSequence & Appendable> S apply(S sb, Term<String> item) {
			return item.appendTo(sb, Appender.STRING_APPENDER);
		}
	};

	/**
	 * Used primarily to check for equality between Terms, not to act like {@link #match(Collection)}.
	 *
	 * @param o another Object that must be a Term of the same class for this to be able to return true
	 * @return true if and only if o is a Term of the same class, and this Term is equivalent to o
	 */
	boolean equals(Object o);
}

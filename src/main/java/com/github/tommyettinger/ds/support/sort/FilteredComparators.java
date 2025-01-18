/*
 * Copyright (c) 2022-2025 See AUTHORS file.
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

package com.github.tommyettinger.ds.support.sort;

import com.github.tommyettinger.ds.CharFilter;
import com.github.tommyettinger.function.CharPredicate;
import com.github.tommyettinger.function.CharToCharFunction;
import com.github.tommyettinger.function.ObjPredicate;
import com.github.tommyettinger.function.ObjToSameFunction;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Objects;

/**
 * Produces {@link Comparator}s that can sort Strings and Collections while filtering out chars/items that don't match a predicate,
 * and/or altering chars/items when they are compared. Meant to be used with
 * {@link com.github.tommyettinger.ds.FilteredStringOrderedSet} and {@link com.github.tommyettinger.ds.FilteredStringOrderedMap}.
 */
public class FilteredComparators {

	private FilteredComparators () {

	}

	public static Comparator<String> makeStringComparator(final CharFilter filter) {
		return makeStringComparator(filter.filter, filter.editor);
	}

	public static Comparator<String> makeStringComparator(final CharComparator baseComparator, final CharFilter filter) {
		return makeStringComparator(baseComparator, filter.filter, filter.editor);
	}

	public static Comparator<String> makeStringComparator(final CharPredicate filter, final CharToCharFunction editor) {
		return (String l, String r) -> {
			int llen = l.length(), rlen = r.length(), countL = llen, countR = rlen;
			int cl = -1, cr = -1;
			int i = 0, j = 0;
			char el, er;
			while (i < llen || j < rlen) {
				if (i == llen) {
					cl = -1;
				}
				else {
					while (i < llen && !filter.test((char)(cl = l.charAt(i++)))) {
						cl = -1;
						countL--;
					}
				}
				if (j == rlen) {
					cr = -1;
				}
				else {
					while (j < rlen && !filter.test((char)(cr = r.charAt(j++)))) {
						cr = -1;
						countR--;
					}
				}
				if (cl != cr && (el = editor.applyAsChar((char)cl)) != (er = editor.applyAsChar((char)cr)))
					return (el - er) * ((cl ^ cr) >> 31 | 1);
			}
			return countL - countR;
		};
	}

	/**
	 * Like {@link #makeStringComparator(CharPredicate, CharToCharFunction)}, but takes a base comparator that compare char items
	 * in a non-ascending order if needed. Another option could be to call the other makeStringComparator() and reverse the
	 * Comparator it returns.
	 * @param baseComparator a CharComparator that will be used to compare individual chars, not Strings
	 * @param filter          a CharPredicate that should return true iff a character should be considered for equality/hashing
	 * @param editor          a CharToCharFunction that will take a char from a key String and return a potentially different char
	 * @return a Comparator for Strings that will respect the given filter, editor, and base comparator
	 */
	public static Comparator<String> makeStringComparator(final CharComparator baseComparator, final CharPredicate filter, final CharToCharFunction editor) {
		return (String l, String r) -> {
			int llen = l.length(), rlen = r.length(), countL = llen, countR = rlen;
			int cl = -1, cr = -1;
			int i = 0, j = 0;
			char el, er;
			while (i < llen || j < rlen) {
				if (i == llen) {
					cl = -1;
				}
				else {
					while (i < llen && !filter.test((char)(cl = l.charAt(i++)))) {
						cl = -1;
						countL--;
					}
				}
				if (j == rlen) {
					cr = -1;
				}
				else {
					while (j < rlen && !filter.test((char)(cr = r.charAt(j++)))) {
						cr = -1;
						countR--;
					}
				}
				if (cl != cr && (el = editor.applyAsChar((char)cl)) != (er = editor.applyAsChar((char)cr)))
					return baseComparator.compare(el, er) * ((cl ^ cr) >> 31 | 1);
			}
			return countL - countR;
		};
	}

	/**
	 * Creates a new Comparator that will respect a filter and an editor; it will be able to compare
	 * any {@link Iterable} of T with any other Iterable of T.  The filter is an {@link ObjPredicate} of T items
	 * (not Iterable of T items!); the comparison of two Iterable of T will only consider T items for which
	 * the filter returns true. When two T items need to be compared for equality, they will each have the
	 * editor applied to that item during the comparison only; the editor is an {@link ObjToSameFunction} of T,
	 * so it takes a T item and returns a T item.
	 * <br>
	 * This overload requires T to be {@link Comparable} to other T items, and does not need another Comparator.
	 * @param filter this will be called on each T item in consideration; only items where this returns true will be used
	 * @param editor this will be called on each T item that needs to be compared and can return a different T item to actually compare
	 * @return a Comparator of Iterable of T, which will use the given filter and editor
	 * @param <T> the type of items in each Iterable; must be {@link Comparable} to other T items
	 */
	public static <T extends Comparable<T>, I extends Iterable<T>> Comparator<I> makeComparator(final ObjPredicate<T> filter,
		final ObjToSameFunction<T> editor) {
		return makeComparator(T::compareTo, filter, editor);
	}

	/**
	 * Creates a new Comparator that will respect a filter and an editor; it will be able to compare
	 * any {@link Iterable} of T with any other Iterable of T.  The filter is an {@link ObjPredicate} of T items
	 * (not Iterable of T items!); the comparison of two Iterable of T will only consider T items for which
	 * the filter returns true. When two T items need to be compared for equality, they will each have the
	 * editor applied to that item during the comparison only; the editor is an {@link ObjToSameFunction} of T,
	 * so it takes a T item and returns a T item.
	 * <br>
	 * This overload allows specifying a
	 * {@code baseComparator} that can compare T items; this is needed when T is not {@link Comparable}
	 * or when the order needs to be non-standard even after items are filtered and edited (such as if
	 * you need reversed sort order, or need to sort based on an item or field in each T).
	 * @param baseComparator used to compare T items after the filter and editor have been applied
	 * @param filter this will be called on each T item in consideration; only items where this returns true will be used
	 * @param editor this will be called on each T item that needs to be compared and can return a different T item to actually compare
	 * @return a Comparator of Iterable of T, which will use the given filter and editor
	 * @param <T> the type of items in each Iterable
	 */
	public static <T, I extends Iterable<T>> Comparator<I> makeComparator(final @NonNull Comparator<T> baseComparator,
		final ObjPredicate<T> filter, final ObjToSameFunction<T> editor) {
		return (I l, I r) -> {
			int countL = 0, countR = 0;
			Iterator<? extends T> i = l.iterator(), j = r.iterator();
			T cl = null, cr = null;
			T el, er;
			while (i.hasNext() || j.hasNext()) {
				if (!i.hasNext()) {
					cl = null;
				} else {
					boolean found = false;
					while (i.hasNext() && !(found = filter.test(cl = i.next()))) {
						cl = null;
					}
					if(found) countL++;
				}
				if (!j.hasNext()) {
					cr = null;
				} else {
					boolean found = false;
					while (j.hasNext() && !(found = filter.test(cr = j.next()))) {
						cr = null;
					}
					if(found) countR++;
				}
				if (!Objects.equals(cl, cr) && !Objects.equals((el = editor.apply(cl)), (er = editor.apply(cr)))) {
					return cl == null || cr == null ? baseComparator.compare(er, el) : baseComparator.compare(el, er);
				}
			}
			return countL - countR;
		};
	}
}

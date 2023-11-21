/*
 * Copyright (c) 2022-2023 See AUTHORS file.
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
 *
 */

package com.github.tommyettinger.ds.support.sort;

import com.github.tommyettinger.function.CharPredicate;
import com.github.tommyettinger.function.CharToCharFunction;
import com.github.tommyettinger.function.ObjPredicate;
import com.github.tommyettinger.function.ObjToSameFunction;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;

/**
 * Produces {@link Comparator}s that can sort Strings and Collections while filtering out chars/items that don't match a predicate,
 * and/or altering chars/items when they are compared. Meant to be used with
 * {@link com.github.tommyettinger.ds.FilteredStringOrderedSet} and {@link com.github.tommyettinger.ds.FilteredStringOrderedMap}.
 */
public class FilteredComparators {

	private FilteredComparators () {

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

	public static <C  extends Collection<T>, T extends Comparable<T>> Comparator<C> makeComparator(final ObjPredicate<T> filter, final ObjToSameFunction<T> editor) {
		return makeComparator(null, filter, editor);
	}

	public static <C extends Collection<T>, T> Comparator<C> makeComparator(final Comparator<T> baseComparator,
		final ObjPredicate<T> filter, final ObjToSameFunction<T> editor) {
		return (C l, C r) -> {
			int llen = l.size(), rlen = r.size(), countL = llen, countR = rlen;
			T cl = null, cr = null;
			Iterator<? extends T> i = l.iterator(), j = r.iterator();
			T el, er;
			while (i.hasNext() || j.hasNext()) {
				if (!i.hasNext()) {
					cl = null;
				} else {
					while (i.hasNext() && !filter.test(cl = i.next())) {
						cl = null;
						countL--;
					}
				}
				if (!j.hasNext()) {
					cr = null;
				} else {
					while (j.hasNext() && !filter.test(cr = j.next())) {
						cr = null;
						countR--;
					}
				}
				if (cl != cr && (el = editor.apply(cl)) != (er = editor.apply(cr))) {
					if (baseComparator == null) {
						if (er instanceof Comparable && el instanceof Comparable) {
							if (cl == null || cr == null)
								return ((Comparable)er).compareTo(el);
							else
								return ((Comparable)el).compareTo(er);
						}
						throw new UnsupportedOperationException("Items must implement Comparable.");
					}
					return cl == null || cr == null ? baseComparator.compare(er, el) : baseComparator.compare(el, er);
				}
			}
			return countL - countR;
		};
	}
}

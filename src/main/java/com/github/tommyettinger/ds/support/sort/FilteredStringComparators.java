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

import java.util.Comparator;

/**
 * Produces {@link Comparator}s that can sort Strings while filtering out chars that don't match a predicate, and/or
 * altering characters using a CharToCharFunction when they are compared. Meant to be used with
 * {@link com.github.tommyettinger.ds.FilteredStringOrderedSet} and {@link com.github.tommyettinger.ds.FilteredStringOrderedMap}.
 */
public class FilteredStringComparators {

	private FilteredStringComparators () {

	}

	public static Comparator<String> makeComparator(final CharPredicate filter, final CharToCharFunction editor) {
		return (String l, String r) -> {
			int llen = l.length(), rlen = r.length(), countL = llen, countR = rlen;
			int cl = -1, cr = -1;
			int i = 0, j = 0;
			char el, er;
			while (i < llen || j < rlen) {
				if (i == llen) {
					cl = -1;
					countL--;
				}
				else {
					while (i < llen && !filter.test((char)(cl = l.charAt(i++)))) {
						cl = -1;
						countL--;
					}
				}
				if (j == rlen) {
					cr = -1;
					countR--;
				}
				else {
					while (j < rlen && !filter.test((char)(cr = r.charAt(j++)))) {
						cr = -1;
						countR--;
					}
				}
				if (cl != cr && (el = editor.applyAsChar((char)cl)) != (er = editor.applyAsChar((char)cr)))
					return el - er;
			}
			return countL - countR;
		};
	}
}

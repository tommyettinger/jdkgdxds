/*
 * Copyright (C) 2003-2020 Paolo Boldi and Sebastiano Vigna
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.tommyettinger.ds.support.sort;

import java.util.Comparator;

/**
 * A class providing static methods and objects that do useful things with
 * comparators.
 */
public final class CharComparators {
	private CharComparators () {
	}

	/**
	 * A type-specific comparator mimicking the natural order.
	 */
	protected static class NaturalImplicitComparator implements CharComparator {


		@Override
		public final int compare (final char a, final char b) {
			return Integer.compare(a, b);
		}

		@Override
		public CharComparator reversed () {
			return OPPOSITE_COMPARATOR;
		}
	}

	public static final CharComparator NATURAL_COMPARATOR = new NaturalImplicitComparator();

	/**
	 * A type-specific comparator mimicking the opposite of the natural order.
	 */
	protected static class OppositeImplicitComparator implements CharComparator {


		@Override
		public final int compare (final char a, final char b) {
			return Integer.compare(b, a);
		}

		@Override
		public CharComparator reversed () {
			return NATURAL_COMPARATOR;
		}
	}

	public static final CharComparator OPPOSITE_COMPARATOR = new OppositeImplicitComparator();

	protected static class OppositeComparator implements CharComparator {

		final CharComparator comparator;

		protected OppositeComparator (final CharComparator c) {
			comparator = c;
		}

		@Override
		public final int compare (final char a, final char b) {
			return comparator.compare(b, a);
		}

		@Override
		public final CharComparator reversed () {
			return comparator;
		}
	}

	/**
	 * Returns a comparator representing the opposite order of the given comparator.
	 *
	 * @param c a comparator.
	 * @return a comparator representing the opposite order of {@code c}.
	 */
	public static CharComparator oppositeComparator (final CharComparator c) {
		if (c instanceof OppositeComparator) { return ((OppositeComparator)c).comparator; }
		return new OppositeComparator(c);
	}

	/**
	 * Returns a type-specific comparator that is equivalent to the given
	 * comparator.
	 *
	 * @param c a Comparator of Char values.
	 * @return a type-specific comparator representing the order of {@code c}.
	 */
	public static CharComparator asCharComparator (final Comparator<? super Character> c) {
		if (c instanceof CharComparator) { return (CharComparator)c; }
		return new CharComparator() {
			@Override
			public int compare (char x, char y) {
				return c.compare(Character.valueOf(x), Character.valueOf(y));
			}

			@SuppressWarnings("deprecation")
			@Override
			public int compare (Character x, Character y) {
				return c.compare(x, y);
			}
		};
	}
}

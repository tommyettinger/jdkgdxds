/*
 * Copyright (c) 2022 See AUTHORS file.
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

import java.util.Comparator;

/**
 * A class providing static methods and objects that do useful things with
 * comparators.
 */
public final class BooleanComparators {
	private BooleanComparators () {
	}

	/**
	 * A type-specific comparator mimicking the natural order (false then true)
	 */
	protected static class NaturalImplicitComparator implements BooleanComparator {

		@Override
		public final int compare (final boolean a, final boolean b) {
			return Boolean.compare(a, b);
		}

		@Override
		public BooleanComparator reversed () {
			return OPPOSITE_COMPARATOR;
		}
	}

	public static final BooleanComparator NATURAL_COMPARATOR = new NaturalImplicitComparator();

	/**
	 * A type-specific comparator mimicking the opposite of the natural order (the opposite being true then false).
	 */
	protected static class OppositeImplicitComparator implements BooleanComparator {

		@Override
		public final int compare (final boolean a, final boolean b) {
			return Boolean.compare(b, a);
		}

		@Override
		public BooleanComparator reversed () {
			return NATURAL_COMPARATOR;
		}
	}

	public static final BooleanComparator OPPOSITE_COMPARATOR = new OppositeImplicitComparator();

	protected static class OppositeComparator implements BooleanComparator {

		final BooleanComparator comparator;

		protected OppositeComparator (final BooleanComparator c) {
			comparator = c;
		}

		@Override
		public final int compare (final boolean a, final boolean b) {
			return comparator.compare(b, a);
		}

		@Override
		public final BooleanComparator reversed () {
			return comparator;
		}
	}

	/**
	 * Returns a comparator representing the opposite order of the given comparator.
	 *
	 * @param c a comparator.
	 * @return a comparator representing the opposite order of {@code c}.
	 */
	public static BooleanComparator oppositeComparator (final BooleanComparator c) {
		if (c instanceof OppositeComparator) {return ((OppositeComparator)c).comparator;}
		return new OppositeComparator(c);
	}

	/**
	 * Returns a type-specific comparator that is equivalent to the given
	 * comparator.
	 *
	 * @param c a Comparator of Boolean values.
	 * @return a type-specific comparator representing the order of {@code c}.
	 */
	public static BooleanComparator asBooleanComparator (final Comparator<? super Boolean> c) {
		if (c instanceof BooleanComparator) {return (BooleanComparator)c;}
		return new BooleanComparator() {
			@Override
			public int compare (boolean x, boolean y) {
				return c.compare(Boolean.valueOf(x), Boolean.valueOf(y));
			}

			@SuppressWarnings("deprecation")
			@Override
			public int compare (Boolean x, Boolean y) {
				return c.compare(x, y);
			}
		};
	}
}

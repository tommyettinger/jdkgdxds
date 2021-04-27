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
public final class ShortComparators {
	private ShortComparators () {
	}

	/**
	 * A type-specific comparator mimicking the natural order.
	 */
	protected static class NaturalImplicitComparator implements ShortComparator {


		@Override
		public final int compare (final short a, final short b) {
			return Short.compare(a, b);
		}

		@Override
		public ShortComparator reversed () {
			return OPPOSITE_COMPARATOR;
		}
	}

	public static final ShortComparator NATURAL_COMPARATOR = new NaturalImplicitComparator();

	/**
	 * A type-specific comparator mimicking the opposite of the natural order.
	 */
	protected static class OppositeImplicitComparator implements ShortComparator {


		@Override
		public final int compare (final short a, final short b) {
			return Short.compare(b, a);
		}

		@Override
		public ShortComparator reversed () {
			return NATURAL_COMPARATOR;
		}
	}

	public static final ShortComparator OPPOSITE_COMPARATOR = new OppositeImplicitComparator();

	protected static class OppositeComparator implements ShortComparator {

		final ShortComparator comparator;

		protected OppositeComparator (final ShortComparator c) {
			comparator = c;
		}

		@Override
		public final int compare (final short a, final short b) {
			return comparator.compare(b, a);
		}

		@Override
		public final ShortComparator reversed () {
			return comparator;
		}
	}

	/**
	 * Returns a comparator representing the opposite order of the given comparator.
	 *
	 * @param c a comparator.
	 * @return a comparator representing the opposite order of {@code c}.
	 */
	public static ShortComparator oppositeComparator (final ShortComparator c) {
		if (c instanceof OppositeComparator) { return ((OppositeComparator)c).comparator; }
		return new OppositeComparator(c);
	}

	/**
	 * Returns a type-specific comparator that is equivalent to the given
	 * comparator.
	 *
	 * @param c a Comparator of Short values.
	 * @return a type-specific comparator representing the order of {@code c}.
	 */
	public static ShortComparator asShortComparator (final Comparator<? super Short> c) {
		if (c instanceof ShortComparator) { return (ShortComparator)c; }
		return new ShortComparator() {
			@Override
			public int compare (short x, short y) {
				return c.compare(Short.valueOf(x), Short.valueOf(y));
			}

			@SuppressWarnings("deprecation")
			@Override
			public int compare (Short x, Short y) {
				return c.compare(x, y);
			}
		};
	}

	/**
	 * A type-specific comparator that compares items in the natural order, but as if they are unsigned
	 * (so, all negative items are greater than any non-negative items).
	 */
	protected static class UnsignedComparator implements ShortComparator {


		@Override
		public final int compare (final short a, final short b) {
			return Integer.compare(a & 0xFFFF, b & 0xFFFF);
		}

		@Override
		public ShortComparator reversed () {
			return UNSIGNED_OPPOSITE_COMPARATOR;
		}


	}

	public static final ShortComparator UNSIGNED_COMPARATOR = new ShortComparators.UnsignedComparator();

	/**
	 * A type-specific comparator that compares items in the opposite of the natural order, but as if they
	 * are unsigned.
	 */
	protected static class UnsignedOppositeComparator implements ShortComparator {


		@Override
		public final int compare (final short a, final short b) {
			return Integer.compare(b & 0xFFFF, a & 0xFFFF);
		}

		@Override
		public ShortComparator reversed () {
			return UNSIGNED_COMPARATOR;
		}


	}

	public static final ShortComparator UNSIGNED_OPPOSITE_COMPARATOR = new ShortComparators.UnsignedComparator();
}

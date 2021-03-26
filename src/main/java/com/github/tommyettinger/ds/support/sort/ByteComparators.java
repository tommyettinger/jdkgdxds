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
public final class ByteComparators {
	private ByteComparators () {
	}

	/**
	 * A type-specific comparator mimicking the natural order.
	 */
	protected static class NaturalImplicitComparator implements ByteComparator, java.io.Serializable {
		private static final long serialVersionUID = 1L;

		@Override
		public final int compare (final byte a, final byte b) {
			return Byte.compare(a, b);
		}

		@Override
		public ByteComparator reversed () {
			return OPPOSITE_COMPARATOR;
		}
	}

	public static final ByteComparator NATURAL_COMPARATOR = new NaturalImplicitComparator();

	/**
	 * A type-specific comparator mimicking the opposite of the natural order.
	 */
	protected static class OppositeImplicitComparator implements ByteComparator, java.io.Serializable {
		private static final long serialVersionUID = 1L;

		@Override
		public final int compare (final byte a, final byte b) {
			return Byte.compare(b, a);
		}

		@Override
		public ByteComparator reversed () {
			return NATURAL_COMPARATOR;
		}
	}

	public static final ByteComparator OPPOSITE_COMPARATOR = new OppositeImplicitComparator();

	protected static class OppositeComparator implements ByteComparator, java.io.Serializable {
		private static final long serialVersionUID = 1L;
		final ByteComparator comparator;

		protected OppositeComparator (final ByteComparator c) {
			comparator = c;
		}

		@Override
		public final int compare (final byte a, final byte b) {
			return comparator.compare(b, a);
		}

		@Override
		public final ByteComparator reversed () {
			return comparator;
		}
	}

	/**
	 * Returns a comparator representing the opposite order of the given comparator.
	 *
	 * @param c a comparator.
	 * @return a comparator representing the opposite order of {@code c}.
	 */
	public static ByteComparator oppositeComparator (final ByteComparator c) {
		if (c instanceof OppositeComparator) { return ((OppositeComparator)c).comparator; }
		return new OppositeComparator(c);
	}

	/**
	 * Returns a type-specific comparator that is equivalent to the given
	 * comparator.
	 *
	 * @param c a Comparator of Byte values.
	 * @return a type-specific comparator representing the order of {@code c}.
	 */
	public static ByteComparator asByteComparator (final Comparator<? super Byte> c) {
		if (c instanceof ByteComparator) { return (ByteComparator)c; }
		return new ByteComparator() {
			@Override
			public int compare (byte x, byte y) {
				return c.compare(Byte.valueOf(x), Byte.valueOf(y));
			}

			@SuppressWarnings("deprecation")
			@Override
			public int compare (Byte x, Byte y) {
				return c.compare(x, y);
			}
		};
	}
}

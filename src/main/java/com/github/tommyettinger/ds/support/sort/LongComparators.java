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
public final class LongComparators {
	private LongComparators() {
	}
	/** A type-specific comparator mimicking the natural order. */
	protected static class NaturalImplicitComparator implements LongComparator, java.io.Serializable {
		private static final long serialVersionUID = 1L;
		@Override
		public final int compare(final long a, final long b) {
			return (Long.compare((a), (b)));
		}
		@Override
		public LongComparator reversed() {
			return OPPOSITE_COMPARATOR;
		}
		private Object readResolve() {
			return NATURAL_COMPARATOR;
		}
	}

	public static final LongComparator NATURAL_COMPARATOR = new NaturalImplicitComparator();
	/** A type-specific comparator mimicking the opposite of the natural order. */
	protected static class OppositeImplicitComparator implements LongComparator, java.io.Serializable {
		private static final long serialVersionUID = 1L;
		@Override
		public final int compare(final long a, final long b) {
			return -(Long.compare((a), (b)));
		}
		@Override
		public LongComparator reversed() {
			return NATURAL_COMPARATOR;
		}
		private Object readResolve() {
			return OPPOSITE_COMPARATOR;
		}
	}

	public static final LongComparator OPPOSITE_COMPARATOR = new OppositeImplicitComparator();
	protected static class OppositeComparator implements LongComparator, java.io.Serializable {
		private static final long serialVersionUID = 1L;
		final LongComparator comparator;
		protected OppositeComparator(final LongComparator c) {
			comparator = c;
		}
		@Override
		public final int compare(final long a, final long b) {
			return comparator.compare(b, a);
		}
		@Override
		public final LongComparator reversed() {
			return comparator;
		}
	}

	/**
	 * Returns a comparator representing the opposite order of the given comparator.
	 *
	 * @param c
	 *            a comparator.
	 * @return a comparator representing the opposite order of {@code c}.
	 */
	public static LongComparator oppositeComparator(final LongComparator c) {
		if (c instanceof OppositeComparator)
			return ((OppositeComparator) c).comparator;
		return new OppositeComparator(c);
	}
	/**
	 * Returns a type-specific comparator that is equivalent to the given
	 * comparator.
	 *
	 * @param c
	 *            a comparator, or {@code null}.
	 * @return a type-specific comparator representing the order of {@code c}.
	 */
	public static LongComparator asLongComparator(final Comparator<? super Long> c) {
		if (c instanceof LongComparator)
			return (LongComparator) c;
		return new LongComparator() {
			@Override
			public int compare(long x, long y) {
				return c.compare(Long.valueOf(x), Long.valueOf(y));
			}
			@SuppressWarnings("deprecation")
			@Override
			public int compare(Long x, Long y) {
				return c.compare(x, y);
			}
		};
	}
}

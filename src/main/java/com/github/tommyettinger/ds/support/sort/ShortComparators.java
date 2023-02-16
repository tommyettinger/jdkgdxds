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

import org.checkerframework.checker.nullness.qual.Nullable;
import java.util.Arrays;
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
		if (c instanceof OppositeComparator) {return ((OppositeComparator)c).comparator;}
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
		if (c instanceof ShortComparator) {return (ShortComparator)c;}
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

	/// The remainder of the code is based on FastUtil.

	private static void swap (short[] items, int first, int second) {
		short firstValue = items[first];
		items[first] = items[second];
		items[second] = firstValue;
	}

	/**
	 * Transforms two consecutive sorted ranges into a single sorted range. The initial ranges are
	 * {@code [first..middle)} and {@code [middle..last)}, and the resulting range is
	 * {@code [first..last)}. Elements in the first input range will precede equal elements in
	 * the second.
	 */
	private static void inPlaceMerge (short[] items, final int from, int mid, final int to, final ShortComparator comp) {
		if (from >= mid || mid >= to) {return;}
		if (to - from == 2) {
			if (comp.compare(items[mid], items[from]) < 0) {swap(items, from, mid);}
			return;
		}

		int firstCut;
		int secondCut;

		if (mid - from > to - mid) {
			firstCut = from + (mid - from) / 2;
			secondCut = lowerBound(items, mid, to, firstCut, comp);
		} else {
			secondCut = mid + (to - mid) / 2;
			firstCut = upperBound(items, from, mid, secondCut, comp);
		}

		int first2 = firstCut;
		int middle2 = mid;
		int last2 = secondCut;
		if (middle2 != first2 && middle2 != last2) {
			int first1 = first2;
			int last1 = middle2;
			while (first1 < --last1) {swap(items, first1++, last1);}
			first1 = middle2;
			last1 = last2;
			while (first1 < --last1) {swap(items, first1++, last1);}
			first1 = first2;
			last1 = last2;
			while (first1 < --last1) {swap(items, first1++, last1);}
		}

		mid = firstCut + secondCut - mid;
		inPlaceMerge(items, from, firstCut, mid, comp);
		inPlaceMerge(items, mid, secondCut, to, comp);
	}

	/**
	 * Performs a binary search on an already-sorted range: finds the first position where an
	 * element can be inserted without violating the ordering. Sorting is by a user-supplied
	 * comparison function.
	 *
	 * @param items the short array to be sorted
	 * @param from  the index of the first element (inclusive) to be included in the binary search.
	 * @param to    the index of the last element (exclusive) to be included in the binary search.
	 * @param pos   the position of the element to be searched for.
	 * @param comp  the comparison function.
	 * @return the largest index i such that, for every j in the range {@code [first..i)},
	 * {@code comp.compare(get(j), get(pos))} is {@code true}.
	 */
	private static int lowerBound (short[] items, int from, final int to, final int pos, final ShortComparator comp) {
		int len = to - from;
		while (len > 0) {
			int half = len / 2;
			int middle = from + half;
			if (comp.compare(items[middle], items[pos]) < 0) {
				from = middle + 1;
				len -= half + 1;
			} else {
				len = half;
			}
		}
		return from;
	}

	/**
	 * Performs a binary search on an already sorted range: finds the last position where an element
	 * can be inserted without violating the ordering. Sorting is by a user-supplied comparison
	 * function.
	 *
	 * @param items the short array to be sorted
	 * @param from  the index of the first element (inclusive) to be included in the binary search.
	 * @param to    the index of the last element (exclusive) to be included in the binary search.
	 * @param pos   the position of the element to be searched for.
	 * @param comp  the comparison function.
	 * @return The largest index i such that, for every j in the range {@code [first..i)},
	 * {@code comp.compare(get(pos), get(j))} is {@code false}.
	 */
	private static int upperBound (short[] items, int from, final int to, final int pos, final ShortComparator comp) {
		int len = to - from;
		while (len > 0) {
			int half = len / 2;
			int middle = from + half;
			if (comp.compare(items[pos], items[middle]) < 0) {
				len = half;
			} else {
				from = middle + 1;
				len -= half + 1;
			}
		}
		return from;
	}

	/**
	 * Sorts all of {@code items} by simply calling {@link #sort(short[], int, int, ShortComparator)},
	 * setting {@code from} and {@code to} so the whole array is sorted.
	 *
	 * @param items the short array to be sorted
	 * @param c     a ShortComparator to alter the sort order; if null, the natural order will be used
	 */
	public static void sort (short[] items, final @Nullable ShortComparator c) {
		sort(items, 0, items.length, c);
	}

	/**
	 * Sorts the specified range of elements according to the order induced by the specified
	 * comparator using mergesort.
	 *
	 * <p>This sort is guaranteed to be <i>stable</i>: equal elements will not be reordered as a result
	 * of the sort. The sorting algorithm is an in-place mergesort that is significantly slower than a
	 * standard mergesort, as its running time is <i>O</i>(<var>n</var>&nbsp;(log&nbsp;<var>n</var>)<sup>2</sup>),
	 * but it does not allocate additional memory; as a result, it can be
	 * used as a generic sorting algorithm.
	 *
	 * <p>If and only if {@code c} is null, this will delegate to {@link Arrays#sort(short[], int, int)}, which
	 * does not have the same guarantees regarding allocation.
	 *
	 * @param items the short array to be sorted
	 * @param from  the index of the first element (inclusive) to be sorted.
	 * @param to    the index of the last element (exclusive) to be sorted.
	 * @param c     a ShortComparator to alter the sort order; if null, the natural order will be used
	 */
	public static void sort (short[] items, final int from, final int to, final @Nullable ShortComparator c) {
		if (to <= 0) {
			return;
		}
		if (from < 0 || from >= items.length || to > items.length) {
			throw new UnsupportedOperationException("The given from/to range in ShortComparators.sort() is invalid.");
		}
		if (c == null) {
			Arrays.sort(items, from, to);
			return;
		}
		/*
		 * We retain the same method signature as quickSort. Given only a comparator and this list
		 * do not know how to copy and move elements from/to temporary arrays. Hence, in contrast to
		 * the JDK mergesorts this is an "in-place" mergesort, i.e. does not allocate any temporary
		 * arrays. A non-inplace mergesort would perhaps be faster in most cases, but would require
		 * non-intuitive delegate objects...
		 */
		final int length = to - from;

		// Insertion sort on smallest arrays, less than 16 items
		if (length < 16) {
			for (int i = from; i < to; i++) {
				for (int j = i; j > from && c.compare(items[j - 1], items[j]) > 0; j--) {
					swap(items, j, j - 1);
				}
			}
			return;
		}

		// Recursively sort halves
		int mid = from + to >>> 1;
		sort(items, from, mid, c);
		sort(items, mid, to, c);

		// If list is already sorted, nothing left to do. This is an
		// optimization that results in faster sorts for nearly ordered lists.
		if (c.compare(items[mid - 1], items[mid]) <= 0) {return;}

		// Merge sorted halves
		inPlaceMerge(items, from, mid, to, c);
	}
}

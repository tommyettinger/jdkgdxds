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

import org.checkerframework.checker.nullness.qual.Nullable;

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


	/// The remainder of the code is based on FastUtil.

	private static void swap (boolean[] items, int first, int second) {
		boolean firstValue = items[first];
		items[first] = items[second];
		items[second] = firstValue;
	}

	/**
	 * Transforms two consecutive sorted ranges into a single sorted range. The initial ranges are
	 * {@code [first..middle)} and {@code [middle..last)}, and the resulting range is
	 * {@code [first..last)}. Elements in the first input range will precede equal elements in
	 * the second.
	 */
	private static void inPlaceMerge (boolean[] items, final int from, int mid, final int to, final BooleanComparator comp) {
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
	 * @param items the boolean array to be sorted
	 * @param from  the index of the first element (inclusive) to be included in the binary search.
	 * @param to    the index of the last element (exclusive) to be included in the binary search.
	 * @param pos   the position of the element to be searched for.
	 * @param comp  the comparison function.
	 * @return the largest index i such that, for every j in the range {@code [first..i)},
	 * {@code comp.compare(get(j), get(pos))} is {@code true}.
	 */
	private static int lowerBound (boolean[] items, int from, final int to, final int pos, final BooleanComparator comp) {
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
	 * @param items the boolean array to be sorted
	 * @param from  the index of the first element (inclusive) to be included in the binary search.
	 * @param to    the index of the last element (exclusive) to be included in the binary search.
	 * @param pos   the position of the element to be searched for.
	 * @param comp  the comparison function.
	 * @return The largest index i such that, for every j in the range {@code [first..i)},
	 * {@code comp.compare(get(pos), get(j))} is {@code false}.
	 */
	private static int upperBound (boolean[] items, int from, final int to, final int pos, final BooleanComparator comp) {
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
	 * Sorts all of {@code items} by simply calling {@link #sort(boolean[], int, int, BooleanComparator)},
	 * setting {@code from} and {@code to} so the whole array is sorted.
	 *
	 * @param items the boolean array to be sorted
	 * @param c     a BooleanComparator to alter the sort order; if null, the natural order will be used
	 */
	public static void sort (boolean[] items, final @Nullable BooleanComparator c) {
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
	 * <p>If and only if {@code c} is null, this will use {@link #NATURAL_COMPARATOR} as its c.
	 *
	 * @param items the boolean array to be sorted
	 * @param from  the index of the first element (inclusive) to be sorted.
	 * @param to    the index of the last element (exclusive) to be sorted.
	 * @param c     a BooleanComparator to alter the sort order; if null, the natural order will be used
	 */
	public static void sort (boolean[] items, final int from, final int to, final @Nullable BooleanComparator c) {
		if (to <= 0) {
			return;
		}
		if (from < 0 || from >= items.length || to > items.length) {
			throw new UnsupportedOperationException("The given from/to range in BooleanComparators.sort() is invalid.");
		}
		if (c == null) {
			sort(items, from, to, NATURAL_COMPARATOR);
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

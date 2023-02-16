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
public final class ByteComparators {
	private ByteComparators () {
	}

	/**
	 * A type-specific comparator mimicking the natural order.
	 */
	protected static class NaturalImplicitComparator implements ByteComparator {

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
	protected static class OppositeImplicitComparator implements ByteComparator {

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

	protected static class OppositeComparator implements ByteComparator {

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
		if (c instanceof OppositeComparator) {return ((OppositeComparator)c).comparator;}
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
		if (c instanceof ByteComparator) {return (ByteComparator)c;}
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

	/**
	 * A type-specific comparator that compares items in the natural order, but as if they are unsigned
	 * (so, all negative items are greater than any non-negative items).
	 */
	protected static class UnsignedComparator implements ByteComparator {

		@Override
		public final int compare (final byte a, final byte b) {
			return Integer.compare(a & 0xFF, b & 0xFF);
		}

		@Override
		public ByteComparator reversed () {
			return UNSIGNED_OPPOSITE_COMPARATOR;
		}

	}

	public static final ByteComparator UNSIGNED_COMPARATOR = new ByteComparators.UnsignedComparator();

	/**
	 * A type-specific comparator that compares items in the opposite of the natural order, but as if they
	 * are unsigned.
	 */
	protected static class UnsignedOppositeComparator implements ByteComparator {

		@Override
		public final int compare (final byte a, final byte b) {
			return Integer.compare(b & 0xFF, a & 0xFF);
		}

		@Override
		public ByteComparator reversed () {
			return UNSIGNED_COMPARATOR;
		}

	}

	public static final ByteComparator UNSIGNED_OPPOSITE_COMPARATOR = new ByteComparators.UnsignedComparator();
	/// The remainder of the code is based on FastUtil.

	private static void swap (byte[] items, int first, int second) {
		byte firstValue = items[first];
		items[first] = items[second];
		items[second] = firstValue;
	}

	/**
	 * Transforms two consecutive sorted ranges into a single sorted range. The initial ranges are
	 * {@code [first..middle)} and {@code [middle..last)}, and the resulting range is
	 * {@code [first..last)}. Elements in the first input range will precede equal elements in
	 * the second.
	 */
	private static void inPlaceMerge (byte[] items, final int from, int mid, final int to, final ByteComparator comp) {
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
	 * @param items the byte array to be sorted
	 * @param from  the index of the first element (inclusive) to be included in the binary search.
	 * @param to    the index of the last element (exclusive) to be included in the binary search.
	 * @param pos   the position of the element to be searched for.
	 * @param comp  the comparison function.
	 * @return the largest index i such that, for every j in the range {@code [first..i)},
	 * {@code comp.compare(get(j), get(pos))} is {@code true}.
	 */
	private static int lowerBound (byte[] items, int from, final int to, final int pos, final ByteComparator comp) {
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
	 * @param items the byte array to be sorted
	 * @param from  the index of the first element (inclusive) to be included in the binary search.
	 * @param to    the index of the last element (exclusive) to be included in the binary search.
	 * @param pos   the position of the element to be searched for.
	 * @param comp  the comparison function.
	 * @return The largest index i such that, for every j in the range {@code [first..i)},
	 * {@code comp.compare(get(pos), get(j))} is {@code false}.
	 */
	private static int upperBound (byte[] items, int from, final int to, final int pos, final ByteComparator comp) {
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
	 * Sorts all of {@code items} by simply calling {@link #sort(byte[], int, int, ByteComparator)},
	 * setting {@code from} and {@code to} so the whole array is sorted.
	 *
	 * @param items the byte array to be sorted
	 * @param c     a ByteComparator to alter the sort order; if null, the natural order will be used
	 */
	public static void sort (byte[] items, final @Nullable ByteComparator c) {
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
	 * <p>If and only if {@code c} is null, this will delegate to {@link Arrays#sort(byte[], int, int)}, which
	 * does not have the same guarantees regarding allocation.
	 *
	 * @param items the byte array to be sorted
	 * @param from  the index of the first element (inclusive) to be sorted.
	 * @param to    the index of the last element (exclusive) to be sorted.
	 * @param c     a ByteComparator to alter the sort order; if null, the natural order will be used
	 */
	public static void sort (byte[] items, final int from, final int to, final @Nullable ByteComparator c) {
		if (to <= 0) {
			return;
		}
		if (from < 0 || from >= items.length || to > items.length) {
			throw new UnsupportedOperationException("The given from/to range in ByteComparators.sort() is invalid.");
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

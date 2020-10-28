package com.github.tommyettinger.ds.support.sort;

/*
 * Copyright (C) 2002-2020 Sebastiano Vigna
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

import com.github.tommyettinger.ds.Arrangeable;

/** A class providing static methods and objects that do useful things with arrays.
 *
 * <p>In addition to commodity methods, this class contains {@link Arrangeable}-based implementations
 * of a stable, in-place {@linkplain #mergeSort(int, int, IntComparator, Arrangeable) mergesort}.
 */

public class Sorts {

	private Sorts() {}
	
	/**
	 * Transforms two consecutive sorted ranges into a single sorted range. The initial ranges are
	 * {@code [first..middle)} and {@code [middle..last)}, and the resulting range is
	 * {@code [first..last)}. Elements in the first input range will precede equal elements in
	 * the second.
	 */
	private static void inPlaceMerge(final int from, int mid, final int to, final IntComparator comp, final Arrangeable swapper) {
		if (from >= mid || mid >= to) return;
		if (to - from == 2) {
			if (comp.compare(mid, from) < 0) swapper.swap(from, mid);
			return;
		}

		int firstCut;
		int secondCut;

		if (mid - from > to - mid) {
			firstCut = from + (mid - from) / 2;
			secondCut = lowerBound(mid, to, firstCut, comp);
		}
		else {
			secondCut = mid + (to - mid) / 2;
			firstCut = upperBound(from, mid, secondCut, comp);
		}

		int first2 = firstCut;
		int middle2 = mid;
		int last2 = secondCut;
		if (middle2 != first2 && middle2 != last2) {
			int first1 = first2;
			int last1 = middle2;
			while (first1 < --last1)
				swapper.swap(first1++, last1);
			first1 = middle2;
			last1 = last2;
			while (first1 < --last1)
				swapper.swap(first1++, last1);
			first1 = first2;
			last1 = last2;
			while (first1 < --last1)
				swapper.swap(first1++, last1);
		}

		mid = firstCut + (secondCut - mid);
		inPlaceMerge(from, firstCut, mid, comp, swapper);
		inPlaceMerge(mid, secondCut, to, comp, swapper);
	}

	/**
	 * Performs a binary search on an already-sorted range: finds the first position where an
	 * element can be inserted without violating the ordering. Sorting is by a user-supplied
	 * comparison function.
	 *
	 * @param from the index of the first element (inclusive) to be included in the binary search.
	 * @param to the index of the last element (exclusive) to be included in the binary search.
	 * @param pos the position of the element to be searched for.
	 * @param comp the comparison function.
	 * @return the largest index i such that, for every j in the range {@code [first..i)},
	 * {@code comp.compare(j, pos)} is {@code true}.
	 */
	private static int lowerBound(int from, final int to, final int pos, final IntComparator comp) {
		// if (comp==null) throw new NullPointerException();
		int len = to - from;
		while (len > 0) {
			int half = len / 2;
			int middle = from + half;
			if (comp.compare(middle, pos) < 0) {
				from = middle + 1;
				len -= half + 1;
			}
			else {
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
	 * @param from the index of the first element (inclusive) to be included in the binary search.
	 * @param to the index of the last element (exclusive) to be included in the binary search.
	 * @param pos the position of the element to be searched for.
	 * @param comp the comparison function.
	 * @return The largest index i such that, for every j in the range {@code [first..i)},
	 * {@code comp.compare(pos, j)} is {@code false}.
	 */
	private static int upperBound(int from, final int to, final int pos, final IntComparator comp) {
		// if (comp==null) throw new NullPointerException();
		int len = to - from;
		while (len > 0) {
			int half = len / 2;
			int middle = from + half;
			if (comp.compare(pos, middle) < 0) {
				len = half;
			}
			else {
				from = middle + 1;
				len -= half + 1;
			}
		}
		return from;
	}


	 /** Sorts the specified range of elements using the specified swapper and according to the order induced by the specified
	 * comparator using mergesort.
	 *
	 * <p>This sort is guaranteed to be <i>stable</i>: equal elements will not be reordered as a result
	 * of the sort. The sorting algorithm is an in-place mergesort that is significantly slower than a
	 * standard mergesort, as its running time is <i>O</i>(<var>n</var>&nbsp;(log&nbsp;<var>n</var>)<sup>2</sup>), but it does not allocate additional memory; as a result, it can be
	 * used as a generic sorting algorithm.
	 *
	 * @param from the index of the first element (inclusive) to be sorted.
	 * @param to the index of the last element (exclusive) to be sorted.
	 * @param c the comparator to determine the order of the generic data (arguments are positions).
	 * @param swapper an object that knows how to swap the elements at any two positions.
	 */
	public static void mergeSort(final int from, final int to, final IntComparator c, final Arrangeable swapper) {
		/*
		 * We retain the same method signature as quickSort. Given only a comparator and swapper we
		 * do not know how to copy and move elements from/to temporary arrays. Hence, in contrast to
		 * the JDK mergesorts this is an "in-place" mergesort, i.e. does not allocate any temporary
		 * arrays. A non-inplace mergesort would perhaps be faster in most cases, but would require
		 * non-intuitive delegate objects...
		 */
		final int length = to - from;

		// Insertion sort on smallest arrays, less than 16 items
		if (length < 16) {
			for (int i = from; i < to; i++) {
				for (int j = i; j > from && (c.compare(j - 1, j) > 0); j--) {
					swapper.swap(j, j - 1);
				}
			}
			return;
		}

		// Recursively sort halves
		int mid = (from + to) >>> 1;
		mergeSort(from, mid, c, swapper);
		mergeSort(mid, to, c, swapper);

		// If list is already sorted, nothing left to do. This is an
		// optimization that results in faster sorts for nearly ordered lists.
		if (c.compare(mid - 1, mid) <= 0) return;

		// Merge sorted halves
		inPlaceMerge(from, mid, to, c, swapper);
	}

}

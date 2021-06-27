/*
 * Copyright (C) 2002-2021 Sebastiano Vigna
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
 *
 *
 *
 * For the sorting and binary search code:
 *
 * Copyright (C) 1999 CERN - European Organization for Nuclear Research.
 *
 *   Permission to use, copy, modify, distribute and sell this software and
 *   its documentation for any purpose is hereby granted without fee,
 *   provided that the above copyright notice appear in all copies and that
 *   both that copyright notice and this permission notice appear in
 *   supporting documentation. CERN makes no representations about the
 *   suitability of this software for any purpose. It is provided "as is"
 *   without expressed or implied warranty.
 */
package com.github.tommyettinger.ds.support.sort;

import javax.annotation.Nullable;
import java.util.Comparator;

public final class ObjectComparators {
	private ObjectComparators () {
	}
	private static final int QUICKSORT_NO_REC = 16;
	private static final int QUICKSORT_MEDIAN_OF_9 = 128;

	/** Swaps two elements of an anrray.
	 *
	 * @param x an array.
	 * @param a a position in {@code x}.
	 * @param b another position in {@code x}.
	 */
	public static <K> void swap(final K x[], final int a, final int b) {
		final K t = x[a];
		x[a] = x[b];
		x[b] = t;
	}
	/** Swaps two sequences of elements of an array.
	 *
	 * @param x an array.
	 * @param a a position in {@code x}.
	 * @param b another position in {@code x}.
	 * @param n the number of elements to exchange starting at {@code a} and {@code b}.
	 */
	public static <K> void swap(final K[] x, int a, int b, final int n) {
		for(int i = 0; i < n; i++, a++, b++) swap(x, a, b);
	}
	private static <K> int med3(final K x[], final int a, final int b, final int c, Comparator <K> comp) {
		final int ab = comp.compare(x[a], x[b]);
		final int ac = comp.compare(x[a], x[c]);
		final int bc = comp.compare(x[b], x[c]);
		return (ab < 0 ?
			(bc < 0 ? b : ac < 0 ? c : a) :
			(bc > 0 ? b : ac > 0 ? c : a));
	}
	private static <K> void selectionSort(final K[] a, final int from, final int to, final Comparator <K> comp) {
		for(int i = from; i < to - 1; i++) {
			int m = i;
			for(int j = i + 1; j < to; j++) if (comp.compare(a[j], a[m]) < 0) m = j;
			if (m != i) {
				final K u = a[i];
				a[i] = a[m];
				a[m] = u;
			}
		}
	}
	private static <K> void insertionSort(final K[] a, final int from, final int to, final Comparator <K> comp) {
		for (int i = from; ++i < to;) {
			K t = a[i];
			int j = i;
			for (K u = a[j - 1]; comp.compare(t, u) < 0; u = a[--j - 1]) {
				a[j] = u;
				if (from == j - 1) {
					--j;
					break;
				}
			}
			a[j] = t;
		}
	}
	/** Sorts the specified range of elements according to the order induced by the specified
	 * comparator using quicksort.
	 *
	 * <p>The sorting algorithm is a tuned quicksort adapted from Jon L. Bentley and M. Douglas
	 * McIlroy, &ldquo;Engineering a Sort Function&rdquo;, <i>Software: Practice and Experience</i>, 23(11), pages
	 * 1249&minus;1265, 1993.
	 *
	 * <p>Note that this implementation does not allocate any object, contrarily to the implementation
	 * used to sort primitive types in {@link java.util.Arrays}, which switches to mergesort on large inputs.
	 *
	 * @param x the array to be sorted.
	 * @param from the index of the first element (inclusive) to be sorted.
	 * @param to the index of the last element (exclusive) to be sorted.
	 * @param comp the comparator to determine the sorting order.
	 *
	 */
	@SuppressWarnings("unchecked")
	public static <K> void sort (final K[] x, final int from, final int to, final @Nullable Comparator <K> comp) {
		if(comp == null) {
			sort(x, from, to, (Comparator<K>)Comparator.naturalOrder());
			return;
		}
		final int len = to - from;
		// Selection sort on smallest arrays
		if (len < QUICKSORT_NO_REC) {
			selectionSort(x, from, to, comp);
			return;
		}
		// Choose a partition element, v
		int m = from + len / 2;
		int l = from;
		int n = to - 1;
		if (len > QUICKSORT_MEDIAN_OF_9) { // Big arrays, pseudomedian of 9
			int s = len / 8;
			l = med3(x, l, l + s, l + 2 * s, comp);
			m = med3(x, m - s, m, m + s, comp);
			n = med3(x, n - 2 * s, n - s, n, comp);
		}
		m = med3(x, l, m, n, comp); // Mid-size, med of 3
		final K v = x[m];
		// Establish Invariant: v* (<v)* (>v)* v*
		int a = from, b = a, c = to - 1, d = c;
		while(true) {
			int comparison;
			while (b <= c && (comparison = comp.compare(x[b], v)) <= 0) {
				if (comparison == 0) swap(x, a++, b);
				b++;
			}
			while (c >= b && (comparison = comp.compare(x[c], v)) >=0) {
				if (comparison == 0) swap(x, c, d--);
				c--;
			}
			if (b > c) break;
			swap(x, b++, c--);
		}
		// Swap partition elements back to middle
		int s;
		s = Math.min(a - from, b - a);
		swap(x, from, b - s, s);
		s = Math.min(d - c, to - d - 1);
		swap(x, b, to - s, s);
		// Recursively sort non-partition-elements
		if ((s = b - a) > 1) sort(x, from, from + s, comp);
		if ((s = d - c) > 1) sort(x, to - s, to, comp);
	}
	/** Sorts an array according to the order induced by the specified
	 * comparator using quicksort.
	 *
	 * <p>The sorting algorithm is a tuned quicksort adapted from Jon L. Bentley and M. Douglas
	 * McIlroy, &ldquo;Engineering a Sort Function&rdquo;, <i>Software: Practice and Experience</i>, 23(11), pages
	 * 1249&minus;1265, 1993.
	 *
	 * <p>Note that this implementation does not allocate any object, contrarily to the implementation
	 * used to sort primitive types in {@link java.util.Arrays}, which switches to mergesort on large inputs.
	 *
	 * @param x the array to be sorted.
	 * @param comp the comparator to determine the sorting order.
	 *
	 */
	public static <K> void sort (final K[] x, final @Nullable Comparator<K> comp) {
		sort(x, 0, x.length, comp);
	}

}

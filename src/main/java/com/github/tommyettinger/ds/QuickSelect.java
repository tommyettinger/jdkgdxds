/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
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
 ******************************************************************************/

package com.github.tommyettinger.ds;

import java.util.Comparator;

/**
 * Implementation of Tony Hoare's quickselect algorithm. Running time is generally O(n), but worst case is O(n^2).
 * Pivot choice is median of three method, providing better performance than a random pivot for partially sorted data.
 * See <a href="http://en.wikipedia.org/wiki/Quickselect">Wikipedia's Quickselect article</a> for more.
 * @author Jon Renner
 * @author Tommy Ettinger (adapted the class to carry no state)
 */
public class QuickSelect {
	public static <T> int select (T[] items, Comparator<? super T> comp, int n, int size) {
		return recursiveSelect(items, comp, 0, size - 1, n);
	}

	private static <T> int partition (T[] items, Comparator<? super T> comp, int left, int right, int pivot) {
		T pivotValue = items[pivot];
		swap(items, right, pivot);
		int storage = left;
		for (int i = left; i < right; i++) {
			if (comp.compare(items[i], pivotValue) < 0) {
				swap(items, storage, i);
				storage++;
			}
		}
		swap(items, right, storage);
		return storage;
	}

	private static <T> int recursiveSelect (T[] items, Comparator<? super T> comp, int left, int right, int k) {
		if (left == right) return left;
		int pivotIndex = medianOfThreePivot(items, comp, left, right);
		int pivotNewIndex = partition(items, comp, left, right, pivotIndex);
		int pivotDist = (pivotNewIndex - left) + 1;
		int result;
		if (pivotDist == k) {
			result = pivotNewIndex;
		} else if (k < pivotDist) {
			result = recursiveSelect(items, comp, left, pivotNewIndex - 1, k);
		} else {
			result = recursiveSelect(items, comp, pivotNewIndex + 1, right, k - pivotDist);
		}
		return result;
	}

	/** Median of Three has the potential to outperform a random pivot, especially for partially sorted arrays */
	private static <T> int medianOfThreePivot (T[] items, Comparator<? super T> comp, int leftIdx, int rightIdx) {
		T left = items[leftIdx];
		int midIdx = (leftIdx + rightIdx) / 2;
		T mid = items[midIdx];
		T right = items[rightIdx];

		// spaghetti median of three algorithm
		// does at most 3 comparisons
		if (comp.compare(left, mid) > 0) {
			if (comp.compare(mid, right) > 0) {
				return midIdx;
			} else if (comp.compare(left, right) > 0) {
				return rightIdx;
			} else {
				return leftIdx;
			}
		} else {
			if (comp.compare(left, right) > 0) {
				return leftIdx;
			} else if (comp.compare(mid, right) > 0) {
				return rightIdx;
			} else {
				return midIdx;
			}
		}
	}

	private static <T> void swap (T[] items, int left, int right) {
		T tmp = items[left];
		items[left] = items[right];
		items[right] = tmp;
	}

	public static <T> int select (ObjectList<T> items, Comparator<? super T> comp, int n, int size) {
		return recursiveSelect(items, comp, 0, size - 1, n);
	}

	private static <T> int partition (ObjectList<T> items, Comparator<? super T> comp, int left, int right, int pivot) {
		T pivotValue = items.get(pivot);
		items.swap(right, pivot);
		int storage = left;
		for (int i = left; i < right; i++) {
			if (comp.compare(items.get(i), pivotValue) < 0) {
				items.swap(storage, i);
				storage++;
			}
		}
		items.swap(right, storage);
		return storage;
	}

	private static <T> int recursiveSelect (ObjectList<T> items, Comparator<? super T> comp, int left, int right, int k) {
		if (left == right) return left;
		int pivotIndex = medianOfThreePivot(items, comp, left, right);
		int pivotNewIndex = partition(items, comp, left, right, pivotIndex);
		int pivotDist = (pivotNewIndex - left) + 1;
		int result;
		if (pivotDist == k) {
			result = pivotNewIndex;
		} else if (k < pivotDist) {
			result = recursiveSelect(items, comp, left, pivotNewIndex - 1, k);
		} else {
			result = recursiveSelect(items, comp, pivotNewIndex + 1, right, k - pivotDist);
		}
		return result;
	}

	/** Median of Three has the potential to outperform a random pivot, especially for partially sorted arrays */
	private static <T> int medianOfThreePivot (ObjectList<T> items, Comparator<? super T> comp, int leftIdx, int rightIdx) {
		T left = items.get(leftIdx);
		int midIdx = (leftIdx + rightIdx) / 2;
		T mid = items.get(midIdx);
		T right = items.get(rightIdx);

		// spaghetti median of three algorithm
		// does at most 3 comparisons
		if (comp.compare(left, mid) > 0) {
			if (comp.compare(mid, right) > 0) {
				return midIdx;
			} else if (comp.compare(left, right) > 0) {
				return rightIdx;
			} else {
				return leftIdx;
			}
		} else {
			if (comp.compare(left, right) > 0) {
				return leftIdx;
			} else if (comp.compare(mid, right) > 0) {
				return rightIdx;
			} else {
				return midIdx;
			}
		}
	}

}

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

package com.github.tommyettinger.ds;

import com.github.tommyettinger.ds.support.sort.BooleanComparator;
import com.github.tommyettinger.ds.support.sort.ByteComparator;
import com.github.tommyettinger.ds.support.sort.CharComparator;
import com.github.tommyettinger.ds.support.sort.DoubleComparator;
import com.github.tommyettinger.ds.support.sort.FloatComparator;
import com.github.tommyettinger.ds.support.sort.IntComparator;
import com.github.tommyettinger.ds.support.sort.LongComparator;
import com.github.tommyettinger.ds.support.sort.ShortComparator;

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
		if (left == right)
			return left;
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

	/**
	 * Median of Three has the potential to outperform a random pivot, especially for partially sorted arrays
	 */
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
		if (left == right)
			return left;
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

	/**
	 * Median of Three has the potential to outperform a random pivot, especially for partially sorted arrays
	 */
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

	//// primitive lists

	// ints
	public static int select (IntList items, IntComparator comp, int n, int size) {
		return recursiveSelect(items, comp, 0, size - 1, n);
	}

	private static int partition (IntList items, IntComparator comp, int left, int right, int pivot) {
		int pivotValue = items.get(pivot);
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

	private static int recursiveSelect (IntList items, IntComparator comp, int left, int right, int k) {
		if (left == right)
			return left;
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

	/**
	 * Median of Three has the potential to outperform a random pivot, especially for partially sorted arrays
	 */
	private static int medianOfThreePivot (IntList items, IntComparator comp, int leftIdx, int rightIdx) {
		int left = items.get(leftIdx);
		int midIdx = (leftIdx + rightIdx) / 2;
		int mid = items.get(midIdx);
		int right = items.get(rightIdx);

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

	// longs
	public static int select (LongList items, LongComparator comp, int n, int size) {
		return recursiveSelect(items, comp, 0, size - 1, n);
	}

	private static int partition (LongList items, LongComparator comp, int left, int right, int pivot) {
		long pivotValue = items.get(pivot);
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

	private static int recursiveSelect (LongList items, LongComparator comp, int left, int right, int k) {
		if (left == right)
			return left;
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

	/**
	 * Median of Three has the potential to outperform a random pivot, especially for partially sorted arrays
	 */
	private static int medianOfThreePivot (LongList items, LongComparator comp, int leftIdx, int rightIdx) {
		long left = items.get(leftIdx);
		int midIdx = (leftIdx + rightIdx) / 2;
		long mid = items.get(midIdx);
		long right = items.get(rightIdx);

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

	//// floats
	public static int select (FloatList items, FloatComparator comp, int n, int size) {
		return recursiveSelect(items, comp, 0, size - 1, n);
	}

	private static int partition (FloatList items, FloatComparator comp, int left, int right, int pivot) {
		float pivotValue = items.get(pivot);
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

	private static int recursiveSelect (FloatList items, FloatComparator comp, int left, int right, int k) {
		if (left == right)
			return left;
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

	/**
	 * Median of Three has the potential to outperform a random pivot, especially for partially sorted arrays
	 */
	private static int medianOfThreePivot (FloatList items, FloatComparator comp, int leftIdx, int rightIdx) {
		float left = items.get(leftIdx);
		int midIdx = (leftIdx + rightIdx) / 2;
		float mid = items.get(midIdx);
		float right = items.get(rightIdx);

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


	//// doubles
	public static int select (DoubleList items, DoubleComparator comp, int n, int size) {
		return recursiveSelect(items, comp, 0, size - 1, n);
	}

	private static int partition (DoubleList items, DoubleComparator comp, int left, int right, int pivot) {
		double pivotValue = items.get(pivot);
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

	private static int recursiveSelect (DoubleList items, DoubleComparator comp, int left, int right, int k) {
		if (left == right)
			return left;
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

	/**
	 * Median of Three has the potential to outperform a random pivot, especially for partially sorted arrays
	 */
	private static int medianOfThreePivot (DoubleList items, DoubleComparator comp, int leftIdx, int rightIdx) {
		double left = items.get(leftIdx);
		int midIdx = (leftIdx + rightIdx) / 2;
		double mid = items.get(midIdx);
		double right = items.get(rightIdx);

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

	// shorts
	public static int select (ShortList items, ShortComparator comp, int n, int size) {
		return recursiveSelect(items, comp, 0, size - 1, n);
	}

	private static int partition (ShortList items, ShortComparator comp, int left, int right, int pivot) {
		short pivotValue = items.get(pivot);
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

	private static int recursiveSelect (ShortList items, ShortComparator comp, int left, int right, int k) {
		if (left == right)
			return left;
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

	/**
	 * Median of Three has the potential to outperform a random pivot, especially for partially sorted arrays
	 */
	private static int medianOfThreePivot (ShortList items, ShortComparator comp, int leftIdx, int rightIdx) {
		short left = items.get(leftIdx);
		int midIdx = (leftIdx + rightIdx) / 2;
		short mid = items.get(midIdx);
		short right = items.get(rightIdx);

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

	// bytes
	public static int select (ByteList items, ByteComparator comp, int n, int size) {
		return recursiveSelect(items, comp, 0, size - 1, n);
	}

	private static int partition (ByteList items, ByteComparator comp, int left, int right, int pivot) {
		byte pivotValue = items.get(pivot);
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

	private static int recursiveSelect (ByteList items, ByteComparator comp, int left, int right, int k) {
		if (left == right)
			return left;
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

	/**
	 * Median of Three has the potential to outperform a random pivot, especially for partially sorted arrays
	 */
	private static int medianOfThreePivot (ByteList items, ByteComparator comp, int leftIdx, int rightIdx) {
		byte left = items.get(leftIdx);
		int midIdx = (leftIdx + rightIdx) / 2;
		byte mid = items.get(midIdx);
		byte right = items.get(rightIdx);

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

	// chars
	public static int select (CharList items, CharComparator comp, int n, int size) {
		return recursiveSelect(items, comp, 0, size - 1, n);
	}

	private static int partition (CharList items, CharComparator comp, int left, int right, int pivot) {
		char pivotValue = items.get(pivot);
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

	private static int recursiveSelect (CharList items, CharComparator comp, int left, int right, int k) {
		if (left == right)
			return left;
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

	/**
	 * Median of Three has the potential to outperform a random pivot, especially for partially sorted arrays
	 */
	private static int medianOfThreePivot (CharList items, CharComparator comp, int leftIdx, int rightIdx) {
		char left = items.get(leftIdx);
		int midIdx = (leftIdx + rightIdx) / 2;
		char mid = items.get(midIdx);
		char right = items.get(rightIdx);

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

	// booleans
	public static int select (BooleanList items, BooleanComparator comp, int n, int size) {
		return recursiveSelect(items, comp, 0, size - 1, n);
	}

	private static int partition (BooleanList items, BooleanComparator comp, int left, int right, int pivot) {
		boolean pivotValue = items.get(pivot);
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

	private static int recursiveSelect (BooleanList items, BooleanComparator comp, int left, int right, int k) {
		if (left == right)
			return left;
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

	/**
	 * Median of Three has the potential to outperform a random pivot, especially for partially sorted arrays
	 */
	private static int medianOfThreePivot (BooleanList items, BooleanComparator comp, int leftIdx, int rightIdx) {
		boolean left = items.get(leftIdx);
		int midIdx = (leftIdx + rightIdx) / 2;
		boolean mid = items.get(midIdx);
		boolean right = items.get(rightIdx);

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
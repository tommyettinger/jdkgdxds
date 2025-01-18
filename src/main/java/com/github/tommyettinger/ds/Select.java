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
 * This class is for selecting a ranked element (kth ordered statistic) from an unordered list in faster time than sorting the
 * whole array. Typical applications include finding the nearest enemy unit(s), and other operations which are likely to run as
 * often as every x frames. Certain values of k will result in a partial sorting of the array.
 * <p>
 * The lowest ranking element starts at 1, not 0. 1 = first, 2 = second, 3 = third, etc. calling with a value of zero will result
 * in a {@link RuntimeException}
 * </p>
 * <p>
 * This class uses very minimal extra memory, as it makes no copies of the array. The underlying algorithms used are a naive
 * single-pass for k=min and k=max, and Hoare's quickselect for values in between.
 * </p>
 *
 * @author Jon Renner
 * @author Tommy Ettinger (just made it carry no state)
 */
public final class Select {
	/**
	 * Not instantiable.
	 */
	private Select () {
	}

	public static <T> T select (T[] items, Comparator<T> comp, int kthLowest, int size) {
		int idx = selectIndex(items, comp, kthLowest, size);
		return items[idx];
	}

	public static <T> int selectIndex (T[] items, Comparator<T> comp, int kthLowest, int size) {
		if (size < 1) {
			throw new RuntimeException("cannot select from empty array (size < 1)");
		} else if (kthLowest > size) {
			throw new RuntimeException("Kth rank is larger than size. k: " + kthLowest + ", size: " + size);
		}
		int idx;
		// naive partial selection sort almost certain to outperform quickselect where n is min or max
		if (kthLowest == 1) {
			// find min
			idx = fastMin(items, comp, size);
		} else if (kthLowest == size) {
			// find max
			idx = fastMax(items, comp, size);
		} else {
			idx = QuickSelect.select(items, comp, kthLowest, size);
		}
		return idx;
	}

	/**
	 * Faster than quickselect for n = min
	 */
	private static <T> int fastMin (T[] items, Comparator<T> comp, int size) {
		int lowestIdx = 0;
		for (int i = 1; i < size; i++) {
			int comparison = comp.compare(items[i], items[lowestIdx]);
			if (comparison < 0) {
				lowestIdx = i;
			}
		}
		return lowestIdx;
	}

	/**
	 * Faster than quickselect for n = max
	 */
	private static <T> int fastMax (T[] items, Comparator<T> comp, int size) {
		int highestIdx = 0;
		for (int i = 1; i < size; i++) {
			int comparison = comp.compare(items[i], items[highestIdx]);
			if (comparison > 0) {
				highestIdx = i;
			}
		}
		return highestIdx;
	}

	public static <T> T select (ObjectList<T> items, Comparator<T> comp, int kthLowest, int size) {
		int idx = selectIndex(items, comp, kthLowest, size);
		return items.get(idx);
	}

	public static <T> int selectIndex (ObjectList<T> items, Comparator<T> comp, int kthLowest, int size) {
		if (size < 1) {
			throw new RuntimeException("cannot select from empty array (size < 1)");
		} else if (kthLowest > size) {
			throw new RuntimeException("Kth rank is larger than size. k: " + kthLowest + ", size: " + size);
		}
		int idx;
		// naive partial selection sort almost certain to outperform quickselect where n is min or max
		if (kthLowest == 1) {
			// find min
			idx = fastMin(items, comp, size);
		} else if (kthLowest == size) {
			// find max
			idx = fastMax(items, comp, size);
		} else {
			idx = QuickSelect.select(items, comp, kthLowest, size);
		}
		return idx;
	}

	/**
	 * Faster than quickselect for n = min
	 */
	private static <T> int fastMin (ObjectList<T> items, Comparator<T> comp, int size) {
		int lowestIdx = 0;
		for (int i = 1; i < size; i++) {
			int comparison = comp.compare(items.get(i), items.get(lowestIdx));
			if (comparison < 0) {
				lowestIdx = i;
			}
		}
		return lowestIdx;
	}

	/**
	 * Faster than quickselect for n = max
	 */
	private static <T> int fastMax (ObjectList<T> items, Comparator<T> comp, int size) {
		int highestIdx = 0;
		for (int i = 1; i < size; i++) {
			int comparison = comp.compare(items.get(i), items.get(highestIdx));
			if (comparison > 0) {
				highestIdx = i;
			}
		}
		return highestIdx;
	}

	public static int select (IntList items, IntComparator comp, int kthLowest, int size) {
		int idx = selectIndex(items, comp, kthLowest, size);
		return items.get(idx);
	}

	public static int selectIndex (IntList items, IntComparator comp, int kthLowest, int size) {
		if (size < 1) {
			throw new RuntimeException("cannot select from empty array (size < 1)");
		} else if (kthLowest > size) {
			throw new RuntimeException("Kth rank is larger than size. k: " + kthLowest + ", size: " + size);
		}
		int idx;
		// naive partial selection sort almost certain to outperform quickselect where n is min or max
		if (kthLowest == 1) {
			// find min
			idx = fastMin(items, comp, size);
		} else if (kthLowest == size) {
			// find max
			idx = fastMax(items, comp, size);
		} else {
			idx = QuickSelect.select(items, comp, kthLowest, size);
		}
		return idx;
	}

	/**
	 * Faster than quickselect for n = min
	 */
	private static int fastMin (IntList items, IntComparator comp, int size) {
		int lowestIdx = 0;
		for (int i = 1; i < size; i++) {
			int comparison = comp.compare(items.get(i), items.get(lowestIdx));
			if (comparison < 0) {
				lowestIdx = i;
			}
		}
		return lowestIdx;
	}

	/**
	 * Faster than quickselect for n = max
	 */
	private static int fastMax (IntList items, IntComparator comp, int size) {
		int highestIdx = 0;
		for (int i = 1; i < size; i++) {
			int comparison = comp.compare(items.get(i), items.get(highestIdx));
			if (comparison > 0) {
				highestIdx = i;
			}
		}
		return highestIdx;
	}

	public static long select (LongList items, LongComparator comp, int kthLowest, int size) {
		int idx = selectIndex(items, comp, kthLowest, size);
		return items.get(idx);
	}

	public static int selectIndex (LongList items, LongComparator comp, int kthLowest, int size) {
		if (size < 1) {
			throw new RuntimeException("cannot select from empty array (size < 1)");
		} else if (kthLowest > size) {
			throw new RuntimeException("Kth rank is larger than size. k: " + kthLowest + ", size: " + size);
		}
		int idx;
		// naive partial selection sort almost certain to outperform quickselect where n is min or max
		if (kthLowest == 1) {
			// find min
			idx = fastMin(items, comp, size);
		} else if (kthLowest == size) {
			// find max
			idx = fastMax(items, comp, size);
		} else {
			idx = QuickSelect.select(items, comp, kthLowest, size);
		}
		return idx;
	}

	/**
	 * Faster than quickselect for n = min
	 */
	private static int fastMin (LongList items, LongComparator comp, int size) {
		int lowestIdx = 0;
		for (int i = 1; i < size; i++) {
			int comparison = comp.compare(items.get(i), items.get(lowestIdx));
			if (comparison < 0) {
				lowestIdx = i;
			}
		}
		return lowestIdx;
	}

	/**
	 * Faster than quickselect for n = max
	 */
	private static int fastMax (LongList items, LongComparator comp, int size) {
		int highestIdx = 0;
		for (int i = 1; i < size; i++) {
			int comparison = comp.compare(items.get(i), items.get(highestIdx));
			if (comparison > 0) {
				highestIdx = i;
			}
		}
		return highestIdx;
	}

	public static float select (FloatList items, FloatComparator comp, int kthLowest, int size) {
		int idx = selectIndex(items, comp, kthLowest, size);
		return items.get(idx);
	}

	public static int selectIndex (FloatList items, FloatComparator comp, int kthLowest, int size) {
		if (size < 1) {
			throw new RuntimeException("cannot select from empty array (size < 1)");
		} else if (kthLowest > size) {
			throw new RuntimeException("Kth rank is larger than size. k: " + kthLowest + ", size: " + size);
		}
		int idx;
		// naive partial selection sort almost certain to outperform quickselect where n is min or max
		if (kthLowest == 1) {
			// find min
			idx = fastMin(items, comp, size);
		} else if (kthLowest == size) {
			// find max
			idx = fastMax(items, comp, size);
		} else {
			idx = QuickSelect.select(items, comp, kthLowest, size);
		}
		return idx;
	}

	/**
	 * Faster than quickselect for n = min
	 */
	private static int fastMin (FloatList items, FloatComparator comp, int size) {
		int lowestIdx = 0;
		for (int i = 1; i < size; i++) {
			int comparison = comp.compare(items.get(i), items.get(lowestIdx));
			if (comparison < 0) {
				lowestIdx = i;
			}
		}
		return lowestIdx;
	}

	/**
	 * Faster than quickselect for n = max
	 */
	private static int fastMax (FloatList items, FloatComparator comp, int size) {
		int highestIdx = 0;
		for (int i = 1; i < size; i++) {
			int comparison = comp.compare(items.get(i), items.get(highestIdx));
			if (comparison > 0) {
				highestIdx = i;
			}
		}
		return highestIdx;
	}

	public static double select (DoubleList items, DoubleComparator comp, int kthLowest, int size) {
		int idx = selectIndex(items, comp, kthLowest, size);
		return items.get(idx);
	}

	public static int selectIndex (DoubleList items, DoubleComparator comp, int kthLowest, int size) {
		if (size < 1) {
			throw new RuntimeException("cannot select from empty array (size < 1)");
		} else if (kthLowest > size) {
			throw new RuntimeException("Kth rank is larger than size. k: " + kthLowest + ", size: " + size);
		}
		int idx;
		// naive partial selection sort almost certain to outperform quickselect where n is min or max
		if (kthLowest == 1) {
			// find min
			idx = fastMin(items, comp, size);
		} else if (kthLowest == size) {
			// find max
			idx = fastMax(items, comp, size);
		} else {
			idx = QuickSelect.select(items, comp, kthLowest, size);
		}
		return idx;
	}

	/**
	 * Faster than quickselect for n = min
	 */
	private static int fastMin (DoubleList items, DoubleComparator comp, int size) {
		int lowestIdx = 0;
		for (int i = 1; i < size; i++) {
			int comparison = comp.compare(items.get(i), items.get(lowestIdx));
			if (comparison < 0) {
				lowestIdx = i;
			}
		}
		return lowestIdx;
	}

	/**
	 * Faster than quickselect for n = max
	 */
	private static int fastMax (DoubleList items, DoubleComparator comp, int size) {
		int highestIdx = 0;
		for (int i = 1; i < size; i++) {
			int comparison = comp.compare(items.get(i), items.get(highestIdx));
			if (comparison > 0) {
				highestIdx = i;
			}
		}
		return highestIdx;
	}

	public static short select (ShortList items, ShortComparator comp, int kthLowest, int size) {
		int idx = selectIndex(items, comp, kthLowest, size);
		return items.get(idx);
	}

	public static int selectIndex (ShortList items, ShortComparator comp, int kthLowest, int size) {
		if (size < 1) {
			throw new RuntimeException("cannot select from empty array (size < 1)");
		} else if (kthLowest > size) {
			throw new RuntimeException("Kth rank is larger than size. k: " + kthLowest + ", size: " + size);
		}
		int idx;
		// naive partial selection sort almost certain to outperform quickselect where n is min or max
		if (kthLowest == 1) {
			// find min
			idx = fastMin(items, comp, size);
		} else if (kthLowest == size) {
			// find max
			idx = fastMax(items, comp, size);
		} else {
			idx = QuickSelect.select(items, comp, kthLowest, size);
		}
		return idx;
	}

	/**
	 * Faster than quickselect for n = min
	 */
	private static int fastMin (ShortList items, ShortComparator comp, int size) {
		int lowestIdx = 0;
		for (int i = 1; i < size; i++) {
			int comparison = comp.compare(items.get(i), items.get(lowestIdx));
			if (comparison < 0) {
				lowestIdx = i;
			}
		}
		return lowestIdx;
	}

	/**
	 * Faster than quickselect for n = max
	 */
	private static int fastMax (ShortList items, ShortComparator comp, int size) {
		int highestIdx = 0;
		for (int i = 1; i < size; i++) {
			int comparison = comp.compare(items.get(i), items.get(highestIdx));
			if (comparison > 0) {
				highestIdx = i;
			}
		}
		return highestIdx;
	}

	public static byte select (ByteList items, ByteComparator comp, int kthLowest, int size) {
		int idx = selectIndex(items, comp, kthLowest, size);
		return items.get(idx);
	}

	public static int selectIndex (ByteList items, ByteComparator comp, int kthLowest, int size) {
		if (size < 1) {
			throw new RuntimeException("cannot select from empty array (size < 1)");
		} else if (kthLowest > size) {
			throw new RuntimeException("Kth rank is larger than size. k: " + kthLowest + ", size: " + size);
		}
		int idx;
		// naive partial selection sort almost certain to outperform quickselect where n is min or max
		if (kthLowest == 1) {
			// find min
			idx = fastMin(items, comp, size);
		} else if (kthLowest == size) {
			// find max
			idx = fastMax(items, comp, size);
		} else {
			idx = QuickSelect.select(items, comp, kthLowest, size);
		}
		return idx;
	}

	/**
	 * Faster than quickselect for n = min
	 */
	private static int fastMin (ByteList items, ByteComparator comp, int size) {
		int lowestIdx = 0;
		for (int i = 1; i < size; i++) {
			int comparison = comp.compare(items.get(i), items.get(lowestIdx));
			if (comparison < 0) {
				lowestIdx = i;
			}
		}
		return lowestIdx;
	}

	/**
	 * Faster than quickselect for n = max
	 */
	private static int fastMax (ByteList items, ByteComparator comp, int size) {
		int highestIdx = 0;
		for (int i = 1; i < size; i++) {
			int comparison = comp.compare(items.get(i), items.get(highestIdx));
			if (comparison > 0) {
				highestIdx = i;
			}
		}
		return highestIdx;
	}

	public static char select (CharList items, CharComparator comp, int kthLowest, int size) {
		int idx = selectIndex(items, comp, kthLowest, size);
		return items.get(idx);
	}

	public static int selectIndex (CharList items, CharComparator comp, int kthLowest, int size) {
		if (size < 1) {
			throw new RuntimeException("cannot select from empty array (size < 1)");
		} else if (kthLowest > size) {
			throw new RuntimeException("Kth rank is larger than size. k: " + kthLowest + ", size: " + size);
		}
		int idx;
		// naive partial selection sort almost certain to outperform quickselect where n is min or max
		if (kthLowest == 1) {
			// find min
			idx = fastMin(items, comp, size);
		} else if (kthLowest == size) {
			// find max
			idx = fastMax(items, comp, size);
		} else {
			idx = QuickSelect.select(items, comp, kthLowest, size);
		}
		return idx;
	}

	/**
	 * Faster than quickselect for n = min
	 */
	private static int fastMin (CharList items, CharComparator comp, int size) {
		int lowestIdx = 0;
		for (int i = 1; i < size; i++) {
			int comparison = comp.compare(items.get(i), items.get(lowestIdx));
			if (comparison < 0) {
				lowestIdx = i;
			}
		}
		return lowestIdx;
	}

	/**
	 * Faster than quickselect for n = max
	 */
	private static int fastMax (CharList items, CharComparator comp, int size) {
		int highestIdx = 0;
		for (int i = 1; i < size; i++) {
			int comparison = comp.compare(items.get(i), items.get(highestIdx));
			if (comparison > 0) {
				highestIdx = i;
			}
		}
		return highestIdx;
	}

	public static boolean select (BooleanList items, BooleanComparator comp, int kthLowest, int size) {
		int idx = selectIndex(items, comp, kthLowest, size);
		return items.get(idx);
	}

	public static int selectIndex (BooleanList items, BooleanComparator comp, int kthLowest, int size) {
		if (size < 1) {
			throw new RuntimeException("cannot select from empty array (size < 1)");
		} else if (kthLowest > size) {
			throw new RuntimeException("Kth rank is larger than size. k: " + kthLowest + ", size: " + size);
		}
		int idx;
		// naive partial selection sort almost certain to outperform quickselect where n is min or max
		if (kthLowest == 1) {
			// find min
			idx = fastMin(items, comp, size);
		} else if (kthLowest == size) {
			// find max
			idx = fastMax(items, comp, size);
		} else {
			idx = QuickSelect.select(items, comp, kthLowest, size);
		}
		return idx;
	}

	/**
	 * Faster than quickselect for n = min
	 */
	private static int fastMin (BooleanList items, BooleanComparator comp, int size) {
		int lowestIdx = 0;
		for (int i = 1; i < size; i++) {
			int comparison = comp.compare(items.get(i), items.get(lowestIdx));
			if (comparison < 0) {
				lowestIdx = i;
			}
		}
		return lowestIdx;
	}

	/**
	 * Faster than quickselect for n = max
	 */
	private static int fastMax (BooleanList items, BooleanComparator comp, int size) {
		int highestIdx = 0;
		for (int i = 1; i < size; i++) {
			int comparison = comp.compare(items.get(i), items.get(highestIdx));
			if (comparison > 0) {
				highestIdx = i;
			}
		}
		return highestIdx;
	}

}

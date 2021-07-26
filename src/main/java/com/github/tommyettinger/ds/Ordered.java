/*******************************************************************************
 * Copyright 2021 See AUTHORS file.
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

import com.github.tommyettinger.ds.support.LaserRandom;
import com.github.tommyettinger.ds.support.sort.BooleanComparator;
import com.github.tommyettinger.ds.support.sort.ByteComparator;
import com.github.tommyettinger.ds.support.sort.CharComparator;
import com.github.tommyettinger.ds.support.sort.DoubleComparator;
import com.github.tommyettinger.ds.support.sort.FloatComparator;
import com.github.tommyettinger.ds.support.sort.IntComparator;
import com.github.tommyettinger.ds.support.sort.LongComparator;
import com.github.tommyettinger.ds.support.sort.ShortComparator;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Comparator;
import com.github.tommyettinger.ds.support.EnhancedRandom;

/**
 * Ensures that implementors allow access to the order of {@code T} items as an ObjectList.
 * This is meant to allow different (typically insertion-ordered) data structures to all have their order
 * manipulated by the same methods. This interface extends {@link Arrangeable}, which itself is compatible
 * both with primitive-backed collections like {@link IntList} and generic ones like the implementations of
 * Ordered. This has default implementations of {@link Arrangeable#swap(int, int)} and {@link Arrangeable#shuffle(EnhancedRandom)}.
 *
 * @author Tommy Ettinger
 */
public interface Ordered<T> extends Arrangeable {
	/**
	 * Gets the ObjectList of T items that this data structure holds, in the order it uses for iteration.
	 * This should usually return a direct reference to an ObjectList used inside this object, so changes
	 * to the list will affect this.
	 *
	 * @return the ObjectList of T items that this data structure holds
	 */
	ObjectList<T> order ();

	/**
	 * Switches the ordering of positions {@code first} and {@code second}, without changing any items beyond that.
	 *
	 * @param first the first position, must not be negative and must be less than {@link #size()}
	 * @param second the second position, must not be negative and must be less than {@link #size()}
	 */
	@Override
	default void swap (int first, int second) {
		ObjectList<T> order = order();
		order.set(first, order.set(second, order.get(first)));
	}

	/**
	 * Pseudo-randomly shuffles the order of this Ordered in-place.
	 * You can seed {@code rng}, the random number generator, with an identical seed (or in {@link LaserRandom}'s
	 * case, an identical pair of seeds) to reproduce a shuffle on two Ordered with the same {@link #size()}.
	 * Using {@link LaserRandom} is recommended because it has the potential to produce all shuffles for up to 33
	 * items (though it would take millions of years to do so, and would require changing the stream every few
	 * years), while {@link java.util.Random} can only produce all shuffles for up to 16 items. The RandomXS128
	 * class in libGDX technically can produce more possible shuffles than LaserRandom (34 items instead of 33),
	 * but is somewhat slower and is not ideal in terms of statistical bias.
	 *
	 * @param rng any {@link EnhancedRandom} implementation; you can use {@link LaserRandom} in this library
	 */
	@Override
	default void shuffle (EnhancedRandom rng) {
		ObjectList<T> order = order();
		for (int i = order.size() - 1; i >= 0; i--) {
			order.set(i, order.set(rng.nextInt(i + 1), order.get(i)));
		}
	}

	/**
	 * Reverses the order of this Ordered in-place.
	 */
	@Override
	default void reverse () {
		Collections.reverse(order());
	}

	/**
	 * Gets a random T value from this Ordered, where T is typically the key type for Maps and the
	 * item type for Lists and Sets. The random number generator {@code rng} should probably be a
	 * {@link LaserRandom}, because it implements a fast {@link LaserRandom#nextInt(int)} method.
	 *
	 * @param rng any {@link EnhancedRandom} implementation; you can use {@link LaserRandom} in this library
	 * @return a random T value from this Ordered
	 */
	default T random (EnhancedRandom rng) {
		return order().random(rng);
	}

	/**
	 * Sorts this Ordered according to the order induced by the specified
	 * {@link Comparator}.  The sort is <i>stable</i>: this method must not
	 * reorder equal elements.
	 * <br>
	 * All elements in the {@link #order()} must be <i>mutually comparable</i> using the
	 * specified comparator (that is, {@code c.compare(e1, e2)} must not throw
	 * a {@code ClassCastException} for any elements {@code e1} and {@code e2}
	 * in the list).
	 * <br>
	 * If the specified comparator is {@code null} then all elements in this
	 * Ordered must implement the {@link Comparable} interface and the elements'
	 * {@linkplain Comparable natural ordering} should be used.
	 * @param comparator used to sort the T items this contains; may be null if T implements Comparable
	 */
	default void sort(@Nullable Comparator<? super T> comparator) {
		order().sort(comparator);
	}

	/**
	 * Selects the kth-lowest element from this Ordered according to Comparator ranking. This might partially sort the Ordered,
	 * changing its order. The Ordered must have a size greater than 0, or a {@link RuntimeException} will be thrown.
	 * @see Select
	 * @param comparator used for comparison
	 * @param kthLowest rank of desired object according to comparison; k is based on ordinal numbers, not array indices. For min
	 *           value use 1, for max value use size of the Ordered; using 0 results in a runtime exception.
	 * @return the value of the kth lowest ranked object. */
	default T selectRanked (Comparator<T> comparator, int kthLowest) {
		if (kthLowest < 1) {
			throw new RuntimeException("kthLowest must be greater than 0; 1 = first, 2 = second...");
		}
		return Select.select(order(), comparator, kthLowest, size());
	}

	/**
	 * Gets the index of the kth-lowest element from this Ordered according to Comparator ranking. This might partially sort the
	 * Ordered, changing its order. The Ordered must have a size greater than 0, or a {@link RuntimeException} will be thrown.
	 * @see Ordered#selectRanked(java.util.Comparator, int)
	 * @param comparator used for comparison
	 * @param kthLowest rank of desired object according to comparison; k is based on ordinal numbers, not array indices. For min
	 *           value use 1, for max value use size of the Ordered; using 0 results in a runtime exception.
	 * @return the index of the kth lowest ranked object. */
	default int selectRankedIndex (Comparator<T> comparator, int kthLowest) {
		if (kthLowest < 1) {
			throw new RuntimeException("kthLowest must be greater than 0; 1 = first, 2 = second...");
		}
		return Select.selectIndex(order(), comparator, kthLowest, size());
	}

	/**
	 * A primitive specialization of {@link Ordered} for collections of int values instead of objects.
	 */
	interface OfInt extends Arrangeable {
		/**
		 * Gets the IntList of int items that this data structure holds, in the order it uses for iteration.
		 * This should usually return a direct reference to an IntList used inside this object, so changes
		 * to the list will affect this.
		 *
		 * @return the IntList of int items that this data structure holds
		 */
		IntList order ();

		/**
		 * Switches the ordering of positions {@code first} and {@code second}, without changing any items beyond that.
		 *
		 * @param first the first position, must not be negative and must be less than {@link #size()}
		 * @param second the second position, must not be negative and must be less than {@link #size()}
		 */
		@Override
		default void swap (int first, int second) {
			order().swap(first, second);
		}

		/**
		 * Pseudo-randomly shuffles the order of this Ordered in-place.
		 * You can seed {@code rng}, the random number generator, with an identical seed (or in {@link LaserRandom}'s
		 * case, an identical pair of seeds) to reproduce a shuffle on two Ordered with the same {@link #size()}.
		 * Using {@link LaserRandom} is recommended because it has the potential to produce all shuffles for up to 33
		 * items (though it would take millions of years to do so, and would require changing the stream every few
		 * years), while {@link java.util.Random} can only produce all shuffles for up to 16 items. The RandomXS128 class in
		 * libGDX technically can produce more possible shuffles than LaserRandom (34 items instead of 33), but is
		 * somewhat slower and is not ideal in terms of statistical bias.
		 *
		 * @param rng any {@link EnhancedRandom} implementation; you can use {@link LaserRandom} in this library
		 */
		@Override
		default void shuffle (EnhancedRandom rng) {
			IntList order = order();
			for (int i = order.size() - 1; i >= 0; i--) {
				order.swap(i, rng.nextInt(i + 1));
			}
		}

		/**
		 * Reverses the order of this Ordered in-place.
		 */
		@Override
		default void reverse () {
			order().reverse();
		}

		/**
		 * Gets a random int value from this Ordered. The random number generator {@code rng} would
		 * be well-served by a {@link LaserRandom}, because it implements a fast
		 * {@link LaserRandom#nextInt(int)} method.
		 *
		 * @param rng any {@link EnhancedRandom} implementation; you can use {@link LaserRandom} in this library
		 * @return a random int value from this Ordered.OfInt
		 */
		default int random (EnhancedRandom rng) {
			return order().random(rng);
		}

		/**
		 * Sorts this Ordered according to the order induced by the specified
		 * {@link IntComparator}.  The sort is <i>stable</i>: this method must not
		 * reorder equal elements.
		 * <br>
		 * If the specified comparator is {@code null} then the numeric elements'
		 * natural ordering should be used.
		 * @param comparator used to sort the T items this contains; may be null to use natural ordering
		 */
		default void sort(@Nullable IntComparator comparator) {
			order().sort(comparator);
		}

		/**
		 * Selects the kth-lowest element from this Ordered according to IntComparator ranking. This might partially sort the Ordered,
		 * changing its order. The Ordered must have a size greater than 0, or a {@link RuntimeException} will be thrown.
		 * @see Select
		 * @param comparator used for comparison
		 * @param kthLowest rank of desired object according to comparison; k is based on ordinal numbers, not array indices. For min
		 *           value use 1, for max value use size of the Ordered; using 0 results in a runtime exception.
		 * @return the value of the kth lowest ranked item. */
		default int selectRanked (IntComparator comparator, int kthLowest) {
			if (kthLowest < 1) {
				throw new RuntimeException("kthLowest must be greater than 0; 1 = first, 2 = second...");
			}
			return Select.select(order(), comparator, kthLowest, size());
		}

		/**
		 * Gets the index of the kth-lowest element from this Ordered according to IntComparator ranking. This might partially sort the
		 * Ordered, changing its order. The Ordered must have a size greater than 0, or a {@link RuntimeException} will be thrown.
		 * @see Ordered.OfInt#selectRanked(java.util.Comparator, int)
		 * @param comparator used for comparison
		 * @param kthLowest rank of desired object according to comparison; k is based on ordinal numbers, not array indices. For min
		 *           value use 1, for max value use size of the Ordered; using 0 results in a runtime exception.
		 * @return the index of the kth lowest ranked item. */
		default int selectRankedIndex (IntComparator comparator, int kthLowest) {
			if (kthLowest < 1) {
				throw new RuntimeException("kthLowest must be greater than 0; 1 = first, 2 = second...");
			}
			return Select.selectIndex(order(), comparator, kthLowest, size());
		}
	}

	/**
	 * A primitive specialization of {@link Ordered} for collections of long values instead of objects.
	 */
	interface OfLong extends Arrangeable {
		/**
		 * Gets the LongList of long items that this data structure holds, in the order it uses for iteration.
		 * This should usually return a direct reference to an LongList used inside this object, so changes
		 * to the list will affect this.
		 *
		 * @return the LongList of long items that this data structure holds
		 */
		LongList order ();

		/**
		 * Switches the ordering of positions {@code first} and {@code second}, without changing any items beyond that.
		 *
		 * @param first the first position, must not be negative and must be less than {@link #size()}
		 * @param second the second position, must not be negative and must be less than {@link #size()}
		 */
		@Override
		default void swap (int first, int second) {
			order().swap(first, second);
		}

		/**
		 * Pseudo-randomly shuffles the order of this Ordered in-place.
		 * You can seed {@code rng}, the random number generator, with an identical seed (or in {@link LaserRandom}'s
		 * case, an identical pair of seeds) to reproduce a shuffle on two Ordered with the same {@link #size()}.
		 * Using {@link LaserRandom} is recommended because it has the potential to produce all shuffles for up to 33
		 * items (though it would take millions of years to do so, and would require changing the stream every few
		 * years), while {@link java.util.Random} can only produce all shuffles for up to 16 items. The RandomXS128 class in
		 * libGDX technically can produce more possible shuffles than LaserRandom (34 items instead of 33), but is
		 * somewhat slower and is not ideal in terms of statistical bias.
		 *
		 * @param rng any {@link EnhancedRandom} implementation; you can use {@link LaserRandom} in this library
		 */
		@Override
		default void shuffle (EnhancedRandom rng) {
			LongList order = order();
			for (int i = order.size() - 1; i >= 0; i--) {
				order.swap(i, rng.nextInt(i + 1));
			}
		}

		/**
		 * Reverses the order of this Ordered in-place.
		 */
		@Override
		default void reverse () {
			order().reverse();
		}

		/**
		 * Gets a random long value from this Ordered. The random number generator {@code rng} would
		 * be well-served by a {@link LaserRandom}, because it implements a fast
		 * {@link LaserRandom#nextInt(int)} method.
		 *
		 * @param rng any {@link EnhancedRandom} implementation; you can use {@link LaserRandom} in this library
		 * @return a random long value from this Ordered.OfLong
		 */
		default long random (EnhancedRandom rng) {
			return order().random(rng);
		}

		/**
		 * Sorts this Ordered according to the order induced by the specified
		 * {@link LongComparator}.  The sort is <i>stable</i>: this method must not
		 * reorder equal elements.
		 * <br>
		 * If the specified comparator is {@code null} then the numeric elements'
		 * natural ordering should be used.
		 * @param comparator used to sort the T items this contains; may be null to use natural ordering
		 */
		default void sort(@Nullable LongComparator comparator) {
			order().sort(comparator);
		}

		/**
		 * Selects the kth-lowest element from this Ordered according to LongComparator ranking. This might partially sort the Ordered,
		 * changing its order. The Ordered must have a size greater than 0, or a {@link RuntimeException} will be thrown.
		 * @see Select
		 * @param comparator used for comparison
		 * @param kthLowest rank of desired object according to comparison; k is based on ordinal numbers, not array indices. For min
		 *           value use 1, for max value use size of the Ordered; using 0 results in a runtime exception.
		 * @return the value of the kth lowest ranked item. */
		default long selectRanked (LongComparator comparator, int kthLowest) {
			if (kthLowest < 1) {
				throw new RuntimeException("kthLowest must be greater than 0; 1 = first, 2 = second...");
			}
			return Select.select(order(), comparator, kthLowest, size());
		}

		/**
		 * Gets the index of the kth-lowest element from this Ordered according to LongComparator ranking. This might partially sort the
		 * Ordered, changing its order. The Ordered must have a size greater than 0, or a {@link RuntimeException} will be thrown.
		 * @see Ordered.OfLong#selectRanked(LongComparator, int)
		 * @param comparator used for comparison
		 * @param kthLowest rank of desired object according to comparison; k is based on ordinal numbers, not array indices. For min
		 *           value use 1, for max value use size of the Ordered; using 0 results in a runtime exception.
		 * @return the index of the kth lowest ranked item. */
		default int selectRankedIndex (LongComparator comparator, int kthLowest) {
			if (kthLowest < 1) {
				throw new RuntimeException("kthLowest must be greater than 0; 1 = first, 2 = second...");
			}
			return Select.selectIndex(order(), comparator, kthLowest, size());
		}
	}

	/**
	 * A primitive specialization of {@link Ordered} for collections of float values instead of objects.
	 */
	interface OfFloat extends Arrangeable {
		/**
		 * Gets the FloatList of float items that this data structure holds, in the order it uses for iteration.
		 * This should usually return a direct reference to an FloatList used inside this object, so changes
		 * to the list will affect this.
		 *
		 * @return the FloatList of float items that this data structure holds
		 */
		FloatList order ();

		/**
		 * Switches the ordering of positions {@code first} and {@code second}, without changing any items beyond that.
		 *
		 * @param first the first position, must not be negative and must be less than {@link #size()}
		 * @param second the second position, must not be negative and must be less than {@link #size()}
		 */
		@Override
		default void swap (int first, int second) {
			order().swap(first, second);
		}

		/**
		 * Pseudo-randomly shuffles the order of this Ordered in-place.
		 * You can seed {@code rng}, the random number generator, with an identical seed (or in {@link LaserRandom}'s
		 * case, an identical pair of seeds) to reproduce a shuffle on two Ordered with the same {@link #size()}.
		 * Using {@link LaserRandom} is recommended because it has the potential to produce all shuffles for up to 33
		 * items (though it would take millions of years to do so, and would require changing the stream every few
		 * years), while {@link java.util.Random} can only produce all shuffles for up to 16 items. The RandomXS128 class in
		 * libGDX technically can produce more possible shuffles than LaserRandom (34 items instead of 33), but is
		 * somewhat slower and is not ideal in terms of statistical bias.
		 *
		 * @param rng any {@link EnhancedRandom} implementation; you can use {@link LaserRandom} in this library
		 */
		@Override
		default void shuffle (EnhancedRandom rng) {
			FloatList order = order();
			for (int i = order.size() - 1; i >= 0; i--) {
				order.swap(i, rng.nextInt(i + 1));
			}
		}

		/**
		 * Reverses the order of this Ordered in-place.
		 */
		@Override
		default void reverse () {
			order().reverse();
		}

		/**
		 * Gets a random float value from this Ordered. The random number generator {@code rng} would
		 * be well-served by a {@link LaserRandom}, because it implements a fast
		 * {@link LaserRandom#nextInt(int)} method.
		 *
		 * @param rng any {@link EnhancedRandom} implementation; you can use {@link LaserRandom} in this library
		 * @return a random float value from this Ordered.OfFloat
		 */
		default float random (EnhancedRandom rng) {
			return order().random(rng);
		}
		/**
		 * Sorts this Ordered according to the order induced by the specified
		 * {@link FloatComparator}.  The sort is <i>stable</i>: this method must not
		 * reorder equal elements.
		 * <br>
		 * If the specified comparator is {@code null} then the numeric elements'
		 * natural ordering should be used.
		 * @param comparator used to sort the T items this contains; may be null to use natural ordering
		 */
		default void sort(@Nullable FloatComparator comparator) {
			order().sort(comparator);
		}

		/**
		 * Selects the kth-lowest element from this Ordered according to FloatComparator ranking. This might partially sort the Ordered,
		 * changing its order. The Ordered must have a size greater than 0, or a {@link RuntimeException} will be thrown.
		 * @see Select
		 * @param comparator used for comparison
		 * @param kthLowest rank of desired object according to comparison; k is based on ordinal numbers, not array indices. For min
		 *           value use 1, for max value use size of the Ordered; using 0 results in a runtime exception.
		 * @return the value of the kth lowest ranked item. */
		default float selectRanked (FloatComparator comparator, int kthLowest) {
			if (kthLowest < 1) {
				throw new RuntimeException("kthLowest must be greater than 0; 1 = first, 2 = second...");
			}
			return Select.select(order(), comparator, kthLowest, size());
		}

		/**
		 * Gets the index of the kth-lowest element from this Ordered according to FloatComparator ranking. This might partially sort the
		 * Ordered, changing its order. The Ordered must have a size greater than 0, or a {@link RuntimeException} will be thrown.
		 * @see Ordered.OfFloat#selectRanked(FloatComparator, int)
		 * @param comparator used for comparison
		 * @param kthLowest rank of desired object according to comparison; k is based on ordinal numbers, not array indices. For min
		 *           value use 1, for max value use size of the Ordered; using 0 results in a runtime exception.
		 * @return the index of the kth lowest ranked item.
		 */
		default int selectRankedIndex (FloatComparator comparator, int kthLowest) {
			if (kthLowest < 1) {
				throw new RuntimeException("kthLowest must be greater than 0; 1 = first, 2 = second...");
			}
			return Select.selectIndex(order(), comparator, kthLowest, size());
		}
	}


	/**
	 * A primitive specialization of {@link Ordered} for collections of double values instead of objects.
	 */
	interface OfDouble extends Arrangeable {
		/**
		 * Gets the DoubleList of double items that this data structure holds, in the order it uses for iteration.
		 * This should usually return a direct reference to an DoubleList used inside this object, so changes
		 * to the list will affect this.
		 *
		 * @return the DoubleList of double items that this data structure holds
		 */
		DoubleList order ();

		/**
		 * Switches the ordering of positions {@code first} and {@code second}, without changing any items beyond that.
		 *
		 * @param first the first position, must not be negative and must be less than {@link #size()}
		 * @param second the second position, must not be negative and must be less than {@link #size()}
		 */
		@Override
		default void swap (int first, int second) {
			order().swap(first, second);
		}

		/**
		 * Pseudo-randomly shuffles the order of this Ordered in-place.
		 * You can seed {@code rng}, the random number generator, with an identical seed (or in {@link LaserRandom}'s
		 * case, an identical pair of seeds) to reproduce a shuffle on two Ordered with the same {@link #size()}.
		 * Using {@link LaserRandom} is recommended because it has the potential to produce all shuffles for up to 33
		 * items (though it would take millions of years to do so, and would require changing the stream every few
		 * years), while {@link java.util.Random} can only produce all shuffles for up to 16 items. The RandomXS128 class in
		 * libGDX technically can produce more possible shuffles than LaserRandom (34 items instead of 33), but is
		 * somewhat slower and is not ideal in terms of statistical bias.
		 *
		 * @param rng any {@link EnhancedRandom} implementation; you can use {@link LaserRandom} in this library
		 */
		@Override
		default void shuffle (EnhancedRandom rng) {
			DoubleList order = order();
			for (int i = order.size() - 1; i >= 0; i--) {
				order.swap(i, rng.nextInt(i + 1));
			}
		}

		/**
		 * Reverses the order of this Ordered in-place.
		 */
		@Override
		default void reverse () {
			order().reverse();
		}

		/**
		 * Gets a random double value from this Ordered. The random number generator {@code rng} would
		 * be well-served by a {@link LaserRandom}, because it implements a fast
		 * {@link LaserRandom#nextInt(int)} method.
		 *
		 * @param rng any {@link EnhancedRandom} implementation; you can use {@link LaserRandom} in this library
		 * @return a random double value from this Ordered.OfDouble
		 */
		default double random (EnhancedRandom rng) {
			return order().random(rng);
		}

		/**
		 * Sorts this Ordered according to the order induced by the specified
		 * {@link DoubleComparator}.  The sort is <i>stable</i>: this method must not
		 * reorder equal elements.
		 * <br>
		 * If the specified comparator is {@code null} then the numeric elements'
		 * natural ordering should be used.
		 * @param comparator used to sort the T items this contains; may be null to use natural ordering
		 */
		default void sort(@Nullable DoubleComparator comparator) {
			order().sort(comparator);
		}

		/**
		 * Selects the kth-lowest element from this Ordered according to DoubleComparator ranking. This might partially sort the Ordered,
		 * changing its order. The Ordered must have a size greater than 0, or a {@link RuntimeException} will be thrown.
		 * @see Select
		 * @param comparator used for comparison
		 * @param kthLowest rank of desired object according to comparison; k is based on ordinal numbers, not array indices. For min
		 *           value use 1, for max value use size of the Ordered; using 0 results in a runtime exception.
		 * @return the value of the kth lowest ranked item. */
		default double selectRanked (DoubleComparator comparator, int kthLowest) {
			if (kthLowest < 1) {
				throw new RuntimeException("kthLowest must be greater than 0; 1 = first, 2 = second...");
			}
			return Select.select(order(), comparator, kthLowest, size());
		}

		/**
		 * Gets the index of the kth-lowest element from this Ordered according to DoubleComparator ranking. This might partially sort the
		 * Ordered, changing its order. The Ordered must have a size greater than 0, or a {@link RuntimeException} will be thrown.
		 * @see Ordered.OfDouble#selectRanked(DoubleComparator, int)
		 * @param comparator used for comparison
		 * @param kthLowest rank of desired object according to comparison; k is based on ordinal numbers, not array indices. For min
		 *           value use 1, for max value use size of the Ordered; using 0 results in a runtime exception.
		 * @return the index of the kth lowest ranked item. */
		default int selectRankedIndex (DoubleComparator comparator, int kthLowest) {
			if (kthLowest < 1) {
				throw new RuntimeException("kthLowest must be greater than 0; 1 = first, 2 = second...");
			}
			return Select.selectIndex(order(), comparator, kthLowest, size());
		}
	}

	interface OfShort extends Arrangeable {
		/**
		 * Gets the ShortList of short items that this data structure holds, in the order it uses for iteration.
		 * This should usually return a direct reference to an ShortList used inside this object, so changes
		 * to the list will affect this.
		 *
		 * @return the ShortList of short items that this data structure holds
		 */
		ShortList order ();

		/**
		 * Switches the ordering of positions {@code first} and {@code second}, without changing any items beyond that.
		 *
		 * @param first the first position, must not be negative and must be less than {@link #size()}
		 * @param second the second position, must not be negative and must be less than {@link #size()}
		 */
		@Override
		default void swap (int first, int second) {
			order().swap(first, second);
		}

		/**
		 * Pseudo-randomly shuffles the order of this Ordered in-place.
		 * You can seed {@code rng}, the random number generator, with an identical seed (or in {@link LaserRandom}'s
		 * case, an identical pair of seeds) to reproduce a shuffle on two Ordered with the same {@link #size()}.
		 * Using {@link LaserRandom} is recommended because it has the potential to produce all shuffles for up to 33
		 * items (though it would take millions of years to do so, and would require changing the stream every few
		 * years), while {@link java.util.Random} can only produce all shuffles for up to 16 items. The RandomXS128 class in
		 * libGDX technically can produce more possible shuffles than LaserRandom (34 items instead of 33), but is
		 * somewhat slower and is not ideal in terms of statistical bias.
		 *
		 * @param rng any {@link EnhancedRandom} implementation; you can use {@link LaserRandom} in this library
		 */
		@Override
		default void shuffle (EnhancedRandom rng) {
			ShortList order = order();
			for (int i = order.size() - 1; i >= 0; i--) {
				order.swap(i, rng.nextInt(i + 1));
			}
		}

		/**
		 * Reverses the order of this Ordered in-place.
		 */
		@Override
		default void reverse () {
			order().reverse();
		}

		/**
		 * Gets a random short value from this Ordered. The random number generator {@code rng} would
		 * be well-served by a {@link LaserRandom}, because it implements a fast
		 * {@link LaserRandom#nextInt(int)} method.
		 *
		 * @param rng any {@link EnhancedRandom} implementation; you can use {@link LaserRandom} in this library
		 * @return a random short value from this Ordered.OfShort
		 */
		default short random (EnhancedRandom rng) {
			return order().random(rng);
		}

		/**
		 * Sorts this Ordered according to the order induced by the specified
		 * {@link ShortComparator}.  The sort is <i>stable</i>: this method must not
		 * reorder equal elements.
		 * <br>
		 * If the specified comparator is {@code null} then the numeric elements'
		 * natural ordering should be used.
		 * @param comparator used to sort the T items this contains; may be null to use natural ordering
		 */
		default void sort(@Nullable ShortComparator comparator) {
			order().sort(comparator);
		}

		/**
		 * Selects the kth-lowest element from this Ordered according to ShortComparator ranking. This might partially sort the Ordered,
		 * changing its order. The Ordered must have a size greater than 0, or a {@link RuntimeException} will be thrown.
		 * @see Select
		 * @param comparator used for comparison
		 * @param kthLowest rank of desired object according to comparison; k is based on ordinal numbers, not array indices. For min
		 *           value use 1, for max value use size of the Ordered; using 0 results in a runtime exception.
		 * @return the value of the kth lowest ranked item. */
		default short selectRanked (ShortComparator comparator, int kthLowest) {
			if (kthLowest < 1) {
				throw new RuntimeException("kthLowest must be greater than 0; 1 = first, 2 = second...");
			}
			return Select.select(order(), comparator, kthLowest, size());
		}

		/**
		 * Gets the index of the kth-lowest element from this Ordered according to ShortComparator ranking. This might partially sort the
		 * Ordered, changing its order. The Ordered must have a size greater than 0, or a {@link RuntimeException} will be thrown.
		 * @see Ordered.OfShort#selectRanked(ShortComparator, int)
		 * @param comparator used for comparison
		 * @param kthLowest rank of desired object according to comparison; k is based on ordinal numbers, not array indices. For min
		 *           value use 1, for max value use size of the Ordered; using 0 results in a runtime exception.
		 * @return the index of the kth lowest ranked item. */
		default int selectRankedIndex (ShortComparator comparator, int kthLowest) {
			if (kthLowest < 1) {
				throw new RuntimeException("kthLowest must be greater than 0; 1 = first, 2 = second...");
			}
			return Select.selectIndex(order(), comparator, kthLowest, size());
		}
	}

	interface OfByte extends Arrangeable {
		/**
		 * Gets the ByteList of byte items that this data structure holds, in the order it uses for iteration.
		 * This should usually return a direct reference to an ByteList used inside this object, so changes
		 * to the list will affect this.
		 *
		 * @return the ByteList of byte items that this data structure holds
		 */
		ByteList order ();

		/**
		 * Switches the ordering of positions {@code first} and {@code second}, without changing any items beyond that.
		 *
		 * @param first the first position, must not be negative and must be less than {@link #size()}
		 * @param second the second position, must not be negative and must be less than {@link #size()}
		 */
		@Override
		default void swap (int first, int second) {
			order().swap(first, second);
		}

		/**
		 * Pseudo-randomly shuffles the order of this Ordered in-place.
		 * You can seed {@code rng}, the random number generator, with an identical seed (or in {@link LaserRandom}'s
		 * case, an identical pair of seeds) to reproduce a shuffle on two Ordered with the same {@link #size()}.
		 * Using {@link LaserRandom} is recommended because it has the potential to produce all shuffles for up to 33
		 * items (though it would take millions of years to do so, and would require changing the stream every few
		 * years), while {@link java.util.Random} can only produce all shuffles for up to 16 items. The RandomXS128 class in
		 * libGDX technically can produce more possible shuffles than LaserRandom (34 items instead of 33), but is
		 * somewhat slower and is not ideal in terms of statistical bias.
		 *
		 * @param rng any {@link EnhancedRandom} implementation; you can use {@link LaserRandom} in this library
		 */
		@Override
		default void shuffle (EnhancedRandom rng) {
			ByteList order = order();
			for (int i = order.size() - 1; i >= 0; i--) {
				order.swap(i, rng.nextInt(i + 1));
			}
		}

		/**
		 * Reverses the order of this Ordered in-place.
		 */
		@Override
		default void reverse () {
			order().reverse();
		}

		/**
		 * Gets a random byte value from this Ordered. The random number generator {@code rng} would
		 * be well-served by a {@link LaserRandom}, because it implements a fast
		 * {@link LaserRandom#nextInt(int)} method.
		 *
		 * @param rng any {@link EnhancedRandom} implementation; you can use {@link LaserRandom} in this library
		 * @return a random byte value from this Ordered.OfByte
		 */
		default byte random (EnhancedRandom rng) {
			return order().random(rng);
		}

		/**
		 * Sorts this Ordered according to the order induced by the specified
		 * {@link ByteComparator}.  The sort is <i>stable</i>: this method must not
		 * reorder equal elements.
		 * <br>
		 * If the specified comparator is {@code null} then the numeric elements'
		 * natural ordering should be used.
		 * @param comparator used to sort the T items this contains; may be null to use natural ordering
		 */
		default void sort(@Nullable ByteComparator comparator) {
			order().sort(comparator);
		}

		/**
		 * Selects the kth-lowest element from this Ordered according to ByteComparator ranking. This might partially sort the Ordered,
		 * changing its order. The Ordered must have a size greater than 0, or a {@link RuntimeException} will be thrown.
		 * @see Select
		 * @param comparator used for comparison
		 * @param kthLowest rank of desired object according to comparison; k is based on ordinal numbers, not array indices. For min
		 *           value use 1, for max value use size of the Ordered; using 0 results in a runtime exception.
		 * @return the value of the kth lowest ranked item. */
		default byte selectRanked (ByteComparator comparator, int kthLowest) {
			if (kthLowest < 1) {
				throw new RuntimeException("kthLowest must be greater than 0; 1 = first, 2 = second...");
			}
			return Select.select(order(), comparator, kthLowest, size());
		}

		/**
		 * Gets the index of the kth-lowest element from this Ordered according to ByteComparator ranking. This might partially sort the
		 * Ordered, changing its order. The Ordered must have a size greater than 0, or a {@link RuntimeException} will be thrown.
		 * @see Ordered.OfByte#selectRanked(ByteComparator, int)
		 * @param comparator used for comparison
		 * @param kthLowest rank of desired object according to comparison; k is based on ordinal numbers, not array indices. For min
		 *           value use 1, for max value use size of the Ordered; using 0 results in a runtime exception.
		 * @return the index of the kth lowest ranked item. */
		default int selectRankedIndex (ByteComparator comparator, int kthLowest) {
			if (kthLowest < 1) {
				throw new RuntimeException("kthLowest must be greater than 0; 1 = first, 2 = second...");
			}
			return Select.selectIndex(order(), comparator, kthLowest, size());
		}
	}

	interface OfChar extends Arrangeable {
		/**
		 * Gets the CharList of char items that this data structure holds, in the order it uses for iteration.
		 * This should usually return a direct reference to an CharList used inside this object, so changes
		 * to the list will affect this.
		 *
		 * @return the CharList of char items that this data structure holds
		 */
		CharList order ();

		/**
		 * Switches the ordering of positions {@code first} and {@code second}, without changing any items beyond that.
		 *
		 * @param first the first position, must not be negative and must be less than {@link #size()}
		 * @param second the second position, must not be negative and must be less than {@link #size()}
		 */
		@Override
		default void swap (int first, int second) {
			order().swap(first, second);
		}

		/**
		 * Pseudo-randomly shuffles the order of this Ordered in-place.
		 * You can seed {@code rng}, the random number generator, with an identical seed (or in {@link LaserRandom}'s
		 * case, an identical pair of seeds) to reproduce a shuffle on two Ordered with the same {@link #size()}.
		 * Using {@link LaserRandom} is recommended because it has the potential to produce all shuffles for up to 33
		 * items (though it would take millions of years to do so, and would require changing the stream every few
		 * years), while {@link java.util.Random} can only produce all shuffles for up to 16 items. The RandomXS128 class in
		 * libGDX technically can produce more possible shuffles than LaserRandom (34 items instead of 33), but is
		 * somewhat slower and is not ideal in terms of statistical bias.
		 *
		 * @param rng any {@link EnhancedRandom} implementation; you can use {@link LaserRandom} in this library
		 */
		@Override
		default void shuffle (EnhancedRandom rng) {
			CharList order = order();
			for (int i = order.size() - 1; i >= 0; i--) {
				order.swap(i, rng.nextInt(i + 1));
			}
		}

		/**
		 * Reverses the order of this Ordered in-place.
		 */
		@Override
		default void reverse () {
			order().reverse();
		}

		/**
		 * Gets a random char value from this Ordered. The random number generator {@code rng} would
		 * be well-served by a {@link LaserRandom}, because it implements a fast
		 * {@link LaserRandom#nextInt(int)} method.
		 *
		 * @param rng any {@link EnhancedRandom} implementation; you can use {@link LaserRandom} in this library
		 * @return a random char value from this Ordered.OfChar
		 */
		default char random (EnhancedRandom rng) {
			return order().random(rng);
		}

		/**
		 * Sorts this Ordered according to the order induced by the specified
		 * {@link CharComparator}.  The sort is <i>stable</i>: this method must not
		 * reorder equal elements.
		 * <br>
		 * If the specified comparator is {@code null} then the numeric elements'
		 * natural ordering should be used.
		 * @param comparator used to sort the T items this contains; may be null to use natural ordering
		 */
		default void sort(@Nullable CharComparator comparator) {
			order().sort(comparator);
		}

		/**
		 * Selects the kth-lowest element from this Ordered according to CharComparator ranking. This might partially sort the Ordered,
		 * changing its order. The Ordered must have a size greater than 0, or a {@link RuntimeException} will be thrown.
		 * @see Select
		 * @param comparator used for comparison
		 * @param kthLowest rank of desired object according to comparison; k is based on ordinal numbers, not array indices. For min
		 *           value use 1, for max value use size of the Ordered; using 0 results in a runtime exception.
		 * @return the value of the kth lowest ranked item. */
		default char selectRanked (CharComparator comparator, int kthLowest) {
			if (kthLowest < 1) {
				throw new RuntimeException("kthLowest must be greater than 0; 1 = first, 2 = second...");
			}
			return Select.select(order(), comparator, kthLowest, size());
		}

		/**
		 * Gets the index of the kth-lowest element from this Ordered according to CharComparator ranking. This might partially sort the
		 * Ordered, changing its order. The Ordered must have a size greater than 0, or a {@link RuntimeException} will be thrown.
		 * @see Ordered.OfChar#selectRanked(CharComparator, int)
		 * @param comparator used for comparison
		 * @param kthLowest rank of desired object according to comparison; k is based on ordinal numbers, not array indices. For min
		 *           value use 1, for max value use size of the Ordered; using 0 results in a runtime exception.
		 * @return the index of the kth lowest ranked item. */
		default int selectRankedIndex (CharComparator comparator, int kthLowest) {
			if (kthLowest < 1) {
				throw new RuntimeException("kthLowest must be greater than 0; 1 = first, 2 = second...");
			}
			return Select.selectIndex(order(), comparator, kthLowest, size());
		}
	}

	interface OfBoolean extends Arrangeable {
		/**
		 * Gets the BooleanList of boolean items that this data structure holds, in the order it uses for iteration.
		 * This should usually return a direct reference to an BooleanList used inside this object, so changes
		 * to the list will affect this.
		 *
		 * @return the BooleanList of boolean items that this data structure holds
		 */
		BooleanList order ();

		/**
		 * Switches the ordering of positions {@code first} and {@code second}, without changing any items beyond that.
		 *
		 * @param first the first position, must not be negative and must be less than {@link #size()}
		 * @param second the second position, must not be negative and must be less than {@link #size()}
		 */
		@Override
		default void swap (int first, int second) {
			order().swap(first, second);
		}

		/**
		 * Pseudo-randomly shuffles the order of this Ordered in-place.
		 * You can seed {@code rng}, the random number generator, with an identical seed (or in {@link LaserRandom}'s
		 * case, an identical pair of seeds) to reproduce a shuffle on two Ordered with the same {@link #size()}.
		 * Using {@link LaserRandom} is recommended because it has the potential to produce all shuffles for up to 33
		 * items (though it would take millions of years to do so, and would require changing the stream every few
		 * years), while {@link java.util.Random} can only produce all shuffles for up to 16 items. The RandomXS128 class in
		 * libGDX technically can produce more possible shuffles than LaserRandom (34 items instead of 33), but is
		 * somewhat slower and is not ideal in terms of statistical bias.
		 *
		 * @param rng any {@link EnhancedRandom} implementation; you can use {@link LaserRandom} in this library
		 */
		@Override
		default void shuffle (EnhancedRandom rng) {
			BooleanList order = order();
			for (int i = order.size() - 1; i >= 0; i--) {
				order.swap(i, rng.nextInt(i + 1));
			}
		}

		/**
		 * Reverses the order of this Ordered in-place.
		 */
		@Override
		default void reverse () {
			order().reverse();
		}

		/**
		 * Gets a random boolean value from this Ordered. The random number generator {@code rng} would
		 * be well-served by a {@link LaserRandom}, because it implements a fast
		 * {@link LaserRandom#nextInt(int)} method.
		 *
		 * @param rng any {@link EnhancedRandom} implementation; you can use {@link LaserRandom} in this library
		 * @return a random boolean value from this Ordered.OfBoolean
		 */
		default boolean random (EnhancedRandom rng) {
			return order().random(rng);
		}

		/**
		 * Sorts this Ordered according to the order induced by the specified
		 * {@link BooleanComparator}.  The sort is <i>stable</i>: this method must not
		 * reorder equal elements.
		 * <br>
		 * If the specified comparator is {@code null} then the numeric elements'
		 * natural ordering should be used.
		 * @param comparator used to sort the T items this contains; may be null to use natural ordering
		 */
		default void sort(@Nullable BooleanComparator comparator) {
			order().sort(comparator);
		}

		/**
		 * Selects the kth-lowest element from this Ordered according to BooleanComparator ranking. This might partially sort the Ordered,
		 * changing its order. The Ordered must have a size greater than 0, or a {@link RuntimeException} will be thrown.
		 * @see Select
		 * @param comparator used for comparison
		 * @param kthLowest rank of desired object according to comparison; k is based on ordinal numbers, not array indices. For min
		 *           value use 1, for max value use size of the Ordered; using 0 results in a runtime exception.
		 * @return the value of the kth lowest ranked item. */
		default boolean selectRanked (BooleanComparator comparator, int kthLowest) {
			if (kthLowest < 1) {
				throw new RuntimeException("kthLowest must be greater than 0; 1 = first, 2 = second...");
			}
			return Select.select(order(), comparator, kthLowest, size());
		}

		/**
		 * Gets the index of the kth-lowest element from this Ordered according to BooleanComparator ranking. This might partially sort the
		 * Ordered, changing its order. The Ordered must have a size greater than 0, or a {@link RuntimeException} will be thrown.
		 * @see Ordered.OfBoolean#selectRanked(BooleanComparator, int)
		 * @param comparator used for comparison
		 * @param kthLowest rank of desired object according to comparison; k is based on ordinal numbers, not array indices. For min
		 *           value use 1, for max value use size of the Ordered; using 0 results in a runtime exception.
		 * @return the index of the kth lowest ranked item. */
		default int selectRankedIndex (BooleanComparator comparator, int kthLowest) {
			if (kthLowest < 1) {
				throw new RuntimeException("kthLowest must be greater than 0; 1 = first, 2 = second...");
			}
			return Select.selectIndex(order(), comparator, kthLowest, size());
		}
	}
}

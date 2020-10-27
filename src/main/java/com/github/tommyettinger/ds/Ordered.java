package com.github.tommyettinger.ds;

import com.github.tommyettinger.ds.support.LaserRandom;

import java.util.Collections;
import java.util.Random;

/**
 * Ensures that implementors allow access to the order of {@code T} items as an ObjectList.
 * This is meant to allow different (typically insertion-ordered) data structures to all have their order
 * manipulated by the same methods. This interface extends {@link Arrangeable}, which itself is compatible
 * both with primitive-backed collections like {@link IntList} and generic ones like the implementations of
 * Ordered. This has default implementations of {@link Arrangeable#swap(int, int)} and {@link Arrangeable#shuffle(Random)}.
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
	 * Switches the ordering of positions {@code a} and {@code b}, without changing any items beyond that.
	 *
	 * @param a the first position
	 * @param b the second position
	 */
	@Override
	default void swap (int a, int b) {
		ObjectList<T> order = order();
		order.set(a, order.set(b, order.get(a)));
	}

	/**
	 * Pseudo-randomly shuffles the order of this Ordered in-place.
	 *
	 * @param random any {@link Random} implementation; prefer {@link LaserRandom} in this library
	 */
	@Override
	default void shuffle (Random random) {
		ObjectList<T> order = order();
		for (int i = order.size() - 1; i >= 0; i--) {
			order.set(i, order.set(random.nextInt(i + 1), order.get(i)));
		}
	}

	@Override
	default void reverse () {
		Collections.reverse(order());
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
		 * Switches the ordering of positions {@code a} and {@code b}, without changing any items beyond that.
		 *
		 * @param a the first position
		 * @param b the second position
		 */
		@Override
		default void swap (int a, int b) {
			order().swap(a, b);
		}

		/**
		 * Pseudo-randomly shuffles the order of this Ordered in-place.
		 *
		 * @param random any {@link Random} implementation; prefer {@link LaserRandom} in this library
		 */
		@Override
		default void shuffle (Random random) {
			IntList order = order();
			for (int i = order.size() - 1; i >= 0; i--) {
				order.swap(i, random.nextInt(i + 1));
			}
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
		 * Switches the ordering of positions {@code a} and {@code b}, without changing any items beyond that.
		 *
		 * @param a the first position
		 * @param b the second position
		 */
		@Override
		default void swap (int a, int b) {
			order().swap(a, b);
		}

		/**
		 * Pseudo-randomly shuffles the order of this Ordered in-place.
		 *
		 * @param random any {@link Random} implementation; prefer {@link LaserRandom} in this library
		 */
		@Override
		default void shuffle (Random random) {
			LongList order = order();
			for (int i = order.size() - 1; i >= 0; i--) {
				order.swap(i, random.nextInt(i + 1));
			}
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
		 * Switches the ordering of positions {@code a} and {@code b}, without changing any items beyond that.
		 *
		 * @param a the first position
		 * @param b the second position
		 */
		@Override
		default void swap (int a, int b) {
			order().swap(a, b);
		}

		/**
		 * Pseudo-randomly shuffles the order of this Ordered in-place.
		 *
		 * @param random any {@link Random} implementation; prefer {@link LaserRandom} in this library
		 */
		@Override
		default void shuffle (Random random) {
			FloatList order = order();
			for (int i = order.size() - 1; i >= 0; i--) {
				order.swap(i, random.nextInt(i + 1));
			}
		}
	}
}

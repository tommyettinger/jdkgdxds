package com.github.tommyettinger.ds;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Random;

/**
 * Ensures that implementors allow access to the order of {@code T} items as an ArrayList.
 * This is meant to allow different (typically insertion-ordered) data structures to all have their order
 * manipulated by the same methods. This interface extends {@link Arrangeable}, which itself is compatible
 * both with primitive-backed collections like {@link IntList} and generic ones like the implementations of
 * Ordered. This has default implementations of {@link Arrangeable#swap(int, int)} and {@link Arrangeable#shuffle(Random)}.
 */
public interface Ordered<T> extends Arrangeable {
	/**
	 * Gets the ArrayList of T items that this data structure holds, in the order it uses for iteration.
	 * This should usually return a direct reference to an ArrayList used inside this object, so changes
	 * to the list will affect this.
	 * @return the ArrayList of T items that this data structure holds
	 */
	ArrayList<T> order ();

	/**
	 * Switches the ordering of positions {@code a} and {@code b}, without changing any items beyond that.
	 *
	 * @param a the first position
	 * @param b the second position
	 */
	@Override
	default void swap (int a, int b){
		ArrayList<T> order = order();
		order.set(a, order.set(b, order.get(a)));
	}

	/**
	 * Pseudo-randomly shuffles the order of this Ordered in-place.
	 * @param random any {@link Random} implementation; prefer {@link LaserRandom} in this library
	 */
	@Override
	default void shuffle (@NotNull Random random){
		ArrayList<T> order = order();
		for (int i = order.size() - 1; i >= 0; i--) {
			order.set(i, order.set(random.nextInt(i+1), order.get(i)));
		}
	}
}

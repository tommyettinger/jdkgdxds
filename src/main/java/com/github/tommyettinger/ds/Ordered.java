package com.github.tommyettinger.ds;

import java.util.ArrayList;

/**
 * Ensures that implementors allow access to the order of {@code T} items as an ArrayList.
 * This is meant to allow different (typically insertion-ordered) data structures to all have their order
 * manipulated by the same methods.
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
	 * Pseudo-randomly shuffles the order of this Arrangeable in-place.
	 * <br>
	 * This implementation is currently kinda bad and uses Math.random(); it will likely change.
	 */
	@Override
	default void shuffle (){
		ArrayList<T> order = order();
		for (int i = order.size() - 1; i >= 0; i--) {
			int j = (int)(Math.random() * (i+1));
			order.set(i, order.set(j, order.get(i)));
		}
	}
}

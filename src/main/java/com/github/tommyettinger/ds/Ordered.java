package com.github.tommyettinger.ds;

import java.util.ArrayList;

/**
 * Ensures that implementors allow access to the order of {@code T} items as an ArrayList.
 * This is meant to allow different (typically insertion-ordered) data structures to all have their order
 * manipulated by the same methods.
 */
public interface Ordered<T> {
	/**
	 * Gets the ArrayList of T items that this data structure holds, in the order it uses for iteration.
	 * This should usually return a direct reference to an ArrayList used inside this object, so changes
	 * to the list will affect this.
	 * @return the ArrayList of T items that this data structure holds
	 */
	ArrayList<T> order ();
}

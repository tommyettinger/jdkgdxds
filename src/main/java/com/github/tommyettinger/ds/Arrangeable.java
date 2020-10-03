package com.github.tommyettinger.ds;

import java.util.Random;

/**
 * Indicates that a type can have its contents change position, without specifying the type of contents.
 * This can be used for primitive-backed collections as well as generic ones.
 */
public interface Arrangeable {
	/**
	 * Switches the ordering of positions {@code a} and {@code b}, without changing any items beyond that.
	 * @param a the first position
	 * @param b the second position
	 */
	void swap(int a, int b);

	/**
	 * Pseudo-randomly shuffles the order of this Arrangeable in-place.
	 * @param random any {@link Random} implementation; prefer {@link LaserRandom} in this library
	 */
	void shuffle(Random random);
	
	void reverse();
}

package com.github.tommyettinger.ds;

import com.github.tommyettinger.ds.support.LaserRandom;

import java.util.Collection;
import com.github.tommyettinger.ds.support.EnhancedRandom;

/**
 * Indicates that a type can have its contents change position, without specifying the type of contents.
 * This can be used for primitive-backed collections as well as generic ones.
 *
 * @author Tommy Ettinger
 */
public interface Arrangeable {
	/**
	 * Switches the ordering of positions {@code first} and {@code second}, without changing any items beyond that.
	 *
	 * @param first the first position, must not be negative and must be less than {@link #size()}
	 * @param second the second position, must not be negative and must be less than {@link #size()}
	 */
	void swap (int first, int second);

	/**
	 * Pseudo-randomly shuffles the order of this Arrangeable in-place.
	 *
	 * @param random any {@link EnhancedRandom} implementation; you can use {@link LaserRandom} in this library
	 */
	void shuffle (EnhancedRandom random);

	/**
	 * Reverses this Arrangeable in-place.
	 */
	void reverse ();

	/**
	 * Returns the number of elements in this Arrangeable.
	 * Often this is shared with {@link Collection#size()}, but isn't always.
	 *
	 * @return the number of elements in this Arrangeable
	 */
	int size ();
}

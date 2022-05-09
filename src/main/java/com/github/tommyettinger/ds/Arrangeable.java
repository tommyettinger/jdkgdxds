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

import com.github.tommyettinger.ds.support.EnhancedRandom;
import com.github.tommyettinger.ds.support.LaserRandom;

import java.util.Collection;

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
	 * @param first  the first position, must not be negative and must be less than {@link #size()}
	 * @param second the second position, must not be negative and must be less than {@link #size()}
	 */
	void swap (int first, int second);

	/**
	 * Pseudo-randomly shuffles the order of this Arrangeable in-place.
	 *
	 * @param random any {@link EnhancedRandom} implementation; e.g. you can use {@link LaserRandom} in this library
	 */
	default void shuffle (EnhancedRandom random) {
		for (int i = size() - 1; i >= 0; i--) {
			swap(i, random.nextInt(i + 1));
		}
	}

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

	/**
	 * Rearranges this Arrangeable using the given {@link EnhancedRandom} to shuffle it, then tries to restore the prior
	 * state of the EnhancedRandom so it can be used to reorder other Arrangeables. The attempt to restore state can
	 * fail if {@link EnhancedRandom#getStateCount()} returns 0, meaning the state is not accessible for that
	 * EnhancedRandom implementation, so this just shuffles without restoring the state afterwards in that case. If
	 * {@link EnhancedRandom#getStateCount()} is 5 or less, this will not allocate, but if it is 6 or more, then this
	 * has to allocate a temporary array. The rearrangement is done in-place.
	 *
	 * @param random a non-null EnhancedRandom, ideally one where {@link EnhancedRandom#getStateCount()} is between 1 and 5, inclusive
	 */
	default void rearrange (EnhancedRandom random) {
		final int c = random.getStateCount();
		switch (c) {
		case 0: {
			random.shuffle(this);
			break;
		}
		case 1: {
			long s0 = random.getSelectedState(0);
			random.shuffle(this);
			random.setState(s0);
			break;
		}
		case 2: {
			long s0 = random.getSelectedState(0), s1 = random.getSelectedState(1);
			random.shuffle(this);
			random.setState(s0, s1);
			break;
		}
		case 3: {
			long s0 = random.getSelectedState(0), s1 = random.getSelectedState(1), s2 = random.getSelectedState(2);
			random.shuffle(this);
			random.setState(s0, s1, s2);
			break;
		}
		case 4: {
			long s0 = random.getSelectedState(0), s1 = random.getSelectedState(1), s2 = random.getSelectedState(2), s3 = random.getSelectedState(3);
			random.shuffle(this);
			random.setState(s0, s1, s2, s3);
			break;
		}
		case 5: {
			long s0 = random.getSelectedState(0), s1 = random.getSelectedState(1), s2 = random.getSelectedState(2), s3 = random.getSelectedState(3), s4 = random.getSelectedState(4);
			random.shuffle(this);
			random.setSelectedState(0, s0);
			random.setSelectedState(1, s1);
			random.setSelectedState(2, s2);
			random.setSelectedState(3, s3);
			random.setSelectedState(4, s4);
			break;
		}
		default: {
			final long[] states = new long[c];
			for (int i = 0; i < c; i++) {
				states[i] = random.getSelectedState(i);
			}
			random.shuffle(this);
			random.setState(states);
		}
		}
	}

}

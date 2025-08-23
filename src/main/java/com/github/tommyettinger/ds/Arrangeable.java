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

import com.github.tommyettinger.digital.ArrayTools;
import com.github.tommyettinger.digital.Hasher;

import java.util.Collection;
import java.util.List;
import java.util.Random;

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
	void swap(int first, int second);

	/**
	 * Pseudo-randomly shuffles the order of this Arrangeable in-place, using {@link ArrayTools#RANDOM} as the unseeded
	 * random number generator.
 	 */
	default void shuffle() {
		shuffle(ArrayTools.RANDOM);
	}

	/**
	 * Pseudo-randomly shuffles the order of this Arrangeable in-place.
	 *
	 * @param random any {@link Random}, such as {@link com.github.tommyettinger.digital.AlternateRandom} or one from juniper
	 */
	default void shuffle(Random random) {
		for (int i = size() - 1; i > 0; i--) swap(i, random.nextInt(i + 1));
	}

	/**
	 * Reverses this Arrangeable in-place.
	 */
	void reverse();

	/**
	 * Returns the number of elements in this Arrangeable.
	 * Often this is shared with {@link Collection#size()}, but isn't always.
	 *
	 * @return the number of elements in this Arrangeable
	 */
	int size();

	/**
	 * Rearranges this Arrangeable using the given seed to shuffle it. The rearrangement is done in-place.
	 * You can reuse the seed on different calls to rearrange with Arrangeables of the same size, which will
	 * perform the same reordering steps on each Arrangeable (so if the first item in one Arrangeable became
	 * the fifth item after a call to rearrange(), reusing the seed in another same-size Arrangeable would
	 * also move its first item to become its fifth item).
	 * <br>
	 * If you don't need to reuse a seed, you should consider {@link #shuffle(Random)}, which can be faster
	 * than this with a good enough Random subclass (likely not with {@link Random}).
	 * <br>
	 * This uses {@link Hasher#randomize2Bounded(long, int)} to randomize the swap positions. If you want to
	 * use a stronger randomization method, such as {@link Hasher#randomize3Bounded(long, int)}, use a faster
	 * one, such as {@link Hasher#randomize1Bounded(long, int)}, or just to inline this method manually, the
	 * code used here fits in one line:
	 * <br>
	 * {@code for (int i = size() - 1; i > 0; i--) swap(i, Hasher.randomize2Bounded(++seed, i + 1));}
	 *
	 * @param seed a (typically random) long seed to determine the shuffled order
	 */
	default void rearrange(long seed) {
		for (int i = size() - 1; i > 0; i--) swap(i, Hasher.randomize2Bounded(++seed, i + 1));
	}

	/**
	 * An empty interface that merges Arrangeable and java.util.List APIs.
	 * This is only really meant to make {@link Select} and {@link QuickSelect} able to take more List types
	 * that also support the necessary {@link #swap(int, int)} method.
	 *
	 * @param <T> the type of items this ArrangeableList contains
	 */
	interface ArrangeableList<T> extends Arrangeable, List<T> {

	}
}

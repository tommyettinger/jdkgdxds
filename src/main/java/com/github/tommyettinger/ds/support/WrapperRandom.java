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

package com.github.tommyettinger.ds.support;

import javax.annotation.Nonnull;
import java.util.Random;

/**
 * If, for whatever reason, you need a {@link Random} and all you have is an {@link EnhancedRandom} from this library,
 * you can give that EnhancedRandom to {@link #WrapperRandom(EnhancedRandom)} and then use this as a replacement for
 * java.util.Random.
 */
public class WrapperRandom extends Random {
	/**
	 * The EnhancedRandom this uses for almost all of its methods (not Streams).
	 */
	@Nonnull
	public EnhancedRandom rng;

	/**
	 * Creates a WrapperRandom around a {@link FourWheelRandom} with a random seed.
	 */
	public WrapperRandom () {
		this(EnhancedRandom.seedFromMath());
	}

	/**
	 * Uses the given EnhancedRandom for all operations it can (everything but Streams), and seeds the inherited Random state
	 * with the given EnhancedRandom's first state.
	 * @param rng an EnhancedRandom that will be referenced directly (not copied)
	 */
	public WrapperRandom (@Nonnull EnhancedRandom rng) {
		super();
		this.rng = rng;
		super.setSeed(rng.getSelectedState(0));
	}

	/**
	 * Uses the given EnhancedRandom for all operations it can (everything but Streams), and seeds both that EnhancedRandom
	 * and the inherited Random state with the given seed.
	 * @param rng an EnhancedRandom that will be referenced directly (not copied) and will be seeded with {@code seed}
	 * @param seed a long seed (which can be any long) that will be used with {@link EnhancedRandom#setSeed(long)}
	 */
	public WrapperRandom (@Nonnull EnhancedRandom rng, long seed) {
		super();
		this.rng = rng;
		setSeed(seed);
	}

	/**
	 * Creates a WrapperRandom around a {@link FourWheelRandom}, seeded with the given seed.
	 * @param seed a long to be used as a seed for a FourWheelRandom
	 */
	public WrapperRandom (long seed) {
		super();
		this.rng = new FourWheelRandom(seed);
		setSeed(seed);
	}

	@Override
	public void setSeed (long seed) {
		super.setSeed(seed);
		if(rng != null) // needed because superclass Random calls this during construction
			rng.setSeed(seed);
	}

	@Override
	protected int next (int bits) {
		return rng.next(bits);
	}

	@Override
	public int nextInt () {
		return rng.nextInt();
	}

	@Override
	public long nextLong () {
		return rng.nextLong();
	}

	@Override
	public boolean nextBoolean () {
		return rng.nextBoolean();
	}

	@Override
	public float nextFloat () {
		return rng.nextFloat();
	}

	@Override
	public double nextDouble () {
		return rng.nextDouble();
	}

	@Override
	public boolean equals (Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		WrapperRandom that = (WrapperRandom)o;

		return rng.equals(that.rng);
	}

	@Override
	public int hashCode () {
		return rng.hashCode();
	}

	@Override
	public String toString () {
		return "[WrapperRandom:" + rng + ']';
	}
}

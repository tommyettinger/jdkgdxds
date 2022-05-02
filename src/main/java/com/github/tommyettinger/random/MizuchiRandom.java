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

package com.github.tommyettinger.random;

import com.github.tommyettinger.digital.Base;

/**
 * A relatively-simple RNG that's similar to {@link LaserRandom} with less correlation between similar initial states,
 * but without the ability to {@link #skip(long)}. It has two {@code long} states, one of which changes with every
 * generated value and one always-odd state which never changes (the "stream"). This uses a linear congruential
 * generator for its changing state (the state changes by multiplying with a large constant and adding the stream), and
 * feeds the resulting value to a small, simple unary hash to get a more-random result.
 * <br>
 * This always has a period of 2 to the 64, and there are 2 to the 63 possible sequences that result from changing the
 * stream value. MizuchiRandom implements all optional methods in EnhancedRandom except
 * {@link #skip(long)}; it does implement {@link #previousLong()} without using skip().
 * <br>
 * MizuchiRandom passes 64TB of testing with PractRand, which uses a suite of tests to look for a variety of potential
 * problems. It has not been tested with hwd or remortality. All the generators here are considered stable.
 * <br>
 * The name comes from combining the concept of a dragon, with streams. A mythological theme was carried throughout some
 * generators that I designed and that were designed by others, such as Fortuna. Mizuchi allows many possible streams, so
 * the mizuchi, a (by some versions of the story) river dragon from Japanese mythology, seemed fitting.
 * <br>
 * This is present here for two reasons. First, it can be used in cases where similar initial states are expected to be
 * given to a two-state generator like LaserRandom (visible patterns are obviously correlated with LaserRandom but are
 * not at all correlated with MizuchiRandom). Second, MizuchiRandom is often the fastest 64-bit generator available in
 * the closely-related C# library ShaiRandom, and for compatibility purposes it makes sense to support this in both.
 * This generator is not especially fast here compared to LaserRandom, especially on OpenJ9, nor is it fast compared to
 * FourWheelRandom on HotSpot JDKs, but it does maintain its quality well.
 */
public class MizuchiRandom extends EnhancedRandom {

	/**
	 * The first state, also called the changing state; can be any long.
	 */
	protected long stateA;
	/**
	 * The second state, also called the stream; can be any odd-number long.
	 */
	protected long stateB;

	/**
	 * Creates a new MizuchiRandom with a random state.
	 */
	public MizuchiRandom () {
		stateA = EnhancedRandom.seedFromMath();
		stateB = EnhancedRandom.seedFromMath() | 1L;
	}

	/**
	 * Creates a new MizuchiRandom with the given seed; all {@code long} values are permitted.
	 * The seed will be passed to {@link #setSeed(long)} to attempt to adequately distribute the seed randomly.
	 *
	 * @param seed any {@code long} value
	 */
	public MizuchiRandom (long seed) {
		setSeed(seed);
	}

	/**
	 * Creates a new MizuchiRandom with the given two states; all {@code long} values are permitted for
	 * stateA, and all odd-number {@code long} values are permitted for stateB. These states are not
	 * changed as long as they are permitted values.
	 *
	 * @param stateA any {@code long} value
	 * @param stateB any {@code long} value; should be odd, otherwise this will add 1 to make it odd
	 */
	public MizuchiRandom (long stateA, long stateB) {
		this.stateA = stateA;
		this.stateB = stateB | 1L;
	}

	/**
	 * This generator has 2 {@code long} states, so this returns 2.
	 *
	 * @return 2 (two)
	 */
	@Override
	public int getStateCount () {
		return 2;
	}

	/**
	 * Gets the state determined by {@code selection}, as-is.
	 * Selections 0 (or any even number) and 1 (or any odd number) refer to states A and B.
	 *
	 * @param selection used to select which state variable to get; generally 0 or 1
	 * @return the value of the selected state
	 */
	@Override
	public long getSelectedState (int selection) {
		if ((selection & 1) == 1) {
			return stateB;
		}
		return stateA;
	}

	/**
	 * Sets one of the states, determined by {@code selection}, to {@code value}, as-is.
	 * Selections 0 (or any even number) and 1 (or any odd number) refer to states A and B.
	 *
	 * @param selection used to select which state variable to set; generally 0 or 1
	 * @param value     the exact value to use for the selected state, if valid
	 */
	@Override
	public void setSelectedState (int selection, long value) {
		if ((selection & 1) == 1) {
			stateB = value | 1L;
		}
		stateA = value;
	}

	/**
	 * This initializes both states of the generator to random values based on the given seed.
	 * (2 to the 64) possible initial generator states can be produced here.
	 *
	 * @param seed the initial seed; may be any long
	 */
	@Override
	public void setSeed (long seed) {
		long x = (seed += 0x9E3779B97F4A7C15L);
		x ^= x >>> 27;
		x *= 0x3C79AC492BA7B653L;
		x ^= x >>> 33;
		x *= 0x1C69B3F74AC4AE35L;
		stateA = x ^ x >>> 27;
		x = (seed + 0x9E3779B97F4A7C15L);
		x ^= x >>> 27;
		x *= 0x3C79AC492BA7B653L;
		x ^= x >>> 33;
		x *= 0x1C69B3F74AC4AE35L;
		stateB = (x ^ x >>> 27) | 1L;
	}

	public long getStateA () {
		return stateA;
	}

	/**
	 * Sets the first part of the state (the changing state).
	 *
	 * @param stateA can be any long
	 */
	public void setStateA (long stateA) {
		this.stateA = stateA;
	}

	public long getStateB () {
		return stateB;
	}

	/**
	 * Sets the second part of the state (the stream). This must be odd, otherwise this will add 1 to make it odd.
	 *
	 * @param stateB can be any odd-number long; otherwise this adds 1 to make it odd
	 */
	public void setStateB (long stateB) {
		this.stateB = stateB | 1L;
	}

	/**
	 * Sets the state completely to the given three state variables.
	 * This is the same as calling {@link #setStateA(long)} and {@link #setStateB(long)}
	 * as a group.
	 *
	 * @param stateA the first state; can be any long
	 * @param stateB the second state; can be any odd-number long
	 */
	@Override
	public void setState (long stateA, long stateB) {
		this.stateA = stateA;
		this.stateB = stateB | 1L;
	}

	@Override
	public long nextLong () {
		long z = (stateA = stateA * 0xF7C2EBC08F67F2B5L + stateB);
		z = (z ^ z >>> 23 ^ z >>> 47) * 0xAEF17502108EF2D9L;
		return (z ^ z >>> 25);
	}

	@Override
	public long previousLong () {
		long z = (stateA = (stateA - stateB) * 0x09795DFF8024EB9DL);
		z = (z ^ z >>> 23 ^ z >>> 47) * 0xAEF17502108EF2D9L;
		return z ^ z >>> 25;

	}

	@Override
	public int next (int bits) {
		long z = (stateA = stateA * 0xF7C2EBC08F67F2B5L + stateB);
		z = (z ^ z >>> 23 ^ z >>> 47) * 0xAEF17502108EF2D9L;
		return (int)(z ^ z >>> 25) >>> (32 - bits);
	}

	@Override
	public MizuchiRandom copy () {
		return new MizuchiRandom(stateA, stateB);
	}

	@Override
	public boolean equals (Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		MizuchiRandom that = (MizuchiRandom)o;

		if (stateA != that.stateA)
			return false;
		return stateB == that.stateB;
	}

	public String toString () {
		return "MizuchiRandom{" + "stateA=0x" + Base.BASE16.unsigned(stateA) + "L, stateB=0x" + Base.BASE16.unsigned(stateB) + "L}";
	}
}

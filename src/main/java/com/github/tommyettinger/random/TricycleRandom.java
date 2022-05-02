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
 * An unusual RNG that's extremely fast on HotSpot JDK 16 and higher, and still fairly fast on earlier JDKs. It has
 * three {@code long} states, which as far as I can tell can be initialized to any values without hitting any known
 * problems for initialization. These states, a, b, and c, are passed around so a is determined by the previous c, b is
 * determined by the previous a, b, and c, and c is determined by the previous b. This updates a with a multiplication,
 * b with two XOR operations, and c with a bitwise-left-rotate by 41 and then an addition with a constant. If you want
 * to alter this generator so results will be harder to reproduce, the simplest way is to change the constant added to
 * c -- it can be any substantially-large odd number, though preferably one with a {@link Long#bitCount(long)} of 32.
 * <br>
 * Other useful traits of this generator are that it almost certainly has a longer period than you need for a game, and
 * that all values are permitted for the states (that we know of). It is possible that some initialization will put the
 * generator in a shorter-period subcycle, but the odds of this being a subcycle that's small enough to run out of
 * period during a game are effectively 0. It's also possible that the generator only has one cycle of length 2 to the
 * 192, though this doesn't seem at all likely. TricycleRandom implements all optional methods in EnhancedRandom except
 * {@link #skip(long)}; it does implement {@link #previousLong()} without using skip().
 * <br>
 * This is closely related to Mark Overton's <a href="https://www.romu-random.org/">Romu generators</a>, specifically
 * RomuTrio, but this gets a little faster than RomuTrio in some situations by using just one less rotation. Unlike
 * RomuTrio, there isn't a clear problematic state with a period of 1 (which happens when all of its states are 0).
 * This is often slightly slower than RomuTrio, but only by a tiny margin. This generator isn't an ARX generator any
 * more (a previous version was), but its performance isn't much different (like RomuTrio, the one multiplication this
 * uses pipelines very well, so it doesn't slow down the generator).
 * <br>
 * TricycleRandom passes 64TB of testing with PractRand, which uses a suite of tests to look for a variety of potential
 * problems. It has also passed a whopping 4 petabytes of testing with hwd, can test a much larger amount of data but
 * only runs a single test. The test hwd uses looks for long-range bit-dependencies, where one bit's state earlier in
 * the generated numbers determines the state of a future bit with a higher-than-reasonable likelihood. All the
 * generators here are considered stable.
 * <br>
 * It is strongly recommended that you seed this with {@link #setSeed(long)} instead of
 * {@link #setState(long, long, long)}, because if you give sequential seeds to both setSeed() and setState(), the
 * former will start off random, while the latter will start off repeating the seed sequence. After about 20-40 random
 * numbers generated, any correlation between similarly seeded generators will probably be completely gone, though.
 */
public class TricycleRandom extends EnhancedRandom {

	/**
	 * The first state; can be any long. If this has just been set to some value, then the next call to
	 * {@link #nextLong()} will return that value as-is. Later calls will be more random.
	 */
	protected long stateA;
	/**
	 * The second state; can be any long.
	 */
	protected long stateB;
	/**
	 * The third state; can be any long.
	 */
	protected long stateC;

	/**
	 * Creates a new TricycleRandom with a random state.
	 */
	public TricycleRandom () {
		stateA = EnhancedRandom.seedFromMath();
		stateB = EnhancedRandom.seedFromMath();
		stateC = EnhancedRandom.seedFromMath();
	}

	/**
	 * Creates a new TricycleRandom with the given seed; all {@code long} values are permitted.
	 * The seed will be passed to {@link #setSeed(long)} to attempt to adequately distribute the seed randomly.
	 *
	 * @param seed any {@code long} value
	 */
	public TricycleRandom (long seed) {
		setSeed(seed);
	}

	/**
	 * Creates a new TricycleRandom with the given three states; all {@code long} values are permitted.
	 * These states will be used verbatim.
	 *
	 * @param stateA any {@code long} value
	 * @param stateB any {@code long} value
	 * @param stateC any {@code long} value
	 */
	public TricycleRandom (long stateA, long stateB, long stateC) {
		this.stateA = stateA;
		this.stateB = stateB;
		this.stateC = stateC;
	}

	/**
	 * This generator has 3 {@code long} states, so this returns 3.
	 *
	 * @return 3 (three)
	 */
	@Override
	public int getStateCount () {
		return 3;
	}

	/**
	 * Gets the state determined by {@code selection}, as-is.
	 *
	 * @param selection used to select which state variable to get; generally 0, 1, or 2
	 * @return the value of the selected state
	 */
	@Override
	public long getSelectedState (int selection) {
		switch (selection & 3) {
		case 0:
			return stateA;
		case 1:
			return stateB;
		default:
			return stateC;
		}
	}

	/**
	 * Sets one of the states, determined by {@code selection}, to {@code value}, as-is.
	 * Selections 0, 1, and 2 refer to states A, B, and C, and if the selection is anything
	 * else, this treats it as 2 and sets stateC.
	 *
	 * @param selection used to select which state variable to set; generally 0, 1, or 2
	 * @param value     the exact value to use for the selected state, if valid
	 */
	@Override
	public void setSelectedState (int selection, long value) {
		switch (selection & 3) {
		case 0:
			stateA = value;
			break;
		case 1:
			stateB = value;
			break;
		default:
			stateC = value;
			break;
		}
	}

	/**
	 * This initializes all 3 states of the generator to random values based on the given seed.
	 * (2 to the 64) possible initial generator states can be produced here, all with a different
	 * first value returned by {@link #nextLong()} (because {@code stateA} is guaranteed to be
	 * different for every different {@code seed}).
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
		x = (seed += 0x9E3779B97F4A7C15L);
		x ^= x >>> 27;
		x *= 0x3C79AC492BA7B653L;
		x ^= x >>> 33;
		x *= 0x1C69B3F74AC4AE35L;
		stateB = x ^ x >>> 27;
		x = (seed + 0x9E3779B97F4A7C15L);
		x ^= x >>> 27;
		x *= 0x3C79AC492BA7B653L;
		x ^= x >>> 33;
		x *= 0x1C69B3F74AC4AE35L;
		stateC = x ^ x >>> 27;
	}

	public long getStateA () {
		return stateA;
	}

	/**
	 * Sets the first part of the state. Note that if you call {@link #nextLong()}
	 * immediately after this, it will return the given {@code stateA} as-is, so you
	 * may want to call some random generation methods (such as nextLong()) and discard
	 * the results after setting the state.
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
	 * Sets the second part of the state.
	 *
	 * @param stateB can be any long
	 */
	public void setStateB (long stateB) {
		this.stateB = stateB;
	}

	public long getStateC () {
		return stateC;
	}

	/**
	 * Sets the third part of the state.
	 *
	 * @param stateC can be any long
	 */
	public void setStateC (long stateC) {
		this.stateC = stateC;
	}

	/**
	 * Sets the state completely to the given three state variables.
	 * This is the same as calling {@link #setStateA(long)}, {@link #setStateB(long)},
	 * and {@link #setStateC(long)} as a group. You may want to call {@link #nextLong()}
	 * a few times after setting the states like this, unless the value for stateA (in
	 * particular) is already adequately random; the first call to {@link #nextLong()},
	 * if it is made immediately after calling this, will return {@code stateA} as-is.
	 *
	 * @param stateA the first state; this will be returned as-is if the next call is to {@link #nextLong()}
	 * @param stateB the second state; can be any long
	 * @param stateC the third state; can be any long
	 */
	@Override
	public void setState (long stateA, long stateB, long stateC) {
		this.stateA = stateA;
		this.stateB = stateB;
		this.stateC = stateC;
	}

	@Override
	public long nextLong () {
		final long fa = stateA;
		final long fb = stateB;
		final long fc = stateC;
		stateA = 0xD1342543DE82EF95L * fc;
		stateB = fa ^ fb ^ fc;
		stateC = (fb << 41 | fb >>> 23) + 0xC6BC279692B5C323L;
		return fa;
	}

	@Override
	public long previousLong () {
		final long fa = stateA;
		final long fb = stateB;
		long fc = stateC - 0xC6BC279692B5C323L;
		stateC = 0x572B5EE77A54E3BDL * fa;
		stateB = (fc >>> 41 | fc << 23);
		stateA = fb ^ stateB ^ stateC;
		fc = stateC - 0xC6BC279692B5C323L;
		return stateB ^ 0x572B5EE77A54E3BDL * stateA ^ (fc >>> 41 | fc << 23);

	}

	@Override
	public int next (int bits) {
		final long fa = stateA;
		final long fb = stateB;
		final long fc = stateC;
		stateA = 0xD1342543DE82EF95L * fc;
		stateB = fa ^ fb ^ fc;
		stateC = (fb << 41 | fb >>> 23) + 0xC6BC279692B5C323L;
		return (int)fa >>> (32 - bits);
	}

	@Override
	public TricycleRandom copy () {
		return new TricycleRandom(stateA, stateB, stateC);
	}

	@Override
	public boolean equals (Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		TricycleRandom that = (TricycleRandom)o;

		if (stateA != that.stateA)
			return false;
		if (stateB != that.stateB)
			return false;
		return stateC == that.stateC;
	}

	public String toString () {
		return "TricycleRandom{" + "stateA=0x" + Base.BASE16.unsigned(stateA) + "L, stateB=0x" + Base.BASE16.unsigned(stateB) + "L, stateC=0x" + Base.BASE16.unsigned(stateC) + "L}";
	}
}

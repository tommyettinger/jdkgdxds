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

/**
 * A random number generator that is very fast on Java 16+, has both a very large probable period and a large guaranteed
 * minimum period, and uses only add, bitwise-rotate, and XOR operations (no multiplication). This generator is not quite
 * as fast as {@link FourWheelRandom} on machines that can multiply {@code long} values efficiently, but is faster than
 * just about everything else (except {@link TricycleRandom} and {@link DistinctRandom} on Java 8 with HotSpot, or
 * DistinctRandom on any OpenJ9 version). If this algorithm is run on a GPU, on most hardware it will be significantly
 * faster than FourWheelRandom (indeed, it was faster than any other algorithm I tested on a low-end GPU).
 * <br>
 * This can now be considered stable, like the other EnhancedRandom implementations here. Testing performed should be
 * sufficient, but more can always be done; this passes at least 64TB of PractRand without issues, and passes a much more
 * rigorous single test ("remortality," which measures how often the bitwise AND/bitwise OR of sequential numbers become
 * all 0 bits or all 1 bits) through over 150 PB. The test in question runs on the GPU using CUDA, so was able to generate
 * far more numbers in a timeframe of days than most CPU approaches could. Earlier versions of remortality incorrectly
 * measured byte length and reported a higher size, so reports of 1 exabyte by earlier versions are roughly equivalent to
 * 150 petabytes now. This is still a tremendous amount of data. This was changed a few times; when the algorithm could be
 * strengthened, I took the chance to do so.
 * <br>
 * The algorithm used here has four states purely to exploit instruction-level parallelism; one state is a counter (this
 * gives the guaranteed minimum period of 2 to the 64), and the others combine the values of the four states across three
 * variables. There's a complex tangle of dependencies across the states, but it is possible to invert the generator
 * given a full 256-bit state; this is vital for its period and quality.
 * <br>
 * It is strongly recommended that you seed this with {@link #setSeed(long)} instead of
 * {@link #setState(long, long, long, long)}, because if you give sequential seeds to both setSeed() and setState(), the
 * former will start off random, while the latter will start off repeating the seed sequence. After about 20-40 random
 * numbers generated, any correlation between similarly seeded generators will probably be completely gone, though.
 * <br>
 * This implements all optional methods in EnhancedRandom except {@link #skip(long)}; it does implement
 * {@link #previousLong()} without using skip().
 * <br>
 * This is called TrimRandom because it uses a trimmed-down set of operations, purely "ARX" -- add, rotate, XOR.
 */
public class TrimRandom implements EnhancedRandom {

	/**
	 * The first state; can be any long.
	 */
	protected long stateA;
	/**
	 * The second state; can be any long.
	 */
	protected long stateB;
	/**
	 * The third state; can be any long. If this has just been set to some value, then the next call to
	 * {@link #nextLong()} will return that value as-is. Later calls will be more random.
	 */
	protected long stateC;
	/**
	 * The fourth state; can be any long.
	 */
	protected long stateD;

	/**
	 * Creates a new TrimRandom with a random state.
	 */
	public TrimRandom () {
		stateA = EnhancedRandom.seedFromMath();
		stateB = EnhancedRandom.seedFromMath();
		stateC = EnhancedRandom.seedFromMath();
		stateD = EnhancedRandom.seedFromMath();
	}

	/**
	 * Creates a new TrimRandom with the given seed; all {@code long} values are permitted.
	 * The seed will be passed to {@link #setSeed(long)} to attempt to adequately distribute the seed randomly.
	 *
	 * @param seed any {@code long} value
	 */
	public TrimRandom (long seed) {
		setSeed(seed);
	}

	/**
	 * Creates a new TrimRandom with the given four states; all {@code long} values are permitted.
	 * These states will be used verbatim.
	 *
	 * @param stateA any {@code long} value
	 * @param stateB any {@code long} value
	 * @param stateC any {@code long} value; will be returned exactly on the first call to {@link #nextLong()}
	 * @param stateD any {@code long} value
	 */
	public TrimRandom (long stateA, long stateB, long stateC, long stateD) {
		this.stateA = stateA;
		this.stateB = stateB;
		this.stateC = stateC;
		this.stateD = stateD;
	}

	/**
	 * This generator has 4 {@code long} states, so this returns 4.
	 *
	 * @return 4 (four)
	 */
	@Override
	public int getStateCount () {
		return 4;
	}

	/**
	 * Gets the state determined by {@code selection}, as-is. The value for selection should be
	 * between 0 and 3, inclusive; if it is any other value this gets state D as if 3 was given.
	 *
	 * @param selection used to select which state variable to get; generally 0, 1, 2, or 3
	 * @return the value of the selected state
	 */
	@Override
	public long getSelectedState (int selection) {
		switch (selection) {
		case 0:
			return stateA;
		case 1:
			return stateB;
		case 2:
			return stateC;
		default:
			return stateD;
		}
	}

	/**
	 * Sets one of the states, determined by {@code selection}, to {@code value}, as-is.
	 * Selections 0, 1, 2, and 3 refer to states A, B, C, and D,  and if the selection is anything
	 * else, this treats it as 3 and sets stateD.
	 *
	 * @param selection used to select which state variable to set; generally 0, 1, 2, or 3
	 * @param value     the exact value to use for the selected state, if valid
	 */
	@Override
	public void setSelectedState (int selection, long value) {
		switch (selection) {
		case 0:
			stateA = value;
			break;
		case 1:
			stateB = value;
			break;
		case 2:
			stateC = value;
			break;
		default:
			stateD = value;
			break;
		}
	}

	/**
	 * This initializes all 4 states of the generator to random values based on the given seed.
	 * (2 to the 64) possible initial generator states can be produced here, all with a different
	 * first value returned by {@link #nextLong()} (because {@code stateC} is guaranteed to be
	 * different for every different {@code seed}).
	 *
	 * @param seed the initial seed; may be any long
	 */
	@Override
	public void setSeed (long seed) {
		stateA = seed ^ 0xC6BC279692B5C323L;
		stateB = ~seed;
		stateC = seed ^ ~0xC6BC279692B5C323L;
		stateD = seed;
	}
	{
}
	public long getStateA () {
		return stateA;
	}

	/**
	 * Sets the first part of the state.
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
	 * Sets the third part of the state. Note that if you call {@link #nextLong()}
	 * immediately after this, it will return the given {@code stateC} as-is, so you
	 * may want to call some random generation methods (such as nextLong()) and discard
	 * the results after setting the state.
	 *
	 * @param stateC can be any long
	 */
	public void setStateC (long stateC) {
		this.stateC = stateC;
	}

	public long getStateD () {
		return stateD;
	}

	/**
	 * Sets the fourth part of the state.
	 *
	 * @param stateD can be any long
	 */
	public void setStateD (long stateD) {
		this.stateD = stateD;
	}

	/**
	 * Sets the state completely to the given four state variables.
	 * This is the same as calling {@link #setStateA(long)}, {@link #setStateB(long)},
	 * {@link #setStateC(long)}, and {@link #setStateD(long)} as a group. You may want
	 * to call {@link #nextLong()} a few times after setting the states like this, unless
	 * the value for stateC (in particular) is already adequately random; the first call
	 * to {@link #nextLong()}, if it is made immediately after calling this, will return {@code stateC} as-is.
	 *
	 * @param stateA the first state; can be any long
	 * @param stateB the second state; can be any long
	 * @param stateC the third state; this will be returned as-is if the next call is to {@link #nextLong()}
	 * @param stateD the fourth state; can be any long
	 */
	@Override
	public void setState (long stateA, long stateB, long stateC, long stateD) {
		this.stateA = stateA;
		this.stateB = stateB;
		this.stateC = stateC;
		this.stateD = stateD;
	}

	@Override
	public long nextLong () {
		final long fa = stateA;
		final long fb = stateB;
		final long fc = stateC;
		final long fd = stateD;
		final long bc = fb ^ fc;
		final long cd = fc ^ fd;
		stateA = (bc << 57 | bc >>> 7);
		stateB = (cd << 18 | cd >>> 46);
		stateC = fa + bc;
		stateD = fd + 0xDE916ABCC965815BL;
		return fc;
	}

	@Override
	public long previousLong () {
		final long fa = stateA;
		final long fb = stateB;
		final long fc = stateC;
		stateD -= 0xDE916ABCC965815BL;
		long t = (fb >>> 18 | fb << 46);
		stateC = t ^ stateD;
		t = (fa >>> 57 | fa << 7);
		stateB = t ^ stateC;
		stateA = fc - t;
		return (stateB >>> 18 | stateB << 46) ^ stateD - 0xDE916ABCC965815BL;
	}

	@Override
	public int next (int bits) {
		final long fa = stateA;
		final long fb = stateB;
		final long fc = stateC;
		final long fd = stateD;
		final long bc = fb ^ fc;
		final long cd = fc ^ fd;
		stateA = (bc << 57 | bc >>> 7);
		stateB = (cd << 18 | cd >>> 46);
		stateC = fa + bc;
		stateD = fd + 0xDE916ABCC965815BL;
		return (int)fc >>> (32 - bits);
	}

	@Override
	public TrimRandom copy () {
		return new TrimRandom(stateA, stateB, stateC, stateD);
	}

	@Override
	public boolean equals (Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		TrimRandom that = (TrimRandom)o;

		return stateA == that.stateA && stateB == that.stateB && stateC == that.stateC && stateD == that.stateD;
	}

	public String toString () {
		return "TrimRandom{" + "stateA=0x" + Base.BASE16.unsigned(stateA) + "L, stateB=0x" + Base.BASE16.unsigned(stateB) + "L, stateC=0x" + Base.BASE16.unsigned(stateC) + "L, stateD=0x" + Base.BASE16.unsigned(stateD) + "L}";
	}
}

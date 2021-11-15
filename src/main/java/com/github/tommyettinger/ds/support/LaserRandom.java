/*******************************************************************************
 * Copyright 2021 See AUTHORS file.
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
 ******************************************************************************/

package com.github.tommyettinger.ds.support;

import java.util.Random;

/**
 * A faster and much-higher-quality substitute for {@link Random}. This allows many different random number
 * streams that don't overlap, and offers a more substantial API for commonly-used functions.
 * <br>
 * This fills in much of the functionality of MathUtils in libGDX, though with all code as instance methods
 * instead of static methods, and some things renamed (randomTriangular() became {@link #nextTriangular()},
 * for instance, and random() became {@link #nextFloat()}). It also supplies some rare and sometimes-useful
 * code: {@link #skip(long)} allows "fast-forward" and "rewind" along with {@link #previousLong()} to simply
 * go back one step, you can get and set the exact state with {@link #getStateA()}, {@link #getStateB()},
 * {@link #setStateA(long)}, {@link #setStateB(long)}, and {@link #setState(long, long)} (which is useful
 * if you want to save a LaserRandom and reload it later), and there's bounded int and long generators which
 * can use a negative number as their exclusive outer bound ({@link #nextSignedInt(int)} and
 * {@link #nextSignedLong(long)}, plus overloads that take an inner bound). There's float and double
 * generators that are inclusive on both ends ({@link #nextInclusiveFloat()}, and
 * {@link #nextInclusiveDouble()}. There's {@link #nextGaussian()}, which is implemented differently from
 * java.util.Random and always advances the state once. This implements all optional methods in
 * EnhancedRandom, and implements almost all EnhancedRandom methods explicitly, which allows LaserRandom to
 * be copied more easily without depending on jdkgdxds (see below).
 * <br>
 * Every method defined in this class advances the state by the same amount unless otherwise documented (only
 * {@link #nextTriangular()} and {@link #nextTriangular(float)} advance the state twice). The state can
 * advance 2 to the 64 times before the sequence of random numbers repeats, which would take a few years of
 * continuous generation. There are also 2 to the 63 possible sequences this can produce; you can tell which
 * one you're using with {@link #getStream()}. Note, {@link Random} can only advance 2 to the 48 times, which
 * takes under half a day to make it repeat on recent laptop hardware while also analyzing the numbers for
 * statistical issues. This generator is more comparable to SplittableRandom, introduced in JDK 8 but not
 * available in Android (even with desugaring) or GWT currently. SplittableRandom also can produce 2 to the
 * 64 numbers before repeating the sequence, and also has 2 to the 63 streams, but it will always produce
 * each possible long value exactly once over the course of that sequence. Each of LaserRandom's streams
 * produces a different sequence of numbers with a different set of numbers it omits and a different set of
 * numbers it produces more than once; each of SplittableRandom's streams simply rearranges the order of all
 * possible longs. Though it might seem like an issue that a LaserRandom stream has gaps in its possible
 * output, if you appended all 2 to the 63 possible LaserRandom streams in full, the gargantuan result would
 * include all longs equally often. So, if the stream is selected effectively at random, then the subset of
 * that stream that actually gets used should be fair (and it's very unlikely that any usage will need a full
 * stream of over 18 quintillion pseudo-random longs). It is strongly recommended that you use very different
 * numbers when creating many LaserRandom objects with similar states, because there is a noticeable
 * correlation between, for instance, a grid of LaserRandom objects initialized with stateA drawn from the
 * odd numbers 1 through 101, and stateB drawn from another odd number 1 through 101. Using
 * {@link #setSeed(long)} essentially eliminates this risk, so it's a good idea to seed this with one long.
 * <br>
 * If statistical quality is a concern, don't use {@link Random}, since the aforementioned
 * analysis finds statistical failures in about a minute when checking about 16GB of output; this class can
 * produce 64TB of random output without a tool like PractRand finding any failures (sometimes it can't find
 * any minor anomaly over several days of testing). RandomXS128 has some flaws, though they are not nearly as
 * severe as Random's; mostly they are limited to a particular kind of failure affecting the least
 * significant bits (the technical name for the test it fails is a "binary matrix rank" test, which a wide
 * variety of related generators can fail if they don't adequately randomize their outputs). RandomXS128's
 * flaws would be permissible if it was faster than any competitors, but it isn't, and there have been two
 * improved relatives of its algorithm published since it was created. Both of these improvements,
 * xoroshiro128** and xoshiro256**, are slower when implemented in Java than LaserRandom (also when all are
 * implemented in C and compiled with GCC or Clang, typically). There are also some concerns about specific
 * failure cases when the output of xoroshiro128** or xoshiro256** is multiplied by any of quadrillions of
 * constants and tested after that multiplication (see M.E. O'Neill's dissection of xoshiro256**
 * <a href="https://www.pcg-random.org/posts/a-quick-look-at-xoshiro256.html">here</a>). Xoshiro256**, like
 * LaserRandom, can't be reliably initialized using nearby values for its state variables, and does much
 * better if you use its {@link Xoshiro256StarStarRandom#setSeed(long)} method. We do implement Xoshiro256**
 * here, because it provides 4-dimensional equidistribution, and that is hard to find.
 * <br>
 * You can copy this class independently of the library it's part of; it's meant as a general replacement for
 * Random and also RandomXS128. LaserRandom is generally faster than RandomXS128, and can be over 3x faster
 * when running on OpenJ9 (generating over 3 billion random long values per second). If you copy this, the
 * only step you probably need to do is to remove {@code implements EnhancedRandom} from the class, since
 * almost all of EnhancedRandom consists of either default methods that this implements explicitly, or to
 * provide a common interface for pseudo-random number generators on the JVM. This class avoids using the
 * Override annotation specifically because copying the class and removing the EnhancedRandom implementation
 * would cause compile errors if Override annotations were present. If you do keep this class implementing
 * EnhancedRandom, then that permits some extra methods to come in via default implementations, like
 * nextExclusiveFloat() (which uses the BitConversion class here in jdkgdxds), minIntOf(), maxLongOf(), etc.
 * <br>
 * You may want to compare this class with TricycleRandom and FourWheelRandom in the same package; both of
 * those have a larger state size (and should usually have a larger period), are usually faster, and also
 * implement all of EnhancedRandom (except for {@link #skip(long)}), but they do even less randomizing for
 * the first result they return, so if the seeding has a pattern, then the start of their sequences will
 * have patterns. These patterns are less obvious but do persist in LaserRandom, and don't persist in
 * TricycleRandom or FourWheelRandom over a long run. All generators here do well when using
 * {@link #setSeed(long)} to set the full state.
 * <br>
 * Pew pew! Lasers!
 *
 * @author Tommy Ettinger
 */
public class LaserRandom extends Random implements EnhancedRandom {
	/**
	 * Can be any long value.
	 */
	protected long stateA;

	/**
	 * Must be odd.
	 */
	protected long stateB;

	/**
	 * Creates a new LaserRandom. This constructor sets the states of the
	 * random number generator to values very likely to be distinct from
	 * any other invocation of this constructor.
	 */
	public LaserRandom () {
		super();
		stateA = super.nextLong();
		stateB = super.nextLong() | 1L;
	}

	/**
	 * Creates a new LaserRandom using a single {@code long} seed; the stream depends on whether the seed is even or odd.
	 *
	 * @param seed the initial seed
	 * @see #setSeed(long)
	 */
	public LaserRandom (long seed) {
		super(seed);
		stateA = seed;
		stateB = seed | 1L;
	}

	/**
	 * Creates a new LaserRandom using {@code seedA} exactly to set stateA (as with {@link #setStateA(long)},,
	 * and using {@code seedB} to set stateB as with {@link #setStateB(long)} (meaning seedB will be used exactly if odd,
	 * otherwise it will have 1 added to it and then used).
	 *
	 * @param seedA any long; will be used exactly to set stateA as with {@link #setStateA(long)}
	 * @param seedB any odd long will be used exactly to set stateB, otherwise, as with {@link #setStateB(long)}, it will be made odd
	 */
	public LaserRandom (final long seedA, final long seedB) {
		super(seedA);
		stateA = seedA;
		stateB = seedB | 1L;
	}

	/**
	 * LaserRandom has two possible states, both {@code long}.
	 * The second state (selection {@code 1}) is always an odd number, and if
	 * anything tries to set an even number to that state, the actual state used
	 * will be one greater.
	 * @return 2 (two)
	 */
	public int getStateCount() {
		return 2;
	}
	/**
	 * Get the "A" part of the internal state as a long.
	 *
	 * @return the current internal "A" state of this object.
	 */
	public long getStateA () {
		return stateA;
	}

	/**
	 * Set the "A" part of the internal state with a long.
	 *
	 * @param stateA a 64-bit long
	 */
	public void setStateA (long stateA) {
		this.stateA = stateA;
	}

	/**
	 * Get the "B" part of the internal state as a long.
	 *
	 * @return the current internal "B" state of this object.
	 */
	public long getStateB () {
		return stateB;
	}

	/**
	 * Set the "B" part of the internal state with a long; the least significant bit is ignored (will always be odd).
	 * That is, if stateB is odd, this uses it verbatim; if stateB is even, it adds 1 to it to make it odd.
	 *
	 * @param stateB a 64-bit long; the lowest bit will be ignored and the result always used as an odd number
	 */
	public void setStateB (long stateB) {
		this.stateB = stateB | 1L;
	}

	/**
	 * Sets both parts of the internal state with one call; {@code stateA} is used verbatim, but {@code stateB} has
	 * its least significant bit ignored and always overwritten with a '1' bit (meaning stateB will always be odd).
	 * You can use any long for stateA without it being changed, and can use any odd long for stateB without it
	 * being changed; as such, keeping {@code stateB} an odd number should be optimal.
	 *
	 * @param stateA a 64-bit long
	 * @param stateB a 64-bit long; the lowest bit will be ignored and the result always used as an odd number
	 */
	public void setState (long stateA, long stateB) {
		this.stateA = stateA;
		this.stateB = stateB | 1L;
	}

	/**
	 * Gets a selected state value from this LaserRandom. If selection is an even number,
	 * this returns stateA, and if selection is odd, it returns stateB. This returns the
	 * exact value of the selected state.
	 *
	 * @param selection used to select which state variable to get (usually 0 or 1)
	 * @return the exact value of the selected state
	 */
	public long getSelectedState (int selection) {
		return (selection & 1) == 0 ? stateA : stateB;
	}

	/**
	 * Sets a selected state value to the given long {@code value}. If selection is an even
	 * number, this sets stateA to value as-is, and if selection is odd, this sets stateB to
	 * value made odd (that is, if value is even, it uses value + 1, otherwise it uses value).
	 *
	 * @param selection used to select which state variable to set (usually 0 or 1)
	 * @param value     the exact value to use for the selected state, if valid
	 */
	public void setSelectedState (int selection, long value) {
		if((selection & 1) == 0)
			stateA = value;
		else
			stateB = value | 1L;
	}

	/**
	 * Sets the seed of this random number generator using a single
	 * {@code long} seed. The general contract of {@code setSeed} is
	 * that it alters the state of this random number generator object
	 * so as to be in exactly the same state as if it had just been
	 * created with the argument {@code seed} as a seed.
	 *
	 * <p>The implementation of {@code setSeed} by class
	 * {@code LaserRandom} uses all 64 bits of the given seed for
	 * {@link #setStateA(long)}, and all but the least-significant bit
	 * of the seed for {@link #setStateB(long)} (the omitted bit is
	 * always set to 1 in stateB, meaning stateB is always odd).
	 *
	 * @param seed the initial seed
	 */
	public void setSeed (long seed) {
		stateB = (stateA = seed) | 1L;
	}

	/**
	 * Generates the next pseudorandom number with a specific maximum size in bits (not a max number).
	 * If you want to get a random number in a range, you should usually use {@link #nextInt(int)} instead.
	 * For some specific cases, this method is more efficient and less biased than {@link #nextInt(int)}.
	 * For {@code bits} values between 1 and 30, this should be similar in effect to
	 * {@code nextInt(1 << bits)}; though it won't typically produce the same values, they will have
	 * the correct range. If {@code bits} is 31, this can return any non-negative {@code int}; note that
	 * {@code nextInt(1 << 31)} won't behave this way because {@code 1 << 31} is negative. If
	 * {@code bits} is 32 (or 0), this can return any {@code int}.
	 *
	 * <p>The general contract of {@code next} is that it returns an
	 * {@code int} value and if the argument {@code bits} is between
	 * {@code 1} and {@code 32} (inclusive), then that many low-order
	 * bits of the returned value will be (approximately) independently
	 * chosen bit values, each of which is (approximately) equally
	 * likely to be {@code 0} or {@code 1}.
	 * <p>
	 *
	 * @param bits the amount of random bits to request, from 1 to 32
	 * @return the next pseudorandom value from this random number
	 * generator's sequence
	 */
	public int next (int bits) {
		final long s = stateA += 0xC6BC279692B5C323L;
		final long z = (s ^ s >>> 31) * (stateB += 0x9E3779B97F4A7C16L);
		return (int)(z ^ z >>> 26 ^ z >>> 6) >>> 32 - bits;
	}

	/**
	 * Generates random bytes and places them into a user-supplied
	 * byte array.  The number of random bytes produced is equal to
	 * the length of the byte array.
	 *
	 * @param bytes the byte array to fill with random bytes
	 * @throws NullPointerException if the byte array is null
	 */
	public void nextBytes (byte[] bytes) {
		for (int i = 0; i < bytes.length; ) { for (long r = nextLong(), n = Math.min(bytes.length - i, 8); n-- > 0; r >>>= 8) { bytes[i++] = (byte)r; } }
	}

	/**
	 * Returns the next pseudorandom, uniformly distributed {@code int}
	 * value from this random number generator's sequence. The general
	 * contract of {@code nextInt} is that one {@code int} value is
	 * pseudorandomly generated and returned. All 2<sup>32</sup> possible
	 * {@code int} values are produced with (approximately) equal probability.
	 *
	 * @return the next pseudorandom, uniformly distributed {@code int}
	 * value from this random number generator's sequence
	 */
	public int nextInt () {
		final long s = stateA += 0xC6BC279692B5C323L;
		final long z = (s ^ s >>> 31) * (stateB += 0x9E3779B97F4A7C16L);
		return (int)(z ^ z >>> 26 ^ z >>> 6);
	}

	/**
	 * Returns a pseudorandom, uniformly distributed {@code int} value
	 * between 0 (inclusive) and the specified value (exclusive), drawn from
	 * this random number generator's sequence.  The general contract of
	 * {@code nextInt} is that one {@code int} value in the specified range
	 * is pseudorandomly generated and returned.  All {@code bound} possible
	 * {@code int} values are produced with (approximately) equal
	 * probability.
	 * <br>
	 * It should be mentioned that the technique this uses has some bias, depending
	 * on {@code bound}, but it typically isn't measurable without specifically looking
	 * for it. Using the method this does allows this method to always advance the state
	 * by one step, instead of a varying and unpredictable amount with the more typical
	 * ways of rejection-sampling random numbers and only using numbers that can produce
	 * an int within the bound without bias.
	 * See <a href="https://www.pcg-random.org/posts/bounded-rands.html">M.E. O'Neill's
	 * blog about random numbers</a> for discussion of alternative, unbiased methods.
	 *
	 * @param bound the upper bound (exclusive). If negative or 0, this always returns 0.
	 * @return the next pseudorandom, uniformly distributed {@code int}
	 * value between zero (inclusive) and {@code bound} (exclusive)
	 * from this random number generator's sequence
	 */
	public int nextInt (int bound) {
		final long s = stateA += 0xC6BC279692B5C323L;
		final long z = (s ^ s >>> 31) * (stateB += 0x9E3779B97F4A7C16L);
		return (int)(bound * ((z ^ z >>> 26 ^ z >>> 6) & 0xFFFFFFFFL) >> 32) & ~(bound >> 31);
	}

	/**
	 * Returns a pseudorandom, uniformly distributed {@code int} value between an
	 * inner bound of 0 (inclusive) and the specified {@code outerBound} (exclusive).
	 * This is meant for cases where the outer bound may be negative, especially if
	 * the bound is unknown or may be user-specified. A negative outer bound is used
	 * as the lower bound; a positive outer bound is used as the upper bound. An outer
	 * bound of -1, 0, or 1 will always return 0, keeping the bound exclusive (except
	 * for outer bound 0). This method is slightly slower than {@link #nextInt(int)}.
	 *
	 * @see #nextInt(int) Here's a note about the bias present in the bounded generation.
	 * @param outerBound the outer exclusive bound; may be any int value, allowing negative
	 * @return a pseudorandom int between 0 (inclusive) and outerBound (exclusive)
	 */
	public int nextSignedInt (int outerBound) {
		final long s = stateA += 0xC6BC279692B5C323L;
		final long z = (s ^ s >>> 31) * (stateB += 0x9E3779B97F4A7C16L);
		outerBound = (int)(outerBound * ((z ^ z >>> 26 ^ z >>> 6) & 0xFFFFFFFFL) >> 32);
		return outerBound + (outerBound >>> 31);
	}

	/**
	 * Returns a pseudorandom, uniformly distributed {@code int} value between the
	 * specified {@code innerBound} (inclusive) and the specified {@code outerBound}
	 * (exclusive). If {@code outerBound} is less than or equal to {@code innerBound},
	 * this always returns {@code innerBound}. Internally, this calls
	 * {@link #nextLong(long, long)} and casts it to int (because the range between
	 * innerBound and outerBound can be greater than the largest int).
	 *
	 * <br> For any case where outerBound might be valid but less than innerBound, you
	 * can use {@link #nextSignedInt(int, int)}.
	 *
	 * @see #nextInt(int) Here's a note about the bias present in the bounded generation.
	 * @param innerBound the inclusive inner bound; may be any int, allowing negative
	 * @param outerBound the exclusive outer bound; must be greater than innerBound (otherwise this returns innerBound)
	 * @return a pseudorandom int between innerBound (inclusive) and outerBound (exclusive)
	 */
	public int nextInt (int innerBound, int outerBound) {
		return (int)nextLong(innerBound, outerBound);
	}

	/**
	 * Returns a pseudorandom, uniformly distributed {@code int} value between the
	 * specified {@code innerBound} (inclusive) and the specified {@code outerBound}
	 * (exclusive). This is meant for cases where either bound may be negative,
	 * especially if the bounds are unknown or may be user-specified. It is slightly
	 * slower than {@link #nextInt(int, int)}. Internally, this calls
	 * {@link #nextSignedLong(long, long)} and casts it to int (because the range
	 * between innerBound and outerBound can be greater than the largest int).
	 *
	 * @see #nextInt(int) Here's a note about the bias present in the bounded generation.
	 * @param innerBound the inclusive inner bound; may be any int, allowing negative
	 * @param outerBound the exclusive outer bound; may be any int, allowing negative
	 * @return a pseudorandom int between innerBound (inclusive) and outerBound (exclusive)
	 */
	public int nextSignedInt (int innerBound, int outerBound) {
		return (int)nextSignedLong(innerBound, outerBound);
	}

	/**
	 * Returns the next pseudorandom, uniformly distributed {@code long}
	 * value from this random number generator's sequence. The general
	 * contract of {@code nextLong} is that one {@code long} value is
	 * pseudorandomly generated and returned.
	 * <br>
	 * An individual {@code LaserRNG} can't return all 18-quintillion possible {@code long} values,
	 * but the full set of 9-quintillion possible random number streams that this class can produce will,
	 * as a whole, produce all {@code long} values with equal likelihood.
	 *
	 * @return the next pseudorandom, uniformly distributed {@code long}
	 * value from this random number generator's sequence
	 */
	public long nextLong () {
		final long s = stateA += 0xC6BC279692B5C323L;
		final long z = (s ^ s >>> 31) * (stateB += 0x9E3779B97F4A7C16L);
		return z ^ z >>> 26 ^ z >>> 6;
	}

	/**
	 * Returns a pseudorandom, uniformly distributed {@code long} value
	 * between 0 (inclusive) and the specified value (exclusive), drawn from
	 * this random number generator's sequence.  The general contract of
	 * {@code nextLong} is that one {@code long} value in the specified range
	 * is pseudorandomly generated and returned.  All {@code bound} possible
	 * {@code long} values are produced with (approximately) equal
	 * probability, though there is a small amount of bias depending on the bound.
	 *
	 * <br> Note that this advances the state by the same amount as a single call to
	 * {@link #nextLong()}, which allows methods like {@link #skip(long)} to function
	 * correctly, but introduces some bias when {@code bound} is very large. This will
	 * also advance the state if {@code bound} is 0 or negative, so usage with a variable
	 * bound will advance the state reliably.
	 *
	 * <br> This method has some bias, particularly on larger bounds. Actually measuring
	 * bias with bounds in the trillions or greater is challenging but not impossible, so
	 * don't use this for a real-money gambling purpose. The bias isn't especially
	 * significant, though.
	 *
	 * @see #nextInt(int) Here's a note about the bias present in the bounded generation.
	 * @param bound the upper bound (exclusive). If negative or 0, this always returns 0.
	 * @return the next pseudorandom, uniformly distributed {@code long}
	 * value between zero (inclusive) and {@code bound} (exclusive)
	 * from this random number generator's sequence
	 */
	public long nextLong (long bound) {
		return nextLong(0L, bound);
	}

	/**
	 * Returns a pseudorandom, uniformly distributed {@code long} value between an
	 * inner bound of 0 (inclusive) and the specified {@code outerBound} (exclusive).
	 * This is meant for cases where the outer bound may be negative, especially if
	 * the bound is unknown or may be user-specified. A negative outer bound is used
	 * as the lower bound; a positive outer bound is used as the upper bound. An outer
	 * bound of -1, 0, or 1 will always return 0, keeping the bound exclusive (except
	 * for outer bound 0).
	 *
	 * <p>Note that this advances the state by the same amount as a single call to
	 * {@link #nextLong()}, which allows methods like {@link #skip(long)} to function
	 * correctly, but introduces some bias when {@code bound} is very large. This
	 * method should be about as fast as {@link #nextLong(long)} , unlike the speed
	 * difference between {@link #nextInt(int)} and {@link #nextSignedInt(int)}.
	 *
	 * @see #nextInt(int) Here's a note about the bias present in the bounded generation.
	 * @param outerBound the outer exclusive bound; may be any long value, allowing negative
	 * @return a pseudorandom long between 0 (inclusive) and outerBound (exclusive)
	 */
	public long nextSignedLong (long outerBound) {
		return nextSignedLong(0L, outerBound);
	}

	/**
	 * Returns a pseudorandom, uniformly distributed {@code long} value between the
	 * specified {@code innerBound} (inclusive) and the specified {@code outerBound}
	 * (exclusive). If {@code outerBound} is less than or equal to {@code innerBound},
	 * this always returns {@code innerBound}.
	 *
	 * <br> For any case where outerBound might be valid but less than innerBound, you
	 * can use {@link #nextSignedLong(long, long)}.
	 *
	 * @see #nextInt(int) Here's a note about the bias present in the bounded generation.
	 * @param inner the inclusive inner bound; may be any long, allowing negative
	 * @param outer the exclusive outer bound; must be greater than innerBound (otherwise this returns innerBound)
	 * @return a pseudorandom long between innerBound (inclusive) and outerBound (exclusive)
	 */
	public long nextLong (long inner, long outer) {
		final long rand = nextLong();
		if(inner >= outer) return inner;
		final long bound = outer - inner;
		final long randLow = rand & 0xFFFFFFFFL;
		final long boundLow = bound & 0xFFFFFFFFL;
		final long randHigh = (rand >>> 32);
		final long boundHigh = (bound >>> 32);
		return inner + (randHigh * boundLow >>> 32) + (randLow * boundHigh >>> 32) + randHigh * boundHigh;
	}

	/**
	 * Returns a pseudorandom, uniformly distributed {@code long} value between the
	 * specified {@code innerBound} (inclusive) and the specified {@code outerBound}
	 * (exclusive). This is meant for cases where either bound may be negative,
	 * especially if the bounds are unknown or may be user-specified.
	 *
	 * @see #nextInt(int) Here's a note about the bias present in the bounded generation.
	 * @param inner the inclusive inner bound; may be any long, allowing negative
	 * @param outer the exclusive outer bound; may be any long, allowing negative
	 * @return a pseudorandom long between innerBound (inclusive) and outerBound (exclusive)
	 */
	public long nextSignedLong (long inner, long outer) {
		final long rand = nextLong();
		if(outer < inner) {
			long t = outer;
			outer = inner + 1L;
			inner = t + 1L;
		}
		final long bound = outer - inner;
		final long randLow = rand & 0xFFFFFFFFL;
		final long boundLow = bound & 0xFFFFFFFFL;
		final long randHigh = (rand >>> 32);
		final long boundHigh = (bound >>> 32);
		return inner + (randHigh * boundLow >>> 32) + (randLow * boundHigh >>> 32) + randHigh * boundHigh;
	}

	/**
	 * Returns the next pseudorandom, uniformly distributed
	 * {@code boolean} value from this random number generator's
	 * sequence. The general contract of {@code nextBoolean} is that one
	 * {@code boolean} value is pseudorandomly generated and returned.  The
	 * values {@code true} and {@code false} are produced with
	 * (approximately) equal probability.
	 *
	 * @return the next pseudorandom, uniformly distributed
	 * {@code boolean} value from this random number generator's
	 * sequence
	 */
	public boolean nextBoolean () {
		final long s = stateA += 0xC6BC279692B5C323L;
		return (s ^ s >>> 31) * (stateB += 0x9E3779B97F4A7C16L) < 0L;
	}

	/**
	 * Returns the next pseudorandom, uniformly distributed {@code float}
	 * value between {@code 0.0} (inclusive) and {@code 1.0} (exclusive)
	 * from this random number generator's sequence.
	 *
	 * <p>The general contract of {@code nextFloat} is that one
	 * {@code float} value, chosen (approximately) uniformly from the
	 * range {@code 0.0f} (inclusive) to {@code 1.0f} (exclusive), is
	 * pseudorandomly generated and returned. All 2<sup>24</sup> possible
	 * {@code float} values of the form <i>m&nbsp;x&nbsp;</i>2<sup>-24</sup>,
	 * where <i>m</i> is a positive integer less than 2<sup>24</sup>, are
	 * produced with (approximately) equal probability.
	 *
	 * <p>The hedge "approximately" is used in the foregoing description only
	 * because the next method is only approximately an unbiased source of
	 * independently chosen bits. If it were a perfect source of randomly
	 * chosen bits, then the algorithm shown would choose {@code float}
	 * values from the stated range with perfect uniformity.<p>
	 *
	 * @return the next pseudorandom, uniformly distributed {@code float}
	 * value between {@code 0.0} and {@code 1.0} from this
	 * random number generator's sequence
	 */
	public float nextFloat () {
		final long s = stateA += 0xC6BC279692B5C323L;
		final long z = (s ^ s >>> 31) * (stateB += 0x9E3779B97F4A7C16L);
		return ((z ^ z >>> 6) >>> 40) * 0x1p-24f;
	}

	/**
	 * Gets a pseudo-random float between 0 (inclusive) and {@code outerBound} (exclusive).
	 * The outerBound may be positive or negative.
	 * Exactly the same as {@code nextFloat() * outerBound}.
	 * @param outerBound the exclusive outer bound
	 * @return a float between 0 (inclusive) and {@code outerBound} (exclusive)
	 */
	public float nextFloat (float outerBound) {
		return nextFloat() * outerBound;
	}

	/**
	 * Gets a pseudo-random float between {@code innerBound} (inclusive) and {@code outerBound} (exclusive).
	 * Either, neither, or both of innerBound and outerBound may be negative; this does not change which is
	 * inclusive and which is exclusive.
	 * @param innerBound the inclusive inner bound; may be negative
	 * @param outerBound the exclusive outer bound; may be negative
	 * @return a float between {@code innerBound} (inclusive) and {@code outerBound} (exclusive)
	 */
	public float nextFloat (float innerBound, float outerBound) {
		return innerBound + nextFloat() * (outerBound - innerBound);
	}

	/**
	 * Returns the next pseudorandom, uniformly distributed
	 * {@code double} value between {@code 0.0} (inclusive) and {@code 1.0}
	 * (exclusive) from this random number generator's sequence.
	 *
	 * <p>The general contract of {@code nextDouble} is that one
	 * {@code double} value, chosen (approximately) uniformly from the
	 * range {@code 0.0d} (inclusive) to {@code 1.0d} (exclusive), is
	 * pseudorandomly generated and returned.
	 *
	 * <p>The hedge "approximately" is used in the foregoing description only
	 * because the {@code next} method is only approximately an unbiased
	 * source of independently chosen bits. If it were a perfect source of
	 * randomly chosen bits, then the algorithm shown would choose
	 * {@code double} values from the stated range with perfect uniformity.
	 *
	 * @return the next pseudorandom, uniformly distributed {@code double}
	 * value between {@code 0.0} and {@code 1.0} from this
	 * random number generator's sequence
	 */
	public double nextDouble () {
		final long s = stateA += 0xC6BC279692B5C323L;
		final long z = (s ^ s >>> 31) * (stateB += 0x9E3779B97F4A7C16L);
		return (z >>> 11 ^ z >>> 37 ^ z >>> 17) * 0x1.0p-53;
	}

	/**
	 * Gets a pseudo-random double between 0 (inclusive) and {@code outerBound} (exclusive).
	 * The outerBound may be positive or negative.
	 * Exactly the same as {@code nextDouble() * outerBound}.
	 * @param outerBound the exclusive outer bound
	 * @return a double between 0 (inclusive) and {@code outerBound} (exclusive)
	 */
	public double nextDouble (double outerBound) {
		return nextDouble() * outerBound;
	}

	/**
	 * Gets a pseudo-random double between {@code innerBound} (inclusive) and {@code outerBound} (exclusive).
	 * Either, neither, or both of innerBound and outerBound may be negative; this does not change which is
	 * inclusive and which is exclusive.
	 * @param innerBound the inclusive inner bound; may be negative
	 * @param outerBound the exclusive outer bound; may be negative
	 * @return a double between {@code innerBound} (inclusive) and {@code outerBound} (exclusive)
	 */
	public double nextDouble (double innerBound, double outerBound) {
		return innerBound + nextDouble() * (outerBound - innerBound);
	}

	/**
	 * This is just like {@link #nextDouble()}, returning a double between 0 and 1, except that it is inclusive on both 0.0 and 1.0.
	 * It returns 1.0 extremely rarely, 0.000000000000011102230246251565% of the time if there is no bias in the generator, but it
	 * can happen. This uses {@link #nextLong(long)} internally, so it may have some bias towards or against specific
	 * subtly-different results.
	 * @return a double between 0.0, inclusive, and 1.0, inclusive
	 */
	public double nextInclusiveDouble() {
		return nextLong(0x20000000000001L) * 0x1p-53;
	}

	/**
	 * Just like {@link #nextDouble(double)}, but this is inclusive on both 0.0 and {@code outerBound}.
	 * It may be important to note that it returns outerBound on only 0.000000000000011102230246251565% of calls.
	 * @param outerBound the outer inclusive bound; may be positive or negative
	 * @return a double between 0.0, inclusive, and {@code outerBound}, inclusive
	 */
	public double nextInclusiveDouble(double outerBound) {
		return nextInclusiveDouble() * outerBound;
	}

	/**
	 * Just like {@link #nextDouble(double, double)}, but this is inclusive on both {@code innerBound} and {@code outerBound}.
	 * It may be important to note that it returns outerBound on only 0.000000000000011102230246251565% of calls, if it can
	 * return it at all because of floating-point imprecision when innerBound is a larger number.
	 * @param innerBound the inner inclusive bound; may be positive or negative
	 * @param outerBound the outer inclusive bound; may be positive or negative
	 * @return a double between {@code innerBound}, inclusive, and {@code outerBound}, inclusive
	 */
	public double nextInclusiveDouble(double innerBound, double outerBound) {
		return innerBound + nextInclusiveDouble() * (outerBound - innerBound);
	}

	/**
	 * This is just like {@link #nextFloat()}, returning a float between 0 and 1, except that it is inclusive on both 0.0 and 1.0.
	 * It returns 1.0 rarely, 0.00000596046412226771% of the time if there is no bias in the generator, but it can happen. This method
	 * has been tested by generating 268435456 (or 0x10000000) random ints with {@link #nextInt(int)}, and just before the end of that
	 * it had generated every one of the 16777217 roughly-equidistant floats this is able to produce. Not all seeds and streams are
	 * likely to accomplish that in the same time, or at all, depending on the generator.
	 * @return a float between 0.0, inclusive, and 1.0, inclusive
	 */
	public float nextInclusiveFloat() {
		return nextInt(0x1000001) * 0x1p-24f;
	}

	/**
	 * Just like {@link #nextFloat(float)}, but this is inclusive on both 0.0 and {@code outerBound}.
	 * It may be important to note that it returns outerBound on only 0.00000596046412226771% of calls.
	 * @param outerBound the outer inclusive bound; may be positive or negative
	 * @return a float between 0.0, inclusive, and {@code outerBound}, inclusive
	 */
	public float nextInclusiveFloat(float outerBound) {
		return nextInclusiveFloat() * outerBound;
	}

	/**
	 * Just like {@link #nextFloat(float, float)}, but this is inclusive on both {@code innerBound} and {@code outerBound}.
	 * It may be important to note that it returns outerBound on only 0.00000596046412226771% of calls, if it can return
	 * it at all because of floating-point imprecision when innerBound is a larger number.
	 * @param innerBound the inner inclusive bound; may be positive or negative
	 * @param outerBound the outer inclusive bound; may be positive or negative
	 * @return a float between {@code innerBound}, inclusive, and {@code outerBound}, inclusive
	 */
	public float nextInclusiveFloat(float innerBound, float outerBound) {
		return innerBound + nextInclusiveFloat() * (outerBound - innerBound);
	}

	/**
	 * Returns the next pseudorandom, Gaussian ("normally") distributed
	 * {@code double} value with mean {@code 0.0} and standard
	 * deviation {@code 1.0} from this random number generator's sequence.
	 * <p>
	 * The general contract of {@code nextGaussian} is that one
	 * {@code double} value, chosen from (approximately) the usual
	 * normal distribution with mean {@code 0.0} and standard deviation
	 * {@code 1.0}, is pseudorandomly generated and returned.
	 * <p>
	 * This uses an imperfect approximation, but one that is much faster than
	 * the Box-Muller transform, Marsaglia Polar method, or a transform using the
	 * probit function. Like earlier versions that used probit(), it requests
	 * exactly one long from the generator's sequence (using {@link #nextLong()}).
	 * This makes it different from code like java.util.Random's nextGaussian()
	 * method, which can (rarely) fetch a higher number of random doubles.
	 * <p>
	 * This can't produce as extreme results in extremely-rare cases as methods
	 * like Box-Muller and Marsaglia Polar can. All possible results are between
	 * {@code -7.929080009460449} and {@code 7.929080009460449}, inclusive.
	 * <p>
	 * <a href="https://marc-b-reynolds.github.io/distribution/2021/03/18/CheapGaussianApprox.html">Credit
	 * to Marc B. Reynolds</a> for coming up with this clever fusion of the
	 * already-bell-curved bit count and a triangular distribution to smooth
	 * it out. Using one random long instead of two is the contribution here.
	 *
	 * @return the next pseudorandom, Gaussian ("normally") distributed
	 * {@code double} value with mean {@code 0.0} and standard deviation
	 * {@code 1.0} from this random number generator's sequence
	 */
	public double nextGaussian () {
		//// here, we want to only request one long from this LaserRandom.
		//// because the bitCount() doesn't really care about the numerical value of its argument, only its Hamming weight,
		//// we use the random long un-scrambled, and get the bit count of that.
		//// for the later steps, we multiply the random long by a specific constant and get the difference of its halves.
		//// 0xC6AC29E4C6AC29E5L is... OK, it's complicated. It needs to have almost-identical upper and lower halves, but
		//// for reasons I don't currently understand, if the upper and lower halves are equal, then the min and max results
		//// of the Gaussian aren't equally distant from 0. By using an upper half that is exactly 1 less than the lower
		//// half, we get bounds of -7.929080009460449 to 7.929080009460449, returned when the RNG gives 0 and -1 resp.
		//// because it only needs one floating-point operation, it is quite fast on a CPU.
		//// this winds up being a very smooth Gaussian, as Marc B. Reynolds had it with two random longs.
		long u = nextLong();
		final long c = Long.bitCount(u) - 32L << 32;
		u *= 0xC6AC29E4C6AC29E5L;
		return 0x1.fb760cp-35 * (c + (u & 0xFFFFFFFFL) - (u >>> 32));
	}

	/**
	 * Advances or rolls back the {@code LaserRandom}' state without actually generating each number. Skips forward
	 * or backward a number of steps specified by advance, where a step is equal to one call to {@link #nextLong()},
	 * and returns the random number produced at that step. Negative numbers can be used to step backward, or 0 can be
	 * given to get the most-recently-generated long from {@link #nextLong()}.
	 *
	 * <p>Note that none of the number-generating methods here advance state differently from {@link #nextLong()} except
	 * for the Stream APIs. This is somewhat unusual; in many generators, calls to {@link #nextInt(int)} and similar
	 * bounded-range random generators can advance the state by a variable amount. Using a fixed advance permits this
	 * method and also allows guaranteeing the cycle length (also called period), but introduces a tiny amount of bias
	 * for some bounds (mostly very large ones).
	 *
	 * @param advance Number of future generations to skip over; can be negative to backtrack, 0 gets the most-recently-generated number
	 * @return the random long generated after skipping forward or backwards by {@code advance} numbers
	 */
	public long skip (long advance) {
		final long s = stateA += 0xC6BC279692B5C323L * advance;
		final long z = (s ^ s >>> 31) * (stateB += 0x9E3779B97F4A7C16L * advance);
		return z ^ z >>> 26 ^ z >>> 6;
	}

	public long previousLong (){
		final long s = stateA -= 0xC6BC279692B5C323L;
		final long z = (s ^ s >>> 31) * (stateB -= 0x9E3779B97F4A7C16L);
		return z ^ z >>> 26 ^ z >>> 6;
	}
	/**
	 * Creates a new {@code LaserRandom} with identical states to this one, so if the same LaserRandom methods are
	 * called on this object and its copy (in the same order), the same outputs will be produced. This is not
	 * guaranteed to copy the inherited state of the {@link Random} parent class, so if you call methods that are
	 * only implemented by Random and not LaserRandom, the results may differ.
	 * @return a deep copy of this LaserRandom.
	 */
	public LaserRandom copy () {
		return new LaserRandom(stateA, stateB);
	}

	/**
	 * Gets a long that identifies which stream of numbers this generator is producing; this stream identifier is always
	 * an odd long and won't change by generating numbers. It is determined at construction and will usually (not
	 * always) change if {@link #setStateA(long)} or {@link #setStateB(long)} are called. Each stream is a
	 * probably-unique sequence of 2 to the 64 longs, where approximately 1/3 of all possible longs will not ever occur
	 * (while others occur twice or more), but this set of results is different for every stream. There are 2 to the 63
	 * possible streams, one for every odd long.
	 *
	 * @return an odd long that identifies which stream this LaserRandom is generating from
	 */
	public long getStream () {
		return stateB - stateA * 0x3085776F0FBEB7F2L; // 0x3085776F0FBEB7F2L == 0x1743CE5C6E1B848BL * 0x9E3779B97F4A7C16L
	}

	/**
	 * Returns true if a random value between 0 and 1 is less than the specified value.
	 *
	 * @param chance a float between 0.0 and 1.0; higher values are more likely to result in true
	 * @return a boolean selected with the given {@code chance} of being true
	 */
	public boolean nextBoolean (float chance) {
		return nextFloat() < chance;
	}

	/**
	 * Returns -1 or 1, randomly.
	 *
	 * @return -1 or 1, selected with approximately equal likelihood
	 */
	public int nextSign () {
		return 1 | nextInt() >> 31;
	}

	/**
	 * Returns a triangularly distributed random number between -1.0 (exclusive) and 1.0 (exclusive), where values around zero are
	 * more likely. Advances the state twice.
	 * <p>
	 * This is an optimized version of {@link #nextTriangular(float, float, float) randomTriangular(-1, 1, 0)}
	 */
	public float nextTriangular () {
		return nextFloat() - nextFloat();
	}

	/**
	 * Returns a triangularly distributed random number between {@code -max} (exclusive) and {@code max} (exclusive), where values
	 * around zero are more likely. Advances the state twice.
	 * <p>
	 * This is an optimized version of {@link #nextTriangular(float, float, float) randomTriangular(-max, max, 0)}
	 *
	 * @param max the upper limit
	 */
	public float nextTriangular (float max) {
		return (nextFloat() - nextFloat()) * max;
	}

	/**
	 * Returns a triangularly distributed random number between {@code min} (inclusive) and {@code max} (exclusive), where the
	 * {@code mode} argument defaults to the midpoint between the bounds, giving a symmetric distribution. Advances the state once.
	 * <p>
	 * This method is equivalent of {@link #nextTriangular(float, float, float) randomTriangular(min, max, (min + max) * 0.5f)}
	 *
	 * @param min the lower limit
	 * @param max the upper limit
	 */
	public float nextTriangular (float min, float max) {
		return nextTriangular(min, max, (min + max) * 0.5f);
	}

	/**
	 * Returns a triangularly distributed random number between {@code min} (inclusive) and {@code max} (exclusive), where values
	 * around {@code mode} are more likely. Advances the state once.
	 *
	 * @param min  the lower limit
	 * @param max  the upper limit
	 * @param mode the point around which the values are more likely
	 */
	public float nextTriangular (float min, float max, float mode) {
		float u = nextFloat();
		float d = max - min;
		if (u <= (mode - min) / d) { return min + (float)Math.sqrt(u * d * (mode - min)); }
		return max - (float)Math.sqrt((1 - u) * d * (max - mode));
	}

	/**
	 * Gets a randomly-selected item from the given array, which must be non-null and non-empty
	 * @param array a non-null, non-empty array of {@code T} items
	 * @param <T> any reference type
	 * @return a random item from {@code array}
	 * @throws NullPointerException if array is null
	 * @throws IndexOutOfBoundsException if array is empty
	 */
	public <T> T randomElement (T[] array) {
		return array[nextInt(array.length)];
	}

	/**
	 * Shuffles the given array in-place pseudo-randomly, using this to determine how to shuffle.
	 *
	 * @param items an int array; must be non-null
	 */
	public void shuffle (int[] items) {
		for (int i = items.length - 1; i > 0; i--) {
			int ii = nextInt(i + 1);
			int temp = items[i];
			items[i] = items[ii];
			items[ii] = temp;
		}
	}

	/**
	 * Shuffles the given array in-place pseudo-randomly, using this to determine how to shuffle.
	 *
	 * @param items a long array; must be non-null
	 */
	public void shuffle (long[] items) {
		for (int i = items.length - 1; i > 0; i--) {
			int ii = nextInt(i + 1);
			long temp = items[i];
			items[i] = items[ii];
			items[ii] = temp;
		}
	}

	/**
	 * Shuffles the given array in-place pseudo-randomly, using this to determine how to shuffle.
	 *
	 * @param items a float array; must be non-null
	 */
	public void shuffle (float[] items) {
		for (int i = items.length - 1; i > 0; i--) {
			int ii = nextInt(i + 1);
			float temp = items[i];
			items[i] = items[ii];
			items[ii] = temp;
		}
	}

	/**
	 * Shuffles the given array in-place pseudo-randomly, using this to determine how to shuffle.
	 *
	 * @param items a char array; must be non-null
	 */
	public void shuffle (char[] items) {
		for (int i = items.length - 1; i > 0; i--) {
			int ii = nextInt(i + 1);
			char temp = items[i];
			items[i] = items[ii];
			items[ii] = temp;
		}
	}

	/**
	 * Shuffles the given array in-place pseudo-randomly, using this to determine how to shuffle.
	 *
	 * @param items a double array; must be non-null
	 */
	public void shuffle (double[] items) {
		for (int i = items.length - 1; i > 0; i--) {
			int ii = nextInt(i + 1);
			double temp = items[i];
			items[i] = items[ii];
			items[ii] = temp;
		}
	}

	/**
	 * Shuffles the given array in-place pseudo-randomly, using this to determine how to shuffle.
	 *
	 * @param items a short array; must be non-null
	 */
	public void shuffle (short[] items) {
		for (int i = items.length - 1; i > 0; i--) {
			int ii = nextInt(i + 1);
			short temp = items[i];
			items[i] = items[ii];
			items[ii] = temp;
		}
	}

	/**
	 * Shuffles the given array in-place pseudo-randomly, using this to determine how to shuffle.
	 *
	 * @param items a boolean array; must be non-null
	 */
	public void shuffle (boolean[] items) {
		for (int i = items.length - 1; i > 0; i--) {
			int ii = nextInt(i + 1);
			boolean temp = items[i];
			items[i] = items[ii];
			items[ii] = temp;
		}
	}

	/**
	 * Shuffles the given array in-place pseudo-randomly, using this to determine how to shuffle.
	 *
	 * @param items an array of some reference type; must be non-null but may contain null items
	 */
	public <T> void shuffle (T[] items) {
		for (int i = items.length - 1; i > 0; i--) {
			int ii = nextInt(i + 1);
			T temp = items[i];
			items[i] = items[ii];
			items[ii] = temp;
		}
	}

	@Override
	public boolean equals (Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		LaserRandom that = (LaserRandom)o;

		if (stateA != that.stateA)
			return false;
		return stateB == that.stateB;
	}

	@Override
	public String toString () {
		return "LaserRandom{" + "stateA=" + stateA + "L, stateB=" + stateB + "L}";
	}
}

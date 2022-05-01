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

import com.github.tommyettinger.ds.Arrangeable;
import com.github.tommyettinger.ds.FloatList;
import com.github.tommyettinger.ds.IntList;
import com.github.tommyettinger.ds.LongList;
import com.github.tommyettinger.ds.ObjectList;
import com.github.tommyettinger.ds.Ordered;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Random;

/**
 * A superset of the functionality in {@link java.util.Random}, meant for random number generators
 * that would be too bare-bones with just Random's methods.
 */
public abstract class AbstractRandom extends Random {

	/**
	 * Uses {@link Math#random()} to hastily put together a not-especially-uniform {@code long} value,
	 * meant only to produce a seed when no seed was specified (the "I don't care" seed).
	 *
	 * @return a kind-of-uniform {@code long} value
	 */
	public static long seedFromMath () {
		return (long)((Math.random() - 0.5) * 0x1p52) ^ (long)((Math.random() - 0.5) * 0x1p64);
	}

	/**
	 * Sets the seed of this random number generator using a single
	 * {@code long} seed. This should behave exactly the same as if a new
	 * object of this type was created with the constructor that takes a single
	 * {@code long} value. This does not necessarily assign the state
	 * variable(s) of the implementation with the exact contents of seed, so
	 * {@link #getSelectedState(int)} should not be expected to return
	 * {@code seed} after this, though it may. If this implementation has more
	 * than one {@code long} of state, then the expectation is that none of
	 * those state variables will be exactly equal to {@code seed} (almost all
	 * of the time).
	 *
	 * @param seed the initial seed
	 */
	public abstract void setSeed (long seed);

	/**
	 * Gets the number of possible state variables that can be selected with
	 * {@link #getSelectedState(int)} or {@link #setSelectedState(int, long)}.
	 * This defaults to returning 0, making no state variable available for
	 * reading or writing. An implementation that has only one {@code long}
	 * state, like a SplitMix64 generator, should return {@code 1}. A
	 * generator that permits setting two different {@code long} values, like
	 * {@link LaserRandom}, should return {@code 2}. Much larger values are
	 * possible for types like the Mersenne Twister or some CMWC generators.
	 *
	 * @return the non-negative number of selections possible for state variables
	 */
	public int getStateCount () {
		return 0;
	}

	/**
	 * Gets a selected state value from this AbstractRandom. The number of possible selections
	 * is up to the implementing class, and is accessible via {@link #getStateCount()}, but
	 * negative values for {@code selection} are typically not tolerated. This should return
	 * the exact value of the selected state, assuming it is implemented. The default
	 * implementation throws an UnsupportedOperationException, and implementors only have to
	 * allow reading the state if they choose to implement this differently. If this method
	 * is intended to be used, {@link #getStateCount()} must also be implemented.
	 *
	 * @param selection used to select which state variable to get; generally non-negative
	 * @return the exact value of the selected state
	 */
	public long getSelectedState (int selection) {
		throw new UnsupportedOperationException("getSelectedState() not supported.");
	}

	/**
	 * Sets a selected state value to the given long {@code value}. The number of possible
	 * selections is up to the implementing class, but negative values for {@code selection}
	 * are typically not tolerated. Implementors are permitted to change {@code value} if it
	 * is not valid, but they should not alter it if it is valid. The public implementation
	 * calls {@link #setSeed(long)} with {@code value}, which doesn't need changing if the
	 * generator has one state that is set verbatim by setSeed(). Otherwise, this method
	 * should be implemented when {@link #getSelectedState(int)} is and the state is allowed
	 * to be set by users. Having accurate ways to get and set the full state of a random
	 * number generator makes it much easier to serialize and deserialize that class.
	 *
	 * @param selection used to select which state variable to set; generally non-negative
	 * @param value     the exact value to use for the selected state, if valid
	 */
	public void setSelectedState (int selection, long value) {
		setSeed(value);
	}

	/**
	 * Sets each state variable to the given {@code state}. If {@link #getStateCount()} is
	 * 1, then this should set the whole state to the given value using
	 * {@link #setSelectedState(int, long)}. If getStateCount() is more than 1, then all
	 * states will be set in the same way (using setSelectedState(), all to {@code state}).
	 *
	 * @param state the long value to use for each state variable
	 */
	public void setState (long state) {
		for (int i = getStateCount() - 1; i >= 0; i--) {
			setSelectedState(i, state);
		}
	}

	/**
	 * Sets each state variable to either {@code stateA} or {@code stateB}, alternating.
	 * This uses {@link #setSelectedState(int, long)} to set the values. If there is one
	 * state variable ({@link #getStateCount()} is 1), then this only sets that state
	 * variable to stateA. If there are two state variables, the first is set to stateA,
	 * and the second to stateB. If there are more, it reuses stateA, then stateB, then
	 * stateA, and so on until all variables are set.
	 *
	 * @param stateA the long value to use for states at index 0, 2, 4, 6...
	 * @param stateB the long value to use for states at index 1, 3, 5, 7...
	 */
	public void setState (long stateA, long stateB) {
		final int c = getStateCount();
		for (int i = 0; i < c; i += 2) {
			setSelectedState(i, stateA);
		}
		for (int i = 1; i < c; i += 2) {
			setSelectedState(i, stateB);
		}
	}

	/**
	 * Sets each state variable to {@code stateA}, {@code stateB}, or {@code stateC},
	 * alternating. This uses {@link #setSelectedState(int, long)} to set the values.
	 * If there is one state variable ({@link #getStateCount()} is 1), then this only
	 * sets that state variable to stateA. If there are two state variables, the first
	 * is set to stateA, and the second to stateB. With three state variables, the
	 * first is set to stateA, the second to stateB, and the third to stateC. If there
	 * are more, it reuses stateA, then stateB, then stateC, then stateA, and so on
	 * until all variables are set.
	 *
	 * @param stateA the long value to use for states at index 0, 3, 6, 9...
	 * @param stateB the long value to use for states at index 1, 4, 7, 10...
	 * @param stateC the long value to use for states at index 2, 5, 8, 11...
	 */
	public void setState (long stateA, long stateB, long stateC) {
		final int c = getStateCount();
		for (int i = 0; i < c; i += 3) {
			setSelectedState(i, stateA);
		}
		for (int i = 1; i < c; i += 3) {
			setSelectedState(i, stateB);
		}
		for (int i = 2; i < c; i += 3) {
			setSelectedState(i, stateC);
		}
	}

	/**
	 * Sets each state variable to {@code stateA}, {@code stateB}, {@code stateC}, or
	 * {@code stateD}, alternating. This uses {@link #setSelectedState(int, long)} to
	 * set the values. If there is one state variable ({@link #getStateCount()} is 1),
	 * then this only sets that state variable to stateA. If there are two state
	 * variables, the first is set to stateA, and the second to stateB. With three
	 * state variables, the first is set to stateA, the second to stateB, and the third
	 * to stateC. With four state variables, the first is set to stateA, the second to
	 * stateB, the third to stateC, and the fourth to stateD. If there are more, it
	 * reuses stateA, then stateB, then stateC, then stateD, then stateA, and so on
	 * until all variables are set.
	 *
	 * @param stateA the long value to use for states at index 0, 4, 8, 12...
	 * @param stateB the long value to use for states at index 1, 5, 9, 13...
	 * @param stateC the long value to use for states at index 2, 6, 10, 14...
	 * @param stateD the long value to use for states at index 3, 7, 11, 15...
	 */
	public void setState (long stateA, long stateB, long stateC, long stateD) {
		final int c = getStateCount();
		for (int i = 0; i < c; i += 4) {
			setSelectedState(i, stateA);
		}
		for (int i = 1; i < c; i += 4) {
			setSelectedState(i, stateB);
		}
		for (int i = 2; i < c; i += 4) {
			setSelectedState(i, stateC);
		}
		for (int i = 3; i < c; i += 4) {
			setSelectedState(i, stateD);
		}
	}

	/**
	 * Sets all state variables to alternating values chosen from {@code states}. If states is empty,
	 * then this does nothing, and leaves the current generator unchanged. This works for
	 * generators with any {@link #getStateCount()}, but may allocate an array if states is
	 * used as a varargs (you can pass an existing array without needing to allocate). This
	 * uses {@link #setSelectedState(int, long)} to change the states.
	 *
	 * @param states an array or varargs of long values to use as states
	 */
	public void setState (@Nonnull long... states) {
		final int c = getStateCount(), sl = states.length;
		for (int b = 0; b < sl; b++) {
			final long curr = states[b];
			for (int i = b; i < c; i += sl) {
				setSelectedState(i, curr);
			}
		}
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
	 * Note that you can give this values for {@code bits} that are outside its expected range of 1 to 32,
	 * but the value used, as long as bits is positive, will effectively be {@code bits % 32}. As stated
	 * before, a value of 0 for bits is the same as a value of 32.<p>
	 *
	 * @param bits the amount of random bits to request, from 1 to 32
	 * @return the next pseudorandom value from this random number
	 * generator's sequence
	 */
	public int next (int bits) {
		return (int)nextLong() >>> 32 - bits;
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
		for (int i = 0; i < bytes.length; ) {for (long r = nextLong(), n = Math.min(bytes.length - i, 8); n-- > 0; r >>>= 8) {bytes[i++] = (byte)r;}}
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
		return (int)nextLong();
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
		return (int)(bound * (nextLong() & 0xFFFFFFFFL) >> 32) & ~(bound >> 31);
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
	 * @param outerBound the outer exclusive bound; may be any int value, allowing negative
	 * @return a pseudorandom int between 0 (inclusive) and outerBound (exclusive)
	 * @see #nextInt(int) Here's a note about the bias present in the bounded generation.
	 */
	public int nextSignedInt (int outerBound) {
		outerBound = (int)(outerBound * (nextLong() & 0xFFFFFFFFL) >> 32);
		return outerBound + (outerBound >>> 31);
	}

	/**
	 * Returns a pseudorandom, uniformly distributed {@code int} value between the
	 * specified {@code innerBound} (inclusive) and the specified {@code outerBound}
	 * (exclusive). If {@code outerBound} is less than or equal to {@code innerBound},
	 * this always returns {@code innerBound}. This is significantly slower than
	 * {@link #nextInt(int)} or {@link #nextSignedInt(int)},
	 * because this handles even ranges that go from large negative numbers to large
	 * positive numbers, and since that would be larger than the largest possible int,
	 * this has to use {@link #nextLong(long, long)}.
	 *
	 * <br> For any case where outerBound might be valid but less than innerBound, you
	 * can use {@link #nextSignedInt(int, int)}. If outerBound is less than innerBound
	 * here, this simply returns innerBound.
	 *
	 * @param innerBound the inclusive inner bound; may be any int, allowing negative
	 * @param outerBound the exclusive outer bound; must be greater than innerBound (otherwise this returns innerBound)
	 * @return a pseudorandom int between innerBound (inclusive) and outerBound (exclusive)
	 * @see #nextInt(int) Here's a note about the bias present in the bounded generation.
	 */
	public int nextInt (int innerBound, int outerBound) {
		return (int)nextLong(innerBound, outerBound);
	}

	/**
	 * Returns a pseudorandom, uniformly distributed {@code int} value between the
	 * specified {@code innerBound} (inclusive) and the specified {@code outerBound}
	 * (exclusive). This is meant for cases where either bound may be negative,
	 * especially if the bounds are unknown or may be user-specified. It is slightly
	 * slower than {@link #nextInt(int, int)}, and significantly slower than
	 * {@link #nextInt(int)} or {@link #nextSignedInt(int)}. This last part is
	 * because this handles even ranges that go from large negative numbers to large
	 * positive numbers, and since that range is larger than the largest possible int,
	 * this has to use {@link #nextSignedLong(long, long)}.
	 *
	 * @param innerBound the inclusive inner bound; may be any int, allowing negative
	 * @param outerBound the exclusive outer bound; may be any int, allowing negative
	 * @return a pseudorandom int between innerBound (inclusive) and outerBound (exclusive)
	 * @see #nextInt(int) Here's a note about the bias present in the bounded generation.
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
	 * The only methods that need to be implemented by this interface are
	 * this and {@link #copy()}, though other methods can be implemented
	 * as appropriate for generators that, for instance, natively produce
	 * ints rather than longs.
	 *
	 * @return the next pseudorandom, uniformly distributed {@code long}
	 * value from this random number generator's sequence
	 */
	public abstract long nextLong ();

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
	 * @param bound the upper bound (exclusive). If negative or 0, this always returns 0.
	 * @return the next pseudorandom, uniformly distributed {@code long}
	 * value between zero (inclusive) and {@code bound} (exclusive)
	 * from this random number generator's sequence
	 * @see #nextInt(int) Here's a note about the bias present in the bounded generation.
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
	 * @param outerBound the outer exclusive bound; may be any long value, allowing negative
	 * @return a pseudorandom long between 0 (inclusive) and outerBound (exclusive)
	 * @see #nextInt(int) Here's a note about the bias present in the bounded generation.
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
	 * @param inner the inclusive inner bound; may be any long, allowing negative
	 * @param outer the exclusive outer bound; must be greater than innerBound (otherwise this returns innerBound)
	 * @return a pseudorandom long between innerBound (inclusive) and outerBound (exclusive)
	 * @see #nextInt(int) Here's a note about the bias present in the bounded generation.
	 */
	public long nextLong (long inner, long outer) {
		final long rand = nextLong();
		if (inner >= outer)
			return inner;
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
	 * @param inner the inclusive inner bound; may be any long, allowing negative
	 * @param outer the exclusive outer bound; may be any long, allowing negative
	 * @return a pseudorandom long between innerBound (inclusive) and outerBound (exclusive)
	 * @see #nextInt(int) Here's a note about the bias present in the bounded generation.
	 */
	public long nextSignedLong (long inner, long outer) {
		final long rand = nextLong();
		if (outer < inner) {
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
	 * <br>
	 * The public implementation simply returns a sign check on {@link #nextLong()},
	 * returning true if the generated long is negative. This is typically the safest
	 * way to implement this method; many types of generators have less statistical
	 * quality on their lowest bit, so just returning based on the lowest bit isn't
	 * always a good idea.
	 *
	 * @return the next pseudorandom, uniformly distributed
	 * {@code boolean} value from this random number generator's
	 * sequence
	 */
	public boolean nextBoolean () {
		return nextLong() < 0L;
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
	 * <p>The public implementation uses the upper 24 bits of {@link #nextLong()},
	 * with an unsigned right shift and a multiply by a very small float
	 * ({@code 5.9604645E-8f} or {@code 0x1p-24f}). It tends to be fast if
	 * nextLong() is fast, but alternative implementations could use 24 bits of
	 * {@link #nextInt()} (or just {@link #next(int)}, giving it {@code 24})
	 * if that generator doesn't efficiently generate 64-bit longs.<p>
	 *
	 * @return the next pseudorandom, uniformly distributed {@code float}
	 * value between {@code 0.0} and {@code 1.0} from this
	 * random number generator's sequence
	 */
	public float nextFloat () {
		return (nextLong() >>> 40) * 0x1p-24f;
	}

	/**
	 * Gets a pseudo-random float between 0 (inclusive) and {@code outerBound} (exclusive).
	 * The outerBound may be positive or negative.
	 * Exactly the same as {@code nextFloat() * outerBound}.
	 *
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
	 *
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
	 * <p>The public implementation uses the upper 53 bits of {@link #nextLong()},
	 * with an unsigned right shift and a multiply by a very small double
	 * ({@code 1.1102230246251565E-16}, or {@code 0x1p-53}). It should perform well
	 * if nextLong() performs well, and is expected to perform less well if the
	 * generator naturally produces 32 or fewer bits at a time.<p>
	 *
	 * @return the next pseudorandom, uniformly distributed {@code double}
	 * value between {@code 0.0} and {@code 1.0} from this
	 * random number generator's sequence
	 */
	public double nextDouble () {
		return (nextLong() >>> 11) * 0x1.0p-53;
	}

	/**
	 * Gets a pseudo-random double between 0 (inclusive) and {@code outerBound} (exclusive).
	 * The outerBound may be positive or negative.
	 * Exactly the same as {@code nextDouble() * outerBound}.
	 *
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
	 *
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
	 *
	 * @return a double between 0.0, inclusive, and 1.0, inclusive
	 */
	public double nextInclusiveDouble () {
		return nextLong(0x20000000000001L) * 0x1p-53;
	}

	/**
	 * Just like {@link #nextDouble(double)}, but this is inclusive on both 0.0 and {@code outerBound}.
	 * It may be important to note that it returns outerBound on only 0.000000000000011102230246251565% of calls.
	 *
	 * @param outerBound the outer inclusive bound; may be positive or negative
	 * @return a double between 0.0, inclusive, and {@code outerBound}, inclusive
	 */
	public double nextInclusiveDouble (double outerBound) {
		return nextInclusiveDouble() * outerBound;
	}

	/**
	 * Just like {@link #nextDouble(double, double)}, but this is inclusive on both {@code innerBound} and {@code outerBound}.
	 * It may be important to note that it returns outerBound on only 0.000000000000011102230246251565% of calls, if it can
	 * return it at all because of floating-point imprecision when innerBound is a larger number.
	 *
	 * @param innerBound the inner inclusive bound; may be positive or negative
	 * @param outerBound the outer inclusive bound; may be positive or negative
	 * @return a double between {@code innerBound}, inclusive, and {@code outerBound}, inclusive
	 */
	public double nextInclusiveDouble (double innerBound, double outerBound) {
		return innerBound + nextInclusiveDouble() * (outerBound - innerBound);
	}

	/**
	 * This is just like {@link #nextFloat()}, returning a float between 0 and 1, except that it is inclusive on both 0.0 and 1.0.
	 * It returns 1.0 rarely, 0.00000596046412226771% of the time if there is no bias in the generator, but it can happen. This method
	 * has been tested by generating 268435456 (or 0x10000000) random ints with {@link #nextInt(int)}, and just before the end of that
	 * it had generated every one of the 16777217 roughly-equidistant floats this is able to produce. Not all seeds and streams are
	 * likely to accomplish that in the same time, or at all, depending on the generator.
	 *
	 * @return a float between 0.0, inclusive, and 1.0, inclusive
	 */
	public float nextInclusiveFloat () {
		return nextInt(0x1000001) * 0x1p-24f;
	}

	/**
	 * Just like {@link #nextFloat(float)}, but this is inclusive on both 0.0 and {@code outerBound}.
	 * It may be important to note that it returns outerBound on only 0.00000596046412226771% of calls.
	 *
	 * @param outerBound the outer inclusive bound; may be positive or negative
	 * @return a float between 0.0, inclusive, and {@code outerBound}, inclusive
	 */
	public float nextInclusiveFloat (float outerBound) {
		return nextInclusiveFloat() * outerBound;
	}

	/**
	 * Just like {@link #nextFloat(float, float)}, but this is inclusive on both {@code innerBound} and {@code outerBound}.
	 * It may be important to note that it returns outerBound on only 0.00000596046412226771% of calls, if it can return
	 * it at all because of floating-point imprecision when innerBound is a larger number.
	 *
	 * @param innerBound the inner inclusive bound; may be positive or negative
	 * @param outerBound the outer inclusive bound; may be positive or negative
	 * @return a float between {@code innerBound}, inclusive, and {@code outerBound}, inclusive
	 */
	public float nextInclusiveFloat (float innerBound, float outerBound) {
		return innerBound + nextInclusiveFloat() * (outerBound - innerBound);
	}

	/**
	 * Gets a random double between 0.0 and 1.0, exclusive at both ends; this method is also more uniform than
	 * {@link #nextDouble()} if you use the bit-patterns of the returned doubles. This is a simplified version of
	 * <a href="https://allendowney.com/research/rand/">this algorithm by Allen Downey</a>. This can return double
	 * values between 2.710505431213761E-20 and 0.9999999999999999, or 0x1.0p-65 and 0x1.fffffffffffffp-1 in hex
	 * notation. It cannot return 0 or 1. Most cases can instead use {@link #nextExclusiveDoubleEquidistant()}, which is
	 * implemented more traditionally but may have different performance. This method can also return doubles that
	 * are extremely close to 0, but can't return doubles that are as close to 1, due to limits of doubles.
	 * However, nextExclusiveDoubleEquidistant() can return only a minimum value that is as distant from 0 as its maximum
	 * value is distant from 1.
	 * <br>
	 * To compare, nextDouble() and nextExclusiveDoubleEquidistant() are less likely to produce a "1" bit for their
	 * lowest 5 bits of mantissa/significand (the least significant bits numerically, but potentially important
	 * for some uses), with the least significant bit produced half as often as the most significant bit in the
	 * mantissa. As for this method, it has approximately the same likelihood of producing a "1" bit for any
	 * position in the mantissa.
	 * <br>
	 * The public implementation may have different performance characteristics than {@link #nextDouble()},
	 * because this doesn't perform any floating-point multiplication or division, and instead assembles bits
	 * obtained by one call to {@link #nextLong()}. This uses {@link BitConversion#longBitsToDouble(long)} and
	 * {@link Long#numberOfTrailingZeros(long)}, both of which typically have optimized intrinsics on HotSpot,
	 * and this is branchless and loopless, unlike the original algorithm by Allen Downey. When compared with
	 * {@link #nextExclusiveDoubleEquidistant()}, this method performs better on at least HotSpot JVMs.
	 *
	 * @return a random uniform double between 0 and 1 (both exclusive)
	 */
	public double nextExclusiveDouble () {
		final long bits = nextLong();
		return BitConversion.longBitsToDouble(1022L - Long.numberOfTrailingZeros(bits) << 52 | bits >>> 12);
	}

	/**
	 * Gets a random double between 0.0 and 1.0, exclusive at both ends. This can return double
	 * values between 1.1102230246251565E-16 and 0.9999999999999999, or 0x1.0p-53 and 0x1.fffffffffffffp-1 in hex
	 * notation. It cannot return 0 or 1, and its minimum and maximum results are equally distant from 0 and from
	 * 1, respectively. Some usages may prefer {@link #nextExclusiveDouble()}, which is
	 * better-distributed if you consider the bit representation of the returned doubles, tends to perform
	 * better, and can return doubles that much closer to 0 than this can.
	 * <br>
	 * The public implementation simply uses {@link #nextLong(long)} to get a uniformly-chosen long between 1 and
	 * (2 to the 53) - 1, both inclusive, and multiplies it by (2 to the -53). Using larger values than (2 to the
	 * 53) would cause issues with the double math.
	 *
	 * @return a random uniform double between 0 and 1 (both exclusive)
	 */
	public double nextExclusiveDoubleEquidistant () {
		return (nextLong(0x1FFFFFFFFFFFFFL) + 1L) * 0x1p-53;
	}

	/**
	 * Just like {@link #nextDouble(double)}, but this is exclusive on both 0.0 and {@code outerBound}.
	 * Like {@link #nextExclusiveDouble()}, which this uses, this may have better bit-distribution of
	 * double values, and it may also be better able to produce very small doubles when {@code outerBound} is large.
	 *
	 * @param outerBound the outer exclusive bound; may be positive or negative
	 * @return a double between 0.0, exclusive, and {@code outerBound}, exclusive
	 */
	public double nextExclusiveDouble (double outerBound) {
		return nextExclusiveDouble() * outerBound;
	}

	/**
	 * Just like {@link #nextDouble(double, double)}, but this is exclusive on both {@code innerBound} and {@code outerBound}.
	 * Like {@link #nextExclusiveDouble()}, which this uses,, this may have better bit-distribution of double values,
	 * and it may also be better able to produce doubles close to innerBound when {@code outerBound - innerBound} is large.
	 *
	 * @param innerBound the inner exclusive bound; may be positive or negative
	 * @param outerBound the outer exclusive bound; may be positive or negative
	 * @return a double between {@code innerBound}, exclusive, and {@code outerBound}, exclusive
	 */
	public double nextExclusiveDouble (double innerBound, double outerBound) {
		return innerBound + nextExclusiveDouble() * (outerBound - innerBound);
	}

	/**
	 * Gets a random float between 0.0 and 1.0, exclusive at both ends. This method is also more uniform than
	 * {@link #nextFloat()} if you use the bit-patterns of the returned floats. This is a simplified version of
	 * <a href="https://allendowney.com/research/rand/">this algorithm by Allen Downey</a>. This version can
	 * return float values between 2.7105054E-20 to 0.99999994, or 0x1.0p-65 to 0x1.fffffep-1 in hex notation.
	 * It cannot return 0 or 1. To compare, nextFloat() is less likely to produce a "1" bit for its
	 * lowest 5 bits of mantissa/significand (the least significant bits numerically, but potentially important
	 * for some uses), with the least significant bit produced half as often as the most significant bit in the
	 * mantissa. As for this method, it has approximately the same likelihood of producing a "1" bit for any
	 * position in the mantissa.
	 * <br>
	 * The public implementation may have different performance characteristics than {@link #nextFloat()},
	 * because this doesn't perform any floating-point multiplication or division, and instead assembles bits
	 * obtained by one call to {@link #nextLong()}. This uses {@link BitConversion#intBitsToFloat(int)} and
	 * {@link Long#numberOfTrailingZeros(long)}, both of which typically have optimized intrinsics on HotSpot,
	 * and this is branchless and loopless, unlike the original algorithm by Allen Downey. When compared with
	 * {@link #nextExclusiveFloatEquidistant()}, this method performs better on at least HotSpot JVMs.
	 *
	 * @return a random uniform float between 0 and 1 (both exclusive)
	 */
	public float nextExclusiveFloat () {
		final long bits = nextLong();
		return BitConversion.intBitsToFloat(126 - Long.numberOfTrailingZeros(bits) << 23 | (int)(bits >>> 41));
	}

	/**
	 * Gets a random float between 0.0 and 1.0, exclusive at both ends. This can return float
	 * values between 5.9604645E-8 and 0.99999994, or 0x1.0p-24 and 0x1.fffffep-1 in hex notation.
	 * It cannot return 0 or 1, and its minimum and maximum results are equally distant from 0 and from
	 * 1, respectively. Some usages may prefer {@link #nextExclusiveFloat()}, which is
	 * better-distributed if you consider the bit representation of the returned floats, tends to perform
	 * better, and can return floats that much closer to 0 than this can.
	 * <br>
	 * The public implementation simply uses {@link #nextInt(int)} to get a uniformly-chosen int between 1 and
	 * (2 to the 24) - 1, both inclusive, and multiplies it by (2 to the -24). Using larger values than (2 to the
	 * 24) would cause issues with the float math.
	 *
	 * @return a random uniform float between 0 and 1 (both exclusive)
	 */
	public float nextExclusiveFloatEquidistant () {
		return (nextInt(0xFFFFFF) + 1) * 0x1p-24f;
	}

	/**
	 * Just like {@link #nextFloat(float)}, but this is exclusive on both 0.0 and {@code outerBound}.
	 * Like {@link #nextExclusiveFloat()}, this may have better bit-distribution of float values, and
	 * it may also be better able to produce very small floats when {@code outerBound} is large.
	 *
	 * @param outerBound the outer exclusive bound; may be positive or negative
	 * @return a float between 0.0, exclusive, and {@code outerBound}, exclusive
	 */
	public float nextExclusiveFloat (float outerBound) {
		return nextExclusiveFloat() * outerBound;
	}

	/**
	 * Just like {@link #nextFloat(float, float)}, but this is exclusive on both {@code innerBound} and {@code outerBound}.
	 * Like {@link #nextExclusiveFloat()}, this may have better bit-distribution of float values, and
	 * it may also be better able to produce floats close to innerBound when {@code outerBound - innerBound} is large.
	 *
	 * @param innerBound the inner exclusive bound; may be positive or negative
	 * @param outerBound the outer exclusive bound; may be positive or negative
	 * @return a float between {@code innerBound}, exclusive, and {@code outerBound}, exclusive
	 */
	public float nextExclusiveFloat (float innerBound, float outerBound) {
		return innerBound + nextExclusiveFloat() * (outerBound - innerBound);
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
	 * @return the next pseudorandom, approximately Gaussian ("normally") distributed
	 * {@code double} value with mean {@code 0.0} and standard deviation
	 * {@code 1.0} from this random number generator's sequence
	 */
	public double nextGaussian () {
		//// here, we want to only request one long from this AbstractRandom.
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
	 * Optional; advances or rolls back the {@code AbstractRandom}' state without actually generating each number.
	 * Skips forward or backward a number of steps specified by advance, where a step is equal to one call to
	 * {@link #nextLong()}, and returns the random number produced at that step. Negative numbers can be used to
	 * step backward, or 0 can be given to get the most-recently-generated long from {@link #nextLong()}.
	 *
	 * <p>The public implementation throws an UnsupportedOperationException. Many types of random
	 * number generator do not have an efficient way of skipping arbitrarily through the state sequence,
	 * and those types should not implement this method differently.
	 *
	 * @param advance Number of future generations to skip over; can be negative to backtrack, 0 gets the most-recently-generated number
	 * @return the random long generated after skipping forward or backwards by {@code advance} numbers
	 */
	public long skip (long advance) {
		throw new UnsupportedOperationException("skip() not supported.");
	}

	/**
	 * Optional; moves the state to its previous value and returns the previous long that would have been produced by
	 * {@link #nextLong()}. This is often equivalent to calling {@link #skip(long)} with -1L, but not always; some
	 * generators can't efficiently skip long distances, but can step back by one value.
	 *
	 * <p>The public implementation calls {@link #skip(long)} with -1L, and if skip() has not been implemented
	 * differently, then it will throw an UnsupportedOperationException.
	 *
	 * @return the previous number this would have produced with {@link #nextLong()}
	 */
	public long previousLong () {
		return skip(-1L);
	}

	/**
	 * Creates a new AbstractRandom with identical states to this one, so if the same AbstractRandom methods are
	 * called on this object and its copy (in the same order), the same outputs will be produced. This is not
	 * guaranteed to copy the inherited state of any parent class, so if you call methods that are
	 * only implemented by a superclass (like {@link java.util.Random}) and not this one, the results may differ.
	 *
	 * @return a deep copy of this AbstractRandom.
	 */
	public abstract AbstractRandom copy ();

	/**
	 * Similar to {@link #copy()}, but fills this AbstractRandom with the state of another AbstractRandom, usually
	 * (but not necessarily) one of the same type. If this class has the same {@link #getStateCount()} as other's
	 * class, then this method copies the full state of other into this object. Otherwise, if this class has a
	 * larger state count than other's class, then all of other's state is copied into the same selections in this
	 * object, and the rest of this object's state is filled with {@code -1L} using
	 * {@link #setSelectedState(int, long)}. If this class has a smaller state count than other's class, then only
	 * part of other's state is copied, and this method stops when all of this object's states have been assigned.
	 * <br>
	 * If this class has restrictions on its state, they will be respected by the public implementation of this
	 * method as long as {@link #setSelectedState(int, long)} behaves correctly for those restrictions. Note that
	 * this method will public to throwing an UnsupportedOperationException unless {@link #getSelectedState(int)}
	 * is implemented by other so its state can be accessed. This may also behave badly if
	 * {@link #setSelectedState(int, long)} isn't implemented (it may be fine for some cases where the state count
	 * is 1, but don't count on it). If other's class doesn't implement {@link #getStateCount()}, then this method
	 * sets the entire state of this object to -1L; if this class doesn't implement getStateCount(), then this
	 * method does nothing.
	 *
	 * @param other another AbstractRandom, typically with the same class as this one, to copy its state into this
	 */
	public void setWith (AbstractRandom other) {
		final int myCount = getStateCount(), otherCount = other.getStateCount();
		int i = 0;
		for (; i < myCount && i < otherCount; i++) {
			setSelectedState(i, other.getSelectedState(i));
		}
		for (; i < myCount; i++) {
			setSelectedState(i, -1L);
		}
	}

	/**
	 * A way of taking a double in the (0.0, 1.0) range and mapping it to a Gaussian or normal distribution, so high
	 * inputs correspond to high outputs, and similarly for the low range. This is centered on 0.0 and its standard
	 * deviation seems to be 1.0 (the same as {@link java.util.Random#nextGaussian()}). If this is given an input of 0.0
	 * or less, it returns -38.5, which is slightly less than the result when given {@link Double#MIN_VALUE}. If it is
	 * given an input of 1.0 or more, it returns 38.5, which is significantly larger than the result when given the
	 * largest double less than 1.0 (this value is further from 1.0 than {@link Double#MIN_VALUE} is from 0.0). If
	 * given {@link Double#NaN}, it returns whatever {@link Math#copySign(double, double)} returns for the arguments
	 * {@code 38.5, Double.NaN}, which is implementation-dependent. It uses an algorithm by Peter John Acklam, as
	 * implemented by Sherali Karimov.
	 * <a href="https://web.archive.org/web/20150910002142/http://home.online.no/~pjacklam/notes/invnorm/impl/karimov/StatUtil.java">Original source</a>.
	 * <a href="https://web.archive.org/web/20151030215612/http://home.online.no/~pjacklam/notes/invnorm/">Information on the algorithm</a>.
	 * <a href="https://en.wikipedia.org/wiki/Probit_function">Wikipedia's page on the probit function</a> may help, but
	 * is more likely to just be confusing.
	 * <br>
	 * Acklam's algorithm and Karimov's implementation are both quite fast. This appears faster than generating
	 * Gaussian-distributed numbers using either the Box-Muller Transform or Marsaglia's Polar Method, though it isn't
	 * as precise and can't produce as extreme min and max results in the extreme cases they should appear. If given
	 * a typical uniform random {@code double} that's exclusive on 1.0, it won't produce a result higher than
	 * {@code 8.209536145151493}, and will only produce results of at least {@code -8.209536145151493} if 0.0 is
	 * excluded from the inputs (if 0.0 is an input, the result is {@code 38.5}). A chief advantage of using this with
	 * a random number generator is that it only requires one random double to obtain one Gaussian value;
	 * {@link java.util.Random#nextGaussian()} generates at least two random doubles for each two Gaussian values, but
	 * may rarely require much more random generation.
	 * <br>
	 * This can be used both as an optimization for generating Gaussian random values, and as a way of generating
	 * Gaussian values that match a pattern present in the inputs (which you could have by using a sub-random sequence
	 * as the input, such as those produced by a van der Corput, Halton, Sobol or R2 sequence). Most methods of generating
	 * Gaussian values (e.g. Box-Muller and Marsaglia polar) do not have any way to preserve a particular pattern.
	 *
	 * @param d should be between 0 and 1, exclusive, but other values are tolerated
	 * @return a normal-distributed double centered on 0.0; all results will be between -38.5 and 38.5, both inclusive
	 */
	public static double probit (final double d) {
		if (d <= 0 || d >= 1) {
			return Math.copySign(38.5, d - 0.5);
		} else if (d < 0.02425) {
			final double q = Math.sqrt(-2.0 * Math.log(d));
			return (((((-7.784894002430293e-03 * q + -3.223964580411365e-01) * q + -2.400758277161838e+00) * q + -2.549732539343734e+00) * q + 4.374664141464968e+00) * q + 2.938163982698783e+00) / (
				(((7.784695709041462e-03 * q + 3.224671290700398e-01) * q + 2.445134137142996e+00) * q + 3.754408661907416e+00) * q + 1.0);
		} else if (0.97575 < d) {
			final double q = Math.sqrt(-2.0 * Math.log(1 - d));
			return -(((((-7.784894002430293e-03 * q + -3.223964580411365e-01) * q + -2.400758277161838e+00) * q + -2.549732539343734e+00) * q + 4.374664141464968e+00) * q + 2.938163982698783e+00) / (
				(((7.784695709041462e-03 * q + 3.224671290700398e-01) * q + 2.445134137142996e+00) * q + 3.754408661907416e+00) * q + 1.0);
		} else {
			final double q = d - 0.5;
			final double r = q * q;
			return (((((-3.969683028665376e+01 * r + 2.209460984245205e+02) * r + -2.759285104469687e+02) * r + 1.383577518672690e+02) * r + -3.066479806614716e+01) * r + 2.506628277459239e+00) * q / (
				((((-5.447609879822406e+01 * r + 1.615858368580409e+02) * r + -1.556989798598866e+02) * r + 6.680131188771972e+01) * r + -1.328068155288572e+01) * r + 1.0);
		}
	}

	/**
	 * Given two AbstractRandom objects that could have the same or different classes,
	 * this returns true if they have the same class and same state, or false otherwise.
	 * Both of the arguments should implement {@link #getSelectedState(int)}, or this
	 * will throw an UnsupportedOperationException. This can be useful for comparing
	 * AbstractRandom classes that do not implement equals(), for whatever reason.
	 *
	 * @param left  an AbstractRandom to compare for equality
	 * @param right another AbstractRandom to compare for equality
	 * @return true if the two AbstractRandom objects have the same class and state, or false otherwise
	 */
	public static boolean areEqual (AbstractRandom left, AbstractRandom right) {
		if (left == right)
			return true;
		if (left.getClass() != right.getClass())
			return false;

		final int count = left.getStateCount();
		for (int i = 0; i < count; i++) {
			if (left.getSelectedState(i) != right.getSelectedState(i))
				return false;
		}
		return true;
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
	 * This is an optimized version of {@link #nextTriangular(float, float, float) nextTriangular(-1, 1, 0)}
	 */
	public float nextTriangular () {
		return nextFloat() - nextFloat();
	}

	/**
	 * Returns a triangularly distributed random number between {@code -max} (exclusive) and {@code max} (exclusive), where values
	 * around zero are more likely. Advances the state twice.
	 * <p>
	 * This is an optimized version of {@link #nextTriangular(float, float, float) nextTriangular(-max, max, 0)}
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
	 * This method is equivalent to {@link #nextTriangular(float, float, float) nextTriangular(min, max, (min + max) * 0.5f)}
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
		if (u <= (mode - min) / d) {return min + (float)Math.sqrt(u * d * (mode - min));}
		return max - (float)Math.sqrt((1 - u) * d * (max - mode));
	}

	/**
	 * Returns the minimum result of {@code trials} calls to {@link #nextSignedInt(int, int)} using the given {@code innerBound}
	 * and {@code outerBound}. The innerBound is inclusive; the outerBound is exclusive.
	 * The higher trials is, the lower the average value this returns.
	 *
	 * @param innerBound the inner inclusive bound; may be positive or negative
	 * @param outerBound the outer exclusive bound; may be positive or negative
	 * @param trials     how many random numbers to acquire and compare
	 * @return the lowest random number between innerBound (inclusive) and outerBound (exclusive) this found
	 */
	public int minIntOf (int innerBound, int outerBound, int trials) {
		int v = nextSignedInt(innerBound, outerBound);
		for (int i = 1; i < trials; i++) {
			v = Math.min(v, nextSignedInt(innerBound, outerBound));
		}
		return v;
	}

	/**
	 * Returns the maximum result of {@code trials} calls to {@link #nextSignedInt(int, int)} using the given {@code innerBound}
	 * and {@code outerBound}. The innerBound is inclusive; the outerBound is exclusive.
	 * The higher trials is, the higher the average value this returns.
	 *
	 * @param innerBound the inner inclusive bound; may be positive or negative
	 * @param outerBound the outer exclusive bound; may be positive or negative
	 * @param trials     how many random numbers to acquire and compare
	 * @return the highest random number between innerBound (inclusive) and outerBound (exclusive) this found
	 */
	public int maxIntOf (int innerBound, int outerBound, int trials) {
		int v = nextSignedInt(innerBound, outerBound);
		for (int i = 1; i < trials; i++) {
			v = Math.max(v, nextSignedInt(innerBound, outerBound));
		}
		return v;
	}

	/**
	 * Returns the minimum result of {@code trials} calls to {@link #nextSignedLong(long, long)} using the given {@code innerBound}
	 * and {@code outerBound}. The innerBound is inclusive; the outerBound is exclusive.
	 * The higher trials is, the lower the average value this returns.
	 *
	 * @param innerBound the inner inclusive bound; may be positive or negative
	 * @param outerBound the outer exclusive bound; may be positive or negative
	 * @param trials     how many random numbers to acquire and compare
	 * @return the lowest random number between innerBound (inclusive) and outerBound (exclusive) this found
	 */
	public long minLongOf (long innerBound, long outerBound, int trials) {
		long v = nextSignedLong(innerBound, outerBound);
		for (int i = 1; i < trials; i++) {
			v = Math.min(v, nextSignedLong(innerBound, outerBound));
		}
		return v;
	}

	/**
	 * Returns the maximum result of {@code trials} calls to {@link #nextSignedLong(long, long)} using the given {@code innerBound}
	 * and {@code outerBound}. The innerBound is inclusive; the outerBound is exclusive.
	 * The higher trials is, the higher the average value this returns.
	 *
	 * @param innerBound the inner inclusive bound; may be positive or negative
	 * @param outerBound the outer exclusive bound; may be positive or negative
	 * @param trials     how many random numbers to acquire and compare
	 * @return the highest random number between innerBound (inclusive) and outerBound (exclusive) this found
	 */
	public long maxLongOf (long innerBound, long outerBound, int trials) {
		long v = nextSignedLong(innerBound, outerBound);
		for (int i = 1; i < trials; i++) {
			v = Math.max(v, nextSignedLong(innerBound, outerBound));
		}
		return v;
	}

	/**
	 * Returns the minimum result of {@code trials} calls to {@link #nextDouble(double, double)} using the given {@code innerBound}
	 * and {@code outerBound}. The innerBound is inclusive; the outerBound is exclusive.
	 * The higher trials is, the lower the average value this returns.
	 *
	 * @param innerBound the inner inclusive bound; may be positive or negative
	 * @param outerBound the outer exclusive bound; may be positive or negative
	 * @param trials     how many random numbers to acquire and compare
	 * @return the lowest random number between innerBound (inclusive) and outerBound (exclusive) this found
	 */
	public double minDoubleOf (double innerBound, double outerBound, int trials) {
		double v = nextDouble(innerBound, outerBound);
		for (int i = 1; i < trials; i++) {
			v = Math.min(v, nextDouble(innerBound, outerBound));
		}
		return v;
	}

	/**
	 * Returns the maximum result of {@code trials} calls to {@link #nextDouble(double, double)} using the given {@code innerBound}
	 * and {@code outerBound}. The innerBound is inclusive; the outerBound is exclusive.
	 * The higher trials is, the higher the average value this returns.
	 *
	 * @param innerBound the inner inclusive bound; may be positive or negative
	 * @param outerBound the outer exclusive bound; may be positive or negative
	 * @param trials     how many random numbers to acquire and compare
	 * @return the highest random number between innerBound (inclusive) and outerBound (exclusive) this found
	 */
	public double maxDoubleOf (double innerBound, double outerBound, int trials) {
		double v = nextDouble(innerBound, outerBound);
		for (int i = 1; i < trials; i++) {
			v = Math.max(v, nextDouble(innerBound, outerBound));
		}
		return v;
	}

	/**
	 * Returns the minimum result of {@code trials} calls to {@link #nextFloat(float, float)} using the given {@code innerBound}
	 * and {@code outerBound}. The innerBound is inclusive; the outerBound is exclusive.
	 * The higher trials is, the lower the average value this returns.
	 *
	 * @param innerBound the inner inclusive bound; may be positive or negative
	 * @param outerBound the outer exclusive bound; may be positive or negative
	 * @param trials     how many random numbers to acquire and compare
	 * @return the lowest random number between innerBound (inclusive) and outerBound (exclusive) this found
	 */
	public float minFloatOf (float innerBound, float outerBound, int trials) {
		float v = nextFloat(innerBound, outerBound);
		for (int i = 1; i < trials; i++) {
			v = Math.min(v, nextFloat(innerBound, outerBound));
		}
		return v;
	}

	/**
	 * Returns the maximum result of {@code trials} calls to {@link #nextFloat(float, float)} using the given {@code innerBound}
	 * and {@code outerBound}. The innerBound is inclusive; the outerBound is exclusive.
	 * The higher trials is, the higher the average value this returns.
	 *
	 * @param innerBound the inner inclusive bound; may be positive or negative
	 * @param outerBound the outer exclusive bound; may be positive or negative
	 * @param trials     how many random numbers to acquire and compare
	 * @return the highest random number between innerBound (inclusive) and outerBound (exclusive) this found
	 */
	public float maxFloatOf (float innerBound, float outerBound, int trials) {
		float v = nextFloat(innerBound, outerBound);
		for (int i = 1; i < trials; i++) {
			v = Math.max(v, nextFloat(innerBound, outerBound));
		}
		return v;
	}

	/**
	 * Gets a randomly-selected item from the given array, which must be non-null and non-empty
	 *
	 * @param array a non-null, non-empty array of {@code T} items
	 * @param <T>   any reference type
	 * @return a random item from {@code array}
	 * @throws NullPointerException      if array is null
	 * @throws IndexOutOfBoundsException if array is empty
	 */
	public <T> T randomElement (T[] array) {
		return array[nextInt(array.length)];
	}

	/**
	 * Gets a randomly selected item from the given List, such as an ArrayList.
	 * If the List is empty, this throws an IndexOutOfBoundsException.
	 *
	 * @param list    a non-empty implementation of List, such as ArrayList
	 * @param <T>     the type of items
	 * @return a randomly-selected item from list
	 */
	public <T> T randomElement (List<T> list) {
		return list.get(nextInt(list.size()));
	}

	/**
	 * Shuffles the given array in-place pseudo-randomly, using this to determine how to shuffle.
	 *
	 * @param items an int array; must be non-null
	 */
	public void shuffle (int[] items) {
		shuffle(items, 0, items.length);
	}

	/**
	 * Shuffles a section of the given array in-place pseudo-randomly, using this to determine how to shuffle.
	 *
	 * @param items  an int array; must be non-null
	 * @param offset the index of the first element of the array that can be shuffled
	 * @param length the length of the section to shuffle
	 */
	public void shuffle (int[] items, int offset, int length) {
		offset = Math.min(Math.max(0, offset), items.length);
		length = Math.min(items.length - offset, Math.max(0, length));
		for (int i = offset + length - 1; i > offset; i--) {
			int ii = nextInt(offset, i + 1);
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
		shuffle(items, 0, items.length);
	}

	/**
	 * Shuffles a section of the given array in-place pseudo-randomly, using this to determine how to shuffle.
	 *
	 * @param items  a long array; must be non-null
	 * @param offset the index of the first element of the array that can be shuffled
	 * @param length the length of the section to shuffle
	 */
	public void shuffle (long[] items, int offset, int length) {
		offset = Math.min(Math.max(0, offset), items.length);
		length = Math.min(items.length - offset, Math.max(0, length));
		for (int i = offset + length - 1; i > offset; i--) {
			int ii = nextInt(offset, i + 1);
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
		shuffle(items, 0, items.length);
	}

	/**
	 * Shuffles a section of the given array in-place pseudo-randomly, using this to determine how to shuffle.
	 *
	 * @param items  a float array; must be non-null
	 * @param offset the index of the first element of the array that can be shuffled
	 * @param length the length of the section to shuffle
	 */
	public void shuffle (float[] items, int offset, int length) {
		offset = Math.min(Math.max(0, offset), items.length);
		length = Math.min(items.length - offset, Math.max(0, length));
		for (int i = offset + length - 1; i > offset; i--) {
			int ii = nextInt(offset, i + 1);
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
		shuffle(items, 0, items.length);
	}

	/**
	 * Shuffles a section of the given array in-place pseudo-randomly, using this to determine how to shuffle.
	 *
	 * @param items  a char array; must be non-null
	 * @param offset the index of the first element of the array that can be shuffled
	 * @param length the length of the section to shuffle
	 */
	public void shuffle (char[] items, int offset, int length) {
		offset = Math.min(Math.max(0, offset), items.length);
		length = Math.min(items.length - offset, Math.max(0, length));
		for (int i = offset + length - 1; i > offset; i--) {
			int ii = nextInt(offset, i + 1);
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
		shuffle(items, 0, items.length);
	}

	/**
	 * Shuffles a section of the given array in-place pseudo-randomly, using this to determine how to shuffle.
	 *
	 * @param items  a double array; must be non-null
	 * @param offset the index of the first element of the array that can be shuffled
	 * @param length the length of the section to shuffle
	 */
	public void shuffle (double[] items, int offset, int length) {
		offset = Math.min(Math.max(0, offset), items.length);
		length = Math.min(items.length - offset, Math.max(0, length));
		for (int i = offset + length - 1; i > offset; i--) {
			int ii = nextInt(offset, i + 1);
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
		shuffle(items, 0, items.length);
	}

	/**
	 * Shuffles a section of the given array in-place pseudo-randomly, using this to determine how to shuffle.
	 *
	 * @param items  a short array; must be non-null
	 * @param offset the index of the first element of the array that can be shuffled
	 * @param length the length of the section to shuffle
	 */
	public void shuffle (short[] items, int offset, int length) {
		offset = Math.min(Math.max(0, offset), items.length);
		length = Math.min(items.length - offset, Math.max(0, length));
		for (int i = offset + length - 1; i > offset; i--) {
			int ii = nextInt(offset, i + 1);
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
		shuffle(items, 0, items.length);
	}

	/**
	 * Shuffles a section of the given array in-place pseudo-randomly, using this to determine how to shuffle.
	 *
	 * @param items  a boolean array; must be non-null
	 * @param offset the index of the first element of the array that can be shuffled
	 * @param length the length of the section to shuffle
	 */
	public void shuffle (boolean[] items, int offset, int length) {
		offset = Math.min(Math.max(0, offset), items.length);
		length = Math.min(items.length - offset, Math.max(0, length));
		for (int i = offset + length - 1; i > offset; i--) {
			int ii = nextInt(offset, i + 1);
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
		shuffle(items, 0, items.length);
	}

	/**
	 * Shuffles a section of the given array in-place pseudo-randomly, using this to determine how to shuffle.
	 *
	 * @param items  an array of some reference type; must be non-null but may contain null items
	 * @param offset the index of the first element of the array that can be shuffled
	 * @param length the length of the section to shuffle
	 */
	public <T> void shuffle (T[] items, int offset, int length) {
		offset = Math.min(Math.max(0, offset), items.length);
		length = Math.min(items.length - offset, Math.max(0, length));
		for (int i = offset + length - 1; i > offset; i--) {
			int ii = nextInt(offset, i + 1);
			T temp = items[i];
			items[i] = items[ii];
			items[ii] = temp;
		}
	}
}

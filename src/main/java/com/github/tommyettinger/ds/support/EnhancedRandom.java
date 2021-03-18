package com.github.tommyettinger.ds.support;

import java.util.Random;

/**
 * A superset of the functionality in {@link java.util.Random}, meant for random number generators
 * that would be too bare-bones with just Random's methods.
 */
public interface EnhancedRandom {
	/**
	 * Sets the seed of this random number generator using a single
	 * {@code long} seed. The general contract of {@code setSeed} is
	 * that it alters the state of this random number generator object
	 * so as to be in exactly the same state as if it had just been
	 * created with the argument {@code seed} as a seed.
	 *
	 * @param seed the initial seed
	 */
	void setSeed (long seed);

	/**
	 * Gets a selected state value from this EnhancedRandom. The number of possible selections
	 * is up to the implementing class, but negative values for {@code selection} are
	 * typically not tolerated. This should return the exact value of the selected state,
	 * assuming it is implemented. The default implementation throws an
	 * UnsupportedOperationException, and implementors only have to allow reading the state if
	 * they choose to implement this differently.
	 * @param selection used to select which state variable to get; generally non-negative
	 * @return the exact value of the selected state
	 */
	default long getSelectedState(int selection) {
		throw new UnsupportedOperationException("getSelectedState() not supported.");
	}

	/**
	 * Sets a selected state value to the given long {@code value}. The number of possible
	 * selections is up to the implementing class, but negative values for {@code selection}
	 * are typically not tolerated. Implementors are permitted to change {@code value} if it
	 * is not valid, but they should not alter it if it is valid.
	 * @param selection used to select which state variable to set; generally non-negative
	 * @param value the exact value to use for the selected state, if valid
	 */
	default void setSelectedState(int selection, long value) {
		setSeed(value);
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
	default int next (int bits) {
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
	default void nextBytes (byte[] bytes) {
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
	default int nextInt () {
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
	default int nextInt (int bound) {
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
	 * @see #nextInt(int) Here's a note about the bias present in the bounded generation.
	 * @param outerBound the outer exclusive bound; may be any int value, allowing negative
	 * @return a pseudorandom int between 0 (inclusive) and outerBound (exclusive)
	 */
	default int nextSignedInt (int outerBound) {
		outerBound = (int)(outerBound * (nextLong() & 0xFFFFFFFFL) >> 32);
		return outerBound + (outerBound >>> 31);
	}

	/**
	 * Returns a pseudorandom, uniformly distributed {@code int} value between the
	 * specified {@code innerBound} (inclusive) and the specified {@code outerBound}
	 * (exclusive). If {@code outerBound} is less than or equal to {@code innerBound},
	 * this always returns {@code innerBound}.
	 *
	 * <br> For any case where outerBound might be valid but less than innerBound, you
	 * can use {@link #nextSignedInt(int, int)}.
	 *
	 * @see #nextInt(int) Here's a note about the bias present in the bounded generation.
	 * @param innerBound the inclusive inner bound; may be any int, allowing negative
	 * @param outerBound the exclusive outer bound; must be greater than innerBound (otherwise this returns innerBound)
	 * @return a pseudorandom int between innerBound (inclusive) and outerBound (exclusive)
	 */
	default int nextInt (int innerBound, int outerBound) {
		return innerBound + nextInt(outerBound - innerBound);
	}

	/**
	 * Returns a pseudorandom, uniformly distributed {@code int} value between the
	 * specified {@code innerBound} (inclusive) and the specified {@code outerBound}
	 * (exclusive). This is meant for cases where either bound may be negative,
	 * especially if the bounds are unknown or may be user-specified. It is slightly
	 * slower than {@link #nextInt(int, int)}.
	 *
	 * @see #nextInt(int) Here's a note about the bias present in the bounded generation.
	 * @param innerBound the inclusive inner bound; may be any int, allowing negative
	 * @param outerBound the exclusive outer bound; may be any int, allowing negative
	 * @return a pseudorandom int between innerBound (inclusive) and outerBound (exclusive)
	 */
	default int nextSignedInt (int innerBound, int outerBound) {
		return innerBound + nextSignedInt(outerBound - innerBound);
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
	long nextLong ();

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
	default long nextLong (long bound) {
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
	default long nextSignedLong (long outerBound) {
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
	default long nextLong (long inner, long outer) {
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
	default long nextSignedLong (long inner, long outer) {
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
	default boolean nextBoolean () {
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
	default float nextFloat () {
		return (nextLong() >>> 40) * 0x1p-24f;
	}

	/**
	 * Gets a pseudo-random float between 0 (inclusive) and {@code outerBound} (exclusive).
	 * The outerBound may be positive or negative.
	 * Exactly the same as {@code nextFloat() * outerBound}.
	 * @param outerBound the exclusive outer bound
	 * @return a float between 0 (inclusive) and {@code outerBound} (exclusive)
	 */
	default float nextFloat (float outerBound) {
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
	default float nextFloat (float innerBound, float outerBound) {
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
	default double nextDouble () {
		return (nextLong() >>> 11) * 0x1.0p-53;
	}

	/**
	 * Gets a pseudo-random double between 0 (inclusive) and {@code outerBound} (exclusive).
	 * The outerBound may be positive or negative.
	 * Exactly the same as {@code nextDouble() * outerBound}.
	 * @param outerBound the exclusive outer bound
	 * @return a double between 0 (inclusive) and {@code outerBound} (exclusive)
	 */
	default double nextDouble (double outerBound) {
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
	default double nextDouble (double innerBound, double outerBound) {
		return innerBound + nextDouble() * (outerBound - innerBound);
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
	 *
	 * <p>This uses an approximation as implemented by {@link #probit(double)},
	 * which can't produce as extreme results in extremely-rare cases as methods
	 * like Box-Muller and Marsaglia Polar can. All but one possible result are
	 * between {@code -8.209536145151493} and {@code 8.209536145151493}, and the
	 * one result outside that is {@code -38.5}.
	 *
	 * @return the next pseudorandom, Gaussian ("normally") distributed
	 * {@code double} value with mean {@code 0.0} and
	 * standard deviation {@code 1.0} from this random number
	 * generator's sequence
	 */
	default double nextGaussian () {
		return probit(nextDouble());
	}

	/**
	 * Advances or rolls back the {@code LaserRandom}' state without actually generating each number. Skips forward
	 * or backward a number of steps specified by advance, where a step is equal to one call to {@link #nextLong()},
	 * and returns the random number produced at that step. Negative numbers can be used to step backward, or 0 can be
	 * given to get the most-recently-generated long from {@link #nextLong()}.
	 *
	 * <p>The default implementation throws an UnsupportedOperationException. Many types of random
	 * number generator do not have an efficient way of skipping arbitrarily through the state sequence,
	 * and those types should not implement this method differently.
	 *
	 * @param advance Number of future generations to skip over; can be negative to backtrack, 0 gets the most-recently-generated number
	 * @return the random long generated after skipping forward or backwards by {@code advance} numbers
	 */
	default long skip (long advance) {
		throw new UnsupportedOperationException("skip() not supported.");
	}

	/**
	 * Creates a new EnhancedRandom with identical states to this one, so if the same EnhancedRandom methods are
	 * called on this object and its copy (in the same order), the same outputs will be produced. This is not
	 * guaranteed to copy the inherited state of any parent class, so if you call methods that are
	 * only implemented by a superclass (like {@link Random}) and not this one, the results may differ.
	 * @return a deep copy of this LaserRandom.
	 */
	EnhancedRandom copy ();

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
	 * {@link Random#nextGaussian()} generates at least two random doubles for each two Gaussian values, but may rarely
	 * require much more random generation.
	 * <br>
	 * This can be used both as an optimization for generating Gaussian random values, and as a way of generating
	 * Gaussian values that match a pattern present in the inputs (which you could have by using a sub-random sequence
	 * as the input, such as those produced by a van der Corput, Halton, Sobol or R2 sequence). Most methods of generating
	 * Gaussian values (e.g. Box-Muller and Marsaglia polar) do not have any way to preserve a particular pattern.
	 *
	 * @param d should be between 0 and 1, exclusive, but other values are tolerated
	 * @return a normal-distributed double centered on 0.0; all results will be between -38.5 and 38.5, both inclusive
	 */
	static double probit (final double d) {
		if (d <= 0 || d >= 1) {
			return Math.copySign(38.5, d - 0.5);
		}
		else if (d < 0.02425) {
			final double q = Math.sqrt(-2.0 * Math.log(d));
			return (((((-7.784894002430293e-03 * q + -3.223964580411365e-01) * q + -2.400758277161838e+00) * q + -2.549732539343734e+00) * q + 4.374664141464968e+00) * q + 2.938163982698783e+00) / (
				(((7.784695709041462e-03 * q + 3.224671290700398e-01) * q + 2.445134137142996e+00) * q + 3.754408661907416e+00) * q + 1.0);
		}
		else if (0.97575 < d) {
			final double q = Math.sqrt(-2.0 * Math.log(1 - d));
			return -(((((-7.784894002430293e-03 * q + -3.223964580411365e-01) * q + -2.400758277161838e+00) * q + -2.549732539343734e+00) * q + 4.374664141464968e+00) * q + 2.938163982698783e+00) / (
				(((7.784695709041462e-03 * q + 3.224671290700398e-01) * q + 2.445134137142996e+00) * q + 3.754408661907416e+00) * q + 1.0);
		}
		else {
			final double q = d - 0.5;
			final double r = q * q;
			return (((((-3.969683028665376e+01 * r + 2.209460984245205e+02) * r + -2.759285104469687e+02) * r + 1.383577518672690e+02) * r + -3.066479806614716e+01) * r + 2.506628277459239e+00) * q / (
				((((-5.447609879822406e+01 * r + 1.615858368580409e+02) * r + -1.556989798598866e+02) * r + 6.680131188771972e+01) * r + -1.328068155288572e+01) * r + 1.0);
		}
	}

	/**
	 * Returns true if a random value between 0 and 1 is less than the specified value.
	 *
	 * @param chance a float between 0.0 and 1.0; higher values are more likely to result in true
	 * @return a boolean selected with the given {@code chance} of being true
	 */
	default boolean nextBoolean (float chance) {
		return nextFloat() < chance;
	}

	/**
	 * Returns -1 or 1, randomly.
	 *
	 * @return -1 or 1, selected with approximately equal likelihood
	 */
	default int nextSign () {
		return 1 | nextInt() >> 31;
	}

	/**
	 * Returns a triangularly distributed random number between -1.0 (exclusive) and 1.0 (exclusive), where values around zero are
	 * more likely. Advances the state twice.
	 * <p>
	 * This is an optimized version of {@link #nextTriangular(float, float, float) randomTriangular(-1, 1, 0)}
	 */
	default float nextTriangular () {
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
	default float nextTriangular (float max) {
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
	default float nextTriangular (float min, float max) {
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
	default float nextTriangular (float min, float max, float mode) {
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
	default <T> T randomElement (T[] array) {
		return array[nextInt(array.length)];
	}

	/**
	 * Shuffles the given array in-place pseudo-randomly, using this to determine how to shuffle.
	 *
	 * @param items an int array; must be non-null
	 */
	default void shuffle (int[] items) {
		for (int i = items.length - 1; i >= 0; i--) {
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
	default void shuffle (long[] items) {
		for (int i = items.length - 1; i >= 0; i--) {
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
	default void shuffle (float[] items) {
		for (int i = items.length - 1; i >= 0; i--) {
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
	default void shuffle (char[] items) {
		for (int i = items.length - 1; i >= 0; i--) {
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
	default void shuffle (double[] items) {
		for (int i = items.length - 1; i >= 0; i--) {
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
	default void shuffle (short[] items) {
		for (int i = items.length - 1; i >= 0; i--) {
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
	default void shuffle (boolean[] items) {
		for (int i = items.length - 1; i >= 0; i--) {
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
	default <T> void shuffle (T[] items) {
		for (int i = items.length - 1; i >= 0; i--) {
			int ii = nextInt(i + 1);
			T temp = items[i];
			items[i] = items[ii];
			items[ii] = temp;
		}
	}

}

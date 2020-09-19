package com.github.tommyettinger.ds;

import java.io.Serializable;
import java.util.Random;

/**
 * A faster and much-higher-quality substitute for {@link Random}.
 * Like how a laser is made of many beams of photons that don't cross paths, this allows many different
 * random number streams that don't overlap either.
 * <br>
 * Pew pew!
 */
public class LaserRandom extends Random implements Serializable {
	private static final long serialVersionUID = 0L;
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
		stateB =  super.nextLong() | 1L;
	}

	/**
	 * Creates a new LaserRandom using a single {@code long} seed; the stream depends on whether the seed is even or odd.
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
	 * @param seedA any long; will be used exactly to set stateA as with {@link #setStateA(long)}
	 * @param seedB any odd long will be used exactly to set stateB, otherwise, as with {@link #setStateB(long)}, it will be made odd
	 */
	public LaserRandom(final long seedA, final long seedB) {
		super(seedA);
		stateA = seedA;
		stateB = seedB | 1L;
	}

	/**
	 * Get the "A" part of the internal state as a long.
	 *
	 * @return the current internal "A" state of this object.
	 */
	public long getStateA() {
		return stateA;
	}

	/**
	 * Set the "A" part of the internal state with a long.
	 *
	 * @param stateA a 64-bit long
	 */
	public void setStateA(long stateA) {
		this.stateA = stateA;
	}

	/**
	 * Get the "B" part of the internal state as a long.
	 *
	 * @return the current internal "B" state of this object.
	 */
	public long getStateB() {
		return stateB;
	}

	/**
	 * Set the "B" part of the internal state with a long; the least significant bit is ignored (will always be odd).
	 * That is, if stateB is odd, this uses it verbatim; if stateB is even, it adds 1 to it to make it odd.
	 *
	 * @param stateB a 64-bit long
	 */
	public void setStateB(long stateB) {
		this.stateB = stateB | 1L;
	}

	/**
	 * Sets the seed of this random number generator using a single
	 * {@code long} seed. The general contract of {@code setSeed} is
	 * that it alters the state of this random number generator object
	 * so as to be in exactly the same state as if it had just been
	 * created with the argument {@code seed} as a seed. 
	 *
	 * <p>The implementation of {@code setSeed} by class {@code Random}
	 * uses all 64 bits of the given seed. In general, however,
	 * an overriding method may use all 64 bits of the {@code long}
	 * argument as a seed value.
	 *
	 * @param seed the initial seed
	 */
	@Override
	public void setSeed (long seed) {
		stateB = (stateA = seed) | 1L; 
	}

	/**
	 * Generates the next pseudorandom number with a specific maximum size in bits (not a max value).
	 * If you want to get a random number in a range, use {@link #nextInt(int)} instead.
	 * 
	 * <p>The general contract of {@code next} is that it returns an
	 * {@code int} value and if the argument {@code bits} is between
	 * {@code 1} and {@code 32} (inclusive), then that many low-order
	 * bits of the returned value will be (approximately) independently
	 * chosen bit values, each of which is (approximately) equally
	 * likely to be {@code 0} or {@code 1}.
	 * <p>
	 *
	 * @param bits random bits
	 * @return the next pseudorandom value from this random number
	 * generator's sequence
	 */
	@Override
	public int next (int bits) {
		final long s = (stateA += 0xC6BC279692B5C323L);
		final long z = (s ^ s >>> 31) * (stateB += 0x9E3779B97F4A7C16L);
		return (int)(z ^ z >>> 26 ^ z >>> 6) >>> (32 - bits);
	}

	/**
	 * Generates random bytes and places them into a user-supplied
	 * byte array.  The number of random bytes produced is equal to
	 * the length of the byte array.
	 *
	 * @param bytes the byte array to fill with random bytes
	 * @throws NullPointerException if the byte array is null
	 */
	@Override
	public void nextBytes (byte[] bytes) {
		for (int i = 0; i < bytes.length; )
			for (long r = nextLong(), n = Math.min(bytes.length - i, 8); n-- > 0; r >>>= 8)
				bytes[i++] = (byte) r;
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
	@Override
	public int nextInt () {
		final long s = (stateA += 0xC6BC279692B5C323L);
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
	 * 
	 * @param bound the upper bound (exclusive). If negative or 0, this always returns 0.
	 * @return the next pseudorandom, uniformly distributed {@code int}
	 * value between zero (inclusive) and {@code bound} (exclusive)
	 * from this random number generator's sequence
	 */
	@Override
	public int nextInt (int bound) {
		final long s = (stateA += 0xC6BC279692B5C323L);
		final long z = (s ^ s >>> 31) * (stateB += 0x9E3779B97F4A7C16L);
		return (int) ((bound * ((z ^ z >>> 26 ^ z >>> 6) & 0xFFFFFFFFL)) >> 32) & ~(bound >> 31);
	}

	/**
	 * Returns the next pseudorandom, uniformly distributed {@code long}
	 * value from this random number generator's sequence. The general
	 * contract of {@code nextLong} is that one {@code long} value is
	 * pseudorandomly generated and returned.
	 * <p>
	 * An individual {@code LaserRNG} can't return all 18-quintillion possible {@code long} values,
	 * but the full set of 9-quintillion possible random number streams that this class can produce will,
	 * as a whole, produce all {@code long} values with equal likelihood.
	 *
	 * @return the next pseudorandom, uniformly distributed {@code long}
	 * value from this random number generator's sequence
	 */
	@Override
	public long nextLong () {
		final long s = (stateA += 0xC6BC279692B5C323L);
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
	 * probability.
	 * 
	 * <p>Note that this advances the state by the same amount as a single call to
	 * {@link #nextLong()}, which allows methods like {@link #skip(long)} to function
	 * correctly, but introduces some bias when {@code bound} is very large. This will
	 * also advance the state if {@code bound} is 0 or negative, so usage with a variable
	 * bound will advance the state reliably.
	 * @param bound the upper bound (exclusive). If negative or 0, this always returns 0.
	 * @return the next pseudorandom, uniformly distributed {@code long}
	 * value between zero (inclusive) and {@code bound} (exclusive)
	 * from this random number generator's sequence
	 */
	public long nextLong(long bound) {
		final long s = (stateA += 0xC6BC279692B5C323L);
		final long z = (s ^ s >>> 31) * (stateB += 0x9E3779B97F4A7C16L);
		if (bound <= 0) return 0;
		long rand = z ^ z >>> 26 ^ z >>> 6;
		final long randLow = rand & 0xFFFFFFFFL;
		final long boundLow = bound & 0xFFFFFFFFL;
		rand >>>= 32;
		bound >>>= 32;
		final long a = rand * bound;
		final long b = randLow * boundLow;
		return (((b >>> 32) + (rand + randLow) * (bound + boundLow) - a - b) >>> 32) + a;
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
	@Override
	public boolean nextBoolean () {
		final long s = (stateA += 0xC6BC279692B5C323L);
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
	@Override
	public float nextFloat () {
		final long s = (stateA += 0xC6BC279692B5C323L);
		final long z = (s ^ s >>> 31) * (stateB += 0x9E3779B97F4A7C16L);
		return ((z ^ z >>> 6) >>> 40) * 0x1p-24f;
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
	@Override
	public double nextDouble () {
		final long s = (stateA += 0xC6BC279692B5C323L);
		final long z = (s ^ s >>> 31) * (stateB += 0x9E3779B97F4A7C16L);
		return ((z ^ z >>> 26 ^ z >>> 6) >>> 11) * 0x1.0p-53;
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
	 * one result outside that is {@code 38.5}.
	 * @return the next pseudorandom, Gaussian ("normally") distributed
	 * {@code double} value with mean {@code 0.0} and
	 * standard deviation {@code 1.0} from this random number
	 * generator's sequence
	 */
	@Override
	public synchronized double nextGaussian () {
		return probit(nextDouble());
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
	public long skip(long advance) {
		final long s = (stateA += 0xC6BC279692B5C323L * advance);
		final long z = (s ^ s >>> 31) * (stateB += 0x9E3779B97F4A7C16L * advance);
		return z ^ z >>> 26 ^ z >>> 6;
	}
	
	public LaserRandom copy(){
		return new LaserRandom(stateA, stateB);
	}
	
	/**
	 * Gets a long that identifies which stream of numbers this generator is producing; this stream identifier is always
	 * an odd long and won't change by generating numbers. It is determined at construction and will usually (not
	 * always) change if {@link #setStateA(long)} or {@link #setStateB(long)} are called. Each stream is a
	 * probably-unique sequence of 2 to the 64 longs, where approximately 1/3 of all possible longs will not ever occur
	 * (while others occur twice or more), but this set of results is different for every stream. There are 2 to the 63
	 * possible streams, one for every odd long.
	 * @return an odd long that identifies which stream this LaserRandom is generating from
	 */
	public long getStream()
	{
		return stateB - (stateA * 0x1743CE5C6E1B848BL) * 0x9E3779B97F4A7C16L;
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
	 * {@link Random#nextGaussian()} generates at least two random doubles for each two Gaussian values, but may rarely
	 * require much more random generation.
	 * <br>
	 * This can be used both as an optimization for generating Gaussian random values, and as a way of generating
	 * Gaussian values that match a pattern present in the inputs (which you could have by using a sub-random sequence
	 * as the input, such as those produced by a van der Corput, Halton, Sobol or R2 sequence). Most methods of generating
	 * Gaussian values (e.g. Box-Muller and Marsaglia polar) do not have any way to preserve a particular pattern.
	 * @param d should be between 0 and 1, exclusive, but other values are tolerated
	 * @return a normal-distributed double centered on 0.0; all results will be between -38.5 and 38.5, both inclusive
	 */
	public static double probit(final double d) {
		if (d <= 0 || d >= 1) {
			return Math.copySign(38.5, d - 0.5);
		}
		// Rational approximation for lower region:
		else if (d < 0.02425) {
			final double q = Math.sqrt(-2.0 * Math.log(d));
			return (((((-7.784894002430293e-03 * q + -3.223964580411365e-01) * q + -2.400758277161838e+00) * q + -2.549732539343734e+00) * q + 4.374664141464968e+00) * q + 2.938163982698783e+00) / (
				(((7.784695709041462e-03 * q + 3.224671290700398e-01) * q + 2.445134137142996e+00) * q + 3.754408661907416e+00) * q + 1.0);
		}
		// Rational approximation for upper region:
		else if (0.97575 < d) {
			final double q = Math.sqrt(-2.0 * Math.log(1 - d));
			return -(((((-7.784894002430293e-03 * q + -3.223964580411365e-01) * q + -2.400758277161838e+00) * q + -2.549732539343734e+00) * q + 4.374664141464968e+00) * q + 2.938163982698783e+00) / (
				(((7.784695709041462e-03 * q + 3.224671290700398e-01) * q + 2.445134137142996e+00) * q + 3.754408661907416e+00) * q + 1.0);
		}
		// Rational approximation for central region:
		else {
			final double q = d - 0.5;
			final double r = q * q;
			return (((((-3.969683028665376e+01 * r + 2.209460984245205e+02) * r + -2.759285104469687e+02) * r + 1.383577518672690e+02) * r + -3.066479806614716e+01) * r + 2.506628277459239e+00) * q / (
				((((-5.447609879822406e+01 * r + 1.615858368580409e+02) * r + -1.556989798598866e+02) * r + 6.680131188771972e+01) * r + -1.328068155288572e+01) * r + 1.0);
		}
	}
}

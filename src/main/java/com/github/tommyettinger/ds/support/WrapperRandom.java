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
	public final EnhancedRandom rng;

	/**
	 * Creates a WrapperRandom around a {@link FourWheelRandom} with a random seed.
	 */
	public WrapperRandom () {
		this(EnhancedRandom.seedFromMath());
	}

	/**
	 * Uses the given EnhancedRandom for all operations it can (everything but Streams), and seeds the inherited Random state
	 * with the given rng's state 0.
	 * @param rng
	 */
	public WrapperRandom (@Nonnull EnhancedRandom rng) {
		super(rng.getSelectedState(0));
		this.rng = rng;
	}

	/**
	 * Uses the given EnhancedRandom for all operations it can (everything but Streams), and seeds both that EnhancedRandom
	 * and the inherited Random state with the given seed.
	 * @param rng
	 * @param seed
	 */
	public WrapperRandom (@Nonnull EnhancedRandom rng, long seed) {
		super(seed);
		rng.setSeed(seed);
		this.rng = rng;
	}

	/**
	 * Creates a WrapperRandom around a {@link FourWheelRandom}, seeded with the given seed.
	 * @param seed a long to be used as a seed.
	 */
	public WrapperRandom (long seed) {
		super(seed);
		this.rng = new FourWheelRandom(seed);
	}

	@Override
	public void setSeed (long seed) {
		super.setSeed(seed);
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

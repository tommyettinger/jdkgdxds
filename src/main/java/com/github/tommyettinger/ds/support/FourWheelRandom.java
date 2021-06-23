package com.github.tommyettinger.ds.support;

import java.util.Random;

/**
 * A random number generator that is extremely fast on Java 16, and has a very large probable period.
 * This generator is measurably faster than {@link TricycleRandom} on Java 16 but slightly slower than it on Java 8.
 * Not stable currently; API, algorithm, and results may change at any time. Testing performed is limited so far, but
 * this passes at least 16TB of PractRand and 125TB of hwd without issues.
 */
public class FourWheelRandom extends Random implements EnhancedRandom {

    /**
     * The first state; can be any long.
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
     * The fourth state; can be any long. If this has just been set to some value, then the next call to
     * {@link #nextLong()} will return that value as-is. Later calls will be more random.
     */
    protected long stateD;

    /**
     * Creates a new TricycleRandom with a random state.
     */
    public FourWheelRandom () {
        super();
        stateA = super.nextLong();
        stateB = super.nextLong();
        stateC = super.nextLong();
        stateD = super.nextLong();
    }

    /**
     * Creates a new TricycleRandom with the given seed; all {@code long} values are permitted.
     * The seed will be passed to {@link #setSeed(long)} to attempt to adequately distribute the seed randomly.
     * @param seed any {@code long} value
     */
    public FourWheelRandom (long seed) {
        super(seed);
        setSeed(seed);
    }

    /**
     * Creates a new TricycleRandom with the given three states; all {@code long} values are permitted.
     * These states will be used verbatim.
     * @param stateA any {@code long} value
     * @param stateB any {@code long} value
     * @param stateC any {@code long} value
     */
    public FourWheelRandom (long stateA, long stateB, long stateC, long stateD) {
        super(stateA + stateB ^ stateC - stateD);
        this.stateA = stateA;
        this.stateB = stateB;
        this.stateC = stateC;
        this.stateD = stateD;
    }

    /**
     * This generator has 4 {@code long} states, so this returns 4.
     * @return 4 (four)
     */
    public int getStateCount() {
        return 4;
    }

    /**
     * Gets the state determined by {@code selection}, as-is. The value for selection should be
     * between 0 and 3, inclusive; if it is any other value this gets state D as if 3 was given.
     * @param selection used to select which state variable to get; generally 0, 1, 2, or 3
     * @return the value of the selected state
     */
    public long getSelectedState(int selection) {
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
     * @param selection used to select which state variable to set; generally 0, 1, 2, or 3
     * @param value the exact value to use for the selected state, if valid
     */
    public void setSelectedState(int selection, long value) {
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
     * first value returned by {@link #nextLong()} (because {@code stateD} is guaranteed to be
     * different for every different {@code seed}).
     * @param seed the initial seed; may be any long
     */
    public void setSeed(long seed) {
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
        x = (seed += 0x9E3779B97F4A7C15L);
        x ^= x >>> 27;
        x *= 0x3C79AC492BA7B653L;
        x ^= x >>> 33;
        x *= 0x1C69B3F74AC4AE35L;
        stateC = x ^ x >>> 27;
        x = (seed + 0x9E3779B97F4A7C15L);
        x ^= x >>> 27;
        x *= 0x3C79AC492BA7B653L;
        x ^= x >>> 33;
        x *= 0x1C69B3F74AC4AE35L;
        stateD = x ^ x >>> 27;
    }

    public long getStateA() {
        return stateA;
    }

    /**
     * Sets the first part of the state.
     * @param stateA can be any long
     */
    public void setStateA(long stateA) {
        this.stateA = stateA;
    }

    public long getStateB() {
        return stateB;
    }

    /**
     * Sets the second part of the state.
     * @param stateB can be any long
     */
    public void setStateB(long stateB) {
        this.stateB = stateB;
    }

    public long getStateC() {
        return stateC;
    }

    /**
     * Sets the third part of the state.
     * @param stateC can be any long
     */
    public void setStateC(long stateC) {
        this.stateC = stateC;
    }

    public long getStateD() {
        return stateD;
    }

    /**
     * Sets the fourth part of the state. Note that if you call {@link #nextLong()}
     * immediately after this, it will return the given {@code stateD} as-is, so you
     * may want to call some random generation methods (such as nextLong()) and discard
     * the results after setting the state.
     * @param stateD can be any long
     */
    public void setStateD(long stateD) {
        this.stateD = stateD;
    }

    /**
     * Sets the state completely to the given three state variables.
     * This is the same as calling {@link #setStateA(long)}, {@link #setStateB(long)},
     * {@link #setStateC(long)}, and {@link #setStateD(long)} as a group. You may want
     * to call {@link #nextLong()} a few times after setting the states like this, unless
     * the value for stateD (in particular) is already adequately random; the first call
     * to {@link #nextLong()}, if it is made immediately after calling this, will return {@code stateD} as-is.
     * @param stateA the first state; can be any long
     * @param stateB the second state; can be any long
     * @param stateC the third state; can be any long
     * @param stateD the fourth state; this will be returned as-is if the next call is to {@link #nextLong()}
     */
    public void setState(long stateA, long stateB, long stateC, long stateD) {
        this.stateA = stateA;
        this.stateB = stateB;
        this.stateC = stateC;
        this.stateD = stateD;
    }

    public long nextLong() {
        final long fa = this.stateA;
        final long fb = this.stateB;
        final long fc = this.stateC;
        final long fd = this.stateD;
        this.stateA = 0xD1342543DE82EF95L * fd;
        this.stateB = fa + 0xC6BC279692B5C323L;
        this.stateC = Long.rotateLeft(fb, 47) - fd;
        this.stateD = fb ^ fc;
        return fd;
    }

    public int next(int bits) {
        final long fa = this.stateA;
        final long fb = this.stateB;
        final long fc = this.stateC;
        final long fd = this.stateD;
        this.stateA = 0xD1342543DE82EF95L * fd;
        this.stateB = fa + 0xC6BC279692B5C323L;
        this.stateC = Long.rotateLeft(fb, 47) - fd;
        this.stateD = fb ^ fc;
        return (int)fd >>> (32 - bits);
    }

    public FourWheelRandom copy() {
        return new FourWheelRandom(stateA, stateB, stateC, stateD);
    }

    public void nextBytes(byte[] bytes) {
        EnhancedRandom.super.nextBytes(bytes);
    }

    public int nextInt() {
        return EnhancedRandom.super.nextInt();
    }

    public int nextInt(int bound) {
        return EnhancedRandom.super.nextInt(bound);
    }

    public boolean nextBoolean() {
        return EnhancedRandom.super.nextBoolean();
    }

    public float nextFloat() {
        return EnhancedRandom.super.nextFloat();
    }

    public double nextDouble() {
        return EnhancedRandom.super.nextDouble();
    }

    public double nextGaussian() {
        return EnhancedRandom.super.nextGaussian();
    }

    @Override
    public boolean equals (Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        FourWheelRandom that = (FourWheelRandom)o;

        if (stateA != that.stateA)
            return false;
        if (stateB != that.stateB)
            return false;
        if (stateC != that.stateC)
            return false;
        return stateD == that.stateD;
    }

    public String toString() {
        return "TricycleRandom{" +
                "stateA=" + stateA +
                "L, stateB=" + stateB +
                "L, stateC=" + stateC +
                "L, stateC=" + stateD +
                "L}";
    }
}

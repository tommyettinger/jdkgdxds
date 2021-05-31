package com.github.tommyettinger.ds.support;

import java.util.Random;

/**
 * An unusual RNG that's extremely fast on HotSpot JDK 16 and higher, and still fairly fast on earlier JDKs. It has
 * three {@code long} states, which as far as I can tell can be initialized to any values without hitting any known
 * problems for initialization. These states, a, b, and c, are passed around so b and c affect the next result for a,
 * but the previous value of a will only affect the next results for b and c, not a itself. The next values for b and c
 * involve a rotation with {@link Long#rotateLeft(long, int)} for one state, and an extra operation using another state
 * (b uses add, c uses subtract). The next value for a is {@code (c + 0xC6BC279692B5C323L) ^ b}, where the long constant
 * is a probable prime with an equal number of 1 and 0 bits, but is otherwise arbitrary.
 * <br>
 * This complicated transfer of states happens to be optimized very nicely by recent JVM versions (mostly for HotSpot,
 * but OpenJ9 also does well), since a, b, and c can all be updated in parallel instructions. It passes 64TB of
 * PractRand testing with no anomalies and also passes Dieharder. It's also really fast, and is one of only a few
 * generators I have benchmarked as producing more than 1 billion longs per second on a HotSpot JVM (on a recent laptop).
 * <br>
 * Other useful traits of this generator are that it almost certainly has a longer period than you need for a game, and
 * that all values are permitted for the states (that we know of). It is possible that some initialization will put the
 * generator in a shorter-period subcycle, but the odds of this being a subcycle that's small enough to run out of
 * period during a game are effectively 0. It's also possible that the generator only has one cycle of length 2 to the
 * 192, though this doesn't seem likely.
 * <br>
 * This is closely related to Mark Overton's <a href="https://www.romu-random.org/">Romu generators</a>, specifically
 * RomuTrio, but this gets a little faster than RomuTrio by using just one less rotation. Unlike RomuTrio, there isn't a
 * clear problematic state with a period of 1 (which happens when all of its states are 0). This generator isn't an ARX
 * generator any more (a previous version was), but its performance isn't much different (like RomuTrio, the one
 * multiplication this uses pipelines very well, so it doesn't slow down the generator).
 * <br>
 * TricycleRandom passes 64TB of testing with PractRand, which uses a suite of tests to look for a variety of potential
 * problems. Testing a larger amount of random data needs quite a bit more memory than I currently have, at least with
 * PractRand -- another test, hwd, can test a much larger amount of data but only uses a single test. The test hwd uses
 * looks for long-range bit-dependencies, where one bit's state earlier in the generated numbers determines the state of
 * a future bit with a higher-than-reasonable likelihood. A previous version of TricycleRandom passed PractRand, also to
 * 64TB, but failed hwd around 400 TB of data. This version of TricycleRandom
 * <b>TricycleRandom isn't yet considered a stable API</b> as a result, and its algorithm may change (along with the
 * numbers it produces) in future versions. Both {@link LaserRandom} and {@link DistinctRandom} are considered stable.
 * <br>
 * This can be used as a substitute for {@link LaserRandom}, but it doesn't start out randomizing its early results very
 * well, unlike LaserRandom. If you initialize this with {@link #setSeed(long)}, then the results should be random from
 * the start, and unrelated to the original seed. It can also be more of a challenge to handle 3 states than 2 in some
 * situations, or 1 state for DistinctRandom. You might prefer LaserRandom's many different streams, which shouldn't
 * overlap and have a guaranteed period of 2 to the 64, to TricycleRandom's big unknown sub-cycles. LaserRandom and
 * DistinctRandom can also {@link LaserRandom#skip(long)} while this cannot.
 */
public class TricycleRandom extends Random implements EnhancedRandom {

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
        super();
        stateA = super.nextLong();
        stateB = super.nextLong();
        stateC = super.nextLong();
    }

    /**
     * Creates a new TricycleRandom with the given seed; all {@code long} values are permitted.
     * The seed will be passed to {@link #setSeed(long)} to attempt to adequately distribute the seed randomly.
     * @param seed any {@code long} value
     */
    public TricycleRandom (long seed) {
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
    public TricycleRandom (long stateA, long stateB, long stateC) {
        super(stateA + stateB ^ stateC);
        this.stateA = stateA;
        this.stateB = stateB;
        this.stateC = stateC;
    }

    /**
     * This generator has 3 {@code long} states, so this returns 3.
     * @return 3 (three)
     */
    public int getStateCount() {
        return 3;
    }

    /**
     * Gets the state determined by {@code selection}, as-is.
     * @param selection used to select which state variable to get; generally 0, 1, or 2
     * @return the value of the selected state
     */
    public long getSelectedState(int selection) {
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
     * @param selection used to select which state variable to set; generally 0, 1, or 2
     * @param value the exact value to use for the selected state, if valid
     */
    public void setSelectedState(int selection, long value) {
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
        x = (seed + 0x9E3779B97F4A7C15L);
        x ^= x >>> 27;
        x *= 0x3C79AC492BA7B653L;
        x ^= x >>> 33;
        x *= 0x1C69B3F74AC4AE35L;
        stateC = x ^ x >>> 27;
    }

    public long getStateA() {
        return stateA;
    }

    /**
     * Sets the first part of the state. Note that if you call {@link #nextLong()}
     * immediately after this, it will return the given {@code stateA} as-is, so you
     * may want to call some random generation methods (such as nextLong()) and discard
     * the results after setting the state.
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

    /**
     * Sets the state completely to the given three state variables.
     * This is the same as calling {@link #setStateA(long)}, {@link #setStateB(long)},
     * and {@link #setStateC(long)} as a group. You may want to call {@link #nextLong()}
     * a few times after setting the states like this, unless the value for stateA (in
     * particular) is already adequately random; the first call to {@link #nextLong()},
     * if it is made immediately after calling this, will return {@code stateA} as-is.
     * @param stateA the first state; this will be returned as-is if the next call is to {@link #nextLong()}
     * @param stateB the second state; can be any long
     * @param stateC the third state; can be any long
     */
    public void setState(long stateA, long stateB, long stateC) {
        this.stateA = stateA;
        this.stateB = stateB;
        this.stateC = stateC;
    }

    public long nextLong() {
        final long fa = this.stateA;
        final long fb = this.stateB;
        final long fc = this.stateC;
        this.stateA = 0xD1342543DE82EF95L * fc;
        this.stateB = fa ^ fb ^ fc;
        this.stateC = Long.rotateLeft(fb, 41) + 0xC6BC279692B5C323L;
        return fa;
    }

    public int next(int bits) {
        final long fa = this.stateA;
        final long fb = this.stateB;
        final long fc = this.stateC;
        this.stateA = 0xD1342543DE82EF95L * fc;
        this.stateB = fa ^ fb ^ fc;
        this.stateC = Long.rotateLeft(fb, 41) + 0xC6BC279692B5C323L;
        return (int)fa >>> (32 - bits);
    }

    public TricycleRandom copy() {
        return new TricycleRandom(stateA, stateB, stateC);
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

        TricycleRandom that = (TricycleRandom)o;

        if (stateA != that.stateA)
            return false;
        if (stateB != that.stateB)
            return false;
        return stateC == that.stateC;
    }

    public String toString() {
        return "TricycleRandom{" +
                "stateA=" + stateA +
                "L, stateB=" + stateB +
                "L, stateC=" + stateC +
                "L}";
    }
}

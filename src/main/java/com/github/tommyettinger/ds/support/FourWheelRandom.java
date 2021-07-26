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
 * A random number generator that is extremely fast on Java 16, and has a very large probable period.
 * This generator is measurably faster than {@link TricycleRandom} on Java 16 but slightly slower than it on Java 8.
 * It can be considered stable, like the other EnhancedRandom implementations here. Testing performed should be sufficient,
 * but more can always be done; this passes at least 64TB of PractRand and 2PB of hwd without issues. The second test, hwd,
 * only checks for a specific type of quality issue, but also fails if the period is exhausted; going through 2 to the 52
 * bytes of data (taking over a week to do so) without exhausting the period should be a strong sign that it will have
 * enough period for most tasks.
 * <br>
 * The algorithm used here has four states purely to exploit instruction-level parallelism; it isn't trying to extend the
 * period of the generator beyond about 2 to the 64 (the expected bare minimum, though some cycles will likely be much
 * longer). There's a complex tangle of dependencies across the four states, but it is possible to invert the generator
 * given a full 256-bit state; this is vital for its period and quality. State A and state B operate like a staggered LCG
 * that starts with stateD; this part is why 2 to the 64 is expected as the bare minimum period. State C and state D take
 * two of the other states and combine them; C rotates state B and subtracts state D, while D simply XORs states B and C.
 * This returns the state D that the previous step generated. This performs better than TricycleRandom simply because each
 * of the states can be updated in parallel (using ILP) and all of the updates depend on either one or two states, instead
 * of one, two, or three with TricycleRandom. It implements all optional methods in EnhancedRandom except
 * {@link #skip(long)}; it does implement {@link #previousLong()} without using skip().
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
     * Creates a new FourWheelRandom with a random state.
     */
    public FourWheelRandom () {
        super();
        stateA = super.nextLong();
        stateB = super.nextLong();
        stateC = super.nextLong();
        stateD = super.nextLong();
    }

    /**
     * Creates a new FourWheelRandom with the given seed; all {@code long} values are permitted.
     * The seed will be passed to {@link #setSeed(long)} to attempt to adequately distribute the seed randomly.
     * @param seed any {@code long} value
     */
    public FourWheelRandom (long seed) {
        super(seed);
        setSeed(seed);
    }

    /**
     * Creates a new FourWheelRandom with the given four states; all {@code long} values are permitted.
     * These states will be used verbatim.
     * @param stateA any {@code long} value
     * @param stateB any {@code long} value
     * @param stateC any {@code long} value
     * @param stateD any {@code long} value
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
    @Override
    public int getStateCount() {
        return 4;
    }

    /**
     * Gets the state determined by {@code selection}, as-is. The value for selection should be
     * between 0 and 3, inclusive; if it is any other value this gets state D as if 3 was given.
     * @param selection used to select which state variable to get; generally 0, 1, 2, or 3
     * @return the value of the selected state
     */
    @Override
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
    @Override
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
    @Override
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
    @Override
    public void setState(long stateA, long stateB, long stateC, long stateD) {
        this.stateA = stateA;
        this.stateB = stateB;
        this.stateC = stateC;
        this.stateD = stateD;
    }

    @Override
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

    @Override
    public long previousLong() {
        final long fa = this.stateA;
        final long fb = this.stateB;
        final long fc = this.stateC;
        this.stateD = 0x572B5EE77A54E3BDL * fa;
        this.stateA = fb - 0xC6BC279692B5C323L;
        this.stateB = Long.rotateRight(fc + this.stateD, 47);
        this.stateC = this.stateD ^ this.stateB;
        return 0x572B5EE77A54E3BDL * this.stateA;
    }

    @Override
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

    @Override
    public FourWheelRandom copy() {
        return new FourWheelRandom(stateA, stateB, stateC, stateD);
    }

    @Override
    public void nextBytes(byte[] bytes) {
        EnhancedRandom.super.nextBytes(bytes);
    }

    @Override
    public int nextInt() {
        return EnhancedRandom.super.nextInt();
    }

    @Override
    public int nextInt(int bound) {
        return EnhancedRandom.super.nextInt(bound);
    }

    @Override
    public boolean nextBoolean() {
        return EnhancedRandom.super.nextBoolean();
    }

    @Override
    public float nextFloat() {
        return EnhancedRandom.super.nextFloat();
    }

    @Override
    public double nextDouble() {
        return EnhancedRandom.super.nextDouble();
    }

    @Override
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
        return "FourWheelRandom{" +
                "stateA=" + stateA +
                "L, stateB=" + stateB +
                "L, stateC=" + stateC +
                "L, stateC=" + stateD +
                "L}";
    }
}

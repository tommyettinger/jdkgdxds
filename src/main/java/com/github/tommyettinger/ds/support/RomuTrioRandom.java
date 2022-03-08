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
 * A well-studied RNG that can be quite fast in some circumstances. This is a port of
 * <a href="https://romu-random.org/">Romu-Random</a>'s RomuTrio generator to Java. It has
 * three {@code long} states, which must never all be 0 but otherwise have no known restrictions.
 * <br>
 * RomuTrioRandom implements all optional methods in EnhancedRandom except
 * {@link #skip(long)} or {@link #previousLong()}.
 * <br>
 * It is strongly recommended that you seed this with {@link #setSeed(long)} instead of
 * {@link #setState(long, long, long)}, because if you give sequential seeds to both setSeed() and setState(), the
 * former will start off random, while the latter will start off repeating the seed sequence. After about 20-40 random
 * numbers generated, any correlation between similarly seeded generators will probably be completely gone, though.
 * <br>
 * The implementation here uses the algorithm from <a href="https://romu-random.org/">Romu-Random</a>, but bases its
 * implementation more closely on <a href="https://github.com/bgrainger/RomuRandom">this C# port</a>. The C# port by
 * Bradley Grainger is Apache 2.0 licensed, like jdkgdxds.
 */
public class RomuTrioRandom implements EnhancedRandom {

    /**
	 * The first state; can be any long unless all states are 0. If this has just been set to some value, then the
     * next call to {@link #nextLong()} will return that value as-is. Later calls will be more random.
     */
    protected long stateA;
    /**
	 * The second state; can be any long; can be any long unless all states are 0.
     */
    protected long stateB;
    /**
     * The third state; can be any long; can be any long unless all states are 0.
     */
    protected long stateC;

    /**
     * Creates a new RomuTrioRandom with a random state.
     */
    public RomuTrioRandom () {
        stateA = EnhancedRandom.seedFromMath();
        stateB = EnhancedRandom.seedFromMath();
        stateC = EnhancedRandom.seedFromMath();
        if((stateA | stateB | stateC) == 0L) stateC = -1L;
    }

    /**
     * Creates a new RomuTrioRandom with the given seed; all {@code long} values are permitted.
     * The seed will be passed to {@link #setSeed(long)} to attempt to adequately distribute the seed randomly.
     * @param seed any {@code long} value
     */
    public RomuTrioRandom (long seed) {
        setSeed(seed);
    }

    /**
     * Creates a new RomuTrioRandom with the given three states; all {@code long} values are permitted unless all
     * three states are 0. In that case, it treats stateC as if it were -1. Otherwise, these states will be used verbatim.
     * @param stateA any {@code long} value
     * @param stateB any {@code long} value
     * @param stateC any {@code long} value
     */
    public RomuTrioRandom (long stateA, long stateB, long stateC) {
        if((stateA | stateB | stateC) == 0L) stateC = -1L;
        this.stateA = stateA;
        this.stateB = stateB;
        this.stateC = stateC;
    }

    /**
     * This generator has 3 {@code long} states, so this returns 3.
     * @return 3 (three)
     */
    @Override
    public int getStateCount() {
        return 3;
    }

    /**
     * Gets the state determined by {@code selection}, as-is.
     * @param selection used to select which state variable to get; generally 0, 1, or 2
     * @return the value of the selected state
     */
    @Override
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
    @Override
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
     * Sets the third part of the state; if all states would be 0, this instead assigns -1.
     * @param stateC can be any long
     */
    public void setStateC(long stateC) {
        if((stateA | stateB | stateC) == 0L) this.stateC = -1L;
        else this.stateC = stateC;
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
    @Override
    public void setState(long stateA, long stateB, long stateC) {
        this.stateA = stateA;
        this.stateB = stateB;
        this.stateC = stateC;
        if((stateA | stateB | stateC) == 0L) this.stateC = -1L;
    }

    @Override
    public long nextLong() {
        final long fa = stateA;
        stateA = 0xD3833E804F4C574BL * stateC;
        stateC -= stateB;
        stateB -= fa;
        stateB = (stateB << 12 | stateB >>> 52);
        stateC = (stateC << 44 | stateC >>> 20);
        return fa;
    }

    @Override
    public int next(int bits) {
        final long fa = stateA;
        stateA = 0xD3833E804F4C574BL * stateC;
        stateC -= stateB;
        stateB -= fa;
        stateB = (stateB << 12 | stateB >>> 52);
        stateC = (stateC << 44 | stateC >>> 20);
        return (int)fa >>> (32 - bits);
    }

    @Override
    public RomuTrioRandom copy() {
        return new RomuTrioRandom(stateA, stateB, stateC);
    }

    @Override
    public boolean equals (Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        RomuTrioRandom that = (RomuTrioRandom)o;

        if (stateA != that.stateA)
            return false;
        if (stateB != that.stateB)
            return false;
        return stateC == that.stateC;
    }

    public String toString() {
        return "RomuTrioRandom{" +
                   "stateA=0x" + Base.BASE16.unsigned(stateA) +
                "L, stateB=0x" + Base.BASE16.unsigned(stateB) +
                "L, stateC=0x" + Base.BASE16.unsigned(stateC) +
                "L}";
    }
}

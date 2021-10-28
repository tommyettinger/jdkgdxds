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
 * A random number generator that acts as a counterpart to {@link FourWheelRandom} by guaranteeing a slightly longer period
 * and potentially being faster in some situations because it uses no multiplication. Like FourWheelRandom, this has four
 * {@code long} states, and is quite fast on desktop platforms (FourWheelRandom is about 25% faster, but both are very good).
 * It can be considered stable, like the other EnhancedRandom implementations here. I've tested this more than any other
 * generator I've written; it passes 64TB of PractRand, 5PB of hwd, and an absolutely massive amount of another test, extsat,
 * run on a GPU using CUDA. This last test is still a work in progress, so the exact amount of data tested may not be
 * accurate, but it appears to be over 2 exabytes after over 500 hours.
 * <br>
 * The reason this has undergone so much testing is that it is built on top of some of the weakest random number generators
 * out there -- two interleaved 64-bit two-step xorshift generators, and some simple chaotic generators that incorporate the
 * results of those xorshift generators. This avoids multiplication entirely; the operations it uses are two xors, one left
 * shift, one unsigned right shift, one addition, two subtractions, and one bitwise rotation. It has a guaranteed minimum
 * period of (2 to the 65) - 2, and the actual minimum period is almost certainly higher (the guarantee comes purely from its
 * stateA and stateB, which interleave two periods of (2 to the 64) - 1; stateC and stateD extend this period by an unknown
 * amount). The xorshift generators are the absolute weakest generators of their kind -- they use constants of 7 and 9, which
 * are the only two full-period constants for a 64-bit xorshift generator. These only cause an avalanche of 4 bits to
 * change when one bit changes in their input, but this turns out to be more than enough for the chaotic stateC and stateD
 * generators. We ensure that stateA and stateB are sufficiently distant in their shared sequence by using a jump polynomial
 * on stateA to get a stateB that is {@code 0x9E3779B97F4A7C15} steps ahead of stateA (11.4 quintillion steps forward or 7
 * quintillion steps backward). The complicated calculations for the jump polynomial were done by Spencer Fleming; this was
 * not easy. This generator is meant in particular to optimize well for GPU computations, even though Java doesn't have much
 * ability to do this currently. Some uncommon platforms may also optimize this better than FourWheelRandom.
 * <br>
 * It implements all optional methods in EnhancedRandom except {@link #skip(long)} and {@link #previousLong()}.
 */
public class StrangerRandom extends Random implements EnhancedRandom {

    /**
     * The first state; can be any long except 0
     */
    protected long stateA;
    /**
	 * The second state; can be any long except 0, and should be a significant distance from stateA in the xorshift sequence.
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
     * Jumps {@code state} ahead by 0x9E3779B97F4A7C15 steps of the generator StrangerRandom uses for its stateA
     * and stateB. When used how it is here, it ensures stateB is 11.4 quintillion steps ahead of stateA in their
     * shared sequence, or 7 quintillion behind if you look at it another way. It would typically take years of
     * continuously running this generator at 100GB/s to have stateA become any state that stateB has already been.
     * Users only need this function if setting stateB by-hand; in that case, {@code state} should be their stateA.
     * <br>
     * Massive credit to Spencer Fleming for writing essentially all of this function over several days.
     * @param state the initial state of a 7-9 xorshift generator
     * @return state jumped ahead 0x9E3779B97F4A7C15 times (unsigned)
     */
    public static long jump(long state){
        final long poly = 0x5556837749D9A17FL;
        long val = 0L, b = 1L;
        for (int i = 0; i < 63; i++, b <<= 1) {
            if((poly & b) != 0L) val ^= state;
            state ^= state << 7;
            state ^= state >>> 9;
        }
        return val;
    }

    /**
     * Creates a new StrangerRandom with a random state.
     */
    public StrangerRandom () {
        super();
        stateA = super.nextLong();
        if(stateA == 0L) stateA = 0xD3833E804F4C574BL;
        stateB = jump(stateA);
        stateC = super.nextLong();
        stateD = super.nextLong();
    }

    /**
     * Creates a new StrangerRandom with the given seed; all {@code long} values are permitted.
     * The seed will be passed to {@link #setSeed(long)} to attempt to adequately distribute the seed randomly.
     * @param seed any {@code long} value
     */
    public StrangerRandom (long seed) {
        super(seed);
        setSeed(seed);
    }

    /**
     * Creates a new StrangerRandom with the given four states; all {@code long} values are permitted.
     * These states will be used verbatim, unless stateA or stateB is 0. If stateA is given 0, it instead
     * uses {@code 0xD3833E804F4C574BL}; if stateB is given 0, it instead uses {@code 0x790B300BF9FE738FL}.
     * @param stateA any {@code long} value
     * @param stateB any {@code long} value
     * @param stateC any {@code long} value
     * @param stateD any {@code long} value
     */
    public StrangerRandom (long stateA, long stateB, long stateC, long stateD) {
        super(stateA + stateB ^ stateC - stateD);
        this.stateA = (stateA == 0L) ? 0xD3833E804F4C574BL : stateA;
        this.stateB = (stateB == 0L) ? 0x790B300BF9FE738FL : stateB;
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
            stateA = value == 0L ? 0xD3833E804F4C574BL : value;
            break;
        case 1:
            stateB = value == 0L ? 0x790B300BF9FE738FL : value;
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
     * different for every different {@code seed}). This ensures stateB is a sufficient distance
     * from stateA in their shared sequence, and also does some randomizing on the seed before it
     * assigns the result to stateC. This isn't an instantaneously-fast method to call like some
     * versions of setSeed(), but it shouldn't be too slow unless it is called before every
     * generated number (even then, it might be fine).
     * @param seed the initial seed; may be any long
     */
    @Override
    public void setSeed(long seed) {
        stateA = seed ^ 0xFA346CBFD5890825L;
        if(stateA == 0L) stateA = 0xD3833E804F4C574BL;
        stateB = jump(stateA);
        stateC = jump(stateB - seed);
        stateD = jump(stateC + 0xC6BC279692B5C323L);
    }

    public long getStateA() {
        return stateA;
    }

    /**
     * Sets the first part of the state.
     * @param stateA can be any long except 0; this treats 0 as 0xD3833E804F4C574BL
     */
    public void setStateA(long stateA) {
        this.stateA = (stateA == 0L) ? 0xD3833E804F4C574BL : stateA;
    }

    public long getStateB() {
        return stateB;
    }

    /**
     * Sets the second part of the state.
     * @param stateB can be any long except 0; this treats 0 as 0x790B300BF9FE738FL
     */
    public void setStateB(long stateB) {
        this.stateB = (stateB == 0L) ? 0x790B300BF9FE738FL : stateB;
    }

    public long getStateC() {
        return stateC;
    }

    /**
     * Sets the third part of the state. Note that if you call {@link #nextLong()}
     * immediately after this, it will return the given {@code stateC} as-is, so you
     * may want to call some random generation methods (such as nextLong()) and discard
     * the results after setting the state.
     * @param stateC can be any long
     */
    public void setStateC(long stateC) {
        this.stateC = stateC;
    }

    public long getStateD() {
        return stateD;
    }

    /**
     * Sets the fourth part of the state.
     * @param stateD can be any long
     */
    public void setStateD(long stateD) {
        this.stateD = stateD;
    }

    /**
     * Sets the state completely to the given four state variables, unless stateA or stateB are 0.
     * This is the same as calling {@link #setStateA(long)}, {@link #setStateB(long)},
     * {@link #setStateC(long)}, and {@link #setStateD(long)} as a group. You may want
     * to call {@link #nextLong()} a few times after setting the states like this, unless
     * the value for stateC (in particular) is already adequately random; the first call
     * to {@link #nextLong()}, if it is made immediately after calling this, will return {@code stateC} as-is.
     * @param stateA the first state; can be any long; can be any long except 0
     * @param stateB the second state; can be any long; can be any long except 0
     * @param stateC the third state; this will be returned as-is if the next call is to {@link #nextLong()}
     * @param stateD the fourth state; can be any long
     */
    @Override
    public void setState(long stateA, long stateB, long stateC, long stateD) {
        this.stateA = (stateA == 0L) ? 0xD3833E804F4C574BL : stateA;
        this.stateB = (stateB == 0L) ? 0x790B300BF9FE738FL : stateB;
        this.stateC = stateC;
        this.stateD = stateD;
    }

    /**
     * Sets the state with three variables, ensuring that the result has states A and B
     * sufficiently separated from each other, while keeping states C and D as given.
     * Note that this does not take a stateB parameter, and instead obtains it by jumping
     * stateA ahead by about 11.4 quintillion steps using {@link #jump(long)}. If stateA is
     * given as 0, this uses 0xD3833E804F4C574BL instead for stateA and 0x790B300BF9FE738FL
     * for stateB. States C and D can each be any long.
     * @param stateA the long value to use for stateA and also used to get stateB; can be any long except 0
     * @param stateC the long value to use for stateC; this will be returned as-is if the next call is to {@link #nextLong()}
     * @param stateD the long value to use for stateD; can be any long
     */
    @Override
    public void setState (long stateA, long stateC, long stateD) {
        this.stateA = (stateA == 0L) ? 0xD3833E804F4C574BL : stateA;
        this.stateB = jump(this.stateA);
        this.stateC = stateC;
        this.stateD = stateD;
    }

    @Override
    public long nextLong() {
        final long fa = this.stateA;
        final long fb = this.stateB;
        final long fc = this.stateC;
        final long fd = this.stateD;
        this.stateA = fb ^ fb << 7;
        this.stateB = fa ^ fa >>> 9;
        this.stateC = Long.rotateLeft(fd, 39) - fb;
        this.stateD = fa - fc + 0xC6BC279692B5C323L;
        return fc;
    }

    @Override
    public int next(int bits) {
        final long fa = this.stateA;
        final long fb = this.stateB;
        final long fc = this.stateC;
        final long fd = this.stateD;
        this.stateA = fb ^ fb << 7;
        this.stateB = fa ^ fa >>> 9;
        this.stateC = Long.rotateLeft(fd, 39) - fb;
        this.stateD = fa - fc + 0xC6BC279692B5C323L;
        return (int)fc >>> (32 - bits);
    }

    @Override
    public StrangerRandom copy() {
        return new StrangerRandom(stateA, stateB, stateC, stateD);
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

        StrangerRandom that = (StrangerRandom)o;

        if (stateA != that.stateA)
            return false;
        if (stateB != that.stateB)
            return false;
        if (stateC != that.stateC)
            return false;
        return stateD == that.stateD;
    }

    public String toString() {
        return "StrangerRandom{" +
                "stateA=" + stateA +
                "L, stateB=" + stateB +
                "L, stateC=" + stateC +
                "L, stateC=" + stateD +
                "L}";
    }
}

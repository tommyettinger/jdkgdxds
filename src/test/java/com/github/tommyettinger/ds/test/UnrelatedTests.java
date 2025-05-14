/*
 * Copyright (c) 2025 See AUTHORS file.
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
 */

package com.github.tommyettinger.ds.test;

import com.github.tommyettinger.digital.Base;
import com.github.tommyettinger.ds.LongSet;
import com.github.tommyettinger.ds.support.util.LongIterator;

public class UnrelatedTests {
    public static void main(String[] args) {
        // 10700330 unique results in mask
//        testMaskedUniquenessCounter();
        // 16777216 unique results in mask
//        testMaskedUniquenessDeposit();
        System.out.println(Base.BASE16.unsigned(deposit(1L, 0x003569CA5369AC00L)));
        System.out.println(Base.BASE16.unsigned(depositPrecomputed(1L, 0x003569CA5369AC00L, new long[]{0x0004600240602000L, 0x0030380830381C00L, 0x000F0003000F8000L, 0x0000FFC00000FF00L, 0x000000FFFF000000L})));
    }

    public static void testMaskedUniquenessCounter() {
        long mask = 0x003569CA5369AC00L;
        LongSet all = new LongSet(1 << 25, 0.5f);
        long ctr = 0;
        for (int i = 0, n = 1 << 24; i < n; i++) {
            all.add(ctr & mask);
            ctr += mask;
        }
        System.out.println(all.size());
    }

    public static void testMaskedUniquenessDeposit() {
        long mask = 0x003569CA5369AC00L;
        long[] table = {0x0004600240602000L, 0x0030380830381C00L, 0x000F0003000F8000L, 0x0000FFC00000FF00L, 0x000000FFFF000000L};
//        long[] table = computeDepositTable(mask, new long[5]);

        for (int i = 0; i < 5; i++) {
            System.out.print("0x" + Base.BASE16.unsigned(table[i]) + "L, ");
        }
        System.out.println();
        LongSet all = new LongSet(1 << 25, 0.5f);
        long ctr = 0;
        long max = 0;
        for (int i = 0, n = 1 << 24; i < n; i++) {
            long r = depositPrecomputed(ctr++, mask, table);
            all.add(r);
            max = Math.max(r, max);
        }
        System.out.println("Forward has " + all.size() + " items and a max value of 0x" + Base.BASE16.unsigned(max));
        LongSet inverse = new LongSet(1 << 25, 0.5f);
        LongIterator it = all.iterator();
        max = 0;
        while (it.hasNext()){
            long r = extract(it.next(), mask);
            inverse.add(r);
            max = Math.max(r, max);
        }
        System.out.println("Inverse has " + inverse.size() + " items and a max value of 0x" + Base.BASE16.unsigned(max));
    }

    /**
     * Given a long {@code bits} where the first N positions can have variable bits, and a long {@code mask} with N bits
     * set to 1, produces a long where the least-significant N bits of {@code bits} have been placed into consecutively
     * greater set bits in {@code mask}. This method does not allocate, but it spends much of its time computing five
     * long "table" values that can be precomputed using {@link #computeDepositTable(long, long[])} and then used with
     * {@link #depositPrecomputed(long, long, long[])} for cases when the mask stays the same across many calls.
     * <br>
     * Based on Hacker's Delight (2nd edition).
     * @param bits the bit values to be deposited into positions denoted by mask
     * @param mask where a bit is 1, a bit from {@code bits} will be deposited
     * @return a long where only bits in mask can be set
     */
    public static long deposit(long bits, long mask) {
        long table0, table1, table2, table3, table4;
        long mp, t, m0 = mask; // Save original mask.
        long mk = ~mask; // We will count 0's to right.

        mp = mk ^ mk << 1; // Parallel suffix.
        mp ^= mp << 2;
        mp ^= mp << 4;
        mp ^= mp << 8;
        mp ^= mp << 16;
        mp ^= mp << 32;
        t = mp & mask; // Bits to move.
        table0 = t;
        mask = (mask ^ t) | (t >>> 1); // Compress mask.
        mk = mk & ~mp;

        mp = mk ^ mk << 1; // Parallel suffix.
        mp ^= mp << 2;
        mp ^= mp << 4;
        mp ^= mp << 8;
        mp ^= mp << 16;
        mp ^= mp << 32;
        t = mp & mask; // Bits to move.
        table1 = t;
        mask = (mask ^ t) | (t >>> 2); // Compress mask.
        mk = mk & ~mp;

        mp = mk ^ mk << 1; // Parallel suffix.
        mp ^= mp << 2;
        mp ^= mp << 4;
        mp ^= mp << 8;
        mp ^= mp << 16;
        mp ^= mp << 32;
        t = mp & mask; // Bits to move.
        table2 = t;
        mask = (mask ^ t) | (t >>> 4); // Compress mask.
        mk = mk & ~mp;

        mp = mk ^ mk << 1; // Parallel suffix.
        mp ^= mp << 2;
        mp ^= mp << 4;
        mp ^= mp << 8;
        mp ^= mp << 16;
        mp ^= mp << 32;
        t = mp & mask; // Bits to move.
        table3 = t;
        mask = (mask ^ t) | (t >>> 8); // Compress mask.
        mk = mk & ~mp;

        mp = mk ^ mk << 1; // Parallel suffix.
        mp ^= mp << 2;
        mp ^= mp << 4;
        mp ^= mp << 8;
        mp ^= mp << 16;
        mp ^= mp << 32;
        t = mp & mask; // Bits to move.
        table4 = t;
        // done making the table values.

        System.out.println("deposit() table values: " + Base.BASE16.unsigned(table0) + ", " + Base.BASE16.unsigned(table1) + ", " + Base.BASE16.unsigned(table2) + ", " + Base.BASE16.unsigned(table3) + ", " + Base.BASE16.unsigned(table4));

        // actually using the five table values:
        t = bits << 16;
        bits = (bits & ~table4) | (t & table4);
        t = bits << 8;
        bits = (bits & ~table3) | (t & table3);
        t = bits << 4;
        bits = (bits & ~table2) | (t & table2);
        t = bits << 2;
        bits = (bits & ~table1) | (t & table1);
        t = bits << 1;
        bits = (bits & ~table0) | (t & table0);
        return bits & m0; // Clear out extraneous bits.
    }
    /**
     * Given a long {@code bits} where the first N positions can have variable bits, and a long {@code mask} with N bits
     * set to 1, produces a long where the least-significant N bits of {@code bits} have been placed into consecutively
     * greater set bits in {@code mask}. This permits taking a long array with 5 items, {@code table}, that can be null
     * if the mask is expected to change often, making this recompute a table every time, or precomputed via
     * {@link #computeDepositTable(long, long[])} and passed here for when the mask will be the same many times.
     * <br>
     * Based on Hacker's Delight (2nd edition).
     * @param bits the bit values to be deposited into positions denoted by mask
     * @param mask where a bit is 1, a bit from {@code bits} will be deposited
     * @param table if null, will be computed each time, but can be precomputed with {@link #computeDepositTable(long, long[])}
     * @return a long where only bits in mask can be set
     */
    public static long depositPrecomputed(long bits, long mask, long[] table) {
        if(table == null || table.length < 5)
            table = computeDepositTable(mask, table);
        for (int i = 4; i >= 0; i--) {
            long mv = table[i];
            long t = bits << (1 << i);
            bits = (bits & ~mv) | (t & mv);
        }
        return bits & mask; // Clear out extraneous bits.
    }

    /**
     * Precomputes the {@code table} argument for the given {@code mask} that can be given to
     * {@link #depositPrecomputed(long, long, long[])} to avoid recalculating and reallocating a 5-item table.
     * @param mask the mask that will be used with {@link #depositPrecomputed(long, long, long[])}
     * @param table an existing long array of length 5 or greater that will be overwritten, otherwise this will create a new array
     * @return {@code table} after reassignment, or a new long array if {@code table} was null or too small
     */
    public static long[] computeDepositTable(long mask, long[] table) {
        if(table == null || table.length < 5)
            table = new long[5];
        long mk = ~mask; // We will count 0's to right.
        for (int i = 0; i < 5; i++) {
            long mp = mk ^ mk << 1; // Parallel suffix.
            mp ^= mp << 2;
            mp ^= mp << 4;
            mp ^= mp << 8;
            mp ^= mp << 16;
            mp ^= mp << 32;
            long v = mp & mask; // Bits to move.
            table[i] = v;
            mask = (mask ^ v) | (v >>> (1 << i)); // Compress mask.
            mk = mk & ~mp;
        }
        return table;
    }

    /**
     * Given a long {@code bits} where any bits may be set, and a long {@code mask} with N bits set to 1 that determines
     * which positions in {@code bits} will matter, this produces an up-to-N-bit long result where positions in
     * {@code bits} matching positions in {@code mask} were placed in sequentially-more-significant positions, starting
     * at the least significant bit.
     * <br>
     * Based on Hacker's Delight (2nd edition).
     * @param bits the bit values that will be masked by {@code mask} and placed into the low-order bits of the result
     * @param mask where a bit is 1, a bit from {@code bits} will be extracted to be returned
     * @return a long with the highest bit that can be set equal to the {@link Long#bitCount(long)} of {@code mask}
     */
    public static long extract(long bits, long mask) {
        bits &= mask; // Clear irrelevant bits.
        long mk = ~mask; // We will count 0's to right.
        for (int i = 0; i < 5; i++) {
            long mp = mk ^ mk << 1; // Parallel suffix.
            mp ^= mp << 2;
            mp ^= mp << 4;
            mp ^= mp << 8;
            mp ^= mp << 16;
            mp ^= mp << 32;
            long mv = mp & mask; // Bits to move.
            mask = (mask ^ mv) | (mv >>> (1 << i)); // Compress mask.
            long t = bits & mv;
            bits = (bits ^ t) | (t >>> (1 << i)); // Compress bits.
            mk = mk & ~mp;
        }
        return bits;
    }
}

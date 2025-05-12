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

public class UnrelatedTests {
    public static void main(String[] args) {
        // 10700330 unique results in mask
//        testMaskedUniquenessCounter();
        // 16777216 unique results in mask
        testMaskedUniquenessDeposit();
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
//        long[] table = computeDepositTable(mask, new long[5]);
        long[] table = new long[]{0x0004600240602000L, 0x0030380830381C00L, 0x000F0003000F8000L, 0x0000FFC00000FF00L, 0x000000FFFF000000L};
        for (int i = 0; i < table.length; i++) {
            System.out.print("0x" + Base.BASE16.unsigned(table[i]) + "L, ");
        }
        System.out.println();
        LongSet all = new LongSet(1 << 25, 0.5f);
        long ctr = 0;
        for (int i = 0, n = 1 << 24; i < n; i++) {
            all.add(depositPrecomputed(ctr++, mask, table));
        }
        System.out.println(all.size());
    }

    /**
     * Based on Hacker's Delight (2nd edition).
     * @param bits the bit values to be deposited into positions denoted by mask
     * @param mask where a bit is 1, a bit from {@code bits} will be deposited
     * @return a long where only bits in mask can be set
     */
    public static long deposit(long bits, long mask) {
        long[] table = new long[5];
        long m0 = mask; // Save original mask.
        long mk = ~mask; // We will count 0's to right.
        for (int i = 0; i < 5; i++) {
            long mp = mk ^ (mk << 1); // Parallel suffix.
            mp = mp ^ (mp << 2);
            mp = mp ^ (mp << 4);
            mp = mp ^ (mp << 8);
            mp = mp ^ (mp << 16);
            mp = mp ^ (mp << 32);
            long v = mp & mask; // Bits to move.
            table[i] = v;
            mask = (mask ^ v) | (v >>> (1 << i)); // Compress mask.
            mk = mk & ~mp;
        }
        for (int i = 4; i >= 0; i--) {
            long mv = table[i];
            long t = bits << (1 << i);
            bits = (bits & ~mv) | (t & mv);
        }
        return bits & m0; // Clear out extraneous bits.
    }
    public static long depositPrecomputed(long bits, long mask, long[] table) {
        for (int i = 4; i >= 0; i--) {
            long mv = table[i];
            long t = bits << (1 << i);
            bits = (bits & ~mv) | (t & mv);
        }
        return bits & mask; // Clear out extraneous bits.
    }
    public static long[] computeDepositTable(long mask, long[] table) {
        if(table == null || table.length < 5)
            table = new long[5];
        long mk = ~mask; // We will count 0's to right.
        for (int i = 0; i < 5; i++) {
            long mp = mk ^ (mk << 1); // Parallel suffix.
            mp = mp ^ (mp << 2);
            mp = mp ^ (mp << 4);
            mp = mp ^ (mp << 8);
            mp = mp ^ (mp << 16);
            mp = mp ^ (mp << 32);
            long v = mp & mask; // Bits to move.
            table[i] = v;
            mask = (mask ^ v) | (v >>> (1 << i)); // Compress mask.
            mk = mk & ~mp;
        }
        return table;
    }
}

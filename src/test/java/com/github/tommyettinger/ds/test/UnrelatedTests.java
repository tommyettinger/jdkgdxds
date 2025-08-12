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
import com.github.tommyettinger.digital.MathTools;
import com.github.tommyettinger.ds.LongSet;
import com.github.tommyettinger.ds.support.util.LongIterator;
import org.junit.Test;

public class UnrelatedTests {
	public static void main(String[] args) {
		// 10700330 unique results in mask
//        testMaskedUniquenessCounter();
		// 16777216 unique results in mask
//        testMaskedUniquenessDeposit();
//        System.out.println(Base.BASE16.unsigned(deposit(1L, 0x9E3779B97F4A7C15L)));
//        System.out.println(Base.BASE16.unsigned(depositPrecomputed(1L, 0x9E3779B97F4A7C15L, 0x003079807F027C04L, 0x80003C0100083E10L, 0x001F00007FC00F80L, 0x3E00000007FF0000L, 0x003FFFF800000000L)));
//        System.out.println(Base.BASE16.unsigned(deposit((1L << 38) - 1L, 0x9E3779B97F4A7C15L)));
//        System.out.println(Base.BASE16.unsigned(depositPrecomputed((1L << 38) - 1L, 0x9E3779B97F4A7C15L, new long[]{0x003079807F027C04L, 0x80003C0100083E10L, 0x001F00007FC00F80L, 0x3E00000007FF0000L, 0x003FFFF800000000L})));
//        System.out.println(Base.BASE16.unsigned(deposit((1L << 57) - 1L, -255L)));
//        System.out.println(Base.BASE16.unsigned(depositPrecomputed((1L << 57) - 1L, -255L)));

		// 16777216 unique results in input range up to (1<<24) inputs, all odd
		// even with mask=1 !
		// 536870912 unique results in input range up to (1<<29) inputs, all odd
		// even with mask=4 !
		// but, with mask=1, there is a single collision at this size.
		// 268435456 unique results in input range up to (1<<28) inputs, all odd
		// even with mask=1 !
		testUniquenessFixGamma();
	}

	public static long fixGamma(long gamma, int threshold) {
		gamma |= 1L;
		long inverse, add = 0L;
		while (Math.abs(Long.bitCount(gamma) - 32) > threshold
			|| Math.abs(Long.bitCount(gamma ^ gamma >>> 1) - 32) > threshold
			|| Math.abs(Long.bitCount(inverse = MathTools.modularMultiplicativeInverse(gamma)) - 32) > threshold
			|| Math.abs(Long.bitCount(inverse ^ inverse >>> 1) - 32) > threshold) {
			gamma = gamma * 0xD1342543DE82EF95L + (add += 2L);
		}
		return gamma;
	}

	public static void testUniquenessFixGamma() {
		LongSet all = new LongSet(1 << 28, 0.75f);
		final long n = 1L << 29;
		for (long i = 1; i < n; i += 2L) {
			if (!all.add(fixGamma(i, 1))) {
				System.out.println("Collision at size " + all.size() + " !");
			}
		}
		System.out.println(all.size());
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
//        long[] table = {0x0004600240602000L, 0x0030380830381C00L, 0x000F0003000F8000L, 0x0000FFC00000FF00L, 0x000000FFFF000000L};
		long[] table = computeDepositTable(mask, new long[5]);

		printTable(table);
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
		while (it.hasNext()) {
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
	 * Based on Hacker's Delight (2nd edition). This can be replaced with {@code Long.expand(bits, mask)} in Java 19 or
	 * later, which may perform better if the processor in use has a fast PDEP instruction.
	 *
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

//        System.out.println("deposit() table values: 0x" + Base.BASE16.unsigned(table0) + "L, 0x" + Base.BASE16.unsigned(table1) + "L, 0x" + Base.BASE16.unsigned(table2) + "L, 0x" + Base.BASE16.unsigned(table3) + "L, 0x" + Base.BASE16.unsigned(table4) + "L");

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
	 * In the case where {@code table} is passed as varargs and has exactly 5 elements, this should instead delegate to
	 * {@link #depositPrecomputed(long, long, long, long, long, long, long)}, which doesn't allocate an array.
	 * <br>
	 * Based on Hacker's Delight (2nd edition). This is similar to {@code Long.expand(bits, mask)} in Java 19 or later,
	 * which may perform better if the processor in use has a fast PDEP instruction, though this saves a substantial
	 * amount of effort on the software fallback for expand().
	 *
	 * @param bits  the bit values to be deposited into positions denoted by mask
	 * @param mask  where a bit is 1, a bit from {@code bits} will be deposited
	 * @param table if null, will be computed each time, but can be precomputed with {@link #computeDepositTable(long, long[])}
	 * @return a long where only bits in mask can be set
	 */
	public static long depositPrecomputed(long bits, long mask, long... table) {
		if (table == null || table.length < 5)
			table = computeDepositTable(mask, table);
		for (int i = 4; i >= 0; i--) {
			long mv = table[i];
			long t = bits << (1 << i);
			bits = (bits & ~mv) | (t & mv);
		}
		return bits & mask; // Clear out extraneous bits.
	}

	/**
	 * Given a long {@code bits} where the first N positions can have variable bits, and a long {@code mask} with N bits
	 * set to 1, produces a long where the least-significant N bits of {@code bits} have been placed into consecutively
	 * greater set bits in {@code mask}. This overload takes 5 long "table" arguments explicitly to avoid allocating an
	 * array when exactly 5 longs are given as varargs to {@link #depositPrecomputed(long, long, long...)}. The table
	 * arguments should be exactly what {@link #printTable(long...)} prints for a table produced by
	 * {@link #computeDepositTable(long, long[])}, and can also be
	 * {@code table[0], table[1], table[2], table[3], table[4]}. This version is optimized relative to
	 * {@link #depositPrecomputed(long, long, long...)}, and should perform better if called often.
	 * <br>
	 * Based on Hacker's Delight (2nd edition). This is similar to {@code Long.expand(bits, mask)} in Java 19 or later,
	 * which may perform better if the processor in use has a fast PDEP instruction, though this saves a substantial
	 * amount of effort on the software fallback for expand().
	 *
	 * @param bits   the bit values to be deposited into positions denoted by mask
	 * @param mask   where a bit is 1, a bit from {@code bits} will be deposited
	 * @param table0 item from a precomputed table produced by {@link #computeDepositTable(long, long[])}
	 * @param table1 item from a precomputed table produced by {@link #computeDepositTable(long, long[])}
	 * @param table2 item from a precomputed table produced by {@link #computeDepositTable(long, long[])}
	 * @param table3 item from a precomputed table produced by {@link #computeDepositTable(long, long[])}
	 * @param table4 item from a precomputed table produced by {@link #computeDepositTable(long, long[])}
	 * @return a long where only bits in mask can be set
	 */
	public static long depositPrecomputed(long bits, long mask, long table0, long table1, long table2, long table3, long table4) {
		bits = (bits & ~table4) | (bits << 16 & table4);
		bits = (bits & ~table3) | (bits << 8 & table3);
		bits = (bits & ~table2) | (bits << 4 & table2);
		bits = (bits & ~table1) | (bits << 2 & table1);
		bits = (bits & ~table0) | (bits << 1 & table0);
		return bits & mask;
	}

	/**
	 * Precomputes the {@code table} argument for the given {@code mask} that can be given to
	 * {@link #depositPrecomputed(long, long, long[])} to avoid recalculating and reallocating a 5-item table.
	 * <br>
	 * Based on Hacker's Delight (2nd edition).
	 *
	 * @param mask  the mask that will be used with {@link #depositPrecomputed(long, long, long[])}
	 * @param table an existing long array of length 5 or greater that will be overwritten, otherwise this will create a new array
	 * @return {@code table} after reassignment, or a new long array if {@code table} was null or too small
	 */
	public static long[] computeDepositTable(long mask, long... table) {
		if (table == null || table.length < 5)
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
	 * Intended for debugging, this takes an array or varargs of 5 long items, which should have been produced by
	 * {@link #computeDepositTable(long, long[])}, and prints them in a format that can be entered as Java or Kotlin
	 * source code, either as varargs to {@link #depositPrecomputed(long, long, long...)} or as the 5 closing arguments
	 * to {@link #depositPrecomputed(long, long, long, long, long, long, long)}.
	 *
	 * @param table
	 */
	public static void printTable(long... table) {
		if (table == null || table.length < 5)
			System.out.println("Invalid table.");
		else {
			for (int i = 0; i < 5; i++) {
				System.out.print(Base.readable(table[i]));
				if (i < 4) System.out.print(", ");
			}
		}

	}

	/**
	 * Given a long {@code bits} where any bits may be set, and a long {@code mask} with N bits set to 1 that determines
	 * which positions in {@code bits} will matter, this produces an up-to-N-bit long result where positions in
	 * {@code bits} matching positions in {@code mask} were placed in sequentially-more-significant positions, starting
	 * at the least significant bit.
	 * <br>
	 * Based on Hacker's Delight (2nd edition). This can be replaced with {@code Long.compress(bits, mask)} in Java 19
	 * or later, which may perform better if the processor in use has a fast PEXT instruction.
	 *
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

	@Test
	public void testBaseAppend() {
		StringBuilder sb = new StringBuilder();
		Base.BASE10.appendSigned(sb, 1);
		System.out.println("sb should contain '1', and have length 1");
		System.out.println("sb is             '" + sb + "', and has length  " + sb.length());
		sb.setLength(0);
		Base.BASE16.appendUnsigned(sb, 1);
		System.out.println("sb should contain '00000001', and have length 8");
		System.out.println("sb is             '" + sb + "', and has length  " + sb.length());
		sb.setLength(0);
	}
}

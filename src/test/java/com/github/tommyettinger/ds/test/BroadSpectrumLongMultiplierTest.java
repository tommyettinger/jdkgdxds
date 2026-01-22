/*
 * Copyright (c) 2022-2025 See AUTHORS file.
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
import com.github.tommyettinger.digital.BitConversion;
import com.github.tommyettinger.ds.*;
import com.github.tommyettinger.ds.ObjectSet;
import com.github.tommyettinger.ds.support.sort.LongComparators;
import com.github.tommyettinger.random.DistinctRandom;
import com.github.tommyettinger.random.EnhancedRandom;
import com.github.tommyettinger.random.LowChangeQuasiRandom;

import java.io.IOException;

import static com.github.tommyettinger.ds.test.PileupTest.LEN;
import static com.github.tommyettinger.ds.test.PileupTest.generatePointSpiral;

/**
 * On small inputs only:
 * <pre>
 * 0000000263/0000000512: latest 0xA55C6C2FB54D7F91 gets total collisions: 172151, PILEUP: 0
 * </pre>
 * On larger inputs:
 * <pre>
 * (item ^ 0xD5C6E8C646A11777) * 0xDA91DC9C6494C03BL
 * 0000000291/0000000512: latest 0xD5C6E8C646A11777 gets total collisions: 408675, PILEUP: 7
 * </pre>
 *
 * <pre>
 * (item ^ 0xD5C6E8C646A11777L) * 0x80F53442FAE81817L
 * 0000000386/0000000512: latest 0x80F53442FAE81817 gets total collisions: 308835, PILEUP: 4
 * </pre>
 *
 * <pre>
 * (item ^ 0xD5C6E8C646A11727L) * 0x80F53442FAE81817L
 * 0000000040/0000000512: latest 0xD5C6E8C646A11727 gets total collisions: 239061, PILEUP: 1
 * </pre>
 *
 * <pre>
 * (item ^ 0xD5C6E8C246A11723L) * 0x80F53442FAE81817L
 * 0000000097/0000000512: latest 0xD5C6E8C246A11723 gets total collisions: 238332, PILEUP: 1
 * </pre>
 *
 * <pre>
 * (item ^ 0xD5C6E8E246A11703L) * 0x80F53442FAE81817L
 * 0000000292/0000000512: latest 0xD5C6E8E246A11703 gets total collisions: 237597, PILEUP: 1
 * </pre>
 *
 * <pre>
 * (item ^ 0xD5C6E86246A11783L) * 0x80F53442FAE81817L
 * 0000000422/0000000512: latest 0xD5C6E86246A11783 gets total collisions: 236813, PILEUP: 1
 * </pre>
 *
 * <pre>
 * (item ^ 0xD5C6E86246A11783L) * 0x883A21A5C11EF379L
 * 0000000306/0000000512: latest 0x883A21A5C11EF379 gets total collisions: 223470, PILEUP: 1
 * </pre>
 *
 * <pre>
 * (item ^ 0xD5C6E84246A117A3L) * 0x883A21A5C11EF379L
 * 0000000292/0000000512: latest 0xD5C6E84246A117A3 gets total collisions: 223297, PILEUP: 1
 * </pre>
 *
 * <pre>
 * (item ^ 0xD5C6E84646A117A7L) * 0x883A21A5C11EF379L
 * 0000000097/0000000512: latest 0xD5C6E84646A117A7 gets total collisions: 222936, PILEUP: 1
 * </pre>
 *
 * <pre>
 * (item ^ 0xD5C2E85646A517B7L) * 0x883A21A5C11EF379L
 * 0000000869/0000065536: latest 0xD5C2E85646A517B7 gets total collisions: 222565, PILEUP: 0
 * </pre>
 */
public class BroadSpectrumLongMultiplierTest {

	public static void main(String[] args) throws IOException {
		final Point2[] spiral = generatePointSpiral(LEN);
		IntSet collisions = new IntSet(LEN);
		for (int i = 0; i < LEN; i++) {
			collisions.add(spiral[i].hashCode());
		}
		System.out.println(collisions.size() + "/" + LEN + " hashes are unique.");
//		final long THRESHOLD = (long)(Math.pow(LEN, 11.0/10.0));
		final long THRESHOLD = (long) ((double) LEN * (double) LEN / (0.25 * collisions.size()));

		final int[] problems = {0};
		LongList likelyBad = new LongList(64);
		final int COUNT = 0x10000;
		LongLongOrderedMap good = new LongLongOrderedMap(COUNT);
		DistinctRandom random = new DistinctRandom(1L);
		int[] buffer = new int[Utilities.HASH_MULTIPLIERS.length];
		long[] minMax = new long[]{Long.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE, Long.MIN_VALUE};
		for (int a = 0; a < COUNT; a++) {
			final int finalA = a;

			LongSet set = new LongSet(51, 0.7f) {
//				final long hm = EnhancedRandom.fixGamma(finalA, 2);
				final long hm = 0xD5C6E84646A117A7L ^ (random.nextLong() & random.nextLong() & random.nextLong());
//				final long hm = 0xD5C6E84646A117A7L ^ finalA << 1;
//				final long hm = 0xD5C6E84646A117A7L ^ 2L << (finalA & 63) ^ 2L << (finalA >>> 6);
//				final long hm = LongUtilities.GOOD_MULTIPLIERS[finalA];
				long collisionTotal = 0;
				int longestPileup = 0;

				@Override
				protected int place(long item) {
					return (int)((item ^ hm) * 0x883A21A5C11EF379L >>> shift);
				}

				@Override
				protected void addResize(long key) {
					long[] keyTable = this.keyTable;
					for (int i = place(key), p = 0; ; i = i + 1 & mask) {
						if (keyTable[i] == 0) {
							keyTable[i] = key;
							return;
						} else {
							collisionTotal++;
							longestPileup = Math.max(longestPileup, ++p);
						}
					}
				}

				@Override
				protected void resize(int newSize) {
					int oldCapacity = keyTable.length;
					threshold = (int) (newSize * loadFactor);
					mask = newSize - 1;
					shift = BitConversion.countLeadingZeros(mask) + 32;

					long[] oldKeyTable = keyTable;

					keyTable = new long[newSize];

					longestPileup = 0;

					if (size > 0) {
						for (int i = 0; i < oldCapacity; i++) {
							long key = oldKeyTable[i];
							if (key != 0) {
								addResize(key);
							}
						}
					}
					if (collisionTotal > THRESHOLD) {
//					if (longestPileup > 118 - shift * 2) {
//						System.out.printf("  WHOOPS!!!  Multiplier 0x%016X on index %4d has %d collisions and %d pileup\n", hm, finalA, collisionTotal, longestPileup);
						good.remove(hm);
						likelyBad.add(hm);
						problems[0]++;
						throw new RuntimeException();
					}
				}

				@Override
				public void clear() {
					if(longestPileup == 0 || (finalA & 127) == 0) {
						System.out.print(Base.BASE10.unsigned(finalA) + "/" + Base.BASE10.unsigned(COUNT) + ": latest 0x" + Base.BASE16.unsigned(hm));
						System.out.println(" gets total collisions: " + collisionTotal + ", PILEUP: " + longestPileup);
					}
					minMax[0] = Math.min(minMax[0], collisionTotal);
					minMax[1] = Math.max(minMax[1], collisionTotal);
					minMax[2] = Math.min(minMax[2], longestPileup);
					minMax[3] = Math.max(minMax[3], longestPileup);
					good.put(hm, collisionTotal);
					super.clear();
				}
			};
			try {
				for (int i = 0, n = spiral.length; i < n; i++) {
					long h = spiral[i].hashCode();
					set.add((h & 0xFFFFFFFFL) | h << 32);
				}
				set.clear();
			} catch (RuntimeException ignored) {
//				System.out.println(finalA + " FAILURE");
			}
			// rotate multipliers by 1
//			System.arraycopy(Utilities.HASH_MULTIPLIERS, 1, buffer, 0, Utilities.HASH_MULTIPLIERS.length - 1);
//			System.arraycopy(Utilities.HASH_MULTIPLIERS, 0, buffer, Utilities.HASH_MULTIPLIERS.length - 1, 1);
//			System.arraycopy(buffer, 0, Utilities.HASH_MULTIPLIERS, 0, buffer.length);

		}
		good.sortByValue(LongComparators.NATURAL_COMPARATOR);

		long bigTotal = 0L;
		System.out.println("\n\npublic static final int[] GOOD_MULTIPLIERS = new int[]{");
		for (int i = 0, n = Math.min(600, good.size()); i < n; i++) {
			long collCount = good.getAt(i);
			bigTotal += collCount;
			System.out.println("0x" + Base.BASE16.unsigned(good.keyAt(i)) + ", //" + Base.BASE10.signed(collCount));
		}
		System.out.println("};\n");

		System.out.println("This used a THRESHOLD of " + THRESHOLD + ", and LEN of " + LEN);
//		System.out.println("This used a pileup threshold of 118 - shift * 2, and LEN of " + LEN);
		System.out.println(problems[0] + " problem multipliers in total, " + (COUNT - problems[0]) + " likely good multipliers in total.");
		System.out.println("Lowest collisions : " + minMax[0]);
		System.out.println("Highest collisions: " + minMax[1]);
		System.out.println("Average collisions: " + (bigTotal / Math.min((double) COUNT, good.size())));
		System.out.println("Lowest pileup     : " + minMax[2]);
		System.out.println("Highest pileup    : " + minMax[3]);
		System.out.println("Likely bad multipliers (base 16):");
		System.out.println(likelyBad.toString(" ", false, Base.BASE16::appendUnsigned));
	}

}

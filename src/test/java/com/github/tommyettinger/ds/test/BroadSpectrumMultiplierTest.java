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
import com.github.tommyettinger.digital.MathTools;
import com.github.tommyettinger.ds.IntList;
import com.github.tommyettinger.ds.IntLongOrderedMap;
import com.github.tommyettinger.ds.IntSet;
import com.github.tommyettinger.ds.Utilities;
import com.github.tommyettinger.ds.support.sort.LongComparators;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Arrays;

import static com.github.tommyettinger.ds.test.PileupTest.LEN;
import static com.github.tommyettinger.ds.test.PileupTest.generatePointSpiral;

/**
 * Trying a shorter LEN value (10K) here so the tests finish quickly.
 * Using 1.12.4's Utilities class:
 * <pre>
 * This used a threshold of 28571 and LEN of 10000
 * 36 problem multipliers in total, 476 likely good multipliers in total.
 * Lowest collisions : 3373
 * Highest collisions: 76238
 * Average collisions: 9126.755859375
 * Lowest pileup     : 7
 * Highest pileup    : 79
 * Likely bad multipliers (base 16):
 * A7E675F3 A4613217 C0A33019 EE99B50F BBEB6F4F D6F7F7AB 80A58489 FCE635CB C2107ECF D689CA89 C0A7D057 825DFCA3 87D6B58B 92648319 844BAB95 9296AFF5 C465778F F4E86F33 9DF00FC5 BF8F0567 95B94A41 9E55CBF3 9E55CBF3 E09F978B B5CE57BB A4E2AF8D DFBF3899 9D9532F7 E73DB38D E72A5A97 CC0B2619 A85E7F21 9AC482DB B6815DDB 937CD501 85C8ADB5
 * </pre>
 * Even though only the first 32 multipliers are used in 1.12.4, one of the problem multipliers has issues when 28
 * rotations through GOOD_MULTIPLIERS have occurred, so it might have issues in practice on very large inputs...
 * <br>
 * Using Utilities2.THREE_PERCENT_MULTIPLIERS, the first 512 as-is:
 * <pre>
 * This used a threshold of 28571 and LEN of 10000
 * 23 problem multipliers in total, 489 likely good multipliers in total.
 * Lowest collisions : 3203
 * Highest collisions: 87588
 * Average collisions: 9508.37109375
 * Lowest pileup     : 7
 * Highest pileup    : 60
 * Likely bad multipliers (base 16):
 * DAC8E8C7 C262775B 17B32B61 69216F17 4CB73891 B7F01169 250BA8FB 437B98C5 6A35C0DD 115583CF 4AF8364D E1A368E3 F0074D35 D39B0435 18354E97 8B5018EF 614C5CFD 752D9E21 9B8C9E0B 7F28885B 56F88E89 661E99C5 39D8AC85
 * </pre>
 * Index 0, which wasn't using the first element in THREE_PERCENT_MULTIPLIERS, has significant problems, and they make
 * it into a problem multiplier when the threshold is just a little tighter.
 */
public class BroadSpectrumMultiplierTest {

	public static void main(String[] args) throws IOException {
		Utilities2.replaceGoodMultipliers(Utilities2.THREE_PERCENT_MULTIPLIERS);
		final Point2[] spiral = generatePointSpiral(LEN);
		IntSet collisions = new IntSet(LEN);
		for (int i = 0; i < LEN; i++) {
			collisions.add(spiral[i].hashCode());
		}
		System.out.println(collisions.size() + "/" + LEN + " hashes are unique.");
//		final long THRESHOLD = (long)(Math.pow(LEN, 11.0/10.0));
		final long THRESHOLD = (long) ((double) LEN * (double) LEN / (0.35 * collisions.size()));

		final int[] problems = {0};
		IntList likelyBad = new IntList(64);
		final int COUNT = 512;
		IntLongOrderedMap good = new IntLongOrderedMap(COUNT);
		int[] buffer = new int[Utilities.GOOD_MULTIPLIERS.length];
		long[] minMax = new long[]{Long.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE, Long.MIN_VALUE};
		for (int a = 0; a < COUNT; a++) {
			final int finalA = a;

			ObjectSet set = new ObjectSet(51, 0.7f) {
				long collisionTotal = 0;
				int longestPileup = 0;

				@Override
				protected void addResize(@NotNull Object key) {
					Object[] keyTable = this.keyTable;
					for (int i = place(key), p = 0; ; i = i + 1 & mask) {
						if (keyTable[i] == null) {
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
					hashMultiplier = Utilities.GOOD_MULTIPLIERS[64 - shift & 511];
					Object[] oldKeyTable = keyTable;

					keyTable = new Object[newSize];

					if (size > 0) {
						for (int i = 0; i < oldCapacity; i++) {
							Object key = oldKeyTable[i];
							if (key != null) {
								addResize(key);
							}
						}
					}
					if (collisionTotal > THRESHOLD) {
						System.out.printf("  WHOOPS!!!  Multiplier 0x%016X on index %4d has %d collisions and %d pileup\n", hashMultiplier, finalA, collisionTotal, longestPileup);
//							good.remove(g);
						likelyBad.add(hashMultiplier);
						problems[0]++;
						throw new RuntimeException();
					}
				}

				@Override
				public void clear() {
					System.out.print(Base.BASE10.unsigned(finalA) + "/" + Base.BASE10.unsigned(COUNT) + ": latest 0x" + Base.BASE16.unsigned(hashMultiplier));
					System.out.println(" gets total collisions: " + collisionTotal + ", PILEUP: " + longestPileup);
					minMax[0] = Math.min(minMax[0], collisionTotal);
					minMax[1] = Math.max(minMax[1], collisionTotal);
					minMax[2] = Math.min(minMax[2], longestPileup);
					minMax[3] = Math.max(minMax[3], longestPileup);
					good.put(finalA, collisionTotal);
					super.clear();
				}
			};
			try {
				for (int i = 0, n = spiral.length; i < n; i++) {
					set.add(spiral[i]);
				}
			} catch (RuntimeException ignored) {
				System.out.println(finalA + " FAILURE");
			}
			set.clear();
			// rotate multipliers by 1
			System.arraycopy(Utilities.GOOD_MULTIPLIERS, 1, buffer, 0, Utilities.GOOD_MULTIPLIERS.length - 1);
			System.arraycopy(Utilities.GOOD_MULTIPLIERS, 0, buffer, Utilities.GOOD_MULTIPLIERS.length - 1, 1);
			System.arraycopy(buffer, 0, Utilities.GOOD_MULTIPLIERS, 0, buffer.length);

		}
		System.out.println("This used a threshold of " + THRESHOLD + " and LEN of " + LEN);
		good.sortByValue(LongComparators.NATURAL_COMPARATOR);

		long bigTotal = 0L;
		for (int i = 0, n = Math.min(512, good.size()); i < n; i++) {
			long collCount = good.getAt(i);
			bigTotal += collCount;
		}
		System.out.println(problems[0] + " problem multipliers in total, " + (COUNT - problems[0]) + " likely good multipliers in total.");
		System.out.println("Lowest collisions : " + minMax[0]);
		System.out.println("Highest collisions: " + minMax[1]);
		System.out.println("Average collisions: " + (bigTotal / Math.min(512.0, good.size())));
		System.out.println("Lowest pileup     : " + minMax[2]);
		System.out.println("Highest pileup    : " + minMax[3]);
		System.out.println("Likely bad multipliers (base 16):");
		System.out.println(likelyBad.toString(" ", false, Base.BASE16::appendUnsigned));
	}

}

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
 */
public class BroadSpectrumMultiplierTest {

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
		System.out.println("This used a threshold of " + THRESHOLD);
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
	}

}

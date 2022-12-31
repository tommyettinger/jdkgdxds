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

package com.github.tommyettinger.ds.test;

import com.github.tommyettinger.digital.Base;
import com.github.tommyettinger.digital.MathTools;
import com.github.tommyettinger.ds.LongLongOrderedMap;
import com.github.tommyettinger.ds.ObjectSet;
import com.github.tommyettinger.ds.support.sort.LongComparators;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.IOException;

import static com.github.tommyettinger.ds.test.PileupTest.LEN;
import static com.github.tommyettinger.ds.test.PileupTest.generateVectorSpiral;

public class AllGoldenVectorHashTest {
	public static void main(String[] args) throws IOException {
		final Vector2[] spiral = generateVectorSpiral(LEN);
		final long THRESHOLD = (long)(Math.pow(LEN, 7.0/6.0));
		LongLongOrderedMap problems = new LongLongOrderedMap(100);
		for (int a = -1; a < MathTools.GOLDEN_LONGS.length; a++) {
			final long g = a == -1 ? 1 : MathTools.GOLDEN_LONGS[a];
			{
				ObjectSet set = new ObjectSet(51, 0.6f) {
					long collisionTotal = 0;
					int longestPileup = 0;

					@Override
					protected void addResize (@NonNull Object key) {
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
					protected void resize (int newSize) {
						int oldCapacity = keyTable.length;
						threshold = (int)(newSize * loadFactor);
						mask = newSize - 1;
						shift = Long.numberOfLeadingZeros(mask);

//						// we modify the hash multiplier by multiplying it by a number that Vigna and Steele considered optimal
//						// for a 64-bit MCG random number generator, XORed with 2 times size to randomize the low bits more.
//						hashMultiplier *= size + size ^ 0xF1357AEA2E62A9C5L;
//						hashMultiplier += 0x6A09E667F3BCC90AL ^ size + size; // fractional part of the silver ratio, times 2 to the 64
//						hashMultiplier ^= size + size; // 86 problems, worst collisions 68609571
//						hashMultiplier ^= hashMultiplier * hashMultiplier * 0x6A09E667F3BCC90AL;
//						hashMultiplier = ~((hashMultiplier ^ -(hashMultiplier * hashMultiplier | 5L)) << 1);
//						hashMultiplier *= 0xD413CCCFE7799215L + size + size; // 113 problems, worst collisions 109417377
//						hashMultiplier += 0xD413CCCFE7799216L + size + size; // 118 problems, worst collisions 296284292
//						hashMultiplier += 0xD413CCCFE7799216L * size; // 137 problems, worst collisions 290750405
						hashMultiplier ^= 0xD413CCCFEL * size; // 105 problems, worst collisions 87972280

						Object[] oldKeyTable = keyTable;

						keyTable = new Object[newSize];

						collisionTotal = 0;
						longestPileup = 0;

						if (size > 0) {
							for (int i = 0; i < oldCapacity; i++) {
								Object key = oldKeyTable[i];
								if (key != null) {addResize(key);}
							}
						}
						if (collisionTotal > THRESHOLD) {
							System.out.printf("  WHOOPS!!!  Multiplier %016X with bit count %d has %d collisions and %d pileup\n", hashMultiplier, Long.bitCount(hashMultiplier), collisionTotal, longestPileup);
							problems.put(g, collisionTotal);
//							throw new RuntimeException();
						}
					}

					@Override
					public void clear () {
						System.out.print("Original 0x" + Base.BASE16.unsigned(g));
						System.out.println(" gets total collisions: " + collisionTotal + ", PILEUP: " + longestPileup);
						super.clear();
					}
				};
				if(a != -1)
					set.setHashMultiplier(g);
				try {
					for (int i = 0, n = spiral.length; i < n; i++) {
						set.add(spiral[i]);
					}
				}catch (RuntimeException ignored){
					System.out.println(g + " FAILURE");
					continue;
				}
				set.clear();
			}
		}
		System.out.println("This used a threshold of " + THRESHOLD);
		problems.sortByValue(LongComparators.NATURAL_COMPARATOR);
		System.out.println(problems);
		System.out.println("\n\n" + problems.size() + " problem multipliers in total.");
	}

}

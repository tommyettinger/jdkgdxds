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
import com.github.tommyettinger.digital.BitConversion;
import com.github.tommyettinger.digital.MathTools;
import com.github.tommyettinger.ds.LongLongOrderedMap;
import com.github.tommyettinger.ds.LongOrderedSet;
import com.github.tommyettinger.ds.ObjectSet;
import com.github.tommyettinger.ds.Utilities;
import com.github.tommyettinger.ds.support.sort.LongComparators;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.IOException;

import static com.github.tommyettinger.ds.test.PileupTest.LEN;
import static com.github.tommyettinger.ds.test.PileupTest.generateVectorSpiral;

public class AllGoldenVectorHashTest {

	public static void main(String[] args) throws IOException {
		final Vector2[] spiral = generateVectorSpiral(LEN);
		final long THRESHOLD = (long)(Math.pow(LEN, 11.0/10.0));// (long)(Math.pow(LEN, 7.0/6.0));
		LongLongOrderedMap problems = new LongLongOrderedMap(100);
		LongOrderedSet good = LongOrderedSet.with(MathTools.GOLDEN_LONGS);
		for (int a = -1; a < MathTools.GOLDEN_LONGS.length; a++) {
			final long g = a == -1 ? 1 : MathTools.GOLDEN_LONGS[a];
			{
				int finalA = a;
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
						shift = BitConversion.countLeadingZeros((long)mask);

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
//						hashMultiplier ^= 0xD413CCCFEL * size; // 105 problems, worst collisions 87972280
//						hashMultiplier = MathTools.GOLDEN_LONGS[(int)(hashMultiplier >>> 40) % MathTools.GOLDEN_LONGS.length]; // 99 problems, worst collisions 68917443
//						hashMultiplier = MathTools.GOLDEN_LONGS[(int)(hashMultiplier & 0xFF)]; // 39 problems, worst collisions 9800516
//						hashMultiplier = MathTools.GOLDEN_LONGS[(int)(hashMultiplier & 0x7F)]; // 0 problems, worst collisions nope
//						hashMultiplier = MathTools.GOLDEN_LONGS[(int)(hashMultiplier >>> 29 & 0xF8) | 7]; // 163 problems, worst collisions 2177454
//						hashMultiplier = Utilities.GOOD_MULTIPLIERS[(int)(hashMultiplier >>> 27) + shift & 0x1FF]; // 0 problems, worst collisions nope

						// this next one deserves some explanation...
						// shift is always between 33 and 63 or so, so adding 48 to it moves it to the 85 to 115 range.
						// but, shifts are always implicitly masked to use only their lowest 6 bits (when shifting longs).
						// this means the shift on hashMultiplier is between 17 and 47, which is a good random-ish range for these.
						hashMultiplier = Utilities.GOOD_MULTIPLIERS[(int)(hashMultiplier >>> 48 + shift) & 511]; // 0 problems, worst collisions nope
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
							System.out.printf("  WHOOPS!!!  Multiplier %016X on index %4d has %d collisions and %d pileup\n", hashMultiplier, finalA, collisionTotal, longestPileup);
							problems.put(g, collisionTotal);
							good.remove(g);
//							throw new RuntimeException();
						}
					}

					@Override
					public void clear () {
						System.out.print("Original 0x" + Base.BASE16.unsigned(g) + " on latest " + Base.BASE16.unsigned(hashMultiplier));
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
		System.out.println("\n\nnew long[]{");
		for (int i = 0; i < Integer.highestOneBit(good.size()); i++) {
			System.out.print("0x"+Base.BASE16.unsigned(good.getAt(i))+"L, ");
			if((i & 3) == 3)
				System.out.println();
		}
		System.out.println("};\n\n" + problems.size() + " problem multipliers in total, " + good.size() + " likely good multipliers in total.");
	}

}

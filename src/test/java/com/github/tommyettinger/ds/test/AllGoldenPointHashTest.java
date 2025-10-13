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
import com.github.tommyettinger.ds.*;
import com.github.tommyettinger.ds.support.sort.IntComparators;
import com.github.tommyettinger.ds.support.sort.LongComparators;
import com.github.tommyettinger.random.EnhancedRandom;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static com.github.tommyettinger.ds.test.PileupTest.*;

/**
 * This uses a different collision tracking method than before; it counts every collision at every size.
 * Using the original 31-based simple hashing, this fails to get under its threshold every time.
 * But, with a no-multiplication Rosenberg-Strong-based hash, it gets:
 * 36 problem multipliers in total, 476 likely good multipliers in total.
 * Lowest collisions : 45222
 * Highest collisions: 673485
 * Lowest pileup     : 1
 * Highest pileup    : 25
 * Adjusting the threshold to be much higher, and switching Point2.hashCode() to be what GridPoint2 uses:
 * 122 problem multipliers in total, 390 likely good multipliers in total.
 * Lowest collisions : 5212359
 * Highest collisions: 6699761
 * Lowest pileup     : 16
 * Highest pileup    : 184
 * With changing hm re-enabled, using (int)(hm >>> 48 + shift) & 511 :
 * 168 problem multipliers in total, 344 likely good multipliers in total.
 * Lowest collisions : 5215319
 * Highest collisions: 6729779
 * Lowest pileup     : 16
 * Highest pileup    : 91
 * With changing hm re-enabled, using (int)(hm * shift >>> 10) & 511 :
 * 267 problem multipliers in total, 245 likely good multipliers in total.
 * Lowest collisions : 5218683
 * Highest collisions: 6445984
 * Lowest pileup     : 16
 * Highest pileup    : 103
 * With changing hm re-enabled, using (int)(hm * shift >>> 5) & 511 :
 * 254 problem multipliers in total, 258 likely good multipliers in total.
 * Lowest collisions : 5218487
 * Highest collisions: 6697191
 * Lowest pileup     : 16
 * Highest pileup    : 62
 * <br>
 * With changing disabled and using the all-unique hashCodes from Coord...
 *   WHOOPS!!!  Multiplier 0xAD9BA24D9CF0D513 on index   28 has 40657592 collisions and 29 pileup
 * 0xAD9BA24D9CF0D513L FAILURE
 *   WHOOPS!!!  Multiplier 0xF5EA757A0A98C863 on index  228 has 57754458 collisions and 127 pileup
 * 0xF5EA757A0A98C863L FAILURE
 *   WHOOPS!!!  Multiplier 0x8DC566B8C4F9DBF5 on index  282 has 21721638 collisions and 24 pileup
 * 0x8DC566B8C4F9DBF5L FAILURE
 *   WHOOPS!!!  Multiplier 0xBD2A41A08F91F0ED on index  342 has 44644926 collisions and 29 pileup
 * 0xBD2A41A08F91F0EDL FAILURE
 *   WHOOPS!!!  Multiplier 0xCB3AFBA6E7EED305 on index  472 has 29584896 collisions and 25 pileup
 * 0xCB3AFBA6E7EED305L FAILURE
 * <br>
 * With changing disabled, all-unique hashCodes from Coord, and testing 51200 randomized multipliers...
 * (Pileup is counted cumulatively here, not reset like in earlier tests.)
 * 1525 problem multipliers in total, 49675 likely good multipliers in total.
 * Lowest collisions : 137457
 * Highest collisions: 3997604
 * Lowest pileup     : 12
 * Highest pileup    : 515
 * <br>
 * Just checking the earlier GOOD_MULTIPLIERS...
 * 12 problem multipliers in total, 500 likely good multipliers in total.
 * Lowest collisions : 539010
 * Highest collisions: 14342524
 * Lowest pileup     : 15
 * Highest pileup    : 166
 * Against the newer ONE_PERCENT_MULTIPLIERS...
 * 0 problem multipliers in total, 512 likely good multipliers in total.
 * Lowest collisions : 624037
 * Highest collisions: 7821127
 * Lowest pileup     : 15
 * Highest pileup    : 32
 */
public class AllGoldenPointHashTest {

	public static void main(String[] args) throws IOException {
		final Point2[] spiral = generatePointSpiral(LEN);

		IntSet collisions = new IntSet(LEN);
		for (int i = 0; i < LEN; i++) {
			collisions.add(spiral[i].hashCode());
		}
		System.out.println(collisions.size() + "/" + LEN + " hashes are unique.");
//		final long THRESHOLD = (long)(Math.pow(LEN, 11.0/10.0));
		final long THRESHOLD = (long) ((double) LEN * (double) LEN / (0.125 * collisions.size()));

		final int[] problems = {0};
		final int COUNT = 512;
		LongLongOrderedMap good = new LongLongOrderedMap(COUNT);
		long[] minMax = new long[]{Long.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE, Long.MIN_VALUE};
		for (int a = 0; a < COUNT; a++) {
//			final long g = EnhancedRandom.fixGamma(a << 1, 1);
			final long g = LongUtilities.ONE_PERCENT_MULTIPLIERS[a];
			good.put(g, 0);
			{
				int finalA = a;
				ObjectSet set = new ObjectSet(51, 0.7f) {
					long collisionTotal = 0;
					int longestPileup = 0;
					long hm = g;

					@Override
					protected int place(@NotNull Object item) {
						return (int) (item.hashCode() * hm >>> shift);
					}

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
//						hashMultiplier = LongUtilities.GOOD_MULTIPLIERS[(int)(hashMultiplier >>> 40) % LongUtilities.GOOD_MULTIPLIERS.length]; // 99 problems, worst collisions 68917443
//						hashMultiplier = LongUtilities.GOOD_MULTIPLIERS[(int)(hashMultiplier & 0xFF)]; // 39 problems, worst collisions 9800516
//						hashMultiplier = LongUtilities.GOOD_MULTIPLIERS[(int)(hashMultiplier & 0x7F)]; // 0 problems, worst collisions nope
//						hashMultiplier = LongUtilities.GOOD_MULTIPLIERS[(int)(hashMultiplier >>> 29 & 0xF8) | 7]; // 163 problems, worst collisions 2177454
//						hashMultiplier = Utilities.GOOD_MULTIPLIERS[(int)(hashMultiplier >>> 27) + shift & 0x1FF]; // 0 problems, worst collisions nope

						// this next one deserves some explanation...
						// shift is always between 33 and 63 or so, so adding 48 to it moves it to the 85 to 115 range.
						// but, shifts are always implicitly masked to use only their lowest 6 bits (when shifting longs).
						// this means the shift on hashMultiplier is between 17 and 47, which is a good random-ish range for these.
//						hashMultiplier = Utilities.GOOD_MULTIPLIERS[(hashMultiplier ^ hashMultiplier >>> 17 ^ shift) & 511]; // 0 problems, worst collisions nope

//						hashMultiplier = LongUtilities.GOOD_MULTIPLIERS[(int)(hashMultiplier >>> 48 + shift) & 511];
//						int index = (int)(hm >>> 48 + shift) & 511;
//						int index = (int)(hm * shift >>> 10) & 511;
//						int index = (int) (hm * shift >>> 5) & 511;
//						int index = 64 - shift + finalA & 511;
//						chosen[index]++;
						hashMultiplier = Utilities.GOOD_MULTIPLIERS[finalA & 511];
//						hm = LongUtilities.GOOD_MULTIPLIERS[index];
						hm = g;
						Object[] oldKeyTable = keyTable;

						keyTable = new Object[newSize];

//						collisionTotal = 0;
//						longestPileup = 0;

						if (size > 0) {
							for (int i = 0; i < oldCapacity; i++) {
								Object key = oldKeyTable[i];
								if (key != null) {
									addResize(key);
								}
							}
						}
						if (collisionTotal > THRESHOLD) {
							System.out.printf("  WHOOPS!!!  Multiplier 0x%016X on index %4d has %d collisions and %d pileup\n", hm, finalA, collisionTotal, longestPileup);
							good.remove(g);
							problems[0]++;
							throw new RuntimeException();
						}
					}

					@Override
					public void clear() {
						System.out.print(Base.BASE10.unsigned(finalA) + "/" + Base.BASE10.unsigned(COUNT) + ": Original 0x" + Base.BASE16.unsigned(g) + " on latest " + Base.BASE16.unsigned(hm));
						System.out.println(" gets total collisions: " + collisionTotal + ", PILEUP: " + longestPileup);
						minMax[0] = Math.min(minMax[0], collisionTotal);
						minMax[1] = Math.max(minMax[1], collisionTotal);
						minMax[2] = Math.min(minMax[2], longestPileup);
						minMax[3] = Math.max(minMax[3], longestPileup);
						good.put(g, collisionTotal);
						super.clear();
					}

					public void setHashMultiplier(int index) {
						super.setHashMultiplier(Utilities.GOOD_MULTIPLIERS[index & 511]);
//						hm = EnhancedRandom.fixGamma(index << 1, 1);
						hm = LongUtilities.ONE_PERCENT_MULTIPLIERS[index & 511];
					}
				};
				set.setHashMultiplier(finalA);
				try {
					for (int i = 0, n = spiral.length; i < n; i++) {
						set.add(spiral[i]);
					}
				} catch (RuntimeException ignored) {
					System.out.println("0x"+ Base.BASE16.unsigned(g) + "L FAILURE");
					continue;
				}
				set.clear();
			}
		}
		System.out.println("This used a threshold of " + THRESHOLD);
		good.sortByValue(LongComparators.NATURAL_COMPARATOR);

		System.out.println("\n\npublic static final long[] GOOD_MULTIPLIERS = new long[]{");
		for (int i = 0, n = Math.min(good.size(), 600); i < n; i++) {
			System.out.println("0x" + Base.BASE16.unsigned(good.keyAt(i)) + "L, //" + Base.BASE10.signed(good.getAt(i)));
		}
		System.out.println("};\n");
		System.out.println(problems[0] + " problem multipliers in total, " + (COUNT - problems[0]) + " likely good multipliers in total.");
		System.out.println("Lowest collisions : " + minMax[0]);
		System.out.println("Highest collisions: " + minMax[1]);
		System.out.println("Lowest pileup     : " + minMax[2]);
		System.out.println("Highest pileup    : " + minMax[3]);
	}

}

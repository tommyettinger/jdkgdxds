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
import com.github.tommyettinger.ds.*;
import com.github.tommyettinger.ds.support.sort.IntComparators;
import com.github.tommyettinger.ds.support.sort.LongComparators;
import org.checkerframework.checker.nullness.qual.NonNull;

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
		final long THRESHOLD = (long)((double)LEN * (double) LEN / (0.125 * collisions.size()));

		final int[] problems = {0};
		final int COUNT = 512;
		LongIntOrderedMap good = new LongIntOrderedMap(512);
		for (int x = 0; x < COUNT; x++) {
			good.put(LongUtilities.GOOD_MULTIPLIERS[x], 0);
		}
		long[] minMax = new long[]{Long.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE, Long.MIN_VALUE};
		short[] chosen = new short[512];
		for (int a = 0; a < COUNT; a++) {
			final long g = LongUtilities.GOOD_MULTIPLIERS[a];
			{
				int finalA = a;
				ObjectSet set = new ObjectSet(51, 0.6f) {
					long collisionTotal = 0;
					int longestPileup = 0;
					long hm = hashMultiplier * 0xF1357AEA2E62A9C5L;

					@Override
					protected int place(@NonNull Object item) {
						return (int)(item.hashCode() * hm >>> shift);
					}

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
								good.put(g, longestPileup);
							}
						}
					}

					@Override
					protected void resize (int newSize) {
						int oldCapacity = keyTable.length;
						threshold = (int)(newSize * loadFactor);
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
						int index = (int)(hm * shift >>> 5) & 511;
						chosen[index]++;
						hashMultiplier = Utilities.GOOD_MULTIPLIERS[index];
						hm = LongUtilities.GOOD_MULTIPLIERS[index];
						Object[] oldKeyTable = keyTable;

						keyTable = new Object[newSize];

//						collisionTotal = 0;
						longestPileup = 0;

						if (size > 0) {
							for (int i = 0; i < oldCapacity; i++) {
								Object key = oldKeyTable[i];
								if (key != null) {addResize(key);}
							}
						}
						if (collisionTotal > THRESHOLD) {
//							System.out.printf("  WHOOPS!!!  Multiplier %016X on index %4d has %d collisions and %d pileup\n", hashMultiplier, finalA, collisionTotal, longestPileup);
//							problems.put(g, collisionTotal);
//							good.remove(g);
							problems[0]++;
							throw new RuntimeException();
						}
					}

					@Override
					public void clear () {
						System.out.print(Base.BASE10.unsigned(finalA) + "/" + Base.BASE10.unsigned(COUNT) + ": Original 0x" + Base.BASE16.unsigned(g) + " on latest " + Base.BASE16.unsigned(hm));
						System.out.println(" gets total collisions: " + collisionTotal + ", PILEUP: " + good.get(g));
						minMax[0] = Math.min(minMax[0], collisionTotal);
						minMax[1] = Math.max(minMax[1], collisionTotal);
						minMax[2] = Math.min(minMax[2], good.get(g));
						minMax[3] = Math.max(minMax[3], good.get(g));
						super.clear();
					}

					public void setHashMultiplier(int index) {
						super.setHashMultiplier(Utilities.GOOD_MULTIPLIERS[index & 511]);
						hm = LongUtilities.GOOD_MULTIPLIERS[index & 511];
					}
				};
				set.setHashMultiplier(finalA);
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
		System.out.println("Indices used: ");
		for (int y = 0, idx = 0; y < 32; y++) {
			for (int x = 0; x < 16; x++) {
				System.out.print(Base.BASE16.unsigned(chosen[idx++]) + " ");
			}
			System.out.println();
		}
		good.sortByValue(IntComparators.NATURAL_COMPARATOR);

		System.out.println("\n\nint[] GOOD_MULTIPLIERS = new int[]{");
		for (int i = 0; i < Integer.highestOneBit(good.size()); i++) {
			System.out.print("0x"+Base.BASE16.unsigned(good.keyAt(i))+"=0x"+Base.BASE16.unsigned(good.getAt(i))+", ");
			if((i & 7) == 7)
				System.out.println();
		}
		System.out.println("};\n");
		System.out.println(problems[0] + " problem multipliers in total, " + (COUNT - problems[0]) + " likely good multipliers in total.");
		System.out.println("Lowest collisions : " + minMax[0]);
		System.out.println("Highest collisions: " + minMax[1]);
		System.out.println("Lowest pileup     : " + minMax[2]);
		System.out.println("Highest pileup    : " + minMax[3]);
	}

}

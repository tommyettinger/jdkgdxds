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

import org.checkerframework.checker.nullness.qual.NonNull;

import static com.github.tommyettinger.ds.test.PileupTest.LEN;
import static com.github.tommyettinger.ds.test.PileupTest.generateVectorSpiral;

public class ExhaustiveVectorHashTest {
	// These hash multipliers are all the value they had after many resizes; to get the
	// original (starting) value, use the iteration number for the hashShiftA below.

	//hash multiplier: 0x6CF8E91E893CB78D on iteration 287365
	//gets total collisions: 1757600, PILEUP: 21

	//hash multiplier: 0x4C7DA0AB8EFC274B on iteration 286778
	//gets total collisions: 1754581, PILEUP: 21

	//hash multiplier: 0x2D73A0F706C4DFE7 on iteration 366992
	//gets total collisions: 1753314, PILEUP: 21

	//hash multiplier: 0xA514593820923135 on iteration 14025
	//gets total collisions: 1752811, PILEUP: 21

	//hash multiplier: 0x5ADB4D08F27F4CC1 on iteration 280687
	//gets total collisions: 1752780, PILEUP: 21

	//hash multiplier: 0xBF276EB5FCCDECA3 on iteration 271814
	//gets total collisions: 1752322, PILEUP: 21

	//hash multiplier: 0x48887737D50F5C31 on iteration 333383
	//gets total collisions: 1761164, PILEUP: 20

	//hash multiplier: 0xFF97035ABD3FB86D on iteration 245941
	//gets total collisions: 1753275, PILEUP: 20

	//hash multiplier: 0x856008634862D9E3 on iteration 243814
	//gets total collisions: 1761470, PILEUP: 19

	// Using newer resize() that XORs by 0xF1357AEA2E62A9C1L

	//hash multiplier: 0x4A66C1BB08EF3BC9 on iteration 405
	//gets total collisions: 1758213, PILEUP: 23

	public static void main(String[] args) { //testVector2SetExhaustive
		final Vector2[] spiral = generateVectorSpiral(LEN);

		for (int a = 0; a < 4000; a++) {
//            for (int b = a + 1; b < 32; b++)
			{
				// change hashShiftA to an iteration in the above listing to get its original multiplier
//                final int hashShiftA = 243814, hashShiftB = 1;
				final int hashShiftA = a, hashShiftB = 1;
				ObjectSet set = new ObjectSet(51, 0.6f) {
					long collisionTotal = 0;
					int longestPileup = 0;
					int originalMultiplier;
					int hashAddend = 0x9E3779B9;

					{
						long hashMultiplier = 0xD1B54A32D192ED03L;
						long ctr = hashMultiplier << 1;
						for (int i = 0; i < hashShiftA; i++) {
							hashAddend = hashAddend * hashAddend + (int) (ctr += 0x9E3779B97F4A7C16L);
						}
						originalMultiplier = hashAddend;
					}

					//					int hashAddend = 0xD1B54A32;
//					{
//						hashMultiplier = 0xD1B54A32D192ED03L;
//						long ctr = hashAddend;
//						for (int i = 0; i < hashShiftA; i++) {
//							hashMultiplier = hashMultiplier * hashMultiplier + (ctr += 0x9E3779B97F4A7C16L);
//						}
//						originalMultiplier = hashMultiplier;
//					}
					@Override
					protected int place(@NonNull Object item) {
						return item.hashCode() * hashAddend >>> shift;
//						return (int)(item.hashCode() * hashMultiplier >>> shift);
//                        final int h = item.hashCode() + (int)(hashMultiplier>>>32);
//                        return (h ^ h >>> hashShiftA ^ h >>> hashShiftB) & mask;
//                        return (h ^ h >>> hashShiftA) + hashAddend & mask;
					}

					@Override
					protected void addResize(@NonNull Object key) {
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
//                        hashMultiplier *= ((long)size << 3) ^ 0xF1357AEA2E62A9C1L;
						hashMultiplier *= (long) size << 3 ^ 0xF1357AEA2E62A9C5L;
						hashAddend = hashAddend * 0x2E62A9C5;
//                        hashAddend = (hashAddend ^ hashAddend >>> 11 ^ size) * 0x13C6EB ^ 0xC79E7B1D;

						Object[] oldKeyTable = keyTable;

						keyTable = new Object[newSize];

						collisionTotal = 0;
						longestPileup = 0;

						if (size > 0) {
							for (int i = 0; i < oldCapacity; i++) {
								Object key = oldKeyTable[i];
								if (key != null) {
									addResize(key);
								}
							}
						}
						if (collisionTotal > 1810000L) throw new RuntimeException();
//                        System.out.println("hash multiplier: " + Base.BASE16.unsigned(hashMultiplier) + " with new size " + newSize);
//                        System.out.println("total collisions: " + collisionTotal);
//                        System.out.println("longest pileup: " + longestPileup);
					}

					@Override
					public void clear() {
						if (longestPileup <= 23) {
							System.out.println("hash multiplier: 0x" + Base.BASE16.unsigned(originalMultiplier) + " on iteration " + hashShiftA);
//                            System.out.println("shifts: a " + hashShiftA + ", b " + hashShiftB);
							System.out.println("gets total collisions: " + collisionTotal + ", PILEUP: " + longestPileup);
						}
						super.clear();
					}
				};
//        final int limit = (int)(Math.sqrt(LEN));
//        for (int x = -limit; x < limit; x+=2) {
//            for (int y = -limit; y < limit; y+=2) {
//                set.add(new Vector2(x, y));
//            }
//        }
				try {
					for (int i = 0; i < LEN; i++) {
						set.add(spiral[i]);
					}
				} catch (RuntimeException ignored) {
					continue;
				}
//        System.out.println(System.nanoTime() - start);
				set.clear();
			}
		}
	}

}

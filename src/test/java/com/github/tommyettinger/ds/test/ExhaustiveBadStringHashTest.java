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
import com.github.tommyettinger.ds.ObjectSet;

import javax.annotation.Nonnull;

import static com.github.tommyettinger.ds.test.PileupTest.LEN;
import static com.github.tommyettinger.ds.test.PileupTest.generateUniqueBad;

public class ExhaustiveBadStringHashTest {
	// FLOAT BITS OF STRING HASHCODE

	//hash multiplier: 0x5E5F6580F1FBDD77 on iteration 280
	//gets total collisions: 62508, PILEUP: 10

	//hash multiplier: 0x514394EB2C83D22B on iteration 9386
	//gets total collisions: 60693, PILEUP: 9

	// Using newer resize code that XORs by 0xF1357AEA2E62A9C1L

	//hash multiplier: 0x7A5A85AE2AD70CD5 on iteration 2075
	//gets total collisions: 63283, PILEUP: 9

	// STRING HASHCODE

	//hash multiplier: 0xA2762D7BDFCD4717 on iteration 104
	//gets total collisions: 56173, PILEUP: 9

	public static void main(String[] args) {
		final BadString[] words = generateUniqueBad(LEN, -123456789L);

		for (int a = 0; a < 40000; a++) {
//            for (int b = a + 1; b < 32; b++)
			{
				final int hashShiftA = a, hashShiftB = 1;
				ObjectSet set = new ObjectSet(51, 0.6f) {
					long collisionTotal = 0;
					int longestPileup = 0;
					long originalMultiplier;
					int hashAddend = 0xD1B54A32;
					{
						hashMultiplier = 0xD1B54A32D192ED03L;
						long ctr = hashAddend;
						for (int i = 0; i < hashShiftA; i++) {
							hashMultiplier = hashMultiplier * hashMultiplier + (ctr += 0x9E3779B97F4A7C16L);
						}
						originalMultiplier = hashMultiplier;
					}
					@Override
					protected int place (Object item) {
						return (int)(item.hashCode() * hashMultiplier >>> shift);
//                        final int h = item.hashCode() + (int)(hashMultiplier>>>32);
//                        return (h ^ h >>> hashShiftA ^ h >>> hashShiftB) & mask;
//                        return (h ^ h >>> hashShiftA) + hashAddend & mask;
					}

					@Override
					protected void addResize (@Nonnull Object key) {
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

						hashMultiplier *= ((long)size << 3) ^ 0xF1357AEA2E62A9C5L;
//                        hashAddend = (hashAddend ^ hashAddend >>> 11 ^ size) * 0x13C6EB ^ 0xC79E7B1D;

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
						if(longestPileup > 18) throw new RuntimeException();
//                        System.out.println("hash multiplier: " + Base.BASE16.unsigned(hashMultiplier) + " with new size " + newSize);
//                        System.out.println("total collisions: " + collisionTotal);
//                        System.out.println("longest pileup: " + longestPileup);
					}

					@Override
					public void clear () {
						if(longestPileup <= 11) {
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
						set.add(words[i]);
					}
				}catch (RuntimeException ignored){
					continue;
				}
//        System.out.println(System.nanoTime() - start);
				set.clear();
			}
		}
	}

}

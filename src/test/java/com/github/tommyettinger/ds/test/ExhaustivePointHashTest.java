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

import org.checkerframework.checker.nullness.qual.NonNull;

import static com.github.tommyettinger.ds.test.PileupTest.*;

public class ExhaustivePointHashTest {
	// hundreds of the tested hash constants do perfectly here, with 0 total collisions and 0 pileup.
	// Ones I want to try: 0x7587 by 0x6A89, 0xBB85B by 0xD2BCD, 0x9122F by 0xDA877, 0xA0DBD by 0xCB2E3 ...
	public static void main(String[] args) {
		final Point2[] spiral = generatePointRectangle(LEN / 50, 50, 0);

		for (int a = 0; a < 4000; a++) {
//            for (int b = a + 1; b < 32; b++)
			{
				// change hashShiftA to an iteration in the above listing to get its original multiplier
//                final int hashShiftA = 243814, hashShiftB = 1;
				final int hashShiftA = a;
				ObjectSet set = new ObjectSet(51, 0.6f) {
					long collisionTotal = 0;
					int longestPileup = 0;
					long originalMultiplier;
					int hashX = 0x12345, hashY = 0x54321;
					{
						originalMultiplier = 0xD1B54A32D192ED03L;
						for (int i = 0; i < hashShiftA; i++) {
							originalMultiplier = originalMultiplier * 0xF1357AEA2E62A9C5L + 0x9E3779B97F4A7C15L;
						}
						hashX = ((int)(originalMultiplier >>> 24) & 0xFFFFF) | 1;
						hashY = ((int)(originalMultiplier >>> 44) & 0xFFFFF) | 1;
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
					protected int place (Object item) {
						final Point2 p = (Point2)item;
						return p.x * hashX + p.y * hashY & mask;
//						return (int)(item.hashCode() * hashMultiplier >>> shift);
//                        final int h = item.hashCode() + (int)(hashMultiplier>>>32);
//                        return (h ^ h >>> hashShiftA ^ h >>> hashShiftB) & mask;
//                        return (h ^ h >>> hashShiftA) + hashAddend & mask;
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
							}
						}
					}

					@Override
					protected void resize (int newSize) {
						int oldCapacity = keyTable.length;
						threshold = (int)(newSize * loadFactor);
						mask = newSize - 1;
						shift = com.github.tommyettinger.digital.BitConversion.countLeadingZeros(mask);
//                        hashMultiplier *= ((long)size << 3) ^ 0xF1357AEA2E62A9C1L;
//						hashMultiplier *= (long)size << 3 ^ 0xF1357AEA2E62A9C5L;

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
						if(collisionTotal > 1810000L) throw new RuntimeException();
//                        System.out.println("hash multiplier: " + Base.BASE16.unsigned(hashMultiplier) + " with new size " + newSize);
//                        System.out.println("total collisions: " + collisionTotal);
//                        System.out.println("longest pileup: " + longestPileup);
					}

					@Override
					public void clear () {
						if(longestPileup == 0)
						{
							System.out.println("hash multiplier: 0x" + Base.BASE16.signed(hashX) + " by 0x" + Base.BASE16.signed(hashY) + " on iteration " + hashShiftA);
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
				}catch (RuntimeException ignored){
					continue;
				}
//        System.out.println(System.nanoTime() - start);
				set.clear();
			}
		}
	}

}

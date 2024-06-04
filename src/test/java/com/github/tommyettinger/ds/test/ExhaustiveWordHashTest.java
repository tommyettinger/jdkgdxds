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
import com.github.tommyettinger.ds.ObjectSet;
import com.github.tommyettinger.random.WhiskerRandom;

import org.checkerframework.checker.nullness.qual.NonNull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

public class ExhaustiveWordHashTest {

	// these two: {
	//hash * 0x95C0793F + 0xEEC45107 on iteration 1428
	//gets total collisions: 32984, PILEUP: 9

	//hash * 0x95C0793F on iteration 1428
	//gets total collisions: 32984, PILEUP: 9
	//}
	// have identical collisions and pileup, so the addition does nothing to help.

	//hash * 0xEE1862A3 on iteration 6486
	//gets total collisions: 33339, PILEUP: 9

	//hash * 0x8A58E8C9 on iteration 22551
	//gets total collisions: 33359, PILEUP: 9

	//hash * 0x04DA4427 on iteration 32184
	//gets total collisions: 33026, PILEUP: 9

	//hash * 0x92D390C1 on iteration 38611
	//gets total collisions: 33180, PILEUP: 9

	//hash * 0x38442BE5 on iteration 41565
	//gets total collisions: 33175, PILEUP: 9

	//hash * 0x94BF16E7FCB6C8F3 on iteration 3678
	//gets total collisions: 33063, PILEUP: 9
	public static void main(String[] args) throws IOException {
		final List<String> words = Files.readAllLines(Paths.get("src/test/resources/word_list.txt"));
		WhiskerRandom rng = new WhiskerRandom(1234567890L);
		for (int a = 0; a < 100000; a++) {
//            for (int b = a + 1; b < 32; b++)
			Collections.shuffle(words, rng);
			{
				final int hashShiftA = a, hashShiftB = 1;
				ObjectSet set = new ObjectSet(51, 0.6f) {
					long collisionTotal = 0;
					long originalMultiplier;
					int longestPileup = 0;
					int originalMul, originalAdd;
					int hashMul = 0x9E377, hashAdd = 0xD192ED03;
					{
						long hashMultiplier = 0xD1B54A32D192ED03L;
						long ctr = hashMultiplier << 1;
						for (int i = 0; i < hashShiftA; i++) {
							hashMultiplier = hashMultiplier * hashMultiplier + (ctr += 0x9E3779B97F4A7C16L);
							hashMul = hashMul + (int)ctr & 0xFFFFF;
							hashAdd += ctr;
						}
						originalMultiplier = hashMultiplier;
						originalMul = hashMul;
						originalAdd = hashAdd;
					}
//					long originalMultiplier;
//					int longestPileup = 0;
//					int originalMul, originalAdd;
//					int hashMul = 0x9E3779B9, hashAdd = 0xD192ED03;
//					{
//						hashMultiplier = 0xD1B54A32D192ED03L;
//						long ctr = hashMultiplier << 1;
//						for (int i = 0; i < hashShiftA; i++) {
//							hashMultiplier = hashMultiplier * hashMultiplier + (ctr += 0x9E3779B97F4A7C16L);
//							hashMul = hashMul * hashMul + (int)ctr;
//							hashAdd += ctr;
//						}
//						originalMultiplier = hashMultiplier;
//						originalMul = hashMul;
//						originalAdd = hashAdd;
//					}

//					long originalMultiplier;
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
						return item.hashCode() * hashMul >>> shift;
//						return item.hashCode() * hashMul + hashAdd >>> shift;
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
						shift = BitConversion.countLeadingZeros((long)mask);

						hashMultiplier *= ((long)size << 3) ^ 0xF1357AEA2E62A9C5L;
						hashMul =  hashMul * 0x9E377 & 0xFFFFF;
//						hashMul *= 0x2E62A9C5 ^ size + size;
						hashAdd += 0xF1357AEA;
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
						if(longestPileup > 13) throw new RuntimeException();
//                        System.out.println("hash multiplier: " + Base.BASE16.unsigned(hashMultiplier) + " with new size " + newSize);
//                        System.out.println("total collisions: " + collisionTotal);
//                        System.out.println("longest pileup: " + longestPileup);
					}

					@Override
					public void clear () {
						if(longestPileup <= 10) {
							System.out.println(
//								"hash * 0x" + Base.BASE16.unsigned(originalMultiplier) +
								"hash * 0x" + Base.BASE16.unsigned(originalMul) +
//								" + 0x" + Base.BASE16.unsigned(originalAdd) +
								" on iteration " + hashShiftA);
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
					for (int i = 0, n = words.size(); i < n; i++) {
						set.add(words.get(i));
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

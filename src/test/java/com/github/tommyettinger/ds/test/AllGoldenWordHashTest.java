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
import com.github.tommyettinger.ds.ObjectSet;
import com.github.tommyettinger.ds.Utilities;
import com.github.tommyettinger.ds.support.sort.LongComparators;
import com.github.tommyettinger.random.WhiskerRandom;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

public class AllGoldenWordHashTest {
	public static void main(String[] args) throws IOException {
		final List<String> words = Files.readAllLines(Paths.get("src/test/resources/word_list.txt"));
		WhiskerRandom rng = new WhiskerRandom(1234567890L);
		Collections.shuffle(words, rng);
		LongLongOrderedMap problems = new LongLongOrderedMap(100);
		long[] minMax = new long[]{Long.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE, Long.MIN_VALUE};
		for (int a = -1; a < MathTools.GOLDEN_LONGS.length; a++) {
			final long g = a == -1 ? 1 : MathTools.GOLDEN_LONGS[a];
			{
				ObjectSet set = new ObjectSet(51, 0.6f) {
					long collisionTotal = 0;
					int longestPileup = 0;
					/* // with default behavior, changing hashMultiplier on resize
0 problem multipliers in total.
Lowest collisions : 33094
Highest collisions: 34642
Lowest pileup     : 10
Highest pileup    : 18

					 */
					/** // using below hashMul as an int
					 * 0 problem multipliers in total.
					 * Lowest collisions : 32959
					 * Highest collisions: 34389
					 * Lowest pileup     : 10
					 * Highest pileup    : 19
					 */
					int hashMul = (int)((hashMultiplier >>> 32 & -16) | (hashMultiplier & 15));
//					@Override
//					protected int place (Object item) {
//						return item.hashCode() * hashMul >>> shift;
//					}

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

//						hashMultiplier = Utilities.GOOD_MULTIPLIERS[(int)(hashMultiplier >>> 27) + shift & 0x1FF];
						hashMultiplier = Utilities.GOOD_MULTIPLIERS[(int)(hashMultiplier >>> 48 + shift) & 511];
						hashMul = (int)((hashMultiplier >>> 32 & -16) | (hashMultiplier & 15));
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
						if(collisionTotal > 40000) {
							problems.put(g, collisionTotal);
//							throw new RuntimeException();
						}
					}

					@Override
					public void clear () {
						System.out.print("Original 0x" + Base.BASE16.unsigned(g));
						System.out.println(" gets total collisions: " + collisionTotal + ", PILEUP: " + longestPileup);
						minMax[0] = Math.min(minMax[0], collisionTotal);
						minMax[1] = Math.max(minMax[1], collisionTotal);
						minMax[2] = Math.min(minMax[2], longestPileup);
						minMax[3] = Math.max(minMax[3], longestPileup);
						super.clear();
					}
				};
				if(a != -1)
					set.setHashMultiplier(g);
				try {
					for (int i = 0, n = words.size(); i < n; i++) {
						set.add(words.get(i));
					}
				}catch (RuntimeException ignored){
					System.out.println(g + " FAILURE");
					continue;
				}
				set.clear();
			}
		}
		problems.sortByValue(LongComparators.NATURAL_COMPARATOR);
		System.out.println("\n\nnew long[]");
		System.out.println(problems);
		System.out.println(";\n\n" + problems.size() + " problem multipliers in total.");
		System.out.println("Lowest collisions : " + minMax[0]);
		System.out.println("Highest collisions: " + minMax[1]);
		System.out.println("Lowest pileup     : " + minMax[2]);
		System.out.println("Highest pileup    : " + minMax[3]);

	}

}

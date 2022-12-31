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
import com.github.tommyettinger.ds.ObjectSet;
import com.github.tommyettinger.ds.Utilities;
import com.github.tommyettinger.random.WhiskerRandom;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

/**
 * Wow, because of the randomization in resize(), these "poor-quality" hash multipliers aren't bad at all!
 */
public class AllPoorWordHashTest {
	public static void main(String[] args) throws IOException {
		final List<String> words = Files.readAllLines(Paths.get("src/test/resources/word_list.txt"));
		WhiskerRandom rng = new WhiskerRandom(1234567890L);
		Collections.shuffle(words, rng);
		final long[] POOR = new long[]{1L, -1L, -1L << 32, 0x4000000000000L, 0x4000000000000L - 1L, 0x5555555555555555L,
			MathTools.modularMultiplicativeInverse(3L), MathTools.modularMultiplicativeInverse(5L),
			MathTools.modularMultiplicativeInverse(7L), MathTools.modularMultiplicativeInverse(9L),
			MathTools.modularMultiplicativeInverse(0xF1357AEA2E62A9C5L), // this one makes the hashMultiplier 1L briefly
		};
		for (int a = 0; a < POOR.length; a++) {
			final long g = POOR[a] | 1L;
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

						hashMultiplier = Utilities.GOOD_MULTIPLIERS[(int)(hashMultiplier >>> 27) + shift & 511];

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
						if(collisionTotal > 40000) throw new RuntimeException();
					}

					@Override
					public void clear () {
							System.out.print("Original 0x" + Base.BASE16.unsigned(g));
							System.out.println(" gets total collisions: " + collisionTotal + ", PILEUP: " + longestPileup);
						super.clear();
					}
				};
				set.setHashMultiplier(g);
				try {
					for (int i = 0, n = words.size(); i < n; i++) {
						set.add(words.get(i));
					}
				}catch (RuntimeException ignored){
					System.out.println(g + " FAILURE");
					continue;
				}
//        System.out.println(System.nanoTime() - start);
				set.clear();
			}
		}
	}

}

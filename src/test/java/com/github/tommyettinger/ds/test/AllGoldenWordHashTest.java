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
import com.github.tommyettinger.random.WhiskerRandom;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

public class AllGoldenWordHashTest {

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
		for (int a = 0; a < MathTools.GOLDEN_LONGS.length; a++) {
			final long g = MathTools.GOLDEN_LONGS[a];
//            for (int b = a + 1; b < 32; b++)
			Collections.shuffle(words, rng);
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

						// we modify the hash multiplier by multiplying it by a number that Vigna and Steele considered optimal
						// for a 64-bit MCG random number generator, XORed with 2 times size to randomize the low bits more.
						hashMultiplier *= size + size ^ 0xF1357AEA2E62A9C5L;

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

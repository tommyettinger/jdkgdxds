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
import com.github.tommyettinger.ds.IntIntOrderedMap;
import com.github.tommyettinger.ds.ObjectSet;
import com.github.tommyettinger.ds.support.sort.IntComparators;
import com.github.tommyettinger.random.WhiskerRandom;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import static com.github.tommyettinger.ds.test.PileupTest.*;

/**
 *
 */
public class SmallCombinedHashTest {
	public static final int LEN = 200000;
	public static final int COUNT = 65535;
	public static final IntIntOrderedMap good = new IntIntOrderedMap(COUNT);

	static {
		int running = 1;
		for (int x = 0; x < COUNT; x++) {
			good.put((running += 16) | 0x100000, 0);
		}
	}
	public static void main(String[] args) throws IOException {
		final List<String> words = Files.readAllLines(Paths.get("src/test/resources/word_list.txt"));
		WhiskerRandom rng = new WhiskerRandom(1234567890L);
		Collections.shuffle(words, rng);
		final int LEN = words.size();
        final Point2[] pointSpiral = generatePointSpiral(LEN);
		final Vector2[] vectorSpiral = generateVectorSpiral(LEN);

		final int[] problems = {0};
		for (int a = 0; a < COUNT; a++) {
			final int g = good.keyAt(a);
			{
				MetricSet pointSet = new MetricSet(g);
				MetricSet vectorSet = new MetricSet(g);
				MetricSet wordSet = new MetricSet(g);
				for (int i = 0, n = LEN; i < n; i++) {
					pointSet.add(pointSpiral[i]);
					vectorSet.add(vectorSpiral[i]);
					wordSet.add(words.get(i));
				}
				System.out.print(Base.BASE10.unsigned(a) + "/" + Base.BASE10.unsigned(COUNT) + " P ");
				pointSet.clear();
				System.out.print(Base.BASE10.unsigned(a) + "/" + Base.BASE10.unsigned(COUNT) + " V ");
				vectorSet.clear();
				System.out.print(Base.BASE10.unsigned(a) + "/" + Base.BASE10.unsigned(COUNT) + " W ");
				wordSet.clear();
			}
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
		System.out.println("Lowest collisions : " + MetricSet.minMax[0]);
		System.out.println("Highest collisions: " + MetricSet.minMax[1]);
		System.out.println("Lowest pileup     : " + MetricSet.minMax[2]);
		System.out.println("Highest pileup    : " + MetricSet.minMax[3]);
	}

	private static class MetricSet extends ObjectSet {
		private final int initialMul;
        public static final long[] minMax = new long[]{Long.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE, Long.MIN_VALUE};
		long collisionTotal;
		int longestPileup;

		public MetricSet(int initialMul) {
			super(51, 0.6f);
			hashMultiplier = initialMul;
			this.initialMul = initialMul;
			collisionTotal = 0;
			longestPileup = good.get(initialMul);
		}

		@Override
		protected int place (Object item) {
			return item.hashCode() * hashMultiplier >>> shift;
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
					good.put(initialMul, longestPileup);
				}
			}
		}

		@Override
		protected void resize (int newSize) {
			int oldCapacity = keyTable.length;
			threshold = (int)(newSize * loadFactor);
			mask = newSize - 1;
			shift = BitConversion.countLeadingZeros(mask) + 32;

//						int index = (hm * shift >>> 5) & 511;
//						chosen[index]++;
//						hashMultiplier = hm = GOOD[index];
			Object[] oldKeyTable = keyTable;

			keyTable = new Object[newSize];

			longestPileup = 0;

			if (size > 0) {
				for (int i = 0; i < oldCapacity; i++) {
					Object key = oldKeyTable[i];
					if (key != null) {addResize(key);}
				}
			}
		}

		@Override
		public void clear () {
			System.out.print(": Original 0x" + Base.BASE16.unsigned(initialMul) + " on latest " + Base.BASE16.unsigned(hashMultiplier));
			System.out.println(" gets total collisions: " + collisionTotal + ", PILEUP: " + good.get(initialMul));
			minMax[0] = Math.min(minMax[0], collisionTotal);
			minMax[1] = Math.max(minMax[1], collisionTotal);
			minMax[2] = Math.min(minMax[2], good.get(initialMul));
			minMax[3] = Math.max(minMax[3], good.get(initialMul));
			super.clear();
		}

		@Override
		public void setHashMultiplier (int hashMultiplier) {
			this.hashMultiplier = hashMultiplier | 1;
			resize(keyTable.length);
		}
	}
}

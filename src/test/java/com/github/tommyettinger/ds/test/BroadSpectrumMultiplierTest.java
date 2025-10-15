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
import com.github.tommyettinger.ds.IntList;
import com.github.tommyettinger.ds.IntLongOrderedMap;
import com.github.tommyettinger.ds.IntSet;
import com.github.tommyettinger.ds.Utilities;
import com.github.tommyettinger.ds.support.sort.LongComparators;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Arrays;

import static com.github.tommyettinger.ds.test.PileupTest.LEN;
import static com.github.tommyettinger.ds.test.PileupTest.generatePointSpiral;

/**
 * Trying a shorter LEN value (10K) here so the tests finish quickly.
 * Using 1.12.4's Utilities class:
 * <pre>
 * This used a threshold of 28571 and LEN of 10000
 * 36 problem multipliers in total, 476 likely good multipliers in total.
 * Lowest collisions : 3373
 * Highest collisions: 76238
 * Average collisions: 9126.755859375
 * Lowest pileup     : 7
 * Highest pileup    : 79
 * Likely bad multipliers (base 16):
 * A7E675F3 A4613217 C0A33019 EE99B50F BBEB6F4F D6F7F7AB 80A58489 FCE635CB C2107ECF D689CA89 C0A7D057 825DFCA3 87D6B58B 92648319 844BAB95 9296AFF5 C465778F F4E86F33 9DF00FC5 BF8F0567 95B94A41 9E55CBF3 9E55CBF3 E09F978B B5CE57BB A4E2AF8D DFBF3899 9D9532F7 E73DB38D E72A5A97 CC0B2619 A85E7F21 9AC482DB B6815DDB 937CD501 85C8ADB5
 * </pre>
 * Even though only the first 32 multipliers are used in 1.12.4, one of the problem multipliers has issues when 28
 * rotations through GOOD_MULTIPLIERS have occurred, so it might have issues in practice on very large inputs...
 * <br>
 * Using Utilities2.THREE_PERCENT_MULTIPLIERS, the first 512 as-is:
 * <pre>
 * This used a threshold of 28571 and LEN of 10000
 * 23 problem multipliers in total, 489 likely good multipliers in total.
 * Lowest collisions : 3203
 * Highest collisions: 87588
 * Average collisions: 9508.37109375
 * Lowest pileup     : 7
 * Highest pileup    : 60
 * Likely bad multipliers (base 16):
 * DAC8E8C7 C262775B 17B32B61 69216F17 4CB73891 B7F01169 250BA8FB 437B98C5 6A35C0DD 115583CF 4AF8364D E1A368E3 F0074D35 D39B0435 18354E97 8B5018EF 614C5CFD 752D9E21 9B8C9E0B 7F28885B 56F88E89 661E99C5 39D8AC85
 * </pre>
 * Index 0, which wasn't using the first element in THREE_PERCENT_MULTIPLIERS, has significant problems, and they make
 * it into a problem multiplier when the threshold is just a little tighter.
 * <br>
 * Great... the likely bad multipliers are probably pointing in the wrong place...
 * <pre>
 * This used a threshold of 28571 and LEN of 10000
 * 22 problem multipliers in total, 490 likely good multipliers in total.
 * Lowest collisions : 3203
 * Highest collisions: 87588
 * Average collisions: 9332.3671875
 * Lowest pileup     : 7
 * Highest pileup    : 60
 * Likely bad multipliers (base 16):
 * 54138AFD 70A4F257 9D0F4E11 69F30297 36E8CCB1 4CFC12D1 770B9169 173291DF 64BF161D 1737AC19 712E0957 BD011A75 F3B16263 29DC70E9 50D33F41 A471E313 C93631C9 1F8972B3 2509EDA3 E586C9D1 59E6478B E10933CB
 * </pre>
 * <br>
 * Changed threshold so it checks the current pileup and can show problems earlier... still odd results with AMENDED...
 * <pre>
 * This used a threshold of 28571 and LEN of 10000
 * 86 problem multipliers in total, 426 likely good multipliers in total.
 * Lowest collisions : 20
 * Highest collisions: 82470
 * Average collisions: 7636.15625
 * Lowest pileup     : 0
 * Highest pileup    : 23
 * Likely bad multipliers (base 16):
 * E10933CB 300B59D7 300B59D7 CDA1D21D 1A73ED11 15C2D39F 66C1D617 6A878DA1 15C17F29 C2A74F31 741A7723 69F30297 AB9F1231 61E2DAB3 11BC992F BC1938ED 7C706855 9EA79A41 1B7E3415 843EAE93 4B8E319B 5E59E991 5C1B45B3 A4BB3907 A4BB3907 13ECAA63 13ECAA63 C2E438BB C612375D A10F9E5D C1CC9749 0936779D 2682D739 F4266789 E48B93D9 173291DF C8A0BE5B CC8F906D 24FB06BB 3ED416B3 1737AC19 47B092EB 490D724F D8F50D87 645A39A7 289FF151 289FF151 1AC26D9F 50A985FB C2D8E0EB 70F295D1 B1C6C2CB 315719F1 E66874CB 50D33F41 9365B107 6EE5C0A9 2A077597 3CB0D173 3CB0D173 D165C619 1C829FA9 A49BFA61 9DA5CC87 3C83BD2B 872C5C8D 3298728F C93631C9 872C5C8D C93631C9 1F8972B3 2F6C35C5 2F6C35C5 68C8BB13 290FC5B7 30E72577 D486E87D A2E2229F F9EA02B5 80BA7EE9 80BA7EE9 5D7A03B1 83EB2913 87F2D139 61BDE4C5 59644FCD
 * </pre>
 * <br>
 * The others, first Utilities from 1.12.4:
 * <pre>
 * This used a threshold of 28571 and LEN of 10000
 * 90 problem multipliers in total, 422 likely good multipliers in total.
 * Lowest collisions : 27
 * Highest collisions: 60031
 * Average collisions: 7177.833984375
 * Lowest pileup     : 0
 * Highest pileup    : 29
 * Likely bad multipliers (base 16):
 * AC11C8A3 F01DF9A5 F01DF9A5 F16A6DCD BCA43B89 E208D04F E1D8CCA3 98EFE1B5 BBEB6F4F D6F7F7AB 80A58489 8492ADC3 BF03ABE5 EA326ABB FE852FB3 CBC5273F B784AF47 E0F6C62B 8C4C960F FCC13CA9 ECE5E5D5 C1ADCC9D A1FE1C2F 9D3430ED E81D17CD C0A7D057 FA0C22AB 9AC1679B C076504F E6F4AAB7 AD1185C7 9F620FFD 825DFCA3 D568B12D D5AC8F79 DE0C7DF7 87D6B58B C8FF67C7 CFB5FEA7 A5EBC713 CC356C4D E2061CD3 C4431803 CF26F6F7 E3C1B8CF B943F971 B943F971 C465778F F4E86F33 BF8F0567 F0A31CA7 E6B6789F EA393429 A4CB2F61 F0CE6ED1 9E55CBF3 B302C987 D0ED94DB C30969E3 CB0959AB DA322367 83657DE7 9A3AB6F9 9A3AB6F9 E09F978B E39ECEF7 EDD14F51 F701704F 9BE73B1B A4E2AF8D C6E3D5B7 86C337AD 8CE7A2FB 9D9532F7 9D9532F7 EF226C59 CC448CC9 95C66415 CFCBE891 A94D8587 C14D4C3D CC8EAA37 F2406667 A8FFC28D FAFF47A5 93A04925 AB058D3B B87E35EB A03786ED 9B89CD59
 * </pre>
 * <br>
 * Then THREE_PERCENT_MULTIPLIERS, not amended...
 * <pre>
 * This used a threshold of 28571 and LEN of 10000
 * 86 problem multipliers in total, 426 likely good multipliers in total.
 * Lowest collisions : 20
 * Highest collisions: 82470
 * Average collisions: 7813.07421875
 * Lowest pileup     : 0
 * Highest pileup    : 23
 * Likely bad multipliers (base 16):
 * 59644FCD DAC8E8C7 8574B837 300B59D7 CDA1D21D 1A73ED11 15C2D39F 66C1D617 6A878DA1 15C17F29 C2A74F31 741A7723 4CB73891 E213ACCB 7222B9DB 7222B9DB 11BC992F BC1938ED 0E3D936D 9EA79A41 1B7E3415 843EAE93 4B8E319B 5E59E991 5C1B45B3 A4BB3907 A4BB3907 13ECAA63 13ECAA63 C2E438BB C612375D A10F9E5D 6F1DD941 8FB22447 2682D739 F4266789 E48B93D9 6A35C0DD C8A0BE5B CC8F906D 24FB06BB C884DFA5 4AF8364D 712E0957 47B092EB 0599FA53 BD011A75 D8F50D87 645A39A7 289FF151 289FF151 1AC26D9F 50A985FB C2D8E0EB 70F295D1 B1C6C2CB 315719F1 E66874CB 752D9E21 425E437B 6EE5C0A9 2A077597 3CB0D173 3CB0D173 D165C619 1C829FA9 A49BFA61 9DA5CC87 3C83BD2B 872C5C8D 3298728F 7F28885B 872C5C8D 7F28885B 56F88E89 8DD33671 2F6C35C5 2F6C35C5 68C8BB13 290FC5B7 30E72577 D486E87D A2E2229F 9A1E2EE1 9923E931 61BDE4C5
 * </pre>
 * There's some overlap with known problems from earlier, at least... hm...
 */
public class BroadSpectrumMultiplierTest {

	public static void main(String[] args) throws IOException {
		Utilities2.replaceGoodMultipliers(Utilities2.THREE_PERCENT_MULTIPLIERS);
		final Point2[] spiral = generatePointSpiral(LEN);
		IntSet collisions = new IntSet(LEN);
		for (int i = 0; i < LEN; i++) {
			collisions.add(spiral[i].hashCode());
		}
		System.out.println(collisions.size() + "/" + LEN + " hashes are unique.");
//		final long THRESHOLD = (long)(Math.pow(LEN, 11.0/10.0));
		final long THRESHOLD = (long) ((double) LEN * (double) LEN / (0.35 * collisions.size()));

		final int[] problems = {0};
		IntList likelyBad = new IntList(64);
		final int COUNT = 512;
		IntLongOrderedMap good = new IntLongOrderedMap(COUNT);
		int[] buffer = new int[Utilities.GOOD_MULTIPLIERS.length];
		long[] minMax = new long[]{Long.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE, Long.MIN_VALUE};
		for (int a = 0; a < COUNT; a++) {
			final int finalA = a;

			ObjectSet set = new ObjectSet(51, 0.7f) {
				long collisionTotal = 0;
				int longestPileup = 0;

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
					hashMultiplier = Utilities.GOOD_MULTIPLIERS[64 - shift & 511];
					Object[] oldKeyTable = keyTable;

					keyTable = new Object[newSize];

					longestPileup = 0;

					if (size > 0) {
						for (int i = 0; i < oldCapacity; i++) {
							Object key = oldKeyTable[i];
							if (key != null) {
								addResize(key);
							}
						}
					}
					if (longestPileup > 64 - shift) {
						System.out.printf("  WHOOPS!!!  Multiplier 0x%016X on index %4d has %d collisions and %d pileup\n", hashMultiplier, finalA, collisionTotal, longestPileup);
//							good.remove(g);
						likelyBad.add(hashMultiplier);
						problems[0]++;
						throw new RuntimeException();
					}
				}

				@Override
				public void clear() {
					System.out.print(Base.BASE10.unsigned(finalA) + "/" + Base.BASE10.unsigned(COUNT) + ": latest 0x" + Base.BASE16.unsigned(hashMultiplier));
					System.out.println(" gets total collisions: " + collisionTotal + ", PILEUP: " + longestPileup);
					minMax[0] = Math.min(minMax[0], collisionTotal);
					minMax[1] = Math.max(minMax[1], collisionTotal);
					minMax[2] = Math.min(minMax[2], longestPileup);
					minMax[3] = Math.max(minMax[3], longestPileup);
					good.put(finalA, collisionTotal);
					super.clear();
				}
			};
			try {
				for (int i = 0, n = spiral.length; i < n; i++) {
					set.add(spiral[i]);
				}
			} catch (RuntimeException ignored) {
				System.out.println(finalA + " FAILURE");
			}
			set.clear();
			// rotate multipliers by 1
			System.arraycopy(Utilities.GOOD_MULTIPLIERS, 1, buffer, 0, Utilities.GOOD_MULTIPLIERS.length - 1);
			System.arraycopy(Utilities.GOOD_MULTIPLIERS, 0, buffer, Utilities.GOOD_MULTIPLIERS.length - 1, 1);
			System.arraycopy(buffer, 0, Utilities.GOOD_MULTIPLIERS, 0, buffer.length);

		}
		System.out.println("This used a threshold of " + THRESHOLD + " and LEN of " + LEN);
		good.sortByValue(LongComparators.NATURAL_COMPARATOR);

		long bigTotal = 0L;
		for (int i = 0, n = Math.min(512, good.size()); i < n; i++) {
			long collCount = good.getAt(i);
			bigTotal += collCount;
		}
		System.out.println(problems[0] + " problem multipliers in total, " + (COUNT - problems[0]) + " likely good multipliers in total.");
		System.out.println("Lowest collisions : " + minMax[0]);
		System.out.println("Highest collisions: " + minMax[1]);
		System.out.println("Average collisions: " + (bigTotal / Math.min(512.0, good.size())));
		System.out.println("Lowest pileup     : " + minMax[2]);
		System.out.println("Highest pileup    : " + minMax[3]);
		System.out.println("Likely bad multipliers (base 16):");
		System.out.println(likelyBad.toString(" ", false, Base.BASE16::appendUnsigned));
	}

}

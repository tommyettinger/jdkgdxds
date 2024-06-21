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
 * (This doesn't mark any problem multipliers.)
 * 0 problem multipliers in total, 65535 likely good multipliers in total.
 * Lowest collisions : 65119
 * Highest collisions: 339968825
 * Lowest pileup     : 9
 * Highest pileup    : 34
 */
public class SmallCombinedHashTest {
	public static final int[] GOOD_MULTIPLIERS = {
			0x001006D1, 0x00104E61, 0x00105531, 0x00105981, 0x00108651, 0x0010A3E1, 0x00111431, 0x001138B1,
			0x00119E61, 0x0011A3A1, 0x0011AE01, 0x00122151, 0x00127EC1, 0x00127F41, 0x0012ADA1, 0x0012B1E1,
			0x0012BBD1, 0x0012EB71, 0x0012FB91, 0x001309E1, 0x00131911, 0x0013C481, 0x00140F71, 0x00142021,
			0x00149261, 0x0014E021, 0x0015BE41, 0x0015F5E1, 0x00161871, 0x00161A41, 0x00167B41, 0x001686F1,
			0x0016B0B1, 0x0016E381, 0x0016E9C1, 0x0016EC81, 0x0017BC01, 0x0017F0A1, 0x00184F31, 0x00185BB1,
			0x00187FD1, 0x0018F781, 0x001928D1, 0x001938E1, 0x0019F771, 0x001B0E11, 0x001B3551, 0x001B8031,
			0x001BB9A1, 0x001BBDC1, 0x001C0101, 0x001C52C1, 0x001C7531, 0x001CC121, 0x001D27B1, 0x001DD5C1,
			0x001E3191, 0x001E4E91, 0x001ECCD1, 0x001EF321, 0x001FDD61, 0x00100141, 0x00100621, 0x001006E1,
			0x001007D1, 0x00100851, 0x001009F1, 0x00100A21, 0x00100F11, 0x00101061, 0x00101461, 0x00101741,
			0x00101A51, 0x00101CA1, 0x00101CF1, 0x00101F21, 0x00102261, 0x00102341, 0x00102601, 0x00102731,
			0x001027D1, 0x00102981, 0x00102A61, 0x00102B01, 0x00103201, 0x00103211, 0x001033A1, 0x00103531,
			0x00103A31, 0x00103B41, 0x00103E91, 0x00103FE1, 0x001041B1, 0x00104551, 0x001048A1, 0x001049F1,
			0x00104AD1, 0x00104C11, 0x00104EF1, 0x001053B1, 0x00105811, 0x00105921, 0x00105AA1, 0x00105DE1,
			0x00105F61, 0x00105FE1, 0x001061A1, 0x00106231, 0x00106401, 0x00106541, 0x00106861, 0x001069D1,
			0x00106E21, 0x001070F1, 0x001073D1, 0x001074C1, 0x00108141, 0x001083B1, 0x00108491, 0x00108661,
			0x00108961, 0x00108BC1, 0x00108CA1, 0x00109251, 0x00109291, 0x00109621, 0x00109881, 0x00109D91,
			0x00109E31, 0x0010A2A1, 0x0010A311, 0x0010ABB1, 0x0010B1F1, 0x0010B311, 0x0010B631, 0x0010B891,
			0x0010BAD1, 0x0010BBD1, 0x0010BEA1, 0x0010BF41, 0x0010C151, 0x0010C511, 0x0010C651, 0x0010C881,
			0x0010C891, 0x0010C941, 0x0010CC01, 0x0010CC51, 0x0010CCB1, 0x0010CEA1, 0x0010D081, 0x0010D591,
			0x0010DAE1, 0x0010DB41, 0x0010DC51, 0x0010DD11, 0x0010DD31, 0x0010DE51, 0x0010DE81, 0x0010DEE1,
			0x0010E3B1, 0x0010E3E1, 0x0010E751, 0x0010E761, 0x0010E771, 0x0010E841, 0x0010E8C1, 0x0010EAE1,
			0x0010ED81, 0x0010F1D1, 0x0010F271, 0x0010F3C1, 0x0010F6C1, 0x0010F741, 0x0010F7E1, 0x0010F931,
			0x0010FAA1, 0x0010FE51, 0x0010FEB1, 0x001100B1, 0x00110151, 0x00110261, 0x001103B1, 0x00110521,
			0x001105D1, 0x00110BC1, 0x00110C41, 0x00111721, 0x00111961, 0x00111AE1, 0x00111D71, 0x00111F71,
			0x00112431, 0x001124B1, 0x00112721, 0x001127D1, 0x001128A1, 0x00112A91, 0x00112BA1, 0x00113381,
			0x00113431, 0x00113561, 0x00113581, 0x00113601, 0x00113761, 0x001137C1, 0x00113A31, 0x00113A41,
			0x00113C01, 0x00113C41, 0x00113D91, 0x00113E51, 0x00114211, 0x00114451, 0x00114501, 0x00114531,
			0x001145C1, 0x00114691, 0x00114BB1, 0x00114D21, 0x00114EC1, 0x00115461, 0x001159D1, 0x00115A11,
			0x00115AD1, 0x00115E51, 0x00116051, 0x00116A91, 0x00116B21, 0x00116EB1, 0x001170E1, 0x00117311,
			0x00117901, 0x00117AF1, 0x00117BB1, 0x001180A1, 0x00118E91, 0x001190E1, 0x00119531, 0x00119C61,
			0x00119E11, 0x00119FC1, 0x0011A141, 0x0011A1A1, 0x0011A2E1, 0x0011A411, 0x0011A611, 0x0011AF51,
			0x0011B201, 0x0011B351, 0x0011B661, 0x0011B851, 0x0011BAD1, 0x0011BED1, 0x0011BF91, 0x0011C3C1,
			0x0011C501, 0x0011C851, 0x0011CA91, 0x0011CD61, 0x0011CE61, 0x0011CEF1, 0x0011D081, 0x0011D511,
			0x0011D5C1, 0x0011D881, 0x0011D8B1, 0x0011DA31, 0x0011DAF1, 0x0011DD51, 0x0011E1E1, 0x0011E361,
			0x0011E5E1, 0x0011ED51, 0x0011F901, 0x0011F9F1, 0x0011FBA1, 0x0011FD01, 0x00120201, 0x001206A1,
			0x00120931, 0x00120D91, 0x00121191, 0x00121221, 0x001213D1, 0x001213F1, 0x00121481, 0x00121501,
			0x001216D1, 0x00121771, 0x00121AD1, 0x001224D1, 0x00122691, 0x001227E1, 0x001228D1, 0x001228F1,
			0x00122AE1, 0x00122C51, 0x00122C91, 0x00122E11, 0x00123481, 0x00123671, 0x00123DB1, 0x001240E1,
			0x001240F1, 0x00124101, 0x001241C1, 0x00124671, 0x00124861, 0x00124951, 0x001258C1, 0x00125B71,
			0x00126021, 0x00126161, 0x00126261, 0x00126271, 0x00126671, 0x001266A1, 0x001266E1, 0x00126891,
			0x00126911, 0x00126951, 0x001269F1, 0x00126DC1, 0x00127171, 0x001273A1, 0x00127541, 0x00127741,
			0x001277F1, 0x00127831, 0x001279E1, 0x00127AF1, 0x00127B11, 0x00127B51, 0x00127EA1, 0x001280A1,
			0x00128451, 0x00128461, 0x00128771, 0x00128881, 0x00128951, 0x00128B11, 0x00128CB1, 0x001293D1,
			0x00129E71, 0x00129FD1, 0x0012A141, 0x0012A4F1, 0x0012A561, 0x0012A701, 0x0012ABA1, 0x0012ABE1,
			0x0012ACC1, 0x0012AE41, 0x0012AE91, 0x0012AF21, 0x0012B091, 0x0012B541, 0x0012B681, 0x0012B831,
			0x0012B9A1, 0x0012BD61, 0x0012C241, 0x0012C3F1, 0x0012C651, 0x0012C671, 0x0012C681, 0x0012C6F1,
			0x0012C821, 0x0012C921, 0x0012C981, 0x0012C9A1, 0x0012CA11, 0x0012CC01, 0x0012CD71, 0x0012D101,
			0x0012D121, 0x0012D241, 0x0012D461, 0x0012D5C1, 0x0012D6D1, 0x0012D821, 0x0012D9F1, 0x0012DD01,
			0x0012E9B1, 0x0012EC61, 0x0012EFF1, 0x0012F1D1, 0x0012F2B1, 0x0012F3F1, 0x0012F591, 0x0012F5E1,
			0x0012FA41, 0x0012FEF1, 0x0012FF41, 0x0012FFA1, 0x0012FFD1, 0x001301C1, 0x00130211, 0x00130541,
			0x00130651, 0x001308F1, 0x00130A91, 0x00130B91, 0x00130C51, 0x00130C91, 0x00130D11, 0x00130F11,
			0x00130F91, 0x001310D1, 0x001310E1, 0x00131231, 0x001317E1, 0x00131A21, 0x00131A51, 0x00131B41,
			0x001322C1, 0x00132551, 0x00132571, 0x00132661, 0x00132CB1, 0x00132D21, 0x00132D41, 0x00132E01,
			0x00133031, 0x001333D1, 0x00133971, 0x00133A51, 0x00133C11, 0x00133E11, 0x00134431, 0x00134461,
			0x001346E1, 0x00134761, 0x00134EF1, 0x00134F51, 0x00135121, 0x00135171, 0x00135311, 0x001356E1,
			0x00135E61, 0x00135F51, 0x00136621, 0x00136651, 0x00136D71, 0x001370D1, 0x00137191, 0x001372C1,
			0x001378B1, 0x00137CB1, 0x00137E41, 0x00137EC1, 0x00137F91, 0x00138011, 0x001382B1, 0x00138491,
			0x001385E1, 0x00138771, 0x00138A71, 0x00138AA1, 0x00138B01, 0x00138DE1, 0x00138F01, 0x00138F81,
			0x001393B1, 0x00139521, 0x00139A51, 0x00139AD1, 0x00139C01, 0x00139C51, 0x0013A0A1, 0x0013A2D1,
			0x0013A2F1, 0x0013A6C1, 0x0013A761, 0x0013AD21, 0x0013AE01, 0x0013AE41, 0x0013AEA1, 0x0013B011,
			0x0013B191, 0x0013B311, 0x0013B501, 0x0013B861, 0x0013BA01, 0x0013BAD1, 0x0013C181, 0x0013C7D1,
			0x0013CD11, 0x0013CFA1, 0x0013D501, 0x0013D541, 0x0013D631, 0x0013D781, 0x0013DEA1, 0x0013E431,
			0x0013E631, 0x0013E8B1, 0x0013E981, 0x0013E9A1, 0x0013EC71, 0x0013EE61, 0x0013F081, 0x0013F451,
			0x0013F4A1, 0x0013F501, 0x0013F631, 0x0013F691, 0x0013F831, 0x0013FB81, 0x0013FFC1, 0x00140281,
	};
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

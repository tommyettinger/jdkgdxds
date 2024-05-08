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
import com.github.tommyettinger.ds.IntLongOrderedMap;
import com.github.tommyettinger.ds.IntOrderedSet;
import com.github.tommyettinger.ds.ObjectSet;
import com.github.tommyettinger.ds.Utilities;
import com.github.tommyettinger.ds.support.sort.LongComparators;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.IOException;

import static com.github.tommyettinger.ds.test.PileupTest.LEN;
import static com.github.tommyettinger.ds.test.PileupTest.generateVectorSpiral;

public class AllGoldenIntVectorHashTest {

	public static void main(String[] args) throws IOException {
		final int[] GOOD = new int[]{
			0x9E3779B9, 0x91E10DA5, 0xD1B54A33, 0xABC98389, 0x8CB92BA7, 0xDB4F0B91, 0xBBE05633, 0x89E18285,
			0xC6D1D6C9, 0xAF36D01F, 0x9A69443F, 0x881403B9, 0xCEBD76D9, 0xB9C9AA3B, 0xA6F5777F, 0x86D516E5,
			0xE95E1DD1, 0xD4BC74E1, 0xC1EDBC5B, 0xB0C8AC51, 0xA127A31D, 0x92E852C9, 0x85EB75C3, 0xEBEDEED9,
			0xC862B36D, 0xB8ACD90D, 0xAA324F91, 0x9CDA5E69, 0x908E3D2D, 0x8538ECB5, 0xBF25C1FB, 0xB1AF5C05,
			0x9989A7D9, 0x8EB95D05, 0xE0504E7B, 0xD1F91E9D, 0xB7FBD901, 0xAC38B669, 0xA13614FB, 0x96E7A621,
			0x8D41E4AD, 0x843A0803, 0xF1042721, 0xC91FE60D, 0xBD5A4AD1, 0xB24512C7, 0xA7D5EB01, 0x94C37CD5,
			0x8C0E724F, 0x83DBDF3F, 0xF22EECF7, 0xE51CC09B, 0xD8BF2D51, 0xCD0C73D1, 0xC1FB5B85, 0xAD9BA24D,
			0xA43CF217, 0x9B5FB7D3, 0x92FCF6CB, 0x8B0E12CF, 0x838CCB05, 0xDB70396F, 0xBC193375, 0xA9BBB6A1,
			0x8A35060F, 0xF40BA295, 0xE8A62E75, 0xDDC8F72B, 0xD36DA013, 0xC0249885, 0xB72B9CF7, 0x9747627B,
			0x9036EA01, 0x897AE4FD, 0xF4CCD627, 0xEA171C21, 0xD60E4185, 0xCCB0DCDF, 0xC3BC5AB1, 0xBB2C2447,
			0xAB273EB1, 0xA3AA5A35, 0x88D9849B, 0xF57716BD, 0xEB5D28EB, 0xE1ADA55D, 0xC6F2B277, 0xB6E92FC9,
			0xAF62415F, 0xA13F04BD, 0x943F8703, 0x8E25C2E9, 0x884C43B5, 0x82B0645B, 0xF60E4093, 0xEC7F64E5,
			0xDA7B216D, 0xD1FE7BF7, 0xC9D63C03, 0xC1FF1A4B, 0xB337B641, 0xAC418363, 0xA5908B4B, 0x9F221D85,
			0x98F3A45F, 0x9302A3DB, 0x8D4CB8AF, 0xF6955E05, 0xDC5C91B7, 0xD44186D3, 0xBDAF3BEB, 0xB6B5103B,
			0xA98368C9, 0x9D45AB03, 0x977CB5A3, 0x8C8C366B, 0x8760BD83, 0x8265F323, 0xD64E71E3, 0xCED220B1,
			0xC09FF697, 0xB9E58585, 0xB3673E6F, 0xAD23075B, 0xA716D915, 0xA140BE91, 0x9B9ED43F, 0x962F4775,
			0x8BE04CC3, 0x86FD88C3, 0x8246750D, 0xF77CB1D7, 0xEF41DBE9, 0xDF9C098D, 0xD0FC339F, 0xCA092125,
			0xC35137BF, 0xBCD27FCF, 0xB68B1273, 0xB07918F9, 0xAA9ACC57, 0x9A250D21, 0x90103F13, 0x8B45D8CF,
			0x86A43AC3, 0x822A09C7, 0xF002ED13, 0xE8654625, 0xE1057C8D, 0xD2F7B733, 0xCC45FDB1, 0xC5CAA50D,
			0xBF83F38F, 0xB38DE4B9, 0xADDB5841, 0xA85713D1, 0x8ABA52BD, 0x86536A15, 0xF83B8239, 0xF0B35A25,
			0xDB72DC35, 0xD4CA486F, 0xCE556D09, 0xC812B845, 0xC200A49B, 0xBC1DB84D, 0xB6688515, 0xB0DFA7CD,
			0xAB81C813, 0xA64D97FB, 0xA141D3B5, 0x9C5D414B, 0x979EB049, 0x8E8EFE9F, 0x8A3BAA23, 0x8609EEE3,
			0x81F8C7E5, 0xF88EE8ED, 0xF1553311, 0xEA51424F, 0xDCE47B27, 0xD03C9BDD, 0xCA2EF613, 0xBE997F95,
			0xB90F1A29, 0xAE74CE8F, 0xA9628B9F, 0xA476060B, 0x9FAE24F5, 0x9B09D7AF, 0x96881577, 0x9227DD3D,
			0x8DE8356B, 0x81E34C0F, 0xF8DB9899, 0xF1EA3409, 0xE49ACC3B, 0xDE3A0E8B, 0xD806DE3B, 0xD1FFF5EF,
			0xC0E8BE65, 0xBB86F28B, 0xB64B9731, 0xAC43EF7B, 0x9E3EE499, 0x99D4AB95, 0x915DF131, 0x8D4FB3AD,
			0xF92258D9, 0xF273D571, 0xEBF33221, 0xE59F33F1, 0xDF76A85B, 0xD9786511, 0xD3A347C5, 0xCDF635F1,
			0xC30FF04F, 0xB8BD53F7, 0xB3C8EFED, 0xAEF69073, 0xAA454BF7, 0xA5B43F2F, 0xA1428CE7, 0x9CEF5DDB,
			0x8CC3B777, 0x8550B4B7, 0x81BD62C3, 0xECAD6DC1, 0xE690FA59, 0xE09CEC0D, 0xDAD037D9, 0xCA4C3001,
			0xC512FD47, 0xBFFC5183, 0xBB074877, 0xB63303D1, 0xB17EAAF7, 0xA419044D, 0x9BBBA271, 0x97B63B39,
			0x93CB6869, 0x8FFA7A51, 0x8C42C5C9, 0x88A3A413, 0x851C72C1, 0x81AC9397, 0xE7720D2D, 0xE1AF214F,
			0xDC10EC99, 0xD13F0697, 0xCC0992A9, 0xC6F5505B, 0xC2016C27, 0xB8778A19, 0xB3DFFEFD, 0xAF65B735,
			0xAB07F845, 0xA6C60C57, 0xA29F4217, 0x9E92ECA3, 0x96C701E9, 0x930627ED, 0x8F5D391D, 0xF9D92999,
			0xF3D82B23, 0xE844186B, 0xE2AF4337, 0xDD3CC3C5, 0xD2BB7E5F, 0xCDAB2117, 0xC8B9EAA9, 0xBF31F86F,
			0xBA99CB0F, 0xB61DE131, 0xAD7824A7, 0xA94D02AD, 0xA53B8563, 0xA1430F0B, 0x9D6305AD, 0x999AD307,
			0x95E9E475, 0x924FAAD3, 0x8ECB9A75, 0x8B5D2B07, 0x8803D777, 0x84BF1DEF, 0x818E7FAF, 0xFA0DEED9,
			0xF43F3741, 0xEE93070D, 0xE90890F1, 0xE39F0C67, 0xDE55B58F, 0xD92BCD17, 0xCF336023, 0xCA6372CF,
			0xC5B021FF, 0xAFC6001D, 0xA00117CF, 0x9C49C697, 0x98A88D81, 0x951CE92D, 0x8AF6847D, 0x87BC4D8D,
			0x8180FE8F, 0xFA3F486F, 0xF49FA91F, 0xE9C0BDF5, 0xE4800215, 0xDF5D7E31, 0xDA588471, 0xD0A48B75,
			0xC75EF4F3, 0xBA3AE02B, 0xB60B8819, 0xB1F44341, 0xADF48725, 0xAA0BCC5F, 0xA6398E99, 0xA27D4C75,
			0x9ED6877B, 0x9B44C40B, 0x97C7894D, 0x945E6123, 0x9108D811, 0x8DC67D37, 0x8A96E23D, 0x87799B49,
			0xFA6D85BD, 0xF4FA1811, 0xEFA509F9, 0xEA6DB233, 0xE5536B35, 0xDB738B33, 0xD6ACB8C9, 0xCD6E58F9,
			0xC8F5A633, 0xC495DDE1, 0xC04E7531, 0xBC1EE451, 0xB806A665, 0xB01A1E63, 0xAC44D8CD, 0xA884EF07,
			0xA4D9EA11, 0x9A51B865, 0x9075C7D5, 0x873B1719, 0x84498F5D, 0x81686E6F, 0xFA98EFEF, 0xF54F0FFD,
			0xEB106D05, 0xE61A7887, 0xDC7E65E5, 0xD7D72935, 0xD349104B, 0xCED39351, 0xCA762D55, 0xC201A07F,
			0xB9E77939, 0xB5FB1C0D, 0xB223F10D, 0xA01EB619, 0x9CBDA623, 0x996ED7BF, 0x9631E84F, 0x8FEC261B,
			0x8CE29851, 0x87005E7F, 0x815D0BFB, 0xFAC1E365, 0xF59F42F9, 0xF0978EA7, 0xEBAA3947, 0xE6D6B899,
			0xDD7B1A6B, 0xD8F1F657, 0xD48099B1, 0xD02687CD, 0xCBE3468F, 0xBF9DC6D1, 0xBBB1344F, 0xB7D93465,
			0xB4155B3B, 0xB0653F27, 0xACC878AF, 0xA93EA279, 0xA5C75937, 0xA2623BAB, 0x9F0EEA91, 0x9BCD089B,
			0x989C3A5F, 0x957C2657, 0x926C74D1, 0x8C7CE379, 0x84084133, 0x81540F2D, 0xF5EA757B, 0xF1061383,
			0xEC3A9B97, 0xE7878ED5, 0xE2EC70E1, 0xDE68C7D9, 0xD9FC1C49, 0xD5A5F919, 0xD165EB8B, 0xCD3B8325,
			0xC92651AD, 0xC139E57F, 0xBD61D91F, 0xB5EC1723, 0xB24D9C1F, 0xAEC18F6D, 0xAB47932D, 0xA7DF4B61,
			0xA4885DE1, 0xA1427249, 0x9E0D31FF, 0x9AE8481D, 0x97D3616F, 0x94CE2C6B, 0x91D85923, 0x8C199FFD,
			0x89502219, 0x8694D5D3, 0x8147B271, 0xFB0C904D, 0xECC37005, 0xE3B1B659, 0xDF4A6A77, 0xD6BCCE3D,
			0xC699FDE9, 0xBEFE7C61, 0xB7AD9447, 0xB420340D, 0xB0A46A15, 0xAD39DF49, 0xA9E03E49, 0xA6973357,
			0xA35E6C53, 0x9A129165, 0x9717C447, 0x942BB7B9, 0x914E22B5, 0x8E7EBD99, 0x83C99FAF, 0x813D274D,
			0xF1D08AF9, 0xED43566B, 0xE8CC107F, 0xDBE5BFB7, 0xD7C22743, 0xD3B28091, 0xC433F035, 0xC0828467,
			0xBCE2E433, 0xB954B9D7, 0xB5D7B133, 0xB26B77B9, 0xAF0FBC67, 0xABC42FC9, 0xA55C6C2F, 0x9F31CE91,
			0x99420F2B, 0x965F9259, 0x88BE7A7B, 0x862B90A5, 0x83A50DD7, 0xFB50D045, 0xF6B791E9, 0xE96B8497,
			0xE5261A7F, 0xE0F4B24F, 0xDCD6EE4F, 0xD8CC727B, 0xD0EFEBB9, 0xCD1D3113, 0xC95C5F1B, 0xC5AD21EF,
			0xC20F2731, 0xBE821E09, 0xBB05B719, 0xB0F14B83, 0xADB47065, 0xAA86BFE7, 0xA457C3ED, 0xA155EE39,
			0x9E622EAD, 0x9B7C434D, 0x98A3EB51, 0x95D8E71F, 0x9069E18D, 0x8DC566B9, 0x8B2D4CBD, 0x83AD0557,
			0x8144356D, 0xFB6D5599, 0xF6EF948D, 0xEE3151B7, 0xE5C24EC9, 0xDD9FBD6F, 0xD9AA45DB, 0xD5C6E8C7,
			0xD1F55367, 0xCE35346F, 0xCA863C03, 0xC6E81BB7, 0xBFDD30C1, 0xBC6FD021, 0xB9121BAF, 0xB5C3CBBD,
		};

		int[] GOLDEN_INTS = GOOD;
//		int[] GOLDEN_INTS = new int[MathTools.GOLDEN_LONGS.length];
//		for (int i = 0; i < GOLDEN_INTS.length; i++) {
//			GOLDEN_INTS[i] = (int)(MathTools.GOLDEN_LONGS[i] >>> 32) | 1;
//		}
		final Vector2[] spiral = generateVectorSpiral(LEN);
		final long THRESHOLD = (long)(Math.pow(LEN, 11.0/10.0));// (long)(Math.pow(LEN, 7.0/6.0));
		IntLongOrderedMap problems = new IntLongOrderedMap(100);
		IntOrderedSet good = IntOrderedSet.with(GOLDEN_INTS);
		for (int a = -1; a < GOLDEN_INTS.length; a++) {
			final int g = a == -1 ? 1 : GOLDEN_INTS[a];
			{
				int finalA = a;
				ObjectSet set = new ObjectSet(51, 0.6f) {
					long collisionTotal = 0;
					int longestPileup = 0;
					int hm = 0x9E3779B7;

					@Override
					protected int place (Object item) {
						return BitConversion.imul(item.hashCode(), hm) >>> shift;
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
						shift = BitConversion.countLeadingZeros(mask);

//						// we modify the hash multiplier by multiplying it by a number that Vigna and Steele considered optimal
//						// for a 64-bit MCG random number generator, XORed with 2 times size to randomize the low bits more.
//						hashMultiplier *= size + size ^ 0xF1357AEA2E62A9C5L;
//						hashMultiplier += 0x6A09E667F3BCC90AL ^ size + size; // fractional part of the silver ratio, times 2 to the 64
//						hashMultiplier ^= size + size; // 86 problems, worst collisions 68609571
//						hashMultiplier ^= hashMultiplier * hashMultiplier * 0x6A09E667F3BCC90AL;
//						hashMultiplier = ~((hashMultiplier ^ -(hashMultiplier * hashMultiplier | 5L)) << 1);
//						hashMultiplier *= 0xD413CCCFE7799215L + size + size; // 113 problems, worst collisions 109417377
//						hashMultiplier += 0xD413CCCFE7799216L + size + size; // 118 problems, worst collisions 296284292
//						hashMultiplier += 0xD413CCCFE7799216L * size; // 137 problems, worst collisions 290750405
//						hashMultiplier ^= 0xD413CCCFEL * size; // 105 problems, worst collisions 87972280
//						hashMultiplier = MathTools.GOLDEN_LONGS[(int)(hashMultiplier >>> 40) % MathTools.GOLDEN_LONGS.length]; // 99 problems, worst collisions 68917443
//						hashMultiplier = MathTools.GOLDEN_LONGS[(int)(hashMultiplier & 0xFF)]; // 39 problems, worst collisions 9800516
//						hashMultiplier = MathTools.GOLDEN_LONGS[(int)(hashMultiplier & 0x7F)]; // 0 problems, worst collisions nope
//						hashMultiplier = MathTools.GOLDEN_LONGS[(int)(hashMultiplier >>> 29 & 0xF8) | 7]; // 163 problems, worst collisions 2177454
//						hashMultiplier = Utilities.GOOD_MULTIPLIERS[(int)(hashMultiplier >>> 27) + shift & 0x1FF]; // 0 problems, worst collisions nope

						// this next one deserves some explanation...
						// shift is always between 33 and 63 or so, so adding 48 to it moves it to the 85 to 115 range.
						// but, shifts are always implicitly masked to use only their lowest 6 bits (when shifting longs).
						// this means the shift on hashMultiplier is between 17 and 47, which is a good random-ish range for these.
//						hashMultiplier = Utilities.GOOD_MULTIPLIERS[(int)(hashMultiplier >>> 48 + shift) & 511]; // 0 problems, worst collisions nope

						hashMultiplier = hm = GOOD[BitConversion.imul(shift, hm) >>> 23];
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
						if (collisionTotal > THRESHOLD) {
							System.out.printf("  WHOOPS!!!  Multiplier %016X on index %4d has %d collisions and %d pileup\n", hashMultiplier, finalA, collisionTotal, longestPileup);
							problems.put(g, collisionTotal);
							good.remove(g);
//							throw new RuntimeException();
						}
					}

					@Override
					public void clear () {
						System.out.print("Original 0x" + Base.BASE16.unsigned(g) + " on latest " + Base.BASE16.unsigned(hashMultiplier));
						System.out.println(" gets total collisions: " + collisionTotal + ", PILEUP: " + longestPileup);
						super.clear();
					}

					/**
					 * Sets the current hash multiplier, then immediately calls {@link #resize(int)} without changing the target size; this
					 * is for specific advanced usage only. Calling resize() will change the multiplier before it gets used, and the current
					 * {@link #size()} of the data structure also changes the value. The hash multiplier is used by {@link #place(Object)}.
					 * The hash multiplier must be an odd long, and should usually be "rather large." Here, that means the absolute value of
					 * the multiplier should be at least a quadrillion or so (a million billions, or roughly {@code 0x4000000000000L}). The
					 * only validation this does is to ensure the multiplier is odd; everything else is up to the caller. The hash multiplier
					 * changes whenever {@link #resize(int)} is called, though its value before the resize affects its value after. Because
					 * of how resize() randomizes the multiplier, even inputs such as {@code 1L} and {@code -1L} actually work well.
					 * <br>
					 * This is accessible at all mainly so serialization code that has a need to access the hash multiplier can do so, but
					 * also to provide an "emergency escape route" in case of hash flooding. Using one of the "known good" longs in
					 * {@link Utilities#GOOD_MULTIPLIERS} should usually be fine if you don't know what multiplier will work well.
					 * Be advised that because this has to call resize(), it isn't especially fast, and it slows
					 * down the more items are in the data structure. If you in a situation where you are worried about hash flooding, you
					 * also shouldn't permit adversaries to cause this method to be called frequently.
					 *
					 * @param hashMultiplier any odd long; will not be used as-is
					 */
					@Override
					public void setHashMultiplier (long hashMultiplier) {
						super.setHashMultiplier(hashMultiplier);
						hm = (int)hashMultiplier;
					}
				};
				if(a != -1)
					set.setHashMultiplier(g);
				try {
					for (int i = 0, n = spiral.length; i < n; i++) {
						set.add(spiral[i]);
					}
				}catch (RuntimeException ignored){
					System.out.println(g + " FAILURE");
					continue;
				}
				set.clear();
			}
		}
		System.out.println("This used a threshold of " + THRESHOLD);
		problems.sortByValue(LongComparators.NATURAL_COMPARATOR);
		System.out.println(problems.toStringUnsigned(", ", false, Base.BASE16));
		System.out.println("\n\nnew int[]{");
		for (int i = 0; i < Integer.highestOneBit(good.size()); i++) {
//		for (int i = 0; i < (good.size()); i++) {
			System.out.print("0x"+Base.BASE16.unsigned(good.getAt(i))+", ");
			if((i & 7) == 7)
				System.out.println();
		}
		System.out.println("};\n\n" + problems.size() + " problem multipliers in total, " + good.size() + " likely good multipliers in total.");
	}

}

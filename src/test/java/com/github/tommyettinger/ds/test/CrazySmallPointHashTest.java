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
import com.github.tommyettinger.ds.IntIntOrderedMap;
import com.github.tommyettinger.ds.IntSet;
import com.github.tommyettinger.ds.support.sort.IntComparators;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.IOException;

import static com.github.tommyettinger.ds.test.PileupTest.generatePointSpiral;

/**
 * 2428 problem multipliers in total, 5764 likely good multipliers in total.
 * Lowest collisions : 225307
 * Highest collisions: 273108
 * Lowest pileup     : 5
 * Highest pileup    : 29
 */
public class CrazySmallPointHashTest {
	public static final int LEN = 20000;

	public static void main(String[] args) throws IOException {
		// has 512 int multipliers, each using 21 bits or fewer.
		final int[] GOOD = new int[]{
			0x00197D75, 0x001F6AE3, 0x0006F5F3, 0x001C4C3D, 0x001AD501, 0x000D36F9, 0x0012D2B7, 0x001A0E77,
			0x000AB569, 0x00114C3D, 0x0016CC3D, 0x000348A3, 0x0002F571, 0x0009C255, 0x000DF571, 0x000DFD75,
			0x000B1E47, 0x001254FF, 0x000F52B7, 0x00150E77, 0x000C5205, 0x000B7D75, 0x0017D5EB, 0x0015EAE3,
			0x001A4987, 0x00012207, 0x0002E173, 0x001FA119, 0x001A7B2F, 0x00116CE9, 0x000214D9, 0x000B85C7,
			0x000CC255, 0x00102DC3, 0x00020427, 0x0001A649, 0x000D7B2F, 0x0010D2B7, 0x000694D9, 0x001865D5,
			0x00073B83, 0x0004D4FF, 0x0018EAE3, 0x0013F5F3, 0x0016E5D5, 0x000765D5, 0x0003B6F9, 0x000AD4FF,
			0x000B8E77, 0x0000D339, 0x0005ADC3, 0x00114C17, 0x001FE173, 0x0011ADC3, 0x000BC057, 0x001A52B7,
			0x00130427, 0x00165205, 0x001FBCA7, 0x000EFB2F, 0x000BD4FF, 0x000B55EB, 0x0001B569, 0x001CA649,
			0x00163713, 0x000DB569, 0x00018427, 0x00075501, 0x000C3B83, 0x000B4987, 0x001265D5, 0x000914D9,
			0x001854FF, 0x0007C8D7, 0x0014D5EB, 0x00087571, 0x0002AA37, 0x001714D9, 0x00016173, 0x0008D2B7,
			0x0012BB83, 0x001F7BB3, 0x0003A119, 0x0002C8D7, 0x0008C057, 0x0001E8E7, 0x00065205, 0x00024057,
			0x001C14D9, 0x0010C8A3, 0x0006F6F7, 0x001E0E77, 0x00182649, 0x00013713, 0x0001F5F3, 0x0016CC17,
			0x000BB569, 0x001ACC17, 0x0011FBB3, 0x0000F6F7, 0x0016FD75, 0x001BAA37, 0x0015B713, 0x000F2207,
			0x0012A649, 0x000F5501, 0x00050415, 0x0009D501, 0x0001CC17, 0x001AA649, 0x0002C8A3, 0x001D1E47,
			0x0010ECE9, 0x001548A3, 0x000D7D75, 0x000DADC3, 0x00014C3D, 0x0012C987, 0x0008C4BF, 0x00084057,
			0x000750DB, 0x0019D0DB, 0x001B7571, 0x001D2DC3, 0x00153569, 0x001365D5, 0x000C1E47, 0x0006B569,
			0x000DEA89, 0x0015D205, 0x000CD501, 0x0000C2E5, 0x00105339, 0x001D42E5, 0x001FC057, 0x000568E7,
			0x000F2649, 0x000B0415, 0x0018CC17, 0x000D48A3, 0x000052B7, 0x000DD501, 0x0009D0DB, 0x00060415,
			0x00043B83, 0x001848D7, 0x0007F6F7, 0x0014C987, 0x0016CD59, 0x0005BCA7, 0x00138427, 0x00024987,
			0x001A8427, 0x001C42E5, 0x000B0427, 0x00035339, 0x000A6173, 0x001BCC3D, 0x000D4EC9, 0x000E94D9,
			0x001A4C3D, 0x001D65D5, 0x0018E8E7, 0x001D5205, 0x000FEAE3, 0x00098427, 0x000FD5EB, 0x0000E173,
			0x0017E5D5, 0x000D2119, 0x0019A207, 0x001955EB, 0x001F2207, 0x00010427, 0x001DFD75, 0x00104D59,
			0x0012B713, 0x001FE8E7, 0x001EF6F7, 0x00104C17, 0x0019C057, 0x00168415, 0x001A48D7, 0x0011A649,
			0x001BA207, 0x000DAA37, 0x001355EB, 0x000DC057, 0x000952B7, 0x0015AA37, 0x001F9497, 0x001C52B7,
			0x001F85C7, 0x0003C8A3, 0x000CE5D5, 0x000FB713, 0x00037B2F, 0x0000EA89, 0x001BD0DB, 0x000CC057,
			0x000AF571, 0x00112DC3, 0x0014C4BF, 0x00062A37, 0x001DC8A3, 0x0001D205, 0x000B48A3, 0x00084987,
			0x001DE173, 0x001950DB, 0x0012F571, 0x00076AE3, 0x0005D5EB, 0x00093B83, 0x00064C3D, 0x0019B6F9,
			0x000FD2B7, 0x00135501, 0x001442E5, 0x000CD205, 0x000EA649, 0x000F8427, 0x000014D9, 0x001155EB,
			0x000B9497, 0x00094EC9, 0x001594D9, 0x00027D75, 0x001EE5D5, 0x00042119, 0x0001AA37, 0x000A4987,
			0x000F5205, 0x001C5501, 0x000C9497, 0x001BC2E5, 0x000C55EB, 0x000FD4FF, 0x000B6AE3, 0x0013B6F9,
			0x000848A3, 0x00022207, 0x00084C17, 0x0000CC3D, 0x001D52B7, 0x00114D59, 0x001ACEC9, 0x00047B2F,
			0x000A2119, 0x00051E47, 0x000C48D7, 0x0013FD75, 0x001055EB, 0x001CD205, 0x00127BB3, 0x0016B569,
			0x0012BCA7, 0x0015ADC3, 0x001A8415, 0x0008FBB3, 0x0009ECE9, 0x000A42E5, 0x001F3713, 0x00130E77,
			0x00005339, 0x0001ADC3, 0x00150427, 0x0019EA89, 0x0003A649, 0x001E1497, 0x000BCC17, 0x000CD0DB,
			0x000B4255, 0x0019FB2F, 0x001DD2B7, 0x000AD0DB, 0x000EAA37, 0x00146173, 0x00172119, 0x001DA207,
			0x00086173, 0x0003D501, 0x001B85C7, 0x000E85C7, 0x00044057, 0x0014E8E7, 0x000FFB2F, 0x000AC987,
			0x001A2119, 0x00021497, 0x00044255, 0x00078427, 0x000C52B7, 0x00074EC9, 0x00164987, 0x0004ECE9,
			0x001D2207, 0x0012CC3D, 0x00192207, 0x0006CC3D, 0x00103713, 0x00134C3D, 0x000E3569, 0x0014C057,
			0x0011E173, 0x00182A37, 0x0019C987, 0x00197B2F, 0x00195501, 0x001DC987, 0x000AF5F3, 0x0010FBB3,
			0x001905C7, 0x00077571, 0x000FF6F7, 0x000314D9, 0x0005E8E7, 0x00145501, 0x0017CEC9, 0x0017D205,
			0x000F85C7, 0x001F4EC9, 0x001876F7, 0x001DCEC9, 0x0007C987, 0x000942E5, 0x000BF571, 0x000442E5,
			0x000E50DB, 0x001344BF, 0x00108E77, 0x001AB713, 0x00034987, 0x00180427, 0x000585C7, 0x0002B6F9,
			0x0008F571, 0x00024EC9, 0x001FCDD1, 0x00014DD1, 0x001FC7FB, 0x001B136F, 0x0009C7FB, 0x00110B4F,
			0x0016D71B, 0x0009CDD1, 0x0010CDD1, 0x000F0B4F, 0x0012C7FB, 0x00188B4F, 0x000547FB, 0x001AC7FB,
			0x000647FB, 0x0018936F, 0x0008CDD1, 0x0014936F, 0x0019136F, 0x0007936F, 0x001DC7FB, 0x0014571B,
			0x000E936F, 0x0004936F, 0x001B0B4F, 0x001DD71B, 0x0011CDD1, 0x0015C7FB, 0x000C4DD1, 0x0004571B,
			0x0013571B, 0x000E4DD1, 0x00148B4F, 0x001D136F, 0x000C8B4F, 0x0001571B, 0x001E136F, 0x001C571B,
			0x000747FB, 0x000CC7FB, 0x0014548F, 0x001A2AA9, 0x00118A27, 0x00101A2D, 0x000514C9, 0x001EF7C7,
			0x00164ED1, 0x001A0EB9, 0x001E0CAF, 0x001FC43F, 0x000C00F5, 0x000F77C7, 0x000CD0CD, 0x000A14D1,
			0x0012EAF1, 0x0006CAB9, 0x000C98E1, 0x00011663, 0x0005CBF3, 0x001AB53D, 0x00137C2F, 0x001438A1,
			0x000F5DB3, 0x001CE795, 0x001A741B, 0x001AE9E7, 0x001040CF, 0x001D7E5D, 0x00081CB1, 0x000B6F35,
			0x0004A781, 0x000EBF25, 0x00195695, 0x001BCB61, 0x001DF667, 0x00098BD7, 0x0010AC31, 0x001EF46B,
			0x001DF7C7, 0x0000E53B, 0x001D0A5D, 0x0010F71F, 0x000F0475, 0x001F10DF, 0x00139CEB, 0x00102733,
			0x0017D14F, 0x0001409D, 0x0007D7F3, 0x0011357D, 0x001BF855, 0x000937AD, 0x001470FD, 0x000B3DE1,
			0x001C9C89, 0x0007A781, 0x0018E671, 0x00022A4F, 0x0008548F, 0x000D38A1, 0x000E00C9, 0x00028D31,
			0x001D66FF, 0x00039F61, 0x000BFDE7, 0x0010F17B, 0x00023DE1, 0x00164B83, 0x000EDD93, 0x00138D13,
			0x0012765B, 0x0019D823, 0x0007AB3F, 0x0019C7D7, 0x0008773B, 0x0012CC4F, 0x0006E53B, 0x00031FAD,
			0x0009443F, 0x0015E81D, 0x000439B5, 0x000B35F7, 0x00179D2F, 0x0019A733, 0x00014BA7, 0x0018FC4F,
			0x001D40FB, 0x0000286B, 0x001D653D, 0x00157861, 0x00138B11, 0x0012BD63, 0x0016AC31, 0x001F6795,
			0x00022BC5, 0x00166795, 0x0012548F, 0x000AF8A9, 0x0015B873, 0x0017746B, 0x000A2AA9, 0x00088A73,
			0x0014AC31, 0x00031F61, 0x0010653B, 0x000E5D93, 0x001514D1, 0x001A66F1, 0x001A3D49, 0x000B186F,
			0x0017BB2B, 0x0014A2DD, 0x00152C2F, 0x000A66FF, 0x0007C54B, 0x001CBF25, 0x000C0A5D, 0x001EBA91,
			0x000A98C5, 0x0015DD93, 0x001E5D43, 0x000F10DF, 0x00052807, 0x000B70FD, 0x00018615, 0x0005F855,
			0x000A02C5, 0x000DBA8D, 0x0001F029, 0x00078A73, 0x0019B7AD, 0x001347D7, 0x001F44F1, 0x001970D5,
		};

		final Point2[] pointSpiral = generatePointSpiral(LEN);

		IntSet pointCollisions = new IntSet(LEN);
		for (int i = 0; i < LEN; i++) {
			pointCollisions.add(pointSpiral[i].hashCode());
		}
		System.out.println(pointCollisions.size() + "/" + LEN + " hashes are unique.");
//		final long THRESHOLD = (long)(Math.pow(LEN, 11.0/10.0));
		final long THRESHOLD = (long) ((double) LEN * (double) LEN / (0.125 * pointCollisions.size()));

		final int[] problems = {0};
		final int COUNT = 8192;//GOOD.length;
		IntIntOrderedMap good = new IntIntOrderedMap(COUNT);
		int running = 0x31337;
		for (int x = 0; x < COUNT; x++) {
			good.put(running = running * GOOD[x & 511] & 0x1FFFFF, 0);
		}
		long[] minMax = new long[]{Long.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE, Long.MIN_VALUE};
		for (int a = 0; a < COUNT; a++) {
			final int g = good.keyAt(a);
			{
				final int finalA = a;
				ObjectSet set = new ObjectSet(51, 0.6f) {
					long collisionTotal = 0;
					int longestPileup = 0;

					@Override
					protected int place(@NonNull Object item) {
						return item.hashCode() * hashMultiplier >>> shift;
					}

					@Override
					protected void addResize(@NonNull Object key) {
						Object[] keyTable = this.keyTable;
						for (int i = place(key), p = 0; ; i = i + 1 & mask) {
							if (keyTable[i] == null) {
								keyTable[i] = key;
								return;
							} else {
								collisionTotal++;
								longestPileup = Math.max(longestPileup, ++p);
								good.put(g, longestPileup);
							}
						}
					}

					@Override
					protected void resize(int newSize) {
						int oldCapacity = keyTable.length;
						threshold = (int) (newSize * loadFactor);
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
								if (key != null) {
									addResize(key);
								}
							}
						}
						if (collisionTotal > THRESHOLD) {
							problems[0]++;
							throw new RuntimeException();
						}
					}

					@Override
					public void clear() {
						System.out.print(Base.BASE10.unsigned(finalA) + "/" + Base.BASE10.unsigned(COUNT) + ": Original 0x" + Base.BASE16.unsigned(g) + " on latest " + Base.BASE16.unsigned(hashMultiplier));
						System.out.println(" gets total collisions: " + collisionTotal + ", PILEUP: " + good.get(g));
						minMax[0] = Math.min(minMax[0], collisionTotal);
						minMax[1] = Math.max(minMax[1], collisionTotal);
						minMax[2] = Math.min(minMax[2], good.get(g));
						minMax[3] = Math.max(minMax[3], good.get(g));
						super.clear();
					}

					@Override
					public void setHashMultiplier(int hashMultiplier) {
						this.hashMultiplier = hashMultiplier | 1;
						resize(keyTable.length);
					}
				};
				set.setHashMultiplier(g);
				try {
					for (int i = 0, n = pointSpiral.length; i < n; i++) {
						set.add(pointSpiral[i]);
					}
				} catch (RuntimeException ignored) {
					System.out.println(g + " FAILURE");
					continue;
				}
				set.clear();
			}
		}
		System.out.println("This used a threshold of " + THRESHOLD);
		good.sortByValue(IntComparators.NATURAL_COMPARATOR);

		System.out.println("\n\nint[] GOOD_MULTIPLIERS = new int[]{");
		for (int i = 0; i < Integer.highestOneBit(good.size()); i++) {
			System.out.print("0x" + Base.BASE16.unsigned(good.keyAt(i)) + "=0x" + Base.BASE16.unsigned(good.getAt(i)) + ", ");
			if ((i & 7) == 7)
				System.out.println();
		}
		System.out.println("};\n");
		System.out.println(problems[0] + " problem multipliers in total, " + (COUNT - problems[0]) + " likely good multipliers in total.");
		System.out.println("Lowest collisions : " + minMax[0]);
		System.out.println("Highest collisions: " + minMax[1]);
		System.out.println("Lowest pileup     : " + minMax[2]);
		System.out.println("Highest pileup    : " + minMax[3]);
	}

}

/*
 * Copyright (c) 2022-2023 See AUTHORS file.
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

package com.github.tommyettinger.ds;

import com.github.tommyettinger.digital.BitConversion;
import com.github.tommyettinger.digital.Hasher;

import java.util.Arrays;

import static com.github.tommyettinger.digital.Hasher.*;

/**
 * Utility code shared by various data structures in this package.
 *
 * @author Tommy Ettinger
 */
public final class Utilities {
	/**
	 * A final array of 512 long multipliers that have been tested to work on at least some large
	 * input sets without excessively high collision rates. The initial value passed to a
	 * {@link ObjectSet#setHashMultiplier(long)} method (on any hashed data structure here) is used
	 * to choose one of these based on that long value (actually 11 of its middle bits). All hashed
	 * data structures here currently start with a multiplier of 0xD1B54A32D192ED03L, which is not
	 * in this array by default, but is still a pretty good multiplier.
	 * <br>
	 * You can mutate this array, but you should only do so if you encounter high collision rates or
	 * resizes with a particular multiplier from this table. Any long you set into this array must
	 * be an odd number, should be very large (typically the most significant bit is set), and should
	 * ideally have an "unpredictable" bit pattern. This last quality is the hardest to define, but
	 * generally dividing 2 to the 64 by an irrational number using BigDecimal math gets such an
	 * unpredictable pattern. Not all irrational numbers work; some are pathologically non-random
	 * when viewed as bits, but usually that only happens when the number was specifically constructed
	 * as a counter-example or an oddity. As always, <a href="http://oeis.org/search?q=decimal+expansion">The OEIS</a>
	 * proves a useful resource for getting long sequences of irrational digits, and if you make a
	 * BigDecimal with about 25 irrational digits, and divide (or multiply) it by 2 to the 64, convert
	 * it to a {@code long}, and if even, add 1, you'll have a good number to substitute in here...
	 * most of the time. Using the golden ratio (phi) does startlingly poorly here, even though
	 * mathematically it should be the absolute best multiplier.
	 * <br>
	 * The specific numbers in this array were chosen because they performed well (that is, without
	 * ever colliding more than about 25% over the average) on a data set of 200000 Vector2
	 * objects. Vector2 is from libGDX, and it can have an extremely challenging hashCode() for hashed
	 * data structures to handle if they don't adequately mix the hash's bits. On this data set, no
	 * multiplier used recorded fewer than 600000 collisions, and any multipliers with collision counts
	 * above a threshold of 677849 were rejected. That means these 512 are about as good as it gets.
	 * The multipliers were also tested on a similar data set of 235970 English dictionary words
	 * (including many proper nouns), with fewer collisions here (a little more than 1/20 as many).
	 * All multipliers satisfied a threshold of 35000 collisions with the dictionary data set.
	 */
	public static final int[] GOOD_MULTIPLIERS = new int[]{
		0x30997D75, 0x419F6AE3, 0x7FC6F5F3, 0xD33C4C3D, 0xEEDAD501, 0x158D36F9, 0x4912D2B7, 0xA27A0E77,
		0x5A8AB569, 0xF7914C3D, 0x9B56CC3D, 0x0A4348A3, 0x47C2F571, 0xB4A9C255, 0xE0EDF571, 0x260DFD75,
		0xBF0B1E47, 0xC55254FF, 0x802F52B7, 0x16150E77, 0xFE2C5205, 0x6EAB7D75, 0xB7F7D5EB, 0x2E15EAE3,
		0x111A4987, 0xFB412207, 0x3262E173, 0x9EBFA119, 0x78DA7B2F, 0xCA116CE9, 0xE8A214D9, 0x456B85C7,
		0xDB8CC255, 0x37702DC3, 0x60820427, 0xA361A649, 0x2F4D7B2F, 0x45B0D2B7, 0x2BE694D9, 0xAE9865D5,
		0x90273B83, 0x2224D4FF, 0xE798EAE3, 0xE6D3F5F3, 0x9356E5D5, 0xD04765D5, 0x6243B6F9, 0x8FCAD4FF,
		0x68AB8E77, 0xD680D339, 0x44A5ADC3, 0x80914C17, 0xE43FE173, 0xDF31ADC3, 0x5D8BC057, 0x987A52B7,
		0x9CF30427, 0x2B165205, 0x22DFBCA7, 0xF70EFB2F, 0x3DEBD4FF, 0x07AB55EB, 0x2A61B569, 0xA32B7D75,
		0xE1DCA649, 0x02F63713, 0x52DAD501, 0xC7ADB569, 0xAB818427, 0x1F675501, 0x4FCC3B83, 0xEEEEFB2F,
		0x77CB4987, 0xBC9265D5, 0x3EE914D9, 0x77F854FF, 0xB3E7C8D7, 0x89D4D5EB, 0x37487571, 0xC2C2AA37,
		0xFE3714D9, 0xBEC16173, 0xD9C8D2B7, 0x0212BB83, 0xEFBF7BB3, 0xEAC3A119, 0x18C2C8D7, 0xCE48C057,
		0x7C41E8E7, 0x35C65205, 0xAFE24057, 0x3D5C14D9, 0x06F0C8A3, 0x1D06F6F7, 0x9C5E0E77, 0x72CB7D75,
		0x4E582649, 0x41E13713, 0xC5A1F5F3, 0x1F36CC17, 0x774BB569, 0xC31ACC17, 0xB0B1FBB3, 0x2E20F6F7,
		0x2156FD75, 0xBE5BAA37, 0x7935B713, 0xDC8F2207, 0x5852A649, 0x448F5501, 0xFD250415, 0x46A9D501,
		0x5161CC17, 0x491AA649, 0xCB02C8A3, 0x8EDD1E47, 0x8130ECE9, 0x895548A3, 0xCB8D7D75, 0x2B6DADC3,
		0x7B814C3D, 0x6912C987, 0x2A08C4BF, 0x1D484057, 0x224750DB, 0x939F7BB3, 0x7F19D0DB, 0x671B7571,
		0x9D1D2DC3, 0x50D5EAE3, 0x33953569, 0xF3B365D5, 0xB88C1E47, 0x0FE6B569, 0xC7CDEA89, 0xE1F5D205,
		0x908CD501, 0x52E0C2E5, 0xC3B05339, 0x245D42E5, 0xD3BFC057, 0x648568E7, 0x002F2649, 0x37CB0415,
		0x8778CC17, 0xD80D48A3, 0xB16052B7, 0xACEDD501, 0x94E9D0DB, 0xE9260415, 0x8AE43B83, 0x691848D7,
		0xDFE7F6F7, 0xE874C987, 0x7BD6CD59, 0xAF05BCA7, 0x01938427, 0xBE624987, 0xF3DA8427, 0xB43C42E5,
		0x5E4B0427, 0x37A35339, 0xAAAA6173, 0x335BCC3D, 0xD6CD4EC9, 0x5E2E94D9, 0x549A4C3D, 0x005D65D5,
		0x99B8E8E7, 0x3F5D5205, 0x050FEAE3, 0x5CC98427, 0x67CFD5EB, 0x6E4B85C7, 0x16A0E173, 0xA4B7E5D5,
		0x85ED2119, 0xA999A207, 0x241955EB, 0x2CBF2207, 0xD7610427, 0xC85DFD75, 0x9C104D59, 0x7032B713,
		0xA95FE8E7, 0x337EF6F7, 0x74104C17, 0x9099C057, 0x11568415, 0x359A48D7, 0x2191A649, 0xB87BA207,
		0x684DAA37, 0xA9F355EB, 0x51CDC057, 0xEB2952B7, 0xF6F5AA37, 0xFD7F9497, 0x4C3C52B7, 0xF2DF85C7,
		0x4443C8A3, 0x44CCE5D5, 0x49F854FF, 0x47EFB713, 0xF0D02DC3, 0xEC437B2F, 0x7680EA89, 0x121BD0DB,
		0xC3ECC057, 0x0C0AF571, 0xD5312DC3, 0xAFF4C4BF, 0xA6DF2207, 0xDB062A37, 0x633DC8A3, 0x34C1D205,
		0x36CB48A3, 0xACE84987, 0x9A7DE173, 0xE05950DB, 0x0412F571, 0x5C276AE3, 0xB705D5EB, 0xC1C93B83,
		0xB6864C3D, 0x2079B6F9, 0xB62FD2B7, 0x92B35501, 0xFED442E5, 0xB3ACD205, 0xC4EEA649, 0xB44F8427,
		0xE6A014D9, 0x8FD3F5F3, 0x291155EB, 0x51EB9497, 0x99694EC9, 0x7DB594D9, 0xA1627D75, 0x6A1EE5D5,
		0xD7442119, 0x0801AA37, 0x0D6A4987, 0x55AF5205, 0x511C5501, 0x452C9497, 0x355BC2E5, 0x878C55EB,
		0x464FD4FF, 0xCBAB6AE3, 0x0E33B6F9, 0x4FA848A3, 0xE0C22207, 0x20C84C17, 0x7DC0CC3D, 0x2E3FC057,
		0xE75D52B7, 0x53D14D59, 0x6EDACEC9, 0xD7E47B2F, 0xDDCA2119, 0x44851E47, 0xE24C1E47, 0x036C48D7,
		0xB1D3FD75, 0x007055EB, 0xD32AF571, 0x3E1CD205, 0x3A527BB3, 0xB7F6B569, 0x76B2BCA7, 0x8135ADC3,
		0x071A8415, 0x3B48FBB3, 0x5F89ECE9, 0xD4CA42E5, 0x80FF3713, 0xE0930E77, 0xD8538427, 0x7DC05339,
		0xEEE1ADC3, 0xD7750427, 0x5B79EA89, 0x22A3A649, 0xBB9E1497, 0xDFEBCC17, 0x1E4CD0DB, 0x334B4255,
		0xFFEFD4FF, 0xB979FB2F, 0xB19DD2B7, 0x0D6AD0DB, 0x43AEAA37, 0x23346173, 0xD9372119, 0x694952B7,
		0x88BDA207, 0xE3686173, 0x3BB5ADC3, 0xA7E3D501, 0x3DFB85C7, 0x25368415, 0x5401CC17, 0x74AE85C7,
		0x54844057, 0xE9D4E8E7, 0x2B8FFB2F, 0x67EAC987, 0xEB5A2119, 0x68E21497, 0xC6244255, 0xE3278427,
		0x448C52B7, 0xAA274EC9, 0xCAF64987, 0x14A4ECE9, 0xB2BD2207, 0x0DF2CC3D, 0x90F92207, 0x47C6CC3D,
		0xA7503713, 0xA5B34C3D, 0xFB5A0E77, 0xAACE3569, 0x3894C057, 0x4E51E173, 0xC5382A37, 0x6FD9C987,
		0xA0B97B2F, 0x73D95501, 0xD49DC987, 0xC22AF5F3, 0xF910FBB3, 0xF93905C7, 0xD9077571, 0x17AFF6F7,
		0xB4A314D9, 0xA945E8E7, 0x0A345501, 0x4237CEC9, 0xA837D205, 0x02CF85C7, 0x289F4EC9, 0x07F876F7,
		0xA65DCEC9, 0xEF87C987, 0x694942E5, 0xF02BF571, 0xDB8442E5, 0x694E50DB, 0x261344BF, 0x38708E77,
		0x7E9AB713, 0xC8034987, 0x97580427, 0x2EC585C7, 0x10A2B6F9, 0x06E8F571, 0xE5C24EC9, 0xDDDFCDD1,
		0x53014DD1, 0xDA1FC7FB, 0xE17B136F, 0x9B89C7FB, 0x34D10B4F, 0x5996D71B, 0x35C9CDD1, 0xB910CDD1,
		0x572F0B4F, 0xA9B2C7FB, 0x2EB88B4F, 0x680547FB, 0xFFFAC7FB, 0x0EE647FB, 0x6878936F, 0xBA28CDD1,
		0x2F54936F, 0x3970CDD1, 0x0F39136F, 0x5EC7936F, 0xC5BDC7FB, 0x05F4571B, 0x078E936F, 0xF904936F,
		0xCD9B0B4F, 0x71DDD71B, 0xF791CDD1, 0xDAD5C7FB, 0xB32C4DD1, 0x1444571B, 0x32BAC7FB, 0xD713571B,
		0xF96E4DD1, 0x27948B4F, 0xCAE9CDD1, 0x90FD136F, 0xC4EC8B4F, 0x6061571B, 0xC9FE136F, 0x501C571B,
		0x5CC747FB, 0x8ECCC7FB, 0x3614548F, 0x54BA2AA9, 0x3FB18A27, 0xD7101A2D, 0x2E2514C9, 0x61DEF7C7,
		0xCB564ED1, 0x153A0EB9, 0x87BE0CAF, 0xF89FC43F, 0xBCAC00F5, 0xCD2F77C7, 0xE44CD0CD, 0x9A8A14D1,
		0x3572EAF1, 0xF906CAB9, 0xC24C98E1, 0xB0C11663, 0x1085CBF3, 0x3C5AB53D, 0x23F37C2F, 0xC3F438A1,
		0x1C0F5DB3, 0x6B1CE795, 0x409A741B, 0x1A7AE9E7, 0x737040CF, 0xFD1D7E5D, 0x14481CB1, 0xBF4B6F35,
		0x6DA4A781, 0xAD6EBF25, 0x76595695, 0xAE1BCB61, 0x6D7DF667, 0x33098BD7, 0x83D0AC31, 0xE6BEF46B,
		0x579DF7C7, 0x7F80E53B, 0x977D0A5D, 0x4B10F71F, 0xA7CF0475, 0xCEDF10DF, 0xAB739CEB, 0xA8702733,
		0xCDB7D14F, 0x6C41409D, 0xB907D7F3, 0x6831357D, 0x7FFBF855, 0x39A937AD, 0x4B3470FD, 0xFF6B3DE1,
		0x175C9C89, 0xC527A781, 0xFC18E671, 0x77622A4F, 0x2F88548F, 0xC54D38A1, 0x31EE00C9, 0x08428D31,
		0x191D66FF, 0x7C639F61, 0x1E8BFDE7, 0x2990F17B, 0xB9023DE1, 0x85D64B83, 0xD7EEDD93, 0x48738D13,
		0xEE72765B, 0x88F9D823, 0xC807AB3F, 0x6D79C7D7, 0xEDC8773B, 0x13D2CC4F, 0xB106CAB9, 0x21E6E53B,
		0xCA031FAD, 0x9A69443F, 0xA175E81D, 0xC6E439B5, 0xC3AB35F7, 0x62579D2F, 0xA8D9A733, 0x25014BA7,
		0x12B8FC4F, 0xB8FD40FB, 0xDB00286B, 0x509D653D, 0x5D157861, 0x6CB38B11, 0xA732BD63, 0x9FF6AC31,
		0x74DF6795, 0x7E622BC5, 0xC8D66795, 0x0872548F, 0x92EAF8A9, 0x1E15B873, 0xE257746B, 0x5A4A2AA9,
		0xE0A88A73, 0xC5D4AC31, 0x54231F61, 0x4DF0653B, 0x836E5D93, 0x335514D1, 0x759A66F1, 0x925A3D49,
	};


	private static final int COPY_THRESHOLD = 128;
	private static final int NIL_ARRAY_SIZE = 1024;
	private static final Object[] NIL_ARRAY = new Object[NIL_ARRAY_SIZE];

	/**
	 * Not instantiable.
	 */
	private Utilities () {
	}

	private static float defaultLoadFactor = 0.7f;

	/**
	 * Sets the load factor that will be used when none is specified during construction (for
	 * data structures that have a load factor, such as all sets and maps here). The load factor
	 * will be clamped so that it is greater than 0 (the lowest possible is
	 * {@link #FLOAT_ROUNDING_ERROR}, but it should never actually be that low) and less than or
	 * equal to 1. The initial value for the default load factor is 0.7.
	 * <br>
	 * If multiple libraries and/or your own code depend on jdkgdxds, then they may attempt to set
	 * the default load factor independently of each other, but this only has one setting at a time.
	 * The best solution for this is to specify the load factor you want when it matters, possibly
	 * to a variable set per-library or even per section of a library that needs some load factor.
	 * That means <b>not using the default load factor in this class</b>, and always using the
	 * constructors that specify a load factor. Libraries are generally discouraged from setting the
	 * default load factor; that decision should be left up to the application using the library.
	 *
	 * @param loadFactor a float that will be clamped between 0 (exclusive) and 1 (inclusive)
	 */
	public static void setDefaultLoadFactor (float loadFactor) {
		defaultLoadFactor = Math.min(Math.max(loadFactor, FLOAT_ROUNDING_ERROR), 1f);
	}

	/**
	 * Gets the default load factor, meant to be used when no load factor is specified during the
	 * construction of a data structure such as a map or set. The initial value for the default
	 * load factor is 0.7.
	 *
	 * @return the default load factor, always between 0 (exclusive) and 1 (inclusive)
	 */
	public static float getDefaultLoadFactor () {
		return defaultLoadFactor;
	}

	/**
	 * Used to establish the size of a hash table for {@link ObjectSet}, {@link ObjectObjectMap}, and related code.
	 * The table size will always be a power of two, and should be the next power of two that is at least equal
	 * to {@code capacity / loadFactor}.
	 *
	 * @param capacity   the amount of items the hash table should be able to hold
	 * @param loadFactor between 0.0 (exclusive) and 1.0 (inclusive); the fraction of how much of the table can be filled
	 * @return the size of a hash table that can handle the specified capacity with the given loadFactor
	 */
	public static int tableSize (int capacity, float loadFactor) {
		if (capacity < 0) {
			throw new IllegalArgumentException("capacity must be >= 0: " + capacity);
		}
		int tableSize = 1 << -BitConversion.countLeadingZeros(Math.max(2, (int)Math.ceil(capacity / loadFactor)) - 1);
		if (tableSize > 1 << 30 || tableSize < 0) {
			throw new IllegalArgumentException("The required capacity is too large: " + capacity);
		}
		return tableSize;
	}

	/**
	 * Set all elements in {@code objects} to null.
	 * This method is faster than {@link Arrays#fill} for large arrays (> 128).
	 * <br>
	 * From Apache Fury's ObjectArray class.
	 */
	public static void clear(Object[] objects) {
		clear(objects, 0, objects.length);
	}

	/**
	 * Set all {@code size} elements in {@code objects}, starting at index {@code start}, to null.
	 * This method is faster than {@link Arrays#fill} for large arrays (> 128).
	 * <br>
	 * From Apache Fury's ObjectArray class.
	 */
	public static void clear(Object[] objects, int start, int size) {
		if (size < COPY_THRESHOLD) {
			Arrays.fill(objects, start, start + size, null);
		} else {
			if (size < NIL_ARRAY_SIZE) {
				System.arraycopy(NIL_ARRAY, 0, objects, start, size);
			} else {
				while (size > NIL_ARRAY_SIZE) {
					System.arraycopy(NIL_ARRAY, 0, objects, start, NIL_ARRAY_SIZE);
					size -= NIL_ARRAY_SIZE;
					start += NIL_ARRAY_SIZE;
				}
				System.arraycopy(NIL_ARRAY, 0, objects, start, size);
			}
		}
	}

	/**
	 * A placeholder Object that should never be reference-equivalent to any Object used as a key or value. This is only public
	 * so data structures can use it for comparisons; never put it into a data structure.
	 */
	public static final Object neverIdentical = new Object();

	/**
	 * A float that is meant to be used as the smallest reasonable tolerance for methods like {@link #isEqual(float, float, float)}.
	 */
	public static final float FLOAT_ROUNDING_ERROR = 1E-6f;

	/**
	 * Equivalent to libGDX's isEqual() method in MathUtils; this compares two floats for equality and allows just enough
	 * tolerance to ignore a rounding error. An example is {@code 0.3f - 0.2f == 0.1f} vs. {@code isEqual(0.3f - 0.2f, 0.1f)};
	 * the first is incorrectly false, while the second is correctly true.
	 *
	 * @param a the first float to compare
	 * @param b the second float to compare
	 * @return true if a and b are equal or extremely close to equal, or false otherwise.
	 */
	public static boolean isEqual (float a, float b) {
		return Math.abs(a - b) <= FLOAT_ROUNDING_ERROR;
	}

	/**
	 * Equivalent to libGDX's isEqual() method in MathUtils; this compares two floats for equality and allows the given
	 * tolerance during comparison. An example is {@code 0.3f - 0.2f == 0.1f} vs. {@code isEqual(0.3f - 0.2f, 0.1f, 0.000001f)};
	 * the first is incorrectly false, while the second is correctly true.
	 *
	 * @param a         the first float to compare
	 * @param b         the second float to compare
	 * @param tolerance the maximum difference between a and b permitted for this to return true, inclusive
	 * @return true if a and b have a difference less than or equal to tolerance, or false otherwise.
	 */
	public static boolean isEqual (float a, float b, float tolerance) {
		return Math.abs(a - b) <= tolerance;
	}

	/**
	 * A simple equality comparison for {@link CharSequence} values such as {@link String}s or {@link StringBuilder}s
	 * that ignores case by upper-casing any cased letters. This works for all alphabets in Unicode except Georgian.
	 *
	 * @param a a non-null CharSequence, such as a String or StringBuilder
	 * @param b a non-null CharSequence, such as a String or StringBuilder
	 * @return whether the contents of {@code a} and {@code b} are equal ignoring case
	 */
	public static boolean equalsIgnoreCase (CharSequence a, CharSequence b) {
		if (a == b)
			return true;
		final int al = a.length();
		if (al != b.length())
			return false;
		for (int i = 0; i < al; i++) {
			char ac = a.charAt(i), bc = b.charAt(i);
			if (ac != bc && Character.toUpperCase(ac) != Character.toUpperCase(bc)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Like {@link String#compareToIgnoreCase(String)}, but works for all {@link CharSequence} values, such as
	 * {@link String}s or {@link StringBuilder}s; this ignores case by upper-casing any cased letters.
	 * This technique works for all alphabets in Unicode except Georgian.
	 *
	 * @param l a non-null CharSequence, such as a String or StringBuilder
	 * @param r a non-null CharSequence, such as a String or StringBuilder
	 * @return  ignoring case: 0 if the two {@code CharSequence}s are equal;
	 *          a negative integer if {@code l}
	 *          is lexicographically less than {@code r}; or a
	 *          positive integer if {@code l} is
	 *          lexicographically greater than {@code r}
	 */
	public static int compareIgnoreCase (CharSequence l, CharSequence r) {
		if (l == r)
			return 0;
		for (int i = 0, len = Math.min(l.length(), r.length()); i < len; i++) {
			char a = Character.toUpperCase(l.charAt(i));
			char b = Character.toUpperCase(r.charAt(i));
			if (a != b) {
				return a - b;
			}
		}

		return l.length() - r.length();
	}

	/**
	 * Gets a 64-bit thoroughly-random hashCode from the given CharSequence, ignoring the case of any cased letters.
	 * Uses Water hash, which is a variant on <a href="https://github.com/vnmakarov/mum-hash">mum-hash</a> and
	 * <a href="https://github.com/wangyi-fudan/wyhash">wyhash</a>. This gets the hash as if all cased letters have been
	 * converted to upper case by {@link Character#toUpperCase(char)}; this should be correct for all alphabets in
	 * Unicode except Georgian. Typically, place() methods in Sets and Maps here that want case-insensitive hashing
	 * would use this with {@code (int)(longHashCodeIgnoreCase(text) >>> shift)}.
	 * <br>
	 * This is very similar to the {@link Hasher#hash64(CharSequence)} method, and shares
	 * the same constants and mum() method with Hasher.
	 *
	 * @param data a non-null CharSequence; often a String, but this has no trouble with a StringBuilder
	 * @return a long hashCode; quality should be similarly good across any bits
	 */
	public static long longHashCodeIgnoreCase (final CharSequence data) {
		return longHashCodeIgnoreCase(data, 9069147967908697017L);
	}

	/**
	 * Gets a 64-bit thoroughly-random hashCode from the given CharSequence, ignoring the case of any cased letters.
	 * Uses Water hash, which is a variant on <a href="https://github.com/vnmakarov/mum-hash">mum-hash</a> and
	 * <a href="https://github.com/wangyi-fudan/wyhash">wyhash</a>. This gets the hash as if all cased letters have been
	 * converted to upper case by {@link Character#toUpperCase(char)}; this should be correct for all alphabets in
	 * Unicode except Georgian. Typically, place() methods in Sets and Maps here that want case-insensitive hashing
	 * would use this with {@code (int)(longHashCodeIgnoreCase(text) >>> shift)}.
	 * <br>
	 * This is very similar to the {@link Hasher#hash64(CharSequence)} method, and shares
	 * the same constants and mum() method with Hasher.
	 *
	 * @param data a non-null CharSequence; often a String, but this has no trouble with a StringBuilder
	 * @param seed any long; must be the same between calls if two equivalent values for {@code data} must be the same
	 * @return a long hashCode; quality should be similarly good across any bits
	 */
	public static long longHashCodeIgnoreCase (final CharSequence data, long seed) {
		final int len = data.length();
		for (int i = 3; i < len; i += 4) {
			seed = mum(
				mum(Character.toUpperCase(data.charAt(i - 3)) ^ b1, Character.toUpperCase(data.charAt(i - 2)) ^ b2) - seed,
				mum(Character.toUpperCase(data.charAt(i - 1)) ^ b3, Character.toUpperCase(data.charAt(i)) ^ b4));
		}

		switch (len & 3) {
		case 0:
			seed = mum(b1 - seed, b4 + seed);
			break;
		case 1:
			seed = mum(b5 - seed, b3 ^ Character.toUpperCase(data.charAt(len - 1)));
			break;
		case 2:
			seed = mum(Character.toUpperCase(data.charAt(len - 2)) - seed, b0 ^ Character.toUpperCase(data.charAt(len - 1)));
			break;
		case 3:
			seed = mum(Character.toUpperCase(data.charAt(len - 3)) - seed, b2 ^ Character.toUpperCase(data.charAt(len - 2))) + mum(b5 ^ seed, b4 ^ Character.toUpperCase(data.charAt(len - 1)));
			break;
		}
		seed = (seed ^ len) * (seed << 16 ^ b0);
		return (seed ^ (seed << 33 | seed >>> 31) ^ (seed << 19 | seed >>> 45));
	}

}

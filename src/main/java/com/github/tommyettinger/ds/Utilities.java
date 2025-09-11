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

package com.github.tommyettinger.ds;

import com.github.tommyettinger.digital.BitConversion;
import com.github.tommyettinger.digital.Hasher;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

import static com.github.tommyettinger.digital.Hasher.*;

/**
 * Utility code shared by various data structures in this package.
 *
 * @author Tommy Ettinger
 */
public final class Utilities {
	/**
	 * A final array of 512 int multipliers that have shown to have few collisions in some kinds of
	 * hash table. These multipliers aren't currently used within jdkgdxds except in
	 * {@link #hashCodeIgnoreCase(CharSequence, int)}, the CaseInsensitive maps and sets, and the
	 * Filtered maps and sets, but may still be used externally.
	 * <br>
	 * You can mutate this array, but you should only do so if you encounter high collision rates or
	 * resizes with a particular multiplier from this table. Any int m you set into this array must
	 * satisfy {@code (m & 0x80000001) == 0x80000001}, and should ideally have an "unpredictable" bit
	 * pattern. This last quality is the hardest to define, but
	 * generally dividing 2 to the 32 by an irrational number between 1 and 2 using BigDecimal or double
	 * math gets such an unpredictable pattern. Not all irrational numbers work, and the ones here were
	 * found empirically.
	 * <br>
	 * The specific numbers in this array were chosen because they performed well (that is, without
	 * ever colliding more than about 25% over the average) on a data set of 200000 Vector2
	 * objects. Vector2 is from libGDX, and it can have an extremely challenging hashCode() for hashed
	 * data structures to handle if they don't adequately mix the hash's bits. On this data set, no
	 * multiplier used recorded fewer than 600000 collisions, and any multipliers with collision counts
	 * above a threshold of 677849 were rejected. That means these 512 are about as good as it gets.
	 * When tested with an equivalent to GridPoint2 from libGDX, vastly more objects had identical
	 * hashCode() results to a different GridPoint2 in the set. Only about a quarter as many hashCode()
	 * values were returned as there would be with all-unique results. Even with that more-challenging
	 * situation, this set did not have any multipliers that were significantly worse than average.
	 * An earlier version of the multipliers did have 97 "problem multipliers" for GridPoint2.
	 * The multipliers were also tested on a similar data set of 235970 English dictionary words
	 * (including many proper nouns), with fewer collisions here (a little more than 1/20 as many).
	 * All multipliers satisfied a threshold of 35000 collisions with the dictionary data set.
	 */
	public static final int[] GOOD_MULTIPLIERS = new int[]{
		0xEC6794E3,
		0x9B89CD59, 0xDCA1C8D7, 0xC5F768E7, 0x92317571, 0x937CD501, 0xE993C987, 0xD5567571, 0x85C8ADB5,
		0xE6AC8B4F, 0xC21736F9, 0xFD890F79, 0xC514D823, 0xF151575F, 0x8BDCE3EF, 0xA7F27B2F, 0x8C1EAA4F,
		0xCCE4C43F, 0x82E28415, 0xC6A39455, 0xE6245E51, 0xC33AFB2F, 0xBFA927CB, 0xAC11C8A3, 0xC00E6AF1,
		0xF98DDA5B, 0x8FA1F025, 0xF0CFFC71, 0xA49DC54B, 0xB3A7C3C3, 0xC2F9C7BD, 0xE0CC8899, 0xB3B0A51D,
		0xF01DF9A5, 0x9E7300F5, 0xA7E675F3, 0xFE3FB283, 0xE0FF1497, 0xB2CE9603, 0xD9EF3FCD, 0xB7F3D71B,
		0xE438BEA9, 0xF16A6DCD, 0xA4613217, 0xAAE7C54B, 0xB56A208B, 0xBCA43B89, 0xEBE28BC7, 0x8567101B,
		0x9F45E6F1, 0xF95D5505, 0xCDCBFDE3, 0xECFA5363, 0xB449917B, 0xE1D8CCA3, 0xE208D04F, 0xC0A33019,
		0xCD722469, 0xC3D56C8B, 0xEC34F7A1, 0xBF6C9497, 0xAE7244F5, 0xF3DEC4BF, 0xB79FD4FF, 0xFF54D7E7,
		0xEE99B50F, 0xB965E897, 0xE37B5231, 0xF9FBF639, 0x965B209B, 0xFB164EC9, 0xE33667EF, 0x808498B7,
		0xFFD4C7BD, 0xCF6740FB, 0x98EFE1B5, 0xA5CCFBCF, 0xB268E277, 0xFF48E3EF, 0xAB8ECED1, 0x83B4DFC5,
		0x8E4EA9ED, 0xDB6B35F7, 0x9DCAF41B, 0xC2EEC5D5, 0xBBEB6F4F, 0x9645256D, 0x9BE82D2B, 0xC7C8533D,
		0x8FA69905, 0xE4F8CD59, 0xAFDBF6C1, 0xC17E533D, 0x80922B93, 0xF40F52B7, 0xB824EF5F, 0x8EFBF295,
		0xFA482B93, 0x8492ADC3, 0xBCBA2E15, 0xD6F7F7AB, 0x80A58489, 0xB867400B, 0xD5D33021, 0xF43AAD83,
		0xA495DEA3, 0xBF03ABE5, 0xFCE635CB, 0xB4422C7D, 0xCB39CDBD, 0xB370E565, 0x8701C045, 0xCD01F09F,
		0xEA326ABB, 0x8DB96BBD, 0x97D3EA41, 0xE4B3EBB9, 0xA9D2159B, 0xF3EDEDDD, 0xE018F82D, 0xE609C2E5,
		0xFE852FB3, 0xC2107ECF, 0xD42AE479, 0x9E128095, 0xF38831AF, 0xAF439D51, 0x87FF76C1, 0xD48BF7C7,
		0x8099F855, 0xCBC5273F, 0x978BDC1D, 0xEDEAD625, 0xB784AF47, 0x8C4C960F, 0xDC0CFB0D, 0xFCB95601,
		0xA25AF703, 0xE0F6C62B, 0xB9229497, 0x80B41AB9, 0xCEA1EDDB, 0xB5A7B713, 0xC7AA1AE7, 0xED81746B,
		0xAB08B11D, 0x8F63D7F3, 0x8FABDD75, 0x8F405B2D, 0xDD51AB15, 0xD7131D63, 0xFCC13CA9, 0xE7D8B7D7,
		0x83A0FC83, 0xD689CA89, 0xECE5E5D5, 0xC1CB8BEB, 0xF1D565C9, 0xC1ADCC9D, 0xB2985335, 0xF28FB701,
		0xE8F1FBCF, 0xC8E7F03D, 0xF3AEC9B5, 0xCF030129, 0xB84DA7F3, 0x94A32A2B, 0xEB1A2609, 0xA1E0543F,
		0xFF96253F, 0x87604B41, 0xA1FE1C2F, 0xD5FBE8B5, 0xF0707F65, 0xE81D17CD, 0x9D3430ED, 0xF44D7C2D,
		0xFAAD5D75, 0x9ED5A1E3, 0xBA56FE47, 0xFAB9E45B, 0x94DFAC1B, 0xC0A7D057, 0xC4E985F9, 0xA8612159,
		0xE35C86ED, 0xFA0C22AB, 0xE607294F, 0xCC11653B, 0xFAD1EED3, 0xA49D3AFF, 0xB7D7B6B1, 0xC0A8E4EF,
		0xAF255C59, 0xF101965F, 0xBA7DB033, 0x9AC1679B, 0xC1831603, 0xE6F4AAB7, 0x8DC1D40F, 0xC076504F,
		0xCC8965C7, 0xA562FE85, 0xEE138EB9, 0xF0EE34C7, 0xEE739611, 0xFE191A7D, 0xCBBEE81D, 0xD0DE6BBD,
		0xF6D38D31, 0x9F620FFD, 0x82E63243, 0xF4A1FFF3, 0xAD1185C7, 0x83B2BF37, 0xADE3D033, 0xD568B12D,
		0x927B92DF, 0xF664655D, 0x9CB98587, 0x825DFCA3, 0xD5AC8F79, 0xC79860D5, 0xD063D19B, 0xDE0C7DF7,
		0x9C2759F5, 0xA856B25F, 0xB1BEB50F, 0xCED724B3, 0xD68F0657, 0xC862D5DD, 0xACABAFB3, 0x9E01B893,
		0x98FB4B61, 0x87D6B58B, 0xCE42682D, 0xC8FF67C7, 0x8F531893, 0xCFB5FEA7, 0x83D18A7B, 0xB07D3A2D,
		0xB423727D, 0xAA333A2D, 0x978F92ED, 0x830CD2EB, 0xA5EBC713, 0xD3C8C535, 0xDAE520E5, 0xCC356C4D,
		0x8AE04AAD, 0xAC226E1D, 0x966E5FAB, 0xD241CD23, 0x84487F11, 0xA5DE22F3, 0xE2061CD3, 0xBE5B9F0B,
		0xC88E2807, 0x8AEDE62B, 0xAF2D17D7, 0xC1188E6D, 0xD08851B5, 0xA979EC49, 0xA26FD4F7, 0xFC92BDFB,
		0xFB73BC8B, 0xB26FABE5, 0xC2F8BBC7, 0xC5AA2797, 0xB5511B61, 0x8BCDD4B7, 0xC4431803, 0x853BC693,
		0xFF544D9B, 0x8D32BB1B, 0xCF2CB46F, 0xBCBECE53, 0xCF26F6F7, 0xE3C1B8CF, 0x92648319, 0xC29E60A3,
		0x880E8E9B, 0xD7336DA5, 0xDF391243, 0xA2945DB3, 0xB943F971, 0x8009F5F3, 0x844BAB95, 0x8C515033,
		0x9296AFF5, 0xA9288C65, 0xA0B1A807, 0xB725C529, 0xA36E8EC7, 0xA59B6A83, 0xB1173B23, 0x91F6787F,
		0x8F009B75, 0x93A815E7, 0xC465778F, 0x9425DC8D, 0xCA564247, 0xDF3909A1, 0xC788231F, 0x854066D1,
		0xAAA0CBBF, 0xF4E86F33, 0xD1D3B9E7, 0xE81D90D5, 0xFB9F0613, 0xCD3E83D3, 0xC3A1CBF5, 0xD4D8A629,
		0xB1A74627, 0x9DF00FC5, 0xC7559721, 0xAA19AAB7, 0xF0A31CA7, 0xBD355B25, 0xA3A55BE1, 0xF0855D59,
		0xE935C8D7, 0xBF8F0567, 0x93912007, 0xCE810209, 0x8E0CE38D, 0xA898701D, 0xE6B6789F, 0x901BFFFB,
		0xCA0A9FDB, 0xFC8F186F, 0xA8A4FFA5, 0xEA393429, 0xEF3AF87B, 0xB80947FD, 0xC2D190D3, 0xA8DBCCBF,
		0xF7A1B909, 0xA4CB2F61, 0xC5AD4B79, 0xF288FED7, 0xE45710E5, 0x8A2E6A69, 0xE69BEE77, 0xF0CE6ED1,
		0x95B94A41, 0xD2403F83, 0xD3A1919F, 0x81668DEB, 0xEBA70489, 0xD0CDB4FF, 0xD81FD1D3, 0xB302C987,
		0xABB672CD, 0x9E55CBF3, 0xCF5B331D, 0xA8FA9803, 0x8BD6AD6F, 0xD0ED94DB, 0xC3BCF1AD, 0xC30969E3,
		0x847FE7BB, 0xCB0959AB, 0x8CF7D80F, 0xFAEFB6C3, 0xD9EFDB5D, 0xA02B8A53, 0xCED41215, 0xDA322367,
		0x8C2A253F, 0x88663643, 0xB8580E4B, 0x83657DE7, 0xE4E04137, 0xF9E0C7DF, 0xA68C7791, 0xC17DC8F1,
		0xC9247ACF, 0xDC8DEE37, 0xA88A6439, 0x95E6BCF9, 0xDE0184B1, 0xE881C805, 0xCEA405C7, 0x9A3AB6F9,
		0xB520960B, 0xD83C14C5, 0xFF570117, 0x855FCDA5, 0xEB65580F, 0xB1A10705, 0xFE1B43AD, 0xDA0B0115,
		0xE2C5A9D9, 0xC9FB76BD, 0xDA6C84C9, 0xB76FD153, 0xF3A9039B, 0xA49668EF, 0xE09F978B, 0xF701704F,
		0xE39ECEF7, 0xEDD14F51, 0xB1E0228B, 0xE78D0427, 0x9438B3D9, 0xB5CE57BB, 0xABFBDEB9, 0x9CEC22C9,
		0x9BE73B1B, 0x897954FF, 0xC6E3D5B7, 0xE0D3DC53, 0xE79030AB, 0xA4E2AF8D, 0x86C337AD, 0xADCBDFA1,
		0x8E2ED2B3, 0xB3A06767, 0xC179176F, 0xE0503E4F, 0xD30A3B83, 0xDD8A7ED7, 0xB68E5DC9, 0xA00EC5B7,
		0xB57AD749, 0xA0148BD1, 0xEB8D7DB5, 0x8CE7A2FB, 0xA8D481A7, 0xF54EBE4F, 0xDFBF3899, 0x8F7507B9,
		0x8DAF8FB1, 0x83C514D9, 0xC5414787, 0xD5FA5B15, 0xD90AC247, 0x864C3A75, 0x828E004F, 0xC18F9447,
		0xF458E5D9, 0x950540CD, 0xF13D03B7, 0xABDF22BF, 0xEF226C59, 0xACA4EEE7, 0x9D9532F7, 0xDB65787F,
		0x86F7E439, 0xA3AA8F0D, 0xEFBF06E5, 0xCC448CC9, 0x95C66415, 0xACBE0555, 0xCC211003, 0xE72A6339,
		0x8699F4CD, 0xCAC0C9DD, 0xCA5B050D, 0xCA3D45BF, 0xE73DB38D, 0xAFA63E3F, 0xC94E3F59, 0xDAF09BD5,
		0xCFCBE891, 0xFDA3293B, 0xDB41FBB9, 0xE86CE16F, 0xA94D8587, 0xA6D2E6D1, 0xCA179FC9, 0xDC9D87BB,
		0xC14D4C3D, 0xF58C4C35, 0xB30ECEC3, 0xC4B12B3F, 0xE72A5A97, 0xF8CCB713, 0xF2406667, 0xE0FC83A3,
		0xCC8EAA37, 0x87668A63, 0xFC988415, 0xCC0B2619, 0xBE0EF94B, 0xC75DEE2F, 0xFBDF3ED3, 0xF176FD55,
		0xA8FFC28D, 0x83887061, 0xCD0A47AD, 0xB672ADC5, 0xFAFF47A5, 0xA85E7F21, 0xCAA1ED55, 0xE9E5B3B7,
		0x888A3D55, 0xCCFED55F, 0xA3F9555B, 0x93A04925, 0xC905253F, 0xAF7525FB, 0xC427E9A9, 0xA69E3A45,
		0x84C86EE7, 0xAB058D3B, 0xB87E35EB, 0xA03786ED, 0x9AC482DB, 0xB6815DDB, 0xC2E0B9F1,
	};


	private static final int COPY_THRESHOLD = 128;
	private static final int NIL_ARRAY_SIZE = 1024;
	@SuppressWarnings({"MismatchedReadAndWriteOfArray"})
	private static final @Nullable Object[] NIL_ARRAY = new Object[NIL_ARRAY_SIZE];

	/**
	 * Not instantiable.
	 */
	private Utilities() {
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
	public static void setDefaultLoadFactor(float loadFactor) {
		defaultLoadFactor = Math.min(Math.max(loadFactor, FLOAT_ROUNDING_ERROR), 1f);
	}

	/**
	 * Gets the default load factor, meant to be used when no load factor is specified during the
	 * construction of a data structure such as a map or set. The initial value for the default
	 * load factor is 0.7.
	 *
	 * @return the default load factor, always between 0 (exclusive) and 1 (inclusive)
	 */
	public static float getDefaultLoadFactor() {
		return defaultLoadFactor;
	}

	/**
	 * Gets the default capacity for maps and sets backed by hash tables, meant to be used when no capacity is specified
	 * during the construction of a map or set. This depends on the current {@link #getDefaultLoadFactor()}, and is
	 * equivalent to the floor of {@code 64 * getDefaultLoadFactor()}, or 44 if unchanged.
	 *
	 * @return the default capacity for hash-based maps and sets, when none is given
	 */
	public static int getDefaultTableCapacity() {
		return (int) (64 * getDefaultLoadFactor());
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
	public static int tableSize(int capacity, float loadFactor) {
		if (capacity < 0) {
			throw new IllegalArgumentException("capacity must be >= 0: " + capacity);
		}
		int tableSize = 1 << -BitConversion.countLeadingZeros(Math.max(2, (int) Math.ceil(capacity / (double) loadFactor)) - 1);
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
	public static void clear(@Nullable Object[] objects) {
		clear(objects, 0, objects.length);
	}

	/**
	 * Set all {@code size} elements in {@code objects}, starting at index {@code start}, to null.
	 * This method is faster than {@link Arrays#fill} for large arrays (> 128).
	 * <br>
	 * From Apache Fury's ObjectArray class.
	 */
	public static void clear(@Nullable Object[] objects, int start, int size) {
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
	public static boolean isEqual(float a, float b) {
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
	public static boolean isEqual(float a, float b, float tolerance) {
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
	public static boolean equalsIgnoreCase(CharSequence a, CharSequence b) {
		if (a == b)
			return true;
		final int al = a.length();
		if (al != b.length())
			return false;
		for (int i = 0; i < al; i++) {
			char ac = a.charAt(i), bc = b.charAt(i);
			if (ac != bc && Casing.caseUp(ac) != Casing.caseUp(bc)) {
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
	 * @return ignoring case: 0 if the two {@code CharSequence}s are equal;
	 * a negative integer if {@code l}
	 * is lexicographically less than {@code r}; or a
	 * positive integer if {@code l} is
	 * lexicographically greater than {@code r}
	 */
	public static int compareIgnoreCase(CharSequence l, CharSequence r) {
		if (l == r)
			return 0;
		for (int i = 0, len = Math.min(l.length(), r.length()); i < len; i++) {
			char a = Casing.caseUp(l.charAt(i));
			char b = Casing.caseUp(r.charAt(i));
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
	 * converted to upper case by {@link Casing#caseUp(char)}; this should be correct for all alphabets in
	 * Unicode except Georgian. Typically, place() methods in Sets and Maps here that want case-insensitive hashing
	 * would use this with {@code (int)(longHashCodeIgnoreCase(text) >>> shift)}.
	 * <br>
	 * This is very similar to the {@link Hasher#hash64(CharSequence)} method, and shares
	 * the same constants and mum() method with Hasher.
	 *
	 * @param data a non-null CharSequence; often a String, but this has no trouble with a StringBuilder
	 * @return a long hashCode; quality should be similarly good across any bits
	 */
	public static long longHashCodeIgnoreCase(final CharSequence data) {
		return longHashCodeIgnoreCase(data, 9069147967908697017L);
	}

	/**
	 * Gets a 64-bit thoroughly-random hashCode from the given CharSequence, ignoring the case of any cased letters.
	 * Uses Water hash, which is a variant on <a href="https://github.com/vnmakarov/mum-hash">mum-hash</a> and
	 * <a href="https://github.com/wangyi-fudan/wyhash">wyhash</a>. This gets the hash as if all cased letters have been
	 * converted to upper case by {@link Casing#caseUp(char)}; this should be correct for all alphabets in
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
	public static long longHashCodeIgnoreCase(final CharSequence data, long seed) {
		final int len = data.length();
		for (int i = 3; i < len; i += 4) {
			seed = mum(
				mum(Casing.caseUp(data.charAt(i - 3)) ^ b1, Casing.caseUp(data.charAt(i - 2)) ^ b2) - seed,
				mum(Casing.caseUp(data.charAt(i - 1)) ^ b3, Casing.caseUp(data.charAt(i)) ^ b4));
		}

		switch (len & 3) {
			case 0:
				seed = mum(b1 - seed, b4 + seed);
				break;
			case 1:
				seed = mum(b5 - seed, b3 ^ Casing.caseUp(data.charAt(len - 1)));
				break;
			case 2:
				seed = mum(Casing.caseUp(data.charAt(len - 2)) - seed, b0 ^ Casing.caseUp(data.charAt(len - 1)));
				break;
			case 3:
				seed = mum(Casing.caseUp(data.charAt(len - 3)) - seed, b2 ^ Casing.caseUp(data.charAt(len - 2))) + mum(b5 ^ seed, b4 ^ Casing.caseUp(data.charAt(len - 1)));
				break;
		}
		seed = (seed ^ len) * (seed << 16 ^ b0);
		return (seed ^ (seed << 33 | seed >>> 31) ^ (seed << 19 | seed >>> 45));
	}

	/**
	 * Gets a 32-bit thoroughly-random hashCode from the given CharSequence, ignoring the case of any cased letters.
	 * Uses <a href="https://github.com/wangyi-fudan/wyhash">wyhash</a> version 4.2, but shrunk down to work on 16-bit
	 * char values instead of 64-bit long values. This gets the hash as if all cased letters have been
	 * converted to upper case by {@link Casing#caseUp(char)}; this should be correct for all alphabets in
	 * Unicode except Georgian. Typically, place() methods in Sets and Maps here that want case-insensitive hashing
	 * would use this with {@code (hashCodeIgnoreCase(text) >>> shift)}.
	 *
	 * @param data a non-null CharSequence; often a String, but this has no trouble with a StringBuilder
	 * @return an int hashCode; quality should be similarly good across any bits
	 */
	public static int hashCodeIgnoreCase(final CharSequence data) {
		return hashCodeIgnoreCase(data, 0x36299db9);
	}

	/**
	 * Gets a 32-bit thoroughly-random hashCode from the given CharSequence, ignoring the case of any cased letters.
	 * Uses <a href="https://github.com/wangyi-fudan/wyhash">wyhash</a> version 4.2, but shrunk down to work on 16-bit
	 * char values instead of 64-bit long values. This gets the hash as if all cased letters have been
	 * converted to upper case by {@link Casing#caseUp(char)}; this should be correct for all alphabets in
	 * Unicode except Georgian. Typically, place() methods in Sets and Maps here that want case-insensitive hashing
	 * would use this with {@code (hashCodeIgnoreCase(text, seed) >>> shift)}.
	 *
	 * @param data a non-null CharSequence; often a String, but this has no trouble with a StringBuilder
	 * @param seed any int; must be the same between calls if two equivalent values for {@code data} must be the same
	 * @return an int hashCode; quality should be similarly good across any bits
	 */
	public static int hashCodeIgnoreCase(final CharSequence data, int seed) {
		final int len = data.length();
		final int x = GOOD_MULTIPLIERS[(seed & 127)];
		final int y = GOOD_MULTIPLIERS[(seed >>> 7 & 127) + 128];
		final int z = GOOD_MULTIPLIERS[(seed >>> 14 & 255) + 256];
		int a, b;
		int p = 0;
		if (len <= 2) {
			if (len == 2) {
				a = Casing.caseUp(data.charAt(0));
				b = Casing.caseUp(data.charAt(1));
			} else if (len == 1) {
				a = Casing.caseUp(data.charAt(0));
				b = 0;
			} else a = b = 0;
		} else {
			int i = len;
			if (i >= 6) {
				int see1 = seed, see2 = seed;
				do {
					seed = BitConversion.imul(Casing.caseUp(data.charAt(p)) ^ x, Casing.caseUp(data.charAt(p + 1)) ^ seed);
					seed ^= (seed << 3 | seed >>> 29) ^ (seed << 24 | seed >>> 8);
					see1 = BitConversion.imul(Casing.caseUp(data.charAt(p + 2)) ^ y, Casing.caseUp(data.charAt(p + 3)) ^ see1);
					see1 ^= (see1 << 21 | see1 >>> 11) ^ (see1 << 15 | see1 >>> 19);
					see2 = BitConversion.imul(Casing.caseUp(data.charAt(p + 4)) ^ z, Casing.caseUp(data.charAt(p + 5)) ^ see2);
					see2 ^= (see2 << 26 | see2 >>> 6) ^ (see2 << 7 | see2 >>> 25);
					p += 6;
					i -= 6;
				} while (i >= 6);
				seed ^= see1 ^ see2;
			}
			while ((i > 2)) {
				seed = BitConversion.imul(Casing.caseUp(data.charAt(p)) ^ x, Casing.caseUp(data.charAt(p + 1)) ^ seed);
				seed ^= (seed << 3 | seed >>> 29) ^ (seed << 24 | seed >>> 8);
				i -= 2;
				p += 2;
			}
			a = Casing.caseUp(data.charAt(len - 2));
			b = Casing.caseUp(data.charAt(len - 1));
		}
		a = BitConversion.imul(a, z);
		b ^= seed + len;
		b = (b << 3 | b >>> 29) ^ (a = (a << 24 | a >>> 8) + b ^ y) + (a << 7 | a >>> 25);
		a = (a << 14 | a >>> 18) ^ (b = (b << 29 | b >>> 3) + a ^ x) + (b << 11 | b >>> 21);
		// I don't know if we need this level of robust mixing.
//		b=(b<<19|b>>>13)^(a=(a<< 5|a>>>27)+b^y)+(a<<29|a>>> 3);
//		a=(a<<17|a>>>15)^(b=(b<<11|b>>>21)+a^z)+(b<<23|b>>> 9);
		return a ^ (a << 27 | a >>> 5) ^ (a << 9 | a >>> 23);
	}
}

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
	 * A final array of 512 int multipliers that have been tested to work on at least some large
	 * input sets without excessively high collision rates. The initial value passed to a
	 * {@link ObjectSet#setHashMultiplier(int)} method (on any hashed data structure here) is used
	 * to choose one of these based on the int parameter (it also uses the current shift value).
	 * All hashed data structures here currently start with a multiplier of 0x00137B2F, which is not
	 * in this array by default, but is still a pretty good multiplier. The numbers here all use the
	 * same range of the low 21 bits, because multiplying an arbitrary {@code int} by a larger value
	 * can lose precision on GWT unless something like {@link BitConversion#imul(int, int)} is used.
	 * Since it isn't clear how much speed penalty imul() incurs, sticking with smaller multipliers
	 * and avoiding imul() may help performance.
	 * <br>
	 * You can mutate this array, but you should only do so if you encounter high collision rates or
	 * resizes with a particular multiplier from this table. Any int m you set into this array must
	 * satisfy {@code (m & 0x00100001) == 0x00100001}, and should ideally have an "unpredictable" bit
	 * pattern. This last quality is the hardest to define, but
	 * generally dividing 2 to the 22 by an irrational number (that is greater than 1) using BigDecimal
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
	public static final int[] GOOD_MULTIPLIERS = {
			0x00110427, 0x00144057, 0x001AFB2F, 0x001F1753, 0x00135205, 0x00176C45, 0x001E3A15, 0x001F406D,
			0x001DEF1D, 0x0018BD49, 0x001DE7A9, 0x00117949, 0x001BDC1D, 0x00190A37, 0x0014A839, 0x00108EB9,
			0x0019EB97, 0x0014A6B7, 0x001B3283, 0x001F890F, 0x001502ED, 0x00197DB1, 0x001A2447, 0x001F9159,
			0x001C8F59, 0x00115359, 0x00163683, 0x00121771, 0x001FC839, 0x001D782D, 0x0010B147, 0x0017A481,
			0x00146285, 0x001CDEFD, 0x001853BF, 0x001F1921, 0x001CE9E7, 0x001A3CF7, 0x001D6E03, 0x00192469,
			0x001746EF, 0x0013A80F, 0x00104C7B, 0x0012B54B, 0x00126351, 0x0010B375, 0x0014D59D, 0x00123A37,
			0x0015F383, 0x00134417, 0x0017E00B, 0x0013CDC5, 0x0016141B, 0x001FE2B7, 0x00199397, 0x00188BF9,
			0x0010BA01, 0x00145F47, 0x00197749, 0x001F55DF, 0x00128BA9, 0x001871B3, 0x0016B5DF, 0x00144577,
			0x0013FEDB, 0x001EA4FB, 0x0015E779, 0x001EA353, 0x001CFB85, 0x001B66E9, 0x001DDD59, 0x001493AD,
			0x0017EBF1, 0x00105079, 0x001D51C9, 0x0014BDFB, 0x001D41AD, 0x001B85D9, 0x0015E173, 0x00196513,
			0x001F7329, 0x00175EB9, 0x00199109, 0x0018EE5D, 0x0013C2EF, 0x00137FA3, 0x0011DE3B, 0x00167B03,
			0x00186BAF, 0x00174413, 0x00115527, 0x00155B3B, 0x00192E1F, 0x001262F1, 0x0015C417, 0x0016BC6D,
			0x0012B161, 0x0016BF83, 0x001FD2AF, 0x00164C1D, 0x001FC9CD, 0x001B0351, 0x0015C3DD, 0x0019A535,
			0x001C87D1, 0x001A5EFD, 0x001796CD, 0x001F8FFD, 0x00155AC7, 0x001B61A3, 0x0016A8C7, 0x0014598F,
			0x001C2379, 0x0013CAE9, 0x001711A3, 0x001A3051, 0x001207DB, 0x00177419, 0x0015682D, 0x00197E5D,
			0x001E71A3, 0x001FF6F7, 0x001D41F9, 0x001304B5, 0x001DAF93, 0x0010B893, 0x001D4F5F, 0x0011571B,
			0x00146829, 0x00127DE3, 0x001DFAB3, 0x0015D6FB, 0x0014C823, 0x00118E35, 0x0011FFF3, 0x00163C87,
			0x001EEAC1, 0x001774D9, 0x00178F45, 0x001A1355, 0x00163055, 0x001406AD, 0x001F837D, 0x001D7791,
			0x00132189, 0x001AAF61, 0x001A5DA1, 0x00195339, 0x001C0959, 0x00118555, 0x001F5089, 0x0014F9AD,
			0x0017AD03, 0x00149B21, 0x0015A77D, 0x0019598F, 0x001399E9, 0x0015F519, 0x0017B019, 0x0016DFCF,
			0x001E3727, 0x0014E715, 0x001E00A7, 0x001D2923, 0x0019DA5B, 0x001E999D, 0x001692CD, 0x0011675F,
			0x00154251, 0x001E1FD1, 0x001CA0E3, 0x00104C8F, 0x00172AEF, 0x001FB11D, 0x0011C82D, 0x00156639,
			0x0019C547, 0x001313B1, 0x00111491, 0x001B2013, 0x001C9161, 0x00174255, 0x001B9E9D, 0x00136EED,
			0x00180BB5, 0x0015CA1D, 0x001011B1, 0x001D4F13, 0x00167571, 0x0014C73D, 0x0013CE13, 0x0018AEFB,
			0x001AA60D, 0x0010B4F7, 0x001177A1, 0x001CB051, 0x001AD93D, 0x0011EE1D, 0x0014AAEF, 0x00156D99,
			0x00118A99, 0x001D4AB5, 0x0019386F, 0x001A6671, 0x001BB619, 0x0016AC51, 0x001A9B49, 0x001405C7,
			0x001F8297, 0x001FAAA3, 0x00165A91, 0x00198B9D, 0x001FAE8D, 0x001A1161, 0x001BCC61, 0x001289EF,
			0x0016359B, 0x0014A90D, 0x00116F1F, 0x001D1327, 0x00195907, 0x0010D205, 0x00160305, 0x00103CF9,
			0x001E52B3, 0x001531E7, 0x0018214F, 0x0018BA45, 0x001224C3, 0x00172017, 0x0016E997, 0x001E11A9,
			0x0018B621, 0x001C0415, 0x001FBD61, 0x0018F233, 0x001DFB27, 0x00149CA1, 0x0015727D, 0x001EDCFB,
			0x00137A97, 0x0010470F, 0x00193EFB, 0x00186D09, 0x001D5457, 0x001FF939, 0x001A125B, 0x001FC2B9,
			0x001DFED7, 0x0010E173, 0x001D8C45, 0x001A5F23, 0x001130B7, 0x001E7627, 0x001FFB7B, 0x00117FFB,
			0x001CE8C5, 0x00194911, 0x001C755F, 0x001FA005, 0x001AB7E3, 0x001B2267, 0x0015E959, 0x0011E587,
			0x0013A087, 0x0013E2FF, 0x001DEE81, 0x001E9C51, 0x0017582B, 0x001A987F, 0x0013110D, 0x00139D37,
			0x0013E6E9, 0x00146573, 0x00150CDD, 0x001A6D23, 0x00173335, 0x001519A9, 0x0012AF31, 0x0011BC6D,
			0x0012208B, 0x0015E777, 0x001D9D5B, 0x0010B5A3, 0x001D16C3, 0x001D747B, 0x001BAB07, 0x00110B4D,
			0x00169F97, 0x001D9863, 0x0019A897, 0x00117281, 0x001171AD, 0x001CFC1D, 0x0017A8A3, 0x001E22E5,
			0x0017FF21, 0x001BBED3, 0x00171397, 0x00141705, 0x001764F9, 0x001FD64D, 0x001E575F, 0x001AC54B,
			0x00184525, 0x00167C85, 0x001D0467, 0x0014849F, 0x00142D4D, 0x001E466F, 0x001CF5F3, 0x0012BB2B,
			0x00177A6D, 0x001F739D, 0x001E1CBB, 0x00110B4F, 0x001CCA97, 0x001A7A8B, 0x001EE27B, 0x001F10ED,
			0x0015E8E7, 0x00127213, 0x001FA37D, 0x001A5CCF, 0x00174AED, 0x0013CDB3, 0x001D0285, 0x00160E77,
			0x0012839D, 0x0019D48F, 0x00175D4B, 0x001EDD83, 0x001D28E9, 0x0019CD55, 0x0018A5B9, 0x001890DF,
			0x0011AA71, 0x001F5B39, 0x00161E59, 0x00126B73, 0x0019F94B, 0x001EFB05, 0x0018D0DB, 0x00161CB1,
			0x00172839, 0x0016A807, 0x0016DDB3, 0x001C29F3, 0x00130927, 0x00110933, 0x001D48AD, 0x001D771F,
			0x0015F46B, 0x0012F029, 0x001D0FB1, 0x001F2203, 0x0019C823, 0x001D3083, 0x0014D7F3, 0x0010980F,
			0x0012F39D, 0x0010973B, 0x001FE897, 0x001646C5, 0x0016B883, 0x00132743, 0x001BC7DD, 0x00177A59,
			0x00125625, 0x00102159, 0x00198BD7, 0x001D5929, 0x0012DB15, 0x0013F511, 0x00120391, 0x00139CEB,
			0x001DD079, 0x001C14A5, 0x00199D61, 0x0016AD25, 0x00189031, 0x00108961, 0x0012E565, 0x001C1FC9,
			0x00165357, 0x001036CD, 0x001CBFF9, 0x001D1677, 0x00111F07, 0x0016F10B, 0x00135FCB, 0x001039E3,
			0x0011AB31, 0x0018E81D, 0x001C5E1D, 0x0015E307, 0x001F24A5, 0x00133BA9, 0x001CBA2D, 0x0018AFF5,
			0x00110C6F, 0x00194F51, 0x001F1489, 0x0010BB83, 0x0011C7DF, 0x0018AD79, 0x001DC40D, 0x00182C73,
			0x001152D1, 0x00189D5D, 0x00135DE9, 0x0019E5CB, 0x0012D751, 0x00107A79, 0x001CFC6B, 0x0017874B,
			0x001673B5, 0x001BFC07, 0x00134693, 0x001CCB7D, 0x0012FC0D, 0x001084C9, 0x00109195, 0x0017C81B,
			0x001436DB, 0x00117511, 0x001B43AD, 0x0014A08B, 0x00110E77, 0x00198705, 0x001BF0A9, 0x0015B1A5,
			0x00147C69, 0x0011A699, 0x001283AF, 0x00192D37, 0x001A258D, 0x001E7977, 0x0016DDFF, 0x001B6795,
			0x00182DA7, 0x001FADDF, 0x0017708F, 0x0010CD6D, 0x001D6439, 0x001929E7, 0x0017A3BF, 0x001F8F4F,
			0x0012DD43, 0x0016A42F, 0x0019D07D, 0x0014FC61, 0x00103C4B, 0x00193B71, 0x001515F9, 0x001FD01F,
			0x001AABEB, 0x001D4B3B, 0x00163CC1, 0x001FFDBD, 0x00162D79, 0x001EBA0D, 0x001FDA6F, 0x00108105,
			0x0011F17F, 0x00108697, 0x00102E71, 0x001B618F, 0x001FFF2B, 0x001366B7, 0x0019D359, 0x001A0F6B,
			0x001790ED, 0x001CC927, 0x001E68E7, 0x0011B6DB, 0x0015E91F, 0x00155B4D, 0x00137107, 0x0011E479,
			0x001E5339, 0x0011A12D, 0x0012F0D5, 0x0011C939, 0x001F87A1, 0x001CEEB7, 0x001DAFB9, 0x00199E47,
			0x00162773, 0x00138E89, 0x001C365D, 0x001619D3, 0x001D56BF, 0x001405D9, 0x001D39D7, 0x00185F55,
			0x00181C09, 0x0010C3DD, 0x0010D7E3, 0x00109497, 0x0018B4FF, 0x00112CB9, 0x001FBBE1, 0x001D05F9,
			0x0018F571, 0x0011D957, 0x001CD6C9, 0x001D12DB, 0x001E982F, 0x001F2B93, 0x0015EF87, 0x0018A2DD,
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

	/**
	 * Gets a 32-bit thoroughly-random hashCode from the given CharSequence, ignoring the case of any cased letters.
	 * Uses <a href="https://github.com/wangyi-fudan/wyhash">wyhash</a> version 4.2, but shrunk down to work on 16-bit
	 * char values instead of 64-bit long values. This gets the hash as if all cased letters have been
	 * converted to upper case by {@link Character#toUpperCase(char)}; this should be correct for all alphabets in
	 * Unicode except Georgian. Typically, place() methods in Sets and Maps here that want case-insensitive hashing
	 * would use this with {@code (hashCodeIgnoreCase(text) >>> shift)}.
	 *
	 * @param data a non-null CharSequence; often a String, but this has no trouble with a StringBuilder
	 * @return an int hashCode; quality should be similarly good across any bits
	 */
	public static int hashCodeIgnoreCase (final CharSequence data) {
		return hashCodeIgnoreCase(data, 908697017);
	}

	/**
	 * Gets a 32-bit thoroughly-random hashCode from the given CharSequence, ignoring the case of any cased letters.
	 * UUses <a href="https://github.com/wangyi-fudan/wyhash">wyhash</a> version 4.2, but shrunk down to work on 16-bit
	 * char values instead of 64-bit long values. This gets the hash as if all cased letters have been
	 * converted to upper case by {@link Character#toUpperCase(char)}; this should be correct for all alphabets in
	 * Unicode except Georgian. Typically, place() methods in Sets and Maps here that want case-insensitive hashing
	 * would use this with {@code (hashCodeIgnoreCase(text, seed) >>> shift)}.
	 *
	 * @param data a non-null CharSequence; often a String, but this has no trouble with a StringBuilder
	 * @param seed any int; must be the same between calls if two equivalent values for {@code data} must be the same
	 * @return an int hashCode; quality should be similarly good across any bits
	 */
	public static int hashCodeIgnoreCase (final CharSequence data, int seed) {
		if(data == null) return 0;
		final int len = data.length();
		int b0 = GOOD_MULTIPLIERS[(seed & 127)];
		int b1 = GOOD_MULTIPLIERS[(seed >>>  8 & 127)+128];
		int b2 = GOOD_MULTIPLIERS[(seed >>> 16 & 127)+256];
		int b3 = GOOD_MULTIPLIERS[(seed >>> 24 & 127)+384];
		int a, b;
		int p = 0;
		if(len<=2){
			if(len==2){ a=Character.toUpperCase(data.charAt(0)); b=Character.toUpperCase(data.charAt(1)); }
			else if(len==1){ a=Character.toUpperCase(data.charAt(0)); b=0;}
			else a=b=0;
		}
		else{
			int i=len;
			if(i>=6){
				int see1=seed, see2=seed;
				do{
					seed=(Character.toUpperCase(data.charAt(p  ))^b1)*(Character.toUpperCase(data.charAt(p+1))^seed);seed^=(seed<< 3|seed>>>29)^(seed<<24|seed>>> 8);
					see1=(Character.toUpperCase(data.charAt(p+2))^b2)*(Character.toUpperCase(data.charAt(p+3))^see1);see1^=(see1<<21|see1>>>11)^(see1<<15|see1>>>19);
					see2=(Character.toUpperCase(data.charAt(p+4))^b3)*(Character.toUpperCase(data.charAt(p+5))^see2);see2^=(see2<<26|see2>>> 6)^(see2<< 7|see2>>>25);
					p+=6;i-=6;
				}while(i>=6);
				seed^=see1^see2;
			}
			while((i>2)){
				seed=(Character.toUpperCase(data.charAt(p  ))^b1)*(Character.toUpperCase(data.charAt(p+1))^seed);seed^=(seed<< 3|seed>>>29)^(seed<<24|seed>>> 8);
				i-=2; p+=2;
			}
			a=Character.toUpperCase(data.charAt(len-2));
			b=Character.toUpperCase(data.charAt(len-1));
		}
		a^=b1;
		b^=seed;
		p=a*b;
		a=p^p>>>11^p>>>21^b0^len;
		b=p^(p<<29|p>>> 3)^(p<< 8|p>>>24)^b1;
		p=BitConversion.imul(a, b);
		return p^(p<<29|p>>> 3)^(p<< 8|p>>>24);
	}
}

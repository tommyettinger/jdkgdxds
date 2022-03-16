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

package com.github.tommyettinger.ds;

/**
 * Utility code shared by various data structures in this package.
 *
 * @author Tommy Ettinger
 */
public class Utilities {
	private static float defaultLoadFactor = 0.7f;

	/**
	 * Sets the load factor that will be used when none is specified during construction (for
	 * data structures that have a load factor, such as all sets and maps here). The load factor
	 * will be clamped so it is greater than 0 (the lowest possible is {@link #FLOAT_ROUNDING_ERROR},
	 * but it should never actually be that low) and less than or equal to 1. The initial value for
	 * the default load factor is 0.7.
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
		int tableSize = 1 << -Integer.numberOfLeadingZeros(Math.max(2, (int)Math.ceil(capacity / loadFactor)) - 1);
		if (tableSize > 1 << 30 || tableSize < 0) {
			throw new IllegalArgumentException("The required capacity is too large: " + capacity);
		}
		return tableSize;
	}

	static final Object neverIdentical = new Object();

	/**
	 * A float that is meant to be used as the smallest reasonable tolerance for methods like {@link #isEqual(float, float, float)}.
	 */
	public static final float FLOAT_ROUNDING_ERROR = 0.000001f;

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
	 * Big constant 0, used by {@link #longHashCodeIgnoreCase(CharSequence)}.
	 */
	protected static final long b0 = 0xA0761D6478BD642FL;
	/**
	 * Big constant 1, used by {@link #longHashCodeIgnoreCase(CharSequence)}.
	 */
	protected static final long b1 = 0xE7037ED1A0B428DBL;
	/**
	 * Big constant 2, used by {@link #longHashCodeIgnoreCase(CharSequence)}.
	 */
	protected static final long b2 = 0x8EBC6AF09C88C6E3L;
	/**
	 * Big constant 3, used by {@link #longHashCodeIgnoreCase(CharSequence)}.
	 */
	protected static final long b3 = 0x589965CC75374CC3L;
	/**
	 * Big constant 4, used by {@link #longHashCodeIgnoreCase(CharSequence)}.
	 */
	protected static final long b4 = 0x1D8E4E27C47D124FL;

	/**
	 * Part of the hashing function used by {@link #longHashCodeIgnoreCase(CharSequence)}.
	 * <br>
	 * Takes two arguments that are technically longs, and should be very different, and uses them to get a result
	 * that is technically a long and mixes the bits of the inputs. The arguments and result are only technically
	 * longs because their lower 32 bits matter much more than their upper 32, and giving just any long won't work.
	 * <br>
	 * This is very similar to wyhash's mum function, but doesn't use 128-bit math because it expects that its
	 * arguments are only relevant in their lower 32 bits (allowing their product to fit in 64 bits).
	 *
	 * @param a a long that should probably only hold an int's worth of data
	 * @param b a long that should probably only hold an int's worth of data
	 * @return a sort-of randomized output dependent on both inputs
	 */
	protected static long mum (final long a, final long b) {
		final long n = a * b;
		return n - (n >>> 32);
	}

	/**
	 * Gets a 64-bit thoroughly-random hashCode from the given CharSequence, ignoring the case of any cased letters.
	 * Uses Water hash, which is a variant on <a href="https://github.com/vnmakarov/mum-hash">mum-hash</a> and
	 * <a href="https://github.com/wangyi-fudan/wyhash">wyhash</a>. This gets the hash as if all cased letters have been
	 * converted to upper case by {@link Character#toUpperCase(char)}; this should be correct for all alphabets in
	 * Unicode except Georgian. Typically place() methods in Sets and Maps here that want case-insensitive hashing
	 * would use this with {@code (int)(longHashCodeIgnoreCase(text) >>> shift)}.
	 *
	 * @param item a non-null CharSequence; often be a String, but this has no trouble with a StringBuilder
	 * @return a long hashCode; quality should be similarly good across any bits
	 */
	public static long longHashCodeIgnoreCase (final CharSequence item) {
		long seed = 9069147967908697017L;
		final int len = item.length();
		for (int i = 3; i < len; i += 4) {
			seed = mum(mum(Character.toUpperCase(item.charAt(i - 3)) ^ b1, Character.toUpperCase(item.charAt(i - 2)) ^ b2) + seed, mum(Character.toUpperCase(item.charAt(i - 1)) ^ b3, Character.toUpperCase(item.charAt(i)) ^ b4));
		}
		switch (len & 3) {
		case 0:
			seed = mum(b1 ^ seed, b4 + seed);
			break;
		case 1:
			seed = mum(seed ^ b3, b4 ^ Character.toUpperCase(item.charAt(len - 1)));
			break;
		case 2:
			seed = mum(seed ^ Character.toUpperCase(item.charAt(len - 2)), b3 ^ Character.toUpperCase(item.charAt(len - 1)));
			break;
		case 3:
			seed = mum(seed ^ Character.toUpperCase(item.charAt(len - 3)) ^ (long)Character.toUpperCase(item.charAt(len - 2)) << 16, b1 ^ Character.toUpperCase(item.charAt(len - 1)));
			break;
		}
		seed = (seed ^ seed << 16) * (len ^ b0);
		return seed - (seed >>> 31) + (seed << 33);
	}

}

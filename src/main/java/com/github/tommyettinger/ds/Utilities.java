/*******************************************************************************
 * Copyright 2020 See AUTHORS file.
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
 ******************************************************************************/

package com.github.tommyettinger.ds;

/**
 * Utility code shared by various data structures in this package.
 *
 * @author Tommy Ettinger
 */
public class Utilities {
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
		if (capacity < 0) { throw new IllegalArgumentException("capacity must be >= 0: " + capacity); }
		int tableSize = 1 << -Integer.numberOfLeadingZeros(Math.max(2, (int)Math.ceil(capacity / loadFactor)) - 1);
		if (tableSize > 1 << 30 || tableSize < 0) { throw new IllegalArgumentException("The required capacity is too large: " + capacity); }
		return tableSize;
	}

	static final Object neverIdentical = new Object();

	/**
	 * A simple equality comparison for {@link CharSequence} values such as {@link String}s or {@link StringBuilder}s
	 * that ignores case by upper-casing any cased letters. This works for all alphabets in Unicode except Georgian.
	 * @param a a non-null CharSequence, such as a String or StringBuilder
	 * @param b a non-null CharSequence, such as a String or StringBuilder
	 * @return whether the contents of {@code a} and {@code b} are equal ignoring case
	 */
	public static boolean equalsIgnoreCase(CharSequence a, CharSequence b) {
		if(a == b) return true;
		final int al = a.length();
		if(al != b.length()) return false;
		for (int i = 0; i < al; i++) {
			char ac = a.charAt(i), bc = b.charAt(i);
			if(ac != bc && Character.toUpperCase(ac) != Character.toUpperCase(bc)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Gets a 64-bit thoroughly-random hashCode from the given CharSequence, ignoring the case of any cased letters.
	 * Uses Frost hash, but doesn't fully finish the hashing because some usage will need some variable amount of the
	 * high bits (which should be somewhat higher-quality). This gets the hash as if all cased letters have been
	 * converted to upper case by {@link Character#toUpperCase(char)}; this should be correct for all alphabets in
	 * Unicode except Georgian. Typically place() methods in Sets and Maps here that want case-insensitive hashing
	 * would use this with {@code (int)(longHashCodeIgnoreCase(text) >>> shift)}.
	 * @param item a non-null CharSequence; often be a String, but this has no trouble with a StringBuilder
	 * @return a long hashCode that has higher-quality upper bits (shift right if you need fewer than 64 bits)
	 */
	public static long longHashCodeIgnoreCase(CharSequence item) {
		final int len = item.length();
		if(len == 0) return 0;
		long h = len ^ 0xC6BC279692B5C323L, m = 0xDB4F0B9175AE2165L, t, r;
		for (int i = 0; i < len; i++) {
			t = (0x3C79AC492BA7B653L + Character.toUpperCase(item.charAt(i))) * m;
			r = (m += 0x95B534A1ACCD52DAL) >>> 58;
			h ^= t << r | t >>> -r;
		}
		// Pelican unary hash, with a different last step that can adapt to different shift values.
		h = (h ^ (h << 41 | h >>> 23) ^ (h << 17 | h >>> 47) ^ 0xD1B54A32D192ED03L) * 0xAEF17502108EF2D9L;
		return (h ^ h >>> 43 ^ h >>> 31 ^ h >>> 23) * 0xDB4F0B9175AE2165L;
	}
}

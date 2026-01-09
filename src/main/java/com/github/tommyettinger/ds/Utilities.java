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
import com.github.tommyettinger.digital.MathTools;

import java.util.Arrays;

import static com.github.tommyettinger.digital.Hasher.*;

/**
 * Utility code shared by various data structures in this package.
 *
 * @author Tommy Ettinger
 */
public final class Utilities {
	/**
	 * A final array of 32 int multipliers that have shown to have few collisions in some kinds of
	 * hash table. These multipliers are used within jdkgdxds in a fixed order as the hash multiplier
	 * to mix each hashCode in place() for most maps and sets. The multiplier changes each time the
	 * hash table's size increases sufficiently; more specifically, whenever the variable
	 * {@code shift} changes.
	 * <br>
	 * You can mutate this array, but you should only do so if you encounter high collision rates or
	 * resizes with a particular multiplier from this table. Any int you set into this array must
	 * be an odd number, and ideally should be produced by {@link #optimizeMultiplier(int, int)}
	 * with a threshold of 1.
	 * <br>
	 * This is a small subset of {@link #GOOD_MULTIPLIERS}, which stores 512 int multipliers instead
	 * of just 32 here. The full table of 512 GOOD_MULTIPLIERS is only needed in certain places, and
	 * 32 ints may be more amenable to fitting in a processor cache.
	 */
	public static final int[] HASH_MULTIPLIERS = new int[]{
		0xF0CFFC71, 0x1F2BD44D, 0xC143F257, 0x115583CF, 0xA4613217, 0x9645256D, 0xF0EE34C7, 0xF6C4F429,
		0x854066D1, 0x9104FADF, 0x93A04925, 0x0AEF4329, 0xF58C4C35, 0x31A6C2EB, 0xE5182F73, 0xB26FABE5,
		0xB520960B, 0x570B3F85, 0x83657DE7, 0x9980FAB9, 0x2F299C91, 0xB423727D, 0xAA333A2D, 0x8AE04AAD,
		0x288FAD91, 0xD90AC247, 0xC5F768E7, 0x92317571, 0xD5FA5B15, 0xDB6B35F7, 0xCC8965C7, 0xE0503E4F,
	};

	/**
	 * A final array of 512 int multipliers that have shown to have few collisions in some kinds of
	 * hash table. These multipliers are used within jdkgdxds in the CaseInsensitive maps and sets
	 * and by {@link #hashCodeIgnoreCase(CharSequence, int)}.
	 * <br>
	 * You can mutate this array, but you should only do so if you encounter high collision rates or
	 * resizes with a particular multiplier from this table. Any int you set into this array must
	 * be an odd number, and ideally should be produced by {@link #optimizeMultiplier(int, int)}
	 * with a threshold of 1.
	 * <br>
	 * The specific numbers in this array have been changed a few times, and they have also gone from
	 * {@code long} to {@code int}. The current set was drawn from a larger set of candidates and also
	 * evaluated using {@link #optimizeMultiplier(int, int)} as a tool for most of them.
	 */
	public static final int[] GOOD_MULTIPLIERS = new int[]{
		0xF0CFFC71, 0x1F2BD44D, 0xC143F257, 0x115583CF, 0xA4613217, 0x9645256D, 0xF0EE34C7, 0xF6C4F429,
		0x854066D1, 0x9104FADF, 0x93A04925, 0x0AEF4329, 0xF58C4C35, 0x31A6C2EB, 0xE5182F73, 0xB26FABE5,
		0xB520960B, 0x570B3F85, 0x83657DE7, 0x9980FAB9, 0x2F299C91, 0xB423727D, 0xAA333A2D, 0x8AE04AAD,
		0x288FAD91, 0xD90AC247, 0xC5F768E7, 0x92317571, 0xD5FA5B15, 0xDB6B35F7, 0xCC8965C7, 0xE0503E4F,
		0xFC92BDFB, 0xBCA43B89, 0x60BB491B, 0xE4E04137, 0xCCFC4B61, 0x9DCAF41B, 0xC2F8BBC7, 0x844BAB95,
		0xFF54D7E7, 0xE2061CD3, 0xB76FD153, 0x9E55CBF3, 0x8EB221CD, 0x614C5CFD, 0xEC990779, 0xFB73BC8B,
		0x9E01675D, 0xDF829389, 0x1B7E3415, 0xE9E5B3B7, 0xA8FA9803, 0x90C42FAF, 0xF28FB701, 0xB79FD4FF,
		0x4B85C9CB, 0xC3A1CBF5, 0x8009F5F3, 0x8D32BB1B, 0xB4422C7D, 0xDC92F983, 0xA25AF703, 0x815F1DD9,
		0xEE138EB9, 0xFA0C22AB, 0xF43AAD83, 0xFA9B0143, 0xE37B5231, 0x8C4C960F, 0xF9FBF639, 0x6A3DC2CD,
		0xB5A7B713, 0x15C17F29, 0xDE086517, 0x0D379137, 0xC6A39455, 0xA5C4151F, 0x65868CA7, 0x3CF16429,
		0x978F92ED, 0xDB41FBB9, 0xE993C987, 0x35E8E8A3, 0x0936779D, 0xB1A10705, 0x1C543763, 0xCBAE03A7,
		0xF0CE6ED1, 0x91F12ED1, 0xD0CDC9B1, 0xD80DB433, 0xCEA405C7, 0x7F8A46A5, 0x4B8E319B, 0xD207F62D,
		0xC32D0D1D, 0x8E4EA9ED, 0xC33AFB2F, 0x937CD501, 0xC7AA1AE7, 0x5A052EF9, 0x66C1D617, 0x247D170B,
		0x8FA69905, 0xBFA927CB, 0x85C8ADB5, 0x4A4C196F, 0xAF2D17D7, 0x2F6C35C5, 0x5B82BDE1, 0xA2DFA303,
		0xFCC13CA9, 0xA562FE85, 0xE881C805, 0xC8F0EC99, 0x3A3DF0A5, 0x8CF7D80F, 0xF9EA02B5, 0x8DAF8FB1,
		0x458F6D71, 0xB07D3A2D, 0x77480EEB, 0xEBE28BC7, 0xB7D7B6B1, 0xE72A6339, 0xE1126CDF, 0x21A1BAE9,
		0x2CED1987, 0xB784AF47, 0xCB0959AB, 0x94A32A2B, 0xD0DE6BBD, 0xC5AA2797, 0x8C515033, 0x8C2A253F,
		0x86F7E439, 0x9296AFF5, 0xB672ADC5, 0xAFA63E3F, 0xC335F929, 0xEA326ABB, 0xC94E3F59, 0xE018F82D,
		0x888A3D55, 0xD963E9A1, 0x9E01B893, 0x50D33F41, 0x437B98C5, 0xA49D3AFF, 0xE35C86ED, 0xC0A7D057,
		0xC2A74F31, 0xA03E58D5, 0x758D8E27, 0xE6245E51, 0xF3DEC4BF, 0xC076504F, 0xCD0A47AD, 0xA979EC49,
		0xE7D8B7D7, 0xD48BF7C7, 0xDC0CFB0D, 0x83D18A7B, 0xE609C2E5, 0x4F942E43, 0xE4FC8857, 0x8DD33671,
		0x70F295D1, 0x9A844F27, 0x372BD143, 0xC79860D5, 0xB56A208B, 0xD8F50D87, 0x7E171315, 0xE0CC8899,
		0xC1188E6D, 0x7A49B039, 0x7F28885B, 0xC88E2807, 0xAA19AAB7, 0xA42433BF, 0xA8D481A7, 0x98EFE1B5,
		0xB1709A3B, 0x3A7D0AC9, 0xDCA1C8D7, 0xEC34F7A1, 0x8BDCE3EF, 0xCE51173D, 0x269473B7, 0x9BE82D2B,
		0xD83C14C5, 0xFCE635CB, 0xC2EEC5D5, 0x30E72577, 0x2DA9833D, 0x82E28415, 0x84487F11, 0xBF6C9497,
		0x07E95971, 0xED2C4799, 0xE39ECEF7, 0x9AC1679B, 0xA1C2516F, 0xADCBDFA1, 0x752D9E21, 0xDEC21B69,
		0x90738567, 0xFFD4C7BD, 0x9BA429C3, 0xB7F3D71B, 0xD3C8C535, 0x4AF8364D, 0xFAEFB6C3, 0xA5DE22F3,
		0xD0CC2DA3, 0x64BF161D, 0x674634E7, 0x560BCF69, 0x872C5C8D, 0xCB39CDBD, 0xB30ECEC3, 0xE208D04F,
		0x1771C695, 0xA68C7791, 0xAC612BF1, 0x645A39A7, 0xE8F1FBCF, 0xA471E313, 0xFD890F79, 0xCA3D45BF,
		0xF101965F, 0xB965E897, 0x8BD6AD6F, 0x8A2E6A69, 0xDA6C84C9, 0xE4F8CD59, 0x17EA1CCB, 0x847FE7BB,
		0xC2D190D3, 0xA8612159, 0xD0CDB4FF, 0xC62F2887, 0xDB65787F, 0x8BC825F9, 0x1737AC19, 0xC6B71D41,
		0xE438BEA9, 0x24AE0D67, 0xABDF22BF, 0xFAB9E45B, 0xB5511B61, 0x2682D739, 0xC788231F, 0xC8FF67C7,
		0xFC5B4507, 0x89C7165B, 0xF16A6DCD, 0xC1CC9749, 0xCC211003, 0x5489C2BF, 0xE607294F, 0xA1F6EA0B,
		0x6A878DA1, 0xC17DC8F1, 0x8AEDE62B, 0x4386ED2F, 0x54138AFD, 0xD0ED94DB, 0x87F2D139, 0xA26FD4F7,
		0x95E6BCF9, 0x9A3AB6F9, 0x75A6CE23, 0xC1CB8BEB, 0x4732C927, 0x2B3413D9, 0xC0D86B95, 0xCD3E83D3,
		0xC1ADCC9D, 0x6D480B97, 0x9BE73B1B, 0x72E9D8C1, 0x3A282E7B, 0xCE810209, 0xF3AEC9B5, 0xF1D565C9,
		0x88663643, 0xCFCBE891, 0x5DEA5C13, 0xBF03ABE5, 0xE8738C4D, 0x78C9A5E1, 0xC5414787, 0x7222B9DB,
		0x91F6787F, 0xEF3AF87B, 0xC8E7F03D, 0xF458E5D9, 0xF288FED7, 0xF98DDA5B, 0xC0933DB3, 0xEDEAD625,
		0xF9E0C7DF, 0x8699F4CD, 0xBA7B6B01, 0x63A95E87, 0xF55C20BB, 0xA49BFA61, 0xCCF4E271, 0x8F531893,
		0x1B15AC79, 0xA2E2229F, 0xB26623D3, 0xACABAFB3, 0x251B0779, 0xC2F9C7BD, 0x9AD331F1, 0xE213ACCB,
		0xA30FAD43, 0xF3B16263, 0xCCFED55F, 0xCFB5FEA7, 0x51AE4FC5, 0xA4E2AF8D, 0x82C39AB7, 0x843EAE93,
		0x547CE861, 0x68A8C7F3, 0x22FC451B, 0xCA1659CF, 0x8CD41DE7, 0xD68F0657, 0xAF439D51, 0xDF391243,
		0xECE5E5D5, 0x9B7E21D1, 0x83887061, 0xDC95806B, 0x47B092EB, 0xD063D19B, 0xDF3909A1, 0xB3A06767,
		0xBBEB6F4F, 0xDAF09BD5, 0xAB8ECED1, 0xD678C827, 0xCC8EAA37, 0xBF8F0567, 0xAB08B11D, 0xEE99B50F,
		0x8CE7A2FB, 0xF1D2B243, 0xDC85CD0B, 0xA85E7F21, 0xB8580E4B, 0xF38831AF, 0xA0AF70B9, 0xD241CD23,
		0x8701C045, 0x87604B41, 0xC179176F, 0xB84DA7F3, 0x6B8D07D1, 0xAE927879, 0xB57AD749, 0x33CB249D,
		0xD866C25D, 0xC3D56C8B, 0xA495DEA3, 0x6AC394CF, 0x808498B7, 0xC21736F9, 0xF1093D73, 0xD1D3B9E7,
		0xE298BD0D, 0x978BDC1D, 0xA8FFC28D, 0x36E8CCB1, 0xFE191A7D, 0xD8279727, 0x289FF151, 0xCBC9B631,
		0x532E29E3, 0x83B4DFC5, 0xE1D8CCA3, 0xB1173B23, 0x6C66DB0B, 0x47BE5089, 0xE6F4AAB7, 0x508B85AF,
		0x2B0D237F, 0x8F3A3417, 0x91D8C35D, 0xA6989707, 0x5C1B45B3, 0x4A1DDE53, 0x2B5310FD, 0xC08D363B,
		0xC9FB76BD, 0xD30A3B83, 0xD7105CF3, 0xC5DE44E3, 0x518CF5CD, 0xB9224787, 0x83C86EB9, 0xADE3D033,
		0x71E95783, 0x8CC4FC91, 0x6D01CDAF, 0x4B9E1127, 0xA49DC54B, 0x7E869917, 0x35D834E5, 0xA9D2159B,
		0x80B85B7B, 0xB17706B9, 0x1C829FA9, 0x8DC1D40F, 0xCE0A8EC3, 0x14A4FF99, 0xD9ABF481, 0xFF570117,
		0xA6D2E6D1, 0x9D9532F7, 0x83B2BF37, 0xCED41215, 0x2547949F, 0x76188D6B, 0xCDCBFDE3, 0x37E1550D,
		0xB9229497, 0xA00EC5B7, 0xC144EF39, 0x9A1E2EE1, 0x92C11BED, 0x25743A2F, 0xCF2CB46F, 0xFAD1EED3,
		0xFF544D9B, 0xEC3CC2C9, 0xBC1938ED, 0x3670A8E3, 0x2D9889AF, 0x3CB0D173, 0x625A86C7, 0xD9EF3FCD,
		0xE10E9D61, 0x24DCF9A5, 0xB6815DDB, 0x4CFC12D1, 0x16439177, 0xCBA24E17, 0xEB8D7DB5, 0x7E540D63,
		0xC9F8D491, 0xA3C9B0C7, 0x9E0B289B, 0xC6E3D5B7, 0x9D3430ED, 0xA32B06F3, 0x47243EDD, 0x08EE7A29,
		0x1F8972B3, 0x87FF76C1, 0xF701704F, 0x08FE596D, 0xB725C529, 0xCDC96E23, 0x16989FA9, 0x0A4F5C59,
		0xAAE7C54B, 0x291C7533, 0x5B073B15, 0xCF6740FB, 0x3319EA1D, 0xEA3D4E13, 0xE485D147, 0x0E4CE74B,
		0x7DDE4451, 0xF5B9820D, 0x9D0F4E11, 0xF01DF9A5, 0x887652E7, 0xD5AC8F79, 0xBD355B25, 0x70A4F257,
		0x7C706855, 0xCD01F09F, 0xCF26F6F7, 0x17B32B61, 0xFB164EC9, 0xC9A4BB81, 0x1D5131DB, 0x9B8C9E0B,
		0xCEA1EDDB, 0xDAEE1029, 0xD98EC64B, 0x8099F855, 0x34FC5617, 0xFF48E3EF, 0xDE0C7DF7, 0xCDA1D21D,
		0xC262775B, 0x250BA8FB, 0x3867C535, 0xA3A55BE1, 0x2509EDA3, 0xF664655D, 0xE935C8D7, 0xF40F52B7,
	};

	/**
	 * Like {@link #HASH_MULTIPLIERS}, but specifically chosen to have no full collisions on a large list of English
	 * words (235970 of them) when used with the hashing function for FilteredString data structures like
	 * {@link FilteredStringSet} and {@link FilteredStringMap}.
	 */
	public static final int[] FILTERED_HASH_MULTIPLIERS = new int[]{
		0x00000719, 0x0000088F, 0x00000C53, 0x00000F45, 0x000011AF, 0x0000161D, 0x0000236B, 0x000029BB,
		0x00002AF7, 0x000038C5, 0x0000425B, 0x00004D35, 0x00005165, 0x000051B1, 0x000052CD, 0x00005881,
		0x00005AD7, 0x00005DCD, 0x00006A81, 0x00006EF7, 0x0000737D, 0x00007893, 0x000078D1, 0x00007E69,
		0x00008BB5, 0x00008CB5, 0x0000999F, 0x00009C79, 0x00009D0F, 0x0000A161, 0x0000A641, 0x0000A82D,
	};

	/**
	 * The recommended method to find potential replacements for entries in {@link #HASH_MULTIPLIERS}. This takes an
	 * odd-number int as a candidate, and evaluates four criteria:
	 * <ul>
	 *     <li>The Hamming weight of {@code candidate}, determined by {@link Integer#bitCount(int)},</li>
	 *     <li>The Hamming weight of {@code candidate}'s Gray code, which is {@code candidate ^ (candidate >>> 1)}</li>,
	 *     <li>The Hamming weight of {@code candidate}'s {@link MathTools#modularMultiplicativeInverse(int)}</li>, and
	 *     <li>The Hamming weight of the Gray code of {@code candidate}'s modular multiplicative inverse</li>.
	 * </ul>
	 * If all of these criteria are less than {@code threshold} different from 16 (the most central value possible), the
	 * candidate is considered optimal by that threshold. If it isn't optimal, the candidate will be randomized to a
	 * different odd number and evaluated again until it satisfies the threshold.
	 *
	 * @param candidate an odd-number int that will either be returned as-is if it is already considered optimal, or
	 *                  changed until it is evaluated to fit the {@code threshold}
	 * @param threshold recommended to be 1, the most stringest threshold allowed, but may be higher for a looser bound
	 * @return {@code candidate} if it is already optimal by {@code threshold}, otherwise a different odd int that is
	 */
	public static int optimizeMultiplier(int candidate, int threshold) {
		candidate |= 1;
		threshold = Math.min(Math.max(threshold, 1), 24);
		int inverse, add = 0;
		while (Math.abs(Integer.bitCount(candidate) - 16) > threshold
			|| Math.abs(Integer.bitCount(candidate ^ candidate >>> 1) - 16) > threshold
			|| Math.abs(Integer.bitCount(inverse = MathTools.modularMultiplicativeInverse(candidate)) - 16) > threshold
			|| Math.abs(Integer.bitCount(inverse ^ inverse >>> 1) - 16) > threshold) {
			candidate = candidate * 0xDE82EF95 + (add += 2);
		}
		return candidate;
	}

	private static final int COPY_THRESHOLD = 128;
	private static final int NIL_ARRAY_SIZE = 1024;
	@SuppressWarnings({"MismatchedReadAndWriteOfArray"})
	private static final Object[] NIL_ARRAY = new Object[NIL_ARRAY_SIZE];

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
		if (data == null) return seed;
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

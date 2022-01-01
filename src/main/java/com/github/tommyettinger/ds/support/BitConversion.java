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

package com.github.tommyettinger.ds.support;

/**
 * Methods for converting floats to and from ints, as well as doubles to and from longs and ints.
 * This is like NumberUtils in libGDX, but is closer to a subset of NumberTools in SquidLib. It
 * includes methods like {@link #floatToReversedIntBits(float)} (which is also in NumberTools, and
 * makes converting from an OpenGL packed ABGR float to an RGBA8888 int very easy) and
 * {@link #doubleToMixedIntBits(double)} (which is useful when implementing hashCode() for double
 * values). Everything's optimized for GWT, which is important because some core JDK methods like
 * {@link Float#floatToIntBits(float)} are quite slow on GWT. This makes heavy use of JS typed
 * arrays to accomplish its conversions; these are widespread even on mobile browsers, and are very
 * convenient for this sort of code (in some ways, they're a better fit for this sort of bit-level
 * operation in JavaScript than anything Java provides).
 *
 * @author Tommy Ettinger
 */
public final class BitConversion {
	/**
	 * Identical to {@link Double#doubleToLongBits(double)} on desktop; optimized on GWT. When compiling to JS via GWT,
	 * there is no way to distinguish NaN values with different bits but that are still NaN, so this doesn't try to
	 * somehow permit that. Uses JS typed arrays on GWT, which are well-supported now across all recent browsers and
	 * have fallbacks in GWT in the unlikely event of a browser not supporting them. JS typed arrays support double, but
	 * not long, so this needs to compose a long from two ints, which means the double-to/from-long conversions aren't
	 * as fast as float-to/from-int conversions.
	 * <br>
	 * This method may be a tiny bit slower than {@link #doubleToRawLongBits(double)} on non-HotSpot JVMs.
	 *
	 * @param value a {@code double} floating-point number.
	 * @return the bits that represent the floating-point number.
	 */
	public static long doubleToLongBits (final double value) {
		return Double.doubleToLongBits(value);
	}

	/**
	 * Identical to {@link Double#doubleToRawLongBits(double)} on desktop; optimized on GWT. When compiling to JS via
	 * GWT, there is no way to distinguish NaN values with different bits but that are still NaN, so this doesn't try
	 * to somehow permit that. Uses JS typed arrays on GWT, which are well-supported now across all recent browsers and
	 * have fallbacks in GWT in the unlikely event of a browser not supporting them. JS typed arrays support double, but
	 * not long, so this needs to compose a long from two ints, which means the double-to/from-long conversions aren't
	 * as fast as float-to/from-int conversions on GWT.
	 *
	 * @param value a {@code double} floating-point number.
	 * @return the bits that represent the floating-point number.
	 */
	public static long doubleToRawLongBits (final double value) {
		return Double.doubleToRawLongBits(value);
	}

	/**
	 * Identical to {@link Double#longBitsToDouble(long)} on desktop; optimized on GWT. Uses JS typed arrays on GWT,
	 * which are well-supported now across all recent browsers and have fallbacks in GWT in the unlikely event of a
	 * browser not supporting them. JS typed arrays support double, but not long, so this needs to compose a long from
	 * two ints, which means the double-to/from-long conversions aren't as fast as float-to/from-int conversions.
	 *
	 * @param bits a long.
	 * @return the {@code double} floating-point value with the same bit pattern.
	 */
	public static double longBitsToDouble (final long bits) {
		return Double.longBitsToDouble(bits);
	}

	/**
	 * Converts the bits of {@code value} to a long and gets the lower 32 bits of that long, as an int.
	 *
	 * @param value a {@code double} precision floating-point number.
	 * @return the lower half of the bits that represent the floating-point number, as an int.
	 */
	public static int doubleToLowIntBits (final double value) {
		return (int)Double.doubleToRawLongBits(value);
	}

	/**
	 * Converts the bits of {@code value} to a long and gets the upper 32 bits of that long, as an int.
	 *
	 * @param value a {@code double} precision floating-point number.
	 * @return the upper half of the bits that represent the floating-point number, as an int.
	 */
	public static int doubleToHighIntBits (final double value) {
		return (int)(Double.doubleToRawLongBits(value) >>> 32);
	}

	/**
	 * Converts the bits of {@code value} to a long and gets the XOR of its upper and lower 32-bit sections. Useful for
	 * numerical code where a 64-bit double needs to be reduced to a 32-bit value with some hope of keeping different
	 * doubles giving different ints.
	 *
	 * @param value a {@code double} precision floating-point number.
	 * @return the XOR of the lower and upper halves of the bits that represent the floating-point number.
	 */
	public static int doubleToMixedIntBits (final double value) {
		final long l = Double.doubleToRawLongBits(value);
		return (int)(l ^ l >>> 32);
	}

	/**
	 * Identical to {@link Float#floatToIntBits(float)} on desktop; optimized on GWT. Uses JS typed arrays on GWT, which
	 * are well-supported now across all recent browsers and have fallbacks in GWT in the unlikely event of a browser
	 * not supporting them.
	 * <br>
	 * This method may be a tiny bit slower than {@link #doubleToRawLongBits(double)} on non-HotSpot JVMs.
	 *
	 * @param value a floating-point number.
	 * @return the bits that represent the floating-point number.
	 */
	public static int floatToIntBits (final float value) {
		return Float.floatToIntBits(value);
	}

	/**
	 * Identical to {@link Float#floatToRawIntBits(float)} on desktop; optimized on GWT. When compiling to JS via GWT,
	 * there is no way to distinguish NaN values with different bits but that are still NaN, so this doesn't try to
	 * somehow permit that. Uses JS typed arrays on GWT, which are well-supported now across all recent browsers and
	 * have fallbacks in GWT in the unlikely event of a browser not supporting them.
	 *
	 * @param value a floating-point number.
	 * @return the bits that represent the floating-point number.
	 */
	public static int floatToRawIntBits (final float value) {
		return Float.floatToRawIntBits(value);
	}

	/**
	 * Gets the bit representation of the given float {@code value}, but with reversed byte order. On desktop, this is
	 * equivalent to calling {@code Integer.reverseBytes(Float.floatToRawIntBits(value))}, but it is implemented using
	 * typed arrays on GWT.
	 * <br>
	 * This is primarily intended for a common task in libGDX's internals: converting between RGBA8888 int colors and
	 * ABGR packed float colors. This method runs at the expected speed on desktop and mobile, but GWT should run it
	 * much more quickly than a direct translation of the Java would provide.
	 *
	 * @param value a floating-point number
	 * @return the bits that represent the floating-point number, with their byte order reversed from normal.
	 */
	public static int floatToReversedIntBits (final float value) {
		return Integer.reverseBytes(Float.floatToRawIntBits(value));
	}

	/**
	 * Reverses the byte order of {@code bits} and converts that to a float. On desktop, this is
	 * equivalent to calling {@code Float.intBitsToFloat(Integer.reverseBytes(bits))}, but it is implemented using
	 * typed arrays on GWT.
	 * <br>
	 * This is primarily intended for a common task in libGDX's internals: converting between RGBA8888 int colors and
	 * ABGR packed float colors. This method runs at the expected speed on desktop and mobile, but GWT should run it
	 * much more quickly than a direct translation of the Java would provide.
	 *
	 * @param bits an integer
	 * @return the {@code float} floating-point value with the given bits using their byte order reversed from normal.
	 */
	public static float reversedIntBitsToFloat (final int bits) {
		return Float.intBitsToFloat(Integer.reverseBytes(bits));
	}

	/**
	 * Identical to {@link Float#intBitsToFloat(int)} on desktop; optimized on GWT. Uses JS typed arrays on GWT, which
	 * are well-supported now across all recent browsers and have fallbacks in GWT in the unlikely event of a browser
	 * not supporting them.
	 *
	 * @param bits an integer.
	 * @return the {@code float} floating-point value with the same bit pattern.
	 */
	public static float intBitsToFloat (final int bits) {
		return Float.intBitsToFloat(bits);
	}

	/**
	 * Returns an int value with at most a single one-bit, in the position of the lowest-order ("rightmost") one-bit in
	 * the specified int value. Returns zero if the specified value has no one-bits in its two's complement binary
	 * representation, that is, if it is equal to zero.
	 * <br>
	 * Identical to {@link Integer#lowestOneBit(int)}, including on GWT. GWT calculates Integer.lowestOneBit() correctly,
	 * but does not always calculate Long.lowestOneBit() correctly. This overload is here so you can use lowestOneBit on
	 * an int value and get an int value back (which could be assigned to a long without losing data), or use it on a
	 * long value and get the correct long result on both GWT and other platforms.
	 *
	 * @param num the value whose lowest one bit is to be computed
	 * @return an int value with a single one-bit, in the position of the lowest-order one-bit in the specified value,
	 * or zero if the specified value is itself equal to zero.
	 */
	public static int lowestOneBit (int num) {
		return num & -num;
	}

	/**
	 * Returns an long value with at most a single one-bit, in the position of the lowest-order ("rightmost") one-bit in
	 * the specified long value. Returns zero if the specified value has no one-bits in its two's complement binary
	 * representation, that is, if it is equal to zero.
	 * <br>
	 * Identical to {@link Long#lowestOneBit(long)}, but super-sourced to act correctly on GWT. At least on GWT 2.8.2,
	 * {@link Long#lowestOneBit(long)} does not provide correct results for certain inputs on GWT. For example, when given
	 * -17592186044416L, Long.lowestOneBit() returns 0 on GWT, possibly because it converts to an int at some point. On
	 * other platforms, like desktop JDKs, {@code Long.lowestOneBit(-17592186044416L)} returns 17592186044416L.
	 *
	 * @param num the value whose lowest one bit is to be computed
	 * @return a long value with a single one-bit, in the position of the lowest-order one-bit in the specified value,
	 * or zero if the specified value is itself equal to zero.
	 */
	public static long lowestOneBit (long num) {
		return num & -num;
	}

}

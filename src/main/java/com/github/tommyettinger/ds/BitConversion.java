package com.github.tommyettinger.ds;

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
 */
public final class BitConversion {
	/**
	 * Identical to {@link Double#doubleToLongBits(double)} on desktop; optimized on GWT. When compiling to JS via GWT,
	 * there is no way to distinguish NaN values with different bits but that are still NaN, so this doesn't try to
	 * somehow permit that. Uses JS typed arrays on GWT, which are well-supported now across all recent browsers and
	 * have fallbacks in GWT in the unlikely event of a browser not supporting them. JS typed arrays support double, but
	 * not long, so this needs to compose a long from two ints, which means the double-to/from-long conversions aren't
	 * as fast as float-to/from-int conversions.
	 * @param value a {@code double} floating-point number.
	 * @return the bits that represent the floating-point number.
	 */
	public static long doubleToLongBits(final double value)
	{
		return Double.doubleToLongBits(value);
	}
	/**
	 * Identical to {@link Double#doubleToLongBits(double)} on desktop (note, not
	 * {@link Double#doubleToRawLongBits(double)}); optimized on GWT. When compiling to JS via GWT, there is no way to
	 * distinguish NaN values with different bits but that are still NaN, so this doesn't try to somehow permit that.
	 * Uses JS typed arrays on GWT, which are well-supported now across all recent browsers and have fallbacks in GWT in
	 * the unlikely event of a browser not supporting them. JS typed arrays support double, but not long, so this needs
	 * to compose a long from two ints, which means the double-to/from-long conversions aren't as fast as
	 * float-to/from-int conversions.
	 * @param value a {@code double} floating-point number.
	 * @return the bits that represent the floating-point number.
	 */
	public static long doubleToRawLongBits(final double value)
	{
		return Double.doubleToLongBits(value);
	}

	/**
	 * Identical to {@link Double#longBitsToDouble(long)} on desktop; optimized on GWT. Uses JS typed arrays on GWT,
	 * which are well-supported now across all recent browsers and have fallbacks in GWT in the unlikely event of a
	 * browser not supporting them. JS typed arrays support double, but not long, so this needs to compose a long from
	 * two ints, which means the double-to/from-long conversions aren't as fast as float-to/from-int conversions.
	 * @param bits a long.
	 * @return the {@code double} floating-point value with the same bit pattern.
	 */
	public static double longBitsToDouble(final long bits)
	{
		return Double.longBitsToDouble(bits);
	}
	/**
	 * Converts {@code value} to a long and gets the lower 32 bits of that long, as an int.
	 * @param value a {@code double} precision floating-point number.
	 * @return the lower half of the bits that represent the floating-point number, as an int.
	 */
	public static int doubleToLowIntBits(final double value)
	{
		return (int)(Double.doubleToLongBits(value) & 0xffffffffL);
	}

	/**
	 * Converts {@code value} to a long and gets the upper 32 bits of that long, as an int.
	 * @param value a {@code double} precision floating-point number.
	 * @return the upper half of the bits that represent the floating-point number, as an int.
	 */
	public static int doubleToHighIntBits(final double value)
	{
		return (int)(Double.doubleToLongBits(value) >>> 32);
	}

	/**
	 * Converts {@code value} to a long and gets the XOR of its upper and lower 32-bit sections. Useful for numerical
	 * code where a 64-bit double needs to be reduced to a 32-bit value with some hope of keeping different doubles
	 * giving different ints.
	 * @param value a {@code double} precision floating-point number.
	 * @return the XOR of the lower and upper halves of the bits that represent the floating-point number.
	 */
	public static int doubleToMixedIntBits(final double value)
	{
		final long l = Double.doubleToLongBits(value);
		return (int)((l ^ l >>> 32) & 0xFFFFFFFFL);
	}
	/**
	 * Identical to {@link Float#floatToIntBits(float)} on desktop; optimized on GWT. Uses JS typed arrays on GWT, which
	 * are well-supported now across all recent browsers and have fallbacks in GWT in the unlikely event of a browser
	 * not supporting them.
	 * @param value a floating-point number.
	 * @return the bits that represent the floating-point number.
	 */
	public static int floatToIntBits(final float value)
	{
		return Float.floatToIntBits(value);
	}
	/**
	 * Identical to {@link Float#floatToIntBits(float)} on desktop (note, not {@link Float#floatToRawIntBits(float)});
	 * optimized on GWT. When compiling to JS via GWT, there is no way to distinguish NaN values with different bits but
	 * that are still NaN, so this doesn't try to somehow permit that. Uses JS typed arrays on GWT, which are
	 * well-supported now across all recent browsers and have fallbacks in GWT in the unlikely event of a browser not
	 * supporting them.
	 * @param value a floating-point number.
	 * @return the bits that represent the floating-point number.
	 */
	public static int floatToRawIntBits(final float value)
	{
		return Float.floatToIntBits(value);
	}


	/**
	 * Gets the bit representation of the given float {@code value}, but with reversed byte order. On desktop, this is
	 * equivalent to calling {@code Integer.reverseBytes(Float.floatToIntBits(value))}, but it is implemented using
	 * typed arrays on GWT.
	 * @param value a floating-point number
	 * @return the bits that represent the floating-point number, with their byte order reversed from normal.
	 */
	public static int floatToReversedIntBits(final float value) {
		return Integer.reverseBytes(Float.floatToIntBits(value));
	}

	/**
	 * Reverses the byte order of {@code bits} and converts that to a float. On desktop, this is
	 * equivalent to calling {@code Float.intBitsToFloat(Integer.reverseBytes(bits))}, but it is implemented using
	 * typed arrays on GWT.
	 * @param bits an integer
	 * @return the {@code float} floating-point value with the given bits using their byte order reversed from normal.
	 */
	public static float reversedIntBitsToFloat(final int bits) {
		return Float.intBitsToFloat(Integer.reverseBytes(bits));
	}

	/**
	 * Identical to {@link Float#intBitsToFloat(int)} on desktop; optimized on GWT. Uses JS typed arrays on GWT, which
	 * are well-supported now across all recent browsers and have fallbacks in GWT in the unlikely event of a browser
	 * not supporting them.
	 * @param bits an integer.
	 * @return the {@code float} floating-point value with the same bit pattern.
	 */
	public static float intBitsToFloat(final int bits)
	{
		return Float.intBitsToFloat(bits);
	}

}

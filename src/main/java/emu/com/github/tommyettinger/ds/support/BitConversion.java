package emu.com.github.tommyettinger.ds.support;

public final class BitConversion {
	public static final Int8Array wba = Int8ArrayNative.create(8);
	public static final Int32Array wia = Int32ArrayNative.create(wba.buffer(), 0, 2);
	public static final Float32Array wfa = Float32ArrayNative.create(wba.buffer(), 0, 2);
	public static final Float64Array wda = Float64ArrayNative.create(wba.buffer(), 0, 1);
	public static final DataView dv = DataViewNative.create(wba.buffer());

	public static long doubleToLongBits (final double value) {
		wda.set(0, value);
		return ((long)wia.get(1) << 32) | (wia.get(0) & 0xffffffffL);
	}

	public static long doubleToRawLongBits (final double value) {
		wda.set(0, value);
		return ((long)wia.get(1) << 32) | (wia.get(0) & 0xffffffffL);
	}

	public static double longBitsToDouble (final long bits) {
		wia.set(1, (int)(bits >>> 32));
		wia.set(0, (int)(bits & 0xffffffffL));
		return wda.get(0);
	}

	public static int doubleToLowIntBits (final double value) {
		wda.set(0, value);
		return wia.get(0);
	}

	public static int doubleToHighIntBits (final double value) {
		wda.set(0, value);
		return wia.get(1);
	}

	public static int doubleToMixedIntBits (final double value) {
		wda.set(0, value);
		return wia.get(0) ^ wia.get(1);
	}

	public static int floatToIntBits (final float value) {
		wfa.set(0, value);
		return wia.get(0);
	}

	public static int floatToRawIntBits (final float value) {
		wfa.set(0, value);
		return wia.get(0);
	}

	public static int floatToReversedIntBits (final float value) {
		dv.setFloat32(0, value, true);
		return dv.getInt32(0, false);
	}

	public static float reversedIntBitsToFloat (final int bits) {
		dv.setInt32(0, bits, true);
		return dv.getFloat32(0, false);
	}

	public static float intBitsToFloat (final int bits) {
		wia.set(0, bits);
		return wfa.get(0);
	}

	public static int lowestOneBit(int num) {
		return num & -num;
	}

	public static long lowestOneBit(long num) {
		return num & ~(num - 1L);
	}

}

/*******************************************************************************
 * Copyright 2021 See AUTHORS file.
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

package com.github.tommyettinger.ds.support;

import com.google.gwt.typedarrays.client.Float64ArrayNative;
import com.google.gwt.typedarrays.client.Float32ArrayNative;
import com.google.gwt.typedarrays.client.Int32ArrayNative;
import com.google.gwt.typedarrays.client.Int8ArrayNative;
import com.google.gwt.typedarrays.client.DataViewNative;
import com.google.gwt.typedarrays.shared.Float64Array;
import com.google.gwt.typedarrays.shared.Float32Array;
import com.google.gwt.typedarrays.shared.Int32Array;
import com.google.gwt.typedarrays.shared.Int8Array;
import com.google.gwt.typedarrays.shared.DataView;

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

/*
 * Copyright (c) 2025 See AUTHORS file.
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

package com.github.tommyettinger.ds.test;

import com.github.tommyettinger.digital.TextTools;
import com.github.tommyettinger.ds.*;
import org.junit.Assert;
import org.junit.Test;

public class LegibleTest {
	public static final String[] strings = {
		"alpha", "beta", "gamma", "delta", "epsilon", "zeta", "eta", "theta", "iota", "kappa", "lambda",
		"mu", "nu", "xi", "omicron", "pi", "rho", "sigma", "tau", "upsilon", "phi", "chi", "psi", "omega"
	};
	public static final boolean[] booleans = {
		true, false, false, false, true, false, true, true, false, true, true, false, true, true, false, true
	};
	public static final byte[] bytes = {
		1, 0, -1, 2, -2, 3, -3, 11, 10, -11, 12, -12, 13, -13, 111, 110, -111, 112, -112, 113, -113
	};
	public static final short[] shorts = {
		'Α', 'Β', 'Γ', 'Δ', 'Ε', 'Ζ', 'Η', 'Θ', 'Ι', 'Κ', 'Λ', 'Μ', 'Ν', 'Ξ', 'Ο', 'Π', 'Ρ', 'Σ', 'Τ', 'Υ', 'Φ', 'Χ', 'Ψ', 'Ω'
	};
	public static final char[] chars = {
		'Α', 'Β', 'Γ', 'Δ', 'Ε', 'Ζ', 'Η', 'Θ', 'Ι', 'Κ', 'Λ', 'Μ', 'Ν', 'Ξ', 'Ο', 'Π', 'Ρ', 'Σ', 'Τ', 'Υ', 'Φ', 'Χ', 'Ψ', 'Ω'
	};
	public static final int[] ints = {
		'Α', 'Β', 'Γ', 'Δ', 'Ε', 'Ζ', 'Η', 'Θ', 'Ι', 'Κ', 'Λ', 'Μ', 'Ν', 'Ξ', 'Ο', 'Π', 'Ρ', 'Σ', 'Τ', 'Υ', 'Φ', 'Χ', 'Ψ', 'Ω'
	};
	public static final long[] longs = {
		'Α', 'Β', 'Γ', 'Δ', 'Ε', 'Ζ', 'Η', 'Θ', 'Ι', 'Κ', 'Λ', 'Μ', 'Ν', 'Ξ', 'Ο', 'Π', 'Ρ', 'Σ', 'Τ', 'Υ', 'Φ', 'Χ', 'Ψ', 'Ω'
	};
	public static final float[] floats = {
		'Α', 'Β', 'Γ', 'Δ', 'Ε', 'Ζ', 'Η', 'Θ', 'Ι', 'Κ', 'Λ', 'Μ', 'Ν', 'Ξ', 'Ο', 'Π', 'Ρ', 'Σ', 'Τ', 'Υ', 'Φ', 'Χ', 'Ψ', 'Ω'
	};
	public static final double[] doubles = {
		'Α', 'Β', 'Γ', 'Δ', 'Ε', 'Ζ', 'Η', 'Θ', 'Ι', 'Κ', 'Λ', 'Μ', 'Ν', 'Ξ', 'Ο', 'Π', 'Ρ', 'Σ', 'Τ', 'Υ', 'Φ', 'Χ', 'Ψ', 'Ω'
	};

//	@Test
//	public void testObjectListLegible() {
//		ObjectList<String> data = ObjectList.with(strings), loaded = new ObjectList<>(strings.length);
//		String legible = data.toString(";");
//		loaded.addAll(TextTools.split(legible, ";"));
//		Assert.assertEquals("Lists were not equal! legible was: " + legible, data, loaded);
//	}
//
//	@Test
//	public void testObjectDequeLegible() {
//		ObjectDeque<String> data = ObjectDeque.with(strings), loaded = new ObjectDeque<>(strings.length);
//		String legible = data.toString(";");
//		loaded.addAll(TextTools.split(legible, ";"));
//		Assert.assertEquals("Deques were not equal! legible was: " + legible, data, loaded);
//	}

	@Test
	public void testByteListLegible() {
		ByteList data = ByteList.with(bytes), loaded = new ByteList(bytes.length);
		String legible = data.toString(", ", false);
		loaded.addLegible(legible, ", ", 0, -1);
		Assert.assertEquals("Lists were not equal! legible was: " + legible, data, loaded);
	}

	@Test
	public void testByteDequeLegible() {
		ByteDeque data = ByteDeque.with(bytes), loaded = new ByteDeque(bytes.length);
		String legible = data.toString(", ", false);
		loaded.addLegible(legible, ", ", 0, -1);
		Assert.assertEquals("Deques were not equal! legible was: " + legible, data, loaded);
	}

	@Test
	public void testShortListLegible() {
		ShortList data = ShortList.with(shorts), loaded = new ShortList(shorts.length);
		String legible = data.toString(", ", false);
		loaded.addLegible(legible, ", ", 0, -1);
		Assert.assertEquals("Lists were not equal! legible was: " + legible, data, loaded);
	}

	@Test
	public void testShortDequeLegible() {
		ShortDeque data = ShortDeque.with(shorts), loaded = new ShortDeque(shorts.length);
		String legible = data.toString(", ", false);
		loaded.addLegible(legible, ", ", 0, -1);
		Assert.assertEquals("Deques were not equal! legible was: " + legible, data, loaded);
	}

	@Test
	public void testIntListLegible() {
		IntList data = IntList.with(ints), loaded = new IntList(ints.length);
		String legible = data.toString(", ", false);
		loaded.addLegible(legible, ", ", 0, -1);
		Assert.assertEquals("Lists were not equal! legible was: " + legible, data, loaded);
	}

	@Test
	public void testIntDequeLegible() {
		IntDeque data = IntDeque.with(ints), loaded = new IntDeque(ints.length);
		String legible = data.toString(", ", false);
		loaded.addLegible(legible, ", ", 0, -1);
		Assert.assertEquals("Deques were not equal! legible was: " + legible, data, loaded);
	}

	@Test
	public void testLongListLegible() {
		LongList data = LongList.with(longs), loaded = new LongList(longs.length);
		String legible = data.toString(", ", false);
		loaded.addLegible(legible, ", ", 0, -1);
		Assert.assertEquals("Lists were not equal! legible was: " + legible, data, loaded);
	}

	@Test
	public void testLongDequeLegible() {
		LongDeque data = LongDeque.with(longs), loaded = new LongDeque(longs.length);
		String legible = data.toString(", ", false);
		loaded.addLegible(legible, ", ", 0, -1);
		Assert.assertEquals("Deques were not equal! legible was: " + legible, data, loaded);
	}

	@Test
	public void testFloatListLegible() {
		FloatList data = FloatList.with(floats), loaded = new FloatList(floats.length);
		String legible = data.toString(", ", false);
		loaded.addLegible(legible, ", ", 0, -1);
		Assert.assertEquals("Lists were not equal! legible was: " + legible, data, loaded);
	}

	@Test
	public void testFloatDequeLegible() {
		FloatDeque data = FloatDeque.with(floats), loaded = new FloatDeque(floats.length);
		String legible = data.toString(", ", false);
		loaded.addLegible(legible, ", ", 0, -1);
		Assert.assertEquals("Deques were not equal! legible was: " + legible, data, loaded);
	}

	@Test
	public void testDoubleListLegible() {
		DoubleList data = DoubleList.with(doubles), loaded = new DoubleList(doubles.length);
		String legible = data.toString(", ", false);
		loaded.addLegible(legible, ", ", 0, -1);
		Assert.assertEquals("Lists were not equal! legible was: " + legible, data, loaded);
	}

	@Test
	public void testDoubleDequeLegible() {
		DoubleDeque data = DoubleDeque.with(doubles), loaded = new DoubleDeque(doubles.length);
		String legible = data.toString(", ", false);
		loaded.addLegible(legible, ", ", 0, -1);
		Assert.assertEquals("Deques were not equal! legible was: " + legible, data, loaded);
	}

	@Test
	public void testCharListLegible() {
		CharList data = CharList.with(chars), loaded = new CharList(chars.length);
		String legible = data.toString(", ", false);
		loaded.addLegible(legible, ", ", 0, -1);
		Assert.assertEquals("Lists were not equal! legible was: " + legible, data, loaded);
	}

	@Test
	public void testCharDequeLegible() {
		CharDeque data = CharDeque.with(chars), loaded = new CharDeque(chars.length);
		String legible = data.toString(", ", false);
		loaded.addLegible(legible, ", ", 0, -1);
		Assert.assertEquals("Deques were not equal! legible was: " + legible, data, loaded);
	}

	@Test
	public void testBooleanListLegible() {
		BooleanList data = BooleanList.with(booleans), loaded = new BooleanList(booleans.length);
		String legible = data.toString(", ", false);
		loaded.addLegible(legible, ", ", 0, -1);
		Assert.assertEquals("Lists were not equal! legible was: " + legible, data, loaded);
	}

	@Test
	public void testBooleanDequeLegible() {
		BooleanDeque data = BooleanDeque.with(booleans), loaded = new BooleanDeque(booleans.length);
		String legible = data.toString(", ", false);
		loaded.addLegible(legible, ", ", 0, -1);
		Assert.assertEquals("Deques were not equal! legible was: " + legible, data, loaded);
	}

	@Test
	public void testLongFloatMapLegible() {
		LongFloatMap data = new LongFloatMap(longs, floats), loaded = new LongFloatMap(longs.length);
		String legible = data.toString(", ", false);
		loaded.putLegible(legible);
		Assert.assertEquals("Maps were not equal! legible was: " + legible, data, loaded);
	}
}
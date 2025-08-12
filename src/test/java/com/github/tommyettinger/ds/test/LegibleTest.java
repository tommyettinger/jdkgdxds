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
import com.github.tommyettinger.ds.support.util.PartialParser;
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

	@Test
	public void testObjectListLegible() {
		ObjectList<String> data = ObjectList.with(strings), loaded = new ObjectList<>(strings.length);
		String legible = data.toString(";");
		loaded.addLegible(legible, ";", PartialParser.DEFAULT_STRING);
		Assert.assertEquals("Lists were not equal! legible was: " + legible, data, loaded);
	}

	@Test
	public void testObjectDequeLegible() {
		ObjectDeque<String> data = ObjectDeque.with(strings), loaded = new ObjectDeque<>(strings.length);
		String legible = data.toString(";");
		loaded.addLegible(legible, ";", PartialParser.DEFAULT_STRING, 0, -1);
		Assert.assertEquals("Deques were not equal! legible was: " + legible, data, loaded);
	}

	@Test
	public void testEnumSetLegible() {
		OrderType[] universe = OrderType.values();
		EnumSet data = EnumSet.allOf(universe), loaded = EnumSet.noneOf(universe);
		String legible = data.toString(";");
		loaded.addLegible(legible, ";", PartialParser.DEFAULT_ORDER_TYPE);
		Assert.assertEquals("Sets were not equal! legible was: " + legible, data, loaded);
	}

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
		String legible = data.toString(", ", true);
		loaded.addLegible(legible, ", ", 1, legible.length() - 2);
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
		String legible = data.toString(", ", true);
		loaded.putLegible(legible, ", ", "=", 1, legible.length() - 2);
		Assert.assertEquals("Maps were not equal! legible was: " + legible, data, loaded);
	}

	@Test
	public void testLongIntMapLegible() {
		LongIntMap data = new LongIntMap(longs, ints), loaded = new LongIntMap(longs.length);
		String legible = data.toString(", ", true);
		loaded.putLegible(legible, ", ", "=", 1, legible.length() - 2);
		Assert.assertEquals("Maps were not equal! legible was: " + legible, data, loaded);
	}

	@Test
	public void testLongLongMapLegible() {
		LongLongMap data = new LongLongMap(longs, longs), loaded = new LongLongMap(longs.length);
		String legible = data.toString(", ", true);
		loaded.putLegible(legible, ", ", "=", 1, legible.length() - 2);
		Assert.assertEquals("Maps were not equal! legible was: " + legible, data, loaded);
	}

	@Test
	public void testLongObjectMapLegible() {
		long[] longs = new long[]{1L, 2L, 3L};
		int size = longs.length;
		DoubleList nums = DoubleList.with(doubles);
		ObjectList<DoubleList> lists = new ObjectList<>(size);
		for (int i = 0; i < size; i++) {
			lists.add(nums);
		}
		LongObjectMap<DoubleList> data = new LongObjectMap<>(LongList.with(longs), lists), loaded = new LongObjectMap<>(size);
		String legible = data.toString(";;", false);
		loaded.putLegible(legible, ";;", (text, start, end) ->
		{
			DoubleList list = new DoubleList();
			list.addLegible(text, ", ", start + 1, end - start - 2);
			return list;
		});
		Assert.assertEquals("Maps were not equal! legible was: " + legible, data, loaded);
	}

	@Test
	public void testIntFloatMapLegible() {
		IntFloatMap data = new IntFloatMap(ints, floats), loaded = new IntFloatMap(ints.length);
		String legible = data.toString(", ", true);
		loaded.putLegible(legible, ", ", "=", 1, legible.length() - 2);
		Assert.assertEquals("Maps were not equal! legible was: " + legible, data, loaded);
	}

	@Test
	public void testIntIntMapLegible() {
		IntIntMap data = new IntIntMap(ints, ints), loaded = new IntIntMap(ints.length);
		String legible = data.toString(", ", true);
		loaded.putLegible(legible, ", ", "=", 1, legible.length() - 2);
		Assert.assertEquals("Maps were not equal! legible was: " + legible, data, loaded);
	}

	@Test
	public void testIntLongMapLegible() {
		IntLongMap data = new IntLongMap(ints, longs), loaded = new IntLongMap(ints.length);
		String legible = data.toString(", ", true);
		loaded.putLegible(legible, ", ", "=", 1, legible.length() - 2);
		Assert.assertEquals("Maps were not equal! legible was: " + legible, data, loaded);
	}

	@Test
	public void testIntObjectMapLegible() {
		int[] ints = new int[]{1, 2, 3};
		int size = ints.length;
		DoubleList nums = DoubleList.with(doubles);
		ObjectList<DoubleList> lists = new ObjectList<>(size);
		for (int i = 0; i < size; i++) {
			lists.add(nums);
		}
		IntObjectMap<DoubleList> data = new IntObjectMap<>(IntList.with(ints), lists), loaded = new IntObjectMap<>(size);
		String legible = data.toString(";;", false);
		loaded.putLegible(legible, ";;", PartialParser.doubleCollectionParser(DoubleList::new, ", ", true));
		Assert.assertEquals("Maps were not equal! legible was: " + legible, data, loaded);
	}

	@Test
	public void testObjectObjectMapLegible() {
		int size = strings.length;
		DoubleList nums = DoubleList.with(doubles);
		ObjectList<DoubleList> lists = new ObjectList<>(size);
		for (int i = 0; i < size; i++) {
			lists.add(nums);
		}
		ObjectObjectMap<String, DoubleList> data = new ObjectObjectMap<>(ObjectList.with(strings), lists), loaded = new ObjectObjectMap<>(size);
		String legible = data.toString(";;", false);
		loaded.putLegible(legible, ";;", PartialParser.DEFAULT_STRING, PartialParser.doubleCollectionParser(DoubleList::new, ", ", true));
		Assert.assertEquals("Maps were not equal! legible was: " + legible, data, loaded);
	}
}

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

import com.github.tommyettinger.ds.*;
import com.github.tommyettinger.ds.support.util.*;
import org.junit.Assert;
import org.junit.Test;

public class LimitingIteratorTest {
	//		"alpha", "beta", "gamma", "delta", "epsilon", "zeta", "eta", "theta", "iota", "kappa", "lambda",
	//		"mu", "nu", "xi", "omicron", "pi", "rho", "sigma", "tau", "upsilon", "phi", "chi", "psi", "omega"
	@Test
	public void testObjLimitingIterator() {
		ObjectList<String> data = ObjectList.with(IteratorTest.strings);
		LimitingIterator<String> lim = new LimitingIterator<>(data.iterator(), 4);
		ObjectList<String> next = new ObjectList<>();
		next.addAll(lim);
		Assert.assertEquals("beta", next.get(1));
		Assert.assertEquals("delta", next.peek());
		lim.set(data.iterator(), 3);
		next.clear();
		next.addAll(lim);
		Assert.assertEquals("alpha", next.first());
		Assert.assertEquals("gamma", next.peek());
	}

	//		'Α', 'Β', 'Γ', 'Δ', 'Ε', 'Ζ', 'Η', 'Θ', 'Ι', 'Κ', 'Λ', 'Μ', 'Ν', 'Ξ', 'Ο', 'Π', 'Ρ', 'Σ', 'Τ', 'Υ', 'Φ', 'Χ', 'Ψ', 'Ω'
	@Test
	public void testLimitingLongIterator() {
		LongList data = LongList.with(IteratorTest.longs);
		LimitingLongIterator lim = new LimitingLongIterator(data.iterator(), 4);
		LongList next = new LongList();
		next.addAll(lim);
		Assert.assertEquals('Β', next.get(1));
		Assert.assertEquals('Δ', next.peek());
		lim.set(data.iterator(), 3);
		next.clear();
		next.addAll(lim);
		Assert.assertEquals('Α', next.first());
		Assert.assertEquals('Γ', next.peek());
	}

	@Test
	public void testLimitingIntIterator() {
		IntList data = IntList.with(IteratorTest.ints);
		LimitingIntIterator lim = new LimitingIntIterator(data.iterator(), 4);
		IntList next = new IntList();
		next.addAll(lim);
		Assert.assertEquals('Β', next.get(1));
		Assert.assertEquals('Δ', next.peek());
		lim.set(data.iterator(), 3);
		next.clear();
		next.addAll(lim);
		Assert.assertEquals('Α', next.first());
		Assert.assertEquals('Γ', next.peek());
	}

	@Test
	public void testLimitingShortIterator() {
		ShortList data = ShortList.with(IteratorTest.shorts);
		LimitingShortIterator lim = new LimitingShortIterator(data.iterator(), 4);
		ShortList next = new ShortList();
		next.addAll(lim);
		Assert.assertEquals('Β', next.get(1));
		Assert.assertEquals('Δ', next.peek());
		lim.set(data.iterator(), 3);
		next.clear();
		next.addAll(lim);
		Assert.assertEquals('Α', next.first());
		Assert.assertEquals('Γ', next.peek());
	}

	//    //1, 0, -1, 2, -2, 3, -3, 11, 10, -11, 12, -12, 13, -13, 111, 110, -111, 112, -112, 113, -113
	@Test
	public void testLimitingByteIterator() {
		ByteList data = ByteList.with(IteratorTest.bytes);
		LimitingByteIterator lim = new LimitingByteIterator(data.iterator(), 4);
		ByteList next = new ByteList();
		next.addAll(lim);
		Assert.assertEquals(0, next.get(1));
		Assert.assertEquals(2, next.peek());
		lim.set(data.iterator(), 3);
		next.clear();
		next.addAll(lim);
		Assert.assertEquals(1, next.first());
		Assert.assertEquals(-1, next.peek());
	}

	@Test
	public void testLimitingFloatIterator() {
		FloatList data = FloatList.with(IteratorTest.floats);
		LimitingFloatIterator lim = new LimitingFloatIterator(data.iterator(), 4);
		FloatList next = new FloatList();
		next.addAll(lim);
		Assert.assertEquals('Β', next.get(1), 0);
		Assert.assertEquals('Δ', next.peek(), 0);
		lim.set(data.iterator(), 3);
		next.clear();
		next.addAll(lim);
		Assert.assertEquals('Α', next.first(), 0);
		Assert.assertEquals('Γ', next.peek(), 0);
	}

	@Test
	public void testLimitingDoubleIterator() {
		DoubleList data = DoubleList.with(IteratorTest.doubles);
		LimitingDoubleIterator lim = new LimitingDoubleIterator(data.iterator(), 4);
		DoubleList next = new DoubleList();
		next.addAll(lim);
		Assert.assertEquals('Β', next.get(1), 0);
		Assert.assertEquals('Δ', next.peek(), 0);
		lim.set(data.iterator(), 3);
		next.clear();
		next.addAll(lim);
		Assert.assertEquals('Α', next.first(), 0);
		Assert.assertEquals('Γ', next.peek(), 0);
	}

	@Test
	public void testLimitingCharIterator() {
		CharList data = CharList.with(IteratorTest.chars);
		LimitingCharIterator lim = new LimitingCharIterator(data.iterator(), 4);
		CharList next = new CharList();
		next.addAll(lim);
		Assert.assertEquals('Β', next.get(1));
		Assert.assertEquals('Δ', next.peek());
		lim.set(data.iterator(), 3);
		next.clear();
		next.addAll(lim);
		Assert.assertEquals('Α', next.first());
		Assert.assertEquals('Γ', next.peek());
	}

	/// /true, false, false, false, true, false, true, true, false, true, true, false, true, true, false, true

	@Test
	public void testLimitingBooleanIterator() {
		BooleanList data = BooleanList.with(IteratorTest.booleans);
		LimitingBooleanIterator lim = new LimitingBooleanIterator(data.iterator(), 5);
		BooleanList next = new BooleanList();
		next.addAll(lim);
		Assert.assertFalse(next.get(1));
		Assert.assertTrue(next.peek());
		lim.set(data.iterator(), 3);
		next.clear();
		next.addAll(lim);
		Assert.assertTrue(next.first());
		Assert.assertFalse(next.peek());
	}
}

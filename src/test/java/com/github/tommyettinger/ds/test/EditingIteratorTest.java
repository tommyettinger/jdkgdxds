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

import com.github.tommyettinger.digital.MathTools;
import com.github.tommyettinger.ds.*;
import com.github.tommyettinger.ds.support.util.*;
import org.junit.Assert;
import org.junit.Test;

public class EditingIteratorTest {
	@Test
	public void testObjEditingIterator() {
		ObjectList<String> data = ObjectList.with(IteratorTest.strings);
		EditingIterator<String> fil =
			new EditingIterator<>(data.iterator(), (String s) -> s + " " + s.length());
		ObjectList<String> next = new ObjectList<>();
		next.addAll(fil);
		Assert.assertEquals("alpha 5", next.first());
		fil.set(data.iterator(), (String s) -> s + s + s);
		next.clear();
		next.addAll(fil);
		Assert.assertEquals("betabetabeta", next.get(1));
	}

	@Test
	public void testObjAlteringIterator() {
		ObjectList<String> data = ObjectList.with(IteratorTest.strings);
		AlteringIterator<String, Integer> fil =
			new AlteringIterator<>(data.iterator(), String::length);
		ObjectList<Integer> next = new ObjectList<>();
		next.addAll(fil);
		Assert.assertEquals(Integer.valueOf(5), next.first());
		fil.set(data.iterator(), (String s) -> (int) s.charAt(0));
		next.clear();
		next.addAll(fil);
		Assert.assertEquals(Integer.valueOf('b'), next.get(1));
	}

	@Test
	public void testEditingLongIterator() {
		LongList data = LongList.with(IteratorTest.longs);
		EditingLongIterator fil =
			new EditingLongIterator(data.iterator(), (long s) -> s & 1L);
		LongList next = new LongList();
		next.addAll(fil);
		Assert.assertEquals('Α' & 1L, next.first());
		fil.set(data.iterator(), (long s) -> s % 10);
		next.clear();
		next.addAll(fil);
		Assert.assertEquals('Β' % 10, next.get(1));
	}

	@Test
	public void testEditingIntIterator() {
		IntList data = IntList.with(IteratorTest.ints);
		EditingIntIterator fil =
			new EditingIntIterator(data.iterator(), (int s) -> s & 1);
		IntList next = new IntList();
		next.addAll(fil);
		Assert.assertEquals('Α' & 1, next.first());
		fil.set(data.iterator(), (int s) -> s % 10);
		next.clear();
		next.addAll(fil);
		Assert.assertEquals('Β' % 10, next.get(1));
	}

	@Test
	public void testEditingShortIterator() {
		ShortList data = ShortList.with(IteratorTest.shorts);
		EditingShortIterator fil =
			new EditingShortIterator(data.iterator(), (short s) -> (short) (s & 1));
		ShortList next = new ShortList();
		next.addAll(fil);
		Assert.assertEquals('Α' & 1, next.first());
		fil.set(data.iterator(), (short s) -> (short) (s % 10));
		next.clear();
		next.addAll(fil);
		Assert.assertEquals('Β' % 10, next.get(1));
	}

	@Test
	public void testEditingByteIterator() {
		ByteList data = ByteList.with(IteratorTest.bytes);
		EditingByteIterator fil =
			new EditingByteIterator(data.iterator(), (byte s) -> (byte) (s & 1));
		ByteList next = new ByteList();
		next.addAll(fil);
		Assert.assertEquals(-1 & 1, next.get(2));
		fil.set(data.iterator(), (byte s) -> (byte) (s % 10));
		next.clear();
		next.addAll(fil);
		Assert.assertEquals(2 % 10, next.get(3));
	}

	@Test
	public void testEditingFloatIterator() {
		FloatList data = FloatList.with(IteratorTest.floats);
		EditingFloatIterator fil =
			new EditingFloatIterator(data.iterator(), (float s) -> (float) (s % 2));
		FloatList next = new FloatList();
		next.addAll(fil);
		Assert.assertEquals('Α' & 1, next.first(), MathTools.EPSILON);
		fil.set(data.iterator(), (float s) -> (float) (s % 10));
		next.clear();
		next.addAll(fil);
		Assert.assertEquals('Β' % 10, next.get(1), MathTools.EPSILON);
	}

	@Test
	public void testEditingDoubleIterator() {
		DoubleList data = DoubleList.with(IteratorTest.doubles);
		EditingDoubleIterator fil =
			new EditingDoubleIterator(data.iterator(), (double s) -> (double) (s % 2));
		DoubleList next = new DoubleList();
		next.addAll(fil);
		Assert.assertEquals('Α' & 1, next.first(), MathTools.EPSILON_D);
		fil.set(data.iterator(), (double s) -> (double) (s % 10));
		next.clear();
		next.addAll(fil);
		Assert.assertEquals('Β' % 10, next.get(1), MathTools.EPSILON_D);
	}

	@Test
	public void testEditingCharIterator() {
		CharList data = CharList.with(IteratorTest.chars);
		EditingCharIterator fil =
			new EditingCharIterator(data.iterator(), (char s) -> (char) (s & 1));
		CharList next = new CharList();
		next.addAll(fil);
		Assert.assertEquals('Α' & 1, next.first());
		fil.set(data.iterator(), (char s) -> (char) (s % 10));
		next.clear();
		next.addAll(fil);
		Assert.assertEquals('Β' % 10, next.get(1));
	}

	//true, false, false, false, true, false, true, true, false, true, true, false, true, true, false, true
	@Test
	public void testEditingBooleanIterator() {
		BooleanList data = BooleanList.with(IteratorTest.booleans);
		EditingBooleanIterator fil =
			new EditingBooleanIterator(data.iterator(), (boolean s) -> !s);
		BooleanList next = new BooleanList();
		next.addAll(fil);
		Assert.assertFalse(next.first());
		fil.set(data.iterator(), (boolean s) -> !s);
		next.clear();
		next.addAll(fil);
		Assert.assertTrue(next.get(1));
	}
}

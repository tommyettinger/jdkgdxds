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

public class FilteringIteratorTest {
	@Test
	public void testObjFilteringIterator() {
		ObjectList<String> data = ObjectList.with(IteratorTest.strings);
		FilteringIterator<String> fil =
			new FilteringIterator<>(data.iterator(), (String s) -> s.length() == 2);
		ObjectList<String> next = new ObjectList<>();
		next.addAll(fil);
		Assert.assertEquals(4, next.size());
		fil.set(data.iterator(), (String s) -> s.charAt(0) == 'p');
		next.clear();
		next.addAll(fil);
		Assert.assertEquals(3, next.size());
	}

	@Test
	public void testFilteringLongIterator() {
		LongList data = LongList.with(IteratorTest.longs);
		FilteringLongIterator fil =
			new FilteringLongIterator(data.iterator(), (long s) -> s % 50 == 25);
		LongList next = new LongList();
		next.addAll(fil);
		Assert.assertEquals(1, next.size());
		fil.set(data.iterator(), (long s) -> s % 10 == 1);
		next.clear();
		next.addAll(fil);
		Assert.assertEquals(2, next.size());
	}

	@Test
	public void testFilteringIntIterator() {
		IntList data = IntList.with(IteratorTest.ints);
		FilteringIntIterator fil =
			new FilteringIntIterator(data.iterator(), (int s) -> s % 50 == 25);
		IntList next = new IntList();
		next.addAll(fil);
		Assert.assertEquals(1, next.size());
		fil.set(data.iterator(), (int s) -> s % 10 == 1);
		next.clear();
		next.addAll(fil);
		Assert.assertEquals(2, next.size());
	}

	@Test
	public void testFilteringShortIterator() {
		ShortList data = ShortList.with(IteratorTest.shorts);
		FilteringShortIterator fil =
			new FilteringShortIterator(data.iterator(), (short s) -> s % 50 == 25);
		ShortList next = new ShortList();
		next.addAll(fil);
		Assert.assertEquals(1, next.size());
		fil.set(data.iterator(), (short s) -> s % 10 == 1);
		next.clear();
		next.addAll(fil);
		Assert.assertEquals(2, next.size());
	}

	@Test
	public void testFilteringByteIterator() {
		ByteList data = ByteList.with(IteratorTest.bytes);
		FilteringByteIterator fil =
			new FilteringByteIterator(data.iterator(), (byte s) -> s % 50 == 13);
		ByteList next = new ByteList();
		next.addAll(fil);
		Assert.assertEquals(2, next.size());
		fil.set(data.iterator(), (byte s) -> s % 10 == 0);
		next.clear();
		next.addAll(fil);
		Assert.assertEquals(3, next.size());
	}

	@Test
	public void testFilteringFloatIterator() {
		FloatList data = FloatList.with(IteratorTest.floats);
		FilteringFloatIterator fil =
			new FilteringFloatIterator(data.iterator(), (float s) -> s % 50 == 25);
		FloatList next = new FloatList();
		next.addAll(fil);
		Assert.assertEquals(1, next.size());
		fil.set(data.iterator(), (float s) -> s % 10 == 1);
		next.clear();
		next.addAll(fil);
		Assert.assertEquals(2, next.size());
	}

	@Test
	public void testFilteringDoubleIterator() {
		DoubleList data = DoubleList.with(IteratorTest.doubles);
		FilteringDoubleIterator fil =
			new FilteringDoubleIterator(data.iterator(), (double s) -> s % 50 == 25);
		DoubleList next = new DoubleList();
		next.addAll(fil);
		Assert.assertEquals(1, next.size());
		fil.set(data.iterator(), (double s) -> s % 10 == 1);
		next.clear();
		next.addAll(fil);
		Assert.assertEquals(2, next.size());
	}

	@Test
	public void testFilteringCharIterator() {
		CharList data = CharList.with(IteratorTest.chars);
		FilteringCharIterator fil =
			new FilteringCharIterator(data.iterator(), (char s) -> s % 50 == 25);
		CharList next = new CharList();
		next.addAll(fil);
		Assert.assertEquals(1, next.size());
		fil.set(data.iterator(), (char s) -> s % 10 == 1);
		next.clear();
		next.addAll(fil);
		Assert.assertEquals(2, next.size());
	}

	@Test
	public void testFilteringBooleanIterator() {
		BooleanList data = BooleanList.with(IteratorTest.booleans);
		FilteringBooleanIterator fil =
			new FilteringBooleanIterator(data.iterator(), (boolean s) -> s);
		BooleanList next = new BooleanList();
		next.addAll(fil);
		Assert.assertEquals(9, next.size());
		fil.set(data.iterator(), (boolean s) -> !s);
		next.clear();
		next.addAll(fil);
		Assert.assertEquals(7, next.size());
	}
}

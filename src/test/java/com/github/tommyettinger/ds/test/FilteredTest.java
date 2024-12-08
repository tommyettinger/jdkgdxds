/*
 * Copyright (c) 2023 See AUTHORS file.
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

package com.github.tommyettinger.ds.test;

import com.github.tommyettinger.ds.*;
import org.junit.Assert;
import org.junit.Test;

public class FilteredTest {
	@Test
	public void testIterableSet() {
		FilteredIterableSet<String, Iterable<String>> fil = FilteredIterableSet.with(
			(String s) -> s.length() > 3, String::toUpperCase,
			ObjectList.with("zzz", "bee", "binturong"),
			ObjectDeque.with("hm?", "bee", "BINTURONG"),
			ObjectList.with(":D", "bee", "Aardvark", "bandicoot")
			);
		Assert.assertEquals(2, fil.size());

		fil.add(ObjectList.with("let's"));
		fil.add(ObjectList.with("get this"));
		fil.add(ObjectList.with("party"));
		fil.add(ObjectList.with("started"));
	}
	@Test
	public void testStringSet() {
		FilteredStringSet fil = FilteredStringSet.with(
			CharFilter.getOrCreate("CAPS", (c) -> true, Character::toUpperCase),
			"zzz", "bee", "binturong",
			"hm?", "bee", "BINTURONG");
		Assert.assertEquals(4, fil.size());

		fil.add("let's");
		fil.add("get this");
		fil.add("party");
		fil.add("started");
		fil.add("nn nn");
		fil.add("tss tss");
		fil.add("wub wub");
		fil.add("WRRRRR");
	}

	/**
	 * This is a little odd... the key isn't updated but the value is... The keys are considered equivalent, though.
	 */
	@Test
	public void testIterableMap() {
		FilteredIterableMap<String, Iterable<String>, Integer> fil = FilteredIterableMap.with(
			(String s) -> s.length() > 3, String::toUpperCase,
			ObjectList.with("zzz", "bee", "binturong"), -1,
				new Object[]{
			ObjectDeque.with("hm?", "bee", "BINTURONG"), 1,
			ObjectList.with(":D", "bee", "Aardvark", "bandicoot"), 2}
			);
		System.out.println(fil);
		Assert.assertEquals(2, fil.size());

		fil = FilteredIterableMap.with(
				(String s) -> s.length() > 3, String::toUpperCase,
				ObjectList.with("zzz", "bee", "binturong"), 1234,
				ObjectList.with("hm?", "bee", "BINTURONG"), -5678,
				ObjectList.with(":D", "bee", "Aardvark", "bandicoot"), Integer.MIN_VALUE
		);

		System.out.println(fil);
		Assert.assertEquals(2, fil.size());



	}
	@Test
	public void testIterableSetSubtype() {
		FilteredIterableSet<String, ObjectList<String>> fil = FilteredIterableSet.with(
			(String s) -> s.length() > 3, String::toUpperCase, new Iterable[]{
			ObjectList.with("zzz", "bee", "binturong"),
			ObjectList.with("hm?", "bee", "BINTURONG"),
			ObjectList.with(":D", "bee", "Aardvark", "bandicoot")}
			);
		Assert.assertEquals(2, fil.size());
	}
	@Test
	public void testIterableMapSubtype() {
		FilteredIterableMap<String, ObjectList<String>, Integer> fil = FilteredIterableMap.with(
			(String s) -> s.length() > 3, String::toUpperCase,
			ObjectList.with("zzz", "bee", "binturong"), -1,
				new Object[]{
			ObjectList.with("hm?", "bee", "BINTURONG"), 1,
			ObjectList.with(":D", "bee", "Aardvark", "bandicoot"), 2}
		);
		System.out.println(fil);
		Assert.assertEquals(2, fil.size());
	}
}

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

import com.github.tommyettinger.ds.FilteredIterableMap;
import com.github.tommyettinger.ds.FilteredIterableSet;
import com.github.tommyettinger.ds.ObjectDeque;
import com.github.tommyettinger.ds.ObjectList;
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
	}

	/**
	 * This is a little odd... the key isn't updated but the value is... The keys are considered equivalent, though.
	 */
	@Test
	public void testIterableMap() {
		FilteredIterableMap<String, Iterable<String>, Integer> fil = FilteredIterableMap.with(
			(String s) -> s.length() > 3, String::toUpperCase,
			ObjectList.with("zzz", "bee", "binturong"), -1,
			ObjectDeque.with("hm?", "bee", "BINTURONG"), 1,
			ObjectList.with(":D", "bee", "Aardvark", "bandicoot"), 2
			);
		System.out.println(fil);
		Assert.assertEquals(2, fil.size());
	}
	@Test
	public void testIterableSetSubtype() {
		FilteredIterableSet<String, ObjectList<String>> fil = FilteredIterableSet.with(
			(String s) -> s.length() > 3, String::toUpperCase,
			ObjectList.with("zzz", "bee", "binturong"),
			ObjectList.with("hm?", "bee", "BINTURONG"),
			ObjectList.with(":D", "bee", "Aardvark", "bandicoot")
			);
		Assert.assertEquals(2, fil.size());
	}
	@Test
	public void testIterableMapSubtype() {
		FilteredIterableMap<String, ObjectList<String>, Integer> fil = FilteredIterableMap.with(
			(String s) -> s.length() > 3, String::toUpperCase,
			ObjectList.with("zzz", "bee", "binturong"), -1,
			ObjectList.with("hm?", "bee", "BINTURONG"), 1,
			ObjectList.with(":D", "bee", "Aardvark", "bandicoot"), 2
		);
		System.out.println(fil);
		Assert.assertEquals(2, fil.size());
	}
}

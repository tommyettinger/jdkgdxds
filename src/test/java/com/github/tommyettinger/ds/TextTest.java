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

package com.github.tommyettinger.ds;

import org.junit.Assert;
import org.junit.Test;

public class TextTest {
	public static void subTest(CharList list) {
		list.append("dolla dolla bill, y'all");
		System.out.println(list.getClass() + ": " + list.toDenseString());
		Assert.assertEquals(-1, list.indexOf("problems"));
		Assert.assertEquals(-1, list.indexOf("problems indicative of higher-order structural flaws in society"));
		Assert.assertEquals(0, list.indexOf("dolla"));
		Assert.assertEquals(5, list.indexOf(' '));
		Assert.assertEquals(5, list.indexOf(" "));
		Assert.assertEquals(12, list.indexOf("bill"));
		list.insert(0, '$');
		System.out.println(list.getClass() + ": " + list.toDenseString());
		Assert.assertEquals(1, list.indexOf("dolla"));
		Assert.assertEquals(6, list.indexOf(' '));
		Assert.assertEquals(6, list.indexOf(" "));
		Assert.assertEquals(13, list.indexOf("bill"));
		list.insert(0, '$');
		System.out.println(list.getClass() + ": " + list.toDenseString());
		Assert.assertEquals(2, list.indexOf("dolla"));
		Assert.assertEquals(7, list.indexOf(' '));
		Assert.assertEquals(7, list.indexOf(" "));
		Assert.assertEquals(14, list.indexOf("bill"));
		list.removeAt(0);
		list.removeAt(0);
		list.removeAt(0);
		System.out.println(list.getClass() + ": " + list.toDenseString());
		Assert.assertEquals(5, list.indexOf("dolla"));
		Assert.assertEquals(4, list.indexOf(' '));
		Assert.assertEquals(4, list.indexOf(" "));
		Assert.assertEquals(11, list.indexOf("bill"));
		list.removeAt(0);
		System.out.println(list.getClass() + ": " + list.toDenseString());
		Assert.assertEquals(4, list.indexOf("dolla"));
		Assert.assertEquals(3, list.indexOf(' '));
		Assert.assertEquals(3, list.indexOf(" "));
		Assert.assertEquals(10, list.indexOf("bill"));
	}

	@Test
	public void testCharLists() {
		subTest(new CharList(25));
//		subTest(new CharBag(25)); // doesn't work because of insert() behavior
		subTest(new CharDeque(25));
	}
}

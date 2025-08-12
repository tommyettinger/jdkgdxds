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

import com.github.tommyettinger.ds.IntList;
import com.github.tommyettinger.ds.ObjectList;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;

public class ListRemoveTest {
	@Test
	public void testArrayListRemoveAll() {
		ArrayList<String> list = new ArrayList<>(ObjectList.with(
			"foo", "bar", "baz", "quux",
			"foo", "bar", "baz", "quux",
			"foo", "bar", "baz", "quux"));
		ArrayList<String> remover = new ArrayList<>(ObjectList.with("foo", "bar", "baz", "quux"));
		Assert.assertEquals(list.size(), 12);
		for (String s : remover) list.remove(s);
		Assert.assertEquals(list.size(), 8);
		System.out.println(list);
		list.removeAll(ObjectList.with("foo", "foo"));
		Assert.assertEquals(list.size(), 6);
		Assert.assertFalse(list.contains("foo"));
		Assert.assertTrue(list.contains("bar"));
		Assert.assertTrue(list.contains("baz"));
		Assert.assertTrue(list.contains("quux"));
	}

	@Test
	public void testObjectListRemoveEach() {
		ObjectList<String> list = ObjectList.with(
			"foo", "bar", "baz", "quux",
			"foo", "bar", "baz", "quux",
			"foo", "bar", "baz", "quux");
		ObjectList<String> remover = ObjectList.with("foo", "bar", "baz", "quux");
		Assert.assertEquals(list.size(), 12);
		list.removeEachIterable(remover);
		Assert.assertEquals(list.size(), 8);
		System.out.println(list);
		list.removeAll(ObjectList.with("foo", "foo"));
		Assert.assertEquals(list.size(), 6);
		Assert.assertFalse(list.contains("foo"));
		Assert.assertTrue(list.contains("bar"));
		Assert.assertTrue(list.contains("baz"));
		Assert.assertTrue(list.contains("quux"));
	}

	@Test
	public void testIntListRemoveAll() {
		IntList list = IntList.with(
			1, 2, 3, 4,
			1, 2, 3, 4,
			1, 2, 3, 4);
		IntList remover = IntList.with(1, 2, 3);
		Assert.assertEquals(list.size(), 12);
		list.removeAll(remover);
		Assert.assertEquals(list.size(), 3);
		System.out.println(list);
		list.removeEach(IntList.with(4));
		Assert.assertEquals(list.size(), 2);
		Assert.assertFalse(list.contains(1));
		Assert.assertFalse(list.contains(2));
		Assert.assertFalse(list.contains(3));
		Assert.assertTrue(list.contains(4));
	}

	@Test()
	public void testIntListRemoveEach() {
		IntList list = IntList.with(
			1, 2, 3, 4,
			1, 2, 3, 4,
			1, 2, 3, 4);
		IntList remover = IntList.with(1, 2, 3, 4);
		Assert.assertEquals(list.size(), 12);
		list.removeEach(remover);
		Assert.assertEquals(list.size(), 8);
		System.out.println(list);
		list.removeEach(IntList.with(1, 1));
		Assert.assertEquals(list.size(), 6);
		Assert.assertFalse(list.contains(1));
		Assert.assertTrue(list.contains(2));
		Assert.assertTrue(list.contains(3));
		Assert.assertTrue(list.contains(4));
	}
}

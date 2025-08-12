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

import com.github.tommyettinger.ds.IntOrderedSet;
import com.github.tommyettinger.ds.LongOrderedSet;
import com.github.tommyettinger.ds.ObjectList;
import com.github.tommyettinger.ds.ObjectObjectOrderedMap;
import com.github.tommyettinger.ds.ObjectOrderedSet;
import org.junit.Assert;
import org.junit.Test;

public class RetainTest {
	@Test
	public void testObjectObjectOrderedMapRetainKeys() {
		ObjectObjectOrderedMap<String, String> map = ObjectObjectOrderedMap.with("foo", "wizard", "bar", "people", "baz", "dear", "quux", "readers");
		Assert.assertEquals(map.size(), 4);
		ObjectList<String> half = new ObjectList<>(map.order());
		half.truncate(2);
		Assert.assertEquals(half.size(), 2);
		map.keySet().retainAll(half);
		Assert.assertEquals(map.size(), 2);
		Assert.assertTrue(map.containsKey("foo"));
		Assert.assertTrue(map.containsKey("bar"));
		Assert.assertFalse(map.containsKey("baz"));
		Assert.assertFalse(map.containsKey("quux"));
	}

	@Test
	public void testObjectObjectOrderedMapRemoveRange() {
		ObjectObjectOrderedMap<String, String> map = ObjectObjectOrderedMap.with("foo", "wizard", "bar", "people", "baz", "dear", "quux", "readers");
		Assert.assertEquals(map.size(), 4);
		map.removeRange(1, 3);
		Assert.assertEquals(map.size(), 2);
		Assert.assertTrue(map.containsKey("foo"));
		Assert.assertFalse(map.containsKey("bar"));
		Assert.assertFalse(map.containsKey("baz"));
		Assert.assertTrue(map.containsKey("quux"));
	}

	@Test
	public void testObjectOrderedSetRemoveRange() {
		ObjectOrderedSet<String> set = ObjectOrderedSet.with("foo", "bar", "baz", "quux");
		Assert.assertEquals(set.size(), 4);
		set.removeRange(1, 3);
		Assert.assertEquals(set.size(), 2);
		System.out.println(set);
		Assert.assertTrue(set.contains("foo"));
		Assert.assertFalse(set.contains("bar"));
		Assert.assertFalse(set.contains("baz"));
		Assert.assertTrue(set.contains("quux"));
	}

	@Test
	public void testIntOrderedSetRemoveRange() {
		IntOrderedSet set = IntOrderedSet.with(new int[]{1, 111, 11111, 1111111, 2, 222, 22222});
		Assert.assertEquals(set.size(), 7);
		set.truncate(4);
		Assert.assertEquals(set.size(), 4);
		set.removeRange(1, 3);
		Assert.assertEquals(set.size(), 2);
		System.out.println(set);
		Assert.assertTrue(set.contains(1));
		Assert.assertFalse(set.contains(111));
		Assert.assertFalse(set.contains(11111));
		Assert.assertTrue(set.contains(1111111));
	}

	@Test
	public void testLongOrderedSetRemoveRange() {
		LongOrderedSet set = LongOrderedSet.with(1, 111, 11111, 1111111);
		Assert.assertEquals(set.size(), 4);
		set.removeRange(1, 3);
		Assert.assertEquals(set.size(), 2);
		System.out.println(set);
		Assert.assertTrue(set.contains(1));
		Assert.assertFalse(set.contains(111));
		Assert.assertFalse(set.contains(11111));
		Assert.assertTrue(set.contains(1111111));
	}
}

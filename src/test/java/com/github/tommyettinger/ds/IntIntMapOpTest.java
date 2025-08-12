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

import com.github.tommyettinger.random.AceRandom;
import org.junit.Assert;
import org.junit.Test;

public class IntIntMapOpTest {
	private static final int LIMIT = 1000;

	@Test
	public void addThenRemoveTest() {
		AceRandom random = new AceRandom(123);
		IntIntMap map = new IntIntMap(16);
		IntList neo = new IntList(LIMIT), age = new IntList(LIMIT);
		com.github.tommyettinger.ds.old.IntIntMap old = new com.github.tommyettinger.ds.old.IntIntMap(16);
		Assert.assertTrue(map.keySet().equalContents(old.keySet()));
		neo.clear();
		neo.addAll(map.values());
		neo.sort();
		age.clear();
		age.addAll(map.values());
		age.sort();
		Assert.assertEquals(neo, age);

		for (int i = 0; i < LIMIT; i++) {
			int randomKey = random.next(9) - 200;
			int randomValue = random.nextInt();
			map.put(randomKey, randomValue);
			old.put(randomKey, randomValue);
			Assert.assertTrue(map.keySet().equalContents(old.keySet()));
			neo.clear();
			neo.addAll(map.values());
			neo.sort();
			age.clear();
			age.addAll(map.values());
			age.sort();
			Assert.assertEquals(neo, age);

		}
		System.out.printf("After adding, each map has %d items, with keyTable length %d\n", map.size, map.keyTable.length);
		for (int i = 0; i < LIMIT; i++) {
			int randomKey = random.next(9) - 200;
			map.remove(randomKey);
			old.remove(randomKey);
			Assert.assertTrue(map.keySet().equalContents(old.keySet()));
			neo.clear();
			neo.addAll(map.values());
			neo.sort();
			age.clear();
			age.addAll(map.values());
			age.sort();
			Assert.assertEquals(neo, age);

		}
		System.out.printf("After removing, each map has %d items, with keyTable length %d\n", map.size, map.keyTable.length);
	}

	@Test
	public void mixedAddRemoveTest() {
		AceRandom random = new AceRandom(123);
		IntList neo = new IntList(LIMIT), age = new IntList(LIMIT);
		IntIntMap map = new IntIntMap(16);
		com.github.tommyettinger.ds.old.IntIntMap old = new com.github.tommyettinger.ds.old.IntIntMap(16);
		Assert.assertTrue(map.keySet().equalContents(old.keySet()));
		neo.clear();
		neo.addAll(map.values());
		neo.sort();
		age.clear();
		age.addAll(map.values());
		age.sort();
		Assert.assertEquals(neo, age);

		for (int i = 0; i < LIMIT; i++) {
			int randomKey = random.next(7) << 20;
			int randomValue = random.nextInt();
			if (random.nextBoolean(0.7f)) {
				map.put(randomKey, randomValue);
				old.put(randomKey, randomValue);
			} else {
				map.remove(randomKey);
				old.remove(randomKey);
			}
			Assert.assertTrue(map.keySet().equalContents(old.keySet()));
			neo.clear();
			neo.addAll(map.values());
			neo.sort();
			age.clear();
			age.addAll(map.values());
			age.sort();
			Assert.assertEquals(neo, age);

		}
		System.out.printf("Each map has %d items, with keyTable length %d\n", map.size, map.keyTable.length);
	}
}

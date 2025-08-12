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

import com.github.tommyettinger.digital.ArrayTools;
import com.github.tommyettinger.random.AceRandom;
import org.junit.Assert;
import org.junit.Test;

public class ObjectSetOpTest {
	@Test
	public void addThenRemoveTest() {
		AceRandom random = new AceRandom(123);
		ObjectSet<String> set = new ObjectSet<>(16);
		com.github.tommyettinger.ds.old.ObjectSet<String> old = new com.github.tommyettinger.ds.old.ObjectSet<>(16);
		Assert.assertArrayEquals(set.keyTable, old.keyTable);
		for (int i = 0; i < 1000; i++) {
			String randomValue = ArrayTools.stringAt(random.nextInt(428));
			set.add(randomValue);
			old.add(randomValue);
			Assert.assertEquals(set, old);
		}
		System.out.printf("After adding, each set has %d items, with table size %d\n", set.size, set.getTableSize());
		for (int i = 0; i < 1000; i++) {
			String randomValue = ArrayTools.stringAt(random.nextInt(428));
			set.remove(randomValue);
			old.remove(randomValue);
			Assert.assertEquals(set, old);
		}
		System.out.printf("After removing, each set has %d items, with table size %d\n", set.size, set.getTableSize());
	}

	@Test
	public void mixedAddRemoveTest() {
		AceRandom random = new AceRandom(123);
		ObjectSet<String> set = new ObjectSet<>(16);
		com.github.tommyettinger.ds.old.ObjectSet<String> old = new com.github.tommyettinger.ds.old.ObjectSet<>(16);
		Assert.assertEquals(set, old);
		for (int i = 0; i < 10000; i++) {
			String randomValue = ArrayTools.stringAt(random.nextInt(428));
			if (random.nextBoolean(0.7f)) {
				set.add(randomValue);
				old.add(randomValue);
			} else {
				set.remove(randomValue);
				old.remove(randomValue);
			}
			Assert.assertEquals(set, old);
		}
		System.out.printf("Each set has %d items, with table size %d\n", set.size, set.getTableSize());
	}
}

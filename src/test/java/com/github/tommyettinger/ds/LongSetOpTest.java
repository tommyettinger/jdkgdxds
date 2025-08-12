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

public class LongSetOpTest {
	private static final int LIMIT = 1000;

	@Test
	public void addThenRemoveTest() {
		AceRandom random = new AceRandom(123);
		LongSet set = new LongSet(16);
		com.github.tommyettinger.ds.old.LongSet old = new com.github.tommyettinger.ds.old.LongSet(16);
		Assert.assertTrue(set.equalContents(old));
		for (int i = 0; i < LIMIT; i++) {
			long randomValue = random.nextLong();
			randomValue ^= (randomValue << 32 | randomValue >>> 32);
			set.add(randomValue);
			old.add(randomValue);
			Assert.assertTrue(set.equalContents(old));
		}
		System.out.printf("After adding, each set has %d items, with keyTable length %d\n", set.size, set.keyTable.length);
		for (int i = 0; i < LIMIT; i++) {
			long randomValue = random.nextLong();
			randomValue ^= (randomValue << 32 | randomValue >>> 32);
			set.remove(randomValue);
			old.remove(randomValue);
			Assert.assertTrue(set.equalContents(old));
		}
		System.out.printf("After removing, each set has %d items, with keyTable length %d\n", set.size, set.keyTable.length);
	}

	@Test
	public void mixedAddRemoveTest() {
		AceRandom random = new AceRandom(123);
		LongSet set = new LongSet(16);
		com.github.tommyettinger.ds.old.LongSet old = new com.github.tommyettinger.ds.old.LongSet(16);
		Assert.assertTrue(set.equalContents(old));
		for (int i = 0; i < LIMIT; i++) {
			long randomValue = random.nextLong();
			randomValue ^= (randomValue << 32 | randomValue >>> 32);
			if (random.nextBoolean(0.7f)) {
				set.add(randomValue);
				old.add(randomValue);
			} else {
				set.remove(randomValue);
				old.remove(randomValue);
			}
			Assert.assertTrue(set.equalContents(old));
		}
		System.out.printf("Each set has %d items, with keyTable length %d\n", set.size, set.keyTable.length);
	}
}

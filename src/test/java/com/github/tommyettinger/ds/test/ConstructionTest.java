/*
 * Copyright (c) 2023-2025 See AUTHORS file.
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

import com.github.tommyettinger.digital.ArrayTools;
import com.github.tommyettinger.ds.*;
import org.junit.Assert;
import org.junit.Test;

public class ConstructionTest {
	@Test
	public void testObjectEmpty() {
		final String[] names = ArrayTools.chemicalElements(0, 118, false);
		final String[] namesUpper = ArrayTools.chemicalElements(0, 118, true);
		final int targetLength = names.length + 1;

		ObjectList<String> list = new ObjectList<>(0);
		list.add("START");
		list.addAll(names);
		Assert.assertEquals(targetLength, list.size());

		ObjectBag<String> bag = new ObjectBag<>(0);
		bag.add("START");
		bag.addAll(names);
		Assert.assertEquals(targetLength, bag.size());

		ObjectDeque<String> deque = new ObjectDeque<>(0);
		deque.add("START");
		deque.addAll(names);
		Assert.assertEquals(targetLength, deque.size());

		ObjectSet<String> set = new ObjectSet<>(0);
		set.add("START");
		set.addAll(names);
		Assert.assertEquals(targetLength, set.size());

		ObjectOrderedSet<String> orderedSet = new ObjectOrderedSet<>(0);
		orderedSet.add("START");
		orderedSet.addAll(names);
		Assert.assertEquals(targetLength, orderedSet.size());

		NumberedSet<String> numberedSet = new NumberedSet<>(0);
		numberedSet.add("START");
		numberedSet.addAll(names);
		Assert.assertEquals(targetLength, numberedSet.size());

		ObjectObjectMap<String, String> map = new ObjectObjectMap<>(0);
		map.put("start", "START");
		map.putAll(names, namesUpper);
		Assert.assertEquals(targetLength, map.size());

		ObjectObjectOrderedMap<String, String> orderedMap = new ObjectObjectOrderedMap<>(0);
		orderedMap.put("start", "START");
		orderedMap.putAll(names, namesUpper);
		Assert.assertEquals(targetLength, orderedMap.size());
	}
}

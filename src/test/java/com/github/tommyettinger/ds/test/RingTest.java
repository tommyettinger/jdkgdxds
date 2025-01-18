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

import com.github.tommyettinger.ds.ObjectRing;
import org.junit.Assert;
import org.junit.Test;

public class RingTest {
	@Test
	public void testBasics() {
		ObjectRing<String> deque = new ObjectRing<>(5);
		deque.add("Carl");
		deque.add("John");
		deque.add("theodore");
		deque.add("jamie");
		deque.add("bob");
		deque.add("alice");
		deque.add("carol");
		deque.add("billy");
		System.out.println(deque);
		Assert.assertEquals("billy", deque.getLast());
		Assert.assertEquals("jamie", deque.getFirst());
	}
}
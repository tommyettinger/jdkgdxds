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

import java.util.TreeSet;

public class TreeSetQuestionTest {
	public static void main(String[] args) {
		TreeSet<Object> set = new TreeSet<>((a, b) -> 0);
		set.add("merry christmas");
		set.add(new Object());

		Object o = new Object();
		set.add(o);
		set.add(o);
		set.add(o);
		set.add("whee");
		System.out.println(set.size());
		System.out.println(set);
		// size is 1, each item where the comparator returned 0 won't replace anything
		// prints [merry christmas]
	}
}

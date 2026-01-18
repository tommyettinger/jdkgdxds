/*
 * Copyright (c) 2022-2025 See AUTHORS file.
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

import com.github.tommyettinger.random.WhiskerRandom;
import com.github.tommyettinger.textra.utils.CaseInsensitiveIntMap228;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

/**
 * Using initial capacity 235970 and load factor 0.6f...
 * 2.2.8 map gets total collisions: 0, PILEUP: 0
 * 11581639000 ns taken for 1000 ops
 * <br>
 * Using initial capacity 58992 and load factor 0.6f...
 * 2.2.8 map gets total collisions: 50277, PILEUP: 17
 * 26009535500 ns taken for 1000 ops
 * <br>
 * Using initial capacity 230 and load factor 0.6f...
 * 2.2.8 map gets total collisions: 67118, PILEUP: 17
 * 28378377500 ns taken for 1000 ops
 * <br>
 * Using initial capacity 230 and load factor 0.8f...
 * 2.2.8 map gets total collisions: 140569, PILEUP: 23
 * 36598554700 ns taken for 1000 ops
 * <br>
 * Using initial capacity 230 and load factor 0.5f...
 * 2.2.8 map gets total collisions: 43191, PILEUP: 13
 * 23581745600 ns taken for 1000 ops
 */
public class CaseInsensitiveIntMap228CollisionTest {
	public static void main(String[] args) throws IOException {
		final List<String> words = Files.readAllLines(Paths.get("src/test/resources/word_list.txt"));
		WhiskerRandom rng = new WhiskerRandom(1234567890L);
		Collections.shuffle(words, rng);
		final int CAPACITY = words.size() >> 10;
		System.out.println("Using initial capacity " + CAPACITY + " and load factor 0.5f...");
		long start = System.nanoTime();
		for (int it = 0; it < 1000; it++) {
			CaseInsensitiveIntMap228 set = new CaseInsensitiveIntMap228(CAPACITY, 0.5f);
			for (int i = 0, n = words.size(); i < n; i++) {
				set.put(words.get(i), i);
			}
			if(it == 0)
				set.clear();
		}
		System.out.println((System.nanoTime() - start) + " ns taken for 1000 ops");
	}

}

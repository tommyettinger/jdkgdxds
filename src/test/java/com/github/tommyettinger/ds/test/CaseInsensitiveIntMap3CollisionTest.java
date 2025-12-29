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
import com.github.tommyettinger.textra.utils.CaseInsensitiveIntMap3;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

/**
 * Using initial capacity 235970 and load factor 0.6f...
 * Revision 3 map gets total collisions: 0, PILEUP: 0
 * 9898430300 ns taken for 1000 ops
 * <br>
 * Using initial capacity 58992 and load factor 0.6f...
 * Revision 3 map gets total collisions: 49944, PILEUP: 10
 * 20365499600 ns taken for 1000 ops
 * <br>
 * Using initial capacity 230 and load factor 0.6f...
 * Revision 3 map gets total collisions: 66637, PILEUP: 13
 * 23372192300 ns taken for 1000 ops
 */
public class CaseInsensitiveIntMap3CollisionTest {
	public static void main(String[] args) throws IOException {
		final List<String> words = Files.readAllLines(Paths.get("src/test/resources/word_list.txt"));
		WhiskerRandom rng = new WhiskerRandom(1234567890L);
		Collections.shuffle(words, rng);
		final int CAPACITY = words.size() >> 10;
		System.out.println("Using initial capacity " + CAPACITY + " and load factor 0.6f...");
		long start = System.nanoTime();
		for (int it = 0; it < 1000; it++) {
//		for (int sh = 0; sh < 512; sh++) {
			CaseInsensitiveIntMap3 set = new CaseInsensitiveIntMap3(CAPACITY, 0.6f);
//			set.shift = sh;
			for (int i = 0, n = words.size(); i < n; i++) {
				set.put(words.get(i), i);
			}
			if(it == 0)
				set.clear();
		}
		System.out.println((System.nanoTime() - start) + " ns taken for 1000 ops");
	}

}

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

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import com.github.tommyettinger.digital.Hasher;
import com.github.tommyettinger.ds.IntList;

/**
 * With h = 0:
 * Good multipliers for simpleHash(): [2913, 4637, 6197, 8663, 9865]
 * With h = len:
 * Good multipliers for simpleHash(): [541, 2357, 2733, 3927, 5983, 6123]
 * With h = len, and XOR instead of add:
 * Good multipliers for simpleHash(): [809, 877, 1399, 2083, 3185, 3529, 4397, 4733, 4873, 5273, 9925]
 * <br>
 * Good seeds for hash(): [14, 276, 1562, 2327, 2713, 2750, 2765, 2785]
 * <br>
 * Good seeds for hashBulk(): [17, 564, 1127, 1663, 1859, 2269, 4345, 4827, 4919]
 */
public class WordListStats {
	public static class SimpleHasher {
		public final int mul;
		public SimpleHasher(int seed){
			mul = seed * 2 + 1;
		}
		public int simpleHash(String s) {
			final int len = s.length();
			int h = len;
			for (int i = 0; i < len; i++) {
				h = h * mul ^ s.charAt(i);
			}
			return h;
		}
	}

	public static void main(String[] args) throws IOException {
		final List<String> words = Files.readAllLines(Paths.get("src/test/resources/word_list.txt"));
		int wordCount = words.size();
		System.out.printf("Number of words   : %d\n", wordCount);
		IntList goodSimpleHashMultipliers = new IntList();
		IntList goodHashSeeds = new IntList();
		IntList goodHashBulkSeeds = new IntList();
		for (int i = 1; i <= 5000; i++) {
			SimpleHasher op = new SimpleHasher(i);
			long collisionCount = wordCount - words.parallelStream().mapToInt(op::simpleHash).distinct().count();
			if(collisionCount == 0) {
				System.out.print("SimpleHasher.simpleHash() with mul " + op.mul);
				System.out.println(BigInteger.valueOf(op.mul).isProbablePrime(9) ? " (PRIME!)" : "");
				goodSimpleHashMultipliers.add(op.mul);
			}
		}
		System.out.println("Good multipliers for simpleHash(): " + goodSimpleHashMultipliers);

//		for (int i = 0; i < 5000; i++) {
//			Hasher op = new Hasher(i);
//			long collisionCount = wordCount - words.parallelStream().mapToInt(op::hash).distinct().count();
//			if(collisionCount == 0) {
//				System.out.println("Hasher.hash() with seed " + i);
//				goodHashSeeds.add(i);
//			}
//		}
//		System.out.println("Good seeds for hash(): " + goodHashSeeds);
//
//		for (int i = 0; i < 5000; i++) {
//			Hasher op = new Hasher(i);
//			long collisionCount = wordCount - words.parallelStream().mapToInt(op::hashBulk).distinct().count();
//			if(collisionCount == 0) {
//				System.out.println("Hasher.hashBulk() with seed " + i);
//				goodHashBulkSeeds.add(i);
//			}
//		}
//		System.out.println("Good seeds for hashBulk(): " + goodHashBulkSeeds);
	}
}

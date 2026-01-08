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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import com.github.tommyettinger.digital.Hasher;
import com.github.tommyettinger.ds.IntList;

/**
 * Good seeds for hash(): [14, 276, 1562, 2327, 2713, 2750, 2765, 2785]
 * Good seeds for hashBulk(): [17, 564, 1127, 1663, 1859, 2269, 4345, 4827, 4919]
 */
public class WordListStats {
	public static void main(String[] args) throws IOException {
		final List<String> words = Files.readAllLines(Paths.get("src/test/resources/word_list.txt"));
		int wordCount = words.size();
		System.out.printf("Number of words   : %d\n", wordCount);
		IntList goodHashSeeds = new IntList();
		IntList goodHashBulkSeeds = new IntList();
		for (int i = 0; i < 5000; i++) {
			Hasher op = new Hasher(i);
//			System.out.println("Working with new Hasher(" + i + ").hash()");
//			long sum = words.parallelStream().mapToLong(op::hash).sum();
//			double averageHashCode = sum / (double) wordCount;
//			double averageBitCount = words.parallelStream().mapToLong((s) -> Integer.bitCount(op.hash(s))).sum() / (double) wordCount;
//			double averageExtent = words.parallelStream().mapToLong((s) -> 32 - Integer.numberOfLeadingZeros(op.hash(s))).sum() / (double) wordCount;
			long collisionCount = wordCount - words.parallelStream().mapToInt(op::hash).distinct().count();
//			System.out.printf("Number of words   : %d\n", wordCount);
//			System.out.printf("Collision count   : %d\n", collisionCount);
//			System.out.printf("hashCode() sum    : %d\n", sum);
//			System.out.printf("hashCode() average: %10.8f\n", averageHashCode);
//			System.out.printf("bitCount() average: %10.8f\n", averageBitCount);
//			System.out.printf("extent average    : %10.8f\n", averageExtent);

			if(collisionCount == 0) goodHashSeeds.add(i);
		}
		System.out.println("Good seeds for hash(): " + goodHashSeeds);
		for (int i = 0; i < 5000; i++) {
			Hasher op = new Hasher(i);
//			System.out.println("Working with new Hasher(" + i + ").hashBulk()");
//			long sum = words.parallelStream().mapToLong(op::hashBulk).sum();
//			double averageHashCode = sum / (double) wordCount;
//			double averageBitCount = words.parallelStream().mapToLong((s) -> Integer.bitCount(op.hashBulk(s))).sum() / (double) wordCount;
//			double averageExtent = words.parallelStream().mapToLong((s) -> 32 - Integer.numberOfLeadingZeros(op.hashBulk(s))).sum() / (double) wordCount;
			long collisionCount = wordCount - words.parallelStream().mapToInt(op::hashBulk).distinct().count();
//			System.out.printf("Number of words   : %d\n", wordCount);
//			System.out.printf("Collision count   : %d\n", collisionCount);
//			System.out.printf("hashCode() sum    : %d\n", sum);
//			System.out.printf("hashCode() average: %10.8f\n", averageHashCode);
//			System.out.printf("bitCount() average: %10.8f\n", averageBitCount);
//			System.out.printf("extent average    : %10.8f\n", averageExtent);
			if(collisionCount == 0) goodHashBulkSeeds.add(i);
		}
		System.out.println("Good seeds for hashBulk(): " + goodHashBulkSeeds);
	}
}

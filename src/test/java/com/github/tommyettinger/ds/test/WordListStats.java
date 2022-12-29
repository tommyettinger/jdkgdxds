/*
 * Copyright (c) 2022 See AUTHORS file.
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
 *
 */

package com.github.tommyettinger.ds.test;

import com.github.tommyettinger.digital.BitConversion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.IntUnaryOperator;

public class WordListStats {
	public static final IntUnaryOperator[] worse = {
		h -> h, // hash 0, identity
		h -> h & (h ^ h >>> 1), // hash 1, mask with its own gray code
		h -> h & (h << 21 | h >>> 11) & (h << 13 | h >>> 19), // hash 2, ARR
		h -> h & BitConversion.imul(h, h), // hash 3, AQ
		h -> h & (h ^ h << 1), // hash 4, mask with a sort of gray-ish code
		h -> h << 16, // hash 5, imitating float hash behavior in Vector2
		h -> Float.floatToIntBits(h), // hash 6, current BadString behavior
	};
	public static void main(String[] args) throws IOException {
		final List<String> words = Files.readAllLines(Paths.get("src/test/resources/word_list.txt"));
		int wordCount = words.size();
		for (int i = 0; i < worse.length; i++) {
			IntUnaryOperator op = worse[i];
			System.out.println("Working with hash-worsening operator " + i);
			long sum = words.parallelStream().mapToLong(s -> op.applyAsInt(s.hashCode())).sum();
			double averageHashCode = sum / (double)wordCount;
			double averageBitCount = words.parallelStream().mapToLong((s) -> Integer.bitCount(op.applyAsInt(s.hashCode()))).sum() / (double)wordCount;
			double averageExtent = words.parallelStream().mapToLong((s) -> 32 - Integer.numberOfLeadingZeros(op.applyAsInt(s.hashCode()))).sum() / (double)wordCount;
			System.out.printf("Number of words   : %d\n", wordCount);
			System.out.printf("hashCode() sum    : %d\n", sum);
			System.out.printf("hashCode() average: %10.8f\n", averageHashCode);
			System.out.printf("bitCount() average: %10.8f\n", averageBitCount);
			System.out.printf("extent average    : %10.8f\n", averageExtent);
		}
	}
}

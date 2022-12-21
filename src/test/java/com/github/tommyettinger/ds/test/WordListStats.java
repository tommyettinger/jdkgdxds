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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class WordListStats {
	public static void main(String[] args) throws IOException {
		final List<String> words = Files.readAllLines(Paths.get("src/test/resources/word_list.txt"));
		int wordCount = words.size();
		long sum = words.parallelStream().mapToLong(String::hashCode).sum();
		double averageHashCode = sum / (double)wordCount;
		double averageBitCount = words.parallelStream().mapToLong((s) -> Integer.bitCount(s.hashCode())).sum() / (double)wordCount;
		double averageExtent = words.parallelStream().mapToLong((s) -> 32 - Integer.numberOfLeadingZeros(s.hashCode())).sum() / (double)wordCount;
		System.out.printf("Number of words   : %d\n", wordCount);
		System.out.printf("hashCode() sum    : %d\n", sum);
		System.out.printf("hashCode() average: %10.8f\n", averageHashCode);
		System.out.printf("bitCount() average: %10.8f\n", averageBitCount);
		System.out.printf("extent average    : %10.8f\n", averageExtent);
	}

}

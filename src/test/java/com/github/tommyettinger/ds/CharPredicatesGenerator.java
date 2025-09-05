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

public class CharPredicatesGenerator {
	public static void main(String[] args) {
		CharBitSet isLetter = new CharBitSet(Character::isLetter);
		System.out.println("isLetter");
		System.out.println(isLetter.toJavaCode());

		CharBitSet isDigit = new CharBitSet(Character::isDigit);
		System.out.println("isDigit");
		System.out.println(isDigit.toJavaCode());

		CharBitSet isAlphabetic = new CharBitSet(Character::isAlphabetic);
		System.out.println("isAlphabetic");
		System.out.println(isAlphabetic.toJavaCode());

		CharBitSet isWhitespace = new CharBitSet(Character::isWhitespace);
		System.out.println("isWhitespace");
		System.out.println(isWhitespace.toJavaCode());

		CharBitSet isJavaIdentifierStart = new CharBitSet(Character::isJavaIdentifierStart);
		System.out.println("isJavaIdentifierStart");
		System.out.println(isJavaIdentifierStart.toJavaCode());

		CharBitSet isJavaIdentifierPart = new CharBitSet(Character::isJavaIdentifierPart);
		System.out.println("isJavaIdentifierPart");
		System.out.println(isJavaIdentifierPart.toJavaCode());
	}
}

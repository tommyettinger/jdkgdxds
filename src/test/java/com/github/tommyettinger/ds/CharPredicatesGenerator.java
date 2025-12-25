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
		System.out.print("private static CharBitSet isLetter() {\nreturn ");
		System.out.println(isLetter.toJavaCode() + ";\n}");

		CharBitSet isDigit = new CharBitSet(Character::isDigit);
		System.out.print("private static CharBitSet isDigit() {\nreturn ");
		System.out.println(isDigit.toJavaCode() + ";\n}");

		CharBitSet isAlphabetic = new CharBitSet(Character::isAlphabetic);
		System.out.print("private static CharBitSet isAlphabetic() {\nreturn ");
		System.out.println(isAlphabetic.toJavaCode() + ";\n}");

		CharBitSet isWhitespace = new CharBitSet(Character::isWhitespace);
		System.out.print("private static CharBitSet isWhitespace() {\nreturn ");
		System.out.println(isWhitespace.toJavaCode() + ";\n}");

		CharBitSet isJavaIdentifierStart = new CharBitSet(Character::isJavaIdentifierStart);
		System.out.print("private static CharBitSet isJavaIdentifierStart() {\nreturn ");
		System.out.println(isJavaIdentifierStart.toJavaCode() + ";\n}");

		CharBitSet isJavaIdentifierPart = new CharBitSet(Character::isJavaIdentifierPart);
		System.out.print("private static CharBitSet isJavaIdentifierPart() {\nreturn ");
		System.out.println(isJavaIdentifierPart.toJavaCode() + ";\n}");
	}
}

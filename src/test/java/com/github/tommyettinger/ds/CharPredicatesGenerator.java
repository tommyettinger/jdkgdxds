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
		CharBitSetFixedSize isLetter = new CharBitSetFixedSize(Character::isLetter);
		System.out.print("private static CharBitSetFixedSize isLetter() {\nreturn ");
		System.out.println(isLetter.toJavaCode() + ";\n}");

		CharBitSetFixedSize isDigit = new CharBitSetFixedSize(Character::isDigit);
		System.out.print("private static CharBitSetFixedSize isDigit() {\nreturn ");
		System.out.println(isDigit.toJavaCode() + ";\n}");

		CharBitSetFixedSize isAlphabetic = new CharBitSetFixedSize(Character::isAlphabetic);
		System.out.print("private static CharBitSetFixedSize isAlphabetic() {\nreturn ");
		System.out.println(isAlphabetic.toJavaCode() + ";\n}");

		CharBitSetFixedSize isWhitespace = new CharBitSetFixedSize(Character::isWhitespace);
		System.out.print("private static CharBitSetFixedSize isWhitespace() {\nreturn ");
		System.out.println(isWhitespace.toJavaCode() + ";\n}");

		CharBitSetFixedSize isJavaIdentifierStart = new CharBitSetFixedSize(Character::isJavaIdentifierStart);
		System.out.print("private static CharBitSetFixedSize isJavaIdentifierStart() {\nreturn ");
		System.out.println(isJavaIdentifierStart.toJavaCode() + ";\n}");

		CharBitSetFixedSize isJavaIdentifierPart = new CharBitSetFixedSize(Character::isJavaIdentifierPart);
		System.out.print("private static CharBitSetFixedSize isJavaIdentifierPart() {\nreturn ");
		System.out.println(isJavaIdentifierPart.toJavaCode() + ";\n}");
	}
}

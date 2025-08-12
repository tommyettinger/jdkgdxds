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

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A wrapper around a CharSequence (almost always a String) with a typically-worse hashCode() implementation,
 * for stress-testing hash tables that might need better hashes.
 * This currently just calls {@link Float#floatToIntBits(float)} on the result of the inner CharSequence's
 * hashCode(), which is implicitly cast to float before its int bits are obtained. Larger hashes from the
 * inner CharSequence are much more likely to collide just because not all int values can be represented by
 * a float, and the same bits will be used for otherwise identical hashes.
 */
public class BadString implements CharSequence {
	private static int COUNTER = 1;
	public CharSequence text;
	public final int hash;

	public BadString() {
		text = "aaa0";
		hash = 0;
	}

	public BadString(CharSequence text) {
		this.text = text;
		hash = text.toString().hashCode();
//		hash = Hasher.hashBulk(1234567890123456789L, text);
//		hash = Hasher.hash(1234567890123456789L, text);
//		hash = System.identityHashCode(text);
//		hash = COUNTER++;
//		hash = Integer.reverse(COUNTER++);
	}

	@Override
	public int hashCode() {
//            return text.charAt(0);
//            return text.charAt(0) * text.charAt(1);
//            int h = 1;
//            for (int i = 0, n = text.length(); i < n; i++) {
//                h = h * 127 + text.charAt(i);
//            }
//            return h;
//            return (text.hashCode());

//		return Float.floatToIntBits(text.hashCode());

//		return BitConversion.doubleToMixedIntBits(text.hashCode());
		return hash;

//		final int h = text.hashCode();
//		return h & (h * h); // should use BitConversion.imul() if this would run on GWT, but it won't
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		BadString badString = (BadString) o;

		return text.equals(badString.text);
	}

	@Override
	public int length() {
		return text.length();
	}

	@Override
	public char charAt(int index) {
		return text.charAt(index);
	}

	@NonNull
	@Override
	public CharSequence subSequence(int start, int end) {
		return new BadString(text.subSequence(start, end));
	}

	@NonNull
	@Override
	public String toString() {
		return text.toString();
	}
}

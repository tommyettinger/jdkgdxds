/*
 * Copyright (c) 2024-2025 See AUTHORS file.
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

package com.github.tommyettinger.ds.support.util;

import com.github.tommyettinger.digital.Base;

import java.io.IOException;

/**
 * A functional interface that takes and returns an object that is a CharSequence and is Appendable, appending
 * a {@code byte} item to it.
 * This is often a method reference to a method in {@link Base}, such as {@link Base#appendSigned(CharSequence, char)}.
 */
public interface CharAppender {

	/**
	 * Appends {@code item} to {@code sb} and returns {@code sb} for chaining.
	 *
	 * @param sb   an Appendable CharSequence that will be modified, such as a StringBuilder
	 * @param item the item to append
	 * @return {@code sb}, after modification
	 * @param <S>  any type that is both a CharSequence and an Appendable, such as StringBuilder, StringBuffer, CharBuffer, or CharList
	 */
	<S extends CharSequence & Appendable> S apply(S sb, char item);

	/**
	 * Appends one char at a time, without anything around it and without escaping any chars.
	 * This is a static constant to avoid Android and its R8 compiler allocating a new lambda every time
	 * {@code StringBuilder::append} is present at a call-site. This should be used in place of
	 * {@link StringBuilder#append(char)} when you want to use that as a CharAppender.
	 * <br>
	 * This functional interface doesn't have a {@code DENSE} method reference because appending one shown char per char
	 * item is really as dense as you can get already.
	 */
	CharAppender DEFAULT = CharAppender::append;

	/**
	 * Appends the given char in single quotes, with a backslash-escape if it would be necessary in Java sources.
	 */
	CharAppender QUOTED = Base::appendReadable;

	/**
	 * Appends a single char to sb. Doesn't append anything around it. Throws a RuntimeException if sb can't
	 * be appended to due to an IOException, which should never really happen unless you're using a fixed-size
	 * Appendable that has no room left.
	 * @param sb   an Appendable CharSequence that will be modified, such as a StringBuilder
	 * @param item the item to append
	 * @return {@code sb}, after modification
	 * @param <S>  any type that is both a CharSequence and an Appendable, such as StringBuilder, StringBuffer, CharBuffer, or CharList
	 */
	static <S extends CharSequence & Appendable> S append(S sb, char item) {
		try {
			sb.append(item);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return sb;
	}

	/**
	 * Appends char constants as they would be read in Java sources, in single quotes, with backslash escapes if
	 * necessary.
	 */
	CharAppender READABLE = Base::appendReadable;
}

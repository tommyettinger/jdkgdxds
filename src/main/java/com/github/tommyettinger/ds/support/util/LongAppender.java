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

/**
 * A functional interface that takes and returns an object that is a CharSequence and is Appendable, appending
 * a {@code long} item to it.
 * This is often a method reference to a method in {@link Base}, such as {@link Base#appendSigned(CharSequence, long)}.
 */
public interface LongAppender {
	/**
	 * Appends {@code item} to {@code sb} and returns {@code sb} for chaining.
	 *
	 * @param sb an Appendable CharSequence that will be modified, such as a StringBuilder
	 * @param item the item to append
	 * @return {@code first}, after modification
	 * @param <S> any type that is both a CharSequence and an Appendable, such as StringBuilder, StringBuffer, CharBuffer, or CharList
	 */
	<S extends CharSequence & Appendable> S apply(S sb, long item);

	/**
	 * A static constant to avoid Android and its R8 compiler allocating a new lambda every time
	 * {@code StringBuilder::append} is present at a call-site. This should be used in place of
	 * {@link StringBuilder#append(long)} when you want to use that as an LongAppender.
	 * This actually calls {@link Base#appendSigned(CharSequence, long)} on {@link Base#BASE10}, and works with more
	 * than StringBuilder.
	 */
	LongAppender DEFAULT = Base.BASE10::appendSigned;

	/**
	 * An alternative LongAppender constant that appends five {@link Base#BASE90} digits for every long input.
	 * The five ASCII chars are not expected to be human-readable.
	 * <br>
	 * This is a static constant to avoid Android and its R8 compiler allocating a new lambda every time
	 * this lambda would be present at a call-site.
	 */
	LongAppender DENSE = Base.BASE90::appendUnsigned;

	/**
	 * Appends long constants as they would be read in Java sources, in base 10 with a trailing {@code 'L'}.
	 */
	LongAppender READABLE = Base::appendReadable;
}

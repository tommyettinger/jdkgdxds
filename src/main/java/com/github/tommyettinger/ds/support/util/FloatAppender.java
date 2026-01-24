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
import com.github.tommyettinger.digital.BitConversion;

/**
 * A functional interface that takes and returns an object that is a CharSequence and is Appendable, appending
 * a {@code float} item to it.
 * This is often a method reference to a method in {@link Base}, such as {@link Base#appendSigned(CharSequence, float)}.
 */
public interface FloatAppender {
	/**
	 * Appends {@code item} to {@code sb} and returns {@code sb} for chaining.
	 *
	 * @param sb   an Appendable CharSequence that will be modified, such as a StringBuilder
	 * @param item the item to append
	 * @param <S>  any type that is both a CharSequence and an Appendable, such as StringBuilder, StringBuffer, CharBuffer, or CharList
	 * @return {@code sb}, after modification
	 */
	<S extends CharSequence & Appendable> S apply(S sb, float item);

	/**
	 * A static constant to avoid Android and its R8 compiler allocating a new lambda every time
	 * {@code StringBuilder::append} is present at a call-site. This should be used in place of
	 * {@link StringBuilder#append(float)} when you want to use that as an FloatAppender.
	 * This actually calls {@link Base#appendSigned(CharSequence, float)} on {@link Base#BASE10}, and works with more
	 * than StringBuilder.
	 */
	FloatAppender DEFAULT = Base.BASE10::appendGeneral;

	/**
	 * An alternative FloatAppender constant that appends five {@link Base#BASE90} digits for every float input.
	 * The five ASCII chars are not expected to be human-readable.
	 * <br>
	 * This is a static constant to avoid Android and its R8 compiler allocating a new lambda every time
	 * this lambda would be present at a call-site.
	 */
	FloatAppender DENSE = FloatAppender::appendDense;

	static <S extends CharSequence & Appendable> S appendDense(S sb, float item) {
		return Base.BASE90.appendUnsigned(sb, BitConversion.floatToRawIntBits(item));
	}

	/**
	 * Appends float constants as they would be read in Java sources, in base 10 with a trailing {@code 'f'}.
	 */
	FloatAppender READABLE = Base::appendReadable;
}

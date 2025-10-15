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
import java.util.Objects;

/**
 * A functional interface that takes and returns an object that is a CharSequence and is Appendable, appending
 * a {@code T} item to it.
 * This is not typically a method reference to anything in {@link Base}, which is different from other Appender types. This will frequently
 * use a lambda.
 * @param <T> the type of items that can be appended by this functional interface
 */
public interface Appender<T> {
	/**
	 * Appends {@code item} to {@code sb} and returns {@code sb} for chaining.
	 *
	 * @param sb an Appendable CharSequence that will be modified, such as a StringBuilder
	 * @param item the item to append
	 * @return {@code sb}, after modification
	 * @param <S> any type that is both a CharSequence and an Appendable, such as StringBuilder, StringBuffer, CharBuffer, or CharList
	 */
	<S extends CharSequence & Appendable> S apply(S sb, T item);

	/**
	 * Commonly used as a method reference where a generic Appender is needed.
	 * @param sb an Appendable CharSequence that will be modified, such as a StringBuilder
	 * @param item the item to append
	 * @return {@code sb}, after modification
	 * @param <S> any type that is both a CharSequence and an Appendable, such as StringBuilder, StringBuffer, CharBuffer, or CharList
	 * @param <T> the type of {@code item}, which will be run through {@link Objects#toString(Object)} to get a String representation
	 */
	static <S extends CharSequence & Appendable, T> S append(S sb, T item) {
		try {
			sb.append(Objects.toString(item));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return sb;
	}

	static <S extends CharSequence & Appendable, E extends Enum<?>> S appendEnumName(S sb, E item) {
		try {
			sb.append(item == null ? "null" : item.name());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return sb;
	}

	/**
	 * Used in enum-keyed maps and sets to write an Enum constant's name, rather than its toString result.
	 */
	Appender<Enum<?>> ENUM_NAME_APPENDER = Appender::appendEnumName;

}

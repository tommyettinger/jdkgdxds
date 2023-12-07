/*
 * Copyright (c) 2023 See AUTHORS file.
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

package com.github.tommyettinger.ds;

import com.github.tommyettinger.function.CharPredicate;
import com.github.tommyettinger.function.CharToCharFunction;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A small class that holds two functional-interface values used for filtering and editing characters, and a name they can be
 * associated with. Every time a CharFilter is constructed, it is registered internally, so it can be looked up at a later time
 * by name, using {@link #get(String)} or {@link #getOrDefault(String, CharFilter)}. Actually obtaining CharFilter objects is done
 * with {@link #getOrCreate(String, CharPredicate, CharToCharFunction)}, which tries to get an existing CharFilter first, and if it
 * can't find one, then it creates, registers, and returns one. A suggested practice for names is to describe
 * the filter's effects first, if any, followed by the editor's effects. A typical filter might only allow letter chars through,
 * and would convert them to upper-case so that they can (almost always) be treated as case-insensitive. This could use method
 * references to {@link Character#isLetter(char)} for the filter, {@link Character#toUpperCase(char)} for the editor, and could be
 * named {@code "LetterOnlyCaseInsensitive"}.
 */
public class CharFilter {
	/**
	 * The unique identifying name for this combination of filter and editor.
	 */
	public final @NonNull String name;
	/**
	 * A CharPredicate that should return true iff a character should be considered for equality/hashing.
	 */
	public final @NonNull CharPredicate filter;
	/**
	 * A CharToCharFunction that will take a char from a key String and return a potentially different char.
	 */
	public final @NonNull CharToCharFunction editor;

	private static final HolderOrderedSet<CharFilter, String> REGISTRY = new HolderOrderedSet<>(CharFilter::getName);

	protected CharFilter() {
		this("Identity", c -> true, c -> c);
	}

	protected CharFilter (@NonNull String name, @NonNull CharPredicate filter, @NonNull CharToCharFunction editor) {
		this.name = name;
		this.filter = filter;
		this.editor = editor;
		REGISTRY.add(this);
	}

	/**
	 * Tries to get an existing CharFilter known by {@code name}, and if one exists, this returns that CharFilter.
	 * Otherwise, this will construct and return a new CharFilter with the given name, filter, and editor, registering it
	 * in the process.
	 * @param name the String name to look up, or to register by if none was found
	 * @param filter a CharPredicate that should return true iff a character should be considered for equality/hashing
	 * @param editor a CharToCharFunction that will take a char from a key String and return a potentially different char
	 * @return a CharFilter, either one that already exists by the given name, or a newly-registered one that was just created
	 */
	public static CharFilter getOrCreate(@NonNull String name, @NonNull CharPredicate filter, @NonNull CharToCharFunction editor) {
		CharFilter existing = REGISTRY.get(name);
		if(existing == null) {
			return new CharFilter(name, filter, editor);
		}
		return existing;
	}

	@Override
	public boolean equals (Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		CharFilter that = (CharFilter)o;

		return name.equals(that.name);
	}

	@Override
	public int hashCode () {
		return name.hashCode();
	}

	@Override
	public String toString () {
		return "CharFilter{'" + name + "'}";
	}

	public @NonNull String getName () {
		return name;
	}

	public @NonNull CharPredicate getFilter () {
		return filter;
	}

	public @NonNull CharToCharFunction getEditor () {
		return editor;
	}

	/**
	 * Checks if a CharFilter is registered to {@code name}, returning true if one is, or false otherwise.
	 * @param name the name to look up
	 * @return true if a CharFilter is registered by {@code name}, or false otherwise
	 */
	public static boolean contains(String name) {
		//noinspection SuspiciousMethodCalls
		return REGISTRY.contains(name);
	}

	/**
	 * Gets the CharFilter registered to {@code name}, or null if none exists.
	 * @param name the name to look up
	 * @return a registered CharFilter or null
	 */
	public static CharFilter get(String name) {
		return REGISTRY.get(name);
	}

	/**
	 * Gets the CharFilter registered to {@code name}, or {@code defaultValue} if none exists.
	 * @param name the name to look up
	 * @param defaultValue a CharFilter to return if none was found; may be null
	 * @return a registered CharFilter or {@code defaultValue}
	 */
	public static CharFilter getOrDefault(String name, CharFilter defaultValue) {
		return REGISTRY.getOrDefault(name, defaultValue);
	}
}

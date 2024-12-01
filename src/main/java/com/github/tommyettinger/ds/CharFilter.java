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
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A small class that holds two functional-interface values used for filtering and editing characters, and a name they are
 * associated with. Every time a CharFilter is constructed, it is registered internally, so it can be looked up at a later time
 * by name, using {@link #get(String)} or {@link #getOrDefault(String, CharFilter)}. Actually obtaining CharFilter objects is done
 * with {@link #getOrCreate(String, CharPredicate, CharToCharFunction)}, which tries to get an existing CharFilter first, and if it
 * can't find one, then it creates, registers, and returns one. A suggested practice for names is to describe the
 * filter's effects first, if any, followed by the editor's effects. A typical filter might only allow letter chars through,
 * and would convert them to upper-case so that they can (almost always) be treated as case-insensitive. This could use method
 * references to {@link Character#isLetter(char)} for the filter, {@link Character#toUpperCase(char)} for the editor, and could be
 * named {@code "LetterOnlyCaseInsensitive"}.
 * <br>
 * Any CharFilter can be used as a factory to create {@link FilteredStringSet} and {@link FilteredStringOrderedSet}
 * collections as if using their {@link FilteredStringSet#with()} static method and this CharFilter. These use the
 * {@link #makeSet()} and {@link #makeOrderedSet()} methods, which can take 0-8 String parameters without allocating,
 * or any number of String parameters if given an array or varargs.
 * <br>
 * If you target GWT, be aware that several built-in JDK methods for handling chars may work differently in HTML than on
 * desktop, Android, or other platforms. In particular, {@link Character#toUpperCase(char)} will not work on most Unicode
 * chars on GWT, so you need another way to handle case-insensitivity. If you depend on
 * <a href="https://github.com/tommyettinger/RegExodus">RegExodus</a>, you can use its {@code Category::caseUp} as an
 * editor to make a char upper-case (if such a transformation can be done), and this works on GWT. Similarly,
 * {@code Category.L::contains} can be used as a filter to allow only chars in Unicode category L (letters). Because
 * jdkgdxds does not depend on RegExodus, these aren't used in predefined filters or editors here.
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
	public static @Nullable CharFilter get(String name) {
		return REGISTRY.get(name);
	}

	/**
	 * Gets the CharFilter registered to {@code name}, or {@code defaultValue} if none exists.
	 * @param name the name to look up
	 * @param defaultValue a CharFilter to return if none was found; may be null
	 * @return a registered CharFilter or {@code defaultValue}
	 */
	public static @Nullable CharFilter getOrDefault(String name, @Nullable CharFilter defaultValue) {
		return REGISTRY.getOrDefault(name, defaultValue);
	}

	/**
	 * Constructs an empty set using this CharFilter.
	 * This is usually less useful than just using the constructor, but can be handy
	 * in some code-generation scenarios when you don't know how many arguments you will have.
	 *
	 * @return a new set containing nothing
	 */
	public FilteredStringSet makeSet () {
		return new FilteredStringSet(this, 0);
	}

	/**
	 * Creates a new FilteredStringSet that holds only the given item, but can be resized.
	 * Uses this CharFilter in the new set.
	 * @param item one String item
	 * @return a new FilteredStringSet that holds the given item
	 */
	public FilteredStringSet makeSet (String item) {
		FilteredStringSet set = new FilteredStringSet(this, 1);
		set.add(item);
		return set;
	}

	/**
	 * Creates a new FilteredStringSet that holds only the given items, but can be resized.
	 * Uses this CharFilter in the new set.
	 * @param item0 a String item
	 * @param item1 a String item
	 * @return a new FilteredStringSet that holds the given items
	 */
	public FilteredStringSet makeSet (String item0, String item1) {
		FilteredStringSet set = new FilteredStringSet(this, 2);
		set.add(item0, item1);
		return set;
	}

	/**
	 * Creates a new FilteredStringSet that holds only the given items, but can be resized.
	 * Uses this CharFilter in the new set.
	 * @param item0 a String item
	 * @param item1 a String item
	 * @param item2 a String item
	 * @return a new FilteredStringSet that holds the given items
	 */
	public FilteredStringSet makeSet (String item0, String item1, String item2) {
		FilteredStringSet set = new FilteredStringSet(this, 3);
		set.add(item0, item1, item2);
		return set;
	}

	/**
	 * Creates a new FilteredStringSet that holds only the given items, but can be resized.
	 * Uses this CharFilter in the new set.
	 * @param item0 a String item
	 * @param item1 a String item
	 * @param item2 a String item
	 * @param item3 a String item
	 * @return a new FilteredStringSet that holds the given items
	 */
	public FilteredStringSet makeSet (String item0, String item1, String item2, String item3) {
		FilteredStringSet set = new FilteredStringSet(this, 4);
		set.add(item0, item1, item2, item3);
		return set;
	}

	/**
	 * Creates a new FilteredStringSet that holds only the given items, but can be resized.
	 * Uses this CharFilter in the new set.
	 * @param item0 a String item
	 * @param item1 a String item
	 * @param item2 a String item
	 * @param item3 a String item
	 * @param item4 a String item
	 * @return a new FilteredStringSet that holds the given items
	 */
	public FilteredStringSet makeSet (String item0, String item1, String item2, String item3, String item4) {
		FilteredStringSet set = new FilteredStringSet(this, 5);
		set.add(item0, item1, item2, item3);
		set.add(item4);
		return set;
	}

	/**
	 * Creates a new FilteredStringSet that holds only the given items, but can be resized.
	 * Uses this CharFilter in the new set.
	 * @param item0 a String item
	 * @param item1 a String item
	 * @param item2 a String item
	 * @param item3 a String item
	 * @param item4 a String item
	 * @param item5 a String item
	 * @return a new FilteredStringSet that holds the given items
	 */
	public FilteredStringSet makeSet (String item0, String item1, String item2, String item3, String item4, String item5) {
		FilteredStringSet set = new FilteredStringSet(this, 6);
		set.add(item0, item1, item2, item3);
		set.add(item4, item5);
		return set;
	}

	/**
	 * Creates a new FilteredStringSet that holds only the given items, but can be resized.
	 * Uses this CharFilter in the new set.
	 * @param item0 a String item
	 * @param item1 a String item
	 * @param item2 a String item
	 * @param item3 a String item
	 * @param item4 a String item
	 * @param item5 a String item
	 * @param item6 a String item
	 * @return a new FilteredStringSet that holds the given items
	 */
	public FilteredStringSet makeSet (String item0, String item1, String item2, String item3, String item4, String item5, String item6) {
		FilteredStringSet set = new FilteredStringSet(this, 7);
		set.add(item0, item1, item2, item3);
		set.add(item4, item5, item6);
		return set;
	}

	/**
	 * Creates a new FilteredStringSet that holds only the given items, but can be resized.
	 * Uses this CharFilter in the new set.
	 * @param item0 a String item
	 * @param item1 a String item
	 * @param item2 a String item
	 * @param item3 a String item
	 * @param item4 a String item
	 * @param item5 a String item
	 * @param item6 a String item
	 * @return a new FilteredStringSet that holds the given items
	 */
	public FilteredStringSet makeSet (String item0, String item1, String item2, String item3, String item4, String item5, String item6, String item7) {
		FilteredStringSet set = new FilteredStringSet(this, 8);
		set.add(item0, item1, item2, item3);
		set.add(item4, item5, item6, item7);
		return set;
	}

	/**
	 * Creates a new FilteredStringSet that holds only the given items, but can be resized.
	 * Uses this CharFilter in the new set.
	 * This overload will only be used when an array is supplied or if varargs are used and
	 * there are 9 or more arguments.
	 * @param varargs a String varargs or String array; remember that varargs allocate
	 * @return a new FilteredStringSet that holds the given items
	 */
	public FilteredStringSet makeSet (String... varargs) {
		return new FilteredStringSet(this, varargs);
	}

	/**
	 * Constructs an empty ordered set using this CharFilter.
	 * This is usually less useful than just using the constructor, but can be handy
	 * in some code-generation scenarios when you don't know how many arguments you will have.
	 *
	 * @return a new ordered set containing nothing
	 */
	public FilteredStringOrderedSet makeOrderedSet () {
		return new FilteredStringOrderedSet(this, 0);
	}

	/**
	 * Creates a new FilteredStringOrderedSet that holds only the given item, but can be resized.
	 * Uses this CharFilter in the new ordered set.
	 * @param item one String item
	 * @return a new FilteredStringOrderedSet that holds the given item
	 */
	public FilteredStringOrderedSet makeOrderedSet (String item) {
		FilteredStringOrderedSet set = new FilteredStringOrderedSet(this, 1);
		set.add(item);
		return set;
	}

	/**
	 * Creates a new FilteredStringOrderedSet that holds only the given items, but can be resized.
	 * Uses this CharFilter in the new ordered set.
	 * @param item0 a String item
	 * @param item1 a String item
	 * @return a new FilteredStringOrderedSet that holds the given items
	 */
	public FilteredStringOrderedSet makeOrderedSet (String item0, String item1) {
		FilteredStringOrderedSet set = new FilteredStringOrderedSet(this, 2);
		set.add(item0, item1);
		return set;
	}

	/**
	 * Creates a new FilteredStringOrderedSet that holds only the given items, but can be resized.
	 * Uses this CharFilter in the new ordered set.
	 * @param item0 a String item
	 * @param item1 a String item
	 * @param item2 a String item
	 * @return a new FilteredStringOrderedSet that holds the given items
	 */
	public FilteredStringOrderedSet makeOrderedSet (String item0, String item1, String item2) {
		FilteredStringOrderedSet set = new FilteredStringOrderedSet(this, 3);
		set.add(item0, item1, item2);
		return set;
	}

	/**
	 * Creates a new FilteredStringOrderedSet that holds only the given items, but can be resized.
	 * Uses this CharFilter in the new ordered set.
	 * @param item0 a String item
	 * @param item1 a String item
	 * @param item2 a String item
	 * @param item3 a String item
	 * @return a new FilteredStringOrderedSet that holds the given items
	 */
	public FilteredStringOrderedSet makeOrderedSet (String item0, String item1, String item2, String item3) {
		FilteredStringOrderedSet set = new FilteredStringOrderedSet(this, 4);
		set.add(item0, item1, item2, item3);
		return set;
	}

	/**
	 * Creates a new FilteredStringOrderedSet that holds only the given items, but can be resized.
	 * Uses this CharFilter in the new ordered set.
	 * @param item0 a String item
	 * @param item1 a String item
	 * @param item2 a String item
	 * @param item3 a String item
	 * @param item4 a String item
	 * @return a new FilteredStringOrderedSet that holds the given items
	 */
	public FilteredStringOrderedSet makeOrderedSet (String item0, String item1, String item2, String item3, String item4) {
		FilteredStringOrderedSet set = new FilteredStringOrderedSet(this, 5);
		set.add(item0, item1, item2, item3);
		set.add(item4);
		return set;
	}

	/**
	 * Creates a new FilteredStringOrderedSet that holds only the given items, but can be resized.
	 * Uses this CharFilter in the new ordered set.
	 * @param item0 a String item
	 * @param item1 a String item
	 * @param item2 a String item
	 * @param item3 a String item
	 * @param item4 a String item
	 * @param item5 a String item
	 * @return a new FilteredStringOrderedSet that holds the given items
	 */
	public FilteredStringOrderedSet makeOrderedSet (String item0, String item1, String item2, String item3, String item4, String item5) {
		FilteredStringOrderedSet set = new FilteredStringOrderedSet(this, 6);
		set.add(item0, item1, item2, item3);
		set.add(item4, item5);
		return set;
	}

	/**
	 * Creates a new FilteredStringOrderedSet that holds only the given items, but can be resized.
	 * Uses this CharFilter in the new ordered set.
	 * @param item0 a String item
	 * @param item1 a String item
	 * @param item2 a String item
	 * @param item3 a String item
	 * @param item4 a String item
	 * @param item5 a String item
	 * @param item6 a String item
	 * @return a new FilteredStringOrderedSet that holds the given items
	 */
	public FilteredStringOrderedSet makeOrderedSet (String item0, String item1, String item2, String item3, String item4, String item5, String item6) {
		FilteredStringOrderedSet set = new FilteredStringOrderedSet(this, 7);
		set.add(item0, item1, item2, item3);
		set.add(item4, item5, item6);
		return set;
	}

	/**
	 * Creates a new FilteredStringOrderedSet that holds only the given items, but can be resized.
	 * Uses this CharFilter in the new ordered set.
	 * @param item0 a String item
	 * @param item1 a String item
	 * @param item2 a String item
	 * @param item3 a String item
	 * @param item4 a String item
	 * @param item5 a String item
	 * @param item6 a String item
	 * @return a new FilteredStringOrderedSet that holds the given items
	 */
	public FilteredStringOrderedSet makeOrderedSet (String item0, String item1, String item2, String item3, String item4, String item5, String item6, String item7) {
		FilteredStringOrderedSet set = new FilteredStringOrderedSet(this, 8);
		set.add(item0, item1, item2, item3);
		set.add(item4, item5, item6, item7);
		return set;
	}

	/**
	 * Creates a new FilteredStringOrderedSet that holds only the given items, but can be resized.
	 * Uses this CharFilter in the new ordered set.
	 * This overload will only be used when an array is supplied or if varargs are used and
	 * there are 9 or more arguments.
	 * @param varargs a String varargs or String array; remember that varargs allocate
	 * @return a new FilteredStringOrderedSet that holds the given items
	 */
	public FilteredStringOrderedSet makeOrderedSet (String... varargs) {
		return new FilteredStringOrderedSet(this, varargs);
	}
}

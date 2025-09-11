/*
 * Copyright (c) 2023-2025 See AUTHORS file.
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

import com.github.tommyettinger.function.CharPredicate;
import com.github.tommyettinger.function.CharToCharFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A small class that holds two functional-interface values used for filtering and editing characters, and a name they
 * are associated with. Every time a CharFilter is constructed, it is registered internally, so it can be looked up at a
 * later time by name, using {@link #get(String)} or {@link #getOrDefault(String, CharFilter)}. Actually obtaining
 * CharFilter objects is done with {@link #getOrCreate(String, CharPredicate, CharToCharFunction)}, which tries to get
 * an existing CharFilter first, and if it can't find one, then it creates, registers, and returns one. A suggested
 * practice for names is to describe the filter's effects first, if any, followed by the editor's effects. A typical
 * filter might only allow letter chars through, and would convert them to upper-case so that they can (almost always)
 * be treated as case-insensitive. This could use
 * {@code com.github.tommyettinger.ds.support.util.CharPredicates.IS_LETTER} (or a method reference to
 * {@link Character#isLetter(char)}) for the filter, {@link Casing#caseUp(char)} (or
 * {@link Character#toUpperCase(char)}) for the editor, and could be named {@code "LetterOnlyCaseInsensitive"}.
 * <br>
 * Any CharFilter can be used as a factory to create {@link FilteredStringSet} and {@link FilteredStringOrderedSet}
 * collections as if using their {@link FilteredStringSet#with()} static method and this CharFilter. These use the
 * {@link #makeSet()} and {@link #makeOrderedSet()} methods, which can take 0-8 String parameters without allocating,
 * or any number of String parameters if given an array or varargs.
 * <br>
 * Any CharFilter can be also used as a factory to create {@link FilteredStringMap} and {@link FilteredStringOrderedMap}
 * collections as if using their {@link FilteredStringMap#with()} static method and this CharFilter. These use the
 * {@link #makeMap()} and {@link #makeOrderedMap()} methods, which can take 0-4 String-V pairs without allocating,
 * or one or more String-V pairs if given an array or varargs.
 * <br>
 * If you target GWT, be aware that several built-in JDK methods for handling chars may work differently in HTML than on
 * desktop, Android, or other platforms. In particular, {@link Character#isLetter(char)} will not work on most Unicode
 * chars on GWT, so you need another way to handle checks like that.
 * {@code com.github.tommyettinger.ds.support.util.CharPredicates.IS_LETTER} is a CharPredicate you can use instead of
 * isLetter on any platform, and acts as {@link Character#isLetter(char)} does on Java 24, but on all Java versions and
 * target platforms (including GWT). Other fields in CharPredicates may also be useful, and you can create your own
 * {@link CharBitSet} objects to serve as predefined predicates that look up a char in an uncompressed bit set.
 */
public class CharFilter {
	/**
	 * The unique identifying name for this combination of filter and editor.
	 */
	public final @NotNull String name;
	/**
	 * A CharPredicate that should return true iff a character should be considered for equality/hashing.
	 */
	public final @NotNull CharPredicate filter;
	/**
	 * A CharToCharFunction that will take a char from a key String and return a potentially different char.
	 */
	public final @NotNull CharToCharFunction editor;

	private static final HolderOrderedSet<CharFilter, String> REGISTRY = new HolderOrderedSet<>(CharFilter::getName);

	protected CharFilter() {
		this("Identity", c -> true, c -> c);
	}

	protected CharFilter(@NotNull String name, @NotNull CharPredicate filter, @NotNull CharToCharFunction editor) {
		this.name = name;
		this.filter = filter;
		this.editor = editor;
		REGISTRY.add(this);
	}

	/**
	 * Tries to get an existing CharFilter known by {@code name}, and if one exists, this returns that CharFilter.
	 * Otherwise, this will construct and return a new CharFilter with the given name, filter, and editor, registering it
	 * in the process.
	 *
	 * @param name   the String name to look up, or to register by if none was found
	 * @param filter a CharPredicate that should return true iff a character should be considered for equality/hashing
	 * @param editor a CharToCharFunction that will take a char from a key String and return a potentially different char
	 * @return a CharFilter, either one that already exists by the given name, or a newly-registered one that was just created
	 */
	public static CharFilter getOrCreate(@NotNull String name, @NotNull CharPredicate filter, @NotNull CharToCharFunction editor) {
		CharFilter existing = REGISTRY.get(name);
		if (existing == null) {
			return new CharFilter(name, filter, editor);
		}
		return existing;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		CharFilter that = (CharFilter) o;

		return name.equals(that.name);
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public String toString() {
		return "CharFilter{'" + name + "'}";
	}

	public @NotNull String getName() {
		return name;
	}

	public @NotNull CharPredicate getFilter() {
		return filter;
	}

	public @NotNull CharToCharFunction getEditor() {
		return editor;
	}

	/**
	 * Checks if a CharFilter is registered to {@code name}, returning true if one is, or false otherwise.
	 *
	 * @param name the name to look up
	 * @return true if a CharFilter is registered by {@code name}, or false otherwise
	 */
	public static boolean contains(String name) {
		//noinspection SuspiciousMethodCalls
		return REGISTRY.contains(name);
	}

	/**
	 * Gets the CharFilter registered to {@code name}, or null if none exists.
	 *
	 * @param name the name to look up
	 * @return a registered CharFilter or null
	 */
	public static @Nullable CharFilter get(String name) {
		return REGISTRY.get(name);
	}

	/**
	 * Gets the CharFilter registered to {@code name}, or {@code defaultValue} if none exists.
	 *
	 * @param name         the name to look up
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
	public FilteredStringSet makeSet() {
		return new FilteredStringSet(this, 0);
	}

	/**
	 * Creates a new FilteredStringSet that holds only the given item, but can be resized.
	 * Uses this CharFilter in the new set.
	 *
	 * @param item one String item
	 * @return a new FilteredStringSet that holds the given item
	 */
	public FilteredStringSet makeSet(String item) {
		FilteredStringSet set = new FilteredStringSet(this, 1);
		set.add(item);
		return set;
	}

	/**
	 * Creates a new FilteredStringSet that holds only the given items, but can be resized.
	 * Uses this CharFilter in the new set.
	 *
	 * @param item0 a String item
	 * @param item1 a String item
	 * @return a new FilteredStringSet that holds the given items
	 */
	public FilteredStringSet makeSet(String item0, String item1) {
		FilteredStringSet set = new FilteredStringSet(this, 2);
		set.add(item0, item1);
		return set;
	}

	/**
	 * Creates a new FilteredStringSet that holds only the given items, but can be resized.
	 * Uses this CharFilter in the new set.
	 *
	 * @param item0 a String item
	 * @param item1 a String item
	 * @param item2 a String item
	 * @return a new FilteredStringSet that holds the given items
	 */
	public FilteredStringSet makeSet(String item0, String item1, String item2) {
		FilteredStringSet set = new FilteredStringSet(this, 3);
		set.add(item0, item1, item2);
		return set;
	}

	/**
	 * Creates a new FilteredStringSet that holds only the given items, but can be resized.
	 * Uses this CharFilter in the new set.
	 *
	 * @param item0 a String item
	 * @param item1 a String item
	 * @param item2 a String item
	 * @param item3 a String item
	 * @return a new FilteredStringSet that holds the given items
	 */
	public FilteredStringSet makeSet(String item0, String item1, String item2, String item3) {
		FilteredStringSet set = new FilteredStringSet(this, 4);
		set.add(item0, item1, item2, item3);
		return set;
	}

	/**
	 * Creates a new FilteredStringSet that holds only the given items, but can be resized.
	 * Uses this CharFilter in the new set.
	 *
	 * @param item0 a String item
	 * @param item1 a String item
	 * @param item2 a String item
	 * @param item3 a String item
	 * @param item4 a String item
	 * @return a new FilteredStringSet that holds the given items
	 */
	public FilteredStringSet makeSet(String item0, String item1, String item2, String item3, String item4) {
		FilteredStringSet set = new FilteredStringSet(this, 5);
		set.add(item0, item1, item2, item3);
		set.add(item4);
		return set;
	}

	/**
	 * Creates a new FilteredStringSet that holds only the given items, but can be resized.
	 * Uses this CharFilter in the new set.
	 *
	 * @param item0 a String item
	 * @param item1 a String item
	 * @param item2 a String item
	 * @param item3 a String item
	 * @param item4 a String item
	 * @param item5 a String item
	 * @return a new FilteredStringSet that holds the given items
	 */
	public FilteredStringSet makeSet(String item0, String item1, String item2, String item3, String item4, String item5) {
		FilteredStringSet set = new FilteredStringSet(this, 6);
		set.add(item0, item1, item2, item3);
		set.add(item4, item5);
		return set;
	}

	/**
	 * Creates a new FilteredStringSet that holds only the given items, but can be resized.
	 * Uses this CharFilter in the new set.
	 *
	 * @param item0 a String item
	 * @param item1 a String item
	 * @param item2 a String item
	 * @param item3 a String item
	 * @param item4 a String item
	 * @param item5 a String item
	 * @param item6 a String item
	 * @return a new FilteredStringSet that holds the given items
	 */
	public FilteredStringSet makeSet(String item0, String item1, String item2, String item3, String item4, String item5, String item6) {
		FilteredStringSet set = new FilteredStringSet(this, 7);
		set.add(item0, item1, item2, item3);
		set.add(item4, item5, item6);
		return set;
	}

	/**
	 * Creates a new FilteredStringSet that holds only the given items, but can be resized.
	 * Uses this CharFilter in the new set.
	 *
	 * @param item0 a String item
	 * @param item1 a String item
	 * @param item2 a String item
	 * @param item3 a String item
	 * @param item4 a String item
	 * @param item5 a String item
	 * @param item6 a String item
	 * @return a new FilteredStringSet that holds the given items
	 */
	public FilteredStringSet makeSet(String item0, String item1, String item2, String item3, String item4, String item5, String item6, String item7) {
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
	 *
	 * @param varargs a String varargs or String array; remember that varargs allocate
	 * @return a new FilteredStringSet that holds the given items
	 */
	public FilteredStringSet makeSet(String... varargs) {
		return new FilteredStringSet(this, varargs);
	}

	/**
	 * Constructs an empty ordered set using this CharFilter.
	 * This is usually less useful than just using the constructor, but can be handy
	 * in some code-generation scenarios when you don't know how many arguments you will have.
	 *
	 * @return a new ordered set containing nothing
	 */
	public FilteredStringOrderedSet makeOrderedSet() {
		return new FilteredStringOrderedSet(this, 0);
	}

	/**
	 * Creates a new FilteredStringOrderedSet that holds only the given item, but can be resized.
	 * Uses this CharFilter in the new ordered set.
	 *
	 * @param item one String item
	 * @return a new FilteredStringOrderedSet that holds the given item
	 */
	public FilteredStringOrderedSet makeOrderedSet(String item) {
		FilteredStringOrderedSet set = new FilteredStringOrderedSet(this, 1);
		set.add(item);
		return set;
	}

	/**
	 * Creates a new FilteredStringOrderedSet that holds only the given items, but can be resized.
	 * Uses this CharFilter in the new ordered set.
	 *
	 * @param item0 a String item
	 * @param item1 a String item
	 * @return a new FilteredStringOrderedSet that holds the given items
	 */
	public FilteredStringOrderedSet makeOrderedSet(String item0, String item1) {
		FilteredStringOrderedSet set = new FilteredStringOrderedSet(this, 2);
		set.add(item0, item1);
		return set;
	}

	/**
	 * Creates a new FilteredStringOrderedSet that holds only the given items, but can be resized.
	 * Uses this CharFilter in the new ordered set.
	 *
	 * @param item0 a String item
	 * @param item1 a String item
	 * @param item2 a String item
	 * @return a new FilteredStringOrderedSet that holds the given items
	 */
	public FilteredStringOrderedSet makeOrderedSet(String item0, String item1, String item2) {
		FilteredStringOrderedSet set = new FilteredStringOrderedSet(this, 3);
		set.add(item0, item1, item2);
		return set;
	}

	/**
	 * Creates a new FilteredStringOrderedSet that holds only the given items, but can be resized.
	 * Uses this CharFilter in the new ordered set.
	 *
	 * @param item0 a String item
	 * @param item1 a String item
	 * @param item2 a String item
	 * @param item3 a String item
	 * @return a new FilteredStringOrderedSet that holds the given items
	 */
	public FilteredStringOrderedSet makeOrderedSet(String item0, String item1, String item2, String item3) {
		FilteredStringOrderedSet set = new FilteredStringOrderedSet(this, 4);
		set.add(item0, item1, item2, item3);
		return set;
	}

	/**
	 * Creates a new FilteredStringOrderedSet that holds only the given items, but can be resized.
	 * Uses this CharFilter in the new ordered set.
	 *
	 * @param item0 a String item
	 * @param item1 a String item
	 * @param item2 a String item
	 * @param item3 a String item
	 * @param item4 a String item
	 * @return a new FilteredStringOrderedSet that holds the given items
	 */
	public FilteredStringOrderedSet makeOrderedSet(String item0, String item1, String item2, String item3, String item4) {
		FilteredStringOrderedSet set = new FilteredStringOrderedSet(this, 5);
		set.add(item0, item1, item2, item3);
		set.add(item4);
		return set;
	}

	/**
	 * Creates a new FilteredStringOrderedSet that holds only the given items, but can be resized.
	 * Uses this CharFilter in the new ordered set.
	 *
	 * @param item0 a String item
	 * @param item1 a String item
	 * @param item2 a String item
	 * @param item3 a String item
	 * @param item4 a String item
	 * @param item5 a String item
	 * @return a new FilteredStringOrderedSet that holds the given items
	 */
	public FilteredStringOrderedSet makeOrderedSet(String item0, String item1, String item2, String item3, String item4, String item5) {
		FilteredStringOrderedSet set = new FilteredStringOrderedSet(this, 6);
		set.add(item0, item1, item2, item3);
		set.add(item4, item5);
		return set;
	}

	/**
	 * Creates a new FilteredStringOrderedSet that holds only the given items, but can be resized.
	 * Uses this CharFilter in the new ordered set.
	 *
	 * @param item0 a String item
	 * @param item1 a String item
	 * @param item2 a String item
	 * @param item3 a String item
	 * @param item4 a String item
	 * @param item5 a String item
	 * @param item6 a String item
	 * @return a new FilteredStringOrderedSet that holds the given items
	 */
	public FilteredStringOrderedSet makeOrderedSet(String item0, String item1, String item2, String item3, String item4, String item5, String item6) {
		FilteredStringOrderedSet set = new FilteredStringOrderedSet(this, 7);
		set.add(item0, item1, item2, item3);
		set.add(item4, item5, item6);
		return set;
	}

	/**
	 * Creates a new FilteredStringOrderedSet that holds only the given items, but can be resized.
	 * Uses this CharFilter in the new ordered set.
	 *
	 * @param item0 a String item
	 * @param item1 a String item
	 * @param item2 a String item
	 * @param item3 a String item
	 * @param item4 a String item
	 * @param item5 a String item
	 * @param item6 a String item
	 * @return a new FilteredStringOrderedSet that holds the given items
	 */
	public FilteredStringOrderedSet makeOrderedSet(String item0, String item1, String item2, String item3, String item4, String item5, String item6, String item7) {
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
	 *
	 * @param varargs a String varargs or String array; remember that varargs allocate
	 * @return a new FilteredStringOrderedSet that holds the given items
	 */
	public FilteredStringOrderedSet makeOrderedSet(String... varargs) {
		return new FilteredStringOrderedSet(this, varargs);
	}

	/**
	 * Constructs an empty map given just a CharFilter.
	 * This is usually less useful than just using the constructor, but can be handy
	 * in some code-generation scenarios when you don't know how many arguments you will have.
	 *
	 * @param <V> the type of values
	 * @return a new map containing nothing
	 */
	public <V> FilteredStringMap<V> makeMap() {
		return new FilteredStringMap<>(this);
	}

	/**
	 * Constructs a single-entry map given a CharFilter, one key and one value.
	 * This is mostly useful as an optimization for {@link #makeMap(String, Object, Object...)}
	 * when there's no "rest" of the keys or values.
	 *
	 * @param key0   the first and only key
	 * @param value0 the first and only value
	 * @param <V>    the type of value0
	 * @return a new map containing just the entry mapping key0 to value0
	 */
	public <V> FilteredStringMap<V> makeMap(String key0, V value0) {
		FilteredStringMap<V> map = new FilteredStringMap<>(this, 1);
		map.put(key0, value0);
		return map;
	}

	/**
	 * Constructs a single-entry map given a CharFilter and two key-value pairs.
	 * This is mostly useful as an optimization for {@link #makeMap(String, Object, Object...)}
	 * when there's no "rest" of the keys or values.
	 *
	 * @param key0   a String key
	 * @param value0 a V value
	 * @param key1   a String key
	 * @param value1 a V value
	 * @param <V>    the type of value0
	 * @return a new map containing entries mapping each key to the following value
	 */
	public <V> FilteredStringMap<V> makeMap(String key0, V value0, String key1, V value1) {
		FilteredStringMap<V> map = new FilteredStringMap<>(this, 2);
		map.put(key0, value0);
		map.put(key1, value1);
		return map;
	}

	/**
	 * Constructs a single-entry map given a CharFilter and three key-value pairs.
	 * This is mostly useful as an optimization for {@link #makeMap(String, Object, Object...)}
	 * when there's no "rest" of the keys or values.
	 *
	 * @param key0   a String key
	 * @param value0 a V value
	 * @param key1   a String key
	 * @param value1 a V value
	 * @param key2   a String key
	 * @param value2 a V value
	 * @param <V>    the type of value0
	 * @return a new map containing entries mapping each key to the following value
	 */
	public <V> FilteredStringMap<V> makeMap(String key0, V value0, String key1, V value1, String key2, V value2) {
		FilteredStringMap<V> map = new FilteredStringMap<>(this, 3);
		map.put(key0, value0);
		map.put(key1, value1);
		map.put(key2, value2);
		return map;
	}

	/**
	 * Constructs a single-entry map  given a CharFilter and four key-value pairs.
	 * This is mostly useful as an optimization for {@link #makeMap(String, Object, Object...)}
	 * when there's no "rest" of the keys or values.
	 *
	 * @param key0   a String key
	 * @param value0 a V value
	 * @param key1   a String key
	 * @param value1 a V value
	 * @param key2   a String key
	 * @param value2 a V value
	 * @param key3   a String key
	 * @param value3 a V value
	 * @param <V>    the type of value0
	 * @return a new map containing entries mapping each key to the following value
	 */
	public <V> FilteredStringMap<V> makeMap(String key0, V value0, String key1, V value1, String key2, V value2, String key3, V value3) {
		FilteredStringMap<V> map = new FilteredStringMap<>(this, 4);
		map.put(key0, value0);
		map.put(key1, value1);
		map.put(key2, value2);
		map.put(key3, value3);
		return map;
	}

	/**
	 * Constructs a map given a CharFilter, then alternating keys and values.
	 * This can be useful in some code-generation scenarios, or when you want to make a
	 * map conveniently by-hand and have it populated at the start. You can also use
	 * {@link FilteredStringMap#FilteredStringMap(CharFilter, String[], Object[])}, which takes all keys and then all values.
	 * This needs all keys to have the same type and all values to have the same type, because
	 * it gets those types from the first key parameter and first value parameter. Any keys that don't
	 * have String as their type or values that don't have V as their type have that entry skipped.
	 *
	 * @param key0   the first key; will be used to determine the type of all keys
	 * @param value0 the first value; will be used to determine the type of all values
	 * @param rest   an array or varargs of alternating String, V, String, V... elements
	 * @param <V>    the type of values, inferred from value0
	 * @return a new map containing the given keys and values
	 */
	@SuppressWarnings("unchecked")
	public <V> FilteredStringMap<V> makeMap(String key0, V value0, Object... rest) {
		FilteredStringMap<V> map = new FilteredStringMap<>(this, 1 + (rest.length >>> 1));
		map.put(key0, value0);
		for (int i = 1; i < rest.length; i += 2) {
			try {
				map.put((String) rest[i - 1], (V) rest[i]);
			} catch (ClassCastException ignored) {
			}
		}
		return map;
	}

	/**
	 * Constructs an empty ordered map given just a CharFilter.
	 * This is usually less useful than just using the constructor, but can be handy
	 * in some code-generation scenarios when you don't know how many arguments you will have.
	 *
	 * @param <V> the type of values
	 * @return a new ordered map containing nothing
	 */
	public <V> FilteredStringOrderedMap<V> makeOrderedMap() {
		return new FilteredStringOrderedMap<>(this);
	}

	/**
	 * Constructs a single-entry ordered map given a CharFilter, one key and one value.
	 * This is mostly useful as an optimization for {@link #makeOrderedMap(String, Object, Object...)}
	 * when there's no "rest" of the keys or values.
	 *
	 * @param key0   the first and only key
	 * @param value0 the first and only value
	 * @param <V>    the type of value0
	 * @return a new ordered map containing just the entry mapping key0 to value0
	 */
	public <V> FilteredStringOrderedMap<V> makeOrderedMap(String key0, V value0) {
		FilteredStringOrderedMap<V> map = new FilteredStringOrderedMap<>(this, 1);
		map.put(key0, value0);
		return map;
	}

	/**
	 * Constructs a single-entry ordered map given a CharFilter and two key-value pairs.
	 * This is mostly useful as an optimization for {@link #makeOrderedMap(String, Object, Object...)}
	 * when there's no "rest" of the keys or values.
	 *
	 * @param key0   a String key
	 * @param value0 a V value
	 * @param key1   a String key
	 * @param value1 a V value
	 * @param <V>    the type of value0
	 * @return a new ordered map containing entries mapping each key to the following value
	 */
	public <V> FilteredStringOrderedMap<V> makeOrderedMap(String key0, V value0, String key1, V value1) {
		FilteredStringOrderedMap<V> map = new FilteredStringOrderedMap<>(this, 2);
		map.put(key0, value0);
		map.put(key1, value1);
		return map;
	}

	/**
	 * Constructs a single-entry ordered map given a CharFilter and three key-value pairs.
	 * This is mostly useful as an optimization for {@link #makeOrderedMap(String, Object, Object...)}
	 * when there's no "rest" of the keys or values.
	 *
	 * @param key0   a String key
	 * @param value0 a V value
	 * @param key1   a String key
	 * @param value1 a V value
	 * @param key2   a String key
	 * @param value2 a V value
	 * @param <V>    the type of value0
	 * @return a new ordered map containing entries mapping each key to the following value
	 */
	public <V> FilteredStringOrderedMap<V> makeOrderedMap(String key0, V value0, String key1, V value1, String key2, V value2) {
		FilteredStringOrderedMap<V> map = new FilteredStringOrderedMap<>(this, 3);
		map.put(key0, value0);
		map.put(key1, value1);
		map.put(key2, value2);
		return map;
	}

	/**
	 * Constructs a single-entry ordered map  given a CharFilter and four key-value pairs.
	 * This is mostly useful as an optimization for {@link #makeOrderedMap(String, Object, Object...)}
	 * when there's no "rest" of the keys or values.
	 *
	 * @param key0   a String key
	 * @param value0 a V value
	 * @param key1   a String key
	 * @param value1 a V value
	 * @param key2   a String key
	 * @param value2 a V value
	 * @param key3   a String key
	 * @param value3 a V value
	 * @param <V>    the type of value0
	 * @return a new ordered map containing entries mapping each key to the following value
	 */
	public <V> FilteredStringOrderedMap<V> makeOrderedMap(String key0, V value0, String key1, V value1, String key2, V value2, String key3, V value3) {
		FilteredStringOrderedMap<V> map = new FilteredStringOrderedMap<>(this, 4);
		map.put(key0, value0);
		map.put(key1, value1);
		map.put(key2, value2);
		map.put(key3, value3);
		return map;
	}

	/**
	 * Constructs a ordered map given a CharFilter, then alternating keys and values.
	 * This can be useful in some code-generation scenarios, or when you want to make a
	 * ordered map conveniently by-hand and have it populated at the start. You can also use
	 * {@link FilteredStringOrderedMap#FilteredStringOrderedMap(CharFilter, String[], Object[])}, which takes all keys and then all values.
	 * This needs all keys to have the same type and all values to have the same type, because
	 * it gets those types from the first key parameter and first value parameter. Any keys that don't
	 * have String as their type or values that don't have V as their type have that entry skipped.
	 *
	 * @param key0   the first key; will be used to determine the type of all keys
	 * @param value0 the first value; will be used to determine the type of all values
	 * @param rest   an array or varargs of alternating String, V, String, V... elements
	 * @param <V>    the type of values, inferred from value0
	 * @return a new ordered map containing the given keys and values
	 */
	@SuppressWarnings("unchecked")
	public <V> FilteredStringOrderedMap<V> makeOrderedMap(String key0, V value0, Object... rest) {
		FilteredStringOrderedMap<V> map = new FilteredStringOrderedMap<>(this, 1 + (rest.length >>> 1));
		map.put(key0, value0);
		for (int i = 1; i < rest.length; i += 2) {
			try {
				map.put((String) rest[i - 1], (V) rest[i]);
			} catch (ClassCastException ignored) {
			}
		}
		return map;
	}
}

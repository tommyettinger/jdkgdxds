/*
 * Copyright (c) 2022-2023 See AUTHORS file.
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

import com.github.tommyettinger.ds.support.sort.FilteredComparators;
import com.github.tommyettinger.function.CharPredicate;
import com.github.tommyettinger.function.CharToCharFunction;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;

import static com.github.tommyettinger.ds.Utilities.neverIdentical;

/**
 * A custom variant on ObjectObjectOrderedMap that always uses String keys, but only considers any character in an item (for
 * equality and hashing purposes) if that character satisfies a predicate. This can also edit the characters that pass
 * the filter, such as by changing their case during comparisons (and hashing). You will usually want to call
 * {@link #setFilter(CharPredicate)} and/or {@link #setEditor(CharToCharFunction)} to change the behavior of hashing and
 * equality before you enter any items, unless you have specified the filter and/or editor you want in the constructor.
 * <br>
 * You can use this class as a replacement for {@link CaseInsensitiveMap} if you set the editor to a method reference to
 * {@link Character#toUpperCase(char)}. You can go further by setting the editor to make the hashing and equality checks
 * ignore characters that don't satisfy a predicate, such as {@link Character#isLetter(char)}.
 * CaseInsensitiveOrderedMap does allow taking arbitrary CharSequence types as keys, but it doesn't permit modifying
 * them, so usually Strings are a good choice anyway.
 * <br>
 * Be advised that if you use some (most) checks in {@link Character} for properties of a char, and you try to use them
 * on GWT, those checks will not work as expected for non-ASCII characters. Some other platforms might also be affected,
 * such as TeaVM, but it isn't clear yet which platforms have full Unicode support. You can consider depending upon
 * <a href="https://github.com/tommyettinger/RegExodus">RegExodus</a> for more cross-platform Unicode support; a method
 * reference to {@code Category.L::contains} acts like {@code Character::isLetter}, but works on GWT.
 * <br>
 * This is very similar to {@link FilteredStringMap},
 * except that this class maintains insertion order and can be sorted with {@link #sort()}, {@link #sort(Comparator)}, etc.
 * Note that because each String is stored in here in its original form (not modified to make it use the filter and editor),
 * the sorted order might be different than you expect.
 * You can use {@link FilteredComparators#makeStringComparator(CharPredicate, CharToCharFunction)} to create a Comparator
 * for Strings that uses the same rules this class does.
 */
public class FilteredStringOrderedMap<V> extends ObjectObjectOrderedMap<String, V> {
	protected CharPredicate filter = c -> true;
	protected CharToCharFunction editor = c -> c;

	/**
	 * Creates a new map with an initial capacity of 51 and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 */
	public FilteredStringOrderedMap () {
		super();
	}

	/**
	 * Creates a new map with the specified initial capacity and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 * This map will hold initialCapacity items before growing the backing table.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 */
	public FilteredStringOrderedMap (int initialCapacity) {
		super(initialCapacity);
	}

	/**
	 * Creates a new map with the specified initial capacity and load factor. This map will hold initialCapacity items before
	 * growing the backing table.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 * @param loadFactor      what fraction of the capacity can be filled before this has to resize; 0 &lt; loadFactor &lt;= 1
	 */
	public FilteredStringOrderedMap (int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
	}

	/**
	 * Creates a new set with an initial capacity of 51 and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 * This uses the specified filter and editor.
	 *
	 * @param filter a CharPredicate that should return true iff a character should be considered for equality/hashing
	 * @param editor a CharToCharFunction that will take a char from a key String and return a potentially different char
	 */
	public FilteredStringOrderedMap (CharPredicate filter, CharToCharFunction editor) {
		super();
		this.filter = filter;
		this.editor = editor;
	}

	/**
	 * Creates a new set with the specified initial capacity and load factor. This set will hold initialCapacity items before
	 * growing the backing table.
	 * This uses the specified filter and editor.
	 *
	 * @param filter          a CharPredicate that should return true iff a character should be considered for equality/hashing
	 * @param editor          a CharToCharFunction that will take a char from a key String and return a potentially different char
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 * @param loadFactor      what fraction of the capacity can be filled before this has to resize; 0 &lt; loadFactor &lt;= 1
	 */
	public FilteredStringOrderedMap (CharPredicate filter, CharToCharFunction editor, int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
		this.filter = filter;
		this.editor = editor;
	}

	/**
	 * Creates a new map identical to the specified map.
	 *
	 * @param map an FilteredStringOrderedMap to copy
	 */
	public FilteredStringOrderedMap (FilteredStringOrderedMap<? extends V> map) {
		super(map);
		filter = map.filter;
		editor = map.editor;
	}

	/**
	 * Creates a new map identical to the specified map.
	 * This uses the specified filter and editor.
	 *
	 * @param filter a CharPredicate that should return true iff a character should be considered for equality/hashing
	 * @param editor a CharToCharFunction that will take a char from a key String and return a potentially different char
	 * @param map    a Map to copy; ObjectObjectOrderedMap and subclasses of it will be faster to load from
	 */
	public FilteredStringOrderedMap (CharPredicate filter, CharToCharFunction editor, Map<String, ? extends V> map) {
		this(filter, editor, map.size(), Utilities.getDefaultLoadFactor());
		for (String k : map.keySet()) {
			put(k, map.get(k));
		}

	}

	/**
	 * Given two side-by-side arrays, one of keys, one of values, this constructs a map and inserts each pair of key and value into it.
	 * If keys and values have different lengths, this only uses the length of the smaller array.
	 * This uses the specified filter and editor.
	 *
	 * @param filter a CharPredicate that should return true iff a character should be considered for equality/hashing
	 * @param editor a CharToCharFunction that will take a char from a key String and return a potentially different char
	 * @param keys   an array of keys
	 * @param values an array of values
	 */
	public FilteredStringOrderedMap (CharPredicate filter, CharToCharFunction editor, String[] keys, V[] values) {
		this(filter, editor, Math.min(keys.length, values.length), Utilities.getDefaultLoadFactor());
		putAll(keys, values);
	}

	/**
	 * Given two side-by-side collections, one of keys, one of values, this constructs a map and inserts each pair of key and value into it.
	 * If keys and values have different lengths, this only uses the length of the smaller collection.
	 * This uses the specified filter and editor.
	 *
	 * @param filter a CharPredicate that should return true iff a character should be considered for equality/hashing
	 * @param editor a CharToCharFunction that will take a char from a key String and return a potentially different char
	 * @param keys   a Collection of keys
	 * @param values a Collection of values
	 */
	public FilteredStringOrderedMap (CharPredicate filter, CharToCharFunction editor, Collection<String> keys, Collection<? extends V> values) {
		this(filter, editor, Math.min(keys.size(), values.size()), Utilities.getDefaultLoadFactor());
		putAll(keys, values);
	}

	/**
	 * Creates a new set by copying {@code count} items from the given ObjectObjectOrderedMap (or a subclass, such as
	 * CaseInsensitiveOrderedMap), starting at {@code offset} in that Map, into this.
	 *
	 * @param filter a CharPredicate that should return true iff a character should be considered for equality/hashing
	 * @param editor a CharToCharFunction that will take a char from a key String and return a potentially different char
	 * @param other  another ObjectObjectOrderedMap of the same types (key must be String)
	 * @param offset the first index in other's ordering to draw an item from
	 * @param count  how many items to copy from other
	 */
	public FilteredStringOrderedMap (CharPredicate filter, CharToCharFunction editor, ObjectObjectOrderedMap<String, ? extends V> other, int offset, int count) {
		this(filter, editor, count, Utilities.getDefaultLoadFactor());
		putAll(0, other, offset, count);
	}

	public CharPredicate getFilter () {
		return filter;
	}

	/**
	 * Sets the filter that determines which characters in a String are considered for equality and hashing, then
	 * returns this object, for chaining. Common CharPredicate filters you might use could be method references to
	 * {@link Character#isLetter(char)} or {@link CharList#contains(char)}, for example. If the filter returns true for
	 * a given character, that character will be used for hashing/equality; otherwise it will be ignored.
	 * The default filter always returns true. If the filter changes, that invalidates anything previously entered into
	 * this, so before changing the filter <em>this clears the entire data structure</em>, removing all existing items.
	 *
	 * @param filter a CharPredicate that should return true iff a character should be considered for equality/hashing
	 * @return this, for chaining
	 */
	public FilteredStringOrderedMap<V> setFilter (CharPredicate filter) {
		clear();
		this.filter = filter;
		return this;
	}

	public CharToCharFunction getEditor () {
		return editor;
	}

	/**
	 * Sets the editor that can alter the characters in a String when they are being used for equality and hashing. This
	 * does not apply any changes to the Strings in this data structure; it only affects how they are hashed or
	 * compared. Common CharToCharFunction editors you might use could be a method reference to
	 * {@link Character#toUpperCase(char)} (useful for case-insensitivity) or a lambda that could do... anything.
	 * The default filter returns the char it is passed without changes. If the editor changes, that invalidates
	 * anything previously entered into this, so before changing the editor <em>this clears the entire data
	 * structure</em>, removing all existing items.
	 *
	 * @param editor a CharToCharFunction that will take a char from a key String and return a potentially different char
	 * @return this, for chaining
	 */
	public FilteredStringOrderedMap<V> setEditor (CharToCharFunction editor) {
		clear();
		this.editor = editor;
		return this;
	}

	/**
	 * Equivalent to calling {@code mySet.setFilter(filter).setEditor(editor)}, but only clears the data structure once.
	 *
	 * @param filter a CharPredicate that should return true iff a character should be considered for equality/hashing
	 * @param editor a CharToCharFunction that will take a char from a key String and return a potentially different char
	 * @return this, for chaining
	 * @see #setFilter(CharPredicate)
	 * @see #setEditor(CharToCharFunction)
	 */
	public FilteredStringOrderedMap<V> setModifiers (CharPredicate filter, CharToCharFunction editor) {
		clear();
		this.filter = filter;
		this.editor = editor;
		return this;
	}

	protected long hashHelper (String s) {
		long hash = 0x9E3779B97F4A7C15L + hashMultiplier; // golden ratio
		for (int i = 0, len = s.length(); i < len; i++) {
			char c = s.charAt(i);
			if (filter.test(c)) {
				hash = (hash + editor.applyAsChar(c)) * hashMultiplier;
			}
		}
		return hash;
	}

	@Override
	protected int place (Object item) {
		if (item instanceof String) {
			return (int)(hashHelper((String)item) >>> shift);
		}
		return super.place(item);
	}

	/**
	 * Compares two objects for equality by the rules this filtered data structure uses for keys.
	 * This will return true if the arguments are reference-equivalent or both null. Otherwise, it
	 * requires that both are {@link String}s and compares them using the {@link #getFilter() filter}
	 * and {@link #getEditor() editor} of this object.
	 *
	 * @param left  must be non-null; typically a key being compared, but not necessarily
	 * @param right may be null; typically a key being compared, but can often be null for an empty key slot, or some other type
	 * @return true if left and right are equivalent according to the rules this filtered type uses
	 */
	@Override
	public boolean equate (Object left, @Nullable Object right) {
		if (left == right)
			return true;
		if (right == null)
			return false;
		if ((left instanceof String) && (right instanceof String)) {
			String l = (String)left, r = (String)right;
			int llen = l.length(), rlen = r.length();
			int cl = -1, cr = -1;
			int i = 0, j = 0;
			while (i < llen || j < rlen) {
				if (i == llen)
					cl = -1;
				else {
					while (i < llen && !filter.test((char)(cl = l.charAt(i++)))) {
						cl = -1;
					}
				}
				if (j == rlen)
					cr = -1;
				else {
					while (j < rlen && !filter.test((char)(cr = r.charAt(j++)))) {
						cr = -1;
					}
				}
				if (cl != cr && editor.applyAsChar((char)cl) != editor.applyAsChar((char)cr))
					return false;
			}
			return true;
		}
		return false;
	}

	@Override
	public int hashCode () {
		int h = size;
		String[] keyTable = this.keyTable;
		V[] valueTable = this.valueTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			String key = keyTable[i];
			if (key != null) {
				h ^= hashHelper(key);
				V value = valueTable[i];
				if (value != null) {h ^= value.hashCode();}
			}
		}
		return h;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	@Override
	public boolean equals (Object obj) {
		if (obj == this) {return true;}
		if (!(obj instanceof Map)) {return false;}
		Map other = (Map)obj;
		if (other.size() != size) {return false;}
		Object[] keyTable = this.keyTable;
		V[] valueTable = this.valueTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			Object key = keyTable[i];
			if (key != null) {
				V value = valueTable[i];
				if (value == null) {
					if (other.getOrDefault(key, neverIdentical) != null) {return false;}
				} else {
					if (!value.equals(other.get(key))) {return false;}
				}
			}
		}
		return true;
	}

	/**
	 * Constructs a single-entry map given one key and one value.
	 * This is mostly useful as an optimization for {@link #with(Object, Object, Object...)}
	 * when there's no "rest" of the keys or values.
	 *
	 * @param filter a CharPredicate that should return true iff a character should be considered for equality/hashing
	 * @param editor a CharToCharFunction that will take a char from a key String and return a potentially different char
	 * @param key0   the first and only key
	 * @param value0 the first and only value
	 * @param <V>    the type of value0
	 * @return a new map containing just the entry mapping key0 to value0
	 */
	public static <V> FilteredStringOrderedMap<V> with (CharPredicate filter, CharToCharFunction editor, String key0, V value0) {
		FilteredStringOrderedMap<V> map = new FilteredStringOrderedMap<>(filter, editor, 1, Utilities.getDefaultLoadFactor());
		map.put(key0, value0);
		return map;
	}

	/**
	 * Constructs a map given alternating keys and values.
	 * This can be useful in some code-generation scenarios, or when you want to make a
	 * map conveniently by-hand and have it populated at the start. You can also use
	 * {@link #FilteredStringOrderedMap(CharPredicate, CharToCharFunction, String[], Object[])},
	 * which takes all keys and then all values.
	 * This needs all keys to be {@code String}s and all values to
	 * have the same type, because it gets those types from the first value parameter. Any keys that
	 * aren't Strings or values that don't have V as their type have that entry skipped.
	 *
	 * @param filter a CharPredicate that should return true iff a character should be considered for equality/hashing
	 * @param editor a CharToCharFunction that will take a char from a key String and return a potentially different char
	 * @param key0   the first key
	 * @param value0 the first value; will be used to determine the type of all values
	 * @param rest   an array or varargs of alternating K, V, K, V... elements
	 * @param <V>    the type of values, inferred from value0
	 * @return a new map containing the given keys and values
	 */
	@SuppressWarnings("unchecked")
	public static <V> FilteredStringOrderedMap<V> with (CharPredicate filter, CharToCharFunction editor, String key0, V value0, Object... rest) {
		FilteredStringOrderedMap<V> map = new FilteredStringOrderedMap<>(filter, editor, 1 + (rest.length >>> 1), Utilities.getDefaultLoadFactor());
		map.put(key0, value0);
		for (int i = 1; i < rest.length; i += 2) {
			try {
				map.put((String)rest[i - 1], (V)rest[i]);
			} catch (ClassCastException ignored) {
			}
		}
		return map;
	}
}
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

package com.github.tommyettinger.ds;

import com.github.tommyettinger.digital.BitConversion;
import com.github.tommyettinger.ds.support.util.PartialParser;
import com.github.tommyettinger.function.CharPredicate;
import com.github.tommyettinger.function.CharToCharFunction;

import java.util.Collection;

/**
 * A customizable variant on ObjectSet that always uses String keys, but only considers any character in an item (for
 * equality and hashing purposes) if that character satisfies a predicate. This can also edit the characters that pass
 * the filter, such as by changing their case during comparisons (and hashing). You will usually want to call
 * {@link #setFilter(CharFilter)} to change the behavior of hashing and equality before you enter any items, unless you
 * have specified the CharFilter you want in the constructor.
 * <br>
 * You can use this class as a replacement for {@link CaseInsensitiveSet} if you set the editor to a method reference to
 * {@link Character#toUpperCase(char)} or {@link Casing#caseUp(char)}. You can go further by setting the filter to make
 * the hashing and equality checks ignore characters that don't satisfy a predicate, such as {@link Character#isLetter(char)}.
 * CaseInsensitiveSet does allow taking arbitrary CharSequence types as keys, but it doesn't permit modifying
 * them, so usually Strings are a good choice anyway.
 * <br>
 * Be advised that if you use some (most) checks in {@link Character} for properties of a char, and you try to use them
 * on GWT, those checks will not work as expected for non-ASCII characters. Some other platforms might also be affected,
 * such as TeaVM, but it isn't clear yet which platforms have full Unicode support. You can consider depending upon
 * <a href="https://github.com/tommyettinger/RegExodus">RegExodus</a> for more cross-platform Unicode support; a method
 * reference to {@code Category.L::contains} acts like {@code Character::isLetter}, but works on GWT.
 * {@code com.github.tommyettinger.ds.support.util.CharPredicates} provides a few common CharPredicate constants that
 * will work identically on all platforms.
 */
public class FilteredStringSet extends ObjectSet<String> {
	protected CharFilter filter = CharFilter.getOrCreate("Identity", c -> true, c -> c);

	/**
	 * Creates a new set with an initial capacity of {@link Utilities#getDefaultTableCapacity()} and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 * This considers all characters in a String key and does not edit them.
	 */
	public FilteredStringSet() {
		super();
	}

	/**
	 * Creates a new set with the specified initial capacity a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 * This set will hold initialCapacity items before growing the backing table.
	 * This considers all characters in a String key and does not edit them.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 */
	public FilteredStringSet(int initialCapacity) {
		super(initialCapacity);
	}

	/**
	 * Creates a new set with the specified initial capacity and load factor. This set will hold initialCapacity items before
	 * growing the backing table.
	 * This considers all characters in a String key and does not edit them.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 * @param loadFactor      what fraction of the capacity can be filled before this has to resize; 0 &lt; loadFactor &lt;= 1
	 */
	public FilteredStringSet(int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
	}

	/**
	 * Creates a new set with an initial capacity of {@link Utilities#getDefaultTableCapacity()} and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 * This uses the specified CharFilter.
	 *
	 * @param filter a CharFilter that can be obtained with {@link CharFilter#getOrCreate(String, CharPredicate, CharToCharFunction)}
	 */
	public FilteredStringSet(CharFilter filter) {
		super();
		this.filter = filter;
	}

	/**
	 * Creates a new set with the specified initial capacity and the default load factor. This set will hold initialCapacity items
	 * before growing the backing table.
	 * This uses the specified CharFilter.
	 *
	 * @param filter          a CharFilter that can be obtained with {@link CharFilter#getOrCreate(String, CharPredicate, CharToCharFunction)}
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 */
	public FilteredStringSet(CharFilter filter, int initialCapacity) {
		super(initialCapacity);
		this.filter = filter;
	}

	/**
	 * Creates a new set with the specified initial capacity and load factor. This set will hold initialCapacity items before
	 * growing the backing table.
	 * This uses the specified CharFilter.
	 *
	 * @param filter          a CharFilter that can be obtained with {@link CharFilter#getOrCreate(String, CharPredicate, CharToCharFunction)}
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 * @param loadFactor      what fraction of the capacity can be filled before this has to resize; 0 &lt; loadFactor &lt;= 1
	 */
	public FilteredStringSet(CharFilter filter, int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
		this.filter = filter;
	}

	/**
	 * Creates a new set identical to the specified set.
	 *
	 * @param set another FilteredStringSet to copy
	 */
	public FilteredStringSet(FilteredStringSet set) {
		super(set.size(), set.loadFactor);
		filter = set.filter;
		this.hashMultiplier = set.hashMultiplier;
		addAll(set);
	}

	/**
	 * Creates a new set that contains all distinct elements in {@code coll}.
	 * This uses the specified CharFilter, including while it enters the items in coll.
	 *
	 * @param filter a CharFilter that can be obtained with {@link CharFilter#getOrCreate(String, CharPredicate, CharToCharFunction)}
	 * @param coll   a Collection implementation to copy, such as an ObjectList or a Set that isn't a FilteredStringSet
	 */
	public FilteredStringSet(CharFilter filter, Collection<? extends String> coll) {
		this(filter, coll.size());
		addAll(coll);
	}

	/**
	 * Creates a new set using {@code length} items from the given {@code array}, starting at {@code} offset (inclusive).
	 * This uses the specified CharFilter, including while it enters the items in array.
	 *
	 * @param filter a CharFilter that can be obtained with {@link CharFilter#getOrCreate(String, CharPredicate, CharToCharFunction)}
	 * @param array  an array to draw items from
	 * @param offset the first index in array to draw an item from
	 * @param length how many items to take from array; bounds-checking is the responsibility of the using code
	 */
	public FilteredStringSet(CharFilter filter, String[] array, int offset, int length) {
		this(filter, length);
		addAll(array, offset, length);
	}

	/**
	 * Creates a new set containing all the items in the given array.
	 * This uses the specified CharFilter, including while it enters the items in array.
	 *
	 * @param filter a CharFilter that can be obtained with {@link CharFilter#getOrCreate(String, CharPredicate, CharToCharFunction)}
	 * @param array  an array that will be used in full, except for duplicate items
	 */
	public FilteredStringSet(CharFilter filter, String[] array) {
		this(filter, array, 0, array.length);
	}

	public CharFilter getFilter() {
		return filter;
	}

	/**
	 * Sets the CharFilter that determines which characters in a String are considered for equality and hashing, as well
	 * as any changes made to characters before hashing or equating, then
	 * returns this object, for chaining. If the filter changes, that invalidates anything previously entered into
	 * this, so before changing the filter <em>this clears the entire data structure</em>, removing all existing items.
	 *
	 * @param filter a CharFilter that can be obtained with {@link CharFilter#getOrCreate(String, CharPredicate, CharToCharFunction)}
	 * @return this, for chaining
	 */
	public FilteredStringSet setFilter(CharFilter filter) {
		clear();
		this.filter = filter;
		return this;
	}

	/**
	 * Gets a low-to-moderate quality 32-bit hash code from the given String.
	 * This operates by checking if a char in {@code s} matches the filter, and if it does, it rotates the current hash,
	 * multiplies it by the {@link #getHashMultiplier() hash multiplier}, and XORs with the current char after editing.
	 * This finalizes the hash by multiplying it again by the hash multiplier, then using the reversible
	 * XOR-rotate-XOR-rotate sequence of operations to adequately jumble the bits.
	 *
	 * @param s a String to hash
	 * @return a 32-bit hash of {@code s}
	 */
	protected int hashHelper(final String s) {
		final int hm = hashMultiplier;
		int hash = hm;
		for (int i = 0, len = s.length(); i < len; i++) {
			final char c = s.charAt(i);
			if (filter.filter.test(c)) {
				hash = BitConversion.imul((hash << 13 | hash >>> 19), hm) ^ filter.editor.applyAsChar(c);
			}
		}
		hash = BitConversion.imul(hash, hm);
		return hash ^ (hash << 23 | hash >>> 9) ^ (hash << 11 | hash >>> 21);
	}

	@Override
	protected int place(Object item) {
		if (item instanceof String) {
			return hashHelper((String) item) & mask;
		}
		return super.place(item);
	}

	/**
	 * Compares two objects for equality by the rules this filtered data structure uses for keys.
	 * This will return true if the arguments are reference-equivalent or both null. Otherwise, it
	 * requires that both are {@link String}s and compares them using the {@link #getFilter() filter}
	 * of this object.
	 *
	 * @param left  must be non-null; typically a key being compared, but not necessarily
	 * @param right may be null; typically a key being compared, but can often be null for an empty key slot, or some other type
	 * @return true if left and right are equivalent according to the rules this filtered type uses
	 */
	@Override
	public boolean equate(Object left, Object right) {

		if (left == right)
			return true;
		if (right == null) return false;
		if ((left instanceof String) && (right instanceof String)) {
			String l = (String) left, r = (String) right;
			int llen = l.length(), rlen = r.length();
			int cl = -1, cr = -1;
			int i = 0, j = 0;
			while (i < llen || j < rlen) {
				if (i == llen) cl = -1;
				else {
					while (i < llen && !filter.filter.test((char) (cl = l.charAt(i++)))) {
						cl = -1;
					}
				}
				if (j == rlen) cr = -1;
				else {
					while (j < rlen && !filter.filter.test((char) (cr = r.charAt(j++)))) {
						cr = -1;
					}
				}
				if (cl != cr && filter.editor.applyAsChar((char) cl) != filter.editor.applyAsChar((char) cr))
					return false;
			}
			return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		int h = size;
		String[] keyTable = this.keyTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			String key = keyTable[i];
			if (key != null) {
				h += hashHelper(key);
			}
		}
		return h ^ h >>> 16;
	}

	/**
	 * Constructs an empty set given a CharFilter.
	 * This is usually less useful than just using the constructor, but can be handy
	 * in some code-generation scenarios when you don't know how many arguments you will have.
	 *
	 * @param filter a CharFilter that can be obtained with {@link CharFilter#getOrCreate(String, CharPredicate, CharToCharFunction)}
	 * @return a new set containing nothing
	 */
	public static FilteredStringSet with(CharFilter filter) {
		return new FilteredStringSet(filter, 0);
	}

	/**
	 * Creates a new FilteredStringSet that holds only the given item, but can be resized.
	 *
	 * @param filter a CharFilter that can be obtained with {@link CharFilter#getOrCreate(String, CharPredicate, CharToCharFunction)}
	 * @param item   one String item
	 * @return a new FilteredStringSet that holds the given item
	 */
	public static FilteredStringSet with(CharFilter filter, String item) {
		FilteredStringSet set = new FilteredStringSet(filter, 1);
		set.add(item);
		return set;
	}

	/**
	 * Creates a new FilteredStringSet that holds only the given items, but can be resized.
	 *
	 * @param filter a CharFilter that can be obtained with {@link CharFilter#getOrCreate(String, CharPredicate, CharToCharFunction)}
	 * @param item0  a String item
	 * @param item1  a String item
	 * @return a new FilteredStringSet that holds the given items
	 */
	public static FilteredStringSet with(CharFilter filter, String item0, String item1) {
		FilteredStringSet set = new FilteredStringSet(filter, 2);
		set.add(item0, item1);
		return set;
	}

	/**
	 * Creates a new FilteredStringSet that holds only the given items, but can be resized.
	 *
	 * @param filter a CharFilter that can be obtained with {@link CharFilter#getOrCreate(String, CharPredicate, CharToCharFunction)}
	 * @param item0  a String item
	 * @param item1  a String item
	 * @param item2  a String item
	 * @return a new FilteredStringSet that holds the given items
	 */
	public static FilteredStringSet with(CharFilter filter, String item0, String item1, String item2) {
		FilteredStringSet set = new FilteredStringSet(filter, 3);
		set.add(item0, item1, item2);
		return set;
	}

	/**
	 * Creates a new FilteredStringSet that holds only the given items, but can be resized.
	 *
	 * @param filter a CharFilter that can be obtained with {@link CharFilter#getOrCreate(String, CharPredicate, CharToCharFunction)}
	 * @param item0  a String item
	 * @param item1  a String item
	 * @param item2  a String item
	 * @param item3  a String item
	 * @return a new FilteredStringSet that holds the given items
	 */
	public static FilteredStringSet with(CharFilter filter, String item0, String item1, String item2, String item3) {
		FilteredStringSet set = new FilteredStringSet(filter, 4);
		set.add(item0, item1, item2, item3);
		return set;
	}

	/**
	 * Creates a new FilteredStringSet that holds only the given items, but can be resized.
	 *
	 * @param filter a CharFilter that can be obtained with {@link CharFilter#getOrCreate(String, CharPredicate, CharToCharFunction)}
	 * @param item0  a String item
	 * @param item1  a String item
	 * @param item2  a String item
	 * @param item3  a String item
	 * @param item4  a String item
	 * @return a new FilteredStringSet that holds the given items
	 */
	public static FilteredStringSet with(CharFilter filter, String item0, String item1, String item2, String item3, String item4) {
		FilteredStringSet set = new FilteredStringSet(filter, 5);
		set.add(item0, item1, item2, item3);
		set.add(item4);
		return set;
	}

	/**
	 * Creates a new FilteredStringSet that holds only the given items, but can be resized.
	 *
	 * @param filter a CharFilter that can be obtained with {@link CharFilter#getOrCreate(String, CharPredicate, CharToCharFunction)}
	 * @param item0  a String item
	 * @param item1  a String item
	 * @param item2  a String item
	 * @param item3  a String item
	 * @param item4  a String item
	 * @param item5  a String item
	 * @return a new FilteredStringSet that holds the given items
	 */
	public static FilteredStringSet with(CharFilter filter, String item0, String item1, String item2, String item3, String item4, String item5) {
		FilteredStringSet set = new FilteredStringSet(filter, 6);
		set.add(item0, item1, item2, item3);
		set.add(item4, item5);
		return set;
	}

	/**
	 * Creates a new FilteredStringSet that holds only the given items, but can be resized.
	 *
	 * @param filter a CharFilter that can be obtained with {@link CharFilter#getOrCreate(String, CharPredicate, CharToCharFunction)}
	 * @param item0  a String item
	 * @param item1  a String item
	 * @param item2  a String item
	 * @param item3  a String item
	 * @param item4  a String item
	 * @param item5  a String item
	 * @param item6  a String item
	 * @return a new FilteredStringSet that holds the given items
	 */
	public static FilteredStringSet with(CharFilter filter, String item0, String item1, String item2, String item3, String item4, String item5, String item6) {
		FilteredStringSet set = new FilteredStringSet(filter, 7);
		set.add(item0, item1, item2, item3);
		set.add(item4, item5, item6);
		return set;
	}

	/**
	 * Creates a new FilteredStringSet that holds only the given items, but can be resized.
	 *
	 * @param filter a CharFilter that can be obtained with {@link CharFilter#getOrCreate(String, CharPredicate, CharToCharFunction)}
	 * @param item0  a String item
	 * @param item1  a String item
	 * @param item2  a String item
	 * @param item3  a String item
	 * @param item4  a String item
	 * @param item5  a String item
	 * @param item6  a String item
	 * @return a new FilteredStringSet that holds the given items
	 */
	public static FilteredStringSet with(CharFilter filter, String item0, String item1, String item2, String item3, String item4, String item5, String item6, String item7) {
		FilteredStringSet set = new FilteredStringSet(filter, 8);
		set.add(item0, item1, item2, item3);
		set.add(item4, item5, item6, item7);
		return set;
	}

	/**
	 * Creates a new FilteredStringSet that holds only the given items, but can be resized.
	 * This overload will only be used when an array is supplied or if varargs are used and
	 * there are 9 or more arguments.
	 *
	 * @param filter  a CharFilter that can be obtained with {@link CharFilter#getOrCreate(String, CharPredicate, CharToCharFunction)}
	 * @param varargs a String varargs or String array; remember that varargs allocate
	 * @return a new FilteredStringSet that holds the given items
	 */
	public static FilteredStringSet with(CharFilter filter, String... varargs) {
		return new FilteredStringSet(filter, varargs);
	}

	/**
	 * Calls {@link #parse(String, String, PartialParser, boolean)} with brackets set to false.
	 * @param filter a CharFilter that can be obtained with {@link CharFilter#getOrCreate(String, CharPredicate, CharToCharFunction)}
	 * @param str a String that will be parsed in full
	 * @param delimiter the delimiter between items in str
	 * @return a new collection parsed from str
	 */
	public static FilteredStringSet parse(CharFilter filter, String str, String delimiter) {
		return parse(filter, str, delimiter, false);
	}

	/**
	 * Creates a new FilteredStringSet using {@code filter} and fills it by calling
	 * {@link #addLegible(String, String, PartialParser, int, int)} on
	 * either all of {@code str} (if {@code brackets} is false) or {@code str} without its first and last chars (if
	 * {@code brackets} is true). Each item is expected to be separated by {@code delimiter}.
	 *
	 * @param filter a CharFilter that can be obtained with {@link CharFilter#getOrCreate(String, CharPredicate, CharToCharFunction)}
	 * @param str a String that will be parsed in full (depending on brackets)
	 * @param delimiter the delimiter between items in str
	 * @param brackets if true, the first and last chars in str will be ignored
	 * @return a new collection parsed from str
	 */
	public static FilteredStringSet parse(CharFilter filter, String str, String delimiter, boolean brackets) {
		FilteredStringSet c = new FilteredStringSet(filter);
		if(brackets)
			c.addLegible(str, delimiter, PartialParser.DEFAULT_STRING, 1, str.length() - 1);
		else
			c.addLegible(str, delimiter, PartialParser.DEFAULT_STRING);
		return c;
	}

	/**
	 * Creates a new FilteredStringSet using {@code filter} and fills it by calling
	 * {@link #addLegible(String, String, PartialParser, int, int)}, using {@link PartialParser#DEFAULT_STRING} and
	 * with the other four parameters as-is.
	 *
	 * @param filter a CharFilter that can be obtained with {@link CharFilter#getOrCreate(String, CharPredicate, CharToCharFunction)}
	 * @param str a String that will have the given section parsed
	 * @param delimiter the delimiter between items in str
	 * @param offset the first position to parse in str, inclusive
	 * @param length how many chars to parse, starting from offset
	 * @return a new collection parsed from str
	 */
	public static FilteredStringSet parse(CharFilter filter, String str, String delimiter, int offset, int length) {
		FilteredStringSet c = new FilteredStringSet(filter);
		c.addLegible(str, delimiter, PartialParser.DEFAULT_STRING, offset, length);
		return c;
	}
}

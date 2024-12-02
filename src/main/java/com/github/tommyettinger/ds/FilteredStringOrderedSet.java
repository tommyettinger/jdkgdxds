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
 */

package com.github.tommyettinger.ds;

import com.github.tommyettinger.digital.BitConversion;
import com.github.tommyettinger.ds.support.sort.FilteredComparators;
import com.github.tommyettinger.function.CharPredicate;
import com.github.tommyettinger.function.CharToCharFunction;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.Comparator;

/**
 * A customizable variant on ObjectOrderedSet that always uses String keys, but only considers any character in an item
 * (for equality and hashing purposes) if that character satisfies a predicate. This can also edit the characters that
 * pass the filter, such as by changing their case during comparisons (and hashing). You will usually want to call
 * {@link #setFilter(CharFilter)} to change the behavior of hashing and equality before you enter any items, unless you
 * have specified the CharFilter you want in the constructor.
 * <br>
 * You can use this class as a replacement for {@link CaseInsensitiveOrderedSet} if you set the editor to a method reference to
 * {@link Character#toUpperCase(char)}. You can go further by setting the filter to make the hashing and equality checks
 * ignore characters that don't satisfy a predicate, such as {@link Character#isLetter(char)}.
 * CaseInsensitiveOrderedSet does allow taking arbitrary CharSequence types as keys, but it doesn't permit modifying
 * them, so usually Strings are a good choice anyway.
 * <br>
 * Be advised that if you use some (most) checks in {@link Character} for properties of a char, and you try to use them
 * on GWT, those checks will not work as expected for non-ASCII characters. Some other platforms might also be affected,
 * such as TeaVM, but it isn't clear yet which platforms have full Unicode support. You can consider depending upon
 * <a href="https://github.com/tommyettinger/RegExodus">RegExodus</a> for more cross-platform Unicode support; a method
 * reference to {@code Category.L::contains} acts like {@code Character::isLetter}, but works on GWT.
 * <br>
 * This is very similar to {@link FilteredStringSet},
 * except that this class maintains insertion order and can be sorted with {@link #sort()}, {@link #sort(Comparator)}, etc.
 * Note that because each String is stored in here in its original form (not modified to make it use the filter and editor),
 * the sorted order might be different than you expect.
 * You can use {@link FilteredComparators#makeStringComparator(CharPredicate, CharToCharFunction)} to create a Comparator
 * for Strings that uses the same rules this class does.
 */
public class FilteredStringOrderedSet extends ObjectOrderedSet<String> {
	protected CharFilter filter = CharFilter.getOrCreate("Identity", c -> true, c -> c);

	/**
	 * Used by {@link #place(Object)} to mix hashCode() results. Changes on every call to {@link #resize(int)} by default.
	 * This only needs to be serialized if the full key and value tables are serialized, or if the iteration order should be
	 * the same before and after serialization. Iteration order is better handled by using {@link ObjectOrderedSet}.
	 */
	protected int hashMultiplier = 0xEFAA28F1;

	/**
	 * Creates a new set with an initial capacity of 51 and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 * This considers all characters in a String key and does not edit them.
	 */
	public FilteredStringOrderedSet () {
		super();
	}

	/**
	 * Creates a new set with the specified initial capacity a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 * This set will hold initialCapacity items before growing the backing table.
	 * This considers all characters in a String key and does not edit them.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 */
	public FilteredStringOrderedSet (int initialCapacity) {
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
	public FilteredStringOrderedSet (int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
	}

	/**
	 * Creates a new set with an initial capacity of 51 and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 * This uses the specified CharFilter.
	 *
	 * @param filter a CharFilter that can be obtained with {@link CharFilter#getOrCreate(String, CharPredicate, CharToCharFunction)}
	 */
	public FilteredStringOrderedSet (CharFilter filter) {
		super();
		this.filter = filter;
	}

	/**
	 * Creates a new set with the specified initial capacity and the default load factor. This set will hold initialCapacity items
	 * before growing the backing table.
	 * This uses the specified CharFilter.
	 *
	 * @param filter a CharFilter that can be obtained with {@link CharFilter#getOrCreate(String, CharPredicate, CharToCharFunction)}
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 */
	public FilteredStringOrderedSet (CharFilter filter, int initialCapacity) {
		super(initialCapacity);
		this.filter = filter;
	}

	/**
	 * Creates a new set with the specified initial capacity and load factor. This set will hold initialCapacity items before
	 * growing the backing table.
	 * This uses the specified CharFilter.
	 *
	 * @param filter a CharFilter that can be obtained with {@link CharFilter#getOrCreate(String, CharPredicate, CharToCharFunction)}
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 * @param loadFactor      what fraction of the capacity can be filled before this has to resize; 0 &lt; loadFactor &lt;= 1
	 */
	public FilteredStringOrderedSet (CharFilter filter, int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
		this.filter = filter;
	}

	/**
	 * Creates a new set identical to the specified set.
	 *
	 * @param set another FilteredStringOrderedSet to copy
	 */
	public FilteredStringOrderedSet (FilteredStringOrderedSet set) {
		super(set.size());
		filter = set.filter;
		this.hashMultiplier = set.hashMultiplier;
		addAll(set);
	}

	/**
	 * Creates a new set that contains all distinct elements in {@code coll}.
	 * This uses the specified CharFilter, including while it enters the items in coll.
	 *
	 * @param filter a CharFilter that can be obtained with {@link CharFilter#getOrCreate(String, CharPredicate, CharToCharFunction)}
	 * @param coll a Collection implementation to copy, such as an ObjectList or a Set that isn't a FilteredStringOrderedSet
	 */
	public FilteredStringOrderedSet (CharFilter filter, Collection<? extends String> coll) {
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
	public FilteredStringOrderedSet (CharFilter filter, String[] array, int offset, int length) {
		this(filter, length);
		addAll(array, offset, length);
	}

	/**
	 * Creates a new set containing all the items in the given array.
	 * This uses the specified CharFilter, including while it enters the items in array.
	 *
	 * @param filter a CharFilter that can be obtained with {@link CharFilter#getOrCreate(String, CharPredicate, CharToCharFunction)}
	 * @param array an array that will be used in full, except for duplicate items
	 */
	public FilteredStringOrderedSet (CharFilter filter, String[] array) {
		this(filter, array, 0, array.length);
	}

	/**
	 * Creates a new set using {@code count} items from the given {@code ordered}, starting at {@code} offset (inclusive).
	 * This uses the specified CharFilter, including while it enters the items in ordered.
	 *
	 * @param filter a CharFilter that can be obtained with {@link CharFilter#getOrCreate(String, CharPredicate, CharToCharFunction)}
	 * @param ordered  an ordered to draw items from
	 * @param offset the first index in ordered to draw an item from
	 * @param count  how many items to take from ordered; bounds-checking is the responsibility of the using code
	 */
	public FilteredStringOrderedSet (CharFilter filter, Ordered<String> ordered, int offset, int count) {
		this(filter, count);
		addAll(0, ordered, offset, count);
	}

	public CharFilter getFilter() {
		return filter;
	}

	/**
	 * Sets the CharFilter that determines which characters in a String are considered for equality and hashing, as well
	 * as any changes made to characters before hashing or equating, then
	 * returns this object, for chaining. If the filter changes, that invalidates anything previously entered into
	 * this, so before changing the filter <em>this clears the entire data structure</em>, removing all existing items.
	 * @param filter a CharFilter that can be obtained with {@link CharFilter#getOrCreate(String, CharPredicate, CharToCharFunction)}
	 * @return this, for chaining
	 */
	public FilteredStringOrderedSet setFilter(CharFilter filter) {
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
	 * @param s a String to hash
	 * @return a 32-bit hash of {@code s}
	 */
	protected int hashHelper (final String s) {
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
	protected int place (@NonNull Object item) {
		if (item instanceof String) {
			return hashHelper((String) item) & mask;
		}
		return super.place(item);
	}

	/**
	 * This actually does something here because the hash multiplier can change.
	 *
	 * @return this class' current hash multiplier
	 */
	@Override
	public int getHashMultiplier() {
		return hashMultiplier;
	}

	/**
	 * This actually does something here because the hash multiplier can change.
	 * The {@code mul} will be made negative and odd if it wasn't both already.
	 *
	 * @param mul any int; will be made negative and odd before using
	 */
	@Override
	public void setHashMultiplier(int mul) {
		hashMultiplier = mul | 0x80000001;
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
	public boolean equate (Object left, @Nullable Object right) {

		if (left == right)
			return true;
		if(right == null) return false;
		if ((left instanceof String) && (right instanceof String)) {
			String l = (String)left, r = (String)right;
			int llen = l.length(), rlen = r.length();
			int cl = -1, cr = -1;
			int i = 0, j = 0;
			while (i < llen || j < rlen) {
				if(i == llen) cl = -1;
				else {
					while (i < llen && !filter.filter.test((char) (cl = l.charAt(i++)))) {
						cl = -1;
					}
				}
				if(j == rlen) cr = -1;
				else {
					while (j < rlen && !filter.filter.test((char) (cr = r.charAt(j++)))) {
						cr = -1;
					}
				}
				if(cl != cr && filter.editor.applyAsChar((char)cl) != filter.editor.applyAsChar((char)cr))
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
		for (int i = 0, n = keyTable.length; i < n; i++) {
			String key = keyTable[i];
			if (key != null) {h += hashHelper(key);}
		}
		return h;
	}

	protected void resize (int newSize) {
		int oldCapacity = keyTable.length;
		threshold = (int)(newSize * loadFactor);
		mask = newSize - 1;
		shift = BitConversion.countLeadingZeros(mask) + 32;

		hashMultiplier = Utilities.GOOD_MULTIPLIERS[BitConversion.imul(hashMultiplier, shift) >>> 5 & 511];
		@Nullable String[] oldKeyTable = keyTable;

		keyTable = new String[newSize];

		if (size > 0) {
			for (int i = 0; i < oldCapacity; i++) {
				String key = oldKeyTable[i];
				if (key != null) {addResize(key);}
			}
		}
	}

	/**
	 * Constructs an empty set given a CharFilter.
	 * This is usually less useful than just using the constructor, but can be handy
	 * in some code-generation scenarios when you don't know how many arguments you will have.
	 *
	 * @param filter a CharFilter that can be obtained with {@link CharFilter#getOrCreate(String, CharPredicate, CharToCharFunction)}
	 * @return a new set containing nothing
	 */
	public static FilteredStringOrderedSet with(CharFilter filter) {
		return new FilteredStringOrderedSet(filter, 0);
	}

	/**
	 * Creates a new FilteredStringOrderedSet that holds only the given item, but can be resized.
	 * @param filter a CharFilter that can be obtained with {@link CharFilter#getOrCreate(String, CharPredicate, CharToCharFunction)}
	 * @param item one String item
	 * @return a new FilteredStringOrderedSet that holds the given item
	 */
	public static FilteredStringOrderedSet with(CharFilter filter, String item) {
		FilteredStringOrderedSet set = new FilteredStringOrderedSet(filter, 1);
		set.add(item);
		return set;
	}

	/**
	 * Creates a new FilteredStringOrderedSet that holds only the given items, but can be resized.
	 * @param filter a CharFilter that can be obtained with {@link CharFilter#getOrCreate(String, CharPredicate, CharToCharFunction)}
	 * @param item0 a String item
	 * @param item1 a String item
	 * @return a new FilteredStringOrderedSet that holds the given items
	 */
	public static FilteredStringOrderedSet with(CharFilter filter, String item0, String item1) {
		FilteredStringOrderedSet set = new FilteredStringOrderedSet(filter, 2);
		set.add(item0, item1);
		return set;
	}

	/**
	 * Creates a new FilteredStringOrderedSet that holds only the given items, but can be resized.
	 * @param filter a CharFilter that can be obtained with {@link CharFilter#getOrCreate(String, CharPredicate, CharToCharFunction)}
	 * @param item0 a String item
	 * @param item1 a String item
	 * @param item2 a String item
	 * @return a new FilteredStringOrderedSet that holds the given items
	 */
	public static FilteredStringOrderedSet with(CharFilter filter, String item0, String item1, String item2) {
		FilteredStringOrderedSet set = new FilteredStringOrderedSet(filter, 3);
		set.add(item0, item1, item2);
		return set;
	}

	/**
	 * Creates a new FilteredStringOrderedSet that holds only the given items, but can be resized.
	 * @param filter a CharFilter that can be obtained with {@link CharFilter#getOrCreate(String, CharPredicate, CharToCharFunction)}
	 * @param item0 a String item
	 * @param item1 a String item
	 * @param item2 a String item
	 * @param item3 a String item
	 * @return a new FilteredStringOrderedSet that holds the given items
	 */
	public static FilteredStringOrderedSet with(CharFilter filter, String item0, String item1, String item2, String item3) {
		FilteredStringOrderedSet set = new FilteredStringOrderedSet(filter, 4);
		set.add(item0, item1, item2, item3);
		return set;
	}

	/**
	 * Creates a new FilteredStringOrderedSet that holds only the given items, but can be resized.
	 * @param filter a CharFilter that can be obtained with {@link CharFilter#getOrCreate(String, CharPredicate, CharToCharFunction)}
	 * @param item0 a String item
	 * @param item1 a String item
	 * @param item2 a String item
	 * @param item3 a String item
	 * @param item4 a String item
	 * @return a new FilteredStringOrderedSet that holds the given items
	 */
	public static FilteredStringOrderedSet with(CharFilter filter, String item0, String item1, String item2, String item3, String item4) {
		FilteredStringOrderedSet set = new FilteredStringOrderedSet(filter, 5);
		set.add(item0, item1, item2, item3);
		set.add(item4);
		return set;
	}

	/**
	 * Creates a new FilteredStringOrderedSet that holds only the given items, but can be resized.
	 * @param filter a CharFilter that can be obtained with {@link CharFilter#getOrCreate(String, CharPredicate, CharToCharFunction)}
	 * @param item0 a String item
	 * @param item1 a String item
	 * @param item2 a String item
	 * @param item3 a String item
	 * @param item4 a String item
	 * @param item5 a String item
	 * @return a new FilteredStringOrderedSet that holds the given items
	 */
	public static FilteredStringOrderedSet with(CharFilter filter, String item0, String item1, String item2, String item3, String item4, String item5) {
		FilteredStringOrderedSet set = new FilteredStringOrderedSet(filter, 6);
		set.add(item0, item1, item2, item3);
		set.add(item4, item5);
		return set;
	}

	/**
	 * Creates a new FilteredStringOrderedSet that holds only the given items, but can be resized.
	 * @param filter a CharFilter that can be obtained with {@link CharFilter#getOrCreate(String, CharPredicate, CharToCharFunction)}
	 * @param item0 a String item
	 * @param item1 a String item
	 * @param item2 a String item
	 * @param item3 a String item
	 * @param item4 a String item
	 * @param item5 a String item
	 * @param item6 a String item
	 * @return a new FilteredStringOrderedSet that holds the given items
	 */
	public static FilteredStringOrderedSet with(CharFilter filter, String item0, String item1, String item2, String item3, String item4, String item5, String item6) {
		FilteredStringOrderedSet set = new FilteredStringOrderedSet(filter, 7);
		set.add(item0, item1, item2, item3);
		set.add(item4, item5, item6);
		return set;
	}

	/**
	 * Creates a new FilteredStringOrderedSet that holds only the given items, but can be resized.
	 * @param filter a CharFilter that can be obtained with {@link CharFilter#getOrCreate(String, CharPredicate, CharToCharFunction)}
	 * @param item0 a String item
	 * @param item1 a String item
	 * @param item2 a String item
	 * @param item3 a String item
	 * @param item4 a String item
	 * @param item5 a String item
	 * @param item6 a String item
	 * @return a new FilteredStringOrderedSet that holds the given items
	 */
	public static FilteredStringOrderedSet with(CharFilter filter, String item0, String item1, String item2, String item3, String item4, String item5, String item6, String item7) {
		FilteredStringOrderedSet set = new FilteredStringOrderedSet(filter, 8);
		set.add(item0, item1, item2, item3);
		set.add(item4, item5, item6, item7);
		return set;
	}

	/**
	 * Creates a new FilteredStringOrderedSet that holds only the given items, but can be resized.
	 * This overload will only be used when an array is supplied or if varargs are used and
	 * there are 9 or more arguments.
	 * @param filter a CharFilter that can be obtained with {@link CharFilter#getOrCreate(String, CharPredicate, CharToCharFunction)}
	 * @param varargs a String varargs or String array; remember that varargs allocate
	 * @return a new FilteredStringOrderedSet that holds the given items
	 */
	public static FilteredStringOrderedSet with(CharFilter filter, String... varargs) {
		return new FilteredStringOrderedSet(filter, varargs);
	}
}

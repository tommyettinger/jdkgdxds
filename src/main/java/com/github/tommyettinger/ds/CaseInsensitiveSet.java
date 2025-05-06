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
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import java.util.Collection;
import java.util.Iterator;

/**
 * A custom variant on ObjectSet that always uses CharSequence keys and compares them as case-insensitive.
 * This uses a fairly complex, quite-optimized hashing function because it needs to hash CharSequences rather
 * often, and to do so ignoring case means {@link String#hashCode()} won't work, plus not all CharSequences
 * implement hashCode() themselves (such as {@link StringBuilder}). User code similar to this can often get away
 * with a simple polynomial hash (the typical Java kind, used by String and Arrays), or if more speed is needed,
 * one with <a href="https://richardstartin.github.io/posts/collecting-rocks-and-benchmarks">some of these
 * optimizations by Richard Startin</a>. If you don't want to write or benchmark a hash function (which is quite
 * understandable), {@link Utilities#hashCodeIgnoreCase(CharSequence)} can get a case-insensitive hash of any
 * CharSequence, as a long. It does this without allocating new Strings all over, where many case-insensitive
 * algorithms do allocate quite a lot, but it does this by handling case incorrectly for the Georgian alphabet.
 * If I see Georgian text in-the-wild, I may reconsider, but I don't think that particular alphabet is in
 * widespread use. There's also {@link Utilities#equalsIgnoreCase(CharSequence, CharSequence)} for equality
 * comparisons that are similarly case-insensitive, except for Georgian.
 * <br>
 * This is very similar to {@link FilteredStringSet} when its {@link CharFilter#getEditor() editor}
 * is {@link Character#toUpperCase(char)} or {@link Casing#caseUp(char)}.
 * FilteredStringSet works with Strings rather than CharSequences, which
 * may be more convenient, and allows filtering some characters out of hashing and equality comparisons. If you want a
 * case-insensitive set that ignores any non-letter characters in a String, then CaseInsensitiveSet won't do,
 * but {@code new FilteredStringSet<>(Character::isLetter, Character::toUpperCase)} will work. Note that GWT only
 * handles {@link Character#isLetter(char)} for ASCII letters; the library RegExodus offers replacements in Category.
 */
public class CaseInsensitiveSet extends ObjectSet<CharSequence> {

	/**
	 * Used by {@link #place(Object)} to mix hashCode() results. Changes on every call to {@link #resize(int)} by default.
	 * This only needs to be serialized if the full key and value tables are serialized, or if the iteration order should be
	 * the same before and after serialization. Iteration order is better handled by using {@link ObjectObjectOrderedMap}.
	 */
	protected int hashMultiplier = 0xEFAA28F1;

	/**
	 * Creates a new set with an initial capacity of {@link Utilities#getDefaultTableCapacity()} and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 */
	public CaseInsensitiveSet () {
		super();
	}

	/**
	 * Creates a new set with the specified initial capacity a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 * This set will hold initialCapacity items before growing the backing table.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 */
	public CaseInsensitiveSet (int initialCapacity) {
		super(initialCapacity);
	}

	/**
	 * Creates a new set with the specified initial capacity and load factor. This set will hold initialCapacity items before
	 * growing the backing table.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 * @param loadFactor      what fraction of the capacity can be filled before this has to resize; 0 &lt; loadFactor &lt;= 1
	 */
	public CaseInsensitiveSet (int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
	}

	/**
	 * Creates a new instance containing the items in the specified iterator.
	 *
	 * @param coll an iterator that will have its remaining contents added to this
	 */
	public CaseInsensitiveSet (Iterator<? extends CharSequence> coll) {
		this();
		hashMultiplier = 0xEFAA28F1;
		addAll(coll);
	}

	/**
	 * Creates a new set identical to the specified set.
	 *
	 * @param set an ObjectSet or one of its subclasses
	 */
	public CaseInsensitiveSet (ObjectSet<? extends CharSequence> set) {
		this(set.size());
		hashMultiplier = 0xEFAA28F1;
		addAll(set);
	}

	/**
	 * Creates a new set that contains all distinct elements in {@code coll}.
	 *
	 * @param coll a Collection implementation, such as an ObjectList
	 */
	public CaseInsensitiveSet (Collection<? extends CharSequence> coll) {
		super(coll.size());
		hashMultiplier = 0xEFAA28F1;
		addAll(coll);

	}

	/**
	 * Creates a new set using {@code length} items from the given {@code array}, starting at {@code} offset (inclusive).
	 *
	 * @param array  an array to draw items from
	 * @param offset the first index in array to draw an item from
	 * @param length how many items to take from array; bounds-checking is the responsibility of the using code
	 */
	public CaseInsensitiveSet (CharSequence[] array, int offset, int length) {
		this(length);
		hashMultiplier = 0xEFAA28F1;
		addAll(array, offset, length);
	}

	/**
	 * Creates a new set containing all the items in the given array.
	 *
	 * @param array an array that will be used in full, except for duplicate items
	 */
	public CaseInsensitiveSet (CharSequence[] array) {
		this(array.length);
		hashMultiplier = 0xEFAA28F1;
		addAll(array);
	}

	/**
	 * Creates a new set that contains all distinct elements in {@code set}.
	 *
	 * @param set another CaseInsensitiveSet
	 */
	public CaseInsensitiveSet (CaseInsensitiveSet set) {
		super(set.size);
		this.hashMultiplier = set.hashMultiplier;
		addAll(set);
	}

	@Override
	protected int place (@NonNull Object item) {
		if (item instanceof CharSequence)
			return Utilities.hashCodeIgnoreCase((CharSequence)item, hashMultiplier) & mask;
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

	@Override
	protected boolean equate (Object left, @Nullable Object right) {
		if ((left instanceof CharSequence) && (right instanceof CharSequence)) {
			return Utilities.equalsIgnoreCase((CharSequence)left, (CharSequence)right);
		}
		return false;
	}

	@Override
	public int hashCode () {
		int h = size;
		CharSequence[] keyTable = this.keyTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			CharSequence key = keyTable[i];
			if (key != null) {h ^= Utilities.hashCodeIgnoreCase(key);}
		}
		return h;
	}

	protected void resize (int newSize) {
		hashMultiplier = Utilities.GOOD_MULTIPLIERS[BitConversion.imul(hashMultiplier, BitConversion.countLeadingZeros(newSize - 1) + 32) >>> 5 & 511];
		super.resize(newSize);
	}

	/**
	 * Constructs an empty set.
	 * This is usually less useful than just using the constructor, but can be handy
	 * in some code-generation scenarios when you don't know how many arguments you will have.
	 *
	 * @return a new set containing nothing
	 */
	public static CaseInsensitiveSet with () {
		return new CaseInsensitiveSet(0);
	}

	/**
	 * Creates a new CaseInsensitiveSet that holds only the given item, but can be resized.
	 * @param item one CharSequence item
	 * @return a new CaseInsensitiveSet that holds the given item
	 */
	public static CaseInsensitiveSet with (CharSequence item) {
		CaseInsensitiveSet set = new CaseInsensitiveSet(1);
		set.add(item);
		return set;
	}

	/**
	 * Creates a new CaseInsensitiveSet that holds only the given items, but can be resized.
	 * @param item0 a CharSequence item
	 * @param item1 a CharSequence item
	 * @return a new CaseInsensitiveSet that holds the given items
	 */
	public static CaseInsensitiveSet with (CharSequence item0, CharSequence item1) {
		CaseInsensitiveSet set = new CaseInsensitiveSet(2);
		set.add(item0, item1);
		return set;
	}

	/**
	 * Creates a new CaseInsensitiveSet that holds only the given items, but can be resized.
	 * @param item0 a CharSequence item
	 * @param item1 a CharSequence item
	 * @param item2 a CharSequence item
	 * @return a new CaseInsensitiveSet that holds the given items
	 */
	public static CaseInsensitiveSet with (CharSequence item0, CharSequence item1, CharSequence item2) {
		CaseInsensitiveSet set = new CaseInsensitiveSet(3);
		set.add(item0, item1, item2);
		return set;
	}

	/**
	 * Creates a new CaseInsensitiveSet that holds only the given items, but can be resized.
	 * @param item0 a CharSequence item
	 * @param item1 a CharSequence item
	 * @param item2 a CharSequence item
	 * @param item3 a CharSequence item
	 * @return a new CaseInsensitiveSet that holds the given items
	 */
	public static CaseInsensitiveSet with (CharSequence item0, CharSequence item1, CharSequence item2, CharSequence item3) {
		CaseInsensitiveSet set = new CaseInsensitiveSet(4);
		set.add(item0, item1, item2, item3);
		return set;
	}

	/**
	 * Creates a new CaseInsensitiveSet that holds only the given items, but can be resized.
	 * @param item0 a CharSequence item
	 * @param item1 a CharSequence item
	 * @param item2 a CharSequence item
	 * @param item3 a CharSequence item
	 * @param item4 a CharSequence item
	 * @return a new CaseInsensitiveSet that holds the given items
	 */
	public static CaseInsensitiveSet with (CharSequence item0, CharSequence item1, CharSequence item2, CharSequence item3, CharSequence item4) {
		CaseInsensitiveSet set = new CaseInsensitiveSet(5);
		set.add(item0, item1, item2, item3);
		set.add(item4);
		return set;
	}

	/**
	 * Creates a new CaseInsensitiveSet that holds only the given items, but can be resized.
	 * @param item0 a CharSequence item
	 * @param item1 a CharSequence item
	 * @param item2 a CharSequence item
	 * @param item3 a CharSequence item
	 * @param item4 a CharSequence item
	 * @param item5 a CharSequence item
	 * @return a new CaseInsensitiveSet that holds the given items
	 */
	public static CaseInsensitiveSet with (CharSequence item0, CharSequence item1, CharSequence item2, CharSequence item3, CharSequence item4, CharSequence item5) {
		CaseInsensitiveSet set = new CaseInsensitiveSet(6);
		set.add(item0, item1, item2, item3);
		set.add(item4, item5);
		return set;
	}

	/**
	 * Creates a new CaseInsensitiveSet that holds only the given items, but can be resized.
	 * @param item0 a CharSequence item
	 * @param item1 a CharSequence item
	 * @param item2 a CharSequence item
	 * @param item3 a CharSequence item
	 * @param item4 a CharSequence item
	 * @param item5 a CharSequence item
	 * @param item6 a CharSequence item
	 * @return a new CaseInsensitiveSet that holds the given items
	 */
	public static CaseInsensitiveSet with (CharSequence item0, CharSequence item1, CharSequence item2, CharSequence item3, CharSequence item4, CharSequence item5, CharSequence item6) {
		CaseInsensitiveSet set = new CaseInsensitiveSet(7);
		set.add(item0, item1, item2, item3);
		set.add(item4, item5, item6);
		return set;
	}

	/**
	 * Creates a new CaseInsensitiveSet that holds only the given items, but can be resized.
	 * @param item0 a CharSequence item
	 * @param item1 a CharSequence item
	 * @param item2 a CharSequence item
	 * @param item3 a CharSequence item
	 * @param item4 a CharSequence item
	 * @param item5 a CharSequence item
	 * @param item6 a CharSequence item
	 * @return a new CaseInsensitiveSet that holds the given items
	 */
	public static CaseInsensitiveSet with (CharSequence item0, CharSequence item1, CharSequence item2, CharSequence item3, CharSequence item4, CharSequence item5, CharSequence item6, CharSequence item7) {
		CaseInsensitiveSet set = new CaseInsensitiveSet(8);
		set.add(item0, item1, item2, item3);
		set.add(item4, item5, item6, item7);
		return set;
	}

	/**
	 * Creates a new CaseInsensitiveSet that holds only the given items, but can be resized.
	 * This overload will only be used when varargs are used and
	 * there are 9 or more arguments.
	 * @param varargs a CharSequence varargs or CharSequence array; remember that varargs allocate
	 * @return a new CaseInsensitiveSet that holds the given items
	 */
	public static CaseInsensitiveSet with (CharSequence... varargs) {
		return new CaseInsensitiveSet(varargs);
	}
}

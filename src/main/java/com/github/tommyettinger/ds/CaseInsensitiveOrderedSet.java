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

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import java.util.Collection;
import java.util.Comparator;

/**
 * A custom variant on ObjectOrderedSet that always uses CharSequence keys and compares them as case-insensitive.
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
 * This is very similar to {@link CaseInsensitiveSet},
 * except that this class maintains insertion order and can be sorted with {@link #sort()}, {@link #sort(Comparator)}, etc.
 * Note that because each CharSequence is stored in here in its
 * original form (not modified to make it ignore case), the sorted order might be different than you expect.
 * {@link Utilities#compareIgnoreCase(CharSequence, CharSequence)} can be used to sort this as case-insensitive.
 * <br>
 * This is also very similar to {@link FilteredStringOrderedSet} when its {@link CharFilter#getEditor() editor}
 * is {@link Character#toUpperCase(char)}. FilteredStringOrderedSet works with Strings rather than CharSequences, which
 * may be more convenient, and allows filtering some characters out of hashing and equality comparisons. If you want a
 * case-insensitive set that ignores any non-letter characters in a String, then CaseInsensitiveOrderedSet won't do,
 * but {@code new FilteredStringOrderedSet<>(Character::isLetter, Character::toUpperCase)} will work.
 */
public class CaseInsensitiveOrderedSet extends ObjectOrderedSet<CharSequence> {

	/**
	 * Creates a new set with an initial capacity of 51 and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 */
	public CaseInsensitiveOrderedSet () {
		super();
	}

	/**
	 * Creates a new set with the specified initial capacity a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 * This set will hold initialCapacity items before growing the backing table.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 */
	public CaseInsensitiveOrderedSet (int initialCapacity) {
		super(initialCapacity);
	}

	/**
	 * Creates a new set with the specified initial capacity and load factor. This set will hold initialCapacity items before
	 * growing the backing table.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 * @param loadFactor      what fraction of the capacity can be filled before this has to resize; 0 &lt; loadFactor &lt;= 1
	 */
	public CaseInsensitiveOrderedSet (int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
	}

	/**
	 * Creates a new set identical to the specified set.
	 *
	 * @param set an ObjectSet or one of its subclasses; ObjectOrderedSet uses a different constructor
	 */
	public CaseInsensitiveOrderedSet (ObjectSet<? extends CharSequence> set) {
		super(set);
	}

	/**
	 * Creates a new ordered set identical to the specified ordered set.
	 * Typically, this would take another CaseInsensitiveOrderedSet, but you can use an ObjectOrderedSet
	 * or one of its other subclasses as well.
	 *
	 * @param set an ObjectOrderedSet or one of its subclasses, such as a CaseInsensitiveOrderedSet
	 */
	public CaseInsensitiveOrderedSet (ObjectOrderedSet<? extends CharSequence> set) {
		super(set);
	}

	/**
	 * Creates a new set that contains all distinct elements in {@code coll}.
	 *
	 * @param coll a Collection implementation, such as an ObjectList
	 */
	public CaseInsensitiveOrderedSet (Collection<? extends CharSequence> coll) {
		super(coll);
	}

	/**
	 * Creates a new set using {@code length} items from the given {@code array}, starting at {@code} offset (inclusive).
	 *
	 * @param array  an array to draw items from
	 * @param offset the first index in array to draw an item from
	 * @param length how many items to take from array; bounds-checking is the responsibility of the using code
	 */
	public CaseInsensitiveOrderedSet (CharSequence[] array, int offset, int length) {
		super(array, offset, length);
	}

	/**
	 * Creates a new set containing all the items in the given array.
	 *
	 * @param array an array that will be used in full, except for duplicate items
	 */
	public CaseInsensitiveOrderedSet (CharSequence[] array) {
		super(array);
	}

	/**
	 * Creates a new set by copying {@code count} items from the given Ordered, starting at {@code offset} in that Ordered,
	 * into this.
	 *
	 * @param other  another Ordered of CharSequence
	 * @param offset the first index in other's ordering to draw an item from
	 * @param count  how many items to copy from other
	 */
	public CaseInsensitiveOrderedSet (Ordered<CharSequence> other, int offset, int count) {
		this(count);
		addAll(0, other, offset, count);
	}

	@Override
	protected int place (@NonNull Object item) {
		if (item instanceof CharSequence)
			return Utilities.hashCodeIgnoreCase((CharSequence)item, hashMultiplier) & mask;
		return super.place(item);
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

	public static CaseInsensitiveOrderedSet with (CharSequence item) {
		CaseInsensitiveOrderedSet set = new CaseInsensitiveOrderedSet(1);
		set.add(item);
		return set;
	}

	public static CaseInsensitiveOrderedSet with (CharSequence... array) {
		return new CaseInsensitiveOrderedSet(array);
	}

}

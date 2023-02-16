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

import org.checkerframework.checker.nullness.qual.Nullable;
import java.util.Collection;
import java.util.Set;

/**
 * A custom variant on ObjectSet that always uses CharSequence keys and compares them as case-insensitive.
 * This uses a fairly complex, quite-optimized hashing function because it needs to hash CharSequences rather
 * often, and to do so ignoring case means {@link String#hashCode()} won't work, plus not all CharSequences
 * implement hashCode() themselves (such as {@link StringBuilder}). User code similar to this can often get away
 * with a simple polynomial hash (the typical Java kind, used by String and Arrays), or if more speed is needed,
 * one with <a href="https://richardstartin.github.io/posts/collecting-rocks-and-benchmarks">some of these
 * optimizations by Richard Startin</a>. If you don't want to write or benchmark a hash function (which is quite
 * understandable), {@link Utilities#longHashCodeIgnoreCase(CharSequence)} can get a case-insensitive hash of any
 * CharSequence, as a long. It does this without allocating new Strings all over, where many case-insensitive
 * algorithms do allocate quite a lot, but it does this by handling case incorrectly for the Georgian alphabet.
 * If I see Georgian text in-the-wild, I may reconsider, but I don't think that particular alphabet is in
 * widespread use. There's also {@link Utilities#equalsIgnoreCase(CharSequence, CharSequence)} for equality
 * comparisons that are similarly case-insensitive, except for Georgian.
 */
public class CaseInsensitiveSet extends ObjectSet<CharSequence> {
	/**
	 * Creates a new set with an initial capacity of 51 and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 */
	public CaseInsensitiveSet () {
		super();
	}

	/**
	 * Creates a new set with a load factor of {@link Utilities#getDefaultLoadFactor()}.
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
	 * Creates a new set identical to the specified set.
	 *
	 * @param set an ObjectSet or subclass to copy, such as another CaseInsensitiveSet
	 */
	public CaseInsensitiveSet (ObjectSet<? extends CharSequence> set) {
		super(set);
	}

	/**
	 * Creates a new set that contains all distinct elements in {@code coll}.
	 *
	 * @param coll a Collection implementation to copy, such as an ObjectList or a Set that doesn't subclass ObjectSet
	 */
	public CaseInsensitiveSet (Collection<? extends CharSequence> coll) {
		super(coll);
	}

	/**
	 * Creates a new set using {@code length} items from the given {@code array}, starting at {@code} offset (inclusive).
	 * This takes a CharSequence array, not a String array, though Strings can be put into a CharSequence array (along with
	 * StringBuilders and similar CharSequences).
	 *
	 * @param array  an array to draw items from
	 * @param offset the first index in array to draw an item from
	 * @param length how many items to take from array; bounds-checking is the responsibility of the using code
	 */
	public CaseInsensitiveSet (CharSequence[] array, int offset, int length) {
		super(array, offset, length);
	}

	/**
	 * Creates a new set containing all of the items in the given array.
	 * This takes a CharSequence array, not a String array, though Strings can be put into a CharSequence array (along with
	 * StringBuilders and similar CharSequences).
	 *
	 * @param array an array that will be used in full, except for duplicate items
	 */
	public CaseInsensitiveSet (CharSequence[] array) {
		super(array);
	}

	@Override
	protected int place (Object item) {
		if (item instanceof CharSequence)
			return (int)Utilities.longHashCodeIgnoreCase((CharSequence)item, hashMultiplier) & mask;
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
			if (key != null) {h += Utilities.longHashCodeIgnoreCase(key);}
		}
		return h;
	}

	@Override
	public boolean equals (Object o) {
		if (o == this)
			return true;
		if (!(o instanceof Set))
			return false;
		Set<?> s = (Set<?>)o;
		if (s.size() != size())
			return false;
		try {
			return containsAll(s);
		} catch (ClassCastException | NullPointerException unused) {
			return false;
		}
	}

	public static CaseInsensitiveSet with (CharSequence item) {
		CaseInsensitiveSet set = new CaseInsensitiveSet(1);
		set.add(item);
		return set;
	}

	public static CaseInsensitiveSet with (CharSequence... array) {
		return new CaseInsensitiveSet(array);
	}

}

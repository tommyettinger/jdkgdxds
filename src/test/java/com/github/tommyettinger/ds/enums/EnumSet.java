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

package com.github.tommyettinger.ds.enums;

import com.github.tommyettinger.ds.ObjectSet;
import com.github.tommyettinger.ds.Utilities;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;

/**
 * A custom variant on ObjectSet that always uses enum items, which simplifies some operations.
 */
public class EnumSet<E extends Enum<E>> extends ObjectSet<E> {
	/**
	 * Creates a new set with an initial capacity of {@link Utilities#getDefaultTableCapacity()} and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 */
	public EnumSet() {
		super();
	}

	/**
	 * Creates a new set with the specified initial capacity a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 * This set will hold initialCapacity items before growing the backing table.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 */
	public EnumSet(int initialCapacity) {
		super(initialCapacity);
	}

	/**
	 * Creates a new set with the specified initial capacity and load factor. This set will hold initialCapacity items before
	 * growing the backing table.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 * @param loadFactor      what fraction of the capacity can be filled before this has to resize; 0 &lt; loadFactor &lt;= 1
	 */
	public EnumSet(int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
	}

	/**
	 * Creates a new set identical to the specified set.
	 *
	 * @param set an ObjectSet or subclass to copy, such as another EnumSet
	 */
	public EnumSet(ObjectSet<? extends E> set) {
		super(set);
	}

	/**
	 * Creates a new set that contains all distinct elements in {@code coll}.
	 *
	 * @param coll a Collection implementation to copy, such as an ObjectList or a Set that doesn't subclass ObjectSet
	 */
	public EnumSet(Collection<? extends E> coll) {
		super(coll);
	}

	/**
	 * Creates a new set using {@code length} items from the given {@code array}, starting at {@code} offset (inclusive).
	 * This takes an enum array.
	 *
	 * @param array  an array to draw items from
	 * @param offset the first index in array to draw an item from
	 * @param length how many items to take from array; bounds-checking is the responsibility of the using code
	 */
	public EnumSet(E[] array, int offset, int length) {
		super(array, offset, length);
	}

	/**
	 * Creates a new set containing all the items in the given array.
	 * This takes am enum array.
	 *
	 * @param array an array that will be used in full, except for duplicate items
	 */
	public EnumSet(E[] array) {
		super(array);
	}

	@Override
	protected int place(@NonNull Object item) {
		// As long as the capacity is sufficient, ordinals will never collide.
		if (item instanceof Enum)
			return ((Enum<?>) item).ordinal() & mask;
		return super.place(item);
	}

	@Override
	protected boolean equate(Object left, @Nullable Object right) {
		// Enums can use reference equality.
		return left == right;
	}

	@Override
	public int hashCode() {
		int h = size;
		Enum[] keyTable = this.keyTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			Enum key = keyTable[i];
			if (key != null) {
				h += key.ordinal() * 421;
			}
		}
		return h;
	}

	public static <E extends Enum<E>> EnumSet<E> with(E item) {
		EnumSet<E> set = new EnumSet<>(1);
		set.add(item);
		return set;
	}

	@SafeVarargs
	public static <E extends Enum<E>> EnumSet<E> with(E... array) {
		return new EnumSet<>(array);
	}

}

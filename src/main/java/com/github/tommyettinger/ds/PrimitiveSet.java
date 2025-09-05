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

import com.github.tommyettinger.ds.support.util.CharIterator;
import com.github.tommyettinger.ds.support.util.IntIterator;
import com.github.tommyettinger.ds.support.util.LongIterator;

/**
 * Analogous to {@link java.util.Set} but for a primitive type, this extends {@link PrimitiveCollection} and the
 * nested classes here extend the nested classes in {@link PrimitiveCollection}. This is not necessarily a modifiable
 * collection. The nested interfaces define all the actually useful operations, and you will probably
 * never use PrimitiveSet directly.
 */
public interface PrimitiveSet<T> extends PrimitiveCollection<T> {
	@Override
	int hashCode();

	@Override
	boolean equals(Object other);

	interface SetOfInt extends PrimitiveCollection.OfInt, PrimitiveSet<Integer> {
		/**
		 * Compares this PrimitiveSet.SetOfInt with another PrimitiveSet.SetOfInt by checking their identity,
		 * their types (both must implement PrimitiveSet.SetOfInt), and their sizes, before checking if other
		 * contains each item in this PrimitiveSet.SetOfInt, in any order or quantity. This is most useful for
		 * the key "set" or value collection in a primitive-backed map, since quantity doesn't matter for keys and
		 * order doesn't matter for either. Many implementations may need to reset the iterator on this
		 * PrimitiveSet.SetOfInt, but that isn't necessary for {@code other}.
		 *
		 * @param other another Object that should be a PrimitiveSet.SetOfInt
		 * @return true if other is another PrimitiveSet.SetOfInt with exactly the same items, false otherwise
		 */
		default boolean equalContents(Object other) {
			if (this == other) return true;
			if (!(other instanceof PrimitiveSet.SetOfInt)) return false;
			OfInt o = (OfInt) other;
			if (size() != o.size()) return false;
			IntIterator it = iterator();
			while (it.hasNext()) {
				if (!o.contains(it.nextInt())) return false;
			}
			return true;
		}
	}

	interface SetOfLong extends PrimitiveCollection.OfLong, PrimitiveSet<Long> {
		/**
		 * Compares this PrimitiveSet.SetOfLong with another PrimitiveSet.SetOfLong by checking their identity,
		 * their types (both must implement PrimitiveSet.SetOfLong), and their sizes, before checking if other
		 * contains each item in this PrimitiveSet.SetOfLong, in any order or quantity. This is most useful for
		 * the key "set" or value collection in a primitive-backed map, since quantity doesn't matter for keys and
		 * order doesn't matter for either. Many implementations may need to reset the iterator on this
		 * PrimitiveSet.SetOfLong, but that isn't necessary for {@code other}.
		 *
		 * @param other another Object that should be a PrimitiveSet.SetOfLong
		 * @return true if other is another PrimitiveSet.SetOfLong with exactly the same items, false otherwise
		 */
		default boolean equalContents(Object other) {
			if (this == other) return true;
			if (!(other instanceof PrimitiveSet.SetOfLong)) return false;
			OfLong o = (OfLong) other;
			if (size() != o.size()) return false;
			LongIterator it = iterator();
			while (it.hasNext()) {
				if (!o.contains(it.nextLong())) return false;
			}
			return true;
		}
	}

	interface SetOfChar extends PrimitiveCollection.OfChar, PrimitiveSet<Character> {
		/**
		 * Compares this PrimitiveSet.SetOfChar with another PrimitiveSet.SetOfChar by checking their identity,
		 * their types (both must implement PrimitiveSet.SetOfChar), and their sizes, before checking if other
		 * contains each item in this PrimitiveSet.SetOfChar, in any order or quantity. This is most useful for
		 * the key "set" or value collection in a primitive-backed map, since quantity doesn't matter for keys and
		 * order doesn't matter for either. Many implementations may need to reset the iterator on this
		 * PrimitiveSet.SetOfChar, but that isn't necessary for {@code other}.
		 *
		 * @param other another Object that should be a PrimitiveSet.SetOfChar
		 * @return true if other is another PrimitiveSet.SetOfChar with exactly the same items, false otherwise
		 */
		default boolean equalContents(Object other) {
			if (this == other) return true;
			if (!(other instanceof PrimitiveSet.SetOfChar)) return false;
			OfChar o = (OfChar) other;
			if (size() != o.size()) return false;
			CharIterator it = iterator();
			while (it.hasNext()) {
				if (!o.contains(it.nextChar())) return false;
			}
			return true;
		}
	}
}

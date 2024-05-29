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

import com.github.tommyettinger.ds.support.util.BooleanIterator;
import com.github.tommyettinger.ds.support.util.ByteIterator;
import com.github.tommyettinger.ds.support.util.CharIterator;
import com.github.tommyettinger.ds.support.util.DoubleIterator;
import com.github.tommyettinger.ds.support.util.FloatIterator;
import com.github.tommyettinger.ds.support.util.IntIterator;
import com.github.tommyettinger.ds.support.util.LongIterator;
import com.github.tommyettinger.ds.support.util.ShortIterator;
import com.github.tommyettinger.function.BooleanConsumer;
import com.github.tommyettinger.function.BooleanPredicate;
import com.github.tommyettinger.function.ByteConsumer;
import com.github.tommyettinger.function.BytePredicate;
import com.github.tommyettinger.function.CharConsumer;
import com.github.tommyettinger.function.CharPredicate;
import com.github.tommyettinger.function.DoubleConsumer;
import com.github.tommyettinger.function.DoublePredicate;
import com.github.tommyettinger.function.FloatConsumer;
import com.github.tommyettinger.function.FloatPredicate;
import com.github.tommyettinger.function.IntConsumer;
import com.github.tommyettinger.function.IntPredicate;
import com.github.tommyettinger.function.LongConsumer;
import com.github.tommyettinger.function.LongPredicate;
import com.github.tommyettinger.function.ShortConsumer;
import com.github.tommyettinger.function.ShortPredicate;

import java.util.Iterator;

/**
 * Analogous to {@link java.util.Set} but for a primitive type, this extends {@link PrimitiveCollection} and the
 * nested classes here extend the nested classes in {@link PrimitiveCollection}. This is not necessarily a modifiable
 * collection. The nested interfaces define all the actually useful operations, and you will probably
 * never use PrimitiveSet directly.
 */
public interface PrimitiveSet<T> extends PrimitiveCollection<T> {
	@Override
	int hashCode ();

	@Override
	boolean equals (Object other);

	interface SetOfInt extends PrimitiveCollection.OfInt, PrimitiveSet<Integer> {
		/**
		 * Compares this PrimitiveSet.SetOfInt with another PrimitiveSet.SetOfInt by checking their identity,
		 * their types (both must implement PrimitiveSet.SetOfInt), and their sizes, before checking if other
		 * contains each item in this PrimitiveSet.SetOfInt, in any order or quantity. This is most useful for
		 * the key "set" or value collection in a primitive-backed map, since quantity doesn't matter for keys and
		 * order doesn't matter for either. Many implementations may need to reset the iterator on this
		 * PrimitiveSet.SetOfInt, but that isn't necessary for {@code other}.
		 * @param other another Object that should be a PrimitiveSet.SetOfInt
		 * @return true if other is another PrimitiveSet.SetOfInt with exactly the same items, false otherwise
		 */
		default boolean equalContents (Object other) {
			if(this == other) return true;
			if(!(other instanceof PrimitiveSet.SetOfInt)) return false;
			OfInt o = (OfInt) other;
			if(size() != o.size()) return false;
			IntIterator it = iterator();
			while (it.hasNext()) {
				if(!o.contains(it.nextInt())) return false;
			}
			return true;
		}
	}
}

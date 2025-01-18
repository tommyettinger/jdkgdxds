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

package com.github.tommyettinger.ds.support.sort;

import java.util.Comparator;

/**
 * A type-specific {@link Comparator}; provides methods to compare two primitive
 * types both as objects and as primitive types.
 *
 * @see Comparator
 * @see CharComparators
 */
public interface CharComparator extends Comparator<Character> {
	/**
	 * Compares its two primitive-type arguments for order. Returns a negative
	 * integer, zero, or a positive integer as the first argument is less than,
	 * equal to, or greater than the second.
	 *
	 * @return a negative integer, zero, or a positive integer as the first argument
	 * is less than, equal to, or greater than the second.
	 * @see Comparator
	 */
	int compare (char k1, char k2);

	@Override
	default CharComparator reversed () {
		return CharComparators.oppositeComparator(this);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This implementation delegates to the corresponding type-specific method.
	 *
	 * @deprecated Please use the corresponding type-specific method instead.
	 */
	@Deprecated
	@Override
	default int compare (Character ok1, Character ok2) {
		return compare(ok1.charValue(), ok2.charValue());
	}

	/**
	 * Return a new comparator that first uses this comparator, then uses the second
	 * comparator if this comparator compared the two elements as equal.
	 *
	 * @see Comparator#thenComparing(Comparator)
	 */
	default CharComparator thenComparing (CharComparator second) {
		return (k1, k2) -> {
			int comp = compare(k1, k2);
			return comp == 0 ? second.compare(k1, k2) : comp;
		};
	}

	@Override
	default Comparator<Character> thenComparing (Comparator<? super Character> second) {
		if (second instanceof CharComparator) {return thenComparing((CharComparator)second);}
		return Comparator.super.thenComparing(second);
	}
}

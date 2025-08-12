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
 * @see DoubleComparators
 */
public interface DoubleComparator extends Comparator<Double> {
	/**
	 * Compares its two primitive-type arguments for order. Returns a negative
	 * integer, zero, or a positive integer as the first argument is less than,
	 * equal to, or greater than the second.
	 *
	 * @return a negative integer, zero, or a positive integer as the first argument
	 * is less than, equal to, or greater than the second.
	 * @see Comparator
	 */
	int compare(double k1, double k2);

	@Override
	default DoubleComparator reversed() {
		return DoubleComparators.oppositeComparator(this);
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
	default int compare(Double ok1, Double ok2) {
		return compare(ok1.doubleValue(), ok2.doubleValue());
	}

	/**
	 * Return a new comparator that first uses this comparator, then uses the second
	 * comparator if this comparator compared the two elements as equal.
	 *
	 * @see Comparator#thenComparing(Comparator)
	 */
	default DoubleComparator thenComparing(DoubleComparator second) {
		return (k1, k2) -> {
			int comp = compare(k1, k2);
			return comp == 0 ? second.compare(k1, k2) : comp;
		};
	}

	@Override
	default Comparator<Double> thenComparing(Comparator<? super Double> second) {
		if (second instanceof DoubleComparator) {
			return thenComparing((DoubleComparator) second);
		}
		return Comparator.super.thenComparing(second);
	}
}

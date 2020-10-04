/*******************************************************************************
 * Copyright 2020 See AUTHORS file.
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
 ******************************************************************************/

package com.github.tommyettinger.ds;

/**
 * Utility code shared by various data structures in this package.
 */
public class Utilities {
	/**
	 * Used to establish the size of a hash table for {@link ObjectSet}, {@link ObjectMap}, and related code.
	 * The table size will always be a power of two, and should be the next power of two that is at least equal
	 * to {@code capacity / loadFactor}.
	 * @param capacity the amount of items the hash table should be able to hold
	 * @param loadFactor between 0.0 (exclusive) and 1.0 (inclusive); the fraction of how much of the table can be filled
	 * @return the size of a hash table that can handle the specified capacity with the given loadFactor
	 */
	public static int tableSize (int capacity, float loadFactor) {
		if (capacity < 0)
			throw new IllegalArgumentException("capacity must be >= 0: " + capacity);
		int tableSize = 1 << -Integer.numberOfLeadingZeros(Math.max(2, (int)Math.ceil(capacity / loadFactor)) - 1);
		if (tableSize > 1 << 30 || tableSize < 0)
			throw new IllegalArgumentException("The required capacity is too large: " + capacity);
		return tableSize;
	}

	static final Object neverIdentical = new Object();

}

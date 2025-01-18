/*
 * Copyright (c) 2025 See AUTHORS file.
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

package com.github.tommyettinger.ds.test;

import com.github.tommyettinger.ds.Utilities;

import java.util.Collection;
import java.util.Iterator;

public class ObjectSet<T> extends com.github.tommyettinger.ds.ObjectSet<T> {
    public int hashMultiplier = 0xEFAA28F1;

    /**
     * Creates a new set with an initial capacity of 51 and a load factor of {@link Utilities#getDefaultLoadFactor()}.
     */
    public ObjectSet() {
    }

    /**
     * Creates a new set with a load factor of {@link Utilities#getDefaultLoadFactor()}.
     *
     * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
     */
    public ObjectSet(int initialCapacity) {
        super(initialCapacity);
    }

    /**
     * Creates a new set with the specified initial capacity and load factor. This set will hold initialCapacity items before
     * growing the backing table.
     *
     * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
     * @param loadFactor      what fraction of the capacity can be filled before this has to resize; 0 &lt; loadFactor &lt;= 1
     */
    public ObjectSet(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    /**
     * Creates a new instance containing the items in the specified iterator.
     *
     * @param coll an iterator that will have its remaining contents added to this
     */
    public ObjectSet(Iterator<? extends T> coll) {
        super(coll);
    }

    /**
     * Creates a new set identical to the specified set.
     *
     * @param set
     */
    public ObjectSet(com.github.tommyettinger.ds.ObjectSet<? extends T> set) {
        super(set);
    }

    /**
     * Creates a new set that contains all distinct elements in {@code coll}.
     *
     * @param coll
     */
    public ObjectSet(Collection<? extends T> coll) {
        super(coll);
    }

    /**
     * Creates a new set using {@code length} items from the given {@code array}, starting at {@code} offset (inclusive).
     *
     * @param array  an array to draw items from
     * @param offset the first index in array to draw an item from
     * @param length how many items to take from array; bounds-checking is the responsibility of the using code
     */
    public ObjectSet(T[] array, int offset, int length) {
        super(array, offset, length);
    }

    /**
     * Creates a new set containing all of the items in the given array.
     *
     * @param array an array that will be used in full, except for duplicate items
     */
    public ObjectSet(T[] array) {
        super(array);
    }

    /**
     * @return any int, though here it should be negative and odd
     */
    @Override
    public int getHashMultiplier() {
        return hashMultiplier;
    }

    /**
     * @param mul any int; will be made negative and odd if it isn't already both
     */
    @Override
    public void setHashMultiplier(int mul) {
        hashMultiplier = mul | 0x80000001;
    }
}

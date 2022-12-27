/*
 * Copyright (c) 2022 See AUTHORS file.
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

/**
 * A variant on {@link ObjectSet} that compares items by identity (using {@code ==}) instead of equality (using {@code equals()}).
 * It also hashes with {@link System#identityHashCode(Object)} instead of calling the {@code hashCode()} of an item. This can be useful in
 * some cases where items may have invalid {@link Object#equals(Object)} and/or {@link Object#hashCode()} implementations, or if items
 * could be very large (making a hashCode() that uses all the parts of the item slow). Oddly, {@link System#identityHashCode(Object)} tends
 * to be slower than the hashCode() for most small items, because an explicitly-written hashCode() typically doesn't need to do anything
 * concurrently, but identityHashCode() needs to (concurrently) modify an internal JVM variable that ensures the results are unique, and
 * that requires the JVM to do lots of extra work whenever identityHashCode() is called. Despite that, identityHashCode() doesn't depend
 * on the quantity of variables in the item, so identityHashCode() gets relatively faster for larger items. The equals() method used by
 * ObjectSet also tends to slow down for large items, relative to the constant-time {@code ==} this uses.
 * <br>
 * This can potentially be useful for tracking references to ensure they are all unique by identity, such as to avoid referential cycles.
 */
public class IdentitySet<T> extends ObjectSet<T> {
	public IdentitySet () {
		super();
	}

	public IdentitySet (int initialCapacity) {
		super(initialCapacity);
	}

	public IdentitySet (int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
	}

	public IdentitySet (ObjectSet<? extends T> set) {
		super(set);
	}

	public IdentitySet (Collection<? extends T> coll) {
		super(coll);
	}

	public IdentitySet (T[] array, int offset, int length) {
		super(array, offset, length);
	}

	public IdentitySet (T[] array) {
		super(array);
	}

	@Override
	protected int place (Object item) {
		return System.identityHashCode(item) & mask;
	}

	@Override
	protected boolean equate (Object left, @Nullable Object right) {
		return left == right;
	}

	@Override
	public int hashCode () {
		int h = size;
		T[] keyTable = this.keyTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			T key = keyTable[i];
			if (key != null) {h += System.identityHashCode(key);}
		}
		return h;
	}


	public static <T> IdentitySet<T> with (T item) {
		IdentitySet<T> set = new IdentitySet<>(1);
		set.add(item);
		return set;
	}

	@SafeVarargs
	public static <T> IdentitySet<T> with (T... array) {
		return new IdentitySet<>(array);
	}

}

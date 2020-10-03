/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Random;

/**
 * A resizable, ordered list of {@code T} items, typically objects (they can also be arrays).
 * This is a thin wrapper around {@link ArrayList} to implement {@link Ordered}.
 * 
 * @author Tommy Ettinger
 */
public class ObjectList<T> extends ArrayList<T> implements Ordered<T> {
	/**
	 * Constructs an empty list with the specified initial capacity.
	 *
	 * @param initialCapacity the initial capacity of the list
	 * @throws IllegalArgumentException if the specified initial capacity
	 *                                  is negative
	 */
	public ObjectList (int initialCapacity) {
		super(initialCapacity);
	}

	/**
	 * Constructs an empty list with an initial capacity of 16.
	 */
	public ObjectList () {
		super(16);
	}

	/**
	 * Constructs a list containing the elements of the specified
	 * collection, in the order they are returned by the collection's
	 * iterator.
	 *
	 * @param c the collection whose elements are to be placed into this list
	 * @throws NullPointerException if the specified collection is null
	 */
	public ObjectList (Collection<? extends T> c) {
		super(c);
	}

	public ObjectList (T[] a) {
		super(a.length);
		Collections.addAll(this, a);
	}

	public ObjectList (T[] a, int offset, int count) {
		super(a.length);
		for (int i = offset, n = Math.min(offset + count, a.length); i < n; i++) {
			add(a[i]);
		}
	}



	/** Uses == for comparison of each item. Returns false if either array is unordered. */
	public boolean equalsIdentity (Object object) {
		if (object == this) return true;
		if (!(object instanceof ObjectList)) return false;
		ObjectList array = (ObjectList)object;
		int n = size();
		if (n != array.size()) return false;
		for (int i = 0; i < n; i++)
			if (get(i) != array.get(i)) return false;
		return true;
	}

	public String toString () {
		int n = size();
		if (n == 0) return "[]";
		StringBuilder buffer = new StringBuilder(32);
		buffer.append('[');
		buffer.append(get(0));
		for (int i = 1; i < n; i++) {
			buffer.append(", ");
			buffer.append(get(i));
		}
		buffer.append(']');
		return buffer.toString();
	}

	public String toString (String separator) {
		int n = size();
		if (n == 0) return "";
		StringBuilder builder = new StringBuilder(32);
		builder.append(get(0));
		for (int i = 1; i < n; i++) {
			builder.append(separator);
			builder.append(get(i));
		}
		return builder.toString();
	}
	
	public StringBuilder builderAppend (StringBuilder builder, String separator) {
		int n = size();
		if (n == 0) return builder;
		builder.append(get(0));
		for (int i = 1; i < n; i++) {
			builder.append(separator);
			builder.append(get(i));
		}
		return builder;
	}
	
	@SafeVarargs
	static public <T> ObjectList<T> with (T... array) {
		return new ObjectList<>(array);
	}
	/**
	 * Gets the ArrayList of T items that this data structure holds, in the order it uses for iteration.
	 * This method actually returns this ObjectList directly, since it extends ArrayList. 
	 *
	 * @return this ObjectList
	 */
	@Override
	public ObjectList<T> order () {
		return this;
	}

	/**
	 * Switches the ordering of positions {@code a} and {@code b}, without changing any items beyond that.
	 *
	 * @param a the first position
	 * @param b the second position
	 */
	@Override
	public void swap (int a, int b) {
		set(a, set(b, get(a)));

	}

	/**
	 * Pseudo-randomly shuffles the order of this Ordered in-place.
	 *
	 * @param random any {@link Random} implementation; prefer {@link LaserRandom} in this library
	 */
	@Override
	public void shuffle (Random random) {
		for (int i = size() - 1; i >= 0; i--) {
			set(i, set(random.nextInt(i+1), get(i)));
		}
	}
}

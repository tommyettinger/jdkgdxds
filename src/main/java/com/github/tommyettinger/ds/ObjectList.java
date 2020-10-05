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

import java.io.Serializable;
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
public class ObjectList<T> extends ArrayList<T> implements Ordered<T>, Serializable {
	private static final long serialVersionUID = 0L;

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

	/**
	 * Adds each item in the array {@code a} to this ObjectList, appending to the end.
	 * @param a a non-null array of {@code T}
	 * @return true, as {@link #addAll(Collection)} does
	 */
	public boolean addAll(T[] a) {
		return Collections.addAll(this, a);
	}

	/**
	 * Adds each item in the array {@code a} to this ObjectList, inserting starting at {@code insertionIndex}.
	 * @param insertionIndex where to insert into this ObjectList
	 * @param a a non-null array of {@code T}
	 * @return true, as {@link #addAll(Collection)} does
	 */
	public boolean addAll(int insertionIndex, T[] a) {
		return addAll(insertionIndex, a, 0, a.length);
	}

	/**
	 * Adds up to {@code count} items, starting from {@code offset}, in the array {@code a} to this ObjectList, appending to the end.
	 * @param a a non-null array of {@code T}
	 * @param offset the first index in {@code a} to use
	 * @param count how many indices in {@code a} to use
	 * @return true, as {@link #addAll(Collection)} does
	 */
	public boolean addAll(T[] a, int offset, int count) {
		for (int i = offset, n = Math.min(offset + count, a.length); i < n; i++) {
			add(a[i]);
		}
		return true;
	}
	
	public boolean duplicateRange(int index, int count) {
		if (index + count >= size()) 
			throw new IllegalStateException("Sum of index and count is too large: " + (index + count) + " must not be >= " + size());
		addAll(index, subList(index, index + count));
		return count > 0;
	}

	/** Returns true if this ObjectList contains any the specified values.
	 * @param values May contains nulls.
	 * @return true if this ObjectList contains any of the items in {@code values}, false otherwise
	 */
	public boolean containsAny (Collection<? extends T> values) {
		for(T v : values) {
			if (contains(v)) return true;
		}
		return false;
	}
	/**
	 * Adds up to {@code count} items, starting from {@code offset}, in the array {@code a} to this ObjectList, inserting starting at {@code insertionIndex}.
	 * @param insertionIndex where to insert into this ObjectList
	 * @param a a non-null array of {@code T}
	 * @param offset the first index in {@code a} to use
	 * @param count how many indices in {@code a} to use
	 * @return true, as {@link #addAll(Collection)} does
	 */
	public boolean addAll(int insertionIndex, T[] a, int offset, int count) {
		for (int i = offset, n = Math.min(offset + count, a.length); i < n; i++) {
			add(insertionIndex++, a[i]);
		}
		return true;
	}
	
	/** Removes and returns the last item. */
	public T pop() {
		int n = size();
		if (n == 0) throw new IllegalStateException("ObjectList is empty.");
		return remove(n - 1);
	}

	/** Returns the last item. */
	public T peek() {
		int n = size();
		if (n == 0) throw new IllegalStateException("ObjectList is empty.");
		return get(n - 1);
	}

	/** Returns the first item. */
	public T first() {
		if (size() == 0) throw new IllegalStateException("ObjectList is empty.");
		return get(0);
	}

	/** Returns true if the array has one or more items. */
	public boolean notEmpty () {
		return size() != 0;
	}

	/** Uses == for comparison of each item. */
	public boolean equalsIdentity (Object object) {
		if (object == this) return true;
		if (!(object instanceof ObjectList)) return false;
		ObjectList list = (ObjectList)object;
		int n = size();
		if (n != list.size()) return false;
		for (int i = 0; i < n; i++)
			if (get(i) != list.get(i)) return false;
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
	static public <T> ObjectList<T> with (T... varargs) {
		return new ObjectList<>(varargs);
	}
	/**
	 * Gets the ObjectList of T items that this data structure holds, in the order it uses for iteration.
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

	/**
	 * Returns a {@code T} item from anywhere in this ObjectList, chosen pseudo-randomly using {@code random}.
	 * If this ObjectList is empty, throws an {@link IllegalStateException}.
	 * @param random a {@link Random} or a subclass, such as {@link LaserRandom} (recommended)
	 * @return a pseudo-randomly selected item from this ObjectLists
	 */
	public T random(Random random){
		int n = size();
		if (n == 0) throw new IllegalStateException("ObjectList is empty.");
		return get(random.nextInt(n));
	}

	@Override
	public void reverse () {
		Collections.reverse(this);
	}
}

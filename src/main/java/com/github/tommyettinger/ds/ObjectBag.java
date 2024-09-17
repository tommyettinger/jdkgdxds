/*
 * Copyright (c) 2023 See AUTHORS file.
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

import com.github.tommyettinger.ds.support.util.ByteIterator;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.Iterator;

/**
 * An unordered List of T items. This allows efficient iteration via a reused iterator or via index.
 * This class avoids a memory copy when removing elements (the last element is moved to the removed element's position).
 * Items are permitted to change position in the ordering when any item is removed or added.
 * Although this won't keep an order during modifications, you can {@link #sort()} the bag to ensure,
 * if no modifications are made later, that the iteration will happen in sorted order.
 */
public class ObjectBag<T> extends ObjectList<T> {
	/**
	 * Returns true if this implementation retains order, which it does not.
	 *
	 * @return false
	 */
	@Override
	public boolean keepsOrder () {
		return false;
	}

	/**
	 * Constructs an empty bag with an initial capacity of 10.
	 */
	public ObjectBag () {
		super();
	}

	/**
	 * Constructs an empty bag with the specified initial capacity.
	 *
	 * @param initialCapacity the initial capacity of the bag
	 * @throws IllegalArgumentException if the specified initial capacity
	 *                                  is negative
	 */
	public ObjectBag (int initialCapacity) {
		super(initialCapacity);
	}

	/**
	 * Constructs a bag containing the elements of the specified
	 * collection, in the order they are returned by the collection's
	 * iterator.
	 *
	 * @param c the collection whose elements are to be placed into this bag
	 * @throws NullPointerException if the specified collection is null
	 */
	public ObjectBag (Collection<? extends T> c) {
		super(c);
	}

	public ObjectBag (T[] a) {
		super(a);
	}

	public ObjectBag (T[] a, int offset, int count) {
		super(a, offset, count);
	}

	/**
	 * Creates a new instance containing the items in the specified iterator.
	 *
	 * @param coll an iterator that will have its remaining contents added to this
	 */
	public ObjectBag (Iterator<? extends T> coll) {
		this();
		addAll(coll);
	}

	/**
	 * Creates a new bag by copying {@code count} items from the given Ordered, starting at {@code offset} in that Ordered,
	 * into this.
	 *
	 * @param other  another Ordered of the same type
	 * @param offset the first index in other's ordering to draw an item from
	 * @param count  how many items to copy from other
	 */
	public ObjectBag (Ordered<T> other, int offset, int count) {
		super(other, offset, count);
	}

	/**
	 * This always adds {@code element} to the end of this bag's ordering; {@code index} is ignored.
	 *
	 * @param index ignored
	 * @param element element to be inserted
	 */
	@Override
	public void add (int index, @Nullable T element) {
		super.add(element);
	}

	/**
	 * This always adds {@code element} to the end of this bag's ordering; {@code index} is ignored.
	 * This is an alias for {@link #add(int, Object)} to improve compatibility with primitive lists.
	 *
	 * @param index   ignored
	 * @param element element to be inserted
	 */
	@Override
	public void insert (int index, @Nullable T element) {
		super.add(element);
	}

	/**
	 * This removes the item at the given index and returns it, but also changes the ordering.
	 *
	 * @param index the index of the element to be removed, which must be non-negative and less than {@link #size()}
	 * @return the removed item
	 * @throws IndexOutOfBoundsException if the bag is empty
	 */
	@Override
	public @Nullable T remove (int index) {
		int size = size();
		T value = super.set(index, get(size - 1));
		super.remove(size - 1);
		return value;
	}

	/**
	 * This removes the item at the given index and returns it, but also changes the ordering.
	 * This is an alias for {@link #remove(int)} to make the API the same for primitive lists.
	 *
	 * @param index must be non-negative and less than {@link #size()}
	 * @return the previously-held item at the given index
	 * @throws IndexOutOfBoundsException if the bag is empty
	 */
	@Override
	public @Nullable T removeAt (int index) {
		int size = size();
		T value = super.set(index, get(size - 1));
		super.remove(size - 1);
		return value;
	}

	/**
	 * Uses == for comparison of the bags; does not compare their items.
	 */
	public boolean equalsIdentity (Object object) {
		return object == this;
	}

	@Override
	public int hashCode () {
		int h = 1, n = size();
		for (int i = 0; i < n; i++) {
			h += get(i).hashCode();
		}
		return h;
	}

	/**
	 * Creates a new ObjectBag that holds only the given item, but can be resized.
	 * @param item one T item
	 * @return a new ObjectBag that holds the given item
	 * @param <T> the type of item, typically inferred except for array arguments
	 */
	public static <T> ObjectBag<T> with (T item) {
		ObjectBag<T> list = new ObjectBag<>(1);
		list.add(item);
		return list;
	}

	/**
	 * Creates a new ObjectBag that will hold the items in the given array or varargs.
	 * @param varargs either 0 or more T items, or an array of T
	 * @return a new ObjectBag that holds the given T items
	 * @param <T> the type of items, typically inferred by all the items being the same type
	 */
	@SafeVarargs
	public static <T> ObjectBag<T> with (T... varargs) {
		return new ObjectBag<>(varargs);
	}
}

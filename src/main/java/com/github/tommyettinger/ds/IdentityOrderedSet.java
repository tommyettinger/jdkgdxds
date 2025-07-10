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

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.Iterator;

/**
 * A variant on {@link ObjectOrderedSet} that compares items by identity (using {@code ==}) instead of equality (using {@code equals()}).
 * It also hashes with {@link System#identityHashCode(Object)} instead of calling the {@code hashCode()} of an item. This can be useful in
 * some cases where items may have invalid {@link Object#equals(Object)} and/or {@link Object#hashCode()} implementations, or if items
 * could be very large (making a hashCode() that uses all the parts of the item slow). Oddly, {@link System#identityHashCode(Object)} tends
 * to be slower than the hashCode() for most small items, because an explicitly-written hashCode() typically doesn't need to do anything
 * concurrently, but identityHashCode() needs to (concurrently) modify an internal JVM variable that ensures the results are unique, and
 * that requires the JVM to do lots of extra work whenever identityHashCode() is called. Despite that, identityHashCode() doesn't depend
 * on the quantity of variables in the item, so identityHashCode() gets relatively faster for larger items. The equals() method used by
 * ObjectOrderedSet also tends to slow down for large items, relative to the constant-time {@code ==} this uses.
 * <br>
 * This can potentially be useful for tracking references to ensure they are all unique by identity, such as to avoid referential cycles.
 * You might prefer this to {@link IdentitySet} if you need to ensure fast iteration or want access by index.
 */
public class IdentityOrderedSet<T> extends ObjectOrderedSet<T> {
	public IdentityOrderedSet (OrderType type) {
		super(type);
	}

	public IdentityOrderedSet (int initialCapacity, OrderType type) {
		super(initialCapacity, type);
	}

	public IdentityOrderedSet (int initialCapacity, float loadFactor, OrderType type) {
		super(initialCapacity, loadFactor, type);
	}

	/**
	 * Creates a new instance containing the items in the specified iterator.
	 *
	 * @param coll an iterator that will have its remaining contents added to this
	 */
	public IdentityOrderedSet (Iterator<? extends T> coll, OrderType type) {
		super(coll, type);
	}

	public IdentityOrderedSet (ObjectOrderedSet<? extends T> set, OrderType type) {
		super(set, type);
	}

	public IdentityOrderedSet (Collection<? extends T> coll, OrderType type) {
		super(coll, type);
	}

	public IdentityOrderedSet (T[] array, int offset, int length, OrderType type) {
		super(array, offset, length, type);
	}

	public IdentityOrderedSet (T[] array, OrderType type) {
		super(array, type);
	}

	public IdentityOrderedSet (Ordered<T> other, int offset, int count, OrderType type) {
		super(other, offset, count, type);
	}

	// default order type

	public IdentityOrderedSet () {
		super();
	}

	public IdentityOrderedSet (int initialCapacity) {
		super(initialCapacity);
	}

	public IdentityOrderedSet (int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
	}

	/**
	 * Creates a new instance containing the items in the specified iterator.
	 *
	 * @param coll an iterator that will have its remaining contents added to this
	 */
	public IdentityOrderedSet (Iterator<? extends T> coll) {
		super(coll);
	}

	public IdentityOrderedSet (ObjectOrderedSet<? extends T> set) {
		super(set);
	}

	public IdentityOrderedSet (Collection<? extends T> coll) {
		super(coll);
	}

	public IdentityOrderedSet (T[] array, int offset, int length) {
		super(array, offset, length);
	}

	public IdentityOrderedSet (T[] array) {
		super(array);
	}

	public IdentityOrderedSet (Ordered<T> other, int offset, int count) {
		super(other, offset, count);
	}

	@Override
	protected int place (@NonNull Object item) {
		return System.identityHashCode(item) & mask;
	}

	@Override
	protected boolean equate (Object left, @Nullable Object right) {
		return left == right;
	}

	@Override
	public int hashCode () {
		int h = size;
		ObjectList<@Nullable T> order = items;
		for (int i = 0, n = order.size(); i < n; i++) {
			h += System.identityHashCode(order.get(i)); // checking for null items doesn't matter here; their hash is 0.
		}
		return h ^ h >>> 16;
	}

	/**
	 * Effectively does nothing here because the hashMultiplier is not used by identity hashing.
	 *
	 * @return any int; the value isn't used internally, but may be used by subclasses to identify something
	 */
	public int getHashMultiplier() {
		return hashMultiplier;
	}

	/**
	 * Effectively does nothing here because the hashMultiplier is not used by identity hashing.
	 * Subclasses can use this to set some kind of identifier or user data, though.
	 * Unlike the superclass implementation, this does not alter the given int to make it negative or odd.
	 *
	 * @param hashMultiplier any int; will not be used
	 */
	public void setHashMultiplier(int hashMultiplier) {
		this.hashMultiplier = hashMultiplier;
	}

	/**
	 * Constructs an empty set given the type as a generic type argument.
	 * This is usually less useful than just using the constructor, but can be handy
	 * in some code-generation scenarios when you don't know how many arguments you will have.
	 *
	 * @param <T>    the type of items; must be given explicitly
	 * @return a new set containing nothing
	 */
	public static <T> IdentityOrderedSet<T> with () {
		return new IdentityOrderedSet<>(0);
	}

	/**
	 * Creates a new IdentityOrderedSet that holds only the given item, but can be resized.
	 * @param item one T item
	 * @return a new IdentityOrderedSet that holds the given item
	 * @param <T> the type of item, typically inferred
	 */
	public static <T> IdentityOrderedSet<T> with (T item) {
		IdentityOrderedSet<T> set = new IdentityOrderedSet<>(1);
		set.add(item);
		return set;
	}

	/**
	 * Creates a new IdentityOrderedSet that holds only the given items, but can be resized.
	 * @param item0 a T item
	 * @param item1 a T item
	 * @return a new IdentityOrderedSet that holds the given items
	 * @param <T> the type of item, typically inferred
	 */
	public static <T> IdentityOrderedSet<T> with (T item0, T item1) {
		IdentityOrderedSet<T> set = new IdentityOrderedSet<>(2);
		set.add(item0, item1);
		return set;
	}

	/**
	 * Creates a new IdentityOrderedSet that holds only the given items, but can be resized.
	 * @param item0 a T item
	 * @param item1 a T item
	 * @param item2 a T item
	 * @return a new IdentityOrderedSet that holds the given items
	 * @param <T> the type of item, typically inferred
	 */
	public static <T> IdentityOrderedSet<T> with (T item0, T item1, T item2) {
		IdentityOrderedSet<T> set = new IdentityOrderedSet<>(3);
		set.add(item0, item1, item2);
		return set;
	}

	/**
	 * Creates a new IdentityOrderedSet that holds only the given items, but can be resized.
	 * @param item0 a T item
	 * @param item1 a T item
	 * @param item2 a T item
	 * @param item3 a T item
	 * @return a new IdentityOrderedSet that holds the given items
	 * @param <T> the type of item, typically inferred
	 */
	public static <T> IdentityOrderedSet<T> with (T item0, T item1, T item2, T item3) {
		IdentityOrderedSet<T> set = new IdentityOrderedSet<>(4);
		set.add(item0, item1, item2, item3);
		return set;
	}

	/**
	 * Creates a new IdentityOrderedSet that holds only the given items, but can be resized.
	 * @param item0 a T item
	 * @param item1 a T item
	 * @param item2 a T item
	 * @param item3 a T item
	 * @param item4 a T item
	 * @return a new IdentityOrderedSet that holds the given items
	 * @param <T> the type of item, typically inferred
	 */
	public static <T> IdentityOrderedSet<T> with (T item0, T item1, T item2, T item3, T item4) {
		IdentityOrderedSet<T> set = new IdentityOrderedSet<>(5);
		set.add(item0, item1, item2, item3);
		set.add(item4);
		return set;
	}

	/**
	 * Creates a new IdentityOrderedSet that holds only the given items, but can be resized.
	 * @param item0 a T item
	 * @param item1 a T item
	 * @param item2 a T item
	 * @param item3 a T item
	 * @param item4 a T item
	 * @param item5 a T item
	 * @return a new IdentityOrderedSet that holds the given items
	 * @param <T> the type of item, typically inferred
	 */
	public static <T> IdentityOrderedSet<T> with (T item0, T item1, T item2, T item3, T item4, T item5) {
		IdentityOrderedSet<T> set = new IdentityOrderedSet<>(6);
		set.add(item0, item1, item2, item3);
		set.add(item4, item5);
		return set;
	}

	/**
	 * Creates a new IdentityOrderedSet that holds only the given items, but can be resized.
	 * @param item0 a T item
	 * @param item1 a T item
	 * @param item2 a T item
	 * @param item3 a T item
	 * @param item4 a T item
	 * @param item5 a T item
	 * @param item6 a T item
	 * @return a new IdentityOrderedSet that holds the given items
	 * @param <T> the type of item, typically inferred
	 */
	public static <T> IdentityOrderedSet<T> with (T item0, T item1, T item2, T item3, T item4, T item5, T item6) {
		IdentityOrderedSet<T> set = new IdentityOrderedSet<>(7);
		set.add(item0, item1, item2, item3);
		set.add(item4, item5, item6);
		return set;
	}

	/**
	 * Creates a new IdentityOrderedSet that holds only the given items, but can be resized.
	 * @param item0 a T item
	 * @param item1 a T item
	 * @param item2 a T item
	 * @param item3 a T item
	 * @param item4 a T item
	 * @param item5 a T item
	 * @param item6 a T item
	 * @return a new IdentityOrderedSet that holds the given items
	 * @param <T> the type of item, typically inferred
	 */
	public static <T> IdentityOrderedSet<T> with (T item0, T item1, T item2, T item3, T item4, T item5, T item6, T item7) {
		IdentityOrderedSet<T> set = new IdentityOrderedSet<>(8);
		set.add(item0, item1, item2, item3);
		set.add(item4, item5, item6, item7);
		return set;
	}

	/**
	 * Creates a new IdentityOrderedSet that holds only the given items, but can be resized.
	 * This overload will only be used when an array is supplied and the type of the
	 * items requested is the component type of the array, or if varargs are used and
	 * there are 9 or more arguments.
	 * @param varargs a T varargs or T array; remember that varargs allocate
	 * @return a new IdentityOrderedSet that holds the given items
	 * @param <T> the type of item, typically inferred
	 */
	@SafeVarargs
	public static <T> IdentityOrderedSet<T> with (T... varargs) {
		return new IdentityOrderedSet<>(varargs);
	}
}

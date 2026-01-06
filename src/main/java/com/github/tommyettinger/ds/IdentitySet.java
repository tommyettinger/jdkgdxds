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

import com.github.tommyettinger.ds.support.util.PartialParser;

import java.util.Collection;
import java.util.Iterator;

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
	public IdentitySet() {
		super();
	}

	public IdentitySet(int initialCapacity) {
		super(initialCapacity);
	}

	public IdentitySet(int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
	}

	/**
	 * Creates a new instance containing the items in the specified iterator.
	 *
	 * @param coll an iterator that will have its remaining contents added to this
	 */
	public IdentitySet(Iterator<? extends T> coll) {
		super(coll);
	}

	public IdentitySet(ObjectSet<? extends T> set) {
		super(set);
	}

	public IdentitySet(Collection<? extends T> coll) {
		super(coll);
	}

	public IdentitySet(T[] array, int offset, int length) {
		super(array, offset, length);
	}

	public IdentitySet(T[] array) {
		super(array);
	}

	@Override
	protected int place(Object item) {
		return System.identityHashCode(item) & mask;
	}

	@Override
	protected boolean equate(Object left, Object right) {
		return left == right;
	}

	@Override
	public int hashCode() {
		int h = size;
		T[] keyTable = this.keyTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			T key = keyTable[i];
			if (key != null) {
				h += System.identityHashCode(key);
			}
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
	 * Unlike the superclass implementation, this does not alter the given int to make it odd.
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
	 * @param <T> the type of items; must be given explicitly
	 * @return a new set containing nothing
	 */
	public static <T> IdentitySet<T> with() {
		return new IdentitySet<>(0);
	}

	/**
	 * Creates a new IdentitySet that holds only the given item, but can be resized.
	 *
	 * @param item one T item
	 * @param <T>  the type of item, typically inferred
	 * @return a new IdentitySet that holds the given item
	 */
	public static <T> IdentitySet<T> with(T item) {
		IdentitySet<T> set = new IdentitySet<>(1);
		set.add(item);
		return set;
	}

	/**
	 * Creates a new IdentitySet that holds only the given items, but can be resized.
	 *
	 * @param item0 a T item
	 * @param item1 a T item
	 * @param <T>   the type of item, typically inferred
	 * @return a new IdentitySet that holds the given items
	 */
	public static <T> IdentitySet<T> with(T item0, T item1) {
		IdentitySet<T> set = new IdentitySet<>(2);
		set.add(item0, item1);
		return set;
	}

	/**
	 * Creates a new IdentitySet that holds only the given items, but can be resized.
	 *
	 * @param item0 a T item
	 * @param item1 a T item
	 * @param item2 a T item
	 * @param <T>   the type of item, typically inferred
	 * @return a new IdentitySet that holds the given items
	 */
	public static <T> IdentitySet<T> with(T item0, T item1, T item2) {
		IdentitySet<T> set = new IdentitySet<>(3);
		set.add(item0, item1, item2);
		return set;
	}

	/**
	 * Creates a new IdentitySet that holds only the given items, but can be resized.
	 *
	 * @param item0 a T item
	 * @param item1 a T item
	 * @param item2 a T item
	 * @param item3 a T item
	 * @param <T>   the type of item, typically inferred
	 * @return a new IdentitySet that holds the given items
	 */
	public static <T> IdentitySet<T> with(T item0, T item1, T item2, T item3) {
		IdentitySet<T> set = new IdentitySet<>(4);
		set.add(item0, item1, item2, item3);
		return set;
	}

	/**
	 * Creates a new IdentitySet that holds only the given items, but can be resized.
	 *
	 * @param item0 a T item
	 * @param item1 a T item
	 * @param item2 a T item
	 * @param item3 a T item
	 * @param item4 a T item
	 * @param <T>   the type of item, typically inferred
	 * @return a new IdentitySet that holds the given items
	 */
	public static <T> IdentitySet<T> with(T item0, T item1, T item2, T item3, T item4) {
		IdentitySet<T> set = new IdentitySet<>(5);
		set.add(item0, item1, item2, item3);
		set.add(item4);
		return set;
	}

	/**
	 * Creates a new IdentitySet that holds only the given items, but can be resized.
	 *
	 * @param item0 a T item
	 * @param item1 a T item
	 * @param item2 a T item
	 * @param item3 a T item
	 * @param item4 a T item
	 * @param item5 a T item
	 * @param <T>   the type of item, typically inferred
	 * @return a new IdentitySet that holds the given items
	 */
	public static <T> IdentitySet<T> with(T item0, T item1, T item2, T item3, T item4, T item5) {
		IdentitySet<T> set = new IdentitySet<>(6);
		set.add(item0, item1, item2, item3);
		set.add(item4, item5);
		return set;
	}

	/**
	 * Creates a new IdentitySet that holds only the given items, but can be resized.
	 *
	 * @param item0 a T item
	 * @param item1 a T item
	 * @param item2 a T item
	 * @param item3 a T item
	 * @param item4 a T item
	 * @param item5 a T item
	 * @param item6 a T item
	 * @param <T>   the type of item, typically inferred
	 * @return a new IdentitySet that holds the given items
	 */
	public static <T> IdentitySet<T> with(T item0, T item1, T item2, T item3, T item4, T item5, T item6) {
		IdentitySet<T> set = new IdentitySet<>(7);
		set.add(item0, item1, item2, item3);
		set.add(item4, item5, item6);
		return set;
	}

	/**
	 * Creates a new IdentitySet that holds only the given items, but can be resized.
	 *
	 * @param item0 a T item
	 * @param item1 a T item
	 * @param item2 a T item
	 * @param item3 a T item
	 * @param item4 a T item
	 * @param item5 a T item
	 * @param item6 a T item
	 * @param <T>   the type of item, typically inferred
	 * @return a new IdentitySet that holds the given items
	 */
	public static <T> IdentitySet<T> with(T item0, T item1, T item2, T item3, T item4, T item5, T item6, T item7) {
		IdentitySet<T> set = new IdentitySet<>(8);
		set.add(item0, item1, item2, item3);
		set.add(item4, item5, item6, item7);
		return set;
	}

	/**
	 * Creates a new IdentitySet that holds only the given items, but can be resized.
	 * This overload will only be used when an array is supplied and the type of the
	 * items requested is the component type of the array, or if varargs are used and
	 * there are 9 or more arguments.
	 *
	 * @param varargs a T varargs or T array; remember that varargs allocate
	 * @param <T>     the type of item, typically inferred
	 * @return a new IdentitySet that holds the given items
	 */
	@SafeVarargs
	public static <T> IdentitySet<T> with(T... varargs) {
		return new IdentitySet<>(varargs);
	}

	/**
	 * Calls {@link #parse(String, String, PartialParser, boolean)} with brackets set to false.
	 *
	 * @param str       a String that will be parsed in full
	 * @param delimiter the delimiter between items in str
	 * @param parser    a PartialParser that returns a {@code T} item from a section of {@code str}
	 * @return a new collection parsed from str
	 */
	public static <T> IdentitySet<T> parse(String str, String delimiter, PartialParser<T> parser) {
		return parse(str, delimiter, parser, false);
	}

	/**
	 * Creates a new collection and fills it by calling {@link #addLegible(String, String, PartialParser, int, int)} on
	 * either all of {@code str} (if {@code brackets} is false) or {@code str} without its first and last chars (if
	 * {@code brackets} is true). Each item is expected to be separated by {@code delimiter}.
	 *
	 * @param str       a String that will be parsed in full (depending on brackets)
	 * @param delimiter the delimiter between items in str
	 * @param parser    a PartialParser that returns a {@code T} item from a section of {@code str}
	 * @param brackets  if true, the first and last chars in str will be ignored
	 * @return a new collection parsed from str
	 */
	public static <T> IdentitySet<T> parse(String str, String delimiter, PartialParser<T> parser, boolean brackets) {
		IdentitySet<T> c = new IdentitySet<>();
		if (brackets)
			c.addLegible(str, delimiter, parser, 1, str.length() - 1);
		else
			c.addLegible(str, delimiter, parser);
		return c;
	}

	/**
	 * Creates a new collection and fills it by calling {@link #addLegible(String, String, PartialParser, int, int)}
	 * with the given five parameters as-is.
	 *
	 * @param str       a String that will have the given section parsed
	 * @param delimiter the delimiter between items in str
	 * @param parser    a PartialParser that returns a {@code T} item from a section of {@code str}
	 * @param offset    the first position to parse in str, inclusive
	 * @param length    how many chars to parse, starting from offset
	 * @return a new collection parsed from str
	 */
	public static <T> IdentitySet<T> parse(String str, String delimiter, PartialParser<T> parser, int offset, int length) {
		IdentitySet<T> c = new IdentitySet<>();
		c.addLegible(str, delimiter, parser, offset, length);
		return c;
	}
}

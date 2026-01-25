/*
 * Copyright (c) 2024-2025 See AUTHORS file.
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

import com.github.tommyettinger.ds.support.util.Appender;
import com.github.tommyettinger.ds.support.util.PartialParser;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

/**
 * Augments {@link Collection} with default methods that can usually use the same implementation across subtypes.
 * This generally brings Object-based collections to parity with the default methods provided by the various
 * {@link PrimitiveCollection} types.
 *
 * @param <T> the type of items in this collection
 */
public interface EnhancedCollection<T> extends Collection<T> {
	/**
	 * Adds all parameters using {@link #add(Object)} for each one. Returns true if the set was modified.
	 *
	 * @param item0 a T item
	 * @param item1 a T item
	 * @return true if this modified the set
	 */
	default boolean add(T item0, T item1) {
		return add(item0) | add(item1);
	}

	/**
	 * Adds all parameters using {@link #add(Object)} for each one. Returns true if the set was modified.
	 *
	 * @param item0 a T item
	 * @param item1 a T item
	 * @param item2 a T item
	 * @return true if this modified the set
	 */
	default boolean add(T item0, T item1, T item2) {
		return add(item0) | add(item1) | add(item2);
	}

	/**
	 * Adds all parameters using {@link #add(Object)} for each one. Returns true if the set was modified.
	 *
	 * @param item0 a T item
	 * @param item1 a T item
	 * @param item2 a T item
	 * @param item3 a T item
	 * @return true if this modified the set
	 */
	default boolean add(T item0, T item1, T item2, T item3) {
		return add(item0) | add(item1) | add(item2) | add(item3);
	}

	/**
	 * Gets the {@link Iterable#iterator()} from the parameter and delegates to {@link #addAll(Iterator)}.
	 *
	 * @param it an Iterable of items to append to this EnhancedCollection
	 * @return true if this collection was modified.
	 */
	default boolean addAllIterable(Iterable<? extends T> it) {
		return addAll(it.iterator());
	}

	/**
	 * Goes through the given Iterator until it is exhausted, adding every item to this EnhancedCollection using
	 * {@link #add(Object)}.
	 *
	 * @param it an Iterator of items to append to this EnhancedCollection
	 * @return true if this collection was modified.
	 */
	default boolean addAll(Iterator<? extends T> it) {
		int oldSize = size();
		while (it.hasNext()) {
			add(it.next());
		}
		return oldSize != size();
	}

	default boolean addAll(T[] array) {
		return addAll(array, 0, array.length);
	}

	default boolean addAll(T[] array, int offset, int length) {
		boolean changed = false;
		for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
			changed |= add(array[i]);
		}
		return changed;
	}

	/**
	 * Takes an array of items to add, or more simply 0 or more arguments that will each be added.
	 * If {@code varargs} is null, this won't add anything and will return false.
	 * If you have what is usually an array, consider calling {@link #addAll(Object[])} to avoid the
	 * possibility of heap pollution from a varargs with a generic type.
	 *
	 * @param varargs 0 or more items to add; may also be an array
	 * @return true if this collection was modified
	 */
	default boolean addVarargs(T... varargs) {
		return varargs != null && addAll(varargs, 0, varargs.length);
	}

	/**
	 * Gets the {@link Iterable#iterator()} from the parameter and delegates to {@link #removeAll(Iterator)}.
	 *
	 * @param it an Iterable of items to remove fully
	 * @return true if this collection was modified.
	 */
	default boolean removeAllIterable(Iterable<? extends T> it) {
		return removeAll(it.iterator());
	}

	/**
	 * Removes from this collection all occurrences of any elements contained in the specified Iterator.
	 *
	 * @param it an Iterator of items to remove fully
	 * @return true if this collection was modified.
	 */
	default boolean removeAll(Iterator<? extends T> it) {
		Iterator<T> me;
		int originalSize = size();
		while (it.hasNext()) {
			T item = it.next();
			me = iterator();
			while (me.hasNext()) {
				if (me.next() == item) {
					me.remove();
				}
			}
		}
		return originalSize != size();
	}

	/**
	 * Removes from this collection all occurrences of any elements contained in the specified array.
	 *
	 * @param array a non-null array of items to remove fully
	 * @return true if this collection was modified.
	 */
	default boolean removeAll(Object[] array) {
		return removeAll(array, 0, array.length);
	}

	/**
	 * Removes from this collection all occurrences of any elements contained in the specified array, but only starts
	 * reading elements from the array starting at the given {@code offset} and only uses {@code length} items.
	 *
	 * @param array  a non-null array of items to remove fully
	 * @param offset the first index in {@code array} to use
	 * @param length how many items from {@code array} should be used
	 * @return true if this collection was modified.
	 */
	default boolean removeAll(Object[] array, int offset, int length) {
		Iterator<T> me;
		int originalSize = size();
		for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
			Object item = array[i];
			me = iterator();
			while (me.hasNext()) {
				if (me.next() == item) {
					me.remove();
				}
			}
		}
		return originalSize != size();
	}

	/**
	 * Removes from this collection element-wise occurrences of elements contained in the specified Iterable.
	 * Note that if a value is present more than once in this collection, only one of those occurrences
	 * will be removed for each occurrence of that value in {@code other}. If {@code other} has the same
	 * contents as this collection or has additional items, then removing each of {@code other} will clear this.
	 *
	 * @param other an Iterable of any items to remove one-by-one, such as an ObjectList or ObjectSet
	 * @return true if this collection was modified.
	 */
	default boolean removeEachIterable(Iterable<?> other) {
		return removeEach(other.iterator());
	}

	/**
	 * Removes from this collection element-wise occurrences of elements given by the specified Iterator.
	 * Note that if a value is present more than once in this collection, only one of those occurrences
	 * will be removed for each time {@code other} yields that value. If {@code other} has the same
	 * contents as this collection or has additional items, then removing each of {@code other} will clear this.
	 *
	 * @param it an Iterator of any items to remove one-by-one, such as an ObjectList or ObjectSet
	 * @return true if this collection was modified.
	 */
	default boolean removeEach(Iterator<?> it) {
		boolean changed = false;
		while (it.hasNext()) {
			changed |= remove(it.next());
		}
		return changed;
	}

	/**
	 * Removes from this collection element-wise occurrences of elements contained in the specified array.
	 * Note that if a value is present more than once in this collection, only one of those occurrences
	 * will be removed for each occurrence of that value in {@code other}. If {@code other} has the same
	 * contents as this collection or has additional items, then removing each of {@code other} will clear this.
	 *
	 * @param array an array of any items to remove one-by-one, such as an ObjectList or ObjectSet
	 * @return true if this collection was modified.
	 */
	default boolean removeEach(Object[] array) {
		return removeEach(array, 0, array.length);
	}

	/**
	 * Removes from this collection element-wise occurrences of elements contained in the specified array.
	 * Note that if a value is present more than once in this collection, only one of those occurrences
	 * will be removed for each occurrence of that value in {@code other}. If {@code other} has the same
	 * contents as this collection or has additional items, then removing each of {@code other} will clear this.
	 *
	 * @param array  an array of any items to remove one-by-one, such as an ObjectList or ObjectSet
	 * @param offset the first index in {@code array} to use
	 * @param length how many items from {@code array} should be used
	 * @return true if this collection was modified.
	 */
	default boolean removeEach(Object[] array, int offset, int length) {
		boolean changed = false;
		for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
			changed |= remove(array[i]);
		}
		return changed;
	}

	/**
	 * Returns {@code true} if this collection contains all the elements
	 * in the specified Iterable.
	 *
	 * @param it a non-null Collection or other Iterable to have items checked for containment in this collection
	 * @return {@code true} if this collection contains all the elements
	 * in the specified Iterable
	 * @throws NullPointerException if the specified Iterable is null.
	 * @see #contains(Object)
	 */
	default boolean containsAllIterable(Iterable<?> it) {
		return containsAll(it.iterator());
	}

	/**
	 * Returns {@code true} if this collection contains all the elements
	 * remaining in the specified Iterator.
	 *
	 * @param it a non-null Iterator to have items checked for containment in this collection
	 * @return {@code true} if this collection contains all the elements
	 * in the specified Iterator
	 * @throws NullPointerException if the specified Iterator is null.
	 * @see #contains(Object)
	 */
	default boolean containsAll(Iterator<?> it) {
		while (it.hasNext()) {
			if (!contains(it.next())) return false;
		}
		return true;
	}

	/**
	 * Returns {@code true} if this collection contains all the elements
	 * in the specified array.
	 *
	 * @param array a non-null array to have items checked for containment in this collection
	 * @return {@code true} if this collection contains all the elements
	 * in the specified collection
	 * @throws NullPointerException if the specified collection is null.
	 * @see #contains(Object)
	 */
	default boolean containsAll(Object[] array) {
		return containsAll(array, 0, array.length);
	}

	/**
	 * Returns {@code true} if this collection contains all the elements
	 * in the specified array starting from {@code offset} and using {@code length} items from the array.
	 *
	 * @param array  a non-null array to have items checked for containment in this collection
	 * @param offset the first index in {@code array} to use
	 * @param length how many items from {@code array} should be used
	 * @return {@code true} if this collection contains all the elements
	 * in the specified collection
	 * @throws NullPointerException if the specified collection is null.
	 * @see #contains(Object)
	 */
	default boolean containsAll(Object[] array, int offset, int length) {
		for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
			if (!contains(array[i])) return false;
		}
		return true;
	}

	/**
	 * Like {@link Collection#containsAll(Collection)}, but returns true immediately if any item in the given Iterable
	 * {@code other} is present in this EnhancedCollection.
	 *
	 * @param other a Collection or other Iterable of any type to look through
	 * @return true if any items from the Iterable are present in this EnhancedCollection
	 */
	default boolean containsAnyIterable(Iterable<?> other) {
		return containsAny(other.iterator());
	}

	/**
	 * Like {@link Collection#containsAll(Collection)}, but returns true immediately if any item in the given Iterator
	 * {@code it} is present in this EnhancedCollection.
	 *
	 * @param it an Iterator of any type to look through
	 * @return true if any items from the Iterator are present in this EnhancedCollection
	 */
	default boolean containsAny(Iterator<?> it) {
		while (it.hasNext()) {
			if (contains(it.next())) return true;
		}
		return false;
	}

	default boolean containsAny(Object[] array) {
		return containsAny(array, 0, array.length);
	}

	/**
	 * Like {@link Collection#containsAll(Collection)}, but returns true immediately if any item in the given {@code array}
	 * is present in this EnhancedCollection.
	 *
	 * @param array  an array to look through; will not be modified
	 * @param offset the first index in array to check
	 * @param length how many items in array to check
	 * @return true if any items from array are present in this EnhancedCollection
	 */
	default boolean containsAny(Object[] array, int offset, int length) {
		for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
			if (contains(array[i])) return true;
		}
		return false;
	}

	/**
	 * Attempts to get the first item in this EnhancedCollection, where "first" is only
	 * defined meaningfully if this type is ordered. Many times, this applies to a class
	 * that is not ordered, and in those cases it can get an arbitrary item, and that item
	 * is permitted to be different for different calls to first().
	 * <br>
	 * This is useful for cases where you would normally be able to call something like
	 * {@link java.util.List#get(int)} to get an item, any item, from a collection, but
	 * whatever class you're using doesn't necessarily provide a get(), first(), peek(),
	 * or similar method.
	 * <br>
	 * The default implementation uses {@link #iterator()}, tries to get the first item,
	 * or throws an IllegalStateException if this is empty.
	 *
	 * @return the first item in this EnhancedCollection, as produced by {@link #iterator()}
	 * @throws IllegalStateException if this is empty
	 */
	default T first() {
		Iterator<T> it = iterator();
		if (it.hasNext())
			return it.next();
		throw new IllegalStateException("Can't get the first() item of an empty EnhancedCollection.");
	}

	// STRING CONVERSION

	/**
	 * Delegates to {@link #toString(String, boolean)} with the given itemSeparator and without surrounding brackets.
	 *
	 * @param itemSeparator how to separate items, such as {@code ", "}
	 * @return a new String representing this map
	 */
	default String toString(String itemSeparator) {
		return toString(itemSeparator, false);
	}

	/**
	 * Makes a String from the contents of this EnhancedCollection, using the {@link Object#toString()} method of each
	 * item, separating items with the given {@code itemSeparator}, and wrapping the result in square brackets if
	 * {@code brackets} is true.
	 * <br>
	 * Delegates to {@link #appendTo(CharSequence, String, boolean)}.
	 *
	 * @param itemSeparator how to separate items, such as {@code ", "}
	 * @param brackets      true to wrap the result in square brackets, or false to leave the items unadorned
	 * @return a new String representing this EnhancedCollection
	 */
	default String toString(String itemSeparator, boolean brackets) {
		return appendTo(new StringBuilder(6 * size()), itemSeparator, brackets).toString();
	}

	/**
	 * Makes a String from the contents of this EnhancedCollection, but uses the given {@link Appender}
	 * to convert each item to a customizable representation and append them to a StringBuilder. To use
	 * the default String representation, you can use {@code Appender::append} as an appender.
	 * <br>
	 * Be advised that {@code Appender::append} will
	 * allocate a method reference, each time this is called, on minimized Android builds due to R8 behavior. You can
	 * cache an Appender of the appropriate T type easily, however, as with this for when T is String:
	 * {@code public static final Appender<String> STRING_APPENDER = Appender::append;}
	 * (There is also {@link Appender#STRING_APPENDER} already.)
	 * <br>
	 * Delegates to {@link #appendTo(CharSequence, String, boolean, Appender)}.
	 *
	 * @param separator how to separate items, such as {@code ", "}
	 * @param brackets  true to wrap the output in square brackets, or false to omit them
	 * @param appender  a function that takes a StringBuilder and a T, and returns the modified StringBuilder
	 * @return a new String representing this EnhancedCollection
	 */
	default String toString(String separator, boolean brackets,
							Appender<T> appender) {
		return appendTo(new StringBuilder(6 * size()), separator, brackets, appender).toString();
	}

	/**
	 * Appends to an Appendable CharSequence from the contents of this EnhancedCollection, using {@code Appender::append} to
	 * append each item's String representation, separating items with {@code separator}, and optionally wrapping the
	 * output in square brackets if {@code brackets} is true.
	 * <br>
	 * Be advised that {@code Appender::append} will
	 * allocate a method reference, each time this is called, on minimized Android builds due to R8 behavior. You can
	 * cache an Appender of the appropriate T type easily, however, as with this for when T is String:
	 * {@code public static final Appender<String> STRING_APPENDER = Appender::append;}
	 * (There is also {@link Appender#STRING_APPENDER} already.)
	 * <br>
	 * Delegates to {@link #appendTo(CharSequence, String, boolean, Appender)}.
	 *
	 * @param sb        an Appendable CharSequence that this can append to
	 * @param separator how to separate items, such as {@code ", "}
	 * @param brackets  true to wrap the output in square brackets, or false to omit them
	 * @return {@code sb}, with the appended items of this EnhancedCollection
	 * @param <S>  any type that is both a CharSequence and an Appendable, such as StringBuilder, StringBuffer, CharBuffer, or CharList
	 */
	default <S extends CharSequence & Appendable> S appendTo(S sb, String separator, boolean brackets) {
		return appendTo(sb, separator, brackets, Appender::append);
	}

	/**
	 * Appends to an Appendable CharSequence from the contents of this EnhancedCollection, but uses the given {@link Appender}
	 * to convert each item to a customizable representation and append them to an Appendable CharSequence. To use
	 * the default String representation, you can use {@code Appender::append}, but be advised that it will
	 * allocate a method reference, each time this is called, on minimized Android builds due to R8 behavior. You can
	 * cache an Appender of the appropriate T type easily, however, as with this for when T is String:
	 * {@code public static final Appender<String> STRING_APPENDER = Appender::append;}
	 * (There is also {@link Appender#STRING_APPENDER} already.)
	 *
	 * @param sb        an Appendable CharSequence that this can append to
	 * @param separator how to separate items, such as {@code ", "}
	 * @param brackets  true to wrap the output in square brackets, or false to omit them
	 * @param appender  a function that takes an Appendable CharSequence and a T, and returns the modified {@code S}
	 * @return {@code sb}, with the appended items of this EnhancedCollection
	 * @param <S>  any type that is both a CharSequence and an Appendable, such as StringBuilder, StringBuffer, CharBuffer, or CharList
	 */
	default <S extends CharSequence & Appendable> S appendTo(S sb, String separator, boolean brackets, Appender<T> appender) {
		try {
			if (isEmpty()) {
				if (brackets) sb.append("[]");
				return sb;
			}
			if (brackets) {
				sb.append('[');
			}
			Iterator<T> it = iterator();
			if (it.hasNext()) {
				while (true) {
					T next = it.next();
					if (next == this) sb.append("(this)");
					else appender.apply(sb, next);
					if (it.hasNext()) sb.append(separator);
					else break;
				}
			}
			if (brackets) sb.append(']');
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return sb;
	}

	/**
	 * Adds items to this EnhancedCollection drawn from the result of {@link #toString(String)} or
	 * {@link #appendTo(CharSequence, String, boolean)}.
	 * A PartialParser will be used to parse items from sections of {@code str}. Any brackets inside the given range
	 * of characters will ruin the parsing, so increase offset by 1 and
	 * reduce length by 2 if the original String had brackets added to it.
	 *
	 * @param str       a String containing string representations of items
	 * @param delimiter the String separating every item in str
	 * @param parser    a PartialParser that returns a {@code T} item from a section of {@code str}
	 */
	default void addLegible(String str, String delimiter, PartialParser<T> parser) {
		addLegible(str, delimiter, parser, 0, -1);
	}

	/**
	 * Adds items to this EnhancedCollection drawn from the result of {@link #toString(String)} or
	 * {@link #appendTo(CharSequence, String, boolean)}.
	 * A PartialParser will be used to parse items from sections of {@code str}. Any brackets inside the given range
	 * of characters will ruin the parsing, so increase offset by 1 and
	 * reduce length by 2 if the original String had brackets added to it.
	 *
	 * @param str       a String containing string representations of items
	 * @param delimiter the String separating every item in str
	 * @param parser    a PartialParser that returns a {@code T} item from a section of {@code str}
	 * @param offset    the first position to read items from in {@code str}
	 * @param length    how many chars to read; -1 is treated as maximum length
	 */
	default void addLegible(String str, String delimiter, PartialParser<T> parser, int offset, int length) {
		int sl, dl;
		if (str == null || delimiter == null || parser == null || (sl = str.length()) < 1 || (dl = delimiter.length()) < 1 || offset < 0 || offset > sl - 1)
			return;
		final int lim = length < 0 ? sl : Math.min(offset + length, sl);
		int end = str.indexOf(delimiter, offset + 1);
		while (end != -1 && end + dl < lim) {
			add(parser.parse(str, offset, end));
			offset = end + dl;
			end = str.indexOf(delimiter, offset + 1);
		}
		if (offset < lim) {
			add(parser.parse(str, offset, lim));
		}
	}
}

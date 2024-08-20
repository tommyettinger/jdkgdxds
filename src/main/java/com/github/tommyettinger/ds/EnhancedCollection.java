/*
 * Copyright (c) 2024 See AUTHORS file.
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

import com.github.tommyettinger.ds.support.util.Appender;

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
	default boolean addAll (Iterator<? extends T> it) {
		int oldSize = size();
		while (it.hasNext()) {
			add(it.next());
		}
		return oldSize != size();
	}
	/**
	 * Removes from this collection all occurrences of any elements contained in the specified Iterator.
	 *
	 * @param it an Iterator of items to remove fully
	 * @return true if this collection was modified.
	 */
	default boolean removeAll (Iterator<? extends T> it) {
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

	default boolean removeAll (Object[] array) {
		return removeAll(array, 0, array.length);
	}

	default boolean removeAll (Object[] array, int offset, int length) {
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
	 * Removes from this collection element-wise occurrences of elements contained in the specified other collection.
	 * Note that if a value is present more than once in this collection, only one of those occurrences
	 * will be removed for each occurrence of that value in {@code other}. If {@code other} has the same
	 * contents as this collection or has additional items, then removing each of {@code other} will clear this.
	 *
	 * @param other an Iterable of any items to remove one-by-one, such as an ObjectList or ObjectSet
	 * @return true if this collection was modified.
	 */
	default boolean removeEach (Iterable<?> other) {
		return removeEach(other.iterator());
	}

	/**
	 * Removes from this collection element-wise occurrences of elements contained in the specified other collection.
	 * Note that if a value is present more than once in this collection, only one of those occurrences
	 * will be removed for each occurrence of that value in {@code other}. If {@code other} has the same
	 * contents as this collection or has additional items, then removing each of {@code other} will clear this.
	 *
	 * @param it an Iterable of any items to remove one-by-one, such as an ObjectList or ObjectSet
	 * @return true if this collection was modified.
	 */
	default boolean removeEach (Iterator<?> it) {
		boolean changed = false;
		while (it.hasNext()) {
			changed |= remove(it.next());
		}
		return changed;
	}

	default boolean removeEach (Object[] array) {
		return removeEach(array, 0, array.length);
	}

	default boolean removeEach (Object[] array, int offset, int length) {
		boolean changed = false;
		for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
			changed |= remove(array[i]);
		}
		return changed;
	}

	default boolean containsAll (Iterator<?> it) {
		while (it.hasNext()) {
			if(!contains(it.next())) return false;
		}
		return true;
	}

	default boolean containsAll (Object[] array) {
		return containsAll(array, 0, array.length);
	}

	default boolean containsAll (Object[] array, int offset, int length) {
		for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
			if(!contains(array[i])) return false;
		}
		return true;
	}

	/**
	 * Like {@link Collection#containsAll(Collection)}, but returns true immediately if any item in the given Iterable
	 * {@code other} is present in this EnhancedCollection.
	 * @param other a Collection or other Iterable of any type to look through
	 * @return true if any items from the Iterable are present in this EnhancedCollection
	 */
	default boolean containsAny (Iterable<?> other) {
		return containsAny(other.iterator());
	}

	/**
	 * Like {@link Collection#containsAll(Collection)}, but returns true immediately if any item in the given Iterator
	 * {@code it} is present in this EnhancedCollection.
	 * @param it an Iterator of any type to look through
	 * @return true if any items from the Iterator are present in this EnhancedCollection
	 */
	default boolean containsAny (Iterator<?> it) {
		while (it.hasNext()) {
			if(contains(it.next())) return true;
		}
		return false;
	}

	default boolean containsAny (Object[] array) {
		return containsAny(array, 0, array.length);
	}

	/**
	 * Like {@link Collection#containsAll(Collection)}, but returns true immediately if any item in the given {@code array}
	 * is present in this EnhancedCollection.
	 * @param array an array to look through; will not be modified
	 * @param offset the first index in array to check
	 * @param length how many items in array to check
	 * @return true if any items from array are present in this EnhancedCollection
	 */
	default boolean containsAny (Object[] array, int offset, int length) {
		for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
			if(contains(array[i])) return true;
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
	default T first () {
		Iterator<T> it = iterator();
		if (it.hasNext())
			return it.next();
		throw new IllegalStateException("Can't get the first() item of an empty EnhancedCollection.");
	}

	// STRING CONVERSION

	/**
	 * Delegates to {@link #toString(String, boolean)} with the given entrySeparator and without brackets.
	 *
	 * @param entrySeparator how to separate entries, such as {@code ", "}
	 * @return a new String representing this map
	 */
	default String toString (String entrySeparator) {
		return toString(entrySeparator, false);
	}

	default String toString (String entrySeparator, boolean brackets) {
		return appendTo(new StringBuilder(32), entrySeparator, brackets).toString();
	}

	/**
	 * Makes a String from the contents of this EnhancedCollection, but uses the given {@link Appender}
	 * to convert each item to a customizable representation and append them to a StringBuilder. To use
	 * the default String representation, you can use {@code StringBuilder::append} as an appender.
	 *
	 * @param separator how to separate items, such as {@code ", "}
	 * @param brackets true to wrap the output in square brackets, or false to omit them
	 * @param appender a function that takes a StringBuilder and a T, and returns the modified StringBuilder
	 * @return a new String representing this EnhancedCollection
	 */
	default String toString (String separator, boolean brackets,
		Appender<T> appender){
		return appendTo(new StringBuilder(), separator, brackets, appender).toString();
	}

	default StringBuilder appendTo (StringBuilder sb, String separator, boolean brackets) {
		return appendTo(sb, separator, brackets, StringBuilder::append);
	}

	/**
	 * Appends to a StringBuilder from the contents of this EnhancedCollection, but uses the given {@link Appender}
	 * to convert each item to a customizable representation and append them to a StringBuilder. To use
	 * the default String representation, you can use {@code StringBuilder::append} as an appender.
	 *
	 * @param sb a StringBuilder that this can append to
	 * @param separator how to separate items, such as {@code ", "}
	 * @param brackets true to wrap the output in square brackets, or false to omit them
	 * @param appender a function that takes a StringBuilder and a T, and returns the modified StringBuilder
	 * @return {@code sb}, with the appended items of this EnhancedCollection
	 */
	default StringBuilder appendTo (StringBuilder sb, String separator, boolean brackets, Appender<T> appender) {
		if (isEmpty()) {return brackets ? sb.append("[]") : sb;}
		if (brackets) {sb.append('[');}
		Iterator<T> it = iterator();
		while (it.hasNext()) {
			T next = it.next();
			if(next == this) sb.append("(this)");
			else appender.apply(sb, next);
			if(it.hasNext()) sb.append(separator);
		}
		if (brackets) {sb.append(']');}
		return sb;
	}

}

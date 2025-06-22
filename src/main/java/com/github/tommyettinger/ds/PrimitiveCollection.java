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

import com.github.tommyettinger.ds.support.util.BooleanAppender;
import com.github.tommyettinger.ds.support.util.ByteAppender;
import com.github.tommyettinger.ds.support.util.CharAppender;
import com.github.tommyettinger.ds.support.util.DoubleAppender;
import com.github.tommyettinger.ds.support.util.DoubleIterator;
import com.github.tommyettinger.ds.support.util.FloatAppender;
import com.github.tommyettinger.ds.support.util.IntAppender;
import com.github.tommyettinger.ds.support.util.IntIterator;
import com.github.tommyettinger.ds.support.util.LongAppender;
import com.github.tommyettinger.ds.support.util.LongIterator;
import com.github.tommyettinger.ds.support.util.ShortAppender;
import com.github.tommyettinger.function.BooleanConsumer;
import com.github.tommyettinger.function.BooleanPredicate;
import com.github.tommyettinger.function.ByteConsumer;
import com.github.tommyettinger.function.BytePredicate;
import com.github.tommyettinger.function.CharConsumer;
import com.github.tommyettinger.function.CharPredicate;
import com.github.tommyettinger.function.FloatConsumer;
import com.github.tommyettinger.function.FloatPredicate;
import com.github.tommyettinger.function.ShortConsumer;
import com.github.tommyettinger.function.ShortPredicate;
import com.github.tommyettinger.ds.support.util.BooleanIterator;
import com.github.tommyettinger.ds.support.util.ByteIterator;
import com.github.tommyettinger.ds.support.util.CharIterator;
import com.github.tommyettinger.ds.support.util.FloatIterator;
import com.github.tommyettinger.ds.support.util.ShortIterator;

import java.util.Iterator;

import com.github.tommyettinger.function.DoubleConsumer;
import com.github.tommyettinger.function.DoublePredicate;
import com.github.tommyettinger.function.IntConsumer;
import com.github.tommyettinger.function.IntPredicate;
import com.github.tommyettinger.function.LongConsumer;
import com.github.tommyettinger.function.LongPredicate;

/**
 * Analogous to {@link java.util.Collection} but for a primitive type, this is technically built around
 * {@link Iterator}, but should almost always use a primitive-specialized iterator such as
 * {@link FloatIterator} instead of the generic {@link Iterator}. This is not necessarily a modifiable
 * collection. The nested interfaces define most of the actually useful operations, and you will probably
 * never use PrimitiveCollection directly.
 */
public interface PrimitiveCollection<T> {
	int size ();

	default boolean isEmpty () {
		return size() == 0;
	}

	default boolean notEmpty () {
		return size() != 0;
	}

	Iterator<T> iterator ();

	void clear ();

	@Override
	int hashCode ();

	@Override
	boolean equals (Object other);

	/**
	 * A PrimitiveCollection with unboxed {@code int} items.
	 */
	interface OfInt extends PrimitiveCollection<Integer> {
		boolean add (int item);

		boolean remove (int item);

		boolean contains (int item);

		default boolean addAll (OfInt other) {
			return addAll(other.iterator());
		}

		default boolean addAll (IntIterator it) {
			boolean changed = false;
			while (it.hasNext()) {
				changed |= add(it.nextInt());
			}
			return changed;
		}

		default boolean addAll (int[] array) {
			return addAll(array, 0, array.length);
		}

		default boolean addAll (int[] array, int offset, int length) {
			boolean changed = false;
			for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
				changed |= add(array[i]);
			}
			return changed;
		}

		/**
		 * Removes from this collection all occurrences of any elements contained in the specified other collection.
		 *
		 * @param other a primitive collection of int items to remove fully, such as an IntList or an IntSet
		 * @return true if this collection was modified.
		 */
		default boolean removeAll (OfInt other) {
			return removeAll(other.iterator());
		}
		/**
		 * Removes from this collection all occurrences of any elements contained in the specified IntIterator.
		 *
		 * @param it a IntIterator of items to remove fully
		 * @return true if this collection was modified.
		 */
		default boolean removeAll (IntIterator it) {
			IntIterator me;
			int originalSize = size();
			while (it.hasNext()) {
				int item = it.nextInt();
				me = iterator();
				while (me.hasNext()) {
					if (me.nextInt() == item) {
						me.remove();
					}
				}
			}
			return originalSize != size();
		}

		default boolean removeAll (int[] array) {
			return removeAll(array, 0, array.length);
		}

		default boolean removeAll (int[] array, int offset, int length) {
			IntIterator me;
			int originalSize = size();
			for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
				int item = array[i];
				me = iterator();
				while (me.hasNext()) {
					if (me.nextInt() == item) {
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
		 * @param other a primitive collection of int items to remove one-by-one, such as an IntList or an IntSet
		 * @return true if this collection was modified.
		 */
		default boolean removeEach (OfInt other) {
			return removeEach(other.iterator());
		}
		/**
		 * Removes from this collection element-wise occurrences of elements contained in the specified IntIterator.
		 * Note that if a value is present more than once in this collection, only one of those occurrences
		 * will be removed for each occurrence of that value in {@code it}. If {@code it} has the same
		 * contents as this collection or has additional items, then removing each of {@code it} will clear this.
		 *
		 * @param it a IntIterator of items to remove one-by-one
		 * @return true if this collection was modified.
		 */
		default boolean removeEach (IntIterator it) {
			boolean changed = false;
			while (it.hasNext()) {
				changed |= remove(it.nextInt());
			}
			return changed;
		}

		default boolean removeEach (int[] array) {
			return removeEach(array, 0, array.length);
		}

		default boolean removeEach (int[] array, int offset, int length) {
			boolean changed = false;
			for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
				changed |= remove(array[i]);
			}
			return changed;
		}

		default boolean containsAll (OfInt other) {
			return containsAll(other.iterator());
		}

		default boolean containsAll (IntIterator it) {
			while (it.hasNext()) {
				if(!contains(it.nextInt())) return false;
			}
			return true;
		}

		default boolean containsAll (int[] array) {
			return containsAll(array, 0, array.length);
		}

		default boolean containsAll (int[] array, int offset, int length) {
			for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
				if(!contains(array[i])) return false;
			}
			return true;
		}

		default boolean containsAny (OfInt other) {
			return containsAny(other.iterator());
		}

		default boolean containsAny (IntIterator it) {
			while (it.hasNext()) {
				if(contains(it.nextInt())) return true;
			}
			return false;
		}

		default boolean containsAny (int[] array) {
			return containsAny(array, 0, array.length);
		}

		default boolean containsAny (int[] array, int offset, int length) {
			for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
				if(contains(array[i])) return true;
			}
			return false;
		}

		/**
		 * Removes all the elements of this collection that satisfy the given
		 * predicate.  Errors or runtime exceptions thrown during iteration or by
		 * the predicate are relayed to the caller.
		 *
		 * @param filter a predicate which returns {@code true} for elements to be
		 *               removed
		 * @return {@code true} if any elements were removed
		 * @throws UnsupportedOperationException if elements cannot be removed
		 *                                       from this collection.  Implementations may throw this exception if a
		 *                                       matching element cannot be removed or if, in general, removal is not
		 *                                       supported.
		 * @implSpec The default implementation traverses all elements of the collection using
		 * its {@link #iterator()}.  Each matching element is removed using
		 * {@link Iterator#remove()}.  If the collection's iterator does not
		 * support removal then an {@code UnsupportedOperationException} will be
		 * thrown on the first matching element.
		 */
		default boolean removeIf (IntPredicate filter) {
			boolean removed = false;
			final IntIterator each = iterator();
			while (each.hasNext()) {
				if (filter.test(each.nextInt())) {
					each.remove();
					removed = true;
				}
			}
			return removed;
		}

		default boolean retainAll (OfInt other) {
			boolean changed = false;
			IntIterator it = iterator();
			while (it.hasNext()) {
				if (!other.contains(it.nextInt())) {
					it.remove();
					changed = true;
				}
			}
			return changed;
		}
		
		default boolean retainAll (int[] array) {
			return retainAll(array, 0, array.length);
		}

		default boolean retainAll (int[] array, int offset, int length) {
			boolean modified = false;
			IntIterator it = iterator();
			OUTER:
			while (it.hasNext()) {
				int check = it.next();
				for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
					if(array[i] == check)
						continue OUTER;
				}
				it.remove();
				modified = true;
			}
			return modified;
		}

		@Override
		IntIterator iterator ();

		/**
		 * Allocates a new int array with exactly {@link #size()} items, fills it with the
		 * contents of this PrimitiveCollection, and returns it.
		 *
		 * @return a new int array
		 */
		default int[] toArray () {
			final int sz = size();
			int[] receiver = new int[sz];
			IntIterator it = iterator();
			int i = 0;
			while (it.hasNext())
				receiver[i++] = it.nextInt();
			return receiver;
		}

		/**
		 * Fills the given array with the entire contents of this PrimitiveCollection, up to
		 * {@link #size()} items, or if receiver is not large enough, then this allocates a new
		 * int array with {@link #size()} items and returns that.
		 *
		 * @param receiver an int array that will be filled with the items from this, if possible
		 * @return {@code receiver}, if it was modified, or a new int array otherwise
		 */
		default int[] toArray (int[] receiver) {
			final int sz = size();
			if (receiver.length < sz)
				receiver = new int[sz];
			IntIterator it = iterator();
			int i = 0;
			while (it.hasNext())
				receiver[i++] = it.nextInt();
			return receiver;
		}

		/**
		 * Performs the given action for each element of the {@code PrimitiveCollection.OfInt}
		 * until all elements have been processed or the action throws an
		 * exception.  Actions are performed in the order of iteration, if that
		 * order is specified.  Exceptions thrown by the action are relayed to the
		 * caller.
		 *
		 * @param action The action to be performed for each element
		 */
		default void forEach (IntConsumer action) {
			IntIterator it = iterator();
			while (it.hasNext())
				action.accept(it.nextInt());
		}

		/**
		 * Attempts to get the first item in this PrimitiveCollection, where "first" is only
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
		 * @return the first item in this PrimitiveCollection, as produced by {@link #iterator()}
		 * @throws IllegalStateException if this is empty
		 */
		default int first () {
			IntIterator it = iterator();
			if (it.hasNext())
				return it.nextInt();
			throw new IllegalStateException("Can't get the first() item of an empty PrimitiveCollection.");
		}

		/**
		 * Compares this PrimitiveCollection.OfInt with a PrimitiveSet.SetOfInt by checking their identity,
		 * their types (both must implement PrimitiveCollection.OfInt, and other must implement PrimitiveSet.SetOfInt),
		 * and their sizes, before checking if other contains each item in this PrimitiveCollection.OfInt, in any order
		 * or quantity. This is most useful for the key "set" or value collection in a primitive-backed map, since
		 * quantity doesn't matter for keys and order doesn't matter for either. Many implementations may need to reset
		 * the iterator on this PrimitiveCollection.OfInt, but that isn't necessary for {@code other}.
		 * <br>
		 * This is not meant for general object equality, but instead for equality following Set semantics. Classes that
		 * implement PrimitiveCollection that are not Set-like should usually not use this.
		 *
		 * @param other another Object that should be a PrimitiveSet.SetOfInt
		 * @return true if other is a PrimitiveSet.SetOfInt with exactly the same items, false otherwise
		 */
		default boolean equalContents (Object other) {
			if(this == other) return true;
			if(!(other instanceof PrimitiveSet.SetOfInt)) return false;
			PrimitiveSet.SetOfInt o = (PrimitiveSet.SetOfInt) other;
			if(size() != o.size()) return false;
			IntIterator it = iterator();
			while (it.hasNext()) {
				if(!o.contains(it.nextInt())) return false;
			}
			return true;
		}

		// STRING CONVERSION

		/**
		 * Delegates to {@link #toString(String, boolean)} with the given entrySeparator and without brackets.
		 *
		 * @param entrySeparator how to separate entries, such as {@code ", "}
		 * @return a new String representing this PrimitiveCollection
		 */
		default String toString (String entrySeparator) {
			return toString(entrySeparator, false);
		}

		/**
		 * Delegates to {@link #appendTo(StringBuilder, String, boolean)} using a new StringBuilder and converts it to
		 * a new String.
		 * @param entrySeparator how to separate entries, such as {@code ", "}
		 * @param brackets true to wrap the output in square brackets, or false to omit them
		 * @return a new String representing this PrimitiveCollection
		 */
		default String toString (String entrySeparator, boolean brackets) {
			return appendTo(new StringBuilder(32), entrySeparator, brackets).toString();
		}

		/**
		 * Makes a String from the contents of this PrimitiveCollection, but uses the given {@link IntAppender}
		 * to convert each item to a customizable representation and append them to a StringBuilder. To use
		 * the default String representation, you can use {@code StringBuilder::append} as an appender, or better yet,
		 * use {@link IntAppender#DEFAULT}, which caches the above method reference when Android won't do that.
		 *
		 * @param separator how to separate items, such as {@code ", "}
		 * @param brackets true to wrap the output in square brackets, or false to omit them
		 * @param appender a function that takes a StringBuilder and an int, and returns the modified StringBuilder
		 * @return a new String representing this PrimitiveCollection
		 */
		default String toString (String separator, boolean brackets,
			IntAppender appender){
			return appendTo(new StringBuilder(), separator, brackets, appender).toString();
		}

		/**
		 * Delegates to {@link #appendTo(StringBuilder, String, boolean, IntAppender)} using
		 * {@link StringBuilder#append(int)} to append int items.
		 * @param sb a StringBuilder that this can append to
		 * @param separator how to separate items, such as {@code ", "}
		 * @param brackets true to wrap the output in square brackets, or false to omit them
		 * @return {@code sb}, with the appended items of this PrimitiveCollection
		 */
		default StringBuilder appendTo (StringBuilder sb, String separator, boolean brackets) {
			return appendTo(sb, separator, brackets, IntAppender.DEFAULT);
		}

		/**
		 * Appends to a StringBuilder from the contents of this PrimitiveCollection, but uses the given {@link IntAppender}
		 * to convert each item to a customizable representation and append them to a StringBuilder. To use
		 * the default String representation, you can use {@code StringBuilder::append} as an appender, or better yet,
		 * use {@link IntAppender#DEFAULT}, which caches the above method reference when Android won't do that.
		 *
		 * @param sb a StringBuilder that this can append to
		 * @param separator how to separate items, such as {@code ", "}
		 * @param brackets true to wrap the output in square brackets, or false to omit them
		 * @param appender a function that takes a StringBuilder and an int, and returns the modified StringBuilder
		 * @return {@code sb}, with the appended items of this PrimitiveCollection
		 */
		default StringBuilder appendTo (StringBuilder sb, String separator, boolean brackets, IntAppender appender) {
			if (isEmpty()) return brackets ? sb.append("[]") : sb;
			if (brackets) sb.append('[');
			IntIterator it = iterator();
			if (it.hasNext()) {
				while (true) {
					appender.apply(sb, it.nextInt());
					if (it.hasNext()) sb.append(separator);
					else break;
				}
			}
			if (brackets) sb.append(']');
			return sb;
		}
	}

	/**
	 * A PrimitiveCollection with unboxed {@code long} items.
	 */
	interface OfLong extends PrimitiveCollection<Long> {
		boolean add (long item);

		boolean remove (long item);

		boolean contains (long item);

		default boolean addAll (OfLong other) {
			return addAll(other.iterator());
		}

		default boolean addAll (LongIterator it) {
			boolean changed = false;
			while (it.hasNext()) {
				changed |= add(it.nextLong());
			}
			return changed;
		}

		default boolean addAll (long[] array) {
			return addAll(array, 0, array.length);
		}

		default boolean addAll (long[] array, int offset, int length) {
			boolean changed = false;
			for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
				changed |= add(array[i]);
			}
			return changed;
		}

		/**
		 * Removes from this collection all occurrences of any elements contained in the specified other collection.
		 *
		 * @param other a primitive collection of long items to remove fully, such as a LongList or a LongSet
		 * @return true if this collection was modified.
		 */
		default boolean removeAll (OfLong other) {
			return removeAll(other.iterator());
		}
		/**
		 * Removes from this collection all occurrences of any elements contained in the specified LongIterator.
		 *
		 * @param it a LongIterator of items to remove fully
		 * @return true if this collection was modified.
		 */
		default boolean removeAll (LongIterator it) {
			LongIterator me;
			int originalSize = size();
			while (it.hasNext()) {
				long item = it.nextLong();
				me = iterator();
				while (me.hasNext()) {
					if (me.nextLong() == item) {
						me.remove();
					}
				}
			}
			return originalSize != size();
		}

		default boolean removeAll (long[] array) {
			return removeAll(array, 0, array.length);
		}

		default boolean removeAll (long[] array, int offset, int length) {
			LongIterator me;
			int originalSize = size();
			for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
				long item = array[i];
				me = iterator();
				while (me.hasNext()) {
					if (me.nextLong() == item) {
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
		 * @param other a primitive collection of long items to remove one-by-one, such as a LongList or a LongSet
		 * @return true if this collection was modified.
		 */
		default boolean removeEach (OfLong other) {
			return removeEach(other.iterator());
		}
		/**
		 * Removes from this collection element-wise occurrences of elements contained in the specified LongIterator.
		 * Note that if a value is present more than once in this collection, only one of those occurrences
		 * will be removed for each occurrence of that value in {@code it}. If {@code it} has the same
		 * contents as this collection or has additional items, then removing each of {@code it} will clear this.
		 *
		 * @param it a LongIterator of items to remove one-by-one
		 * @return true if this collection was modified.
		 */
		default boolean removeEach (LongIterator it) {
			boolean changed = false;
			while (it.hasNext()) {
				changed |= remove(it.nextLong());
			}
			return changed;
		}

		default boolean removeEach (long[] array) {
			return removeEach(array, 0, array.length);
		}

		default boolean removeEach (long[] array, int offset, int length) {
			boolean changed = false;
			for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
				changed |= remove(array[i]);
			}
			return changed;
		}

		default boolean containsAll (OfLong other) {
			return containsAll(other.iterator());
		}

		default boolean containsAll (LongIterator it) {
			while (it.hasNext()) {
				if(!contains(it.nextLong())) return false;
			}
			return true;
		}

		default boolean containsAll (long[] array) {
			return containsAll(array, 0, array.length);
		}

		default boolean containsAll (long[] array, int offset, int length) {
			for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
				if(!contains(array[i])) return false;
			}
			return true;
		}

		default boolean containsAny (OfLong other) {
			return containsAny(other.iterator());
		}

		default boolean containsAny (LongIterator it) {
			while (it.hasNext()) {
				if(contains(it.nextLong())) return true;
			}
			return false;
		}

		default boolean containsAny (long[] array) {
			return containsAny(array, 0, array.length);
		}

		default boolean containsAny (long[] array, int offset, int length) {
			for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
				if(contains(array[i])) return true;
			}
			return false;
		}

		/**
		 * Removes all the elements of this collection that satisfy the given
		 * predicate.  Errors or runtime exceptions thrown during iteration or by
		 * the predicate are relayed to the caller.
		 *
		 * @param filter a predicate which returns {@code true} for elements to be
		 *               removed
		 * @return {@code true} if any elements were removed
		 * @throws UnsupportedOperationException if elements cannot be removed
		 *                                       from this collection.  Implementations may throw this exception if a
		 *                                       matching element cannot be removed or if, in general, removal is not
		 *                                       supported.
		 * @implSpec The default implementation traverses all elements of the collection using
		 * its {@link #iterator()}.  Each matching element is removed using
		 * {@link Iterator#remove()}.  If the collection's iterator does not
		 * support removal then an {@code UnsupportedOperationException} will be
		 * thrown on the first matching element.
		 */
		default boolean removeIf (LongPredicate filter) {
			boolean removed = false;
			final LongIterator each = iterator();
			while (each.hasNext()) {
				if (filter.test(each.nextLong())) {
					each.remove();
					removed = true;
				}
			}
			return removed;
		}

		default boolean retainAll (OfLong other) {
			boolean changed = false;
			LongIterator it = iterator();
			while (it.hasNext()) {
				if (!other.contains(it.nextLong())) {
					it.remove();
					changed = true;
				}
			}
			return changed;
		}

		default boolean retainAll (long[] array) {
			return retainAll(array, 0, array.length);
		}

		default boolean retainAll (long[] array, int offset, int length) {
			boolean modified = false;
			LongIterator it = iterator();
			OUTER:
			while (it.hasNext()) {
				long check = it.next();
				for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
					if(array[i] == check)
						continue OUTER;
				}
				it.remove();
				modified = true;
			}
			return modified;
		}

		@Override
		LongIterator iterator ();

		/**
		 * Allocates a new long array with exactly {@link #size()} items, fills it with the
		 * contents of this PrimitiveCollection, and returns it.
		 *
		 * @return a new long array
		 */
		default long[] toArray () {
			final int sz = size();
			long[] receiver = new long[sz];
			LongIterator it = iterator();
			int i = 0;
			while (it.hasNext())
				receiver[i++] = it.nextLong();
			return receiver;
		}

		/**
		 * Fills the given array with the entire contents of this PrimitiveCollection, up to
		 * {@link #size()} items, or if receiver is not large enough, then this allocates a new
		 * long array with {@link #size()} items and returns that.
		 *
		 * @param receiver a long array that will be filled with the items from this, if possible
		 * @return {@code receiver}, if it was modified, or a new long array otherwise
		 */
		default long[] toArray (long[] receiver) {
			final int sz = size();
			if (receiver.length < sz)
				receiver = new long[sz];
			LongIterator it = iterator();
			int i = 0;
			while (it.hasNext())
				receiver[i++] = it.nextLong();
			return receiver;
		}

		/**
		 * Performs the given action for each element of the {@code PrimitiveCollection.OfLong}
		 * until all elements have been processed or the action throws an
		 * exception.  Actions are performed in the order of iteration, if that
		 * order is specified.  Exceptions thrown by the action are relayed to the
		 * caller.
		 *
		 * @param action The action to be performed for each element
		 */
		default void forEach (LongConsumer action) {
			LongIterator it = iterator();
			while (it.hasNext())
				action.accept(it.nextLong());
		}

		/**
		 * Attempts to get the first item in this PrimitiveCollection, where "first" is only
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
		 * @return the first item in this PrimitiveCollection, as produced by {@link #iterator()}
		 * @throws IllegalStateException if this is empty
		 */
		default long first () {
			LongIterator it = iterator();
			if (it.hasNext())
				return it.nextLong();
			throw new IllegalStateException("Can't get the first() item of an empty PrimitiveCollection.");
		}

		/**
		 * Compares this PrimitiveCollection.OfLong with a PrimitiveSet.SetOfLong by checking their identity,
		 * their types (both must implement PrimitiveCollection.OfLong, and other must implement PrimitiveSet.SetOfLong),
		 * and their sizes, before checking if other contains each item in this PrimitiveCollection.OfLong, in any order
		 * or quantity. This is most useful for the key "set" or value collection in a primitive-backed map, since
		 * quantity doesn't matter for keys and order doesn't matter for either. Many implementations may need to reset
		 * the iterator on this PrimitiveCollection.OfLong, but that isn't necessary for {@code other}.
		 * <br>
		 * This is not meant for general object equality, but instead for equality following Set semantics. Classes that
		 * implement PrimitiveCollection that are not Set-like should usually not use this.
		 *
		 * @param other another Object that should be a PrimitiveSet.SetOfLong
		 * @return true if other is a PrimitiveSet.SetOfLong with exactly the same items, false otherwise
		 */
		default boolean equalContents (Object other) {
			if(this == other) return true;
			if(!(other instanceof PrimitiveSet.SetOfLong)) return false;
			PrimitiveSet.SetOfLong o = (PrimitiveSet.SetOfLong) other;
			if(size() != o.size()) return false;
			LongIterator it = iterator();
			while (it.hasNext()) {
				if(!o.contains(it.nextLong())) return false;
			}
			return true;
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
		 * Makes a String from the contents of this PrimitiveCollection, but uses the given {@link LongAppender}
		 * to convert each item to a customizable representation and append them to a StringBuilder. To use
		 * the default String representation, you can use {@code StringBuilder::append} as an appender, or better yet,
		 * use {@link LongAppender#DEFAULT}, which caches the above method reference when Android won't do that.
		 *
		 * @param separator how to separate items, such as {@code ", "}
		 * @param brackets true to wrap the output in square brackets, or false to omit them
		 * @param appender a function that takes a StringBuilder and a long, and returns the modified StringBuilder
		 * @return a new String representing this PrimitiveCollection
		 */
		default String toString (String separator, boolean brackets,
			LongAppender appender){
			return appendTo(new StringBuilder(), separator, brackets, appender).toString();
		}

		default StringBuilder appendTo (StringBuilder sb, String separator, boolean brackets) {
			return appendTo(sb, separator, brackets, LongAppender.DEFAULT);
		}

		/**
		 * Appends to a StringBuilder from the contents of this PrimitiveCollection, but uses the given {@link LongAppender}
		 * to convert each item to a customizable representation and append them to a StringBuilder. To use
		 * the default String representation, you can use {@code StringBuilder::append} as an appender, or better yet,
		 * use {@link LongAppender#DEFAULT}, which caches the above method reference when Android won't do that.
		 *
		 * @param sb a StringBuilder that this can append to
		 * @param separator how to separate items, such as {@code ", "}
		 * @param brackets true to wrap the output in square brackets, or false to omit them
		 * @param appender a function that takes a StringBuilder and a long, and returns the modified StringBuilder
		 * @return {@code sb}, with the appended items of this PrimitiveCollection
		 */
		default StringBuilder appendTo (StringBuilder sb, String separator, boolean brackets, LongAppender appender) {
			if (isEmpty()) return brackets ? sb.append("[]") : sb;
			if (brackets) sb.append('[');
			LongIterator it = iterator();
			if (it.hasNext()) {
				while (true) {
					appender.apply(sb, it.nextLong());
					if (it.hasNext()) sb.append(separator);
					else break;
				}
			}
			if (brackets) sb.append(']');
			return sb;
		}
	}

	/**
	 * A PrimitiveCollection with unboxed {@code float} items.
	 */
	interface OfFloat extends PrimitiveCollection<Float> {
		boolean add (float item);

		boolean remove (float item);

		boolean contains (float item);

		default boolean addAll (OfFloat other) {
			return addAll(other.iterator());
		}

		default boolean addAll (FloatIterator it) {
			boolean changed = false;
			while (it.hasNext()) {
				changed |= add(it.nextFloat());
			}
			return changed;
		}

		default boolean addAll (float[] array) {
			return addAll(array, 0, array.length);
		}

		default boolean addAll (float[] array, int offset, int length) {
			boolean changed = false;
			for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
				changed |= add(array[i]);
			}
			return changed;
		}

		/**
		 * Removes from this collection all occurrences of any elements contained in the specified other collection.
		 *
		 * @param other a primitive collection of float items to remove fully, such as a FloatList or a FloatDeque
		 * @return true if this collection was modified.
		 */
		default boolean removeAll (OfFloat other) {
			return removeAll(other.iterator());
		}
		/**
		 * Removes from this collection all occurrences of any elements contained in the specified FloatIterator.
		 *
		 * @param it a FloatIterator of items to remove fully
		 * @return true if this collection was modified.
		 */
		default boolean removeAll (FloatIterator it) {
			FloatIterator me;
			int originalSize = size();
			while (it.hasNext()) {
				float item = it.nextFloat();
				me = iterator();
				while (me.hasNext()) {
					if (me.nextFloat() == item) {
						me.remove();
					}
				}
			}
			return originalSize != size();
		}

		default boolean removeAll (float[] array) {
			return removeAll(array, 0, array.length);
		}

		default boolean removeAll (float[] array, int offset, int length) {
			FloatIterator me;
			int originalSize = size();
			for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
				float item = array[i];
				me = iterator();
				while (me.hasNext()) {
					if (me.nextFloat() == item) {
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
		 * @param other a primitive collection of float items to remove one-by-one, such as a FloatList or a FloatDeque
		 * @return true if this collection was modified.
		 */
		default boolean removeEach (OfFloat other) {
			return removeEach(other.iterator());
		}
		/**
		 * Removes from this collection element-wise occurrences of elements contained in the specified FloatIterator.
		 * Note that if a value is present more than once in this collection, only one of those occurrences
		 * will be removed for each occurrence of that value in {@code it}. If {@code it} has the same
		 * contents as this collection or has additional items, then removing each of {@code it} will clear this.
		 *
		 * @param it a FloatIterator of items to remove one-by-one
		 * @return true if this collection was modified.
		 */
		default boolean removeEach (FloatIterator it) {
			boolean changed = false;
			while (it.hasNext()) {
				changed |= remove(it.nextFloat());
			}
			return changed;
		}

		default boolean removeEach (float[] array) {
			return removeEach(array, 0, array.length);
		}

		default boolean removeEach (float[] array, int offset, int length) {
			boolean changed = false;
			for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
				changed |= remove(array[i]);
			}
			return changed;
		}

		default boolean containsAll (OfFloat other) {
			return containsAll(other.iterator());
		}

		default boolean containsAll (FloatIterator it) {
			while (it.hasNext()) {
				if(!contains(it.nextFloat())) return false;
			}
			return true;
		}

		default boolean containsAll (float[] array) {
			return containsAll(array, 0, array.length);
		}

		default boolean containsAll (float[] array, int offset, int length) {
			for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
				if(!contains(array[i])) return false;
			}
			return true;
		}

		default boolean containsAny (OfFloat other) {
			return containsAny(other.iterator());
		}

		default boolean containsAny (FloatIterator it) {
			while (it.hasNext()) {
				if(contains(it.nextFloat())) return true;
			}
			return false;
		}

		default boolean containsAny (float[] array) {
			return containsAny(array, 0, array.length);
		}

		default boolean containsAny (float[] array, int offset, int length) {
			for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
				if(contains(array[i])) return true;
			}
			return false;
		}

		/**
		 * Removes all the elements of this collection that satisfy the given
		 * predicate.  Errors or runtime exceptions thrown during iteration or by
		 * the predicate are relayed to the caller.
		 *
		 * @param filter a predicate which returns {@code true} for elements to be
		 *               removed
		 * @return {@code true} if any elements were removed
		 * @throws UnsupportedOperationException if elements cannot be removed
		 *                                       from this collection.  Implementations may throw this exception if a
		 *                                       matching element cannot be removed or if, in general, removal is not
		 *                                       supported.
		 * @implSpec The default implementation traverses all elements of the collection using
		 * its {@link #iterator()}.  Each matching element is removed using
		 * {@link Iterator#remove()}.  If the collection's iterator does not
		 * support removal then an {@code UnsupportedOperationException} will be
		 * thrown on the first matching element.
		 */
		default boolean removeIf (FloatPredicate filter) {
			boolean removed = false;
			final FloatIterator each = iterator();
			while (each.hasNext()) {
				if (filter.test(each.nextFloat())) {
					each.remove();
					removed = true;
				}
			}
			return removed;
		}

		default boolean retainAll (OfFloat other) {
			boolean changed = false;
			FloatIterator it = iterator();
			while (it.hasNext()) {
				if (!other.contains(it.nextFloat())) {
					it.remove();
					changed = true;
				}
			}
			return changed;
		}

		default boolean retainAll (float[] array) {
			return retainAll(array, 0, array.length);
		}

		default boolean retainAll (float[] array, int offset, int length) {
			boolean modified = false;
			FloatIterator it = iterator();
			OUTER:
			while (it.hasNext()) {
				float check = it.next();
				for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
					if(array[i] == check)
						continue OUTER;
				}
				it.remove();
				modified = true;
			}
			return modified;
		}

		@Override
		FloatIterator iterator ();

		/**
		 * Allocates a new float array with exactly {@link #size()} items, fills it with the
		 * contents of this PrimitiveCollection, and returns it.
		 *
		 * @return a new float array
		 */
		default float[] toArray () {
			final int sz = size();
			float[] receiver = new float[sz];
			FloatIterator it = iterator();
			int i = 0;
			while (it.hasNext())
				receiver[i++] = it.nextFloat();
			return receiver;
		}

		/**
		 * Fills the given array with the entire contents of this PrimitiveCollection, up to
		 * {@link #size()} items, or if receiver is not large enough, then this allocates a new
		 * float array with {@link #size()} items and returns that.
		 *
		 * @param receiver a float array that will be filled with the items from this, if possible
		 * @return {@code receiver}, if it was modified, or a new float array otherwise
		 */
		default float[] toArray (float[] receiver) {
			final int sz = size();
			if (receiver.length < sz)
				receiver = new float[sz];
			FloatIterator it = iterator();
			int i = 0;
			while (it.hasNext())
				receiver[i++] = it.nextFloat();
			return receiver;
		}

		/**
		 * Performs the given action for each element of the {@code PrimitiveCollection.OfFloat}
		 * until all elements have been processed or the action throws an
		 * exception.  Actions are performed in the order of iteration, if that
		 * order is specified.  Exceptions thrown by the action are relayed to the
		 * caller.
		 *
		 * @param action The action to be performed for each element
		 */
		default void forEach (FloatConsumer action) {
			FloatIterator it = iterator();
			while (it.hasNext())
				action.accept(it.nextFloat());
		}

		/**
		 * Attempts to get the first item in this PrimitiveCollection, where "first" is only
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
		 * @return the first item in this PrimitiveCollection, as produced by {@link #iterator()}
		 * @throws IllegalStateException if this is empty
		 */
		default float first () {
			FloatIterator it = iterator();
			if (it.hasNext())
				return it.nextFloat();
			throw new IllegalStateException("Can't get the first() item of an empty PrimitiveCollection.");
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
		 * Makes a String from the contents of this PrimitiveCollection, but uses the given {@link FloatAppender}
		 * to convert each item to a customizable representation and append them to a StringBuilder. To use
		 * the default String representation, you can use {@code StringBuilder::append} as an appender.
		 *
		 * @param separator how to separate items, such as {@code ", "}
		 * @param brackets true to wrap the output in square brackets, or false to omit them
		 * @param appender a function that takes a StringBuilder and a float, and returns the modified StringBuilder
		 * @return a new String representing this PrimitiveCollection
		 */
		default String toString (String separator, boolean brackets,
			FloatAppender appender){
			return appendTo(new StringBuilder(), separator, brackets, appender).toString();
		}

		default StringBuilder appendTo (StringBuilder sb, String separator, boolean brackets) {
			return appendTo(sb, separator, brackets, StringBuilder::append);
		}

		/**
		 * Appends to a StringBuilder from the contents of this PrimitiveCollection, but uses the given {@link FloatAppender}
		 * to convert each item to a customizable representation and append them to a StringBuilder. To use
		 * the default String representation, you can use {@code StringBuilder::append} as an appender.
		 *
		 * @param sb a StringBuilder that this can append to
		 * @param separator how to separate items, such as {@code ", "}
		 * @param brackets true to wrap the output in square brackets, or false to omit them
		 * @param appender a function that takes a StringBuilder and a float, and returns the modified StringBuilder
		 * @return {@code sb}, with the appended items of this PrimitiveCollection
		 */
		default StringBuilder appendTo (StringBuilder sb, String separator, boolean brackets, FloatAppender appender) {
			if (isEmpty()) return brackets ? sb.append("[]") : sb;
			if (brackets) sb.append('[');
			FloatIterator it = iterator();
			if (it.hasNext()) {
				while (true) {
					appender.apply(sb, it.nextFloat());
					if (it.hasNext()) sb.append(separator);
					else break;
				}
			}
			if (brackets) sb.append(']');
			return sb;
		}
	}

	/**
	 * A PrimitiveCollection with unboxed {@code double} items.
	 */
	interface OfDouble extends PrimitiveCollection<Double> {
		boolean add (double item);

		boolean remove (double item);

		boolean contains (double item);

		default boolean addAll (OfDouble other) {
			return addAll(other.iterator());
		}

		default boolean addAll (DoubleIterator it) {
			boolean changed = false;
			while (it.hasNext()) {
				changed |= add(it.nextDouble());
			}
			return changed;
		}

		default boolean addAll (double[] array) {
			return addAll(array, 0, array.length);
		}

		default boolean addAll (double[] array, int offset, int length) {
			boolean changed = false;
			for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
				changed |= add(array[i]);
			}
			return changed;
		}

		/**
		 * Removes from this collection all occurrences of any elements contained in the specified other collection.
		 *
		 * @param other a primitive collection of double items to remove fully, such as a DoubleList or a DoubleDeque
		 * @return true if this collection was modified.
		 */
		default boolean removeAll (OfDouble other) {
			return removeAll(other.iterator());
		}
		/**
		 * Removes from this collection all occurrences of any elements contained in the specified DoubleIterator.
		 *
		 * @param it a DoubleIterator of items to remove fully
		 * @return true if this collection was modified.
		 */
		default boolean removeAll (DoubleIterator it) {
			DoubleIterator me;
			int originalSize = size();
			while (it.hasNext()) {
				double item = it.nextDouble();
				me = iterator();
				while (me.hasNext()) {
					if (me.nextDouble() == item) {
						me.remove();
					}
				}
			}
			return originalSize != size();
		}

		default boolean removeAll (double[] array) {
			return removeAll(array, 0, array.length);
		}

		default boolean removeAll (double[] array, int offset, int length) {
			DoubleIterator me;
			int originalSize = size();
			for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
				double item = array[i];
				me = iterator();
				while (me.hasNext()) {
					if (me.nextDouble() == item) {
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
		 * @param other a primitive collection of double items to remove one-by-one, such as a DoubleList or a DoubleSet
		 * @return true if this collection was modified.
		 */
		default boolean removeEach (OfDouble other) {
			return removeEach(other.iterator());
		}
		/**
		 * Removes from this collection element-wise occurrences of elements contained in the specified DoubleIterator.
		 * Note that if a value is present more than once in this collection, only one of those occurrences
		 * will be removed for each occurrence of that value in {@code it}. If {@code it} has the same
		 * contents as this collection or has additional items, then removing each of {@code it} will clear this.
		 *
		 * @param it a DoubleIterator of items to remove one-by-one
		 * @return true if this collection was modified.
		 */
		default boolean removeEach (DoubleIterator it) {
			boolean changed = false;
			while (it.hasNext()) {
				changed |= remove(it.nextDouble());
			}
			return changed;
		}

		default boolean removeEach (double[] array) {
			return removeEach(array, 0, array.length);
		}

		default boolean removeEach (double[] array, int offset, int length) {
			boolean changed = false;
			for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
				changed |= remove(array[i]);
			}
			return changed;
		}

		default boolean containsAll (OfDouble other) {
			return containsAll(other.iterator());
		}

		default boolean containsAll (DoubleIterator it) {
			while (it.hasNext()) {
				if(!contains(it.nextDouble())) return false;
			}
			return true;
		}

		default boolean containsAll (double[] array) {
			return containsAll(array, 0, array.length);
		}

		default boolean containsAll (double[] array, int offset, int length) {
			for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
				if(!contains(array[i])) return false;
			}
			return true;
		}

		default boolean containsAny (OfDouble other) {
			return containsAny(other.iterator());
		}

		default boolean containsAny (DoubleIterator it) {
			while (it.hasNext()) {
				if(contains(it.nextDouble())) return true;
			}
			return false;
		}

		default boolean containsAny (double[] array) {
			return containsAny(array, 0, array.length);
		}

		default boolean containsAny (double[] array, int offset, int length) {
			for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
				if(contains(array[i])) return true;
			}
			return false;
		}

		/**
		 * Removes all the elements of this collection that satisfy the given
		 * predicate.  Errors or runtime exceptions thrown during iteration or by
		 * the predicate are relayed to the caller.
		 *
		 * @param filter a predicate which returns {@code true} for elements to be
		 *               removed
		 * @return {@code true} if any elements were removed
		 * @throws UnsupportedOperationException if elements cannot be removed
		 *                                       from this collection.  Implementations may throw this exception if a
		 *                                       matching element cannot be removed or if, in general, removal is not
		 *                                       supported.
		 * @implSpec The default implementation traverses all elements of the collection using
		 * its {@link #iterator()}.  Each matching element is removed using
		 * {@link Iterator#remove()}.  If the collection's iterator does not
		 * support removal then an {@code UnsupportedOperationException} will be
		 * thrown on the first matching element.
		 */
		default boolean removeIf (DoublePredicate filter) {
			boolean removed = false;
			final DoubleIterator each = iterator();
			while (each.hasNext()) {
				if (filter.test(each.nextDouble())) {
					each.remove();
					removed = true;
				}
			}
			return removed;
		}

		default boolean retainAll (OfDouble other) {
			boolean changed = false;
			DoubleIterator it = iterator();
			while (it.hasNext()) {
				if (!other.contains(it.nextDouble())) {
					it.remove();
					changed = true;
				}
			}
			return changed;
		}

		default boolean retainAll (double[] array) {
			return retainAll(array, 0, array.length);
		}

		default boolean retainAll (double[] array, int offset, int length) {
			boolean modified = false;
			DoubleIterator it = iterator();
			OUTER:
			while (it.hasNext()) {
				double check = it.next();
				for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
					if(array[i] == check)
						continue OUTER;
				}
				it.remove();
				modified = true;
			}
			return modified;
		}

		@Override
		DoubleIterator iterator ();

		/**
		 * Allocates a new double array with exactly {@link #size()} items, fills it with the
		 * contents of this PrimitiveCollection, and returns it.
		 *
		 * @return a new double array
		 */
		default double[] toArray () {
			final int sz = size();
			double[] receiver = new double[sz];
			DoubleIterator it = iterator();
			int i = 0;
			while (it.hasNext())
				receiver[i++] = it.nextDouble();
			return receiver;
		}

		/**
		 * Fills the given array with the entire contents of this PrimitiveCollection, up to
		 * {@link #size()} items, or if receiver is not large enough, then this allocates a new
		 * double array with {@link #size()} items and returns that.
		 *
		 * @param receiver a double array that will be filled with the items from this, if possible
		 * @return {@code receiver}, if it was modified, or a new double array otherwise
		 */
		default double[] toArray (double[] receiver) {
			final int sz = size();
			if (receiver.length < sz)
				receiver = new double[sz];
			DoubleIterator it = iterator();
			int i = 0;
			while (it.hasNext())
				receiver[i++] = it.nextDouble();
			return receiver;
		}

		/**
		 * Performs the given action for each element of the {@code PrimitiveCollection.OfDouble}
		 * until all elements have been processed or the action throws an
		 * exception.  Actions are performed in the order of iteration, if that
		 * order is specified.  Exceptions thrown by the action are relayed to the
		 * caller.
		 *
		 * @param action The action to be performed for each element
		 */
		default void forEach (DoubleConsumer action) {
			DoubleIterator it = iterator();
			while (it.hasNext())
				action.accept(it.nextDouble());
		}

		/**
		 * Attempts to get the first item in this PrimitiveCollection, where "first" is only
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
		 * @return the first item in this PrimitiveCollection, as produced by {@link #iterator()}
		 * @throws IllegalStateException if this is empty
		 */
		default double first () {
			DoubleIterator it = iterator();
			if (it.hasNext())
				return it.nextDouble();
			throw new IllegalStateException("Can't get the first() item of an empty PrimitiveCollection.");
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
		 * Makes a String from the contents of this PrimitiveCollection, but uses the given {@link DoubleAppender}
		 * to convert each item to a customizable representation and append them to a StringBuilder. To use
		 * the default String representation, you can use {@code StringBuilder::append} as an appender.
		 *
		 * @param separator how to separate items, such as {@code ", "}
		 * @param brackets true to wrap the output in square brackets, or false to omit them
		 * @param appender a function that takes a StringBuilder and a double, and returns the modified StringBuilder
		 * @return a new String representing this PrimitiveCollection
		 */
		default String toString (String separator, boolean brackets,
			DoubleAppender appender){
			return appendTo(new StringBuilder(), separator, brackets, appender).toString();
		}

		default StringBuilder appendTo (StringBuilder sb, String separator, boolean brackets) {
			return appendTo(sb, separator, brackets, StringBuilder::append);
		}

		/**
		 * Appends to a StringBuilder from the contents of this PrimitiveCollection, but uses the given {@link DoubleAppender}
		 * to convert each item to a customizable representation and append them to a StringBuilder. To use
		 * the default String representation, you can use {@code StringBuilder::append} as an appender.
		 *
		 * @param sb a StringBuilder that this can append to
		 * @param separator how to separate items, such as {@code ", "}
		 * @param brackets true to wrap the output in square brackets, or false to omit them
		 * @param appender a function that takes a StringBuilder and a double, and returns the modified StringBuilder
		 * @return {@code sb}, with the appended items of this PrimitiveCollection
		 */
		default StringBuilder appendTo (StringBuilder sb, String separator, boolean brackets, DoubleAppender appender) {
			if (isEmpty()) return brackets ? sb.append("[]") : sb;
			if (brackets) sb.append('[');
			DoubleIterator it = iterator();
			if (it.hasNext()) {
				while (true) {
					appender.apply(sb, it.nextDouble());
					if (it.hasNext()) sb.append(separator);
					else break;
				}
			}
			if (brackets) sb.append(']');
			return sb;
		}
	}

	/**
	 * A PrimitiveCollection with unboxed {@code short} items.
	 */
	interface OfShort extends PrimitiveCollection<Short> {
		boolean add (short item);

		boolean remove (short item);

		boolean contains (short item);

		default boolean addAll (OfShort other) {
			return addAll(other.iterator());
		}
		
		default boolean addAll (ShortIterator it) {
			boolean changed = false;
			while (it.hasNext()) {
				changed |= add(it.nextShort());
			}
			return changed;
		}

		default boolean addAll (short[] array) {
			return addAll(array, 0, array.length);
		}

		default boolean addAll (short[] array, int offset, int length) {
			boolean changed = false;
			for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
				changed |= add(array[i]);
			}
			return changed;
		}

		/**
		 * Removes from this collection all occurrences of any elements contained in the specified other collection.
		 *
		 * @param other a primitive collection of short items to remove fully, such as a ShortList or a ShortDeque
		 * @return true if this collection was modified.
		 */
		default boolean removeAll (OfShort other) {
			return removeAll(other.iterator());
		}
		/**
		 * Removes from this collection all occurrences of any elements contained in the specified ShortIterator.
		 *
		 * @param it a ShortIterator of items to remove fully
		 * @return true if this collection was modified.
		 */
		default boolean removeAll (ShortIterator it) {
			ShortIterator me;
			int originalSize = size();
			while (it.hasNext()) {
				short item = it.nextShort();
				me = iterator();
				while (me.hasNext()) {
					if (me.nextShort() == item) {
						me.remove();
					}
				}
			}
			return originalSize != size();
		}

		default boolean removeAll (short[] array) {
			return removeAll(array, 0, array.length);
		}

		default boolean removeAll (short[] array, int offset, int length) {
			ShortIterator me;
			int originalSize = size();
			for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
				short item = array[i];
				me = iterator();
				while (me.hasNext()) {
					if (me.nextShort() == item) {
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
		 * @param other a primitive collection of short items to remove one-by-one, such as a ShortList or a ShortDeque
		 * @return true if this collection was modified.
		 */
		default boolean removeEach (OfShort other) {
			return removeEach(other.iterator());
		}
		/**
		 * Removes from this collection element-wise occurrences of elements contained in the specified ShortIterator.
		 * Note that if a value is present more than once in this collection, only one of those occurrences
		 * will be removed for each occurrence of that value in {@code it}. If {@code it} has the same
		 * contents as this collection or has additional items, then removing each of {@code it} will clear this.
		 *
		 * @param it a ShortIterator of items to remove one-by-one
		 * @return true if this collection was modified.
		 */
		default boolean removeEach (ShortIterator it) {
			boolean changed = false;
			while (it.hasNext()) {
				changed |= remove(it.nextShort());
			}
			return changed;
		}

		default boolean removeEach (short[] array) {
			return removeEach(array, 0, array.length);
		}

		default boolean removeEach (short[] array, int offset, int length) {
			boolean changed = false;
			for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
				changed |= remove(array[i]);
			}
			return changed;
		}

		default boolean containsAll (OfShort other) {
			return containsAll(other.iterator());
		}

		default boolean containsAll (ShortIterator it) {
			while (it.hasNext()) {
				if(!contains(it.nextShort())) return false;
			}
			return true;
		}

		default boolean containsAll (short[] array) {
			return containsAll(array, 0, array.length);
		}

		default boolean containsAll (short[] array, int offset, int length) {
			for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
				if(!contains(array[i])) return false;
			}
			return true;
		}

		default boolean containsAny (OfShort other) {
			return containsAny(other.iterator());
		}

		default boolean containsAny (ShortIterator it) {
			while (it.hasNext()) {
				if(contains(it.nextShort())) return true;
			}
			return false;
		}

		default boolean containsAny (short[] array) {
			return containsAny(array, 0, array.length);
		}

		default boolean containsAny (short[] array, int offset, int length) {
			for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
				if(contains(array[i])) return true;
			}
			return false;
		}

		/**
		 * Removes all the elements of this collection that satisfy the given
		 * predicate.  Errors or runtime exceptions thrown during iteration or by
		 * the predicate are relayed to the caller.
		 *
		 * @param filter a predicate which returns {@code true} for elements to be
		 *               removed
		 * @return {@code true} if any elements were removed
		 * @throws UnsupportedOperationException if elements cannot be removed
		 *                                       from this collection.  Implementations may throw this exception if a
		 *                                       matching element cannot be removed or if, in general, removal is not
		 *                                       supported.
		 * @implSpec The default implementation traverses all elements of the collection using
		 * its {@link #iterator()}.  Each matching element is removed using
		 * {@link Iterator#remove()}.  If the collection's iterator does not
		 * support removal then an {@code UnsupportedOperationException} will be
		 * thrown on the first matching element.
		 */
		default boolean removeIf (ShortPredicate filter) {
			boolean removed = false;
			final ShortIterator each = iterator();
			while (each.hasNext()) {
				if (filter.test(each.nextShort())) {
					each.remove();
					removed = true;
				}
			}
			return removed;
		}

		default boolean retainAll (OfShort other) {
			boolean changed = false;
			ShortIterator it = iterator();
			while (it.hasNext()) {
				if (!other.contains(it.nextShort())) {
					it.remove();
					changed = true;
				}
			}
			return changed;
		}

		default boolean retainAll (short[] array) {
			return retainAll(array, 0, array.length);
		}

		default boolean retainAll (short[] array, int offset, int length) {
			boolean modified = false;
			ShortIterator it = iterator();
			OUTER:
			while (it.hasNext()) {
				short check = it.next();
				for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
					if(array[i] == check)
						continue OUTER;
				}
				it.remove();
				modified = true;
			}
			return modified;
		}

		@Override
		ShortIterator iterator ();

		/**
		 * Allocates a new short array with exactly {@link #size()} items, fills it with the
		 * contents of this PrimitiveCollection, and returns it.
		 *
		 * @return a new short array
		 */
		default short[] toArray () {
			final int sz = size();
			short[] receiver = new short[sz];
			ShortIterator it = iterator();
			int i = 0;
			while (it.hasNext())
				receiver[i++] = it.nextShort();
			return receiver;
		}

		/**
		 * Fills the given array with the entire contents of this PrimitiveCollection, up to
		 * {@link #size()} items, or if receiver is not large enough, then this allocates a new
		 * short array with {@link #size()} items and returns that.
		 *
		 * @param receiver a short array that will be filled with the items from this, if possible
		 * @return {@code receiver}, if it was modified, or a new short array otherwise
		 */
		default short[] toArray (short[] receiver) {
			final int sz = size();
			if (receiver.length < sz)
				receiver = new short[sz];
			ShortIterator it = iterator();
			int i = 0;
			while (it.hasNext())
				receiver[i++] = it.nextShort();
			return receiver;
		}

		/**
		 * Performs the given action for each element of the {@code PrimitiveCollection.OfShort}
		 * until all elements have been processed or the action throws an
		 * exception.  Actions are performed in the order of iteration, if that
		 * order is specified.  Exceptions thrown by the action are relayed to the
		 * caller.
		 *
		 * @param action The action to be performed for each element
		 */
		default void forEach (ShortConsumer action) {
			ShortIterator it = iterator();
			while (it.hasNext())
				action.accept(it.nextShort());
		}

		/**
		 * Attempts to get the first item in this PrimitiveCollection, where "first" is only
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
		 * @return the first item in this PrimitiveCollection, as produced by {@link #iterator()}
		 * @throws IllegalStateException if this is empty
		 */
		default short first () {
			ShortIterator it = iterator();
			if (it.hasNext())
				return it.nextShort();
			throw new IllegalStateException("Can't get the first() item of an empty PrimitiveCollection.");
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
		 * Makes a String from the contents of this PrimitiveCollection, but uses the given {@link ShortAppender}
		 * to convert each item to a customizable representation and append them to a StringBuilder. To use
		 * the default String representation, you can use {@code StringBuilder::append} as an appender, or better yet,
		 * use {@link ShortAppender#DEFAULT}, which caches the above method reference when Android won't do that.
		 *
		 * @param separator how to separate items, such as {@code ", "}
		 * @param brackets true to wrap the output in square brackets, or false to omit them
		 * @param appender a function that takes a StringBuilder and a short, and returns the modified StringBuilder
		 * @return a new String representing this PrimitiveCollection
		 */
		default String toString (String separator, boolean brackets,
			ShortAppender appender){
			return appendTo(new StringBuilder(), separator, brackets, appender).toString();
		}

		default StringBuilder appendTo (StringBuilder sb, String separator, boolean brackets) {
			return appendTo(sb, separator, brackets, ShortAppender.DEFAULT);
		}

		/**
		 * Appends to a StringBuilder from the contents of this PrimitiveCollection, but uses the given {@link ShortAppender}
		 * to convert each item to a customizable representation and append them to a StringBuilder. To use
		 * the default String representation, you can use {@code StringBuilder::append} as an appender, or better yet,
		 * use {@link ShortAppender#DEFAULT}, which caches the above method reference when Android won't do that.
		 *
		 * @param sb a StringBuilder that this can append to
		 * @param separator how to separate items, such as {@code ", "}
		 * @param brackets true to wrap the output in square brackets, or false to omit them
		 * @param appender a function that takes a StringBuilder and a short, and returns the modified StringBuilder
		 * @return {@code sb}, with the appended items of this PrimitiveCollection
		 */
		default StringBuilder appendTo (StringBuilder sb, String separator, boolean brackets, ShortAppender appender) {
			if (isEmpty()) return brackets ? sb.append("[]") : sb;
			if (brackets) sb.append('[');
			ShortIterator it = iterator();
			if (it.hasNext()) {
				while (true) {
					appender.apply(sb, it.nextShort());
					if (it.hasNext()) sb.append(separator);
					else break;
				}
			}
			if (brackets) sb.append(']');
			return sb;
		}
	}

	/**
	 * A PrimitiveCollection with unboxed {@code byte} items.
	 */
	interface OfByte extends PrimitiveCollection<Byte> {
		boolean add (byte item);

		boolean remove (byte item);

		boolean contains (byte item);

		default boolean addAll (OfByte other) {
			return addAll(other.iterator());
		}
		
		default boolean addAll (ByteIterator it) {
			boolean changed = false;
			while (it.hasNext()) {
				changed |= add(it.nextByte());
			}
			return changed;
		}

		default boolean addAll (byte[] array) {
			return addAll(array, 0, array.length);
		}

		default boolean addAll (byte[] array, int offset, int length) {
			boolean changed = false;
			for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
				changed |= add(array[i]);
			}
			return changed;
		}

		/**
		 * Removes from this collection all occurrences of any elements contained in the specified other collection.
		 *
		 * @param other a primitive collection of byte items to remove fully, such as a ByteList or a ByteDeque
		 * @return true if this collection was modified.
		 */
		default boolean removeAll (OfByte other) {
			return removeAll(other.iterator());
		}
		/**
		 * Removes from this collection all occurrences of any elements contained in the specified ByteIterator.
		 *
		 * @param it a ByteIterator of items to remove fully
		 * @return true if this collection was modified.
		 */
		default boolean removeAll (ByteIterator it) {
			ByteIterator me;
			int originalSize = size();
			while (it.hasNext()) {
				byte item = it.nextByte();
				me = iterator();
				while (me.hasNext()) {
					if (me.nextByte() == item) {
						me.remove();
					}
				}
			}
			return originalSize != size();
		}

		default boolean removeAll (byte[] array) {
			return removeAll(array, 0, array.length);
		}

		default boolean removeAll (byte[] array, int offset, int length) {
			ByteIterator me;
			int originalSize = size();
			for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
				byte item = array[i];
				me = iterator();
				while (me.hasNext()) {
					if (me.nextByte() == item) {
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
		 * @param other a primitive collection of byte items to remove one-by-one, such as a ByteList or a ByteDeque
		 * @return true if this collection was modified.
		 */
		default boolean removeEach (OfByte other) {
			return removeEach(other.iterator());
		}
		/**
		 * Removes from this collection element-wise occurrences of elements contained in the specified ByteIterator.
		 * Note that if a value is present more than once in this collection, only one of those occurrences
		 * will be removed for each occurrence of that value in {@code it}. If {@code it} has the same
		 * contents as this collection or has additional items, then removing each of {@code it} will clear this.
		 *
		 * @param it a ByteIterator of items to remove one-by-one
		 * @return true if this collection was modified.
		 */
		default boolean removeEach (ByteIterator it) {
			boolean changed = false;
			while (it.hasNext()) {
				changed |= remove(it.nextByte());
			}
			return changed;
		}

		default boolean removeEach (byte[] array) {
			return removeEach(array, 0, array.length);
		}

		default boolean removeEach (byte[] array, int offset, int length) {
			boolean changed = false;
			for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
				changed |= remove(array[i]);
			}
			return changed;
		}

		default boolean containsAll (OfByte other) {
			return containsAll(other.iterator());
		}

		default boolean containsAll (ByteIterator it) {
			while (it.hasNext()) {
				if(!contains(it.nextByte())) return false;
			}
			return true;
		}

		default boolean containsAll (byte[] array) {
			return containsAll(array, 0, array.length);
		}

		default boolean containsAll (byte[] array, int offset, int length) {
			for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
				if(!contains(array[i])) return false;
			}
			return true;
		}

		default boolean containsAny (OfByte other) {
			return containsAny(other.iterator());
		}

		default boolean containsAny (ByteIterator it) {
			while (it.hasNext()) {
				if(contains(it.nextByte())) return true;
			}
			return false;
		}

		default boolean containsAny (byte[] array) {
			return containsAny(array, 0, array.length);
		}

		default boolean containsAny (byte[] array, int offset, int length) {
			for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
				if(contains(array[i])) return true;
			}
			return false;
		}

		/**
		 * Removes all the elements of this collection that satisfy the given
		 * predicate.  Errors or runtime exceptions thrown during iteration or by
		 * the predicate are relayed to the caller.
		 *
		 * @param filter a predicate which returns {@code true} for elements to be
		 *               removed
		 * @return {@code true} if any elements were removed
		 * @throws UnsupportedOperationException if elements cannot be removed
		 *                                       from this collection.  Implementations may throw this exception if a
		 *                                       matching element cannot be removed or if, in general, removal is not
		 *                                       supported.
		 * @implSpec The default implementation traverses all elements of the collection using
		 * its {@link #iterator()}.  Each matching element is removed using
		 * {@link Iterator#remove()}.  If the collection's iterator does not
		 * support removal then an {@code UnsupportedOperationException} will be
		 * thrown on the first matching element.
		 */
		default boolean removeIf (BytePredicate filter) {
			boolean removed = false;
			final ByteIterator each = iterator();
			while (each.hasNext()) {
				if (filter.test(each.nextByte())) {
					each.remove();
					removed = true;
				}
			}
			return removed;
		}

		default boolean retainAll (OfByte other) {
			boolean changed = false;
			ByteIterator it = iterator();
			while (it.hasNext()) {
				if (!other.contains(it.nextByte())) {
					it.remove();
					changed = true;
				}
			}
			return changed;
		}

		default boolean retainAll (byte[] array) {
			return retainAll(array, 0, array.length);
		}

		default boolean retainAll (byte[] array, int offset, int length) {
			boolean modified = false;
			ByteIterator it = iterator();
			OUTER:
			while (it.hasNext()) {
				byte check = it.next();
				for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
					if(array[i] == check)
						continue OUTER;
				}
				it.remove();
				modified = true;
			}
			return modified;
		}

		@Override
		ByteIterator iterator ();

		/**
		 * Allocates a new byte array with exactly {@link #size()} items, fills it with the
		 * contents of this PrimitiveCollection, and returns it.
		 *
		 * @return a new byte array
		 */
		default byte[] toArray () {
			final int sz = size();
			byte[] receiver = new byte[sz];
			ByteIterator it = iterator();
			int i = 0;
			while (it.hasNext())
				receiver[i++] = it.nextByte();
			return receiver;
		}

		/**
		 * Fills the given array with the entire contents of this PrimitiveCollection, up to
		 * {@link #size()} items, or if receiver is not large enough, then this allocates a new
		 * byte array with {@link #size()} items and returns that.
		 *
		 * @param receiver a byte array that will be filled with the items from this, if possible
		 * @return {@code receiver}, if it was modified, or a new byte array otherwise
		 */
		default byte[] toArray (byte[] receiver) {
			final int sz = size();
			if (receiver.length < sz)
				receiver = new byte[sz];
			ByteIterator it = iterator();
			int i = 0;
			while (it.hasNext())
				receiver[i++] = it.nextByte();
			return receiver;
		}

		/**
		 * Performs the given action for each element of the {@code PrimitiveCollection.OfByte}
		 * until all elements have been processed or the action throws an
		 * exception.  Actions are performed in the order of iteration, if that
		 * order is specified.  Exceptions thrown by the action are relayed to the
		 * caller.
		 *
		 * @param action The action to be performed for each element
		 */
		default void forEach (ByteConsumer action) {
			ByteIterator it = iterator();
			while (it.hasNext())
				action.accept(it.nextByte());
		}

		/**
		 * Attempts to get the first item in this PrimitiveCollection, where "first" is only
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
		 * @return the first item in this PrimitiveCollection, as produced by {@link #iterator()}
		 * @throws IllegalStateException if this is empty
		 */
		default byte first () {
			ByteIterator it = iterator();
			if (it.hasNext())
				return it.nextByte();
			throw new IllegalStateException("Can't get the first() item of an empty PrimitiveCollection.");
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
		 * Makes a String from the contents of this PrimitiveCollection, but uses the given {@link ByteAppender}
		 * to convert each item to a customizable representation and append them to a StringBuilder. To use
		 * the default String representation, you can use {@code StringBuilder::append} as an appender, or better yet,
		 * use {@link ByteAppender#DEFAULT}, which caches the above method reference when Android won't do that.
		 *
		 * @param separator how to separate items, such as {@code ", "}
		 * @param brackets true to wrap the output in square brackets, or false to omit them
		 * @param appender a function that takes a StringBuilder and a byte, and returns the modified StringBuilder
		 * @return a new String representing this PrimitiveCollection
		 */
		default String toString (String separator, boolean brackets,
			ByteAppender appender){
			return appendTo(new StringBuilder(), separator, brackets, appender).toString();
		}

		default StringBuilder appendTo (StringBuilder sb, String separator, boolean brackets) {
			return appendTo(sb, separator, brackets, ByteAppender.DEFAULT);
		}

		/**
		 * Appends to a StringBuilder from the contents of this PrimitiveCollection, but uses the given {@link ByteAppender}
		 * to convert each item to a customizable representation and append them to a StringBuilder. To use
		 * the default String representation, you can use {@code StringBuilder::append} as an appender, or better yet,
		 * use {@link ByteAppender#DEFAULT}, which caches the above method reference when Android won't do that.
		 *
		 * @param sb a StringBuilder that this can append to
		 * @param separator how to separate items, such as {@code ", "}
		 * @param brackets true to wrap the output in square brackets, or false to omit them
		 * @param appender a function that takes a StringBuilder and a byte, and returns the modified StringBuilder
		 * @return {@code sb}, with the appended items of this PrimitiveCollection
		 */
		default StringBuilder appendTo (StringBuilder sb, String separator, boolean brackets, ByteAppender appender) {
			if (isEmpty()) return brackets ? sb.append("[]") : sb;
			if (brackets) sb.append('[');
			ByteIterator it = iterator();
			if (it.hasNext()) {
				while (true) {
					appender.apply(sb, it.nextByte());
					if (it.hasNext()) sb.append(separator);
					else break;
				}
			}
			if (brackets) sb.append(']');
			return sb;
		}
	}

	/**
	 * A PrimitiveCollection with unboxed {@code char} items.
	 */
	interface OfChar extends PrimitiveCollection<Character> {
		boolean add (char item);

		boolean remove (char item);

		boolean contains (char item);

		default boolean addAll (OfChar other) {
			return addAll(other.iterator());
		}
		
		default boolean addAll (CharIterator it) {
			boolean changed = false;
			while (it.hasNext()) {
				changed |= add(it.nextChar());
			}
			return changed;
		}

		default boolean addAll (char[] array) {
			return addAll(array, 0, array.length);
		}

		default boolean addAll (char[] array, int offset, int length) {
			boolean changed = false;
			for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
				changed |= add(array[i]);
			}
			return changed;
		}

		/**
		 * Removes from this collection all occurrences of any elements contained in the specified other collection.
		 *
		 * @param other a primitive collection of char items to remove fully, such as a CharList or a CharDeque
		 * @return true if this collection was modified.
		 */
		default boolean removeAll (OfChar other) {
			return removeAll(other.iterator());
		}
		/**
		 * Removes from this collection all occurrences of any elements contained in the specified CharIterator.
		 *
		 * @param it a CharIterator of items to remove fully
		 * @return true if this collection was modified.
		 */
		default boolean removeAll (CharIterator it) {
			CharIterator me;
			int originalSize = size();
			while (it.hasNext()) {
				char item = it.nextChar();
				me = iterator();
				while (me.hasNext()) {
					if (me.nextChar() == item) {
						me.remove();
					}
				}
			}
			return originalSize != size();
		}

		default boolean removeAll (char[] array) {
			return removeAll(array, 0, array.length);
		}

		default boolean removeAll (char[] array, int offset, int length) {
			CharIterator me;
			int originalSize = size();
			for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
				char item = array[i];
				me = iterator();
				while (me.hasNext()) {
					if (me.nextChar() == item) {
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
		 * @param other a primitive collection of char items to remove one-by-one, such as a CharList or a CharDeque
		 * @return true if this collection was modified.
		 */
		default boolean removeEach (OfChar other) {
			return removeEach(other.iterator());
		}
		/**
		 * Removes from this collection element-wise occurrences of elements contained in the specified CharIterator.
		 * Note that if a value is present more than once in this collection, only one of those occurrences
		 * will be removed for each occurrence of that value in {@code it}. If {@code it} has the same
		 * contents as this collection or has additional items, then removing each of {@code it} will clear this.
		 *
		 * @param it a CharIterator of items to remove one-by-one
		 * @return true if this collection was modified.
		 */
		default boolean removeEach (CharIterator it) {
			boolean changed = false;
			while (it.hasNext()) {
				changed |= remove(it.nextChar());
			}
			return changed;
		}

		default boolean removeEach (char[] array) {
			return removeEach(array, 0, array.length);
		}

		default boolean removeEach (char[] array, int offset, int length) {
			boolean changed = false;
			for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
				changed |= remove(array[i]);
			}
			return changed;
		}

		default boolean containsAll (OfChar other) {
			return containsAll(other.iterator());
		}

		default boolean containsAll (CharIterator it) {
			while (it.hasNext()) {
				if(!contains(it.nextChar())) return false;
			}
			return true;
		}

		default boolean containsAll (char[] array) {
			return containsAll(array, 0, array.length);
		}

		default boolean containsAll (char[] array, int offset, int length) {
			for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
				if(!contains(array[i])) return false;
			}
			return true;
		}

		default boolean containsAny (OfChar other) {
			return containsAny(other.iterator());
		}

		default boolean containsAny (CharIterator it) {
			while (it.hasNext()) {
				if(contains(it.nextChar())) return true;
			}
			return false;
		}

		default boolean containsAny (char[] array) {
			return containsAny(array, 0, array.length);
		}

		default boolean containsAny (char[] array, int offset, int length) {
			for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
				if(contains(array[i])) return true;
			}
			return false;
		}

		/**
		 * Removes all the elements of this collection that satisfy the given
		 * predicate.  Errors or runtime exceptions thrown during iteration or by
		 * the predicate are relayed to the caller.
		 *
		 * @param filter a predicate which returns {@code true} for elements to be
		 *               removed
		 * @return {@code true} if any elements were removed
		 * @throws UnsupportedOperationException if elements cannot be removed
		 *                                       from this collection.  Implementations may throw this exception if a
		 *                                       matching element cannot be removed or if, in general, removal is not
		 *                                       supported.
		 * @implSpec The default implementation traverses all elements of the collection using
		 * its {@link #iterator()}.  Each matching element is removed using
		 * {@link Iterator#remove()}.  If the collection's iterator does not
		 * support removal then an {@code UnsupportedOperationException} will be
		 * thrown on the first matching element.
		 */
		default boolean removeIf (CharPredicate filter) {
			boolean removed = false;
			final CharIterator each = iterator();
			while (each.hasNext()) {
				if (filter.test(each.nextChar())) {
					each.remove();
					removed = true;
				}
			}
			return removed;
		}

		default boolean retainAll (OfChar other) {
			boolean changed = false;
			CharIterator it = iterator();
			while (it.hasNext()) {
				if (!other.contains(it.nextChar())) {
					it.remove();
					changed = true;
				}
			}
			return changed;
		}

		default boolean retainAll (char[] array) {
			return retainAll(array, 0, array.length);
		}

		default boolean retainAll (char[] array, int offset, int length) {
			boolean modified = false;
			CharIterator it = iterator();
			OUTER:
			while (it.hasNext()) {
				char check = it.next();
				for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
					if(array[i] == check)
						continue OUTER;
				}
				it.remove();
				modified = true;
			}
			return modified;
		}

		@Override
		CharIterator iterator ();

		/**
		 * Allocates a new char array with exactly {@link #size()} items, fills it with the
		 * contents of this PrimitiveCollection, and returns it.
		 *
		 * @return a new char array
		 */
		default char[] toArray () {
			final int sz = size();
			char[] receiver = new char[sz];
			CharIterator it = iterator();
			int i = 0;
			while (it.hasNext())
				receiver[i++] = it.nextChar();
			return receiver;
		}

		/**
		 * Fills the given array with the entire contents of this PrimitiveCollection, up to
		 * {@link #size()} items, or if receiver is not large enough, then this allocates a new
		 * char array with {@link #size()} items and returns that.
		 *
		 * @param receiver a char array that will be filled with the items from this, if possible
		 * @return {@code receiver}, if it was modified, or a new char array otherwise
		 */
		default char[] toArray (char[] receiver) {
			final int sz = size();
			if (receiver.length < sz)
				receiver = new char[sz];
			CharIterator it = iterator();
			int i = 0;
			while (it.hasNext())
				receiver[i++] = it.nextChar();
			return receiver;
		}

		/**
		 * Performs the given action for each element of the {@code PrimitiveCollection.OfChar}
		 * until all elements have been processed or the action throws an
		 * exception.  Actions are performed in the order of iteration, if that
		 * order is specified.  Exceptions thrown by the action are relayed to the
		 * caller.
		 *
		 * @param action The action to be performed for each element
		 */
		default void forEach (CharConsumer action) {
			CharIterator it = iterator();
			while (it.hasNext())
				action.accept(it.nextChar());
		}

		/**
		 * Attempts to get the first item in this PrimitiveCollection, where "first" is only
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
		 * @return the first item in this PrimitiveCollection, as produced by {@link #iterator()}
		 * @throws IllegalStateException if this is empty
		 */
		default char first () {
			CharIterator it = iterator();
			if (it.hasNext())
				return it.nextChar();
			throw new IllegalStateException("Can't get the first() item of an empty PrimitiveCollection.");
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
		 * Makes a String from the contents of this PrimitiveCollection, but uses the given {@link CharAppender}
		 * to convert each item to a customizable representation and append them to a StringBuilder. To use
		 * the default String representation, you can use {@code StringBuilder::append} as an appender.
		 *
		 * @param separator how to separate items, such as {@code ", "}
		 * @param brackets true to wrap the output in square brackets, or false to omit them
		 * @param appender a function that takes a StringBuilder and a char, and returns the modified StringBuilder
		 * @return a new String representing this PrimitiveCollection
		 */
		default String toString (String separator, boolean brackets,
			CharAppender appender){
			return appendTo(new StringBuilder(), separator, brackets, appender).toString();
		}

		default StringBuilder appendTo (StringBuilder sb, String separator, boolean brackets) {
			return appendTo(sb, separator, brackets, StringBuilder::append);
		}

		/**
		 * Appends to a StringBuilder from the contents of this PrimitiveCollection, but uses the given {@link CharAppender}
		 * to convert each item to a customizable representation and append them to a StringBuilder. To use
		 * the default String representation, you can use {@code StringBuilder::append} as an appender.
		 *
		 * @param sb a StringBuilder that this can append to
		 * @param separator how to separate items, such as {@code ", "}
		 * @param brackets true to wrap the output in square brackets, or false to omit them
		 * @param appender a function that takes a StringBuilder and a char, and returns the modified StringBuilder
		 * @return {@code sb}, with the appended items of this PrimitiveCollection
		 */
		default StringBuilder appendTo (StringBuilder sb, String separator, boolean brackets, CharAppender appender) {
			if (isEmpty()) return brackets ? sb.append("[]") : sb;
			if (brackets) sb.append('[');
			CharIterator it = iterator();
			if (it.hasNext()) {
				while (true) {
					appender.apply(sb, it.nextChar());
					if (it.hasNext()) sb.append(separator);
					else break;
				}
			}
			if (brackets) sb.append(']');
			return sb;
		}
	}

	/**
	 * A PrimitiveCollection with unboxed {@code boolean} items.
	 */
	interface OfBoolean extends PrimitiveCollection<Boolean> {
		boolean add (boolean item);

		boolean remove (boolean item);

		boolean contains (boolean item);

		default boolean addAll (OfBoolean other) {
			return addAll(other.iterator());
		}
		
		default boolean addAll (BooleanIterator it) {
			boolean changed = false;
			while (it.hasNext()) {
				changed |= add(it.nextBoolean());
			}
			return changed;
		}

		default boolean addAll (boolean[] array) {
			return addAll(array, 0, array.length);
		}

		default boolean addAll (boolean[] array, int offset, int length) {
			boolean changed = false;
			for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
				changed |= add(array[i]);
			}
			return changed;
		}

		/**
		 * Removes from this collection all occurrences of any elements contained in the specified other collection.
		 *
		 * @param other a primitive collection of boolean items to remove fully, such as a BooleanList or a BooleanDeque
		 * @return true if this collection was modified.
		 */
		default boolean removeAll (OfBoolean other) {
			return removeAll(other.iterator());
		}
		/**
		 * Removes from this collection all occurrences of any elements contained in the specified BooleanIterator.
		 *
		 * @param it a BooleanIterator of items to remove fully
		 * @return true if this collection was modified.
		 */
		default boolean removeAll (BooleanIterator it) {
			BooleanIterator me;
			int originalSize = size();
			while (it.hasNext()) {
				boolean item = it.nextBoolean();
				me = iterator();
				while (me.hasNext()) {
					if (me.nextBoolean() == item) {
						me.remove();
					}
				}
			}
			return originalSize != size();
		}

		default boolean removeAll (boolean[] array) {
			return removeAll(array, 0, array.length);
		}

		default boolean removeAll (boolean[] array, int offset, int length) {
			BooleanIterator me;
			int originalSize = size();
			for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
				boolean item = array[i];
				me = iterator();
				while (me.hasNext()) {
					if (me.nextBoolean() == item) {
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
		 * @param other a primitive collection of boolean items to remove one-by-one, such as a BooleanList or a BooleanDeque
		 * @return true if this collection was modified.
		 */
		default boolean removeEach (OfBoolean other) {
			return removeEach(other.iterator());
		}
		/**
		 * Removes from this collection element-wise occurrences of elements contained in the specified BooleanIterator.
		 * Note that if a value is present more than once in this collection, only one of those occurrences
		 * will be removed for each occurrence of that value in {@code it}. If {@code it} has the same
		 * contents as this collection or has additional items, then removing each of {@code it} will clear this.
		 *
		 * @param it a BooleanIterator of items to remove one-by-one
		 * @return true if this collection was modified.
		 */
		default boolean removeEach (BooleanIterator it) {
			boolean changed = false;
			while (it.hasNext()) {
				changed |= remove(it.nextBoolean());
			}
			return changed;
		}

		default boolean removeEach (boolean[] array) {
			return removeEach(array, 0, array.length);
		}

		default boolean removeEach (boolean[] array, int offset, int length) {
			boolean changed = false;
			for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
				changed |= remove(array[i]);
			}
			return changed;
		}

		default boolean containsAll (OfBoolean other) {
			return containsAll(other.iterator());
		}

		default boolean containsAll (BooleanIterator it) {
			while (it.hasNext()) {
				if(!contains(it.nextBoolean())) return false;
			}
			return true;
		}

		default boolean containsAll (boolean[] array) {
			return containsAll(array, 0, array.length);
		}

		default boolean containsAll (boolean[] array, int offset, int length) {
			for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
				if(!contains(array[i])) return false;
			}
			return true;
		}

		default boolean containsAny (OfBoolean other) {
			return containsAny(other.iterator());
		}

		default boolean containsAny (BooleanIterator it) {
			while (it.hasNext()) {
				if(contains(it.nextBoolean())) return true;
			}
			return false;
		}

		default boolean containsAny (boolean[] array) {
			return containsAny(array, 0, array.length);
		}

		default boolean containsAny (boolean[] array, int offset, int length) {
			for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
				if(contains(array[i])) return true;
			}
			return false;
		}

		/**
		 * Removes all the elements of this collection that satisfy the given
		 * predicate.  Errors or runtime exceptions thrown during iteration or by
		 * the predicate are relayed to the caller.
		 *
		 * @param filter a predicate which returns {@code true} for elements to be
		 *               removed
		 * @return {@code true} if any elements were removed
		 * @throws UnsupportedOperationException if elements cannot be removed
		 *                                       from this collection.  Implementations may throw this exception if a
		 *                                       matching element cannot be removed or if, in general, removal is not
		 *                                       supported.
		 * @implSpec The default implementation traverses all elements of the collection using
		 * its {@link #iterator()}.  Each matching element is removed using
		 * {@link Iterator#remove()}.  If the collection's iterator does not
		 * support removal then an {@code UnsupportedOperationException} will be
		 * thrown on the first matching element.
		 */
		default boolean removeIf (BooleanPredicate filter) {
			boolean removed = false;
			final BooleanIterator each = iterator();
			while (each.hasNext()) {
				if (filter.test(each.nextBoolean())) {
					each.remove();
					removed = true;
				}
			}
			return removed;
		}

		default boolean retainAll (OfBoolean other) {
			boolean changed = false;
			BooleanIterator it = iterator();
			while (it.hasNext()) {
				if (!other.contains(it.nextBoolean())) {
					it.remove();
					changed = true;
				}
			}
			return changed;
		}

		default boolean retainAll (boolean[] array) {
			return retainAll(array, 0, array.length);
		}

		default boolean retainAll (boolean[] array, int offset, int length) {
			boolean modified = false;
			BooleanIterator it = iterator();
			OUTER:
			while (it.hasNext()) {
				boolean check = it.next();
				for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
					if(array[i] == check)
						continue OUTER;
				}
				it.remove();
				modified = true;
			}
			return modified;
		}

		@Override
		BooleanIterator iterator ();

		/**
		 * Allocates a new boolean array with exactly {@link #size()} items, fills it with the
		 * contents of this PrimitiveCollection, and returns it.
		 *
		 * @return a new boolean array
		 */
		default boolean[] toArray () {
			final int sz = size();
			boolean[] receiver = new boolean[sz];
			BooleanIterator it = iterator();
			int i = 0;
			while (it.hasNext())
				receiver[i++] = it.nextBoolean();
			return receiver;
		}

		/**
		 * Fills the given array with the entire contents of this PrimitiveCollection, up to
		 * {@link #size()} items, or if receiver is not large enough, then this allocates a new
		 * boolean array with {@link #size()} items and returns that.
		 *
		 * @param receiver a boolean array that will be filled with the items from this, if possible
		 * @return {@code receiver}, if it was modified, or a new boolean array otherwise
		 */
		default boolean[] toArray (boolean[] receiver) {
			final int sz = size();
			if (receiver.length < sz)
				receiver = new boolean[sz];
			BooleanIterator it = iterator();
			int i = 0;
			while (it.hasNext())
				receiver[i++] = it.nextBoolean();
			return receiver;
		}

		/**
		 * Performs the given action for each element of the {@code PrimitiveCollection.OfBoolean}
		 * until all elements have been processed or the action throws an
		 * exception.  Actions are performed in the order of iteration, if that
		 * order is specified.  Exceptions thrown by the action are relayed to the
		 * caller.
		 *
		 * @param action The action to be performed for each element
		 */
		default void forEach (BooleanConsumer action) {
			BooleanIterator it = iterator();
			while (it.hasNext())
				action.accept(it.nextBoolean());
		}

		/**
		 * Attempts to get the first item in this PrimitiveCollection, where "first" is only
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
		 * @return the first item in this PrimitiveCollection, as produced by {@link #iterator()}
		 * @throws IllegalStateException if this is empty
		 */
		default boolean first () {
			BooleanIterator it = iterator();
			if (it.hasNext())
				return it.nextBoolean();
			throw new IllegalStateException("Can't get the first() item of an empty PrimitiveCollection.");
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
		 * Makes a String from the contents of this PrimitiveCollection, but uses the given {@link BooleanAppender}
		 * to convert each item to a customizable representation and append them to a StringBuilder. To use
		 * the default String representation, you can use {@code StringBuilder::append} as an appender, or better yet,
		 * use {@link BooleanAppender#DEFAULT}, which caches the above method reference when Android won't do that.
		 *
		 * @param separator how to separate items, such as {@code ", "}
		 * @param brackets true to wrap the output in square brackets, or false to omit them
		 * @param appender a function that takes a StringBuilder and a boolean, and returns the modified StringBuilder
		 * @return a new String representing this PrimitiveCollection
		 */
		default String toString (String separator, boolean brackets,
			BooleanAppender appender){
			return appendTo(new StringBuilder(), separator, brackets, appender).toString();
		}

		/**
		 * Appends to {@code sb} any items in this PrimitiveCollection as either "true" or "false", separated by
		 * {@code separator} and optionally with square brackets surrounding the text if {@code brackets} is true.
		 * @param sb the StringBuilder to append to
		 * @param separator the String that will separate each item
		 * @param brackets if true, square brackets will surround the appended text
		 * @return {@code sb}, for chaining
		 */
		default StringBuilder appendTo (StringBuilder sb, String separator, boolean brackets) {
			return appendTo(sb, separator, brackets, BooleanAppender.DEFAULT);
		}

		/**
		 * Appends to a StringBuilder from the contents of this PrimitiveCollection, but uses the given {@link BooleanAppender}
		 * to convert each item to a customizable representation and append them to a StringBuilder. To use
		 * the default String representation, you can use {@code StringBuilder::append} as an appender, or better yet,
		 * use {@link BooleanAppender#DEFAULT}, which caches the above method reference when Android won't do that.
		 *
		 * @param sb a StringBuilder that this can append to
		 * @param separator how to separate items, such as {@code ", "}
		 * @param brackets true to wrap the output in square brackets, or false to omit them
		 * @param appender a function that takes a StringBuilder and a boolean, and returns the modified StringBuilder
		 * @return {@code sb}, with the appended items of this PrimitiveCollection
		 */
		default StringBuilder appendTo (StringBuilder sb, String separator, boolean brackets, BooleanAppender appender) {
			if (isEmpty()) return brackets ? sb.append("[]") : sb;
			if (brackets) sb.append('[');
			BooleanIterator it = iterator();
			if (it.hasNext()) {
				while (true) {
					appender.apply(sb, it.nextBoolean());
					if (it.hasNext()) sb.append(separator);
					else break;
				}
			}
			if (brackets) sb.append(']');
			return sb;
		}


		/**
		 * Returns a String representing this PrimitiveCollection with "1" for true items and "0" for false, with no
		 * surrounding brackets.
		 * @return a String made of "0" and "1"
		 */
		default String toDenseString () {
			return denseAppendTo(new StringBuilder(size()), false).toString();
		}

		/**
		 * Returns a String representing this PrimitiveCollection with "1" for true items and "0" for false, with
		 * surrounding square brackets if {@code brackets} is true.
		 * @param brackets if true, the result will be surrounded by square brackets
		 * @return a String made of "0" and "1", optionally with surrounding square brackets
		 */
		default String toDenseString (boolean brackets) {
			return denseAppendTo(new StringBuilder(size() + 2), brackets).toString();
		}

		/**
		 * Appends to {@code sb} any items in this PrimitiveCollection as either "1" for true items or "0" for false,
		 * with no separators and optionally with square brackets surrounding the text if {@code brackets} is true.
		 * @param sb the StringBuilder to append to
		 * @param brackets if true, square brackets will surround the appended text
		 * @return {@code sb}, for chaining
		 */
		default StringBuilder denseAppendTo (StringBuilder sb, boolean brackets) {
			return appendTo(sb, "", brackets, BooleanAppender.BINARY);
		}
	}
}

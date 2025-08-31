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

import com.github.tommyettinger.digital.BitConversion;
import com.github.tommyettinger.ds.support.util.Appender;
import com.github.tommyettinger.ds.support.util.PartialParser;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * An Ordered Set of {@code T} items where the {@link #indexOf(Object)} operation runs in constant time, but
 * any removal from the middle of the order runs in linear time. If you primarily append to a Set with
 * {@link #add(Object)}, this will perform like {@link ObjectIntOrderedMap}, since it's backed internally by
 * one of those Maps; indexOf() delegates to the map's {@link ObjectIntOrderedMap#get(Object)} method. This
 * has to do some bookkeeping to make sure the index for each item is stored as the value for the key matching
 * the item in the map. That bookkeeping will fail if you use the {@link Iterator#remove()} method on this
 * class' iterator; you can correct the indices with {@link #renumber()}, or {@link #renumber(int)} if you know
 * the first incorrect index.
 *
 * @param <T> the type of items; should implement {@link Object#equals(Object)} and {@link Object#hashCode()}
 */
public class NumberedSet<T> implements Set<T>, Ordered<T>, EnhancedCollection<T> {

	protected class InternalMap extends ObjectIntOrderedMap<T> {
		public InternalMap() {
			super();
		}

		public InternalMap(int initialCapacity) {
			super(initialCapacity);
		}

		public InternalMap(int initialCapacity, float loadFactor) {
			super(initialCapacity, loadFactor);
		}

		public InternalMap(ObjectIntOrderedMap<? extends T> map) {
			super(map);
		}

		public InternalMap(ObjectIntMap<? extends T> map) {
			super(map);
		}

		public InternalMap(T[] keys, int[] values) {
			super(keys, values);
		}

		public InternalMap(Collection<? extends T> keys, PrimitiveCollection.OfInt values) {
			super(keys, values);
		}

		@Override
		protected int place(@NonNull Object item) {
			return NumberedSet.this.place(item);
		}

		@Override
		protected boolean equate(Object left, @Nullable Object right) {
			return NumberedSet.this.equate(left, right);
		}

		protected int addOrIndex(final T t) {
			int index;
			if ((index = getOrDefault(t, -1)) == -1) {
				put(t, size);
				return size - 1;
			}
			return index;
		}
	}

	protected transient InternalMap map;
	@Nullable
	protected transient NumberedSetIterator<T> iterator1;
	@Nullable
	protected transient NumberedSetIterator<T> iterator2;

	public NumberedSet() {
		this(Utilities.getDefaultTableCapacity(), Utilities.getDefaultLoadFactor());
	}

	public NumberedSet(int initialCapacity, float loadFactor) {
		map = new InternalMap(initialCapacity, loadFactor);
		map.setDefaultValue(-1);
	}

	public NumberedSet(int initialCapacity) {
		this(initialCapacity, Utilities.getDefaultLoadFactor());
	}

	public NumberedSet(NumberedSet<? extends T> other) {
		map = new InternalMap(other.map);
	}

	/**
	 * Can be used to make a NumberedSet from any {@link Ordered} map or set with Object keys or items, using
	 * the keys for a map and the items for a set.
	 *
	 * @param ordered any {@link Ordered} with the same type as this NumberSet
	 */
	public NumberedSet(Ordered<? extends T> ordered) {
		this(ordered.size());
		addAll(ordered.order());
	}

	/**
	 * Creates a new set that contains all distinct elements in {@code coll}.
	 *
	 * @param coll all distinct items in this Collection will become items in this NumberedSet
	 */
	public NumberedSet(Iterator<? extends T> coll) {
		this();
		addAll(coll);
	}

	/**
	 * Creates a new set that contains all distinct elements in {@code coll}.
	 *
	 * @param coll all distinct items in this Collection will become items in this NumberedSet
	 */
	public NumberedSet(Collection<? extends T> coll) {
		this(coll.size());
		addAll(coll);
	}

	/**
	 * Creates a new set that contains all distinct elements in {@code items}.
	 *
	 * @param items all distinct elements in this array will become items in this NumberedSet
	 */
	public NumberedSet(T[] items) {
		this(items.length);
		addAll(items);
	}

	/**
	 * Creates a new set by copying {@code count} items from the given Ordered, starting at {@code offset} in that Ordered,
	 * into this.
	 *
	 * @param other  another Ordered of the same type
	 * @param offset the first index in other's ordering to draw an item from
	 * @param count  how many items to copy from other
	 */
	public NumberedSet(Ordered<T> other, int offset, int count) {
		this(count);
		addAll(0, other, offset, count);
	}

	/**
	 * Returns an index &gt;= 0 and &lt;= {@link InternalMap#mask} for the specified {@code item}, mixed.
	 *
	 * @param item a non-null Object; its hashCode() method should be used by most implementations
	 * @return an index between 0 and {@link InternalMap#mask} (both inclusive)
	 */
	protected int place(@NonNull Object item) {
		return BitConversion.imul(item.hashCode(), map.hashMultiplier) >>> map.shift;
		// This can be used if you know hashCode() has few collisions normally, and won't be maliciously manipulated.
//		return item.hashCode() & map.mask;
	}

	/**
	 * Compares the objects left and right, which are usually keys, for equality, returning true if they are considered
	 * equal. This is used by the rest of this class to determine whether two keys are considered equal. Normally, this
	 * returns {@code left.equals(right)}, but subclasses can override it to use reference equality, fuzzy equality, deep
	 * array equality, or any other custom definition of equality. Usually, {@link #place(Object)} is also overridden if
	 * this method is.
	 * <br>
	 * You can override this, which will affect the internal map that NumberedSet uses.
	 *
	 * @param left  must be non-null; typically a key being compared, but not necessarily
	 * @param right may be null; typically a key being compared, but can often be null for an empty key slot, or some other type
	 * @return true if left and right are considered equal for the purposes of this class
	 */
	protected boolean equate(Object left, @Nullable Object right) {
		return left.equals(right);
	}

	@Override
	public ObjectList<T> order() {
		return map.keys;
	}

	/**
	 * Reassigns all index values to match {@link #order()}.
	 * This should be called if you have removed any items using {@link Iterator#remove()} from this
	 * NumberedSet, since the iterator's remove() method doesn't update the numbering on its own.
	 * Use this method if you don't know the first incorrect index, or {@link #renumber(int)} if you do.
	 * Note that you can remove multiple items using the iterator, and only need to renumber just before
	 * you need the indices (such as for {@link #indexOf(Object)}).
	 */
	public void renumber() {
		final int s = size();
		for (int i = 0; i < s; i++) {
			map.valueTable[map.locateKey(map.keys.get(i))] = i;
		}
	}

	/**
	 * Reassigns the index values for each index starting with {@code start}, and going to the end.
	 * This should be called if you have removed any items using {@link Iterator#remove()} from this
	 * NumberedSet, since the iterator's remove() method doesn't update the numbering on its own.
	 * Use {@link #renumber()} with no argument if you don't know the first incorrect index, or this
	 * method if you do. Note that you can remove multiple items using the iterator, and only need
	 * to renumber just before you need the indices (such as for {@link #indexOf(Object)}).
	 *
	 * @param start the first index to reassign, which must be non-negative
	 */
	public void renumber(final int start) {
		final int s = size();
		for (int i = start; i < s; i++) {
			map.valueTable[map.locateKey(map.keys.get(i))] = i;
		}
	}

	/**
	 * Tries to remove an item from this set and calls {@link #renumber(int)} if that item was removed
	 *
	 * @param item object to be removed from this set, if present
	 * @return true if this set was modified, or false if it wasn't
	 */
	@Override
	public boolean remove(Object item) {
		int prev = size();
		int oldIndex = map.remove(item);
		if (size() != prev) {
			renumber(oldIndex);
			return true;
		}
		return false;
	}

	@Override
	public boolean containsAll(Collection<@NonNull ?> c) {
		for (Object e : c) {
			if (!map.containsKey(e))
				return false;
		}
		return true;
	}

	public boolean containsAll(@NonNull Object[] values) {
		for (Object e : values) {
			if (!map.containsKey(e))
				return false;
		}
		return true;
	}

	public boolean containsAll(@NonNull Object[] values, int offset, int length) {
		for (int i = offset, n = 0; n < length && i < values.length; i++, n++) {
			if (!map.containsKey(values[i]))
				return false;
		}
		return true;
	}

	public boolean containsAnyIterable(Iterable<@NonNull ?> c) {
		for (Object e : c) {
			if (map.containsKey(e))
				return true;
		}
		return false;
	}

	public boolean containsAny(@NonNull Object[] values) {
		for (Object e : values) {
			if (map.containsKey(e))
				return true;
		}
		return false;
	}

	public boolean containsAny(@NonNull Object[] values, int offset, int length) {
		for (int i = offset, n = 0; n < length && i < values.length; i++, n++) {
			if (map.containsKey(values[i]))
				return true;
		}
		return false;
	}

	@Override
	public boolean addAll(Collection<@NonNull ? extends T> c) {
		boolean modified = false;
		for (T t : c)
			modified |= add(t);
		return modified;
	}

	/**
	 * Adds up to {@code count} items, starting from {@code offset}, in the Ordered {@code other} to this set,
	 * inserting at the end of the iteration order.
	 *
	 * @param other  a non-null {@link Ordered} of {@code T}
	 * @param offset the first index in {@code other} to use
	 * @param count  how many indices in {@code other} to use
	 * @return true if this is modified by this call, as {@link #addAll(Collection)} does
	 */
	public boolean addAll(Ordered<@NonNull T> other, int offset, int count) {
		return addAll(map.size, other, offset, count);
	}

	/**
	 * Adds up to {@code count} items, starting from {@code offset}, in the Ordered {@code other} to this set,
	 * inserting starting at {@code insertionIndex} in the iteration order.
	 *
	 * @param insertionIndex where to insert into the iteration order
	 * @param other          a non-null {@link Ordered} of {@code T}
	 * @param offset         the first index in {@code other} to use
	 * @param count          how many indices in {@code other} to use
	 * @return true if this is modified by this call, as {@link #addAll(Collection)} does
	 */
	public boolean addAll(int insertionIndex, Ordered<@NonNull T> other, int offset, int count) {
		boolean changed = false;
		int end = Math.min(offset + count, other.size());
		ensureCapacity(end - offset);
		for (int i = offset; i < end; i++) {
			add(insertionIndex++, other.order().get(i));
			changed = true;
		}
		return changed;
	}

	/**
	 * Adds all items in the T array {@code array} to this set, inserting at the end of the iteration order.
	 *
	 * @param array a non-null array of {@code T}
	 * @return true if this is modified by this call, as {@link #addAll(Collection)} does
	 */
	public boolean addAll(@NonNull T[] array) {
		return addAll(array, 0, array.length);
	}

	/**
	 * Adds up to {@code length} items, starting from {@code offset}, in the T array {@code array} to this set,
	 * inserting at the end of the iteration order.
	 *
	 * @param array  a non-null array of {@code T}
	 * @param offset the first index in {@code other} to use
	 * @param length how many indices in {@code other} to use
	 * @return true if this is modified by this call, as {@link #addAll(Collection)} does
	 */
	public boolean addAll(@NonNull T[] array, int offset, int length) {
		ensureCapacity(length);
		int oldSize = size();
		for (int i = offset, n = i + length; i < n; i++) {
			add(array[i]);
		}
		return oldSize != size();
	}

	/**
	 * Adds up to {@code count} items, starting from {@code offset}, in the T array {@code array} to this set,
	 * inserting starting at {@code insertionIndex} in the iteration order.
	 *
	 * @param insertionIndex where to insert into the iteration order
	 * @param array          a non-null array of {@code T}
	 * @param offset         the first index in {@code other} to use
	 * @param count          how many indices in {@code other} to use
	 * @return true if this is modified by this call, as {@link #addAll(Collection)} does
	 */
	public boolean addAll(int insertionIndex, @NonNull T[] array, int offset, int count) {
		boolean changed = false;
		int end = Math.min(offset + count, array.length);
		ensureCapacity(end - offset);
		for (int i = offset; i < end; i++) {
			add(insertionIndex++, array[i]);
			changed = true;
		}
		return changed;
	}

	@Override
	public boolean retainAll(@NonNull Collection<@NonNull ?> c) {
		boolean modified = false;
		Iterator<T> it = iterator();
		while (it.hasNext()) {
			if (!c.contains(it.next())) {
				it.remove();
				modified = true;
			}
		}
		if (modified) {
			renumber();
			return true;
		}
		return false;
	}

	@Override
	public boolean removeAll(@NonNull Collection<?> c) {
		boolean modified = false;
		Iterator<?> it = iterator();
		while (it.hasNext()) {
			if (c.contains(it.next())) {
				it.remove();
				modified = true;
			}
		}
		if (modified) {
			renumber();
			return true;
		}
		return false;
	}

	/**
	 * Bulk-removes each item in the given array from this set. If an item appears more
	 * than once in {@code arr}, this will be able to quickly verify that it was removed the first
	 * time it appeared, and won't spend as long processing later items. This calls
	 * {@link #renumber()} only after all removals were completed, and only if one or more items
	 * were actually removed.
	 *
	 * @param arr a non-null array of items to remove from this set
	 * @return true if this had one or more items removed, or false if it is unchanged
	 */
	public boolean removeAll(@NonNull Object[] arr) {
		int prevSize = size();
		for (int i = 0, len = arr.length; i < len; i++) {
			map.remove(arr[i]);
		}
		if (prevSize != size()) {
			renumber();
			return true;
		}
		return false;
	}

	/**
	 * Bulk-removes each item in the given array from this set. If an item appears more
	 * than once in {@code values}, this will be able to quickly verify that it was removed the first
	 * time it appeared, and won't spend as long processing later items. This calls
	 * {@link #renumber()} only after all removals were completed, and only if one or more items
	 * were actually removed.
	 *
	 * @param values a non-null array of items to remove from this set
	 * @param offset the index of the first item in values to remove
	 * @param length how many items, at most, to get from values and remove from this
	 * @return true if this had one or more items removed, or false if it is unchanged
	 */
	public boolean removeAll(@NonNull Object[] values, int offset, int length) {
		int prevSize = size();
		for (int i = offset, n = 0; n < length && i < values.length; i++, n++) {
			map.remove(values[i]);
		}
		if (prevSize != size()) {
			renumber();
			return true;
		}
		return false;
	}

	/**
	 * Removes and returns the item at the given index in this set's order.
	 *
	 * @param index the index of the item to remove
	 * @return the removed item
	 */
	public T removeAt(int index) {
		T old = map.keyAt(index);
		map.removeAt(index);
		renumber(index);
		return old;
	}

	/**
	 * Increases the size of the backing array to accommodate the specified number of additional items / loadFactor.
	 * Useful before adding many items to avoid multiple backing array resizes.
	 *
	 * @param additionalCapacity how many more items this must be able to hold; the load factor increases the actual capacity change
	 */
	public void ensureCapacity(int additionalCapacity) {
		map.ensureCapacity(additionalCapacity);
	}

	/**
	 * Gets the current hashMultiplier, used in {@link #place(Object)} to mix hash codes.
	 * If {@link #setHashMultiplier(int)} is never called, the hashMultiplier will always be drawn from
	 * {@link Utilities#GOOD_MULTIPLIERS}, with the index equal to {@code 64 - shift}.
	 *
	 * @return the current hashMultiplier
	 */
	public int getHashMultiplier() {
		return map.getHashMultiplier();
	}

	/**
	 * Sets the hashMultiplier to the given int, which will be made odd if even and always negative (by OR-ing with
	 * 0x80000001). This can be any negative, odd int, but should almost always be drawn from
	 * {@link Utilities#GOOD_MULTIPLIERS} or something like it.
	 *
	 * @param hashMultiplier any int; will be made odd if even.
	 */
	public void setHashMultiplier(int hashMultiplier) {
		map.setHashMultiplier(hashMultiplier);
	}

	/**
	 * Gets the length of the internal array used to store all keys, as well as empty space awaiting more items to be
	 * entered. This length is equal to the length of the array used to store all values, and empty space for values,
	 * here. This is also called the capacity.
	 *
	 * @return the length of the internal array that holds all keys
	 */
	public int getTableSize() {
		return map.getTableSize();
	}

	/**
	 * Changes the item {@code before} to {@code after} without changing its position in the order or its value. Returns true if
	 * {@code after} has been added to the NumberedSet and {@code before} has been removed; returns false if {@code after} is
	 * already present or {@code before} is not present. If you are iterating over a NumberedSet and have an index, you should
	 * prefer {@link #alterAt(int, Object)}, which doesn't need to search for an index like this does and so can be faster.
	 *
	 * @param before an item that must be present for this to succeed
	 * @param after  an item that must not be in this map for this to succeed
	 * @return true if {@code before} was removed and {@code after} was added, false otherwise
	 */
	public boolean alter(T before, T after) {
		return map.alter(before, after);
	}

	/**
	 * Changes the item at the given {@code index} in the order to {@code after}, without changing the ordering of other entries or
	 * any values. If {@code after} is already present, this returns false; it will also return false if {@code index} is invalid
	 * for the size of this map. Otherwise, it returns true. Unlike {@link #alter(Object, Object)}, this operates in constant time.
	 *
	 * @param index the index in the order of the item to change; must be non-negative and less than {@link #size}
	 * @param after the item that will replace the contents at {@code index}; this item must not be present for this to succeed
	 * @return true if {@code after} successfully replaced the item at {@code index}, false otherwise
	 */
	public boolean alterAt(int index, T after) {
		return map.alterAt(index, after);
	}

	/**
	 * Returns the item at the given index, which must be at least 0 and less than {@link #size()}.
	 *
	 * @param index the index to retrieve; must be between 0, inclusive, and {@link #size()}, exclusive
	 * @return the item at {@code index}
	 */
	public T getAt(int index) {
		return map.keyAt(index);
	}

	/**
	 * Clears the map and reduces the size of the backing arrays to be the specified capacity / loadFactor, if they are larger.
	 */
	public void clear(int maximumCapacity) {
		map.clear(maximumCapacity);
	}

	@Override
	public void clear() {
		map.clear();
	}

	/**
	 * Gets the index of a given item in this set's ordering. Unlike most collections, this takes O(1) time here.
	 * This returns {@link #getDefaultValue()} (usually -1) if the item was not present.
	 *
	 * @param item the item to retrieve the index for
	 * @return the index of the item, or {@link #getDefaultValue()} (usually -1) if it was not found
	 */
	public int indexOf(Object item) {
		return map.get(item);
	}

	/**
	 * Gets the index of a given item in this set's ordering. Unlike most collections, this takes O(1) time here.
	 * This returns {@code defaultValue} if the item was not present.
	 *
	 * @param item the item to retrieve the index for
	 * @return the index of the item, or {@code defaultValue} if it was not found
	 */
	public int indexOfOrDefault(Object item, int defaultValue) {
		return map.getOrDefault(item, defaultValue);
	}

	public boolean notEmpty() {
		return map.notEmpty();
	}

	@Override
	public int size() {
		return map.size();
	}

	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	/**
	 * Gets the default value, a {@code int} which is returned by {@link #indexOf(Object)} if the key is not found.
	 * If not changed, the default value is -1 .
	 *
	 * @return the current default value
	 */
	public int getDefaultValue() {
		return map.getDefaultValue();
	}

	/**
	 * Sets the default value, a {@code int} which is returned by {@link #indexOf(Object)} if the key is not found.
	 * If not changed, the default value is -1 . Note that {@link #indexOfOrDefault(Object, int)} is also available,
	 * which allows specifying a "not-found" value per-call.
	 *
	 * @param defaultValue may be any int; should usually be one that doesn't occur as a typical value
	 */
	public void setDefaultValue(int defaultValue) {
		map.setDefaultValue(defaultValue);
	}

	public void shrink(int maximumCapacity) {
		map.shrink(maximumCapacity);
	}

	/**
	 * Returns true if this NumberedSet contains the given item, or false otherwise.
	 *
	 * @param item element whose presence in this set is to be tested
	 * @return true if this set contains item, or false otherwise
	 */
	@Override
	public boolean contains(Object item) {
		return map.containsKey(item);
	}

	/**
	 * Reduces the size of the set to the specified size. If the set is already smaller than the specified
	 * size, no action is taken.
	 *
	 * @param newSize the target size to try to reach by removing items, if smaller than the current size
	 */
	public void truncate(int newSize) {
		if (size() > newSize) {
			removeRange(newSize, size());
		}
	}

	/**
	 * Returns a {@link ListIterator} starting at index 0.
	 * This caches the iterator to avoid repeated allocation, and so is not
	 * suitable for nested iteration. You can use
	 * {@link NumberedSetIterator#NumberedSetIterator(NumberedSet)} if
	 * you need nested iteration.
	 * This is equivalent to {@link #listIterator()}.
	 *
	 * @return a ListIterator, or more specifically a {@link NumberedSetIterator} over this set
	 */
	@Override
	public @NonNull NumberedSetIterator<T> iterator() {
		if (iterator1 == null || iterator2 == null) {
			iterator1 = new NumberedSetIterator<>(this);
			iterator2 = new NumberedSetIterator<>(this);
		}
		if (!iterator1.valid) {
			iterator1.reset();
			iterator1.valid = true;
			iterator2.valid = false;
			return iterator1;
		}
		iterator2.reset();
		iterator2.valid = true;
		iterator1.valid = false;
		return iterator2;
	}

	/**
	 * Returns a {@link ListIterator} starting at index 0.
	 * This caches the iterator to avoid repeated allocation, and so is not
	 * suitable for nested iteration. You can use
	 * {@link NumberedSetIterator#NumberedSetIterator(NumberedSet)} if
	 * you need nested iteration.
	 *
	 * @return a ListIterator, or more specifically a {@link NumberedSetIterator} over this set
	 */
	public NumberedSetIterator<T> listIterator() {
		if (iterator1 == null || iterator2 == null) {
			iterator1 = new NumberedSetIterator<>(this);
			iterator2 = new NumberedSetIterator<>(this);
		}
		if (!iterator1.valid) {
			iterator1.reset();
			iterator1.valid = true;
			iterator2.valid = false;
			return iterator1;
		}
		iterator2.reset();
		iterator2.valid = true;
		iterator1.valid = false;
		return iterator2;
	}

	/**
	 * Returns a {@link ListIterator} starting at the specified index.
	 * This caches the iterator to avoid repeated allocation, and so is not
	 * suitable for nested iteration. You can use
	 * {@link NumberedSetIterator#NumberedSetIterator(NumberedSet, int)} if
	 * you need nested iteration. Giving an index of 0 is equivalent to calling
	 * {@link #listIterator()}, and starts at the first item in the order.
	 *
	 * @param index the first index in this set's order to iterate from
	 * @return a ListIterator, or more specifically a {@link NumberedSetIterator} over this set
	 */
	public NumberedSetIterator<T> listIterator(int index) {
		if (iterator1 == null || iterator2 == null) {
			iterator1 = new NumberedSetIterator<>(this, index);
			iterator2 = new NumberedSetIterator<>(this, index);
		}
		if (!iterator1.valid) {
			iterator1.reset(index);
			iterator1.valid = true;
			iterator2.valid = false;
			return iterator1;
		}
		iterator2.reset(index);
		iterator2.valid = true;
		iterator1.valid = false;
		return iterator2;
	}

	/**
	 * Removes the items between the specified start index, inclusive, and end index, exclusive.
	 * Note that this takes different arguments than some other range-related methods; this needs
	 * a start index and an end index, rather than a count of items. This matches the behavior in
	 * the JDK collections.
	 *
	 * @param start the first index to remove, inclusive
	 * @param end   the last index (after what should be removed), exclusive
	 */
	@Override
	public void removeRange(int start, int end) {
		map.removeRange(start, end);
	}

	@Override
	public Object @NonNull [] toArray() {
		return map.keySet().toArray();
	}

	@Override
	public <T1> T1 @NonNull [] toArray(T1 @NonNull [] a) {
		return map.keySet().toArray(a);
	}

	@Override
	public boolean add(T t) {
		final int s = size();
		map.putIfAbsent(t, s);
		return s != size();
	}

	/**
	 * If the given item {@code t} is present, this returns its index without modifying the NumberedSet; otherwise, it
	 * adds t to the end of the collection and returns the index for it there.
	 *
	 * @param t an item to get the index of, adding it if not present
	 * @return the index of {@code t} in this Arrangement
	 */
	public int addOrIndex(final T t) {
		return map.addOrIndex(t);
	}

	/**
	 * Sets the key at the specified index. Returns true if the key was not already in the set. If this set already contains the
	 * key, the existing key's index is changed if needed and false is returned. Note, the order of the parameters matches the
	 * order in {@link ObjectList} and the rest of the JDK, not OrderedSet in libGDX.
	 *
	 * @param index where in the iteration order to add the given key, or to move it if already present
	 * @param key   what T item to try to add, if not already present
	 * @return true if the key was added for the first time, or false if the key was already present (even if moved)
	 */
	public boolean add(int index, T key) {
		int old = map.get(key);
		if (old != -1) {
			if (old != index) {
				map.remove(key);
				map.put(key, index, index);
				renumber(index);
			}
			return false;
		}
		map.put(key, index, index);
		renumber(index);
		return true;
	}

	public void resize(int newSize) {
		map.resize(newSize);
	}

	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;

		if (!(o instanceof Set))
			return false;
		Collection<?> c = (Collection<?>) o;
		if (c.size() != size())
			return false;
		try {
			return containsAll(c);
		} catch (ClassCastException | NullPointerException unused) {
			return false;
		}
	}

	public float getLoadFactor() {
		return map.getLoadFactor();
	}

	public void setLoadFactor(float loadFactor) {
		map.setLoadFactor(loadFactor);
	}

	public T first() {
		if (size() == 0)
			throw new IllegalStateException("Cannot get the first() item of an empty NumberedSet.");
		return map.keyAt(0);
	}

	@Override
	public int hashCode() {
		return map.hashCode();
	}

	@Override
	public String toString() {
		return map.toString(", ", true);
	}

	/**
	 * Delegates to {@link #toString(String, boolean)} with the given itemSeparator and without braces.
	 * This is different from {@link #toString()}, which includes braces by default.
	 *
	 * @param itemSeparator how to separate set items, such as {@code ", "}
	 * @return a new String representing this set
	 */
	public String toString(String itemSeparator) {
		return map.keys.toString(itemSeparator, false);
	}

	public String toString(String itemSeparator, boolean braces) {
		return map.keys.appendTo(new StringBuilder(32), itemSeparator, braces).toString();
	}

	/**
	 * Makes a String from the contents of this NumberedSet, but uses the given {@link Appender}
	 * to convert all set items to a customizable representation and append them
	 * to a temporary StringBuilder. To use
	 * the default String representation, you can use {@code Appender::append} as an appender.
	 *
	 * @param entrySeparator how to separate set items, such as {@code ", "}
	 * @param braces         true to wrap the output in curly braces, or false to omit them
	 * @param keyAppender    a function that takes a StringBuilder and a T, and returns the modified StringBuilder
	 * @return a new String representing this set
	 */
	public String toString(String entrySeparator, boolean braces, Appender<T> keyAppender) {
		return map.keys.appendTo(new StringBuilder(), entrySeparator, braces, keyAppender).toString();
	}

	public StringBuilder appendTo(StringBuilder sb, String entrySeparator, boolean braces) {
		return map.keys.appendTo(sb, entrySeparator, braces, Appender::append);
	}

	/**
	 * Appends to a StringBuilder from the contents of this NumberedSet, but uses the given {@link Appender}
	 * to convert all keys to a customizable representation and append them
	 * to a StringBuilder. To use
	 * the default String representation, you can use {@code Appender::append} as an appender.
	 *
	 * @param sb            a StringBuilder that this can append to
	 * @param itemSeparator how to separate set items, such as {@code ", "}
	 * @param braces        true to wrap the output in curly braces, or false to omit them
	 * @param keyAppender   a function that takes a StringBuilder and a T, and returns the modified StringBuilder
	 * @return {@code sb}, with the appended items of this set
	 */
	public StringBuilder appendTo(StringBuilder sb, String itemSeparator, boolean braces,
								  Appender<T> keyAppender) {
		return map.keys.appendTo(sb, itemSeparator, braces, keyAppender);
	}

	/**
	 * An {@link Iterator} and {@link ListIterator} over the elements of an ObjectList, while also an {@link Iterable}.
	 *
	 * @param <T> the generic type for the ObjectList this iterates over
	 */
	public static class NumberedSetIterator<T> implements Iterable<T>, ListIterator<T> {
		protected int index, latest = -1;
		protected NumberedSet<T> ns;
		protected boolean valid = true;

		public NumberedSetIterator(NumberedSet<T> ns) {
			this.ns = ns;
		}

		public NumberedSetIterator(NumberedSet<T> ns, int index) {
			if (index < 0 || index >= ns.size())
				throw new IndexOutOfBoundsException("NumberedSetIterator does not satisfy index >= 0 && index < list.size()");
			this.ns = ns;
			this.index = index;
		}

		/**
		 * Returns the next {@code int} element in the iteration.
		 *
		 * @return the next {@code int} element in the iteration
		 * @throws NoSuchElementException if the iteration has no more elements
		 */
		@Override
		@Nullable
		public T next() {
			if (!valid) {
				throw new RuntimeException("#iterator() cannot be used nested.");
			}
			if (index >= ns.size()) {
				throw new NoSuchElementException();
			}
			return ns.getAt(latest = index++);
		}

		/**
		 * Returns {@code true} if the iteration has more elements.
		 * (In other words, returns {@code true} if {@link #next} would
		 * return an element rather than throwing an exception.)
		 *
		 * @return {@code true} if the iteration has more elements
		 */
		@Override
		public boolean hasNext() {
			if (!valid) {
				throw new RuntimeException("#iterator() cannot be used nested.");
			}
			return index < ns.size();
		}

		/**
		 * Returns {@code true} if this list iterator has more elements when
		 * traversing the list in the reverse direction.  (In other words,
		 * returns {@code true} if {@link #previous} would return an element
		 * rather than throwing an exception.)
		 *
		 * @return {@code true} if the list iterator has more elements when
		 * traversing the list in the reverse direction
		 */
		@Override
		public boolean hasPrevious() {
			if (!valid) {
				throw new RuntimeException("#iterator() cannot be used nested.");
			}
			return index > 0 && ns.notEmpty();
		}

		/**
		 * Returns the previous element in the list and moves the cursor
		 * position backwards.  This method may be called repeatedly to
		 * iterate through the list backwards, or intermixed with calls to
		 * {@link #next} to go back and forth.  (Note that alternating calls
		 * to {@code next} and {@code previous} will return the same
		 * element repeatedly.)
		 *
		 * @return the previous element in the list
		 * @throws NoSuchElementException if the iteration has no previous
		 *                                element
		 */
		@Override
		@Nullable
		public T previous() {
			if (!valid) {
				throw new RuntimeException("#iterator() cannot be used nested.");
			}
			if (index <= 0 || ns.isEmpty()) {
				throw new NoSuchElementException();
			}
			return ns.getAt(latest = --index);
		}

		/**
		 * Returns the index of the element that would be returned by a
		 * subsequent call to {@link #next}. (Returns list size if the list
		 * iterator is at the end of the list.)
		 *
		 * @return the index of the element that would be returned by a
		 * subsequent call to {@code next}, or list size if the list
		 * iterator is at the end of the list
		 */
		@Override
		public int nextIndex() {
			return index;
		}

		/**
		 * Returns the index of the element that would be returned by a
		 * subsequent call to {@link #previous}. (Returns -1 if the list
		 * iterator is at the beginning of the list.)
		 *
		 * @return the index of the element that would be returned by a
		 * subsequent call to {@code previous}, or -1 if the list
		 * iterator is at the beginning of the list
		 */
		@Override
		public int previousIndex() {
			return index - 1;
		}

		/**
		 * Removes from the list the last element that was returned by {@link
		 * #next} or {@link #previous} (optional operation).  This call can
		 * only be made once per call to {@code next} or {@code previous}.
		 * It can be made only if {@link #add} has not been
		 * called after the last call to {@code next} or {@code previous}.
		 *
		 * @throws UnsupportedOperationException if the {@code remove}
		 *                                       operation is not supported by this list iterator
		 * @throws IllegalStateException         if neither {@code next} nor
		 *                                       {@code previous} have been called, or {@code remove} or
		 *                                       {@code add} have been called after the last call to
		 *                                       {@code next} or {@code previous}
		 */
		@Override
		public void remove() {
			if (!valid) {
				throw new RuntimeException("#iterator() cannot be used nested.");
			}
			if (latest == -1 || latest >= ns.size()) {
				throw new NoSuchElementException();
			}
			ns.removeAt(latest);
			index = latest;
			latest = -1;
		}

		/**
		 * Replaces the last element returned by {@link #next} or
		 * {@link #previous} with the specified element (optional operation).
		 * This call can be made only if neither {@link #remove} nor {@link
		 * #add} have been called after the last call to {@code next} or
		 * {@code previous}.
		 *
		 * @param t the element with which to replace the last element returned by
		 *          {@code next} or {@code previous}
		 * @throws UnsupportedOperationException if the {@code set} operation
		 *                                       is not supported by this list iterator
		 * @throws ClassCastException            if the class of the specified element
		 *                                       prevents it from being added to this list
		 * @throws IllegalArgumentException      if some aspect of the specified
		 *                                       element prevents it from being added to this list
		 * @throws IllegalStateException         if neither {@code next} nor
		 *                                       {@code previous} have been called, or {@code remove} or
		 *                                       {@code add} have been called after the last call to
		 *                                       {@code next} or {@code previous}
		 */
		@Override
		public void set(T t) {
			if (!valid) {
				throw new RuntimeException("#iterator() cannot be used nested.");
			}
			if (latest == -1 || latest >= ns.size()) {
				throw new NoSuchElementException();
			}
			ns.alterAt(latest, t);
		}

		/**
		 * Inserts the specified element into the list (optional operation).
		 * The element is inserted immediately before the element that
		 * would be returned by {@link #next}, if any, and after the element
		 * that would be returned by {@link #previous}, if any.  (If the
		 * list contains no elements, the new element becomes the sole element
		 * on the list.)  The new element is inserted before the implicit
		 * cursor: a subsequent call to {@code next} would be unaffected, and a
		 * subsequent call to {@code previous} would return the new element.
		 * (This call increases by one the value that would be returned by a
		 * call to {@code nextIndex} or {@code previousIndex}.)
		 *
		 * @param t the element to insert
		 * @throws UnsupportedOperationException if the {@code add} method is
		 *                                       not supported by this list iterator
		 * @throws ClassCastException            if the class of the specified element
		 *                                       prevents it from being added to this list
		 * @throws IllegalArgumentException      if some aspect of this element
		 *                                       prevents it from being added to this list
		 */
		@Override
		public void add(@Nullable T t) {
			if (!valid) {
				throw new RuntimeException("#iterator() cannot be used nested.");
			}
			if (index > ns.size()) {
				throw new NoSuchElementException();
			}
			ns.add(index++, t);
			latest = -1;
		}

		public void reset() {
			index = 0;
			latest = -1;
		}

		public void reset(int index) {
			if (index < 0 || index >= ns.size())
				throw new IndexOutOfBoundsException("NumberedSetIterator does not satisfy index >= 0 && index < list.size()");
			this.index = index;
			latest = -1;
		}

		/**
		 * Returns an iterator over elements of type {@code T}.
		 *
		 * @return a ListIterator; really this same NumberedSetIterator.
		 */
		@Override
		public @NonNull NumberedSetIterator<T> iterator() {
			return this;
		}
	}


	/**
	 * Constructs an empty set given the type as a generic type argument.
	 * This is usually less useful than just using the constructor, but can be handy
	 * in some code-generation scenarios when you don't know how many arguments you will have.
	 *
	 * @param <T> the type of items; must be given explicitly
	 * @return a new set containing nothing
	 */
	public static <T> NumberedSet<T> with() {
		return new NumberedSet<>(0);
	}

	/**
	 * Creates a new NumberedSet that holds only the given item, but can be resized.
	 *
	 * @param item one T item
	 * @param <T>  the type of item, typically inferred
	 * @return a new NumberedSet that holds the given item
	 */
	public static <T> NumberedSet<T> with(T item) {
		NumberedSet<T> set = new NumberedSet<>(1);
		set.add(item);
		return set;
	}

	/**
	 * Creates a new NumberedSet that holds only the given items, but can be resized.
	 *
	 * @param item0 a T item
	 * @param item1 a T item
	 * @param <T>   the type of item, typically inferred
	 * @return a new NumberedSet that holds the given items
	 */
	public static <T> NumberedSet<T> with(T item0, T item1) {
		NumberedSet<T> set = new NumberedSet<>(2);
		set.add(item0, item1);
		return set;
	}

	/**
	 * Creates a new NumberedSet that holds only the given items, but can be resized.
	 *
	 * @param item0 a T item
	 * @param item1 a T item
	 * @param item2 a T item
	 * @param <T>   the type of item, typically inferred
	 * @return a new NumberedSet that holds the given items
	 */
	public static <T> NumberedSet<T> with(T item0, T item1, T item2) {
		NumberedSet<T> set = new NumberedSet<>(3);
		set.add(item0, item1, item2);
		return set;
	}

	/**
	 * Creates a new NumberedSet that holds only the given items, but can be resized.
	 *
	 * @param item0 a T item
	 * @param item1 a T item
	 * @param item2 a T item
	 * @param item3 a T item
	 * @param <T>   the type of item, typically inferred
	 * @return a new NumberedSet that holds the given items
	 */
	public static <T> NumberedSet<T> with(T item0, T item1, T item2, T item3) {
		NumberedSet<T> set = new NumberedSet<>(4);
		set.add(item0, item1, item2, item3);
		return set;
	}

	/**
	 * Creates a new NumberedSet that holds only the given items, but can be resized.
	 *
	 * @param item0 a T item
	 * @param item1 a T item
	 * @param item2 a T item
	 * @param item3 a T item
	 * @param item4 a T item
	 * @param <T>   the type of item, typically inferred
	 * @return a new NumberedSet that holds the given items
	 */
	public static <T> NumberedSet<T> with(T item0, T item1, T item2, T item3, T item4) {
		NumberedSet<T> set = new NumberedSet<>(5);
		set.add(item0, item1, item2, item3);
		set.add(item4);
		return set;
	}

	/**
	 * Creates a new NumberedSet that holds only the given items, but can be resized.
	 *
	 * @param item0 a T item
	 * @param item1 a T item
	 * @param item2 a T item
	 * @param item3 a T item
	 * @param item4 a T item
	 * @param item5 a T item
	 * @param <T>   the type of item, typically inferred
	 * @return a new NumberedSet that holds the given items
	 */
	public static <T> NumberedSet<T> with(T item0, T item1, T item2, T item3, T item4, T item5) {
		NumberedSet<T> set = new NumberedSet<>(6);
		set.add(item0, item1, item2, item3);
		set.add(item4, item5);
		return set;
	}

	/**
	 * Creates a new NumberedSet that holds only the given items, but can be resized.
	 *
	 * @param item0 a T item
	 * @param item1 a T item
	 * @param item2 a T item
	 * @param item3 a T item
	 * @param item4 a T item
	 * @param item5 a T item
	 * @param item6 a T item
	 * @param <T>   the type of item, typically inferred
	 * @return a new NumberedSet that holds the given items
	 */
	public static <T> NumberedSet<T> with(T item0, T item1, T item2, T item3, T item4, T item5, T item6) {
		NumberedSet<T> set = new NumberedSet<>(7);
		set.add(item0, item1, item2, item3);
		set.add(item4, item5, item6);
		return set;
	}

	/**
	 * Creates a new NumberedSet that holds only the given items, but can be resized.
	 *
	 * @param item0 a T item
	 * @param item1 a T item
	 * @param item2 a T item
	 * @param item3 a T item
	 * @param item4 a T item
	 * @param item5 a T item
	 * @param item6 a T item
	 * @param <T>   the type of item, typically inferred
	 * @return a new NumberedSet that holds the given items
	 */
	public static <T> NumberedSet<T> with(T item0, T item1, T item2, T item3, T item4, T item5, T item6, T item7) {
		NumberedSet<T> set = new NumberedSet<>(8);
		set.add(item0, item1, item2, item3);
		set.add(item4, item5, item6, item7);
		return set;
	}

	/**
	 * Creates a new NumberedSet that holds only the given items, but can be resized.
	 * This overload will only be used when an array is supplied and the type of the
	 * items requested is the component type of the array, or if varargs are used and
	 * there are 9 or more arguments.
	 *
	 * @param varargs a T varargs or T array; remember that varargs allocate
	 * @param <T>     the type of item, typically inferred
	 * @return a new NumberedSet that holds the given items
	 */
	@SafeVarargs
	public static <T> NumberedSet<T> with(T... varargs) {
		return new NumberedSet<>(varargs);
	}

	/**
	 * Calls {@link #withLegible(String, String, PartialParser, boolean)} with brackets set to false.
	 * @param str a String that will be parsed in full
	 * @param delimiter the delimiter between items in str
	 * @return a new collection parsed from str
	 */
	public static <T> NumberedSet<T> withLegible(String str, String delimiter, PartialParser<T> parser) {
		return withLegible(str, delimiter, parser, false);
	}

	/**
	 * Creates a new collection and fills it by calling {@link #addLegible(String, String, PartialParser, int, int)} on
	 * either all of {@code str} (if {@code brackets} is false) or {@code str} without its first and last chars (if
	 * {@code brackets} is true). Each item is expected to be separated by {@code delimiter}.
	 *
	 * @param str a String that will be parsed in full (depending on brackets)
	 * @param delimiter the delimiter between items in str
	 * @param brackets if true, the first and last chars in str will be ignored
	 * @return a new collection parsed from str
	 */
	public static <T> NumberedSet<T> withLegible(String str, String delimiter, PartialParser<T> parser, boolean brackets) {
		NumberedSet<T> c = new NumberedSet<>();
		if(brackets)
			c.addLegible(str, delimiter, parser, 1, str.length() - 1);
		else
			c.addLegible(str, delimiter, parser);
		return c;
	}

	/**
	 * Creates a new collection and fills it by calling {@link #addLegible(String, String, PartialParser, int, int)}
	 * with the given five parameters as-is.
	 *
	 * @param str a String that will have the given section parsed
	 * @param delimiter the delimiter between items in str
	 * @param parser a PartialParser that returns a {@code T} item from a section of {@code str}
	 * @param offset the first position to parse in str, inclusive
	 * @param length how many chars to parse, starting from offset
	 * @return a new collection parsed from str
	 */
	public static <T> NumberedSet<T> withLegible(String str, String delimiter, PartialParser<T> parser, int offset, int length) {
		NumberedSet<T> c = new NumberedSet<>();
		c.addLegible(str, delimiter, parser, offset, length);
		return c;
	}
}

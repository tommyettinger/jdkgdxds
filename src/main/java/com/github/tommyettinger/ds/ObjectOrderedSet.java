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
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static com.github.tommyettinger.ds.Utilities.tableSize;

/**
 * A {@link ObjectSet} that also stores keys in an {@link ObjectList} using the insertion order. Null keys are not allowed. No
 * allocation is done except when growing the table size.
 * <p>
 * {@link #iterator() Iteration} is ordered and faster than an unordered set. Keys can also be accessed and the order changed
 * using {@link #order()}. There is some additional overhead for put and remove.
 * <p>
 * This class performs fast contains (typically O(1), worst case O(n) but that is rare in practice). Remove is somewhat slower due
 * to {@link #order()}. Add may be slightly slower, depending on hash collisions. Load factors greater than 0.9 greatly increase
 * the chances to resize to the next higher POT size.
 * <p>
 * Unordered sets and maps are not designed to provide especially fast iteration. Iteration is faster with {@link Ordered} types like
 * ObjectOrderedSet and ObjectObjectOrderedMap.
 * <p>
 * You can customize most behavior of this map by extending it. {@link #place(Object)} can be overridden to change how hashCodes
 * are calculated (which can be useful for types like {@link StringBuilder} that don't implement hashCode()), and
 * {@link #equate(Object, Object)} can be overridden to change how equality is calculated.
 * <p>
 * This implementation uses linear probing with the backward shift algorithm for removal.
 * It tries different hashes from a simple family, with the hash changing on resize.
 * Linear probing continues to work even when all hashCodes collide; it just works more slowly in that case.
 *
 * @author Nathan Sweet
 * @author Tommy Ettinger
 */
public class ObjectOrderedSet<T> extends ObjectSet<T> implements Ordered<T> {

	protected final ObjectList<T> items;

	public ObjectOrderedSet(OrderType ordering) {
		items = ordering == OrderType.BAG ? new ObjectBag<>() : new ObjectList<>();
	}

	public ObjectOrderedSet(int initialCapacity, float loadFactor, OrderType ordering) {
		super(initialCapacity, loadFactor);
		items = ordering == OrderType.BAG ? new ObjectBag<>(initialCapacity) : new ObjectList<>(initialCapacity);
	}

	public ObjectOrderedSet(int initialCapacity, OrderType ordering) {
		super(initialCapacity);
		items = ordering == OrderType.BAG ? new ObjectBag<>(initialCapacity) : new ObjectList<>(initialCapacity);
	}

	/**
	 * Creates a new instance containing the items in the specified iterator.
	 *
	 * @param coll an iterator that will have its remaining contents added to this
	 */
	public ObjectOrderedSet(Iterator<? extends T> coll, OrderType ordering) {
		this(ordering);
		addAll(coll);
	}

	public ObjectOrderedSet(ObjectOrderedSet<? extends T> set, OrderType ordering) {
		super(set);
		items = ordering == OrderType.BAG ? new ObjectBag<>(set.items) : new ObjectList<>(set.items);
	}

	/**
	 * Creates a new set that contains all distinct elements in {@code set}.
	 */
	public ObjectOrderedSet(ObjectSet<? extends T> set, OrderType ordering) {
		this(set.size(), set.loadFactor, ordering);
		hashMultiplier = set.hashMultiplier;
		addAll(set);
	}


	/**
	 * Creates a new set that contains all distinct elements in {@code coll}.
	 */
	public ObjectOrderedSet(Collection<? extends T> coll, OrderType ordering) {
		this(coll.size(), ordering);
		addAll(coll);
	}

	/**
	 * Creates a new set using {@code length} items from the given {@code array}, starting at {@code} offset (inclusive).
	 *
	 * @param array  an array to draw items from
	 * @param offset the first index in array to draw an item from
	 * @param length how many items to take from array; bounds-checking is the responsibility of the using code
	 */
	public ObjectOrderedSet(T[] array, int offset, int length, OrderType ordering) {
		this(length, ordering);
		addAll(array, offset, length);
	}

	/**
	 * Creates a new set that contains all distinct elements in {@code items}.
	 *
	 * @param items an array that will be used in full, except for duplicate items
	 */
	public ObjectOrderedSet(T[] items, OrderType ordering) {
		this(items.length, ordering);
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
	public ObjectOrderedSet(Ordered<T> other, int offset, int count, OrderType ordering) {
		this(count, ordering);
		addAll(other, offset, count);
	}

	// default order type

	public ObjectOrderedSet() {
		this(OrderType.LIST);
	}

	public ObjectOrderedSet(int initialCapacity, float loadFactor) {
		this(initialCapacity, loadFactor, OrderType.LIST);
	}

	public ObjectOrderedSet(int initialCapacity) {
		this(initialCapacity, OrderType.LIST);
	}

	/**
	 * Creates a new instance containing the items in the specified iterator.
	 *
	 * @param coll an iterator that will have its remaining contents added to this
	 */
	public ObjectOrderedSet(Iterator<? extends T> coll) {
		this(coll, OrderType.LIST);
	}

	public ObjectOrderedSet(ObjectOrderedSet<? extends T> set) {
		super(set);
		items = set.items instanceof ObjectBag ? new ObjectBag<>(set.items) : new ObjectList<>(set.items);
	}

	/**
	 * Creates a new set that contains all distinct elements in {@code set}.
	 */
	public ObjectOrderedSet(ObjectSet<? extends T> set) {
		this(set, OrderType.LIST);
	}


	/**
	 * Creates a new set that contains all distinct elements in {@code coll}.
	 */
	public ObjectOrderedSet(Collection<? extends T> coll) {
		this(coll, OrderType.LIST);
	}

	/**
	 * Creates a new set using {@code length} items from the given {@code array}, starting at {@code} offset (inclusive).
	 *
	 * @param array  an array to draw items from
	 * @param offset the first index in array to draw an item from
	 * @param length how many items to take from array; bounds-checking is the responsibility of the using code
	 */
	public ObjectOrderedSet(T[] array, int offset, int length) {
		this(array, offset, length, OrderType.LIST);
	}

	/**
	 * Creates a new set that contains all distinct elements in {@code items}.
	 *
	 * @param items an array that will be used in full, except for duplicate items
	 */
	public ObjectOrderedSet(T[] items) {
		this(items, OrderType.LIST);
	}

	/**
	 * Creates a new set by copying {@code count} items from the given Ordered, starting at {@code offset} in that Ordered,
	 * into this.
	 *
	 * @param other  another Ordered of the same type
	 * @param offset the first index in other's ordering to draw an item from
	 * @param count  how many items to copy from other
	 */
	public ObjectOrderedSet(Ordered<T> other, int offset, int count) {
		this(other, offset, count, OrderType.LIST);
	}

	@Override
	public boolean add(T key) {
		return super.add(key) && items.add(key);
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
		if (key == null || index < 0 || index > size) return false;
		if (!super.add(key)) {
			int oldIndex = items.indexOf(key);
			if (oldIndex != index) {
				items.add(index, items.remove(oldIndex));
			}
			return false;
		}
		items.add(index, key);
		return true;
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
	public boolean addAll(Ordered<T> other, int offset, int count) {
		return addAll(size, other, offset, count);
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
	public boolean addAll(int insertionIndex, Ordered<T> other, int offset, int count) {
		boolean changed = false;
		int end = Math.min(offset + count, other.size());
		ensureCapacity(end - offset);
		for (int i = offset; i < end; i++) {
			add(insertionIndex++, other.order().get(i));
			changed = true;
		}
		return changed;
	}

	@Override
	public boolean remove(@NonNull Object key) {
		return super.remove(key) && items.remove(key);
	}

	/**
	 * Removes and returns the item at the given index in this set's order.
	 *
	 * @param index the index of the item to remove
	 * @return the removed item
	 */
	public T removeAt(int index) {
		T key = items.removeAt(index);
		if (key != null)
			super.remove(key);
		return key;
	}

	/**
	 * Increases the size of the backing array to accommodate the specified number of additional items / loadFactor. Useful before
	 * adding many items to avoid multiple backing array resizes.
	 *
	 * @param additionalCapacity how many additional items this should be able to hold without resizing (probably)
	 */
	@Override
	public void ensureCapacity(int additionalCapacity) {
		int tableSize = tableSize(size + additionalCapacity, loadFactor);
		if (keyTable.length < tableSize) {
			resize(tableSize);
		}
		items.ensureCapacity(additionalCapacity);
	}

	/**
	 * Changes the item {@code before} to {@code after} without changing its position in the order. Returns true if {@code after}
	 * has been added to the ObjectOrderedSet and {@code before} has been removed; returns false if {@code after} is already present or
	 * {@code before} is not present. If you are iterating over an ObjectOrderedSet and have an index, you should prefer
	 * {@link #alterAt(int, Object)}, which doesn't need to search for an index like this does and so can be faster.
	 *
	 * @param before an item that must be present for this to succeed
	 * @param after  an item that must not be in this set for this to succeed
	 * @return true if {@code before} was removed and {@code after} was added, false otherwise
	 */
	public boolean alter(T before, T after) {
		if (contains(after)) {
			return false;
		}
		if (!super.remove(before)) {
			return false;
		}
		super.add(after);
		items.set(items.indexOf(before), after);
		return true;
	}

	/**
	 * Changes the item at the given {@code index} in the order to {@code after}, without changing the ordering of other items. If
	 * {@code after} is already present, this returns false; it will also return false if {@code index} is invalid for the size of
	 * this set. Otherwise, it returns true. Unlike {@link #alter(Object, Object)}, this operates in constant time.
	 *
	 * @param index the index in the order of the item to change; must be non-negative and less than {@link #size}
	 * @param after the item that will replace the contents at {@code index}; this item must not be present for this to succeed
	 * @return true if {@code after} successfully replaced the contents at {@code index}, false otherwise
	 */
	public boolean alterAt(int index, T after) {
		if (after == null || index < 0 || index >= size || contains(after)) {
			return false;
		}
		super.remove(items.get(index));
		super.add(after);
		items.set(index, after);
		return true;
	}

	/**
	 * Gets the T item at the given {@code index} in the insertion order. The index should be between 0 (inclusive) and
	 * {@link #size()} (exclusive).
	 *
	 * @param index an index in the insertion order, between 0 (inclusive) and {@link #size()} (exclusive)
	 * @return the item at the given index
	 */
	public T getAt(int index) {
		return items.get(index);
	}

	/**
	 * Returns the first item in the order of this set, or null if the set is empty. This set can never contain null
	 * normally, so if this returns null, that indicates an abnormal situation, and you can opt to throw an Exception.
	 *
	 * @return the first item in the order, or null if this set is empty
	 */
	@Override
	public @Nullable T first() {
		return (size == 0) ? null : items.first();
	}

	@Override
	public void clear(int maximumCapacity) {
		items.clear();
		super.clear(maximumCapacity);
	}

	@Override
	public void clear() {
		items.clear();
		super.clear();
	}

	/**
	 * Gets the ObjectList of items in the order this class will iterate through them.
	 * Returns a direct reference to the same ObjectList this uses, so changes to the returned list will
	 * also change the iteration order here.
	 *
	 * @return the ObjectList of items, in iteration order (usually insertion-order), that this uses
	 */
	@Override
	public ObjectList<T> order() {
		return items;
	}

	/**
	 * Sorts this ObjectOrderedSet in-place by the keys' natural ordering; {@code T} must implement {@link Comparable}.
	 */
	public void sort() {
		items.sort(null);
	}

	/**
	 * Sorts this ObjectOrderedSet in-place by the given Comparator used on the keys. If {@code comp} is null, then this
	 * will sort by the natural ordering of the keys, which requires {@code T} to {@link Comparable}.
	 *
	 * @param comp a Comparator that can compare two {@code T} keys, or null to use the keys' natural ordering
	 */
	public void sort(@Nullable Comparator<? super T> comp) {
		items.sort(comp);
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
		start = Math.max(0, start);
		end = Math.min(items.size(), end);
		for (int i = start; i < end; i++) {
			super.remove(items.get(i));
		}
		items.removeRange(start, end);
	}

	/**
	 * Reduces the size of the set to the specified size. If the set is already smaller than the specified
	 * size, no action is taken.
	 *
	 * @param newSize the target size to try to reach by removing items, if smaller than the current size
	 */
	@Override
	public void truncate(int newSize) {
		if (size > newSize) {
			removeRange(newSize, size);
		}
	}

	/**
	 * Iterates through items in the same order as {@link #order()}.
	 * Reuses one of two iterators, and does not permit nested iteration;
	 * use {@link ObjectOrderedSetIterator#ObjectOrderedSetIterator(ObjectOrderedSet)} to nest iterators.
	 *
	 * @return an {@link Iterator} over the T items in this, in order
	 */
	@Override
	public @NonNull ObjectSetIterator<T> iterator() {
		if (iterator1 == null || iterator2 == null) {
			iterator1 = new ObjectOrderedSetIterator<>(this);
			iterator2 = new ObjectOrderedSetIterator<>(this);
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

	@Override
	public String toString(String itemSeparator) {
		if (size == 0) {
			return "{}";
		}
		ObjectList<T> items = this.items;
		StringBuilder buffer = new StringBuilder(32);
		buffer.append('{');
		buffer.append(items.get(0));
		for (int i = 1; i < size; i++) {
			buffer.append(itemSeparator);
			buffer.append(items.get(i));
		}
		buffer.append('}');
		return buffer.toString();
	}

	@Override
	public int hashCode() {
		int h = size;
		// Iterating over the order rather than the key table avoids wasting time on empty entries.
		ObjectList<@Nullable T> order = items;
		for (int i = 0, n = order.size(); i < n; i++) {
			T key = order.get(i);
			if (key != null) {
				h += key.hashCode();
			}
		}
		// Using any bitwise operation can help by keeping results in int range on GWT.
		// This also can improve the low-order bits on problematic item types like Vector2.
		return h ^ h >>> 16;
	}

	@Override
	public String toString() {
		return toString(", ");
	}

	public static class ObjectOrderedSetIterator<K> extends ObjectSetIterator<K> {
		private final ObjectList<K> items;

		public ObjectOrderedSetIterator(ObjectOrderedSet<K> set) {
			super(set);
			items = set.items;
		}

		@Override
		public void reset() {
			nextIndex = 0;
			hasNext = set.size > 0;
		}

		@Override
		public K next() {
			if (!hasNext) {
				throw new NoSuchElementException();
			}
			if (!valid) {
				throw new RuntimeException("#iterator() cannot be used nested.");
			}
			K key = items.get(nextIndex);
			nextIndex++;
			hasNext = nextIndex < set.size;
			return key;
		}

		@Override
		public void remove() {
			if (nextIndex < 0) {
				throw new IllegalStateException("next must be called before remove.");
			}
			nextIndex--;
			set.remove(items.get(nextIndex));
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
	public static <T> ObjectOrderedSet<T> with() {
		return new ObjectOrderedSet<>(0);
	}

	/**
	 * Creates a new ObjectOrderedSet that holds only the given item, but can be resized.
	 *
	 * @param item one T item
	 * @param <T>  the type of item, typically inferred
	 * @return a new ObjectOrderedSet that holds the given item
	 */
	public static <T> ObjectOrderedSet<T> with(T item) {
		ObjectOrderedSet<T> set = new ObjectOrderedSet<>(1);
		set.add(item);
		return set;
	}

	/**
	 * Creates a new ObjectOrderedSet that holds only the given items, but can be resized.
	 *
	 * @param item0 a T item
	 * @param item1 a T item
	 * @param <T>   the type of item, typically inferred
	 * @return a new ObjectOrderedSet that holds the given items
	 */
	public static <T> ObjectOrderedSet<T> with(T item0, T item1) {
		ObjectOrderedSet<T> set = new ObjectOrderedSet<>(2);
		set.add(item0, item1);
		return set;
	}

	/**
	 * Creates a new ObjectOrderedSet that holds only the given items, but can be resized.
	 *
	 * @param item0 a T item
	 * @param item1 a T item
	 * @param item2 a T item
	 * @param <T>   the type of item, typically inferred
	 * @return a new ObjectOrderedSet that holds the given items
	 */
	public static <T> ObjectOrderedSet<T> with(T item0, T item1, T item2) {
		ObjectOrderedSet<T> set = new ObjectOrderedSet<>(3);
		set.add(item0, item1, item2);
		return set;
	}

	/**
	 * Creates a new ObjectOrderedSet that holds only the given items, but can be resized.
	 *
	 * @param item0 a T item
	 * @param item1 a T item
	 * @param item2 a T item
	 * @param item3 a T item
	 * @param <T>   the type of item, typically inferred
	 * @return a new ObjectOrderedSet that holds the given items
	 */
	public static <T> ObjectOrderedSet<T> with(T item0, T item1, T item2, T item3) {
		ObjectOrderedSet<T> set = new ObjectOrderedSet<>(4);
		set.add(item0, item1, item2, item3);
		return set;
	}

	/**
	 * Creates a new ObjectOrderedSet that holds only the given items, but can be resized.
	 *
	 * @param item0 a T item
	 * @param item1 a T item
	 * @param item2 a T item
	 * @param item3 a T item
	 * @param item4 a T item
	 * @param <T>   the type of item, typically inferred
	 * @return a new ObjectOrderedSet that holds the given items
	 */
	public static <T> ObjectOrderedSet<T> with(T item0, T item1, T item2, T item3, T item4) {
		ObjectOrderedSet<T> set = new ObjectOrderedSet<>(5);
		set.add(item0, item1, item2, item3);
		set.add(item4);
		return set;
	}

	/**
	 * Creates a new ObjectOrderedSet that holds only the given items, but can be resized.
	 *
	 * @param item0 a T item
	 * @param item1 a T item
	 * @param item2 a T item
	 * @param item3 a T item
	 * @param item4 a T item
	 * @param item5 a T item
	 * @param <T>   the type of item, typically inferred
	 * @return a new ObjectOrderedSet that holds the given items
	 */
	public static <T> ObjectOrderedSet<T> with(T item0, T item1, T item2, T item3, T item4, T item5) {
		ObjectOrderedSet<T> set = new ObjectOrderedSet<>(6);
		set.add(item0, item1, item2, item3);
		set.add(item4, item5);
		return set;
	}

	/**
	 * Creates a new ObjectOrderedSet that holds only the given items, but can be resized.
	 *
	 * @param item0 a T item
	 * @param item1 a T item
	 * @param item2 a T item
	 * @param item3 a T item
	 * @param item4 a T item
	 * @param item5 a T item
	 * @param item6 a T item
	 * @param <T>   the type of item, typically inferred
	 * @return a new ObjectOrderedSet that holds the given items
	 */
	public static <T> ObjectOrderedSet<T> with(T item0, T item1, T item2, T item3, T item4, T item5, T item6) {
		ObjectOrderedSet<T> set = new ObjectOrderedSet<>(7);
		set.add(item0, item1, item2, item3);
		set.add(item4, item5, item6);
		return set;
	}

	/**
	 * Creates a new ObjectOrderedSet that holds only the given items, but can be resized.
	 *
	 * @param item0 a T item
	 * @param item1 a T item
	 * @param item2 a T item
	 * @param item3 a T item
	 * @param item4 a T item
	 * @param item5 a T item
	 * @param item6 a T item
	 * @param <T>   the type of item, typically inferred
	 * @return a new ObjectOrderedSet that holds the given items
	 */
	public static <T> ObjectOrderedSet<T> with(T item0, T item1, T item2, T item3, T item4, T item5, T item6, T item7) {
		ObjectOrderedSet<T> set = new ObjectOrderedSet<>(8);
		set.add(item0, item1, item2, item3);
		set.add(item4, item5, item6, item7);
		return set;
	}

	/**
	 * Creates a new ObjectOrderedSet that holds only the given items, but can be resized.
	 * This overload will only be used when an array is supplied and the type of the
	 * items requested is the component type of the array, or if varargs are used and
	 * there are 9 or more arguments.
	 *
	 * @param varargs a T varargs or T array; remember that varargs allocate
	 * @param <T>     the type of item, typically inferred
	 * @return a new ObjectOrderedSet that holds the given items
	 */
	@SafeVarargs
	public static <T> ObjectOrderedSet<T> with(T... varargs) {
		return new ObjectOrderedSet<>(varargs);
	}

	/**
	 * Calls {@link #parse(String, String, PartialParser, boolean)} with brackets set to false.
	 * @param str a String that will be parsed in full
	 * @param delimiter the delimiter between items in str
	 * @param parser a PartialParser that returns a {@code T} item from a section of {@code str}
	 * @return a new collection parsed from str
	 */
	public static <T> ObjectOrderedSet<T> parse(String str, String delimiter, PartialParser<T> parser) {
		return parse(str, delimiter, parser, false);
	}

	/**
	 * Creates a new collection and fills it by calling {@link #addLegible(String, String, PartialParser, int, int)} on
	 * either all of {@code str} (if {@code brackets} is false) or {@code str} without its first and last chars (if
	 * {@code brackets} is true). Each item is expected to be separated by {@code delimiter}.
	 *
	 * @param str a String that will be parsed in full (depending on brackets)
	 * @param delimiter the delimiter between items in str
	 * @param parser a PartialParser that returns a {@code T} item from a section of {@code str}
	 * @param brackets if true, the first and last chars in str will be ignored
	 * @return a new collection parsed from str
	 */
	public static <T> ObjectOrderedSet<T> parse(String str, String delimiter, PartialParser<T> parser, boolean brackets) {
		ObjectOrderedSet<T> c = new ObjectOrderedSet<>();
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
	public static <T> ObjectOrderedSet<T> parse(String str, String delimiter, PartialParser<T> parser, int offset, int length) {
		ObjectOrderedSet<T> c = new ObjectOrderedSet<>();
		c.addLegible(str, delimiter, parser, offset, length);
		return c;
	}
}

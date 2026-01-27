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
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;

import com.github.tommyettinger.function.ObjToObjFunction;

import static com.github.tommyettinger.ds.Utilities.tableSize;

/**
 * A {@link HolderSet} that also stores items in an {@link ObjectList} using the insertion order. This acts like
 * HolderSet instead of like {@link ObjectSet}, with the constructors typically taking an extractor function,
 * {@link #contains(Object)} and {@link #remove(Object)} accepting a K key instead of a T item, and
 * {@link #get(Object)} used to get a T item from a K key. Neither null items nor null keys are allowed.
 * No allocation is done except when growing the table size.
 * <p>
 * {@link #iterator() Iteration} is ordered over items and faster than an unordered set. Items can also be accessed
 * and the order changed using {@link #order()}. There is some additional overhead for put and remove.
 * <p>
 * This class performs fast contains (typically O(1), worst case O(n) but that is rare in practice). Remove is somewhat slower due
 * to {@link #order()}. Add may be slightly slower, depending on hash collisions. Hashcodes are rehashed to reduce
 * collisions and the need to resize. Load factors greater than 0.91 greatly increase the chances to resize to the next higher POT
 * size.
 * <p>
 * Unordered sets and maps are not designed to provide especially fast iteration. Iteration is faster with {@link Ordered} types like
 * HolderOrderedSet and ObjectObjectOrderedMap.
 * <p>
 * You can customize most behavior of this set by extending it. {@link #place(Object)} can be overridden to change how hashCodes
 * are calculated on K keys (which can be useful for types like {@link StringBuilder} that don't implement hashCode()), and
 * {@link #locateKey(Object)} can be overridden to change how equality is calculated for K keys.
 * <p>
 * This implementation uses linear probing with the backward shift algorithm for removal.
 * It tries different hashes from a simple family, with the hash changing on resize.
 * Linear probing continues to work even when all hashCodes collide, just more slowly.
 *
 * @author Nathan Sweet
 * @author Tommy Ettinger
 */
public class HolderOrderedSet<T, K> extends HolderSet<T, K> implements Ordered<T> {

	protected final ObjectList<T> items;

	/**
	 * Creates a new set with an initial capacity of {@link Utilities#getDefaultTableCapacity()} and a load factor of {@link Utilities#getDefaultLoadFactor()}. This does not set the
	 * extractor, so the HolderSet will not be usable until {@link #setExtractor(ObjToObjFunction)} is called with
	 * a valid ObjToObjFunction that gets K keys from T items.
	 *
	 * @param type either {@link OrderType#BAG} to use unreliable ordering with faster deletion, or anything else to
	 *             use a list type that takes longer to delete but maintains insertion order reliably
	 */
	public HolderOrderedSet(OrderType type) {
		super();
		items = type == OrderType.BAG ? new ObjectBag<>() : new ObjectList<>();
	}

	/**
	 * Creates a new set with an initial capacity of {@link Utilities#getDefaultTableCapacity()} and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 *
	 * @param extractor a function that will be used to extract K keys from the T items put into this
	 * @param type      either {@link OrderType#BAG} to use unreliable ordering with faster deletion, or anything else to
	 *                  use a list type that takes longer to delete but maintains insertion order reliably
	 */
	public HolderOrderedSet(ObjToObjFunction<T, K> extractor, OrderType type) {
		super(extractor);
		items = type == OrderType.BAG ? new ObjectBag<>() : new ObjectList<>();
	}

	/**
	 * Creates a new set with a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 *
	 * @param extractor       a function that will be used to extract K keys from the T items put into this
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 * @param type            either {@link OrderType#BAG} to use unreliable ordering with faster deletion, or anything else to
	 *                        use a list type that takes longer to delete but maintains insertion order reliably
	 */
	public HolderOrderedSet(ObjToObjFunction<T, K> extractor, int initialCapacity, OrderType type) {
		super(extractor, initialCapacity);
		items = type == OrderType.BAG ? new ObjectBag<>(initialCapacity) : new ObjectList<>(initialCapacity);

	}

	/**
	 * Creates a new set with the specified initial capacity and load factor. This set will hold initialCapacity items before
	 * growing the backing table.
	 *
	 * @param extractor       a function that will be used to extract K keys from the T items put into this
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 * @param loadFactor      what fraction of the capacity can be filled before this has to resize; 0 &lt; loadFactor &lt;= 1
	 * @param type            either {@link OrderType#BAG} to use unreliable ordering with faster deletion, or anything else to
	 *                        use a list type that takes longer to delete but maintains insertion order reliably
	 */
	public HolderOrderedSet(ObjToObjFunction<T, K> extractor, int initialCapacity, float loadFactor, OrderType type) {
		super(extractor, initialCapacity, loadFactor);
		items = type == OrderType.BAG ? new ObjectBag<>(initialCapacity) : new ObjectList<>(initialCapacity);
	}

	/**
	 * Creates a new instance containing the items in the specified iterator.
	 *
	 * @param coll an iterator that will have its remaining contents added to this
	 * @param type either {@link OrderType#BAG} to use unreliable ordering with faster deletion, or anything else to
	 *             use a list type that takes longer to delete but maintains insertion order reliably
	 */
	public HolderOrderedSet(ObjToObjFunction<T, K> extractor, Iterator<? extends T> coll, OrderType type) {
		this(extractor, type);
		addAll(coll);
	}

	/**
	 * Creates a new set identical to the specified set.
	 * This doesn't copy the extractor; instead it references the same ObjToObjFunction from the argument.
	 * This can have issues if the extractor causes side effects or is stateful.
	 *
	 * @param set  another HolderOrderedSet which will have its contents copied
	 * @param type either {@link OrderType#BAG} to use unreliable ordering with faster deletion, or anything else to
	 *             use a list type that takes longer to delete but maintains insertion order reliably
	 */
	public HolderOrderedSet(HolderOrderedSet<T, K> set, OrderType type) {
		super(set);
		items = type == OrderType.BAG ? new ObjectBag<>(set.items) : new ObjectList<>(set.items);
	}

	/**
	 * Creates a new set that contains all distinct elements in {@code coll}, using {@code extractor} to get the keys that determine distinctness.
	 *
	 * @param extractor a function that will be used to extract K keys from the T items in coll
	 * @param coll      a Collection of T items; depending on extractor, some different T items may not be added because their K key is equal
	 * @param type      either {@link OrderType#BAG} to use unreliable ordering with faster deletion, or anything else to
	 *                  use a list type that takes longer to delete but maintains insertion order reliably
	 */
	public HolderOrderedSet(ObjToObjFunction<T, K> extractor, Collection<? extends T> coll, OrderType type) {
		this(extractor, coll.size(), type);
		addAll(coll);
	}

	/**
	 * Creates a new set that contains all distinct elements in {@code items}, using {@code extractor} to get the keys that determine distinctness.
	 *
	 * @param extractor a function that will be used to extract K keys from the T items in coll
	 * @param items     an array of T items; depending on extractor, some different T items may not be added because their K key is equal
	 * @param type      either {@link OrderType#BAG} to use unreliable ordering with faster deletion, or anything else to
	 *                  use a list type that takes longer to delete but maintains insertion order reliably
	 */
	public HolderOrderedSet(ObjToObjFunction<T, K> extractor, T[] items, OrderType type) {
		this(extractor, items.length, type);
		addAll(items);
	}

	/**
	 * Creates a new set by copying {@code count} items from the given Ordered, starting at {@code offset} in that Ordered,
	 * into this, using {@code extractor} to get the keys that determine distinctness.
	 *
	 * @param extractor a function that will be used to extract K keys from the T items in coll
	 * @param other     another Ordered of the same type
	 * @param offset    the first index in other's ordering to draw an item from
	 * @param count     how many items to copy from other
	 * @param type      either {@link OrderType#BAG} to use unreliable ordering with faster deletion, or anything else to
	 *                  use a list type that takes longer to delete but maintains insertion order reliably
	 */
	public HolderOrderedSet(ObjToObjFunction<T, K> extractor, Ordered<T> other, int offset, int count, OrderType type) {
		this(extractor, count, type);
		addAll(0, other, offset, count);
	}

	// default order type

	/**
	 * Creates a new set with an initial capacity of {@link Utilities#getDefaultTableCapacity()} and a load factor of {@link Utilities#getDefaultLoadFactor()}. This does not set the
	 * extractor, so the HolderSet will not be usable until {@link #setExtractor(ObjToObjFunction)} is called with
	 * a valid ObjToObjFunction that gets K keys from T items.
	 */
	public HolderOrderedSet() {
		super();
		items = new ObjectList<>();
	}

	/**
	 * Creates a new set with an initial capacity of {@link Utilities#getDefaultTableCapacity()} and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 *
	 * @param extractor a function that will be used to extract K keys from the T items put into this
	 */
	public HolderOrderedSet(ObjToObjFunction<T, K> extractor) {
		super(extractor);
		items = new ObjectList<>();
	}

	/**
	 * Creates a new set with a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 *
	 * @param extractor       a function that will be used to extract K keys from the T items put into this
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 */
	public HolderOrderedSet(ObjToObjFunction<T, K> extractor, int initialCapacity) {
		super(extractor, initialCapacity);
		items = new ObjectList<>(initialCapacity);
	}

	/**
	 * Creates a new set with the specified initial capacity and load factor. This set will hold initialCapacity items before
	 * growing the backing table.
	 *
	 * @param extractor       a function that will be used to extract K keys from the T items put into this
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 * @param loadFactor      what fraction of the capacity can be filled before this has to resize; 0 &lt; loadFactor &lt;= 1
	 */
	public HolderOrderedSet(ObjToObjFunction<T, K> extractor, int initialCapacity, float loadFactor) {
		super(extractor, initialCapacity, loadFactor);
		items = new ObjectList<>(initialCapacity);
	}

	/**
	 * Creates a new instance containing the items in the specified iterator.
	 *
	 * @param coll an iterator that will have its remaining contents added to this
	 */
	public HolderOrderedSet(ObjToObjFunction<T, K> extractor, Iterator<? extends T> coll) {
		this(extractor);
		addAll(coll);
	}

	/**
	 * Creates a new set identical to the specified set.
	 * This doesn't copy the extractor; instead it references the same ObjToObjFunction from the argument.
	 * This can have issues if the extractor causes side effects or is stateful.
	 */
	public HolderOrderedSet(HolderOrderedSet<T, K> set) {
		super(set);
		items = set.getOrderType() == OrderType.BAG ? new ObjectBag<>(set.items) : new ObjectList<>(set.items);
	}

	/**
	 * Creates a new set that contains all distinct elements in {@code coll}, using {@code extractor} to get the keys that determine distinctness.
	 *
	 * @param extractor a function that will be used to extract K keys from the T items in coll
	 * @param coll      a Collection of T items; depending on extractor, some different T items may not be added because their K key is equal
	 */
	public HolderOrderedSet(ObjToObjFunction<T, K> extractor, Collection<? extends T> coll) {
		this(extractor, coll.size());
		addAll(coll);
	}

	/**
	 * Creates a new set that contains all distinct elements in {@code items}, using {@code extractor} to get the keys that determine distinctness.
	 *
	 * @param extractor a function that will be used to extract K keys from the T items in coll
	 * @param items     an array of T items; depending on extractor, some different T items may not be added because their K key is equal
	 */
	public HolderOrderedSet(ObjToObjFunction<T, K> extractor, T[] items) {
		this(extractor, items.length);
		addAll(items);
	}

	/**
	 * Creates a new set by copying {@code count} items from the given Ordered, starting at {@code offset} in that Ordered,
	 * into this, using {@code extractor} to get the keys that determine distinctness.
	 *
	 * @param extractor a function that will be used to extract K keys from the T items in coll
	 * @param other     another Ordered of the same type
	 * @param offset    the first index in other's ordering to draw an item from
	 * @param count     how many items to copy from other
	 */
	public HolderOrderedSet(ObjToObjFunction<T, K> extractor, Ordered<T> other, int offset, int count) {
		this(extractor, count);
		addAll(0, other, offset, count);
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
		if (key == null) return false;
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

	public boolean addAll(HolderOrderedSet<T, ?> set) {
		ensureCapacity(set.size);
		ObjectList<T> si = set.items;
		int oldSize = size;
		for (int i = 0, n = si.size(); i < n; i++) {
			add(si.get(i));
		}
		return size != oldSize;
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

	/**
	 * Takes a K key to remove from this HolderOrderedSet, not a T item.
	 *
	 * @param key should be a K key, not a T item
	 * @return true if this was modified
	 */
	@Override
	public boolean remove(Object key) {
		return key != null && items.remove(super.get(key)) && super.remove(key);
	}

	/**
	 * Removes and returns the item at the given index in this set's order.
	 *
	 * @param index the index of the item to remove
	 * @return the removed item
	 */
	public T removeAt(int index) {
		T item = items.removeAt(index);
		assert extractor != null;
		super.remove(extractor.apply(item));
		return item;
	}

	/**
	 * Gets the first item in the order. If the set is empty, this returns null. Note that this set cannot contain null.
	 *
	 * @return the first item in this set's order
	 */
	@Override
	public T first() {
		if (size == 0) return null;
		return items.get(0);
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
		if (before == null || after == null || contains(extractor.apply(after))) {
			return false;
		}
		if (!super.remove(extractor.apply(before))) {
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
		if (after == null || index < 0 || index >= size || contains(extractor.apply(after))) {
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
	 * Gets the ObjectList of T items in the order this class will iterate through them.
	 * Returns a direct reference to the same ObjectList this uses, so changes to the returned list will
	 * also change the iteration order here.
	 *
	 * @return the ObjectList of T items, in iteration order (usually insertion-order), that this uses
	 */
	@Override
	public ObjectList<T> order() {
		return items;
	}

	/**
	 * Sorts this ObjectOrderedSet in-place by the T items' natural ordering; {@code T} must implement {@link Comparable}.
	 */
	public void sort() {
		items.sort(null);
	}

	/**
	 * Sorts this ObjectOrderedSet in-place by the given Comparator used on the T items. If {@code comp} is null, then this
	 * will sort by the natural ordering of the items, which requires {@code T} to {@link Comparable}.
	 *
	 * @param comp a Comparator that can compare two {@code T} items, or null to use the items' natural ordering
	 */
	public void sort(Comparator<? super T> comp) {
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
	 *
	 * @return an {@link Iterator} over the T items in this, in order
	 */
	@Override
	public HolderSetIterator<T, K> iterator() {
		return new HolderOrderedSetIterator<>(this);
	}

	@Override
	public int hashCode() {
		int h = size;
		if (extractor != null) {
			ObjectList<T> order = items;
			for (int i = 0, n = order.size(); i < n; i++) {
				T key = order.get(i);
				if (key != null) {
					h += extractor.apply(key).hashCode();
				}
			}
		}
		return h ^ h >>> 16;
	}

	@Override
	public String toString(String itemSeparator) {
		if (size == 0) {
			return "{}";
		}
		ObjectList<T> items = this.items;
		StringBuilder buffer = new StringBuilder(6 * size());
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
	public String toString() {
		return toString(", ");
	}

	public static class HolderOrderedSetIterator<T, K> extends HolderSetIterator<T, K> {
		protected final ObjectList<T> items;

		public HolderOrderedSetIterator(HolderOrderedSet<T, K> set) {
			super(set);
			items = set.items;
		}

		@Override
		public void reset() {
			nextIndex = 0;
			hasNext = set.size > 0;
		}

		@Override
		public T next() {
			if (!hasNext) {
				throw new NoSuchElementException();
			}
			T key = items.get(nextIndex);
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
			assert set.extractor != null;
			set.remove(set.extractor.apply(items.get(nextIndex)));
		}
	}

	/**
	 * Constructs an empty set given only an extractor function.
	 * This is usually less useful than just using the constructor, but can be handy
	 * in some code-generation scenarios when you don't know how many arguments you will have.
	 *
	 * @param extractor a ObjToObjFunction that takes a T and gets a unique K from it; often a method reference
	 * @param <T>       the type of items; must be given explicitly
	 * @param <K>       the type of keys that extractor pulls from T items
	 * @return a new set containing nothing
	 */
	public static <T, K> HolderOrderedSet<T, K> with(ObjToObjFunction<T, K> extractor) {
		return new HolderOrderedSet<>(extractor, 0);
	}

	/**
	 * Creates a new HolderOrderedSet that holds only the given item, but can be resized.
	 *
	 * @param extractor a ObjToObjFunction that takes a T and gets a unique K from it; often a method reference
	 * @param item      one T item
	 * @param <T>       the type of item, typically inferred
	 * @param <K>       the type of keys that extractor pulls from T items
	 * @return a new HolderOrderedSet that holds the given item
	 */
	public static <T, K> HolderOrderedSet<T, K> with(ObjToObjFunction<T, K> extractor, T item) {
		HolderOrderedSet<T, K> set = new HolderOrderedSet<>(extractor, 1);
		set.add(item);
		return set;
	}

	/**
	 * Creates a new HolderOrderedSet that holds only the given items, but can be resized.
	 *
	 * @param extractor a ObjToObjFunction that takes a T and gets a unique K from it; often a method reference
	 * @param item0     a T item
	 * @param item1     a T item
	 * @param <T>       the type of item, typically inferred
	 * @param <K>       the type of keys that extractor pulls from T items
	 * @return a new HolderOrderedSet that holds the given items
	 */
	public static <T, K> HolderOrderedSet<T, K> with(ObjToObjFunction<T, K> extractor, T item0, T item1) {
		HolderOrderedSet<T, K> set = new HolderOrderedSet<>(extractor, 2);
		set.add(item0, item1);
		return set;
	}

	/**
	 * Creates a new HolderOrderedSet that holds only the given items, but can be resized.
	 *
	 * @param extractor a ObjToObjFunction that takes a T and gets a unique K from it; often a method reference
	 * @param item0     a T item
	 * @param item1     a T item
	 * @param item2     a T item
	 * @param <T>       the type of item, typically inferred
	 * @param <K>       the type of keys that extractor pulls from T items
	 * @return a new HolderOrderedSet that holds the given items
	 */
	public static <T, K> HolderOrderedSet<T, K> with(ObjToObjFunction<T, K> extractor, T item0, T item1, T item2) {
		HolderOrderedSet<T, K> set = new HolderOrderedSet<>(extractor, 3);
		set.add(item0, item1, item2);
		return set;
	}

	/**
	 * Creates a new HolderOrderedSet that holds only the given items, but can be resized.
	 *
	 * @param extractor a ObjToObjFunction that takes a T and gets a unique K from it; often a method reference
	 * @param item0     a T item
	 * @param item1     a T item
	 * @param item2     a T item
	 * @param item3     a T item
	 * @param <T>       the type of item, typically inferred
	 * @param <K>       the type of keys that extractor pulls from T items
	 * @return a new HolderOrderedSet that holds the given items
	 */
	public static <T, K> HolderOrderedSet<T, K> with(ObjToObjFunction<T, K> extractor, T item0, T item1, T item2, T item3) {
		HolderOrderedSet<T, K> set = new HolderOrderedSet<>(extractor, 4);
		set.add(item0, item1, item2, item3);
		return set;
	}

	/**
	 * Creates a new HolderOrderedSet that holds only the given items, but can be resized.
	 *
	 * @param extractor a ObjToObjFunction that takes a T and gets a unique K from it; often a method reference
	 * @param item0     a T item
	 * @param item1     a T item
	 * @param item2     a T item
	 * @param item3     a T item
	 * @param item4     a T item
	 * @param <T>       the type of item, typically inferred
	 * @param <K>       the type of keys that extractor pulls from T items
	 * @return a new HolderOrderedSet that holds the given items
	 */
	public static <T, K> HolderOrderedSet<T, K> with(ObjToObjFunction<T, K> extractor, T item0, T item1, T item2, T item3, T item4) {
		HolderOrderedSet<T, K> set = new HolderOrderedSet<>(extractor, 5);
		set.add(item0, item1, item2, item3);
		set.add(item4);
		return set;
	}

	/**
	 * Creates a new HolderOrderedSet that holds only the given items, but can be resized.
	 *
	 * @param extractor a ObjToObjFunction that takes a T and gets a unique K from it; often a method reference
	 * @param item0     a T item
	 * @param item1     a T item
	 * @param item2     a T item
	 * @param item3     a T item
	 * @param item4     a T item
	 * @param item5     a T item
	 * @param <T>       the type of item, typically inferred
	 * @param <K>       the type of keys that extractor pulls from T items
	 * @return a new HolderOrderedSet that holds the given items
	 */
	public static <T, K> HolderOrderedSet<T, K> with(ObjToObjFunction<T, K> extractor, T item0, T item1, T item2, T item3, T item4, T item5) {
		HolderOrderedSet<T, K> set = new HolderOrderedSet<>(extractor, 6);
		set.add(item0, item1, item2, item3);
		set.add(item4, item5);
		return set;
	}

	/**
	 * Creates a new HolderOrderedSet that holds only the given items, but can be resized.
	 *
	 * @param extractor a ObjToObjFunction that takes a T and gets a unique K from it; often a method reference
	 * @param item0     a T item
	 * @param item1     a T item
	 * @param item2     a T item
	 * @param item3     a T item
	 * @param item4     a T item
	 * @param item5     a T item
	 * @param item6     a T item
	 * @param <T>       the type of item, typically inferred
	 * @param <K>       the type of keys that extractor pulls from T items
	 * @return a new HolderOrderedSet that holds the given items
	 */
	public static <T, K> HolderOrderedSet<T, K> with(ObjToObjFunction<T, K> extractor, T item0, T item1, T item2, T item3, T item4, T item5, T item6) {
		HolderOrderedSet<T, K> set = new HolderOrderedSet<>(extractor, 7);
		set.add(item0, item1, item2, item3);
		set.add(item4, item5, item6);
		return set;
	}

	/**
	 * Creates a new HolderOrderedSet that holds only the given items, but can be resized.
	 *
	 * @param extractor a ObjToObjFunction that takes a T and gets a unique K from it; often a method reference
	 * @param item0     a T item
	 * @param item1     a T item
	 * @param item2     a T item
	 * @param item3     a T item
	 * @param item4     a T item
	 * @param item5     a T item
	 * @param item6     a T item
	 * @param <T>       the type of item, typically inferred
	 * @param <K>       the type of keys that extractor pulls from T items
	 * @return a new HolderOrderedSet that holds the given items
	 */
	public static <T, K> HolderOrderedSet<T, K> with(ObjToObjFunction<T, K> extractor, T item0, T item1, T item2, T item3, T item4, T item5, T item6, T item7) {
		HolderOrderedSet<T, K> set = new HolderOrderedSet<>(extractor, 8);
		set.add(item0, item1, item2, item3);
		set.add(item4, item5, item6, item7);
		return set;
	}

	/**
	 * Creates a new HolderOrderedSet that holds only the given items, but can be resized.
	 * This overload will only be used when an array is supplied and the type of the
	 * items requested is the component type of the array, or if varargs are used and
	 * there are 9 or more arguments.
	 *
	 * @param extractor a ObjToObjFunction that takes a T and gets a unique K from it; often a method reference
	 * @param varargs   a T varargs or T array; remember that varargs allocate
	 * @param <T>       the type of item, typically inferred
	 * @param <K>       the type of keys that extractor pulls from T items
	 * @return a new HolderOrderedSet that holds the given items
	 */
	@SafeVarargs
	public static <T, K> HolderOrderedSet<T, K> with(ObjToObjFunction<T, K> extractor, T... varargs) {
		return new HolderOrderedSet<>(extractor, varargs);
	}

	/**
	 * Calls {@link #parse(ObjToObjFunction, String, String, PartialParser, boolean)} with brackets set to false.
	 *
	 * @param extractor a ObjToObjFunction that takes a T and gets a unique K from it; often a method reference
	 * @param str       a String that will be parsed in full
	 * @param delimiter the delimiter between items in str
	 * @param parser    a PartialParser that returns a {@code T} item from a section of {@code str}
	 * @param <T>       the type of item, typically inferred
	 * @param <K>       the type of keys that extractor pulls from T items
	 * @return a new collection parsed from str
	 */
	public static <T, K> HolderOrderedSet<T, K> parse(ObjToObjFunction<T, K> extractor, String str, String delimiter, PartialParser<T> parser) {
		return parse(extractor, str, delimiter, parser, false);
	}

	/**
	 * Creates a new HolderOrderedSet using {@code extractor} and fills it by calling
	 * {@link #addLegible(String, String, PartialParser, int, int)} on
	 * either all of {@code str} (if {@code brackets} is false) or {@code str} without its first and last chars (if
	 * {@code brackets} is true). Each item is expected to be separated by {@code delimiter}.
	 *
	 * @param extractor a ObjToObjFunction that takes a T and gets a unique K from it; often a method reference
	 * @param str       a String that will be parsed in full (depending on brackets)
	 * @param delimiter the delimiter between items in str
	 * @param parser    a PartialParser that returns a {@code T} item from a section of {@code str}
	 * @param brackets  if true, the first and last chars in str will be ignored
	 * @param <T>       the type of item, typically inferred
	 * @param <K>       the type of keys that extractor pulls from T items
	 * @return a new collection parsed from str
	 */
	public static <T, K> HolderOrderedSet<T, K> parse(ObjToObjFunction<T, K> extractor, String str, String delimiter, PartialParser<T> parser, boolean brackets) {
		HolderOrderedSet<T, K> c = new HolderOrderedSet<>(extractor);
		if (brackets)
			c.addLegible(str, delimiter, parser, 1, str.length() - 1);
		else
			c.addLegible(str, delimiter, parser);
		return c;
	}

	/**
	 * Creates a new HolderOrderedSet using {@code extractor} and fills it by calling
	 * {@link #addLegible(String, String, PartialParser, int, int)}
	 * with the other five parameters as-is.
	 *
	 * @param extractor a ObjToObjFunction that takes a T and gets a unique K from it; often a method reference
	 * @param str       a String that will have the given section parsed
	 * @param delimiter the delimiter between items in str
	 * @param parser    a PartialParser that returns a {@code T} item from a section of {@code str}
	 * @param offset    the first position to parse in str, inclusive
	 * @param length    how many chars to parse, starting from offset
	 * @param <T>       the type of item, typically inferred
	 * @param <K>       the type of keys that extractor pulls from T items
	 * @return a new collection parsed from str
	 */
	public static <T, K> HolderOrderedSet<T, K> parse(ObjToObjFunction<T, K> extractor, String str, String delimiter, PartialParser<T> parser, int offset, int length) {
		HolderOrderedSet<T, K> c = new HolderOrderedSet<>(extractor);
		c.addLegible(str, delimiter, parser, offset, length);
		return c;
	}
}

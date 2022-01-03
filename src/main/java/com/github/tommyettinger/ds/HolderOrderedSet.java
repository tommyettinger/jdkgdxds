/*
 * Copyright (c) 2022 See AUTHORS file.
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

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;

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
 * This implementation uses linear probing with the backward shift algorithm for removal. Hashcodes are rehashed using Fibonacci
 * hashing, instead of the more common power-of-two mask, to better distribute poor hashCodes (see <a href=
 * "https://probablydance.com/2018/06/16/fibonacci-hashing-the-optimization-that-the-world-forgot-or-a-better-alternative-to-integer-modulo/">Malte
 * Skarupke's blog post</a>). Linear probing continues to work even when all hashCodes collide, just more slowly.
 *
 * @author Nathan Sweet
 * @author Tommy Ettinger
 */
public class HolderOrderedSet<T, K> extends HolderSet<T, K> implements Ordered<T> {

	protected final ObjectList<T> items;

	/**
	 * Creates a new set with an initial capacity of 51 and a load factor of {@link Utilities#getDefaultLoadFactor()}. This does not set the
	 * extractor, so the HolderSet will not be usable until {@link #setExtractor(Function)} is called with
	 * a valid Function that gets K keys from T items.
	 */
	public HolderOrderedSet () {
		super();
		items = new ObjectList<>();
	}

	/**
	 * Creates a new set with an initial capacity of 51 and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 * @param extractor a function that will be used to extract K keys from the T items put into this
	 */
	public HolderOrderedSet (Function<T, K> extractor) {
		super(extractor);
		items = new ObjectList<>();
	}

	/**
	 * Creates a new set with a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 *
	 * @param extractor a function that will be used to extract K keys from the T items put into this
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 */
	public HolderOrderedSet (Function<T, K> extractor, int initialCapacity) {
		super(extractor, initialCapacity);
		items = new ObjectList<>(initialCapacity);
	}

	/**
	 * Creates a new set with the specified initial capacity and load factor. This set will hold initialCapacity items before
	 * growing the backing table.
	 *
	 * @param extractor a function that will be used to extract K keys from the T items put into this
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 * @param loadFactor what fraction of the capacity can be filled before this has to resize; 0 &lt; loadFactor &lt;= 1
	 */
	public HolderOrderedSet (Function<T, K> extractor, int initialCapacity, float loadFactor) {
		super(extractor, initialCapacity, loadFactor);
		items = new ObjectList<>(initialCapacity);
	}

	/**
	 * Creates a new set identical to the specified set.
	 * This doesn't copy the extractor; instead it references the same Function from the argument.
	 * This can have issues if the extractor causes side effects or is stateful.
	 */
	public HolderOrderedSet (HolderOrderedSet<T, K> set) {
		super(set);
		items = new ObjectList<>(set.items);
	}

	/**
	 * Creates a new set that contains all distinct elements in {@code coll}, using {@code extractor} to get the keys that determine distinctness.
	 *
	 * @param extractor a function that will be used to extract K keys from the T items in coll
	 * @param coll      a Collection of T items; depending on extractor, some different T items may not be added because their K key is equal
	 */
	public HolderOrderedSet (Function<T, K> extractor, Collection<? extends T> coll) {
		this(extractor, coll.size());
		addAll(coll);
	}

	/**
	 * Creates a new set that contains all distinct elements in {@code items}, using {@code extractor} to get the keys that determine distinctness.
	 *
	 * @param extractor a function that will be used to extract K keys from the T items in coll
	 * @param items     an array of T items; depending on extractor, some different T items may not be added because their K key is equal
	 */
	public HolderOrderedSet (Function<T, K> extractor, T[] items) {
		this(extractor, items.length);
		addAll(items);
	}

	/**
	 * Creates a new set by copying {@code count} items from the given Ordered, starting at {@code offset} in that Ordered,
	 * into this, using {@code extractor} to get the keys that determine distinctness.
	 * @param extractor a function that will be used to extract K keys from the T items in coll
	 * @param other another Ordered of the same type
	 * @param offset the first index in other's ordering to draw an item from
	 * @param count how many items to copy from other
	 */
	public HolderOrderedSet (Function<T, K> extractor, Ordered<T> other, int offset, int count) {
		this(extractor, count);
		addAll(0, other, offset, count);
	}

	@Override
	public boolean add (T key) {
		return super.add(key) && items.add(key);
	}

	/**
	 * Sets the key at the specified index. Returns true if the key was not already in the set. If this set already contains the
	 * key, the existing key's index is changed if needed and false is returned. Note, the order of the parameters matches the
	 * order in {@link ObjectList} and the rest of the JDK, not OrderedSet in libGDX.
	 *
	 * @param index where in the iteration order to add the given key, or to move it if already present
	 * @param key what T item to try to add, if not already present
	 * @return true if the key was added for the first time, or false if the key was already present (even if moved)
	 */
	public boolean add (int index, T key) {
		if (!super.add(key)) {
			int oldIndex = items.indexOf(key);
			if (oldIndex != index) { items.add(index, items.remove(oldIndex)); }
			return false;
		}
		items.add(index, key);
		return true;
	}

	public boolean addAll (HolderOrderedSet<T, ?> set) {
		ensureCapacity(set.size);
		ObjectList<T> si = set.items;
		int oldSize = size;
		for (int i = 0, n = si.size(); i < n; i++) { add(si.get(i)); }
		return size != oldSize;
	}

	/**
	 * Adds up to {@code count} items, starting from {@code offset}, in the Ordered {@code other} to this set,
	 * inserting at the end of the iteration order.
	 *
	 * @param other          a non-null {@link Ordered} of {@code T}
	 * @param offset         the first index in {@code other} to use
	 * @param count          how many indices in {@code other} to use
	 * @return true if this is modified by this call, as {@link #addAll(Collection)} does
	 */
	public boolean addAll (Ordered<T> other, int offset, int count) {
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
	public boolean addAll (int insertionIndex, Ordered<T> other, int offset, int count) {
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
	public boolean remove (Object key) {
		return items.remove(super.get(key)) && super.remove(key);
	}

	/**
	 * Removes and returns the item at the given index in this set's order.
	 * @param index the index of the item to remove
	 * @return the removed item
	 */
	public T removeAt (int index) {
		T key = items.removeAt(index);
		assert extractor != null;
		super.remove(extractor.apply(key));
		return key;
	}

	/**
	 * Gets the first item in the order.
	 * @throws IllegalStateException if this is empty.
	 * @return the first item in this set's order
	 */
	@Override
	public T first () {
		if(size == 0) throw new IllegalStateException("HolderOrderedSet is empty.");
		return items.get(0);
	}

	/**
	 * Increases the size of the backing array to accommodate the specified number of additional items / loadFactor. Useful before
	 * adding many items to avoid multiple backing array resizes.
	 *
	 * @param additionalCapacity how many additional items this should be able to hold without resizing (probably)
	 */
	@Override
	public void ensureCapacity (int additionalCapacity) {
		int tableSize = tableSize(size + additionalCapacity, loadFactor);
		if (keyTable.length < tableSize) { resize(tableSize); }
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
	public boolean alter (T before, T after) {
		if (contains(after)) { return false; }
		if (!super.remove(before)) { return false; }
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
	public boolean alterAt (int index, T after) {
		if (index < 0 || index >= size || contains(after)) { return false; }
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
	public T getAt (int index) {
		return items.get(index);
	}

	@Override
	public void clear (int maximumCapacity) {
		items.clear();
		super.clear(maximumCapacity);
	}

	@Override
	public void clear () {
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
	public ObjectList<T> order () {
		return items;
	}

	/**
	 * Sorts this ObjectOrderedSet in-place by the keys' natural ordering; {@code T} must implement {@link Comparable}.
	 */
	public void sort () {
		items.sort(null);
	}

	/**
	 * Sorts this ObjectOrderedSet in-place by the given Comparator used on the keys. If {@code comp} is null, then this
	 * will sort by the natural ordering of the keys, which requires {@code T} to {@link Comparable}.
	 *
	 * @param comp a Comparator that can compare two {@code T} keys, or null to use the keys' natural ordering
	 */
	public void sort (@Nullable Comparator<? super T> comp) {
		items.sort(comp);
	}

	@Override
	public void removeRange (int start, int end) {
		start = Math.max(0, start);
		end = Math.min(items.size(), end);
		for (int i = start; i < end; i++) {
			super.remove(items.get(i));
		}
		items.removeRange(start, end);
	}

	/**
	 * Iterates through items in the same order as {@link #order()}.
	 * Reuses one of two iterators, and does not permit nested iteration;
	 * use {@link HolderOrderedSetIterator#HolderOrderedSetIterator(HolderOrderedSet)} to nest iterators.
	 *
	 * @return an {@link Iterator} over the T items in this, in order
	 */
	@Override
	public Iterator<T> iterator () {
		if (iterator1 == null || iterator2 == null) {
			iterator1 = new HolderOrderedSetIterator<>(this);
			iterator2 = new HolderOrderedSetIterator<>(this);
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
	public String toString (String separator) {
		if (size == 0) { return "{}"; }
		ObjectList<T> items = this.items;
		StringBuilder buffer = new StringBuilder(32);
		buffer.append('{');
		buffer.append(items.get(0));
		for (int i = 1; i < size; i++) {
			buffer.append(separator);
			buffer.append(items.get(i));
		}
		buffer.append('}');
		return buffer.toString();
	}

	@Override
	public String toString () {
		return toString(", ");
	}

	public static class HolderOrderedSetIterator<T, K> extends HolderSetIterator<T, K> {
		private final ObjectList<T> items;

		public HolderOrderedSetIterator (HolderOrderedSet<T, K> set) {
			super(set);
			items = set.items;
		}

		@Override
		public void reset () {
			nextIndex = 0;
			hasNext = set.size > 0;
		}

		@Override
		public T next () {
			if (!hasNext) { throw new NoSuchElementException(); }
			if (!valid) { throw new RuntimeException("#iterator() cannot be used nested."); }
			T key = items.get(nextIndex);
			nextIndex++;
			hasNext = nextIndex < set.size;
			return key;
		}

		@Override
		public void remove () {
			if (nextIndex < 0) { throw new IllegalStateException("next must be called before remove."); }
			nextIndex--;
			set.remove(items.get(nextIndex));
		}
	}

	public static <T, K> HolderOrderedSet<T, K> with (Function<T, K> extractor, T item) {
		HolderOrderedSet<T, K> set = new HolderOrderedSet<>(extractor, 1);
		set.add(item);
		return set;
	}

	@SafeVarargs
	public static <T, K> HolderOrderedSet<T, K> with (Function<T, K> extractor, T... array) {
		return new HolderOrderedSet<>(extractor, array);
	}
}

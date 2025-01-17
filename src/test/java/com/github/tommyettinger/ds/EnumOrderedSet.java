/*
 * Copyright (c) 2022-2023 See AUTHORS file.
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

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A {@link EnumSet} that also stores keys in an {@link ObjectList} using the insertion order. Null keys are not allowed. No
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
 * This implementation uses linear probing with the backward shift algorithm for removal.
 * It tries different hashes from a simple family, with the hash changing on resize.
 * Linear probing continues to work even when all hashCodes collide; it just works more slowly in that case.
 *
 * @author Nathan Sweet
 * @author Tommy Ettinger
 */
public class EnumOrderedSet extends EnumSet implements Ordered<Enum<?>> {

	protected final ObjectList<@NonNull Enum<?>> ordering;


	/**
	 * Empty constructor; using this will postpone allocating any internal arrays until {@link #add(Enum)} is first called
	 * (potentially indirectly).
	 */
	public EnumOrderedSet () {
		super();
		ordering = new ObjectList<>();
	}

	/**
	 * Initializes this set so that it has exactly enough capacity as needed to contain each Enum constant defined in
	 * {@code universe}, assuming universe stores every possible constant in one Enum type.
	 * You almost always obtain universe from calling {@code values()} on an Enum type, and you can share one
	 * reference to one Enum array across many EnumSet instances if you don't modify the shared array. Sharing the same
	 * universe helps save some memory if you have (very) many EnumSet instances.
	 * <br>
	 * Because the {@code boolean} parameter here is easy to forget, you may want to prefer calling {@link #noneOf(Enum[])}
	 * instead of using this directly.
	 *
	 * @param universe almost always, the result of calling {@code values()} on an Enum type; used directly, not copied
	 * @param ignoredToDistinguish an ignored boolean that differentiates this constructor, which defined a key universe,
	 *                               from one that takes contents
	 */
	public EnumOrderedSet (Enum<?>@Nullable [] universe, boolean ignoredToDistinguish) {
		super();
		if(universe == null) {
			ordering = new ObjectList<>();
			return;
		}
		this.universe = universe;
		table = new int[universe.length + 31 >>> 5];
		ordering = new ObjectList<>(universe.length);
	}

	/**
	 * Initializes this set so that it has exactly enough capacity as needed to contain each Enum constant defined by the
	 * Class {@code universeClass}, assuming universeClass is non-null. This simply calls {@link #EnumOrderedSet(Enum[], boolean)}
	 * for convenience. Note that this constructor allocates a new array of Enum constants each time it is called, where
	 * if you use {@link #EnumOrderedSet(Enum[], boolean)} (or its equivalent, {@link #noneOf(Enum[])}), you can reuse an
	 * unmodified array to reduce allocations.
	 *
	 * @param universeClass the Class of an Enum type that defines the universe of valid Enum items this can hold
	 */
	public EnumOrderedSet (@Nullable Class<? extends Enum<?>> universeClass) {
		this(universeClass == null ? null : universeClass.getEnumConstants(), true);
	}

	/**
	 * Initializes this set so that it holds the given Enum values, with the universe of possible Enum constants this can hold
	 * determined by the type of the first Enum in {@code contents}.
	 *
	 * @param contents a Collection of Enum items to place into this set
	 */
	public EnumOrderedSet (@NonNull Iterator<? extends Enum<?>> contents) {
		this();
		addAll(contents);
	}

	/**
	 * Initializes this set so that it holds the given Enum values, with the universe of possible Enum constants this can hold
	 * determined by the type of the first Enum in {@code contents}.
	 *
	 * @param contents a Collection of Enum items to place into this set
	 */
	public EnumOrderedSet (@NonNull Collection<? extends Enum<?>> contents) {
		this();
		addAll(contents);
	}

	/**
	 * Copy constructor; uses a direct reference to the enum values that may be cached in {@code other}, but copies other fields.
	 * @param other another EnumSet that will have most of its data copied, but its cached {@code values()} results will be used directly
	 */
	public EnumOrderedSet (@NonNull EnumSet other) {
		if(other.table != null)
			this.table = Arrays.copyOf(other.table, other.table.length);
		this.universe = other.universe;
		ordering = new ObjectList<>(other.size());
		this.addAll(other);
	}

	public EnumOrderedSet(@NonNull EnumOrderedSet other) {
		this.size = other.size;
		if(other.table != null)
			this.table = Arrays.copyOf(other.table, other.table.length);
		this.universe = other.universe;
		ordering = new ObjectList<>(other.ordering);
	}

	/**
	 * Creates a new set using {@code length} items from the given {@code array}, starting at {@code} offset (inclusive).
	 *
	 * @param array  an array to draw items from
	 * @param offset the first index in array to draw an item from
	 * @param length how many items to take from array; bounds-checking is the responsibility of the using code
	 */
	public EnumOrderedSet(Enum<?>[] array, int offset, int length) {
		this();
		addAll(array, offset, length);
	}


	/**
	 * Initializes this set so that it holds the given Enum values, with the universe of possible Enum constants this can hold
	 * determined by the type of the first Enum in {@code contents}.
	 * <br>
	 * This is different from {@link #EnumOrderedSet(Enum[], boolean)} in that this takes constants and puts them in the set, while the other
	 * constructor takes all possible Enum constants, usually from calling {@code values()}. You can also specify the contents of a
	 * new EnumSet conveniently using {@link #with(Enum[])}, which allows passing items as varargs.
	 *
	 * @param contents an array of Enum items to place into this set
	 */
	public EnumOrderedSet (Enum<?> @NonNull [] contents) {
		this();
		addAll(contents);
	}

	/**
	 * Creates a new set by copying {@code count} items from the given Ordered, starting at {@code offset} in that Ordered,
	 * into this.
	 *
	 * @param other  another Ordered of the same type
	 * @param offset the first index in other's ordering to draw an item from
	 * @param count  how many items to copy from other
	 */
	public EnumOrderedSet(Ordered<Enum<?>> other, int offset, int count) {
		this();
		addAll(0, other, offset, count);
	}

	@Override
	public boolean add (Enum<?> key) {
		return super.add(key) && ordering.add(key);
	}

	/**
	 * Sets the key at the specified index. Returns true if the key was not already in the set. If this set already contains the
	 * key, the existing key's index is changed if needed and false is returned. Note, the order of the parameters matches the
	 * order in {@link ObjectList} and the rest of the JDK, not OrderedSet in libGDX.
	 *
	 * @param index where in the iteration order to add the given key, or to move it if already present
	 * @param key   what Enum item to try to add, if not already present
	 * @return true if the key was added for the first time, or false if the key was already present (even if moved)
	 */
	public boolean add (int index, Enum<?> key) {
		if (!super.add(key)) {
			int oldIndex = ordering.indexOf(key);
			if (oldIndex != index) {
				ordering.add(index, ordering.remove(oldIndex));}
			return false;
		}
		ordering.add(index, key);
		return true;
	}

	/**
	 * Adds up to {@code count} items, starting from {@code offset}, in the Ordered {@code other} to this set,
	 * inserting at the end of the iteration order.
	 *
	 * @param other  a non-null {@link Ordered} of {@code Enum}
	 * @param offset the first index in {@code other} to use
	 * @param count  how many indices in {@code other} to use
	 * @return true if this is modified by this call, as {@link #addAll(Collection)} does
	 */
	public boolean addAll (Ordered<Enum<?>> other, int offset, int count) {
		return addAll(size, other, offset, count);
	}

	/**
	 * Adds up to {@code count} items, starting from {@code offset}, in the Ordered {@code other} to this set,
	 * inserting starting at {@code insertionIndex} in the iteration order.
	 *
	 * @param insertionIndex where to insert into the iteration order
	 * @param other          a non-null {@link Ordered} of {@code Enum}
	 * @param offset         the first index in {@code other} to use
	 * @param count          how many indices in {@code other} to use
	 * @return true if this is modified by this call, as {@link #addAll(Collection)} does
	 */
	public boolean addAll (int insertionIndex, Ordered<Enum<?>> other, int offset, int count) {
		boolean changed = false;
		int end = Math.min(offset + count, other.size());
		for (int i = offset; i < end; i++) {
			add(insertionIndex++, other.order().get(i));
			changed = true;
		}
		return changed;
	}

	/**
	 * Adds up to {@code count} items, starting from {@code offset}, in the Ordered {@code other} to this set,
	 * inserting at the end of the iteration order.
	 *
	 * @param other  a non-null {@link Ordered} of {@code Enum}
	 * @param offset the first index in {@code other} to use
	 * @param count  how many indices in {@code other} to use
	 * @return true if this is modified by this call, as {@link #addAll(Collection)} does
	 */
	public boolean addAll (Enum<?> @NonNull[] other, int offset, int count) {
		return addAll(size, other, offset, count);
	}

	/**
	 * Adds up to {@code count} items, starting from {@code offset}, in the Ordered {@code other} to this set,
	 * inserting starting at {@code insertionIndex} in the iteration order.
	 *
	 * @param insertionIndex where to insert into the iteration order
	 * @param other          a non-null {@link Ordered} of {@code Enum}
	 * @param offset         the first index in {@code other} to use
	 * @param count          how many indices in {@code other} to use
	 * @return true if this is modified by this call, as {@link #addAll(Collection)} does
	 */
	public boolean addAll (int insertionIndex, Enum<?> @NonNull[] other, int offset, int count) {
		boolean changed = false;
		int end = Math.min(offset + count, other.length);
		for (int i = offset; i < end; i++) {
			add(insertionIndex++, other[i]);
			changed = true;
		}
		return changed;
	}

	@Override
	public boolean remove (@NonNull Object key) {
		return super.remove(key) && ordering.remove(key);
	}

	/**
	 * Removes and returns the item at the given index in this set's order.
	 *
	 * @param index the index of the item to remove
	 * @return the removed item
	 */
	public Enum<?> removeAt (int index) {
		Enum<?> key = ordering.removeAt(index);
		if(key != null)
			super.remove(key);
		return key;
	}

	/**
	 * Changes the item {@code before} to {@code after} without changing its position in the order. Returns true if {@code after}
	 * has been added to the ObjectOrderedSet and {@code before} has been removed; returns false if {@code after} is already present or
	 * {@code before} is not present. If you are iterating over an ObjectOrderedSet and have an index, you should prefer
	 * {@link #alterAt(int, Enum)}, which doesn't need to search for an index like this does and so can be faster.
	 *
	 * @param before an item that must be present for this to succeed
	 * @param after  an item that must not be in this set for this to succeed
	 * @return true if {@code before} was removed and {@code after} was added, false otherwise
	 */
	public boolean alter (Enum<?> before, Enum<?> after) {
		if (contains(after)) {return false;}
		if (!super.remove(before)) {return false;}
		super.add(after);
		ordering.set(ordering.indexOf(before), after);
		return true;
	}

	/**
	 * Changes the item at the given {@code index} in the order to {@code after}, without changing the ordering of other items. If
	 * {@code after} is already present, this returns false; it will also return false if {@code index} is invalid for the size of
	 * this set. Otherwise, it returns true. Unlike {@link #alter(Enum, Enum)}, this operates in constant time.
	 *
	 * @param index the index in the order of the item to change; must be non-negative and less than {@link #size}
	 * @param after the item that will replace the contents at {@code index}; this item must not be present for this to succeed
	 * @return true if {@code after} successfully replaced the contents at {@code index}, false otherwise
	 */
	public boolean alterAt (int index, Enum<?> after) {
		if (index < 0 || index >= size || contains(after)) {return false;}
		super.remove(ordering.get(index));
		super.add(after);
		ordering.set(index, after);
		return true;
	}

	/**
	 * Gets the Enum item at the given {@code index} in the insertion order. The index should be between 0 (inclusive) and
	 * {@link #size()} (exclusive).
	 *
	 * @param index an index in the insertion order, between 0 (inclusive) and {@link #size()} (exclusive)
	 * @return the item at the given index
	 */
	public Enum<?> getAt (int index) {
		return ordering.get(index);
	}

	@Override
	public Enum<?> first () {
		if (size == 0 || ordering.isEmpty())
			throw new IllegalStateException("ObjectOrderedSet is empty.");
		return ordering.first();
	}


	@Override
	public void clear () {
		ordering.clear();
		super.clear();
	}

	@Override
	public void clearToUniverse(Enum<?> @Nullable [] universe) {
		super.clearToUniverse(universe);
		ordering.clear();
	}

	@Override
	public void clearToUniverse(@Nullable Class<? extends Enum<?>> universe) {
		super.clearToUniverse(universe);
		ordering.clear();
	}

	/**
	 * Gets the ObjectList of keys in the order this class will iterate through them.
	 * Returns a direct reference to the same ObjectList this uses, so changes to the returned list will
	 * also change the iteration order here.
	 *
	 * @return the ObjectList of keys, in iteration order (usually insertion-order), that this uses
	 */
	@Override
	public ObjectList<Enum<?>> order () {
		return ordering;
	}

	/**
	 * Sorts this EnumFloatOrderedMap in-place by the keys' natural ordering.
	 */
	public void sort () {
		ordering.sort(null);
	}

	/**
	 * Sorts this EnumFloatOrderedMap in-place by the given Comparator used on the keys. If {@code comp} is null, then this
	 * will sort by the natural ordering of the keys.
	 *
	 * @param comp a Comparator that can compare two {@code Enum} keys, or null to use the keys' natural ordering
	 */
	public void sort (@Nullable Comparator<? super Enum<?>> comp) {
		ordering.sort(comp);
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
	public void removeRange (int start, int end) {
		start = Math.max(0, start);
		end = Math.min(ordering.size(), end);
		for (int i = start; i < end; i++) {
			super.remove(ordering.get(i));
		}
		ordering.removeRange(start, end);
	}

	/**
	 * Reduces the size of the set to the specified size. If the set is already smaller than the specified
	 * size, no action is taken.
	 *
	 * @param newSize the target size to try to reach by removing items, if smaller than the current size
	 */
	@Override
	public void truncate (int newSize) {
		if (size > newSize) {removeRange(newSize, size);}
	}

	/**
	 * Iterates through items in the same order as {@link #order()}.
	 * Reuses one of two iterators, and does not permit nested iteration;
	 * use {@link EnumOrderedSetIterator#EnumOrderedSetIterator(EnumOrderedSet)} to nest iterators.
	 *
	 * @return an {@link Iterator} over the Enum items in this, in order
	 */
	@Override
	public @NonNull EnumSetIterator iterator () {
		if (iterator1 == null || iterator2 == null) {
			iterator1 = new EnumOrderedSetIterator(this);
			iterator2 = new EnumOrderedSetIterator(this);
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
		if (size == 0) {return "{}";}
		ObjectList<Enum<?>> items = this.ordering;
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

	public static class EnumOrderedSetIterator extends EnumSetIterator {
		private final ObjectList<Enum<?>> items;

		public EnumOrderedSetIterator(EnumOrderedSet set) {
			super(set);
			items = set.ordering;
		}

		@Override
		public void reset () {
			nextIndex = 0;
			hasNext = set.size > 0;
		}

		@Override
		public Enum<?> next () {
			if (!hasNext) {throw new NoSuchElementException();}
			if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
			Enum<?> key = items.get(nextIndex);
			nextIndex++;
			hasNext = nextIndex < set.size;
			return key;
		}

		@Override
		public void remove () {
			if (nextIndex < 0) {throw new IllegalStateException("next must be called before remove.");}
			nextIndex--;
			set.remove(items.get(nextIndex));
		}
	}

	/**
	 * Constructs an empty set given the type as a generic type argument.
	 * This is usually less useful than just using the constructor, but can be handy
	 * in some code-generation scenarios when you don't know how many arguments you will have.
	 *
	 * @param <T>    the type of items; must be given explicitly
	 * @return a new set containing nothing
	 */
	public static <Enum<?>> EnumOrderedSet<Enum<?>> with () {
		return new EnumOrderedSet<>(0);
	}

	/**
	 * Creates a new ObjectOrderedSet that holds only the given item, but can be resized.
	 * @param item one T item
	 * @return a new ObjectOrderedSet that holds the given item
	 * @param <T> the type of item, typically inferred
	 */
	public static <Enum<?>> EnumOrderedSet<Enum<?>> with (Enum<?> item) {
		EnumOrderedSet<Enum<?>> set = new EnumOrderedSet<>(1);
		set.add(item);
		return set;
	}

	/**
	 * Creates a new ObjectOrderedSet that holds only the given items, but can be resized.
	 * @param item0 a T item
	 * @param item1 a T item
	 * @return a new ObjectOrderedSet that holds the given items
	 * @param <T> the type of item, typically inferred
	 */
	public static <Enum<?>> EnumOrderedSet<Enum<?>> with (Enum<?> item0, Enum<?> item1) {
		EnumOrderedSet<Enum<?>> set = new EnumOrderedSet<>(2);
		set.add(item0, item1);
		return set;
	}

	/**
	 * Creates a new ObjectOrderedSet that holds only the given items, but can be resized.
	 * @param item0 a T item
	 * @param item1 a T item
	 * @param item2 a T item
	 * @return a new ObjectOrderedSet that holds the given items
	 * @param <T> the type of item, typically inferred
	 */
	public static <Enum<?>> EnumOrderedSet<Enum<?>> with (Enum<?> item0, Enum<?> item1, Enum<?> item2) {
		EnumOrderedSet<Enum<?>> set = new EnumOrderedSet<>(3);
		set.add(item0, item1, item2);
		return set;
	}

	/**
	 * Creates a new ObjectOrderedSet that holds only the given items, but can be resized.
	 * @param item0 a T item
	 * @param item1 a T item
	 * @param item2 a T item
	 * @param item3 a T item
	 * @return a new ObjectOrderedSet that holds the given items
	 * @param <T> the type of item, typically inferred
	 */
	public static <Enum<?>> EnumOrderedSet<Enum<?>> with (Enum<?> item0, Enum<?> item1, Enum<?> item2, Enum<?> item3) {
		EnumOrderedSet<Enum<?>> set = new EnumOrderedSet<>(4);
		set.add(item0, item1, item2, item3);
		return set;
	}

	/**
	 * Creates a new ObjectOrderedSet that holds only the given items, but can be resized.
	 * @param item0 a T item
	 * @param item1 a T item
	 * @param item2 a T item
	 * @param item3 a T item
	 * @param item4 a T item
	 * @return a new ObjectOrderedSet that holds the given items
	 * @param <T> the type of item, typically inferred
	 */
	public static <Enum<?>> EnumOrderedSet<Enum<?>> with (Enum<?> item0, Enum<?> item1, Enum<?> item2, Enum<?> item3, Enum<?> item4) {
		EnumOrderedSet<Enum<?>> set = new EnumOrderedSet<>(5);
		set.add(item0, item1, item2, item3);
		set.add(item4);
		return set;
	}

	/**
	 * Creates a new ObjectOrderedSet that holds only the given items, but can be resized.
	 * @param item0 a T item
	 * @param item1 a T item
	 * @param item2 a T item
	 * @param item3 a T item
	 * @param item4 a T item
	 * @param item5 a T item
	 * @return a new ObjectOrderedSet that holds the given items
	 * @param <T> the type of item, typically inferred
	 */
	public static <Enum<?>> EnumOrderedSet<Enum<?>> with (Enum<?> item0, Enum<?> item1, Enum<?> item2, Enum<?> item3, Enum<?> item4, Enum<?> item5) {
		EnumOrderedSet<Enum<?>> set = new EnumOrderedSet<>(6);
		set.add(item0, item1, item2, item3);
		set.add(item4, item5);
		return set;
	}

	/**
	 * Creates a new ObjectOrderedSet that holds only the given items, but can be resized.
	 * @param item0 a T item
	 * @param item1 a T item
	 * @param item2 a T item
	 * @param item3 a T item
	 * @param item4 a T item
	 * @param item5 a T item
	 * @param item6 a T item
	 * @return a new ObjectOrderedSet that holds the given items
	 * @param <T> the type of item, typically inferred
	 */
	public static <Enum<?>> EnumOrderedSet<Enum<?>> with (Enum<?> item0, Enum<?> item1, Enum<?> item2, Enum<?> item3, Enum<?> item4, Enum<?> item5, Enum<?> item6) {
		EnumOrderedSet<Enum<?>> set = new EnumOrderedSet<>(7);
		set.add(item0, item1, item2, item3);
		set.add(item4, item5, item6);
		return set;
	}

	/**
	 * Creates a new ObjectOrderedSet that holds only the given items, but can be resized.
	 * @param item0 a T item
	 * @param item1 a T item
	 * @param item2 a T item
	 * @param item3 a T item
	 * @param item4 a T item
	 * @param item5 a T item
	 * @param item6 a T item
	 * @return a new ObjectOrderedSet that holds the given items
	 * @param <T> the type of item, typically inferred
	 */
	public static <Enum<?>> EnumOrderedSet<Enum<?>> with (Enum<?> item0, Enum<?> item1, Enum<?> item2, Enum<?> item3, Enum<?> item4, Enum<?> item5, Enum<?> item6, Enum<?> item7) {
		EnumOrderedSet<Enum<?>> set = new EnumOrderedSet<>(8);
		set.add(item0, item1, item2, item3);
		set.add(item4, item5, item6, item7);
		return set;
	}

	/**
	 * Creates a new ObjectOrderedSet that holds only the given items, but can be resized.
	 * This overload will only be used when an array is supplied and the type of the
	 * items requested is the component type of the array, or if varargs are used and
	 * there are 9 or more arguments.
	 * @param varargs a T varargs or T array; remember that varargs allocate
	 * @return a new ObjectOrderedSet that holds the given items
	 * @param <T> the type of item, typically inferred
	 */
	@SafeVarargs
	public static <Enum<?>> EnumOrderedSet<Enum<?>> with (Enum<?>... varargs) {
		return new EnumOrderedSet<>(varargs);
	}
}

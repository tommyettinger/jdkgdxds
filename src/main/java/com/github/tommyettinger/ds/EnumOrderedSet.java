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
import com.github.tommyettinger.function.ObjToObjFunction;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;

/**
 * An {@link EnumSet} that also stores keys in an {@link ObjectList} using the insertion order.
 * Unlike {@link java.util.EnumSet}, this does not require a Class at construction time, which can be
 * useful for serialization purposes. Instead of storing a Class, this holds a "key universe" (which is almost always the
 * same as an array returned by calling {@code values()} on an Enum type), and key universes are ideally shared between
 * compatible EnumSets. No allocation is done unless this is changing its table size and/or key universe. You can change
 * the ordering of the Enum items using methods like {@link #sort(Comparator)} and {@link #shuffle(Random)}. You can also
 * access enums via their index in the ordering, using methods such as {@link #getAt(int)}, {@link #alterAt(int, Enum)},
 * and {@link #removeAt(int)}.
 * <br>
 * The key universe is an important concept here; it is simply an array of all possible Enum values the EnumSet can use as keys, in
 * the specific order they are declared. You almost always get a key universe by calling {@code MyEnum.values()}, but you
 * can also use {@link Class#getEnumConstants()} for an Enum class. You can and generally should reuse key universes in order to
 * avoid allocations and/or save memory; the method {@link #noneOf(Enum[])} creates an empty EnumSet with
 * a given key universe. If you need to use the zero-argument constructor, you can, and the key universe will be obtained from the
 * first key placed into the EnumSet, though it won't be shared at first. You can also set the key universe with
 * {@link #clearToUniverse(Enum[])}, in the process of clearing the map.
 * <br>
 * {@link #iterator() Iteration} is ordered and faster than an unordered set. Keys can also be accessed and the order changed
 * using {@link #order()}. There is some additional overhead for put and remove.
 * <br>
 * This class tries to be as compatible as possible with {@link java.util.EnumSet}, though this expands on that where possible.
 *
 * @author Tommy Ettinger
 */
public class EnumOrderedSet extends EnumSet implements Ordered<Enum<?>> {

	protected final ObjectList<@NonNull Enum<?>> ordering;

	/**
	 * Only specifies an OrderType; using this will postpone allocating any internal arrays until {@link #add(Enum)} is
	 * first called (potentially indirectly).
	 *
	 * @param type either {@link OrderType#BAG} to use unreliable ordering with faster deletion, or anything else to
	 *             use a list type that takes longer to delete but maintains insertion order reliably
	 */
	public EnumOrderedSet(OrderType type) {
		super();
		ordering = type == OrderType.BAG ? new ObjectBag<>() : new ObjectList<>();
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
	 * @param universe             almost always, the result of calling {@code values()} on an Enum type; used directly, not copied
	 * @param ignoredToDistinguish an ignored boolean that differentiates this constructor, which defined a key universe,
	 *                             from one that takes contents
	 * @param type                 either {@link OrderType#BAG} to use unreliable ordering with faster deletion, or anything else to
	 *                             use a list type that takes longer to delete but maintains insertion order reliably
	 */
	public EnumOrderedSet(Enum<?> @Nullable [] universe, boolean ignoredToDistinguish, OrderType type) {
		super();
		if (universe == null) {
			ordering = new ObjectList<>();
			return;
		}
		this.universe = universe;
		table = new int[universe.length + 31 >>> 5];
		ordering = type == OrderType.BAG ? new ObjectBag<>(universe.length) : new ObjectList<>(universe.length);
	}

	/**
	 * Initializes this set so that it has exactly enough capacity as needed to contain each Enum constant defined by the
	 * Class {@code universeClass}, assuming universeClass is non-null. This simply calls {@link #EnumOrderedSet(Enum[], boolean)}
	 * for convenience. Note that this constructor allocates a new array of Enum constants each time it is called, where
	 * if you use {@link #EnumOrderedSet(Enum[], boolean)} (or its equivalent, {@link #noneOf(Enum[])}), you can reuse an
	 * unmodified array to reduce allocations.
	 *
	 * @param universeClass the Class of an Enum type that defines the universe of valid Enum items this can hold
	 * @param type          either {@link OrderType#BAG} to use unreliable ordering with faster deletion, or anything else to
	 *                      use a list type that takes longer to delete but maintains insertion order reliably
	 */
	public EnumOrderedSet(@Nullable Class<? extends Enum<?>> universeClass, OrderType type) {
		this(universeClass == null ? null : universeClass.getEnumConstants(), true, type);
	}

	/**
	 * Initializes this set so that it holds the given Enum values, with the universe of possible Enum constants this can hold
	 * determined by the type of the first Enum in {@code contents}.
	 *
	 * @param contents a Collection of Enum items to place into this set
	 * @param type     either {@link OrderType#BAG} to use unreliable ordering with faster deletion, or anything else to
	 *                 use a list type that takes longer to delete but maintains insertion order reliably
	 */
	public EnumOrderedSet(@NonNull Iterator<? extends Enum<?>> contents, OrderType type) {
		this(type);
		addAll(contents);
	}

	/**
	 * Initializes this set so that it holds the given Enum values, with the universe of possible Enum constants this can hold
	 * determined by the type of the first Enum in {@code contents}.
	 *
	 * @param contents a Collection of Enum items to place into this set
	 * @param type     either {@link OrderType#BAG} to use unreliable ordering with faster deletion, or anything else to
	 *                 use a list type that takes longer to delete but maintains insertion order reliably
	 */
	public EnumOrderedSet(@NonNull Collection<? extends Enum<?>> contents, OrderType type) {
		this(type);
		addAll(contents);
	}

	/**
	 * Copy constructor; uses a direct reference to the enum values that may be cached in {@code other}, but copies other fields.
	 *
	 * @param other another EnumSet that will have most of its data copied, but its cached {@code values()} results will be used directly
	 * @param type  either {@link OrderType#BAG} to use unreliable ordering with faster deletion, or anything else to
	 *              use a list type that takes longer to delete but maintains insertion order reliably
	 */
	public EnumOrderedSet(@NonNull EnumSet other, OrderType type) {
		if (other.table != null)
			this.table = Arrays.copyOf(other.table, other.table.length);
		this.universe = other.universe;
		ordering = type == OrderType.BAG ? new ObjectBag<>(other.size()) : new ObjectList<>(other.size());
		this.addAll(other);
	}

	/**
	 * Copies {@code other} but allows specifying an OrderType independently of {@code other}'s ordering.
	 *
	 * @param other another EnumOrderedSet that will have its contents copied
	 * @param type  either {@link OrderType#BAG} to use unreliable ordering with faster deletion, or anything else to
	 *              use a list type that takes longer to delete but maintains insertion order reliably
	 */
	public EnumOrderedSet(@NonNull EnumOrderedSet other, OrderType type) {
		this.size = other.size;
		if (other.table != null)
			this.table = Arrays.copyOf(other.table, other.table.length);
		this.universe = other.universe;
		ordering = type == OrderType.BAG ? new ObjectBag<>(other.ordering) : new ObjectList<>(other.ordering);
	}

	/**
	 * Creates a new set using {@code length} items from the given {@code array}, starting at {@code} offset (inclusive).
	 *
	 * @param array  an array to draw items from
	 * @param offset the first index in array to draw an item from
	 * @param length how many items to take from array; bounds-checking is the responsibility of the using code
	 * @param type   either {@link OrderType#BAG} to use unreliable ordering with faster deletion, or anything else to
	 *               use a list type that takes longer to delete but maintains insertion order reliably
	 */
	public EnumOrderedSet(Enum<?>[] array, int offset, int length, OrderType type) {
		this(type);
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
	 * @param type     either {@link OrderType#BAG} to use unreliable ordering with faster deletion, or anything else to
	 *                 use a list type that takes longer to delete but maintains insertion order reliably
	 */
	public EnumOrderedSet(Enum<?> @NonNull [] contents, OrderType type) {
		this(type);
		addAll(contents);
	}

	/**
	 * Creates a new set by copying {@code count} items from the given Ordered, starting at {@code offset} in that Ordered,
	 * into this.
	 *
	 * @param other  another Ordered of the same type
	 * @param offset the first index in other's ordering to draw an item from
	 * @param count  how many items to copy from other
	 * @param type   either {@link OrderType#BAG} to use unreliable ordering with faster deletion, or anything else to
	 *               use a list type that takes longer to delete but maintains insertion order reliably
	 */
	public EnumOrderedSet(Ordered<Enum<?>> other, int offset, int count, OrderType type) {
		this(type);
		addAll(0, other, offset, count);
	}

	// Default order type.

	/**
	 * Empty constructor; using this will postpone allocating any internal arrays until {@link #add(Enum)} is first called
	 * (potentially indirectly).
	 */
	public EnumOrderedSet() {
		this(OrderType.LIST);
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
	 * @param universe             almost always, the result of calling {@code values()} on an Enum type; used directly, not copied
	 * @param ignoredToDistinguish an ignored boolean that differentiates this constructor, which defined a key universe,
	 *                             from one that takes contents
	 */
	public EnumOrderedSet(Enum<?> @Nullable [] universe, boolean ignoredToDistinguish) {
		this(universe, ignoredToDistinguish, OrderType.LIST);
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
	public EnumOrderedSet(@Nullable Class<? extends Enum<?>> universeClass) {
		this(universeClass, OrderType.LIST);
	}

	/**
	 * Initializes this set so that it holds the given Enum values, with the universe of possible Enum constants this can hold
	 * determined by the type of the first Enum in {@code contents}.
	 *
	 * @param contents a Collection of Enum items to place into this set
	 */
	public EnumOrderedSet(@NonNull Iterator<? extends Enum<?>> contents) {
		this(contents, OrderType.LIST);
	}

	/**
	 * Initializes this set so that it holds the given Enum values, with the universe of possible Enum constants this can hold
	 * determined by the type of the first Enum in {@code contents}.
	 *
	 * @param contents a Collection of Enum items to place into this set
	 */
	public EnumOrderedSet(@NonNull Collection<? extends Enum<?>> contents) {
		this(contents, OrderType.LIST);
	}

	/**
	 * Copy constructor; uses a direct reference to the enum values that may be cached in {@code other}, but copies other fields.
	 *
	 * @param other another EnumSet that will have most of its data copied, but its cached {@code values()} results will be used directly
	 */
	public EnumOrderedSet(@NonNull EnumSet other) {
		this(other, OrderType.LIST);
	}

	/**
	 * Copies the entirety of {@code other}, including using the same OrderType.
	 *
	 * @param other another EnumOrderedSet that will have its contents and ordering copied
	 */
	public EnumOrderedSet(@NonNull EnumOrderedSet other) {
		this.size = other.size;
		if (other.table != null)
			this.table = Arrays.copyOf(other.table, other.table.length);
		this.universe = other.universe;
		ordering = other.ordering instanceof ObjectBag ? new ObjectBag<>(other.ordering) : new ObjectList<>(other.ordering);
	}

	/**
	 * Creates a new set using {@code length} items from the given {@code array}, starting at {@code} offset (inclusive).
	 *
	 * @param array  an array to draw items from
	 * @param offset the first index in array to draw an item from
	 * @param length how many items to take from array; bounds-checking is the responsibility of the using code
	 */
	public EnumOrderedSet(Enum<?>[] array, int offset, int length) {
		this(array, offset, length, OrderType.LIST);
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
	public EnumOrderedSet(Enum<?> @NonNull [] contents) {
		this(contents, OrderType.LIST);
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
		this(other, offset, count, OrderType.LIST);
	}

	@Override
	public boolean add(Enum<?> key) {
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
	public boolean add(int index, Enum<?> key) {
		if (key == null) return false;
		if (!super.add(key)) {
			int oldIndex = ordering.indexOf(key);
			if (oldIndex != index) {
				ordering.add(index, ordering.remove(oldIndex));
			}
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
	public boolean addAll(Ordered<Enum<?>> other, int offset, int count) {
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
	public boolean addAll(int insertionIndex, Ordered<Enum<?>> other, int offset, int count) {
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
	public boolean addAll(Enum<?> @NonNull [] other, int offset, int count) {
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
	public boolean addAll(int insertionIndex, Enum<?> @NonNull [] other, int offset, int count) {
		boolean changed = false;
		int end = Math.min(offset + count, other.length);
		for (int i = offset; i < end; i++) {
			add(insertionIndex++, other[i]);
			changed = true;
		}
		return changed;
	}

	@Override
	public boolean remove(@NonNull Object key) {
		return super.remove(key) && ordering.remove(key);
	}

	/**
	 * Removes and returns the item at the given index in this set's order.
	 *
	 * @param index the index of the item to remove
	 * @return the removed item
	 */
	public Enum<?> removeAt(int index) {
		Enum<?> key = ordering.removeAt(index);
		if (key != null)
			super.remove(key);
		return key;
	}

	/**
	 * Changes the item {@code before} to {@code after} without changing its position in the order. Returns true if {@code after}
	 * has been added to the EnumOrderedSet and {@code before} has been removed; returns false if {@code after} is already present or
	 * {@code before} is not present. If you are iterating over an EnumOrderedSet and have an index, you should prefer
	 * {@link #alterAt(int, Enum)}, which doesn't need to search for an index like this does and so can be faster.
	 *
	 * @param before an item that must be present for this to succeed
	 * @param after  an item that must not be in this set for this to succeed
	 * @return true if {@code before} was removed and {@code after} was added, false otherwise
	 */
	public boolean alter(Enum<?> before, Enum<?> after) {
		if (before == null || after == null || contains(after)) {
			return false;
		}
		if (!super.remove(before)) {
			return false;
		}
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
	public boolean alterAt(int index, Enum<?> after) {
		if (after == null || index < 0 || index >= size || contains(after)) {
			return false;
		}
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
	public Enum<?> getAt(int index) {
		return ordering.get(index);
	}

	@Override
	public Enum<?> first() {
		if (size == 0 || ordering.isEmpty())
			throw new IllegalStateException("EnumOrderedSet is empty.");
		return ordering.first();
	}


	@Override
	public void clear() {
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
	public ObjectList<Enum<?>> order() {
		return ordering;
	}

	/**
	 * Sorts this EnumOrderedMap in-place by the keys' natural ordering.
	 */
	public void sort() {
		ordering.sort(null);
	}

	/**
	 * Sorts this EnumOrderedMap in-place by the given Comparator used on the keys. If {@code comp} is null, then this
	 * will sort by the natural ordering of the keys.
	 *
	 * @param comp a Comparator that can compare two {@code Enum} keys, or null to use the keys' natural ordering
	 */
	public void sort(@Nullable Comparator<? super Enum<?>> comp) {
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
	public void removeRange(int start, int end) {
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
	public void truncate(int newSize) {
		if (size > newSize) {
			removeRange(newSize, size);
		}
	}

	/**
	 * Iterates through items in the same order as {@link #order()}.
	 * Reuses one of two iterators, and does not permit nested iteration;
	 * use {@link EnumOrderedSetIterator#EnumOrderedSetIterator(EnumOrderedSet)} to nest iterators.
	 *
	 * @return an {@link Iterator} over the Enum items in this, in order
	 */
	@Override
	public @NonNull EnumSetIterator iterator() {
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
	public String toString(String itemSeparator) {
		if (size == 0) {
			return "{}";
		}
		ObjectList<Enum<?>> items = this.ordering;
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
	public String toString() {
		return toString(", ");
	}

	public static class EnumOrderedSetIterator extends EnumSetIterator {
		private final ObjectList<Enum<?>> items;

		public EnumOrderedSetIterator(EnumOrderedSet set) {
			super(set);
			items = set.ordering;
		}

		@Override
		public void reset() {
			nextIndex = 0;
			hasNext = set.size > 0;
		}

		@Override
		public Enum<?> next() {
			if (!hasNext) {
				throw new NoSuchElementException();
			}
			if (!valid) {
				throw new RuntimeException("#iterator() cannot be used nested.");
			}
			Enum<?> key = items.get(nextIndex);
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
	 * @return a new set containing nothing
	 */
	public static EnumOrderedSet with() {
		return new EnumOrderedSet();
	}

	/**
	 * Creates a new EnumOrderedSet that holds only the given item, but can be resized.
	 *
	 * @param item one Enum item
	 * @return a new EnumOrderedSet that holds the given item
	 */
	public static EnumOrderedSet with(Enum<?> item) {
		EnumOrderedSet set = new EnumOrderedSet();
		set.add(item);
		return set;
	}

	/**
	 * Creates a new EnumOrderedSet that holds only the given items, but can be resized.
	 *
	 * @param item0 an Enum item
	 * @param item1 an Enum item
	 * @return a new EnumOrderedSet that holds the given items
	 */
	public static EnumOrderedSet with(Enum<?> item0, Enum<?> item1) {
		EnumOrderedSet set = new EnumOrderedSet();
		set.add(item0, item1);
		return set;
	}

	/**
	 * Creates a new EnumOrderedSet that holds only the given items, but can be resized.
	 *
	 * @param item0 an Enum item
	 * @param item1 an Enum item
	 * @param item2 an Enum item
	 * @return a new EnumOrderedSet that holds the given items
	 */
	public static EnumOrderedSet with(Enum<?> item0, Enum<?> item1, Enum<?> item2) {
		EnumOrderedSet set = new EnumOrderedSet();
		set.add(item0, item1, item2);
		return set;
	}

	/**
	 * Creates a new EnumOrderedSet that holds only the given items, but can be resized.
	 *
	 * @param item0 an Enum item
	 * @param item1 an Enum item
	 * @param item2 an Enum item
	 * @param item3 an Enum item
	 * @return a new EnumOrderedSet that holds the given items
	 */
	public static EnumOrderedSet with(Enum<?> item0, Enum<?> item1, Enum<?> item2, Enum<?> item3) {
		EnumOrderedSet set = new EnumOrderedSet();
		set.add(item0, item1, item2, item3);
		return set;
	}

	/**
	 * Creates a new EnumOrderedSet that holds only the given items, but can be resized.
	 *
	 * @param item0 an Enum item
	 * @param item1 an Enum item
	 * @param item2 an Enum item
	 * @param item3 an Enum item
	 * @param item4 an Enum item
	 * @return a new EnumOrderedSet that holds the given items
	 */
	public static EnumOrderedSet with(Enum<?> item0, Enum<?> item1, Enum<?> item2, Enum<?> item3, Enum<?> item4) {
		EnumOrderedSet set = new EnumOrderedSet();
		set.add(item0, item1, item2, item3);
		set.add(item4);
		return set;
	}

	/**
	 * Creates a new EnumOrderedSet that holds only the given items, but can be resized.
	 *
	 * @param item0 an Enum item
	 * @param item1 an Enum item
	 * @param item2 an Enum item
	 * @param item3 an Enum item
	 * @param item4 an Enum item
	 * @param item5 an Enum item
	 * @return a new EnumOrderedSet that holds the given items
	 */
	public static EnumOrderedSet with(Enum<?> item0, Enum<?> item1, Enum<?> item2, Enum<?> item3, Enum<?> item4, Enum<?> item5) {
		EnumOrderedSet set = new EnumOrderedSet();
		set.add(item0, item1, item2, item3);
		set.add(item4, item5);
		return set;
	}

	/**
	 * Creates a new EnumOrderedSet that holds only the given items, but can be resized.
	 *
	 * @param item0 an Enum item
	 * @param item1 an Enum item
	 * @param item2 an Enum item
	 * @param item3 an Enum item
	 * @param item4 an Enum item
	 * @param item5 an Enum item
	 * @param item6 an Enum item
	 * @return a new EnumOrderedSet that holds the given items
	 */
	public static EnumOrderedSet with(Enum<?> item0, Enum<?> item1, Enum<?> item2, Enum<?> item3, Enum<?> item4, Enum<?> item5, Enum<?> item6) {
		EnumOrderedSet set = new EnumOrderedSet();
		set.add(item0, item1, item2, item3);
		set.add(item4, item5, item6);
		return set;
	}

	/**
	 * Creates a new EnumOrderedSet that holds only the given items, but can be resized.
	 *
	 * @param item0 an Enum item
	 * @param item1 an Enum item
	 * @param item2 an Enum item
	 * @param item3 an Enum item
	 * @param item4 an Enum item
	 * @param item5 an Enum item
	 * @param item6 an Enum item
	 * @return a new EnumOrderedSet that holds the given items
	 */
	public static EnumOrderedSet with(Enum<?> item0, Enum<?> item1, Enum<?> item2, Enum<?> item3, Enum<?> item4, Enum<?> item5, Enum<?> item6, Enum<?> item7) {
		EnumOrderedSet set = new EnumOrderedSet();
		set.add(item0, item1, item2, item3);
		set.add(item4, item5, item6, item7);
		return set;
	}

	/**
	 * Creates a new EnumOrderedSet that holds only the given items, but can be resized.
	 * This overload will only be used when an array is supplied and the type of the
	 * items requested is the component type of the array, or if varargs are used and
	 * there are 9 or more arguments.
	 *
	 * @param varargs an Enum varargs or Enum array; remember that varargs allocate
	 * @return a new EnumOrderedSet that holds the given items
	 */
	public static EnumOrderedSet with(Enum<?>... varargs) {
		return new EnumOrderedSet(varargs);
	}

	/**
	 * Calls {@link #parse(String, String, PartialParser, boolean)} with brackets set to false.
	 * <br>
	 * The {@code parser} is often produced by {@link PartialParser#enumParser(ObjToObjFunction)}.
	 *
	 *  @param str a String that will be parsed in full
	 * @param delimiter the delimiter between items in str
	 * @param parser a PartialParser that returns an {@link Enum} item from a section of {@code str}
	 * @return a new collection parsed from str
	 */
	public static EnumOrderedSet parse(String str, String delimiter, PartialParser<Enum<?>> parser) {
		return parse(str, delimiter, parser, false);
	}

	/**
	 * Creates a new collection and fills it by calling {@link #addLegible(String, String, PartialParser, int, int)} on
	 * either all of {@code str} (if {@code brackets} is false) or {@code str} without its first and last chars (if
	 * {@code brackets} is true). Each item is expected to be separated by {@code delimiter}.
	 * <br>
	 * The {@code parser} is often produced by {@link PartialParser#enumParser(ObjToObjFunction)}.
	 *
	 * @param str a String that will be parsed in full (depending on brackets)
	 * @param delimiter the delimiter between items in str
	 * @param parser a PartialParser that returns an {@link Enum} item from a section of {@code str}
	 * @param brackets if true, the first and last chars in str will be ignored
	 * @return a new collection parsed from str
	 */
	public static EnumOrderedSet parse(String str, String delimiter, PartialParser<Enum<?>> parser, boolean brackets) {
		EnumOrderedSet c = new EnumOrderedSet();
		if(brackets)
			c.addLegible(str, delimiter, parser, 1, str.length() - 1);
		else
			c.addLegible(str, delimiter, parser);
		return c;
	}

	/**
	 * Creates a new collection and fills it by calling {@link #addLegible(String, String, PartialParser, int, int)}
	 * with the given five parameters as-is.
	 * <br>
	 * The {@code parser} is often produced by {@link PartialParser#enumParser(ObjToObjFunction)}.
	 *
	 * @param str a String that will have the given section parsed
	 * @param delimiter the delimiter between items in str
	 * @param parser a PartialParser that returns an {@link Enum} item from a section of {@code str}
	 * @param offset the first position to parse in str, inclusive
	 * @param length how many chars to parse, starting from offset
	 * @return a new collection parsed from str
	 */
	public static EnumOrderedSet parse(String str, String delimiter, PartialParser<Enum<?>> parser, int offset, int length) {
		EnumOrderedSet c = new EnumOrderedSet();
		c.addLegible(str, delimiter, parser, offset, length);
		return c;
	}

	/**
	 * Alias of {@link #with(Enum)} for compatibility.
	 *
	 * @param item the one item to initialize the EnumSet with
	 * @return a new EnumOrderedSet containing {@code item}
	 */
	public static EnumOrderedSet of(Enum<?> item) {
		return with(item);
	}

	/**
	 * Alias of {@link #with(Enum[])} for compatibility.
	 *
	 * @param array an array or varargs of Enum constants, which should all have the same Enum type
	 * @return a new EnumOrderedSet containing each unique item from {@code array}
	 */
	public static EnumOrderedSet of(Enum<?>... array) {
		return with(array);
	}

	/**
	 * Creates a new EnumOrderedSet using the given result of calling {@code values()} on an Enum type (the universe), but with no items
	 * initially stored in the set. You can reuse the universe between EnumOrderedSet instances as long as it is not modified.
	 * <br>
	 * This is the same as calling {@link #EnumOrderedSet(Enum[], boolean)}.
	 *
	 * @param universe almost always, the result of calling {@code values()} on an Enum type; used directly, not copied
	 * @return a new EnumOrderedSet with the specified universe of possible items, but none present in the set
	 */
	public static EnumOrderedSet noneOf(Enum<?> @Nullable [] universe) {
		return new EnumOrderedSet(universe, true);
	}

	/**
	 * Creates a new EnumOrderedSet using the given result of calling {@code values()} on an Enum type (the universe), and with all possible
	 * items initially stored in the set. You can reuse the universe between EnumOrderedSet instances as long as it is not modified.
	 *
	 * @param universe almost always, the result of calling {@code values()} on an Enum type; used directly, not copied
	 * @return a new EnumOrderedSet with the specified universe of possible items, and all of them present in the set
	 */
	public static EnumOrderedSet allOf(Enum<?> @Nullable [] universe) {
		if (universe == null) return new EnumOrderedSet();
		return new EnumOrderedSet(universe);
	}

	/**
	 * Creates a new EnumOrderedSet using the constants from the given Class (of an Enum type), but with no items initially
	 * stored in the set.
	 * <br>
	 * This is the same as calling {@link #EnumOrderedSet(Class)}.
	 *
	 * @param clazz the Class of any Enum type; you can get this from a constant with {@link Enum#getDeclaringClass()}
	 * @return a new EnumOrderedSet with the specified universe of possible items, but none present in the set
	 */
	public static EnumOrderedSet noneOf(@Nullable Class<? extends Enum<?>> clazz) {
		if (clazz == null)
			return new EnumOrderedSet();
		return new EnumOrderedSet(clazz.getEnumConstants(), true);
	}

	/**
	 * Creates a new EnumOrderedSet using the constants from the given Class (of an Enum type), and with all possible items initially
	 * stored in the set.
	 *
	 * @param clazz the Class of any Enum type; you can get this from a constant with {@link Enum#getDeclaringClass()}
	 * @return a new EnumOrderedSet with the specified universe of possible items, and all of them present in the set
	 */
	public static EnumOrderedSet allOf(@Nullable Class<? extends Enum<?>> clazz) {
		if (clazz == null)
			return new EnumOrderedSet();
		return new EnumOrderedSet(clazz.getEnumConstants());
	}

	/**
	 * Given another EnumOrderedSet, this creates a new EnumOrderedSet with the same universe as {@code other}, but with any elements present in other
	 * absent in the new set, and any elements absent in other present in the new set.
	 *
	 * @param other another EnumOrderedSet that this will copy
	 * @return a complemented copy of {@code other}
	 */
	public static EnumOrderedSet complementOf(EnumOrderedSet other) {
		if (other == null || other.universe == null) return new EnumOrderedSet();
		EnumOrderedSet coll = new EnumOrderedSet(other);
		coll.ordering.clear();
		for (int i = 0; i < coll.table.length - 1; i++) {
			coll.table[i] ^= -1;
		}
		coll.table[coll.table.length - 1] ^= -1 >>> -coll.universe.length;
		coll.size = coll.universe.length - other.size;
		for (int i = 0; i < coll.universe.length; i++) {
			if ((coll.table[i >>> 5] & (1 << i)) != 0)
				coll.add(coll.universe[i]);
		}
		return coll;
	}

	/**
	 * Creates an EnumOrderedSet holding any Enum items in the given {@code contents}, which may be any Collection of Enum, including another
	 * EnumOrderedSet. If given an EnumOrderedSet, this will copy its Enum universe and other information even if it is empty.
	 *
	 * @param contents a Collection of Enum values, which may be another EnumOrderedSet
	 * @return a new EnumOrderedSet containing the unique items in contents
	 */
	public static EnumOrderedSet copyOf(Collection<? extends Enum<?>> contents) {
		if (contents == null) throw new NullPointerException("Cannot copy a null Collection.");
		return new EnumOrderedSet(contents);
	}

	/**
	 * Creates an EnumOrderedSet holding Enum items between the ordinals of {@code start} and {@code end}. If the ordinal of end is less than
	 * the ordinal of start, this throws an {@link IllegalArgumentException}.
	 * If start and end are the same, this just inserts that one Enum.
	 *
	 * @param start the starting inclusive Enum to insert
	 * @param end   the ending inclusive Enum to insert
	 * @param <E>   the shared Enum type of both start and end
	 * @return a new EnumOrderedSet containing start, end, and any Enum constants with ordinals between them
	 * @throws IllegalArgumentException if the {@link Enum#ordinal() ordinal} of end is less than the ordinal of start
	 */
	public static <E extends Enum<E>> EnumOrderedSet range(Enum<E> start, Enum<E> end) {
		final int mn = start.ordinal();
		final int mx = end.ordinal();
		if (mx < mn) throw new IllegalArgumentException("The ordinal of " + end + " (" + mx +
			") must be at least equal to the ordinal of " + start + " (" + mn + ")");
		EnumOrderedSet coll = new EnumOrderedSet();
		coll.add(start);
		for (int i = mn + 1; i <= mx; i++) {
			coll.add(coll.universe[i]);
		}
		return coll;
	}
}

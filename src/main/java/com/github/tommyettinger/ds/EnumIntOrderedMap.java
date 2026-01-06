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

import com.github.tommyettinger.digital.Base;
import com.github.tommyettinger.ds.support.sort.IntComparator;
import com.github.tommyettinger.ds.support.sort.IntComparators;
import com.github.tommyettinger.ds.support.util.Appender;
import com.github.tommyettinger.ds.support.util.IntAppender;
import com.github.tommyettinger.ds.support.util.PartialParser;
import com.github.tommyettinger.function.ObjToObjFunction;

import java.util.*;

/**
 * An insertion-ordered map where the keys are {@code Enum}s and values are primitive ints. Null keys are not allowed.
 * Unlike {@link java.util.EnumMap}, this does not require a Class at construction time, which can be useful for serialization
 * purposes. No allocation is done unless this is changing its table size and/or key universe.
 * <br>
 * This class never actually hashes keys in its primary operations (get(), put(), remove(), containsKey(), etc.), since it can
 * rely on keys having an Enum type, and so having {@link Enum#ordinal()} available. The ordinal allows constant-time access
 * to a guaranteed-unique {@code int} that will always be non-negative and less than the size of the key universe. The table of
 * possible values always starts sized to fit exactly as many values as there are keys in the key universe.
 * <br>
 * The key universe is an important concept here; it is simply an array of all possible Enum values the EnumIntOrderedMap can use as keys, in
 * the specific order they are declared. You almost always get a key universe by calling {@code MyEnum.values()}, but you
 * can also use {@link Class#getEnumConstants()} for an Enum class. You can and generally should reuse key universes in order to
 * avoid allocations and/or save memory; the constructor {@link #EnumIntOrderedMap(Enum[])} (with no values given) creates an empty EnumIntOrderedMap with
 * a given key universe. If you need to use the zero-argument constructor, you can, and the key universe will be obtained from the
 * first key placed into the EnumIntOrderedMap. You can also set the key universe with {@link #clearToUniverse(Enum[])}, in the process of
 * clearing the map.
 * <br>
 * This class tries to be as compatible as possible with {@link java.util.EnumMap} while using primitive keys,
 * though this expands on that where possible.
 *
 * @author Nathan Sweet (Keys, Values, Entries, and MapIterator, as well as general structure)
 * @author Tommy Ettinger (Enum-related adaptation)
 */
public class EnumIntOrderedMap extends EnumIntMap implements Ordered<Enum<?>> {

	protected final ObjectList<Enum<?>> ordering;

	/**
	 * Constructor that only specifies an OrderType; using this will postpone creating the key universe and allocating the value table until {@link #put} is
	 * first called (potentially indirectly). You can also use {@link #clearToUniverse} to set the key universe and value table.
	 *
	 * @param type either {@link OrderType#BAG} to use unreliable ordering with faster deletion, or anything else to
	 *             use a list type that takes longer to delete but maintains insertion order reliably
	 */
	public EnumIntOrderedMap(OrderType type) {
		ordering = type == OrderType.BAG ? new ObjectBag<>() : new ObjectList<>();
	}

	/**
	 * Initializes this map so that it has exactly enough capacity as needed to contain each Enum constant defined in
	 * {@code universe}, assuming universe stores every possible constant in one Enum type. This map will start empty.
	 * You almost always obtain universe from calling {@code values()} on an Enum type, and you can share one
	 * reference to one Enum array across many EnumIntOrderedMap instances if you don't modify the shared array. Sharing the same
	 * universe helps save some memory if you have (very) many EnumIntOrderedMap instances.
	 *
	 * @param universe almost always, the result of calling {@code values()} on an Enum type; used directly, not copied
	 * @param type     either {@link OrderType#BAG} to use unreliable ordering with faster deletion, or anything else to
	 *                 use a list type that takes longer to delete but maintains insertion order reliably
	 */
	public EnumIntOrderedMap(Enum<?>[] universe, OrderType type) {
		if (universe == null) {
			ordering = type == OrderType.BAG ? new ObjectBag<>() : new ObjectList<>();
			return;
		}
		this.keys = EnumSet.noneOf(universe);
		valueTable = new int[universe.length];
		ordering = type == OrderType.BAG ? new ObjectBag<>(universe.length) : new ObjectList<>(universe.length);
	}

	/**
	 * Initializes this map so that it has exactly enough capacity as needed to contain each Enum constant defined by the
	 * Class {@code universeClass}, assuming universeClass is non-null. This simply calls {@link #EnumIntOrderedMap(Enum[])}
	 * for convenience. Note that this constructor allocates a new array of Enum constants each time it is called, where
	 * if you use {@link #EnumIntOrderedMap(Enum[])}, you can reuse an unmodified array to reduce allocations.
	 *
	 * @param universeClass the Class of an Enum type that defines the universe of valid Enum items this can hold
	 * @param type          either {@link OrderType#BAG} to use unreliable ordering with faster deletion, or anything else to
	 *                      use a list type that takes longer to delete but maintains insertion order reliably
	 */
	public EnumIntOrderedMap(Class<? extends Enum<?>> universeClass, OrderType type) {
		this(universeClass == null ? null : universeClass.getEnumConstants(), type);
	}

	/**
	 * Creates a new map identical to the specified EnumIntOrderedMap. This will share a key universe with the given EnumIntOrderedMap, if non-null.
	 * This overload allows specifying the OrderType independently of the one used in {@code map}.
	 *
	 * @param map  an EnumIntOrderedMap to copy
	 * @param type either {@link OrderType#BAG} to use unreliable ordering with faster deletion, or anything else to
	 *             use a list type that takes longer to delete but maintains insertion order reliably
	 */
	public EnumIntOrderedMap(EnumIntOrderedMap map, OrderType type) {
		this.keys = map.keys;
		if (map.valueTable != null)
			valueTable = Arrays.copyOf(map.valueTable, map.valueTable.length);
		defaultValue = map.defaultValue;
		ordering = type == OrderType.BAG ? new ObjectBag<>(map.ordering) : new ObjectList<>(map.ordering);

	}

	/**
	 * Given two side-by-side arrays, one of Enum keys, one of int values, this constructs a map and inserts each pair of key and
	 * value into it. If keys and values have different lengths, this only uses the length of the smaller array.
	 *
	 * @param keys   an array of Enum keys
	 * @param values an array of int values
	 * @param type   either {@link OrderType#BAG} to use unreliable ordering with faster deletion, or anything else to
	 *               use a list type that takes longer to delete but maintains insertion order reliably
	 */
	public EnumIntOrderedMap(Enum<?>[] keys, int[] values, OrderType type) {
		this(type);
		putAll(keys, values);
	}

	/**
	 * Given two side-by-side collections, one of Enum keys, one of int values, this constructs a map and inserts each pair of key
	 * and value into it. If keys and values have different lengths, this only uses the length of the smaller collection.
	 *
	 * @param keys   a Collection of Enum keys
	 * @param values a PrimitiveCollection of int values
	 * @param type   either {@link OrderType#BAG} to use unreliable ordering with faster deletion, or anything else to
	 *               use a list type that takes longer to delete but maintains insertion order reliably
	 */
	public EnumIntOrderedMap(Collection<? extends Enum<?>> keys, PrimitiveCollection.OfInt values, OrderType type) {
		this(type);
		putAll(keys, values);
	}

	/**
	 * Creates a new map by copying {@code count} items from the given EnumIntOrderedMap, starting at {@code offset} in that Map,
	 * into this. This overload allows specifying the OrderType independently of the one used in {@code other}.
	 *
	 * @param other  another EnumIntOrderedMap
	 * @param offset the first index in other's ordering to draw an item from
	 * @param count  how many items to copy from other
	 * @param type   either {@link OrderType#BAG} to use unreliable ordering with faster deletion, or anything else to
	 *               use a list type that takes longer to delete but maintains insertion order reliably
	 */
	public EnumIntOrderedMap(EnumIntOrderedMap other, int offset, int count, OrderType type) {
		this(other.keys == null ? null : other.keys.universe, type);
		putAll(0, other, offset, count);
	}

	// using default order type

	/**
	 * Empty constructor; using this will postpone creating the key universe and allocating the value table until {@link #put} is
	 * first called (potentially indirectly). You can also use {@link #clearToUniverse} to set the key universe and value table.
	 */
	public EnumIntOrderedMap() {
		this(OrderType.LIST);
	}

	/**
	 * Initializes this map so that it has exactly enough capacity as needed to contain each Enum constant defined in
	 * {@code universe}, assuming universe stores every possible constant in one Enum type. This map will start empty.
	 * You almost always obtain universe from calling {@code values()} on an Enum type, and you can share one
	 * reference to one Enum array across many EnumIntOrderedMap instances if you don't modify the shared array. Sharing the same
	 * universe helps save some memory if you have (very) many EnumIntOrderedMap instances.
	 *
	 * @param universe almost always, the result of calling {@code values()} on an Enum type; used directly, not copied
	 */
	public EnumIntOrderedMap(Enum<?>[] universe) {
		this(universe, OrderType.LIST);
	}

	/**
	 * Initializes this map so that it has exactly enough capacity as needed to contain each Enum constant defined by the
	 * Class {@code universeClass}, assuming universeClass is non-null. This simply calls {@link #EnumIntOrderedMap(Enum[])}
	 * for convenience. Note that this constructor allocates a new array of Enum constants each time it is called, where
	 * if you use {@link #EnumIntOrderedMap(Enum[])}, you can reuse an unmodified array to reduce allocations.
	 *
	 * @param universeClass the Class of an Enum type that defines the universe of valid Enum items this can hold
	 */
	public EnumIntOrderedMap(Class<? extends Enum<?>> universeClass) {
		this(universeClass, OrderType.LIST);
	}

	/**
	 * Creates a new map identical to the specified EnumIntOrderedMap. This will share a key universe with the given EnumIntOrderedMap, if non-null.
	 * This overload uses the OrderType of the given map.
	 *
	 * @param map an EnumIntOrderedMap to copy
	 */
	public EnumIntOrderedMap(EnumIntOrderedMap map) {
		this.keys = map.keys;
		if (map.valueTable != null)
			valueTable = Arrays.copyOf(map.valueTable, map.valueTable.length);
		defaultValue = map.defaultValue;
		ordering = map.ordering instanceof ObjectBag ? new ObjectBag<>(map.ordering) : new ObjectList<>(map.ordering);
	}

	/**
	 * Given two side-by-side arrays, one of Enum keys, one of int values, this constructs a map and inserts each pair of key and
	 * value into it. If keys and values have different lengths, this only uses the length of the smaller array.
	 *
	 * @param keys   an array of Enum keys
	 * @param values an array of int values
	 */
	public EnumIntOrderedMap(Enum<?>[] keys, int[] values) {
		this(keys, values, OrderType.LIST);
	}

	/**
	 * Given two side-by-side collections, one of Enum keys, one of int values, this constructs a map and inserts each pair of key
	 * and value into it. If keys and values have different lengths, this only uses the length of the smaller collection.
	 *
	 * @param keys   a Collection of Enum keys
	 * @param values a PrimitiveCollection of int values
	 */
	public EnumIntOrderedMap(Collection<? extends Enum<?>> keys, PrimitiveCollection.OfInt values) {
		this(keys, values, OrderType.LIST);
	}

	/**
	 * Creates a new map by copying {@code count} items from the given EnumIntOrderedMap, starting at {@code offset} in that Map,
	 * into this. This overload uses the OrderType of the given map.
	 *
	 * @param other  another EnumIntOrderedMap
	 * @param offset the first index in other's ordering to draw an item from
	 * @param count  how many items to copy from other
	 */
	public EnumIntOrderedMap(EnumIntOrderedMap other, int offset, int count) {
		this(other.keys == null ? null : other.keys.universe, other.ordering instanceof ObjectBag ? OrderType.BAG : OrderType.LIST);
		putAll(0, other, offset, count);
	}

	/**
	 * Returns the old value associated with the specified key, or this map's {@link #defaultValue} if there was no prior value.
	 * If this EnumIntOrderedMap does not yet have a key universe and/or value table, this gets the key universe from {@code key} and uses it
	 * from now on for this EnumIntOrderedMap.
	 *
	 * @param key   the Enum key to try to place into this EnumIntOrderedMap
	 * @param value the int value to associate with {@code key}
	 * @return the previous value associated with {@code key}, or {@link #getDefaultValue()} if the given key was not present
	 */
	public int put(Enum<?> key, int value) {
		if (key == null) return defaultValue;
		Enum<?>[] universe = key.getDeclaringClass().getEnumConstants();
		if (keys == null) keys = new EnumSet();
		if (valueTable == null) valueTable = new int[universe.length];
		int i = key.ordinal();
		if (i >= valueTable.length || universe[i] != key)
			throw new ClassCastException("Incompatible key for the EnumIntOrderedMap's universe.");
		int oldValue = valueTable[i];
		valueTable[i] = value;
		if (keys.add(key)) {
			ordering.add(key);
			return defaultValue;
		}
		return oldValue;
	}

	/**
	 * Puts the given key and value into this map at the given index in its order.
	 * If the key is already present at a different index, it is moved to the given index and its
	 * value is set to the given value.
	 *
	 * @param key   an Enum key; must not be null
	 * @param value a int value
	 * @param index the index in the order to place the given key and value; must be non-negative and less than {@link #size()}
	 * @return the previous value associated with key, if there was one, or {@link #defaultValue} otherwise
	 */
	public int put(Enum<?> key, int value, int index) {
		if (key == null) return defaultValue;
		Enum<?>[] universe = key.getDeclaringClass().getEnumConstants();
		if (keys == null) keys = new EnumSet();
		if (valueTable == null) valueTable = new int[universe.length];
		int i = key.ordinal();
		if (i >= valueTable.length || universe[i] != key)
			throw new ClassCastException("Incompatible key for the EnumIntOrderedMap's universe.");
		int oldValue = valueTable[i];
		valueTable[i] = value;
		if (keys.add(key)) {
			ordering.insert(index, key);
			return defaultValue;
		}
		int oldIndex = ordering.indexOf(key);
		if (oldIndex != index) {
			ordering.insert(index, ordering.removeAt(oldIndex));
		}
		return oldValue;
	}

	@Override
	public int putOrDefault(Enum<?> key, int value, int defaultValue) {
		if (key == null) return defaultValue;
		Enum<?>[] universe = key.getDeclaringClass().getEnumConstants();
		if (keys == null) keys = new EnumSet();
		if (valueTable == null) valueTable = new int[universe.length];
		int i = key.ordinal();
		if (i >= valueTable.length || universe[i] != key)
			throw new ClassCastException("Incompatible key for the EnumIntOrderedMap's universe.");
		int oldValue = valueTable[i];
		valueTable[i] = value;
		if (keys.add(key)) {
			ordering.add(key);
			return defaultValue;
		}
		return oldValue;
	}

	/**
	 * Puts every key-value pair in the given map into this, with the values from the given map
	 * overwriting the previous values if two keys are identical. This will put keys in the order of the given map.
	 *
	 * @param map a map with compatible key and value types; will not be modified
	 */
	public void putAll(EnumIntOrderedMap map) {
		for (int i = 0, kl = map.size(); i < kl; i++) {
			put(map.keyAt(i), map.getAt(i));
		}
	}

	/**
	 * Adds up to {@code count} entries, starting from {@code offset}, in the map {@code other} to this set,
	 * inserting at the end of the iteration order.
	 *
	 * @param other  a non-null ordered map with the same type and compatible generic types
	 * @param offset the first index in {@code other} to use
	 * @param count  how many indices in {@code other} to use
	 */
	public void putAll(EnumIntOrderedMap other, int offset, int count) {
		putAll(size(), other, offset, count);
	}

	/**
	 * Adds up to {@code count} entries, starting from {@code offset}, in the map {@code other} to this set,
	 * inserting starting at {@code insertionIndex} in the iteration order.
	 *
	 * @param insertionIndex where to insert into the iteration order
	 * @param other          a non-null ordered map with the same type and compatible generic types
	 * @param offset         the first index in {@code other} to use
	 * @param count          how many indices in {@code other} to use
	 */
	public void putAll(int insertionIndex, EnumIntOrderedMap other, int offset, int count) {
		int end = Math.min(offset + count, other.size());
		for (int i = offset; i < end; i++) {
			put(other.keyAt(i), other.getAt(i), insertionIndex++);
		}
	}

	@Override
	public int remove(Object key) {
		// If key is not present, using an O(1) containsKey() lets us avoid an O(n) remove step on keys.
		if (!super.containsKey(key)) {
			return defaultValue;
		}
		ordering.remove(key);
		return super.remove(key);
	}

	/**
	 * Removes the entry at the given index in the order, returning the value of that entry.
	 *
	 * @param index the index of the entry to remove; must be at least 0 and less than {@link #size()}
	 * @return the value of the removed entry
	 */
	public int removeAt(int index) {
		return super.remove(ordering.removeAt(index));
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
	 * Reduces the size of the map to the specified size. If the map is already smaller than the specified
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
	 * Copies all the mappings from the specified map to this map
	 * (optional operation).  The effect of this call is equivalent to that
	 * of calling {@link #put(Enum, int) put(k, v)} on this map once
	 * for each mapping from key {@code k} to value {@code v} in the
	 * specified map.  The behavior of this operation is undefined if the
	 * specified map is modified while the operation is in progress.
	 * <br>
	 * Note that {@link #putAll(EnumIntOrderedMap)} is more specific and can be
	 * more efficient by using the internal details of this class.
	 *
	 * @param map mappings to be stored in this map
	 * @throws UnsupportedOperationException if the {@code putAll} operation
	 *                                       is not supported by this map
	 * @throws ClassCastException            if the class of a key or value in the
	 *                                       specified map prevents it from being stored in this map
	 * @throws NullPointerException          if the specified map is null, or if
	 *                                       this map does not permit null keys or values, and the
	 *                                       specified map contains null keys or values
	 * @throws IllegalArgumentException      if some property of a key or value in
	 *                                       the specified map prevents it from being stored in this map
	 */
	public void putAll(ObjectIntOrderedMap<Enum<?>> map) {
		for (int i = 0, kl = map.size(); i < kl; i++) {
			put(map.keyAt(i), map.getAt(i));
		}
	}

	@Override
	public int getAndIncrement(Enum<?> key, int defaultValue, int increment) {
		if (key == null) return defaultValue;
		Enum<?>[] universe = key.getDeclaringClass().getEnumConstants();
		if (keys == null) keys = new EnumSet();
		if (valueTable == null) valueTable = new int[universe.length];
		int i = key.ordinal();
		if (i >= valueTable.length || universe[i] != key)
			throw new ClassCastException("Incompatible key for the EnumIntOrderedMap's universe.");
		int oldValue = valueTable[i];
		if (keys.add(key)) {
			valueTable[i] = defaultValue + increment;
			return defaultValue;
		}
		valueTable[i] += increment;
		ordering.add(key);
		return oldValue;
	}

	/**
	 * Changes the key {@code before} to {@code after} without changing its position in the order or its value. Returns true if
	 * {@code after} has been added to the EnumIntOrderedMap and {@code before} has been removed; returns false if {@code after} is
	 * already present or {@code before} is not present. If you are iterating over an EnumIntOrderedMap and have an index, you should
	 * prefer {@link #alterAt(int, Enum)}, which doesn't need to search for an index like this does and so can be faster.
	 *
	 * @param before a key that must be present for this to succeed
	 * @param after  a key that must not be in this map for this to succeed
	 * @return true if {@code before} was removed and {@code after} was added, false otherwise
	 */
	public boolean alter(Enum<?> before, Enum<?> after) {
		if (before == null || after == null || containsKey(after)) {
			return false;
		}
		int index = ordering.indexOf(before);
		if (index == -1) {
			return false;
		}
		super.put(after, super.remove(before));
		ordering.set(index, after);
		return true;
	}

	/**
	 * Changes the key at the given {@code index} in the order to {@code after}, without changing the ordering of other entries or
	 * any values. If {@code after} is already present, this returns false; it will also return false if {@code index} is invalid
	 * for the size of this map. Otherwise, it returns true. Unlike {@link #alter(Enum, Enum)}, this operates in constant time.
	 *
	 * @param index the index in the order of the key to change; must be non-negative and less than {@link #size}
	 * @param after the key that will replace the contents at {@code index}; this key must not be present for this to succeed
	 * @return true if {@code after} successfully replaced the key at {@code index}, false otherwise
	 */
	public boolean alterAt(int index, Enum<?> after) {
		if (after == null || index < 0 || index >= size() || containsKey(after)) {
			return false;
		}
		super.put(after, super.remove(ordering.get(index)));
		ordering.set(index, after);
		return true;
	}

	/**
	 * Changes the value at a specified {@code index} in the iteration order to {@code v}, without changing keys at all.
	 * If {@code index} isn't currently a valid index in the iteration order, this returns null. Otherwise, it returns the
	 * value that was previously held at {@code index}, which may also be null.
	 *
	 * @param v     the new int value to assign
	 * @param index the index in the iteration order to set {@code v} at
	 * @return the previous value held at {@code index} in the iteration order, which may be null if the value was null or if {@code index} was invalid
	 */
	public int setAt(int index, int v) {
		if (index < 0 || index >= size() || keys == null || valueTable == null) {
			return defaultValue;
		}
		final int pos = ordering.get(index).ordinal();
		final int oldValue = valueTable[pos];
		valueTable[pos] = v;
		return oldValue;
	}

	/**
	 * Gets the int value at the given {@code index} in the insertion order. The index should be between 0
	 * (inclusive) and {@link #size()} (exclusive).
	 *
	 * @param index an index in the insertion order, between 0 (inclusive) and {@link #size()} (exclusive)
	 * @return the value at the given index
	 */
	public int getAt(int index) {
		return get(ordering.get(index));
	}

	/**
	 * Gets the K key at the given {@code index} in the insertion order. The index should be between 0
	 * (inclusive) and {@link #size()} (exclusive).
	 *
	 * @param index an index in the insertion order, between 0 (inclusive) and {@link #size()} (exclusive)
	 * @return the key at the given index
	 */
	public Enum<?> keyAt(int index) {
		return ordering.get(index);
	}

	@Override
	public void clear() {
		ordering.clear();
		super.clear();
	}

	@Override
	public void clearToUniverse(Enum<?>[] universe) {
		super.clearToUniverse(universe);
		ordering.clear();
	}

	@Override
	public void clearToUniverse(Class<? extends Enum<?>> universe) {
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
	 * Sorts this EnumIntOrderedMap in-place by the keys' natural ordering.
	 */
	public void sort() {
		ordering.sort(null);
	}

	/**
	 * Sorts this EnumIntOrderedMap in-place by the given Comparator used on the keys. If {@code comp} is null, then this
	 * will sort by the natural ordering of the keys.
	 *
	 * @param comp a Comparator that can compare two {@code Enum} keys, or null to use the keys' natural ordering
	 */
	public void sort(Comparator<? super Enum<?>> comp) {
		ordering.sort(comp);
	}

	/**
	 * Sorts this EnumIntOrderedMap in-place by the given IntComparator used on the values. {@code comp} must not be null,
	 * and must be able to compare {@code int} values. You can use {@link IntComparators#NATURAL_COMPARATOR} to do
	 * what {@link #sort()} does (just sorting values in this case instead of keys); there is also a reversed comparator
	 * available, {@link IntComparators#OPPOSITE_COMPARATOR}.
	 *
	 * @param comp a non-null {@link IntComparator}
	 */
	public void sortByValue(IntComparator comp) {
		ordering.sort((a, b) -> comp.compare(get(a), get(b)));
	}

	/**
	 * Returns a {@link Set} view of the keys contained in this map.
	 * The set is backed by the map, so changes to the map are
	 * reflected in the set, and vice versa.  If the map is modified
	 * while an iteration over the set is in progress (except through
	 * the iterator's own {@code remove} operation), the results of
	 * the iteration are undefined.  The set supports element removal,
	 * which removes the corresponding mapping from the map, via the
	 * {@code Iterator.remove}, {@code Set.remove},
	 * {@code removeAll}, {@code retainAll}, and {@code clear}
	 * operations.  It does not support the {@code add} or {@code addAll}
	 * operations.
	 *
	 * @return a set view of the keys contained in this map
	 */
	@Override
	public Keys keySet() {
		return new OrderedMapKeys(this);
	}

	/**
	 * Returns a PrimitiveCollection for the values in the map. Remove is supported by the PrimitiveCollection's iterator.
	 *
	 * @return a {@link PrimitiveCollection.OfInt} of the int values
	 */
	@Override
	public Values values() {
		return new OrderedMapValues(this);
	}

	/**
	 * Returns a Set of Map.Entry, containing the entries in the map. Remove is supported by the Set's iterator.
	 *
	 * @return a {@link Set} of {@link Map.Entry} key-value pairs
	 */
	@Override
	public Entries entrySet() {
		return new OrderedMapEntries(this);
	}

	/**
	 * Creates a new {@link OrderedMapEntries} and gets its iterator.
	 * You can remove an Entry from this map using this Iterator.
	 *
	 * @return an {@link Iterator} over key-value pairs as {@link Entry} values
	 */
	@Override
	public EntryIterator iterator() {
		return entrySet().iterator();
	}

	/**
	 * Appends to a StringBuilder from the contents of this EnumIntOrderedMap, but uses the given {@link Appender} and
	 * {@link IntAppender} to convert each key and each value to a customizable representation and append them
	 * to a StringBuilder. These functions are often method references to methods in Base, such as
	 * {@link Base#appendUnsigned(CharSequence, int)}. To use
	 * the default toString representation, you can use {@code Appender::append} as an appender, or to use the readable
	 * Enum {@link Enum#name()}, use {@link Appender#ENUM_NAME_APPENDER}. Use {@link IntAppender#DEFAULT} or
	 * {@link IntAppender#READABLE} for human-readable or source-code-readable results, respectively.
	 *
	 * @param sb                a StringBuilder that this can append to
	 * @param entrySeparator    how to separate entries, such as {@code ", "}
	 * @param keyValueSeparator how to separate each key from its value, such as {@code "="} or {@code ":"}
	 * @param braces            true to wrap the output in curly braces, or false to omit them
	 * @param keyAppender       a function that takes a StringBuilder and a K, and returns the modified StringBuilder
	 * @param valueAppender     a function that takes a StringBuilder and a int, and returns the modified StringBuilder
	 * @return {@code sb}, with the appended keys and values of this map
	 */
	@Override
	public StringBuilder appendTo(StringBuilder sb, String entrySeparator, String keyValueSeparator, boolean braces, Appender<Enum<?>> keyAppender, IntAppender valueAppender) {
		if (size() == 0) {
			return braces ? sb.append("{}") : sb;
		}
		if (braces) {
			sb.append('{');
		}
		ObjectList<Enum<?>> keys = this.ordering;
		for (int i = 0, n = keys.size(); i < n; i++) {
			Enum<?> key = keys.get(i);
			if (i > 0) {
				sb.append(entrySeparator);
			}
			keyAppender.apply(sb, key);
			sb.append(keyValueSeparator);
			valueAppender.apply(sb, get(key));
		}
		if (braces) {
			sb.append('}');
		}
		return sb;
	}

	public static class OrderedMapEntries extends Entries {
		protected ObjectList<Enum<?>> ordering;

		public OrderedMapEntries(EnumIntOrderedMap map) {
			super(map);
			ordering = map.ordering;
			iter = new EntryIterator(map) {

				@Override
				public void reset() {
					currentIndex = -1;
					nextIndex = 0;
					hasNext = map.notEmpty();
				}

				@Override
				public Entry next() {
					if (!hasNext) {
						throw new NoSuchElementException();
					}
					currentIndex = nextIndex;
					entry.key = ordering.get(nextIndex);
					entry.value = map.get(entry.key);
					nextIndex++;
					hasNext = nextIndex < map.size();
					return entry;
				}

				@Override
				public void remove() {
					if (currentIndex < 0) {
						throw new IllegalStateException("next must be called before remove.");
					}
					if (entry.key != null) {
						map.remove(entry.key);
					}
					nextIndex--;
					currentIndex = -1;
				}
			};
		}

	}

	public static class OrderedMapKeys extends Keys {
		private final ObjectList<Enum<?>> ordering;

		public OrderedMapKeys(EnumIntOrderedMap map) {
			super(map);
			ordering = map.ordering;
			iter = new KeyIterator(map) {

				@Override
				public void reset() {
					currentIndex = -1;
					nextIndex = 0;
					hasNext = map.notEmpty();
				}

				@Override
				public Enum<?> next() {
					if (!hasNext) {
						throw new NoSuchElementException();
					}
					Enum<?> key = ordering.get(nextIndex);
					currentIndex = nextIndex;
					nextIndex++;
					hasNext = nextIndex < map.size();
					return key;
				}

				@Override
				public void remove() {
					if (currentIndex < 0) {
						throw new IllegalStateException("next must be called before remove.");
					}
					map.remove(ordering.get(currentIndex));
					nextIndex = currentIndex;
					currentIndex = -1;
				}
			};
		}

	}

	public static class OrderedMapValues extends Values {
		private final ObjectList<Enum<?>> ordering;

		public OrderedMapValues(EnumIntOrderedMap map) {
			super(map);
			ordering = map.ordering;
			iter = new ValueIterator(map) {

				@Override
				public void reset() {
					currentIndex = -1;
					nextIndex = 0;
					hasNext = map.notEmpty();
				}

				@Override
				public int nextInt() {
					if (!hasNext) {
						throw new NoSuchElementException();
					}
					int value = map.get(ordering.get(nextIndex));
					currentIndex = nextIndex;
					nextIndex++;
					hasNext = nextIndex < map.size();
					return value;
				}

				@Override
				public void remove() {
					if (currentIndex < 0) {
						throw new IllegalStateException("next must be called before remove.");
					}
					map.remove(ordering.get(currentIndex));
					nextIndex = currentIndex;
					currentIndex = -1;
				}
			};
		}

	}


	/**
	 * Constructs an empty map.
	 * This is usually less useful than just using the constructor, but can be handy
	 * in some code-generation scenarios when you don't know how many arguments you will have.
	 *
	 * @return a new map containing nothing
	 */
	public static EnumIntOrderedMap with() {
		return new EnumIntOrderedMap();
	}

	/**
	 * Constructs a single-entry map given one key and one value.
	 * This is mostly useful as an optimization for {@link #with(Enum, Number, Object...)}
	 * when there's no "rest" of the keys or values. Like the more-argument with(), this will
	 * convert its Number value to a primitive int, regardless of which Number type was used.
	 *
	 * @param key0   the first and only Enum key
	 * @param value0 the first and only value; will be converted to primitive int
	 * @return a new map containing just the entry mapping key0 to value0
	 */
	public static EnumIntOrderedMap with(Enum<?> key0, Number value0) {
		EnumIntOrderedMap map = new EnumIntOrderedMap();
		map.put(key0, value0.intValue());
		return map;
	}

	/**
	 * Constructs a map given alternating keys and values.
	 * This is mostly useful as an optimization for {@link #with(Enum, Number, Object...)}
	 * when there's no "rest" of the keys or values. Like the more-argument with(), this will
	 * convert its Number values to primitive ints, regardless of which Number type was used.
	 *
	 * @param key0   an Enum key
	 * @param value0 a Number for a value; will be converted to primitive int
	 * @param key1   an Enum key
	 * @param value1 a Number for a value; will be converted to primitive int
	 * @return a new map containing the given key-value pairs
	 */
	public static EnumIntOrderedMap with(Enum<?> key0, Number value0, Enum<?> key1, Number value1) {
		EnumIntOrderedMap map = new EnumIntOrderedMap();
		map.put(key0, value0.intValue());
		map.put(key1, value1.intValue());
		return map;
	}

	/**
	 * Constructs a map given alternating keys and values.
	 * This is mostly useful as an optimization for {@link #with(Enum, Number, Object...)}
	 * when there's no "rest" of the keys or values. Like the more-argument with(), this will
	 * convert its Number values to primitive ints, regardless of which Number type was used.
	 *
	 * @param key0   an Enum key
	 * @param value0 a Number for a value; will be converted to primitive int
	 * @param key1   an Enum key
	 * @param value1 a Number for a value; will be converted to primitive int
	 * @param key2   an Enum key
	 * @param value2 a Number for a value; will be converted to primitive int
	 * @return a new map containing the given key-value pairs
	 */
	public static EnumIntOrderedMap with(Enum<?> key0, Number value0, Enum<?> key1, Number value1, Enum<?> key2, Number value2) {
		EnumIntOrderedMap map = new EnumIntOrderedMap();
		map.put(key0, value0.intValue());
		map.put(key1, value1.intValue());
		map.put(key2, value2.intValue());
		return map;
	}

	/**
	 * Constructs a map given alternating keys and values.
	 * This is mostly useful as an optimization for {@link #with(Enum, Number, Object...)}
	 * when there's no "rest" of the keys or values. Like the more-argument with(), this will
	 * convert its Number values to primitive ints, regardless of which Number type was used.
	 *
	 * @param key0   an Enum key
	 * @param value0 a Number for a value; will be converted to primitive int
	 * @param key1   an Enum key
	 * @param value1 a Number for a value; will be converted to primitive int
	 * @param key2   an Enum key
	 * @param value2 a Number for a value; will be converted to primitive int
	 * @param key3   an Enum key
	 * @param value3 a Number for a value; will be converted to primitive int
	 * @return a new map containing the given key-value pairs
	 */
	public static EnumIntOrderedMap with(Enum<?> key0, Number value0, Enum<?> key1, Number value1, Enum<?> key2, Number value2, Enum<?> key3, Number value3) {
		EnumIntOrderedMap map = new EnumIntOrderedMap();
		map.put(key0, value0.intValue());
		map.put(key1, value1.intValue());
		map.put(key2, value2.intValue());
		map.put(key3, value3.intValue());
		return map;
	}

	/**
	 * Constructs a map given alternating keys and values.
	 * This can be useful in some code-generation scenarios, or when you want to make a
	 * map conveniently by-hand and have it populated at the start. You can also use
	 * {@link #EnumIntOrderedMap(Enum[], int[])}, which takes all keys and then all values.
	 * This needs all keys to be Enum constants.
	 * All values must be some type of boxed Number, such as {@link Integer}
	 * or {@link Double}, and will be converted to primitive {@code int}s. Any keys that don't
	 * have Enum as their type or values that aren't {@code Number}s have that entry skipped.
	 *
	 * @param key0   the first Enum key
	 * @param value0 the first value; will be converted to primitive int
	 * @param rest   an array or varargs of alternating Enum, Number, Enum, Number... elements
	 * @return a new map containing the given keys and values
	 */
	public static EnumIntOrderedMap with(Enum<?> key0, Number value0, Object... rest) {
		EnumIntOrderedMap map = new EnumIntOrderedMap();
		map.put(key0, value0.intValue());
		map.putPairs(rest);
		return map;
	}

	/**
	 * Creates a new map by parsing all of {@code str} with the given PartialParser for keys,
	 * with entries separated by {@code entrySeparator}, such as {@code ", "} and
	 * the keys separated from values by {@code keyValueSeparator}, such as {@code "="}.
	 * <br>
	 * The {@code keyParser} is often produced by {@link PartialParser#enumParser(ObjToObjFunction)}.
	 *
	 * @param str               a String containing parseable text
	 * @param entrySeparator    the String separating every key-value pair
	 * @param keyValueSeparator the String separating every key from its corresponding value
	 * @param keyParser         a PartialParser that returns an {@link Enum} key from a section of {@code str}
	 */
	public static EnumIntOrderedMap parse(String str,
										  String entrySeparator,
										  String keyValueSeparator,
										  PartialParser<Enum<?>> keyParser) {
		return parse(str, entrySeparator, keyValueSeparator, keyParser, false);
	}

	/**
	 * Creates a new map by parsing all of {@code str} (or if {@code brackets} is true, all but the first and last
	 * chars) with the given PartialParser for keys, with entries separated by {@code entrySeparator},
	 * such as {@code ", "} and the keys separated from values by {@code keyValueSeparator}, such as {@code "="}.
	 * <br>
	 * The {@code keyParser} is often produced by {@link PartialParser#enumParser(ObjToObjFunction)}.
	 *
	 * @param str               a String containing parseable text
	 * @param entrySeparator    the String separating every key-value pair
	 * @param keyValueSeparator the String separating every key from its corresponding value
	 * @param keyParser         a PartialParser that returns an {@link Enum} key from a section of {@code str}
	 * @param brackets          if true, the first and last chars in {@code str} will be ignored
	 */
	public static EnumIntOrderedMap parse(String str,
										  String entrySeparator,
										  String keyValueSeparator,
										  PartialParser<Enum<?>> keyParser,
										  boolean brackets) {
		EnumIntOrderedMap m = new EnumIntOrderedMap();
		if (brackets)
			m.putLegible(str, entrySeparator, keyValueSeparator, keyParser, 1, str.length() - 1);
		else
			m.putLegible(str, entrySeparator, keyValueSeparator, keyParser, 0, -1);
		return m;
	}

	/**
	 * Creates a new map by parsing the given subrange of {@code str} with the given PartialParser for keys,
	 * with entries separated by {@code entrySeparator}, such as {@code ", "} and the keys separated from values
	 * by {@code keyValueSeparator}, such as {@code "="}.
	 * <br>
	 * The {@code keyParser} is often produced by {@link PartialParser#enumParser(ObjToObjFunction)}.
	 *
	 * @param str               a String containing parseable text
	 * @param entrySeparator    the String separating every key-value pair
	 * @param keyValueSeparator the String separating every key from its corresponding value
	 * @param keyParser         a PartialParser that returns an {@link Enum} key from a section of {@code str}
	 * @param offset            the first position to read parseable text from in {@code str}
	 * @param length            how many chars to read; -1 is treated as maximum length
	 */
	public static EnumIntOrderedMap parse(String str,
										  String entrySeparator,
										  String keyValueSeparator,
										  PartialParser<Enum<?>> keyParser,
										  int offset,
										  int length) {
		EnumIntOrderedMap m = new EnumIntOrderedMap();
		m.putLegible(str, entrySeparator, keyValueSeparator, keyParser, offset, length);
		return m;
	}

	/**
	 * Constructs an empty map.
	 * This is usually less useful than just using the constructor, but can be handy
	 * in some code-generation scenarios when you don't know how many arguments you will have.
	 *
	 * @return a new map containing nothing
	 */
	public static EnumIntOrderedMap withPrimitive() {
		return new EnumIntOrderedMap();
	}

	/**
	 * Constructs a single-entry map given one key and one value.
	 * This is mostly useful as an optimization for {@link #with(Enum, Number, Object...)}
	 * when there's no "rest" of the keys or values. Unlike with(), this takes unboxed int as
	 * its value type, and will not box it.
	 *
	 * @param key0   an Enum for a key
	 * @param value0 a int for a value
	 * @return a new map containing just the entry mapping key0 to value0
	 */
	public static EnumIntOrderedMap withPrimitive(Enum<?> key0, int value0) {
		EnumIntOrderedMap map = new EnumIntOrderedMap();
		map.put(key0, value0);
		return map;
	}

	/**
	 * Constructs a map given alternating keys and values.
	 * This is mostly useful as an optimization for {@link #with(Enum, Number, Object...)}
	 * when there's no "rest" of the keys or values. Unlike with(), this takes unboxed int as
	 * its value type, and will not box it.
	 *
	 * @param key0   an Enum key
	 * @param value0 a int for a value
	 * @param key1   an Enum key
	 * @param value1 a int for a value
	 * @return a new map containing the given key-value pairs
	 */
	public static EnumIntOrderedMap withPrimitive(Enum<?> key0, int value0, Enum<?> key1, int value1) {
		EnumIntOrderedMap map = new EnumIntOrderedMap();
		map.put(key0, value0);
		map.put(key1, value1);
		return map;
	}

	/**
	 * Constructs a map given alternating keys and values.
	 * This is mostly useful as an optimization for {@link #with(Enum, Number, Object...)}
	 * when there's no "rest" of the keys or values.  Unlike with(), this takes unboxed int as
	 * its value type, and will not box it.
	 *
	 * @param key0   an Enum key
	 * @param value0 a int for a value
	 * @param key1   an Enum key
	 * @param value1 a int for a value
	 * @param key2   an Enum key
	 * @param value2 a int for a value
	 * @return a new map containing the given key-value pairs
	 */
	public static EnumIntOrderedMap withPrimitive(Enum<?> key0, int value0, Enum<?> key1, int value1, Enum<?> key2, int value2) {
		EnumIntOrderedMap map = new EnumIntOrderedMap();
		map.put(key0, value0);
		map.put(key1, value1);
		map.put(key2, value2);
		return map;
	}

	/**
	 * Constructs a map given alternating keys and values.
	 * This is mostly useful as an optimization for {@link #with(Enum, Number, Object...)}
	 * when there's no "rest" of the keys or values.  Unlike with(), this takes unboxed int as
	 * its value type, and will not box it.
	 *
	 * @param key0   an Enum key
	 * @param value0 a int for a value
	 * @param key1   an Enum key
	 * @param value1 a int for a value
	 * @param key2   an Enum key
	 * @param value2 a int for a value
	 * @param key3   an Enum key
	 * @param value3 a int for a value
	 * @return a new map containing the given key-value pairs
	 */
	public static EnumIntOrderedMap withPrimitive(Enum<?> key0, int value0, Enum<?> key1, int value1, Enum<?> key2, int value2, Enum<?> key3, int value3) {
		EnumIntOrderedMap map = new EnumIntOrderedMap();
		map.put(key0, value0);
		map.put(key1, value1);
		map.put(key2, value2);
		map.put(key3, value3);
		return map;
	}

	/**
	 * Constructs an empty map.
	 * This is usually less useful than just using the constructor, but can be handy
	 * in some code-generation scenarios when you don't know how many arguments you will have.
	 * This is an alias for {@link #with()}.
	 *
	 * @return a new map containing nothing
	 */
	public static EnumIntOrderedMap of() {
		return with();
	}

	/**
	 * Constructs a map given alternating keys and values.
	 * This can be useful in some code-generation scenarios, or when you want to make a
	 * map conveniently by-hand and have it populated at the start. You can also use
	 * {@link #EnumIntOrderedMap(Enum[], int[])}, which takes all keys and then all values.
	 * This needs all keys to be Enum constants.
	 * All values must be some type of boxed Number, such as {@link Integer}
	 * or {@link Double}, and will be converted to primitive {@code int}s. Any keys that don't
	 * have Enum as their type or values that aren't {@code Number}s have that entry skipped.
	 * This is an alias for {@link #with(Enum, Number, Object...)}.
	 *
	 * @param key0   the first Enum key
	 * @param value0 the first value; will be converted to primitive int
	 * @param rest   an array or varargs of alternating Enum, Number, Enum, Number... elements
	 * @return a new map containing the given keys and values
	 */
	public static EnumIntOrderedMap of(Enum<?> key0, Number value0, Object... rest) {
		return with(key0, value0, rest);
	}
}

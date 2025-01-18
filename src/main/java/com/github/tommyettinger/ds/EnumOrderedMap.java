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

import com.github.tommyettinger.ds.support.util.Appender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;

/**
 * An {@link EnumMap} that also stores keys in an {@link ObjectList} using the insertion order. Null keys are not allowed. No
 * allocation is done except when growing the table size.
 * Unlike {@link java.util.EnumMap}, this does not require a Class at construction time, which can be
 * useful for serialization purposes. Instead of storing a Class, this holds a "key universe" (which is almost always the
 * same as an array returned by calling {@code values()} on an Enum type), and key universes are ideally shared between
 * compatible EnumOrderedMaps. No allocation is done unless this is changing its table size and/or key universe. You can change
 * the ordering of the Enum items using methods like {@link #sort(Comparator)} and {@link #shuffle(Random)}. You can also
 * access enums via their index in the ordering, using methods such as {@link #getAt(int)}, {@link #alterAt(int, Enum)},
 * and {@link #removeAt(int)}.
 * <br>
 * The key universe is an important concept here; it is simply an array of all possible Enum values the EnumOrderedMap can use as keys, in
 * the specific order they are declared. You almost always get a key universe by calling {@code MyEnum.values()}, but you
 * can also use {@link Class#getEnumConstants()} for an Enum class. You can and generally should reuse key universes in order to
 * avoid allocations and/or save memory; the static method {@link #noneOf(Enum[])} creates an empty EnumOrderedMap with
 * a given key universe. If you need to use the zero-argument constructor, you can, and the key universe will be obtained from the
 * first key placed into the EnumOrderedMap, though it won't be shared at first. You can also set the key universe with
 * {@link #clearToUniverse(Enum[])}, in the process of clearing the map.
 * <br>
 * {@link #iterator() Iteration} is ordered and faster than an unordered map. Keys can also be accessed and the order changed
 * using {@link #order()}. There is some additional overhead for put and remove.
 * Unordered sets and maps are not designed to provide especially fast iteration. Iteration is faster with {@link Ordered} types like
 * EnumOrderedSet and EnumOrderedMap.
 * <br>
 * This class tries to be as compatible as possible with {@link java.util.EnumMap}, though this expands on that where possible.
 *
 * @author Nathan Sweet
 * @author Tommy Ettinger
 */
public class EnumOrderedMap<V> extends EnumMap<V> implements Ordered<Enum<?>> {

	protected final ObjectList<Enum<?>> ordering;


	/**
	 * Empty constructor; using this will postpone creating the key universe and allocating the value table until {@link #put} is
	 * first called (potentially indirectly). You can also use {@link #clearToUniverse} to set the key universe and value table.
	 */
	public EnumOrderedMap () {
		ordering = new ObjectList<>();
	}

	/**
	 * Initializes this map so that it has exactly enough capacity as needed to contain each Enum constant defined in
	 * {@code universe}, assuming universe stores every possible constant in one Enum type. This map will start empty.
	 * You almost always obtain universe from calling {@code values()} on an Enum type, and you can share one
	 * reference to one Enum array across many EnumOrderedMap instances if you don't modify the shared array. Sharing the same
	 * universe helps save some memory if you have (very) many EnumOrderedMap instances.
	 * @param universe almost always, the result of calling {@code values()} on an Enum type; used directly, not copied
	 */
	public EnumOrderedMap (Enum<?> @Nullable [] universe) {
		super();
		if(universe == null) {
			ordering = new ObjectList<>();
			return;
		}
		this.universe = universe;
		valueTable = new Object[universe.length];
		ordering = new ObjectList<>(universe.length);
	}

	/**
	 * Initializes this map so that it has exactly enough capacity as needed to contain each Enum constant defined by the
	 * Class {@code universeClass}, assuming universeClass is non-null. This simply calls {@link #EnumOrderedMap(Enum[])}
	 * for convenience. Note that this constructor allocates a new array of Enum constants each time it is called, where
	 * if you use {@link #EnumOrderedMap(Enum[])}, you can reuse an unmodified array to reduce allocations.
	 * @param universeClass the Class of an Enum type that defines the universe of valid Enum items this can hold
	 */
	public EnumOrderedMap (@Nullable Class<? extends Enum<?>> universeClass) {
		this(universeClass == null ? null : universeClass.getEnumConstants());
	}

	/**
	 * Creates a new map identical to the specified EnumOrderedMap. This will share a key universe with the given EnumOrderedMap, if non-null.
	 *
	 * @param map an EnumMap to copy
	 */
	public EnumOrderedMap (EnumOrderedMap<? extends V> map) {
		universe = map.universe;
		if(map.valueTable != null)
			valueTable = Arrays.copyOf(map.valueTable, map.valueTable.length);
		size = map.size;
		defaultValue = map.defaultValue;
		this.ordering = new ObjectList<>(map.ordering);
	}

	/**
	 * Creates a new map identical to the specified map.
	 *
	 * @param map a Map to copy; EnumOrderedMap will be faster
	 */
	public EnumOrderedMap (Map<? extends Enum<?>, ? extends V> map) {
		this();
		putAll(map);
	}

	/**
	 * Given two side-by-side arrays, one of Enum keys, one of V values, this constructs a map and inserts each pair of key and
	 * value into it. If keys and values have different lengths, this only uses the length of the smaller array.
	 *
	 * @param keys   an array of Enum keys
	 * @param values an array of V values
	 */
	public EnumOrderedMap (Enum<?>[] keys, V[] values) {
		this();
		putAll(keys, values);
	}

	/**
	 * Given two side-by-side collections, one of Enum keys, one of V values, this constructs a map and inserts each pair of key
	 * and value into it. If keys and values have different lengths, this only uses the length of the smaller collection.
	 *
	 * @param keys   a Collection of Enum keys
	 * @param values a Collection of V values
	 */
	public EnumOrderedMap (Collection<? extends Enum<?>> keys, Collection<? extends V> values) {
		this();
		putAll(keys, values);
	}

	/**
	 * Creates a new map by copying {@code count} items from the given EnumOrderedMap, starting at {@code offset} in that Map,
	 * into this.
	 *
	 * @param other  another EnumOrderedMap of the same types
	 * @param offset the first index in other's ordering to draw an item from
	 * @param count  how many items to copy from other
	 */
	public EnumOrderedMap(EnumOrderedMap<? extends V> other, int offset, int count) {
		this();
		putAll(0, other, offset, count);
	}


	/**
	 * Returns the old value associated with the specified key, or this map's {@link #defaultValue} if there was no prior value.
	 * If this EnumOrderedMap does not yet have a key universe and/or value table, this gets the key universe from {@code key} and uses it
	 * from now on for this EnumOrderedMap.
	 *
	 * @param key the Enum key to try to place into this EnumOrderedMap
	 * @param value the V value to associate with {@code key}
	 * @return the previous value associated with {@code key}, or {@link #getDefaultValue()} if the given key was not present
	 */
	@Override
	@Nullable
	public V put (Enum<?> key, @Nullable V value) {
		if(key == null) throw new NullPointerException("Keys added to an EnumMap must not be null.");
		if(universe == null) universe = key.getDeclaringClass().getEnumConstants();
		if(valueTable == null) valueTable = new Object[universe.length];
		int i = key.ordinal();
		if(i >= valueTable.length || universe[i] != key)
			throw new ClassCastException("Incompatible key for the EnumMap's universe.");
		Object oldValue = valueTable[i];
		valueTable[i] = hold(value);
		if (oldValue != null) {
			// Existing key was found.
			return release(oldValue);
		}
		ordering.add(key);
		++size;
		return defaultValue;
	}

	/**
	 * Puts the given key and value into this map at the given index in its order.
	 * If the key is already present at a different index, it is moved to the given index and its
	 * value is set to the given value.
	 *
	 * @param key   an Enum key; must not be null
	 * @param value a V value; permitted to be null
	 * @param index the index in the order to place the given key and value; must be non-negative and less than {@link #size()}
	 * @return the previous value associated with key, if there was one, or null otherwise
	 */
	@Nullable
	public V put (Enum<?> key, @Nullable V value, int index) {
		if(key == null) throw new NullPointerException("Keys added to an EnumMap must not be null.");
		if(universe == null) universe = key.getDeclaringClass().getEnumConstants();
		if(valueTable == null) valueTable = new Object[universe.length];
		int i = key.ordinal();
		if(i >= valueTable.length || universe[i] != key)
			throw new ClassCastException("Incompatible key for the EnumMap's universe.");
		Object oldValue = valueTable[i];
		valueTable[i] = hold(value);
		if (oldValue != null) {
			// Existing key was found.
			int oldIndex = ordering.indexOf(key);
			if (oldIndex != index) {
				ordering.insert(index, ordering.removeAt(oldIndex));}
			return release(oldValue);
		}
		ordering.add(key);
		ordering.insert(index, key);
		++size;
		return defaultValue;
	}

	@Nullable
	@Override
	public V putOrDefault (Enum<?> key, @Nullable V value, @Nullable V defaultValue) {
		if(key == null) throw new NullPointerException("Keys added to an EnumMap must not be null.");
		if(universe == null) universe = key.getDeclaringClass().getEnumConstants();
		if(valueTable == null) valueTable = new Object[universe.length];
		int i = key.ordinal();
		if(i >= valueTable.length || universe[i] != key)
			throw new ClassCastException("Incompatible key for the EnumMap's universe.");
		Object oldValue = valueTable[i];
		valueTable[i] = hold(value);
		if (oldValue != null) {
			// Existing key was found.
			return release(oldValue);
		}
		ordering.add(key);
		++size;
		return defaultValue;
	}

	/**
	 * Puts every key-value pair in the given map into this, with the values from the given map
	 * overwriting the previous values if two keys are identical. This will put keys in the order of the given map.
	 *
	 * @param map a map with compatible key and value types; will not be modified
	 */
	public void putAll (EnumOrderedMap<? extends V> map) {
		for (int i = 0, kl = map.size; i < kl; i++) {
			put(map.keyAt(i), map.getAt(i));
		}
	}

	/**
	 * Adds up to {@code count} entries, starting from {@code offset}, in the map {@code other} to this map,
	 * inserting at the end of the iteration order.
	 *
	 * @param other  a non-null ordered map with the same type and compatible generic types
	 * @param offset the first index in {@code other} to use
	 * @param count  how many indices in {@code other} to use
	 */
	public void putAll (EnumOrderedMap<? extends V> other, int offset, int count) {
		putAll(size, other, offset, count);
	}

	/**
	 * Adds up to {@code count} entries, starting from {@code offset}, in the map {@code other} to this map,
	 * inserting starting at {@code insertionIndex} in the iteration order.
	 *
	 * @param insertionIndex where to insert into the iteration order
	 * @param other          a non-null ordered map with the same type and compatible generic types
	 * @param offset         the first index in {@code other} to use
	 * @param count          how many indices in {@code other} to use
	 */
	public void putAll (int insertionIndex, EnumOrderedMap<? extends V> other, int offset, int count) {
		int end = Math.min(offset + count, other.size());
		for (int i = offset; i < end; i++) {
			put(other.keyAt(i), other.getAt(i), insertionIndex++);
		}
	}

	@Override
	public V remove (Object key) {
		// If key is not present, using an O(1) containsKey() lets us avoid an O(n) remove step on keys.
		if (!super.containsKey(key)) {return defaultValue;}
		ordering.remove(key);
		return super.remove(key);
	}

	/**
	 * Removes the entry at the given index in the order, returning the value of that entry.
	 *
	 * @param index the index of the entry to remove; must be at least 0 and less than {@link #size()}
	 * @return the value of the removed entry
	 */
	@Nullable
	public V removeAt (int index) {
		return super.remove(ordering.removeAt(index));
	}

	/**
	 * Changes the key {@code before} to {@code after} without changing its position in the order or its value. Returns true if
	 * {@code after} has been added to the EnumOrderedMap and {@code before} has been removed; returns false if {@code after} is
	 * already present or {@code before} is not present. If you are iterating over an EnumOrderedMap and have an index, you should
	 * prefer {@link #alterAt(int, Enum)}, which doesn't need to search for an index like this does and so can be faster.
	 *
	 * @param before a key that must be present for this to succeed
	 * @param after  a key that must not be in this map for this to succeed
	 * @return true if {@code before} was removed and {@code after} was added, false otherwise
	 */
	public boolean alter (Enum<?> before, Enum<?> after) {
		if (containsKey(after)) {return false;}
		int index = ordering.indexOf(before);
		if (index == -1) {return false;}
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
	public boolean alterAt (int index, Enum<?> after) {
		if (index < 0 || index >= size || containsKey(after)) {return false;}
		super.put(after, super.remove(ordering.get(index)));
		ordering.set(index, after);
		return true;
	}

	/**
	 * Changes the value at a specified {@code index} in the iteration order to {@code v}, without changing keys at all.
	 * If {@code index} isn't currently a valid index in the iteration order, this returns null. Otherwise, it returns the
	 * value that was previously held at {@code index}, which may also be null.
	 *
	 * @param v     the new V value to assign
	 * @param index the index in the iteration order to set {@code v} at
	 * @return the previous value held at {@code index} in the iteration order, which may be null if the value was null or if {@code index} was invalid
	 */
	@Nullable
	public V setAt (int index, V v) {
		if (index < 0 || index >= size || universe == null || valueTable == null) {return null;}
		int pos = ordering.get(index).ordinal();
		final Object oldValue = valueTable[pos];
		valueTable[pos] = v;
		if(oldValue == null) return null;
		return release(oldValue);
	}

	/**
	 * Gets the V value at the given {@code index} in the insertion order. The index should be between 0
	 * (inclusive) and {@link #size()} (exclusive).
	 *
	 * @param index an index in the insertion order, between 0 (inclusive) and {@link #size()} (exclusive)
	 * @return the value at the given index
	 */
	@Nullable
	public V getAt (int index) {
		return get(ordering.get(index));
	}

	/**
	 * Gets the Enum key at the given {@code index} in the insertion order. The index should be between 0
	 * (inclusive) and {@link #size()} (exclusive).
	 *
	 * @param index an index in the insertion order, between 0 (inclusive) and {@link #size()} (exclusive)
	 * @return the key at the given index
	 */
	public Enum<?> keyAt (int index) {
		return ordering.get(index);
	}


	/**
	 * Removes all the elements from this map.
	 * The map will be empty after this call returns.
	 * This does not change the universe of possible Enum items this can hold.
	 */
	@Override
	public void clear () {
		super.clear();
		ordering.clear();
	}

	/**
	 * Removes all the elements from this map and can reset the universe of possible Enum items this can hold.
	 * The map will be empty after this call returns.
	 * This changes the universe of possible Enum items this can hold to match {@code universe}.
	 * If {@code universe} is null, this resets this map to the state it would have after {@link #EnumOrderedMap()} was called.
	 * If the table this would need is the same size as or smaller than the current table (such as if {@code universe} is the same as
	 * the universe here), this will not allocate, but will still clear any items this holds and will set the universe to the given one.
	 * Otherwise, this allocates and uses a new table of a larger size, with nothing in it, and uses the given universe.
	 * This always uses {@code universe} directly, without copying.
	 * <br>
	 * This can be useful to allow an EnumOrderedMap that was created with {@link #EnumOrderedMap()} to share a universe with other EnumOrderedMaps.
	 *
	 * @param universe the universe of possible Enum items this can hold; almost always produced by {@code values()} on an Enum
	 */
	public void clearToUniverse (Enum<?>@Nullable [] universe) {
		super.clearToUniverse(universe);
		ordering.clear();
	}


	/**
	 * Removes all the elements from this map and can reset the universe of possible Enum items this can hold.
	 * The map will be empty after this call returns.
	 * This changes the universe of possible Enum items this can hold to match the Enum constants in {@code universe}.
	 * If {@code universe} is null, this resets this map to the state it would have after {@link #EnumOrderedMap()} was called.
	 * If the table this would need is the same size as or smaller than the current table (such as if {@code universe} is the same as
	 * the universe here), this will not allocate, but will still clear any items this holds and will set the universe to the given one.
	 * Otherwise, this allocates and uses a new table of a larger size, with nothing in it, and uses the given universe.
	 * This calls {@link Class#getEnumConstants()} if universe is non-null, which allocates a new array.
	 * <br>
	 * You may want to prefer calling {@link #clearToUniverse(Enum[])} (the overload that takes an array), because it can be used to
	 * share one universe array between many EnumOrderedMap instances. This overload, given a Class, has to call {@link Class#getEnumConstants()}
	 * and thus allocate a new array each time this is called.
	 *
	 * @param universe the Class of an Enum type that stores the universe of possible Enum items this can hold
	 */
	public void clearToUniverse (@Nullable Class<? extends Enum<?>> universe) {
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
	 * Sorts this EnumOrderedMap in-place by the keys' natural ordering.
	 */
	public void sort () {
		ordering.sort(null);
	}

	/**
	 * Sorts this EnumOrderedMap in-place by the given Comparator used on the keys. If {@code comp} is null, then this
	 * will sort by the natural ordering of the keys.
	 *
	 * @param comp a Comparator that can compare two {@code Enum} keys, or null to use the keys' natural ordering
	 */
	public void sort (@Nullable Comparator<? super Enum<?>> comp) {
		ordering.sort(comp);
	}

	/**
	 * Sorts this ObjectObjectOrderedMap in-place by the given Comparator used on the values. {@code comp} must not be null,
	 * and must be able to compare {@code V} values. If any null values are present in this EnumOrderedMap, then comp
	 * must be able to sort or otherwise handle null values. You can use {@link Comparator#naturalOrder()} to do
	 * what {@link #sort()} does (just sorting values in this case instead of keys) if the values implement
	 * {@link Comparable} (requiring all of them to be non-null).
	 *
	 * @param comp a non-null Comparator that can compare {@code V} values; if this contains null values, comp must handle them
	 */
	public void sortByValue (Comparator<V> comp) {
		ordering.sort((a, b) -> comp.compare(get(a), get(b)));
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
	 * Reduces the size of the map to the specified size. If the map is already smaller than the specified
	 * size, no action is taken.
	 *
	 * @param newSize the target size to try to reach by removing items, if smaller than the current size
	 */
	@Override
	public void truncate (int newSize) {
		if (size > newSize) {removeRange(newSize, size);}
	}

	/**
	 * Returns a {@link Set} view of the keys contained in this map.
	 * The set is backed by the map, so changes to the map are
	 * reflected in the set, and vice versa. If the map is modified
	 * while an iteration over the set is in progress (except through
	 * the iterator's own {@code remove} operation), the results of
	 * the iteration are undefined. The set supports element removal,
	 * which removes the corresponding mapping from the map, via the
	 * {@code Iterator.remove}, {@code Set.remove},
	 * {@code removeAll}, {@code retainAll}, and {@code clear}
	 * operations. It does not support the {@code add} or {@code addAll}
	 * operations.
	 *
	 * <p>Note that the same Collection instance is returned each time this
	 * method is called. Use the {@link OrderedMapKeys#OrderedMapKeys(EnumOrderedMap)}
	 * constructor for nested or multithreaded iteration.
	 *
	 * @return a set view of the keys contained in this map
	 */
	@Override
	public @NonNull Keys keySet () {
		if (keys1 == null || keys2 == null) {
			keys1 = new OrderedMapKeys(this);
			keys2 = new OrderedMapKeys(this);
		}
		if (!keys1.iter.valid) {
			keys1.iter.reset();
			keys1.iter.valid = true;
			keys2.iter.valid = false;
			return keys1;
		}
		keys2.iter.reset();
		keys2.iter.valid = true;
		keys1.iter.valid = false;
		return keys2;
	}

	/**
	 * Returns a Collection for the values in the map. Remove is supported by the Collection's iterator.
	 * <p>Note that the same Collection instance is returned each time this method is called. Use the
	 * {@link OrderedMapValues#OrderedMapValues(EnumOrderedMap)} constructor for nested or multithreaded iteration.
	 *
	 * @return a {@link Collection} of V values
	 */
	@Override
	public @NonNull Values<V> values () {
		if (values1 == null || values2 == null) {
			values1 = new OrderedMapValues<>(this);
			values2 = new OrderedMapValues<>(this);
		}
		if (!values1.iter.valid) {
			values1.iter.reset();
			values1.iter.valid = true;
			values2.iter.valid = false;
			return values1;
		}
		values2.iter.reset();
		values2.iter.valid = true;
		values1.iter.valid = false;
		return values2;
	}

	/**
	 * Returns a Set of Map.Entry, containing the entries in the map. Remove is supported by the Set's iterator.
	 *
	 * <p>Note that the same iterator instance is returned each time this method is called.
	 * Use the {@link OrderedMapEntries#OrderedMapEntries(EnumOrderedMap)} constructor for nested or multithreaded iteration.
	 *
	 * @return a {@link Set} of {@link Map.Entry} key-value pairs
	 */
	@Override
	public @NonNull Entries<V> entrySet () {
		if (entries1 == null || entries2 == null) {
			entries1 = new OrderedMapEntries<>(this);
			entries2 = new OrderedMapEntries<>(this);
		}
		if (!entries1.iter.valid) {
			entries1.iter.reset();
			entries1.iter.valid = true;
			entries2.iter.valid = false;
			return entries1;
		}
		entries2.iter.reset();
		entries2.iter.valid = true;
		entries1.iter.valid = false;
		return entries2;
	}

	/**
	 * Reuses the iterator of the reused {@link Entries}
	 * produced by {@link #entrySet()}; does not permit nested iteration. Iterate over
	 * {@link OrderedMapEntries#OrderedMapEntries(EnumOrderedMap)} if you need nested or
	 * multithreaded iteration. You can remove an Entry from this EnumOrderedMap
	 * using this Iterator.
	 *
	 * @return an {@link Iterator} over key-value pairs as {@link Map.Entry} values
	 */
	@Override
	public @NonNull MapIterator<V, Map.Entry<Enum<?>, V>> iterator () {
		return entrySet().iterator();
	}

	/**
	 * Appends to a StringBuilder from the contents of this EnumOrderedMap, but uses the given {@link Appender} and
	 * {@link Appender} to convert each key and each value to a customizable representation and append them
	 * to a StringBuilder. To use
	 * the default String representation, you can use {@code StringBuilder::append} as an appender.
	 *
	 * @param sb                a StringBuilder that this can append to
	 * @param entrySeparator    how to separate entries, such as {@code ", "}
	 * @param keyValueSeparator how to separate each key from its value, such as {@code "="} or {@code ":"}
	 * @param braces            true to wrap the output in curly braces, or false to omit them
	 * @param keyAppender       a function that takes a StringBuilder and an Enum, and returns the modified StringBuilder
	 * @param valueAppender     a function that takes a StringBuilder and a V, and returns the modified StringBuilder
	 * @return {@code sb}, with the appended keys and values of this map
	 */
	@Override
	public StringBuilder appendTo (StringBuilder sb, String entrySeparator, String keyValueSeparator, boolean braces, Appender<Enum<?>> keyAppender, Appender<V> valueAppender) {
		if (size == 0) {return braces ? sb.append("{}") : sb;}
		if (braces) {sb.append('{');}
		ObjectList<Enum<?>> keys = this.ordering;
		for (int i = 0, n = keys.size(); i < n; i++) {
			Enum<?> key = keys.get(i);
			if (i > 0) {sb.append(entrySeparator);}
			keyAppender.apply(sb, key);
			sb.append(keyValueSeparator);
			V value = get(key);
			if(value == this)
				sb.append("(this)");
			else
				valueAppender.apply(sb, value);

		}
		if (braces) {sb.append('}');}
		return sb;
	}

	public static class OrderedMapEntries<V> extends Entries<V> {
		protected ObjectList<Enum<?>> keys;

		public OrderedMapEntries (EnumOrderedMap<V> map) {
			super(map);
			keys = map.ordering;
			iter = new MapIterator<V, Map.Entry<Enum<?>, V>>(map) {
				@Override
				public @NonNull MapIterator<V, Map.Entry<Enum<?>, V>> iterator () {
					return this;
				}

				@Override
				public void reset () {
					currentIndex = -1;
					nextIndex = 0;
					hasNext = map.size > 0;
				}

				@Override
				public boolean hasNext () {
					if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
					return hasNext;
				}

				@Override
				public Entry<V> next () {
					if (!hasNext) {throw new NoSuchElementException();}
					if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
					currentIndex = nextIndex;
					entry.key = keys.get(nextIndex);
					entry.value = map.get(entry.key);
					nextIndex++;
					hasNext = nextIndex < map.size;
					return entry;
				}

				@Override
				public void remove () {
					if (currentIndex < 0) {throw new IllegalStateException("next must be called before remove.");}
					assert entry.key != null;
					map.remove(entry.key);
					nextIndex--;
					currentIndex = -1;
				}
			};
		}

	}

	public static class OrderedMapKeys extends Keys {
		private final ObjectList<Enum<?>> ordering;

		public OrderedMapKeys (EnumOrderedMap<?> map) {
			super(map);
			ordering = map.ordering;
			iter = new MapIterator<Object, Enum<?>>(map) {
				@Override
				public @NonNull MapIterator<?, Enum<?>> iterator () {
					return this;
				}

				@Override
				public boolean hasNext () {
					if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
					return hasNext;
				}

				@Override
				public void reset () {
					currentIndex = -1;
					nextIndex = 0;
					hasNext = map.size > 0;
				}

				@Override
				public Enum<?> next () {
					if (!hasNext) {throw new NoSuchElementException();}
					if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
					Enum<?> key = ordering.get(nextIndex);
					currentIndex = nextIndex;
					nextIndex++;
					hasNext = nextIndex < map.size;
					return key;
				}

				@Override
				public void remove () {
					if (currentIndex < 0) {throw new IllegalStateException("next must be called before remove.");}
					map.remove(ordering.get(currentIndex));
					nextIndex = currentIndex;
					currentIndex = -1;
				}
			};
		}

	}

	public static class OrderedMapValues<V> extends Values<V> {
		private final ObjectList<Enum<?>> keys;

		public OrderedMapValues (EnumOrderedMap<V> map) {
			super(map);
			keys = map.ordering;
			iter = new MapIterator<V, V>(map) {
				@Override
				public @NonNull MapIterator<V, V> iterator () {
					return this;
				}

				@Override
				public boolean hasNext () {
					if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
					return hasNext;
				}

				@Override
				public void reset () {
					currentIndex = -1;
					nextIndex = 0;
					hasNext = map.size > 0;
				}

				@Override
				@Nullable
				public V next () {
					if (!hasNext) {throw new NoSuchElementException();}
					if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
					V value = map.get(keys.get(nextIndex));
					currentIndex = nextIndex;
					nextIndex++;
					hasNext = nextIndex < map.size;
					return value;
				}

				@Override
				public void remove () {
					if (currentIndex < 0) {throw new IllegalStateException("next must be called before remove.");}
					map.remove(keys.get(currentIndex));
					nextIndex = currentIndex;
					currentIndex = -1;
				}
			};
		}

	}

	/**
	 * Constructs an empty map given the value type as a generic type argument.
	 * This is usually less useful than just using the constructor, but can be handy
	 * in some code-generation scenarios when you don't know how many arguments you will have.
	 *
	 * @param <V>    the type of values
	 * @return a new map containing nothing
	 */
	public static <V> EnumOrderedMap<V> with () {
		return new EnumOrderedMap<>();
	}

	/**
	 * Constructs a single-entry map given one key and one value.
	 * This is mostly useful as an optimization for {@link #with(Enum, Object, Object...)}
	 * when there's no "rest" of the keys or values.
	 *
	 * @param key0   the first and only key
	 * @param value0 the first and only value
	 * @param <V>    the type of value0
	 * @return a new map containing just the entry mapping key0 to value0
	 */
	public static <V> EnumOrderedMap<V> with (Enum<?> key0, V value0) {
		EnumOrderedMap<V> map = new EnumOrderedMap<>();
		map.put(key0, value0);
		return map;
	}

	/**
	 * Constructs a single-entry map given two key-value pairs.
	 * This is mostly useful as an optimization for {@link #with(Enum, Object, Object...)}
	 * when there's no "rest" of the keys or values.
	 *
	 * @param key0   an Enum key
	 * @param value0 a V value
	 * @param key1   an Enum key
	 * @param value1 a V value
	 * @param <V>    the type of value0
	 * @return a new map containing entries mapping each key to the following value
	 */
	public static <V> EnumOrderedMap<V> with (Enum<?> key0, V value0, Enum<?> key1, V value1) {
		EnumOrderedMap<V> map = new EnumOrderedMap<>();
		map.put(key0, value0);
		map.put(key1, value1);
		return map;
	}

	/**
	 * Constructs a single-entry map given three key-value pairs.
	 * This is mostly useful as an optimization for {@link #with(Enum, Object, Object...)}
	 * when there's no "rest" of the keys or values.
	 *
	 * @param key0   an Enum key
	 * @param value0 a V value
	 * @param key1   an Enum key
	 * @param value1 a V value
	 * @param key2   an Enum key
	 * @param value2 a V value
	 * @param <V>    the type of value0
	 * @return a new map containing entries mapping each key to the following value
	 */
	public static <V> EnumOrderedMap<V> with (Enum<?> key0, V value0, Enum<?> key1, V value1, Enum<?> key2, V value2) {
		EnumOrderedMap<V> map = new EnumOrderedMap<>();
		map.put(key0, value0);
		map.put(key1, value1);
		map.put(key2, value2);
		return map;
	}

	/**
	 * Constructs a single-entry map given four key-value pairs.
	 * This is mostly useful as an optimization for {@link #with(Enum, Object, Object...)}
	 * when there's no "rest" of the keys or values.
	 *
	 * @param key0   an Enum key
	 * @param value0 a V value
	 * @param key1   an Enum key
	 * @param value1 a V value
	 * @param key2   an Enum key
	 * @param value2 a V value
	 * @param key3   an Enum key
	 * @param value3 a V value
	 * @param <V>    the type of value0
	 * @return a new map containing entries mapping each key to the following value
	 */
	public static <V> EnumOrderedMap<V> with (Enum<?> key0, V value0, Enum<?> key1, V value1, Enum<?> key2, V value2, Enum<?> key3, V value3) {
		EnumOrderedMap<V> map = new EnumOrderedMap<>();
		map.put(key0, value0);
		map.put(key1, value1);
		map.put(key2, value2);
		map.put(key3, value3);
		return map;
	}

	/**
	 * Constructs a map given alternating keys and values.
	 * This can be useful in some code-generation scenarios, or when you want to make a
	 * map conveniently by-hand and have it populated at the start. You can also use
	 * {@link #EnumOrderedMap(Enum[], Object[])}, which takes all keys and then all values.
	 * This needs all keys to have the same type and all values to have the same type, because
	 * it gets those types from the first key parameter and first value parameter. Any keys that don't
	 * have Enum as their type or values that don't have V as their type have that entry skipped.
	 *
	 * @param key0   the first key
	 * @param value0 the first value; will be used to determine the type of all values
	 * @param rest   an array or varargs of alternating Enum, V, Enum, V... elements
	 * @param <V>    the type of values, inferred from value0
	 * @return a new map containing the given keys and values
	 */
	@SuppressWarnings("unchecked")
	public static <V> EnumOrderedMap<V> with (Enum<?> key0, V value0, Object... rest) {
		EnumOrderedMap<V> map = new EnumOrderedMap<>();
		map.put(key0, value0);
		for (int i = 1; i < rest.length; i += 2) {
			try {
				map.put((Enum<?>)rest[i - 1], (V)rest[i]);
			} catch (ClassCastException ignored) {
			}
		}
		return map;
	}

	/**
	 * Constructs an empty map that can store keys from the given universe, using the
	 * specified generic type for values.
	 * The universe is usually obtained from an Enum type's {@code values()} method,
	 * and is often shared between Enum-keyed maps and sets.
	 *
	 * @param universe a key universe, as an array of Enum constants typically obtained via {@code values()}
	 * @param <V>      the type of values
	 * @return a new map containing nothing
	 */
	public static <V> EnumOrderedMap<V> noneOf (Enum<?>[] universe) {
		return new EnumOrderedMap<>(universe);
	}
}

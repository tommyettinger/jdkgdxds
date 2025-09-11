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
import com.github.tommyettinger.ds.support.util.PartialParser;
import com.github.tommyettinger.function.ObjObjToObjBiFunction;
import com.github.tommyettinger.function.ObjToObjFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

import static com.github.tommyettinger.ds.Utilities.neverIdentical;

/**
 * An unordered map where the keys are {@code Enum}s and values are objects. Null keys are not allowed; null values are permitted.
 * Unlike {@link java.util.EnumMap}, this does not require a Class at construction time, which can be useful for serialization
 * purposes. No allocation is done unless this is changing its table size and/or key universe.
 * <br>
 * This class never actually hashes keys in its primary operations (get(), put(), remove(), containsKey(), etc.), since it can
 * rely on keys having an Enum type, and so having {@link Enum#ordinal()} available. The ordinal allows constant-time access
 * to a guaranteed-unique {@code int} that will always be non-negative and less than the size of the key universe. The table of
 * possible values always starts sized to fit exactly as many values as there are keys in the key universe.
 * <br>
 * The key universe is an important concept here; it is simply an array of all possible Enum values the EnumMap can use as keys, in
 * the specific order they are declared. You almost always get a key universe by calling {@code MyEnum.values()}, but you
 * can also use {@link Class#getEnumConstants()} for an Enum class. You can and generally should reuse key universes in order to
 * avoid allocations and/or save memory; the constructor {@link #EnumMap(Enum[])} (with no values given) creates an empty EnumMap with
 * a given key universe. If you need to use the zero-argument constructor, you can, and the key universe will be obtained from the
 * first key placed into the EnumMap. You can also set the key universe with {@link #clearToUniverse(Enum[])}, in the process of
 * clearing the map.
 * <br>
 * This class tries to be as compatible as possible with {@link java.util.EnumMap}, though this expands on that where possible.
 *
 * @author Nathan Sweet (Keys, Values, Entries, and MapIterator, as well as general structure)
 * @author Tommy Ettinger (Enum-related adaptation)
 */
public class EnumMap<V> implements Map<Enum<?>, V>, Iterable<Map.Entry<Enum<?>, V>> {

	protected int size;

	protected Enum<?> @Nullable [] universe = null;

	protected @Nullable Object @Nullable [] valueTable = null;

	@Nullable
	protected transient Entries<V> entries1;
	@Nullable
	protected transient Entries<V> entries2;
	@Nullable
	protected transient Values<V> values1;
	@Nullable
	protected transient Values<V> values2;
	@Nullable
	protected transient Keys keys1;
	@Nullable
	protected transient Keys keys2;

	/**
	 * Returned by {@link #get(Object)} when no value exists for the given key, as well as some other methods to indicate that
	 * no value in the Map could be returned. Defaults to {@code null}.
	 */
	@Nullable
	public V defaultValue = null;

	/**
	 * Empty constructor; using this will postpone creating the key universe and allocating the value table until {@link #put} is
	 * first called (potentially indirectly). You can also use {@link #clearToUniverse} to set the key universe and value table.
	 */
	public EnumMap() {
	}

	/**
	 * Initializes this map so that it has exactly enough capacity as needed to contain each Enum constant defined in
	 * {@code universe}, assuming universe stores every possible constant in one Enum type. This map will start empty.
	 * You almost always obtain universe from calling {@code values()} on an Enum type, and you can share one
	 * reference to one Enum array across many EnumMap instances if you don't modify the shared array. Sharing the same
	 * universe helps save some memory if you have (very) many EnumMap instances.
	 *
	 * @param universe almost always, the result of calling {@code values()} on an Enum type; used directly, not copied
	 */
	public EnumMap(Enum<?> @Nullable [] universe) {
		super();
		if (universe == null) return;
		this.universe = universe;
		valueTable = new Object[universe.length];
	}

	/**
	 * Initializes this map so that it has exactly enough capacity as needed to contain each Enum constant defined by the
	 * Class {@code universeClass}, assuming universeClass is non-null. This simply calls {@link #EnumMap(Enum[])}
	 * for convenience. Note that this constructor allocates a new array of Enum constants each time it is called, where
	 * if you use {@link #EnumMap(Enum[])}, you can reuse an unmodified array to reduce allocations.
	 *
	 * @param universeClass the Class of an Enum type that defines the universe of valid Enum items this can hold
	 */
	public EnumMap(@Nullable Class<? extends Enum<?>> universeClass) {
		this(universeClass == null ? null : universeClass.getEnumConstants());
	}

	/**
	 * Creates a new map identical to the specified EnumMap. This will share a key universe with the given EnumMap, if non-null.
	 *
	 * @param map an EnumMap to copy
	 */
	public EnumMap(EnumMap<? extends V> map) {
		universe = map.universe;
		if (map.valueTable != null)
			valueTable = Arrays.copyOf(map.valueTable, map.valueTable.length);
		size = map.size;
		defaultValue = map.defaultValue;
	}

	/**
	 * Creates a new map identical to the specified map.
	 *
	 * @param map a Map to copy; EnumMap or its subclasses will be faster
	 */
	public EnumMap(Map<? extends Enum<?>, ? extends V> map) {
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
	public EnumMap(Enum<?>[] keys, V[] values) {
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
	public EnumMap(Collection<? extends Enum<?>> keys, Collection<? extends V> values) {
		this();
		putAll(keys, values);
	}

	/**
	 * If the given Object is {@code null}, this replaces it with a placeholder value ({@link Utilities#neverIdentical});
	 * otherwise, it returns the given Object as-is.
	 *
	 * @param o any Object; will be returned as-is unless it is null
	 * @return the given Object or {@link Utilities#neverIdentical}
	 */
	protected Object hold(@Nullable Object o) {
		return o == null ? neverIdentical : o;
	}

	/**
	 * If the given Object is {@link Utilities#neverIdentical}, this "releases its hold" on that placeholder value and returns
	 * null; otherwise, it returns the given Object (cast to V if non-null).
	 *
	 * @param o any Object, but should be the placeholder {@link Utilities#neverIdentical} or a V instance
	 * @return the V passed in, or null if it is the placeholder
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	protected V release(@Nullable Object o) {
		if (o == neverIdentical || o == null)
			return null;
		return (V) o;
	}

	/**
	 * Returns the old value associated with the specified key, or this map's {@link #defaultValue} if there was no prior value.
	 * If this EnumMap does not yet have a key universe and/or value table, this gets the key universe from {@code key} and uses it
	 * from now on for this EnumMap.
	 *
	 * @param key   the Enum key to try to place into this EnumMap
	 * @param value the V value to associate with {@code key}
	 * @return the previous value associated with {@code key}, or {@link #getDefaultValue()} if the given key was not present
	 */
	@Override
	@Nullable
	public V put(Enum<?> key, @Nullable V value) {
		if (key == null) return defaultValue;
		if (universe == null) universe = key.getDeclaringClass().getEnumConstants();
		if (valueTable == null) valueTable = new Object[universe.length];
		int i = key.ordinal();
		if (i >= valueTable.length || universe[i] != key)
			throw new ClassCastException("Incompatible key for the EnumMap's universe.");
		Object oldValue = valueTable[i];
		valueTable[i] = hold(value);
		if (oldValue != null) {
			// Existing key was found.
			return release(oldValue);
		}
		++size;
		return defaultValue;
	}

	/**
	 * Acts like {@link #put(Enum, Object)}, but uses the specified {@code defaultValue} instead of
	 * {@link #getDefaultValue() the default value for this EnumMap}.
	 *
	 * @param key          the Enum key to try to place into this EnumMap
	 * @param value        the V value to associate with {@code key}
	 * @param defaultValue the V value to return if {@code key} was not already present
	 * @return the previous value associated with {@code key}, or the given {@code defaultValue} if the given key was not present
	 */
	@Nullable
	public V putOrDefault(Enum<?> key, @Nullable V value, @Nullable V defaultValue) {
		if (key == null) return defaultValue;
		if (universe == null) universe = key.getDeclaringClass().getEnumConstants();
		if (valueTable == null) valueTable = new Object[universe.length];
		int i = key.ordinal();
		if (i >= valueTable.length || universe[i] != key)
			throw new ClassCastException("Incompatible key for the EnumMap's universe.");
		Object oldValue = valueTable[i];
		valueTable[i] = hold(value);
		if (oldValue != null) {
			// Existing key was found.
			return release(oldValue);
		}
		++size;
		return defaultValue;
	}

	/**
	 * Puts every key-value pair in the given map into this, with the values from the given map
	 * overwriting the previous values if two keys are identical. If this EnumMap doesn't yet have
	 * a key universe, it will now share a key universe with the given {@code map}. Even if the
	 * given EnumMap is empty, it can still be used to obtain a key universe for this EnumMap
	 * (assuming it has a key universe).
	 *
	 * @param map a map with compatible key and value types; will not be modified
	 */
	public void putAll(@NotNull EnumMap<? extends V> map) {
		if (map.universe == null) return;
		if (universe == null) universe = map.universe;
		if (valueTable == null) valueTable = new Object[universe.length];
		if (map.size == 0) return;
		final int n = map.valueTable.length;
		if (this.valueTable.length != n) return;
		Object[] valueTable = map.valueTable;
		Object value;
		for (int i = 0; i < n; i++) {
			if (universe[i] != map.universe[i])
				throw new ClassCastException("Incompatible key for the EnumMap's universe.");
			value = valueTable[i];
			if (value != null) {
				if (this.valueTable[i] == null) ++size;
				this.valueTable[i] = value;
			}
		}
	}

	/**
	 * Given two side-by-side arrays, one of keys, one of values, this inserts each pair of key and value into this map with put().
	 * Delegates to {@link #putAll(Enum[], int, Object[], int, int)}.
	 *
	 * @param keys   an array of keys
	 * @param values an array of values
	 */
	public void putAll(Enum<?>[] keys, V[] values) {
		putAll(keys, 0, values, 0, Math.min(keys.length, values.length));
	}

	/**
	 * Given two side-by-side arrays, one of keys, one of values, this inserts each pair of key and value into this map with put().
	 * Delegates to {@link #putAll(Enum[], int, Object[], int, int)}.
	 *
	 * @param keys   an array of keys
	 * @param values an array of values
	 * @param length how many items from keys and values to insert, at-most
	 */
	public void putAll(Enum<?>[] keys, V[] values, int length) {
		putAll(keys, 0, values, 0, Math.min(length, Math.min(keys.length, values.length)));
	}

	/**
	 * Given two side-by-side arrays, one of keys, one of values, this inserts each pair of key and value into this map with
	 * {@link #put(Enum, Object)}.
	 *
	 * @param keys        an array of keys
	 * @param keyOffset   the first index in keys to insert
	 * @param values      an array of values
	 * @param valueOffset the first index in values to insert
	 * @param length      how many items from keys and values to insert, at-most
	 */
	public void putAll(Enum<?>[] keys, int keyOffset, V[] values, int valueOffset, int length) {
		length = Math.min(length, Math.min(keys.length - keyOffset, values.length - valueOffset));
		Enum<?> key;
		for (int k = keyOffset, v = valueOffset, i = 0, n = length; i < n; i++, k++, v++) {
			key = keys[k];
			if (key != null) {
				put(key, values[v]);
			}
		}
	}

	/**
	 * Given two side-by-side collections, one of Enum keys, one of V values, this inserts each pair of key and
	 * value into this map with put().
	 *
	 * @param keys   a Collection of Enum keys
	 * @param values a Collection of V values
	 */
	public void putAll(Collection<? extends Enum<?>> keys, Collection<? extends V> values) {
		Enum<?> key;
		Iterator<? extends Enum<?>> ki = keys.iterator();
		Iterator<? extends V> vi = values.iterator();
		while (ki.hasNext() && vi.hasNext()) {
			key = ki.next();
			if (key != null) {
				put(key, vi.next());
			}
		}
	}

	/**
	 * Returns the value for the specified key, or {@link #defaultValue} if the key is not in the map.
	 * Note that {@link #defaultValue} is often null, which is also a valid value that can be assigned to a
	 * legitimate key. Checking that the result of this method is null does not guarantee that the
	 * {@code key} is not present.
	 *
	 * @param key a non-null Object that should almost always be a {@code K} (or an instance of a subclass of {@code K})
	 */
	@Override
	@Nullable
	public V get(Object key) {
		if (size == 0 || !(key instanceof Enum<?>))
			return defaultValue;
		final Enum<?> e = (Enum<?>) key;
		final int ord = e.ordinal();
		if (ord >= universe.length || universe[ord] != e)
			return defaultValue;
		Object o = valueTable[ord];
		return o == null ? defaultValue : release(o);
	}

	/**
	 * Returns the value for the specified key, or the given default value if the key is not in the map.
	 */
	@Override
	@Nullable
	public V getOrDefault(Object key, @Nullable V defaultValue) {
		if (size == 0 || valueTable == null || !(key instanceof Enum<?>))
			return defaultValue;
		Enum<?> e = (Enum<?>) key;
		Object o = valueTable[e.ordinal()];
		return o == null ? defaultValue : release(o);
	}

	@Override
	@Nullable
	public V remove(Object key) {
		if (size == 0 || !(key instanceof Enum<?>))
			return defaultValue;
		Enum<?> e = (Enum<?>) key;
		Object o = valueTable[e.ordinal()];
		valueTable[e.ordinal()] = null;
		if (o == null) return defaultValue;
		--size;
		return release(o);
	}

	/**
	 * Copies all the mappings from the specified map to this map
	 * (optional operation).  The effect of this call is equivalent to that
	 * of calling {@link #put(Enum, Object) put(k, v)} on this map once
	 * for each mapping from key {@code k} to value {@code v} in the
	 * specified map.  The behavior of this operation is undefined if the
	 * specified map is modified while the operation is in progress.
	 * <br>
	 * Note that {@link #putAll(EnumMap)} is more specific and can be
	 * more efficient by using the internal details of this class.
	 *
	 * @param m mappings to be stored in this map
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
	@Override
	public void putAll(Map<? extends Enum<?>, ? extends V> m) {
		for (Map.Entry<? extends Enum<?>, ? extends V> kv : m.entrySet()) {
			put(kv.getKey(), kv.getValue());
		}
	}

	/**
	 * Returns true if the map has one or more items.
	 */
	public boolean notEmpty() {
		return size != 0;
	}

	/**
	 * Returns the number of key-value mappings in this map.  If the
	 * map contains more than {@code Integer.MAX_VALUE} elements, returns
	 * {@code Integer.MAX_VALUE}.
	 *
	 * @return the number of key-value mappings in this map
	 */
	@Override
	public int size() {
		return size;
	}

	/**
	 * Returns true if the map is empty.
	 */
	@Override
	public boolean isEmpty() {
		return size == 0;
	}

	/**
	 * Gets the default value, a {@code V} which is returned by {@link #get(Object)} if the key is not found.
	 * If not changed, the default value is null.
	 *
	 * @return the current default value
	 */
	@Nullable
	public V getDefaultValue() {
		return defaultValue;
	}

	/**
	 * Sets the default value, a {@code V} which is returned by {@link #get(Object)} if the key is not found.
	 * If not changed, the default value is null. Note that {@link #getOrDefault(Object, Object)} is also available,
	 * which allows specifying a "not-found" value per-call.
	 *
	 * @param defaultValue may be any V object or null; should usually be one that doesn't occur as a typical value
	 */
	public void setDefaultValue(@Nullable V defaultValue) {
		this.defaultValue = defaultValue;
	}

	/**
	 * Removes all the elements from this map.
	 * The map will be empty after this call returns.
	 * This does not change the universe of possible Enum items this can hold.
	 */
	@Override
	public void clear() {
		size = 0;
		if (valueTable != null)
			Utilities.clear(valueTable);
	}

	/**
	 * Removes all the elements from this map and can reset the universe of possible Enum items this can hold.
	 * The map will be empty after this call returns.
	 * This changes the universe of possible Enum items this can hold to match {@code universe}.
	 * If {@code universe} is null, this resets this map to the state it would have after {@link #EnumMap()} was called.
	 * If the table this would need is the same size as or smaller than the current table (such as if {@code universe} is the same as
	 * the universe here), this will not allocate, but will still clear any items this holds and will set the universe to the given one.
	 * Otherwise, this allocates and uses a new table of a larger size, with nothing in it, and uses the given universe.
	 * This always uses {@code universe} directly, without copying.
	 * <br>
	 * This can be useful to allow an EnumMap that was created with {@link #EnumMap()} to share a universe with other EnumMaps.
	 *
	 * @param universe the universe of possible Enum items this can hold; almost always produced by {@code values()} on an Enum
	 */
	public void clearToUniverse(Enum<?> @Nullable [] universe) {
		size = 0;
		if (universe == null) {
			valueTable = null;
		} else if (universe.length <= this.universe.length) {
			if (valueTable != null)
				Utilities.clear(valueTable);
		} else {
			valueTable = new Object[universe.length];
		}
		this.universe = universe;
	}


	/**
	 * Removes all the elements from this map and can reset the universe of possible Enum items this can hold.
	 * The map will be empty after this call returns.
	 * This changes the universe of possible Enum items this can hold to match the Enum constants in {@code universe}.
	 * If {@code universe} is null, this resets this map to the state it would have after {@link #EnumMap()} was called.
	 * If the table this would need is the same size as or smaller than the current table (such as if {@code universe} is the same as
	 * the universe here), this will not allocate, but will still clear any items this holds and will set the universe to the given one.
	 * Otherwise, this allocates and uses a new table of a larger size, with nothing in it, and uses the given universe.
	 * This calls {@link Class#getEnumConstants()} if universe is non-null, which allocates a new array.
	 * <br>
	 * You may want to prefer calling {@link #clearToUniverse(Enum[])} (the overload that takes an array), because it can be used to
	 * share one universe array between many EnumMap instances. This overload, given a Class, has to call {@link Class#getEnumConstants()}
	 * and thus allocate a new array each time this is called.
	 *
	 * @param universe the Class of an Enum type that stores the universe of possible Enum items this can hold
	 */
	public void clearToUniverse(@Nullable Class<? extends Enum<?>> universe) {
		size = 0;
		if (universe == null) {
			valueTable = null;
			this.universe = null;
		} else {
			Enum<?>[] cons = universe.getEnumConstants();
			if (this.universe != null && cons.length <= this.universe.length) {
				if (valueTable != null)
					Utilities.clear(valueTable);
			} else {
				valueTable = new Object[cons.length];
			}
			this.universe = cons;
		}
	}

	/**
	 * Gets the current key universe; this is a technically-mutable array, but should never be modified.
	 * To set the universe on an existing EnumMap (with existing contents), you can use {@link #clearToUniverse(Enum[])}.
	 * If an EnumMap has not been initialized, just adding a key will set the key universe to match the given item.
	 *
	 * @return the current key universe
	 */
	public Enum<?> @Nullable [] getUniverse() {
		return universe;
	}

	/**
	 * Reduces the size of the map to the specified size. If the map is already smaller than the specified
	 * size, no action is taken. This indiscriminately removes items from the backing array until the
	 * requested newSize is reached, or until the full backing array has had its elements removed.
	 * <br>
	 * This tries to remove from the end of the iteration order, but because the iteration order is not
	 * guaranteed by an unordered map, this can remove essentially any item(s) from the map if it is larger
	 * than newSize.
	 *
	 * @param newSize the target size to try to reach by removing items, if smaller than the current size
	 */
	public void truncate(int newSize) {
		@Nullable Object[] table = this.valueTable;
		newSize = Math.max(0, newSize);
		for (int i = table.length - 1; i >= 0 && size > newSize; i--) {
			if (table[i] != null) {
				table[i] = null;
				--size;
			}
		}
	}

	/**
	 * Returns true if the specified value is in the map. Note this traverses the entire map and compares every value, which may
	 * be an expensive operation.
	 *
	 * @param identity If true, uses == to compare the specified value with values in the map. If false, uses
	 *                 {@link #equals(Object)}.
	 */
	public boolean containsValue(@Nullable Object value, boolean identity) {
		if (this.valueTable == null) return false;
		@Nullable Object @Nullable [] valueTable = this.valueTable;
		Object held = hold(value);
		if (identity) {
			for (int i = valueTable.length - 1; i >= 0; i--) {
				if (valueTable[i] == held) {
					return true;
				}
			}
		} else {
			for (int i = valueTable.length - 1; i >= 0; i--) {
				if (held.equals(valueTable[i])) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean containsKey(Object key) {
		if (size == 0 || universe == null || !(key instanceof Enum<?>))
			return false;
		final Enum<?> e = (Enum<?>) key;
		final int ord = e.ordinal();
		return ord < universe.length && universe[ord] == e && valueTable[ord] != null;
	}

	/**
	 * Returns {@code true} if this map maps one or more keys to the
	 * specified value.  More formally, returns {@code true} if and only if
	 * this map contains at least one mapping to a value {@code v} such that
	 * {@code (value==null ? v==null : value.equals(v))}.  This operation
	 * will probably require time linear in the map size for most
	 * implementations of the {@code Map} interface.
	 *
	 * @param value value whose presence in this map is to be tested
	 * @return {@code true} if this map maps one or more keys to the
	 * specified value
	 * @throws ClassCastException   if the value is of an inappropriate type for
	 *                              this map
	 *                              (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
	 * @throws NullPointerException if the specified value is null and this
	 *                              map does not permit null values
	 *                              (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
	 */
	@Override
	public boolean containsValue(Object value) {
		return containsValue(value, false);
	}

	/**
	 * Returns the key for the specified value, or null if it is not in the map. Note this traverses the entire map and compares
	 * every value, which may be an expensive operation.
	 *
	 * @param identity If true, uses == to compare the specified value with values in the map. If false, uses
	 *                 {@link #equals(Object)}.
	 * @return the corresponding Enum if the value was found, or null otherwise
	 */
	@Nullable
	public Enum<?> findKey(@Nullable Object value, boolean identity) {
		if (this.universe == null || this.valueTable == null) return null;
		@Nullable Object[] valueTable = this.valueTable;
		Object held = hold(value);
		if (identity) {
			for (int i = valueTable.length - 1; i >= 0; i--) {
				if (valueTable[i] == held) {
					return universe[i];
				}
			}
		} else {
			for (int i = valueTable.length - 1; i >= 0; i--) {
				if (held.equals(valueTable[i])) {
					return universe[i];
				}
			}
		}
		return null;
	}

	@Override
	public int hashCode() {
		int h = size;
		if (this.universe != null && this.valueTable != null) {
			Enum<?>[] universe = this.universe;
			@Nullable Object[] valueTable = this.valueTable;
			for (int i = 0, n = universe.length; i < n; i++) {
				Enum<?> key = universe[i];
				h ^= key.hashCode();
				V value = release(valueTable[i]);
				if (value != null) {
					h ^= value.hashCode();
				}
			}
		}
		return h;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof Map)) {
			return false;
		}
		Map other = (Map) obj;
		if (other.size() != size) {
			return false;
		}
		Enum<?> @Nullable [] universe = this.universe;
		@Nullable Object @Nullable [] valueTable = this.valueTable;
		if (universe == null || valueTable == null || size == 0) return other.isEmpty();
		try {
			for (int i = 0, n = universe.length; i < n; i++) {
				@Nullable Object rawValue = valueTable[i];
				if (rawValue != null) {
					V value = release(rawValue);
					if (value == null) {
						if (other.getOrDefault(universe[i], neverIdentical) != null) {
							return false;
						}
					} else {
						if (!value.equals(other.get(universe[i]))) {
							return false;
						}
					}
				}
			}
		} catch (ClassCastException | NullPointerException unused) {
			return false;
		}

		return true;
	}

	/**
	 * Uses == for comparison of each value.
	 */
	public boolean equalsIdentity(@Nullable Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof EnumMap)) {
			return false;
		}
		EnumMap other = (EnumMap) obj;
		if (other.size != size) {
			return false;
		}
		Enum<?>[] universe = this.universe;
		Object[] valueTable = this.valueTable;
		for (int i = 0, n = universe.length; i < n; i++) {
			Enum<?> key = universe[i];
			if (key != null && release(valueTable[i]) != other.getOrDefault(key, neverIdentical)) {
				return false;
			}
		}
		return true;
	}


	@Override
	public String toString() {
		return toString(", ", true);
	}

	/**
	 * Delegates to {@link #toString(String, boolean)} with the given entrySeparator and without braces.
	 * This is different from {@link #toString()}, which includes braces by default.
	 *
	 * @param entrySeparator how to separate entries, such as {@code ", "}
	 * @return a new String representing this map
	 */
	public String toString(String entrySeparator) {
		return toString(entrySeparator, false);
	}

	public String toString(String entrySeparator, boolean braces) {
		return appendTo(new StringBuilder(32), entrySeparator, braces).toString();
	}

	/**
	 * Makes a String from the contents of this EnumMap, but uses the given {@link Appender} and
	 * {@link Appender} to convert each key and each value to a customizable representation and append them
	 * to a temporary StringBuilder. To use
	 * the default toString representation, you can use {@code Appender::append} as an appender, or to use the readable
	 * Enum {@link Enum#name()}, use {@link Appender#ENUM_NAME_APPENDER}.
	 *
	 * @param entrySeparator    how to separate entries, such as {@code ", "}
	 * @param keyValueSeparator how to separate each key from its value, such as {@code "="} or {@code ":"}
	 * @param braces            true to wrap the output in curly braces, or false to omit them
	 * @param keyAppender       a function that takes a StringBuilder and an Enum, and returns the modified StringBuilder
	 * @param valueAppender     a function that takes a StringBuilder and a V, and returns the modified StringBuilder
	 * @return a new String representing this map
	 */
	public String toString(String entrySeparator, String keyValueSeparator, boolean braces,
						   Appender<Enum<?>> keyAppender, Appender<V> valueAppender) {
		return appendTo(new StringBuilder(), entrySeparator, keyValueSeparator, braces, keyAppender, valueAppender).toString();
	}

	public StringBuilder appendTo(StringBuilder sb, String entrySeparator, boolean braces) {
		return appendTo(sb, entrySeparator, "=", braces, Appender.ENUM_NAME_APPENDER, Appender::append);
	}

	/**
	 * Appends to a StringBuilder from the contents of this EnumMap, but uses the given {@link Appender} and
	 * {@link Appender} to convert each key and each value to a customizable representation and append them
	 * to a StringBuilder. To use
	 * the default String representation, you can use {@code Appender::append} as an appender.
	 *
	 * @param sb                a StringBuilder that this can append to
	 * @param entrySeparator    how to separate entries, such as {@code ", "}
	 * @param keyValueSeparator how to separate each key from its value, such as {@code "="} or {@code ":"}
	 * @param braces            true to wrap the output in curly braces, or false to omit them
	 * @param keyAppender       a function that takes a StringBuilder and an Enum, and returns the modified StringBuilder
	 * @param valueAppender     a function that takes a StringBuilder and a V, and returns the modified StringBuilder
	 * @return {@code sb}, with the appended keys and values of this map
	 */
	public StringBuilder appendTo(StringBuilder sb, String entrySeparator, String keyValueSeparator, boolean braces,
								  Appender<Enum<?>> keyAppender, Appender<V> valueAppender) {
		if (size == 0 || this.universe == null || this.valueTable == null) {
			return braces ? sb.append("{}") : sb;
		}
		if (braces) {
			sb.append('{');
		}
		Enum<?>[] universe = this.universe;
		@Nullable Object[] valueTable = this.valueTable;
		int i = -1;
		final int len = universe.length;
		while (++i < len) {
			@Nullable Object v = valueTable[i];
			if (v == null) {
				continue;
			}
			keyAppender.apply(sb, universe[i]);
			sb.append(keyValueSeparator);
			V value = release(v);
			if (value == this) sb.append("(this)");
			else valueAppender.apply(sb, value);
			break;
		}
		while (++i < len) {
			@Nullable Object v = valueTable[i];
			if (v == null) {
				continue;
			}
			sb.append(entrySeparator);
			keyAppender.apply(sb, universe[i]);
			sb.append(keyValueSeparator);
			V value = release(v);
			if (value == this) sb.append("(this)");
			else valueAppender.apply(sb, value);
		}
		if (braces) {
			sb.append('}');
		}
		return sb;
	}

	@Override
	@Nullable
	public V replace(Enum<?> key, V value) {
		if (key != null) {
			int i = key.ordinal();
			if (i < universe.length) {
				V oldValue = release(valueTable[i]);
				valueTable[i] = hold(value);
				return oldValue;
			}
		}
		return defaultValue;
	}

	/**
	 * Just like Map's merge() default method, but this doesn't use Java 8 APIs (so it should work on RoboVM), and this
	 * won't remove entries if the remappingFunction returns null (in that case, it will call {@code put(key, null)}).
	 * This also uses a functional interface from Funderby instead of the JDK, for RoboVM support.
	 *
	 * @param key               key with which the resulting value is to be associated
	 * @param value             the value to be merged with the existing value
	 *                          associated with the key or, if no existing value
	 *                          is associated with the key, to be associated with the key
	 * @param remappingFunction given a V from this and the V {@code value}, this should return what V to use
	 * @return the value now associated with key
	 */
	@Nullable
	public V combine(Enum<?> key, V value, ObjObjToObjBiFunction<? super V, ? super V, ? extends V> remappingFunction) {
		if (key == null) return defaultValue;
		int i = key.ordinal();
		V next = (valueTable[i] == null) ? value : remappingFunction.apply(release(valueTable[i]), value);
		put(key, next);
		return next;
	}

	/**
	 * Simply calls {@link #combine(Enum, Object, ObjObjToObjBiFunction)} on this map using every
	 * key-value pair in {@code other}. If {@code other} isn't empty, calling this will probably modify
	 * this map, though this depends on the {@code remappingFunction}.
	 *
	 * @param other             a non-null Map (or subclass) with compatible key and value types
	 * @param remappingFunction given a V value from this and a value from other, this should return what V to use
	 */
	public void combine(Map<? extends Enum<?>, ? extends V> other, ObjObjToObjBiFunction<? super V, ? super V, ? extends V> remappingFunction) {
		for (Map.Entry<? extends Enum<?>, ? extends V> e : other.entrySet()) {
			combine(e.getKey(), e.getValue(), remappingFunction);
		}
	}

	/**
	 * Reuses the iterator of the reused {@link Entries} produced by {@link #entrySet()};
	 * does not permit nested iteration. Iterate over {@link Entries#Entries(EnumMap)} if you
	 * need nested or multithreaded iteration. You can remove an Entry from this EnumMap
	 * using this Iterator.
	 *
	 * @return an {@link Iterator} over {@link Map.Entry} key-value pairs; remove is supported.
	 */
	@Override
	public @NotNull MapIterator<V, Map.Entry<Enum<?>, V>> iterator() {
		return entrySet().iterator();
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
	 * <p>Note that the same Collection instance is returned each time this
	 * method is called. Use the {@link Keys} constructor for nested or
	 * multithreaded iteration.
	 *
	 * @return a set view of the keys contained in this map
	 */
	@Override
	public @NotNull Keys keySet() {
		if (keys1 == null || keys2 == null) {
			keys1 = new Keys(this);
			keys2 = new Keys(this);
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
	 * Returns a Collection of the values in the map. Remove is supported. Note that the same Collection instance is returned each
	 * time this method is called. Use the {@link Values} constructor for nested or multithreaded iteration.
	 *
	 * @return a {@link Collection} of V values
	 */
	@Override
	public @NotNull Values<V> values() {
		if (values1 == null || values2 == null) {
			values1 = new Values<>(this);
			values2 = new Values<>(this);
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
	 * Note that the same iterator instance is returned each time this method is called.
	 * Use the {@link Entries} constructor for nested or multithreaded iteration.
	 *
	 * @return a {@link Set} of {@link Map.Entry} key-value pairs
	 */
	@Override
	public @NotNull Entries<V> entrySet() {
		if (entries1 == null || entries2 == null) {
			entries1 = new Entries<>(this);
			entries2 = new Entries<>(this);
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

	public static class Entry<V> implements Map.Entry<Enum<?>, V> {
		@Nullable
		public Enum<?> key;
		@Nullable
		public V value;

		public Entry() {
		}

		public Entry(@Nullable Enum<?> key, @Nullable V value) {
			this.key = key;
			this.value = value;
		}

		public Entry(Map.Entry<? extends Enum<?>, ? extends V> entry) {
			key = entry.getKey();
			value = entry.getValue();
		}

		@Override
		@Nullable
		public String toString() {
			return key + "=" + value;
		}

		/**
		 * Returns the key corresponding to this entry.
		 *
		 * @return the key corresponding to this entry
		 * @throws IllegalStateException implementations may, but are not
		 *                               required to, throw this exception if the entry has been
		 *                               removed from the backing map.
		 */
		@Override
		public Enum<?> getKey() {
			Objects.requireNonNull(key);
			return key;
		}

		/**
		 * Returns the value corresponding to this entry.  If the mapping
		 * has been removed from the backing map (by the iterator's
		 * {@code remove} operation), the results of this call are undefined.
		 *
		 * @return the value corresponding to this entry
		 * @throws IllegalStateException implementations may, but are not
		 *                               required to, throw this exception if the entry has been
		 *                               removed from the backing map.
		 */
		@Override
		@Nullable
		public V getValue() {
			return value;
		}

		/**
		 * Sets the value of this Entry, but does <em>not</em> write through to the containing EnumMap.
		 *
		 * @param value the new V value to use
		 * @return the old value this held, before modification
		 */
		@Override
		@Nullable
		public V setValue(V value) {
			V old = this.value;
			this.value = value;
			return old;
		}

		@Override
		public boolean equals(@Nullable Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof Map.Entry)) {
				return false;
			}

			Map.Entry entry = (Map.Entry) o;

			if (!Objects.equals(key, entry.getKey())) {
				return false;
			}
			return Objects.equals(value, entry.getValue());
		}

		@Override
		public int hashCode() {
			return (key != null ? key.hashCode() : 0) ^ (value != null ? value.hashCode() : 0);
		}
	}

	public static abstract class MapIterator<V, I> implements Iterable<I>, Iterator<I> {
		public boolean hasNext;

		protected final EnumMap<? extends V> map;
		protected int nextIndex, currentIndex;
		public boolean valid = true;

		public MapIterator(EnumMap<? extends V> map) {
			this.map = map;
			reset();
		}

		public void reset() {
			currentIndex = -1;
			nextIndex = -1;
			findNextIndex();
		}

		protected void findNextIndex() {
			Object[] valueTable = map.valueTable;
			if (valueTable != null) {
				for (int n = map.universe.length; ++nextIndex < n; ) {
					if (valueTable[nextIndex] != null) {
						hasNext = true;
						return;
					}
				}
			}
			hasNext = false;
		}

		@Override
		public void remove() {
			int i = currentIndex;
			if (i < 0) {
				throw new IllegalStateException("next must be called before remove.");
			}
			Object[] valueTable = map.valueTable;
			if (valueTable == null) return;
			// This condition can happen if the map had this the current item removed without using this method.
			if (valueTable[i] != null)
				map.size--;
			valueTable[i] = null;
			currentIndex = -1;
		}
	}

	public static class Entries<V> extends AbstractSet<Map.Entry<Enum<?>, V>> implements EnhancedCollection<Map.Entry<Enum<?>, V>> {
		protected Entry<V> entry = new Entry<>();
		protected MapIterator<V, Map.Entry<Enum<?>, V>> iter;

		public Entries(EnumMap<V> map) {
			iter = new MapIterator<V, Map.Entry<Enum<?>, V>>(map) {
				@Override
				public @NotNull MapIterator<V, Map.Entry<Enum<?>, V>> iterator() {
					return this;
				}

				/**
				 * Note: the same entry instance is returned each time this method is called.
				 *
				 * @return a reused Entry that will have its key and value set to the next pair
				 */
				@Override
				public Map.Entry<Enum<?>, V> next() {
					if (!hasNext) {
						throw new NoSuchElementException();
					}
					if (!valid) {
						throw new RuntimeException("#iterator() cannot be used nested.");
					}
					Enum<?>[] universe = map.universe;
					entry.key = universe[nextIndex];
					entry.value = map.release(map.valueTable[nextIndex]);
					currentIndex = nextIndex;
					findNextIndex();
					return entry;
				}

				@Override
				public boolean hasNext() {
					if (!valid) {
						throw new RuntimeException("#iterator() cannot be used nested.");
					}
					return hasNext;
				}
			};
		}

		@Override
		public boolean contains(Object o) {
			if (o instanceof Map.Entry) {
				Map.Entry ent = ((Map.Entry) o);
				if (ent.getKey() instanceof Enum<?>) {
					Enum<?> e = (Enum<?>) ent.getKey();
					int ord = e.ordinal();
					return (ord < iter.map.universe.length && iter.map.universe[ord] == e
						&& iter.map.valueTable[ord] != null && iter.map.valueTable[ord].equals(iter.map.hold(ent.getValue())));
				}
			}
			return false;
		}

		@Override
		public boolean remove(Object o) {
			if (o instanceof Map.Entry) {
				Map.Entry ent = ((Map.Entry) o);
				if (ent.getKey() instanceof Enum<?>) {
					Enum<?> e = (Enum<?>) ent.getKey();
					int ord = e.ordinal();
					if (ord < iter.map.universe.length && iter.map.universe[ord] == e
						&& iter.map.valueTable[ord] != null && iter.map.valueTable[ord].equals(iter.map.hold(ent.getValue()))) {
						iter.map.remove(e);
						return true;
					}
				}
			}
			return false;
		}

		/**
		 * Removes from this set all of its elements that are contained in the
		 * specified collection (optional operation).  If the specified
		 * collection is also a set, this operation effectively modifies this
		 * set so that its value is the <i>asymmetric set difference</i> of
		 * the two sets.
		 *
		 * @param c collection containing elements to be removed from this set
		 * @return {@code true} if this set changed as a result of the call
		 */
		@Override
		public boolean removeAll(Collection<?> c) {
			iter.reset();
			boolean res = false;
			for (Object o : c) {
				if (remove(o)) {
					iter.reset();
					res = true;
				}
			}
			return res;
		}

		/**
		 * {@inheritDoc}
		 *
		 * @param c
		 * @implSpec This implementation iterates over this collection, checking each
		 * element returned by the iterator in turn to see if it's contained
		 * in the specified collection.  If it's not so contained, it's removed
		 * from this collection with the iterator's {@code remove} method.
		 */
		@Override
		public boolean retainAll(Collection<?> c) {
			Objects.requireNonNull(c);
			iter.reset();
			boolean modified = false;
			while (iter.hasNext) {
				Map.Entry<Enum<?>, V> n = iter.next();
				if (!c.contains(n)) {
					iter.remove();
					modified = true;
				}
			}
			iter.reset();
			return modified;
		}

		/**
		 * {@inheritDoc}
		 *
		 * @param c
		 * @throws ClassCastException   {@inheritDoc}
		 * @throws NullPointerException {@inheritDoc}
		 * @implSpec This implementation iterates over the specified collection,
		 * checking each element returned by the iterator in turn to see
		 * if it's contained in this collection.  If all elements are so
		 * contained {@code true} is returned, otherwise {@code false}.
		 * @see #contains(Object)
		 */
		@Override
		public boolean containsAll(Collection<?> c) {
			iter.reset();
			return super.containsAll(c);
		}

		/**
		 * Returns an iterator over the elements contained in this collection.
		 *
		 * @return an iterator over the elements contained in this collection
		 */
		@Override
		public @NotNull MapIterator<V, Map.Entry<Enum<?>, V>> iterator() {
			return iter;
		}

		@Override
		public int size() {
			return iter.map.size;
		}

		@Override
		public int hashCode() {
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			iter.reset();
			int hc = super.hashCode();
			iter.currentIndex = currentIdx;
			iter.nextIndex = nextIdx;
			iter.hasNext = hn;
			return hc;
		}

		@Override
		public boolean equals(Object other) {
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			iter.reset();
			boolean res = super.equals(other);
			iter.currentIndex = currentIdx;
			iter.nextIndex = nextIdx;
			iter.hasNext = hn;
			return res;
		}


		@Override
		public String toString() {
			return toString(", ", true);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void clear() {
			iter.map.clear();
			iter.reset();
		}

		/**
		 * The iterator is reused by this data structure, and you can reset it
		 * back to the start of the iteration order using this.
		 */
		public void resetIterator() {
			iter.reset();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Object @NotNull [] toArray() {
			Object[] a = new Object[iter.map.size];
			int i = 0;
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			while (iter.hasNext) {
				a[i++] = new Entry<>(iter.next());
			}
			iter.currentIndex = currentIdx;
			iter.nextIndex = nextIdx;
			iter.hasNext = hn;

			return a;
		}

		/**
		 * {@inheritDoc}
		 *
		 * @param a
		 */
		@Override
		public <T> T @NotNull [] toArray(T[] a) {
			if (a.length < iter.map.size) a = Arrays.copyOf(a, iter.map.size);
			int i = 0;
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			while (iter.hasNext) {
				a[i++] = (T) new Entry<>(iter.next());
			}
			iter.currentIndex = currentIdx;
			iter.nextIndex = nextIdx;
			iter.hasNext = hn;

			return a;
		}

		/**
		 * Returns a new {@link ObjectList} containing the remaining items.
		 * Does not change the position of this iterator.
		 */
		public ObjectList<Map.Entry<Enum<?>, V>> toList() {
			ObjectList<Map.Entry<Enum<?>, V>> list = new ObjectList<>(iter.map.size);
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			while (iter.hasNext) {
				list.add(new Entry<>(iter.next()));
			}
			iter.currentIndex = currentIdx;
			iter.nextIndex = nextIdx;
			iter.hasNext = hn;
			return list;
		}

		/**
		 * Append the remaining items that this can iterate through into the given Collection.
		 * Does not change the position of this iterator.
		 *
		 * @param coll any modifiable Collection; may have items appended into it
		 * @return the given collection
		 */
		public Collection<Map.Entry<Enum<?>, V>> appendInto(Collection<Map.Entry<Enum<?>, V>> coll) {
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			while (iter.hasNext) {
				coll.add(new Entry<>(iter.next()));
			}
			iter.currentIndex = currentIdx;
			iter.nextIndex = nextIdx;
			iter.hasNext = hn;
			return coll;
		}

		/**
		 * Append the remaining items that this can iterate through into the given Map.
		 * Does not change the position of this iterator. Note that a Map is not a Collection.
		 *
		 * @param coll any modifiable Map; may have items appended into it
		 * @return the given map
		 */
		public Map<Enum<?>, V> appendInto(Map<Enum<?>, V> coll) {
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			while (iter.hasNext) {
				iter.next();
				coll.put(entry.key, entry.value);
			}
			iter.currentIndex = currentIdx;
			iter.nextIndex = nextIdx;
			iter.hasNext = hn;
			return coll;
		}
	}

	public static class Values<V> extends AbstractCollection<V> implements EnhancedCollection<V> {
		protected MapIterator<V, V> iter;

		public Values(EnumMap<V> map) {
			iter = new MapIterator<V, V>(map) {
				@Override
				public @NotNull MapIterator<V, V> iterator() {
					return this;
				}

				@Override
				public boolean hasNext() {
					if (!valid) {
						throw new RuntimeException("#iterator() cannot be used nested.");
					}
					return hasNext;
				}

				@Override
				public V next() {
					if (!hasNext) {
						throw new NoSuchElementException();
					}
					if (!valid) {
						throw new RuntimeException("#iterator() cannot be used nested.");
					}
					V value = map.release(map.valueTable[nextIndex]);
					currentIndex = nextIndex;
					findNextIndex();
					return value;
				}
			};

		}

		/**
		 * Returns an iterator over the elements contained in this collection.
		 *
		 * @return an iterator over the elements contained in this collection
		 */
		@Override
		public @NotNull MapIterator<V, V> iterator() {
			return iter;
		}

		@Override
		public boolean contains(Object o) {
			return iter.map.containsValue(o);
		}

		/**
		 * {@inheritDoc}
		 *
		 * @param o
		 * @throws UnsupportedOperationException {@inheritDoc}
		 * @throws ClassCastException            {@inheritDoc}
		 * @throws NullPointerException          {@inheritDoc}
		 * @implSpec This implementation iterates over the collection looking for the
		 * specified element.  If it finds the element, it removes the element
		 * from the collection using the iterator's remove method.
		 */
		@Override
		public boolean remove(Object o) {
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			iter.reset();
			boolean res = super.remove(o);
			iter.currentIndex = currentIdx;
			iter.nextIndex = nextIdx;
			iter.hasNext = hn;
			return res;
		}

		/**
		 * {@inheritDoc}
		 *
		 * @param c
		 * @throws UnsupportedOperationException {@inheritDoc}
		 * @throws ClassCastException            {@inheritDoc}
		 * @throws NullPointerException          {@inheritDoc}
		 */
		@Override
		public boolean removeAll(@NotNull Collection<?> c) {
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			iter.reset();
			boolean res = super.removeAll(c);
			iter.currentIndex = currentIdx;
			iter.nextIndex = nextIdx;
			iter.hasNext = hn;
			return res;

		}

		@Override
		public boolean retainAll(@NotNull Collection<?> c) {
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			iter.reset();
			boolean res = super.retainAll(c);
			iter.currentIndex = currentIdx;
			iter.nextIndex = nextIdx;
			iter.hasNext = hn;
			return res;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void clear() {
			iter.map.clear();
			iter.reset();
		}

		@Override
		public final boolean equals(Object o) {
			if (this == o)
				return true;
			if (!(o instanceof Collection))
				return false;

			Collection<?> values = (Collection<?>) o;
			if (size() != values.size()) return false;

			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			iter.reset();
			boolean res = true;
			for (Object obj : values) {
				if (!iter.hasNext) {
					res = false;
					break;
				}
				Object mine = iter.next();
				if (!Objects.equals(mine, obj)) {
					res = false;
					break;
				}
			}
			iter.currentIndex = currentIdx;
			iter.nextIndex = nextIdx;
			iter.hasNext = hn;
			return res;
		}

		@Override
		public int hashCode() {
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			iter.reset();
			int hc = 1;
			for (V v : this)
				hc += (v == null ? 0 : v.hashCode());
			iter.currentIndex = currentIdx;
			iter.nextIndex = nextIdx;
			iter.hasNext = hn;
			return hc;
		}

		/**
		 * The iterator is reused by this data structure, and you can reset it
		 * back to the start of the iteration order using this.
		 */
		public void resetIterator() {
			iter.reset();
		}


		@Override
		public String toString() {
			return toString(", ", true);
		}

		@Override
		public int size() {
			return iter.map.size;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Object @NotNull [] toArray() {
			Object[] a = new Object[iter.map.size];
			int i = 0;
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			while (iter.hasNext) {
				a[i++] = iter.next();
			}
			iter.currentIndex = currentIdx;
			iter.nextIndex = nextIdx;
			iter.hasNext = hn;

			return a;
		}

		/**
		 * {@inheritDoc}
		 *
		 * @param a
		 */
		@Override
		public <T> T @NotNull [] toArray(T[] a) {
			if (a.length < iter.map.size) a = Arrays.copyOf(a, iter.map.size);
			int i = 0;
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			while (iter.hasNext) {
				a[i++] = (T) iter.next();
			}
			iter.currentIndex = currentIdx;
			iter.nextIndex = nextIdx;
			iter.hasNext = hn;

			return a;
		}

		/**
		 * Returns a new {@link ObjectList} containing the remaining items.
		 * Does not change the position of this iterator.
		 */
		public ObjectList<V> toList() {
			ObjectList<V> list = new ObjectList<>(iter.map.size);
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			while (iter.hasNext) {
				list.add(iter.next());
			}
			iter.currentIndex = currentIdx;
			iter.nextIndex = nextIdx;
			iter.hasNext = hn;
			return list;
		}

		/**
		 * Append the remaining items that this can iterate through into the given Collection.
		 * Does not change the position of this iterator.
		 *
		 * @param coll any modifiable Collection; may have items appended into it
		 * @return the given collection
		 */
		public Collection<V> appendInto(Collection<V> coll) {
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			while (iter.hasNext) {
				coll.add(iter.next());
			}
			iter.currentIndex = currentIdx;
			iter.nextIndex = nextIdx;
			iter.hasNext = hn;
			return coll;
		}
	}

	public static class Keys extends AbstractSet<Enum<?>> implements EnhancedCollection<Enum<?>> {
		protected MapIterator<?, Enum<?>> iter;

		public Keys(EnumMap<?> map) {
			iter = new MapIterator<Object, Enum<?>>(map) {
				@Override
				public @NotNull MapIterator<?, Enum<?>> iterator() {
					return this;
				}

				@Override
				public boolean hasNext() {
					if (!valid) {
						throw new RuntimeException("#iterator() cannot be used nested.");
					}
					return hasNext;
				}

				@Override
				public Enum<?> next() {
					if (!hasNext) {
						throw new NoSuchElementException();
					}
					if (!valid) {
						throw new RuntimeException("#iterator() cannot be used nested.");
					}
					Enum<?> key = map.universe[nextIndex];
					currentIndex = nextIndex;
					findNextIndex();
					return key;
				}
			};
		}

		@Override
		public boolean contains(Object o) {
			return iter.map.containsKey(o);
		}

		/**
		 * {@inheritDoc}
		 *
		 * @param o
		 * @throws UnsupportedOperationException {@inheritDoc}
		 * @throws ClassCastException            {@inheritDoc}
		 * @throws NullPointerException          {@inheritDoc}
		 * @implSpec This implementation iterates over the collection looking for the
		 * specified element.  If it finds the element, it removes the element
		 * from the collection using the iterator's remove method.
		 *
		 * <p>Note that this implementation throws an
		 * {@code UnsupportedOperationException} if the iterator returned by this
		 * collection's iterator method does not implement the {@code remove}
		 * method and this collection contains the specified object.
		 */
		@Override
		public boolean remove(Object o) {
			if (o instanceof Enum<?>) {
				Enum<?> e = (Enum<?>) o;
				int ord = e.ordinal();
				if (ord < iter.map.universe.length && iter.map.universe[ord] == e
					&& iter.map.valueTable[ord] != null) {
					iter.map.remove(e);
					return true;
				}
			}
			return false;
		}

		/**
		 * Removes from this set all of its elements that are contained in the
		 * specified collection (optional operation).  If the specified
		 * collection is also a set, this operation effectively modifies this
		 * set so that its value is the <i>asymmetric set difference</i> of
		 * the two sets.
		 *
		 * @param c collection containing elements to be removed from this set
		 * @return {@code true} if this set changed as a result of the call
		 */
		@Override
		public boolean removeAll(Collection<?> c) {
			iter.reset();
			boolean res = false;
			for (Object o : c) {
				if (remove(o)) {
					iter.reset();
					res = true;
				}
			}
			return res;
		}

		/**
		 * Returns an iterator over the elements contained in this collection.
		 *
		 * @return an iterator over the elements contained in this collection
		 */
		@Override
		public @NotNull MapIterator<?, Enum<?>> iterator() {
			return iter;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void clear() {
			iter.map.clear();
			iter.reset();
		}


		@Override
		public String toString() {
			return toString(", ", true);
		}

		@Override
		public int size() {
			return iter.map.size;
		}

		@Override
		public boolean equals(Object other) {
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			iter.reset();
			boolean res = super.equals(other);
			iter.currentIndex = currentIdx;
			iter.nextIndex = nextIdx;
			iter.hasNext = hn;
			return res;
		}

		@Override
		public int hashCode() {
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			iter.reset();
			int hc = super.hashCode();
			iter.currentIndex = currentIdx;
			iter.nextIndex = nextIdx;
			iter.hasNext = hn;
			return hc;
		}

		/**
		 * The iterator is reused by this data structure, and you can reset it
		 * back to the start of the iteration order using this.
		 */
		public void resetIterator() {
			iter.reset();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Object @NotNull [] toArray() {
			Object[] a = new Object[iter.map.size];
			int i = 0;
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			while (iter.hasNext) {
				a[i++] = iter.next();
			}
			iter.currentIndex = currentIdx;
			iter.nextIndex = nextIdx;
			iter.hasNext = hn;

			return a;
		}

		/**
		 * {@inheritDoc}
		 *
		 * @param a
		 */
		@Override
		public <T> T @NotNull [] toArray(T[] a) {
			if (a.length < iter.map.size) a = Arrays.copyOf(a, iter.map.size);
			int i = 0;
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			while (iter.hasNext) {
				a[i++] = (T) iter.next();
			}
			iter.currentIndex = currentIdx;
			iter.nextIndex = nextIdx;
			iter.hasNext = hn;

			return a;
		}

		/**
		 * Returns a new {@link ObjectList} containing the remaining items.
		 * Does not change the position of this iterator.
		 *
		 * @return a new ObjectList containing the remaining items
		 */
		public ObjectList<Enum<?>> toList() {
			ObjectList<Enum<?>> list = new ObjectList<>(iter.map.size);
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			while (iter.hasNext) {
				list.add(iter.next());
			}
			iter.currentIndex = currentIdx;
			iter.nextIndex = nextIdx;
			iter.hasNext = hn;
			return list;
		}

		/**
		 * Returns a new {@link EnumSet} containing the remaining items.
		 * Does not change the position of this iterator.
		 * The EnumSet this returns will share a key universe with the map linked to this key set.
		 *
		 * @return a new EnumSet containing the remaining items, sharing a universe with this key set
		 */
		public EnumSet toEnumSet() {
			EnumSet es = new EnumSet(iter.map.universe, true);
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			while (iter.hasNext) {
				es.add(iter.next());
			}
			iter.currentIndex = currentIdx;
			iter.nextIndex = nextIdx;
			iter.hasNext = hn;
			return es;
		}

		/**
		 * Append the remaining items that this can iterate through into the given Collection.
		 * Does not change the position of this iterator.
		 *
		 * @param coll any modifiable Collection; may have items appended into it
		 * @return the given collection, potentially after modifications
		 */
		public Collection<Enum<?>> appendInto(Collection<Enum<?>> coll) {
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			while (iter.hasNext) {
				coll.add(iter.next());
			}
			iter.currentIndex = currentIdx;
			iter.nextIndex = nextIdx;
			iter.hasNext = hn;
			return coll;
		}
	}

	/**
	 * Constructs an empty map given the types as generic type arguments.
	 * This is usually less useful than just using the constructor, but can be handy
	 * in some code-generation scenarios when you don't know how many arguments you will have.
	 *
	 * @param <V> the type of values
	 * @return a new map containing nothing
	 */
	public static <V> EnumMap<V> with() {
		return new EnumMap<>();
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
	public static <V> EnumMap<V> with(Enum<?> key0, V value0) {
		EnumMap<V> map = new EnumMap<>();
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
	public static <V> EnumMap<V> with(Enum<?> key0, V value0, Enum<?> key1, V value1) {
		EnumMap<V> map = new EnumMap<>();
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
	public static <V> EnumMap<V> with(Enum<?> key0, V value0, Enum<?> key1, V value1, Enum<?> key2, V value2) {
		EnumMap<V> map = new EnumMap<>();
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
	public static <V> EnumMap<V> with(Enum<?> key0, V value0, Enum<?> key1, V value1, Enum<?> key2, V value2, Enum<?> key3, V value3) {
		EnumMap<V> map = new EnumMap<>();
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
	 * {@link #EnumMap(Enum[], Object[])}, which takes all keys and then all values.
	 * This needs all keys to have the same type and all values to have the same type, because
	 * it gets those types from the first key parameter and first value parameter. Any keys that don't
	 * have Enum as their type or values that don't have V as their type have that entry skipped.
	 *
	 * @param key0   the first key (an Enum)
	 * @param value0 the first value; will be used to determine the type of all values
	 * @param rest   an array or varargs of alternating Enum, V, Enum, V... elements
	 * @param <V>    the type of values, inferred from value0
	 * @return a new map containing the given keys and values
	 */
	public static <V> EnumMap<V> with(Enum<?> key0, V value0, Object... rest) {
		EnumMap<V> map = new EnumMap<>();
		map.put(key0, value0);
		map.putPairs(rest);
		return map;
	}


	/**
	 * Attempts to put alternating key-value pairs into this map, drawing a key, then a value from {@code pairs}, then
	 * another key, another value, and so on until another pair cannot be drawn. Any keys that don't
	 * have K as their type or values that don't have V as their type have that entry skipped.
	 * <br>
	 * If any item in {@code pairs} cannot be cast to the appropriate K or V type for its position in the arguments,
	 * that pair is ignored and neither that key nor value is put into the map. If any key is null, that pair is
	 * ignored, as well. If {@code pairs} is an Object array that is null, the entire call to putPairs() is ignored.
	 * If the length of {@code pairs} is odd, the last item (which will be unpaired) is ignored.
	 *
	 * @param pairs an array or varargs of alternating K, V, K, V... elements
	 */
	@SuppressWarnings("unchecked")
	public void putPairs(Object... pairs) {
		if (pairs != null) {
			for (int i = 1; i < pairs.length; i += 2) {
				try {
					if (pairs[i - 1] != null)
						put((Enum<?>) pairs[i - 1], (V) pairs[i]);
				} catch (ClassCastException ignored) {
				}
			}
		}
	}

	/**
	 * Adds items to this map drawn from the result of {@link #toString(String)} or
	 * {@link #appendTo(StringBuilder, String, boolean)}. Every key-value pair should be separated by
	 * {@code ", "}, and every key should be followed by {@code "="} before the value (which
	 * {@link #toString()} does).
	 * A PartialParser will be used to parse keys from sections of {@code str}, and a different PartialParser to
	 * parse values. Usually, keyParser is produced by
	 * {@link PartialParser#enumParser(ObjToObjFunction)}. Any brackets inside the given range
	 * of characters will ruin the parsing, so increase offset by 1 and
	 * reduce length by 2 if the original String had brackets added to it.
	 *
	 * @param str         a String containing parseable text
	 * @param keyParser   a PartialParser that returns a {@code Enum<?>} key from a section of {@code str}, typically
	 *                    produced by {@link PartialParser#enumParser(ObjToObjFunction)}
	 * @param valueParser a PartialParser that returns a {@code V} value from a section of {@code str}
	 */
	public void putLegible(String str, PartialParser<Enum<?>> keyParser, PartialParser<V> valueParser) {
		putLegible(str, ", ", "=", keyParser, valueParser, 0, -1);
	}

	/**
	 * Adds items to this map drawn from the result of {@link #toString(String)} or
	 * {@link #appendTo(StringBuilder, String, boolean)}. Every key-value pair should be separated by
	 * {@code entrySeparator}, and every key should be followed by "=" before the value (which
	 * {@link #toString(String)} does).
	 * A PartialParser will be used to parse keys from sections of {@code str}, and a different PartialParser to
	 * parse values. Usually, keyParser is produced by
	 * {@link PartialParser#enumParser(ObjToObjFunction)}. Any brackets inside the given range
	 * of characters will ruin the parsing, so increase offset by 1 and
	 * reduce length by 2 if the original String had brackets added to it.
	 *
	 * @param str            a String containing parseable text
	 * @param entrySeparator the String separating every key-value pair
	 * @param keyParser      a PartialParser that returns a {@code Enum<?>} key from a section of {@code str}, typically
	 *                       produced by {@link PartialParser#enumParser(ObjToObjFunction)}
	 * @param valueParser    a PartialParser that returns a {@code V} value from a section of {@code str}
	 */
	public void putLegible(String str, String entrySeparator, PartialParser<Enum<?>> keyParser, PartialParser<V> valueParser) {
		putLegible(str, entrySeparator, "=", keyParser, valueParser, 0, -1);
	}

	/**
	 * Adds items to this map drawn from the result of {@link #toString(String)} or
	 * {@link #appendTo(StringBuilder, String, String, boolean, Appender, Appender)}. A PartialParser will be used to
	 * parse keys from sections of {@code str}, and a different PartialParser to parse values. Usually, keyParser is
	 * produced by {@link PartialParser#enumParser(ObjToObjFunction)}. Any brackets
	 * inside the given range of characters will ruin the parsing, so increase offset by 1 and
	 * reduce length by 2 if the original String had brackets added to it.
	 *
	 * @param str               a String containing parseable text
	 * @param entrySeparator    the String separating every key-value pair
	 * @param keyValueSeparator the String separating every key from its corresponding value
	 * @param keyParser         a PartialParser that returns a {@code Enum<?>} key from a section of {@code str}, typically
	 *                          produced by {@link PartialParser#enumParser(ObjToObjFunction)}
	 * @param valueParser       a PartialParser that returns a {@code V} value from a section of {@code str}
	 */
	public void putLegible(String str, String entrySeparator, String keyValueSeparator, PartialParser<Enum<?>> keyParser, PartialParser<V> valueParser) {
		putLegible(str, entrySeparator, keyValueSeparator, keyParser, valueParser, 0, -1);
	}

	/**
	 * Puts key-value pairs into this map drawn from the result of {@link #toString(String)} or
	 * {@link #appendTo(StringBuilder, String, String, boolean, Appender, Appender)}. A PartialParser will be used
	 * to parse keys from sections of {@code str}, and a different PartialParser to parse values. Usually, keyParser is
	 * produced by {@link PartialParser#enumParser(ObjToObjFunction)}. Any brackets
	 * inside the given range of characters will ruin the parsing, so increase offset by 1 and
	 * reduce length by 2 if the original String had brackets added to it.
	 *
	 * @param str               a String containing parseable text
	 * @param entrySeparator    the String separating every key-value pair
	 * @param keyValueSeparator the String separating every key from its corresponding value
	 * @param keyParser         a PartialParser that returns a {@code Enum<?>} key from a section of {@code str}, typically
	 *                          produced by {@link PartialParser#enumParser(ObjToObjFunction)}
	 * @param valueParser       a PartialParser that returns a {@code V} value from a section of {@code str}
	 * @param offset            the first position to read parseable text from in {@code str}
	 * @param length            how many chars to read; -1 is treated as maximum length
	 */
	public void putLegible(String str, String entrySeparator, String keyValueSeparator, PartialParser<Enum<?>> keyParser, PartialParser<V> valueParser, int offset, int length) {
		int sl, el, kvl;
		if (str == null || entrySeparator == null || keyValueSeparator == null || keyParser == null || valueParser == null
			|| (sl = str.length()) < 1 || (el = entrySeparator.length()) < 1 || (kvl = keyValueSeparator.length()) < 1
			|| offset < 0 || offset > sl - 1) return;
		final int lim = length < 0 ? sl : Math.min(offset + length, sl);
		int end = str.indexOf(keyValueSeparator, offset + 1);
		@Nullable Enum<?> k = null;
		boolean incomplete = false;
		while (end != -1 && end + kvl < lim) {
			k = keyParser.parse(str, offset, end);
			offset = end + kvl;
			end = str.indexOf(entrySeparator, offset + 1);
			if (end != -1 && end + el < lim) {
				put(k, valueParser.parse(str, offset, end));
				offset = end + el;
				end = str.indexOf(keyValueSeparator, offset + 1);
			} else {
				incomplete = true;
			}
		}
		if (incomplete && offset < lim) {
			put(k, valueParser.parse(str, offset, lim));
		}
	}

	/**
	 * Creates a new map by parsing all of {@code str} with the given PartialParser for keys and
	 * for values, with entries separated by {@code entrySeparator}, such as {@code ", "} and
	 * the keys separated from values by {@code keyValueSeparator}, such as {@code "="}.
	 * <br>
	 * Various {@link PartialParser} instances are defined as constants, such as
	 * {@link PartialParser#DEFAULT_STRING}, and others can be created by static methods in PartialParser, such as
	 * {@link PartialParser#objectListParser(PartialParser, String, boolean)}.
	 * The {@code keyParser} is often produced by {@link PartialParser#enumParser(ObjToObjFunction)}.
	 *
	 * @param str               a String containing parseable text
	 * @param entrySeparator    the String separating every key-value pair
	 * @param keyValueSeparator the String separating every key from its corresponding value
	 * @param keyParser         a PartialParser that returns an {@link Enum} key from a section of {@code str}
	 * @param valueParser       a PartialParser that returns a {@code V} value from a section of {@code str}
	 */
	public static <V> EnumMap<V> parse(String str,
													 String entrySeparator,
													 String keyValueSeparator,
													 PartialParser<Enum<?>> keyParser,
													 PartialParser<V> valueParser) {
		return parse(str, entrySeparator, keyValueSeparator, keyParser, valueParser, false);
	}
	/**
	 * Creates a new map by parsing all of {@code str} (or if {@code brackets} is true, all but the first and last
	 * chars) with the given PartialParser for keys and for values, with entries separated by {@code entrySeparator},
	 * such as {@code ", "} and the keys separated from values by {@code keyValueSeparator}, such as {@code "="}.
	 * <br>
	 * Various {@link PartialParser} instances are defined as constants, such as
	 * {@link PartialParser#DEFAULT_STRING}, and others can be created by static methods in PartialParser, such as
	 * {@link PartialParser#objectListParser(PartialParser, String, boolean)}.
	 * The {@code keyParser} is often produced by {@link PartialParser#enumParser(ObjToObjFunction)}.
	 *
	 * @param str               a String containing parseable text
	 * @param entrySeparator    the String separating every key-value pair
	 * @param keyValueSeparator the String separating every key from its corresponding value
	 * @param keyParser         a PartialParser that returns an {@link Enum} key from a section of {@code str}
	 * @param valueParser       a PartialParser that returns a {@code V} value from a section of {@code str}
	 * @param brackets          if true, the first and last chars in {@code str} will be ignored
	 */
	public static <V> EnumMap<V> parse(String str,
													 String entrySeparator,
													 String keyValueSeparator,
													 PartialParser<Enum<?>> keyParser,
													 PartialParser<V> valueParser,
													 boolean brackets) {
		EnumMap<V> m = new EnumMap<>();
		if(brackets)
			m.putLegible(str, entrySeparator, keyValueSeparator, keyParser, valueParser, 1, str.length() - 1);
		else
			m.putLegible(str, entrySeparator, keyValueSeparator, keyParser, valueParser, 0, -1);
		return m;
	}

	/**
	 * Creates a new map by parsing the given subrange of {@code str} with the given PartialParser for keys and for
	 * values, with entries separated by {@code entrySeparator}, such as {@code ", "} and the keys separated from values
	 * by {@code keyValueSeparator}, such as {@code "="}.
	 * <br>
	 * Various {@link PartialParser} instances are defined as constants, such as
	 * {@link PartialParser#DEFAULT_STRING}, and others can be created by static methods in PartialParser, such as
	 * {@link PartialParser#objectListParser(PartialParser, String, boolean)}.
	 * The {@code keyParser} is often produced by {@link PartialParser#enumParser(ObjToObjFunction)}.
	 *
	 * @param str               a String containing parseable text
	 * @param entrySeparator    the String separating every key-value pair
	 * @param keyValueSeparator the String separating every key from its corresponding value
	 * @param keyParser         a PartialParser that returns an {@link Enum} key from a section of {@code str}
	 * @param valueParser       a PartialParser that returns a {@code V} value from a section of {@code str}
	 * @param offset            the first position to read parseable text from in {@code str}
	 * @param length            how many chars to read; -1 is treated as maximum length
	 */
	public static <V> EnumMap<V> parse(String str,
													 String entrySeparator,
													 String keyValueSeparator,
													 PartialParser<Enum<?>> keyParser,
													 PartialParser<V> valueParser,
													 int offset,
													 int length) {
		EnumMap<V> m = new EnumMap<>();
		m.putLegible(str, entrySeparator, keyValueSeparator, keyParser, valueParser, offset, length);
		return m;
	}

	/**
	 * Constructs an empty map given the types as generic type arguments; an alias for {@link #with()}.
	 *
	 * @param <V> the type of values
	 * @return a new map containing nothing
	 */
	public static <V> EnumMap<V> of() {
		return with();
	}

	/**
	 * Constructs a single-entry map given one key and one value; an alias for {@link #with(Enum, Object)}.
	 *
	 * @param key0   the first and only key
	 * @param value0 the first and only value
	 * @param <V>    the type of value0
	 * @return a new map containing just the entry mapping key0 to value0
	 */
	public static <V> EnumMap<V> of(Enum<?> key0, V value0) {
		return with(key0, value0);
	}

	/**
	 * Constructs a map given alternating keys and values; an alias for {@link #with(Enum, Object, Object...)}.
	 *
	 * @param key0   the first key (an Enum)
	 * @param value0 the first value; will be used to determine the type of all values
	 * @param rest   an array or varargs of alternating Enum, V, Enum, V... elements
	 * @param <V>    the type of values, inferred from value0
	 * @return a new map containing the given keys and values
	 */
	public static <V> EnumMap<V> of(Enum<?> key0, V value0, Object... rest) {
		return with(key0, value0, rest);
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
	public static <V> EnumMap<V> noneOf(Enum<?>[] universe) {
		return new EnumMap<>(universe);
	}
}

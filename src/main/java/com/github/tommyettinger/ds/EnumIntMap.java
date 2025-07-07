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
import com.github.tommyettinger.digital.BitConversion;
import com.github.tommyettinger.ds.support.util.*;
import com.github.tommyettinger.function.IntIntToIntBiFunction;
import com.github.tommyettinger.function.ObjToIntFunction;
import com.github.tommyettinger.function.ObjToObjFunction;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;

/**
 * An unordered map where the keys are {@code Enum}s and values are primitive ints. Null keys are not allowed.
 * Unlike {@link java.util.EnumMap}, this does not require a Class at construction time, which can be useful for serialization
 * purposes. No allocation is done unless this is changing its table size and/or key universe.
 * <br>
 * This class never actually hashes keys in its primary operations (get(), put(), remove(), containsKey(), etc.), since it can
 * rely on keys having an Enum type, and so having {@link Enum#ordinal()} available. The ordinal allows constant-time access
 * to a guaranteed-unique {@code int} that will always be non-negative and less than the size of the key universe. The table of
 * possible values always starts sized to fit exactly as many values as there are keys in the key universe.
 * <br>
 * The key universe is an important concept here; it is simply an array of all possible Enum values the EnumIntMap can use as keys, in
 * the specific order they are declared. You almost always get a key universe by calling {@code MyEnum.values()}, but you
 * can also use {@link Class#getEnumConstants()} for an Enum class. You can and generally should reuse key universes in order to
 * avoid allocations and/or save memory; the constructor {@link #EnumIntMap(Enum[])} (with no values given) creates an empty EnumIntMap with
 * a given key universe. If you need to use the zero-argument constructor, you can, and the key universe will be obtained from the
 * first key placed into the EnumIntMap. You can also set the key universe with {@link #clearToUniverse(Enum[])}, in the process of
 * clearing the map.
 * <br>
 * This class tries to be as compatible as possible with {@link java.util.EnumMap} while using primitive keys,
 * though this expands on that where possible.
 *
 * @author Nathan Sweet (Keys, Values, Entries, and MapIterator, as well as general structure)
 * @author Tommy Ettinger (Enum-related adaptation)
 */
public class EnumIntMap implements Iterable<EnumIntMap.Entry> {
	protected @Nullable EnumSet keys;

	protected int @Nullable[] valueTable;

	@Nullable protected transient Entries entries1;
	@Nullable protected transient Entries entries2;
	@Nullable protected transient Values values1;
	@Nullable protected transient Values values2;
	@Nullable protected transient Keys keys1;
	@Nullable protected transient Keys keys2;

	/**
	 * Returned by {@link #get(Object)} when no value exists for the given key, as well as some other methods to indicate that
	 * no value in the Map could be returned. Defaults to {@code null}.
	 */
	public int defaultValue = 0;

	/**
	 * Empty constructor; using this will postpone creating the key universe and allocating the value table until {@link #put} is
	 * first called (potentially indirectly). You can also use {@link #clearToUniverse} to set the key universe and value table.
	 */
	public EnumIntMap() {
	}

	/**
	 * Initializes this map so that it has exactly enough capacity as needed to contain each Enum constant defined in
	 * {@code universe}, assuming universe stores every possible constant in one Enum type. This map will start empty.
	 * You almost always obtain universe from calling {@code values()} on an Enum type, and you can share one
	 * reference to one Enum array across many EnumIntMap instances if you don't modify the shared array. Sharing the same
	 * universe helps save some memory if you have (very) many EnumIntMap instances.
	 * @param universe almost always, the result of calling {@code values()} on an Enum type; used directly, not copied
	 */
	public EnumIntMap(Enum<?> @Nullable [] universe) {
		if(universe == null) return;
		this.keys = EnumSet.noneOf(universe);
		valueTable = new int[universe.length];
	}

	/**
	 * Initializes this map so that it has exactly enough capacity as needed to contain each Enum constant defined by the
	 * Class {@code universeClass}, assuming universeClass is non-null. This simply calls {@link #EnumIntMap(Enum[])}
	 * for convenience. Note that this constructor allocates a new array of Enum constants each time it is called, where
	 * if you use {@link #EnumIntMap(Enum[])}, you can reuse an unmodified array to reduce allocations.
	 * @param universeClass the Class of an Enum type that defines the universe of valid Enum items this can hold
	 */
	public EnumIntMap(@Nullable Class<? extends Enum<?>> universeClass) {
		this(universeClass == null ? null : universeClass.getEnumConstants());
	}

	/**
	 * Creates a new map identical to the specified EnumIntMap. This will share a key universe with the given EnumIntMap, if non-null.
	 *
	 * @param map an EnumIntMap to copy
	 */
	public EnumIntMap(EnumIntMap map) {
		this.keys = map.keys;
		if(map.valueTable != null)
			valueTable = Arrays.copyOf(map.valueTable, map.valueTable.length);
		defaultValue = map.defaultValue;
	}

	/**
	 * Given two side-by-side arrays, one of Enum keys, one of int values, this constructs a map and inserts each pair of key and
	 * value into it. If keys and values have different lengths, this only uses the length of the smaller array.
	 *
	 * @param keys   an array of Enum keys
	 * @param values an array of int values
	 */
	public EnumIntMap(Enum<?>[] keys, int[] values) {
		this();
		putAll(keys, values);
	}

	/**
	 * Given two side-by-side collections, one of Enum keys, one of int values, this constructs a map and inserts each pair of key
	 * and value into it. If keys and values have different lengths, this only uses the length of the smaller collection.
	 *
	 * @param keys   a Collection of Enum keys
	 * @param values a PrimitiveCollection of int values
	 */
	public EnumIntMap(Collection<? extends Enum<?>> keys, PrimitiveCollection.OfInt values) {
		this();
		putAll(keys, values);
	}

	/**
	 * Given two side-by-side collections, one of Enum keys, one of int values, this inserts each pair of key and
	 * value into this map with put().
	 *
	 * @param keys   a Collection of Enum keys
	 * @param values a PrimitiveCollection of int values
	 */
	public void putAll (Collection<? extends Enum<?>> keys, PrimitiveCollection.OfInt values) {
		Enum<?> key;
		Iterator<? extends Enum<?>> ki = keys.iterator();
		IntIterator vi = values.iterator();
		while (ki.hasNext() && vi.hasNext()) {
			key = ki.next();
			put(key, vi.nextInt());
		}
	}

	/**
	 * Returns the old value associated with the specified key, or this map's {@link #defaultValue} if there was no prior value.
	 * If this EnumIntMap does not yet have a key universe and/or value table, this gets the key universe from {@code key} and uses it
	 * from now on for this EnumIntMap.
	 *
	 * @param key the Enum key to try to place into this EnumIntMap
	 * @param value the int value to associate with {@code key}
	 * @return the previous value associated with {@code key}, or {@link #getDefaultValue()} if the given key was not present
	 */
	public int put (@NonNull Enum<?> key, int value) {
		if(key == null) return defaultValue;
		Enum<?>[] universe = key.getDeclaringClass().getEnumConstants();
		if(keys == null) keys = new EnumSet();
		if(valueTable == null) valueTable = new int[universe.length];
		int i = key.ordinal();
		if(i >= valueTable.length || universe[i] != key)
			throw new ClassCastException("Incompatible key for the EnumIntMap's universe.");
		int oldValue = valueTable[i];
		valueTable[i] = value;
		if (keys.add(key)) {
			return defaultValue;
		}
		return oldValue;
	}

	/**
	 * Acts like {@link #put(Enum, int)}, but uses the specified {@code defaultValue} instead of
	 * {@link #getDefaultValue() the default value for this EnumIntMap}.
	 * @param key the Enum key to try to place into this EnumIntMap
	 * @param value the int value to associate with {@code key}
	 * @param defaultValue the int value to return if {@code key} was not already present
	 * @return the previous value associated with {@code key}, or the given {@code defaultValue} if the given key was not present
	 */
	public int putOrDefault (@NonNull Enum<?> key, int value, int defaultValue) {
		if(key == null) return defaultValue;
		Enum<?>[] universe = key.getDeclaringClass().getEnumConstants();
		if(keys == null) keys = new EnumSet();
		if(valueTable == null) valueTable = new int[universe.length];
		int i = key.ordinal();
		if(i >= valueTable.length || universe[i] != key)
			throw new ClassCastException("Incompatible key for the EnumIntMap's universe.");
		int oldValue = valueTable[i];
		valueTable[i] = value;
		if (keys.add(key)) {
			return defaultValue;
		}
		return oldValue;
	}

	/**
	 * Puts every key-value pair in the given map into this, with the values from the given map
	 * overwriting the previous values if two keys are identical. If this EnumIntMap doesn't yet have
	 * a key universe, it will now share a key universe with the given {@code map}. Even if the
	 * given EnumIntMap is empty, it can still be used to obtain a key universe for this EnumIntMap
	 * (assuming it has a key universe).
	 *
	 * @param map another EnumIntMap with an equivalent key universe
	 */
	public void putAll (@NonNull EnumIntMap map) {
		if(map.keys == null || map.keys.universe == null) return;
		if(keys == null || keys.universe == null) keys = map.keys;
		Enum<?>[] universe = keys.universe;
		if(valueTable == null) valueTable = new int[universe.length];
		if(map.keys.isEmpty()) return;
		final int n = map.valueTable.length;
		if(this.valueTable.length != n) return;
		int[] valueTable = map.valueTable;
		int value;
		for (int i = 0; i < n; i++) {
			if(universe[i] != map.keys.universe[i])
				throw new ClassCastException("Incompatible key for the EnumIntMap's universe.");
			value = valueTable[i];
			this.keys.add(universe[i]);
			this.valueTable[i] = value;
		}
	}

	/**
	 * Given two side-by-side arrays, one of keys, one of values, this inserts each pair of key and value into this map with put().
	 * Delegates to {@link #putAll(Enum[], int, int[], int, int)}.
	 *
	 * @param keys   an array of keys
	 * @param values an array of values
	 */
	public void putAll (Enum<?>[] keys, int[] values) {
		putAll(keys, 0, values, 0, Math.min(keys.length, values.length));
	}

	/**
	 * Given two side-by-side arrays, one of keys, one of values, this inserts each pair of key and value into this map with put().
	 * Delegates to {@link #putAll(Enum[], int, int[], int, int)}.
	 *
	 * @param keys   an array of keys
	 * @param values an array of values
	 * @param length how many items from keys and values to insert, at-most
	 */
	public void putAll (Enum<?>[] keys, int[] values, int length) {
		putAll(keys, 0, values, 0, Math.min(length, Math.min(keys.length, values.length)));
	}

	/**
	 * Given two side-by-side arrays, one of keys, one of values, this inserts each pair of key and value into this map with
	 * {@link #put(Enum, int)}.
	 *
	 * @param keys        an array of keys
	 * @param keyOffset   the first index in keys to insert
	 * @param values      an array of values
	 * @param valueOffset the first index in values to insert
	 * @param length      how many items from keys and values to insert, at-most
	 */
	public void putAll (Enum<?>[] keys, int keyOffset, int[] values, int valueOffset, int length) {
		length = Math.min(length, Math.min(keys.length - keyOffset, values.length - valueOffset));
		Enum<?> key;
		for (int k = keyOffset, v = valueOffset, i = 0, n = length; i < n; i++, k++, v++) {
			key = keys[k];
			put(key, values[v]);
		}
	}

	/**
	 * Returns the value for the specified key, or {@link #defaultValue} if the key is not in the map.
	 * Note that {@link #defaultValue} is often null, which is also a valid value that can be assigned to a
	 * legitimate key. Checking that the result of this method is null does not guarantee that the
	 * {@code key} is not present.
	 *
	 * @param key a non-null Object that should always be an Enum
	 */
	public int get (Object key) {
		if(keys == null || keys.isEmpty() || keys.universe == null || !(key instanceof Enum<?>))
			return defaultValue;
		final Enum<?> e = (Enum<?>)key;
		return keys.contains(e) ? valueTable[e.ordinal()] : defaultValue;
	}

	/**
	 * Returns the value for the specified key, or the given default value if the key is not in the map.
	 */
	public int getOrDefault (Object key, int defaultValue) {
		if(keys == null || keys.isEmpty() || keys.universe == null || !(key instanceof Enum<?>))
			return defaultValue;
		final Enum<?> e = (Enum<?>)key;
		return keys.contains(e) ? valueTable[e.ordinal()] : defaultValue;
	}

	public int remove (Object key) {
		if(keys == null || keys.isEmpty() || keys.universe == null || !(key instanceof Enum<?>))
			return defaultValue;
		Enum<?> e = (Enum<?>)key;
		final int ord = e.ordinal();
		int o = valueTable[ord];
		if(!keys.remove(e)) return defaultValue;
		return o;
	}

	/**
	 * Copies all the mappings from the specified map to this map
	 * (optional operation).  The effect of this call is equivalent to that
	 * of calling {@link #put(Enum, int) put(k, v)} on this map once
	 * for each mapping from key {@code k} to value {@code v} in the
	 * specified map.  The behavior of this operation is undefined if the
	 * specified map is modified while the operation is in progress.
	 * <br>
	 * Note that {@link #putAll(EnumIntMap)} is more specific and can be
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
	public void putAll (ObjectIntMap<Enum<?>> m) {
		for (ObjectIntMap.Entry<Enum<?>> kv : m.entrySet()) {put(kv.getKey(), kv.getValue());}
	}	/**
	 * Returns the key's current value and increments the stored value. If the key is not in the map, defaultValue + increment is
	 * put into the map and defaultValue is returned.
	 */
	public int getAndIncrement (Enum<?> key, int defaultValue, int increment) {
		if(key == null) return defaultValue;
		Enum<?>[] universe = key.getDeclaringClass().getEnumConstants();
		if(keys == null) keys = new EnumSet();
		if(valueTable == null) valueTable = new int[universe.length];
		int i = key.ordinal();
		if(i >= valueTable.length || universe[i] != key)
			throw new ClassCastException("Incompatible key for the EnumIntMap's universe.");
		int oldValue = valueTable[i];
		if (keys.add(key)) {
			valueTable[i] = defaultValue + increment;
			return defaultValue;
		}
		valueTable[i] += increment;
		return oldValue;
	}

	/**
	 * Returns true if the map has one or more items.
	 */
	public boolean notEmpty () {
		return keys != null && keys.size != 0;
	}

	/**
	 * Returns the number of key-value mappings in this map.
	 *
	 * @return the number of key-value mappings in this map
	 */
	public int size () {
		if(keys == null) return 0;
		return keys.size;
	}

	/**
	 * Returns true if the map is empty.
	 */
	public boolean isEmpty () {
		return keys == null || keys.size == 0;
	}

	/**
	 * Gets the default value, a {@code int} which is returned by {@link #get(Object)} if the key is not found.
	 * If not changed, the default value is 0.
	 *
	 * @return the current default value
	 */
	public int getDefaultValue () {
		return defaultValue;
	}

	/**
	 * Sets the default value, a {@code int} which is returned by {@link #get(Object)} if the key is not found.
	 * If not changed, the default value is 0. Note that {@link #getOrDefault(Object, int)}  is also available,
	 * which allows specifying a "not-found" value per-call.
	 *
	 * @param defaultValue may be any int; should usually be one that doesn't occur as a typical value
	 */
	public void setDefaultValue (int defaultValue) {
		this.defaultValue = defaultValue;
	}

	/**
	 * Removes all the elements from this map.
	 * The map will be empty after this call returns.
	 * This does not change the universe of possible Enum items this can hold.
	 */
	public void clear () {
		if(keys != null)
			keys.clear();
	}

	/**
	 * Removes all the elements from this map and can reset the universe of possible Enum items this can hold.
	 * The map will be empty after this call returns.
	 * This changes the universe of possible Enum items this can hold to match {@code universe}.
	 * If {@code universe} is null, this resets this map to the state it would have after {@link #EnumIntMap()} was called.
	 * If the table this would need is the same size as or smaller than the current table (such as if {@code universe} is the same as
	 * the universe here), this will not allocate, but will still clear any items this holds and will set the universe to the given one.
	 * Otherwise, this allocates and uses a new table of a larger size, with nothing in it, and uses the given universe.
	 * This always uses {@code universe} directly, without copying.
	 * <br>
	 * This can be useful to allow an EnumIntMap that was created with {@link #EnumIntMap()} to share a universe with other EnumIntMaps.
	 *
	 * @param universe the universe of possible Enum items this can hold; almost always produced by {@code values()} on an Enum
	 */
	public void clearToUniverse (Enum<?>@Nullable [] universe) {
		if (universe == null) {
			keys = null;
			valueTable = null;
		} else {
			if (keys == null) {
				keys = EnumSet.noneOf(universe);
			} else
				keys.clearToUniverse(universe);

			if (keys.universe != null && universe.length > keys.universe.length) {
				valueTable = new int[universe.length];
			}
		}
	}


	/**
	 * Removes all the elements from this map and can reset the universe of possible Enum items this can hold.
	 * The map will be empty after this call returns.
	 * This changes the universe of possible Enum items this can hold to match the Enum constants in {@code universe}.
	 * If {@code universe} is null, this resets this map to the state it would have after {@link #EnumIntMap()} was called.
	 * If the table this would need is the same size as or smaller than the current table (such as if {@code universe} is the same as
	 * the universe here), this will not allocate, but will still clear any items this holds and will set the universe to the given one.
	 * Otherwise, this allocates and uses a new table of a larger size, with nothing in it, and uses the given universe.
	 * This calls {@link Class#getEnumConstants()} if universe is non-null, which allocates a new array.
	 * <br>
	 * You may want to prefer calling {@link #clearToUniverse(Enum[])} (the overload that takes an array), because it can be used to
	 * share one universe array between many EnumIntMap instances. This overload, given a Class, has to call {@link Class#getEnumConstants()}
	 * and thus allocate a new array each time this is called.
	 *
	 * @param universe the Class of an Enum type that stores the universe of possible Enum items this can hold
	 */
	public void clearToUniverse (@Nullable Class<? extends Enum<?>> universe) {
		if (universe == null) {
			keys = null;
			valueTable = null;
		} else {
			Enum<?>[] cons = universe.getEnumConstants();
			if (keys == null) {
				keys = EnumSet.noneOf(cons);
			} else
				keys.clearToUniverse(cons);

			if (keys.universe != null && cons.length > keys.universe.length) {
				valueTable = new int[cons.length];
			}
		}
	}

	/**
	 * Gets the current key universe; this is a technically-mutable array, but should never be modified.
	 * To set the universe on an existing EnumIntMap (with existing contents), you can use {@link #clearToUniverse(Enum[])}.
	 * If an EnumIntMap has not been initialized, just adding a key will set the key universe to match the given item.
	 * @return the current key universe
	 */
	public Enum<?> @Nullable[] getUniverse () {
		return keys == null ? null : keys.universe;
	}

	/**
	 * Returns true if the specified value is in the map. Note this traverses the entire map and compares every value, which may
	 * be an expensive operation.
	 */
	public boolean containsValue (int value) {
		if(this.valueTable == null) return false;
		int[] valueTable = this.valueTable;
		for (int i = valueTable.length - 1; i >= 0; i--) {if (valueTable[i] == value) {return true;}}
		return false;
	}

	public boolean containsKey (Object key) {
		if(keys == null || keys.isEmpty() || !(key instanceof Enum<?>))
			return false;
		final Enum<?> e = (Enum<?>)key;
		return keys.contains(e);
	}

	/**
	 * Returns the key for the specified value, or null if it is not in the map. Note this traverses the entire map and compares
	 * every value, which may be an expensive operation.
	 *
	 * @return the corresponding Enum if the value was found, or null otherwise
	 */
	@Nullable
	public Enum<?> findKey (int value) {
		if(this.keys == null || this.valueTable == null || this.keys.isEmpty() || keys.universe == null) return null;
		int[] valueTable = this.valueTable;
		for (int i = valueTable.length - 1; i >= 0; i--) {
			if (valueTable[i] == value) {
				Enum<?> item = keys.universe[i];
				if (keys.contains(item)) return item;
			}
		}
		return null;
	}

	@Override
	public int hashCode () {
		if(keys == null || keys.universe == null || keys.isEmpty())
			return 0;
		int h = keys.size;
		Enum<?>[] universe = keys.universe;
		int[] valueTable = this.valueTable;
		for (int i = keys.nextOrdinal(0); i != -1; i = keys.nextOrdinal(i+1)) {
			Enum<?> key = universe[i];
			h ^= key.hashCode();
			int value = valueTable[i];
			h ^= value;
		}
		return h;
	}

	@Override
	public boolean equals (Object obj) {
		if (obj == this) {return true;}
		if (!(obj instanceof EnumIntMap)) {return false;}
		EnumIntMap other = (EnumIntMap)obj;
		if (other.size() != size()) {return false;}
		if(this.keys == null || this.keys.universe == null || this.valueTable == null) return other.isEmpty();
		Enum<?>[] universe = this.keys.universe;
		int[] valueTable = this.valueTable;
		try {
			for (int i = keys.nextOrdinal(0); i != -1; i = keys.nextOrdinal(i+1)) {
				int value = valueTable[i];
				if (value != (other.get(universe[i]))) {return false;}
			}
		}catch (ClassCastException | NullPointerException unused) {
			return false;
		}

		return true;
	}

	@Override
	public String toString () {
		return toString(", ", true);
	}

	/**
	 * Delegates to {@link #toString(String, boolean)} with the given entrySeparator and without braces.
	 * This is different from {@link #toString()}, which includes braces by default.
	 *
	 * @param entrySeparator how to separate entries, such as {@code ", "}
	 * @return a new String representing this map
	 */
	public String toString (String entrySeparator) {
		return toString(entrySeparator, false);
	}

	public String toString (String entrySeparator, boolean braces) {
		return appendTo(new StringBuilder(32), entrySeparator, braces).toString();
	}
	/**
	 * Makes a String from the contents of this ObjectObjectMap, but uses the given {@link Appender} and
	 * {@link IntAppender} to convert each key and each value to a customizable representation and append them
	 * to a temporary StringBuilder. To use
	 * the default String representation, you can use {@code StringBuilder::append} as an appender.
	 *
	 * @param entrySeparator how to separate entries, such as {@code ", "}
	 * @param keyValueSeparator how to separate each key from its value, such as {@code "="} or {@code ":"}
	 * @param braces true to wrap the output in curly braces, or false to omit them
	 * @param keyAppender a function that takes a StringBuilder and an Enum, and returns the modified StringBuilder
	 * @param valueAppender a function that takes a StringBuilder and a int, and returns the modified StringBuilder
	 * @return a new String representing this map
	 */
	public String toString (String entrySeparator, String keyValueSeparator, boolean braces,
		Appender<Enum<?>> keyAppender, IntAppender valueAppender){
		return appendTo(new StringBuilder(), entrySeparator, keyValueSeparator, braces, keyAppender, valueAppender).toString();
	}
	public StringBuilder appendTo (StringBuilder sb, String entrySeparator, boolean braces) {
		return appendTo(sb, entrySeparator, "=", braces, (builder, e) -> builder.append(e.name()), IntAppender.DEFAULT);
	}

	/**
		 * Appends to a StringBuilder from the contents of this ObjectObjectMap, but uses the given {@link Appender} and
		 * {@link Appender} to convert each key and each value to a customizable representation and append them
		 * to a StringBuilder. To use
		 * the default String representation, you can use {@code StringBuilder::append} as an appender.
		 *
		 * @param sb a StringBuilder that this can append to
		 * @param entrySeparator how to separate entries, such as {@code ", "}
		 * @param keyValueSeparator how to separate each key from its value, such as {@code "="} or {@code ":"}
		 * @param braces true to wrap the output in curly braces, or false to omit them
		 * @param keyAppender a function that takes a StringBuilder and an Enum, and returns the modified StringBuilder
		 * @param valueAppender a function that takes a StringBuilder and a int, and returns the modified StringBuilder
		 * @return {@code sb}, with the appended keys and values of this map
		 */
	public StringBuilder appendTo (StringBuilder sb, String entrySeparator, String keyValueSeparator, boolean braces,
		Appender<Enum<?>> keyAppender, IntAppender valueAppender) {
		if (size() == 0) {
			return braces ? sb.append("{}") : sb;
		}
		if (braces) {
			sb.append('{');
		}
		Enum<?>[] universe = this.keys.universe;
		int[] valueTable = this.valueTable;
		int i = 0;
		final int len = universe.length;
		while ((i = keys.nextOrdinal(i)) != -1) {
			int v = valueTable[i];
			keyAppender.apply(sb, universe[i]);
			sb.append(keyValueSeparator);
			valueAppender.apply(sb, v);
			break;
		}
		while ((i = keys.nextOrdinal(i)) != -1) {
			int v = valueTable[i];
			sb.append(entrySeparator);
			keyAppender.apply(sb, universe[i]);
			sb.append(keyValueSeparator);
			valueAppender.apply(sb, v);
		}
		if (braces) {
			sb.append('}');
		}
		return sb;
	}

	public int replace (Enum<?> key, int value) {
		if(key != null && keys != null && keys.contains(key)) {
			int i = key.ordinal();
			if (i < valueTable.length) {
				int oldValue = valueTable[i];
				valueTable[i] = value;
				return oldValue;
			}
		}
		return defaultValue;
	}

	public int computeIfAbsent (Enum<?> key, ObjToIntFunction<? super Enum<?>> mappingFunction) {
		if(key == null) return defaultValue;
        if (keys != null && keys.universe != null && keys.contains(key)) {
            return valueTable[key.ordinal()];
        }
		int newValue = mappingFunction.applyAsInt(key);
		put(key, newValue);
		return newValue;
    }

	public boolean remove (Object key, int value) {
		if (key != null && keys != null && keys.contains(key) && valueTable[((Enum<?>)key).ordinal()] == value) {
			remove(key);
			return true;
		}
		return false;
	}

	/**
	 * Just like Map's merge() default method, but this doesn't use Java 8 APIs (so it should work on RoboVM),
	 * this uses primitive values, and this won't remove entries if the remappingFunction returns null (because
	 * that isn't possible with primitive types).
	 * This uses a functional interface from Funderby.
	 * @param key key with which the resulting value is to be associated
	 * @param value the value to be merged with the existing value
	 *        associated with the key or, if no existing value
	 *        is associated with the key, to be associated with the key
	 * @param remappingFunction given a int from this and the int {@code value}, this should return what int to use
	 * @return the value now associated with key
	 */
	public int combine (Enum<?> key, int value, IntIntToIntBiFunction remappingFunction) {
		if(key == null) return defaultValue;
		if(keys == null || keys.universe == null) {
			put(key, value);
			return value;
		}
		int next = (keys.contains(key)) ? remappingFunction.applyAsInt(valueTable[key.ordinal()], value) : value;
		put(key, next);
		return next;
	}

	/**
	 * Simply calls {@link #combine(Enum, int, IntIntToIntBiFunction)} on this map using every
	 * key-value pair in {@code other}. If {@code other} isn't empty, calling this will probably modify
	 * this map, though this depends on the {@code remappingFunction}.
	 * @param other a non-null ObjectIntMap (or subclass) with a compatible key type
	 * @param remappingFunction given a int value from this and a value from other, this should return what int to use
	 */
	public void combine (EnumIntMap other, IntIntToIntBiFunction remappingFunction) {
		for (Entry e : other.entrySet()) {
			combine(e.key, e.value, remappingFunction);
		}
	}

	/**
	 * Reuses the iterator of the reused {@link Entries} produced by {@link #entrySet()};
	 * does not permit nested iteration. Iterate over {@link Entries#Entries(EnumIntMap)} if you
	 * need nested or multithreaded iteration. You can remove an Entry from this EnumIntMap
	 * using this Iterator.
	 *
	 * @return an {@link Iterator} over {@link Map.Entry} key-value pairs; remove is supported.
	 */
	@Override
	public @NonNull EntryIterator iterator () {
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
	public @NonNull Keys keySet () {
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
	 * Returns a PrimitiveCollection of the values in the map. Remove is supported. Note that the same
	 * PrimitiveCollection instance is returned each
	 * time this method is called. Use the {@link Values} constructor for nested or multithreaded iteration.
	 *
	 * @return a {@link PrimitiveCollection} of int values
	 */
	public @NonNull Values values () {
		if (values1 == null || values2 == null) {
			values1 = new Values(this);
			values2 = new Values(this);
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
	 * @return a {@link Set} of {@link Entry} key-value pairs
	 */
	public @NonNull Entries entrySet () {
		if (entries1 == null || entries2 == null) {
			entries1 = new Entries(this);
			entries2 = new Entries(this);
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

	public static class Entry {
		@Nullable public Enum<?> key;
		public int value;

		public Entry () {
		}

		public Entry (@NonNull Entry entry) {
			this.key = entry.key;
			this.value = entry.value;
		}

		public Entry (@Nullable Enum<?> key, int value) {
			this.key = key;
			this.value = value;
		}

		@Override
		public String toString () {
			return key + "=" + value;
		}

		public Enum<?> getKey () {
			Objects.requireNonNull(key);
			return key;
		}

		public int getValue () {
			return value;
		}

		/**
		 * Sets the value of this Entry, but does <em>not</em> write through to the containing EnumIntMap.
		 * @param value the new int value to use
		 * @return the old value this held, before modification
		 */
		public int setValue (int value) {
			int old = this.value;
			this.value = value;
			return old;
		}

		@Override
		public boolean equals (@Nullable Object o) {
			if (this == o) {return true;}
			if (!(o instanceof Entry)) {return false;}

			Entry entry = (Entry)o;

			if (!Objects.equals(key, entry.getKey())) {return false;}
			return value == entry.getValue();
		}

		@Override
		public int hashCode () {
			return (key != null ? key.hashCode() : 0) ^ value;
		}
	}

	public static abstract class MapIterator {
		public boolean hasNext;

		protected final EnumIntMap map;
		protected int nextIndex, currentIndex;
		public boolean valid = true;

		public MapIterator (EnumIntMap map) {
			this.map = map;
			reset();
		}

		public void reset () {
			currentIndex = -1;
			nextIndex = -1;
			findNextIndex();
		}

		protected void findNextIndex () {
			if(map.keys == null || map.keys.universe == null) return;
			nextIndex = map.keys.nextOrdinal(nextIndex+1);
			hasNext = nextIndex != -1;
		}

		public void remove () {
			int i = currentIndex;
			if (i < 0 || map.keys == null || map.keys.universe == null) {
				throw new IllegalStateException("next must be called before remove.");
			}
			map.keys.remove(map.keys.universe[i]);
			currentIndex = -1;
		}
	}

	public static class KeyIterator extends MapIterator implements Iterable<Enum<?>>, Iterator<Enum<?>> {

		public KeyIterator (EnumIntMap map) {
			super(map);
		}

		@Override
		public @NonNull KeyIterator iterator () {
			return this;
		}

		@Override
		public boolean hasNext () {
			if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
			return hasNext;
		}

		@Override
		public Enum<?> next () {
			if (!hasNext) {throw new NoSuchElementException();}
			if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
			Enum<?> key = map.keys.universe[nextIndex];
			currentIndex = nextIndex;
			findNextIndex();
			return key;
		}
	}

	public static class ValueIterator extends MapIterator implements IntIterator {
		public ValueIterator (EnumIntMap map) {
			super(map);
		}

		/**
		 * Returns the next {@code int} element in the iteration.
		 *
		 * @return the next {@code int} element in the iteration
		 * @throws NoSuchElementException if the iteration has no more elements
		 */
		@Override
		public int nextInt () {
			if (!hasNext) {throw new NoSuchElementException();}
			if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
			int value = map.valueTable[nextIndex];
			currentIndex = nextIndex;
			findNextIndex();
			return value;
		}

		@Override
		public boolean hasNext () {
			if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
			return hasNext;
		}
	}

	public static class EntryIterator extends MapIterator implements Iterable<Entry>, Iterator<Entry> {
		protected Entry entry = new Entry();

		public EntryIterator (EnumIntMap map) {
			super(map);
		}

		@Override
		public @NonNull EntryIterator iterator () {
			return this;
		}

		/**
		 * Note the same entry instance is returned each time this method is called.
		 */
		@Override
		public Entry next () {
			if (!hasNext) {throw new NoSuchElementException();}
			if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
			entry.key = map.keys.universe[nextIndex];
			entry.value = map.valueTable[nextIndex];
			currentIndex = nextIndex;
			findNextIndex();
			return entry;
		}

		@Override
		public boolean hasNext () {
			if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
			return hasNext;
		}
	}

	public static class Entries extends AbstractSet<Entry> implements EnhancedCollection<Entry> {
		protected Entry entry = new Entry();
		protected EntryIterator iter;

		public Entries (EnumIntMap map) {
			iter = new EntryIterator(map);
		}

		@Override
		public boolean contains (Object o) {
			if(o instanceof Entry && iter.map.keys != null && iter.map.keys.universe != null) {
				Entry ent = ((Entry)o);
                Enum<?> e = ent.getKey();
                int ord = e.ordinal();
                return (ord < iter.map.keys.universe.length && iter.map.keys.universe[ord] == e
                    && iter.map.keys.contains(e) && iter.map.valueTable[ord] == ent.getValue());
            }
			return false;
		}

		@Override
		public boolean remove (Object o) {
			if(o instanceof Entry && iter.map.keys != null && iter.map.keys.universe != null) {
				Entry ent = ((Entry)o);
                Enum<?> e = ent.getKey();
                int ord = e.ordinal();
                if (ord < iter.map.keys.universe.length && iter.map.keys.universe[ord] == e
                    && iter.map.keys.contains(e) && iter.map.valueTable[ord] == ent.getValue()){
                    iter.map.keys.remove(e);
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
		public boolean removeAll (Collection<?> c) {
			iter.reset();
			boolean res = false;
			for(Object o : c) {
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
		public boolean retainAll (Collection<?> c) {
			Objects.requireNonNull(c);
			iter.reset();
			boolean modified = false;
			while (iter.hasNext) {
				Entry n = iter.next();
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
		public boolean containsAll (Collection<?> c) {
			iter.reset();
			return super.containsAll(c);
		}

		/**
		 * Returns an iterator over the elements contained in this collection.
		 *
		 * @return an iterator over the elements contained in this collection
		 */
		@Override
		public @NonNull EntryIterator iterator () {
			return iter;
		}

		@Override
		public int size () {
			return iter.map.size();
		}

		@Override
		public int hashCode () {
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
		public boolean equals (Object other) {
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
		public String toString () {
			return toString(", ", true);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void clear () {
			iter.map.clear();
			iter.reset();
		}

		/**
		 * The iterator is reused by this data structure, and you can reset it
		 * back to the start of the iteration order using this.
		 */
		public void resetIterator () {
			iter.reset();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Object @NonNull [] toArray () {
			Object[] a = new Object[iter.map.size()];
			int i = 0;
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			while (iter.hasNext) {
				a[i++] = new Entry(iter.next());
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
		@SuppressWarnings("unchecked")
        @Override
		public <T> T @NonNull [] toArray (T[] a) {
			if(a.length < iter.map.size()) a = Arrays.copyOf(a, iter.map.size());
			int i = 0;
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			while (iter.hasNext) {
				a[i++] = (T)new Entry(iter.next());
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
		public ObjectList<Entry> toList () {
			ObjectList<Entry> list = new ObjectList<>(iter.map.size());
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			while (iter.hasNext) {list.add(new Entry(iter.next()));}
			iter.currentIndex = currentIdx;
			iter.nextIndex = nextIdx;
			iter.hasNext = hn;
			return list;
		}

		/**
		 * Append the remaining items that this can iterate through into the given Collection.
		 * Does not change the position of this iterator.
		 * @param coll any modifiable Collection; may have items appended into it
		 * @return the given collection
		 */
		public Collection<Entry> appendInto(Collection<Entry> coll) {
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			while (iter.hasNext) {coll.add(new Entry(iter.next()));}
			iter.currentIndex = currentIdx;
			iter.nextIndex = nextIdx;
			iter.hasNext = hn;
			return coll;
		}

		/**
		 * Append the remaining items that this can iterate through into the given ObjectIntMap.
		 * Does not change the position of this iterator. The ObjectIntMap must have Enum keys.
		 * @param coll a modifiable ObjectIntMap; may have items appended into it
		 * @return the given ObjectIntMap
		 */
		public ObjectIntMap<Enum<?>> appendInto(ObjectIntMap<Enum<?>> coll) {
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

		/**
		 * Append the remaining items that this can iterate through into the given EnumIntMap.
		 * Does not change the position of this iterator.
		 * @param coll another EnumIntMap; may have items appended into it
		 * @return the given EnumIntMap
		 */
		public EnumIntMap appendInto(EnumIntMap coll) {
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


	public static class Values implements PrimitiveCollection.OfInt {
		protected ValueIterator iter;

		@Override
		public boolean add (int item) {
			throw new UnsupportedOperationException("ObjectIntMap.Values is read-only");
		}

		@Override
		public boolean remove (int item) {
			throw new UnsupportedOperationException("ObjectIntMap.Values is read-only");
		}

		@Override
		public boolean contains (int item) {
			return iter.map.containsValue(item);
		}

		@Override
		public void clear () {
			throw new UnsupportedOperationException("ObjectIntMap.Values is read-only");
		}

		@Override
		public ValueIterator iterator () {
			return iter;
		}

		@Override
		public int size () {
			return iter.map.size();
		}

		public Values (EnumIntMap map) {
			iter = new ValueIterator(map);
		}

		/**
		 * The iterator is reused by this data structure, and you can reset it
		 * back to the start of the iteration order using this.
		 */
		public void resetIterator () {
			iter.reset();
		}

		/**
		 * Returns a new {@link ObjectList} containing the remaining items.
		 * Does not change the position of this iterator.
		 */
		public IntList toList () {
			IntList list = new IntList(iter.map.size());
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			while (iter.hasNext) {list.add(iter.nextInt());}
			iter.currentIndex = currentIdx;
			iter.nextIndex = nextIdx;
			iter.hasNext = hn;
			return list;
		}

		/**
		 * Append the remaining items that this can iterate through into the given PrimitiveCollection.
		 * Does not change the position of this iterator.
		 * @param coll any modifiable PrimitiveCollection.OfInt; may have items appended into it
		 * @return the given collection
		 */
		public OfInt appendInto(OfInt coll) {
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			while (iter.hasNext) {coll.add(iter.nextInt());}
			iter.currentIndex = currentIdx;
			iter.nextIndex = nextIdx;
			iter.hasNext = hn;
			return coll;
		}

		@Override
		public int hashCode () {
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			iter.reset();
			int hc = iter.map.size();
			while (iter.hasNext) {
				int v = iter.nextInt();
				hc = BitConversion.imul(hc, 0x9E3779BB) ^ v;
			}
			iter.currentIndex = currentIdx;
			iter.nextIndex = nextIdx;
			iter.hasNext = hn;
			return hc;
		}

		@Override
		public boolean equals (Object other) {
			if(other instanceof PrimitiveCollection.OfInt) {
				boolean res = iter.map.size() == ((OfInt) other).size();
				if(res) {
					IntIterator otter = ((OfInt) other).iterator();
					int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
					boolean hn = iter.hasNext;
					iter.reset();

					while (iter.hasNext && otter.hasNext()) {
						if (iter.nextInt() != otter.nextInt()) {
							res = false;
							break;
						}
					}
					res &= iter.hasNext == otter.hasNext();

					iter.currentIndex = currentIdx;
					iter.nextIndex = nextIdx;
					iter.hasNext = hn;
				}
				return res;
			}
			return false;
		}

		@Override
		public String toString () {
			return toString(", ", true);
		}


	}

	public static class Keys extends EnumSet {
		protected KeyIterator iter;

		public Keys (EnumIntMap map) {
			super();
			iter = new KeyIterator(map);
			if(map.keys == null) return;

			EnumSet other = map.keys;
			this.size = other.size;
			if(other.table != null)
				this.table = other.table;
			this.universe = other.universe;

		}

		@Override
		public boolean add(@NonNull Enum<?> item) {
			throw new UnsupportedOperationException("Keys cannot have items added.");
		}

		@Override
		public boolean addAll(@NonNull Collection<? extends Enum<?>> c) {
			throw new UnsupportedOperationException("Keys cannot have items added.");
		}

		@Override
		public boolean addAll(Enum<?> @NonNull [] c) {
			throw new UnsupportedOperationException("Keys cannot have items added.");
		}

		@Override
		public boolean contains (Object o) {
			return super.contains(o);
		}

		@Override
		public boolean remove (Object o) {
			return super.remove(o);
		}

		@Override
		public boolean removeAll (Collection<?> c) {
			return super.removeAll(c);
		}

		/**
		 * Returns an iterator over the elements contained in this collection.
		 *
		 * @return an iterator over the elements contained in this collection
		 */
		@Override
		public @NonNull Iterator<Enum<?>> iterator () {
			return iter;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void clear () {
			super.clear();
		}

        @Override
		public int size () {
			return super.size();
		}

		@Override
		public boolean equals (Object other) {
			return super.equals(other);
		}

		@Override
		public int hashCode () {
			return super.hashCode();
		}

		/**
		 * The iterator is reused by this data structure, and you can reset it
		 * back to the start of the iteration order using this.
		 */
		public void resetIterator () {
			iter.reset();
		}
		/**
		 * Returns a new {@link ObjectList} containing the remaining items.
		 * Does not change the position of this iterator.
		 * @return a new ObjectList containing the remaining items
		 */
		public ObjectList<Enum<?>> toList () {
			ObjectList<Enum<?>> list = new ObjectList<>(iter.map.size());
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			while (iter.hasNext) {list.add(iter.next());}
			iter.currentIndex = currentIdx;
			iter.nextIndex = nextIdx;
			iter.hasNext = hn;
			return list;
		}

		/**
		 * Returns a new {@link EnumSet} containing the remaining items.
		 * Does not change the position of this iterator.
		 * The EnumSet this returns will share a key universe with the map linked to this key set.
		 * @return a new EnumSet containing the remaining items, sharing a universe with this key set
		 */
		public EnumSet toEnumSet() {
			EnumSet es = new EnumSet(super.universe, true);
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			while (iter.hasNext) {es.add(iter.next());}
			iter.currentIndex = currentIdx;
			iter.nextIndex = nextIdx;
			iter.hasNext = hn;
			return es;
		}

		/**
		 * Append the remaining items that this can iterate through into the given Collection.
		 * Does not change the position of this iterator.
		 * @param coll any modifiable Collection; may have items appended into it
		 * @return the given collection, potentially after modifications
		 */
		public Collection<Enum<?>> appendInto(Collection<Enum<?>> coll) {
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			while (iter.hasNext) {coll.add(iter.next());}
			iter.currentIndex = currentIdx;
			iter.nextIndex = nextIdx;
			iter.hasNext = hn;
			return coll;
		}
	}

	/**
	 * Constructs an empty map.
	 * This is usually less useful than just using the constructor, but can be handy
	 * in some code-generation scenarios when you don't know how many arguments you will have.
	 *
	 * @return a new map containing nothing
	 */
	public static EnumIntMap with () {
		return new EnumIntMap();
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
	public static EnumIntMap with (Enum<?> key0, Number value0) {
		EnumIntMap map = new EnumIntMap();
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
	public static EnumIntMap with (Enum<?> key0, Number value0, Enum<?> key1, Number value1) {
		EnumIntMap map = new EnumIntMap();
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
	public static EnumIntMap with (Enum<?> key0, Number value0, Enum<?> key1, Number value1, Enum<?> key2, Number value2) {
		EnumIntMap map = new EnumIntMap();
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
	public static EnumIntMap with (Enum<?> key0, Number value0, Enum<?> key1, Number value1, Enum<?> key2, Number value2, Enum<?> key3, Number value3) {
		EnumIntMap map = new EnumIntMap();
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
	 * {@link #EnumIntMap(Enum[], int[])}, which takes all keys and then all values.
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
	public static EnumIntMap with (Enum<?> key0, Number value0, Object... rest) {
		EnumIntMap map = new EnumIntMap();
		map.put(key0, value0.intValue());
		map.putPairs(rest);
		return map;
	}

	/**
	 * Attempts to put alternating key-value pairs into this map, drawing a key, then a value from {@code pairs}, then
	 * another key, another value, and so on until another pair cannot be drawn.  All values must be some type of boxed
	 * Number, such as {@link Integer} or {@link Double}, and will be converted to primitive {@code int}s. Any keys
	 * that don't have some Enum as their type or values that aren't {@code Number}s have that entry skipped.
	 * <br>
	 * If any item in {@code pairs} cannot be cast to the Enum or Number type for its position in the
	 * arguments, that pair is ignored and neither that key nor value is put into the map. If any key is null, that pair
	 * is ignored, as well. If {@code pairs} is an Object array that is null, the entire call to putPairs() is ignored.
	 * If the length of {@code pairs} is odd, the last item (which will be unpaired) is ignored.
	 *
	 * @param pairs an array or varargs of alternating Enum, Number, Enum, Number... elements
	 */
	public void putPairs(Object... pairs) {
		if(pairs != null) {
			for (int i = 1; i < pairs.length; i += 2) {
				try {
					if(pairs[i-1] != null && pairs[i] != null)
						put((Enum<?>) pairs[i - 1], ((Number)pairs[i]).intValue());
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
	 * A PartialParser will be used to parse keys from sections of {@code str}, and values are parsed with
	 * {@link Base#readInt(CharSequence, int, int)}. Usually, keyParser is produced by
	 * {@link PartialParser#enumParser(ObjToObjFunction)}. Any brackets inside the given range
	 * of characters will ruin the parsing, so increase offset by 1 and
	 * reduce length by 2 if the original String had brackets added to it.
	 * @param str a String containing parseable text
	 * @param keyParser a PartialParser that returns a {@code Enum<?>} key from a section of {@code str}, typically
	 *                     produced by {@link PartialParser#enumParser(ObjToObjFunction)}
	 */
	public void putLegible(String str, PartialParser<Enum<?>> keyParser) {
		putLegible(str, ", ", "=", keyParser, 0, -1);
	}

	/**
	 * Adds items to this map drawn from the result of {@link #toString(String)} or
	 * {@link #appendTo(StringBuilder, String, boolean)}. Every key-value pair should be separated by
	 * {@code entrySeparator}, and every key should be followed by "=" before the value (which
	 * {@link #toString(String)} does).
	 * A PartialParser will be used to parse keys from sections of {@code str}, and values are parsed with
	 * {@link Base#readInt(CharSequence, int, int)}. Usually, keyParser is produced by
	 * {@link PartialParser#enumParser(ObjToObjFunction)}. Any brackets inside the given range
	 * of characters will ruin the parsing, so increase offset by 1 and
	 * reduce length by 2 if the original String had brackets added to it.
	 * @param str a String containing parseable text
	 * @param entrySeparator the String separating every key-value pair
	 * @param keyParser a PartialParser that returns a {@code Enum<?>} key from a section of {@code str}, typically
	 *                     produced by {@link PartialParser#enumParser(ObjToObjFunction)}
	 */
	public void putLegible(String str, String entrySeparator, PartialParser<Enum<?>> keyParser) {
		putLegible(str, entrySeparator, "=", keyParser, 0, -1);
	}

	/**
	 * Adds items to this map drawn from the result of {@link #toString(String)} or
	 * {@link #appendTo(StringBuilder, String, String, boolean, Appender, IntAppender)}.
	 * A PartialParser will be used to parse keys from sections of {@code str}, and values are parsed with
	 * {@link Base#readInt(CharSequence, int, int)}. Usually, keyParser is produced by
	 * {@link PartialParser#enumParser(ObjToObjFunction)}. Any brackets
	 * inside the given range of characters will ruin the parsing, so increase offset by 1 and
	 * reduce length by 2 if the original String had brackets added to it.
	 * @param str a String containing parseable text
	 * @param entrySeparator the String separating every key-value pair
	 * @param keyValueSeparator the String separating every key from its corresponding value
	 * @param keyParser a PartialParser that returns a {@code Enum<?>} key from a section of {@code str}, typically
	 *                     produced by {@link PartialParser#enumParser(ObjToObjFunction)}
	 */
	public void putLegible(String str, String entrySeparator, String keyValueSeparator, PartialParser<Enum<?>> keyParser) {
		putLegible(str, entrySeparator, keyValueSeparator, keyParser, 0, -1);
	}

	/**
	 * Puts key-value pairs into this map drawn from the result of {@link #toString(String)} or
	 * {@link #appendTo(StringBuilder, String, String, boolean, Appender, IntAppender)}.
	 * A PartialParser will be used to parse keys from sections of {@code str}, and values are parsed with
	 * {@link Base#readInt(CharSequence, int, int)}. Usually, keyParser is produced by
	 * {@link PartialParser#enumParser(ObjToObjFunction)}. Any brackets
	 * inside the given range of characters will ruin the parsing, so increase offset by 1 and
	 * reduce length by 2 if the original String had brackets added to it.
	 * @param str a String containing parseable text
	 * @param entrySeparator the String separating every key-value pair
	 * @param keyValueSeparator the String separating every key from its corresponding value
	 * @param keyParser a PartialParser that returns a {@code Enum<?>} key from a section of {@code str}, typically
	 *                     produced by {@link PartialParser#enumParser(ObjToObjFunction)}
	 * @param offset the first position to read parseable text from in {@code str}
	 * @param length how many chars to read; -1 is treated as maximum length
	 */
	public void putLegible(String str, String entrySeparator, String keyValueSeparator, PartialParser<Enum<?>> keyParser, int offset, int length) {
		int sl, el, kvl;
		if(str == null || entrySeparator == null || keyValueSeparator == null || keyParser == null
				|| (sl = str.length()) < 1 || (el = entrySeparator.length()) < 1 || (kvl = keyValueSeparator.length()) < 1
				|| offset < 0 || offset > sl - 1) return;
		final int lim = length < 0 ? sl : Math.min(offset + length, sl);
		int end = str.indexOf(keyValueSeparator, offset+1);
		@Nullable Enum<?> k = null;
		boolean incomplete = false;
		while (end != -1 && end + kvl < lim) {
			k = keyParser.parse(str, offset, end);
			offset = end + kvl;
			end = str.indexOf(entrySeparator, offset+1);
			if(end != -1 && end + el < lim){
				put(k, Base.BASE10.readInt(str, offset, end));
				offset = end + el;
				end = str.indexOf(keyValueSeparator, offset+1);
			} else {
				incomplete = true;
			}
		}
		if(incomplete && offset < lim){
			put(k, Base.BASE10.readInt(str, offset, lim));
		}
	}

	/**
	 * Constructs an empty map.
	 * This is usually less useful than just using the constructor, but can be handy
	 * in some code-generation scenarios when you don't know how many arguments you will have.
	 *
	 * @return a new map containing nothing
	 */
	public static EnumIntMap withPrimitive () {
		return new EnumIntMap();
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
	public static EnumIntMap withPrimitive (Enum<?> key0, int value0) {
		EnumIntMap map = new EnumIntMap();
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
	public static EnumIntMap withPrimitive (Enum<?> key0, int value0, Enum<?> key1, int value1) {
		EnumIntMap map = new EnumIntMap();
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
	public static EnumIntMap withPrimitive (Enum<?> key0, int value0, Enum<?> key1, int value1, Enum<?> key2, int value2) {
		EnumIntMap map = new EnumIntMap();
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
	public static EnumIntMap withPrimitive (Enum<?> key0, int value0, Enum<?> key1, int value1, Enum<?> key2, int value2, Enum<?> key3, int value3) {
		EnumIntMap map = new EnumIntMap();
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
	public static EnumIntMap of () {
		return with();
	}

	/**
	 * Constructs a map given alternating keys and values.
	 * This can be useful in some code-generation scenarios, or when you want to make a
	 * map conveniently by-hand and have it populated at the start. You can also use
	 * {@link #EnumIntMap(Enum[], int[])}, which takes all keys and then all values.
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
	public static EnumIntMap of (Enum<?> key0, Number value0, Object... rest) {
		return with(key0, value0, rest);
	}
}

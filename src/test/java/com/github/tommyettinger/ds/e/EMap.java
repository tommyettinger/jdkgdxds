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

package com.github.tommyettinger.ds.e;

import com.github.tommyettinger.digital.BitConversion;
import com.github.tommyettinger.ds.ObjectList;
import com.github.tommyettinger.ds.ObjectObjectOrderedMap;
import com.github.tommyettinger.ds.Ordered;
import com.github.tommyettinger.ds.Utilities;
import com.github.tommyettinger.function.ObjObjToObjBiFunction;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

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
import static com.github.tommyettinger.ds.Utilities.tableSize;

/**
 * An unordered map where the keys and values are objects. Null keys are not allowed. No allocation is done except when growing
 * the table size.
 * <p>
 * This class performs fast contains and remove (typically O(1), worst case O(n) but that is rare in practice). Add may be
 * slightly slower, depending on hash collisions. Hashcodes are rehashed to reduce collisions and the need to resize. Load factors
 * greater than 0.91 greatly increase the chances to resize to the next higher POT size.
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
public class EMap<V> implements Map<Enum<?>, V>, Iterable<Map.Entry<Enum<?>, V>> {

	protected int size;

	protected Enum<?>[] universe;

	protected Object[] valueTable;

	@Nullable protected transient Entries<V> entries1;
	@Nullable protected transient Entries<V> entries2;
	@Nullable protected transient Values<V> values1;
	@Nullable protected transient Values<V> values2;
	@Nullable protected transient Keys keys1;
	@Nullable protected transient Keys keys2;

	/**
	 * Returned by {@link #get(Object)} when no value exists for the given key, as well as some other methods to indicate that
	 * no value in the Map could be returned.
	 */
	@Nullable public V defaultValue = null;

	/**
	 * Empty constructor; using this will postpone allocating any internal arrays until {@link #put} is first called
	 * (potentially indirectly).
	 */
	public EMap () {
	}

	/**
	 * Initializes this map so that it has exactly enough capacity as needed to contain each Enum constant defined in
	 * {@code universe}, assuming universe stores every possible constant in one Enum type. This map will start empty.
	 * You almost always obtain universe from calling {@code values()} on an Enum type, and you can share one
	 * reference to one Enum array across many EMap instances if you don't modify the shared array. Sharing the same
	 * universe helps save some memory if you have (very) many EMap instances.
	 * @param universe almost always, the result of calling {@code values()} on an Enum type; used directly, not copied
	 */
	public EMap (@Nullable Enum<?>[] universe) {
		super();
		if(universe == null) return;
		this.universe = universe;
		valueTable = new Object[universe.length];
	}

	/**
	 * Initializes this set so that it has exactly enough capacity as needed to contain each Enum constant defined by the
	 * Class {@code universeClass}, assuming universeClass is non-null. This simply calls {@link #EMap(Enum[])}
	 * for convenience. Note that this constructor allocates a new array of Enum constants each time it is called, where
	 * if you use {@link #EMap(Enum[])}, you can reuse an unmodified array to reduce allocations.
	 * @param universeClass the Class of an Enum type that defines the universe of valid Enum items this can hold
	 */
	public EMap(@Nullable Class<? extends Enum<?>> universeClass) {
		this(universeClass == null ? null : universeClass.getEnumConstants());
	}

	/**
	 * Creates a new map identical to the specified map.
	 *
	 * @param map an ObjectObjectMap to copy
	 */
	public EMap (EMap<? extends V> map) {
		universe = map.universe;
		valueTable = Arrays.copyOf(map.valueTable, map.valueTable.length);
		size = map.size;
		defaultValue = map.defaultValue;
	}

	/**
	 * Creates a new map identical to the specified map.
	 *
	 * @param map a Map to copy; ObjectObjectMap or its subclasses will be faster
	 */
	public EMap (Map<? extends Enum<?>, ? extends V> map) {
		this();
		putAll(map);
	}

	/**
	 * Given two side-by-side arrays, one of keys, one of values, this constructs a map and inserts each pair of key and value into it.
	 * If keys and values have different lengths, this only uses the length of the smaller array.
	 *
	 * @param keys   an array of keys
	 * @param values an array of values
	 */
	public EMap (Enum<?>[] keys, V[] values) {
		this();
		putAll(keys, values);
	}

	/**
	 * Given two side-by-side collections, one of keys, one of values, this constructs a map and inserts each pair of key and value into it.
	 * If keys and values have different lengths, this only uses the length of the smaller collection.
	 *
	 * @param keys   a Collection of keys
	 * @param values a Collection of values
	 */
	public EMap (Collection<? extends Enum<?>> keys, Collection<? extends V> values) {
		this();
		putAll(keys, values);
	}

	protected Object hold(Object o){
		return o == null ? neverIdentical : o;
	}

	@SuppressWarnings("unchecked")
	protected V release(Object o) {
		if(o == neverIdentical)
			return null;
		return (V) o;
	}

	/**
	 * Given two side-by-side collections, one of keys, one of values, this inserts each pair of key and value into this map with put().
	 *
	 * @param keys   a Collection of keys
	 * @param values a Collection of values
	 */
	public void putAll (Collection<? extends Enum<?>> keys, Collection<? extends V> values) {
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
	 * Returns the old value associated with the specified key, or this map's {@link #defaultValue} if there was no prior value.
	 */
	@Override
	@Nullable
	public V put (Enum<?> key, @Nullable V value) {
		if(key == null) throw new NullPointerException("Keys added to an EMap must not be null.");
		if(universe == null) universe = key.getDeclaringClass().getEnumConstants();
		if(valueTable == null) valueTable = new Object[universe.length];
		int i = key.ordinal();
		if(i >= valueTable.length) return defaultValue;
		Object oldValue = valueTable[i];
		valueTable[i] = hold(value);
		if (oldValue != null) {
			// Existing key was found.
			return release(oldValue);
		}
		++size;
		return defaultValue;
	}

	@Nullable
	public V putOrDefault (Enum<?> key, @Nullable V value, @Nullable V defaultValue) {
		if(key == null) throw new NullPointerException("Keys added to an EMap must not be null.");
		if(universe == null) universe = key.getDeclaringClass().getEnumConstants();
		if(valueTable == null) valueTable = new Object[universe.length];
		int i = key.ordinal();
		if(i >= valueTable.length) return defaultValue;
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
	 * overwriting the previous values if two keys are identical.
	 *
	 * @param map a map with compatible key and value types; will not be modified
	 */
	public void putAll (@NonNull EMap<? extends V> map) {
		if(map.size == 0) return;
		if(universe == null) universe = map.universe;
		if(valueTable == null) valueTable = new Object[universe.length];
		Object[] valueTable = map.valueTable;
		Object value;
		for (int i = 0, n = valueTable.length; i < n; i++) {
			value = valueTable[i];
			if (value != null) {
				if(this.valueTable[i] == null) ++size;
				this.valueTable[i] = value;
			}
		}
	}

	/**
	 * Given two side-by-side arrays, one of keys, one of values, this inserts each pair of key and value into this map with put().
	 *
	 * @param keys   an array of keys
	 * @param values an array of values
	 */
	public void putAll (Enum<?>[] keys, V[] values) {
		putAll(keys, 0, values, 0, Math.min(keys.length, values.length));
	}

	/**
	 * Given two side-by-side arrays, one of keys, one of values, this inserts each pair of key and value into this map with put().
	 *
	 * @param keys   an array of keys
	 * @param values an array of values
	 * @param length how many items from keys and values to insert, at-most
	 */
	public void putAll (Enum<?>[] keys, V[] values, int length) {
		putAll(keys, 0, values, 0, length);
	}

	/**
	 * Given two side-by-side arrays, one of keys, one of values, this inserts each pair of key and value into this map with put().
	 *
	 * @param keys        an array of keys
	 * @param keyOffset   the first index in keys to insert
	 * @param values      an array of values
	 * @param valueOffset the first index in values to insert
	 * @param length      how many items from keys and values to insert, at-most
	 */
	public void putAll (Enum<?>[] keys, int keyOffset, V[] values, int valueOffset, int length) {
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
	 * Returns the value for the specified key, or {@link #defaultValue} if the key is not in the map.
	 * Note that {@link #defaultValue} is often null, which is also a valid value that can be assigned to a
	 * legitimate key. Checking that the result of this method is null does not guarantee that the
	 * {@code key} is not present.
	 *
	 * @param key a non-null Object that should almost always be a {@code K} (or an instance of a subclass of {@code K})
	 */
	@Override
	@Nullable
	public V get (Object key) {
		if(size == 0 || !(key instanceof Enum<?>))
			return defaultValue;
		Enum<?> e = (Enum<?>)key;
		Object o = valueTable[e.ordinal()];
		return o == null ? defaultValue : release(o);
	}

	/**
	 * Returns the value for the specified key, or the given default value if the key is not in the map.
	 */
	@Override
	@Nullable
	public V getOrDefault (Object key, @Nullable V defaultValue) {
		if(size == 0 || !(key instanceof Enum<?>))
			return defaultValue;
		Enum<?> e = (Enum<?>)key;
		Object o = valueTable[e.ordinal()];
		return o == null ? defaultValue : release(o);
	}

	@Override
	@Nullable
	public V remove (Object key) {
		if(size == 0 || !(key instanceof Enum<?>))
			return defaultValue;
		Enum<?> e = (Enum<?>)key;
		Object o = valueTable[e.ordinal()];
		valueTable[e.ordinal()] = null;
		if(o == null) return defaultValue;
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
	 * Note that {@link #putAll(EMap)} is more specific and can be
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
	public void putAll (Map<? extends Enum<?>, ? extends V> m) {
		for (Map.Entry<? extends Enum<?>, ? extends V> kv : m.entrySet()) {put(kv.getKey(), kv.getValue());}
	}

	/**
	 * Returns true if the map has one or more items.
	 */
	public boolean notEmpty () {
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
	public int size () {
		return size;
	}

	/**
	 * Returns true if the map is empty.
	 */
	@Override
	public boolean isEmpty () {
		return size == 0;
	}

	/**
	 * Gets the default value, a {@code V} which is returned by {@link #get(Object)} if the key is not found.
	 * If not changed, the default value is null.
	 *
	 * @return the current default value
	 */
	@Nullable
	public V getDefaultValue () {
		return defaultValue;
	}

	/**
	 * Sets the default value, a {@code V} which is returned by {@link #get(Object)} if the key is not found.
	 * If not changed, the default value is null. Note that {@link #getOrDefault(Object, Object)} is also available,
	 * which allows specifying a "not-found" value per-call.
	 *
	 * @param defaultValue may be any V object or null; should usually be one that doesn't occur as a typical value
	 */
	public void setDefaultValue (@Nullable V defaultValue) {
		this.defaultValue = defaultValue;
	}

	/**
	 * Removes all the elements from this map.
	 * The map will be empty after this call returns.
	 * This does not change the universe of possible Enum items this can hold.
	 */
	@Override
	public void clear () {
		size = 0;
		if(valueTable != null)
			Utilities.clear(valueTable);
	}

	/**
	 * Removes all the elements from this map and can reset the universe of possible Enum items this can hold.
	 * The map will be empty after this call returns.
	 * This changes the universe of possible Enum items this can hold to match {@code universe}.
	 * If {@code universe} is null, this resets this map to the state it would have after {@link #EMap()} was called.
	 * If the table this would need is the same size as or smaller than the current table (such as if {@code universe} is the same as
	 * the universe here), this will not allocate, but will still clear any items this holds and will set the universe to the given one.
	 * Otherwise, this allocates and uses a new table of a larger size, with nothing in it, and uses the given universe.
	 * This always uses {@code universe} directly, without copying.
	 * <br>
	 * This can be useful to allow an EMap that was created with {@link #EMap()} to share a universe with other EMaps.
	 *
	 * @param universe the universe of possible Enum items this can hold; almost always produced by {@code values()} on an Enum
	 */
	public void clearToUniverse (Enum<?>@Nullable [] universe) {
		size = 0;
		if (universe == null) {
			valueTable = null;
		} else if(universe.length <= this.universe.length) {
			if(valueTable != null)
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
	 * If {@code universe} is null, this resets this map to the state it would have after {@link #EMap()} was called.
	 * If the table this would need is the same size as or smaller than the current table (such as if {@code universe} is the same as
	 * the universe here), this will not allocate, but will still clear any items this holds and will set the universe to the given one.
	 * Otherwise, this allocates and uses a new table of a larger size, with nothing in it, and uses the given universe.
	 * This calls {@link Class#getEnumConstants()} if universe is non-null, which allocates a new array.
	 * <br>
	 * You may want to prefer calling {@link #clearToUniverse(Enum[])} (the overload that takes an array), because it can be used to
	 * share one universe array between many EMap instances. This overload, given a Class, has to call {@link Class#getEnumConstants()}
	 * and thus allocate a new array each time this is called.
	 *
	 * @param universe the Class of an Enum type that stores the universe of possible Enum items this can hold
	 */
	public void clearToUniverse (@Nullable Class<? extends Enum<?>> universe) {
		size = 0;
		if (universe == null) {
			valueTable = null;
			this.universe = null;
		} else {
			Enum<?>[] cons = universe.getEnumConstants();
			if(cons.length <= this.universe.length) {
				if(valueTable != null)
					Utilities.clear(valueTable);
			} else {
				valueTable = new Object[cons.length];
			}
			this.universe = cons;
		}
	}

	/**
	 * Returns true if the specified value is in the map. Note this traverses the entire map and compares every value, which may
	 * be an expensive operation.
	 *
	 * @param identity If true, uses == to compare the specified value with values in the map. If false, uses
	 *                 {@link #equals(Object)}.
	 */
	public boolean containsValue (@Nullable Object value, boolean identity) {
		Object[] valueTable = this.valueTable;
		Object held = hold(value);
		if (identity) {
			for (int i = valueTable.length - 1; i >= 0; i--) {if (valueTable[i] == held) {return true;}}
		} else {
			for (int i = valueTable.length - 1; i >= 0; i--) {if (held.equals(valueTable[i])) {return true;}}
		}
		return false;
	}

	@Override
	public boolean containsKey (Object key) {
		if(size == 0 || !(key instanceof Enum<?>))
			return false;
		Enum<?> e = (Enum<?>)key;
		return e.ordinal() < universe.length && valueTable[e.ordinal()] != null;
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
	public boolean containsValue (Object value) {
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
	public Enum<?> findKey (@Nullable Object value, boolean identity) {
		Object[] valueTable = this.valueTable;
		Object held = hold(value);
		if (identity) {
			for (int i = valueTable.length - 1; i >= 0; i--) {if (valueTable[i] == held) {return universe[i];}}
		} else {
			for (int i = valueTable.length - 1; i >= 0; i--) {if (held.equals(valueTable[i])) {return universe[i];}}
		}
		return null;
	}

	@Override
	public int hashCode () {
		int h = size;
		Object keyTable = this.keyTable;
		V[] valueTable = this.valueTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			Enum<?> key = keyTable[i];
			if (key != null) {
				h ^= key.hashCode();
				V value = valueTable[i];
				if (value != null) {h ^= value.hashCode();}
			}
		}
		return h;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	@Override
	public boolean equals (Object obj) {
		if (obj == this) {return true;}
		if (!(obj instanceof Map)) {return false;}
		Map other = (Map)obj;
		if (other.size() != size) {return false;}
		Object[] keyTable = this.keyTable;
		V[] valueTable = this.valueTable;
		try {
			for (int i = 0, n = keyTable.length; i < n; i++) {
				Object key = keyTable[i];
				if (key != null) {
					V value = valueTable[i];
					if (value == null) {
						if (other.getOrDefault(key, neverIdentical) != null) {return false;}
					} else {
						if (!value.equals(other.get(key))) {return false;}
					}
				}
			}
		}catch (ClassCastException | NullPointerException unused) {
			return false;
		}

		return true;
	}

	/**
	 * Uses == for comparison of each value.
	 */
	public boolean equalsIdentity (@Nullable Object obj) {
		if (obj == this) {return true;}
		if (!(obj instanceof EMap)) {return false;}
		EMap other = (EMap)obj;
		if (other.size != size) {return false;}
		Enum<?>[] keyTable = this.keyTable;
		V[] valueTable = this.valueTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			Enum<?> key = keyTable[i];
			if (key != null && valueTable[i] != other.getOrDefault(key, neverIdentical)) {return false;}
		}
		return true;
	}

	public String toString (String separator) {
		return toString(separator, false);
	}

	@Override
	public String toString () {
		return toString(", ", true);
	}

	protected String toString (String separator, boolean braces) {
		if (size == 0) {return braces ? "{}" : "";}
		StringBuilder buffer = new StringBuilder(32);
		if (braces) {buffer.append('{');}
		Enum<?>[] keyTable = this.keyTable;
		V[] valueTable = this.valueTable;
		int i = keyTable.length;
		while (i-- > 0) {
			Enum<?> key = keyTable[i];
			if (key == null) {continue;}
			buffer.append(key == this ? "(this)" : key);
			buffer.append('=');
			V value = valueTable[i];
			buffer.append(value == this ? "(this)" : value);
			break;
		}
		while (i-- > 0) {
			Enum<?> key = keyTable[i];
			if (key == null) {continue;}
			buffer.append(separator);
			buffer.append(key == this ? "(this)" : key);
			buffer.append('=');
			V value = valueTable[i];
			buffer.append(value == this ? "(this)" : value);
		}
		if (braces) {buffer.append('}');}
		return buffer.toString();
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
	public void truncate (int newSize) {
		Object keyTable = this.keyTable;
		V[] valTable = this.valueTable;
		newSize = Math.max(0, newSize);
		for (int i = keyTable.length - 1; i >= 0 && size > newSize; i--) {
			if (keyTable[i] != null) {
				keyTable[i] = null;
				valTable[i] = null;
				--size;
			}
		}
	}

	@Override
	@Nullable
	public V replace (Enum<?> key, V value) {
		int i = locateKey(key);
		if (i >= 0) {
			V oldValue = valueTable[i];
			valueTable[i] = value;
			return oldValue;
		}
		return defaultValue;
	}

	/**
	 * Just like Map's merge() default method, but this doesn't use Java 8 APIs (so it should work on RoboVM), and this
	 * won't remove entries if the remappingFunction returns null (in that case, it will call {@code put(key, null)}).
	 * This also uses a functional interface from Funderby instead of the JDK, for RoboVM support.
	 * @param key key with which the resulting value is to be associated
	 * @param value the value to be merged with the existing value
	 *        associated with the key or, if no existing value
	 *        is associated with the key, to be associated with the key
	 * @param remappingFunction given a V from this and the V {@code value}, this should return what V to use
	 * @return the value now associated with key
	 */
	@Nullable
	public V combine (Object key, V value, ObjObjToObjBiFunction<? super V, ? super V, ? extends V> remappingFunction) {
		int i = locateKey(key);
		V next = (i < 0) ? value : remappingFunction.apply(valueTable[i], value);
		put(key, next);
		return next;
	}

	/**
	 * Simply calls {@link #combine(Object, Object, ObjObjToObjBiFunction)} on this map using every
	 * key-value pair in {@code other}. If {@code other} isn't empty, calling this will probably modify
	 * this map, though this depends on the {@code remappingFunction}.
	 * @param other a non-null Map (or subclass) with compatible key and value types
	 * @param remappingFunction given a V value from this and a value from other, this should return what V to use
	 */
	public void combine (Map<? extends Enum<?>, ? extends V> other, ObjObjToObjBiFunction<? super V, ? super V, ? extends V> remappingFunction) {
		for (Map.Entry<? extends Object, ? extends V> e : other.entrySet()) {
			combine(e.getKey(), e.getValue(), remappingFunction);
		}
	}

	/**
	 * Reuses the iterator of the reused {@link Entries} produced by {@link #entrySet()};
	 * does not permit nested iteration. Iterate over {@link Entries#Entries(EMap)} if you
	 * need nested or multithreaded iteration. You can remove an Entry from this ObjectObjectMap
	 * using this Iterator.
	 *
	 * @return an {@link Iterator} over {@link Map.Entry} key-value pairs; remove is supported.
	 */
	@Override
	public @NonNull MapIterator<Object, V, Map.Entry<Object, V>> iterator () {
		return entrySet().iterator();
	}

	/**
	 * Returns a {@link Set} view of the keys contained in this map.
	 * The set is backed by the map, so changes to the map are
	 * reflected in the set, and vice-versa.  If the map is modified
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
	public @NonNull Keys<Object, V> keySet () {
		if (keys1 == null || keys2 == null) {
			keys1 = new Keys<>(this);
			keys2 = new Keys<>(this);
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
	public @NonNull Values<Object, V> values () {
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
	public @NonNull Entries<Object, V> entrySet () {
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

	public static class Entry<Enum<?>, V> implements Map.Entry<Enum<?>, V> {
		@Nullable public Enum<?> key;
		@Nullable public V value;

		public Entry () {
		}

		public Entry (@Nullable Enum<?> key, @Nullable V value) {
			this.key = key;
			this.value = value;
		}

		public Entry (Map.Entry<? extends Enum<?>, ? extends V> entry) {
			key = entry.getKey();
			value = entry.getValue();
		}

		@Override
		@Nullable
		public String toString () {
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
		public Enum<?> getKey () {
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
		public V getValue () {
			return value;
		}

		/**
		 * Replaces the value corresponding to this entry with the specified
		 * value (optional operation).  (Writes through to the map.)  The
		 * behavior of this call is undefined if the mapping has already been
		 * removed from the map (by the iterator's {@code remove} operation).
		 *
		 * @param value new value to be stored in this entry
		 * @return old value corresponding to the entry
		 * @throws UnsupportedOperationException if the {@code put} operation
		 *                                       is not supported by the backing map
		 * @throws ClassCastException            if the class of the specified value
		 *                                       prevents it from being stored in the backing map
		 * @throws NullPointerException          if the backing map does not permit
		 *                                       null values, and the specified value is null
		 * @throws IllegalArgumentException      if some property of this value
		 *                                       prevents it from being stored in the backing map
		 * @throws IllegalStateException         implementations may, but are not
		 *                                       required to, throw this exception if the entry has been
		 *                                       removed from the backing map.
		 */
		@Override
		@Nullable
		public V setValue (V value) {
			V old = this.value;
			this.value = value;
			return old;
		}

		@Override
		public boolean equals (@Nullable Object o) {
			if (this == o) {return true;}
			if (o == null || getClass() != o.getClass()) {return false;}

			Entry<?, ?> entry = (Entry<?, ?>)o;

			if (!Objects.equals(key, entry.key)) {return false;}
			return Objects.equals(value, entry.value);
		}

		@Override
		public int hashCode () {
			int result = key != null ? key.hashCode() : 0;
			result = 31 * result + (value != null ? value.hashCode() : 0);
			return result;
		}
	}

	public static abstract class MapIterator<Enum<?>, V, I> implements Iterable<I>, Iterator<I> {
		public boolean hasNext;

		protected final EMap<V> map;
		protected int nextIndex, currentIndex;
		public boolean valid = true;

		public MapIterator (EMap<V> map) {
			this.map = map;
			reset();
		}

		public void reset () {
			currentIndex = -1;
			nextIndex = -1;
			findNextIndex();
		}

		protected void findNextIndex () {
			Enum<?>[] keyTable = map.keyTable;
			for (int n = keyTable.length; ++nextIndex < n; ) {
				if (keyTable[nextIndex] != null) {
					hasNext = true;
					return;
				}
			}
			hasNext = false;
		}

		@Override
		public void remove () {
			int i = currentIndex;
			if (i < 0) {throw new IllegalStateException("next must be called before remove.");}
			Enum<?>[] keyTable = map.keyTable;
			V[] valueTable = map.valueTable;
			int mask = map.mask, next = i + 1 & mask;
			Enum<?> key;
			while ((key = keyTable[next]) != null) {
				int placement = map.place(key);
				if ((next - placement & mask) > (i - placement & mask)) {
					keyTable[i] = key;
					valueTable[i] = valueTable[next];
					i = next;
				}
				next = next + 1 & mask;
			}
			keyTable[i] = null;
			valueTable[i] = null;
			map.size--;
			if (i != currentIndex) {--nextIndex;}
			currentIndex = -1;
		}
	}

	public static class Entries<Enum<?>, V> extends AbstractSet<Map.Entry<Enum<?>, V>> {
		protected Entry<Enum<?>, V> entry = new Entry<>();
		protected MapIterator<Enum<?>, V, Map.Entry<Enum<?>, V>> iter;

		public Entries (EMap<V> map) {
			iter = new MapIterator<Enum<?>, V, Map.Entry<Enum<?>, V>>(map) {
				@Override
				public @NonNull MapIterator<Enum<?>, V, Map.Entry<Enum<?>, V>> iterator () {
					return this;
				}

				/**
				 * Note: the same entry instance is returned each time this method is called.
				 *
				 * @return a reused Entry that will have its key and value set to the next pair
				 */
				@Override
				public Map.Entry<Enum<?>, V> next () {
					if (!hasNext) {throw new NoSuchElementException();}
					if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
					Enum<?>[] keyTable = map.keyTable;
					entry.key = keyTable[nextIndex];
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
			};
		}

		@Override
		public boolean contains (Object o) {
			return iter.map.containsKey(o);
		}

		/**
		 * Returns an iterator over the elements contained in this collection.
		 *
		 * @return an iterator over the elements contained in this collection
		 */
		@Override
		public @NonNull MapIterator<Enum<?>, V, Map.Entry<Enum<?>, V>> iterator () {
			return iter;
		}

		@Override
		public int size () {
			return iter.map.size;
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
		public ObjectList<Map.Entry<Enum<?>, V>> toList () {
			ObjectList<Map.Entry<Enum<?>, V>> list = new ObjectList<>(iter.map.size);
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			while (iter.hasNext) {list.add(new Entry<>(iter.next()));}
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
		public Collection<Map.Entry<Enum<?>, V>> appendInto(Collection<Map.Entry<Enum<?>, V>> coll) {
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			while (iter.hasNext) {coll.add(new Entry<>(iter.next()));}
			iter.currentIndex = currentIdx;
			iter.nextIndex = nextIdx;
			iter.hasNext = hn;
			return coll;
		}

		/**
		 * Append the remaining items that this can iterate through into the given Map.
		 * Does not change the position of this iterator. Note that a Map is not a Collection.
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

	public static class Values<Enum<?>, V> extends AbstractCollection<V> {
		protected MapIterator<Enum<?>, V, V> iter;

		public Values (EMap<V> map) {
			iter = new MapIterator<Enum<?>, V, V>(map) {
				@Override
				public @NonNull MapIterator<Enum<?>, V, V> iterator () {
					return this;
				}

				@Override
				public boolean hasNext () {
					if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
					return hasNext;
				}

				@Override
				public V next () {
					if (!hasNext) {throw new NoSuchElementException();}
					if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
					V value = map.valueTable[nextIndex];
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
		public @NonNull MapIterator<Enum<?>, V, V> iterator () {
			return iter;
		}

		@Override
		public int hashCode () {
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			iter.reset();
			int hc = 1;
			for (V v : this)
				hc = 421 * hc + (v == null ? 0 : v.hashCode());
			iter.currentIndex = currentIdx;
			iter.nextIndex = nextIdx;
			iter.hasNext = hn;
			return hc;
		}

		/**
		 * The iterator is reused by this data structure, and you can reset it
		 * back to the start of the iteration order using this.
		 */
		public void resetIterator () {
			iter.reset();
		}

		@Override
		public int size () {
			return iter.map.size;
		}

		/**
		 * Returns a new {@link ObjectList} containing the remaining items.
		 * Does not change the position of this iterator.
		 */
		public ObjectList<V> toList () {
			ObjectList<V> list = new ObjectList<>(iter.map.size);
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			while (iter.hasNext) {list.add(iter.next());}
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
		public Collection<V> appendInto(Collection<V> coll) {
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			while (iter.hasNext) {coll.add(iter.next());}
			iter.currentIndex = currentIdx;
			iter.nextIndex = nextIdx;
			iter.hasNext = hn;
			return coll;
		}
	}

	public static class Keys<Enum<?>, V> extends AbstractSet<Enum<?>> {
		protected MapIterator<Enum<?>, V, Enum<?>> iter;

		public Keys (EMap<V> map) {
			iter = new MapIterator<Enum<?>, V, Enum<?>>(map) {
				@Override
				public @NonNull MapIterator<Enum<?>, V, Enum<?>> iterator () {
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
					Enum<?> key = map.keyTable[nextIndex];
					currentIndex = nextIndex;
					findNextIndex();
					return key;
				}
			};
		}

		@Override
		public boolean contains (Object o) {
			return iter.map.containsKey(o);
		}

		/**
		 * Returns an iterator over the elements contained in this collection.
		 *
		 * @return an iterator over the elements contained in this collection
		 */
		@Override
		public @NonNull MapIterator<Enum<?>, V, Enum<?>> iterator () {
			return iter;
		}

		@Override
		public int size () {
			return iter.map.size;
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
		public ObjectList<Enum<?>> toList () {
			ObjectList<Enum<?>> list = new ObjectList<>(iter.map.size);
			int currentIdx = iter.currentIndex, nextIdx = iter.nextIndex;
			boolean hn = iter.hasNext;
			while (iter.hasNext) {list.add(iter.next());}
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
	 * Constructs an empty map given the types as generic type arguments.
	 * This is usually less useful than just using the constructor, but can be handy
	 * in some code-generation scenarios when you don't know how many arguments you will have.
	 *
	 * @param <K>    the type of keys
	 * @param <V>    the type of values
	 * @return a new map containing nothing
	 */
	public static <Enum<?>, V> EMap<V> with () {
		return new EMap<>(0);
	}

	/**
	 * Constructs a single-entry map given one key and one value.
	 * This is mostly useful as an optimization for {@link #with(Object, Object, Object...)}
	 * when there's no "rest" of the keys or values.
	 *
	 * @param key0   the first and only key
	 * @param value0 the first and only value
	 * @param <K>    the type of key0
	 * @param <V>    the type of value0
	 * @return a new map containing just the entry mapping key0 to value0
	 */
	public static <Enum<?>, V> EMap<V> with (Enum<?> key0, V value0) {
		EMap<V> map = new EMap<>(1);
		map.put(key0, value0);
		return map;
	}

	/**
	 * Constructs a map given alternating keys and values.
	 * This can be useful in some code-generation scenarios, or when you want to make a
	 * map conveniently by-hand and have it populated at the start. You can also use
	 * {@link #EMap(Object[], Object[])}, which takes all keys and then all values.
	 * This needs all keys to have the same type and all values to have the same type, because
	 * it gets those types from the first key parameter and first value parameter. Any keys that don't
	 * have K as their type or values that don't have V as their type have that entry skipped.
	 *
	 * @param key0   the first key; will be used to determine the type of all keys
	 * @param value0 the first value; will be used to determine the type of all values
	 * @param rest   an array or varargs of alternating K, V, K, V... elements
	 * @param <K>    the type of keys, inferred from key0
	 * @param <V>    the type of values, inferred from value0
	 * @return a new map containing the given keys and values
	 */
	@SuppressWarnings("unchecked")
	public static <Enum<?>, V> EMap<V> with (Enum<?> key0, V value0, Object... rest) {
		EMap<V> map = new EMap<>(1 + (rest.length >>> 1));
		map.put(key0, value0);
		for (int i = 1; i < rest.length; i += 2) {
			try {
				map.put((Enum<?>)rest[i - 1], (V)rest[i]);
			} catch (ClassCastException ignored) {
			}
		}
		return map;
	}
}

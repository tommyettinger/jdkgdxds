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

package com.github.tommyettinger.ds.enums;

import com.github.tommyettinger.ds.ObjectIntMap;
import com.github.tommyettinger.ds.ObjectIntOrderedMap;
import com.github.tommyettinger.ds.PrimitiveCollection;
import com.github.tommyettinger.ds.Utilities;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;

/**
 * A custom variant on ObjectIntOrderedMap that always uses enum keys, which simplifies some operations.
 */
public class EnumIntOrderedMap<K extends Enum<K>> extends ObjectIntOrderedMap<K> {

	/**
	 * Creates a new map with an initial capacity of 51 and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 */
	public EnumIntOrderedMap () {
		super();
	}

	/**
	 * Creates a new map with the specified initial capacity and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 * This map will hold initialCapacity items before growing the backing table.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 */
	public EnumIntOrderedMap (int initialCapacity) {
		super(initialCapacity);
	}

	/**
	 * Creates a new map with the specified initial capacity and load factor. This map will hold initialCapacity items before
	 * growing the backing table.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 * @param loadFactor      what fraction of the capacity can be filled before this has to resize; 0 &lt; loadFactor &lt;= 1
	 */
	public EnumIntOrderedMap (int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
	}

	/**
	 * Creates a new map identical to the specified map.
	 *
	 * @param map an ObjectIntOrderedMap to copy, or a subclass such as this one
	 */
	public EnumIntOrderedMap (ObjectIntOrderedMap<? extends K> map) {
		super(map);
	}

	/**
	 * Creates a new map identical to the specified map.
	 *
	 * @param map an ObjectIntMap to copy, or a subclass
	 */
	public EnumIntOrderedMap (ObjectIntMap<? extends K> map) {
		super(map);
	}

	/**
	 * Given two side-by-side arrays, one of keys, one of values, this constructs a map and inserts each pair of key and value into it.
	 * If keys and values have different lengths, this only uses the length of the smaller array.
	 *
	 * @param keys   an array of keys
	 * @param values an array of values
	 */
	public EnumIntOrderedMap (K[] keys, int[] values) {
		super(keys, values);
	}

	/**
	 * Given two side-by-side collections, one of keys, one of values, this constructs a map and inserts each pair of key and value into it.
	 * If keys and values have different lengths, this only uses the length of the smaller collection.
	 *
	 * @param keys   a Collection of keys
	 * @param values a Collection of values
	 */
	public EnumIntOrderedMap (Collection<? extends K> keys, PrimitiveCollection.OfInt values) {
		super(keys, values);
	}

	@Override
	protected int place (Object item) {
		// As long as the capacity is sufficient, ordinals will never collide.
		if (item instanceof Enum)
			return ((Enum<?>)item).ordinal() & mask;
		return super.place(item);
	}

	@Override
	protected boolean equate (Object left, @Nullable Object right) {
		// Enums can use reference equality.
		return left == right;
	}

	@Override
	public int hashCode () {
		int h = size;
		K[] keyTable = this.keyTable;
		int[] valueTable = this.valueTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			K key = keyTable[i];
			if (key != null) {
				h += key.ordinal() * 421;
				int value = valueTable[i];
				h += value ^ value >>> 32;
			}
		}
		return h;
	}

	@SuppressWarnings({"rawtypes"})
	@Override
	public boolean equals (Object obj) {
		if (obj == this) {return true;}
		if (!(obj instanceof EnumIntOrderedMap)) {return false;}
		EnumIntOrderedMap other = (EnumIntOrderedMap)obj;
		if (other.size != size) {return false;}
		Enum[] keyTable = this.keyTable;
		int[] valueTable = this.valueTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			Enum key = keyTable[i];
			if (key != null) {
				int value = valueTable[i];
				if (value != other.get(key)) {return false;}
			}
		}
		return true;
	}

	/**
	 * Constructs a single-entry map given one key and one value.
	 * This is mostly useful as an optimization for {@link #with(Enum, Number, Object...)}
	 * when there's no "rest" of the keys or values.
	 *
	 * @param key0   the first and only key; must be an enum
	 * @param value0 the first and only value
	 * @return a new map containing just the entry mapping key0 to value0
	 */
	public static <K extends Enum<K>> EnumIntOrderedMap<K> with (K key0, Number value0) {
		EnumIntOrderedMap<K> map = new EnumIntOrderedMap<>(1);
		map.put(key0, value0.intValue());
		return map;
	}

	/**
	 * Constructs a map given alternating keys and values.
	 * This can be useful in some code-generation scenarios, or when you want to make a
	 * map conveniently by-hand and have it populated at the start. You can also use
	 * {@link #EnumIntOrderedMap(Enum[], int[])}, which takes all keys and then all values.
	 * This needs all keys to be {@code enum}s and all values to be {@code Number}s. Any keys that
	 * aren't Enums or values that aren't Numbers have that entry skipped.
	 *
	 * @param key0   the first key; will be used to determine the type of all keys
	 * @param value0 the first value; will be used to determine the type of all values
	 * @param rest   an array or varargs of alternating K, V, K, V... elements
	 * @param <V>    the type of values, inferred from value0
	 * @return a new map containing the given keys and values
	 */
	@SuppressWarnings("unchecked")
	public static <K extends Enum<K>, V> EnumIntOrderedMap<K> with (K key0, Number value0, Object... rest) {
		EnumIntOrderedMap<K> map = new EnumIntOrderedMap<>(1 + (rest.length >>> 1));
		map.put(key0, value0.intValue());
		for (int i = 1; i < rest.length; i += 2) {
			try {
				map.put((K)rest[i - 1], ((Number)rest[i]).intValue());
			} catch (ClassCastException ignored) {
			}
		}
		return map;
	}
}

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

import org.checkerframework.checker.nullness.qual.Nullable;
import java.util.Collection;
import java.util.Map;

/**
 * A variant on {@link ObjectObjectOrderedMap} that compares keys by identity (using {@code ==}) instead of equality (using equals()}).
 * It also hashes with {@link System#identityHashCode(Object)} instead of calling the {@code hashCode()} of a key. This can be useful in
 * some cases where keys may have invalid {@link Object#equals(Object)} and/or {@link Object#hashCode()} implementations, or if keys could
 * be very large (making a hashCode() that uses all the items in the key slow). Oddly, {@link System#identityHashCode(Object)} tends to
 * be slower than the hashCode() for most small keys, because an explicitly-written hashCode() typically doesn't need to do anything
 * concurrently, but identityHashCode() needs to (concurrently) modify an internal JVM variable that ensures the results are unique, and
 * that requires the JVM to do lots of extra work whenever identityHashCode() is called. Despite that, identityHashCode() doesn't depend
 * on the quantity of variables in the key, so identityHashCode() gets relatively faster for larger keys. The equals() method used by
 * ObjectObjectOrderedMap also tends to slow down for large keys, relative to the constant-time {@code ==} this uses.
 * <br>
 * Note that the {@link #entrySet()}, {@link #keySet()} and individual Entry items this
 * produces are those of an {@link ObjectObjectOrderedMap}, and so do not compare by identity.
 */
public class IdentityObjectOrderedMap<K, V> extends ObjectObjectOrderedMap<K, V> {
	/**
	 * Creates a new map with an initial capacity of 51 and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 */
	public IdentityObjectOrderedMap () {
		super();
	}

	/**
	 * Creates a new map with the given starting capacity and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 */
	public IdentityObjectOrderedMap (int initialCapacity) {
		super(initialCapacity);
	}

	/**
	 * Creates a new map with the specified initial capacity and load factor. This map will hold initialCapacity items before
	 * growing the backing table.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 * @param loadFactor      what fraction of the capacity can be filled before this has to resize; 0 &lt; loadFactor &lt;= 1
	 */
	public IdentityObjectOrderedMap (int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
	}

	/**
	 * Creates a new map identical to the specified map.
	 *
	 * @param map an ObjectObjectOrderedMap to copy, or a subclass such as this one
	 */
	public IdentityObjectOrderedMap (ObjectObjectOrderedMap<? extends K, ? extends V> map) {
		super(map);
	}

	/**
	 * Creates a new map identical to the specified map.
	 *
	 * @param map an ObjectObjectMap to copy, or a subclass
	 */
	public IdentityObjectOrderedMap (ObjectObjectMap<? extends K, ? extends V> map) {
		super(map);
	}

	/**
	 * Creates a new map identical to the specified map.
	 *
	 * @param map a Map to copy
	 */
	public IdentityObjectOrderedMap (Map<? extends K, ? extends V> map) {
		super(map);
	}

	/**
	 * Given two side-by-side arrays, one of keys, one of values, this constructs a map and inserts each pair of key and value into it.
	 * If keys and values have different lengths, this only uses the length of the smaller array.
	 *
	 * @param keys   an array of keys
	 * @param values an array of values
	 */
	public IdentityObjectOrderedMap (K[] keys, V[] values) {
		super(keys, values);
	}

	/**
	 * Given two side-by-side collections, one of keys, one of values, this constructs a map and inserts each pair of key and value into it.
	 * If keys and values have different lengths, this only uses the length of the smaller collection.
	 *
	 * @param keys   a Collection of keys
	 * @param values a Collection of values
	 */
	public IdentityObjectOrderedMap (Collection<? extends K> keys, Collection<? extends V> values) {
		super(keys, values);
	}

	/**
	 * Creates a new set by copying {@code count} items from the given ObjectObjectOrderedMap (or subclass, such as IdentityObjectOrderedMap),
	 * starting at {@code offset} in that Map, into this.
	 *
	 * @param other  an ObjectObjectOrderedMap or other subclass, like IdentityObjectOrderedMap, of the same generic types
	 * @param offset the first index in other's ordering to draw an item from
	 * @param count  how many items to copy from other
	 */
	public IdentityObjectOrderedMap (ObjectObjectOrderedMap<? extends K, ? extends V> other, int offset, int count) {
		this(count);
		putAll(0, other, offset, count);
	}

	/**
	 * Returns an index &gt;= 0 and &lt;= {@link #mask} for the specified {@code item}.
	 * <p>
	 * This particular overload relies on the naturally-random nature of {@link System#identityHashCode(Object)}, and just
	 * masks the identity hash code so only the lower bits are used. This should be fine because the identity hash code
	 * defaults to a decent random number generator for its output, so it should collide over all bits very rarely, and
	 * collide only over the masked bits somewhat rarely.
	 *
	 * @param item a non-null Object; its identityHashCode is used here
	 */
	@Override
	protected int place (Object item) {
		return (System.identityHashCode(item) & mask);
	}

	@Override
	protected boolean equate (Object left, @Nullable Object right) {
		return left == right;
	}

	@Override
	public int hashCode () {
		int h = size;
		K[] keyTable = this.keyTable;
		V[] valueTable = this.valueTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			K key = keyTable[i];
			if (key != null) {
				h ^= System.identityHashCode(key);
				V value = valueTable[i];
				if (value != null) {h ^= value.hashCode();}
			}
		}
		return h;
	}

	/**
	 * Gets the hash multiplier, which this class does not use itself.
	 * @return the current hash multiplier
	 */
	@Override
	public long getHashMultiplier () {
		return hashMultiplier;
	}

	/**
	 * Sets the hash multiplier, which this class does not use itself. Because of that, this method does not
	 * call {@link #resize(int)} at all. The hash multiplier will change anyway the next time resize() is called.
	 * You probably just don't need to call this method.
	 * @param hashMultiplier will not be used, but will be treated as odd and stored in case some other code needs it
	 */
	@Override
	public void setHashMultiplier (long hashMultiplier) {
		this.hashMultiplier = hashMultiplier | 1L;
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
	public static <K, V> IdentityObjectOrderedMap<K, V> with (K key0, V value0) {
		IdentityObjectOrderedMap<K, V> map = new IdentityObjectOrderedMap<>(1);
		map.put(key0, value0);
		return map;
	}

	/**
	 * Constructs a map given alternating keys and values.
	 * This can be useful in some code-generation scenarios, or when you want to make a
	 * map conveniently by-hand and have it populated at the start. You can also use
	 * {@link #IdentityObjectOrderedMap(Object[], Object[])}, which takes all keys and then all values.
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
	public static <K, V> IdentityObjectOrderedMap<K, V> with (K key0, V value0, Object... rest) {
		IdentityObjectOrderedMap<K, V> map = new IdentityObjectOrderedMap<>(1 + (rest.length >>> 1));
		map.put(key0, value0);
		for (int i = 1; i < rest.length; i += 2) {
			try {
				map.put((K)rest[i - 1], (V)rest[i]);
			} catch (ClassCastException ignored) {
			}
		}
		return map;
	}
}

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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;

import static com.github.tommyettinger.ds.Utilities.neverIdentical;

/**
 * A custom variant on ObjectObjectOrderedMap that always uses CharSequence keys and compares them as case-insensitive.
 * This uses a fairly complex, quite-optimized hashing function because it needs to hash CharSequences rather
 * often, and to do so ignoring case means {@link String#hashCode()} won't work, plus not all CharSequences
 * implement hashCode() themselves (such as {@link StringBuilder}). User code similar to this can often get away
 * with a simple polynomial hash (the typical Java kind, used by String and Arrays), or if more speed is needed,
 * one with <a href="https://richardstartin.github.io/posts/collecting-rocks-and-benchmarks">some of these
 * optimizations by Richard Startin</a>. If you don't want to write or benchmark a hash function (which is quite
 * understandable), {@link Utilities#hashCodeIgnoreCase(CharSequence)} can get a case-insensitive hash of any
 * CharSequence, as a long. It does this without allocating new Strings all over, where many case-insensitive
 * algorithms do allocate quite a lot, but it does this by handling case incorrectly for the Georgian alphabet.
 * If I see Georgian text in-the-wild, I may reconsider, but I don't think that particular alphabet is in
 * widespread use. There's also {@link Utilities#equalsIgnoreCase(CharSequence, CharSequence)} for equality
 * comparisons that are similarly case-insensitive, except for Georgian.
 * <br>
 * This is very similar to
 * {@link CaseInsensitiveMap}, except that this class maintains insertion order and can be sorted with
 * {@link #sort()}, {@link #sortByValue(Comparator)}, etc. Note that because each CharSequence is stored in here
 * in its original form (not modified to make it ignore case), the sorted order might be different than you expect.
 * {@link Utilities#compareIgnoreCase(CharSequence, CharSequence)} can be used to sort this as case-insensitive.
 * <br>
 * This is also very similar to {@link FilteredStringOrderedMap} when its {@link CharFilter#getEditor() editor}
 * is {@link Character#toUpperCase(char)} or {@link Casing#caseUp(char)}.
 * FilteredStringOrderedMap works with Strings rather than CharSequences, which
 * may be more convenient, and allows filtering some characters out of hashing and equality comparisons. If you want a
 * case-insensitive map that ignores any non-letter characters in a String, then CaseInsensitiveOrderedMap won't do,
 * but {@code new FilteredStringOrderedMap<>(CharPredicates.IS_LETTER, Casing::caseUp} will work. Note that GWT only
 * handles {@link Character#isLetter(char)} for ASCII letters; CharPredicates in this library provides cross-platform
 * predicates that use {@link CharBitSet} to store their data, and the library RegExodus offers replacements in
 * Category for other Unicode categories, such as upper-case letters, currency symbols, decimal digits, and so on.
 */
public class CaseInsensitiveOrderedMap<V> extends ObjectObjectOrderedMap<CharSequence, V> {

	/**
	 * Creates a new map with an initial capacity of {@link Utilities#getDefaultTableCapacity()} and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 *
	 * @param ordering determines what implementation {@link #order()} will use
	 */
	public CaseInsensitiveOrderedMap(OrderType ordering) {
		super(ordering);
	}

	/**
	 * Creates a new map with the given starting capacity and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 * @param ordering        determines what implementation {@link #order()} will use
	 */
	public CaseInsensitiveOrderedMap(int initialCapacity, OrderType ordering) {
		super(initialCapacity, ordering);
	}

	/**
	 * Creates a new map with the specified initial capacity and load factor. This map will hold initialCapacity items before
	 * growing the backing table.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 * @param loadFactor      what fraction of the capacity can be filled before this has to resize; 0 &lt; loadFactor &lt;= 1
	 * @param ordering        determines what implementation {@link #order()} will use
	 */
	public CaseInsensitiveOrderedMap(int initialCapacity, float loadFactor, OrderType ordering) {
		super(initialCapacity, loadFactor, ordering);
	}

	/**
	 * Creates a new map identical to the specified map.
	 *
	 * @param map      the map to copy
	 * @param ordering determines what implementation {@link #order()} will use
	 */
	public CaseInsensitiveOrderedMap(Map<? extends CharSequence, ? extends V> map, OrderType ordering) {
		super(map, ordering);
	}

	/**
	 * Creates a new map identical to the specified map.
	 *
	 * @param map      the map to copy
	 * @param ordering determines what implementation {@link #order()} will use
	 */
	public CaseInsensitiveOrderedMap(ObjectObjectMap<? extends CharSequence, ? extends V> map, OrderType ordering) {
		super(map, ordering);
	}

	/**
	 * Creates a new set by copying {@code count} items from the given ObjectObjectOrderedMap, starting at {@code offset} in that Map,
	 * into this.
	 *
	 * @param other    another ObjectObjectOrderedMap of the same type
	 * @param offset   the first index in other's ordering to draw an item from
	 * @param count    how many items to copy from other
	 * @param ordering determines what implementation {@link #order()} will use
	 */
	public CaseInsensitiveOrderedMap(ObjectObjectOrderedMap<? extends CharSequence, ? extends V> other, int offset, int count, OrderType ordering) {
		super(other, offset, count, ordering);
	}

	/**
	 * Given two side-by-side arrays, one of keys, one of values, this constructs a map and inserts each pair of key and value into it.
	 * If keys and values have different lengths, this only uses the length of the smaller array.
	 *
	 * @param keys     an array of keys
	 * @param values   an array of values
	 * @param ordering determines what implementation {@link #order()} will use
	 */
	public CaseInsensitiveOrderedMap(CharSequence[] keys, V[] values, OrderType ordering) {
		super(keys, values, ordering);
	}

	/**
	 * Given two side-by-side collections, one of keys, one of values, this constructs a map and inserts each pair of key and value into it.
	 * If keys and values have different lengths, this only uses the length of the smaller collection.
	 *
	 * @param keys     a Collection of keys
	 * @param values   a Collection of values
	 * @param ordering determines what implementation {@link #order()} will use
	 */
	public CaseInsensitiveOrderedMap(Collection<? extends CharSequence> keys, Collection<? extends V> values, OrderType ordering) {
		super(keys, values, ordering);
	}

	/**
	 * Creates a new map with an initial capacity of {@link Utilities#getDefaultTableCapacity()} and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 */
	public CaseInsensitiveOrderedMap() {
		super();
	}

	/**
	 * Creates a new map with the specified initial capacity and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 * This map will hold initialCapacity items before growing the backing table.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 */
	public CaseInsensitiveOrderedMap(int initialCapacity) {
		super(initialCapacity);
	}

	/**
	 * Creates a new map with the specified initial capacity and load factor. This map will hold initialCapacity items before
	 * growing the backing table.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 * @param loadFactor      what fraction of the capacity can be filled before this has to resize; 0 &lt; loadFactor &lt;= 1
	 */
	public CaseInsensitiveOrderedMap(int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
	}

	/**
	 * Creates a new map identical to the specified map.
	 *
	 * @param map a Map to copy; ObjectObjectOrderedMap and subclasses of it will be faster
	 */
	public CaseInsensitiveOrderedMap(Map<? extends CharSequence, ? extends V> map) {
		super(map.size());
		putAll(map);
	}

	/**
	 * Given two side-by-side arrays, one of keys, one of values, this constructs a map and inserts each pair of key and value into it.
	 * If keys and values have different lengths, this only uses the length of the smaller array.
	 *
	 * @param keys   an array of keys
	 * @param values an array of values
	 */
	public CaseInsensitiveOrderedMap(CharSequence[] keys, V[] values) {
		super(Math.min(keys.length, values.length));
		putAll(keys, values);
	}

	/**
	 * Creates a new map identical to the specified map.
	 *
	 * @param map a CaseInsensitiveOrderedMap to copy
	 */
	public CaseInsensitiveOrderedMap(CaseInsensitiveOrderedMap<? extends V> map) {
		super(map.size(), map.loadFactor);
		this.hashMultiplier = map.hashMultiplier;
		putAll(map);

	}

	/**
	 * Given two side-by-side collections, one of keys, one of values, this constructs a map and inserts each pair of key and value into it.
	 * If keys and values have different lengths, this only uses the length of the smaller collection.
	 *
	 * @param keys   a Collection of keys
	 * @param values a Collection of values
	 */
	public CaseInsensitiveOrderedMap(Collection<? extends CharSequence> keys, Collection<? extends V> values) {
		super(Math.min(keys.size(), values.size()));
		putAll(keys, values);
	}

	/**
	 * Creates a new set by copying {@code count} items from the given ObjectObjectOrderedMap (or a subclass, such as
	 * CaseInsensitiveOrderedMap), starting at {@code offset} in that Map, into this.
	 *
	 * @param other  another ObjectObjectOrderedMap of the same types (key must be some kind of CharSequence)
	 * @param offset the first index in other's ordering to draw an item from
	 * @param count  how many items to copy from other
	 */
	public CaseInsensitiveOrderedMap(ObjectObjectOrderedMap<? extends CharSequence, ? extends V> other, int offset, int count) {
		this(count, other.loadFactor);
		hashMultiplier = other.hashMultiplier;
		putAll(0, other, offset, count);
	}

	@Override
	protected int place(@NotNull Object item) {
		if (item instanceof CharSequence)
			return Utilities.hashCodeIgnoreCase((CharSequence) item, hashMultiplier) & mask;
		return super.place(item);
	}

	@Override
	protected boolean equate(Object left, @Nullable Object right) {
		if ((left instanceof CharSequence) && (right instanceof CharSequence)) {
			return Utilities.equalsIgnoreCase((CharSequence) left, (CharSequence) right);
		}
		return false;
	}

	@Override
	public int hashCode() {
		int h = size;
		@Nullable CharSequence[] keyTable = this.keyTable;
		@Nullable V[] valueTable = this.valueTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			@Nullable CharSequence key = keyTable[i];
			if (key != null) {
				h ^= Utilities.hashCodeIgnoreCase(key);
				V value = valueTable[i];
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
		Object[] keyTable = this.keyTable;
		V[] valueTable = this.valueTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			Object key = keyTable[i];
			if (key != null) {
				V value = valueTable[i];
				if (value == null) {
					if (other.getOrDefault(key, neverIdentical) != null) {
						return false;
					}
				} else {
					if (!value.equals(other.get(key))) {
						return false;
					}
				}
			}
		}
		return true;
	}

	@Override
	public @NotNull Keys<CharSequence, V> keySet() {
		if (keys1 == null || keys2 == null) {
			keys1 = new CaseInsensitiveKeys<>(this);
			keys2 = new CaseInsensitiveKeys<>(this);
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

	public static class CaseInsensitiveKeys<V> extends OrderedMapKeys<CharSequence, V> {
		public CaseInsensitiveKeys(ObjectObjectOrderedMap<CharSequence, V> map) {
			super(map);
		}

		@Override
		public int hashCode() {
			int h = 0;
			iter.reset();
			while (iter.hasNext()) {
				CharSequence obj = iter.next();
				if (obj != null)
					h ^= Utilities.hashCodeIgnoreCase(obj);
			}
			return h;
		}
	}

	/**
	 * Constructs a single-entry map given one key and one value.
	 * This is mostly useful as an optimization for {@link #with(Object, Object, Object...)}
	 * when there's no "rest" of the keys or values.
	 *
	 * @param key0   the first and only key
	 * @param value0 the first and only value
	 * @param <V>    the type of value0
	 * @return a new map containing just the entry mapping key0 to value0
	 */
	public static <V> CaseInsensitiveOrderedMap<V> with(CharSequence key0, V value0) {
		CaseInsensitiveOrderedMap<V> map = new CaseInsensitiveOrderedMap<>(1);
		map.put(key0, value0);
		return map;
	}

	/**
	 * Constructs a single-entry map given two key-value pairs.
	 * This is mostly useful as an optimization for {@link #with(Object, Object, Object...)}
	 * when there's no "rest" of the keys or values.
	 *
	 * @param key0   a K key
	 * @param value0 a V value
	 * @param key1   a K key
	 * @param value1 a V value
	 * @param <V>    the type of value0
	 * @return a new map containing entries mapping each key to the following value
	 */
	public static <V> CaseInsensitiveOrderedMap<V> with(CharSequence key0, V value0, CharSequence key1, V value1) {
		CaseInsensitiveOrderedMap<V> map = new CaseInsensitiveOrderedMap<>(2);
		map.put(key0, value0);
		map.put(key1, value1);
		return map;
	}

	/**
	 * Constructs a single-entry map given three key-value pairs.
	 * This is mostly useful as an optimization for {@link #with(Object, Object, Object...)}
	 * when there's no "rest" of the keys or values.
	 *
	 * @param key0   a CharSequence key
	 * @param value0 a V value
	 * @param key1   a CharSequence key
	 * @param value1 a V value
	 * @param key2   a CharSequence key
	 * @param value2 a V value
	 * @param <V>    the type of value0
	 * @return a new map containing entries mapping each key to the following value
	 */
	public static <V> CaseInsensitiveOrderedMap<V> with(CharSequence key0, V value0, CharSequence key1, V value1, CharSequence key2, V value2) {
		CaseInsensitiveOrderedMap<V> map = new CaseInsensitiveOrderedMap<>(3);
		map.put(key0, value0);
		map.put(key1, value1);
		map.put(key2, value2);
		return map;
	}

	/**
	 * Constructs a single-entry map given four key-value pairs.
	 * This is mostly useful as an optimization for {@link #with(Object, Object, Object...)}
	 * when there's no "rest" of the keys or values.
	 *
	 * @param key0   a CharSequence key
	 * @param value0 a V value
	 * @param key1   a CharSequence key
	 * @param value1 a V value
	 * @param key2   a CharSequence key
	 * @param value2 a V value
	 * @param key3   a CharSequence key
	 * @param value3 a V value
	 * @param <V>    the type of value0
	 * @return a new map containing entries mapping each key to the following value
	 */
	public static <V> CaseInsensitiveOrderedMap<V> with(CharSequence key0, V value0, CharSequence key1, V value1, CharSequence key2, V value2, CharSequence key3, V value3) {
		CaseInsensitiveOrderedMap<V> map = new CaseInsensitiveOrderedMap<>(4);
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
	 * {@link #CaseInsensitiveOrderedMap(CharSequence[], Object[])}, which takes all keys and then all values.
	 * This needs all keys to have the same type and all values to have the same type, because
	 * it gets those types from the first key parameter and first value parameter. Any keys that don't
	 * have CharSequence as their type or values that don't have V as their type have that entry skipped.
	 *
	 * @param key0   the first key; will be used to determine the type of all keys
	 * @param value0 the first value; will be used to determine the type of all values
	 * @param rest   an array or varargs of alternating CharSequence, V, CharSequence, V... elements
	 * @param <V>    the type of values, inferred from value0
	 * @return a new map containing the given keys and values
	 */
	public static <V> CaseInsensitiveOrderedMap<V> with(CharSequence key0, V value0, Object... rest) {
		CaseInsensitiveOrderedMap<V> map = new CaseInsensitiveOrderedMap<>(1 + (rest.length >>> 1));
		map.put(key0, value0);
		map.putPairs(rest);
		return map;
	}

	/**
	 * Creates a new map by parsing all of {@code str} with the given PartialParser for values,
	 * with entries separated by {@code entrySeparator}, such as {@code ", "} and
	 * the keys separated from values by {@code keyValueSeparator}, such as {@code "="}.
	 * <br>
	 * Various {@link PartialParser} instances are defined as constants, such as
	 * {@link PartialParser#DEFAULT_STRING}, and others can be created by static methods in PartialParser, such as
	 * {@link PartialParser#objectListParser(PartialParser, String, boolean)}.
	 *
	 * @param str               a String containing parseable text
	 * @param entrySeparator    the String separating every key-value pair
	 * @param keyValueSeparator the String separating every key from its corresponding value
	 * @param valueParser       a PartialParser that returns a {@code V} value from a section of {@code str}
	 */
	public static <V> CaseInsensitiveOrderedMap<V> parse(String str,
												  String entrySeparator,
												  String keyValueSeparator,
												  PartialParser<V> valueParser) {
		return parse(str, entrySeparator, keyValueSeparator, valueParser, false);
	}
	/**
	 * Creates a new map by parsing all of {@code str} (or if {@code brackets} is true, all but the first and last
	 * chars) with the given PartialParser for values, with entries separated by {@code entrySeparator},
	 * such as {@code ", "} and the keys separated from values by {@code keyValueSeparator}, such as {@code "="}.
	 * <br>
	 * Various {@link PartialParser} instances are defined as constants, such as
	 * {@link PartialParser#DEFAULT_STRING}, and others can be created by static methods in PartialParser, such as
	 * {@link PartialParser#objectListParser(PartialParser, String, boolean)}.
	 *
	 * @param str               a String containing parseable text
	 * @param entrySeparator    the String separating every key-value pair
	 * @param keyValueSeparator the String separating every key from its corresponding value
	 * @param valueParser       a PartialParser that returns a {@code V} value from a section of {@code str}
	 * @param brackets          if true, the first and last chars in {@code str} will be ignored
	 */
	public static <V> CaseInsensitiveOrderedMap<V> parse(String str,
												  String entrySeparator,
												  String keyValueSeparator,
												  PartialParser<V> valueParser,
												  boolean brackets) {
		CaseInsensitiveOrderedMap<V> m = new CaseInsensitiveOrderedMap<>();
		if(brackets)
			m.putLegible(str, entrySeparator, keyValueSeparator, PartialParser.DEFAULT_CHAR_SEQUENCE, valueParser, 1, str.length() - 1);
		else
			m.putLegible(str, entrySeparator, keyValueSeparator, PartialParser.DEFAULT_CHAR_SEQUENCE, valueParser, 0, -1);
		return m;
	}

	/**
	 * Creates a new map by parsing the given subrange of {@code str} with the given PartialParser for values,
	 * with entries separated by {@code entrySeparator}, such as {@code ", "} and the keys separated from values
	 * by {@code keyValueSeparator}, such as {@code "="}.
	 * <br>
	 * Various {@link PartialParser} instances are defined as constants, such as
	 * {@link PartialParser#DEFAULT_STRING}, and others can be created by static methods in PartialParser, such as
	 * {@link PartialParser#objectListParser(PartialParser, String, boolean)}.
	 *
	 * @param str               a String containing parseable text
	 * @param entrySeparator    the String separating every key-value pair
	 * @param keyValueSeparator the String separating every key from its corresponding value
	 * @param valueParser       a PartialParser that returns a {@code V} value from a section of {@code str}
	 * @param offset            the first position to read parseable text from in {@code str}
	 * @param length            how many chars to read; -1 is treated as maximum length
	 */
	public static <V> CaseInsensitiveOrderedMap<V> parse(String str,
												  String entrySeparator,
												  String keyValueSeparator,
												  PartialParser<V> valueParser,
												  int offset,
												  int length) {
		CaseInsensitiveOrderedMap<V> m = new CaseInsensitiveOrderedMap<>();
		m.putLegible(str, entrySeparator, keyValueSeparator, PartialParser.DEFAULT_CHAR_SEQUENCE, valueParser, offset, length);
		return m;
	}
}

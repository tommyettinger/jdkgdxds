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

import com.github.tommyettinger.digital.BitConversion;
import com.github.tommyettinger.ds.support.sort.FilteredComparators;
import com.github.tommyettinger.function.CharPredicate;
import com.github.tommyettinger.function.CharToCharFunction;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;

import static com.github.tommyettinger.ds.Utilities.neverIdentical;

/**
 * A custom variant on ObjectObjectOrderedMap that always uses String keys, but only considers any character in an item (for
 * equality and hashing purposes) if that character satisfies a predicate. This can also edit the characters that pass
 * the filter, such as by changing their case during comparisons (and hashing). You will usually want to call
 * {@link #setFilter(CharFilter)} to change the behavior of hashing and
 * equality before you enter any items, unless you have specified the CharFilter you want in the constructor.
 * <br>
 * You can use this class as a replacement for {@link CaseInsensitiveMap} if you set the editor to a method reference to
 * {@link Character#toUpperCase(char)}. You can go further by setting the filter to make the hashing and equality checks
 * ignore characters that don't satisfy a predicate, such as {@link Character#isLetter(char)}.
 * CaseInsensitiveOrderedMap does allow taking arbitrary CharSequence types as keys, but it doesn't permit modifying
 * them, so usually Strings are a good choice anyway.
 * <br>
 * Be advised that if you use some (most) checks in {@link Character} for properties of a char, and you try to use them
 * on GWT, those checks will not work as expected for non-ASCII characters. Some other platforms might also be affected,
 * such as TeaVM, but it isn't clear yet which platforms have full Unicode support. You can consider depending upon
 * <a href="https://github.com/tommyettinger/RegExodus">RegExodus</a> for more cross-platform Unicode support; a method
 * reference to {@code Category.L::contains} acts like {@code Character::isLetter}, but works on GWT.
 * <br>
 * This is very similar to {@link FilteredStringMap},
 * except that this class maintains insertion order and can be sorted with {@link #sort()}, {@link #sort(Comparator)}, etc.
 * Note that because each String is stored in here in its original form (not modified to make it use the CharFilter),
 * the sorted order might be different than you expect.
 * You can use {@link FilteredComparators#makeStringComparator(CharPredicate, CharToCharFunction)} to create a Comparator
 * for Strings that uses the same rules this class does.
 */
public class FilteredStringOrderedMap<V> extends ObjectObjectOrderedMap<String, V> {
	protected CharFilter filter = CharFilter.getOrCreate("Identity", c -> true, c -> c);

	/**
	 * Used by {@link #place(Object)} to mix hashCode() results. Changes on every call to {@link #resize(int)} by default.
	 * This only needs to be serialized if the full key and value tables are serialized, or if the iteration order should be
	 * the same before and after serialization. Iteration order is better handled by using {@link ObjectObjectOrderedMap}.
	 */
	protected int hashMultiplier = 0xEFAA28F1;

	/**
	 * Creates a new map with an initial capacity of 51 and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 */
	public FilteredStringOrderedMap () {
		super();
	}

	/**
	 * Creates a new map with the specified initial capacity and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 * This map will hold initialCapacity items before growing the backing table.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 */
	public FilteredStringOrderedMap (int initialCapacity) {
		super(initialCapacity);
	}

	/**
	 * Creates a new map with the specified initial capacity and load factor. This map will hold initialCapacity items before
	 * growing the backing table.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 * @param loadFactor      what fraction of the capacity can be filled before this has to resize; 0 &lt; loadFactor &lt;= 1
	 */
	public FilteredStringOrderedMap (int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
	}

	/**
	 * Creates a new map with an initial capacity of 51 and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 * This uses the specified CharFilter.
	 *
	 * @param filter a CharFilter that can be obtained with {@link CharFilter#getOrCreate(String, CharPredicate, CharToCharFunction)}
	 */
	public FilteredStringOrderedMap (CharFilter filter) {
		super();
		this.filter = filter;
	}

	/**
	 * Creates a new map with the specified initial capacity and th default load factor. This map will hold initialCapacity items
	 * before growing the backing table.
	 * This uses the specified CharFilter.
	 *
	 * @param filter a CharFilter that can be obtained with {@link CharFilter#getOrCreate(String, CharPredicate, CharToCharFunction)}
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 */
	public FilteredStringOrderedMap (CharFilter filter, int initialCapacity) {
		super(initialCapacity);
		this.filter = filter;
	}

	/**
	 * Creates a new map with the specified initial capacity and load factor. This map will hold initialCapacity items before
	 * growing the backing table.
	 * This uses the specified CharFilter.
	 *
	 * @param filter a CharFilter that can be obtained with {@link CharFilter#getOrCreate(String, CharPredicate, CharToCharFunction)}
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 * @param loadFactor      what fraction of the capacity can be filled before this has to resize; 0 &lt; loadFactor &lt;= 1
	 */
	public FilteredStringOrderedMap (CharFilter filter, int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
		this.filter = filter;
	}

	/**
	 * Creates a new map identical to the specified map.
	 *
	 * @param map an FilteredStringOrderedMap to copy
	 */
	public FilteredStringOrderedMap (FilteredStringOrderedMap<? extends V> map) {
		super(map.size());
		filter = map.filter;
		this.hashMultiplier = map.hashMultiplier;
		putAll(map);
	}

	/**
	 * Creates a new map identical to the specified map.
	 * This uses the specified CharFilter.
	 *
	 * @param filter a CharFilter that can be obtained with {@link CharFilter#getOrCreate(String, CharPredicate, CharToCharFunction)}
	 * @param map    a Map to copy; ObjectObjectOrderedMap and subclasses of it will be faster to load from
	 */
	public FilteredStringOrderedMap (CharFilter filter, Map<String, ? extends V> map) {
		this(filter, map.size());
		for (String k : map.keySet()) {
			put(k, map.get(k));
		}
	}

	/**
	 * Given two side-by-side arrays, one of keys, one of values, this constructs a map and inserts each pair of key and value into it.
	 * If keys and values have different lengths, this only uses the length of the smaller array.
	 * This uses the specified CharFilter.
	 *
	 * @param filter a CharFilter that can be obtained with {@link CharFilter#getOrCreate(String, CharPredicate, CharToCharFunction)}
	 * @param keys   an array of keys
	 * @param values an array of values
	 */
	public FilteredStringOrderedMap (CharFilter filter, String[] keys, V[] values) {
		this(filter, Math.min(keys.length, values.length));
		putAll(keys, values);
	}

	/**
	 * Given two side-by-side collections, one of keys, one of values, this constructs a map and inserts each pair of key and value into it.
	 * If keys and values have different lengths, this only uses the length of the smaller collection.
	 * This uses the specified CharFilter.
	 *
	 * @param filter a CharFilter that can be obtained with {@link CharFilter#getOrCreate(String, CharPredicate, CharToCharFunction)}
	 * @param keys   a Collection of keys
	 * @param values a Collection of values
	 */
	public FilteredStringOrderedMap (CharFilter filter, Collection<String> keys, Collection<? extends V> values) {
		this(filter, Math.min(keys.size(), values.size()));
		putAll(keys, values);
	}

	/**
	 * Creates a new set by copying {@code count} items from the given ObjectObjectOrderedMap (or a subclass, such as
	 * CaseInsensitiveOrderedMap), starting at {@code offset} in that Map, into this.
	 *
	 * @param filter a CharFilter that can be obtained with {@link CharFilter#getOrCreate(String, CharPredicate, CharToCharFunction)}
	 * @param other  another ObjectObjectOrderedMap of the same types (key must be String)
	 * @param offset the first index in other's ordering to draw an item from
	 * @param count  how many items to copy from other
	 */
	public FilteredStringOrderedMap (CharFilter filter, ObjectObjectOrderedMap<String, ? extends V> other, int offset, int count) {
		this(filter, count);
		putAll(0, other, offset, count);
	}

	public CharFilter getFilter () {
		return filter;
	}

	/**
	 * Sets the filter that determines which characters in a String are considered for equality and hashing, then
	 * returns this object, for chaining. Common CharPredicate filters you might use could be method references to
	 * {@link Character#isLetter(char)} or {@link CharList#contains(char)}, for example. If the filter returns true for
	 * a given character, that character will be used for hashing/equality; otherwise it will be ignored.
	 * The default filter always returns true. If the filter changes, that invalidates anything previously entered into
	 * this, so before changing the filter <em>this clears the entire data structure</em>, removing all existing items.
	 *
	 * @param filter a CharPredicate that should return true iff a character should be considered for equality/hashing
	 * @return this, for chaining
	 */
	public FilteredStringOrderedMap<V> setFilter (CharFilter filter) {
		clear();
		this.filter = filter;
		return this;
	}

	/**
	 * Gets a low-to-moderate quality 32-bit hash code from the given String.
	 * This operates by checking if a char in {@code s} matches the filter, and if it does, it rotates the current hash,
	 * multiplies it by the {@link #getHashMultiplier() hash multiplier}, and XORs with the current char after editing.
	 * This finalizes the hash by multiplying it again by the hash multiplier, then using the reversible
	 * XOR-rotate-XOR-rotate sequence of operations to adequately jumble the bits.
	 * @param s a String to hash
	 * @return a 32-bit hash of {@code s}
	 */
	protected int hashHelper (final String s) {
		final int hm = hashMultiplier;
		int hash = hm;
		for (int i = 0, len = s.length(); i < len; i++) {
			final char c = s.charAt(i);
			if (filter.filter.test(c)) {
				hash = BitConversion.imul((hash << 13 | hash >>> 19), hm) ^ filter.editor.applyAsChar(c);
			}
		}
		hash = BitConversion.imul(hash, hm);
		return hash ^ (hash << 23 | hash >>> 9) ^ (hash << 11 | hash >>> 21);
	}

	@Override
	protected int place (@NonNull Object item) {
		if (item instanceof String) {
			return hashHelper((String) item) & mask;
		}
		return super.place(item);
	}

	/**
	 * Compares two objects for equality by the rules this filtered data structure uses for keys.
	 * This will return true if the arguments are reference-equivalent or both null. Otherwise, it
	 * requires that both are {@link String}s and compares them using the {@link #getFilter() filter}
	 * of this object.
	 *
	 * @param left  must be non-null; typically a key being compared, but not necessarily
	 * @param right may be null; typically a key being compared, but can often be null for an empty key slot, or some other type
	 * @return true if left and right are equivalent according to the rules this filtered type uses
	 */
	@Override
	public boolean equate (Object left, @Nullable Object right) {

		if (left == right)
			return true;
		if(right == null) return false;
		if ((left instanceof String) && (right instanceof String)) {
			String l = (String)left, r = (String)right;
			int llen = l.length(), rlen = r.length();
			int cl = -1, cr = -1;
			int i = 0, j = 0;
			while (i < llen || j < rlen) {
				if(i == llen) cl = -1;
				else {
					while (i < llen && !filter.filter.test((char) (cl = l.charAt(i++)))) {
						cl = -1;
					}
				}
				if(j == rlen) cr = -1;
				else {
					while (j < rlen && !filter.filter.test((char) (cr = r.charAt(j++)))) {
						cr = -1;
					}
				}
				if(cl != cr && filter.editor.applyAsChar((char)cl) != filter.editor.applyAsChar((char)cr))
					return false;
			}
			return true;
		}
		return false;
	}

	@Override
	public int hashCode () {
		int h = size;
		String[] keyTable = this.keyTable;
		V[] valueTable = this.valueTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			String key = keyTable[i];
			if (key != null) {
				h ^= hashHelper(key);
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
		return true;
	}

	protected void resize (int newSize) {
		int oldCapacity = keyTable.length;
		threshold = (int)(newSize * loadFactor);
		mask = newSize - 1;
		shift = BitConversion.countLeadingZeros(mask) + 32;

		hashMultiplier = Utilities.GOOD_MULTIPLIERS[BitConversion.imul(hashMultiplier, shift) >>> 5 & 511];
		String[] oldKeyTable = keyTable;
		V[] oldValueTable = valueTable;

		keyTable = new String[newSize];
		valueTable = (V[])new Object[newSize];

		if (size > 0) {
			for (int i = 0; i < oldCapacity; i++) {
				String key = oldKeyTable[i];
				if (key != null) {putResize(key, oldValueTable[i]);}
			}
		}
	}


	/**
	 * Constructs an empty map given just a CharFilter.
	 * This is usually less useful than just using the constructor, but can be handy
	 * in some code-generation scenarios when you don't know how many arguments you will have.
	 *
	 * @param filter a CharFilter that can be obtained with {@link CharFilter#getOrCreate(String, CharPredicate, CharToCharFunction)}
	 * @param <V>    the type of values
	 * @return a new map containing nothing
	 */
	public static <V> FilteredStringOrderedMap<V> with (CharFilter filter) {
		return new FilteredStringOrderedMap<>(filter);
	}

	/**
	 * Constructs a single-entry map given a CharFilter, one key and one value.
	 * This is mostly useful as an optimization for {@link #with(Object, Object, Object...)}
	 * when there's no "rest" of the keys or values.
	 *
	 * @param filter a CharFilter that can be obtained with {@link CharFilter#getOrCreate(String, CharPredicate, CharToCharFunction)}
	 * @param key0   the first and only key
	 * @param value0 the first and only value
	 * @param <V>    the type of value0
	 * @return a new map containing just the entry mapping key0 to value0
	 */
	public static <V> FilteredStringOrderedMap<V> with (CharFilter filter, String key0, V value0) {
		FilteredStringOrderedMap<V> map = new FilteredStringOrderedMap<>(filter, 1);
		map.put(key0, value0);
		return map;
	}

	/**
	 * Constructs a single-entry map given a CharFilter and two key-value pairs.
	 * This is mostly useful as an optimization for {@link #with(Object, Object, Object...)}
	 * when there's no "rest" of the keys or values.
	 *
	 * @param filter a CharFilter that can be obtained with {@link CharFilter#getOrCreate(String, CharPredicate, CharToCharFunction)}
	 * @param key0   a String key
	 * @param value0 a V value
	 * @param key1   a String key
	 * @param value1 a V value
	 * @param <V>    the type of value0
	 * @return a new map containing entries mapping each key to the following value
	 */
	public static <V> FilteredStringOrderedMap<V> with (CharFilter filter, String key0, V value0, String key1, V value1) {
		FilteredStringOrderedMap<V> map = new FilteredStringOrderedMap<>(filter, 2);
		map.put(key0, value0);
		map.put(key1, value1);
		return map;
	}

	/**
	 * Constructs a single-entry map given a CharFilter and three key-value pairs.
	 * This is mostly useful as an optimization for {@link #with(Object, Object, Object...)}
	 * when there's no "rest" of the keys or values.
	 *
	 * @param filter a CharFilter that can be obtained with {@link CharFilter#getOrCreate(String, CharPredicate, CharToCharFunction)}
	 * @param key0   a String key
	 * @param value0 a V value
	 * @param key1   a String key
	 * @param value1 a V value
	 * @param key2   a String key
	 * @param value2 a V value
	 * @param <V>    the type of value0
	 * @return a new map containing entries mapping each key to the following value
	 */
	public static <V> FilteredStringOrderedMap<V> with (CharFilter filter, String key0, V value0, String key1, V value1, String key2, V value2) {
		FilteredStringOrderedMap<V> map = new FilteredStringOrderedMap<>(filter, 3);
		map.put(key0, value0);
		map.put(key1, value1);
		map.put(key2, value2);
		return map;
	}

	/**
	 * Constructs a single-entry map  given a CharFilter and four key-value pairs.
	 * This is mostly useful as an optimization for {@link #with(Object, Object, Object...)}
	 * when there's no "rest" of the keys or values.
	 *
	 * @param filter a CharFilter that can be obtained with {@link CharFilter#getOrCreate(String, CharPredicate, CharToCharFunction)}
	 * @param key0   a String key
	 * @param value0 a V value
	 * @param key1   a String key
	 * @param value1 a V value
	 * @param key2   a String key
	 * @param value2 a V value
	 * @param key3   a String key
	 * @param value3 a V value
	 * @param <V>    the type of value0
	 * @return a new map containing entries mapping each key to the following value
	 */
	public static <V> FilteredStringOrderedMap<V> with (CharFilter filter, String key0, V value0, String key1, V value1, String key2, V value2, String key3, V value3) {
		FilteredStringOrderedMap<V> map = new FilteredStringOrderedMap<>(filter, 4);
		map.put(key0, value0);
		map.put(key1, value1);
		map.put(key2, value2);
		map.put(key3, value3);
		return map;
	}

	/**
	 * Constructs a map given a CharFilter, then alternating keys and values.
	 * This can be useful in some code-generation scenarios, or when you want to make a
	 * map conveniently by-hand and have it populated at the start. You can also use
	 * {@link #FilteredStringOrderedMap(CharFilter, String[], Object[])}, which takes all keys and then all values.
	 * This needs all keys to have the same type and all values to have the same type, because
	 * it gets those types from the first key parameter and first value parameter. Any keys that don't
	 * have String as their type or values that don't have V as their type have that entry skipped.
	 *
	 * @param filter a CharFilter that can be obtained with {@link CharFilter#getOrCreate(String, CharPredicate, CharToCharFunction)}
	 * @param key0   the first key; will be used to determine the type of all keys
	 * @param value0 the first value; will be used to determine the type of all values
	 * @param rest   an array or varargs of alternating String, V, String, V... elements
	 * @param <V>    the type of values, inferred from value0
	 * @return a new map containing the given keys and values
	 */
	@SuppressWarnings("unchecked")
	public static <V> FilteredStringOrderedMap<V> with (CharFilter filter, String key0, V value0, Object... rest) {
		FilteredStringOrderedMap<V> map = new FilteredStringOrderedMap<>(filter, 1 + (rest.length >>> 1));
		map.put(key0, value0);
		for (int i = 1; i < rest.length; i += 2) {
			try {
				map.put((String)rest[i - 1], (V)rest[i]);
			} catch (ClassCastException ignored) {
			}
		}
		return map;
	}
}
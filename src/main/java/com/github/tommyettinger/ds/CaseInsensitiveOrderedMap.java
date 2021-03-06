package com.github.tommyettinger.ds;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;

import static com.github.tommyettinger.ds.Utilities.neverIdentical;

/**
 * A custom variant on ObjectObjectOrderedMap that always uses CharSequence keys and compares them as case-insensitive.
 * This uses a fairly complex, quite-optimized hashing function because it needs to hash CharSequences rather
 * often, and to do so ignoring case means {@link String#hashCode()} won't work, plus not all CharSequences
 * implement hashCode() themselves (such as {@link StringBuilder}). User code similar to this can often get away
 * with a simple polynomial hash (the typical Java kind, used by String and Arrays), or if more speed is needed,
 * one with <a href="https://richardstartin.github.io/posts/collecting-rocks-and-benchmarks">some of these
 * optimizations by Richard Startin</a>. If you don't want to write or benchmark a hash function (which is quite
 * understandable), {@link Utilities#longHashCodeIgnoreCase(CharSequence)} can get a case-insensitive hash of any
 * CharSequence, as a long. It does this without allocating new Strings all over, where many case-insensitive
 * algorithms do allocate quite a lot, but it does this by handling case incorrectly for the Georgian alphabet.
 * If I see Georgian text in-the-wild, I may reconsider, but I don't think that particular alphabet is in
 * widespread use. There's also {@link Utilities#equalsIgnoreCase(CharSequence, CharSequence)} for equality
 * comparisons that are similarly case-insensitive, except for Georgian. This is very similar to
 * {@link CaseInsensitiveMap}, except that this class maintains insertion order and can be sorted with
 * {@link #sort()}, {@link #sortByValue(Comparator)}, etc.
 */
public class CaseInsensitiveOrderedMap<V> extends ObjectObjectOrderedMap<CharSequence, V> {

	/**
	 * Creates a new map with an initial capacity of 51 and a load factor of 0.8.
	 */
	public CaseInsensitiveOrderedMap () {
		super();
	}
	/**
	 * Creates a new map with the given starting capacity and a load factor of 0.8.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 */
	public CaseInsensitiveOrderedMap (int initialCapacity) {
		super(initialCapacity);
	}

	/**
	 * Creates a new map with the specified initial capacity and load factor. This map will hold initialCapacity items before
	 * growing the backing table.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 * @param loadFactor what fraction of the capacity can be filled before this has to resize; 0 &lt; loadFactor &lt;= 1
	 */
	public CaseInsensitiveOrderedMap (int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
	}

	/**
	 * Creates a new map identical to the specified map.
	 * @param map an ObjectObjectOrderedMap to copy, or a subclass such as this one
	 */
	public CaseInsensitiveOrderedMap (ObjectObjectOrderedMap<? extends CharSequence, ? extends V> map) {
		super(map);
	}

	/**
	 * Creates a new map identical to the specified map.
	 * @param map a Map to copy; ObjectObjectOrderedMap and subclasses of it will be faster
	 */
	public CaseInsensitiveOrderedMap (Map<? extends CharSequence, ? extends V> map) {
		super(map);
	}

	/**
	 * Given two side-by-side arrays, one of keys, one of values, this constructs a map and inserts each pair of key and value into it.
	 * If keys and values have different lengths, this only uses the length of the smaller array.
	 *
	 * @param keys   an array of keys
	 * @param values an array of values
	 */
	public CaseInsensitiveOrderedMap (CharSequence[] keys, V[] values) {
		super(keys, values);
	}
	/**
	 * Given two side-by-side collections, one of keys, one of values, this constructs a map and inserts each pair of key and value into it.
	 * If keys and values have different lengths, this only uses the length of the smaller collection.
	 *
	 * @param keys   a Collection of keys
	 * @param values a Collection of values
	 */
	public CaseInsensitiveOrderedMap (Collection<? extends CharSequence> keys, Collection<? extends V> values) {
		super(keys, values);
	}

	@Override
	protected int place (Object item) {
		if(item instanceof CharSequence) return (int)Utilities.longHashCodeIgnoreCase((CharSequence)item) & mask;
		return super.place(item);
	}

	/**
	 * Gets a case-insensitive hash code for the String or other CharSequence {@code item} and shifts it so it is between 0 and
	 * {@link #mask} inclusive. This gets the hash as if all cased letters have been converted to upper case by
	 * {@link Character#toUpperCase(char)}; this should be correct for all alphabets in Unicode except Georgian.
	 *
	 * @param item any non-null CharSequence, such as a String or StringBuilder; will be treated as if it is all upper-case
	 * @return a position in the key table where {@code item} would be placed; between 0 and {@link #mask} inclusive
	 * @implNote Uses Water hash, which passes SMHasher's test battery and is very fast in Java. Water uses 64-bit math,
	 * which behaves reliably but somewhat slowly on GWT, but uses it on usually-small char values. This can't use the
	 * built-in pre-calculated hashCode of a String because it's case-sensitive. You can use the same hashing function as this
	 * with {@link Utilities#longHashCodeIgnoreCase(CharSequence)}.
	 */
	protected int place (CharSequence item) {
		return (int)Utilities.longHashCodeIgnoreCase(item) & mask;
	}

	@Override
	protected int locateKey (Object key) {
		Object[] keyTable = this.keyTable;
		if (!(key instanceof CharSequence))
			return super.locateKey(key);
		CharSequence sk = (CharSequence)key;
		for (int i = place(sk); ; i = i + 1 & mask) {
			Object other = keyTable[i];
			if (other == null) {
				return ~i;
			}
			if (other instanceof CharSequence && Utilities.equalsIgnoreCase(sk, (CharSequence)other)) {
				return i;
			}
		}
	}

	@Override
	public int hashCode () {
		int h = size;
		CharSequence[] keyTable = this.keyTable;
		V[] valueTable = this.valueTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			CharSequence key = keyTable[i];
			if (key != null) {
				h ^= Utilities.longHashCodeIgnoreCase(key);
				V value = valueTable[i];
				if (value != null) { h ^= value.hashCode(); }
			}
		}
		return h;
	}

	@Override
	public boolean equals (Object obj) {
		if (obj == this) { return true; }
		if (!(obj instanceof CaseInsensitiveOrderedMap)) { return false; }
		CaseInsensitiveOrderedMap other = (CaseInsensitiveOrderedMap)obj;
		if (other.size != size) { return false; }
		CharSequence[] keyTable = this.keyTable;
		V[] valueTable = this.valueTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			CharSequence key = keyTable[i];
			if (key != null) {
				V value = valueTable[i];
				if (value == null) {
					if (other.getOrDefault(key, neverIdentical) != null) { return false; }
				} else {
					if (!value.equals(other.get(key))) { return false; }
				}
			}
		}
		return true;
	}

	@Override
	public ObjectObjectMap.Keys<CharSequence, V> keySet () {
		if (keys1 == null || keys2 == null) {
			keys1 = new CaseInsensitiveMap.Keys<>(this);
			keys2 = new CaseInsensitiveMap.Keys<>(this);
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

	public static class Entry<V> extends ObjectObjectMap.Entry<CharSequence, V> {
		@Override
		public boolean equals (@Nullable Object o) {
			if (this == o) { return true; }
			if (o == null || getClass() != o.getClass()) { return false; }

			ObjectObjectMap.Entry<CharSequence, ?> entry = (ObjectObjectMap.Entry<CharSequence, ?>)o;

			if (key != null ? (entry.key == null || !Utilities.equalsIgnoreCase(key, entry.key)) : entry.key != null) { return false; }
			return Objects.equals(value, entry.value);

		}

		@Override
		public int hashCode () {
			int result = key != null ? (int)Utilities.longHashCodeIgnoreCase(key) : 0;
			result = 31 * result + (value != null ? value.hashCode() : 0);
			return result;
		}
	}

	public static class Keys<V> extends OrderedMapKeys<CharSequence, V> {
		public Keys (ObjectObjectOrderedMap<CharSequence, V> map) {
			super(map);
		}

		@Override
		public int hashCode () {
			int h = 0;
			iter.reset();
			while (iter.hasNext()) {
				CharSequence obj = iter.next();
				if (obj != null)
					h += Utilities.longHashCodeIgnoreCase(obj);
			}
			return h;
		}
	}

	/**
	 * Constructs a single-entry map given one key and one value.
	 * This is mostly useful as an optimization for {@link #with(Object, Object, Object...)}
	 * when there's no "rest" of the keys or values.
	 * @param key0 the first and only key
	 * @param value0 the first and only value
	 * @param <V> the type of value0
	 * @return a new map containing just the entry mapping key0 to value0
	 */
	public static <V> CaseInsensitiveOrderedMap<V> with(CharSequence key0, V value0) {
		CaseInsensitiveOrderedMap<V> map = new CaseInsensitiveOrderedMap<>(1);
		map.put(key0, value0);
		return map;
	}
	/**
	 * Constructs a map given alternating keys and values.
	 * This can be useful in some code-generation scenarios, or when you want to make a
	 * map conveniently by-hand and have it populated at the start. You can also use
	 * {@link #CaseInsensitiveOrderedMap(CharSequence[], Object[])}, which takes all keys and then all values.
	 * This needs all keys to be {@code CharSequence}s (like String or StringBuilder) and all values to
	 * have the same type, because it gets those types from the first value parameter. Any keys that
	 * aren't CharSequences or values that don't have V as their type have that entry skipped.
	 * @param key0 the first key; will be used to determine the type of all keys
	 * @param value0 the first value; will be used to determine the type of all values
	 * @param rest an array or varargs of alternating K, V, K, V... elements
	 * @param <V> the type of values, inferred from value0
	 * @return a new map containing the given keys and values
	 */
	@SuppressWarnings("unchecked")
	public static <V> CaseInsensitiveOrderedMap<V> with(CharSequence key0, V value0, Object... rest){
		CaseInsensitiveOrderedMap<V> map = new CaseInsensitiveOrderedMap<>(1 + (rest.length >>> 1));
		map.put(key0, value0);
		for (int i = 1; i < rest.length; i += 2) {
			try {
				map.put((CharSequence)rest[i - 1], (V)rest[i]);
			}catch (ClassCastException ignored){
			}
		}
		return map;
	}
}

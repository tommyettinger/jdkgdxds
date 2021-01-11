package com.github.tommyettinger.ds;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.*;

/**
 * A custom variant on ObjectObjectMap that always uses CharSequence keys and compares them as case-insensitive.
 * This uses a fairly complex, quite-optimized hashing function because it needs to hash CharSequences rather
 * often, and to do so ignoring case means {@link String#hashCode()} won't work, plus not all CharSequences
 * implement hashCode() themselves (such as {@link StringBuilder}). User code similar to this can often get away
 * with a simple polynomial hash (the typical Java kind, used by String and Arrays), or if more speed is needed,
 * one with <a href="https://richardstartin.github.io/posts/collecting-rocks-and-benchmarks">some of these
 * optimizations by Richard Startin</a>.
 */
public class CaseInsensitiveMap<V> extends ObjectObjectMap<CharSequence, V> implements Serializable {
	private static final long serialVersionUID = 0L;
	/**
	 * Creates a new map with an initial capacity of 51 and a load factor of 0.8.
	 */
	public CaseInsensitiveMap () {
		super();
	}

	/**
	 * Creates a new map with the specified initial capacity and load factor. This map will hold initialCapacity items before
	 * growing the backing table.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 */
	public CaseInsensitiveMap (int initialCapacity) {
		super(initialCapacity);
	}
	/**
	 * Creates a new map with the specified initial capacity and load factor. This map will hold initialCapacity items before
	 * growing the backing table.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 * @param loadFactor what fraction of the capacity can be filled before this has to resize; 0 < loadFactor <= 1
	 */
	public CaseInsensitiveMap (int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
	}

	/**
	 * Creates a new map identical to the specified map.
	 * @param map an ObjectObjectMap to copy, or a subclass such as this one
	 */
	public CaseInsensitiveMap (ObjectObjectMap<? extends CharSequence, ? extends V> map) {
		super(map);
	}

	/**
	 * Creates a new map identical to the specified map.
	 * @param map a Map to copy; ObjectObjectMap and subclasses of it will be faster
	 */
	public CaseInsensitiveMap (Map<? extends CharSequence, ? extends V> map) {
		super(map);
	}

	/**
	 * Given two side-by-side arrays, one of keys, one of values, this constructs a map and inserts each pair of key and value into it.
	 * If keys and values have different lengths, this only uses the length of the smaller array.
	 *
	 * @param keys   an array of keys
	 * @param values an array of values
	 */
	public CaseInsensitiveMap (CharSequence[] keys, V[] values) {
		super(keys, values);
	}
	/**
	 * Given two side-by-side collections, one of keys, one of values, this constructs a map and inserts each pair of key and value into it.
	 * If keys and values have different lengths, this only uses the length of the smaller collection.
	 *
	 * @param keys   a Collection of keys
	 * @param values a Collection of values
	 */
	public CaseInsensitiveMap (Collection<? extends CharSequence> keys, Collection<? extends V> values) {
		super(keys, values);
	}

	@Override
	protected int place (Object item) {
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
	public ObjectObjectMap.Keys<CharSequence, V> keySet () {
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

	public static class Entry<V> extends ObjectObjectMap.Entry<CharSequence, V> {
		@Override
		public boolean equals (@Nullable Object o) {
			if (this == o) { return true; }
			if (o == null || getClass() != o.getClass()) { return false; }

			Entry<?> entry = (Entry<?>)o;

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

	public static class Keys<V> extends ObjectObjectMap.Keys<CharSequence, V> {
		public Keys (ObjectObjectMap<CharSequence, V> map) {
			super(map);
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
		public Iterator<CharSequence> iterator () {
			return iter;
		}

		@Override
		public int size () {
			return iter.map.size;
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
}

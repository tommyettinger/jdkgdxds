package com.github.tommyettinger.ds;

import java.util.Collection;
import java.util.Map;
/**
 * A variant on {@link ObjectObjectOrderedMap} that compares keys by identity (using {@code ==}) instead of equality
 * (using {@code equals()}). It also hashes with {@link System#identityHashCode(Object)} instead of calling the
 * {@code hashCode()} of a key. This can be useful in some cases where keys may have invalid {@link Object#equals(Object)}
 * and/or {@link Object#hashCode()} implementations, or if keys could be very large (making a hashCode() that uses all of
 * the items in the key slow). Oddly, {@link System#identityHashCode(Object)} tends to be slower than the hashCode() for
 * most smaller keys, because an explicitly-written hashCode() typically doesn't need to do anything concurrently, but
 * identityHashCode() needs to (concurrently) modify an internal variable that ensures the results are unique, which
 * requires the JVM to do lots of extra work whenever identityHashCode() is called (but it doesn't depends on the quantity
 * of variables in the key, so identityHashCode() gets relatively faster for larger keys). The equals() method also tends
 * to slow down for large keys, relative to the constant-time {@code ==} this uses.
 * <br>
 * Note that the {@link #entrySet()}, {@link #keySet()} and individual Entry items this
 * produces are those of an {@link ObjectObjectOrderedMap}, and so do not compare by identity.
 */
public class IdentityObjectOrderedMap<K, V> extends ObjectObjectOrderedMap<K, V> {
	/**
	 * Creates a new map with an initial capacity of 51 and a load factor of 0.8.
	 */
	public IdentityObjectOrderedMap () {
		super();
	}

	/**
	 * Creates a new map with the given starting capacity and a load factor of 0.8.
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
	 * @param loadFactor what fraction of the capacity can be filled before this has to resize; 0 &lt; loadFactor &lt;= 1
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
	 * Returns an index &gt;= 0 and &lt;= {@link #mask} for the specified {@code item}.
	 * <p>
	 * The default behavior uses Fibonacci hashing; it simply gets the {@link System#identityHashCode(Object)}
	 * of {@code item}, multiplies it by a specific long constant related to the golden ratio,
	 * and makes an unsigned right shift by {@link #shift} before casting to int and returning.
	 * This can be overridden to hash {@code item} differently, though all implementors must
	 * ensure this returns results in the range of 0 to {@link #mask}, inclusive. If nothing
	 * else is changed, then unsigned-right-shifting an int or long by {@link #shift} will also
	 * restrict results to the correct range.
	 *
	 * @param item a non-null Object; its identityHashCode is used here
	 */
	@Override
	protected int place (Object item) {
		return (System.identityHashCode(item) & mask);
	}

	/**
	 * Returns the index of the key if already present, else {@code ~index} for the next empty index. This can be overridden
	 * to compare for equality differently than by using {@code ==}, as this does.
	 *
	 * @param key a non-null Object that should probably be a K
	 */
	@Override
	protected int locateKey (Object key) {
		K[] keyTable = this.keyTable;
		for (int i = place(key); ; i = i + 1 & mask) {
			K other = keyTable[i];
			if (other == null) {
				return ~i; // Always negative; means empty space is available at i.
			}
			if (other == key) // If you want to change how equality is determined, do it here.
			{
				return i; // Same key was found.
			}
		}
	}

	@Override
	public boolean equals (Object obj) {
		return super.equalsIdentity(obj);
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
				if (value != null) { h ^= value.hashCode(); }
			}
		}
		return h;
	}

}

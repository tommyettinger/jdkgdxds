package com.github.tommyettinger.ds;

import java.util.Collection;
import java.util.Map;

public class IdentityObjectOrderedMap<K, V> extends ObjectObjectOrderedMap<K, V>{
	public IdentityObjectOrderedMap () {
		super();
	}

	public IdentityObjectOrderedMap (int initialCapacity) {
		super(initialCapacity);
	}

	public IdentityObjectOrderedMap (int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
	}

	public IdentityObjectOrderedMap (ObjectObjectOrderedMap<? extends K, ? extends V> map) {
		super(map);
	}

	public IdentityObjectOrderedMap (Map<? extends K, ? extends V> map) {
		super(map);
	}

	public IdentityObjectOrderedMap (K[] keys, V[] values) {
		super(keys, values);
	}

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
		return (int)(System.identityHashCode(item) * 0x9E3779B97F4A7C15L >>> shift);
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

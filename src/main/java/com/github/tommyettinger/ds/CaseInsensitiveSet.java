package com.github.tommyettinger.ds;

import java.io.Serializable;
import java.util.Collection;
/**
 * A custom variant on ObjectSet that always uses CharSequence keys and compares them as case-insensitive.
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
 * comparisons that are similarly case-insensitive, except for Georgian.
 */
public class CaseInsensitiveSet extends ObjectSet<CharSequence> implements Serializable {
	private static final long serialVersionUID = 0L;

	/**
	 * Creates a new set with an initial capacity of 51 and a load factor of 0.8.
	 */
	public CaseInsensitiveSet () {
		super();
	}

	/**
	 * Creates a new set with a load factor of 0.8.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 */
	public CaseInsensitiveSet (int initialCapacity) {
		super(initialCapacity);
	}

	/**
	 * Creates a new set with the specified initial capacity and load factor. This set will hold initialCapacity items before
	 * growing the backing table.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 * @param loadFactor      what fraction of the capacity can be filled before this has to resize; 0 &lt; loadFactor &lt;= 1
	 */
	public CaseInsensitiveSet (int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
	}

	/**
	 * Creates a new set identical to the specified set.
	 *
	 * @param set
	 */
	public CaseInsensitiveSet (ObjectSet<? extends CharSequence> set) {
		super(set);
	}

	/**
	 * Creates a new set that contains all distinct elements in {@code coll}.
	 *
	 * @param coll
	 */
	public CaseInsensitiveSet (Collection<? extends CharSequence> coll) {
		super(coll);
	}

	/**
	 * Creates a new set using {@code length} items from the given {@code array}, starting at {@code} offset (inclusive).
	 *
	 * @param array  an array to draw items from
	 * @param offset the first index in array to draw an item from
	 * @param length how many items to take from array; bounds-checking is the responsibility of the using code
	 */
	public CaseInsensitiveSet (CharSequence[] array, int offset, int length) {
		super(array, offset, length);
	}

	/**
	 * Creates a new set containing all of the items in the given array.
	 *
	 * @param array an array that will be used in full, except for duplicate items
	 */
	public CaseInsensitiveSet (CharSequence[] array) {
		super(array);
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

	/**
	 * Returns the index of the key if already present, else {@code ~index} for the next empty index. This can be overridden
	 * to compare for equality differently than {@link Object#equals(Object)}.
	 * <p>
	 * If source is not easily available and you want to override this, the reference source is:
	 * <pre>
	 * protected int locateKey (Object key) {
	 * 	   if (!(key instanceof CharSequence))
	 * 	       return super.locateKey(key);
	 *     CharSequence sk = (CharSequence)key;
	 * 	   CharSequence[] keyTable = this.keyTable;
	 * 	   for (int i = place(sk); ; i = i + 1 &amp; mask) {
	 * 	       CharSequence other = keyTable[i];
	 *         if (other == null) {
	 *             return ~i; // Always negative; means empty space is available at i.
	 *         }
	 *         if (Utilities.equalsIgnoreCase(other, sk)) // If you want to change how equality is determined, do it here.
	 *         {
	 *             return i; // Same key was found.
	 *         }
	 *     }
	 * }
	 * </pre>
	 *
	 * @param key a non-null Object that should probably be a CharSequence
	 */
	@Override
	protected int locateKey (Object key) {
		if (!(key instanceof CharSequence))
			return super.locateKey(key);
		CharSequence sk = (CharSequence)key;
		Object[] keyTable = this.keyTable;
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
		for (int i = 0, n = keyTable.length; i < n; i++) {
			CharSequence key = keyTable[i];
			if (key != null) { h += Utilities.longHashCodeIgnoreCase(key); }
		}
		return h;
	}

	@Override
	public boolean equals (Object obj) {
		if (!(obj instanceof CaseInsensitiveSet)) { return false; }
		CaseInsensitiveSet other = (CaseInsensitiveSet)obj;
		if (other.size != size) { return false; }
		CharSequence[] keyTable = this.keyTable;
		for (int i = 0, n = keyTable.length; i < n; i++) { if (keyTable[i] != null && !other.contains(keyTable[i])) { return false; } }
		return true;
	}

	public static CaseInsensitiveSet with (CharSequence... array) {
		return new CaseInsensitiveSet(array);
	}

}

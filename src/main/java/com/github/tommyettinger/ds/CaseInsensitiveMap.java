package com.github.tommyettinger.ds;

import java.io.Serializable;
import java.util.Map;

/**
 * A custom variant on ObjectObjectMap that always uses CharSequence keys and compares them as case-insensitive.
 * This uses a fairly complex, somewhat-optimized hashing function because it needs to hash CharSequences rather
 * often, and to do so ignoring case means {@link String#hashCode()} won't work, plus not all CharSequences
 * implement hashCode() themselves (such as {@link StringBuilder}). User code similar to this can often get away
 * with a simple polynomial hash (the typical Java kind, used by String and Arrays), or if more speed is needed,
 * one with <a href="https://richardstartin.github.io/posts/collecting-rocks-and-benchmarks">some of these
 * optimizations by Richard Startin</a>.
 */
public class CaseInsensitiveMap<V> extends ObjectObjectMap<CharSequence, V> implements Serializable {
	private static final long serialVersionUID = 0L;

	public CaseInsensitiveMap () {
		super();
	}

	public CaseInsensitiveMap (int initialCapacity) {
		super(initialCapacity);
	}

	public CaseInsensitiveMap (int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
	}

	public CaseInsensitiveMap (ObjectObjectMap<? extends CharSequence, ? extends V> map) {
		super(map);
	}

	public CaseInsensitiveMap (Map<? extends CharSequence, ? extends V> map) {
		super(map);
	}

	@Override
	protected int place (Object item) {
		return super.place(item);
	}
	
	/**
	 * Gets a case-insensitive hash code for the String or other CharSequence {@code item} and shifts it so it is between 0 and
	 * {@link #mask} inclusive. This gets the hash as if all cased letters have been converted to upper case by
	 * {@link Character#toUpperCase(char)}; this should be correct for all alphabets in Unicode except Georgian.
	 * @implNote Uses Frost hash, which passes SMHasher's test battery and is fairly fast, at least in C. Frost uses 64-bit math,
	 * which behaves reliably but somewhat slowly on GWT, but uses it on usually-small char values. This can't use the
	 * built-in pre-calculated hashCode of a String because it's case-sensitive.
	 * @param item any non-null CharSequence, such as a String or StringBuilder; will be treated as if it is all upper-case
	 * @return a position in the key table where {@code item} would be placed; between 0 and {@link #mask} inclusive
	 */
	protected int place(CharSequence item) {
		final int len = item.length();
		if(len == 0) return 0;
		long h = len ^ 0xC6BC279692B5C323L, m = 0xDB4F0B9175AE2165L ^ h << 1, t, r;
		for (int i = 0; i < len; i++) {
			t = (0x3C79AC492BA7B653L + Character.toUpperCase(item.charAt(i))) * m;
			r = (m += 0x95B534A1ACCD52DAL) >>> 58;
			h ^= t << r | t >>> -r;
		}
		// Pelican unary hash, with a different last step that adapts to different shift values.
		h = (h ^ (h << 41 | h >>> 23) ^ (h << 17 | h >>> 47) ^ 0xD1B54A32D192ED03L) * 0xAEF17502108EF2D9L;
		return (int)((h ^ h >>> 43 ^ h >>> 31 ^ h >>> 23) * 0xDB4F0B9175AE2165L >>> shift);
	}
	
	@Override
	protected int locateKey (Object key) {
		Object[] keyTable = this.keyTable;
		if(!(key instanceof CharSequence))
			return super.locateKey(key);
		CharSequence sk = (CharSequence)key;
		for (int i = place(sk); ; i = i + 1 & mask) {
			Object other = keyTable[i];
			if (other == null) {
				return ~i;
			}
			if (other instanceof CharSequence && Utilities.equalsIgnoreCase(sk, (CharSequence)other))
			{
				return i;
			}
		}
	}
	
}

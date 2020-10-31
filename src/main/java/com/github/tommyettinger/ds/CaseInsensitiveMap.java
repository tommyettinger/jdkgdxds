package com.github.tommyettinger.ds;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

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
		return (int)(Utilities.longHashCodeIgnoreCase(item) >>> shift);
	}

	@Override
	public int hashCode () {
		int h = size;
		CharSequence[] keyTable = this.keyTable;
		V[] valueTable = this.valueTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			CharSequence key = keyTable[i];
			if (key != null) {
				h ^= Utilities.longHashCodeIgnoreCase(key) >>> 32;
				V value = valueTable[i];
				if (value != null) { h ^= value.hashCode(); }
			}
		}
		return h;

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
	public static class Entry<V> extends ObjectObjectMap.Entry<CharSequence, V> {
		@Override
		public boolean equals (@Nullable Object o) {
			if (this == o) { return true; }
			if (o == null || getClass() != o.getClass()) { return false; }

			Entry<?> entry = (Entry<?>)o;

			if (key != null ? (entry.key == null || !Utilities.equalsIgnoreCase(key, entry.key)) : entry.key != null) { return false; }
			return value != null ? value.equals(entry.value) : entry.value == null;

		}

		@Override
		public int hashCode () {
			int result = key != null ? (int)(Utilities.longHashCodeIgnoreCase(key) >>> 32) : 0;
			result = 31 * result + (value != null ? value.hashCode() : 0);
			return result;
		}
	}
	public static class Entries<V> extends AbstractSet<Entry<V>> {
		protected Entry<V> entry = new Entry<>();
		protected MapIterator<CharSequence, V, Entry<V>> iter;

		public Entries (ObjectObjectMap<CharSequence, V> map) {
			iter = new MapIterator<CharSequence, V, Entry<V>>(map) {
				@Override
				public Iterator<Entry<V>> iterator () {
					return this;
				}

				/** Note the same entry instance is returned each time this method is called. 
				 * @return a reused Entry instance, filled with the next Entry in no particular order
				 */
				@Override
				public Entry<V> next () {
					if (!hasNext) { throw new NoSuchElementException(); }
					if (!valid) { throw new RuntimeException("#iterator() cannot be used nested."); }
					CharSequence[] keyTable = map.keyTable;
					entry.key = keyTable[nextIndex];
					entry.value = map.valueTable[nextIndex];
					currentIndex = nextIndex;
					findNextIndex();
					return entry;
				}

				@Override
				public boolean hasNext () {
					if (!valid) { throw new RuntimeException("#iterator() cannot be used nested."); }
					return hasNext;
				}
			};
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
		public Iterator<Entry<V>> iterator () {
			return iter;
		}

		@Override
		public int size () {
			return iter.map.size;
		}
	}
	public static class Keys<V> extends AbstractSet<CharSequence> {
		protected MapIterator<CharSequence, V, CharSequence> iter;

		public Keys (ObjectObjectMap<CharSequence, V> map) {
			iter = new MapIterator<CharSequence, V, CharSequence>(map) {
				@Override
				public Iterator<CharSequence> iterator () {
					return this;
				}

				@Override
				public boolean hasNext () {
					if (!valid) { throw new RuntimeException("#iterator() cannot be used nested."); }
					return hasNext;
				}

				@Override
				public CharSequence next () {
					if (!hasNext) { throw new NoSuchElementException(); }
					if (!valid) { throw new RuntimeException("#iterator() cannot be used nested."); }
					CharSequence key = map.keyTable[nextIndex];
					currentIndex = nextIndex;
					findNextIndex();
					return key;
				}
			};
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
					h += Utilities.longHashCodeIgnoreCase(obj) >>> 32;
			}
			return h;
		}
	}

}

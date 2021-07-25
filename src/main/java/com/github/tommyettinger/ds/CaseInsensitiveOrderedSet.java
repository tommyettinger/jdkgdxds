package com.github.tommyettinger.ds;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Comparator;
import java.util.Set;

/**
 * A custom variant on ObjectOrderedSet that always uses CharSequence keys and compares them as case-insensitive.
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
 * {@link CaseInsensitiveSet}, except that this class maintains insertion order and can be sorted with
 * {@link #sort()}, {@link #sort(Comparator)}, etc.
 */
public class CaseInsensitiveOrderedSet extends ObjectOrderedSet<CharSequence> {


	/**
	 * Creates a new set with an initial capacity of 51 and a load factor of 0.8.
	 */
	public CaseInsensitiveOrderedSet () {
		super();
	}

	/**
	 * Creates a new set with a load factor of 0.8.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 */
	public CaseInsensitiveOrderedSet (int initialCapacity) {
		super(initialCapacity);
	}

	/**
	 * Creates a new set with the specified initial capacity and load factor. This set will hold initialCapacity items before
	 * growing the backing table.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 * @param loadFactor      what fraction of the capacity can be filled before this has to resize; 0 &lt; loadFactor &lt;= 1
	 */
	public CaseInsensitiveOrderedSet (int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
	}

	/**
	 * Creates a new set identical to the specified set.
	 *
	 * @param set an ObjectSet or one of its subclasses; ObjectOrderedSet uses a different constructor
	 */
	public CaseInsensitiveOrderedSet (ObjectSet<? extends CharSequence> set) {
		super(set);
	}

	/**
	 * Creates a new ordered set identical to the specified ordered set.
	 * Typically this would take another CaseInsensitiveOrderedSet, but you can use an ObjectOrderedSet
	 * or one of its other subclasses as well.
	 * @param set an ObjectOrderedSet or one of its subclasses, such as a CaseInsensitiveOrderedSet
	 */
	public CaseInsensitiveOrderedSet (ObjectOrderedSet<? extends CharSequence> set) {
		super(set);
	}

	/**
	 * Creates a new set that contains all distinct elements in {@code coll}.
	 *
	 * @param coll a Collection implementation, such as an ObjectList
	 */
	public CaseInsensitiveOrderedSet (Collection<? extends CharSequence> coll) {
		super(coll);
	}

	/**
	 * Creates a new set using {@code length} items from the given {@code array}, starting at {@code} offset (inclusive).
	 *
	 * @param array  an array to draw items from
	 * @param offset the first index in array to draw an item from
	 * @param length how many items to take from array; bounds-checking is the responsibility of the using code
	 */
	public CaseInsensitiveOrderedSet (CharSequence[] array, int offset, int length) {
		super(array, offset, length);
	}

	/**
	 * Creates a new set containing all of the items in the given array.
	 *
	 * @param array an array that will be used in full, except for duplicate items
	 */
	public CaseInsensitiveOrderedSet (CharSequence[] array) {
		super(array);
	}

	@Override
	protected int place (Object item) {
		if(item instanceof CharSequence) return (int)Utilities.longHashCodeIgnoreCase((CharSequence)item) & mask;
		return super.place(item);
	}

	@Override
	protected boolean equate (@Nonnull Object left, @Nullable Object right) {
		if((left instanceof CharSequence) && (right instanceof CharSequence)){
			return Utilities.equalsIgnoreCase((CharSequence)left, (CharSequence)right);
		}
		return false;
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
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof Set))
			return false;
		Set<?> s = (Set<?>) o;
		if (s.size() != size())
			return false;
		try {
			return containsAll(s);
		} catch (ClassCastException | NullPointerException unused) {
			return false;
		}
	}

	public static CaseInsensitiveOrderedSet with(CharSequence item) {
		CaseInsensitiveOrderedSet set = new CaseInsensitiveOrderedSet(1);
		set.add(item);
		return set;
	}

	public static CaseInsensitiveOrderedSet with (CharSequence... array) {
		return new CaseInsensitiveOrderedSet(array);
	}

}

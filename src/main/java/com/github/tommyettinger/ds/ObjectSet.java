/*
 * Copyright (c) 2022 See AUTHORS file.
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

import org.checkerframework.checker.nullness.qual.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import static com.github.tommyettinger.ds.Utilities.tableSize;

/**
 * An unordered set where the keys are objects. Null keys are not allowed. No allocation is done except when growing the table
 * size.
 * <p>
 * This class performs fast contains and remove (typically O(1), worst case O(n) but that is rare in practice). Add may be
 * slightly slower, depending on hash collisions. Hashcodes are rehashed to reduce collisions and the need to resize. Load factors
 * greater than 0.91 greatly increase the chances to resize to the next higher POT size.
 * <p>
 * Unordered sets and maps are not designed to provide especially fast iteration. Iteration is faster with {@link Ordered} types like
 * ObjectOrderedSet and ObjectObjectOrderedMap.
 * <p>
 * You can customize most behavior of this map by extending it. {@link #place(Object)} can be overridden to change how hashCodes
 * are calculated (which can be useful for types like {@link StringBuilder} that don't implement hashCode()), and
 * {@link #equate(Object, Object)} can be overridden to change how equality is calculated.
 * <p>
 * This implementation uses linear probing with the backward shift algorithm for removal.
 * It tries different hashes from a simple family, with the hash changing on resize.
 * Linear
 * probing continues to work even when all hashCodes collide; it just works more slowly in that case.
 *
 * @author Nathan Sweet
 * @author Tommy Ettinger
 */
public class ObjectSet<T> implements Iterable<T>, Set<T> {

	protected int size;

	protected T[] keyTable;

	/**
	 * Between 0f (exclusive) and 1f (inclusive, if you're careful), this determines how full the backing table
	 * can get before this increases its size. Larger values use less memory but make the data structure slower.
	 */
	protected float loadFactor;

	/**
	 * Precalculated value of {@code (int)(keyTable.length * loadFactor)}, used to determine when to resize.
	 */
	protected int threshold;

	/**
	 * Used by {@link #place(Object)} typically, this should always equal {@code Long.numberOfLeadingZeros(mask)}.
	 * For a table that could hold 2 items (with 1 bit indices), this would be {@code 64 - 1 == 63}. For a table that
	 * could hold 256 items (with 8 bit indices), this would be {@code 64 - 8 == 56}.
	 */
	protected int shift;

	/**
	 * Used by {@link #place(Object)} to mix hashCode() results. Changes on every call to {@link #resize(int)} by default.
	 * This only needs to be serialized if the full key and value tables are serialized, or if the iteration order should be
	 * the same before and after serialization. Iteration order is better handled by using {@link ObjectOrderedSet}.
	 */
	protected long hashMultiplier = 0xD1B54A32D192ED03L;

	/**
	 * A bitmask used to confine hashcodes to the size of the table. Must be all 1 bits in its low positions, ie a power of two
	 * minus 1. If {@link #place(Object)} is overridden, this can be used instead of {@link #shift} to isolate usable bits of a
	 * hash.
	 */
	protected int mask;
	@Nullable protected transient ObjectSetIterator<T> iterator1;
	@Nullable protected transient ObjectSetIterator<T> iterator2;

	/**
	 * Creates a new set with an initial capacity of 51 and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 */
	public ObjectSet () {
		this(51, Utilities.getDefaultLoadFactor());
	}

	/**
	 * Creates a new set with a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 */
	public ObjectSet (int initialCapacity) {
		this(initialCapacity, Utilities.getDefaultLoadFactor());
	}

	/**
	 * Creates a new set with the specified initial capacity and load factor. This set will hold initialCapacity items before
	 * growing the backing table.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 * @param loadFactor      what fraction of the capacity can be filled before this has to resize; 0 &lt; loadFactor &lt;= 1
	 */
	public ObjectSet (int initialCapacity, float loadFactor) {
		if (loadFactor <= 0f || loadFactor > 1f) {throw new IllegalArgumentException("loadFactor must be > 0 and <= 1: " + loadFactor);}
		this.loadFactor = loadFactor;

		int tableSize = tableSize(initialCapacity, loadFactor);
		threshold = (int)(tableSize * loadFactor);
		mask = tableSize - 1;
		shift = Long.numberOfLeadingZeros(mask);

		keyTable = (T[])new Object[tableSize];
	}

	/**
	 * Creates a new set identical to the specified set.
	 */
	public ObjectSet (ObjectSet<? extends T> set) {
		loadFactor = set.loadFactor;
		threshold = set.threshold;
		mask = set.mask;
		shift = set.shift;
		keyTable = Arrays.copyOf(set.keyTable, set.keyTable.length);
		size = set.size;
		hashMultiplier = set.hashMultiplier;

	}

	/**
	 * Creates a new set that contains all distinct elements in {@code coll}.
	 */
	public ObjectSet (Collection<? extends T> coll) {
		this(coll.size());
		addAll(coll);
	}

	/**
	 * Creates a new set using {@code length} items from the given {@code array}, starting at {@code} offset (inclusive).
	 *
	 * @param array  an array to draw items from
	 * @param offset the first index in array to draw an item from
	 * @param length how many items to take from array; bounds-checking is the responsibility of the using code
	 */
	public ObjectSet (T[] array, int offset, int length) {
		this(length);
		addAll(array, offset, length);
	}

	/**
	 * Creates a new set containing all of the items in the given array.
	 *
	 * @param array an array that will be used in full, except for duplicate items
	 */
	public ObjectSet (T[] array) {
		this(array, 0, array.length);
	}

	/**
	 * Returns an index &gt;= 0 and &lt;= {@link #mask} for the specified {@code item}, mixed.
	 * <p>
	 * The default behavior uses a basic hash mixing family; it simply gets the
	 * {@link Object#hashCode()} of {@code item}, multiplies it by the current
	 * {@link #hashMultiplier}, and makes an unsigned right shift by {@link #shift} before
	 * casting to int and returning. Because the hashMultiplier changes every time the backing
	 * table resizes, if a problematic sequence of keys piles up with many collisions, that won't
	 * continue to cause problems when the next resize changes the hashMultiplier again. This
	 * doesn't have much way of preventing trouble from hashCode() implementations that always
	 * or very frequently return 0, but nothing really can handle that well.
	 * <br>
	 * This can be overridden to hash {@code item} differently, though all implementors must
	 * ensure this returns results in the range of 0 to {@link #mask}, inclusive. If nothing
	 * else is changed, then unsigned-right-shifting an int or long by {@link #shift} will also
	 * restrict results to the correct range. You should usually override this method
	 * if you also override {@link #equate(Object, Object)}, because two equal values should have
	 * the same hash. If you are confident that the hashCode() implementation used by item will
	 * have reasonable quality, you can override this with a simpler implementation, such as
	 * {@code return item.hashCode() & mask;}. This simpler version is not used by default, even
	 * though it can be slightly faster, because the default hashing family provides much
	 * better resilience against high collision rates when they occur accidentally. If collision
	 * rates are high on the low bits of many hashes, then the simpler version tends to be
	 * significantly slower than the hashing family. Neither version provides stronger defenses
	 * against maliciously-chosen items, but linear probing naturally won't fail entirely even in
	 * that case. It is possible that a user could write an implementation of place() that is more
	 * robust against malicious inputs; one such approach is optionally employed by .NET Core and
	 * newer versions for the hashes of strings. That approach is similar to the current one here.
	 *
	 * @param item a non-null Object; its hashCode() method should be used by most implementations
	 * @return an index between 0 and {@link #mask} (both inclusive)
	 */
	protected int place (Object item) {
		return (int)(item.hashCode() * hashMultiplier >>> shift);
		// This can be used if you know hashCode() has few collisions normally, and won't be maliciously manipulated.
//		return item.hashCode() & mask;
	}

	/**
	 * Compares the objects left and right, which are usually keys, for equality, returning true if they are considered
	 * equal. This is used by the rest of this class to determine whether two keys are considered equal. Normally, this
	 * returns {@code left.equals(right)}, but subclasses can override it to use reference equality, fuzzy equality, deep
	 * array equality, or any other custom definition of equality. Usually, {@link #place(Object)} is also overridden if
	 * this method is.
	 *
	 * @param left  must be non-null; typically a key being compared, but not necessarily
	 * @param right may be null; typically a key being compared, but can often be null for an empty key slot, or some other type
	 * @return true if left and right are considered equal for the purposes of this class
	 */
	protected boolean equate (Object left, @Nullable Object right) {
		return left.equals(right);
	}

	/**
	 * Returns the index of the key if already present, else {@code ~index} for the next empty index. This calls
	 * {@link #equate(Object, Object)} to determine if two keys are equivalent.
	 *
	 * @param key a non-null K key
	 * @return a negative index if the key was not found, or the non-negative index of the existing key if found
	 */
	protected int locateKey (Object key) {
		T[] keyTable = this.keyTable;
		for (int i = place(key); ; i = i + 1 & mask) {
			T other = keyTable[i];
			if (equate(key, other))
				return i; // Same key was found.
			if (other == null)
				return ~i; // Always negative; means empty space is available at i.
		}
	}

	/**
	 * Returns true if the key was not already in the set. If this set already contains the key, the call leaves the set unchanged
	 * and returns false.
	 */
	@Override
	public boolean add (T key) {
		T[] keyTable = this.keyTable;
		for (int i = place(key); ; i = i + 1 & mask) {
			T other = keyTable[i];
			if (equate(key, other))
				return false; // Existing key was found.
			if (other == null) {
				keyTable[i] = key;
				if (++size >= threshold) {resize(keyTable.length << 1);}
				return true;
			}
		}
	}

	@Override
	public boolean containsAll (Collection<?> c) {
		for (Object o : c) {
			if (!contains(o)) {return false;}
		}
		return true;
	}

	@Override
	public boolean addAll (Collection<? extends T> coll) {
		final int length = coll.size();
		ensureCapacity(length);
		int oldSize = size;
		for (T t : coll) {add(t);}
		return oldSize != size;

	}

	@Override
	public boolean retainAll (Collection<?> c) {
		boolean modified = false;
		for (Object o : this) {
			if (!c.contains(o)) {modified |= remove(o);}
		}
		return modified;
	}

	@Override
	public boolean removeAll (Collection<?> c) {
		boolean modified = false;
		for (Object o : c) {
			modified |= remove(o);
		}
		return modified;
	}

	public boolean addAll (T[] array) {
		return addAll(array, 0, array.length);
	}

	public boolean addAll (T[] array, int offset, int length) {
		ensureCapacity(length);
		int oldSize = size;
		for (int i = offset, n = i + length; i < n; i++) {add(array[i]);}
		return oldSize != size;
	}

	public boolean addAll (ObjectSet<T> set) {
		ensureCapacity(set.size);
		T[] keyTable = set.keyTable;
		int oldSize = size;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			T key = keyTable[i];
			if (key != null) {add(key);}
		}
		return size != oldSize;
	}

	/**
	 * Like {@link #add(Object)}, but skips checks for existing keys, and doesn't increment size.
	 */
	protected void addResize (T key) {
		T[] keyTable = this.keyTable;
		for (int i = place(key); ; i = i + 1 & mask) {
			if (keyTable[i] == null) {
				keyTable[i] = key;
				return;
			}
		}
	}

	/**
	 * Returns true if the key was removed.
	 */
	@Override
	public boolean remove (Object key) {
		int i = locateKey(key);
		if (i < 0) {return false;}
		T[] keyTable = this.keyTable;
		int mask = this.mask, next = i + 1 & mask;
		while ((key = keyTable[next]) != null) {
			int placement = place(key);
			if ((next - placement & mask) > (i - placement & mask)) {
				keyTable[i] = (T)key;
				i = next;
			}
			next = next + 1 & mask;
		}
		keyTable[i] = null;
		size--;
		return true;
	}

	/**
	 * Returns true if the set has one or more items.
	 */
	public boolean notEmpty () {
		return size > 0;
	}

	/**
	 * Returns the number of elements in this set (its cardinality).  If this
	 * set contains more than {@code Integer.MAX_VALUE} elements, returns
	 * {@code Integer.MAX_VALUE}.
	 *
	 * @return the number of elements in this set (its cardinality)
	 */
	@Override
	public int size () {
		return size;
	}

	/**
	 * Returns true if the set is empty.
	 */
	@Override
	public boolean isEmpty () {
		return size == 0;
	}

	/**
	 * Reduces the size of the backing arrays to be the specified capacity / loadFactor, or less. If the capacity is already less,
	 * nothing is done. If the set contains more items than the specified capacity, the next highest power of two capacity is used
	 * instead.
	 */
	public void shrink (int maximumCapacity) {
		if (maximumCapacity < 0) {throw new IllegalArgumentException("maximumCapacity must be >= 0: " + maximumCapacity);}
		int tableSize = tableSize(Math.max(maximumCapacity, size), loadFactor);
		if (keyTable.length > tableSize) {resize(tableSize);}
	}

	/**
	 * Clears the set and reduces the size of the backing arrays to be the specified capacity / loadFactor, if they are larger.
	 * The reduction is done by allocating new arrays, though for large arrays this can be faster than clearing the existing
	 * array.
	 */
	public void clear (int maximumCapacity) {
		int tableSize = tableSize(maximumCapacity, loadFactor);
		if (keyTable.length <= tableSize) {
			clear();
			return;
		}
		size = 0;
		resize(tableSize);
	}

	/**
	 * Clears the set, leaving the backing arrays at the current capacity. When the capacity is high and the population is low,
	 * iteration can be unnecessarily slow. {@link #clear(int)} can be used to reduce the capacity.
	 */
	@Override
	public void clear () {
		if (size == 0) {return;}
		size = 0;
		Arrays.fill(keyTable, null);
	}

	@Override
	public boolean contains (Object key) {
		T[] keyTable = this.keyTable;
		for (int i = place(key); ; i = i + 1 & mask) {
			T other = keyTable[i];
			if (equate(key, other))
				return true;
			if (other == null)
				return false;
		}
	}

	@Nullable
	public T get (T key) {
		T[] keyTable = this.keyTable;
		for (int i = place(key); ; i = i + 1 & mask) {
			T other = keyTable[i];
			if (equate(key, other))
				return other;
			if (other == null)
				return null;
		}
	}

	public T first () {
		T[] keyTable = this.keyTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {if (keyTable[i] != null) {return keyTable[i];}}
		throw new IllegalStateException("ObjectSet is empty.");
	}

	/**
	 * Increases the size of the backing array to accommodate the specified number of additional items / loadFactor. Useful before
	 * adding many items to avoid multiple backing array resizes.
	 *
	 * @param additionalCapacity how many additional items this should be able to hold without resizing (probably)
	 */
	public void ensureCapacity (int additionalCapacity) {
		int tableSize = tableSize(size + additionalCapacity, loadFactor);
		if (keyTable.length < tableSize) {resize(tableSize);}
	}

	protected void resize (int newSize) {
		int oldCapacity = keyTable.length;
		threshold = (int)(newSize * loadFactor);
		mask = newSize - 1;
		shift = Long.numberOfLeadingZeros(mask);

hashMultiplier = Utilities.GOOD_MULTIPLIERS[(int)(hashMultiplier >>> 27) + shift & 511];
		T[] oldKeyTable = keyTable;

		keyTable = (T[])new Object[newSize];

		if (size > 0) {
			for (int i = 0; i < oldCapacity; i++) {
				T key = oldKeyTable[i];
				if (key != null) {addResize(key);}
			}
		}
	}

	/**
	 * Gets the current hash multiplier as used by {@link #place(Object)}; for specific advanced usage only.
	 * The hash multiplier changes whenever {@link #resize(int)} is called, though its value before the resize
	 * affects its value after.
	 * @return the current hash multiplier, which should always be a large odd long
	 */
	public long getHashMultiplier () {
		return hashMultiplier;
	}

	/**
	 * Sets the current hash multiplier, then immediately calls {@link #resize(int)} without changing the target size; this
	 * is for specific advanced usage only. Calling resize() will change the multiplier before it gets used, and the current
	 * {@link #size()} of the data structure also changes the value. The hash multiplier is used by {@link #place(Object)}.
	 * The hash multiplier must be an odd long, and should usually be "rather large." Here, that means the absolute value of
	 * the multiplier should be at least a quadrillion or so (a million billions, or roughly {@code 0x4000000000000L}). The
	 * only validation this does is to ensure the multiplier is odd; everything else is up to the caller. The hash multiplier
	 * changes whenever {@link #resize(int)} is called, though its value before the resize affects its value after. Because
	 * of how resize() randomizes the multiplier, even inputs such as {@code 1L} and {@code -1L} actually work well.
	 * <br>
	 * This is accessible at all mainly so serialization code that has a need to access the hash multiplier can do so, but
	 * also to provide an "emergency escape route" in case of hash flooding. Using one of the "golden longs" in
	 * {@link com.github.tommyettinger.digital.MathTools#GOLDEN_LONGS} should usually be fine if you don't know what
	 * multiplier will work well. Be advised that because this has to call resize(), it isn't especially fast, and it slows
	 * down the more items are in the data structure. If you in a situation where you are worried about hash flooding, you
	 * also shouldn't permit adversaries to cause this method to be called frequently.
	 * @param hashMultiplier any odd long; will not be used as-is
	 */
	public void setHashMultiplier (long hashMultiplier) {
		this.hashMultiplier = hashMultiplier | 1L;
		resize(keyTable.length);
	}

	@Override
	public Object[] toArray () {
		return toArray(new Object[size()]);
	}

	/**
	 * Returns an array containing all of the elements in this set; the
	 * runtime type of the returned array is that of the specified array.
	 * If the set fits in the specified array, it is returned therein.
	 * Otherwise, a new array is allocated with the runtime type of the
	 * specified array and the size of this set.
	 * <br>
	 * Implementation is mostly copied from GWT, but uses Arrays.copyOf() instead of their internal APIs.
	 *
	 * @param a   the array into which the elements of this set are to be
	 *            stored, if it is big enough; otherwise, a new array of the same
	 *            runtime type is allocated for this purpose.
	 * @param <E> must be the same as {@code T} or a superclass/interface of it; not checked
	 * @return an array containing all the elements in this set
	 */
	@Override
	public <E> E[] toArray (E[] a) {
		int size = size();
		if (a.length < size) {
			a = Arrays.copyOf(a, size);
		}
		Object[] result = a;
		Iterator<T> it = iterator();
		for (int i = 0; i < size; ++i) {
			result[i] = it.next();
		}
		if (a.length > size) {
			a[size] = null;
		}
		return a;
	}

	public float getLoadFactor () {
		return loadFactor;
	}

	public void setLoadFactor (float loadFactor) {
		if (loadFactor <= 0f || loadFactor > 1f) {throw new IllegalArgumentException("loadFactor must be > 0 and <= 1: " + loadFactor);}
		this.loadFactor = loadFactor;
		int tableSize = tableSize(size, loadFactor);
		if (tableSize - 1 != mask) {
			resize(tableSize);
		}
	}

	@Override
	public int hashCode () {
		int h = size;
		T[] keyTable = this.keyTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			T key = keyTable[i];
			if (key != null) {h += key.hashCode();}
		}
		return h;
	}

	@Override
	public boolean equals (Object o) {
		if (o == this)
			return true;
		if (!(o instanceof Set))
			return false;
		Set<?> s = (Set<?>)o;
		if (s.size() != size())
			return false;
		try {
			return containsAll(s);
		} catch (ClassCastException | NullPointerException unused) {
			return false;
		}
	}
	public StringBuilder appendTo (StringBuilder builder, String separator) {
		if (size == 0) {return builder;}
		T[] keyTable = this.keyTable;
		int i = keyTable.length;
		while (i-- > 0) {
			T key = keyTable[i];
			if (key == null) {continue;}
			builder.append(key == this ? "(this)" : key);
			break;
		}
		while (i-- > 0) {
			T key = keyTable[i];
			if (key == null) {continue;}
			builder.append(separator);
			builder.append(key == this ? "(this)" : key);
		}
		return builder;
	}

	@Override
	public String toString () {
		return appendTo(new StringBuilder(32).append('['), ", ").append(']').toString();
	}

	public String toString (String separator) {
		return appendTo(new StringBuilder(32), separator).toString();
	}

	/**
	 * Reduces the size of the set to the specified size. If the set is already smaller than the specified
	 * size, no action is taken. This indiscriminately removes items from the backing array until the
	 * requested newSize is reached, or until the full backing array has had its elements removed.
	 * <br>
	 * This tries to remove from the end of the iteration order, but because the iteration order is not
	 * guaranteed by an unordered set, this can remove essentially any item(s) from the set if it is larger
	 * than newSize.
	 *
	 * @param newSize the target size to try to reach by removing items, if smaller than the current size
	 */
	public void truncate (int newSize) {
		T[] keyTable = this.keyTable;
		for (int i = keyTable.length - 1; i >= 0 && size > newSize; i--) {
			if (keyTable[i] != null) {
				keyTable[i] = null;
				--size;
			}
		}
	}

	/**
	 * Returns an iterator for the keys in the set. Remove is supported.
	 * <p>
	 * Reuses one of two iterators for this set. For nested or multithreaded
	 * iteration, use {@link ObjectSetIterator#ObjectSetIterator(ObjectSet)}.
	 */
	@Override
	public ObjectSetIterator<T> iterator () {
		if (iterator1 == null || iterator2 == null) {
			iterator1 = new ObjectSetIterator<>(this);
			iterator2 = new ObjectSetIterator<>(this);
		}
		if (!iterator1.valid) {
			iterator1.reset();
			iterator1.valid = true;
			iterator2.valid = false;
			return iterator1;
		}
		iterator2.reset();
		iterator2.valid = true;
		iterator1.valid = false;
		return iterator2;
	}

	public static class ObjectSetIterator<T> implements Iterable<T>, Iterator<T> {
		/**
		 * This can be queried in place of calling {@link #hasNext()}. The method also performs
		 * a check that the iterator is valid, where using the field does not check.
		 */
		public boolean hasNext;
		/**
		 * The next index in the set's key table to go to and return from {@link #next()}.
		 */
		protected int nextIndex;
		/**
		 * The current index in the set's key table; this is the index that will be removed if
		 * {@link #remove()} is called.
		 */
		protected int currentIndex;
		/**
		 * Internally employed by the iterator-reuse functionality.
		 */
		protected boolean valid = true;
		/**
		 * The set to iterate over.
		 */
		protected final ObjectSet<T> set;

		public ObjectSetIterator (ObjectSet<T> set) {
			this.set = set;
			reset();
		}

		public void reset () {
			currentIndex = -1;
			nextIndex = -1;
			findNextIndex();
		}

		protected void findNextIndex () {
			T[] keyTable = set.keyTable;
			for (int n = keyTable.length; ++nextIndex < n; ) {
				if (keyTable[nextIndex] != null) {
					hasNext = true;
					return;
				}
			}
			hasNext = false;
		}

		@Override
		public void remove () {
			int i = currentIndex;
			if (i < 0) {throw new IllegalStateException("next must be called before remove.");}
			T[] keyTable = set.keyTable;
			int mask = set.mask, next = i + 1 & mask;
			T key;
			while ((key = keyTable[next]) != null) {
				int placement = set.place(key);
				if ((next - placement & mask) > (i - placement & mask)) {
					keyTable[i] = key;
					i = next;
				}
				next = next + 1 & mask;
			}
			keyTable[i] = null;
			set.size--;
			if (i != currentIndex) {--nextIndex;}
			currentIndex = -1;
		}

		@Override
		public boolean hasNext () {
			if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
			return hasNext;
		}

		@Override
		public T next () {
			if (!hasNext) {throw new NoSuchElementException();}
			if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
			T key = set.keyTable[nextIndex];
			currentIndex = nextIndex;
			findNextIndex();
			return key;
		}

		@Override
		public ObjectSetIterator<T> iterator () {
			return this;
		}

		/**
		 * Returns a new {@link ObjectList} containing the remaining items.
		 * Does not change the position of this iterator.
		 */
		public ObjectList<T> toList () {
			ObjectList<T> list = new ObjectList<>(set.size);
			int currentIdx = currentIndex, nextIdx = nextIndex;
			boolean hn = hasNext;
			while (hasNext) {list.add(next());}
			currentIndex = currentIdx;
			nextIndex = nextIdx;
			hasNext = hn;
			return list;
		}

		/**
		 * Append the remaining items that this can iterate through into the given Collection.
		 * Does not change the position of this iterator.
		 * @param coll any modifiable Collection; may have items appended into it
		 * @return the given collection
		 */
		public Collection<T> appendInto(Collection<T> coll) {
			int currentIdx = currentIndex, nextIdx = nextIndex;
			boolean hn = hasNext;
			while (hasNext) {coll.add(next());}
			currentIndex = currentIdx;
			nextIndex = nextIdx;
			hasNext = hn;
			return coll;
		}
	}

	public static <T> ObjectSet<T> with (T item) {
		ObjectSet<T> set = new ObjectSet<>(1);
		set.add(item);
		return set;
	}

	@SafeVarargs
	public static <T> ObjectSet<T> with (T... array) {
		return new ObjectSet<>(array);
	}
}

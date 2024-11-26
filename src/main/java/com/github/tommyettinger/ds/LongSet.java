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
import com.github.tommyettinger.ds.support.util.IntIterator;
import com.github.tommyettinger.ds.support.util.LongIterator;
import org.checkerframework.checker.nullness.qual.Nullable;
import java.util.Arrays;
import java.util.NoSuchElementException;

import static com.github.tommyettinger.ds.Utilities.tableSize;

/**
 * An unordered set where the items are unboxed longs. No allocation is done except when growing the table size.
 * <p>
 * This class performs fast contains and remove (typically O(1), worst case O(n) but that is rare in practice). Add may be
 * slightly slower, depending on hash collisions. Hashcodes are rehashed to reduce collisions and the need to resize. Load factors
 * greater than 0.91 greatly increase the chances to resize to the next higher POT size.
 * <p>
 * Unordered sets and maps are not designed to provide especially fast iteration. Iteration is faster with {@link Ordered} types like
 * ObjectOrderedSet and ObjectObjectOrderedMap.
 * <p>
 * This implementation uses linear probing with the backward shift algorithm for removal. Linear probing continues to work even
 * when all hashCodes collide; it just works more slowly in that case.
 *
 * @author Nathan Sweet
 * @author Tommy Ettinger
 */
public class LongSet implements PrimitiveSet.SetOfLong {

	protected int size;

	protected long[] keyTable;
	protected boolean hasZeroValue;

	/**
	 * Between 0f (exclusive) and 1f (inclusive, if you're careful), this determines how full the backing table
	 * can get before this increases their size. Larger values use less memory but make the data structure slower.
	 */
	protected float loadFactor;

	/**
	 * Precalculated value of {@code (int)(keyTable.length * loadFactor)}, used to determine when to resize.
	 */
	protected int threshold;

	/**
	 * Used by {@link #place(long)} to bit shift the upper bits of an {@code int} into a usable range (&gt;= 0 and &lt;=
	 * {@link #mask}). The shift can be negative, which is convenient to match the number of bits in mask: if mask is a 7-bit
	 * number, a shift of -7 shifts the upper 7 bits into the lowest 7 positions. This class sets the shift &gt; 32 and &lt; 64,
	 * which when used with an int will still move the upper bits of an int to the lower bits due to Java's implicit modulus on
	 * shifts.
	 * <p>
	 * {@link #mask} can also be used to mask the low bits of a number, which may be faster for some hashcodes, if
	 * {@link #place(long)} is overridden.
	 */
	protected int shift;

	/**
	 * Used by {@link #place(long)} to mix hashCode() results. Changes on every call to {@link #resize(int)} by default.
	 * This only needs to be serialized if the full key table is serialized, or if the iteration order should be
	 * the same before and after serialization. Iteration order is better handled by using {@link LongOrderedSet}.
	 */
	protected int hashMultiplier = 0xEFAA28F1;

	/**
	 * A bitmask used to confine hashcodes to the size of the table. Must be all 1 bits in its low positions, ie a power of two
	 * minus 1. If {@link #place(long)} is overridden, this can be used instead of {@link #shift} to isolate usable bits of a
	 * hash.
	 */
	protected int mask;

	@Nullable protected transient LongSetIterator iterator1;
	@Nullable protected transient LongSetIterator iterator2;

	/**
	 * Creates a new set with an initial capacity of 51 and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 */
	public LongSet () {
		this(51, Utilities.getDefaultLoadFactor());
	}

	/**
	 * Creates a new set with a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 */
	public LongSet (int initialCapacity) {
		this(initialCapacity, Utilities.getDefaultLoadFactor());
	}

	/**
	 * Creates a new set with the specified initial capacity and load factor. This set will hold initialCapacity items before
	 * growing the backing table.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 * @param loadFactor      what fraction of the capacity can be filled before this has to resize; 0 &lt; loadFactor &lt;= 1
	 */
	public LongSet (int initialCapacity, float loadFactor) {
		if (loadFactor <= 0f || loadFactor > 1f) {throw new IllegalArgumentException("loadFactor must be > 0 and <= 1: " + loadFactor);}
		this.loadFactor = loadFactor;

		int tableSize = tableSize(initialCapacity, loadFactor);
		threshold = (int)(tableSize * loadFactor);
		mask = tableSize - 1;
		shift = BitConversion.countLeadingZeros(mask) + 32;

		keyTable = new long[tableSize];
	}

	/**
	 * Creates a new instance containing the items in the specified iterator.
	 *
	 * @param coll an iterator that will have its remaining contents added to this
	 */
	public LongSet (LongIterator coll) {
		this();
		addAll(coll);
	}

	/**
	 * Creates a new set identical to the specified set.
	 */
	public LongSet (LongSet set) {
		this((int)(set.keyTable.length * set.loadFactor), set.loadFactor);
		System.arraycopy(set.keyTable, 0, keyTable, 0, set.keyTable.length);
		size = set.size;
		hasZeroValue = set.hasZeroValue;
		hashMultiplier = set.hashMultiplier;
	}

	/**
	 * Creates a new set using all distinct items in the given PrimitiveCollection, such as a
	 * {@link LongList} or {@link LongObjectMap.Keys}.
	 *
	 * @param coll a PrimitiveCollection that will be used in full, except for duplicate items
	 */
	public LongSet (PrimitiveCollection.OfLong coll) {
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
	public LongSet (long[] array, int offset, int length) {
		this(length);
		addAll(array, offset, length);
	}

	/**
	 * Creates a new set containing all of the items in the given array.
	 *
	 * @param array an array that will be used in full, except for duplicate items
	 */
	public LongSet (long[] array) {
		this(array, 0, array.length);
	}

	/**
	 * Returns an index &gt;= 0 and &lt;= {@link #mask} for the specified {@code item}.
	 * Defaults to using {@link #hashMultiplier}, which changes every time the data structure resizes.
	 *
	 * @param item any long; it is usually mixed or masked here
	 * @return an index between 0 and {@link #mask} (both inclusive)
	 */
	protected int place (long item) {
		return BitConversion.imul((int)(item ^ item >>> 32), hashMultiplier) >>> shift;
	}

	/**
	 * Returns the index of the key if already present, else {@code ~index} for the next empty index.
	 * While this can be overridden to compare for equality differently than {@code ==} between ints, that
	 * isn't recommended because this has to treat zero keys differently, and it finds those with {@code ==}.
	 * If you want to treat equality between longs differently for some reason, you would also need to override
	 * {@link #contains(long)} and {@link #add(long)}, at the very least.
	 */
	protected int locateKey (long key) {
		long[] keyTable = this.keyTable;
		for (int i = place(key); ; i = i + 1 & mask) {
			long other = keyTable[i];
			if (other == 0) {
				return ~i; // Empty space is available.
			}
			if (other == key) {
				return i; // Same key was found.
			}
		}
	}

	/**
	 * Returns true if the key was not already in the set.
	 */
	@Override
	public boolean add (long key) {
		if (key == 0) {
			if (hasZeroValue) {return false;}
			hasZeroValue = true;
			size++;
			return true;
		}
		long[] keyTable = this.keyTable;
		for (int i = place(key); ; i = i + 1 & mask) {
			long other = keyTable[i];
			if (key == other)
				return false; // Existing key was found.
			if (other == 0) {
				keyTable[i] = key;
				if (++size >= threshold) {resize(keyTable.length << 1);}
				return true;
			}
		}
	}

	public boolean addAll (LongList array) {
		return addAll(array.items, 0, array.size);
	}

	public boolean addAll (LongList array, int offset, int length) {
		if (offset + length > array.size) {throw new IllegalArgumentException("offset + length must be <= size: " + offset + " + " + length + " <= " + array.size);}
		return addAll(array.items, offset, length);
	}

	public boolean addAll (long... array) {
		return addAll(array, 0, array.length);
	}

	public boolean addAll (long[] array, int offset, int length) {
		ensureCapacity(length);
		int oldSize = size;
		for (int i = offset, n = i + length; i < n; i++) {add(array[i]);}
		return size != oldSize;
	}

	public boolean addAll (LongSet set) {
		ensureCapacity(set.size);
		int oldSize = size;
		if (set.hasZeroValue) {add(0);}
		long[] keyTable = set.keyTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			long key = keyTable[i];
			if (key != 0) {add(key);}
		}
		return size != oldSize;
	}

	/**
	 * Skips checks for existing keys, doesn't increment size, doesn't need to handle key 0.
	 */
	protected void addResize (long key) {
		long[] keyTable = this.keyTable;
		for (int i = place(key); ; i = i + 1 & mask) {
			if (keyTable[i] == 0) {
				keyTable[i] = key;
				return;
			}
		}
	}

	/**
	 * Returns true if the key was removed.
	 */
	@Override
	public boolean remove (long key) {
		if (key == 0) {
			if (!hasZeroValue)
				return false;
			hasZeroValue = false;
			size--;
			return true;
		}

		int pos = locateKey(key);
		if (pos < 0)
			return false;
		long[] keyTable = this.keyTable;
		int mask = this.mask, last, slot;
		size--;
		for (;;) {
			pos = ((last = pos) + 1) & mask;
			for (;;) {
				if ((key = keyTable[pos]) == 0) {
					keyTable[last] = 0;
					return true;
				}
				slot = place(key);
				if (last <= pos ? last >= slot || slot > pos : last >= slot && slot > pos) break;
				pos = (pos + 1) & mask;
			}
			keyTable[last] = key;
		}
	}

	/**
	 * Returns true if the set has one or more items.
	 */
	public boolean notEmpty () {
		return size != 0;
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
	 */
	public void clear (int maximumCapacity) {
		int tableSize = tableSize(maximumCapacity, loadFactor);
		if (keyTable.length <= tableSize) {
			clear();
			return;
		}
		size = 0;
		hasZeroValue = false;
		resize(tableSize);
	}

	@Override
	public void clear () {
		if (size == 0) {return;}
		size = 0;
		Arrays.fill(keyTable, 0);
		hasZeroValue = false;
	}

	@Override
	public boolean contains (long key) {
		if (key == 0) {return hasZeroValue;}
		long[] keyTable = this.keyTable;
		for (int i = place(key); ; i = i + 1 & mask) {
			long other = keyTable[i];
			if (key == other)
				return true;
			if (other == 0)
				return false;
		}
	}

	public long first () {
		if (hasZeroValue) {return 0;}
		long[] keyTable = this.keyTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {if (keyTable[i] != 0) {return keyTable[i];}}
		throw new IllegalStateException("IntSet is empty.");
	}

	/**
	 * Increases the size of the backing array to accommodate the specified number of additional items / loadFactor. Useful before
	 * adding many items to avoid multiple backing array resizes.
	 */
	public void ensureCapacity (int additionalCapacity) {
		int tableSize = tableSize(size + additionalCapacity, loadFactor);
		if (keyTable.length < tableSize) {resize(tableSize);}
	}

	@Override
	public int size () {
		return size;
	}

	protected void resize (int newSize) {
		int oldCapacity = keyTable.length;
		threshold = (int)(newSize * loadFactor);
		mask = newSize - 1;
		shift = BitConversion.countLeadingZeros(mask) + 32;

		hashMultiplier = Utilities.GOOD_MULTIPLIERS[BitConversion.imul(hashMultiplier, shift) >>> 5 & 511];
		long[] oldKeyTable = keyTable;

		keyTable = new long[newSize];

		if (size > 0) {
			for (int i = 0; i < oldCapacity; i++) {
				long key = oldKeyTable[i];
				if (key != 0) {addResize(key);}
			}
		}
	}

	/**
	 * Gets the current hash multiplier as used by {@link #place(long)}; for specific advanced usage only.
	 * The hash multiplier changes whenever {@link #resize(int)} is called, though its value before the resize
	 * affects its value after.
	 * @return the current hash multiplier, which should always be a negative, odd int
	 */
	public int getHashMultiplier () {
		return hashMultiplier;
	}

	/**
	 * Sets the current hash multiplier, then immediately calls {@link #resize(int)} without changing the target size;
	 * this is for specific advanced usage only. Calling resize() will change the multiplier before it gets used, and
	 * the current {@link #size()} of the data structure also changes the value. The hash multiplier is used by
	 * {@link #place(long)}. The hash multiplier must be a negative, odd int, and this method will ensure that both the
	 * used multiplier is both negative and odd. The hash multiplier changes whenever {@link #resize(int)} is called,
	 * though its value before the resize affects its value after. Because of how resize() randomizes the multiplier,
	 * even inputs such as {@code 1}, {@code -999999999} and {@code 0} actually work well.
	 * <br>
	 * This is accessible at all mainly so serialization code that has a need to access the hash multiplier can do so, but
	 * also to provide an "emergency escape route" in case of hash flooding. Using one of the "known good" ints in
	 * {@link Utilities#GOOD_MULTIPLIERS} should usually be fine if you don't know what multiplier will work well.
	 * Be advised that because this has to call resize(), it isn't especially fast, and it slows down the more items are
	 * in the data structure. If you in a situation where you are worried about hash flooding, you also shouldn't permit
	 * adversaries to cause this method to be called frequently. Also be advised that because of how resize() works, the
	 * result of {@link #getHashMultiplier()} after calling this will only very rarely be the same as the parameter here.
	 * @param hashMultiplier any int; will not be used as-is
	 */
	public void setHashMultiplier (int hashMultiplier) {
		this.hashMultiplier = hashMultiplier | 0x80000001;
		resize(keyTable.length);
	}

	/**
	 * Gets the length of the internal array used to store all items, as well as empty space awaiting more items to be
	 * entered. This is also called the capacity.
	 * @return the length of the internal array that holds all items
	 */
	public int getTableSize() {
		return keyTable.length;
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
		long h = size;
		long[] keyTable = this.keyTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			long key = keyTable[i];
			if (key != 0) {h += key;}
		}
		return (int)(h ^ h >>> 32);
	}

	@Override
	public boolean equals (Object o) {
		return SetOfLong.super.equalContents(o);
	}

	public StringBuilder appendTo (StringBuilder builder) {
		if (size == 0) {return builder.append("[]");}
		builder.append('[');
		long[] keyTable = this.keyTable;
		int i = keyTable.length;
		if (hasZeroValue) {builder.append('0');} else {
			while (i-- > 0) {
				long key = keyTable[i];
				if (key == 0) {continue;}
				builder.append(key);
				break;
			}
		}
		while (i-- > 0) {
			long key = keyTable[i];
			if (key == 0) {continue;}
			builder.append(", ");
			builder.append(key);
		}
		builder.append(']');
		return builder;
	}

	@Override
	public String toString () {
		return toString(", ", true);
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
		long[] keyTable = this.keyTable;
		newSize = Math.max(0, newSize);
		for (int i = keyTable.length - 1; i >= 0 && size > newSize; i--) {
			if (keyTable[i] != 0) {
				keyTable[i] = 0;
				--size;
			}
		}
		if (hasZeroValue && size > newSize) {
			hasZeroValue = false;
			--size;
		}
	}

	/**
	 * Returns an iterator for the keys in the set. Remove is supported.
	 * <p>
	 * Use the {@link LongSetIterator} constructor for nested or multithreaded iteration.
	 */
	@Override
	public LongSetIterator iterator () {
		if (iterator1 == null || iterator2 == null) {
			iterator1 = new LongSetIterator(this);
			iterator2 = new LongSetIterator(this);
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

	public static class LongSetIterator implements LongIterator {
		static private final int INDEX_ILLEGAL = -2, INDEX_ZERO = -1;

		/**
		 * This can be queried in place of calling {@link #hasNext()}. The method also performs
		 * a check that the iterator is valid, where using the field does not check.
		 */
		public boolean hasNext;
		/**
		 * The next index in the set's key table to go to and return from {@link #nextLong()} (or,
		 * while discouraged because of boxing, {@link #next()}).
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
		protected final LongSet set;

		public LongSetIterator (LongSet set) {
			this.set = set;
			reset();
		}

		public void reset () {
			currentIndex = INDEX_ILLEGAL;
			nextIndex = INDEX_ZERO;
			if (set.hasZeroValue) {hasNext = true;} else {findNextIndex();}
		}

		protected void findNextIndex () {
			long[] keyTable = set.keyTable;
			for (int n = keyTable.length; ++nextIndex < n; ) {
				if (keyTable[nextIndex] != 0) {
					hasNext = true;
					return;
				}
			}
			hasNext = false;
		}

		/**
		 * Returns {@code true} if the iteration has more elements.
		 * (In other words, returns {@code true} if {@link #next} would
		 * return an element rather than throwing an exception.)
		 *
		 * @return {@code true} if the iteration has more elements
		 */
		@Override
		public boolean hasNext () {
			if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
			return hasNext;
		}

		@Override
		public void remove () {
			int i = currentIndex;
			if (i == INDEX_ZERO && set.hasZeroValue) {
				set.hasZeroValue = false;
			} else if (i < 0) {
				throw new IllegalStateException("next must be called before remove.");
			} else {
				long[] keyTable = set.keyTable;
				int mask = set.mask;
				int next = i + 1 & mask;
				long key;
				while ((key = keyTable[next]) != 0) {
					int placement = set.place(key);
					if ((next - placement & mask) > (i - placement & mask)) {
						keyTable[i] = key;
						i = next;
					}
					next = next + 1 & mask;
				}
				keyTable[i] = 0;
				if (i != currentIndex) {--nextIndex;}
			}
			currentIndex = INDEX_ILLEGAL;
			set.size--;
		}

		@Override
		public long nextLong () {
			if (!hasNext) {throw new NoSuchElementException();}
			if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
			long key = nextIndex == INDEX_ZERO ? 0 : set.keyTable[nextIndex];
			currentIndex = nextIndex;
			findNextIndex();
			return key;
		}

		/**
		 * Returns a new {@link LongList} containing the remaining items.
		 * Does not change the position of this iterator.
		 */
		public LongList toList () {
			LongList list = new LongList(set.size);
			int currentIdx = currentIndex, nextIdx = nextIndex;
			boolean hn = hasNext;
			while (hasNext) {list.add(nextLong());}
			currentIndex = currentIdx;
			nextIndex = nextIdx;
			hasNext = hn;
			return list;
		}
		/**
		 * Append the remaining items that this can iterate through into the given PrimitiveCollection.OfLong.
		 * Does not change the position of this iterator.
		 * @param coll any modifiable PrimitiveCollection.OfLong; may have items appended into it
		 * @return the given primitive collection
		 */
		public PrimitiveCollection.OfLong appendInto(PrimitiveCollection.OfLong coll) {
			int currentIdx = currentIndex, nextIdx = nextIndex;
			boolean hn = hasNext;
			while (hasNext) {coll.add(nextLong());}
			currentIndex = currentIdx;
			nextIndex = nextIdx;
			hasNext = hn;
			return coll;
		}
	}

	/**
	 * Creates a new LongSet that holds only the given item, but can be resized.
	 * @param item a long item
	 * @return a new LongSet that holds the given item
	 */
	public static LongSet with (long item) {
		LongSet set = new LongSet(1);
		set.add(item);
		return set;
	}

	/**
	 * Creates a new LongSet that holds only the given items, but can be resized.
	 * @param item0 a long item
	 * @param item1 a long item
	 * @return a new LongSet that holds the given items
	 */
	public static LongSet with (long item0, long item1) {
		LongSet set = new LongSet(2);
		set.add(item0);
		set.add(item1);
		return set;
	}

	/**
	 * Creates a new LongSet that holds only the given items, but can be resized.
	 * @param item0 a long item
	 * @param item1 a long item
	 * @param item2 a long item
	 * @return a new LongSet that holds the given items
	 */
	public static LongSet with (long item0, long item1, long item2) {
		LongSet set = new LongSet(3);
		set.add(item0);
		set.add(item1);
		set.add(item2);
		return set;
	}

	/**
	 * Creates a new LongSet that holds only the given items, but can be resized.
	 * @param item0 a long item
	 * @param item1 a long item
	 * @param item2 a long item
	 * @param item3 a long item
	 * @return a new LongSet that holds the given items
	 */
	public static LongSet with (long item0, long item1, long item2, long item3) {
		LongSet set = new LongSet(4);
		set.add(item0);
		set.add(item1);
		set.add(item2);
		set.add(item3);
		return set;
	}

	/**
	 * Creates a new LongSet that holds only the given items, but can be resized.
	 * @param item0 a long item
	 * @param item1 a long item
	 * @param item2 a long item
	 * @param item3 a long item
	 * @param item4 a long item
	 * @return a new LongSet that holds the given items
	 */
	public static LongSet with (long item0, long item1, long item2, long item3, long item4) {
		LongSet set = new LongSet(5);
		set.add(item0);
		set.add(item1);
		set.add(item2);
		set.add(item3);
		set.add(item4);
		return set;
	}

	/**
	 * Creates a new LongSet that holds only the given items, but can be resized.
	 * @param item0 a long item
	 * @param item1 a long item
	 * @param item2 a long item
	 * @param item3 a long item
	 * @param item4 a long item
	 * @param item5 a long item
	 * @return a new LongSet that holds the given items
	 */
	public static LongSet with (long item0, long item1, long item2, long item3, long item4, long item5) {
		LongSet set = new LongSet(6);
		set.add(item0);
		set.add(item1);
		set.add(item2);
		set.add(item3);
		set.add(item4);
		set.add(item5);
		return set;
	}

	/**
	 * Creates a new LongSet that holds only the given items, but can be resized.
	 * @param item0 a long item
	 * @param item1 a long item
	 * @param item2 a long item
	 * @param item3 a long item
	 * @param item4 a long item
	 * @param item5 a long item
	 * @param item6 a long item
	 * @return a new LongSet that holds the given items
	 */
	public static LongSet with (long item0, long item1, long item2, long item3, long item4, long item5, long item6) {
		LongSet set = new LongSet(7);
		set.add(item0);
		set.add(item1);
		set.add(item2);
		set.add(item3);
		set.add(item4);
		set.add(item5);
		set.add(item6);
		return set;
	}

	/**
	 * Creates a new LongSet that holds only the given items, but can be resized.
	 * @param item0 a long item
	 * @param item1 a long item
	 * @param item2 a long item
	 * @param item3 a long item
	 * @param item4 a long item
	 * @param item5 a long item
	 * @param item6 a long item
	 * @return a new LongSet that holds the given items
	 */
	public static LongSet with (long item0, long item1, long item2, long item3, long item4, long item5, long item6, long item7) {
		LongSet set = new LongSet(8);
		set.add(item0);
		set.add(item1);
		set.add(item2);
		set.add(item3);
		set.add(item4);
		set.add(item5);
		set.add(item6);
		set.add(item7);
		return set;
	}

	/**
	 * Creates a new LongSet that holds only the given items, but can be resized.
	 * This overload will only be used when an array is supplied and the type of the
	 * items requested is the component type of the array, or if varargs are used and
	 * there are 9 or more arguments.
	 * @param varargs a long varargs or long array; remember that varargs allocate
	 * @return a new LongSet that holds the given items
	 */
	public static LongSet with (long... varargs) {
		return new LongSet(varargs);
	}
}

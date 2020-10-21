/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
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
 ******************************************************************************/

package com.github.tommyettinger.ds;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;

import static com.github.tommyettinger.ds.Utilities.tableSize;

/** An unordered set where the items are unboxed longs. No allocation is done except when growing the table size.
 * <p>
 * This class performs fast contains and remove (typically O(1), worst case O(n) but that is rare in practice). Add may be
 * slightly slower, depending on hash collisions. Hashcodes are rehashed to reduce collisions and the need to resize. Load factors
 * greater than 0.91 greatly increase the chances to resize to the next higher POT size.
 * <p>
 * Unordered sets and maps are not designed to provide especially fast iteration. Iteration is faster with OrderedSet and
 * OrderedMap.
 * <p>
 * This implementation uses linear probing with the backward shift algorithm for removal. Linear probing continues to work even
 * when all hashCodes collide, just more slowly.
 * @author Nathan Sweet
 * @author Tommy Ettinger
 */
public class LongSet implements Serializable {
	private static final long serialVersionUID = 0L;

	public int size;

	protected long[] keyTable;
	protected boolean hasZeroValue;

	protected final float loadFactor;
	protected int threshold;

	/** Used by {@link #place(long)} to bit shift the upper bits of a {@code long} into a usable range (&gt;= 0 and &lt;=
	 * {@link #mask}). The shift can be negative, which is convenient to match the number of bits in mask: if mask is a 7-bit
	 * number, a shift of -7 shifts the upper 7 bits into the lowest 7 positions. This class sets the shift &gt; 32 and &lt; 64,
	 * which if used with an int will still move the upper bits of an int to the lower bits due to Java's implicit modulus on
	 * shifts.
	 * <p>
	 * {@link #mask} can also be used to mask the low bits of a number, which may be faster for some hashcodes, if
	 * {@link #place(long)} is overridden. */
	protected int shift;

	/** A bitmask used to confine hashcodes to the size of the table. Must be all 1 bits in its low positions, ie a power of two
	 * minus 1. If {@link #place(long)} is overridden, this can be used instead of {@link #shift} to isolate usable bits of a
	 * hash. */
	protected int mask;

	protected @Nullable LongSetIterator iterator1, iterator2;

	/** Creates a new set with an initial capacity of 51 and a load factor of 0.8. */
	public LongSet () {
		this(51, 0.8f);
	}

	/** Creates a new set with a load factor of 0.8.
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two. */
	public LongSet (int initialCapacity) {
		this(initialCapacity, 0.8f);
	}

	/** Creates a new set with the specified initial capacity and load factor. This set will hold initialCapacity items before
	 * growing the backing table.
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two. */
	public LongSet (int initialCapacity, float loadFactor) {
		if (loadFactor <= 0f || loadFactor >= 1f)
			throw new IllegalArgumentException("loadFactor must be > 0 and < 1: " + loadFactor);
		this.loadFactor = loadFactor;

		int tableSize = tableSize(initialCapacity, loadFactor);
		threshold = (int)(tableSize * loadFactor);
		mask = tableSize - 1;
		shift = Long.numberOfLeadingZeros(mask);

		keyTable = new long[tableSize];
	}

	/** Creates a new set identical to the specified set. */
	public LongSet (LongSet set) {
		this((int)(set.keyTable.length * set.loadFactor), set.loadFactor);
		System.arraycopy(set.keyTable, 0, keyTable, 0, set.keyTable.length);
		size = set.size;
		hasZeroValue = set.hasZeroValue;
	}

	/**
	 * Returns an index &gt;= 0 and &lt;= {@link #mask} for the specified {@code item}.
	 *
	 * @param item any long; it is usually mixed so similar inputs still have different outputs
	 */
	protected int place (long item) {
		return (int)((item ^ item >>> 32) * 0x9E3779B97F4A7C15L >>> shift);
	}

	/** Returns the index of the key if already present, else {@code -1 - index} for the next empty index. This can be overridden in this
	 * pacakge to compare for equality differently than {@link Object#equals(Object)}. */
	private int locateKey (long key) {
		long[] keyTable = this.keyTable;
		for (int i = place(key);; i = i + 1 & mask) {
			long other = keyTable[i];
			if (other == 0) return ~i; // Empty space is available.
			if (other == key) return i; // Same key was found.
		}
	}

	/** Returns true if the key was not already in the set. */
	public boolean add (long key) {
		if (key == 0) {
			if (hasZeroValue) return false;
			hasZeroValue = true;
			size++;
			return true;
		}
		int i = locateKey(key);
		if (i >= 0) return false; // Existing key was found.
		i = ~i; // Empty space was found.
		keyTable[i] = key;
		if (++size >= threshold) resize(keyTable.length << 1);
		return true;
	}

	public void addAll (LongList array) {
		addAll(array.items, 0, array.size);
	}

	public void addAll (LongList array, int offset, int length) {
		if (offset + length > array.size)
			throw new IllegalArgumentException("offset + length must be <= size: " + offset + " + " + length + " <= " + array.size);
		addAll(array.items, offset, length);
	}

	public void addAll (long... array) {
		addAll(array, 0, array.length);
	}

	public void addAll (long[] array, int offset, int length) {
		ensureCapacity(length);
		for (int i = offset, n = i + length; i < n; i++)
			add(array[i]);
	}

	public void addAll (LongSet set) {
		ensureCapacity(set.size);
		if (set.hasZeroValue) add(0);
		long[] keyTable = set.keyTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			long key = keyTable[i];
			if (key != 0) add(key);
		}
	}

	/** Skips checks for existing keys, doesn't increment size, doesn't need to handle key 0. */
	private void addResize (long key) {
		long[] keyTable = this.keyTable;
		for (int i = place(key);; i = (i + 1) & mask) {
			if (keyTable[i] == 0) {
				keyTable[i] = key;
				return;
			}
		}
	}

	/** Returns true if the key was removed. */
	public boolean remove (long key) {
		if (key == 0) {
			if (!hasZeroValue) return false;
			hasZeroValue = false;
			size--;
			return true;
		}

		int i = locateKey(key);
		if (i < 0) return false;
		long[] keyTable = this.keyTable;
		int mask = this.mask;
		int next = i + 1 & mask;
		while ((key = keyTable[next]) != 0) {
			long placement = place(key);
			if ((next - placement & mask) > (i - placement & mask)) {
				keyTable[i] = key;
				i = next;
			}
			next = next + 1 & mask;
		}
		keyTable[i] = 0;
		size--;
		return true;
	}

	/** Returns true if the set has one or more items. */
	public boolean notEmpty () {
		return size > 0;
	}

	/** Returns true if the set is empty. */
	public boolean isEmpty () {
		return size == 0;
	}

	/** Reduces the size of the backing arrays to be the specified capacity / loadFactor, or less. If the capacity is already less,
	 * nothing is done. If the set contains more items than the specified capacity, the next highest power of two capacity is used
	 * instead. */
	public void shrink (int maximumCapacity) {
		if (maximumCapacity < 0) throw new IllegalArgumentException("maximumCapacity must be >= 0: " + maximumCapacity);
		int tableSize = tableSize(maximumCapacity, loadFactor);
		if (keyTable.length > tableSize) resize(tableSize);
	}

	/** Clears the set and reduces the size of the backing arrays to be the specified capacity / loadFactor, if they are larger. */
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

	public void clear () {
		if (size == 0) return;
		size = 0;
		Arrays.fill(keyTable, 0);
		hasZeroValue = false;
	}

	public boolean contains (long key) {
		if (key == 0) return hasZeroValue;
		return locateKey(key) >= 0;
	}

	public long first () {
		if (hasZeroValue) return 0;
		long[] keyTable = this.keyTable;
		for (int i = 0, n = keyTable.length; i < n; i++)
			if (keyTable[i] != 0) return keyTable[i];
		throw new IllegalStateException("IntSet is empty.");
	}

	/** Increases the size of the backing array to accommodate the specified number of additional items / loadFactor. Useful before
	 * adding many items to avoid multiple backing array resizes. */
	public void ensureCapacity (int additionalCapacity) {
		int tableSize = tableSize(size + additionalCapacity, loadFactor);
		if (keyTable.length < tableSize) resize(tableSize);
	}

	private void resize (int newSize) {
		int oldCapacity = keyTable.length;
		threshold = (int)(newSize * loadFactor);
		mask = newSize - 1;
		shift = Long.numberOfLeadingZeros(mask);

		long[] oldKeyTable = keyTable;

		keyTable = new long[newSize];

		if (size > 0) {
			for (int i = 0; i < oldCapacity; i++) {
				long key = oldKeyTable[i];
				if (key != 0) addResize(key);
			}
		}
	}

	public int hashCode () {
		long h = size;
		long[] keyTable = this.keyTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			long key = keyTable[i];
			if (key != 0) h += key;
		}
		return (int)(h ^ h >>> 32);
	}

	public boolean equals (Object obj) {
		if (!(obj instanceof LongSet)) return false;
		LongSet other = (LongSet)obj;
		if (other.size != size) return false;
		if (other.hasZeroValue != hasZeroValue) return false;
		long[] keyTable = this.keyTable;
		for (int i = 0, n = keyTable.length; i < n; i++)
			if (keyTable[i] != 0 && !other.contains(keyTable[i])) return false;
		return true;
	}

	public String toString () {
		if (size == 0) return "[]";
		StringBuilder buffer = new StringBuilder(32);
		buffer.append('[');
		long[] keyTable = this.keyTable;
		int i = keyTable.length;
		if (hasZeroValue)
			buffer.append("0");
		else {
			while (i-- > 0) {
				long key = keyTable[i];
				if (key == 0) continue;
				buffer.append(key);
				break;
			}
		}
		while (i-- > 0) {
			long key = keyTable[i];
			if (key == 0) continue;
			buffer.append(", ");
			buffer.append(key);
		}
		buffer.append(']');
		return buffer.toString();
	}

	/** Returns an iterator for the keys in the set. Remove is supported.
	 * <p>
	 * Use the {@link LongSetIterator} constructor for nested or multithreaded iteration. */
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

	static public LongSet with (long... array) {
		LongSet set = new LongSet();
		set.addAll(array);
		return set;
	}

	static public class LongSetIterator implements PrimitiveIterator.OfLong {
		static private final int INDEX_ILLEGAL = -2, INDEX_ZERO = -1;

		public boolean hasNext;

		final LongSet set;
		int nextIndex, currentIndex;
		boolean valid = true;

		public LongSetIterator (LongSet set) {
			this.set = set;
			reset();
		}

		public void reset () {
			currentIndex = INDEX_ILLEGAL;
			nextIndex = INDEX_ZERO;
			if (set.hasZeroValue)
				hasNext = true;
			else
				findNextIndex();
		}

		void findNextIndex () {
			long[] keyTable = set.keyTable;
			for (int n = keyTable.length; ++nextIndex < n;) {
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
			return hasNext;
		}

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
					long placement = set.place(key);
					if ((next - placement & mask) > (i - placement & mask)) {
						keyTable[i] = key;
						i = next;
					}
					next = next + 1 & mask;
				}
				keyTable[i] = 0;
				if (i != currentIndex) --nextIndex;
			}
			currentIndex = INDEX_ILLEGAL;
			set.size--;
		}

		@Override
		public long nextLong () {
			if (!hasNext) throw new NoSuchElementException();
			if (!valid) throw new RuntimeException("#iterator() cannot be used nested.");
			long key = nextIndex == INDEX_ZERO ? 0 : set.keyTable[nextIndex];
			currentIndex = nextIndex;
			findNextIndex();
			return key;
		}

		/** Returns a new LongList containing the remaining keys. */
		public LongList toList () {
			LongList list = new LongList(true, set.size);
			while (hasNext)
				list.add(next());
			return list;
		}
	}
}

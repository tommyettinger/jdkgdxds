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
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Arrays;
import java.util.NoSuchElementException;

import static com.github.tommyettinger.ds.Utilities.tableSize;

/**
 * An unordered set where the items are unboxed ints. No allocation is done except when growing the table size.
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
public class IntSet implements PrimitiveSet.SetOfInt {

	protected int size;

	protected int[] keyTable;
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
	 * Used by {@link #place(int)} to bit shift the upper bits of an {@code int} into a usable range (&gt;= 0 and &lt;=
	 * {@link #mask}). The shift can be negative, which is convenient to match the number of bits in mask: if mask is a 7-bit
	 * number, a shift of -7 shifts the upper 7 bits into the lowest 7 positions. This class sets the shift &gt; 32 and &lt; 64,
	 * which when used with an int will still move the upper bits of an int to the lower bits due to Java's implicit modulus on
	 * shifts.
	 * <p>
	 * {@link #mask} can also be used to mask the low bits of a number, which may be faster for some hashcodes, if
	 * {@link #place(int)} is overridden.
	 */
	protected int shift;

	/**
	 * A bitmask used to confine hash codes to the size of the table. Must be all 1-bits in its low positions, ie a power of two
	 * minus 1. If {@link #place(int)} is overridden, this can be used instead of {@link #shift} to isolate usable bits of a
	 * hash.
	 */
	protected int mask;

	@Nullable protected transient IntSetIterator iterator1;
	@Nullable protected transient IntSetIterator iterator2;

	/**
	 * Creates a new set with an initial capacity of 51 and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 */
	public IntSet() {
		this(51, Utilities.getDefaultLoadFactor());
	}

	/**
	 * Creates a new set with a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 */
	public IntSet(int initialCapacity) {
		this(initialCapacity, Utilities.getDefaultLoadFactor());
	}

	/**
	 * Creates a new set with the specified initial capacity and load factor. This set will hold initialCapacity items before
	 * growing the backing table.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 * @param loadFactor      what fraction of the capacity can be filled before this has to resize; 0 &lt; loadFactor &lt;= 1
	 */
	public IntSet(int initialCapacity, float loadFactor) {
		if (loadFactor <= 0f || loadFactor > 1f) {throw new IllegalArgumentException("loadFactor must be > 0 and <= 1: " + loadFactor);}
		this.loadFactor = loadFactor;

		int tableSize = tableSize(initialCapacity, loadFactor);
		mask = tableSize - 1;
		threshold = Math.min((int)(tableSize * (double)loadFactor + 1), mask);
		shift = BitConversion.countLeadingZeros(mask) + 32;

		keyTable = new int[tableSize];
	}

	/**
	 * Creates a new instance containing the items in the specified iterator.
	 *
	 * @param coll an iterator that will have its remaining contents added to this
	 */
	public IntSet(IntIterator coll) {
		this();
		addAll(coll);
	}

	/**
	 * Creates a new set identical to the specified set.
	 */
	public IntSet(IntSet set) {
		this((int)(set.keyTable.length * set.loadFactor), set.loadFactor);
		System.arraycopy(set.keyTable, 0, keyTable, 0, set.keyTable.length);
		size = set.size;
		hasZeroValue = set.hasZeroValue;
	}

	/**
	 * Creates a new set using all distinct items in the given PrimitiveCollection, such as a
	 * {@link IntList} or {@link IntObjectMap.Keys}.
	 *
	 * @param coll a PrimitiveCollection that will be used in full, except for duplicate items
	 */
	public IntSet(OfInt coll) {
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
	public IntSet(int[] array, int offset, int length) {
		this(length);
		addAll(array, offset, length);
	}

	/**
	 * Creates a new set containing all the items in the given array.
	 *
	 * @param array an array that will be used in full, except for duplicate items
	 */
	public IntSet(int[] array) {
		this(array, 0, array.length);
	}

	/**
	 * Returns an index &gt;= 0 and &lt;= {@link #mask} for the specified {@code item}.
	 *
	 * @param item any int; it is usually mixed and shifted or masked here
	 * @return an index between 0 and {@link #mask} (both inclusive)
	 */
	protected int place (int item) {
		return (item ^ (item << 9 | item >>> 23) ^ (item << 21 | item >>> 11)) & mask;
	}

	/**
	 * Returns true if the key was not already in the set.
	 */
	@Override
	public boolean add (int key) {
		if (key == 0) {
			if (hasZeroValue) return false;
			hasZeroValue = true;
			size++;
			return true;
		}
		int[] keyTable = this.keyTable;

		for (int i = place(key); ; i = i + 1 & mask) {
			int other = keyTable[i];
			if (key == other)
				return false; // Existing key was found.
			if (other == 0) {
				keyTable[i] = key;
				if (++size >= threshold) {resize(keyTable.length << 1);}
				return true;
			}
		}
	}

	public boolean addAll (IntList array) {
		return addAll(array.items, 0, array.size());
	}

	public boolean addAll (IntList array, int offset, int length) {
		if (offset + length > array.size()) {throw new IllegalArgumentException("offset + length must be <= size: " + offset + " + " + length + " <= " + array.size());}
		return addAll(array.items, offset, length);
	}

	public boolean addAll (int... array) {
		return addAll(array, 0, array.length);
	}

	public boolean addAll (int[] array, int offset, int length) {
		ensureCapacity(length);
		int oldSize = size;
		for (int i = offset, n = i + length; i < n; i++) {add(array[i]);}
		return size != oldSize;
	}

	public boolean addAll (IntSet set) {
		ensureCapacity(set.size);
		int oldSize = size;
		if (set.hasZeroValue) {add(0);}
		int[] keyTable = set.keyTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			int key = keyTable[i];
			if (key != 0) {add(key);}
		}
		return size != oldSize;
	}

	/**
	 * Skips checks for existing keys, doesn't increment size, doesn't need to handle key 0.
	 */
	protected void addResize (int key) {
		int[] keyTable = this.keyTable;
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
	public boolean remove (int key) {
		if (key == 0) {
			if (hasZeroValue) {
				hasZeroValue = false;
				size--;
				return true;
			}
			return false;
		}

		int pos;
		int mask = this.mask;
		int[] keyTable = this.keyTable;
		for (int i = place(key); ; i = i + 1 & mask) {
			int other = keyTable[i];
			if (other == 0) {
				return false; // Nothing is present.
			}
			if (other == key) {
				pos = i; // Same key was found.
				break;
			}
		}
		int last, slot;
		size--;
		for (;;) {
			pos = ((last = pos) + 1) & mask;
			for (;;) {
				if ((key = keyTable[pos]) == 0) {
					keyTable[last] = 0;
//					if(mask >= minCapacity && size < (threshold >>> 2))
//						resize(keyTable.length >>> 1);
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
		int tableSize = tableSize(maximumCapacity, loadFactor);
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
	public boolean contains (int key) {
		if (key == 0) {return hasZeroValue;}
		int[] keyTable = this.keyTable;
		for (int i = place(key); ; i = i + 1 & mask) {
			int other = keyTable[i];
			if (key == other)
				return true;
			if (other == 0)
				return false;
		}
	}

	public int first () {
		if (hasZeroValue) {return 0;}
		int[] keyTable = this.keyTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {if (keyTable[i] != 0) {return keyTable[i];}}
		throw new IllegalStateException("IntSetAlt is empty.");
	}

	/**
	 * Increases the size of the backing array to accommodate the specified number of additional items / loadFactor. Useful before
	 * adding many items to avoid multiple backing array resizes.
	 */
	public void ensureCapacity (int additionalCapacity) {
		int tableSize = tableSize(size + additionalCapacity, loadFactor);
		if (keyTable.length < tableSize) {resize(tableSize);}
	}

	protected void resize (int newSize) {
		int oldCapacity = keyTable.length;
		mask = newSize - 1;
		threshold = Math.min((int)(newSize * (double)loadFactor + 1), mask);
		shift = BitConversion.countLeadingZeros(mask) + 32;

		int[] oldKeyTable = keyTable;

		keyTable = new int[newSize];

		if (size > 0) {
			for (int i = 0; i < oldCapacity; i++) {
				int key = oldKeyTable[i];
				if (key != 0) {addResize(key);}
			}
		}
	}

	/**
	 * Effectively does nothing here because the hashMultiplier is no longer stored or used.
	 * Subclasses can use this as some kind of identifier or user data, though.
	 *
	 * @return any int; the value isn't used internally, but may be used by subclasses to identify something
	 */
	public int getHashMultiplier() {
		return 0;
	}

	/**
	 * Effectively does nothing here because the hashMultiplier is no longer stored or used.
	 * Subclasses can use this to set some kind of identifier or user data, though.
	 *
	 * @param unused any int; will not be used as-is
	 */
	public void setHashMultiplier(int unused) {
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
		int[] keyTable = this.keyTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			int key = keyTable[i];
			if (key != 0) {h += key;}
		}
		return h;
	}

	@SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
	@Override
	public boolean equals (Object o) {
		return SetOfInt.super.equalContents(o);
	}

	public StringBuilder appendTo (StringBuilder builder) {
		if (size == 0) {return builder.append("[]");}
		builder.append('[');
		int[] keyTable = this.keyTable;
		int i = keyTable.length;
		if (hasZeroValue) {builder.append('0');} else {
			while (i-- > 0) {
				int key = keyTable[i];
				if (key == 0) {continue;}
				builder.append(key);
				break;
			}
		}
		while (i-- > 0) {
			int key = keyTable[i];
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
		int[] keyTable = this.keyTable;
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
	 * Use the {@link IntSetIterator} constructor for nested or multithreaded iteration.
	 */
	@Override
	public IntSetIterator iterator () {
		if (iterator1 == null || iterator2 == null) {
			iterator1 = new IntSetIterator(this);
			iterator2 = new IntSetIterator(this);
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

	@Override
	public int size () {
		return size;
	}

	public static class IntSetIterator implements IntIterator {
		static private final int INDEX_ILLEGAL = -2, INDEX_ZERO = -1;

		/**
		 * This can be queried in place of calling {@link #hasNext()}. The method also performs
		 * a check that the iterator is valid, where using the field does not check.
		 */
		public boolean hasNext;
		/**
		 * The next index in the set's key table to go to and return from {@link #nextInt()} (or,
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
		protected final IntSet set;

		public IntSetIterator (IntSet set) {
			this.set = set;
			reset();
		}

		public void reset () {
			currentIndex = INDEX_ILLEGAL;
			nextIndex = INDEX_ZERO;
			if (set.hasZeroValue) {hasNext = true;} else {findNextIndex();}
		}

		protected void findNextIndex () {
			int[] keyTable = set.keyTable;
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
				int[] keyTable = set.keyTable;
				int mask = set.mask, next = i + 1 & mask, key;
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
		public int nextInt () {
			if (!hasNext) {throw new NoSuchElementException();}
			if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
			int key = nextIndex == INDEX_ZERO ? 0 : set.keyTable[nextIndex];
			currentIndex = nextIndex;
			findNextIndex();
			return key;
		}

		/**
		 * Returns a new {@link IntList} containing the remaining items.
		 * Does not change the position of this iterator.
		 */
		public IntList toList () {
			IntList list = new IntList(set.size);
			int currentIdx = currentIndex, nextIdx = nextIndex;
			boolean hn = hasNext;
			while (hasNext) {list.add(nextInt());}
			currentIndex = currentIdx;
			nextIndex = nextIdx;
			hasNext = hn;
			return list;
		}
		/**
		 * Append the remaining items that this can iterate through into the given PrimitiveCollection.OfInt.
		 * Does not change the position of this iterator.
		 * @param coll any modifiable PrimitiveCollection.OfInt; may have items appended into it
		 * @return the given primitive collection
		 */
		public OfInt appendInto(OfInt coll) {
			int currentIdx = currentIndex, nextIdx = nextIndex;
			boolean hn = hasNext;
			while (hasNext) {coll.add(nextInt());}
			currentIndex = currentIdx;
			nextIndex = nextIdx;
			hasNext = hn;
			return coll;
		}
	}

	/**
	 * Constructs an empty set.
	 * This is usually less useful than just using the constructor, but can be handy
	 * in some code-generation scenarios when you don't know how many arguments you will have.
	 *
	 * @return a new set containing nothing
	 */
	public static IntSet with () {
		return new IntSet(0);
	}

	/**
	 * Creates a new IntSet that holds only the given item, but can be resized.
	 * @param item an int item
	 * @return a new IntSet that holds the given item
	 */
	public static IntSet with (int item) {
		IntSet set = new IntSet(1);
		set.add(item);
		return set;
	}

	/**
	 * Creates a new IntSet that holds only the given items, but can be resized.
	 * @param item0 an int item
	 * @param item1 an int item
	 * @return a new IntSet that holds the given items
	 */
	public static IntSet with (int item0, int item1) {
		IntSet set = new IntSet(2);
		set.add(item0);
		set.add(item1);
		return set;
	}

	/**
	 * Creates a new IntSet that holds only the given items, but can be resized.
	 * @param item0 an int item
	 * @param item1 an int item
	 * @param item2 an int item
	 * @return a new IntSet that holds the given items
	 */
	public static IntSet with (int item0, int item1, int item2) {
		IntSet set = new IntSet(3);
		set.add(item0);
		set.add(item1);
		set.add(item2);
		return set;
	}

	/**
	 * Creates a new IntSet that holds only the given items, but can be resized.
	 * @param item0 an int item
	 * @param item1 an int item
	 * @param item2 an int item
	 * @param item3 an int item
	 * @return a new IntSet that holds the given items
	 */
	public static IntSet with (int item0, int item1, int item2, int item3) {
		IntSet set = new IntSet(4);
		set.add(item0);
		set.add(item1);
		set.add(item2);
		set.add(item3);
		return set;
	}

	/**
	 * Creates a new IntSet that holds only the given items, but can be resized.
	 * @param item0 an int item
	 * @param item1 an int item
	 * @param item2 an int item
	 * @param item3 an int item
	 * @param item4 an int item
	 * @return a new IntSet that holds the given items
	 */
	public static IntSet with (int item0, int item1, int item2, int item3, int item4) {
		IntSet set = new IntSet(5);
		set.add(item0);
		set.add(item1);
		set.add(item2);
		set.add(item3);
		set.add(item4);
		return set;
	}

	/**
	 * Creates a new IntSet that holds only the given items, but can be resized.
	 * @param item0 an int item
	 * @param item1 an int item
	 * @param item2 an int item
	 * @param item3 an int item
	 * @param item4 an int item
	 * @param item5 an int item
	 * @return a new IntSet that holds the given items
	 */
	public static IntSet with (int item0, int item1, int item2, int item3, int item4, int item5) {
		IntSet set = new IntSet(6);
		set.add(item0);
		set.add(item1);
		set.add(item2);
		set.add(item3);
		set.add(item4);
		set.add(item5);
		return set;
	}

	/**
	 * Creates a new IntSet that holds only the given items, but can be resized.
	 * @param item0 an int item
	 * @param item1 an int item
	 * @param item2 an int item
	 * @param item3 an int item
	 * @param item4 an int item
	 * @param item5 an int item
	 * @param item6 an int item
	 * @return a new IntSet that holds the given items
	 */
	public static IntSet with (int item0, int item1, int item2, int item3, int item4, int item5, int item6) {
		IntSet set = new IntSet(7);
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
	 * Creates a new IntSet that holds only the given items, but can be resized.
	 * @param item0 an int item
	 * @param item1 an int item
	 * @param item2 an int item
	 * @param item3 an int item
	 * @param item4 an int item
	 * @param item5 an int item
	 * @param item6 an int item
	 * @return a new IntSet that holds the given items
	 */
	public static IntSet with (int item0, int item1, int item2, int item3, int item4, int item5, int item6, int item7) {
		IntSet set = new IntSet(8);
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
	 * Creates a new IntSet that holds only the given items, but can be resized.
	 * This overload will only be used when an array is supplied and the type of the
	 * items requested is the component type of the array, or if varargs are used and
	 * there are 9 or more arguments.
	 * @param varargs an int varargs or int array; remember that varargs allocate
	 * @return a new IntSet that holds the given items
	 */
	public static IntSet with (int... varargs) {
		return new IntSet(varargs);
	}
}

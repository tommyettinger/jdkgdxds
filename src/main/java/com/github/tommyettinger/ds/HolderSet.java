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

import org.checkerframework.checker.nullness.qual.Nullable;
import java.io.Serializable;
import java.util.*;
import java.util.function.Function;

import static com.github.tommyettinger.ds.Utilities.tableSize;

/**
 * An unordered set where the items are objects but access to items is done with a specific key component
 * held by each item, and the component is extracted from an item by a given Function. Neither null items
 * nor null keys are allowed. No allocation is done except when growing the table size. The function is
 * called the extractor, and is often a method reference.
 * <br>
 * Some operations are different here from a normal Set; {@link #contains(Object)} expects a K key, and
 * {@link #remove(Object)} does as well. You can use {@link #get(Object)} to go back to a T value from a
 * K key.
 * <br>
 * This class is currently a work in progress, and documentation may be inaccurate.
 *
 * @author Nathan Sweet
 * @author Tommy Ettinger
 */
public class HolderSet<T, K> implements Iterable<T>, Set<T>, Serializable {
	private static final long serialVersionUID = 0L;

	protected int size;

	protected T[] keyTable;

	protected final float loadFactor;
	protected int threshold;

	protected int shift;

	/**
	 * A bitmask used to confine hashcodes to the size of the table. Must be all 1 bits in its low positions, ie a power of two
	 * minus 1.
	 */
	protected int mask;
	@Nullable
	protected ObjectSetIterator<T, K> iterator1;
	@Nullable
	protected ObjectSetIterator<T, K> iterator2;
	@Nullable
	protected Function<T, K> extractor;

	/**
	 * Creates a new set with an initial capacity of 51 and a load factor of 0.8.
	 */
	public HolderSet () {

		loadFactor = 0.8f;

		int tableSize = tableSize(51, loadFactor);
		threshold = (int)(tableSize * loadFactor);
		mask = tableSize - 1;
		shift = Long.numberOfLeadingZeros(mask);

		keyTable = (T[])new Object[tableSize];
		this.extractor = null;
	}

	/**
	 * Creates a new set with an initial capacity of 51 and a load factor of 0.8.
	 */
	public HolderSet (Function<T, K> extractor) {
		this(extractor, 51, 0.8f);
	}

	/**
	 * Creates a new set with a load factor of 0.8.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 */
	public HolderSet (Function<T, K> extractor, int initialCapacity) {
		this(extractor, initialCapacity, 0.8f);
	}

	/**
	 * Creates a new set with the specified initial capacity and load factor. This set will hold initialCapacity items before
	 * growing the backing table.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 */
	public HolderSet (Function<T, K> extractor, int initialCapacity, float loadFactor) {
		if (loadFactor <= 0f || loadFactor > 1f) {
			throw new IllegalArgumentException("loadFactor must be > 0 and < 1: " + loadFactor);
		}
		this.loadFactor = loadFactor;

		int tableSize = tableSize(initialCapacity, loadFactor);
		threshold = (int)(tableSize * loadFactor);
		mask = tableSize - 1;
		shift = Long.numberOfLeadingZeros(mask);

		keyTable = (T[])new Object[tableSize];
		this.extractor = extractor;
	}

	/**
	 * Creates a new set identical to the specified set.
	 */
	public HolderSet (HolderSet<T, K> set) {
		loadFactor = set.loadFactor;
		threshold = set.threshold;
		mask = set.mask;
		shift = set.shift;
		keyTable = Arrays.copyOf(set.keyTable, set.keyTable.length);
		size = set.size;
		extractor = set.extractor;
	}

	/**
	 * Creates a new set that contains all distinct elements in {@code coll}.
	 */
	public HolderSet (Function<T, K> extractor, Collection<? extends T> coll) {
		this(extractor, coll.size());
		addAll(coll);
	}

	/**
	 * Gets the function this uses to extract keys from items.
	 * This may be null if {@link #HolderSet()} was used to construct this object (or technically if
	 * {@link #HolderSet(HolderSet)} was used to copy a HolderSet with an invalid extractor); in that
	 * case, this cannot have items added, removed, or inserted until a valid extractor is set with
	 * {@link #setExtractor(Function)}.
	 *
	 * @return the extractor function this uses to get keys from items
	 */
	@Nullable
	public Function<T, K> getExtractor () {
		return extractor;
	}

	/**
	 * Sets the function this will use to extract keys from items; this will only have an effect if
	 * the extractor function is currently null/invalid. This is typically needed if {@link #HolderSet()}
	 * was used to construct the HolderSet, but can also be required if {@link #HolderSet(HolderSet)} was
	 * used to copy another HolderSet with an invalid extractor. All other cases should require the
	 * extractor function to be specified at construction-time.
	 *
	 * @param extractor a Function that takes a T and gets a unique K from it; often a method reference
	 */
	public void setExtractor (Function<T, K> extractor) {
		if (this.extractor == null)
			this.extractor = extractor;
	}

	/**
	 * Returns an index &gt;= 0 and &lt;= {@link #mask} for the specified {@code item}, which here
	 * should be a K.
	 * <p>
	 * The default behavior uses Fibonacci hashing; it simply gets the {@link Object#hashCode()}
	 * of {@code item}, multiplies it by a specific long constant related to the golden ratio,
	 * and makes an unsigned right shift by {@link #shift} before casting to int and returning.
	 * This can be overridden to hash {@code item} differently, though all implementors must
	 * ensure this returns results in the range of 0 to {@link #mask}, inclusive. If nothing
	 * else is changed, then unsigned-right-shifting an int or long by {@link #shift} will also
	 * restrict results to the correct range.
	 *
	 * @param item a non-null Object; its hashCode() method should be used by most implementations.
	 */
	protected int place (Object item) {
		return (int)(item.hashCode() * 0x9E3779B97F4A7C15L >>> shift);
	}

	/**
	 * Returns the index of the key if already present, else {@code ~index} for the next empty index. This can be overridden
	 * to compare for equality differently than {@link Object#equals(Object)}. This expects key to be a K
	 * object, not a T item, and will extract keys from existing items to compare against.
	 * <p>
	 * If source is not easily available and you want to override this, the reference source is:
	 * <pre>
	 *     protected int locateKey (Object key) {
	 *         T[] keyTable = this.keyTable;
	 *         for (int i = place(key); ; i = i + 1 &amp; mask) {
	 *             T other = keyTable[i];
	 *             if (other == null) {
	 *                 return ~i; // Always negative; means empty space is available at i.
	 *             }
	 *             if (key.equals(extractor.apply(other))) // If you want to change how equality is determined, do it here.
	 *             {
	 *                 return i; // Same key was found.
	 *             }
	 *         }
	 *     }
	 * </pre>
	 *
	 * @param key a non-null Object that should probably be a K
	 */
	protected int locateKey (Object key) {
		T[] keyTable = this.keyTable;
		for (int i = place(key); ; i = i + 1 & mask) {
			T other = keyTable[i];
			if (other == null) {
				return ~i; // Always negative; means empty space is available at i.
			}
			if (key.equals(extractor.apply(other))) // If you want to change how equality is determined, do it here.
			{
				return i; // Same key was found.
			}
		}
	}

	/**
	 * Returns true if the item was not already in the set. If this set already contains the item,
	 * the call leaves the set unchanged and returns false.
	 * Note that this does not take a K key, but a T item
	 */
	@Override
	public boolean add (T key) {
		int i = locateKey(extractor.apply(key));
		if (i >= 0) {
			return false; // Existing key was found.
		}
		i = ~i; // Empty space was found.
		keyTable[i] = key;
		if (++size >= threshold) {
			resize(keyTable.length << 1);
		}
		return true;
	}

	@Override
	public boolean containsAll (Collection<?> c) {
		for (Object o : c) {
			if (!contains(o)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean addAll (Collection<? extends T> coll) {
		final int length = coll.size();
		ensureCapacity(length);
		int oldSize = size;
		for (T t : coll) {
			add(t);
		}
		return oldSize != size;

	}

	/**
	 * Makes this Set retain a Collection of K key types (not T items).
	 * @param c a Collection that should hold K keys to retain in this
	 * @return true if this Set was modified
	 */
	@Override
	public boolean retainAll (Collection<?> c) {
		boolean modified = false;
		for (Object o : this) {
			if (!c.contains(o)) {
				modified |= remove(o);
			}

		}
		return modified;
	}
	/**
	 * Removes from this Set a Collection of K key types (not T items).
	 * @param c a Collection that should hold K keys to remove from this
	 * @return true if this Set was modified
	 */

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
		for (int i = offset, n = i + length; i < n; i++) {
			add(array[i]);
		}
		return oldSize != size;
	}

	public void addAll (HolderSet<T, K> set) {
		ensureCapacity(set.size);
		T[] keyTable = set.keyTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			T key = keyTable[i];
			if (key != null) {
				add(key);
			}
		}
	}

	/**
	 * Skips checks for existing keys, doesn't increment size.
	 */
	private void addResize (T key) {
		T[] keyTable = this.keyTable;
		for (int i = place(key); ; i = i + 1 & mask) {
			if (keyTable[i] == null) {
				keyTable[i] = key;
				return;
			}
		}
	}

	/**
	 * Takes a K key and not a T value!
	 * Returns true if the key was removed.
	 */
	@Override
	public boolean remove (Object key) {
		int i = locateKey(key);
		if (i < 0) {
			return false;
		}
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
		if (maximumCapacity < 0) {
			throw new IllegalArgumentException("maximumCapacity must be >= 0: " + maximumCapacity);
		}
		int tableSize = tableSize(maximumCapacity, loadFactor);
		if (keyTable.length > tableSize) {
			resize(tableSize);
		}
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
		if (size == 0) {
			return;
		}
		size = 0;
		Arrays.fill(keyTable, null);
	}

	/**
	 * Checks for the presence of a K key, not a T value.
	 *
	 * @param key may be any non-null Object, but should be a K key
	 * @return true if this contains the given key
	 */
	@Override
	public boolean contains (Object key) {
		return locateKey(key) >= 0;
	}

	/**
	 * Given a K key that could have been extracted or extractable from a T item in this,
	 * this returns the T item that holds that key, or null if no item holds key.
	 * @param key a K key that could have been extracted from a T item in this
	 * @return the T item that holds the given key, or null if none was found
	 */
	@Nullable
	public T get (Object key) {
		int i = locateKey(key);
		return i < 0 ? null : keyTable[i];
	}

	public T first () {
		T[] keyTable = this.keyTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			if (keyTable[i] != null) {
				return keyTable[i];
			}
		}
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
		if (keyTable.length < tableSize) {
			resize(tableSize);
		}
	}

	protected void resize (int newSize) {
		int oldCapacity = keyTable.length;
		threshold = (int)(newSize * loadFactor);
		mask = newSize - 1;
		shift = Long.numberOfLeadingZeros(mask);
		T[] oldKeyTable = keyTable;

		keyTable = (T[])new Object[newSize];

		if (size > 0) {
			for (int i = 0; i < oldCapacity; i++) {
				T key = oldKeyTable[i];
				if (key != null) {
					addResize(key);
				}
			}
		}
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

	public int hashCode () {
		int h = size;
		T[] keyTable = this.keyTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			T key = keyTable[i];
			if (key != null) {
				h += key.hashCode();
			}
		}
		return h;
	}

	public boolean equals (Object obj) {
		if (!(obj instanceof HolderSet)) {
			return false;
		}
		HolderSet other = (HolderSet)obj;
		if (other.size != size) {
			return false;
		}
		T[] keyTable = this.keyTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			if (keyTable[i] != null && !other.contains(keyTable[i])) {
				return false;
			}
		}
		return true;
	}

	public String toString () {
		return '{' + toString(", ") + '}';
	}

	public String toString (String separator) {
		if (size == 0) {
			return "";
		}
		StringBuilder buffer = new StringBuilder(32);
		T[] keyTable = this.keyTable;
		int i = keyTable.length;
		while (i-- > 0) {
			T key = keyTable[i];
			if (key == null) {
				continue;
			}
			buffer.append(key == this ? "(this)" : key);
			break;
		}
		while (i-- > 0) {
			T key = keyTable[i];
			if (key == null) {
				continue;
			}
			buffer.append(separator);
			buffer.append(key == this ? "(this)" : key);
		}
		return buffer.toString();
	}

	/**
	 * Returns an iterator for the keys in the set. Remove is supported.
	 * <p>
	 * Reuses one of two iterators for this set. For nested or multithreaded
	 * iteration, use {@link ObjectSetIterator#ObjectSetIterator(HolderSet)}.
	 */
	@Override
	public Iterator<T> iterator () {
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

	@SafeVarargs
	public static <T, K> HolderSet<T, K> with (Function<T, K> extractor, T... array) {
		HolderSet<T, K> set = new HolderSet<T, K>(extractor, array.length);
		set.addAll(array);
		return set;
	}

	public static class ObjectSetIterator<T, K> implements Iterable<T>, Iterator<T> {
		public boolean hasNext;

		final HolderSet<T, K> set;
		int nextIndex, currentIndex;
		boolean valid = true;

		public ObjectSetIterator (HolderSet<T, K> set) {
			this.set = set;
			reset();
		}

		public void reset () {
			currentIndex = -1;
			nextIndex = -1;
			findNextIndex();
		}

		private void findNextIndex () {
			T[] keyTable = set.keyTable;
			for (int n = set.keyTable.length; ++nextIndex < n; ) {
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
			if (i < 0) {
				throw new IllegalStateException("next must be called before remove.");
			}
			T[] keyTable = set.keyTable;
			int mask = set.mask, next = i + 1 & mask;
			T key;
			while ((key = keyTable[next]) != null) {
				int placement = set.place(set.extractor.apply(key));
				if ((next - placement & mask) > (i - placement & mask)) {
					keyTable[i] = key;
					i = next;
				}
				next = next + 1 & mask;
			}
			keyTable[i] = null;
			set.size--;
			if (i != currentIndex) {
				--nextIndex;
			}
			currentIndex = -1;
		}

		@Override
		public boolean hasNext () {
			if (!valid) {
				throw new RuntimeException("#iterator() cannot be used nested.");
			}
			return hasNext;
		}

		@Override
		public T next () {
			if (!hasNext) {
				throw new NoSuchElementException();
			}
			if (!valid) {
				throw new RuntimeException("#iterator() cannot be used nested.");
			}
			T key = set.keyTable[nextIndex];
			currentIndex = nextIndex;
			findNextIndex();
			return key;
		}

		@Override
		public ObjectSetIterator<T, K> iterator () {
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
			while (hasNext) {
				list.add(next());
			}
			currentIndex = currentIdx;
			nextIndex = nextIdx;
			hasNext = hn;
			return list;
		}
	}
}

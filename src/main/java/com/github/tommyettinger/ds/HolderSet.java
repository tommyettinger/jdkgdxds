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
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import com.github.tommyettinger.function.ObjToObjFunction;

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
 *
 * @author Nathan Sweet
 * @author Tommy Ettinger
 */
public class HolderSet<T, K> implements Iterable<T>, Set<T>, EnhancedCollection<T>{

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
	 * Used by {@link #place(Object)} typically, this should always equal {@code com.github.tommyettinger.digital.BitConversion.countLeadingZeros(mask)}.
	 * For a table that could hold 2 items (with 1 bit indices), this would be {@code 64 - 1 == 63}. For a table that
	 * could hold 256 items (with 8 bit indices), this would be {@code 64 - 8 == 56}.
	 */
	protected int shift;

	/**
	 * Used by {@link #place(Object)} to mix hashCode() results. Changes on every call to {@link #resize(int)} by default.
	 * This only needs to be serialized if the full key and value tables are serialized, or if the iteration order should be
	 * the same before and after serialization. Iteration order is better handled by using {@link ObjectOrderedSet}.
	 */
	protected int hashMultiplier = 0xB7AD9447;

	/**
	 * A bitmask used to confine hashcodes to the size of the table. Must be all 1 bits in its low positions, ie a power of two
	 * minus 1. If {@link #place(Object)} is overridden, this can be used instead of {@link #shift} to isolate usable bits of a
	 * hash.
	 */
	protected int mask;
	@Nullable protected transient HolderSetIterator<T, K> iterator1;
	@Nullable protected transient HolderSetIterator<T, K> iterator2;
	@Nullable protected transient ObjToObjFunction<T, K> extractor;

	/**
	 * Creates a new set with an initial capacity of 51 and a load factor of {@link Utilities#getDefaultLoadFactor()}. This does not set the
	 * extractor, so the HolderSet will not be usable until {@link #setExtractor(ObjToObjFunction)} is called with
	 * a valid ObjToObjFunction that gets K keys from T items.
	 */
	@SuppressWarnings("unchecked")
	public HolderSet () {

		loadFactor = Utilities.getDefaultLoadFactor();

		int tableSize = tableSize(51, loadFactor);
		threshold = (int)(tableSize * loadFactor);
		mask = tableSize - 1;
		shift = BitConversion.countLeadingZeros(mask) + 32;

		keyTable = (T[])new Object[tableSize];
		this.extractor = null;
	}

	/**
	 * Creates a new set with an initial capacity of 51 and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 *
	 * @param extractor a function that will be used to extract K keys from the T items put into this
	 */
	public HolderSet (ObjToObjFunction<T, K> extractor) {
		this(extractor, 51, Utilities.getDefaultLoadFactor());
	}

	/**
	 * Creates a new set with a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 *
	 * @param extractor       a function that will be used to extract K keys from the T items put into this
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 */
	public HolderSet (ObjToObjFunction<T, K> extractor, int initialCapacity) {
		this(extractor, initialCapacity, Utilities.getDefaultLoadFactor());
	}

	/**
	 * Creates a new set with the specified initial capacity and load factor. This set will hold initialCapacity items before
	 * growing the backing table.
	 *
	 * @param extractor       a function that will be used to extract K keys from the T items put into this
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 * @param loadFactor      what fraction of the capacity can be filled before this has to resize; 0 &lt; loadFactor &lt;= 1
	 */
	@SuppressWarnings("unchecked")
	public HolderSet (@NonNull ObjToObjFunction<T, K> extractor, int initialCapacity, float loadFactor) {
		if (loadFactor <= 0f || loadFactor > 1f) {
			throw new IllegalArgumentException("loadFactor must be > 0 and <= 1: " + loadFactor);
		}
		this.loadFactor = loadFactor;

		int tableSize = tableSize(initialCapacity, loadFactor);
		threshold = (int)(tableSize * loadFactor);
		mask = tableSize - 1;
		shift = BitConversion.countLeadingZeros(mask) + 32;

		keyTable = (T[])new Object[tableSize];
		this.extractor = extractor;
	}

	/**
	 * Creates a new set identical to the specified set.
	 * This doesn't copy the extractor; instead it references the same ObjToObjFunction from the argument.
	 * This can have issues if the extractor causes side effects or is stateful.
	 */
	public HolderSet (HolderSet<T, K> set) {
		loadFactor = set.loadFactor;
		threshold = set.threshold;
		mask = set.mask;
		shift = set.shift;
		keyTable = Arrays.copyOf(set.keyTable, set.keyTable.length);
		size = set.size;
		extractor = set.extractor;
		hashMultiplier = set.hashMultiplier;
	}

	/**
	 * Creates a new set that contains all distinct elements in {@code coll}, using {@code extractor} to get the keys that determine distinctness.
	 *
	 * @param extractor a function that will be used to extract K keys from the T items in coll
	 * @param coll      a Collection of T items; depending on extractor, some different T items may not be added because their K key is equal
	 */
	public HolderSet (ObjToObjFunction<T, K> extractor, Collection<? extends T> coll) {
		this(extractor, coll.size());
		addAll(coll);
	}

	/**
	 * Creates a new set that contains all distinct elements in {@code items}, using {@code extractor} to get the keys that determine distinctness.
	 *
	 * @param extractor a function that will be used to extract K keys from the T items in coll
	 * @param items     an array of T items; depending on extractor, some different T items may not be added because their K key is equal
	 */
	public HolderSet (ObjToObjFunction<T, K> extractor, T[] items) {
		this(extractor, items.length);
		addAll(items);
	}

	/**
	 * Gets the function this uses to extract keys from items.
	 * This may be null if {@link #HolderSet()} was used to construct this object (or technically if
	 * {@link #HolderSet(HolderSet)} was used to copy a HolderSet with an invalid extractor); in that
	 * case, this cannot have items added, removed, or inserted until a valid extractor is set with
	 * {@link #setExtractor(ObjToObjFunction)}.
	 *
	 * @return the extractor function this uses to get keys from items
	 */
	@Nullable
	public ObjToObjFunction<T, K> getExtractor () {
		return extractor;
	}

	/**
	 * Sets the function this will use to extract keys from items; this will only have an effect if
	 * the extractor function is currently null/invalid. This is typically needed if {@link #HolderSet()}
	 * was used to construct the HolderSet, but can also be required if {@link #HolderSet(HolderSet)} was
	 * used to copy another HolderSet with an invalid extractor. All other cases should require the
	 * extractor function to be specified at construction-time.
	 *
	 * @param extractor a ObjToObjFunction that takes a T and gets a unique K from it; often a method reference
	 */
	public void setExtractor (@NonNull ObjToObjFunction<T, K> extractor) {
		if (this.extractor == null)
			this.extractor = extractor;
	}

	/**
	 * Returns an index &gt;= 0 and &lt;= {@link #mask} for the specified {@code item}, mixed.
	 * <p>
	 * The default behavior uses a basic hash mixing family; it gets the
	 * {@link Object#hashCode()} of {@code item}, does some no-op bitwise math to satisfy GWT,
	 * multiplies that by the current {@link #hashMultiplier}, and makes an unsigned right shift
	 * by {@link #shift} before returning. Because the hashMultiplier changes every time the backing
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
	@SuppressWarnings("PointlessBitwiseExpression")
    protected int place (@NonNull Object item) {
		return (item.hashCode() | 0) * hashMultiplier >>> shift;
		// This can be used if you know hashCode() has few collisions normally, and won't be maliciously manipulated.
//		return item.hashCode() & mask;
	}

	/**
	 * Compares the objects left and right, which should be K keys (not T items), for equality, returning true if they are considered
	 * equal. This is used by the rest of this class to determine whether two keys are considered equal. Normally, this
	 * returns {@code left.equals(right)}, but subclasses can override it to use reference equality, fuzzy equality, deep
	 * array equality, or any other custom definition of equality. Usually, {@link #place(Object)} is also overridden if
	 * this method is.
	 *
	 * @param left  must be non-null; typically a K key being compared, but not necessarily
	 * @param right may be null; typically a K key being compared, but can often be null for an empty key slot, or some other type
	 * @return true if left and right are considered equal for the purposes of this class
	 */
	protected boolean equate (Object left, @Nullable Object right) {
		return left.equals(right);
	}

	/**
	 * Returns the index of the key if already present, else {@code ~index} for the next empty index. This calls
	 * {@link #equate(Object, Object)} to determine if two keys are equivalent. This expects key to be a K
	 * object, not a T item, and will extract keys from existing items to compare against.
	 *
	 * @param key a non-null Object that should probably be a K
	 */
	protected int locateKey (@NonNull Object key) {
		T[] keyTable = this.keyTable;
		for (int i = place(key); ; i = i + 1 & mask) {
			T other = keyTable[i];
			if (other == null) {
				return ~i; // Always negative; means empty space is available at i.
			}
			assert extractor != null;
			if (equate(key, extractor.apply(other))) {
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
		assert extractor != null;
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
	public boolean addAll (Collection<? extends T> coll) {
		final int length = coll.size();
		ensureCapacity(length);
		int oldSize = size;
		for (T t : coll) {
			add(t);
		}
		return oldSize != size;

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

	public boolean addAll (HolderSet<T, ?> set) {
		ensureCapacity(set.size);
		T[] keyTable = set.keyTable;
		int oldSize = size;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			T key = keyTable[i];
			if (key != null) {
				add(key);
			}
		}
		return oldSize != size;
	}

	/**
	 * Makes this Set retain a Collection of K key types (not T items).
	 *
	 * @param c a Collection that should hold K keys to retain in this
	 * @return true if this Set was modified
	 */
	@Override
	public boolean retainAll (@NonNull Collection<@NonNull ?> c) {
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
	 *
	 * @param c a Collection that should hold K keys to remove from this
	 * @return true if this Set was modified
	 */
	@Override
	public boolean removeAll (Collection<@NonNull ?> c) {
		boolean modified = false;
		for (Object o : c) {
			modified |= remove(o);
		}
		return modified;
	}

	public boolean removeAll (@NonNull Object[] values) {
		boolean modified = false;
		for (Object o : values) {
			modified |= remove(o);
		}
		return modified;
	}

	public boolean removeAll (@NonNull Object[] values, int offset, int length) {
		boolean modified = false;
		for (int i = offset, n = 0; n < length && i < values.length; i++, n++) {
			modified |= remove(values[i]);
		}
		return modified;
	}

	@Override
	public boolean containsAll (Collection<@NonNull ?> c) {
		for (Object o : c) {
			if (!contains(o)) {return false;}
		}
		return true;
	}

	/**
	 * Exactly like {@link #containsAll(Collection)}, but takes an array instead of a Collection.
	 * @see #containsAll(Collection)
	 * @param array array to be checked for containment in this set
	 * @return {@code true} if this set contains all the elements
	 * in the specified array
	 */
	public boolean containsAll (@NonNull Object[] array) {
		for (Object o : array) {
			if (!contains(o))
				return false;
		}
		return true;
	}

	/**
	 * Like {@link #containsAll(Object[])}, but only uses at most {@code length} items from {@code array}, starting at {@code offset}.
	 * @see #containsAll(Object[])
	 * @param array array to be checked for containment in this set
	 * @param offset the index of the first item in array to check
	 * @param length how many items, at most, to check from array
	 * @return {@code true} if this set contains all the elements
	 * in the specified range of array
	 */
	public boolean containsAll (@NonNull Object[] array, int offset, int length) {
		for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
			if(!contains(array[i])) return false;
		}
		return true;
	}

	/**
	 * Returns true if this set contains any of the specified values.
	 *
	 * @param values must not contain nulls, and must not be null itself
	 * @return true if this set contains any of the items in {@code values}, false otherwise
	 */
	public boolean containsAny (Iterable<@NonNull ?> values) {
		for (Object v : values) {
			if (contains(v)) {return true;}
		}
		return false;
	}

	/**
	 * Returns true if this set contains any of the specified values.
	 *
	 * @param values must not contain nulls, and must not be null itself
	 * @return true if this set contains any of the items in {@code values}, false otherwise
	 */
	public boolean containsAny (@NonNull Object[] values) {
		for (Object v : values) {
			if (contains(v)) {return true;}
		}
		return false;
	}

	/**
	 * Returns true if this set contains any items from the specified range of values.
	 *
	 * @param values must not contain nulls, and must not be null itself
	 * @param offset the index to start checking in values
	 * @param length how many items to check from values
	 * @return true if this set contains any of the items in the given range of {@code values}, false otherwise
	 */
	public boolean containsAny (@NonNull Object[] values, int offset, int length) {
		for (int i = offset, n = 0; n < length && i < values.length; i++, n++) {
			if (contains(values[i])) {return true;}
		}
		return false;
	}

	/**
	 * Skips checks for existing keys, doesn't increment size.
	 */
	protected void addResize (@NonNull T key) {
		assert extractor != null;
		T[] keyTable = this.keyTable;
		for (int i = place(extractor.apply(key)); ; i = i + 1 & mask) {
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
	@SuppressWarnings("unchecked")
	@Override
	public boolean remove (Object key) {
		int i = locateKey(key);
		if (i < 0) {
			return false;
		}
		assert extractor != null;
		T[] keyTable = this.keyTable;
		int mask = this.mask, next = i + 1 & mask;
		while ((key = keyTable[next]) != null) {
			T tk = (T)key;
			int placement = place(extractor.apply(tk));
			if ((next - placement & mask) > (i - placement & mask)) {
				keyTable[i] = tk;
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
		return size != 0;
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
		int tableSize = tableSize(Math.max(maximumCapacity, size), loadFactor);
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
		Utilities.clear(keyTable);
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
	 *
	 * @param key a K key that could have been extracted from a T item in this
	 * @return the T item that holds the given key, or null if none was found
	 */
	@Nullable
	public T get (Object key) {
		int i = locateKey(key);
		return i < 0 ? null : keyTable[i];
	}

	/**
	 * Given a K key that could have been extracted or extractable from a T item in this,
	 * this returns the T item that holds that key, or {@code defaultValue} if no item holds key.
	 *
	 * @param key a K key that could have been extracted from a T item in this
	 * @param defaultValue the T value to return if key could not be found
	 * @return the T item that holds the given key, or {@code defaultValue} if none was found
	 */
	@Nullable
	public T getOrDefault (Object key, T defaultValue) {
		int i = locateKey(key);
		return i < 0 ? defaultValue : keyTable[i];
	}

	/**
	 * Gets the (arbitrarily-chosen) first item in this HolderSet. Which item is "first" can change
	 * when this resizes, and you can't rely on the order of items in an unordered set like this.
	 *
	 * @return the "first" item in this HolderSet; really an arbitrary item in this
	 * @throws IllegalStateException if this HolderSet is empty
	 */
	public T first () {
		T[] keyTable = this.keyTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			if (keyTable[i] != null) {
				return keyTable[i];
			}
		}
		throw new IllegalStateException("HolderSet is empty.");
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

	@SuppressWarnings("unchecked")
	protected void resize (int newSize) {
		int oldCapacity = keyTable.length;
		threshold = (int)(newSize * loadFactor);
		mask = newSize - 1;
		shift = BitConversion.countLeadingZeros(mask) + 32;
		T[] oldKeyTable = keyTable;

		hashMultiplier = Utilities.GOOD_MULTIPLIERS[hashMultiplier  * shift >>> 5 & 511];
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

	/**
	 * Gets the current hash multiplier as used by {@link #place(Object)}; for specific advanced usage only.
	 * The hash multiplier changes whenever {@link #resize(int)} is called, though its value before the resize
	 * affects its value after.
	 * @return the current hash multiplier, which should always be a large odd int
	 */
	public int getHashMultiplier () {
		return hashMultiplier;
	}

	/**
	 * Sets the current hash multiplier, then immediately calls {@link #resize(int)} without changing the target size; this
	 * is for specific advanced usage only. Calling resize() will change the multiplier before it gets used, and the current
	 * {@link #size()} of the data structure also changes the value. The hash multiplier is used by {@link #place(Object)}.
	 * The hash multiplier must be an odd int, and should usually be "rather large." Here, that means the absolute value of
	 * the multiplier should be at least a billion or so (roughly {@code 0x40000000} in hex). The
	 * only validation this does is to ensure the multiplier is odd; everything else is up to the caller. The hash multiplier
	 * changes whenever {@link #resize(int)} is called, though its value before the resize affects its value after. Because
	 * of how resize() randomizes the multiplier, even inputs such as {@code 1} and {@code -1} actually work well.
	 * <br>
	 * This is accessible at all mainly so serialization code that has a need to access the hash multiplier can do so, but
	 * also to provide an "emergency escape route" in case of hash flooding. Using one of the "known good" ints in
	 * {@link Utilities#GOOD_MULTIPLIERS} should usually be fine if you don't know what multiplier will work well.
	 * Be advised that because this has to call resize(), it isn't especially fast, and it slows
	 * down the more items are in the data structure. If you in a situation where you are worried about hash flooding, you
	 * also shouldn't permit adversaries to cause this method to be called frequently.
	 * @param hashMultiplier any odd int; will not be used as-is
	 */
	public void setHashMultiplier (int hashMultiplier) {
		this.hashMultiplier = hashMultiplier | 1;
		resize(keyTable.length);
	}

	@Override
	public Object @NonNull [] toArray () {
		return toArray(new Object[size()]);
	}

	/**
	 * Returns an array containing all the elements in this set; the
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
	public <E> E @NonNull [] toArray (E @NonNull [] a) {
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
			if (key != null) {
				h += key.hashCode();
			}
		}
		return h;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public boolean equals (Object obj) {
		if (!(obj instanceof HolderSet)) {
			return false;
		}
		HolderSet other = (HolderSet)obj;
		if (other.size != size) {
			return false;
		}
		T[] keyTable = this.keyTable;
		assert extractor != null;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			if (keyTable[i] != null && !other.contains(extractor.apply(keyTable[i]))) {
				return false;
			}
		}
		return true;
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
	 * iteration, use {@link HolderSetIterator#HolderSetIterator(HolderSet)}.
	 */
	@Override
	public @NonNull HolderSetIterator<T, K> iterator () {
		if (iterator1 == null || iterator2 == null) {
			iterator1 = new HolderSetIterator<>(this);
			iterator2 = new HolderSetIterator<>(this);
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

	public static class HolderSetIterator<T, K> implements Iterable<T>, Iterator<T> {
		public boolean hasNext;

		protected final HolderSet<T, K> set;
		protected int nextIndex, currentIndex;
		protected boolean valid = true;

		public HolderSetIterator (HolderSet<T, K> set) {
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
				assert set.extractor != null;
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
			if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
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
		public @NonNull HolderSetIterator<T, K> iterator () {
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

	public static <T, K> HolderSet<T, K> with (ObjToObjFunction<T, K> extractor, T item) {
		HolderSet<T, K> set = new HolderSet<>(extractor, 1);
		set.add(item);
		return set;
	}

	@SafeVarargs
	public static <T, K> HolderSet<T, K> with (ObjToObjFunction<T, K> extractor, T... array) {
		return new HolderSet<>(extractor, array);
	}
}

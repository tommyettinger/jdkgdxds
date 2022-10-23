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

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * An Ordered Set of {@code T} items where the {@link #indexOf(Object)} operation runs in constant time, but
 * any removal from the middle of the order runs in linear time. If you primarily append to a Set with
 * {@link #add(Object)}, this will perform like {@link ObjectIntOrderedMap}, since it's backed internally by
 * one of those Maps; indexOf() delegates to the map's {@link ObjectIntOrderedMap#get(Object)} method. This
 * has to do some bookkeeping to make sure the index for each item is stored as the value for the key matching
 * the item in the map. That bookkeeping will fail if you use the {@link Iterator#remove()} method on this
 * class' iterator; you can correct the indices with {@link #renumber()}, or {@link #renumber(int)} if you know
 * the first incorrect index.
 *
 * @param <T> the type of items; should implement {@link Object#equals(Object)} and {@link Object#hashCode()}
 */
public class NumberedSet<T> implements Set<T>, Ordered<T> {

	protected class InternalMap extends ObjectIntOrderedMap<T> {
		public InternalMap () {
			super();
		}

		public InternalMap (int initialCapacity) {
			super(initialCapacity);
		}

		public InternalMap (int initialCapacity, float loadFactor) {
			super(initialCapacity, loadFactor);
		}

		public InternalMap (ObjectIntOrderedMap<? extends T> map) {
			super(map);
		}

		public InternalMap (ObjectIntMap<? extends T> map) {
			super(map);
		}

		public InternalMap (T[] keys, int[] values) {
			super(keys, values);
		}

		public InternalMap (Collection<? extends T> keys, PrimitiveCollection.OfInt values) {
			super(keys, values);
		}

		@Override
		protected int place (Object item) {
			return NumberedSet.this.place(item);
		}

		@Override
		protected boolean equate (Object left, @Nullable Object right) {
			return NumberedSet.this.equate(left, right);
		}
	}

	protected transient InternalMap map;
	@Nullable protected transient NumberedSetIterator<T> iterator1;
	@Nullable protected transient NumberedSetIterator<T> iterator2;

	public NumberedSet () {
		this(51, Utilities.getDefaultLoadFactor());
	}

	public NumberedSet (int initialCapacity, float loadFactor) {
		map = new InternalMap(initialCapacity, loadFactor);
		map.setDefaultValue(-1);
	}

	public NumberedSet (int initialCapacity) {
		this(initialCapacity, Utilities.getDefaultLoadFactor());
	}

	public NumberedSet (NumberedSet<? extends T> other) {
		map = new InternalMap(other.map);
	}

	/**
	 * Can be used to make a NumberedSet from any {@link Ordered} map or set with Object keys or items, using
	 * the keys for a map and the items for a set.
	 *
	 * @param ordered any {@link Ordered} with the same type as this NumberSet
	 */
	public NumberedSet (Ordered<? extends T> ordered) {
		this(ordered.size());
		addAll(ordered.order());
	}

	/**
	 * Creates a new set that contains all distinct elements in {@code coll}.
	 *
	 * @param coll all distinct items in this Collection will become items in this NumberedSet
	 */
	public NumberedSet (Collection<? extends T> coll) {
		this(coll.size());
		addAll(coll);
	}

	/**
	 * Creates a new set that contains all distinct elements in {@code items}.
	 *
	 * @param items all distinct elements in this array will become items in this NumberedSet
	 */
	public NumberedSet (T[] items) {
		this(items.length);
		addAll(items);
	}

	/**
	 * Creates a new set by copying {@code count} items from the given Ordered, starting at {@code offset} in that Ordered,
	 * into this.
	 *
	 * @param other  another Ordered of the same type
	 * @param offset the first index in other's ordering to draw an item from
	 * @param count  how many items to copy from other
	 */
	public NumberedSet (Ordered<T> other, int offset, int count) {
		this(count);
		addAll(0, other, offset, count);
	}

	/**
	 * Returns an index &gt;= 0 and &lt;= {@link InternalMap#mask} for the specified {@code item}, mixed.
	 * <p>
	 * The default behavior uses a basic hash mixing family; it simply gets the
	 * {@link Object#hashCode()} of {@code item}, multiplies it by the current
	 * {@link InternalMap#hashMultiplier}, and makes an unsigned right shift by {@link InternalMap#shift} before
	 * casting to int and returning. Because the hashMultiplier changes every time the backing
	 * table resizes, if a problematic sequence of keys piles up with many collisions, that won't
	 * continue to cause problems when the next resize changes the hashMultiplier again. This
	 * doesn't have much way of preventing trouble from hashCode() implementations that always
	 * or very frequently return 0, but nothing really can handle that well.
	 * <br>
	 * This can be overridden to hash {@code item} differently, though all implementors must
	 * ensure this returns results in the range of 0 to {@link InternalMap#mask}, inclusive. If nothing
	 * else is changed, then unsigned-right-shifting an int or long by {@link InternalMap#shift} will also
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
	 * @return an index between 0 and {@link InternalMap#mask} (both inclusive)
	 */
	protected int place (Object item) {
		return (int)(item.hashCode() * map.hashMultiplier >>> map.shift);
		// This can be used if you know hashCode() has few collisions normally, and won't be maliciously manipulated.
//		return item.hashCode() & mask;
	}

	/**
	 * Compares the objects left and right, which are usually keys, for equality, returning true if they are considered
	 * equal. This is used by the rest of this class to determine whether two keys are considered equal. Normally, this
	 * returns {@code left.equals(right)}, but subclasses can override it to use reference equality, fuzzy equality, deep
	 * array equality, or any other custom definition of equality. Usually, {@link #place(Object)} is also overridden if
	 * this method is.
	 * <br>
	 * You can override this, which will affect the internal map that NumberedSet uses.
	 *
	 * @param left  must be non-null; typically a key being compared, but not necessarily
	 * @param right may be null; typically a key being compared, but can often be null for an empty key slot, or some other type
	 * @return true if left and right are considered equal for the purposes of this class
	 */
	protected boolean equate (Object left, @Nullable Object right) {
		return left.equals(right);
	}

	@Override
	public ObjectList<T> order () {
		return map.keys;
	}

	/**
	 * Reassigns all index values to match {@link #order()}.
	 * This should be called if you have removed any items using {@link Iterator#remove()} from this
	 * NumberedSet, since the iterator's remove() method doesn't update the numbering on its own.
	 * Use this method if you don't know the first incorrect index, or {@link #renumber(int)} if you do.
	 */
	public void renumber () {
		final int s = size();
		for (int i = 0; i < s; i++) {
			map.valueTable[map.locateKey(map.keys.get(i))] = i;
		}
	}

	/**
	 * Reassigns the index values for each index starting with {@code start}, and going to the end.
	 * This should be called if you have removed any items using {@link Iterator#remove()} from this
	 * NumberedSet, since the iterator's remove() method doesn't update the numbering on its own.
	 * Use {@link #renumber()} if you don't know the first incorrect index, or this method if you do.
	 *
	 * @param start the first index to reassign.
	 */
	public void renumber (final int start) {
		final int s = size();
		for (int i = start; i < s; i++) {
			map.valueTable[map.locateKey(map.keys.get(i))] = i;
		}
	}

	@Override
	public boolean remove (Object key) {
		int prev = size();
		map.remove(key);
		if (size() != prev) {
			renumber();
			return true;
		}
		return false;
	}

	@Override
	public boolean containsAll (Collection<?> c) {
		for (Object e : c) {
			if (!map.containsKey(e))
				return false;
		}
		return true;
	}

	@Override
	public boolean addAll (Collection<? extends T> c) {
		boolean modified = false;
		for (T t : c)
			modified |= add(t);
		return modified;
	}

	/**
	 * Adds up to {@code count} items, starting from {@code offset}, in the Ordered {@code other} to this set,
	 * inserting at the end of the iteration order.
	 *
	 * @param other  a non-null {@link Ordered} of {@code T}
	 * @param offset the first index in {@code other} to use
	 * @param count  how many indices in {@code other} to use
	 * @return true if this is modified by this call, as {@link #addAll(Collection)} does
	 */
	public boolean addAll (Ordered<T> other, int offset, int count) {
		return addAll(map.size, other, offset, count);
	}

	/**
	 * Adds up to {@code count} items, starting from {@code offset}, in the Ordered {@code other} to this set,
	 * inserting starting at {@code insertionIndex} in the iteration order.
	 *
	 * @param insertionIndex where to insert into the iteration order
	 * @param other          a non-null {@link Ordered} of {@code T}
	 * @param offset         the first index in {@code other} to use
	 * @param count          how many indices in {@code other} to use
	 * @return true if this is modified by this call, as {@link #addAll(Collection)} does
	 */
	public boolean addAll (int insertionIndex, Ordered<T> other, int offset, int count) {
		boolean changed = false;
		int end = Math.min(offset + count, other.size());
		ensureCapacity(end - offset);
		for (int i = offset; i < end; i++) {
			add(insertionIndex++, other.order().get(i));
			changed = true;
		}
		return changed;
	}

	public boolean addAll (T[] array) {
		return addAll(array, 0, array.length);
	}

	public boolean addAll (T[] array, int offset, int length) {
		ensureCapacity(length);
		int oldSize = size();
		for (int i = offset, n = i + length; i < n; i++) {add(array[i]);}
		return oldSize != size();
	}

	@Override
	public boolean retainAll (Collection<?> c) {
		boolean modified = false;
		Iterator<T> it = iterator();
		while (it.hasNext()) {
			if (!c.contains(it.next())) {
				it.remove();
				modified = true;
			}
		}
		if (modified) {
			renumber();
			return true;
		}
		return false;
	}

	@Override
	public boolean removeAll (Collection<?> c) {
		boolean modified = false;
		Iterator<?> it = iterator();
		while (it.hasNext()) {
			if (c.contains(it.next())) {
				it.remove();
				modified = true;
			}
		}
		if (modified) {
			renumber();
			return true;
		}
		return false;
	}

	/**
	 * Removes and returns the item at the given index in this set's order.
	 *
	 * @param index the index of the item to remove
	 * @return the removed item
	 */
	public T removeAt (int index) {
		T old = map.keyAt(index);
		map.removeAt(index);
		renumber(index);
		return old;
	}

	public void ensureCapacity (int additionalCapacity) {
		map.ensureCapacity(additionalCapacity);
	}

	public boolean alter (T before, T after) {
		return map.alter(before, after);
	}

	public boolean alterAt (int index, T after) {
		return map.alterAt(index, after);
	}

	public T getAt (int index) {
		return map.keyAt(index);
	}

	public void clear (int maximumCapacity) {
		map.clear(maximumCapacity);
	}

	@Override
	public void clear () {
		map.clear();
	}

	public String toString (String separator, boolean braces) {
		return map.toString(separator, braces);
	}

	public int indexOf (Object key) {
		return map.get(key);
	}

	public int indexOfOrDefault (Object key, int defaultValue) {
		return map.getOrDefault(key, defaultValue);
	}

	public boolean notEmpty () {
		return map.notEmpty();
	}

	@Override
	public int size () {
		return map.size();
	}

	@Override
	public boolean isEmpty () {
		return map.isEmpty();
	}

	public int getDefaultValue () {
		return map.getDefaultValue();
	}

	public void setDefaultValue (int defaultValue) {
		map.setDefaultValue(defaultValue);
	}

	public void shrink (int maximumCapacity) {
		map.shrink(maximumCapacity);
	}

	@Override
	public boolean contains (Object key) {
		return map.containsKey(key);
	}

	/**
	 * Reduces the size of the set to the specified size. If the set is already smaller than the specified
	 * size, no action is taken.
	 *
	 * @param newSize the target size to try to reach by removing items, if smaller than the current size
	 */
	public void truncate (int newSize) {
		if (size() > newSize) {removeRange(newSize, size());}
	}

	@Override
	public NumberedSetIterator<T> iterator () {
		if (iterator1 == null || iterator2 == null) {
			iterator1 = new NumberedSetIterator<>(this);
			iterator2 = new NumberedSetIterator<>(this);
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

	public NumberedSetIterator<T> listIterator () {
		if (iterator1 == null || iterator2 == null) {
			iterator1 = new NumberedSetIterator<>(this);
			iterator2 = new NumberedSetIterator<>(this);
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

	public NumberedSetIterator<T> listIterator (int index) {
		if (iterator1 == null || iterator2 == null) {
			iterator1 = new NumberedSetIterator<>(this, index);
			iterator2 = new NumberedSetIterator<>(this, index);
		}
		if (!iterator1.valid) {
			iterator1.reset(index);
			iterator1.valid = true;
			iterator2.valid = false;
			return iterator1;
		}
		iterator2.reset(index);
		iterator2.valid = true;
		iterator1.valid = false;
		return iterator2;
	}

	/**
	 * Removes the items between the specified start index, inclusive, and end index, exclusive.
	 * Note that this takes different arguments than some other range-related methods; this needs
	 * a start index and an end index, rather than a count of items. This matches the behavior in
	 * the JDK collections.
	 *
	 * @param start the first index to remove, inclusive
	 * @param end   the last index (after what should be removed), exclusive
	 */
	@Override
	public void removeRange (int start, int end) {
		map.removeRange(start, end);
	}

	@Override
	public Object[] toArray () {
		return map.keySet().toArray();
	}

	@Override
	public <T1> T1[] toArray (T1[] a) {
		return map.keySet().toArray(a);
	}

	@Override
	public boolean add (T t) {
		final int s = size();
		map.putIfAbsent(t, s);
		return s != size();
	}

	/**
	 * If the given item {@code t} is present, this returns its index without modifying the NumberedSet; otherwise, it
	 * adds t to the end of the collection and returns the index for it there.
	 *
	 * @param t an item to get the index of, adding it if not present
	 * @return the index of {@code t} in this Arrangement
	 */
	public int addOrIndex (final T t) {
		return map.putIfAbsent(t, size());
	}

	/**
	 * Sets the key at the specified index. Returns true if the key was not already in the set. If this set already contains the
	 * key, the existing key's index is changed if needed and false is returned. Note, the order of the parameters matches the
	 * order in {@link ObjectList} and the rest of the JDK, not OrderedSet in libGDX.
	 *
	 * @param index where in the iteration order to add the given key, or to move it if already present
	 * @param key   what T item to try to add, if not already present
	 * @return true if the key was added for the first time, or false if the key was already present (even if moved)
	 */
	public boolean add (int index, T key) {
		int old = map.get(key);
		if (old != -1) {
			if (old != index) {
				map.remove(key);
				map.put(key, index, index);
				renumber(index);
			}
			return false;
		}
		map.put(key, index, index);
		renumber(index);
		return true;
	}

	public void resize (int newSize) {
		map.resize(newSize);
	}

	@Override
	public boolean equals (Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		NumberedSet<?> that = (NumberedSet<?>)o;

		return map.equals(that.map);
	}

	public float getLoadFactor () {
		return map.getLoadFactor();
	}

	public void setLoadFactor (float loadFactor) {
		map.setLoadFactor(loadFactor);
	}

	public T first () {
		if (size() == 0)
			throw new IllegalStateException("Cannot get the first() item of an empty NumberedSet.");
		return map.keyAt(0);
	}

	@Override
	public int hashCode () {
		return map.hashCode();
	}

	public String toString (String separator) {
		return map.toString(separator);
	}

	@Override
	public String toString () {
		return map.toString();
	}

	/**
	 * An {@link Iterator} and {@link ListIterator} over the elements of an ObjectList, while also an {@link Iterable}.
	 * @param <T> the generic type for the ObjectList this iterates over
	 */
	public static class NumberedSetIterator<T> implements Iterable<T>, ListIterator<T> {
		protected int index, latest = -1;
		protected NumberedSet<T> ns;
		protected boolean valid = true;

		public NumberedSetIterator (NumberedSet<T> ns) {
			this.ns = ns;
		}

		public NumberedSetIterator (NumberedSet<T> ns, int index) {
			if (index < 0 || index >= ns.size())
				throw new IndexOutOfBoundsException("NumberedSetIterator does not satisfy index >= 0 && index < list.size()");
			this.ns = ns;
			this.index = index;
		}

		/**
		 * Returns the next {@code int} element in the iteration.
		 *
		 * @return the next {@code int} element in the iteration
		 * @throws NoSuchElementException if the iteration has no more elements
		 */
		@Override
		@Nullable
		public T next () {
			if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
			if (index >= ns.size()) {throw new NoSuchElementException();}
			return ns.getAt(latest = index++);
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
			return index < ns.size();
		}

		/**
		 * Returns {@code true} if this list iterator has more elements when
		 * traversing the list in the reverse direction.  (In other words,
		 * returns {@code true} if {@link #previous} would return an element
		 * rather than throwing an exception.)
		 *
		 * @return {@code true} if the list iterator has more elements when
		 * traversing the list in the reverse direction
		 */
		@Override
		public boolean hasPrevious () {
			if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
			return index > 0 && ns.notEmpty();
		}

		/**
		 * Returns the previous element in the list and moves the cursor
		 * position backwards.  This method may be called repeatedly to
		 * iterate through the list backwards, or intermixed with calls to
		 * {@link #next} to go back and forth.  (Note that alternating calls
		 * to {@code next} and {@code previous} will return the same
		 * element repeatedly.)
		 *
		 * @return the previous element in the list
		 * @throws NoSuchElementException if the iteration has no previous
		 *                                element
		 */
		@Override
		@Nullable
		public T previous () {
			if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
			if (index <= 0 || ns.isEmpty()) {throw new NoSuchElementException();}
			return ns.getAt(latest = --index);
		}

		/**
		 * Returns the index of the element that would be returned by a
		 * subsequent call to {@link #next}. (Returns list size if the list
		 * iterator is at the end of the list.)
		 *
		 * @return the index of the element that would be returned by a
		 * subsequent call to {@code next}, or list size if the list
		 * iterator is at the end of the list
		 */
		@Override
		public int nextIndex () {
			return index;
		}

		/**
		 * Returns the index of the element that would be returned by a
		 * subsequent call to {@link #previous}. (Returns -1 if the list
		 * iterator is at the beginning of the list.)
		 *
		 * @return the index of the element that would be returned by a
		 * subsequent call to {@code previous}, or -1 if the list
		 * iterator is at the beginning of the list
		 */
		@Override
		public int previousIndex () {
			return index - 1;
		}

		/**
		 * Removes from the list the last element that was returned by {@link
		 * #next} or {@link #previous} (optional operation).  This call can
		 * only be made once per call to {@code next} or {@code previous}.
		 * It can be made only if {@link #add} has not been
		 * called after the last call to {@code next} or {@code previous}.
		 *
		 * @throws UnsupportedOperationException if the {@code remove}
		 *                                       operation is not supported by this list iterator
		 * @throws IllegalStateException         if neither {@code next} nor
		 *                                       {@code previous} have been called, or {@code remove} or
		 *                                       {@code add} have been called after the last call to
		 *                                       {@code next} or {@code previous}
		 */
		@Override
		public void remove () {
			if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
			if (latest == -1 || latest >= ns.size()) {throw new NoSuchElementException();}
			ns.removeAt(latest);
			index = latest;
			latest = -1;
		}

		/**
		 * Replaces the last element returned by {@link #next} or
		 * {@link #previous} with the specified element (optional operation).
		 * This call can be made only if neither {@link #remove} nor {@link
		 * #add} have been called after the last call to {@code next} or
		 * {@code previous}.
		 *
		 * @param t the element with which to replace the last element returned by
		 *          {@code next} or {@code previous}
		 * @throws UnsupportedOperationException if the {@code set} operation
		 *                                       is not supported by this list iterator
		 * @throws ClassCastException            if the class of the specified element
		 *                                       prevents it from being added to this list
		 * @throws IllegalArgumentException      if some aspect of the specified
		 *                                       element prevents it from being added to this list
		 * @throws IllegalStateException         if neither {@code next} nor
		 *                                       {@code previous} have been called, or {@code remove} or
		 *                                       {@code add} have been called after the last call to
		 *                                       {@code next} or {@code previous}
		 */
		@Override
		public void set (T t) {
			if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
			if (latest == -1 || latest >= ns.size()) {throw new NoSuchElementException();}
			ns.alterAt(latest, t);
		}

		/**
		 * Inserts the specified element into the list (optional operation).
		 * The element is inserted immediately before the element that
		 * would be returned by {@link #next}, if any, and after the element
		 * that would be returned by {@link #previous}, if any.  (If the
		 * list contains no elements, the new element becomes the sole element
		 * on the list.)  The new element is inserted before the implicit
		 * cursor: a subsequent call to {@code next} would be unaffected, and a
		 * subsequent call to {@code previous} would return the new element.
		 * (This call increases by one the value that would be returned by a
		 * call to {@code nextIndex} or {@code previousIndex}.)
		 *
		 * @param t the element to insert
		 * @throws UnsupportedOperationException if the {@code add} method is
		 *                                       not supported by this list iterator
		 * @throws ClassCastException            if the class of the specified element
		 *                                       prevents it from being added to this list
		 * @throws IllegalArgumentException      if some aspect of this element
		 *                                       prevents it from being added to this list
		 */
		@Override
		public void add (@Nullable T t) {
			if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
			if (index > ns.size()) {throw new NoSuchElementException();}
			ns.add(index++, t);
			latest = -1;
		}

		public void reset () {
			index = 0;
			latest = -1;
		}

		public void reset (int index) {
			if (index < 0 || index >= ns.size())
				throw new IndexOutOfBoundsException("NumberedSetIterator does not satisfy index >= 0 && index < list.size()");
			this.index = index;
			latest = -1;
		}

		/**
		 * Returns an iterator over elements of type {@code T}.
		 *
		 * @return a ListIterator; really this same NumberedSetIterator.
		 */
		@Override
		public NumberedSetIterator<T> iterator () {
			return this;
		}
	}
	
	public static <T> NumberedSet<T> with (T item) {
		NumberedSet<T> set = new NumberedSet<>(1);
		set.add(item);
		return set;
	}

	@SafeVarargs
	public static <T> NumberedSet<T> with (T... array) {
		return new NumberedSet<>(array);
	}

}

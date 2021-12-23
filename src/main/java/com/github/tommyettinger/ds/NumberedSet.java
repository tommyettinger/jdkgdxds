/*******************************************************************************
 * Copyright 2021 See AUTHORS file.
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
import java.util.Collection;
import java.util.Iterator;
import java.util.ListIterator;
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

	protected class InternalMap extends ObjectIntOrderedMap<T>{
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
	 * Returns an index &gt;= 0 and &lt;= the {@code mask} of this NumberedSet's {@link #map} for the specified {@code item}.
	 * <br>
	 * You can override this, which will affect the internal map that NumberedSet uses.
	 * <br>
	 * The default implementation assumes the low-order bits of item.hashCode() are likely enough to avoid collisions,
	 * and so just returns {@code item.hashCode() & mask}. This method can be overridden to customizing hashing. If you
	 * aren't confident that the hashCode() implementation used by item will have reasonable quality, you can override
	 * this with something such as {@code return (int)(item.hashCode() * 0x9E3779B97F4A7C15L >>> shift);}. That "magic
	 * number" is 2 to the 64, divided by the golden ratio; the golden ratio is used because of various properties it
	 * has that make it better at randomizing bits. You should usually override this method if you also override
	 * {@link #equate(Object, Object)}, because two equal values should have the same hash.
	 * @param item any non-null Object
	 * @return an index between 0 and the {@code mask} of this NumberedSet's {@link #map} (both inclusive)
	 */
	protected int place (Object item) {
		return item.hashCode() & map.mask;
	}

	/**
	 * Compares the objects left and right, which are usually keys, for equality, returning true if they are considered
	 * equal. This is used by the rest of this class to determine whether two keys are considered equal. Normally, this
	 * returns {@code left.equals(right)}, but subclasses can override it to use reference equality, fuzzy equality, deep
	 * array equality, or any other custom definition of equality. Usually, {@link #place(Object)} is also overridden if
	 * this method is.
	 * <br>
	 * You can override this, which will affect the internal map that NumberedSet uses.
	 * @param left must be non-null; typically a key being compared, but not necessarily
	 * @param right may be null; typically a key being compared, but can often be null for an empty key slot, or some other type
	 * @return true if left and right are considered equal for the purposes of this class
	 */
	protected boolean equate(Object left, @Nullable Object right){
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
	 * Adds items from the iteration order of {@code other}, from start (inclusive) to end (exclusive).
	 * @param other any Ordered with the same T type as this
	 * @param start inclusive start index in the order of other
	 * @param end exclusive end index in the order of other
	 * @return true if this was modified
	 */
	public boolean addAll (Ordered<T> other, int start, int end) {
		start = Math.max(0, start);
		end = Math.min(other.size(), end);
		ensureCapacity(end - start);
		int oldSize = map.size;
		for (int i = start; i < end; i++) { add(other.order().get(i)); }
		return map.size != oldSize;
	}

	public boolean addAll (T[] array) {
		return addAll(array, 0, array.length);
	}

	public boolean addAll (T[] array, int offset, int length) {
		ensureCapacity(length);
		int oldSize = size();
		for (int i = offset, n = i + length; i < n; i++) { add(array[i]); }
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

	@Override
	public Iterator<T> iterator () {
		return map.order().iterator();
	}

	public ListIterator<T> listIterator () {
		return map.order().listIterator();
	}

	public ListIterator<T> listIterator (int index) {
		return map.order().listIterator(index);
	}


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
	 * Sets the key at the specified index. Returns true if the key was not already in the set. If this set already contains the
	 * key, the existing key's index is changed if needed and false is returned. Note, the order of the parameters matches the
	 * order in {@link ObjectList} and the rest of the JDK, not OrderedSet in libGDX.
	 *
	 * @param index where in the iteration order to add the given key, or to move it if already present
	 * @param key what T item to try to add, if not already present
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

	public T first() {
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

	public static <T> NumberedSet<T> with(T item) {
		NumberedSet<T> set = new NumberedSet<>(1);
		set.add(item);
		return set;
	}

	@SafeVarargs
	public static <T> NumberedSet<T> with (T... array) {
		return new NumberedSet<>(array);
	}

}

package com.github.tommyettinger.ds;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
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
public class NumberedSet<T> implements Set<T>, Ordered<T>, Serializable {
	private static final long serialVersionUID = 0L;

	protected ObjectIntOrderedMap<T> map;

	public NumberedSet () {
		map = new ObjectIntOrderedMap<>();
		map.setDefaultValue(-1);
	}

	public NumberedSet (int initialCapacity, float loadFactor) {
		map = new ObjectIntOrderedMap<>(initialCapacity, loadFactor);
		map.setDefaultValue(-1);
	}

	public NumberedSet (int initialCapacity) {
		map = new ObjectIntOrderedMap<>(initialCapacity);
		map.setDefaultValue(-1);
	}

	public NumberedSet (NumberedSet<? extends T> other) {
		map = new ObjectIntOrderedMap<>(other.map);
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

	public boolean removeAt (int index) {
		if (index < 0 || index >= size())
			return false;
		map.removeAt(index);
		renumber(index);
		return true;
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
		return map.keySet().iterator();
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


	@SafeVarargs
	public static <T> NumberedSet<T> with (T... array) {
		return new NumberedSet<>(array);
	}

}

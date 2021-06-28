/*
 * Copyright (C) 2002-2021 Sebastiano Vigna
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.tommyettinger.ds;

import javax.annotation.Nonnull;
import java.lang.reflect.Array;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.RandomAccess;
import java.util.function.Consumer;
import java.util.stream.Collector;

public class ObjectList2<K> extends AbstractCollection<K> implements List<K>, RandomAccess {
	protected static class ObjectArrays {
		/** This is a safe value used by {@link ArrayList} (as of Java 7) to avoid
		 *  throwing {@link OutOfMemoryError} on some JVMs. We adopt the same value. */
		public static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
		/** A static, final, empty array. */
		public static final Object[] EMPTY_ARRAY = {};
		/** A static, final, empty array to be used as default array in allocations. An
		 * object distinct from {@link #EMPTY_ARRAY} makes it possible to have different
		 * behaviors depending on whether the user required an empty allocation, or we are
		 * just lazily delaying allocation.
		 *
		 * @see java.util.ArrayList
		 */
		public static final Object[] DEFAULT_EMPTY_ARRAY = {};
		/** Creates a new array using a the given one as prototype.
		 *
		 * <p>This method returns a new array of the given length whose element
		 * are of the same class as of those of {@code prototype}. In case
		 * of an empty array, it tries to return {@link #EMPTY_ARRAY}, if possible.
		 *
		 * @param prototype an array that will be used to type the new one.
		 * @param length the length of the new array.
		 * @return a new array of given type and length.
		 */
		@SuppressWarnings("unchecked")
		private static <K> K[] newArray(final K[] prototype, final int length) {
			final Class<?> klass = prototype.getClass();
			if (klass == Object[].class) return (K[])(length == 0 ? EMPTY_ARRAY : new Object[length]);
			return (K[])java.lang.reflect.Array.newInstance(klass.getComponentType(), length);
		}
		/** Forces an array to contain the given number of entries, preserving just a part of the array.
		 *
		 * @param array an array.
		 * @param length the new minimum length for this array.
		 * @param preserve the number of elements of the array that must be preserved in case a new allocation is necessary.
		 * @return an array with {@code length} entries whose first {@code preserve}
		 * entries are the same as those of {@code array}.
		 */
		public static <K> K[] forceCapacity(final K[] array, final int length, final int preserve) {
			final K[] t =
				newArray(array, length);
			System.arraycopy(array, 0, t, 0, preserve);
			return t;
		}
		/** Ensures that an array can contain the given number of entries.
		 *
		 * <p>If you cannot foresee whether this array will need again to be
		 * enlarged, you should probably use {@code grow()} instead.
		 *
		 * @param array an array.
		 * @param length the new minimum length for this array.
		 * @return {@code array}, if it contains {@code length} entries or more; otherwise,
		 * an array with {@code length} entries whose first {@code array.length}
		 * entries are the same as those of {@code array}.
		 */
		public static <K> K[] ensureCapacity(final K[] array, final int length) {
			return ensureCapacity(array, length, array.length);
		}
		/** Ensures that an array can contain the given number of entries, preserving just a part of the array.
		 *
		 * @param array an array.
		 * @param length the new minimum length for this array.
		 * @param preserve the number of elements of the array that must be preserved in case a new allocation is necessary.
		 * @return {@code array}, if it can contain {@code length} entries or more; otherwise,
		 * an array with {@code length} entries whose first {@code preserve}
		 * entries are the same as those of {@code array}.
		 */
		public static <K> K[] ensureCapacity(final K[] array, final int length, final int preserve) {
			return length > array.length ? forceCapacity(array, length, preserve) : array;
		}
		/** Grows the given array to the maximum between the given length and
		 * the current length increased by 50%, provided that the given
		 * length is larger than the current length.
		 *
		 * <p>If you want complete control on the array growth, you
		 * should probably use {@code ensureCapacity()} instead.
		 *
		 * @param array an array.
		 * @param length the new minimum length for this array.
		 * @return {@code array}, if it can contain {@code length}
		 * entries; otherwise, an array with
		 * max({@code length},{@code array.length}/&phi;) entries whose first
		 * {@code array.length} entries are the same as those of {@code array}.
		 * */
		public static <K> K[] grow(final K[] array, final int length) {
			return grow(array, length, array.length);
		}
		/** Grows the given array to the maximum between the given length and
		 * the current length increased by 50%, provided that the given
		 * length is larger than the current length, preserving just a part of the array.
		 *
		 * <p>If you want complete control on the array growth, you
		 * should probably use {@code ensureCapacity()} instead.
		 *
		 * @param array an array.
		 * @param length the new minimum length for this array.
		 * @param preserve the number of elements of the array that must be preserved in case a new allocation is necessary.
		 * @return {@code array}, if it can contain {@code length}
		 * entries; otherwise, an array with
		 * max({@code length},{@code array.length}/&phi;) entries whose first
		 * {@code preserve} entries are the same as those of {@code array}.
		 * */
		public static <K> K[] grow(final K[] array, final int length, final int preserve) {
			if (length > array.length) {
				final int newLength = (int)Math.max(Math.min((long)array.length + (array.length >> 1), MAX_ARRAY_SIZE), length);
				final K[] t =
					newArray(array, newLength);
				System.arraycopy(array, 0, t, 0, preserve);
				return t;
			}
			return array;
		}
		/** Trims the given array to the given length.
		 *
		 * @param array an array.
		 * @param length the new maximum length for the array.
		 * @return {@code array}, if it contains {@code length}
		 * entries or less; otherwise, an array with
		 * {@code length} entries whose entries are the same as
		 * the first {@code length} entries of {@code array}.
		 *
		 */
		public static <K> K[] trim(final K[] array, final int length) {
			if (length >= array.length) return array;
			final K[] t =
				newArray(array, length);
			System.arraycopy(array, 0, t, 0, length);
			return t;
		}
		/** Sets the length of the given array.
		 *
		 * @param array an array.
		 * @param length the new length for the array.
		 * @return {@code array}, if it contains exactly {@code length}
		 * entries; otherwise, if it contains <em>more</em> than
		 * {@code length} entries, an array with {@code length} entries
		 * whose entries are the same as the first {@code length} entries of
		 * {@code array}; otherwise, an array with {@code length} entries
		 * whose first {@code array.length} entries are the same as those of
		 * {@code array}.
		 *
		 */
		public static <K> K[] setLength(final K[] array, final int length) {
			if (length == array.length) return array;
			if (length < array.length) return trim(array, length);
			return ensureCapacity(array, length);
		}
		/** Returns a copy of a portion of an array.
		 *
		 * @param array an array.
		 * @param offset the first element to copy.
		 * @param length the number of elements to copy.
		 * @return a new array containing {@code length} elements of {@code array} starting at {@code offset}.
		 */
		public static <K> K[] copy(final K[] array, final int offset, final int length) {
			ensureOffsetLength(array, offset, length);
			final K[] a =
				newArray(array, length);
			System.arraycopy(array, offset, a, 0, length);
			return a;
		}
		/** Returns a copy of an array.
		 *
		 * @param array an array.
		 * @return a copy of {@code array}.
		 */
		public static <K> K[] copy(final K[] array) {
			return array.clone();
		}
		/** Fills the given array with the given value.
		 *
		 * @param array an array.
		 * @param value the new value for all elements of the array.
		 * @deprecated Please use the corresponding {@link java.util.Arrays} method.
		 */
		@Deprecated
		public static <K> void fill(final K[] array, final K value) {
			int i = array.length;
			while(i-- != 0) array[i] = value;
		}
		/** Fills a portion of the given array with the given value.
		 *
		 * @param array an array.
		 * @param from the starting index of the portion to fill (inclusive).
		 * @param to the end index of the portion to fill (exclusive).
		 * @param value the new value for all elements of the specified portion of the array.
		 * @deprecated Please use the corresponding {@link java.util.Arrays} method.
		 */
		@Deprecated
		public static <K> void fill(final K[] array, final int from, int to, final K value) {
			ensureFromTo(array, from, to);
			if (from == 0) while(to-- != 0) array[to] = value;
			else for(int i = from; i < to; i++) array[i] = value;
		}
		/** Returns true if the two arrays are elementwise equal.
		 *
		 * @param a1 an array.
		 * @param a2 another array.
		 * @return true if the two arrays are of the same length, and their elements are equal.
		 * @deprecated Please use the corresponding {@link java.util.Arrays} method, which is intrinsified in recent JVMs.
		 */
		@Deprecated
		public static <K> boolean equals(final K[] a1, final K[] a2) {
			int i = a1.length;
			if (i != a2.length) return false;
			while(i-- != 0) if (! java.util.Objects.equals(a1[i], a2[i])) return false;
			return true;
		}
		/** Ensures that a range given by its first (inclusive) and last (exclusive) elements fits an array.
		 *
		 * <p>This method may be used whenever an array range check is needed.
		 *
		 * <p>In Java 9 and up, this method should be considered deprecated in favor of the
		 * {@link java.util.Objects#checkFromToIndex(int, int, int)} method, which may be intrinsified in recent JVMs.
		 *
		 * @param a an array.
		 * @param from a start index (inclusive).
		 * @param to an end index (exclusive).
		 * @throws IllegalArgumentException if {@code from} is greater than {@code to}.
		 * @throws ArrayIndexOutOfBoundsException if {@code from} or {@code to} are greater than the array length or negative.
		 */
		public static <K> void ensureFromTo(final K[] a, final int from, final int to) {
			if (from < 0) throw new ArrayIndexOutOfBoundsException("Start index (" + from + ") is negative");
			if (from > to) throw new IllegalArgumentException("Start index (" + from + ") is greater than end index (" + to + ")");
			if (to > a.length) throw new ArrayIndexOutOfBoundsException("End index (" + to + ") is greater than array length (" + a.length + ")");

		}
		/** Ensures that a range given by an offset and a length fits an array.
		 *
		 * <p>This method may be used whenever an array range check is needed.
		 *
		 * <p>In Java 9 and up, this method should be considered deprecated in favor of the
		 * {@link java.util.Objects#checkFromIndexSize(int, int, int)} method, which may be intrinsified in recent JVMs.
		 *
		 * @param a an array.
		 * @param offset a start index.
		 * @param length a length (the number of elements in the range).
		 * @throws IllegalArgumentException if {@code length} is negative.
		 * @throws ArrayIndexOutOfBoundsException if {@code offset} is negative or {@code offset}+{@code length} is greater than the array length.
		 */
		public static <K> void ensureOffsetLength(final K[] a, final int offset, final int length) {
			if (offset < 0) throw new ArrayIndexOutOfBoundsException("Offset (" + offset + ") is negative");
			if (length < 0) throw new IllegalArgumentException("Length (" + length + ") is negative");
			if (offset + length > a.length) throw new ArrayIndexOutOfBoundsException("Last index (" + (offset + length) + ") is greater than array length (" + a.length + ")");

		}
		/** Ensures that two arrays are of the same length.
		 *
		 * @param a an array.
		 * @param b another array.
		 * @throws IllegalArgumentException if the two argument arrays are not of the same length.
		 */
		public static <K> void ensureSameLength(final K[] a, final K[] b) {
			if (a.length != b.length) throw new IllegalArgumentException("Array size mismatch: " + a.length + " != " + b.length);
		}

		/**
		 * Ensures that a range given by its first (inclusive) and last (exclusive) elements fits an
		 * array of given length.
		 *
		 * <p>
		 * This method may be used whenever an array range check is needed.
		 *
		 * <p>
		 * In Java 9 and up, this method should be considered deprecated in favor of the
		 * {@link java.util.Objects#checkFromToIndex(int, int, int)} method, which may be intrinsified
		 * in recent JVMs.
		 *
		 * @param arrayLength an array length.
		 * @param from a start index (inclusive).
		 * @param to an end index (inclusive).
		 * @throws IllegalArgumentException if {@code from} is greater than {@code to}.
		 * @throws ArrayIndexOutOfBoundsException if {@code from} or {@code to} are greater than
		 *             {@code arrayLength} or negative.
		 */
		public static void ensureFromTo(final int arrayLength, final int from, final int to) {
			// When Java 9 becomes the minimum, use Objects#checkFromToIndex​​, as that can be an intrinsic
			if (from < 0) throw new ArrayIndexOutOfBoundsException("Start index (" + from + ") is negative");
			if (from > to) throw new IllegalArgumentException("Start index (" + from + ") is greater than end index (" + to + ")");
			if (to > arrayLength) throw new ArrayIndexOutOfBoundsException("End index (" + to + ") is greater than array length (" + arrayLength + ")");
		}

		/**
		 * Ensures that a range given by an offset and a length fits an array of given length.
		 *
		 * <p>
		 * This method may be used whenever an array range check is needed.
		 *
		 * <p>
		 * In Java 9 and up, this method should be considered deprecated in favor of the
		 * {@link java.util.Objects#checkFromIndexSize(int, int, int)} method, which may be intrinsified
		 * in recent JVMs.
		 *
		 * @param arrayLength an array length.
		 * @param offset a start index for the fragment
		 * @param length a length (the number of elements in the fragment).
		 * @throws IllegalArgumentException if {@code length} is negative.
		 * @throws ArrayIndexOutOfBoundsException if {@code offset} is negative or
		 *             {@code offset}+{@code length} is greater than {@code arrayLength}.
		 */
		public static void ensureOffsetLength(final int arrayLength, final int offset, final int length) {
			// When Java 9 becomes the minimum, use Objects#checkFromIndexSize​, as that can be an intrinsic
			if (offset < 0) throw new ArrayIndexOutOfBoundsException("Offset (" + offset + ") is negative");
			if (length < 0) throw new IllegalArgumentException("Length (" + length + ") is negative");
			if (offset + length > arrayLength) throw new ArrayIndexOutOfBoundsException("Last index (" + (offset + length) + ") is greater than array length (" + arrayLength + ")");
		}

	}

	/**
	 * The initial default capacity of an array list.
	 */
	public static final int DEFAULT_INITIAL_CAPACITY = 10;
	/**
	 * Whether the backing array was passed to {@code wrap()}. In
	 * this case, we must reallocate with the same type of array.
	 */
	protected final boolean wrapped;
	/**
	 * The backing array.
	 */
	protected transient K[] a;
	/**
	 * The current actual size of the list (never greater than the backing-array length).
	 */
	protected int size;

	/**
	 * Ensures that the component type of the given array is the proper type.
	 */
	@SuppressWarnings("unchecked")
	private static final <K> K[] copyArraySafe(K[] a, int length) {
		if (length == 0) return (K[]) ObjectArrays.EMPTY_ARRAY;
		return (K[]) java.util.Arrays.copyOf(a, length, Object[].class);
	}

	private static final <K> K[] copyArrayFromSafe(ObjectList2<K> l) {
		return copyArraySafe(l.a, l.size);
	}

	/**
	 * Creates a new array list using a given array.
	 *
	 * <p>This constructor is only meant to be used by the wrapping methods.
	 *
	 * @param a the array that will be used to back this array list.
	 */
	protected ObjectList2(final K[] a, @SuppressWarnings("unused") boolean wrapped) {
		this.a = a;
		this.wrapped = wrapped;
	}

	@SuppressWarnings("unchecked")
	private void initArrayFromCapacity(final int capacity) {
		if (capacity < 0) throw new IllegalArgumentException("Initial capacity (" + capacity + ") is negative");
		if (capacity == 0) a = (K[]) ObjectArrays.EMPTY_ARRAY;
		else a = (K[]) new Object[capacity];
	}

	/**
	 * Creates a new array list with given capacity.
	 *
	 * @param capacity the initial capacity of the array list (may be 0).
	 */
	public ObjectList2(final int capacity) {
		initArrayFromCapacity(capacity);
		this.wrapped = false;
	}

	/**
	 * Creates a new array list with {@link #DEFAULT_INITIAL_CAPACITY} capacity.
	 */
	@SuppressWarnings("unchecked")
	public ObjectList2() {
		a = (K[]) ObjectArrays.DEFAULT_EMPTY_ARRAY; // We delay allocation
		wrapped = false;
	}

	private static <K> int unwrap(final Iterator <? extends K> i, final K[] array, int offset, final int max) {
		if (max < 0) throw new IllegalArgumentException("The maximum number of elements (" + max + ") is negative");
		if (offset < 0 || offset + max > array.length) throw new IllegalArgumentException();
		int j = max;
		while(j-- != 0 && i.hasNext()) array[offset++] = i.next();
		return max - j - 1;
	}

	/**
	 * Creates a new array list and fills it with a given collection.
	 *
	 * @param c a collection that will be used to fill the array list.
	 */
	public ObjectList2(final Collection<? extends K> c) {
		if (c instanceof ObjectList2) {
			a = copyArrayFromSafe((ObjectList2<? extends K>) c);
			size = a.length;
		} else {
			initArrayFromCapacity(c.size());
			size = unwrap(c.iterator(), a, 0, a.length);
		}
		this.wrapped = false;
	}

	/**
	 * Creates a new array list and fills it with a given type-specific list.
	 *
	 * @param l a type-specific list that will be used to fill the array list.
	 */
	public ObjectList2(final ObjectList2<? extends K> l) {
		a = copyArrayFromSafe((ObjectList2<? extends K>)l);
		size = a.length;
		this.wrapped = false;
	}

	/**
	 * Creates a new array list and fills it with the elements of a given array.
	 *
	 * @param a an array whose elements will be used to fill the array list.
	 */
	public ObjectList2(final K[] a) {
		this(a, 0, a.length);
	}

	/**
	 * Creates a new array list and fills it with the elements of a given array.
	 *
	 * @param a      an array whose elements will be used to fill the array list.
	 * @param offset the first element to use.
	 * @param length the number of elements to use.
	 */
	public ObjectList2(final K[] a, final int offset, final int length) {
		this(length);
		System.arraycopy(a, offset, this.a, 0, length);
		size = length;
	}

	/**
	 * Creates a new array list and fills it with the elements returned by an iterator.
	 *
	 * @param i an iterator whose returned elements will fill the array list.
	 */
	public ObjectList2(final Iterator<? extends K> i) {
		this();
		while (i.hasNext()) this.add((i.next()));
	}

	/**
	 * Creates a new empty array list.
	 *
	 * @return a new empty array list.
	 */
	public static <K> ObjectList2<K> of() {
		return new ObjectList2<>();
	}

	/**
	 * Creates an array list using an array of elements.
	 *
	 * @param init a the array the will become the new backing array of the array list.
	 * @return a new array list backed by the given array.
	 * @see #ObjectList2(Object[])
	 */
	@SafeVarargs
	public static <K> ObjectList2<K> of(final K... init) {
		return new ObjectList2<>(init);
	}
	/**
	 * Ensures that the given index is nonnegative and not greater than the list size.
	 *
	 * @param index an index.
	 * @throws IndexOutOfBoundsException if the given index is negative or greater than the list size.
	 */
	protected void ensureIndex(final int index) {
		if (index < 0) throw new IndexOutOfBoundsException("Index (" + index + ") is negative");
		if (index > size())
			throw new IndexOutOfBoundsException("Index (" + index + ") is greater than list size (" + (size()) + ")");
	}

	/**
	 * Ensures that the given index is nonnegative and smaller than the list size.
	 *
	 * @param index an index.
	 * @throws IndexOutOfBoundsException if the given index is negative or not smaller than the list size.
	 */
	protected void ensureRestrictedIndex(final int index) {
		if (index < 0) throw new IndexOutOfBoundsException("Index (" + index + ") is negative");
		if (index >= size())
			throw new IndexOutOfBoundsException("Index (" + index + ") is greater than or equal to list size (" + (size()) + ")");
	}

	// Collector wants a function that returns the collection being added to.
	ObjectList2<K> combine(ObjectList2<? extends K> toAddFrom) {
		addAll(toAddFrom);
		return this;
	}

	private static final Collector<Object, ?, ObjectList2<Object>> TO_LIST_COLLECTOR =
		Collector.of(
			ObjectList2::new,
			ObjectList2::add,
			ObjectList2::combine);

	/**
	 * Returns a {@link Collector} that collects a {@code Stream}'s elements into a new ArrayList.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static <K> Collector<K, ?, ObjectList2<K>> toList() {
		return (Collector) TO_LIST_COLLECTOR;
	}

	/**
	 * Ensures that this array list can contain the given number of entries without resizing.
	 *
	 * @param capacity the new minimum capacity for this array list.
	 */
	@SuppressWarnings("unchecked")
	public void ensureCapacity(final int capacity) {
		if (capacity <= a.length || (a == ObjectArrays.DEFAULT_EMPTY_ARRAY && capacity <= DEFAULT_INITIAL_CAPACITY))
			return;
		if (wrapped) a = ObjectArrays.ensureCapacity(a, capacity, size);
		else {
			if (capacity > a.length) {
				final Object[] t = new Object[capacity];
				System.arraycopy(a, 0, t, 0, size);
				a = (K[]) t;
			}
		}
		assert size <= a.length;
	}

	/**
	 * Grows this array list, ensuring that it can contain the given number of entries without resizing,
	 * and in case increasing the current capacity at least by a factor of 50%.
	 *
	 * @param capacity the new minimum capacity for this array list.
	 */
	@SuppressWarnings("unchecked")
	private void grow(int capacity) {
		if (capacity <= a.length) return;
		if (a != ObjectArrays.DEFAULT_EMPTY_ARRAY)
			capacity = (int) Math.max(Math.min((long) a.length + (a.length >> 1), ObjectArrays.MAX_ARRAY_SIZE), capacity);
		else if (capacity < DEFAULT_INITIAL_CAPACITY) capacity = DEFAULT_INITIAL_CAPACITY;
		if (wrapped) a = ObjectArrays.forceCapacity(a, capacity, size);
		else {
			final Object[] t = new Object[capacity];
			System.arraycopy(a, 0, t, 0, size);
			a = (K[]) t;
		}
		assert size <= a.length;
	}

	@Override
	public void add(final int index, final K k) {
		ensureIndex(index);
		grow(size + 1);
		if (index != size) System.arraycopy(a, index, a, index + 1, size - index);
		a[index] = k;
		size++;
		assert size <= a.length;
	}

	@Override
	public boolean add(final K k) {
		grow(size + 1);
		a[size++] = k;
		assert size <= a.length;
		return true;
	}

	@Override
	public K get(final int index) {
		if (index >= size)
			throw new IndexOutOfBoundsException("Index (" + index + ") is greater than or equal to list size (" + size + ")");
		return a[index];
	}

	@Override
	public int indexOf(final Object k) {
		for (int i = 0; i < size; i++) if (java.util.Objects.equals(k, a[i])) return i;
		return -1;
	}

	@Override
	public int lastIndexOf(final Object k) {
		for (int i = size; i-- != 0; ) if (java.util.Objects.equals(k, a[i])) return i;
		return -1;
	}

	@Override
	public K remove(final int index) {
		if (index >= size)
			throw new IndexOutOfBoundsException("Index (" + index + ") is greater than or equal to list size (" + size + ")");
		final K old = a[index];
		size--;
		if (index != size) System.arraycopy(a, index + 1, a, index, size - index);
		a[size] = null;
		assert size <= a.length;
		return old;
	}

	@Override
	public boolean remove(final Object k) {
		int index = indexOf(k);
		if (index == -1) return false;
		remove(index);
		assert size <= a.length;
		return true;
	}

	@Override
	public K set(final int index, final K k) {
		if (index >= size)
			throw new IndexOutOfBoundsException("Index (" + index + ") is greater than or equal to list size (" + size + ")");
		K old = a[index];
		a[index] = k;
		return old;
	}

	@Override
	public void clear() {
		Arrays.fill(a, 0, size, null);
		size = 0;
		assert size <= a.length;
	}

	@Override
	public int size() {
		return size;
	}

	public void setSize (final int size) {
		if (size > a.length) a = ObjectArrays.forceCapacity(a, size, this.size);
		if (size > this.size) Arrays.fill(a, this.size, size, (null));
		else Arrays.fill(a, size, this.size, (null));
		this.size = size;
	}

	@Override
	public boolean isEmpty() {
		return size == 0;
	}

	/**
	 * Trims this array list so that the capacity is equal to the size.
	 *
	 * @see java.util.ArrayList#trimToSize()
	 */
	public void trim() {
		trim(0);
	}

	/**
	 * Trims the backing array if it is too large.
	 * <p>
	 * If the current array length is smaller than or equal to
	 * {@code n}, this method does nothing. Otherwise, it trims the
	 * array length to the maximum between {@code n} and {@link #size()}.
	 *
	 * <p>This method is useful when reusing lists.  {@linkplain #clear() Clearing a
	 * list} leaves the array length untouched. If you are reusing a list
	 * many times, you can call this method with a typical
	 * size to avoid keeping around a very large array just
	 * because of a few large transient lists.
	 *
	 * @param n the threshold for the trimming.
	 */
	@SuppressWarnings("unchecked")
	public void trim(final int n) {
		// TODO: use Arrays.trim() and preserve type only if necessary
		if (n >= a.length || size == a.length) return;
		final K[] t = (K[]) new Object[Math.max(n, size)];
		System.arraycopy(a, 0, t, 0, size);
		a = t;
		assert size <= a.length;
	}

	@Override
	public ObjectList<K> subList(int from, int to) {
		throw new UnsupportedOperationException("subList() is not yet implemented.");
	}

	/**
	 * Copies element of this type-specific list into the given array using optimized system calls.
	 *
	 * @param from   the start index (inclusive).
	 * @param a      the destination array.
	 * @param offset the offset into the destination array where to store the first element copied.
	 * @param length the number of elements to be copied.
	 */
	public void getElements(final int from, final Object[] a, final int offset, final int length) {
		ObjectArrays.ensureOffsetLength(a, offset, length);
		System.arraycopy(this.a, from, a, offset, length);
	}

	/**
	 * Removes elements of this type-specific list using optimized system calls.
	 *
	 * @param from the start index (inclusive).
	 * @param to   the end index (exclusive).
	 */
	public void removeElements(final int from, final int to) {
		ObjectArrays.ensureFromTo(size, from, to);
		System.arraycopy(a, to, a, from, size - to);
		size -= (to - from);
		int i = to - from;
		while (i-- != 0) a[size + i] = null;
	}

	/**
	 * Adds elements to this type-specific list using optimized system calls.
	 *
	 * @param index  the index at which to add elements.
	 * @param a      the array containing the elements.
	 * @param offset the offset of the first element to add.
	 * @param length the number of elements to add.
	 */
	public void addElements(final int index, final K[] a, final int offset, final int length) {
		ensureIndex(index);
		ObjectArrays.ensureOffsetLength(a, offset, length);
		grow(size + length);
		System.arraycopy(this.a, index, this.a, index + length, size - index);
		System.arraycopy(a, offset, this.a, index, length);
		size += length;
	}

	/**
	 * Sets elements to this type-specific list using optimized system calls.
	 *
	 * @param index  the index at which to start setting elements.
	 * @param a      the array containing the elements.
	 * @param offset the offset of the first element to add.
	 * @param length the number of elements to add.
	 */
	public void setElements(final int index, final K[] a, final int offset, final int length) {
		ensureIndex(index);
		ObjectArrays.ensureOffsetLength(a, offset, length);
		if (index + length > size)
			throw new IndexOutOfBoundsException("End index (" + (index + length) + ") is greater than list size (" + size + ")");
		System.arraycopy(a, offset, this.a, index, length);
	}

	@Override
	public void forEach(final Consumer<? super K> action) {
		for (int i = 0; i < size; ++i) {
			action.accept(a[i]);
		}
	}

	@Override
	public boolean addAll(int index, final Collection<? extends K> c) {
		if (c instanceof ObjectList2) {
			return addAll(index, (ObjectList2<? extends K>) c);
		}
		ensureIndex(index);
		int n = c.size();
		if (n == 0) return false;
		grow(size + n);
		System.arraycopy(a, index, a, index + n, size - index);
		final Iterator<? extends K> i = c.iterator();
		size += n;
		while (n-- != 0) a[index++] = i.next();
		assert size <= a.length;
		return true;
	}

	public boolean addAll(final int index, final ObjectList2<? extends K> l) {
		ensureIndex(index);
		final int n = l.size();
		if (n == 0) return false;
		grow(size + n);
		System.arraycopy(a, index, a, index + n, size - index);
		l.getElements(0, a, index, n);
		size += n;
		assert size <= a.length;
		return true;
	}

	@Override
	public boolean removeAll(final Collection<?> c) {
		final Object[] a = this.a;
		int j = 0;
		for (int i = 0; i < size; i++)
			if (!c.contains(a[i])) a[j++] = a[i];
		Arrays.fill(a, j, size, null);
		final boolean modified = size != j;
		size = j;
		return modified;
	}

	@Override
	public Object[] toArray() {
		// A subtle part of the spec says the returned array must be Object[] exactly.
		return Arrays.copyOf(a, size(), Object[].class);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <K> K[] toArray(K[] a) {
		if (a == null) {
			a = (K[]) new Object[size()];
		} else if (a.length < size()) {
			a = (K[]) Array.newInstance(a.getClass().getComponentType(), size());
		}
		System.arraycopy(this.a, 0, a, 0, size());
		if (a.length > size()) {
			a[size()] = null;
		}
		return a;
	}

	@Override
	public ListIterator<K> listIterator(final int index) {
		ensureIndex(index);
		return new ListIterator<K>() {
			int pos = index, last = -1;

			@Override
			public boolean hasNext() {
				return pos < size;
			}

			@Override
			public boolean hasPrevious() {
				return pos > 0;
			}

			@Override
			public K next() {
				if (!hasNext()) throw new NoSuchElementException();
				return a[last = pos++];
			}

			@Override
			public K previous() {
				if (!hasPrevious()) throw new NoSuchElementException();
				return a[last = --pos];
			}

			@Override
			public int nextIndex() {
				return pos;
			}

			@Override
			public int previousIndex() {
				return pos - 1;
			}

			@Override
			public void add(K k) {
				ObjectList2.this.add(pos++, k);
				last = -1;
			}

			@Override
			public void set(K k) {
				if (last == -1) throw new IllegalStateException();
				ObjectList2.this.set(last, k);
			}

			@Override
			public void remove() {
				if (last == -1) throw new IllegalStateException();
				ObjectList2.this.remove(last);
				/* If the last operation was a next(), we are removing an element *before* us, and we must decrease pos correspondingly. */
				if (last < pos) pos--;
				last = -1;
			}

			@Override
			public void forEachRemaining(final Consumer<? super K> action) {
				while (pos < size) {
					action.accept(a[last = pos++]);
				}
			}

			public int back(int n) {
				if (n < 0) throw new IllegalArgumentException("Argument must be nonnegative: " + n);
				final int remaining = size - pos;
				if (n < remaining) {
					pos -= n;
				} else {
					n = remaining;
					pos = 0;
				}
				last = pos;
				return n;
			}

			public int skip(int n) {
				if (n < 0) throw new IllegalArgumentException("Argument must be nonnegative: " + n);
				final int remaining = size - pos;
				if (n < remaining) {
					pos += n;
				} else {
					n = remaining;
					pos = size;
				}
				last = pos - 1;
				return n;
			}
		};
	}

	/**
	 * Returns an iterator over the elements in this list in proper sequence.
	 *
	 * @return an iterator over the elements in this list in proper sequence
	 */
	@Nonnull
	@Override
	public Iterator<K> iterator () {
		return listIterator(0);
	}

	/**
	 * Returns a list iterator over the elements in this list (in proper
	 * sequence).
	 *
	 * @return a list iterator over the elements in this list (in proper
	 * sequence)
	 */
	@Nonnull
	@Override
	public ListIterator<K> listIterator () {
		return listIterator(0);
	}

	/**
	 * Returns true if this list contains the specified element.
	 *
	 * @implSpec This implementation delegates to {@code indexOf()}.
	 * @see List#contains(Object)
	 */
	@Override
	public boolean contains(final Object k) {
		return indexOf(k) >= 0;
	}

	/**
	 * Compares this type-specific array list to another one.
	 *
	 * @param l a type-specific array list.
	 * @return true if the argument contains the same elements of this type-specific array list.
	 * @apiNote This method exists only for sake of efficiency. The implementation
	 * inherited from the abstract implementation would already work.
	 */
	public boolean equals(final ObjectList2<K> l) {
		// TODO When minimum version of Java becomes Java 9, use the Arrays.equals which takes bounds, which is vectorized.
		if (l == this) return true;
		int s = size();
		if (s != l.size()) return false;
		final K[] a1 = a;
		final K[] a2 = l.a;
		if (a1 == a2 && s == l.size()) return true;
		while (s-- != 0) if (!java.util.Objects.equals(a1[s], a2[s])) return false;
		return true;
	}

	@SuppressWarnings({"unchecked", "unlikely-arg-type"})
	@Override
	public boolean equals(final Object o) {
		if (o == this) return true;
		if (o == null) return false;
		if (!(o instanceof java.util.List)) return false;
		if (o instanceof ObjectList2) {
			// Safe cast because we are only going to take elements from other list, never give them
			return equals((ObjectList2<K>) o);
		}
		return super.equals(o);
	}

}

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

import com.github.tommyettinger.ds.support.sort.ObjectComparators;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Random;

/**
 * A resizable, insertion-ordered list of {@code T} items, typically objects (they can also be arrays).
 * This is a thin wrapper around {@link ArrayList} to implement {@link Ordered} and do some of
 * what libGDX's Array class does. Because this is a generic class and arrays do not interact
 * well with generics, ObjectList does not permit access to a {@code T[]} of items like Array
 * does; you can use {@link #toArray(Object[])} or (if you can use Java 11)
 * {@code toArray(IntFunction)} to make a new array of T from the contents of an ArrayList.
 * The second of these toArray methods is newer; You can use it with code like
 * {@code ObjectList<String> myList = new ObjectList<>(); String[] s = myList.toArray(String::new);}.
 *
 * @author Tommy Ettinger
 * @see ObjectBag ObjectBag is an unordered variant on ObjectList.
 */
public class ObjectList<T> extends ArrayList<T> implements Ordered<T> {

	@Nullable protected transient ObjectListIterator<T> iterator1;
	@Nullable protected transient ObjectListIterator<T> iterator2;

	/**
	 * Returns true if this implementation retains order, which it does.
	 * @return true
	 */
	public boolean keepsOrder () {
		return true;
	}

	/**
	 * Constructs an empty list with the specified initial capacity.
	 *
	 * @param initialCapacity the initial capacity of the list
	 * @throws IllegalArgumentException if the specified initial capacity
	 *                                  is negative
	 */
	public ObjectList (int initialCapacity) {
		super(initialCapacity);
	}

	/**
	 * Constructs an empty list with the specified initial capacity.
	 *
	 * @param ordered         ignored; use an {@link ObjectBag} for an unordered list
	 * @param initialCapacity the initial capacity of the list
	 * @throws IllegalArgumentException if the specified initial capacity
	 *                                  is negative
	 * @deprecated This is equivalent to {@link #ObjectList(int)}; make an {@link ObjectBag} for an unordered list
	 */
	@Deprecated
	public ObjectList (boolean ordered, int initialCapacity) {
		super(initialCapacity);
	}

	/**
	 * Constructs an empty list with an initial capacity of 10.
	 */
	public ObjectList () {
		super();
	}

	/**
	 * Constructs a list containing the elements of the specified
	 * collection, in the order they are returned by the collection's
	 * iterator.
	 *
	 * @param c the collection whose elements are to be placed into this list
	 * @throws NullPointerException if the specified collection is null
	 */
	public ObjectList (Collection<? extends T> c) {
		super(c);
	}

	/**
	 * Constructs a list containing the elements of the specified
	 * collection, in the order they are returned by the collection's
	 * iterator.
	 *
	 * @param ordered ignored; use an {@link ObjectBag} for an unordered list
	 * @param c       the collection whose elements are to be placed into this list
	 * @throws NullPointerException if the specified collection is null
	 * @deprecated This is equivalent to {@link #ObjectList(Collection)}; make an {@link ObjectBag} for an unordered list
	 */
	@Deprecated
	public ObjectList (boolean ordered, Collection<? extends T> c) {
		super(c);
	}

	public ObjectList (T[] a) {
		super(a.length);
		Collections.addAll(this, a);
	}

	public ObjectList (T[] a, int offset, int count) {
		super(a.length);
		for (int i = offset, n = Math.min(offset + count, a.length); i < n; i++) {
			add(a[i]);
		}
	}

	/**
	 * Creates a new list by copying {@code count} items from the given Ordered, starting at {@code offset} in that Ordered,
	 * into this.
	 *
	 * @param other  another Ordered of the same type
	 * @param offset the first index in other's ordering to draw an item from
	 * @param count  how many items to copy from other
	 */
	public ObjectList (Ordered<T> other, int offset, int count) {
		super(count);
		addAll(0, other, offset, count);
	}

	@Override
	public void add (int index, @Nullable T element) {
		super.add(index, element);
	}

	/**
	 * This is an alias for {@link #add(int, Object)} to improve compatibility with primitive lists.
	 *
	 * @param index   index at which the specified element is to be inserted
	 * @param element element to be inserted
	 */
	public void insert (int index, @Nullable T element) {
		super.add(index, element);
	}

	@Override
	@Nullable
	public T remove (int index) {
		return super.remove(index);
	}

	/**
	 * This is an alias for {@link #remove(int)} to make the API the same for primitive lists.
	 *
	 * @param index must be non-negative and less than {@link #size()}
	 * @return the previously-held item at the given index
	 */
	@Nullable
	public T removeAt (int index) {
		return super.remove(index);
	}

	/**
	 * Removes the items between the specified start index, inclusive, and end index, exclusive.
	 * Note that this takes different arguments than some other range-related methods; this needs
	 * a start index and an end index, rather than a count of items. This matches the behavior in
	 * the JDK collections. This is also different from removeRange() in the libGDX Array class
	 * because it is exclusive on end, instead of how Array is inclusive on end.
	 *
	 * @param start the first index to remove, inclusive
	 * @param end   the last index (after what should be removed), exclusive
	 */
	@Override
	public void removeRange (int start, int end) {
		super.removeRange(start, end);
	}

	/**
	 * Removes from this ObjectList element-wise occurrences of elements contained in the specified Iterable.
	 * Note that if a value is present more than once in this ObjectList, only one of those occurrences
	 * will be removed for each occurrence of that value in {@code able}. If {@code able} has the same
	 * contents as this ObjectList or has additional items, then removing each of {@code able} will clear this.
	 * <br>
	 * This matches the behavior of the libGDX {@code Array.removeAll(Array)} method in libGDX 1.10.0 and earlier.
	 * The method {@link #removeAll(Collection)} here matches the behavior of the JDK
	 * {@link List#removeAll(Collection)} method.
	 *
	 * @param able an Iterable of T items to remove one-by-one, such as another ObjectList or an ObjectSet
	 * @return true if this list was modified.
	 */
	public boolean removeEach (Iterable<T> able) {
		Iterator<T> it = able.iterator();
		boolean changed = false;
		while (it.hasNext()) {
			changed |= remove(it.next());
		}
		return changed;
	}

	/**
	 * Convenience method that adds two items. This avoids constructing an array for a varargs.
	 * @param value0 a T item to add
	 * @param value1 a T item to add
	 */
	public void add(@Nullable T value0, @Nullable T value1) {
		add(value0);
		add(value1);
	}

	/**
	 * Convenience method that adds three items. This avoids constructing an array for a varargs.
	 * @param value0 a T item to add
	 * @param value1 a T item to add
	 * @param value2 a T item to add
	 */
	public void add(@Nullable T value0, @Nullable T value1, @Nullable T value2) {
		add(value0);
		add(value1);
		add(value2);
	}

	/**
	 * Convenience method that adds four items. This avoids constructing an array for a varargs.
	 * @param value0 a T item to add
	 * @param value1 a T item to add
	 * @param value2 a T item to add
	 * @param value3 a T item to add
	 */
	public void add(@Nullable T value0, @Nullable T value1, @Nullable T value2, @Nullable T value3) {
		add(value0);
		add(value1);
		add(value2);
		add(value3);
	}

	/**
	 * Adds each item in the array {@code a} to this ObjectList, appending to the end.
	 *
	 * @param a a non-null array of {@code T}
	 * @return true if this is modified by this call, as {@link #addAll(Collection)} does
	 */
	public boolean addAll (T[] a) {
		return Collections.addAll(this, a);
	}

	/**
	 * Adds each item in the array {@code a} to this ObjectList, inserting starting at {@code insertionIndex}.
	 *
	 * @param insertionIndex where to insert into this ObjectList
	 * @param a              a non-null array of {@code T}
	 * @return true if this is modified by this call, as {@link #addAll(Collection)} does
	 */
	public boolean addAll (int insertionIndex, T[] a) {
		return addAll(insertionIndex, a, 0, a.length);
	}

	/**
	 * Adds up to {@code count} items, starting from {@code offset}, in the array {@code a} to this ObjectList, appending to the end.
	 *
	 * @param a      a non-null array of {@code T}
	 * @param offset the first index in {@code a} to use
	 * @param count  how many indices in {@code a} to use
	 * @return true if this is modified by this call, as {@link #addAll(Collection)} does
	 */
	public boolean addAll (T[] a, int offset, int count) {
		if(offset < 0) throw new IndexOutOfBoundsException("offset cannot be negative.");
		int n = Math.min(offset + count, a.length);
		for (int i = offset; i < n; i++) {
			add(a[i]);
		}
		return offset < n;
	}

	/**
	 * Inserts the specified number of items at the specified index. The new items will have values equal to the values at those
	 * indices before the insertion, and the previous values will be pushed to after the duplicated range.
	 * @param index the first index to duplicate
	 * @param count how many items to duplicate
	 */
	public boolean duplicateRange (int index, int count) {
		if (index + count >= size()) {throw new IllegalStateException("Sum of index and count is too large: " + (index + count) + " must not be >= " + size());}
		addAll(index, subList(index, index + count));
		return count > 0;
	}

	/**
	 * Returns true if this ObjectList contains any the specified values.
	 *
	 * @param values May contains nulls.
	 * @return true if this ObjectList contains any of the items in {@code values}, false otherwise
	 */
	public boolean containsAny (Collection<? extends T> values) {
		for (T v : values) {
			if (contains(v)) {return true;}
		}
		return false;
	}

	/**
	 * Adds up to {@code count} items, starting from {@code offset}, in the array {@code a} to this ObjectList,
	 * inserting starting at {@code insertionIndex}.
	 *
	 * @param insertionIndex where to insert into this ObjectList
	 * @param a              a non-null array of {@code T}
	 * @param offset         the first index in {@code a} to use
	 * @param count          how many indices in {@code a} to use
	 * @return true if this is modified by this call, as {@link #addAll(Collection)} does
	 */
	public boolean addAll (int insertionIndex, T[] a, int offset, int count) {
		boolean changed = false;
		int end = Math.min(offset + count, a.length);
		ensureCapacity(end - offset);
		for (int i = offset; i < end; i++) {
			add(insertionIndex++, a[i]);
			changed = true;
		}
		return changed;
	}

	/**
	 * Adds up to {@code count} items, starting from {@code offset}, in the Ordered {@code other} to this list,
	 * inserting at the end of the iteration order.
	 *
	 * @param other  a non-null {@link Ordered} of {@code T}
	 * @param offset the first index in {@code other} to use
	 * @param count  how many indices in {@code other} to use
	 * @return true if this is modified by this call, as {@link #addAll(Collection)} does
	 */
	public boolean addAll (Ordered<T> other, int offset, int count) {
		return addAll(size(), other, offset, count);
	}

	/**
	 * Adds up to {@code count} items, starting from {@code offset}, in the Ordered {@code other} to this list,
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

	/**
	 * Removes and returns the last item.
	 */
	@Nullable
	public T pop () {
		int n = size();
		if (n == 0) {throw new IllegalStateException("ObjectList is empty.");}
		return remove(n - 1);
	}

	/**
	 * Returns the last item.
	 */
	@Nullable
	public T peek () {
		int n = size();
		if (n == 0) {throw new IllegalStateException("ObjectList is empty.");}
		return get(n - 1);
	}

	/**
	 * Returns the first item.
	 */
	@Nullable
	public T first () {
		if (size() == 0) {throw new IllegalStateException("ObjectList is empty.");}
		return get(0);
	}

	/**
	 * Returns true if the array has one or more items.
	 */
	public boolean notEmpty () {
		return size() != 0;
	}

	/**
	 * Increases the size of the backing array to accommodate the specified number of additional items. Useful before adding many
	 * items to avoid multiple backing array resizes. Note that this has different behavior from {@link ArrayList#ensureCapacity(int)};
	 * ArrayList's version specifies a minimum capacity (which can result in no change if the capacity is currently larger than that
	 * minimum), whereas this version specifies additional capacity (which always increases capacity if {@code additionalCapacity} is
	 * non-negative). The behavior here matches the primitive-backed lists like {@link IntList}, as well as libGDX Array classes.
	 *
	 * @param additionalCapacity how much room to add to the capacity; this is measured in the number of items this can store
	 */
	@Override
	public void ensureCapacity (int additionalCapacity) {
		super.ensureCapacity(size() + additionalCapacity);
	}

	/**
	 * Uses == for comparison of each item.
	 */
	public boolean equalsIdentity (Object object) {
		if (object == this) {return true;}
		if (!(object instanceof List)) {return false;}
		List list = (List)object;
		int n = size();
		if (n != list.size()) {return false;}
		for (int i = 0; i < n; i++) {if (get(i) != list.get(i)) {return false;}}
		return true;
	}

	@Override
	public boolean equals (Object o) {
		if (o == this)
			return true;
		return super.equals(o);
	}

	@Override
	public int hashCode () {
		return super.hashCode();
	}

	@Override
	public String toString () {
		int n = size();
		if (n == 0) {return "[]";}
		StringBuilder buffer = new StringBuilder(32);
		buffer.append('[');
		buffer.append(get(0));
		for (int i = 1; i < n; i++) {
			buffer.append(", ");
			buffer.append(get(i));
		}
		buffer.append(']');
		return buffer.toString();
	}

	public String toString (String separator) {
		int n = size();
		if (n == 0) {return "";}
		StringBuilder builder = new StringBuilder(32);
		builder.append(get(0));
		for (int i = 1; i < n; i++) {
			builder.append(separator);
			builder.append(get(i));
		}
		return builder.toString();
	}

	public StringBuilder builderAppend (StringBuilder builder, String separator) {
		int n = size();
		if (n == 0) {return builder;}
		builder.append(get(0));
		for (int i = 1; i < n; i++) {
			builder.append(separator);
			builder.append(get(i));
		}
		return builder;
	}

	/**
	 * Returns a list iterator over the elements in this list (in proper
	 * sequence), starting at the specified position in the list.
	 * The specified index indicates the first element that would be
	 * returned by an initial call to {@link ListIterator#next next}.
	 * An initial call to {@link ListIterator#previous previous} would
	 * return the element with the specified index minus one.
	 * <br>
	 * The returned iterator is reused by this ObjectList, so it is likely unsuitable for nested iteration.
	 * Use {@link ObjectListIterator#ObjectListIterator(ObjectList, int)} to create a ListIterator if you need nested iteration.
	 *
	 * @param index
	 * @throws IndexOutOfBoundsException {@inheritDoc}
	 */
	@Override
	public @NonNull ObjectListIterator<T> listIterator (int index) {
		if (iterator1 == null || iterator2 == null) {
			iterator1 = new ObjectListIterator<>(this, index);
			iterator2 = new ObjectListIterator<>(this, index);
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
	 * Returns a list iterator over the elements in this list (in proper sequence).
	 * <br>
	 * The returned iterator is reused by this ObjectList, so it is likely unsuitable for nested iteration.
	 * Use {@link ObjectListIterator#ObjectListIterator(ObjectList)} to create a ListIterator if you need nested iteration.
	 *
	 * @see #listIterator(int)
	 */
	@Override
	public @NonNull ObjectListIterator<T> listIterator () {
		if (iterator1 == null || iterator2 == null) {
			iterator1 = new ObjectListIterator<>(this);
			iterator2 = new ObjectListIterator<>(this);
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

	/**
	 * Returns an iterator over the elements in this list in proper sequence.
	 * <br>
	 * The returned iterator is reused by this ObjectList, so it is likely unsuitable for nested iteration.
	 * Use {@link ObjectListIterator#ObjectListIterator(ObjectList)} to create an Iterator if you need nested iteration.
	 *
	 * @return an iterator over the elements in this list in proper sequence
	 */
	@Override
	public @NonNull ObjectListIterator<T> iterator () {
		if (iterator1 == null || iterator2 == null) {
			iterator1 = new ObjectListIterator<>(this);
			iterator2 = new ObjectListIterator<>(this);
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

	/**
	 * An {@link Iterator} and {@link ListIterator} over the elements of an ObjectList, while also an {@link Iterable}.
	 * @param <T> the generic type for the ObjectList this iterates over
	 */
	public static class ObjectListIterator<T> implements Iterable<T>, ListIterator<T> {
		protected int index, latest = -1;
		protected ObjectList<T> list;
		protected boolean valid = true;

		public ObjectListIterator (ObjectList<T> list) {
			this.list = list;
		}

		public ObjectListIterator (ObjectList<T> list, int index) {
			if (index < 0 || index >= list.size())
				throw new IndexOutOfBoundsException("ObjectListIterator does not satisfy index >= 0 && index < list.size()");
			this.list = list;
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
			if (index >= list.size()) {throw new NoSuchElementException();}
			return list.get(latest = index++);
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
			return index < list.size();
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
			return index > 0 && list.notEmpty();
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
			if (index <= 0 || list.isEmpty()) {throw new NoSuchElementException();}
			return list.get(latest = --index);
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
			if (latest == -1 || latest >= list.size()) {throw new NoSuchElementException();}
			list.removeAt(latest);
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
		public void set (@Nullable T t) {
			if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
			if (latest == -1 || latest >= list.size()) {throw new NoSuchElementException();}
			list.set(latest, t);
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
			if (index > list.size()) {throw new NoSuchElementException();}
			list.insert(index, t);
			if(list.keepsOrder()) ++index;
			latest = -1;
		}

		public void reset () {
			index = 0;
			latest = -1;
		}

		public void reset (int index) {
			if (index < 0 || index >= list.size())
				throw new IndexOutOfBoundsException("ObjectListIterator does not satisfy index >= 0 && index < list.size()");
			this.index = index;
			latest = -1;
		}

		/**
		 * Returns an iterator over elements of type {@code T}.
		 *
		 * @return a ListIterator; really this same ObjectListIterator.
		 */
		@Override
		public @NonNull ObjectListIterator<T> iterator () {
			return this;
		}
	}

	/**
	 * Gets the ObjectList of T items that this data structure holds, in the order it uses for iteration.
	 * This method actually returns this ObjectList directly, since it extends ArrayList.
	 *
	 * @return this ObjectList
	 */
	@Override
	public ObjectList<T> order () {
		return this;
	}

	/**
	 * Switches the ordering of positions {@code a} and {@code b}, without changing any items beyond that.
	 *
	 * @param first  the first position
	 * @param second the second position
	 */
	@Override
	public void swap (int first, int second) {
		set(first, set(second, get(first)));

	}

	/**
	 * Pseudo-randomly shuffles the order of this Ordered in-place.
	 * You can use any {@link Random} class for {@code rng};
	 *
	 * @param rng any {@link Random} class
	 */
	@Override
	public void shuffle (Random rng) {
		for (int i = size() - 1; i > 0; i--) {
			set(i, set(rng.nextInt(i + 1), get(i)));
		}
	}

	/**
	 * Returns a {@code T} item from anywhere in this ObjectList, chosen pseudo-randomly using {@code random}.
	 * If this ObjectList is empty, throws an {@link IllegalStateException}.
	 *
	 * @param random a {@link Random} or a subclass
	 * @return a pseudo-randomly selected item from this ObjectLists
	 */
	@Override
	public T random (Random random) {
		final int n = size();
		if (n == 0) {throw new IllegalStateException("ObjectList is empty.");}
		return get(random.nextInt(n));
	}

	/**
	 * Reduces the size of the array to the specified size. If the array is already smaller than the specified
	 * size, no action is taken.
	 */
	public void truncate (int newSize) {
		if (size() > newSize) {removeRange(newSize, size());}
	}

	@Override
	public void reverse () {
		Collections.reverse(this);
	}

	/**
	 * Sorts this ObjectList based on the natural order of its elements; {@code T} must implement {@link Comparable}
	 * for this to succeed.
	 */
	public void sort () {
		ObjectComparators.sort(this, null);
	}

	/**
	 * Sorts this ObjectList using the given Comparator. If the Comparator is null, then this sorts based on the
	 * natural order of its elements, and {@code T} must implement {@link Comparable}.
	 * <br>
	 * This is implemented explicitly and not annotated with Override because of Android limitations.
	 *
	 * @param c a Comparator that can compare T items, or null to use the natural order of Comparable T items
	 */
	public void sort (@Nullable Comparator<? super T> c) {
		ObjectComparators.sort(this, c);
	}

	/**
	 * Sorts this ObjectList based on the natural order of its elements; {@code T} must implement {@link Comparable}
	 * for this to succeed.
	 * <br>
	 * This uses the TimSort implementation in {@link java.util.Arrays}, instead of the in-place mergesort used by
	 * {@link #sort()}. TimSort tends to be faster for many inputs, but uses more memory. Future JDK versions may
	 * change how {@link ArrayList#sort(Comparator)} works, which would change this as well.
	 */
	public void sortJDK () {
		super.sort(null);
	}

	/**
	 * Sorts this ObjectList using the given Comparator. If the Comparator is null, then this sorts based on the
	 * natural order of its elements, and {@code T} must implement {@link Comparable}.
	 * <br>
	 * This uses the TimSort implementation in {@link java.util.Arrays}, instead of the in-place mergesort used by
	 * {@link #sort()}. TimSort tends to be faster for many inputs, but uses more memory. Future JDK versions may
	 * change how {@link ArrayList#sort(Comparator)} works, which would change this as well.
	 *
	 * @param c a Comparator that can compare T items, or null to use the natural order of Comparable T items
	 */
	public void sortJDK (@Nullable Comparator<? super T> c) {
		super.sort(c);
	}

	/**
	 * Creates a new ObjectList that holds only the given item, but can be resized.
	 * @param item one T item
	 * @return a new ObjectList that holds the given item
	 * @param <T> the type of item, typically inferred except for array arguments
	 */
	public static <T> ObjectList<T> with (T item) {
		ObjectList<T> list = new ObjectList<>(1);
		list.add(item);
		return list;
	}

	/**
	 * Creates a new ObjectList that will hold the items in the given array or varargs.
	 * @param varargs either 0 or more T items, or an array of T
	 * @return a new ObjectList that holds the given T items
	 * @param <T> the type of items, typically inferred by all the items being the same type
	 */
	@SafeVarargs
	public static <T> ObjectList<T> with (T... varargs) {
		return new ObjectList<>(varargs);
	}
}

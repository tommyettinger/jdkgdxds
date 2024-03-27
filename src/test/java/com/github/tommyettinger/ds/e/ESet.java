/*
 * Copyright (c) 2024 See AUTHORS file.
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

package com.github.tommyettinger.ds.e;

import com.github.tommyettinger.digital.BitConversion;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * A Set of Enum items. Unlike {@link java.util.EnumSet}, this does not require a Class at construction time, which can be
 * useful for serialization purposes.
 */
public class ESet extends AbstractSet<Enum<?>> implements Set<Enum<?>>, Iterable<Enum<?>> {
	protected int size;
	protected int[] table;
	protected Enum<?>[] enumValues;

	/**
	 * Empty constructor; using this will postpone allocating any internal arrays until {@link #add(Enum)} is first called
	 * (potentially indirectly).
	 */
	public ESet () {
		super();
	}

	/**
	 * Initializes this set so that it has exactly enough capacity as needed to contain each Enum constant defined in
	 * {@code valuesResult}, assuming valuesResult stores every possible constant in one Enum type. As the name suggests,
	 * you almost always would obtain valuesResult from calling {@code values()} on an Enum type, but you can share one
	 * reference to one Enum array across many ESet instances if you don't modify the shared array.
	 * @param valuesResult almost always, the result of calling {@code values()} on an Enum type; used directly, not copied
	 */
	public ESet (Enum<?>[] valuesResult) {
		super();
		if(valuesResult == null) return;
		enumValues = valuesResult;
		table = new int[valuesResult.length + 31 >>> 5];
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
	 * Returns {@code true} if this set contains no elements.
	 *
	 * @return {@code true} if this set contains no elements
	 */
	@Override
	public boolean isEmpty () {
		return size == 0;
	}

	/**
	 * Returns {@code true} if this set contains the specified element.
	 * More formally, returns {@code true} if and only if this set
	 * contains an element {@code e} such that
	 * {@code Objects.equals(item, e)}.
	 *
	 * @param item element whose presence in this set is to be tested
	 * @return {@code true} if this set contains the specified element
	 * @throws ClassCastException   if the type of the specified element
	 *                              is incompatible with this set
	 *                              (<a href="Collection.html#optional-restrictions">optional</a>)
	 * @throws NullPointerException if the specified element is null and this
	 *                              set does not permit null elements
	 *                              (<a href="Collection.html#optional-restrictions">optional</a>)
	 */
	@Override
	public boolean contains (Object item) {
		if(table == null || item == null || size == 0 || !(item instanceof Enum<?>)) return false;
		final Enum<?> e = (Enum<?>)item;
		final int ord = e.ordinal(), upper = ord >>> 5;
		if(table.length <= upper) return false;
		// (1 << ord) has ord implicitly used modulo 32
		return (table[upper] & 1 << ord) != 0;
	}

	/**
	 * Returns an iterator over the elements in this set.  The elements are
	 * returned in the order of their {@link Enum#ordinal()} values.
	 *
	 * @return an iterator over the elements in this set
	 */
	@Override
	public @NonNull Iterator<Enum<?>> iterator () {
		// TODO: iterator!
		return null;
	}

	/**
	 * Adds the specified element to this set if it is not already present
	 * (optional operation).  More formally, adds the specified element
	 * {@code e} to this set if the set contains no element {@code e2}
	 * such that
	 * {@code Objects.equals(e, e2)}.
	 * If this set already contains the element, the call leaves the set
	 * unchanged and returns {@code false}.  In combination with the
	 * restriction on constructors, this ensures that sets never contain
	 * duplicate elements.
	 *
	 * <p>The stipulation above does not imply that sets must accept all
	 * elements; sets may refuse to add any particular element, including
	 * {@code null}, and throw an exception, as described in the
	 * specification for {@link Collection#add Collection.add}.
	 * Individual set implementations should clearly document any
	 * restrictions on the elements that they may contain.
	 *
	 * @param item element to be added to this set
	 * @return {@code true} if this set did not already contain the specified
	 * element
	 * @throws UnsupportedOperationException if the {@code add} operation
	 *                                       is not supported by this set
	 * @throws ClassCastException            if the class of the specified element
	 *                                       prevents it from being added to this set
	 * @throws NullPointerException          if the specified element is null and this
	 *                                       set does not permit null elements
	 * @throws IllegalArgumentException      if some property of the specified element
	 *                                       prevents it from being added to this set
	 */
	@Override
	public boolean add (Enum<?> item) {
		if(item == null) return false;
		if(enumValues == null) enumValues = item.getDeclaringClass().getEnumConstants();
		if(table == null) table = new int[enumValues.length + 31 >>> 5];
		final int ord = item.ordinal(), upper = ord >>> 5;
		if(table.length <= upper) return false;
		// (1 << ord) has ord implicitly used modulo 32
		boolean changed = (table[upper]) != (table[upper] |= 1 << ord);
		if(changed) size++;
		return changed;
	}

	/**
	 * Removes the specified element from this set if it is present
	 * (optional operation).  More formally, removes an element {@code e}
	 * such that
	 * {@code Objects.equals(item, e)}, if
	 * this set contains such an element.  Returns {@code true} if this set
	 * contained the element (or equivalently, if this set changed as a
	 * result of the call).  (This set will not contain the element once the
	 * call returns.)
	 *
	 * @param item object to be removed from this set, if present
	 * @return {@code true} if this set contained the specified element
	 * @throws ClassCastException            if the type of the specified element
	 *                                       is incompatible with this set
	 *                                       (<a href="Collection.html#optional-restrictions">optional</a>)
	 * @throws NullPointerException          if the specified element is null and this
	 *                                       set does not permit null elements
	 *                                       (<a href="Collection.html#optional-restrictions">optional</a>)
	 * @throws UnsupportedOperationException if the {@code remove} operation
	 *                                       is not supported by this set
	 */
	@Override
	public boolean remove (Object item) {
		if(table == null || item == null || size == 0 || !(item instanceof Enum<?>)) return false;
		final Enum<?> e = (Enum<?>)item;
		final int ord = e.ordinal(), upper = ord >>> 5;
		if(table.length <= upper) return false;
		// (1 << ord) has ord implicitly used modulo 32
		final boolean changed = (table[upper]) != (table[upper] &= ~(1 << ord));
		if(changed) size--;
		return changed;
	}

	/**
	 * Removes all the elements from this set (optional operation).
	 * The set will be empty after this call returns.
	 *
	 * @throws UnsupportedOperationException if the {@code clear} method
	 *                                       is not supported by this set
	 */
	@Override
	public void clear () {
		size = 0;
		if(table != null)
			Arrays.fill(table, 0);
	}

	/**
	 * Returns the first Enum contained in the set with an ordinal equal to or greater than {@code minOrdinal}.
	 * If no such Enum exists, or if minOrdinal is invalid (such as if it is negative or greater than the highest ordinal in the
	 * Enum type this holds), then {@code null} is returned.
	 * @param minOrdinal the index to start looking at; does not need to have an Enum present there, but must be non-negative
	 * @return the first Enum contained in the set on or after the specified starting point, or null if none can be found
	 */
	public Enum<?> nextEnum (int minOrdinal) {
		if(minOrdinal < 0) return null;
		int[] bits = this.table;
		int word = minOrdinal >>> 5;
		int bitsLength = bits.length;
		if (word >= bitsLength)
			return null;
		int bitsAtWord = bits[word] & -1 << minOrdinal; // shift implicitly is masked to bottom 63 bits
		if (bitsAtWord != 0) {
			return enumValues[BitConversion.countTrailingZeros(bitsAtWord) + (word << 5)]; // countTrailingZeros() uses an intrinsic candidate, and should be extremely fast
		}
		for (word++; word < bitsLength; word++) {
			bitsAtWord = bits[word];
			if (bitsAtWord != 0) {
				return enumValues[BitConversion.countTrailingZeros(bitsAtWord) + (word << 5)];
			}
		}
		return null;
	}


	/**
	 * Returns the first Enum contained in the set on or after the point where the given Enum {@code from} would occur.
	 * If no such Enum exists then {@code null} is returned.
	 * @param from the Enum to start looking at; does not need to be present in the set
	 * @return the first Enum contained in the set on or after the specified starting point, or null if none can be found
	 */
	public Enum<?> nextEnum (Enum<?> from) {
		if(from == null) return null;
		int fromIndex = from.ordinal();
		int[] bits = this.table;
		int word = fromIndex >>> 5;
		int bitsLength = bits.length;
		if (word >= bitsLength)
			return null;
		int bitsAtWord = bits[word] & -1 << fromIndex; // shift implicitly is masked to bottom 63 bits
		if (bitsAtWord != 0) {
			return enumValues[BitConversion.countTrailingZeros(bitsAtWord) + (word << 5)]; // countTrailingZeros() uses an intrinsic candidate, and should be extremely fast
		}
		for (word++; word < bitsLength; word++) {
			bitsAtWord = bits[word];
			if (bitsAtWord != 0) {
				return enumValues[BitConversion.countTrailingZeros(bitsAtWord) + (word << 5)];
			}
		}
		return null;
	}

}

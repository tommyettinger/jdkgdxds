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
import com.github.tommyettinger.ds.ObjectList;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * A Set of Enum items. Unlike {@link java.util.EnumSet}, this does not require a Class at construction time, which can be
 * useful for serialization purposes.
 */
public class ESet extends AbstractSet<Enum<?>> implements Set<Enum<?>>, Iterable<Enum<?>> {
	protected int size;
	protected int[] table;
	protected Enum<?>[] enumValues;
	@Nullable protected transient ESetIterator iterator1;
	@Nullable protected transient ESetIterator iterator2;

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
	 * @param ignoredToDistinguish an ignored boolean that differentiates this constructor, which defined a key universe,
	 *                               from one that takes contents
	 */
	public ESet (Enum<?>[] valuesResult, boolean ignoredToDistinguish) {
		super();
		if(valuesResult == null) return;
		enumValues = valuesResult;
		table = new int[valuesResult.length + 31 >>> 5];
	}

	/**
	 * Initializes this set so that it holds the given Enum values, with the universe of possible Enum constants this can hold
	 * determined by the type of the first Enum in {@code contents}.
	 * <br>
	 * This is different from {@link #ESet(Enum[], boolean)} in that this takes constants and puts them in the set, while the other
	 * constructor takes all possible Enum constants, usually from calling {@code values()}.
	 *
	 * @param contents an array of Enum items to place into this set
	 */
	public ESet(Enum<?>[] contents) {
		super();
		if(contents == null) return;
		addAll(contents);
	}

	/**
	 * Initializes this set so that it holds the given Enum values, with the universe of possible Enum constants this can hold
	 * determined by the type of the first Enum in {@code contents}.
	 *
	 * @param contents a Collection of Enum items to place into this set
	 */
	public ESet(Collection<Enum<?>> contents) {
		super();
		if(contents == null) return;
		addAll(contents);
	}

	/**
	 * Copy constructor; uses a direct reference to the enum values that may be cached in {@code other}, but copies other fields.
	 * @param other another ESet that will have most of its data copied, but its cached {@code values()} results will be used directly
	 */
	public ESet (ESet other) {
		this.size = other.size;
		if(other.table != null)
			this.table = Arrays.copyOf(other.table, other.table.length);
		this.enumValues = other.enumValues;
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
	 * Returns an iterator for the items in the set. The elements are
	 * returned in the order of their {@link Enum#ordinal()} values. Remove is supported.
	 * <p>
	 * Use the {@link ESetIterator} constructor for nested or multithreaded iteration.
	 */
	@Override
	public ESetIterator iterator () {
		if (iterator1 == null || iterator2 == null) {
			iterator1 = new ESetIterator(this);
			iterator2 = new ESetIterator(this);
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
	 * Adds all Enum items in the given array to this set. Returns true if this set was modified at all
	 * in the process (that is, if any items in {@code c} were not already present in this set).
	 *
	 * @see #add(Enum)
	 */
	public boolean addAll (Enum<?>[] c) {
		boolean modified = false;
		for (int i = 0; i < c.length; i++) {
			modified |= add(c[i]);
		}
		return modified;
	}

	/**
	 * Removes all the elements from this set.
	 * The set will be empty after this call returns.
	 * This does not change the universe of possible Enum items this can hold.
	 */
	@Override
	public void clear () {
		size = 0;
		if(table != null)
			Arrays.fill(table, 0);
	}

	/**
	 * Returns the first ordinal equal to or greater than {@code minOrdinal} of the an Enum contained in the set.
	 * If no such Enum exists, or if minOrdinal is invalid (such as if it is negative or greater than the highest ordinal in the
	 * Enum type this holds), then {@code -1} is returned.
	 * @param minOrdinal the index to start looking at; does not need to have an Enum present there, but must be non-negative
	 * @return the first ordinal of an Enum contained in the set on or after the specified starting point, or {@code -1} if none can be found
	 */
	public int nextOrdinal (int minOrdinal) {
		if(minOrdinal < 0) return -1;
		int[] bits = this.table;
		int word = minOrdinal >>> 5;
		int bitsLength = bits.length;
		if (word >= bitsLength)
			return -1;
		int bitsAtWord = bits[word] & -1 << minOrdinal; // shift implicitly is masked to bottom 31 bits
		if (bitsAtWord != 0) {
			return BitConversion.countTrailingZeros(bitsAtWord) + (word << 5); // countTrailingZeros() uses an intrinsic candidate, and should be extremely fast
		}
		for (word++; word < bitsLength; word++) {
			bitsAtWord = bits[word];
			if (bitsAtWord != 0) {
				return BitConversion.countTrailingZeros(bitsAtWord) + (word << 5);
			}
		}
		return -1;
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
		int bitsAtWord = bits[word] & -1 << minOrdinal; // shift implicitly is masked to bottom 31 bits
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
		int bitsAtWord = bits[word] & -1 << fromIndex; // shift implicitly is masked to bottom 31 bits
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

	public static class ESetIterator implements Iterator<Enum<?>> {
		static private final int INDEX_ILLEGAL = -1, INDEX_ZERO = -1;

		public boolean hasNext;

		final ESet set;
		int nextIndex, currentIndex;
		boolean valid = true;

		public ESetIterator (ESet set) {
			this.set = set;
			reset();
		}

		public void reset () {
			currentIndex = INDEX_ILLEGAL;
			nextIndex = INDEX_ZERO;
			findNextIndex();
		}

		void findNextIndex () {
			nextIndex = set.nextOrdinal(nextIndex + 1);
			hasNext = nextIndex != INDEX_ILLEGAL;
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
			if (currentIndex < 0) {
				throw new IllegalStateException("next must be called before remove.");
			}
			set.remove(set.enumValues[currentIndex]);
			currentIndex = INDEX_ILLEGAL;
		}

		@Override
		public Enum<?> next () {
			if (!hasNext) {throw new NoSuchElementException();}
			if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
			currentIndex = nextIndex;
			findNextIndex();
			return set.enumValues[currentIndex];
		}

		/**
		 * Returns a new {@link ObjectList} containing the remaining items.
		 * Does not change the position of this iterator.
		 */
		public ObjectList<Enum<?>> toList () {
			ObjectList<Enum<?>> list = new ObjectList<Enum<?>>(set.size());
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

		/**
		 * Append the remaining items that this can iterate through into the given PrimitiveCollection.OfInt.
		 * Does not change the position of this iterator.
		 * @param coll any modifiable PrimitiveCollection.OfInt; may have items appended into it
		 * @return the given primitive collection
		 */
		public Collection<Enum<?>> appendInto(Collection<Enum<?>> coll) {
			int currentIdx = currentIndex, nextIdx = nextIndex;
			boolean hn = hasNext;
			while (hasNext) {coll.add(next());}
			currentIndex = currentIdx;
			nextIndex = nextIdx;
			hasNext = hn;
			return coll;
		}

	}

	public static ESet with (Enum<?> item) {
		ESet set = new ESet();
		set.add(item);
		return set;
	}

	public static ESet with (Enum<?>... array) {
		return new ESet(array);
	}

	/**
	 * Creates a new ESet using the given result of calling {@code values()} on an Enum type, but with no items initially
	 * stored in the set.
	 * <br>
	 * This is the same as calling {@link #ESet(Enum[], boolean)}.
	 *
	 * @param valuesResult almost always, the result of calling {@code values()} on an Enum type; used directly, not copied
	 * @return a new ESet with the specified universe of possible items, but none present in the set
	 */
	public static ESet noneOf(Enum<?>[] valuesResult) {
		return new ESet(valuesResult, true);
	}

	/**
	 * Creates a new ESet using the given result of calling {@code values()} on an Enum type, and with all possible items initially
	 * stored in the set.
	 *
	 * @param valuesResult almost always, the result of calling {@code values()} on an Enum type; used directly, not copied
	 * @return a new ESet with the specified universe of possible items, and all of them present in the set
	 */
	public static ESet allOf(Enum<?>[] valuesResult) {
		ESet coll = new ESet(valuesResult, true);

		for (int i = 0; i < coll.table.length - 1; i++) {
			coll.table[i] = -1;
		}
		coll.table[coll.table.length - 1] = -1 >>> -valuesResult.length;
		coll.size = valuesResult.length;
		return coll;
	}

	/**
	 * Given another ESet, this creates a new ESet with the same universe as {@code other}, but with any elements present in other
	 * absent in the new set, and any elements absent in other present in the new set.
	 *
	 * @param other another ESet that this will copy
	 * @return a complemented copy of {@code other}
	 */
	public static ESet complementOf(ESet other) {
		ESet coll = new ESet(other);
		for (int i = 0; i < coll.table.length - 1; i++) {
			coll.table[i] ^= -1;
		}
		coll.table[coll.table.length - 1] ^= -1 >>> -coll.enumValues.length;
		coll.size = coll.enumValues.length - other.size;
		return coll;
	}

	/**
	 * Creates an ESet holding Enum items between the ordinals of {@code start} and {@code end}. If the ordinal of end is less than
	 * the ordinal of start, this treats start and end as swapped. If start and end are the same, this just inserts that one Enum.
	 * @param start the starting inclusive Enum to insert
	 * @param end the ending inclusive Enum to insert
	 * @return a new ESet containing start, end, and any Enum constants with ordinals between them
	 * @param <E> the shared Enum type of both start and end
	 */
	public <E extends Enum<E>> ESet range(Enum<E> start, Enum<E> end) {
		if(start == null || end == null) return null;
		final int mn = Math.min(start.ordinal(), end.ordinal());
		final int mx = Math.max(start.ordinal(), end.ordinal());
		final int upperMin = mn >>> 5;
		final int upperMax = mx >>> 5;
		ESet coll = new ESet();
		coll.add(start);
		if(upperMin == upperMax){
			coll.table[upperMin] = (-1 >>> ~mx) ^ (-1 >>> -mn);
		} else {
			coll.table[upperMin] = -1 << mn;
			for (int i = upperMin + 1; i < upperMax; i++) {
				coll.table[i] = -1;
			}
			coll.table[upperMax] = -1 >>> ~mx;
		}
		coll.size += mx - mn;
		return coll;
	}
}

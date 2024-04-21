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

package com.github.tommyettinger.ds;

import com.github.tommyettinger.digital.BitConversion;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * A Set of Enum items. Unlike {@link java.util.EnumSet}, this does not require a Class at construction time, which can be
 * useful for serialization purposes. Instead of storing a Class, this holds a "key universe" (which is almost always the
 * same as an array returned by calling {@code values()} on an Enum type), and key universes are ideally shared between
 * compatible EnumSets. No allocation is done unless this is changing its table size and/or key universe.
 * <br>
 * The key universe is an important concept here; it is simply an array of all possible Enum values the EnumSet can use as keys, in
 * the specific order they are declared. You almost always get a key universe by calling {@code MyEnum.values()}, but you
 * can also use {@link Class#getEnumConstants()} for an Enum class. You can and generally should reuse key universes in order to
 * avoid allocations and/or save memory; the method {@link #noneOf(Enum[])} creates an empty EnumSet with
 * a given key universe. If you need to use the zero-argument constructor, you can, and the key universe will be obtained from the
 * first key placed into the EnumSet, though it won't be shared at first. You can also set the key universe with
 * {@link #clearToUniverse(Enum[])}, in the process of clearing the map.
 * <br>
 * This class tries to be as compatible as possible with {@link java.util.EnumSet}, though this expands on that where possible.
 *
 * @author Tommy Ettinger
 */
public class EnumSet extends AbstractSet<Enum<?>> implements Set<Enum<?>>, Iterable<Enum<?>> {
	protected int size;
	protected int[] table;
	protected Enum<?>[] universe;
	@Nullable protected transient ESetIterator iterator1;
	@Nullable protected transient ESetIterator iterator2;

	/**
	 * Empty constructor; using this will postpone allocating any internal arrays until {@link #add(Enum)} is first called
	 * (potentially indirectly).
	 */
	public EnumSet () {
		super();
	}

	/**
	 * Initializes this set so that it has exactly enough capacity as needed to contain each Enum constant defined in
	 * {@code universe}, assuming universe stores every possible constant in one Enum type.
	 * You almost always obtain universe from calling {@code values()} on an Enum type, and you can share one
	 * reference to one Enum array across many EnumSet instances if you don't modify the shared array. Sharing the same
	 * universe helps save some memory if you have (very) many EnumSet instances.
	 * <br>
	 * Because the {@code boolean} parameter here is easy to forget, you may want to prefer calling {@link #noneOf(Enum[])}
	 * instead of using this directly.
	 *
	 * @param universe almost always, the result of calling {@code values()} on an Enum type; used directly, not copied
	 * @param ignoredToDistinguish an ignored boolean that differentiates this constructor, which defined a key universe,
	 *                               from one that takes contents
	 */
	public EnumSet (Enum<?>@Nullable [] universe, boolean ignoredToDistinguish) {
		super();
		if(universe == null) return;
		this.universe = universe;
		table = new int[universe.length + 31 >>> 5];
	}

	/**
	 * Initializes this set so that it has exactly enough capacity as needed to contain each Enum constant defined by the
	 * Class {@code universeClass}, assuming universeClass is non-null. This simply calls {@link #EnumSet(Enum[], boolean)}
	 * for convenience. Note that this constructor allocates a new array of Enum constants each time it is called, where
	 * if you use {@link #EnumSet(Enum[], boolean)} (or its equivalent, {@link #noneOf(Enum[])}), you can reuse an
	 * unmodified array to reduce allocations.
	 *
	 * @param universeClass the Class of an Enum type that defines the universe of valid Enum items this can hold
	 */
	public EnumSet (@Nullable Class<? extends Enum<?>> universeClass) {
		this(universeClass == null ? null : universeClass.getEnumConstants(), true);
	}

	/**
	 * Initializes this set so that it holds the given Enum values, with the universe of possible Enum constants this can hold
	 * determined by the type of the first Enum in {@code contents}.
	 * <br>
	 * This is different from {@link #EnumSet(Enum[], boolean)} in that this takes constants and puts them in the set, while the other
	 * constructor takes all possible Enum constants, usually from calling {@code values()}. You can also specify the contents of a
	 * new EnumSet conveniently using {@link #with(Enum[])}, which allows passing items as varargs.
	 *
	 * @param contents an array of Enum items to place into this set
	 */
	public EnumSet (Enum<?>[] contents) {
		super();
		if(contents == null) throw new NullPointerException("EnumSet cannot be constructed with a null array.");
		addAll(contents);
	}

	/**
	 * Initializes this set so that it holds the given Enum values, with the universe of possible Enum constants this can hold
	 * determined by the type of the first Enum in {@code contents}.
	 *
	 * @param contents a Collection of Enum items to place into this set
	 */
	public EnumSet (Collection<? extends Enum<?>> contents) {
		super();
		if(contents == null) throw new NullPointerException("EnumSet cannot be constructed with a null Collection.");
		addAll(contents);
	}

	/**
	 * Copy constructor; uses a direct reference to the enum values that may be cached in {@code other}, but copies other fields.
	 * @param other another EnumSet that will have most of its data copied, but its cached {@code values()} results will be used directly
	 */
	public EnumSet (EnumSet other) {
		if(other == null) throw new NullPointerException("EnumSet cannot be constructed from a null EnumSet.");
		this.size = other.size;
		if(other.table != null)
			this.table = Arrays.copyOf(other.table, other.table.length);
		this.universe = other.universe;
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
		final int ord = e.ordinal();
		if(ord >= universe.length || universe[ord] != item) return false;
		final int upper = ord >>> 5;
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
	public @NonNull ESetIterator iterator () {
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
	 */
	@Override
	public boolean add (Enum<?> item) {
		if(item == null) throw new NullPointerException("Items added to an EnumSet must not be null.");
		if(universe == null) universe = item.getDeclaringClass().getEnumConstants();
		if(table == null) table = new int[universe.length + 31 >>> 5];
		final int ord = item.ordinal();
		if(ord >= universe.length || universe[ord] != item) throw new ClassCastException("Incompatible item for this EnumSet: " + item);
		final int upper = ord >>> 5;
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
		final int ord = e.ordinal();
		if(ord >= universe.length || universe[ord] != e) return false;
		final int upper = ord >>> 5;
		if(table.length <= upper) return false;
		// (1 << ord) has ord implicitly used modulo 32
		final boolean changed = (table[upper]) != (table[upper] &= ~(1 << ord));
		if(changed) size--;
		return changed;
	}

	/**
	 * Removes all items from this unless they are also in the given Collection {@code c}.
	 *
	 * @param c usually another EnumSet, but not required to be
	 */
	@Override
	public boolean retainAll (@NonNull Collection<?> c) {
		if(size == 0 || table == null || universe == null || universe.length == 0) return false;
		if(!(c instanceof EnumSet))
			return super.retainAll(c);
		EnumSet es = (EnumSet)c;
		if(es.table == null || es.universe == null || es.size == 0 || universe[0] != es.universe[0]) {
			clear();
			return true;
		}
		int oldSize = size;
		size = 0;
		for (int i = 0; i < table.length; i++) {
			size += Integer.bitCount(table[i] &= es.table[i]);
		}
		return size != oldSize;
	}

	/**
	 * Adds all the elements in the specified collection to this collection.
	 *
	 * @param c usually another EnumSet, but not required to be
	 */
	@Override
	public boolean addAll (@NonNull Collection<? extends Enum<?>> c) {
		if(!(c instanceof EnumSet))
			return super.addAll(c);
		EnumSet es = (EnumSet)c;
		if(es.universe == null || es.universe.length == 0) return false;
		if(universe == null) universe = es.universe;
		if(table == null) table = new int[universe.length + 31 >>> 5];
		if(es.universe.length != universe.length || universe[0] != es.universe[0]) return false;
		int oldSize = size;
		size = 0;
		for (int i = 0; i < table.length; i++) {
			size += Integer.bitCount(table[i] |= es.table[i]);
		}
		return size != oldSize;
	}

	/**
	 * Returns true if this EnumSet contains all items in the given Collection, or false otherwise.
	 * @param c usually another EnumSet, but not required to be
	 */
	@Override
	public boolean containsAll (@NonNull Collection<?> c) {
		if(!(c instanceof EnumSet))
			return super.containsAll(c);
		EnumSet es = (EnumSet)c;
		if(es.size == 0 || es.universe == null || es.universe.length == 0) return true;
		if(size < es.size || universe == null || universe.length != es.universe.length || universe[0] != es.universe[0]) return false;
		for (int i = 0; i < table.length; i++) {
			if((~table[i] & es.table[i]) != 0) return false;
		}
		return true;
	}

	/**
	 * Removes from this EnumSet every element in the given Collection.
	 * @param c usually another EnumSet, but not required to be
	 * @return {@code true} if this set changed as a result of the call
	 */
	@Override
	public boolean removeAll (@NonNull Collection<?> c) {
		if(table == null || universe == null || universe.length == 0) return false;
		if(!(c instanceof EnumSet))
			return super.removeAll(c);
		EnumSet es = (EnumSet)c;
		if(es.table == null || es.universe == null || es.universe.length != universe.length || es.size == 0 || universe[0] != es.universe[0])
			return false;
		int oldSize = size;
		size = 0;
		for (int i = 0; i < table.length; i++) {
			size += Integer.bitCount(table[i] &= ~es.table[i]);
		}
		return size != oldSize;
	}

	/**
	 * Adds all Enum items in the given array to this set. Returns true if this set was modified at all
	 * in the process (that is, if any items in {@code c} were not already present in this set).
	 *
	 * @see #add(Enum)
	 */
	public boolean addAll (Enum<?>@NonNull [] c) {
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
	 * Removes all the elements from this set and can reset the universe of possible Enum items this can hold.
	 * The set will be empty after this call returns.
	 * This changes the universe of possible Enum items this can hold to match {@code universe}.
	 * If {@code universe} is null, this resets this set to the state it would have after {@link #EnumSet()} was called.
	 * If the table this would need is the same size as or smaller than the current table (such as if {@code universe} is the same as
	 * the universe here), this will not allocate, but will still clear any items this holds and will set the universe to the given one.
	 * Otherwise, this allocates and uses a new table of a larger size, with nothing in it, and uses the given universe.
	 * This always uses {@code universe} directly, without copying.
	 * <br>
	 * This can be useful to allow an EnumSet that was created with {@link #EnumSet()} to share a universe with other ESets.
	 *
	 * @param universe the universe of possible Enum items this can hold; almost always produced by {@code values()} on an Enum
	 */
	public void clearToUniverse (Enum<?>@Nullable [] universe) {
		size = 0;
		if (universe == null) {
			table = null;
		} else if(universe.length >>> 5 <= this.universe.length >>> 5) {
			if(table != null)
				Arrays.fill(table, 0);
		} else {
			table = new int[universe.length + 31 >>> 5];
		}
		this.universe = universe;
	}


	/**
	 * Removes all the elements from this set and can reset the universe of possible Enum items this can hold.
	 * The set will be empty after this call returns.
	 * This changes the universe of possible Enum items this can hold to match the Enum constants in {@code universe}.
	 * If {@code universe} is null, this resets this set to the state it would have after {@link #EnumSet()} was called.
	 * If the table this would need is the same size as or smaller than the current table (such as if {@code universe} is the same as
	 * the universe here), this will not allocate, but will still clear any items this holds and will set the universe to the given one.
	 * Otherwise, this allocates and uses a new table of a larger size, with nothing in it, and uses the given universe.
	 * This calls {@link Class#getEnumConstants()} if universe is non-null, which allocates a new array.
	 * <br>
	 * You may want to prefer calling {@link #clearToUniverse(Enum[])} (the overload that takes an array), because it can be used to
	 * share one universe array between many EnumSet instances. This overload, given a Class, has to call {@link Class#getEnumConstants()}
	 * and thus allocate a new array each time this is called.
	 *
	 * @param universe the Class of an Enum type that stores the universe of possible Enum items this can hold
	 */
	public void clearToUniverse (@Nullable Class<? extends Enum<?>> universe) {
		size = 0;
		if (universe == null) {
			table = null;
			this.universe = null;
		} else {
			Enum<?>[] cons = universe.getEnumConstants();
			if(cons.length >>> 5 <= this.universe.length >>> 5) {
				if(table != null)
					Arrays.fill(table, 0);
			} else {
				table = new int[cons.length + 31 >>> 5];
			}
			this.universe = cons;
		}
	}

	/**
	 * Returns the first ordinal equal to or greater than the {@code minOrdinal} of an Enum contained in the set.
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
			return universe[BitConversion.countTrailingZeros(bitsAtWord) + (word << 5)]; // countTrailingZeros() uses an intrinsic candidate, and should be extremely fast
		}
		for (word++; word < bitsLength; word++) {
			bitsAtWord = bits[word];
			if (bitsAtWord != 0) {
				return universe[BitConversion.countTrailingZeros(bitsAtWord) + (word << 5)];
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
			return universe[BitConversion.countTrailingZeros(bitsAtWord) + (word << 5)]; // countTrailingZeros() uses an intrinsic candidate, and should be extremely fast
		}
		for (word++; word < bitsLength; word++) {
			bitsAtWord = bits[word];
			if (bitsAtWord != 0) {
				return universe[BitConversion.countTrailingZeros(bitsAtWord) + (word << 5)];
			}
		}
		return null;
	}

	public static class ESetIterator implements Iterator<Enum<?>> {
		static private final int INDEX_ILLEGAL = -1, INDEX_ZERO = -1;

		public boolean hasNext;

		final EnumSet set;
		int nextIndex, currentIndex;
		boolean valid = true;

		public ESetIterator (EnumSet set) {
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
			set.remove(set.universe[currentIndex]);
			currentIndex = INDEX_ILLEGAL;
		}

		@Override
		public Enum<?> next () {
			if (!hasNext) {throw new NoSuchElementException();}
			if (!valid) {throw new RuntimeException("#iterator() cannot be used nested.");}
			currentIndex = nextIndex;
			findNextIndex();
			return set.universe[currentIndex];
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

	/**
	 * Builds an EnumSet that contains only the given element.
	 * @param item the one item to initialize the EnumSet with
	 * @return a new EnumSet containing {@code item}
	 */
	public static EnumSet with (Enum<?> item) {
		EnumSet set = new EnumSet();
		set.add(item);
		return set;
	}

	/**
	 * Builds an EnumSet that contains the unique elements from the given {@code array} or varargs.
	 * @param array an array or varargs of Enum constants, which should all have the same Enum type
	 * @return a new EnumSet containing each unique item from {@code array}
	 */
	public static EnumSet with (Enum<?>... array) {
		return new EnumSet(array);
	}

	/**
	 * Alias of {@link #with(Enum)} for compatibility.
	 * @param item the one item to initialize the EnumSet with
	 * @return a new EnumSet containing {@code item}
	 */
	public static EnumSet of (Enum<?> item) {
		return with(item);
	}

	/**
	 * Alias of {@link #with(Enum[])} for compatibility.
	 * @param array an array or varargs of Enum constants, which should all have the same Enum type
	 * @return a new EnumSet containing each unique item from {@code array}
	 */
	public static EnumSet of (Enum<?>... array) {
		return with(array);
	}

	/**
	 * Creates a new EnumSet using the given result of calling {@code values()} on an Enum type (the universe), but with no items
	 * initially stored in the set. You can reuse the universe between EnumSet instances as long as it is not modified.
	 * <br>
	 * This is the same as calling {@link #EnumSet(Enum[], boolean)}.
	 *
	 * @param universe almost always, the result of calling {@code values()} on an Enum type; used directly, not copied
	 * @return a new EnumSet with the specified universe of possible items, but none present in the set
	 */
	public static EnumSet noneOf(Enum<?>@Nullable [] universe) {
		return new EnumSet(universe, true);
	}

	/**
	 * Creates a new EnumSet using the given result of calling {@code values()} on an Enum type (the universe), and with all possible
	 * items initially stored in the set. You can reuse the universe between EnumSet instances as long as it is not modified.
	 *
	 * @param universe almost always, the result of calling {@code values()} on an Enum type; used directly, not copied
	 * @return a new EnumSet with the specified universe of possible items, and all of them present in the set
	 */
	public static EnumSet allOf(Enum<?>@Nullable [] universe) {
		if(universe == null) return new EnumSet();
		EnumSet coll = new EnumSet(universe, true);
		if(universe.length == 0) return coll;

		for (int i = 0; i < coll.table.length - 1; i++) {
			coll.table[i] = -1;
		}
		coll.table[coll.table.length - 1] = -1 >>> 32 - universe.length;
		coll.size = universe.length;
		return coll;
	}

	/**
	 * Creates a new EnumSet using the constants from the given Class (of an Enum type), but with no items initially
	 * stored in the set.
	 * <br>
	 * This is the same as calling {@link #EnumSet(Class)}.
	 *
	 * @param clazz the Class of any Enum type; you can get this from a constant with {@link Enum#getDeclaringClass()}
	 * @return a new EnumSet with the specified universe of possible items, but none present in the set
	 */
	public static EnumSet noneOf(@Nullable Class<? extends Enum<?>> clazz) {
		if(clazz == null)
			return new EnumSet();
		return new EnumSet(clazz.getEnumConstants(), true);
	}

	/**
	 * Creates a new EnumSet using the constants from the given Class (of an Enum type), and with all possible items initially
	 * stored in the set.
	 *
	 * @param clazz the Class of any Enum type; you can get this from a constant with {@link Enum#getDeclaringClass()}
	 * @return a new EnumSet with the specified universe of possible items, and all of them present in the set
	 */
	public static EnumSet allOf(@Nullable Class<? extends Enum<?>> clazz) {
		if(clazz == null)
			return new EnumSet();
		EnumSet coll = new EnumSet(clazz.getEnumConstants(), true);
		if(coll.universe.length == 0) return coll;

		for (int i = 0; i < coll.table.length - 1; i++) {
			coll.table[i] = -1;
		}
		coll.table[coll.table.length - 1] = -1 >>> 32 - coll.universe.length;
		coll.size = coll.universe.length;
		return coll;
	}

	/**
	 * Given another EnumSet, this creates a new EnumSet with the same universe as {@code other}, but with any elements present in other
	 * absent in the new set, and any elements absent in other present in the new set.
	 *
	 * @param other another EnumSet that this will copy
	 * @return a complemented copy of {@code other}
	 */
	public static EnumSet complementOf(EnumSet other) {
		if(other == null) return new EnumSet();
		EnumSet coll = new EnumSet(other);
		for (int i = 0; i < coll.table.length - 1; i++) {
			coll.table[i] ^= -1;
		}
		coll.table[coll.table.length - 1] ^= -1 >>> -coll.universe.length;
		coll.size = coll.universe.length - other.size;
		return coll;
	}

	/**
	 * Creates an EnumSet holding any Enum items in the given {@code contents}, which may be any Collection of Enum, including another
	 * EnumSet. If given an EnumSet, this will copy its Enum universe and other information even if it is empty.
	 * @param contents a Collection of Enum values, which may be another EnumSet
	 * @return a new EnumSet containing the unique items in contents
	 */
	public static EnumSet copyOf(Collection<? extends Enum<?>> contents) {
		if(contents == null) throw new NullPointerException("Cannot copy a null EnumSet.");
		return new EnumSet(contents);
	}

	/**
	 * Creates an EnumSet holding Enum items between the ordinals of {@code start} and {@code end}. If the ordinal of end is less than
	 * the ordinal of start, this throws an {@link IllegalArgumentException}.
	 * If start and end are the same, this just inserts that one Enum.
	 *
	 * @param start the starting inclusive Enum to insert
	 * @param end the ending inclusive Enum to insert
	 * @return a new EnumSet containing start, end, and any Enum constants with ordinals between them
	 * @param <E> the shared Enum type of both start and end
	 * @throws IllegalArgumentException if the {@link Enum#ordinal() ordinal} of end is less than the ordinal of start
	 */
	public static  <E extends Enum<E>> EnumSet range(Enum<E> start, Enum<E> end) {
		final int mn = start.ordinal();
		final int mx = end.ordinal();
		if(mx < mn) throw new IllegalArgumentException("The ordinal of " + end + " (" + mx +
			") must be at least equal to the ordinal of " + start + " ("+mn+")");
		final int upperMin = mn >>> 5;
		final int upperMax = mx >>> 5;
		EnumSet coll = new EnumSet();
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

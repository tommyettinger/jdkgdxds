package com.github.tommyettinger.ds;

import com.github.tommyettinger.ds.support.function.FloatConsumer;
import com.github.tommyettinger.ds.support.util.FloatIterator;

import java.util.PrimitiveIterator;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

/**
 * Analogous to {@link java.util.Collection} but for a primitive type, this is built around
 * {@link PrimitiveIterator} (or more typically, one of the nested interfaces here, like
 * {@link OfInt}, is built around one of PrimitiveIterator's nested interfaces, like
 * {@link PrimitiveIterator.OfInt}). This is not necessarily a modifiable collection. The
 * nested interfaces define most of the actually useful operations, and you will probably
 * never use PrimitiveCollection directly.
 */
public interface PrimitiveCollection<T, T_CONS> {
	int size ();

	default boolean isEmpty () {
		return size() == 0;
	}

	PrimitiveIterator<T, T_CONS> iterator ();

	void clear ();

	@Override
	int hashCode ();

	@Override
	boolean equals (Object other);

	interface OfInt extends PrimitiveCollection<Integer, IntConsumer> {
		boolean add (int item);

		boolean remove (int item);

		boolean contains (int item);

		default boolean addAll (OfInt other) {
			PrimitiveIterator.OfInt it = other.iterator();
			boolean changed = false;
			while (it.hasNext()) {
				changed |= add(it.nextInt());
			}
			return changed;
		}

		default boolean removeAll (OfInt other) {
			PrimitiveIterator.OfInt it = other.iterator();
			boolean changed = false;
			while (it.hasNext()) {
				changed |= remove(it.nextInt());
			}
			return changed;
		}

		default boolean containsAll (OfInt other) {
			PrimitiveIterator.OfInt it = other.iterator();
			boolean has = true;
			while (it.hasNext()) {
				has &= contains(it.nextInt());
			}
			return has;
		}

		default boolean retainAll (OfInt other) {
			boolean changed = false;
			PrimitiveIterator.OfInt it = iterator();
			while (it.hasNext()) {
				if (!other.contains(it.nextInt())) {
					it.remove();
					changed = true;
				}
			}
			return changed;
		}

		@Override
		PrimitiveIterator.OfInt iterator ();

		/**
		 * Performs the given action for each element of the {@code PrimitiveCollection.OfInt}
		 * until all elements have been processed or the action throws an
		 * exception.  Actions are performed in the order of iteration, if that
		 * order is specified.  Exceptions thrown by the action are relayed to the
		 * caller.
		 *
		 * @param action The action to be performed for each element
		 */
		default void forEach(IntConsumer action) {
			PrimitiveIterator.OfInt it = iterator();
			while (it.hasNext())
				action.accept(it.nextInt());
		}
	}

	interface OfLong extends PrimitiveCollection<Long, LongConsumer> {
		boolean add (long item);

		boolean remove (long item);

		boolean contains (long item);

		default boolean addAll (OfLong other) {
			PrimitiveIterator.OfLong it = other.iterator();
			boolean changed = false;
			while (it.hasNext()) {
				changed |= add(it.nextLong());
			}
			return changed;
		}

		default boolean removeAll (OfLong other) {
			PrimitiveIterator.OfLong it = other.iterator();
			boolean changed = false;
			while (it.hasNext()) {
				changed |= remove(it.nextLong());
			}
			return changed;
		}

		default boolean containsAll (OfLong other) {
			PrimitiveIterator.OfLong it = other.iterator();
			boolean has = true;
			while (it.hasNext()) {
				has &= contains(it.nextLong());
			}
			return has;
		}

		default boolean retainAll (OfLong other) {
			boolean changed = false;
			PrimitiveIterator.OfLong it = iterator();
			while (it.hasNext()) {
				if (!other.contains(it.nextLong())) {
					it.remove();
					changed = true;
				}
			}
			return changed;
		}

		@Override
		PrimitiveIterator.OfLong iterator ();

		/**
		 * Performs the given action for each element of the {@code PrimitiveCollection.OfLong}
		 * until all elements have been processed or the action throws an
		 * exception.  Actions are performed in the order of iteration, if that
		 * order is specified.  Exceptions thrown by the action are relayed to the
		 * caller.
		 *
		 * @param action The action to be performed for each element
		 */
		default void forEach(LongConsumer action) {
			PrimitiveIterator.OfLong it = iterator();
			while (it.hasNext())
				action.accept(it.nextLong());
		}
	}

	interface OfFloat extends PrimitiveCollection<Float, FloatConsumer> {
		boolean add (float item);

		boolean remove (float item);

		boolean contains (float item);

		default boolean addAll (OfFloat other) {
			FloatIterator it = other.iterator();
			boolean changed = false;
			while (it.hasNext()) {
				changed |= add(it.nextFloat());
			}
			return changed;
		}

		default boolean removeAll (OfFloat other) {
			FloatIterator it = other.iterator();
			boolean changed = false;
			while (it.hasNext()) {
				changed |= remove(it.nextFloat());
			}
			return changed;
		}

		default boolean containsAll (OfFloat other) {
			FloatIterator it = other.iterator();
			boolean has = true;
			while (it.hasNext()) {
				has &= contains(it.nextFloat());
			}
			return has;
		}

		default boolean retainAll (OfFloat other) {
			boolean changed = false;
			FloatIterator it = iterator();
			while (it.hasNext()) {
				if (!other.contains(it.nextFloat())) {
					it.remove();
					changed = true;
				}
			}
			return changed;
		}

		@Override
		FloatIterator iterator ();

		/**
		 * Performs the given action for each element of the {@code PrimitiveCollection.OfFloat}
		 * until all elements have been processed or the action throws an
		 * exception.  Actions are performed in the order of iteration, if that
		 * order is specified.  Exceptions thrown by the action are relayed to the
		 * caller.
		 *
		 * @param action The action to be performed for each element
		 */
		default void forEach(FloatConsumer action) {
			FloatIterator it = iterator();
			while (it.hasNext())
				action.accept(it.nextFloat());
		}
	}
}

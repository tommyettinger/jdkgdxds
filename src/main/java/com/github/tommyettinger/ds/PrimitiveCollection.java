package com.github.tommyettinger.ds;

import com.github.tommyettinger.ds.support.function.BooleanConsumer;
import com.github.tommyettinger.ds.support.function.BooleanPredicate;
import com.github.tommyettinger.ds.support.function.ByteConsumer;
import com.github.tommyettinger.ds.support.function.BytePredicate;
import com.github.tommyettinger.ds.support.function.CharConsumer;
import com.github.tommyettinger.ds.support.function.CharPredicate;
import com.github.tommyettinger.ds.support.function.FloatConsumer;
import com.github.tommyettinger.ds.support.function.FloatPredicate;
import com.github.tommyettinger.ds.support.function.ShortConsumer;
import com.github.tommyettinger.ds.support.function.ShortPredicate;
import com.github.tommyettinger.ds.support.util.BooleanIterator;
import com.github.tommyettinger.ds.support.util.ByteIterator;
import com.github.tommyettinger.ds.support.util.CharIterator;
import com.github.tommyettinger.ds.support.util.FloatIterator;
import com.github.tommyettinger.ds.support.util.ShortIterator;

import java.util.PrimitiveIterator;
import java.util.function.DoubleConsumer;
import java.util.function.DoublePredicate;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;

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

	default boolean notEmpty () {
		return size() != 0;
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

		/**
		 * Removes all of the elements of this collection that satisfy the given
		 * predicate.  Errors or runtime exceptions thrown during iteration or by
		 * the predicate are relayed to the caller.
		 *
		 * @implSpec
		 * The default implementation traverses all elements of the collection using
		 * its {@link #iterator()}.  Each matching element is removed using
		 * {@link PrimitiveIterator#remove()}.  If the collection's iterator does not
		 * support removal then an {@code UnsupportedOperationException} will be
		 * thrown on the first matching element.
		 *
		 * @param filter a predicate which returns {@code true} for elements to be
		 *        removed
		 * @return {@code true} if any elements were removed
		 * @throws UnsupportedOperationException if elements cannot be removed
		 *         from this collection.  Implementations may throw this exception if a
		 *         matching element cannot be removed or if, in general, removal is not
		 *         supported.
		 */
		default boolean removeIf(IntPredicate filter) {
			boolean removed = false;
			final PrimitiveIterator.OfInt each = iterator();
			while (each.hasNext()) {
				if (filter.test(each.nextInt())) {
					each.remove();
					removed = true;
				}
			}
			return removed;
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
		 * Allocates a new int array with exactly {@link #size()} items, fills it with the
		 * contents of this PrimitiveCollection, and returns it.
		 * @return a new int array
		 */
		default int[] toArray () {
			final int sz = size();
			int[] receiver = new int[sz];
			PrimitiveIterator.OfInt it = iterator();
			int i = 0;
			while (it.hasNext())
				receiver[i++] = it.nextInt();
			return receiver;
		}
		/**
		 * Fills the given array with the entire contents of this PrimitiveCollection, up to
		 * {@link #size()} items, or if receiver is not large enough, then this allocates a new
		 * int array with {@link #size()} items and returns that.
		 * @param receiver an int array that will be filled with the items from this, if possible
		 * @return {@code receiver}, if it was modified, or a new int array otherwise
		 */
		default int[] toArray (int[] receiver){
			final int sz = size();
			if(receiver.length < sz)
				receiver = new int[sz];
			PrimitiveIterator.OfInt it = iterator();
			int i = 0;
			while (it.hasNext())
				receiver[i++] = it.nextInt();
			return receiver;
		}

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

		/**
		 * Attempts to get the first item in this PrimitiveCollection, where "first" is only
		 * defined meaningfully if this type is ordered. Many times, this applies to a class
		 * that is not ordered, and in those cases it can get an arbitrary item, and that item
		 * is permitted to be different for different calls to first().
		 * <br>
		 * This is useful for cases where you would normally be able to call something like
		 * {@link java.util.List#get(int)} to get an item, any item, from a collection, but
		 * whatever class you're using doesn't necessarily provide a get(), first(), peek(),
		 * or similar method.
		 * <br>
		 * The default implementation uses {@link #iterator()}, tries to get the first item,
		 * or throws an IllegalStateException if this is empty.
		 * @throws IllegalStateException if this is empty
		 * @return the first item in this PrimitiveCollection, as produced by {@link #iterator()}
		 */
		default int first() {
			PrimitiveIterator.OfInt it = iterator();
			if(it.hasNext()) return it.nextInt();
			throw new IllegalStateException("Can't get the first() item of an empty PrimitiveCollection.");
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

		/**
		 * Removes all of the elements of this collection that satisfy the given
		 * predicate.  Errors or runtime exceptions thrown during iteration or by
		 * the predicate are relayed to the caller.
		 *
		 * @implSpec
		 * The default implementation traverses all elements of the collection using
		 * its {@link #iterator()}.  Each matching element is removed using
		 * {@link PrimitiveIterator#remove()}.  If the collection's iterator does not
		 * support removal then an {@code UnsupportedOperationException} will be
		 * thrown on the first matching element.
		 *
		 * @param filter a predicate which returns {@code true} for elements to be
		 *        removed
		 * @return {@code true} if any elements were removed
		 * @throws UnsupportedOperationException if elements cannot be removed
		 *         from this collection.  Implementations may throw this exception if a
		 *         matching element cannot be removed or if, in general, removal is not
		 *         supported.
		 */
		default boolean removeIf(LongPredicate filter) {
			boolean removed = false;
			final PrimitiveIterator.OfLong each = iterator();
			while (each.hasNext()) {
				if (filter.test(each.nextLong())) {
					each.remove();
					removed = true;
				}
			}
			return removed;
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
		 * Allocates a new long array with exactly {@link #size()} items, fills it with the
		 * contents of this PrimitiveCollection, and returns it.
		 * @return a new long array
		 */
		default long[] toArray () {
			final int sz = size();
			long[] receiver = new long[sz];
			PrimitiveIterator.OfLong it = iterator();
			int i = 0;
			while (it.hasNext())
				receiver[i++] = it.nextLong();
			return receiver;
		}
		/**
		 * Fills the given array with the entire contents of this PrimitiveCollection, up to
		 * {@link #size()} items, or if receiver is not large enough, then this allocates a new
		 * long array with {@link #size()} items and returns that.
		 * @param receiver a long array that will be filled with the items from this, if possible
		 * @return {@code receiver}, if it was modified, or a new long array otherwise
		 */
		default long[] toArray (long[] receiver){
			final int sz = size();
			if(receiver.length < sz)
				receiver = new long[sz];
			PrimitiveIterator.OfLong it = iterator();
			int i = 0;
			while (it.hasNext())
				receiver[i++] = it.nextLong();
			return receiver;
		}

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

		/**
		 * Attempts to get the first item in this PrimitiveCollection, where "first" is only
		 * defined meaningfully if this type is ordered. Many times, this applies to a class
		 * that is not ordered, and in those cases it can get an arbitrary item, and that item
		 * is permitted to be different for different calls to first().
		 * <br>
		 * This is useful for cases where you would normally be able to call something like
		 * {@link java.util.List#get(int)} to get an item, any item, from a collection, but
		 * whatever class you're using doesn't necessarily provide a get(), first(), peek(),
		 * or similar method.
		 * <br>
		 * The default implementation uses {@link #iterator()}, tries to get the first item,
		 * or throws an IllegalStateException if this is empty.
		 * @throws IllegalStateException if this is empty
		 * @return the first item in this PrimitiveCollection, as produced by {@link #iterator()}
		 */
		default long first() {
			PrimitiveIterator.OfLong it = iterator();
			if(it.hasNext()) return it.nextLong();
			throw new IllegalStateException("Can't get the first() item of an empty PrimitiveCollection.");
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

		/**
		 * Removes all of the elements of this collection that satisfy the given
		 * predicate.  Errors or runtime exceptions thrown during iteration or by
		 * the predicate are relayed to the caller.
		 *
		 * @implSpec
		 * The default implementation traverses all elements of the collection using
		 * its {@link #iterator()}.  Each matching element is removed using
		 * {@link PrimitiveIterator#remove()}.  If the collection's iterator does not
		 * support removal then an {@code UnsupportedOperationException} will be
		 * thrown on the first matching element.
		 *
		 * @param filter a predicate which returns {@code true} for elements to be
		 *        removed
		 * @return {@code true} if any elements were removed
		 * @throws UnsupportedOperationException if elements cannot be removed
		 *         from this collection.  Implementations may throw this exception if a
		 *         matching element cannot be removed or if, in general, removal is not
		 *         supported.
		 */
		default boolean removeIf(FloatPredicate filter) {
			boolean removed = false;
			final FloatIterator each = iterator();
			while (each.hasNext()) {
				if (filter.test(each.nextFloat())) {
					each.remove();
					removed = true;
				}
			}
			return removed;
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
		 * Allocates a new float array with exactly {@link #size()} items, fills it with the
		 * contents of this PrimitiveCollection, and returns it.
		 * @return a new float array
		 */
		default float[] toArray () {
			final int sz = size();
			float[] receiver = new float[sz];
			FloatIterator it = iterator();
			int i = 0;
			while (it.hasNext())
				receiver[i++] = it.nextFloat();
			return receiver;
		}
		/**
		 * Fills the given array with the entire contents of this PrimitiveCollection, up to
		 * {@link #size()} items, or if receiver is not large enough, then this allocates a new
		 * float array with {@link #size()} items and returns that.
		 * @param receiver a float array that will be filled with the items from this, if possible
		 * @return {@code receiver}, if it was modified, or a new float array otherwise
		 */
		default float[] toArray (float[] receiver){
			final int sz = size();
			if(receiver.length < sz)
				receiver = new float[sz];
			FloatIterator it = iterator();
			int i = 0;
			while (it.hasNext())
				receiver[i++] = it.nextFloat();
			return receiver;
		}

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

		/**
		 * Attempts to get the first item in this PrimitiveCollection, where "first" is only
		 * defined meaningfully if this type is ordered. Many times, this applies to a class
		 * that is not ordered, and in those cases it can get an arbitrary item, and that item
		 * is permitted to be different for different calls to first().
		 * <br>
		 * This is useful for cases where you would normally be able to call something like
		 * {@link java.util.List#get(int)} to get an item, any item, from a collection, but
		 * whatever class you're using doesn't necessarily provide a get(), first(), peek(),
		 * or similar method.
		 * <br>
		 * The default implementation uses {@link #iterator()}, tries to get the first item,
		 * or throws an IllegalStateException if this is empty.
		 * @throws IllegalStateException if this is empty
		 * @return the first item in this PrimitiveCollection, as produced by {@link #iterator()}
		 */
		default float first() {
			FloatIterator it = iterator();
			if(it.hasNext()) return it.nextFloat();
			throw new IllegalStateException("Can't get the first() item of an empty PrimitiveCollection.");
		}
	}

	interface OfDouble extends PrimitiveCollection<Double, DoubleConsumer> {
		boolean add (double item);

		boolean remove (double item);

		boolean contains (double item);

		default boolean addAll (OfDouble other) {
			PrimitiveIterator.OfDouble it = other.iterator();
			boolean changed = false;
			while (it.hasNext()) {
				changed |= add(it.nextDouble());
			}
			return changed;
		}

		default boolean removeAll (OfDouble other) {
			PrimitiveIterator.OfDouble it = other.iterator();
			boolean changed = false;
			while (it.hasNext()) {
				changed |= remove(it.nextDouble());
			}
			return changed;
		}

		default boolean containsAll (OfDouble other) {
			PrimitiveIterator.OfDouble it = other.iterator();
			boolean has = true;
			while (it.hasNext()) {
				has &= contains(it.nextDouble());
			}
			return has;
		}

		/**
		 * Removes all of the elements of this collection that satisfy the given
		 * predicate.  Errors or runtime exceptions thrown during iteration or by
		 * the predicate are relayed to the caller.
		 *
		 * @implSpec
		 * The default implementation traverses all elements of the collection using
		 * its {@link #iterator()}.  Each matching element is removed using
		 * {@link PrimitiveIterator#remove()}.  If the collection's iterator does not
		 * support removal then an {@code UnsupportedOperationException} will be
		 * thrown on the first matching element.
		 *
		 * @param filter a predicate which returns {@code true} for elements to be
		 *        removed
		 * @return {@code true} if any elements were removed
		 * @throws UnsupportedOperationException if elements cannot be removed
		 *         from this collection.  Implementations may throw this exception if a
		 *         matching element cannot be removed or if, in general, removal is not
		 *         supported.
		 */
		default boolean removeIf(DoublePredicate filter) {
			boolean removed = false;
			final PrimitiveIterator.OfDouble each = iterator();
			while (each.hasNext()) {
				if (filter.test(each.nextDouble())) {
					each.remove();
					removed = true;
				}
			}
			return removed;
		}

		default boolean retainAll (OfDouble other) {
			boolean changed = false;
			PrimitiveIterator.OfDouble it = iterator();
			while (it.hasNext()) {
				if (!other.contains(it.nextDouble())) {
					it.remove();
					changed = true;
				}
			}
			return changed;
		}

		@Override
		PrimitiveIterator.OfDouble iterator ();

		/**
		 * Allocates a new double array with exactly {@link #size()} items, fills it with the
		 * contents of this PrimitiveCollection, and returns it.
		 * @return a new double array
		 */
		default double[] toArray () {
			final int sz = size();
			double[] receiver = new double[sz];
			PrimitiveIterator.OfDouble it = iterator();
			int i = 0;
			while (it.hasNext())
				receiver[i++] = it.nextDouble();
			return receiver;
		}
		/**
		 * Fills the given array with the entire contents of this PrimitiveCollection, up to
		 * {@link #size()} items, or if receiver is not large enough, then this allocates a new
		 * double array with {@link #size()} items and returns that.
		 * @param receiver a double array that will be filled with the items from this, if possible
		 * @return {@code receiver}, if it was modified, or a new double array otherwise
		 */
		default double[] toArray (double[] receiver){
			final int sz = size();
			if(receiver.length < sz)
				receiver = new double[sz];
			PrimitiveIterator.OfDouble it = iterator();
			int i = 0;
			while (it.hasNext())
				receiver[i++] = it.nextDouble();
			return receiver;
		}

		/**
		 * Performs the given action for each element of the {@code PrimitiveCollection.OfDouble}
		 * until all elements have been processed or the action throws an
		 * exception.  Actions are performed in the order of iteration, if that
		 * order is specified.  Exceptions thrown by the action are relayed to the
		 * caller.
		 *
		 * @param action The action to be performed for each element
		 */
		default void forEach(DoubleConsumer action) {
			PrimitiveIterator.OfDouble it = iterator();
			while (it.hasNext())
				action.accept(it.nextDouble());
		}

		/**
		 * Attempts to get the first item in this PrimitiveCollection, where "first" is only
		 * defined meaningfully if this type is ordered. Many times, this applies to a class
		 * that is not ordered, and in those cases it can get an arbitrary item, and that item
		 * is permitted to be different for different calls to first().
		 * <br>
		 * This is useful for cases where you would normally be able to call something like
		 * {@link java.util.List#get(int)} to get an item, any item, from a collection, but
		 * whatever class you're using doesn't necessarily provide a get(), first(), peek(),
		 * or similar method.
		 * <br>
		 * The default implementation uses {@link #iterator()}, tries to get the first item,
		 * or throws an IllegalStateException if this is empty.
		 * @throws IllegalStateException if this is empty
		 * @return the first item in this PrimitiveCollection, as produced by {@link #iterator()}
		 */
		default double first() {
			PrimitiveIterator.OfDouble it = iterator();
			if(it.hasNext()) return it.nextDouble();
			throw new IllegalStateException("Can't get the first() item of an empty PrimitiveCollection.");
		}
	}

	interface OfShort extends PrimitiveCollection<Short, ShortConsumer> {
		boolean add (short item);

		boolean remove (short item);

		boolean contains (short item);

		default boolean addAll (OfShort other) {
			ShortIterator it = other.iterator();
			boolean changed = false;
			while (it.hasNext()) {
				changed |= add(it.nextShort());
			}
			return changed;
		}

		default boolean removeAll (OfShort other) {
			ShortIterator it = other.iterator();
			boolean changed = false;
			while (it.hasNext()) {
				changed |= remove(it.nextShort());
			}
			return changed;
		}

		default boolean containsAll (OfShort other) {
			ShortIterator it = other.iterator();
			boolean has = true;
			while (it.hasNext()) {
				has &= contains(it.nextShort());
			}
			return has;
		}

		/**
		 * Removes all of the elements of this collection that satisfy the given
		 * predicate.  Errors or runtime exceptions thrown during iteration or by
		 * the predicate are relayed to the caller.
		 *
		 * @implSpec
		 * The default implementation traverses all elements of the collection using
		 * its {@link #iterator()}.  Each matching element is removed using
		 * {@link PrimitiveIterator#remove()}.  If the collection's iterator does not
		 * support removal then an {@code UnsupportedOperationException} will be
		 * thrown on the first matching element.
		 *
		 * @param filter a predicate which returns {@code true} for elements to be
		 *        removed
		 * @return {@code true} if any elements were removed
		 * @throws UnsupportedOperationException if elements cannot be removed
		 *         from this collection.  Implementations may throw this exception if a
		 *         matching element cannot be removed or if, in general, removal is not
		 *         supported.
		 */
		default boolean removeIf(ShortPredicate filter) {
			boolean removed = false;
			final ShortIterator each = iterator();
			while (each.hasNext()) {
				if (filter.test(each.nextShort())) {
					each.remove();
					removed = true;
				}
			}
			return removed;
		}

		default boolean retainAll (OfShort other) {
			boolean changed = false;
			ShortIterator it = iterator();
			while (it.hasNext()) {
				if (!other.contains(it.nextShort())) {
					it.remove();
					changed = true;
				}
			}
			return changed;
		}

		@Override
		ShortIterator iterator ();

		/**
		 * Allocates a new short array with exactly {@link #size()} items, fills it with the
		 * contents of this PrimitiveCollection, and returns it.
		 * @return a new short array
		 */
		default short[] toArray () {
			final int sz = size();
			short[] receiver = new short[sz];
			ShortIterator it = iterator();
			int i = 0;
			while (it.hasNext())
				receiver[i++] = it.nextShort();
			return receiver;
		}
		/**
		 * Fills the given array with the entire contents of this PrimitiveCollection, up to
		 * {@link #size()} items, or if receiver is not large enough, then this allocates a new
		 * short array with {@link #size()} items and returns that.
		 * @param receiver a short array that will be filled with the items from this, if possible
		 * @return {@code receiver}, if it was modified, or a new short array otherwise
		 */
		default short[] toArray (short[] receiver){
			final int sz = size();
			if(receiver.length < sz)
				receiver = new short[sz];
			ShortIterator it = iterator();
			int i = 0;
			while (it.hasNext())
				receiver[i++] = it.nextShort();
			return receiver;
		}

		/**
		 * Performs the given action for each element of the {@code PrimitiveCollection.OfShort}
		 * until all elements have been processed or the action throws an
		 * exception.  Actions are performed in the order of iteration, if that
		 * order is specified.  Exceptions thrown by the action are relayed to the
		 * caller.
		 *
		 * @param action The action to be performed for each element
		 */
		default void forEach(ShortConsumer action) {
			ShortIterator it = iterator();
			while (it.hasNext())
				action.accept(it.nextShort());
		}

		/**
		 * Attempts to get the first item in this PrimitiveCollection, where "first" is only
		 * defined meaningfully if this type is ordered. Many times, this applies to a class
		 * that is not ordered, and in those cases it can get an arbitrary item, and that item
		 * is permitted to be different for different calls to first().
		 * <br>
		 * This is useful for cases where you would normally be able to call something like
		 * {@link java.util.List#get(int)} to get an item, any item, from a collection, but
		 * whatever class you're using doesn't necessarily provide a get(), first(), peek(),
		 * or similar method.
		 * <br>
		 * The default implementation uses {@link #iterator()}, tries to get the first item,
		 * or throws an IllegalStateException if this is empty.
		 * @throws IllegalStateException if this is empty
		 * @return the first item in this PrimitiveCollection, as produced by {@link #iterator()}
		 */
		default short first() {
			ShortIterator it = iterator();
			if(it.hasNext()) return it.nextShort();
			throw new IllegalStateException("Can't get the first() item of an empty PrimitiveCollection.");
		}
	}

	interface OfByte extends PrimitiveCollection<Byte, ByteConsumer> {
		boolean add (byte item);

		boolean remove (byte item);

		boolean contains (byte item);

		default boolean addAll (OfByte other) {
			ByteIterator it = other.iterator();
			boolean changed = false;
			while (it.hasNext()) {
				changed |= add(it.nextByte());
			}
			return changed;
		}

		default boolean removeAll (OfByte other) {
			ByteIterator it = other.iterator();
			boolean changed = false;
			while (it.hasNext()) {
				changed |= remove(it.nextByte());
			}
			return changed;
		}

		default boolean containsAll (OfByte other) {
			ByteIterator it = other.iterator();
			boolean has = true;
			while (it.hasNext()) {
				has &= contains(it.nextByte());
			}
			return has;
		}

		/**
		 * Removes all of the elements of this collection that satisfy the given
		 * predicate.  Errors or runtime exceptions thrown during iteration or by
		 * the predicate are relayed to the caller.
		 *
		 * @implSpec
		 * The default implementation traverses all elements of the collection using
		 * its {@link #iterator()}.  Each matching element is removed using
		 * {@link PrimitiveIterator#remove()}.  If the collection's iterator does not
		 * support removal then an {@code UnsupportedOperationException} will be
		 * thrown on the first matching element.
		 *
		 * @param filter a predicate which returns {@code true} for elements to be
		 *        removed
		 * @return {@code true} if any elements were removed
		 * @throws UnsupportedOperationException if elements cannot be removed
		 *         from this collection.  Implementations may throw this exception if a
		 *         matching element cannot be removed or if, in general, removal is not
		 *         supported.
		 */
		default boolean removeIf(BytePredicate filter) {
			boolean removed = false;
			final ByteIterator each = iterator();
			while (each.hasNext()) {
				if (filter.test(each.nextByte())) {
					each.remove();
					removed = true;
				}
			}
			return removed;
		}

		default boolean retainAll (OfByte other) {
			boolean changed = false;
			ByteIterator it = iterator();
			while (it.hasNext()) {
				if (!other.contains(it.nextByte())) {
					it.remove();
					changed = true;
				}
			}
			return changed;
		}

		@Override
		ByteIterator iterator ();

		/**
		 * Allocates a new byte array with exactly {@link #size()} items, fills it with the
		 * contents of this PrimitiveCollection, and returns it.
		 * @return a new byte array
		 */
		default byte[] toArray () {
			final int sz = size();
			byte[] receiver = new byte[sz];
			ByteIterator it = iterator();
			int i = 0;
			while (it.hasNext())
				receiver[i++] = it.nextByte();
			return receiver;
		}
		/**
		 * Fills the given array with the entire contents of this PrimitiveCollection, up to
		 * {@link #size()} items, or if receiver is not large enough, then this allocates a new
		 * byte array with {@link #size()} items and returns that.
		 * @param receiver a byte array that will be filled with the items from this, if possible
		 * @return {@code receiver}, if it was modified, or a new byte array otherwise
		 */
		default byte[] toArray (byte[] receiver){
			final int sz = size();
			if(receiver.length < sz)
				receiver = new byte[sz];
			ByteIterator it = iterator();
			int i = 0;
			while (it.hasNext())
				receiver[i++] = it.nextByte();
			return receiver;
		}

		/**
		 * Performs the given action for each element of the {@code PrimitiveCollection.OfByte}
		 * until all elements have been processed or the action throws an
		 * exception.  Actions are performed in the order of iteration, if that
		 * order is specified.  Exceptions thrown by the action are relayed to the
		 * caller.
		 *
		 * @param action The action to be performed for each element
		 */
		default void forEach(ByteConsumer action) {
			ByteIterator it = iterator();
			while (it.hasNext())
				action.accept(it.nextByte());
		}

		/**
		 * Attempts to get the first item in this PrimitiveCollection, where "first" is only
		 * defined meaningfully if this type is ordered. Many times, this applies to a class
		 * that is not ordered, and in those cases it can get an arbitrary item, and that item
		 * is permitted to be different for different calls to first().
		 * <br>
		 * This is useful for cases where you would normally be able to call something like
		 * {@link java.util.List#get(int)} to get an item, any item, from a collection, but
		 * whatever class you're using doesn't necessarily provide a get(), first(), peek(),
		 * or similar method.
		 * <br>
		 * The default implementation uses {@link #iterator()}, tries to get the first item,
		 * or throws an IllegalStateException if this is empty.
		 * @throws IllegalStateException if this is empty
		 * @return the first item in this PrimitiveCollection, as produced by {@link #iterator()}
		 */
		default byte first() {
			ByteIterator it = iterator();
			if(it.hasNext()) return it.nextByte();
			throw new IllegalStateException("Can't get the first() item of an empty PrimitiveCollection.");
		}
	}

	interface OfChar extends PrimitiveCollection<Character, CharConsumer> {
		boolean add (char item);

		boolean remove (char item);

		boolean contains (char item);

		default boolean addAll (OfChar other) {
			CharIterator it = other.iterator();
			boolean changed = false;
			while (it.hasNext()) {
				changed |= add(it.nextChar());
			}
			return changed;
		}

		default boolean removeAll (OfChar other) {
			CharIterator it = other.iterator();
			boolean changed = false;
			while (it.hasNext()) {
				changed |= remove(it.nextChar());
			}
			return changed;
		}

		default boolean containsAll (OfChar other) {
			CharIterator it = other.iterator();
			boolean has = true;
			while (it.hasNext()) {
				has &= contains(it.nextChar());
			}
			return has;
		}

		/**
		 * Removes all of the elements of this collection that satisfy the given
		 * predicate.  Errors or runtime exceptions thrown during iteration or by
		 * the predicate are relayed to the caller.
		 *
		 * @implSpec
		 * The default implementation traverses all elements of the collection using
		 * its {@link #iterator()}.  Each matching element is removed using
		 * {@link PrimitiveIterator#remove()}.  If the collection's iterator does not
		 * support removal then an {@code UnsupportedOperationException} will be
		 * thrown on the first matching element.
		 *
		 * @param filter a predicate which returns {@code true} for elements to be
		 *        removed
		 * @return {@code true} if any elements were removed
		 * @throws UnsupportedOperationException if elements cannot be removed
		 *         from this collection.  Implementations may throw this exception if a
		 *         matching element cannot be removed or if, in general, removal is not
		 *         supported.
		 */
		default boolean removeIf(CharPredicate filter) {
			boolean removed = false;
			final CharIterator each = iterator();
			while (each.hasNext()) {
				if (filter.test(each.nextChar())) {
					each.remove();
					removed = true;
				}
			}
			return removed;
		}

		default boolean retainAll (OfChar other) {
			boolean changed = false;
			CharIterator it = iterator();
			while (it.hasNext()) {
				if (!other.contains(it.nextChar())) {
					it.remove();
					changed = true;
				}
			}
			return changed;
		}

		@Override
		CharIterator iterator ();

		/**
		 * Allocates a new char array with exactly {@link #size()} items, fills it with the
		 * contents of this PrimitiveCollection, and returns it.
		 * @return a new char array
		 */
		default char[] toArray () {
			final int sz = size();
			char[] receiver = new char[sz];
			CharIterator it = iterator();
			int i = 0;
			while (it.hasNext())
				receiver[i++] = it.nextChar();
			return receiver;
		}
		/**
		 * Fills the given array with the entire contents of this PrimitiveCollection, up to
		 * {@link #size()} items, or if receiver is not large enough, then this allocates a new
		 * char array with {@link #size()} items and returns that.
		 * @param receiver a char array that will be filled with the items from this, if possible
		 * @return {@code receiver}, if it was modified, or a new char array otherwise
		 */
		default char[] toArray (char[] receiver){
			final int sz = size();
			if(receiver.length < sz)
				receiver = new char[sz];
			CharIterator it = iterator();
			int i = 0;
			while (it.hasNext())
				receiver[i++] = it.nextChar();
			return receiver;
		}

		/**
		 * Performs the given action for each element of the {@code PrimitiveCollection.OfChar}
		 * until all elements have been processed or the action throws an
		 * exception.  Actions are performed in the order of iteration, if that
		 * order is specified.  Exceptions thrown by the action are relayed to the
		 * caller.
		 *
		 * @param action The action to be performed for each element
		 */
		default void forEach(CharConsumer action) {
			CharIterator it = iterator();
			while (it.hasNext())
				action.accept(it.nextChar());
		}

		/**
		 * Attempts to get the first item in this PrimitiveCollection, where "first" is only
		 * defined meaningfully if this type is ordered. Many times, this applies to a class
		 * that is not ordered, and in those cases it can get an arbitrary item, and that item
		 * is permitted to be different for different calls to first().
		 * <br>
		 * This is useful for cases where you would normally be able to call something like
		 * {@link java.util.List#get(int)} to get an item, any item, from a collection, but
		 * whatever class you're using doesn't necessarily provide a get(), first(), peek(),
		 * or similar method.
		 * <br>
		 * The default implementation uses {@link #iterator()}, tries to get the first item,
		 * or throws an IllegalStateException if this is empty.
		 * @throws IllegalStateException if this is empty
		 * @return the first item in this PrimitiveCollection, as produced by {@link #iterator()}
		 */
		default char first() {
			CharIterator it = iterator();
			if(it.hasNext()) return it.nextChar();
			throw new IllegalStateException("Can't get the first() item of an empty PrimitiveCollection.");
		}
	}

	interface OfBoolean extends PrimitiveCollection<Boolean, BooleanConsumer> {
		boolean add (boolean item);

		boolean remove (boolean item);

		boolean contains (boolean item);

		default boolean addAll (OfBoolean other) {
			BooleanIterator it = other.iterator();
			boolean changed = false;
			while (it.hasNext()) {
				changed |= add(it.nextBoolean());
			}
			return changed;
		}

		default boolean removeAll (OfBoolean other) {
			BooleanIterator it = other.iterator();
			boolean changed = false;
			while (it.hasNext()) {
				changed |= remove(it.nextBoolean());
			}
			return changed;
		}

		default boolean containsAll (OfBoolean other) {
			BooleanIterator it = other.iterator();
			boolean has = true;
			while (it.hasNext()) {
				has &= contains(it.nextBoolean());
			}
			return has;
		}

		/**
		 * Removes all of the elements of this collection that satisfy the given
		 * predicate.  Errors or runtime exceptions thrown during iteration or by
		 * the predicate are relayed to the caller.
		 *
		 * @implSpec
		 * The default implementation traverses all elements of the collection using
		 * its {@link #iterator()}.  Each matching element is removed using
		 * {@link PrimitiveIterator#remove()}.  If the collection's iterator does not
		 * support removal then an {@code UnsupportedOperationException} will be
		 * thrown on the first matching element.
		 *
		 * @param filter a predicate which returns {@code true} for elements to be
		 *        removed
		 * @return {@code true} if any elements were removed
		 * @throws UnsupportedOperationException if elements cannot be removed
		 *         from this collection.  Implementations may throw this exception if a
		 *         matching element cannot be removed or if, in general, removal is not
		 *         supported.
		 */
		default boolean removeIf(BooleanPredicate filter) {
			boolean removed = false;
			final BooleanIterator each = iterator();
			while (each.hasNext()) {
				if (filter.test(each.nextBoolean())) {
					each.remove();
					removed = true;
				}
			}
			return removed;
		}

		default boolean retainAll (OfBoolean other) {
			boolean changed = false;
			BooleanIterator it = iterator();
			while (it.hasNext()) {
				if (!other.contains(it.nextBoolean())) {
					it.remove();
					changed = true;
				}
			}
			return changed;
		}

		@Override
		BooleanIterator iterator ();

		/**
		 * Allocates a new boolean array with exactly {@link #size()} items, fills it with the
		 * contents of this PrimitiveCollection, and returns it.
		 * @return a new boolean array
		 */
		default boolean[] toArray () {
			final int sz = size();
			boolean[] receiver = new boolean[sz];
			BooleanIterator it = iterator();
			int i = 0;
			while (it.hasNext())
				receiver[i++] = it.nextBoolean();
			return receiver;
		}
		/**
		 * Fills the given array with the entire contents of this PrimitiveCollection, up to
		 * {@link #size()} items, or if receiver is not large enough, then this allocates a new
		 * boolean array with {@link #size()} items and returns that.
		 * @param receiver a boolean array that will be filled with the items from this, if possible
		 * @return {@code receiver}, if it was modified, or a new boolean array otherwise
		 */
		default boolean[] toArray (boolean[] receiver){
			final int sz = size();
			if(receiver.length < sz)
				receiver = new boolean[sz];
			BooleanIterator it = iterator();
			int i = 0;
			while (it.hasNext())
				receiver[i++] = it.nextBoolean();
			return receiver;
		}

		/**
		 * Performs the given action for each element of the {@code PrimitiveCollection.OfBoolean}
		 * until all elements have been processed or the action throws an
		 * exception.  Actions are performed in the order of iteration, if that
		 * order is specified.  Exceptions thrown by the action are relayed to the
		 * caller.
		 *
		 * @param action The action to be performed for each element
		 */
		default void forEach(BooleanConsumer action) {
			BooleanIterator it = iterator();
			while (it.hasNext())
				action.accept(it.nextBoolean());
		}

		/**
		 * Attempts to get the first item in this PrimitiveCollection, where "first" is only
		 * defined meaningfully if this type is ordered. Many times, this applies to a class
		 * that is not ordered, and in those cases it can get an arbitrary item, and that item
		 * is permitted to be different for different calls to first().
		 * <br>
		 * This is useful for cases where you would normally be able to call something like
		 * {@link java.util.List#get(int)} to get an item, any item, from a collection, but
		 * whatever class you're using doesn't necessarily provide a get(), first(), peek(),
		 * or similar method.
		 * <br>
		 * The default implementation uses {@link #iterator()}, tries to get the first item,
		 * or throws an IllegalStateException if this is empty.
		 * @throws IllegalStateException if this is empty
		 * @return the first item in this PrimitiveCollection, as produced by {@link #iterator()}
		 */
		default boolean first() {
			BooleanIterator it = iterator();
			if(it.hasNext()) return it.nextBoolean();
			throw new IllegalStateException("Can't get the first() item of an empty PrimitiveCollection.");
		}
	}
}

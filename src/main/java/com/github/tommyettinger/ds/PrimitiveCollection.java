package com.github.tommyettinger.ds;

import com.github.tommyettinger.ds.support.function.FloatConsumer;
import com.github.tommyettinger.ds.support.util.FloatIterator;

import java.util.PrimitiveIterator;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

/**
 * Created by Tommy Ettinger on 10/22/2020.
 */
public interface PrimitiveCollection<T, T_CONS> {
	int size();
	
	default boolean isEmpty() {
		return size() == 0;
	}
	
	PrimitiveIterator<T, T_CONS> iterator();
	
	void clear();
	
	int hashCode();
	
	boolean equals(Object other);

	interface OfInt extends PrimitiveCollection<Integer, IntConsumer> {
		boolean add(int item);

		boolean remove(int item);

		boolean contains(int item);

		default boolean addAll (OfInt other) {
			PrimitiveIterator.OfInt it = other.iterator();
			boolean changed = false;
			while (it.hasNext())
				changed |= add(it.nextInt());
			return changed;
		}

		default boolean removeAll (OfInt other) {
			PrimitiveIterator.OfInt it = other.iterator();
			boolean changed = false;
			while (it.hasNext())
				changed |= remove(it.nextInt());
			return changed;
		}

		default boolean containsAll (OfInt other) {
			PrimitiveIterator.OfInt it = other.iterator();
			boolean has = true;
			while (it.hasNext())
				has &= contains(it.nextInt());
			return has;
		}

		PrimitiveIterator.OfInt iterator();
	}

	interface OfLong extends PrimitiveCollection<Long, LongConsumer> {
		boolean add(long item);

		boolean remove(long item);

		boolean contains(long item);

		default boolean addAll (OfLong other) {
			PrimitiveIterator.OfLong it = other.iterator();
			boolean changed = false;
			while (it.hasNext())
				changed |= add(it.nextLong());
			return changed;
		}

		default boolean removeAll (OfLong other) {
			PrimitiveIterator.OfLong it = other.iterator();
			boolean changed = false;
			while (it.hasNext())
				changed |= remove(it.nextLong());
			return changed;
		}

		default boolean containsAll (OfLong other) {
			PrimitiveIterator.OfLong it = other.iterator();
			boolean has = true;
			while (it.hasNext())
				has &= contains(it.nextLong());
			return has;
		}

		PrimitiveIterator.OfLong iterator();
	}
	interface OfFloat extends PrimitiveCollection<Float, FloatConsumer> {
		boolean add(float item);

		boolean remove(float item);

		boolean contains(float item);

		default boolean addAll (OfFloat other) {
			FloatIterator it = other.iterator();
			boolean changed = false;
			while (it.hasNext())
				changed |= add(it.nextFloat());
			return changed;
		}

		default boolean removeAll (OfFloat other) {
			FloatIterator it = other.iterator();
			boolean changed = false;
			while (it.hasNext())
				changed |= remove(it.nextFloat());
			return changed;
		}

		default boolean containsAll (OfFloat other) {
			FloatIterator it = other.iterator();
			boolean has = true;
			while (it.hasNext())
				has &= contains(it.nextFloat());
			return has;
		}

		FloatIterator iterator();
	}
}

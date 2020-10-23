package com.github.tommyettinger.ds;

import java.util.PrimitiveIterator;
import java.util.function.IntConsumer;

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
}

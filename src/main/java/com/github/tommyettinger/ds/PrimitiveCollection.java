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
		
		boolean addAll(OfInt other);
		
		boolean removeAll(OfInt other);
		
		boolean containsAll(OfInt other);
		
		PrimitiveIterator.OfInt iterator();
		
	}
}

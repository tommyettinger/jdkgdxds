package com.github.tommyettinger.ds.test;

import com.github.tommyettinger.ds.*;
import com.github.tommyettinger.ds.support.iterator.BooleanIterator;
import com.github.tommyettinger.ds.support.iterator.ByteIterator;
import com.github.tommyettinger.ds.support.iterator.CharIterator;
import com.github.tommyettinger.ds.support.iterator.DoubleIterator;
import com.github.tommyettinger.ds.support.iterator.FloatIterator;
import com.github.tommyettinger.ds.support.iterator.IntIterator;
import com.github.tommyettinger.ds.support.iterator.LongIterator;
import com.github.tommyettinger.ds.support.iterator.ShortIterator;
import com.github.tommyettinger.random.WhiskerRandom;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;

public class IteratorTest {
	public static final String[] strings = {
		"alpha", "beta", "gamma", "delta", "epsilon", "zeta", "eta", "theta", "iota", "kappa", "lambda",
		"mu", "nu", "xi", "omicron", "pi", "rho", "sigma", "tau", "upsilon", "phi", "chi", "psi", "omega"
	};
	public static final boolean[] booleans = {
		true, false, false, false, true, false, true, true, false, true, true, false, true, true, false, true
	};
	public static final byte[] bytes = {
		1, 0, -1, 2, -2, 3, -3, 11, 10, -11, 12, -12, 13, -13, 111, 110, -111, 112, -112, 113, -113
	};
	public static final short[] shorts = {
		'Α', 'Β', 'Γ', 'Δ', 'Ε', 'Ζ', 'Η', 'Θ', 'Ι', 'Κ', 'Λ', 'Μ', 'Ν', 'Ξ', 'Ο', 'Π', 'Ρ', 'Σ', 'Τ', 'Υ', 'Φ', 'Χ', 'Ψ', 'Ω'
	};
	public static final char[] chars = {
		'Α', 'Β', 'Γ', 'Δ', 'Ε', 'Ζ', 'Η', 'Θ', 'Ι', 'Κ', 'Λ', 'Μ', 'Ν', 'Ξ', 'Ο', 'Π', 'Ρ', 'Σ', 'Τ', 'Υ', 'Φ', 'Χ', 'Ψ', 'Ω'
	};
	public static final int[] ints = {
		'Α', 'Β', 'Γ', 'Δ', 'Ε', 'Ζ', 'Η', 'Θ', 'Ι', 'Κ', 'Λ', 'Μ', 'Ν', 'Ξ', 'Ο', 'Π', 'Ρ', 'Σ', 'Τ', 'Υ', 'Φ', 'Χ', 'Ψ', 'Ω'
	};
	public static final long[] longs = {
		'Α', 'Β', 'Γ', 'Δ', 'Ε', 'Ζ', 'Η', 'Θ', 'Ι', 'Κ', 'Λ', 'Μ', 'Ν', 'Ξ', 'Ο', 'Π', 'Ρ', 'Σ', 'Τ', 'Υ', 'Φ', 'Χ', 'Ψ', 'Ω'
	};
	public static final float[] floats = {
		'Α', 'Β', 'Γ', 'Δ', 'Ε', 'Ζ', 'Η', 'Θ', 'Ι', 'Κ', 'Λ', 'Μ', 'Ν', 'Ξ', 'Ο', 'Π', 'Ρ', 'Σ', 'Τ', 'Υ', 'Φ', 'Χ', 'Ψ', 'Ω'
	};
	public static final double[] doubles = {
		'Α', 'Β', 'Γ', 'Δ', 'Ε', 'Ζ', 'Η', 'Θ', 'Ι', 'Κ', 'Λ', 'Μ', 'Ν', 'Ξ', 'Ο', 'Π', 'Ρ', 'Σ', 'Τ', 'Υ', 'Φ', 'Χ', 'Ψ', 'Ω'
	};

	@Test
	public void testObjectListIterator() {
		ObjectList<String> data = ObjectList.with(strings);
		int counter = 0, size = data.size();
		for(String item : data){
			Assert.assertNotNull(item);
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		ListIterator<String> it = data.listIterator();
		while (it.hasNext()) {
			String item = it.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
		it = data.listIterator();
		WhiskerRandom random = new WhiskerRandom(12345678901L);
		while (it.hasNext()) {
			String item = it.next();
			if(random.nextBoolean()) {
				//System.out.println("Removing " + item);
				it.remove();
				--counter;
			}
			//else System.out.println("Not removing " + item);
		}
		Assert.assertEquals(data.size(), counter);
		//System.out.println("\n Going backwards now...\n");
		while (it.hasPrevious()) {
			String item = it.previous();
			if(random.nextBoolean()) {
				//System.out.println("Removing " + item);
				//System.out.println("Before: " + data);
				it.remove();
				//System.out.println("After: " + data);
				--counter;
			}
			//else System.out.println("Not removing " + item);
		}
		Assert.assertEquals(data.size(), counter);
		//System.out.println("\n Going forwards now...\n");
		while (it.hasNext()) {
			String item = it.next();
			if (!random.nextBoolean()) {
				//System.out.println("Changing " + item);
				it.set(item.toUpperCase());
			} else {
				//System.out.println("Adding :)");
				it.add(":)");
			}
		}
		//System.out.println(data);
		Assert.assertEquals("[beta, :), ETA, theta, :), iota, :), kappa, :), TAU, chi, :)]", data.toString());
		data.clear();
		data.add("aaa");
		data.add("bbb");
		data.add("ccc");
		it = data.listIterator();
		while (it.hasNext()) {
			String item = it.next();
			if("ccc".equals(item))
				it.remove();
		}
		//System.out.println(data);
	}

	/**
	 * We just use this to verify the behavior of a correct List implementation and its iterators.
	 */
	@Test
	public void testArrayListIterator() {
		ArrayList<String> data = new ArrayList<>(ObjectList.with(strings));
		int counter = 0, size = data.size();
		for(String item : data){
			Assert.assertNotNull(item);
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		ListIterator<String> it = data.listIterator();
		while (it.hasNext()) {
			String item = it.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
		it = data.listIterator();
		WhiskerRandom random = new WhiskerRandom(12345678901L);
		while (it.hasNext()) {
			String item = it.next();
			if(random.nextBoolean()) {
				//System.out.println("Removing " + item);
				it.remove();
				--counter;
			}
			//else System.out.println("Not removing " + item);
		}
		Assert.assertEquals(data.size(), counter);
		//System.out.println("\n Going backwards now...\n");
		while (it.hasPrevious()) {
			String item = it.previous();
			if(random.nextBoolean()) {
				//System.out.println("Removing " + item);
				it.remove();
				--counter;
			}
			//else System.out.println("Not removing " + item);
		}
		Assert.assertEquals(data.size(), counter);
		//System.out.println("\n Going forwards now...\n");
		while (it.hasNext()) {
			String item = it.next();
			if (!random.nextBoolean()) {
				//System.out.println("Changing " + item);
				it.set(item.toUpperCase());
			} else {
				//System.out.println("Adding :)");
				it.add(":)");
			}
		}
		//System.out.println(data);
		Assert.assertEquals("[beta, :), ETA, theta, :), iota, :), kappa, :), TAU, chi, :)]", data.toString());
		data.clear();
		data.add("aaa");
		data.add("bbb");
		data.add("ccc");
		it = data.listIterator();
		while (it.hasNext()) {
			String item = it.next();
			if("ccc".equals(item))
				it.remove();
		}
		//System.out.println(data);
	}

	@Test
	public void testObjectDequeIterator() {
		ObjectDeque<String> data = ObjectDeque.with(strings);
		int counter = 0, size = data.size();
		for(String item : data){
			Assert.assertNotNull(item);
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		counter = 0;
		ListIterator<String> it = data.iterator();
		while (it.hasNext()) {
			String item = it.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
		it = data.iterator();
		WhiskerRandom random = new WhiskerRandom(12345678901L);
		while (it.hasNext()) {
			String item = it.next();
			if(random.nextBoolean()) {
				//System.out.println("Removing " + item);
				it.remove();
				--counter;
			}
			//else System.out.println("Not removing " + item);
		}
		Assert.assertEquals(data.size(), counter);
		//System.out.println("\n Going backwards now...\n");
		while (it.hasPrevious()) {
			String item = it.previous();
			if(random.nextBoolean()) {
				//System.out.println("Removing " + item);
				//System.out.println("Before: " + data);
				it.remove();
				//System.out.println("After: " + data);
				--counter;
			}
			//else System.out.println("Not removing " + item);
		}
		Assert.assertEquals(data.size(), counter);
		//System.out.println("\n Going forwards now...\n");
		while (it.hasNext()) {
			String item = it.next();
			if (!random.nextBoolean()) {
				//System.out.println("Changing " + item);
				it.set(item.toUpperCase());
			} else {
				//System.out.println("Adding :)");
				it.add(":)");
			}
		}
		//System.out.println(data);
		Assert.assertEquals("[beta, :), ETA, theta, :), iota, :), kappa, :), TAU, chi, :)]", data.toString());
		data.clear();
		data.add("aaa");
		data.add("bbb");
		data.add("ccc");
		it = data.iterator();
		while (it.hasNext()) {
			String item = it.next();
			if("ccc".equals(item))
				it.remove();
		}
		//System.out.println(data);
	}

	@Test
	public void testObjectSetIterator() {
		ObjectSet<String> data = ObjectSet.with(strings);
		int counter = 0, size = data.size();
		for(String item : data){
			Assert.assertNotNull(item);
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<String> it = data.iterator();
		while (it.hasNext()) {
			String item = it.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testObjectOrderedSetIterator() {
		ObjectOrderedSet<String> data = ObjectOrderedSet.with(strings);
		int counter = 0, size = data.size();
		for(String item : data){
			Assert.assertNotNull(item);
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<String> it = data.iterator();
		while (it.hasNext()) {
			String item = it.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testObjectObjectMapIterator() {
		ObjectObjectMap<String, String> data = new ObjectObjectMap<>(strings, strings);
		int counter = 0, size = data.size();
		for(Map.Entry<String, String> item : data){
			Assert.assertNotNull(item);
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<Map.Entry<String, String>> ie = data.iterator();
		while (ie.hasNext()) {
			Map.Entry<String, String> item = ie.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<String> ik = data.keySet().iterator();
		while (ik.hasNext()) {
			String item = ik.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<String> iv = data.values().iterator();
		while (iv.hasNext()) {
			String item = iv.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testObjectObjectOrderedMapIterator() {
		ObjectObjectOrderedMap<String, String> data = new ObjectObjectOrderedMap<>(strings, strings);
		int counter = 0, size = data.size();
		for(Map.Entry<String, String> item : data){
			Assert.assertNotNull(item);
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<Map.Entry<String, String>> ie = data.iterator();
		while (ie.hasNext()) {
			Map.Entry<String, String> item = ie.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<String> ik = data.keySet().iterator();
		while (ik.hasNext()) {
			String item = ik.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<String> iv = data.values().iterator();
		while (iv.hasNext()) {
			String item = iv.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testObjectIntMapIterator() {
		ObjectIntMap<String> data = new ObjectIntMap<>(strings, ints);
		int counter = 0, size = data.size();
		for(ObjectIntMap.Entry<String> item : data){
			Assert.assertNotNull(item);
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<ObjectIntMap.Entry<String>> ie = data.iterator();
		while (ie.hasNext()) {
			ObjectIntMap.Entry<String> item = ie.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<String> ik = data.keySet().iterator();
		while (ik.hasNext()) {
			String item = ik.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		IntIterator iv = data.values().iterator();
		while (iv.hasNext()) {
			int item = iv.nextInt();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testObjectIntOrderedMapIterator() {
		ObjectIntOrderedMap<String> data = new ObjectIntOrderedMap<>(strings, ints);
		int counter = 0, size = data.size();
		for(ObjectIntMap.Entry<String> item : data){
			Assert.assertNotNull(item);
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<ObjectIntMap.Entry<String>> ie = data.iterator();
		while (ie.hasNext()) {
			ObjectIntMap.Entry<String> item = ie.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<String> ik = data.keySet().iterator();
		while (ik.hasNext()) {
			String item = ik.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		IntIterator iv = data.values().iterator();
		while (iv.hasNext()) {
			int item = iv.nextInt();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testObjectLongMapIterator() {
		ObjectLongMap<String> data = new ObjectLongMap<>(strings, longs);
		int counter = 0, size = data.size();
		for(ObjectLongMap.Entry<String> item : data){
			Assert.assertNotNull(item);
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<ObjectLongMap.Entry<String>> ie = data.iterator();
		while (ie.hasNext()) {
			ObjectLongMap.Entry<String> item = ie.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<String> ik = data.keySet().iterator();
		while (ik.hasNext()) {
			String item = ik.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		LongIterator iv = data.values().iterator();
		while (iv.hasNext()) {
			long item = iv.nextLong();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testObjectLongOrderedMapIterator() {
		ObjectLongOrderedMap<String> data = new ObjectLongOrderedMap<>(strings, longs);
		int counter = 0, size = data.size();
		for(ObjectLongMap.Entry<String> item : data){
			Assert.assertNotNull(item);
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<ObjectLongMap.Entry<String>> ie = data.iterator();
		while (ie.hasNext()) {
			ObjectLongMap.Entry<String> item = ie.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<String> ik = data.keySet().iterator();
		while (ik.hasNext()) {
			String item = ik.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		LongIterator iv = data.values().iterator();
		while (iv.hasNext()) {
			long item = iv.nextLong();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testObjectFloatMapIterator() {
		ObjectFloatMap<String> data = new ObjectFloatMap<>(strings, floats);
		int counter = 0, size = data.size();
		for(ObjectFloatMap.Entry<String> item : data){
			Assert.assertNotNull(item);
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<ObjectFloatMap.Entry<String>> ie = data.iterator();
		while (ie.hasNext()) {
			ObjectFloatMap.Entry<String> item = ie.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<String> ik = data.keySet().iterator();
		while (ik.hasNext()) {
			String item = ik.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		FloatIterator iv = data.values().iterator();
		while (iv.hasNext()) {
			float item = iv.nextFloat();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testObjectFloatOrderedMapIterator() {
		ObjectFloatOrderedMap<String> data = new ObjectFloatOrderedMap<>(strings, floats);
		int counter = 0, size = data.size();
		for(ObjectFloatMap.Entry<String> item : data){
			Assert.assertNotNull(item);
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<ObjectFloatMap.Entry<String>> ie = data.iterator();
		while (ie.hasNext()) {
			ObjectFloatMap.Entry<String> item = ie.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<String> ik = data.keySet().iterator();
		while (ik.hasNext()) {
			String item = ik.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		FloatIterator iv = data.values().iterator();
		while (iv.hasNext()) {
			float item = iv.nextFloat();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}
	
	@Test
	public void testLongObjectMapIterator() {
		LongObjectMap<String> data = new LongObjectMap<>(longs, strings);
		int counter = 0, size = data.size();
		for(LongObjectMap.Entry<String> item : data){
			Assert.assertNotNull(item);
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<LongObjectMap.Entry<String>> ie = data.iterator();
		while (ie.hasNext()) {
			LongObjectMap.Entry<String> item = ie.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		LongIterator ik = data.keySet().iterator();
		while (ik.hasNext()) {
			long item = ik.nextLong();
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<String> iv = data.values().iterator();
		while (iv.hasNext()) {
			String item = iv.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testLongObjectOrderedMapIterator() {
		LongObjectOrderedMap<String> data = new LongObjectOrderedMap<>(longs, strings);
		int counter = 0, size = data.size();
		for(LongObjectMap.Entry<String> item : data){
			Assert.assertNotNull(item);
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<LongObjectMap.Entry<String>> ie = data.iterator();
		while (ie.hasNext()) {
			LongObjectMap.Entry<String> item = ie.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		LongIterator ik = data.keySet().iterator();
		while (ik.hasNext()) {
			long item = ik.nextLong();
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<String> iv = data.values().iterator();
		while (iv.hasNext()) {
			String item = iv.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testLongIntMapIterator() {
		LongIntMap data = new LongIntMap(longs, ints);
		int counter = 0, size = data.size();
		for(LongIntMap.Entry item : data){
			Assert.assertNotNull(item);
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<LongIntMap.Entry> ie = data.iterator();
		while (ie.hasNext()) {
			LongIntMap.Entry item = ie.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		LongIterator ik = data.keySet().iterator();
		while (ik.hasNext()) {
			long item = ik.nextLong();
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		IntIterator iv = data.values().iterator();
		while (iv.hasNext()) {
			int item = iv.nextInt();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testLongIntOrderedMapIterator() {
		LongIntOrderedMap data = new LongIntOrderedMap(longs, ints);
		int counter = 0, size = data.size();
		for(LongIntMap.Entry item : data){
			Assert.assertNotNull(item);
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<LongIntMap.Entry> ie = data.iterator();
		while (ie.hasNext()) {
			LongIntMap.Entry item = ie.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		LongIterator ik = data.keySet().iterator();
		while (ik.hasNext()) {
			long item = ik.nextLong();
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		IntIterator iv = data.values().iterator();
		while (iv.hasNext()) {
			int item = iv.nextInt();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testLongLongMapIterator() {
		LongLongMap data = new LongLongMap(longs, longs);
		int counter = 0, size = data.size();
		for(LongLongMap.Entry item : data){
			Assert.assertNotNull(item);
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<LongLongMap.Entry> ie = data.iterator();
		while (ie.hasNext()) {
			LongLongMap.Entry item = ie.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		LongIterator ik = data.keySet().iterator();
		while (ik.hasNext()) {
			long item = ik.nextLong();
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		LongIterator iv = data.values().iterator();
		while (iv.hasNext()) {
			long item = iv.nextLong();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testLongLongOrderedMapIterator() {
		LongLongOrderedMap data = new LongLongOrderedMap(longs, longs);
		int counter = 0, size = data.size();
		for(LongLongMap.Entry item : data){
			Assert.assertNotNull(item);
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<LongLongMap.Entry> ie = data.iterator();
		while (ie.hasNext()) {
			LongLongMap.Entry item = ie.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		LongIterator ik = data.keySet().iterator();
		while (ik.hasNext()) {
			long item = ik.nextLong();
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		LongIterator iv = data.values().iterator();
		while (iv.hasNext()) {
			long item = iv.nextLong();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testLongFloatMapIterator() {
		LongFloatMap data = new LongFloatMap(longs, floats);
		int counter = 0, size = data.size();
		for(LongFloatMap.Entry item : data){
			Assert.assertNotNull(item);
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<LongFloatMap.Entry> ie = data.iterator();
		while (ie.hasNext()) {
			LongFloatMap.Entry item = ie.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		LongIterator ik = data.keySet().iterator();
		while (ik.hasNext()) {
			long item = ik.nextLong();
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		FloatIterator iv = data.values().iterator();
		while (iv.hasNext()) {
			float item = iv.nextFloat();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testLongFloatOrderedMapIterator() {
		LongFloatOrderedMap data = new LongFloatOrderedMap(longs, floats);
		int counter = 0, size = data.size();
		for(LongFloatMap.Entry item : data){
			Assert.assertNotNull(item);
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<LongFloatMap.Entry> ie = data.iterator();
		while (ie.hasNext()) {
			LongFloatMap.Entry item = ie.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		LongIterator ik = data.keySet().iterator();
		while (ik.hasNext()) {
			long item = ik.nextLong();
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		FloatIterator iv = data.values().iterator();
		while (iv.hasNext()) {
			float item = iv.nextFloat();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}
	
	@Test
	public void testIntObjectMapIterator() {
		IntObjectMap<String> data = new IntObjectMap<>(ints, strings);
		int counter = 0, size = data.size();
		for(IntObjectMap.Entry<String> item : data){
			Assert.assertNotNull(item);
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<IntObjectMap.Entry<String>> ie = data.iterator();
		while (ie.hasNext()) {
			IntObjectMap.Entry<String> item = ie.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		IntIterator ik = data.keySet().iterator();
		while (ik.hasNext()) {
			int item = ik.nextInt();
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<String> iv = data.values().iterator();
		while (iv.hasNext()) {
			String item = iv.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testIntObjectOrderedMapIterator() {
		IntObjectOrderedMap<String> data = new IntObjectOrderedMap<>(ints, strings);
		int counter = 0, size = data.size();
		for(IntObjectMap.Entry<String> item : data){
			Assert.assertNotNull(item);
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<IntObjectMap.Entry<String>> ie = data.iterator();
		while (ie.hasNext()) {
			IntObjectMap.Entry<String> item = ie.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		IntIterator ik = data.keySet().iterator();
		while (ik.hasNext()) {
			int item = ik.nextInt();
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<String> iv = data.values().iterator();
		while (iv.hasNext()) {
			String item = iv.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testIntIntMapIterator() {
		IntIntMap data = new IntIntMap(ints, ints);
		int counter = 0, size = data.size();
		for(IntIntMap.Entry item : data){
			Assert.assertNotNull(item);
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<IntIntMap.Entry> ie = data.iterator();
		while (ie.hasNext()) {
			IntIntMap.Entry item = ie.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		IntIterator ik = data.keySet().iterator();
		while (ik.hasNext()) {
			int item = ik.nextInt();
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		IntIterator iv = data.values().iterator();
		while (iv.hasNext()) {
			int item = iv.nextInt();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testIntIntOrderedMapIterator() {
		IntIntOrderedMap data = new IntIntOrderedMap(ints, ints);
		int counter = 0, size = data.size();
		for(IntIntMap.Entry item : data){
			Assert.assertNotNull(item);
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<IntIntMap.Entry> ie = data.iterator();
		while (ie.hasNext()) {
			IntIntMap.Entry item = ie.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		IntIterator ik = data.keySet().iterator();
		while (ik.hasNext()) {
			int item = ik.nextInt();
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		IntIterator iv = data.values().iterator();
		while (iv.hasNext()) {
			int item = iv.nextInt();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testIntLongMapIterator() {
		IntLongMap data = new IntLongMap(ints, longs);
		int counter = 0, size = data.size();
		for(IntLongMap.Entry item : data){
			Assert.assertNotNull(item);
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<IntLongMap.Entry> ie = data.iterator();
		while (ie.hasNext()) {
			IntLongMap.Entry item = ie.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		IntIterator ik = data.keySet().iterator();
		while (ik.hasNext()) {
			int item = ik.nextInt();
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		LongIterator iv = data.values().iterator();
		while (iv.hasNext()) {
			long item = iv.nextLong();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testIntLongOrderedMapIterator() {
		IntLongOrderedMap data = new IntLongOrderedMap(ints, longs);
		int counter = 0, size = data.size();
		for(IntLongMap.Entry item : data){
			Assert.assertNotNull(item);
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<IntLongMap.Entry> ie = data.iterator();
		while (ie.hasNext()) {
			IntLongMap.Entry item = ie.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		IntIterator ik = data.keySet().iterator();
		while (ik.hasNext()) {
			int item = ik.nextInt();
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		LongIterator iv = data.values().iterator();
		while (iv.hasNext()) {
			long item = iv.nextLong();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testIntFloatMapIterator() {
		IntFloatMap data = new IntFloatMap(ints, floats);
		int counter = 0, size = data.size();
		for(IntFloatMap.Entry item : data){
			Assert.assertNotNull(item);
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<IntFloatMap.Entry> ie = data.iterator();
		while (ie.hasNext()) {
			IntFloatMap.Entry item = ie.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		IntIterator ik = data.keySet().iterator();
		while (ik.hasNext()) {
			int item = ik.nextInt();
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		FloatIterator iv = data.values().iterator();
		while (iv.hasNext()) {
			float item = iv.nextFloat();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testIntFloatOrderedMapIterator() {
		IntFloatOrderedMap data = new IntFloatOrderedMap(ints, floats);
		int counter = 0, size = data.size();
		for(IntFloatMap.Entry item : data){
			Assert.assertNotNull(item);
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<IntFloatMap.Entry> ie = data.iterator();
		while (ie.hasNext()) {
			IntFloatMap.Entry item = ie.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		IntIterator ik = data.keySet().iterator();
		while (ik.hasNext()) {
			int item = ik.nextInt();
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		FloatIterator iv = data.values().iterator();
		while (iv.hasNext()) {
			float item = iv.nextFloat();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testByteListIterator() {
		ByteList data = ByteList.with(bytes);
		int counter = 0, size = data.size();
		ByteIterator it = data.iterator();
		while (it.hasNext()) {
			byte item = it.nextByte();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testShortListIterator() {
		ShortList data = ShortList.with(shorts);
		int counter = 0, size = data.size();
		ShortIterator it = data.iterator();
		while (it.hasNext()) {
			short item = it.nextShort();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testCharListIterator() {
		CharList data = CharList.with(chars);
		int counter = 0, size = data.size();
		CharIterator it = data.iterator();
		while (it.hasNext()) {
			char item = it.nextChar();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testFloatListIterator() {
		FloatList data = FloatList.with(floats);
		int counter = 0, size = data.size();
		FloatIterator it = data.iterator();
		while (it.hasNext()) {
			float item = it.nextFloat();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testIntListIterator() {
		IntList data = IntList.with(ints);
		int counter = 0, size = data.size();
		IntIterator it = data.iterator();
		while (it.hasNext()) {
			int item = it.nextInt();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testLongListIterator() {
		LongList data = LongList.with(longs);
		int counter = 0, size = data.size();
		LongIterator it = data.iterator();
		while (it.hasNext()) {
			long item = it.nextLong();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testDoubleListIterator() {
		DoubleList data = DoubleList.with(doubles);
		int counter = 0, size = data.size();
		DoubleIterator it = data.iterator();
		while (it.hasNext()) {
			double item = it.nextDouble();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}
	
	@Test
	public void testBooleanListIterator() {
		BooleanList data = BooleanList.with(booleans);
		int counter = 0, size = data.size();
		BooleanIterator it = data.iterator();
		while (it.hasNext()) {
			boolean item = it.nextBoolean();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testByteDequeIterator() {
		ByteDeque data = ByteDeque.with(bytes);
		int counter = 0, size = data.size();
		ByteIterator it = data.iterator();
		while (it.hasNext()) {
			byte item = it.nextByte();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testShortDequeIterator() {
		ShortDeque data = ShortDeque.with(shorts);
		int counter = 0, size = data.size();
		ShortIterator it = data.iterator();
		while (it.hasNext()) {
			short item = it.nextShort();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testCharDequeIterator() {
		CharDeque data = CharDeque.with(chars);
		int counter = 0, size = data.size();
		CharIterator it = data.iterator();
		while (it.hasNext()) {
			char item = it.nextChar();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testFloatDequeIterator() {
		FloatDeque data = FloatDeque.with(floats);
		int counter = 0, size = data.size();
		FloatIterator it = data.iterator();
		while (it.hasNext()) {
			float item = it.nextFloat();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testIntDequeIterator() {
		IntDeque data = IntDeque.with(ints);
		int counter = 0, size = data.size();
		IntIterator it = data.iterator();
		while (it.hasNext()) {
			int item = it.nextInt();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testLongDequeIterator() {
		LongDeque data = LongDeque.with(longs);
		int counter = 0, size = data.size();
		LongIterator it = data.iterator();
		while (it.hasNext()) {
			long item = it.nextLong();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testDoubleDequeIterator() {
		DoubleDeque data = DoubleDeque.with(doubles);
		int counter = 0, size = data.size();
		DoubleIterator it = data.iterator();
		while (it.hasNext()) {
			double item = it.nextDouble();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}
	
	@Test
	public void testBooleanDequeIterator() {
		BooleanDeque data = BooleanDeque.with(booleans);
		int counter = 0, size = data.size();
		BooleanIterator it = data.iterator();
		while (it.hasNext()) {
			boolean item = it.nextBoolean();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testIntSetIterator() {
		IntSet data = IntSet.with(ints);
		int counter = 0, size = data.size();
		IntIterator it = data.iterator();
		while (it.hasNext()) {
			int item = it.nextInt();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testIntOrderedSetIterator() {
		IntOrderedSet data = IntOrderedSet.with(ints);
		int counter = 0, size = data.size();
		IntIterator it = data.iterator();
		while (it.hasNext()) {
			int item = it.nextInt();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testLongSetIterator() {
		LongSet data = LongSet.with(longs);
		int counter = 0, size = data.size();
		LongIterator it = data.iterator();
		while (it.hasNext()) {
			long item = it.nextLong();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testLongOrderedSetIterator() {
		LongOrderedSet data = LongOrderedSet.with(longs);
		int counter = 0, size = data.size();
		LongIterator it = data.iterator();
		while (it.hasNext()) {
			long item = it.nextLong();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testCaseInsensitiveSetIterator() {
		CaseInsensitiveSet data = CaseInsensitiveSet.with(strings);
		int counter = 0, size = data.size();
		for(CharSequence item : data){
			Assert.assertNotNull(item);
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<CharSequence> it = data.iterator();
		while (it.hasNext()) {
			CharSequence item = it.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testCaseInsensitiveOrderedSetIterator() {
		CaseInsensitiveOrderedSet data = CaseInsensitiveOrderedSet.with(strings);
		int counter = 0, size = data.size();
		for(CharSequence item : data){
			Assert.assertNotNull(item);
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<CharSequence> it = data.iterator();
		while (it.hasNext()) {
			CharSequence item = it.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testCaseInsensitiveMapIterator() {
		CaseInsensitiveMap<String> data = new CaseInsensitiveMap<>(strings, strings);
		int counter = 0, size = data.size();
		for(Map.Entry<CharSequence, String> item : data){
			Assert.assertNotNull(item);
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<Map.Entry<CharSequence, String>> ie = data.iterator();
		while (ie.hasNext()) {
			Map.Entry<CharSequence, String> item = ie.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<CharSequence> ik = data.keySet().iterator();
		while (ik.hasNext()) {
			CharSequence item = ik.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<String> iv = data.values().iterator();
		while (iv.hasNext()) {
			String item = iv.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testCaseInsensitiveOrderedMapIterator() {
		CaseInsensitiveOrderedMap<String> data = new CaseInsensitiveOrderedMap<>(strings, strings);
		int counter = 0, size = data.size();
		for(Map.Entry<CharSequence, String> item : data){
			Assert.assertNotNull(item);
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<Map.Entry<CharSequence, String>> ie = data.iterator();
		while (ie.hasNext()) {
			Map.Entry<CharSequence, String> item = ie.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<CharSequence> ik = data.keySet().iterator();
		while (ik.hasNext()) {
			CharSequence item = ik.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<String> iv = data.values().iterator();
		while (iv.hasNext()) {
			String item = iv.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testNumberedSetIterator() {
		NumberedSet<String> data = NumberedSet.with(strings);
		int counter = 0, size = data.size();
		for(String item : data){
			Assert.assertNotNull(item);
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<String> it = data.iterator();
		while (it.hasNext()) {
			String item = it.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}


	@Test
	public void testHolderSetIterator() {
		HolderSet<String, Integer> data = HolderSet.with(String::hashCode, strings);
		int counter = 0, size = data.size();
		for(String item : data){
			Assert.assertNotNull(item);
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<String> it = data.iterator();
		while (it.hasNext()) {
			String item = it.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testHolderOrderedSetIterator() {
		HolderOrderedSet<String, Integer> data = HolderOrderedSet.with(String::hashCode, strings);
		int counter = 0, size = data.size();
		for(String item : data){
			Assert.assertNotNull(item);
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<String> it = data.iterator();
		while (it.hasNext()) {
			String item = it.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testIdentityObjectMapIterator() {
		IdentityObjectMap<String, String> data = new IdentityObjectMap<>(strings, strings);
		int counter = 0, size = data.size();
		for(Map.Entry<String, String> item : data){
			Assert.assertNotNull(item);
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<Map.Entry<String, String>> ie = data.iterator();
		while (ie.hasNext()) {
			Map.Entry<String, String> item = ie.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<String> ik = data.keySet().iterator();
		while (ik.hasNext()) {
			String item = ik.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<String> iv = data.values().iterator();
		while (iv.hasNext()) {
			String item = iv.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testIdentityObjectOrderedMapIterator() {
		IdentityObjectMap<String, String> data = new IdentityObjectMap<>(strings, strings);
		int counter = 0, size = data.size();
		for(Map.Entry<String, String> item : data){
			Assert.assertNotNull(item);
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<Map.Entry<String, String>> ie = data.iterator();
		while (ie.hasNext()) {
			Map.Entry<String, String> item = ie.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<String> ik = data.keySet().iterator();
		while (ik.hasNext()) {
			String item = ik.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<String> iv = data.values().iterator();
		while (iv.hasNext()) {
			String item = iv.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	private static class IntNode extends BinaryHeap.Node {
		public int v;
		public IntNode (int value) {
			super(value);
			v = value;
		}

		@Override
		public float getValue () {
			return super.getValue();
		}
	}
	@Test
	public void testBinaryHeapIterator() {
		BinaryHeap<IntNode> data = BinaryHeap.minHeapWith(
			new IntNode(10), new IntNode(1), new IntNode(3), new IntNode(9), new IntNode(5),
			new IntNode(4), new IntNode(2), new IntNode(7), new IntNode(6), new IntNode(8));
		int counter = 0, size = data.size();
		Iterator<IntNode> it = data.iterator();
		while (it.hasNext()) {
			int item = it.next().v;
			++counter;
			System.out.print(item + ", ");
		}
		Assert.assertEquals(size, counter);
	}


}
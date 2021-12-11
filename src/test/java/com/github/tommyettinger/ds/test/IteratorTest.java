package com.github.tommyettinger.ds.test;

import com.github.tommyettinger.ds.ObjectDeque;
import com.github.tommyettinger.ds.ObjectFloatMap;
import com.github.tommyettinger.ds.ObjectFloatOrderedMap;
import com.github.tommyettinger.ds.ObjectIntMap;
import com.github.tommyettinger.ds.ObjectIntOrderedMap;
import com.github.tommyettinger.ds.ObjectList;
import com.github.tommyettinger.ds.ObjectLongMap;
import com.github.tommyettinger.ds.ObjectLongOrderedMap;
import com.github.tommyettinger.ds.ObjectObjectMap;
import com.github.tommyettinger.ds.ObjectObjectOrderedMap;
import com.github.tommyettinger.ds.ObjectOrderedSet;
import com.github.tommyettinger.ds.ObjectSet;
import org.junit.Assert;
import org.junit.Test;

import java.util.Iterator;
import java.util.Map;

public class IteratorTest {
	public static final String[] strings = {
		"alpha", "beta", "gamma", "delta", "epsilon", "zeta", "eta", "theta", "iota", "kappa", "lambda",
		"mu", "nu", "xi", "omicron", "pi", "rho", "sigma", "tau", "upsilon", "phi", "chi", "psi", "omega"
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
		Iterator<String> it = data.iterator();
		for (; it.hasNext();) {
			String item = it.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
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
		Iterator<String> it = data.iterator();
		for (; it.hasNext();) {
			String item = it.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
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
		for (; it.hasNext();) {
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
		for (; it.hasNext();) {
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
		Iterator<Map.Entry<String, String>> it = data.iterator();
		for (; it.hasNext();) {
			Map.Entry<String, String> item = it.next();
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
		Iterator<Map.Entry<String, String>> it = data.iterator();
		for (; it.hasNext();) {
			Map.Entry<String, String> item = it.next();
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
		Iterator<ObjectIntMap.Entry<String>> it = data.iterator();
		for (; it.hasNext();) {
			ObjectIntMap.Entry<String> item = it.next();
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
		Iterator<ObjectIntMap.Entry<String>> it = data.iterator();
		for (; it.hasNext();) {
			ObjectIntMap.Entry<String> item = it.next();
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
		Iterator<ObjectLongMap.Entry<String>> it = data.iterator();
		for (; it.hasNext();) {
			ObjectLongMap.Entry<String> item = it.next();
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
		Iterator<ObjectLongMap.Entry<String>> it = data.iterator();
		for (; it.hasNext();) {
			ObjectLongMap.Entry<String> item = it.next();
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
		Iterator<ObjectFloatMap.Entry<String>> it = data.iterator();
		for (; it.hasNext();) {
			ObjectFloatMap.Entry<String> item = it.next();
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
		Iterator<ObjectFloatMap.Entry<String>> it = data.iterator();
		for (; it.hasNext();) {
			ObjectFloatMap.Entry<String> item = it.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}
}
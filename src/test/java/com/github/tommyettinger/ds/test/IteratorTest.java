package com.github.tommyettinger.ds.test;

import com.github.tommyettinger.ds.ObjectDeque;
import com.github.tommyettinger.ds.ObjectList;
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

}
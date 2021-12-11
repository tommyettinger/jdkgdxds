package com.github.tommyettinger.ds.test;

import com.github.tommyettinger.ds.ObjectSet;
import org.junit.Assert;
import org.junit.Test;

import java.util.Iterator;

public class IteratorTest {
	public static final String[] strings = {
		"alpha", "beta", "gamma", "delta", "epsilon", "zeta", "eta", "theta", "iota", "kappa", "lambda",
		"mu", "nu", "xi", "omicron", "pi", "rho", "sigma", "tau", "upsilon", "phi", "chi", "psi", "omega"
	};
	
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
}
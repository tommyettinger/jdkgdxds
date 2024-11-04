package com.github.tommyettinger.ds.test;

import com.github.tommyettinger.digital.ArrayTools;
import com.github.tommyettinger.ds.IntSet;
import com.github.tommyettinger.ds.ObjectSet;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;

public class SetRemoveTest {
	@Test
	public void testHashSetRemoveAll (){
		HashSet<String> set = new HashSet<>(ObjectSet.with(
				"alpha", "beta", "gamma", "delta", "epsilon", "zeta",
				"eta", "theta", "iota", "kappa", "lambda", "mu"));
		HashSet<String> remover = new HashSet<>(ObjectSet.with("gamma", "delta", "epsilon", "zeta"));
		Assert.assertEquals(set.size(), 12);
		set.removeAll(remover);
		Assert.assertEquals(set.size(), 8);
		System.out.println(set);
		set.removeAll(ObjectSet.with("theta"));
		Assert.assertEquals(set.size(), 7);
		Assert.assertFalse(set.contains("delta"));
		Assert.assertFalse(set.contains("theta"));
		Assert.assertTrue(set.contains("alpha"));
		Assert.assertTrue(set.contains("mu"));
	}
	@Test
	public void testObjectSetRemoveAll (){
		ObjectSet<String> set = new ObjectSet<>(ObjectSet.with(
				"alpha", "beta", "gamma", "delta", "epsilon", "zeta",
				"eta", "theta", "iota", "kappa", "lambda", "mu"));
		ObjectSet<String> remover = new ObjectSet<>(ObjectSet.with("gamma", "delta", "epsilon", "zeta"));
		Assert.assertEquals(set.size(), 12);
		set.removeAll(remover);
		Assert.assertEquals(set.size(), 8);
		System.out.println(set);
		set.removeAll(ObjectSet.with("theta"));
		Assert.assertEquals(set.size(), 7);
		Assert.assertFalse(set.contains("delta"));
		Assert.assertFalse(set.contains("theta"));
		Assert.assertTrue(set.contains("alpha"));
		Assert.assertTrue(set.contains("mu"));
	}
	@Test
	public void testIntSetRemoveAll (){
		IntSet set = IntSet.with(
			1, 2, 3, 4,
			5, 6, 7, 8,
			9, 10, 11, 12);
		IntSet remover = IntSet.with(3, 4, 5, 6);
		Assert.assertEquals(set.size(), 12);
		set.removeAll(remover);
		Assert.assertEquals(set.size(), 8);
		System.out.println(set);
		set.removeAll(IntSet.with(8));
		Assert.assertEquals(set.size(), 7);
		Assert.assertFalse(set.contains(4));
		Assert.assertFalse(set.contains(8));
		Assert.assertTrue(set.contains(1));
		Assert.assertTrue(set.contains(12));
	}
}

package com.github.tommyettinger.ds.test;

import com.github.tommyettinger.ds.IntIntMap;
import com.github.tommyettinger.ds.IntObjectMap;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for <a href="https://github.com/libgdx/libgdx/issues/6285">libGDX issue #6285</a>.
 */
public class Issue6285Test {
	@Test
	public void testIntObjectMap6285 () {
		IntObjectMap<Integer> map = new IntObjectMap<>();
		Integer value = 123;
		map.put(1, value);
		map.remove(1);

		Assert.assertFalse( map.containsValue(value) );
	}
	@Test
	public void testIntIntMap6285 () {
		IntIntMap map = new IntIntMap();
		int value = 123;
		map.put(1, value);
		map.remove(1);

		Assert.assertFalse( map.containsValue(value) );
	}
}

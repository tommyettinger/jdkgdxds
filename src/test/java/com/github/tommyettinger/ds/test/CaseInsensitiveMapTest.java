package com.github.tommyettinger.ds.test;

import com.github.tommyettinger.ds.CaseInsensitiveMap;
import com.github.tommyettinger.ds.CaseInsensitiveOrderedMap;
import com.sun.org.apache.bcel.internal.generic.PUSH;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

/**
 * Created by Tommy Ettinger on 10/26/2020.
 */
public class CaseInsensitiveMapTest {
	@Test
	public void testMultipleOperations() {
		CaseInsensitiveMap<String> map = new CaseInsensitiveMap<>(8);
		map.put("@", "");
		map.put("ATTACK!", "0");
		map.put("attack!", "1");
		map.put("Attack!", "2");
		Assert.assertEquals(map.size(), 2);
		Assert.assertEquals(map.get("attack!"), "2");
		map.remove("AtTaCk!");
		Assert.assertEquals(map.size(), 1);
		map.put("Something else,", "42");
		map.put("And more,", "23");
		map.put("And even more.", "666");
		Assert.assertEquals(map.size(), 4);
		map.put("A", "-1");
		map.put("B", "-2");
		map.put("C", "-3");
		map.put("E", "-4");
		Assert.assertEquals(map.size(), 8);
	}

	/**
	 * In 0.0.4, there was a chance that this map wouldn't use the same hash when inserting as when resizing.
	 * This checks one case that was known to have a mismatch.
	 */
	@Test
	public void testMissing() {
		CaseInsensitiveMap<String> map = new CaseInsensitiveMap<>(
			new String[]{"foo", "bar", "baz"},
			new String[]{"foo", "bar", "baz"}
			);
		for(Map.Entry<CharSequence, String> ent : map){
			Assert.assertEquals(ent.getKey(), ent.getValue());
		}
	}

	/**
	 * Just after 0.0.4, there was a chance that this map wouldn't use the same hash when inserting as when resizing.
	 * This checks one case that was known to have a mismatch.
	 */
	@Test
	public void testOrdered() {
		CaseInsensitiveOrderedMap<String> map = new CaseInsensitiveOrderedMap<>(
			new String[]{"foo", "bar", "baz"},
			new String[]{"foo", "bar", "baz"}
			);
		for(Map.Entry<CharSequence, String> ent : map){
			Assert.assertEquals(ent.getKey(), ent.getValue());
		}
	}
}

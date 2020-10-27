package com.github.tommyettinger.ds.test;

import com.github.tommyettinger.ds.CaseInsensitiveMap;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by Tommy Ettinger on 10/26/2020.
 */
public class CaseInsensitiveMapTest {
	@Test
	public void testMultipleOperations() {
		CaseInsensitiveMap<String> map = new CaseInsensitiveMap<>(8);
		map.put("@", "");
		map.put("ATTACK!", "ATTACK!");
		map.put("attack!", "attack!");
		map.put("Attack!", "Attack!");
		Assert.assertEquals(map.size(), 2);
		Assert.assertEquals(map.get("attack!"), "Attack!");
		map.remove("AtTaCk!");
		Assert.assertEquals(map.size(), 1);
	}
}

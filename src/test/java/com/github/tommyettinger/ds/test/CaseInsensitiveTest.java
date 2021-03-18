package com.github.tommyettinger.ds.test;

import com.github.tommyettinger.ds.CaseInsensitiveMap;
import com.github.tommyettinger.ds.CaseInsensitiveOrderedMap;
import com.github.tommyettinger.ds.CaseInsensitiveOrderedSet;
import com.github.tommyettinger.ds.CaseInsensitiveSet;
import com.github.tommyettinger.ds.ObjectList;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

/**
 * Created by Tommy Ettinger on 10/26/2020.
 */
public class CaseInsensitiveTest {
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
	public void testMissingInMap () {
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
	public void testOrderedMap () {
		CaseInsensitiveOrderedMap<String> map = new CaseInsensitiveOrderedMap<>(
			new String[]{"foo", "bar", "baz"},
			new String[]{"foo", "bar", "baz"}
			);
		for(Map.Entry<CharSequence, String> ent : map){
			Assert.assertEquals(ent.getKey(), ent.getValue());
		}
		CaseInsensitiveOrderedMap<ObjectList<String>> synonyms = CaseInsensitiveOrderedMap.with(
			"intelligence", ObjectList.with("cunning", "acumen", "wits", "wisdom", "intellect"),
			"strength", ObjectList.with("power", "potency", "brawn", "muscle", "force")
		);
		CaseInsensitiveOrderedMap<ObjectList<String>> syn2 =
			new CaseInsensitiveOrderedMap<>();
		syn2.putAll(synonyms); // this was a problem due to some generics... messiness.
	}

	@Test
	public void testSet() {
		CaseInsensitiveSet set = CaseInsensitiveSet.with("FOO", "foo", "BAR", "BAZ", "bar", "baz");
		Assert.assertEquals(set.size(), 3);
	}

	@Test
	public void testOrderedSet() {
		CaseInsensitiveOrderedSet set = CaseInsensitiveOrderedSet.with("FOO", "foo", "BAR", "BAZ", "bar", "baz");
		Assert.assertEquals(set.size(), 3);
		Assert.assertEquals(set.first(), "FOO");
	}
}

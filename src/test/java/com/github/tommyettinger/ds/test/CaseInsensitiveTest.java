package com.github.tommyettinger.ds.test;

import com.github.tommyettinger.ds.CaseInsensitiveMap;
import com.github.tommyettinger.ds.CaseInsensitiveOrderedMap;
import com.github.tommyettinger.ds.CaseInsensitiveOrderedSet;
import com.github.tommyettinger.ds.CaseInsensitiveSet;
import com.github.tommyettinger.ds.CharFilter;
import com.github.tommyettinger.ds.FilteredStringOrderedMap;
import com.github.tommyettinger.ds.FilteredStringOrderedSet;
import com.github.tommyettinger.ds.FilteredStringSet;
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
		FilteredStringOrderedMap<String> fil = new FilteredStringOrderedMap<>(Character::isLetter, Character::toUpperCase,
			new String[]{"foo42", "bar666", "baz9001"},
			new String[]{"foo", "bar", "baz"}
		);
		for(Map.Entry<String, String> ent : fil){
			Assert.assertTrue(fil.equate(ent.getKey(), ent.getValue()));
		}
		CaseInsensitiveOrderedMap<ObjectList<String>> synonyms = CaseInsensitiveOrderedMap.with(
			"intelligence", ObjectList.with("cunning", "acumen", "wits", "wisdom", "intellect"),
			"strength", ObjectList.with("power", "potency", "brawn", "muscle", "force")
		);
		CaseInsensitiveOrderedMap<ObjectList<String>> syn2 =
			new CaseInsensitiveOrderedMap<>();
		syn2.putAll(synonyms); // this was a problem due to some generics... messiness.
		Assert.assertEquals(synonyms, syn2);
	}

	@Test
	public void testSet() {
		String[] items = new String[]{"FOO", "foo", "BAR", "BAZ", "bar", "baz"};
		CaseInsensitiveSet set = CaseInsensitiveSet.with(items);
		Assert.assertEquals(set.size(), 3);
		CharFilter filter = CharFilter.getOrCreate("LetterOnlyCaseInsensitive", Character::isLetter, Character::toUpperCase);
		FilteredStringSet fil = FilteredStringSet.with(filter, items);
		Assert.assertEquals(fil.size(), 3);
		Assert.assertEquals(set, fil);
	}

	@Test
	public void testOrderedSet() {
		String[] items = new String[]{"FOO", "foo", "BAR", "BAZ", "bar", "baz"};
		CaseInsensitiveOrderedSet set = CaseInsensitiveOrderedSet.with(items);
		Assert.assertEquals(set.size(), 3);
		CharFilter filter = CharFilter.getOrCreate("LetterOnlyCaseInsensitive", Character::isLetter, Character::toUpperCase);
		FilteredStringOrderedSet fil = FilteredStringOrderedSet.with(filter, items);
		Assert.assertEquals(fil.size(), 3);
		Assert.assertEquals(set, fil);
	}
}

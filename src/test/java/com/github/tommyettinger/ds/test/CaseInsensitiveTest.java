/*
 * Copyright (c) 2025 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.tommyettinger.ds.test;

import com.github.tommyettinger.ds.*;
import com.github.tommyettinger.ds.support.util.CharPredicates;
import org.junit.Assert;
import org.junit.Test;

import java.util.Iterator;
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
		Assert.assertEquals(2, map.size());
		Assert.assertEquals("2", map.get("attack!"));
		map.remove("AtTaCk!");
		Assert.assertEquals(1, map.size());
		map.put("Something else,", "42");
		map.put("And more,", "23");
		map.put("And even more.", "666");
		Assert.assertEquals(4, map.size());
		map.put("A", "-1");
		map.put("B", "-2");
		map.put("C", "-3");
		map.put("E", "-4");
		Assert.assertEquals(8, map.size());
	}

	/**
	 * In 0.0.4, there was a chance that this map wouldn't use the same hash when inserting as when resizing.
	 * This checks one case that was known to have a mismatch.
	 */
	@Test
	public void testMissingInMap() {
		CaseInsensitiveMap<String> map = new CaseInsensitiveMap<>(
			new String[]{"foo", "bar", "baz"},
			new String[]{"foo", "bar", "baz"}
		);
		for (Map.Entry<CharSequence, String> ent : map) {
			Assert.assertEquals(ent.getKey(), ent.getValue());
		}
	}

	/**
	 * Just after 0.0.4, there was a chance that this map wouldn't use the same hash when inserting as when resizing.
	 * This checks one case that was known to have a mismatch.
	 */
	@Test
	public void testOrderedMap() {
		CaseInsensitiveOrderedMap<String> map = new CaseInsensitiveOrderedMap<>(
			new String[]{"foo", "bar", "baz"},
			new String[]{"foo", "bar", "baz"}
		);
		for (Map.Entry<CharSequence, String> ent : map) {
			Assert.assertEquals(ent.getKey(), ent.getValue());
		}
		FilteredStringOrderedMap<String> fil = new FilteredStringOrderedMap<>(
			CharFilter.getOrCreate("LetterOnlyCaseInsensitive", Character::isLetter, Character::toUpperCase),
			new String[]{"foo42", "bar666", "baz9001"},
			new String[]{"foo", "bar", "baz"}
		);
		for (Map.Entry<String, String> ent : fil) {
			Assert.assertTrue(fil.equate(ent.getKey(), ent.getValue()));
		}
		fil = new FilteredStringOrderedMap<>(
			CharFilter.getOrCreate("LetterOnlyCaseInsensitive", CharPredicates.IS_LETTER, Casing::caseUp),
			new String[]{"foo42", "bar666", "baz9001"},
			new String[]{"foo", "bar", "baz"}
		);
		for (Map.Entry<String, String> ent : fil) {
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
		Assert.assertEquals(5, synonyms.getAt(0).size());
		Assert.assertEquals(5, syn2.getAt(0).size());
		ObjectObjectOrderedMap.OrderedMapEntries<CharSequence, ObjectList<String>> es = new ObjectObjectOrderedMap.OrderedMapEntries<>(syn2);
		Iterator<Map.Entry<CharSequence, ObjectList<String>>> it = es.iterator();
		while (it.hasNext()) {
			Map.Entry<CharSequence, ObjectList<String>> ent = it.next();
			System.out.print(ent.getKey());
			System.out.print(": ");
			System.out.println(ent.getValue());
		}
		it = es.iterator();
		Assert.assertTrue(it.hasNext());
		while (it.hasNext()) {
			Map.Entry<CharSequence, ObjectList<String>> ent = it.next();
			System.out.print(ent.getKey());
			System.out.print(": ");
			System.out.println(ent.getValue());
		}
	}

	@Test
	public void testSet() {
		String[] items = new String[]{"FOO", "foo", "BAR", "BAZ", "bar", "baz"};
		CaseInsensitiveSet set = CaseInsensitiveSet.with(items);
		Assert.assertEquals(3, set.size());
		CharFilter filter = CharFilter.getOrCreate("LetterOnlyCaseInsensitive", CharPredicates.IS_LETTER, Casing::caseUp);
		FilteredStringSet fil = FilteredStringSet.with(filter, items);
		Assert.assertEquals(3, fil.size());
		Assert.assertEquals(set, fil);
	}

	@Test
	public void testOrderedSet() {
		String[] items = new String[]{"FOO", "foo", "BAR", "BAZ", "bar", "baz"};
		CaseInsensitiveOrderedSet set = CaseInsensitiveOrderedSet.with(items);
		Assert.assertEquals(3, set.size());
		CharFilter filter = CharFilter.getOrCreate("LetterOnlyCaseInsensitive", CharPredicates.IS_LETTER, Casing::caseUp);
		FilteredStringOrderedSet fil = FilteredStringOrderedSet.with(filter, items);
		Assert.assertEquals(3, fil.size());
		Assert.assertEquals(set, fil);
	}
}

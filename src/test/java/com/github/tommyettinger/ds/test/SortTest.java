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

import com.github.tommyettinger.ds.ObjectDeque;
import com.github.tommyettinger.ds.ObjectList;
import com.github.tommyettinger.ds.ObjectOrderedSet;
import com.github.tommyettinger.ds.QuickSelect;
import com.github.tommyettinger.ds.Select;
import com.github.tommyettinger.ds.support.sort.FilteredComparators;
import com.github.tommyettinger.ds.support.sort.ObjectComparators;
import com.github.tommyettinger.random.DistinctRandom;
import com.github.tommyettinger.ds.support.sort.NaturalTextComparator;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;
import java.util.Comparator;

public class SortTest {
	@Test
	public void testObjectListNaturalSort() {
		ObjectList<String> list = ObjectList.with("zed", "annex", "glee", "baleful");
		list.sort();
		System.out.println(list);
	}

	@Test
	public void testObjectListSpecifiedSort() {
		ObjectList<String> list = ObjectList.with("ZED", "annex", "glee", "Baleful");
		list.sort(String.CASE_INSENSITIVE_ORDER);
		System.out.println(list);
	}

	@Test
	public void testNaturalTextSort() {
		// Tommy is me! Satchmo is my cat!
		ObjectList<String> list = ObjectList.with("tommy 1", "tommy 2", "tommy -1", "tommy -2", "satchmo1", "satchmo9000", "satchmo10000");
		list.sort(NaturalTextComparator.CASE_SENSITIVE);
		System.out.println(list);
		list.addAll(new String[]{"Tommy0", "Tommy3", "Tommy10", "Tommy21", "Satchmo0", "Satchmo9001", "Satchmo10001"});
		list.sort(NaturalTextComparator.CASE_INSENSITIVE);
		System.out.println(list);
	}

	@Test
	public void testFilteredStringSort() {
		Comparator<String> comp = FilteredComparators.makeStringComparator(Character::isLetterOrDigit, Character::toUpperCase);
		// Tommy is me! Satchmo is my cat!
		ObjectList<String> list = ObjectList.with("Tommy 2", "tommy    3", "tommy -1", "tommy_0", "TOMMY! 4!!!", "satchmo1", "satchmo9000", "satchmo10000");
		list.sort(comp);
		System.out.println(list);
		list.sort(String.CASE_INSENSITIVE_ORDER);
		System.out.println(list);
		list.addAll(new String[]{"Tommy0", "Tommy3", "Tommy10", "Tommy21", "Satchmo0", "Satchmo9001", "Satchmo10001"});
		list.sort(comp);
		System.out.println(list);
		list.sort(String.CASE_INSENSITIVE_ORDER);
		System.out.println(list);
	}

	@Test
	public void testFilteredObjectSort() {
		Comparator<Iterable<String>> comp = FilteredComparators.makeComparator(String::compareTo, (String s) -> s.matches(".*[er]$"), s -> s);
		ObjectList<Collection<String>> list = ObjectList.with(
			ObjectList.with("whether you're a mother".split(" ")),
			ObjectOrderedSet.with("or whether you're a brother".split(" ")),
			ObjectDeque.with("you're stayin' alive".split(" ")),
			ObjectList.with("stayin' alive".split(" "))
		);
		list.sort(comp);
		System.out.println(list);
	}

	@Test
	public void testQuickSelect() {
		// 20 words, a-t
		String[] words = {"anteater", "bee", "cat", "dog", "elephant", "frog", "gibbon", "horse", "ibex", "jaguar", "koala", "lemur", "mouse", "nuthatch", "okapi", "penguin", "quahog", "ram", "squirrel", "thrush"};
//			, "unicorn"
		DistinctRandom random = new DistinctRandom(123456L);
		random.shuffle(words);
		Comparator<String> comp = String::compareTo;
		Assert.assertEquals("cat", Select.select(words, comp, 3, 20));
		Assert.assertEquals("anteater", Select.select(words, comp, 1, 20));
		Assert.assertEquals("bee", Select.select(words, comp, 2, 20));
		Assert.assertEquals("squirrel", Select.select(words, ObjectComparators.oppositeComparator(comp), 2, 20));
		random.shuffle(words);
		QuickSelect.multiSelect(words, comp, 7);
		for (int row = 0, i = 0; row < 3; row++) {
			for (int col = 0; col < 7 && i < words.length; col++) {
				System.out.print(words[i++] + ", ");
			}
			System.out.println();
		}
	}
}

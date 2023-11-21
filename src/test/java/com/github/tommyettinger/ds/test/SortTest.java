package com.github.tommyettinger.ds.test;

import com.github.tommyettinger.ds.ObjectDeque;
import com.github.tommyettinger.ds.ObjectList;
import com.github.tommyettinger.ds.ObjectOrderedSet;
import com.github.tommyettinger.ds.QuickSelect;
import com.github.tommyettinger.ds.Select;
import com.github.tommyettinger.ds.support.sort.FilteredComparators;
import com.github.tommyettinger.random.DistinctRandom;
import com.github.tommyettinger.ds.support.sort.NaturalTextComparator;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SortTest {
	@Test
	public void testObjectListNaturalSort () {
		ObjectList<String> list = ObjectList.with("zed", "annex", "glee", "baleful");
		list.sort();
		System.out.println(list);
	}

	@Test
	public void testObjectListSpecifiedSort () {
		ObjectList<String> list = ObjectList.with("ZED", "annex", "glee", "Baleful");
		list.sort(String.CASE_INSENSITIVE_ORDER);
		System.out.println(list);
	}

	@Test
	public void testNaturalTextSort () {
		// Tommy is me! Satchmo is my cat!
		ObjectList<String> list = ObjectList.with("tommy 1", "tommy 2", "tommy -1", "tommy -2", "satchmo1", "satchmo9000", "satchmo10000");
		list.sort(NaturalTextComparator.CASE_SENSITIVE);
		System.out.println(list);
		list.addAll(new String[] {"Tommy0", "Tommy3", "Tommy10", "Tommy21", "Satchmo0", "Satchmo9001", "Satchmo10001"});
		list.sort(NaturalTextComparator.CASE_INSENSITIVE);
		System.out.println(list);
	}

	@Test
	public void testFilteredStringSort () {
		Comparator<String> comp = FilteredComparators.makeStringComparator(Character::isLetterOrDigit, Character::toUpperCase);
		// Tommy is me! Satchmo is my cat!
		ObjectList<String> list = ObjectList.with("Tommy 2", "tommy    3", "tommy -1", "tommy_0", "TOMMY! 4!!!", "satchmo1", "satchmo9000", "satchmo10000");
		list.sort(comp);
		System.out.println(list);
		list.sort(String.CASE_INSENSITIVE_ORDER);
		System.out.println(list);
		list.addAll(new String[] {"Tommy0", "Tommy3", "Tommy10", "Tommy21", "Satchmo0", "Satchmo9001", "Satchmo10001"});
		list.sort(comp);
		System.out.println(list);
		list.sort(String.CASE_INSENSITIVE_ORDER);
		System.out.println(list);
	}

	@Test
	public void testFilteredObjectSort () {
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
	public void testQuickSelect () {
		// 20 words, a-t
		String[] words = {"anteater", "bee", "cat", "dog", "elephant", "frog", "gibbon", "horse", "ibex", "jaguar", "koala", "lemur", "mouse", "nuthatch", "okapi", "penguin", "quahog", "ram", "squirrel", "thrush"};
//			, "unicorn"
		DistinctRandom random = new DistinctRandom(123456L);
		random.shuffle(words);
		Assert.assertEquals("cat", Select.select(words, Comparator.naturalOrder(), 3, 20));
		Assert.assertEquals("anteater", Select.select(words, Comparator.naturalOrder(), 1, 20));
		Assert.assertEquals("bee", Select.select(words, Comparator.naturalOrder(), 2, 20));
		Assert.assertEquals("squirrel", Select.select(words, Collections.reverseOrder(), 2, 20));
		random.shuffle(words);
		QuickSelect.multiSelect(words, Comparator.naturalOrder(), 7);
		for (int row = 0, i = 0; row < 3; row++) {
			for (int col = 0; col < 7 && i < words.length; col++) {
				System.out.print(words[i++] + ", ");
			}
			System.out.println();
		}
	}
}
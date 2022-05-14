package com.github.tommyettinger.ds.test;

import com.github.tommyettinger.ds.ObjectList;
import com.github.tommyettinger.ds.QuickSelect;
import com.github.tommyettinger.ds.Select;
import com.github.tommyettinger.random.DistinctRandom;
import com.github.tommyettinger.ds.support.sort.NaturalTextComparator;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.Comparator;

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
		ObjectList<String> list = ObjectList.with("tommy1", "tommy2", "tommy11", "tommy22", "satchmo1", "satchmo9000", "satchmo10000");
		list.sort(NaturalTextComparator.CASE_SENSITIVE);
		System.out.println(list);
		list.addAll(new String[] {"Tommy0", "Tommy3", "Tommy10", "Tommy21", "Satchmo0", "Satchmo9001", "Satchmo10001"});
		list.sort(NaturalTextComparator.CASE_INSENSITIVE);
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
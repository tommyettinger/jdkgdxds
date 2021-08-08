package com.github.tommyettinger.ds.test;

import com.github.tommyettinger.ds.ObjectList;
import com.github.tommyettinger.ds.support.sort.NaturalTextComparator;
import org.junit.Test;

public class SortTest {
	@Test
	public void testObjectListNaturalSort(){
		ObjectList<String> list = ObjectList.with("zed", "annex", "glee", "baleful");
		list.sort();
		System.out.println(list);
	}
	@Test
	public void testObjectListSpecifiedSort(){
		ObjectList<String> list = ObjectList.with("ZED", "annex", "glee", "Baleful");
		list.sort(String.CASE_INSENSITIVE_ORDER);
		System.out.println(list);
	}
	@Test
	public void testNaturalTextSort(){
		// Tommy is me! Satchmo is my cat!
		ObjectList<String> list = ObjectList.with("tommy1", "tommy2", "tommy11", "tommy22", "satchmo1", "satchmo9000", "satchmo10000");
		list.sort(NaturalTextComparator.CASE_SENSITIVE);
		System.out.println(list);
		list.addAll(new String[]{"Tommy0", "Tommy3", "Tommy10", "Tommy21", "Satchmo0", "Satchmo9001", "Satchmo10001"});
		list.sort(NaturalTextComparator.CASE_INSENSITIVE);
		System.out.println(list);
	}
}

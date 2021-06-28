package com.github.tommyettinger.ds.test;

import com.github.tommyettinger.ds.ObjectList;
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
}

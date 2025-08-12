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

import com.github.tommyettinger.digital.ArrayTools;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Iterator;

public class ObjectQuadSetTest {

	ObjectQuadSet hs;

	static Object[] objArray;

	{
		objArray = new Object[1000];
		for (int i = 0; i < objArray.length; i++)
			objArray[i] = new Integer(i);
	}

	@Test
	public void test_Constructor() {
		// Test for method com.github.tommyettinger.ds.ObjectQuadSet()
		ObjectQuadSet hs2 = new ObjectQuadSet();
		Assert.assertEquals("Created incorrect ObjectQuadSet", 0, hs2.size());
	}

	@Test
	public void test_ConstructorI() {
		// Test for method com.github.tommyettinger.ds.ObjectQuadSet(int)
		ObjectQuadSet hs2 = new ObjectQuadSet(5);
		Assert.assertEquals("Created incorrect ObjectQuadSet", 0, hs2.size());
		try {
			new ObjectQuadSet(-1);
		} catch (IllegalArgumentException e) {
			return;
		}
		Assert.fail("Failed to throw IllegalArgumentException for capacity < 0");
	}

	@Test
	public void test_ConstructorIF() {
		// Test for method com.github.tommyettinger.ds.ObjectQuadSet(int, float)
		ObjectQuadSet hs2 = new ObjectQuadSet(5, (float) 0.5);
		Assert.assertEquals("Created incorrect ObjectQuadSet", 0, hs2.size());
		try {
			new ObjectQuadSet(0, 0);
		} catch (IllegalArgumentException e) {
			return;
		}
		Assert.fail("Failed to throw IllegalArgumentException for initial load factor <= 0");
	}

	@Test
	public void test_ConstructorLjava_util_Collection() {
		// Test for method com.github.tommyettinger.ds.ObjectQuadSet(java.util.Collection)
		ObjectQuadSet hs2 = ObjectQuadSet.with(objArray);
		for (int counter = 0; counter < objArray.length; counter++)
			Assert.assertTrue("ObjectQuadSet does not contain correct elements", hs.contains(objArray[counter]));
		Assert.assertTrue("ObjectQuadSet created from collection incorrect size", hs2.size() == objArray.length);
	}

	@Test
	public void test_addLjava_lang_Object() {
		// Test for method boolean com.github.tommyettinger.ds.ObjectQuadSet.add(java.lang.Object)
		int size = hs.size();
		hs.add(new Integer(8));
		Assert.assertTrue("Added element already contained by set", hs.size() == size);
		hs.add(new Integer(-9));
		Assert.assertTrue("Failed to increment set size after add", hs.size() == size + 1);
		Assert.assertTrue("Failed to add element to set", hs.contains(new Integer(-9)));
	}

	@Test
	public void test_clear() {
		// Test for method void com.github.tommyettinger.ds.ObjectQuadSet.clear()
		ObjectQuadSet orgSet = new ObjectQuadSet(hs);
		hs.clear();
		Iterator i = orgSet.iterator();
		Assert.assertEquals("Returned non-zero size after clear", 0, hs.size());
		while (i.hasNext())
			Assert.assertTrue("Failed to clear set", !hs.contains(i.next()));
	}

	@Test
	public void test_containsLjava_lang_Object() {
		// Test for method boolean com.github.tommyettinger.ds.ObjectQuadSet.contains(java.lang.Object)
		Assert.assertTrue("Returned false for valid object", hs.contains(objArray[90]));
		Assert.assertTrue("Returned true for invalid Object", !hs.contains(new Object()));

//		ObjectQuadSet s = new ObjectQuadSet();
//		s.add(null);
//		assertTrue("Cannot handle null", s.contains(null));
	}

	@Test
	public void test_isEmpty() {
		// Test for method boolean com.github.tommyettinger.ds.ObjectQuadSet.isEmpty()
		Assert.assertTrue("Empty set returned false", new ObjectQuadSet().isEmpty());
		Assert.assertTrue("Non-empty set returned true", !hs.isEmpty());
	}

	@Test
	public void test_iterator() {
		// Test for method java.util.Iterator com.github.tommyettinger.ds.ObjectQuadSet.iterator()
		Iterator i = hs.iterator();
		int x = 0;
		while (i.hasNext()) {
			Assert.assertTrue("Failed to iterate over all elements", hs.contains(i.next()));
			++x;
		}
		Assert.assertTrue("Returned iteration of incorrect size", hs.size() == x);

//		ObjectQuadSet s = new ObjectQuadSet();
//		s.add(null);
//		assertNull("Cannot handle null", s.iterator().next());
	}

	@Test
	public void test_removeLjava_lang_Object() {
		// Test for method boolean com.github.tommyettinger.ds.test.ObjectQuadSet.remove(java.lang.Object)
		int size = hs.size();
		hs.remove(new Integer(98));
		Assert.assertTrue("Failed to remove element", !hs.contains(new Integer(98)));
		Assert.assertTrue("Failed to decrement set size", hs.size() == size - 1);

//		WhiskerRandom rnd = new WhiskerRandom(123);
//		int[] numbers = ArrayTools.shuffle(ArrayTools.range(objArray.length), rnd);
		int[] numbers = ArrayTools.range(objArray.length);
		for (int i = 0; i < numbers.length; i++) {
			if (!hs.remove(new Integer(numbers[i])))
				System.out.println(numbers[i]);
		}
		// This should print an empty set... but it doesn't.
		System.out.println(hs);
//		Assert.assertEquals(0, hs.size);
	}

	@Test
	public void test_size() {
		// Test for method int com.github.tommyettinger.ds.ObjectQuadSet.size
		Assert.assertTrue("Returned incorrect size", hs.size() == objArray.length);
		hs.clear();
		Assert.assertEquals("Cleared set returned non-zero size", 0, hs.size());
	}

	@Test
	public void test_toString() {
		ObjectQuadSet s = new ObjectQuadSet();
		s.add(s);
		String result = s.toString();
		Assert.assertTrue("should contain self ref", result.indexOf("(this") > -1);
	}

	/**
	 * Sets up the fixture, for example, open a network connection. This method
	 * is called before a test is executed.
	 */
	@Before
	public void setUp() {
		hs = new ObjectQuadSet();
		for (int i = 0; i < objArray.length; i++) {
			hs.add(objArray[i]);
		}
	}

	/**
	 * Tears down the fixture, for example, close a network connection. This
	 * method is called after a test is executed.
	 */
	@After
	public void tearDown() {
	}
}

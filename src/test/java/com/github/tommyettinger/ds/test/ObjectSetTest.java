/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.github.tommyettinger.ds.test;

import com.github.tommyettinger.ds.ObjectSet;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Iterator;

public class ObjectSetTest {

	ObjectSet hs;

	static Object[] objArray;

	{
		objArray = new Object[1000];
		for (int i = 0; i < objArray.length; i++)
			objArray[i] = new Integer(i);
	}

	@Test public void test_Constructor () {
		// Test for method com.github.tommyettinger.merry.ObjectSet()
		ObjectSet hs2 = new ObjectSet();
		Assert.assertEquals("Created incorrect ObjectSet", 0, hs2.size());
	}

	@Test public void test_ConstructorI () {
		// Test for method com.github.tommyettinger.merry.ObjectSet(int)
		ObjectSet hs2 = new ObjectSet(5);
		Assert.assertEquals("Created incorrect ObjectSet", 0, hs2.size());
		try {
			new ObjectSet(-1);
		} catch (IllegalArgumentException e) {
			return;
		}
		Assert.fail("Failed to throw IllegalArgumentException for capacity < 0");
	}

	@Test public void test_ConstructorIF () {
		// Test for method com.github.tommyettinger.merry.ObjectSet(int, float)
		ObjectSet hs2 = new ObjectSet(5, (float)0.5);
		Assert.assertEquals("Created incorrect ObjectSet", 0, hs2.size());
		try {
			new ObjectSet(0, 0);
		} catch (IllegalArgumentException e) {
			return;
		}
		Assert.fail("Failed to throw IllegalArgumentException for initial load factor <= 0");
	}

	@Test public void test_ConstructorLjava_util_Collection () {
		// Test for method com.github.tommyettinger.merry.ObjectSet(java.util.Collection)
		ObjectSet hs2 = ObjectSet.with(objArray);
		for (int counter = 0; counter < objArray.length; counter++)
			Assert.assertTrue("ObjectSet does not contain correct elements", hs.contains(objArray[counter]));
		Assert.assertTrue("ObjectSet created from collection incorrect size", hs2.size() == objArray.length);
	}

	@Test public void test_addLjava_lang_Object () {
		// Test for method boolean com.github.tommyettinger.merry.ObjectSet.add(java.lang.Object)
		int size = hs.size();
		hs.add(new Integer(8));
		Assert.assertTrue("Added element already contained by set", hs.size() == size);
		hs.add(new Integer(-9));
		Assert.assertTrue("Failed to increment set size after add", hs.size() == size + 1);
		Assert.assertTrue("Failed to add element to set", hs.contains(new Integer(-9)));
	}

	@Test public void test_clear () {
		// Test for method void com.github.tommyettinger.merry.ObjectSet.clear()
		ObjectSet orgSet = new ObjectSet(hs);
		hs.clear();
		Iterator i = orgSet.iterator();
		Assert.assertEquals("Returned non-zero size after clear", 0, hs.size());
		while (i.hasNext())
			Assert.assertTrue("Failed to clear set", !hs.contains(i.next()));
	}

	@Test public void test_containsLjava_lang_Object () {
		// Test for method boolean com.github.tommyettinger.merry.ObjectSet.contains(java.lang.Object)
		Assert.assertTrue("Returned false for valid object", hs.contains(objArray[90]));
		Assert.assertTrue("Returned true for invalid Object", !hs.contains(new Object()));

//		ObjectSet s = new ObjectSet();
//		s.add(null);
//		assertTrue("Cannot handle null", s.contains(null));
	}

	@Test public void test_isEmpty () {
		// Test for method boolean com.github.tommyettinger.merry.ObjectSet.isEmpty()
		Assert.assertTrue("Empty set returned false", new ObjectSet().isEmpty());
		Assert.assertTrue("Non-empty set returned true", !hs.isEmpty());
	}

	@Test public void test_iterator () {
		// Test for method java.util.Iterator com.github.tommyettinger.merry.ObjectSet.iterator()
		Iterator i = hs.iterator();
		int x = 0;
		while (i.hasNext()) {
			Assert.assertTrue("Failed to iterate over all elements", hs.contains(i.next()));
			++x;
		}
		Assert.assertTrue("Returned iteration of incorrect size", hs.size() == x);

//		ObjectSet s = new ObjectSet();
//		s.add(null);
//		assertNull("Cannot handle null", s.iterator().next());
	}

	@Test public void test_removeLjava_lang_Object () {
		// Test for method boolean com.github.tommyettinger.merry.ObjectSet.remove(java.lang.Object)
		int size = hs.size();
		hs.remove(new Integer(98));
		Assert.assertTrue("Failed to remove element", !hs.contains(new Integer(98)));
		Assert.assertTrue("Failed to decrement set size", hs.size() == size - 1);

//		ObjectSet s = new ObjectSet();
//		s.add(null);
//		assertTrue("Cannot handle null", s.remove(null));
	}

	@Test public void test_size () {
		// Test for method int com.github.tommyettinger.merry.ObjectSet.size
		Assert.assertTrue("Returned incorrect size", hs.size() == objArray.length);
		hs.clear();
		Assert.assertEquals("Cleared set returned non-zero size", 0, hs.size());
	}

	@Test public void test_toString () {
		ObjectSet s = new ObjectSet();
		s.add(s);
		String result = s.toString();
		Assert.assertTrue("should contain self ref", result.indexOf("(this") > -1);
	}

	/**
	 * Sets up the fixture, for example, open a network connection. This method
	 * is called before a test is executed.
	 */
	@Before public void setUp () {
		hs = new ObjectSet();
		for (int i = 0; i < objArray.length; i++)
			hs.add(objArray[i]);
//		hs.add(null);
	}

	/**
	 * Tears down the fixture, for example, close a network connection. This
	 * method is called after a test is executed.
	 */
	@After public void tearDown () {
	}
}

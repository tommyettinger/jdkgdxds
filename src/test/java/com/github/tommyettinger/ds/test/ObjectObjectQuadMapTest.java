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

import com.github.tommyettinger.ds.ObjectList;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class ObjectObjectQuadMapTest {

	ObjectObjectQuadMap hm;

	final static int hmSize = 1000;

	static Object[] objArray;

	static Object[] objArray2;

	static {
		objArray = new Object[hmSize];
		objArray2 = new Object[hmSize];
		for (int i = 0; i < objArray.length; i++) {
			objArray[i] = i;
			objArray2[i] = objArray[i].toString();
		}
	}
	public void runBattery(Map<String, String> map) {
		try {
			map.put("one", "1");
			assertEquals("size should be one", 1, map.size());
			map.clear();
			assertEquals("size should be zero", 0, map.size());
			assertTrue("Should not have entries", !map.entrySet().iterator()
					.hasNext());
			assertTrue("Should not have keys", !map.keySet().iterator()
					.hasNext());
			assertTrue("Should not have values", !map.values().iterator()
					.hasNext());
		} catch (UnsupportedOperationException e) {
		}

		try {
			map.put("one", "1");
			assertEquals("size should be one", 1, map.size());
			map.remove("one");
			assertEquals("size should be zero", 0, map.size());
			assertTrue("Should not have entries", !map.entrySet().iterator()
					.hasNext());
			assertTrue("Should not have keys", !map.keySet().iterator()
					.hasNext());
			assertTrue("Should not have values", !map.values().iterator()
					.hasNext());
		} catch (UnsupportedOperationException e) {
		}
	}

	@Test
	public void test_Constructor () {
		// Test for method com.github.tommyettinger.ds.ObjectObjectQuadMap()
		runBattery(new ObjectObjectQuadMap<String, String>());

		ObjectObjectQuadMap hm2 = new ObjectObjectQuadMap<>();
		assertEquals("Created incorrect ObjectObjectQuadMap", 0, hm2.size());
	}

	@Test public void test_ConstructorI () {
		// Test for method com.github.tommyettinger.ds.ObjectObjectQuadMap(int)
		ObjectObjectQuadMap hm2 = new ObjectObjectQuadMap(5);
		assertEquals("Created incorrect ObjectObjectQuadMap", 0, hm2.size());
		do{
			try {
				new ObjectObjectQuadMap(-1);
			} catch (IllegalArgumentException e) {
				break;
			}
			Assert.fail("Failed to throw IllegalArgumentException for initial capacity < 0");
		}while (false);

		ObjectObjectQuadMap empty = new ObjectObjectQuadMap(0);
		Assert.assertNull("Empty hashmap access", empty.get("nothing"));
		empty.put("something", "here");
		assertTrue("cannot get element", empty.get("something") == "here");
	}

	@Test public void test_ConstructorIF () {
		// Test for method com.github.tommyettinger.ds.ObjectObjectQuadMap(int, float)
		ObjectObjectQuadMap hm2 = new ObjectObjectQuadMap(5, (float)0.5);
		assertEquals("Created incorrect ObjectObjectQuadMap", 0, hm2.size());
		do{
			try {
				new ObjectObjectQuadMap(0, 0);
			} catch (IllegalArgumentException e) {
				break;
			}
			Assert.fail("Failed to throw IllegalArgumentException for initial load factor <= 0");
		}while (false);

		ObjectObjectQuadMap empty = new ObjectObjectQuadMap(0, 0.75f);
		Assert.assertNull("Empty hashtable access", empty.get("nothing"));
		empty.put("something", "here");
		assertTrue("cannot get element", empty.get("something") == "here");
	}

	@Test public void test_ConstructorLjava_util_Map () {
		HashMap myMap = new HashMap();
		for (int counter = 0; counter < hmSize; counter++)
			myMap.put(objArray2[counter], objArray[counter]);
		ObjectObjectQuadMap hm2 = new ObjectObjectQuadMap(myMap);
		for (int counter = 0; counter < hmSize; counter++)
			assertTrue("Failed to construct correct ObjectObjectQuadMap",
					hm.get(objArray2[counter]) == hm2.get(objArray2[counter]));

//        try {
//            ObjectObjectQuadMap mockMap = new MockMapNull();
//            hm = new ObjectObjectQuadMap(mockMap);
//            fail("Should throw NullPointerException");
//        } catch (NullPointerException e) {
//            //empty
//        }

		ObjectObjectQuadMap map = new ObjectObjectQuadMap();
		map.put("a", "a");
		SubMap map2 = new SubMap(map);
		assertTrue(map2.containsKey("a"));
		assertTrue(map2.containsValue("a", false));
	}

	@Test public void test_clear () {
		hm.clear();
		assertEquals("Clear failed to reset size", 0, hm.size());
		for (int i = 0; i < hmSize; i++)
			Assert.assertNull("Failed to clear all elements", hm.get(objArray2[i]));

		// Check clear on a large loaded map of Integer keys
		ObjectObjectQuadMap<Integer, String> map = new ObjectObjectQuadMap<Integer, String>();
		for (int i = -32767; i < 32768; i++) {
			map.put(i, "foobar");
		}
		map.clear();
		assertEquals("Failed to reset size on large integer map", 0, hm.size());
		for (int i = -32767; i < 32768; i++) {
			Assert.assertNull("Failed to clear integer map values", map.get(i));
		}
	}

	@Test public void test_containsKeyLjava_lang_Object () {
		// Test for method boolean
		// com.github.tommyettinger.ds.ObjectObjectQuadMap.containsKey(java.lang.Object)
		assertTrue("Returned false for valid key", hm.containsKey(new Integer(876).toString()));
		assertTrue("Returned true for invalid key", !hm.containsKey("KKDKDKD"));

//		ObjectObjectQuadMap m = new ObjectObjectQuadMap();
//		m.put(null, "test");
//		assertTrue("Failed with null key", m.containsKey(null));
//		assertTrue("Failed with missing key matching null hash", !m
//				.containsKey(new Integer(0)));
	}

	@Test public void test_containsValueLjava_lang_Object () {
		// Test for method boolean
		// com.github.tommyettinger.ds.ObjectObjectQuadMap.containsValue(java.lang.Object)
		assertTrue("Returned false for valid value", hm.containsValue(new Integer(875), false));
		assertTrue("Returned true for invalid value", !hm.containsValue(new Integer(-9), false));
	}

	@Test public void test_entrySet () {
		// Test for method java.util.Set com.github.tommyettinger.ds.ObjectObjectQuadMap.entrySet(
		Set<Map.Entry> s = hm.entrySet();
		Iterator i = s.iterator();
		assertTrue("Returned set of incorrect size", hm.size() == s.size());
		while (i.hasNext()) {
			ObjectObjectQuadMap.Entry m = (ObjectObjectQuadMap.Entry)i.next();
			assertTrue("Returned incorrect entry set", hm.containsKey(m.key) && hm.containsValue(m.value, false));
		}

		Iterator<Map.Entry> iter = new ObjectObjectQuadMap.Entries<>(hm).iterator();
		s.remove(iter.next());
		assertEquals(1001, s.size());
	}

	@Test public void test_getLjava_lang_Object () {
		// Test for method java.lang.Object
		// com.github.tommyettinger.ds.ObjectObjectQuadMap.get(java.lang.Object)
		Assert.assertNull("Get returned non-null for non existent key", hm.get("T"));
		hm.put("T", "HELLO");
		assertEquals("Get returned incorrect value for existing key", "HELLO", hm.get("T"));

//		ObjectObjectQuadMap m = new ObjectObjectQuadMap();
//		m.put(null, "test");
//		assertEquals("Failed with null key", "test", m.get(null));
//		assertNull("Failed with missing key matching null hash", m
//				.get(new Integer(0)));

		// Regression for HARMONY-206
		ReusableKey k = new ReusableKey();
		ObjectObjectQuadMap map = new ObjectObjectQuadMap();
		k.setKey(1);
		map.put(k, "value1");

		k.setKey(18);
		Assert.assertNull(map.get(k));

		k.setKey(17);
		Assert.assertNull(map.get(k));
	}

//	/**
//	 * Tests for proxy object keys and values
//	 */
//	public void test_proxies() {
//        // Regression for HARMONY-6237
//        MockInterface proxyKey = (MockInterface) Proxy.newProxyInstance(
//                MockInterface.class.getClassLoader(),
//                new Class[] { MockInterface.class }, new MockHandler(
//                        new MockClass()));
//        MockInterface proxyValue = (MockInterface) Proxy.newProxyInstance(
//                MockInterface.class.getClassLoader(),
//                new Class[] { MockInterface.class }, new MockHandler(
//                        new MockClass()));
//
//        // Proxy key
//        Object val = new Object();
//        hm.put(proxyKey, val);
//
//        assertEquals("Failed with proxy object key", val, hm
//                .get(proxyKey));
//        assertTrue("Failed to find proxy key", hm.containsKey(proxyKey));
//        assertEquals("Failed to remove proxy object key", val,
//                hm.remove(proxyKey));
//        assertFalse("Should not have found proxy key", hm.containsKey(proxyKey));
//        
//        // Proxy value
//        Object k = new Object();
//        hm.put(k, proxyValue);
//        
//        assertTrue("Failed to find proxy object as value", hm.containsValue(proxyValue));
//        
//        // Proxy key and value
//        ObjectObjectQuadMap map = new ObjectObjectQuadMap();
//        map.put(proxyKey, proxyValue);
//        assertTrue("Failed to find proxy key", map.containsKey(proxyKey));
//        assertEquals(1, map.size());
//        Object[] entries = map.entrySet().toArray();
//        Map.Entry entry = (Map.Entry)entries[0];
//        assertTrue("Failed to find proxy association", map.entrySet().contains(entry));
//	}

	@Test public void test_isEmpty () {
		// Test for method boolean com.github.tommyettinger.ds.ObjectObjectQuadMap.isEmpty()
		assertTrue("Returned false for new map", new ObjectObjectQuadMap().isEmpty());
		assertTrue("Returned true for non-empty", !hm.isEmpty());
	}

	@Test public void test_keySet () {
		// Test for method java.util.Set com.github.tommyettinger.ds.ObjectObjectQuadMap.keySet()
		Set s = hm.keySet();
		assertTrue("Returned set of incorrect size()", s.size() == hm.size());
//		for (int i = 0; i < objArray.length; i++)
//			assertTrue("Returned set does not contain all keys", s
//					.contains(objArray[i].toString()));

//		ObjectObjectQuadMap m = new ObjectObjectQuadMap();
//		m.put(null, "test");
//		assertTrue("Failed with null key", m.keySet().contains(null));
//		assertNull("Failed with null key", m.keySet().iterator().next());

		ObjectObjectQuadMap map = new ObjectObjectQuadMap(101);
		map.put(new Integer(1), "1");
		map.put(new Integer(102), "102");
		map.put(new Integer(203), "203");
		Iterator it = map.keySet().iterator();
		Integer remove1 = (Integer)it.next();
		it.hasNext();
		it.remove();
		Integer remove2 = (Integer)it.next();
		it.remove();
		ObjectList list = new ObjectList(Arrays.asList(new Integer(1), new Integer(102), new Integer(203)));
		list.remove(remove1);
		list.remove(remove2);
		assertTrue("Wrong result", it.next().equals(list.get(0)));
		assertEquals("Wrong size", 1, map.size());
		assertTrue("Wrong contents", map.keySet().iterator().next().equals(list.get(0)));

		ObjectObjectQuadMap map2 = new ObjectObjectQuadMap(101);
		map2.put(new Integer(1), "1");
		map2.put(new Integer(4), "4");
		Iterator it2 = map2.keySet().iterator();
		Integer remove3 = (Integer)it2.next();
		Integer next;
		if (remove3.intValue() == 1)
			next = new Integer(4);
		else
			next = new Integer(1);
		it2.hasNext();
		it2.remove();
		assertTrue("Wrong result 2", it2.next().equals(next));
		assertEquals("Wrong size 2", 1, map2.size());
		assertTrue("Wrong contents 2", map2.keySet().iterator().next().equals(next));
	}

	@Test public void test_putLjava_lang_ObjectLjava_lang_Object () {
		hm.put("KEY", "VALUE");
		assertEquals("Failed to install key/value pair", "VALUE", hm.get("KEY"));

//        ObjectObjectQuadMap<Object,Object> m = new ObjectObjectQuadMap<Object,Object>();
//        m.put(new Short((short) 0), "short");
//        m.put(null, "test");
//        m.put(new Integer(0), "int");
//        assertEquals("Failed adding to bucket containing null", "short", m
//                .get(new Short((short) 0)));
//        assertEquals("Failed adding to bucket containing null2", "int", m
//                .get(new Integer(0)));

		// Check my actual key instance is returned
		ObjectObjectQuadMap<Integer, String> map = new ObjectObjectQuadMap<Integer, String>();
		for (int i = -32767; i < 32768; i++) {
			map.put(i, "foobar");
		}
		Integer myKey = new Integer(0);
		// Put a new value at the old key position
		map.put(myKey, "myValue");
		assertTrue(map.containsKey(myKey));
		assertEquals("myValue", map.get(myKey));
		boolean found = false;
		for (Iterator<Integer> itr = map.keySet().iterator(); itr.hasNext(); ) {
			Integer key = itr.next();
			if (found = myKey == key) {
				break;
			}
		}
		Assert.assertFalse("Should not find new key instance in hashmap", found);

		// Add a new key instance and check it is returned
		Assert.assertNotNull(map.remove(myKey));
		map.put(myKey, "myValue");
		assertTrue(map.containsKey(myKey));
		assertEquals("myValue", map.get(myKey));
		for (Iterator<Integer> itr = map.keySet().iterator(); itr.hasNext(); ) {
			Integer key = itr.next();
			if (found = myKey == key) {
				break;
			}
		}
		assertTrue("Did not find new key instance in hashmap", found);

		// Ensure keys with identical hashcode are stored separately
		ObjectObjectQuadMap<Object, Object> objmap = new ObjectObjectQuadMap<Object, Object>();
		for (int i = 0; i < 32768; i++) {
			objmap.put(i, "foobar");
		}
		// Put non-equal object with same hashcode
		MyKey aKey = new MyKey();
		Assert.assertNull(objmap.put(aKey, "value"));
		Assert.assertNull(objmap.remove(new MyKey()));
		assertEquals("foobar", objmap.get(0));
		assertEquals("value", objmap.get(aKey));
	}

	static class MyKey {
		public MyKey () {
			super();
		}

		@Override
		public int hashCode () {
			return 0;
		}
	}

	@Test public void test_putAllLjava_util_Map () {
		// Test for method void com.github.tommyettinger.ds.ObjectObjectQuadMap.putAll(java.util.Map)
		ObjectObjectQuadMap hm2 = new ObjectObjectQuadMap();
		hm2.putAll(hm);
		for (int i = 0; i < 1000; i++)
			assertTrue("Failed to clear all elements", hm2.get(new Integer(i).toString()).equals(new Integer(i)));

//        ObjectObjectQuadMap mockMap = new MockMap();
//        hm2 = new ObjectObjectQuadMap();
//        hm2.putAll(mockMap);
//        assertEquals("Size should be 0", 0, hm2.size);
	}

//    @Test
//    public void test_putAllLjava_util_Map_Null() {
//        ObjectObjectQuadMap hashMap = new ObjectObjectQuadMap();
//        try {
//            hashMap.putAll(new MockMapNull());
//            Assert.fail("Should throw NullPointerException");
//        } catch (NullPointerException e) {
//            // expected.
//        }
//
////        try {
////            hashMap = new ObjectObjectQuadMap(new MockMapNull());
////            fail("Should throw NullPointerException");
////        } catch (NullPointerException e) {
////            // expected.
////        }
//    } 

	@Test public void test_removeLjava_lang_Object () {
		int size = hm.size();
		Integer y = new Integer(9);
		Integer x = (Integer)hm.remove(y.toString());
		assertTrue("Remove returned incorrect value", x.equals(new Integer(9)));
		Assert.assertNull("Failed to remove given key", hm.get(new Integer(9)));
		assertTrue("Failed to decrement size", hm.size() == size - 1);
		Assert.assertNull("Remove of non-existent key returned non-null", hm.remove("LCLCLC"));

//		ObjectObjectQuadMap m = new ObjectObjectQuadMap();
//		m.put(null, "test");
//		assertNull("Failed with same hash as null",
//				m.remove(new Integer(0)));
//		assertEquals("Failed with null key", "test", m.remove(null));

		ObjectObjectQuadMap<Integer, Object> map = new ObjectObjectQuadMap<Integer, Object>();
		for (int i = 0; i < 32768; i++) {
			map.put(i, "const");
		}
		Object[] values = new Object[32768];
		for (int i = 0; i < 32768; i++) {
			values[i] = Integer.toString(i);
			map.put(i, values[i]);
		}
		for (int i = 32767; i >= 0; i--) {
			Object obj = map.remove(i);
			assertEquals("Failed to remove same value", values[i], obj);
		}

		// Ensure keys with identical hashcode are removed properly
		map = new ObjectObjectQuadMap<Integer, Object>();
		for (int i = -32767; i < 32768; i++) {
			map.put(i, "foobar");
		}
		// Remove non equal object with same hashcode
//        assertNull(map.remove(new MyKey()));
		assertEquals("foobar", map.get(0));
		map.remove(0);
		Assert.assertNull(map.get(0));
	}

	/**
	 * Compatibility test to ensure we rehash the same way as the RI.
	 * Not required by the spec, but some apps seem sensitive to it.
	 */
	@Test
	@Ignore
	public void test_rehash () {
		// This map should rehash on adding the ninth element.
		ObjectObjectQuadMap<MyKey, Integer> hm = new ObjectObjectQuadMap<MyKey, Integer>(10, 0.5f);

		// Ordered set of keys.
		MyKey[] keyOrder = new MyKey[9];
		for (int i = 0; i < keyOrder.length; i++) {
			keyOrder[i] = new MyKey();
		}

		// Store eight elements
		for (int i = 0; i < 8; i++) {
			hm.put(keyOrder[i], i);
		}
		// Check expected ordering (same as adding order)
		Object[] returnedKeys = hm.keySet().toArray();
		for (int i = 0; i < 8; i++) {
			Assert.assertSame(keyOrder[i], returnedKeys[i]);
		}

		// The next put causes a rehash
		hm.put(keyOrder[8], 8);
		// Check expected new ordering (adding order)
		returnedKeys = hm.keySet().toArray();
		for (int i = 0; i < 9; i++) {
			Assert.assertSame(keyOrder[i], returnedKeys[i]);
		}
	}

	@Test public void test_size () {
		// Test for method int com.github.tommyettinger.ds.ObjectObjectQuadMap.size()
		assertTrue("Returned incorrect size", hm.size() == objArray.length + 1);
	}

	@Test public void test_values () {
		// Test for method java.util.Collection com.github.tommyettinger.ds.ObjectObjectQuadMap.values()
		Collection c = hm.values();
		assertTrue("Returned collection of incorrect size()", c.size() == hm.size());
//		for (int i = 0; i < objArray.length; i++)
//			assertTrue("Returned collection does not contain all keys", c
//					.contains(objArray[i]));

		ObjectObjectQuadMap myObjectObjectQuadMap = new ObjectObjectQuadMap();
		for (int i = 0; i < 100; i++)
			myObjectObjectQuadMap.put(objArray2[i], objArray[i]);
		Iterator values = myObjectObjectQuadMap.values().iterator();
//		new Support_UnmodifiableCollectionTest(
//				"Test Returned Collection From ObjectObjectQuadMap.values()", values)
//				.runTest();
		values.hasNext();
		Object removed = values.next();
		values.remove();
		assertTrue("Removing from the values collection should remove from the original map",
				!myObjectObjectQuadMap.containsValue(removed, false));

	}

	@Test public void test_toString () {

		ObjectObjectQuadMap m = new ObjectObjectQuadMap();
		m.put(m, m);
		String result = m.toString();
		assertTrue("should contain self ref", result.indexOf("(this") > -1);
	}

	static class ReusableKey {
		private int key = 0;

		public void setKey (int key) {
			this.key = key;
		}

		@Override
		public int hashCode () {
			return key;
		}

		@Override
		public boolean equals (Object o) {
			if (o == this) {
				return true;
			}
			if (!(o instanceof ReusableKey)) {
				return false;
			}
			return key == ((ReusableKey)o).key;
		}
	}
//    
//	public void test_Map_Entry_hashCode() {
//        //Related to HARMONY-403
//	    ObjectObjectQuadMap<Integer, Integer> map = new ObjectObjectQuadMap<Integer, Integer>(10);
//	    Integer key = new Integer(1);
//	    Integer val = new Integer(2);
//	    map.put(key, val);
//	    int expected = key.hashCode() ^ val.hashCode();
//	    assertEquals(expected, map.hashCode());
//	    key = new Integer(4);
//	    val = new Integer(8);
//	    map.put(key, val);
//	    expected += key.hashCode() ^ val.hashCode();
//	    assertEquals(expected, map.hashCode());
//	}

	/*
	 * Regression test for HY-4750
	 */
	@Test public void test_EntrySet () {
//        ObjectObjectQuadMap map = new ObjectObjectQuadMap();
//        map.put(new Integer(1), "ONE");

//        ObjectObjectQuadMap.Entries entrySet = map.entries();
//        Iterator e = entrySet.iterator();
//        Object real = e.next();
//        ObjectObjectQuadMap.Entry copyEntry = new MockEntry();
//        assertEquals(real, copyEntry);
		//assertTrue(entrySet.contains(copyEntry));

		//entrySet.remove(copyEntry);
		//assertFalse(entrySet.contains(copyEntry));
	}

	/**
	 * Sets up the fixture, for example, open a network connection. This method
	 * is called before a test is executed.
	 */
	@Before
	public void setUp () {
		hm = new ObjectObjectQuadMap();
		for (int i = 0; i < objArray.length; i++)
			hm.put(objArray2[i], objArray[i]);
		hm.put("test", null);
//		hm.put(null, "test");
	}

	class SubMap<K, V> extends ObjectObjectQuadMap<K, V> {
		public SubMap (ObjectObjectQuadMap<? extends K, ? extends V> m) {
			super(m);
		}

		@Override
		@Nullable
		public V put (@NonNull K key, @Nullable V value) {
			throw new UnsupportedOperationException();
		}
	}

}

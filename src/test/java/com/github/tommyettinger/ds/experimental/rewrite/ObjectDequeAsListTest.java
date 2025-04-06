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
package com.github.tommyettinger.ds.experimental.rewrite;

import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

/**
 * From <a href="https://github.com/apache/harmony">Apache Harmony's GitHub mirror</a>.
 */
public class ObjectDequeAsListTest extends junit.framework.TestCase {

    List alist;

    static Object[] objArray;
    {
        objArray = new Object[100];
        for (int i = 0; i < objArray.length; i++)
            objArray[i] = new Integer(i);
    }

    /**
     * @tests java.util.ObjectDeque#ObjectDeque()
     */
    public void test_Constructor() {
        // Test for method java.util.ObjectDeque()
        new SupportForListTest("", alist).runTest();

        ObjectDeque subList = new ObjectDeque();
        for (int i = -50; i < 150; i++)
            subList.add(new Integer(i));
        new SupportForListTest("", subList.subList(50, 150)).runTest();
    }

    /**
     * @tests java.util.ObjectDeque#ObjectDeque(int)
     */
    public void test_ConstructorI() {
        // Test for method java.util.ObjectDeque(int)
        ObjectDeque al = new ObjectDeque(5);
        assertEquals("Incorrect ObjectDeque created", 0, al.size());
        
        al = new ObjectDeque(0);
        assertEquals("Incorrect ObjectDeque created", 0, al.size());
        
        try {
            al = new ObjectDeque(-1);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Excepted
        }
    }

    /**
     * @tests java.util.ObjectDeque#ObjectDeque(java.util.Collection)
     */
    public void test_ConstructorLjava_util_Collection() {
        // Test for method java.util.ObjectDeque(java.util.Collection)
        ObjectDeque al = new ObjectDeque(Arrays.asList(objArray));
        assertTrue("ObjectDeque created from collection has incorrect size", al
                .size() == objArray.length);
        for (int counter = 0; counter < objArray.length; counter++)
            assertTrue(
                    "ObjectDeque created from collection has incorrect elements",
                    al.get(counter) == objArray[counter]);

    }

    public void testConstructorWithConcurrentCollection() {
        Collection<String> collection = shrinksOnSize("A", "B", "C", "D");
        ObjectDeque<String> list = new ObjectDeque<String>(collection);
        assertFalse(list.contains(null));
    }

    /**
     * @tests java.util.ObjectDeque#add(int, java.lang.Object)
     */
    public void test_addILjava_lang_Object() {
        // Test for method void java.util.ObjectDeque.add(int, java.lang.Object)
        Object o;
        alist.add(50, o = new Object());
        assertTrue("Failed to add Object", alist.get(50) == o);
        assertTrue("Failed to fix up list after insert",
                alist.get(51) == objArray[50]
                        && (alist.get(52) == objArray[51]));
        Object oldItem = alist.get(25);
        alist.add(25, null);
        assertNull("Should have returned null", alist.get(25));
        assertTrue("Should have returned the old item from slot 25", alist
                .get(26) == oldItem);
        
        alist.add(0, o = new Object());
        assertEquals("Failed to add Object", alist.get(0), o);
        assertEquals(alist.get(1), objArray[0]);
        assertEquals(alist.get(2), objArray[1]);

        oldItem = alist.get(0);
        alist.add(0, null);
        assertNull("Should have returned null", alist.get(0));
        assertEquals("Should have returned the old item from slot 0", alist
                .get(1), oldItem);

        try {
            alist.add(-1, new Object());
            fail("Should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // Expected
            assertNotNull(e.getMessage());
        }

        try {
            alist.add(-1, null);
            fail("Should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // Expected
            assertNotNull(e.getMessage());
        }

        try {
            alist.add(alist.size() + 1, new Object());
            fail("Should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // Expected
            assertNotNull(e.getMessage());
        }

        try {
            alist.add(alist.size() + 1, null);
            fail("Should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // Expected
            assertNotNull(e.getMessage());
        }
    }

    /**
     * @tests java.util.ObjectDeque#add(int, java.lang.Object)
     */
    public void test_addILjava_lang_Object_2() {
        Object o = new Object();
        int size = alist.size();
        alist.add(size, o);
        assertEquals("Failed to add Object", alist.get(size), o);
        assertEquals(alist.get(size - 2), objArray[size - 2]);
        assertEquals(alist.get(size - 1), objArray[size - 1]);

        alist.remove(size);

        size = alist.size();
        alist.add(size, null);
        assertNull("Should have returned null", alist.get(size));
        assertEquals(alist.get(size - 2), objArray[size - 2]);
        assertEquals(alist.get(size - 1), objArray[size - 1]);
    }
    
    /**
     * @tests java.util.ObjectDeque#add(java.lang.Object)
     */
    public void test_addLjava_lang_Object() {
        // Test for method boolean java.util.ObjectDeque.add(java.lang.Object)
        Object o = new Object();
        alist.add(o);
        assertTrue("Failed to add Object", alist.get(alist.size() - 1) == o);
        alist.add(null);
        assertNull("Failed to add null", alist.get(alist.size() - 1));
    }

    /**
     * @tests java.util.ObjectDeque#addAll(int, java.util.Collection)
     */
    public void test_addAllILjava_util_Collection() {
        // Test for method boolean java.util.ObjectDeque.addAll(int,
        // java.util.Collection)
        alist.addAll(50, alist);
        assertEquals("Returned incorrect size after adding to existing list",
                200, alist.size());
        for (int i = 0; i < 50; i++)
            assertTrue("Manipulated elements < index",
                    alist.get(i) == objArray[i]);
        for (int i = 0; i >= 50 && (i < 150); i++)
            assertTrue("Failed to ad elements properly",
                    alist.get(i) == objArray[i - 50]);
        for (int i = 0; i >= 150 && (i < 200); i++)
            assertTrue("Failed to ad elements properly",
                    alist.get(i) == objArray[i - 100]);
        ObjectDeque listWithNulls = new ObjectDeque();
        listWithNulls.add(null);
        listWithNulls.add(null);
        listWithNulls.add("yoink");
        listWithNulls.add("kazoo");
        listWithNulls.add(null);
        alist.addAll(100, listWithNulls);
        assertTrue("Incorrect size: " + alist.size(), alist.size() == 205);
        assertNull("Item at slot 100 should be null", alist.get(100));
        assertNull("Item at slot 101 should be null", alist.get(101));
        assertEquals("Item at slot 102 should be 'yoink'", "yoink", alist
                .get(102));
        assertEquals("Item at slot 103 should be 'kazoo'", "kazoo", alist
                .get(103));
        assertNull("Item at slot 104 should be null", alist.get(104));
        alist.addAll(205, listWithNulls);
        assertTrue("Incorrect size2: " + alist.size(), alist.size() == 210);
    }

    /**
     * @tests java.util.ObjectDeque#addAll(int, java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    public void test_addAllILjava_util_Collection_2() {
        // Regression for HARMONY-467
        ObjectDeque obj = new ObjectDeque();
        try {
            obj.addAll((int) -1, (Collection) null);
            fail("IndexOutOfBoundsException expected");
        } catch (IndexOutOfBoundsException e) {
            // Expected
            assertNotNull(e.getMessage());
        }

        // Regression for HARMONY-5705
        String[] data = new String[] { "1", "2", "3", "4", "5", "6", "7", "8" };
        ObjectDeque list1 = new ObjectDeque();
        ObjectDeque list2 = new ObjectDeque();
        for (String d : data) {
            list1.add(d);
            list2.add(d);
            list2.add(d);
        }
        while (list1.size() > 0)
            list1.remove(0);
        list1.addAll(list2);
        assertTrue("The object list is not the same as original list", list1
                .containsAll(list2)
                && list2.containsAll(list1));

        obj = new ObjectDeque();
        for (int i = 0; i < 100; i++) {
            if (list1.size() > 0) {
                obj.removeAll(list1);
                obj.addAll(list1);
            }
        }
        assertTrue("The object list is not the same as original list", obj
                .containsAll(list1)
                && list1.containsAll(obj));

        // Regression for Harmony-5799
        list1 = new ObjectDeque();
        list2 = new ObjectDeque();
        int location = 2;

        String[] strings = { "0", "1", "2", "3", "4", "5", "6" };
        int[] integers = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        for (int i = 0; i < 7; i++) {
            list1.add(strings[i]);
        }
        for (int i = 0; i < 10; i++) {
            list2.add(integers[i]);
        }
        list1.remove(location);
        list1.addAll(location, list2);

        // Inserted elements should be equal to integers array
        for (int i = 0; i < integers.length; i++) {
            assertEquals(integers[i], list1.get(location + i));
        }
        // Elements after inserted location should
        // be equals to related elements in strings array
        for (int i = location + 1; i < strings.length; i++) {
            assertEquals(strings[i], list1.get(i + integers.length - 1));
        }
    }
    
    /**
     * @tests java.util.ObjectDeque#addAll(int, java.util.Collection)
     */
    public void test_addAllILjava_util_Collection_3() {
        ObjectDeque obj = new ObjectDeque();
        obj.addAll(0, obj);
        obj.addAll(obj.size(), obj);
        try {
            obj.addAll(-1, obj);
            fail("Should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // Expected
            assertNotNull(e.getMessage());
        }

        try {
            obj.addAll(obj.size() + 1, obj);
            fail("Should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // Expected
            assertNotNull(e.getMessage());
        }

        try {
            obj.addAll(0, (Collection) null);
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // Excepted
        }

        try {
            obj.addAll(obj.size() + 1, (Collection) null);
            fail("Should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // Expected
            assertNotNull(e.getMessage());
        }

        try {
            obj.addAll((int) -1, (Collection) null);
            fail("IndexOutOfBoundsException expected");
        } catch (IndexOutOfBoundsException e) {
            // Expected
            assertNotNull(e.getMessage());
        }
    }

    public void test_addAllCollectionOfQextendsE() {
        // Regression for HARMONY-539
        // https://issues.apache.org/jira/browse/HARMONY-539
        ObjectDeque<String> alist = new ObjectDeque<String>();
        ObjectDeque<String> blist = new ObjectDeque<String>();
        alist.add("a");
        alist.add("b");
        blist.add("c");
        blist.add("d");
        blist.remove(0);
        blist.addAll(0, alist);
        assertEquals("a", blist.get(0));
        assertEquals("b", blist.get(1));
        assertEquals("d", blist.get(2));
    }

    /**
     * @tests java.util.ObjectDeque#addAll(java.util.Collection)
     */
    public void test_addAllLjava_util_Collection() {
        // Test for method boolean
        // java.util.ObjectDeque.addAll(java.util.Collection)
        List l = new ObjectDeque();
        l.addAll(alist);
        for (int i = 0; i < alist.size(); i++)
            assertTrue("Failed to add elements properly", l.get(i).equals(
                    alist.get(i)));
        alist.addAll(alist);
        assertEquals("Returned incorrect size after adding to existing list",
                200, alist.size());
        for (int i = 0; i < 100; i++) {
            assertTrue("Added to list in incorrect order", alist.get(i).equals(
                    l.get(i)));
            assertTrue("Failed to add to existing list", alist.get(i + 100)
                    .equals(l.get(i)));
        }
        Set setWithNulls = new HashSet();
        setWithNulls.add(null);
        setWithNulls.add(null);
        setWithNulls.add("yoink");
        setWithNulls.add("kazoo");
        setWithNulls.add(null);
        alist.addAll(100, setWithNulls);
        Iterator i = setWithNulls.iterator();
        assertTrue("Item at slot 100 is wrong: " + alist.get(100), alist
                .get(100) == i.next());
        assertTrue("Item at slot 101 is wrong: " + alist.get(101), alist
                .get(101) == i.next());
        assertTrue("Item at slot 103 is wrong: " + alist.get(102), alist
                .get(102) == i.next());

        try {
            alist.addAll(null);
            fail("Should throw NullPointerException");
        } catch (NullPointerException e) {
            // Excepted
        }
        
        // Regression test for Harmony-3481
        ObjectDeque<Integer> originalList = new ObjectDeque<Integer>(12);
        for (int j = 0; j < 12; j++) {
            originalList.add(j);
        }

        originalList.remove(0);
        originalList.remove(0);

        ObjectDeque<Integer> additionalList = new ObjectDeque<Integer>(11);
        for (int j = 0; j < 11; j++) {
            additionalList.add(j);
        }
        assertTrue(originalList.addAll(additionalList));
        assertEquals(21, originalList.size());

    }

        public void test_ArrayList_addAll_scenario1() {
        ObjectDeque arrayListA = new ObjectDeque();
        arrayListA.add(1);
        ObjectDeque arrayListB = new ObjectDeque();
        arrayListB.add(1);
        arrayListA.addAll(1, arrayListB);
        int size = arrayListA.size();
        assertEquals(2, size);
        for (int index = 0; index < size; index++) {
            assertEquals(1, arrayListA.get(index));
        }
    }

    public void test_ArrayList_addAll_scenario2() {
        {
            ObjectDeque objectDeque = new ObjectDeque();
            objectDeque.add(1);
            objectDeque.addAll(0, objectDeque);
            int size = objectDeque.size();
            assertEquals(2, size);
            for (int index = 0; index < size; index++) {
                assertEquals(1, objectDeque.get(index));
            }
        }
        {
            ObjectDeque objectDeque = new ObjectDeque();
            objectDeque.add(1);
            objectDeque.addAll(1, objectDeque);
            int size = objectDeque.size();
            assertEquals(2, size);
            for (int index = 0; index < size; index++) {
                assertEquals(1, objectDeque.get(index));
            }
        }
        {
            ObjectDeque objectDeque = new ObjectDeque();
            objectDeque.add(1);
            objectDeque.add(10);
            objectDeque.addAll(1, objectDeque);
            int size = objectDeque.size();
            assertEquals(4, size);

            for (int index = 0; index < 2; index++) {
                assertEquals(1, objectDeque.get(index));
            }
            for (int index = 2; index < 4; index++) {
                assertEquals(10, objectDeque.get(index));
            }
        }
    }
        
    // Regression test for HARMONY-5839
    public void testaddAllHarmony5839() {
        Collection coll = Arrays.asList(new String[] { "1", "2" });
        List list = new ObjectDeque();
        list.add("a");
        list.add(0, "b");
        list.add(0, "c");
        list.add(0, "d");
        list.add(0, "e");
        list.add(0, "f");
        list.add(0, "g");
        list.add(0, "h");
        list.add(0, "i");

        list.addAll(6, coll);

        assertEquals(11, list.size());
        assertFalse(list.contains(null));
    }

    /**
     * @tests java.util.ObjectDeque#clear()
     */
    public void test_clear() {
        // Test for method void java.util.ObjectDeque.clear()
        alist.clear();
        assertEquals("List did not clear", 0, alist.size());
        alist.add(null);
        alist.add(null);
        alist.add(null);
        alist.add("bam");
        alist.clear();
        assertEquals("List with nulls did not clear", 0, alist.size());
        /*
         * for (int i = 0; i < alist.size(); i++) assertNull("Failed to clear
         * list", alist.get(i));
         */

    }

    /**
     * @tests java.util.ObjectDeque#contains(java.lang.Object)
     */
    public void test_containsLjava_lang_Object() {
        // Test for method boolean
        // java.util.ObjectDeque.contains(java.lang.Object)
        assertTrue("Returned false for valid element", alist
                .contains(objArray[99]));
        assertTrue("Returned false for equal element", alist
                .contains(new Integer(8)));
        assertTrue("Returned true for invalid element", !alist
                .contains(new Object()));
        assertTrue("Returned true for null but should have returned false",
                !alist.contains(null));
        alist.add(null);
        assertTrue("Returned false for null but should have returned true",
                alist.contains(null));
    }

    /**
     * @tests java.util.ObjectDeque#ensureCapacity(int)
     */
    public void test_ensureCapacityI() {
        // Test for method void java.util.ObjectDeque.ensureCapacity(int)
        // TODO : There is no good way to test this as it only really impacts on
        // the private implementation.

        Object testObject = new Object();
        int capacity = 20;
        ObjectDeque al = new ObjectDeque(capacity);
        int i;
        for (i = 0; i < capacity / 2; i++) {
            al.add(i, new Object());
        }
        al.add(i, testObject);
        int location = al.indexOf(testObject);
        al.ensureCapacity(capacity);
        assertTrue("EnsureCapacity moved objects around in array1.",
                location == al.indexOf(testObject));
        al.remove(0);
        al.ensureCapacity(capacity);
        assertTrue("EnsureCapacity moved objects around in array2.",
                --location == al.indexOf(testObject));
        al.ensureCapacity(capacity + 2);
        assertTrue("EnsureCapacity did not change location.", location == al
                .indexOf(testObject));

        ObjectDeque<String> list = new ObjectDeque<String>(1);
        list.add("hello");
        list.ensureCapacity(Integer.MIN_VALUE);
    }

    /**
     * @tests java.util.ObjectDeque#get(int)
     */
    public void test_getI() {
        // Test for method java.lang.Object java.util.ObjectDeque.get(int)
        assertTrue("Returned incorrect element", alist.get(22) == objArray[22]);
        try {
            Object item = alist.get(8765);
            assertEquals(item, alist.get(alist.size() - 1));
        } catch (IndexOutOfBoundsException e) {
            fail("Failed to clamp bounds when index > size");
        }
        try {
            Object item = alist.get(-10);
            assertEquals(item, alist.get(0));
        } catch (IndexOutOfBoundsException e) {
            fail("Failed to clamp bounds when index < 0");
        }
    }

    /**
     * @tests java.util.ObjectDeque#indexOf(java.lang.Object)
     */
    public void test_indexOfLjava_lang_Object() {
        // Test for method int java.util.ObjectDeque.indexOf(java.lang.Object)
        assertEquals("Returned incorrect index", 87, alist
                .indexOf(objArray[87]));
        assertEquals("Returned index for invalid Object", -1, alist
                .indexOf(new Object()));
        alist.add(25, null);
        alist.add(50, null);
        assertTrue("Wrong indexOf for null.  Wanted 25 got: "
                + alist.indexOf(null), alist.indexOf(null) == 25);
    }

    /**
     * @tests java.util.ObjectDeque#isEmpty()
     */
    public void test_isEmpty() {
        // Test for method boolean java.util.ObjectDeque.isEmpty()
        assertTrue("isEmpty returned false for new list", new ObjectDeque()
                .isEmpty());
        assertTrue("Returned true for existing list with elements", !alist
                .isEmpty());
    }

    /**
     * @tests java.util.ObjectDeque#lastIndexOf(java.lang.Object)
     */
    public void test_lastIndexOfLjava_lang_Object() {
        // Test for method int java.util.ObjectDeque.lastIndexOf(java.lang.Object)
        alist.add(new Integer(99));
        assertEquals("Returned incorrect index", 100, alist
                .lastIndexOf(objArray[99]));
        assertEquals("Returned index for invalid Object", -1, alist
                .lastIndexOf(new Object()));
        alist.add(25, null);
        alist.add(50, null);
        assertTrue("Wrong lastIndexOf for null.  Wanted 50 got: "
                + alist.lastIndexOf(null), alist.lastIndexOf(null) == 50);
    }

    /**
     * @tests {@link ObjectDeque#removeRange(int, int)}
     */
    public void test_removeRange() {
        MockArrayList mylist = new MockArrayList();
        mylist.removeRange(0, 0);

        try {
            mylist.removeRange(0, 1);
            fail("Should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // Expected
            assertNotNull(e.getMessage());
        }

        int[] data = { 1, 2, 3 };
        for (int i = 0; i < data.length; i++) {
            mylist.insert(i, data[i]);
        }

        mylist.removeRange(0, 1);
        assertEquals(data[1], mylist.get(0));
        assertEquals(data[2], mylist.get(1));

        try {
            mylist.removeRange(-1, 1);
            fail("Should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // Expected
            assertNotNull(e.getMessage());
        }

        try {
            mylist.removeRange(0, -1);
            fail("Should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // Expected
            assertNotNull(e.getMessage());
        }

        try {
            mylist.removeRange(1, 0);
            fail("Should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // Expected
            assertNotNull(e.getMessage());
        }

        try {
            mylist.removeRange(2, 1);
            fail("Should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
            // Expected
            assertNotNull(e.getMessage());
        }
    }
    
    /**
     * @tests java.util.ObjectDeque#remove(int)
     */
    public void test_removeI() {
        // Test for method java.lang.Object java.util.ObjectDeque.remove(int)
        alist.remove(10);
        assertEquals("Failed to remove element", -1, alist
                .indexOf(objArray[10]));
        try {
            alist.remove(999);
            fail("Failed to throw exception when index out of range");
        } catch (IndexOutOfBoundsException e) {
            // Expected
            assertNotNull(e.getMessage());
        }

        ObjectDeque myList =new ObjectDeque(alist);
        alist.add(25, null);
        alist.add(50, null);
        alist.remove(50);
        alist.remove(25);
        assertTrue("Removing nulls did not work", alist.equals(myList));

        List list = new ObjectDeque(Arrays.asList(new String[] { "a", "b", "c",
                "d", "e", "f", "g" }));
        assertTrue("Removed wrong element 1", list.remove(0) == "a");
        assertTrue("Removed wrong element 2", list.remove(4) == "f");
        String[] result = new String[5];
        list.toArray(result);
        assertTrue("Removed wrong element 3", Arrays.equals(result,
                new String[] { "b", "c", "d", "e", "g" }));

        List l = new ObjectDeque(0);
        l.add(new Object());
        l.add(new Object());
        l.remove(0);
        l.remove(0);
        try {
            l.remove(-1);
            fail("-1 should cause exception");
        } catch (IndexOutOfBoundsException e) {
            // Expected
            assertNotNull(e.getMessage());
        }
        try {
            l.remove(0);
            fail("0 should case exception");
        } catch (IndexOutOfBoundsException e) {
            // Expected
            assertNotNull(e.getMessage());
        }
    }

    /**
     * @tests java.util.ObjectDeque#set(int, java.lang.Object)
     */
    public void test_setILjava_lang_Object() {
        // Test for method java.lang.Object java.util.ObjectDeque.set(int,
        // java.lang.Object)
        Object obj;
        alist.set(65, obj = new Object());
        assertTrue("Failed to set object", alist.get(65) == obj);
        alist.set(50, null);
        assertNull("Setting to null did not work", alist.get(50));
        assertTrue("Setting increased the list's size to: " + alist.size(),
                alist.size() == 100);
        
        obj = new Object();
        alist.set(0, obj);
        assertTrue("Failed to set object", alist.get(0) == obj);

        alist.set(-1, obj);
        Object wasAdded = alist.remove(0);
        assertEquals(obj, wasAdded);

        alist.set(alist.size(), obj);
        wasAdded = alist.remove(alist.size() - 1);
        assertEquals(obj, wasAdded);

        alist.set(-1, null);
        wasAdded = alist.remove(0);
        assertEquals(null, wasAdded);

        alist.set(alist.size(), null);
        wasAdded = alist.remove(alist.size() - 1);
        assertEquals(null, wasAdded);

    }

    /**
     * @tests java.util.ObjectDeque#size()
     */
    public void test_size() {
        // Test for method int java.util.ObjectDeque.size()
        assertEquals("Returned incorrect size for exiting list", 100, alist
                .size());
        assertEquals("Returned incorrect size for new list", 0, new ObjectDeque()
                .size());
    }

    /**
     * @tests java.util.AbstractCollection#toString()
     */
    public void test_toString() {
        ObjectDeque l = new ObjectDeque(1);
        l.add(l);
        String result = l.toString();
        assertTrue("should contain self ref", result.indexOf("(this") > -1);
    }
    
    /**
     * @tests java.util.ObjectDeque#toArray()
     */
    public void test_toArray() {
        // Test for method java.lang.Object [] java.util.ObjectDeque.toArray()
        alist.set(25, null);
        alist.set(75, null);
        Object[] obj = alist.toArray();
        assertEquals("Returned array of incorrect size", objArray.length,
                obj.length);

        for (int i = 0; i < obj.length; i++) {
            if ((i == 25) || (i == 75))
                assertNull("Should be null at: " + i + " but instead got: "
                        + obj[i], obj[i]);
            else
                assertTrue("Returned incorrect array: " + i,
                        obj[i] == objArray[i]);
        }

    }

    /**
     * @tests java.util.ObjectDeque#toArray(java.lang.Object[])
     */
    public void test_toArray$Ljava_lang_Object() {
        // Test for method java.lang.Object []
        // java.util.ObjectDeque.toArray(java.lang.Object [])
        alist.set(25, null);
        alist.set(75, null);
        Integer[] argArray = new Integer[100];
        Object[] retArray;
        retArray = alist.toArray(argArray);
        assertTrue("Returned different array than passed", retArray == argArray);
        argArray = new Integer[1000];
        retArray = alist.toArray(argArray);
        assertNull("Failed to set first extra element to null", argArray[alist
                .size()]);
        for (int i = 0; i < 100; i++) {
            if ((i == 25) || (i == 75))
                assertNull("Should be null: " + i, retArray[i]);
            else
                assertTrue("Returned incorrect array: " + i,
                        retArray[i] == objArray[i]);
        }
    }

//    /**
//     * @tests java.util.ObjectDeque#trimToSize()
//     */
//    public void test_trimToSize() {
//        // Test for method void java.util.ObjectDeque.trimToSize()
//        for (int i = 99; i > 24; i--)
//            alist.remove(i);
//        ((ObjectDeque) alist).trimToSize();
//        assertEquals("Returned incorrect size after trim", 25, alist.size());
//        for (int i = 0; i < alist.size(); i++)
//            assertTrue("Trimmed list contained incorrect elements", alist
//                    .get(i) == objArray[i]);
//        Vector v = new Vector();
//        v.add("a");
//        ObjectDeque al = new ObjectDeque(v);
//        Iterator it = al.iterator();
//        al.trimToSize();
//        try {
//            it.next();
//            fail("should throw a ConcurrentModificationException");
//        } catch (ConcurrentModificationException ioobe) {
//            // expected
//        }
//    }

    /**
     * @test java.util.ObjectDeque#addAll(int, Collection)
     */
    public void test_addAll() {
        ObjectDeque list = new ObjectDeque();
        list.add("one");
        list.add("two");
        assertEquals(2, list.size());

        list.remove(0);
        assertEquals(1, list.size());

        ObjectDeque collection = new ObjectDeque();
        collection.add("1");
        collection.add("2");
        collection.add("3");
        assertEquals(3, collection.size());

        list.addAll(0, collection);
        assertEquals(4, list.size());

        list.remove(0);
        list.remove(0);
        assertEquals(2, list.size());

        collection.add("4");
        collection.add("5");
        collection.add("6");
        collection.add("7");
        collection.add("8");
        collection.add("9");
        collection.add("10");
        collection.add("11");
        collection.add("12");

        assertEquals(12, collection.size());

        list.addAll(0, collection);
        assertEquals(14, list.size());
    }

    public void testAddAllWithConcurrentCollection() {
        ObjectDeque<String> list = new ObjectDeque<String>();
        list.addAll(shrinksOnSize("A", "B", "C", "D"));
        assertFalse(list.contains(null));
    }
//TODO: I don't really care about this case at all...

//    public void testAddAllAtPositionWithConcurrentCollection() {
//        ObjectDeque<String> list = new ObjectDeque<String>(
//                Arrays.asList("A", "B", "C", "D"));
//        System.out.println(list);
//        list.addAll(3, shrinksOnSize("E", "F", "G", "H"));
//        System.out.println(list);
//        assertFalse(list.contains(null));
//    }

    public void test_override_size() throws Exception {
        ObjectDeque testlist = new MockArrayList();
        // though size is overriden, it should passed without exception
        testlist.add("test_0");
        testlist.add("test_1");
        testlist.add("test_2");
        testlist.add(1, "test_3");
        testlist.get(1);
        testlist.remove(2);
        testlist.set(1, "test_4");
    }

    public static class ArrayListExtend extends ObjectDeque {

        private int size = 0;

        public ArrayListExtend() {
            super(10);
        }

        public boolean add(Object o) {
            size++;
            return super.add(o);
        }

        public int size() {
            return size;
        }
    }

    public class MockArrayList extends ObjectDeque {
        public int size() {
            return 0;
        }
        
        public void removeRange(int start, int end) {
            super.removeRange(start, end);
        }
    }

    public void test_subclassing() {
        ArrayListExtend a = new ArrayListExtend();
        /*
         * Regression test for subclasses that override size() (which used to
         * cause an exception when growing 'a').
         */
        for (int i = 0; i < 100; i++) {
            a.add(new Object());
        }
    }

    /**
     * Sets up the fixture, for example, open a network connection. This method
     * is called before a test is executed.
     */
    protected void setUp() throws Exception {
        super.setUp();
        alist = new ObjectDeque();
        for (int i = 0; i < objArray.length; i++)
            alist.add(objArray[i]);
    }

    /**
     * Returns a collection that emulates another thread calling remove() each
     * time the current thread calls size().
     */
    private <T> Collection<T> shrinksOnSize(T... elements) {
        return new HashSet<T>(Arrays.asList(elements)) {
            boolean shrink = true;

            @Override
            public int size() {
                int result = super.size();
                if (shrink) {
                    Iterator<T> i = iterator();
                    i.next();
                    i.remove();
                }
                return result;
            }

            @Override
            public Object[] toArray() {
                shrink = false;
                return super.toArray();
            }
        };
    }
}

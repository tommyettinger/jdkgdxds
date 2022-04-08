/*
 * Copyright (c) 2017-2022 See AUTHORS file.
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
 *
 */

package com.github.tommyettinger.ds.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import com.github.tommyettinger.ds.support.TrimRandom;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class for {@link ObjectObjectSortedMap}
 *
 * @author Robin Wang
 */
public class ObjectObjectSortedMapTest {

    private SortedMap<Integer, String> map;

    @Before
    public void setUp() {
        map = new ObjectObjectSortedMap<>();
    }

    @Test
    public void testPut() {
        map.put(1, "a");
        map.put(2, "b");
        Assert.assertEquals("a", map.get(1));
        Assert.assertEquals("b", map.get(2));
        map.put(1, "c");
        Assert.assertEquals("c", map.get(1));
    }

    @Test
    public void testRemove() {
        map.put(1, "a");
        map.put(2, "b");
        Assert.assertEquals("a", map.get(1));

        Assert.assertEquals("a", map.remove(1));
        Assert.assertEquals(1, map.size());
        Assert.assertEquals("b", map.remove(2));
        Assert.assertNull(map.remove(2));
        Assert.assertTrue(map.isEmpty());
    }

    @Test
    public void testClear() {
        for (int i = 0; i < 10000; i++) {
            map.put(i, String.valueOf(i));
        }
        Assert.assertEquals(10000, map.size());
        map.clear();
        Assert.assertEquals(0, map.size());
    }

    @Test
    public void testSize() {
        Assert.assertEquals(0, map.size());
        for (int i = 1; i <= 10000; i++) {
            map.put(i, null);
            Assert.assertEquals(i, map.size());
        }

        for (int i = 10000; i >= 1; i--) {
            map.remove(i);
            Assert.assertEquals(i - 1, map.size());
        }
    }

    @Test
    public void testContainsKey() {
        for (int i = 0; i < 10000; i++) {
            map.put(i, null);
            Assert.assertTrue(map.containsKey(i));
        }

        for (int i = 0; i < 10000; i++) {
            map.remove(i, null);
            Assert.assertFalse(map.containsKey(i));
        }
    }

    @Test
    public void testEntrySet() {
        TrimRandom random = new TrimRandom(12345L);
        List<Integer> randoms = new ArrayList<>(10000);
        for (int i = 0; i < 10; i++) {
            int r = random.nextInt();
            randoms.add(r);
            map.put(r, "test" + r);
        }
        Collections.sort(randoms);

        int c = 0;
        for (Map.Entry<Integer, String> entry : map.entrySet()) {
            Assert.assertEquals(randoms.get(c++), entry.getKey());
        }
    }

    @Test
    public void testKeySet() {
        TrimRandom random = new TrimRandom(12345L);
        List<Integer> randoms = new ArrayList<>(10000);
        for (int i = 0; i < 10000; i++) {
            int r = random.nextInt();
            randoms.add(r);
            map.put(r, null);
        }
        Collections.sort(randoms);

        int c = 0;
        for (Integer x : map.keySet()) {
            Assert.assertEquals(randoms.get(c++), x);
        }
    }

    @Test
    public void testValues() {
        TrimRandom random = new TrimRandom(12345L);
        List<Integer> randoms = new ArrayList<>(10000);
        for (int i = 0; i < 10; i++) {
            int r = random.nextInt();
            randoms.add(r);
            map.put(r, "test" + r);
        }
        Collections.sort(randoms);

        int c = 0;
        for (String s : map.values()) {
            Assert.assertEquals("test" + randoms.get(c++), s);
        }
    }

    @Test
    public void testComparator() {
        map = new ObjectObjectSortedMap<>(Comparator.<Integer>naturalOrder().reversed());
        for (int i = 0; i < 10000; i++) {
            map.put(i, null);
        }
        int c = 10000;
        for (Map.Entry<Integer, String> entry : map.entrySet()) {
            Assert.assertEquals(--c, (int) entry.getKey());
        }
    }

    @Test
    public void testSubMap() {
        map.put(2, "2");
        map.put(-5, "-5");
        map.put(12, "12");
        map.put(-22, "-22");
        map.put(7, "7");
        map.put(10, "10");
        map.put(11, "11");
        map.put(32, "32");

        SortedMap<Integer, String> subMap = map.subMap(2, 11);

        Assert.assertEquals(3, subMap.size());
        Assert.assertTrue(subMap.containsKey(2));
        Assert.assertTrue(subMap.containsKey(7));
        Assert.assertTrue(subMap.containsKey(10));

        Assert.assertEquals(2, (int) subMap.firstKey());
        Assert.assertEquals(10, (int) subMap.lastKey());

        SortedMap<Integer, String> subSubMap = subMap.subMap(3, 10);
        Assert.assertEquals(1, subSubMap.size());
        Assert.assertTrue(subMap.containsKey(7));
        Assert.assertEquals(7, (int) subSubMap.firstKey());
        Assert.assertEquals(7, (int) subSubMap.lastKey());
    }

    @Test
    public void testHeadMap() {
        for (int i = 0; i < 100; i++) {
            map.put(i, String.valueOf(i));
        }
        SortedMap<Integer, String> headMap = map.headMap(74);
        Assert.assertEquals(74, headMap.size());
        for (int i = 0; i < 74; i++) {
            Assert.assertTrue(headMap.containsKey(i));
        }
        Assert.assertFalse(headMap.containsKey(74));

        SortedMap<Integer, String> headHeadMap = headMap.headMap(53);
        Assert.assertEquals(53, headHeadMap.size());
        for (int i = 0; i < 53; i++) {
            Assert.assertTrue(headHeadMap.containsKey(i));
        }
        Assert.assertFalse(headHeadMap.containsKey(53));

    }

    @Test
    public void testTailMap() {
        for (int i = 0; i < 100; i++) {
            map.put(i, String.valueOf(i));
        }
        SortedMap<Integer, String> tailMap = map.tailMap(21);
        Assert.assertEquals(79, tailMap.size());
        for (int i = 21; i < 100; i++) {
            Assert.assertTrue(tailMap.containsKey(i));
        }

        SortedMap<Integer, String> tailTailMap = tailMap.tailMap(59);
        Assert.assertEquals(41, tailTailMap.size());
        for (int i = 59; i < 100; i++) {
            Assert.assertTrue(tailTailMap.containsKey(i));
        }

    }
}
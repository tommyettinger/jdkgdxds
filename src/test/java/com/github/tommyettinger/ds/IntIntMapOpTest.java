package com.github.tommyettinger.ds;

import com.github.tommyettinger.random.AceRandom;
import org.junit.Assert;
import org.junit.Test;

public class IntIntMapOpTest {
    @Test
    public void addThenRemoveTest() {
        AceRandom random = new AceRandom(123);
        IntIntMap map = new IntIntMap(16);
        com.github.tommyettinger.ds.old.IntIntMap old = new com.github.tommyettinger.ds.old.IntIntMap(16);
        Assert.assertArrayEquals(map.keyTable, old.keyTable);
        Assert.assertArrayEquals(map.valueTable, old.valueTable);

        for (int i = 0; i < 1000; i++) {
            int randomKey = random.next(9) - 200;
            int randomValue = random.nextInt();
            map.put(randomKey, randomValue);
            old.put(randomKey, randomValue);
            Assert.assertArrayEquals(map.keyTable, old.keyTable);
            Assert.assertArrayEquals(map.valueTable, old.valueTable);

        }
        System.out.printf("After adding, each map has %d items, with keyTable length %d\n", map.size, map.keyTable.length);
        for (int i = 0; i < 1000; i++) {
            int randomKey = random.next(9) - 200;
            map.remove(randomKey);
            old.remove(randomKey);
            Assert.assertArrayEquals(map.keyTable, old.keyTable);
            Assert.assertArrayEquals(map.valueTable, old.valueTable);

        }
        System.out.printf("After removing, each map has %d items, with keyTable length %d\n", map.size, map.keyTable.length);
    }

    @Test
    public void mixedAddRemoveTest() {
        AceRandom random = new AceRandom(123);
        IntIntMap map = new IntIntMap(16);
        com.github.tommyettinger.ds.old.IntIntMap old = new com.github.tommyettinger.ds.old.IntIntMap(16);
        Assert.assertArrayEquals(map.keyTable, old.keyTable);
        Assert.assertArrayEquals(map.valueTable, old.valueTable);

        for (int i = 0; i < 10000; i++) {
            int randomKey = random.next(7) << 20;
            int randomValue = random.nextInt();
            if (random.nextBoolean(0.7f)) {
                map.put(randomKey, randomValue);
                old.put(randomKey, randomValue);
            } else {
                map.remove(randomKey);
                old.remove(randomKey);
            }
            Assert.assertArrayEquals(map.keyTable, old.keyTable);
            Assert.assertArrayEquals(map.valueTable, old.valueTable);

        }
        System.out.printf("Each map has %d items, with keyTable length %d\n", map.size, map.keyTable.length);
    }

    @Test
    public void mixedAddIteratorRemoveTest() {
        AceRandom random = new AceRandom(123);
        IntIntMap map = new IntIntMap(16);
        com.github.tommyettinger.ds.old.IntIntMap old = new com.github.tommyettinger.ds.old.IntIntMap(16);
        Assert.assertArrayEquals(map.keyTable, old.keyTable);
        Assert.assertArrayEquals(map.valueTable, old.valueTable);

        for (int i = 0; i < 10000; i++) {
            int randomKey = random.next(7) << 20;
            int randomValue = random.nextInt();
            if (random.nextBoolean(0.7f)) {
                map.put(randomKey, randomValue);
                old.put(randomKey, randomValue);
            } else {
                IntIntMap.EntryIterator mi = map.iterator();
                com.github.tommyettinger.ds.old.IntIntMap.EntryIterator oi = old.iterator();
                for (int j = 1; j < map.size; j++) {
                    mi.next();
                }
                for (int j = 1; j < old.size(); j++) {
                    oi.next();
                }
                if (map.size > 1)
                    mi.remove();
                if (old.size() > 1)
                    oi.remove();
                if (map.size > 2) {
                    mi.next();
                    mi.remove();
                }
                if (old.size() > 2) {
                    oi.next();
                    oi.remove();
                }
                if (mi.hasNext && oi.hasNext) {
                    IntIntMap.Entry me = mi.next();
                    com.github.tommyettinger.ds.old.IntIntMap.Entry oe = oi.next();
                    Assert.assertEquals(me.key, oe.key);
                    Assert.assertEquals(me.value, oe.value);
                }
            }
            Assert.assertArrayEquals(map.keyTable, old.keyTable);
            Assert.assertArrayEquals(map.valueTable, old.valueTable);

        }
        System.out.printf("Each map has %d items, with keyTable length %d\n", map.size, map.keyTable.length);
    }
}

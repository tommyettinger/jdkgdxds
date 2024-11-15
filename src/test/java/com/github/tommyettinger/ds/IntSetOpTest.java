package com.github.tommyettinger.ds;

import com.github.tommyettinger.random.AceRandom;
import org.junit.Assert;
import org.junit.Test;

public class IntSetOpTest {
    @Test
    public void addThenRemoveTest() {
        AceRandom random = new AceRandom(123);
        IntSet set = new IntSet(16);
        com.github.tommyettinger.ds.old.IntSet old = new com.github.tommyettinger.ds.old.IntSet(16);
        Assert.assertArrayEquals(set.keyTable, old.keyTable);
        for (int i = 0; i < 1000; i++) {
            int randomValue = random.next(9) - 200;
            set.add(randomValue);
            old.add(randomValue);
            Assert.assertArrayEquals(set.keyTable, old.keyTable);
        }
        System.out.printf("After adding, each set has %d items, with keyTable length %d\n", set.size, set.keyTable.length);
        for (int i = 0; i < 1000; i++) {
            int randomValue = random.next(9) - 200;
            set.remove(randomValue);
            old.remove(randomValue);
            Assert.assertArrayEquals(set.keyTable, old.keyTable);
        }
        System.out.printf("After removing, each set has %d items, with keyTable length %d\n", set.size, set.keyTable.length);
    }

    @Test
    public void mixedAddRemoveTest() {
        AceRandom random = new AceRandom(123);
        IntSet set = new IntSet(16);
        com.github.tommyettinger.ds.old.IntSet old = new com.github.tommyettinger.ds.old.IntSet(16);
        Assert.assertArrayEquals(set.keyTable, old.keyTable);
        for (int i = 0; i < 10000; i++) {
            int randomValue = random.next(7) << 20;
            if (random.nextBoolean(0.7f)) {
                set.add(randomValue);
                old.add(randomValue);
            } else {
                set.remove(randomValue);
                old.remove(randomValue);
            }
            Assert.assertArrayEquals(set.keyTable, old.keyTable);
        }
        System.out.printf("Each set has %d items, with keyTable length %d\n", set.size, set.keyTable.length);
    }
    @Test
    public void mixedAddIteratorRemoveTest() {
        AceRandom random = new AceRandom(123);
        IntSet set = new IntSet(16);
        com.github.tommyettinger.ds.old.IntSet old = new com.github.tommyettinger.ds.old.IntSet(16);
        Assert.assertArrayEquals(set.keyTable, old.keyTable);
        for (int i = 0; i < 10000; i++) {
            int randomValue = random.next(7) << 20;
            if(random.nextBoolean(0.7f)){
                set.add(randomValue);
                old.add(randomValue);
            } else {
                IntSet.IntSetIterator si = set.iterator();
                com.github.tommyettinger.ds.old.IntSet.IntSetIterator oi = old.iterator();
                for (int j = 1; j < set.size; j++) {
                    si.nextInt();
                }
                for (int j = 1; j < old.size(); j++) {
                    oi.nextInt();
                }
                if(set.size > 1)
                    si.remove();
                if(old.size() > 1)
                    oi.remove();
                if(set.size > 2) {
                    si.nextInt();
                    si.remove();
                }
                if(old.size() > 2) {
                    oi.nextInt();
                    oi.remove();
                }
                if(si.hasNext && oi.hasNext) {
                    Assert.assertEquals(si.nextInt(), oi.nextInt());
                }
            }
            Assert.assertArrayEquals(set.keyTable, old.keyTable);
        }
        System.out.printf("Each set has %d items, with keyTable length %d\n", set.size, set.keyTable.length);
    }
}

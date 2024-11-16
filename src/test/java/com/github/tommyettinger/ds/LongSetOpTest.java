package com.github.tommyettinger.ds;

import com.github.tommyettinger.random.AceRandom;
import org.junit.Assert;
import org.junit.Test;

public class LongSetOpTest {
    @Test
    public void addThenRemoveTest() {
        AceRandom random = new AceRandom(123);
        LongSet set = new LongSet(16);
        com.github.tommyettinger.ds.old.LongSet old = new com.github.tommyettinger.ds.old.LongSet(16);
        Assert.assertArrayEquals(set.keyTable, old.keyTable);
        for (int i = 0; i < 10000; i++) {
            long randomValue = random.nextLong();
            randomValue ^= (randomValue << 32 | randomValue >>> 32);
            set.add(randomValue);
            old.add(randomValue);
            Assert.assertArrayEquals(set.keyTable, old.keyTable);
        }
        System.out.printf("After adding, each set has %d items, with keyTable length %d\n", set.size, set.keyTable.length);
        for (int i = 0; i < 10000; i++) {
            long randomValue = random.nextLong();
            randomValue ^= (randomValue << 32 | randomValue >>> 32);
            set.remove(randomValue);
            old.remove(randomValue);
            Assert.assertArrayEquals(set.keyTable, old.keyTable);
        }
        System.out.printf("After removing, each set has %d items, with keyTable length %d\n", set.size, set.keyTable.length);
    }

    @Test
    public void mixedAddRemoveTest() {
        AceRandom random = new AceRandom(123);
        LongSet set = new LongSet(16);
        com.github.tommyettinger.ds.old.LongSet old = new com.github.tommyettinger.ds.old.LongSet(16);
        Assert.assertArrayEquals(set.keyTable, old.keyTable);
        for (int i = 0; i < 10000; i++) {
            long randomValue = random.nextLong();
            randomValue ^= (randomValue << 32 | randomValue >>> 32);
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
        LongSet set = new LongSet(16);
        com.github.tommyettinger.ds.old.LongSet old = new com.github.tommyettinger.ds.old.LongSet(16);
        Assert.assertArrayEquals(set.keyTable, old.keyTable);
        for (int i = 0; i < 10000; i++) {
            long randomValue = random.nextLong();
            randomValue ^= (randomValue << 32 | randomValue >>> 32);
            if(random.nextBoolean(0.7f)){
                set.add(randomValue);
                old.add(randomValue);
            } else {
                LongSet.LongSetIterator si = set.iterator();
                com.github.tommyettinger.ds.old.LongSet.LongSetIterator oi = old.iterator();
                for (int j = 1; j < set.size; j++) {
                    si.nextLong();
                }
                for (int j = 1; j < old.size(); j++) {
                    oi.nextLong();
                }
                if(set.size > 1)
                    si.remove();
                if(old.size() > 1)
                    oi.remove();
                if(set.size > 2) {
                    si.nextLong();
                    si.remove();
                }
                if(old.size() > 2) {
                    oi.nextLong();
                    oi.remove();
                }
                if(si.hasNext && oi.hasNext) {
                    Assert.assertEquals(si.nextLong(), oi.nextLong());
                }
            }
            Assert.assertArrayEquals(set.keyTable, old.keyTable);
        }
        System.out.printf("Each set has %d items, with keyTable length %d\n", set.size, set.keyTable.length);
    }
}

package com.github.tommyettinger.ds;

import com.github.tommyettinger.digital.ArrayTools;
import com.github.tommyettinger.random.AceRandom;
import org.junit.Assert;
import org.junit.Test;

public class ObjectSetOpTest {
    @Test
    public void addThenRemoveTest() {
        AceRandom random = new AceRandom(123);
        ObjectSet<String> set = new ObjectSet<>(16);
        com.github.tommyettinger.ds.old.ObjectSet<String> old = new com.github.tommyettinger.ds.old.ObjectSet<>(16);
        Assert.assertArrayEquals(set.keyTable, old.keyTable);
        for (int i = 0; i < 1000; i++) {
            String randomValue = ArrayTools.stringAt(random.nextInt(428));
            set.add(randomValue);
            old.add(randomValue);
            Assert.assertArrayEquals(set.keyTable, old.keyTable);
        }
        System.out.printf("After adding, each set has %d items, with table size %d\n", set.size, set.getTableSize());
        for (int i = 0; i < 1000; i++) {
            String randomValue = ArrayTools.stringAt(random.nextInt(428));
            set.remove(randomValue);
            old.remove(randomValue);
            Assert.assertArrayEquals(set.keyTable, old.keyTable);
        }
        System.out.printf("After removing, each set has %d items, with table size %d\n", set.size, set.getTableSize());
    }

    @Test
    public void mixedAddRemoveTest() {
        AceRandom random = new AceRandom(123);
        ObjectSet<String> set = new ObjectSet<>(16);
        com.github.tommyettinger.ds.old.ObjectSet<String> old = new com.github.tommyettinger.ds.old.ObjectSet<>(16);
        Assert.assertArrayEquals(set.keyTable, old.keyTable);
        for (int i = 0; i < 10000; i++) {
            String randomValue = ArrayTools.stringAt(random.nextInt(428));
            if (random.nextBoolean(0.7f)) {
                set.add(randomValue);
                old.add(randomValue);
            } else {
                set.remove(randomValue);
                old.remove(randomValue);
            }
            Assert.assertArrayEquals(set.keyTable, old.keyTable);
        }
        System.out.printf("Each set has %d items, with table size %d\n", set.size, set.getTableSize());
    }
    @Test
    public void mixedAddIteratorRemoveTest() {
        AceRandom random = new AceRandom(123);
        ObjectSet<String> set = new ObjectSet<>(16);
        com.github.tommyettinger.ds.old.ObjectSet<String> old = new com.github.tommyettinger.ds.old.ObjectSet<>(16);
        Assert.assertArrayEquals(set.keyTable, old.keyTable);
        for (int i = 0; i < 10000; i++) {
            String randomValue = ArrayTools.stringAt(random.nextInt(428));
            if(random.nextBoolean(0.7f)){
                set.add(randomValue);
                old.add(randomValue);
            } else {
                ObjectSet.ObjectSetIterator<String> si = set.iterator();
                com.github.tommyettinger.ds.old.ObjectSet.ObjectSetIterator<String> oi = old.iterator();
                for (int j = 1; j < set.size; j++) {
                    si.next();
                }
                for (int j = 1; j < old.size(); j++) {
                    oi.next();
                }
                if(set.size > 1)
                    si.remove();
                if(old.size() > 1)
                    oi.remove();
                if(set.size > 2) {
                    si.next();
                    si.remove();
                }
                if(old.size() > 2) {
                    oi.next();
                    oi.remove();
                }
                if(si.hasNext && oi.hasNext) {
                    Assert.assertEquals(si.next(), oi.next());
                }
            }
            Assert.assertArrayEquals(set.keyTable, old.keyTable);
        }
        System.out.printf("Each set has %d items, with table size %d\n", set.size, set.getTableSize());
    }
}

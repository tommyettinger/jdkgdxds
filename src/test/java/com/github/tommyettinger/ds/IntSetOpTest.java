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
        for (int i = 0; i < 1000; i++) {
            int randomValue = random.next(9) - 200;
            set.remove(randomValue);
            old.remove(randomValue);
            Assert.assertArrayEquals(set.keyTable, old.keyTable);
        }
    }
    
    @Test
    public void mixedAddRemoveTest() {
        AceRandom random = new AceRandom(123);
        IntSet set = new IntSet(16);
        com.github.tommyettinger.ds.old.IntSet old = new com.github.tommyettinger.ds.old.IntSet(16);
        Assert.assertArrayEquals(set.keyTable, old.keyTable);
        for (int i = 0; i < 1000; i++) {
            int randomValue = random.next(9) - 200;
            if(random.nextBoolean(0.7f)){
                set.add(randomValue);
                old.add(randomValue);
            } else {
                set.remove(randomValue);
                old.remove(randomValue);
            }
            Assert.assertArrayEquals(set.keyTable, old.keyTable);
        }
    }
}

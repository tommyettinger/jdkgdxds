package com.github.tommyettinger.ds.test;

import com.github.tommyettinger.digital.Base;
import com.github.tommyettinger.ds.ObjectSet;
import org.junit.Test;

public class PileupTest {
    public static final int LEN = 2000000;

    @Test
    public void testObjectSetOld() {
        long start = System.nanoTime();
        ObjectSet set = new ObjectSet(51, 0.75f) {
            long collisionTotal = 0;
            int longestPileup = 0;

            @Override
            protected void addResize (Object key) {
                Object[] keyTable = this.keyTable;
                for (int i = place(key), p = 0; ; i = i + 1 & mask) {
                    if (keyTable[i] == null) {
                        keyTable[i] = key;
                        return;
                    } else {
                        collisionTotal++;
                        longestPileup = Math.max(longestPileup, ++p);
                    }
                }
            }

            @Override
            protected void resize (int newSize) {
                int oldCapacity = keyTable.length;
                threshold = (int)(newSize * loadFactor);
                mask = newSize - 1;
                shift = Long.numberOfLeadingZeros(mask);

                Object[] oldKeyTable = keyTable;

                keyTable = new Object[newSize];

                collisionTotal = 0;
                longestPileup = 0;

                if (size > 0) {
                    for (int i = 0; i < oldCapacity; i++) {
                        Object key = oldKeyTable[i];
                        if (key != null) {addResize(key);}
                    }
                }
                System.out.println("total collisions: " + collisionTotal);
                System.out.println("longest pileup: " + longestPileup);
            }
        };
        for (int i = 0; i < LEN; i++) {
            set.add(new Vector2(Integer.rotateRight(i & 0x55555555, 1), Integer.rotateRight(i & 0xAAAAAAAA, 2)));
        }
        System.out.println(System.nanoTime() - start);
    }
    @Test
    public void testObjectSetNew() {
        long start = System.nanoTime();
        ObjectSet set = new ObjectSet(51, 0.75f) {
            long collisionTotal = 0;
            int longestPileup = 0;

            @Override
            protected void addResize (Object key) {
                Object[] keyTable = this.keyTable;
                for (int i = place(key), p = 0; ; i = i + 1 & mask) {
                    if (keyTable[i] == null) {
                        keyTable[i] = key;
                        return;
                    } else {
                        collisionTotal++;
                        longestPileup = Math.max(longestPileup, ++p);
                    }
                }
            }

            @Override
            protected void resize (int newSize) {
                int oldCapacity = keyTable.length;
                threshold = (int)(newSize * loadFactor);
                mask = newSize - 1;
                shift = Long.numberOfLeadingZeros(mask);

                // multiplier from Steele and Vigna, Computationally Easy, Spectrally Good Multipliers for Congruential
                // Pseudorandom Number Generators
                hashMultiplier *= 0xF1357AEA2E62A9C5L;
                // ensures hashMultiplier is never too small, and is always odd
                hashMultiplier |= 0x0000010000000001L;

                Object[] oldKeyTable = keyTable;

                keyTable = new Object[newSize];

                collisionTotal = 0;
                longestPileup = 0;

                if (size > 0) {
                    for (int i = 0; i < oldCapacity; i++) {
                        Object key = oldKeyTable[i];
                        if (key != null) {addResize(key);}
                    }
                }
                System.out.println("total collisions: " + collisionTotal);
                System.out.println("longest pileup: " + longestPileup);
            }
        };
        for (int i = 0; i < LEN; i++) {
            set.add(new Vector2(Integer.rotateRight(i & 0x55555555, 1), Integer.rotateRight(i & 0xAAAAAAAA, 2)));
        }
        System.out.println(System.nanoTime() - start);
    }
}

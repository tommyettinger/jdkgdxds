/*
 * Copyright (c) 2025 See AUTHORS file.
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
 */

package com.github.tommyettinger.ds.test;

import com.github.tommyettinger.digital.Base;
import com.github.tommyettinger.digital.BitConversion;
import com.github.tommyettinger.digital.MathTools;
import com.github.tommyettinger.ds.CaseInsensitiveSet;
import com.github.tommyettinger.ds.CharFilter;
import com.github.tommyettinger.ds.FilteredStringOrderedSet;
import com.github.tommyettinger.ds.FilteredStringSet;
import com.github.tommyettinger.ds.IntLongMap;
import com.github.tommyettinger.ds.IntLongOrderedMap;
import com.github.tommyettinger.ds.IntSet;
import com.github.tommyettinger.ds.ObjectList;
import com.github.tommyettinger.ds.ObjectOrderedSet;
import com.github.tommyettinger.ds.Utilities;
import com.github.tommyettinger.ds.support.sort.LongComparators;
import com.github.tommyettinger.random.WhiskerRandom;
import org.junit.Ignore;
import org.junit.Test;

import org.checkerframework.checker.nullness.qual.NonNull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.github.tommyettinger.function.IntIntToIntBiFunction;

//@Ignore
public class PileupTest {
    public static final int LEN = 2000000;//500000;//1000000;//
    public static final float LOAD = 0.5f; //0.6f

    public static String[] generateUniqueWordsFibSet (int size) {
        final int numLetters = 4;
        ObjectSet<String> set = new ObjectSet<String>(size, LOAD) {
            @Override
            protected int place (@NonNull Object item) {
                return (int)(item.hashCode() * 0x9E3779B97F4A7C15L >>> shift);
            }
        };
        char[] maker = new char[numLetters];
        for (int i = 0; set.size() < size; ) {
            for (int w = 0; w < 26 && set.size() < size; w++) {
                maker[0] = (char)('a' + w);
                for (int x = 0; x < 26 && set.size() < size; x++) {
                    maker[1] = (char)('a' + x);
                    for (int y = 0; y < 26 && set.size() < size; y++) {
                        maker[2] = (char)('a' + y);
                        for (int z = 0; z < 26 && set.size() < size; z++) {
                            maker[3] = (char)('a' + z);
                            set.add(String.valueOf(maker) + (i++));
                        }
                    }
                }
            }
        }
        return set.toArray(new String[0]);
    }

    public static String[] generateUniqueWords (int size) {
        final int numLetters = 4;
        String[] items = new String[size];
        char[] maker = new char[numLetters];
        for (int i = 0; i < size; ) {
            for (int w = 0; w < 26 && i < size; w++) {
                maker[0] = (char)('a' + w);
                for (int x = 0; x < 26 && i < size; x++) {
                    maker[1] = (char)('a' + x);
                    for (int y = 0; y < 26 && i < size; y++) {
                        maker[2] = (char)('a' + y);
                        for (int z = 0; z < 26 && i < size; z++) {
                            maker[3] = (char)('a' + z);
                            final String s = String.valueOf(maker) + i;
                            items[i] = s;
                            i++;
                        }
                    }
                }
            }
        }
        return items;
    }

    public static String[] generateUniqueWords (int size, long shuffleSeed) {
        final int numLetters = 4;
        String[] items = new String[size];
        char[] maker = new char[numLetters];
        for (int i = 0; i < size; ) {
            for (int w = 0; w < 26 && i < size; w++) {
                maker[0] = (char)('a' + w);
                for (int x = 0; x < 26 && i < size; x++) {
                    maker[1] = (char)('a' + x);
                    for (int y = 0; y < 26 && i < size; y++) {
                        maker[2] = (char)('a' + y);
                        for (int z = 0; z < 26 && i < size; z++) {
                            maker[3] = (char)('a' + z);
                            final String s = String.valueOf(maker) + i;
                            items[i] = s;
                            i++;
                        }
                    }
                }
            }
        }
        WhiskerRandom rng = new WhiskerRandom(shuffleSeed);
        rng.shuffle(items);
        return items;
    }

    /**
     * average pileup: 0.21540069580078125
     * 59512900 ns
     * final size 200000
     * total collisions: 28233
     * longest pileup: 15
     * total of 12 pileups: 88
     * <br>
     * I didn't think I changed anything, but this seems different...
     * average pileup: 0.21602630615234375
     * 51168100 ns
     * hash multiplier: EFAA28F1 with final size 200000
     * total collisions: 28315
     * longest pileup: 11
     * total of 12 pileups: 73
     * <br>
     * Using imul of golden ratio and hashCode, lower bits:
     * average pileup: 0.16037750244140625
     * 49177400 ns
     * hash multiplier: EFAA28F1 with final size 200000
     * total collisions: 21021
     * longest pileup: 14
     * total of 12 pileups: 73
     * <br>
     * Same as above, but upper bits:
     * average pileup: 0.14096832275390625
     * 49312000 ns
     * hash multiplier: EFAA28F1 with final size 200000
     * total collisions: 18477
     * longest pileup: 9
     * total of 12 pileups: 77
     * <br>
     * Using GWT-safe multiplier 0x19E377, low bits:
     * average pileup: 0.15993499755859375
     * 48731100 ns
     * hash multiplier: EFAA28F1 with final size 200000
     * total collisions: 20963
     * longest pileup: 10
     * total of 12 pileups: 90
     * <br>
     * XMUL, lower bits:
     * average pileup: 0.16020965576171875
     * 48294000 ns
     * hash multiplier: EFAA28F1 with final size 200000
     * total collisions: 20999
     * longest pileup: 9
     * total of 12 pileups: 77
     * <br>
     * XMUL, upper bits:
     * average pileup: 0.170196533203125
     * 49598800 ns
     * hash multiplier: EFAA28F1 with final size 200000
     * total collisions: 22308
     * longest pileup: 11
     * total of 12 pileups: 71
     */
    //@Ignore // this test used to take much longer to run than the others here (over a minute; everything else is under a second).
    @Test
    public void testObjectSetOld () {
        final String[] words = generateUniqueWords(LEN, -123456789L);
        long start = System.nanoTime();
        // replicates old ObjectSet behavior, with added logging and the constant in place() changed back
        ObjectSet set = new ObjectSet(64, LOAD) {
            long collisionTotal = 0;
            int longestPileup = 0, allPileups = 0, pileupChecks = 0;
            double averagePileup = 0;

            @Override
            protected int place (@NonNull Object item) {
//                return (item.hashCode() * 0x9E3779B9) >>> shift;
//                return (item.hashCode() * 0x9E3779B9) & mask;
//                return (item.hashCode() * 0x19E377) & mask;
//                return (item.hashCode() ^ 0x7F4A7C15) * 0x19E373 & mask;
                return (item.hashCode() ^ 0x7F4A7C15) * 0x19E373 >>> shift;
            }

//            @Override
//            protected int place (@NonNull Object item) {
//                return (int)(item.hashCode() * 0x9E3779B97F4A7C15L >>> shift);
//            }

//            @Override
//            protected int place (Object item) {
//                return (int)(item.hashCode() * 0xD1B54A32D192ED03L >>> shift); // does extremely well???
//            }

            @Override
            protected void addResize (@NonNull Object key) {
                Object[] keyTable = this.keyTable;
                for (int i = place(key), p = 0; ; i = i + 1 & mask) {
                    if (keyTable[i] == null) {
                        keyTable[i] = key;
                        averagePileup += p;

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
                shift = BitConversion.countLeadingZeros(mask) + 32;

//                hashMultiplier *= size | 0xF1357AEA2E62A9C5L;
//                hashMultiplier *= size + size ^ 0xF1357AEA2E62A9C5L;

                Object[] oldKeyTable = keyTable;

                keyTable = new Object[newSize];

                allPileups += longestPileup;
                pileupChecks++;
                collisionTotal = 0;
                longestPileup = 0;
                averagePileup = 0.0;

                if (size > 0) {
                    for (int i = 0; i < oldCapacity; i++) {
                        Object key = oldKeyTable[i];
                        if (key != null) {addResize(key);}
                    }
                }
                System.out.println("hash multiplier: " + Base.BASE16.unsigned(hashMultiplier) + " with new size " + newSize);
                System.out.println("total collisions: " + collisionTotal);
                System.out.println("longest pileup: " + longestPileup);
                System.out.println("average pileup: " + (averagePileup / size));
            }

            @Override
            public void clear () {
                System.out.println("hash multiplier: " + Base.BASE16.unsigned(hashMultiplier) + " with final size " + size);
                System.out.println("total collisions: " + collisionTotal);
                System.out.println("longest pileup: " + longestPileup);
                System.out.println("total of " + pileupChecks + " pileups: " + (allPileups + longestPileup));
                super.clear();
            }
        };
//        final int limit = (int)(Math.sqrt(LEN));
//        for (int x = -limit; x < limit; x+=2) {
//            for (int y = -limit; y < limit; y+=2) {
//                set.add(new Vector2(x, y));
//            }
//        }
        for (int i = 0; i < LEN; i++) {
            set.add(words[i]);
        }
        System.out.println((System.nanoTime() - start) + " ns");
        set.clear();
    }

    /**
     * average pileup: 0.16497802734375
     * 64831700 ns
     * hash multiplier: C35BAAAA3DACBEF3 with final size 200000
     * total collisions: 21624
     * longest pileup: 8
     * total of 12 pileups: 72
     * <br>
     * Using imul of an int from GOOD_MULTIPLIERS with the hashCode, upper bits:
     * average pileup: 0.20114898681640625
     * 47200000 ns
     * hash multiplier: E993C987 with final size 200000
     * total collisions: 26365
     * longest pileup: 8
     * total of 12 pileups: 69
     * <br>
     * Same as above but lower bits:
     * average pileup: 0.1577911376953125
     * 46397500 ns
     * hash multiplier: E993C987 with final size 200000
     * total collisions: 20682
     * longest pileup: 8
     * total of 12 pileups: 72
     */
    @Test
    public void testObjectSetNew () {
        final String[] words = generateUniqueWords(LEN, -123456789L);
        long start = System.nanoTime();
        ObjectSet set = new ObjectSet(64, LOAD) {
            long collisionTotal = 0;
            int longestPileup = 0, allPileups = 0, pileupChecks = 0;
            double averagePileup = 0;

            @Override
            protected int place (@NonNull Object item) {
//                return BitConversion.imul(hashMultiplier, item.hashCode()) & mask;
//                return (hashMultiplier * item.hashCode()) & mask;
                return (hashMultiplier * item.hashCode()) >>> shift;
            }

            @Override
            protected void addResize (@NonNull Object key) {
                Object[] keyTable = this.keyTable;
                for (int i = place(key), p = 0; ; i = i + 1 & mask) {
                    if (keyTable[i] == null) {
                        keyTable[i] = key;
                        averagePileup += p;
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
                shift = BitConversion.countLeadingZeros(mask) + 32;

                // multiplier from Steele and Vigna, Computationally Easy, Spectrally Good Multipliers for Congruential
                // Pseudorandom Number Generators
//                hashMultiplier *= 0xF1357AEA2E62A9C5L;
                // ensures hashMultiplier is never too small, and is always odd
//                hashMultiplier |= 0x0000010000000001L;

                // we modify the hash multiplier by multiplying it by a number that Vigna and Steele considered optimal
                // for a 64-bit MCG random number generator, XORed with 2 times size to randomize the low bits more.
//                hashMultiplier *= size + size ^ 0xF1357AEA2E62A9C5L;

//                hashMultiplier *= 0xF1357AEA2E62A9C5L;

                hashMultiplier = Utilities.GOOD_MULTIPLIERS[(hashMultiplier >>> 48 + shift) & 511];

                Object[] oldKeyTable = keyTable;

                keyTable = new Object[newSize];

                allPileups += longestPileup;
                pileupChecks++;
                collisionTotal = 0;
                longestPileup = 0;
                averagePileup = 0.0;

                if (size > 0) {
                    for (int i = 0; i < oldCapacity; i++) {
                        Object key = oldKeyTable[i];
                        if (key != null) {addResize(key);}
                    }
                }
                System.out.println("hash multiplier: " + Base.BASE16.unsigned(hashMultiplier) + " with new size " + newSize);
                System.out.println("total collisions: " + collisionTotal);
                System.out.println("longest pileup: " + longestPileup);
                System.out.println("average pileup: " + (averagePileup / size));
            }

            @Override
            public void clear () {
                System.out.println("hash multiplier: " + Base.BASE16.unsigned(hashMultiplier) + " with final size " + size);
                System.out.println("total collisions: " + collisionTotal);
                System.out.println("longest pileup: " + longestPileup);
                System.out.println("total of " + pileupChecks + " pileups: " + (allPileups + longestPileup));
                super.clear();
            }
        };
//        final int limit = (int)(Math.sqrt(LEN));
//        for (int x = -limit; x < limit; x+=2) {
//            for (int y = -limit; y < limit; y+=2) {
//                set.add(new Vector2(x, y));
//            }
//        }
        for (int i = 0; i < LEN; i++) {
            set.add(words[i]);
        }
        System.out.println((System.nanoTime() - start) + " ns");
        set.clear();
    }

    /**
     * Initial version using {@link Utilities#GOOD_MULTIPLIERS}:
     * {@code hashMultiplier = Utilities.GOOD_MULTIPLIERS[(int)(hashMultiplier >>> 27) + shift & 511];}
     * <br>
     * average pileup: 0.20037841796875
     * 56633100 ns
     * hash multiplier: 992942A852DFBF6F with final size 200000
     * total collisions: 26264
     * longest pileup: 9
     * total of 12 pileups: 75
     * <br>
     * Alternate version with variable shift:
     * {@code hashMultiplier = Utilities.GOOD_MULTIPLIERS[(int)(hashMultiplier >>> 48 + shift) & 511];}
     * <br>
     * average pileup: 0.17543792724609375
     * 49647200 ns
     * hash multiplier: 8BA92E4143ACC451 with final size 200000
     * total collisions: 22995
     * longest pileup: 8
     * total of 12 pileups: 61
     * <br>
     * Using only the XOR-ROL(9)-XOR-ROL(21) step on the hashCode:
     * average pileup: 0.16771697998046875
     * 48049200 ns
     * final size: 200000
     * total collisions: 21983
     * longest pileup: 10
     * total of 12 pileups: 71
     */
    @Test
    public void testObjectSetCurrent () {
        final String[] words = generateUniqueWords(LEN, -123456789L);
        long start = System.nanoTime();
        com.github.tommyettinger.ds.ObjectSet set = new com.github.tommyettinger.ds.ObjectSet(64, LOAD) {
            long collisionTotal = 0;
            int longestPileup = 0, allPileups = 0, pileupChecks = 0;
            double averagePileup = 0;

            @Override
            protected void addResize (@NonNull Object key) {
                Object[] keyTable = this.keyTable;
                for (int i = place(key), p = 0; ; i = i + 1 & mask) {
                    if (keyTable[i] == null) {
                        keyTable[i] = key;
                        averagePileup += p;
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
                shift = BitConversion.countLeadingZeros(mask) + 32;

                Object[] oldKeyTable = keyTable;

                keyTable = new Object[newSize];

                allPileups += longestPileup;
                pileupChecks++;
                collisionTotal = 0;
                longestPileup = 0;
                averagePileup = 0.0;

                if (size > 0) {
                    for (int i = 0; i < oldCapacity; i++) {
                        Object key = oldKeyTable[i];
                        if (key != null) {addResize(key);}
                    }
                }
                System.out.println("new size: " + newSize);
                System.out.println("total collisions: " + collisionTotal);
                System.out.println("longest pileup: " + longestPileup);
                System.out.println("average pileup: " + (averagePileup / size));
            }

            @Override
            public void clear () {
                System.out.println("final size: " + size);
                System.out.println("total collisions: " + collisionTotal);
                System.out.println("longest pileup: " + longestPileup);
                System.out.println("total of " + pileupChecks + " pileups: " + (allPileups + longestPileup));
                super.clear();
            }
        };
        for (int i = 0; i < LEN; i++) {
            set.add(words[i]);
        }
        System.out.println((System.nanoTime() - start) + " ns");
        set.clear();
    }

    /**
     * The Quad classes use quadratic probing, and removal doesn't work in them yet.
     * Not ready for prime time currently, or any usage.
     */
    @Test
    public void testObjectQuadSet () {
        final String[] words = generateUniqueWords(LEN, -123456789L);
        long start = System.nanoTime();
        ObjectQuadSet set = new ObjectQuadSet(51, LOAD) {
            long collisionTotal = 0;
            int longestPileup = 0, allPileups = 0, pileupChecks = 0;
            double averagePileup = 0;

            @Override
            protected void addResize (@NonNull Object key) {
                Object[] keyTable = this.keyTable;
                for (int i = place(key), p = 0; ; i = i + p & mask) {
                    if (keyTable[i] == null) {
                        keyTable[i] = key;
                        averagePileup += p;

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
                shift = BitConversion.countLeadingZeros(mask) + 32;

////                 we modify the hash multiplier by... basically it just needs to stay odd, and use 21 bits or fewer (for GWT reasons).
////                 we incorporate the size in here (times 2, so it doesn't make the multiplier even) to randomize things more.
//                hashMultiplier = (hashMultiplier + size + size ^ 0xC79E7B18) * 0x13C6EB & 0x1FFFFF;

                // we modify the hash multiplier by multiplying it by a number that Vigna and Steele considered optimal
                // for a 64-bit MCG random number generator, XORed with 2 times size to randomize the low bits more.
                hashMultiplier *= size + size ^ 0xF1357AEA2E62A9C5L;

                Object[] oldKeyTable = keyTable;

                keyTable = new Object[newSize];

                allPileups += longestPileup;
                pileupChecks++;
                collisionTotal = 0;
                longestPileup = 0;
                averagePileup = 0.0;

                if (size > 0) {
                    for (int i = 0; i < oldCapacity; i++) {
                        Object key = oldKeyTable[i];
                        if (key != null) {addResize(key);}
                    }
                }
                System.out.println("hash multiplier: " + Base.BASE16.unsigned(hashMultiplier) + " with new size " + newSize);
                System.out.println("total collisions: " + collisionTotal);
                System.out.println("longest pileup: " + longestPileup);
                System.out.println("average pileup: " + (averagePileup / size));
            }

            @Override
            public void clear () {
                System.out.println("hash multiplier: " + Base.BASE16.unsigned(hashMultiplier) + " with final size " + size);
                System.out.println("total collisions: " + collisionTotal);
                System.out.println("longest pileup: " + longestPileup);
                System.out.println("total of " + pileupChecks + " pileups: " + (allPileups + longestPileup));
                super.clear();
            }
        };
//        final int limit = (int)(Math.sqrt(LEN));
//        for (int x = -limit; x < limit; x+=2) {
//            for (int y = -limit; y < limit; y+=2) {
//                set.add(new Vector2(x, y));
//            }
//        }
        for (int i = 0; i < LEN; i++) {
            set.add(words[i]);
        }
        System.out.println((System.nanoTime() - start) + " ns");
        set.clear();
    }

    @Test
    public void testObjectQuadSetExperimental () {
        final String[] words = generateUniqueWords(LEN, -123456789L);
        long start = System.nanoTime();
        ObjectQuadSet set = new ObjectQuadSet(51, LOAD) {
            long collisionTotal = 0;
            int longestPileup = 0, allPileups = 0, pileupChecks = 0;
            double averagePileup = 0;

            @Override
            protected int place (Object item) {
                return (int)(item.hashCode() * 0xD1B54A32D192ED03L >>> shift); // does extremely well???
            }
//                return (item.hashCode() * hashMultiplier >>> shift); // what we're using now
//                return (int)(item.hashCode() * 0x9E3779B97F4A7C15L >>> shift); // actually does very poorly here
//                return (item.hashCode() * (0x1827F5) >>> shift); // not as good as...
//                return (item.hashCode() * (0x13C6ED) >>> shift); // ... this one, for some reason
//                return (item.hashCode() & mask); // only good if the hashCode is already a high-quality random number
//                final int h = item.hashCode();
//                return (h ^ h >>> 11 ^ h >>> 21) & mask; // eh, maybe? a few more collisions than ideal
//            }

            @Override
            protected void addResize (@NonNull Object key) {
                Object[] keyTable = this.keyTable;
                for (int i = place(key), p = 0; ; i = i + p & mask) {
                    if (keyTable[i] == null) {
                        keyTable[i] = key;
                        averagePileup += p;

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
                shift = BitConversion.countLeadingZeros(mask) + 32;

//                // We modify the hash multiplier by... basically it just needs to stay odd, and use 21 bits or fewer (for GWT reasons).
//                // We incorporate the size in here to randomize things more. The multiplier seems to do a little better if it ends in the
//                // hex digit 5 or D -- this makes it a valid power-of-two-modulus MCG multiplier, which might help a bit. We also always
//                // set the bit 0x100000, so we know there will at least be some bits moved to the upper third or so.
//                hashMultiplier = ((hashMultiplier + size << 3 ^ 0xC79E7B1D) * 0x13C6EB + 0xAF36D01E & 0xFFFFF) | 0x100000;
                // we modify the hash multiplier by multiplying it by a number that Vigna and Steele considered optimal
                // for a 64-bit MCG random number generator, XORed with 2 times size to randomize the low bits more.
                hashMultiplier *= size + size ^ 0xF1357AEA2E62A9C5L;

                Object[] oldKeyTable = keyTable;

                keyTable = new Object[newSize];

                allPileups += longestPileup;
                pileupChecks++;
                collisionTotal = 0;
                longestPileup = 0;
                averagePileup = 0.0;

                if (size > 0) {
                    for (int i = 0; i < oldCapacity; i++) {
                        Object key = oldKeyTable[i];
                        if (key != null) {addResize(key);}
                    }
                }
                System.out.println("hash multiplier: " + Base.BASE16.unsigned(hashMultiplier) + " with new size " + newSize);
                System.out.println("total collisions: " + collisionTotal);
                System.out.println("longest pileup: " + longestPileup);
                System.out.println("average pileup: " + (averagePileup / size));
            }

            @Override
            public void clear () {
                System.out.println("hash multiplier: " + Base.BASE16.unsigned(hashMultiplier) + " with final size " + size);
                System.out.println("total collisions: " + collisionTotal);
                System.out.println("longest pileup: " + longestPileup);
                System.out.println("total of " + pileupChecks + " pileups: " + (allPileups + longestPileup));
                super.clear();
            }
        };
//        final int limit = (int)(Math.sqrt(LEN));
//        for (int x = -limit; x < limit; x+=2) {
//            for (int y = -limit; y < limit; y+=2) {
//                set.add(new Vector2(x, y));
//            }
//        }
        for (int i = 0; i < LEN; i++) {
            set.add(words[i]);
        }
        System.out.println((System.nanoTime() - start) + " ns");
        set.clear();
    }

    @Test
    public void testObjectQuadSetSimplePlace () {
        final String[] words = generateUniqueWords(LEN, -123456789L);
        long start = System.nanoTime();
        ObjectQuadSet set = new ObjectQuadSet(51, LOAD) {
            long collisionTotal = 0;
            int longestPileup = 0, allPileups = 0, pileupChecks = 0;
            double averagePileup = 0;
            int hm = 0x13C6ED;

            @Override
            protected int place (Object item) {
                return item.hashCode() * hm & mask;
            }

            @Override
            protected void addResize (@NonNull Object key) {
                Object[] keyTable = this.keyTable;
                for (int i = place(key), p = 0; ; i = i + p & mask) {
                    if (keyTable[i] == null) {
                        keyTable[i] = key;
                        averagePileup += p;

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
                shift = BitConversion.countLeadingZeros(mask) + 32;

                // multiplier from Steele and Vigna, Computationally Easy, Spectrally Good Multipliers for Congruential
                // Pseudorandom Number Generators
//                hashMultiplier *= 0xF1357AEA2E62A9C5L;
                // ensures hashMultiplier is never too small, and is always odd
//                hashMultiplier |= 0x0000010000000001L;

                // we modify the hash multiplier by multiplying it by a number that Vigna and Steele considered optimal
                // for a 64-bit MCG random number generator, XORed with 2 times size to randomize the low bits more.
                hashMultiplier *= size + size ^ 0xF1357AEA2E62A9C5L;

                hm = (hm + size + size ^ 0xC79E7B18) * 0x13C6EB & 0x1FFFFF; //0x11357B 0x13C6EB
                Object[] oldKeyTable = keyTable;

                keyTable = new Object[newSize];

                allPileups += longestPileup;
                pileupChecks++;
                collisionTotal = 0;
                longestPileup = 0;
                averagePileup = 0.0;

                if (size > 0) {
                    for (int i = 0; i < oldCapacity; i++) {
                        Object key = oldKeyTable[i];
                        if (key != null) {addResize(key);}
                    }
                }
                System.out.println("hash multiplier: " + Base.BASE16.unsigned(hm) + " with new size " + newSize);
                System.out.println("total collisions: " + collisionTotal);
                System.out.println("longest pileup: " + longestPileup);
                System.out.println("average pileup: " + (averagePileup / size));
            }

            @Override
            public void clear () {
                System.out.println("hash multiplier: " + Base.BASE16.unsigned(hm) + " with final size " + size);
                System.out.println("total collisions: " + collisionTotal);
                System.out.println("longest pileup: " + longestPileup);
                System.out.println("total of " + pileupChecks + " pileups: " + (allPileups + longestPileup));
                super.clear();
            }
        };
//        final int limit = (int)(Math.sqrt(LEN));
//        for (int x = -limit; x < limit; x+=2) {
//            for (int y = -limit; y < limit; y+=2) {
//                set.add(new Vector2(x, y));
//            }
//        }

        for (int i = 0; i < LEN; i++) {
            set.add(words[i]);
        }
        System.out.println((System.nanoTime() - start) + " ns");
        set.clear();
    }

    @Test
    public void testObjectSetIntPlace () {
        final String[] words = generateUniqueWords(LEN, -123456789L);
        long start = System.nanoTime();
        ObjectSet set = new ObjectSet(51, LOAD) {
            long collisionTotal = 0;
            int longestPileup = 0, allPileups = 0, pileupChecks = 0;
            double averagePileup = 0;
            int hm = 0x13C6ED;

            @Override
            protected int place (@NonNull Object item) {
                return item.hashCode() * hm >>> shift;
            }

            @Override
            protected void addResize (@NonNull Object key) {
                Object[] keyTable = this.keyTable;
                for (int i = place(key), p = 0; ; i = i + 1 & mask) {
                    if (keyTable[i] == null) {
                        keyTable[i] = key;
                        averagePileup += p;

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
                shift = BitConversion.countLeadingZeros(mask) + 32;

                // multiplier from Steele and Vigna, Computationally Easy, Spectrally Good Multipliers for Congruential
                // Pseudorandom Number Generators
//                hashMultiplier *= 0xF1357AEA2E62A9C5L;
                // ensures hashMultiplier is never too small, and is always odd
//                hashMultiplier |= 0x0000010000000001L;

                // we modify the hash multiplier by multiplying it by a number that Vigna and Steele considered optimal
                // for a 64-bit MCG random number generator, XORed with 2 times size to randomize the low bits more.
                hashMultiplier *= size + size ^ 0xF1357AEA2E62A9C5L;

                // We modify the hash multiplier by... basically it just needs to stay odd, and use 21 bits or fewer (for GWT reasons).
                // We incorporate the size in here to randomize things more. The multiplier seems to do a little better if it ends in the
                // hex digit 5 or D -- this makes it a valid power-of-two-modulus MCG multiplier, which might help a bit. We also always
                // set the bit 0x100000, so we know there will at least be some bits moved to the upper third or so.
                hm = ((hm + size << 3 ^ 0xC79E7B1D) * 0x13C6EB + 0xAF36D01E & 0xFFFFF) | 0x100000;
                Object[] oldKeyTable = keyTable;

                keyTable = new Object[newSize];

                allPileups += longestPileup;
                pileupChecks++;
                collisionTotal = 0;
                longestPileup = 0;
                averagePileup = 0.0;

                if (size > 0) {
                    for (int i = 0; i < oldCapacity; i++) {
                        Object key = oldKeyTable[i];
                        if (key != null) {addResize(key);}
                    }
                }
                System.out.println("hash multiplier: " + Base.BASE16.unsigned(hm) + " with new size " + newSize);
                System.out.println("total collisions: " + collisionTotal);
                System.out.println("longest pileup: " + longestPileup);
                System.out.println("average pileup: " + (averagePileup / size));
            }

            @Override
            public void clear () {
                System.out.println("hash multiplier: " + Base.BASE16.unsigned(hm) + " with final size " + size);
                System.out.println("total collisions: " + collisionTotal);
                System.out.println("longest pileup: " + longestPileup);
                System.out.println("total of " + pileupChecks + " pileups: " + (allPileups + longestPileup));
                super.clear();
            }
        };
//        final int limit = (int)(Math.sqrt(LEN));
//        for (int x = -limit; x < limit; x+=2) {
//            for (int y = -limit; y < limit; y+=2) {
//                set.add(new Vector2(x, y));
//            }
//        }

        for (int i = 0; i < LEN; i++) {
            set.add(words[i]);
        }
        System.out.println((System.nanoTime() - start) + " ns");
        set.clear();
    }

    public static BadString[] generateUniqueBadFibSet (int size) {
        final int numLetters = 4;
        ObjectSet<BadString> set = new ObjectSet<BadString>(size, LOAD) {
            @Override
            protected int place (@NonNull Object item) {
                return (int)(item.hashCode() * 0x9E3779B97F4A7C15L >>> shift);
            }
        };
        char[] maker = new char[numLetters];
        for (int i = 0; set.size() < size; ) {
            for (int w = 0; w < 26 && set.size() < size; w++) {
                maker[0] = (char)('a' + w);
                for (int x = 0; x < 26 && set.size() < size; x++) {
                    maker[1] = (char)('a' + x);
                    for (int y = 0; y < 26 && set.size() < size; y++) {
                        maker[2] = (char)('a' + y);
                        for (int z = 0; z < 26 && set.size() < size; z++) {
                            maker[3] = (char)('a' + z);
                            set.add(new BadString(String.valueOf(maker) + (i++)));
                        }
                    }
                }
            }
        }
        return set.toArray(new BadString[0]);
    }

    public static BadString[] generateUniqueBad (int size) {
        final int numLetters = 4;
        BadString[] items = new BadString[size];
        char[] maker = new char[numLetters];
        for (int i = 0; i < size; ) {
            for (int w = 0; w < 26 && i < size; w++) {
                maker[0] = (char)('a' + w);
                for (int x = 0; x < 26 && i < size; x++) {
                    maker[1] = (char)('a' + x);
                    for (int y = 0; y < 26 && i < size; y++) {
                        maker[2] = (char)('a' + y);
                        for (int z = 0; z < 26 && i < size; z++) {
                            maker[3] = (char)('a' + z);
                            final String s = String.valueOf(maker) + i;
                            items[i] = new BadString(s);
                            i++;
                        }
                    }
                }
            }
        }
        return items;
    }

    public static BadString[] generateUniqueBad (int size, long shuffleSeed) {
        final int numLetters = 4;
        BadString[] items = new BadString[size];
        char[] maker = new char[numLetters];
        for (int i = 0; i < size; ) {
            for (int w = 0; w < 26 && i < size; w++) {
                maker[0] = (char)('a' + w);
                for (int x = 0; x < 26 && i < size; x++) {
                    maker[1] = (char)('a' + x);
                    for (int y = 0; y < 26 && i < size; y++) {
                        maker[2] = (char)('a' + y);
                        for (int z = 0; z < 26 && i < size; z++) {
                            maker[3] = (char)('a' + z);
                            final String s = String.valueOf(maker) + i;
                            items[i] = new BadString(s);
                            i++;
                        }
                    }
                }
            }
        }
        WhiskerRandom rng = new WhiskerRandom(shuffleSeed);
        rng.shuffle(items);
        return items;
    }

    /**
     * Testing with BadStrings that use `(h * h) & h` for the hashCode...
     * <br>
     * average pileup: 0.3085174560546875
     * 71534200 ns
     * hash multiplier: D1B54A32D192ED03 with final size 200000
     * total collisions: 40438
     * longest pileup: 101
     * total of 12 pileups: 225
     * <br>
     * With BadStrings that use floatToIntBits:
     * <br>
     * average pileup: 0.1641387939453125
     * 52647700 ns
     * hash multiplier: EFAA28F1 with final size 200000
     * total collisions: 21514
     * longest pileup: 9
     * total of 12 pileups: 72
     */
    @Test
    public void testBadStringSetOld () {
        final BadString[] words = generateUniqueBad(LEN, -123456789L);
        long start = System.nanoTime();
        // replicates old ObjectSet behavior, with added logging
        ObjectSet set = new ObjectSet(64, LOAD) {
            long collisionTotal = 0;
            int longestPileup = 0, allPileups = 0, pileupChecks = 0;
            double averagePileup = 0;

            @Override
            protected int place (@NonNull Object item) {
                return (int)(item.hashCode() * 0xD1B54A32D192ED03L >>> shift); // if this long constant is the same as the one used
                // by place() in generateUniqueBadFibSet's map, and this uses that FibSet version, then this slows down massively.
            }

            @Override
            protected void addResize (@NonNull Object key) {
                Object[] keyTable = this.keyTable;
                for (int i = place(key), p = 0; ; i = i + 1 & mask) {
                    if (keyTable[i] == null) {
                        keyTable[i] = key;
                        averagePileup += p;

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
                shift = BitConversion.countLeadingZeros(mask) + 32;

                Object[] oldKeyTable = keyTable;

                keyTable = new Object[newSize];

                allPileups += longestPileup;
                pileupChecks++;
                collisionTotal = 0;
                longestPileup = 0;
                averagePileup = 0.0;

                if (size > 0) {
                    for (int i = 0; i < oldCapacity; i++) {
                        Object key = oldKeyTable[i];
                        if (key != null) {addResize(key);}
                    }
                }
                System.out.println("hash multiplier: " + Base.BASE16.unsigned(hashMultiplier) + " with new size " + newSize);
                System.out.println("total collisions: " + collisionTotal);
                System.out.println("longest pileup: " + longestPileup);
                System.out.println("average pileup: " + (averagePileup / size));
            }

            @Override
            public void clear () {
                System.out.println("hash multiplier: " + Base.BASE16.unsigned(hashMultiplier) + " with final size " + size);
                System.out.println("total collisions: " + collisionTotal);
                System.out.println("longest pileup: " + longestPileup);
                System.out.println("total of " + pileupChecks + " pileups: " + (allPileups + longestPileup));
                super.clear();
            }
        };

        for (int i = 0; i < LEN; i++) {
            set.add(words[i]);
        }
        System.out.println((System.nanoTime() - start) + " ns");
        set.clear();
    }

    /**
     * Testing with BadStrings that use `(h * h) & h` for the hashCode...
     * <br>
     * With some older method...
     * average pileup: 0.29967498779296875
     * 66977800 ns
     * hash multiplier: 620F7FCC96CA89A5 with final size 200000
     * total collisions: 39279
     * longest pileup: 74
     * total of 12 pileups: 177
     * <br>
     * Multiply with hashMultiplier, but hashMultiplier is squared every resize(), upper bits:
     * average pileup: 68.09420871734619
     * 16807753600 ns
     * hash multiplier: 24380001 with final size 2000000
     * total collisions: 71401953
     * longest pileup: 11073
     * total of 15 pileups: 16641
     * <br>
     * Multiply with a GOOD_MULTIPLIERS int, upper bits:
     * average pileup: 0.30040740966796875
     * 54214400 ns
     * hash multiplier: E993C987 with final size 200000
     * total collisions: 39375
     * longest pileup: 89
     * total of 12 pileups: 197
     * <br>
     * Same as above, but 2 million items:
     * average pileup: 1.2349624633789062
     * 577683700 ns
     * hash multiplier: CDCBFDE3 with final size 2000000
     * total collisions: 1294952
     * longest pileup: 585
     * total of 15 pileups: 1216
     * <br>
     * Same as above, 2 million items again, but selecting multiplier with {@code hashMultiplier >>> 16 & 511}:
     * average pileup: 1.246358871459961
     * 579392600 ns
     * hash multiplier: D68F0657 with final size 2000000
     * total collisions: 1306902
     * longest pileup: 628
     * total of 15 pileups: 1271
     * <br>
     * Same as above, 2 million items, but selecting multiplier with {@code 64 - shift}:
     * average pileup: 1.244131088256836
     * 585086400 ns
     * hash multiplier: BFA927CB with final size 2000000
     * total collisions: 1304566
     * longest pileup: 627
     * total of 15 pileups: 1239
     * <br>
     * Same as above, 2 million items, but selecting multiplier with {@code 63 - shift}:
     * average pileup: 1.2399606704711914
     * 587314300 ns
     * hash multiplier: C33AFB2F with final size 2000000
     * total collisions: 1300193
     * longest pileup: 627
     * total of 15 pileups: 1303
     * <br>
     * Same as above, 2 million items, but selecting multiplier with {@code shift}:
     * average pileup: 1.3248004913330078
     * 598893900 ns
     * hash multiplier: F16A6DCD with final size 2000000
     * total collisions: 1389154
     * longest pileup: 667
     * total of 15 pileups: 1295
     * <br>
     * Multiply with a GOOD_MULTIPLIERS int, lower bits:
     * average pileup: 54.30384063720703
     * 461599500 ns
     * hash multiplier: E993C987 with final size 200000
     * total collisions: 7117713
     * longest pileup: 2786
     * total of 12 pileups: 6817
     * <br>
     * Same as above, but 2 million items:
     * average pileup: 112.36906814575195
     * 15071464800 ns
     * hash multiplier: CDCBFDE3 with final size 2000000
     * total collisions: 117827508
     * longest pileup: 8642
     * total of 15 pileups: 24936
     * <br>
     * 2 million items multiplying by a GOOD, then mixing lower and upper bits with XOR:
     * average pileup: 1.2725563049316406
     * 614320900 ns
     * hash multiplier: CDCBFDE3 with final size 2000000
     * total collisions: 1334372
     * longest pileup: 591
     * total of 15 pileups: 1194
     * <br>
     * 2 million items multiplying by GOOD and then XRXR:
     * average pileup: 1.2690820693969727
     * 621597700 ns
     * hash multiplier: CDCBFDE3 with final size 2000000
     * total collisions: 1330729
     * longest pileup: 694
     * total of 15 pileups: 1311
     * <br>
     * 2 million items using {@code (h ^ h >>> 16) * hashMultiplier >>> shift}:
     * average pileup: 1.2753381729125977
     * 589847800 ns
     * hash multiplier: CDCBFDE3 with final size 2000000
     * total collisions: 1337289
     * longest pileup: 587
     * total of 15 pileups: 1192
     * <br>
     * 2 million items
     * <br>
     * With BadStrings that use floatToIntBits(h) for the hashCode...
     * <br>
     * Multiply with a GOOD_MULTIPLIERS int, upper bits:
     * average pileup: 0.21096038818359375
     * 45957000 ns
     * hash multiplier: E993C987 with final size 200000
     * total collisions: 27651
     * longest pileup: 12
     * total of 12 pileups: 79
     * <br>
     * Multiply with a GOOD_MULTIPLIERS int, lower bits:
     * average pileup: 0.17116546630859375
     * 39000200 ns
     * hash multiplier: E993C987 with final size 200000
     * total collisions: 22435
     * longest pileup: 11
     * total of 12 pileups: 78
     * <br>
     * With BadStrings that use BitConversion.doubleToMixedIntBits(h) for the hashCode...
     * <br>
     * Select GOOD_MULTIPLIER with {@code 64 - shift}, multiply and use upper bits:
     * average pileup: 0.16861629486083984
     * 423045800 ns
     * hash multiplier: BFA927CB with final size 2000000
     * total collisions: 176807
     * longest pileup: 13
     * total of 15 pileups: 112
     * <br>
     * With BadStrings that use a precomputed int by identity per BadString, but use a ++ counter...
     * <br>
     * Select GOOD_MULTIPLIER with {@code 64 - shift}, multiply and use upper bits:
     * average pileup: 0.01916980743408203
     * 270676000 ns
     * hash multiplier: BFA927CB with final size 2000000
     * total collisions: 20101
     * longest pileup: 1
     * total of 15 pileups: 96
     * <br>
     * With BadStrings that use a precomputed int by identity per BadString, but use a reversed-bits ++ counter...
     * <br>
     * Select GOOD_MULTIPLIER with {@code 64 - shift}, multiply and use upper bits:
     * average pileup: 0.0
     * 257917100 ns
     * hash multiplier: BFA927CB with final size 2000000
     * total collisions: 0
     * longest pileup: 0
     * total of 15 pileups: 75
     * <br>
     * With BadStrings that use a precomputed {@link System#identityHashCode(Object)} on the text...
     * <br>
     * Select GOOD_MULTIPLIER with {@code 64 - shift}, multiply and use upper bits:
     * average pileup: 0.16813087463378906
     * 326049100 ns
     * hash multiplier: BFA927CB with final size 2000000
     * total collisions: 176298
     * longest pileup: 13
     * total of 15 pileups: 116
     * <br>
     * With BadStrings that use a precomputed {@link com.github.tommyettinger.digital.Hasher#hashBulk(CharSequence)}
     * on the text...
     * <br>
     * Select GOOD_MULTIPLIER with {@code 64 - shift}, multiply and use upper bits:
     * average pileup: 0.16617584228515625
     * 324275400 ns
     * hash multiplier: BFA927CB with final size 2000000
     * total collisions: 174248
     * longest pileup: 14
     * total of 15 pileups: 106
     * <br>
     * With BadStrings that use a precomputed {@link com.github.tommyettinger.digital.Hasher#hash(CharSequence)}
     * on the text...
     * <br>
     * Select GOOD_MULTIPLIER with {@code 64 - shift}, multiply and use upper bits:
     * average pileup: 0.1665639877319336
     * 321122900 ns
     * hash multiplier: BFA927CB with final size 2000000
     * total collisions: 174655
     * longest pileup: 14
     * total of 15 pileups: 121
     * <br>
     * With BadStrings that use precomputed {@code Hasher.hash(1234567890123456789L, text)}:
     * on the text...
     * <br>
     * Select GOOD_MULTIPLIER with {@code 64 - shift}, multiply and use upper bits:
     * average pileup: 0.16701221466064453
     * 332232300 ns
     * hash multiplier: BFA927CB with final size 2000000
     * total collisions: 175125
     * longest pileup: 14
     * total of 15 pileups: 106
     * <br>
     * With BadStrings that use precomputed {@code Hasher.hashBulk(1234567890123456789L, text)}:
     * on the text...
     * <br>
     * Select GOOD_MULTIPLIER with {@code 64 - shift}, multiply and use upper bits:
     * average pileup: 0.16585063934326172
     * 301888300 ns
     * hash multiplier: BFA927CB with final size 2000000
     * total collisions: 173907
     * longest pileup: 13
     * total of 15 pileups: 106
     * <br>
     * Using just the normal String.hashCode() on text:
     * <br>
     * Select GOOD_MULTIPLIER with {@code 64 - shift}, multiply and use upper bits:
     * average pileup: 0.17447280883789062
     * 330542100 ns
     * hash multiplier: BFA927CB with final size 2000000
     * total collisions: 182948
     * longest pileup: 13
     * total of 15 pileups: 101
     * <br>
     * Selecting GOOD_MULTIPLIER with {@code hashMultiplier >>> 48 + shift}, upper bits:
     * average pileup: 0.15322017669677734
     * 324264400 ns
     * hash multiplier: CDCBFDE3 with final size 2000000
     * total collisions: 160663
     * longest pileup: 10
     * total of 15 pileups: 111
     */
    @Test
    public void testBadStringSetNew () {
        final BadString[] words = generateUniqueBad(LEN, -123456789L);
        long start = System.nanoTime();
        ObjectSet set = new ObjectSet(64, LOAD) {
            long collisionTotal = 0;
            int longestPileup = 0, allPileups = 0, pileupChecks = 0;
            double averagePileup = 0;

//            {
//                hashMultiplier = 0x9E3779B97F4A7C15L; // total collisions: 69101, longest pileup: 15
//                hashMultiplier = 0x769C3DC968DB6A07L; // total collisions: 74471, longest pileup: 14
//                hashMultiplier = 0xD1B54A32D192ED03L; // total collisions: 68210, longest pileup: 19
//            }

            @Override
            protected int place (@NonNull Object item) {
//                return BitConversion.imul(hashMultiplier, item.hashCode()) & mask;
//                return (hashMultiplier * item.hashCode()) & mask;
                return (hashMultiplier * item.hashCode()) >>> shift;
//                final int h = hashMultiplier * item.hashCode();
//                return h >>> shift;
//                return (h & mask) ^ h >>> shift;
//                return (h ^ (h << 21 | h >>> 11) ^ (h << 9 | h >>> 23)) & mask;
//                final int h = item.hashCode();
//                return (h ^ h >>> 16) * hashMultiplier >>> shift;
            }

            @Override
            protected void addResize (@NonNull Object key) {
                Object[] keyTable = this.keyTable;
                for (int i = place(key), p = 0; ; i = i + 1 & mask) {
                    if (keyTable[i] == null) {
                        keyTable[i] = key;
                        averagePileup += p;

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
                shift = BitConversion.countLeadingZeros(mask) + 32;

                // multiplier from Steele and Vigna, Computationally Easy, Spectrally Good Multipliers for Congruential
                // Pseudorandom Number Generators
//                hashMultiplier *= 0xF1357AEA2E62A9C5L;
                // ensures hashMultiplier is never too small, and is always odd
//                hashMultiplier |= 0x0000010000000001L;

                // we add a constant from Steele and Vigna, Computationally Easy, Spectrally Good Multipliers for Congruential
                // Pseudorandom Number Generators, times -8 to keep the bottom 3 bits the same every time.
                //361274435
//                hashMultiplier += 0xC3910C8D016B07D6L;//0x765428AE8CEAB1D8L;
                //211888218
                // we modify the hash multiplier by multiplying it by a number that Vigna and Steele considered optimal
                // for a 64-bit MCG random number generator, XORed with 2 times size to randomize the low bits more.
//                hashMultiplier *= 0xF1357AEA2E62A9C5L;//0x59E3779B97F4A7C1L;
//                hashMultiplier = (int)MathTools.GOLDEN_LONGS[(hashMultiplier >>> 48 + shift) & 1023];
//                hashMultiplier *= size + size ^ 0xF1357AEA2E62A9C5L;
//                hashMultiplier *= size + size ^ 0xF1357AEA2E62A9C5L;
//                hashMultiplier *= 0xF1357AEA2E62A9C5L;
                hashMultiplier = Utilities.GOOD_MULTIPLIERS[(hashMultiplier >>> 48 + shift) & 511];
//                hashMultiplier = Utilities.GOOD_MULTIPLIERS[hashMultiplier >>> 16 & 511];
//                hashMultiplier = Utilities.GOOD_MULTIPLIERS[hashMultiplier >>> 8 & 511];
//                hashMultiplier = Utilities.GOOD_MULTIPLIERS[hashMultiplier >>> 1 & 511];
//                hashMultiplier = Utilities.GOOD_MULTIPLIERS[64 - shift];
//                hashMultiplier *= hashMultiplier;

                Object[] oldKeyTable = keyTable;

                keyTable = new Object[newSize];

                allPileups += longestPileup;
                pileupChecks++;
                collisionTotal = 0;
                longestPileup = 0;
                averagePileup = 0.0;

                if (size > 0) {
                    for (int i = 0; i < oldCapacity; i++) {
                        Object key = oldKeyTable[i];
                        if (key != null) {addResize(key);}
                    }
                }
                System.out.println("hash multiplier: " + Base.BASE16.unsigned(hashMultiplier) + " with new size " + newSize);
                System.out.println("total collisions: " + collisionTotal);
                System.out.println("longest pileup: " + longestPileup);
                System.out.println("average pileup: " + (averagePileup / size));
            }

            @Override
            public void clear () {
                System.out.println("hash multiplier: " + Base.BASE16.unsigned(hashMultiplier) + " with final size " + size);
                System.out.println("total collisions: " + collisionTotal);
                System.out.println("longest pileup: " + longestPileup);
                System.out.println("total of " + pileupChecks + " pileups: " + (allPileups + longestPileup));
                super.clear();
            }
        };

        for (int i = 0; i < LEN; i++) {
            set.add(words[i]);
        }
        System.out.println((System.nanoTime() - start) + " ns");
        set.clear();
    }

    /**
     * Testing with BadStrings that use `(h * h) & h` for the hashCode...
     * <br>
     * Older method using GOOD_MULTIPLIERS
     * average pileup: 0.3052825927734375
     * 80341400 ns
     * hash multiplier: 8BA92E4143ACC451 with final size 200000
     * total collisions: 40014
     * longest pileup: 98
     * total of 12 pileups: 208
     * <br>
     * With XRR...
     * average pileup: 3.0543365478515625
     * 235703500 ns
     * final size: 200000
     * total collisions: 400338
     * longest pileup: 1435
     * total of 12 pileups: 2756
     * <br>
     * Same as above, but 2 million items:
     * average pileup: 12.597228050231934
     * 18113255800 ns
     * final size: 2000000
     * total collisions: 13209151
     * longest pileup: 5287
     * total of 15 pileups: 13421
     * <br>
     * With BadStrings that use floatToIntBits(h) for the hashCode...
     * <br>
     * With XRR...
     * average pileup: 0.21096038818359375
     * 40184400 ns
     * hash multiplier: E993C987 with final size 200000
     * total collisions: 27651
     * longest pileup: 12
     * total of 12 pileups: 79
     */
    @Test
    public void testBadStringSetCurrent () {
        final BadString[] words = generateUniqueBad(LEN, -123456789L);
        long start = System.nanoTime();
        com.github.tommyettinger.ds.ObjectSet set = new com.github.tommyettinger.ds.ObjectSet(64, LOAD) {
            long collisionTotal = 0;
            int longestPileup = 0, allPileups = 0, pileupChecks = 0;
            double averagePileup = 0;

            @Override
            protected void addResize (@NonNull Object key) {
                Object[] keyTable = this.keyTable;
                for (int i = place(key), p = 0; ; i = i + 1 & mask) {
                    if (keyTable[i] == null) {
                        keyTable[i] = key;
                        averagePileup += p;

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
                shift = BitConversion.countLeadingZeros(mask) + 32;

//                hashMultiplier = Utilities.GOOD_MULTIPLIERS[(hashMultiplier >>> 48 + shift) & 511];

                Object[] oldKeyTable = keyTable;

                keyTable = new Object[newSize];

                allPileups += longestPileup;
                pileupChecks++;
                collisionTotal = 0;
                longestPileup = 0;
                averagePileup = 0.0;

                if (size > 0) {
                    for (int i = 0; i < oldCapacity; i++) {
                        Object key = oldKeyTable[i];
                        if (key != null) {addResize(key);}
                    }
                }
                System.out.println("new size: " + newSize);
                System.out.println("total collisions: " + collisionTotal);
                System.out.println("longest pileup: " + longestPileup);
                System.out.println("average pileup: " + (averagePileup / size));
            }

            @Override
            public void clear () {
                System.out.println("final size: " + size);
                System.out.println("total collisions: " + collisionTotal);
                System.out.println("longest pileup: " + longestPileup);
                System.out.println("total of " + pileupChecks + " pileups: " + (allPileups + longestPileup));
                super.clear();
            }
        };

        for (int i = 0; i < LEN; i++) {
            set.add(words[i]);
        }
        System.out.println((System.nanoTime() - start) + " ns");
        set.clear();
    }

    @Test
    public void testBadStringQuadSet () {
        final BadString[] words = generateUniqueBad(LEN, -123456789L);
        long start = System.nanoTime();
        ObjectQuadSet set = new ObjectQuadSet(51, LOAD) {
            long collisionTotal = 0;
            int longestPileup = 0, allPileups = 0, pileupChecks = 0;
            double averagePileup = 0;

            @Override
            protected void addResize (@NonNull Object key) {
                Object[] keyTable = this.keyTable;
                for (int i = place(key), p = 0; ; i = i + p & mask) {
                    if (keyTable[i] == null) {
                        keyTable[i] = key;
                        averagePileup += p;

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
                shift = BitConversion.countLeadingZeros(mask) + 32;
//
//                // We modify the hash multiplier by... basically it just needs to stay odd, and use 21 bits or fewer (for GWT reasons).
//                // We incorporate the size in here to randomize things more. The multiplier seems to do a little better if it ends in the
//                // hex digit 5 or D -- this makes it a valid power-of-two-modulus MCG multiplier, which might help a bit. We also always
//                // set the bit 0x100000, so we know there will at least be some bits moved to the upper third or so.
//                hashMultiplier = ((hashMultiplier + size << 3 ^ 0xC79E7B1D) * 0x13C6EB + 0xAF36D01E & 0xFFFFF) | 0x100000;

                // we modify the hash multiplier by multiplying it by a number that Vigna and Steele considered optimal
                // for a 64-bit MCG random number generator, XORed with 2 times size to randomize the low bits more.
                hashMultiplier *= size + size ^ 0xF1357AEA2E62A9C5L;

                Object[] oldKeyTable = keyTable;

                keyTable = new Object[newSize];

                allPileups += longestPileup;
                pileupChecks++;
                collisionTotal = 0;
                longestPileup = 0;
                averagePileup = 0.0;

                if (size > 0) {
                    for (int i = 0; i < oldCapacity; i++) {
                        Object key = oldKeyTable[i];
                        if (key != null) {addResize(key);}
                    }
                }
                System.out.println("hash multiplier: " + Base.BASE16.unsigned(hashMultiplier) + " with new size " + newSize);
                System.out.println("total collisions: " + collisionTotal);
                System.out.println("longest pileup: " + longestPileup);
                System.out.println("average pileup: " + (averagePileup / size));
            }

            @Override
            public void clear () {
                System.out.println("hash multiplier: " + Base.BASE16.unsigned(hashMultiplier) + " with final size " + size);
                System.out.println("total collisions: " + collisionTotal);
                System.out.println("longest pileup: " + longestPileup);
                System.out.println("total of " + pileupChecks + " pileups: " + (allPileups + longestPileup));
                super.clear();
            }
        };
        for (int i = 0; i < LEN; i++) {
            set.add(words[i]);
        }
        System.out.println((System.nanoTime() - start) + " ns");
        set.clear();
    }

    @Test
    public void testBadStringQuadSetGold () {
        final BadString[] words = generateUniqueBad(LEN, -123456789L);
        long start = System.nanoTime();
        ObjectQuadSet set = new ObjectQuadSet(51, LOAD) {
            long collisionTotal = 0;
            int longestPileup = 0, allPileups = 0, pileupChecks = 0;
            double averagePileup = 0;

//            {
//                hashMultiplier = 0xD1B54A32D192ED03L;
//            }

            @Override
            protected void addResize (@NonNull Object key) {
                Object[] keyTable = this.keyTable;
                for (int i = place(key), p = 0; ; i = i + p & mask) {
                    if (keyTable[i] == null) {
                        keyTable[i] = key;
                        averagePileup += p;

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
                shift = BitConversion.countLeadingZeros(mask) + 32;
//
//                // We modify the hash multiplier by... basically it just needs to stay odd, and use 21 bits or fewer (for GWT reasons).
//                // We incorporate the size in here to randomize things more. The multiplier seems to do a little better if it ends in the
//                // hex digit 5 or D -- this makes it a valid power-of-two-modulus MCG multiplier, which might help a bit. We also always
//                // set the bit 0x100000, so we know there will at least be some bits moved to the upper third or so.
//                hashMultiplier = ((hashMultiplier + size << 3 ^ 0xC79E7B1D) * 0x13C6EB + 0xAF36D01E & 0xFFFFF) | 0x100000;

                // we modify the hash multiplier by multiplying it by a number that Vigna and Steele considered optimal
                // for a 64-bit MCG random number generator, XORed with 2 times size to randomize the low bits more.
                hashMultiplier *= 0xD1B54A32D192ED03L; //MathTools.GOLDEN_LONGS[64 - shift];

                Object[] oldKeyTable = keyTable;

                keyTable = new Object[newSize];

                allPileups += longestPileup;
                pileupChecks++;
                collisionTotal = 0;
                longestPileup = 0;
                averagePileup = 0.0;

                if (size > 0) {
                    for (int i = 0; i < oldCapacity; i++) {
                        Object key = oldKeyTable[i];
                        if (key != null) {addResize(key);}
                    }
                }
                System.out.println("hash multiplier: " + Base.BASE16.unsigned(hashMultiplier) + " with new size " + newSize);
                System.out.println("total collisions: " + collisionTotal);
                System.out.println("longest pileup: " + longestPileup);
                System.out.println("average pileup: " + (averagePileup / size));
            }

            @Override
            public void clear () {
                System.out.println("hash multiplier: " + Base.BASE16.unsigned(hashMultiplier) + " with final size " + size);
                System.out.println("total collisions: " + collisionTotal);
                System.out.println("longest pileup: " + longestPileup);
                System.out.println("total of " + pileupChecks + " pileups: " + (allPileups + longestPileup));
                super.clear();
            }
        };
//        final int limit = (int)(Math.sqrt(LEN));
//        for (int x = -limit; x < limit; x+=2) {
//            for (int y = -limit; y < limit; y+=2) {
//                set.add(new Vector2(x, y));
//            }
//        }
        for (int i = 0; i < LEN; i++) {
            set.add(words[i]);
        }
        System.out.println((System.nanoTime() - start) + " ns");
        set.clear();
    }

    /**
     * Generates an array of Vector2 points (with all integer components) in a spiral order, starting
     * at (0,0) and moving in a square spiral through all quadrants equally often. This can be a good
     * worst-case test for hash tables that don't adequately use the sign bit, since the float bits of
     * (-1,-1) and (1,1) are the same if you don't look at the sign bits.
     * Thanks to Jonathan M, <a href="https://stackoverflow.com/a/20591835">on Stack Overflow</a>.
     *
     * @param size
     * @return
     */
    public static Vector2[] generateVectorSpiral (int size) {
        Vector2[] result = new Vector2[size];
        for (int root = 0, index = 0; ; ++root) {
            for (int limit = index + root + root; index <= limit; ) {
                final int sign = -(root & 1);
                final int big = (root * (root + 1)) - index << 1;
                final int y = ((root + 1 >> 1) + sign ^ sign) + ((sign ^ sign + Math.min(big, 0)) >> 1);
                final int x = ((root + 1 >> 1) + sign ^ sign) - ((sign ^ sign + Math.max(big, 0)) >> 1);
                result[index] = new Vector2(x, y);
                if (++index >= size)
                    return result;
            }
        }
    }

    /**
     * Generates an array of Point2 points (with all integer components) in a series of concentric shells,
     * starting at (0,0) and moving through quadrant I.
     * Thanks to <a href="https://hbfs.wordpress.com/2018/08/07/moeud-deux/">Steven Pigeon's blog</a>.
     *
     * @param size
     * @return
     */
    public static Point2[] generatePointShells (int size) {
        return generatePointShells(size, 2);
    }

    public static Point2[] generatePointShells (int size, int reduction) {
        WhiskerRandom random = new WhiskerRandom(size);
        size <<= reduction;
        Point2[] result = new Point2[size];
        for (int root = 0, index = 0; ; ++root) {
            for (int limit = index + root + root; index <= limit; ) {
                final int r = index - root * root;
                final int x = (r < root) ? root : root + root - r;
                final int y = Math.min(r, root);
                result[index] = new Point2(x, y);
                if (++index >= size) {
                    random.shuffle(result);
                    return result;
                }
            }
        }
    }

    public static Point2[] generatePointRectangle(int width, int height) {
        return generatePointRectangle(width, height, 1);
    }

    public static Point2[] generatePointRectangle(int width, int height, int reduction) {
        WhiskerRandom random = new WhiskerRandom((long)width * height);
        width <<= reduction;
        height <<= reduction;
        Point2[] result = new Point2[width * height];
        for (int x = 0, index = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                result[index++] = new Point2(x, y);
            }
        }
        random.shuffle(result);
        return result;
    }

    /**
     * Generates an array of Point2 points (with all integer components) in a series of concentric triangle shapes,
     * starting at (0,0) and moving through quadrant I.
     * Thanks to <a href="https://en.wikipedia.org/wiki/Pairing_function#Inverting_the_Cantor_pairing_function">Wikipedia</a>.
     *
     * @param size
     * @return
     */
    public static Point2[] generatePointAngles (int size) {
        return generatePointAngles(size, 2);
    }
    public static Point2[] generatePointAngles (int size, int reduction) {
        WhiskerRandom random = new WhiskerRandom(size);
        size <<= reduction;
        Point2[] result = new Point2[size];
        for (int index = 0; ; ) {
            final int w = (int)(Math.sqrt(index << 3 | 1) - 1) >> 1;
            final int t = w * w + w;
            final int y = index - t;
            final int x = w - y;
            result[index] = new Point2(x, y);
            if (++index >= size) {
                random.shuffle(result);
                return result;
            }
        }
    }

    /**
     * Generates an array of Point2 points in a spiral order, starting at (0,0) and moving in a square
     * spiral through all quadrants equally often. This can be a good worst-case test for hash tables
     * that don't adequately handle negative inputs (like some point hashes that are only defined for
     * non-negative inputs). This is different from {@link #generateVectorSpiral(int)} because it
     * generates a 4x larger spiral, shuffles it, and expects only {@code size} elements to actually
     * be read from it (effectively drawing a random quarter of the points to be used).
     * Thanks to Jonathan M, <a href="https://stackoverflow.com/a/20591835">on Stack Overflow</a>.
     *
     * @param size
     * @return
     */
    public static Point2[] generatePointSpiral (int size) {
        return generatePointSpiral(size, 2);
    }
    public static Point2[] generatePointSpiral (int size, int reduction) {
        WhiskerRandom random = new WhiskerRandom(size);
        size <<= reduction;
        Point2[] result = new Point2[size];
        for (int root = 0, index = 0; ; ++root) {
            for (int limit = index + root + root; index <= limit; ) {
                final int sign = -(root & 1);
                final int big = (root * (root + 1)) - index << 1;
                final int y = ((root + 1 >> 1) + sign ^ sign) + ((sign ^ sign + Math.min(big, 0)) >> 1);
                final int x = ((root + 1 >> 1) + sign ^ sign) - ((sign ^ sign + Math.max(big, 0)) >> 1);
                result[index] = new Point2(x, y);
                if (++index >= size) {
                    random.shuffle(result);
                    return result;
                }
            }
        }
    }

    public static Point2[] generatePointScatter (int size) {
        long xc = 1L, yc = 2L;
        ObjectOrderedSet<Point2> pts = new ObjectOrderedSet<>(size);

        while (pts.size() < size) {
            // R2 sequence, sub-random with lots of space between nearby points
            xc += 0xC13FA9A902A6328FL;
            yc += 0x91E10DA5C79E7B1DL;
            // Using well-spread inputs to Probit() gives points that shouldn't overlap as often.
            // 1.1102230246251565E-16 is 2 to the -53 .
            pts.add(new Point2((int)(MathTools.probit((xc >>> 11) * 0x1p-53) * 1024.0), (int)(MathTools.probit((yc >>> 11) * 0x1p-53) * 1024.0)));
        }
        return pts.order().toArray(new Point2[size]);
    }

    public static final long CONSTANT =
//        0x9E3779B97F4A7C15L; //int: total collisions: 33748, longest pileup: 12, long: total collisions: 34124, longest pileup: 13
//        0xD1B54A32D192ED03L;//long: total collisions: 33579, longest pileup: 12
        0xF1357AEA2E62A9C5L;//long: total collisions: 34430, longest pileup: 11

    @Test
    public void testStringSetWordList () throws IOException {
        final List<String> words = Files.readAllLines(Paths.get("src/test/resources/word_list.txt"));
        Collections.shuffle(words, new WhiskerRandom(1234567890L));
        long start = System.nanoTime();
        ObjectSet set = new ObjectSet(51, LOAD) {
            long collisionTotal = 0;
            int longestPileup = 0, allPileups = 0, pileupChecks = 0;
            double averagePileup = 0;

            int hashMul =
//                0x0009EE97; //total collisions: 33705, longest pileup: 14 with hashMul =  hashMul * 0x9E377 & 0xFFFFF;
//                0x000357BB; //total collisions: 33736, longest pileup: 13 with hashMul =  hashMul * 0x9E377 & 0xFFFFF;
                0x000CF093; //total collisions: 34015, longest pileup: 10 with hashMul =  hashMul * 0x9E377 & 0xFFFFF;
//                0x0005303D; //total collisions: 33361, longest pileup: 14 with hashMul =  hashMul * 0x9E377 & 0xFFFFF;
//                0x00056511; //total collisions: 33857, longest pileup: 13 with hashMul =  hashMul * 0x9E377 & 0xFFFFF;
//                0x0003FAB9; //total collisions: 33877, longest pileup: 12 with hashMul =  hashMul * 0x9E377 & 0xFFFFF;
//                0x000824D5; //total collisions: 33081, longest pileup: 12 with hashMul =  hashMul * 0x9E377 & 0xFFFFF;
//                0x000AC73D; //total collisions: 33702, longest pileup: 12 with hashMul =  hashMul * 0x9E377 & 0xFFFFF;
//                0x00052137; //total collisions: 33885, longest pileup: 12 with hashMul =  hashMul * 0x9E377 & 0xFFFFF;
//                0x000D1007; //total collisions: 33767, longest pileup: 12 with hashMul =  hashMul * 0x9E377 & 0xFFFFF;
//                0x0006D1BD; //total collisions: 33256, longest pileup: 11 with hashMul =  hashMul * 0x9E377 & 0xFFFFF;
                //0x8A58E8C9; //total collisions: 33952, longest pileup: 11
//                0xEE1862A3; //total collisions: 33307, longest pileup: 14
//                0x95C0793F; // total collisions: 32984, longest pileup: 9 with hashMul *= 0x2E62A9C5;
//                0x95C0793F; // total collisions: 33466, longest pileup: 10 with hashMul *= 0x9E3779B9 + size + size;
//                0x04DA4427; // total collisions: 32925, longest pileup: 11 with hashMul *= 0x2E62A9C5 + size + size;
//                0x38442BE5; // total collisions: 33930, longest pileup: 11 with hashMul *= 0x2E62A9C5 ^ size + size;
//                0x92D390C1; // total collisions: 33478, longest pileup: 14 with hashMul *= 0x2E62A9C5 ^ size + size;
//                0xF0364419; // total collisions: 33307, longest pileup: 12 with hashMul *= 0x2E62A9C5 ^ size + size;
//            0xEB18A809; // total collisions: 33823, longest pileup: 15
//            0x9E3779B9; //total collisions: 33807, longest pileup: 14
            // 0x1A36A9;

            {
//                hashMultiplier = 0x9E3779B97F4A7C15L;
//                hashMultiplier = 0x9E3779B97F4A7C15L >>> shift + 32 | 1L; //total collisions: 34124, longest pileup: 13
//                hashMultiplier = 0xD1B54A32D192ED03L >>> shift + 32 | 1L; //total collisions: 33579, longest pileup: 12
//                hashMultiplier = 0xF1357AEA2E62A9C5L >>> shift + 32 | 1L; //total collisions: 34430, longest pileup: 11
//                hashMultiplier = CONSTANT >>> shift + 32 | 1L; //total collisions: 34430, longest pileup: 11
//                hashMultiplier = 0x769C3DC968DB6A07L;
                hashMul = (int)(hashMultiplier >>> 32);
            }

            @Override
            protected int place (@NonNull Object item) {
//                return item.hashCode() * hashMul >>> shift;
//                final int h = item.hashCode() * hashAddend;
//                return (h ^ h >>> 16) & mask; //total collisions: 1842294, longest pileup: 35
//                return (int)(item.hashCode() * hashMultiplier) >>> shift; // total collisions: 1757128,    longest pileup: 23 ( hashMultiplier *= 0xF1357AEA2E62A9C5L; )
//                return (int)(item.hashCode() * hashMultiplier >>> shift); // total collisions: 1761470,    longest pileup: 19
//                return (int)(item.hashCode() * 0xD1B54A32D192ED03L >>> shift); // total collisions: 2695641,    longest pileup: 41
//                return (int)(item.hashCode() * 0x9E3779B97F4A7C15L >>> shift); // total collisions: 5949677,    longest pileup: 65
                return (int)(item.hashCode() * hashMultiplier >>> shift); //
//                return (item.hashCode() & mask);                               // total collisions: 2783028662, longest pileup: 25751
//                final int h = item.hashCode() + hashAddend;
//                return (h ^ h >>> 12 ^ h >>> 22) & mask;                       // total collisions: 1786887, longest pileup: 35
//                return (h ^ h >>> 10 ^ h >>> 18) & mask;                       // total collisions: 1779695, longest pileup: 62
//                return (h ^ h >>> 12 ^ h >>> 23) & mask;                       // total collisions: 1799252, longest pileup: 43
//                return (h ^ h >>> 13 ^ h >>> 14) & mask;                       // total collisions: 1805792, longest pileup: 27 (slow?)
//                return (h ^ h >>> 11 ^ h >>> 21) & mask;                       // total collisions: 3691850, longest pileup: 101
//                return (h ^ h >>> 11 ^ h >>> 23) & mask;                       // total collisions: 1849222, longest pileup: 59
//                return (h ^ h >>> 9 ^ h >>> 25) & mask;                        // total collisions: 2027066, longest pileup: 36
//                return (h & mask) ^ (h * (0x1A36A9) >>> shift);                // total collisions: 1847298, longest pileup: 47
//                return (h & mask) ^ (h >>> shift);                             // total collisions: 1998198, longest pileup: 61
            }

            @Override
            protected void addResize (@NonNull Object key) {
                Object[] keyTable = this.keyTable;
                for (int i = place(key), p = 0; ; i = i + 1 & mask) {
                    if (keyTable[i] == null) {
                        keyTable[i] = key;
                        averagePileup += p;

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
                shift = BitConversion.countLeadingZeros(mask) + 32;

//                hashAddend = (hashAddend ^ hashAddend >>> 11 ^ size) * 0x13C6EB ^ 0xC79E7B1D;
//                hashMul *= 0x9E3779B9 + size + size;
//                hashMul *= 0x2E62A9C5 + size + size;
//                hashMul *= 0x2E62A9C5 ^ size + size;
//                hashMul =  hashMul * 0x9E377 & 0xFFFFF;
//                hashMultiplier *= (long)size << 3 ^ 0xF1357AEA2E62A9C5L;
//                hashMultiplier *= 0xF1357AEA2E62A9C5L;
//                hashMultiplier = CONSTANT >>> shift + 32 | 1L;
                hashMul = (int)(hashMultiplier >>> 32);

                Object[] oldKeyTable = keyTable;

                keyTable = new Object[newSize];

                allPileups += longestPileup;
                pileupChecks++;
                collisionTotal = 0;
                longestPileup = 0;
                averagePileup = 0.0;

                if (size > 0) {
                    for (int i = 0; i < oldCapacity; i++) {
                        Object key = oldKeyTable[i];
                        if (key != null) {addResize(key);}
                    }
                }
                System.out.println("hash multiplier: " + Base.BASE16.unsigned(hashMultiplier) + " with new size " + newSize);
                System.out.print("total collisions: " + collisionTotal);
                System.out.println(", longest pileup: " + longestPileup);
                System.out.println("average pileup: " + (averagePileup / size));
            }

            @Override
            public void clear () {
                System.out.println("hash multiplier: " + Base.BASE16.unsigned(hashMultiplier) + " with final size " + size);
                System.out.print("total collisions: " + collisionTotal);
                System.out.println(", longest pileup: " + longestPileup);
                System.out.println("total of " + pileupChecks + " longest pileups: " + (allPileups + longestPileup));
                super.clear();
            }
        };
        for (int i = 0; i < words.size(); i++) {
            set.add(words.get(i));
        }
        System.out.println((System.nanoTime() - start) + " ns");
        set.clear();
    }

    @Test
    public void testVariousSetWordList () throws IOException {
        CharFilter filter = CharFilter.getOrCreate("CaseInsensitive", c -> true, Character::toUpperCase);
        final IntSet vowels = IntSet.with('A', 'E', 'I', 'O', 'U', 'a', 'e', 'i', 'o', 'u');
        CharFilter noVowels = CharFilter.getOrCreate("NoVowelsCaseInsensitive", c -> !vowels.contains(c), Character::toUpperCase);

        FilteredStringOrderedSet wordSet = new FilteredStringOrderedSet(noVowels, Files.readAllLines(Paths.get("src/test/resources/word_list.txt")));
        final ObjectList<String> words = wordSet.order();
        Collections.shuffle(words, new WhiskerRandom(1234567890L));

        for(Set set : new Set[]{new MeasuredCaseInsensitiveSet(), new MeasuredFilteredStringSet(filter), }){
            long start = System.nanoTime();
            for (int i = 0; i < words.size(); i++) {
                set.add(words.get(i));
            }
            System.out.println((System.nanoTime() - start) + " ns");
            set.clear();
            System.out.println("\n");
        }
    }

    @Test
    public void testStringSetWordListRotating () throws IOException {
        final List<String> words = Files.readAllLines(Paths.get("src/test/resources/word_list.txt"));
        Collections.shuffle(words, new WhiskerRandom(1234567890L));
        for (int a = 1; a < 32; a += 4) {
            for (int b = 2; b < 32; b += 4) {
                // using Hasher:
                // (5, 18) (5, 22) (5, 26) (5, 30): total collisions: 33432, longest pileup: 11
                // (13, 18) (13, 22) (13, 26) (13, 30): total collisions: 33432, longest pileup: 11
                // (21, 18) (21, 22) (21, 26) (21, 30): total collisions: 33432, longest pileup: 11
                // using 2 rounds of int LCG, shifted over 16:
                // (1, 2) (1, 10) (1, 18) (5, 6) (5, 26) (9, 10) (9, 18) (9, 22) (13, 18) (17, 2) (21, 2) (21, 10) (21, 18) (25, 26) (29, 10) (29, 22) : total collisions: 33207, longest pileup: 11
                long start = System.nanoTime();
                // replicates old ObjectSet behavior, with added logging
                int finalA = a;
                int finalB = b;
                ObjectSet set = new ObjectSet(51, LOAD) {
                    long collisionTotal = 0;
                    int longestPileup = 0, allPileups = 0, pileupChecks = 0;
                    double averagePileup = 0;

                    int ra = finalA, rb = finalB; //total collisions: 33707, longest pileup: 17
                    int oa = ra, ob = rb;

                    @Override
                    protected int place (@NonNull Object item) {
                        final int h = item.hashCode();
                        return (h ^ (h << ra | h >>> -ra) ^ (h << rb | h >>> -rb)) & mask;
                    }

                    @Override
                    protected void addResize (@NonNull Object key) {
                        Object[] keyTable = this.keyTable;
                        for (int i = place(key), p = 0; ; i = i + 1 & mask) {
                            if (keyTable[i] == null) {
                                keyTable[i] = key;
                                averagePileup += p;

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
                        shift = BitConversion.countLeadingZeros(mask) + 32;

//                hashAddend = (hashAddend ^ hashAddend >>> 11 ^ size) * 0x13C6EB ^ 0xC79E7B1D;
//                hashMul *= 0x9E3779B9 + size + size;
//                hashMul *= 0x2E62A9C5 + size + size;
//                hashMul *= 0x2E62A9C5 ^ size + size;
//                hashMul =  hashMul * 0x9E377 & 0xFFFFF;
//                hashMultiplier *= (long)size << 3 ^ 0xF1357AEA2E62A9C5L;
//                hashMultiplier *= 0xF1357AEA2E62A9C5L;
//                        hashMultiplier *= 0xF1357AEA2E62A9C5L;
//                        ra = ((int)(hashMultiplier >>> 59) & 28) | 1;
//                        rb = ((int)hashMultiplier >>> 27 & 28) | 2;

//                        ra = ((int)Hasher.randomize3(ra + mask) & 28) | 1;
//                        rb = ((int)Hasher.randomize3(rb - mask) & 28) | 2;

                        ra = ((ra * 0xDE4D + 0x9E377 - rb) * 0xDE4D + 0x9E377 >>> 16 & 28) | 1;
                        rb = ((rb * 0xBA55 + 0x2ED03 - ra) * 0xBA55 + 0x2ED03 >>> 16 & 28) | 2;
                        Object[] oldKeyTable = keyTable;

                        keyTable = new Object[newSize];

                        allPileups += longestPileup;
                        pileupChecks++;
                        collisionTotal = 0;
                        longestPileup = 0;
                        averagePileup = 0.0;

                        if (size > 0) {
                            for (int i = 0; i < oldCapacity; i++) {
                                Object key = oldKeyTable[i];
                                if (key != null) {addResize(key);}
                            }
                        }
//                        System.out.println("rotations: " + ra + ", " + rb + ", with new size " + newSize);
//                        System.out.print("total collisions: " + collisionTotal);
//                        System.out.println(", longest pileup: " + longestPileup);
//                        System.out.println("average pileup: " + (averagePileup / size));
                    }

                    @Override
                    public void clear () {
                        System.out.println("rotations: " + oa + ", " + ob + ", with final size " + size);
                        System.out.print("total collisions: " + collisionTotal);
                        System.out.println(", longest pileup: " + longestPileup);
                        System.out.println("total of " + pileupChecks + " pileups: " + (allPileups + longestPileup));
                        super.clear();
                    }
                };

                for (int i = 0; i < words.size(); i++) {
                    set.add(words.get(i));
                }
//                System.out.println((System.nanoTime() - start) + " ns");
                set.clear();

            }
        }
    }

    @Test
    public void testVector2SetOld () {
        final Vector2[] spiral = generateVectorSpiral(LEN);
        long start = System.nanoTime();
        // replicates old ObjectSet behavior, with added logging
        ObjectSet set = new ObjectSet(51, LOAD) {
            long collisionTotal = 0;
            int longestPileup = 0, allPileups = 0, pileupChecks = 0;
            double averagePileup = 0;

            int hashMul = 0xEB18A809; // total collisions: 1752402, longest pileup: 21
            //0x9E3779B9;  // total collisions: 1759892,    longest pileup: 25 , maybe?
            // 0x1A36A9;

            {
//                hashMultiplier =
//                    0x9E3779B97F4A7C15L;
//                    0x769C3DC968DB6A07L;
//                    0xD1B54A32D192ED03L;//long: total collisions: 33579, longest pileup: 12
//                    0xF1357AEA2E62A9C5L;//long: total collisions: 34430, longest pileup: 11
            }

            @Override
            protected int place (@NonNull Object item) {
//                return item.hashCode() * hashMul >>> shift;
//                final int h = item.hashCode() * hashAddend;
//                return (h ^ h >>> 16) & mask; //total collisions: 1842294, longest pileup: 35
//                return (int)(item.hashCode() * hashMultiplier) >>> shift; // total collisions: 1757128,    longest pileup: 23 ( hashMultiplier *= 0xF1357AEA2E62A9C5L; )
                return (int)(item.hashCode() * hashMultiplier >>> shift); // total collisions: 1761470,    longest pileup: 19
//                return (int)(item.hashCode() * 0xD1B54A32D192ED03L >>> shift); // total collisions: 2695641,    longest pileup: 41
//                return (int)(item.hashCode() * 0x9E3779B97F4A7C15L >>> shift); // total collisions: 5949677,    longest pileup: 65
//                return (item.hashCode() & mask);                               // total collisions: 2783028662, longest pileup: 25751
//                final int h = item.hashCode() + hashAddend;
//                return (h ^ h >>> 12 ^ h >>> 22) & mask;                       // total collisions: 1786887, longest pileup: 35
//                return (h ^ h >>> 10 ^ h >>> 18) & mask;                       // total collisions: 1779695, longest pileup: 62
//                return (h ^ h >>> 12 ^ h >>> 23) & mask;                       // total collisions: 1799252, longest pileup: 43
//                return (h ^ h >>> 13 ^ h >>> 14) & mask;                       // total collisions: 1805792, longest pileup: 27 (slow?)
//                return (h ^ h >>> 11 ^ h >>> 21) & mask;                       // total collisions: 3691850, longest pileup: 101
//                return (h ^ h >>> 11 ^ h >>> 23) & mask;                       // total collisions: 1849222, longest pileup: 59
//                return (h ^ h >>> 9 ^ h >>> 25) & mask;                        // total collisions: 2027066, longest pileup: 36
//                return (h & mask) ^ (h * (0x1A36A9) >>> shift);                // total collisions: 1847298, longest pileup: 47
//                return (h & mask) ^ (h >>> shift);                             // total collisions: 1998198, longest pileup: 61
            }

            @Override
            protected void addResize (@NonNull Object key) {
                Object[] keyTable = this.keyTable;
                for (int i = place(key), p = 0; ; i = i + 1 & mask) {
                    if (keyTable[i] == null) {
                        keyTable[i] = key;
                        averagePileup += p;

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
                shift = BitConversion.countLeadingZeros(mask) + 32;

//                hashAddend = (hashAddend ^ hashAddend >>> 11 ^ size) * 0x13C6EB ^ 0xC79E7B1D;
//                hashMul = hashMul * 0x2E62A9C5;

                // this one is used in 1.0.5
//                hashMultiplier *= size + size ^ 0xF1357AEA2E62A9C5L;
//                hashMultiplier *= 0xF1357AEA2E62A9C5L;

//                hashMultiplier *= (long)size << 3 ^ 0xF1357AEA2E62A9C5L;


                Object[] oldKeyTable = keyTable;

                keyTable = new Object[newSize];

                allPileups += longestPileup;
                pileupChecks++;
                collisionTotal = 0;
                longestPileup = 0;
                averagePileup = 0.0;

                if (size > 0) {
                    for (int i = 0; i < oldCapacity; i++) {
                        Object key = oldKeyTable[i];
                        if (key != null) {addResize(key);}
                    }
                }
                System.out.println("hash multiplier: " + Base.BASE16.unsigned(hashMultiplier) + " with new size " + newSize);
                System.out.println("total collisions: " + collisionTotal);
                System.out.println("longest pileup: " + longestPileup);
                System.out.println("average pileup: " + (averagePileup / size));
            }

            @Override
            public void clear () {
                System.out.println("hash multiplier: " + Base.BASE16.unsigned(hashMultiplier) + " with final size " + size);
                System.out.println("total collisions: " + collisionTotal);
                System.out.println("longest pileup: " + longestPileup);
                System.out.println("total of " + pileupChecks + " pileups: " + (allPileups + longestPileup));
                super.clear();
            }
        };
//        final int limit = (int)(Math.sqrt(LEN));
//        for (int x = -limit; x < limit; x+=2) {
//            for (int y = -limit; y < limit; y+=2) {
//                set.add(new Vector2(x, y));
//            }
//        }
        for (int i = 0; i < LEN; i++) {
            set.add(spiral[i]);
        }
        System.out.println((System.nanoTime() - start) + " ns");
        set.clear();
    }

    /**
     * With 1M vectors and default settings, this gets:
     * average pileup: 19.86948013305664
     * 334691000 ns
     * total collisions: 10417330
     * longest pileup: 79
     * total of 14 pileups: 320
     * With 1M vectors and hashMultiplier starting at 0xD1B54A32D192ED03L, this gets:
     * average pileup: 7.402956008911133
     * 205292300 ns
     * total collisions: 3881281
     * longest pileup: 45
     * total of 14 pileups: 226
     */
    @Test
    public void testVector2SetNew () {
        final Vector2[] spiral = generateVectorSpiral(LEN);
        long start = System.nanoTime();
        ObjectSet set = new ObjectSet(51, LOAD) {
            long collisionTotal = 0;
            int longestPileup = 0, allPileups = 0, pileupChecks = 0;
            double averagePileup = 0;

//            {
//                hashMultiplier = 0xD1B54A32D192ED03L;
//            }
            @Override
            protected void addResize (@NonNull Object key) {
                Object[] keyTable = this.keyTable;
                for (int i = place(key), p = 0; ; i = i + 1 & mask) {
                    if (keyTable[i] == null) {
                        keyTable[i] = key;
                        averagePileup += p;

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
                shift = BitConversion.countLeadingZeros(mask) + 32;

                // multiplier from Steele and Vigna, Computationally Easy, Spectrally Good Multipliers for Congruential
                // Pseudorandom Number Generators
//                hashMultiplier *= 0xF1357AEA2E62A9C5L;
                // ensures hashMultiplier is never too small, and is always odd
//                hashMultiplier |= 0x0000010000000001L;

                // we add a constant from Steele and Vigna, Computationally Easy, Spectrally Good Multipliers for Congruential
                // Pseudorandom Number Generators, times -8 to keep the bottom 3 bits the same every time.
                //361274435
//                hashMultiplier += 0xC3910C8D016B07D6L;//0x765428AE8CEAB1D8L;
                //211888218
                // we modify the hash multiplier by multiplying it by a number that Vigna and Steele considered optimal
                // for a 64-bit MCG random number generator, XORed with 2 times size to randomize the low bits more.
//                hashMultiplier *= 0xF1357AEA2E62A9C5L;//0x59E3779B97F4A7C1L;
//                hashMultiplier *= MathTools.GOLDEN_LONGS[size & 1023];
//                hashMultiplier ^= size + size;
//                hashMultiplier *= 0xF1357AEA2E62A9C5L;

                // was using this in many tests
                // total 1788695, longest 33, average 5.686122731838816, sum 160
                hashMultiplier *= size + size ^ 0xF1357AEA2E62A9C5L;
//                hashMultiplier *= 0xF1357AEA2E62A9C5L;
//                hashMultiplier *= 0xF1357AEA2E62A9C5L; // average 5.898153681828007
//                hashMultiplier *= 0x9E3779B97F4A7C15L; // average 5.793621174166804
//                hashMultiplier *= 0xD1B54A32D192ED03L; // average 5.9661476545909995
//                hashMultiplier *= (long)size << 10 ^ 0xF1357AEA2E62A9C5L; // average 5.865185076866346
//                hashMultiplier = (hashMultiplier + size + size) * 0xF1357AEA2E62A9C5L + 0xD1B54A32D192ED03L ^ 0x9E3779B97F4A7C15L;
//                hashMultiplier = ((hashMultiplier + size << 3 ^ 0xE19B01AA9D42C631L) * 0xF1357AEA2E62A9C5L); // | 0x8000000000000000L; // + 0xC13FA9A902A6328EL

//                hashMultiplier = MathTools.GOLDEN_LONGS[64 - shift];
//                hashMultiplier = MathTools.GOLDEN_LONGS[size - shift & 255];

                Object[] oldKeyTable = keyTable;

                keyTable = new Object[newSize];

                allPileups += longestPileup;
                pileupChecks++;
                collisionTotal = 0;
                longestPileup = 0;
                averagePileup = 0.0;

                if (size > 0) {
                    for (int i = 0; i < oldCapacity; i++) {
                        Object key = oldKeyTable[i];
                        if (key != null) {addResize(key);}
                    }
                }
                System.out.println("hash multiplier: " + Base.BASE16.unsigned(hashMultiplier) + " with new size " + newSize);
                System.out.println("total collisions: " + collisionTotal);
                System.out.println("longest pileup: " + longestPileup);
                System.out.println("average pileup: " + (averagePileup / size));
            }

            @Override
            public void clear () {
                System.out.println("hash multiplier: " + Base.BASE16.unsigned(hashMultiplier) + " with final size " + size);
                System.out.println("total collisions: " + collisionTotal);
                System.out.println("longest pileup: " + longestPileup);
                System.out.println("total of " + pileupChecks + " pileups: " + (allPileups + longestPileup));
                super.clear();
            }
        };
//        final int limit = (int)(Math.sqrt(LEN));
//        for (int x = -limit; x < limit; x+=2) {
//            for (int y = -limit; y < limit; y+=2) {
//                set.add(new Vector2(x, y));
//            }
//        }
        for (int i = 0; i < LEN; i++) {
            set.add(spiral[i]);
        }
        System.out.println((System.nanoTime() - start) + " ns");
        set.clear();
    }

    /**
     * With 1M vectors and default settings, this gets:
     * average pileup: 8.4169921875
     * 241277500 ns
     * total collisions: 4412928
     * longest pileup: 103
     * total of 14 pileups: 325
     */
    @Test
    public void testVector2SetHarder () {
        final Vector2[] spiral = generateVectorSpiral(LEN);
        long start = System.nanoTime();
        ObjectSet set = new ObjectSet(51, LOAD) {
            long collisionTotal = 0;
            int longestPileup = 0, allPileups = 0, pileupChecks = 0;
            double averagePileup = 0;

//            {
//                hashMultiplier = 0x9E3779B97F4A7C15L;
//            }

            @Override
            protected int place (@NonNull Object item) {
// 97823400 ns, total collisions: 1917655, longest pileup: 125, average pileup: 7.315273284912109, total of 13 pileups: 428
//                return (int)Hasher.randomize2(hashMultiplier ^ item.hashCode()) & mask;
//77881700 ns, total collisions: 1564928, longest pileup: 67, average pileup: 5.9697265625, total of 13 pileups: 222
                final long y = item.hashCode() * 0xC13FA9A902A6328FL + hashMultiplier;
                return (int)(y ^ y >>> 32) & mask;
            }

            @Override
            protected void addResize (@NonNull Object key) {
                Object[] keyTable = this.keyTable;
                for (int i = place(key), p = 0; ; i = i + 1 & mask) {
                    if (keyTable[i] == null) {
                        keyTable[i] = key;
                        averagePileup += p;

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
                shift = BitConversion.countLeadingZeros(mask) + 32;

                hashMultiplier = Utilities.GOOD_MULTIPLIERS[(hashMultiplier ^ hashMultiplier >>> 17 ^ shift) & 511];
//                hashMultiplier *= size + size ^ 0xF1357AEA2E62A9C5L;

//                hashMultiplier = (size + size ^ hashMultiplier) * 0xD1342543DE82EF95L + 0xF1357AEA2E62A9C5L;

//                hashMultiplier = (hashMultiplier) * 0xD1342543DE82EF95L + 0xF1357AEA2E62A9C5L ^ size + size;

//79389600 ns, total collisions: 1604133, longest pileup: 57, average pileup: 6.119281768798828, total of 13 pileups: 241
//                hashMultiplier = (hashMultiplier ^ (hashMultiplier * hashMultiplier | 5L)) * 0xD1B54A32D192ED03L + size;

//                hashMultiplier = (hashMultiplier ^ (hashMultiplier * hashMultiplier | 5L)) * 0xD1B54A32D192ED03L ^ size + size;


                Object[] oldKeyTable = keyTable;

                keyTable = new Object[newSize];

                allPileups += longestPileup;
                pileupChecks++;
                collisionTotal = 0;
                longestPileup = 0;
                averagePileup = 0.0;

                if (size > 0) {
                    for (int i = 0; i < oldCapacity; i++) {
                        Object key = oldKeyTable[i];
                        if (key != null) {addResize(key);}
                    }
                }
                System.out.println("hash multiplier: " + Base.BASE16.unsigned(hashMultiplier) + " with new size " + newSize);
                System.out.println("total collisions: " + collisionTotal);
                System.out.println("longest pileup: " + longestPileup);
                System.out.println("average pileup: " + (averagePileup / size));
            }

            @Override
            public void clear () {
                System.out.println("hash multiplier: " + Base.BASE16.unsigned(hashMultiplier) + " with final size " + size);
                System.out.println("total collisions: " + collisionTotal);
                System.out.println("longest pileup: " + longestPileup);
                System.out.println("total of " + pileupChecks + " pileups: " + (allPileups + longestPileup));
                super.clear();
            }
        };
//        final int limit = (int)(Math.sqrt(LEN));
//        for (int x = -limit; x < limit; x+=2) {
//            for (int y = -limit; y < limit; y+=2) {
//                set.add(new Vector2(x, y));
//            }
//        }
        for (int i = 0; i < LEN; i++) {
            set.add(spiral[i]);
        }
        System.out.println((System.nanoTime() - start) + " ns");
        set.clear();
    }


    @Test
    public void testVector2QuadSet () {
        final Vector2[] spiral = generateVectorSpiral(LEN);
        long start = System.nanoTime();
        ObjectQuadSet set = new ObjectQuadSet(51, LOAD) {
            long collisionTotal = 0;
            int longestPileup = 0, allPileups = 0, pileupChecks = 0;
            double averagePileup = 0;

//            {
//                hashMultiplier = 0xD1B54A32D192ED03L;
//            }

            @Override
            protected int place (Object item) {
                return (int)(hashMultiplier * item.hashCode()) >>> shift;
            }

            @Override
            protected void addResize (@NonNull Object key) {
                Object[] keyTable = this.keyTable;
                for (int i = place(key), p = 0; ; i = i + p & mask) {
                    if (keyTable[i] == null) {
                        keyTable[i] = key;
                        averagePileup += p;
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
                shift = BitConversion.countLeadingZeros(mask) + 32;

//                // We modify the hash multiplier by... basically it just needs to stay odd, and use 21 bits or fewer (for GWT reasons).
//                // We incorporate the size in here to randomize things more. The multiplier seems to do a little better if it ends in the
//                // hex digit 5 or D -- this makes it a valid power-of-two-modulus MCG multiplier, which might help a bit. We also always
//                // set the bit 0x100000, so we know there will at least be some bits moved to the upper third or so.
//                hashMultiplier = ((hashMultiplier + size << 3 ^ 0xC79E7B1D) * 0x13C6EB + 0xAF36D01E & 0xFFFFF) | 0x100000;

                // we modify the hash multiplier by multiplying it by a number that Vigna and Steele considered optimal
                // for a 64-bit MCG random number generator, XORed with 2 times size to randomize the low bits more.
//                hashMultiplier *= size + size ^ 0xF1357AEA2E62A9C5L;
                hashMultiplier *= 0xF1357AEA2E62A9C5L; // average 5.861974365169182
//                hashMultiplier *= 0xD1B54A32D192ED03L; // average 6.411664102335872
//                hashMultiplier *= 0x9E3779B97F4A7C15L; // average 6.314214233943263
//                hashMultiplier *= 0xF1357AEA2E62A9C5L ^ Long.reverseBytes(size); // average 6.339461236219371
//                hashMultiplier *= (long)size << 4 ^ 0xF1357AEA2E62A9C5L; // average 6.149158221329298
//                hashMultiplier *= (long)size << 6 ^ 0xF1357AEA2E62A9C5L; // average 6.166817135663695
//                hashMultiplier *= (long)size << 10 ^ 0xF1357AEA2E62A9C5L; // average 5.93734025914576
//                hashMultiplier *= (long)size << 27 ^ 0xF1357AEA2E62A9C5L; // average 6.140517910049209
//                hashMultiplier *= (long)size << 11 ^ 0xD1B54A32D192ED03L; // average 6.031191587299569
//                hashMultiplier *= (long)size << 13 ^ 0x9E3779B97F4A7C15L; // average 6.060720598146053
//                hashMultiplier *= (long)size << 3 ^ 0x123456789ABCDEFL; // average 6.060720598146053
//
//                hashMultiplier = MathTools.GOLDEN_LONGS[64 - shift];
//                hashMultiplier = MathTools.GOLDEN_LONGS[(int)((size * 320L >>> 1) % 509L)]; // average 6.126352631512023
//                hashMultiplier = MathTools.GOLDEN_LONGS[(int)((size * 70L >>> 1) % 509L)]; // average 6.814363643299467
//                hashMultiplier = MathTools.GOLDEN_LONGS[(int)((size * 472L >>> 1) % 509L)]; // average 6.354624696412904
//                hashMultiplier = MathTools.GOLDEN_LONGS[(int)((size * 220L >>> 1) % 509L)]; // average 6.2165990615820865
//                hashMultiplier = MathTools.GOLDEN_LONGS[(int)((size * 798L >>> 1) % 509L)]; // average 6.1938061874547
//                hashMultiplier = MathTools.GOLDEN_LONGS[(int)((size * 546L >>> 1) % 509L)]; // average 6.567555281461796
//                hashMultiplier = MathTools.GOLDEN_LONGS[(int)((size * 948L >>> 1) % 509L)]; // average 10.943815724222118
//                hashMultiplier = MathTools.GOLDEN_LONGS[(int)((size * 698L >>> 1) % 509L)]; // average 7.3446110906247215

//                hashMultiplier = MathTools.GOLDEN_LONGS[(int)((size * 802L >>> 1) % 1021L)]; // average 6.050716529125288, interestingly this isn't one of the good multipliers (see L'Ecuyer errata)
//                hashMultiplier = MathTools.GOLDEN_LONGS[(int)((size * 1288L >>> 1) % 1021L)]; // average 6.232080414022863
//                hashMultiplier = MathTools.GOLDEN_LONGS[(int)((size * 1912L >>> 1) % 1021L)]; // average 6.610314967638569
//                hashMultiplier = MathTools.GOLDEN_LONGS[(int)((size * 130L >>> 1) % 1021L)]; // average 6.241976399679564
//                hashMultiplier = MathTools.GOLDEN_LONGS[(int)((size * 662L >>> 1) % 1021L)]; // average 6.261281995854685, interestingly this isn't one of the good multipliers (see L'Ecuyer errata)
//                hashMultiplier = MathTools.GOLDEN_LONGS[(int)((size * 774L >>> 1) % 1021L)]; // average 6.815190163142301
//                hashMultiplier = MathTools.GOLDEN_LONGS[(64 - shift) * 21 & 1023]; // average 6.024830563432219
//                hashMultiplier = MathTools.GOLDEN_LONGS[size + 64 - shift & 1023]; // average 6.062132039723815
//                hashMultiplier *= MathTools.GOLDEN_LONGS[size + 64 - shift & 1022]; // average 5.971809315514413
                //(int)((size * 320L >>> 1) % 509L)

                Object[] oldKeyTable = keyTable;

                keyTable = new Object[newSize];

                allPileups += longestPileup;
                pileupChecks++;
                collisionTotal = 0;
                longestPileup = 0;
                averagePileup = 0.0;

                if (size > 0) {
                    for (int i = 0; i < oldCapacity; i++) {
                        Object key = oldKeyTable[i];
                        if (key != null) {addResize(key);}
                    }
                }
                System.out.println("hash multiplier: " + Base.BASE16.unsigned(hashMultiplier) + " with new size " + newSize);
                System.out.println("total collisions: " + collisionTotal);
                System.out.println("longest pileup: " + longestPileup);
                System.out.println("average pileup: " + (averagePileup / size));
            }

            @Override
            public void clear () {
                System.out.println("hash multiplier: " + Base.BASE16.unsigned(hashMultiplier) + " with final size " + size);
                System.out.println("total collisions: " + collisionTotal);
                System.out.println("longest pileup: " + longestPileup);
                System.out.println("total of " + pileupChecks + " pileups: " + (allPileups + longestPileup));
                super.clear();
            }
        };
        for (int i = 0; i < LEN; i++) {
            set.add(spiral[i]);
        }
        System.out.println((System.nanoTime() - start) + " ns");
        set.clear();
    }


    @Test
    public void testPointSetShells () {
        final Point2[] shells = generatePointShells(LEN);
        final IntIntToIntBiFunction[] hashes = {
            ((x, y) -> x * 0x125493 + y * 0x19E373), // hash 0 1MS strong, 2A strong, 3MA strong
            ((x, y) -> x * 0xDEED5 + y * 0xBEA57), // hash 1 1MS strong, 2A strong, 3MA strong
            ((x, y) -> 31 * x + y), // hash 2 1MS fail 131072/500000, 2A fail 65536/500000, 3MA fail 131072/500000
            ((x, y) -> (x >= y ? x * (x + 8) - y + 12 : y * (y + 6) + x + 12)), // hash 3 1MS strong, 2A strong, 3MA strong
            ((x, y) -> {int n = (x >= y ? x * (x + 8) - y + 12 : y * (y + 6) + x + 12); return n ^ n >>> 1;}), // hash 4 1MS strong, 2A strong, 3MA strong
            ((x, y) -> {int n = (x >= y ? x * (x + 8) - y + 12 : y * (y + 6) + x + 12); return ((n ^ n >>> 1) * 0x9E373 ^ 0xD1B54A35) * 0x125493 ^ 0x91E10DA5;}), // hash 5 1MS strong, 2A strong, 3MA strong
            ((x, y) -> y + ((x + y + 6) * (x + y + 7) >>> 1)), // hash 6 1MS strong, 2A strong, 3MA strong
            ((x, y) -> {int n = (y + ((x + y + 6) * (x + y + 7) >> 1)); return n ^ n >>> 1;}), // hash 7 1MS strong, 2A strong, 3MA strong
            ((x, y) -> {int n = (y + ((x + y + 6) * (x + y + 7) >> 1)); return ((n ^ n >>> 1) * 0x9E373 ^ 0xD1B54A35) * 0x125493 ^ 0x91E10DA5;}), // hash 8 1MS strong, 2A strong, 3MA strong
            ((x, y) -> y + ((x + y) * (x + y + 1) + 36 >>> 1)), // hash 9 1MS strong, 2A strong, 3MA strong
            ((x, y) -> {
                x |= y << 16;
                x =    ((x & 0x0000ff00) << 8) | ((x >>> 8) & 0x0000ff00) | (x & 0xff0000ff);
                x =    ((x & 0x00f000f0) << 4) | ((x >>> 4) & 0x00f000f0) | (x & 0xf00ff00f);
                x =    ((x & 0x0c0c0c0c) << 2) | ((x >>> 2) & 0x0c0c0c0c) | (x & 0xc3c3c3c3);
                return ((x & 0x22222222) << 1) | ((x >>> 1) & 0x22222222) | (x & 0x99999999);
            }), // hash 10 1MS strong, 2A strong, 3MA strong
            ((x, y) -> (x ^ (y << 16 | y >>> 16))), // hash 11 1MS strong, 2A fail 2048/500000, 3MA fail 16384/500000
            ((x, y) -> (x + (y << 16 | y >>> 16))), // hash 12 1MS strong, 2A fail 2048/500000, 3MA fail 16384/500000
            ((x, y) -> x ^ y ^ (BitConversion.imul(y, y) | 1)), // hash 13 1MS strong, 2A fail 65536/500000, 3MA strong
            ((x, y) -> BitConversion.imul(x, 0xC13FA9A9) + BitConversion.imul(0x91E10DA5, y)), // hash 14 1MS strong, 2A strong, 3MA strong
            ((x, y) -> x * 0xC13F + y * 0x91E1), // hash 15 1MS strong, 2A strong, 3MA strong
            ((x, y) -> x * 0x7587 + y * 0x6A89), // hash 16 1MS strong, 2A strong, 3MA strong
        };

        // for method 1MS, hash 15 is fastest, followed by hash 16.
        // for method 2A , hash 3 is fastest, followed by hash 16.
        // for method 3MA, hash 16 is fastest, followed by hash 7.

        /* // Very rough speed benchmark. This needs to be verified with JMH or BumbleBench!
    1MS                          2A                           3MA
12: 74702300 ns            |  3: 63187100 ns            | 16: 71190400 ns
15: 75322000 ns            |  4: 65759900 ns            |  7: 75657500 ns
 4: 76360700 ns            |  8: 72703400 ns            |  4: 75894500 ns
 7: 77831800 ns            |  7: 74673300 ns            |  3: 76486400 ns
 9: 79387600 ns            |  9: 76345800 ns            | 10: 77296100 ns
 6: 79399500 ns            | 14: 79661000 ns            |  8: 77299300 ns
13: 80128900 ns            |  6: 80823300 ns            | 15: 78662700 ns
 8: 80272200 ns            | 15: 82385200 ns            |  9: 83325900 ns
11: 82534100 ns            | 16: 99081800 ns            |  6: 85033800 ns
16: 85207800 ns            |  5: 106265600 ns           |  5: 97736000 ns
14: 85919200 ns            |  0: 120346200 ns           | 13: 100021900 ns
 3: 88574100 ns            |  1: 131019300 ns           |  1: 110444800 ns
10: 100581900 ns           | 10: 165088500 ns           |  0: 125566300 ns
 5: 108926500 ns           |  2: 9223372036854775807 ns | 14: 1130961600 ns
 1: 120539900 ns           | 11: 9223372036854775807 ns |  2: 9223372036854775807 ns
 0: 125462900 ns           | 12: 9223372036854775807 ns | 11: 9223372036854775807 ns
 2: 9223372036854775807 ns | 13: 9223372036854775807 ns | 12: 9223372036854775807 ns
         */


        int index = -1;
        IntLongOrderedMap timing = new IntLongOrderedMap(hashes.length);
        for(IntIntToIntBiFunction op : hashes) {
            ++index;
            System.out.println("Working with hash " + index + ":");
            final IntIntToIntBiFunction hash = op;
            long start = System.nanoTime();
            ObjectSet set = new ObjectSet(51, LOAD) {
                long collisionTotal = 0;
                int longestPileup = 0, allPileups = 0, pileupChecks = 0;
                double averagePileup = 0;

                //            {
//                hashMultiplier = 0xD1B54A32D192ED03L;
//            }
                int hashMul = (int)(hashMultiplier & 0x1FFFFFL);

                @Override
                protected int place (@NonNull Object item) {
                    final Point2 p = (Point2)item;
//                    return (int)(hash.applyAsInt(p.x, p.y) * hashMultiplier >>> shift);
//                    return hash.applyAsInt(p.x, p.y) & mask;
                    return hash.applyAsInt(p.x, p.y) * hashMul & mask;
                }

                @Override
                protected void addResize (@NonNull Object key) {
                    Object[] keyTable = this.keyTable;
                    for (int i = place(key), p = 0; ; i = i + 1 & mask) {
                        if (keyTable[i] == null) {
                            keyTable[i] = key;
                            averagePileup += p;

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
                    shift = BitConversion.countLeadingZeros(mask) + 32;

                    hashMultiplier *= size + size ^ 0xF1357AEA2E62A9C5L;
                    hashMul = (int)(hashMultiplier & 0x1FFFFFL);

                    Object[] oldKeyTable = keyTable;

                    keyTable = new Object[newSize];

                    allPileups += longestPileup;
                    pileupChecks++;
                    collisionTotal = 0;
                    longestPileup = 0;
                    averagePileup = 0.0;

                    if (size > 0) {
                        for (int i = 0; i < oldCapacity; i++) {
                            Object key = oldKeyTable[i];
                            if (key != null) {addResize(key);}
                        }
                    }
                    if(collisionTotal > 150000) throw new IllegalStateException("UH OH");
//                    System.out.println("hash multiplier: " + Base.BASE16.unsigned(hashMultiplier) + " with new size " + newSize);
//                    System.out.println("total collisions: " + collisionTotal);
//                    System.out.println("longest pileup: " + longestPileup);
//                    System.out.println("average pileup: " + (averagePileup / size));
                }

                @Override
                public void clear () {
//                    System.out.println("hash multiplier: " + Base.BASE16.unsigned(hashMultiplier) + " with final size " + size);
                    System.out.println("total collisions: " + collisionTotal + ", longest pileup: " + longestPileup);
                    System.out.println("total of " + pileupChecks + " pileups: " + (allPileups + longestPileup));
                    super.clear();
                }
            };
//        final int limit = (int)(Math.sqrt(LEN));
//        for (int x = -limit; x < limit; x+=2) {
//            for (int y = -limit; y < limit; y+=2) {
//                set.add(new Vector2(x, y));
//            }
//        }
            try {
                for (int i = 0; i < LEN; i++) {
                    set.add(shells[i]);
                }
                System.out.println("strong");
                long time = System.nanoTime() - start;
                System.out.println(time);
                timing.put(index, time);
            } catch (IllegalStateException ex) {
                System.out.println("Way too many collisions!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                System.out.println("fail " + set.size() + "/" + LEN);
                timing.put(index, Long.MAX_VALUE);
            }
            set.clear();
        }
        timing.sortByValue(LongComparators.NATURAL_COMPARATOR);
        for(IntLongMap.Entry ent : timing.entrySet()){
            System.out.printf("%2d: %d ns\n", ent.key, ent.value);
        }
        System.gc();
    }

    @Test
    public void testPointSetAngles () {
        final Point2[] shells = generatePointAngles(LEN);
        IntIntToIntBiFunction[] hashes = {
            ((x, y) -> x * 0x125493 + y * 0x19E373), // 1MS strong, 2A strong, 3MA strong
            ((x, y) -> x * 0xDEED5 + y * 0xBEA57), // 1MS strong, 2A strong, 3MA strong
            ((x, y) -> (int)((x * 107 + y) * 0xD1B54A32D192ED03L >>> 32)), // 1MS strong, 2A strong, 3MA strong
            ((x, y) -> (x >= y ? x * (x + 8) - y + 12 : y * (y + 6) + x + 12)), // 1MS strong, 2A strong, 3MA strong
            ((x, y) -> {int n = (x >= y ? x * (x + 8) - y + 12 : y * (y + 6) + x + 12); return n ^ n >>> 1;}), // 1MS strong, 2A strong, 3MA strong
            ((x, y) -> {int n = (x >= y ? x * (x + 8) - y + 12 : y * (y + 6) + x + 12); return ((n ^ n >>> 1) * 0x9E373 ^ 0xD1B54A35) * 0x125493 ^ 0x91E10DA5;}), // 1MS strong, 2A strong, 3MA strong
            ((x, y) -> y + ((x + y + 6) * (x + y + 7) >>> 1)), // 1MS fail 78643/500000, 2A fail 9830/500000, 3MA fail 78643/500000
            ((x, y) -> {int n = (y + ((x + y + 6) * (x + y + 7) >> 1)); return n ^ n >>> 1;}), // 1MS fail 78643/500000, 2A fail 9830/500000, 3MA fail 78643/500000
            ((x, y) -> {int n = (y + ((x + y + 6) * (x + y + 7) >> 1)); return ((n ^ n >>> 1) * 0x9E373 ^ 0xD1B54A35) * 0x125493 ^ 0x91E10DA5;}), // 1MS fail 78643/500000, 2A fail 78643/500000, 3MA fail 78643/500000
            ((x, y) -> y + ((x + y) * (x + y + 1) + 36 >>> 1)), // 1MS fail 39321/500000, 2A 2457/500000, 3MA fail 39321/500000
            ((x, y) -> {
                x |= y << 16;
                x =    ((x & 0x0000ff00) << 8) | ((x >>> 8) & 0x0000ff00) | (x & 0xff0000ff);
                x =    ((x & 0x00f000f0) << 4) | ((x >>> 4) & 0x00f000f0) | (x & 0xf00ff00f);
                x =    ((x & 0x0c0c0c0c) << 2) | ((x >>> 2) & 0x0c0c0c0c) | (x & 0xc3c3c3c3);
                return ((x & 0x22222222) << 1) | ((x >>> 1) & 0x22222222) | (x & 0x99999999);
            }), // 1MS strong, 2A fail 78643/500000, 3MA fail 314572/500000
            ((x, y) -> (x ^ (y << 16 | y >>> 16))), // 1MS strong, 2A strong, 3MA strong
            ((x, y) -> (x + (y << 16 | y >>> 16))), // 1MS strong, 2A strong, 3MA strong
        };
        int index = 0;
        for(IntIntToIntBiFunction op : hashes) {
            System.out.println("Working with hash " + index++ + ":");
            final IntIntToIntBiFunction hash = op;
            long start = System.nanoTime();
            ObjectSet set = new ObjectSet(51, LOAD) {
                long collisionTotal = 0;
                int longestPileup = 0, allPileups = 0, pileupChecks = 0;
                double averagePileup = 0;

                //            {
//                hashMultiplier = 0xD1B54A32D192ED03L;
//            }
                int hashMul = (int)(hashMultiplier & 0x1FFFFFL);

                @Override
                protected int place (@NonNull Object item) {
                    final Point2 p = (Point2)item;
//                    return (int)(hash.applyAsInt(p.x, p.y) * hashMultiplier >>> shift); // option 1MS
                    return hash.applyAsInt(p.x, p.y) & mask; // option 2A
//                    return hash.applyAsInt(p.x, p.y) * hashMul & mask; // option 3MA
                }

                @Override
                protected void addResize (@NonNull Object key) {
                    Object[] keyTable = this.keyTable;
                    for (int i = place(key), p = 0; ; i = i + 1 & mask) {
                        if (keyTable[i] == null) {
                            keyTable[i] = key;
                            averagePileup += p;

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
                    shift = BitConversion.countLeadingZeros(mask) + 32;

                    // was using this in many tests
                    // total 1788695, longest 33, average 5.686122731838816, sum 160
                    hashMultiplier *= size + size ^ 0xF1357AEA2E62A9C5L;

                    hashMul = (int)(hashMultiplier & 0x1FFFFFL);

                    Object[] oldKeyTable = keyTable;

                    keyTable = new Object[newSize];

                    allPileups += longestPileup;
                    pileupChecks++;
                    collisionTotal = 0;
                    longestPileup = 0;
                    averagePileup = 0.0;

                    if (size > 0) {
                        for (int i = 0; i < oldCapacity; i++) {
                            Object key = oldKeyTable[i];
                            if (key != null) {addResize(key);}
                        }
                    }
                    if(collisionTotal > 150000) throw new IllegalStateException("UH OH");

//                    System.out.println("hash multiplier: " + Base.BASE16.unsigned(hashMultiplier) + " with new size " + newSize);
//                    System.out.println("total collisions: " + collisionTotal);
//                    System.out.println("longest pileup: " + longestPileup);
//                    System.out.println("average pileup: " + (averagePileup / size));
                }

                @Override
                public void clear () {
//                    System.out.println("hash multiplier: " + Base.BASE16.unsigned(hashMultiplier) + " with final size " + size);
                    System.out.println("total collisions: " + collisionTotal + ", longest pileup: " + longestPileup);
                    System.out.println("total of " + pileupChecks + " pileups: " + (allPileups + longestPileup));
                    super.clear();
                }
            };
            try {
                for (int i = 0; i < LEN; i++) {
                    set.add(shells[i]);
                }
                System.out.println("strong");
            }catch (IllegalStateException ex) {
                System.out.println("Way too many collisions!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                System.out.println("fail " + set.size() + "/" + LEN);
            }
            System.out.println((System.nanoTime() - start) + " ns");
            set.clear();
        }
    }

    @Test
    public void testPointSetSpiral () {
        final Point2[] shells = generatePointSpiral(LEN);
        IntIntToIntBiFunction[] hashes = {
            ((x, y) -> x * 0x125493 + y * 0x19E373), // 1MS strong, 2A strong, 3MA strong
            ((x, y) -> x * 0xDEED5 + y * 0xBEA57), // 1MS very strong, 2A strong, 3MA strong
            ((x, y) -> (int)((x * 107 + y) * 0xD1B54A32D192ED03L >>> 32)), // 1MS fail 314572/500000, 2A fail 314572/500000, 3MA fail 314572/500000
            ((x, y) -> (x >= y ? x * (x + 8) - y + 12 : y * (y + 6) + x + 12)), // 1MS fail 314572/500000, 2A fail 39321/500000, 3MA fail 314572/500000
            ((x, y) -> {int n = (x >= y ? x * (x + 8) - y + 12 : y * (y + 6) + x + 12); return n ^ n >>> 1;}), // 1MS fail 314572/500000, 2A fail 39321/500000, 3MA fail 314572/500000
            ((x, y) -> {int n = (x >= y ? x * (x + 8) - y + 12 : y * (y + 6) + x + 12); return ((n ^ n >>> 1) * 0x9E373 ^ 0xD1B54A35) * 0x125493 ^ 0x91E10DA5;}), // 1MS fail 314572/500000, 2A fail 314572/500000, 3MA fail 314572/500000
            ((x, y) -> y + ((x + y + 6) * (x + y + 7) >>> 1)), // 1MS fail 314572/500000, 2A fail 39321/500000, 3MA fail 314572/500000
            ((x, y) -> {int n = (y + ((x + y + 6) * (x + y + 7) >> 1)); return n ^ n >>> 1;}), // 1MS fail 314572/500000, 2A fail 19660/500000, 3MA fail 314572/500000
            ((x, y) -> {int n = (y + ((x + y + 6) * (x + y + 7) >> 1)); return ((n ^ n >>> 1) * 0x9E373 ^ 0xD1B54A35) * 0x125493 ^ 0x91E10DA5;}), // 1MS fail 314572/500000, 2A fail 314572/500000, 3MA fail 314572/500000
            ((x, y) -> y + ((x + y) * (x + y + 1) + 36 >>> 1)), // 1MS fail 314572/500000, 2A 39321/500000, 3MA fail fail 314572/500000
            ((x, y) -> {
                x |= y << 16;
                x =    ((x & 0x0000ff00) << 8) | ((x >>> 8) & 0x0000ff00) | (x & 0xff0000ff);
                x =    ((x & 0x00f000f0) << 4) | ((x >>> 4) & 0x00f000f0) | (x & 0xf00ff00f);
                x =    ((x & 0x0c0c0c0c) << 2) | ((x >>> 2) & 0x0c0c0c0c) | (x & 0xc3c3c3c3);
                return ((x & 0x22222222) << 1) | ((x >>> 1) & 0x22222222) | (x & 0x99999999);
            }), // 1MS fail 39321/500000, 2A fail 4915/500000, 3MA fail 19660/500000
            ((x, y) -> (x ^ (y << 16 | y >>> 16))), // 1MS strong, 2A fail 2457/500000, 3MA fail 39321/500000
            ((x, y) -> (x + (y << 16 | y >>> 16))), // 1MS very strong, 2A fail 2457/500000, 3MA fail 39321/500000
        };
        int index = 0;
        for(IntIntToIntBiFunction op : hashes) {
            System.out.println("Working with hash " + index++ + ":");
            final IntIntToIntBiFunction hash = op;
            long start = System.nanoTime();
            ObjectSet set = new ObjectSet(51, LOAD) {
                long collisionTotal = 0;
                int longestPileup = 0, allPileups = 0, pileupChecks = 0;
                double averagePileup = 0;

                int hashMul = (int)(hashMultiplier & 0x1FFFFFL);

                @Override
                protected int place (@NonNull Object item) {
                    final Point2 p = (Point2)item;
                    return (int)(hash.applyAsInt(p.x, p.y) * hashMultiplier >>> shift); // option 1MS
//                    return hash.applyAsInt(p.x, p.y) & mask; // option 2A
//                    return hash.applyAsInt(p.x, p.y) * hashMul & mask; // option 3MA
                }

                @Override
                protected void addResize (@NonNull Object key) {
                    Object[] keyTable = this.keyTable;
                    for (int i = place(key), p = 0; ; i = i + 1 & mask) {
                        if (keyTable[i] == null) {
                            keyTable[i] = key;
                            averagePileup += p;

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
                    shift = BitConversion.countLeadingZeros(mask) + 32;

                    // was using this in many tests
                    // total 1788695, longest 33, average 5.686122731838816, sum 160
                    hashMultiplier *= size + size ^ 0xF1357AEA2E62A9C5L;

                    hashMul = (int)(hashMultiplier & 0x1FFFFFL);

                    Object[] oldKeyTable = keyTable;

                    keyTable = new Object[newSize];

                    allPileups += longestPileup;
                    pileupChecks++;
                    collisionTotal = 0;
                    longestPileup = 0;
                    averagePileup = 0.0;

                    if (size > 0) {
                        for (int i = 0; i < oldCapacity; i++) {
                            Object key = oldKeyTable[i];
                            if (key != null) {addResize(key);}
                        }
                    }
                    if(collisionTotal > 150000) throw new IllegalStateException("UH OH");

//                    System.out.println("hash multiplier: " + Base.BASE16.unsigned(hashMultiplier) + " with new size " + newSize);
//                    System.out.println("total collisions: " + collisionTotal);
//                    System.out.println("longest pileup: " + longestPileup);
//                    System.out.println("average pileup: " + (averagePileup / size));
                }

                @Override
                public void clear () {
//                    System.out.println("hash multiplier: " + Base.BASE16.unsigned(hashMultiplier) + " with final size " + size);
                    System.out.println("total collisions: " + collisionTotal + ", longest pileup: " + longestPileup);
                    System.out.println("total of " + pileupChecks + " pileups: " + (allPileups + longestPileup));
                    super.clear();
                }
            };
            try {
                for (int i = 0; i < LEN; i++) {
                    set.add(shells[i]);
                }
                System.out.println("strong");
            } catch (IllegalStateException ex) {
                System.out.println("Way too many collisions!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                System.out.println("fail " + set.size() + "/" + LEN);
            }
            System.out.println((System.nanoTime() - start) + " ns");
            set.clear();
        }
    }

    @Test
    public void testPointSetScatter () {
        final Point2[] shells = generatePointScatter(LEN);
        IntIntToIntBiFunction[] hashes = {
            ((x, y) -> x * 0x125493 + y * 0x19E373), // 1MS strong
            ((x, y) -> x * 0xDEED5 + y * 0xBEA57), // 1MS strong
            ((x, y) -> (int)((x * 107 + y) * 0xD1B54A32D192ED03L >>> 32)), // 1MS weak pass
            ((x, y) -> (x >= y ? x * (x + 8) - y + 12 : y * (y + 6) + x + 12)), // 1MS moderate
            ((x, y) -> {int n = (x >= y ? x * (x + 8) - y + 12 : y * (y + 6) + x + 12); return n ^ n >>> 1;}), // 1MS moderate
            ((x, y) -> {int n = (x >= y ? x * (x + 8) - y + 12 : y * (y + 6) + x + 12); return ((n ^ n >>> 1) * 0x9E373 ^ 0xD1B54A35) * 0x125493 ^ 0x91E10DA5;}), // 1MS moderate
            ((x, y) -> y + ((x + y + 6) * (x + y + 7) >>> 1)), // 1MS moderate
            ((x, y) -> {int n = (y + ((x + y + 6) * (x + y + 7) >> 1)); return n ^ n >>> 1;}), // 1MS moderate
            ((x, y) -> {int n = (y + ((x + y + 6) * (x + y + 7) >> 1)); return ((n ^ n >>> 1) * 0x9E373 ^ 0xD1B54A35) * 0x125493 ^ 0x91E10DA5;}), // 1MS moderate
            ((x, y) -> y + ((x + y) * (x + y + 1) + 36 >>> 1)), // 1MS moderate
            ((x, y) -> {
                x |= y << 16;
                x =    ((x & 0x0000ff00) << 8) | ((x >>> 8) & 0x0000ff00) | (x & 0xff0000ff);
                x =    ((x & 0x00f000f0) << 4) | ((x >>> 4) & 0x00f000f0) | (x & 0xf00ff00f);
                x =    ((x & 0x0c0c0c0c) << 2) | ((x >>> 2) & 0x0c0c0c0c) | (x & 0xc3c3c3c3);
                return ((x & 0x22222222) << 1) | ((x >>> 1) & 0x22222222) | (x & 0x99999999);
            }), // 1MS fail 655356/500000
            ((x, y) -> (x ^ (y << 16 | y >>> 16))), // 1MS strong
            ((x, y) -> (x + (y << 16 | y >>> 16))), // 1MS strong
        };
        int index = 0;
        for(IntIntToIntBiFunction op : hashes) {
            System.out.println("Working with hash " + index++ + ":");
            final IntIntToIntBiFunction hash = op;
            long start = System.nanoTime();
            ObjectSet set = new ObjectSet(51, LOAD) {
                long collisionTotal = 0;
                int longestPileup = 0, allPileups = 0, pileupChecks = 0;
                double averagePileup = 0;

                int hashMul = (int)(hashMultiplier & 0x1FFFFFL);

                @Override
                protected int place (@NonNull Object item) {
                    final Point2 p = (Point2)item;
                    return (int)(hash.applyAsInt(p.x, p.y) * hashMultiplier >>> shift); // option 1MS
//                    return hash.applyAsInt(p.x, p.y) & mask; // option 2A
//                    return hash.applyAsInt(p.x, p.y) * hashMul & mask; // option 3MA
                }

                @Override
                protected void addResize (@NonNull Object key) {
                    Object[] keyTable = this.keyTable;
                    for (int i = place(key), p = 0; ; i = i + 1 & mask) {
                        if (keyTable[i] == null) {
                            keyTable[i] = key;
                            averagePileup += p;

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
                    shift = BitConversion.countLeadingZeros(mask) + 32;

                    // was using this in many tests
                    // total 1788695, longest 33, average 5.686122731838816, sum 160
//                    hashMultiplier *= size + size ^ 0xF1357AEA2E62A9C5L;

                    hashMultiplier = Utilities.GOOD_MULTIPLIERS[(int)(hashMultiplier >>> 48 + shift) & 511];

                    hashMul = (int)(hashMultiplier & 0x1FFFFFL);

                    Object[] oldKeyTable = keyTable;

                    keyTable = new Object[newSize];

                    allPileups += longestPileup;
                    pileupChecks++;
                    collisionTotal = 0;
                    longestPileup = 0;
                    averagePileup = 0.0;

                    if (size > 0) {
                        for (int i = 0; i < oldCapacity; i++) {
                            Object key = oldKeyTable[i];
                            if (key != null) {addResize(key);}
                        }
                    }
                    if(collisionTotal > 150000) throw new IllegalStateException("UH OH");

//                    System.out.println("hash multiplier: " + Base.BASE16.unsigned(hashMultiplier) + " with new size " + newSize);
//                    System.out.println("total collisions: " + collisionTotal);
//                    System.out.println("longest pileup: " + longestPileup);
//                    System.out.println("average pileup: " + (averagePileup / size));
                }

                @Override
                public void clear () {
//                    System.out.println("hash multiplier: " + Base.BASE16.unsigned(hashMultiplier) + " with final size " + size);
                    System.out.println("total collisions: " + collisionTotal + ", longest pileup: " + longestPileup);
                    System.out.println("total of " + pileupChecks + " pileups: " + (allPileups + longestPileup));
                    super.clear();
                }
            };
            try {
                for (int i = 0; i < LEN; i++) {
                    set.add(shells[i]);
                }
                System.out.println("strong");
            } catch (IllegalStateException ex) {
                System.out.println("Way too many collisions!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                System.out.println("fail " + set.size() + "/" + LEN);
            }
            System.out.println((System.nanoTime() - start) + " ns");
            set.clear();
        }
    }

    @Test
    public void testPointSetRectangles () {
        final int[] widths = {500, 200, 100, 50};
        final int[] heights = {LEN / 500, LEN / 200, LEN / 100, LEN / 50};
        final IntIntToIntBiFunction[] hashes = {
            ((x, y) -> x * 0x125493 + y * 0x19E373), // hash 0, 1MS , 2A , 3MA
            ((x, y) -> x * 0xDEED5 + y * 0xBEA57), // hash 1, 1MS , 2A , 3MA
            ((x, y) -> (int)((x * 107 + y) * 0xD1B54A32D192ED03L >>> 32)), // hash 2, 1MS , 2A , 3MA
            ((x, y) -> (x >= y ? x * (x + 8) - y + 12 : y * (y + 6) + x + 12)), // hash 3, 1MS , 2A , 3MA
            ((x, y) -> {
                int n = (x >= y ? x * (x + 8) - y + 12 : y * (y + 6) + x + 12);
                return n ^ n >>> 1;
            }), // hash 4, 1MS , 2A , 3MA
            ((x, y) -> {
                int n = (x >= y ? x * (x + 8) - y + 12 : y * (y + 6) + x + 12);
                return ((n ^ n >>> 1) * 0x9E373 ^ 0xD1B54A35) * 0x125493 ^ 0x91E10DA5;
            }), // hash 5, 1MS , 2A , 3MA
            ((x, y) -> y + ((x + y + 6) * (x + y + 7) >>> 1)), // hash 6, 1MS , 2A , 3MA
            ((x, y) -> {
                int n = (y + ((x + y + 6) * (x + y + 7) >> 1));
                return n ^ n >>> 1;
            }), // hash 7, 1MS , 2A , 3MA
            ((x, y) -> {
                int n = (y + ((x + y + 6) * (x + y + 7) >> 1));
                return ((n ^ n >>> 1) * 0x9E373 ^ 0xD1B54A35) * 0x125493 ^ 0x91E10DA5;
            }), // hash 8, 1MS , 2A , 3MA
            ((x, y) -> y + ((x + y) * (x + y + 1) + 36 >>> 1)), // hash 9, 1MS , 2A , 3MA
            ((x, y) -> {
                x |= y << 16;
                x = ((x & 0x0000ff00) << 8) | ((x >>> 8) & 0x0000ff00) | (x & 0xff0000ff);
                x = ((x & 0x00f000f0) << 4) | ((x >>> 4) & 0x00f000f0) | (x & 0xf00ff00f);
                x = ((x & 0x0c0c0c0c) << 2) | ((x >>> 2) & 0x0c0c0c0c) | (x & 0xc3c3c3c3);
                return ((x & 0x22222222) << 1) | ((x >>> 1) & 0x22222222) | (x & 0x99999999);
            }), // hash 10, 1MS , 2A , 3MA
            ((x, y) -> (x ^ (y << 16 | y >>> 16))), // hash 11, 1MS , 2A , 3MA
            ((x, y) -> (x + (y << 16 | y >>> 16))), // hash 12, 1MS , 2A , 3MA
        };

        /*
         width 500
         hash 0, 1MS strong, 2A strong, 3MA strong
         hash 1, 1MS strong, 2A strong, 3MA strong
         hash 2, 1MS fail 314572/500000, 2A fail 314572/500000, 3MA fail 314572/500000
         hash 3, 1MS strong, 2A strong, 3MA strong
         hash 4, 1MS strong, 2A strong, 3MA strong
         hash 5, 1MS strong, 2A strong, 3MA strong
         hash 6, 1MS strong, 2A strong, 3MA strong
         hash 7, 1MS strong, 2A strong, 3MA strong
         hash 8, 1MS strong, 2A strong, 3MA strong
         hash 9, 1MS strong, 2A strong, 3MA strong
         hash 10, 1MS strong, 2A strong, 3MA strong
         hash 11, 1MS strong, 2A fail 2457/500000, 3MA fail 19660/500000
         hash 12, 1MS strong, 2A fail 2457/500000, 3MA fail 19660/500000
         */

        /*
         width 200
         hash 0, 1MS strong, 2A strong, 3MA strong
         hash 1, 1MS strong, 2A strong, 3MA strong
         hash 2, 1MS fail 78643/500000, 2A fail 157286/500000, 3MA fail 157286/500000
         hash 3, 1MS strong, 2A fail 314572/500000, 3MA strong
         hash 4, 1MS strong, 2A strong, 3MA strong
         hash 5, 1MS strong, 2A strong, 3MA strong
         hash 6, 1MS strong, 2A strong, 3MA strong
         hash 7, 1MS strong, 2A strong, 3MA strong
         hash 8, 1MS strong, 2A strong, 3MA strong
         hash 9, 1MS strong, 2A strong, 3MA strong
         hash 10, 1MS strong, 2A fail 157286/500000, 3MA strong
         hash 11, 1MS strong, 2A fail 1228/500000, 3MA fail 19660/500000
         hash 12, 1MS strong, 2A fail 1228/500000, 3MA fail 19660/500000
         */

        /*
         width 100
         hash 0, 1MS strong, 2A fail 314572/500000, 3MA strong
         hash 1, 1MS strong, 2A strong, 3MA strong
         hash 2, 1MS fail 78643/500000, 2A fail 157286/500000, 3MA fail 157286/500000
         hash 3, 1MS strong, 2A fail 314572/500000, 3MA strong
         hash 4, 1MS strong, 2A fail 314572/500000, 3MA strong
         hash 5, 1MS strong, 2A strong, 3MA strong
         hash 6, 1MS strong, 2A fail 314572/500000, 3MA strong
         hash 7, 1MS strong, 2A fail 314572/500000, 3MA strong
         hash 8, 1MS strong, 2A strong, 3MA strong
         hash 9, 1MS strong, 2A fail 314572/500000, 3MA strong
         hash 10, 1MS strong, 2A fail 157286/500000, 3MA fail 314572/500000
         hash 11, 1MS strong, 2A fail 1228/500000, 3MA fail 9830/500000
         hash 12, 1MS strong, 2A fail 1228/500000, 3MA fail 9830/500000
         */

        /*
         width 50
         hash 0, 1MS strong, 2A strong, 3MA
         hash 1, 1MS strong, 2A strong, 3MA
         hash 2, 1MS fail 78643/500000, 2A fail 157286/500000, 3MA
         hash 3, 1MS strong, 2A fail 157286/500000, 3MA
         hash 4, 1MS strong, 2A fail 314572/500000, 3MA
         hash 5, 1MS strong, 2A strong, 3MA
         hash 6, 1MS strong, 2A fail 314572/500000, 3MA
         hash 7, 1MS strong, 2A fail 314572/500000, 3MA
         hash 8, 1MS strong, 2A strong, 3MA
         hash 9, 1MS strong, 2A fail 314572/500000, 3MA
         hash 10, 1MS strong, 2A fail 39321/500000, 3MA
         hash 11, 1MS strong, 2A fail 614/500000, 3MA
         hash 12, 1MS strong, 2A fail 614/500000, 3MA
         */


        for (int idx = 0; idx < widths.length; idx++) {
            final int wide = widths[idx], high = heights[idx];
            final Point2[] shells = generatePointRectangle(wide, high);
            int index = 0;
            for (IntIntToIntBiFunction op : hashes) {
                System.out.println("Working with hash " + index++ + ":");
                final IntIntToIntBiFunction hash = op;
                long start = System.nanoTime();
                ObjectSet set = new ObjectSet(51, LOAD) {
                    long collisionTotal = 0;
                    int longestPileup = 0, allPileups = 0, pileupChecks = 0;
                    double averagePileup = 0;

                    int hashMul = (int)(hashMultiplier & 0x1FFFFFL);

                    @Override
                    protected int place (@NonNull Object item) {
                        final Point2 p = (Point2)item;
//                    return (int)(hash.applyAsInt(p.x, p.y) * hashMultiplier >>> shift); // option 1MS
//                        return hash.applyAsInt(p.x, p.y) & mask; // option 2A
                    return hash.applyAsInt(p.x, p.y) * hashMul & mask; // option 3MA
                    }

                    @Override
                    protected void addResize (@NonNull Object key) {
                        Object[] keyTable = this.keyTable;
                        for (int i = place(key), p = 0; ; i = i + 1 & mask) {
                            if (keyTable[i] == null) {
                                keyTable[i] = key;
                                averagePileup += p;

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
                        shift = BitConversion.countLeadingZeros(mask) + 32;

                        // was using this in many tests
                        // total 1788695, longest 33, average 5.686122731838816, sum 160
                        hashMultiplier *= size + size ^ 0xF1357AEA2E62A9C5L;

                        hashMul = (int)(hashMultiplier & 0x1FFFFFL);

                        Object[] oldKeyTable = keyTable;

                        keyTable = new Object[newSize];

                        allPileups += longestPileup;
                        pileupChecks++;
                        collisionTotal = 0;
                        longestPileup = 0;
                        averagePileup = 0.0;

                        if (size > 0) {
                            for (int i = 0; i < oldCapacity; i++) {
                                Object key = oldKeyTable[i];
                                if (key != null) {addResize(key);}
                            }
                        }
                        if (collisionTotal > 150000)
                            throw new IllegalStateException("UH OH");

//                    System.out.println("hash multiplier: " + Base.BASE16.unsigned(hashMultiplier) + " with new size " + newSize);
//                    System.out.println("total collisions: " + collisionTotal);
//                    System.out.println("longest pileup: " + longestPileup);
//                    System.out.println("average pileup: " + (averagePileup / size));
                    }

                    @Override
                    public void clear () {
//                    System.out.println("hash multiplier: " + Base.BASE16.unsigned(hashMultiplier) + " with final size " + size);
                        System.out.println("total collisions: " + collisionTotal + ", longest pileup: " + longestPileup);
                        System.out.println("total of " + pileupChecks + " pileups: " + (allPileups + longestPileup));
                        super.clear();
                    }
                };
                try {
                    for (int i = 0; i < LEN; i++) {
                        set.add(shells[i]);
                    }
                    System.out.println("strong");
                } catch (IllegalStateException ex) {
                    System.out.println("Way too many collisions!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                    System.out.println("fail " + set.size() + "/" + LEN);
                }
                System.out.println((System.nanoTime() - start) + " ns");
                set.clear();
            }
        }
    }



    @Test
    public void testPointSetShellsFull () {
        final Point2[] shells = generatePointShells(LEN, 0);
        IntIntToIntBiFunction[] hashes = {
            // 1MS defaults to strong unless it failed or did very well
            // 2A almost all have no collisions; only 2, 11, 12, 13 and 14 have any (and all fail except 14)
            // 3MA almost all have no collisions; only 2, 11, 12, and 13 have any (and all fail)
            ((x, y) -> x * 0x125493 + y * 0x19E373), // hash 0 1MS , 2A , 3MA
            ((x, y) -> x * 0xDEED5 + y * 0xBEA57), // hash 1 1MS absurdly strong, 2A , 3MA
            ((x, y) -> 31 * x + y), // hash 2 1MS fail 157286/500000, 2A fail 39321/500000, 3MA fail 157286/500000
            ((x, y) -> (x >= y ? x * (x + 8) - y + 12 : y * (y + 6) + x + 12)), // hash 3 1MS , 2A seems unusually fast, 3MA
            ((x, y) -> {int n = (x >= y ? x * (x + 8) - y + 12 : y * (y + 6) + x + 12); return n ^ n >>> 1;}), // hash 4 1MS , 2A , 3MA
            ((x, y) -> {int n = (x >= y ? x * (x + 8) - y + 12 : y * (y + 6) + x + 12); return ((n ^ n >>> 1) * 0x9E373 ^ 0xD1B54A35) * 0x125493 ^ 0x91E10DA5;}), // hash 5 1MS , 2A , 3MA
            ((x, y) -> y + ((x + y + 6) * (x + y + 7) >>> 1)), // hash 6 1MS , 2A , 3MA seems fast
            ((x, y) -> {int n = (y + ((x + y + 6) * (x + y + 7) >> 1)); return n ^ n >>> 1;}), // hash 7 1MS , 2A , 3MA
            ((x, y) -> {int n = (y + ((x + y + 6) * (x + y + 7) >> 1)); return ((n ^ n >>> 1) * 0x9E373 ^ 0xD1B54A35) * 0x125493 ^ 0x91E10DA5;}), // hash 8 1MS , 2A , 3MA seems fast
            ((x, y) -> y + ((x + y) * (x + y + 1) + 36 >>> 1)), // hash 9 1MS , 2A , 3MA
            ((x, y) -> {
                x |= y << 16;
                x =    ((x & 0x0000ff00) << 8) | ((x >>> 8) & 0x0000ff00) | (x & 0xff0000ff);
                x =    ((x & 0x00f000f0) << 4) | ((x >>> 4) & 0x00f000f0) | (x & 0xf00ff00f);
                x =    ((x & 0x0c0c0c0c) << 2) | ((x >>> 2) & 0x0c0c0c0c) | (x & 0xc3c3c3c3);
                return ((x & 0x22222222) << 1) | ((x >>> 1) & 0x22222222) | (x & 0x99999999);
            }), // hash 10 1MS , 2A , 3MA
            ((x, y) -> (x ^ (y << 16 | y >>> 16))), // hash 11 1MS , 2A fail 1228/500000, 3MA fail 19660/500000
            ((x, y) -> (x + (y << 16 | y >>> 16))), // hash 12 1MS , 2A fail 1228/500000, 3MA fail 19660/500000
            ((x, y) -> x ^ y ^ (BitConversion.imul(y, y) | 1)), // hash 13 1MS fail 314572/500000, 2A fail 39321/500000, 3MA fail 314572/500000
            ((x, y) -> BitConversion.imul(x, 0xC13FA9A9) + BitConversion.imul(0x91E10DA5, y)), // hash 14 1MS , 2A , 3MA fail 314572/500000
            ((x, y) -> x * 0xC13F + y * 0x91E1), // hash 15 1MS , 2A , 3MA
            ((x, y) -> x * 0x7587 + y * 0x6A89), // hash 16 1MS , 2A , 3MA
        };
        int index = 0;
        for(IntIntToIntBiFunction op : hashes) {
            System.out.println("Working with hash " + index++ + ":");
            final IntIntToIntBiFunction hash = op;
            long start = System.nanoTime();
            ObjectSet set = new ObjectSet(51, LOAD) {
                long collisionTotal = 0;
                int longestPileup = 0, allPileups = 0, pileupChecks = 0;
                double averagePileup = 0;

                int hashMul = (int)(hashMultiplier & 0x1FFFFFL);

                @Override
                protected int place (@NonNull Object item) {
                    final Point2 p = (Point2)item;
//                    return (int)(hash.applyAsInt(p.x, p.y) * hashMultiplier >>> shift);
                    return hash.applyAsInt(p.x, p.y) & mask;
//                    return hash.applyAsInt(p.x, p.y) * hashMul & mask;
                }

                @Override
                protected void addResize (@NonNull Object key) {
                    Object[] keyTable = this.keyTable;
                    for (int i = place(key), p = 0; ; i = i + 1 & mask) {
                        if (keyTable[i] == null) {
                            keyTable[i] = key;
                            averagePileup += p;

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
                    shift = BitConversion.countLeadingZeros(mask) + 32;

                    hashMultiplier *= size + size ^ 0xF1357AEA2E62A9C5L;
                    hashMul = (int)(hashMultiplier & 0x1FFFFFL);

                    Object[] oldKeyTable = keyTable;

                    keyTable = new Object[newSize];

                    allPileups += longestPileup;
                    pileupChecks++;
                    collisionTotal = 0;
                    longestPileup = 0;
                    averagePileup = 0.0;

                    if (size > 0) {
                        for (int i = 0; i < oldCapacity; i++) {
                            Object key = oldKeyTable[i];
                            if (key != null) {addResize(key);}
                        }
                    }
                    if(collisionTotal > 150000) throw new IllegalStateException("UH OH");
//                    System.out.println("hash multiplier: " + Base.BASE16.unsigned(hashMultiplier) + " with new size " + newSize);
//                    System.out.println("total collisions: " + collisionTotal);
//                    System.out.println("longest pileup: " + longestPileup);
//                    System.out.println("average pileup: " + (averagePileup / size));
                }

                @Override
                public void clear () {
//                    System.out.println("hash multiplier: " + Base.BASE16.unsigned(hashMultiplier) + " with final size " + size);
                    System.out.println("total collisions: " + collisionTotal + ", longest pileup: " + longestPileup);
                    System.out.println("total of " + pileupChecks + " pileups: " + (allPileups + longestPileup));
                    super.clear();
                }
            };
//        final int limit = (int)(Math.sqrt(LEN));
//        for (int x = -limit; x < limit; x+=2) {
//            for (int y = -limit; y < limit; y+=2) {
//                set.add(new Vector2(x, y));
//            }
//        }
            try {
                for (int i = 0; i < LEN; i++) {
                    set.add(shells[i]);
                }
                System.out.println("strong");
            } catch (IllegalStateException ex) {
                System.out.println("Way too many collisions!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                System.out.println("fail " + set.size() + "/" + LEN);
            }
            System.out.println((System.nanoTime() - start) + " ns");
            set.clear();
        }
    }

    @Test
    public void testPointSetAnglesFull () {
        final Point2[] shells = generatePointAngles(LEN, 0);
        IntIntToIntBiFunction[] hashes = {
            // the old hash 2 was: (int)((x * 107 + y) * 0xD1B54A32D192ED03L >>> 32)
            // it had a ton of collisions. The new hash 2 is what a typical auto-generated hashCode would use.
            // it is also horrible in some tables.

            // 1MS defaults to strong unless it failed or did very well. Only Cantor-based hashes fail here, and all do.
            // 2A only hash 10, and all Cantor-based hashes (6, 7, 8, and 9) fail
            // 3MA only hash 10, and all Cantor-based hashes (6, 7, 8, and 9) fail
            ((x, y) -> x * 0x125493 + y * 0x19E373), // hash 0 1MS , 2A , 3MA
            ((x, y) -> x * 0xDEED5 + y * 0xBEA57), // hash 1 1MS , 2A , 3MA
            ((x, y) -> 31 * x + y), // hash 2 1MS , 2A , 3MA
            ((x, y) -> (x >= y ? x * (x + 8) - y + 12 : y * (y + 6) + x + 12)), // hash 3 1MS , 2A , 3MA
            ((x, y) -> {int n = (x >= y ? x * (x + 8) - y + 12 : y * (y + 6) + x + 12); return n ^ n >>> 1;}), // hash 4 1MS , 2A , 3MA
            ((x, y) -> {int n = (x >= y ? x * (x + 8) - y + 12 : y * (y + 6) + x + 12); return ((n ^ n >>> 1) * 0x9E373 ^ 0xD1B54A35) * 0x125493 ^ 0x91E10DA5;}), // hash 5 1MS , 2A , 3MA
            ((x, y) -> y + ((x + y + 6) * (x + y + 7) >>> 1)), // hash 6 1MS fail 78643/500000, 2A fail 4915/500000, 3MA fail 78643/500000
            ((x, y) -> {int n = (y + ((x + y + 6) * (x + y + 7) >> 1)); return n ^ n >>> 1;}), // hash 7 1MS fail 78643/500000, 2A fail 4915/500000, 3MA fail 78643/500000
            ((x, y) -> {int n = (y + ((x + y + 6) * (x + y + 7) >> 1)); return ((n ^ n >>> 1) * 0x9E373 ^ 0xD1B54A35) * 0x125493 ^ 0x91E10DA5;}), // hash 8 1MS fail 39321/500000, 2A fail 39321/500000, 3MA fail 39321/500000
            ((x, y) -> y + ((x + y) * (x + y + 1) + 36 >>> 1)), // hash 9 1MS fail 19660/500000, 2A fail 1228/500000, 3MA fail 19660/500000
            ((x, y) -> {
                x |= y << 16;
                x =    ((x & 0x0000ff00) << 8) | ((x >>> 8) & 0x0000ff00) | (x & 0xff0000ff);
                x =    ((x & 0x00f000f0) << 4) | ((x >>> 4) & 0x00f000f0) | (x & 0xf00ff00f);
                x =    ((x & 0x0c0c0c0c) << 2) | ((x >>> 2) & 0x0c0c0c0c) | (x & 0xc3c3c3c3);
                return ((x & 0x22222222) << 1) | ((x >>> 1) & 0x22222222) | (x & 0x99999999);
            }), // hash 10 1MS , 2A fail 314572/500000, 3MA fail 314572/500000
            ((x, y) -> (x ^ (y << 16 | y >>> 16))), // hash 11 1MS , 2A , 3MA
            ((x, y) -> (x + (y << 16 | y >>> 16))), // hash 12 1MS , 2A , 3MA
            ((x, y) -> x ^ y ^ (BitConversion.imul(y, y) | 1)), // hash 13 1MS , 2A , 3MA
            ((x, y) -> BitConversion.imul(x, 0xC13FA9A9) + BitConversion.imul(0x91E10DA5, y)), // hash 14 1MS , 2A , 3MA
            ((x, y) -> x * 0xC13F + y * 0x91E1), // hash 15 1MS , 2A , 3MA
            ((x, y) -> x * 0x7587 + y * 0x6A89), // hash 16 1MS , 2A , 3MA
        };
        int index = 0;
        for(IntIntToIntBiFunction op : hashes) {
            System.out.println("Working with hash " + index++ + ":");
            final IntIntToIntBiFunction hash = op;
            long start = System.nanoTime();
            ObjectSet set = new ObjectSet(51, LOAD) {
                long collisionTotal = 0;
                int longestPileup = 0, allPileups = 0, pileupChecks = 0;
                double averagePileup = 0;

                int hashMul = (int)(hashMultiplier & 0x1FFFFFL);

                @Override
                protected int place (@NonNull Object item) {
                    final Point2 p = (Point2)item;
//                    return (int)(hash.applyAsInt(p.x, p.y) * hashMultiplier >>> shift); // option 1MS
                    return hash.applyAsInt(p.x, p.y) & mask; // option 2A
//                    return hash.applyAsInt(p.x, p.y) * hashMul & mask; // option 3MA
                }

                @Override
                protected void addResize (@NonNull Object key) {
                    Object[] keyTable = this.keyTable;
                    for (int i = place(key), p = 0; ; i = i + 1 & mask) {
                        if (keyTable[i] == null) {
                            keyTable[i] = key;
                            averagePileup += p;

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
                    shift = BitConversion.countLeadingZeros(mask) + 32;

                    // was using this in many tests
                    // total 1788695, longest 33, average 5.686122731838816, sum 160
                    hashMultiplier *= size + size ^ 0xF1357AEA2E62A9C5L;

                    hashMul = (int)(hashMultiplier & 0x1FFFFFL);

                    Object[] oldKeyTable = keyTable;

                    keyTable = new Object[newSize];

                    allPileups += longestPileup;
                    pileupChecks++;
                    collisionTotal = 0;
                    longestPileup = 0;
                    averagePileup = 0.0;

                    if (size > 0) {
                        for (int i = 0; i < oldCapacity; i++) {
                            Object key = oldKeyTable[i];
                            if (key != null) {addResize(key);}
                        }
                    }
                    if(collisionTotal > 150000) throw new IllegalStateException("UH OH");

//                    System.out.println("hash multiplier: " + Base.BASE16.unsigned(hashMultiplier) + " with new size " + newSize);
//                    System.out.println("total collisions: " + collisionTotal);
//                    System.out.println("longest pileup: " + longestPileup);
//                    System.out.println("average pileup: " + (averagePileup / size));
                }

                @Override
                public void clear () {
//                    System.out.println("hash multiplier: " + Base.BASE16.unsigned(hashMultiplier) + " with final size " + size);
                    System.out.println("total collisions: " + collisionTotal + ", longest pileup: " + longestPileup);
                    System.out.println("total of " + pileupChecks + " pileups: " + (allPileups + longestPileup));
                    super.clear();
                }
            };
            try {
                for (int i = 0; i < LEN; i++) {
                    set.add(shells[i]);
                }
                System.out.println("strong");
            }catch (IllegalStateException ex) {
                System.out.println("Way too many collisions!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                System.out.println("fail " + set.size() + "/" + LEN);
            }
            System.out.println((System.nanoTime() - start) + " ns");
            set.clear();
        }
    }

    @Test
    public void testPointSetSpiralFull () {
        final Point2[] shells = generatePointSpiral(LEN, 0);
        IntIntToIntBiFunction[] hashes = {
            // 1MS everything fails except 0, 1, 12, 14, and 15. 15, though, gets NO collisions at the end.
            // 2A only 0, 1, 14, and 15 pass. for 0, 1, and 15 with no collisions, even. wowza yow.
            // 3MA 0, 1, and 15 pass with no collisions, everything else fails. mm.
            ((x, y) -> x * 0x125493 + y * 0x19E373), // hash 0 1MS , 2A , 3MA
            ((x, y) -> x * 0xDEED5 + y * 0xBEA57), // hash 1 1MS absurdly good, 2A , 3MA
            ((x, y) -> 31 * x + y), // hash 2 1MS fail 157286/500000, 2A fail 39321/500000, 3MA fail 157286/500000
            ((x, y) -> (x >= y ? x * (x + 8) - y + 12 : y * (y + 6) + x + 12)), // hash 3 1MS fail 157286/500000, 2A fail 19660/500000, 3MA fail 157286/500000
            ((x, y) -> {int n = (x >= y ? x * (x + 8) - y + 12 : y * (y + 6) + x + 12); return n ^ n >>> 1;}), // hash 4 1MS fail 157286/500000, 2A fail 19660/500000, 3MA fail 157286/500000
            ((x, y) -> {int n = (x >= y ? x * (x + 8) - y + 12 : y * (y + 6) + x + 12); return ((n ^ n >>> 1) * 0x9E373 ^ 0xD1B54A35) * 0x125493 ^ 0x91E10DA5;}), // hash 5 1MS fail 157286/500000, 2A fail 157286/500000, 3MA fail 157286/500000
            ((x, y) -> y + ((x + y + 6) * (x + y + 7) >>> 1)), // hash 6 1MS fail 157286/500000, 2A fail 9830/500000, 3MA fail 157286/500000
            ((x, y) -> {int n = (y + ((x + y + 6) * (x + y + 7) >> 1)); return n ^ n >>> 1;}), // hash 7 1MS fail 157286/500000, 2A fail 9830/500000, 3MA fail 157286/500000
            ((x, y) -> {int n = (y + ((x + y + 6) * (x + y + 7) >> 1)); return ((n ^ n >>> 1) * 0x9E373 ^ 0xD1B54A35) * 0x125493 ^ 0x91E10DA5;}), // hash 8 1MS fail 157286/500000, 2A fail 157286/500000, 3MA fail 157286/500000
            ((x, y) -> y + ((x + y) * (x + y + 1) + 36 >>> 1)), // hash 9 1MS fail 157286/500000, 2A fail 9830/500000, 3MA fail 157286/500000
            ((x, y) -> {
                x |= y << 16;
                x =    ((x & 0x0000ff00) << 8) | ((x >>> 8) & 0x0000ff00) | (x & 0xff0000ff);
                x =    ((x & 0x00f000f0) << 4) | ((x >>> 4) & 0x00f000f0) | (x & 0xf00ff00f);
                x =    ((x & 0x0c0c0c0c) << 2) | ((x >>> 2) & 0x0c0c0c0c) | (x & 0xc3c3c3c3);
                return ((x & 0x22222222) << 1) | ((x >>> 1) & 0x22222222) | (x & 0x99999999);
            }), // hash 10 1MS fail 19660/500000, 2A fail 4915/500000, 3MA fail 19660/500000
            ((x, y) -> (x ^ (y << 16 | y >>> 16))), // hash 11 1MS fail 314572/500000, 2A fail 1228/500000, 3MA fail 19660/500000
            ((x, y) -> (x + (y << 16 | y >>> 16))), // hash 12 1MS , 2A fail 1228/500000, 3MA fail 19660/500000
            ((x, y) -> x ^ y ^ (BitConversion.imul(y, y) | 1)), // hash 13 1MS fail 314572/500000, 2A fail 19660/500000, 3MA fail 314572/500000
            ((x, y) -> BitConversion.imul(x, 0xC13FA9A9) + BitConversion.imul(0x91E10DA5, y)), // hash 14 1MS , 2A , 3MA fail 314572/500000
            ((x, y) -> x * 0xC13F + y * 0x91E1), // hash 15 1MS NO COLLISIONS, 2A NO COLLISIONS, 3MA NO COLLISIONS
            ((x, y) -> x * 0x7587 + y * 0x6A89), // hash 16 1MS , 2A , 3MA
        };
        int index = 0;
        for(IntIntToIntBiFunction op : hashes) {
            System.out.println("Working with hash " + index++ + ":");
            final IntIntToIntBiFunction hash = op;
            long start = System.nanoTime();
            ObjectSet set = new ObjectSet(51, LOAD) {
                long collisionTotal = 0;
                int longestPileup = 0, allPileups = 0, pileupChecks = 0;
                double averagePileup = 0;

                int hashMul = (int)(hashMultiplier & 0x1FFFFFL);

                @Override
                protected int place (@NonNull Object item) {
                    final Point2 p = (Point2)item;
//                    return (int)(hash.applyAsInt(p.x, p.y) * hashMultiplier >>> shift); // option 1MS
                    return hash.applyAsInt(p.x, p.y) & mask; // option 2A
//                    return hash.applyAsInt(p.x, p.y) * hashMul & mask; // option 3MA
                }

                @Override
                protected void addResize (@NonNull Object key) {
                    Object[] keyTable = this.keyTable;
                    for (int i = place(key), p = 0; ; i = i + 1 & mask) {
                        if (keyTable[i] == null) {
                            keyTable[i] = key;
                            averagePileup += p;

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
                    shift = BitConversion.countLeadingZeros(mask) + 32;

                    // was using this in many tests
                    // total 1788695, longest 33, average 5.686122731838816, sum 160
                    hashMultiplier *= size + size ^ 0xF1357AEA2E62A9C5L;

                    hashMul = (int)(hashMultiplier & 0x1FFFFFL);

                    Object[] oldKeyTable = keyTable;

                    keyTable = new Object[newSize];

                    allPileups += longestPileup;
                    pileupChecks++;
                    collisionTotal = 0;
                    longestPileup = 0;
                    averagePileup = 0.0;

                    if (size > 0) {
                        for (int i = 0; i < oldCapacity; i++) {
                            Object key = oldKeyTable[i];
                            if (key != null) {addResize(key);}
                        }
                    }
                    if(collisionTotal > 150000) throw new IllegalStateException("UH OH");

//                    System.out.println("hash multiplier: " + Base.BASE16.unsigned(hashMultiplier) + " with new size " + newSize);
//                    System.out.println("total collisions: " + collisionTotal);
//                    System.out.println("longest pileup: " + longestPileup);
//                    System.out.println("average pileup: " + (averagePileup / size));
                }

                @Override
                public void clear () {
//                    System.out.println("hash multiplier: " + Base.BASE16.unsigned(hashMultiplier) + " with final size " + size);
                    System.out.println("total collisions: " + collisionTotal + ", longest pileup: " + longestPileup);
                    System.out.println("total of " + pileupChecks + " pileups: " + (allPileups + longestPileup));
                    super.clear();
                }
            };
            try {
                for (int i = 0; i < LEN; i++) {
                    set.add(shells[i]);
                }
                System.out.println("strong");
            } catch (IllegalStateException ex) {
                System.out.println("Way too many collisions!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                System.out.println("fail " + set.size() + "/" + LEN);
            }
            System.out.println((System.nanoTime() - start) + " ns");
            set.clear();
        }
    }

    @Test
    public void testPointSetRectanglesFull () {
        final int[] widths = {500, 200, 100, 50};
        final int[] heights = {LEN / 500, LEN / 200, LEN / 100, LEN / 50};
        final IntIntToIntBiFunction[] hashes = {
            ((x, y) -> x * 0x125493 + y * 0x19E373), // hash 0 1MS , 2A , 3MA
            ((x, y) -> x * 0xDEED5 + y * 0xBEA57), // hash 1 1MS , 2A , 3MA
            ((x, y) -> 31 * x + y), // hash 2 1MS , 2A , 3MA
            ((x, y) -> (x >= y ? x * (x + 8) - y + 12 : y * (y + 6) + x + 12)), // hash 3 1MS , 2A , 3MA
            ((x, y) -> {int n = (x >= y ? x * (x + 8) - y + 12 : y * (y + 6) + x + 12); return n ^ n >>> 1;}), // hash 4 1MS , 2A , 3MA
            ((x, y) -> {int n = (x >= y ? x * (x + 8) - y + 12 : y * (y + 6) + x + 12); return ((n ^ n >>> 1) * 0x9E373 ^ 0xD1B54A35) * 0x125493 ^ 0x91E10DA5;}), // hash 5 1MS , 2A , 3MA
            ((x, y) -> y + ((x + y + 6) * (x + y + 7) >>> 1)), // hash 6 1MS , 2A , 3MA
            ((x, y) -> {int n = (y + ((x + y + 6) * (x + y + 7) >> 1)); return n ^ n >>> 1;}), // hash 7 1MS , 2A , 3MA
            ((x, y) -> {int n = (y + ((x + y + 6) * (x + y + 7) >> 1)); return ((n ^ n >>> 1) * 0x9E373 ^ 0xD1B54A35) * 0x125493 ^ 0x91E10DA5;}), // hash 8 1MS , 2A , 3MA
            ((x, y) -> y + ((x + y) * (x + y + 1) + 36 >>> 1)), // hash 9 1MS , 2A , 3MA
            ((x, y) -> {
                x |= y << 16;
                x =    ((x & 0x0000ff00) << 8) | ((x >>> 8) & 0x0000ff00) | (x & 0xff0000ff);
                x =    ((x & 0x00f000f0) << 4) | ((x >>> 4) & 0x00f000f0) | (x & 0xf00ff00f);
                x =    ((x & 0x0c0c0c0c) << 2) | ((x >>> 2) & 0x0c0c0c0c) | (x & 0xc3c3c3c3);
                return ((x & 0x22222222) << 1) | ((x >>> 1) & 0x22222222) | (x & 0x99999999);
            }), // hash 10 1MS , 2A , 3MA
            ((x, y) -> (x ^ (y << 16 | y >>> 16))), // hash 11 1MS , 2A , 3MA
            ((x, y) -> (x + (y << 16 | y >>> 16))), // hash 12 1MS , 2A , 3MA
            ((x, y) -> x ^ y ^ (BitConversion.imul(y, y) | 1)), // hash 13 1MS , 2A , 3MA
            ((x, y) -> BitConversion.imul(x, 0xC13FA9A9) + BitConversion.imul(0x91E10DA5, y)), // hash 14 1MS , 2A , 3MA
            ((x, y) -> x * 0xC13F + y * 0x91E1), // hash 15 1MS , 2A , 3MA
            ((x, y) -> x * 0x7587 + y * 0x6A89), // hash 16 1MS , 2A , 3MA
            ((x, y) -> x * 0xC13FA9A9 + y * 0x91E10DA5), // hash 17 1MS , 2A , 3MA
            ((x, y) -> {
                int xs = x >> 31, ys = y >> 31;
                x ^= xs;
                y ^= ys;
                y += ((x + y) * (x + y + 1) >>> 1);
                return BitConversion.imul(y ^ y >>> 1, 0xD1B54A35) ^ (xs & 0x55555555) ^ (ys & 0xAAAAAAAA);
            }), // hash 18 1MS , 2A , 3MA
            ((x, y) -> {
                int xs = x >> 31, ys = y >> 31;
                x ^= xs;
                y ^= ys;
                y = (x >= y) ? (((x & 1) == 0) ? x * (x + 2) - y : x * x + y) : (((y & 1) == 0) ? y * y + x : y * (y + 2) - x);
                return BitConversion.imul(y ^ y >>> 1, 0x9E3779B9) ^ (xs & 0x55555555) ^ (ys & 0xAAAAAAAA);
            }), // hash 19 1MS , 2A , 3MA
            ((x, y) -> x * 0x3D99C097 + y * 0x3E66C06B), // hash 20 1MS , 2A , 3MA
            ((x, y) -> (x = x * 0x3D99C097 + y * 0x3E66C06B) ^ x >>> 2), // hash 21 1MS , 2A , 3MA
            ((x, y) -> (x = x * 0xC13FA9A9 + y * 0x91E10DA5) ^ x >>> 1), // hash 22 1MS , 2A , 3MA
        };

        /*
         width 500
         hash 0, 1MS , 2A , 3MA
         hash 1, 1MS , 2A , 3MA
         hash 2, 1MS fail 78643/500000, 2A fail 19660/500000, 3MA fail 78643/500000
         hash 3, 1MS , 2A , 3MA
         hash 4, 1MS , 2A , 3MA
         hash 5, 1MS , 2A , 3MA
         hash 6, 1MS , 2A , 3MA
         hash 7, 1MS , 2A , 3MA
         hash 8, 1MS , 2A , 3MA
         hash 9, 1MS , 2A , 3MA
         hash 10, 1MS , 2A , 3MA
         hash 11, 1MS , 2A fail 1228/500000, 3MA fail 19660/500000
         hash 12, 1MS , 2A fail 1228/500000, 3MA fail 19660/500000
         hash 13, 1MS fail 314572/500000, 2A fail 39321/500000, 3MA
         hash 14, 1MS , 2A , 3MA
         hash 15, 1MS , 2A , 3MA
         hash 16, 1MS , 2A , 3MA
         */

        /*
         width 200
         hash 0, 1MS , 2A fail 314572/500000, 3MA
         hash 1, 1MS , 2A , 3MA
         hash 2, 1MS fail 78643/500000, 2A fail 9830/500000, 3MA fail 78643/500000
         hash 3, 1MS , 2A fail 157286/500000, 3MA
         hash 4, 1MS , 2A fail 157286/500000, 3MA
         hash 5, 1MS , 2A , 3MA
         hash 6, 1MS , 2A fail 157286/500000, 3MA
         hash 7, 1MS , 2A fail 157286/500000, 3MA
         hash 8, 1MS , 2A , 3MA
         hash 9, 1MS , 2A fail 157286/500000, 3MA
         hash 10, 1MS , 2A fail 157286/500000, 3MA fail 314572/500000
         hash 11, 1MS , 2A fail 1228/500000, 3MA fail 9830/500000
         hash 12, 1MS , 2A fail 1228/500000, 3MA fail 9830/500000
         hash 13, 1MS , 2A fail 78643/500000, 3MA
         hash 14, 1MS , 2A , 3MA
         hash 15, 1MS , 2A , 3MA
         hash 16, 1MS , 2A , 3MA
         */

        /*
         width 100
         hash 0, 1MS , 2A fail 314572/500000, 3MA fail 314572/500000
         hash 1, 1MS , 2A , 3MA
         hash 2, 1MS fail 78643/500000, 2A fail 9830/500000, 3MA fail 78643/500000
         hash 3, 1MS , 2A fail 157286/500000, 3MA
         hash 4, 1MS , 2A fail 157286/500000, 3MA
         hash 5, 1MS , 2A , 3MA
         hash 6, 1MS , 2A fail 157286/500000, 3MA
         hash 7, 1MS , 2A fail 157286/500000, 3MA
         hash 8, 1MS , 2A , 3MA
         hash 9, 1MS , 2A fail 157286/500000, 3MA
         hash 10, 1MS , 2A fail 39321/500000, 3MA fail 157286/500000
         hash 11, 1MS , 2A fail 614/500000, 3MA fail 4915/500000
         hash 12, 1MS , 2A fail 614/500000, 3MA fail 4915/500000
         hash 13, 1MS , 2A fail 157286/500000, 3MA
         hash 14, 1MS , 2A , 3MA
         hash 15, 1MS , 2A , 3MA
         hash 16, 1MS , 2A , 3MA
         */

        /*
         width 50
         hash 0, 1MS , 2A fail 314572/500000, 3MA
         hash 1, 1MS , 2A , 3MA
         hash 2, 1MS fail 78643/500000, 2A fail 9830/500000, 3MA fail 78643/500000
         hash 3, 1MS , 2A fail 157286/500000, 3MA
         hash 4, 1MS , 2A fail 157286/500000, 3MA
         hash 5, 1MS , 2A , 3MA
         hash 6, 1MS , 2A fail 157286/500000, 3MA
         hash 7, 1MS , 2A fail 157286/500000, 3MA
         hash 8, 1MS , 2A , 3MA
         hash 9, 1MS , 2A fail 157286/500000, 3MA
         hash 10, 1MS , 2A fail 9830/500000, 3MA fail 78643/500000
         hash 11, 1MS , 2A fail 614/500000, 3MA fail 4915/500000
         hash 12, 1MS , 2A fail 614/500000, 3MA fail 4915/500000
         hash 13, 1MS , 2A fail 157286/500000, 3MA
         hash 14, 1MS , 2A , 3MA
         hash 15, 1MS , 2A fail 157286/500000, 3MA fail 314572/500000
         hash 16, 1MS , 2A , 3MA
         */


        for (int idx = 0; idx < widths.length; idx++) {
            final int wide = widths[idx], high = heights[idx];
            final Point2[] shells = generatePointRectangle(wide, high, 0);
            int index = 0;
            for (IntIntToIntBiFunction op : hashes) {
                System.out.println("Working on size " + wide + " with hash " + index++ + ":");
                final IntIntToIntBiFunction hash = op;
                long start = System.nanoTime();
                ObjectSet set = new ObjectSet(51, LOAD) {
                    long collisionTotal = 0;
                    int longestPileup = 0, allPileups = 0, pileupChecks = 0;
                    double averagePileup = 0;

                    int hashMul = (int)(hashMultiplier & 0x1FFFFFL);

                    @Override
                    protected int place (@NonNull Object item) {
                        final Point2 p = (Point2)item;
//                        return (int)(hash.applyAsInt(p.x, p.y) * hashMultiplier >>> shift); // option 1MS
                        return hash.applyAsInt(p.x, p.y) & mask; // option 2A
//                        return hash.applyAsInt(p.x, p.y) * hashMul & mask; // option 3MA
                    }

                    @Override
                    protected void addResize (@NonNull Object key) {
                        Object[] keyTable = this.keyTable;
                        for (int i = place(key), p = 0; ; i = i + 1 & mask) {
                            if (keyTable[i] == null) {
                                keyTable[i] = key;
                                averagePileup += p;

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
                        shift = BitConversion.countLeadingZeros(mask) + 32;

                        // was using this in many tests
                        // total 1788695, longest 33, average 5.686122731838816, sum 160
                        hashMultiplier *= size + size ^ 0xF1357AEA2E62A9C5L;

                        hashMul = (int)(hashMultiplier & 0x1FFFFFL);

                        Object[] oldKeyTable = keyTable;

                        keyTable = new Object[newSize];

                        allPileups += longestPileup;
                        pileupChecks++;
                        collisionTotal = 0;
                        longestPileup = 0;
                        averagePileup = 0.0;

                        if (size > 0) {
                            for (int i = 0; i < oldCapacity; i++) {
                                Object key = oldKeyTable[i];
                                if (key != null) {addResize(key);}
                            }
                        }
                        if (collisionTotal > 150000)
                            throw new IllegalStateException("UH OH");

//                    System.out.println("hash multiplier: " + Base.BASE16.unsigned(hashMultiplier) + " with new size " + newSize);
//                    System.out.println("total collisions: " + collisionTotal);
//                    System.out.println("longest pileup: " + longestPileup);
//                    System.out.println("average pileup: " + (averagePileup / size));
                    }

                    @Override
                    public void clear () {
//                    System.out.println("hash multiplier: " + Base.BASE16.unsigned(hashMultiplier) + " with final size " + size);
                        System.out.println("total collisions: " + collisionTotal + ", longest pileup: " + longestPileup);
                        System.out.println("total of " + pileupChecks + " pileups: " + (allPileups + longestPileup));
                        super.clear();
                    }
                };
                try {
                    for (int i = 0; i < LEN; i++) {
                        set.add(shells[i]);
                    }
                    System.out.println("strong");
                } catch (IllegalStateException ex) {
                    System.out.println("Way too many collisions!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                    System.out.println("fail " + set.size() + "/" + LEN);
                }
                System.out.println((System.nanoTime() - start) + " ns");
                set.clear();
            }
        }
    }

    static class MeasuredFilteredStringSet extends FilteredStringSet {
        long collisionTotal = 0;
        int longestPileup = 0, allPileups = 0, pileupChecks = 0;
        double averagePileup = 0;

        public MeasuredFilteredStringSet () {
            super(51, PileupTest.LOAD);
        }
        public MeasuredFilteredStringSet (CharFilter filter) {
            super(filter, 51, PileupTest.LOAD);
        }
/*
        @Override
        protected long hashHelper (String s) {
            long hash = hashMultiplier;
            for (int i = 0, len = s.length(); i < len; i++) {
                char c = s.charAt(i);
                if (filter.filter.test(c)) {
                    //total collisions: 22039, longest pileup: 12
                    //total of 12 longest pileups: 78
//                    hash = ((hash << 57 | hash >>> 7) + filter.editor.applyAsChar(c)) * hashMultiplier;
                    //total collisions: 21691, longest pileup: 11
                    //total of 12 longest pileups: 72
//                    hash = ((hash << 53 | hash >>> 11) + filter.editor.applyAsChar(c)) * hashMultiplier;
                    //total collisions: 22262, longest pileup: 11
                    //total of 12 longest pileups: 82
//                    hash = ((hash << 49 | hash >>> 15) + filter.editor.applyAsChar(c)) * hashMultiplier;
                    //total collisions: 21868, longest pileup: 9
                    //total of 12 longest pileups: 66
                    hash = ((hash << 47 | hash >>> 17) + filter.editor.applyAsChar(c)) * hashMultiplier;
                    //total collisions: 21883, longest pileup: 11
                    //total of 12 longest pileups: 80
//                    hash = ((hash << 42 | hash >>> 22) + filter.editor.applyAsChar(c)) * hashMultiplier;
                    //total collisions: 21647, longest pileup: 9
                    //total of 12 longest pileups: 81
//                    hash = ((hash << 32 | hash >>> 32) + filter.editor.applyAsChar(c)) * hashMultiplier;
                    //total collisions: 21634, longest pileup: 10
                    //total of 12 longest pileups: 74
//                    hash = ((hash << 17 | hash >>> 47) + filter.editor.applyAsChar(c)) * hashMultiplier;
                    //total collisions: 21616, longest pileup: 12
                    //total of 12 longest pileups: 74
//                    hash = ((hash << 11 | hash >>> 53) + filter.editor.applyAsChar(c)) * hashMultiplier;
                    //total collisions: 22322, longest pileup: 11
                    //total of 12 longest pileups: 70
//                    hash = (hash + filter.editor.applyAsChar(c)) * hashMultiplier;
                    //total collisions: 22167, longest pileup: 12
                    //total of 12 longest pileups: 76
//                    hash = (hash ^ filter.editor.applyAsChar(c)) * hashMultiplier;
                    //total collisions: 12643054, longest pileup: 395
                    //total of 12 longest pileups: 934
//                    hash += filter.editor.applyAsChar(c) * hashMultiplier;
                }
            }
            return hash;
        }
*/

        @Override
        protected int hashHelper (final String s) {
            final int hm = hashMultiplier;
            int hash = hm;
            for (int i = 0, len = s.length(), ctr = len; i < len; i++) {
                final char c = s.charAt(i);
                if (filter.filter.test(c)) {
                    // WITH XRR AND MULTIPLY
                    //total collisions: 21573, longest pileup: 9
                    //total of 12 longest pileups: 69
                    // WITH JUST RETURN HASH
                    //total collisions: 21879, longest pileup: 12
                    //total of 12 longest pileups: 76
                    // WITH XRR ONLY
                    //total collisions: 21823, longest pileup: 11
                    //total of 12 longest pileups: 80
                    // WITH MULTIPLY ONLY
                    //total collisions: 22152, longest pileup: 11
                    //total of 12 longest pileups: 74
                    hash = (hash << 13 | hash >>> 19) + filter.editor.applyAsChar(c);
                    if((--ctr & 1) == 0) hash *= hm;
                    // WITH XOR HM
                    //total collisions: 21626, longest pileup: 11
                    //total of 12 longest pileups: 74
                    // WITH XOR ORIGINAL
                    //total collisions: 21959, longest pileup: 11
                    //total of 12 longest pileups: 69
//                    hash = (hash << 16 | hash >>> 48) ^ filter.editor.applyAsChar(c);
//                    if((--ctr & 3) == 0) hash *= (hm += 0x9E3779B97F4A7C16L);
                    // XRR MULTIPLY
                    //total collisions: 21773, longest pileup: 11
                    //total of 12 longest pileups: 70
                    // WITH JUST RETURN HASH
                    //total collisions: 21967, longest pileup: 13
                    //total of 12 longest pileups: 72
//                    hash = (hash << 16 | hash >>> 48) + filter.editor.applyAsChar(c);
//                    if((--ctr & 3) == 0) hash *= hm;
                    // XRR MULTIPLY
                    //total collisions: 21681, longest pileup: 9
                    //total of 12 longest pileups: 84
                    // WITH JUST RETURN HASH
                    //total collisions: 21706, longest pileup: 10
                    //total of 12 longest pileups: 77
//                    hash = (hash << 16 | hash >>> 48) - filter.editor.applyAsChar(c);
//                    if((--ctr & 3) == 0) hash *= hm;
                    // XRR MULTIPLY
                    //total collisions: 21840, longest pileup: 12
                    //total of 12 longest pileups: 87
                    // WITH JUST RETURN HASH
                    //total collisions: 21746, longest pileup: 11
                    //total of 12 longest pileups: 81
//                    hash = filter.editor.applyAsChar(c) - (hash << 16 | hash >>> 48);
//                    if((--ctr & 3) == 0) hash *= hm;
                    // XRR MULTIPLY
                    //total collisions: 22401, longest pileup: 10
                    //total of 12 longest pileups: 68
//                    hash = (hash << 16 | hash >>> 48) ^ filter.editor.applyAsChar(c);
//                    if((--ctr & 3) == 0) hash = (hash ^ hash >>> 29) * hm;
                }
            }
            hash *= hm;
            return hash ^ (hash << 23 | hash >>> 9) ^ (hash << 11 | hash >>> 21);
//            return (hash ^ (hash << 23 | hash >>> 41) ^ (hash << 42 | hash >>> 22));
//            return hash * hm;
//            return hash * (hm ^ 0x9E3779B97F4A7C16L);
//            return hash * (hashMultiplier);
//            return hash;
        }

        @Override
        protected int place (@NonNull Object item) {
            return super.place(item);
        }

        @Override
        protected void addResize (@NonNull String key) {
            Object[] keyTable = this.keyTable;
            for (int i = place(key), p = 0; ; i = i + 1 & mask) {
                if (keyTable[i] == null) {
                    keyTable[i] = key;
                    averagePileup += p;

                    return;
                } else {
                    collisionTotal++;
                    longestPileup = Math.max(longestPileup, ++p);
                }
            }
        }

        @Override
        protected void resize (int newSize) {
            allPileups += longestPileup;
            pileupChecks++;
            collisionTotal = 0;
            longestPileup = 0;
            averagePileup = 0.0;

            super.resize(newSize);

            System.out.println("hash multiplier: " + Base.BASE16.unsigned(hashMultiplier) + " with new size " + newSize);
            System.out.print("total collisions: " + collisionTotal);
            System.out.println(", longest pileup: " + longestPileup);
            System.out.println("average pileup: " + (averagePileup / size));
        }

        @Override
        public void clear () {
            System.out.println("hash multiplier: " + Base.BASE16.unsigned(hashMultiplier) + " with final size " + size);
            System.out.print("total collisions: " + collisionTotal);
            System.out.println(", longest pileup: " + longestPileup);
            System.out.println("total of " + pileupChecks + " longest pileups: " + (allPileups + longestPileup));
            super.clear();
        }
    }
    static class MeasuredCaseInsensitiveSet extends CaseInsensitiveSet {
        long collisionTotal = 0;
        int longestPileup = 0, allPileups = 0, pileupChecks = 0;
        double averagePileup = 0;

        public MeasuredCaseInsensitiveSet () {
            super(51, PileupTest.LOAD);
        }

        @Override
        protected int place (@NonNull Object item) {
            //total collisions: 21742, longest pileup: 10
            //total of 12 longest pileups: 69
            return super.place(item);
        }

        @Override
        protected void addResize (@NonNull CharSequence key) {
            Object[] keyTable = this.keyTable;
            for (int i = place(key), p = 0; ; i = i + 1 & mask) {
                if (keyTable[i] == null) {
                    keyTable[i] = key;
                    averagePileup += p;

                    return;
                } else {
                    collisionTotal++;
                    longestPileup = Math.max(longestPileup, ++p);
                }
            }
        }

        @Override
        protected void resize (int newSize) {
            allPileups += longestPileup;
            pileupChecks++;
            collisionTotal = 0;
            longestPileup = 0;
            averagePileup = 0.0;

            super.resize(newSize);

            System.out.println("hash multiplier: " + Base.BASE16.unsigned(hashMultiplier) + " with new size " + newSize);
            System.out.print("total collisions: " + collisionTotal);
            System.out.println(", longest pileup: " + longestPileup);
            System.out.println("average pileup: " + (averagePileup / size));
        }

        @Override
        public void clear () {
            System.out.println("hash multiplier: " + Base.BASE16.unsigned(hashMultiplier) + " with final size " + size);
            System.out.print("total collisions: " + collisionTotal);
            System.out.println(", longest pileup: " + longestPileup);
            System.out.println("total of " + pileupChecks + " longest pileups: " + (allPileups + longestPileup));
            super.clear();
        }
    }
}

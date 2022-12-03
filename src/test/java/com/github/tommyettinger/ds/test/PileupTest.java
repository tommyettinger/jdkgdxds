package com.github.tommyettinger.ds.test;

import com.github.tommyettinger.digital.Base;
import com.github.tommyettinger.digital.BitConversion;
import com.github.tommyettinger.digital.Hasher;
import com.github.tommyettinger.ds.ObjectSet;
import com.github.tommyettinger.random.WhiskerRandom;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.IntBinaryOperator;

public class PileupTest {
    public static final int LEN = 500000;

    public static String[] generateUniqueWordsFibSet (int size) {
        final int numLetters = 4;
        ObjectSet<String> set = new ObjectSet<String>(size, 0.6f) {
            @Override
            protected int place (Object item) {
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

    //    @Ignore // this test used to take much longer to run than the others here (over a minute; everything else is under a second).
    @Test
    public void testObjectSetOld () {
        final String[] words = generateUniqueWords(LEN, -123456789L);
        long start = System.nanoTime();
        // replicates old ObjectSet behavior, with added logging and the constant in place() changed
        ObjectSet set = new ObjectSet(51, 0.6f) {
            long collisionTotal = 0;
            int longestPileup = 0, allPileups = 0, pileupChecks = 0;
            double averagePileup = 0;

            @Override
            protected int place (Object item) {
                return (int)(item.hashCode() * 0x9E3779B97F4A7C15L >>> shift);
            }

//            @Override
//            protected int place (Object item) {
//                return (int)(item.hashCode() * 0xD1B54A32D192ED03L >>> shift); // does extremely well???
//            }

            @Override
            protected void addResize (@Nonnull Object key) {
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
                shift = Long.numberOfLeadingZeros(mask);

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
        System.out.println(System.nanoTime() - start);
        set.clear();
    }

    @Test
    public void testObjectSetNew () {
        final String[] words = generateUniqueWords(LEN, -123456789L);
        long start = System.nanoTime();
        ObjectSet set = new ObjectSet(51, 0.6f) {
            long collisionTotal = 0;
            int longestPileup = 0, allPileups = 0, pileupChecks = 0;
            double averagePileup = 0;

            @Override
            protected int place (Object item) {
                return (int)(hashMultiplier * item.hashCode()) >>> shift;
            }

            @Override
            protected void addResize (@Nonnull Object key) {
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
                shift = Long.numberOfLeadingZeros(mask);

                // multiplier from Steele and Vigna, Computationally Easy, Spectrally Good Multipliers for Congruential
                // Pseudorandom Number Generators
//                hashMultiplier *= 0xF1357AEA2E62A9C5L;
                // ensures hashMultiplier is never too small, and is always odd
//                hashMultiplier |= 0x0000010000000001L;

                // we modify the hash multiplier by multiplying it by a number that Vigna and Steele considered optimal
                // for a 64-bit MCG random number generator, XORed with 2 times size to randomize the low bits more.
//                hashMultiplier *= size + size ^ 0xF1357AEA2E62A9C5L;

                hashMultiplier *= 0xF1357AEA2E62A9C5L;

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
        System.out.println(System.nanoTime() - start);
        set.clear();
    }

    @Test
    public void testObjectQuadSet () {
        final String[] words = generateUniqueWords(LEN, -123456789L);
        long start = System.nanoTime();
        ObjectQuadSet set = new ObjectQuadSet(51, 0.6f) {
            long collisionTotal = 0;
            int longestPileup = 0, allPileups = 0, pileupChecks = 0;
            double averagePileup = 0;

            @Override
            protected void addResize (@Nonnull Object key) {
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
                shift = Long.numberOfLeadingZeros(mask);

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
        System.out.println(System.nanoTime() - start);
        set.clear();
    }

    @Test
    public void testObjectQuadSetExperimental () {
        final String[] words = generateUniqueWords(LEN, -123456789L);
        long start = System.nanoTime();
        ObjectQuadSet set = new ObjectQuadSet(51, 0.6f) {
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
            protected void addResize (@Nonnull Object key) {
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
                shift = Long.numberOfLeadingZeros(mask);

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
        System.out.println(System.nanoTime() - start);
        set.clear();
    }

    @Test
    public void testObjectQuadSetSimplePlace () {
        final String[] words = generateUniqueWords(LEN, -123456789L);
        long start = System.nanoTime();
        ObjectQuadSet set = new ObjectQuadSet(51, 0.6f) {
            long collisionTotal = 0;
            int longestPileup = 0, allPileups = 0, pileupChecks = 0;
            double averagePileup = 0;
            int hm = 0x13C6ED;

            @Override
            protected int place (Object item) {
                return item.hashCode() * hm & mask;
            }

            @Override
            protected void addResize (@Nonnull Object key) {
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
                shift = Long.numberOfLeadingZeros(mask);

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
        System.out.println(System.nanoTime() - start);
        set.clear();
    }

    @Test
    public void testObjectSetIntPlace () {
        final String[] words = generateUniqueWords(LEN, -123456789L);
        long start = System.nanoTime();
        ObjectSet set = new ObjectSet(51, 0.6f) {
            long collisionTotal = 0;
            int longestPileup = 0, allPileups = 0, pileupChecks = 0;
            double averagePileup = 0;
            int hm = 0x13C6ED;

            @Override
            protected int place (Object item) {
                return item.hashCode() * hm >>> shift;
            }

            @Override
            protected void addResize (@Nonnull Object key) {
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
                shift = Long.numberOfLeadingZeros(mask);

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
        System.out.println(System.nanoTime() - start);
        set.clear();
    }

    public static BadString[] generateUniqueBadFibSet (int size) {
        final int numLetters = 4;
        ObjectSet<BadString> set = new ObjectSet<BadString>(size, 0.6f) {
            @Override
            protected int place (Object item) {
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

    @Test
    public void testBadStringSetOld () {
        final BadString[] words = generateUniqueBad(LEN, -123456789L);
        long start = System.nanoTime();
        // replicates old ObjectSet behavior, with added logging
        ObjectSet set = new ObjectSet(51, 0.6f) {
            long collisionTotal = 0;
            int longestPileup = 0, allPileups = 0, pileupChecks = 0;
            double averagePileup = 0;

            @Override
            protected int place (Object item) {
                return (int)(item.hashCode() * 0xD1B54A32D192ED03L >>> shift); // if this long constant is the same as the one used
                // by place() in generateUniqueBad's map, then this slows down massively.
            }

            @Override
            protected void addResize (@Nonnull Object key) {
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
                shift = Long.numberOfLeadingZeros(mask);

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
        System.out.println(System.nanoTime() - start);
        set.clear();
    }

    @Test
    public void testBadStringSetNew () {
        final BadString[] words = generateUniqueBad(LEN, -123456789L);
        long start = System.nanoTime();
        ObjectSet set = new ObjectSet(51, 0.6f) {
            long collisionTotal = 0;
            int longestPileup = 0, allPileups = 0, pileupChecks = 0;
            double averagePileup = 0;

            {
                hashMultiplier = 0x9E3779B97F4A7C15L; // total collisions: 69101, longest pileup: 15
//                hashMultiplier = 0x769C3DC968DB6A07L; // total collisions: 74471, longest pileup: 14
//                hashMultiplier = 0xD1B54A32D192ED03L; // total collisions: 68210, longest pileup: 19
            }

            @Override
            protected void addResize (@Nonnull Object key) {
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
                shift = Long.numberOfLeadingZeros(mask);

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
//                hashMultiplier *= size + size ^ 0xF1357AEA2E62A9C5L;
//                hashMultiplier *= size + size ^ 0xF1357AEA2E62A9C5L;
                hashMultiplier *= 0xF1357AEA2E62A9C5L;

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
        System.out.println(System.nanoTime() - start);
        set.clear();
    }

    @Test
    public void testBadStringQuadSet () {
        final BadString[] words = generateUniqueBad(LEN, -123456789L);
        long start = System.nanoTime();
        ObjectQuadSet set = new ObjectQuadSet(51, 0.6f) {
            long collisionTotal = 0;
            int longestPileup = 0, allPileups = 0, pileupChecks = 0;
            double averagePileup = 0;

            @Override
            protected void addResize (@Nonnull Object key) {
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
                shift = Long.numberOfLeadingZeros(mask);
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
//        final int limit = (int)(Math.sqrt(LEN));
//        for (int x = -limit; x < limit; x+=2) {
//            for (int y = -limit; y < limit; y+=2) {
//                set.add(new Vector2(x, y));
//            }
//        }
        for (int i = 0; i < LEN; i++) {
            set.add(words[i]);
        }
        System.out.println(System.nanoTime() - start);
        set.clear();
    }

    @Test
    public void testBadStringQuadSetGold () {
        final BadString[] words = generateUniqueBad(LEN, -123456789L);
        long start = System.nanoTime();
        ObjectQuadSet set = new ObjectQuadSet(51, 0.6f) {
            long collisionTotal = 0;
            int longestPileup = 0, allPileups = 0, pileupChecks = 0;
            double averagePileup = 0;

            {
                hashMultiplier = 0xD1B54A32D192ED03L;
            }

            @Override
            protected void addResize (@Nonnull Object key) {
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
                shift = Long.numberOfLeadingZeros(mask);
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
        System.out.println(System.nanoTime() - start);
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

    public static final long CONSTANT =
//        0x9E3779B97F4A7C15L; //int: total collisions: 33748, longest pileup: 12, long: total collisions: 34124, longest pileup: 13
//        0xD1B54A32D192ED03L;//long: total collisions: 33579, longest pileup: 12
        0xF1357AEA2E62A9C5L;//long: total collisions: 34430, longest pileup: 11

    @Test
    public void testStringSetWordList () throws IOException {
        final List<String> words = Files.readAllLines(Paths.get("src/test/resources/word_list.txt"));
        Collections.shuffle(words, new WhiskerRandom(1234567890L));
        long start = System.nanoTime();
        // replicates old ObjectSet behavior, with added logging
        ObjectSet set = new ObjectSet(51, 0.6f) {
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
                hashMultiplier = CONSTANT >>> shift + 32 | 1L; //total collisions: 34430, longest pileup: 11
//                hashMultiplier = 0x769C3DC968DB6A07L;
                hashMul = (int)(hashMultiplier >>> 32);
            }

            @Override
            protected int place (Object item) {
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
            protected void addResize (@Nonnull Object key) {
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
                shift = Long.numberOfLeadingZeros(mask);

//                hashAddend = (hashAddend ^ hashAddend >>> 11 ^ size) * 0x13C6EB ^ 0xC79E7B1D;
//                hashMul *= 0x9E3779B9 + size + size;
//                hashMul *= 0x2E62A9C5 + size + size;
//                hashMul *= 0x2E62A9C5 ^ size + size;
//                hashMul =  hashMul * 0x9E377 & 0xFFFFF;
//                hashMultiplier *= (long)size << 3 ^ 0xF1357AEA2E62A9C5L;
//                hashMultiplier *= 0xF1357AEA2E62A9C5L;
                hashMultiplier = CONSTANT >>> shift + 32 | 1L;
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
        for (int i = 0; i < words.size(); i++) {
            set.add(words.get(i));
        }
        System.out.println(System.nanoTime() - start);
        set.clear();
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
                ObjectSet set = new ObjectSet(51, 0.6f) {
                    long collisionTotal = 0;
                    int longestPileup = 0, allPileups = 0, pileupChecks = 0;
                    double averagePileup = 0;

                    int ra = finalA, rb = finalB; //total collisions: 33707, longest pileup: 17
                    int oa = ra, ob = rb;

                    @Override
                    protected int place (Object item) {
                        final int h = item.hashCode();
                        return (h ^ (h << ra | h >>> -ra) ^ (h << rb | h >>> -rb)) & mask;
                    }

                    @Override
                    protected void addResize (@Nonnull Object key) {
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
                        shift = Long.numberOfLeadingZeros(mask);

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
//        final int limit = (int)(Math.sqrt(LEN));
//        for (int x = -limit; x < limit; x+=2) {
//            for (int y = -limit; y < limit; y+=2) {
//                set.add(new Vector2(x, y));
//            }
//        }
                for (int i = 0; i < words.size(); i++) {
                    set.add(words.get(i));
                }
//                System.out.println(System.nanoTime() - start);
                set.clear();

            }
        }
    }

    @Test
    public void testVector2SetOld () {
        final Vector2[] spiral = generateVectorSpiral(LEN);
        long start = System.nanoTime();
        // replicates old ObjectSet behavior, with added logging
        ObjectSet set = new ObjectSet(51, 0.6f) {
            long collisionTotal = 0;
            int longestPileup = 0, allPileups = 0, pileupChecks = 0;
            double averagePileup = 0;

            int hashMul = 0xEB18A809; // total collisions: 1752402, longest pileup: 21
            //0x9E3779B9;  // total collisions: 1759892,    longest pileup: 25 , maybe?
            // 0x1A36A9;

            {
                hashMultiplier =
//                    0x9E3779B97F4A7C15L;
                    0x769C3DC968DB6A07L;
//                    0xD1B54A32D192ED03L;//long: total collisions: 33579, longest pileup: 12
//                    0xF1357AEA2E62A9C5L;//long: total collisions: 34430, longest pileup: 11
            }

            @Override
            protected int place (Object item) {
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
            protected void addResize (@Nonnull Object key) {
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
                shift = Long.numberOfLeadingZeros(mask);

//                hashAddend = (hashAddend ^ hashAddend >>> 11 ^ size) * 0x13C6EB ^ 0xC79E7B1D;
                hashMul = hashMul * 0x2E62A9C5;

                // this one is used in 1.0.5
//                hashMultiplier *= size + size ^ 0xF1357AEA2E62A9C5L;
                hashMultiplier *= (long)size << 3 ^ 0xF1357AEA2E62A9C5L;
//                hashMultiplier *= 0xF1357AEA2E62A9C5L;

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
        System.out.println(System.nanoTime() - start);
        set.clear();
    }

    @Test
    public void testVector2SetNew () {
        final Vector2[] spiral = generateVectorSpiral(LEN);
        long start = System.nanoTime();
        ObjectSet set = new ObjectSet(51, 0.6f) {
            long collisionTotal = 0;
            int longestPileup = 0, allPileups = 0, pileupChecks = 0;
            double averagePileup = 0;

//            {
//                hashMultiplier = 0xD1B54A32D192ED03L;
//            }
            @Override
            protected void addResize (@Nonnull Object key) {
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
                shift = Long.numberOfLeadingZeros(mask);

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
        System.out.println(System.nanoTime() - start);
        set.clear();
    }

    @Test
    public void testVector2QuadSet () {
        final Vector2[] spiral = generateVectorSpiral(LEN);
        long start = System.nanoTime();
        ObjectQuadSet set = new ObjectQuadSet(51, 0.6f) {
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
            protected void addResize (@Nonnull Object key) {
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
                shift = Long.numberOfLeadingZeros(mask);

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
        System.out.println(System.nanoTime() - start);
        set.clear();
    }


    @Test
    public void testPointSetShells () {
        final Point2[] shells = generatePointShells(LEN);
        IntBinaryOperator[] hashes = {
            ((x, y) -> x * 0x125493 + y * 0x19E373),
            ((x, y) -> x * 0xDEED5 + y * 0xBEA57),
            ((x, y) -> (int)((x * 107 + y) * 0xD1B54A32D192ED03L >>> 32)),
            ((x, y) -> (x >= y ? x * (x + 8) - y + 12 : y * (y + 6) + x + 12)),
            ((x, y) -> {int n = (x >= y ? x * (x + 8) - y + 12 : y * (y + 6) + x + 12); return n ^ n >>> 1;}),
            ((x, y) -> {int n = (x >= y ? x * (x + 8) - y + 12 : y * (y + 6) + x + 12); return ((n ^ n >>> 1) * 0x9E373 ^ 0xD1B54A35) * 0x125493 ^ 0x91E10DA5;}),
            ((x, y) -> y + ((x + y + 6) * (x + y + 7) >>> 1)),
            ((x, y) -> {int n = (y + ((x + y + 6) * (x + y + 7) >> 1)); return n ^ n >>> 1;}),
            ((x, y) -> {int n = (y + ((x + y + 6) * (x + y + 7) >> 1)); return ((n ^ n >>> 1) * 0x9E373 ^ 0xD1B54A35) * 0x125493 ^ 0x91E10DA5;}),
            ((x, y) -> (y + ((x + y) * (x + y + 1) + 36 >>> 1))),
            ((x, y) -> {
                x |= y << 16;
                x =    ((x & 0x0000ff00) << 8) | ((x >>> 8) & 0x0000ff00) | (x & 0xff0000ff);
                x =    ((x & 0x00f000f0) << 4) | ((x >>> 4) & 0x00f000f0) | (x & 0xf00ff00f);
                x =    ((x & 0x0c0c0c0c) << 2) | ((x >>> 2) & 0x0c0c0c0c) | (x & 0xc3c3c3c3);
                return ((x & 0x22222222) << 1) | ((x >>> 1) & 0x22222222) | (x & 0x99999999);
            }),
            ((x, y) -> (x ^ (y << 16 | y >>> 16))),
            ((x, y) -> (x + (y << 16 | y >>> 16))),
        };
        int index = 0;
        for(IntBinaryOperator op : hashes) {
            System.out.println("Working with hash " + index++ + ":");
            final IntBinaryOperator hash = op;
            long start = System.nanoTime();
            ObjectSet set = new ObjectSet(51, 0.6f) {
                long collisionTotal = 0;
                int longestPileup = 0, allPileups = 0, pileupChecks = 0;
                double averagePileup = 0;

                //            {
//                hashMultiplier = 0xD1B54A32D192ED03L;
//            }
                int hashMul = (int)(hashMultiplier & 0x1FFFFFL);

                @Override
                protected int place (Object item) {
                    final Point2 p = (Point2)item;
//                    return (int)(hash.applyAsInt(p.x, p.y) * hashMultiplier >>> shift);
                    return hash.applyAsInt(p.x, p.y) & mask;
//                    return hash.applyAsInt(p.x, p.y) * hashMul & mask;
                }

                @Override
                protected void addResize (@Nonnull Object key) {
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
                    shift = Long.numberOfLeadingZeros(mask);

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
            System.out.println(System.nanoTime() - start);
            set.clear();
        }
    }

    @Test
    public void testPointSetAngles () {
        final Point2[] shells = generatePointAngles(LEN);
        IntBinaryOperator[] hashes = {
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
        for(IntBinaryOperator op : hashes) {
            System.out.println("Working with hash " + index++ + ":");
            final IntBinaryOperator hash = op;
            long start = System.nanoTime();
            ObjectSet set = new ObjectSet(51, 0.6f) {
                long collisionTotal = 0;
                int longestPileup = 0, allPileups = 0, pileupChecks = 0;
                double averagePileup = 0;

                //            {
//                hashMultiplier = 0xD1B54A32D192ED03L;
//            }
                int hashMul = (int)(hashMultiplier & 0x1FFFFFL);

                @Override
                protected int place (Object item) {
                    final Point2 p = (Point2)item;
//                    return (int)(hash.applyAsInt(p.x, p.y) * hashMultiplier >>> shift); // option 1MS
                    return hash.applyAsInt(p.x, p.y) & mask; // option 2A
//                    return hash.applyAsInt(p.x, p.y) * hashMul & mask; // option 3MA
                }

                @Override
                protected void addResize (@Nonnull Object key) {
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
                    shift = Long.numberOfLeadingZeros(mask);

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
            System.out.println(System.nanoTime() - start);
            set.clear();
        }
    }

    @Test
    public void testPointSetSpiral () {
        final Point2[] shells = generatePointSpiral(LEN);
        IntBinaryOperator[] hashes = {
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
        for(IntBinaryOperator op : hashes) {
            System.out.println("Working with hash " + index++ + ":");
            final IntBinaryOperator hash = op;
            long start = System.nanoTime();
            ObjectSet set = new ObjectSet(51, 0.6f) {
                long collisionTotal = 0;
                int longestPileup = 0, allPileups = 0, pileupChecks = 0;
                double averagePileup = 0;

                int hashMul = (int)(hashMultiplier & 0x1FFFFFL);

                @Override
                protected int place (Object item) {
                    final Point2 p = (Point2)item;
                    return (int)(hash.applyAsInt(p.x, p.y) * hashMultiplier >>> shift); // option 1MS
//                    return hash.applyAsInt(p.x, p.y) & mask; // option 2A
//                    return hash.applyAsInt(p.x, p.y) * hashMul & mask; // option 3MA
                }

                @Override
                protected void addResize (@Nonnull Object key) {
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
                    shift = Long.numberOfLeadingZeros(mask);

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
            System.out.println(System.nanoTime() - start);
            set.clear();
        }
    }

    @Test
    public void testPointSetRectangles () {
        final int[] widths = {500, 200, 100, 50};
        final int[] heights = {LEN / 500, LEN / 200, LEN / 100, LEN / 50};
        final IntBinaryOperator[] hashes = {
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
            for (IntBinaryOperator op : hashes) {
                System.out.println("Working with hash " + index++ + ":");
                final IntBinaryOperator hash = op;
                long start = System.nanoTime();
                ObjectSet set = new ObjectSet(51, 0.6f) {
                    long collisionTotal = 0;
                    int longestPileup = 0, allPileups = 0, pileupChecks = 0;
                    double averagePileup = 0;

                    int hashMul = (int)(hashMultiplier & 0x1FFFFFL);

                    @Override
                    protected int place (Object item) {
                        final Point2 p = (Point2)item;
//                    return (int)(hash.applyAsInt(p.x, p.y) * hashMultiplier >>> shift); // option 1MS
//                        return hash.applyAsInt(p.x, p.y) & mask; // option 2A
                    return hash.applyAsInt(p.x, p.y) * hashMul & mask; // option 3MA
                    }

                    @Override
                    protected void addResize (@Nonnull Object key) {
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
                        shift = Long.numberOfLeadingZeros(mask);

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
                System.out.println(System.nanoTime() - start);
                set.clear();
            }
        }
    }



    @Test
    public void testPointSetShellsFull () {
        final Point2[] shells = generatePointShells(LEN, 0);
        IntBinaryOperator[] hashes = {
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
        for(IntBinaryOperator op : hashes) {
            System.out.println("Working with hash " + index++ + ":");
            final IntBinaryOperator hash = op;
            long start = System.nanoTime();
            ObjectSet set = new ObjectSet(51, 0.6f) {
                long collisionTotal = 0;
                int longestPileup = 0, allPileups = 0, pileupChecks = 0;
                double averagePileup = 0;

                int hashMul = (int)(hashMultiplier & 0x1FFFFFL);

                @Override
                protected int place (Object item) {
                    final Point2 p = (Point2)item;
//                    return (int)(hash.applyAsInt(p.x, p.y) * hashMultiplier >>> shift);
                    return hash.applyAsInt(p.x, p.y) & mask;
//                    return hash.applyAsInt(p.x, p.y) * hashMul & mask;
                }

                @Override
                protected void addResize (@Nonnull Object key) {
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
                    shift = Long.numberOfLeadingZeros(mask);

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
            System.out.println(System.nanoTime() - start);
            set.clear();
        }
    }

    @Test
    public void testPointSetAnglesFull () {
        final Point2[] shells = generatePointAngles(LEN, 0);
        IntBinaryOperator[] hashes = {
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
        for(IntBinaryOperator op : hashes) {
            System.out.println("Working with hash " + index++ + ":");
            final IntBinaryOperator hash = op;
            long start = System.nanoTime();
            ObjectSet set = new ObjectSet(51, 0.6f) {
                long collisionTotal = 0;
                int longestPileup = 0, allPileups = 0, pileupChecks = 0;
                double averagePileup = 0;

                int hashMul = (int)(hashMultiplier & 0x1FFFFFL);

                @Override
                protected int place (Object item) {
                    final Point2 p = (Point2)item;
//                    return (int)(hash.applyAsInt(p.x, p.y) * hashMultiplier >>> shift); // option 1MS
                    return hash.applyAsInt(p.x, p.y) & mask; // option 2A
//                    return hash.applyAsInt(p.x, p.y) * hashMul & mask; // option 3MA
                }

                @Override
                protected void addResize (@Nonnull Object key) {
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
                    shift = Long.numberOfLeadingZeros(mask);

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
            System.out.println(System.nanoTime() - start);
            set.clear();
        }
    }

    @Test
    public void testPointSetSpiralFull () {
        final Point2[] shells = generatePointSpiral(LEN, 0);
        IntBinaryOperator[] hashes = {
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
        for(IntBinaryOperator op : hashes) {
            System.out.println("Working with hash " + index++ + ":");
            final IntBinaryOperator hash = op;
            long start = System.nanoTime();
            ObjectSet set = new ObjectSet(51, 0.6f) {
                long collisionTotal = 0;
                int longestPileup = 0, allPileups = 0, pileupChecks = 0;
                double averagePileup = 0;

                int hashMul = (int)(hashMultiplier & 0x1FFFFFL);

                @Override
                protected int place (Object item) {
                    final Point2 p = (Point2)item;
//                    return (int)(hash.applyAsInt(p.x, p.y) * hashMultiplier >>> shift); // option 1MS
                    return hash.applyAsInt(p.x, p.y) & mask; // option 2A
//                    return hash.applyAsInt(p.x, p.y) * hashMul & mask; // option 3MA
                }

                @Override
                protected void addResize (@Nonnull Object key) {
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
                    shift = Long.numberOfLeadingZeros(mask);

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
            System.out.println(System.nanoTime() - start);
            set.clear();
        }
    }

    @Test
    public void testPointSetRectanglesFull () {
        final int[] widths = {500, 200, 100, 50};
        final int[] heights = {LEN / 500, LEN / 200, LEN / 100, LEN / 50};
        final IntBinaryOperator[] hashes = {
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
            for (IntBinaryOperator op : hashes) {
                System.out.println("Working on size " + wide + " with hash " + index++ + ":");
                final IntBinaryOperator hash = op;
                long start = System.nanoTime();
                ObjectSet set = new ObjectSet(51, 0.6f) {
                    long collisionTotal = 0;
                    int longestPileup = 0, allPileups = 0, pileupChecks = 0;
                    double averagePileup = 0;

                    int hashMul = (int)(hashMultiplier & 0x1FFFFFL);

                    @Override
                    protected int place (Object item) {
                        final Point2 p = (Point2)item;
//                        return (int)(hash.applyAsInt(p.x, p.y) * hashMultiplier >>> shift); // option 1MS
                        return hash.applyAsInt(p.x, p.y) & mask; // option 2A
//                        return hash.applyAsInt(p.x, p.y) * hashMul & mask; // option 3MA
                    }

                    @Override
                    protected void addResize (@Nonnull Object key) {
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
                        shift = Long.numberOfLeadingZeros(mask);

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
                System.out.println(System.nanoTime() - start);
                set.clear();
            }
        }
    }

}

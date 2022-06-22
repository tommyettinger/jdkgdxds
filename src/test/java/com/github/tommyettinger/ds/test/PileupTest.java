package com.github.tommyettinger.ds.test;

import com.github.tommyettinger.digital.Base;
import com.github.tommyettinger.ds.IdentityObjectMap;
import com.github.tommyettinger.ds.ObjectSet;
import org.junit.Ignore;
import org.junit.Test;

import javax.annotation.Nonnull;

public class PileupTest {
    public static final int LEN = 200000;
    public static String[] generateUniqueWords(int size) {
        final int numLetters = 3;
        ObjectSet<String> set = new ObjectSet<>(size, 0.8f);
        char[] maker = new char[numLetters];
        for (int i = 0; set.size() < size; ) {
            for (int x = 0; x < 26 && set.size() < size; x++) {
                maker[0] = (char) ('a' + x);
                for (int y = 0; y < 26 && set.size() < size; y++) {
                    maker[1] = (char) ('a' + y);
                    for (int z = 0; z < 26 && set.size() < size; z++) {
                        maker[2] = (char) ('a' + z);
                        set.add(String.valueOf(maker) + (i++));
                    }
                }
            }
        }
        return set.toArray(new String[0]);
    }

    @Ignore
    @Test
    public void testObjectSetOld() {
        long start = System.nanoTime();
        ObjectSet set = new ObjectSet(51, 0.8f) {
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
                System.out.println("hash multiplier: " + Base.BASE16.unsigned(hashMultiplier));
                System.out.println("total collisions: " + collisionTotal);
                System.out.println("longest pileup: " + longestPileup);
            }
        };
//        final int limit = (int)(Math.sqrt(LEN));
//        for (int x = -limit; x < limit; x+=2) {
//            for (int y = -limit; y < limit; y+=2) {
//                set.add(new Vector2(x, y));
//            }
//        }
        String[] words = generateUniqueWords(LEN);
        for (int i = 0; i < LEN; i++) {
            set.add(words[i]);
        }
        System.out.println(System.nanoTime() - start);
    }
    @Test
    public void testObjectSetNew() {
        long start = System.nanoTime();
        ObjectSet set = new ObjectSet(51, 0.8f) {
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
//                hashMultiplier *= 0xF1357AEA2E62A9C5L;
                // ensures hashMultiplier is never too small, and is always odd
//                hashMultiplier |= 0x0000010000000001L;

                // we modify the hash multiplier by adding size twice (keeping the result odd), then multiply by a number
                // that Vigna and Steele considered optimal for a 64-bit MCG random number generator.
                hashMultiplier = (hashMultiplier + size + size) * 0xF1357AEA2E62A9C5L;

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
                System.out.println("hash multiplier: " + Base.BASE16.unsigned(hashMultiplier));
                System.out.println("total collisions: " + collisionTotal);
                System.out.println("longest pileup: " + longestPileup);
            }
        };
//        final int limit = (int)(Math.sqrt(LEN));
//        for (int x = -limit; x < limit; x+=2) {
//            for (int y = -limit; y < limit; y+=2) {
//                set.add(new Vector2(x, y));
//            }
//        }
        String[] words = generateUniqueWords(LEN);
        for (int i = 0; i < LEN; i++) {
            set.add(words[i]);
        }
        System.out.println(System.nanoTime() - start);
    }

    private static class BadString implements CharSequence {
        public CharSequence text;
        public BadString () {
            text = "aaa0";
        }

        public BadString(CharSequence text){
            this.text = text;
        }

        @Override
        public int hashCode () {
//            return text.charAt(0);
            int h = 0;
            for (int i = 0, n = text.length(); i < n; i++) {
                h = h * 107 + text.charAt(i);
            }
            return h;
        }

        @Override
        public boolean equals (Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            BadString badString = (BadString)o;

            return text.equals(badString.text);
        }

        @Override
        public int length () {
            return text.length();
        }

        @Override
        public char charAt (int index) {
            return text.charAt(index);
        }

        @Nonnull
        @Override
        public CharSequence subSequence (int start, int end) {
            return new BadString(text.subSequence(start, end));
        }

        @Nonnull
        @Override
        public String toString () {
            return text.toString();
        }
    }
    public static BadString[] generateUniqueBad(int size) {
        final int numLetters = 3;
        IdentityObjectMap<BadString, Object> set = new IdentityObjectMap<>(size, 0.8f);
        char[] maker = new char[numLetters];
        for (int i = 0; set.size() < size; ) {
            for (int x = 0; x < 26 && set.size() < size; x++) {
                maker[0] = (char) ('a' + x);
                for (int y = 0; y < 26 && set.size() < size; y++) {
                    maker[1] = (char) ('a' + y);
                    for (int z = 0; z < 26 && set.size() < size; z++) {
                        maker[2] = (char) ('a' + z);
                        set.put(new BadString(String.valueOf(maker) + (i++)), null);
                    }
                }
            }
        }
        return set.keySet().toArray(new BadString[0]);
    }

    //takes 12794336100
    @Test
    public void testBadStringSetOld() {
        long start = System.nanoTime();
        ObjectSet set = new ObjectSet(51, 0.8f) {
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
                System.out.println("hash multiplier: " + Base.BASE16.unsigned(hashMultiplier) + " with new size " + newSize);
                System.out.println("total collisions: " + collisionTotal);
                System.out.println("longest pileup: " + longestPileup);
            }

            @Override
            public void clear () {
                System.out.println("hash multiplier: " + Base.BASE16.unsigned(hashMultiplier) + " with final size " + size);
                System.out.println("total collisions: " + collisionTotal);
                System.out.println("longest pileup: " + longestPileup);
                super.clear();
            }
        };
//        final int limit = (int)(Math.sqrt(LEN));
//        for (int x = -limit; x < limit; x+=2) {
//            for (int y = -limit; y < limit; y+=2) {
//                set.add(new Vector2(x, y));
//            }
//        }
        BadString[] words = generateUniqueBad(LEN);
        for (int i = 0; i < LEN; i++) {
            set.add(words[i]);
        }
        System.out.println(System.nanoTime() - start);
        set.clear();
    }

    // takes 11830103300 ns
    @Test
    public void testBadStringSetNew() {
        long start = System.nanoTime();
        ObjectSet set = new ObjectSet(51, 0.8f) {
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
//                hashMultiplier *= 0xF1357AEA2E62A9C5L;
                // ensures hashMultiplier is never too small, and is always odd
//                hashMultiplier |= 0x0000010000000001L;

                // we add a constant from Steele and Vigna, Computationally Easy, Spectrally Good Multipliers for Congruential
                // Pseudorandom Number Generators, times -8 to keep the bottom 3 bits the same every time.
                //361274435
//                hashMultiplier += 0xC3910C8D016B07D6L;//0x765428AE8CEAB1D8L;
                //211888218
                // we modify the hash multiplier by adding size twice (keeping the result odd), then multiply by a number
                // that Vigna and Steele considered optimal for a 64-bit MCG random number generator.
//                hashMultiplier *= 0xF1357AEA2E62A9C5L;//0x59E3779B97F4A7C1L;
//                hashMultiplier *= MathTools.GOLDEN_LONGS[size & 1023];
                hashMultiplier = (hashMultiplier + size + size) * 0xF1357AEA2E62A9C5L;
                
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
                System.out.println("hash multiplier: " + Base.BASE16.unsigned(hashMultiplier) + " with new size " + newSize);
                System.out.println("total collisions: " + collisionTotal);
                System.out.println("longest pileup: " + longestPileup);
            }

            @Override
            public void clear () {
                System.out.println("hash multiplier: " + Base.BASE16.unsigned(hashMultiplier) + " with final size " + size);
                System.out.println("total collisions: " + collisionTotal);
                System.out.println("longest pileup: " + longestPileup);
                super.clear();
            }
        };
//        final int limit = (int)(Math.sqrt(LEN));
//        for (int x = -limit; x < limit; x+=2) {
//            for (int y = -limit; y < limit; y+=2) {
//                set.add(new Vector2(x, y));
//            }
//        }
        BadString[] words = generateUniqueBad(LEN);
        for (int i = 0; i < LEN; i++) {
            set.add(words[i]);
        }
        System.out.println(System.nanoTime() - start);
        set.clear();
    }
}

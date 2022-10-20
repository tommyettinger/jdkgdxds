package com.github.tommyettinger.ds.test;

import com.github.tommyettinger.digital.Base;
import com.github.tommyettinger.ds.ObjectObjectMap;
import com.github.tommyettinger.ds.ObjectSet;
import org.junit.Ignore;
import org.junit.Test;

import javax.annotation.Nonnull;

public class PileupTest {
    public static final int LEN = 500000;
    public static String[] generateUniqueWords(int size) {
        final int numLetters = 4;
        ObjectSet<String> set = new ObjectSet<String>(size, 0.6f){
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

//    @Ignore // this test used to take much longer to run than the others here (over a minute; everything else is under a second).
    @Test
    public void testObjectSetOld() {
        final String[] words = generateUniqueWords(LEN);
        long start = System.nanoTime();
        // replicates old ObjectSet behavior, with added logging and the constant in place() changed
        ObjectSet set = new ObjectSet(51, 0.6f) {
            long collisionTotal = 0;
            int longestPileup = 0;
//            @Override
//            protected int place (Object item) {
//                return (int)(item.hashCode() * 0x9E3779B97F4A7C15L >>> shift);
//            }

            @Override
            protected int place (Object item) {
                return (int)(item.hashCode() * 0xD1B54A32D192ED03L >>> shift); // does extremely well???
            }

            @Override
            protected void addResize (@Nonnull Object key) {
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
        for (int i = 0; i < LEN; i++) {
            set.add(words[i]);
        }
        System.out.println(System.nanoTime() - start);
        set.clear();
    }
    @Test
    public void testObjectSetNew() {
        final String[] words = generateUniqueWords(LEN);
        long start = System.nanoTime();
        ObjectSet set = new ObjectSet(51, 0.6f) {
            long collisionTotal = 0;
            int longestPileup = 0;

            @Override
            protected void addResize (@Nonnull Object key) {
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

                // we modify the hash multiplier by multiplying it by a number that Vigna and Steele considered optimal
                // for a 64-bit MCG random number generator, XORed with 2 times size to randomize the low bits more.
                hashMultiplier *= size + size ^ 0xF1357AEA2E62A9C5L;

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
        for (int i = 0; i < LEN; i++) {
            set.add(words[i]);
        }
        System.out.println(System.nanoTime() - start);
        set.clear();
    }

    @Test
    public void testObjectQuadSet() {
        final String[] words = generateUniqueWords(LEN);
        long start = System.nanoTime();
        ObjectQuadSet set = new ObjectQuadSet(51, 0.6f) {
            long collisionTotal = 0;
            int longestPileup = 0;

            @Override
            protected void addResize (@Nonnull Object key) {
                Object[] keyTable = this.keyTable;
                for (int i = place(key), dist = 0; ; i = i + dist & mask) {
                    if (keyTable[i] == null) {
                        keyTable[i] = key;
                        return;
                    } else {
                        collisionTotal++;
                        longestPileup = Math.max(longestPileup, ++dist);
                    }
                }
            }

            @Override
            protected void resize (int newSize) {
                int oldCapacity = keyTable.length;
                threshold = (int)(newSize * loadFactor);
                mask = newSize - 1;
                shift = Long.numberOfLeadingZeros(mask);

                // we modify the hash multiplier by... basically it just needs to stay odd, and use 21 bits or fewer (for GWT reasons).
                // we incorporate the size in here (times 2, so it doesn't make the multiplier even) to randomize things more.
                hashMultiplier = (hashMultiplier + size + size ^ 0xC79E7B18) * 0x13C6EB & 0x1FFFFF;

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
        for (int i = 0; i < LEN; i++) {
            set.add(words[i]);
        }
        System.out.println(System.nanoTime() - start);
        set.clear();
    }

    @Test
    public void testObjectQuadSetExperimental() {
        final String[] words = generateUniqueWords(LEN);
        long start = System.nanoTime();
        ObjectQuadSet set = new ObjectQuadSet(51, 0.6f) {
            long collisionTotal = 0;
            int longestPileup = 0;

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
                for (int i = place(key), dist = 0; ; i = i + dist & mask) {
                    if (keyTable[i] == null) {
                        keyTable[i] = key;
                        return;
                    } else {
                        collisionTotal++;
                        longestPileup = Math.max(longestPileup, ++dist);
                    }
                }
            }

            @Override
            protected void resize (int newSize) {
                int oldCapacity = keyTable.length;
                threshold = (int)(newSize * loadFactor);
                mask = newSize - 1;
                shift = Long.numberOfLeadingZeros(mask);

                // We modify the hash multiplier by... basically it just needs to stay odd, and use 21 bits or fewer (for GWT reasons).
                // We incorporate the size in here to randomize things more. The multiplier seems to do a little better if it ends in the
                // hex digit 5 or D -- this makes it a valid power-of-two-modulus MCG multiplier, which might help a bit. We also always
                // set the bit 0x100000, so we know there will at least be some bits moved to the upper third or so.
                hashMultiplier = ((hashMultiplier + size << 3 ^ 0xC79E7B1D) * 0x13C6EB + 0xAF36D01E & 0xFFFFF) | 0x100000;

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
        for (int i = 0; i < LEN; i++) {
            set.add(words[i]);
        }
        System.out.println(System.nanoTime() - start);
        set.clear();
    }

    @Test
    public void testObjectQuadSetSimplePlace() {
        final String[] words = generateUniqueWords(LEN);
        long start = System.nanoTime();
        ObjectQuadSet set = new ObjectQuadSet(51, 0.6f) {
            long collisionTotal = 0;
            int longestPileup = 0;
            int hm = 0x13C6ED;

            @Override
            protected int place (Object item) {
                return item.hashCode() * hm & mask;
            }

            @Override
            protected void addResize (@Nonnull Object key) {
                Object[] keyTable = this.keyTable;
                for (int i = place(key), dist = 0; ; i = i + dist & mask) {
                    if (keyTable[i] == null) {
                        keyTable[i] = key;
                        return;
                    } else {
                        collisionTotal++;
                        longestPileup = Math.max(longestPileup, ++dist);
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

                collisionTotal = 0;
                longestPileup = 0;

                if (size > 0) {
                    for (int i = 0; i < oldCapacity; i++) {
                        Object key = oldKeyTable[i];
                        if (key != null) {addResize(key);}
                    }
                }
                System.out.println("hash multiplier: " + Base.BASE16.unsigned(hm) + " with new size " + newSize);
                System.out.println("total collisions: " + collisionTotal);
                System.out.println("longest pileup: " + longestPileup);
            }

            @Override
            public void clear () {
                System.out.println("hash multiplier: " + Base.BASE16.unsigned(hm) + " with final size " + size);
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

        for (int i = 0; i < LEN; i++) {
            set.add(words[i]);
        }
        System.out.println(System.nanoTime() - start);
        set.clear();
    }

    @Test
    public void testObjectSetIntPlace() {
        final String[] words = generateUniqueWords(LEN);
        long start = System.nanoTime();
        ObjectSet set = new ObjectSet(51, 0.6f) {
            long collisionTotal = 0;
            int longestPileup = 0;
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

                collisionTotal = 0;
                longestPileup = 0;

                if (size > 0) {
                    for (int i = 0; i < oldCapacity; i++) {
                        Object key = oldKeyTable[i];
                        if (key != null) {addResize(key);}
                    }
                }
                System.out.println("hash multiplier: " + Base.BASE16.unsigned(hm) + " with new size " + newSize);
                System.out.println("total collisions: " + collisionTotal);
                System.out.println("longest pileup: " + longestPileup);
            }

            @Override
            public void clear () {
                System.out.println("hash multiplier: " + Base.BASE16.unsigned(hm) + " with final size " + size);
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

        for (int i = 0; i < LEN; i++) {
            set.add(words[i]);
        }
        System.out.println(System.nanoTime() - start);
        set.clear();
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
//            return text.charAt(0) * text.charAt(1);
//            int h = 1;
//            for (int i = 0, n = text.length(); i < n; i++) {
//                h = h * 127 + text.charAt(i);
//            }
//            return h;
            return text.hashCode();
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
        ObjectObjectMap<BadString, Object> set = new ObjectObjectMap<BadString, Object>(size, 0.5f){
            @Override
            protected int place (@Nonnull Object item) {
                return (int)(item.hashCode() * 0xABCDEF0987654321L >>> shift);
            }
        };
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

    @Test
    public void testBadStringSetOld() {
        final BadString[] words = generateUniqueBad(LEN);
        long start = System.nanoTime();
        // replicates old ObjectSet behavior, with added logging
        ObjectSet set = new ObjectSet(51, 0.6f) {
            long collisionTotal = 0;
            int longestPileup = 0;
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
        for (int i = 0; i < LEN; i++) {
            set.add(words[i]);
        }
        System.out.println(System.nanoTime() - start);
        set.clear();
    }

    @Test
    public void testBadStringSetNew() {
        final BadString[] words = generateUniqueBad(LEN);
        long start = System.nanoTime();
        ObjectSet set = new ObjectSet(51, 0.6f) {
            long collisionTotal = 0;
            int longestPileup = 0;

            @Override
            protected void addResize (@Nonnull Object key) {
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
                // we modify the hash multiplier by multiplying it by a number that Vigna and Steele considered optimal
                // for a 64-bit MCG random number generator, XORed with 2 times size to randomize the low bits more.
//                hashMultiplier *= 0xF1357AEA2E62A9C5L;//0x59E3779B97F4A7C1L;
//                hashMultiplier *= MathTools.GOLDEN_LONGS[size & 1023];
//                hashMultiplier *= size + size ^ 0xF1357AEA2E62A9C5L;
                hashMultiplier *= size + size ^ 0xF1357AEA2E62A9C5L;

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
        for (int i = 0; i < LEN; i++) {
            set.add(words[i]);
        }
        System.out.println(System.nanoTime() - start);
        set.clear();
    }


    @Test
    public void testBadStringQuadSet() {
        final BadString[] words = generateUniqueBad(LEN);
        long start = System.nanoTime();
        ObjectQuadSet set = new ObjectQuadSet(51, 0.6f) {
            long collisionTotal = 0;
            int longestPileup = 0;

            @Override
            protected void addResize (@Nonnull Object key) {
                Object[] keyTable = this.keyTable;
                for (int i = place(key), dist = 0; ; i = i + dist & mask) {
                    if (keyTable[i] == null) {
                        keyTable[i] = key;
                        return;
                    } else {
                        collisionTotal++;
                        longestPileup = Math.max(longestPileup, ++dist);
                    }
                }
            }

            @Override
            protected void resize (int newSize) {
                int oldCapacity = keyTable.length;
                threshold = (int)(newSize * loadFactor);
                mask = newSize - 1;
                shift = Long.numberOfLeadingZeros(mask);

                // We modify the hash multiplier by... basically it just needs to stay odd, and use 21 bits or fewer (for GWT reasons).
                // We incorporate the size in here to randomize things more. The multiplier seems to do a little better if it ends in the
                // hex digit 5 or D -- this makes it a valid power-of-two-modulus MCG multiplier, which might help a bit. We also always
                // set the bit 0x100000, so we know there will at least be some bits moved to the upper third or so.
                hashMultiplier = ((hashMultiplier + size << 3 ^ 0xC79E7B1D) * 0x13C6EB + 0xAF36D01E & 0xFFFFF) | 0x100000;

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
        for (int i = 0; i < LEN; i++) {
            set.add(words[i]);
        }
        System.out.println(System.nanoTime() - start);
        set.clear();
    }
    /**
     * Thanks to Jonathan M, <a href="https://stackoverflow.com/a/20591835">on Stack Overflow</a>.
     * @param size
     * @return
     */
    public static Vector2[] generateVectorSpiral(int size) {
        Vector2[] result = new Vector2[size];
        for (int root = 0, index = 0; ; ++root) {
            for (int limit = index + root + root; index <= limit;) {
                final int sign = -(root & 1);
                final int big = (root * (root + 1)) - index << 1;
                final int y = ((root + 1 >> 1) + sign ^ sign) + ((sign ^ sign + Math.min(big, 0)) >> 1);
                final int x = ((root + 1 >> 1) + sign ^ sign) - ((sign ^ sign + Math.max(big, 0)) >> 1);
                result[index] = new Vector2(x, y);
                if(++index >= size) return result;
            }
        }
    }


    @Test
    public void testVector2SetOld() {
        final Vector2[] words = generateVectorSpiral(LEN);
        long start = System.nanoTime();
        // replicates old ObjectSet behavior, with added logging
        ObjectSet set = new ObjectSet(51, 0.6f) {
            long collisionTotal = 0;
            int longestPileup = 0;
            @Override
            protected int place (Object item) {
                return (int)(item.hashCode() * 0xD1B54A32D192ED03L >>> shift);
//                return (int)(item.hashCode() * 0x9E3779B97F4A7C15L >>> shift);
            }

            @Override
            protected void addResize (@Nonnull Object key) {
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
        for (int i = 0; i < LEN; i++) {
            set.add(words[i]);
        }
        System.out.println(System.nanoTime() - start);
        set.clear();
    }

    @Test
    public void testVector2SetNew() {
        final Vector2[] words = generateVectorSpiral(LEN);
        long start = System.nanoTime();
        ObjectSet set = new ObjectSet(51, 0.6f) {
            long collisionTotal = 0;
            int longestPileup = 0;

            @Override
            protected void addResize (@Nonnull Object key) {
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
                // we modify the hash multiplier by multiplying it by a number that Vigna and Steele considered optimal
                // for a 64-bit MCG random number generator, XORed with 2 times size to randomize the low bits more.
//                hashMultiplier *= 0xF1357AEA2E62A9C5L;//0x59E3779B97F4A7C1L;
//                hashMultiplier *= MathTools.GOLDEN_LONGS[size & 1023];
//                hashMultiplier *= size + size ^ 0xF1357AEA2E62A9C5L;
                hashMultiplier *= size + size ^ 0xF1357AEA2E62A9C5L;

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
        for (int i = 0; i < LEN; i++) {
            set.add(words[i]);
        }
        System.out.println(System.nanoTime() - start);
        set.clear();
    }

    @Test
    public void testVector2QuadSet() {
        final Vector2[] words = generateVectorSpiral(LEN);
        long start = System.nanoTime();
        ObjectQuadSet set = new ObjectQuadSet(51, 0.6f) {
            long collisionTotal = 0;
            int longestPileup = 0;

            @Override
            protected void addResize (@Nonnull Object key) {
                Object[] keyTable = this.keyTable;
                for (int i = place(key), dist = 0; ; i = i + dist & mask) {
                    if (keyTable[i] == null) {
                        keyTable[i] = key;
                        return;
                    } else {
                        collisionTotal++;
                        longestPileup = Math.max(longestPileup, ++dist);
                    }
                }
            }

            @Override
            protected void resize (int newSize) {
                int oldCapacity = keyTable.length;
                threshold = (int)(newSize * loadFactor);
                mask = newSize - 1;
                shift = Long.numberOfLeadingZeros(mask);

                // We modify the hash multiplier by... basically it just needs to stay odd, and use 21 bits or fewer (for GWT reasons).
                // We incorporate the size in here to randomize things more. The multiplier seems to do a little better if it ends in the
                // hex digit 5 or D -- this makes it a valid power-of-two-modulus MCG multiplier, which might help a bit. We also always
                // set the bit 0x100000, so we know there will at least be some bits moved to the upper third or so.
                hashMultiplier = ((hashMultiplier + size << 3 ^ 0xC79E7B1D) * 0x13C6EB + 0xAF36D01E & 0xFFFFF) | 0x100000;

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
        for (int i = 0; i < LEN; i++) {
            set.add(words[i]);
        }
        System.out.println(System.nanoTime() - start);
        set.clear();
    }

}

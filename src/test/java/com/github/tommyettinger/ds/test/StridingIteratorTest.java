package com.github.tommyettinger.ds.test;

import com.github.tommyettinger.ds.*;
import com.github.tommyettinger.ds.support.iterator.*;
import org.junit.Assert;
import org.junit.Test;

public class StridingIteratorTest {
    @Test
    public void testObjStridingIterator() {
        ObjectList<String> data = ObjectList.with(IteratorTest.strings);
        StridingIterator<String> stri = new StridingIterator<>(data.iterator(), 1, 2);
        ObjectList<String> next = new ObjectList<>();
        next.addAll(stri);
        Assert.assertEquals("beta", next.get(0));
        Assert.assertEquals("delta", next.get(1));
        stri.set(data.iterator(), 0, 3);
        next.clear();
        next.addAll(stri);
        Assert.assertEquals("alpha", next.get(0));
        Assert.assertEquals("delta", next.get(1));
    }
    //		'Α', 'Β', 'Γ', 'Δ', 'Ε', 'Ζ', 'Η', 'Θ', 'Ι', 'Κ', 'Λ', 'Μ', 'Ν', 'Ξ', 'Ο', 'Π', 'Ρ', 'Σ', 'Τ', 'Υ', 'Φ', 'Χ', 'Ψ', 'Ω'
    @Test
    public void testStridingLongIterator() {
        LongList data = LongList.with(IteratorTest.longs);
        StridingLongIterator stri = new StridingLongIterator(data.iterator(), 1, 2);
        LongList next = new LongList();
        next.addAll(stri);
        Assert.assertEquals('Β', next.get(0));
        Assert.assertEquals('Δ', next.get(1));
        stri.set(data.iterator(), 0, 3);
        next.clear();
        next.addAll(stri);
        Assert.assertEquals('Α', next.get(0));
        Assert.assertEquals('Δ', next.get(1));
    }
//
//    @Test
//    public void testStridingIntIterator() {
//        IntList data = IntList.with(IteratorTest.ints);
//        StridingIntIterator fil =
//                new StridingIntIterator(data.iterator(), (int s) -> s % 50 == 25);
//        IntList next = new IntList();
//        next.addAll(fil);
//        Assert.assertEquals(1, next.size());
//        fil.set(data.iterator(), (int s) -> s % 10 == 1);
//        next.clear();
//        next.addAll(fil);
//        Assert.assertEquals(2, next.size());
//    }
//
//    @Test
//    public void testStridingShortIterator() {
//        ShortList data = ShortList.with(IteratorTest.shorts);
//        StridingShortIterator fil =
//                new StridingShortIterator(data.iterator(), (short s) -> s % 50 == 25);
//        ShortList next = new ShortList();
//        next.addAll(fil);
//        Assert.assertEquals(1, next.size());
//        fil.set(data.iterator(), (short s) -> s % 10 == 1);
//        next.clear();
//        next.addAll(fil);
//        Assert.assertEquals(2, next.size());
//    }
//
//    @Test
//    public void testStridingByteIterator() {
//        ByteList data = ByteList.with(IteratorTest.bytes);
//        StridingByteIterator fil =
//                new StridingByteIterator(data.iterator(), (byte s) -> s % 50 == 13);
//        ByteList next = new ByteList();
//        next.addAll(fil);
//        Assert.assertEquals(2, next.size());
//        fil.set(data.iterator(), (byte s) -> s % 10 == 0);
//        next.clear();
//        next.addAll(fil);
//        Assert.assertEquals(3, next.size());
//    }

    @Test
    public void testStridingFloatIterator() {
        FloatList data = FloatList.with(IteratorTest.floats);
        StridingFloatIterator stri = new StridingFloatIterator(data.iterator(), 1, 2);
        FloatList next = new FloatList();
        next.addAll(stri);
        Assert.assertEquals('Β', next.get(0), 0);
        Assert.assertEquals('Δ', next.get(1), 0);
        stri.set(data.iterator(), 0, 3);
        next.clear();
        next.addAll(stri);
        Assert.assertEquals('Α', next.get(0), 0);
        Assert.assertEquals('Δ', next.get(1), 0);
    }

    @Test
    public void testStridingDoubleIterator() {
        DoubleList data = DoubleList.with(IteratorTest.doubles);
        StridingDoubleIterator stri = new StridingDoubleIterator(data.iterator(), 1, 2);
        DoubleList next = new DoubleList();
        next.addAll(stri);
        Assert.assertEquals('Β', next.get(0), 0);
        Assert.assertEquals('Δ', next.get(1), 0);
        stri.set(data.iterator(), 0, 3);
        next.clear();
        next.addAll(stri);
        Assert.assertEquals('Α', next.get(0), 0);
        Assert.assertEquals('Δ', next.get(1), 0);
    }
//
//    @Test
//    public void testStridingCharIterator() {
//        CharList data = CharList.with(IteratorTest.chars);
//        StridingCharIterator fil =
//                new StridingCharIterator(data.iterator(), (char s) -> s % 50 == 25);
//        CharList next = new CharList();
//        next.addAll(fil);
//        Assert.assertEquals(1, next.size());
//        fil.set(data.iterator(), (char s) -> s % 10 == 1);
//        next.clear();
//        next.addAll(fil);
//        Assert.assertEquals(2, next.size());
//    }
//
//    @Test
//    public void testStridingBooleanIterator() {
//        BooleanList data = BooleanList.with(IteratorTest.booleans);
//        StridingBooleanIterator fil =
//                new StridingBooleanIterator(data.iterator(), (boolean s) -> s);
//        BooleanList next = new BooleanList();
//        next.addAll(fil);
//        Assert.assertEquals(9, next.size());
//        fil.set(data.iterator(), (boolean s) -> !s);
//        next.clear();
//        next.addAll(fil);
//        Assert.assertEquals(7, next.size());
//    }
}

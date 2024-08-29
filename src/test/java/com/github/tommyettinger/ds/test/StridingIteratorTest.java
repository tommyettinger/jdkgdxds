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
//
//    @Test
//    public void testStridingLongIterator() {
//        LongList data = LongList.with(IteratorTest.longs);
//        StridingLongIterator fil =
//                new StridingLongIterator(data.iterator(), (long s) -> s % 50 == 25);
//        LongList next = new LongList();
//        next.addAll(fil);
//        Assert.assertEquals(1, next.size());
//        fil.set(data.iterator(), (long s) -> s % 10 == 1);
//        next.clear();
//        next.addAll(fil);
//        Assert.assertEquals(2, next.size());
//    }
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
//
//    @Test
//    public void testStridingFloatIterator() {
//        FloatList data = FloatList.with(IteratorTest.floats);
//        StridingFloatIterator fil =
//                new StridingFloatIterator(data.iterator(), (float s) -> s % 50 == 25);
//        FloatList next = new FloatList();
//        next.addAll(fil);
//        Assert.assertEquals(1, next.size());
//        fil.set(data.iterator(), (float s) -> s % 10 == 1);
//        next.clear();
//        next.addAll(fil);
//        Assert.assertEquals(2, next.size());
//    }
//
//    @Test
//    public void testStridingDoubleIterator() {
//        DoubleList data = DoubleList.with(IteratorTest.doubles);
//        StridingDoubleIterator fil =
//                new StridingDoubleIterator(data.iterator(), (double s) -> s % 50 == 25);
//        DoubleList next = new DoubleList();
//        next.addAll(fil);
//        Assert.assertEquals(1, next.size());
//        fil.set(data.iterator(), (double s) -> s % 10 == 1);
//        next.clear();
//        next.addAll(fil);
//        Assert.assertEquals(2, next.size());
//    }
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

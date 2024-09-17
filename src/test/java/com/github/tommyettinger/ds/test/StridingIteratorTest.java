package com.github.tommyettinger.ds.test;

import com.github.tommyettinger.ds.*;
import com.github.tommyettinger.ds.support.util.*;
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

    @Test
    public void testStridingIntIterator() {
        IntList data = IntList.with(IteratorTest.ints);
        StridingIntIterator stri = new StridingIntIterator(data.iterator(), 1, 2);
        IntList next = new IntList();
        next.addAll(stri);
        Assert.assertEquals('Β', next.get(0));
        Assert.assertEquals('Δ', next.get(1));
        stri.set(data.iterator(), 0, 3);
        next.clear();
        next.addAll(stri);
        Assert.assertEquals('Α', next.get(0));
        Assert.assertEquals('Δ', next.get(1));
    }
    
    @Test
    public void testStridingShortIterator() {
        ShortList data = ShortList.with(IteratorTest.shorts);
        StridingShortIterator stri = new StridingShortIterator(data.iterator(), 1, 2);
        ShortList next = new ShortList();
        next.addAll(stri);
        Assert.assertEquals('Β', next.get(0));
        Assert.assertEquals('Δ', next.get(1));
        stri.set(data.iterator(), 0, 3);
        next.clear();
        next.addAll(stri);
        Assert.assertEquals('Α', next.get(0));
        Assert.assertEquals('Δ', next.get(1));
    }

    //1, 0, -1, 2, -2, 3, -3, 11, 10, -11, 12, -12, 13, -13, 111, 110, -111, 112, -112, 113, -113
    @Test
    public void testStridingByteIterator() {
        ByteList data = ByteList.with(IteratorTest.bytes);
        StridingByteIterator stri = new StridingByteIterator(data.iterator(), 1, 2);
        ByteList next = new ByteList();
        next.addAll(stri);
        Assert.assertEquals(0, next.get(0));
        Assert.assertEquals(2, next.get(1));
        stri.set(data.iterator(), 0, 3);
        next.clear();
        next.addAll(stri);
        Assert.assertEquals(1, next.get(0));
        Assert.assertEquals(2, next.get(1));
    }

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

    @Test
    public void testStridingCharIterator() {
        CharList data = CharList.with(IteratorTest.chars);
        StridingCharIterator stri = new StridingCharIterator(data.iterator(), 1, 2);
        CharList next = new CharList();
        next.addAll(stri);
        Assert.assertEquals('Β', next.get(0));
        Assert.assertEquals('Δ', next.get(1));
        stri.set(data.iterator(), 0, 3);
        next.clear();
        next.addAll(stri);
        Assert.assertEquals('Α', next.get(0));
        Assert.assertEquals('Δ', next.get(1));
    }
//true, false, false, false, true, false, true, true, false, true, true, false, true, true, false, true

    @Test
    public void testStridingBooleanIterator() {
        BooleanList data = BooleanList.with(IteratorTest.booleans);
        StridingBooleanIterator stri = new StridingBooleanIterator(data.iterator(), 1, 2);
        BooleanList next = new BooleanList();
        next.addAll(stri);
        Assert.assertEquals(false, next.get(0));
        Assert.assertEquals(false, next.get(1));
        Assert.assertEquals(true, next.get(3));
        stri.set(data.iterator(), 0, 3);
        next.clear();
        next.addAll(stri);
        Assert.assertEquals(true, next.get(0));
        Assert.assertEquals(false, next.get(1));
        Assert.assertEquals(true, next.get(2));
    }
}

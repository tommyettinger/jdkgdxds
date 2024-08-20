package com.github.tommyettinger.ds.test;

import com.github.tommyettinger.ds.IntList;
import com.github.tommyettinger.ds.LongList;
import com.github.tommyettinger.ds.ObjectList;
import com.github.tommyettinger.ds.support.iterator.FilteringIntIterator;
import com.github.tommyettinger.ds.support.iterator.FilteringIterator;
import com.github.tommyettinger.ds.support.iterator.FilteringLongIterator;
import org.junit.Assert;
import org.junit.Test;

public class FilteringIteratorTest {
    @Test
    public void testObjFilteringIterator() {
        ObjectList<String> data = ObjectList.with(IteratorTest.strings);
        FilteringIterator<String> fil =
                new FilteringIterator<>(data.iterator(), (String s) -> s.length() == 2);
        ObjectList<String> next = new ObjectList<>();
        next.addAll(fil);
        Assert.assertEquals(4, next.size());
        fil.set(data.iterator(), (String s) -> s.charAt(0) == 'p');
        next.clear();
        next.addAll(fil);
        Assert.assertEquals(3, next.size());
    }

    @Test
    public void testFilteringLongIterator() {
        LongList data = LongList.with(IteratorTest.longs);
        FilteringLongIterator fil =
                new FilteringLongIterator(data.iterator(), (long s) -> s % 50 == 25);
        LongList next = new LongList();
        next.addAll(fil);
        Assert.assertEquals(1, next.size());
        fil.set(data.iterator(), (long s) -> s % 10 == 1);
        next.clear();
        next.addAll(fil);
        Assert.assertEquals(2, next.size());
    }

    @Test
    public void testFilteringIntIterator() {
        IntList data = IntList.with(IteratorTest.ints);
        FilteringIntIterator fil =
                new FilteringIntIterator(data.iterator(), (int s) -> s % 50 == 25);
        IntList next = new IntList();
        next.addAll(fil);
        Assert.assertEquals(1, next.size());
        fil.set(data.iterator(), (int s) -> s % 10 == 1);
        next.clear();
        next.addAll(fil);
        Assert.assertEquals(2, next.size());
    }
}

package com.github.tommyettinger.ds.test;

import com.github.tommyettinger.ds.CaseInsensitiveMap;
import com.github.tommyettinger.ds.CaseInsensitiveOrderedMap;
import com.github.tommyettinger.ds.CaseInsensitiveOrderedSet;
import com.github.tommyettinger.ds.CaseInsensitiveSet;
import com.github.tommyettinger.ds.ObjectList;
import com.github.tommyettinger.ds.support.DistinctRandom;
import com.github.tommyettinger.ds.support.FourWheelRandom;
import com.github.tommyettinger.ds.support.LaserRandom;
import com.github.tommyettinger.ds.support.StrangerRandom;
import com.github.tommyettinger.ds.support.TricycleRandom;
import com.github.tommyettinger.ds.support.TrimRandom;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

/**
 * Created by Tommy Ettinger on 10/26/2020.
 */
public class EnhancedRandomTest {
	@Test
	public void testDistinctPrevious() {
		DistinctRandom random = new DistinctRandom(123L);
		long n0 = random.nextLong();
		long n1 = random.nextLong();
		long n2 = random.nextLong();
		long p1 = random.previousLong();
		long p0 = random.previousLong();
		Assert.assertEquals(n0, p0);
		Assert.assertEquals(n1, p1);
	}
	@Test
	public void testLaserPrevious() {
		DistinctRandom random = new DistinctRandom(123L);
		long n0 = random.nextLong();
		long n1 = random.nextLong();
		long n2 = random.nextLong();
		long p1 = random.previousLong();
		long p0 = random.previousLong();
		Assert.assertEquals(n0, p0);
		Assert.assertEquals(n1, p1);
	}
	@Test
	public void testTricyclePrevious() {
		TricycleRandom random = new TricycleRandom(123L);
		long n0 = random.nextLong();
		long n1 = random.nextLong();
		long n2 = random.nextLong();
		long p1 = random.previousLong();
		long p0 = random.previousLong();
		Assert.assertEquals(n0, p0);
		Assert.assertEquals(n1, p1);
	}

	@Test
	public void testFourWheelPrevious() {
		FourWheelRandom random = new FourWheelRandom(123L);
		long n0 = random.nextLong();
		long n1 = random.nextLong();
		long n2 = random.nextLong();
		long p1 = random.previousLong();
		long p0 = random.previousLong();
		Assert.assertEquals(n0, p0);
		Assert.assertEquals(n1, p1);
	}

	@Test
	public void testTrimPrevious() {
		TrimRandom random = new TrimRandom(0L);
		long n0 = random.nextLong();
		long n1 = random.nextLong();
		long n2 = random.nextLong();
		long p1 = random.previousLong();
		long p0 = random.previousLong();
		Assert.assertEquals(n0, p0);
		Assert.assertEquals(n1, p1);
	}

	@Test
	public void testStrangerPrevious() {
		StrangerRandom random = new StrangerRandom(0L);
		long n0 = random.nextLong();
		long n1 = random.nextLong();
		long n2 = random.nextLong();
		long p1 = random.previousLong();
		long p0 = random.previousLong();
		Assert.assertEquals(n0, p0);
		Assert.assertEquals(n1, p1);
	}

	@Test
	public void testDistinctBoundedLong() {
		final boolean PRINTING = false;
		DistinctRandom random = new DistinctRandom(123L);
		long inner = -0x7000000000000000L, outer = 0x7000000000000000L;
		for (int i = 0; i < 1024; i++) {
			long bounded = random.nextLong(inner, outer);
			if(PRINTING) System.out.println(bounded);
			Assert.assertTrue(bounded >= inner && bounded < outer);
		}
		for (int i = 0; i < 1024; i++) {
			long bounded = random.nextSignedLong(inner, outer);
			if(PRINTING) System.out.println(bounded);
			Assert.assertTrue(bounded >= inner && bounded < outer);
		}
	}

	@Test
	public void testDistinctBoundedInt() {
		final boolean PRINTING = false;
		DistinctRandom random = new DistinctRandom(123L);
		int inner = -1879048192, outer = 1879048192;
		for (int i = 0; i < 1024; i++) {
			int bounded = random.nextInt(inner, outer);
			if(PRINTING) System.out.println(bounded);
			Assert.assertTrue(bounded >= inner && bounded < outer);
		}
		for (int i = 0; i < 1024; i++) {
			int bounded = random.nextSignedInt(inner, outer);
			if(PRINTING) System.out.println(bounded);
			Assert.assertTrue(bounded >= inner && bounded < outer);
		}
		for (int i = 0; i < 1024; i++) {
			int bounded = random.nextInt(outer, inner);
			// this will look strange, but it prints outer 1024 times.
			if(PRINTING) System.out.println(bounded);
			Assert.assertEquals(bounded, outer);
		}
	}

	@Test
	public void testLaserBoundedInt() {
		final boolean PRINTING = false;
		LaserRandom random = new LaserRandom(123L);
		int inner = -1879048192, outer = 1879048192;
		for (int i = 0; i < 1024; i++) {
			int bounded = random.nextInt(inner, outer);
			if(PRINTING) System.out.println(bounded);
			Assert.assertTrue(bounded >= inner && bounded < outer);
		}
		for (int i = 0; i < 1024; i++) {
			int bounded = random.nextSignedInt(inner, outer);
			if(PRINTING) System.out.println(bounded);
			Assert.assertTrue(bounded >= inner && bounded < outer);
		}
		for (int i = 0; i < 1024; i++) {
			int bounded = random.nextInt(outer, inner);
			// this will look strange, but it prints outer 1024 times.
			if(PRINTING) System.out.println(bounded);
			Assert.assertEquals(bounded, outer);
		}
	}
}

package com.github.tommyettinger.ds.test;

import com.github.tommyettinger.ds.support.ChopRandom;
import com.github.tommyettinger.ds.support.DistinctRandom;
import com.github.tommyettinger.ds.support.FourWheelRandom;
import com.github.tommyettinger.ds.support.LaserRandom;
import com.github.tommyettinger.ds.support.MizuchiRandom;
import com.github.tommyettinger.ds.support.RomuTrioRandom;
import com.github.tommyettinger.ds.support.StrangerRandom;
import com.github.tommyettinger.ds.support.TricycleRandom;
import com.github.tommyettinger.ds.support.TrimRandom;
import com.github.tommyettinger.ds.support.WrapperRandom;
import com.github.tommyettinger.ds.support.Xoshiro256StarStarRandom;
import org.junit.Assert;
import org.junit.Test;

import java.util.BitSet;

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
		long n3 = random.nextLong();
		long p2 = random.previousLong();
		long p1 = random.previousLong();
		long p0 = random.previousLong();
		Assert.assertEquals(n0, p0);
		Assert.assertEquals(n1, p1);
		Assert.assertEquals(n2, p2);
	}
	@Test
	public void testLaserPrevious() {
		LaserRandom random = new LaserRandom(123L);
		long n0 = random.nextLong();
		long n1 = random.nextLong();
		long n2 = random.nextLong();
		long n3 = random.nextLong();
		long p2 = random.previousLong();
		long p1 = random.previousLong();
		long p0 = random.previousLong();
		Assert.assertEquals(n0, p0);
		Assert.assertEquals(n1, p1);
		Assert.assertEquals(n2, p2);
	}
	@Test
	public void testTricyclePrevious() {
		TricycleRandom random = new TricycleRandom(123L);
		long n0 = random.nextLong();
		long n1 = random.nextLong();
		long n2 = random.nextLong();
		long n3 = random.nextLong();
		long p2 = random.previousLong();
		long p1 = random.previousLong();
		long p0 = random.previousLong();
		Assert.assertEquals(n0, p0);
		Assert.assertEquals(n1, p1);
		Assert.assertEquals(n2, p2);
	}

	@Test
	public void testFourWheelPrevious() {
		FourWheelRandom random = new FourWheelRandom(123L);
		long n0 = random.nextLong();
		long n1 = random.nextLong();
		long n2 = random.nextLong();
		long n3 = random.nextLong();
		long p2 = random.previousLong();
		long p1 = random.previousLong();
		long p0 = random.previousLong();
		Assert.assertEquals(n0, p0);
		Assert.assertEquals(n1, p1);
		Assert.assertEquals(n2, p2);
	}

	@Test
	public void testTrimPrevious() {
		TrimRandom random = new TrimRandom(0L);
		long n0 = random.nextLong();
		long n1 = random.nextLong();
		long n2 = random.nextLong();
		long n3 = random.nextLong();
		long p2 = random.previousLong();
		long p1 = random.previousLong();
		long p0 = random.previousLong();
		Assert.assertEquals(n0, p0);
		Assert.assertEquals(n1, p1);
		Assert.assertEquals(n2, p2);
	}

	@Test
	public void testChopPrevious() {
		ChopRandom random = new ChopRandom(0L);
		long n0 = random.nextLong();
		long n1 = random.nextLong();
		long n2 = random.nextLong();
		long n3 = random.nextLong();
		long p2 = random.previousLong();
		long p1 = random.previousLong();
		long p0 = random.previousLong();
		Assert.assertEquals(n0, p0);
		Assert.assertEquals(n1, p1);
		Assert.assertEquals(n2, p2);
	}

	@Test
	public void testMizuchiPrevious() {
		MizuchiRandom random = new MizuchiRandom(0L);
		long n0 = random.nextLong();
		long n1 = random.nextLong();
		long n2 = random.nextLong();
		long n3 = random.nextLong();
		long p2 = random.previousLong();
		long p1 = random.previousLong();
		long p0 = random.previousLong();
		Assert.assertEquals(n0, p0);
		Assert.assertEquals(n1, p1);
		Assert.assertEquals(n2, p2);
	}

	@Test
	public void testStrangerPrevious() {
		StrangerRandom random = new StrangerRandom(0L);
		long n0 = random.nextLong();
		long n1 = random.nextLong();
		long n2 = random.nextLong();
		long n3 = random.nextLong();
		long p2 = random.previousLong();
		long p1 = random.previousLong();
		long p0 = random.previousLong();
		Assert.assertEquals(n0, p0);
		Assert.assertEquals(n1, p1);
		Assert.assertEquals(n2, p2);
	}

	@Test
	public void testXoshiro256StarStarPrevious() {
		Xoshiro256StarStarRandom random = new Xoshiro256StarStarRandom(0L);
		long n0 = random.nextLong();
		long n1 = random.nextLong();
		long n2 = random.nextLong();
		long n3 = random.nextLong();
		long p2 = random.previousLong();
		long p1 = random.previousLong();
		long p0 = random.previousLong();
		Assert.assertEquals(n0, p0);
		Assert.assertEquals(n1, p1);
		Assert.assertEquals(n2, p2);
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

	@Test
	public void testRomuTrioBoundedInt() {
		final boolean PRINTING = false;
		RomuTrioRandom random = new RomuTrioRandom(123L);
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
	public void testWrapperRandom() {
		WrapperRandom random = new WrapperRandom(new FourWheelRandom(123456789, 0xFA7BAB1E5L, 0xB0BAFE77L, 0x1234123412341234L));
		FourWheelRandom fwr = new FourWheelRandom(123456789, 0xFA7BAB1E5L, 0xB0BAFE77L, 0x1234123412341234L);
		random.nextLong();
		fwr.nextLong();
		Assert.assertEquals(random.nextLong(), fwr.nextLong());
		random.rng = new FourWheelRandom(123L);
		fwr.setSeed(123L);
		random.nextLong();
		fwr.nextLong();
		Assert.assertEquals(random.nextLong(), fwr.nextLong());
		random = new WrapperRandom(new FourWheelRandom(1234L), 12345L);
		fwr.setSeed(12345L);
		random.nextLong();
		fwr.nextLong();
		Assert.assertEquals(random.nextLong(), fwr.nextLong());
	}

	public static void main(String[] args){
		System.out.println("\nChecking if all floats [0.0,1.0) can be generated by nextFloat()...");
		BitSet bits = new BitSet(0x800000);
		bits.set(0, 0x800000);
		LaserRandom gen = new LaserRandom(1L);
		for (int i = 0; i < 0x10000000; i++) {
			bits.clear((int)((Float.intBitsToFloat(0x3F800000 | gen.next(23)) - 1f) * 0x800000));
		}
		System.out.println("Positions remaining: " + bits.cardinality());

	}
}

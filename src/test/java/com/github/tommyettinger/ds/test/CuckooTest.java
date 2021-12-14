package com.github.tommyettinger.ds.test;

import org.junit.Test;

public class CuckooTest {
	// Expected to fail with an OutOfMemoryError.
	@Test(expected = OutOfMemoryError.class)
	public void failingCuckooTest(){
		CuckooObjectMap<String, Object> map = new CuckooObjectMap<>();
		String[] problems = ("21oo 0oq1 0opP 0ooo 0pPo 21pP 21q1 1Poo 1Pq1 1PpP 0q31 0pR1 0q2P 0q1o 232P 231o 2331 0pQP 22QP 22Po 22R1 1QQP 1R1o 1QR1 1R2P 1R31 1QPo 1Qup 1S7p 0r8Q 0r7p 0r92 23X2 2492 248Q 247p 22vQ 22up 1S92 1S8Q 23WQ 23Vp 22w2 1QvQ 1Qw2 1RVp 1RWQ 1RX2 0qX2".split(" "));
		System.out.println("Trying to enter " + problems.length + " String keys into a CuckooObjectMap.");
		for (int i = 0; i < problems.length; i++) {
			System.out.println("Entered " + i + " keys successfully.");
			map.put(problems[i], null);
		}
		System.out.println("Unexpectedly succeeded; finished map has size: " + map.size);
	}
}

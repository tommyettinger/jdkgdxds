package com.github.tommyettinger.ds.test;

public class CuckooTest {
	// Expected to fail with an OutOfMemoryError.
	public static void main(String[] args){
		CuckooObjectMap<String, Object> map = new CuckooObjectMap<>();
		String[] problems = ("21oo 0oq1 0opP 0ooo 0pPo 21pP 21q1 1Poo 1Pq1 1PpP 0q31 0pR1 0q2P 0q1o 232P 231o 2331 0pQP 22QP 22Po 22R1 1QQP 1R1o 1QR1 1R2P 1R31 1QPo 1Qup 1S7p 0r8Q 0r7p 0r92 23X2 2492 248Q 247p 22vQ 22up 1S92 1S8Q 23WQ 23Vp 22w2 1QvQ 1Qw2 1RVp 1RWQ 1RX2 0qX2".split(" "));
		for (int i = 0; i < problems.length; i++) {
			System.out.println("Entered " + i + " keys successfully.");
			map.put(problems[i], null);
		}
		System.out.println(map.size);
	}
}

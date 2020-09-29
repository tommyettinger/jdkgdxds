package com.github.tommyettinger.ds.test;

import com.github.tommyettinger.ds.LaserRandom;

import java.util.Random;
import java.util.SplittableRandom;

/**
 * Created by Tommy Ettinger on 9/23/2020.
 */
public class LaserRandomTest {
	public static void main(String[] args) {
		int upperBound = 79;
		System.out.println("Consecutive seeds:");
		System.out.println("\nRandom");
		for(int i = 0; i < 50; i++) System.out.print(new java.util.Random(i).nextInt(upperBound) + " ");
		System.out.println("\nSplittableRandom");
		for(int i = 0; i < 50; i++) System.out.print(new java.util.SplittableRandom(i).nextInt(upperBound) + " ");
		System.out.println("\nLaserRandom");
		for(int i = 0; i < 50; i++) System.out.print(new LaserRandom(i).nextInt(upperBound) + " ");

		System.out.println("\n\nNormal usage");
		System.out.println("\nRandom");
		Random jur = new java.util.Random(10);
		for(int i = 0; i < 50; i++) System.out.print(jur.nextInt(upperBound) + " ");
		System.out.println("\nSplittableRandom");
		SplittableRandom jusr = new java.util.SplittableRandom(10);
		for(int i = 0; i < 50; i++) System.out.print(jusr.nextInt(upperBound) + " ");
		System.out.println("\nLaserRandom");
		LaserRandom lr = new LaserRandom(10);
		for(int i = 0; i < 50; i++) System.out.print(lr.nextInt(upperBound) + " ");

		System.out.println("\n\nWeird usage");
		System.out.println("\nRandom");
		jur = new java.util.Random(50);
		jusr = new java.util.SplittableRandom(50);
		lr = new LaserRandom(50);
		for(int i = 0; i < 50; i++) System.out.print((((jur.nextInt() & 0xFFFFFFFFL) * -2 >> 32) - (-2 >> 31)) + " ");
		System.out.println("\nSplittableRandom");
		for(int i = 0; i < 50; i++) System.out.print((((jusr.nextInt() & 0xFFFFFFFFL) * -2 >> 32) - (-2 >> 31)) + " ");
		System.out.println("\nLaserRandom");
		for(int i = 0; i < 50; i++) System.out.print(lr.nextSignedLong(-2) + " ");

		System.out.println("\n\nRepeat count:");
		jur = new java.util.Random(100);
		jusr = new java.util.SplittableRandom(100);
		lr = new LaserRandom(100);
		int prev, count;
		prev = -1;
		count = 0;
		for (int i = upperBound * 1000; i >= 0; i--) {
			if(prev == (prev = jur.nextInt(upperBound))) ++count;
		}
		System.out.println("\nRandom: " + count);
		prev = -1;
		count = 0;
		for (int i = upperBound * 1000; i >= 0; i--) {
			if(prev == (prev = jusr.nextInt(upperBound))) ++count;
		}
		System.out.println("\nSplittableRandom: " + count);
		prev = -1;
		count = 0;
		for (int i = upperBound * 1000; i >= 0; i--) {
			if(prev == (prev = lr.nextInt(upperBound))) ++count;
		}
		System.out.println("\nLaserRandom: " + count);

		System.out.println("Generating 2 to the 35 longs, should all be 0 or -1...");
		lr = new LaserRandom(-0xC6BC279692B5C323L, -0x9E3779B97F4A7C17L);
		for (int i = 0; i < 0x40000; i++) {
			for (int j = 0; j < 0x20000; j++) {
				if(lr.nextSignedLong(-2) > 0) {
					System.out.println("nextSignedLong(-2) had an incorrect result.\nFAILURE!");
					System.out.printf("0x%016XL, 0x%016XL", lr.getStateA(), lr.getStateB());
					return;
				}
			}
		}
		System.out.println("Success!");
	}
}

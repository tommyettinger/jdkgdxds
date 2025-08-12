/*
 * Copyright (c) 2025 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.tommyettinger.ds.test;

import com.github.tommyettinger.ds.IdentityObjectMap;
import com.github.tommyettinger.ds.flip.ObjectObjectMap;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;
import java.util.IdentityHashMap;

@Ignore
public class CuckooTest {
	// Expected to fail with an OutOfMemoryError.
	@Test(expected = OutOfMemoryError.class)
	public void failingCuckooTest() {
		CuckooObjectMap<String, Object> map = new CuckooObjectMap<>();
		String[] problems = ("21oo 0oq1 0opP 0ooo 0pPo 21pP 21q1 1Poo 1Pq1 1PpP 0q31 0pR1 0q2P 0q1o 232P 231o 2331 0pQP 22QP 22Po 22R1 1QQP 1R1o 1QR1 1R2P 1R31 1QPo 1Qup 1S7p 0r8Q 0r7p 0r92 23X2 2492 248Q 247p 22vQ 22up 1S92 1S8Q 23WQ 23Vp 22w2 1QvQ 1Qw2 1RVp 1RWQ 1RX2 0qX2".split(" "));
		System.out.println("Trying to enter " + problems.length + " String keys into a CuckooObjectMap.");
		for (int i = 0; i < problems.length; i++) {
			System.out.println("Entered " + i + " keys successfully.");
			map.put(problems[i], null);
		}
		System.out.println("Unexpectedly succeeded; finished CuckooObjectMap has size: " + map.size);
	}

	// Expected to fail with an OutOfMemoryError.
	@Test(expected = OutOfMemoryError.class)
	public void failingStringentCuckooTest() {
		CuckooObjectMap<String, Object> map = new CuckooObjectMap<>();
		// all have a hashCode() of -2140395045
		String[] problems = {
			"Haxvnr", "HaxvoS", "Haxvp4", "HaxwOr", "HaxwPS", "HaxwQ4", "Haxx0r", "Haxx1S",
			"Haxx24", "HayWnr", "HayWoS", "HayWp4", "HayXOr", "HayXPS", "HayXQ4", "HayY0r",
			"HayY1S", "HayY24", "Haz8nr", "Haz8oS", "Haz8p4", "Haz9Or", "Haz9PS", "Haz9Q4",
			"HbYvnr", "HbYvoS", "HbYvp4", "HbYwOr", "HbYwPS", "HbYwQ4", "HbYx0r", "HbYx1S",
			"HbYx24", "HbZWnr", "HbZWoS", "HbZWp4", "HbZXOr", "HbZXPS", "HbZXQ4", "HbZY0r",
			"HbZY1S", "HbZY24", "IBxvnr", "IBxvoS", "IBxvp4", "IBxwOr", "IBxwPS", "IBxwQ4",
			"IBxx0r", "IBxx1S", "IBxx24", "IByWnr", "IByWoS", "IByWp4", "IByXOr", "IByXPS",
			"IByXQ4", "IByY0r", "IByY1S", "IByY24", "IBz8nr", "IBz8oS", "IBz8p4", "IBz9Or",
			"IBz9PS", "IBz9Q4", "ICYvnr", "ICYvoS", "ICYvp4", "ICYwOr", "ICYwPS", "ICYwQ4",
			"ICYx0r", "ICYx1S", "ICYx24", "ICZWnr", "ICZWoS", "ICZWp4", "ICZXOr", "ICZXPS",
			"ICZXQ4", "ICZY0r", "ICZY1S", "ICZY24",};
		System.out.println("Trying to enter " + problems.length + " String keys into a CuckooObjectMap.");
		for (int i = 0; i < problems.length; i++) {
			System.out.println("Entered " + i + " keys successfully.");
			map.put(problems[i], null);
		}
		System.out.println("Unexpectedly succeeded; finished CuckooObjectMap has size: " + map.size);
	}

	@Test
	public void workingCuckooTest() {
		ObjectObjectCuckooMap<String, Object> map = new ObjectObjectCuckooMap<>();
		String[] problems = ("21oo 0oq1 0opP 0ooo 0pPo 21pP 21q1 1Poo 1Pq1 1PpP 0q31 0pR1 0q2P 0q1o 232P 231o 2331 0pQP 22QP 22Po 22R1 1QQP 1R1o 1QR1 1R2P 1R31 1QPo 1Qup 1S7p 0r8Q 0r7p 0r92 23X2 2492 248Q 247p 22vQ 22up 1S92 1S8Q 23WQ 23Vp 22w2 1QvQ 1Qw2 1RVp 1RWQ 1RX2 0qX2".split(" "));
		System.out.println("Trying to enter " + problems.length + " String keys into an ObjectObjectCuckooMap.");
		for (int i = 0; i < problems.length; i++) {
			System.out.println("Entered " + i + " keys successfully.");
			map.put(problems[i], null);
		}
		System.out.println("Succeeded; finished ObjectObjectCuckooMap has size: " + map.size);
	}

	@Test
	public void workingStringentCuckooTest() {
		ObjectObjectCuckooMap<String, Object> map = new ObjectObjectCuckooMap<>();
		// all have a hashCode() of -2140395045
		String[] problems = {
			"Haxvnr", "HaxvoS", "Haxvp4", "HaxwOr", "HaxwPS", "HaxwQ4", "Haxx0r", "Haxx1S",
			"Haxx24", "HayWnr", "HayWoS", "HayWp4", "HayXOr", "HayXPS", "HayXQ4", "HayY0r",
			"HayY1S", "HayY24", "Haz8nr", "Haz8oS", "Haz8p4", "Haz9Or", "Haz9PS", "Haz9Q4",
			"HbYvnr", "HbYvoS", "HbYvp4", "HbYwOr", "HbYwPS", "HbYwQ4", "HbYx0r", "HbYx1S",
			"HbYx24", "HbZWnr", "HbZWoS", "HbZWp4", "HbZXOr", "HbZXPS", "HbZXQ4", "HbZY0r",
			"HbZY1S", "HbZY24", "IBxvnr", "IBxvoS", "IBxvp4", "IBxwOr", "IBxwPS", "IBxwQ4",
			"IBxx0r", "IBxx1S", "IBxx24", "IByWnr", "IByWoS", "IByWp4", "IByXOr", "IByXPS",
			"IByXQ4", "IByY0r", "IByY1S", "IByY24", "IBz8nr", "IBz8oS", "IBz8p4", "IBz9Or",
			"IBz9PS", "IBz9Q4", "ICYvnr", "ICYvoS", "ICYvp4", "ICYwOr", "ICYwPS", "ICYwQ4",
			"ICYx0r", "ICYx1S", "ICYx24", "ICZWnr", "ICZWoS", "ICZWp4", "ICZXOr", "ICZXPS",
			"ICZXQ4", "ICZY0r", "ICZY1S", "ICZY24",
		};
		System.out.println("Trying to enter " + problems.length + " String keys into an ObjectObjectCuckooMap.");
		for (int i = 0; i < problems.length; i++) {
			System.out.println("Entered " + i + " keys successfully.");
			map.put(problems[i], null);
		}
		System.out.println("Succeeded; finished ObjectObjectCuckooMap has size: " + map.size);
	}

	private static class Killer {
		int state;

		public Killer() {
			state = 0;
		}

		public Killer(int s) {
			state = s;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;

			Killer killer = (Killer) o;

			return state == killer.state;
		}

		@Override
		public int hashCode() {
			return 0; // UH OH HERE WE GO
		}
	}

	@Ignore
	@Test
	public void workingLethalCuckooTest() {
		ObjectObjectCuckooMap<Killer, Object> map = new ObjectObjectCuckooMap<>();
		int size = 0x8000;
		System.out.println("Trying to enter " + size + " Killer keys into an ObjectObjectCuckooMap.");
		for (int i = 0; i < size; i++) {
			System.out.println("Entered " + i + " keys successfully.");
			map.put(new Killer(i), null);
		}
		System.out.println("Yay! Succeeded; finished ObjectObjectCuckooMap has size: " + map.size);
	}

	@Test
	public void workingLethalLinearTest() {
		com.github.tommyettinger.ds.ObjectObjectMap<Killer, Object> map = new com.github.tommyettinger.ds.ObjectObjectMap<>();
		int size = 0x8000;
		System.out.println("Trying to enter " + size + " Killer keys into an ObjectObjectMap.");
		for (int i = 0; i < size; i++) {
			if ((i & i - 1) == 0)
				System.out.println("Entered " + i + " keys successfully.");
			map.put(new Killer(i), null);
		}
		System.out.println("Yay! Succeeded; finished ObjectObjectMap has size: " + map.size());
	}

	@Test
	public void workingLethalFlipTest() {
		ObjectObjectMap<Killer, Object> map = new ObjectObjectMap<>();
		int size = 0x8000;
		System.out.println("Trying to enter " + size + " Killer keys into a ObjectObjectMap.");
		for (int i = 0; i < size; i++) {
			if ((i & i - 1) == 0)
				System.out.println("Entered " + i + " keys successfully.");
			map.put(new Killer(i), null);
		}
		System.out.println("Yay! Succeeded; finished ObjectObjectMap has size: " + map.size());
	}

	// Expected to fail with an OutOfMemoryError.
	@Test(expected = OutOfMemoryError.class)
	public void vectorCuckooTest() {
		CuckooObjectMap<Vector2, Object> map = new CuckooObjectMap<>();
		final int LIMIT = 16, TOTAL = 1 << LIMIT, BOUND = 1 << (LIMIT - 2 >>> 1);
		Vector2[] problems = new Vector2[TOTAL];
		for (int x = -BOUND, i = 0; x < BOUND; x++) {
			for (int y = -BOUND; y < BOUND; y++) {
				problems[i++] = new Vector2(x, y);
			}
		}
//		Vector2[] problems = new Vector2[4096];
//		for (int x = -32, i = 0; x < 32; x++) {
//			for (int y = -32; y < 32; y++) {
//				problems[i++] = new Vector2(x, y);
//			}
//		}
		System.out.println("Trying to enter " + problems.length + " Vector2 keys into a CuckooObjectMap.");
		for (int i = 0; i < problems.length; i++) {
			System.out.println("Entered " + i + " keys successfully.");
			map.put(problems[i], null);
		}
		System.out.println("Unexpectedly succeeded; finished CuckooObjectMap has size: " + map.size);
	}

	//1504 ms at LIMIT=16
	//At LIMIT=20, OutOfMemoryError trying to enter 1048576 Vector2 keys into an ObjectObjectCuckooMap.
	@Test
	public void vectorGoodCuckooTest() {
		final long startTime = System.nanoTime();
		ObjectObjectCuckooMap<Vector2, Object> map = new ObjectObjectCuckooMap<>();
		final int LIMIT = 16, TOTAL = 1 << LIMIT, BOUND = 1 << (LIMIT - 2 >>> 1);
		Vector2[] problems = new Vector2[TOTAL];
		for (int x = -BOUND, i = 0; x < BOUND; x++) {
			for (int y = -BOUND; y < BOUND; y++) {
				problems[i++] = new Vector2(x, y);
			}
		}
		System.out.println("Trying to enter " + problems.length + " Vector2 keys into an ObjectObjectCuckooMap.");
		for (int i = 0; i < problems.length; i++) {
//			System.out.println("Entered " + i + " keys successfully.");
			map.put(problems[i], null);
		}
		System.out.println("Succeeded in " + Math.round((System.nanoTime() - startTime) * 1E-6) +
			" ms; finished ObjectObjectCuckooMap has size: " + map.size());
	}

	//19 ms at LIMIT=16
	//335 ms at LIMIT=20
	@Test
	public void vectorLinearTest() {
		final long startTime = System.nanoTime();
		com.github.tommyettinger.ds.ObjectObjectMap<Vector2, Object> map = new com.github.tommyettinger.ds.ObjectObjectMap<>();
		final int LIMIT = 16, TOTAL = 1 << LIMIT, BOUND = 1 << (LIMIT - 2 >>> 1);
		Vector2[] problems = new Vector2[TOTAL];
		for (int x = -BOUND, i = 0; x < BOUND; x++) {
			for (int y = -BOUND; y < BOUND; y++) {
				problems[i++] = new Vector2(x, y);
			}
		}
		System.out.println("Trying to enter " + problems.length + " Vector2 keys into an ObjectObjectMap.");
		for (int i = 0; i < problems.length; i++) {
//			System.out.println("Entered " + i + " keys successfully.");
			map.put(problems[i], null);
		}
		System.out.println("Succeeded in " + Math.round((System.nanoTime() - startTime) * 1E-6) +
			" ms; finished ObjectObjectMap has size: " + map.size());
	}

	@Test
	public void vectorHashMapTest() {
		final long startTime = System.nanoTime();
		HashMap<Vector2, Object> map = new HashMap<>();
		final int LIMIT = 20, TOTAL = 1 << LIMIT, BOUND = 1 << (LIMIT - 2 >>> 1);
		Vector2[] problems = new Vector2[TOTAL];
		for (int x = -BOUND, i = 0; x < BOUND; x++) {
			for (int y = -BOUND; y < BOUND; y++) {
				problems[i++] = new Vector2(x, y);
			}
		}
		System.out.println("Trying to enter " + problems.length + " Vector2 keys into a HashMap.");
		for (int i = 0; i < problems.length; i++) {
//			System.out.println("Entered " + i + " keys successfully.");
			map.put(problems[i], null);
		}
		System.out.println("Succeeded in " + Math.round((System.nanoTime() - startTime) * 1E-6) +
			" ms; finished HashMap has size: " + map.size());
	}

	//681 ms at LIMIT=22
	//1555 ms at LIMIT=23
	@Test
	public void blankLinearTest() {
		final long startTime = System.nanoTime();
		final int LIMIT = 22, TOTAL = 1 << LIMIT;
		com.github.tommyettinger.ds.ObjectObjectMap<Object, Object> map = new com.github.tommyettinger.ds.ObjectObjectMap<>(TOTAL);
		Object[] problems = new Object[TOTAL];
		for (int x = 0, i = 0; x < TOTAL; x++) {
			problems[i++] = new Object();
		}
		System.out.println("Trying to enter " + problems.length + " Object keys into an ObjectObjectMap.");
		for (int i = 0; i < problems.length; i++) {
//			System.out.println("Entered " + i + " keys successfully.");
			map.put(problems[i], null);
		}
		System.out.println("Succeeded in " + Math.round((System.nanoTime() - startTime) * 1E-6) +
			" ms; finished ObjectObjectMap has size: " + map.size());
	}

	//1435 ms at LIMIT=22
	//3002 ms at LIMIT=23
	@Test
	public void blankHashMapTest() {
		final long startTime = System.nanoTime();
		final int LIMIT = 22, TOTAL = 1 << LIMIT;
		HashMap<Object, Object> map = new HashMap<>(TOTAL);
		Object[] problems = new Object[TOTAL];
		for (int x = 0, i = 0; x < TOTAL; x++) {
			problems[i++] = new Object();
		}
		System.out.println("Trying to enter " + problems.length + " Object keys into an HashMap.");
		for (int i = 0; i < problems.length; i++) {
//			System.out.println("Entered " + i + " keys successfully.");
			map.put(problems[i], null);
		}
		System.out.println("Succeeded in " + Math.round((System.nanoTime() - startTime) * 1E-6) +
			" ms; finished HashMap has size: " + map.size());
	}

	//630 ms at LIMIT=22
	//1314 ms at LIMIT=23
	@Test
	public void blankIdentityTest() {
		final long startTime = System.nanoTime();
		final int LIMIT = 23, TOTAL = 1 << LIMIT;
		IdentityObjectMap<Object, Object> map = new IdentityObjectMap<>(TOTAL);
		Object[] problems = new Object[TOTAL];
		for (int x = 0, i = 0; x < TOTAL; x++) {
			problems[i++] = new Object();
		}
		System.out.println("Trying to enter " + problems.length + " Object keys into an IdentityObjectMap.");
		for (int i = 0; i < problems.length; i++) {
//			System.out.println("Entered " + i + " keys successfully.");
			map.put(problems[i], null);
		}
		System.out.println("Succeeded in " + Math.round((System.nanoTime() - startTime) * 1E-6) +
			" ms; finished IdentityObjectMap has size: " + map.size());
	}

	//681 ms at LIMIT=22
	//1401 ms at LIMIT=23
	@Test
	public void blankIdentityHashMapTest() {
		final long startTime = System.nanoTime();
		final int LIMIT = 23, TOTAL = 1 << LIMIT;
		IdentityHashMap<Object, Object> map = new IdentityHashMap<>(TOTAL);
		Object[] problems = new Object[TOTAL];
		for (int x = 0, i = 0; x < TOTAL; x++) {
			problems[i++] = new Object();
		}
		System.out.println("Trying to enter " + problems.length + " Object keys into an IdentityHashMap.");
		for (int i = 0; i < problems.length; i++) {
//			System.out.println("Entered " + i + " keys successfully.");
			map.put(problems[i], null);
		}
		System.out.println("Succeeded in " + Math.round((System.nanoTime() - startTime) * 1E-6) +
			" ms; finished IdentityHashMap has size: " + map.size());
	}

	@Test(timeout = 4000L)
	@Ignore("This will not terminate on its own, so it is ignored.")
	public void failingCHMTest() {
		CuckooHashMap<String, Object> map = new CuckooHashMap<>();
		String[] problems = ("0q1o 0oq1 0ooo 21oo 0qX2 0opP 0pPo 21pP 21q1 1Poo 1Pq1 1PpP 0q31 0pR1 0q2P 232P 231o 2331 0pQP 22QP 22Po 22R1 1QQP 1R1o 1QR1 1R2P 1R31 1QPo 1Qup 1S7p 0r8Q 0r7p 0r92 23X2 2492 248Q 247p 22vQ 22up 1S92 1S8Q 23WQ 23Vp 22w2 1QvQ 1Qw2 1RVp 1RWQ 1RX2".split(" "));
		System.out.println("Trying to enter " + problems.length + " String keys into a CuckooHashMap.");
		for (int i = 0; i < problems.length; i++) {
			map.put(problems[i], null);
			System.out.println("Entered key " + i + ", '" + problems[i] + "' successfully.");
		}
		System.out.println("Succeeded; finished CuckooHashMap has size: " + map.size());
	}

	@Test
	public void workingICMTest() {
		IdentityCuckooMap<String, Object> map = new IdentityCuckooMap<>();
		String[] problems = ("21oo 0oq1 0opP 0ooo 0pPo 21pP 21q1 1Poo 1Pq1 1PpP 0q31 0pR1 0q2P 0q1o 232P 231o 2331 0pQP 22QP 22Po 22R1 1QQP 1R1o 1QR1 1R2P 1R31 1QPo 1Qup 1S7p 0r8Q 0r7p 0r92 23X2 2492 248Q 247p 22vQ 22up 1S92 1S8Q 23WQ 23Vp 22w2 1QvQ 1Qw2 1RVp 1RWQ 1RX2 0qX2".split(" "));
		System.out.println("Trying to enter " + problems.length + " String keys into a IdentityCuckooMap.");
		for (int i = 0; i < problems.length; ) {
			map.put(problems[i], null);
			System.out.println("Entered " + ++i + " keys successfully.");
		}
		System.out.println("Succeeded; finished IdentityCuckooMap has size: " + map.size());
	}


	@Test
	public void basicFlipTest() {
		ObjectObjectMap<String, Object> map = new ObjectObjectMap<>();
		String[] problems = ("21oo 0oq1 0opP 0ooo 0pPo 21pP 21q1 1Poo 1Pq1 1PpP 0q31 0pR1 0q2P 0q1o 232P 231o 2331 0pQP 22QP 22Po 22R1 1QQP 1R1o 1QR1 1R2P 1R31 1QPo 1Qup 1S7p 0r8Q 0r7p 0r92 23X2 2492 248Q 247p 22vQ 22up 1S92 1S8Q 23WQ 23Vp 22w2 1QvQ 1Qw2 1RVp 1RWQ 1RX2 0qX2".split(" "));
		System.out.println("Trying to enter " + problems.length + " String keys into a ObjectObjectMap.");
		for (int i = 0; i < problems.length; ) {
			map.put(problems[i], null);
			System.out.println("Entered " + ++i + " keys successfully.");
		}
		System.out.println("Succeeded; finished ObjectObjectMap has size: " + map.size());
	}


	/**
	 * Generates 6-char Strings that all have the same hashCode().
	 *
	 * @param args ignored
	 */
	public static void main(String[] args) {
		int[] coeffs = {
			31 * 31 * 31 * 31 * 31,
			31 * 31 * 31 * 31,
			31 * 31 * 31,
			31 * 31,
			31,
			1};
		char[] usable = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
		final int target = "Haxx0r".hashCode();
		int count = 0;
		for (int a = 0, aReset = 0; a < usable.length; a++) {
			aReset = coeffs[0] * usable[a];
			for (int b = 0, bReset = aReset; b < usable.length; b++) {
				bReset = aReset + coeffs[1] * usable[b];
				for (int c = 0, cReset = bReset; c < usable.length; c++) {
					cReset = bReset + coeffs[2] * usable[c];
					for (int d = 0, dReset = cReset; d < usable.length; d++) {
						dReset = cReset + coeffs[3] * usable[d];
						for (int e = 0, eReset = dReset; e < usable.length; e++) {
							eReset = dReset + coeffs[4] * usable[e];
							for (int f = 0, fReset = eReset; f < usable.length; f++) {
								fReset = eReset + coeffs[5] * usable[f];
								if (fReset == target) {
									System.out.print('"');
									System.out.print(usable[a]);
									System.out.print(usable[b]);
									System.out.print(usable[c]);
									System.out.print(usable[d]);
									System.out.print(usable[e]);
									System.out.print(usable[f]);
									System.out.println("\",");
									count++;
								}
							}
						}
					}
				}
			}
		}
		System.out.println("\nFound " + count + " colliding hashCodes.");
	}
}

package com.github.tommyettinger.ds.test;

import com.github.tommyettinger.ds.ObjectObjectMap;
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
		System.out.println("Unexpectedly succeeded; finished CuckooObjectMap has size: " + map.size);
	}
	// Expected to fail with an OutOfMemoryError.
	@Test(expected = OutOfMemoryError.class)
	public void failingStringentCuckooTest(){
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
			"ICZXQ4", "ICZY0r", "ICZY1S", "ICZY24",		};
		System.out.println("Trying to enter " + problems.length + " String keys into a CuckooObjectMap.");
		for (int i = 0; i < problems.length; i++) {
			System.out.println("Entered " + i + " keys successfully.");
			map.put(problems[i], null);
		}
		System.out.println("Unexpectedly succeeded; finished CuckooObjectMap has size: " + map.size);
	}

	@Test
	public void workingCuckooTest (){
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
	public void workingStringentCuckooTest (){
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
		public Killer(){
			state = 0;
		}
		public Killer(int s){
			state = s;
		}

		@Override
		public boolean equals (Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;

			Killer killer = (Killer)o;

			return state == killer.state;
		}

		@Override
		public int hashCode () {
			return 0; // UH OH HERE WE GO
		}
	}

	@Test
	public void workingLethalCuckooTest (){
		ObjectObjectCuckooMap<Killer, Object> map = new ObjectObjectCuckooMap<>();
		int size = 100000;
		System.out.println("Trying to enter " + size + " Killer keys into an ObjectObjectCuckooMap.");
		for (int i = 0; i < size; i++) {
			System.out.println("Entered " + i + " keys successfully.");
			map.put(new Killer(i), null);
		}
		System.out.println("Surprise! Succeeded; finished ObjectObjectCuckooMap has size: " + map.size);
	}

	@Test
	public void workingLethalLinearTest (){
		ObjectObjectMap<Killer, Object> map = new ObjectObjectMap<>();
		int size = 100000;
		System.out.println("Trying to enter " + size + " Killer keys into an ObjectObjectMap.");
		for (int i = 0; i < size; i++) {
			System.out.println("Entered " + i + " keys successfully.");
			map.put(new Killer(i), null);
		}
		System.out.println("Surprise! Succeeded; finished ObjectObjectMap has size: " + map.size());
	}

	// Expected to fail with an OutOfMemoryError.
	@Test(expected = OutOfMemoryError.class)
	public void vectorCuckooTest(){
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

	//3.46 seconds at LIMIT=16
	@Test
	public void vectorGoodCuckooTest(){
		ObjectObjectCuckooMap<Vector2, Object> map = new ObjectObjectCuckooMap<>();
		final int LIMIT = 16, TOTAL = 1 << LIMIT, BOUND = 1 << (LIMIT - 2 >>> 1);
		Vector2[] problems = new Vector2[TOTAL];
		for (int x = -BOUND, i = 0; x < BOUND; x++) {
			for (int y = -BOUND; y < BOUND; y++) {
				problems[i++] = new Vector2(x, y);
			}
		}
		System.out.println("Trying to enter " + problems.length + " Vector2 keys into a ObjectObjectCuckooMap.");
		for (int i = 0; i < problems.length; i++) {
			System.out.println("Entered " + i + " keys successfully.");
			map.put(problems[i], null);
		}
		System.out.println("Succeeded; finished ObjectObjectCuckooMap has size: " + map.size());
	}

	//6.969 seconds at LIMIT=16
	@Test
	public void vectorLinearTest(){
		ObjectObjectMap<Vector2, Object> map = new ObjectObjectMap<>();
		final int LIMIT = 16, TOTAL = 1 << LIMIT, BOUND = 1 << (LIMIT - 2 >>> 1);
		Vector2[] problems = new Vector2[TOTAL];
		for (int x = -BOUND, i = 0; x < BOUND; x++) {
			for (int y = -BOUND; y < BOUND; y++) {
				problems[i++] = new Vector2(x, y);
			}
		}
		System.out.println("Trying to enter " + problems.length + " Vector2 keys into a ObjectObjectMap.");
		for (int i = 0; i < problems.length; i++) {
			System.out.println("Entered " + i + " keys successfully.");
			map.put(problems[i], null);
		}
		System.out.println("Succeeded; finished ObjectObjectMap has size: " + map.size());
	}

	/**
	 * Generates 6-char Strings that all have the same hashCode().
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

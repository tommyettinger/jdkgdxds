package com.github.tommyettinger.ds.test;

import com.github.tommyettinger.ds.ObjectDeque;
import org.junit.Assert;
import org.junit.Test;

public class DequeTest {
	@Test
	public void testSort() {
		ObjectDeque<String> deque = new ObjectDeque<>(8);
		deque.add("Carl");
		deque.add("theodore");
		deque.add("John");
		deque.add("jamie");
		deque.removeFirst();
		deque.removeFirst();
		deque.add("bob");
		deque.add("alice");
		deque.add("carol");
		deque.add("Billy");
		System.out.println(deque);
		System.out.println();
		deque.sort(null);
		System.out.println(deque);
		System.out.println();
		deque.sort(String.CASE_INSENSITIVE_ORDER);
		System.out.println(deque);
	}

	@Test
	public void testInsert() {
		ObjectDeque<String> deque = ObjectDeque.with(
			"alpha", "beta", "gamma", "delta", "epsilon", "zeta",
			"eta", "theta", "iota", "kappa", "lambda",
			"mu", "nu", "xi", "omicron", "pi", "rho", "sigma",
			"tau", "upsilon", "phi", "chi", "psi", "omega"
		);
		deque.insert(3, "STEVE");
		deque.removeAt(2);
		deque.removeAt(6);
		deque.removeAt(6);
		deque.removeAt(6);
		deque.removeAt(6);
		deque.removeAt(6);
		deque.insert(12, "BILLY");
		deque.insert(4, "JIMBO");
		Assert.assertEquals("[alpha, beta, STEVE, delta, JIMBO, epsilon, zeta, mu, nu, xi, omicron, pi, rho, "
			+ "BILLY, sigma, tau, upsilon, phi, chi, psi, omega]", deque.toString());
	}

	@Test
	public void testMixedRemoval() {
		ObjectDeque<String> deque = ObjectDeque.with(
			"alpha", "beta", "gamma", "delta", "epsilon", "zeta",
			"eta", "theta", "iota", "kappa", "lambda",
			"mu", "nu", "xi", "omicron", "pi", "rho", "sigma",
			"tau", "upsilon", "phi", "chi", "psi", "omega"
		);
		deque.removeFirst();
		deque.removeFirst();
		deque.removeFirst();
		deque.removeFirst();
		deque.removeFirst();
		deque.removeFirst();
		// by now, the first row has been removed, and the first 6 of 24 slots are empty.
		// the deque should contain eta through omega.
		System.out.println(deque);
		deque.addLast("AMBER");
		deque.addLast("BETTY");
		deque.addLast("CAROL");
		// the deque should contain eta through omega, followed by three all-caps names.
		// the names should be at slots 0, 1, 2, and eta starts at slot 6.
		System.out.println(deque);
		deque.removeAt(1); // removes theta
		System.out.println(deque);
		deque.removeAt(18); // removes BETTY
		System.out.println(deque);
		deque.addFirst("bet");
		deque.addFirst("alef");
		System.out.println(deque);
		deque.removeLast(); // removes CAROL
		deque.removeLast(); // removes AMBER
		deque.removeLast(); // removes omega
		deque.addLast("ZILTOID THE OMNISCIENT");
		System.out.println(deque);

	}
}
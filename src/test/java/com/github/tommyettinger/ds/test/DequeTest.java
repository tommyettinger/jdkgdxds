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
}
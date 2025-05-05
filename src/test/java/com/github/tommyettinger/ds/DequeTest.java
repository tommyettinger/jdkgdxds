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

package com.github.tommyettinger.ds;

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
		deque.remove(2);
		deque.remove(6);
		deque.remove(6);
		deque.remove(6);
		deque.remove(6);
		deque.remove(6);
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
		deque.remove(1); // removes theta
		System.out.println(deque);
		deque.remove(18); // removes BETTY
		System.out.println(deque);
		deque.addFirst("alef");
		deque.add(1, "bet");
		System.out.println(deque);
		deque.removeLast(); // removes CAROL
		deque.removeLast(); // removes AMBER
		deque.removeLast(); // removes omega
		deque.addLast("ZILTOID THE OMNISCIENT");
		System.out.println(deque);


		deque = ObjectDeque.with(
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
		System.out.println(deque);
		deque.removeLast();
		deque.removeLast();
		System.out.println(deque);
		deque.addLast("OMEGA");
		deque.addLast("CAPTAIN SPECTACULAR");
		deque.add(17, "PSI");
		String text = deque.toString(", ");
		System.out.println(text);

//		String[] put = "foo bar baz quux blin".split(" ");
		deque.duplicateRange(4, 5);
//		deque.addAll(4, put);
		text = deque.toString(", ");
//		text = deque.subList(4, 9).toString();
		System.out.println(text);

		deque.duplicateRange(2, 5);
//		deque.addAll(2, put);
		text = deque.toString(", ");
//		text = deque.subList(2, 7).toString();
		System.out.println(text);

		deque.duplicateRange(0, 5);
//		deque.addAll(0, put);
		text = deque.toString(", ");
//		text = deque.subList(0, 5).toString();
		System.out.println(text);

		deque.clear();
		deque.addFirst("wombat");
		deque.addLast("kangaroo");
		deque.addFirst("boomerang");
		deque.addLast("didgeridoo");
		deque.clear();

		System.out.println("NOW WITH LIST");
		ObjectList<String> list = new ObjectList<>("eta, theta, iota, kappa, lambda, mu, nu, xi, omicron, pi, rho, sigma, tau, upsilon, phi, chi, OMEGA, PSI, CAPTAIN SPECTACULAR".split(", "));

		list.duplicateRange(4, 5);
		text = list.toString(", ");
		System.out.println(text);

		list.duplicateRange(2, 5);
		text = list.toString(", ");
		System.out.println(text);

		list.duplicateRange(0, 5);
		text = list.toString(", ");
		System.out.println(text);

	}

	@Test
	public void testTruncate() {
		{
			System.out.println("truncating with head < tail, tail < values.length");
			ObjectDeque<String> deque = ObjectDeque.with(
				"alpha", "beta", "gamma", "delta", "epsilon", "zeta",
				"eta", "theta", "iota", "kappa", "lambda",
				"mu", "nu", "xi", "omicron", "pi", "rho", "sigma",
				"tau", "upsilon", "phi", "chi", "psi", "omega"
			);
			deque.removeLast();
			deque.truncate(deque.size - 5);
			// by now, the last row has been removed, and the first 6 of 24 slots are empty.
			// the deque should contain eta through omega.
			System.out.println(deque);
			deque.addLast("AMBER");
			deque.addLast("BETTY");
			deque.addLast("CAROL");
			// the deque should contain eta through omega, followed by three all-caps names.
			// the names should be at slots 0, 1, 2, and eta starts at slot 6.
			System.out.println(deque);
		}
		{
			System.out.println("truncating with tail < head, removing more than tail items (wrapping around)");
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
			deque.truncate(deque.size - 5);
			System.out.println(deque);
		}
		{
			System.out.println("truncating with tail < head, removing less than tail items (not wrapping)");
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
			deque.truncate(deque.size - 2);
			System.out.println(deque);
		}
	}
}
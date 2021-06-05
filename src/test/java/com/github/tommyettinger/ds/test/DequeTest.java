package com.github.tommyettinger.ds.test;

import com.github.tommyettinger.ds.ObjectDeque;
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
}
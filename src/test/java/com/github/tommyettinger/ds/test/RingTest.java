package com.github.tommyettinger.ds.test;

import com.github.tommyettinger.ds.ObjectDeque;
import com.github.tommyettinger.ds.ObjectRing;
import org.junit.Assert;
import org.junit.Test;

public class RingTest {
	@Test
	public void testBasics() {
		ObjectRing<String> deque = new ObjectRing<>(5);
		deque.add("Carl");
		deque.add("John");
		deque.add("theodore");
		deque.add("jamie");
		deque.add("bob");
		deque.add("alice");
		deque.add("carol");
		deque.removeLast();
		deque.add("billy");
		System.out.println(deque);
		Assert.assertEquals("billy", deque.getLast());
		Assert.assertEquals("theodore", deque.getFirst());
	}
}
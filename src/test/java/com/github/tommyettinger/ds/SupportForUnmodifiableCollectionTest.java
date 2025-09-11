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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import junit.framework.TestCase;
import org.junit.Ignore;

/**
 * From <a href="https://github.com/apache/harmony">Apache Harmony's GitHub mirror</a>.
 * Originally {@code Support_UnmodifiableCollectionTest}.
 */
@Ignore
public class SupportForUnmodifiableCollectionTest extends TestCase {

	Collection<Integer> col;

	// must be a collection containing the Integers 0 to 99 (which will iterate
	// in order)

	public SupportForUnmodifiableCollectionTest(String p1) {
		super(p1);
	}

	public SupportForUnmodifiableCollectionTest(String p1, Collection<Integer> c) {
		super(p1);
		col = c;
	}

	@Override
	public void runTest() {

		// contains
		assertTrue("UnmodifiableCollectionTest - should contain 0", col
			.contains(Integer.valueOf(0)));
		assertTrue("UnmodifiableCollectionTest - should contain 50", col
			.contains(Integer.valueOf(50)));
		assertTrue("UnmodifiableCollectionTest - should not contain 100", !col
			.contains(Integer.valueOf(100)));

		// containsAll
		HashSet<Integer> hs = new HashSet<Integer>();
		hs.add(Integer.valueOf(0));
		hs.add(Integer.valueOf(25));
		hs.add(Integer.valueOf(99));
		assertTrue(
			"UnmodifiableCollectionTest - should contain set of 0, 25, and 99",
			col.containsAll(hs));
		hs.add(Integer.valueOf(100));
		assertTrue(
			"UnmodifiableCollectionTest - should not contain set of 0, 25, 99 and 100",
			!col.containsAll(hs));

		// isEmpty
		assertTrue("UnmodifiableCollectionTest - should not be empty", !col
			.isEmpty());

		// iterator
		Iterator<Integer> it = col.iterator();
		SortedSet<Integer> ss = new TreeSet<Integer>();
		while (it.hasNext()) {
			ss.add(it.next());
		}
		it = ss.iterator();
		for (int counter = 0; it.hasNext(); counter++) {
			int nextValue = it.next().intValue();
			assertTrue(
				"UnmodifiableCollectionTest - Iterator returned wrong value.  Wanted: "
					+ counter + " got: " + nextValue,
				nextValue == counter);
		}

		// size
		assertTrue(
			"UnmodifiableCollectionTest - returned wrong size.  Wanted 100, got: "
				+ col.size(), col.size() == 100);

		// toArray
		Object[] objArray;
		objArray = col.toArray();
		for (int counter = 0; it.hasNext(); counter++) {
			assertTrue(
				"UnmodifiableCollectionTest - toArray returned incorrect array",
				objArray[counter] == it.next());
		}

		// toArray (Object[])
		objArray = new Object[100];
		col.toArray(objArray);
		for (int counter = 0; it.hasNext(); counter++) {
			assertTrue(
				"UnmodifiableCollectionTest - toArray(Object) filled array incorrectly",
				objArray[counter] == it.next());
		}

	}

}

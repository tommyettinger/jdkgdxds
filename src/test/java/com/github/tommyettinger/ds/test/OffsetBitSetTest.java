/*
 * Copyright (c) 2022 See AUTHORS file.
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
 *
 */

package com.github.tommyettinger.ds.test;

import com.github.tommyettinger.digital.ArrayTools;
import com.github.tommyettinger.ds.IntList;
import com.github.tommyettinger.ds.OffsetBitSet;
import com.github.tommyettinger.ds.support.util.IntIterator;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * This was originally BitsTest, from libGDX's tests.
 */
public class OffsetBitSetTest {

	@Test
	public void testHashcodeAndEquals () {
		OffsetBitSet b1 = new OffsetBitSet(1);
		OffsetBitSet b2 = new OffsetBitSet(1);

		b1.add(31);
		//		System.out.println("Length is " + b1.length());
		b2.add(31);

		assertEquals(b1.hashCode(), b2.hashCode());
		assertEquals(b1, b2);

		// temporarily setting/clearing a single bit causing
		// the backing array to grow
		b2.add(420);
		b2.deactivate(420);

		assertEquals(b1.hashCode(), b2.hashCode());
		assertEquals(b1, b2);

		b1.add(810);
		b1.deactivate(810);

		assertEquals(b1.hashCode(), b2.hashCode());
		assertEquals(b1, b2);

		OffsetBitSet o1 = new OffsetBitSet(100, 200);
		OffsetBitSet o2 = new OffsetBitSet(100, 200);

		o1.add(100);
		o2.add(100);

		assertEquals(o1.hashCode(), o2.hashCode());
		assertEquals(o1, o2);

		o2.add(420);
		o2.deactivate(420);

		assertEquals(o1.hashCode(), o2.hashCode());
		assertEquals(o1, o2);

		o1.add(810);
		o1.deactivate(810);

		assertEquals(o1.hashCode(), o2.hashCode());
		assertEquals(o1, o2);
	}

	@Test
	public void testXor () {
		OffsetBitSet b1 = new OffsetBitSet();
		OffsetBitSet b2 = new OffsetBitSet();

		b2.add(200);

		// b1:s array should grow to accommodate b2
		b1.xor(b2);

		assertTrue(b1.contains(200));

		b1.add(1024);
		b2.xor(b1);

		assertTrue(b2.contains(1024));
	}

	@Test
	public void testOr () {
		OffsetBitSet b1 = new OffsetBitSet();
		OffsetBitSet b2 = new OffsetBitSet();

		b2.add(200);

		// b1:s array should grow to accommodate b2
		b1.or(b2);

		assertTrue(b1.contains(200));

		b1.add(1024);
		b2.or(b1);

		assertTrue(b2.contains(1024));
	}

	@Test
	public void testAnd () {
		OffsetBitSet b1 = new OffsetBitSet();
		OffsetBitSet b2 = new OffsetBitSet();

		b2.add(200);
		// b1 should cancel b2:s bit
		b2.and(b1);

		assertFalse(b2.contains(200));

		b1.add(400);
		b1.and(b2);

		assertFalse(b1.contains(400));
	}

	@Test
	public void testCopyConstructor () {
		OffsetBitSet b1 = new OffsetBitSet();
		b1.add(50);
		b1.add(100);
		b1.add(150);
		b1.changeOffset(1000);

		OffsetBitSet b2 = new OffsetBitSet(b1);
		assertNotSame(b1, b2);
		assertTrue(b1.containsAll(b2));
		assertTrue(b2.containsAll(b1));
		assertEquals(b1, b2);
	}

	@Test
	public void testNextSetBit () {
		OffsetBitSet b1 = OffsetBitSet.with(50, 100, 200);
		b1.setOffset(-1000);
		int bit = b1.nextSetBit(-1000);
		Assert.assertEquals(50-1000, bit);
		bit = b1.nextSetBit(bit+1);
		Assert.assertEquals(100-1000, bit);
		bit = b1.nextSetBit(bit+1);
		Assert.assertEquals(200-1000, bit);
		bit = b1.nextSetBit(bit+1);
		Assert.assertEquals(-1-1000, bit);
	}

	@Test
	public void testNextClearBit () {
		OffsetBitSet b1 = new OffsetBitSet(256);
		b1.addAll(ArrayTools.range(1, 50));
		b1.addAll(ArrayTools.range(51, 100));
		b1.addAll(ArrayTools.range(101, 256));
		b1.changeOffset(-5);
		int bit = b1.nextClearBit(0-5);
		Assert.assertEquals(0-5, bit);
		bit = b1.nextClearBit(bit+1);
		Assert.assertEquals(50-5, bit);
		bit = b1.nextClearBit(bit+1);
		Assert.assertEquals(100-5, bit);
		bit = b1.nextClearBit(bit+1);
		Assert.assertEquals(256-5, bit);
		bit = b1.nextClearBit(bit+1);
		Assert.assertEquals(256-5, bit);
	}

	@Test
	public void testIterator () {
//		int[] items = new int[]{0, 1, 4, 20, 50, 9, 100};
//		OffsetBitSet b1 = OffsetBitSet.with(items);
//		b1.changeOffset(1000);
//		IntIterator it = b1.iterator();
//		while (it.hasNext()){
//			int n = it.next();
//			if(n == 1009) it.remove();
//		}
//		it = b1.iterator();
//		while (it.hasNext()) {
//			System.out.print(it.next() + ", ");
//		}
//		Assert.assertEquals(IntList.with(1000, 1001, 1004, 1020, 1050, 1100), b1.iterator().toList());


		int[] items = new int[]{0, 1, 4, 20, 50, 9, 100};
		OffsetBitSet b1 = OffsetBitSet.with(items);
		b1.changeOffset(1000);
		IntIterator it = b1.iterator();
		while (it.hasNext()){
			int n = it.nextInt();
			if(n == 1009) it.remove();
		}
		Assert.assertEquals(IntList.with(1000, 1001, 1004, 1020, 1050, 1100), b1.iterator().toList());
	}

	@Test
	public void testToString () {
		OffsetBitSet o1 = OffsetBitSet.with(3, 99, 2, 0, 1);
		o1.changeOffset(-5);
		Assert.assertEquals("[-5, -4, -3, -2, 94]", o1.toString());
//		System.out.println(o1.toString());
	}
}

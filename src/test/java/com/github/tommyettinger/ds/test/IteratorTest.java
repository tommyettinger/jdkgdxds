package com.github.tommyettinger.ds.test;

import com.github.tommyettinger.ds.*;
import com.github.tommyettinger.ds.support.util.BooleanIterator;
import com.github.tommyettinger.ds.support.util.ByteIterator;
import com.github.tommyettinger.ds.support.util.CharIterator;
import com.github.tommyettinger.ds.support.util.FloatIterator;
import com.github.tommyettinger.ds.support.util.ShortIterator;
import org.junit.Assert;
import org.junit.Test;

import java.util.Iterator;
import java.util.Map;
import java.util.PrimitiveIterator;

public class IteratorTest {
	public static final String[] strings = {
		"alpha", "beta", "gamma", "delta", "epsilon", "zeta", "eta", "theta", "iota", "kappa", "lambda",
		"mu", "nu", "xi", "omicron", "pi", "rho", "sigma", "tau", "upsilon", "phi", "chi", "psi", "omega"
	};
	public static final boolean[] booleans = {
		true, false, false, false, true, false, true, true, false, true, true, false, true, true, false, true
	};
	public static final byte[] bytes = {
		1, 0, -1, 2, -2, 3, -3, 11, 10, -11, 12, -12, 13, -13, 111, 110, -111, 112, -112, 113, -113
	};
	public static final short[] shorts = {
		'Α', 'Β', 'Γ', 'Δ', 'Ε', 'Ζ', 'Η', 'Θ', 'Ι', 'Κ', 'Λ', 'Μ', 'Ν', 'Ξ', 'Ο', 'Π', 'Ρ', 'Σ', 'Τ', 'Υ', 'Φ', 'Χ', 'Ψ', 'Ω'
	};
	public static final char[] chars = {
		'Α', 'Β', 'Γ', 'Δ', 'Ε', 'Ζ', 'Η', 'Θ', 'Ι', 'Κ', 'Λ', 'Μ', 'Ν', 'Ξ', 'Ο', 'Π', 'Ρ', 'Σ', 'Τ', 'Υ', 'Φ', 'Χ', 'Ψ', 'Ω'
	};
	public static final int[] ints = {
		'Α', 'Β', 'Γ', 'Δ', 'Ε', 'Ζ', 'Η', 'Θ', 'Ι', 'Κ', 'Λ', 'Μ', 'Ν', 'Ξ', 'Ο', 'Π', 'Ρ', 'Σ', 'Τ', 'Υ', 'Φ', 'Χ', 'Ψ', 'Ω'
	};
	public static final long[] longs = {
		'Α', 'Β', 'Γ', 'Δ', 'Ε', 'Ζ', 'Η', 'Θ', 'Ι', 'Κ', 'Λ', 'Μ', 'Ν', 'Ξ', 'Ο', 'Π', 'Ρ', 'Σ', 'Τ', 'Υ', 'Φ', 'Χ', 'Ψ', 'Ω'
	};
	public static final float[] floats = {
		'Α', 'Β', 'Γ', 'Δ', 'Ε', 'Ζ', 'Η', 'Θ', 'Ι', 'Κ', 'Λ', 'Μ', 'Ν', 'Ξ', 'Ο', 'Π', 'Ρ', 'Σ', 'Τ', 'Υ', 'Φ', 'Χ', 'Ψ', 'Ω'
	};
	public static final double[] doubles = {
		'Α', 'Β', 'Γ', 'Δ', 'Ε', 'Ζ', 'Η', 'Θ', 'Ι', 'Κ', 'Λ', 'Μ', 'Ν', 'Ξ', 'Ο', 'Π', 'Ρ', 'Σ', 'Τ', 'Υ', 'Φ', 'Χ', 'Ψ', 'Ω'
	};

	@Test
	public void testObjectListIterator() {
		ObjectList<String> data = ObjectList.with(strings);
		int counter = 0, size = data.size();
		for(String item : data){
			Assert.assertNotNull(item);
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<String> it = data.iterator();
		while (it.hasNext()) {
			String item = it.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testObjectDequeIterator() {
		ObjectDeque<String> data = ObjectDeque.with(strings);
		int counter = 0, size = data.size();
		for(String item : data){
			Assert.assertNotNull(item);
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<String> it = data.iterator();
		while (it.hasNext()) {
			String item = it.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testObjectSetIterator() {
		ObjectSet<String> data = ObjectSet.with(strings);
		int counter = 0, size = data.size();
		for(String item : data){
			Assert.assertNotNull(item);
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<String> it = data.iterator();
		while (it.hasNext()) {
			String item = it.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testObjectOrderedSetIterator() {
		ObjectOrderedSet<String> data = ObjectOrderedSet.with(strings);
		int counter = 0, size = data.size();
		for(String item : data){
			Assert.assertNotNull(item);
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<String> it = data.iterator();
		while (it.hasNext()) {
			String item = it.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testObjectObjectMapIterator() {
		ObjectObjectMap<String, String> data = new ObjectObjectMap<>(strings, strings);
		int counter = 0, size = data.size();
		for(Map.Entry<String, String> item : data){
			Assert.assertNotNull(item);
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<Map.Entry<String, String>> it = data.iterator();
		while (it.hasNext()) {
			Map.Entry<String, String> item = it.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testObjectObjectOrderedMapIterator() {
		ObjectObjectOrderedMap<String, String> data = new ObjectObjectOrderedMap<>(strings, strings);
		int counter = 0, size = data.size();
		for(Map.Entry<String, String> item : data){
			Assert.assertNotNull(item);
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<Map.Entry<String, String>> it = data.iterator();
		while (it.hasNext()) {
			Map.Entry<String, String> item = it.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testObjectIntMapIterator() {
		ObjectIntMap<String> data = new ObjectIntMap<>(strings, ints);
		int counter = 0, size = data.size();
		for(ObjectIntMap.Entry<String> item : data){
			Assert.assertNotNull(item);
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<ObjectIntMap.Entry<String>> it = data.iterator();
		while (it.hasNext()) {
			ObjectIntMap.Entry<String> item = it.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testObjectIntOrderedMapIterator() {
		ObjectIntOrderedMap<String> data = new ObjectIntOrderedMap<>(strings, ints);
		int counter = 0, size = data.size();
		for(ObjectIntMap.Entry<String> item : data){
			Assert.assertNotNull(item);
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<ObjectIntMap.Entry<String>> it = data.iterator();
		while (it.hasNext()) {
			ObjectIntMap.Entry<String> item = it.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testObjectLongMapIterator() {
		ObjectLongMap<String> data = new ObjectLongMap<>(strings, longs);
		int counter = 0, size = data.size();
		for(ObjectLongMap.Entry<String> item : data){
			Assert.assertNotNull(item);
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<ObjectLongMap.Entry<String>> it = data.iterator();
		while (it.hasNext()) {
			ObjectLongMap.Entry<String> item = it.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testObjectLongOrderedMapIterator() {
		ObjectLongOrderedMap<String> data = new ObjectLongOrderedMap<>(strings, longs);
		int counter = 0, size = data.size();
		for(ObjectLongMap.Entry<String> item : data){
			Assert.assertNotNull(item);
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<ObjectLongMap.Entry<String>> it = data.iterator();
		while (it.hasNext()) {
			ObjectLongMap.Entry<String> item = it.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testObjectFloatMapIterator() {
		ObjectFloatMap<String> data = new ObjectFloatMap<>(strings, floats);
		int counter = 0, size = data.size();
		for(ObjectFloatMap.Entry<String> item : data){
			Assert.assertNotNull(item);
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<ObjectFloatMap.Entry<String>> it = data.iterator();
		while (it.hasNext()) {
			ObjectFloatMap.Entry<String> item = it.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testObjectFloatOrderedMapIterator() {
		ObjectFloatOrderedMap<String> data = new ObjectFloatOrderedMap<>(strings, floats);
		int counter = 0, size = data.size();
		for(ObjectFloatMap.Entry<String> item : data){
			Assert.assertNotNull(item);
			++counter;
		}
		Assert.assertEquals(size, counter);
		counter = 0;
		Iterator<ObjectFloatMap.Entry<String>> it = data.iterator();
		while (it.hasNext()) {
			ObjectFloatMap.Entry<String> item = it.next();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testByteListIterator() {
		ByteList data = ByteList.with(bytes);
		int counter = 0, size = data.size();
		ByteIterator it = data.iterator();
		while (it.hasNext()) {
			byte item = it.nextByte();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testShortListIterator() {
		ShortList data = ShortList.with(shorts);
		int counter = 0, size = data.size();
		ShortIterator it = data.iterator();
		while (it.hasNext()) {
			short item = it.nextShort();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testCharListIterator() {
		CharList data = CharList.with(chars);
		int counter = 0, size = data.size();
		CharIterator it = data.iterator();
		while (it.hasNext()) {
			char item = it.nextChar();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testFloatListIterator() {
		FloatList data = FloatList.with(floats);
		int counter = 0, size = data.size();
		FloatIterator it = data.iterator();
		while (it.hasNext()) {
			float item = it.nextFloat();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testIntListIterator() {
		IntList data = IntList.with(ints);
		int counter = 0, size = data.size();
		PrimitiveIterator.OfInt it = data.iterator();
		while (it.hasNext()) {
			int item = it.nextInt();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testLongListIterator() {
		LongList data = LongList.with(longs);
		int counter = 0, size = data.size();
		PrimitiveIterator.OfLong it = data.iterator();
		while (it.hasNext()) {
			long item = it.nextLong();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testDoubleListIterator() {
		DoubleList data = DoubleList.with(doubles);
		int counter = 0, size = data.size();
		PrimitiveIterator.OfDouble it = data.iterator();
		while (it.hasNext()) {
			double item = it.nextDouble();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}
	
	@Test
	public void testBooleanListIterator() {
		BooleanList data = BooleanList.with(booleans);
		int counter = 0, size = data.size();
		BooleanIterator it = data.iterator();
		while (it.hasNext()) {
			boolean item = it.nextBoolean();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testByteDequeIterator() {
		ByteDeque data = ByteDeque.with(bytes);
		int counter = 0, size = data.size();
		ByteIterator it = data.iterator();
		while (it.hasNext()) {
			byte item = it.nextByte();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testShortDequeIterator() {
		ShortDeque data = ShortDeque.with(shorts);
		int counter = 0, size = data.size();
		ShortIterator it = data.iterator();
		while (it.hasNext()) {
			short item = it.nextShort();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testCharDequeIterator() {
		CharDeque data = CharDeque.with(chars);
		int counter = 0, size = data.size();
		CharIterator it = data.iterator();
		while (it.hasNext()) {
			char item = it.nextChar();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testFloatDequeIterator() {
		FloatDeque data = FloatDeque.with(floats);
		int counter = 0, size = data.size();
		FloatIterator it = data.iterator();
		while (it.hasNext()) {
			float item = it.nextFloat();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testIntDequeIterator() {
		IntDeque data = IntDeque.with(ints);
		int counter = 0, size = data.size();
		PrimitiveIterator.OfInt it = data.iterator();
		while (it.hasNext()) {
			int item = it.nextInt();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testLongDequeIterator() {
		LongDeque data = LongDeque.with(longs);
		int counter = 0, size = data.size();
		PrimitiveIterator.OfLong it = data.iterator();
		while (it.hasNext()) {
			long item = it.nextLong();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}

	@Test
	public void testDoubleDequeIterator() {
		DoubleDeque data = DoubleDeque.with(doubles);
		int counter = 0, size = data.size();
		PrimitiveIterator.OfDouble it = data.iterator();
		while (it.hasNext()) {
			double item = it.nextDouble();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}
	
	@Test
	public void testBooleanDequeIterator() {
		BooleanDeque data = BooleanDeque.with(booleans);
		int counter = 0, size = data.size();
		BooleanIterator it = data.iterator();
		while (it.hasNext()) {
			boolean item = it.nextBoolean();
			++counter;
		}
		Assert.assertEquals(size, counter);
	}
}
package com.github.tommyettinger.ds.test;

import com.github.tommyettinger.ds.ObjectObjectMap;
import org.junit.Test;

public class ShrinkTest {
	@Test
	public void testObjectMapShrink() {
		ObjectObjectMap<String, Integer> points = new ObjectObjectMap<String, Integer>();
		for (int x = -10; x <= 10; x++) {
			for (int y = -10; y <= 10; y++) {
				//// A better option if you want to actually remove entries past a size limit.
//				Iterator<ObjectMap.Entry<String, Integer>> it = points.iterator();
//				for(; points.size >= 100 && it.hasNext(); )
//				{
//					it.next();
//					it.remove();
//				}
				//// What we're testing.
				points.shrink(100);

				String k = x + "," + y;

				//// This line can end up in an infinite loop if the keyTable is 100% full.
				//// Normal Map API usage should never allow that, but shrink() could, maybe.
				if(!points.containsKey(k))
					points.put(k, points.size());
				if(points.size() >= 127)
					System.out.println(points.size() + ": " + points);
			}
		}
		System.out.println(points);
	}
}

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

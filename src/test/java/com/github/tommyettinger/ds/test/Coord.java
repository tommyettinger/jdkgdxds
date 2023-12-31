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

import com.github.tommyettinger.digital.MathTools;
import com.github.tommyettinger.ds.IntSet;

public class Coord {

	/** the x-component of this vector **/
	public final short x;
	/** the y-component of this vector **/
	public final short y;

	public final int hash;

	/** Constructs a new vector at (0,0) */
	public Coord () {
		x = 0;
		y = 0;
		hash = 0;
	}

	/** Constructs a vector with the given components
	 * @param x The x-component
	 * @param y The y-component */
	public Coord (int x, int y) {
		this.x = (short)x;
		this.y = (short)y;
		hash = this.x * 0x17587 + this.y * 0x16A89;
	}

	@Override
	public boolean equals (Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		Coord point2 = (Coord)o;

		if (x != point2.x)
			return false;
		return y == point2.y;
	}

	@Override
	public int hashCode () {
		return hash;
	}

	private static int hash(int x, int y, int mask) {
//		return x * 0x17587 + y * 0x16A89 & mask;
		return (y + ((x + y) * (x + y + 1) >>> 1)) * 0x9E3779B9 & mask;
	}
	public static void main(String[] args) {
		int LIMIT = 10000;
		int collisions = 0;
		IntSet ints = new IntSet(51, 0.5f)
//		{
//			@Override
//			protected int place (int item) {
//				return item & mask;
//			}
//		}
		;
		int mask = MathTools.nextPowerOfTwo((int)(LIMIT * LIMIT / 0.5f)) - 1;
		ints.add(0);
		int latest;
		for (int shell = 1; shell < LIMIT; shell++) {
			for (int high = 0; high <= shell; high++) {
				if(!ints.add(latest = hash(shell, high, mask)))
					System.out.println("Collision #" + (++collisions) + " with hash " + latest + " on x="+shell + ", y="+high);
			}
			for (int side = shell - 1; side >= 0; side--) {
				if(!ints.add(latest = hash(side, shell, mask)))
					System.out.println("Collision #" + (++collisions) + " with hash " + latest + " on x="+side + ", y="+shell);
			}
		}
		System.out.println("Total collisions: " + collisions);
	}
}

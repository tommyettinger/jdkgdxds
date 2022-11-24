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

public class Point2 {

	/** the x-component of this vector **/
	public int x;
	/** the y-component of this vector **/
	public int y;

	/** Constructs a new vector at (0,0) */
	public Point2 () {
	}

	/** Constructs a vector with the given components
	 * @param x The x-component
	 * @param y The y-component */
	public Point2 (int x, int y) {
		this.x = x;
		this.y = y;
	}

//	@Override
//	public int hashCode () {
//		final int prime = 31;
//		int result = 1;
//		result = prime * result + Float.floatToIntBits(x);
//		result = prime * result + Float.floatToIntBits(y);
//		return (int)(result * 0x9E3779B97F4A7C15L >>> 32);
//	}

	@Override
	public boolean equals (Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		Point2 point2 = (Point2)o;

		if (x != point2.x)
			return false;
		return y == point2.y;
	}

	@Override
	public int hashCode () {
		int result = x;
		result = 31 * result + y;
		return result;
	}
}

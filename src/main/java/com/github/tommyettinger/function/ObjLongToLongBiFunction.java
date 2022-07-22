/*
 * Copyright (c) 2012-2022 See AUTHORS file.
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

package com.github.tommyettinger.function;

import java.util.function.BiFunction;

/**
 * Represents a function that accepts an Object argument and a {@code long}
 * argument, and produces a long-valued result.  This is the {@code (reference, long)},
 * {@code long}-producing primitive specialization for {@link BiFunction}.
 *
 * <p>This is a <a href="package-summary.html">functional interface</a>
 * whose functional method is {@link #applyAsLong(Object, long)}.
 *
 * @param <T> the type of the object argument to the function
 * @see BiFunction
 */
@FunctionalInterface
public interface ObjLongToLongBiFunction<T> {

	/**
	 * Applies this function to the given arguments.
	 *
	 * @param first  the first function argument
	 * @param second the second function argument
	 * @return the function result
	 */
	long applyAsLong (T first, long second);
}

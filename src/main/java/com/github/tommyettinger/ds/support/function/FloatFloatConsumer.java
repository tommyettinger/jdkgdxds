/*
 * Copyright (c) 2013-2022 See AUTHORS file.
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

package com.github.tommyettinger.ds.support.function;

import java.util.function.BiConsumer;

/**
 * Represents an operation that accepts a {@code float}-valued and a
 * {@code float}-valued argument, and returns no result.  This is the
 * {@code (float, float)} specialization of {@link BiConsumer}.
 * Unlike most other functional interfaces, {@code FloatFloatConsumer} is
 * expected to operate via side-effects.
 *
 * <p>This is a <a href="package-summary.html">functional interface</a>
 * whose functional method is {@link #accept(float, float)}.
 *
 * @see BiConsumer
 */
@FunctionalInterface
public interface FloatFloatConsumer {

	/**
	 * Performs this operation on the given arguments.
	 *
	 * @param first  the first input argument
	 * @param second the second input argument
	 */
	void accept (float first, float second);
}

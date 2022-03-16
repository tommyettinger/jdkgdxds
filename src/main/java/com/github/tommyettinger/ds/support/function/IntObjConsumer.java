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

import javax.annotation.Nullable;
import java.util.function.BiConsumer;

/**
 * Represents an operation that accepts a {@code int}-valued and an
 * object-valued argument, and returns no result.  This is the
 * {@code (int, reference)} specialization of {@link BiConsumer}.
 * Unlike most other functional interfaces, {@code IntObjConsumer} is
 * expected to operate via side-effects.
 *
 * <p>This is a <a href="package-summary.html">functional interface</a>
 * whose functional method is {@link #accept(int, Object)}.
 *
 * @param <U> the type of the object argument to the operation
 * @see BiConsumer
 */
@FunctionalInterface
public interface IntObjConsumer<U> {

	/**
	 * Performs this operation on the given arguments.
	 *
	 * @param value the first input argument
	 * @param u     the second input argument
	 */
	void accept (int value, @Nullable U u);
}

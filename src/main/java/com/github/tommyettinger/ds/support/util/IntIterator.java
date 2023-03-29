/*
 * Copyright (c) 2013-2023 See AUTHORS file.
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

package com.github.tommyettinger.ds.support.util;

import com.github.tommyettinger.function.IntConsumer;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

/**
 * An Iterator specialized for {@code int} values.
 * This iterates over primitive ints using {@link #nextInt()}.
 * <br>
 * This is roughly equivalent to {@code PrimitiveIterator.OfInt} in Java 8, and is present here so environments
 * don't fully support Java 8 APIs (such as RoboVM) can use it.
 */
public interface IntIterator extends Iterator<Integer> {
	/**
	 * Returns the next {@code int} element in the iteration.
	 *
	 * @return the next {@code int} element in the iteration
	 * @throws NoSuchElementException if the iteration has no more elements
	 */
	int nextInt ();

	/**
	 * Performs the given action for each remaining element until all elements
	 * have been processed or the action throws an exception.  Actions are
	 * performed in the order of iteration, if that order is specified.
	 * Exceptions thrown by the action are relayed to the caller.
	 *
	 * @param action The action to be performed for each element
	 * @throws NullPointerException if the specified action is null
	 * @implSpec <p>The default implementation behaves as if:
	 * <pre>{@code
	 *     while (hasNext())
	 *         action.accept(nextInt());
	 * }</pre>
	 */
	default void forEachRemaining (IntConsumer action) {
		while (hasNext()) {action.accept(nextInt());}
	}

	/**
	 * {@inheritDoc}
	 *
	 * @implSpec The default implementation boxes the result of calling
	 * {@link #nextInt()}, and returns that boxed result.
	 */
	@Override
	default Integer next () {
		return nextInt();
	}

	/**
	 * {@inheritDoc}
	 *
	 * @implSpec If the action is an instance of {@code IntConsumer} then it is cast
	 * to {@code IntConsumer} and passed to {@link #forEachRemaining};
	 * otherwise the action is adapted to an instance of
	 * {@code IntConsumer}, by boxing the argument of {@code IntConsumer},
	 * and then passed to {@link #forEachRemaining}.
	 */
	@Override
	default void forEachRemaining (Consumer<? super Integer> action) {
		if (action instanceof IntConsumer) {
			forEachRemaining((IntConsumer)action);
		} else {
			forEachRemaining((IntConsumer)action::accept);
		}
	}

}
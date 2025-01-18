/*
 * Copyright (c) 2013-2025 See AUTHORS file.
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

package com.github.tommyettinger.ds.support.util;

import com.github.tommyettinger.function.LongConsumer;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An Iterator specialized for {@code long} values.
 * This iterates over primitive longs using {@link #nextLong()}.
 * <br>
 * This is roughly equivalent to {@code LongIterator} in Java 8, and is present here so environments
 * don't fully support Java 8 APIs (such as RoboVM) can use it.
 * <br>
 * This interface is closely based on {@code LongIterator} in OpenJDK 8.
 * This iterator interface is extremely simple and there's no way to implement it in a way
 * that respects compatibility other than the way OpenJDK 8 does.
 * <a href="https://github.com/openjdk/jdk/blob/d3f2498ed72089301a49ddf0bc7bd2df54368033/LICENSE">OpenJDK's
 * license is available here</a>,
 * if it applies at all.
 */
public interface LongIterator extends Iterator<Long> {
	/**
	 * Returns the next {@code long} element in the iteration.
	 *
	 * @return the next {@code long} element in the iteration
	 * @throws NoSuchElementException if the iteration has no more elements
	 */
	long nextLong ();

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
	 *         action.accept(nextLong());
	 * }</pre>
	 */
	default void forEachRemaining (LongConsumer action) {
		while (hasNext()) {action.accept(nextLong());}
	}

	/**
	 * {@inheritDoc}
	 *
	 * @implSpec The default implementation boxes the result of calling
	 * {@link #nextLong()}, and returns that boxed result.
	 */
	@Override
	default Long next () {
		return nextLong();
	}
}
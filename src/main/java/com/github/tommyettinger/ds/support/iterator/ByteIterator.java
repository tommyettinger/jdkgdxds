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

package com.github.tommyettinger.ds.support.iterator;

import com.github.tommyettinger.function.ByteConsumer;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An Iterator specialized for {@code byte} values.
 * This iterates over primitive bytes using {@link #nextByte()}.
 * <br>
 * This interface is loosely based on a similar interface in OpenJDK 8.
 * This iterator interface is extremely simple and there's no way to implement it in a way
 * that respects compatibility other than the way OpenJDK 8 does.
 * <a href="https://github.com/openjdk/jdk/blob/d3f2498ed72089301a49ddf0bc7bd2df54368033/LICENSE">OpenJDK's
 * license is available here</a>,
 * if it applies at all.
 */
public interface ByteIterator extends Iterator<Byte> {
	/**
	 * Returns the next {@code byte} element in the iteration.
	 *
	 * @return the next {@code byte} element in the iteration
	 * @throws NoSuchElementException if the iteration has no more elements
	 */
	byte nextByte ();

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
	 *         action.accept(nextByte());
	 * }</pre>
	 */
	default void forEachRemaining (ByteConsumer action) {
		while (hasNext()) {action.accept(nextByte());}
	}

	/**
	 * {@inheritDoc}
	 *
	 * @implSpec The default implementation boxes the result of calling
	 * {@link #nextByte()}, and returns that boxed result.
	 */
	@Override
	default Byte next () {
		return nextByte();
	}
}
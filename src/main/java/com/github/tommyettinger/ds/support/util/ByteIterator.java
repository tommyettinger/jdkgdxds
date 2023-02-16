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

import com.github.tommyettinger.function.ByteConsumer;

import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.function.Consumer;

/**
 * An Iterator specialized for {@code byte} values.
 * This is a {@link PrimitiveIterator}, like the existing {@link OfInt}
 * and {@link OfLong} interfaces, but it can't be a nested interface like
 * those because it is defined outside of the JDK.
 */
public interface ByteIterator extends PrimitiveIterator<Byte, ByteConsumer> {
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
	@Override
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

	/**
	 * {@inheritDoc}
	 *
	 * @implSpec If the action is an instance of {@code LongConsumer} then it is cast
	 * to {@code LongConsumer} and passed to {@link #forEachRemaining};
	 * otherwise the action is adapted to an instance of
	 * {@code LongConsumer}, by boxing the argument of {@code LongConsumer},
	 * and then passed to {@link #forEachRemaining}.
	 */
	@Override
	default void forEachRemaining (Consumer<? super Byte> action) {
		if (action instanceof ByteConsumer) {
			forEachRemaining((ByteConsumer)action);
		} else {
			forEachRemaining((ByteConsumer)action::accept);
		}
	}

}
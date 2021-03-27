/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.github.tommyettinger.ds.support.util;

import com.github.tommyettinger.ds.support.function.CharConsumer;

import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.function.Consumer;

/**
 * An Iterator specialized for {@code char} values.
 * This is a {@link PrimitiveIterator}, like the existing {@link OfInt}
 * and {@link OfLong} interfaces, but it can't be a nested interface like
 * those because it is defined outside of the JDK.
 */
public interface CharIterator extends PrimitiveIterator<Character, CharConsumer> {
	/**
	 * Returns the next {@code char} element in the iteration.
	 *
	 * @return the next {@code char} element in the iteration
	 * @throws NoSuchElementException if the iteration has no more elements
	 */
	char nextChar ();

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
	 *         action.accept(nextChar());
	 * }</pre>
	 */
	@Override
	default void forEachRemaining (CharConsumer action) {
		while (hasNext()) { action.accept(nextChar()); }
	}

	/**
	 * {@inheritDoc}
	 *
	 * @implSpec The default implementation boxes the result of calling
	 * {@link #nextChar()}, and returns that boxed result.
	 */
	@Override
	default Character next () {
		return nextChar();
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
	default void forEachRemaining (Consumer<? super Character> action) {
		if (action instanceof CharConsumer) {
			forEachRemaining((CharConsumer)action);
		} else {
			forEachRemaining((CharConsumer)action::accept);
		}
	}

}
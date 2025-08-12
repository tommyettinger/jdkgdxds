/*
 * Copyright (c) 2025 See AUTHORS file.
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

import com.github.tommyettinger.function.BooleanPredicate;

import java.util.NoSuchElementException;

/**
 * Wraps an Iterator so that it calls a function on each item it would otherwise return, and returns that function's
 * result. The function in this is actually a predicate, since all function types in Funderby that return booleans are
 * considered predicates.
 * If you need to return a different type than what the iterator returns, you can use an {@link AlteringIterator}.
 * This has undefined behavior if any items the Iterator could return are modified during iteration.
 * <br>
 * You can change both the Iterator and editor at once using {@link #set(BooleanIterator, BooleanPredicate)} ,
 * and can also just change the Iterator with {@link #set(BooleanIterator)}.
 */
public class EditingBooleanIterator implements BooleanIterator {
	public BooleanIterator iterator;
	public BooleanPredicate editor;

	public EditingBooleanIterator() {
	}

	public EditingBooleanIterator(final BooleanIterator iterator, final BooleanPredicate editor) {
		set(iterator, editor);
	}

	public void set(final BooleanIterator iterator, final BooleanPredicate editor) {
		this.iterator = iterator;
		this.editor = editor;
	}

	public void set(final BooleanIterator iterator) {
		set(iterator, editor);
	}

	@Override
	public boolean hasNext() {
		return iterator.hasNext();
	}

	@Override
	public boolean nextBoolean() {
		if (!hasNext()) throw new NoSuchElementException("No elements remaining.");
		return editor.test(iterator.nextBoolean());
	}

	@Override
	public void remove() {
		iterator.remove();
	}
}

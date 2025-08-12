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

import com.github.tommyettinger.function.FloatToFloatFunction;

import java.util.NoSuchElementException;

/**
 * Wraps an Iterator so that it calls a function on each item it would otherwise return, and returns that function's
 * result.
 * If you need to return a different type than what the iterator returns, you can use an {@link AlteringIterator}.
 * This has undefined behavior if any items the Iterator could return are modified during iteration.
 * <br>
 * You can change both the Iterator and editor at once using {@link #set(FloatIterator, FloatToFloatFunction)} ,
 * and can also just change the Iterator with {@link #set(FloatIterator)}.
 */
public class EditingFloatIterator implements FloatIterator {
	public FloatIterator iterator;
	public FloatToFloatFunction editor;

	public EditingFloatIterator() {
	}

	public EditingFloatIterator(final FloatIterator iterator, final FloatToFloatFunction editor) {
		set(iterator, editor);
	}

	public void set(final FloatIterator iterator, final FloatToFloatFunction editor) {
		this.iterator = iterator;
		this.editor = editor;
	}

	public void set(final FloatIterator iterator) {
		set(iterator, editor);
	}

	@Override
	public boolean hasNext() {
		return iterator.hasNext();
	}

	@Override
	public float nextFloat() {
		if (!hasNext()) throw new NoSuchElementException("No elements remaining.");
		return editor.applyAsFloat(iterator.nextFloat());
	}

	@Override
	public void remove() {
		iterator.remove();
	}
}

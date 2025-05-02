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

package com.github.tommyettinger.ds;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.Deque;
import java.util.List;

/**
 * A combination List/Deque with some expanded features based on Deque's mix of exceptional and non-exceptional methods.
 * @param <T> the generic type of items
 */
public interface Lisque<@Nullable T> extends List<T>, Deque<T> {
    boolean insert (int index, T item);
    boolean insertAll(int index, Collection<@Nullable ? extends T> c);
    boolean addAllLast (Collection<? extends T> c);
    boolean addAllFirst (Collection<? extends T> c);

}

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
    default boolean insert (int index, T item) {
        add(index, item);
        return true;
    }
    default boolean insertAll(int index, Collection<@Nullable ? extends T> c){
        return addAll(index, c);
    }
    default boolean addAllLast (Collection<? extends T> c) {
        return addAll(c);
    }
    boolean addAllFirst (Collection<? extends T> c);
    boolean addAll (T[] array);
    boolean addAll (T[] array, int offset, int length);
    boolean addAll (int index, T[] array);
    boolean addAll (int index, T[] array, int offset, int length);
    default boolean insertAll (int index, T[] array) {
        return addAll(index, array);
    }
    default boolean insertAll (int index, T[] array, int offset, int length){
        return addAll(index, array, offset, length);
    }
    default boolean addAllLast (T[] array) {
        return addAll(array);
    }
    default boolean addAllLast (T[] array, int offset, int length) {
        return addAll(array, offset, length);
    }
    boolean addAllFirst (T[] array);
    boolean addAllFirst (T[] array, int offset, int length);

}

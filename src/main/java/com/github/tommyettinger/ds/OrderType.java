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

/**
 * Used to determine what class Ordered objects will use for their {@link Ordered#order()} implementation. This is
 * always a subclass of the type returned by order(), such as {@link ObjectList} for Ordered or {@link LongList} for
 * {@link Ordered.OfLong}. {@link ObjectList} only has {@link ObjectBag} as its subclass here, but the primitive lists
 * such as {@link IntList} have {@link IntBag} and {@link IntDeque} as subclasses that may be used.
 * <br>
 * Each ordering type has different advantages and disadvantages. Each variety offers constant-time
 * {@link ObjectList#get(int)} performance, and amortized constant-time {@link ObjectList#add(Object)} to the end.
 * {@link #DEQUE} has slower get(int) performance in practice than the others, but not worse algorithmic complexity.
 * Adding to the beginning of the iteration order is constant-time for {@link #DEQUE}, but linear-time for
 * {@link #LIST} and {@link #BAG}. Both {@link #LIST} and {@link #DEQUE} preserve insertion order, but {@link #BAG} is
 * permitted to rearrange items whenever an item is removed. Removal performs very differently. with {@link #LIST}
 * removing in constant time only for the last item in the iteration order, or linear time for any other item.
 * {@link #DEQUE} removes items at the start or end of the order in constant-time, and linear time for other positions.
 * {@link #BAG} has constant-time removal from any position by swapping the item at the end into the place of the
 * removed item. All types permit sorting, but {@link #BAG} only holds its sorted order until an item is removed. All
 * types have faster iteration than an unordered set or map.
 * <br>
 * The reason to use {@link #LIST} is to preserve iteration order and allow fast get() by index. {@link #DEQUE} also
 * preserves iteration order and makes insertion or removal at either end of the order faster. {@link #BAG} allows sets
 * and maps to keep or improve on the complexity of an unordered set or map for all operations, while speeding up
 * iteration as long as the order doesn't need to be kept between iterations over the type/.
 * <br>
 * If a type of ordering isn't available for a given item type, this should default to {@link #LIST}.
 */
public enum OrderType {
    /**
     * The {@code order()} method will return a list, such as {@link ObjectList} or {@link IntList}.
     */
    LIST,
    /**
     * The {@code order()} method will return a bag with non-persistent iteration order, such as {@link ObjectBag}
     * or {@link IntBag}.
     */
    BAG,
    /**
     * The {@code order()} method will return a deque (which is also always a list here), such as {@link LongDeque}
     * or {@link IntDeque}. This will not be valid for Object-based types, because {@link ObjectDeque} is not a subclass
     * of {@link ObjectList}.
     */
    DEQUE;
}

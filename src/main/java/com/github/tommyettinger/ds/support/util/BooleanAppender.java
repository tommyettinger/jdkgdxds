/*
 * Copyright (c) 2024-2025 See AUTHORS file.
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

import com.github.tommyettinger.digital.Base;
import com.github.tommyettinger.function.ObjBooleanToObjBiFunction;

/**
 * A convenience wrapper around an {@link ObjBooleanToObjBiFunction} that takes and returns a StringBuilder, as well as taking a {@code boolean}.
 * This is not typically a method reference to anything in {@link Base}, which is different from other Appender types.
 */
public interface BooleanAppender extends ObjBooleanToObjBiFunction<StringBuilder, StringBuilder> {
    /**
     * A static constant to avoid Android and its R8 compiler allocating a new lambda every time
     * {@code StringBuilder::append} is present at a call-site. This should be used in place of
     * {@link StringBuilder#append(boolean)} when you want to use that as a BooleanAppender.
     */
    BooleanAppender DEFAULT = StringBuilder::append;

    /**
     * An alternative BooleanAppender constant that appends {@code '1'} when the given boolean item is
     * true, or {@code '0'} when it is false. This is named differently from the {@code DENSE} method
     * reference in other Appender functional interfaces because there's no need or ability to use
     * base-90 digits to show true and false values densely.
     * <br>
     * This is a static constant to avoid Android and its R8 compiler allocating a new lambda every time
     * this lambda would be present at a call-site.
     */
    BooleanAppender BINARY = (StringBuilder sb, boolean item) -> sb.append(item ? '1' : '0');
}

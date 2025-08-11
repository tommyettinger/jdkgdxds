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
import com.github.tommyettinger.function.ObjLongToObjBiFunction;

/**
 * A convenience wrapper around an {@link ObjLongToObjBiFunction} that takes and returns a StringBuilder, as well as taking a {@code long}.
 * This is often a method reference to a method in {@link Base}, such as {@link Base#appendSigned(CharSequence, long)}.
 */
public interface LongAppender extends ObjLongToObjBiFunction<StringBuilder, StringBuilder> {
    /**
     * A static constant to avoid Android and its R8 compiler allocating a new lambda every time
     * {@code StringBuilder::append} is present at a call-site. This should be used in place of
     * {@link StringBuilder#append(long)} when you want to use that as a LongAppender.
     */
    LongAppender DEFAULT = StringBuilder::append;
    
    /**
     * An alternative LongAppender constant that appends ten {@link Base#BASE90} digits for every long input.
     * The ten ASCII chars are not expected to be human-readable.
     * <br>
     * This is a static constant to avoid Android and its R8 compiler allocating a new lambda every time
     * this lambda would be present at a call-site.
     */
    LongAppender DENSE = Base.BASE90::appendUnsigned;
}

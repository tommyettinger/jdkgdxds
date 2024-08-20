/*
 * Copyright (c) 2024 See AUTHORS file.
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

package com.github.tommyettinger.ds.support.text;

import com.github.tommyettinger.digital.Base;
import com.github.tommyettinger.function.ObjCharToObjBiFunction;

/**
 * A convenience wrapper around an {@link ObjCharToObjBiFunction} that takes and returns a StringBuilder, as well as taking a {@code char}.
 * This is often a method reference to a method in {@link Base}, such as {@link Base#appendSigned(StringBuilder, char)}.
 */
public interface CharAppender extends ObjCharToObjBiFunction<StringBuilder, StringBuilder> {
}

/*
 * Copyright (c) 2022-2023 See AUTHORS file.
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

/**
 * Fills in code missing from the java.util package; this package is separate
 * in an attempt to keep a clean break between the rest of the library and
 * code based on OpenJDK's GPL v2 (with classpath exception)-licensed code.
 * <br>
 * The interface in this package is extremely simple and there's
 * no way to implement it in a way that respects compatibility other
 * than the way OpenJDK 8 does.
 * <a href="https://github.com/openjdk/jdk/blob/d3f2498ed72089301a49ddf0bc7bd2df54368033/LICENSE">OpenJDK's
 * license is available here</a>, if there's any confusion. If
 * required, this entire package can be moved to an external
 * dependency, which I believe would be unambiguously permitted by
 * the classpath exception.
 */

package com.github.tommyettinger.ds.support.util;


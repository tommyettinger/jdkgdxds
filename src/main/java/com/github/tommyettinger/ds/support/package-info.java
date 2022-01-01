/*
 * Copyright (c) 2022 See AUTHORS file.
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
 * Code that isn't directly related to data structures, but supports their implementation.
 * <br>
 * Importantly, contains {@link com.github.tommyettinger.ds.support.BitConversion}, which
 * allows certain bit-manipulation methods to work the same on GWT and other platforms.
 * This also has {@link com.github.tommyettinger.ds.support.EnhancedRandom} and its primary
 * implementation, {@link com.github.tommyettinger.ds.support.LaserRandom}, which together
 * define and implement a superset of {@link java.util.Random}'s API that can be used by
 * various new classes.
 */

@NotNullDefault
package com.github.tommyettinger.ds.support;

import com.github.tommyettinger.ds.annotations.NotNullDefault;

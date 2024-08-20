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
 * Code for customized sorting on primitive arrays and collections. With some exceptions (namely,
 * {@link com.github.tommyettinger.ds.support.sort.ObjectComparators#comparingFloat(com.github.tommyettinger.function.ObjToFloatFunction)}
 * and {@link com.github.tommyettinger.ds.support.sort.FilteredComparators}, which are original to jdkgdxds, as well as
 * {@link com.github.tommyettinger.ds.support.sort.NaturalTextComparator}, which is based on the Apache-licensed
 * <a href="https://github.com/gpanther/java-nat-sort">java-nat-sort</a>), this is
 * Apache-licensed code from the <a href="https://github.com/vigna/fastutil">FastUtil project</a> with minimal changes.
 * Like fastutil, jdkgdxds is Apache-licensed, but if you want to refer to FastUtil's license,
 * <a href="https://github.com/vigna/fastutil/blob/2411defb3b1bd7f004ea75a7706ccaebb4dbfc25/LICENSE-2.0">it is here</a>.
 */
package com.github.tommyettinger.ds.support.sort;


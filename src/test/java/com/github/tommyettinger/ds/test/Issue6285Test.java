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

package com.github.tommyettinger.ds.test;

import com.github.tommyettinger.ds.IntIntMap;
import com.github.tommyettinger.ds.IntObjectMap;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for <a href="https://github.com/libgdx/libgdx/issues/6285">libGDX issue #6285</a>.
 */
public class Issue6285Test {
    @Test
    public void testIntObjectMap6285() {
        IntObjectMap<Integer> map = new IntObjectMap<>();
        Integer value = 123;
        map.put(1, value);
        map.remove(1);

        Assert.assertFalse(map.containsValue(value));
    }

    @Test
    public void testIntIntMap6285() {
        IntIntMap map = new IntIntMap();
        int value = 123;
        map.put(1, value);
        map.remove(1);

        Assert.assertFalse(map.containsValue(value));
    }
}

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

import com.github.tommyettinger.ds.LongSet;

public class UnrelatedTests {
    public static void main(String[] args) {
        // 10700330 unique results in mask
        testMaskedUniquenessCounter();
    }

    public static void testMaskedUniquenessCounter() {
        long mask = 0x003569CA5369AC00L;
        LongSet all = new LongSet(1 << 25, 0.5f);
        long ctr = 0;
        for (int i = 0, n = 1 << 24; i < n; i++) {
            all.add(ctr & mask);
            ctr += mask;
        }
        System.out.println(all.size());
    }
}

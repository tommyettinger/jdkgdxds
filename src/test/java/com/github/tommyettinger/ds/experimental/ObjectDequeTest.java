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

package com.github.tommyettinger.ds.experimental;

import com.github.tommyettinger.digital.TextTools;
import org.junit.Test;

public class ObjectDequeTest {
    public void show(ObjectDeque<?> od){
        System.out.println(od + "  " + TextTools.join(" ", od.values) + "  with head: " + od.head + ", tail: " + od.tail);
        System.out.println();
    }

    public ObjectDeque<String> makeLinearNoGaps() {
        String[] alphabet = "A B C D E F G H I J K L M N O P".split(" ");
//        System.out.println(TextTools.join(" ", alphabet));
        ObjectDeque<String> od = new ObjectDeque<>(alphabet);
        return od;
    }

    @Test
    public void testBasics() {
        ObjectDeque<String> od = makeLinearNoGaps();
        show(od);
        od.pollFirst();
        od.addLast("Q");
        show(od);
        od.removeLast();
        show(od);
        od.removeLast();
        od.addFirst("A");
        od.addFirst("@");
        show(od);
    }
}

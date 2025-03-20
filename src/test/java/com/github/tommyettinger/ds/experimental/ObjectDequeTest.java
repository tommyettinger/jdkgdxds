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
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayDeque;

public class ObjectDequeTest {
    public void show(ObjectDeque<?> od){
        System.out.println(od.toString() + "  " + TextTools.join(" ", od.values) + "  with head: " + od.head + ", tail: " + od.tail);
        System.out.println();
    }

    public ObjectDeque<String> makeLinearNoGaps() {
        String[] alphabet = "A B C D E F G H I J K L M N O P".split(" ");
        ObjectDeque<String> od = new ObjectDeque<>(alphabet);
        return od;
    }

    public ObjectDeque<String> makeLinearGapsAtEnds() {
        String[] alphabet = "A B C D E F G H I J K L M N O P".split(" ");
        ObjectDeque<String> od = new ObjectDeque<>(alphabet);
        od.truncate(14);
        od.removeRange(0, 2);
        return od;
    }

    public ObjectDeque<String> makeWrapAroundFull() {
        String[] alphabet = "N O P A B C D E F G H I J K L M".split(" ");
        ObjectDeque<String> od = new ObjectDeque<>(alphabet);
        od.head = 3;
        od.tail = 3;
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

    @Test
    public void testGapsAtEnds() {
        ObjectDeque<String> od = makeLinearGapsAtEnds();
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

    @Test
    public void testTruncate() {
        ObjectDeque<String> od = makeWrapAroundFull();
        show(od);
        od.truncate(14);
        show(od);
        od.truncateFirst(12);
        show(od);
        od.truncateFirst(1);
        show(od);
        od.addFirst("M");
        od.addFirst("L");
        od.addLast("O");
        show(od);
        od.truncateFirst(1);
        show(od);
        od.addFirst("N");
        od.addFirst("M");
        od.addFirst("L");
        od.addLast("P");
        show(od);
        od.truncate(1);
        show(od);
    }

    @Test
    public void testRemoveRange() {
        ObjectDeque<String> od = makeWrapAroundFull();
        show(od);
        od.removeRange(11, 14);
        show(od);
        od.insert(11, "L");
        od.insert(12, "M");
        od.insert(14, "o");
        show(od);
        System.out.println();
        od = makeWrapAroundFull();
        show(od);
        od.removeRange(13, 15);
        show(od);
        System.out.println();
        od = makeLinearNoGaps();
        show(od);
        od.removeRange(11, 14);
        show(od);
        od.insert(11, "L");
        od.insert(12, "M");
        od.insert(14, "o");
        show(od);
        System.out.println();
        od = makeLinearGapsAtEnds();
        show(od);
        od.removeRange(8, 11);
        show(od);
        od.insert(8, "K");
        od.insert(9, "L");
        od.insert(11, "n");
        show(od);
    }

    @Test
    public void testEquals() {
        {
            ObjectDeque<String> od = makeLinearNoGaps();
            show(od);
            ArrayDeque<String> ad = new ArrayDeque<>(16);
            ad.addAll(od);
            System.out.println("Does od equal ad? " + od.equals(ad));
            Assert.assertEquals(od, ad);
            System.out.println("Does ad equal od? " + ad.equals(od));
        }
        {
            ObjectDeque<String> od = makeLinearNoGaps();
            show(od);
            ArrayDeque<String> ad = new ArrayDeque<>(16);
            ad.addAll(od);
            System.out.println("Does od equal ad? " + od.equals(ad));
            Assert.assertEquals(od, ad);
            System.out.println("Does ad equal od? " + ad.equals(od));
        }
        {
            ObjectDeque<String> od = makeWrapAroundFull();
            show(od);
            ArrayDeque<String> ad = new ArrayDeque<>(16);
            ad.addAll(od);
            System.out.println("Does od equal ad? " + od.equals(ad));
            Assert.assertEquals(od, ad);
            System.out.println("Does ad equal od? " + ad.equals(od));

        }
    }
}

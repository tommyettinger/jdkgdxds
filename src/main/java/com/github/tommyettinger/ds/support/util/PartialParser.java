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

package com.github.tommyettinger.ds.support.util;

import com.github.tommyettinger.ds.Junction;
import com.github.tommyettinger.ds.PrimitiveCollection;
import com.github.tommyettinger.function.ObjSupplier;

/**
 * A functional interface to parse part of a String and obtain a {@code R} instance as a result.
 * This is a functional interface whose functional method is {@link #parse(String, int, int)}
 * @param <R> the type to produce
 */
public interface PartialParser<R> {
    /**
     * Creates or obtains an {@code R} object by parsing a section of {@code text} starting at {@code start} (inclusive)
     * and ending at {@code end} (exclusive) in text.
     * @param text a String containing a section that will be parsed
     * @param start the first character index to parse in text, inclusive
     * @param end the last character index to parse in text, exclusive
     * @return a (typically new) {@code R} object loaded from the given section of text
     */
    R parse(String text, int start, int end);

    /**
     * Wraps {@link String#substring(int, int)}.
     */
    PartialParser<String> DEFAULT_STRING = String::substring;


    /**
     * Wraps {@link Junction#parse(String, int, int)}.
     */
    PartialParser<Junction<String>> DEFAULT_JUNCTION_STRING = Junction::parse;

    /**
     * Creates a PartialParser that can parse a section of text with multiple int items separated by {@code delimiter},
     * creates a PrimitiveCollection using the given supplier, populates it with
     * {@link PrimitiveCollection.OfInt#addLegible(String, String, int, int)}, and returns the new collection.
     * @param supplier typically a constructor reference, which can be stored for better Android performance
     * @param delimiter the String that separates items in the expected text
     * @param brackets if true, the text will be expected to be surrounded by one bracket char on each side, which will be ignored
     * @return a (typically new) PrimitiveCollection loaded from the given text
     * @param <C> a PrimitiveCollection.OfInt type such as IntList
     */
    static <C extends PrimitiveCollection.OfInt> PartialParser<C> intCollectionParser(final ObjSupplier<C> supplier, final String delimiter, final boolean brackets) {
        return (String text, int start, int end) -> {
            final C coll = supplier.get();
            if(brackets)
                coll.addLegible(text, delimiter, start + 1, end - start - 2);
            else
                coll.addLegible(text, delimiter, start, end - start);
            return coll;
        };
    }

    /**
     * Creates a PartialParser that can parse a section of text with multiple long items separated by {@code delimiter},
     * creates a PrimitiveCollection using the given supplier, populates it with
     * {@link PrimitiveCollection.OfLong#addLegible(String, String, int, int)}, and returns the new collection.
     * @param supplier typically a constructor reference, which can be stored for better Android performance
     * @param delimiter the String that separates items in the expected text
     * @param brackets if true, the text will be expected to be surrounded by one bracket char on each side, which will be ignored
     * @return a (typically new) PrimitiveCollection loaded from the given text
     * @param <C> a PrimitiveCollection.OfLong type such as LongList
     */
    static <C extends PrimitiveCollection.OfLong> PartialParser<C> longCollectionParser(final ObjSupplier<C> supplier, final String delimiter, final boolean brackets) {
        return (String text, int start, int end) -> {
            final C coll = supplier.get();
            if(brackets)
                coll.addLegible(text, delimiter, start + 1, end - start - 2);
            else
                coll.addLegible(text, delimiter, start, end - start);
            return coll;
        };
    }

    /**
     * Creates a PartialParser that can parse a section of text with multiple float items separated by {@code delimiter},
     * creates a PrimitiveCollection using the given supplier, populates it with
     * {@link PrimitiveCollection.OfFloat#addLegible(String, String, int, int)}, and returns the new collection.
     * @param supplier typically a constructor reference, which can be stored for better Android performance
     * @param delimiter the String that separates items in the expected text
     * @param brackets if true, the text will be expected to be surrounded by one bracket char on each side, which will be ignored
     * @return a (typically new) PrimitiveCollection loaded from the given text
     * @param <C> a PrimitiveCollection.OfFloat type such as FloatList
     */
    static <C extends PrimitiveCollection.OfFloat> PartialParser<C> floatCollectionParser(final ObjSupplier<C> supplier, final String delimiter, final boolean brackets) {
        return (String text, int start, int end) -> {
            final C coll = supplier.get();
            if(brackets)
                coll.addLegible(text, delimiter, start + 1, end - start - 2);
            else
                coll.addLegible(text, delimiter, start, end - start);
            return coll;
        };
    }

    /**
     * Creates a PartialParser that can parse a section of text with multiple double items separated by {@code delimiter},
     * creates a PrimitiveCollection using the given supplier, populates it with
     * {@link PrimitiveCollection.OfDouble#addLegible(String, String, int, int)}, and returns the new collection.
     * @param supplier typically a constructor reference, which can be stored for better Android performance
     * @param delimiter the String that separates items in the expected text
     * @param brackets if true, the text will be expected to be surrounded by one bracket char on each side, which will be ignored
     * @return a (typically new) PrimitiveCollection loaded from the given text
     * @param <C> a PrimitiveCollection.OfDouble type such as DoubleList
     */
    static <C extends PrimitiveCollection.OfDouble> PartialParser<C> doubleCollectionParser(final ObjSupplier<C> supplier, final String delimiter, final boolean brackets) {
        return (String text, int start, int end) -> {
            final C coll = supplier.get();
            if(brackets)
                coll.addLegible(text, delimiter, start + 1, end - start - 2);
            else
                coll.addLegible(text, delimiter, start, end - start);
            return coll;
        };
    }

    /**
     * Creates a PartialParser that can parse a section of text with multiple short items separated by {@code delimiter},
     * creates a PrimitiveCollection using the given supplier, populates it with
     * {@link PrimitiveCollection.OfShort#addLegible(String, String, int, int)}, and returns the new collection.
     * @param supplier typically a constructor reference, which can be stored for better Android performance
     * @param delimiter the String that separates items in the expected text
     * @param brackets if true, the text will be expected to be surrounded by one bracket char on each side, which will be ignored
     * @return a (typically new) PrimitiveCollection loaded from the given text
     * @param <C> a PrimitiveCollection.OfShort type such as ShortList
     */
    static <C extends PrimitiveCollection.OfShort> PartialParser<C> shortCollectionParser(final ObjSupplier<C> supplier, final String delimiter, final boolean brackets) {
        return (String text, int start, int end) -> {
            final C coll = supplier.get();
            if(brackets)
                coll.addLegible(text, delimiter, start + 1, end - start - 2);
            else
                coll.addLegible(text, delimiter, start, end - start);
            return coll;
        };
    }

    /**
     * Creates a PartialParser that can parse a section of text with multiple byte items separated by {@code delimiter},
     * creates a PrimitiveCollection using the given supplier, populates it with
     * {@link PrimitiveCollection.OfByte#addLegible(String, String, int, int)}, and returns the new collection.
     * @param supplier typically a constructor reference, which can be stored for better Android performance
     * @param delimiter the String that separates items in the expected text
     * @param brackets if true, the text will be expected to be surrounded by one bracket char on each side, which will be ignored
     * @return a (typically new) PrimitiveCollection loaded from the given text
     * @param <C> a PrimitiveCollection.OfByte type such as ByteList
     */
    static <C extends PrimitiveCollection.OfByte> PartialParser<C> byteCollectionParser(final ObjSupplier<C> supplier, final String delimiter, final boolean brackets) {
        return (String text, int start, int end) -> {
            final C coll = supplier.get();
            if(brackets)
                coll.addLegible(text, delimiter, start + 1, end - start - 2);
            else
                coll.addLegible(text, delimiter, start, end - start);
            return coll;
        };
    }

    /**
     * Creates a PartialParser that can parse a section of text with multiple char items separated by {@code delimiter},
     * creates a PrimitiveCollection using the given supplier, populates it with
     * {@link PrimitiveCollection.OfChar#addLegible(String, String, int, int)}, and returns the new collection.
     * @param supplier typically a constructor reference, which can be stored for better Android performance
     * @param delimiter the String that separates items in the expected text
     * @param brackets if true, the text will be expected to be surrounded by one bracket char on each side, which will be ignored
     * @return a (typically new) PrimitiveCollection loaded from the given text
     * @param <C> a PrimitiveCollection.OfChar type such as CharList
     */
    static <C extends PrimitiveCollection.OfChar> PartialParser<C> charCollectionParser(final ObjSupplier<C> supplier, final String delimiter, final boolean brackets) {
        return (String text, int start, int end) -> {
            final C coll = supplier.get();
            if(brackets)
                coll.addLegible(text, delimiter, start + 1, end - start - 2);
            else
                coll.addLegible(text, delimiter, start, end - start);
            return coll;
        };
    }

    /**
     * Creates a PartialParser that can parse a section of text with multiple boolean items separated by {@code delimiter},
     * creates a PrimitiveCollection using the given supplier, populates it with
     * {@link PrimitiveCollection.OfBoolean#addLegible(String, String, int, int)}, and returns the new collection.
     * @param supplier typically a constructor reference, which can be stored for better Android performance
     * @param delimiter the String that separates items in the expected text
     * @param brackets if true, the text will be expected to be surrounded by one bracket char on each side, which will be ignored
     * @return a (typically new) PrimitiveCollection loaded from the given text
     * @param <C> a PrimitiveCollection.OfBoolean type such as BooleanList
     */
    static <C extends PrimitiveCollection.OfBoolean> PartialParser<C> booleanCollectionParser(final ObjSupplier<C> supplier, final String delimiter, final boolean brackets) {
        return (String text, int start, int end) -> {
            final C coll = supplier.get();
            if(brackets)
                coll.addLegible(text, delimiter, start + 1, end - start - 2);
            else
                coll.addLegible(text, delimiter, start, end - start);
            return coll;
        };
    }
}

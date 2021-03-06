/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.github.tommyettinger.ds.support.function;

import javax.annotation.Nullable;
import java.util.function.BiConsumer;

/**
 * Represents an operation that accepts a {@code int}-valued and an
 * object-valued argument, and returns no result.  This is the
 * {@code (int, reference)} specialization of {@link BiConsumer}.
 * Unlike most other functional interfaces, {@code IntObjConsumer} is
 * expected to operate via side-effects.
 *
 * <p>This is a <a href="package-summary.html">functional interface</a>
 * whose functional method is {@link #accept(int, Object)}.
 *
 * @param <U> the type of the object argument to the operation
 *
 * @see BiConsumer
 */
@FunctionalInterface
public interface IntObjConsumer<U> {

    /**
     * Performs this operation on the given arguments.
     *
     * @param value the first input argument
     * @param u the second input argument
     */
    void accept(int value, @Nullable U u);
}

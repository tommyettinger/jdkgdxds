/**
 * Fills in code missing from the java.util.function package, mostly
 * with primitive-specialized functions. This package is separate in
 * an attempt to keep a clean break between the rest of the library
 * and code based on OpenJDK's GPL v2 (with classpath
 * exception)-licensed code.
 * <br>
 * The interfaces in this package are all extremely simple and there's
 * no way to implement them in a way that respects compatibility other
 * than the way OpenJDK 8 does.
 * <a href="https://github.com/openjdk/jdk/blob/d3f2498ed72089301a49ddf0bc7bd2df54368033/LICENSE">OpenJDK's
 * license is available here</a>, if there's any confusion. If
 * required, this entire package can be moved to an external
 * dependency, which I believe would be unambiguously permitted by
 * the classpath exception.
 */

@NotNullDefault
package com.github.tommyettinger.ds.support.function;

import com.github.tommyettinger.ds.annotations.NotNullDefault;

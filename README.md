# jdkgdxds
Making libGDX's data structures implement JDK interfaces

## What is this?

Some background, first... libGDX has its own data structures, and they're mostly nice to work with. They have fast iteration by
reusing iterators, they are designed to use low memory (both in the way the hashed maps and sets are designed and by allowing
primitive data types for many data structures), and they have some nice features that aren't present in all standard libraries,
like optional insertion-ordering. The problem with libGDX's data structures is that they are extremely limited in what interfaces
they implement, typically implementing no more than `java.io.Serializable` and `java.lang.Iterable`. They also are limited to Java
6 or 7 features, despite Java 8 features being available on Android and GWT for some time now, and even reaching iOS soon, if not
already. So what is this? It is a redo of libGDX's data structures so that they implement common JDK interfaces like
`java.util.Map`, `java.util.List`, and `java.util.Set`, plus their parts that can't implement generic interfaces use JDK 8's
`java.util.PrimitiveIterator` in some way. It also sharply increases the number of primitive-backed maps; they don't implement
`java.util.Map`, but often implement other interfaces here. As an example, `com.github.tommyettinger.ds.IntLongOrderedMap`
implements `com.github.tommyettinger.ds.Ordered.OfInt`, which specifies that the order of items (keys here) is represented by a
`com.github.tommyettinger.ds.IntList` containing those keys.

## OK, how do I use it?

You use jdkgdxds much like the standard JDK collections, just extended for primitive types. The Object-based classes are generic,
centered around `com.github.tommyettinger.ds.ObjectList`, `com.github.tommyettinger.ds.ObjectSet`, and
`com.github.tommyettinger.ds.ObjectObjectMap`; `ObjectOrderedSet` and `ObjectObjectOrderedMap` are also here and extend the other
Set and Map. These are effectively replacements for `com.badlogic.gdx.utils.Array`, `com.badlogic.gdx.utils.ObjectSet`,
`com.badlogic.gdx.utils.ObjectMap`, `com.badlogic.gdx.utils.OrderedSet`, and `com.badlogic.gdx.utils.OrderedMap`. As nice as it
would be to just call these by the same names (except Array, that one's just confusing), we have other kinds of Object-keyed Maps,
and other kinds of insertion-ordered Maps, so `ObjectMap` is now `ObjectObjectMap` because it has Object keys and Object values,
while `OrderedMap` is now `ObjectObjectOrderedMap`, because of the same reason. Primitive-backed collections support `int`
and `long` keys, and `int`, `long`, or `float` values; all primitive types are available for lists. So, there's `IntSet` and
`LongSet`, with ordered variants `IntOrderedSet` and `LongOrderedSet`, while their map counterparts are more numerous.
Most of the primitive lists are very similar, only changing the numeric type, but there are some small changes for `CharList`
(which doesn't define math operations on its items) and `BooleanList` (which defines logical operations but not math ones).

There's `IntFloatMap`, `IntFloatOrderedMap`, `IntIntMap`, `IntIntOrderedMap`, `IntLongMap`, `IntLongOrderedMap`,
`IntObjectMap`, `IntObjectOrderedMap`, `LongFloatMap`, `LongFloatOrderedMap`, `LongIntMap`, `LongIntOrderedMap`,
`LongLongMap`, `LongLongOrderedMap`, `LongObjectMap`, and `LongObjectOrderedMap`, so I hope that's enough. Then again, there's
still `ObjectFloatMap`, `ObjectFloatOrderedMap`, `ObjectIntMap`, `ObjectIntOrderedMap`, `ObjectLongMap`, and
`ObjectLongOrderedMap` for the primitive-valued maps with Object keys. There's a `CaseInsensitiveMap` (and a
`CaseInsensitiveOrderedMap`) that requires `CharSequence` keys (such as String or StringBuilder), but treats them as
case-insensitive, and allows a generic Object type for its values. There's `IdentityObjectMap` and `IdentityObjectOrderedMap`,
which compare keys by reference identity (not by `equals()`) and hash their keys using their identity hash code. There's the
unusual `HolderSet` and `HolderOrderedSet`, which take an "extractor" function when constructed and use it to hash items by an
extracted value; this lets you, for example, make a HolderSet of "Employee" objects and look up a full Employee given only their
UUID. In that case, an Employee's value could change, and the result of hashCode() on an Employee would change, but as long as the
UUID of the Employee stays the same, the same Employee will be found by methods like `get()` and `contains()`. `NumberedSet` wraps
an `ObjectIntOrderedMap` and makes it so that Object keys can be looked up by index (using the standard ordered set methods like
`getAt()`), but also so that their `indexOf()` method runs in constant time instead of linear time. This is at the expense of
slower removal from the middle of the NumberedSet; that class doesn't implement insertion in the middle of the NumberedSet either.
There's also a close relative of libGDX's `BinaryHeap` class, but the one here implements the JDK's `Queue`.

The library includes expanded interfaces for these to implement, like the aforementioned `Ordered` interface,
`PrimitiveCollection` to complement Java 8's `PrimitiveIterator`, some `float`-based versions of primitive specializations where
the JDK only offers `int`, `long`, and `double`, and primitive `Comparator`s (which are Java 8 `FunctionalInterface`s). Lastly,
because there are some randomized methods here and `java.util.SplittableRandom` isn't available everywhere, an alternative
high-quality and very-fast random number generator is here, `com.github.tommyettinger.ds.support.LaserRandom`, which extends
`java.util.Random` for maximum compatibility. It implements `com.github.tommyettinger.ds.support.EnhancedRandom`, an interface
that allows external code to match the API used by LaserRandom; EnhancedRandom is mostly default methods. There's also more
implementations of EnhancedRandom here. `com.github.tommyettinger.ds.support.TricycleRandom` can be significantly faster
but doesn't always produce very-random numbers right at the start of usage. `com.github.tommyettinger.ds.support.DistinctRandom`
is very similar to JDK 8's SplittableRandom, without the splitting, and will produce every possible `long` with its
`nextLong()` method before it ever repeats a returned value.

## How do I get it?

You have two options: Maven Central for stable-ish releases, or JitPack to select a commit of your choice to build.

Maven Central uses the dependency `api 'com.github.tommyettinger:jdkgdxds:0.1.3'` (you can use `implementation` instead
of `api` if you don't use the `java-library` plugin). It does not need any additional repository to be specified in most
cases; if it can't be found, you may need the repository `mavenCentral()` . If you have an HTML module, add
`implementation 'com.github.tommyettinger:jdkgdxds:0.1.3:sources'` to its dependencies, and in its
`GdxDefinition.gwt.xml` (in the HTML module), add
```xml
<inherits name="jdkgdxds" />
```
in with the other `inherits` lines.

You can build specific, typically brand-new commits on JitPack.
[JitPack has instructions for any recent commit you want here](https://jitpack.io/#tommyettinger/jdkgdxds/9e13d2150e).
To reiterate, you add `maven { url 'https://jitpack.io' }` to your project's `repositories` section, just **not** the one inside
`buildscript` (that just applies to the Gradle script itself, not your project). Then you can add
`implementation 'com.github.tommyettinger:jdkgdxds:9e13d2150e'` or `api 'com.github.tommyettinger:jdkgdxds:9e13d2150e'`, depending
on what your other dependencies use, to your project or its core module (if there are multiple modules, as in a typical libGDX
project). If you have an HTML module, add `implementation 'com.github.tommyettinger:jdkgdxds:9e13d2150e:sources'` to its
dependencies, and in its `GdxDefinition.gwt.xml` (in the HTML module), add
```xml
<inherits name="jdkgdxds" />
```
in with the other `inherits` lines. `9e13d2150e` is an example of a recent commit, and can be
replaced with other commits shown on JitPack.
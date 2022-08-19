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

You use jdkgdxds much like the standard JDK collections, just extended for primitive types. The types of data structure offered
here are lists (array-backed, like `ArrayList`), deques (double-ended queues, like `ArrayDeque` but also allowing access inside
the deque), sets (allowing only unique items, and coming in unordered and insertion-ordered varieties), maps (allowing unique keys
associated with values, and also coming in unordered and insertion-ordered varieties), and some extra types. The Object-based
classes are generic, centered around `com.github.tommyettinger.ds.ObjectList`, `com.github.tommyettinger.ds.ObjectDeque`,
`com.github.tommyettinger.ds.ObjectSet`, and `com.github.tommyettinger.ds.ObjectObjectMap`; `ObjectOrderedSet` and
`ObjectObjectOrderedMap` are also here and extend the other Set and Map. These are effectively replacements for
`com.badlogic.gdx.utils.Array`, `com.badlogic.gdx.utils.Queue`, `com.badlogic.gdx.utils.ObjectSet`,
`com.badlogic.gdx.utils.ObjectMap`, `com.badlogic.gdx.utils.OrderedSet`, and `com.badlogic.gdx.utils.OrderedMap`. As nice as it
would be to just call these by the same names (except Array, that one's just confusing), we have other kinds of Object-keyed Maps,
and other kinds of insertion-ordered Maps, so `ObjectMap` is now `ObjectObjectMap` because it has Object keys and Object values,
while `OrderedMap` is now `ObjectObjectOrderedMap`, because of the same reason. Primitive-backed collections support `int`
and `long` keys, and `int`, `long`, or `float` values; all primitive types are available for lists and deques. So, there's
`IntSet` and `LongSet`, with ordered variants `IntOrderedSet` and `LongOrderedSet`, while their map counterparts are more
numerous. Most of the primitive lists are very similar, only changing the numeric type, but there are some small changes for
`CharList` (which doesn't define math operations on its items) and `BooleanList` (which defines logical operations but not math
ones). The deques don't currently implement math operations on their items. As for the maps...

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
the JDK only offers `int`, `long`, and `double`, and primitive `Comparator`s (which are Java 8 `FunctionalInterface`s).

You can extend essentially all classes in jdkgdxds, and it's meant to be friendlier to inherit from than the libGDX collections.
The Object-keyed maps and sets have protected `place()` and `equate()` methods to allow changing the behavior of hashing (with
`place()`) and equality (with `equate()`). If you, for instance, wanted to use `char[]` as a key type in a map, the normal
behavior of an array in a hashed collection like a map or set is basically unusable. Arrays are compared by reference rather than
by value, so you would need the exact `char[]` you had put in to get a value out. They're also hashed by identity, which means
more than just the equality comparison needs to change. Thankfully, in jdkgdxds you can override `place()` to
`return Arrays.hashCode(item) & mask;` and `equate()` to `return Objects.deepEquals(left, right);`; this uses standard JDK methods
to hash and compare arrays, and will work as long as you don't edit any array keys while they are in the map. There are other
potential uses for this extensibility, like the case-insensitive CharSequence comparison that `CaseInsensitiveMap` and related
classes use, or some form of fuzzy equality for float or double keys.

Most of the ordered data structures now allow `addAll()` or `putAll()` to specify a range with a starting index and count of how
many items to copy from the data structure passed as a parameter (often some kind of `Ordered`). This also optionally takes a
starting index to add the range at in the order. When constructing one of these ordered data structures with a copy constructor,
you usually have the option to copy only a range of the data structure you are copying. Similarly, there's often a `removeRange()`
method, also present on all ordered types except deques (and it takes a start and end index, rather than a start index and count,
which imitates the method by that name in the JDK, not the similar one in libGDX's Array class). All of these are intended to be
useful for imitating disjoint sets, and other ways of isolating part of a data structure. You might shuffle an `ObjectList`, then
make two more distinct `ObjectList`s by copying different ranges from the shuffled "deck," for example.

An oddity in libGDX's Array classes (such as IntArray, FloatArray, and of course Array) is that their removeAll() method doesn't
act like removeAll() in the JDK List interface. In `List.removeAll(Collection)`, when the Collection `c` contains an item even
once, every occurrence of that item is removed from the `List`. In libGDX, if an item appears once in the parameter, it is removed
once from the Array; similarly, if it appears twice, it is removed twice. Here, we have the List behavior for removeAll(), but
also keep the Array behavior in `removeEach()`.

Here, we rely on some shared common functionality in two other libraries (by the same author).
[Digital](https://github.com/tommyettinger/digital) has core math code, including the BitConversion and Base classes that were
here earlier. [Juniper](https://github.com/tommyettinger/juniper) has the random number generators that also used to be here.
Having these as external libraries allows someone's LibraryA that really only needs the core math from digital to only use that,
but for projects that need both jdkgdxds and LibraryA, the shared dependency won't be duplicated.

Versions of jdkgdxds before 1.0.2 used "Fibonacci hashing" to mix `hashCode()` results. This involved multiplying the hash by a
specific constant (2 to the 64, divided by the golden ratio) and shifting the resulting bits so only an upper portion was used
(its size depended on the size of the backing table or tables). This works well in most situations, but a few were found where it
had catastrophically bad performance. The easiest case to reproduce was much like [this bug in Rust's standard library](https://accidentallyquadratic.tumblr.com/post/153545455987/rust-hash-iteration-reinsertion),
relating to reinserting already-partially-colliding keys in a large map or set. Starting in 1.0.2, we take a different route to
mixing `hashCode()` results -- instead of multiplying by a specific constant every time, we change the constant every time we need
to resize the backing tables. Everything else is the same. This simple change allows one test, inserting 2 million
specifically-chosen Strings, to complete in under 2 seconds, when without the change, it wouldn't complete given 77 minutes (a
speedup of over 3 orders of magnitude).

## How do I get it?

You have two options: Maven Central for stable releases, or JitPack to select a commit of your choice to build.

Maven Central uses the Gradle dependency `api 'com.github.tommyettinger:jdkgdxds:1.0.3'` (you can use `implementation` instead
of `api` if you don't use the `java-library` plugin). It does not need any additional repository to be specified in most
cases; if it can't be found, you may need the repository `mavenCentral()` . If you have an HTML module, add:
```
implementation "com.github.tommyettinger:funderby:0.0.1:sources"
implementation "com.github.tommyettinger:digital:0.1.0:sources"
implementation "com.github.tommyettinger:jdkgdxds:1.0.3:sources"
```
to its
dependencies, and in its `GdxDefinition.gwt.xml` (in the HTML module), add
```
<inherits name="funderby" />
<inherits name="digital" />
<inherits name="jdkgdxds" />
```
in with the other `inherits` lines. The dependency (and `inherits` line) on digital is not necessary for jdkgdxds
0.2.8, but is necessary starting in 1.0.3 and later. The dependency and `inherits` line for funderby is new in 1.0.4 .
Versions 1.0.1 and 1.0.2 also depended on [juniper](https://github.com/tommyettinger/juniper) 0.0.2 ; if you intend to use the
randomized algorithms here (like shuffles), then depending on Juniper might be a good idea, though it is still optional. The
version is expected to increase somewhat for digital as bugs are found and fixed, but a low version number isn't a bad thing for
that library -- both digital and juniper were both mostly drawn from code in this library, and were tested significantly here.
The version for funderby is expected to stay at or around 0.0.1, since it is a relatively small library and is probably complete.

You can build specific, typically brand-new commits on JitPack.
[JitPack has instructions for any recent commit you want here](https://jitpack.io/#tommyettinger/jdkgdxds/d0c385905f).
To reiterate, you add `maven { url 'https://jitpack.io' }` to your project's `repositories` section, just **not** the one inside
`buildscript` (that just applies to the Gradle script itself, not your project). Then you can add
`implementation 'com.github.tommyettinger:jdkgdxds:d0c385905f'` or `api 'com.github.tommyettinger:jdkgdxds:d0c385905f'`, depending
on what your other dependencies use, to your project or its core module (if there are multiple modules, as in a typical libGDX
project). If you have an HTML module, add:
```
implementation "com.github.tommyettinger:funderby:0.0.1:sources"
implementation "com.github.tommyettinger:digital:0.1.0:sources"
implementation "com.github.tommyettinger:jdkgdxds:d0c385905f:sources"
```
to its
dependencies, and in its `GdxDefinition.gwt.xml` (in the HTML module), add
```
<inherits name="funderby" />
<inherits name="digital" />
<inherits name="jdkgdxds" />
```
in with the other `inherits` lines. `d0c385905f` is an example of a recent commit, and can be
replaced with other commits shown on JitPack.

There is an optional dependency, [jdkgdxds-interop](https://github.com/tommyettinger/jdkgdxds_interop), that provides code to
transfer libGDX data structures to and from jdkgdxds data structures, and more importantly, to store any`*` jdkgdxds classes using
libGDX's `Json` class. `*`Any, only because `IdentityMap` and `IdentityOrderedMap` don't make sense to serialize, while
`HolderSet` and `HolderOrderedSet` can't be serialized easily because their behavior depends on a `Function`. For historical
reasons, jdkgdxds-interop also can serialize classes from digital and juniper.

## Updating to 1.0.1

The 1.0.1 release is a more significant set of breaking changes, but thankfully, most of the changes have been very easy to adjust
to in practice. First, the core math utilities in `BitConversion` and `Base` were moved into the
[digital](https://github.com/tommyettinger/digital) library. Then, the random number generators that were here were moved to the
[juniper](https://github.com/tommyettinger/juniper) library. Because of changes in juniper, jdkgdxds can now just use its
generators as `java.util.Random` subclasses, so juniper is simply an optional, but recommended, dependency starting in jdkgdxds
1.0.3 . There are various new additions to both of these small libraries to make them more useful as shared libraries for other
libraries to depend on. While `digital` has common math and trigonometry methods now, the random number generators in `juniper`
can serialize themselves to Strings without needing external code, and deserialize any of the serialized forms back to the
appropriate generator using `Deserializer`.

To update to 1.0.1, most of the changes are package-related, and often only need changing import statements. Code that previously
imported:

  - `com.github.tommyettinger.ds.support.BitConversion` changes to `com.github.tommyettinger.digital.BitConversion`
  - `com.github.tommyettinger.ds.support.Base` changes to `com.github.tommyettinger.digital.Base`
  - `com.github.tommyettinger.ds.support.ChopRandom` changes to `com.github.tommyettinger.random.ChopRandom`
  - `com.github.tommyettinger.ds.support.DistinctRandom` changes to `com.github.tommyettinger.random.DistinctRandom`
  - `com.github.tommyettinger.ds.support.FourWheelRandom` changes to `com.github.tommyettinger.random.FourWheelRandom`
  - `com.github.tommyettinger.ds.support.LaserRandom` changes to `com.github.tommyettinger.random.LaserRandom`
  - `com.github.tommyettinger.ds.support.MizuchiRandom` changes to `com.github.tommyettinger.random.MizuchiRandom`
  - `com.github.tommyettinger.ds.support.RomuTrioRandom` changes to `com.github.tommyettinger.random.RomuTrioRandom`
  - `com.github.tommyettinger.ds.support.StrangerRandom` changes to `com.github.tommyettinger.random.StrangerRandom`
  - `com.github.tommyettinger.ds.support.TricycleRandom` changes to `com.github.tommyettinger.random.TricycleRandom`
  - `com.github.tommyettinger.ds.support.TrimRandom` changes to `com.github.tommyettinger.random.TrimRandom`
  - `com.github.tommyettinger.ds.support.Xoshiro256StarStarRandom` changes to `com.github.tommyettinger.random.Xoshiro256StarStarRandom`
  - `com.github.tommyettinger.ds.support.EnhancedRandom` is slightly more complicated, but it changes to `com.github.tommyettinger.random.EnhancedRandom`

`EnhancedRandom` is now an abstract class, instead of a default-method-heavy interface, which makes it a little less flexible, but
allows it to work smoothly on Java 17 and much earlier Java versions. Extending the new `EnhancedRandom` only needs the new
`getTag()` method implemented, and maybe changes to `copy()`, `equals()` or `toString()` could be used as well.

If you are migrating other code to `digital`'s new math functions, you may need to rename some called methods -- the `sin_()`,
`cos_()`, and similar trigonometric methods that worked with turns instead of radians now explicitly are called `sinTurns()` and
`cosTurns()`.

There was a 1.0.0 release, but it mistakenly shadowed the `digital` code, without super-sourcing `BitConversion` for GWT support.
So, the first 1.x release is 1.0.1.

## Updating to 1.0.4

Likely less significant than the 1.0.1 update, 1.0.4 still "removed" some classes, though they were only moved to the funderby
library. The `com.github.tommyettinger.ds.support.function` package is now `com.github.tommyettinger.function`, and has many more
functional interfaces, but if they were being provided as lambdas, no difference will be noticeable. There are quite a lot more
interfaces in funderby than there ever were in jdkgdxds, which may help in uncommon situations that use primitives in lambdas
(so as if you need a `ByteLongPredicate`, you'll be ready). Some classes may have had their names changed; you can consult
[funderby's README.md](https://github.com/tommyettinger/funderby#what-is-it) for the naming conventions. 

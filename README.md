# IMCE MagicDraw Library for AspectJ-based MD API enhancements

This library provides the following enhancements to MagicDraw's APIs:

- [Enhanced MD Browser context action](src/main/scala/gov/nasa/jpl/magicdraw/enhanced/ui/browser/EnhancedBrowserContextAMConfigurator.scala)

- [Enhanced MD Draw Path action](src/main/scala/gov/nasa/jpl/magicdraw/enhanced/actions/paths/EnhancedDrawPathAction.scala)

- [Load Module Migration Interceptor](src/main/scala/gov/nasa/jpl/magicdraw/enhanced/migration/LocalModuleMigrationInterceptor.scala)

## About using AspectJ in MagicDraw

To enable these enhancements at runtime in MD, this library uses the so-called
[load-time weaving](https://eclipse.org/aspectj/doc/released/devguide/ltw.html)
strategy. AspectJ is an aspect-oriented extension to Java. Because AspectJ operates
 at the level of bytecode, it is also applicable to other languages like Scala that compile to the JVM.

Whereas interface and class are the basic units of modularity in object-oriented programming,
AspectJ adds the concept of `join point` -- a point in the flow of program execution -- and a few
related constructs as the basic unit of modularity in aspect-oriented programming:
- a `pointcut` selects one or more `join points` to access runtime values at those points,
- an `advice` is a piece of code that will be executed when the `joint points` of a `pointcut`
  will be reached and that will have access to the `pointcut`'s runtime values.

Executing AspectJ code requires weaving all the `join points`, `pointcuts` and `advice` into the
existing code where each `joint point` can possibly be triggered at runtime,
where each `pointcut` may access runtime values at such `join points` and where `advice`
code may need to be executed. Since this weaving process operates at the level of JVM bytecode,
the available strategies fall into two categories depending on when the JVM bytecode is modified:

- Early: `compile-time weaving` and `post-compile weaving` occur during development, resulting in jar files
  with all the applicable `join points`, `pointcuts` and `advice` already woven.

  Pros: No runtime weaving overhead

  Cons: Requires using the modified jar files with woven aspects instead of the original unmodified jar files

- Late: `load-time weaving` examines each jar file loaded and weaves all applicable `join points`,
  `pointcuts` and `advice` before execution

  Pros: No modification of existing jar files

  Cons: Runtime weaving overhead (only when a library is loaded)

This package uses the Late `load-time weaving` strategy and uses filters
to minimize the runtime overhead of the weaving process when loading libraries.

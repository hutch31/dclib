# dclib
Decoupled library, a group of useful components implementing the DecoupledIO interface from Chisel3

This library is a collection of components I have found useful in building Chisel-based designs.  Many
of the components in this library were ported over from similar components implemented in Verilog in
the sdlib repository.

## Naming Conventions

Blocks which have FIFO-like behavior (DCInput, DCOutput, DCHold, etc.) have their incoming port
named "enq" and their outgoing port named "deq", to match the naming convention for Queue.  These
parts are often swapped for Queue as designs evolve so matching their naming makes this simpler.

Other blocks have their incoming ports named "c" (consumer) and outgoing ports named "p" (producer).

## Using dclib

There are two main ways to incorporate dclib into your project.

 - Publish as a local artifact
 - Copy files into existing project

The local artifact method is generally cleaner, and keeps the unit tests
built into dclib intact.

### Publish as local artifact

To publish locally, run the command **sbt publishLocal**.  This willl compile the source
files and put the artifact in your Ivy repository in your home directory.  Note
that publishing locally is specific to your account and machine, so builds from other
users will need to do their own publishLocal.

You can then add the artifact to your project build by adding:

`libraryDependencies ++= Seq("org.ghutchis" %% "dclib" % "2025-01-06")
`

### Copy files into existing project

Add the files to your project by checking out as a submodule or subtree.

Then add the new path to dclib to your build.sbt:

`unmanagedSourceDirectories in Compile += baseDirectory.value / "dclib" / "src"
`

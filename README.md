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

The easiest way to incorporate dclib into your project, particularly if you do not intend to push
changes back, is to add it to your project as a subtree.

`git subtree add --prefix dclib https://github.com/hutch31/dclib.git master --squash
`

This will add dclib as a directory under your main directory, the --squash omits the subtree history
from your main repository.

Another straightforward way to incorporate dclib into your project if you are using git is to check it
out as a submodule:

`git submodule add https://github.com/hutch31/dclib.git
git submodule init
`

Then add the new path to dclib to your build.sbt:

`unmanagedSourceDirectories in Compile += baseDirectory.value / "dclib" / "src"
`

Remember that if pull dclib to get new source versions, you will also need to commit the
pointer to the current rev of dclib in your base repository.

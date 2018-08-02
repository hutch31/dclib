# dclib
Decoupled library, a group of useful components implementing the DecoupledIO interface from Chisel3

This library is a collection of components I have found useful in building Chisel-based designs.  Many
of the components in this library were ported over from similar components implemented in Verilog in
the sdlib repository.

# Naming Conventions

Blocks which have FIFO-like behavior (DCInput, DCOutput, DCHold, etc.) have their incoming port
named "enq" and their outgoing port named "deq", to match the naming convention for Queue.  These
parts are often swapped for Queue as designs evolve so matching their naming makes this simpler.

Other blocks have their incoming ports named "c" (consumer) and outgoing ports named "p" (producer).

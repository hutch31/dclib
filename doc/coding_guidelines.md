Chisel Coding Guidelines
========================

## Modules

### Output Module Names

When Chisel generates output code, modules generated from the same class which have
different Verilog implementations will be uniquified by the Chisel backend generator.
This results in unneeded code churn as new module names are created or renamed due to
minor changes in the Chisel source code.

To fix this, Chisel Modules should explicitly pick their Verilog module names using
desiredName.  The example below shows using the two parameters to DCMirror to create
a unique name.

```scala
class DCMirror[D <: Data](data: D, num: Int) extends Module {
  override def desiredName: String = f"DCMirror_${data.toString}_N${num.toString}}"
}
```

### Instance Module Names

The code renamer creates the same problem of renaming for Chisel standard modules 
such as Queue or RRArbiter.  In this case, we can use suggestName to provide unique
module names for these ouptut files.

```scala
 val txq = Module(new Queue(...).suggestName("ReaderTransmitQueue"))
 val metaQueue = Module(new Queue(...).suggestName("ReaderMetaQueue"))
```

### Inheritance

One of Chisel's major advantages is a full class system with inheritance.  This allows
us to build code similar to VHDL's interface/implementation paradigm.

Modules which have identical interfaces should be create an abstract base class which 
defines only IO, and then different implementations should inherit from the base class.

## Traits

Generally use of traits for IO should be avoided, as traits do not allow for parameterization.
This recommendation may change if Chisel moves to Scala 3, which supports paramterized traits.

When traits are used, they should either be used for common functionality which doesn't affect
IO, such as a common CRC library, or for functionality which always has the same IO.  An example
of this would be a DFT trait:

```scala
trait HasDftInterface {
  val dft = IO(Input(new dft_pins))
}
```

Outside of this, IO should be grouped using Bundles which allow parameters, and can be re-used
by both source and sink modules by using Flipped().

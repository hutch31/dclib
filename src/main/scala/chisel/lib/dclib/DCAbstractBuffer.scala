package chisel.lib.dclib

import chisel3._
import chisel3.util._

class DCBufferIO[D <: Data](data: D) extends Bundle {
  val enq = Flipped(new DecoupledIO(data.cloneType))
  val deq = new DecoupledIO(data.cloneType)
}

/** Base class for decoupled types supporting a buffer interface
 *
 * This interface applies to blocks which behave similar to a buffer of
 * unknown depth.  These blocks have paired input and output Decoupled
 * interfaces and an internal buffer of unspecified, but fixed depth.
 *
 * Using this base class allows all blocks confirming to this interface
 * to share a common test class.
 */
abstract class DCAbstractBuffer[D <: Data](data: D) extends Module {
  val io = IO(new DCBufferIO(data))
}

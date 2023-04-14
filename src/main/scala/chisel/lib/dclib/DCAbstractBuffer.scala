package chisel.lib.dclib

import chisel3._
import chisel3.util._

class DCBufferIO[D <: Data](data: D) extends Bundle {
  val enq = Flipped(new DecoupledIO(data.cloneType))
  val deq = new DecoupledIO(data.cloneType)
}

abstract class DCAbstractBuffer[D <: Data](data: D) extends Module {
  val io = IO(new DCBufferIO(data))
}

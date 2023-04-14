package chisel.lib.dclib

import chisel3._
import chisel3.util.ImplicitConversions.intToUInt
import chisel3.util._

/**
 * Closes output timing on an input of type D
 * deq.valid and deq.bits will be registered, enq.ready will be combinatorial
 *
 * @param data Data type to be wrapped
 * @param dataReset When true, resets the datapath bits.  When false, reset is only for valid/ready.
 */
class DCOutput[D <: Data](data: D, dataReset : Boolean = false) extends DCAbstractBuffer(data) {
  override def desiredName: String = "DCOutput_" + data.toString

  val r_valid = RegInit(false.B)

  io.enq.ready := io.deq.ready || !r_valid
  r_valid := io.enq.fire || (r_valid && !io.deq.ready)
  if (dataReset)
    io.deq.bits := RegEnable(next=io.enq.bits, enable=io.enq.fire, init=0.asTypeOf(data))
  else
    io.deq.bits := RegEnable(next=io.enq.bits, enable=io.enq.fire)
  io.deq.valid := r_valid
}

// Helper function for functional inference
object DCOutput {
  def apply[D <: Data](x : DecoupledIO[D]) : DecoupledIO[D] = {
    val tout = Module(new DCOutput(x.bits.cloneType))
    tout.io.enq <> x
    tout.io.deq
  }
}

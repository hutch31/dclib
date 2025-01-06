//----------------------------------------------------------------------
// This file has no Copyright, as it is released in the public domain
// Author: Guy Hutchison (guy@ghutchis.org)
// see http://unlicense.org/
//----------------------------------------------------------------------


package chisel.lib.dclib

import chisel3._
import chisel3.util.ImplicitConversions.intToUInt
import chisel3.util._
import chisel3.util.experimental.InlineInstance

/**
 * Closes output timing on an input of type D
 * deq.valid and deq.bits will be registered, enq.ready will be combinatorial
 */
class DCOutput[D <: Data](data: D, dataReset : Boolean = false) extends Module with InlineInstance {
  val io = IO(new Bundle {
    val enq = Flipped(new DecoupledIO(data.cloneType))
    val deq = new DecoupledIO(data.cloneType)
  })
  override def desiredName: String = "DCOutput_" + data.toString

  val r_valid = RegInit(false.B)

  io.enq.ready := io.deq.ready || !r_valid
  r_valid := io.enq.fire || (r_valid && !io.deq.ready)
  if (dataReset)
    io.deq.bits := RegEnable(io.enq.bits, 0.asTypeOf(data), io.enq.fire)
  else
    io.deq.bits := RegEnable(io.enq.bits, io.enq.fire)
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

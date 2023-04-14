package chisel.lib.dclib

import chisel3._
import chisel3.util.DecoupledIO
import chisel3.util.ImplicitConversions.intToUInt

/**
  * Closes timing on the ready signal of a decoupled interface.  Called an
  * "input" module because if used on input interfaces provides registered-output
  * ready.  deq.valid and deq.bits have combination paths.
  *
  * Internally implements a single hold register to hold data in the event that
  * deq interface is not ready
  */
class DCInput[D <: Data](data: D, dataReset : Boolean = false) extends DCAbstractBuffer(data) {
  override def desiredName: String = "DCInput_" + data.toString

  // val r_valid = RegInit(false.B)
  val ready_r = RegInit(true.B)
  val occupied = RegInit(false.B)
  val hold = if (dataReset) RegInit(init=0.asTypeOf(data.cloneType)) else Reg(data.cloneType)
  val load = Wire(Bool())
  val drain = Wire(Bool())

  drain := occupied && io.deq.ready
  load := io.enq.valid && ready_r && (!io.deq.ready || drain)

  when (occupied) {
    io.deq.bits := hold
  }.otherwise {
    io.deq.bits := io.enq.bits
  }

  io.deq.valid := io.enq.valid || occupied
  when (load) {
    occupied := true.B
    hold := io.enq.bits
  }.elsewhen (drain) {
    occupied := false.B
  }

  ready_r := (!occupied && !load) || (drain && !load)
  io.enq.ready := ready_r
}

// Helper function for functional inference
object DCInput {
  def apply[D <: Data](x: DecoupledIO[D]): DecoupledIO[D] = {
    val tout = Module(new DCInput(x.bits.cloneType))
    tout.io.enq <> x
    tout.io.deq
  }
}


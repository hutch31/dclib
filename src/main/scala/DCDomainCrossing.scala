package dclib

import chisel3._
import chisel3.core.withClockAndReset
import chisel3.util._

/**
  * Clock domain crossing block for low-speed signals.  Uses a pair of hold registers
  * in each clock domain and synchronizes the control signals between them.  Worst
  * case throughput (equal clock speeds) should be 1 transaction per 6 cycles.
  *
  * Between holding registers uses two signals and looks for a phase change between
  * them.
  *
  * @param data
  * @tparam D
  */
class DCDomainCrossing[D <: Data](data: D, sync_size : Int=2) extends Module {
  val io = IO(new Bundle {
    val enq = Flipped(Decoupled(data.cloneType))
    val deq = Decoupled(data.cloneType)

    val deq_clock = Input(Clock())
    val deq_reset = Input(Bool())
  })

  val enq_phase = RegInit(init=false.B)
  val deq_phase = withClockAndReset(io.deq_clock, io.deq_reset) { RegInit(init=false.B) }
  val enq_hold = Reg(data.cloneType)
  val enq_phase_sync = withClockAndReset(io.deq_clock, io.deq_reset) { ShiftRegister(enq_phase, sync_size) }
  val deq_phase_sync = ShiftRegister(deq_phase, sync_size)
  val deq_hold = withClockAndReset(io.deq_clock, io.deq_reset) { Module(new DCHold(data.cloneType)) }

  io.enq.ready := enq_phase === deq_phase_sync
  deq_hold.io.enq.valid := enq_phase_sync =/= deq_phase

  when (deq_hold.io.enq.fire()) {
    deq_phase := ~deq_phase
  }

  when (io.enq.fire()) {
    enq_phase := ~enq_phase
    enq_hold := io.enq.bits
  }
  deq_hold.io.enq.bits := enq_hold
  io.deq <> deq_hold.io.deq
}

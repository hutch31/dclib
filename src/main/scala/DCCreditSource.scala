package dclib

import chisel3._
import chisel3.util.DecoupledIO

/**
  * Converts decoupled interface to credit-based interface, either for top-
  * level data transfers or for interfaces which need to be rate limited.
  *
  * @param data    Link data type
  * @param credit  Amount of link credit the source should start with
  */
class DCCreditSource[D <: Data](data: D, credit: Int) extends Module {
  val io = IO(new Bundle {
    val enq = Flipped(new DecoupledIO(data.cloneType))
    val deq = new DecoupledIO(data.cloneType)
  })

  val link_credit = RegInit(init=credit.U)
  val has_credit = link_credit =/= 0.U

  io.enq.ready := has_credit
  io.deq.valid := io.enq.valid && has_credit
  when (io.deq.valid && has_credit && !io.deq.ready) {
    link_credit := link_credit - 1.U
  }.elsewhen (!io.deq.valid && io.deq.ready) {
    link_credit := link_credit + 1.U
  }
  io.deq.bits := io.enq.bits
}

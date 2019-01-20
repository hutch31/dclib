package dclib

import chisel3._
import chisel3.util.{DecoupledIO, Queue}

/**
  * Sink module corresponding to DCCreditSource, converts back from a credit
  * based interface into decoupled interface.  Has an integrated Queue to
  * handle incoming transactions, issues credit when tokens are removed
  * from the queue.
  *
  * @param data    Interface data type
  * @param credit  Maximum credit allowed on interface
  */
class DCCreditSink[D <: Data](data: D, credit: Int) extends Module {
  val io = IO(new Bundle {
    val enq = Flipped(new DecoupledIO(data.cloneType))
    val deq = new DecoupledIO(data.cloneType)
  })

  val q = Module(new Queue(data.cloneType, credit))

  q.io.enq.valid := io.enq.valid
  q.io.enq.bits := io.enq.bits
  io.enq.ready := q.io.deq.fire()
  q.io.deq <> io.deq
}

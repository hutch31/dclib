//----------------------------------------------------------------------
// This file has no Copyright, as it is released in the public domain
// Author: Guy Hutchison (guy@ghutchis.org)
// see http://unlicense.org/
//----------------------------------------------------------------------


package chisel.lib.dclib

import chisel3._
import chisel3.util._
import chisel3.util.ImplicitConversions.intToUInt

/**
 * Creates a new signal class for sending credit-based flow control.  The credit-based
 * flow control is intended for two purposes; first, credit based flow control can
 * be used to create a registered-input/output interface at the top level.  Second,
 * credits can be a more effective way to carry traffic which is bursty but low-rate
 * or limited by some underlying resource.
 */
class CreditIO[D <: Data](data: D) extends Bundle {
  val valid = Output(Bool())
  val credit = Input(Bool())
  val bits = Output(data)
}

/**
 * Creates a credit sender which converts between DecoupledIO interfaces and
 * a [[CreditIO]] interface.  This basic credit interface requires that the amount
 * of maximum receiver credit be known at design time, and DCCreditSender
 * initializes to this credit amount.
 *
 * This block requires that both sender and receiver be initialized on the same
 * reset, or that the receiver is released from reset before the sender.
 *
 * @param data  Data type for the interface
 * @param maxCredit Amount of credit for the interface
 */
class DCCreditSender[D <: Data](data: D, maxCredit: Int) extends Module {
  val io = IO(new Bundle {
    val enq = Flipped(Decoupled(data.cloneType))
    val deq = new CreditIO(data.cloneType)
    val curCredit = Output(UInt(log2Ceil(maxCredit+1).W))
  })
  require(maxCredit >= 1)
  override def desiredName: String = "DCCreditSender_" + data.toString

  val icredit = RegNext(io.deq.credit, init=0.B)
  val curCredit = RegInit(init=maxCredit.U)
  when (icredit && !io.enq.fire) {
    curCredit := curCredit + 1.U
  }.elsewhen(!icredit && io.enq.fire) {
    curCredit := curCredit - 1.U
  }
  io.enq.ready := curCredit > 0.U
  val dataOut = RegEnable(io.enq.bits, 0.asTypeOf(data), io.enq.fire)
  val validOut = RegNext(next=io.enq.fire, init=false.B)
  io.deq.valid := validOut
  io.deq.bits := dataOut
  io.curCredit := curCredit
}

/**
 * Creates a credit receiver as the counterpoint to [[DCCreditSender]].
 *
 * @param data Data type for the interface
 * @param maxCredit Amount of credit for the interface
 */
class DCCreditReceiver[D <: Data](data: D, val maxCredit: Int) extends Module {
  val io = IO(new Bundle {
    val enq = Flipped(new CreditIO(data.cloneType))
    val deq = new DecoupledIO(data.cloneType)
    val fifoCount = Output(UInt(log2Ceil(maxCredit+1).W))
  })
  require(maxCredit >= 1)
  override def desiredName: String = "DCCreditReceiver_" + data.toString

  val ivalid = RegNext(io.enq.valid, init=0.B)
  val idata = RegNext(io.enq.bits, 0.asTypeOf(data))
  val outFifo = Module(new Queue(data.cloneType, maxCredit))
  val nextCredit = WireDefault(0.B)

  outFifo.io.enq.bits := idata

  // bypass the FIFO when empty
  when (!outFifo.io.deq.valid && (outFifo.io.count === 0.U)) {
    when (io.deq.ready) {
      outFifo.io.enq.valid := false.B
      nextCredit := ivalid
    }.otherwise {
      outFifo.io.enq.valid := ivalid
    }
    outFifo.io.deq.ready := false.B
    io.deq.valid := ivalid
    io.deq.bits := idata
  }.otherwise {
    outFifo.io.enq.valid := ivalid
    outFifo.io.enq.bits := idata
    io.deq <> outFifo.io.deq
    nextCredit := outFifo.io.deq.fire
  }
  io.fifoCount := outFifo.io.count
  val ocredit = RegNext(next=nextCredit, init=false.B)
  io.enq.credit := ocredit
}

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
 * Sends tokens to multiple output destinations, as selected by bit
 * vector "dst".  dst must have at least one bit set for correct
 * operation.
 *
 * DCMirror supports the ability to do partial completion, which means that if some
 * outputs assert ready but others do not, then the ready ports will deassert valid
 * and the input will be blocked until all ports have asserted ready.  Output ports
 * can assert ready in any order.
 *
 * @param data Payload data type
 * @param n    Number of output destinations
 */
class DCMirror[D <: Data](data: D, n: Int, dataReset : Boolean = false) extends Module {
  val io = IO(new Bundle {
    val dst = Input(UInt(n.W))
    val c = Flipped(new DecoupledIO(data.cloneType))
    val p = Vec(n, new DecoupledIO(data.cloneType))
  })
  override def desiredName: String = "DCMirror_" + data.toString + "_N" + n.toString

  val p_data = if (dataReset) RegInit(0.asTypeOf(data)) else Reg(data.cloneType)
  val p_valid = RegInit(0.asUInt(n.W))
  val p_ready = Cat(io.p.map(_.ready).reverse)
  val nxt_accept = (p_valid === 0.U) || ((p_valid =/= 0.U) && ((p_valid & p_ready) === p_valid))

  when (nxt_accept) {
    p_valid := Fill(n, io.c.valid) & io.dst
    p_data := io.c.bits
  }.otherwise {
    p_valid := p_valid & ~p_ready
  }
  io.c.ready := nxt_accept

  for (i <- 0 until n) {
    io.p(i).bits := p_data
    io.p(i).valid := p_valid(i)
  }
}

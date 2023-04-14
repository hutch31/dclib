package chisel.lib.dclib.experimental

import chisel.lib.dclib.{DCOutput, defaultDoubleSync}
import chisel3._
import chisel3.util._

/**
 * Asynchronous FIFO with Decoupled interfaces
 *
 * Implements a clock-domain crossing FIFO with synchronized pointers.  The FIFO must be a natural power of 2
 * in depth, and should be at least 8 words deep for full performance.  Internally uses a 1-bit larger pointer
 * value to track empty vs. full.
 *
 * The Async FIFO has an optional outputHold register, which prevents propagation of potentially metastable data
 * when the FIFO is empty.  This parameter should be set when there is any downstream combinatorial logic which
 * might examine the FIFO data bits.  This can be set to false if downstream logic is all sequential and qualified
 * by the output valid signal.
 *
 * @param data       Data type for FIFO
 * @param depth      Depth of FIFO, must be power of 2
 * @param doubleSync Generator function to create a double-synchronized version of input
 * @param outputHold Generate an additional flop stage on dequeue to prevent invalid propagation
 */
class DCAsyncFifo[D <: Data](data: D, depth: Int, doubleSync: (UInt) => UInt = defaultDoubleSync, outputHold : Boolean = false) extends RawModule {
  val io = IO(new Bundle {
    val enqClock = Input(Clock())
    val enqReset = Input(Reset())
    val deqClock = Input(Clock())
    val deqReset = Input(Reset())
    val enq = Flipped(new DecoupledIO(data.cloneType))
    val deq = new DecoupledIO(data.cloneType)
  })
  override def desiredName: String = s"DCAsyncFifo_${data.toString}_D$depth"
  // Async FIFO must be power of two for pointer sync to work correctly
  val asz = log2Ceil(depth)
  val ptrsz = asz+1
  require(depth == 1 << asz)

  val mem = withClockAndReset(io.enqClock, io.enqReset) {
    Mem(depth, data.cloneType)
  }
  val wrptr_grey_enq = withClockAndReset(io.enqClock, io.enqReset) { RegInit(init=0.U(ptrsz.W)) }
  val wrptr_enq = GrayToBinary(wrptr_grey_enq)
  val wrptr_grey_deq = Wire(UInt(ptrsz.W))

  wrptr_grey_deq := withClockAndReset(io.deqClock, io.deqReset) {
    doubleSync(wrptr_grey_enq)
  }

  val rdptr_grey_deq = withClockAndReset(io.deqClock, io.deqReset) { RegInit(init = 0.U(ptrsz.W)) }
  val rdptr_deq = GrayToBinary(rdptr_grey_deq)
  val rdptr_grey_enq = Wire(UInt(ptrsz.W))
  val rdptr_enq = Wire(UInt(ptrsz.W))
  val wrptr_deq = Wire(UInt(ptrsz.W))

  rdptr_grey_deq := BinaryToGray(rdptr_deq)
  rdptr_grey_enq := withClockAndReset(io.enqClock, io.enqReset) {
    doubleSync(rdptr_grey_deq)
  }
  rdptr_enq := GrayToBinary(rdptr_grey_enq)

  val full_enq = wrptr_enq(asz - 1, 0) === rdptr_enq(asz - 1, 0) & (wrptr_enq(asz) =/= rdptr_enq(asz))

  wrptr_deq := GrayToBinary(wrptr_grey_deq)
  val empty_deq = wrptr_deq === rdptr_deq
  io.enq.ready := !full_enq

  when(io.enq.fire) {
    wrptr_grey_enq := BinaryToGray(wrptr_enq + 1.U)
    withClockAndReset(io.enqClock, io.enqReset) {
      mem.write(wrptr_enq(asz - 1, 0), io.enq.bits)
    }
  }

  when(io.deq.fire) {
    rdptr_grey_deq := BinaryToGray(rdptr_deq + 1.U)
  }
  if (outputHold) {
    val outHold = withClockAndReset(io.deqClock, io.deqReset) { Module(new DCOutput(data.cloneType))}
    outHold.io.enq.valid := !empty_deq
    outHold.io.enq.bits := withClock(io.enqClock) {
      mem.read(rdptr_deq(asz - 1, 0))
    }
    io.deq <> outHold.io.deq
  } else {
    io.deq.valid := !empty_deq
    io.deq.bits := withClock(io.enqClock) {
      mem.read(rdptr_deq(asz - 1, 0))
    }
  }
}

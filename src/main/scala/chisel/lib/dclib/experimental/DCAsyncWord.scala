package chisel.lib.dclib.experimental

import chisel.lib.dclib.defaultDoubleSync
import chisel3._
import chisel3.util._

/**
 * Area-optimized single-word synchronized crossing with ready/valid handshake.
 * For full metastability protection, the input to this block should should be
 * registered.
 *
 * @param doubleSync Function which returns a double-synchronized version of the input data
 */
class DCAsyncWord[D <: Data](data: D, doubleSync: (UInt) => UInt = defaultDoubleSync) extends Module {
  val io = IO(new Bundle {
    val enqClock = Input(Clock())
    val enqReset = Input(Reset())
    val enq = Flipped(new DecoupledIO(data.cloneType))
    val deq = new DecoupledIO(data.cloneType)
  })
  val enqPhase_E = Wire(Bool())
  val enqPhase_D = doubleSync(enqPhase_E).asBool
  val deqPhase_D = RegInit(init=0.B)
  val deqPhase_E = doubleSync(deqPhase_D).asBool
  val loadData = WireDefault(0.B)
  val dataOut = RegEnable(next=io.enq.bits, enable=loadData)
  val sDeqIdle :: sDeqValid :: sDeqClear :: Nil = Enum(3)
  val deqState = RegInit(init=sDeqIdle)

  io.enq.ready := 0.B

  withClockAndReset(io.enqClock, io.enqReset) {
    val sEnqIdle :: sEnqWait :: Nil = Enum(2)
    val enqState = RegInit(init=sEnqIdle)

    switch (enqState) {
      is (sEnqIdle) {

        when (io.enq.valid & !deqPhase_E) {
          enqState := sEnqWait
        }
      }

      is (sEnqWait) {
        when (deqPhase_E) {
          io.enq.ready := 1.B
          enqState := sEnqIdle
        }
      }
    }
    enqPhase_E := enqState === sEnqWait
  }

  io.deq.valid := deqState === sDeqValid
  io.deq.bits := dataOut

  switch (deqState) {
    is (sDeqIdle) {
      when (enqPhase_D) {
        loadData := 1.B
        deqState := sDeqValid
        deqPhase_D := 1.B
      }
    }

    is (sDeqValid) {
      when (io.deq.ready) {
        deqState := sDeqClear
      }
    }

    is (sDeqClear) {
      when (!enqPhase_D) {
        deqState := sDeqIdle
        deqPhase_D := 0.B
      }
    }
  }
}

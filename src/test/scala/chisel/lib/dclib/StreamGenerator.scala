package chisel.lib.dclib

import chisel3._
import chisel3.util._

class StreamGenerator(streams : Int, dwidth : Int) extends Module {
  val streamSize = log2Ceil(streams)
  val seqSize = dwidth - streamSize
  val streamId = IO(Input(UInt(streamSize.W)))
  val out = IO(Decoupled(UInt(dwidth.W)))
  val tcount = IO(Flipped(Decoupled(UInt(32.W))))
  val s_idle :: s_send :: Nil = Enum(2)

  require(dwidth > (streamSize+2))

  val sequence = RegInit(init=0.U(seqSize.W))
  val state = RegInit(init=s_idle)
  val count = RegInit(init=0.U(32.W))

  tcount.ready := 0.B
  out.valid := 0.B
  out.bits := Cat(streamId, sequence)

  switch (state) {
    is (s_idle) {
      tcount.ready := 1.B
      when (tcount.valid) {
        state := s_send
        count := tcount.bits
      }
    }

    is (s_send) {
      out.valid := 1.B
      when (out.ready) {
        count := count - 1.U
        sequence := sequence + 1.U
        when (count === 1.U) {
          state := s_idle
        }
      }
    }
  }
}

package chisel.lib.dclib

import chisel3._
import chisel3.util._


class StreamChecker(streams : Int, dwidth : Int) extends Module {
  val streamSize = log2Ceil(streams)
  val seqSize = dwidth - streamSize
  val streamId = IO(Input(UInt(streamSize.W)))
  val receiveCount = IO(Output(UInt(32.W)))
  val pausePattern = IO(Input(UInt(16.W)))
  val in = IO(Flipped(Decoupled(UInt(dwidth.W))))
  val errorCount = IO(Output(UInt(32.W)))

  val sequence = RegInit(init = 0.U(seqSize.W))
  val wordCount = RegInit(init = 0.U(32.W))
  val errCount = RegInit(init = 0.U(32.W))
  val pauseIndex = RegInit(init = 0.U(4.W))
  val seqErr = wordCount(seqSize - 1, 0) =/= in.bits(seqSize - 1, 0)
  val streamErr = streamId =/= in.bits(dwidth - 1, dwidth - streamSize)

  in.ready := !pausePattern(pauseIndex)
  receiveCount := wordCount
  errorCount := errCount

  pauseIndex := pauseIndex + 1.U

  when(in.fire) {
    wordCount := wordCount + 1.U

    when(seqErr || streamErr) {
      errCount := errCount + 1.U
    }
  }
}

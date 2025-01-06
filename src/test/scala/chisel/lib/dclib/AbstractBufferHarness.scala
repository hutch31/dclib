package chisel.lib.dclib

import chisel3._
import chisel3.util._

class AbstractBufferHarness(block : String, width : Int) extends Module {
  val tcount = IO(Input(UInt(32.W)))
  val rcount = IO(Output(UInt(32.W)))
  val start = IO(Input(Bool()))
  val errors = IO(Output(UInt(32.W)))

  val gen = Module(new StreamGenerator(2, width))
  val checker = Module(new StreamChecker(2, width))
  val dut = Module(AbstractBufferFactory(block, UInt(width.W)))

  gen.out <> dut.io.enq
  dut.io.deq <> checker.in
  gen.tcount.valid := start && !RegNext(start, 0.B)
  gen.tcount.bits := tcount

  errors := checker.errorCount
  rcount := checker.receiveCount

  gen.streamId := 1.U
  checker.streamId := 1.U
  checker.pausePattern := 0.U
}

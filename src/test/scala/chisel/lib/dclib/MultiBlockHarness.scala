package chisel.lib.dclib

import chisel3._
import chisel3.util._
class MultiBlockHarness(pipelines : Int, width : Int) extends Module {
  val tcount = IO(Input(UInt(32.W)))
  val rcount = IO(Output(Vec(pipelines, UInt(32.W))))
  val start = IO(Input(Bool()))
  val errors = IO(Output(UInt(32.W)))
  val streamSize = log2Ceil(pipelines)
  val seqSize = width - streamSize

  val gen = for (_ <- 0 until pipelines) yield Module(new StreamGenerator(pipelines, width))
  val checker = for (_ <- 0 until pipelines) yield Module(new StreamChecker(pipelines, width))
  val arb = Module(new DCArbiter(UInt(width.W), pipelines, false))
  val steer = Module(new DCDemux(UInt(width.W), pipelines))
  val startPulse = start && !RegNext(start, 0.B)
  val dcfull = Module(new DCFull(UInt(width.W)))

  for (p <- 0 until pipelines) {
    val dcinput = Module(new DCInput(UInt(width.W)))
    val dcoutput = Module(new DCOutput(UInt(width.W)))

    gen(p).tcount.valid := startPulse
    gen(p).tcount.bits := tcount
    gen(p).out <> dcinput.io.enq
    gen(p).streamId := p.U
    dcinput.io.deq <> arb.io.c(p)
    steer.io.sel := dcfull.io.deq.bits(width-1,width-streamSize)
    steer.io.p(p) <> dcoutput.io.enq
    dcoutput.io.deq <> checker(p).in
    checker(p).streamId := p.U
    rcount(p) := checker(p).receiveCount
    checker(p).pausePattern := (0xAA << pipelines).U
  }
  arb.io.p <> dcfull.io.enq
  dcfull.io.deq <> steer.io.c
  errors := checker.map(_.errorCount).reduce(_ + _)
}

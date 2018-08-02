package dclib

import chisel3._
import chisel3.iotesters.PeekPokeTester

class DCOutputTestbench extends Module {
  val io = IO(new Bundle {
    val src_pat = Input(UInt(16.W))
    val dst_pat = Input(UInt(16.W))
    val color_error = Output(Bool())
    val seq_error = Output(Bool())
  })

  val src = Module(new ColorSource(1, 16))
  val dst = Module(new ColorSink(1, 16))
  val dut = Module(new DCOutput(new ColorToken(1,16)))

  src.io.pattern := io.src_pat
  dst.io.pattern := io.dst_pat
  io.color_error := dst.io.color_error
  io.seq_error := dst.io.seq_error

  src.io.color := 0.U
  dst.io.color := 0.U

  src.io.enable := true.B
  dst.io.enable := true.B

  src.io.p <> dut.io.enq
  dut.io.deq <> dst.io.c
}

class DCOutputTester(tb: DCOutputTestbench) extends PeekPokeTester(tb) {
  poke(tb.io.src_pat, 0xFFFF.U)
  poke(tb.io.dst_pat, 0xFFFF.U)

  for (i <- 0 until 100) {
    step(1)
    expect(tb.io.color_error, false.B)
    expect(tb.io.seq_error, false.B)
  }
}

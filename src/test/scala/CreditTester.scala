package dclib

import chisel3._
import chisel3.util._
import chisel3.iotesters.PeekPokeTester

class CreditTestbench extends Module {
  val io = IO(new Bundle {
    val src_pat = Input(UInt(16.W))
    val dst_pat = Input(UInt(16.W))
    val color_error = Output(Bool())
    val seq_error = Output(Bool())
  })

  val src = Module(new ColorSource(1, 16))
  val dst = Module(new ColorSink(1, 16))
  //val dut = Module(new DCOutput(new ColorToken(1,16)))
  val srcdut = Module(new DCCreditSource(new ColorToken(1,16), 4))
  val snkdut = Module(new DCCreditSink(new ColorToken(1,16), 4))

  src.io.pattern := io.src_pat
  dst.io.pattern := io.dst_pat
  io.color_error := dst.io.color_error
  io.seq_error := dst.io.seq_error

  src.io.color := 0.U
  dst.io.color := 0.U

  src.io.enable := true.B
  dst.io.enable := true.B


  srcdut.io.enq <> src.io.p

  snkdut.io.enq.valid := ShiftRegister(srcdut.io.deq.valid, 2)
  snkdut.io.enq.bits := ShiftRegister(srcdut.io.deq.bits, 2)
  srcdut.io.deq.ready := ShiftRegister(snkdut.io.enq.ready, 2)
  snkdut.io.deq <> dst.io.c
}

class CreditTester(tb: CreditTestbench) extends PeekPokeTester(tb) {
  poke(tb.io.src_pat, 0xFFFF.U)
  poke(tb.io.dst_pat, 0xFFFF.U)

  step(100)

  // try a couple other flow control patterns
  poke(tb.io.src_pat, 0xF000.U)
  poke(tb.io.dst_pat, 0xC0A0.U)

  step(50)
  poke(tb.io.src_pat, 0xAA55.U)
  poke(tb.io.dst_pat, 0xF00F.U)

  // errors are sticky, only need to check at the end
  expect(tb.io.color_error, false.B)
  expect(tb.io.seq_error, false.B)
}

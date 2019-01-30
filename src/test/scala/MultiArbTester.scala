package dclib

import chisel3._
import chisel3.iotesters.PeekPokeTester
import chisel3.util._


class MultiArbTestbench(ways: Int, iq : Int = 0) extends Module {
  val io = IO(new Bundle {
    val src_pat = Input(UInt(16.W))
    val dst_pat = Input(UInt(16.W))
    val addr_error = Output(Bool())
    val enable = Input(Bool())
    val dst = Input(Vec(ways, UInt(log2Ceil(ways).W)))
    val cum_latency = Output(Vec(ways, UInt(32.W)))
    val pkt_count = Output(Vec(ways, UInt(32.W)))
    val cum_delay = Output(Vec(ways, UInt(32.W)))
  })

  val asz = log2Ceil(ways)
  val marb = Module(new DCMultiArbiter(new PktToken(asz), ways, ways, iq=iq))
  val i_addr_error = Wire(Vec(ways, Bool()))

  for (i <- 0 until ways) {
    val src = Module(new PktTokenSource(asz, id=i))
    val dst = Module(new PktTokenSink(asz, id=i))

    src.io.pattern := io.src_pat
    dst.io.pattern := io.dst_pat
    src.io.dst := io.dst(i)

    src.io.src := i.U
    dst.io.dest := i.U

    src.io.enable := io.enable
    dst.io.enable := io.enable

    src.io.p <> marb.io.c(i)
    marb.io.c_dest(i) := marb.io.c(i).bits.dst
    marb.io.p(i) <> dst.io.c
    i_addr_error(i) := dst.io.addr_error
    io.cum_latency(i) := dst.io.cum_latency
    io.pkt_count(i) := dst.io.pkt_count
    io.cum_delay(i) := src.io.cum_delay
  }

  io.addr_error := Cat(i_addr_error).orR()
}

class MultiArbTester(tb: MultiArbTestbench) extends PeekPokeTester(tb) {
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
  expect(tb.io.addr_error, false.B)
}

package dclib

import chisel3._
import chisel3.util._

class ColorToken(colors: Int, dsz: Int) extends Bundle {
  val color = UInt(log2Ceil(colors).W)
  val seqnum = UInt(dsz.W)
  override def cloneType =
    new ColorToken(colors, dsz).asInstanceOf[this.type]

}

class ColorSource(colors: Int, dsz: Int) extends Module {
  val io = IO(new Bundle {
    val p = Decoupled(new ColorToken(colors, dsz))
    val enable = Input(Bool())
    val pattern = Input(UInt(16.W))
    val color = Input(UInt(log2Ceil(colors).W))
  })

  val seqnum = RegInit(0.asUInt(dsz.W))
  val strobe = RegInit(0.asUInt(4.W))

  when (io.p.fire()) {
    seqnum := seqnum + 1.U
  }

  io.p.valid := io.pattern(strobe)

  // advance the strobe whenever we are not providing data or when it
  // is accepted
  when (io.p.ready || !io.pattern(strobe)) {
    strobe := strobe + 1.U
  }
  io.p.bits.color := io.color
  io.p.bits.seqnum := seqnum
}

/**
  * Receive an incoming stream of color tokens, assert error whenever
  * the tokens don't match
  */
class ColorSink(colors: Int, dsz: Int) extends Module {
  val io = IO(new Bundle {
    val c = Flipped(Decoupled(new ColorToken(colors, dsz)))
    val enable = Input(Bool())
    val pattern = Input(UInt(16.W))
    val color = Input(UInt(log2Ceil(colors).W))
    val seq_error = Output(Bool())
    val color_error = Output(Bool())
  })

  val seqnum = RegInit(0.asUInt(dsz.W))
  val strobe = RegInit(0.asUInt(4.W))
  val seq_error = RegInit(false.B)
  val color_error = RegInit(false.B)

  when (io.c.fire()) {
    seqnum := seqnum + 1.U
    when (io.c.bits.seqnum =/= seqnum) {
      seq_error := true.B
    }
    when (io.c.bits.color =/= io.color) {
      color_error := true.B
    }
  }

  io.c.ready := io.pattern(strobe)

  // advance the strobe whenever we accept a word or whenever
  // we are stalling
  when (io.c.valid || !io.pattern(strobe)) {
    strobe := strobe + 1.U
  }

  io.seq_error := seq_error
  io.color_error := color_error
}

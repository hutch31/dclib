package chisel.lib.dclib

import chisel3.util._
import chisel3._

/**
  * Demultiplex a stream of tokens with an identifier "sel",
  * as inverse of RRArbiter.
  *
  * @param data  Data type of incoming/outgoing data
  * @param n     Number of mux outputs
  */
class DCDemux[D <: Data](data: D, n: Int) extends Module {
  val io = IO(new Bundle {
    val sel = Input(UInt(log2Ceil(n).W))
    val c = Flipped(new DecoupledIO(data.cloneType))
    val p = Vec(n, new DecoupledIO(data.cloneType))
  })
  override def desiredName: String = "DCDemux_" + data.toString

  io.c.ready := 0.U
  for (i <- 0 until n) {
    io.p(i).bits := io.c.bits
    when (i.U === io.sel) {
      io.p(i).valid := io.c.valid
      io.c.ready := io.p(i).ready
    }.otherwise {
      io.p(i).valid := 0.U
    }
  }
}

class DCMux[D <: Data](data: D, n: Int) extends Module {
  val io = IO(new Bundle {
    val sel = Input(UInt(log2Ceil(n).W))
    val c = Vec(n, Flipped(new DecoupledIO(data.cloneType)))
    val p = new DecoupledIO(data.cloneType)
  })
  io.p <> io.c(0)
  for (i <- 0 until n) {
    when (i.U === io.sel) {
      io.p <> io.c(i)
    }.otherwise {
      io.c(i).ready := 0.B
    }
  }
}



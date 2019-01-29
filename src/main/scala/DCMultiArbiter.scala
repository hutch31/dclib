package dclib

import chisel3._
import chisel3.util._

/**
  * Receive requests from N sources and send to M destinations
  * Arbitrate source and destination requests using round-robin arbiters
  *
  * This is an input-based implementation which has known HOL issues,
  * maximum throughput is 58% with uniform distribution.
  */
class DCMultiArbiter[D <: Data](data: D, nsource: Int, ndest: Int) extends Module {
  val io = IO(new Bundle {
    val c = Vec(nsource, Flipped(Decoupled(data.cloneType)))
    val c_dest = Vec(nsource, Input(UInt(log2Ceil(ndest).W)))

    val p = Vec(ndest, Decoupled(data.cloneType))
    val p_grant = Vec(ndest, Output(UInt(log2Ceil(nsource).W)))
  })

  val mirvec = for (i <- 0 until nsource) yield {
    val imirror = Module(new DCMirror(data, ndest))
    imirror.io.c <> io.c(i)
    imirror.io.dst := 1.U << io.c_dest(i)
    imirror
  }

  val arbvec = for (i <- 0 until ndest) yield {
    val iarb = Module(new DCArbiter(data, nsource, locking = false))
    for (j <- 0 until nsource) {
      iarb.io.c(j) <> mirvec(j).io.p(i)
    }
    iarb.io.p <> io.p(i)
    io.p_grant(i) := iarb.io.grant
    iarb
  }
}

package dclib

import chisel3._
import chisel3.util._

class DeficitWeightedRR[D <: Data](data: D, n: Int, max: Int) extends Module {
  val io = IO(new Bundle {
    val req = Vec(n, Flipped(new DecoupledIO(data.cloneType)))
    val req_qty = Vec(n, Input(UInt(log2Ceil(max).W)))
    val resp = new DecoupledIO(data.cloneType)
    val gnt = Output(UInt(log2Ceil(n).W))
    val quant = Vec(n, Input(UInt(log2Ceil(max).W)))
    val limit = Vec(n, Input(UInt(log2Ceil(max).W)))
  })

  val enable = Wire(Bool())
  val nxt_dc = Wire(Vec(n, UInt(log2Ceil(max).W)))
  val nxt_fill_dc = Wire(Vec(n, UInt(log2Ceil(max).W)))
  val has_credit = Wire(Vec(n, Bool()))
  val masked_has_credit = Wire(Vec(n, Bool()))
  val dc = RegEnable(next=nxt_dc, enable=enable)
  val nxt_gnt = Wire(UInt(log2Ceil(n).W))
  val gnt_r = RegEnable(init=0.U, next=nxt_gnt, enable=enable)
  val nxt_valid = Wire(Bool())
  val valid_r = RegEnable(init=false.B, next=nxt_valid, enable=enable)
  val data_r = RegEnable(next=io.req(nxt_gnt).bits, enable=enable)
  val refill = Wire(Bool())

  //nxt_dc := dc
  nxt_gnt := gnt_r
  io.gnt := gnt_r

  // When no device can send, fill all buckets with credit
  refill := true.B
  for (i <- 0 until n) {
    when (io.req(i).valid && (dc(i) >= io.req_qty(i))) {
      refill := false.B
    }
  }

  when (reset.toBool()) {
    for (i <- 0 until n) {
      nxt_fill_dc(i) := 0.U
      has_credit(i) := false.B
      masked_has_credit(i) := false.B
    }
  }.otherwise {
    for (i <- 0 until n) {
      when(io.req(i).valid) {
        val tmp = dc(i) + io.quant(i)
        when (refill && (tmp > io.limit(i))) {
          nxt_fill_dc(i) := io.limit(i)
        }.elsewhen (refill) {
          nxt_fill_dc(i) := tmp
        }.otherwise {
          nxt_fill_dc(i) := dc(i)
        }
        has_credit(i) := (nxt_fill_dc(i) >= io.req_qty(i))
      }.otherwise {
        nxt_fill_dc(i) := 0.U
        has_credit(i) := false.B
      }
      masked_has_credit(i) := has_credit(i) && (i.U > gnt_r)
    }
  }

  // select a bucket
  when (masked_has_credit.asUInt().orR()) {
    nxt_gnt := PriorityEncoder(masked_has_credit)
  }.otherwise {
    nxt_gnt := PriorityEncoder(has_credit)
  }

  // subtract credit from granted bucket
  nxt_valid := false.B
  for (i <- 0 until n) {
    when (io.req(i).valid && (nxt_gnt === i.U) && has_credit(i)) {
      nxt_dc(i) := nxt_fill_dc(i) - io.req_qty(i)
      nxt_valid := true.B
    }.otherwise {
      nxt_dc(i) := nxt_fill_dc(i)
    }
  }

  for (i <- 0 until n) {
    io.req(i).ready := io.resp.ready && (gnt_r === i.U)
  }

  enable := reset.toBool() || !valid_r || (valid_r && io.resp.ready)
  io.resp.valid := valid_r
  io.resp.bits := data_r
}


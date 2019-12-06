package dclib

import chisel3._
import chisel3.util._
import chisel3.core.withClock
import chisel3.core.withClockAndReset

class SyncFifo[D <: Data](data: D, size: Int) extends Module {
  // This sync FIFO must be a natural power of two to work correctly
  require (size == scala.math.pow(2, log2Ceil(size)).toInt)

  val ptrsz = log2Ceil(size)+1
  val io = IO(new Bundle {
    val enq = Flipped(Decoupled(data.cloneType))
    val deq = Decoupled(data.cloneType)

    val deq_clock = Input(Clock())
    val deq_reset = Input(Bool())
    val enq_usage = Output(UInt(ptrsz.W))
    val deq_usage = Output(UInt(ptrsz.W))
  })
  override def desiredName: String = "SyncFifo_" + data.toString

  val head = Module(new SyncFifoHead(size))
  val tail = withClockAndReset(io.deq_clock, io.deq_reset)(Module(new SyncFifoTail(size)))
  //val mem = Module(new SyncFifoMem(data, size))
  val mem = SyncReadMem(size, data.cloneType)
  val tsync1 = Reg(UInt(ptrsz.W))
  val tsync2 = Reg(UInt(ptrsz.W))

  io.enq.ready := head.io.enq_ready
  head.io.enq_valid := io.enq.valid
  io.enq_usage := head.io.usage

  when (head.io.wr_en) {
    mem.write(head.io.wr_addr, io.enq.bits)
  }
  //mem.io.wr_data := io.enq.bits
  //mem.io.wr_en := head.io.wr_en
  //mem.io.wr_addr := head.io.wr_addr

  tsync1 := tail.io.rdptr_tail
  tsync2 := tsync1
  head.io.rdptr_tail := tsync2

  withClockAndReset(io.deq_clock, io.deq_reset) {
    val hsync1 = Reg(UInt(ptrsz.W))
    val hsync2 = Reg(UInt(ptrsz.W))

    io.deq.valid := tail.io.deq_valid
    tail.io.deq_ready := io.deq.ready
    io.deq.bits := mem.read(tail.io.rd_addr)
    io.deq_usage := tail.io.usage

    //mem.io.rd_en := tail.io.rd_en
    //mem.io.rd_addr := tail.io.rd_addr
    //mem.io.rd_clock := io.deq_clock

    hsync1 := head.io.wrptr_head
    hsync2 := hsync1
    tail.io.wrptr_head := hsync2
  }
}

class SyncFifoMem[D <: Data](data: D, size: Int) extends Module {
  val asz = log2Ceil(size)
  val io = IO(new Bundle {
    val wr_addr = Input(UInt(asz.W))
    val wr_data = Input(data.cloneType)
    val wr_en = Input(Bool())
    val rd_clock = Input(Clock())
    val rd_data = Output(data.cloneType)
    val rd_addr = Input(UInt(asz.W))
    val rd_en = Input(Bool())

  })
  val ram = Mem(data.cloneType, size)
  val rdata = Reg(data.cloneType)

  when(io.wr_en) {
    ram(io.wr_addr) := io.wr_data
  }

  withClock(io.rd_clock) {
    when (io.rd_en) {
      rdata := ram(io.rd_addr)
    }
  }
  io.rd_data := rdata
}


class SyncFifoHead(depth: Int) extends Module {
  val asz = log2Ceil(depth)
  val ptrsz = asz+1
  val io = IO(new Bundle {
    val enq_valid = Input(Bool())
    val enq_ready = Output(Bool())

    val wrptr_head = Output(UInt(ptrsz.W))
    val rdptr_tail = Input(UInt(ptrsz.W))
    val wr_addr = Output(UInt(asz.W))
    val wr_en = Output(Bool())

    val usage = Output(UInt(ptrsz.W))
  })

  val nxt_wrptr = Wire(UInt(ptrsz.W))
  val wrptr = Wire(UInt(ptrsz.W))
  //val wrptr = RegNext(init=0.U, next=nxt_wrptr)
  val full = Wire(Bool())
  val rdptr = Wire(UInt(ptrsz.W))
  val wrptr_head = RegInit(init=0.asUInt(ptrsz.W))
  val wrptr_head_gc = dclib.grey2bin(wrptr, ptrsz)

  io.enq_ready := !full
  io.wr_addr := wrptr(asz-1,0)

  full := ((wrptr(asz-1,0) === rdptr(asz-1,0)) && (wrptr(asz) === !rdptr(asz)))

  when (io.enq_valid && !full) {
    nxt_wrptr := wrptr + 1.U
  }.otherwise {
    nxt_wrptr := wrptr
  }

  io.wr_en := io.enq_valid && !full

  when (wrptr(asz) === rdptr(asz)) {
    io.usage := wrptr - rdptr
  }.otherwise {
    io.usage := (wrptr(asz - 1,0)+depth.U) - rdptr(asz-1,0)
  }

  wrptr := dclib.grey2bin(wrptr_head, ptrsz)
  wrptr_head := RegNext(wrptr_head_gc)

  rdptr := dclib.grey2bin(io.rdptr_tail, ptrsz)
  io.wrptr_head := wrptr_head
}


class SyncFifoTail(depth: Int) extends Module {
  val asz = log2Ceil(depth)
  val ptrsz = asz + 1
  val io = IO(new Bundle {
    //val deq = Decoupled(data.cloneType)
    val deq_valid = Output(Bool())
    val deq_ready = Input(Bool())

    val wrptr_head = Input(UInt(ptrsz.W))
    val rdptr_tail = Output(UInt(ptrsz.W))
    val rd_addr = Output(UInt(asz.W))
    val rd_en = Output(Bool())

    val usage = Output(UInt(ptrsz.W))
  })

  val rdptr = Wire(UInt(ptrsz.W))
  val nxt_rdptr = Wire(UInt(ptrsz.W))
  val rdptr_tail = RegInit(0.asUInt(ptrsz.W))
  val nxt_valid = Wire(Bool())
  val valid = RegNext(init=false.B, next=nxt_valid)
  val wrptr = Wire(UInt(ptrsz.W))

  when (io.deq_valid && io.deq_ready) {
    nxt_rdptr := rdptr + 1.U
  }.otherwise {
    nxt_rdptr := rdptr
  }

  nxt_valid := (wrptr =/= nxt_rdptr)
  io.rd_en := nxt_valid && !(valid && !io.deq_ready)

  when (wrptr(asz) === rdptr(asz)) {
    io.usage := wrptr - rdptr
  }.otherwise {
    io.usage := (wrptr(asz-1,0) + depth.U - rdptr(asz-1,0))
  }

  rdptr_tail := dclib.bin2grey(nxt_rdptr, ptrsz)
  rdptr := dclib.grey2bin(rdptr_tail, ptrsz)
  io.rdptr_tail := rdptr_tail
  wrptr := dclib.grey2bin(io.wrptr_head, ptrsz)
  io.deq_valid := valid
  io.rd_addr := rdptr(asz-1,0)
}

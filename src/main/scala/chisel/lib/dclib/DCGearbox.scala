package chisel.lib.dclib

import chisel3._
import chisel3.util._

import scala.collection.mutable.ArrayBuffer

/**
 * Creates a Decoupled Gearbox, a block which converts a data stream from any input width to any
 * output width.
 *
 * @param inWidth   Bit width of data input
 * @param outWidth  Bit width of data output
 */
class DCGearbox(inWidth : Int, outWidth : Int, hasClear : Boolean = false) extends Module {
  val holdSize = if (inWidth > outWidth) 2 else outWidth / inWidth + 2
  val io = IO(new Bundle {
    val c = Flipped(Decoupled(UInt(inWidth.W)))
    val p = Decoupled(UInt(outWidth.W))
    val clear = if (hasClear) Some(Input(Bool())) else None
  })
  val modTable = VecInit(for (i <- 0 until (inWidth+outWidth)) yield (i % inWidth).U)
  val divTable = VecInit(for (i <- 0 until (inWidth+outWidth)) yield (i / inWidth).U)
  def divide(a : UInt) : UInt = {
    //a / inWidth.U
    divTable(a)
  }
  def modulo(a : UInt) : UInt = {
    //a % inWidth.U
    modTable(a)
  }
  val hold = Reg(Vec(holdSize, UInt(inWidth.W)))
  val bitCount = RegInit(init=0.U(log2Ceil(holdSize*inWidth+1).W))
  val bitShift = RegInit(init=0.U(log2Ceil(inWidth).W))
  val wordCount = RegInit(init=0.U(log2Ceil(holdSize+1).W))
  val nextInHold = Wire(Vec(holdSize+1, UInt(inWidth.W)))
  val nextShiftHold = Wire(Vec(holdSize+1, UInt(inWidth.W)))
  val wordShift = divide(outWidth.U +& bitShift)

  io.p.valid := bitCount >= outWidth.U
  io.p.bits := Cat(hold.reverse) >> bitShift
  io.c.ready := wordCount =/= holdSize.U

  for (i <- 0 to holdSize) {
    when (i.U >= wordCount) {
      nextInHold(i) := io.c.bits
    }.otherwise {
      if (i >= holdSize)
        nextInHold(i) := 0.U
      else
        nextInHold(i) := hold(i)
    }

    when (wordShift + i.U < holdSize.U) {
      nextShiftHold(i) := nextInHold(i.U + wordShift)
    }.otherwise {
      nextShiftHold(i) := 0.U
    }
  }

  when (io.p.fire) {
    bitShift := modulo(outWidth.U +& bitShift)
    for (i <- 0 until holdSize)
      hold(i) := nextShiftHold(i)
    when (io.c.fire) {
      wordCount := wordCount +& 1.U - wordShift
      bitCount := bitCount +& inWidth.U - outWidth.U
    }.otherwise {
      wordCount := wordCount - wordShift
      bitCount := bitCount - outWidth.U
    }
  }.elsewhen(io.c.fire) {
    for (i <- 0 until holdSize)
      hold(i) := nextInHold(i)
    wordCount := wordCount + 1.U
    bitCount := bitCount +& inWidth.U
  }

  if (hasClear) {
    when (io.clear.get) {
      bitCount := 0.U
      bitShift := 0.U
      wordCount := 0.U
      io.c.ready := 0.B
      io.p.valid := 0.B
    }
  }
}

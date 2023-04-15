package chisel.lib.dclib.experimental

import chisel3._
import chisel3.util._

class DCSerial[D <: Data](data: D, clockPerBit : Int = 8) extends Module {
  override def desiredName : String = "dc_serial_" + data.toString + "_cpb" + clockPerBit.toString

  val io = IO(new Bundle {
    val dataIn = Flipped(Decoupled(data.cloneType))
    val serialClk = Output(Bool())
    val serialData = Output(Bool())
  })

  val txbits = data.getWidth+2
  val hold = Reg(UInt(txbits.W))
  val bitcount = Reg(UInt(log2Ceil(txbits).W))
  val sercount = Reg(UInt(log2Ceil(clockPerBit).W))
  val regout = RegInit(init = 0.B)
  val regclk = RegInit(init = 0.B)
  val s_idle :: s_transmit1 :: s_transmit0 :: Nil = Enum(3)
  val state = RegInit(init = s_idle)

  io.serialClk := regclk
  io.serialData := regout

  io.dataIn.ready := false.B
  switch(state) {
    is(s_idle) {
      bitcount := 0.U
      sercount := 0.U
      regout := 0.B
      regclk := 0.B
      io.dataIn.ready := true.B
      when(io.dataIn.valid) {
        hold := Cat(io.dataIn.bits.asUInt().xorR(), io.dataIn.bits.asUInt(), 1.U(1.W))
        state := s_transmit0
      }
    }

    is(s_transmit0) {
      regout := hold(bitcount)
      regclk := 0.B
      sercount := sercount + 1.U
      when (sercount === (clockPerBit/2).U) {
        state := s_transmit1
      }
    }

    is (s_transmit1) {
      regclk := 1.B
      when (sercount === (clockPerBit-1).U) {
        sercount := 0.U
        when (bitcount === (txbits-1).U) {
          state := s_idle
        }.otherwise {
          state := s_transmit0
          bitcount := bitcount + 1.U
        }
      }.otherwise {
        sercount := sercount + 1.U
      }
    }
  }
}

class DCDeserial[D <: Data](data: D) extends Module {
  val io = IO(new Bundle {
    val dataOut = Decoupled(data.cloneType)
    val serialClk = Input(Bool())
    val serialData = Input(Bool())
    val parityErr = Output(Bool())
    val clear = Input(Bool())  // sync reset module state
  })
  override def desiredName : String = "dc_deserial_" + data.toString

  val txbits = data.getWidth+2
  val hold = Reg(Vec(txbits, Bool()))
  val bitcount = RegInit(init=0.U(log2Ceil(txbits).W))
  val prevClk = RegNext(next=io.serialClk)
  val posEdge = io.serialClk & !prevClk
  val s_idle :: s_receive :: s_hold :: Nil = Enum(3)
  val state = RegInit(init = s_idle)
  val cathold = Wire(UInt(txbits.W))

  cathold := Cat(hold.reverse)
  io.dataOut.valid := false.B
  io.parityErr := 0.B
  io.dataOut.bits := cathold(txbits-2,1).asTypeOf(io.dataOut.bits)

  switch (state) {
    is (s_idle) {
      when(posEdge) {
        hold(bitcount) := io.serialData
        state := s_receive
        bitcount := bitcount + 1.U
      }
    }

    is (s_receive) {
      when(posEdge) {
        hold(bitcount) := io.serialData
        bitcount := bitcount + 1.U
        when (bitcount === (txbits-1).U) {
          state := s_hold
        }
      }
    }

    is (s_hold) {
      io.parityErr := cathold(txbits-1,1).xorR()
      bitcount := 0.U
      io.dataOut.valid := true.B
      when (io.dataOut.ready) {
        state := s_idle
      }
    }
  }

  when (io.clear) {
    state := s_idle
    bitcount := 0.U
  }
}
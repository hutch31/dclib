package dclib
import chisel3._
import chisel3.util._

/**
  * Creates a ready/valid holding register, will not accept new data
  * until current data word is unloaded
  *
  * @param data The data type for the payload
  */
class DCHold[D <: Data](data: D) extends Module {
  val io = IO(new Bundle {
    val enq = Flipped(new DecoupledIO(data.cloneType))
    val deq = new DecoupledIO(data.cloneType)
  })

  val p_valid = RegInit(init = 0.U)
  val p_data = Reg(data.cloneType)

  when (io.enq.valid && !p_valid) {
    p_valid := io.enq.valid
    p_data := io.enq.bits
  }.elsewhen((p_valid & io.deq.ready) === 1.U) {
    p_valid := 0.U
  }
  io.deq.valid := p_valid
  io.deq.bits := p_data
  io.enq.ready := ~p_valid
}

// Helper function for functional inference
object DCHold {
  def apply[D <: Data](x : DecoupledIO[D]) : DecoupledIO[D] = {
    val tout = Module(new DCHold(x.bits.cloneType))
    tout.io.enq <> x
    tout.io.deq
  }
}

class DCHoldPipe[D <: Data](data: D, stages: Int=1) extends Module {
  val io = IO(new Bundle {
    val enq = Flipped(new DecoupledIO(data.cloneType))
    val deq = new DecoupledIO(data.cloneType)
  })

  if (stages == 0) {
    io.enq <> io.deq
  } else {
    val holders = for (i <- 0 until stages) yield Module(new DCHold(data.cloneType))

    for (i <- 0 until stages) {
      if (i == 0) {
        holders(i).io.enq <> io.enq
      } else {
        holders(i).io.enq <> holders(i-1).io.deq
      }
    }
    holders(stages-1).io.deq <> io.deq
  }
}

// Helper function for functional inference
object DCHoldPipe {
  def apply[D <: Data](x : DecoupledIO[D], s : Int = 1) : DecoupledIO[D] = {
    val tout = Module(new DCHoldPipe(x.bits.cloneType, s))
    tout.io.enq <> x
    tout.io.deq
  }
}

// Helper function for functional inference
object DCPipe {
  def apply[D <: Data](x : DecoupledIO[D], s : Int = 1) : DecoupledIO[D] = {
    if (s == 0) return x
    val dcin = for (i <- 0 until s) yield Module(new DCInput(x.bits.cloneType))
    val dcout = for (i <- 0 until s) yield Module(new DCOutput(x.bits.cloneType))

    for (i <- 0 until s) {
      if (i == 0) {
        dcin(i).io.enq <> x
      } else {
        dcin(i).io.enq <> dcout(i-1).io.deq
      }
      dcin(i).io.deq <> dcout(i).io.enq
    }

    dcout(s-1).io.deq
  }
}


class DCInput[D <: Data](data: D) extends Module {
  val io = IO(new Bundle {
    val enq = Flipped(new DecoupledIO(data.cloneType))
    val deq = new DecoupledIO(data.cloneType)
  })
 // val r_valid = RegInit(false.B)
  val ready_r = RegInit(true.B)
  val occupied = RegInit(false.B)
  val hold = Reg(data.cloneType)
  val load = Wire(Bool())
  val drain = Wire(Bool())

  drain := occupied && io.deq.ready
  load := io.enq.valid && ready_r && (!io.deq.ready || drain)

  when (occupied) {
    io.deq.bits := hold
  }.otherwise {
    io.deq.bits := io.enq.bits
  }

  io.deq.valid := io.enq.valid || occupied
  when (load) {
    occupied := true.B
    hold := io.enq.bits
  }.elsewhen (drain) {
    occupied := false.B
  }

  ready_r := (!occupied && !load) || (drain && !load)
  io.enq.ready := ready_r
}

// Helper function for functional inference
object DCInput {
  def apply[D <: Data](x: DecoupledIO[D]): DecoupledIO[D] = {
    val tout = Module(new DCInput(x.bits.cloneType))
    tout.io.enq <> x
    tout.io.deq
  }
}

/**
  * Closes output timing on an input of type D
  * valid and bits will be registered, ready will be combinatorial
  * @param data
  * @tparam D
  */
class DCOutput[D <: Data](data: D) extends Module {
  val io = IO(new Bundle {
    val enq = Flipped(new DecoupledIO(data.cloneType))
    val deq = new DecoupledIO(data.cloneType)
  })
  val r_valid = RegInit(false.B)

  io.enq.ready := io.deq.ready || !r_valid
  r_valid := io.enq.fire() || (r_valid && !io.deq.ready)
  io.deq.bits := RegEnable(next=io.enq.bits, enable=io.enq.fire())
  io.deq.valid := r_valid
}

// Helper function for functional inference
object DCOutput {
  def apply[D <: Data](x : DecoupledIO[D]) : DecoupledIO[D] = {
    val tout = Module(new DCOutput(x.bits.cloneType))
    tout.io.enq <> x
    tout.io.deq
  }
}

object DCFull {
  def apply[D <: Data](x : DecoupledIO[D]) : DecoupledIO[D] = {
    val tin = Module(new DCInput(x.bits.cloneType))
    val tout = Module(new DCOutput(x.bits.cloneType))
    tin.io.enq <> x
    tin.io.deq <> tout.io.enq
    tout.io.deq
  }
}

/**
  * Demultiplex a stream of tokens with an identifier "sel",
  * as inverse of RRArbiter.
  * @param data  Data type of incoming/outgoing data
  * @param n     Number of mux outputs
  */
class DCDemux[D <: Data](data: D, n: Int) extends Module {
  val io = IO(new Bundle {
    val sel = Input(UInt(log2Ceil(n).W))
    val c = Flipped(new DecoupledIO(data.cloneType))
    val p = Vec(n, new DecoupledIO(data.cloneType))
  })

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

/**
  * Sends tokens to multiple output destinations, as selected by bit
  * vector "dst".  dst must have at least one bit set for correct
  * operation.
  *
  * @param data Payload data type
  * @param n    Number of output destinations
  */
class DCMirror[D <: Data](data: D, n: Int) extends Module {
  val io = IO(new Bundle {
    val dst = Input(UInt(n.W))
    val c = Flipped(new DecoupledIO(data.cloneType))
    val p = Vec(n, new DecoupledIO(data.cloneType))
  })

  val p_data = Reg(data.cloneType)
  val p_valid = RegInit(0.asUInt(n.W))
  val p_ready = Cat(io.p.map(_.ready).reverse)
  val nxt_accept = (p_valid === 0.U) || ((p_valid =/= 0.U) && ((p_valid & p_ready) === p_valid))

  when (nxt_accept) {
    p_valid := Fill(n, io.c.valid) & io.dst
    p_data := io.c.bits
  }.otherwise {
    p_valid := p_valid & ~p_ready
  }
  io.c.ready := nxt_accept

  for (i <- 0 until n) {
    io.p(i).bits := p_data
    io.p(i).valid := p_valid(i)
  }
}

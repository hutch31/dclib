package dclib
import chisel3._
import chisel3.util._


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

/**
  * Provides timing closure on valid, ready and bits interfaces by
  * using DCInput and DCOutput back to back.  Effectively a 2-entry
  * FIFO.
  */
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

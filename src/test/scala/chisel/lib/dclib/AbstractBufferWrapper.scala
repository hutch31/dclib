package chisel.lib.dclib

import chisel3._

class GearboxWrapper[D <: Data](data : D) extends DCAbstractBuffer(data) {
  val up = Module(new DCGearbox(data.getWidth, data.getWidth*2))
  val down = Module(new DCGearbox(data.getWidth*2, data.getWidth))

  io.enq <> up.io.c
  up.io.p <> down.io.c
  down.io.p <> io.deq
}

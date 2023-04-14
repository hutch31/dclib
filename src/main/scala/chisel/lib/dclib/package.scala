package chisel.lib

import chisel3._

package object dclib {
  def bin2grey(x : UInt) : UInt = (x >> 1.U) ^ x

  def defaultDoubleSync(x : UInt) : UInt = {
    RegNext(RegNext(x))
  }
}

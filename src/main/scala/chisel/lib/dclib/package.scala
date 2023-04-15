package chisel.lib

import chisel3._

package object dclib {
  def defaultDoubleSync(x : UInt) : UInt = {
    RegNext(RegNext(x))
  }
}

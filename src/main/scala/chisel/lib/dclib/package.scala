//----------------------------------------------------------------------
// This file has no Copyright, as it is released in the public domain
// Author: Guy Hutchison (guy@ghutchis.org)
// see http://unlicense.org/
//----------------------------------------------------------------------


package chisel.lib

import chisel3._

package object dclib {
  def defaultDoubleSync(x : UInt) : UInt = {
    RegNext(RegNext(x))
  }
}

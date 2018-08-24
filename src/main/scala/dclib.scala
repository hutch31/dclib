package dclib

import chisel3._
import chisel3.util._

package object dclib {
  def bin2grey(in: UInt, bits: Int) : UInt = {
    val grey = Wire(Vec(bits, Bool()))

    for (i <- 0 until bits) {
      if (i == (bits-1)) {
        grey(i) := in(i)
      } else {
        grey(i) := in(i) ^ in(i+1)
      }
    }
    Cat(grey.reverse)
  }

  def grey2bin(in: UInt, bits: Int) : UInt = {
    val bin = Wire(Vec(bits, Bool()))

    bin(bits-1) := in(bits-1)
    for (i <- (bits-2) to 0 by -1) {
      bin(i) := in(i) ^ bin(i+1)
    }
    Cat(bin.reverse)
  }
}

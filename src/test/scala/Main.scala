/**
 *
 */
package dclib

import chisel3._
import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}


object Main extends App {
  // Generate RTL
  chisel3.Driver.execute(args, () => new DCOutputTestbench)

  // Run unit tests
  iotesters.Driver.execute(args, () => new DCOutputTestbench) {
    tb => new DCOutputTester(tb)
  }
  iotesters.Driver.execute(args, () => new ArbMirrorTestbench(4)) {
    tb => new ArbMirrorTester(tb)
  }
}

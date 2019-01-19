/**
 *
 */
package dclib

import chisel3._
import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}


object Main extends App {
  // Run unit tests
  iotesters.Driver.execute(args, () => new DCOutputTestbench) {
    tb => new DCOutputTester(tb)
  }

  iotesters.Driver.execute(args, () => new ArbMirrorTestbench(5)) {
    tb => new ArbMirrorTester(tb)
  }

  iotesters.Driver.execute(args, () => new DCDomainCrossingTestbench) {
    tb => new DomainCrossingTester(tb)
  }
  //iotesters.Driver.execute(args, () => new SyncFifoTestbench) {
  //  tb => new SyncFifoTester(tb)
  //}

  //chisel3.Driver.execute(args, () => new SyncFifo(UInt(16.W), size=16))
}


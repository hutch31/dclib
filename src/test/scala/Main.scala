/**
 *
 */
package dclib

import chisel3._
import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}


object testMain extends App {
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
  iotesters.Driver.execute(args, () => new CreditTestbench) {
    tb => new CreditTester(tb)
  }

  iotesters.Driver.execute(args, () => new MultiArbTestbench(8)) {
    tb => new MultiArbTester(tb)
  }

  chisel3.Driver.execute(args, () => new MultiArbTestbench(ways=8, iq=4))
}

object buildFifo extends App {
  chisel3.Driver.execute(args, () => new SyncFifo(UInt(16.W), size=16))
}

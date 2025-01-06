package chisel.lib.dclib

import chisel3._
import chisel3.util.DecoupledIO
import chiseltest._
import chiseltest.simulator.VerilatorFlags
import org.scalatest.freespec.AnyFreeSpec

import scala.util.Random

object AbstractBufferFactory {
  def apply[D <: Data](s : String, dtype : D) : DCAbstractBuffer[D] = {
    s match {
      case "DCInput" => new DCInput(dtype)
      case "DCOutput" => new DCOutput(dtype)
      case "DCGearbox" => new GearboxWrapper(dtype)
      case "DCHold" => new DCHold(dtype)
      case _ => new DCFull(dtype)
    }
  }
}

class TestAbstractBuffer  extends AnyFreeSpec with ChiselScalatestTester{
  val dumpEnable = true
  val annotations = if (dumpEnable) Seq(WriteVcdAnnotation) else Seq()
  val moduleList = Seq("DCInput", "DCOutput", "DCFull", "DCHold", "DCGearbox")

  "test abstract harness" in {
    for (m <- moduleList) {
      test(new AbstractBufferHarness(m, 8)).withAnnotations(annotations) {
        c => {
          c.clock.step()
          c.tcount.poke(1000)
          c.start.poke(1)
          c.clock.step()
          c.start.poke(0)
          while (c.rcount.peekInt() < 100) {
            c.clock.step()
          }
          c.errors.expect(0)
        }
      }
    }
  }

  "test multi block" in {
    test(new MultiBlockHarness(4, 8)).withAnnotations(annotations) {
      c => {
        var done = false
        val numTransaction = 1000
        c.clock.step()
        c.tcount.poke(numTransaction)
        c.start.poke(1)
        c.clock.step()
        c.start.poke(0)
        c.clock.setTimeout(numTransaction * 4 + 100)
        while (!done) {
          c.clock.step()
          val rcount = c.rcount.map(_.peekInt()).min
          done = rcount >= numTransaction
        }
        c.errors.expect(0)
      }
    }
  }
}

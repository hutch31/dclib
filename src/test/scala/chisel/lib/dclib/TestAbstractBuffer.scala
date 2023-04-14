package chisel.lib.dclib

import chisel3._
import chisel3.util.DecoupledIO
import chiseltest._
import chiseltest.simulator.VerilatorFlags
import org.scalatest.freespec.AnyFreeSpec

import scala.util.Random

object AbstractBufferFactory {
  def apply[D <: Data](s : String, dtype : D) : DCAbstractBuffer[D] = {
    if (s == "DCInput") return new DCInput(dtype)
    else if (s == "DCOutput") return new DCOutput(dtype)
    else if (s == "DCGearbox") new GearboxWrapper(dtype)
    else new DCFull(dtype)
  }
}

class TestAbstractBuffer  extends AnyFreeSpec with ChiselScalatestTester{
  val dumpEnable = false
  val annotations = if (dumpEnable) Seq(WriteVcdAnnotation) else Seq()
  val moduleList = Seq("DCInput", "DCOutput", "DCFull", "DCHold", "DCGearbox")

  "start and stop randomly" in {
    for (m <- moduleList) {
      println(s"Testing module $m")
      test(AbstractBufferFactory(m, UInt(16.W))).withAnnotations(annotations) {
        c => {
          c.io.enq.initSource().setSourceClock(c.clock)
          c.io.deq.initSink().setSinkClock(c.clock)
          val rand = new Random(1)

          val total_count = 250
          var tx_count: Int = 0
          var rx_count: Int = 0
          var tx_prob: Float = 0.1f
          var rx_prob: Float = 0.1f

          fork {
            while (tx_count < total_count) {
              tx_prob = 1.0f - (tx_count.toFloat / total_count.toFloat) * 0.8f
              if (rand.nextFloat() > tx_prob) {
                c.clock.step(1)
              }
              c.io.enq.enqueue(tx_count.U)
              tx_count += 1
            }
          }.fork {
            while (rx_count < total_count) {
              rx_prob = (rx_count.toFloat / total_count.toFloat) * 0.8f + 0.2f
              if (rand.nextFloat() > rx_prob) {
                c.clock.step(1)
              }
              c.io.deq.expectDequeue(rx_count.U)
              rx_count += 1
            }
          }.join()
        }
      }
    }
  }

  "test performance" in {
    for (m <- Seq("DCInput", "DCOutput", "DCFull")) {
      test(AbstractBufferFactory(m, UInt(16.W))).withAnnotations(annotations) {
        c => {
          c.io.enq.initSource().setSourceClock(c.clock)
          c.io.deq.initSink().setSinkClock(c.clock)
          val rand = new Random(1)

          val total_count = 250
          var tx_count: Int = 0
          var rx_count: Int = 0

          fork {
            while (tx_count < total_count) {
              c.io.enq.enqueue(tx_count.U)
              tx_count += 1
              c.io.enq.ready.expect(1)
            }
          }.fork {
            while (rx_count < total_count) {
              c.io.deq.expectDequeue(rx_count.U)
              rx_count += 1
            }
          }.join()
        }
      }
    }
  }
}

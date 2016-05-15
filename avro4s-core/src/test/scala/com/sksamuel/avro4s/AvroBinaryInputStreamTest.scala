package com.sksamuel.avro4s

import java.io.ByteArrayOutputStream

import org.scalatest.{Matchers, WordSpec}

class AvroBinaryInputStreamTest extends WordSpec with Matchers {

  case class Composer(name: String, birthplace: String, compositions: Seq[String])
  val ennio = Composer("ennio morricone", "rome", Seq("legend of 1900", "ecstasy of gold"))
  val hans = Composer("hans zimmer", "berlin", Seq("batman", "superman"))

  "AvroBinaryInputStream" should {
    "load binary output" in {

      val baos = new ByteArrayOutputStream()
      val output = AvroOutputStream[Composer](baos, false)
      output.write(ennio)
      output.write(hans)
      output.close()

      new String(baos.toByteArray) should not include "birthplace"
      new String(baos.toByteArray) should not include "compositions"

      val input = AvroInputStream[Composer](baos.toByteArray, true)
      input.iterator.toSet shouldBe Set(ennio, hans)
    }
  }
}

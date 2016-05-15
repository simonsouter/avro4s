package com.sksamuel.avro4s

import java.io._
import java.nio.file.{Files, Path, Paths}

import org.apache.avro.file.{DataFileReader, SeekableByteArrayInput, SeekableFileInput, SeekableInput}
import org.apache.avro.generic.{GenericDatumReader, GenericRecord}
import org.apache.avro.io.DecoderFactory

import scala.util.Try

trait AvroInputStream[T] {
  def close(): Unit
  def iterator: Iterator[T]
  def tryIterator: Iterator[Try[T]]
}

class AvroDataInputStream[T](in: SeekableInput)(implicit schemaFor: SchemaFor[T], fromRecord: FromRecord[T]) extends AvroInputStream[T] {

  val datumReader = new GenericDatumReader[GenericRecord](schemaFor())
  val dataFileReader = new DataFileReader[GenericRecord](in, datumReader)

  override def iterator: Iterator[T] = new Iterator[T] {
    override def hasNext: Boolean = dataFileReader.hasNext
    override def next(): T = fromRecord(dataFileReader.next)
  }

  override def tryIterator: Iterator[Try[T]] = new Iterator[Try[T]] {
    override def hasNext: Boolean = dataFileReader.hasNext
    override def next(): Try[T] = Try(fromRecord(dataFileReader.next))
  }

  override def close(): Unit = in.close()
}

class AvroBinaryInputStream[T](in: InputStream)(implicit schemaFor: SchemaFor[T], fromRecord: FromRecord[T]) extends AvroInputStream[T] {

  var decoder = DecoderFactory.get().binaryDecoder(in, null)
  val datumReader = new GenericDatumReader[GenericRecord](schemaFor())

  def iterator: Iterator[T] = new Iterator[T] {
    var el: GenericRecord = _
    override def hasNext: Boolean = {
      if (el == null) {
        el = datumReader.read(null, decoder)
      }
      el != null
    }
    override def next(): T = fromRecord(el)
  }

  def tryIterator: Iterator[Try[T]] = new Iterator[Try[T]] {
    var el: GenericRecord = _
    override def hasNext: Boolean = {
      if (el == null)
        el = datumReader.read(null, decoder)
      el != null
    }
    override def next(): Try[T] = Try(fromRecord(el))
  }

  override def close(): Unit = in.close()
}

object AvroInputStream {

  def apply[T: SchemaFor : FromRecord](bytes: Array[Byte]): AvroInputStream[T] = apply(bytes, false)
  def apply[T: SchemaFor : FromRecord](bytes: Array[Byte], binary: Boolean): AvroInputStream[T] = {
    if (binary) new AvroBinaryInputStream[T](new ByteArrayInputStream(bytes))
    else new AvroDataInputStream[T](new SeekableByteArrayInput(bytes))
  }

  def apply[T: SchemaFor : FromRecord](path: String): AvroInputStream[T] = apply(path, false)
  def apply[T: SchemaFor : FromRecord](path: String, binary: Boolean): AvroInputStream[T] = apply(Paths.get(path), binary)

  def apply[T: SchemaFor : FromRecord](file: File): AvroInputStream[T] = apply(file, false)
  def apply[T: SchemaFor : FromRecord](file: File, binary: Boolean): AvroInputStream[T] = apply(file.toPath, binary)

  def apply[T: SchemaFor : FromRecord](path: Path): AvroInputStream[T] = apply(path, false)
  def apply[T: SchemaFor : FromRecord](path: Path, binary: Boolean): AvroInputStream[T] = {
    if (binary) new AvroBinaryInputStream[T](Files.newInputStream(path))
    else new AvroDataInputStream[T](new SeekableFileInput(path.toFile))
  }
}
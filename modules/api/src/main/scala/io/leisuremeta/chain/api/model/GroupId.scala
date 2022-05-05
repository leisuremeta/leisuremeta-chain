package io.leisuremeta.chain
package api.model

import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema

import lib.datatype.Utf8

opaque type GroupId = Utf8
object GroupId:
  def apply(utf8: Utf8): GroupId = utf8

  extension (a: GroupId)
    def utf8: Utf8 = a

  given Encoder[GroupId] = Encoder.encodeString.contramap(_.utf8.value)
  given Decoder[GroupId] = Decoder.decodeString.emap(Utf8.from(_).left.map(_.getMessage)).map(apply)
  given Schema[GroupId] = Schema.string

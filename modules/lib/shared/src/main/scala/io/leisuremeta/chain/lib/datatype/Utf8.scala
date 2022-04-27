package io.leisuremeta.chain.lib.datatype

import java.nio.charset.{CharacterCodingException, StandardCharsets}
import scala.util.Try

import io.circe.{Decoder, Encoder, KeyDecoder, KeyEncoder}
import scodec.bits.ByteVector

opaque type Utf8 = String
object Utf8:
  def from(s: String): Either[CharacterCodingException, Utf8] =
    ByteVector.encodeUtf8(s).map(_ => s)

  extension (u: Utf8)
    def value: String = u

  given utf8CirceDecoder: Decoder[Utf8] = Decoder.decodeString.emap(from(_).left.map(_.getMessage))
  given utf8CirceEncoder: Encoder[Utf8] = Encoder.encodeString
  
  given utf8CirceKeyDecoder: KeyDecoder[Utf8] = KeyDecoder.instance(from(_).toOption)
  given utf8CirceKeyEncoder: KeyEncoder[Utf8] = KeyEncoder.encodeKeyString

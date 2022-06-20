package io.leisuremeta.chain
package api.model.offering

import java.util.Locale

import io.circe.{Decoder, Encoder}
import scodec.bits.ByteVector
import scodec.bits.Bases.Alphabets

import lib.codec.byte.{ByteDecoder, ByteEncoder}
import lib.datatype.BigNat

opaque type VrfPublicKey = ByteVector

object VrfPublicKey:
  def apply(bytes: ByteVector): VrfPublicKey = bytes

  extension (key: VrfPublicKey)
    def toBytes: ByteVector = key
    def toHex: String       = key.toHex

  given byteEncoder: ByteEncoder[VrfPublicKey] = (key: VrfPublicKey) =>
    ByteEncoder[BigNat].encode(BigNat.unsafeFromLong(key.size)) ++ key
  given byteDecoder: ByteDecoder[VrfPublicKey] =
    ByteDecoder[BigNat].flatMap{ size =>
      ByteDecoder.fromFixedSizeBytes(size.toBigInt.toInt)(identity)
    }

  given circeEncoder: Encoder[VrfPublicKey] =
    Encoder.encodeString.contramap(_.toHex)
  given circeDecoder: Decoder[VrfPublicKey] =
    Decoder.decodeString
      .emap { (s: String) =>
        ByteVector.fromHexDescriptive(
          s.toLowerCase(Locale.ENGLISH),
          Alphabets.HexLowercase,
        )
      }
      .map(apply)

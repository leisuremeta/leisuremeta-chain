package io.leisuremeta.chain
package api.model

import scodec.bits.ByteVector
import sttp.tapir.{Codec, DecodeResult}
import sttp.tapir.CodecFormat.TextPlain

import lib.crypto.Hash
import lib.datatype.UInt256
import io.circe.{Decoder, Encoder}



final case class Signed[A](sig: AccountSignature, value: A)

object Signed:
  type Tx = Signed[Transaction]

  type TxHash = Hash.Value[Tx]
  object TxHash{
    given txHashCodec: Codec[String, TxHash, TextPlain] = Codec.string.mapDecode{ (s: String) =>
      ByteVector.fromHexDescriptive(s).left.map(new Exception(_)).flatMap(UInt256.from) match
        case Left(e) => DecodeResult.Error(s, e)
        case Right(v) => DecodeResult.Value(Hash.Value(v))
    }(_.toUInt256Bytes.toBytes.toHex)
  }

  given signedHash[A: Hash]: Hash[Signed[A]] = Hash[A].contramap(_.value)

  given txhashDecoder: Decoder[TxHash] = Hash.Value.circeValueDecoder[Tx]
  given txhashEncoder: Encoder[TxHash] = Hash.Value.circeValueEncoder[Tx]

  import io.circe.generic.semiauto.*

  given signedDecoder[A: Decoder]: Decoder[Signed[A]] = deriveDecoder
  given signedEncoder[A: Encoder]: Encoder[Signed[A]] = deriveEncoder

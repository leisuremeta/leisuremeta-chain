package io.leisuremeta.chain
package api.model.account

import cats.syntax.either.*

import io.circe.{Decoder, Encoder, KeyDecoder, KeyEncoder}
import sttp.tapir.{Codec, DecodeResult}//, Schema}
import sttp.tapir.CodecFormat.TextPlain

import lib.codec.byte.{ByteDecoder, ByteEncoder}
import lib.datatype.BigNat
import lib.failure.DecodingFailure
import java.util.Locale

enum ExternalChain(val name: String, val abbr: String):
  case ETH extends ExternalChain("Ethereum", "eth")
  case SOL extends ExternalChain("Solana", "sol")

object ExternalChain :
  def fromAbbr(abbr: String): Option[ExternalChain] =
    abbr.toLowerCase(Locale.US) match
      case "eth" => Some(ETH)
      case "sol" => Some(SOL)
      case _ => None

  given Decoder[ExternalChain] = Decoder.decodeString.emap:
    fromAbbr(_).toRight("Unknown public chain")

  given Encoder[ExternalChain] = Encoder.encodeString.contramap(_.abbr)

  given KeyDecoder[ExternalChain] = KeyDecoder.instance(fromAbbr(_))
  given KeyEncoder[ExternalChain] = KeyEncoder.encodeKeyString.contramap(_.abbr)

  given ByteDecoder[ExternalChain] = BigNat.bignatByteDecoder.emap: (bn: BigNat) =>
    bn.toBigInt.toInt match
      case 0 => ExternalChain.ETH.asRight[DecodingFailure]
      case 1 => ExternalChain.SOL.asRight[DecodingFailure]
      case _ => DecodingFailure("Unknown public chain").asLeft[ExternalChain]
  given ByteEncoder[ExternalChain] = BigNat.bignatByteEncoder.contramap:
    case ExternalChain.ETH => BigNat.Zero
    case ExternalChain.SOL => BigNat.One

  given Codec[String, ExternalChain, TextPlain] = Codec.string
    .mapDecode: abbr =>
      fromAbbr(abbr) match
        case Some(chain) => DecodeResult.Value(chain)
        case None => DecodeResult.Error(abbr, DecodingFailure(s"Invalid public chain: $abbr"))
    .apply(_.abbr)

package io.leisuremeta.chain
package api.model.voting

import scala.util.Try

import cats.Eq
import cats.syntax.either.*
import io.circe.{Decoder, Encoder}

import lib.codec.byte.{ByteDecoder, ByteEncoder}
import lib.datatype.BigNat
import lib.failure.DecodingFailure

enum VoteType(val name: String):
  case ONE_PERSON_ONE_VOTE extends VoteType("ONE_PERSON_ONE_VOTE")
  case TOKEN_WEIGHTED      extends VoteType("TOKEN_WEIGHTED")
  case NFT_BASED           extends VoteType("NFT_BASED")

object VoteType:
  given eq: Eq[VoteType]                = Eq.fromUniversalEquals
  given circeEncoder: Encoder[VoteType] = Encoder.encodeString.contramap(_.name)
  given circeDecoder: Decoder[VoteType] = Decoder.decodeString.emap: str =>
    Try(VoteType.valueOf(str)).toEither.leftMap: err =>
      s"VoteType $str is not valid: ${err.getMessage}"
  given byteEncoder: ByteEncoder[VoteType] =
    BigNat.bignatByteEncoder.contramap: voteType =>
      BigNat.unsafeFromBigInt(BigInt(voteType.ordinal))
  given byteDecoder: ByteDecoder[VoteType] =
    BigNat.bignatByteDecoder.emap: bignat =>
      Try(VoteType.fromOrdinal(bignat.toBigInt.toInt)).toEither.leftMap: err =>
        DecodingFailure:
          s"VoteType $bignat is not valid: ${err.getMessage}"

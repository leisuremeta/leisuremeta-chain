package io.leisuremeta.chain
package api.model

import cats.Eq

import io.circe.{Decoder, Encoder}
import sttp.tapir.*

import lib.codec.byte.{ByteDecoder, ByteEncoder}
import lib.crypto.Hash
import lib.merkle.MerkleTrieNode.MerkleRoot

opaque type StateRoot = Option[MerkleRoot]

object StateRoot:

  def apply(main: Option[MerkleRoot]): StateRoot = main

  val empty: StateRoot = None

  extension (sr: StateRoot) def main: Option[MerkleRoot] = sr

  given byteDecoder: ByteDecoder[StateRoot] =
    ByteDecoder.optionByteDecoder[MerkleRoot]
  given byteEncoder: ByteEncoder[StateRoot] =
    ByteEncoder.optionByteEncoder[MerkleRoot]

  given circeDecoder: Decoder[StateRoot] = 
    Decoder.decodeOption[MerkleRoot]
  given circeEncoder: Encoder[StateRoot] =
    Encoder.encodeOption[MerkleRoot]

  given tapirSchema: Schema[StateRoot] = Schema.string
  given eqStateRoot: Eq[StateRoot]     = Eq.fromUniversalEquals

package io.leisuremeta.chain
package lmscan.agent.model

import api.model.*

import java.time.Instant

import cats.kernel.Eq
import scodec.bits.ByteVector

import lib.codec.byte.{ByteDecoder, ByteEncoder}
import lib.crypto.{CryptoOps, Hash, KeyPair, Recover, Sign, Signature}
import lib.datatype.BigNat
import lib.merkle.GenericMerkleTrieNode

final case class Block0(
    header: Block0.Header,
    transactionHashes: Set[Signed.TxHash],
    votes: Set[Signature],
)

object Block0:

  type BlockHash = Hash.Value[Block0]

  final case class Header(
      number: BigNat,
      parentHash: BlockHash,
      stateRoot: StateRoot0,
      transactionsRoot: Option[
        GenericMerkleTrieNode.MerkleRoot[Signed.TxHash, Unit],
      ],
      timestamp: Instant,
  )

  object Header:
    given eqHeader: Eq[Header] = Eq.fromUniversalEquals

    given headerHash: Hash[Header] = Hash.build

    given encoder: ByteEncoder[Header] with
      def encode(header: Header): ByteVector =
        ByteEncoder[BigNat].encode(header.number)
          ++ ByteEncoder[BlockHash].encode(header.parentHash)
          ++ ByteEncoder[StateRoot0].encode(header.stateRoot)
          ++ ByteEncoder[Option[
            GenericMerkleTrieNode.MerkleRoot[Signed.TxHash, Unit],
          ]].encode(header.transactionsRoot)
          ++ ByteEncoder[Instant].encode(header.timestamp)
    given decoder: ByteDecoder[Header] =
      for
        number     <- ByteDecoder[BigNat]
        parentHash <- ByteDecoder[BlockHash]
        stateRoot  <- ByteDecoder[StateRoot0]
        transactionsRoot <- ByteDecoder[Option[
          GenericMerkleTrieNode.MerkleRoot[Signed.TxHash, Unit],
        ]]
        timestamp <- ByteDecoder[Instant]
      yield Header(number, parentHash, stateRoot, transactionsRoot, timestamp)

  object ops:
    extension (blockHash: Hash.Value[Block0])
      def toHeaderHash: Hash.Value[Header] =
        Hash.Value[Header](blockHash.toUInt256Bytes)

    extension [A](headerHash: Hash.Value[A])
      def toBlockHash: Hash.Value[Block0] =
        Hash.Value[Block0](headerHash.toUInt256Bytes)

  given blockHash: Hash[Block0] = Header.headerHash.contramap(_.header)

  given signBlock: Sign[Block0.Header] = Sign.build

  given recoverBlockHeader: Recover[Block0.Header] = Recover.build


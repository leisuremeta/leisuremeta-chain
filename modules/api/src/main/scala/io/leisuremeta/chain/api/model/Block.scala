package io.leisuremeta.chain
package api.model

import java.time.Instant

import cats.kernel.Eq
import scodec.bits.ByteVector

import lib.codec.byte.{ByteDecoder, ByteEncoder}
import lib.crypto.{CryptoOps, Hash, KeyPair, Recover, Sign, Signature}
import lib.datatype.BigNat
import lib.merkle.MerkleTrieNode

final case class Block(
    header: Block.Header,
    transactionHashes: Set[Signed.TxHash],
    votes: Set[Signature],
)

object Block:

  type BlockHash = Hash.Value[Block]

  final case class Header(
      number: BigNat,
      parentHash: BlockHash,
      stateRoot: StateRoot,
      transactionsRoot: Option[MerkleTrieNode.MerkleRoot[Signed.TxHash, Unit]],
      timestamp: Instant,
  )

  object Header:
    given eqHeader: Eq[Header] = Eq.fromUniversalEquals

    given headerHash: Hash[Header] = Hash.build

    given encoder: ByteEncoder[Header] with
      def encode(header: Header): ByteVector =
        ByteEncoder[BigNat].encode(header.number)
          ++ ByteEncoder[BlockHash].encode(header.parentHash)
          ++ ByteEncoder[StateRoot].encode(header.stateRoot)
          ++ ByteEncoder[Option[MerkleTrieNode.MerkleRoot[Signed.TxHash, Unit]]].encode(header.transactionsRoot)
          ++ ByteEncoder[Instant].encode(header.timestamp)
    given decoder: ByteDecoder[Header] =
      for
        number <- ByteDecoder[BigNat]
        parentHash <- ByteDecoder[BlockHash]
        stateRoot <- ByteDecoder[StateRoot]
        transactionsRoot <- ByteDecoder[Option[MerkleTrieNode.MerkleRoot[Signed.TxHash, Unit]]]
        timestamp <- ByteDecoder[Instant]
      yield Header(number, parentHash, stateRoot, transactionsRoot, timestamp)

  object ops:
    extension (blockHash: Hash.Value[Block])
      def toHeaderHash: Hash.Value[Header] =
        Hash.Value[Header](blockHash.toUInt256Bytes)

    extension (headerHash: Hash.Value[Header])
      def toBlockHash: Hash.Value[Block] =
        Hash.Value[Block](headerHash.toUInt256Bytes)

  given blockHash: Hash[Block] = Header.headerHash.contramap(_.header)

  given signBlock: Sign[Block.Header] = Sign.build

  given recoverBlockHeader: Recover[Block.Header] = Recover.build

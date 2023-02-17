package io.leisuremeta.chain.lmscan.agent
package entity


import java.time.Instant
import model.Block0
// import lib.crypto.Hash.ops.*
// import lib.crypto.Sign.ops.*


final case class BlockEntity(
    hash: String,
    number: Long,
    parentHash: String,
    txCount: Long,
    eventTime: Long,
    createdAt: Long,
)

object BlockEntity:
  def from(block: Block0, blockHash: String): BlockEntity =
    BlockEntity (
      hash = blockHash,
      number = block.header.number.toBigInt.longValue,
      parentHash = block.header.parentHash.toUInt256Bytes.toBytes.toHex,
      txCount = block.transactionHashes.size,
      eventTime = block.header.timestamp.getEpochSecond(),
      createdAt = Instant.now().getEpochSecond(),
    )

/*
for 
  _ <- upsertTransaction[F, BlockEntity](
    quote { query[BlockEntity].insertValue(lift(BlockEntity.from(block, blockHash))).onConflictUpdate(_.hash)((t, e) => t.hash -> e.hash) })
  _ <- updateTransaction[F, BlockStateEntity](quote { query[BlockStateEntity].filter(b => b.hash == lift(blockHash)).update(_.isBuild -> lift(true)) })
yield ()
*/

package io.leisuremeta.chain.lmscan.agent.entity

import io.leisuremeta.chain.api.model.Transaction.TokenTx.MintNFT
import io.leisuremeta.chain.lmscan.agent.model.PBlock
import java.time.Instant
import io.leisuremeta.chain.api.model.Transaction.TokenTx.TransferNFT

final case class Tx(
  hash: String,
  txType: String, // col_name : type
  tokenType: String,
  fromAddr: Option[String],
  toAddr: Seq[String],
  blockHash: String,
  blockNumber: Long,
  eventTime: Long,
  createdAt: Long,
  inputHashs: Option[Seq[String]],
  outputVals: Seq[String],
  json: String,
) 

object Tx:
  def fromNft(txHash: String, nft: MintNFT, block: PBlock, blockHash: String, txJson: String): Tx =
    Tx(
      txHash,
      "Token",
      "NFT",
      None,
      Seq(nft.output.utf8.value),
      blockHash,
      block.header.number,
      nft.createdAt.getEpochSecond(),
      Instant.now().getEpochSecond(),
      None,
      Seq(nft.output.utf8.value+"/"+nft.tokenId.utf8.value),
      txJson,
  )

  def fromNft(txHash: String, nft: TransferNFT, block: PBlock, blockHash: String, txJson: String): Tx =
    Tx(
      txHash,
      "Token",
      "NFT",
      None,
      Seq(nft.output.utf8.value),
      blockHash,
      block.header.number,
      nft.createdAt.getEpochSecond(),
      Instant.now().getEpochSecond,
      Some(Seq(nft.input.toUInt256Bytes.toHex)),
      Seq(nft.output.utf8.value++"/"+nft.tokenId.utf8.value),
      txJson,
    )
  
  
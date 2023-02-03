package io.leisuremeta.chain.lmscan.agent.entity


import io.leisuremeta.chain.lmscan.agent.model.PBlock
import java.time.Instant
import io.leisuremeta.chain.api.model.Transaction.TokenTx.*

final case class Tx(
  hash: String,
  txType: String, // col_name : type
  tokenType: String,
  fromAddr: Option[String],
  toAddr: Option[Seq[String]],
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
      Some(Seq(nft.output.utf8.value)),
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
      Some(Seq(nft.output.utf8.value)),
      blockHash,
      block.header.number,
      nft.createdAt.getEpochSecond(),
      Instant.now().getEpochSecond,
      Some(Seq(nft.input.toUInt256Bytes.toHex)),
      Seq(nft.output.utf8.value++"/"+nft.tokenId.utf8.value),
      txJson,
    )

  def fromNft(txHash: String, nft: BurnNFT, tokenId: String, block: PBlock, blockHash: String, txJson: String): Tx =
    Tx(
      txHash,
      "Token",
      "NFT",
      None,
      None,
      blockHash,
      block.header.number,
      nft.createdAt.getEpochSecond(),
      Instant.now().getEpochSecond,
      Some(Seq(nft.input.toUInt256Bytes.toHex)),
      Seq("/"+tokenId),
      txJson,
    )

  def fromNft(txHash: String, nft: EntrustNFT, block: PBlock, blockHash: String, txJson: String): Tx =
    Tx(
      hash = txHash,
      txType = "Token",
      tokenType = "NFT",
      fromAddr = None,
      toAddr = Some(Seq(nft.to.utf8.value)),
      blockHash = blockHash,
      blockNumber = block.header.number,
      eventTime = nft.createdAt.getEpochSecond(),
      createdAt = Instant.now().getEpochSecond,
      inputHashs = Some(Seq(nft.input.toUInt256Bytes.toHex)),
      outputVals = Seq(nft.to.utf8.value++"/"+nft.tokenId.utf8.value),
      json = txJson,
    )
  
  
  def fromNft(txHash: String, nft: DisposeEntrustedNFT, block: PBlock, blockHash: String, txJson: String): Tx =
    Tx(
      hash = txHash,
      txType = "Token",
      tokenType = "NFT",
      fromAddr = None,
      toAddr = Some(Seq(nft.to.utf8.value)),
      blockHash = blockHash,
      blockNumber = block.header.number,
      eventTime = nft.createdAt.getEpochSecond(),
      createdAt = Instant.now().getEpochSecond,
      inputHashs = Some(Seq(nft.input.toUInt256Bytes.toHex)),
      outputVals = Seq(nft.to.utf8.value++"/"+nft.tokenId.utf8.value),
      json = txJson,
    )
package io.leisuremeta.chain.lmscan.agent.entity


import java.time.Instant
import io.leisuremeta.chain.api.model.Transaction.TokenTx.{TransferNFT, BurnNFT, EntrustNFT, DisposeEntrustedNFT}

final case class NftTxEntity(
    tokenId: String,
    txHash: String,
    // rarity: Option[String],
    // owner: String,
    action: String,
    fromAddr: Option[String],
    toAddr: Option[String],
    eventTime: Long,
    createdAt: Long,
)

object NftTxEntity:
  def from(nft: TransferNFT) =
    NftTxEntity(
      tokenId = nft.tokenId.utf8.value,
      txHash = nft.input.toUInt256Bytes.toBytes.toHex,
      action = "TransferNFT",
      fromAddr = None,
      toAddr = Some(nft.output.utf8.value),
      eventTime = nft.createdAt.getEpochSecond(),
      createdAt = Instant.now().getEpochSecond(),
    )

  def from(nft: BurnNFT, tokenId: String) =
    NftTxEntity(
      tokenId = tokenId,
      txHash = nft.input.toUInt256Bytes.toBytes.toHex,
      action = "BurnNFT",
      fromAddr = None,
      toAddr = None,
      eventTime = nft.createdAt.getEpochSecond(),
      createdAt = Instant.now().getEpochSecond(),
    )

  def from(nft: EntrustNFT) =
    NftTxEntity(
      tokenId = nft.tokenId.utf8.value,
      txHash = nft.input.toUInt256Bytes.toBytes.toHex,
      action = "EntrustNFT",
      fromAddr = None,
      toAddr = Some(nft.to.utf8.value),
      eventTime = nft.createdAt.getEpochSecond(),
      createdAt = Instant.now().getEpochSecond(),
    )

  def from(nft: DisposeEntrustedNFT) =
    val toAccountOpt = nft.output match 
        case Some(value) => Some(value.utf8.value)
        case None =>  None
    NftTxEntity(
      tokenId = nft.tokenId.utf8.value,
      txHash = nft.input.toUInt256Bytes.toBytes.toHex,
      action = "DisposeEntrustedNFT",
      fromAddr = None,
      toAddr = toAccountOpt,
      eventTime = nft.createdAt.getEpochSecond(),
      createdAt = Instant.now().getEpochSecond(),
    )
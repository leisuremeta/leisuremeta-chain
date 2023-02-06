package io.leisuremeta.chain.lmscan.agent.entity


import java.time.Instant
import io.leisuremeta.chain.api.model.Transaction.TokenTx.{TransferNFT, BurnNFT, EntrustNFT, DisposeEntrustedNFT}
import io.leisuremeta.chain.api.model.Transaction.TokenTx.MintNFT

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
  inline def from(nft: MintNFT, txHash: String) =
    NftTxEntity(
      tokenId = nft.tokenId.utf8.value,
      txHash = txHash,
      action = "TransferNFT",
      fromAddr = None,
      toAddr = Some(nft.output.utf8.value),
      eventTime = nft.createdAt.getEpochSecond(),
      createdAt = Instant.now().getEpochSecond(),
    )

  def from(nft: TransferNFT, txHash: String) =
    NftTxEntity(
      tokenId = nft.tokenId.utf8.value,
      txHash = txHash,
      action = "TransferNFT",
      fromAddr = None,
      toAddr = Some(nft.output.utf8.value),
      eventTime = nft.createdAt.getEpochSecond(),
      createdAt = Instant.now().getEpochSecond(),
    )

  def from(nft: BurnNFT, tokenId: String, txHash: String) =
    NftTxEntity(
      tokenId = tokenId,
      txHash = txHash,
      action = "BurnNFT",
      fromAddr = None,
      toAddr = None,
      eventTime = nft.createdAt.getEpochSecond(),
      createdAt = Instant.now().getEpochSecond(),
    )

  def from(nft: EntrustNFT, txHash: String) =
    NftTxEntity(
      tokenId = nft.tokenId.utf8.value,
      txHash = txHash,
      action = "EntrustNFT",
      fromAddr = None,
      toAddr = Some(nft.to.utf8.value),
      eventTime = nft.createdAt.getEpochSecond(),
      createdAt = Instant.now().getEpochSecond(),
    )

  def from(nft: DisposeEntrustedNFT, txHash: String) =
    val toAccountOpt = nft.output match 
        case Some(value) => Some(value.utf8.value)
        case None =>  None
    NftTxEntity(
      tokenId = nft.tokenId.utf8.value,
      txHash = txHash,
      action = "DisposeEntrustedNFT",
      fromAddr = None,
      toAddr = toAccountOpt,
      eventTime = nft.createdAt.getEpochSecond(),
      createdAt = Instant.now().getEpochSecond(),
    )
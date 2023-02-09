package io.leisuremeta.chain.lmscan.agent.entity


import java.time.Instant
import io.leisuremeta.chain.api.model.Transaction.TokenTx.{TransferNFT, BurnNFT, EntrustNFT, DisposeEntrustedNFT}
import io.leisuremeta.chain.api.model.Transaction.TokenTx.MintNFT

final case class NftTxEntity(
    tokenId: String,
    txHash: String,
    action: String,
    fromAddr: String,
    toAddr: Option[String],
    eventTime: Long,
    createdAt: Long,
)

object NftTxEntity:
  inline def from(nft: MintNFT, txHash: String, fromAccount: String) =
    NftTxEntity(
      tokenId = nft.tokenId.utf8.value,
      txHash = txHash,
      action = "MintNFT",
      fromAddr = fromAccount,
      toAddr = Some(nft.output.utf8.value),
      eventTime = nft.createdAt.getEpochSecond(),
      createdAt = Instant.now().getEpochSecond(),
    )

  def from(nft: TransferNFT, txHash: String, fromAccount: String) =
    NftTxEntity(
      tokenId = nft.tokenId.utf8.value,
      txHash = txHash,
      action = "TransferNFT",
      fromAddr = fromAccount,
      toAddr = Some(nft.output.utf8.value),
      eventTime = nft.createdAt.getEpochSecond(),
      createdAt = Instant.now().getEpochSecond(),
    )

  def from(nft: BurnNFT, tokenId: String, txHash: String, fromAccount: String) =
    NftTxEntity(
      tokenId = tokenId,
      txHash = txHash,
      action = "BurnNFT",
      fromAddr = fromAccount,
      toAddr = None,
      eventTime = nft.createdAt.getEpochSecond(),
      createdAt = Instant.now().getEpochSecond(),
    )

  def from(nft: EntrustNFT, txHash: String, fromAccount: String) =
    NftTxEntity(
      tokenId = nft.tokenId.utf8.value,
      txHash = txHash,
      action = "EntrustNFT",
      fromAddr = fromAccount,
      toAddr = Some(nft.to.utf8.value),
      eventTime = nft.createdAt.getEpochSecond(),
      createdAt = Instant.now().getEpochSecond(),
    )

  def from(nft: DisposeEntrustedNFT, txHash: String, fromAccount: String) =
    val toAccountOpt = nft.output match 
      case Some(value) => Some(value.utf8.value)
      case None =>  None
    NftTxEntity(
      tokenId = nft.tokenId.utf8.value,
      txHash = txHash,
      action = "DisposeEntrustedNFT",
      fromAddr = fromAccount,
      toAddr = toAccountOpt,
      eventTime = nft.createdAt.getEpochSecond(),
      createdAt = Instant.now().getEpochSecond(),
    )

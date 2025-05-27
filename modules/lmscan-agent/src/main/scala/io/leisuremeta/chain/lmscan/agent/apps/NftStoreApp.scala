package io.leisuremeta.chain.lmscan.agent
package apps

import cats.effect.*
import cats.implicits.*
import io.leisuremeta.chain.lmscan.agent.service.*
import cats.effect.std.Queue
import io.leisuremeta.chain.api.model.Transaction
import io.leisuremeta.chain.api.model.Transaction.*
import io.leisuremeta.chain.api.model.Transaction.TokenTx.*
import scala.concurrent.duration.DurationInt
import io.leisuremeta.chain.api.model.Account
import io.circe.Decoder
import io.circe.generic.semiauto.*
import cats.data.EitherT
import io.leisuremeta.chain.api.model.Signed.TxHash
import cats.Monad

trait NftApp[F[_]]:
  def run: F[Unit]

object NftApp:
  case class Nft(
      txHash: String,
      tokenId: String,
      action: String,
      fromAddr: String,
      toAddr: String,
      eventTime: Long,
  )
  case class NftFile(
      tokenId: String,
      tokenDefId: String,
      collectionName: String,
      nftName: String,
      nftUri: String,
      creatorDescription: String,
      dataUrl: String,
      rarity: String,
      creator: String,
      eventTime: Long,
  )
  case class NftOwner(
      tokenId: String,
      ownerAddr: String,
      eventTime: Long,
  )
  case class NftMetaInfo(
      Creator_description: String,
      Collection_description: String,
      Rarity: String,
      NFT_checksum: String,
      Collection_name: String,
      Creator: String,
      NFT_name: String,
      NFT_URI: String,
  )
  given Decoder[NftMetaInfo] = deriveDecoder[NftMetaInfo]
  def build[F[_]: Async: Monad](nftQ: Queue[F, (TxHash, TokenTx, Account)])(
      remote: RemoteStoreApp[F],
      client: RequestServiceApp[F],
  ): NftApp[F] = new NftApp[F]:
    def run: F[Unit] =
      for
        q <- nftQ.tryTakeN(Some(1000))
        _ = scribe.info(s"nftQ size: ${q.size}")
        files <- q
          .traverse((_, tx, _) => getNftFileReq(tx).value)
        fs <- remote.nftRepo.putNftFileList(files.mapFilter(_.toOption))
        (nfts, owners) <- Async[F].pure:
          q.mapFilter(parseTxToRaw).separate
        ns <- remote.nftRepo.putNftList(nfts)
        os <- remote.nftRepo.putNftOwnerList(owners)
        _  <- Async[F].sleep(30.seconds)
        r  <- run
      yield r
    def getNftFileReq(tx: TokenTx) = tx match
      case tx: MintNFT =>
        for
          info <- client.getResult[NftMetaInfo](tx.dataUrl.toString)
          res = NftFile(
            tx.tokenId.toString,
            tx.tokenDefinitionId.toString,
            info.Collection_name,
            info.NFT_name,
            info.NFT_URI,
            info.Creator_description,
            tx.dataUrl.toString,
            info.Rarity,
            info.Creator,
            tx.createdAt.getEpochSecond,
          )
        yield res
      case tx: MintNFTWithMemo =>
        for
          info <- client.getResult[NftMetaInfo](tx.dataUrl.toString)
          res = NftFile(
            tx.tokenId.toString,
            tx.tokenDefinitionId.toString,
            info.Collection_name,
            info.NFT_name,
            info.NFT_URI,
            info.Creator_description,
            tx.dataUrl.toString,
            info.Rarity,
            info.Creator,
            tx.createdAt.getEpochSecond,
          )
        yield res
      case _ => EitherT.leftT[F, NftFile]("Not MintNFT")
    def parseTxToRaw(hash: TxHash, tx: TokenTx, from: Account) = tx match
      case tx: MintNFT =>
        Some(
          Nft(
            hash.toUInt256Bytes.toHex,
            tx.tokenId.toString,
            "MintNft",
            from.toString,
            tx.output.toString,
            tx.createdAt.getEpochSecond,
          ),
          NftOwner(
            tx.tokenId.toString,
            tx.output.toString,
            tx.createdAt.getEpochSecond,
          ),
        )
      case tx: MintNFTWithMemo =>
        Some(
          Nft(
            hash.toUInt256Bytes.toHex,
            tx.tokenId.toString,
            "MintNft",
            from.toString,
            tx.output.toString,
            tx.createdAt.getEpochSecond,
          ),
          NftOwner(
            tx.tokenId.toString,
            tx.output.toString,
            tx.createdAt.getEpochSecond,
          ),
        )
      case tx: EntrustNFT =>
        Some(
          Nft(
            hash.toUInt256Bytes.toHex,
            tx.tokenId.toString,
            "EntrustNft",
            tx.input.toUInt256Bytes.toHex,
            tx.to.toString,
            tx.createdAt.getEpochSecond,
          ),
          NftOwner(
            tx.tokenId.toString,
            tx.to.toString,
            tx.createdAt.getEpochSecond,
          ),
        )
      case tx: TransferNFT =>
        Some(
          Nft(
            hash.toUInt256Bytes.toHex,
            tx.tokenId.toString,
            "TransferNft",
            tx.input.toUInt256Bytes.toHex,
            tx.output.toString,
            tx.createdAt.getEpochSecond,
          ),
          NftOwner(
            tx.tokenId.toString,
            tx.output.toString,
            tx.createdAt.getEpochSecond,
          ),
        )
      case tx: DisposeEntrustedNFT =>
        Some(
          Nft(
            hash.toUInt256Bytes.toHex,
            tx.tokenId.toString,
            "DisposeEntrustedNft",
            tx.input.toUInt256Bytes.toHex,
            tx.output.map(_.toString).getOrElse(""),
            tx.createdAt.getEpochSecond,
          ),
          NftOwner(
            tx.tokenId.toString,
            tx.output.map(_.toString).getOrElse(""),
            tx.createdAt.getEpochSecond,
          ),
        )
      case _ => None

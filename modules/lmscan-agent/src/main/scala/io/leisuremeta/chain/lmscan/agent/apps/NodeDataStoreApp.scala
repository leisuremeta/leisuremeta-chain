package io.leisuremeta.chain.lmscan.agent
package apps

import cats.effect.*
import cats.effect.kernel.instances.all.*
import cats.implicits.*
import cats.data.*
import io.leisuremeta.chain.lmscan.agent.service.*
import io.leisuremeta.chain.lmscan.agent.service.RequestServiceApp
import io.leisuremeta.chain.api.model.NodeStatus
import io.leisuremeta.chain.api.model.Block.BlockHash
import io.circe.*, io.circe.generic.semiauto.*
import io.leisuremeta.chain.lmscan.backend.entity.Block
import io.leisuremeta.chain.lib.crypto.Hash
import io.leisuremeta.chain.lib.datatype.UInt256
import scodec.bits.ByteVector
import cats.effect.std.Queue
import io.leisuremeta.chain.api.model.Transaction
import io.leisuremeta.chain.api.model.Transaction.*
import io.leisuremeta.chain.api.model.Transaction.AccountTx.*
import io.leisuremeta.chain.api.model.Transaction.GroupTx.*
import io.leisuremeta.chain.api.model.Transaction.TokenTx.*
import io.leisuremeta.chain.api.model.Transaction.RewardTx.*
import io.leisuremeta.chain.api.model.Transaction.AgendaTx.*
import io.leisuremeta.chain.api.model.Transaction.VotingTx.*
import io.leisuremeta.chain.api.model.Transaction.CreatorDaoTx.*
import io.leisuremeta.chain.api.model.Block as NodeBlock
import io.leisuremeta.chain.api.model.Signed.TxHash
import io.leisuremeta.chain.api.model.TransactionWithResult
import java.time.Instant
import scala.concurrent.duration.DurationInt
import io.leisuremeta.chain.api.model.Account

case class Tx(
    hash: String,
    signer: String,
    txType: Option[String] = None,
    blockHash: String,
    eventTime: Long,
    tokenType: String = "LM",
    blockNumber: Long,
    subType: Option[String],
)

trait DataStoreApp[F[_]]:
  def run: F[Unit]

object DataStoreApp:
  given Decoder[NodeStatus]            = deriveDecoder[NodeStatus]
  given Decoder[NodeBlock]             = deriveDecoder[NodeBlock]
  given Decoder[TransactionWithResult] = deriveDecoder[TransactionWithResult]
  extension (b: Block)
    def toHash =
      Hash.Value[NodeBlock](
        UInt256
          .from(ByteVector.fromHex(b.hash).get)
          .getOrElse(UInt256.EmptyBytes),
      )
    def getParent =
      Hash.Value[NodeBlock](
        UInt256
          .from(ByteVector.fromHex(b.parentHash).get)
          .getOrElse(UInt256.EmptyBytes),
      )
  def checkHashRange(
      from: BlockHash,
      to: BlockHash,
  ): Either[String, (String, BlockHash)] =
    if from != to then Right(from.toUInt256Bytes.toHex, to)
    else Left("Store block is latest")
  def build[F[_]: Async](
      nftQ: Queue[F, (TxHash, TokenTx, Account)],
      balQ: Queue[F, (TxHash, TransactionWithResult)],
  )(
      db: RemoteStoreApp[F],
      client: RequestServiceApp[F],
      base: String,
  ): DataStoreApp[F] =
    new DataStoreApp[F]:
      def run: F[Unit] =
        toGen &> toBest
      def toGen: F[Unit] =
        for
          lowest <- db.blcRepo.getLowestBlock
          x = lowest
            .leftMap(_.getMessage)
            .flatMap:
              case Some(b) =>
                Right(b)
              case None =>
                Left("block not found")
          sEither <- client.getResult[NodeStatus](s"$base/status").value
          _ <- (x, sEither) match
            case (Right(blc), Right(status)) =>
              storeBlockLoop(blc.getParent, status.genesisHash)
            case (Left(s), Right(status)) =>
              storeBlockLoop(status.bestHash, status.genesisHash)
            case _ =>
              Async[F].unit
        yield ()
      def toBest: F[Unit] =
        for
          latest <- db.blcRepo.getLatestBlock
          x = latest
            .leftMap(_.getMessage)
            .flatMap:
              case Some(b) =>
                Right(b)
              case None =>
                Left("block not found")
          sEither <- client.getResult[NodeStatus](s"$base/status").value
          _ <- (x, sEither) match
            case (Right(blc), Right(status)) =>
              scribe.info(
                s"storeHashRange: ${status.bestHash.toUInt256Bytes.toHex}, ${blc.hash}",
              )
              storeBlockLoop(status.bestHash, blc.toHash)
            case _ =>
              Async[F].unit
          _ <- Async[F].sleep(10.seconds)
          r <- toBest
        yield r
      def storeBlockLoop(from: BlockHash, to: BlockHash): F[Unit] =
        for
          next <- Async[F].delay:
            checkHashRange(from, to)
          res <- next match
            case Right((f, _)) =>
              for
                blc <- client.getResult[NodeBlock](s"$base/block/$f").value
                r <- blc match
                  case Right(b) =>
                    for
                      _ <- db.blcRepo.putBlock(
                        f,
                        b.header.number.toBigInt.toLong,
                        b.header.parentHash.toUInt256Bytes.toHex,
                        b.transactionHashes.size,
                        b.header.timestamp.getEpochSecond,
                      )
                      _ <- storeTxLoop(f, b, b.transactionHashes.toList)
                      r <- storeBlockLoop(b.header.parentHash, to)
                    yield r
                  case Left(_) =>
                    Async[F].delay:
                      scribe.error(s"block not found: $f")
              yield r
            case Left(_) =>
              Async[F].unit
        yield res
      def storeTxLoop(
          bHash: String,
          blc: NodeBlock,
          txs: List[TxHash],
      ): F[Unit] =
        txs match
          case Nil => Async[F].unit
          case x :: xs =>
            for
              resE <- client
                .getResultWithJsonString[TransactionWithResult](
                  s"$base/tx/${x.toUInt256Bytes.toHex}",
                )
                .value
              res <- resE match
                case Right((tx, json)) =>
                  val hash                 = x.toUInt256Bytes.toHex
                  val (txEntity, accounts) = parseTxr(hash, bHash, tx, blc)
                  for
                    _ <- db.txRepo.putTx(txEntity)
                    _ <- db.txRepo.putTxState(
                      hash,
                      blc.header.parentHash.toUInt256Bytes.toHex,
                      tx,
                      json,
                    )
                    _ <- db.accRepo.putAccountMapper(hash, accounts)
                    _ <- addNftQueue(x, tx)
                    _ <- addBalanceQueue(x, tx)
                    r <- storeTxLoop(bHash, blc, xs)
                  yield r
                case Left(_) =>
                  Async[F].delay:
                    scribe.error(s"tx not found: ${x.toUInt256Bytes.toHex}")
            yield res
      def parseTxr(
          hash: String,
          bHash: String,
          txr: TransactionWithResult,
          blc: NodeBlock,
      ) =
        val t =
          Tx(
            hash,
            txr.signedTx.sig.account.toString,
            None,
            bHash,
            txr.signedTx.value.createdAt.getEpochSecond,
            "LM",
            blc.header.number.toBigInt.toLong,
            None,
          )
        txr.signedTx.value match
          case tx: AccountTx =>
            parseAccountTx(t, tx)
          case tx: GroupTx =>
            parseGroupTx(t, tx, txr.signedTx.sig.account)
          case tT: TokenTx =>
            parseTokenTx(t, tT, txr.signedTx.sig.account)
          case tx: RewardTx =>
            parseRewardTx(t, tx, txr.signedTx.sig.account)
          case tx: AgendaTx =>
            parseAgendaTx(t, tx, txr.signedTx.sig.account)
          case tx: VotingTx =>
            parseVotingTx(t, tx, txr.signedTx.sig.account)
          case tx: CreatorDaoTx =>
            parseCreatorDaoTx(t, tx, txr.signedTx.sig.account)
      def parseAccountTx(c: Tx, a: AccountTx) =
        val t = c.copy(txType = Some("Account"))
        a match
          case tx: CreateAccount =>
            (
              t.copy(
                subType = Some("CreateAccount"),
              ),
              Set(tx.account),
            )
          case tx: CreateAccountWithExternalChainAddresses =>
            (
              t.copy(
                subType = Some("CreateAccountWithExternalChainAddresses"),
              ),
              Set.empty,
            )
          case tx: UpdateAccount =>
            (
              t.copy(
                subType = Some("UpdateAccount"),
              ),
              Set.empty,
            )
          case tx: UpdateAccountWithExternalChainAddresses =>
            (
              t.copy(
                subType = Some("UpdateAccountWithExternalChainAddresses"),
              ),
              Set.empty,
            )
          case tx: AddPublicKeySummaries =>
            (
              t.copy(
                subType = Some("AddPublicKeySummaries"),
              ),
              Set.empty,
            )
      def parseGroupTx(c: Tx, g: GroupTx, signer: Account) =
        val t = c.copy(txType = Some("Group"))
        g match
          case tx: CreateGroup =>
            (
              t.copy(
                subType = Some("CreateGroup"),
              ),
              Set(tx.coordinator),
            )
          case tx: AddAccounts =>
            (
              t.copy(
                subType = Some("AddAccounts"),
              ),
              tx.accounts + signer,
            )
      def parseTokenTx(c: Tx, tt: TokenTx, signer: Account) =
        val t = c.copy(txType = Some("Token"))
        tt match
          case tx: DefineToken =>
            (
              t.copy(
                tokenType = tx.definitionId.toString,
                subType = Some("DefineToken"),
              ),
              Set(signer),
            )
          case tx: DefineTokenWithPrecision =>
            (
              t.copy(
                tokenType = tx.definitionId.toString,
                subType = Some("DefineToken"),
              ),
              Set(signer),
            )
          case tx: MintFungibleToken =>
            (
              t.copy(
                subType = Some("MintFungibleToken"),
              ),
              tx.outputs.keySet + signer,
            )
          case _: MintNFT =>
            (
              t.copy(
                subType = Some("MintNFT"),
              ),
              Set(signer),
            )
          case _: MintNFTWithMemo =>
            (
              t.copy(
                subType = Some("MintNFT"),
              ),
              Set(signer),
            )
          case _: BurnFungibleToken =>
            (
              t.copy(
                subType = Some("BurnFungibleToken"),
              ),
              Set(signer),
            )
          case _: BurnNFT =>
            (
              t.copy(
                subType = Some("BurnNFT"),
              ),
              Set(signer),
            )
          case _: UpdateNFT =>
            (
              t.copy(
                subType = Some("UpdateNFT"),
              ),
              Set(signer),
            )
          case tx: TransferFungibleToken =>
            (
              t.copy(
                subType = Some("TransferFungibleToken"),
              ),
              tx.outputs.keySet + signer,
            )
          case tx: TransferNFT =>
            (
              t.copy(
                subType = Some("TransferNFT"),
              ),
              Set(signer, tx.output),
            )
          case tx: EntrustFungibleToken =>
            (
              t.copy(
                subType = Some("EntrustFungibleToken"),
              ),
              Set(signer, tx.to),
            )
          case tx: EntrustNFT =>
            (
              t.copy(
                subType = Some("EntrustNFT"),
              ),
              Set(signer, tx.to),
            )
          case tx: DisposeEntrustedFungibleToken =>
            (
              t.copy(
                subType = Some("DisposeEntrustedFungibleToken"),
              ),
              tx.outputs.keySet + signer,
            )
          case tx: DisposeEntrustedNFT =>
            (
              t.copy(
                subType = Some("DisposeEntrustedNFT"),
              ),
              Set(signer, tx.output.getOrElse(signer)),
            )
          case _: CreateSnapshots =>
            (
              t.copy(
                subType = Some("CreateSnapshots"),
              ),
              Set(signer),
            )
      def parseRewardTx(c: Tx, r: RewardTx, signer: Account) =
        val t = c.copy(txType = Some("Reward"))
        r match
          case tx: RegisterDao =>
            (
              t.copy(
                subType = Some("RegisterDao"),
              ),
              tx.moderators + tx.daoAccountName + signer,
            )
          case tx: UpdateDao =>
            (
              t.copy(
                subType = Some("UpdateDao"),
              ),
              tx.moderators + signer,
            )
          case tx: RecordActivity =>
            (
              t.copy(
                subType = Some("RecordActivity"),
              ),
              tx.userActivity.keySet + signer,
            )
          case tx: OfferReward =>
            (
              t.copy(
                subType = Some("OfferReward"),
              ),
              tx.outputs.keySet + signer,
            )
          case tx: BuildSnapshot =>
            (
              t.copy(
                subType = Some("BuildSnapshot"),
              ),
              Set(signer),
            )
          case tx: ExecuteReward =>
            (
              t.copy(
                subType = Some("ExecuteReward"),
              ),
              Set(signer, tx.daoAccount.getOrElse(signer)),
            )
          case tx: ExecuteOwnershipReward =>
            (
              t.copy(
                subType = Some("ExecuteOwnershipReward"),
              ),
              Set(signer),
            )
      def parseAgendaTx(c: Tx, a: AgendaTx, signer: Account) =
        val t = c.copy(txType = Some("Agenda"))
        a match
          case _: SuggestSimpleAgenda =>
            (
              t.copy(
                subType = Some("SuggestSimpleAgenda"),
              ),
              Set(signer),
            )
          case _: VoteSimpleAgenda =>
            (
              t.copy(
                subType = Some("VoteSimpleAgenda"),
              ),
              Set(signer),
            )
      def parseVotingTx(c: Tx, v: VotingTx, signer: Account) =
        val t = c.copy(txType = Some("Voting"))
        v match
          case tx: CreateVoteProposal =>
            (
              t.copy(
                subType = Some("CreateVoteProposal"),
              ),
              Set(signer),
            )
          case tx: CastVote =>
            (
              t.copy(
                subType = Some("CastVote"),
              ),
              Set(signer),
            )
          case tx: TallyVotes =>
            (
              t.copy(
                subType = Some("TallyVotes"),
              ),
              Set(signer),
            )
      def parseCreatorDaoTx(c: Tx, cd: CreatorDaoTx, signer: Account) =
        val t = c.copy(txType = Some("CreatorDao"))
        cd match
          case tx: CreateCreatorDao =>
            (
              t.copy(
                subType = Some("CreateCreatorDao"),
              ),
              Set(signer),
            )
          case tx: UpdateCreatorDao =>
            (
              t.copy(
                subType = Some("UpdateCreatorDao"),
              ),
              Set(signer),
            )
          case tx: DisbandCreatorDao =>
            (
              t.copy(
                subType = Some("DisbandCreatorDao"),
              ),
              Set(signer),
            )
          case tx: ReplaceCoordinator =>
            (
              t.copy(
                subType = Some("ReplaceCoordinator"),
              ),
              Set(signer, tx.newCoordinator),
            )
          case tx: AddMembers =>
            (
              t.copy(
                subType = Some("AddMembers"),
              ),
              tx.members + signer,
            )
          case tx: RemoveMembers =>
            (
              t.copy(
                subType = Some("RemoveMembers"),
              ),
              tx.members + signer,
            )
          case tx: PromoteModerators =>
            (
              t.copy(
                subType = Some("PromoteModerators"),
              ),
              tx.members + signer,
            )
          case tx: DemoteModerators =>
            (
              t.copy(
                subType = Some("DemoteModerators"),
              ),
              tx.members + signer,
            )

      def addNftQueue(hash: TxHash, tx: TransactionWithResult): F[Unit] =
        tx.signedTx.value match
          case v: MintNFT =>
            nftQ.offer((hash, v, tx.signedTx.sig.account))
          case v: MintNFTWithMemo =>
            nftQ.offer((hash, v, tx.signedTx.sig.account))
          case v: TransferNFT =>
            nftQ.offer((hash, v, tx.signedTx.sig.account))
          case v: EntrustNFT =>
            nftQ.offer((hash, v, tx.signedTx.sig.account))
          case v: DisposeEntrustedNFT =>
            nftQ.offer((hash, v, tx.signedTx.sig.account))
          case _ => Async[F].unit

      def addBalanceQueue(hash: TxHash, tx: TransactionWithResult): F[Unit] =
        tx.signedTx.value match
          case _: MintFungibleToken             => balQ.offer(hash -> tx)
          case _: BurnFungibleToken             => balQ.offer(hash -> tx)
          case _: TransferFungibleToken         => balQ.offer(hash -> tx)
          case _: EntrustFungibleToken          => balQ.offer(hash -> tx)
          case _: DisposeEntrustedFungibleToken => balQ.offer(hash -> tx)
          case _: OfferReward                   => balQ.offer(hash -> tx)
          case _                                => Async[F].unit

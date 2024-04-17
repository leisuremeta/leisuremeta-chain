package io.leisuremeta.chain
package node
package service

import cats.data.EitherT
import cats.effect.Concurrent
import cats.syntax.eq.given
import cats.syntax.either.*
import cats.syntax.functor.*
import cats.syntax.flatMap.*
import cats.syntax.traverse.*

import fs2.Stream
import scodec.bits.ByteVector

import api.{LeisureMetaChainApi as Api}
import api.model.{
  Account,
  AccountData,
  GroupId,
  GroupData,
  PublicKeySummary,
  Transaction,
  TransactionWithResult,
}
import api.model.account.EthAddress
import api.model.api_model.{
  AccountInfo,
  ActivityInfo,
  BalanceInfo,
  GroupInfo,
  NftBalanceInfo,
}
import api.model.reward.{
  ActivitySnapshot,
  OwnershipSnapshot,
  OwnershipRewardLog,
}
import api.model.token.{
  NftState,
  TokenDefinition,
  TokenDefinitionId,
  TokenId,
}
import dapp.PlayNommState
import lib.codec.byte.ByteEncoder.ops.*
import lib.crypto.Hash
import lib.datatype.BigNat
import lib.merkle.MerkleTrieState
import repository.{BlockRepository, TransactionRepository}

object StateReadService:
  def getAccountInfo[F[_]: Concurrent: BlockRepository: PlayNommState](
      account: Account,
  ): F[Option[AccountInfo]] = for
    bestHeaderEither <- BlockRepository[F].bestHeader.value
    bestHeader <- bestHeaderEither match
      case Left(err) => Concurrent[F].raiseError(err)
      case Right(None) =>
        Concurrent[F].raiseError(new Exception("No best header"))
      case Right(Some(bestHeader)) => Concurrent[F].pure(bestHeader)
    merkleState = MerkleTrieState.fromRootOption(bestHeader.stateRoot.main)
    accountStateEither <- PlayNommState[F].account.name
      .get(account)
      .runA(merkleState)
      .value
    accountStateOption <- accountStateEither match
      case Left(err) => Concurrent[F].raiseError(new Exception(err))
      case Right(accountStateOption) => Concurrent[F].pure(accountStateOption)
    keyListEither <- PlayNommState[F].account.key
      .from(account.toBytes)
      .runA(merkleState)
      .flatMap(_.compile.toList)
      .map(_.map { case ((_, pks), info) => (pks, info) })
      .value
    keyList <- keyListEither match
      case Left(err)      => Concurrent[F].raiseError(new Exception(err))
      case Right(keyList) => Concurrent[F].pure(keyList)
  yield accountStateOption.map(accountData =>
    AccountInfo(
      accountData.ethAddress,
      accountData.guardian,
      keyList.toMap,
    ),
  )

  def getEthAccount[F[_]: Concurrent: BlockRepository: PlayNommState](
      ethAddress: EthAddress,
  ): F[Option[Account]] = for
    bestHeaderEither <- BlockRepository[F].bestHeader.value
    bestHeader <- bestHeaderEither match
      case Left(err) => Concurrent[F].raiseError(err)
      case Right(None) =>
        Concurrent[F].raiseError(new Exception("No best header"))
      case Right(Some(bestHeader)) => Concurrent[F].pure(bestHeader)
    merkleState = MerkleTrieState.fromRootOption(bestHeader.stateRoot.main)
    ethStateEither <- PlayNommState[F].account.eth
      .get(ethAddress)
      .runA(merkleState)
      .value
    ethStateOption <- ethStateEither match
      case Left(err)             => Concurrent[F].raiseError(new Exception(err))
      case Right(ethStateOption) => Concurrent[F].pure(ethStateOption)
  yield ethStateOption

  def getGroupInfo[F[_]: Concurrent: BlockRepository: PlayNommState](
      groupId: GroupId,
  ): F[Option[GroupInfo]] = for
    bestHeaderEither <- BlockRepository[F].bestHeader.value
    bestHeader <- bestHeaderEither match
      case Left(err) => Concurrent[F].raiseError(err)
      case Right(None) =>
        Concurrent[F].raiseError(new Exception("No best header"))
      case Right(Some(bestHeader)) => Concurrent[F].pure(bestHeader)
    merkleState = MerkleTrieState.fromRootOption(bestHeader.stateRoot.main)
    groupDataEither <- PlayNommState[F].group.group
      .get(groupId)
      .runA(merkleState)
      .value
    groupDataOption <- groupDataEither match
      case Left(err) => Concurrent[F].raiseError(new Exception(err))
      case Right(groupDataOption) => Concurrent[F].pure(groupDataOption)
    accountListEither <- PlayNommState[F].group.groupAccount
      .from(groupId.toBytes)
      .runA(merkleState)
      .flatMap(_.compile.toList)
      .map(_.map(_._1._2))
      .value
    accountList <- accountListEither match
      case Left(err)          => Concurrent[F].raiseError(new Exception(err))
      case Right(accountList) => Concurrent[F].pure(accountList)
  yield groupDataOption.map: groupData =>
    GroupInfo(groupData, accountList.toSet)

  def getTokenDef[F[_]: Concurrent: BlockRepository: PlayNommState](
      tokenDefinitionId: TokenDefinitionId,
  ): F[Option[TokenDefinition]] = for
    bestHeaderEither <- BlockRepository[F].bestHeader.value
    bestHeader <- bestHeaderEither match
      case Left(err) => Concurrent[F].raiseError(err)
      case Right(None) =>
        Concurrent[F].raiseError(new Exception("No best header"))
      case Right(Some(bestHeader)) => Concurrent[F].pure(bestHeader)
    merkleState = MerkleTrieState.fromRootOption(bestHeader.stateRoot.main)
    tokenDefEither <- PlayNommState[F].token.definition
      .get(tokenDefinitionId)
      .runA(merkleState)
      .value
    tokenDefOption <- tokenDefEither match
      case Left(err)             => Concurrent[F].raiseError(new Exception(err))
      case Right(tokenDefOption) => Concurrent[F].pure(tokenDefOption)
  yield tokenDefOption

  def getBalance[F[_]
    : Concurrent: BlockRepository: TransactionRepository: PlayNommState](
      account: Account,
      movable: Api.Movable,
  ): F[Map[TokenDefinitionId, BalanceInfo]] = movable match
    case Api.Movable.Free   => getFreeBalance[F](account)
    case Api.Movable.Locked => getEntrustBalance[F](account)

  def getFreeBalance[F[_]
    : Concurrent: BlockRepository: TransactionRepository: PlayNommState](
      account: Account,
  ): F[Map[TokenDefinitionId, BalanceInfo]] =
    for
      bestHeaderEither <- BlockRepository[F].bestHeader.value
      bestHeader <- bestHeaderEither match
        case Left(err) => Concurrent[F].raiseError(err)
        case Right(None) =>
          Concurrent[F].raiseError(new Exception("No best header"))
        case Right(Some(bestHeader)) => Concurrent[F].pure(bestHeader)
      merkleState = MerkleTrieState.fromRootOption(bestHeader.stateRoot.main)
      balanceListEither <- PlayNommState[F].token.fungibleBalance
        .from(account.toBytes)
        .runA(merkleState)
        .flatMap: stream =>
          stream
            .map:
              case ((account, defId, txHash), _) => (defId, txHash)
            .compile
            .toList
        .value
      balanceList <- balanceListEither match
        case Left(err)          => Concurrent[F].raiseError(new Exception(err))
        case Right(balanceList) => Concurrent[F].pure(balanceList)
      balanceTxEither <- balanceList
        .traverse: (defId, txHash) =>
          TransactionRepository[F]
            .get(txHash)
            .map: txWithResultOption =>
              txWithResultOption.map(txWithResult =>
                (defId, txHash, txWithResult),
              )
        .value
      balanceTxList <- balanceTxEither match
        case Left(err) => Concurrent[F].raiseError(new Exception(err.msg))
        case Right(balanceTxList) => Concurrent[F].pure(balanceTxList.flatten)
    yield balanceTxList
      .groupMapReduce(_._1): (_, txHash, txWithResult) =>
        txWithResult.signedTx.value match
          case fb: Transaction.FungibleBalance =>
            fb match
              case mf: Transaction.TokenTx.MintFungibleToken =>
                BalanceInfo(
                  totalAmount = mf.outputs.get(account).getOrElse(BigNat.Zero),
                  unused = Map(txHash -> txWithResult),
                )
              case tf: Transaction.TokenTx.TransferFungibleToken =>
                BalanceInfo(
                  totalAmount = tf.outputs.get(account).getOrElse(BigNat.Zero),
                  unused = Map(txHash -> txWithResult),
                )
              case bf: Transaction.TokenTx.BurnFungibleToken =>
                val amount = txWithResult.result match
                  case Some(
                        Transaction.TokenTx.BurnFungibleTokenResult(
                          outputAmount,
                        ),
                      ) =>
                    outputAmount
                  case _ => BigNat.Zero
                BalanceInfo(
                  totalAmount = amount,
                  unused = Map(txHash -> txWithResult),
                )
              case ef: Transaction.TokenTx.EntrustFungibleToken =>
                val amount = txWithResult.result.fold(BigNat.Zero) {
                  case Transaction.TokenTx.EntrustFungibleTokenResult(
                        remainder,
                      ) =>
                    remainder
                  case _ => BigNat.Zero
                }
                BalanceInfo(
                  totalAmount = amount,
                  unused = Map(txHash -> txWithResult),
                )
              case de: Transaction.TokenTx.DisposeEntrustedFungibleToken =>
                val amount = de.outputs.get(account).getOrElse(BigNat.Zero)
                BalanceInfo(
                  totalAmount = amount,
                  unused = Map(txHash -> txWithResult),
                )
              case or: Transaction.RewardTx.OfferReward =>
                BalanceInfo(
                  totalAmount = or.outputs.get(account).getOrElse(BigNat.Zero),
                  unused = Map(txHash -> txWithResult),
                )
              case xr: Transaction.RewardTx.ExecuteReward =>
                val amount = txWithResult.result.fold(BigNat.Zero) {
                  case Transaction.RewardTx.ExecuteRewardResult(outputs) =>
                    outputs.get(account).getOrElse(BigNat.Zero)
                  case _ => BigNat.Zero
                }
                BalanceInfo(
                  totalAmount = amount,
                  unused = Map(txHash -> txWithResult),
                )
              case xo: Transaction.RewardTx.ExecuteOwnershipReward =>
                val amount = txWithResult.result.fold(BigNat.Zero) {
                  case Transaction.RewardTx.ExecuteOwnershipRewardResult(
                        outputs,
                      ) =>
                    outputs.get(account).getOrElse(BigNat.Zero)
                  case _ => BigNat.Zero
                }
                BalanceInfo(
                  totalAmount = amount,
                  unused = Map(txHash -> txWithResult),
                )

          case _ => BalanceInfo(totalAmount = BigNat.Zero, unused = Map.empty)
      .apply: (a, b) =>
        BalanceInfo(
          totalAmount = BigNat.add(a.totalAmount, b.totalAmount),
          unused = a.unused ++ b.unused,
        )

  def getEntrustBalance[F[_]
    : Concurrent: BlockRepository: TransactionRepository: PlayNommState](
      account: Account,
  ): F[Map[TokenDefinitionId, BalanceInfo]] =
    for
      bestHeaderEither <- BlockRepository[F].bestHeader.value
      bestHeader <- bestHeaderEither match
        case Left(err) => Concurrent[F].raiseError(err)
        case Right(None) =>
          Concurrent[F].raiseError(new Exception("No best header"))
        case Right(Some(bestHeader)) => Concurrent[F].pure(bestHeader)
      merkleState = MerkleTrieState.fromRootOption(bestHeader.stateRoot.main)
      balanceListEither <- PlayNommState[F].token.entrustFungibleBalance
        .from(account.toBytes)
        .runA(merkleState)
        .flatMap: stream =>
          stream
            .map:
              case ((account, toAccount, defId, txHash), _) => (defId, txHash)
            .compile
            .toList
        .value
      balanceList <- balanceListEither match
        case Left(err)          => Concurrent[F].raiseError(new Exception(err))
        case Right(balanceList) => Concurrent[F].pure(balanceList)
      balanceTxEither <- balanceList
        .traverse: (defId, txHash) =>
          TransactionRepository[F]
            .get(txHash)
            .map: txWithResultOption =>
              txWithResultOption.map(txWithResult =>
                (defId, txHash, txWithResult),
              )
        .value
      balanceTxList <- balanceTxEither match
        case Left(err) => Concurrent[F].raiseError(new Exception(err.msg))
        case Right(balanceTxList) => Concurrent[F].pure(balanceTxList.flatten)
    yield balanceTxList
      .groupMapReduce(_._1): (_, txHash, txWithResult) =>
        txWithResult.signedTx.value match
          case ef: Transaction.TokenTx.EntrustFungibleToken =>
            BalanceInfo(
              totalAmount = ef.amount,
              unused = Map(txHash -> txWithResult),
            )
          case _ => BalanceInfo(totalAmount = BigNat.Zero, unused = Map.empty)
      .apply: (a, b) =>
        BalanceInfo(
          totalAmount = BigNat.add(a.totalAmount, b.totalAmount),
          unused = a.unused ++ b.unused,
        )

  def getNftBalance[F[_]
    : Concurrent: BlockRepository: TransactionRepository: PlayNommState](
      account: Account,
      movableOption: Option[Api.Movable],
  ): F[Map[TokenId, NftBalanceInfo]] = movableOption match
    case None                     => getAllNftBalance[F](account)
    case Some(Api.Movable.Free)   => getFreeNftBalance[F](account)
    case Some(Api.Movable.Locked) => getEntrustedNftBalance[F](account)

  def getAllNftBalance[F[_]
    : Concurrent: BlockRepository: TransactionRepository: PlayNommState](
      account: Account,
  ): F[Map[TokenId, NftBalanceInfo]] = for
    free      <- getFreeNftBalance[F](account)
    entrusted <- getEntrustedNftBalance[F](account)
  yield free ++ entrusted

  def getFreeNftBalance[F[_]
    : Concurrent: BlockRepository: TransactionRepository: PlayNommState](
      account: Account,
  ): F[Map[TokenId, NftBalanceInfo]] = for
    bestHeaderEither <- BlockRepository[F].bestHeader.value
    bestHeader <- bestHeaderEither match
      case Left(err) => Concurrent[F].raiseError(err)
      case Right(None) =>
        Concurrent[F].raiseError(new Exception("No best header"))
      case Right(Some(bestHeader)) => Concurrent[F].pure(bestHeader)
    merkleState = MerkleTrieState.fromRootOption(bestHeader.stateRoot.main)
    nftBalanceMap <- getNftBalanceFromNftBalanceState[F](
      account,
      merkleState,
    )
  yield nftBalanceMap

  def getEntrustedNftBalance[F[_]
    : Concurrent: BlockRepository: TransactionRepository: PlayNommState](
      account: Account,
  ): F[Map[TokenId, NftBalanceInfo]] = for
    bestHeaderEither <- BlockRepository[F].bestHeader.value
    bestHeader <- bestHeaderEither match
      case Left(err) => Concurrent[F].raiseError(err)
      case Right(None) =>
        Concurrent[F].raiseError(new Exception("No best header"))
      case Right(Some(bestHeader)) => Concurrent[F].pure(bestHeader)
    merkleState = MerkleTrieState.fromRootOption(bestHeader.stateRoot.main)
    nftBalanceMap <- getEntrustedNftBalanceFromEntrustedNftBalanceState[F](
      account,
      merkleState,
    )
  yield nftBalanceMap

  def getNftBalanceFromNftBalanceState[F[_]
    : Concurrent: BlockRepository: TransactionRepository: PlayNommState](
      account: Account,
      mts: MerkleTrieState,
  ): F[Map[TokenId, NftBalanceInfo]] = for
    balanceListEither <- PlayNommState[F].token.nftBalance
      .from(account.toBytes)
      .runA(mts)
      .flatMap: stream =>
        stream
          .map:
            case ((account, tokenId, txHash), _) => (tokenId, txHash)
          .compile
          .toList
      .value
    balanceList <- balanceListEither match
      case Left(err)          => Concurrent[F].raiseError(new Exception(err))
      case Right(balanceList) => Concurrent[F].pure(balanceList)
    balanceTxEither <- balanceList
      .traverse: (tokenId, txHash) =>
        TransactionRepository[F]
          .get(txHash)
          .map: txWithResultOption =>
            txWithResultOption.map: txWithResult =>
              txWithResult.signedTx.value match
                case nb: Transaction.NftBalance =>
                  nb match
                    case mf: Transaction.TokenTx.MintNFT =>
                      Map:
                        tokenId -> NftBalanceInfo(
                          mf.tokenDefinitionId,
                          txHash,
                          txWithResult,
                          None,
                        )
                    case mfm: Transaction.TokenTx.MintNFTWithMemo =>
                      Map:
                        tokenId -> NftBalanceInfo(
                          mfm.tokenDefinitionId,
                          txHash,
                          txWithResult,
                          mfm.memo,
                        )
                    case tn: Transaction.TokenTx.TransferNFT =>
                      Map:
                        tokenId -> NftBalanceInfo(
                          tn.definitionId,
                          txHash,
                          txWithResult,
                          tn.memo,
                        )
                    case de: Transaction.TokenTx.DisposeEntrustedNFT =>
                      Map:
                        tokenId -> NftBalanceInfo(
                          de.definitionId,
                          txHash,
                          txWithResult,
                          None,
                        )
                case _ =>
                  Map.empty
      .value
    balanceTxList <- balanceTxEither match
      case Left(err) => Concurrent[F].raiseError(new Exception(err.msg))
      case Right(balanceTxList) => Concurrent[F].pure(balanceTxList.flatten)
  yield balanceTxList.foldLeft(Map.empty)(_ ++ _)

  def getEntrustedNftBalanceFromEntrustedNftBalanceState[F[_]
    : Concurrent: BlockRepository: TransactionRepository: PlayNommState](
      account: Account,
      mts: MerkleTrieState,
  ): F[Map[TokenId, NftBalanceInfo]] = for
    balanceListEither <- PlayNommState[F].token.entrustNftBalance
      .from(account.toBytes)
      .runA(mts)
      .flatMap: stream =>
        stream
          .map:
            case ((account, toAccount, tokenId, txHash), _) => (tokenId, txHash)
          .compile
          .toList
      .value
    balanceList <- balanceListEither match
      case Left(err)          => Concurrent[F].raiseError(new Exception(err))
      case Right(balanceList) => Concurrent[F].pure(balanceList)
    balanceTxEither <- balanceList
      .traverse: (tokenId, txHash) =>
        TransactionRepository[F]
          .get(txHash)
          .map: txWithResultOption =>
            txWithResultOption.map: txWithResult =>
              txWithResult.signedTx.value match
                case en: Transaction.TokenTx.EntrustNFT =>
                  Map:
                    tokenId -> NftBalanceInfo(
                      en.definitionId,
                      txHash,
                      txWithResult,
                      None,
                    )
                case _ =>
                  Map.empty
      .value
    balanceTxList <- balanceTxEither match
      case Left(err) => Concurrent[F].raiseError(new Exception(err.msg))
      case Right(balanceTxList) => Concurrent[F].pure(balanceTxList.flatten)
  yield balanceTxList.foldLeft(Map.empty)(_ ++ _)

  def getToken[F[_]: Concurrent: BlockRepository: PlayNommState](
      tokenId: TokenId,
  ): EitherT[F, String, Option[NftState]] = for
    bestHeaderOption <- BlockRepository[F].bestHeader.leftMap(_.msg)
    bestHeader <- EitherT.fromOption[F](bestHeaderOption, "No best header")
    merkleState = MerkleTrieState.fromRootOption(bestHeader.stateRoot.main)
    nftStateOption <- PlayNommState[F].token.nftState
      .get(tokenId)
      .runA(merkleState)
  yield nftStateOption

  def getTokenHistory[F[_]: Concurrent: BlockRepository: PlayNommState](
      txHash: Hash.Value[TransactionWithResult],
  ): EitherT[F, String, Option[NftState]] = for
    bestHeaderOption <- BlockRepository[F].bestHeader.leftMap(_.msg)
    bestHeader <- EitherT.fromOption[F](bestHeaderOption, "No best header")
    merkleState = MerkleTrieState.fromRootOption(bestHeader.stateRoot.main)
    nftStateOption <- PlayNommState[F].token.nftHistory
      .get(txHash)
      .runA(merkleState)
  yield nftStateOption

  def getOwners[F[_]: Concurrent: BlockRepository: PlayNommState](
      tokenDefinitionId: TokenDefinitionId,
  ): EitherT[F, String, Map[TokenId, Account]] = for
    bestHeaderOption <- BlockRepository[F].bestHeader.leftMap(_.msg)
    bestHeader <- EitherT.fromOption[F](bestHeaderOption, "No best header")
    merkleState = MerkleTrieState.fromRootOption(bestHeader.stateRoot.main)
    tokenIdList <- PlayNommState[F].token.rarityState
      .from(tokenDefinitionId.toBytes)
      .runA(merkleState)
      .flatMap(stream => stream.map(_._1._3).compile.toList)
    ownerOptionList <- tokenIdList.traverse: (tokenId: TokenId) =>
      PlayNommState[F].token.nftState
        .get(tokenId)
        .runA(merkleState)
        .map: nftStateOption =>
          nftStateOption.map(state => (tokenId, state.currentOwner))
  yield ownerOptionList.flatten.toMap

  def getAccountActivity[F[_]: Concurrent: BlockRepository: PlayNommState](
      account: Account,
  ): EitherT[F, Either[String, String], Seq[ActivityInfo]] =

    val program = PlayNommState[F].reward.accountActivity
      .from(account.toBytes)
      .map: stream =>
        stream
          .takeWhile(_._1._1 === account)
          .flatMap:
            case ((account, instant), logs) =>
              Stream.emits:
                logs.map: log =>
                  ActivityInfo(
                    timestamp = instant,
                    point = log.point,
                    description = log.description,
                    txHash = log.txHash,
                  )
          .compile
          .toList

    for
      bestHeaderOption <- BlockRepository[F].bestHeader.leftMap: e =>
        Left(e.msg)
      bestHeader <- EitherT
        .fromOption[F](bestHeaderOption, Left("No best header"))
      merkleState = MerkleTrieState.fromRootOption(bestHeader.stateRoot.main)
      infosEitherT <- program.runA(merkleState).leftMap(_.asLeft[String])
      infos        <- infosEitherT.leftMap(_.asLeft[String])
    yield infos.toSeq

  def getTokenActivity[F[_]: Concurrent: BlockRepository: PlayNommState](
      tokenId: TokenId,
  ): EitherT[F, Either[String, String], Seq[ActivityInfo]] =

    val program = PlayNommState[F].reward.tokenReceived
      .from(tokenId.toBytes)
      .map: stream =>
        stream
          .takeWhile(_._1._1 === tokenId)
          .flatMap:
            case ((tokenId, instant), logs) =>
              Stream.emits:
                logs.map: log =>
                  ActivityInfo(
                    timestamp = instant,
                    point = log.point,
                    description = log.description,
                    txHash = log.txHash,
                  )
          .compile
          .toList

    for
      bestHeaderOption <- BlockRepository[F].bestHeader.leftMap: e =>
        Left(e.msg)
      bestHeader <- EitherT
        .fromOption[F](bestHeaderOption, Left("No best header"))
      merkleState = MerkleTrieState.fromRootOption(bestHeader.stateRoot.main)
      infosEitherT <- program.runA(merkleState).leftMap(_.asLeft[String])
      infos        <- infosEitherT.leftMap(_.asLeft[String])
    yield infos.toSeq

  def getAccountSnapshot[F[_]: Concurrent: BlockRepository: PlayNommState](
      account: Account,
  ): EitherT[F, Either[String, String], Option[ActivitySnapshot]] =

    val program = PlayNommState[F].reward.accountSnapshot.get(account)

    for
      bestHeaderOption <- BlockRepository[F].bestHeader.leftMap: e =>
        Left(e.msg)
      bestHeader <- EitherT
        .fromOption[F](bestHeaderOption, Left("No best header"))
      merkleState = MerkleTrieState.fromRootOption(bestHeader.stateRoot.main)
      snapshotOption <- program.runA(merkleState).leftMap(_.asLeft[String])
    yield snapshotOption

  def getTokenSnapshot[F[_]: Concurrent: BlockRepository: PlayNommState](
      tokenId: TokenId,
  ): EitherT[F, Either[String, String], Option[ActivitySnapshot]] =

    val program = PlayNommState[F].reward.tokenSnapshot.get(tokenId)

    for
      bestHeaderOption <- BlockRepository[F].bestHeader.leftMap: e =>
        Left(e.msg)
      bestHeader <- EitherT
        .fromOption[F](bestHeaderOption, Left("No best header"))
      merkleState = MerkleTrieState.fromRootOption(bestHeader.stateRoot.main)
      snapshotOption <- program.runA(merkleState).leftMap(_.asLeft[String])
    yield snapshotOption

  def getOwnershipSnapshot[F[_]: Concurrent: BlockRepository: PlayNommState](
      tokenId: TokenId,
  ): EitherT[F, Either[String, String], Option[OwnershipSnapshot]] =

    val program = PlayNommState[F].reward.ownershipSnapshot.get(tokenId)

    for
      bestHeaderOption <- BlockRepository[F].bestHeader.leftMap: e =>
        Left(e.msg)
      bestHeader <- EitherT
        .fromOption[F](bestHeaderOption, Left("No best header"))
      merkleState = MerkleTrieState.fromRootOption(bestHeader.stateRoot.main)
      snapshotOption <- program.runA(merkleState).leftMap(_.asLeft[String])
    yield snapshotOption

  def getOwnershipSnapshotMap[F[_]: Concurrent: BlockRepository: PlayNommState](
      from: Option[TokenId],
      limit: Int,
  ): EitherT[F, Either[String, String], Map[TokenId, OwnershipSnapshot]] =

    val program = PlayNommState[F].reward.ownershipSnapshot
      .from(from.fold(ByteVector.empty)(_.toBytes))

    for
      bestHeaderOption <- BlockRepository[F].bestHeader.leftMap: e =>
        Left(e.msg)
      bestHeader <- EitherT
        .fromOption[F](bestHeaderOption, Left("No best header"))
      merkleState = MerkleTrieState.fromRootOption(bestHeader.stateRoot.main)
      snapshotStream <- program.runA(merkleState).leftMap(_.asLeft[String])
      snapshots <- snapshotStream
        .take(limit)
        .compile
        .toList
        .leftMap(_.asLeft[String])
    yield snapshots.toMap

  def getOwnershipRewarded[F[_]: Concurrent: BlockRepository: PlayNommState](
      tokenId: TokenId,
  ): EitherT[F, Either[String, String], Option[OwnershipRewardLog]] =

    val program = PlayNommState[F].reward.ownershipRewarded.get(tokenId)

    for
      bestHeaderOption <- BlockRepository[F].bestHeader.leftMap: e =>
        Left(e.msg)
      bestHeader <- EitherT
        .fromOption[F](bestHeaderOption, Left("No best header"))
      merkleState = MerkleTrieState.fromRootOption(bestHeader.stateRoot.main)
      logOption <- program.runA(merkleState).leftMap(_.asLeft[String])
    yield logOption

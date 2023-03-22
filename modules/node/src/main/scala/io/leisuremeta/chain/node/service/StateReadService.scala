package io.leisuremeta.chain
package node
package service

import cats.data.EitherT
import cats.effect.Concurrent
import cats.syntax.bifunctor.*
import cats.syntax.eq.given
import cats.syntax.either.*
import cats.syntax.functor.*
import cats.syntax.flatMap.*
import cats.syntax.traverse.*

import fs2.Stream

import GossipDomain.MerkleState
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
import api.model.TransactionWithResult.ops.*
import api.model.account.EthAddress
import api.model.api_model.{
  AccountInfo,
  ActivityInfo,
  BalanceInfo,
  GroupInfo,
  NftBalanceInfo,
}
import api.model.reward.{ActivitySnapshot, OwnershipSnapshot}
import api.model.token.{
  Rarity,
  NftState,
  TokenDefinition,
  TokenDefinitionId,
  TokenDetail,
  TokenId,
}
import dapp.PlayNommState
import lib.codec.byte.{ByteDecoder, DecodeResult}
import lib.codec.byte.ByteEncoder.ops.*
import lib.crypto.Hash
import lib.datatype.BigNat
import lib.merkle.{GenericMerkleTrie, GenericMerkleTrieState}
import repository.{
  BlockRepository,
  GenericStateRepository,
  TransactionRepository,
}
import GenericStateRepository.given
import io.leisuremeta.chain.api.model.Transaction.TokenTx.BurnFungibleTokenResult

import io.leisuremeta.chain.node.dapp.PlayNommState
import scodec.bits.ByteVector
object StateReadService:
  def getAccountInfo[F[_]
    : Concurrent: BlockRepository: GenericStateRepository.AccountState](
      account: Account,
  ): F[Option[AccountInfo]] = for
    bestHeaderEither <- BlockRepository[F].bestHeader.value
    bestHeader <- bestHeaderEither match
      case Left(err) => Concurrent[F].raiseError(err)
      case Right(None) =>
        Concurrent[F].raiseError(new Exception("No best header"))
      case Right(Some(bestHeader)) => Concurrent[F].pure(bestHeader)
    merkleState = MerkleState.from(bestHeader)
    accountStateEither <- GenericMerkleTrie
      .get[F, Account, AccountData](account.toBytes.bits)
      .runA(merkleState.account.namesState)
      .value
    accountStateOption <- accountStateEither match
      case Left(err) => Concurrent[F].raiseError(new Exception(err))
      case Right(accountStateOption) => Concurrent[F].pure(accountStateOption)
    keyListEither <- GenericMerkleTrie
      .from[F, (Account, PublicKeySummary), PublicKeySummary.Info](
        account.toBytes.bits,
      )
      .runA(merkleState.account.keyState)
      .flatMap(
        _.takeWhile(_._1.startsWith(account.toBytes.bits)).compile.toList
          .flatMap { (list) =>
            list.traverse { case (bits, v) =>
              EitherT.fromEither[F] {
                ByteDecoder[(Account, PublicKeySummary)]
                  .decode(bits.bytes) match
                  case Left(err) => Left(err.msg)
                  case Right(
                        DecodeResult((account, publicKeySummary), remainder),
                      ) =>
                    if remainder.isEmpty then Right((publicKeySummary, v))
                    else
                      Left(s"non-empty remainder in decoding $publicKeySummary")
              }
            }
          },
      )
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

  def getEthAccount[F[_]
    : Concurrent: BlockRepository: GenericStateRepository.AccountState](
      ethAddress: EthAddress,
  ): F[Option[Account]] = for
    bestHeaderEither <- BlockRepository[F].bestHeader.value
    bestHeader <- bestHeaderEither match
      case Left(err) => Concurrent[F].raiseError(err)
      case Right(None) =>
        Concurrent[F].raiseError(new Exception("No best header"))
      case Right(Some(bestHeader)) => Concurrent[F].pure(bestHeader)
    merkleState = MerkleState.from(bestHeader)
    ethStateEither <- GenericMerkleTrie
      .get[F, EthAddress, Account](ethAddress.toBytes.bits)
      .runA(merkleState.account.ethState)
      .value
    ethStateOption <- ethStateEither match
      case Left(err)             => Concurrent[F].raiseError(new Exception(err))
      case Right(ethStateOption) => Concurrent[F].pure(ethStateOption)
  yield ethStateOption

  def getGroupInfo[F[_]
    : Concurrent: BlockRepository: GenericStateRepository.GroupState](
      groupId: GroupId,
  ): F[Option[GroupInfo]] = for
    bestHeaderEither <- BlockRepository[F].bestHeader.value
    bestHeader <- bestHeaderEither match
      case Left(err) => Concurrent[F].raiseError(err)
      case Right(None) =>
        Concurrent[F].raiseError(new Exception("No best header"))
      case Right(Some(bestHeader)) => Concurrent[F].pure(bestHeader)
    merkleState = MerkleState.from(bestHeader)
    groupDataEither <- GenericMerkleTrie
      .get[F, GroupId, GroupData](groupId.toBytes.bits)
      .runA(merkleState.group.groupState)
      .value
    groupDataOption <- groupDataEither match
      case Left(err) => Concurrent[F].raiseError(new Exception(err))
      case Right(groupDataOption) => Concurrent[F].pure(groupDataOption)
    accountListEither <- GenericMerkleTrie
      .from[F, (GroupId, Account), Unit](
        groupId.toBytes.bits,
      )
      .runA(merkleState.group.groupAccountState)
      .flatMap(
        _.takeWhile(_._1.startsWith(groupId.toBytes.bits)).compile.toList
          .flatMap { (list) =>
            list.traverse { case (bits, _) =>
              EitherT.fromEither[F] {
                ByteDecoder[(GroupId, Account)]
                  .decode(bits.bytes) match
                  case Left(err) => Left(err.msg)
                  case Right(
                        DecodeResult((groupId, account), remainder),
                      ) =>
                    if remainder.isEmpty then Right(account)
                    else Left(s"non-empty remainder in decoding $account")
              }
            }
          },
      )
      .value
    accountList <- accountListEither match
      case Left(err)          => Concurrent[F].raiseError(new Exception(err))
      case Right(accountList) => Concurrent[F].pure(accountList)
  yield groupDataOption.map(groupData =>
    GroupInfo(groupData, accountList.toSet),
  )

  def getTokenDef[F[_]
    : Concurrent: BlockRepository: GenericStateRepository.TokenState](
      tokenDefinitionId: TokenDefinitionId,
  ): F[Option[TokenDefinition]] = for
    bestHeaderEither <- BlockRepository[F].bestHeader.value
    bestHeader <- bestHeaderEither match
      case Left(err) => Concurrent[F].raiseError(err)
      case Right(None) =>
        Concurrent[F].raiseError(new Exception("No best header"))
      case Right(Some(bestHeader)) => Concurrent[F].pure(bestHeader)
    merkleState = MerkleState.from(bestHeader)
    tokenDefEither <- GenericMerkleTrie
      .get[F, TokenDefinitionId, TokenDefinition](
        tokenDefinitionId.toBytes.bits,
      )
      .runA(merkleState.token.tokenDefinitionState)
      .value
    tokenDefOption <- tokenDefEither match
      case Left(err)             => Concurrent[F].raiseError(new Exception(err))
      case Right(tokenDefOption) => Concurrent[F].pure(tokenDefOption)
  yield tokenDefOption

  def getBalance[F[_]
    : Concurrent: BlockRepository: TransactionRepository: GenericStateRepository.TokenState](
      account: Account,
      movable: Api.Movable,
  ): F[Map[TokenDefinitionId, BalanceInfo]] = movable match
    case Api.Movable.Free   => getFreeBalance[F](account)
    case Api.Movable.Locked => getEntrustBalance[F](account)

  def getFreeBalance[F[_]
    : Concurrent: BlockRepository: TransactionRepository: GenericStateRepository.TokenState](
      account: Account,
  ): F[Map[TokenDefinitionId, BalanceInfo]] =
    for
      bestHeaderEither <- BlockRepository[F].bestHeader.value
      bestHeader <- bestHeaderEither match
        case Left(err) => Concurrent[F].raiseError(err)
        case Right(None) =>
          Concurrent[F].raiseError(new Exception("No best header"))
        case Right(Some(bestHeader)) => Concurrent[F].pure(bestHeader)
      merkleState = MerkleState.from(bestHeader)
      balanceListEither <- GenericMerkleTrie
        .from[
          F,
          (Account, TokenDefinitionId, Hash.Value[TransactionWithResult]),
          Unit,
        ](
          account.toBytes.bits,
        )
        .runA(merkleState.token.fungibleBalanceState)
        .flatMap(
          _.takeWhile(_._1.startsWith(account.toBytes.bits)).compile.toList
            .flatMap { (list) =>
              list.traverse { case (bits, _) =>
                EitherT.fromEither[F] {
                  ByteDecoder[
                    (
                        Account,
                        TokenDefinitionId,
                        Hash.Value[TransactionWithResult],
                    ),
                  ]
                    .decode(bits.bytes) match
                    case Left(err) => Left(err.msg)
                    case Right(
                          DecodeResult(
                            (account, tokenDefinitionId, txHash),
                            remainder,
                          ),
                        ) =>
                      if remainder.isEmpty then
                        Right((tokenDefinitionId, txHash))
                      else Left(s"non-empty remainder in decoding $account")
                }
              }
            },
        )
        .value
      balanceList <- balanceListEither match
        case Left(err)          => Concurrent[F].raiseError(new Exception(err))
        case Right(balanceList) => Concurrent[F].pure(balanceList)
      balanceTxEither <- balanceList.traverse { (defId, txHash) =>
        TransactionRepository[F].get(txHash).map { txWithResultOption =>
          txWithResultOption.map(txWithResult => (defId, txHash, txWithResult))
        }
      }.value
      balanceTxList <- balanceTxEither match
        case Left(err) => Concurrent[F].raiseError(new Exception(err.msg))
        case Right(balanceTxList) => Concurrent[F].pure(balanceTxList.flatten)
    yield balanceTxList.groupMapReduce(_._1) { (defId, txHash, txWithResult) =>
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
                case Some(BurnFungibleTokenResult(outputAmount)) => outputAmount
                case _                                           => BigNat.Zero
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
    }((a, b) =>
      BalanceInfo(
        totalAmount = BigNat.add(a.totalAmount, b.totalAmount),
        unused = a.unused ++ b.unused,
      ),
    )

  def getEntrustBalance[F[_]
    : Concurrent: BlockRepository: TransactionRepository: GenericStateRepository.TokenState](
      account: Account,
  ): F[Map[TokenDefinitionId, BalanceInfo]] =
    for
      bestHeaderEither <- BlockRepository[F].bestHeader.value
      bestHeader <- bestHeaderEither match
        case Left(err) => Concurrent[F].raiseError(err)
        case Right(None) =>
          Concurrent[F].raiseError(new Exception("No best header"))
        case Right(Some(bestHeader)) => Concurrent[F].pure(bestHeader)
      merkleState = MerkleState.from(bestHeader)
      balanceListEither <- GenericMerkleTrie
        .from[
          F,
          (
              Account,
              Account,
              TokenDefinitionId,
              Hash.Value[TransactionWithResult],
          ),
          Unit,
        ](
          account.toBytes.bits,
        )
        .runA(merkleState.token.entrustFungibleBalanceState)
        .flatMap(
          _.takeWhile(_._1.startsWith(account.toBytes.bits)).compile.toList
            .flatMap { (list) =>
              list.traverse { case (bits, _) =>
                EitherT.fromEither[F] {
                  ByteDecoder[
                    (
                        Account,
                        Account,
                        TokenDefinitionId,
                        Hash.Value[TransactionWithResult],
                    ),
                  ]
                    .decode(bits.bytes) match
                    case Left(err) => Left(err.msg)
                    case Right(
                          DecodeResult(
                            (account, toAccount, tokenDefinitionId, txHash),
                            remainder,
                          ),
                        ) =>
                      if remainder.isEmpty then
                        Right((tokenDefinitionId, txHash))
                      else Left(s"non-empty remainder in decoding $account")
                }
              }
            },
        )
        .value
      balanceList <- balanceListEither match
        case Left(err)          => Concurrent[F].raiseError(new Exception(err))
        case Right(balanceList) => Concurrent[F].pure(balanceList)
      balanceTxEither <- balanceList.traverse { (defId, txHash) =>
        TransactionRepository[F].get(txHash).map { txWithResultOption =>
          txWithResultOption.map(txWithResult => (defId, txHash, txWithResult))
        }
      }.value
      balanceTxList <- balanceTxEither match
        case Left(err) => Concurrent[F].raiseError(new Exception(err.msg))
        case Right(balanceTxList) => Concurrent[F].pure(balanceTxList.flatten)
    yield balanceTxList.groupMapReduce(_._1) { (defId, txHash, txWithResult) =>
      txWithResult.signedTx.value match
        case ef: Transaction.TokenTx.EntrustFungibleToken =>
          BalanceInfo(
            totalAmount = ef.amount,
            unused = Map(txHash -> txWithResult),
          )
        case _ => BalanceInfo(totalAmount = BigNat.Zero, unused = Map.empty)
    }((a, b) =>
      BalanceInfo(
        totalAmount = BigNat.add(a.totalAmount, b.totalAmount),
        unused = a.unused ++ b.unused,
      ),
    )

  def getNftBalance[F[_]
    : Concurrent: BlockRepository: TransactionRepository: GenericStateRepository.TokenState](
      account: Account,
      movableOption: Option[Api.Movable],
  ): F[Map[TokenId, NftBalanceInfo]] = movableOption match
    case None                     => getAllNftBalance[F](account)
    case Some(Api.Movable.Free)   => getFreeNftBalance[F](account)
    case Some(Api.Movable.Locked) => getEntrustedNftBalance[F](account)

  def getAllNftBalance[F[_]
    : Concurrent: BlockRepository: TransactionRepository: GenericStateRepository.TokenState](
      account: Account,
  ): F[Map[TokenId, NftBalanceInfo]] = for
    free      <- getFreeNftBalance[F](account)
    entrusted <- getEntrustedNftBalance[F](account)
  yield free ++ entrusted

  def getFreeNftBalance[F[_]
    : Concurrent: BlockRepository: TransactionRepository: GenericStateRepository.TokenState](
      account: Account,
  ): F[Map[TokenId, NftBalanceInfo]] = for
    bestHeaderEither <- BlockRepository[F].bestHeader.value
    bestHeader <- bestHeaderEither match
      case Left(err) => Concurrent[F].raiseError(err)
      case Right(None) =>
        Concurrent[F].raiseError(new Exception("No best header"))
      case Right(Some(bestHeader)) => Concurrent[F].pure(bestHeader)
    merkleState = MerkleState.from(bestHeader)
    nftBalanceMap <- getNftBalanceFromNftBalanceState[F](
      account,
      merkleState.token.nftBalanceState,
    )
  yield nftBalanceMap

  def getEntrustedNftBalance[F[_]
    : Concurrent: BlockRepository: TransactionRepository: GenericStateRepository.TokenState](
      account: Account,
  ): F[Map[TokenId, NftBalanceInfo]] = for
    bestHeaderEither <- BlockRepository[F].bestHeader.value
    bestHeader <- bestHeaderEither match
      case Left(err) => Concurrent[F].raiseError(err)
      case Right(None) =>
        Concurrent[F].raiseError(new Exception("No best header"))
      case Right(Some(bestHeader)) => Concurrent[F].pure(bestHeader)
    merkleState = MerkleState.from(bestHeader)
    nftBalanceMap <- getEntrustedNftBalanceFromEntrustedNftBalanceState[F](
      account,
      merkleState.token.entrustNftBalanceState,
    )
  yield nftBalanceMap

  def getNftBalanceFromNftBalanceState[F[_]
    : Concurrent: BlockRepository: TransactionRepository: GenericStateRepository.TokenState](
      account: Account,
      nftBalanceState: GenericMerkleTrieState[
        (Account, TokenId, Hash.Value[TransactionWithResult]),
        Unit,
      ],
  ): F[Map[TokenId, NftBalanceInfo]] = for
    balanceListEither <- GenericMerkleTrie
      .from[
        F,
        (Account, TokenId, Hash.Value[TransactionWithResult]),
        Unit,
      ](
        account.toBytes.bits,
      )
      .runA(nftBalanceState)
      .flatMap(
        _.takeWhile(_._1.startsWith(account.toBytes.bits)).compile.toList
          .flatMap { (list) =>
            list.traverse { case (bits, _) =>
              EitherT.fromEither[F] {
                ByteDecoder[
                  (
                      Account,
                      TokenId,
                      Hash.Value[TransactionWithResult],
                  ),
                ]
                  .decode(bits.bytes) match
                  case Left(err) => Left(err.msg)
                  case Right(
                        DecodeResult(
                          (account, tokenId, txHash),
                          remainder,
                        ),
                      ) =>
                    if remainder.isEmpty then Right((tokenId, txHash))
                    else Left(s"non-empty remainder in decoding $account")
              }
            }
          },
      )
      .value
    balanceList <- balanceListEither match
      case Left(err)          => Concurrent[F].raiseError(new Exception(err))
      case Right(balanceList) => Concurrent[F].pure(balanceList)
    balanceTxEither <- balanceList.traverse { (tokenId, txHash) =>
      TransactionRepository[F].get(txHash).map { txWithResultOption =>
        txWithResultOption.map(txWithResult =>
          txWithResult.signedTx.value match
            case nb: Transaction.NftBalance =>
              nb match
                case mf: Transaction.TokenTx.MintNFT =>
                  Map(
                    tokenId -> NftBalanceInfo(
                      mf.tokenDefinitionId,
                      txHash,
                      txWithResult,
                    ),
                  )
                case tn: Transaction.TokenTx.TransferNFT =>
                  Map(
                    tokenId -> NftBalanceInfo(
                      tn.definitionId,
                      txHash,
                      txWithResult,
                    ),
                  )
                case de: Transaction.TokenTx.DisposeEntrustedNFT =>
                  Map(
                    tokenId -> NftBalanceInfo(
                      de.definitionId,
                      txHash,
                      txWithResult,
                    ),
                  )
            case _ =>
              Map.empty,
        )
      }
    }.value
    balanceTxList <- balanceTxEither match
      case Left(err) => Concurrent[F].raiseError(new Exception(err.msg))
      case Right(balanceTxList) => Concurrent[F].pure(balanceTxList.flatten)
  yield balanceTxList.foldLeft(Map.empty)(_ ++ _)

  def getEntrustedNftBalanceFromEntrustedNftBalanceState[F[_]
    : Concurrent: BlockRepository: TransactionRepository: GenericStateRepository.TokenState](
      account: Account,
      entrustedNftBalanceState: GenericMerkleTrieState[
        (Account, Account, TokenId, Hash.Value[TransactionWithResult]),
        Unit,
      ],
  ): F[Map[TokenId, NftBalanceInfo]] = for
    balanceListEither <- GenericMerkleTrie
      .from[
        F,
        (Account, Account, TokenId, Hash.Value[TransactionWithResult]),
        Unit,
      ](
        account.toBytes.bits,
      )
      .runA(entrustedNftBalanceState)
      .flatMap(
        _.takeWhile(_._1.startsWith(account.toBytes.bits)).compile.toList
          .flatMap { (list) =>
            list.traverse { case (bits, _) =>
              EitherT.fromEither[F] {
                ByteDecoder[
                  (
                      Account,
                      Account,
                      TokenId,
                      Hash.Value[TransactionWithResult],
                  ),
                ]
                  .decode(bits.bytes) match
                  case Left(err) => Left(err.msg)
                  case Right(
                        DecodeResult(
                          (accountFrom, accountTo, tokenId, txHash),
                          remainder,
                        ),
                      ) =>
                    if remainder.isEmpty then Right((tokenId, txHash))
                    else Left(s"non-empty remainder in decoding $account")
              }
            }
          },
      )
      .value
    balanceList <- balanceListEither match
      case Left(err)          => Concurrent[F].raiseError(new Exception(err))
      case Right(balanceList) => Concurrent[F].pure(balanceList)
    balanceTxEither <- balanceList.traverse { (tokenId, txHash) =>
      TransactionRepository[F].get(txHash).map { txWithResultOption =>
        txWithResultOption.map(txWithResult =>
          txWithResult.signedTx.value match
            case en: Transaction.TokenTx.EntrustNFT =>
              Map(
                tokenId -> NftBalanceInfo(
                  en.definitionId,
                  txHash,
                  txWithResult,
                ),
              )
            case _ =>
              Map.empty,
        )
      }
    }.value
    balanceTxList <- balanceTxEither match
      case Left(err) => Concurrent[F].raiseError(new Exception(err.msg))
      case Right(balanceTxList) => Concurrent[F].pure(balanceTxList.flatten)
  yield balanceTxList.foldLeft(Map.empty)(_ ++ _)

  def getToken[F[_]
    : Concurrent: BlockRepository: GenericStateRepository.TokenState](
      tokenId: TokenId,
  ): EitherT[F, String, Option[NftState]] = for
    bestHeaderOption <- BlockRepository[F].bestHeader.leftMap(_.msg)
    bestHeader <- EitherT.fromOption[F](bestHeaderOption, "No best header")
    merkleState = MerkleState.from(bestHeader)
    nftStateOption <- GenericMerkleTrie
      .get[F, TokenId, NftState](tokenId.toBytes.bits)
      .runA(merkleState.token.nftState)
  yield nftStateOption

  def getOwners[F[_]
    : Concurrent: BlockRepository: GenericStateRepository.TokenState](
      tokenDefinitionId: TokenDefinitionId,
  ): EitherT[F, String, Map[TokenId, Account]] = for
    bestHeaderOption <- BlockRepository[F].bestHeader.leftMap(_.msg)
    bestHeader <- EitherT.fromOption[F](bestHeaderOption, "No best header")
    merkleState = MerkleState.from(bestHeader)
    tokenIdList <- GenericMerkleTrie
      .from[F, (TokenDefinitionId, Rarity, TokenId), Unit](
        tokenDefinitionId.toBytes.bits,
      )
      .runA(merkleState.token.rarityState)
      .flatMap(
        _.takeWhile(
          _._1.startsWith(tokenDefinitionId.toBytes.bits),
        ).compile.toList
          .flatMap { (list) =>
            list.traverse { case (bits, _) =>
              EitherT.fromEither[F] {
                ByteDecoder[(TokenDefinitionId, Rarity, TokenId)]
                  .decode(bits.bytes) match
                  case Left(err) => Left(err.msg)
                  case Right(DecodeResult((_, _, tokenId), remainder)) =>
                    if remainder.isEmpty then Right(tokenId)
                    else
                      Left(
                        s"non-empty remainder in decoding rarity state of $tokenDefinitionId",
                      )
              }
            }
          },
      )
    ownerOptionList <- tokenIdList.traverse { (tokenId: TokenId) =>
      GenericMerkleTrie
        .get[F, TokenId, NftState](tokenId.toBytes.bits)
        .runA(merkleState.token.nftState)
        .map(nftStateOption =>
          nftStateOption.map(state => (tokenId, state.currentOwner)),
        )
    }
  yield ownerOptionList.flatten.toMap

  def getAccountActivity[F[_]: Concurrent: BlockRepository: PlayNommState](
      account: Account,
  ): EitherT[F, Either[String, String], Seq[ActivityInfo]] =

    val program = PlayNommState[F].reward.accountActivity
      .from(account.toBytes)
      .map { stream =>
        stream
          .takeWhile(_._1._1 === account)
          .flatMap { case ((account, instant), logs) =>
            Stream.emits(logs.map { log =>
              ActivityInfo(
                timestamp = instant,
                point = log.point,
                description = log.description,
                txHash = log.txHash,
              )
            })
          }
          .compile
          .toList
      }

    for
      bestHeaderOption <- BlockRepository[F].bestHeader.leftMap { e =>
        Left(e.msg)
      }
      bestHeader <- EitherT
        .fromOption[F](bestHeaderOption, Left("No best header"))
      merkleState = MerkleState.from(bestHeader)
      infosEitherT <- program.runA(merkleState.main).leftMap(_.asLeft[String])
      infos        <- infosEitherT.leftMap(_.asLeft[String])
    yield infos.toSeq

  def getTokenActivity[F[_]: Concurrent: BlockRepository: PlayNommState](
      tokenId: TokenId,
  ): EitherT[F, Either[String, String], Seq[ActivityInfo]] =

    val program = PlayNommState[F].reward.tokenReceived
      .from(tokenId.toBytes)
      .map { stream =>
        stream
          .takeWhile(_._1._1 === tokenId)
          .flatMap { case ((tokenId, instant), logs) =>
            Stream.emits(logs.map { log =>
              ActivityInfo(
                timestamp = instant,
                point = log.point,
                description = log.description,
                txHash = log.txHash,
              )
            })
          }
          .compile
          .toList
      }

    for
      bestHeaderOption <- BlockRepository[F].bestHeader.leftMap { e =>
        Left(e.msg)
      }
      bestHeader <- EitherT
        .fromOption[F](bestHeaderOption, Left("No best header"))
      merkleState = MerkleState.from(bestHeader)
      infosEitherT <- program.runA(merkleState.main).leftMap(_.asLeft[String])
      infos        <- infosEitherT.leftMap(_.asLeft[String])
    yield infos.toSeq

  def getAccountSnapshot[F[_]: Concurrent: BlockRepository: PlayNommState](
      account: Account,
  ): EitherT[F, Either[String, String], Option[ActivitySnapshot]] =

    val program = PlayNommState[F].reward.accountSnapshot.get(account)

    for
      bestHeaderOption <- BlockRepository[F].bestHeader.leftMap { e =>
        Left(e.msg)
      }
      bestHeader <- EitherT
        .fromOption[F](bestHeaderOption, Left("No best header"))
      merkleState = MerkleState.from(bestHeader)
      snapshotOption <- program.runA(merkleState.main).leftMap(_.asLeft[String])
    yield snapshotOption

  def getTokenSnapshot[F[_]: Concurrent: BlockRepository: PlayNommState](
      tokenId: TokenId,
  ): EitherT[F, Either[String, String], Option[ActivitySnapshot]] =

    val program = PlayNommState[F].reward.tokenSnapshot.get(tokenId)

    for
      bestHeaderOption <- BlockRepository[F].bestHeader.leftMap { e =>
        Left(e.msg)
      }
      bestHeader <- EitherT
        .fromOption[F](bestHeaderOption, Left("No best header"))
      merkleState = MerkleState.from(bestHeader)
      snapshotOption <- program.runA(merkleState.main).leftMap(_.asLeft[String])
    yield snapshotOption

  def getOwnershipSnapshot[F[_]: Concurrent: BlockRepository: PlayNommState](
      tokenId: TokenId,
  ): EitherT[F, Either[String, String], Option[OwnershipSnapshot]] =

    val program = PlayNommState[F].reward.ownershipSnapshot.get(tokenId)

    for
      bestHeaderOption <- BlockRepository[F].bestHeader.leftMap { e =>
        Left(e.msg)
      }
      bestHeader <- EitherT
        .fromOption[F](bestHeaderOption, Left("No best header"))
      merkleState = MerkleState.from(bestHeader)
      snapshotOption <- program.runA(merkleState.main).leftMap(_.asLeft[String])
    yield snapshotOption

  def getOwnershipSnapshotMap[F[_]: Concurrent: BlockRepository: PlayNommState](
      from: Option[TokenId],
      limit: Int,
  ): EitherT[F, Either[String, String], Map[TokenId, OwnershipSnapshot]] =

    val program = PlayNommState[F].reward.ownershipSnapshot
      .from(from.fold(ByteVector.empty)(_.toBytes))

    for
      bestHeaderOption <- BlockRepository[F].bestHeader.leftMap { e =>
        Left(e.msg)
      }
      bestHeader <- EitherT
        .fromOption[F](bestHeaderOption, Left("No best header"))
      merkleState = MerkleState.from(bestHeader)
      snapshotStream <- program.runA(merkleState.main).leftMap(_.asLeft[String])
      snapshots <- snapshotStream
        .take(limit)
        .compile
        .toList
        .leftMap(_.asLeft[String])
    yield snapshots.toMap

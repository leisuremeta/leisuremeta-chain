package io.leisuremeta.chain
package node
package service

import cats.data.EitherT
import cats.effect.Concurrent
import cats.syntax.bifunctor.*
import cats.syntax.functor.*
import cats.syntax.flatMap.*
import cats.syntax.traverse.*

import GossipDomain.MerkleState
import api.{LeisureMetaChainApi as Api}
import api.model.{
  Account,
  GroupId,
  GroupData,
  PublicKeySummary,
  Transaction,
  TransactionWithResult,
}
import api.model.api_model.{AccountInfo, BalanceInfo, GroupInfo, NftBalanceInfo}
import api.model.token.{
  Rarity,
  NftState,
  TokenDefinition,
  TokenDefinitionId,
  TokenId,
}
import lib.codec.byte.{ByteDecoder, DecodeResult}
import lib.codec.byte.ByteEncoder.ops.*
import lib.crypto.Hash
import lib.datatype.BigNat
import lib.merkle.MerkleTrie
import repository.{BlockRepository, StateRepository, TransactionRepository}
import StateRepository.given

object StateReadService:
  def getAccountInfo[F[_]
    : Concurrent: BlockRepository: StateRepository.AccountState](
      account: Account,
  ): F[Option[AccountInfo]] = for
    bestHeaderEither <- BlockRepository[F].bestHeader.value
    bestHeader <- bestHeaderEither match
      case Left(err) => Concurrent[F].raiseError(err)
      case Right(None) =>
        Concurrent[F].raiseError(new Exception("No best header"))
      case Right(Some(bestHeader)) => Concurrent[F].pure(bestHeader)
    merkleState = MerkleState.from(bestHeader)
    accountStateEither <- MerkleTrie
      .get[F, Account, Option[Account]](account.toBytes.bits)
      .runA(merkleState.account.namesState)
      .value
    accountStateOption <- accountStateEither match
      case Left(err) => Concurrent[F].raiseError(new Exception(err))
      case Right(accountStateOption) => Concurrent[F].pure(accountStateOption)
    keyListEither <- MerkleTrie
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
  yield accountStateOption.map(guardian => AccountInfo(guardian, keyList.toMap))

  def getGroupInfo[F[_]
    : Concurrent: BlockRepository: StateRepository.GroupState](
      groupId: GroupId,
  ): F[Option[GroupInfo]] = for
    bestHeaderEither <- BlockRepository[F].bestHeader.value
    bestHeader <- bestHeaderEither match
      case Left(err) => Concurrent[F].raiseError(err)
      case Right(None) =>
        Concurrent[F].raiseError(new Exception("No best header"))
      case Right(Some(bestHeader)) => Concurrent[F].pure(bestHeader)
    merkleState = MerkleState.from(bestHeader)
    groupDataEither <- MerkleTrie
      .get[F, GroupId, GroupData](groupId.toBytes.bits)
      .runA(merkleState.group.groupState)
      .value
    groupDataOption <- groupDataEither match
      case Left(err) => Concurrent[F].raiseError(new Exception(err))
      case Right(groupDataOption) => Concurrent[F].pure(groupDataOption)
    accountListEither <- MerkleTrie
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
    : Concurrent: BlockRepository: StateRepository.TokenState](
      tokenDefinitionId: TokenDefinitionId,
  ): F[Option[TokenDefinition]] = for
    bestHeaderEither <- BlockRepository[F].bestHeader.value
    bestHeader <- bestHeaderEither match
      case Left(err) => Concurrent[F].raiseError(err)
      case Right(None) =>
        Concurrent[F].raiseError(new Exception("No best header"))
      case Right(Some(bestHeader)) => Concurrent[F].pure(bestHeader)
    merkleState = MerkleState.from(bestHeader)
    tokenDefEither <- MerkleTrie
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
    : Concurrent: BlockRepository: TransactionRepository: StateRepository.TokenState](
      account: Account,
      movableOption: Option[Api.Movable],
  ): F[Map[TokenDefinitionId, BalanceInfo]] = for
    bestHeaderEither <- BlockRepository[F].bestHeader.value
    bestHeader <- bestHeaderEither match
      case Left(err) => Concurrent[F].raiseError(err)
      case Right(None) =>
        Concurrent[F].raiseError(new Exception("No best header"))
      case Right(Some(bestHeader)) => Concurrent[F].pure(bestHeader)
    merkleState = MerkleState.from(bestHeader)
    balanceListEither <- MerkleTrie
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
                    if remainder.isEmpty then Right((tokenDefinitionId, txHash))
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
      case fb : Transaction.FungibleBalance => fb match
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
      case _ => BalanceInfo(totalAmount = BigNat.Zero, unused = Map.empty)
  }((a, b) =>
    BalanceInfo(
      totalAmount = BigNat.add(a.totalAmount, b.totalAmount),
      unused = a.unused ++ b.unused,
    ),
  )

  def getNftBalance[F[_]
    : Concurrent: BlockRepository: TransactionRepository: StateRepository.TokenState](
      account: Account,
      movableOption: Option[Api.Movable],
  ): F[Map[TokenId, NftBalanceInfo]] = for
    bestHeaderEither <- BlockRepository[F].bestHeader.value
    bestHeader <- bestHeaderEither match
      case Left(err) => Concurrent[F].raiseError(err)
      case Right(None) =>
        Concurrent[F].raiseError(new Exception("No best header"))
      case Right(Some(bestHeader)) => Concurrent[F].pure(bestHeader)
    merkleState = MerkleState.from(bestHeader)
    balanceListEither <- MerkleTrie
      .from[
        F,
        (Account, TokenId, Hash.Value[TransactionWithResult]),
        Unit,
      ](
        account.toBytes.bits,
      )
      .runA(merkleState.token.nftBalanceState)
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
            case nb: Transaction.NftBalance => nb match
              case mf: Transaction.TokenTx.MintNFT =>
                Map(
                  tokenId -> NftBalanceInfo(
                    mf.tokenDefinitionId,
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

  def getToken[F[_]: Concurrent: BlockRepository: StateRepository.TokenState](
      tokenId: TokenId,
  ): EitherT[F, String, Option[NftState]] = for
    bestHeaderOption <- BlockRepository[F].bestHeader.leftMap(_.msg)
    bestHeader <- EitherT.fromOption[F](bestHeaderOption, "No best header")
    merkleState = MerkleState.from(bestHeader)
    nftStateOption <- MerkleTrie
      .get[F, TokenId, NftState](tokenId.toBytes.bits)
      .runA(merkleState.token.nftState)
  yield nftStateOption

  def getOwners[F[_]: Concurrent: BlockRepository: StateRepository.TokenState](
      tokenDefinitionId: TokenDefinitionId,
  ): EitherT[F, String, Map[TokenId, Account]] = for
    bestHeaderOption <- BlockRepository[F].bestHeader.leftMap(_.msg)
    bestHeader <- EitherT.fromOption[F](bestHeaderOption, "No best header")
    merkleState = MerkleState.from(bestHeader)
    tokenIdList <- MerkleTrie
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
      MerkleTrie
        .get[F, TokenId, NftState](tokenId.toBytes.bits)
        .runA(merkleState.token.nftState)
        .map(nftStateOption => nftStateOption.map(state => (tokenId, state.currentOwner)))
    }
  yield ownerOptionList.flatten.toMap

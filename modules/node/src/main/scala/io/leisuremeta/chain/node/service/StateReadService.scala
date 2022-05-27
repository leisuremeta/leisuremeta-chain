package io.leisuremeta.chain
package node
package service

import cats.data.EitherT
import cats.effect.Concurrent
import cats.syntax.functor.*
import cats.syntax.flatMap.*
import cats.syntax.traverse.*

import GossipDomain.MerkleState
import api.model.{Account, PublicKeySummary}
import api.model.api_model.AccountInfo
import lib.codec.byte.{ByteDecoder, DecodeResult}
import lib.codec.byte.ByteEncoder.ops.*
import lib.merkle.MerkleTrie
import repository.{BlockRepository, StateRepository}
import StateRepository.given

object StateReadService:
  def getAccountInfo[F[_]
    : Concurrent: BlockRepository: StateRepository.AccountState.Name: StateRepository.AccountState.Key](
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
      .runA(merkleState.namesState)
      .value
    accountStateOption <- accountStateEither match
      case Left(err) => Concurrent[F].raiseError(new Exception(err))
      case Right(accountStateOption) => Concurrent[F].pure(accountStateOption)
    keyListEither <- MerkleTrie
      .from[F, (Account, PublicKeySummary), PublicKeySummary.Info](
        account.toBytes.bits,
      )
      .runA(merkleState.keyState)
      .flatMap(_.compile.toList.flatMap{
        (list) => list.traverse{
          case (bits, v) => EitherT.fromEither[F]{
            ByteDecoder[(Account,PublicKeySummary)].decode(bits.bytes) match
              case Left(err) => Left(err.msg)
              case Right(DecodeResult((account, publicKeySummary), remainder)) =>
                if remainder.isEmpty then Right((publicKeySummary, v))
                else Left(s"non-empty remainder in decoding $publicKeySummary")
          }
        }
      })
      .value
    keyList <- keyListEither match
      case Left(err)      => Concurrent[F].raiseError(new Exception(err))
      case Right(keyList) => Concurrent[F].pure(keyList)
  yield
    accountStateOption.map(guardian => AccountInfo(guardian, keyList.toMap))

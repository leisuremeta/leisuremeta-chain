package org.leisuremeta.lmchain.core
package node
package service

import cats.Monad
import cats.data.EitherT
import cats.effect.Sync
import cats.implicits._

import codec.byte.ByteDecoder
import codec.byte.ByteEncoder.ops._
import crypto.MerkleTrie
import model.{Account, Block, NameState, TokenState, Transaction}
import model.Account.Name
import model.Transaction.Token.DefinitionId
import repository.{BlockRepository, StateRepository}
import repository.StateRepository._
import scodec.bits.BitVector
import org.leisuremeta.lmchain.core.failure.DecodingFailure
import org.leisuremeta.lmchain.core.codec.byte.DecodeResult

object StateReadService {

  def getState[F[_]: Monad: BlockRepository, K, V: ByteDecoder](
      key: BitVector,
      root: Block.Header => Option[MerkleTrie.MerkleRoot[K, V]],
  )(implicit
      stateRepo: StateRepository[F, K, V]
  ): EitherT[F, String, Option[V]] = for {
    bestBlockHeaderOption <- BlockRepository[F].bestHeader.leftMap(_.msg)
    bestBlockHeader <- EitherT
      .fromOption[F](bestBlockHeaderOption, "No best block header")
    state = root(bestBlockHeader).fold(MerkleTrie.MerkleTrieState.empty[K, V])(
      MerkleTrie.MerkleTrieState.fromRoot[K, V]
    )
    vOption <- MerkleTrie.get[F, K, V](key).runA(state)
  } yield vOption

  def getNameState[F[_]: Monad: BlockRepository: StateRepository.Name](
      name: Name
  ): EitherT[F, String, Option[NameState]] = {
    getState[F, Name, NameState](name.bytes.bits, _.namesRoot)
  }

  def getTokenState[F[_]: Monad: BlockRepository: StateRepository.Token](
      definitionId: DefinitionId
  ): EitherT[F, String, Option[TokenState]] = {
    getState[F, DefinitionId, TokenState](
      definitionId.toBytes.bits,
      _.tokenRoot,
    )
  }

  def getBalance[F[_]: Sync: BlockRepository: StateRepository.Balance](
      account: Account
  ): EitherT[F, String, List[Transaction.Input.Tx]] = for {
    bestBlockHeaderOption <- BlockRepository[F].bestHeader.leftMap(_.msg)
    bestBlockHeader <- EitherT
      .fromOption[F](bestBlockHeaderOption, "No best block header")
    state = bestBlockHeader.balanceRoot.fold(
      MerkleTrie.MerkleTrieState.empty[(Account, Transaction.Input.Tx), Unit]
    )(
      MerkleTrie.MerkleTrieState.fromRoot[(Account, Transaction.Input.Tx), Unit]
    )
    accountBits = account.toBytes.bits
    iterant <- MerkleTrie
      .from[F, (Account, Transaction.Input.Tx), Unit](accountBits)
      .runA(state)
    bits <- iterant.map(_._1).takeWhile(_ `startsWith` accountBits).toListL
    txs <- EitherT.fromEither[F](bits.traverse { bit =>
      ByteDecoder[(Account, Transaction.Input.Tx)]
        .decode(bit.toByteVector) match {
        case Left(DecodingFailure(msg))                  => Left(msg)
        case Right(DecodeResult((_, t), r)) if r.isEmpty => Right(t)
        case Right(_)                                    => Left(s"Non empty decode result: ${bit.toByteVector}")
      }
    })
  } yield txs

}

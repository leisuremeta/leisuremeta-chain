package io.leisuremeta.chain
package node
package dapp
package submodule

import cats.data.{EitherT, StateT}
import cats.effect.Concurrent
import cats.syntax.all.*

import api.model.{
  Account,
  AccountSignature,
  Signed,
  Transaction,
  TransactionWithResult,
}
import lib.codec.byte.ByteEncoder.ops.*
import lib.crypto.Hash
import lib.datatype.BigNat
import lib.merkle.MerkleTrieState
import repository.TransactionRepository

object PlayNommDAppAgenda:
  def apply[F[_]: Concurrent: PlayNommState: TransactionRepository](
      tx: Transaction.AgendaTx,
      sig: AccountSignature,
  ): StateT[
    EitherT[F, PlayNommDAppFailure, *],
    MerkleTrieState,
    TransactionWithResult,
  ] = tx match
    case ssa: Transaction.AgendaTx.SuggestSimpleAgenda =>
      PlayNommDAppAccount
        .verifySignature[F](sig, ssa)
        .map: _ =>
          TransactionWithResult(Signed(sig, ssa), None)

    case vsa: Transaction.AgendaTx.VoteSimpleAgenda =>
      for
        agendaTx <- StateT.liftF(getSuggestSimpleAgendaTx(vsa.agendaTxHash))
        fungibleBalanceStream <- PlayNommState[F].token.fungibleBalance
          .streamWithPrefix((sig.account, agendaTx.votingToken).toBytes)
          .mapK:
            PlayNommDAppFailure.mapInternal:
              s"Fail to get fungible balance stream of ${sig.account} ${agendaTx.votingToken}"
        fungibleBalanceTxs <- StateT
          .liftF:
            fungibleBalanceStream
              .map(_._1._3)
              .compile
              .toList
          .mapK:
            PlayNommDAppFailure.mapInternal:
              s"Fail to get fungible balance txs of ${sig.account} ${agendaTx.votingToken}"
        freeBalance <- PlayNommDAppToken.getFungibleBalanceTotalAmounts(
          fungibleBalanceTxs.toSet,
          sig.account,
        )
        entrustBalanceStream <- PlayNommState[F].token.entrustFungibleBalance
          .streamWithPrefix(sig.account.toBytes)
          .mapK:
            PlayNommDAppFailure.mapInternal:
              s"Fail to get entrust balance stream of ${sig.account} ${agendaTx.votingToken}"
        enturstBalanceAmounts <- StateT
          .liftF:
            entrustBalanceStream
              .filter(_._1._3 === agendaTx.votingToken)
              .map(_._1._4)
              .evalMap: txHash =>
                TransactionRepository[F]
                  .get(txHash)
                  .leftMap(_.msg)
                  .map:
                    case Some(txWithResult) =>
                      txWithResult.signedTx.value match
                        case ef: Transaction.TokenTx.EntrustFungibleToken =>
                          ef.amount
                        case _ =>
                          BigNat.Zero
                    case None => BigNat.Zero
              .compile
              .toList
          .mapK:
            PlayNommDAppFailure.mapInternal:
              s"Fail to get entrust balance txs of ${sig.account} ${agendaTx.votingToken}"
        entrustBalance = enturstBalanceAmounts.foldLeft(BigNat.Zero)(BigNat.add)
        votingAmount   = BigNat.add(freeBalance, entrustBalance)
        txResult = Transaction.AgendaTx.VoteSimpleAgendaResult(votingAmount)
      yield TransactionWithResult(Signed(sig, vsa), Some(txResult))

  def getSuggestSimpleAgendaTx[F[_]: Concurrent: TransactionRepository](
      txHash: Hash.Value[TransactionWithResult],
  ): EitherT[F, PlayNommDAppFailure, Transaction.AgendaTx.SuggestSimpleAgenda] =
    for
      txWithResultOption <- TransactionRepository[F]
        .get(txHash)
        .leftMap: e =>
          PlayNommDAppFailure.internal(s"Fail to get tx $txHash: ${e.msg}")
      txWithResult <- EitherT.fromOption(
        txWithResultOption,
        PlayNommDAppFailure.external(s"AgendaTx ${txHash} not found"),
      )
      agendaTx <- txWithResult.signedTx.value match
        case ssa: Transaction.AgendaTx.SuggestSimpleAgenda => EitherT.pure(ssa)
        case _ =>
          EitherT.leftT:
            PlayNommDAppFailure.external:
              s"AgendaTx ${txHash} is not SuggestSimpleAgenda"
    yield agendaTx

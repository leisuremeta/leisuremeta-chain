package io.leisuremeta.chain
package node
package dapp
package submodule

//import cats.Monad
import cats.data.{EitherT, StateT}
import cats.effect.Concurrent
//import cats.syntax.all.*

import api.model.{
//  Account,
  AccountSignature,
//  Signed,
  Transaction,
  TransactionWithResult,
}
//import api.model.TransactionWithResult.ops.*
//import api.model.token.*
//import api.model.token.SnapshotState.SnapshotId.*
//import lib.codec.byte.ByteEncoder.ops.*
//import lib.crypto.Hash
//import lib.crypto.Hash.ops.*
//import lib.datatype.BigNat
import lib.merkle.MerkleTrieState
import repository.TransactionRepository

object PlayNommDAppVoting:

  def apply[F[_]: Concurrent: PlayNommState: TransactionRepository](
      tx: Transaction.VotingTx,
      sig: AccountSignature,
  ): StateT[
    EitherT[F, PlayNommDAppFailure, *],
    MerkleTrieState,
    TransactionWithResult,
  ] = tx match
    case cp: Transaction.VotingTx.CreateVoteProposal =>
      ???
    case cv: Transaction.VotingTx.CastVote =>
      ???
    case tv: Transaction.VotingTx.TallyVotes =>
      ???


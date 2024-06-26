package io.leisuremeta.chain
package node
package dapp
package submodule

//import cats.Monad
import cats.data.{EitherT, StateT}
import cats.effect.Concurrent
import cats.syntax.all.*

import api.model.{
//  Account,
  AccountSignature,
  Signed,
  Transaction,
  TransactionWithResult,
}
//import api.model.TransactionWithResult.ops.*
//import api.model.token.*
import api.model.voting.*
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
      for
        _ <- PlayNommDAppAccount.verifySignature(sig, tx)
        _ <- cp.votingTokens.toList.traverse: defId =>
          PlayNommDAppToken.checkMinterAndGetTokenDefinition(sig.account, defId)
        proposal = Proposal(
          createdAt = cp.createdAt,
          proposalId = cp.proposalId,
          title = cp.title,
          description = cp.description,
          votingTokens = cp.votingTokens,
          snapshotId = cp.snapshotId,
          voteStart = cp.voteStart,
          voteEnd = cp.voteEnd,
          voteOptions = cp.voteOptions,
          quorum = cp.quorum,
          passThresholdNumer = cp.passThresholdNumer,
          passThresholdDenom = cp.passThresholdDenom,
        )
        _ <- PlayNommState[F].voting.proposal
          .put(cp.proposalId, proposal)
          .mapK:
            PlayNommDAppFailure.mapInternal:
              s"Failed to save proposal: ${cp.proposalId}"
      yield TransactionWithResult(Signed(sig, cp))(None)
    case cv: Transaction.VotingTx.CastVote =>
      ???
    case tv: Transaction.VotingTx.TallyVotes =>
      ???

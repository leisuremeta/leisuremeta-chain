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
import lib.codec.byte.ByteEncoder.ops.*
//import lib.crypto.Hash
//import lib.crypto.Hash.ops.*
import lib.datatype.BigNat
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
        _ <- cp.votingPower.toList.traverse: (defId, _) =>
          PlayNommDAppToken.checkMinterAndGetTokenDefinition(sig.account, defId)
        proposal = Proposal(
          createdAt = cp.createdAt,
          proposalId = cp.proposalId,
          title = cp.title,
          description = cp.description,
          votingPower = cp.votingPower,
          voteStart = cp.voteStart,
          voteEnd = cp.voteEnd,
          voteType = cp.voteType,
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
      for
        _ <- PlayNommDAppAccount.verifySignature(sig, tx)
        voteOption <- PlayNommState[F].voting.votes
          .get((cv.proposalId, sig.account))
          .mapK:
            PlayNommDAppFailure.mapInternal:
              s"Failed to get vote: ${cv.proposalId} ${sig.account}"
        _ <- checkExternal(
          voteOption.isEmpty,
          s"Vote already casted: ${cv.proposalId} ${sig.account}",
        )
        proposalOption <- PlayNommState[F].voting.proposal
          .get(cv.proposalId)
          .mapK:
            PlayNommDAppFailure.mapInternal:
              s"Failed to get proposal: ${cv.proposalId}"
        proposal <- fromOption(proposalOption, s"Proposal not found: ${cv.proposalId}")
        amountSeq <- proposal.voteType match
          case VoteType.ONE_PERSON_ONE_VOTE =>
            pure(Seq(BigNat.One))
          case VoteType.TOKEN_WEIGHTED =>
            proposal.votingPower.toSeq.traverse: (defId, snapshotId) =>
              for
                balance <- PlayNommState[F].token.fungibleSnapshot
                  .reverseStreamFrom((sig.account, defId).toBytes, Some(snapshotId.toBytes))
                  .mapK:
                    PlayNommDAppFailure.mapInternal:
                      s"Failed to get balance stream of: ${sig.account} ${defId}"
                  .flatMap: stream =>
                    StateT.liftF:
                      stream.head.compile.toList
                        .map:
                          case Nil => Map.empty
                          case (k, v) :: _ => v
                        .leftMap: msg =>
                          PlayNommDAppFailure.internal:
                            s"Failed to get balance of: ${sig.account} ${defId} ${snapshotId}: ${msg}"
              yield balance.values.foldLeft(BigNat.Zero)(BigNat.add)
          case VoteType.NFT_BASED =>
            proposal.votingPower.toSeq.traverse: (defId, snapshotId) =>
              for
                count <- PlayNommState[F].token.nftSnapshot
                  .reverseStreamFrom((sig.account, defId).toBytes, Some(snapshotId.toBytes))
                  .mapK:
                    PlayNommDAppFailure.mapInternal:
                      s"Failed to get balance stream of: ${sig.account} ${defId}"
                  .flatMap: stream =>
                    StateT.liftF:
                      stream.head.compile.toList
                        .map: tokenIds =>
                          tokenIds.size
                        .leftMap: msg =>
                          PlayNommDAppFailure.internal:
                            s"Failed to get balance of: ${sig.account} ${defId} ${snapshotId}: ${msg}"
              yield BigNat.unsafeFromBigInt(BigInt(count))
        powerSum = amountSeq.foldLeft(BigNat.Zero)(BigNat.add)
        originalPowerOption <- PlayNommState[F].voting.votes
          .put((cv.proposalId, sig.account), (cv.selectedOption, powerSum))
          .mapK:
            PlayNommDAppFailure.mapInternal:
              s"Failed to save vote: ${cv.proposalId} ${sig.account}"
        originalMapOption <- PlayNommState[F].voting.counting
          .get(cv.proposalId)
          .mapK:
            PlayNommDAppFailure.mapInternal:
              s"Failed to get counting: ${cv.proposalId}"
        originalMap = originalMapOption.getOrElse(Map.empty)
        newMap = originalMap.updated(cv.selectedOption, powerSum)
        _ <- PlayNommState[F].voting.counting
          .put(cv.proposalId, newMap)
          .mapK:
            PlayNommDAppFailure.mapInternal:
              s"Failed to save counting: ${cv.proposalId}"
      yield TransactionWithResult(Signed(sig, cv))(None)
      
    case tv: Transaction.VotingTx.TallyVotes =>
      ???

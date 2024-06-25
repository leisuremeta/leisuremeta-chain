package io.leisuremeta.chain
package api.model
package voting

import java.time.Instant
import lib.datatype.{BigNat, Utf8}
import token.TokenDefinitionId

final case class Proposal(
    createdAt: Instant,
    proposalId: ProposalId,
    title: Utf8,
    description: Utf8,
    votingTokens: Set[TokenDefinitionId],
    snapshotId: BigNat,
    voteStart: Instant,
    voteEnd: Instant,
    voteOptions: Map[Utf8, Utf8],
    quorum: BigNat,
    passThresholdNumer: BigNat,
    passThresholdDenom: BigNat,
)

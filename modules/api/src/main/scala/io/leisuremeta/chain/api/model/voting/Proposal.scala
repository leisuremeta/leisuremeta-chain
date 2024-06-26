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
    votingPower: Map[TokenDefinitionId, BigNat],
    voteStart: Instant,
    voteEnd: Instant,
    voteType: VoteType,
    voteOptions: Map[Utf8, Utf8],
    quorum: BigNat,
    passThresholdNumer: BigNat,
    passThresholdDenom: BigNat,
)

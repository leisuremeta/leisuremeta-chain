package io.leisuremeta.chain.api.model

import java.time.Instant

sealed trait Transaction:
  def networkId: NetworkId
  def createdAt: Instant

object Transaction:
  sealed trait AccountTx extends Transaction
  object AccountTx:
    final case class CreateAccount(
        networkId: NetworkId,
        createdAt: Instant,
        account: Account,
        guardian: Option[Account],
    ) extends AccountTx

    final case class AddPublicKeySummaries(
        networkId: NetworkId,
        createdAt: Instant,
        account: Account,
        summaries: Map[PublicKeySummary, String],
    ) extends AccountTx

    final case class RemovePublicKeySummaries(
        networkId: NetworkId,
        createdAt: Instant,
        account: Account,
        summaries: Set[PublicKeySummary],
    ) extends AccountTx

    final case class RemoveAccount(
        networkId: NetworkId,
        createdAt: Instant,
        account: Account,
    ) extends AccountTx
  end AccountTx

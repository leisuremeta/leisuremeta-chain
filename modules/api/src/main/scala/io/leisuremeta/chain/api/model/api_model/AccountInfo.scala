package io.leisuremeta.chain.api.model
package api_model

final case class AccountInfo(
    guardian: Option[Account],
    publicKeySummaries: Map[PublicKeySummary, PublicKeySummary.Info],
)

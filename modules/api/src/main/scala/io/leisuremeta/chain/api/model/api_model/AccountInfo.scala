package io.leisuremeta.chain
package api.model
package api_model

import account.EthAddress

final case class AccountInfo(
    ethAddress: Option[EthAddress],
    guardian: Option[Account],
    publicKeySummaries: Map[PublicKeySummary, PublicKeySummary.Info],
)

package io.leisuremeta.chain
package api.model
package api_model

import account.*
import lib.datatype.Utf8

final case class AccountInfo(
    externalChainAddresses: Map[ExternalChain, ExternalChainAddress],
    ethAddress: Option[EthAddress],
    guardian: Option[Account],
    memo: Option[Utf8],
    publicKeySummaries: Map[PublicKeySummary, PublicKeySummary.Info],
)

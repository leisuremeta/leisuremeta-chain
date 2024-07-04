package io.leisuremeta.chain
package api.model

import java.time.Instant

import account.{ExternalChain, ExternalChainAddress}
import lib.datatype.Utf8

final case class AccountData(
    externalChainAddresses: Map[ExternalChain, ExternalChainAddress],
    guardian: Option[Account],
    lastChecked: Instant,
    memo: Option[Utf8],
)

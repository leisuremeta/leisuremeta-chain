package io.leisuremeta.chain
package api.model

import account.EthAddress

final case class AccountData(
    ethAddress: Option[EthAddress],
    guardian: Option[Account],
)

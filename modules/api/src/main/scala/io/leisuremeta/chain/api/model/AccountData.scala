package io.leisuremeta.chain
package api.model

import lib.datatype.Utf8

final case class AccountData(
    ethAddress: Option[Utf8],
    guardian: Option[Account],
)

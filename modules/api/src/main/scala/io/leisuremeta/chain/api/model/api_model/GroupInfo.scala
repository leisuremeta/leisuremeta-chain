package io.leisuremeta.chain.api.model
package api_model

final case class GroupInfo(
    data: GroupData,
    accounts: Set[Account],
)

package io.leisuremeta.chain
package api.model

import lib.datatype.Utf8

final case class GroupData(
    name: Utf8,
    coordinator: Account,
)

package io.leisuremeta.chain
package api.model
package creator_dao

import lib.datatype.Utf8

final case class CreatorDaoInfo(
    id: CreatorDaoId,
    name: Utf8,
    description: Utf8,
    founder: Account,
    coordinator: Account,
)

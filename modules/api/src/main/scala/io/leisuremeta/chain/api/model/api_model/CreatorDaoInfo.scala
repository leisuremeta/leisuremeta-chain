package io.leisuremeta.chain
package api.model
package api_model

import lib.datatype.Utf8
import creator_dao.CreatorDaoId

final case class CreatorDaoInfo(
    id: CreatorDaoId,
    name: Utf8,
    description: Utf8,
    founder: Account,
    coordinator: Account,
    moderators: Set[Account],
)

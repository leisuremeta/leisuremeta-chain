package io.leisuremeta.chain.lmscan.backend.model

import io.leisuremeta.chain.lmscan.backend.model.NftActivity
import io.leisuremeta.chain.lmscan.backend.entity.NftFile

final case class NftDetail(
    nftFile: Option[NftFile],
    activities: Seq[NftActivity],
)

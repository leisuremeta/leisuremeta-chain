package io.leisuremeta.chain.lmscan.common.model

import io.leisuremeta.chain.lmscan.common.model.NftActivity
import io.leisuremeta.chain.lmscan.common.model.NftFileModel

final case class NftDetail(
    nftFile: Option[NftFileModel] = None,
    activities: Option[Seq[NftActivity]] = None,
) extends ApiModel 

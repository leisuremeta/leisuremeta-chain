package io.leisuremeta.chain.lmscan.common.model

final case class NftDetail(
    nftFile: Option[NftFileModel] = None,
    activities: Option[Seq[NftActivity]] = None,
) extends ApiModel 

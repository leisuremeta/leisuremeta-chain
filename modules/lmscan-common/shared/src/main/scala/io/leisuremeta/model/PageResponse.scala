package io.leisuremeta.chain.lmscan.common.model

import sttp.tapir.generic.auto.*

final case class PageResponse[T](
    totalCount: Long = 0L,
    totalPages: Int = 0,
    payload: Seq[T] = Seq(),
) extends ApiModel

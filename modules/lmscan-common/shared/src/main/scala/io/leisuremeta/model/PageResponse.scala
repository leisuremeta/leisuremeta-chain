package io.leisuremeta.chain.lmscan.common.model

import sttp.tapir.generic.auto.*

final case class PageResponse[T](
    totalCount: Long = 0L,
    totalPages: Int = 0,
    payload: Seq[T] = Seq(),
) extends ApiModel

object PageResponse:
    def from[T](total: Long, cnt: Int, seq: Seq[T]) =
        val totalPage = Math.ceil(total.toDouble / cnt).toInt;
        PageResponse(total, totalPage, seq)

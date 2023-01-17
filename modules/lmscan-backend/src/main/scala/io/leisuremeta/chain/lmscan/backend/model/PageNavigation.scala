package io.leisuremeta.chain.lmscan.backend.model

import sttp.tapir.EndpointIO.annotations.endpointInput
import sttp.tapir.EndpointIO.annotations.query

// @endpointInput("tx/list")
case class PageNavigation(
    @query
    useDataNav: Boolean,
    @query
    pageNo: Int,
    @query
    sizePerRequest: Int,
)

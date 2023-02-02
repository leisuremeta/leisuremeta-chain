package io.leisuremeta.chain.lmscan.frontend

import io.circe.Json

// import cats.effect.IO
final case class Model(
    // nav move
    prevPage: NavMsg,
    curPage: NavMsg,

    // input search string
    searchValue: String,

    // transaction detail toggle
    toggle: Boolean,

    // page move
    tx_CurrentPage: Int,
    tx_TotalPage: Int,
    block_CurrentPage: Int,
    block_TotalPage: Int,

    // page_Search: String,
    block_list_Search: String,
    tx_list_Search: String,

    // api data
    txListData: Option[String] = Some(""),
    blockListData: Option[String] = Some(""),
    txDetailData: Option[String] = Some(""),
    blockDetailData: Option[String] = Some(""),
    nftDetailData: Option[String] = Some(""),
)

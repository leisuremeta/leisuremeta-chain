package io.leisuremeta.chain.lmscan.frontend

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
)

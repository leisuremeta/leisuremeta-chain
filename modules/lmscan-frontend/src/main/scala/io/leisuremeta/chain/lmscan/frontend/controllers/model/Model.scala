package io.leisuremeta.chain.lmscan.frontend

// import cats.effect.IO
final case class Model(
    prevPage: NavMsg,
    curPage: NavMsg,
    searchValue: String,
    toggle: Boolean,
)

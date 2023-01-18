package io.leisuremeta.chain.lmscan.frontend
// import tyrian.*
// import cats.effect.IO
final case class Model(
    prevPage: NavMsg,
    curPage: NavMsg,
    searchValue: String,
)

package io.leisuremeta.chain.lmscan.common.model

import io.circe.HCursor
import io.circe.Decoder

enum SearchResult:
  case acc(item: AccountDetail)
  case tx(item: TxDetail)
  case blc(item: BlockDetail)
  case nft(item: NftDetail)
  case empty

object SearchResult:
  given Decoder[SearchResult] = (c: HCursor) =>
    c.keys match
      case Some(k) if k.head == "acc" => 
        c.downField("acc").get[AccountDetail]("item").map(item => SearchResult.acc(item))
      case Some(k) if k.head == "tx" => 
        c.downField("tx").get[TxDetail]("item").map(item => SearchResult.tx(item))
      case Some(k) if k.head == "blc" => 
        c.downField("blc").get[BlockDetail]("item").map(item => SearchResult.blc(item))
      case Some(k) if k.head == "nft" => 
        c.downField("nft").get[NftDetail]("item").map(item => SearchResult.nft(item))
      case _ => Decoder.resultInstance.pure(SearchResult.empty)
      // res = acc.downField("item").as[AccountDetail]
    // SearchResult.acc(res.get.)

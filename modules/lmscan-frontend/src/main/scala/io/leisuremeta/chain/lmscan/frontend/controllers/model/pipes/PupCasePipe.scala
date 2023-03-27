package io.leisuremeta.chain.lmscan.frontend
import scala.util.chaining.*
import io.leisuremeta.chain.lmscan.common.model.*
import scala.scalajs.js

object PupCasePipe:
  def in_Page(pubCase: PubCase) =
    pubCase match
      case PubCase.BlockPub(page, _, _)       => page
      case PubCase.TxPub(page, _, _, _, _, _) => page
      case PubCase.BoardPub(page, _, _)       => page
      case _                                  => 1

  def in_pub_m1(pubCase: PubCase) =
    pubCase match
      case PubCase.BlockPub(_, pub_m1, _)         => pub_m1
      case PubCase.TxPub(_, _, _, _, pub_m1, _)   => pub_m1
      case PubCase.BoardPub(_, pub_m1, _)         => pub_m1
      case PubCase.BlockDetailPub(_, pub_m1, _)   => pub_m1
      case PubCase.TxDetailPub(_, pub_m1, _)      => pub_m1
      case PubCase.AccountDetailPub(_, pub_m1, _) => pub_m1

  def in_pub_m2(pubCase: PubCase) =
    pubCase match
      case PubCase.BlockPub(_, _, pub_m2)         => pub_m2
      case PubCase.TxPub(_, _, _, _, _, pub_m2)   => pub_m2
      case PubCase.BoardPub(_, _, pub_m2)         => pub_m2
      case PubCase.BlockDetailPub(_, _, pub_m2)   => pub_m2
      case PubCase.TxDetailPub(_, _, pub_m2)      => pub_m2
      case PubCase.AccountDetailPub(_, _, pub_m2) => pub_m2

  def update_PubCase_data(pub: PubCase, data: String) =
    pub match
      case PubCase.BlockPub(_, _, _) =>
        PubCase.BlockPub(
          pub_m1 = data,
          pub_m2 = BlockParser
            .decodeParser(data)
            .getOrElse(new PageResponse(0, 0, List())),
        )

      case PubCase.TxPub(_, _, _, _, _, _) =>
        PubCase.TxPub(
          pub_m1 = data,
          pub_m2 = TxParser
            .decodeParser(data)
            .getOrElse(new PageResponse(0, 0, List())),
        )

      case PubCase.BoardPub(_, _, _) =>
        PubCase.BoardPub(
          pub_m1 = data,
          pub_m2 = ApiParser
            .decodeParser(data)
            .getOrElse(new SummaryModel),
        )

      case PubCase.BlockDetailPub(_, _, _) =>
        PubCase.BlockDetailPub(
          pub_m1 = data,
          pub_m2 = BlockDetailParser
            .decodeParser(data)
            .getOrElse(new BlockDetail),
        )
      case PubCase.TxDetailPub(_, _, _) =>
        PubCase.TxDetailPub(
          pub_m1 = data,
          pub_m2 = TxDetailParser
            .decodeParser(data)
            .getOrElse(new TxDetail),
        )
      case PubCase.AccountDetailPub(_, _, _) =>
        PubCase.AccountDetailPub(
          pub_m1 = data,
          pub_m2 = AccountDetailParser
            .decodeParser(data)
            .getOrElse(new AccountDetail),
        )

  def get_api_link(pub: PubCase) =
    var base = js.Dynamic.global.process.env.BASE_API_URL
    pub match

      case PubCase.BoardPub(page, _, _) =>
        s"$base/summary/main"

      case PubCase.BlockPub(page, _, _) =>
        s"$base/block/list?pageNo=${(page - 1).toString()}&sizePerRequest=10"

      case PubCase.TxPub(page, sizePerRequest, accountAddr, blockAddr, _, _) =>
        s"$base/tx/list?pageNo=${(page - 1)
            .toString()}&sizePerRequest=${sizePerRequest}" ++ {
          accountAddr match
            case "" => ""
            case _  => s"&accountAddr=${accountAddr}"
        } ++ {
          blockAddr match
            case "" => ""
            case _  => s"&blockAddr=${blockAddr}"
        }

      case PubCase.BlockDetailPub(hash, _, _) =>
        s"$base/block/$hash/detail"

      case PubCase.TxDetailPub(hash, _, _) =>
        s"$base/tx/$hash/detail"

      case PubCase.AccountDetailPub(hash, _, _) =>
        s"$base/account/$hash/detail"

  def filter_txPub(pubs: List[PubCase]) =
    pubs.filter(pub =>
      pub match
        case pub: PubCase.TxPub => true
        case _                  => false,
    )

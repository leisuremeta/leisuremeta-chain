package io.leisuremeta.chain.lmscan.frontend
import scala.util.chaining.*
import io.leisuremeta.chain.lmscan.common.model.*
import scala.scalajs.js
import io.leisuremeta.chain.lmscan.frontend.Log.log
import scala.reflect.ClassTag

object PupCasePipe:
  def in_Page(pubCase: PubCase) =
    pubCase match
      case PubCase.BlockPub(page, _, _, _)    => page
      case PubCase.TxPub(page, _, _, _, _, _) => page
      case PubCase.BoardPub(page, _, _)       => page
      case _                                  => 1

  def in_pub_m1(pubCase: PubCase) =
    pubCase match
      case PubCase.BlockPub(_, _, pub_m1, _)      => pub_m1
      case PubCase.TxPub(_, _, _, _, pub_m1, _)   => pub_m1
      case PubCase.BoardPub(_, pub_m1, _)         => pub_m1
      case PubCase.BlockDetailPub(_, pub_m1, _)   => pub_m1
      case PubCase.TxDetailPub(_, pub_m1, _)      => pub_m1
      case PubCase.AccountDetailPub(_, pub_m1, _) => pub_m1
      case PubCase.NftDetailPub(_, pub_m1, _)     => pub_m1

  def in_pub_m2(pubCase: PubCase) =
    pubCase match
      case PubCase.BlockPub(_, _, _, pub_m2)      => pub_m2
      case PubCase.TxPub(_, _, _, _, _, pub_m2)   => pub_m2
      case PubCase.BoardPub(_, _, pub_m2)         => pub_m2
      case PubCase.BlockDetailPub(_, _, pub_m2)   => pub_m2
      case PubCase.TxDetailPub(_, _, pub_m2)      => pub_m2
      case PubCase.AccountDetailPub(_, _, pub_m2) => pub_m2
      case PubCase.NftDetailPub(_, _, pub_m2)     => pub_m2

  def filterByType[T <: PubCase](lst: List[PubCase])(implicit
      tag: ClassTag[T],
  ): List[T] =
    lst.collect { case t: T =>
      t
    }

  def getPubCase[T <: PubCase](lst: List[PubCase])(implicit
      tag: ClassTag[T],
  ): Option[PubCase] =
    filterByType[T](lst) match
      case Nil       => None
      case head :: _ => Some(head)

  def update_PubCase_data(pub: PubCase, data: String) =
    pub match
      case PubCase.BlockPub(_, _, _, _) =>
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

      case PubCase.NftDetailPub(_, _, _) =>
        PubCase.NftDetailPub(
          pub_m1 = data,
          pub_m2 = NftDetailParser
            .decodeParser(data)
            .getOrElse(new NftDetail),
        )

      case PubCase.AccountDetailPub(_, _, _) =>
        PubCase.AccountDetailPub(
          pub_m1 = data,
          pub_m2 = AccountDetailParser
            .decodeParser(data)
            .getOrElse(new AccountDetail),
        )

  def get_api_link(pub: PubCase, model: Model) =
    // var base = js.Dynamic.global.process.env.BASE_API_URL_DEV
    var base =
      model.commandLink match
        case CommandCaseLink.Development =>
          js.Dynamic.global.process.env.BASE_API_URL_DEV
        case CommandCaseLink.Production =>
          js.Dynamic.global.process.env.BASE_API_URL_PROD
        case CommandCaseLink.Local =>
          js.Dynamic.global.process.env.BASE_API_URL_LOCAL

    pub match

      case PubCase.BoardPub(page, _, _) =>
        s"$base/summary/main"

      case PubCase.BlockPub(page, sizePerRequest, _, _) =>
        s"$base/block/list?pageNo=${(page - 1).toString()}&sizePerRequest=${sizePerRequest}"

      case PubCase.TxPub(page, sizePerRequest, accountAddr, blockHash, _, _) =>
        s"$base/tx/list?pageNo=${(page - 1)
            .toString()}&sizePerRequest=${sizePerRequest}" ++ {
          accountAddr match
            case "" => ""
            case _  => s"&accountAddr=${accountAddr}"
        } ++ {
          blockHash match
            case "" => ""
            case _  => s"&blockHash=${blockHash}"
        }

      case PubCase.BlockDetailPub(hash, _, _) =>
        s"$base/block/$hash/detail"

      case PubCase.TxDetailPub(hash, _, _) =>
        s"$base/tx/$hash/detail"

      case PubCase.NftDetailPub(hash, _, _) =>
        s"$base/nft/$hash/detail"

      case PubCase.AccountDetailPub(hash, _, _) =>
        s"$base/account/$hash/detail"

  def filter_txPub(pubs: List[PubCase]) =
    pubs.filter(pub =>
      pub match
        case pub: PubCase.TxPub => true
        case _                  => false,
    )

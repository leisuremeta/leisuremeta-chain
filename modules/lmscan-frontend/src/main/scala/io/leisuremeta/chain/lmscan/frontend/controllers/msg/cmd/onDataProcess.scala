package io.leisuremeta.chain.lmscan
package frontend

import cats.effect.IO
import io.circe.parser.*
import tyrian.Html.*
import tyrian.*
import tyrian.http.*
import scala.scalajs.js
import Dom.*
import org.scalajs.dom
import org.scalajs.dom.HTMLElement
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration.*
import common.model._

case class ApiPayload(page: String)

object UnderDataProcess:
  private def onResponse(model: TxModel): Response => Msg = response =>
    parse(response.body) match
      case Left(parsingError) => PageMsg.RolloBack
      case Right(json) =>
        PageMsg.UpdateTx(decode[TxList](response.body).getOrElse(model.list))
  private def onResponse(model: BlockModel): Response => Msg = response =>
    parse(response.body) match
      case Left(parsingError) => PageMsg.RolloBack
      case Right(json) =>
        PageMsg.UpdateBlc(decode[BlcList](response.body).getOrElse(model.list))

  private def onResponse(model: TxDetail): Response => Msg = response =>
    import io.circe.*, io.circe.generic.semiauto.*
    given Decoder[TxDetail] = deriveDecoder[TxDetail]
    given Decoder[TransferHist] = deriveDecoder[TransferHist]
    parse(response.body) match
      case Left(parsingError) => PageMsg.RolloBack
      case Right(json) =>
        PageMsg.UpdateTxDetail(decode[TxDetail](response.body).getOrElse(model))
  private def onResponse(model: BlockDetail): Response => Msg = response =>
    import io.circe.*, io.circe.generic.semiauto.*
    given Decoder[BlockDetail] = deriveDecoder[BlockDetail]
    given Decoder[TxInfo] = deriveDecoder[TxInfo]
    parse(response.body) match
      case Left(parsingError) => PageMsg.RolloBack
      case Right(json) =>
        PageMsg.UpdateBlcDetail(decode[BlockDetail](response.body).getOrElse(model))

  private def onResponse(pub: String): Response => Msg = response =>
    import io.circe.*, io.circe.generic.semiauto.*
    given Decoder[SummaryModel] = deriveDecoder[SummaryModel]
    parse(response.body) match
      case Left(parsingError) => PageMsg.RolloBack
      case Right(json) =>
        pub match
          case "a" => PageMsg.Update1(decode[SummaryModel](response.body).getOrElse(SummaryModel()))
          case "b" => PageMsg.Update2(decode[BlcList](response.body).getOrElse(BlcList()))
          case "c" => PageMsg.Update3(decode[TxList](response.body).getOrElse(TxList()))
          case "_" => PageMsg.RolloBack

  private val onError: HttpError => Msg = e => PageMsg.BackObserver

  def fromHttpResponse(model: TxModel): Decoder[Msg] =
    Decoder[Msg](onResponse(model), onError)
  def fromHttpResponse(model: BlockModel): Decoder[Msg] =
    Decoder[Msg](onResponse(model), onError)
  def fromHttpResponse(pub: String): Decoder[Msg] =
    Decoder[Msg](onResponse(pub), onError)
  def fromHttpResponse(model: TxDetail): Decoder[Msg] =
    Decoder[Msg](onResponse(model), onError)
  def fromHttpResponse(model: BlockDetail): Decoder[Msg] =
    Decoder[Msg](onResponse(model), onError)

object OnDataProcess:
  val base = js.Dynamic.global.process.env.BASE_API_URL
  def getList(api: (Int, Int) => String, page: Int = 0, size: Int = 10) = api(page, size)
  def blist(page: Int, size: Int) = s"${base}block/list?pageNo=${page}&sizePerRequest=${size}"
  def tlist(page: Int, size: Int) = s"${base}tx/list?pageNo=${page}&sizePerRequest=${size}"
  def getData(
      model: TxModel,
  ): Cmd[IO, Msg] =
    Http.send(
      Request.get(getList(api = tlist, page = model.page - 1, size = model.size)).withTimeout(30.seconds),
      UnderDataProcess.fromHttpResponse(model),
    )
  def getData(
      model: BlockModel,
  ): Cmd[IO, Msg] =
    Http.send(
      Request.get(getList(api = blist, page = model.page - 1, size = model.size)).withTimeout(30.seconds),
      UnderDataProcess.fromHttpResponse(model),
    )
  def getData(
      pub: String,
  ): Cmd[IO, Msg] =
    Http.send(
      Request.get(getApi(pub)).withTimeout(30.seconds),
      UnderDataProcess.fromHttpResponse(pub),
    )
  def getData(
      detail: TxDetail,
  ): Cmd[IO, Msg] =
    Http.send(
      Request.get(s"${base}tx/${detail.hash.getOrElse("")}/detail").withTimeout(30.seconds),
      UnderDataProcess.fromHttpResponse(detail),
    )
  def getData(
      detail: BlockDetail,
  ): Cmd[IO, Msg] =
    Http.send(
      Request.get(s"${base}block/${detail.hash.getOrElse("")}/detail").withTimeout(30.seconds),
      UnderDataProcess.fromHttpResponse(detail),
    )
  def getApi(target: String) =
    target match
      case "a" => s"${base}summary/main"
      case "b" => getList(blist)
      case "c" => getList(tlist)
      case _ => s""

    // Http.send(
    //   Request.get(get_api_link(pub, model)).withTimeout(30.seconds),
    //   UnderDataProcess.fromHttpResponse(pub),
    // )



      // case PubCase.BoardPub(page, _, _) =>
      //   s"$base/summary/main"

      // case PubCase.BlockPub(page, sizePerRequest, _, _) =>
      //   s"$base/block/list?pageNo=${(page - 1).toString()}&sizePerRequest=${sizePerRequest}"

      // case PubCase.TxPub(
      //       page,
      //       sizePerRequest,
      //       accountAddr,
      //       blockHash,
      //       subtype,
      //       _,
      //       _,
      //     ) =>
      //   s"$base/tx/list?pageNo=${(page - 1)
      //       .toString()}&sizePerRequest=${sizePerRequest}" ++ {
      //     accountAddr match
      //       case "" => ""
      //       case _  => s"&accountAddr=${accountAddr}"
      //   } ++ {
      //     blockHash match
      //       case "" => ""
      //       case _  => s"&blockHash=${blockHash}"
      //   } ++ {
      //     subtype match
      //       case "" => ""
      //       case _  => s"&subtype=${subtype}"
      //   }

      // case PubCase.BlockDetailPub(hash, _, _) =>
      //   s"$base/block/$hash/detail"

      // case PubCase.TxDetailPub(hash, _, _) =>
      //   s"$base/tx/$hash/detail"

      // case PubCase.NftDetailPub(hash, _, _) =>
      //   s"$base/nft/$hash/detail"

      // case PubCase.AccountDetailPub(hash, _, _) =>
      //   s"$base/account/$hash/detail"
// def getPageFromString(search: String): PageCase | CommandCaseMode |
  //   CommandCaseLink =
  //   search match
  //     case ":on"    => CommandCaseMode.Development
  //     case ":off"   => CommandCaseMode.Production
  //     case ":prod"  => CommandCaseLink.Production
  //     case ":dev"   => CommandCaseLink.Development
  //     case ":local" => CommandCaseLink.Local
  //     case subtype if subtypeList.contains(subtype.replaceAll(" ", "")) =>
  //       Transactions()
  //     case _ =>
  //       search.length() match
  //         case 40 =>
  //           AccountDetail(
  //             name = AccountDetail().name,
  //             url = s"account/${search}",
  //           )
  //         case 42 =>
  //           AccountDetail(
  //             name = AccountDetail().name,
  //             url = s"account/${search}",
  //           )
  //         case 25 =>
  //           NftDetail(
  //             url = s"nft/${search}",
  //           )
  //         case 64 =>
  //           TxDetail(
  //             name = Transactions().name,
  //             url = s"transaction/${search}",
  //             pubs = List(PubCase.TxDetailPub(hash = search)),
  //           )
  //         case _ =>
  //           AccountDetail(
  //             name = AccountDetail().name,
  //             url = s"account/${search}",
  //           )

  // def getPageCaseFromUrl(url: String): PageCase =
  //   url match
  //     case s"/dashboard" => DashBoard()
  //     case s"/blocks"    => Blocks()
  //     case s"/blocks/${page}"
  //         if page.forall(
  //           Character.isDigit,
  //         ) =>
  //       Blocks(
  //         url = s"blocks/${limit_value(page)}",
  //       )
  //     case s"/transactions" => Transactions()
  //     case s"/transactions/${page}"
  //         if page.forall(
  //           Character.isDigit,
  //         ) =>
  //       Transactions(
  //         url = s"transactions/${limit_value(page)}",
  //       )
  //     case s"/txs" => Transactions()
  //     case s"/txs/${page}"
  //         if page.forall(
  //           Character.isDigit,
  //         ) =>
  //       Transactions(
  //         url = s"transactions/${limit_value(page)}",
  //       )

  //     case s"/transaction/${hash}" if hash.length() == 64 =>
  //       TxDetail(
  //         name = Transactions().name,
  //         url = s"transaction/${hash}",
  //         pubs = List(PubCase.TxDetailPub(hash = hash)),
  //       )
  //     case s"/tx/${hash}" if hash.length() == 64 =>
  //       TxDetail(
  //         name = Transactions().name,
  //         url = s"transaction/${hash}",
  //         pubs = List(PubCase.TxDetailPub(hash = hash)),
  //       )
  //     case s"/block/${hash}" if hash.length() == 64 =>
  //       BlockDetail(
  //         name = Blocks().name,
  //         url = s"block/${hash}",
  //       )

  //     case s"/nft/${hash}" if hash.length() == 25 =>
  //       NftDetail(
  //         url = s"nft/${hash}",
  //       )
  //     case s"/account/${hash}" =>
  //       AccountDetail(
  //         name = AccountDetail().name,
  //         url = s"account/${hash}",
  //       )

  //     case _ => DashBoard()

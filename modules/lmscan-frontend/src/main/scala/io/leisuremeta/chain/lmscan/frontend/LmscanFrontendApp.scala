package io.leisuremeta.chain.lmscan.frontend

import cats.effect.IO
import tyrian.*
import tyrian.Html.*
import scala.scalajs.js.annotation.*
import io.leisuremeta.chain.lmscan.common.model._
import concurrent.duration.DurationInt

@JSExportTopLevel("LmScan")
object LmscanFrontendApp extends TyrianIOApp[Msg, Model]:
  def init(flags: Map[String, String]): (Model, Cmd[IO, Msg]) = (BaseModel(), Cmd.None)

  def update(model: Model): Msg => (Model, Cmd[IO, Msg]) = model.update
  def view(model: Model): Html[Msg] = model.view

  def subscriptions(model: Model): Sub[IO, Msg] =
    SearchView.detectSearch ++ Pagination.detectSearch
    ++ Sub.Batch(
      Sub.every[IO](1.second, "clock-ticks").map(UpdateTime.apply),
      Sub.every[IO](60.second, "refresh-ticks").map(_ => RefreshData),
    )

  def router: Location => Msg =
    case loc: Location.Internal =>
      loc.pathName match
        case s"/blcs/$page" => ToPage(BlcModel(page = page.toInt))
        case s"/txs/$page" => ToPage(TxModel(page = page.toInt))
        case s"/nfts/$page" => ToPage(NftModel(page = page.toInt))
        case s"/accs/$page" => ToPage(AccModel(page = page.toInt))
        case s"/tx/$hash" => ToPage(TxDetailModel(txDetail = TxDetail(hash = Some(hash))))
        case s"/nft/$id/$page" => ToPage(NftTokenModel(page = page.toInt, id = id))
        case s"/nft/$id" => ToPage(NftDetailModel(nftDetail = NftDetail(nftFile = Some(NftFileModel(tokenId = Some(id))))))
        case s"/blc/$hash" => 
          val p = loc.search.getOrElse("").split("=").last
          ToPage(BlcDetailModel(blcDetail = BlockDetail(hash = Some(hash)), page = p.toInt))
        case s"/acc/$address" => 
          val p = loc.search.getOrElse("").split("=").last
          ToPage(AccDetailModel(accDetail = AccountDetail(address = Some(address)), page = p.toInt))
        case "/" => ToPage(BaseModel())
        case "" => ToPage(BaseModel())
        case _   => ToPage(ErrorModel(error = ""))
    case loc: Location.External =>
      NavigateToUrl(loc.href)

package io.leisuremeta.chain.lmscan.frontend

import cats.effect.IO
import tyrian.*
import tyrian.Html.*
import scala.scalajs.js.annotation.*
import io.leisuremeta.chain.lmscan.common.model._

@JSExportTopLevel("LmScan")
object LmscanFrontendApp extends TyrianIOApp[Msg, Model]:
  def init(flags: Map[String, String]): (Model, Cmd[IO, Msg]) = (BaseModel(), Cmd.None)

  def update(model: Model): Msg => (Model, Cmd[IO, Msg]) = model match
    case m: BaseModel => Update.update(m)
    case m: TxDetailModel => TxDetailPage.update(m)
    case m: BlcDetailModel => BlockDetailPage.update(m)
    case m: AccDetailModel => AccountDetailPage.update(m)
    case m: NftDetailModel => NftDetailPage.update(m)

  def view(model: Model): Html[Msg] = model.view

  def subscriptions(model: Model): Sub[IO, Msg] =
    SearchView.detectSearch

  def router: Location => Msg =
    case loc: Location.Internal =>
      loc.pathName match
        case "/dashboard" => RouterMsg.NavigateTo(MainPage)
        case s"/chart/$t" => t match
          case "account" => RouterMsg.NavigateTo(TotalAcChart)
          case "balance" => RouterMsg.NavigateTo(TotalBalChart)
          case _ => RouterMsg.NavigateTo(TotalTxChart)
        case s"/blocks/$page" => RouterMsg.NavigateTo(BlockPage(page.toInt))
        case s"/txs/$page" => RouterMsg.NavigateTo(TxPage(page.toInt))
        case s"/nfts/$page" => RouterMsg.NavigateTo(NftPage(page.toInt))
        case s"/accounts/$page" => RouterMsg.NavigateTo(AccountPage(page.toInt))
        case s"/tx/$hash" => RouterMsg.ToDetail(TxDetailModel(txDetail = TxDetail(hash = Some(hash))))
        case s"/nft/$id/$page" => RouterMsg.NavigateTo(NftTokenPage(id, page.toInt))
        case s"/nft/$id" => RouterMsg.ToDetail(NftDetailModel(nftDetail = NftDetail(nftFile = Some(NftFileModel(tokenId = Some(id))))))
        case s"/blc/$hash" => RouterMsg.ToDetail(BlcDetailModel(blcDetail = BlockDetail(hash = Some(hash))))
        case s"/acc/$address" => RouterMsg.ToDetail(AccDetailModel(accDetail = AccountDetail(address = Some(address))))
        case "/" => RouterMsg.NavigateTo(MainPage)
        case "" => RouterMsg.NavigateTo(MainPage)
        case _   => RouterMsg.NoOp
    case loc: Location.External =>
      RouterMsg.NavigateToUrl(loc.href)

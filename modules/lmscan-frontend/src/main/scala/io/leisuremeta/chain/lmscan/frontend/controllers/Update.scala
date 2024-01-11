package io.leisuremeta.chain.lmscan
package frontend

import tyrian.*
import cats.effect.IO
import io.leisuremeta.chain.lmscan.common.model._

object Update:
  def update(model: BaseModel): Msg => (Model, Cmd[IO, Msg]) =
    case routerMsg: RouterMsg   => routerMsg match
      case RouterMsg.NavigateTo(page) => page.update(model.copy(page = page))(routerMsg)
      case RouterMsg.ToDetail(m) => m match 
        case d: TxDetailModel => TxDetailPage.update(d)(Init)
        case d: BlcDetailModel => BlockDetailPage.update(d)(Init)
        case d: AccDetailModel => AccountDetailPage.update(d)(Init)
        case d: NftDetailModel => NftDetailPage.update(d)(Init)
      case RouterMsg.NavigateToUrl(url) => (model, Nav.loadUrl(url))
      case _ => (model, Cmd.None)
    case PopupMsg(v)     => (model.copy(global = model.global.copy(popup = v)), Cmd.None)
    case ErrorMsg => (model.copy(page = ErrorPage), Cmd.None)
    case NoneMsg => (model, Cmd.None)

    case GlobalInput(s) => (model.copy(global = model.global.copy(searchValue = s)), Cmd.None)
    case GlobalSearch => 
      (model, DataProcess.globalSearch(model.global.searchValue))

    case UpdateBlockPage(page: Int) =>
      (
        model.copy(blcPage = BlockModel(page = page)),
        Cmd.Batch(
          DataProcess.getData(BlockModel(page = page)),
          Nav.pushUrl(s"/blocks/$page"),
        )
      )
    case UpdateTxPage(page: Int) =>
      (
        model.copy(txPage = TxModel(page = page)), 
        Cmd.Batch(
          DataProcess.getData(TxModel(page = page)),
          Nav.pushUrl(s"/txs/$page"),
        )
      )
    case UpdateNftPage(page: Int) =>
      (
        model.copy(nftPage = NftModel(page = page)), 
        Cmd.Batch(
          DataProcess.getData(NftModel(page = page)),
          Nav.pushUrl(s"/nfts/$page"),
        )
      )
    case UpdateNftTokenPage(id: String, page: Int) =>
      (
        model.copy(nftTokenPage = NftTokenModel(id = id, page = page)), 
        Cmd.Batch(
          DataProcess.getData(NftTokenModel(id = id, page = page)),
          Nav.pushUrl(s"/nft/$id/$page"),
        )
      )
    case UpdateAccPage(page: Int) =>
      (
        model.copy(accPage = AccModel(page = page)), 
        Cmd.Batch(
          DataProcess.getData(AccModel(page = page)),
          Nav.pushUrl(s"/accounts/$page"),
        )
      )
    case UpdateSummary => (model, DataProcess.getData(model.summary))
    case UpdateChart => (model, DataProcess.getData(model.chartData))
    case UpdateChartAll => (model, DataProcess.getDataAll(model.chartData))

    case UpdateSearch(v: Int) => model.page match
      case _: BlockPage => (model.copy(blcPage = model.blcPage.copy(searchPage = v)), Cmd.None)
      case _: TxPage => (model.copy(txPage = model.txPage.copy(searchPage = v)), Cmd.None)
      case _: AccountPage => (model.copy(accPage = model.accPage.copy(searchPage = v)), Cmd.None)
      case _: NftPage => (model.copy(nftPage = model.nftPage.copy(searchPage = v)), Cmd.None)
      case _: NftTokenPage => (model.copy(nftTokenPage = model.nftTokenPage.copy(searchPage = v)), Cmd.None)
      case _ => (model, Cmd.None)

    case UpdateModel(v: ApiModel) => v match
      case v: TxList => (model.copy(txPage = model.txPage.copy(list = Some(v))), Cmd.None)
      case v: BlcList => (model.copy(blcPage = model.blcPage.copy(list = Some(v))), Cmd.None)
      case v: NftList => (model.copy(nftPage = model.nftPage.copy(list = Some(v))), Cmd.None)
      case v: AccList => (model.copy(accPage = model.accPage.copy(list = Some(v))), Cmd.None)
      case v: NftTokenList => (model.copy(nftTokenPage = model.nftTokenPage.copy(list = Some(v))), Cmd.None)
      case v: SummaryBoard => (model.copy(summary = v), Cmd.None)
      case v: SummaryChart => (model.copy(chartData = v), Cmd.None)
      case v: TxDetail => (TxDetailModel(txDetail = v), Nav.pushUrl(s"/tx/${v.hash}"))
      case v: BlockDetail => (BlcDetailModel(blcDetail = v), Nav.pushUrl(s"/blc/${v.hash.get}"))
      case v: AccountDetail => (AccDetailModel(accDetail = v), Nav.pushUrl(s"/acc/${v.address.get}"))
      case v: NftDetail => (NftDetailModel(nftDetail = v), Nav.pushUrl(s"/nft/${v.nftFile.get.tokenId.get}"))
      // case v: => (model.copy(nftDetail = v), Nav.pushUrl(model.page.url))
    
    case _ => (model, Cmd.None)

package io.leisuremeta.chain.lmscan
package frontend

import tyrian.*
import cats.effect.IO
import io.leisuremeta.chain.lmscan.common.model._

object Update:
  def update(model: Model): Msg => (Model, Cmd[IO, Msg]) =
    case routerMsg: RouterMsg   => routerMsg match
      case RouterMsg.NavigateTo(page) => page.update(model.copy(page = page))(routerMsg)
      case RouterMsg.NavigateToUrl(url) => (model, Nav.loadUrl(url))
      case _ => (model, Cmd.None)
    case PopupMsg(v)     => (model.copy(popup = v), Cmd.None)
    case ErrorMsg => (model, Cmd.None)
    case NoneMsg => (model, Cmd.None)

    case GlobalInput(s) => (model.copy(searchValue = s), Cmd.None)
    case GlobalSearch => 
      (model, DataProcess.globalSearch(model.searchValue))

    case UpdateAccDetailPage(address) => (model, Cmd.Batch(DataProcess.getData(AccountDetail(address = Some(address)))))
    case UpdateNftDetailPage(tokenId) => (model, Cmd.Batch(DataProcess.getData(NftFileModel(tokenId = Some(tokenId)))))
    case UpdateTxDetailPage(hash) => (model, DataProcess.getData(TxDetail(hash = Some(hash))))
    case UpdateBlcDetailPage(hash) => (model, DataProcess.getData(BlockDetail(hash = Some(hash))))
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
    case UpdateSummary => (model, DataProcess.getData(model.summary))
    case UpdateChart => (model, DataProcess.getData(model.chartData))

    case UpdateBlcsSearch(v: Int) => (model.copy(blcPage = model.blcPage.copy(searchPage = v)), Cmd.None)
    case UpdateTxsSearch(v: Int) => (model.copy(txPage = model.txPage.copy(searchPage = v)), Cmd.None)
    case PageMsg.UpdateTx(value: TxList) => (model.copy(txPage = model.txPage.copy(list = Some(value))), Cmd.None)
    case PageMsg.UpdateBlc(value: BlcList) => (model.copy(blcPage = model.blcPage.copy(list = Some(value))), Cmd.None)
    case PageMsg.Update1(value: SummaryModel) => (model.copy(summary = value), Cmd.None)
    case PageMsg.UpdateChart(value: List[SummaryModel]) => (model.copy(chartData = value.reverse), Cmd.None)
    case PageMsg.UpdateTxDetail(v: TxDetail) => (model.copy(txDetail = v), Nav.pushUrl(s"/tx/${v.hash.getOrElse("")}"))
    case PageMsg.UpdateBlcDetail(v: BlockDetail) =>
      (model.copy(blcDetail = v), Nav.pushUrl(s"/block/${v.hash.getOrElse("")}"))
    case PageMsg.UpdateAccDetail(v: AccountDetail) =>
      (model.copy(accDetail = v), Nav.pushUrl(s"/account/${v.address.getOrElse("")}"))
    case PageMsg.UpdateNftDetail(v: NftDetail) =>
      (model.copy(nftDetail = v), Nav.pushUrl(s"/nft/${v.nftFile.getOrElse(NftFileModel()).tokenId.getOrElse("")}"))

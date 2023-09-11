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
    case NotFoundMsg() => 
      println("erorr ehre!") 
      (model, Cmd.None)
    case ErrorMsg => (model.copy(page = ErrorPage), Cmd.None)
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
      case v: SummaryModel => (model.copy(summary = v), Cmd.None)
      case v: SummaryChart => (model.copy(chartData = v), Cmd.None)
      case v: TxDetail => (model.copy(txDetail = v), Nav.pushUrl(s"/tx/${v.hash.getOrElse("")}"))
      case v: BlockDetail => (model.copy(blcDetail = v), Nav.pushUrl(s"/block/${v.hash.getOrElse("")}"))
      case v: AccountDetail => (model.copy(accDetail = v), Nav.pushUrl(s"/account/${v.address.getOrElse("")}"))
      case v: NftDetail => (model.copy(nftDetail = v), Nav.pushUrl(s"/nft/${v.nftFile.getOrElse(NftFileModel()).tokenId.getOrElse("")}"))

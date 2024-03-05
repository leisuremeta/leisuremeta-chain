package io.leisuremeta.chain.lmscan
package frontend

import tyrian.*
import cats.effect.IO
import tyrian.Html.*
import common.model._

object NftDetailPage:
  def update(model: NftDetailModel): Msg => (Model, Cmd[IO, Msg]) =
    case Init => (model, DataProcess.getData(model.nftDetail.nftFile.get))
    case RefreshData => (model, Cmd.None)
    case UpdateDetailPage(d: NftDetail) => (model, DataProcess.getData(d.nftFile.get))
    case UpdateModel(v: NftDetail) => (NftDetailModel(nftDetail = v), Nav.pushUrl(model.url))
    case msg: GlobalMsg => (model.copy(global = model.global.update(msg)), Cmd.None)
    case msg => (model.toEmptyModel, Cmd.emit(msg))

  def view(model: NftDetailModel): Html[Msg] =
    DefaultLayout.view(
      model,
      List(
        NftDetailTable.view(model.nftDetail),
        Table.view(model)
      ),
    )

final case class NftDetailModel(
    global: GlobalModel = GlobalModel(),
    nftDetail: NftDetail = NftDetail(),
) extends Model:
    def view: Html[Msg] = NftDetailPage.view(this)
    def url = s"/nft/${nftDetail.nftFile.get.tokenId.get}"
    def update: Msg => (Model, Cmd[IO, Msg]) = NftDetailPage.update(this)

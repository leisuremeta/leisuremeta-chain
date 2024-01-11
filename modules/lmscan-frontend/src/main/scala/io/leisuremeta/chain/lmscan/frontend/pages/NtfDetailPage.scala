package io.leisuremeta.chain.lmscan
package frontend

import tyrian.*
import cats.effect.IO
import tyrian.Html.*
import common.model._

object NftDetailPage:
  def update(model: NftDetailModel): Msg => (Model, Cmd[IO, Msg]) =
    case Init => (model, DataProcess.getData(model.nftDetail.nftFile.get))
    case UpdateDetailPage(detail) => detail match
      case d: NftDetail => (model, DataProcess.getData(d.nftFile.get))
    case UpdateModel(v: ApiModel) => v match
      case v: NftDetail => (NftDetailModel(nftDetail = v), Nav.pushUrl(model.url))
    case GlobalInput(s) => (model.copy(global = model.global.updateSearchValue(s)), Cmd.None)
    case msg => (BaseModel(global = model.global), Cmd.emit(msg))

  def view(model: NftDetailModel): Html[Msg] =
    DefaultLayout.view(
      model,
      div(`class` := "pb-32px pt-16px color-white")(
        NftDetailTable.view(model.nftDetail),
        Table.view(model.nftDetail)
      ),
    )

final case class NftDetailModel(
    global: GlobalModel = GlobalModel(),
    nftDetail: NftDetail = NftDetail(),
) extends Model:
    def view: Html[Msg] = NftDetailPage.view(this)
    def url = s"/nft/${nftDetail.nftFile.get.tokenId.get}"

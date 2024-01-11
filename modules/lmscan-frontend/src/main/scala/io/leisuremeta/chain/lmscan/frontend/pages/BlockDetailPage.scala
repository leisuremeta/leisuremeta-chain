package io.leisuremeta.chain.lmscan
package frontend

import tyrian.*
import cats.effect.IO
import tyrian.Html.*
import common.model._

object BlockDetailPage:
  def update(model: BlcDetailModel): Msg => (Model, Cmd[IO, Msg]) =
    case Init => (model, DataProcess.getData(model.blcDetail))
    case UpdateDetailPage(detail) => detail match
      case d: BlockDetail => (model, DataProcess.getData(d))
    case UpdateModel(v: ApiModel) => v match
      case v: BlockDetail => (BlcDetailModel(blcDetail = v), Nav.pushUrl(model.url))
    case GlobalInput(s) => (model.copy(global = model.global.updateSearchValue(s)), Cmd.None)
    case msg => (BaseModel(global = model.global), Cmd.emit(msg))

  def view(model: BlcDetailModel): Html[Msg] =
    DefaultLayout.view(
      model,
      div(`class` := "pb-32px")(
        div(
          `class` := "font-40px pt-16px font-block-detail pb-16px color-white",
        )(
          "Block Details",
        ),
        BlockDetailTable.view(model.blcDetail),
        Table.view(model.blcDetail)
      ),
    )

final case class BlcDetailModel(
    global: GlobalModel = GlobalModel(),
    blcDetail: BlockDetail = BlockDetail(),
) extends Model:
    def view: Html[Msg] = BlockDetailPage.view(this)
    def url = s"/blc/${blcDetail.hash.get}"

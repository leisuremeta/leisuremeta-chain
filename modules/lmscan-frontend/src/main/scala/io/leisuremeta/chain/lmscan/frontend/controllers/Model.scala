package io.leisuremeta.chain.lmscan
package frontend

import common.model.*
import tyrian._
import tyrian.Html.div
import cats.effect.IO
import org.jline.console.CmdLine

final case class GlobalModel(
    popup: Boolean = false,
    searchValue: String = "",
):
  def updateSearchValue(s: String) = GlobalModel(popup, s)

trait Model:
  val global: GlobalModel
  def view: Html[Msg]
  def url: String
  def update: Msg => (Model, Cmd[IO, Msg])
  def toEmptyModel: EmptyModel = EmptyModel(global)

trait PageModel extends Model with ApiModel:
  val page: Int
  val size: Int = 20
  val searchPage: Int
  val data: Option[ApiModel]

case class EmptyModel(
  global: GlobalModel = GlobalModel(),
) extends Model:
  def view = div("")
  def url = ""
  def update: Msg => (Model, Cmd[IO, Msg]) =
    case ToPage(model) => model.update(Init)
    case NavigateToUrl(url) => (this, Nav.loadUrl(url))
    case ErrorMsg => (ErrorModel(error = ""), Cmd.None)
    case GlobalSearch => (this, Cmd.Emit(DataProcess.globalSearch(global.searchValue)))
    case _ => (this, Cmd.None)

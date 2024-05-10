package io.leisuremeta.chain.lmscan
package frontend

import common.model.*
import tyrian._
import cats.effect.IO
import scalajs.js
import io.circe.Decoder

final case class GlobalModel(
    popup: Boolean = false,
    searchValue: String = "",
    current: js.Date = new js.Date(),
):
  def update(msg: GlobalMsg): GlobalModel = msg match 
    case GlobalInput(s) => this.copy(searchValue = s)
    case UpdateTime(t) => this.copy(current = t)

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
  val pageToggle: Boolean

case class EmptyModel(
  global: GlobalModel = GlobalModel(),
) extends Model:
  def view = DefaultLayout.view(
      this,
      LoaderView.view
    )
  def url = ""
  def update: Msg => (Model, Cmd[IO, Msg]) =
    case ToPage(model) => model.update(Init)
    case NavigateToUrl(url) => (this, Nav.loadUrl(url))
    case ErrorMsg => (ErrorModel(error = ""), Cmd.None)
    case GlobalSearch => (this, DataProcess.globalSearch(global.searchValue.toLowerCase))
    case GlobalSearchResult(v) => (v, Nav.pushUrl(v.url))
    case _ => (this, Cmd.None)

case class IssueInfo(date: String, n: Int)
given Decoder[IssueInfo] =
  Decoder.forProduct2(
    "Issue_date",
    "Issuance"
  )(IssueInfo.apply)

case class NftJson(
  creatorDesc: String,
  collectionDesc: String,
  rarity: String,
  checksum: String,
  issue: IssueInfo,
  collection: String,
  creator: String,
  name: String,
  uri: String,
)
given Decoder[NftJson] =
  Decoder.forProduct9(
    "Creator_description",
    "Collection_description",
    "Rarity",
    "NFT_checksum",
    "Issuance_info",
    "Collection_name",
    "Creator",
    "NFT_name",
    "NFT_URI"
  )(NftJson.apply)

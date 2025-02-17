package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import io.leisuremeta.chain.lmscan.common.model.*
import org.scalajs.dom._
import cats.effect.IO

object Pagination:
  def toInt(s: String) =
    try Some(Integer.parseInt(s))
    catch case _ => None
  def checkAndMake(s: String, last: Int, cur: Int) =
    val v = s.toIntOption.getOrElse(cur)
    if v < 1 then 1
    else if v > last then last
    else v
  def view[T](model: PageModel) =
    val curPage = model.page
    val totalPage = model.data match
      case None    => 0
      case Some(v) => v match
        case PageResponse(totalCount, totalPages, payload) => totalPages.toInt

    def goTo(v: Int) = model match
      case _: BlcModel => ToPage(BlcModel(page = v))
      case _: TxModel => ToPage(TxModel(page = v))
      case _: AccModel => ToPage(AccModel(page = v))
      case _: NftModel => ToPage(NftModel(page = v))
      case n: BlcDetailModel=> ToPage(n.copy(page = v))
      case n: AccDetailModel=> ToPage(n.copy(page = v))
      case n: NftTokenModel => ToPage(n.copy(page = v))
      case n: VdDetailModel => ToPage(n.copy(page = v))
    def isDis(condition: Boolean) = if condition then "dis" else ""
    def toggleInput = TogglePageInput(!model.pageToggle)

    div(cls := s"table-search")(
      a(
        cls := s"${isDis(1 == curPage)}",
        onClick(goTo(1)),
      )("First"),
      a(
        cls := s"${isDis(curPage <= 1)}",
        onClick(goTo(curPage - 1)),
      )("<"),
      model.pageToggle match
        case true => 
          input(
            id := "list-search",
            onInput(s => UpdateSearch(checkAndMake(s, totalPage, curPage))),
            value := s"${curPage}",
            cls := "type-search",
          )
        case false => p(onClick(toggleInput))(s"${curPage} of ${totalPage}")
      ,
      a(
        cls := s"${isDis(curPage >= totalPage)}",
        onClick(goTo(curPage + 1)),
      )(">"),
      a(
        cls := s"${isDis(curPage == totalPage)}",
        onClick(goTo(totalPage)),
      )("Last"),
    )
  
  def detectSearch = 
    Sub.Batch(
      Option(document.getElementById("list-search")) match
        case None => Sub.None
        case Some(el) =>
          Sub.fromEvent[IO, KeyboardEvent, Msg]("keyup", el): e =>
            e.key match
              case "Enter" => Some(ListSearch)
              case _ => None
      ,
    )

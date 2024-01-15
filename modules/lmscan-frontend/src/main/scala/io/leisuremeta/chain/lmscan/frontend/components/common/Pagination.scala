package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import io.leisuremeta.chain.lmscan.common.model._
import io.leisuremeta.chain.lmscan.frontend.V.plainStr
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

    val btnFistPage = curPage match
      case x if (x <= 2)               => 1
      case x if (x >= (totalPage - 1)) => totalPage - 4
      case x                           => (curPage - 2)
    val btnLastPage = Math.min(totalPage + 1, btnFistPage + 5)
    def goTo(v: Int) = model match
      case _: BlcModel => ToPage(BlcModel(page = v))
      case _: TxModel => ToPage(TxModel(page = v))
      case _: AccModel => ToPage(AccModel(page = v))
      case _: NftModel => ToPage(NftModel(page = v))
      case n: NftTokenModel => ToPage(n.copy(page = v))

    div(
      `class` := s"_search table-search xy-center",
    )(
      div(`class` := "xy-center")(
        div(
          `class` := s"type-arrow",
          curPage match
            case 1 => style(Style("color" -> "gray"))
            case _ => onClick(goTo(1)),
        )("<<"),
        div(
          `class` := s"type-arrow",
          curPage <= 10 match
            case true  => style(Style("color" -> "gray"))
            case false => onClick(goTo(curPage - 10)),
        )("<"),
        div(`class` := s"type-text-btn")(
          List
            .range(btnFistPage, btnLastPage)
            .map(idx =>
              span(
                `class` := (if curPage == idx then "selected" else ""),
                onClick(goTo(idx)),
              )(idx.toString()),
            ),
        ),
        div(
          `class` := s"type-arrow",
          curPage >= totalPage - 10 match
            case true  => style(Style("color" -> "gray"))
            case false => onClick(goTo(curPage + 10)),
        )(">"),
        div(
          `class` := s"type-arrow",
          curPage == totalPage match
            case true => style(Style("color" -> "gray"))
            case _    => onClick(goTo(totalPage)),
        )(">>"),
        div(
          style(Style("margin-left" -> "10px")),
        )(
          input(
            id := "list-search",
            onInput(s => UpdateSearch(checkAndMake(s, totalPage, curPage))),
            value := s"${curPage}",
            `class` := "type-search xy-center margin-right text-center",
          ),
          div(`class` := "type-plain-text margin-right")("of"),
          div(`class` := "type-plain-text margin-right")(totalPage.toString),
        ),
      ),
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

package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*
import Dom._selectedPage
import io.leisuremeta.chain.lmscan.common.model._
import io.leisuremeta.chain.lmscan.frontend.V.plainStr

object Pagination:
  def toInt(s: String) =
    try Some(Integer.parseInt(s))
    catch case _ => None
  def checkAndMake(v: Int, last: Int) =
    if v < 1 then 1
    else if v > last then last
    else v
  def view[T](model: ListPage[T]) =
    val curPage = model.page
    val totalPage = model.list match
      case None    => 0
      case Some(v) => v.totalPages.toInt

    val btnFistPage = curPage match
      case x if (x <= 2)               => 1
      case x if (x >= (totalPage - 1)) => totalPage - 4
      case x                           => (curPage - 2)
    val btnLastPage = btnFistPage + 5
    def goTo(v: Int) = model match
      case _: BlockModel => UpdateBlockPage(v)
      case _: TxModel => UpdateTxPage(v)
      case _: NftModel => UpdateNftPage(v)
      case n: NftTokenModel => UpdateNftTokenPage(n.id, v)

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
                `class` := _selectedPage(curPage, idx),
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
            onInput(s =>
              toInt(s) match
                case Some(v) => goTo(checkAndMake(v, totalPage))
                case None    => NoneMsg,
            ),
            onKeyUp(e =>
              e.key match
                case "Enter" => goTo(model.searchPage)
                case _       => NoneMsg,
            ),
            value := s"${curPage}",
            `class` := "type-search xy-center DOM-page1 margin-right text-center",
          ),
          div(`class` := "type-plain-text margin-right")("of"),
          div(`class` := "type-plain-text margin-right")(totalPage.toString),
        ),
      ),
    )

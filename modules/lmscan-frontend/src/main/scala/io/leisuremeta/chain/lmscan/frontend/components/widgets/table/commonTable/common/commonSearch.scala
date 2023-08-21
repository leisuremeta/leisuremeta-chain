package io.leisuremeta.chain.lmscan.frontend
import scala.util.chaining.*
import tyrian.Html.*
import tyrian.*
import Dom.{_hidden, timeAgo, yyyy_mm_dd_time, _selectedPage}
import io.leisuremeta.chain.lmscan.common.model.SummaryModel
import io.leisuremeta.chain.lmscan.frontend.V.plainStr
import io.leisuremeta.chain.lmscan.frontend.StateCasePipe.*
import io.leisuremeta.chain.lmscan.frontend.ModelPipe.*
import io.leisuremeta.chain.lmscan.frontend.PageCasePipe.*
import io.leisuremeta.chain.lmscan.frontend.PupCasePipe.*

object Search:
  val search_block = (model: Model) =>

    // todo :: make as pipe
    val curPage = find_current_PubPage(model)

    val totalPage = model.block_total_page.toInt

    val btnFistPage = curPage match
      case x if (x == 1 || x == 2) => 1
      case x
          if (x == limit_value(totalPage.toString())) || (x == (limit_value(
            totalPage.toString(),
          ) - 1)) => (
        limit_value(totalPage.toString()) - 4
      )
      case x => (curPage - 2)

    val btnLastPage = btnFistPage + 5

    div(
      // TODO :: fix class name => fix css
      `class` := s"state DashBoard _search table-search xy-center ",
    )(
      div(`class` := "xy-center")(
        div(
          `class` := s"type-arrow",
          curPage == 1 match
            case true =>
              style(Style("color" -> "lightgray"))
            case false =>
              onClick(
                PageMsg.PreUpdate(
                  Blocks(
                    url = s"blocks/${1}",
                    pubs = List(PubCase.BlockPub(page = 1)),
                  ),
                ),
              ),
        )("<<"),
        div(
          `class` := s"type-arrow",
          curPage <= 10 match
            case true =>
              style(Style("color" -> "lightgray"))
            case false =>
              onClick(
                PageMsg.PreUpdate(
                  Blocks(
                    url = s"blocks/${curPage - 10}",
                    pubs = List(PubCase.BlockPub(page = curPage - 10)),
                  ),
                ),
              ),
        )("<"),
        div(`class` := s"type-text-btn")(
          List
            .range(btnFistPage, btnLastPage)
            .map(idx =>
              span(
                `class` := s"${_selectedPage[Int](curPage, idx)}",
                onClick(
                  (limit_value(
                    idx.toString(),
                  ) == 50000 && block_validPageNumber(
                    model,
                  ) == 50000) match
                    case true =>
                      PopupMsg.OnClick(true)
                    case false =>
                      PageMsg.PreUpdate(
                        Blocks(
                          url = s"blocks/${limit_value(idx.toString())}",
                          pubs = List(
                            PubCase.BlockPub(page = limit_value(idx.toString())),
                          ),
                        ),
                      ),
                ),
              )(idx.toString()),
            ),
        ),
        div(
          `class` := s"type-arrow",
          curPage >= (limit_value(totalPage.toString()) - 10) match
            case true =>
              style(Style("color" -> "gray"))
            case false =>
              onClick(
                (limit_value(
                  (curPage + 10).toString(),
                ) == 50000 && block_validPageNumber(
                  model,
                ) == 50000) match
                  case true =>
                    PopupMsg.OnClick(true)
                  case false =>
                    PageMsg.PreUpdate(
                      Blocks(
                        url =
                          s"blocks/${limit_value((curPage + 10).toString())}",
                        pubs = List(
                          PubCase.BlockPub(page =
                            limit_value((curPage + 10).toString()),
                          ),
                        ),
                      ),
                    ),
              ),
        )(">"),
        div(
          `class` := s"type-arrow",
          (limit_value(
            totalPage.toString(),
          ) == 50000 && block_validPageNumber(
            model,
          ) == 50000) match
            case true =>
              style(Style("color" -> "gray"))
            case false =>
              onClick(
                (limit_value(
                  totalPage.toString(),
                ) == 50000 && block_validPageNumber(
                  model,
                ) == 50000) match
                  case true =>
                    PopupMsg.OnClick(true)
                  case false =>
                    PageMsg.PreUpdate(
                      Blocks(
                        url = s"blocks/${limit_value(totalPage.toString())}",
                        pubs = List(
                          PubCase.BlockPub(page =
                            limit_value(totalPage.toString()),
                          ),
                        ),
                      ),
                    ),
              ),
        )(">>"),
        div(
          style(Style("margin-left" -> "10px")),
        )(
          input(
            onInput(s => PageMsg.GetFromBlockSearch(s)),
            onKeyUp(e =>
              e.key match
                case "Enter" =>
                  block_validPageNumber(model) == 50000 match
                    case true =>
                      PopupMsg.OnClick(true)
                    case false =>
                      PageMsg.PatchFromBlockSearch(
                        block_validPageNumber(model).toString(),
                      )

                case _ => PageMsg.None,
            ),
            value := s"${model.block_current_page}",
            `class` := "type-search xy-center DOM-page1 margin-right text-center",
          ),
          div(`class` := "type-plain-text margin-right")("of"),
          div(`class` := "type-plain-text margin-right")({
            limit_value(model.block_total_page).toString()
          }),
        ),
      ),
    )
    // )
  val search_tx = (model: Model) =>

    // todo :: make as pipe
    val curPage = find_current_PubPage(model)

    val totalPage = model.tx_total_page.toInt

    val btnFistPage = curPage match
      case x if (x == 1 || x == 2) => 1
      case x
          if (x == limit_value(totalPage.toString())) || (x == (limit_value(
            totalPage.toString(),
          ) - 1)) => (
        limit_value(totalPage.toString()) - 4
      )
      case x => (curPage - 2)

    val btnLastPage = btnFistPage + 5

    div(
      // TODO :: fix class name => fix css
      `class` := s"state DashBoard _search table-search xy-center ",
    )(
      div(`class` := "xy-center")(
        div(
          `class` := s"type-arrow",
          curPage == 1 match
            case true =>
              style(Style("color" -> "lightgray"))
            case false =>
              onClick(
                PageMsg.PreUpdate(
                  Transactions(
                    url = s"transactions/${1}",
                    pubs = List(
                      PubCase.TxPub(page = 1, subtype = model.subtype),
                    ),
                  ),
                ),
              ),
        )("<<"),
        div(
          `class` := s"type-arrow",
          curPage <= 10 match
            case true =>
              style(Style("color" -> "lightgray"))
            case false =>
              onClick(
                PageMsg.PreUpdate(
                  Transactions(
                    url = s"transactions/${curPage - 10}",
                    pubs = List(
                      PubCase.TxPub(
                        page = curPage - 10,
                        subtype = model.subtype,
                      ),
                    ),
                  ),
                ),
              ),
        )("<"),
        // AccountDetail 일때는 현재페이지 정보를 복사한뒤, page 만 바꿔준다
        // 현재페이지 가져온다 |> 현재페이지의 pubs 의 page 만 업데이트 한뒤 |> 페이지를 리턴한다

        div(`class` := s"type-text-btn")(
          List
            .range(btnFistPage, btnLastPage)
            .map(idx =>
              span(
                `class` := s"${_selectedPage[Int](curPage, idx)}",
                onClick(
                  (limit_value(
                    idx.toString(),
                  ) == 50000 && tx_validPageNumber(
                    model,
                  ) == 50000) match
                    case true =>
                      PopupMsg.OnClick(true)
                    case false =>
                      PageMsg.PreUpdate(
                        Transactions(
                          url = s"transactions/${limit_value(idx.toString())}",
                          pubs = List(
                            PubCase.TxPub(
                              page = limit_value(idx.toString()),
                              subtype = model.subtype,
                            ),
                          ),
                        ),
                      ),
                ),
              )(idx.toString()),
            ),
        ),
        div(
          `class` := s"type-arrow",
          curPage >= (limit_value(totalPage.toString()) - 10) match
            case true =>
              style(Style("color" -> "gray"))
            case false =>
              onClick(
                (limit_value(
                  (curPage + 10).toString(),
                ) == 50000 && tx_validPageNumber(
                  model,
                ) == 50000) match
                  case true =>
                    PopupMsg.OnClick(true)
                  case false =>
                    PageMsg.PreUpdate(
                      Transactions(
                        url =
                          s"transactions/${limit_value((curPage + 10).toString())}",
                        pubs = List(
                          PubCase.TxPub(
                            page = limit_value((curPage + 10).toString()),
                            subtype = model.subtype,
                          ),
                        ),
                      ),
                    ),
              ),
        )(">"),
        div(
          `class` := s"type-arrow",
          (limit_value(totalPage.toString()) == 50000 && tx_validPageNumber(
            model,
          ) == 50000) match
            case true => style(Style("color" -> "gray"))
            case false =>
              onClick(
                (limit_value(
                  totalPage.toString(),
                ) == 50000 && tx_validPageNumber(
                  model,
                ) == 50000) match
                  case true =>
                    PopupMsg.OnClick(true)
                  case false =>
                    PageMsg.PreUpdate(
                      Transactions(
                        url =
                          s"transactions/${limit_value(totalPage.toString())}",
                        pubs = List(
                          PubCase.TxPub(
                            page = limit_value(totalPage.toString()),
                            subtype = model.subtype,
                          ),
                        ),
                      ),
                    ),
              ),
        )(">>"),
        div(
          style(Style("margin-left" -> "10px")),
        )(
          input(
            onInput(s => PageMsg.GetFromTxSearch(s)),
            onKeyUp(e =>
              e.key match
                case "Enter" =>
                  tx_validPageNumber(model) == 50000 match
                    case true =>
                      PopupMsg.OnClick(true)
                    case false =>
                      PageMsg.PatchFromTxSearch(
                        tx_validPageNumber(model).toString(),
                      )

                case _ => PageMsg.None,
            ),
            value := s"${model.tx_current_page}",
            `class` := "type-search xy-center DOM-page1 margin-right text-center",
          ),
          div(`class` := "type-plain-text margin-right")("of"),
          div(`class` := "type-plain-text margin-right")({
            limit_value(model.tx_total_page).toString()
          }),
        ),
      ),
    )

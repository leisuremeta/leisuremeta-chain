package io.leisuremeta.chain.lmscan.frontend
import scala.util.chaining.*
import tyrian.Html.*
import tyrian.*
import Dom.{_hidden, timeAgo, yyyy_mm_dd_time, _selectedPage}
import io.leisuremeta.chain.lmscan.frontend.Builder.*
import io.leisuremeta.chain.lmscan.frontend.Log.log
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

    val totalPage = getPubData(model).block.totalPages

    val btnFistPage = curPage match
      case x if (x == 1 || x == 2)                         => 1
      case x if (x == totalPage) || (x == (totalPage - 1)) => (totalPage - 4)
      case x                                               => (curPage - 2)

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
                  PageCase.Blocks(
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
                  PageCase.Blocks(
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
                  PageMsg.PreUpdate(
                    PageCase.Blocks(
                      url = s"blocks/${idx}",
                      pubs = List(PubCase.BlockPub(page = idx)),
                    ),
                  ),
                ),
              )(idx.toString()),
            ),
        ),
        div(
          `class` := s"type-arrow",
          curPage >= (totalPage - 10) match
            case true =>
              style(Style("color" -> "lightgray"))
            case false =>
              onClick(
                PageMsg.PreUpdate(
                  PageCase.Blocks(
                    url = s"blocks/${curPage + 10}",
                    pubs = List(PubCase.BlockPub(page = curPage + 10)),
                  ),
                ),
              ),
        )(">"),
        div(
          `class` := s"type-arrow",
          onClick(
            PageMsg.PreUpdate(
              PageCase.Blocks(
                url = s"blocks/${totalPage}",
                pubs = List(PubCase.BlockPub(page = totalPage)),
              ),
            ),
          ),
        )(">>"),
        div(
          style(Style("margin-left" -> "10px")),
        )(
          input(
            // onInput(s => PageMoveMsg.Get(s)), // todo :: input 함수 추가
            value := s"${curPage}",
            `class` := "type-search xy-center DOM-page1 margin-right text-center",
          ),
          div(`class` := "type-plain-text margin-right")("of"),
          div(`class` := "type-plain-text margin-right")(totalPage.toString()),
        ),
      ),
    )
// val s_s = 1
  val search_tx = (model: Model) =>

    // todo :: make as pipe
    val curPage = find_tx_curpage(model)

    val totalPage = getPubData(model).tx.totalPages

    val btnFistPage = curPage match
      case x if (x == 1 || x == 2)                         => 1
      case x if (x == totalPage) || (x == (totalPage - 1)) => (totalPage - 4)
      case x                                               => (curPage - 2)

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
                  PageCase.Transactions(
                    url = s"transactions/${1}",
                    pubs = List(PubCase.TxPub(page = 1)),
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
                  PageCase.Transactions(
                    url = s"transactions/${curPage - 10}",
                    pubs = List(PubCase.TxPub(page = curPage - 10)),
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
                  PageMsg.PreUpdate(
                    get_PageCase(model) match
                      case page: PageCase.Transactions =>
                        page.copy(
                          url = s"transactions/${idx}",
                          pubs = List(PubCase.TxPub(page = idx)),
                        )

                      // TODO:: replaced with values ​​received from the cache.
                      case page: PageCase.AccountDetail =>
                        page.copy(
                          url = s"account/${idx}/${page.pubs.length}",
                          pubs = List(
                            PubCase.BoardPub(1, "", SummaryModel()),
                            PubCase.AccountDetailPub(hash =
                              page.pubs.filter(pub =>
                                pub match
                                  case pub: PubCase.TxPub => true
                                  case _                  => false,
                              )(0) match
                                case pub: PubCase.TxPub => pub.accountAddr
                                case _                  => "",
                            ),
                            PubCase.TxPub(
                              accountAddr = page.pubs.filter(pub =>
                                pub match
                                  case pub: PubCase.TxPub => true
                                  case _                  => false,
                              )(0) match
                                case pub: PubCase.TxPub => pub.accountAddr
                                case _                  => ""
                              ,
                              page = idx,
                            ),
                          ),
                        )
                      case _ => get_PageCase(model),
                  ),
                ),
              )(idx.toString()),
            ),
        ),
        div(
          `class` := s"type-arrow",
          curPage >= (totalPage - 10) match
            case true =>
              style(Style("color" -> "lightgray"))
            case false =>
              onClick(
                PageMsg.PreUpdate(
                  PageCase.Transactions(
                    url = s"transactions/${curPage + 10}",
                    pubs = List(PubCase.TxPub(page = curPage + 10)),
                  ),
                ),
              ),
        )(">"),
        div(
          `class` := s"type-arrow",
          onClick(
            PageMsg.PreUpdate(
              PageCase.Transactions(
                url = s"transactions/${totalPage}",
                pubs = List(PubCase.TxPub(page = totalPage)),
              ),
            ),
          ),
        )(">>"),
        div(
          style(Style("margin-left" -> "10px")),
        )(
          input(
            // onInput(s => PageMoveMsg.Get(s)), // todo :: input 함수 추가
            value := s"${curPage}",
            `class` := "type-search xy-center DOM-page1 margin-right text-center",
          ),
          div(`class` := "type-plain-text margin-right")("of"),
          div(`class` := "type-plain-text margin-right")(totalPage.toString()),
        ),
      ),
    )

package io.leisuremeta.chain.lmscan.frontend
import cats.effect.IO
import org.scalajs.dom.window
import org.scalajs.dom.KeyboardEvent
import org.scalajs.dom.PopStateEvent
import tyrian.*
import Log.*

object Subscriptions:
  def subscriptions(model: Model): Sub[IO, Msg] =
    Sub.Batch(
      Option(Dom.select("DOM-search")) match
        // 처음 화면이 로드될때, dom 이 잡히지 않은경우 or 돔이 검색되지 않는 경우
        case None => Sub.None

        // Dom 이 select 된 경우
        case Some(element) =>
          Sub.fromEvent[IO, KeyboardEvent, Msg](
            "keydown",
            element,
          ) { e =>
            e.keyCode match
              case 13 =>
                // Enter key
                Some(InputMsg.Patch)
              case _ =>
                None
          }
      ,
      Option(Dom.select("DOM-page1")) match
        // 처음 화면이 로드될때, dom 이 잡히지 않은경우 or 돔이 검색되지 않는 경우
        case None => Sub.None

        // Dom 이 select 된 경우
        case Some(element) =>
          Sub.fromEvent[IO, KeyboardEvent, Msg](
            "keydown",
            element,
          ) { e =>
            e.keyCode match
              case 13 =>
                // Enter key
                None
              // Some("")
              case _ =>
                None
          }
      ,
      Sub.fromEvent("popstate", window) { e =>
        val state = e.asInstanceOf[PopStateEvent].state
        log("#state " + state)
        // Some(PageMsg.PreUpdate(PageCase.NoPage()))
        // log(model.observerNumber)

        // 1 보다 작을 경우 => 1로 보낸다
        // val page = state.toString().toIntOption.getOrElse(0) case
        log("model.observerNumber")
        log(model.pointer)
        Some(PageMsg.BackObserver)
        // val page: PageCase = ValidPageName.getPageFromStr(state.toString)

        // // PageMoveMsg.Get(value)
        // page match
        //   case PageName.Transactions(page1) =>
        //     Some(PageMoveMsg.Goto(page))
        //   case PageName.Blocks(page1) =>
        //     Some(PageMoveMsg.Goto(page))
        //   case _ => Some(PageMsg.PreUpdate(page))
      },
    )

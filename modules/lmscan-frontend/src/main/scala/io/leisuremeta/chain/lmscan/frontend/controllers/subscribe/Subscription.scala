package io.leisuremeta.chain.lmscan.frontend
import cats.effect.IO
import org.scalajs.dom.KeyboardEvent
import tyrian.*
import Log.*

object Subscriptions:
  def subscriptions(model: Model): Sub[IO, Msg] =
    Option(Dom.select("DOM-search")) match
      // 처음 화면이 로드될때, dom 이 잡히지 않은경우 or 돔이 검색되지 않는 경우
      case None => Sub.None

      // Dom 이 select 된 경우
      case Some(element) =>
        Sub.Batch(
          Sub.fromEvent[IO, KeyboardEvent, Msg](
            "keydown",
            element,
          ) { e =>
            e.keyCode match
              case 13 =>
                log("서치 엔터 클릭")
                // Enter key
                Some(InputMsg.Patch)
              case _ =>
                None
          },
          // TODO :: remove redundant optional dom select
          Option(Dom.select("DOM-page1")) match
            // 처음 화면이 로드될때, dom 이 잡히지 않은경우 or 돔이 검색되지 않는 경우
            case None => Sub.None

            // Dom 이 select 된 경우
            case Some(element) =>
              Sub.Batch(
                Sub.fromEvent[IO, KeyboardEvent, Msg](
                  "keydown",
                  element,
                ) { e =>
                  e.keyCode match
                    case 13 =>
                      log("페이지 서치 엔터 클릭")
                      // Enter key
                      Some(PageMoveMsg.Patch)
                    case _ =>
                      None
                },
              ),
        )

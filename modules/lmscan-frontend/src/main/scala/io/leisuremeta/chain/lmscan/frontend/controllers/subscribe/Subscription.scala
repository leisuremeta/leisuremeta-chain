package io.leisuremeta.chain.lmscan.frontend
import cats.effect.IO
import org.scalajs.dom.KeyboardEvent
import tyrian.*

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
                // Enter key
                Some(InputMsg.Patch)
              case _ =>
                None
          },
        )

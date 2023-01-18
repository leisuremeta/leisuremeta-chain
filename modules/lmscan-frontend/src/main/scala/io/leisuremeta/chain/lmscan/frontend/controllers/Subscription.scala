package io.leisuremeta.chain.lmscan.frontend
import cats.effect.IO
import org.scalajs.dom.KeyboardEvent
import org.scalajs.dom.window
import tyrian.*

object Subscriptions:
  def subscriptions(model: Model): Sub[IO, Msg] =
    Sub.Batch(
      Sub.fromEvent[IO, KeyboardEvent, Msg](
        "keydown",
        window.document.getElementsByClassName("sub-search").item(0),
      ) { e =>
        e.keyCode match
          case 13 =>
            // Enter key
            Some(InputMsg.Patch)
          case _ =>
            None
      },
    )

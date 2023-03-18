package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import scala.scalajs.js
import org.scalajs.dom.window
import tyrian.*
import cats.effect.IO

object PageUpdate:
  def update(model: Model): PageMsg => (Model, Cmd[IO, Msg]) =
    case PageMsg.PreUpdate(page: PageName) =>
      window.history.pushState(
        // save page to history
        page.toString(),
        null,
        // show url
        s"${window.location.origin}/"
          ++
            page
              .toString()
              .replace("Detail", "")
              .replace("(", "/")
              .replace(
                ",",
                "/",
              ) // accountdetail(playnomm,1) => account/playnomm/1
              .replace(")", "")
              .toLowerCase(),
      )
      (
        model.copy(
          curPage = page,
          observer = model.observer ++ Seq(page),
        ),
        Cmd.None,
      )
    case _ =>
      (
        model.copy(),
        Cmd.None,
      )

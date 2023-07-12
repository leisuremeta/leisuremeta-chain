package io.leisuremeta.chain.lmscan.frontend
import io.leisuremeta.chain.lmscan.frontend.Log.log

import org.scalajs.dom.window

object Window:
  def History = (save: String, show: String) =>
    window.history.pushState(
      save,
      null,
      s"${window.location.origin}/"
        ++ show,
    )

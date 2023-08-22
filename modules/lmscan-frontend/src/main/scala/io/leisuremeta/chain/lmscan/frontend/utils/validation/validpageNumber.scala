package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*
import org.scalajs.dom.window

def limit_value(page: String, limit: Int = 50000) =
  window.localStorage.getItem("commandMode") match
    case "Production" =>
      page.toInt > limit match
        case true  => limit
        case false => page.toInt
    case "Development" => page.toInt

def validPageNumber(t: "Block" | "Tx")(model: Model) =
  t match
    case "Block" =>
      val page = model.block_current_page
      limit_value(page)

    case "Tx" =>
      val page = model.tx_current_page
      limit_value(page)

def block_validPageNumber = validPageNumber("Block")
def tx_validPageNumber    = validPageNumber("Tx")

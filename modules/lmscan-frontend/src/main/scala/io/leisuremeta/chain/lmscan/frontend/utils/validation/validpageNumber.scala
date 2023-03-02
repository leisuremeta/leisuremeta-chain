package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*
import io.leisuremeta.chain.lmscan.frontend.Log.log
import io.leisuremeta.chain.lmscan.frontend.ModelPipe.find_current_PubPage
import org.scalajs.dom.window

def limit_value(page: String, limit: Int = 50000) =
  window.localStorage.getItem("commandMode") match
    case "Production" =>
      page.toInt > limit match
        case true  => limit
        case false => page.toInt
    case "Development" => page.toInt

// def limit_value2(page: String, limit: Int = 50000) =
//   getLimit_value(
//     page, {
//       window.localStorage.getItem("commandMode") match
//         case "Production"  => window.localStorage.getItem("limit").toInt
//         case "Development" => 10000000
//     },
//   )

def validPageNumber(t: "Block" | "Tx")(model: Model) =
  t match
    case "Block" =>
      val page = model.block_current_page
      !page.forall(
        Character.isDigit,
      ) || page == "" || page.toInt > model.block_total_page.toInt match
        case true =>
          find_current_PubPage(model)
        case false =>
          limit_value(page)

    case "Tx" =>
      val page = model.tx_current_page
      !page.forall(
        Character.isDigit,
      ) || page == "" || page.toInt > model.tx_total_page.toInt match
        case true =>
          find_current_PubPage(model)
        case false =>
          limit_value(page)

def block_validPageNumber = validPageNumber("Block")
def tx_validPageNumber    = validPageNumber("Tx")

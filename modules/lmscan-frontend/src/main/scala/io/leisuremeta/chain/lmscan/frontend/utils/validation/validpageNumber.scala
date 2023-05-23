package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*
import io.leisuremeta.chain.lmscan.frontend.Log.log
import io.leisuremeta.chain.lmscan.frontend.ModelPipe.find_current_PubPage

def limit_value(page: String, limit: Int = 50) = page.toInt > 50 match
  case true  => 50
  case false => page.toInt

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
          page.toInt > 50 match
            case true  => 50
            case false => page.toInt

    case "Tx" =>
      val page = model.tx_current_page
      !page.forall(
        Character.isDigit,
      ) || page == "" || page.toInt > model.tx_total_page.toInt match
        case true =>
          find_current_PubPage(model)
        case false =>
          page.toInt > 50 match
            case true  => 50
            case false => page.toInt

def block_validPageNumber = validPageNumber("Block")
def tx_validPageNumber    = validPageNumber("Tx")

package io.leisuremeta.chain.lmscan.frontend

import Log.log
import cats.effect.IO
import tyrian.Html.*
import tyrian.*

object SearchUpdate:
  def update(model: Model): InputMsg => (Model, Cmd[IO, Msg]) =
    case InputMsg.Get(s) =>
      (model.copy(searchValue = s), Cmd.None)

    case InputMsg.Patch =>
      (
        model.copy(searchValue = ""),
        Cmd.emit(
          ValidPageName.getPageFromString(model.searchValue) match
            case pagecase: PageCase =>
              PageMsg.PreUpdate(
                pagecase,
              )
            case commandcase: CommandCaseMode =>
              CommandMsg.OnClick(
                commandcase,
              )
            case commandcase: CommandCaseLink =>
              CommandMsg.OnClick(
                commandcase,
              ),
        ),
      )

// case PageMoveMsg.Get(value) =>
//       model.curPage.toString() match
//         case "Transactions" =>
//           (
//             model.copy(tx_list_Search = value),
//             Cmd.None,
//           )
//         case "Blocks" =>
//           (
//             model.copy(block_list_Search = value),
//             Cmd.None,
//           )
//         case _ => (model, Cmd.None)

//     case PageMoveMsg.Patch(value) =>
//       model.curPage.toString() match
//         case "Transactions" =>
//           val str = value match
//             case "Enter" => model.tx_list_Search
//             case _       => value

//           val res = // filter only number like string and filter overflow pagenumber
//             !str.forall(
//               Character.isDigit,
//             ) || str == "" || str.toInt > model.tx_TotalPage match
//               case true  => model.tx_CurrentPage
//               case false => str.toInt

//           log(s"PageMoveMsg.Patch ${str} ${res}")
//           (
//             model.copy(
//               tx_CurrentPage = res,
//               tx_list_Search = res.toString(),
//             ),
//             OnDataProcess.getData(
//               PageName.Transactions,
//               ApiPayload(page = res.toString()),
//             ),
//           )
//         case "Blocks" =>
//           val str = value match
//             case "Enter" => model.block_list_Search
//             case _       => value

//           val res = // filter only number like string and filter overflow pagenumber
//             !str.forall(
//               Character.isDigit,
//             ) || str == "" || str.toInt > model.block_TotalPage match
//               case true  => model.block_CurrentPage
//               case false => str.toInt

//           (
//             model.copy(
//               block_CurrentPage = res,
//               block_list_Search = res.toString(),
//             ),
//             OnDataProcess.getData(
//               PageName.Blocks,
//               ApiPayload(page = res.toString()),
//             ),
//           )

//         case _ => (model, Cmd.None)

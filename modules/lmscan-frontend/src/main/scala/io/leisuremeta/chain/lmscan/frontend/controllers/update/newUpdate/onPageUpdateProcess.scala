// package io.leisuremeta.chain.lmscan.frontend

// import cats.effect.IO
// import tyrian.Html.*
// import tyrian.*
// import Log.log

// object PageUpdate:
//   def update(model: Model): PageMsg => (Model, Cmd[IO, Msg]) =
//     case PageMsg.PreProcess =>
//       log(
//         (
//           model.copy(prevPage = model.curPage),
//           Cmd.None,
//         ),
//       )
//     case PageMsg.DataProcess =>
//       log(
//         (
//           model.copy(curPage = NavMsg.DashBoard),
//           Cmd.None,
//         ),
//       )
//     case PageMsg.PageUpdateProcess =>
//       log(
//         (
//           model.copy(curPage = NavMsg.DashBoard),
//           Cmd.None,
//         ),
//       )
//     case PageMsg.PostProcess =>
//       log(
//         (
//           model.copy(curPage = NavMsg.DashBoard),
//           Cmd.None,
//         ),
//       )

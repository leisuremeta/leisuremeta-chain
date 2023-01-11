// package io.leisuremeta.chain.lmscan.frontend

// import cats.effect.IO
// import tyrian.Html.*
// import tyrian.*

// object BlocksUpdate:
//   def update(model: Model): NavMsg => (Model, Cmd[IO, Msg]) =
//     case NavMsg.DashBoard =>
//       println(model)
//       (model.copy(tab = NavMsg.DashBoard), Cmd.None)
//     case NavMsg.Blocks =>
//       println(model)
//       (model.copy(tab = NavMsg.Blocks), Cmd.None)
//     case NavMsg.Transactions =>
//       println(model)
//       (model.copy(tab = NavMsg.Transactions), Cmd.None)

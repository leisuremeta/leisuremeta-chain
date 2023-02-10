// package io.leisuremeta.chain.lmscan.frontend

// import cats.effect.IO
// import tyrian.Html.*
// import tyrian.*

// import Log.log

// object NftDetailUpdate:
//   def update(model: Model): NftDetailMsg => (Model, Cmd[IO, Msg]) =
//     case NftDetailMsg.Patch(hash) =>
//       OnNftDetailMsg.getNftDetail(hash)
//       (
//         model,
//         Cmd.None,
//       )
//     case NftDetailMsg.Update(data) =>
//       (
//         model.copy(
//           nftDetailData = Some(data),
//           curPage = PageName.NftDetail(model.searchValue),
//           searchValue = "",
//         ),
//         Cmd.None,
//       )
//     case NftDetailMsg.GetError(msg) =>
//       log(msg)
//       (model.copy(curPage = PageName.NoPage, searchValue = ""), Cmd.None)

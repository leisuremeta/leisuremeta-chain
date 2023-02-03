package io.leisuremeta.chain.lmscan.frontend
import tyrian.*
import cats.effect.IO
import Log.log

object Update:
  def update(model: Model): Msg => (Model, Cmd[IO, Msg]) =
    // component handle
    case navMsg: NavMsg           => NavUpdate.update(model)(navMsg)
    case inputMsg: InputMsg       => SearchUpdate.update(model)(inputMsg)
    case toggleMsg: ToggleMsg     => ToggleUpdate.update(model)(toggleMsg)
    case pageMoveMsg: PageMoveMsg => PageMoveUpdate.update(model)(pageMoveMsg)

    // api handle
    case apiMsg: ApiMsg           => ApiUpdate.update(model)(apiMsg)
    case txMsg: TxMsg             => TxUpdate.update(model)(txMsg)
    case txDetailMsg: TxDetailMsg => TxDetailUpdate.update(model)(txDetailMsg)
    case blockMsg: BlockMsg       => BlockUpdate.update(model)(blockMsg)
    case blockDetailMsg: BlockDetailMsg =>
      BlockDetailUpdate.update(model)(blockDetailMsg)
    case nftDetailMsg: NftDetailMsg =>
      NftDetailUpdate.update(model)(nftDetailMsg)
    case accountDetailMsg: AccountDetailMsg =>
      AccountDetailUpdate.update(model)(accountDetailMsg)  

    // case dashboardMsg: DashboardMsg => Board.update(model)(dashboardMsg)

package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*
import tyrian.*

object NavBar:
  val main = "Dashboard"
  val blc = "Blocks"
  val tx = "Transactions"
  val acc = "Accounts"
  val nft = "NFTs"
  val vd = "Validators"
  def view(model: Model): Html[Msg] =
    nav()(
      p(onClick(ToPage(BaseModel())))("LM SCAN")
      ::
      List(
        (main, ToPage(BaseModel())),
        (blc, ToPage(BlcModel(page = 1))),
        (tx, ToPage(TxModel(page = 1))),
        (acc, ToPage(AccModel(page = 1))),
        (nft, ToPage(NftModel(page = 1))),
        (vd, ToPage(VdModel())),
      ).map((name, msg) =>
        a(
          cls := isActive(name, model),
          onClick(msg),
        )(span(name))
      ),
    )

  def isActive(name: String, model: Model) = model match
    case m: BaseModel => if name == main then "active" else ""
    case _: BlcModel if name == blc => "active"
    case _: BlcDetailModel if name == blc => "active"
    case _: TxModel if name == tx => "active"
    case _: TxDetailModel if name == tx => "active"
    case _: AccModel if name == acc => "active"
    case _: AccDetailModel if name == acc => "active"
    case _: NftModel if name == nft => "active"
    case _: NftTokenModel if name == nft => "active"
    case _: NftDetailModel if name == nft => "active"
    case _: VdModel if name == vd => "active"
    case _ => ""
    

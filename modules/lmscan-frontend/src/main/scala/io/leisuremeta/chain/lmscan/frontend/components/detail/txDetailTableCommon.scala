package io.leisuremeta.chain
package lmscan.frontend

import tyrian.Html.*
import tyrian.*
import api.model._
import api.model.Transaction.AccountTx.*
import api.model.Transaction.TokenTx.*
import api.model.Transaction.GroupTx.*
import api.model.Transaction.RewardTx.*
import api.model.Transaction.AgendaTx.*
import api.model.token.*
import io.leisuremeta.chain.lib.datatype.BigNat
import io.leisuremeta.chain.api.model.Signed.TxHash
import io.leisuremeta.chain.api.model.Transaction._

object TxDetailTableCommon:
  def row(head: String, value: String) = div(cls := "row")(span(head), span(value))

  def rowCustom(head: String, custom: Html[Msg]) = 
    div(cls := "row")(span(head), custom)

  def rowInputHead = row("Input", "Transaction Hash")

  def rowTriOutput(s: String) = div(cls := "row tri")(span("Output"), span("To"), span(s))

  def rowInputBody(input: TxHash, i: Int = 0) = 
    div(cls := "row")(span(s"${i + 1}"), ParseHtml.fromTxHash(input.toUInt256Bytes.toHex))

  def rowAccToken(output: Account, v: TokenId) =
    div(cls := "row tri")(span("1"), ParseHtml.fromAccHash(Some(output.toString)), ParseHtml.fromTokenId(v.toString))

  def rowAccBal(outputs: (Account, BigNat), i: Int) =
    div(cls := "row tri")(
      span(s"${i + 1}"),
      ParseHtml.fromAccHash(Some(outputs._1.toString())),
      ParseHtml.fromBal(Some(BigDecimal(outputs._2.toString))),
    )

  val r = """.*\/collections\/(.*)\/.*\/.*\.json""".r
  def getCollectionNameFromUrl(url: String) = 
    r.replaceFirstIn(url, "$1")

  
  def getNFromId(id: String) =
    id.drop(16).dropWhile(c => !c.isDigit || c == '0').prepended('#')
    

  def view(data: Option[TransactionWithResult]) =
    val result = for
      tx <- data
      res = tx.signedTx.value match
        case tx: Transaction.TokenTx => tokenView(tx)
        case tx: Transaction.AccountTx => accountView(tx)
        case tx: Transaction.GroupTx => groupView(tx)
        case tx: Transaction.RewardTx => rewardView(tx)
        case t: Transaction.AgendaTx => agendaView(t, tx.result)
    yield res
    result.getOrElse(List(div(""))).prepended(div(cls := "page-title")("Transaction Values"))

  def tokenView(tx: TokenTx) = tx match
    case nft: DefineToken => 
      div(cls := "detail table-container")(
        row("Definition ID", nft.definitionId.toString),
        row("Name", nft.name.toString()),
      ) :: List()
    case nft: DefineTokenWithPrecision => 
      div(cls := "detail table-container")(
        row("Definition ID", nft.definitionId.toString),
        row("Name", nft.name.toString()),
      ) :: List()
    case nft: MintNFT => 
      div(cls := "detail table-container")(
        rowCustom("Token ID", ParseHtml.fromTokenId(nft.tokenId.toString)),
        row("Collection Name", getCollectionNameFromUrl(nft.dataUrl.toString)),
        row("No", getNFromId(nft.tokenId.toString)),
        rowCustom("Data URI", a(href := nft.dataUrl.toString, target := "_blank")(nft.dataUrl.toString)),
        row("Content Hash", nft.contentHash.toHex),
        row("Rarity", nft.rarity.toString),
      ) :: List()
    case nft: MintNFTWithMemo => 
      div(cls := "detail table-container")(
        rowCustom("Token ID", ParseHtml.fromTokenId(nft.tokenId.toString)),
        row("Collection Name", getCollectionNameFromUrl(nft.dataUrl.toString)),
        row("No", getNFromId(nft.tokenId.toString)),
        rowCustom("Data URI", a(href := nft.dataUrl.toString, target := "_blank")(nft.dataUrl.toString)),
        row("Content Hash", nft.contentHash.toHex),
        row("Rarity", nft.rarity.toString),
      ) :: List()
    case nft: UpdateNFT => 
      div(cls := "detail table-container")(
        rowTriOutput("Token ID"),
        rowAccToken(nft.output, nft.tokenId),
      ) :: List()
    case nft: TransferNFT => 
      div(cls := "detail table-container")(
        rowInputHead,
        rowInputBody(nft.input),
      ) ::
      div(cls := "detail table-container")(
        rowTriOutput("Token ID"),
        rowAccToken(nft.output, nft.tokenId),
      ) :: List()
    case nft: BurnNFT => 
      div(cls := "detail table-container")(
        row("Definition ID", nft.definitionId.toString),
      ) :: List()
    case nft: EntrustNFT => 
      div(cls := "detail table-container")(
        rowInputHead,
        rowInputBody(nft.input),
      ) ::
      div(cls := "detail table-container")(
        rowTriOutput("Token ID"),
        rowAccToken(nft.to, nft.tokenId),
      ) :: List()
    case nft: DisposeEntrustedNFT => 
      div(cls := "detail table-container")(
        rowInputHead,
        rowInputBody(nft.input),
      ) ::
      div(cls := "detail table-container")(
        rowTriOutput("Token ID"),
        rowAccToken(nft.output.get, nft.tokenId),
      ) :: List()
    case tx: MintFungibleToken => 
      div(cls := "detail table-container")(
        rowTriOutput("Value") ::
        tx.outputs.zipWithIndex
          .map((d, i) => rowAccBal(d, i))
          .toList,
      ) :: List()
    case tx: TransferFungibleToken => 
      div(cls := "detail table-container")(
        row("Definition ID", tx.tokenDefinitionId.toString),
      ) ::
      div(cls := "detail table-container")(
        rowInputHead ::
        tx.inputs.zipWithIndex
          .map((a, i) => rowInputBody(a, i))
          .toList
      ) ::
      div(cls := "detail table-container")(
        rowTriOutput("Value") ::
        tx.outputs.zipWithIndex
          .map((d, i) => rowAccBal(d, i))
          .toList
      ) :: List()
    case tx: EntrustFungibleToken => 
      div(cls := "detail table-container")(
        rowInputHead ::
        tx.inputs.zipWithIndex
          .map((a, i) => rowInputBody(a, i))
          .toList
      ) ::
      div(cls := "detail table-container")(
        rowTriOutput("Value"),
        rowAccBal((tx.to, tx.amount), 0),
      ) :: List()
    case tx: DisposeEntrustedFungibleToken => 
      div(cls := "detail table-container")(
        rowInputHead ::
        tx.inputs.zipWithIndex
          .map((a, i) => rowInputBody(a, i))
          .toList
      ) ::
      div(cls := "detail table-container")(
        rowTriOutput("Value") ::
        tx.outputs.zipWithIndex
          .map((d, i) => rowAccBal(d, i))
          .toList
      ) :: List()
    case tx: BurnFungibleToken =>
      div(cls := "detail table-container")(
        row("Ammount", tx.amount.toString),
      ) :: List()

  def accountView(tx: AccountTx) = tx match
    case tx: CreateAccount =>
      div(cls := "detail table-container")(
        rowCustom(
          "Account",
          ParseHtml.fromAccHash(Some(tx.account.toString)),
        ),
      ) :: List()
    case tx: UpdateAccount =>
      div(cls := "detail table-container")(
        rowCustom(
          "Account",
          ParseHtml.fromAccHash(Some(tx.account.toString)),
        ),
        row("Ethereum Address", tx.ethAddress.get.toString),
        rowCustom(
          "Guardian",
          ParseHtml.fromAccHash(tx.guardian.map(_.toString), false),
        ),
      ) :: List()
    case tx: AddPublicKeySummaries =>
      div(cls := "detail table-container")(
        rowCustom(
          "Account",
          ParseHtml.fromAccHash(Some(tx.account.toString)),
        ),
        row("PublicKey Summary", tx.summaries.keys.head.toBytes.toHex),
      ) :: List()

  def groupView(tx: GroupTx) = tx match
    case tx: CreateGroup =>
      div(cls := "detail table-container")(
        row("Group ID", tx.groupId.toString),
        row("Coordinator Account", tx.coordinator.toString),
      ) :: List()
    case tx: AddAccounts =>
      div(cls := "detail table-container")(
        row("Group ID", tx.groupId.toString),
        row("Account", tx.accounts.toList(0).toString()),
      ) :: List()

  def rewardView(tx: RewardTx) = tx match
    case tx: OfferReward =>
      div(cls := "detail table-container")(
        row("Definition ID", tx.tokenDefinitionId.toString),
      ) ::
      div(cls := "detail table-container")(
        rowInputHead ::
        tx.inputs.zipWithIndex
          .map((a, i) => rowInputBody(a, i))
          .toList
      ) :: List()
      div(cls := "detail table-container")(
        rowTriOutput("Value") ::
        tx.outputs.zipWithIndex
          .map((d, i) => rowAccBal(d, i))
          .toList,
      ) :: List()
    case tx: RegisterDao =>
      div(cls := "detail table-container")(
        row("Group ID", tx.groupId.toString),
        row("DAO Account Name", tx.daoAccountName.toString),
        if tx.moderators.size > 0 then row("Moderators", tx.moderators.mkString(",")) else Empty,
      ) :: List()
    case tx: UpdateDao =>
      div(cls := "detail table-container")(
        row("Group ID", tx.groupId.toString),
      ) :: List()
    case _ => 
      div(cls := "detail table-container")(
        ""
      ) :: List()
    // case tx: RecordActivity =>
    // case tx: BuildSnapshot =>
    // case tx: ExecuteOwnershipReward =>
    // case tx: ExecuteReward =>

  def agendaView(tx: AgendaTx, result: Option[TransactionResult]) = tx match
    case tx: SuggestSimpleAgenda =>
      div(cls := "detail table-container")(
        row("Title", tx.title.toString),
        row("Voting Token", tx.votingToken.toString),
        rowCustom("Vote Start", ParseHtml.fromDate(Some(tx.voteStart.getEpochSecond()))),
        rowCustom("Vote End", ParseHtml.fromDate(Some(tx.voteEnd.getEpochSecond()))),
        rowCustom("Vote End", p(tx.voteEnd.toString())),
      ) ::
      div(cls := "detail table-container")(
        row("vote option", "selected") ::
        tx.voteOptions
          .map(a => row(a._1.toString, a._2.toString))
          .toList
      ) :: List()
    case tx: VoteSimpleAgenda =>
      div(cls := "detail table-container")(
        rowCustom(
          "Agenda Tx Hash",
          ParseHtml.fromTxHash(tx.agendaTxHash.toUInt256Bytes.toHex),
        ),
        row("Selected Option", tx.selectedOption.toString),
        // row(
        //   "Voting Power",
        //   result.map(d =>
        //     d match
        //       case d: VoteSimpleAgendaResult => d.votingAmount.toString
        //       case _ => "",
        //   ).getOrElse(""),
        // ),
      ) :: List()

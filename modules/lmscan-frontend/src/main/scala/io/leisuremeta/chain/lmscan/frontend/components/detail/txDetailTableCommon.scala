package io.leisuremeta.chain
package lmscan.frontend

import tyrian.Html.*
import tyrian.*
import lib.datatype.BigNat
import api.model._
import api.model.token._
import api.model.Signed.TxHash
import api.model.Transaction._
import api.model.Transaction.AccountTx._
import api.model.Transaction.TokenTx._
import api.model.Transaction.GroupTx._
import api.model.Transaction.RewardTx._
import api.model.Transaction.AgendaTx._
import api.model.Transaction.CreatorDaoTx._
import api.model.Transaction.VotingTx._

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
  
  def getNFromId(id: String) =
    id.drop(16).dropWhile(c => !c.isDigit || c == '0').prepended('#')
    
  def view(data: Option[TransactionWithResult]) =
    val result = for
      tx <- data
      res = tx.signedTx.value match
        case t: Transaction.TokenTx => tokenView(t)
        case t: Transaction.AccountTx => accountView(t)
        case t: Transaction.GroupTx => groupView(t)
        case t: Transaction.RewardTx => rewardView(t)
        case t: Transaction.AgendaTx => agendaView(t, tx.result)
        case t: Transaction.VotingTx => votingView(t)
        case t: Transaction.CreatorDaoTx => creatorDaoView(t)
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
        // row("Collection Name", nft.collectionName),
        row("No", getNFromId(nft.tokenId.toString)),
        rowCustom("Data URI", a(href := nft.dataUrl.toString, target := "_blank")(nft.dataUrl.toString)),
        row("Content Hash", nft.contentHash.toHex),
        row("Rarity", nft.rarity.toString),
      ) :: List()
    case nft: MintNFTWithMemo => 
      div(cls := "detail table-container")(
        rowCustom("Token ID", ParseHtml.fromTokenId(nft.tokenId.toString)),
        // row("Collection Name", nft.collectionName),
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
        tx.inputs
          .zipWithIndex
          .toList
          .sortBy(_._2)
          .map((a, i) => rowInputBody(a, i))
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
    case tx: CreateSnapshots =>
      div(cls := "detail table-container")(
        rowCustom("Definition ID", div(cls := "inner")(tx.definitionIds.map(di => span(di.toString)).toList)),
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
    case _ => List()

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
      ) :: List()

  def votingView(tx: VotingTx) = tx match
    case tx: CreateVoteProposal =>
      div(cls := "detail table-container")(
        row("Proposal ID", tx.proposalId.value.toString),
        row("Title", tx.title.value),
        row("Description", tx.description.value),
        rowCustom("Vote Start", ParseHtml.fromDate(Some(tx.voteStart.getEpochSecond()))),
        rowCustom("Vote End", ParseHtml.fromDate(Some(tx.voteEnd.getEpochSecond()))),
        rowCustom("Vote Type", p(tx.voteType.name)),
        rowCustom("Voting Power", div(cls := "inner")(tx.votingPower.keys.map(k => p(k.toString)).toList)),
      ) ::
      div(cls := "detail table-container")(
        rowCustom("Voting Option", div(cls := "inner")(tx.voteOptions.map(a => row(a._1.toString, a._2.toString)).toList))
      ) :: List()
    case tx: CastVote =>
      div(cls := "detail table-container")(
        row("Proposal Id", tx.proposalId.value.toString),
        row("Selected Option", tx.selectedOption.toString),
      ) :: List()
    case tx: TallyVotes =>
      div(cls := "detail table-container")(
        row("Proposal Id", tx.proposalId.value.toString),
      ) :: List()

  def creatorDaoView(tx: CreatorDaoTx) = tx match
    case tx: CreateCreatorDao =>
      div(cls := "detail table-container")(
        row("ID", tx.id.toString),
        row("Name", tx.name.toString),
        row("Description", tx.description.toString),
        row("Founder", tx.founder.toString),
        row("Coordinator", tx.coordinator.toString),
      ) :: List()
    case tx: UpdateCreatorDao =>
      div(cls := "detail table-container")(
        row("ID", tx.id.toString),
        row("Name", tx.name.toString),
        row("Description", tx.description.toString),
      ) :: List()
    case tx: DisbandCreatorDao =>
      div(cls := "detail table-container")(
        row("ID", tx.id.toString),
      ) :: List()
    case tx: ReplaceCoordinator =>
      div(cls := "detail table-container")(
        row("ID", tx.id.toString),
        row("New Coordinator", tx.newCoordinator.utf8.value),
      ) :: List()
    case tx: AddMembers =>
      div(cls := "detail table-container")(
        row("ID", tx.id.toString),
        rowCustom("Members", div(cls := "inner")(tx.members.map(a => div(a.toString)).toList))
      ) :: List()
    case tx: RemoveMembers =>
      div(cls := "detail table-container")(
        row("ID", tx.id.toString),
        rowCustom("Members", div(cls := "inner")(tx.members.map(a => div(a.toString)).toList))
      ) :: List()
    case tx: PromoteModerators =>
      div(cls := "detail table-container")(
        row("ID", tx.id.toString),
        rowCustom("Members", div(cls := "inner")(tx.members.map(a => div(a.toString)).toList))
      ) :: List()
    case tx: DemoteModerators =>
      div(cls := "detail table-container")(
        row("ID", tx.id.toString),
        rowCustom("Members", div(cls := "inner")(tx.members.map(a => div(a.toString)).toList))
      ) :: List()

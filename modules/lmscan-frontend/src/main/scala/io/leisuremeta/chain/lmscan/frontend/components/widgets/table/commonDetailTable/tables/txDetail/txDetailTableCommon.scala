package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*
import V.*
import io.leisuremeta.chain.lmscan.common.model.TxDetail
import scala.util.chaining.*
import io.leisuremeta.chain.lmscan.frontend.Log.log2
import io.circe.parser.decode
import cats.data.EitherT
import io.leisuremeta.chain.api.model.Transaction
import io.leisuremeta.chain.api.model.Transaction.AccountTx.*
import io.leisuremeta.chain.api.model.Transaction.TokenTx.*
import io.leisuremeta.chain.api.model.Transaction.GroupTx.*
import io.leisuremeta.chain.api.model.Transaction.RewardTx.*
import io.leisuremeta.chain.api.model.Transaction.AgendaTx.*
import io.leisuremeta.chain.api.model.account.*
import io.leisuremeta.chain.api.model.*
import io.leisuremeta.chain.lib.datatype.Utf8
import io.leisuremeta.chain.api.model.token.*
import io.leisuremeta.chain.lib.crypto.Hash.Value
import io.leisuremeta.chain.lib.datatype.BigNat
import io.leisuremeta.chain.api.model.Signed.TxHash
import io.leisuremeta.chain.lmscan.frontend.TestoLogic.set_MaximumPoint
import io.leisuremeta.chain.lmscan.frontend.Pipe._Down18

object TxDetailTableCommon:
  def genTable(d: List[Html[PageMsg]]) = div(`class` := "x")(
    div(`class` := "type-TableDetail  table-container")(
      div(`class` := "table w-[100%] ")(
        d,
      ),
    ),
  )

  def genRow(d: List[Html[PageMsg]]) = div(`class` := "row")(
    d,
  )

  def onlyNumber(d: String) =
    """\d+""".r
      .findAllIn(d)
      .toList
      .mkString("")

  def genTxDetail() = gen.cell(
    Cell.Head("genTxDetail", "cell type-detail-head"),
    Cell.PlainStr(Some("{genTxDetail}"), "cell type-detail-body"),
  )
  def genTxDetail(head: String, value: String) = gen.cell(
    Cell.Head(head, "cell type-detail-head"),
    Cell.PlainStr(Some(value), "cell type-detail-body"),
  )
  def genTxDetail_value(head: String, value: String) = gen.cell(
    Cell.Head(head, "cell type-detail-head"),
    Cell.Tx_VALUE(
      Some("Value"),
      Some(value),
    ),
  )
  def genTxDetail_hash(head: String, value: String) = gen.cell(
    Cell.Head(head, "cell type-detail-head"),
    Cell.TX_HASH(
      Some(value),
    ),
  )

  def genTxDetail_input_head =
    gen.cell(
      Cell.Head("Input", "cell type-detail-head"),
      Cell.Head("Transaction Hash", "cell type-detail-body"),
    )
  def genTxDetail_input_head_value =
    gen.cell(
      Cell.Head("Input", "cell type-detail-head"),
      Cell.Head("To", "cell type-detail-body"),
      Cell.Head("Value", "cell type-detail-body"),
    )

  def genTxDetail_input_body(input: TxHash) =
    gen.cell(
      Cell.PlainStr(Some(1), "cell type-detail-body"),
      Cell.TX_HASH(
        Some(input.toUInt256Bytes.toHex),
      ),
    )

  def genTxDetail_inputs_body(
      intputs: TxHash,
      i: Int,
  ) = gen.cell(
    Cell.PlainStr(Some(i + 1), "cell type-detail-body"),
    Cell.TX_HASH(
      Some(intputs.toUInt256Bytes.toHex),
    ),
  )
  def genTxDetail_inputs_body(
      a: Utf8,
      b: Utf8,
  ) = gen.cell(
    Cell.PlainStr(Some(a.toString()), "cell type-detail-body"),
    Cell.PlainStr(Some(b.toString()), "cell type-detail-body"),
  )

  def genTxDetail_output_head(v: TokenId) =
    gen.cell(
      Cell.Head("Output", "cell type-detail-head"),
      Cell.Head("To", "cell type-detail-body"),
      Cell.Head("Token ID", "cell type-detail-body"),
    )
  def genTxDetail_output_head(v: BigNat | Int) =
    gen.cell(
      Cell.Head("Output", "cell type-detail-head"),
      Cell.Head("To", "cell type-detail-body"),
      Cell.Head("Value", "cell type-detail-body"),
    )

  def genTxDetail_output_body(
      output: Account,
      v: TokenId,
  ) = gen.cell(
    Cell.PlainStr(Some(1), "cell type-detail-body"),
    Cell.ACCOUNT_HASH_Long(
      Some(output.toString),
    ),
    Cell.Tx_VALUE(Some("Nft"), Some(v.toString())),
  )
  def genTxDetail_output_body(
      output: Account,
      v: BigNat,
  ) = gen.cell(
    Cell.PlainStr(Some(1), "cell type-detail-body"),
    Cell.ACCOUNT_HASH_Long(
      Some(output.toString),
    ),
    Cell.PlainStr(Some(v.toString), "cell type-detail-body"),
  )
  def genTxDetail_outputs_body(
      outputs: (Account, BigNat),
      i: Int,
  ) = gen.cell(
    Cell.PlainStr(Some(i + 1), "cell type-detail-body"),
    Cell.ACCOUNT_HASH_Long(
      Some(outputs._1.toString),
      // Some("10"),
    ),
    Cell.PlainStr(
      Some(
        BigDecimal(outputs._2.toString.pipe(_Down18).toString)
          .pipe(set_MaximumPoint(4)),
          // "1",
      ),
      // Some(outputs._2.toString()),
      "cell type-detail-body",
    ),
  )
  // offer 리워드의 경우 value 가 18자리로 오기 때문에 자릿수 조절을 해야한다.
  def genTxDetail_outputs_body_offer(
      outputs: (Account, BigNat),
      i: Int,
  ) = gen.cell(
    Cell.PlainStr(Some(i + 1), "cell type-detail-body"),
    Cell.ACCOUNT_HASH_Long(
      Some(outputs._1.toString),
    ),
    Cell.PlainStr(
      Some(
        outputs._2
          .toString()
          .pipe(_Down18)
          .pipe(set_MaximumPoint(4)),
      ),
      "cell type-detail-body",
    ),
  )

  def genTxDetail_account(output: Account) =
    gen.cell(
      Cell.Head("output", "cell type-detail-head"),
      Cell.PlainStr(Some(output.toString()), "cell  type-3 type-detail-body"),
    )

  def genTxDetailView(json: String) = TransactionWithResultParser
    .decodeParser(json)
    .map(tx =>
      tx.signedTx.value match
        case tokenTx: Transaction.TokenTx =>
          tokenTx match
            case nft: DefineToken =>
              List(
                List(
                  genTxDetail("Definition ID", nft.definitionId.toString),
                  genTxDetail("Name", nft.name.toString()),
                ),
              )
            case nft: DefineTokenWithPrecision =>
              List(
                List(
                  genTxDetail("Definition ID", nft.definitionId.toString),
                  genTxDetail("Name", nft.name.toString()),
                ),
              )
            case nft: MintNFT =>
              List(
                List(
                  genTxDetail_output_head(nft.tokenId),
                  genTxDetail_output_body(nft.output, nft.tokenId),
                ),
              )
            case nft: TransferNFT =>
              List(
                List(
                  genTxDetail_input_head,
                  genTxDetail_input_body(nft.input),
                ),
                List(
                  genTxDetail_output_head(nft.tokenId),
                  genTxDetail_output_body(nft.output, nft.tokenId),
                ),
              )
            case nft: BurnNFT =>
              List(
                List(
                  genTxDetail("Definition ID", nft.definitionId.toString),
                ),
              )
            case nft: EntrustNFT =>
              List(
                List(
                  genTxDetail_input_head,
                  genTxDetail_input_body(nft.input),
                ),
                List(
                  genTxDetail_output_head(nft.tokenId),
                  genTxDetail_output_body(nft.to, nft.tokenId),
                ),
              )
            case nft: DisposeEntrustedNFT =>
              List(
                List(
                  genTxDetail_input_head,
                  genTxDetail_input_body(nft.input),
                ),
                List(
                  genTxDetail_output_head(nft.tokenId),
                  genTxDetail_output_body(nft.output.get, nft.tokenId),
                ),
              )
            case tx: MintFungibleToken =>
              List(
                List(
                  genTxDetail_output_head(1),
                )
                  ++
                    tx.outputs.zipWithIndex
                      .map((d, i) => genTxDetail_outputs_body(d, i))
                      .toList,
              )
            case tx: TransferFungibleToken =>
              List(
                List(
                  genTxDetail("Definition ID", tx.tokenDefinitionId.toString),
                ),
                List(
                  genTxDetail_input_head,
                )
                  ++ {
                    tx.inputs.zipWithIndex
                      .map((a, i) => genTxDetail_inputs_body(a, i))
                      .toList
                    // .pipe(a => a)
                  },
                List(
                  genTxDetail_output_head(1),
                ) ++
                  tx.outputs.zipWithIndex
                    .map((d, i) => genTxDetail_outputs_body(d, i))
                    .toList,
              )
            case tx: EntrustFungibleToken =>
              List(
                List(
                  genTxDetail_input_head,
                )
                  ++ {
                    tx.inputs.zipWithIndex
                      .map((a, i) => genTxDetail_inputs_body(a, i))
                      .toList
                      .pipe(a => a)
                  },
                List(
                  genTxDetail_output_head(1),
                  genTxDetail_output_body(tx.to, tx.amount),
                ),
              )
            case tx: DisposeEntrustedFungibleToken =>
              List(
                List(
                  genTxDetail_input_head,
                )
                  ++ {
                    tx.inputs.zipWithIndex
                      .map((a, i) => genTxDetail_inputs_body(a, i))
                      .toList
                      .pipe(a => a)
                  },
                List(
                  genTxDetail_output_head(1),
                )
                  ++
                    tx.outputs.zipWithIndex
                      .map((d, i) => genTxDetail_outputs_body(d, i))
                      .toList,
              )
            case tx: BurnFungibleToken =>
              List(
                List(
                  genTxDetail("Ammount", tx.amount.toString),
                ),
              )

        case accountTx: Transaction.AccountTx =>
          accountTx match
            case tx: CreateAccount =>
              List(
                List(
                ),
              )
            case tx: UpdateAccount =>
              List(
                List(
                  genTxDetail("Ethereum Address", tx.ethAddress.get.toString),
                ),
              )
            case tx: AddPublicKeySummaries =>
              List(
                List(
                  genTxDetail(
                    "PublicKey Summary",
                    tx.summaries.keys.head.toBytes.toHex,
                  ),
                ),
              )

        case groupTx: Transaction.GroupTx =>
          groupTx match
            case tx: CreateGroup =>
              List(
                List(
                  genTxDetail("Group ID", tx.groupId.toString),
                  genTxDetail("Coordinator Account", tx.coordinator.toString),
                ),
              )
            case tx: AddAccounts =>
              List(
                List(
                  genTxDetail("Group ID", tx.groupId.toString),
                  genTxDetail("Account", tx.accounts.toList(0).toString()),
                ),
              )

        case rewardTx: Transaction.RewardTx =>
          rewardTx match
            case tx: OfferReward =>
              List(
                List(
                  genTxDetail("Definition ID", tx.tokenDefinitionId.toString),
                ),
                List(
                  genTxDetail_input_head,
                )
                  ++ {
                    tx.inputs.zipWithIndex
                      .map((a, i) => genTxDetail_inputs_body(a, i))
                      .toList
                      .pipe(a => a)
                  },
                List(
                  genTxDetail_output_head(1),
                )
                  ++
                    tx.outputs.zipWithIndex
                      .map((d, i) => genTxDetail_outputs_body_offer(d, i))
                      .toList,
              )
            case tx: RegisterDao =>
              List(
                List(
                  genTxDetail("Group ID", tx.groupId.toString),
                ),
              )
            case tx: UpdateDao =>
              List(
                List(
                  genTxDetail("Group ID", tx.groupId.toString),
                ),
              )
            case tx: RecordActivity =>
              List(
                List(
                ),
              )
            case tx: BuildSnapshot =>
              List(
                List(
                ),
              )
            case tx: ExecuteOwnershipReward =>
              List(
                List(
                ),
              )
            case tx: ExecuteReward =>
              List(
                List(
                ),
              )

        case agendaTx: Transaction.AgendaTx =>
          agendaTx match
            case tx: SuggestSimpleAgenda =>
              List(
                List(
                  genTxDetail("Agenda Title", tx.title.toString),
                  genTxDetail("Start", tx.voteStart.toString),
                  genTxDetail("End", tx.voteEnd.toString),
                ),
                List(
                  genTxDetail("vote option", "selected"),
                ) ++ {
                  tx.voteOptions
                    .map((a) => genTxDetail_inputs_body(a._1, a._2))
                    .toList
                },
              )
            case _tx: VoteSimpleAgenda =>
              List(
                List(
                  genTxDetail_hash(
                    "Agenda TxHash",
                    _tx.agendaTxHash.toUInt256Bytes.toHex,
                  ),
                  genTxDetail("Selected", _tx.selectedOption.toString),
                  genTxDetail_value(
                    "Voting Power",
                    tx.result
                      .map(d =>
                        d match
                          case d: VoteSimpleAgendaResult =>
                            d.votingAmount.toString().pipe(onlyNumber)
                          case _ => "",
                      )
                      .getOrElse(""),
                  ),
                ),
              ),
    )
    .getOrElse(
      List(
        List(
          genTxDetail(),
        ),
      ),
    )

  def view(data: TxDetail) =
    genTxDetailView((data.json.getOrElse("")))
      .map(d => d.map(d => genRow(d)))
      .map(genTable)

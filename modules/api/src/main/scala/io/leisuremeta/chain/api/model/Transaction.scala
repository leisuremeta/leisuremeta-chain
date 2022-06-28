package io.leisuremeta.chain
package api.model

import java.time.Instant

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.*
import scodec.bits.ByteVector

import lib.crypto.{CryptoOps, Hash, KeyPair, Recover, Sign}
import lib.codec.byte.{ByteDecoder, ByteEncoder}
import lib.datatype.{BigNat, UInt256Bytes, Utf8}
import offering.{VrfPublicKey}
import token.{Rarity, NftInfo, TokenDefinitionId, TokenDetail, TokenId}
import io.leisuremeta.chain.api.model.Transaction.TokenTx.MintFungibleToken
import io.leisuremeta.chain.api.model.Transaction.TokenTx.TransferFungibleToken

sealed trait TransactionResult
object TransactionResult:
  given txResultByteEncoder: ByteEncoder[TransactionResult] =
    (txr: TransactionResult) =>
      txr match
        case Transaction.AccountTx.AddPublicKeySummariesResult(removed) =>
          ByteVector.fromByte(0) ++ ByteEncoder[Map[PublicKeySummary, Utf8]]
            .encode(removed)
        case Transaction.TokenTx.AcceptDealResult(outputs) =>
          ByteVector.fromByte(1) ++ ByteEncoder[
            Map[Account, Map[TokenDefinitionId, TokenDetail]],
          ].encode(outputs)
        case Transaction.TokenTx.CancelSuggestionResult(defId, tokenDetail) =>
          ByteVector.fromByte(2)
            ++ ByteEncoder[TokenDefinitionId].encode(defId)
            ++ ByteEncoder[TokenDetail].encode(tokenDetail)
          
        case Transaction.RandomOfferingTx.JoinTokenOfferingResult(output) =>
          ByteVector.fromByte(3) ++ ByteEncoder[BigNat].encode(output)
        case Transaction.RandomOfferingTx.InitialTokenOfferingResult(
              totalOutputs,
            ) =>
          ByteVector.fromByte(4) ++ ByteEncoder[
            Map[Account, Map[TokenDefinitionId, BigNat]],
          ].encode(totalOutputs)

  given txResultByteDecoder: ByteDecoder[TransactionResult] =
    ByteDecoder.byteDecoder.flatMap {
      case 0 =>
        ByteDecoder[Map[PublicKeySummary, Utf8]].map(
          Transaction.AccountTx.AddPublicKeySummariesResult(_),
        )
      case 1 =>
        ByteDecoder[Map[Account, Map[TokenDefinitionId, TokenDetail]]].map(
          Transaction.TokenTx.AcceptDealResult(_),
        )
      case 2 =>
        for
          defId <- ByteDecoder[TokenDefinitionId]
          tokenDetail <- ByteDecoder[TokenDetail]
        yield Transaction.TokenTx.CancelSuggestionResult(defId, tokenDetail)
      case 3 =>
        ByteDecoder[BigNat].map(
          Transaction.RandomOfferingTx.JoinTokenOfferingResult(_),
        )
      case 4 =>
        ByteDecoder[Map[Account, Map[TokenDefinitionId, BigNat]]].map(
          Transaction.RandomOfferingTx.InitialTokenOfferingResult(_),
        )
    }

  given txResultCirceEncoder: Encoder[TransactionResult] =
    deriveEncoder[TransactionResult]

  given txResultCirceDecoder: Decoder[TransactionResult] =
    deriveDecoder[TransactionResult]

sealed trait Transaction:
  def networkId: NetworkId
  def createdAt: Instant

object Transaction:
  sealed trait AccountTx extends Transaction:
    def account: Account
  object AccountTx:
    final case class CreateAccount(
        networkId: NetworkId,
        createdAt: Instant,
        account: Account,
        ethAddress: Option[Utf8],
        guardian: Option[Account],
    ) extends AccountTx

    final case class UpdateAccount(
        networkId: NetworkId,
        createdAt: Instant,
        account: Account,
        ethAddress: Option[Utf8],
        guardian: Option[Account],
    ) extends AccountTx

    final case class AddPublicKeySummaries(
        networkId: NetworkId,
        createdAt: Instant,
        account: Account,
        summaries: Map[PublicKeySummary, Utf8],
    ) extends AccountTx

    final case class AddPublicKeySummariesResult(
        removed: Map[PublicKeySummary, Utf8],
    ) extends TransactionResult

//    final case class RemovePublicKeySummaries(
//        networkId: NetworkId,
//        createdAt: Instant,
//        account: Account,
//        summaries: Set[PublicKeySummary],
//    ) extends AccountTx
//
//    final case class RemoveAccount(
//        networkId: NetworkId,
//        createdAt: Instant,
//        account: Account,
//    ) extends AccountTx

    given txByteDecoder: ByteDecoder[AccountTx] = ByteDecoder[BigNat].flatMap {
      bignat =>
        bignat.toBigInt.toInt match
          case 0 => ByteDecoder[CreateAccount].widen
          case 1 => ByteDecoder[UpdateAccount].widen
          case 2 => ByteDecoder[AddPublicKeySummaries].widen
//          case 3 => ByteDecoder[RemovePublicKeySummaries].widen
//          case 4 => ByteDecoder[RemoveAccount].widen
    }
    given txByteEncoder: ByteEncoder[AccountTx] = (atx: AccountTx) =>
      atx match
        case tx: CreateAccount         => build(0)(tx)
        case tx: UpdateAccount         => build(1)(tx)
        case tx: AddPublicKeySummaries => build(2)(tx)
//        case tx: RemovePublicKeySummaries => build(3)(tx)
//        case tx: RemoveAccount            => build(4)(tx)
  end AccountTx

  sealed trait GroupTx extends Transaction
  object GroupTx:
    final case class CreateGroup(
        networkId: NetworkId,
        createdAt: Instant,
        groupId: GroupId,
        name: Utf8,
        coordinator: Account,
    ) extends GroupTx

//    final case class DisbandGroup(
//        networkId: NetworkId,
//        createdAt: Instant,
//        groupId: GroupId,
//    ) extends GroupTx

    final case class AddAccounts(
        networkId: NetworkId,
        createdAt: Instant,
        groupId: GroupId,
        accounts: Set[Account],
    ) extends GroupTx

//    final case class RemoveAccounts(
//        networkId: NetworkId,
//        createdAt: Instant,
//        groupId: GroupId,
//        accounts: Set[Account],
//    ) extends GroupTx

//    final case class ReplaceCoordinator(
//        networkId: NetworkId,
//        createdAt: Instant,
//        groupId: GroupId,
//        newCoordinator: Account,
//    ) extends GroupTx

    given txByteDecoder: ByteDecoder[GroupTx] = ByteDecoder[BigNat].flatMap {
      bignat =>
        bignat.toBigInt.toInt match
          case 0 => ByteDecoder[CreateGroup].widen
          case 2 => ByteDecoder[AddAccounts].widen
    }
    given txByteEncoder: ByteEncoder[GroupTx] = (atx: GroupTx) =>
      atx match
        case tx: CreateGroup => build(0)(tx)
        case tx: AddAccounts => build(2)(tx)
  end GroupTx

  sealed trait TokenTx extends Transaction
  object TokenTx:
    final case class DefineToken(
        networkId: NetworkId,
        createdAt: Instant,
        definitionId: TokenDefinitionId,
        name: Utf8,
        symbol: Option[Utf8],
        minterGroup: Option[GroupId],
        nftInfo: Option[NftInfo],
    ) extends TokenTx

    final case class MintFungibleToken(
        networkId: NetworkId,
        createdAt: Instant,
        definitionId: TokenDefinitionId,
        outputs: Map[Account, BigNat],
    ) extends TokenTx
        with FungibleBalance

    final case class MintNFT(
        networkId: NetworkId,
        createdAt: Instant,
        tokenDefinitionId: TokenDefinitionId,
        tokenId: TokenId,
        rarity: Rarity,
        dataUrl: Utf8,
        contentHash: UInt256Bytes,
        output: Account,
    ) extends TokenTx
        with NftBalance

//    final case class BurnNFT(
//        networkId: NetworkId,
//        createdAt: Instant,
//        definitionId: TokenDefinitionId,
//        input: Signed.TxHash,
//    ) extends TokenTx

    final case class TransferFungibleToken(
        networkId: NetworkId,
        createdAt: Instant,
        tokenDefinitionId: TokenDefinitionId,
        inputs: Set[Signed.TxHash],
        outputs: Map[Account, BigNat],
        memo: Option[Utf8],
    ) extends TokenTx
        with FungibleBalance

//    final case class TransferNft(
//        networkId: NetworkId,
//        createdAt: Instant,
//        definitionId: TokenDefinitionId,
//        tokenId: TokenId,
//        input: Signed.TxHash,
//        output: Account,
//        memo: Option[String],
//    ) extends TokenTx

    final case class SuggestFungibleTokenDeal(
        networkId: NetworkId,
        createdAt: Instant,
        originalSuggestion: Option[Signed.TxHash],
        inputDefinitionId: TokenDefinitionId,
        inputs: Set[Signed.TxHash],
        output: BigNat,
        dealDeadline: Instant,
        requirement: FungibleRequirement,
    ) extends TokenTx
        with DealSuggestion

    object SuggestFungibleTokenDeal:
      given txEncoder: Encoder[SuggestFungibleTokenDeal] = deriveEncoder

    case class FungibleRequirement(
        definitionId: TokenDefinitionId,
        amount: BigNat,
    )

//    final case class SuggestSellDeal(
//        networkId: NetworkId,
//        createdAt: Instant,
//        originalSuggestion: Option[Signed.TxHash],
//        inputDefinitionId: TokenDefinitionId,
//        input: Signed.TxHash,
//        dealDeadline: Instant,
//        requirement: FungibleRequirement,
//    ) extends TokenTx

//    final case class SuggestBuyDeal(
//        networkId: NetworkId,
//        createdAt: Instant,
//        originalSuggestion: Option[Signed.TxHash],
//        inputDefinitionId: TokenDefinitionId,
//        input: Signed.TxHash,
//        dealDeadline: Instant,
//        requirement: NftRequirement,
//    ) extends TokenTx

//    case class NftRequirement(
//        definitionID: TokenDefinitionId,
//        tokenID: TokenId,
//    )

//    final case class SuggestSwapDeal(
//        networkId: NetworkId,
//        createdAt: Instant,
//        originalSuggestion: Option[Signed.TxHash],
//        inputDefinitionId: TokenDefinitionId,
//        input: Set[Signed.TxHash],
//        dealDeadline: Instant,
//        requirement: NftRequirement,
//    ) extends TokenTx

    final case class AcceptDeal(
        networkId: NetworkId,
        createdAt: Instant,
        suggestion: Signed.TxHash,
        inputs: Set[Signed.TxHash],
    ) extends TokenTx
        with FungibleBalance

    final case class AcceptDealResult(
        outputs: Map[Account, Map[TokenDefinitionId, TokenDetail]],
    ) extends TransactionResult

    final case class CancelSuggestion(
        networkId: NetworkId,
        createdAt: Instant,
        suggestion: Signed.TxHash,
    ) extends TokenTx
        with FungibleBalance
        with NftBalance

    final case class CancelSuggestionResult(
        tokenDefinitionId: TokenDefinitionId,
        detail: TokenDetail,
    ) extends TransactionResult

    given txByteDecoder: ByteDecoder[TokenTx] = ByteDecoder[BigNat].flatMap {
      bignat =>
        bignat.toBigInt.toInt match
          case 0  => ByteDecoder[DefineToken].widen
          case 1  => ByteDecoder[MintFungibleToken].widen
          case 2  => ByteDecoder[MintNFT].widen
          case 4  => ByteDecoder[TransferFungibleToken].widen
          case 6  => ByteDecoder[SuggestFungibleTokenDeal].widen
          case 10 => ByteDecoder[AcceptDeal].widen
          case 11 => ByteDecoder[CancelSuggestion].widen

    }
    given txByteEncoder: ByteEncoder[TokenTx] = (ttx: TokenTx) =>
      ttx match
        case tx: DefineToken              => build(0)(tx)
        case tx: MintFungibleToken        => build(1)(tx)
        case tx: MintNFT                  => build(2)(tx)
        case tx: TransferFungibleToken    => build(4)(tx)
        case tx: SuggestFungibleTokenDeal => build(6)(tx)
        case tx: AcceptDeal               => build(10)(tx)
        case tx: CancelSuggestion         => build(11)(tx)

    given txCirceDecoder: Decoder[TokenTx] = deriveDecoder
    given txCirceEncoder: Encoder[TokenTx] = deriveEncoder

  end TokenTx

  sealed trait RandomOfferingTx extends Transaction
  object RandomOfferingTx:
    final case class NoticeTokenOffering(
        networkId: NetworkId,
        createdAt: Instant,
        groupId: GroupId,
        offeringAccount: Account,
        feeReceivingAccount: Account,
        feeRatePerMille: BigNat,
        tokenDefinitionId: TokenDefinitionId,
        vrfPublicKey: VrfPublicKey,
        autojoin: Map[Account, BigNat],
        inputs: Set[Signed.TxHash],
        requirement: Option[(TokenDefinitionId, BigNat)],
        claimStartDate: Instant,
        note: Utf8,
    ) extends RandomOfferingTx

    final case class JoinTokenOffering(
        networkId: NetworkId,
        createdAt: Instant,
        noticeTxHash: Signed.TxHash,
        amount: BigNat,
        inputTokenDefinitionId: TokenDefinitionId,
        inputs: Set[Signed.TxHash],
    ) extends RandomOfferingTx
        with FungibleBalance

    final case class JoinTokenOfferingResult(
        outout: BigNat,
    ) extends TransactionResult

    final case class InitialTokenOffering(
        networkId: NetworkId,
        createdAt: Instant,
        noticeTxHash: Signed.TxHash,
        outputs: Map[Account, BigNat],
    ) extends RandomOfferingTx
        with FungibleBalance

    final case class InitialTokenOfferingResult(
        totalOutputs: Map[Account, Map[TokenDefinitionId, BigNat]],
    ) extends TransactionResult

    given txByteDecoder: ByteDecoder[RandomOfferingTx] =
      ByteDecoder[BigNat].flatMap { bignat =>
        bignat.toBigInt.toInt match
          case 0 => ByteDecoder[NoticeTokenOffering].widen
          case 1 => ByteDecoder[JoinTokenOffering].widen
          case 2 => ByteDecoder[InitialTokenOffering].widen
      }
    given txByteEncoder: ByteEncoder[RandomOfferingTx] =
      (tx: RandomOfferingTx) =>
        tx match
          case tx: NoticeTokenOffering  => build(0)(tx)
          case tx: JoinTokenOffering    => build(1)(tx)
          case tx: InitialTokenOffering => build(2)(tx)
  end RandomOfferingTx

  private def build[A: ByteEncoder](discriminator: Long)(tx: A): ByteVector =
    ByteEncoder[BigNat].encode(
      BigNat.unsafeFromLong(discriminator),
    ) ++ ByteEncoder[A].encode(tx)

  given txByteDecoder: ByteDecoder[Transaction] = ByteDecoder[BigNat].flatMap {
    bignat =>
      bignat.toBigInt.toInt match
        case 0 => ByteDecoder[AccountTx].widen
        case 1 => ByteDecoder[GroupTx].widen
        case 2 => ByteDecoder[TokenTx].widen
        case 4 => ByteDecoder[RandomOfferingTx].widen
  }
  given txByteEncoder: ByteEncoder[Transaction] = (tx: Transaction) =>
    tx match
      case tx: AccountTx        => build(0)(tx)
      case tx: GroupTx          => build(1)(tx)
      case tx: TokenTx          => build(2)(tx)
      case tx: RandomOfferingTx => build(4)(tx)

  given txHash: Hash[Transaction] = Hash.build

  given txSign: Sign[Transaction] = Sign.build

  given txRecover: Recover[Transaction] = Recover.build

  given txCirceDecoder: Decoder[Transaction] = deriveDecoder
  given txCirceEncoder: Encoder[Transaction] = deriveEncoder

  /*
  sealed trait AgendaTx extends Transaction
  object AgendaTx:
    final case class Suggest(
        networkId: NetworkId,
        createdAt: Instant,
    ) extends AgendaTx

    final case class Vote(
        networkId: NetworkId,
        createdAt: Instant,
    ) extends AgendaTx

    final case class Finalize(
        networkId: NetworkId,
        createdAt: Instant,
    ) extends AgendaTx
  end AgendaTx

   */

  sealed trait FungibleBalance

  sealed trait NftBalance

  sealed trait DealSuggestion:
    def originalSuggestion: Option[Signed.TxHash]

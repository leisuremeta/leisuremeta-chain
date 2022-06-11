package io.leisuremeta.chain
package api.model

import java.time.Instant

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.*
import scodec.bits.ByteVector

import lib.crypto.{CryptoOps, Hash, KeyPair, Recover, Sign}
import lib.codec.byte.{ByteDecoder, ByteEncoder}
import lib.datatype.{BigNat, UInt256Bytes, Utf8}
import token.{Rarity, NftInfo, TokenDefinitionId, TokenId}

sealed trait TransactionResult
object TransactionResult:
  given txResultByteEncoder: ByteEncoder[TransactionResult] =
    (txr: TransactionResult) =>
      txr match
        case Transaction.AccountTx.AddPublicKeySummariesResult(removed) =>
          ByteVector.fromByte(0) ++ ByteEncoder[Map[PublicKeySummary, Utf8]]
            .encode(removed)

  given txResultByteDecoder: ByteDecoder[TransactionResult] =
    ByteDecoder.byteDecoder.flatMap { case 0 =>
      ByteDecoder[Map[PublicKeySummary, Utf8]].map(
        Transaction.AccountTx.AddPublicKeySummariesResult(_),
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
          case 1 => ByteDecoder[AddPublicKeySummaries].widen
//          case 2 => ByteDecoder[RemovePublicKeySummaries].widen
//          case 3 => ByteDecoder[RemoveAccount].widen
    }
    given txByteEncoder: ByteEncoder[AccountTx] = (atx: AccountTx) =>
      atx match
        case tx: CreateAccount         => build(0)(tx)
        case tx: AddPublicKeySummaries => build(1)(tx)
//        case tx: RemovePublicKeySummaries => build(2)(tx)
//        case tx: RemoveAccount            => build(3)(tx)
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
    /*
//    final case class BurnNFT(
//        networkId: NetworkId,
//        createdAt: Instant,
//        definitionId: TokenDefinitionId,
//        input: Signed.TxHash,
//    ) extends TokenTx

    final case class TransferFungibleToken(
        networkId: NetworkId,
        createdAt: Instant,
        definitionId: TokenDefinitionId,
        inputs: Set[Signed.TxHash],
        outputs: Map[Account, BigNat],
        memo: Option[String],
    ) extends TokenTx

    final case class TransferNft(
        networkId: NetworkId,
        createdAt: Instant,
        definitionId: TokenDefinitionId,
        tokenId: TokenId,
        input: Signed.TxHash,
        output: Account,
        memo: Option[String],
    ) extends TokenTx

//    final case class SuggestFungibleTokenDeal(
//        networkId: NetworkId,
//        createdAt: Instant,
//        originalSuggestion: Option[Signed.TxHash],
//        inputDefinitionId: TokenDefinitionId,
//        inputs: Set[Signed.TxHash],
//        output: BigNat,
//        dealDeadline: Instant,
//        requirement: FungibleRequirement,
//    ) extends TokenTx

    case class FungibleRequirement(
        definitionID: TokenDefinitionId,
        amount: BigNat,
    )

    final case class SuggestSellDeal(
        networkId: NetworkId,
        createdAt: Instant,
        originalSuggestion: Option[Signed.TxHash],
        inputDefinitionId: TokenDefinitionId,
        input: Signed.TxHash,
        dealDeadline: Instant,
        requirement: FungibleRequirement,
    ) extends TokenTx

    final case class SuggestBuyDeal(
        networkId: NetworkId,
        createdAt: Instant,
        originalSuggestion: Option[Signed.TxHash],
        inputDefinitionId: TokenDefinitionId,
        input: Signed.TxHash,
        dealDeadline: Instant,
        requirement: NftRequirement,
    ) extends TokenTx

    case class NftRequirement(
        definitionID: TokenDefinitionId,
        tokenID: TokenId,
    )

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
    ) extends TokenTx

    final case class CancelSuggestion(
        networkId: NetworkId,
        createdAt: Instant,
    ) extends TokenTx
     */
    given txByteDecoder: ByteDecoder[TokenTx] = ByteDecoder[BigNat].flatMap {
      bignat =>
        bignat.toBigInt.toInt match
          case 0 => ByteDecoder[DefineToken].widen
          case 1 => ByteDecoder[MintFungibleToken].widen
          case 2 => ByteDecoder[MintNFT].widen
//          case _ => ???
    }
    given txByteEncoder: ByteEncoder[TokenTx] = (ttx: TokenTx) =>
      ttx match
        case tx: DefineToken       => build(0)(tx)
        case tx: MintFungibleToken => build(1)(tx)
        case tx: MintNFT           => build(2)(tx)
//        case _                     => ???

  end TokenTx

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
  }
  given txByteEncoder: ByteEncoder[Transaction] = (tx: Transaction) =>
    tx match
      case tx: AccountTx => build(0)(tx)
      case tx: GroupTx   => build(1)(tx)
      case tx: TokenTx   => build(2)(tx)

  given txHash: Hash[Transaction] = Hash.build

  given txSign: Sign[Transaction] = Sign.build

  given txRecover: Recover[Transaction] = Recover.build

/*
  sealed trait RandomOfferingTx extends Transaction
  object RandomOfferingTx:
    final case class Notice(
        networkId: NetworkId,
        createdAt: Instant,
    ) extends RandomOfferingTx

    final case class InitialTokenOffering(
        networkId: NetworkId,
        createdAt: Instant,
    ) extends RandomOfferingTx

    final case class ClaimToken(
        networkId: NetworkId,
        createdAt: Instant,
    ) extends RandomOfferingTx
  end RandomOfferingTx

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

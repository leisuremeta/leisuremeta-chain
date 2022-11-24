package io.leisuremeta.chain
package api.model

import java.time.Instant

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.*
import scodec.bits.ByteVector

import account.EthAddress
import reward.DaoActivity
import lib.crypto.{CryptoOps, Hash, KeyPair, Recover, Sign}
import lib.codec.byte.{ByteDecoder, ByteEncoder}
import lib.datatype.{BigNat, UInt256Bytes, Utf8}
import token.{Rarity, NftInfo, TokenDefinitionId, TokenDetail, TokenId}

sealed trait TransactionResult
object TransactionResult:
  given txResultByteEncoder: ByteEncoder[TransactionResult] =
    (txr: TransactionResult) =>
      txr match
        case Transaction.AccountTx.AddPublicKeySummariesResult(removed) =>
          ByteVector.fromByte(0) ++ ByteEncoder[Map[PublicKeySummary, Utf8]]
            .encode(removed)
        case Transaction.TokenTx.BurnFungibleTokenResult(outputAmount) =>
          ByteVector.fromByte(1) ++ ByteEncoder[BigNat].encode(outputAmount)
        case Transaction.TokenTx.EntrustFungibleTokenResult(remainder) =>
          ByteVector.fromByte(2) ++ ByteEncoder[BigNat].encode(remainder)
        case Transaction.RewardTx.ExecuteRewardResult(outputs) =>
          ByteVector.fromByte(3) ++ ByteEncoder[Map[Account, BigNat]].encode(outputs)

  given txResultByteDecoder: ByteDecoder[TransactionResult] =
    ByteDecoder.byteDecoder.flatMap {
      case 0 =>
        ByteDecoder[Map[PublicKeySummary, Utf8]].map(
          Transaction.AccountTx.AddPublicKeySummariesResult(_),
        )
      case 1 =>
        ByteDecoder[BigNat].map(Transaction.TokenTx.BurnFungibleTokenResult(_))
      case 2 =>
        ByteDecoder[BigNat].map(
          Transaction.TokenTx.EntrustFungibleTokenResult(_),
        )
      case 3 =>
        ByteDecoder[Map[Account, BigNat]].map(
          Transaction.RewardTx.ExecuteRewardResult(_),
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
        ethAddress: Option[EthAddress],
        guardian: Option[Account],
    ) extends AccountTx

    final case class UpdateAccount(
        networkId: NetworkId,
        createdAt: Instant,
        account: Account,
        ethAddress: Option[EthAddress],
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

    final case class BurnFungibleToken(
        networkId: NetworkId,
        createdAt: Instant,
        definitionId: TokenDefinitionId,
        amount: BigNat,
        inputs: Set[Signed.TxHash],
    ) extends TokenTx
        with FungibleBalance

    final case class BurnFungibleTokenResult(
        outputAmount: BigNat,
    ) extends TransactionResult

    final case class BurnNFT(
        networkId: NetworkId,
        createdAt: Instant,
        definitionId: TokenDefinitionId,
        input: Signed.TxHash,
    ) extends TokenTx

    final case class TransferFungibleToken(
        networkId: NetworkId,
        createdAt: Instant,
        tokenDefinitionId: TokenDefinitionId,
        inputs: Set[Signed.TxHash],
        outputs: Map[Account, BigNat],
        memo: Option[Utf8],
    ) extends TokenTx
        with FungibleBalance

    final case class TransferNFT(
        networkId: NetworkId,
        createdAt: Instant,
        definitionId: TokenDefinitionId,
        tokenId: TokenId,
        input: Signed.TxHash,
        output: Account,
        memo: Option[Utf8],
    ) extends TokenTx
        with NftBalance

    final case class EntrustFungibleToken(
        networkId: NetworkId,
        createdAt: Instant,
        definitionId: TokenDefinitionId,
        amount: BigNat,
        inputs: Set[Signed.TxHash],
        to: Account,
    ) extends TokenTx
        with FungibleBalance

    final case class EntrustFungibleTokenResult(
        remainder: BigNat,
    ) extends TransactionResult

    final case class EntrustNFT(
        networkId: NetworkId,
        createdAt: Instant,
        definitionId: TokenDefinitionId,
        tokenId: TokenId,
        input: Signed.TxHash,
        to: Account,
    ) extends TokenTx

    final case class DisposeEntrustedFungibleToken(
        networkId: NetworkId,
        createdAt: Instant,
        definitionId: TokenDefinitionId,
        inputs: Set[Signed.TxHash],
        outputs: Map[Account, BigNat],
    ) extends TokenTx
        with FungibleBalance

    final case class DisposeEntrustedNFT(
        networkId: NetworkId,
        createdAt: Instant,
        definitionId: TokenDefinitionId,
        tokenId: TokenId,
        input: Signed.TxHash,
        output: Option[Account],
    ) extends TokenTx
        with NftBalance

    given txByteDecoder: ByteDecoder[TokenTx] = ByteDecoder[BigNat].flatMap {
      bignat =>
        bignat.toBigInt.toInt match
          case 0  => ByteDecoder[DefineToken].widen
          case 1  => ByteDecoder[MintFungibleToken].widen
          case 2  => ByteDecoder[MintNFT].widen
          case 4  => ByteDecoder[TransferFungibleToken].widen
          case 5  => ByteDecoder[TransferNFT].widen
          case 6  => ByteDecoder[BurnFungibleToken].widen
          case 7  => ByteDecoder[BurnNFT].widen
          case 8  => ByteDecoder[EntrustFungibleToken].widen
          case 9  => ByteDecoder[EntrustNFT].widen
          case 10 => ByteDecoder[DisposeEntrustedFungibleToken].widen
          case 11 => ByteDecoder[DisposeEntrustedNFT].widen
    }

    given txByteEncoder: ByteEncoder[TokenTx] = (ttx: TokenTx) =>
      ttx match
        case tx: DefineToken                   => build(0)(tx)
        case tx: MintFungibleToken             => build(1)(tx)
        case tx: MintNFT                       => build(2)(tx)
        case tx: TransferFungibleToken         => build(4)(tx)
        case tx: TransferNFT                   => build(5)(tx)
        case tx: BurnFungibleToken             => build(6)(tx)
        case tx: BurnNFT                       => build(7)(tx)
        case tx: EntrustFungibleToken          => build(8)(tx)
        case tx: EntrustNFT                    => build(9)(tx)
        case tx: DisposeEntrustedFungibleToken => build(10)(tx)
        case tx: DisposeEntrustedNFT           => build(11)(tx)

    given txCirceDecoder: Decoder[TokenTx] = deriveDecoder
    given txCirceEncoder: Encoder[TokenTx] = deriveEncoder

  end TokenTx

  sealed trait RewardTx extends Transaction
  object RewardTx:
    final case class RegisterDao(
        networkId: NetworkId,
        createdAt: Instant,
        groupId: GroupId,
        daoAccountName: Account,
        moderators: Set[Account],
    ) extends RewardTx

    final case class UpdateDao(
        networkId: NetworkId,
        createdAt: Instant,
        groupId: GroupId,
        moderators: Set[Account],
    ) extends RewardTx

    final case class RecordActivity(
        networkId: NetworkId,
        createdAt: Instant,
        timestamp: Instant,
        userActivity: Map[Account, DaoActivity],
        tokenReceived: Map[TokenId, DaoActivity],
    ) extends RewardTx

    final case class RegisterStaking(
        networkId: NetworkId,
        createdAt: Instant,
        inputs: Set[Signed.TxHash],
        outputs: Map[Account, BigNat],
    ) extends RewardTx

    final case class RemoveStaking(
        networkId: NetworkId,
        createdAt: Instant,
        inputs: Set[Signed.TxHash],
        outputs: Map[Account, BigNat],
    ) extends RewardTx

//    final case class ExcuteStakingRequest(
//        networkId: NetworkId,
//        createdAt: Instant,
//    ) extends RewardTx
//    final case class ExecuteReward(
//        networkId: NetworkId,
//        createdAt: Instant,
//    ) extends RewardTx

    final case class ExecuteReward(
        networkId: NetworkId,
        createdAt: Instant,
        daoAccount: Option[Account],
    ) extends RewardTx
        with FungibleBalance

    final case class ExecuteRewardResult(
        outputs: Map[Account, BigNat],
    ) extends TransactionResult

    given txByteDecoder: ByteDecoder[RewardTx] = ByteDecoder[BigNat].flatMap {
      bignat =>
        bignat.toBigInt.toInt match
          case 0 => ByteDecoder[RegisterDao].widen
          case 1 => ByteDecoder[UpdateDao].widen
          case 2 => ByteDecoder[RecordActivity].widen
          case 3 => ByteDecoder[RegisterStaking].widen
          case 4 => ByteDecoder[RemoveStaking].widen
//          case 5 => ByteDecoder[ExcuteStakingRequest].widen
          case 6 => ByteDecoder[ExecuteReward].widen
    }

    given txByteEncoder: ByteEncoder[RewardTx] = (rtx: RewardTx) =>
      rtx match
        case tx: RegisterDao     => build(0)(tx)
        case tx: UpdateDao       => build(1)(tx)
        case tx: RecordActivity  => build(2)(tx)
        case tx: RegisterStaking => build(3)(tx)
        case tx: RemoveStaking   => build(4)(tx)
//        case tx: ExcuteStakingRequest => build(5)(tx)
        case tx: ExecuteReward        => build(6)(tx)

    given txCirceDecoder: Decoder[RewardTx] = deriveDecoder
    given txCirceEncoder: Encoder[RewardTx] = deriveEncoder

  end RewardTx

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
        case 3 => ByteDecoder[RewardTx].widen
  }
  given txByteEncoder: ByteEncoder[Transaction] = (tx: Transaction) =>
    tx match
      case tx: AccountTx => build(0)(tx)
      case tx: GroupTx   => build(1)(tx)
      case tx: TokenTx   => build(2)(tx)
      case tx: RewardTx  => build(3)(tx)

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

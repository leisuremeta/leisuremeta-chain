package io.leisuremeta.chain
package api.model

import java.time.Instant

import scodec.bits.ByteVector

import lib.crypto.Hash
import lib.codec.byte.{ByteDecoder, ByteEncoder}
import lib.datatype.{BigNat, Utf8}
import io.leisuremeta.chain.api.model.Transaction.AccountTx.CreateAccount

sealed trait Transaction:
  def networkId: NetworkId
  def createdAt: Instant

trait TransactionResult

object Transaction:
  sealed trait AccountTx extends Transaction
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

    final case class RemovePublicKeySummaries(
        networkId: NetworkId,
        createdAt: Instant,
        account: Account,
        summaries: Set[PublicKeySummary],
    ) extends AccountTx

    final case class RemoveAccount(
        networkId: NetworkId,
        createdAt: Instant,
        account: Account,
    ) extends AccountTx

    given txByteDecoder: ByteDecoder[AccountTx] = ByteDecoder[BigNat].flatMap {
      bignat =>
        bignat.toBigInt.toInt match
          case 0 => ByteDecoder[CreateAccount].widen
          case 1 => ByteDecoder[AddPublicKeySummaries].widen
          case 2 => ByteDecoder[RemovePublicKeySummaries].widen
          case 3 => ByteDecoder[RemoveAccount].widen
    }
    given txByteEncoder: ByteEncoder[AccountTx] = (atx: AccountTx) =>
      atx match
        case tx: CreateAccount            => build(0)(tx)
        case tx: AddPublicKeySummaries    => build(1)(tx)
        case tx: RemovePublicKeySummaries => build(2)(tx)
        case tx: RemoveAccount            => build(3)(tx)
  end AccountTx

  private def build[A: ByteEncoder](discriminator: Long)(tx: A): ByteVector =
    ByteEncoder[BigNat].encode(
      BigNat.unsafeFromLong(discriminator),
    ) ++ ByteEncoder[A].encode(tx)

  given txByteDecoder: ByteDecoder[Transaction] = ByteDecoder[BigNat].flatMap {
    bignat =>
      bignat.toBigInt.toInt match
        case 0 => ByteDecoder[AccountTx].widen
  }
  given txByteEncoder: ByteEncoder[Transaction] = (tx: Transaction) =>
    tx match
      case tx: AccountTx => build(0)(tx)

  given txHash: Hash[Transaction] = Hash.build

package io.leisuremeta.chain
package bulkinsert

import cats.effect.{Async, Resource}
import cats.effect.std.Console

import api.model.{Account, Transaction}
import api.model.token.{TokenId}
import lib.datatype.BigNat
import lib.crypto.Hash.ops.*

final case class InvalidTx(
  signer: Account,
  reason: InvalidReason,
  amountToBurn: BigNat,
  tx: Transaction,
  wrongNftInput: Option[TokenId] = None,
  memo: String = "",
):
  def txType: String = tx.getClass.getSimpleName

enum InvalidReason:
  case OutputMoreThanInput, InputAlreadyUsed, BalanceNotExist

trait InvalidTxLogger[F[_]]:
  def log(invalidTx: InvalidTx): F[Unit]

object InvalidTxLogger:
  def apply[F[_]: InvalidTxLogger]: InvalidTxLogger[F] = summon

  def console[F[_]: Async: Console]: InvalidTxLogger[F] =
    invalidTx =>
      Console[F].println(invalidTx)

  def file[F[_]: Async](filename: String): Resource[F, InvalidTxLogger[F]] =
    Resource
      .make:
        import java.io.{File, FileOutputStream, PrintWriter}
        Async[F].delay(new PrintWriter(new FileOutputStream(new File(filename), true)))
      .apply: out =>
        Async[F].delay:
          out.flush()
          out.close()
      .map: out =>
        case InvalidTx(signer, reason, amountToBurn, tx, wrongNftInput, memo) =>
          Async[F].delay:
            out.println(s"$signer,$reason,$amountToBurn,${tx.getClass.getSimpleName},${tx.toHash}, ${wrongNftInput}, $memo")
            out.flush()

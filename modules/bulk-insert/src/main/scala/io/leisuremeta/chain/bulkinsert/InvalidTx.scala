package io.leisuremeta.chain
package bulkinsert

import cats.effect.{Async, Resource}
import cats.effect.std.Console

import api.model.{Account, Transaction}
import lib.datatype.BigNat
import lib.crypto.Hash.ops.*

final case class InvalidTx(
  signer: Account,
  reason: InvalidReason,
  amountToBurn: BigNat,
  tx: Transaction,
):
  def txType: String = tx.getClass.getSimpleName

enum InvalidReason:
  case OutputMoreThanInput, InputAlreadyUsed

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
        import java.io.PrintWriter
        Async[F].delay(new PrintWriter(filename))
      .apply: out =>
        Async[F].delay:
          out.flush()
          out.close()
      .map: out =>
        case InvalidTx(signer, reason, amountToBurn, tx) =>
          Async[F].delay:
            out.println(s"$signer,$reason,$amountToBurn,${tx.getClass.getSimpleName},${tx.toHash}")
            out.flush()

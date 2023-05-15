package io.leisuremeta.chain
package api.model

import lib.crypto.Hash

final case class TransactionWithResult (
    signedTx: Signed.Tx,
    result: Option[TransactionResult],
)

object TransactionWithResult:
  
  @SuppressWarnings(Array("org.wartremover.warts.Overloading"))
  inline def apply[Tx <: Transaction](signedTx: Signed[Tx])(
      inline resultOption: Option[TransactionResult],
  ): TransactionWithResult =

    def widenTx = signedTx.asInstanceOf[Signed.Tx]

    import scala.compiletime.*

    inline signedTx.value match
      case ap: Transaction.AccountTx.AddPublicKeySummaries =>
        inline resultOption match
          case Some(Transaction.AccountTx.AddPublicKeySummariesResult(removed)) =>
            TransactionWithResult(widenTx, resultOption)
          case other =>
            error("wrong result type: expected AddPublicKeySummariesResult")
        
      case bt: Transaction.TokenTx.BurnFungibleToken =>
        inline resultOption match
          case Some(Transaction.TokenTx.BurnFungibleTokenResult(amount)) =>
            TransactionWithResult(widenTx, resultOption)
          case other =>
            error("wrong result type: expected BurnFungibleTokenResult")
      case bt: Transaction.TokenTx.EntrustFungibleToken =>
        inline resultOption match
          case Some(Transaction.TokenTx.EntrustFungibleTokenResult(amount)) =>
            TransactionWithResult(widenTx, resultOption)
          case other =>
            error("wrong result type: expected EntrustFungibleTokenResult")
      case xr: Transaction.RewardTx.ExecuteReward =>
        inline resultOption match
          case Some(Transaction.RewardTx.ExecuteRewardResult(outputs)) =>
            TransactionWithResult(widenTx, resultOption)
          case other =>
            error("wrong result type: expected ExecuteRewardResult")
      case _ =>
        inline resultOption match
          case None =>
            TransactionWithResult(widenTx, resultOption)
          case other =>
            error(
              "wrong result type: expected None but " + codeOf(resultOption),
            )

  given Hash[TransactionWithResult] =
    Hash[Transaction].contramap(_.signedTx.value)

  object ops:
    extension [A](txHash: Hash.Value[A])
      def toResultHashValue: Hash.Value[TransactionWithResult] =
        Hash.Value[TransactionWithResult](txHash.toUInt256Bytes)
      def toSignedTxHash: Hash.Value[Signed.Tx] =
        Hash.Value[Signed.Tx](txHash.toUInt256Bytes)

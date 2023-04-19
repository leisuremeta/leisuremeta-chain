package io.leisuremeta.chain.node.proxy.model

import io.circe._
import io.circe.generic.semiauto
import io.circe.generic.auto

case class TxModel (
  signedTx: String,
  result: Option[String]
)

object TxModel {
  implicit val txModelDecoder: Decoder[TxModel] = new Decoder[TxModel] {
    final def apply(c: HCursor): Decoder.Result[TxModel] = {
      for {
        signedTxJson <- c.downField("signedTx").as[Json]
        signedTx = signedTxJson.noSpaces
        result <- c.downField("result").as[Option[String]].orElse(Right(None))
      } yield {
        TxModel(signedTx, result)
      }
    }
  }
}

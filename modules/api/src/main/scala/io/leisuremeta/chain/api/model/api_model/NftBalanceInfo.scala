package io.leisuremeta.chain
package api.model
package api_model

import io.circe.generic.semiauto.*

import token.TokenDefinitionId
import lib.crypto.Hash
import lib.datatype.Utf8

final case class NftBalanceInfo(
  tokenDefinitionId: TokenDefinitionId,
  txHash: Hash.Value[TransactionWithResult],
  tx: TransactionWithResult,
  memo: Option[Utf8],
)

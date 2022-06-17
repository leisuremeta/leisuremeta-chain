package io.leisuremeta.chain.api.model
package api_model

import token.{TokenDefinitionId, TokenId}

final case class RandomOfferingInfo(
    tokenDefinitionId: TokenDefinitionId,
    noticeTxHash: Signed.TxHash,
    noticeTx: Transaction.RandomOfferingTx.NoticeTokenOffering,
    currentBalance: Map[TokenId, NftBalanceInfo],
)

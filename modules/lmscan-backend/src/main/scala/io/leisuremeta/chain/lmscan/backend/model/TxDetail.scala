package io.leisuremeta.chain.lmscan.backend.model

final case class TxDetail(
    hash: String,
    createdAt: Long,
    signer: String,
    txType: String,
    tokenType: String,
    inputHashs: Option[Seq[String]],
    transferHist: Option[Seq[TransferHist]],
    json: String,
)

final case class TransferHist(
    toAddress: String,
    value: String,
)

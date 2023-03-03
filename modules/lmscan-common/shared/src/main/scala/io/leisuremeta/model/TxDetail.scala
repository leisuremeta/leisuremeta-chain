package io.leisuremeta.chain.lmscan.common.model

final case class TxDetail(
    hash: Option[String] = None,
    createdAt: Option[Long] = None,
    signer: Option[String] = None,
    txType: Option[String] = None,
    tokenType: Option[String] = None,
    inputHashs: Option[Seq[String]] = None,
    transferHist: Option[Seq[TransferHist]] = None,
    json: Option[String] = None,
)

final case class TransferHist(
    toAddress: Option[String] = None,
    value: Option[String] = None,
)

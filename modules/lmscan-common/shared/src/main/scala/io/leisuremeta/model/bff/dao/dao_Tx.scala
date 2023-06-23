package io.leisuremeta.chain.lmscan.common.model.dao

// import io.getquill.Quoted

// final case class Tx(
//     hash: String,
//     txType: String, // col_name : type
//     fromAddr: String,
//     toAddr: Seq[String],
//     blockHash: String,
//     eventTime: Long,
//     createdAt: Long,
//     tokenType: String,
//     outputVals: Option[Seq[String]],
//     json: String,
//     blockNumber: Long,
//     inputHashs: Option[Seq[String]],
//     subType: String,
// )
final case class Tx(
    hash: String,
    txType: String, // col_name : type
    fromAddr: String,
    toAddr: Option[String] = None,
    blockHash: String,
    eventTime: Long,
    createdAt: Long,
    tokenType: String,
    outputVals: Option[String],
    json: String,
    blockNumber: Long,
    inputHashs: Option[String],
    amount: Option[Double],
    subType: String,
    displayYn: Boolean,
) extends DAO

// 옵션
// final case class Tx(
//     hash: Option[String] = None,
//     txType: Option[String] = None,
//     fromAddr: Option[String] = None,
//     toAddr: Option[String] = None,
//     blockHash: Option[String] = None,
//     eventTime: Option[Long] = None,
//     createdAt: Option[Long] = None,
//     tokenType: Option[String] = None,
//     outputVals: Option[String] = None,
//     json: Option[String] = None,
//     blockNumber: Option[Long] = None,
//     inputHashs: Option[String] = None,
//     amount: Option[Double] = None,
//     subType: Option[String] = None,
//     displayYn: Option[Boolean] = None,
// )

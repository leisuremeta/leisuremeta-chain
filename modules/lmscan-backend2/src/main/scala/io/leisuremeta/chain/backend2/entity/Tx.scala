package io.leisuremeta.chain.lmscan.backend2.entity

// import io.getquill.Quoted

final case class Tx(
    hash: String,
    txType: String, // col_name : type
    fromAddr: String,
    toAddr: Seq[String],
    blockHash: String,
    eventTime: Long,
    createdAt: Long,
    tokenType: String,
    // outputVals: Option[Seq[String]],
    outputVals: Seq[String],
    json: String,
    blockNumber: Long,
    // inputHashs: Seq[String],
    inputHashs: Seq[String],
    amount: Double,
    subType: String,
    displayYn: Boolean,
)

// final case class Tx(
//     hash: String = "",
//     txType: String = "", // col_name : type
//     fromAddr: String = "",
//     toAddr: Seq[String] = List(""),
//     blockHash: String = "",
//     eventTime: Long = 0,
//     createdAt: Long = 0,
//     tokenType: String = "",
//     outputVals: Option[Seq[String]] = Some(List("")),
//     json: String = "",
//     blockNumber: Long = 0,
//     inputHashs: Option[Seq[String]] = Some(List("")),
//     amount: Option[Double] = Some(0),
//     subType: String = "",
//     displayYn: Boolean = true,
// )

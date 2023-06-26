package io.leisuremeta.chain.lmscan.common.model

object DTO:
  object Tx:
    final case class type1(
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
    )
    final case class type2(
        hash: Option[String] = None,
        txType: Option[String] = None,
        createdAt: Option[Long] = None,
        tokenType: Option[String] = None,
        outputVals: Option[String] = None,
        blockNumber: Option[Long] = None,
        inputHashs: Option[String] = None,
        amount: Option[Double] = None,
        subType: Option[String] = None,
    )
    final case class count(
        count: Long = 0,
    )

  object Account:
    final case class Detail(
        address: Option[String] = None,
        balance: Option[BigDecimal] = None,
        value: Option[BigDecimal] = None,
        txList: Option[List[DTO.Tx.type1]] = None,
    )

    final case class Account(
        address: Option[String] = None,
        createdAt: Option[Long] = None,
        eventTime: Option[Long] = None,
        balance: Option[BigDecimal] = None,
        amount: Option[BigDecimal] = None,
    )

    // final case class Block(
//     number: Long,
//     hash: String,
//     parentHash: String,
//     txCount: Long,
//     eventTime: Long,
//     createdAt: Long,
// )

// final case class Nft(
//     tokenId: String,
//     txHash: String,
//     action: String,
//     fromAddr: String,
//     toAddr: String,
//     eventTime: Long,
//     createdAt: Long,
// )

// // final case class NftActivity(
// //     txHash: String,
// //     action: String,
// //     fromAddr: String,
// //     toAddr: String,
// //     createdAt: Long,
// // )

// final case class NftFile(
//     tokenId: String,
//     tokenDefId: String,
//     collectionName: String,
//     nftName: String,
//     nftUri: String,
//     creatorDescription: String,
//     dataUrl: String,
//     rarity: String,
//     creator: String,
//     eventTime: Long,
//     createdAt: Long,
//     owner: String,
// )

// final case class Summary(
//     id: Long,
//     lmPrice: Double,
//     blockNumber: Long,
//     totalTxSize: Long,
//     totalAccounts: Long,
//     createdAt: Long,
// )

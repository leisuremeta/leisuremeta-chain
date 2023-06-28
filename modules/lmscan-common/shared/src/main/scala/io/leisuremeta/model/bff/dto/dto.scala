package io.leisuremeta.chain.lmscan.common.model

object DTO:
  object Tx:
    final case class Tx_self(
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
    final case class Tx_Type2(
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
        txList: Option[List[DTO.Tx.Tx_self]] = None,
    )

    final case class Account(
        address: Option[String] = None,
        createdAt: Option[Long] = None,
        eventTime: Option[Long] = None,
        balance: Option[BigDecimal] = None,
        amount: Option[BigDecimal] = None,
    )

  object Block:
    final case class Block_self(
        number: Option[Long],
        hash: Option[String],
        parentHash: Option[String],
        txCount: Option[Long],
        eventTime: Option[Long],
        createdAt: Option[Long],
    )

  object Summary:
    final case class SummaryMain(
        id: Option[Long] = None,
        lmPrice: Option[Double] = None,
        blockNumber: Option[Long] = None,
        totalAccounts: Option[Long] = None,
        createdAt: Option[Long] = None,
        totalTxSize: Option[Long] = None,
        total_balance: Option[String] = None,
    )
    final case class SummaryMainOption(
        id: Option[Long] = None,
        lmPrice: Option[Double] = None,
        blockNumber: Option[Long] = None,
        totalTxSize: Option[Long] = None,
        totalAccounts: Option[Long] = None,
        createdAt: Option[Long] = None,
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

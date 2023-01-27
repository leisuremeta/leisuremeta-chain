package io.leisuremeta.chain.lmscan.frontend

object SampleJson:
  // http://localhost:8081/tx/list?useDataNav=true&pageNo=0&sizePerRequest=10
  val TxList = """{
    "totalCount": 21,
    "totalPages": 3,
    "payload": [
        {
            "hash": "7913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2dc",
            "txType": "account",
            "fromAddr": "26A463A0ED56A4A97D673A47C254728409C7B002",
            "toAddr": [
                "b775871c85faae7eb5f6bcebfd28b1e1b412235c"
            ],
            "amount": 1.2345678912345679E8,
            "blockHash": "6913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2da",
            "eventTime": 1673939878,
            "createdAt": 21312412
        },
        {
            "hash": "7913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2dc",
            "txType": "account",
            "fromAddr": "26A463A0ED56A4A97D673A47C254728409C7B002",
            "toAddr": [
                "b775871c85faae7eb5f6bcebfd28b1e1b412235c"
            ],
            "amount": 1.2345678912345679E8,
            "blockHash": "6913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2da",
            "eventTime": 1673939878,
            "createdAt": 21312412
        },
    ]
    }"""

  val Tx = """{
            "hash": "7913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2dc",
            "txType": "account",
            "fromAddr": "26A463A0ED56A4A97D673A47C254728409C7B002",
            "toAddr": [
                "b775871c85faae7eb5f6bcebfd28b1e1b412235c"
            ],
            "amount": 1.2345678912345679E8,
            "blockHash": "6913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2da",
            "eventTime": 1673939878,
            "createdAt": 21312412
        
    }"""

//     case class Tx(
//     hash: String,
//     txType: String,
//     fromAddr: List[String],
//     toAddr: List[String],
//     amount: Long,
//     blockHash: String,
//     eventTime: Int,
//     createdAt: Int,
// )
// case class Payload(tx_list: List[Tx])
// case class TxList(totalCount: Int, totalPages: Int, payload: Payload)

// object Parser:

//   implicit val txDecoder: Decoder[TxList] = deriveDecoder
//   implicit val txEncoder: Encoder[TxList] = deriveEncoder

package io.leisuremeta.chain.lmscan.frontend
import io.circe.*, io.circe.generic.semiauto.*

object Parser:
  case class AddrList(list: List[String])
  case class Tx(
      hash: String,
      txType: String,
      fromAddr: AddrList,
      toAddr: AddrList,
      amount: Long,
      blockHash: String,
      eventTime: Int,
      createdAt: Int,
  )
  case class Payload(tx_list: List[Tx])
  case class TxList(totalCount: Int, totalPages: Int, payload: Payload)

  val sample = """{
    "sample_url": "http://localhost:8081/tx/list?useDataNav=true&pageNo=0&sizePerRequest=10",
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

  implicit val txDecoder: Decoder[Tx] = deriveDecoder
  implicit val txEncoder: Encoder[Tx] = deriveEncoder

  object ParserSample:

    case class Payload(
        title: String,
        content: String,
        author: String,
        comments: List[String],
    )
    case class BoardApi(totalCount: Int, payload: List[Payload])

    val sample = """{
        "totalCount": 21,
        "payload": [
            {
                "title": "hello world",
                "content": "some contents...",
                "author": "j",
                "comments": [
                    "b775871c85faae7eb5f6bcebfd28b1e1b412235c"
                ],
            },
           {
                "title": "matrix reloaded",
                "content": "some contents...",
                "author": "xxx",
                "comments": [
                    "b775871c85faae7eb5f6bcebfd28b1e1b412235c",
                    "b775871c85faae7eb5f6bcebfd28b1e1b412235c2",
                ],
            },
        ]
        }"""

    implicit val BoardsApiDecoder: Decoder[Payload] = deriveDecoder
    implicit val BoardsApiEncoder: Encoder[Payload] = deriveEncoder

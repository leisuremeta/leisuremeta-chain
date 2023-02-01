package io.leisuremeta.chain.lmscan.frontend
import io.circe.*, io.circe.generic.semiauto.*
import io.circe.syntax.*
import io.circe.parser.*

// {
//   "hash": "1513b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2da",
//   "createdAt": 1673767078,
//   "signer": "26A463A0ED56A4A97D673A47C254728409C7B002",
//   "txType": "account",
//   "tokenType": "LM",
//   "inputHashs": [
//     "4913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2da"
//   ],
//   "transferHist": [
//     {
//       "toAddress": "b775871c85faae7eb5f6bcebfd28b1e1b412235c",
//       "value": "123456789.12345678912345678"
//     },
//     {
//       "toAddress": "b775871c85faae7eb5f6bcebfd28b1e1b412235c",
//       "value": "123456789.12345678912345678"
//     }
//   ],
//   "json": "test"
// }

case class TxDetail(
    hash: String,
    createdAt: Int,
    signer: String,
    txType: String,
    inputHashs: List[String],
    transferHist: List[Transfer],
    json: String,
)

case class Transfer(
    toAddress: String,
    value: Double,
)

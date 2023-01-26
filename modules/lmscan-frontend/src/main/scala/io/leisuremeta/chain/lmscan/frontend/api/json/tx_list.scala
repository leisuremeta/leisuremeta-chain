case class Payload()
// case class TxList(sample_url: String, totalCount: Number, totalPages: Number, payload:)

object JsonData:
  val simpleTxList = """
  {
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
        ]
    }
  """

  val data = """
        {
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
            "hash": "6913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2db",
            "txType": "account",
            "fromAddr": "26A463A0ED56A4A97D673A47C254728409C7B002",
            "toAddr": [
                "b775871c85faae7eb5f6bcebfd28b1e1b412235c"
            ],
            "amount": 1.2345678912345679E8,
            "blockHash": "6913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2da",
            "eventTime": 1673853478,
            "createdAt": 21312412
        },
        {
            "hash": "5913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2da",
            "txType": "account",
            "fromAddr": "26A463A0ED56A4A97D673A47C254728409C7B002",
            "toAddr": [
                "b775871c85faae7eb5f6bcebfd28b1e1b412235c"
            ],
            "amount": 1.2345678912345679E8,
            "blockHash": "6913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2da",
            "eventTime": 1673767078,
            "createdAt": 21312412
        },
        {
            "hash": "4913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2da",
            "txType": "account",
            "fromAddr": "26A463A0ED56A4A97D673A47C254728409C7B002",
            "toAddr": [
                "b775871c85faae7eb5f6bcebfd28b1e1b412235c"
            ],
            "amount": 1.2345678912345679E8,
            "blockHash": "6913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2da",
            "eventTime": 1673567078,
            "createdAt": 1673767078
        },
        {
            "hash": "3913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2da",
            "txType": "account",
            "fromAddr": "26A463A0ED56A4A97D673A47C254728409C7B002",
            "toAddr": [
                "b775871c85faae7eb5f6bcebfd28b1e1b412235c"
            ],
            "amount": 1.2345678912345679E8,
            "blockHash": "6913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2da",
            "eventTime": 1673467078,
            "createdAt": 1673767078
        },
        {
            "hash": "2913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2da",
            "txType": "account",
            "fromAddr": "26A463A0ED56A4A97D673A47C254728409C7B002",
            "toAddr": [
                "b775871c85faae7eb5f6bcebfd28b1e1b412235c"
            ],
            "amount": 1.2345678912345679E8,
            "blockHash": "6913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2da",
            "eventTime": 1673367078,
            "createdAt": 1673767078
        },
        {
            "hash": "1913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2da",
            "txType": "account",
            "fromAddr": "26A463A0ED56A4A97D673A47C254728409C7B002",
            "toAddr": [
                "b775871c85faae7eb5f6bcebfd28b1e1b412235c"
            ],
            "amount": 1.2345678912345679E8,
            "blockHash": "6913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2da",
            "eventTime": 1673267078,
            "createdAt": 1673767078
        },
        {
            "hash": "0913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2da",
            "txType": "account",
            "fromAddr": "26A463A0ED56A4A97D673A47C254728409C7B002",
            "toAddr": [
                "b775871c85faae7eb5f6bcebfd28b1e1b412235c"
            ],
            "amount": 1.2345678912345679E8,
            "blockHash": "6913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2da",
            "eventTime": 1673167078,
            "createdAt": 1673767078
        },
        {
            "hash": "0113b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2da",
            "txType": "account",
            "fromAddr": "26A463A0ED56A4A97D673A47C254728409C7B002",
            "toAddr": [
                "b775871c85faae7eb5f6bcebfd28b1e1b412235c"
            ],
            "amount": 1.2345678912345679E8,
            "blockHash": "6913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2da",
            "eventTime": 1673067078,
            "createdAt": 1673767078
        },
        {
            "hash": "1213b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2da",
            "txType": "account",
            "fromAddr": "26A463A0ED56A4A97D673A47C254728409C7B002",
            "toAddr": [
                "b775871c85faae7eb5f6bcebfd28b1e1b412235c"
            ],
            "amount": 1.2345678912345679E8,
            "blockHash": "6913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2da",
            "eventTime": 1673067078,
            "createdAt": 1673767078
        }
    ]
}
"""

# Lmscan API

`GET` **/tx/list** 트랜잭션(Tx) 목록 페이지 조회

> `param` pageNo: 페이지 번호
> `param` sizePerRequest: 페이지 당 출력할 레코드 갯수
> `param` _(optional)_ accountAddr: 사용자 지갑 주소
> `param` _(optional)_ blockHash: 블록 해쉬 값
> (단, accountAddr / blockHash 모두 입력시 에러)

- Response: PageResponse[TxInfo]

* totalCount: 트랜잭션 레코드의 총 갯수
* totalPages: 'sizePerRequest' 파라미터 값에 따른 총 페이지 번호
* payload: 요청 페이지의 트랜잭션 목록
  - hash: 트랜잭션 해쉬 값
  - blockNumber: 블록 번호
  - txType: 트랜잭션 형태에 따른 구분 (account / group / token / reward)
  - tokenType: 토큰 타입 구분 ( LM / NFT )
  - createdAt: 트랜잭션 생성 시간

- Example (pageNo `0`, sizePerRequest: `3` 으로 요청한 예시)
  - http://localhost:8081/tx/list?pageNo=0&sizePerRequest=3

```json
{
  "totalCount": 21,
  "totalPages": 7,
  "payload": [
    {
      "hash": "7913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2dc",
      "blockNumber": 14,
      "createdAt": 1673939878,
      "txType": "account",
      "tokenType": "LM",
      "signer": "26A463A0ED56A4A97D673A47C254728409C7B002",
      "value": "123456789.12345678912345678"
    },
    {
      "hash": "6913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2db",
      "blockNumber": 12,
      "createdAt": 1673853478,
      "txType": "account",
      "tokenType": "LM",
      "signer": "26A463A0ED56A4A97D673A47C254728409C7B002",
      "value": "123456789.12345678912345678"
    },
    {
      "hash": "5913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2da",
      "blockNumber": 15,
      "createdAt": 1673767078,
      "txType": "account",
      "tokenType": "LM",
      "signer": "26A463A0ED56A4A97D673A47C254728409C7B002",
      "value": "123456789.12345678912345678"
    }
  ]
}
```

`GET` **/tx/{transactionHash}/detail** 특정 트랜잭션 상세정보 조회

> `param` transactionHash: 트랜잭션 해쉬 값

- Response: Option[TxDetail]

  - hash: 트랜잭션 해쉬 값
  - txType: 트랜잭션 형태에 따른 구분 (account / group / token / reward)
  - signer: 해당 TX의 서명인(발신자)
  - amount: Tx에 의해 전송되는 LM
  - createdAt: Tx 가 Lmscan Db에 저장된 시간
  - eventTime: 트랜잭션 생성 시간
  - inputHashs: 인풋 트랜잭션 해쉬 목록
  - transferHist:
  - json: 트랜잭션의 raw json

  - Example (transactionHash `1513b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2da` 으로 요청한 예시)
    - http://localhost:8081/tx/1513b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2da/detail

```json
{
  "hash": "1513b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2da",
  "createdAt": 1673767078,
  "signer": "26A463A0ED56A4A97D673A47C254728409C7B002",
  "txType": "account",
  "tokenType": "LM",
  "inputHashs": [
    "4913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2da"
  ],
  "transferHist": [
    {
      "toAddress": "b775871c85faae7eb5f6bcebfd28b1e1b412235c",
      "value": "123456789.12345678912345678"
    },
    {
      "toAddress": "b775871c85faae7eb5f6bcebfd28b1e1b412235c",
      "value": "123456789.12345678912345678"
    }
  ],
  "json": "test"
}
```

`GET` **/block/list** 블록 목록 페이지 조회

> `param` pageNo: 페이지 번호
> `param` sizePerRequest: 페이지 당 출력할 레코드 갯수

- Response: PageResponse[TxInfo]

* totalCount: 트랜잭션 레코드의 총 갯수
* totalPages: 'sizePerRequest' 파라미터 값에 따른 총 페이지 번호
* payload: 요청 페이지의 트랜잭션 목록
  - number: 블록 번호
  - hash: 블록 해쉬 값
  - txCount: 트랜잭션 갯수
  - createdAt: 블록 생성 시간

- Example (pageNo `0`, sizePerRequest: `3` 으로 요청한 예시)
  - http://localhost:8081/block/list?pageNo=0&sizePerRequest=3

```json
{
  "totalCount": 2,
  "totalPages": 1,
  "payload": [
    {
      "number": 123456789,
      "hash": "6913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2da",
      "txCount": 1234,
      "createdAt": 1675068555
    },
    {
      "number": 123456790,
      "hash": "2913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2dc",
      "txCount": 1234,
      "createdAt": 1675068555
    }
  ]
}
```

`GET` **/block/{blockHash}/detail** 특정 블록 상세정보 조회

> `param` blockHash: 블록 해쉬 값

- Response: Option[BlockDetail]

  - hash: 블록 해쉬 값
  - parentHash: 이전 블록 해쉬 값
  - number: 블록 번호
  - txCount: 트랜잭션 갯수
  - createdAt: 블록 생성 시간
  - txs: 해당 블록의 트랜잭션 목록 ()

  - Example (blockHash `6913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2da` 으로 요청한 예시)
    - http://localhost:8081/block/6913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2da/detail

```json
{
  "hash": "6913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2da",
  "parentHash": "7913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2db",
  "number": 123456789,
  "timestamp": 1675068000,
  "txCount": 1234,
  "txs": [
    {
      "hash": "7913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2dc",
      "blockNumber": 14,
      "createdAt": 1673939878,
      "txType": "account",
      "tokenType": "LM",
      "signer": "26A463A0ED56A4A97D673A47C254728409C7B002",
      "value": "123456789.12345678912345678"
    },
    {
      "hash": "6913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2db",
      "blockNumber": 12,
      "createdAt": 1673853478,
      "txType": "account",
      "tokenType": "LM",
      "signer": "26A463A0ED56A4A97D673A47C254728409C7B002",
      "value": "123456789.12345678912345678"
    },
    {
      "hash": "5913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2da",
      "blockNumber": 15,
      "createdAt": 1673767078,
      "txType": "account",
      "tokenType": "LM",
      "signer": "26A463A0ED56A4A97D673A47C254728409C7B002",
      "value": "123456789.12345678912345678"
    }
  ]
}
```

`GET` **/account/{accountAddr}/detail** 특정 어카운트 상세정보 조회

> `param` accountAddr: 어카운트 해쉬 값

- Response: Option[AccountDetail]

  - address: 어카운트 주소
  - balance: 보유 LM 토큰 수량
  - value: 해당 토큰 수량의 달러화 환산 가치
  - txHistory: 해당 어카운트의 트랜잭션 히스토리

  - Example (pageNo `0`, sizePerRequest: `3` 으로 요청한 예시)
    - http://localhost:8081/account/26A463A0ED56A4A97D673A47C254728409C7B002/detail

```json
{
  "address": "26A463A0ED56A4A97D673A47C254728409C7B002",
  "balance": 100.2222,
  "value": 12.32,
  "txHistory": [
    {
      "hash": "7913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2dc",
      "blockNumber": 14,
      "createdAt": 1673939878,
      "txType": "account",
      "tokenType": "LM",
      "signer": "26A463A0ED56A4A97D673A47C254728409C7B002",
      "value": "123456789.12345678912345678"
    },
    {
      "hash": "6913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2db",
      "blockNumber": 12,
      "createdAt": 1673853478,
      "txType": "account",
      "tokenType": "LM",
      "signer": "26A463A0ED56A4A97D673A47C254728409C7B002",
      "value": "123456789.12345678912345678"
    },
    {
      "hash": "5913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2da",
      "blockNumber": 15,
      "createdAt": 1673767078,
      "txType": "account",
      "tokenType": "LM",
      "signer": "26A463A0ED56A4A97D673A47C254728409C7B002",
      "value": "123456789.12345678912345678"
    }
  ]
}
```

`GET` **/nft/{tokenId}/detail** 특정 NFT 상세정보 조회

> `param` tokenId: nft 토큰 아이디

- Response: Option[NftDetail]

  - nftFile: Nft 파일 정보
  - activities: 보유 LM 토큰 수량

  - Example (pageNo `0`, sizePerRequest: `3` 으로 요청한 예시)
    - http://localhost:8081/nft/2022122110000930000002558/detail

```json
{
  "nftFile": {
    "tokenId": "2022122110000930000002558",
    "tokenDefId": "test-token",
    "collectionName": "BPS-JinKei",
    "nftName": "#2558",
    "nftUri": "https://d2t5puzz68k49j.cloudfront.net/release/collections/BPS_JinKei/NFT_ITEM/CE298DB9-66E4-4258-9A73-A00E09899698.mp4",
    "creatorDescription": "It is an Act to Earn NFT based on the artwork of Jin Kei [Block Artist] and Younghoon Shin [Sumukhwa (Ink Wash Painting) Artist].",
    "dataUrl": "https://d2t5puzz68k49j.cloudfront.net/release/collections/BPS_JinKei/NFT_ITEM_META/CE298DB9-66E4-4258-9A73-A00E09899698.json",
    "rarity": "UNIQ",
    "creator": "JinKei",
    "eventTime": 1675069161,
    "createdAt": 1675069161,
    "owner": "b775871c85faae7eb5f6bcebfd28b1e1b412235c"
  },
  "activities": [
    {
      "txHash": "6913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2da",
      "action": "MintNFT",
      "fromAddr": "b775871c85faae7eb5f6bcebfd28b1e1b412235c",
      "toAddr": "b775871c85faae7eb5f6bcebfd28b1e1b412235c",
      "createdAt": 1675068858
    }
  ]
}
```

`GET` **/summary/main** 조회시점 기준 가장 최근 24시간 이내 통계 데이터 조회

- Response: Option[Summary]

  - Example 
    - http://localhost:8081/summary/main

```json
{
  "id": 1,
  "lmPrice": 0.394,
  "blockNumber": 123456789,
  "txCountInLatest24h": 123456789,
  "totalAccounts": 123456789,
  "createdAt": 1675612055
}
```

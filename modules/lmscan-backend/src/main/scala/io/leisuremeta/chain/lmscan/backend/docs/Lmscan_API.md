# Lmscan API

`GET` **/tx/list** 트랜잭션(Tx) 페이지 조회

> `query param`
>
> - 'pageNo': 페이지 번호
> - 'sizePerRequest': 페이지 당 출력할 레코드 갯수

- Response: PageResponse[Tx]

* totalCount: 트랜잭션 레코드의 총 갯수
* totalPages: 'sizePerRequest' 파라미터 값에 따른 총 페이지 번호
* payload: 요청 페이지의 트랜잭션 목록
  - hash: 트랜잭션 해쉬 값
  - txType: 트랜잭션 형태에 따른 구분 (account / group / token / reward)
  - fromAddr: Tx 발신자 어카운트 주소
  - toAddr: Tx 수신자 어카운트 주소 목록
  - amount: Tx에 의해 전송되는 LM
  - eventTime: 트랜잭션 생성 시간
  - createdAt: Tx 가 Lmscan Db에 저장된 시간

- Example (pageNo `0`, sizePerRequest: `3` 으로 요청한 예시)
  - http://localhost:8081/tx/list?pageNo=0&sizePerRequest=3

```json
{
  "totalCount": 21,
  "totalPages": 7,
  "payload": [
    {
      "hash": "7913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2dc",
      "txType": "account",
      "fromAddr": "26A463A0ED56A4A97D673A47C254728409C7B002",
      "toAddr": ["b775871c85faae7eb5f6bcebfd28b1e1b412235c"],
      "amount": 1.2345678912345679e8,
      "blockHash": "6913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2da",
      "eventTime": 1673939878,
      "createdAt": 21312412
    },
    {
      "hash": "6913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2db",
      "txType": "account",
      "fromAddr": "26A463A0ED56A4A97D673A47C254728409C7B002",
      "toAddr": ["b775871c85faae7eb5f6bcebfd28b1e1b412235c"],
      "amount": 1.2345678912345679e8,
      "blockHash": "6913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2da",
      "eventTime": 1673853478,
      "createdAt": 21312412
    },
    {
      "hash": "5913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2da",
      "txType": "account",
      "fromAddr": "26A463A0ED56A4A97D673A47C254728409C7B002",
      "toAddr": ["b775871c85faae7eb5f6bcebfd28b1e1b412235c"],
      "amount": 1.2345678912345679e8,
      "blockHash": "6913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2da",
      "eventTime": 1673767078,
      "createdAt": 21312412
    }
  ]
}
```

`GET` **/tx/{transactionHash}/detail** 특정 트랜잭션 상세정보 조회

> `path param` transactionHash: 트랜잭션 해쉬 값

- Response: Tx

  - hash: 트랜잭션 해쉬 값
  - txType: 트랜잭션 형태에 따른 구분 (account / group / token / reward)
  - fromAddr: Tx 발신자 어카운트 주소
  - toAddr: Tx 수신자 어카운트 주소 목록
  - amount: Tx에 의해 전송되는 LM
  - eventTime: 트랜잭션 생성 시간
  - createdAt: Tx 가 Lmscan Db에 저장된 시간

  ```json
  {
    "hash": "7913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2dc",
    "txType": "account",
    "fromAddr": "26A463A0ED56A4A97D673A47C254728409C7B002",
    "toAddr": ["b775871c85faae7eb5f6bcebfd28b1e1b412235c"],
    "amount": 1.2345678912345679e8,
    "blockHash": "6913b313f68610159bca2cfcc0758a726494c442d8116200e1ec2f459642f2da",
    "eventTime": 1673939878,
    "createdAt": 21312412
  }
  ```

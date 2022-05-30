# LeisureMeta Chain API with Example

`POST` **/txhash** 트랜잭션 해시값 계산

아직 해시값 계산 모듈을 제공하지 않으므로, 여기에 트랜잭션을 보내면 해시값을 계산해준다. 나온 해시값에 서명해서 정식으로 트랜잭션을 집어넣으면 된다.

```json
[
	{
  	"AccountTx" : {
    	"CreateAccount" : {
      	"networkId" : 1000,
      	"createdAt" : "2020-05-22T09:00:00Z",
      	"account" : "alice",
      	"guardian" : null
    	}
  	}
	}
]
```

```json
["396fb3ef2ecdb800126027a802e26eb2e7e1d47fee28f24287fb836cdafc6f1e"]
```



`POST`**/tx** 트랜잭션 제출

(Private Key `b229e76b742616db3ac2c5c2418f44063fcc5fcc52a08e05d4285bdb31acba06`으로 서명한 예시)

```json
[
  {
    "sig" : {
      "sig" : {
        "v" : 27,
        "r" : "c0cf8bb197d5f0a562fd76200f09480f676f31970e982f65bc1efd707504ef73",
        "s" : "7ad50c3987ce4a9007d093d25caaf701436824dafc6290d8e477b8f1c8b6771d"
      },
      "account" : "alice"
    },
    "value" : {
      "AccountTx" : {
        "CreateAccount" : {
          "networkId" : 1000,
          "createdAt" : "2020-05-22T09:00:00Z",
          "account" : "alice",
          "guardian" : null
        }
      }
    }
  }
]
```

```json
["396fb3ef2ecdb800126027a802e26eb2e7e1d47fee28f24287fb836cdafc6f1e"]
```



`GET` **/status** 노드 현재상태조회

트랜잭션 하나가 들어가서 블록이 하나 생성되었으므로 block number 값이 1이 된다.

(추후 트랜잭션이 없는 빈 블록 하나를 더 찍어서 거래완결을 표시할 예정)

```json
{
  "networkId": 1000,
  "genesisHash": "50f1634b0534d9eaff9bb4084b38839f710b5822a599a10c3b106a19a4315127",
  "bestHash": "a9735ba3420e7d9be5f26b28b035f3141d4586e1015c237a67aa46c90a65b8ca",
  "number": 1
}
```



`GET` **/block**/a9735ba3420e7d9be5f26b28b035f3141d4586e1015c237a67aa46c90a65b8ca 블록 정보 조회

best hash값을 넣어서 최신 블록의 정보를 조회한다

```json
{
  "header": {
    "number": 1,
    "parentHash": "50f1634b0534d9eaff9bb4084b38839f710b5822a599a10c3b106a19a4315127",
    "stateRoot": {
      "account": {
        "namesRoot": "7a3a362149605d574b2eccdf85a0bfe7ca579fd2cfa0a2c19a2b601731d5ddbd",
        "keyRoot": null
      }
    },
    "transactionsRoot": "962dfb46a6439d48efd72e1a21356911f1f5882843c76a3c2b2a2709d44b25eb",
    "timestamp": "2022-05-29T18:13:54.425Z"
  },
  "transactionHashes": [
    "396fb3ef2ecdb800126027a802e26eb2e7e1d47fee28f24287fb836cdafc6f1e"
  ],
  "votes": [
    {
      "v": 28,
      "r": "f6d37c7994cb9f5f84b2a100c2346d6a0aec7e48e14872096cd32a90dc3c43ec",
      "s": "52ad670b041581c3d1d246fb09df36b8a1201db76f4cb2113e0759e16541be20"
    }
  ]
}
```





`GET` **/tx**/396fb3ef2ecdb800126027a802e26eb2e7e1d47fee28f24287fb836cdafc6f1e 트랜잭션 정보 조회

블록에 기록된 트랜잭션 해시값 하나에 대한 정보를 취득한다

```json
{
  "signedTx": {
    "sig": {
      "sig": {
        "v": 27,
        "r": "c0cf8bb197d5f0a562fd76200f09480f676f31970e982f65bc1efd707504ef73",
        "s": "7ad50c3987ce4a9007d093d25caaf701436824dafc6290d8e477b8f1c8b6771d"
      },
      "account": "alice"
    },
    "value": {
      "AccountTx": {
        "CreateAccount": {
          "networkId": 1000,
          "createdAt": "2020-05-22T09:00:00Z",
          "account": "alice",
          "guardian": null
        }
      }
    }
  },
  "result": null
}
```


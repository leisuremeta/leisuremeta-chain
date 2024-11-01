# LeisureMeta Chain API



## API with Top Priority

`GET` **/balance/{accountName}** 계정 잔고 조회

> `param` movable: 잔고의 이동 가능성 여부
>
> * 'free': 유동 자산
> * 'locked': 예치 자산

*  Response: Map[TokenDefinitionID, BalanceInfo]
  * Token Definition ID: 토큰 정의 ID (string)
  * BalanceInfo
    * Total Amount: 해당 토큰 총 금액/수량 (NFT의 경우 랜덤박스 갯수)
    * Map[TxHash, Tx]: 사용하지 않은 트랜잭션 해시 목록

`GET` **/nft-balance/{accountName}** 계정 NFT 잔고 조회

> `param` *(optional)* movable: 잔고의 이동 가능성 여부
>
> * 'free': 유동 자산
> * 'locked': 예치 자산
> * 'all': 전체 자산

*  Response: Map[TokenID, NftBalanceInfo]
  *  NftBalanceInfo
    *  TokenDefinitionId
    *  TxHash
    *  Tx

`GET` **/activity/account/{account}** 계정 활동내역 조회

* Response: Seq[ActivityInfo]
  * ActivityInfo
    * timestamp
    * point
    * description
    * txHash


`GET` **/activity/token/{tokenId}** 토큰이 받은 활동내역 조회

* Response: Seq[ActivityInfo]
  * ActivityInfo
    * timestamp
    * point
    * description
    * txHash


`GET` **/reward/{accountName}** 보상 조회

> `param` *(optional)* timestamp: 기준 시점. 없으면 가장 최근 보상. (월요일 0시 ~ 일요일 23시59분 주기)
>
> `param` *(optional)* dao-account: 마스터 다오 계정. 없으면  `DAO-M` 사용
>
> `param` *(optional)* reward-amount: 리워드 총량. 없으면 마스터 다오 계정의 현재 LM 밸런스. 

* Response:
  * account: 계정이름 
  * reward: 보상
    * total: 총 보상량
    * activity: 활동보상
    * token: 토큰이 받은 사용자액션 보상
    * rarity: 보유 토큰의 희귀도에 따른 보상
    * bonus: 모더레이터인 경우 주어지는 추가 보상 총합 
  * point: 활동내역에 따르는 보상 포인트(1/1000 포인트 단위의 정수)
    * activity: 활동 내역.
      * like: 좋아요
      * comment: 댓글
      * share: 공유
      * report: 신고
    * token: 토큰이 받은 내역
      * like: 좋아요
      * comment: 댓글
      * share: 공유
      * report: 신고
    * rarity: Map[String, Number] 희귀도에 따르는 포인트
  * timestamp: 기준 시점
  * totalNumberOfDao: 시스템에 개설된 다오 총 수


`GET`  **/dao/{groupID}** 특정 그룹의 DAO 정보 조회

* Response: DaoInfo DAO 정보
  * DaoInfo 현재까지 정해진 필드값들
    * NumberOfModerator: 모더레이터 숫자

`GET`  **/owners/{definitionID}** 특정 컬렉션 NFT들의 보유자 정보 조회

* Response:
  * Map[TokenID, AccountName]

`GET`  **/snapshot/ownership/**  받을 토큰 소유보상 점수 조회

> `param` *(optional)* from: 조회를 시작할 token id. 주어지지 않으면 ""
>
> `param` *(optional)* limit: 조회할 총 갯수. 디폴트값은 100

* Response: [TokenId, OwnershipSnapshot]
  * OwnershipSnapshot
    * account
    * timestamp 기준 시점
    * point 포인트. 일반적으론 해당 NFT의 Rarity 점수
    * definitionId 보상받을 토큰 종류. 일반적으론 LM
    * amount 보상량

`GET`  **/rewarded/ownership/{tokenID}**  최근에 받은 토큰 소유보상 조회

* Response: OwnershipRewardLog
  * OwnershipRewardLog
    * OwnershipShapshot
    * ExecuteReward TxHash

`POST` **/tx** 트랜잭션 제출

* 아래의 트랜잭션 목록 참조
* Array로 한 번에 여러개의 트랜잭션 제출 가능

### Blockchain Explorer 지원용 API

`GET` **/status** 블록체인 현재상태 조회 (최신 블록 hash, 블록 number 포함)

`GET` **/block **블록 목록 조회

> `param` *(optional)* from: 찾기 시작할 블록 해시. 없으면 최신 블록
>
> `param` *(optional)* limit: 가져올 블록 갯수. 디폴트 50.

`GET` **/block/{blockHash}** 블록 상세정보 조회 (포함된 트랜잭션 해시 목록 포함)

`GET` **/tx** 특정 블록에 포함된 트랜잭션 목록 조회

> `param`  block: 찾을 블록 해시

`GET` **/tx/{transactionHash}** 트랜잭션 상세정보 조회

### Response HTTP Status Codes

* 요청했을 때 해당 내용이 없는 경우: 404 Not Found
* 서명이 올바르지 않은 경우: 401 Unauthorized
* 트랜잭션이 invalid한 경우: 400 Bad Request
* 블록체인 노드 내부 오류: 500 Internal Server Error

## Transactions

* 모든 트랜잭션 공통 필드
  * "networkId": 다른 네트워크에 똑같은 트랜잭션을 보내는 것을 막기 위한 필드. 
  * "createdAt": 트랜잭션 생성시각
* Format
  * 서명주체
  * Fields: 트랜잭션을 제출할 때 포함시켜야 하는 필드 목록
  * *(optional)* Computed Fields: 블록에 기록될 때 노드에 의해 덧붙여지는 필드들


### Account

* CreateAccount 계정 생성
  * > 사용자 서명
  * Fields
    * account: Account 계정 이름
    * ethAddress: *(optional)* 이더리움 주소
    * guardian: *(optional)* Account
      * 계정에 공개키를 추가할 수 있는 권한을 가진 계정 지정. 일반적으로는 `playnomm`
  
  * Example (private key `b229e76b742616db3ac2c5c2418f44063fcc5fcc52a08e05d4285bdb31acba06` 으로 서명한 예시)
  
  ```json
  [
    {
      "sig" : {
        "sig" : {
          "v" : 28,
          "r" : "495c3bcc143eea328c11b7ec55069dd4fb16c26463999f9dbc085094c3b59423",
          "s" : "707a75e433abd208cfb76d4e0cdbc04b1ce2389e3a1f866348ef2e3ea5785e93"
        },
        "account" : "alice"
      },
      "value" : {
        "AccountTx" : {
          "CreateAccount" : {
            "networkId" : 1000,
            "createdAt" : "2020-05-22T09:00:00Z",
            "account" : "alice",
            "ethAddress" : null,
            "guardian" : null
          }
        }
      }
    }
  ]
  ```
  
  ```json
  ["822380e575e482e829fc9f45ffd0f99f4f0987ccbec0c0a5de5fd640f42a9100"]
  ```

* CreateAccountWithExternalChainAddresses 외부 블록체인 주소를 가진 계정 생성
  * > 사용자 서명
  * Fields
    * account: Account 계정 이름
    * externalChainAddresses: 외부 블록체인 주소. 현재는 `eth`, `sol` 두 가지 지원.
    * guardian: *(optional)* Account
      * 계정에 공개키를 추가할 수 있는 권한을 가진 계정 지정. 일반적으로는 `playnomm`
    * memo: *(optional)* 메모
    
  * Example (private key `b229e76b742616db3ac2c5c2418f44063fcc5fcc52a08e05d4285bdb31acba06` 으로 서명한 예시)
  
  ```json
  [
    {
      "sig" : {
        "sig" : {
          "v" : 28,
          "r" : "2f7a53986a387961047566ab8d31fcdbbe6cc96529cdbfccb68fb268700f2bdf",
          "s" : "56f993f2cca6a5f7410e04a5aa849c7698f1e0966d98b90638d7472cd9eb3210"
        },
        "account" : "alice"
      },
      "value" : {
        "AccountTx" : {
          "CreateAccountWithExternalChainAddresses" : {
            "networkId" : 2021,
            "createdAt" : "2023-01-11T19:01:30Z",
            "account" : "bob",
            "externalChainAddresses" : {
              "eth" : "99f681d29754aeee1426ef991b745a4f662e620c"
            },
            "guardian" : "alice",
            "memo" : null
          }
        }
      }
    }
  ```
  
  ```json
  ["d795dc9205ec5ecb3097fe0ca0326e6597c6ecae497a0876b8cc3737d823264a"]
  ```
  
* UpdateAccount 계정 생성
  * > 사용자 서명 혹은 Guardian 서명
  * Fields
    * account: Account 계정 이름
    * ethAddress: *(optional)* 이더리움 주소
    * guardian: *(optional)* Account
      * 계정에 공개키를 추가할 수 있는 권한을 가진 계정 지정. 일반적으로는 `playnomm`

  * Example
  
  ```json
  [
    {
      "sig" : {
        "sig" : {
          "v" : 28,
          "r" : "22c14ac6fbdce52c256640f1e36851ef901ea1b5cfebc3a430283a89df99bc11",
          "s" : "3474ebcc861c2d31a60d363356c4c89c196d450432b33bedadfb94d66edf2ffd"
        },
        "account" : "alice"
      },
      "value" : {
        "AccountTx" : {
          "UpdateAccount" : {
            "networkId" : 1000,
            "createdAt" : "2020-05-22T09:00:00Z",
            "account" : "alice",
            "ethAddress" : "0xefD277f6da7ac53e709392044AE98220Df142753",
            "guardian" : null
          }
        }
      }
    }
  ]
  ```
  
  ```json
  ["7730dadeff5be3bfd63fdec8853d6301a5ec0e3b8c815a4d7e0ba20e8c52517d"]
  ```


* UpdateAccountWithExternalChainAddresses 계정 생성
  * > 사용자 서명 혹은 Guardian 서명
  * Fields
    * account: Account 계정 이름
    * externalChainAddresses: 외부 블록체인 주소. 현재는 `eth`, `sol` 두 가지 지원.
    * guardian: *(optional)* Account
      * 계정에 공개키를 추가할 수 있는 권한을 가진 계정 지정. 일반적으로는 `playnomm`
    * memo: *(optional)* 메모

  * Example
  
  ```json
  [
    {
      "sig" : {
        "sig" : {
          "v" : 27,
          "r" : "bf7dfb91669233e120a07707084f2b9879d9620cd297096f862d52d53fe9988d",
          "s" : "10d652ba45a81ad572aeae6c25ffd2e6339e8f049d379ae24d5a947f531d5d65"
        },
        "account" : "alice"
      },
      "value" : {
        "AccountTx" : {
          "UpdateAccountWithExternalChainAddresses" : {
            "networkId" : 2021,
            "createdAt" : "2023-01-11T19:01:40Z",
            "account" : "bob",
            "externalChainAddresses" : {
              "eth" : "99f681d29754aeee1426ef991b745a4f662e620c"
            },
            "guardian" : "alice",
            "memo" : "bob updated"
          }
        }
      }
    }
  ]
  ```
  
  ```json
  ["a0e28414a97b3fabb49cf3b77757219b8e1a7205ba3cc4f5e618019b36bc38c3"]
  ```
  
  
* AddPublicKeySummaries 계정에 사용할 공개키요약 추가
  * > 사용자 서명 혹은 Guardian 서명
  
  * Fields
    * account: Account 계정 이름
    * summaries: Map[PublicKeySummary, String]
      * 추가할 공개키요약과 간단한 설명
      * 만약 설명이 `"permanant"` 인 경우 해당 public key summary 는 유효기간 없이 무제한 사용
    
  * Result
    * Removed: Map[PublicKeySummary, Descrption(string)]
  
  * Example
  
  ```json
  [
    {
      "sig" : {
        "sig" : {
          "v" : 27,
          "r" : "816df20e4ff581fd2056689b48be73cca29e4f81977e5c42754e598757434c51",
          "s" : "4e43aef8d836e79380067365cd7a4a452df5f52b73ec78463bdc7cdea2e11ca0"
        },
        "account" : "alice"
      },
      "value" : {
        "AccountTx" : {
          "AddPublicKeySummaries" : {
            "networkId" : 1000,
            "createdAt" : "2020-05-22T09:00:00Z",
            "account" : "alice",
            "summaries" : {
              "5b6ed47b96cd913eb938b81ee3ea9e7dc9affbff" : "another key"
            }
          }
        }
      }
    }
  ]
  ```
  
  ```json
  ["e996dcbabcf8a86208bcc8d683778f5d6b5d1b8ff950c9e60cc72b66fc619cca"]
  ```
  
  
  
* RemovePublicKeySummaries 계정에 사용할 공개키요약 삭제
  * > 사용자 서명 혹은 Guardian 서명
  * Fields
    * Account: AccountName (string)
    * Summaries: Set[PublicKeySummary]
  
* RemoveAccount 계정 삭제
  * > 사용자 서명 혹은 Guardian 서명
  * Fields
    * Account: AccountName (string)


### Group

* CreateGroup 그룹 생성
  * > Coordinator 서명
  * Fields
    * GroupID(string)
    * Name: GroupName(string)
    * Coordinator: AccountName(string)
      * 그룹 조정자. 그룹에 계정 추가, 삭제 및 그룹 해산 권한을 가짐
  
  * Example
  
  ```json
  [
    {
      "sig" : {
        "sig" : {
          "v" : 28,
          "r" : "aab6f7ccc108b8e75601c726d43270c1a60f38f830136dfe293a2633dc86a0dd",
          "s" : "3cc1b610df7a421f9ae560853d5f07005a20c6ad225a00861a76e5e91aa183c0"
        },
        "account" : "alice"
      },
      "value" : {
        "GroupTx" : {
          "CreateGroup" : {
            "networkId" : 1000,
            "createdAt" : "2022-06-08T09:00:00Z",
            "groupId" : "mint-group",
            "name" : "mint group",
            "coordinator" : "alice"
          }
        }
      }
    }
  ]
  ```
  
  ```json
  ["adb9440aeef2de4697774657ebbcce9c1e5b01423e0a21da90da355458400c75"]
  ```
  
  
  
* DisbandGroup 그룹 해산
  * > Coordinator 서명
  * Fields
    * GroupID(string)
  
* AddAccounts 그룹에 계정 추가
  * > Coordinator 서명
  * Fields
    * GroupID(string)
    * Accounts: Set[AccountName(string)]

  * Example
  
  ```json
  [
    {
      "sig" : {
        "sig" : {
          "v" : 28,
          "r" : "2dd00a2ebf07ff2d09d6e9bcd889ddc775c17989827e3e19b5e8d1744c021466",
          "s" : "05bd60fef3d45463e22e5c157c814a7cbd1681410b67b0233c97ce7116d60729"
        },
        "account" : "alice"
      },
      "value" : {
        "GroupTx" : {
          "AddAccounts" : {
            "networkId" : 1000,
            "createdAt" : "2022-06-08T09:00:00Z",
            "groupId" : "mint-group",
            "accounts" : [
              "alice",
              "bob"
            ]
          }
        }
      }
    }
  ]
  
  ```
  
  ```json
  ["015a8cced717ca40a528d9518e8494961a4c4e7fde1422304b751814ed181e00"]
  ```
  
  
  
* RemoveAccounts 그룹에 계정 삭제
  * > Coordinator 서명
  * Fields
    * GroupID(string)
    * Accounts: Set[AccountName(string)]

* ReplaceCoordinator 그룹 조정자 변경
  * > Coordinator 서명
  * Fields
    * GroupID(string)
    * NewCoordinator: AccountName(string)


### Token

* DefineToken 토큰 정의. Fungible Token, NFT 공히 사용한다. (랜덤박스 포함)
  * > MinterGroup에 속한 Account의 서명
  * Fields
    * definitionId: TokenDefinitionID(string)
    * name: String
    * *(optional)* Symbol(string)
    * *(optional)* MinterGroup: GroupID(string) 신규토큰발행 권한을 가진 그룹
    * *(optional)* NftInfo
      * minter: AccountName(string)
      * rarity: Map[(Rarity(string), Weight]
      * *(optional)* DataUrl(string)
      * *(optional)* ContentHash: uint256

  * Example

  ```json
  [
    {
      "sig" : {
        "sig" : {
          "v" : 28,
          "r" : "ce2b48b7da96eef22a2b92170fb81865adb99cbcae99a2b81bb7ce9b4ba990b6",
          "s" : "35a708c9ffc1b7ef4e88389255f883c96e551a404afc4627e3f6ca32a617bae6"
        },
        "account" : "alice"
      },
      "value" : {
        "TokenTx" : {
          "DefineToken" : {
            "networkId" : 1000,
            "createdAt" : "2020-05-22T09:01:00Z",
            "definitionId" : "test-token",
            "name" : "test-token",
            "symbol" : "TT",
            "minterGroup" : "mint-group",
            "nftInfo" : {
              "Some" : {
                "value" : {
                  "minter" : "alice",
                  "rarity" : {
                    "LGDY" : 8,
                    "UNIQ" : 4,
                    "EPIC" : 2,
                    "RARE" : 1
                  },
                  "dataUrl" : "https://www.playnomm.com/data/test-token.json",
                  "contentHash" : "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
                }
              }
            }
          }
        }
      }
    }
  ]
  ```

  ```json
  ["b0cfd8da5ef347762b60162c772148902b54abca4760fb53e3eb752f8b953664"]
  ```


* DefineTokenWithPrecision Precision이 있는 토큰 정의. Fungible Token, NFT 공히 사용한다. (랜덤박스 포함)
  * > MinterGroup에 속한 Account의 서명
  * Fields
    * definitionId: TokenDefinitionID(string)
    * name: String
    * *(optional)* Symbol(string)
    * *(optional)* MinterGroup: GroupID(string) 신규토큰발행 권한을 가진 그룹
    * *(optional)* NftInfo
      * minter: AccountName(string)
      * rarity: Map[(Rarity(string), Weight]
      * precision: int 소숫점 자릿수
      * *(optional)* DataUrl(string)
      * *(optional)* ContentHash: uint256

  * Example

  ```json
  [
    {
      "sig" : {
        "sig" : {
          "v" : 27,
          "r" : "74a1fa40be985b0c9bcf92df0262317a336f585fa24e261780b2ab6ff89d3f6a",
          "s" : "4cea4a8ad18df36a2c140366f8afee36441757921aa351fedf0b53d82307e9c2"
        },
        "account" : "alice"
      },
      "value" : {
        "TokenTx" : {
          "DefineTokenWithPrecision" : {
            "networkId" : 2021,
            "createdAt" : "2023-01-11T19:01:00Z",
            "definitionId" : "nft-with-precision",
            "name" : "NFT with precision",
            "symbol" : "NFTWP",
            "minterGroup" : "mint-group",
            "nftInfo" : {
              "Some" : {
                "value" : {
                  "minter" : "alice",
                  "rarity" : {
                    "LGDY" : 100,
                    "UNIQ" : 66,
                    "EPIC" : 33,
                    "RARE" : 10
                  },
                  "precision" : 2,
                  "dataUrl" : "https://www.playnomm.com/data/nft-with-precision.json",
                  "contentHash" : "2475a387f22c248c5a3f09cea0ef624484431c1eaf8ffbbf98a4a27f43fabc84"
                }
              }
            }
          }
        }
      }
    }
  ]
  ```

  ```json
  ["6d49236405972c01322db054338da2c7ab6fd9662d2a64c9bc1ab4026da9fb8f"]
  ```

* MintFungibleToken
  * > MinterGroup에 속한 Account의 서명
  * Fields
    * TokenDefinitionID(string)
    * Outputs: Map[AccountName, Amount]

  * Example

  ```json
  [
    {
      "sig" : {
        "sig" : {
          "v" : 28,
          "r" : "76fb1b3be81101638c9ce070628db035ad7d86d3363d664da0c5afe254494e90",
          "s" : "7ffb1c751fe4f5341c75341e4a51373139a7f730a56a08078ac89b6e1a77fc76"
        },
        "account" : "alice"
      },
      "value" : {
        "TokenTx" : {
          "MintFungibleToken" : {
            "networkId" : 1000,
            "createdAt" : "2020-05-22T09:01:00Z",
            "definitionId" : "test-token",
            "outputs" : {
              "alice" : 100
            }
          }
        }
      }
    }
  ]
  ```

  ```json
  ["a3f35adb3d5d08692a7350e61aaa28da992a4280ad8e558953898ef96a0051ca"]
  ```

* BurnFungibleToken

  * MinterGroup에 속한 Account의 서명

  * Fields
    * definitionId: TokenDefinitionId

    * amount

    * Inputs: Set[Signed.TxHash]

  * Result
    * outputAmount

* MintNFT

  * > MinterGroup에 속한 Account의 서명
  * Fields
    * TokenDefinitionID(string)
    * TokenID(string)
    * Rarity(string)
    * DataUrl(string)
    * ContentHash: uint256
    * Output: AccountName

  * Example

  ```json
  [
    {
      "sig" : {
        "sig" : {
          "v" : 27,
          "r" : "0a914259cc0e8513512ea6356fc3056efe104e84756cf23a6c1c1aff7a580613",
          "s" : "71a15b331b9e7337a018b442ee978a15f0d86e71ca53d2f54a9a8ccb92646cf9"
        },
        "account" : "alice"
      },
      "value" : {
        "TokenTx" : {
          "MintNFT" : {
            "networkId" : 1000,
            "createdAt" : "2022-06-08T09:00:00Z",
            "tokenDefinitionId" : "test-token",
            "tokenId" : "2022061710000513118",
            "rarity" : "EPIC",
            "dataUrl" : "https://d3j8b1jkcxmuqq.cloudfront.net/temp/collections/TEST_NOMM4/NFT_ITEM/F7A92FB1-B29F-4E6F-BEF1-47C6A1376D68.jpg",
            "contentHash" : "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
            "output" : "alice"
          }
        }
      }
    }
  ]
  
  ```

  ```json
  ["6040003b0020245ce82f352bed95dee2636442efee4e5a15ee3911c67910b657"]
  ```

* MintNFTWithMemo

  * > MinterGroup에 속한 Account의 서명
  * Fields
    * TokenDefinitionID(string)
    * TokenID(string)
    * Rarity(string)
    * DataUrl(string)
    * ContentHash: uint256
    * Output: AccountName

  * Example

  ```json
  [
    {
      "sig" : {
        "sig" : {
          "v" : 28,
          "r" : "d1c7f699ff24b4767e3728f79b13d3d930fa1be02cb511481010fbbaecf538c0",
          "s" : "298829e3f5b03d4b3f87766b655eb3632099f6ea737e5e0d02da6ba03fcd72dd"
        },
        "account" : "alice"
      },
      "value" : {
        "TokenTx" : {
          "MintNFTWithMemo" : {
            "networkId" : 2021,
            "createdAt" : "2023-01-11T19:05:00Z",
            "tokenDefinitionId" : "nft-with-precision",
            "tokenId" : "2022061710000513118",
            "rarity" : "EPIC",
            "dataUrl" : "https://d3j8b1jkcxmuqq.cloudfront.net/temp/collections/TEST_NOMM4/NFT_ITEM/F7A92FB1-B29F-4E6F-BEF1-47C6A1376D68.jpg",
            "contentHash" : "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
            "output" : "alice",
            "memo" : "Test Minting NFT #2022061710000513118"
          }
        }
      }
    }
  ]
  ```
  
  ```json
  ["018edc66aa45e303a2621e5a981c2a2ed5f262802498888814a1844c04b12bd3"]
  ```
  
* BurnNFT
  * > 토큰 소유자 서명
  * Fields
    * TokenDefinitionID(string)
    * Input: SignedTxHash

* UpdateNFT


  * > MinterGroup 에 속한 account 서명

  * Fields
    * TokenDefinitionID(string)

  * Example
	```json
	[
	  {
	    "sig": {
	      "sig": {
	        "v": 28,
	        "r": "1ec82ef3e977dd8e6857e6d77b7955e57bc8d7081730198372f4740c588f0c80",
	        "s": "65031c7011d8aceae4bfbd90049b2bb4c458050988368b3ea3017fb7402c0c03"
	      },
	      "account": "alice"
	    },
	    "value": {
	      "TokenTx": {
	        "UpdateNFT": {
	          "networkId": 2021,
	          "createdAt": "2023-01-11T19:06:00Z",
	          "tokenDefinitionId": "nft-with-precision",
	          "tokenId": "2022061710000513118",
	          "rarity": "EPIC",
	          "dataUrl": "https://d3j8b1jkcxmuqq.cloudfront.net/temp/collections/TEST_NOMM4/NFT_ITEM/F7A92FB1-B29F-4E6F-BEF1-47C6A1376D68.jpg",
	          "contentHash": "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
	          "output": "alice",
	          "memo": "Test Updating NFT #2022061710000513118"
	        }
	      }
	    }
	]
	```

  ```json
  ["e4d85bd90857a9be1e363a10c6543de0a7826966378e2bdb0195572a87e7c1be"]
  ```

* TransferFungibleToken
  * > 토큰 보유자 서명
  * Fields
    * TokenDefinitionID(string)
    * Inputs: Set[SignedTxHash]: UTXO Hash, 모든 토큰 종류는 동일해야 함
    * Outputs: Map[AccountName, Amount]
    * *(optional)* Memo(string)

  * Example

  ```json
  [
    {
      "sig" : {
        "sig" : {
          "v" : 28,
          "r" : "09a5f46d29bd8598f04cb6db32627aadd562e30e181135c2898594080db6aa79",
          "s" : "340abd1b6618d3bbf4b586294a4f902942f597672330563a43591a14be0a6504"
        },
        "account" : "alice"
      },
      "value" : {
        "TokenTx" : {
          "TransferFungibleToken" : {
            "networkId" : 1000,
            "createdAt" : "2022-06-09T09:00:00Z",
            "tokenDefinitionId" : "test-token",
            "inputs" : [
              "a3f35adb3d5d08692a7350e61aaa28da992a4280ad8e558953898ef96a0051ca"
            ],
            "outputs" : {
              "bob" : 10,
              "alice" : 90
            },
            "memo" : "transfer from alice to bob"
          }
        }
      }
    }
  ]
  ```

  ```json
  ["cb3848af6eb3c006c8aa663711d5fcfa2d6b1ccdcaf9837e273a96cc5386785e"]
  ```

  

* TransferNFT
  * > 토큰 보유자 서명
  * Fields
    * TokenDefinitionID(string)
    * TokenID(string)
    * Input: SignedTxHash
    * Output: AccountName
    * *(optional)* Memo(string)

  * Example

  ```json
  [
    {
      "sig" : {
        "sig" : {
          "v" : 27,
          "r" : "c443ed5eda3d484bcda7bf77f030d3f6c20e4130d9bc4e03ca75df3074b40239",
          "s" : "2e7a19f1baee2099ccbef500e7ceb03c5053957a55085ef52b21c022c43242d9"
        },
        "account" : "alice"
      },
      "value" : {
        "TokenTx" : {
          "TransferNFT" : {
            "networkId" : 1000,
            "createdAt" : "2022-06-09T09:00:00Z",
            "definitionId" : "test-token",
            "tokenId" : "2022061710000513118",
            "input" : "6040003b0020245ce82f352bed95dee2636442efee4e5a15ee3911c67910b657",
            "output" : "bob",
            "memo" : null
          }
        }
      }
    }
  ]
  ```

  ```json
  ["1e46633eb70ec8ea484aeb0ef2e7916021b4fcc591712c4ce0514c63c897c6c9"]
  ```

* EntrustFungibleToken 토큰 위임
  * > 토큰 보유자 서명

  * Fields

    * definitionId: TokenDefinitionId 맡길 토큰 종류
    * amount: 맡길 토큰 수량 
    * inputs: Set[SignedTxHash] 입력에 사용할 트랜잭션 해시값.
    * to: Account 위임할 계정. 일반적으로 playnomm

  * Results

    * remainder: Amount 자신에게 돌아오는 수량

  * Example

    ```json
    [
      {
        "sig" : {
          "sig" : {
            "v" : 27,
            "r" : "8d438670820bb788f0ef7106aa55c5fa2fa9c898eaded4d92f29d3c21a99c127",
            "s" : "1545783ca442a5ae2fdd347c79286a1c62256cd91ac76cb392f28dc190ac9c8a"
          },
          "account" : "alice"
        },
        "value" : {
          "TokenTx" : {
            "EntrustFungibleToken" : {
              "networkId" : 1000,
              "createdAt" : "2022-06-09T09:00:00Z",
              "definitionId" : "test-token",
              "amount" : 1000,
              "inputs" : [
                "a3f35adb3d5d08692a7350e61aaa28da992a4280ad8e558953898ef96a0051ca"
              ],
              "to" : "alice"
            }
          }
        }
      }
    ]
    ```

    ```json
    ["45df6a88e74ea44f2d759251fed5a3c319e7cf9c37fafa7471418fec7b26acce"]
    ```

    

* EntrustNFT NFT 위임

  * > NFT 보유자 서명

  * Fields

    * definitionId(string)
    * tokenId(string)
    * input: SignedTxHash
    * to: Account 위임할 계정. 일반적으로 playnomm

  * Example

    ```json
    [
      {
        "sig" : {
          "sig" : {
            "v" : 27,
            "r" : "05705f380f7a7fbad853094f69ff1527703476be30d2ac19f90a24a7900100c0",
            "s" : "37fac4695829b188ebe3d8238259a212ba52588c4593a51ef81631ab9ab90581"
          },
          "account" : "alice"
        },
        "value" : {
          "TokenTx" : {
            "EntrustNFT" : {
              "networkId" : 1000,
              "createdAt" : "2020-06-09T09:00:00Z",
              "definitionId" : "test-token",
              "tokenId" : "2022061710000513118",
              "input" : "6040003b0020245ce82f352bed95dee2636442efee4e5a15ee3911c67910b657",
              "to" : "alice"
            }
          }
        }
      }
    ]
    ```

    ```json
    ["10cb0802f3dfc85abb502bad260120a424fc583016db84d384904c1c0a580955"]
    ```

    

* DisposeEntrustedFungibleToken 위임된 토큰 처분

  * 위임받은 계정(일반적으로 playnomm) 서명

  * Fields
    * definitionID(string)
    * inputs: Set[SignedTxHash]: EntrustFungibleToken 트랜잭션의 UTXO Hash
    * outputs: Map[AccountName, Amount]
      * 토큰을 받아갈 계정과 받아갈 양. 비어 있으면 전체를 원주인에게 반환한다.

  * Example

    ```json
    [
      {
        "sig" : {
          "sig" : {
            "v" : 28,
            "r" : "fb6c99c0e26da04e8dc0855ea629708a17a8deabfabb5a488ba9faa001c4a31f",
            "s" : "7de70d3fd15176451e46856af2dbedf05e58d7cfc0bfb0e0fac1b6d06550f5d3"
          },
          "account" : "alice"
        },
        "value" : {
          "TokenTx" : {
            "DisposeEntrustedFungibleToken" : {
              "networkId" : 1000,
              "createdAt" : "2020-06-10T09:00:00Z",
              "definitionId" : "test-token",
              "inputs" : [
                "45df6a88e74ea44f2d759251fed5a3c319e7cf9c37fafa7471418fec7b26acce"
              ],
              "outputs" : {
                "bob" : 1000
              }
            }
          }
        }
      }
    ]
    ```

    ```json
    ["377fef6a1d85707bb7d84c9b3f5f2a2e409ce57084fbb15a6b200a1237d04119"]
    ```

    

* DisposeEntrustedNFT 위임된 NFT 처분

  * 위임받은 계정(일반적으로 playnomm) 서명

  * Fields
    * definitionID(string)
    * tokenID(string)
    * input: SignedTxHash
    * output: Option[AccountName]
      * NFT를 받아갈 계정. 없으면 원주인에게로 반환한다.

  * Example

    ```json
    [
      {
        "sig" : {
          "sig" : {
            "v" : 28,
            "r" : "a03080b98925010e241783482e83a5fdfc25343406564a4e3fc4e6b2535657d3",
            "s" : "1de0ede5ebeba4aea455094ac1b58fc24ad943f0a5422a93f60a4f2b8b59b982"
          },
          "account" : "alice"
        },
        "value" : {
          "TokenTx" : {
            "DisposeEntrustedNFT" : {
              "networkId" : 1000,
              "createdAt" : "2020-06-10T09:00:00Z",
              "definitionId" : "test-token",
              "tokenId" : "2022061710000513118",
              "input" : "10cb0802f3dfc85abb502bad260120a424fc583016db84d384904c1c0a580955",
              "output" : "bob"
            }
          }
        }
      }
    ]
    ```

    ```json
    ["83c783f31b95cc4a713a921ec1df0725c6675b999ba6285a70c1f777615e4281"]
    ```

* CreateSnapshots

  * > MinterGroup에 속한 Account의 서명

  * Fields
    * definitionID(string)
    * tokenID(string)
    * definitionIds: Set[TokenDefinitionId]
    * *(optional)* Memo(string)

  * Example

    ```json
    [
      {
        "sig" : {
          "sig" : {
            "v" : 27,
            "r" : "2a771418871b3fcfa43a0b00821fce6d9ecec40a1cf2c5ebff4489377c7d0f01",
            "s" : "640b2af02ee4a713d22d2e16e0acd2c61ee1195aa16254cc6481a926d772d866"
          },
          "account" : "alice"
        },
        "value" : {
          "TokenTx" : {
            "CreateSnapshots" : {
              "networkId" : 2021,
              "createdAt" : "2023-01-11T19:09:00Z",
              "definitionIds" : [
                "LM",
                "nft-with-precision"
              ],
              "memo" : "Snapshot for NFT"
            }
          }
        }
      }
    ]
    ```

    ```json
    ["e9fecfafd40e655ac761730bcbb9be524f39370ffa9a272f875275d6cdc50818"]
    ```

### Reward

* RegisterDao 신규 DAO 등록. Group은 미리 생성해 두어야 한다.
  * > Group Coordinator 서명. 일반적으로는 `playnomm`

  * Fields
    * GroupId(string)
    * DaoAccountName(string)
      * 다오 보상 충전용 계정. 여기에 들어온 금액을 매주 정해진 룰에 따라 보상한다. Unique account이어야 한다.
    * Moderators: Set[Account]
      * 최초 모더레이터 목록

  * Example

    ```json
    [
      {
        "sig" : {
          "sig" : {
            "v" : 27,
            "r" : "d4b2d1cfe009e0e5b6dea67779fd898a7f1718e7b1869b5b36b6daacc68e88f6",
            "s" : "42d8c69e964109ceab5996abdbc59d53661904e6b56337599e9c5beebe665d51"
          },
          "account" : "alice"
        },
        "value" : {
          "RewardTx" : {
            "RegisterDao" : {
              "networkId" : 1000,
              "createdAt" : "2020-06-09T09:00:00Z",
              "groupId" : "sample-dao-group-id",
              "daoAccountName" : "sample-dao-group-account",
              "moderators" : [
                "alice"
              ]
            }
          }
        }
      }
    ]
    ```

    ```json
    ["dabd1e1603805080722c6397568e6fc4ef384736a2bf95bc52e0f53acd43bea3"]
    ```

    

* UpdateDao DAO 정보 업데이트. 그룹 조정자가 업데이트 권한을 갖는다.
  * > Group Coordinator 서명. 일반적으로는 `playnomm`

  * Fields
    * GroupId(string)
    * Moderators: Set[Account]
      * 모더레이터 목록

* RecordActivity 활동정보 추가. 그룹 조정자가 업데이트 권한을 갖는다.

  * > Group Coordinator 서명. 일반적으로는 `playnomm`

  * Fields

    * timestamp: 기준시점
    * userActivity: Map[AccountName, Seq[DaoActivity]] 사용자활동 요약 정보

      * DaoActivity 활동정보
        * point 총 점수
        * description 어떤 활동으로 받은 점수인지 간략한 표시
    * tokenReceived: Map[TokenId, Seq[DaoActivity]] 토큰이 받은 사용자활동 요약정보
      * DaoActivity 활동정보
        * point 총 점수
        * description 어떤 활동으로 받은 점수인지 간략한 표시

  * Example

    ```json
    [
      {
        "sig" : {
          "sig" : {
            "v" : 27,
            "r" : "95aff6586d03fa7c66165d9bb49f2a2fd54650f2776c728401c664622d5e2d4c",
            "s" : "2cff82c55822d3266add84ea5853dbc86cf47f24e5787080b76e58681477ba09"
          },
          "account" : "alice"
        },
        "value" : {
          "RewardTx" : {
            "RecordActivity" : {
              "networkId" : 2021,
              "createdAt" : "2023-01-10T18:01:00Z",
              "timestamp" : "2023-01-09T09:00:00Z",
              "userActivity" : {
                "bob" : [
                  {
                    "point" : 3,
                    "description" : "like"
                  }
                ],
                "carol" : [
                  {
                    "point" : 3,
                    "description" : "like"
                  }
                ]
              },
              "tokenReceived" : {
                "text-20230109-0000" : [
                  {
                    "point" : 2,
                    "description" : "like"
                  }
                ],
                "text-20230109-0001" : [
                  {
                    "point" : 2,
                    "description" : "like"
                  }
                ],
                "text-20230109-0002" : [
                  {
                    "point" : 2,
                    "description" : "like"
                  }
                ]
              }
            }
          }
        }
      }
    ]    
    ```
    
    ```json
    ["f08043c06fa17ffaf5c86121db683f5aa879bbf0194de3cac703b0572feaa4cd"]
    ```
    
    

* OfferReward 보상 제공. TransferFungibleToken과 같은 형태로 보상을 실행한다

  * >  보상을 보낼 계정

  * Fields

    * TokenDefinitionID(string)
    * Inputs: Set[SignedTxHash]: UTXO Hash, 모든 토큰 종류는 동일해야 함
    * Outputs: Map[AccountName, Amount]
    * *(optional)* Memo(string)

  * Example

    ```json
    
    ```
    
    ```json
    
    ```

* BuildSnapshot: 보상을 위한 스냅샷 생성. 사용자가 한 활동, 토큰이 받은 활동, 토큰 소유보상의 세 가지 스냅샷을 동시에 만든다

  * > 보상 실행 주체. 일반적으로 Playnomm

  * Fields

    * timestamp: 보상 기준 시점. 이 시점 일주일 전부터 현재 시점까지의 자료를 모아 스냅샷을 생성한다.
    * accountAmount: 계정활동 총 보상량
    * tokenAmount: 토큰이 받을 총 보상량
    * ownershipAmount: 토큰 보유에 따르는 총 보상량

  * Example

    ```json
    [
      {
        "sig" : {
          "sig" : {
            "v" : 28,
            "r" : "004b940e651bb950350157116fbfedf5ec98eed68068cea2b666a9e2b52b9588",
            "s" : "17eb8460877a7d212fac4a59caf7abf1cb96c145f5cae41a8ffce55df226f003"
          },
          "account" : "alice"
        },
        "value" : {
          "RewardTx" : {
            "BuildSnapshot" : {
              "networkId" : 2021,
              "createdAt" : "2023-01-11T18:01:00Z",
              "timestamp" : "2023-01-09T09:00:00Z",
              "accountAmount" : 0,
              "tokenAmount" : 0,
              "ownershipAmount" : 100000000000000000000000
            }
          }
        }
      }
    ]
    ```
    
    ```json
    ["da140a6816e9437c0583b34f64636ba9b3fca02721f2ff90b03460c061067cfa"]
    ```
    
    
    

* ExecuteOwnershipReward: 스냅샷의 자료를 기반으로 토큰 소유 보상 실행.

  * 보상 실행 주체. 일반적으로 Playnomm

  * Fields

    * definitionId 보상에 지급할 토큰 정의 ID. 일반적으로 LM.
    * inputs: Set[TxHash] 보상에 사용할 UTXO
    * targets: Set[TokenId] 보상할 개별 NFT 토큰 ID
    
  * Results
  
    * outputs: Map[Account, Amount] 각 계정별 보상결과
  
  * Example
  
    ```json
    [
      {
        "sig" : {
          "sig" : {
            "v" : 27,
            "r" : "2289a570405738a66d75c1eeae451f899cbcc3bd7fd98b4b4d5aaf807c965211",
            "s" : "0364409abf9829ae5ca38b9c31ee0bcc5ce4dabcff3a5d0be180dd925ec51096"
          },
          "account" : "alice"
        },
        "value" : {
          "RewardTx" : {
            "ExecuteOwnershipReward" : {
              "networkId" : 2021,
              "createdAt" : "2023-01-11T18:01:00Z",
              "inputs" : [
                "270650f92f584d9dbbffb99f3a915dc908fbea28bc3dbf34b8cdbe49c4070611"
              ],
              "targets" : [
                "1234567890",
                "1234567891"
              ]
            }
          }
        }
      }
    ]
    ```
    
    ```json
    ["c7824fd901b71918f10663a2990988b3a933353aebc5d1b80f39d78ce43be1ca"]
    ```
  

### AgendaTx

* SuggestSimpleAgenda 투표 의제 제안.

  * >  투표 의제를 제안할 계정. 일반적으로 playNomm

  * Fields

		* title(string)
    * votingToken: TokenDefinitionId(string) 일반적으로 LM
    * voteStart: Instant
    * voteEnd: Instant
    * voteOption: Map[String, String]

  * Example

    ```json
    [
      {
        "sig" : {
          "sig" : {
            "v" : 27,
            "r" : "dc6e9660b33fdc71b14675e7a7a888fe32e4b3bb6264a3a4e90f572518e53aa8",
            "s" : "069a1c0c2c602f2342384d545aab17e5dd2629efc9f8605dead14df599b5fc96"
          },
          "account" : "alice"
        },
        "value" : {
          "AgendaTx" : {
            "SuggestSimpleAgenda" : {
              "networkId" : 2021,
              "createdAt" : "2023-01-11T18:01:00Z",
              "title" : "Let the world know about LeisureMeta!",
              "votingToken" : "LM",
              "voteStart" : "2023-01-11T18:01:00Z",
              "voteEnd" : "2023-01-12T18:01:00Z",
              "voteOptions" : {
                "1" : "Yes",
                "2" : "No"
              }
            }
          }
        }
      }
    ]
    ```
    
    ```json
    ["2475a387f22c248c5a3f09cea0ef624484431c1eaf8ffbbf98a4a27f43fabc84"]
    ```

* VoteSimpleAgenda 투표.

  * >  투표하는 사용자계정

  * Fields

		* agendaTxHash: 투표할 SuggestSimpleAgenda 트랜잭션의 tx hash
    * selectedOption: 투표내용
    
  * Example
  
    ```json
    [
      {
        "sig" : {
          "sig" : {
            "v" : 28,
            "r" : "89a108a5a933a8d04486384dc90521d0ca5faba1d3a09524068c22936aa2b5ea",
            "s" : "2347e77fa7d1a4f6d10712bb7c5cfb1746f0aef65825dbf03f201fe5e594ee2f"
          },
          "account" : "alice"
        },
        "value" : {
          "AgendaTx" : {
            "VoteSimpleAgenda" : {
              "networkId" : 2021,
              "createdAt" : "2023-01-11T19:01:00Z",
              "agendaTxHash" : "2475a387f22c248c5a3f09cea0ef624484431c1eaf8ffbbf98a4a27f43fabc84",
              "selectedOption" : "1"
            }
          }
        }
      }
    ]
    ```
    
    ```json
    ["07dd86c19884881e1ef037eac4553b735545c03612c5fe368a07189464ad154b"]
    ```

## Other API

| Method | URL                               | Description                      |
| ------ | --------------------------------- | -------------------------------- |
| `GET`  | **/account/{accountName}**        | 계정정보 조회                    |
| `GET`  | **/eth/{ethAddress}**             | 이더리움 주소와 연동된 계정 조회 |
| `GET`  | **/dao**                          | DAO 목록 조회                    |
| `GET`  | **/group/{groupID}**              | 그룹 정보 조회                   |
| `GET`  | **/offering/{offeringID}**        | Offering 정보 조회               |
| `GET`  | **/status**                       | 블록체인 상태 조회               |
| `GET`  | **/token-def/{definitionID}**     | 토큰 정의 정보 조회              |
| `GET`  | **/token/{tokenID}**              | 토큰 정보 조회                  |
| `GET`  | **/token-hist/{txHash}**          | 토큰 과거 정보 조회                  |
| `GET`  | **/snapshot/account/{account}**   | 보상받을 활동 조회               |
| `GET`  | **/snapshot/token/{tokenID}**     | 보상받을 토큰 점수 조회          |
| `GET`  | **/snapshot/ownership/{tokenID}** | 받을 토큰 소유보상 점수 조회     |
| `GET`  | **/rewarded/account/{account}**   | 최근에 받은 활동보상 조회        |
| `GET`  | **/rewarded/token/{tokenID}**     | 최근에 받은 토큰보상 조회        |
| `GET`  | **/rewarded/ownership/{tokenID}** | 최근에 받은 토큰 소유보상 조회   |
| `GET` | **/snapshot-state/{definitionID}** | 토큰정의 스냅샷 상태 조회 |
| `GET` | **/snapshot-balance/ {Account}/{TokenDefinitionID}/{SnapshotID}** | 토큰 스냅샷 잔고 조회 |
| `GET` | **/nft-snapshot-balance/ {Account}/{TokenDefinitionID}/{SnapshotID}** | NFT 스냅샷 잔고 조회 |



## State

Merkle Trie로 관리되는 블록체인 내부 상태들. 키가 사전식으로 정렬되어 있어서 순회 가능하고, StateRoot로 요약가능하다.

### Account

* NameState: AccountName => AccountData
  * AccountData
    * *(optional)* ethAddress
    * *(optional)* guardian (account)

* AccountKeyState: (AccountName, PublicKeySummary) => Desription
  * Description에는 추가된 시각이 포함되어 있어야 함

### Group

* GroupState: GroupID => GroupInfo
  * GroupInfo
    * Group Name
    * Coordinator
* GroupAccountState: (GroupID, AccountName) => ()


### Token

* TokenDefinitionState: TokenDefinitionID(string)=> TokenDefinition
  * TokenDefinition
    * TokenDefinitionID(string)
    * Name(string)
    * *(optional)* Symbol(string)
    * *(optional)* AdminGroup: GroupId
    * TotalAmount
    * *(optional)* NftInfo
      * Minter: AccountName(string)
      * Rarity: Map[(Rarity(string), Weight)]
      * DataUrl(string)
      * ContentHash: uint256
* NftState: TokenID => NftState
  * NftState
    * TokenID
    * TokenDefinitionID
    * Rarity
    * Weight
    * CurrentOwner: Account
* RarityState: (TokenDefinitionID, Rarity, TokenID) => ()
* FungibleBalanceState: (AccountName, TokenDefinitionID, TransactionHash) => ()
* NftBalanceState: (AccountName, TokenID, TransactionHash) => ()
* EntrustFungibleBalanceState: (AccountName, AccountName, TokenDefinitionId, TransactionHash) => ()
* EntrustNftBalanceState: (AccountName, AccountName, TokenId, TransactionHash) => ()

### Reward

* DaoState: GroupID => DaoInfo
  * DaoInfo
    * Moderators: Set[AccountName]
* AccountActivityState: (Account, Instant) => Seq[ActivityLog]
  * ActivityLog
    * account 포인트를 획득한 계정
    * point 총 점수
    * description 묘사
    * txHash 근거가 되는 RecordActivity 트랜잭션 해시값

* TokenReceivedState: (TokenId, Instant) => Seq[ActivityLog]
* AccountSnapshotState: (Account) => ActivitySnapshot
  * ActivitySnapshot
    * account
    * from: Instant
    * to: Instant
    * point 총 포인트
    * definitionId 보상받을 토큰 종류. 일반적으론 LM
    * amount 보상량
    * backlog: Set[TxHash] 해당 카운트의 근거 RecordActivity의 집합
* TokenSnapshotState: (TokenId) => ActivitySnapshot
* OwnershipSnapshotState: (TokenId) => OwnershipSnapshot
  * OwnershipSnapshot
    * account
    * timestamp 기준 시점
    * point 포인트. 일반적으론 해당 NFT의 Rarity 점수
    * definitionId 보상받을 토큰 종류. 일반적으론 LM
    * amount 보상량
* AccountRewardedState: (Account) => ActivityRewardLog
  * ActivityRewardLog
    * ActivitySnapshot
    * ExecuteReward  TxHash
* TokenRewardedState: (TokenId) => ActivityRewardLog
* OwnershipRewardedState: (TokenId) => OwnershipRewardLog
  * OwnershipRewardLog
    * OwnershipShapshot
    * ExecuteReward TxHash


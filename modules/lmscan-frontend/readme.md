#### To start

```md
# sbt 실행

cmd) sbt
sbt) project lmscanFrontend
sbt:leisuremeta-chain-lmscan-fronte) ~fastLinkJS

# yarn 실행

cmd) cd modules/lmscan-frontend
cmd) yarn start
```

## 폴더구조

#### components

```hs
components -- 콤포넌트 파일 모음
├── @pages -- 페이지 콤포넌트
│   ├── PagesUpdate.scala
│   ├── PagesView.scala
│   ├── blocks
│   ├── dashboard
│   └── transcations
└── @widgets -- 위젯 콤포넌트
    ├── board
    ├── nav
    ├── search
    └── table
```

#### controllers

```hs
contrellers -- 기능별 파일 모음
├── Init.scala
├── Model.scala
├── Msg.scala
├── Subscription.scala
├── Update.scala
└── View.scala
```

#### functions

```hs
functions -- 재사용 가능 파일 모음
├── dom.scala
└── log.scala
```

#### 웹서버 동작

```hs

```

#### 이전페이지 구현

```
prev page -> current page :: (prev,current)

case :: 12
1
-> 2 = (1,2)

case :: 101
1
-> 0 = (1,0)
-> 1 = (_1,1)

case :: 100
1
-> 0 = (1,0)
-> 0 = (_1,0)

case :: 12101
1
-> 2 = (1,2)
-> 1 = (2,1)
-> 0 = (1,0)
-> 1 = (_1,0)
```

#### 화면 넘기기 구현

```md
# lists

- tx_curr_page,tx_total_page => tx_list
- block_curr_page,block_total_page => block_list

# details

- tx_hash => tx details
- block_hash => block details
- account_hash => account details (transaction history)
- nft_hash => nft details
```

#### 서치, 클릭 통합

```md
# 파이프라인

[prevPage , searchvalue-store ] => [*prevPage ]

# 처리순서

- 클릭 => 서치 => 완료

== 0. 전처리 ( prevPage , search value store 변경 )
== 1. data-update ( data )
== 2. page-update ( curPage )
== 3. 후처리 ( search value 초기화 , search store 초기화 )

# 변경될 변수

- searchValue => 초기화
- prev , curpage
- data

# 테스트

- 트랜잭션 디테일 클릭 => 트랜잭션 디테일 or nopage
- 트랜잭션 디테일 서치 => 트랜잭션 디테일 or nopage

# 페이징

- 대시보드, 블록, 트랜잭션 ... 일반 문자열 클릭
- 해시값을 클릭
- 해시값을 검색

# TODO

1. 트랜잭션 디테일 검색, 클릭 했을때
   0,1,2,3 의 프로세스를 거쳐서 페이지 전환되도록 만들기
```

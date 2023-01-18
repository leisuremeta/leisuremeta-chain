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

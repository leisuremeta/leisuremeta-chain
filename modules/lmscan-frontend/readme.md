#### To start

```md
cmd) sbt
sbt) project lmscanFrontend
sbt:leisuremeta-chain-lmscan-fronte) ~fastLinkJS
```

#### 폴더구조

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

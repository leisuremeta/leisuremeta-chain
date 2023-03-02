```md
# type flow

type Tx
= Signed[Transaction]
= class Signed[Transaction](sig: AccountSignature, value: Transaction)

final case class Signed[A](sig: AccountSignature, value: A)

Seq[Tx].traverse{ tx: Signed[Transation] => tx.value:Transaction match .... }
```

```scala
# TransactionWithResultParser
TransactionService.scala

    result <- txs
      .traverse { tx => // Signed[Transaction]
        StateT[EitherT[F, String, *], MerkleState, TransactionWithResult] {
          (ms: MerkleState) =>
            tx.value match
              case txv: Transaction.AccountTx =>
                UpdateState[F, Transaction.AccountTx](ms, tx.sig, txv)
              case txv: Transaction.GroupTx =>
                UpdateState[F, Transaction.GroupTx](ms, tx.sig, txv)
              case txv: Transaction.TokenTx =>
                UpdateState[F, Transaction.TokenTx](ms, tx.sig, txv)
              case txv: Transaction.RewardTx =>
                UpdateState[F, Transaction.RewardTx](ms, tx.sig, txv)
              case txv: Transaction.AgendaTx =>
                UpdateState[F, Transaction.AgendaTx](ms, tx.sig, txv)
        }
      }


```

```scala
 tx.signedTx.value match
        case tokenTx: Transaction.TokenTx =>
          tokenTx match
            // d46026045a1c55d296763716200be43770a99b34f305277fbf9ba4b4da7a5539
            case nft: DefineToken                  => List(genTxDetail())
            // 8aef1ebb832e8c019622c161627e6296879a9d11b9408ddbabf8ca7450b3111c
            case nft: MintNFT                      => List(genTxDetail())
            // dc4d9a8194527088e2c5d6e7f0d1377b7b07d1808a458b75183ceaf9dde3cf44
            case nft: TransferNFT                  => List(genTxDetail())
            // ...
            case nft: BurnNFT                      => List(genTxDetail())
            // 947d2abf4b005b167f8af1b2f4635e1ea4697867337d0cb8f98c598ad72631de
            case nft: EntrustNFT                   => List(genTxDetail())
            // ff0caee27216efc42d8fa2b59af244f0b340ee287e249a663551a745b0e7e4de
            case nft: DisposeEntrustedNFT          => List(genTxDetail())
            // 3e55e5645c2fb731e708b170cdb93ac8a0c3ce64a6ea9c9cbeb70f6ed71242eb
            case tx: MintFungibleToken             => List(genTxDetail())
            // 30af10a434c60bd7d76c9595c73239e58566ae4697a29790e8a5c03e765770ad
            case tx: TransferFungibleToken         => List(genTxDetail())
            // f582446a93c1f19d9cbb3636ffb17135de1c1d9ee2529471ff35fe0753530142
            case tx: EntrustFungibleToken          => List(genTxDetail())
            // d5e0db85f565ddee9169b60589c56c9009c29c111a9fa168afda6a283dea2453
            case tx: DisposeEntrustedFungibleToken => List(genTxDetail())
            case tx: BurnFungibleToken             => List(genTxDetail())

        case accountTx: Transaction.AccountTx =>
          accountTx match
            case tx: CreateAccount => "190a6addfe2e71102453bd7a565a7c11d629b5b798e0a3606cf60822793c1f20"
            case tx: UpdateAccount => "f4693919e74140ae177957bcc70288ed9bc38cf00fab4649bf3a224c7f4115ac"
            case tx: AddPublicKeySummaries => "0ed77c040ce631a11c8ecb3f4820cadbc9754c14a3bf8ca977246a8f557e98bc"

        case groupTx: Transaction.GroupTx =>
          groupTx match
            // f4f3162753f115e8889c431c4d35dd8562921dc613b4a0142e75f81fd1e7522b
            case tx: CreateGroup => List(genTxDetail(), genTxDetail())
            // 93b52aaed7fac7af928a80198373376b77b5f42e3f4e406188233ead93d4233c
            case tx: AddAccounts => List(genTxDetail(), genTxDetail())

        case rewardTx: Transaction.RewardTx =>
          rewardTx match
            // 845a79e922298e4da0deb732cf3aa5bdc547a757945cff03699899a1f9871941
            case tx: RegisterDao            => List(genTxDetail())
            case tx: UpdateDao              => List(genTxDetail())
            case tx: RecordActivity         => List(genTxDetail())
            // d6a4d811bc15ec72f3f8ac89c092eb42390d3fb35f432319bf4acd84417f3341
            case tx: OfferReward            => List(genTxDetail())
            case tx: ExecuteReward          => List(genTxDetail())
            case tx: BuildSnapshot          => List(genTxDetail())
            case tx: ExecuteOwnershipReward => List(genTxDetail())

        case agendaTx: Transaction.AgendaTx =>
          agendaTx match
            // 9f236c659a6c496146c72e113f98307792d3fdaa0eea915c7e6b3f71d1a5822b
            case tx: SuggestSimpleAgenda => List(genTxDetail())
            // 7399802e05ebe1584f92d93ba4208ffb1f31fc82b4c472a3b146db6291852d23
            case tx: VoteSimpleAgenda    => List(genTxDetail()),
```

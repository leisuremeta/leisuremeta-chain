#### backend 공통 flow

```md
/api/{hash}
|> 엔드포인트.서버로직
|> 서비스로직
|> 레파지토리
```

#### tx/{hash}/detail

```md
tx/{hash}/detail
|> getTxDetailEndPoint.serverLogic
|> TransactionService.getDetail
|> {
= tx <- TransactionRepository.get ( query[T] )
= txDetail <- f(tx)
}
= txDetail
```

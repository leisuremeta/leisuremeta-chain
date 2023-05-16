```
db
|> dao
|> dto
|> frontend api


```

# 기존

```
**/tx/list**



api
|> enpoint
      pageInfo: PageNavigation, == pageNo,sizePerRequest
      accountAddr: Option[String],
      blockHash: Option[String],

|> getPageByFilter
- getPage
- getPageByBlock
- getPageByAccount
|> dto
- PageResponse(page.totalCount, page.totalPages, txInfo)
|> dao
- TransactionRepository.getPage(pageNavInfo)
|> db



```

# bff 방식

```
**/tx/list**

api
= db
|> dao
|> dto
```

# 시나리오

```
호출 자체를 묶어서 하도록 로직변경

# 기존
|프론트 호출 : a , b1 | => |프론트 : a + b1 |
|프론트 호출 : a , b2 | => |프론트 : a + b2 |
|프론트 호출 : a , b3 | => |프론트 : a + b3 |
...

# bff
| 프론트 호출 : a+b1 | => | 프론트 : a+b1 |
| 프론트 호출 : b2 | => | 프론트 : a+b2 |
| 프론트 호출 : b3 | => | 프론트 : a+b2 |
...
```

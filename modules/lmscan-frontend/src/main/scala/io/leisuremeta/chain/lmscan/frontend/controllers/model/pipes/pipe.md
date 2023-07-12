# app structure

```scala
init
ob[
    new_Page[pubs],
    page[pubs],
    page[pubs],
    page[pubs],
    page[pubs],
    page[pubs],
    page[pubs],
    cur_page[pubs],<= 
    page[pubs],
    page[pubs],
    init_page[pubs],
]
```

# pipeline

```scala
 model.observers
 |> find_Observer => observer
 |> in_Observer_PageCase => pageCase
 |> in_PageCase_pubs => pubs

 Observers
 - find_Observer

 observer
 - in_Observer_PageCase
 - in_Observer_Number  없음

 PageCase(name,url,pubs,status)
 - in_PageCase_Name
 - in_PageCase_url
 - in_PageCase_pubs
 - in_PageCase_status

 PubCase(::page,pub_m1,pub_m2)
 - :: in_PubCase_page
 - :: in_PubCase_pub_m1
 - :: in_PubCase_pub_m2
```

# 시나리오
```scala

onclick ( #pagecase{v_name,v_url, pubs})
|> PreUpdate(
#pagecase
- Window.History(#pagecase.#url) :: page |> url |> window.histroy
- Window.History(#page.url)
)
url(page,options..)
|> urlParser
|> api
|> 
```


# 동형 상태 검색 모델
```scala
states[
    new_PageCase[pubs],
    pageCase[pubs],
    pageCase[pubs],
    pageCase[pubs],
    pageCase[pubs],
    pageCase[pubs],
    pageCase[pubs],
    cur_pageCase[pubs],<= 
    pageCase[pubs],
    pageCase[pubs],
    init_pageCase[pubs],
]




// 성능 최적화를 위한 고안.. 한번 검색한 블록, tx 는 일정시간 이내에 다시 검색하지 않는다

states[
    page[pub-tx,pub-block],
    page[pub-tx,pub-block],
    page[pub-tx,pub-block],
    page[pub-tx,pub-block],
    page[pub-tx,pub-block],
    page[pub-tx,pub-block],
]

pubBlockStates[
    pub-block13,
    pub-block14,
    pub-block15,
    pub-block16,
    pub-block17,
    pub-block18,
    ...
]

pubTxStates[
    pub-tx13,
    pub-tx14,
    pub-tx15,
    pub-tx16,
    pub-tx17,
    pub-tx18,
    ...
]

```
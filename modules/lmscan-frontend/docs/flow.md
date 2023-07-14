# app structure

```scala
appStates[
    new_Page[pubs],
    page[pubs],
    page[pubs],
    page[pubs],
    page[pubs],
    page[pubs],
    page[pubs],
    cur_page[pubs], <= pointer 기준으로 현재페이지 검색
    page[pubs],
    page[pubs],
    init_page[pubs],
]
```

# app 상태 변경 로직

```scala

PageMsg.PreUpdate
- url 히스토리 업데이트
- tx 페이지 상태 업데이트
- block 페이지 상태 업데이트
- 앱 상태 업데이트

PageMsg.PreUpdate => PageMsg.DataUpdate
- 페이지 상태 업데이트 이후 => 데이터 반영

PageMsg.GotoObserver
- 앱의 상태 탐색을 담당 (테스트용으로 만들었으나, 사용 x)

PageMsg.BackObserver
- 페이지 뒤로가기 기능

PageMsg.RolloBack
- 버튼 클릭 뒤로가기 기능
- 뒤로가기 할때, 현재의 상태를 없앰

```

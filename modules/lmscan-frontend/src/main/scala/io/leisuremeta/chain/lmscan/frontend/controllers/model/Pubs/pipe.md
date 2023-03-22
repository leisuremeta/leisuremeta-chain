# 파이프라인

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

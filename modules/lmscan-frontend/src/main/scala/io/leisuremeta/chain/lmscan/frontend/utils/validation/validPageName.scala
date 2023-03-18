package io.leisuremeta.chain.lmscan.frontend

import io.circe.*, io.circe.generic.semiauto.*
import io.circe.syntax.*
import io.circe.parser.*

enum PageName:
  case DashBoard, Blocks, Transactions, NoPage
  case TransactionDetail(hash: String) extends PageName
  case BlockDetail(hash: String)       extends PageName
  case NftDetail(hash: String)         extends PageName
  case AccountDetail(hash: String)     extends PageName
  case Page64(hash: String)            extends PageName
  case None                            extends PageName

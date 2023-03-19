package io.leisuremeta.chain.lmscan.frontend

import io.circe.Json

sealed trait Msg

enum PageMsg extends Msg:
  case PreUpdate(page: PageCase) extends PageMsg
  case UpdateObserver(page: Int) extends PageMsg
  case BackObserver              extends PageMsg

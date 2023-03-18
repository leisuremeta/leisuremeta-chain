package io.leisuremeta.chain.lmscan.frontend

import io.circe.Json

sealed trait Msg

enum PageMsg extends Msg:
  case PreUpdate(page: PageName) extends PageMsg

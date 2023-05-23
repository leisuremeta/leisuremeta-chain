package io.leisuremeta.chain.lmscan.frontend

import io.circe.Json

sealed trait Msg

enum PageMsg extends Msg:
  case PreUpdate(page: PageCase)           extends PageMsg
  case GotoObserver(page: Int)             extends PageMsg
  case BackObserver                        extends PageMsg
  case RolloBack                           extends PageMsg
  case None                                extends PageMsg
  case GetFromBlockSearch(value: String)   extends PageMsg
  case PatchFromBlockSearch(value: String) extends PageMsg
  case GetFromTxSearch(value: String)      extends PageMsg
  case PatchFromTxSearch(value: String)    extends PageMsg

  // 데이터 업데이트
  case DataUpdate(sub: PubCase) extends PageMsg

enum InputMsg extends Msg:
  case Get(value: String) extends InputMsg

  case Patch extends InputMsg

enum ToggleMsg extends Msg:
  case OnClick(value: Boolean) extends ToggleMsg

enum DetailButtonMsg extends Msg:
  case OnClick(value: Boolean) extends DetailButtonMsg

enum CommandMsg extends Msg:
  case OnClick(commandCase: CommandCaseMode | CommandCaseLink)
      extends CommandMsg

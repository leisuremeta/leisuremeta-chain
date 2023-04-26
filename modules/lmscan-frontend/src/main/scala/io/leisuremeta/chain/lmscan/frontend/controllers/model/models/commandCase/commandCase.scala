package io.leisuremeta.chain.lmscan.frontend

enum CommandCaseMode:
  case Development extends CommandCaseMode
  case Production  extends CommandCaseMode

enum CommandCaseLink:
  case Development extends CommandCaseLink
  case Production  extends CommandCaseLink

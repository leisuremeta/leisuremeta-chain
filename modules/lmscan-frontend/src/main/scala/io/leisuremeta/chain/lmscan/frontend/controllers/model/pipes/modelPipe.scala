package io.leisuremeta.chain.lmscan.frontend
import scala.util.chaining.*
import scala.scalajs.js
import io.leisuremeta.chain.lmscan.frontend.PupCasePipe.*
import io.leisuremeta.chain.lmscan.frontend.PageCasePipe.*
import io.leisuremeta.chain.lmscan.frontend.StateCasePipe.*
import io.leisuremeta.chain.lmscan.frontend.Builder.*

object ModelPipe:
  def in_appStates(model: Model) =
    model.appStates

  def in_curAppState(model: Model) =
    model.pointer

  def find_curent_State(model: Model) =
    model.appStates
      .pipe(find_State(model.pointer))

  def find_currentPage(model: Model) =
    model
      .pipe(find_curent_State)
      .pipe(in_pageCase)
      .pipe(in_PubCases)(0) // 0 => find
      .pipe(in_Page)

  def find_current_PageCase(model: Model, find: Int = 0) =
    val _find = find match
      case 0 => model.pointer
      case _ => find

    // in_Observer_PageCase(model.appStates, _find)

  def find_last_PageCase(model: Model, find: Int = 0) =
    val _find = find match
      case 0 => model.pointer
      case _ => find

  def find_cunrrent_PageCase(model: Model) =
    find_PageCase(model.pointer)(model.appStates)

  // in_Observer_PageCase(model.appStates, _find)

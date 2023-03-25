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

  def find_current_PubPage(model: Model) =
    model
      .pipe(find_curent_State)
      .pipe(in_pageCase)
      .pipe(in_PubCases)(0) // 0 => find
      .pipe(in_Page)

  def get_PageCase(model: Model, find: Int = 0) =
    val _find = find match
      case 0 => model.pointer
      case _ => find

    model.pipe(in_appStates).pipe(find_PageCase(_find))

  def find_current_PageCase(model: Model) =
    model
      .pipe(in_appStates)
      .pipe(find_PageCase(model.pointer))

  def find_last_PageCase(model: Model) =
    model
      .pipe(in_appStates)
      .pipe(find_PageCase(model.appStates.length))

  def find_cunrrent_PageCase(model: Model) =
    find_PageCase(model.pointer)(model.appStates)

  def find_tx_curpage(model: Model) =
    model
      .pipe(find_current_PageCase)
      .pipe(in_PubCases)
      .pipe(pubs =>
        pubs.filter(pub =>
          pub match
            case pub: PubCase.TxPub => true
            case _                  => false,
        ),
      )(0)
      .pipe(in_Page)

  def find_name(model: Model) =
    model
      .pipe(find_cunrrent_PageCase)
      .pipe(in_Name)
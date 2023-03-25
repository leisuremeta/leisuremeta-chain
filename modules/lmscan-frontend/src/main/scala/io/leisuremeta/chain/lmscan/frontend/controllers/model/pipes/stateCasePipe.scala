package io.leisuremeta.chain.lmscan.frontend
import scala.scalajs.js
import io.leisuremeta.chain.lmscan.frontend.Log.log
import io.leisuremeta.chain.lmscan.common.model.*
import scala.util.chaining.*

// TODO:: go, pipe 함수로 redesign!
object StateCasePipe:
  def in_pageCase(state: StateCase) = state.pageCase
  def in_number(state: StateCase)   = state.number

  // List[StateCase]
  def find_State(find: Int)(states: List[StateCase]): StateCase =
    // find 가 0이면 가장 최신 옵져버로 검색할수 있게 해준다
    val _find = find match
      case 0 => states.length
      case _ => find
    states.filter(state => state.number == _find)(0)

  def find_PageCase(find: Int = 0)(states: List[StateCase]) =
    states
      .pipe(find_State(find))
      .pipe(in_pageCase)

  def in_Observer_Number(states: List[StateCase], find: Int = 0) =
    states
      .pipe(find_State(find))
      .pipe(in_number)

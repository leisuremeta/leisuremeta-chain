package io.leisuremeta.chain.lmscan.frontend
import scala.scalajs.js
import io.leisuremeta.chain.lmscan.frontend.Log.log
import io.leisuremeta.chain.lmscan.common.model.*

// TODO:: go, pipe 함수로 redesign!
object StateCasePipe:
  // #1-observer
  def find_State(states: List[StateCase], find: Int) =
    // find 가 0이면 가장 최신 옵져버로 검색할수 있게 해준다
    val _find = find match
      case 0 => states.length
      case _ => find
    states.filter(o => o.number == _find)(0)

//   def find_CurState(states: List[StateCase]) =
//     find_State

  // #1-observer-function
  def in_Observer_PageCase(states: List[StateCase], find: Int = 0) =
    find_State(states, find).pageCase

  def in_Observer_Number(states: List[StateCase], find: Int = 0) =
    find_State(states, find).number

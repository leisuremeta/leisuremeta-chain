package io.leisuremeta.chain.lmscan.frontend
import tyrian.Html.*
import tyrian.*
import scala.util.chaining.*
import TestoLogic.*
import TestoSample.*

object TestoView:
  def view(model: Model): Html[Msg] =
    div(`class` := s"testo")(
      h1("testo page"),
      div({
        Map(
          "str2int"     -> strInt.pipe(str2Int).pipe(any2Str),
          "str2Long"    -> strInt.pipe(str2Long).pipe(any2Str),
          "sample1"     -> sample1,
          "sample3"     -> sample3,
          "numberFixed" -> numberFixed,
          // "str2int" -> str.pipe(str2Int).pipe(x2Str),
          // "str2int" -> str.pipe(str2Int).pipe(x2Str),
        )("numberFixed")

      }),
    )

package io.leisuremeta.chain.lmscan.frontend

import cats.effect.IO
import tyrian.Html.*
import tyrian.*

import Log.log

object SearchUpdate:
  def update(model: Model): InputMsg => (Model, Cmd[IO, Msg]) =
    case InputMsg.Get(s) =>
      (model.copy(searchValue = s), Cmd.None)
    case InputMsg.Patch =>
      val updated =
        model.copy(
          searchValue = "",
          curPage = model.searchValue match
            case "1" =>
              log("1클릭!")
              log(OnTxMsg.getTxList("random"))
              // log(OnTxMsg.getTxList("cats"))
              // log("EncoderTest.article")
              // log(EncoderTest.article)
              // log("EncoderTest.authorEncoder(EncoderTest.huet)")
              // log(EncoderTest.authorEncoder(EncoderTest.huet))
              // log("DecoderTest")
              // log(DecoderTest.test1)
              // log("DecoderTest2")
              // log(DecoderTest.test2)
              // log("DecoderTest3")
              // log(DecoderTest.test3)
              // log("DecoderTest4")
              // log(DecoderTest.test4)
              // log("sample txlist")
              // log(SampleJson.parseResult)
              // log(HttpHelper.getRandomGif("cats"))
              NavMsg.DashBoard
            case "2" => NavMsg.Blocks
            case "3" => NavMsg.Transactions
            case _   => NavMsg.NoPage
          ,
          prevPage = model.searchValue match
            case "1" => model.curPage
            case "2" => model.curPage
            case "3" => model.curPage
            case _ =>
              model.curPage match
                // noPage 일때는, 이전페이지를 변경하지 않는다.
                case NavMsg.NoPage => model.prevPage
                case _             => model.curPage,
        )

      (log(updated), Cmd.None)

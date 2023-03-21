package io.leisuremeta.chain.lmscan.frontend

trait PageCase:
  def name: String
  def url: String
  def pubs: List[PubCase]
  def pubs_m1: List[PubCase_M1]
  def status: Boolean
  // def pubsub: List[PageResponse[BlockInfo]] // todo :: fix

object PageCase:
  case class Blocks(
      name: String = "Blocks",
      url: String = "Blocks",
      pubs: List[PubCase] = List(PubCase.blockPub(1)),
      pubs_m1: List[PubCase_M1] = List(PubCase_M1.None()),
      status: Boolean = false,
      // pubsub: List[PageResponse[BlockInfo]] = List(
      //   new PageResponse(0, 0, List()),
      // ),
  ) extends PageCase

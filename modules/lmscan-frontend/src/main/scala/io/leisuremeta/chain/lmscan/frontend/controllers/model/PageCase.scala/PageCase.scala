package io.leisuremeta.chain.lmscan.frontend
import io.leisuremeta.chain.lmscan.common.model.*

trait PageCase:
  def name: String
  def url: String
  def pubs: List[PubCase]
  def status: Boolean
  // def pubsub: List[PageResponse[BlockInfo]] // todo :: fix

object PageCase:
  case class Blocks(
      name: String = "Blocks",
      url: String = "Blocks",
      pubs: List[PubCase] = List(
        PubCase.blockPub(1, "", PageResponse[BlockInfo](0, 0, List())),
      ),
      status: Boolean = false,
  ) extends PageCase

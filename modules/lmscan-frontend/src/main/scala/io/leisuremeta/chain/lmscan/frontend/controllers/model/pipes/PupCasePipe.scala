package io.leisuremeta.chain.lmscan.frontend
import scala.util.chaining.*

object PupCasePipe:
  def in_Page(pubCase: PubCase) =
    pubCase match
      case PubCase.BlockPub(page, _, _)    => page
      case PubCase.TxPub(page, _, _, _, _) => page
      case PubCase.BoardPub(page, _, _)    => page
      case _                               => 1

  def in_pub_m1(pubCase: PubCase) =
    pubCase match
      case PubCase.BlockPub(_, pub_m1, _)         => pub_m1
      case PubCase.TxPub(_, _, _, pub_m1, _)      => pub_m1
      case PubCase.BoardPub(_, pub_m1, _)         => pub_m1
      case PubCase.BlockDetailPub(_, pub_m1, _)   => pub_m1
      case PubCase.TxDetailPub(_, pub_m1, _)      => pub_m1
      case PubCase.AccountDetailPub(_, pub_m1, _) => pub_m1

  def in_pub_m2(pubCase: PubCase) =
    pubCase match
      case PubCase.BlockPub(_, _, pub_m2)         => pub_m2
      case PubCase.TxPub(_, _, _, _, pub_m2)      => pub_m2
      case PubCase.BoardPub(_, _, pub_m2)         => pub_m2
      case PubCase.BlockDetailPub(_, _, pub_m2)   => pub_m2
      case PubCase.TxDetailPub(_, _, pub_m2)      => pub_m2
      case PubCase.AccountDetailPub(_, _, pub_m2) => pub_m2

package io.leisuremeta.chain.lmscan.frontend

import tyrian.Html.*

object Head:
  def template(hs: List[String], cs: String = "") = div(cls := s"row table-head $cs")(hs.map(span(_)))
  val block = template(List("Block", "Age", "Block hash", "TxN")) 
  val accs = template(List("Address", "Balance", "Value", "Last Seen")) 
  val nfts = template(List("", "Season", "Total Supply", "Sale Started", "Sale Ended")) 
  val nftToken = template(List("NFT", "Collection", "Token ID", "Creator", "Rarity"))
  val nft = template(List("Tx Hash", "Timestamp", "Action", "From", "To"))
  val tx = template(List("Tx Hash", "Block", "Age", "Signer", "Subtype"))
  val tx2 = template(List("Tx Hash", "Block", "Age", "Signer", "Subtype", "Value"))
  val tx_dashBoard = template(List("Tx Hash", "Age", "Signer"))
  val vds = template(List("Address", "Voting Power", "Total Block Proposed"))
